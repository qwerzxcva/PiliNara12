package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** 批次L29：首页顶部推荐（/x/web-interface/index/top/rcmd，匿名可用） */
@Serializable
data class TopRcmdResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: TopRcmdData? = null
)

@Serializable
data class TopRcmdData(
    val item: List<TopRcmdItem> = emptyList()
)

@Serializable
data class TopRcmdItem(
    val id: Long = 0L,
    val bvid: String = "",
    val cid: Long = 0L,
    val goto: String = "",
    val uri: String = "",
    val title: String = "",
    val pic: String = "",
    val cover: String = "",
    val duration: Long = 0L,
    val name: String = "",
    @SerialName("owner") val owner: TopRcmdOwner? = null,
    val stat: TopRcmdStat? = null
)

@Serializable
data class TopRcmdOwner(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class TopRcmdStat(
    val view: Long = 0L,
    val like: Long = 0L,
    val danmaku: Long = 0L
)
