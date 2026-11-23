package com.wuzhi.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 衣柜里的一件衣服。 */
public final class Cloth {

    public static final String[] CATEGORIES = {"上衣", "裤子", "裙子", "外套", "鞋", "配饰", "包", "内衣", "其他"};

    /** 搭配槽位 */
    public static final String SLOT_TOP = "上装";
    public static final String SLOT_BOTTOM = "下装";
    public static final String SLOT_OUTER = "外套";
    public static final String SLOT_SHOES = "鞋";
    public static final String SLOT_ACC = "配饰";

    public String id;
    public String name;
    /** 分类，如 上衣 */
    public String category = "上衣";
    /** 搭配槽位 */
    public String slot = SLOT_TOP;
    /** 价格，分 */
    public long priceFen;
    /** 购买日期 */
    public long buyDate;
    /** 舒适温度下限（摄氏，可负） */
    public int tempMin = 15;
    /** 舒适温度上限 */
    public int tempMax = 28;
    /** 厚薄/保暖描述，如 薄款/加绒 */
    public String warmth = "";
    public String color = "";
    /** 穿着次数（由记录推导，缓存） */
    public int wearCount;
    /** 上次穿着日期，0 表示没穿过 */
    public long lastWornDate;
    /** 上次穿着的温度，999 表示无 */
    public int lastWornTemp = NO_TEMP;
    /** 收藏/喜爱 */
    public boolean favorite;
    /** 已淘汰 */
    public boolean retired;
    public String note = "";
    public long createdAt;

    public static final int NO_TEMP = 999;

    public Cloth() {}

    public Cloth(String id, String name, String category, String slot, long priceFen, long buyDate) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.slot = slot;
        this.priceFen = priceFen;
        this.buyDate = buyDate;
        this.createdAt = System.currentTimeMillis();
    }

    /** 单次穿着成本（分）：价格 / 穿着次数 */
    public double costPerWear() {
        if (wearCount <= 0) return priceFen;
        return (double) priceFen / (double) wearCount;
    }

    public String costPerWearText() {
        return wearCount <= 0 ? "还没穿过" : Fmt.perDay(costPerWear()) + "/次";
    }

    /** 已拥有多少天 */
    public long ownedDays(long today) {
        return Math.max(1, today - buyDate);
    }

    /** 温度区间文本：18~26°C */
    public String tempRangeText() {
        return tempMin + "~" + tempMax + "°C";
    }

    /** 是否适合某温度 */
    public boolean fits(int temp) {
        return temp >= tempMin && temp <= tempMax;
    }

    /** 偏离舒适区多少度，0 表示正好 */
    public int distanceTo(int temp) {
        if (temp < tempMin) return tempMin - temp;
        if (temp > tempMax) return temp - tempMax;
        return 0;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("category", category);
        m.put("slot", slot);
        m.put("price", priceFen);
        m.put("buyDate", buyDate);
        m.put("tempMin", tempMin);
        m.put("tempMax", tempMax);
        m.put("warmth", warmth);
        m.put("color", color);
        m.put("wearCount", wearCount);
        m.put("lastWornDate", lastWornDate);
        m.put("lastWornTemp", lastWornTemp);
        m.put("favorite", favorite);
        m.put("retired", retired);
        m.put("note", note);
        m.put("createdAt", createdAt);
        return m;
    }

    public static Cloth fromJson(Map<String, Object> m) {
        Cloth c = new Cloth();
        c.id = Json.str(m, "id", "");
        c.name = Json.str(m, "name", "");
        c.category = Json.str(m, "category", "上衣");
        c.slot = Json.str(m, "slot", SLOT_TOP);
        c.priceFen = Json.lng(m, "price", 0);
        c.buyDate = Json.lng(m, "buyDate", DateUtil.today());
        c.tempMin = Json.integer(m, "tempMin", 15);
        c.tempMax = Json.integer(m, "tempMax", 28);
        c.warmth = Json.str(m, "warmth", "");
        c.color = Json.str(m, "color", "");
        c.wearCount = Json.integer(m, "wearCount", 0);
        c.lastWornDate = Json.lng(m, "lastWornDate", 0);
        c.lastWornTemp = Json.integer(m, "lastWornTemp", NO_TEMP);
        c.favorite = Json.bool(m, "favorite", false);
        c.retired = Json.bool(m, "retired", false);
        c.note = Json.str(m, "note", "");
        c.createdAt = Json.lng(m, "createdAt", 0);
        return c;
    }

    public static String slotForCategory(String category) {
        switch (category) {
            case "上衣": return SLOT_TOP;
            case "裤子":
            case "裙子": return SLOT_BOTTOM;
            case "外套": return SLOT_OUTER;
            case "鞋": return SLOT_SHOES;
            case "配饰":
            case "包": return SLOT_ACC;
            default: return SLOT_TOP;
        }
    }

    /** 默认舒适温度区间（按分类与厚薄猜测） */
    public static int[] defaultTempRange(String category, String warmth) {
        boolean thick = warmth != null && (warmth.contains("厚") || warmth.contains("加绒") || warmth.contains("羽绒") || warmth.contains("毛"));
        boolean thin = warmth != null && (warmth.contains("薄") || warmth.contains("夏") || warmth.contains("短"));
        switch (category) {
            case "外套":
                if (thick) return new int[]{-10, 10};
                return new int[]{8, 20};
            case "上衣":
                if (thick) return new int[]{0, 14};
                if (thin) return new int[]{24, 40};
                return new int[]{15, 27};
            case "裤子":
            case "裙子":
                if (thick) return new int[]{-10, 12};
                if (thin) return new int[]{24, 40};
                return new int[]{12, 28};
            case "鞋":
                if (thick) return new int[]{-10, 12};
                if (thin) return new int[]{22, 40};
                return new int[]{10, 30};
            default:
                return new int[]{10, 30};
        }
    }

    public static List<String> slots() {
        List<String> l = new ArrayList<>();
        l.add(SLOT_TOP);
        l.add(SLOT_BOTTOM);
        l.add(SLOT_OUTER);
        l.add(SLOT_SHOES);
        l.add(SLOT_ACC);
        return l;
    }
}
