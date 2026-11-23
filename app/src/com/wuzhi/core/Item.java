package com.wuzhi.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一件"买来的东西"：价格、购买日、附加花销、状态，以及日均成本。 */
public final class Item {

    public static final String STATUS_ACTIVE = "在用";
    public static final String STATUS_IDLE = "闲置";
    public static final String STATUS_RETIRED = "报废";
    public static final String STATUS_GIFTED = "送人";
    public static final String STATUS_SOLD = "卖出";

    public static final String[] STATUSES = {STATUS_ACTIVE, STATUS_IDLE, STATUS_RETIRED, STATUS_GIFTED, STATUS_SOLD};

    public static final String[] CATEGORIES = {
            "数码", "家电", "家具", "服饰", "鞋包", "美妆", "运动", "厨具", "书籍", "交通", "其他"
    };

    public String id;
    public String name;
    public String category = "其他";
    /** 购入价，分 */
    public long priceFen;
    /** 购买日期（epoch day） */
    public long buyDate;
    /** 附加花销：美化、维修、配件…… */
    public final List<CostEntry> extras = new ArrayList<>();
    public String status = STATUS_ACTIVE;
    /** 报废/送人/卖出 日期；0 表示还没结束 */
    public long endDate;
    /** 卖出回收了多少钱，分 */
    public long salvageFen;
    public String note = "";
    public String emoji = "";
    /** 是否置顶 */
    public boolean pinned;
    /** 创建时间戳，用于排序 */
    public long createdAt;

    public Item() {}

    public Item(String id, String name, long priceFen, long buyDate) {
        this.id = id;
        this.name = name;
        this.priceFen = priceFen;
        this.buyDate = buyDate;
        this.createdAt = System.currentTimeMillis();
    }

    // ------------------------------------------------------------ 成本计算

    /** 附加花销合计 */
    public long extrasFen() {
        long s = 0;
        for (CostEntry c : extras) s += c.amountFen;
        return s;
    }

    /** 总投入 = 购入价 + 附加花销 */
    public long totalFen() {
        return priceFen + extrasFen();
    }

    /** 净投入 = 总投入 - 卖出回收 */
    public long netFen() {
        return Math.max(0, totalFen() - salvageFen);
    }

    public boolean isEnded() {
        return endDate > 0 && !STATUS_ACTIVE.equals(status) && !STATUS_IDLE.equals(status);
    }

    /** 用到 today 或结束日为止，一共用了多少天（至少 1 天） */
    public long usedDays(long today) {
        long end = isEnded() ? endDate : today;
        long d = end - buyDate;
        return Math.max(1, d);
    }

    /** 日均成本（分） */
    public double dailyFen(long today) {
        return (double) netFen() / (double) usedDays(today);
    }

    /** 日均成本（元，保留两位小数的字符串） */
    public String dailyText(long today) {
        return Fmt.perDayText(dailyFen(today));
    }

    /** 如果继续用 n 天，日均会降到多少（分） */
    public double dailyFenAfter(long today, long extraDays) {
        long d = usedDays(today) + Math.max(0, extraDays);
        return (double) netFen() / (double) d;
    }

    /** 最近一次加钱是哪天 */
    public long lastCostDate() {
        long max = buyDate;
        for (CostEntry c : extras) if (c.date > max) max = c.date;
        return max;
    }

    /** 按时间排列的累计投入曲线点：{epochDay, cumulativeFen} */
    public List<long[]> costTimeline() {
        List<long[]> pts = new ArrayList<>();
        pts.add(new long[]{buyDate, priceFen});
        List<CostEntry> sorted = new ArrayList<>(extras);
        sorted.sort((a, b) -> Long.compare(a.date, b.date));
        long acc = priceFen;
        for (CostEntry c : sorted) {
            acc += c.amountFen;
            pts.add(new long[]{c.date, acc});
        }
        return pts;
    }

    // ------------------------------------------------------------ JSON

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("category", category);
        m.put("price", priceFen);
        m.put("buyDate", buyDate);
        m.put("status", status);
        m.put("endDate", endDate);
        m.put("salvage", salvageFen);
        m.put("note", note);
        m.put("emoji", emoji);
        m.put("pinned", pinned);
        m.put("createdAt", createdAt);
        List<Object> ex = new ArrayList<>();
        for (CostEntry c : extras) ex.add(c.toJson());
        m.put("extras", ex);
        return m;
    }

    public static Item fromJson(Map<String, Object> m) {
        Item it = new Item();
        it.id = Json.str(m, "id", "");
        it.name = Json.str(m, "name", "");
        it.category = Json.str(m, "category", "其他");
        it.priceFen = Json.lng(m, "price", 0);
        it.buyDate = Json.lng(m, "buyDate", DateUtil.today());
        it.status = Json.str(m, "status", STATUS_ACTIVE);
        it.endDate = Json.lng(m, "endDate", 0);
        it.salvageFen = Json.lng(m, "salvage", 0);
        it.note = Json.str(m, "note", "");
        it.emoji = Json.str(m, "emoji", "");
        it.pinned = Json.bool(m, "pinned", false);
        it.createdAt = Json.lng(m, "createdAt", 0);
        for (Object o : Json.arr(m.get("extras"))) {
            it.extras.add(CostEntry.fromJson(Json.obj(o)));
        }
        return it;
    }

    public String statusEmoji() {
        switch (status) {
            case STATUS_ACTIVE: return "🟢";
            case STATUS_IDLE: return "⚪";
            case STATUS_RETIRED: return "🔴";
            case STATUS_GIFTED: return "🎁";
            case STATUS_SOLD: return "💰";
            default: return "🟢";
        }
    }
}
