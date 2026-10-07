package com.example.piliai.data.model

import kotlinx.serialization.Serializable

/**
 * User models for Bilibili API
 */

@Serializable
data class UserInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: UserInfoData? = null
)

@Serializable
data class UserInfoData(
    val mid: Long = 0L,
    val uname: String = "",
    val face: String = "",
    val vipStatus: Int = 0,
    val vipType: Int = 0,
    val levelInfo: LevelInfo? = null,
    val coins: Double = 0.0,
    val fans: Long = 0L,
    val friend: Long = 0L,
    val attention: Long = 0L,
    val sign: String = "",
    val official: OfficialInfo? = null,
    val pendant: PendantInfo? = null,
    val badge: BadgeInfo? = null
)

@Serializable
data class LevelInfo(
    val currentLevel: Int = 0,
    val currentMin: Long = 0L,
    val currentExp: Long = 0L,
    val nextExp: Long = 0L
)

@Serializable
data class OfficialInfo(
    val role: Int = 0,
    val title: String = "",
    val desc: String = "",
    val type: Int = 0
)

@Serializable
data class PendantInfo(
    val pid: Long = 0L,
    val name: String = "",
    val image: String = "",
    val expire: Int = 0
)

@Serializable
data class BadgeInfo(
    val name: String = "",
    val image: String = ""
)
