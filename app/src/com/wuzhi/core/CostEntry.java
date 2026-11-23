package com.wuzhi.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** One extra spend attached to an item: 美化 / 维修 / 配件 / 其他. */
public final class CostEntry {

    public static final String KIND_BEAUTY = "美化";
    public static final String KIND_REPAIR = "维修";
    public static final String KIND_ACCESSORY = "配件";
    public static final String KIND_OTHER = "其他";

    public static final String[] KINDS = {KIND_BEAUTY, KIND_REPAIR, KIND_ACCESSORY, KIND_OTHER};

    public String id;
    public String label;
    public long amountFen;
    public long date;
    public String kind;

    public CostEntry() {}

    public CostEntry(String id, String label, long amountFen, long date, String kind) {
        this.id = id;
        this.label = label;
        this.amountFen = amountFen;
        this.date = date;
        this.kind = kind == null ? KIND_OTHER : kind;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("amount", amountFen);
        m.put("date", date);
        m.put("kind", kind);
        return m;
    }

    public static CostEntry fromJson(Map<String, Object> m) {
        CostEntry c = new CostEntry();
        c.id = Json.str(m, "id", "");
        c.label = Json.str(m, "label", "");
        c.amountFen = Json.lng(m, "amount", 0);
        c.date = Json.lng(m, "date", DateUtil.today());
        c.kind = Json.str(m, "kind", KIND_OTHER);
        return c;
    }

    public String kindColorHint() {
        switch (kind) {
            case KIND_BEAUTY: return "美化";
            case KIND_REPAIR: return "维修";
            case KIND_ACCESSORY: return "配件";
            default: return "其他";
        }
    }
}
