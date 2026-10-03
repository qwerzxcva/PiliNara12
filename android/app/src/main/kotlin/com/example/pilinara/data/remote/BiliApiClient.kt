package com.example.pilinara.data.remote

import com.example.pilinara.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Bilibili API Client using Ktor
 */
class BiliApiClient(private val client: HttpClient = BiliHttpClient.client) {
    
    companion object {
        private const val API_BASE = "https://api.bilibili.com"
        private const val WBI_BASE = "https://api.bilibili.com/x/web-interface/nav"
    }
    
    // ========== Popular Videos ==========
    
    suspend fun popularVideos(page: Int = 1, pageSize: Int = 20): Result<PopularResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/popular") {
            url {
                parameters.append("pn", page.toString())
                parameters.append("ps", pageSize.toString())
                parameters.append("type", "rec")
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== Video Info ==========
    
    suspend fun getVideoInfo(bvid: String): Result<VideoInfoResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/view") {
            url {
                parameters.append("bvid", bvid)
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== Play URL ==========
    
    suspend fun getPlayUrl(bvid: String, cid: Long, qn: Int = 80): Result<PlayUrlResponse> = runCatching {
        client.get("$API_BASE/x/player/wbi/playurl") {
            url {
                parameters.append("bvid", bvid)
                parameters.append("cid", cid.toString())
                parameters.append("fnval", "16")  // DASH + flac
                parameters.append("fnver", "0")
                parameters.append("fourk", "1")
                parameters.append("qn", qn.toString())
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== Search ==========
    
    suspend fun search(keyword: String, page: Int = 1, order: String = "totalrank"): Result<SearchResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/search/all/v2") {
            url {
                parameters.append("keyword", keyword)
                parameters.append("page", page.toString())
                parameters.append("order", order)
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    suspend fun searchSuggest(keyword: String): Result<SearchSuggest> = runCatching {
        client.get("$API_BASE/suggestion/search") {
            url {
                parameters.append("keyword", keyword)
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== Comments ==========
    
    suspend fun getComments(bvid: String, oid: Long = 0L, page: Int = 1, pageSize: Int = 20): Result<CommentResponse> = runCatching {
        client.get("$API_BASE/x/v2/reply/main") {
            url {
                parameters.append("oid", oid.toString())
                parameters.append("type", "1")  // 1=video
                parameters.append("pn", page.toString())
                parameters.append("ps", pageSize.toString())
                parameters.append("sort", "1")  // 1=hot, 0=time
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== Danmaku ==========
    
    suspend fun getDanmaku(cid: Long, oid: Long = 0L): Result<DanmakuResponse> = runCatching {
        client.get("$API_BASE/x/v1/dm/list.so") {
            url {
                parameters.append("oid", oid.toString())
                parameters.append("pid", cid.toString())
                parameters.append("type", "1")
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== User Info ==========
    
    suspend fun getUserInfo(uid: Long): Result<UserInfoResponse> = runCatching {
        client.get("$API_BASE/x/space/wbi/acc/info") {
            url {
                parameters.append("mid", uid.toString())
            }
            header("Referer", "https://space.bilibili.com")
        }.body()
    }
    
    suspend fun getUserSpace(uid: Long): Result<UserInfoResponse> = runCatching {
        client.get("$API_BASE/x/space/acc/info") {
            url {
                parameters.append("mid", uid.toString())
            }
            header("Referer", "https://space.bilibili.com")
        }.body()
    }
    
    // ========== Dynamics ==========
    
    suspend fun getUserDynamics(uid: Long, offset: Long = 0L): Result<DynamicsResponse> = runCatching {
        client.get("$API_BASE/x/polymer/web-dynamic/v1/feed/space") {
            url {
                parameters.append("host_uid", uid.toString())
                parameters.append("offset", offset.toString())
            }
            header("Referer", "https://t.bilibili.com")
        }.body()
    }
    
    suspend fun getFollowingsFeed(offset: Long = 0L): Result<DynamicsResponse> = runCatching {
        client.get("$API_BASE/x/polymer/web-dynamic/v1/feed/all") {
            url {
                parameters.append("offset", offset.toString())
            }
            header("Referer", "https://t.bilibili.com")
        }.body()
    }
    
    // ========== Live ==========
    
    suspend fun getLiveInfo(roomId: Long): Result<Map<String, Any>> = runCatching {
        client.get("$API_BASE/room/v1/Room/get_info") {
            url {
                parameters.append("room_id", roomId.toString())
            }
            header("Referer", "https://live.bilibili.com")
        }.body()
    }
    
    // ========== Favorites ==========
    
    suspend fun getFavorites(uid: Long, pageSize: Int = 20, mediaType: String = "video"): Result<Map<String, Any>> = runCatching {
        client.get("$API_BASE/x/v3/fav/folder/created/list-all") {
            url {
                parameters.append("up_mid", uid.toString())
                parameters.append("media_type", mediaType)
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== History ==========
    
    suspend fun getHistory(limit: Int = 20): Result<List<VideoItem>> = runCatching {
        client.get("$API_BASE/x/v2/history/toview") {
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== WBI Signature ==========
    
    suspend fun withWbi(url: String, params: Map<String, String>): String {
        // Simplified WBI signing - in production would fetch mixin key and sign
        return buildString {
            append(url)
            params.forEach { (k, v) ->
                append("&$k=$v")
            }
        }
    }
}
