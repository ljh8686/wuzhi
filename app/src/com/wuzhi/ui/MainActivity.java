package com.wuzhi.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wuzhi.App;
import com.wuzhi.core.DateUtil;
import com.wuzhi.core.Store;

/** 主界面：4 个 Tab（账本 / 衣柜 / 推荐 / 统计）。 */
public class MainActivity extends Activity {

    public static final int TAB_ITEMS = 0;
    public static final int TAB_CLOSET = 1;
    public static final int TAB_SUGGEST = 2;
    public static final int TAB_STATS = 3;

    private static final String[] TAB_LABELS = {"账本", "衣柜", "穿什么", "统计"};

    private FrameLayout content;
    private LinearLayout nav;
    private int tab = TAB_ITEMS;
    private boolean child;

    private ItemsPage itemsPage;
    private ClosetPage closetPage;
    private SuggestPage suggestPage;
    private StatsPage statsPage;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 21) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(0x00FFFFFF);
            w.setNavigationBarColor(0xFFF7F1E9);
            int vis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (Build.VERSION.SDK_INT >= 23) vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            w.getDecorView().setSystemUiVisibility(vis);
            // 毛玻璃底座：整窗铺一张柔化的网格渐变（或用户选的照片）
            Glass.restoreWallpaper(this);
            w.setBackgroundDrawable(Glass.backdropDrawable(this));
        }

        LinearLayout root = Ui.vbox(this);
        root.setPadding(0, statusBarHeight(), 0, 0);

        content = new FrameLayout(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0);
        clp.weight = 1;
        root.addView(content, clp);

        View hair = new View(this);
        hair.setBackgroundColor(0x1A23201C);
        root.addView(hair, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 0.6f))));

        GlassCard bar = new GlassCard(this);
        bar.radius(0).tint(0xE6FFFFFF).noBorder();
        bar.setOrientation(LinearLayout.HORIZONTAL);
        nav = bar;
        nav.setPadding(Ui.dp(this, 6), Ui.dp(this, 7), Ui.dp(this, 6), Ui.dp(this, 7));
        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        itemsPage = new ItemsPage(this);
        closetPage = new ClosetPage(this);
        suggestPage = new SuggestPage(this);
        statsPage = new StatsPage(this);

        buildNav();
        show(TAB_ITEMS);
    }

    public Store store() {
        return App.get(this).store();
    }

    /** 状态栏高度，用于把内容压到状态栏下面（毛玻璃铺满整屏）。 */
    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            int h = getResources().getDimensionPixelSize(id);
            if (h > 0) return h;
        }
        return (int) (24 * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SettingsPage.handleResult(this, requestCode, resultCode, data);
        PhotoPicker.onResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (child) {
            show(tab);
            return;
        }
        super.onBackPressed();
    }

    public void save() {
        App.get(this).save();
    }

    /** 背景（渐变或自定义照片）变了之后重新铺一层。 */
    public void applyBackdrop() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setBackgroundDrawable(Glass.backdropDrawable(this));
        }
    }

    public long today() {
        return DateUtil.today();
    }

    private void buildNav() {
        nav.removeAllViews();
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout item = Ui.vbox(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 3));
            item.setClickable(true);

            View icon = new Glyph(this, i, i == tab, 21f);
            TextView label = Ui.title(this, TAB_LABELS[i], 10.5f, i == tab ? Ui.ACCENT : Ui.MUTED);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, Ui.dp(this, 1), 0, 0);
            item.addView(icon);
            item.addView(label);

            if (i == tab) {
                item.setBackground(Ui.round(0x14C2552E, 14, this));
            }

            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { show(idx); }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.weight = 1;
            nav.addView(item, lp);
        }
    }

    public void show(int idx) {
        child = false;
        boolean animate = idx != tab || content.getChildCount() == 0;
        tab = idx;
        buildNav();
        View page;
        switch (idx) {
            case TAB_CLOSET: page = closetPage.build(); break;
            case TAB_SUGGEST: page = suggestPage.build(); break;
            case TAB_STATS: page = statsPage.build(); break;
            default: page = itemsPage.build(); break;
        }
        content.removeAllViews();
        content.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (animate) Ui.fadeIn(page);
    }

    /** 数据变化后刷新当前页。 */
    public void refresh() {
        save();
        show(tab);
    }

    /** 在内容区显示一个子页面（详情等）。 */
    public void contentView(View v) {
        child = true;
        content.removeAllViews();
        content.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Ui.fadeIn(v);
    }

    public void goSuggestWithTemp(int temp) {
        suggestPage.setTemp(temp);
        show(TAB_SUGGEST);
    }

    /** 统一内容滚动容器。 */
    public static ScrollView scroller(Activity a, View inner) {
        ScrollView s = new ScrollView(a);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.setVerticalScrollBarEnabled(false);
        s.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    public static LinearLayout page(Activity a) {
        LinearLayout l = Ui.vbox(a);
        l.setBackgroundColor(0x00000000);
        l.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 100));
        return l;
    }
}
