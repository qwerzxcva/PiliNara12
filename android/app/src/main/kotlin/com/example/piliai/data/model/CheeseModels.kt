package com.example.piliai.data.model

import kotlinx.serialization.Serializable

/** 批次L45：UP主付费课程（cheese，pugv）模型 */
@Serializable
data class CheesePageResponse(
    val code: Int = 0,
    val message: String = "",
    val data: CheesePageData? = null
)

@Serializable
data class CheesePageData(
    val items: List<CheeseItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CheeseItem(
    val season_id: Long = 0,
    val title: String = "",
    val subtitle: String = "",
    val cover: String = "",
    val ep_count: Int = 0,
    val play: Long = 0,
    val status: Int = 0
)
