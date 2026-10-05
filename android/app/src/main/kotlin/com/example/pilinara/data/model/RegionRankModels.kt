package com.example.pilinara.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** 批次L34：分区排行榜 /x/web-interface/ranking/region（匿名可用，11条/分区） */
@Serializable
data class RegionRankResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: List<RegionRankItem> = emptyList()
)

@Serializable
data class RegionRankItem(
    val aid: Long = 0L,
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val author: String = "",
    val mid: Long = 0L,
    @SerialName("duration") val duration: Long = 0L,
    val score: Long = 0L,
    val create: String = "",
    @SerialName("pts") val pts: Long = 0L,
    val stats: RegionRankStats? = null
)

@Serializable
data class RegionRankStats(
    val view: Long = 0L,
    val danmaku: Long = 0L,
    val favorite: Long = 0L,
    val like: Long = 0L
)
