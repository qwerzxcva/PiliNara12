package com.example.pilinara.model

import kotlinx.serialization.Serializable

@Serializable
data class Video(
    val bvid: String = "",
    val aid: Long = 0,
    val title: String = "",
    val pic: String = "",
    val desc: String = "",
    val pubdate: Long = 0,
    val ctime: Long = 0,
    val videoDuration: Double = 0.0,
    val owner: Owner = Owner(),
    val stat: Stat = Stat(),
    val dynamic: String = "",
    val cid: Long = 0,
    val dimension: Dimension = Dimension(),
    val pages: List<Page> = emptyList(),
    val tag: String = "",
    val state: Int = 0,
    val attribute: Int = 0
) {
    @Serializable
    data class Owner(
        val mid: Long = 0,
        val name: String = "",
        val face: String = ""
    )
    
    @Serializable
    data class Stat(
        val view: Int = 0,
        val danmaku: Int = 0,
        val reply: Int = 0,
        val favorite: Int = 0,
        val coin: Int = 0,
        val share: Int = 0,
        val like: Int = 0,
        val nowRank: Int = 0,
        val hisRank: Int = 0,
        val dislike: Int = 0
    )
    
    @Serializable
    data class Dimension(
        val width: Int = 0,
        val height: Int = 0,
        val rotate: Int = 0
    )
    
    @Serializable
    data class Page(
        val cid: Long = 0,
        val page: Int = 0,
        val part: String = "",
        val duration: Long = 0,
        val vid: String = "",
        val weblink: String = "",
        val dimension: Dimension = Dimension()
    )
}
