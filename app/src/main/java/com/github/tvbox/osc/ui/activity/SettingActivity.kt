package com.github.tvbox.osc.ui.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.IJKCode
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.databinding.ActivitySettingBinding
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectDialogInterface
import com.github.tvbox.osc.ui.dialog.BackupDialog
import com.github.tvbox.osc.ui.dialog.LiveApiDialog
import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.ui.dialog.ThemeColorDialog
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.HistoryHelper
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.PlayerHelper
import com.github.tvbox.osc.util.Utils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup
import com.orhanobut.hawk.Hawk
import java.io.FileOutputStream
import okhttp3.HttpUrl
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File
import android.graphics.Color
import com.bumptech.glide.Glide
import android.widget.SeekBar
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class SettingActivity : BaseVbActivity<ActivitySettingBinding>() {

    private var homeRec = Hawk.get(HawkConfig.HOME_REC, 0)
    private var dnsOpt = Hawk.get(HawkConfig.DOH_URL, 0)
    private var currentLiveApi = Hawk.get(HawkConfig.LIVE_URL, "")
    override fun init() {

        mBinding.titleBar.leftView.setOnClickListener { onBackPressed() }
        mBinding.tvMediaCodec.text = Hawk.get(HawkConfig.IJK_CODEC, "")

        mBinding.tvDns.text = OkGoHelper.dnsHttpsList[Hawk.get(HawkConfig.DOH_URL, 0)]
        mBinding.tvHomeRec.text = getHomeRecName(Hawk.get(HawkConfig.HOME_REC, 0))
        mBinding.tvHistoryNum.text =
            HistoryHelper.getHistoryNumName(Hawk.get(HawkConfig.HISTORY_NUM, 0))
        mBinding.tvScaleType.text = PlayerHelper.getScaleName(Hawk.get(HawkConfig.PLAY_SCALE, 0))
        mBinding.tvPlay.text = PlayerHelper.getPlayerName(Hawk.get(HawkConfig.PLAY_TYPE, 0))
        mBinding.tvRenderType.text =
            PlayerHelper.getRenderName(Hawk.get(HawkConfig.PLAY_RENDER, 0))

        mBinding.switchPrivateBrowsing.setChecked(Hawk.get(HawkConfig.PRIVATE_BROWSING, false))
        mBinding.llPrivateBrowsing.setOnClickListener { view: View? ->
            val newConfig = !Hawk.get(HawkConfig.PRIVATE_BROWSING, false)
            mBinding.switchPrivateBrowsing.setChecked(newConfig)
            Hawk.put(HawkConfig.PRIVATE_BROWSING, newConfig)
        }

        mBinding.llLiveApi.setOnClickListener {
            XPopup.Builder(mContext)
                .autoFocusEditText(false)
                .asCustom(LiveApiDialog(this))
                .show()
        }

        val defaultBgPlayTypePos = Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0)
        val bgPlayTypes = ArrayList<String>()
        bgPlayTypes.add("关闭")
        bgPlayTypes.add("开启")
        bgPlayTypes.add("画中画")
        mBinding.tvBackgroundPlayType.text = bgPlayTypes[defaultBgPlayTypePos]
        mBinding.llBackgroundPlay.setOnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            val dialog = SelectDialog<String>(this@SettingActivity)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogInterface<String?> {
                override fun click(value: String?, pos: Int) {
                    mBinding.tvBackgroundPlayType.text = value
                    Hawk.put(HawkConfig.BACKGROUND_PLAY_TYPE, pos)
                }

                override fun getDisplay(name: String?): String {
                    return name?:""
                }
            },SelectDialogAdapter.stringDiff, bgPlayTypes, defaultBgPlayTypePos)
            dialog.show()
        }

        mBinding.tvSpeed.text = Hawk.get(HawkConfig.VIDEO_SPEED, 2.0f).toString()
        mBinding.llPressSpeed.setOnClickListener {
            val types = ArrayList<String>()
            types.add("2.0")
            types.add("3.0")
            types.add("4.0")
            types.add("5.0")
            types.add("6.0")
            types.add("8.0")
            types.add("10.0")
            val defaultPos = types.indexOf(Hawk.get(HawkConfig.VIDEO_SPEED, 2.0f).toString())
            val dialog = SelectDialog<String>(this@SettingActivity)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogInterface<String?> {
                override fun click(value: String?, pos: Int) {
                    Hawk.put(HawkConfig.VIDEO_SPEED, value?.toFloat())
                    mBinding.tvSpeed.text = value
                }

                override fun getDisplay(name: String?): String {
                    return name ?: ""
                }
            }, SelectDialogAdapter.stringDiff, types, defaultPos)
            dialog.show()
        }

        mBinding.llBackup.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            if (XXPermissions.isGranted(this@SettingActivity, Permission.MANAGE_EXTERNAL_STORAGE)) {
                val dialog = BackupDialog(this@SettingActivity)
                dialog.show()
            } else {
                XXPermissions.with(this@SettingActivity)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                val dialog = BackupDialog(this@SettingActivity)
                                dialog.show()
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                ToastUtils.showLong("获取存储权限失败,请在系统设置中开启")
                                XXPermissions.startPermissionActivity(
                                    this@SettingActivity,
                                    permissions
                                )
                            } else {
                                ToastUtils.showShort("获取存储权限失败")
                            }
                        }
                    })
            }
        }

        mBinding.llDns.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val dohUrl = Hawk.get(HawkConfig.DOH_URL, 0)
            val dialog = SelectDialog<String>(this@SettingActivity)
            dialog.setTip("请选择安全DNS")
            dialog.setAdapter(object : SelectDialogInterface<String?> {
                override fun click(value: String?, pos: Int) {
                    mBinding.tvDns.text = OkGoHelper.dnsHttpsList[pos]
                    Hawk.put(HawkConfig.DOH_URL, pos)
                    val url = OkGoHelper.getDohUrl(pos)
                    OkGoHelper.dnsOverHttps.setUrl(if (url.isEmpty()) null else HttpUrl.get(url))
                    IjkMediaPlayer.toggleDotPort(pos > 0)
                }

                override fun getDisplay(name: String?): String {
                    return name ?: ""
                }
            },SelectDialogAdapter.stringDiff, OkGoHelper.dnsHttpsList, dohUrl)
            dialog.show()
        }

        mBinding.llMediaCodec.setOnClickListener { v: View? ->
            val ijkCodes = ApiConfig.get().ijkCodes
            if (ijkCodes == null || ijkCodes.size == 0) return@setOnClickListener
            FastClickCheckUtil.check(v)
            var defaultPos = 0
            val ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "")
            for (j in ijkCodes.indices) {
                if (ijkSel == ijkCodes[j].name) {
                    defaultPos = j
                    break
                }
            }
            val dialog = SelectDialog<IJKCode>(this@SettingActivity)
            dialog.setTip("请选择IJK解码")
            dialog.setAdapter(object : SelectDialogInterface<IJKCode?> {
                override fun click(value: IJKCode?, pos: Int) {
                    value?.selected(true)
                    mBinding.tvMediaCodec.text = value?.name
                }

                override fun getDisplay(code: IJKCode?): String {
                    return code?.name ?: ""
                }
            }, object : DiffUtil.ItemCallback<IJKCode>() {
                override fun areItemsTheSame(oldItem: IJKCode, newItem: IJKCode): Boolean {
                    return oldItem === newItem
                }

                override fun areContentsTheSame(oldItem: IJKCode, newItem: IJKCode): Boolean {
                    return oldItem.name.contentEquals(newItem.name)
                }
            }, ijkCodes, defaultPos)
            dialog.show()
        }

        mBinding.llScale.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0)
            val players = ArrayList<Int>()
            players.add(0)
            players.add(1)
            players.add(2)
            players.add(3)
            players.add(4)
            players.add(5)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择画面缩放")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_SCALE, value)
                    mBinding.tvScaleType.text = value?.let { PlayerHelper.getScaleName(it) }
                }

                override fun getDisplay(value: Int?): String {
                    return PlayerHelper.getScaleName(value ?: 0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, players, defaultPos)
            dialog.show()
        }

        mBinding.llPlay.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
            var defaultPos = 0
            val players = PlayerHelper.getExistPlayerTypes()
            val renders = ArrayList<Int>()
            for (p in players.indices) {
                renders.add(p)
                if (players[p] == playerType) {
                    defaultPos = p
                }
            }
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择默认播放器")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    val thisPlayerType = players[pos]
                    Hawk.put(HawkConfig.PLAY_TYPE, thisPlayerType)
                    mBinding.tvPlay.text = PlayerHelper.getPlayerName(thisPlayerType)
                    PlayerHelper.init()
                }

                override fun getDisplay(value: Int?): String {
                    return PlayerHelper.getPlayerName(players[value?:0])
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, renders, defaultPos)
            dialog.show()
        }

        mBinding.llRender.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0)
            val renders = ArrayList<Int>()
            renders.add(0)
            renders.add(1)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择默认渲染方式")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_RENDER, value)
                    mBinding.tvRenderType.text = PlayerHelper.getRenderName(value?:0)
                    PlayerHelper.init()
                }

                override fun getDisplay(value: Int?): String {
                    return PlayerHelper.getRenderName(value?:0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, renders, defaultPos)
            dialog.show()
        }
        mBinding.llHomeRec.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.HOME_REC, 0)
            val types = ArrayList<Int>()
            types.add(0)
            types.add(1)
            types.add(2)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("主页内容显示")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.HOME_REC, value)
                    mBinding.tvHomeRec.text = getHomeRecName(value?:0)
                }

                override fun getDisplay(value: Int?): String {
                    return getHomeRecName(value?:0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, defaultPos)
            dialog.show()
        }
        
        mBinding.llHistoryNum.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.HISTORY_NUM, 0)
            val types = ArrayList<Int>()
            types.add(0)
            types.add(1)
            types.add(2)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("保留历史记录数量")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.HISTORY_NUM, value)
                    mBinding.tvHistoryNum.text = HistoryHelper.getHistoryNumName(value?:0)
                }

                override fun getDisplay(value: Int?): String {
                    return HistoryHelper.getHistoryNumName(value?:0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, defaultPos)
            dialog.show()
        }
        mBinding.llClearCache.setOnClickListener { view: View ->
            XPopup.Builder(this)
                .isDarkTheme(Utils.isDarkTheme())
                .asConfirm("提示", "确定清空吗？") { onClickClearCache(view) }.show()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mBinding.llTheme.visibility = View.GONE
        }
        val oldTheme = Hawk.get(HawkConfig.THEME_TAG, 0)
        val themes = arrayOf("跟随系统", "浅色", "深色", "自定义")
        mBinding.tvTheme.text = themes[oldTheme]
        mBinding.llTheme.setOnClickListener(View.OnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            val types = ArrayList<Int>()
            types.add(0)
            types.add(1)
            types.add(2)
            types.add(3)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择")
            dialog.setAllowClickSelected(true)
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    if (value == 3) {
                        dialog.dismiss()
                        showThemeColorDialog()
                    } else {
                        mBinding.tvTheme.text = themes[value?:0]
                        Hawk.put(HawkConfig.THEME_TAG, value)
                    }
                }

                override fun getDisplay(value: Int?): String {
                    return themes[value?:0]
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, oldTheme)
            dialog.setOnDismissListener { dialog1: DialogInterface? ->
                if (oldTheme != Hawk.get(HawkConfig.THEME_TAG, 0)) {
                    Utils.initTheme()
                    val bundle = Bundle()
                    bundle.putBoolean(IntentKey.CACHE_CONFIG_CHANGED, true)
                    jumpActivity(MainActivity::class.java, bundle)
                }
            }
            dialog.show()
        })


        val bgImage = Hawk.get(HawkConfig.BG_IMAGE, "")
        mBinding.tvBgImage.text = if (bgImage.isNullOrEmpty()) "默认" else "自定义"

        // 背景图片透明度
        val bgAlpha = Hawk.get(HawkConfig.BG_IMAGE_ALPHA, 255)
        mBinding.tvBgAlpha.text = String.format("%d%%", bgAlpha * 100 / 255)
        mBinding.llBgAlpha.setOnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            showBgAlphaDialog(bgAlpha)
        }

        // 液态玻璃(统一控制菜单栏+设置页)
        val glassMenu = Hawk.get(HawkConfig.GLASS_MENU, false)
        mBinding.tvGlassMenu.text = if (glassMenu) "开启" else "关闭"
        mBinding.llGlassMenu.setOnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            showGlassToggleDialog(HawkConfig.GLASS_MENU, mBinding.tvGlassMenu, "液态玻璃")
        }

        // 应用背景模糊(独立开关)
        val glassApp = Hawk.get(HawkConfig.GLASS_APP, false)
        mBinding.tvGlassApp.text = if (glassApp) "开启" else "关闭"
        mBinding.llGlassApp.setOnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            showGlassToggleDialog(HawkConfig.GLASS_APP, mBinding.tvGlassApp, "应用背景模糊")
        }

        applySettingGlass()

        mBinding.llBgImage.setOnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            val types = ArrayList<Int>()
            types.add(0)
            types.add(1)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("背景图片")
            dialog.setAllowClickSelected(true)
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    when (value ?: 0) {
                        0 -> {
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.type = "image/*"
                            startActivityForResult(intent, REQ_CODE_BG)
                        }
                        1 -> {
                            Hawk.put(HawkConfig.BG_IMAGE, "")
                            mBinding.tvBgImage.text = "默认"
                            ToastUtils.showShort("已清除背景图片")
                        }
                    }
                }

                override fun getDisplay(value: Int?): String {
                    return if (value == 1) "清除背景" else "从相册选择"
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, 0)
            dialog.show()
        }

        mBinding.switchVideoPurify.setChecked(Hawk.get(HawkConfig.VIDEO_PURIFY, true))
        // toggle purify video -------------------------------------
        mBinding.llVideoPurify.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val newConfig = !Hawk.get(HawkConfig.VIDEO_PURIFY, true)
            mBinding.switchVideoPurify.setChecked(newConfig)
            Hawk.put(HawkConfig.VIDEO_PURIFY, newConfig)
        }
        mBinding.switchIjkCachePlay.setChecked(Hawk.get(HawkConfig.IJK_CACHE_PLAY, false))
        mBinding.llIjkCachePlay.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val newConfig = !Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)
            mBinding.switchIjkCachePlay.setChecked(newConfig)
            Hawk.put(HawkConfig.IJK_CACHE_PLAY, newConfig)
        }
    }

    override fun onBackPressed() {
        if (homeRec != Hawk.get(HawkConfig.HOME_REC, 0) || dnsOpt != Hawk.get(
                HawkConfig.DOH_URL,
                0
            ) || currentLiveApi != Hawk.get(HawkConfig.LIVE_URL, "")
        ) { // 首页类型/dns/doh/直播源有更改,需重载页面
            //AppManager.getInstance().finishAllActivity()
            if (currentLiveApi == Hawk.get(HawkConfig.LIVE_URL, "")) { //未更改直播源,不需重载api等
                val bundle = Bundle()
                bundle.putBoolean(IntentKey.CACHE_CONFIG_CHANGED, true)
                jumpActivity(MainActivity::class.java, bundle)
            } else {
                jumpActivity(MainActivity::class.java)
            }
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        } else {
            super.onBackPressed()
        }
    }

    private fun onClickClearCache(v: View) {
        FastClickCheckUtil.check(v)
        val cachePath = FileUtils.getCachePath()
        val cacheDir = File(cachePath)
        if (!cacheDir.exists()) return
        Thread {
            try {
                FileUtils.cleanDirectory(cacheDir)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        ToastUtils.showLong("缓存已清空")
    }

    private fun getHomeRecName(type: Int): String {
        return when (type) {
            0 -> "豆瓣热播"
            1 -> "站点推荐"
            else -> "关闭"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_BG && resultCode == RESULT_OK && data?.data != null) {
            try {
                val uri = data.data!!
                val dst = File(filesDir, "bg_image.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dst).use { out -> input.copyTo(out) }
                }
                if (dst.exists()) {
                    Hawk.put(HawkConfig.BG_IMAGE, dst.absolutePath)
                    mBinding.tvBgImage.text = "自定义"
                    ToastUtils.showShort("背景图片已设置,重新打开主页生效")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showShort("设置背景图片失败")
            }
        }
    }


    /** 背景图片透明度滑条对话框 */
    private fun showBgAlphaDialog(current: Int) {
        val holder = object {
            var value = current
        }
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(60, 24, 60, 24)
        root.background = resources.getDrawable(R.drawable.bg_large_round_popup)
        val title = TextView(this)
        title.text = "背景图片透明度"
        title.setTextColor(resources.getColor(R.color.text_foreground))
        title.textSize = 16f
        root.addView(title)
        val seek = SeekBar(this)
        seek.max = 255
        seek.progress = current
        seek.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 20 }
        seek.progressTintList = android.content.res.ColorStateList.valueOf(0xFF567DF4.toInt())
        root.addView(seek)
        val tvValue = TextView(this)
        tvValue.text = String.format("%d%%", current * 100 / 255)
        tvValue.setTextColor(resources.getColor(R.color.text_sub_foreground))
        tvValue.textSize = 13f
        tvValue.gravity = Gravity.END
        root.addView(tvValue)
        val btnRow = LinearLayout(this)
        btnRow.orientation = LinearLayout.HORIZONTAL
        btnRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 16 }
        val btnOk = TextView(this)
        btnOk.text = "确 定"
        btnOk.textSize = 14f
        btnOk.setPadding(0, 12, 0, 12)
        btnOk.gravity = Gravity.CENTER
        btnOk.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        btnOk.setTextColor(resources.getColor(R.color.colorPrimary))
        btnRow.addView(btnOk)
        root.addView(btnRow)
        // 使用简单自定义Dialog:标题+滑条+确定
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(true)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                holder.value = progress
                tvValue.text = String.format("%d%%", progress * 100 / 255)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        btnOk.setOnClickListener {
            Hawk.put(HawkConfig.BG_IMAGE_ALPHA, holder.value)
            mBinding.tvBgAlpha.text = String.format("%d%%", holder.value * 100 / 255)
            ToastUtils.showShort("已保存,重新打开主页生效")
            dialog.dismiss()
        }
        dialog.show()
    }

    /** 玻璃效果开关(SelectDialog: 关闭/开启) */
    private fun showGlassToggleDialog(key: String, tv: TextView, tip: String) {
        val types = ArrayList<Int>()
        types.add(0)
        types.add(1)
        val dialog = SelectDialog<Int>(this@SettingActivity)
        dialog.setTip(tip)
        dialog.setAdapter(object : SelectDialogInterface<Int?> {
            override fun click(value: Int?, pos: Int) {
                val on = (value ?: 0) == 1
                Hawk.put(key, on)
                tv.text = if (on) "开启" else "关闭"
                // 液态玻璃开关同时刷新设置页自身背景
                if (key == HawkConfig.GLASS_MENU) {
                    applySettingGlass()
                }
                ToastUtils.showShort("已保存,重新打开主页生效")
            }

            override fun getDisplay(value: Int?): String {
                return if (value == 1) "开启" else "关闭"
            }
        }, object : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                return oldItem == newItem
            }
        }, types, if (Hawk.get(key, false)) 1 else 0)
        dialog.show()
    }

    /** 设置页液态玻璃: 显示模糊背景图(跟随GLASS_MENU总开关) */
    private fun applySettingGlass() {
        val glass = Hawk.get(HawkConfig.GLASS_MENU, false)
        val bg = Hawk.get(HawkConfig.BG_IMAGE, "")
        if (glass && !bg.isNullOrEmpty() && File(bg).exists()) {
            mBinding.ivBg.visibility = View.VISIBLE
            mBinding.ivBg.setImageAlpha(Hawk.get(HawkConfig.BG_IMAGE_ALPHA, 255))
            Glide.with(this).load(File(bg)).centerCrop().into(mBinding.ivBg)
            Utils.applyBlur(mBinding.ivBg, 28f)
            // 卡片半透明玻璃(保留圆角)
            val glassCard = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = resources.getDimension(R.dimen.common_corners)
                setColor(android.graphics.Color.argb(153, 255, 255, 255))
            }
            for (card in listOf(mBinding.card1, mBinding.card2, mBinding.card3)) {
                card.background = glassCard
            }
        } else {
            mBinding.ivBg.visibility = View.GONE
            for (card in listOf(mBinding.card1, mBinding.card2, mBinding.card3)) {
                card.setBackgroundResource(R.drawable.bg_large_round_gray)
            }
        }
    }

    private fun showThemeColorDialog() {
        XPopup.Builder(this@SettingActivity)
            .autoFocusEditText(false)
            .asCustom(ThemeColorDialog(this@SettingActivity) { bg, _ ->
                mBinding.tvTheme.text = "自定义"
                ToastUtils.showShort("主题色已保存,重新打开主页生效")
                // 触发主页重建以应用新颜色
                val bundle = Bundle()
                bundle.putBoolean(IntentKey.CACHE_CONFIG_CHANGED, true)
                jumpActivity(MainActivity::class.java, bundle)
            })
            .show()
    }
    companion object {
        private const val REQ_CODE_BG = 101
    }
}