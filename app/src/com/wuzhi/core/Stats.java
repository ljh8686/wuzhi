package com.wuzhi.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 统计汇总。 */
public final class Stats {

    private Stats() {}

    // ------------------------------------------------------------- 物品

    /** 物品总投入（含附加花销） */
    public static long totalInvested(List<Item> items) {
        long s = 0;
        for (Item i : items) s += i.totalFen();
        return s;
    }

    /** 每天总成本（所有在用物品日均相加） */
    public static double dailyTotal(List<Item> items, long today) {
        double s = 0;
        for (Item i : items) {
            if (i.isEnded()) continue;
            s += i.dailyFen(today);
        }
        return s;
    }

    /** 按分类汇总投入：{分类 -> 分} */
    public static Map<String, Long> byCategory(List<Item> items) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Item i : items) {
            String k = i.category == null ? "其他" : i.category;
            m.put(k, (m.containsKey(k) ? m.get(k) : 0L) + i.totalFen());
        }
        return m;
    }

    /** 按类别统计附加花销总额 */
    public static Map<String, Long> extrasByKind(List<Item> items) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Item i : items) {
            for (CostEntry c : i.extras) {
                String k = c.kind == null ? CostEntry.KIND_OTHER : c.kind;
                m.put(k, (m.containsKey(k) ? m.get(k) : 0L) + c.amountFen);
            }
        }
        return m;
    }

    /** 日均成本最高的 n 件（只算在用的） */
    public static List<Item> mostExpensivePerDay(List<Item> items, long today, int n) {
        List<Item> l = new ArrayList<>();
        for (Item i : items) if (!i.isEnded()) l.add(i);
        l.sort((a, b) -> Double.compare(b.dailyFen(today), a.dailyFen(today)));
        return l.size() > n ? new ArrayList<>(l.subList(0, n)) : l;
    }

    /** 最近 n 天新买的东西 */
    public static List<Item> recentlyBought(List<Item> items, long today, int days) {
        List<Item> l = new ArrayList<>();
        for (Item i : items) if (today - i.buyDate <= days) l.add(i);
        l.sort((a, b) -> Long.compare(b.buyDate, a.buyDate));
        return l;
    }

    /** 每月花费（购买 + 附加花销），按月份倒序 */
    public static List<long[]> monthlySpend(List<Item> items) {
        Map<Long, Long> m = new LinkedHashMap<>();
        for (Item i : items) {
            long k = DateUtil.monthStart(i.buyDate);
            m.put(k, (m.containsKey(k) ? m.get(k) : 0L) + i.priceFen);
            for (CostEntry c : i.extras) {
                long k2 = DateUtil.monthStart(c.date);
                m.put(k2, (m.containsKey(k2) ? m.get(k2) : 0L) + c.amountFen);
            }
        }
        List<long[]> l = new ArrayList<>();
        for (Map.Entry<Long, Long> e : m.entrySet()) l.add(new long[]{e.getKey(), e.getValue()});
        l.sort((a, b) -> Long.compare(b[0], a[0]));
        return l;
    }

    // ------------------------------------------------------------- 衣柜

    /** 衣柜总价值 */
    public static long wardrobeValue(List<Cloth> cloths) {
        long s = 0;
        for (Cloth c : cloths) s += c.priceFen;
        return s;
    }

    /** 平均单次穿着成本（只算穿过的） */
    public static double avgCostPerWear(List<Cloth> cloths) {
        double sum = 0;
        int n = 0;
        for (Cloth c : cloths) {
            if (c.wearCount > 0) { sum += c.costPerWear(); n++; }
        }
        return n == 0 ? 0 : sum / n;
    }

    /** 穿得最多的 n 件 */
    public static List<Cloth> mostWorn(List<Cloth> cloths, int n) {
        List<Cloth> l = new ArrayList<>();
        for (Cloth c : cloths) if (c.wearCount > 0) l.add(c);
        l.sort((a, b) -> Integer.compare(b.wearCount, a.wearCount));
        return l.size() > n ? new ArrayList<>(l.subList(0, n)) : l;
    }

    /** 最久没穿的 n 件（含从没穿过的优先） */
    public static List<Cloth> neglected(List<Cloth> cloths, int n) {
        List<Cloth> l = new ArrayList<>();
        for (Cloth c : cloths) if (!c.retired) l.add(c);
        l.sort((a, b) -> {
            long la = a.lastWornDate == 0 ? Long.MAX_VALUE / 2 : a.lastWornDate;
            long lb = b.lastWornDate == 0 ? Long.MAX_VALUE / 2 : b.lastWornDate;
            return Long.compare(la, lb);
        });
        return l.size() > n ? new ArrayList<>(l.subList(0, n)) : l;
    }

    /** 单次成本最高的 n 件（穿得少又贵的） */
    public static List<Cloth> worstValue(List<Cloth> cloths, int n) {
        List<Cloth> l = new ArrayList<>();
        for (Cloth c : cloths) if (!c.retired) l.add(c);
        l.sort((a, b) -> Double.compare(b.costPerWear(), a.costPerWear()));
        return l.size() > n ? new ArrayList<>(l.subList(0, n)) : l;
    }

    /** 每件衣服的穿着次数分布：{次数 -> 件数} */
    public static Map<Integer, Integer> wearDistribution(List<Cloth> cloths) {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        for (Cloth c : cloths) {
            int bucket = c.wearCount == 0 ? 0 : (c.wearCount < 3 ? 1 : (c.wearCount < 6 ? 2 : (c.wearCount < 12 ? 3 : 4)));
            m.put(bucket, (m.containsKey(bucket) ? m.get(bucket) : 0) + 1);
        }
        return m;
    }

    /** 按分类统计衣柜价值 */
    public static Map<String, Long> wardrobeByCategory(List<Cloth> cloths) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Cloth c : cloths) {
            String k = c.category == null ? "其他" : c.category;
            m.put(k, (m.containsKey(k) ? m.get(k) : 0L) + c.priceFen);
        }
        return m;
    }

    /** 最近 n 天穿了什么：按日期分组 */
    public static List<WearLog> recentLogs(List<WearLog> logs, int n) {
        List<WearLog> l = new ArrayList<>(logs);
        l.sort((a, b) -> Long.compare(b.date, a.date));
        return l.size() > n ? new ArrayList<>(l.subList(0, n)) : l;
    }

    /** 温度分布：把记录按 5°C 一档分组 */
    public static Map<Integer, Integer> tempBuckets(List<WearLog> logs) {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        for (WearLog w : logs) {
            int bucket = Math.floorDiv(w.temp, 5) * 5;
            m.put(bucket, (m.containsKey(bucket) ? m.get(bucket) : 0) + 1);
        }
        return m;
    }
}
