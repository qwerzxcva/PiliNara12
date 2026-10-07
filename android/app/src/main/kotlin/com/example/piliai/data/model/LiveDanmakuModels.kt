package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ==================== 直播弹幕 WebSocket（批次J） ====================

@Serializable
data class DanmuInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: DanmuInfoData? = null
)

@Serializable
data class DanmuInfoData(
    val token: String = "",
    @SerialName("host_list") val hostList: List<DanmuHost> = emptyList()
)

@Serializable
data class DanmuHost(
    val host: String = "",
    @SerialName("wss_port") val wssPort: Int = 443,
    @SerialName("ws_port") val wsPort: Int = 2244
)

/** 一条直播弹幕（从 DANMU_MSG 解析） */
data class LiveDanmakuMsg(
    val uid: Long = 0L,
    val name: String = "",
    val text: String = "",
    val color: Int = 0xFFFFFF,
    val mode: Int = 1,          // 1=滚动 4=底部 5=顶部
    val isEmote: Boolean = false,
    val medalName: String? = null,
    val medalLevel: Int = 0,
    val ts: Long = System.currentTimeMillis() / 1000
)

/** 消息种类标记（聊天列表区分样式） */
enum class LiveMsgKind { CHAT, SUPER_CHAT, GIFT, WELCOME }

/** SC（醒目留言） */
data class LiveSuperChat(
    val uid: Long = 0L,
    val name: String = "",
    val face: String = "",
    val price: Int = 0,          // RMB
    val message: String = "",
    val duration: Long = 0L,     // 展示秒数
    val backgroundColor: String = "#C0000F",
    val ts: Long = System.currentTimeMillis() / 1000
)

/** 礼物消息（SEND_GIFT / COMBO_SEND） */
data class LiveGift(
    val uid: Long = 0L,
    val name: String = "",
    val giftName: String = "",
    val num: Int = 1,
    val coinType: String = "gold",   // gold=金瓜子 silver=银瓜子
    val price: Long = 0,
    val ts: Long = System.currentTimeMillis() / 1000
)
