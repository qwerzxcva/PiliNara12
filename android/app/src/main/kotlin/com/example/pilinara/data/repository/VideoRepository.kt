package com.example.pilinara.data.repository

import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    
    suspend fun getPlayUrl(bvid: String, cid: Long, qn: Int = 80): Result<PlayUrlResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getPlayUrl(bvid, cid, qn)
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
