package com.example.pilinara.data.repository

import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 首页视频仓库 - 实现真实 Bilibili API 数据加载
 */
class HomeRepository(private val apiClient: BiliApiClient = BiliApiClient()) {
    
    // 热门视频列表缓存
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * 获取热门视频列表
     */
    suspend fun getPopularVideos(page: Int = 1, pageSize: Int = 20): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            _isLoading.value = true
            _error.value = null
            
            val response = apiClient.popularVideos(page, pageSize)
            
            response.onSuccess { resp ->
                if (resp.code == 0 && resp.data != null) {
                    // 审核实测（Android 16 模拟器）：直接透传响应 item，手动重建会丢 stat（播放量恒 0）
                    _videos.value = resp.data.list
                } else {
                    _error.value = resp.message ?: "获取数据失败"
                }
            }.onFailure { e ->
                _error.value = e.message ?: "网络错误"
            }
            
            _isLoading.value = false
            _videos.value
        }
    }
    
    /**
     * 刷新视频列表
     */
    suspend fun refresh(): Result<List<VideoItem>> {
        return getPopularVideos(1, 20)
    }
    
    /**
     * 加载更多视频
     */
    suspend fun loadMore(page: Int): Result<List<VideoItem>> {
        val currentVideos = _videos.value.toMutableList()
        val response = apiClient.popularVideos(page, 20)
        
        return response.onSuccess { resp ->
            if (resp.code == 0 && resp.data != null) {
                // 审核实测：同上，直接透传（勿手动重建丢 stat）
                val newVideos = resp.data.list
                currentVideos.addAll(newVideos)
                _videos.value = currentVideos
            }
        }.mapCatching { currentVideos }
    }
    
    /**
     * 清空数据
     */
    fun clear() {
        _videos.value = emptyList()
        _error.value = null
    }
}
