package com.github.tvbox.osc.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * HSV 色盘:外层色相环,内部 SV 方块(横向饱和度,纵向亮度)。
 */
class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 颜色变化回调(不含alpha) */
    var onColorChanged: ((Int) -> Unit)? = null

    private var hue = 0f
    private var sat = 1f
    private var value = 1f

    private var centerX = 0f
    private var centerY = 0f
    private var ringOuter = 0f
    private var ringInner = 0f
    private var svSize = 0f
    private var svLeft = 0f
    private var svTop = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val selectorStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF555555.toInt()
        strokeWidth = 3f
    }
    private val rectF = RectF()

    private val hueColors = intArrayOf(
        Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
    )

    init {
        isClickable = true
    }

    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
        invalidate()
    }

    fun currentColor(): Int =
        Color.HSVToColor(floatArrayOf(hue, sat, value))

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        val ringWidth = min(w, h) * 0.14f
        ringOuter = min(w, h) / 2f - ringWidth / 2f - 4f
        ringInner = ringOuter - ringWidth
        svSize = ringInner * 1.35f
        svLeft = centerX - svSize / 2f
        svTop = centerY - svSize / 2f
    }

    override fun onDraw(canvas: Canvas) {
        // 色相环
        ringPaint.shader = SweepGradient(centerX, centerY, hueColors, null)
        ringPaint.strokeWidth = ringOuter - ringInner
        canvas.drawCircle(centerX, centerY, (ringOuter + ringInner) / 2f, ringPaint)

        // SV 方块: 底色 = 当前hue满s满v
        val base = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        basePaint.color = base
        canvas.drawRect(svLeft, svTop, svLeft + svSize, svTop + svSize, basePaint)
        // 横向白色渐变(饱和度): 左白 -> 右透明
        satPaint.shader = LinearGradient(
            svLeft, svTop, svLeft + svSize, svTop,
            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        canvas.drawRect(svLeft, svTop, svLeft + svSize, svTop + svSize, satPaint)
        // 纵向黑色渐变(亮度): 上透明 -> 下黑
        valuePaint.shader = LinearGradient(
            svLeft, svTop, svLeft, svTop + svSize,
            Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
        )
        canvas.drawRect(svLeft, svTop, svLeft + svSize, svTop + svSize, valuePaint)

        // 选点: 色相环
        val hueAngle = Math.toRadians((hue - 90.0).toDouble())
        val ringR = (ringOuter + ringInner) / 2f
        val hx = centerX + (ringR * cos(hueAngle)).toFloat()
        val hy = centerY + (ringR * sin(hueAngle)).toFloat()
        canvas.drawCircle(hx, hy, ringPaint.strokeWidth / 2f, selectorPaint)
        canvas.drawCircle(hx, hy, ringPaint.strokeWidth / 2f, selectorStroke)
        // 选点: SV
        val sx = svLeft + sat * svSize
        val sy = svTop + (1f - value) * svSize
        canvas.drawCircle(sx, sy, 10f, selectorPaint)
        canvas.drawCircle(sx, sy, 10f, selectorStroke)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val d = hypot(event.x - centerX, event.y - centerY)
                if (d >= ringInner) {
                    // 色相环区域
                    val angle = atan2(event.y - centerY, event.x - centerX)
                    hue = ((Math.toDegrees(angle.toDouble()) + 360f) % 360f).toFloat() + 90f
                    hue %= 360f
                } else {
                    // SV 区域
                    val x = (event.x - svLeft).coerceIn(0f, svSize) / svSize
                    val y = (event.y - svTop).coerceIn(0f, svSize) / svSize
                    sat = x.coerceIn(0f, 1f)
                    value = (1f - y).coerceIn(0f, 1f)
                }
                invalidate()
                onColorChanged?.invoke(currentColor())
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    /** 用于SV方块区域命中判断(供外部使用) */
    fun isInSvArea(x: Float, y: Float): Boolean =
        x >= svLeft && x <= svLeft + svSize && y >= svTop && y <= svTop + svSize
}
