package com.example.pilinara.data.repository

import com.example.pilinara.database.LoginAccountEntity
import com.example.pilinara.database.PiliNaraRepository
import com.example.pilinara.data.model.NavResponse
import com.example.pilinara.data.model.QrGenerateResponse
import com.example.pilinara.data.model.QrPollData
import com.example.pilinara.data.model.QrPollResponse
import com.example.pilinara.data.remote.AccountSession
import com.example.pilinara.data.remote.BiliHttpClient
import com.example.pilinara.data.remote.LoginApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 登录仓储：二维码登录流程 + session 持久化（Room）
 */
class LoginRepository(
    private val api: LoginApiClient = LoginApiClient(),
    private val db: PiliNaraRepository = PiliNaraRepository(com.example.pilinara.AppContext.get())
) {

    /** 生成登录二维码；返回 (二维码内容 url, qrcode_key) */
    suspend fun createQr(): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        api.generateQr().map { resp: QrGenerateResponse ->
            val d = resp.data ?: error("生成二维码失败: ${resp.message}")
            d.url to d.qrcode_key
        }
    }

    /** 轮询一次扫码状态；若登录成功会把 cookie 存入内存与 Room */
    suspend fun pollOnce(qrcodeKey: String): Result<QrPollData> = withContext(Dispatchers.IO) {
        runCatching {
            val (body, setCookies) = api.pollQrWithCookies(qrcodeKey)
            val d = body.data ?: error("轮询失败: ${body.message}")
            if (d.code == 0) {
                // 登录成功：合并响应里的 url 参数 cookie + Set-Cookie 头
                AccountSession.applySetCookies(setCookies)
                parseUrlCookies(d.url)?.let { AccountSession.applySetCookies(it) }
                val nav = api.nav().getOrNull()
                persistAccount(nav)
            }
            d
        }
    }

    /** 从轮询返回的 url（含 crossDomain cookie 参数）里提取 cookie */
    private fun parseUrlCookies(url: String): List<String>? {
        if (!url.contains("?")) return null
        return url.substringAfter('?').split('&')
            .filter { it.substringBefore('=') in setOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5", "sid") }
            .map { it.replace('=', ':') }  // 不直接用；仅占位——Set-Cookie 头是主要来源
    }

    /** 把当前内存 session 写入 Room */
    private suspend fun persistAccount(nav: NavResponse?) {
        val data = nav?.data
        val cookieJson = JSONObject(AccountSession.snapshot()).toString()
        val mid = data?.mid?.toString() ?: AccountSession.mid.toString()
        db.saveLoginAccount(
            LoginAccountEntity(
                mid = mid,
                cookiesJson = cookieJson,
                isLogin = AccountSession.isLogin,
                activated = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** 启动时恢复 session（读 Room 最近账号 → 内存） */
    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val account = db.getAllLoginAccounts().maxByOrNull { it.updatedAt }
        if (account != null && account.isLogin) {
            val map = org.json.JSONObject(account.cookiesJson).let { obj ->
                obj.keys().asSequence().associateWith { obj.optString(it) }
            }
            AccountSession.restore(map.entries.joinToString("; ") { "${it.key}=${it.value}" })
        }
        AccountSession.isLogin
    }

    /** 当前登录用户资料（未登录时 nav.isLogin=false） */
    suspend fun fetchSelfInfo(): Result<NavResponse> = withContext(Dispatchers.IO) {
        api.nav()
    }

    /** 退出登录 */
    suspend fun logout() = withContext(Dispatchers.IO) {
        AccountSession.clear()
        db.deleteAllLoginAccounts()
    }
}
