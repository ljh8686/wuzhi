package com.wuzhi.core;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Date helpers working on "epoch day" (days since 1970-01-01, local calendar).
 * Avoids java.time so the app can run on API 24 without desugaring.
 */
public final class DateUtil {

    private DateUtil() {}

    /** Days since 1970-01-01 for today, local time. */
    public static long today() {
        Calendar c = Calendar.getInstance();
        return fromYmd(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** Current hour 0-23. */
    public static int hourNow() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    public static long fromYmd(int y, int m, int d) {
        Calendar c = new GregorianCalendar(y, m - 1, d, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return Math.floorDiv(c.getTimeInMillis(), 86400000L);
    }

    /** epoch day -> {y, m, d} */
    public static int[] toYmd(long epochDay) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochDay * 86400000L + 43200000L);
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)};
    }

    public static String format(long epochDay) {
        int[] a = toYmd(epochDay);
        return pad(a[0], 4) + "-" + pad(a[1], 2) + "-" + pad(a[2], 2);
    }

    public static String formatShort(long epochDay) {
        int[] a = toYmd(epochDay);
        return a[1] + "月" + a[2] + "日";
    }

    public static String formatCn(long epochDay) {
        int[] a = toYmd(epochDay);
        return a[0] + "年" + a[1] + "月" + a[2] + "日";
    }

    /** Parses "2024-05-01" or "2024/5/1"; returns -1 when invalid. */
    public static long parse(String s) {
        if (s == null) return -1;
        s = s.trim().replace('/', '-').replace('.', '-');
        String[] p = s.split("-");
        if (p.length < 3) return -1;
        try {
            int y = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            int d = Integer.parseInt(p[2].trim());
            if (m < 1 || m > 12 || d < 1 || d > 31 || y < 1900 || y > 2200) return -1;
            return fromYmd(y, m, d);
        } catch (Exception e) {
            return -1;
        }
    }

    public static int dayOfWeek(long epochDay) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochDay * 86400000L + 43200000L);
        int d = c.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        return d == 1 ? 7 : d - 1; // 1=Monday .. 7=Sunday
    }

    public static String weekdayCn(long epochDay) {
        String[] n = {"一", "二", "三", "四", "五", "六", "日"};
        return "周" + n[dayOfWeek(epochDay) - 1];
    }

    /** Whole days from a to b (b - a). */
    public static long between(long a, long b) {
        return b - a;
    }

    /**
     * Human "how long ago" text: 今天 / 昨天 / 3 天前 / 2 个月前 / 1 年前.
     */
    public static String ago(long epochDay, long today) {
        long d = today - epochDay;
        if (d <= 0) return "今天";
        if (d == 1) return "昨天";
        if (d < 7) return d + " 天前";
        if (d < 30) return (d / 7) + " 周前";
        if (d < 365) return Math.max(1, d / 30) + " 个月前";
        long y = d / 365;
        long rm = (d % 365) / 30;
        return rm > 0 ? (y + " 年 " + rm + " 个月前") : (y + " 年前");
    }

    /** "使用了 128 天" style duration text. */
    public static String duration(long days) {
        if (days <= 0) return "0 天";
        if (days < 60) return days + " 天";
        if (days < 365) {
            long m = days / 30;
            long rd = days % 30;
            return rd == 0 ? (m + " 个月") : (m + " 个月 " + rd + " 天");
        }
        long y = days / 365;
        long rd = (days % 365) / 30;
        return rd == 0 ? (y + " 年") : (y + " 年 " + rd + " 个月");
    }

    private static String pad(int v, int len) {
        String s = String.valueOf(v);
        while (s.length() < len) s = "0" + s;
        return s;
    }

    /** Month index key like 2024-05 for grouping. */
    public static String monthKey(long epochDay) {
        int[] a = toYmd(epochDay);
        return a[0] + "-" + pad(a[1], 2);
    }

    /** First day of the month containing epochDay. */
    public static long monthStart(long epochDay) {
        int[] a = toYmd(epochDay);
        return fromYmd(a[0], a[1], 1);
    }

    /** Days in the month containing epochDay. */
    public static int monthLength(long epochDay) {
        int[] a = toYmd(epochDay);
        Calendar c = new GregorianCalendar(a[0], a[1] - 1, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static int yearOf(long epochDay) {
        return toYmd(epochDay)[0];
    }
}
