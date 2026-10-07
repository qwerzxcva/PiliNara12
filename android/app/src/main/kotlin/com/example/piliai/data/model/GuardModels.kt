package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GuardAnchorResponse(
    val code: Int = 0,
    val data: GuardAnchorData? = null
)

@Serializable
data class GuardAnchorData(val uid: Long = 0L)

/** 批次L31：直播间大航海列表 /xlive/app-room/v2/guardTab/topList（匿名可用） */
@Serializable
data class GuardTopListResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: GuardTopListData? = null
)

@Serializable
data class GuardTopListData(
    val info: GuardInfo? = null,
    val topList: List<GuardUser> = emptyList()
)

@Serializable
data class GuardInfo(
    val num: Int = 0,
    @SerialName("achievement_level") val achievementLevel: Int = 0
)

@Serializable
data class GuardUser(
    val uid: Long = 0L,
    val username: String = "",
    val face: String = "",
    @SerialName("guard_level") val guardLevel: Int = 0,
    @SerialName("guard_sub_level") val guardSubLevel: Int = 0
)
