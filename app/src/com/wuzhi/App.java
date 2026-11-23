package com.wuzhi;

import android.app.Application;
import android.content.Context;

import com.wuzhi.core.DateUtil;
import com.wuzhi.core.Store;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** 全局单例：持有数据 + 读写本地 JSON 文件。 */
public class App extends Application {

    private static final String FILE = "wuzhi.json";
    private Store store;

    public static App get(Context c) {
        return (App) c.getApplicationContext();
    }

    public Store store() {
        if (store == null) load();
        return store;
    }

    public File dataFile() {
        return new File(getFilesDir(), FILE);
    }

    public void load() {
        File f = dataFile();
        if (!f.exists()) {
            store = new Store();
            store.seedDemo(DateUtil.today());
            save();
            return;
        }
        String s = null;
        try {
            s = readFile(f);
        } catch (IOException e) {
            s = null;
        }
        try {
            store = Store.fromJsonString(s);
        } catch (Exception e) {
            // 数据损坏：备份坏文件，重新开始，避免直接崩
            File bad = new File(getFilesDir(), FILE + ".broken");
            //noinspection ResultOfMethodCallIgnored
            f.renameTo(bad);
            store = new Store();
        }
    }

    public void save() {
        if (store == null) return;
        File f = dataFile();
        File tmp = new File(getFilesDir(), FILE + ".tmp");
        try {
            writeFile(tmp, store.toJsonString());
            if (f.exists() && !f.delete()) { /* 忽略 */ }
            if (!tmp.renameTo(f)) {
                writeFile(f, store.toJsonString());
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        } catch (IOException e) {
            // 保存失败不应让界面崩掉
        }
    }

    public String exportJson() {
        return store().toJsonString();
    }

    /** 导入：成功返回 null，失败返回错误说明。 */
    public String importJson(String json) {
        try {
            Store s = Store.fromJsonString(json);
            if (s.items.isEmpty() && s.cloths.isEmpty() && s.logs.isEmpty()) {
                return "文件里没有可导入的数据";
            }
            store = s;
            save();
            return null;
        } catch (Exception e) {
            return "格式不对：" + e.getMessage();
        }
    }

    // ---------------------------------------------------------------- io

    private static String readFile(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            try { in.close(); } catch (IOException ignored) { }
        }
    }

    private static void writeFile(File f, String s) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(s.getBytes("UTF-8"));
            out.flush();
        } finally {
            try { out.close(); } catch (IOException ignored) { }
        }
    }
}
