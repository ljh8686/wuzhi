package com.wuzhi.ui;

import android.app.Activity;
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

import com.wuzhi.core.CostEntry;
import com.wuzhi.core.DateUtil;
import com.wuzhi.core.Fmt;
import com.wuzhi.core.Item;
import com.wuzhi.core.PhotoStore;
import com.wuzhi.core.Stats;

import java.util.List;

/** 账本页：总览 + 物品列表 + 新增。 */
public class ItemsPage {

    private final MainActivity a;

    public ItemsPage(MainActivity a) { this.a = a; }

    public View build() {
        LinearLayout page = MainActivity.page(a);
        long today = a.today();
        List<Item> items = a.store().items;

        // 顶部标题
        LinearLayout head = Ui.hbox(a);
        LinearLayout left = Ui.vbox(a);
        left.addView(Ui.title(a, "账本", 26, Ui.INK));
        TextView sub = Ui.text(a, "每一件东西，摊到每天值不值", 12.5f, Ui.MUTED);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        left.addView(sub);
        head.addView(left, Ui.lpw(1));

        TextView add = Ui.buttonSmall(a, "＋ 记一笔", Ui.ACCENT, 0xFFFFFFFF);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addItem(); }
        });
        head.addView(add);
        page.addView(head);
        page.addView(Ui.spacer(a, 14));

        // 总览卡
        page.addView(summaryCard(items, today));
        page.addView(Ui.spacer(a, 12));

        if (items.isEmpty()) {
            page.addView(emptyCard("还没有记录任何东西", "点右上角「＋ 记一笔」开始，比如：手机 5999 元，用了 420 天"));
            return MainActivity.scroller(a, page);
        }

        // 分组：在用 / 已结束
        List<Item> sorted = a.store().sortedItems(today);
        boolean headerDone = false, endedHeaderDone = false;
        for (Item it : sorted) {
            boolean ended = it.isEnded();
            if (ended && !endedHeaderDone) {
                endedHeaderDone = true;
                page.addView(sectionTitle("已结束", countEnded(sorted)));
                page.addView(Ui.spacer(a, 6));
            }
            if (!ended && !headerDone) {
                headerDone = true;
                page.addView(sectionTitle("在用 / 闲置", sorted.size() - countEnded(sorted)));
                page.addView(Ui.spacer(a, 6));
            }
            page.addView(itemRow(it, today));
            page.addView(Ui.spacer(a, 9));
        }

        return MainActivity.scroller(a, page);
    }

    private int countEnded(List<Item> l) {
        int n = 0;
        for (Item i : l) if (i.isEnded()) n++;
        return n;
    }

    private View sectionTitle(String s, int count) {
        LinearLayout h = Ui.hbox(a);
        h.setPadding(Ui.dp(a, 2), Ui.dp(a, 4), Ui.dp(a, 2), 0);
        h.addView(Ui.title(a, s, 13, Ui.INK2), Ui.lpw(1));
        h.addView(Ui.text(a, count + " 件", 11.5f, Ui.MUTED));
        return h;
    }

    // ---------------------------------------------------------------- 总览

    private View summaryCard(List<Item> items, long today) {
        LinearLayout card = Ui.card(a);
        card.setBackground(Ui.gradient(0xFF2E2A26, 0xFF3B352E, 18, a));

        long invested = Stats.totalInvested(items);
        double daily = Stats.dailyTotal(items, today);

        TextView k1 = Ui.text(a, "总投入（含后续花销）", 11.5f, 0x99FFFFFF);
        TextView v1 = Ui.number(a, Fmt.money(invested), 27, 0xFFFFFFFF);
        v1.setPadding(0, Ui.dp(a, 5), 0, 0);
        card.addView(k1);
        card.addView(v1);

        card.addView(Ui.spacer(a, 14));

        LinearLayout row = Ui.hbox(a);
        LinearLayout c1 = Ui.vbox(a);
        c1.addView(Ui.text(a, "每天总共花", 11.5f, 0x99FFFFFF));
        TextView v2 = Ui.number(a, Fmt.perDay(daily), 19, 0xFFFFD9A8);
        v2.setPadding(0, Ui.dp(a, 4), 0, 0);
        c1.addView(v2);
        LinearLayout c2 = Ui.vbox(a);
        c2.addView(Ui.text(a, "在用的东西", 11.5f, 0x99FFFFFF));
        TextView v3 = Ui.number(a, String.valueOf(countActive(items)), 19, 0xFFFFFFFF);
        v3.setPadding(0, Ui.dp(a, 4), 0, 0);
        c2.addView(v3);
        row.addView(c1, Ui.lpw(1));
        row.addView(c2, Ui.lpw(1));
        card.addView(row);

        // 附加花销提示
        long extras = 0;
        for (Item i : items) extras += i.extrasFen();
        if (extras > 0) {
            TextView tip = Ui.text(a, "其中后续加钱 " + Fmt.money(extras) + "（维修/美化/配件）", 11, 0xAAFFFFFF);
            tip.setPadding(0, Ui.dp(a, 12), 0, 0);
            card.addView(tip);
        }
        return card;
    }

    private int countActive(List<Item> l) {
        int n = 0;
        for (Item i : l) if (!i.isEnded()) n++;
        return n;
    }

    // ---------------------------------------------------------------- 列表项

    private View itemRow(final Item it, long today) {
        LinearLayout card = Ui.cardTight(a);
        card.setForeground(Ui.ripple(Ui.round(0x00000000, 16, a), a));

        LinearLayout top = Ui.hbox(a);
        String emoji = it.emoji == null || it.emoji.isEmpty() ? "📦" : it.emoji;
        View ic = Art.avatar(a, it.id, Art.itemKey(it.category), emoji, 40, Ui.softFor(it.category));
        top.addView(ic);

        LinearLayout mid = Ui.vbox(a);
        mid.setPadding(Ui.dp(a, 11), 0, 0, 0);
        LinearLayout nameRow = Ui.hbox(a);
        TextView nm = Ui.title(a, it.name, 15.5f, it.isEnded() ? Ui.MUTED : Ui.INK);
        nameRow.addView(nm);
        if (it.pinned) {
            TextView pin = Ui.chip(a, "置顶", Ui.AMBER_SOFT, Ui.AMBER, 10);
            pin.setPadding(Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6), Ui.dp(a, 2));
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.leftMargin = Ui.dp(a, 6);
            nameRow.addView(pin, plp);
        }
        mid.addView(nameRow);

        String meta = it.category + " · " + DateUtil.ago(it.buyDate, today) + "买的";
        if (it.isEnded()) meta = it.status + " · 共 " + DateUtil.duration(it.usedDays(today));
        TextView mt = Ui.text(a, meta, 11.5f, Ui.MUTED);
        mt.setPadding(0, Ui.dp(a, 3), 0, 0);
        mid.addView(mt);
        top.addView(mid, Ui.lpw(1));

        LinearLayout right = Ui.vbox(a);
        right.setGravity(Gravity.END);
        TextView daily = Ui.title(a, Fmt.perDay(it.dailyFen(today)), 16, it.isEnded() ? Ui.MUTED : Ui.ACCENT);
        daily.setGravity(Gravity.END);
        TextView per = Ui.text(a, "每天", 10.5f, Ui.MUTED);
        per.setGravity(Gravity.END);
        right.addView(daily);
        right.addView(per);
        top.addView(right);
        card.addView(top);

        // 附加花销行
        if (!it.extras.isEmpty()) {
            LinearLayout ex = Ui.hbox(a);
            ex.setPadding(Ui.dp(a, 49), Ui.dp(a, 8), 0, 0);
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < it.extras.size() && i < 3; i++) {
                CostEntry c = it.extras.get(i);
                if (b.length() > 0) b.append(" · ");
                b.append(c.label).append(" ").append(Fmt.perDay(c.amountFen));
            }
            if (it.extras.size() > 3) b.append(" 等 ").append(it.extras.size()).append(" 笔");
            TextView t = Ui.text(a, "＋" + b, 11, Ui.AMBER);
            ex.addView(t);
            card.addView(ex);
        }

        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { detail(it); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { menu(it); return true; }
        });
        return card;
    }

    private View emptyCard(String title, String hint) {
        LinearLayout c = Ui.card(a);
        c.setGravity(Gravity.CENTER);
        c.setPadding(Ui.dp(a, 16), Ui.dp(a, 26), Ui.dp(a, 16), Ui.dp(a, 30));
        View art = Art.avatar(a, null, "item_other", "📦", 76, 0x00000000);
        c.addView(art);
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

    void addItem() {
        final Sheet s = new Sheet(a, "记一笔", "花了多少钱，买了什么");
        final EditText name = s.field("名称", "", "比如 手机 / 跑步鞋", InputType.TYPE_CLASS_TEXT);
        final EditText price = s.money("花了多少钱（元）", 0);
        final EditText days = s.number("已经用了多少天（可留空，默认从购买日算）", "", "比如 420");
        final Sheet.ChipRow cat = s.chips("分类", Item.CATEGORIES, "数码");
        final EditText note = s.field("备注（可留空）", "", "比如 主力机，天天用", InputType.TYPE_CLASS_TEXT);

        // 日期：默认今天
        final long[] buyDate = {a.today()};
        final TextView dateRow = s.row("购买日期", DateUtil.format(buyDate[0]), new Runnable() {
            @Override public void run() { pickDate(buyDate, s); }
        });

        // 天数 -> 反推日期
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                String nm = sheet.text(name);
                if (nm.isEmpty()) { toast("请填写名称"); return; }
                long fen = sheet.fen(price);
                if (fen <= 0) { toast("请填写价格"); return; }
                long bd = buyDate[0];
                String ds = sheet.text(days);
                if (!ds.isEmpty()) {
                    try {
                        int n = Integer.parseInt(ds);
                        if (n > 0) bd = a.today() - n;
                    } catch (Exception ignored) { }
                }
                Item it = a.store().addItem(nm, fen, bd, cat.value(), "");
                it.note = sheet.text(note);
                sheet.dismiss();
                a.refresh();
            }
        });
        s.show();
    }

    private void pickDate(final long[] holder, final Sheet s) {
        int[] ymd = DateUtil.toYmd(holder[0]);
        new DatePickerDialog(a, new DatePickerDialog.OnDateSetListener() {
            @Override public void onDateSet(android.widget.DatePicker view, int y, int m, int d) {
                holder[0] = DateUtil.fromYmd(y, m + 1, d);
                s.row("购买日期", DateUtil.format(holder[0]), null);
            }
        }, ymd[0], ymd[1] - 1, ymd[2]).show();
    }

    // ---------------------------------------------------------------- 详情

    void detail(final Item it) {
        LinearLayout page = MainActivity.page(a);
        long today = a.today();

        // 返回 + 标题
        LinearLayout bar = Ui.hbox(a);
        TextView back = Ui.text(a, "‹ 返回", 14, Ui.ACCENT);
        back.setPadding(0, Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { a.show(MainActivity.TAB_ITEMS); }
        });
        bar.addView(back, Ui.lpw(1));
        TextView edit = Ui.text(a, "编辑", 13.5f, Ui.ACCENT);
        edit.setPadding(Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editItem(it); }
        });
        bar.addView(edit);
        page.addView(bar);
        page.addView(Ui.spacer(a, 10));

        // 主卡
        LinearLayout hero = Ui.card(a);
        hero.setBackground(Ui.gradient(0xFF2E2A26, 0xFF3B352E, 18, a));

        View heroArt = Art.hero(a, it.id, Art.itemKey(it.category),
                it.emoji == null || it.emoji.isEmpty() ? "📦" : it.emoji, 64, 0x22FFFFFF);
        heroArt.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                PhotoPicker.showSheet(a, it.id, new Runnable() {
                    @Override public void run() { detail(it); }
                });
            }
        });
        hero.addView(heroArt);
        hero.addView(Ui.spacer(a, 12));

        TextView nm = Ui.title(a, it.name, 20, 0xFFFFFFFF);
        hero.addView(nm);
        TextView meta = Ui.text(a, it.category + " · " + it.status + " · " + DateUtil.formatCn(it.buyDate) + " 购买", 12, 0x99FFFFFF);
        meta.setPadding(0, Ui.dp(a, 5), 0, 0);
        hero.addView(meta);
        hero.addView(Ui.spacer(a, 16));

        LinearLayout bigRow = Ui.hbox(a);
        LinearLayout c1 = Ui.vbox(a);
        c1.addView(Ui.text(a, "每天成本", 11.5f, 0x99FFFFFF));
        TextView dv = Ui.number(a, Fmt.perDay(it.dailyFen(today)), 26, 0xFFFFD9A8);
        dv.setPadding(0, Ui.dp(a, 4), 0, 0);
        c1.addView(dv);
        bigRow.addView(c1, Ui.lpw(1));

        LinearLayout c2 = Ui.vbox(a);
        c2.addView(Ui.text(a, "已经用了", 11.5f, 0x99FFFFFF));
        TextView dd = Ui.number(a, DateUtil.duration(it.usedDays(today)), 19, 0xFFFFFFFF);
        dd.setPadding(0, Ui.dp(a, 6), 0, 0);
        c2.addView(dd);
        bigRow.addView(c2, Ui.lpw(1));
        hero.addView(bigRow);
        page.addView(hero);
        page.addView(Ui.spacer(a, 12));

        // 成本拆解
        LinearLayout c = Ui.card(a);
        c.addView(Ui.title(a, "钱都花在哪", 14, Ui.INK));
        c.addView(Ui.spacer(a, 10));
        c.addView(kv("购入价", Fmt.money(it.priceFen), Ui.INK));
        if (it.salvageFen > 0) c.addView(kv("卖出回收", "-" + Fmt.money(it.salvageFen), Ui.GREEN));
        for (CostEntry e : it.extras) {
            c.addView(kv("＋ " + e.label + "（" + e.kind + "）", Fmt.money(e.amountFen), Ui.AMBER));
        }
        c.addView(Ui.divider(a));
        c.addView(Ui.spacer(a, 8));
        c.addView(kv("合计", Fmt.money(it.netFen()), Ui.ACCENT, true));
        page.addView(c);
        page.addView(Ui.spacer(a, 12));

        // 曲线：如果再用 365 天，日均会降到多少
        page.addView(chartCard(it, today));
        page.addView(Ui.spacer(a, 12));

        // 操作
        LinearLayout ops = Ui.hbox(a);
        TextView addEx = Ui.button(a, "＋ 加一笔花销", true);
        addEx.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addExtra(it); }
        });
        ops.addView(addEx, Ui.lpw(1));
        page.addView(ops);
        page.addView(Ui.spacer(a, 9));

        LinearLayout ops2 = Ui.hbox(a);
        TextView status = Ui.button(a, "改状态", false);
        status.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changeStatus(it); }
        });
        TextView del = Ui.button(a, "删除", false);
        del.setTextColor(0xFFB03A2E);
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmDelete(it); }
        });
        LinearLayout.LayoutParams p1 = Ui.lpw(1);
        LinearLayout.LayoutParams p2 = Ui.lpw(1);
        p2.leftMargin = Ui.dp(a, 9);
        ops2.addView(status, p1);
        ops2.addView(del, p2);
        page.addView(ops2);

        if (it.note != null && !it.note.isEmpty()) {
            page.addView(Ui.spacer(a, 12));
            LinearLayout n = Ui.cardTight(a);
            n.addView(Ui.text(a, "备注", 11.5f, Ui.MUTED));
            TextView nt = Ui.text(a, it.note, 13.5f, Ui.INK2);
            nt.setPadding(0, Ui.dp(a, 5), 0, 0);
            n.addView(nt);
            page.addView(n);
        }

        a.contentView(MainActivity.scroller(a, page));
    }

    private View chartCard(Item it, long today) {
        LinearLayout c = Ui.card(a);
        c.addView(Ui.title(a, "继续用下去，每天会更便宜", 14, Ui.INK));
        TextView hint = Ui.text(a, "横轴是时间，纵轴是那一天的日均成本", 11.5f, Ui.MUTED);
        hint.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 10));
        c.addView(hint);

        // 从购买日到今天，再往后 1 年，日均成本的下降曲线
        long used = it.usedDays(today);
        long total = used + 365;
        List<float[]> pts = new java.util.ArrayList<>();
        int steps = 48;
        for (int i = 0; i <= steps; i++) {
            long d = Math.max(1, used * i / steps);
            float x = (float) i / steps;
            float y = (float) ((double) it.netFen() / d);
            pts.add(new float[]{x, y});
        }
        // 后面一年
        List<float[]> pts2 = new java.util.ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            long d = used + 365L * i / steps;
            float x = (float) i / steps;
            float y = (float) ((double) it.netFen() / Math.max(1, d));
            pts2.add(new float[]{x, y});
        }

        ChartView ch = new ChartView(a);
        ch.line(pts2, "今天", "+1 年", "现在 " + Fmt.perDay(it.dailyFen(today)) + "/天", Ui.ACCENT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 132));
        c.addView(ch, lp);

        // 未来几个节点
        LinearLayout row = Ui.hbox(a);
        row.setPadding(0, Ui.dp(a, 10), 0, 0);
        long[] marks = {30, 180, 365, 730};
        String[] labels = {"+1 月", "+半年", "+1 年", "+2 年"};
        for (int i = 0; i < marks.length; i++) {
            LinearLayout col = Ui.vbox(a);
            col.setGravity(Gravity.CENTER);
            col.addView(Ui.text(a, labels[i], 10.5f, Ui.MUTED));
            TextView v = Ui.title(a, Fmt.perDay(it.dailyFenAfter(today, marks[i])), 12.5f, Ui.INK2);
            v.setPadding(0, Ui.dp(a, 3), 0, 0);
            col.addView(v);
            row.addView(col, Ui.lpw(1));
        }
        c.addView(row);
        return c;
    }

    private View kv(String k, String v, int color) {
        return kv(k, v, color, false);
    }

    private View kv(String k, String v, int color, boolean bold) {
        LinearLayout r = Ui.hbox(a);
        r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
        TextView kk = Ui.text(a, k, 13, bold ? Ui.INK : Ui.INK2);
        if (bold) kk.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView vv = Ui.title(a, v, bold ? 16 : 13.5f, color);
        r.addView(kk, Ui.lpw(1));
        r.addView(vv);
        return r;
    }

    // ---------------------------------------------------------------- 操作

    void addExtra(final Item it) {
        final Sheet s = new Sheet(a, "加一笔花销", "修了、换了、装饰了，都算进去");
        final EditText label = s.field("花在哪了", "", "比如 换电池 / 换键帽 / 手机壳", InputType.TYPE_CLASS_TEXT);
        final EditText amount = s.money("多少钱（元）", 0);
        final Sheet.ChipRow kind = s.chips("类型", CostEntry.KINDS, CostEntry.KIND_REPAIR);
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
                String l = sheet.text(label);
                if (l.isEmpty()) l = kind.value();
                long fen = sheet.fen(amount);
                if (fen <= 0) { toast("请填写金额"); return; }
                a.store().addExtra(it.id, l, fen, date[0], kind.value());
                sheet.dismiss();
                a.refresh();
                detail(it);
            }
        });
        s.show();
    }

    void changeStatus(final Item it) {
        final Sheet s = new Sheet(a, "状态", "报废、送人、卖出后，日均成本会锁定");
        final Sheet.ChipRow st = s.chips("当前状态", Item.STATUSES, it.status);
        final EditText salvage = s.money("卖出回收了多少钱（仅卖出时填）", it.salvageFen);
        final long[] end = {it.endDate > 0 ? it.endDate : a.today()};
        s.row("结束日期", DateUtil.format(end[0]), new Runnable() {
            @Override public void run() {
                int[] ymd = DateUtil.toYmd(end[0]);
                new DatePickerDialog(a, new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker v, int y, int m, int d) {
                        end[0] = DateUtil.fromYmd(y, m + 1, d);
                        s.row("结束日期", DateUtil.format(end[0]), null);
                    }
                }, ymd[0], ymd[1] - 1, ymd[2]).show();
            }
        });
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                it.status = st.value();
                boolean ended = !Item.STATUS_ACTIVE.equals(it.status) && !Item.STATUS_IDLE.equals(it.status);
                it.endDate = ended ? end[0] : 0;
                long sv = sheet.fen(salvage);
                it.salvageFen = Item.STATUS_SOLD.equals(it.status) && sv > 0 ? sv : 0;
                sheet.dismiss();
                a.refresh();
                detail(it);
            }
        });
        s.show();
    }

    void editItem(final Item it) {
        final Sheet s = new Sheet(a, "编辑", null);
        final EditText name = s.field("名称", it.name, "", InputType.TYPE_CLASS_TEXT);
        final EditText price = s.money("购入价（元）", it.priceFen);
        final Sheet.ChipRow cat = s.chips("分类", Item.CATEGORIES, it.category);
        final EditText note = s.field("备注", it.note, "", InputType.TYPE_CLASS_TEXT);
        final long[] bd = {it.buyDate};
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
                it.name = nm;
                it.priceFen = fen;
                it.category = cat.value();
                it.note = sheet.text(note);
                it.buyDate = bd[0];
                sheet.dismiss();
                a.refresh();
                detail(it);
            }
        });
        s.show();
    }

    void confirmDelete(final Item it) {
        new AlertDialog.Builder(a)
                .setTitle("删除「" + it.name + "」？")
                .setMessage("这条记录和它的花销都会一起删掉。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        a.store().removeItem(it.id);
                        PhotoStore.get(a).delete(it.id);
                        a.refresh();
                    }
                }).show();
    }

    void menu(final Item it) {
        final String[] opts = {"置顶 / 取消置顶", "编辑", "删除"};
        new AlertDialog.Builder(a)
                .setTitle(it.name)
                .setItems(opts, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) { it.pinned = !it.pinned; a.refresh(); }
                        else if (which == 1) editItem(it);
                        else confirmDelete(it);
                    }
                }).show();
    }

    void toast(String s) {
        android.widget.Toast.makeText(a, s, android.widget.Toast.LENGTH_SHORT).show();
    }
}
