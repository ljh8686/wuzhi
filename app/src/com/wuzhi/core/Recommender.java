package com.wuzhi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐引擎：
 * 1) 按当前温度给每件衣服打分（温度匹配 + 上次穿的温度接近 + 冷落天数 + 喜爱 + 单次成本）
 * 2) 按槽位选最佳，组成一套搭配
 * 3) 已有的 saved outfit 中挑温度最合适的优先展示
 */
public final class Recommender {

    private Recommender() {}

    /** 一件衣服的推荐结果。 */
    public static final class Scored {
        public final Cloth cloth;
        public final double score;
        public final List<String> reasons = new ArrayList<>();

        Scored(Cloth c, double s) { this.cloth = c; this.score = s; }

        Scored(Cloth c, double s, List<String> why) {
            this.cloth = c;
            this.score = s;
            this.reasons.addAll(why);
        }

        public String reasonText() {
            if (reasons.isEmpty()) return "";
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < reasons.size(); i++) {
                if (i > 0) b.append(" · ");
                b.append(reasons.get(i));
            }
            return b.toString();
        }
    }

    /** 一套推荐搭配。 */
    public static final class Suggestion {
        public final Outfit outfit;               // 可能是临时组合（id 为空）
        public final List<Cloth> cloths = new ArrayList<>();
        public final double score;
        public final List<String> reasons = new ArrayList<>();
        /** 这套里所有单品共同覆盖的温度区间；无效时等于 NO_TEMP */
        public int fitMin = Cloth.NO_TEMP, fitMax = Cloth.NO_TEMP;
        /** 当前温度下，最不合身那件差了多少度；0 表示每件都合身 */
        public int minGap = 0;

        Suggestion(Outfit o, double s) { this.outfit = o; this.score = s; }

        public String title() {
            if (outfit != null && outfit.name != null && !outfit.name.isEmpty()) return outfit.name;
            StringBuilder b = new StringBuilder();
            for (Cloth c : cloths) {
                if (b.length() > 0) b.append(" + ");
                b.append(c.name);
            }
            return b.length() == 0 ? "暂无搭配" : b.toString();
        }

        public String reasonText() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < reasons.size(); i++) {
                if (i > 0) b.append(" · ");
                b.append(reasons.get(i));
            }
            return b.toString();
        }
    }

    /**
     * 给单件衣服打分。
     *
     * @param temp      目标温度
     * @param today     今天
     * @param wearCount 该衣服历史穿着次数（可从记录统计）
     */
    public static Scored scoreCloth(Cloth c, int temp, long today, int wearCount) {
        double s = 0;
        Scored sc = new Scored(c, 0);

        int dist = c.distanceTo(temp);
        if (dist == 0) {
            s += 60;
            sc.reasons.add("温度正好");
        } else {
            // 不合适的衣服要真的排在后面：允许负分，避免"都不合适时随便挑一件"
            s += Math.max(-45, 45 - dist * 7.5);
            if (dist <= 2) sc.reasons.add("温度接近");
            else if (dist >= 10) sc.reasons.add("温差 " + dist + "°C");
        }

        // 上次穿的时候温度接近当前温度 → 说明这件在类似天气穿过，靠谱
        if (c.lastWornTemp != Cloth.NO_TEMP) {
            int td = Math.abs(c.lastWornTemp - temp);
            if (td <= 2) {
                s += 22;
                sc.reasons.add("上次 " + c.lastWornTemp + "°C 穿的就是它");
            } else if (td <= 5) {
                s += 10;
                sc.reasons.add("上次穿的天气相近");
            }
        }

        // 冷落惩罚：越久没穿越优先（换着穿）
        if (c.lastWornDate > 0) {
            long gap = today - c.lastWornDate;
            if (gap >= 14) {
                s += Math.min(16, 6 + gap / 10.0);
                sc.reasons.add("已经 " + gap + " 天没穿");
            } else if (gap <= 2) {
                s -= 12;
                sc.reasons.add("刚穿过");
            } else {
                s += Math.min(8, gap);
            }
        } else {
            s += 14;
            sc.reasons.add("还没穿过");
        }

        if (c.favorite) {
            s += 9;
            sc.reasons.add("常穿款");
        }

        // 单次成本高 → 鼓励多穿（但别盖过温度匹配）
        if (wearCount > 0 && c.priceFen > 0) {
            double cpw = (double) c.priceFen / wearCount;
            if (cpw > 3000) { // >30 元/次
                s += 8;
                sc.reasons.add("单次成本偏高，值得多穿");
            }
        }

        return new Scored(c, s, sc.reasons);
    }

    /** 按温度给某槽位排序。 */
    public static List<Scored> rankBySlot(List<Cloth> cloths, String slot, int temp, long today,
                                          Map<String, Integer> wearCounts) {
        List<Scored> out = new ArrayList<>();
        for (Cloth c : cloths) {
            if (c.retired) continue;
            if (slot != null && !slot.equals(c.slot)) continue;
            int wc = wearCounts != null && wearCounts.containsKey(c.id) ? wearCounts.get(c.id) : c.wearCount;
            out.add(scoreCloth(c, temp, today, wc));
        }
        out.sort((a, b) -> Double.compare(b.score, a.score));
        return out;
    }

    /** 按温度给全部衣服排序（不分槽位）。 */
    public static List<Scored> rankAll(List<Cloth> cloths, int temp, long today, Map<String, Integer> wearCounts) {
        return rankBySlot(cloths, null, temp, today, wearCounts);
    }

    /**
     * 生成搭配建议。
     * 先看保存过的搭配（温度最接近的），再补一套自动组合的。
     */
    public static List<Suggestion> suggest(List<Cloth> cloths, List<Outfit> outfits, int temp, long today,
                                           Map<String, Integer> wearCounts, int limit) {
        List<Suggestion> out = new ArrayList<>();
        Map<String, Cloth> byId = new HashMap<>();
        for (Cloth c : cloths) byId.put(c.id, c);

        // 1) 已保存的搭配
        List<Suggestion> saved = new ArrayList<>();
        for (Outfit o : outfits) {
            List<Cloth> cs = new ArrayList<>();
            boolean ok = true;
            for (String id : o.clothIds()) {
                Cloth c = byId.get(id);
                if (c == null || c.retired) { ok = false; break; }
                cs.add(c);
            }
            if (!ok || cs.isEmpty()) continue;
            int gap = 0, fmin = Integer.MIN_VALUE, fmax = Integer.MAX_VALUE, outCount = 0;
            for (Cloth c : cs) {
                gap = Math.max(gap, c.distanceTo(temp));
                if (!c.fits(temp)) outCount++;
                fmin = Math.max(fmin, c.tempMin);
                fmax = Math.min(fmax, c.tempMax);
            }
            double s = 70 - gap * 8;
            if (gap == 0) s += 15;
            if (o.favorite) s += 8;
            if (o.lastWornDate > 0 && today - o.lastWornDate <= 2) s -= 10;
            Suggestion sg = new Suggestion(o, s);
            sg.cloths.addAll(cs);
            sg.minGap = gap;
            sg.fitMin = fmin; sg.fitMax = fmax;
            if (gap == 0) sg.reasons.add("每件都适合 " + fmin + "~" + fmax + "°C");
            else sg.reasons.add(cs.size() + " 件里有 " + outCount + " 件不在这个温度的舒适区");
            if (o.favorite) sg.reasons.add("收藏搭配");
            saved.add(sg);
        }
        saved.sort((a, b) -> Double.compare(b.score, a.score));
        out.addAll(saved);

        // 2) 自动组合
        Suggestion auto = autoCompose(cloths, temp, today, wearCounts);
        if (auto != null) out.add(auto);

        if (out.size() > limit) return new ArrayList<>(out.subList(0, limit));
        return out;
    }

    /** 自动从各槽位挑最优，拼一套。 */
    public static Suggestion autoCompose(List<Cloth> cloths, int temp, long today, Map<String, Integer> wearCounts) {
        String[] order = {Cloth.SLOT_TOP, Cloth.SLOT_BOTTOM, Cloth.SLOT_OUTER, Cloth.SLOT_SHOES, Cloth.SLOT_ACC};
        List<Cloth> picked = new ArrayList<>();
        double total = 0;
        List<String> reasons = new ArrayList<>();
        for (String slot : order) {
            List<Scored> ranked = rankBySlot(cloths, slot, temp, today, wearCounts);
            if (ranked.isEmpty()) continue;
            Scored best = ranked.get(0);
            // 温差太大就别硬凑（比如 30 度推荐羽绒服）
            if (best.cloth.distanceTo(temp) > 12) continue;
            // 外套只在偏冷时加
            if (Cloth.SLOT_OUTER.equals(slot)) {
                if (temp >= 24) continue;
                if (temp >= 20 && !best.cloth.fits(temp)) continue;
            }
            picked.add(best.cloth);
            total += best.score;
        }
        if (picked.isEmpty()) return null;
        total = total / picked.size();
        Outfit o = new Outfit("", "");
        o.tempMin = temp;
        o.tempMax = temp;
        Suggestion s = new Suggestion(o, total);
        s.cloths.addAll(picked);
        int gap = 0, fmin = Integer.MIN_VALUE, fmax = Integer.MAX_VALUE;
        for (Cloth c : picked) { gap = Math.max(gap, c.distanceTo(temp)); fmin = Math.max(fmin, c.tempMin); fmax = Math.min(fmax, c.tempMax); }
        s.minGap = gap; s.fitMin = fmin; s.fitMax = fmax;
        if (temp <= 8) s.reasons.add("天冷，注意保暖");
        else if (temp >= 30) s.reasons.add("天热，选透气款");
        else s.reasons.add(temp + "°C 适合这样穿");
        return s;
    }

    /** 找出历史上同温度（±2°C）穿过的搭配记录，用于"相同温度推荐"。 */
    public static List<WearLog> logsAtSimilarTemp(List<WearLog> logs, int temp, int tolerance) {
        List<WearLog> out = new ArrayList<>();
        for (WearLog w : logs) {
            if (Math.abs(w.temp - temp) <= tolerance) out.add(w);
        }
        out.sort((a, b) -> Long.compare(b.date, a.date));
        return out;
    }

    /**
     * 从历史记录里统计：这个温度下最常穿的是哪些衣服（按出现次数）。
     */
    public static List<Cloth> mostWornAtTemp(List<WearLog> logs, List<Cloth> cloths, int temp, int tolerance) {
        Map<String, Integer> cnt = new HashMap<>();
        for (WearLog w : logs) {
            if (Math.abs(w.temp - temp) > tolerance) continue;
            for (String id : w.clothIds) {
                cnt.put(id, cnt.containsKey(id) ? cnt.get(id) + 1 : 1);
            }
        }
        Map<String, Cloth> byId = new HashMap<>();
        for (Cloth c : cloths) byId.put(c.id, c);
        List<Cloth> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cnt.entrySet()) {
            Cloth c = byId.get(e.getKey());
            if (c != null && !c.retired) out.add(c);
        }
        out.sort((a, b) -> Integer.compare(cnt.get(b.id), cnt.get(a.id)));
        return out;
    }
}
