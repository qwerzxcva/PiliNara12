package com.example.pilinara.data.remote

import com.example.pilinara.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

/**
 * Bilibili API Client using Ktor
 */
class BiliApiClient(private val client: HttpClient = BiliHttpClient.client) {
    
    companion object {
        private const val API_BASE = "https://api.bilibili.com"
        private const val WBI_BASE = "https://api.bilibili.com/x/web-interface/nav"
        
        private val commonHeaders = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
    }
    
    // ========== Popular Videos ==========
    
    suspend fun popularVideos(page: Int = 1, pageSize: Int = 20): Result<PopularResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/popular") {
            url {
                parameters.append("ps", pageSize.toString())
                parameters.append("pn", page.toString())
            }
            commonHeaders.forEach { (k, v) -> header(k, v) }
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
        // playurl 必须 wbi 签名，否则 -404/-352（见 docs/bilibili_api_checklist.md §1.2）
        val signed = WbiSigner.sign(
            mapOf(
                "bvid" to bvid,
                "cid" to cid.toString(),
                "qn" to qn.toString(),
                "fnval" to "16",
                "fnver" to "0",
                "fourk" to "1"
            )
        )
        client.get("$API_BASE/x/player/wbi/playurl") {
            url {
                signed.forEach { (k, v) -> parameters.append(k, v) }
            }
            header("Referer", "https://www.bilibili.com")
        }.bodyAsText().let { text ->
            Json { ignoreUnknownKeys = true }
                .decodeFromString<PlayUrlResponse>(text)
                .copy(rawJson = text)
        }
    }
    
    // ========== Write operations（需登录 + csrf） ==========

    /** 点赞/取消点赞（like=1 点赞, 2 取消；返回 code 0 成功） */
    suspend fun like(aid: Long, like: Int = 1): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/web-interface/archive/like",
            mapOf("aid" to aid.toString(), "like" to like.toString())
        )
    }

    /** 投币（multiply=1/2 个币，需先 like=1 一起勾选可传） */
    suspend fun coin(aid: Long, multiply: Int = 1): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/web-interface/coin/add",
            mapOf("aid" to aid.toString(), "multiply" to multiply.toString(), "select_like" to "0")
        )
    }

    /** 收藏/取消收藏到默认收藏夹（需要先知道 target mid 的默认夹 id；deal=1 收藏 2 取消） */
    suspend fun favorite(aid: Long, mediaId: Long, deal: Int = 1): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/v3/fav/resource/deal",
            mapOf(
                "rid" to aid.toString(),
                "type" to "2",
                "add_media_ids" to if (deal == 1) mediaId.toString() else "",
                "del_media_ids" to if (deal == 2) mediaId.toString() else ""
            )
        )
    }

    /** 上报观看历史（progress 秒；sid/cid 可选） */
    suspend fun reportHistory(aid: Long, cid: Long, progress: Long): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/v2/history/report",
            mapOf(
                "aid" to aid.toString(),
                "cid" to cid.toString(),
                "progress" to progress.toString(),
                "type" to "3"
            )
        )
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

    /**
     * 通用 GET：给路径 + 追加查询参数，反序列化为 reified T。
     * 用于收藏夹/历史等带具体模型的新接口，避免每个都加 wrapper。
     * 注意：inline 函数不能访问私有成员，故直接用公开的 BiliHttpClient.client。
     */
    suspend inline fun <reified T : Any> getFavoritesRaw(
        path: String,
        noinline params: io.ktor.http.ParametersBuilder.() -> Unit = {}
    ): T = BiliHttpClient.client.get("https://api.bilibili.com/$path") {
        // URLBuilder.parameters 就是 ParametersBuilder，应用 lambda
        url.parameters.apply(params)
        header("Referer", "https://www.bilibili.com")
    }.body()

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
