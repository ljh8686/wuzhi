package com.wuzhi.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.wuzhi.core.PhotoStore;

/**
 * 毛玻璃（glassmorphism）底座。
 *
 * 做法：先在小尺寸上画一张"网格渐变"背景（米白底 + 几个彩色光斑），
 * 做两遍 box blur 抹平，再放大到屏幕尺寸。卡片再按自己在窗口里的位置
 * 从这张图上裁一块贴到自己身后 —— 于是每张卡片背后透出的颜色和它实际
 * 覆盖的背景完全一致，看起来就是真的毛玻璃（而不是半透明白块）。
 */
public final class Glass {

    /** 玻璃面板默认白度 */
    public static final int TINT = 0x8AFFFFFF;
    public static final int TINT_STRONG = 0xB0FFFFFF;
    public static final int TINT_WEAK = 0x44FFFFFF;
    /** 边缘高光 */
    public static final int STROKE = 0xC6FFFFFF;
    public static final int STROKE_SOFT = 0x66FFFFFF;

    /** 背景照片在 SharedPreferences 里的键 */
    private static final String PREF = "wuzhi";
    private static final String KEY_WALL = "glass_wall";

    private static Bitmap cached;
    private static int cachedW = -1;
    private static int cachedH = -1;
    private static Bitmap noiseCached;
    private static String wallpaperId;

    private Glass() {}

    /**
     * 128x128 平铺噪点。大面积平滑渐变在 8bit 屏上会有 banding（看起来像塑料），
     * 叠一层 alpha 6~14 的噪点就立刻有"玻璃/毛面"的质感。
     */
    public static synchronized Bitmap noise(Context c) {
        if (noiseCached != null && !noiseCached.isRecycled()) return noiseCached;
        final int n = 128;
        int[] pix = new int[n * n];
        java.util.Random rnd = new java.util.Random(20261123L);
        for (int i = 0; i < pix.length; i++) {
            int v = rnd.nextBoolean() ? 0xFF : 0x00;
            int a = 4 + rnd.nextInt(7);
            pix[i] = (a << 24) | (v << 16) | (v << 8) | v;
        }
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        b.setPixels(pix, 0, n, 0, 0, n, n);
        noiseCached = b;
        return b;
    }

    /** 是否用了自定义背景照片。 */
    public static synchronized boolean wallpaperActive(Context c) {
        if (wallpaperId == null || wallpaperId.length() == 0) restoreWallpaper(c);
        return wallpaperId != null && wallpaperId.length() > 0;
    }

    /**
     * 设置背景照片（id 是 PhotoStore 里的文件名）。传 null / 空串恢复默认渐变。
     * 会立刻让缓存失效，下次绘制重新生成。
     */
    public static synchronized void setWallpaper(Context c, String id) {
        wallpaperId = (id == null || id.length() == 0) ? null : id;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(KEY_WALL, wallpaperId == null ? "" : wallpaperId).apply();
        cached = null;
        cachedW = -1;
        cachedH = -1;
    }

    /** 启动时从偏好里恢复背景照片设置。 */
    public static synchronized void restoreWallpaper(Context c) {
        String id = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_WALL, "");
        wallpaperId = (id == null || id.length() == 0) ? null : id;
    }

    /** 全屏柔化背景（缓存，只生成一次）。 */
    public static synchronized Bitmap backdrop(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int w = Math.max(2, dm.widthPixels);
        int h = Math.max(2, dm.heightPixels);
        if (cached != null && cachedW == w && cachedH == h) return cached;

        int sw = Math.max(8, w / 4);
        int sh = Math.max(8, h / 4);
        Bitmap small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(small);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Bitmap photo = null;
        if (wallpaperId != null && wallpaperId.length() > 0) {
            try {
                photo = PhotoStore.loadById(c, wallpaperId, Math.max(w, h));
            } catch (Throwable ignored) {
            }
        }

        if (photo != null) {
            // 背景照片：居中裁切铺满 → 盖一层白纱（保证黑字可读）→ 重度模糊
            p.setFilterBitmap(true);
            float sc = Math.max(sw / (float) photo.getWidth(), sh / (float) photo.getHeight());
            float dw = photo.getWidth() * sc, dh = photo.getHeight() * sc;
            RectF dst = new RectF((sw - dw) / 2f, (sh - dh) / 2f, (sw + dw) / 2f, (sh + dh) / 2f);
            cv.drawBitmap(photo, null, dst, p);
            p.setFilterBitmap(false);
            p.setColor(0x74FFFFFF);
            cv.drawRect(0, 0, sw, sh, p);
        } else {
            p.setShader(new LinearGradient(0, 0, sw * 0.35f, sh,
                    new int[]{0xFFFFFCF6, 0xFFF9EFE2, 0xFFEFE0D0}, null, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, sw, sh, p);
            p.setShader(null);

            blob(cv, sw * 0.14f, sh * 0.08f, sw * 0.72f, 0x77E8825A);
            blob(cv, sw * 0.92f, sh * 0.24f, sw * 0.66f, 0x66F2B25A);
            blob(cv, sw * 0.06f, sh * 0.56f, sw * 0.62f, 0x556FA88A);
            blob(cv, sw * 0.95f, sh * 0.72f, sw * 0.70f, 0x5C7FA3C0);
            blob(cv, sw * 0.42f, sh * 0.40f, sw * 0.55f, 0x40FFFFFF);
            blob(cv, sw * 0.30f, sh * 0.92f, sw * 0.60f, 0x4DD98AA0);
        }

        int[] pix = new int[sw * sh];
        small.getPixels(pix, 0, sw, 0, 0, sw, sh);
        boxBlur(pix, sw, sh, photo != null ? 9 : 5);
        boxBlur(pix, sw, sh, photo != null ? 7 : 4);
        small.setPixels(pix, 0, sw, 0, 0, sw, sh);

        Bitmap full = Bitmap.createScaledBitmap(small, w, h, true);
        if (full != small) small.recycle();

        cached = full;
        cachedW = w;
        cachedH = h;
        return cached;
    }

    /** 1:1 平铺的窗口背景，保证卡片裁剪坐标和它完全对齐。 */
    public static Drawable backdropDrawable(Context c) {
        final Bitmap b = backdrop(c);
        return new Drawable() {
            @Override public void draw(Canvas canvas) {
                if (b == null || b.isRecycled()) return;
                canvas.drawBitmap(b, 0, 0, null);
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(android.graphics.ColorFilter cf) {}
            @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
        };
    }

    private static void blob(Canvas cv, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(cx, cy, r, color, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, p);
    }

    /** 三通道 box blur（两遍就够抹平渐变）。 */
    private static void boxBlur(int[] pix, int w, int h, int r) {
        if (r < 1) return;
        int[] tmp = new int[pix.length];
        int span = r * 2 + 1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int sr = 0, sg = 0, sb = 0;
            for (int x = -r; x <= r; x++) {
                int p = pix[row + clamp(x, w)];
                sr += (p >> 16) & 0xFF; sg += (p >> 8) & 0xFF; sb += p & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                tmp[row + x] = 0xFF000000 | ((sr / span) << 16) | ((sg / span) << 8) | (sb / span);
                int pa = pix[row + clamp(x + r + 1, w)];
                int pb = pix[row + clamp(x - r, w)];
                sr += ((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF);
                sg += ((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF);
                sb += (pa & 0xFF) - (pb & 0xFF);
            }
        }
        for (int x = 0; x < w; x++) {
            int sr = 0, sg = 0, sb = 0;
            for (int y = -r; y <= r; y++) {
                int p = tmp[clamp(y, h) * w + x];
                sr += (p >> 16) & 0xFF; sg += (p >> 8) & 0xFF; sb += p & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                pix[y * w + x] = 0xFF000000 | ((sr / span) << 16) | ((sg / span) << 8) | (sb / span);
                int pa = tmp[clamp(y + r + 1, h) * w + x];
                int pb = tmp[clamp(y - r, h) * w + x];
                sr += ((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF);
                sg += ((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF);
                sb += (pa & 0xFF) - (pb & 0xFF);
            }
        }
    }

    private static int clamp(int v, int n) {
        if (v < 0) return 0;
        if (v >= n) return n - 1;
        return v;
    }

    /** 从背景里裁一块（给需要自己做模糊的控件用）。 */
    public static void paintCrop(Canvas canvas, Context c, int x, int y, int w, int h, Paint paint) {
        Bitmap bg = backdrop(c);
        if (bg == null || w <= 0 || h <= 0) return;
        Rect src = new Rect();
        android.graphics.RectF dst = new android.graphics.RectF();
        int bw = bg.getWidth(), bh = bg.getHeight();
        int cx = Math.max(0, Math.min(x, bw - 1));
        int cy = Math.max(0, Math.min(y, bh - 1));
        int cw = Math.min(w, bw - cx);
        int ch = Math.min(h, bh - cy);
        if (cw <= 0 || ch <= 0) return;
        src.set(cx, cy, cx + cw, cy + ch);
        dst.set(0, 0, cw, ch);
        canvas.drawBitmap(bg, src, dst, paint);
    }
}
