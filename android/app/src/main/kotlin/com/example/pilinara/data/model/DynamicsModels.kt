package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * Dynamics models for Bilibili API
 */

@Serializable
data class DynamicsResponse(
    val code: Int = 0,
    val data: DynamicsData? = null
)

@Serializable
data class DynamicsData(
    val items: List<DynamicItem> = emptyList(),
    val cursor: String = "",
    val updateCursor: String = ""
)

@Serializable
data class DynamicItem(
    val dynamic_id: Long = 0L,
    val type: String = "",
    val uid: Long = 0L,
    val user_info: DynamicUser? = null,
    val card: String = "",  // JSON string of item-specific data
    val like_count: Int = 0,
    val reply_count: Int = 0,
    val forward_count: Int = 0,
    val timestamp: Long = 0L
)

@Serializable
data class DynamicUser(
    val mid: Long = 0L,
    val uname: String = "",
    val face: String = "",
    val official_verify: OfficialVerify? = null
)

// Type-specific data classes
@Serializable
data class DynamicVideo(
    val items: List<DynamicVideoItem>? = null,
    val dynamic: String? = null
)

@Serializable
data class DynamicVideoItem(
    val aux_str: String = "",
    val type: String = "",
    val id: String = "",
    val uri: String = "",
    val cover: String = "",
    val title: String = "",
    val desc: String = "",
    val duration_text: String = "",
    val stat: VideoStat? = null,
    val owner: OwnerInfo? = null
)
