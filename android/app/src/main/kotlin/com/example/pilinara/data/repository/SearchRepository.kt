package com.example.pilinara.data.repository

import com.example.pilinara.data.model.SearchResponse
import com.example.pilinara.data.model.SearchSuggest
import com.example.pilinara.data.model.VideoItem
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Search repository implementing repository pattern
 */
class SearchRepository(private val apiClient: BiliApiClient) {
    
    suspend fun searchVideos(
        keyword: String,
        page: Int = 1,
        order: String = "totalrank"
    ): Result<SearchResponse> = withContext(Dispatchers.IO) {
        apiClient.search(keyword, page, order)
    }
    
    suspend fun searchSuggestions(
        keyword: String
    ): Result<SearchSuggest> = withContext(Dispatchers.IO) {
        apiClient.searchSuggest(keyword)
    }
    
    suspend fun getSearchHistory(): List<String> = emptyList()
    
    suspend fun clearSearchHistory() {}
    
    suspend fun addToSearchHistory(keyword: String) {}
    
    suspend fun getHotSearches(): List<String> = listOf(
        "原神", "崩坏：星穹铁道", "英雄联盟", "进击的巨人",
        "咒术回战", "间谍过家家", "鬼灭之刃", "eva"
    )
}
