package com.wuzhi.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wuzhi.App;
import com.wuzhi.Backup;
import com.wuzhi.core.Cloth;
import com.wuzhi.core.DateUtil;
import com.wuzhi.core.Store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 设置：备份导出/导入、温度容差、城市、示例数据、清空。 */
public class SettingsPage {

    /** 系统文件选择器请求码 */
    public static final int REQ_EXPORT = 9101;
    public static final int REQ_IMPORT = 9102;
    public static final int REQ_ZIP_EXPORT = 9103;
    public static final int REQ_ZIP_IMPORT = 9104;
    /** 选背景照片 */
    public static final int REQ_BG = 9105;

    private final MainActivity a;

    public SettingsPage(MainActivity a) { this.a = a; }

    private File dir() {
        File d = a.getExternalFilesDir(null);
        if (d == null) d = a.getFilesDir();
        return d;
    }

    public void open() {
        LinearLayout page = MainActivity.page(a);
        Store st = a.store();

        LinearLayout head = Ui.hbox(a);
        TextView back = Ui.buttonSmall(a, "‹ 返回", 0xFFF0EAE0, Ui.INK2);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { a.show(MainActivity.TAB_STATS); }
        });
        head.addView(back);
        TextView t = Ui.title(a, "设置", 20, Ui.INK);
        t.setPadding(Ui.dp(a, 12), 0, 0, 0);
        head.addView(t, Ui.lpw(1));
        page.addView(head);
        page.addView(Ui.spacer(a, 14));

        // ---------------- 偏好
        LinearLayout pref = Ui.card(a);
        pref.addView(Ui.title(a, "偏好", 14, Ui.INK));
        pref.addView(Ui.spacer(a, 6));
        pref.addView(settingRow("推荐温度容差", "±" + st.tempTolerance + "°C", "相同温度推荐时允许的浮动范围", new Runnable() {
            @Override public void run() { editTolerance(); }
        }));
        pref.addView(Ui.divider(a));
        pref.addView(settingRow("所在城市", st.city.isEmpty() ? "未设置" : st.city, "仅用于显示", new Runnable() {
            @Override public void run() { editCity(); }
        }));
        pref.addView(Ui.divider(a));
        pref.addView(settingRow("背景图片", Glass.wallpaperActive(a) ? "自定义照片" : "默认渐变",
                "毛玻璃卡片会透出这张图（建议浅色照片）", new Runnable() {
            @Override public void run() { chooseWallpaper(); }
        }));
        page.addView(pref);
        page.addView(Ui.spacer(a, 12));

        // ---------------- 备份
        LinearLayout bak = Ui.card(a);
        bak.addView(Ui.title(a, "备份与恢复", 14, Ui.INK));
        TextView hint = Ui.text(a, "数据只存在这台手机上。换手机或重装前，先导出一份。", 11, Ui.MUTED);
        hint.setPadding(0, Ui.dp(a, 5), 0, Ui.dp(a, 10));
        bak.addView(hint);

        bak.addView(settingRow("导出备份文件", "存到手机里", "用系统文件选择器挑个位置（下载 / 微信 / 网盘）", new Runnable() {
            @Override public void run() { exportTo(); }
        }));
        bak.addView(Ui.divider(a));
        bak.addView(settingRow("从文件恢复", "选一个 .json", "会覆盖当前所有数据", new Runnable() {
            @Override public void run() { importFrom(); }
        }));
        bak.addView(settingRow("完整备份（含照片）", "打包成一个 zip", "换手机时拷这一个文件就能全部搬走", new Runnable() {
            @Override public void run() { exportZipTo(); }
        }));
        bak.addView(settingRow("从完整备份恢复", "选一个 .wuzhi.zip", "数据和照片一起还原", new Runnable() {
            @Override public void run() { importZipFrom(); }
        }));
        bak.addView(Ui.divider(a));
        bak.addView(settingRow("复制 JSON 到剪贴板", "", "方便发到微信 / 邮箱保存", new Runnable() {
            @Override public void run() { copyJson(); }
        }));
        bak.addView(Ui.divider(a));
        bak.addView(settingRow("从剪贴板恢复", "", "粘贴之前复制的 JSON", new Runnable() {
            @Override public void run() { pasteJson(); }
        }));
        bak.addView(Ui.divider(a));
        bak.addView(settingRow("快速备份到应用目录", lastBackupText(), "不需要选位置，留一份在本机", new Runnable() {
            @Override public void run() { exportFile(); }
        }));
        page.addView(bak);
        page.addView(Ui.spacer(a, 12));

        // ---------------- 数据
        LinearLayout data = Ui.card(a);
        data.addView(Ui.title(a, "数据", 14, Ui.INK));
        data.addView(Ui.spacer(a, 6));
        data.addView(settingRow("重新载入示例数据", "", "会清空现有数据，换成演示内容", new Runnable() {
            @Override public void run() { confirmSeed(); }
        }));
        data.addView(Ui.divider(a));
        LinearLayout danger = settingRow("清空所有数据", "", "删除全部物品、衣服和记录", new Runnable() {
            @Override public void run() { confirmClear(); }
        });
        page.addView(data);
        page.addView(Ui.spacer(a, 16));

        // ---------------- 关于
        LinearLayout about = Ui.card(a);
        about.addView(Ui.title(a, "关于「物资」", 14, Ui.INK));
        TextView ab = Ui.text(a, "记录每件东西的真实成本，和衣柜里每件衣服的价值。\n数据全部保存在本机，不联网、不上传。", 12, Ui.INK2);
        ab.setPadding(0, Ui.dp(a, 8), 0, 0);
        ab.setLineSpacing(Ui.dpf(a, 3), 1f);
        about.addView(ab);
        TextView cnt = Ui.text(a, "当前：" + st.items.size() + " 件物品 · " + st.cloths.size() + " 件衣服 · "
                + st.logs.size() + " 条穿着记录", 11, Ui.MUTED);
        cnt.setPadding(0, Ui.dp(a, 10), 0, 0);
        about.addView(cnt);
        page.addView(about);

        a.contentView(MainActivity.scroller(a, page));
    }

    /** 背景照片在 PhotoStore 里的固定 id（同一时间只有一张）。 */
    private static final String WALL_ID = "glass_wall";

    /** 选一张照片当毛玻璃底图 / 恢复默认渐变。 */
    private void chooseWallpaper() {
        boolean has = Glass.wallpaperActive(a);
        String[] items = has
                ? new String[]{"从相册选择", "恢复默认渐变"}
                : new String[]{"从相册选择"};
        new AlertDialog.Builder(a)
                .setTitle("背景图片")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            PhotoPicker.pick(a, REQ_BG, WALL_ID, new Runnable() {
                                @Override public void run() { applyWallpaper(); }
                            });
                        } else {
                            Glass.setWallpaper(a, null);
                            a.applyBackdrop();
                            a.refresh();
                        }
                    }
                })
                .show();
    }

    private void applyWallpaper() {
        Glass.setWallpaper(a, WALL_ID);
        a.applyBackdrop();
        a.refresh();
    }

    private LinearLayout settingRow(String label, String value, String hint, final Runnable onTap) {
        LinearLayout row = Ui.hbox(a);
        row.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 10));
        row.setClickable(true);
        LinearLayout mid = Ui.vbox(a);
        mid.addView(Ui.text(a, label, 13.5f, Ui.INK));
        if (hint != null && !hint.isEmpty()) {
            TextView h = Ui.text(a, hint, 10.5f, Ui.MUTED);
            h.setPadding(0, Ui.dp(a, 3), 0, 0);
            mid.addView(h);
        }
        row.addView(mid, Ui.lpw(1));
        if (value != null && !value.isEmpty()) {
            TextView v = Ui.text(a, value, 12.5f, Ui.MUTED);
            v.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 6), 0);
            row.addView(v);
        }
        TextView arrow = Ui.text(a, "›", 18, Ui.MUTED);
        row.addView(arrow);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onTap.run(); }
        });
        return row;
    }

    // ------------------------------------------------------------ 偏好

    private void editTolerance() {
        final Sheet s = new Sheet(a, "推荐温度容差", "比如设成 ±2°C，找 20°C 时会参考 18~22°C 的记录");
        final EditText n = s.number("容差（°C）", String.valueOf(a.store().tempTolerance), "2");
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                int v = parseInt(sheet.text(n), a.store().tempTolerance);
                if (v < 0) v = 0;
                if (v > 15) v = 15;
                a.store().tempTolerance = v;
                sheet.dismiss();
                a.refresh();
            }
        });
        s.show();
    }

    private void editCity() {
        final Sheet s = new Sheet(a, "所在城市", "只用来显示，不影响计算");
        final EditText c = s.field("城市", a.store().city, "比如 济南", InputType.TYPE_CLASS_TEXT);
        s.onOk(new Sheet.OnOk() {
            @Override public void onOk(Sheet sheet) {
                a.store().city = sheet.text(c);
                sheet.dismiss();
                a.refresh();
            }
        });
        s.show();
    }

    // ------------------------------------------------------------ 备份

    private String stamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
    }

    private String lastBackupText() {
        File[] fs = dir().listFiles();
        long newest = 0;
        if (fs != null) {
            for (File f : fs) {
                if (f.isFile() && f.getName().endsWith(".json") && f.lastModified() > newest) {
                    newest = f.lastModified();
                }
            }
        }
        if (newest == 0) return "还没有";
        return new SimpleDateFormat("MM-dd HH:mm", Locale.US).format(new Date(newest));
    }

    /** 用系统文件选择器导出（可存到下载 / 微信 / 网盘）。 */
    void exportTo() {
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE, "wuzhi-" + stamp() + ".json");
            a.startActivityForResult(i, REQ_EXPORT);
        } catch (Exception e) {
            toast("这台手机没有文件选择器，改用「快速备份到应用目录」");
        }
    }

    /** 用系统文件选择器导入。 */
    void importFrom() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            a.startActivityForResult(i, REQ_IMPORT);
        } catch (Exception e) {
            toast("这台手机没有文件选择器");
        }
    }

    /** 由 MainActivity.onActivityResult 转发。 */
    public static void handleResult(MainActivity a, int req, int res, Intent data) {
        if (res != Activity.RESULT_OK || data == null || data.getData() == null) return;
        SettingsPage p = new SettingsPage(a);
        if (req == REQ_EXPORT) p.writeTo(data.getData());
        else if (req == REQ_IMPORT) p.readFrom(data.getData());
        else if (req == REQ_ZIP_EXPORT) p.writeZipTo(data.getData());
        else if (req == REQ_ZIP_IMPORT) p.readZipFrom(data.getData());
    }

    void exportZipTo() {
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_TITLE, "wuzhi-" + stamp() + Backup.SUFFIX);
            a.startActivityForResult(i, REQ_ZIP_EXPORT);
        } catch (Exception e) {
            toast("这台手机没有文件选择器");
        }
    }

    void importZipFrom() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            a.startActivityForResult(i, REQ_ZIP_IMPORT);
        } catch (Exception e) {
            toast("这台手机没有文件选择器");
        }
    }

    private void writeZipTo(Uri uri) {
        try {
            java.io.OutputStream out = a.getContentResolver().openOutputStream(uri, "wt");
            if (out == null) { toast("写入失败"); return; }
            int photos;
            try {
                photos = Backup.exportZip(a, out);
            } finally {
                try { out.close(); } catch (Exception ignored) { }
            }
            toast("已导出完整备份（含 " + photos + " 张照片）");
        } catch (Exception e) {
            toast("导出失败：" + e.getMessage());
        }
    }

    private void readZipFrom(Uri uri) {
        String err;
        try {
            java.io.InputStream in = a.getContentResolver().openInputStream(uri);
            if (in == null) { toast("读取失败"); return; }
            try {
                err = Backup.importZip(a, in);
            } finally {
                try { in.close(); } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            toast("读取失败：" + e.getMessage());
            return;
        }
        if (err != null) {
            toast(err);
            return;
        }
        toast("已恢复完整备份");
        a.refresh();
    }

    private void writeTo(Uri uri) {
        String json = App.get(a).exportJson();
        try {
            java.io.OutputStream out = a.getContentResolver().openOutputStream(uri, "wt");
            if (out == null) { toast("写入失败"); return; }
            try { out.write(json.getBytes("UTF-8")); out.flush(); } finally { out.close(); }
            toast("已导出备份（" + (json.length() / 1024 + 1) + " KB）");
        } catch (Exception e) {
            toast("写入失败：" + e.getMessage());
        }
    }

    private void readFrom(Uri uri) {
        try {
            InputStream in = a.getContentResolver().openInputStream(uri);
            if (in == null) { toast("读取失败"); return; }
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            } finally { in.close(); }
            doImport(bo.toString("UTF-8"));
        } catch (Exception e) {
            toast("读取失败：" + e.getMessage());
        }
    }

    private void exportFile() {
        String json = App.get(a).exportJson();
        String name = "wuzhi-" + stamp() + ".json";
        File f = new File(dir(), name);
        try {
            FileOutputStream out = new FileOutputStream(f);
            try { out.write(json.getBytes("UTF-8")); out.flush(); } finally { out.close(); }
            toast("已导出：\n" + f.getAbsolutePath());
        } catch (IOException e) {
            toast("导出失败：" + e.getMessage());
        }
    }

    private void importFile() {
        File[] files = dir().listFiles();
        final List<File> list = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".json")) list.add(f);
            }
        }
        if (list.isEmpty()) {
            toast("还没有备份文件，先点「导出到文件」");
            return;
        }
        list.sort((x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        final String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            File f = list.get(i);
            names[i] = f.getName() + "  (" + (f.length() / 1024) + " KB)";
        }
        new AlertDialog.Builder(a)
                .setTitle("选择要恢复的备份")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        doImport(read(list.get(which)));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doImport(String json) {
        String err = App.get(a).importJson(json);
        if (err == null) {
            toast("恢复成功");
            a.refresh();
        } else {
            toast("恢复失败：" + err);
        }
    }

    private void copyJson() {
        String json = App.get(a).exportJson();
        ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("物资备份", json));
            toast("已复制（" + (json.length() / 1024 + 1) + " KB）");
        } else {
            toast("复制失败：剪贴板不可用");
        }
    }

    private void pasteJson() {
        ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null) {
            toast("剪贴板是空的");
            return;
        }
        ClipData clip = cm.getPrimaryClip();
        if (clip.getItemCount() == 0) { toast("剪贴板是空的"); return; }
        CharSequence txt = clip.getItemAt(0).coerceToText(a);
        if (txt == null || txt.length() == 0) { toast("剪贴板里没有文字"); return; }
        doImport(txt.toString());
    }

    // ------------------------------------------------------------ 危险操作

    private void confirmSeed() {
        new AlertDialog.Builder(a)
                .setTitle("重新载入示例数据？")
                .setMessage("会清空现在的所有数据，换成演示内容。此操作不可撤销。")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        Store st = a.store();
                        st.items.clear();
                        st.cloths.clear();
                        st.outfits.clear();
                        st.logs.clear();
                        st.seedDemo(a.today());
                        a.refresh();
                        toast("已载入示例数据");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(a)
                .setTitle("清空所有数据？")
                .setMessage("物品、衣服、穿着记录会全部删除，无法恢复。建议先导出一份备份。")
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        Store st = a.store();
                        st.items.clear();
                        st.cloths.clear();
                        st.outfits.clear();
                        st.logs.clear();
                        st.lastWeatherTemp = Cloth.NO_TEMP;
                        a.refresh();
                        toast("已清空");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ------------------------------------------------------------ io

    private String read(File f) {
        try {
            InputStream in = new FileInputStream(f);
            try {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            } finally { in.close(); }
        } catch (IOException e) {
            return null;
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private void toast(String s) {
        Toast.makeText(a, s, Toast.LENGTH_LONG).show();
    }
}
