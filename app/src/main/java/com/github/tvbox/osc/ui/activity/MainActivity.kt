package com.github.tvbox.osc.ui.activity

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Process
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.databinding.ActivityMainBinding
import com.github.tvbox.osc.ui.fragment.GridFragment
import com.github.tvbox.osc.ui.fragment.HomeFragment
import com.github.tvbox.osc.ui.fragment.MyFragment
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.Utils
import com.orhanobut.hawk.Hawk
import java.io.File
import kotlin.system.exitProcess

class MainActivity : BaseVbActivity<ActivityMainBinding>() {

    var fragments = listOf(HomeFragment(),MyFragment())
    var useCacheConfig = false
    private var exitTime = 0L

    override fun init() {

        useCacheConfig = intent.extras?.getBoolean(IntentKey.CACHE_CONFIG_CHANGED, false)?:false

        mBinding.vp.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }
        }

        mBinding.bottomNav.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            mBinding.vp.setCurrentItem(menuItem.order, false)
            true
        }
        mBinding.vp.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mBinding.bottomNav.menu.getItem(position).setChecked(true)
            }
        })

        applyBgImage()
        applyThemeColor()
        applyGlassMenu()
        applyContentPadding()
    }

    override fun onResume() {
        super.onResume()
        // 从设置页返回时刷新主题/玻璃效果
        applyBgImage()
        applyThemeColor()
        applyGlassMenu()
    }

    private var lastBgPath: String? = null

    private fun applyBgImage() {
        val bg = Hawk.get(HawkConfig.BG_IMAGE, "")
        if (bg.isNullOrEmpty() || !File(bg).exists()) {
            lastBgPath = null
            mBinding.ivBg.visibility = View.GONE
            return
        }
        // 路径未变且已显示: 不重载, 避免onResume反复解码造成闪烁
        if (lastBgPath == bg && mBinding.ivBg.visibility == View.VISIBLE) return
        lastBgPath = bg
        mBinding.ivBg.visibility = View.VISIBLE
        // 背景图片透明度 0-255
        mBinding.ivBg.setImageAlpha(Hawk.get(HawkConfig.BG_IMAGE_ALPHA, 255))
        mBinding.ivBg.alpha = 1f
        val bgFile = File(bg)
        if (Hawk.get(HawkConfig.GLASS_APP, false)) {
            Utils.loadBlurBg(this, mBinding.ivBg, bgFile)
        } else {
            Utils.loadBg(this, mBinding.ivBg, bgFile)
        }
        // 应用本身高斯模糊(API31+)
    }

    /** 底部导航图标/文字选中色跟随主题色, 背景同时换主题浅色调 */
    private fun applyThemeColor() {
        val tc = Utils.getThemeColor()
        if (tc != -1) {
            val unchecked = if (Utils.isColorLight(tc)) 0xFFB3B3B3.toInt() else 0xFF777777.toInt()
            val csl = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(tc, unchecked)
            )
            mBinding.bottomNav.itemIconTintList = csl
            mBinding.bottomNav.itemTextColor = csl
            if (!Hawk.get(HawkConfig.GLASS_MENU, false)) {
                // 非玻璃模式: 白底换主题浅色调,与卡片一致
                mBinding.bottomNav.setBackgroundColor(Utils.getThemePageBg())
            }
        } else {
            mBinding.bottomNav.itemIconTintList = null
            mBinding.bottomNav.itemTextColor = null
            if (!Hawk.get(HawkConfig.GLASS_MENU, false)) {
                mBinding.bottomNav.setBackgroundColor(Color.WHITE)
            }
        }
    }

    /** 菜单栏液态玻璃: 半透明背景浮在内容上, 滚动内容从下面穿过 */
    private fun applyGlassMenu() {
        val glass = Hawk.get(HawkConfig.GLASS_MENU, false)
        if (glass) {
            mBinding.bottomNav.background = null
            // 深色模式: 深色玻璃; 浅色模式: 半透明白
            val navGlass = if (Utils.isDarkTheme()) 0x66292B33.toInt() else 0x66FFFFFF.toInt()
            mBinding.bottomNav.setBackgroundColor(navGlass)
        } else {
            mBinding.bottomNav.setBackgroundColor(Utils.getThemePageBg())
        }
    }

    /** 内容始终避开底部导航48dp(玻璃开启时导航半透明浮于其上) */
    private fun applyContentPadding() {
        if (mBinding.vp.paddingBottom != 48) {
            mBinding.vp.setPadding(0, 0, 0, 48)
        // 硬件层缓存: 半透明底层滚动/切换时不闪烁
        try {
            mBinding.vp.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            mBinding.bottomNav.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        } catch (e: Exception) {
        }
        }
    }

    override fun onBackPressed() {
        if (mBinding.vp.currentItem == 1) {
            mBinding.vp.currentItem = 0
            return
        }
        val homeFragment = fragments[0] as HomeFragment
        if (!homeFragment.isAdded) { // 资源不足销毁重建时未挂载到activity时getChildFragmentManager会崩溃
            confirmExit()
            return
        }
        val childFragments = homeFragment.allFragments
        if (childFragments.isEmpty()) { //加载中(没有tab)
            confirmExit()
            return
        }
        val fragment: Fragment = childFragments[homeFragment.tabIndex]
        if (fragment is GridFragment) { // 首页数据源动态加载的tab
            if (!fragment.restoreView()) { // 有回退的view,先回退(AList等文件夹列表),没有可回退的,返到主页tab
                if (!homeFragment.scrollToFirstTab()) {
                    confirmExit()
                }
            }
        } else {
            confirmExit()
        }
    }

    private fun confirmExit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            ToastUtils.showShort("再按一次退出程序")
            exitTime = System.currentTimeMillis()
        } else {
            ActivityUtils.finishAllActivities(true)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }
}
