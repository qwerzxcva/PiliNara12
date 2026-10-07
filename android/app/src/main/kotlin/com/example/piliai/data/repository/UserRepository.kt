package com.example.piliai.data.repository

import com.example.piliai.data.model.*
import com.example.piliai.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * User repository implementing repository pattern
 */
class UserRepository(private val apiClient: BiliApiClient) {
    
    suspend fun getUserInfo(uid: Long): Result<UserInfoResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getUserInfo(uid)
    }
    
    suspend fun getUserSpace(uid: Long): Result<UserInfoResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getUserSpace(uid)
    }
    
    suspend fun getUserDynamics(uid: Long, offset: Long = 0L): Result<DynamicsResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getUserDynamics(uid, offset)
    }
    
    suspend fun getFollowingsFeed(offset: Long = 0L): Result<DynamicsResponse> = 
        withContext(Dispatchers.IO) {
        apiClient.getFollowingsFeed(offset)
    }
    
    suspend fun getFavorites(uid: Long, pageSize: Int = 20, mediaType: String = "video"): Result<Map<String, Any>> = 
        withContext(Dispatchers.IO) {
        apiClient.getFavorites(uid, pageSize, mediaType)
    }
    
    suspend fun getFollowings(uid: Long, page: Int = 1): Result<CommentResponse> =
        withContext(Dispatchers.IO) {
        apiClient.getComments("", uid) // Placeholder
    }
    
    suspend fun getFollowingCount(uid: Long): Result<Long> = 
        withContext(Dispatchers.IO) {
        runCatching { 0L }
    }
    
    suspend fun getFollowerCount(uid: Long): Result<Long> = 
        withContext(Dispatchers.IO) {
        runCatching { 0L }
    }
}
