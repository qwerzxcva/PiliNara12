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
    val rpid: Long = 0L,
    val root: Long = 0L,
    val parent: Long = 0L,
    val seq: Long = 0L,
    val member: MemberInfo? = null,
    val content: ContentInfo? = null,
    val replies: List<CommentNode> = emptyList(),
    val like: Long = 0L,
    val reply: Long = 0L,
    val ctime: Long = 0L,
    @kotlinx.serialization.SerialName("rcount") val replyCount: Int = 0,
    val action: Int = 0   // 1=已点赞
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

// ===== 批次E：评论区增强 =====

@Serializable
data class CommentOperationResponse(
    val code: Int = 0,
    val message: String = ""
)

/** 楼中楼回复 /x/v2/reply/reply */
@Serializable
data class ReplyListResponse(
    val code: Int = 0,
    val message: String = "",
    val data: ReplyListData? = null
)

@Serializable
data class ReplyListData(
    val root: CommentNode? = null,
    val replies: List<CommentNode> = emptyList(),
    val page: ReplyPage? = null
)

@Serializable
data class ReplyPage(
    val num: Int = 1,
    val size: Int = 10,
    val count: Int = 0
)
