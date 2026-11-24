package com.wuzhi.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wuzhi.core.Cloth;
import com.wuzhi.core.DateUtil;
import com.wuzhi.core.Fmt;
import com.wuzhi.core.PhotoStore;
import com.wuzhi.core.Stats;
import com.wuzhi.core.WearLog;

import java.util.ArrayList;
import java.util.List;

/** 衣柜页：按槽位分组的衣服卡片 + 单次成本 + 穿着记录。 */
public class ClosetPage {

    private final MainActivity a;

    public ClosetPage(MainActivity a) { this.a = a; }

    public View build() {
        LinearLayout page = MainActivity.page(a);
        long today = a.today();
        List<Cloth> cloths = a.store().cloths;

        LinearLayout head = Ui.hbox(a);
        LinearLayout left = Ui.vbox(a);
        left.addView(Ui.title(a, "衣柜", 27, Ui.INK));
        TextView sub = Ui.text(a, "衣服也摊到每次穿，才知道值不值", 12.5f, Ui.MUTED);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        left.addView(sub);
        head.addView(left, Ui.lpw(1));

        TextView add = Ui.buttonSmall(a, "＋ 加衣服", Ui.ACCENT, 0xFFFFFFFF);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addCloth(); }
        });
        head.addView(add);
        page.addView(head);
        page.addView(Ui.spacer(a, 14));

        page.addView(summary(cloths));
        page.addView(Ui.spacer(a, 12));

        if (cloths.isEmpty()) {
            page.addView(emptyCard("衣柜还是空的", "点右上角「＋ 加衣服」，填价格和适合的温度区间"));
            return MainActivity.scroller(a, page);
        }

        // 最近穿着
        if (!a.store().logs.isEmpty()) {
            page.addView(recentWearCard(today));
            page.addView(Ui.spacer(a, 14));
        }

        // 按槽位分组
        String[] order = {Cloth.SLOT_TOP, Cloth.SLOT_BOTTOM, Cloth.SLOT_OUTER, Cloth.SLOT_SHOES, Cloth.SLOT_ACC};
        for (String slot : order) {
            List<Cloth> group = new ArrayList<>();
            for (Cloth c : cloths) if (slot.equals(c.slot)) group.add(c);
            if (group.isEmpty()) continue;
            group.sort((x, y) -> Integer.compare(y.wearCount, x.wearCount));
            page.addView(sectionTitle(slot, group.size()));
            page.addView(Ui.spacer(a, 6));
            for (Cloth c : group) {
                page.addView(clothRow(c, today));
                page.addView(Ui.spacer(a, 9));
            }
            page.addView(Ui.spacer(a, 6));
        }

        return MainActivity.scroller(a, page);
    }

    private View sectionTitle(String s, int count) {
        LinearLayout h = Ui.hbox(a);
        h.setPadding(Ui.dp(a, 2), Ui.dp(a, 4), Ui.dp(a, 2), 0);
        h.addView(Ui.section(a, s), Ui.lpw(1));
        h.addView(Ui.label(a, count + " 件", 11, Ui.MUTED));
        return h;
    }

    private View summary(List<Cloth> cloths) {
        LinearLayout card = Ui.card(a);
        card.setBackground(Ui.gradient(0xFF2F3A34, 0xFF3C4A42, 18, a));
        long value = Stats.wardrobeValue(cloths);
        double avg = Stats.avgCostPerWear(cloths);
        int worn = 0;
        for (Cloth c : cloths) if (c.wearCount > 0) worn++;

        card.addView(Ui.text(a, "衣柜总价值", 11.5f, 0x99FFFFFF));
        TextView v = Ui.number(a, Fmt.money(value), 27, 0xFFFFFFFF);
        v.setPadding(0, Ui.dp(a, 5), 0, 0);
        card.addView(v);
        card.addView(Ui.spacer(a, 14));

        LinearLayout row = Ui.hbox(a);
        LinearLayout c1 = Ui.vbox(a);
        c1.addView(Ui.text(a, "平均每次穿", 11.5f, 0x99FFFFFF));
        TextView a1 = Ui.number(a, worn == 0 ? "—" : Fmt.perDay(avg), 19, 0xFFD6E8DC);
        a1.setPadding(0, Ui.dp(a, 4), 0, 0);
        c1.addView(a1);

        LinearLayout c2 = Ui.vbox(a);
        c2.addView(Ui.text(a, "穿过 / 总共", 11.5f, 0x99FFFFFF));
        TextView a2 = Ui.number(a, worn + " / " + cloths.size(), 19, 0xFFFFFFFF);
        a2.setPadding(0, Ui.dp(a, 4), 0, 0);
        c2.addView(a2);

        row.addView(c1, Ui.lpw(1));
        row.addView(c2, Ui.lpw(1));
        card.addView(row);

        List<Cloth> neg = Stats.neglected(cloths, 1);
        if (!neg.isEmpty() && neg.get(0).lastWornDate == 0) {
            TextView tip = Ui.text(a, "「" + neg.get(0).name + "」还没穿过，去穿一次吧", 11, 0xAAFFFFFF);
            tip.setPadding(0, Ui.dp(a, 12), 0, 0);
            card.addView(tip);
        } else if (!neg.isEmpty()) {
            long days = a.today() - neg.get(0).lastWornDate;
            if (days >= 14) {
                TextView tip = Ui.text(a, "「" + neg.get(0).name + "」已经 " + days + " 天没穿了", 11, 0xAAFFFFFF);
                tip.setPadding(0, Ui.dp(a, 12), 0, 0);
                card.addView(tip);
            }
        }
        return card;
    }

    private View recentWearCard(long today) {
        LinearLayout card = Ui.card(a);
        LinearLayout h = Ui.hbox(a);
        h.addView(Ui.section(a, "最近穿了什么"), Ui.lpw(1));
        TextView all = Ui.text(a, "全部记录 ›", 12, Ui.ACCENT);
        all.setPadding(Ui.dp(a, 6), 0, 0, 0);
        all.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { logList(); }
        });
        h.addView(all);
        card.addView(h);
        card.addView(Ui.spacer(a, 9));

        List<WearLog> logs = Stats.recentLogs(a.store().logs, 3);
        for (int i = 0; i < logs.size(); i++) {
            WearLog w = logs.get(i);
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
            TextView d = Ui.text(a, DateUtil.formatShort(w.date), 12, Ui.MUTED);
            d.setWidth(Ui.dp(a, 52));
            r.addView(d);
            TextView t = Ui.chip(a, w.temp + "°C", 0xFFF1E9DC, 0xFF6B6153, 10.5f);
            t.setPadding(Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2));
            r.addView(t);
            TextView names = Ui.text(a, namesOf(w), 12, Ui.INK2);
            names.setPadding(Ui.dp(a, 8), 0, 0, 0);
            names.setSingleLine(true);
            r.addView(names, Ui.lpw(1));
            card.addView(r);
        }
        return card;
    }

    private String namesOf(WearLog w) {
        StringBuilder b = new StringBuilder();
        for (String id : w.clothIds) {
            Cloth c = a.store().clothById(id);
            if (c == null) continue;
            if (b.length() > 0) b.append(" + ");
            b.append(c.name);
        }
        return b.length() == 0 ? "（衣服已删除）" : b.toString();
    }

    private View clothRow(final Cloth c, long today) {
        LinearLayout card = Ui.cardTight(a);
        card.setForeground(Ui.ripple(Ui.round(0x00000000, 16, a), a));
        if (c.retired) card.setAlpha(0.55f);

        LinearLayout top = Ui.hbox(a);
        View ic = Art.avatar(a, c.id, Art.clothKey(c.slot), emojiFor(c), 40, Ui.softFor(c.slot));
        top.addView(ic);

        LinearLayout mid = Ui.vbox(a);
        mid.setPadding(Ui.dp(a, 11), 0, 0, 0);
        LinearLayout nmRow = Ui.hbox(a);
        nmRow.addView(Ui.title(a, c.name, 15, Ui.INK));
        if (c.favorite) {
            TextView f = Ui.chip(a, "常穿", Ui.ACCENT_SOFT, Ui.ACCENT, 9);
            f.setPadding(Ui.dp(a, 5), Ui.dp(a, 1), Ui.dp(a, 5), Ui.dp(a, 1));
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            flp.leftMargin = Ui.dp(a, 6);
            nmRow.addView(f, flp);
        }
        mid.addView(nmRow);

        String meta = c.tempRangeText() + " 舒服" + (c.color.isEmpty() ? "" : " · " + c.color)
                + " · " + Fmt.money(c.priceFen);
        TextView mt = Ui.text(a, meta, 11.5f, Ui.MUTED);
        mt.setPadding(0, Ui.dp(a, 3), 0, 0);
        mid.addView(mt);
        top.addView(mid, Ui.lpw(1));

        LinearLayout right = Ui.vbox(a);
        right.setGravity(Gravity.END);
        TextView cpw = Ui.title(a, c.wearCount == 0 ? "没穿过" : Fmt.perDay(c.costPerWear()), 15,
                c.wearCount == 0 ? Ui.MUTED : Ui.GREEN);
        cpw.setGravity(Gravity.END);
        TextView cnt = Ui.text(a, c.wearCount == 0 ? "—" : "穿了 " + c.wearCount + " 次", 10.5f, Ui.MUTED);
        cnt.setGravity(Gravity.END);
        right.addView(cpw);
        right.addView(cnt);
        top.addView(right);
        card.addView(top);

        // 上次穿
        if (c.lastWornDate > 0) {
            String s = "上次 " + DateUtil.ago(c.lastWornDate, today);
            if (c.lastWornTemp != Cloth.NO_TEMP) s += "，那天 " + c.lastWornTemp + "°C";
            TextView lt = Ui.text(a, s, 11, Ui.BLUE);
            lt.setPadding(Ui.dp(a, 49), Ui.dp(a, 7), 0, 0);
            card.addView(lt);
        } else {
            TextView lt = Ui.text(a, "还没穿过，今天试试？", 11, Ui.AMBER);
            lt.setPadding(Ui.dp(a, 49), Ui.dp(a, 7), 0, 0);
            card.addView(lt);
        }

        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { detail(c); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { menu(c); return true; }
        });
        return card;
    }

    private String emojiFor(Cloth c) {
        switch (c.category) {
            case "上衣": return "👕";
            case "裤子": return "👖";
            case "裙子": return "👗";
            case "外套": return "🧥";
            case "鞋": return "👟";
            case "配饰": return "🧣";
            case "包": return "👜";
            case "内衣": return "🩲";
            default: return "🧺";
        }
    }

    private View emptyCard(String title, String hint) {
        LinearLayout c = Ui.card(a);
        c.setGravity(Gravity.CENTER);
        c.setPadding(Ui.dp(a, 16), Ui.dp(a, 26), Ui.dp(a, 16), Ui.dp(a, 30));
        c.addView(Art.avatar(a, null, "cloth_top", "👕", 76, 0x00000000));
        TextView t = Ui.title(a, title, 15, Ui.INK2);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(a, 12), 0, 0);
        c.addView(t);
        TextView h = Ui.text(a, hint, 12.5f, Ui.MUTED);
        h.setGravity(Gravity.CENTER);
        h.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), 0);
        c.addView(h);
        return c;
    }

    // ---------------------------------------------------------------- 新增

    void addCloth() {
        final Sheet s = new Sheet(a, "加衣服", "填好价格和适合的温度，推荐才准");
        final EditText name = s.field("名称", "", "比如 灰色卫衣", InputType.TYPE_CLASS_TEXT);
        final EditText price = s.money("价格（元）", 0);
        final Sheet.ChipRow cat = s.chips("分类", Cloth.CATEGORIES, "上衣");
        final Sheet.ChipRow warmth = s.chips("厚薄", new String[]{"薄款", "常规", "厚款", "加绒/羽绒"}, "常规");
        final EditText tmin = s.number("适合温度下限（°C，可填负数）", "15", "15");
        final EditText tmax = s.number("适合温度上限（°C）", "28", "28");
        final EditText color = s.field("颜色（可留空）", "", "比如 灰", InputType.TYPE_CLASS_TEXT);

        // 分类变化时自动带出默认温度
        s.hint("不确定温度区间的话，选完分类会自动填一个默认值");

        final long[] bd = {a.today()};
        s.row("购买日期", DateUtil.format(bd[0]), new Runnable() {
            @Override public void run() {
                int[] ymd = DateUtil.toYmd(bd[0]);
                new DatePickerDialog(a, new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker v, int y, int m, int d) {
                        bd[0] = DateUtil.fromYmd(y, m + 1, d);
                        s.row("购买日期", DateUtil.format(bd[0]), null);
                    }
                }, ymd[0], ymd[1] - 1, ymd[2]).show();
            }
        });

        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                String nm = sheet.text(name);
                if (nm.isEmpty()) { toast("请填写名称"); return; }
                long fen = sheet.fen(price);
                if (fen <= 0) { toast("请填写价格"); return; }
                Cloth c = a.store().addCloth(nm, cat.value(), fen, bd[0]);
                c.warmth = warmth.value();
                c.color = sheet.text(color);
                int lo = parseInt(sheet.text(tmin), Integer.MIN_VALUE);
                int hi = parseInt(sheet.text(tmax), Integer.MIN_VALUE);
                if (lo != Integer.MIN_VALUE && hi != Integer.MIN_VALUE && lo < hi) {
                    c.tempMin = lo;
                    c.tempMax = hi;
                } else {
                    int[] r = Cloth.defaultTempRange(cat.value(), c.warmth);
                    c.tempMin = r[0];
                    c.tempMax = r[1];
                }
                sheet.dismiss();
                a.refresh();
            }
        });
        s.show();
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    // ---------------------------------------------------------------- 详情

    void detail(final Cloth c) {
        LinearLayout page = MainActivity.page(a);
        long today = a.today();

        LinearLayout bar = Ui.hbox(a);
        TextView back = Ui.buttonSmall(a, "‹ 返回", 0xFFF0EAE0, Ui.INK2);
        back.setPadding(0, Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { a.show(MainActivity.TAB_CLOSET); }
        });
        bar.addView(back);
        bar.addView(new View(a), Ui.lpw(1));
        TextView edit = Ui.text(a, "编辑", 13.5f, Ui.ACCENT);
        edit.setPadding(Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editCloth(c); }
        });
        bar.addView(edit);
        page.addView(bar);
        page.addView(Ui.spacer(a, 10));

        LinearLayout hero = Ui.card(a);
        hero.setBackground(Ui.gradient(0xFF2F3A34, 0xFF3C4A42, 18, a));

        View heroArt = Art.hero(a, c.id, Art.clothKey(c.slot), emojiFor(c), 64, 0x22FFFFFF);
        heroArt.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                PhotoPicker.showSheet(a, c.id, new Runnable() {
                    @Override public void run() { detail(c); }
                });
            }
        });
        hero.addView(heroArt);
        hero.addView(Ui.spacer(a, 12));

        hero.addView(Ui.title(a, c.name, 20, 0xFFFFFFFF));
        TextView meta = Ui.text(a, c.category + " · " + c.tempRangeText() + " 舒服"
                + (c.color.isEmpty() ? "" : " · " + c.color), 12, 0x99FFFFFF);
        meta.setPadding(0, Ui.dp(a, 5), 0, 0);
        hero.addView(meta);
        hero.addView(Ui.spacer(a, 16));

        LinearLayout row = Ui.hbox(a);
        LinearLayout c1 = Ui.vbox(a);
        c1.addView(Ui.text(a, "每次穿成本", 11.5f, 0x99FFFFFF));
        TextView v1 = Ui.number(a, c.wearCount == 0 ? "—" : Fmt.perDay(c.costPerWear()), 25, 0xFFD6E8DC);
        v1.setPadding(0, Ui.dp(a, 4), 0, 0);
        c1.addView(v1);

        LinearLayout c2 = Ui.vbox(a);
        c2.addView(Ui.text(a, "穿了", 11.5f, 0x99FFFFFF));
        TextView v2 = Ui.number(a, c.wearCount + " 次", 19, 0xFFFFFFFF);
        v2.setPadding(0, Ui.dp(a, 7), 0, 0);
        c2.addView(v2);
        row.addView(c1, Ui.lpw(1));
        row.addView(c2, Ui.lpw(1));
        hero.addView(row);
        page.addView(hero);
        page.addView(Ui.spacer(a, 12));

        // 信息
        LinearLayout info = Ui.card(a);
        info.addView(Ui.section(a, "这件衣服"));
        info.addView(Ui.spacer(a, 10));
        info.addView(kv("价格", Fmt.money(c.priceFen), Ui.INK));
        info.addView(kv("买了多久", DateUtil.duration(c.ownedDays(today)) + "（" + DateUtil.format(c.buyDate) + "）", Ui.INK2));
        info.addView(kv("适合温度", c.tempRangeText(), Ui.BLUE));
        if (!c.warmth.isEmpty()) info.addView(kv("厚薄", c.warmth, Ui.INK2));
        if (c.lastWornDate > 0) {
            String s = DateUtil.format(c.lastWornDate) + "（" + DateUtil.ago(c.lastWornDate, today) + "）";
            if (c.lastWornTemp != Cloth.NO_TEMP) s += " · 那天 " + c.lastWornTemp + "°C";
            info.addView(kv("上次穿", s, Ui.ACCENT));
        } else {
            info.addView(kv("上次穿", "还没穿过", Ui.AMBER));
        }
        if (!c.note.isEmpty()) info.addView(kv("备注", c.note, Ui.INK2));
        page.addView(info);
        page.addView(Ui.spacer(a, 12));

        // 穿着记录
        List<WearLog> mine = new ArrayList<>();
        for (WearLog w : a.store().logs) if (w.clothIds.contains(c.id)) mine.add(w);
        mine.sort((x, y) -> Long.compare(y.date, x.date));
        if (!mine.isEmpty()) {
            LinearLayout logs = Ui.card(a);
            logs.addView(Ui.section(a, "穿它的记录"));
            logs.addView(Ui.spacer(a, 8));
            for (int i = 0; i < mine.size() && i < 12; i++) {
                WearLog w = mine.get(i);
                LinearLayout r = Ui.hbox(a);
                r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
                r.addView(Ui.text(a, DateUtil.formatShort(w.date), 12, Ui.MUTED), Ui.lpw(1));
                TextView t = Ui.chip(a, w.temp + "°C", 0xFFF1E9DC, 0xFF6B6153, 10.5f);
                t.setPadding(Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2));
                r.addView(t);
                if (w.feel > 0) {
                    TextView f = Ui.text(a, stars(w.feel), 10, Ui.AMBER);
                    f.setPadding(Ui.dp(a, 7), 0, 0, 0);
                    r.addView(f);
                }
                logs.addView(r);
            }
            page.addView(logs);
            page.addView(Ui.spacer(a, 12));
        }

        TextView wear = Ui.button(a, "今天穿了它（记一条）", true);
        wear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { quickWear(c); }
        });
        page.addView(wear);
        page.addView(Ui.spacer(a, 9));

        LinearLayout ops = Ui.hbox(a);
        TextView del = Ui.button(a, "删除", false);
        del.setTextColor(0xFFB03A2E);
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmDelete(c); }
        });
        ops.addView(del, Ui.lpw(1));
        page.addView(ops);

        a.contentView(MainActivity.scroller(a, page));
    }

    private String stars(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 5; i++) b.append(i < n ? "★" : "☆");
        return b.toString();
    }

    private View kv(String k, String v, int color) {
        LinearLayout r = Ui.hbox(a);
        r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
        r.addView(Ui.text(a, k, 13, Ui.INK2), Ui.lpw(1));
        TextView vv = Ui.title(a, v, 13, color);
        vv.setGravity(Gravity.END);
        r.addView(vv);
        return r;
    }

    // ---------------------------------------------------------------- 记穿着

    void quickWear(final Cloth c) {
        final Sheet s = new Sheet(a, "记一次穿着", c.name);
        final EditText temp = s.number("今天多少度（°C）", String.valueOf(lastKnownTemp()), "20");
        final Sheet.ChipRow weather = s.chips("天气", new String[]{"晴", "多云", "阴", "小雨", "雪", "大风"}, "晴");
        final EditText note = s.field("感受（可留空）", "", "比如 刚刚好 / 有点热", InputType.TYPE_CLASS_TEXT);
        final Sheet.ChipRow feel = s.chips("舒适度", new String[]{"1", "2", "3", "4", "5"}, "4");
        final long[] date = {a.today()};
        s.row("日期", DateUtil.format(date[0]), new Runnable() {
            @Override public void run() {
                int[] ymd = DateUtil.toYmd(date[0]);
                new DatePickerDialog(a, new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker v, int y, int m, int d) {
                        date[0] = DateUtil.fromYmd(y, m + 1, d);
                        s.row("日期", DateUtil.format(date[0]), null);
                    }
                }, ymd[0], ymd[1] - 1, ymd[2]).show();
            }
        });
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                int t = parseInt(sheet.text(temp), Integer.MIN_VALUE);
                if (t == Integer.MIN_VALUE) { toast("请填写温度"); return; }
                List<String> ids = new ArrayList<>();
                ids.add(c.id);
                a.store().addLog(date[0], t, ids, weather.value(), sheet.text(note), feel.index() + 1);
                sheet.dismiss();
                a.refresh();
                detail(c);
            }
        });
        s.show();
    }

    int lastKnownTemp() {
        int t = a.store().lastWeatherTemp;
        if (t != Cloth.NO_TEMP) return t;
        if (!a.store().logs.isEmpty()) {
            List<WearLog> l = Stats.recentLogs(a.store().logs, 1);
            if (!l.isEmpty()) return l.get(0).temp;
        }
        return 20;
    }

    void logList() {
        LinearLayout page = MainActivity.page(a);
        LinearLayout bar = Ui.hbox(a);
        TextView back = Ui.buttonSmall(a, "‹ 返回", 0xFFF0EAE0, Ui.INK2);
        back.setPadding(0, Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { a.show(MainActivity.TAB_CLOSET); }
        });
        bar.addView(back);
        bar.addView(new View(a), Ui.lpw(1));
        page.addView(bar);
        page.addView(Ui.spacer(a, 8));
        page.addView(Ui.title(a, "穿着记录", 22, Ui.INK));
        page.addView(Ui.spacer(a, 12));

        List<WearLog> logs = Stats.recentLogs(a.store().logs, 200);
        if (logs.isEmpty()) {
            page.addView(Ui.card(a));
        }
        for (final WearLog w : logs) {
            LinearLayout card = Ui.cardTight(a);
            LinearLayout r = Ui.hbox(a);
            LinearLayout left = Ui.vbox(a);
            left.addView(Ui.title(a, DateUtil.formatShort(w.date) + " · " + DateUtil.weekdayCn(w.date), 13, Ui.INK));
            TextView nm = Ui.text(a, namesOf(w), 12, Ui.INK2);
            nm.setPadding(0, Ui.dp(a, 4), 0, 0);
            nm.setSingleLine(true);
            left.addView(nm);
            r.addView(left, Ui.lpw(1));
            LinearLayout right = Ui.vbox(a);
            right.setGravity(Gravity.END);
            TextView t = Ui.chip(a, w.temp + "°C", 0xFFF1E9DC, 0xFF6B6153, 11);
            t.setPadding(Ui.dp(a, 8), Ui.dp(a, 3), Ui.dp(a, 8), Ui.dp(a, 3));
            right.addView(t);
            if (w.weather != null && !w.weather.isEmpty()) {
                TextView ww = Ui.text(a, w.weather, 10.5f, Ui.MUTED);
                ww.setPadding(0, Ui.dp(a, 4), 0, 0);
                right.addView(ww);
            }
            r.addView(right);
            card.addView(r);
            if (w.note != null && !w.note.isEmpty()) {
                TextView n = Ui.text(a, w.note, 11.5f, Ui.MUTED);
                n.setPadding(0, Ui.dp(a, 7), 0, 0);
                card.addView(n);
            }
            card.setClickable(true);
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { confirmDeleteLog(w); return true; }
            });
            page.addView(card);
            page.addView(Ui.spacer(a, 8));
        }
        a.contentView(MainActivity.scroller(a, page));
    }

    private void confirmDeleteLog(final WearLog w) {
        new AlertDialog.Builder(a)
                .setTitle("删除这条记录？")
                .setMessage(DateUtil.format(w.date) + " · " + w.temp + "°C")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int i) {
                        a.store().removeLog(w.id);
                        a.refresh();
                    }
                }).show();
    }

    void editCloth(final Cloth c) {
        final Sheet s = new Sheet(a, "编辑衣服", null);
        final EditText name = s.field("名称", c.name, "", InputType.TYPE_CLASS_TEXT);
        final EditText price = s.money("价格（元）", c.priceFen);
        final Sheet.ChipRow cat = s.chips("分类", Cloth.CATEGORIES, c.category);
        final Sheet.ChipRow slot = s.chips("搭配槽位", Cloth.slots().toArray(new String[0]), c.slot);
        final EditText tmin = s.number("适合温度下限", String.valueOf(c.tempMin), "15");
        final EditText tmax = s.number("适合温度上限", String.valueOf(c.tempMax), "28");
        final EditText warmth = s.field("厚薄", c.warmth, "薄款 / 常规 / 加绒", InputType.TYPE_CLASS_TEXT);
        final EditText color = s.field("颜色", c.color, "", InputType.TYPE_CLASS_TEXT);
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                String nm = sheet.text(name);
                if (nm.isEmpty()) { toast("请填写名称"); return; }
                long fen = sheet.fen(price);
                if (fen <= 0) { toast("请填写价格"); return; }
                c.name = nm;
                c.priceFen = fen;
                c.category = cat.value();
                c.slot = slot.value();
                c.warmth = sheet.text(warmth);
                c.color = sheet.text(color);
                int lo = parseInt(sheet.text(tmin), c.tempMin);
                int hi = parseInt(sheet.text(tmax), c.tempMax);
                if (lo < hi) { c.tempMin = lo; c.tempMax = hi; }
                sheet.dismiss();
                a.refresh();
                detail(c);
            }
        });
        s.show();
    }

    void confirmDelete(final Cloth c) {
        new AlertDialog.Builder(a)
                .setTitle("删除「" + c.name + "」？")
                .setMessage("它也会从搭配和穿着记录里移除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        a.store().removeCloth(c.id);
                        PhotoStore.get(a).delete(c.id);
                        a.refresh();
                    }
                }).show();
    }

    void menu(final Cloth c) {
        final String[] opts = {"编辑", "标为收藏 / 取消", c.retired ? "重新启用" : "淘汰（不参与推荐）", "删除"};
        new AlertDialog.Builder(a)
                .setTitle(c.name)
                .setItems(opts, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) editCloth(c);
                        else if (which == 1) { c.favorite = !c.favorite; a.refresh(); }
                        else if (which == 2) { c.retired = !c.retired; a.refresh(); }
                        else confirmDelete(c);
                    }
                }).show();
    }

    void toast(String s) {
        android.widget.Toast.makeText(a, s, android.widget.Toast.LENGTH_SHORT).show();
    }
}
