package com.example.piliai.data.model

import kotlinx.serialization.Serializable

/**
 * 历史记录 / 收藏夹模型
 * 历史: GET /x/web-interface/history/cursor?ps=20&max=&view_at=&business=  (需登录)
 * 收藏: GET /x/v3/fav/folder/created/list-all?up_mid=   (公开)
 *       GET /x/v3/fav/resource/list?media_id=&pn=&ps=   (公开)
 */

@Serializable
data class HistoryResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: HistoryData? = null
)

@Serializable
data class HistoryData(
    val cursor: HistoryCursor? = null,
    val list: List<HistoryItem> = emptyList()
)

@Serializable
data class HistoryCursor(
    val max: Long = 0L,
    val view_at: Long = 0L,
    val business: String = "",
    val ps: Int = 20
)

@Serializable
data class HistoryItem(
    val title: String = "",
    val cover: String = "",
    val history: HistoryMeta? = null,
    val videos: Int = 0,
    val author_name: String = "",
    val author_mid: Long = 0L,
    val view_at: Long = 0L,
    val progress: Int = 0,
    val duration: Int = 0,
    val bvid: String = "",
    val aid: Long = 0L,
    val business: String = ""
)

@Serializable
data class HistoryMeta(
    val oid: Long = 0L,
    val bvid: String = "",
    val page: Int = 0,
    val cid: Long = 0L,
    val part: String = "",
    val business: String = ""
)

@Serializable
data class FavFolderListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: List<FavFolder>? = null
)

@Serializable
data class FavFolder(
    val id: Long = 0L,
    val fid: Long = 0L,
    val mid: Long = 0L,
    val title: String = "",
    val media_count: Int = 0
)

@Serializable
data class FavResourceListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: FavResourceData? = null
)

@Serializable
data class FavResourceData(
    val info: FavFolder? = null,
    val medias: List<FavMedia>? = null,
    val has_more: Boolean = false
)

@Serializable
data class FavMedia(
    val id: Long = 0L,
    val type: Int = 0,
    val title: String = "",
    val cover: String = "",
    val intro: String = "",
    val page: Int = 0,
    val duration: Int = 0,
    val upper: FavUpper? = null,
    val bvid: String = "",
    val aid: Long = 0L
)

@Serializable
data class FavUpper(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = ""
)

/** 稍后再看 /x/v2/history/toview */
@kotlinx.serialization.Serializable
data class ToViewResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: List<ToViewItem> = emptyList()
)

@kotlinx.serialization.Serializable
data class ToViewItem(
    val aid: Long = 0L,
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val cid: Long = 0L,
    val duration: Int = 0,
    val add_at: Long = 0L,
    val upper: ToViewUpper? = null
) {
    fun toHistoryItem() = HistoryItem(
        title = title, cover = pic, bvid = bvid, aid = aid,
        duration = duration, progress = 0,
        author_name = upper?.name.orEmpty(), author_mid = upper?.mid ?: 0L,
        business = "archive",
        history = HistoryMeta(oid = aid, bvid = bvid, cid = cid, business = "archive")
    )
}

@kotlinx.serialization.Serializable
data class ToViewUpper(
    val mid: Long = 0L,
    val name: String = "",
    val face: String = ""
)
