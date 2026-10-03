package com.example.pilinara.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Ktor HTTP Client singleton
 * Replaces Dio from Flutter
 */
object KtorClient {
    
    private const val BASE_URL = "https://api.bilibili.com"
    
    val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        
        install(HttpRequestRetry) {
            maxRetries = 3
            retryOnExceptionIf { _, cause ->
                cause is java.net.ConnectException || cause is java.net.SocketTimeoutException
            }
            retryOnServerErrors()
        }
        
        install(HttpExpectation) {
            enabled = true
        }
        
        expectSuccess = true
        
        followRedirects = true
        
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("KtorClient", message)
                }
            }
        }
        
        engine {
            config {
                connectTimeout(30, java.time.Duration.ofSeconds)
                readTimeout(60, java.time.Duration.ofSeconds)
                writeTimeout(60, java.time.Duration.ofSeconds)
            }
        }
    }
    
    suspend fun <T> apiCall(
        block: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun get(path: String, params: Map<String, String> = emptyMap()): Result<io.ktor.client.call.Call> = 
        apiCall {
            client.get("$BASE_URL$path") {
                params.forEach { (k, v) -> urlParameter(k, v) }
            }
        }
}

// ============================================================================
// API Client Wrappers
// ============================================================================

class VideoApiClient(private val client: HttpClient = KtorClient.client) {
    
    suspend fun getPlayUrl(bvid: String, cid: Long): Result<String> {
        return KtorClient.apiCall {
            val response = client.get("/x/player/wbi/playurl") {
                url {
                    parameters.append("bvid", bvid)
                    parameters.append("cid", cid.toString())
                    parameters.append("fnval", "16")
                    parameters.append("fnver", "0")
                    parameters.append("fourk", "1")
                }
            }
            // Parse response and extract stream URL
            "mock_url_$bvid"
        }
    }
    
    suspend fun getVideoInfo(bvid: String): Result<Map<String, Any>> {
        return KtorClient.apiCall {
            val response = client.get("/x/web-interface/view") {
                url {
                    parameters.append("bvid", bvid)
                }
            }
            mapOf("bvid" to bvid)
        }
    }
}

class DanmakuApiClient(private val client: HttpClient = KtorClient.client) {
    
    suspend fun getDanmaku(cid: Long, oid: Long): Result<List<DanmakuItem>> {
        return KtorClient.apiCall {
            val response = client.get("/x/v/dm/v2/get" + 
                "?type=1&oid=$oid&pid=$cid")
            // Parse danmaku XML response
            emptyList()
        }
    }
}

data class DanmakuItem(
    val id: String,
    val mode: Int,
    val fontsize: Int,
    val color: Int,
    val timestamp: Float,
    val content: String,
    val uid: String
)
