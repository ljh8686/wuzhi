package com.wuzhi.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

/**
 * 毛玻璃卡片。继承 LinearLayout（竖直排列），所以原来 {@code Ui.card()} 的
 * 调用点不用改一行代码。
 *
 * 绘制顺序：把全屏柔化背景当 BitmapShader，按自己在窗口里的位置平移后画一个圆角矩形
 * （drawRoundRect 自带抗锯齿，比 clipPath 干净）→ 盖一层白色半透明 → 顶部高光
 * → 一层很淡的噪点 → 1px 白边。
 *
 * 两个关键细节：
 * 1. 滚动容器滚动时子 View 不会重新 onDraw，玻璃里会一直透出滚动前那块背景，
 *    所以挂一个 OnScrollChangedListener 主动 invalidate。
 * 2. 大面积平滑渐变在 8bit 屏上会 banding，叠一层 128x128 平铺噪点立刻就有质感。
 */
public class GlassCard extends LinearLayout {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int[] loc = new int[2];
    private final Matrix matrix = new Matrix();

    private BitmapShader bgShader;
    private Bitmap bgBound;
    private BitmapShader grainShader;
    private Bitmap grainBound;
    private LinearGradient sheenShader;
    private float sheenH = -1f;

    private float radiusDp = 20f;
    private int tint = Glass.TINT;
    private int strokeColor = Glass.STROKE;
    private float strokeDp = 1f;
    private boolean sheen = true;
    private boolean outline = true;
    private boolean grain = true;

    private final ViewTreeObserver.OnScrollChangedListener scrollListener =
            new ViewTreeObserver.OnScrollChangedListener() {
                @Override public void onScrollChanged() { invalidate(); }
            };

    public GlassCard(Context c) {
        super(c);
        setWillNotDraw(false);
        setOrientation(VERTICAL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public GlassCard radius(float dp) { this.radiusDp = dp; invalidate(); return this; }
    public GlassCard tint(int color) { this.tint = color; invalidate(); return this; }
    public GlassCard border(int color, float widthDp) {
        this.strokeColor = color; this.strokeDp = widthDp; invalidate(); return this;
    }
    public GlassCard noBorder() { this.outline = false; invalidate(); return this; }
    public GlassCard noSheen() { this.sheen = false; invalidate(); return this; }
    public GlassCard noGrain() { this.grain = false; invalidate(); return this; }
    public float radiusPx() { return Ui.dpf(getContext(), radiusDp); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(scrollListener);
    }

    @Override protected void onDetachedFromWindow() {
        ViewTreeObserver vto = getViewTreeObserver();
        if (vto != null && vto.isAlive()) vto.removeOnScrollChangedListener(scrollListener);
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        // 显式设了背景（深色 hero 卡、渐变卡）就不再叠玻璃，
        // 否则玻璃白会盖在深色背景上，把卡洗成奶白色。
        if (getBackground() != null) return;
        Context c = getContext();
        float r = Ui.dpf(c, radiusDp);
        rect.set(0, 0, w, h);

        getLocationInWindow(loc);

        // 1) 背后的背景：BitmapShader + drawRoundRect（圆角自带抗锯齿）
        Bitmap bg = Glass.backdrop(c);
        if (bg != null && !bg.isRecycled()) {
            if (bgShader == null || bgBound != bg) {
                bgBound = bg;
                bgShader = new BitmapShader(bg, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            }
            matrix.setTranslate(-loc[0], -loc[1]);
            bgShader.setLocalMatrix(matrix);
            fill.setShader(bgShader);
            canvas.drawRoundRect(rect, r, r, fill);
            fill.setShader(null);
        }

        // 2) 玻璃白
        fill.setColor(tint);
        canvas.drawRoundRect(rect, r, r, fill);

        // 3) 顶部高光（渐变在 55% 处就透明了，直接画圆角矩形即可，不需要裁剪）
        if (sheen) {
            if (sheenShader == null || sheenH != h) {
                sheenH = h;
                sheenShader = new LinearGradient(0, 0, 0, h * 0.55f,
                        new int[]{0x4DFFFFFF, 0x14FFFFFF, 0x00FFFFFF},
                        new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
            }
            sheenPaint.setShader(sheenShader);
            canvas.drawRoundRect(rect, r, r, sheenPaint);
            sheenPaint.setShader(null);
        }

        // 4) 噪点（平铺 128x128，透明度极低，只负责消 banding）
        if (grain) {
            Bitmap n = Glass.noise(c);
            if (n != null && !n.isRecycled()) {
                if (grainShader == null || grainBound != n) {
                    grainBound = n;
                    grainShader = new BitmapShader(n, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                }
                grainPaint.setShader(grainShader);
                canvas.drawRoundRect(rect, r, r, grainPaint);
                grainPaint.setShader(null);
            }
        }

        // 5) 边缘高光
        if (outline) {
            stroke.setColor(strokeColor);
            stroke.setStrokeWidth(Math.max(1f, Ui.dpf(c, strokeDp)));
            canvas.drawRoundRect(rect, r, r, stroke);
        }
    }
}
