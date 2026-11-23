package com.wuzhi.ui;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wuzhi.core.Cloth;
import com.wuzhi.core.DateUtil;
import com.wuzhi.core.Fmt;
import com.wuzhi.core.Item;
import com.wuzhi.core.Stats;
import com.wuzhi.core.WearLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 统计页：投入结构、烧钱榜、衣柜价值、冷落提醒。 */
public class StatsPage {

    private final MainActivity a;

    public StatsPage(MainActivity a) { this.a = a; }

    public View build() {
        LinearLayout page = MainActivity.page(a);
        long today = a.today();
        List<Item> items = a.store().items;
        List<Cloth> cloths = a.store().cloths;

        LinearLayout head = Ui.hbox(a);
        LinearLayout left = Ui.vbox(a);
        left.addView(Ui.title(a, "统计", 26, Ui.INK));
        TextView sub = Ui.text(a, "钱花得值不值，数据说了算", 12.5f, Ui.MUTED);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        left.addView(sub);
        head.addView(left, Ui.lpw(1));
        LinearLayout set = Ui.iconButton(a, Glyph.GEAR, "设置", 0xFFF0EAE0, Ui.INK2);
        set.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { new SettingsPage(a).open(); }
        });
        head.addView(set);
        page.addView(head);
        page.addView(Ui.spacer(a, 14));

        // 关键指标
        page.addView(metrics(items, cloths, today));
        page.addView(Ui.spacer(a, 12));

        // 分类投入柱状图
        Map<String, Long> cat = Stats.byCategory(items);
        if (!cat.isEmpty()) {
            page.addView(categoryChart(cat));
            page.addView(Ui.spacer(a, 12));
        }

        // 烧钱榜
        List<Item> top = Stats.mostExpensivePerDay(items, today, 5);
        if (!top.isEmpty()) {
            page.addView(rankCard("每天最烧钱的", top, today));
            page.addView(Ui.spacer(a, 12));
        }

        // 附加花销结构
        Map<String, Long> kinds = Stats.extrasByKind(items);
        if (!kinds.isEmpty()) {
            page.addView(extrasCard(kinds));
            page.addView(Ui.spacer(a, 12));
        }

        // 衣柜
        if (!cloths.isEmpty()) {
            page.addView(wardrobeCard(cloths, today));
            page.addView(Ui.spacer(a, 12));
            List<Cloth> worst = Stats.worstValue(cloths, 3);
            if (!worst.isEmpty()) {
                page.addView(worstCard(worst, today));
                page.addView(Ui.spacer(a, 12));
            }
        }

        // 每月花费
        List<long[]> monthly = Stats.monthlySpend(items);
        if (!monthly.isEmpty()) {
            page.addView(monthlyCard(monthly));
        }

        return MainActivity.scroller(a, page);
    }

    private View metrics(List<Item> items, List<Cloth> cloths, long today) {
        LinearLayout wrap = Ui.vbox(a);
        LinearLayout row1 = Ui.hbox(a);
        row1.addView(metric("总投入", Fmt.moneyShort(Stats.totalInvested(items)), "全部东西加起来", Ui.ACCENT, Ui.ACCENT_SOFT), weight(1));
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        l2.weight = 1;
        l2.leftMargin = Ui.dp(a, 10);
        row1.addView(metric("每天花", Fmt.perDay(Stats.dailyTotal(items, today)), "在用物品日均合计", Ui.BLUE, Ui.BLUE_SOFT), l2);
        wrap.addView(row1);
        wrap.addView(Ui.spacer(a, 10));

        LinearLayout row2 = Ui.hbox(a);
        row2.addView(metric("衣柜价值", Fmt.moneyShort(Stats.wardrobeValue(cloths)), cloths.size() + " 件衣服", Ui.GREEN, Ui.GREEN_SOFT), weight(1));
        double avg = Stats.avgCostPerWear(cloths);
        LinearLayout.LayoutParams l4 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        l4.weight = 1;
        l4.leftMargin = Ui.dp(a, 10);
        row2.addView(metric("平均每次穿", avg == 0 ? "—" : Fmt.perDay(avg), "衣服摊到每次", Ui.AMBER, Ui.AMBER_SOFT), l4);
        wrap.addView(row2);
        return wrap;
    }

    private LinearLayout.LayoutParams weight(float w) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = w;
        return lp;
    }

    private View metric(String label, String value, String hint, int color, int soft) {
        LinearLayout c = Ui.card(a);
        c.setPadding(Ui.dp(a, 15), Ui.dp(a, 15), Ui.dp(a, 15), Ui.dp(a, 15));
        TextView v = Ui.number(a, value, 21, color);
        c.addView(v);
        TextView k = Ui.text(a, label, 11.5f, Ui.INK2);
        k.setPadding(0, Ui.dp(a, 5), 0, 0);
        c.addView(k);
        TextView h = Ui.text(a, hint, 10, Ui.MUTED);
        h.setPadding(0, Ui.dp(a, 2), 0, 0);
        c.addView(h);
        return c;
    }

    private View categoryChart(Map<String, Long> cat) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.title(a, "钱都花在哪些类别", 14, Ui.INK));
        card.addView(Ui.spacer(a, 12));

        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<Map.Entry<String, Long>> list = new ArrayList<>(cat.entrySet());
        list.sort((x, y) -> Long.compare(y.getValue(), x.getValue()));
        long total = 0;
        for (Map.Entry<String, Long> e : list) total += e.getValue();
        for (int i = 0; i < list.size() && i < 6; i++) {
            labels.add(list.get(i).getKey());
            values.add(list.get(i).getValue() / 100f);
            colors.add(Ui.colorFor(list.get(i).getKey()));
        }
        ChartView ch = new ChartView(a);
        ch.bars(labels, values, colors, "元");
        card.addView(ch, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(a, 150)));

        card.addView(Ui.spacer(a, 10));
        // 明细
        for (int i = 0; i < list.size() && i < 6; i++) {
            Map.Entry<String, Long> e = list.get(i);
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 4));
            View dot = new View(a);
            dot.setBackground(Ui.round(Ui.colorFor(e.getKey()), 5, a));
            dot.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 9), Ui.dp(a, 9)));
            r.addView(dot);
            TextView k = Ui.text(a, e.getKey(), 12.5f, Ui.INK2);
            k.setPadding(Ui.dp(a, 8), 0, 0, 0);
            r.addView(k, Ui.lpw(1));
            TextView pct = Ui.text(a, Fmt.pct(total == 0 ? 0 : (double) e.getValue() / total), 11, Ui.MUTED);
            pct.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 10), 0);
            r.addView(pct);
            r.addView(Ui.title(a, Fmt.money(e.getValue()), 12.5f, Ui.INK));
            card.addView(r);
        }
        return card;
    }

    private View rankCard(String title, List<Item> top, final long today) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.title(a, title, 14, Ui.INK));
        card.addView(Ui.spacer(a, 8));
        for (int i = 0; i < top.size(); i++) {
            final Item it = top.get(i);
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
            TextView rank = Ui.text(a, String.valueOf(i + 1), 12, Ui.MUTED);
            rank.setGravity(Gravity.CENTER);
            rank.setBackground(Ui.round(0xFFF4EFE6, 8, a));
            rank.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 22), Ui.dp(a, 22)));
            r.addView(rank);
            LinearLayout mid = Ui.vbox(a);
            mid.setPadding(Ui.dp(a, 9), 0, 0, 0);
            mid.addView(Ui.text(a, it.name, 13.5f, Ui.INK));
            mid.addView(Ui.text(a, it.category + " · 用了 " + DateUtil.duration(it.usedDays(today)), 10.5f, Ui.MUTED));
            r.addView(mid, Ui.lpw(1));
            r.addView(Ui.title(a, Fmt.perDay(it.dailyFen(today)) + "/天", 13, Ui.ACCENT));
            card.addView(r);
        }
        return card;
    }

    private View extrasCard(Map<String, Long> kinds) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.title(a, "后来追加的钱", 14, Ui.INK));
        TextView h = Ui.text(a, "维修、美化、配件……都会让日均成本变高", 11, Ui.MUTED);
        h.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 10));
        card.addView(h);
        long total = 0;
        for (long v : kinds.values()) total += v;
        for (Map.Entry<String, Long> e : kinds.entrySet()) {
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 4));
            r.addView(Ui.text(a, e.getKey(), 12.5f, Ui.INK2), Ui.lpw(1));
            TextView pct = Ui.text(a, Fmt.pct(total == 0 ? 0 : (double) e.getValue() / total), 11, Ui.MUTED);
            pct.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 10), 0);
            r.addView(pct);
            r.addView(Ui.title(a, Fmt.money(e.getValue()), 12.5f, Ui.AMBER));
            card.addView(r);
        }
        return card;
    }

    private View wardrobeCard(List<Cloth> cloths, long today) {
        LinearLayout card = Ui.card(a);
        LinearLayout h = Ui.hbox(a);
        h.addView(Ui.title(a, "衣柜里穿得最多的", 14, Ui.INK), Ui.lpw(1));
        card.addView(h);
        card.addView(Ui.spacer(a, 8));
        List<Cloth> most = Stats.mostWorn(cloths, 4);
        for (Cloth c : most) {
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
            r.addView(Ui.text(a, c.name, 13, Ui.INK), Ui.lpw(1));
            TextView t = Ui.text(a, c.wearCount + " 次", 11, Ui.MUTED);
            t.setPadding(0, 0, Ui.dp(a, 10), 0);
            r.addView(t);
            r.addView(Ui.title(a, Fmt.perDay(c.costPerWear()) + "/次", 12.5f, Ui.GREEN));
            card.addView(r);
        }
        return card;
    }

    private View worstCard(List<Cloth> worst, long today) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.title(a, "最不划算的衣服", 14, Ui.INK));
        TextView h = Ui.text(a, "买来没怎么穿，单次成本就高", 11, Ui.MUTED);
        h.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 10));
        card.addView(h);
        for (Cloth c : worst) {
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
            LinearLayout mid = Ui.vbox(a);
            mid.addView(Ui.text(a, c.name, 13, Ui.INK));
            String s = c.wearCount == 0 ? "一次没穿过" : "只穿了 " + c.wearCount + " 次";
            TextMuted(mid, s);
            r.addView(mid, Ui.lpw(1));
            r.addView(Ui.title(a, Fmt.perDay(c.costPerWear()) + "/次", 12.5f, Ui.AMBER));
            card.addView(r);
        }
        return card;
    }

    private void TextMuted(LinearLayout parent, String s) {
        TextView t = Ui.text(a, s, 10.5f, Ui.MUTED);
        t.setPadding(0, Ui.dp(a, 2), 0, 0);
        parent.addView(t);
    }

    private View monthlyCard(List<long[]> monthly) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.title(a, "每月花钱", 14, Ui.INK));
        card.addView(Ui.spacer(a, 12));

        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int n = Math.min(6, monthly.size());
        // 倒序取最近 6 个月，按时间正序显示
        List<long[]> show = new ArrayList<>();
        for (int i = 0; i < n; i++) show.add(monthly.get(n - 1 - i));
        for (long[] m : show) {
            int[] ymd = DateUtil.toYmd(m[0]);
            labels.add(ymd[1] + "月");
            values.add(m[1] / 100f);
            colors.add(Ui.ACCENT);
        }
        ChartView ch = new ChartView(a);
        ch.bars(labels, values, colors, "元");
        card.addView(ch, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(a, 150)));
        return card;
    }
}
