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

// ===== 批次F：直播 =====

/** /xlive/web-room/v2/index/getRoomPlayInfo —— 直播间播放信息 */
@kotlinx.serialization.Serializable
data class LivePlayInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LivePlayInfoData? = null
)

@kotlinx.serialization.Serializable
data class LivePlayInfoData(
    @kotlinx.serialization.SerialName("room_id") val roomId: Long = 0L,
    @kotlinx.serialization.SerialName("short_id") val shortId: Long = 0L,
    val uid: Long = 0L,
    @kotlinx.serialization.SerialName("live_status") val liveStatus: Int = 0,  // 0未开播 1直播中 2轮播
    @kotlinx.serialization.SerialName("live_time") val liveTime: Long = 0L,
    @kotlinx.serialization.SerialName("playurl_info") val playurlInfo: LivePlayurlInfo? = null
)

@kotlinx.serialization.Serializable
data class LivePlayurlInfo(
    @kotlinx.serialization.SerialName("playurl") val playurl: LivePlayurl? = null
)

@kotlinx.serialization.Serializable
data class LivePlayurl(
    val host: String = "",
    val base_url: String = "",
    val extra: String = "",
    val streams: List<LiveStreamItem> = emptyList()
)

@kotlinx.serialization.Serializable
data class LiveStreamItem(
    @kotlinx.serialization.SerialName("protocol_name") val protocolName: String = "",  // flv / hls
    val format: List<LiveFormatItem> = emptyList()
)

@kotlinx.serialization.Serializable
data class LiveFormatItem(
    @kotlinx.serialization.SerialName("format_name") val formatName: String = "",  // flv / ts / fmp4
    val codec: List<LiveCodecItem> = emptyList()
)

@kotlinx.serialization.Serializable
data class LiveCodecItem(
    @kotlinx.serialization.SerialName("codec_name") val codecName: String = "",   // avc / hevc
    @kotlinx.serialization.SerialName("current_qn") val currentQn: Int = 0,
    @kotlinx.serialization.SerialName("accept_qn") val acceptQn: List<Int> = emptyList(),
    @kotlinx.serialization.SerialName("base_url") val baseUrl: String = "",
    val url_info: List<LiveUrlInfo> = emptyList()
)

@kotlinx.serialization.Serializable
data class LiveUrlInfo(
    val host: String = "",
    val extra: String = "",
    @kotlinx.serialization.SerialName("host_ttl") val hostTtl: Int = 0
) {
    fun fullUrl(baseUrl: String, extra: String) = host + baseUrl + extra
}

/** /xlive/web-interface/v1/index/getInfoByRoom —— 直播间标题/主播信息 */
@kotlinx.serialization.Serializable
data class LiveRoomInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LiveRoomInfoData? = null
)

@kotlinx.serialization.Serializable
data class LiveRoomInfoData(
    @kotlinx.serialization.SerialName("room_id") val roomId: Long = 0L,
    @kotlinx.serialization.SerialName("live_status") val liveStatus: Int = 0,
    @kotlinx.serialization.SerialName("live_time") val liveTime: Long = 0L,
    @kotlinx.serialization.SerialName("parent_area_name") val parentAreaName: String = "",
    @kotlinx.serialization.SerialName("area_name") val areaName: String = "",
    val title: String = "",
    @kotlinx.serialization.SerialName("user_count") val userCount: Long = 0L,
    @kotlinx.serialization.SerialName("keyframe_url") val keyframeUrl: String = ""
)
