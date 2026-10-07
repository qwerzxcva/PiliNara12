package com.example.piliai.danmaku

import kotlinx.serialization.Serializable

/**
 * 弹弹play (DanDan) 弹幕源数据模型。
 *
 * 来源：Kazumi/Flutter 版的 `services/dandan/api.dart`。
 * DanDan 提供 B 站番剧的**第二弹幕源**，与 B 站原生弹幕互补（尤其老番/冷门番）。
 *
 * 调用前需在 BuildConfig 里配置 `DANDANAPI_APPID` 和 `DANDANAPI_KEY`
 * （从 https://www.dandanplay.com/api-docs/ 申请，免费）。
 */

/** 番剧搜索结果 */
@Serializable
data class DandanAnime(
    val animeId: Int = 0,
    val animeTitle: String = "",
    val typeDescription: String = "",
)

/** 番剧搜索响应 */
@Serializable
data class DandanSearchResponse(
    val animes: List<DandanAnime> = emptyList(),
    val hasMore: Boolean = false,
)

/** 单集信息 */
@Serializable
data class DandanEpisode(
    val episodeId: Int = 0,
    val episodeTitle: String = "",
)

/** 番剧元数据响应 */
@Serializable
data class DandanBangumiResponse(
    val bangumiId: Int = 0,
    val episodes: List<DandanEpisode> = emptyList(),
    val success: Boolean = false,
)

/** 单条弹幕 */
@Serializable
data class DandanComment(
    val time: Double = 0.0,        // 秒
    val type: Int = 1,             // 1=滚动, 2=顶, 3=底, 4=字幕
    val color: Int = 0x756ABE,     // 十进制颜色（7706950 = #756ABE）
    val source: String = "DanDan",
    val message: String = "",
)

/** DanDan API 响应：comments 字段 */
@Serializable
data class DandanCommentsResponse(
    val comments: List<DandanComment> = emptyList(),
)
