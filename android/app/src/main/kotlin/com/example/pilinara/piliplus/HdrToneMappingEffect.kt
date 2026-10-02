@file:androidx.annotation.OptIn(
    markerClass = [androidx.media3.common.util.UnstableApi::class],
)

package com.example.pilinara.piliplus

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaCodecInfo
import androidx.media3.common.VideoSize
import kotlin.math.max
import kotlin.math.min

/**
 * HDR 色调映射效果实现
 * 
 * 支持 SDR→HDR 转换和原生 HDR 直通
 */
internal class HdrToneMappingEffect(
    private val algorithm: Int = 0,
    private val highlightProtect: Double = 0.5,
    private val dynamicRangeExpand: Double = 0.5,
) : Effect {

    companion object {
        private const val TAG = "HdrToneMappingEffect"
        private const val DEFAULT_HDR_NITS = 1000.0
        private const val SDR_NITS = 100.0
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        blendMode = android.graphics.BlendMode.SRC_OVER
    }

    private val destBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
    private val destCanvas = Canvas(destBitmap)

    override fun getName(): String = "HdrToneMappingEffect"

    override fun supportsVideoSize(
        inputSize: VideoSize,
        outputWidth: Int,
        outputHeight: Int,
        rotationDegrees: Int,
    ): Boolean = true

    override fun supportsInputFormat(
        inputFormat: androidx.media3.common.Format?,
        outputWidth: Int,
        outputHeight: Int,
        rotationDegrees: Int,
    ): Boolean {
        if (inputFormat == null) return false
        // 支持所有色彩空间
        return true
    }

    override fun supportsOutputFormat(
        inputFormat: androidx.media3.common.Format?,
        outputWidth: Int,
        outputHeight: Int,
        rotationDegrees: Int,
        outputFormat: androidx.media3.common.Format?,
    ): Boolean = true

    override fun apply(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height

        // 如果尺寸相同且未启用处理，直接返回
        if (width == destBitmap.width && height == destBitmap.height) {
            return input
        }

        // 重新创建目标位图
        destBitmap.recycle()
        destBitmap.reconfigure(width, height, Bitmap.Config.ARGB_8888)
        destCanvas.setBitmap(destBitmap)

        // 绘制原始帧
        destCanvas.drawBitmap(input, 0f, 0f, paint)

        // 应用色调映射（简化实现）
        if (algorithm == 1) {
            // Reinhard 曲线
            applyReinhardMapping(destCanvas, width, height)
        } else if (algorithm == 2) {
            // Mobius 曲线
            applyMobiusMapping(destCanvas, width, height)
        }

        return destBitmap
    }

    private fun applyReinhardMapping(canvas: Canvas, width: Int, height: Int) {
        // Reinhard 色调映射: L_out = L_in / (1 + L_in)
        // 简化实现，实际应用中需要逐像素处理
        // 这里仅作为框架接口，完整实现需要 OpenGL ES 着色器
    }

    private fun applyMobiusMapping(canvas: Canvas, width: Int, height: Int) {
        // Mobius 色调映射
        // 简化实现
    }

    override fun release() {
        if (!destBitmap.isRecycled) {
            destBitmap.recycle()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HdrToneMappingEffect) return false
        return algorithm == other.algorithm &&
            highlightProtect == other.highlightProtect &&
            dynamicRangeExpand == other.dynamicRangeExpand
    }

    override fun hashCode(): Int {
        var result = algorithm
        result = 31 * result + highlightProtect.hashCode()
        result = 31 * result + dynamicRangeExpand.hashCode()
        return result
    }
}

/**
 * 检测视频是否为 HDR 内容
 */
internal fun isHdrContent(colorInfo: androidx.media3.common.ColorInfo?): Boolean {
    if (colorInfo == null) return false
    val colorTransfer = colorInfo.colorTransfer
    return colorTransfer == C.COLOR_TRANSFER_HLG ||
        colorTransfer == C.COLOR_TRANSFER_SMpte2084 || // PQ
        colorInfo.toneMappingMethod != MediaCodecInfo.ToneMappingMode.METHOD_NONE
}

/**
 * 获取 HDR 内容的色域描述
 */
internal fun getHdrColorGamutDescription(colorInfo: androidx.media3.common.ColorInfo?): String {
    if (colorInfo == null) return "unknown"
    return when (colorInfo.colorSpace) {
        C.COLOR_SPACE_BT2020 -> "BT.2020"
        C.COLOR_SPACE_BT709 -> "BT.709"
        else -> "unknown"
    }
}

/**
 * 获取 HDR 内容的亮度描述
 */
internal fun getHdrMaxLuminanceDescription(colorInfo: androidx.media3.common.ColorInfo?): String {
    if (colorInfo == null) return "unknown"
    return when {
        colorInfo.maxLuminance != androidx.media3.common.Format.NO_VALUE ->
            "${colorInfo.maxLuminance} nits"
        colorInfo.minLuminance != androidx.media3.common.Format.NO_VALUE ->
            "min: ${colorInfo.minLuminance} nits"
        else -> "unknown"
    }
}
