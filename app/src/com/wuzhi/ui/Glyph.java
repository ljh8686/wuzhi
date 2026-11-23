package com.wuzhi.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * 底部导航用的线性图标，Canvas 手绘，不依赖任何图标字体 / 图片。
 * kind: 0 账本 / 1 衣柜 / 2 天气 / 3 统计
 */
public class Glyph extends View {

    public static final int LEDGER = 0;
    public static final int SHIRT = 1;
    public static final int SUN = 2;
    public static final int CHART = 3;
    public static final int GEAR = 4;
    public static final int PLUS = 5;

    private final int kind;
    private boolean active;
    private Integer overrideColor;
    private final float sizeDp;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public Glyph(Context c, int kind, boolean active) {
        this(c, kind, active, 22f);
    }

    public Glyph(Context c, int kind, boolean active, float sizeDp) {
        super(c);
        this.kind = kind;
        this.active = active;
        this.sizeDp = sizeDp;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setActive(boolean a) {
        if (active != a) {
            active = a;
            invalidate();
        }
    }

    /** 强制指定颜色（用在按钮里，不再跟随选中态）。 */
    public Glyph color(int c) {
        this.overrideColor = c;
        invalidate();
        return this;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int s = Ui.dp(getContext(), sizeDp);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float u = w / 24f;                       // 以 24 为设计格
        int color = overrideColor != null ? overrideColor : (active ? Ui.ACCENT : Ui.MUTED);
        stroke.setColor(color);
        stroke.setStrokeWidth(1.85f * u);
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);

        if (active && overrideColor == null) {
            fill.setColor(0x1FC2552E);
            cv.drawCircle(w / 2f, h / 2f, 11f * u, fill);
        }

        cv.save();
        cv.translate((w - 24f * u) / 2f, (h - 24f * u) / 2f);
        switch (kind) {
            case LEDGER:
                drawLedger(cv, u);
                break;
            case SHIRT:
                drawShirt(cv, u);
                break;
            case SUN:
                drawSun(cv, u);
                break;
            case GEAR:
                drawGear(cv, u);
                break;
            case PLUS:
                drawPlus(cv, u);
                break;
            default:
                drawChart(cv, u);
                break;
        }
        cv.restore();
    }

    private void drawGear(Canvas cv, float u) {
        cv.drawCircle(12f * u, 12f * u, 6.6f * u, stroke);
        cv.drawCircle(12f * u, 12f * u, 2.6f * u, stroke);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8.0;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            cv.drawLine(12f * u + c * 7.4f * u, 12f * u + s * 7.4f * u,
                    12f * u + c * 9.4f * u, 12f * u + s * 9.4f * u, stroke);
        }
    }

    private void drawPlus(Canvas cv, float u) {
        cv.drawLine(12f * u, 5.6f * u, 12f * u, 18.4f * u, stroke);
        cv.drawLine(5.6f * u, 12f * u, 18.4f * u, 12f * u, stroke);
    }

    private void drawLedger(Canvas cv, float u) {
        rect.set(4.5f * u, 4.5f * u, 19.5f * u, 19.5f * u);
        cv.drawRoundRect(rect, 2.6f * u, 2.6f * u, stroke);
        // 书脊
        cv.drawLine(8.2f * u, 4.5f * u, 8.2f * u, 19.5f * u, stroke);
        // 三条记录
        float x1 = 11f * u, x2 = 16.6f * u;
        for (int i = 0; i < 3; i++) {
            float y = (8.4f + i * 3.2f) * u;
            cv.drawLine(x1, y, x2, y, stroke);
        }
    }

    private void drawShirt(Canvas cv, float u) {
        path.reset();
        // 领口 + 肩 + 袖子 + 下摆
        path.moveTo(9.4f * u, 4.6f * u);
        path.lineTo(5.2f * u, 6.6f * u);
        path.lineTo(3.4f * u, 10.4f * u);
        path.lineTo(6.2f * u, 11.8f * u);
        path.lineTo(6.2f * u, 19.4f * u);
        path.lineTo(17.8f * u, 19.4f * u);
        path.lineTo(17.8f * u, 11.8f * u);
        path.lineTo(20.6f * u, 10.4f * u);
        path.lineTo(18.8f * u, 6.6f * u);
        path.lineTo(14.6f * u, 4.6f * u);
        path.close();
        cv.drawPath(path, stroke);
        // 领口弧
        path.reset();
        path.moveTo(9.4f * u, 4.6f * u);
        path.quadTo(12f * u, 7.6f * u, 14.6f * u, 4.6f * u);
        cv.drawPath(path, stroke);
    }

    private void drawSun(Canvas cv, float u) {
        cv.drawCircle(12f * u, 11f * u, 4.1f * u, stroke);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8.0;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            cv.drawLine(12f * u + c * 6.3f * u, 11f * u + s * 6.3f * u,
                    12f * u + c * 8.4f * u, 11f * u + s * 8.4f * u, stroke);
        }
    }

    private void drawChart(Canvas cv, float u) {
        float base = 19f * u;
        cv.drawLine(4.2f * u, base, 19.8f * u, base, stroke);
        float[] hh = {5.6f, 9.2f, 7.4f};
        float bw = 2.9f * u;
        for (int i = 0; i < 3; i++) {
            float left = (5.2f + i * 4.6f) * u;
            rect.set(left, base - hh[i] * u, left + bw, base);
            cv.drawRoundRect(rect, 1.1f * u, 1.1f * u, stroke);
        }
    }
}
