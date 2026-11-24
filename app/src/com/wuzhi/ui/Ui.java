package com.wuzhi.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 轻量 UI 工具箱：颜色、圆角卡片、文字、按钮。全部基于系统控件。 */
public final class Ui {

    // 配色：暖纸感 + 陶土橙（毛玻璃版本：BG 透明，露出 Glass 的网格渐变底）
    public static final int BG = 0x00000000;
    public static final int PAPER = 0xFFF6F2EB;
    public static final int CARD = 0xFFFFFFFF;
    public static final int INK = 0xFF23201C;
    public static final int INK2 = 0xFF5B554C;
    public static final int MUTED = 0xFF6F695F;
    public static final int LINE = 0xFFE7E0D5;
    public static final int ACCENT = 0xFFC2552E;
    public static final int ACCENT_SOFT = 0xFFF6E3DA;
    public static final int GREEN = 0xFF3F7D58;
    public static final int GREEN_SOFT = 0xFFE2EFE6;
    public static final int BLUE = 0xFF3B6E8F;
    public static final int BLUE_SOFT = 0xFFE1ECF3;
    public static final int AMBER = 0xFF9A6B12;
    public static final int AMBER_SOFT = 0xFFF7EBD2;
    public static final int DARK = 0xFF2A2622;

    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static float dpf(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(dpf(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundStroke(int color, float radiusDp, int strokeColor, float strokeDp, Context c) {
        GradientDrawable d = round(color, radiusDp, c);
        d.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return d;
    }

    public static GradientDrawable gradient(int top, int bottom, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        d.setCornerRadius(dpf(c, radiusDp));
        return d;
    }

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setTypeface(Type.body(c));
        return t;
    }

    /** 标题：默认从系统字族派生（跟随手机字体），可在设置里换宋体。 */
    public static TextView title(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(sp >= 18 ? Type.title(c) : Type.bold(c));
        if (sp >= 20) t.setLetterSpacing(-0.015f);
        return t;
    }

    public static TextView bold(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(Type.bold(c));
        return t;
    }

    /** 大数字：等宽数字，跳动时不抖。 */
    public static TextView number(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(sp >= 20 ? Type.display(c) : Type.body(c));
        Type.tabular(t);
        return t;
    }

    /** 小标签：字距略开，弱化颜色，用在卡片眉题和字段名上。 */
    public static TextView label(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setLetterSpacing(0.08f);
        return t;
    }

    /** 分区标题：比正文重一点、字距略开，不抢主标题。 */
    public static TextView section(Context c, CharSequence s) {
        TextView t = text(c, s, 13, INK2);
        t.setTypeface(Type.bold(c));
        t.setLetterSpacing(0.06f);
        return t;
    }

    /** 毛玻璃卡片（背后透出真实背景色，边缘有 1px 高光）。 */
    public static LinearLayout card(Context c) {
        GlassCard l = new GlassCard(c);
        l.radius(20);
        l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        return l;
    }

    /** 更轻的毛玻璃条（列表行用）。 */
    public static LinearLayout cardTight(Context c) {
        GlassCard l = new GlassCard(c);
        l.radius(16).tint(Glass.TINT_WEAK);
        l.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        return l;
    }

    /** 自定圆角的毛玻璃块。 */
    public static GlassCard glass(Context c, float radiusDp, int tint) {
        GlassCard l = new GlassCard(c);
        l.radius(radiusDp).tint(tint);
        return l;
    }

    public static View spacer(Context c, float dpHeight) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, dpHeight)));
        return v;
    }

    public static View hspacer(Context c, float dpWidth) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, dpWidth), 1));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.7f)));
        v.setLayoutParams(lp);
        return v;
    }

    /** 主按钮 */
    public static TextView button(Context c, String label, boolean primary) {
        TextView t = title(c, label, 15, primary ? 0xFFFFFFFF : INK);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        t.setBackground(ripple(round(primary ? ACCENT : 0xFFF0EAE0, 14, c), c));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static TextView buttonSmall(Context c, String label, int bg, int fg) {
        TextView t = title(c, label, 13, fg);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        t.setBackground(ripple(round(bg, 11, c), c));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /** 标签胶囊 */
    public static TextView chip(Context c, String label, int bg, int fg, float sp) {
        TextView t = text(c, label, sp, fg);
        t.setPadding(dp(c, 9), dp(c, 4), dp(c, 9), dp(c, 4));
        t.setBackground(round(bg, 9, c));
        return t;
    }

    /** 图标 + 文字的小按钮（图标是 Canvas 手绘的，不用 emoji） */
    public static LinearLayout iconButton(Context c, int glyphKind, String label, int bg, int fg) {
        LinearLayout row = hbox(c);
        row.setBackground(ripple(round(bg, 11, c), c));
        row.setPadding(dp(c, 10), dp(c, 7), dp(c, 12), dp(c, 7));
        row.setClickable(true);
        row.setFocusable(true);
        Glyph g = new Glyph(c, glyphKind, false, 16f);
        g.color(fg);
        row.addView(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(c, 5);
        row.addView(title(c, label, 13, fg), lp);
        return row;
    }

    /** 页面切换时轻微的淡入 + 上浮 */
    public static void fadeIn(View v) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 10));
        v.animate().alpha(1f).translationY(0f).setDuration(240).start();
    }

    public static Drawable ripple(GradientDrawable base, Context c) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new RippleDrawable(android.content.res.ColorStateList.valueOf(0x22383020), base, null);
        }
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, round(0xFFE9E1D4, 14, c));
        s.addState(new int[]{}, base);
        return s;
    }

    /** 可点击卡片：按下有反馈 */
    public static void clickable(View v, Context c) {
        v.setClickable(true);
        v.setFocusable(true);
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpw(float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.weight = weight;
        return p;
    }

    public static LinearLayout.LayoutParams lpMatch() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static void margin(View v, Context c, float l, float t, float r, float b) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
            v.setLayoutParams(lp);
        }
    }

    public static int blend(int a, int b, float t) {
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        return Color.rgb((int) (ar + (br - ar) * t), (int) (ag + (bg - ag) * t), (int) (ab + (bb - ab) * t));
    }

    /** 分类配色 */
    public static int colorFor(String category) {
        if (category == null) return ACCENT;
        switch (category) {
            case "数码": return 0xFF3B6E8F;
            case "家电": return 0xFF4C7A6B;
            case "家具": return 0xFF8A6A45;
            case "服饰": return 0xFFB05B4A;
            case "鞋包": return 0xFF8A5A6B;
            case "美妆": return 0xFFC2552E;
            case "运动": return 0xFF2F7F5F;
            case "厨具": return 0xFFB07A2A;
            case "书籍": return 0xFF5A6B7C;
            case "交通": return 0xFF4A5A6A;
            default: return 0xFF7A7568;
        }
    }

    public static int softFor(String category) {
        return blend(colorFor(category), 0xFFFFFFFF, 0.86f);
    }
}
