package com.example.pilinara.data.remote

import com.example.pilinara.data.model.PopularResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Bilibili API 客户端（对应 Dart 侧 Request() + Constants.apiBaseUrl）
 * B 站返回字段远超本地模型，必须 ignoreUnknownKeys，否则反序列化抛异常
 */
class BiliApiClient(
    private val client: HttpClient = defaultHttpClient,
) {
    companion object {
        const val API_BASE = "https://api.bilibili.com"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"

        val json: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

        val defaultHttpClient: HttpClient by lazy {
            HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 20_000
                    connectTimeoutMillis = 10_000
                }
                install(ContentEncoding)
                install(ContentNegotiation) {
                    json(json)
                }
            }
        }
    }

    suspend fun popularVideos(page: Int = 1, pageSize: Int = 20): Result<PopularResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/popular") {
            url {
                parameters.append("pn", page.toString())
                parameters.append("ps", pageSize.toString())
            }
            header("User-Agent", USER_AGENT)
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
}
