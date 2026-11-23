package com.wuzhi.core;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 用户自定义照片的本地仓库。
 *
 * <p>照片统一拷贝到 App 私有目录 {@code getFilesDir()/photos/}，文件名为 {@code <id>.jpg}。
 * 导入时会按 EXIF 摆正方向、把长边压到 {@link #MAX_EDGE} 以内，再用 JPEG {@link #JPEG_QUALITY}
 * 重新编码，避免把相册里的原图（动辄几 MB）直接塞进私有目录。</p>
 *
 * <p>约定：所有方法都不抛异常，失败返回 {@code null} / {@code false}；
 * 方法均为同步（主线程快速连续调用也安全），但导入一张图会有几百毫秒的磁盘与解码开销。</p>
 */
public final class PhotoStore {

    private static final String TAG = "PhotoStore";

    /** 落盘后允许的最长边（像素）。超过则等比缩小。 */
    public static final int MAX_EDGE = 1600;
    /** 重新编码的 JPEG 质量。 */
    public static final int JPEG_QUALITY = 88;

    /** 单张原图最大允许体积，防止从异常 provider 拉取超大文件。 */
    private static final long MAX_SOURCE_BYTES = 64L * 1024 * 1024;
    /** inSampleSize 上限，防止极端图片把循环拖死。 */
    private static final int MAX_SAMPLE = 4096;
    private static final String DIR_NAME = "photos";
    private static final String EXT = ".jpg";

    private static volatile PhotoStore sInstance;

    private final Context app;
    private final File dir;

    private PhotoStore(Context c) {
        Context base = c;
        try {
            Context ac = c.getApplicationContext();
            if (ac != null) base = ac;
        } catch (Throwable ignored) {
            // 极少数 mock / 特殊 Context 会抛异常，退回原 Context
        }
        this.app = base;
        this.dir = dirOf(base);
        ensureDir();
    }

    /** 拿 filesDir；极端情况下（Context 异常 / 目录不可用）退到 cacheDir，再不行给个相对路径。 */
    private static File dirOf(Context c) {
        File files = null;
        try {
            files = c.getFilesDir();
        } catch (Throwable ignored) {
        }
        if (files == null) {
            try {
                files = c.getCacheDir();
            } catch (Throwable ignored) {
            }
        }
        return new File(files, DIR_NAME);
    }

    /** 取单例。多次调用返回同一个实例。 */
    public static PhotoStore get(Context c) {
        PhotoStore s = sInstance;
        if (s == null) {
            synchronized (PhotoStore.class) {
                s = sInstance;
                if (s == null) {
                    s = new PhotoStore(c);
                    sInstance = s;
                }
            }
        }
        return s;
    }

    // ------------------------------------------------------------ 路径

    /** 照片目录 {@code getFilesDir()/photos/}，必要时自动创建。 */
    public synchronized File dir() {
        ensureDir();
        return dir;
    }

    /** id 对应的文件（不保证存在）。 */
    public synchronized File fileFor(String id) {
        return new File(dir, safeId(id) + EXT);
    }

    /** 已存在则返回绝对路径，否则 null。 */
    public synchronized String pathFor(String id) {
        try {
            File f = fileFor(id);
            if (f.isFile() && f.length() > 0) return f.getAbsolutePath();
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 是否已有照片。 */
    public synchronized boolean has(String id) {
        try {
            File f = fileFor(id);
            return f.isFile() && f.length() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------ 导入

    /**
     * 把相册/文件选择器返回的 Uri 拷进私有目录并压好。
     *
     * @param uri content:// 或 file:// 均可（内部走 ContentResolver.openInputStream）
     * @param id  业务 id（物品 / 衣物），会做文件名安全化处理
     * @return 保存后的绝对路径；失败返回 null
     */
    public synchronized String importFromUri(Uri uri, String id) {
        if (uri == null) return null;
        File tmp = null;
        Bitmap src = null;
        Bitmap out = null;
        try {
            if (!ensureDir()) return null;
            tmp = File.createTempFile("imp_", ".tmp", dir);
            if (!copyToFile(uri, tmp)) return null;
            if (tmp.length() <= 0) return null;

            src = decodeScaled(tmp, MAX_EDGE);
            if (src == null) return null;

            out = orient(src, readOrientation(tmp));
            if (out != src) {
                // 旋转产生的新图与原图独立，原图可以立刻释放
                if (!src.isRecycled()) src.recycle();
                src = null;
            }
            return writeJpeg(out, id);
        } catch (Throwable t) {
            Log.w(TAG, "importFromUri failed: " + uri, t);
            return null;
        } finally {
            if (src != null && !src.isRecycled()) src.recycle();
            if (out != null && !out.isRecycled()) out.recycle();
            if (tmp != null) tmp.delete();
        }
    }

    /** 便捷重载：内部 {@code PhotoStore.get(c).importFromUri(uri, id)}。 */
    public static String importFromUri(Context c, Uri uri, String id) {
        if (c == null) return null;
        return get(c).importFromUri(uri, id);
    }

    /**
     * 直接把一张 Bitmap 存成照片（缩放到 MAX_EDGE 内）。不回收传入的 bitmap。
     *
     * @return 保存后的绝对路径；失败返回 null
     */
    public synchronized String importFromBitmap(Bitmap bmp, String id) {
        if (bmp == null || bmp.isRecycled()) return null;
        Bitmap scaled = null;
        try {
            if (!ensureDir()) return null;
            Bitmap work = bmp;
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int longEdge = Math.max(w, h);
            if (longEdge > MAX_EDGE) {
                float s = MAX_EDGE / (float) longEdge;
                int tw = Math.max(1, Math.round(w * s));
                int th = Math.max(1, Math.round(h * s));
                scaled = Bitmap.createScaledBitmap(bmp, tw, th, true);
                if (scaled == null) return null;
                work = scaled;
            }
            return writeJpeg(work, id);
        } catch (Throwable t) {
            Log.w(TAG, "importFromBitmap failed", t);
            return null;
        } finally {
            if (scaled != null && !scaled.isRecycled()) scaled.recycle();
        }
    }

    // ------------------------------------------------------------ 读取

    /**
     * 按需解码。{@code reqPx} 是期望的最长边（<=0 表示原样解码），
     * 内部用 inJustDecodeBounds + inSampleSize 只解到够用的大小，并统一 RGB_565 省内存。
     *
     * @return 解码失败 / 文件不存在返回 null
     */
    public synchronized Bitmap load(String path, int reqPx) {
        if (path == null || path.length() == 0) return null;
        try {
            File f = new File(path);
            if (!f.isFile() || f.length() <= 0) return null;

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            int w = bounds.outWidth;
            int h = bounds.outHeight;
            if (w <= 0 || h <= 0) return null;

            int sample = 1;
            if (reqPx > 0) {
                int longEdge = Math.max(w, h);
                while (sample < MAX_SAMPLE && longEdge / (sample * 2) >= reqPx) {
                    sample *= 2;
                }
            }

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = sample;
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            opt.inScaled = false; // 忽略密度缩放，按真实像素解
            return BitmapFactory.decodeFile(path, opt);
        } catch (Throwable t) {
            Log.w(TAG, "load failed: " + path, t);
            return null;
        }
    }

    /** id 对应的照片按需解码，没有照片返回 null。 */
    public synchronized Bitmap loadById(String id, int reqPx) {
        String p = pathFor(id);
        if (p == null) return null;
        return load(p, reqPx);
    }

    /** 便捷重载：内部 {@code PhotoStore.get(c).loadById(id, reqPx)}。 */
    public static Bitmap loadById(Context c, String id, int reqPx) {
        if (c == null) return null;
        return get(c).loadById(id, reqPx);
    }

    // ------------------------------------------------------------ 删除

    /** 删除某 id 的照片（不存在也安全）。 */
    public synchronized void delete(String id) {
        try {
            File f = fileFor(id);
            if (f.exists()) f.delete();
            // 顺手清掉可能残留的半成品
            File part = new File(dir, f.getName() + ".part");
            if (part.exists()) part.delete();
        } catch (Throwable t) {
            Log.w(TAG, "delete failed: " + id, t);
        }
    }

    /** 清空所有照片（设置页"清理缓存"可用）。 */
    public synchronized void deleteAll() {
        try {
            File[] fs = dir.listFiles();
            if (fs == null) return;
            for (File f : fs) {
                if (f.isFile()) f.delete();
            }
        } catch (Throwable t) {
            Log.w(TAG, "deleteAll failed", t);
        }
    }

    // ------------------------------------------------------------ 内部

    private boolean ensureDir() {
        try {
            if (dir.isDirectory()) return true;
            if (dir.mkdirs()) return true;
            return dir.isDirectory();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 把文件名安全化：只留字母数字、下划线和短横线，避免路径穿越。 */
    private static String safeId(String id) {
        if (id == null) return "photo";
        StringBuilder sb = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char ch = id.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-';
            sb.append(ok ? ch : '_');
        }
        String s = sb.toString();
        if (s.length() == 0) s = "photo";
        if (s.length() > 96) s = s.substring(0, 96);
        return s;
    }

    /** 把 Uri 的字节流写到 dst。 */
    private boolean copyToFile(Uri uri, File dst) {
        InputStream in = null;
        OutputStream out = null;
        try {
            ContentResolver cr = app.getContentResolver();
            if (cr == null) return false;
            in = cr.openInputStream(uri);
            if (in == null) return false;
            out = new BufferedOutputStream(new FileOutputStream(dst), 32 * 1024);
            byte[] buf = new byte[32 * 1024];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_SOURCE_BYTES) {
                    Log.w(TAG, "source too large: " + uri);
                    return false;
                }
                out.write(buf, 0, n);
            }
            out.flush();
            return total > 0;
        } catch (SecurityException se) {
            Log.w(TAG, "no permission to read uri: " + uri, se);
            return false;
        } catch (Throwable t) {
            Log.w(TAG, "copy uri failed: " + uri, t);
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /** 只解到长边 >= maxEdge 的最小采样，再精确缩到 maxEdge 以内。 */
    private static Bitmap decodeScaled(File f, int maxEdge) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), bounds);
        int w = bounds.outWidth;
        int h = bounds.outHeight;
        if (w <= 0 || h <= 0) return null;

        int longEdge = Math.max(w, h);
        int sample = 1;
        while (sample < MAX_SAMPLE && longEdge / (sample * 2) >= maxEdge) {
            sample *= 2;
        }

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = sample;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
        if (bmp == null) return null;

        int bw = bmp.getWidth();
        int bh = bmp.getHeight();
        int bl = Math.max(bw, bh);
        if (bl <= maxEdge) return bmp;

        float s = maxEdge / (float) bl;
        int tw = Math.max(1, Math.round(bw * s));
        int th = Math.max(1, Math.round(bh * s));
        Bitmap scaled;
        try {
            scaled = Bitmap.createScaledBitmap(bmp, tw, th, true);
        } catch (Throwable t) {
            return bmp;
        }
        if (scaled != null && scaled != bmp) {
            if (!bmp.isRecycled()) bmp.recycle();
            return scaled;
        }
        return bmp;
    }

    /** 读 EXIF 方向，读不到按正常处理。 */
    private static int readOrientation(File f) {
        try {
            ExifInterface exif = new ExifInterface(f.getAbsolutePath());
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable t) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    /** 按 EXIF 方向摆正 / 镜像。可能返回原对象。 */
    private static Bitmap orient(Bitmap src, int orientation) {
        if (src == null) return null;
        Matrix m = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                m.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                m.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                m.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                m.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                m.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                m.postRotate(90);
                m.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                m.postRotate(270);
                m.postScale(-1, 1);
                break;
            default:
                return src;
        }
        try {
            Bitmap out = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
            return out == null ? src : out;
        } catch (Throwable t) {
            Log.w(TAG, "orient failed", t);
            return src;
        }
    }

    /** 压成 JPEG 写到 {@code <id>.jpg}；先写 .part 再改名，避免半截文件。 */
    private String writeJpeg(Bitmap bmp, String id) {
        if (bmp == null) return null;
        File target = fileFor(id);
        File part = new File(dir, target.getName() + ".part");
        FileOutputStream fos = null;
        boolean ok = false;
        try {
            fos = new FileOutputStream(part);
            ok = bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
            fos.flush();
        } catch (Throwable t) {
            Log.w(TAG, "write jpeg failed: " + target, t);
            ok = false;
        } finally {
            closeQuietly(fos);
        }
        if (!ok || !part.isFile() || part.length() <= 0) {
            part.delete();
            return null;
        }
        if (target.exists() && !target.delete()) {
            part.delete();
            return null;
        }
        if (!part.renameTo(target)) {
            part.delete();
            return null;
        }
        return target.getAbsolutePath();
    }

    private static void closeQuietly(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }
}
