package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * Comment models for Bilibili API
 */

@Serializable
data class CommentResponse(
    val code: Int = 0,
    val data: CommentData? = null
)

@Serializable
data class CommentData(
    val upper: UpperInfo? = null,
    val replay: List<CommentNode> = emptyList(),
    val cursor: CursorInfo? = null,
    val count: Int = 0,
    val num: Int = 0
)

@Serializable
data class UpperInfo(
    val mid: Long = 0L,
    val uname: String = "",
    val face: String = "",
    val official: OfficialInfo? = null,
    val vip: VipInfo? = null
)

@Serializable
data class VipInfo(
    val vipStatus: Int = 0,
    val vipType: Int = 0,
    val vipDueDate: Long = 0L
)

@Serializable
data class CommentNode(
    val root: Long = 0L,
    val seq: Long = 0L,
    val member: MemberInfo? = null,
    val content: ContentInfo? = null,
    val replies: List<CommentNode> = emptyList(),
    val like: Long = 0L,
    val reply: Long = 0L
)

@Serializable
data class MemberInfo(
    val mid: Long = 0L,
    val uname: String = "",
    val face: String = "",
    val official: OfficialInfo? = null,
    val vip: VipInfo? = null,
    val pendant: PendantInfo? = null
)

@Serializable
data class ContentInfo(
    val message: String = "",
    val date: Long = 0L,
    val round: Int = 0
)

@Serializable
data class CursorInfo(
    val cursor: Long = 0L,
    val upperCursor: Long = 0L,
    val bottomCursor: Long = 0L,
    val nextCursor: Long = 0L,
    val sortType: String = ""
)
