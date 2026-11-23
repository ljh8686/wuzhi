package com.wuzhi.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** 轻量图表：日均成本下降曲线 / 分类柱状图。纯 Canvas 绘制。 */
public final class ChartView extends View {

    public static final int KIND_LINE = 0;
    public static final int KIND_BAR = 1;

    private int kind = KIND_LINE;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);

    // line data
    private final List<float[]> line = new ArrayList<>(); // {x 0..1, y value}
    private String labelStart = "";
    private String labelEnd = "";
    private String caption = "";
    private float maxY = 1;
    private int lineColor = 0xFFC2552E;

    // bar data
    private final List<String> barLabels = new ArrayList<>();
    private final List<Float> barValues = new ArrayList<>();
    private final List<Integer> barColors = new ArrayList<>();
    private String barUnit = "";

    public ChartView(Context c) {
        super(c);
        t.setTextSize(Ui.dpf(c, 10));
        t.setColor(0xFF9A9287);
        p.setStrokeCap(Paint.Cap.ROUND);
    }

    public ChartView line(List<float[]> pts, String start, String end, String caption, int color) {
        this.kind = KIND_LINE;
        this.line.clear();
        this.line.addAll(pts);
        this.labelStart = start;
        this.labelEnd = end;
        this.caption = caption;
        this.lineColor = color;
        maxY = 1;
        for (float[] q : pts) maxY = Math.max(maxY, q[1]);
        invalidate();
        return this;
    }

    public ChartView bars(List<String> labels, List<Float> values, List<Integer> colors, String unit) {
        this.kind = KIND_BAR;
        barLabels.clear();
        barValues.clear();
        barColors.clear();
        barLabels.addAll(labels);
        barValues.addAll(values);
        barColors.addAll(colors);
        this.barUnit = unit;
        maxY = 1;
        for (float v : values) maxY = Math.max(maxY, v);
        invalidate();
        return this;
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (kind == KIND_LINE) drawLine(cv);
        else drawBars(cv);
    }

    private void drawLine(Canvas cv) {
        int w = getWidth(), h = getHeight();
        float pl = Ui.dpf(getContext(), 6), pr = Ui.dpf(getContext(), 6);
        float pt = Ui.dpf(getContext(), 16), pb = Ui.dpf(getContext(), 26);
        float cw = w - pl - pr, ch = h - pt - pb;
        if (cw <= 0 || ch <= 0) return;

        // 横向参考线
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dpf(getContext(), 0.8f));
        p.setColor(0x14000000);
        for (int i = 0; i <= 3; i++) {
            float y = pt + ch * i / 3f;
            cv.drawLine(pl, y, w - pr, y, p);
        }

        if (line.size() < 2) {
            t.setTextAlign(Paint.Align.CENTER);
            cv.drawText("数据还不够，多用几天就有曲线了", w / 2f, pt + ch / 2f, t);
            return;
        }

        // 曲线：x 是时间，y 是日均成本（越用越低）
        Path path = new Path();
        Path fill = new Path();
        float[] xs = new float[line.size()];
        for (int i = 0; i < line.size(); i++) {
            float[] q = line.get(i);
            float x = pl + cw * clamp01(q[0]);
            float y = pt + ch * (1f - clamp01(q[1] / maxY));
            xs[i] = x;
            if (i == 0) {
                path.moveTo(x, y);
                fill.moveTo(x, pt + ch);
                fill.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }
        fill.lineTo(xs[xs.length - 1], pt + ch);
        fill.close();

        p.setStyle(Paint.Style.FILL);
        p.setColor(Ui.blend(lineColor, 0xFFFFFFFF, 0.80f));
        cv.drawPath(fill, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dpf(getContext(), 2.2f));
        p.setColor(lineColor);
        cv.drawPath(path, p);

        // 终点圆点
        p.setStyle(Paint.Style.FILL);
        cv.drawCircle(xs[xs.length - 1], pt + ch * (1f - clamp01(line.get(line.size() - 1)[1] / maxY)),
                Ui.dpf(getContext(), 3.4f), p);

        // 标注
        t.setTextAlign(Paint.Align.LEFT);
        cv.drawText(labelStart, pl, h - Ui.dpf(getContext(), 7), t);
        t.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(labelEnd, w - pr, h - Ui.dpf(getContext(), 7), t);
        if (caption != null && !caption.isEmpty()) {
            t.setTextAlign(Paint.Align.CENTER);
            t.setColor(lineColor);
            cv.drawText(caption, w / 2f, h - Ui.dpf(getContext(), 7), t);
            t.setColor(0xFF9A9287);
        }
    }

    private void drawBars(Canvas cv) {
        int w = getWidth(), h = getHeight();
        if (barValues.isEmpty()) {
            t.setTextAlign(Paint.Align.CENTER);
            cv.drawText("还没有数据", w / 2f, h / 2f, t);
            return;
        }
        float pt = Ui.dpf(getContext(), 18), pb = Ui.dpf(getContext(), 24);
        float ch = h - pt - pb;
        int n = barValues.size();
        float slot = (float) w / n;
        float bw = Math.min(Ui.dpf(getContext(), 26), slot * 0.52f);
        t.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            float v = barValues.get(i);
            float bh = Math.max(Ui.dpf(getContext(), 2), ch * (v / maxY));
            float cx = slot * i + slot / 2f;
            float top = pt + ch - bh;
            p.setStyle(Paint.Style.FILL);
            p.setColor(barColors.size() > i ? barColors.get(i) : lineColor);
            RectF r = new RectF(cx - bw / 2f, top, cx + bw / 2f, pt + ch);
            cv.drawRoundRect(r, Ui.dpf(getContext(), 5), Ui.dpf(getContext(), 5), p);

            t.setColor(0xFF5B554C);
            String val = Ui.dpf(getContext(), 1) > 0 ? fmt(v) : String.valueOf((int) v);
            cv.drawText(val, cx, top - Ui.dpf(getContext(), 5), t);
            t.setColor(0xFF9A9287);
            cv.drawText(barLabels.get(i), cx, h - Ui.dpf(getContext(), 7), t);
        }
        if (barUnit != null && !barUnit.isEmpty()) {
            t.setTextAlign(Paint.Align.RIGHT);
            t.setColor(0xFF9A9287);
            cv.drawText(barUnit, w - Ui.dpf(getContext(), 2), Ui.dpf(getContext(), 10), t);
        }
    }

    private String fmt(float v) {
        if (v >= 10000) return Math.round(v / 10000f) + "w";
        if (v >= 1000) return Math.round(v / 100f) / 10f + "k";
        if (v == Math.rint(v)) return String.valueOf((int) v);
        return String.format(java.util.Locale.US, "%.1f", v);
    }

    private static float clamp01(float v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
