package com.example.pilinara.ui.danmaku

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.pilinara.R
import kotlin.math.min

/**
 * Danmaku overlay view for video player
 * Renders danmaku comments on top of video
 */
class DanmakuOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    
    data class DanmakuItem(
        val id: String,
        val mode: Int, // 1=scroll, 2=top, 3=bottom, 4=subtitle, 5=reverse
        val fontsize: Int,
        val color: Int,
        val timestamp: Float,
        val content: String,
        val uid: String
    )
    
    private val activeDanmaku = mutableStateListOf<DanmakuInstance>()
    private var isPaused = false
    private var lastFrameTime = 0L
    private val animators = mutableListOf<DanmakuAnimator>()
    
    inner class DanmakuInstance(
        val item: DanmakuItem,
        var x: Float,
        var y: Float,
        var textView: TextView,
        var speed: Float
    ) {
        var isActive = true
    }
    
    inner class DanmakuAnimator(
        private val instance: DanmakuInstance,
        private val deltaTime: Long
    ) {
        fun update() {
            if (!instance.isActive) return
            
            when (instance.item.mode) {
                1 -> { // Scroll
                    instance.x -= instance.speed * deltaTime / 1000f
                    instance.textView.x = instance.x
                    instance.textView.y = instance.y
                    
                    if (instance.x < -instance.textView.width) {
                        instance.isActive = false
                        removeView(instance.textView)
                        activeDanmaku.remove(instance)
                    }
                }
                2 -> { // Top
                    instance.textView.y = instance.y
                    // Auto-remove after duration
                }
                3 -> { // Bottom
                    instance.textView.y = instance.y
                    // Auto-remove after duration
                }
                4 -> { // Subtitle
                    // Don't animate, just show at position
                }
                5 -> { // Reverse scroll
                    instance.x += instance.speed * deltaTime / 1000f
                    instance.textView.x = instance.x
                    instance.textView.y = instance.y
                    
                    if (instance.x > measuredWidth) {
                        instance.isActive = false
                        removeView(instance.textView)
                        activeDanmaku.remove(instance)
                    }
                }
            }
        }
    }
    
    init {
        clipChildren = false
        clipToPadding = false
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Items are positioned absolutely
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }
    
    fun addDanmaku(item: DanmakuItem) {
        val textView = LayoutInflater.from(context)
            .inflate(R.layout.danmaku_item_view, this, false) as TextView
        
        textView.text = item.content
        textView.setTextColor(item.color)
        textView.textSize = item.fontsize.toFloat()
        
        // Measure text
        val paint = android.graphics.Paint().apply {
            textSize = item.fontsize.toFloat()
        }
        val textWidth = paint.measureText(item.content).toInt()
        textView.measure(
            MeasureSpec.makeMeasureSpec(textWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(item.fontsize * 2, MeasureSpec.EXACTLY)
        )
        
        // Position
        val yPos = when (item.mode) {
            2 -> 0f  // Top
            3 -> measuredHeight - item.fontsize * 2  // Bottom
            else -> (Math.random() * (measuredHeight - item.fontsize * 2)).toFloat()
        }
        
        val xPos = when (item.mode) {
            5 -> measuredWidth.toFloat()  // Reverse starts from right
            else -> measuredWidth.toFloat()  // Scroll starts from right
        }
        
        textView.x = xPos
        textView.y = yPos
        
        addView(textView)
        
        val instance = DanmakuInstance(
            item = item,
            x = xPos,
            y = yPos,
            textView = textView,
            speed = item.fontsize.toFloat() * 2 // pixels per second
        )
        
        activeDanmaku.add(instance)
    }
    
    fun clear() {
        activeDanmaku.clear()
        removeAllViews()
    }
    
    fun pause() {
        isPaused = true
    }
    
    fun resume() {
        isPaused = false
        lastFrameTime = System.currentTimeMillis()
    }
    
    fun update(currentTime: Long) {
        if (isPaused) return
        
        val deltaTime = currentTime - lastFrameTime
        lastFrameTime = currentTime
        
        activeDanmaku.forEach { instance ->
            val animator = DanmakuAnimator(instance, deltaTime)
            animator.update()
        }
    }
}
