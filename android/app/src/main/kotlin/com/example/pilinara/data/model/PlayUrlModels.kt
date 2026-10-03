package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * Play URL models for Bilibili API
 */

@Serializable
data class PlayUrlResponse(
    val code: Int = 0,
    val message: String = "",
    val data: PlayUrlData? = null,
    /** 原始响应文本，供 Rust 侧流选择使用（不参与序列化比较） */
    val rawJson: String? = null
)

@Serializable
data class PlayUrlData(
    val quality: Int = 0,
    val format: String = "",
    val timelength: Long = 0L,
    val acceptFormat: String = "",
    val acceptDescription: List<String> = emptyList(),
    val acceptQuality: List<QualityInfo> = emptyList(),
    val dash: DashInfo? = null,
    val durl: List<DurlInfo> = emptyList()
)

@Serializable
data class QualityInfo(
    val id: Int = 0,
    val quality: Int = 0,
    val format: String = "",
    val newDescription: String = "",
    val displayDesc: String = ""
)

@Serializable
data class DashInfo(
    val duration: Long = 0L,
    val minBufferTime: Float = 0f,
    val video: List<StreamInfo> = emptyList(),
    val audio: List<StreamInfo> = emptyList()
)

@Serializable
data class StreamInfo(
    val id: Int = 0,
    val baseUrl: String? = null,
    val backupUrl: List<String> = emptyList(),
    val codecs: String = "",
    val bandwidth: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: String = "",
    val md5: String = "",
    val size: Long = 0L
)

@Serializable
data class DurlInfo(
    val url: String = "",
    val length: Long = 0L,
    val size: Long = 0L,
    val md5: String = ""
)

fun PlayUrlData.findBestVideo(): String? {
    return dash?.video
        ?.sortedByDescending { it.width }
        ?.firstOrNull()?.baseUrl
}

fun PlayUrlData.findBestAudio(): String? {
    return dash?.audio
        ?.sortedByDescending { it.bandwidth }
        ?.firstOrNull()?.baseUrl
}
