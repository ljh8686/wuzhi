package com.wuzhi.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 自绘底部弹层：输入框 / 金额 / 日期 / 选项 chips。 */
public class Sheet {

    public interface OnOk {
        void onOk(Sheet s);
    }

    private final Context ctx;
    private final AlertDialog dialog;
    private final LinearLayout body;
    private final List<EditText> fields = new ArrayList<>();
    private OnOk onOk;

    public Sheet(Context c, String title, String subtitle) {
        this.ctx = c;
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        LinearLayout root = Ui.vbox(c);
        root.setBackgroundColor(Ui.CARD);
        root.setPadding(Ui.dp(c, 20), Ui.dp(c, 18), Ui.dp(c, 20), Ui.dp(c, 14));

        LinearLayout head = Ui.hbox(c);
        TextView t = Ui.title(c, title, 18, Ui.INK);
        head.addView(t, Ui.lpw(1));
        TextView x = Ui.text(c, "✕", 16, Ui.MUTED);
        x.setPadding(Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
        x.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        head.addView(x);
        root.addView(head);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView st = Ui.text(c, subtitle, 12.5f, Ui.MUTED);
            st.setPadding(0, Ui.dp(c, 4), 0, 0);
            root.addView(st);
        }

        body = Ui.vbox(c);
        body.setPadding(0, Ui.dp(c, 10), 0, 0);
        ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        sc.addView(body);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0);
        blp.weight = 1;
        root.addView(sc, blp);

        LinearLayout actions = Ui.hbox(c);
        actions.setPadding(0, Ui.dp(c, 10), 0, 0);
        TextView cancel = Ui.button(c, "取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        TextView ok = Ui.button(c, "保存", true);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (onOk != null) onOk.onOk(Sheet.this);
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.weight = 1;
        LinearLayout.LayoutParams alp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp2.weight = 1;
        alp2.leftMargin = Ui.dp(c, 10);
        actions.addView(cancel, alp);
        actions.addView(ok, alp2);
        root.addView(actions);

        dialog = b.setView(root).create();
        dialog.setCanceledOnTouchOutside(true);
    }

    public Sheet onOk(OnOk o) { this.onOk = o; return this; }

    public void show() {
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(Ui.roundStroke(0xF7FFFFFF, 24, 0x66FFFFFF, 1f, ctx));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setDimAmount(0.42f);
            w.addFlags(Window.FEATURE_NO_TITLE);
        }
    }

    public void dismiss() { dialog.dismiss(); }

    // ---------------------------------------------------------------- 字段

    public Sheet label(String s) {
        TextView t = Ui.text(ctx, s, 12, Ui.MUTED);
        t.setPadding(0, Ui.dp(ctx, 10), 0, Ui.dp(ctx, 5));
        body.addView(t);
        return this;
    }

    public Sheet hint(String s) {
        TextView t = Ui.text(ctx, s, 11.5f, Ui.MUTED);
        t.setPadding(0, Ui.dp(ctx, 4), 0, Ui.dp(ctx, 4));
        body.addView(t);
        return this;
    }

    public EditText field(String label, String value, String placeholder, int inputType) {
        if (label != null) label(label);
        EditText e = new EditText(ctx);
        e.setText(value == null ? "" : value);
        e.setHint(placeholder == null ? "" : placeholder);
        e.setTextSize(15);
        e.setTextColor(Ui.INK);
        e.setHintTextColor(0xFFB5ADA0);
        e.setBackground(Ui.round(0xFFF4EFE6, 12, ctx));
        e.setPadding(Ui.dp(ctx, 13), Ui.dp(ctx, 11), Ui.dp(ctx, 13), Ui.dp(ctx, 11));
        e.setSingleLine(true);
        if (inputType >= 0) e.setInputType(inputType);
        body.addView(e, Ui.lpMatch());
        fields.add(e);
        return e;
    }

    public EditText money(String label, long fen) {
        EditText e = field(label, fen > 0 ? com.wuzhi.core.Fmt.yuan(fen) : "", "0.00",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    public EditText number(String label, String value, String placeholder) {
        return field(label, value, placeholder, InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
    }

    /** 横向可选 chips，返回选中的 TextView 列表容器 */
    public ChipRow chips(String label, String[] options, String selected) {
        if (label != null) label(label);
        ChipRow row = new ChipRow(ctx, options, selected);
        body.addView(row.view);
        return row;
    }

    /** 只读行，点击回调 */
    public TextView row(String label, String value, final Runnable onClick) {
        LinearLayout l = Ui.hbox(ctx);
        l.setPadding(0, Ui.dp(ctx, 11), 0, Ui.dp(ctx, 11));
        TextView k = Ui.text(ctx, label, 14, Ui.INK2);
        TextView v = Ui.text(ctx, value, 14, Ui.ACCENT);
        l.addView(k, Ui.lpw(1));
        l.addView(v);
        if (onClick != null) {
            l.setClickable(true);
            l.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { onClick.run(); }
            });
        }
        body.addView(l);
        return v;
    }

    public void add(View v) { body.addView(v); }

    /** 读取某个字段文本 */
    public String text(EditText e) {
        return e.getText().toString().trim();
    }

    /** 读取金额字段，转成分；无法解析返回 -1 */
    public long fen(EditText e) {
        String s = text(e).replace("¥", "").replace(",", "").replace("，", "");
        if (s.isEmpty()) return 0;
        try {
            double d = Double.parseDouble(s);
            return Math.round(d * 100);
        } catch (Exception ex) {
            return -1;
        }
    }

    // ---------------------------------------------------------------- chips

    public static class ChipRow {
        public final ViewGroup view;
        private final List<TextView> chips = new ArrayList<>();
        private final List<String> values = new ArrayList<>();
        private int selected = 0;

        ChipRow(Context c, String[] options, String sel) {
            view = new FlowLayout(c, 7, 5);
            view.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
            for (int i = 0; i < options.length; i++) {
                final int idx = i;
                values.add(options[i]);
                TextView t = Ui.chip(c, options[i], 0xFFF1EBE1, Ui.INK2, 13);
                t.setPadding(Ui.dp(c, 12), Ui.dp(c, 8), Ui.dp(c, 12), Ui.dp(c, 8));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                view.addView(t, lp);
                chips.add(t);
                t.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { select(idx); }
                });
                if (sel != null && sel.equals(options[i])) selected = i;
            }
            paint(c);
        }

        private void paint(Context c) {
            for (int i = 0; i < chips.size(); i++) {
                boolean on = i == selected;
                chips.get(i).setBackground(Ui.round(on ? Ui.ACCENT : 0xFFF1EBE1, 10, c));
                chips.get(i).setTextColor(on ? 0xFFFFFFFF : Ui.INK2);
            }
        }

        public void select(int i) {
            selected = i;
            paint(chips.get(0).getContext());
        }

        public String value() {
            return values.isEmpty() ? "" : values.get(selected);
        }

        public int index() { return selected; }
    }

    /**
     * 自动换行的 chip 容器：一行放不下就换到下一行。
     * 原来用横向 LinearLayout，7 个分类会挤出屏幕、最后一个被压成 56px。
     */
    public static class FlowLayout extends ViewGroup {

        private final int gapX;
        private final int gapY;

        public FlowLayout(Context c, int gapXdp, int gapYdp) {
            super(c);
            gapX = Ui.dp(c, gapXdp);
            gapY = Ui.dp(c, gapYdp);
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int width = MeasureSpec.getSize(wSpec);
            if (width <= 0) width = Ui.dp(getContext(), 320);
            int avail = Math.max(1, width - getPaddingLeft() - getPaddingRight());
            int x = 0, y = 0, rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View ch = getChildAt(i);
                ch.measure(MeasureSpec.makeMeasureSpec(avail, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int cw = Math.min(ch.getMeasuredWidth(), avail);
                int chh = ch.getMeasuredHeight();
                if (x > 0 && x + cw > avail) {
                    x = 0;
                    y += rowH + gapY;
                    rowH = 0;
                }
                x += cw + gapX;
                if (chh > rowH) rowH = chh;
            }
            int h = y + rowH + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, resolveSize(h, hSpec));
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int avail = Math.max(1, getWidth() - getPaddingLeft() - getPaddingRight());
            int x = getPaddingLeft(), y = getPaddingTop(), rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View ch = getChildAt(i);
                int cw = Math.min(ch.getMeasuredWidth(), avail);
                int chh = ch.getMeasuredHeight();
                if (x > getPaddingLeft() && x + cw > getPaddingLeft() + avail) {
                    x = getPaddingLeft();
                    y += rowH + gapY;
                    rowH = 0;
                }
                ch.layout(x, y, x + cw, y + chh);
                x += cw + gapX;
                if (chh > rowH) rowH = chh;
            }
        }
    }
}
