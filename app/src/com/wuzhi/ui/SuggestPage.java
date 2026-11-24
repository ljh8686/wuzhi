package com.wuzhi.ui;

import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wuzhi.core.Cloth;
import com.wuzhi.core.DateUtil;
import com.wuzhi.core.Outfit;
import com.wuzhi.core.Recommender;
import com.wuzhi.core.Store;
import com.wuzhi.core.WearLog;
import com.wuzhi.core.Weather;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 「穿什么」页：按当前温度推荐单件 + 搭配，并支持"今天穿这套"。 */
public class SuggestPage {

    private final MainActivity a;
    private int temp = Integer.MIN_VALUE;
    private int tolerance = 2;
    private boolean loadingWeather;
    private String weatherErr;
    private long lastTryAt;

    public SuggestPage(MainActivity a) { this.a = a; }

    public void setTemp(int t) {
        this.temp = t;
    }

    private int temp() {
        if (temp != Integer.MIN_VALUE) return temp;
        int last = a.store().lastWeatherTemp;
        if (last != Cloth.NO_TEMP) return last;
        if (!a.store().logs.isEmpty()) {
            List<WearLog> l = new ArrayList<>(a.store().logs);
            l.sort((x, y) -> Long.compare(y.date, x.date));
            return l.get(0).temp;
        }
        return 20;
    }

    public View build() {
        LinearLayout page = MainActivity.page(a);
        final long today = a.today();
        final int t = temp();
        Store st = a.store();

        LinearLayout head = Ui.hbox(a);
        LinearLayout left = Ui.vbox(a);
        left.addView(Ui.title(a, "穿什么", 27, Ui.INK));
        TextView sub = Ui.text(a, "按温度翻衣柜，像老朋友一样记得你", 12.5f, Ui.MUTED);
        sub.setPadding(0, Ui.dp(a, 3), 0, 0);
        left.addView(sub);
        head.addView(left, Ui.lpw(1));
        page.addView(head);
        page.addView(Ui.spacer(a, 14));

        // 温度控制卡（顶部带实时天气）
        maybeRefreshWeather();
        page.addView(tempCard(t));
        page.addView(Ui.spacer(a, 12));

        if (st.cloths.isEmpty()) {
            page.addView(empty("衣柜里还没有衣服", "先去「衣柜」加几件，再回来这里看推荐"));
            return MainActivity.scroller(a, page);
        }

        // 历史同温度
        List<WearLog> same = Recommender.logsAtSimilarTemp(st.logs, t, tolerance);
        if (!same.isEmpty()) {
            page.addView(sameTempCard(t, same, today));
            page.addView(Ui.spacer(a, 14));
        }

        // 搭配推荐
        List<Recommender.Suggestion> sug = Recommender.suggest(
                st.cloths, st.outfits, t, today, st.wearCounts(), 4);
        if (!sug.isEmpty()) {
            page.addView(sectionTitle("推荐搭配", sug.size() + " 套"));
            page.addView(Ui.spacer(a, 8));
            for (int i = 0; i < sug.size(); i++) {
                page.addView(outfitCard(sug.get(i), t, i == 0));
                page.addView(Ui.spacer(a, 10));
            }
            page.addView(Ui.spacer(a, 4));
        }

        // 单件推荐（按槽位）
        page.addView(sectionTitle("单件推荐", t + "°C"));
        page.addView(Ui.spacer(a, 8));
        String[] slots = {Cloth.SLOT_TOP, Cloth.SLOT_BOTTOM, Cloth.SLOT_OUTER, Cloth.SLOT_SHOES, Cloth.SLOT_ACC};
        boolean any = false;
        for (String slot : slots) {
            List<Recommender.Scored> ranked = Recommender.rankBySlot(st.cloths, slot, t, today, st.wearCounts());
            if (ranked.isEmpty()) continue;
            any = true;
            page.addView(slotBlock(slot, ranked, today));
            page.addView(Ui.spacer(a, 10));
        }
        if (!any) {
            page.addView(empty("没有可推荐的衣服", "衣柜里的衣服都被淘汰了，或者还没设置槽位"));
        }

        // 保存当前搭配
        page.addView(Ui.spacer(a, 6));
        TextView saveBtn = Ui.button(a, "＋ 把这套存成搭配", false);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { saveOutfit(t, today); }
        });
        page.addView(saveBtn);

        return MainActivity.scroller(a, page);
    }

    // ---------------------------------------------------------------- 温度

    private View tempCard(final int t) {
        LinearLayout card = Ui.card(a);
        card.setBackground(Ui.gradient(0xFF3A4A5E, 0xFF2E3B4C, 18, a));

        Store st = a.store();
        LinearLayout top = Ui.hbox(a);
        String weather = "";
        if (st.weatherText != null && st.weatherText.length() > 0) {
            weather = Weather.emojiOf(st.weatherCode) + " " + st.weatherText;
            if (st.weatherMin != Cloth.NO_TEMP && st.weatherMax != Cloth.NO_TEMP)
                weather += " · 今日 " + st.weatherMin + "~" + st.weatherMax + "°";
            else
                weather += " " + st.lastWeatherTemp + "°";
        } else {
            weather = weatherErr != null ? "天气没拿到，先手动调" : "现在多少度";
        }
        TextView wline = Ui.text(a, loadingWeather ? "正在获取天气…" : weather, 11.5f, 0xCCFFFFFF);
        wline.setSingleLine(true);
        top.addView(wline, Ui.lpw(1));
        TextView reset = Ui.text(a, loadingWeather ? "" : (weatherErr != null ? "重试" : "刷新天气"), 11, 0x99FFFFFF);
        reset.setPadding(Ui.dp(a, 8), Ui.dp(a, 2), 0, Ui.dp(a, 2));
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { refreshWeather(true); }
        });
        top.addView(reset);
        card.addView(top);

        LinearLayout big = Ui.hbox(a);
        big.setPadding(0, Ui.dp(a, 6), 0, 0);
        TextView minus = Ui.text(a, "−", 26, 0xFFFFFFFF);
        minus.setGravity(Gravity.CENTER);
        minus.setBackground(Ui.ripple(Ui.round(0x22FFFFFF, 14, a), a));
        minus.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 46), Ui.dp(a, 46)));
        minus.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { temp = t - 1; a.show(MainActivity.TAB_SUGGEST); }
        });
        big.addView(minus);

        LinearLayout mid = Ui.vbox(a);
        mid.setGravity(Gravity.CENTER);
        TextView tv = Ui.number(a, t + "°", 44, 0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        mid.addView(tv);
        big.addView(mid, Ui.lpw(1));

        TextView plus = Ui.text(a, "＋", 24, 0xFFFFFFFF);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(Ui.ripple(Ui.round(0x22FFFFFF, 14, a), a));
        plus.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 46), Ui.dp(a, 46)));
        plus.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { temp = t + 1; a.show(MainActivity.TAB_SUGGEST); }
        });
        big.addView(plus);
        card.addView(big);

        // 快捷温度
        LinearLayout quick = Ui.hbox(a);
        quick.setPadding(0, Ui.dp(a, 10), 0, 0);
        int[] qs = {0, 10, 18, 24, 30};
        for (final int q : qs) {
            TextView c = Ui.chip(a, q + "°", q == t ? 0x44FFFFFF : 0x1AFFFFFF, 0xFFFFFFFF, 12);
            c.setPadding(Ui.dp(a, 11), Ui.dp(a, 7), Ui.dp(a, 11), Ui.dp(a, 7));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.weight = 1;
            lp.rightMargin = Ui.dp(a, 5);
            c.setGravity(Gravity.CENTER);
            c.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { temp = q; a.show(MainActivity.TAB_SUGGEST); }
            });
            quick.addView(c, lp);
        }
        card.addView(quick);

        // 天气提示
        String hint = t <= 5 ? "很冷，外套要厚一点" : t <= 14 ? "偏凉，记得加外套"
                : t <= 22 ? "舒适，长袖或薄外套都行" : t <= 28 ? "暖和，短袖就够"
                : "很热，选透气薄款";
        TextView h = Ui.text(a, hint, 11.5f, 0x99FFFFFF);
        h.setPadding(0, Ui.dp(a, 10), 0, 0);
        card.addView(h);
        return card;
    }

    // ---------------------------------------------------------------- 天气

    /** 超过 30 分钟就自动拉一次，用户不用管。 */
    private void maybeRefreshWeather() {
        long now = System.currentTimeMillis() / 1000L;
        long at = a.store().lastWeatherAt;
        if (at > 0 && now - at < 1800) return;
        if (now - lastTryAt < 120) return;   // 失败后 2 分钟内不反复打网络
        refreshWeather(false);
    }

    private void refreshWeather(final boolean manual) {
        if (loadingWeather) return;
        loadingWeather = true;
        weatherErr = null;
        lastTryAt = System.currentTimeMillis() / 1000L;
        if (manual) a.show(MainActivity.TAB_SUGGEST);
        final String city = a.store().city;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final Weather.Now n = Weather.load(city);
                    a.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loadingWeather = false;
                            Store st = a.store();
                            st.lastWeatherTemp = n.temp;
                            st.lastWeatherAt = n.at;
                            st.weatherCode = n.code;
                            st.weatherText = n.text();
                            st.weatherMin = n.tmin;
                            st.weatherMax = n.tmax;
                            if (st.city == null || st.city.length() == 0) st.city = n.city;
                            temp = n.temp;
                            a.save();
                            a.show(MainActivity.TAB_SUGGEST);
                        }
                    });
                } catch (final Exception e) {
                    android.util.Log.w("wuzhi", "weather failed: " + e, e);
                    a.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loadingWeather = false;
                            weatherErr = e.getMessage();
                            if (manual) Toast.makeText(a, "天气没拿到：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            a.show(MainActivity.TAB_SUGGEST);
                        }
                    });
                }
            }
        }).start();
    }

    // ---------------------------------------------------------------- 同温度历史

    private View sameTempCard(int t, List<WearLog> logs, long today) {
        LinearLayout card = Ui.card(a);
        LinearLayout h = Ui.hbox(a);
        h.addView(Ui.title(a, "这个温度你穿过", 14, Ui.INK), Ui.lpw(1));
        h.addView(Ui.chip(a, t + "±" + tolerance + "°C", Ui.BLUE_SOFT, Ui.BLUE, 10.5f));
        card.addView(h);
        card.addView(Ui.spacer(a, 9));

        // 统计这个温度下最常穿的单品
        List<Cloth> mw = Recommender.mostWornAtTemp(a.store().logs, a.store().cloths, t, tolerance);
        if (!mw.isEmpty()) {
            LinearLayout chips = Ui.hbox(a);
            for (int i = 0; i < mw.size() && i < 4; i++) {
                Cloth c = mw.get(i);
                TextView ch = Ui.chip(a, c.name, Ui.softFor(c.slot), Ui.INK2, 11.5f);
                ch.setPadding(Ui.dp(a, 9), Ui.dp(a, 5), Ui.dp(a, 9), Ui.dp(a, 5));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(a, 6);
                chips.addView(ch, lp);
            }
            card.addView(chips);
            card.addView(Ui.spacer(a, 8));
        }

        for (int i = 0; i < logs.size() && i < 3; i++) {
            final WearLog w = logs.get(i);
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));
            r.addView(Ui.text(a, DateUtil.formatShort(w.date), 12, Ui.MUTED), Ui.lpw(0.32f));
            TextView t2 = Ui.chip(a, w.temp + "°C", Ui.BLUE_SOFT, Ui.BLUE, 10.5f);
            t2.setPadding(Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2));
            r.addView(t2);
            TextView nm = Ui.text(a, namesOf(w), 12, Ui.INK2);
            nm.setPadding(Ui.dp(a, 8), 0, 0, 0);
            nm.setSingleLine(true);
            r.addView(nm, Ui.lpw(1));
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
        return b.length() == 0 ? "—" : b.toString();
    }

    // ---------------------------------------------------------------- 搭配卡

    private View outfitCard(final Recommender.Suggestion s, final int t, boolean best) {
        LinearLayout card = Ui.card(a);
        if (best && card instanceof GlassCard) {
            ((GlassCard) card).border(Ui.ACCENT, 1.6f).tint(Glass.TINT_STRONG);
        }

        LinearLayout h = Ui.hbox(a);
        LinearLayout tt = Ui.vbox(a);
        TextView name = Ui.title(a, s.title(), 15.5f, Ui.INK);
        tt.addView(name);
        String rangeText = null;
        if (s.fitMin != Cloth.NO_TEMP && s.fitMax != Cloth.NO_TEMP && s.fitMin <= s.fitMax)
            rangeText = s.minGap == 0 ? "每件都适合 " + s.fitMin + "~" + s.fitMax + "°C"
                                      : "共同温区 " + s.fitMin + "~" + s.fitMax + "°C";
        else if (s.outfit != null && s.outfit.tempMin != s.outfit.tempMax)
            rangeText = "适用 " + s.outfit.tempRangeText();
        if (rangeText != null) {
            TextView rng = Ui.text(a, rangeText, 11, Ui.MUTED);
            rng.setPadding(0, Ui.dp(a, 3), 0, 0);
            tt.addView(rng);
        }
        h.addView(tt, Ui.lpw(1));
        if (best) h.addView(Ui.chip(a, s.minGap == 0 ? "最合适" : "较合适", Ui.ACCENT_SOFT, Ui.ACCENT, 10.5f));
        card.addView(h);

        // 衣服条目
        card.addView(Ui.spacer(a, 8));
        for (Cloth c : s.cloths) {
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 3), 0, Ui.dp(a, 3));
            TextView slot = Ui.chip(a, c.slot, Ui.softFor(c.slot), Ui.INK2, 10);
            slot.setPadding(Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6), Ui.dp(a, 2));
            r.addView(slot);
            TextView nm = Ui.text(a, c.name, 13, Ui.INK);
            nm.setPadding(Ui.dp(a, 8), 0, 0, 0);
            r.addView(nm, Ui.lpw(1));
            if (!c.fits(t)) {
                TextView warn = Ui.chip(a, t < c.tempMin ? "偏薄" : "偏厚", 0xFFF7EBD2, 0xFF9A6B12, 9.5f);
                warn.setPadding(Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6), Ui.dp(a, 2));
                LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                wlp.leftMargin = Ui.dp(a, 6);
                r.addView(warn, wlp);
            }
            TextView tg = Ui.text(a, c.tempRangeText(), 10.5f, Ui.MUTED);
            tg.setPadding(Ui.dp(a, 6), 0, 0, 0);
            r.addView(tg);
            card.addView(r);
        }

        String reason = s.reasonText();
        if (!reason.isEmpty()) {
            TextView rr = Ui.text(a, reason, 11, Ui.ACCENT);
            rr.setPadding(0, Ui.dp(a, 8), 0, 0);
            card.addView(rr);
        }

        // 操作
        LinearLayout ops = Ui.hbox(a);
        ops.setPadding(0, Ui.dp(a, 10), 0, 0);
        TextView wear = Ui.buttonSmall(a, "今天就穿这套", Ui.ACCENT, 0xFFFFFFFF);
        wear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { logOutfit(s, t); }
        });
        ops.addView(wear);
        if (s.outfit != null && s.outfit.id != null && !s.outfit.id.isEmpty()) {
            TextView edit = Ui.buttonSmall(a, "编辑", 0xFFF0EAE0, Ui.INK2);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { editOutfit(s.outfit); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = Ui.dp(a, 8);
            ops.addView(edit, lp);
        } else {
            TextView save = Ui.buttonSmall(a, "存为搭配", 0xFFF0EAE0, Ui.INK2);
            save.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Map<String, String> slots = new LinkedHashMap<>();
                    for (Cloth c : s.cloths) slots.put(c.slot, c.id);
                    a.store().addOutfit(s.title(), slots, t, t);
                    a.refresh();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = Ui.dp(a, 8);
            ops.addView(save, lp);
        }
        card.addView(ops);
        return card;
    }

    private View slotBlock(String slot, List<Recommender.Scored> ranked, long today) {
        LinearLayout card = Ui.card(a);
        LinearLayout h = Ui.hbox(a);
        h.addView(Ui.title(a, slot, 13.5f, Ui.INK), Ui.lpw(1));
        h.addView(Ui.text(a, ranked.size() + " 件可选", 10.5f, Ui.MUTED));
        card.addView(h);
        card.addView(Ui.spacer(a, 7));

        int n = Math.min(3, ranked.size());
        for (int i = 0; i < n; i++) {
            Recommender.Scored sc = ranked.get(i);
            final Cloth c = sc.cloth;
            LinearLayout r = Ui.hbox(a);
            r.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 5));

            TextView rank = Ui.text(a, String.valueOf(i + 1), 11, i == 0 ? Ui.ACCENT : Ui.MUTED);
            rank.setGravity(Gravity.CENTER);
            rank.setBackground(Ui.round(i == 0 ? Ui.ACCENT_SOFT : 0xFFF4EFE6, 8, a));
            rank.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 20), Ui.dp(a, 20)));
            r.addView(rank);

            LinearLayout mid = Ui.vbox(a);
            mid.setPadding(Ui.dp(a, 9), 0, 0, 0);
            mid.addView(Ui.text(a, c.name, 13.5f, Ui.INK));
            String why = sc.reasonText();
            if (why.isEmpty()) why = c.tempRangeText();
            TextView w = Ui.text(a, why, 10.5f, i == 0 ? Ui.ACCENT : Ui.MUTED);
            w.setPadding(0, Ui.dp(a, 2), 0, 0);
            w.setSingleLine(true);
            mid.addView(w);
            r.addView(mid, Ui.lpw(1));

            if (c.lastWornTemp != Cloth.NO_TEMP) {
                TextView lt = Ui.text(a, c.lastWornTemp + "°", 11, Ui.BLUE);
                r.addView(lt);
            }
            card.addView(r);

            if (i == 0) {
                final LinearLayout row = r;
                row.setClickable(true);
                row.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { clothQuick(c); }
                });
            }
        }
        return card;
    }

    private void clothQuick(final Cloth c) {
        a.store(); // no-op keep signature
        final Sheet s = new Sheet(a, c.name, c.tempRangeText() + " 舒服 · 穿第 " + (c.wearCount + 1) + " 次");
        final EditText tempIn = s.number("今天多少度", String.valueOf(temp()), "20");
        final Sheet.ChipRow feel = s.chips("舒适度", new String[]{"1", "2", "3", "4", "5"}, "4");
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                int t = parseInt(sheet.text(tempIn), Integer.MIN_VALUE);
                if (t == Integer.MIN_VALUE) { t = temp(); }
                List<String> ids = new ArrayList<>();
                ids.add(c.id);
                a.store().addLog(a.today(), t, ids, "", "", feel.index() + 1);
                sheet.dismiss();
                a.refresh();
            }
        });
        s.show();
    }

    private void logOutfit(final Recommender.Suggestion s, final int t) {
        final Sheet sh = new Sheet(a, "记下今天穿的", s.title());
        final EditText tempIn = sh.number("今天多少度", String.valueOf(t), "20");
        final Sheet.ChipRow weather = sh.chips("天气", new String[]{"晴", "多云", "阴", "小雨", "雪", "大风"}, "晴");
        final EditText note = sh.field("感受（可留空）", "", "比如 刚刚好", InputType.TYPE_CLASS_TEXT);
        sh.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                int tt = parseInt(sheet.text(tempIn), t);
                List<String> ids = new ArrayList<>();
                for (Cloth c : s.cloths) ids.add(c.id);
                a.store().addLog(a.today(), tt, ids, weather.value(), sheet.text(note), 4);
                sheet.dismiss();
                a.refresh();
            }
        });
        sh.show();
    }

    private void editOutfit(final Outfit o) {
        final Sheet s = new Sheet(a, "编辑搭配", o.tempRangeText());
        final EditText name = s.field("名称", o.name, "比如 秋日通勤", InputType.TYPE_CLASS_TEXT);
        final EditText lo = s.number("适用温度下限", String.valueOf(o.tempMin), "15");
        final EditText hi = s.number("适用温度上限", String.valueOf(o.tempMax), "28");
        final Sheet.ChipRow fav = s.chips("收藏", new String[]{"否", "是"}, o.favorite ? "是" : "否");
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                o.name = sheet.text(name);
                int l = parseInt(sheet.text(lo), o.tempMin);
                int h = parseInt(sheet.text(hi), o.tempMax);
                if (l < h) { o.tempMin = l; o.tempMax = h; }
                o.favorite = "是".equals(fav.value());
                sheet.dismiss();
                a.refresh();
            }
        });
        s.show();
    }

    private void saveOutfit(final int t, long today) {
        final Sheet s = new Sheet(a, "存成搭配", "给这套衣服起个名字，以后可以直接用");
        final EditText name = s.field("名称", "", "比如 秋日通勤", InputType.TYPE_CLASS_TEXT);
        final EditText lo = s.number("适用温度下限", String.valueOf(Math.max(-20, t - 3)), "15");
        final EditText hi = s.number("适用温度上限", String.valueOf(t + 3), "28");

        final Map<String, String> slots = new LinkedHashMap<>();
        final List<String> chosen = new ArrayList<>();
        String[] slotNames = {Cloth.SLOT_TOP, Cloth.SLOT_BOTTOM, Cloth.SLOT_OUTER, Cloth.SLOT_SHOES, Cloth.SLOT_ACC};
        for (String slot : slotNames) {
            List<Recommender.Scored> r = Recommender.rankBySlot(a.store().cloths, slot, t, today, a.store().wearCounts());
            if (r.isEmpty()) continue;
            final List<Recommender.Scored> ranked = r;
            final String[] labels = new String[Math.min(4, r.size())];
            for (int i = 0; i < labels.length; i++) labels[i] = ranked.get(i).cloth.name;
            final Sheet.ChipRow cr = s.chips(slot, labels, labels[0]);
            chosen.add(slot);
            // 保存引用，稍后读取
            slotPicks.put(slot, cr);
        }

        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                String nm = sheet.text(name);
                if (nm.isEmpty()) nm = "我的搭配";
                for (String slot : chosen) {
                    Sheet.ChipRow cr = slotPicks.get(slot);
                    if (cr == null) continue;
                    List<Recommender.Scored> r = Recommender.rankBySlot(
                            a.store().cloths, slot, t, today, a.store().wearCounts());
                    int idx = cr.index();
                    if (idx >= 0 && idx < r.size()) slots.put(slot, r.get(idx).cloth.id);
                }
                slotPicks.clear();
                if (slots.isEmpty()) { toast("没有可选的衣物"); return; }
                int l = parseInt(sheet.text(lo), t - 3);
                int h = parseInt(sheet.text(hi), t + 3);
                if (l >= h) { l = t - 3; h = t + 3; }
                a.store().addOutfit(nm, slots, l, h);
                sheet.dismiss();
                a.refresh();
            }
        });
        s.show();
    }

    private final Map<String, Sheet.ChipRow> slotPicks = new HashMap<>();

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private View sectionTitle(String s, String right) {
        LinearLayout h = Ui.hbox(a);
        h.setPadding(Ui.dp(a, 2), Ui.dp(a, 4), Ui.dp(a, 2), 0);
        h.addView(Ui.section(a, s), Ui.lpw(1));
        h.addView(Ui.text(a, right, 11.5f, Ui.MUTED));
        return h;
    }

    private View empty(String title, String hint) {
        LinearLayout c = Ui.card(a);
        c.setGravity(Gravity.CENTER);
        c.setPadding(Ui.dp(a, 16), Ui.dp(a, 34), Ui.dp(a, 16), Ui.dp(a, 34));
        TextView t = Ui.title(a, title, 15, Ui.INK2);
        t.setGravity(Gravity.CENTER);
        c.addView(t);
        TextView h = Ui.text(a, hint, 12.5f, Ui.MUTED);
        h.setGravity(Gravity.CENTER);
        h.setPadding(0, Ui.dp(a, 8), 0, 0);
        c.addView(h);
        return c;
    }

    void toast(String s) {
        android.widget.Toast.makeText(a, s, android.widget.Toast.LENGTH_SHORT).show();
    }
}
