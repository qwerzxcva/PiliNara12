package com.example.pilinara.danmaku

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pilinara.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom Danmaku View for video playback
 * Replaces Flutter canvas_danmaku
 */
class DanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    
    private val danmakuList = ConcurrentHashMap<Long, DanmakuItem>()
    private val activeDanmaku = mutableListOf<DanmakuInstance>()
    private var isDrawing = false
    
    data class DanmakuItem(
        val id: String,
        val mode: Int,
        val fontsize: Int,
        val color: Int,
        val timestamp: Float,
        val content: String,
        val uid: String
    )
    
    inner class DanmakuInstance(
        val item: DanmakuItem,
        var x: Float,
        var y: Float,
        var textView: TextView
    )
    
    init {
        clipChildren = false
        clipToPadding = false
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Danmaku items are positioned absolutely
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }
    
    fun addDanmaku(item: DanmakuItem) {
        val textView = LayoutInflater.from(context)
            .inflate(R.layout.danmaku_item, this, false) as TextView
        
        textView.text = item.content
        textView.setTextColor(item.color)
        textView.textSize = item.fontsize.toFloat()
        
        val width = measureTextWidth(item.content, item.fontsize)
        val height = item.fontsize * 2
        
        textView.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        
        addView(textView)
        
        val instance = DanmakuInstance(
            item = item,
            x = measuredWidth.toFloat(),
            y = (Math.random() * (measuredHeight - height)).toFloat(),
            textView = textView
        )
        
        activeDanmaku.add(instance)
        danmakuList[item.id.toLong()] = item
    }
    
    fun clear() {
        activeDanmaku.clear()
        danmakuList.clear()
        removeAllViews()
    }
    
    fun resume() {
        isDrawing = true
    }
    
    fun pause() {
        isDrawing = false
    }
    
    private fun measureTextWidth(text: String, fontSize: Int): Int {
        val paint = android.graphics.Paint().apply {
            textSize = fontSize.toFloat()
        }
        return paint.measureText(text).toInt()
    }
}
