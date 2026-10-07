package com.example.piliai.data.model

import kotlinx.serialization.Serializable

/**
 * B站二维码登录模型
 * generate: GET https://passport.bilibili.com/x/passport-login/web/qrcode/generate
 * poll:     GET https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=...
 * poll data.code: 86101=未扫码 86090=已扫码未确认 86038=二维码失效 0=登录成功(写 Set-Cookie)
 */
@Serializable
data class QrGenerateResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: QrGenerateData? = null
)

@Serializable
data class QrGenerateData(
    val url: String = "",
    val qrcode_key: String = ""
)

@Serializable
data class QrPollResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: QrPollData? = null
)

@Serializable
data class QrPollData(
    val url: String = "",
    val refresh_token: String = "",
    val timestamp: Long = 0L,
    /** 86101=未扫码 86090=已扫码未确认 86038=过期 0=成功 */
    val code: Int = 86101,
    val message: String = ""
)

/** 登录成功后从 /x/web-interface/nav 拿到的自身资料 */
@Serializable
data class NavResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: NavData? = null
)

@Serializable
data class NavData(
    val isLogin: Boolean = false,
    val mid: Long = 0L,
    val uname: String = "",
    val face: String = "",
    val levelInfo: LevelInfo? = null,
    val money: Double = 0.0,
    val coin: Double = 0.0,
    val vipStatus: Int = 0,
    val vipType: Int = 0
)
