package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * 热门视频列表响应 — 对应 Bilibili /x/web-interface/popular
 * 字段与 Dart 侧 HotVideoItemModel 对齐（仅取 UI 需要的子集）
 */
@Serializable
data class PopularResponse(
    val code: Int,
    val message: String? = null,
    val data: PopularData? = null,
)

@Serializable
data class PopularData(
    val list: List<VideoItem> = emptyList(),
)

@Serializable
data class VideoItem(
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val title: String = "",
    val pic: String = "",
    val desc: String = "",
    val duration: Int = 0,
    val pubdate: Long = 0,
    val tname: String? = null,
    val author: String = "",
    val authorMid: Long = 0,
    val authorFace: String = "",
    val owner: Owner = Owner(),
    val stat: Stat = Stat(),
) {
    val durationText: String
        get() {
            val m = duration / 60
            val s = duration % 60
            return "%d:%02d".format(m, s)
        }

    val viewCountText: String
        get() = formatCount(stat.view)

    val danmakuCountText: String
        get() = formatCount(stat.danmaku.toLong())
}

@Serializable
data class Owner(
    val mid: Long = 0,
    val name: String = "",
    val face: String = "",
)

@Serializable
data class Stat(
    val view: Long = 0,
    val danmaku: Int = 0,
    val reply: Int = 0,
    val favorite: Int = 0,
    val coin: Int = 0,
    val share: Int = 0,
    val like: Int = 0,
)

fun formatCount(n: Long): String = when {
    n >= 100_000_000 -> "%.1f亿".format(n / 100_000_000.0)
    n >= 10_000 -> "%.1f万".format(n / 10_000.0)
    else -> n.toString()
}
