package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * UP 主空间模型（批次C）
 * - /x/space/wbi/acc/info        空间主页信息（wbi）
 * - /x/space/wbi/arc/search      投稿列表（wbi，分页）
 * - /x/relation/stat             粉丝/关注数
 */

/** 空间主页信息 */
@Serializable
data class SpaceInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SpaceInfo? = null
)

@Serializable
data class SpaceInfo(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = "",
    val sign: String = "",
    val level: Int = 0,
    @kotlinx.serialization.SerialName("jointime") val joinTime: Long = 0L,
    val coins: Long = 0L,
    val official: SpaceOfficial? = null,
    val followers: Long = 0L
)

@Serializable
data class SpaceOfficial(
    val role: Int = 0,
    val title: String = "",
    val desc: String = ""
)

/** /x/relation/stat —— 粉丝/关注/悄悄关注计数 */
@Serializable
data class RelationStatResponse(
    val code: Int = 0,
    val message: String = "",
    val data: RelationStat? = null
)

@Serializable
data class RelationStat(
    val mid: Long = 0L,
    val following: Long = 0L,
    val whisper: Long = 0L,
    val black: Long = 0L,
    val follower: Long = 0L
)

/** /x/space/wbi/arc/search —— 投稿列表 */
@Serializable
data class SpaceArchiveResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SpaceArchiveData? = null
)

@Serializable
data class SpaceArchiveData(
    val list: SpaceArchiveList? = null,
    val page: SpaceArchivePage? = null
)

@Serializable
data class SpaceArchiveList(
    val vlist: List<SpaceVideoItem> = emptyList()
)

@Serializable
data class SpaceArchivePage(
    val pn: Int = 1,
    val ps: Int = 30,
    val count: Long = 0L
)

@Serializable
data class SpaceVideoItem(
    val aid: Long = 0L,
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val created: Long = 0L,
    val length: String = "",        // mm:ss
    val play: Long = 0L,
    val review: Long = 0L,
    @kotlinx.serialization.SerialName("video_review") val videoReview: Long = 0L,
    val description: String = "",
    val comment: Long = 0L
)
