package com.wuzhi.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一套搭配：一组衣服 + 适用温度 + 名称。 */
public final class Outfit {

    public String id;
    public String name;
    /** 各槽位选的衣物 id，key = slot */
    public final Map<String, String> slots = new LinkedHashMap<>();
    /** 适用温度下限/上限 */
    public int tempMin = 15;
    public int tempMax = 28;
    /** 是否收藏 */
    public boolean favorite;
    /** 被穿过多少次 */
    public int wearCount;
    public long lastWornDate;
    public String note = "";
    public long createdAt;

    public Outfit() {}

    public Outfit(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    public List<String> clothIds() {
        return new ArrayList<>(slots.values());
    }

    public boolean fits(int temp) {
        return temp >= tempMin && temp <= tempMax;
    }

    public int distanceTo(int temp) {
        if (temp < tempMin) return tempMin - temp;
        if (temp > tempMax) return temp - tempMax;
        return 0;
    }

    public String tempRangeText() {
        return tempMin + "~" + tempMax + "°C";
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("slots", new LinkedHashMap<>(slots));
        m.put("tempMin", tempMin);
        m.put("tempMax", tempMax);
        m.put("favorite", favorite);
        m.put("wearCount", wearCount);
        m.put("lastWornDate", lastWornDate);
        m.put("note", note);
        m.put("createdAt", createdAt);
        return m;
    }

    public static Outfit fromJson(Map<String, Object> m) {
        Outfit o = new Outfit();
        o.id = Json.str(m, "id", "");
        o.name = Json.str(m, "name", "");
        Map<String, Object> s = Json.obj(m.get("slots"));
        for (Map.Entry<String, Object> e : s.entrySet()) {
            if (e.getValue() instanceof String) o.slots.put(e.getKey(), (String) e.getValue());
        }
        o.tempMin = Json.integer(m, "tempMin", 15);
        o.tempMax = Json.integer(m, "tempMax", 28);
        o.favorite = Json.bool(m, "favorite", false);
        o.wearCount = Json.integer(m, "wearCount", 0);
        o.lastWornDate = Json.lng(m, "lastWornDate", 0);
        o.note = Json.str(m, "note", "");
        o.createdAt = Json.lng(m, "createdAt", 0);
        return o;
    }
}
