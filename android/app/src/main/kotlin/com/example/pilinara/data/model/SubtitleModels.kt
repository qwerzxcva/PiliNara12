package com.example.pilinara.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ==================== 播放器字幕（批次K，/x/player/v2） ====================

@Serializable
data class PlayerV2Response(
    val code: Int = 0,
    val message: String = "",
    val data: PlayerV2Data? = null
)

@Serializable
data class PlayerV2Data(
    val subtitle: SubtitleInfo? = null
)

@Serializable
data class SubtitleInfo(
    val allow_submit: Boolean = false,
    val subtitles: List<SubtitleItem> = emptyList()
)

@Serializable
data class SubtitleItem(
    val id: Long = 0,
    @SerialName("lan") val lang: String = "",
    @SerialName("lan_doc") val langDoc: String = "",
    @SerialName("subtitle_url") val subtitleUrl: String = "",
    @SerialName("is_lock") val isLock: Boolean = false
)

/** 字幕内容（subtitle_url 返回的 JSON） */
@Serializable
data class SubtitleBody(
    val body: List<SubtitleCue> = emptyList()
)

@Serializable
data class SubtitleCue(
    @SerialName("from") val from: Float = 0f,
    @SerialName("to") val to: Float = 0f,
    val content: String = ""
)
