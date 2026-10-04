package com.example.pilinara.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 批次L2：pgc 首页分类（番剧/电影/纪录片/国创/综艺索引）
 * API: /pgc/season/index/result?season_type=N&type=1&page=1&pagesize=20
 */
@Serializable
data class PgcIndexResponse(
    val code: Int = 0,
    val message: String = "",
    val data: PgcIndexData? = null
)

@Serializable
data class PgcIndexData(
    @SerialName("has_next") val hasNext: Int = 0,
    val list: List<PgcIndexItem> = emptyList()
)

@Serializable
data class PgcIndexItem(
    @SerialName("season_id") val seasonId: Long = 0L,
    val title: String = "",
    @SerialName("subTitle") val subTitle: String = "",
    val cover: String = "",
    val badge: String = "",
    val score: String = "",
    @SerialName("index_show") val indexShow: String = "",
    @SerialName("media_id") val mediaId: Long = 0L
) {
    /** 详情路由参数 */
    fun routeSeasonId(): Long = seasonId
}
