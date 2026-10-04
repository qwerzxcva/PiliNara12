package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * Search result models for Bilibili API
 */

@Serializable
data class SearchResponse(
    val numResults: Int = 0,
    val pages: Int = 0,
    val result: List<SearchResultItem> = emptyList()
)

@Serializable
data class SearchResultItem(
    @kotlinx.serialization.SerialName("result_type") val resultType: String = "",
    val mid: Long = 0L,
    val name: String = "",
    val head: String = "",
    val signature: String = "",
    val fans: Long = 0L,
    val articleCount: Int = 0,
    // Video results
    val aid: Long = 0L,
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val author: String = "",
    val duration: String = "",
    val pubdate: Long = 0L,
    val play: Long = 0L,
    val view: Long = 0L,
    val danmaku: Long = 0L,
    val reply: Long = 0L,
    val favorite: Long = 0L,
    val coin: Long = 0L,
    val like: Long = 0L,
    val tag: String = "",
    val description: String = "",
    @kotlinx.serialization.SerialName("uimage") val uimage: String? = null,
    val roomid: Long = 0L,
    val online: Long = 0L
)

@Serializable
data class SearchSuggest(
    val query: String = "",
    val results: List<SuggestResult> = emptyList()
)

@Serializable
data class SuggestResult(
    val mid: Long = 0L,
    val uname: String = "",
    val face: String = "",
    val fname: String = ""
)

/** /x/v2/search/trending/ranking —— 热搜榜（无需登录） */
@Serializable
data class SearchTrendingResponse(
    val code: Int = 0,
    val message: String = "",
    val data: TrendingData? = null
)

@Serializable
data class TrendingData(
    val list: List<TrendingItem> = emptyList(),
    val top_list: List<TrendingItem> = emptyList()
)

@Serializable
data class TrendingItem(
    val keyword: String = "",
    val show_name: String = "",
    val icon: String? = null,
    val show_live_icon: Boolean = false,
    val recommend_reason: String? = null
)

/** 搜索结果（分类：video/bili_user/live） */
@Serializable
data class SearchResultResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SearchResultData? = null
)

@Serializable
data class SearchResultData(
    val numResults: Int = 0,
    val result: List<SearchResultItem> = emptyList()
)
