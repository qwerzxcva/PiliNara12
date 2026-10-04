package com.example.pilinara.data.model

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
