package com.wuzhi;

import android.content.Context;

import com.wuzhi.core.PhotoStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 完整备份：一个 zip 里放 wuzhi.json + photos/*.jpg。
 * 换手机时只拷这一个文件就能全部搬过去。
 */
public final class Backup {

    public static final String ENTRY_JSON = "wuzhi.json";
    public static final String ENTRY_PHOTOS = "photos/";
    public static final String SUFFIX = ".wuzhi.zip";

    private Backup() {
    }

    /** 打包导出。调用方负责关流。 */
    public static int exportZip(Context c, OutputStream os) throws IOException {
        ZipOutputStream z = new ZipOutputStream(new BufferedOutputStream(os));
        try {
            z.setLevel(6);
            byte[] json = App.get(c).exportJson().getBytes("UTF-8");
            z.putNextEntry(new ZipEntry(ENTRY_JSON));
            z.write(json);
            z.closeEntry();

            int n = 0;
            File[] fs = PhotoStore.get(c).dir().listFiles();
            if (fs != null) {
                for (File f : fs) {
                    if (!f.isFile()) continue;
                    String name = f.getName();
                    if (name.endsWith(".part")) continue;
                    z.putNextEntry(new ZipEntry(ENTRY_PHOTOS + name));
                    InputStream in = new java.io.FileInputStream(f);
                    try {
                        byte[] buf = new byte[8192];
                        int k;
                        while ((k = in.read(buf)) > 0) z.write(buf, 0, k);
                    } finally {
                        try { in.close(); } catch (IOException ignored) { }
                    }
                    z.closeEntry();
                    n++;
                }
            }
            z.finish();
            return n;
        } finally {
            try { z.close(); } catch (IOException ignored) { }
        }
    }

    /** 从 zip 恢复：成功返回 null，失败返回原因。 */
    public static String importZip(Context c, InputStream is) {
        ZipInputStream z = new ZipInputStream(new BufferedInputStream(is));
        String json = null;
        int photos = 0;
        File dir = PhotoStore.get(c).dir();
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        try {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                String name = e.getName();
                if (e.isDirectory()) {
                    z.closeEntry();
                    continue;
                }
                if (ENTRY_JSON.equals(name) || name.endsWith("/" + ENTRY_JSON)) {
                    json = readAll(z);
                } else if (name.startsWith(ENTRY_PHOTOS)) {
                    String base = name.substring(ENTRY_PHOTOS.length());
                    if (base.indexOf('/') >= 0 || base.indexOf('\\') >= 0 || base.startsWith(".")) {
                        z.closeEntry();
                        continue;
                    }
                    File out = new File(dir, base);
                    FileOutputStream fo = new FileOutputStream(out);
                    try {
                        byte[] buf = new byte[8192];
                        int k;
                        while ((k = z.read(buf)) > 0) fo.write(buf, 0, k);
                        fo.flush();
                    } finally {
                        try { fo.close(); } catch (IOException ignored) { }
                    }
                    photos++;
                }
                z.closeEntry();
            }
        } catch (Exception ex) {
            return "备份文件读不了：" + ex.getMessage();
        } finally {
            try { z.close(); } catch (IOException ignored) { }
        }
        if (json == null) return "这个压缩包里没有 wuzhi.json";
        String err = App.get(c).importJson(json);
        if (err != null) return err;
        return null;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toString("UTF-8");
    }
}
