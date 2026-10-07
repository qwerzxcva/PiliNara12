package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ==================== 私聊会话（api.vc.bilibili.com） ====================

@Serializable
data class SessionListResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SessionListData? = null
)

@Serializable
data class SessionListData(
    @SerialName("session_list") val sessionList: List<SessionItem> = emptyList(),
    @SerialName("has_more") val hasMore: Int = 0
)

@Serializable
data class SessionItem(
    @SerialName("talker_id") val talkerId: Long = 0,
    @SerialName("session_type") val sessionType: Int = 1,
    @SerialName("last_msg") val lastMsg: SessionLastMsg? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("pin_talker") val pinTalker: Int = 0,
    @SerialName("max_seq") val maxSeq: Long = 0,
    @SerialName("session_ts") val sessionTs: Long = 0,
    @SerialName("account_info") val accountInfo: SessionAccount? = null
)

@Serializable
data class SessionLastMsg(
    @SerialName("sender_uid") val senderUid: Long = 0,
    @SerialName("receiver_id") val receiverId: Long = 0,
    @SerialName("content") val content: String = "",
    @SerialName("msg_type") val msgType: Int = 1,
    @SerialName("timestamp") val timestamp: Long = 0,
    @SerialName("seq") val seq: Long = 0
)

@Serializable
data class SessionAccount(
    @SerialName("name") val name: String = "",
    @SerialName("face") val face: String = "",
    @SerialName("mid") val mid: Long = 0
)

// 解析 last_msg.content JSON 里的文本
fun SessionItem.lastText(): String {
    return try {
        val o = org.json.JSONObject(lastMsg?.content ?: "{}")
        o.optString("content").ifBlank { "[消息]" }
    } catch (_: Exception) { "[消息]" }
}

@Serializable
data class SessionMsgsResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SessionMsgsData? = null
)

@Serializable
data class SessionMsgsData(
    @SerialName("messages") val messages: List<SessionMsg> = emptyList()
)

@Serializable
data class SessionMsg(
    @SerialName("sender_uid") val senderUid: Long = 0,
    @SerialName("receiver_id") val receiverId: Long = 0,
    @SerialName("content") val content: String = "",
    @SerialName("msg_type") val msgType: Int = 1,
    @SerialName("timestamp") val timestamp: Long = 0,
    @SerialName("msg_seq") val msgSeq: Long = 0
)

fun SessionMsg.msgText(): String = try {
    org.json.JSONObject(content).optString("content").ifBlank { "[消息]" }
} catch (_: Exception) { "[消息]" }

/** 图片消息（msg_type=2）解析：url/width/height；非图片返回 null */
data class SessionImage(val url: String, val width: Int, val height: Int)

fun SessionMsg.msgImage(): SessionImage? {
    if (msgType != 2) return null
    return try {
        val o = org.json.JSONObject(content)
        val url = o.optString("url")
        if (url.isBlank()) null else SessionImage(
            url, o.optInt("width", 300), o.optInt("height", 300)
        )
    } catch (_: Exception) { null }
}
