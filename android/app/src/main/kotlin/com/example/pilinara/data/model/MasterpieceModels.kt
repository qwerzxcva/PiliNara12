package com.example.pilinara.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 批次L38：UP主代表作 /x/space/masterpiece（匿名可用，3 条精选置顶视频）
 * 复用 SpaceVideoItem 的展示，字段对齐 archive 卡片
 */
@Serializable
data class MasterpieceResponse(
    val code: Int = 0,
    val message: String = "",
    val data: List<MasterpieceArc> = emptyList()
)

@Serializable
data class MasterpieceArc(
    val aid: Long = 0L,
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val duration: Long = 0L,
    @SerialName("play") val playCount: Long = 0L,
    @SerialName("video_review") val danmakuCount: Long = 0L,
    @SerialName("favorites") val favoriteCount: Long = 0L
)
