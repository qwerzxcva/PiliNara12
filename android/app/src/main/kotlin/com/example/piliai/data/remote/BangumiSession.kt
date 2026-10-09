package com.example.piliai.data.remote

import android.content.Context
import androidx.core.content.edit
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.request.HttpRequestBuilder

/**
 * Bangumi(bgm.tv) 登录态 —— Animeko「Bangumi 登录」移植
 *
 * 与 B站登录（AccountSession，Cookie 体系）**相互独立**：
 * Bangumi 走 OAuth 个人访问令牌（Access Token），请求时放在
 * `Authorization: Bearer <token>` 头里。
 *
 * 审核要点：
 * 1. Token 存 SharedPreferences（私有模式），不进 Log、不进 Room 明文表。
 * 2. 只在需要使用时读取；未登录时相关功能返回 false，不报错。
 * 3. 不做任何"假登录"：必须用户填入真实令牌并校验通过才置为已登录。
 */
object BangumiSession {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        followRedirects = false
        engine {
            config {
                followRedirects(false)
                followSslRedirects(false)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
    }

    private const val PREFS = "bangumi_auth"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR = "avatar"

    @Volatile
    var isLogin: Boolean = false
        private set

    @Volatile
    var token: String = ""
        private set

    @Volatile
    var userId: Long = 0L
        private set

    @Volatile
    var nickname: String = ""
        private set

    @Volatile
    var avatar: String = ""
        private set

    /** 从持久化恢复登录态（Application 启动时调用） */
    fun restore(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        token = sp.getString(KEY_TOKEN, "") ?: ""
        userId = sp.getLong(KEY_USER_ID, 0L)
        nickname = sp.getString(KEY_NICKNAME, "") ?: ""
        avatar = sp.getString(KEY_AVATAR, "") ?: ""
        isLogin = token.isNotBlank()
    }

    /**
     * 用个人访问令牌登录 Bangumi。
     *
     * 会真实调用 Bangumi API `/v0/me` 校验令牌有效性：
     * - 200 且返回用户名 → 登录成功并持久化
     * - 否则 → 失败，不写入任何凭据（避免把无效 token 当成已登录）
     */
    suspend fun loginWithToken(context: Context, accessToken: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = accessToken.trim()
                require(trimmed.isNotBlank()) { "令牌不能为空" }

                val resp = client.get("https://api.bgm.tv/v0/me") {
                    header("Authorization", "Bearer $trimmed")
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                }.bodyAsText()

                val user = json.parseToJsonElement(resp).jsonObject
                val username = user["username"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val name = user["nickname"]?.jsonPrimitive?.contentOrNull
                    .orEmpty().ifBlank { username }
                val uid = user["id"]?.jsonPrimitive?.longOrNull ?: 0L
                val face = user["avatar"]?.jsonObject
                    ?.get("large")?.jsonPrimitive?.contentOrNull.orEmpty()

                require(uid > 0L && username.isNotBlank()) {
                    "令牌校验失败：未返回有效用户信息"
                }

                token = trimmed
                userId = uid
                nickname = name
                avatar = face
                isLogin = true

                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                    putString(KEY_TOKEN, trimmed)
                    putLong(KEY_USER_ID, uid)
                    putString(KEY_NICKNAME, name)
                    putString(KEY_AVATAR, face)
                }

                name.ifBlank { "Bangumi 用户" }
            }.onFailure { error ->
                if (error is CancellationException) throw error
            }
        }

    /** 退出 Bangumi 登录（清除本地凭据） */
    fun logout(context: Context) {
        token = ""
        userId = 0L
        nickname = ""
        avatar = ""
        isLogin = false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_NICKNAME)
            remove(KEY_AVATAR)
        }
    }

}

/**
 * 给 Ktor 请求加 Bangumi 授权头（未登录时只加 UA，不加 Authorization）
 *
 * 定义为顶层扩展函数而非 object 成员：成员扩展只能在该 object 作用域内调用，
 * 外部（Repository 里）无法直接使用。
 */
fun io.ktor.client.request.HttpRequestBuilder.withBangumiAuth() {
    require(url.host == "api.bgm.tv" && url.protocol.name == "https") {
        "Bangumi authorization is restricted to https://api.bgm.tv"
    }
    if (BangumiSession.isLogin && BangumiSession.token.isNotBlank()) {
        header("Authorization", "Bearer ${BangumiSession.token}")
    }
    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
}
