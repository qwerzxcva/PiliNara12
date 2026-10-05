package com.example.pilinara.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** 批次L32：视频分区最新 /x/web-interface/newlist（匿名可用） */
@Serializable
data class NewListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: NewListData? = null
)

@Serializable
data class NewListData(
    val page: NewListPage? = null,
    val archives: List<NewListArchive> = emptyList()
)

@Serializable
data class NewListPage(
    val page: Int = 1,
    val size: Int = 20,
    val count: Long = 0L
)

@Serializable
data class NewListArchive(
    val aid: Long = 0L,
    val bvid: String = "",
    val cid: Long = 0L,
    val title: String = "",
    val pic: String = "",
    val desc: String = "",
    val duration: Long = 0L,
    @SerialName("pubdate") val pubdate: Long = 0L,
    val owner: NewListOwner? = null,
    val stat: NewListStat? = null
)

@Serializable
data class NewListOwner(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class NewListStat(
    val view: Long = 0L,
    val danmaku: Long = 0L,
    val favorite: Long = 0L,
    val like: Long = 0L
)
