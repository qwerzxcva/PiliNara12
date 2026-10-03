package com.example.pilinara.data.remote

import java.util.concurrent.ConcurrentHashMap

/**
 * 进程内 Cookie 存储（用于 B站登录 session：SESSDATA / bili_jct / DedeUserID ...）
 *
 * BiliHttpClient 用 OkHttp 引擎，但为了让登录态能在「无 Context 的仓储层」直接注入请求头，
 * 这里维护一个内存 cookie 表，并由 AuthInterceptor 统一挂到每次 B站请求上。
 * 持久化由 LoginRepository 落到 Room 的 LoginAccountEntity。
 */
object AccountSession {

    private val cookies = ConcurrentHashMap<String, String>()

    /** Cookie 白名单：只存登录相关字段，避免把无关 cookie 带进请求 */
    private val ALLOWED = setOf(
        "SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5",
        "sid", "buvid3", "buvid4"
    )

    @Volatile
    var isLogin: Boolean = false
        private set

    @Volatile
    var mid: Long = 0L
        private set

    /** 解析 Set-Cookie 头（形如 "SESSDATA=xxx; Path=/; ..."）并存入内存 */
    fun applySetCookies(headerValues: List<String>) {
        headerValues.forEach { raw ->
            val first = raw.substringBefore(';')
            val name = first.substringBefore('=', "").trim()
            val value = first.substringAfter('=', "").trim()
            if (name.isNotEmpty() && value.isNotEmpty() && name in ALLOWED) {
                cookies[name] = value
            }
        }
        if (cookies["SESSDATA"]?.isNotEmpty() == true) {
            isLogin = true
        }
        cookies["DedeUserID"]?.toLongOrNull()?.let { mid = it }
    }

    /** 直接从 cookie 串恢复（从数据库读回时用），格式 "a=1; b=2" */
    fun restore(cookieString: String) {
        cookies.clear()
        cookieString.split(';').forEach { pair ->
            val k = pair.substringBefore('=', "").trim()
            val v = pair.substringAfter('=', "").trim()
            if (k.isNotEmpty() && v.isNotEmpty()) cookies[k] = v
        }
        isLogin = cookies["SESSDATA"]?.isNotEmpty() == true
        cookies["DedeUserID"]?.toLongOrNull()?.let { mid = it }
    }

    /** 导出为 cookie 头值 */
    fun cookieHeader(): String = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

    fun snapshot(): Map<String, String> = cookies.toMap()

    fun clear() {
        cookies.clear()
        isLogin = false
        mid = 0L
    }
}
