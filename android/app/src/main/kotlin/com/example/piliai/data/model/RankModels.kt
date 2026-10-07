package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 排行榜（批次L17）：/x/web-interface/ranking/v2?rid=0&type=all
 * rid: 0=全站 1=动画 4=游戏 3=音乐 36=科技 160=生活 5=娱乐 等
 * 注意：该接口需桌面 UA + Referer=https://www.bilibili.com/v/popular/rank/all，否则 -352 风控。
 */
@Serializable
data class RankResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: RankData? = null
)

@Serializable
data class RankData(
    val list: List<RankItem> = emptyList()
)

@Serializable
data class RankItem(
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val title: String = "",
    val pic: String = "",
    val score: Long = 0,
    @SerialName("owner") val owner: RankOwner? = null,
    @SerialName("stat") val stat: RankStat? = null,
    val duration: Int = 0
)

@Serializable
data class RankOwner(
    val mid: Long = 0,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class RankStat(
    val view: Long = 0,
    val danmaku: Long = 0,
    val like: Long = 0,
    val favorite: Long = 0
)
