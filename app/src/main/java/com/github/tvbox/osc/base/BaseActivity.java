package com.github.tvbox.osc.base;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.Utils;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.bar.OnTitleBarListener;
import com.hjq.bar.TitleBar;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import me.jessyan.autosize.internal.CustomAdapt;

public abstract class BaseActivity extends AppCompatActivity implements CustomAdapt, OnTitleBarListener {
    protected Context mContext;
    private LoadService mLoadService;

    private ImmersionBar mImmersionBar;
    private TitleBar mTitleBar;
    private LoadingPopupView loadingPopup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        if (getLayoutResID()==-1){
            initVb();
        }else {
            setContentView(getLayoutResID());
        }
        mContext = this;
        AppManager.getInstance().addActivity(this);
        initStatusBar();
        initTitleBar();
        init();
        applyPageBg();
        if (!App.getInstance().isNormalStart){
            AppUtils.relaunchApp(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.applyTitleBarTheme(this);
        Utils.applyThemeRecursive(getWindow().getDecorView());
        applyPageBg();
        // 延迟重跑一次,覆盖Fragment懒加载/动态创建的内容
        final android.view.View decor = getWindow().getDecorView();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Utils.applyThemeRecursive(decor);
                    Utils.themeCardBackgrounds(decor);
                } catch (Exception e) {
                }
            }
        }, 400);
    }

    /** 自定义主题色时,将大片白底换为主题色浅色调, 并立即应用文字色(首帧即生效) */
    private void applyPageBg() {
        applyGlassBackdrop();
        int tc = Utils.getThemeColor();
        try {
            int pageBg = Utils.isGlassOn() ? Utils.getGlassPageBg() : Utils.getThemePageBg();
            // 玻璃模式下主题色未设置也执行: 白底->半透明白, 透出背景图
            if (tc != -1 || Utils.isGlassOn()) {
                // decor背景不透明化(窗口级alpha会引发滚动闪屏), 玻璃感由背景图+卡片层负责
                getWindow().getDecorView().setBackgroundColor((pageBg & 0x00FFFFFF) | 0xFF000000);
                Utils.themeCardBackgrounds(getWindow().getDecorView());
            }
            if (tc != -1) {
                // 文字色/控件色在init()后立即应用, 避免先黑后变色的闪变
                Utils.applyThemeRecursive(getWindow().getDecorView());
            }
            // 滚动容器/动画文字设硬件层: 半透明层滚动时GPU合成残影(闪烁/拖影)的根治
            enableHardwareLayers(getWindow().getDecorView());
        } catch (Exception e) {
        }
    }

    /** 半透明层在滚动/动画时容易产生GPU合成残影(闪屏/拖影); 给滚动容器与固定浮层设硬件层缓存 */
    private void enableHardwareLayers(View root) {
        try {
            enableHardwareLayersInner(root);
        } catch (Exception e) {
        }
    }

    private void enableHardwareLayersInner(android.view.View view) {
        if (view == null) return;
        try {
            if (view instanceof android.widget.ScrollView || view instanceof androidx.recyclerview.widget.RecyclerView
                    || view instanceof androidx.viewpager.widget.ViewPager
                    || view instanceof android.widget.HorizontalScrollView) {
                view.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
            }
        } catch (Exception e) {
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                enableHardwareLayersInner(group.getChildAt(i));
            }
        }
    }

    /** 液态玻璃: 页面无背景图层时,在最底层铺模糊背景图(内容透明后即可透出) */
    private void applyGlassBackdrop() {
        try {
            if (!Utils.isGlassOn()) return;
            String bg = Hawk.get(HawkConfig.BG_IMAGE, "");
            if (bg == null || bg.isEmpty() || !new java.io.File(bg).exists()) return;
            ViewGroup content = getWindow().getDecorView().findViewById(android.R.id.content);
            if (content == null) return;
            if (content.findViewWithTag("glass_backdrop") != null) return; // 已铺
            // 页面自带背景图(ivBg)时跳过,避免双层
            int ivBgId = getResources().getIdentifier("ivBg", "id", getPackageName());
            if (ivBgId != 0 && content.findViewById(ivBgId) != null) return;
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setTag("glass_backdrop");
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            iv.setImageAlpha(Hawk.get(HawkConfig.BG_IMAGE_ALPHA, 255));
            content.addView(iv, 0); // 最底层,不遮挡内容
            // 静态模糊(缩图拉伸), 不用RenderEffect, 避免滚动闪烁/拖影
            Utils.loadBlurBg(this, iv, new java.io.File(bg));
        } catch (Exception e) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {

    }


    private void initStatusBar(){
        int tc = Utils.getThemeColor();
        int textColor = Utils.getThemeTextColorOrDefault(tc);
        ImmersionBar.with(this)
                .statusBarDarkFont(tc == -1 || Utils.isColorLight(textColor))
                .titleBar(findTitleBar(getWindow().getDecorView().findViewById(android.R.id.content)))
                .navigationBarColor(R.color.white)
                .init();
    }

    private void initTitleBar(){
        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
        }
    }

    /**
     * 递归获取 ViewGroup 中的 TitleBar 对象
     */
    private TitleBar findTitleBar(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View view = group.getChildAt(i);
            if ((view instanceof TitleBar)) {
                return (TitleBar) view;
            } else if (view instanceof ViewGroup) {
                TitleBar titleBar = findTitleBar((ViewGroup) view);
                if (titleBar != null) {
                    return titleBar;
                }
            }
        }
        return null;
    }

    private TitleBar getTitleBar() {
        if (mTitleBar == null) {
            mTitleBar = findTitleBar(getWindow().getDecorView().findViewById(android.R.id.content));
        }
        return mTitleBar;
    }


    public boolean hasPermission(String permission) {
        boolean has = true;
        try {
            has = PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return has;
    }

    protected abstract int getLayoutResID();

    protected abstract void init();

    protected void initVb() {

    }

    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                }
            });
        }
    }

    protected void showLoading() {
        if (mLoadService != null) {
            mLoadService.showCallback(LoadingCallback.class);
        }
    }

    protected boolean isLoading() {
        if (mLoadService != null && mLoadService.getCurrentCallback() != null) {
            return mLoadService.getCurrentCallback().equals(LoadingCallback.class);
        }
        return false;
    }

    protected void showEmpty() {
        if (null != mLoadService) {
            mLoadService.showCallback(EmptyCallback.class);
        }
    }

    protected void showSuccess() {
        if (null != mLoadService) {
            mLoadService.showSuccess();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        AppManager.getInstance().finishActivity(this);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz) {
        Intent intent = new Intent(mContext, clazz);
        startActivity(intent);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz, Bundle bundle) {
        if (DetailActivity.class.isAssignableFrom(clazz) && Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 2) {
            //1.重新打开singleTask的页面(关闭小窗) 2.关闭画中画，重进detail再开启画中画会闪退
            ActivityUtils.finishActivity(DetailActivity.class);
        }
        Intent intent = new Intent(mContext, clazz);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    protected String getAssetText(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assets = getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assets.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public float getSizeInDp() {
        return isBaseOnWidth() ? 360 : 720;
    }

    @Override
    public boolean isBaseOnWidth() {
        return true;
    }

    @Override
    public void onLeftClick(TitleBar titleBar) {
        finish();
    }


    /**
     * 显示加载框
     */
    public void showLoadingDialog() {
        if (loadingPopup == null) {
            loadingPopup = new XPopup.Builder(this)
                    .isLightNavigationBar(true)
                    .hasShadowBg(false)
                    .asLoading();
        }
        loadingPopup.show();
    }

    /**
     * 隐藏加载框
     */
    public void dismissLoadingDialog() {
        if (loadingPopup != null && loadingPopup.isShow()) {
            loadingPopup.dismiss();
        }
    }

}
