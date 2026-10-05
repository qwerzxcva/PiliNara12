package com.example.pilinara.danmaku

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * 弹幕渲染 View（批次L14 重写：修复此前"只 add 无渲染"的半成品）。
 * Canvas 直接绘制滚动弹幕，支持 透明度/大小倍率/暂停恢复/轨道避让。
 */
class DanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 一条渲染中的弹幕 */
    data class LiveDanmaku(
        val id: Long,
        val text: String,
        val color: Int,
        val fontSizePx: Float,
        val row: Int,
        var x: Float,           // 当前左边缘 x
        val width: Float,       // 文本宽
        val speed: Float        // px/ms
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val active = mutableListOf<LiveDanmaku>()
    private var lastFrame = 0L
    private var nextId = 1L
    private var maxRows = 8
    private var running = true

    var alphaFactor: Float = 1f     // 0..1 全局透明度
    var scaleFactor: Float = 1f     // 0.5..2 全局大小倍率
    var speedFactor: Float = 1f     // 速度倍率

    /** 计算落点轨道：找一条已腾出的行，满了就重叠最少的一行 */
    private fun pickRow(width: Float): Int {
        val rows = FloatArray(maxRows)
        for (d in active) {
            val r = d.row.coerceIn(0, maxRows - 1)
            rows[r] = maxOf(rows[r], d.x + d.width)
        }
        val edge = measuredWidth.toFloat()
        var best = 0
        var bestRight = Float.MAX_VALUE
        for (i in 0 until maxRows) {
            if (rows[i] < edge) return i
            if (rows[i] < bestRight) { bestRight = rows[i]; best = i }
        }
        return best
    }

    /** 外部追加弹幕（播放到对应时间点 / 本地发送回显） */
    fun add(text: String, color: Int, fontSizeSp: Int) {
        if (text.isBlank() || measuredWidth <= 0 || !running) return
        val fs = fontSizeSp * resources.displayMetrics.density * scaleFactor
        paint.textSize = fs
        val w = paint.measureText(text)
        val row = pickRow(w)
        // 基准速度：整屏 6 秒滚过
        val speed = (measuredWidth + w) / 6000f * speedFactor
        active.add(
            LiveDanmaku(
                id = nextId++, text = text, color = color,
                fontSizePx = fs, row = row, x = measuredWidth.toFloat(),
                width = w, speed = speed
            )
        )
        invalidate()
    }

    /** 清空（seek/切P/弹幕开关） */
    fun clearAll() {
        active.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        val dt = if (lastFrame == 0L) 16L else min(now - lastFrame, 100L)
        lastFrame = if (running) now else lastFrame
        val baseTextSize = active.firstOrNull()?.fontSizePx ?: (40f * scaleFactor)
        val rowH = baseTextSize * 1.4f
        val it2 = active.iterator()
        while (it2.hasNext()) {
            val d = it2.next()
            if (running) d.x -= d.speed * dt
            if (d.x + d.width < 0) { it2.remove(); continue }
            paint.textSize = d.fontSizePx
            paint.color = d.color
            paint.alpha = (alphaFactor * 255).toInt().coerceIn(0, 255)
            canvas.drawText(d.text, d.x, rowH * (d.row + 1), paint)
        }
        if (active.isNotEmpty() && running) invalidate()
    }

    /** 暂停/恢复（跟随播放器） */
    fun setRunning(running: Boolean) {
        this.running = running
        if (running) { lastFrame = 0L; invalidate() }
    }
}
