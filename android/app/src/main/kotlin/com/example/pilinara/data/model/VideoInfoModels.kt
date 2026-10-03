package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * Video information models for Bilibili API
 */

@Serializable
data class VideoInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: VideoInfoData? = null
)

@Serializable
data class VideoInfoData(
    val bvid: String = "",
    val aid: Long = 0L,
    val cid: Long = 0L,
    val title: String = "",
    val desc: String = "",
    val pic: String = "",
    val pubdate: Long = 0L,
    val duration: Long = 0L,
    val stat: VideoStat? = null,
    val owner: OwnerInfo? = null,
    val pages: List<PageInfo>? = null,
    val dynamic: String = "",
    val tag: String = "",
    val copyright: Int = 0
)

@Serializable
data class VideoStat(
    val play: Long = 0L,
    val danmaku: Long = 0L,
    val reply: Long = 0L,
    val favorite: Long = 0L,
    val coin: Long = 0L,
    val like: Long = 0L,
    val share: Long = 0L,
    val nowRank: Long = 0L,
    val hisRank: Long = 0L,
    val view: Long = 0L
)

@Serializable
data class OwnerInfo(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = "",
    val official_verify: OfficialVerify? = null
)

@Serializable
data class OfficialVerify(
    val type: Int = 0,
    val desc: String = ""
)

@Serializable
data class PageInfo(
    val cid: Long = 0L,
    val page: Int = 0,
    val from: String = "",
    val part: String = "",
    val duration: Long = 0L,
    val vid: String? = null,
    val desc: String = "",
    val weblink: String = ""
)

fun VideoInfoData.toVideoItem(): VideoItem {
    return VideoItem(
        bvid = bvid,
        aid = aid,
        cid = cid,
        title = title,
        pic = pic,
        desc = desc,
        duration = duration.toInt(),
        pubdate = pubdate,
        owner = Owner(
            mid = owner?.mid ?: 0L,
            name = owner?.name ?: "",
            face = owner?.face ?: ""
        ),
        stat = Stat(
            view = stat?.play ?: 0L,
            danmaku = (stat?.danmaku ?: 0L).toInt(),
            reply = (stat?.reply ?: 0L).toInt(),
            favorite = (stat?.favorite ?: 0L).toInt(),
            coin = (stat?.coin ?: 0L).toInt(),
            like = (stat?.like ?: 0L).toInt()
        )
    )
}
