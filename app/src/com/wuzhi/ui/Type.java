package com.wuzhi.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * 字体系统。
 *
 * 关键点：正文、标题全部从 Typeface.DEFAULT 派生 —— 也就是「系统默认字族」。
 * 安卓手机的「字体」设置（MIUI/ColorOS/OriginOS 的主题字体、系统字号）会替换
 * 默认字族，因此本 App 默认就跟随手机字体，不需要申请任何权限。
 *
 * 另外提供两种内置排版：
 *   宋体标题 —— 标题/大数字用系统衬线（思源宋体），正文仍跟随系统；
 *   全宋体   —— 全部衬线，更像一本纸质账簿。
 */
public final class Type {

    public static final int SYSTEM = 0;
    public static final int SERIF_TITLE = 1;
    public static final int SERIF_ALL = 2;

    private static final String PREF = "wuzhi";
    private static final String KEY = "font_mode";
    private static int mode = -1;

    private Type() {}

    public static void init(Context c) {
        if (mode < 0) mode = prefs(c).getInt(KEY, SERIF_TITLE);
    }

    public static int mode(Context c) {
        init(c);
        return mode;
    }

    public static void setMode(Context c, int m) {
        mode = m;
        prefs(c).edit().putInt(KEY, m).apply();
    }

    public static String modeName(Context c) {
        switch (mode(c)) {
            case SERIF_TITLE: return "宋体标题";
            case SERIF_ALL: return "全宋体";
            default: return "跟随系统";
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** 正文：系统默认字族，手机换字体这里就跟着换。 */
    public static Typeface body(Context c) {
        init(c);
        if (mode == SERIF_ALL) return Typeface.create("serif", Typeface.NORMAL);
        return Typeface.DEFAULT;
    }

    /** 加粗的正文/名称。 */
    public static Typeface bold(Context c) {
        init(c);
        if (mode == SERIF_ALL) return Typeface.create("serif", Typeface.BOLD);
        return Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
    }

    /** 页面标题、卡片标题。 */
    public static Typeface title(Context c) {
        init(c);
        if (mode == SYSTEM) return Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        return Typeface.create("serif", Typeface.BOLD);
    }

    /** 大号金额/数字：宋体模式下用衬线，数字更挺。 */
    public static Typeface display(Context c) {
        init(c);
        if (mode == SYSTEM) return Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        return Typeface.create("serif", Typeface.NORMAL);
    }

    /** 等宽数字（上下比较、跳动时不抖）。 */
    public static void tabular(TextView t) {
        try {
            t.setFontFeatureSettings("'tnum' 1");
        } catch (Throwable ignored) {
        }
    }
}
