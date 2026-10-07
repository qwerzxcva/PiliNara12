package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

/** 批次L26：入站必刷（/x/web-interface/popular/precious，匿名可用） */
@Serializable
data class PreciousResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: PreciousData? = null
)

@Serializable
data class PreciousData(
    val title: String = "",
    val explain: String = "",
    val list: List<VideoItem> = emptyList()
)

/** 批次L26：每周必看列表（/x/web-interface/popular/series/list，匿名可用） */
@Serializable
data class WeeklyListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: WeeklyListData? = null
)

@Serializable
data class WeeklyListData(
    val list: List<WeeklyItem> = emptyList()
)

@Serializable
data class WeeklyItem(
    val number: Int = 0,
    val subject: String = "",
    val name: String = "",
    val status: Int = 0
)

/** 批次L28：每周必看期数详情（/x/web-interface/popular/series/one） */
@Serializable
data class WeeklyDetailResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: WeeklyDetailData? = null
)

@Serializable
data class WeeklyDetailData(
    @SerialName("config") val config: JsonElement? = null,
    val list: List<VideoItem> = emptyList()
)
