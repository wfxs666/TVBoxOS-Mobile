package com.github.tvbox.osc.util;

import com.github.tvbox.osc.R;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatDelegate;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.bean.VodInfo;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;


public class Utils {

    public static boolean supportsPiPMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static int getSeriesSpanCount(List<VodInfo.VodSeries> list) {
        int spanCount = 4;
        int total = 0;
        for (VodInfo.VodSeries item : list) total += item.name.length();
        int offset = (int) Math.ceil((double) total / list.size());
        if (offset >= 12) spanCount = 1;
        else if (offset >= 8) spanCount = 2;
        else if (offset >= 4) spanCount = 3;
        else if (offset >= 2) spanCount = 4;
        return spanCount;
    }

    public static String stringForTime(long timeMs) {
//        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
//            return "00:00";
//        }
        long totalSeconds = timeMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public static List<VideoInfo> getVideoList() {
        List<VideoInfo> videoList = new ArrayList<>();
        Cursor cursor = App.getInstance().getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[] { // 查询内容
                        MediaStore.Video.Media._ID, // 视频id
                        MediaStore.Video.Media.DATA, // 视频路径
                        MediaStore.Video.Media.SIZE, // 视频字节大小
                        MediaStore.Video.Media.DISPLAY_NAME, // 视频名称 xxx.mp4
                        MediaStore.Video.Media.TITLE, // 视频标题
                        MediaStore.Video.Media.DURATION, // 视频时长
                        MediaStore.Video.Media.RESOLUTION, // 视频分辨率 X x Y格式
                        MediaStore.Video.Media.IS_PRIVATE,
                        MediaStore.Video.Media.BUCKET_ID,
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Video.Media.BOOKMARK // 上次视频播放的位置
                },
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                VideoInfo videoInfo = new VideoInfo();
                videoInfo.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                videoInfo.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
                videoInfo.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
                videoInfo.setDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)));
                videoInfo.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)));
                videoInfo.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
                videoInfo.setResolution(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)));
                videoInfo.setIsPrivate(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_PRIVATE)));
                videoInfo.setBucketId(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)));
                videoInfo.setBucketDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)));
                videoInfo.setBookmark(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BOOKMARK)));
                videoList.add(videoInfo);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return videoList;
    }

    public static boolean isDarkTheme(){
        int currentNightMode = App.getInstance().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES || AppCompatDelegate.getDefaultNightMode()==AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static void initTheme(){
        switch (Hawk.get(HawkConfig.THEME_TAG,0)) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

    }
    /**
     * 自定义主题颜色：仅 THEME_TAG==3（自定义）时返回用户选择的颜色，否则返回 -1（不应用）
     */
    public static int getThemeColor() {
        if (Hawk.get(HawkConfig.THEME_TAG, 0) == 3) {
            Object v = Hawk.get(HawkConfig.THEME_COLOR);
            if (v instanceof Integer) {
                return (Integer) v;
            }
            if (v instanceof String) {
                String color = (String) v;
                if (!android.text.TextUtils.isEmpty(color)) {
                    try {
                        return android.graphics.Color.parseColor(color);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return -1;
    }

    /** 液态玻璃模式: GLASS_MENU开启 且已设置背景图(文件可读取) */
    public static boolean isGlassOn() {
        try {
            if (!Hawk.get(HawkConfig.GLASS_MENU, false)) return false;
            Object bg = Hawk.get(HawkConfig.BG_IMAGE, "");
            if (!(bg instanceof String)) return false;
            String s = (String) bg;
            return !android.text.TextUtils.isEmpty(s) && new java.io.File(s).exists();
        } catch (Exception e) {
            return false;
        }
    }

    /** 玻璃模式下的候选色(带alpha的半透明版); 非玻璃模式原样返回 */
    public static int glassify(int color, int alpha) {
        if (!isGlassOn()) return color;
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** 弹窗统一主题化: 白色/灰白背景 -> 主题浅色调, 蓝色控件 -> 主题色 (弹窗独立Window,需单独调用) */
    public static void themePopupRoot(android.view.View root) {
        if (root == null) return;
        try {
            applyThemeRecursive(root);
            themeCardBackgrounds(root);
            themeShadowLayouts(root);
            int popupBg = isGlassOn() ? getGlassPopupBg() : getThemePageBg();
            android.graphics.drawable.Drawable bg = root.getBackground();
            if (bg instanceof android.graphics.drawable.GradientDrawable) {
                android.content.res.ColorStateList cs = ((android.graphics.drawable.GradientDrawable) bg).getColor();
                if (cs != null && isNearWhite(cs.getDefaultColor())) {
                    ((android.graphics.drawable.GradientDrawable) bg).setColor(popupBg);
                }
            } else if (bg instanceof android.graphics.drawable.ColorDrawable && isNearWhite(((android.graphics.drawable.ColorDrawable) bg).getColor())) {
                ((android.graphics.drawable.ColorDrawable) bg).setColor(popupBg);
            }
        } catch (Exception e) {
        }
    }

    /** 颜色亮度判断：返回 true 表示浅色（文字应使用深色） */
    public static boolean isColorLight(int color) {
        int r = android.graphics.Color.red(color);
        int g = android.graphics.Color.green(color);
        int b = android.graphics.Color.blue(color);
        double brightness = (r * 299 + g * 587 + b * 114) / 1000.0;
        return brightness >= 128;
    }

    /** 自定义文字颜色(仅 THEME_TAG==3 时有效),未设置返回 -1 */
    public static int getThemeTextColor() {
        if (Hawk.get(HawkConfig.THEME_TAG, 0) != 3) return -1;
        Object v = Hawk.get(HawkConfig.THEME_TEXT_COLOR);
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof String) {
            String color = (String) v;
            if (!android.text.TextUtils.isEmpty(color)) {
                try {
                    return android.graphics.Color.parseColor(color);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    /** 主题色上的文字颜色：优先用户自定义,否则按背景亮度自动黑/白 */
    public static int getThemeTextColorOrDefault(int bgColor) {
        int tc = getThemeTextColor();
        if (tc != -1) return tc;
        if (bgColor == -1) return 0xFF212121;
        return isColorLight(bgColor) ? 0xFF212121 : 0xFFFFFFFF;
    }

    public static int getThemeTextColorOrDefault() {
        return getThemeTextColorOrDefault(getThemeColor());
    }

    /** 应用主题色到 TitleBar（背景 + 文字/图标，文字色可单独自定义） */
    public static void applyTitleBarTheme(android.app.Activity activity) {
        int themeColor = getThemeColor();
        if (themeColor == -1) return;
        android.view.View bar = activity.findViewById(R.id.title_bar);
        if (bar instanceof com.hjq.bar.TitleBar) {
            com.hjq.bar.TitleBar titleBar = (com.hjq.bar.TitleBar) bar;
            titleBar.setBackgroundColor(glassify(themeColor, 0x99));
            int textColor = getThemeTextColorOrDefault(themeColor);
            if (titleBar.getTitleView() != null) titleBar.getTitleView().setTextColor(textColor);
            android.view.View left = titleBar.getLeftView();
            if (left instanceof android.widget.ImageView) {
                ((android.widget.ImageView) left).setColorFilter(textColor);
            }
            android.view.View right = titleBar.getRightView();
            if (right instanceof android.widget.ImageView) {
                ((android.widget.ImageView) right).setColorFilter(textColor);
            }
        }
    }

    /** 遍历替换灰色/白色卡片背景为主题色浅色调(递归调用,保持圆角) */
        /** ShadowLayout: 用专用setter替换底色(玻璃模式半透明, 普通模式主题浅色) */
    private static void themeShadowLayouts(android.view.View root) {
        if (root == null) return;
        try {
            if (root instanceof com.lihang.ShadowLayout) {
                com.lihang.ShadowLayout sl = (com.lihang.ShadowLayout) root;
                int c = isGlassOn() ? getGlassPopupBg() : getThemePageBg();
                sl.setLayoutBackground(c);
            }
        } catch (Exception e) {
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                themeShadowLayouts(group.getChildAt(i));
            }
        }
    }

public static void themeCardBackgrounds(android.view.View root) {
        int pageBg = isGlassOn() ? getGlassPageBg() : getThemePageBg();
        themeCardBackgroundsInner(root, pageBg);
    }

    private static void themeCardBackgroundsInner(android.view.View view, int pageBg) {
        if (view == null) return;
        // 已处理过: 跳过, 避免滚动/恢复时反复setColor引发闪屏重绘
        if (Boolean.TRUE.equals(view.getTag(R.id.tag_themed))) return;
        try {
            android.graphics.drawable.Drawable bg = view.getBackground();
            if (bg instanceof android.graphics.drawable.GradientDrawable) {
                android.graphics.drawable.GradientDrawable gd = (android.graphics.drawable.GradientDrawable) bg;
                android.content.res.ColorStateList cs = gd.getColor();
                if (cs != null) {
                    int c = cs.getDefaultColor();
                    if (isNearWhite(c)) {
                        try {
                            gd.setColor(pageBg);
                        } catch (Exception e) {
                        }
                    } else if (isGlassOn() && isDarkCard(c)) {
                        // 深色模式卡片 -> 半透明深色玻璃
                        try {
                            gd.setColor(glassify(c, 0x8C));
                        } catch (Exception e) {
                        }
                    }
                }
            }
            // LinearLayout 等纯色背景
            if (view instanceof android.widget.LinearLayout || view instanceof android.widget.FrameLayout || view instanceof android.widget.RelativeLayout || view instanceof android.widget.ScrollView) {
                if (bg != null && !(bg instanceof android.graphics.drawable.GradientDrawable)
                        && bg instanceof android.graphics.drawable.ColorDrawable) {
                    int c = ((android.graphics.drawable.ColorDrawable) bg).getColor();
                    if (isNearWhite(c)) {
                        ((android.graphics.drawable.ColorDrawable) bg).setColor(pageBg);
                    } else if (isGlassOn() && isDarkCard(c)) {
                        // 深色卡片/页面 -> 半透明深色玻璃, 透出背景图
                        ((android.graphics.drawable.ColorDrawable) bg).setColor(glassify(c, 0x8C));
                    }
                }
            }
            view.setTag(R.id.tag_themed, Boolean.TRUE);
        } catch (Exception e) {
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                themeCardBackgroundsInner(group.getChildAt(i), pageBg);
            }
        }
    }

    /** 接近白色的颜色(亮度>=0xE8)视为待替换 */
    private static boolean isNearWhite(int c) {
        int r = android.graphics.Color.red(c);
        int g = android.graphics.Color.green(c);
        int b = android.graphics.Color.blue(c);
        return r >= 0xE0 && g >= 0xE0 && b >= 0xE0;
    }

    /** 深色卡片/界面背景(深灰/深蓝黑系, 排除纯黑视频区与彩色图片) */
    private static boolean isDarkCard(int c) {
        int r = android.graphics.Color.red(c);
        int g = android.graphics.Color.green(c);
        int b = android.graphics.Color.blue(c);
        // 纯黑(视频/图片区)不处理
        if (r <= 0x10 && g <= 0x10 && b <= 0x10) return false;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        // 亮度低且低饱和度的深灰蓝黑系卡片
        return max <= 0x60 && (max - min) <= 0x22;
    }

    /** 默认正文深色文字(可读性黑/深灰), 全局替换为自定义文字色的目标 */
    private static boolean isDarkText(int c) {
        return c == 0xFF050505 || c == 0xFF000000 || c == 0xFF212121
                || c == 0xFF333333 || c == 0xFF444444 || c == 0xFF3A3A3A
                || c == 0xFF555555 || c == 0xFF666666 || c == 0xFF777777
                || c == 0xFF787878;
    }

    public static int getThemePageBg() {
        int tc = getThemeColor();
        if (tc == -1) return 0xFFFFFFFF;
        try {
            float alpha = 0.12f;
            int r = (int) (android.graphics.Color.red(tc) * alpha + 255 * (1 - alpha));
            int g = (int) (android.graphics.Color.green(tc) * alpha + 255 * (1 - alpha));
            int b = (int) (android.graphics.Color.blue(tc) * alpha + 255 * (1 - alpha));
            return android.graphics.Color.rgb(r, g, b);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    /** 液态玻璃: 无主题色时的玻璃白(半透明,透出背景图); 深色模式用深色玻璃 */
    public static int getGlassWhite() {
        return isDarkTheme() ? 0x8C212121 : 0x59FFFFFF;
    }

    /** 液态玻璃: 页面/卡片背景色(超低alpha,只保留轻微玻璃感); 深色模式用深色玻璃 */
    public static int getGlassPageBg() {
        if (isDarkTheme()) {
            // 深色模式: 深灰玻璃色带轻微主题色调, 保持界面呼吸感
            int tc = getThemeColor();
            if (tc == -1) return 0x66292B33;
            int r = (int) (android.graphics.Color.red(tc) * 0.08f + 0x21 * 0.92f);
            int g = (int) (android.graphics.Color.green(tc) * 0.08f + 0x21 * 0.92f);
            int b = (int) (android.graphics.Color.blue(tc) * 0.08f + 0x22 * 0.92f);
            return glassify(android.graphics.Color.rgb(r, g, b), 0x66);
        }
        int tc = getThemeColor();
        if (tc == -1) return getGlassWhite();
        try {
            float alpha = 0.12f;
            int r = (int) (android.graphics.Color.red(tc) * alpha + 255 * (1 - alpha));
            int g = (int) (android.graphics.Color.green(tc) * alpha + 255 * (1 - alpha));
            int b = (int) (android.graphics.Color.blue(tc) * alpha + 255 * (1 - alpha));
            return glassify(android.graphics.Color.rgb(r, g, b), 0x59);
        } catch (Exception e) {
            return getGlassWhite();
        }
    }

    /** 液态玻璃: 弹窗背景(半透明,重读性稍好); 深色模式用深色玻璃 */
    public static int getGlassPopupBg() {
        if (isDarkTheme()) {
            int tc = getThemeColor();
            if (tc == -1) return 0x8C2A2D32;
            int r = (int) (android.graphics.Color.red(tc) * 0.10f + 0x2A * 0.90f);
            int g = (int) (android.graphics.Color.green(tc) * 0.10f + 0x2D * 0.90f);
            int b = (int) (android.graphics.Color.blue(tc) * 0.10f + 0x32 * 0.90f);
            return glassify(android.graphics.Color.rgb(r, g, b), 0x8C);
        }
        int tc = getThemeColor();
        if (tc == -1) return 0x8CF7F7F7;
        try {
            float alpha = 0.12f;
            int r = (int) (android.graphics.Color.red(tc) * alpha + 255 * (1 - alpha));
            int g = (int) (android.graphics.Color.green(tc) * alpha + 255 * (1 - alpha));
            int b = (int) (android.graphics.Color.blue(tc) * alpha + 255 * (1 - alpha));
            return glassify(android.graphics.Color.rgb(r, g, b), 0x8C);
        } catch (Exception e) {
            return 0x8CF7F7F7;
        }
    }

    /** 回返回跟随主题色的选中背景(drawable);未设置主题色返回 null */
    public static android.graphics.drawable.Drawable getThemeSelectedBg(android.content.Context context) {
        int tc = getThemeColor();
        if (tc == -1) return null;
        try {
            float d = context.getResources().getDisplayMetrics().density;
            int radius = (int) (d * 25f);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setCornerRadius(radius);
            gd.setColor(glassify(tc, 0x99));
            return gd;
        } catch (Exception e) {
            return null;
        }
    }

    /** 返回跟随主题色的选中背景(圆角);未设置主题色返回默认drawable */
    public static android.graphics.drawable.Drawable getThemeSelectedBgOrDefault(android.content.Context context) {
        android.graphics.drawable.Drawable d = getThemeSelectedBg(context);
        if (d != null) return d;
        return context.getResources().getDrawable(com.github.tvbox.osc.R.drawable.bg_r_common_solid_primary);
    }

    /** API31+ 高斯模糊;低版本忽略 */
    public static void applyBlur(android.view.View view, float radius) {
        if (view == null || android.os.Build.VERSION.SDK_INT < 31) return;
        try {
            if (radius <= 0f) {
                view.setRenderEffect(null);
            } else {
                view.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                        radius, radius, android.graphics.Shader.TileMode.CLAMP));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 静态模糊加载背景图: 尺寸缩小后由ImageView拉伸显示,视觉即高斯模糊。
     * 不用RenderEffect(滚动时逐帧重算导致闪烁/拖影), 且强制跳过内存/磁盘缓存(换图立即生效)。
     */
    public static void loadBlurBg(android.content.Context ctx, android.widget.ImageView iv, java.io.File file) {
        if (ctx == null || iv == null || file == null) return;
        try {
            com.bumptech.glide.Glide.with(ctx)
                    .asBitmap()
                    .load(file)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .into(new com.bumptech.glide.request.target.SimpleTarget<android.graphics.Bitmap>(480, 960) {
                        @Override
                        public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                            iv.setImageBitmap(resource); // 小尺寸位图拉伸显示 = 自然模糊, 无RenderEffect逐帧开销
                        }

                        @Override
                        public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                        }
                    });
        } catch (Exception e) {
        }
    }

    /** 普通加载背景图(跳过缓存,换图立即生效) */
    public static void loadBg(android.content.Context ctx, android.widget.ImageView iv, java.io.File file) {
        if (ctx == null || iv == null || file == null) return;
        try {
            com.bumptech.glide.Glide.with(ctx)
                    .load(file)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .centerCrop()
                    .into(iv);
        } catch (Exception e) {
        }
    }

    /**
     * 递归应用主题色到整棵View树中的常见控件:
     * FAB背景/按钮等(仅在自定义主题色时生效)
     */
    public static void applyThemeRecursive(android.view.View root) {
        int tc = getThemeColor();
        if (tc == -1 || root == null) return;
        applyThemeRecursiveInner(root, tc);
    }

    private static void applyThemeRecursiveInner(android.view.View view, int themeColor) {
        if (view == null) return;
        try {
            // FloatingActionButton / MaterialButton: 底色为默认蓝色时替换为主题色
            if (view instanceof com.google.android.material.floatingactionbutton.FloatingActionButton) {
                ((com.google.android.material.floatingactionbutton.FloatingActionButton) view)
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeColor));
                return;
            }
            if (view instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton btn = (com.google.android.material.button.MaterialButton) view;
                android.content.res.ColorStateList tint = btn.getBackgroundTintList();
                int cur = tint == null ? 0 : tint.getDefaultColor();
                // 默认蓝 #FF567DF4 或接近值时替换
                if (cur == 0xFF567DF4 || cur == 0xFF3F51B5) {
                    btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeColor));
                }
            }
            // TextView: 强调蓝 -> 自定义文字色/主题色; 正文深色 -> 自定义文字色(全局替换)
            if (view instanceof android.widget.TextView) {
                android.widget.TextView tv = (android.widget.TextView) view;
                int c = tv.getCurrentTextColor();
                int customText = getThemeTextColor();
                if (c == 0xFF567DF4 || c == 0xFF3F51B5) {
                    tv.setTextColor(customText != -1 ? customText : themeColor);
                } else if (customText != -1 && isDarkText(c)) {
                    tv.setTextColor(customText);
                }
            }
            // DslTabLayout: 选中文字色(自定义文字色优先,否则主题色), 指示器用主题色, 未选中固定灰
            // 使用 configTabLayoutConfig 触发 dslSelector.updateStyle(), 保证所有tab刷新
            if (view instanceof com.angcyo.tablayout.DslTabLayout) {
                final com.angcyo.tablayout.DslTabLayout tab = (com.angcyo.tablayout.DslTabLayout) view;
                final int customText = getThemeTextColor();
                final int selColor = customText != -1 ? customText : themeColor;
                tab.configTabLayoutConfig(new kotlin.jvm.functions.Function1<com.angcyo.tablayout.DslTabLayoutConfig, kotlin.Unit>() {
                    @Override
                    public kotlin.Unit invoke(com.angcyo.tablayout.DslTabLayoutConfig cfg) {
                        cfg.setTabSelectColor(selColor);
                        cfg.setTabDeselectColor(0xFF999999);
                        return kotlin.Unit.INSTANCE;
                    }
                });
                tab.getTabIndicator().setIndicatorColor(themeColor);
                return;
            }
        } catch (Exception e) {
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyThemeRecursiveInner(group.getChildAt(i), themeColor);
            }
        }
    }

}
