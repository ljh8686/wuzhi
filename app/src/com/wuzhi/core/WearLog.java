package com.wuzhi.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一条穿着记录：某天穿了哪些衣服、当时气温多少。 */
public final class WearLog {

    public String id;
    public long date;
    /** 当天温度（摄氏） */
    public int temp;
    /** 天气，如 晴/阴/雨 */
    public String weather = "";
    /** 穿了哪些衣服（id） */
    public final List<String> clothIds = new ArrayList<>();
    /** 备注 */
    public String note = "";
    /** 心情/感受 1-5，0 表示未填 */
    public int feel;
    public long createdAt;

    public WearLog() {}

    public WearLog(String id, long date, int temp) {
        this.id = id;
        this.date = date;
        this.temp = temp;
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("date", date);
        m.put("temp", temp);
        m.put("weather", weather);
        List<Object> l = new ArrayList<>(clothIds);
        m.put("cloths", l);
        m.put("note", note);
        m.put("feel", feel);
        m.put("createdAt", createdAt);
        return m;
    }

    public static WearLog fromJson(Map<String, Object> m) {
        WearLog w = new WearLog();
        w.id = Json.str(m, "id", "");
        w.date = Json.lng(m, "date", DateUtil.today());
        w.temp = Json.integer(m, "temp", 20);
        w.weather = Json.str(m, "weather", "");
        w.clothIds.addAll(Json.strList(m, "cloths"));
        w.note = Json.str(m, "note", "");
        w.feel = Json.integer(m, "feel", 0);
        w.createdAt = Json.lng(m, "createdAt", 0);
        return w;
    }
}
