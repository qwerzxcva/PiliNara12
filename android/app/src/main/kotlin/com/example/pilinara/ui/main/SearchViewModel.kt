package com.example.pilinara.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.data.model.SearchResultItem
import com.example.pilinara.data.model.SuggestResult
import com.example.pilinara.data.model.TrendingItem
import com.example.pilinara.data.remote.BiliApiClient
import com.example.pilinara.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository = SearchRepository()
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val api = BiliApiClient()

    // 搜索历史（进程内持久；重启后清零，后续可挂 Room）
    private val history = mutableListOf<String>()

    data class SearchState(
        val keyword: String = "",
        val isSearching: Boolean = false,
        val results: List<SearchResultItem> = emptyList(),
        val suggestions: List<SuggestResult> = emptyList(),
        val trending: List<TrendingItem> = emptyList(),
        val history: List<String> = emptyList(),
        val searchType: String = "video",     // video / bili_user / live
        val order: String = "",               // totalrank/click/pubdate/danmaku/stow
        val duration: Int = 0,                // 0/10/30/60 分钟
        val page: Int = 1,
        val hasMore: Boolean = false,
        val error: String? = null
    )

    fun setSearchKeyword(keyword: String) {
        _searchState.value = _searchState.value.copy(keyword = keyword)
        if (keyword.isNotEmpty()) fetchSuggestions(keyword)
        else _searchState.value = _searchState.value.copy(suggestions = emptyList())
    }

    fun performSearch(page: Int = 1) {
        val keyword = _searchState.value.keyword
        if (keyword.isEmpty()) return

        // 记入历史（去重、最前）
        history.remove(keyword)
        history.add(0, keyword)
        if (history.size > 20) history.removeAt(history.size - 1)

        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(
                isSearching = true, error = null, page = page,
                history = history.toList()
            )
            api.searchByType(
                keyword = keyword,
                searchType = _searchState.value.searchType,
                page = page,
                order = _searchState.value.order,
                duration = _searchState.value.duration
            ).onSuccess { resp ->
                if (resp.code == 0) {
                    val items = resp.data?.result.orEmpty()
                    _searchState.value = _searchState.value.copy(
                        isSearching = false,
                        results = if (page == 1) items
                        else _searchState.value.results + items,
                        hasMore = items.size >= 20,
                        suggestions = emptyList()
                    )
                } else {
                    _searchState.value = _searchState.value.copy(
                        isSearching = false, error = resp.message.ifEmpty { "搜索失败" }
                    )
                }
            }.onFailure { e ->
                _searchState.value = _searchState.value.copy(isSearching = false, error = e.message)
            }
        }
    }

    fun loadMore() {
        val s = _searchState.value
        // 审核42：防止快速滚动重入（isSearching 检查 + 页码去重）
        if (s.isSearching || !s.hasMore) return
        if (s.page == lastRequestedPage) return
        lastRequestedPage = s.page + 1
        performSearch(s.page + 1)
    }

    private var lastRequestedPage = 0

    fun setFilter(searchType: String? = null, order: String? = null, duration: Int? = null) {
        _searchState.value = _searchState.value.copy(
            searchType = searchType ?: _searchState.value.searchType,
            order = order ?: _searchState.value.order,
            duration = duration ?: _searchState.value.duration
        )
        if (_searchState.value.keyword.isNotEmpty()) performSearch(1)
    }

    /** 加载热搜（首屏一次） */
    fun loadTrending() {
        if (_searchState.value.trending.isNotEmpty()) return
        viewModelScope.launch {
            api.getSearchTrending().onSuccess { items ->
                _searchState.value = _searchState.value.copy(trending = items)
            }
        }
    }

    fun clearHistory() {
        history.clear()
        _searchState.value = _searchState.value.copy(history = emptyList())
    }

    fun selectSuggestion(suggestion: SuggestResult) {
        _searchState.value = _searchState.value.copy(keyword = suggestion.uname)
        performSearch()
    }

    fun clearSearch() {
        _searchState.value = SearchState(trending = _searchState.value.trending, history = history.toList())
    }

    private fun fetchSuggestions(keyword: String) {
        viewModelScope.launch {
            searchRepository.searchSuggestions(keyword).onSuccess { result ->
                _searchState.value = _searchState.value.copy(suggestions = result)
            }
        }
    }
}
