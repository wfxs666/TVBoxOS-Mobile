package com.github.tvbox.osc.ui.dialog

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.NonNull
import com.github.tvbox.osc.R
import com.github.tvbox.osc.databinding.DialogThemeColorBinding
import com.github.tvbox.osc.util.HawkConfig
import com.lxj.xpopup.core.CenterPopupView
import com.orhanobut.hawk.Hawk
import java.util.Locale

/**
 * 自定义主题颜色对话框:背景色/文字色分开,色盘 + 透明度 + 颜色代码。
 */
class ThemeColorDialog(
    @NonNull context: Context,
    private val onSave: (bgColor: Int, textColor: Int) -> Unit
) : CenterPopupView(context) {

    private lateinit var mBinding: DialogThemeColorBinding

    /** true=编辑背景颜色, false=编辑文字颜色 */
    private var editingBg = true
    private var bgColor = 0xFF567DF4.toInt()
    private var textColor = 0xFFFFFFFF.toInt()

    override fun getImplLayoutId(): Int = R.layout.dialog_theme_color

    override fun onCreate() {
        super.onCreate()
        mBinding = DialogThemeColorBinding.bind(getPopupImplView())

        val savedBg = Hawk.get<Any>(HawkConfig.THEME_COLOR, null)
        if (savedBg is Int) bgColor = savedBg
        else if (savedBg is String && savedBg.isNotBlank()) {
            try {
                bgColor = Color.parseColor(savedBg)
            } catch (e: Exception) {
            }
        }
        val savedText = Hawk.get<Any>(HawkConfig.THEME_TEXT_COLOR, null)
        if (savedText is Int) textColor = savedText
        else if (savedText is String && savedText.isNotBlank()) {
            try {
                textColor = Color.parseColor(savedText)
            } catch (e: Exception) {
            }
        }

        mBinding.btnBg.setOnClickListener { switchMode(true) }
        mBinding.btnText.setOnClickListener { switchMode(false) }

        // 色盘变化(无alpha) -> 保留当前alpha
        mBinding.colorWheel.onColorChanged = { rgb ->
            updateCurrentColor((rgb and 0x00FFFFFF) or (currentAlpha() shl 24))
            refresh()
        }

        mBinding.seekAlpha.max = 255
        mBinding.seekAlpha.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val c = currentColor()
                    updateCurrentColor(Color.argb(progress, Color.red(c), Color.green(c), Color.blue(c)))
                    refresh()
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // 颜色代码输入: 合法时同步(8位AARRGGBB或6位RRGGBB自动补FF)
        mBinding.etHex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val txt = s?.toString()?.trim() ?: ""
                val parsed = parseHex(txt)
                if (parsed != null) {
                    updateCurrentColor(parsed)
                    refresh()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        mBinding.tvCancel.setOnClickListener { dismiss() }
        mBinding.tvConfirm.setOnClickListener {
            Hawk.put(HawkConfig.THEME_COLOR, String.format("#%08X", bgColor))
            Hawk.put(HawkConfig.THEME_TEXT_COLOR, String.format("#%08X", textColor))
            Hawk.put(HawkConfig.THEME_TAG, 3)
            onSave(bgColor, textColor)
            dismiss()
        }

        switchMode(true)
        getPopupImplView().post {
            try {
                com.github.tvbox.osc.util.Utils.themePopupRoot(getPopupImplView())
            } catch (e: Exception) {
            }
        }
    }

    private fun switchMode(bg: Boolean) {
        editingBg = bg
        // 当前编辑项高亮,另一项可点击切换
        mBinding.btnBg.alpha = if (bg) 1f else 0.45f
        mBinding.btnBg.backgroundTintList = if (bg) null else android.content.res.ColorStateList.valueOf(0x22000000)
        mBinding.btnText.alpha = if (bg) 0.45f else 1f
        mBinding.btnText.backgroundTintList = if (bg) android.content.res.ColorStateList.valueOf(0x22000000) else null
        refresh()
    }

    private fun currentColor(): Int = if (editingBg) bgColor else textColor

    private fun currentAlpha(): Int = Color.alpha(currentColor())

    private fun updateCurrentColor(c: Int) {
        if (editingBg) bgColor = c else textColor = c
    }

    private fun parseHex(input: String): Int? {
        var s = input.trim().removePrefix("#").removePrefix("0x").trim()
        if (s.length == 6) s = "FF$s"
        if (s.length != 8) return null
        return try {
            s.toLong(16).toInt()
        } catch (e: Exception) {
            null
        }
    }

    /** 刷新选择器状态与预览 */
    private fun refresh() {
        val c = currentColor()
        // setColor 不触发 onColorChanged, 无回调循环
        mBinding.colorWheel.setColor(c)
        mBinding.seekAlpha.progress = Color.alpha(c)
        mBinding.tvAlpha.text = String.format(Locale.US, "%d", Color.alpha(c))
        mBinding.viewPreview.setBackgroundColor(c)
        val hexText = String.format("#%08X", c)
        mBinding.tvHexPreview.text = hexText
        val cur = mBinding.etHex.text.toString().trim().uppercase(Locale.US)
        if (cur != hexText) {
            mBinding.etHex.setText(hexText)
        }
    }
}
