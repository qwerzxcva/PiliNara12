package com.example.piliai.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object BiliHttpClient {
    // 共享单例：避免每次解析都新建 Json 实例（性能 + 统一容错策略）
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true   // 审核4：null/越界值回退默认值，防线上字段突变崩溃
        explicitNulls = false
    }
    
    val client = HttpClient(OkHttp) {
        // Enforce credential boundaries on every actual network hop.
        followRedirects = false
        engine {
            config {
                followRedirects(true)
                followSslRedirects(false)
                addNetworkInterceptor { chain ->
                    val original = chain.call().request().url
                    val request = chain.request()
                    val destination = request.url
                    val originalTrusted = original.scheme == "https" &&
                        original.port == 443 &&
                        (original.host == "bilibili.com" ||
                            original.host.endsWith(".bilibili.com"))
                    val destinationTrusted = destination.scheme == "https" &&
                        destination.port == 443 &&
                        (destination.host == "bilibili.com" ||
                            destination.host.endsWith(".bilibili.com"))
                    val builder = request.newBuilder().removeHeader("Cookie")
                    if (originalTrusted && destinationTrusted) {
                        val cookie = AccountSession.cookieHeader()
                        if (cookie.isNotEmpty()) builder.header("Cookie", cookie)
                    } else {
                        builder.removeHeader("Authorization")
                        builder.removeHeader("Proxy-Authorization")
                    }
                    chain.proceed(builder.build())
                }
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        // 审核31：请求级超时（弱网下防挂死）——总 30s，连接 15s，socket 20s
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
        // 审核31：幂等 GET 失败自动重试（网络抖动/风控瞬时），其余方法不重试
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryOnExceptionIf { _, cause -> cause is java.io.IOException }
            exponentialDelay(base = 1.5, maxDelayMs = 4_000)
            retryIf(maxRetries = 2) { _, resp -> resp.status.value in 500..599 }
        }
        
        defaultRequest {
            url("https://api.bilibili.com")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")
        }
    }
    
    suspend inline fun <reified T : Any> get(path: String, noinline block: HttpRequestBuilder.() -> Unit = {}): T = withContext(Dispatchers.IO) {
        client.get(path) {
            block()
        }.body()
    }

    suspend inline fun <reified T : Any> post(path: String, body: Any? = null, noinline block: HttpRequestBuilder.() -> Unit = {}): T = withContext(Dispatchers.IO) {
        client.post(path) {
            if (body != null) {
                setBody(body)
            }
            block()
        }.body()
    }

    suspend inline fun <reified T : Any> postForm(path: String, form: Map<String, String>): T = withContext(Dispatchers.IO) {
        client.submitForm(
            url = path,
            formParameters = Parameters.build {
                form.forEach { (key, value) -> append(key, value) }
            }
        ).body()
    }

    /**
     * 带登录态的表单 POST（写操作：点赞/投币/收藏等）。
     * B站写操作统一要求：SESSDATA cookie（CookieJar 自动带）+ csrf=bili_jct 表单字段 + Referer。
     */
    suspend inline fun <reified T : Any> postAuthForm(
        path: String,
        form: Map<String, String>,
        noinline block: HttpRequestBuilder.() -> Unit = {}
    ): T = withContext(Dispatchers.IO) {
        val csrf = AccountSession.snapshot()["bili_jct"].orEmpty()
        client.post(path) {
            header("Referer", "https://www.bilibili.com")
            setBody(FormDataContent(Parameters.build {
                form.forEach { (k, v) -> append(k, v) }
                if (csrf.isNotEmpty()) append("csrf", csrf)
            }))
            block()
        }.body()
    }
}
