package com.wuzhi.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.View;

import com.wuzhi.core.PhotoStore;

import java.util.HashMap;
import java.util.Map;

/**
 * 配图：用户照片 &gt; 内置卡通插画 &gt; emoji 兜底。
 * 插画来自 res/drawable-nodpi/art_*.png，用资源名查找，不依赖 R 类。
 */
public final class Art {

    private static final Map<String, Drawable> CACHE = new HashMap<String, Drawable>();

    private Art() {
    }

    /** 名称关键词优先，其次按分类（键盘/耳机/桌子有专属插画）。 */
    public static String itemKey(String name, String category) {
        if (name != null) {
            if (name.contains("键盘")) return "item_keyboard";
            if (name.contains("耳机") || name.contains("耳麦")) return "item_headphone";
            if (name.contains("桌")) return "item_desk";
        }
        return itemKey(category);
    }

    /** 物品分类 → 插画 key（对应 res/drawable-nodpi/art_<key>.png）。 */
    public static String itemKey(String category) {
        if (category == null) return "item_other";
        if (category.equals("数码")) return "item_phone";
        if (category.equals("家电")) return "item_appliance";
        if (category.equals("家具")) return "item_furniture";
        if (category.equals("服饰")) return "item_clothing";
        if (category.equals("鞋包")) return "item_shoe";
        if (category.equals("美妆")) return "item_beauty";
        if (category.equals("运动")) return "item_sport";
        if (category.equals("厨具")) return "item_kitchen";
        if (category.equals("书籍")) return "item_book";
        if (category.equals("交通")) return "item_transport";
        return "item_other";
    }

    /** 衣物槽位 / 分类 → 插画 key（对应 art_cloth_<x>.png）。 */
    public static String clothKey(String slotOrCategory) {
        if (slotOrCategory == null) return "cloth_top";
        String s = slotOrCategory;
        if (s.equals("上装") || s.equals("上衣")) return "cloth_top";
        if (s.equals("下装") || s.equals("裤子")) return "cloth_pants";
        if (s.equals("裙子")) return "cloth_skirt";
        if (s.equals("外套")) return "cloth_outer";
        if (s.equals("鞋") || s.equals("鞋包")) return "cloth_shoes";
        if (s.equals("配饰")) return "cloth_acc";
        if (s.equals("包")) return "cloth_bag";
        if (s.equals("内衣")) return "cloth_underwear";
        return "cloth_top";
    }

    /** 按资源名取插画，没有就返回 null（不崩）。 */
    public static Drawable drawable(Context c, String key) {
        if (key == null) return null;
        Drawable d = CACHE.get(key);
        if (d != null) return d;
        try {
            int id = c.getResources().getIdentifier("art_" + key, "drawable", c.getPackageName());
            if (id == 0) return null;
            d = c.getResources().getDrawable(id);
            if (d != null) CACHE.put(key, d);
        } catch (Exception ignored) {
        }
        return d;
    }

    /** 一个圆角方块头像：照片优先，其次插画，最后 emoji。 */
    public static View avatar(Context c, String id, String key, String emoji, int sizeDp, int bgColor) {
        return new Avatar(c, id, key, emoji, sizeDp, bgColor);
    }

    /** 详情页用的大图（宽高相等，圆角更大）。 */
    public static View hero(Context c, String id, String key, String emoji, int sizeDp, int bgColor) {
        Avatar v = new Avatar(c, id, key, emoji, sizeDp, bgColor);
        v.radiusDp = 18f;
        v.insetDp = 10f;
        v.emojiSize = 40f;
        return v;
    }

    public static final class Avatar extends View {

        private final String id;
        private final String key;
        private final String emoji;
        private final int bg;
        private final int sizePx;

        private Bitmap photo;
        private Drawable art;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final RectF dst = new RectF();
        private final Path clip = new Path();

        float radiusDp = 11f;
        float insetDp = 5f;
        float emojiSize = 19f;

        Avatar(Context c, String id, String key, String emoji, int sizeDp, int bgColor) {
            super(c);
            this.id = id;
            this.key = key;
            this.emoji = emoji == null || emoji.isEmpty() ? "📦" : emoji;
            this.bg = bgColor;
            this.sizePx = Ui.dp(c, sizeDp);
            bgPaint.setColor(bg);
            textPaint.setTextAlign(Paint.Align.CENTER);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            reload(c);
        }

        /** 换了照片之后调用，重新读一次。 */
        public void reload(Context c) {
            photo = null;
            art = null;
            if (id != null) {
                try {
                    photo = PhotoStore.loadById(c, id, sizePx * 2);
                } catch (Throwable ignored) {
                }
            }
            if (photo == null) art = Art.drawable(c, key);
            invalidate();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(sizePx, sizePx);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float r = Ui.dpf(getContext(), radiusDp);
            rect.set(0, 0, getWidth(), getHeight());

            // 底
            clip.reset();
            clip.addRoundRect(rect, r, r, Path.Direction.CW);
            cv.save();
            cv.clipPath(clip);
            if (photo != null) {
                drawCenterCrop(cv, photo);
            } else {
                cv.drawRoundRect(rect, r, r, bgPaint);
                if (art != null) {
                    float inset = Ui.dpf(getContext(), insetDp);
                    dst.set(inset, inset, getWidth() - inset, getHeight() - inset);
                    art.setBounds((int) dst.left, (int) dst.top, (int) dst.right, (int) dst.bottom);
                    art.draw(cv);
                } else {
                    textPaint.setColor(Ui.INK);
                    textPaint.setTextSize(Ui.dpf(getContext(), emojiSize));
                    float cy = getHeight() / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                    cv.drawText(emoji, getWidth() / 2f, cy, textPaint);
                }
            }
            cv.restore();

            // 一圈很淡的内描边，让方块和毛玻璃背景分得开
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, Ui.dpf(getContext(), 0.8f)));
            p.setColor(0x14000000);
            cv.drawRoundRect(rect, r, r, p);
        }

        private void drawCenterCrop(Canvas cv, Bitmap b) {
            if (b.getWidth() <= 0 || b.getHeight() <= 0) return;
            float vw = getWidth(), vh = getHeight();
            float scale = Math.max(vw / b.getWidth(), vh / b.getHeight());
            float dw = b.getWidth() * scale, dh = b.getHeight() * scale;
            float left = (vw - dw) / 2f, top = (vh - dh) / 2f;
            dst.set(left, top, left + dw, top + dh);
            cv.drawBitmap(b, null, dst, bmpPaint);
        }
    }

    /** 给没有照片的列表项生成一个柔和底色（和分类色系一致）。 */
    public static int soft(String categoryOrSlot) {
        return Ui.softFor(categoryOrSlot);
    }

    /** 让 Drawable 变暗一点，用作占位。 */
    static int dim(int color, float f) {
        return Color.argb((int) (Color.alpha(color) * f), Color.red(color), Color.green(color), Color.blue(color));
    }
}
