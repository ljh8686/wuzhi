package com.wuzhi.core;

import java.util.Locale;

/** Money / number formatting helpers. All amounts are stored as integer 分 (cents). */
public final class Fmt {

    private Fmt() {}

    /** 12345 -> "123.45" */
    public static String yuan(long fen) {
        boolean neg = fen < 0;
        long a = Math.abs(fen);
        String s = (a / 100) + "." + two(a % 100);
        return neg ? "-" + s : s;
    }

    /** 12345 -> "¥123.45" */
    public static String money(long fen) {
        boolean neg = fen < 0;
        long a = Math.abs(fen);
        return (neg ? "-¥" : "¥") + group(a / 100) + "." + two(a % 100);
    }

    /** Compact money for big numbers: 1234567 -> "¥1.23万" */
    public static String moneyShort(long fen) {
        boolean neg = fen < 0;
        long a = Math.abs(fen);
        String s;
        if (a >= 100000000L * 100L) {
            s = trim(a / 10000000000.0) + "亿";
        } else if (a >= 10000L * 100L) {
            s = trim(a / 1000000.0) + "万";
        } else if (a >= 100000L) {
            s = (a % 100 == 0) ? group(a / 100) : trim2(a / 100.0);
        } else {
            s = yuan(a);
        }
        return (neg ? "-¥" : "¥") + s;
    }

    /** Per-day style amount with up to 2 decimals: 12 -> "¥12.00" */
    public static String perDay(double fen) {
        if (fen < 0) return "-¥" + trim(-fen / 100.0);
        return "¥" + trim2(fen / 100.0);
    }

    /** Plain 2-decimal number of yuan, no symbol. */
    public static String num(double fen) {
        return trim2(fen / 100.0);
    }

    public static String trim2(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static String trim(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static String pct(double ratio) {
        return Math.round(ratio * 100) + "%";
    }

    /** "¥12.3/天" */
    public static String perDayText(double fen) {
        return perDay(fen) + "/天";
    }

    private static String two(long v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private static String group(long v) {
        String s = String.valueOf(v);
        StringBuilder b = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            b.append(s.charAt(i));
            c++;
            if (c % 3 == 0 && i > 0) b.append(',');
        }
        return b.reverse().toString();
    }
}
