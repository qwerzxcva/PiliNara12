package com.example.piliai.piliplus

import androidx.media3.common.C

/**
 * HDR 色调映射辅助工具（移植自 piliplus 分支）。
 *
 * 说明：Media3 1.3.1 的 `Effect` 接口与 piliplus 上游使用的版本差异较大：
 * 1.3.1 里 Effect 需通过 `toGlShaderProgram()` 返回 GL 实现，没有直接的
 * Bitmap 输入/输出管线；同时 `ExoPlayer.Builder` 在 1.3.1 不提供
 * `setVideoEffects()` 入口（该 API 在 1.5+ 才加入）。
 *
 * 因此本文件当前只保留与 Media3 1.3.1 兼容的**查询/描述辅助函数**。
 * HDR 配置（开关/算法/高光保护/动态范围扩展）已通过 StorageManager +
 * RendererPrefs 持久化与缓存，待后续接入 GL shader 时直接读取。
 */

/**
 * 检测视频是否为 HDR 内容（基于 ColorInfo.colorTransfer）。
 *
 * Media3 1.3.1 里 PQ 常量的正确名字是 `COLOR_TRANSFER_ST2084`（不是 SMPTE2084）。
 */
internal fun isHdrContent(colorInfo: androidx.media3.common.ColorInfo?): Boolean {
    if (colorInfo == null) return false
    val colorTransfer = colorInfo.colorTransfer
    return colorTransfer == C.COLOR_TRANSFER_HLG ||
        colorTransfer == C.COLOR_TRANSFER_ST2084
}

/** 获取 HDR 内容的色域描述（BT.2020 / BT.709 / unknown）。 */
internal fun getHdrColorGamutDescription(colorInfo: androidx.media3.common.ColorInfo?): String {
    if (colorInfo == null) return "unknown"
    return when (colorInfo.colorSpace) {
        C.COLOR_SPACE_BT2020 -> "BT.2020"
        C.COLOR_SPACE_BT709 -> "BT.709"
        else -> "unknown"
    }
}

/** 获取 HDR 内容的色调传输特性描述（PQ / HLG / SDR / unknown）。 */
internal fun getHdrTransferDescription(colorInfo: androidx.media3.common.ColorInfo?): String {
    if (colorInfo == null) return "unknown"
    return when (colorInfo.colorTransfer) {
        C.COLOR_TRANSFER_ST2084 -> "PQ (SMPTE ST 2084)"
        C.COLOR_TRANSFER_HLG -> "HLG"
        C.COLOR_TRANSFER_SDR -> "SDR"
        else -> "unknown"
    }
}
