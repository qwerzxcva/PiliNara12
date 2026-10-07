package com.example.piliai.data.remote

import com.example.piliai.data.model.NavResponse
import com.example.piliai.data.model.QrGenerateResponse
import com.example.piliai.data.model.QrPollResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * 登录专用 API：二维码生成/轮询 + 自身资料（带 Cookie）
 * Cookie 由 BiliHttpClient 的 OkHttp CookieJar 自动管理（AccountSession 安装）
 */
class LoginApiClient(private val client: HttpClient = BiliHttpClient.client) {


    companion object {
        private const val PASSPORT_BASE = "https://passport.bilibili.com"
        private const val API_BASE = "https://api.bilibili.com"
    }

    /** 生成二维码登录 key */
    suspend fun generateQr(): Result<QrGenerateResponse> = runCatching {
        client.get("$PASSPORT_BASE/x/passport-login/web/qrcode/generate") {
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 轮询扫码状态；成功时响应 Set-Cookie 由 CookieJar 捕获 */
    suspend fun pollQr(qrcodeKey: String): Result<QrPollResponse> = runCatching {
        client.get("$PASSPORT_BASE/x/passport-login/web/qrcode/poll") {
            url { parameters.append("qrcode_key", qrcodeKey) }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 拿轮询响应的原始 Set-Cookie 头（SESSDATA/bili_jct/DedeUserID 等） */
    suspend fun pollQrWithCookies(qrcodeKey: String): Pair<QrPollResponse, List<String>> {
        val resp = client.get("$PASSPORT_BASE/x/passport-login/web/qrcode/poll") {
            url { parameters.append("qrcode_key", qrcodeKey) }
            header("Referer", "https://www.bilibili.com")
        }
        val setCookies = resp.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        val body: QrPollResponse = resp.body()
        return body to setCookies
    }

    /** 登录后获取自身资料（isLogin/mid/uname/face 等），需带 Cookie */
    suspend fun nav(): Result<NavResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/nav") {
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 历史记录（需登录） */
    suspend fun history(ps: Int = 20): Result<HttpResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/history/cursor") {
            url { parameters.append("ps", ps.toString()) }
            header("Referer", "https://www.bilibili.com")
        }
    }
}
