package com.wuzhi.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wuzhi.core.PhotoStore;

import java.util.HashMap;
import java.util.Map;

/**
 * 相册选图 + "选照片 / 查看大图 / 删除"菜单。
 *
 * <p>用系统 SAF 选择器（{@link Intent#ACTION_GET_CONTENT} + {@code image/*}），
 * 不申请任何存储权限；选中的图立刻被 {@link PhotoStore} 拷进 App 私有目录，
 * 所以不需要持久化 Uri 授权。</p>
 *
 * <p>接线方式（主智能体在 Activity 里）：</p>
 * <pre>
 * // 弹菜单
 * PhotoPicker.showSheet(this, id, () -&gt; reload());
 * // 或者自己起选择器（记得把 id 带上，结果里要用）
 * PhotoPicker.pick(this, PhotoPicker.REQ_PICK, id);
 *
 * &#64;Override protected void onActivityResult(int req, int res, Intent data) {
 *     super.onActivityResult(req, res, data);
 *     String path = PhotoPicker.onResult(this, req, res, data); // 用 pick 时登记的 id
 *     if (path != null) reload();
 * }
 * </pre>
 */
public final class PhotoPicker {

    /** 默认 requestCode。也可以传自己的，只要 onActivityResult 里对得上。 */
    public static final int REQ_PICK = 0x5048;

    private static final int ACT_PICK = 0;
    private static final int ACT_VIEW = 1;
    private static final int ACT_DELETE = 2;

    /** requestCode -> 这次选图要存到哪个 id + 成功后回调。 */
    private static final Map<Integer, Pending> PENDING = new HashMap<>();

    private PhotoPicker() {}

    // ------------------------------------------------------------ 选择

    /** 系统相册选择器的 Intent（供需要自己 startActivityForResult 的场景）。 */
    public static Intent intent() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        return i;
    }

    /** 只负责打开相册；结果里的 id 由调用方在 onResult 时显式传入。 */
    public static void pick(Activity a, int requestCode) {
        if (!alive(a)) return;
        try {
            a.startActivityForResult(intent(), requestCode);
        } catch (Throwable t) {
            toast(a, "没有可用的相册应用");
        }
    }

    /** 打开相册并登记 id，之后可用 4 参 {@link #onResult(Activity, int, int, Intent)} 收结果。 */
    public static void pick(Activity a, int requestCode, String id) {
        pick(a, requestCode, id, null);
    }

    /** 打开相册，登记 id 与成功后回调（回调在主线程执行）。 */
    public static void pick(Activity a, int requestCode, String id, Runnable onChanged) {
        if (id == null || id.length() == 0) return;
        put(requestCode, new Pending(id, onChanged));
        pick(a, requestCode);
    }

    // ------------------------------------------------------------ 结果

    /**
     * 处理 onActivityResult。
     *
     * @param id 照片归属 id；传 null 时用 {@link #pick(Activity, int, String)} 登记的 id
     * @return 保存后的绝对路径；取消 / 失败 / 没登记 id 都返回 null
     */
    public static String onResult(Activity a, int requestCode, int resultCode, Intent data, String id) {
        Pending p = take(requestCode);
        String useId = (id != null && id.length() > 0) ? id : (p == null ? null : p.id);
        if (useId == null) return null;
        if (!alive(a)) return null;
        if (resultCode != Activity.RESULT_OK || data == null) return null;

        Uri uri = data.getData();
        if (uri == null) {
            // 少数相册把图放在 clipData 里
            try {
                ClipData cd = data.getClipData();
                if (cd != null && cd.getItemCount() > 0) uri = cd.getItemAt(0).getUri();
            } catch (Throwable ignored) {
            }
        }
        if (uri == null) return null;

        String path = PhotoStore.get(a).importFromUri(uri, useId);
        if (path == null) {
            toast(a, "照片导入失败");
            return null;
        }
        if (p != null && p.onChanged != null) {
            try {
                p.onChanged.run();
            } catch (Throwable ignored) {
            }
        }
        return path;
    }

    /** 4 参版本：id 来自 {@link #pick(Activity, int, String)} / {@link #showSheet} 的登记。 */
    public static String onResult(Activity a, int requestCode, int resultCode, Intent data) {
        return onResult(a, requestCode, resultCode, data, (String) null);
    }

    // ------------------------------------------------------------ 菜单

    /**
     * 底部三选一菜单：选照片 / 查看大图 / 删除照片。
     * 没有照片时后两项点了会提示"还没有照片"。
     *
     * @param onChanged 照片变更后的回调（主线程），可为 null
     */
    public static void showSheet(final Activity a, final String id, final Runnable onChanged) {
        if (!alive(a) || id == null || id.length() == 0) return;
        final PhotoStore ps = PhotoStore.get(a);
        final boolean has = ps.has(id);
        CharSequence[] items = {"选照片", "查看大图", "删除照片"};
        try {
            AlertDialog.Builder b = new AlertDialog.Builder(a);
            b.setTitle("照片");
            b.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    if (which == ACT_PICK) {
                        pick(a, REQ_PICK, id, onChanged);
                    } else if (which == ACT_VIEW) {
                        if (!has) {
                            toast(a, "还没有照片");
                            return;
                        }
                        showBig(a, ps.pathFor(id));
                    } else if (which == ACT_DELETE) {
                        if (!has) {
                            toast(a, "还没有照片");
                            return;
                        }
                        confirmDelete(a, id, onChanged);
                    }
                }
            });
            b.setNegativeButton("取消", null);
            b.show();
        } catch (Throwable t) {
            toast(a, "打开菜单失败");
        }
    }

    // ------------------------------------------------------------ 查看大图

    /** 用全屏 Dialog 看大图，点击任意处关闭（不新建 Activity）。 */
    public static void showBig(final Activity a, String path) {
        if (!alive(a)) return;
        if (path == null || path.length() == 0) {
            toast(a, "还没有照片");
            return;
        }
        int w = a.getResources().getDisplayMetrics().widthPixels;
        int h = a.getResources().getDisplayMetrics().heightPixels;
        final Bitmap bmp = PhotoStore.get(a).load(path, Math.max(720, Math.max(w, h)));
        if (bmp == null) {
            toast(a, "照片读取失败");
            return;
        }
        try {
            final Dialog d = new Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
            FrameLayout root = new FrameLayout(a);
            root.setBackgroundColor(0xF2000000);

            ImageView iv = new ImageView(a);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageBitmap(bmp);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            int pad = dp(a, 12);
            lp.setMargins(pad, dp(a, 40), pad, dp(a, 56));
            root.addView(iv, lp);

            TextView hint = new TextView(a);
            hint.setText("点击任意处关闭");
            hint.setTextSize(12);
            hint.setTextColor(0xFFBDB5A8);
            FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            hp.bottomMargin = dp(a, 24);
            root.addView(hint, hp);

            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                }
            });
            d.setContentView(root);
            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface di) {
                    iv.setImageDrawable(null);
                    if (!bmp.isRecycled()) bmp.recycle();
                }
            });
            d.show();
            Window win = d.getWindow();
            if (win != null) {
                win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        } catch (Throwable t) {
            if (!bmp.isRecycled()) bmp.recycle();
            toast(a, "查看大图失败");
        }
    }

    /** 按 id 看大图。 */
    public static void showBigById(Activity a, String id) {
        if (!alive(a)) return;
        showBig(a, PhotoStore.get(a).pathFor(id));
    }

    // ------------------------------------------------------------ 内部

    private static void confirmDelete(final Activity a, final String id, final Runnable onChanged) {
        try {
            AlertDialog.Builder b = new AlertDialog.Builder(a);
            b.setTitle("删除照片");
            b.setMessage("删除后不可恢复，确定吗？");
            b.setNegativeButton("取消", null);
            b.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    PhotoStore.get(a).delete(id);
                    toast(a, "已删除");
                    if (onChanged != null) {
                        try {
                            onChanged.run();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            });
            b.show();
        } catch (Throwable t) {
            toast(a, "删除失败");
        }
    }

    private static final class Pending {
        final String id;
        final Runnable onChanged;

        Pending(String id, Runnable onChanged) {
            this.id = id;
            this.onChanged = onChanged;
        }
    }

    private static void put(int requestCode, Pending p) {
        synchronized (PENDING) {
            PENDING.put(requestCode, p);
        }
    }

    private static Pending take(int requestCode) {
        synchronized (PENDING) {
            return PENDING.remove(requestCode);
        }
    }

    private static boolean alive(Activity a) {
        if (a == null) return false;
        try {
            return !a.isFinishing() && !a.isDestroyed();
        } catch (Throwable t) {
            return false;
        }
    }

    private static int dp(Activity a, float v) {
        return Math.round(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, v, a.getResources().getDisplayMetrics()));
    }

    private static void toast(Activity a, String msg) {
        try {
            Toast.makeText(a, msg, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
