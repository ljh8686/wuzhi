import com.wuzhi.core.*;

import java.util.*;

public class CoreTest {
    static int pass = 0, fail = 0;

    static void check(String name, boolean ok, Object detail) {
        if (ok) { pass++; System.out.println("  PASS  " + name); }
        else { fail++; System.out.println("  FAIL  " + name + "  -> " + detail); }
    }

    static void eq(String name, Object a, Object b) {
        check(name, Objects.equals(String.valueOf(a), String.valueOf(b)), a + " != " + b);
    }

    public static void main(String[] args) {
        System.out.println("== Fmt ==");
        eq("yuan 12345", Fmt.yuan(12345), "123.45");
        eq("yuan 100", Fmt.yuan(100), "1.00");
        eq("yuan 5", Fmt.yuan(5), "0.05");
        eq("money 599900", Fmt.money(599900), "¥5,999.00");
        eq("moneyShort 123456789", Fmt.moneyShort(123456789L), "¥123.46万");
        eq("perDay 1234", Fmt.perDay(1234), "¥12.34");

        System.out.println("== DateUtil ==");
        long d = DateUtil.fromYmd(2024, 5, 1);
        eq("format back", DateUtil.format(d), "2024-05-01");
        eq("parse", DateUtil.parse("2024-05-01"), d);
        eq("parse slash", DateUtil.parse("2024/5/1"), d);
        eq("parse bad", DateUtil.parse("xx"), -1L);
        eq("weekday 2024-05-01", DateUtil.weekdayCn(d), "周三");
        eq("today>=2024", DateUtil.today() > d, true);
        eq("duration 400", DateUtil.duration(400), "1 年 1 个月");
        eq("duration 45", DateUtil.duration(45), "45 天");

        System.out.println("== Item cost ==");
        long today = DateUtil.fromYmd(2025, 6, 1);
        Item it = new Item("i1", "手机", 500000, DateUtil.fromYmd(2024, 1, 1));
        // 500000 分 = 5000 元，用了 517 天
        long used = today - it.buyDate;
        eq("usedDays", it.usedDays(today), used);
        double daily = it.dailyFen(today);
        check("daily ~ 500000/used", Math.abs(daily - 500000.0 / used) < 0.001, daily);
        it.extras.add(new CostEntry("e1", "壳", 10000, DateUtil.fromYmd(2024, 6, 1), CostEntry.KIND_ACCESSORY));
        eq("extras total", it.extrasFen(), 10000L);
        eq("total", it.totalFen(), 510000L);
        eq("net no salvage", it.netFen(), 510000L);
        it.salvageFen = 100000;
        eq("net with salvage", it.netFen(), 410000L);
        it.salvageFen = 0;

        // 报废后日均锁定
        Item dead = new Item("i2", "耳机", 100000, DateUtil.fromYmd(2024, 1, 1));
        dead.status = Item.STATUS_RETIRED;
        dead.endDate = DateUtil.fromYmd(2024, 1, 11); // 10 天
        eq("retired usedDays", dead.usedDays(today), 10L);
        eq("retired daily", Math.round(dead.dailyFen(today)), 10000L);
        check("retired isEnded", dead.isEnded(), "not ended");

        System.out.println("== cost timeline ==");
        Item tl = new Item("i3", "键盘", 10000, DateUtil.fromYmd(2024, 1, 1));
        tl.extras.add(new CostEntry("a", "键帽", 5000, DateUtil.fromYmd(2024, 3, 1), CostEntry.KIND_BEAUTY));
        tl.extras.add(new CostEntry("b", "换轴", 2000, DateUtil.fromYmd(2024, 2, 1), CostEntry.KIND_REPAIR));
        List<long[]> pts = tl.costTimeline();
        eq("timeline size", pts.size(), 3);
        eq("timeline sorted 1", pts.get(1)[1], 12000L); // 2月 10000+2000
        eq("timeline sorted 2", pts.get(2)[1], 17000L);

        System.out.println("== Json roundtrip ==");
        Store st = new Store();
        st.seedDemo(today);
        String json = st.toJsonString();
        Store st2 = Store.fromJsonString(json);
        eq("items roundtrip", st2.items.size(), st.items.size());
        eq("cloths roundtrip", st2.cloths.size(), st.cloths.size());
        eq("logs roundtrip", st2.logs.size(), st.logs.size());
        eq("outfits roundtrip", st2.outfits.size(), st.outfits.size());
        eq("item name", st2.items.get(0).name, st.items.get(0).name);
        eq("extras roundtrip", st2.items.get(0).extras.size(), st.items.get(0).extras.size());
        eq("cloth wearCount rebuilt", st2.cloths.get(0).wearCount > 0, true);
        eq("json pretty parses", Json.obj(Json.parse(Json.writePretty(st.toJson()))).size() > 0, true);

        // unicode + escaping
        Item u = st.addItem("测试\"引号\"\\反斜杠\n换行 中文😀", 100, today, "其他", "🧪");
        Store st3 = Store.fromJsonString(st.toJsonString());
        Item u2 = st3.itemById(u.id);
        eq("unicode roundtrip", u2 != null ? u2.name : null, u.name);

        System.out.println("== Stats ==");
        long invested = Stats.totalInvested(st.items);
        check("totalInvested > 0", invested > 0, invested);
        Map<String, Long> cat = Stats.byCategory(st.items);
        check("categories", cat.size() >= 3, cat);
        long sumCat = 0;
        for (long v : cat.values()) sumCat += v;
        eq("category sum == total", sumCat, invested);
        Map<String, Long> kinds = Stats.extrasByKind(st.items);
        check("extras kinds", kinds.containsKey(CostEntry.KIND_BEAUTY) || kinds.containsKey(CostEntry.KIND_REPAIR), kinds);
        List<Item> top = Stats.mostExpensivePerDay(st.items, today, 3);
        check("top<=3", top.size() <= 3 && top.size() > 0, top.size());
        if (top.size() >= 2) {
            check("top sorted", top.get(0).dailyFen(today) >= top.get(1).dailyFen(today), "unsorted");
        }

        System.out.println("== Recommender ==");
        Map<String, Integer> wc = st.wearCounts();
        List<Recommender.Scored> ranked = Recommender.rankAll(st.activeCloths(), 20, today, wc);
        check("ranked non-empty", !ranked.isEmpty(), ranked.size());
        for (int i = 1; i < ranked.size(); i++) {
            if (ranked.get(i - 1).score < ranked.get(i).score) {
                check("rank desc", false, "at " + i);
                break;
            }
        }
        check("rank desc ok", true, "");

        // 20°C 应该优先推荐 15-27 的衬衫而不是 26-40 的短裤
        Cloth top1 = ranked.get(0).cloth;
        check("20C top pick is warm-appropriate", top1.fits(20) || top1.distanceTo(20) <= 2,
                top1.name + " " + top1.tempRangeText());

        List<Recommender.Suggestion> sug = Recommender.suggest(st.cloths, st.outfits, 20, today, wc, 5);
        check("suggestions non-empty", !sug.isEmpty(), sug.size());
        Recommender.Suggestion s0 = sug.get(0);
        check("suggestion has clothes", !s0.cloths.isEmpty(), s0.title());
        System.out.println("     top suggestion: " + s0.title() + " | " + s0.reasonText());

        Recommender.Suggestion auto = Recommender.autoCompose(st.cloths, 30, today, wc);
        check("auto at 30C", auto != null && !auto.cloths.isEmpty(), "null");
        if (auto != null) {
            boolean hasCoat = false;
            for (Cloth c : auto.cloths) if (Cloth.SLOT_OUTER.equals(c.slot)) hasCoat = true;
            check("no coat at 30C", !hasCoat, "coat picked at 30C");
            System.out.println("     30C auto: " + auto.title());
        }
        Recommender.Suggestion autoCold = Recommender.autoCompose(st.cloths, 0, today, wc);
        check("auto at 0C has coat", autoCold != null, "null");
        if (autoCold != null) {
            boolean hasCoat = false;
            for (Cloth c : autoCold.cloths) if (Cloth.SLOT_OUTER.equals(c.slot)) hasCoat = true;
            check("coat at 0C", hasCoat, "no coat at 0C: " + autoCold.title());
            System.out.println("     0C auto: " + autoCold.title());
        }

        System.out.println("== similar temp logs ==");
        List<WearLog> sim = Recommender.logsAtSimilarTemp(st.logs, 21, 2);
        check("similar logs found", sim.size() >= 1, sim.size());
        List<Cloth> mw = Recommender.mostWornAtTemp(st.logs, st.cloths, 21, 2);
        check("mostWorn non-empty", !mw.isEmpty(), mw.size());
        if (!mw.isEmpty()) System.out.println("     most worn at 21C: " + mw.get(0).name);

        System.out.println("== remove cascades ==");
        String cid = st.cloths.get(0).id;
        st.removeCloth(cid);
        eq("cloth gone", st.clothById(cid), null);
        boolean anyRef = false;
        for (Outfit o : st.outfits) if (o.slots.containsValue(cid)) anyRef = true;
        check("outfit refs cleaned", !anyRef, "dangling ref");
        boolean inLog = false;
        for (WearLog w : st.logs) if (w.clothIds.contains(cid)) inLog = true;
        check("log refs cleaned", !inLog, "dangling log ref");

        System.out.println("== empty store ==");
        Store empty = new Store();
        eq("empty json ok", Store.fromJsonString("").items.size(), 0);
        eq("empty null ok", Store.fromJsonString(null).cloths.size(), 0);
        eq("empty daily", Stats.dailyTotal(empty.items, today), 0.0);
        check("empty suggest", Recommender.suggest(empty.cloths, empty.outfits, 20, today, new HashMap<>(), 5).isEmpty(), "not empty");

        System.out.println();
        System.out.println("RESULT: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
