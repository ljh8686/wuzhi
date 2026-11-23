package com.wuzhi.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 全部数据的容器 + 增删改查 + JSON 序列化。
 * 不依赖 Android，便于单元测试。
 */
public final class Store {

    public static final int VERSION = 1;

    public final List<Item> items = new ArrayList<>();
    public final List<Cloth> cloths = new ArrayList<>();
    public final List<Outfit> outfits = new ArrayList<>();
    public final List<WearLog> logs = new ArrayList<>();

    /** 默认温度容差（推荐"相同温度"时 ±几度） */
    public int tempTolerance = 2;
    /** 用户所在城市（仅用于显示） */
    public String city = "";
    /** 上次同步天气的温度，999 表示无 */
    public int lastWeatherTemp = Cloth.NO_TEMP;
    public long lastWeatherAt;

    // ---------------------------------------------------------------- ids

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    // ---------------------------------------------------------------- items

    public Item itemById(String id) {
        for (Item i : items) if (i.id.equals(id)) return i;
        return null;
    }

    public Item addItem(String name, long priceFen, long buyDate, String category, String emoji) {
        Item it = new Item(newId(), name, priceFen, buyDate);
        it.category = category == null ? "其他" : category;
        it.emoji = emoji == null ? "" : emoji;
        items.add(it);
        return it;
    }

    public void removeItem(String id) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).id.equals(id)) items.remove(i);
        }
    }

    public CostEntry addExtra(String itemId, String label, long amountFen, long date, String kind) {
        Item it = itemById(itemId);
        if (it == null) return null;
        CostEntry c = new CostEntry(newId(), label, amountFen, date, kind);
        it.extras.add(c);
        return c;
    }

    public void removeExtra(String itemId, String extraId) {
        Item it = itemById(itemId);
        if (it == null) return;
        for (int i = it.extras.size() - 1; i >= 0; i--) {
            if (it.extras.get(i).id.equals(extraId)) it.extras.remove(i);
        }
    }

    /** 排序：置顶优先，然后按日均成本从高到低（烧钱榜）。 */
    public List<Item> sortedItems(long today) {
        List<Item> l = new ArrayList<>(items);
        l.sort((a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            boolean ae = a.isEnded(), be = b.isEnded();
            if (ae != be) return ae ? 1 : -1;
            return Double.compare(b.dailyFen(today), a.dailyFen(today));
        });
        return l;
    }

    // ---------------------------------------------------------------- cloths

    public Cloth clothById(String id) {
        for (Cloth c : cloths) if (c.id.equals(id)) return c;
        return null;
    }

    public Cloth addCloth(String name, String category, long priceFen, long buyDate) {
        Cloth c = new Cloth(newId(), name, category, Cloth.slotForCategory(category), priceFen, buyDate);
        int[] r = Cloth.defaultTempRange(category, "");
        c.tempMin = r[0];
        c.tempMax = r[1];
        cloths.add(c);
        return c;
    }

    public void removeCloth(String id) {
        for (int i = cloths.size() - 1; i >= 0; i--) {
            if (cloths.get(i).id.equals(id)) cloths.remove(i);
        }
        for (Outfit o : outfits) {
            for (String k : new ArrayList<>(o.slots.keySet())) {
                if (id.equals(o.slots.get(k))) o.slots.remove(k);
            }
        }
        for (WearLog w : logs) w.clothIds.remove(id);
    }

    public List<Cloth> activeCloths() {
        List<Cloth> l = new ArrayList<>();
        for (Cloth c : cloths) if (!c.retired) l.add(c);
        return l;
    }

    // ---------------------------------------------------------------- logs

    public WearLog addLog(long date, int temp, List<String> clothIds, String weather, String note, int feel) {
        WearLog w = new WearLog(newId(), date, temp);
        w.weather = weather == null ? "" : weather;
        if (clothIds != null) w.clothIds.addAll(clothIds);
        w.note = note == null ? "" : note;
        w.feel = feel;
        logs.add(w);
        rebuildClothStats();
        return w;
    }

    public void removeLog(String id) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            if (logs.get(i).id.equals(id)) logs.remove(i);
        }
        rebuildClothStats();
    }

    /** 从穿着记录重算每件衣服的次数 / 上次穿着日期 / 上次温度。 */
    public void rebuildClothStats() {
        Map<String, Integer> cnt = new HashMap<>();
        Map<String, Long> lastDate = new HashMap<>();
        Map<String, Integer> lastTemp = new HashMap<>();
        List<WearLog> sorted = new ArrayList<>(logs);
        sorted.sort((a, b) -> Long.compare(a.date, b.date));
        for (WearLog w : sorted) {
            for (String id : w.clothIds) {
                cnt.put(id, cnt.containsKey(id) ? cnt.get(id) + 1 : 1);
                lastDate.put(id, w.date);
                lastTemp.put(id, w.temp);
            }
        }
        for (Cloth c : cloths) {
            c.wearCount = cnt.containsKey(c.id) ? cnt.get(c.id) : 0;
            c.lastWornDate = lastDate.containsKey(c.id) ? lastDate.get(c.id) : 0;
            c.lastWornTemp = lastTemp.containsKey(c.id) ? lastTemp.get(c.id) : Cloth.NO_TEMP;
        }
        // 搭配的穿着次数也重算
        for (Outfit o : outfits) {
            int n = 0;
            long last = 0;
            for (WearLog w : logs) {
                boolean match = !o.slots.isEmpty();
                for (String id : o.clothIds()) {
                    if (!w.clothIds.contains(id)) { match = false; break; }
                }
                if (match) { n++; if (w.date > last) last = w.date; }
            }
            o.wearCount = n;
            o.lastWornDate = last;
        }
    }

    public Map<String, Integer> wearCounts() {
        Map<String, Integer> m = new HashMap<>();
        for (Cloth c : cloths) m.put(c.id, c.wearCount);
        return m;
    }

    // ---------------------------------------------------------------- outfits

    public Outfit outfitById(String id) {
        for (Outfit o : outfits) if (o.id.equals(id)) return o;
        return null;
    }

    public Outfit addOutfit(String name, Map<String, String> slots, int tempMin, int tempMax) {
        Outfit o = new Outfit(newId(), name);
        o.slots.putAll(slots);
        o.tempMin = tempMin;
        o.tempMax = tempMax;
        outfits.add(o);
        return o;
    }

    public void removeOutfit(String id) {
        for (int i = outfits.size() - 1; i >= 0; i--) {
            if (outfits.get(i).id.equals(id)) outfits.remove(i);
        }
    }

    // ---------------------------------------------------------------- json

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", VERSION);
        m.put("tempTolerance", tempTolerance);
        m.put("city", city);
        m.put("lastWeatherTemp", lastWeatherTemp);
        m.put("lastWeatherAt", lastWeatherAt);
        List<Object> it = new ArrayList<>();
        for (Item i : items) it.add(i.toJson());
        m.put("items", it);
        List<Object> cl = new ArrayList<>();
        for (Cloth c : cloths) cl.add(c.toJson());
        m.put("cloths", cl);
        List<Object> of = new ArrayList<>();
        for (Outfit o : outfits) of.add(o.toJson());
        m.put("outfits", of);
        List<Object> lg = new ArrayList<>();
        for (WearLog w : logs) lg.add(w.toJson());
        m.put("logs", lg);
        return m;
    }

    public String toJsonString() {
        return Json.write(toJson());
    }

    public static Store fromJsonString(String s) {
        Store st = new Store();
        if (s == null || s.trim().isEmpty()) return st;
        Map<String, Object> m = Json.obj(Json.parse(s));
        st.tempTolerance = Json.integer(m, "tempTolerance", 2);
        st.city = Json.str(m, "city", "");
        st.lastWeatherTemp = Json.integer(m, "lastWeatherTemp", Cloth.NO_TEMP);
        st.lastWeatherAt = Json.lng(m, "lastWeatherAt", 0);
        for (Object o : Json.arr(m.get("items"))) st.items.add(Item.fromJson(Json.obj(o)));
        for (Object o : Json.arr(m.get("cloths"))) st.cloths.add(Cloth.fromJson(Json.obj(o)));
        for (Object o : Json.arr(m.get("outfits"))) st.outfits.add(Outfit.fromJson(Json.obj(o)));
        for (Object o : Json.arr(m.get("logs"))) st.logs.add(WearLog.fromJson(Json.obj(o)));
        st.rebuildClothStats();
        return st;
    }

    // ---------------------------------------------------------------- demo

    /** 首次打开时塞入示例数据，让界面不空。 */
    public void seedDemo(long today) {
        if (!items.isEmpty() || !cloths.isEmpty()) return;

        Item phone = addItem("手机", 599900, today - 420, "数码", "📱");
        phone.note = "主力机，天天用";
        phone.extras.add(new CostEntry(newId(), "换电池", 19900, today - 90, CostEntry.KIND_REPAIR));
        phone.extras.add(new CostEntry(newId(), "手机壳 + 膜", 6800, today - 400, CostEntry.KIND_ACCESSORY));

        Item shoes = addItem("跑步鞋", 89900, today - 200, "鞋包", "👟");
        shoes.extras.add(new CostEntry(newId(), "换鞋垫", 4900, today - 30, CostEntry.KIND_REPAIR));

        Item keyboard = addItem("机械键盘", 46900, today - 300, "数码", "⌨️");
        keyboard.extras.add(new CostEntry(newId(), "换键帽（美化）", 12800, today - 60, CostEntry.KIND_BEAUTY));

        Item desk = addItem("书桌", 79900, today - 500, "家具", "🪑");
        Item headphones = addItem("降噪耳机", 129900, today - 150, "数码", "🎧");
        headphones.status = Item.STATUS_RETIRED;
        headphones.endDate = today - 20;
        headphones.extras.add(new CostEntry(newId(), "换耳罩", 5900, today - 80, CostEntry.KIND_REPAIR));

        Cloth tee = addCloth("白色纯棉T恤", "上衣", 9900, today - 120);
        tee.tempMin = 24; tee.tempMax = 36; tee.warmth = "薄款"; tee.color = "白"; tee.favorite = true;
        Cloth shirt = addCloth("蓝色条纹衬衫", "上衣", 19900, today - 200);
        shirt.tempMin = 16; shirt.tempMax = 27; shirt.warmth = "薄款"; shirt.color = "蓝";
        Cloth hoodie = addCloth("灰色卫衣", "上衣", 25900, today - 260);
        hoodie.tempMin = 8; hoodie.tempMax = 20; hoodie.warmth = "厚款"; hoodie.color = "灰";
        Cloth coat = addCloth("黑色羽绒服", "外套", 89900, today - 400);
        coat.tempMin = -10; coat.tempMax = 8; coat.warmth = "加厚羽绒"; coat.color = "黑";
        Cloth jeans = addCloth("直筒牛仔裤", "裤子", 29900, today - 300);
        jeans.tempMin = 5; jeans.tempMax = 26; jeans.color = "深蓝";
        Cloth shorts = addCloth("运动短裤", "裤子", 8900, today - 100);
        shorts.tempMin = 26; shorts.tempMax = 40; shorts.warmth = "薄款";
        Cloth sneakers = addCloth("白色板鞋", "鞋", 39900, today - 180);
        sneakers.tempMin = 12; sneakers.tempMax = 32; sneakers.color = "白";

        Outfit o1 = addOutfit("秋日通勤", slotsOf(shirt, jeans, sneakers), 16, 24);
        o1.favorite = true;
        addOutfit("夏日轻松", slotsOf(tee, shorts, sneakers), 26, 36);
        addOutfit("深冬保暖", slotsOf(hoodie, jeans, coat, sneakers), -5, 10);

        addLog(today - 2, 21, idsOf(shirt, jeans, sneakers), "晴", "刚刚好", 5);
        addLog(today - 5, 19, idsOf(hoodie, jeans, sneakers), "阴", "", 4);
        addLog(today - 9, 28, idsOf(tee, shorts, sneakers), "晴", "有点热", 3);
        addLog(today - 14, 22, idsOf(shirt, jeans, sneakers), "多云", "", 4);
        addLog(today - 20, 15, idsOf(hoodie, jeans, sneakers), "小雨", "略冷", 3);
        addLog(today - 33, 26, idsOf(tee, shorts, sneakers), "晴", "", 4);
        addLog(today - 41, 20, idsOf(shirt, jeans, sneakers), "晴", "", 5);
    }

    private static Map<String, String> slotsOf(Cloth... cs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (Cloth c : cs) m.put(c.slot, c.id);
        return m;
    }

    private static List<String> idsOf(Cloth... cs) {
        List<String> l = new ArrayList<>();
        for (Cloth c : cs) l.add(c.id);
        return l;
    }
}
