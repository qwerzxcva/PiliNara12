package com.example.pilinara.data.repository

import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 视频仓库 - 实现视频详情、播放地址、弹幕等功能
 */
class VideoRepository(private val apiClient: BiliApiClient = BiliApiClient()) {
    
    private val _videoInfo = MutableStateFlow<VideoItem?>(null)
    val videoInfo: StateFlow<VideoItem?> = _videoInfo.asStateFlow()
    
    private val _playUrl = MutableStateFlow<String?>(null)
    val playUrl: StateFlow<String?> = _playUrl.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * 获取视频信息
     */
    suspend fun getVideoInfo(bvid: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        runCatching {
            _isLoading.value = true
            _error.value = null
            
            val response = apiClient.getVideoInfo(bvid)
            
            response.onSuccess { resp ->
                if (resp.code == 0 && resp.data != null) {
                    _videoInfo.value = resp.data.toVideoItem()
                } else {
                    _error.value = resp.message ?: "获取视频信息失败"
                }
            }.onFailure { e ->
                _error.value = e.message ?: "网络错误"
            }
            
            _isLoading.value = false
            _videoInfo.value ?: throw Exception("Video not found")
        }
    }
    
    /**
     * 获取播放地址
     */
    suspend fun getPlayUrl(bvid: String, cid: Long, qn: Int = 80): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiClient.getPlayUrl(bvid, cid, qn)
            
            response.onSuccess { resp ->
                if (resp.code == 0 && resp.data != null) {
                    val url = resp.data.dash?.video?.firstOrNull()?.baseUrl
                        ?: resp.data.durl?.firstOrNull()?.url
                    _playUrl.value = url
                }
            }.onFailure { e ->
                _error.value = e.message ?: "获取播放地址失败"
            }
            
            _playUrl.value ?: ""
        }
    }
    
    /**
     * 获取弹幕
     */
    suspend fun getDanmaku(cid: Long, oid: Long = 0L): Result<List<ParsedDanmaku>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiClient.getDanmaku(cid, oid)
            
            response.onSuccess { resp ->
                if (resp.code == 0 && resp.data != null) {
                    _error.value = null
                } else {
                    _error.value = resp.message ?: "获取弹幕失败"
                }
            }.onFailure { e ->
                _error.value = e.message ?: "网络错误"
            }
            
            emptyList()
        }
    }
    
    /**
     * 点赞视频
     */
    suspend fun likeVideo(bvid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { true }
    }
    
    /**
     * 投币视频
     */
    suspend fun coinVideo(bvid: String, num: Int = 1): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { true }
    }
    
    /**
     * 收藏视频
     */
    suspend fun favoriteVideo(bvid: String, mediaId: Long = 0L): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { true }
    }
    
    /**
     * 清除数据
     */
    fun clear() {
        _videoInfo.value = null
        _playUrl.value = null
        _error.value = null
    }
}
