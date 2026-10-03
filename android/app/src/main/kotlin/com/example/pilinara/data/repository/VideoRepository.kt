package com.example.pilinara.data.repository

import com.example.pilinara.PlayUrlNativeLib
import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Video repository implementing repository pattern
 */
class VideoRepository(private val apiClient: BiliApiClient) {
    
    suspend fun getPopularVideos(page: Int = 1, pageSize: Int = 20): Result<PopularResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.popularVideos(page, pageSize)
    }
    
    suspend fun getVideoInfo(bvid: String): Result<VideoInfoResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getVideoInfo(bvid)
    }
    
    /**
     * 播放地址：Rust 侧做 DASH 流选择（stage ⑤），失败时回退纯 Kotlin 解析。
     * @return Triple(完整响应, 选中的 video baseUrl, 选中的 audio baseUrl)；Rust 失败时后两者为 null。
     */
    suspend fun getPlayUrl(
        bvid: String,
        cid: Long,
        qn: Int = 80
    ): Result<Triple<PlayUrlResponse, String?, String?>> = withContext(Dispatchers.IO) {
        apiClient.getPlayUrl(bvid, cid, qn).mapCatching { resp ->
            val raw = resp.rawJson
            val selected = raw?.let { PlayUrlNativeLib.select(it, qn) }
            if (selected != null) {
                val arr = JSONObject(selected)
                val video = arr.optJSONObject("video")?.optString("baseUrl")
                val audio = arr.optJSONObject("audio")?.optString("baseUrl")
                Triple(resp, video, audio)
            } else {
                Triple(resp, null, null)
            }
        }
    }

    suspend fun getComments(bvid: String, oid: Long = 0L, pageSize: Int = 20): Result<CommentResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getComments(bvid, oid, pageSize)
    }
    
    suspend fun getDanmaku(cid: Long, oid: Long = 0L): Result<DanmakuResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getDanmaku(cid, oid)
    }
    
    suspend fun getUserInfo(uid: Long): Result<UserInfoResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getUserInfo(uid)
    }
    
    suspend fun getUserDynamics(uid: Long, offset: Long = 0L): Result<DynamicsResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getUserDynamics(uid, offset)
    }
    
    suspend fun getLiveInfo(roomId: Long): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
        apiClient.getLiveInfo(roomId)
    }
    
    suspend fun getFavorites(uid: Long, pageSize: Int = 20): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
        apiClient.getFavorites(uid, pageSize)
    }
    
    suspend fun getHistory(limit: Int = 20): Result<List<VideoItem>> = 
        withContext(Dispatchers.IO) {
        apiClient.getHistory(limit)
    }
}
