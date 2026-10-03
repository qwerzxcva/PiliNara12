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
    val resultType: String = "",
    val mid: Long = 0L,
    val name: String = "",
    val head: String = "",
    val signature: String = "",
    val fans: Long = 0L,
    val articleCount: Int = 0,
    // Video results
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val duration: String = "",
    val pubdate: Long = 0L,
    val view: Long = 0L,
    val danmaku: Long = 0L,
    val reply: Long = 0L,
    val favorite: Long = 0L,
    val coin: Long = 0L,
    val like: Long = 0L,
    val tag: String = "",
    val description: String = ""
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
