package com.example.pilinara.data.repository

import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 搜索仓库 - 实现搜索功能
 */
class SearchRepository(private val apiClient: BiliApiClient = BiliApiClient()) {
    
    private val _searchResults = mutableStateOf<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()
    
    private val _suggestions = mutableStateOf<List<SuggestResult>>(emptyList())
    val suggestions: StateFlow<List<SuggestResult>> = _suggestions.asStateFlow()
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = mutableStateOf<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * 搜索视频
     */
    suspend fun searchVideos(keyword: String, page: Int = 1): Result<List<SearchResultItem>> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            _isLoading.value = true
            _error.value = null
            
            val response = apiClient.search(keyword, page)
            
            response.onSuccess { resp ->
                if (resp.code == 0) {
                    _searchResults.value = resp.result.filter { it.resultType == "video" }
                } else {
                    _error.value = resp.message ?: "搜索失败"
                }
            }.onFailure { e ->
                _error.value = e.message ?: "网络错误"
            }
            
            _isLoading.value = false
            _searchResults.value
        }
    }
    
    /**
     * 获取搜索建议
     */
    suspend fun getSearchSuggestions(keyword: String): Result<List<SuggestResult>> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val response = apiClient.searchSuggest(keyword)
            
            response.onSuccess { resp ->
                _suggestions.value = resp.results
            }.onFailure { e ->
                // 忽略错误
            }
        }
    }
    
    /**
     * 获取热门标签
     */
    suspend fun getHotSearches(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            // 从 Bilibili API 获取或返回默认值
            listOf("原神", "崩坏：星穹铁道", "英雄联盟", "进击的巨人", "咒术回战")
        }
    }
    
    /**
     * 清空数据
     */
    fun clear() {
        _searchResults.value = emptyList()
        _suggestions.value = emptyList()
        _error.value = null
    }
}
