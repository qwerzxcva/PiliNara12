package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 批次L25：直播分区（room/v1/Area/getList 匿名可用 + webMain/getMoreRecList 推荐流）
 */
@Serializable
data class LiveAreaListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: List<LiveParentArea> = emptyList()
)

@Serializable
data class LiveParentArea(
    val id: Long = 0L,
    val name: String = "",
    val list: List<LiveSubArea> = emptyList()
)

@Serializable
data class LiveSubArea(
    val id: Long = 0L,
    @SerialName("parent_id") val parentId: Long = 0L,
    val name: String = "",
    val pic: String = ""
)

/** webMain/getMoreRecList 推荐房间（前端按分区过滤） */
@Serializable
data class LiveRecListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: LiveRecData? = null
)

@Serializable
data class LiveRecData(
    @SerialName("recommend_room_list") val recommendRoomList: List<LiveRecRoom> = emptyList()
)

@Serializable
data class LiveRecRoom(
    val roomid: Long = 0L,
    val title: String = "",
    val uname: String = "",
    val cover: String = "",
    val online: Long = 0L,
    val uid: Long = 0L,
    @SerialName("area_v2_name") val areaName: String = "",
    @SerialName("area_v2_parent_id") val parentAreaId: Long = 0L,
    @SerialName("area_v2_id") val areaId: Long = 0L,
    @SerialName("area_v2_parent_name") val parentAreaName: String = ""
)
