package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 批次L23：专栏文章（/x/article/view，匿名可用）
 */
@Serializable
data class ArticleViewResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: ArticleData? = null
)

@Serializable
data class ArticleData(
    val id: Long = 0L,
    val title: String = "",
    val summary: String = "",
    @SerialName("banner_url") val bannerUrl: String = "",
    val author: ArticleAuthor? = null,
    @SerialName("publish_time") val publishTime: Long = 0L,
    val content: String = "",
    val words: Long = 0L,
    val tags: String = "",
    val stats: ArticleStats? = null
)

@Serializable
data class ArticleAuthor(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = ""
)

@Serializable
data class ArticleStats(
    val view: Long = 0L,
    val favorite: Long = 0L,
    val like: Long = 0L,
    val reply: Long = 0L,
    val share: Long = 0L,
    val coin: Long = 0L
)

/** 批次L24：UP 主专栏列表（/x/space/article） */
@Serializable
data class SpaceArticleResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: SpaceArticleData? = null
)

@Serializable
data class SpaceArticleData(
    val count: Int = 0,
    val articles: List<SpaceArticleItem> = emptyList()
)

@Serializable
data class SpaceArticleItem(
    val id: Long = 0L,
    val title: String = "",
    val summary: String = "",
    @SerialName("banner_url") val bannerUrl: String = "",
    @SerialName("publish_time") val publishTime: Long = 0L,
    val stats: ArticleStats? = null
)
