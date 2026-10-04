package com.example.pilinara.data.repository

import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 首页视频仓库 - 实现真实 Bilibili API 数据加载
 */
class HomeRepository(private val apiClient: BiliApiClient = BiliApiClient()) {
    
    // 热门视频列表缓存
    private val _videos = mutableStateOf<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = mutableStateOf<String?>(null)
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
                    _videos.value = resp.data.list.map { item ->
                        VideoItem(
                            bvid = item.bvid,
                            aid = item.aid,
                            cid = item.cid ?: 0L,
                            title = item.title,
                            author = item.owner?.name ?: "未知UP主",
                            authorMid = item.owner?.mid ?: 0L,
                            authorFace = item.owner?.face ?: "",
                            cover = item.pic,
                            duration = item.duration?.toLong() ?: 0L,
                            play = item.stat?.play ?: 0L,
                            danmaku = item.stat?.danmaku ?: 0L,
                            reply = item.stat?.reply ?: 0L,
                            favorite = item.stat?.favorite ?: 0L,
                            coin = item.stat?.coin ?: 0L,
                            like = item.stat?.like ?: 0L,
                            desc = item.desc,
                            pubdate = item.pubdate
                        )
                    }
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
    suspend fun refresh() {
        getPopularVideos(1, 20)
    }
    
    /**
     * 加载更多视频
     */
    suspend fun loadMore(page: Int) {
        val currentVideos = _videos.value.toMutableList()
        val response = apiClient.popularVideos(page, 20)
        
        response.onSuccess { resp ->
            if (resp.code == 0 && resp.data != null) {
                val newVideos = resp.data.list.map { item ->
                    VideoItem(
                        bvid = item.bvid,
                        aid = item.aid,
                        cid = item.cid ?: 0L,
                        title = item.title,
                        author = item.owner?.name ?: "未知UP主",
                        authorMid = item.owner?.mid ?: 0L,
                        authorFace = item.owner?.face ?: "",
                        cover = item.pic,
                        duration = item.duration?.toLong() ?: 0L,
                        play = item.stat?.play ?: 0L,
                        danmaku = item.stat?.danmaku ?: 0L,
                        reply = item.stat?.reply ?: 0L,
                        favorite = item.stat?.favorite ?: 0L,
                        coin = item.stat?.coin ?: 0L,
                        like = item.stat?.like ?: 0L,
                        desc = item.desc,
                        pubdate = item.pubdate
                    )
                }
                currentVideos.addAll(newVideos)
                _videos.value = currentVideos
            }
        }
    }
    
    /**
     * 清空数据
     */
    fun clear() {
        _videos.value = emptyList()
        _error.value = null
    }
}
