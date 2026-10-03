package com.example.pilinara.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Bilibili API Service
 * Replaces Flutter Dio-based API clients
 */
class ApiService(private val client: HttpClient = KtorClient.client) {
    
    companion object {
        private const val BASE_URL = "https://api.bilibili.com"
        private const val X_BILI_WBI = "https://api.bilibili.com/x/web-interface/nav"
    }
    
    data class ApiResponse<T>(
        val code: Int,
        val message: String,
        val data: T?
    )
    
    // ========== User API ==========
    
    suspend fun getUserInfo(): Result<ApiResponse<UserInfo>> = withContext(Dispatchers.IO) {
        apiCall { client.get("$BASE_URL/x/web-interface/nav") }
    }
    
    @Serializable
    data class UserInfo(
        val mid: Long,
        val uname: String,
        val face: String,
        val vipStatus: Int,
        val vipType: Int,
        val levelInfo: LevelInfo,
        val coins: Double,
        val fans: Long,
        val friend: Long,
        val attention: Long
    )
    
    @Serializable
    data class LevelInfo(
        val currentLevel: Int,
        val currentMin: Long,
        val currentExp: Long,
        val nextExp: Long
    )
    
    // ========== Video API ==========
    
    suspend fun getVideoInfo(bvid: String): Result<ApiResponse<VideoInfo>> = withContext(Dispatchers.IO) {
        apiCall { client.get("$BASE_URL/x/web-interface/view") { url { parameter("bvid", bvid) } } }
    }
    
    @Serializable
    data class VideoInfo(
        val bvid: String,
        val aid: Long,
        val cid: Long,
        val title: String,
        val desc: String,
        val pic: String,
        val pubdate: Long,
        val duration: Long,
        val stat: VideoStat,
        val owner: OwnerInfo,
        val pages: List<PageInfo>
    )
    
    @Serializable
    data class VideoStat(
        val play: Long,
        val danmaku: Long,
        val reply: Long,
        val favorite: Long,
        val coin: Long,
        val like: Long,
        val share: Long,
        val nowRank: Long,
        val hisRank: Long,
        val view: Long
    )
    
    @Serializable
    data class OwnerInfo(
        val mid: Long,
        val name: String,
        val face: String
    )
    
    @Serializable
    data class PageInfo(
        val cid: Long,
        val page: Int,
        val from: String,
        val part: String,
        val duration: Long,
        val vid: String?,
        val desc: String,
        val weblink: String
    )
    
    // ========== Play URL API ==========
    
    suspend fun getPlayUrl(bvid: String, cid: Long, qn: Int = 80): Result<ApiResponse<PlayUrlInfo>> = 
        withContext(Dispatchers.IO) {
        apiCall {
            client.get("$BASE_URL/x/player/playurl") {
                url {
                    parameter("bvid", bvid)
                    parameter("cid", cid.toString())
                    parameter("fnval", "16")
                    parameter("fnver", "0")
                    parameter("fourk", "1")
                    parameter("qn", qn.toString())
                }
            }
        }
    }
    
    @Serializable
    data class PlayUrlInfo(
        val quality: Int,
        val format: String,
        val timelength: Long,
        val acceptQuality: List<QualityInfo>,
        val dash: DashInfo?
    )
    
    @Serializable
    data class QualityInfo(
        val id: Int,
        val quality: Int,
        val format: String,
        val newDescription: String,
        val displayDesc: String
    )
    
    @Serializable
    data class DashInfo(
        val duration: Long,
        val minBufferTime: Float,
        val video: List<StreamInfo>,
        val audio: List<StreamInfo>
    )
    
    @Serializable
    data class StreamInfo(
        val id: Int,
        val baseUrl: String?,
        val backupUrl: List<String>,
        val codecs: String,
        val bandwidth: Int,
        val width: Int?,
        val height: Int?,
        val frameRate: String?,
        val md5: String?,
        val size: Long?
    )
    
    // ========== Danmaku API ==========
    
    suspend fun getDanmaku(cid: Long, oid: Long): Result<ApiResponse<List<DanmakuItem>>> = 
        withContext(Dispatchers.IO) {
        apiCall {
            client.get("$BASE_URL/x/v/dm/v2/get") {
                url {
                    parameter("type", "1")
                    parameter("oid", oid.toString())
                    parameter("pid", cid.toString())
                }
            }
        }
    }
    
    @Serializable
    data class DanmakuItem(
        val p: String,
        val m: String,
        val c: String,
        val id: String
    )
    
    // ========== Feed API ==========
    
    suspend fun getFeed(): Result<ApiResponse<List<FeedItem>>> = withContext(Dispatchers.IO) {
        apiCall {
            client.get("$BASE_URL/x/web-interface/feed")
        }
    }
    
    @Serializable
    data class FeedItem(
        val arc: VideoInfo,
        val tag: List<String>
    )
    
    // ========== Search API ==========
    
    suspend fun search(keyword: String, page: Int = 1, order: String = "totalrank"): 
        Result<ApiResponse<SearchResult>> = withContext(Dispatchers.IO) {
        apiCall {
            client.get("$BASE_URL/x/web-interface/search/all/v2") {
                url {
                    parameter("keyword", keyword)
                    parameter("page", page.toString())
                    parameter("order", order)
                }
            }
        }
    }
    
    @Serializable
    data class SearchResult(
        val numResults: Int,
        val pages: Int,
        val result: List<SearchItem>
    )
    
    @Serializable
    data class SearchItem(
        val type: String,
        val result: Any?
    )
    
    // ========== Subscriptions API ==========
    
    suspend fun getSubscriptions(page: Int = 1): Result<ApiResponse<List<Subscription>>> = 
        withContext(Dispatchers.IO) {
        apiCall {
            client.get("$BASE_URL/x/relation/lists") {
                url {
                    parameter("vmid", "0") // Current user
                    parameter("pn", page.toString())
                    parameter("ps", 50)
                }
            }
        }
    }
    
    @Serializable
    data class Subscription(
        val mid: Long,
        val uname: String,
        val face: String,
        val following: Long,
        val fans: Long,
        val sign: String
    )
    
    // ========== Dynamic API ==========
    
    suspend fun getDynamics(uid: Long, offset: Long = 0): Result<ApiResponse<DynamicsResult>> = 
        withContext(Dispatchers.IO) {
        apiCall {
            client.get("$BASE_URL/x/polymer/web-dynamic/v1/feed/space") {
                url {
                    parameter("host_uid", uid.toString())
                    parameter("offset", offset.toString())
                }
            }
        }
    }
    
    @Serializable
    data class DynamicsResult(
        items: List<DynamicItem>
    )
    
    @Serializable
    data class DynamicItem(
        val dynamic_id: Long,
        val uid: Long,
        val type: String,
        val content: String,
        val cover: String,
        val like_count: Int,
        val reply_count: Int,
        val forward_count: Int
    )
    
    // ========== Helper functions ==========
    
    private suspend fun <T : Any> apiCall(block: suspend () -> io.ktor.client.call.Call): Result<ApiResponse<T>> {
        return try {
            val call = block()
            val response = call.body<ApiResponse<T>>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
