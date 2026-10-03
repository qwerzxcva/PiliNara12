package com.example.pilinara.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.data.model.SearchResponse
import com.example.pilinara.data.model.SearchSuggest
import com.example.pilinara.data.model.SuggestResult
import com.example.pilinara.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {
    
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    data class SearchState(
        val keyword: String = "",
        val isSearching: Boolean = false,
        val searchResults: List<Any> = emptyList(),
        val suggestions: List<SuggestResult> = emptyList(),
        val hotSearches: List<String> = emptyList(),
        val error: String? = null
    )
    
    data class SuggestionItem(
        val mid: Long = 0L,
        val name: String = "",
        val face: String = ""
    )
    
    fun setSearchKeyword(keyword: String) {
        _searchState.value = _searchState.value.copy(keyword = keyword)
        if (keyword.isNotEmpty()) {
            fetchSuggestions(keyword)
        } else {
            _searchState.value = _searchState.value.copy(suggestions = emptyList())
        }
    }
    
    fun performSearch(page: Int = 1) {
        val keyword = _searchState.value.keyword
        if (keyword.isEmpty()) return
        
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(isSearching = true, error = null)
            
            searchRepository.searchVideos(keyword, page).onSuccess { result ->
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    searchResults = result.result,
                    hotSearches = emptyList()
                )
            }.onFailure { e ->
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    error = e.message
                )
            }
        }
    }
    
    fun selectSuggestion(suggestion: SuggestResult) {
        _searchState.value = _searchState.value.copy(keyword = suggestion.uname)
        performSearch()
    }
    
    private fun fetchSuggestions(keyword: String) {
        viewModelScope.launch {
            searchRepository.searchSuggestions(keyword).onSuccess { result ->
                // Parse suggestions
            }
        }
    }
    
    fun clearSearch() {
        _searchState.value = SearchState()
    }
}

data class SearchState(
    val keyword: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<Any> = emptyList(),
    val suggestions: List<SuggestResult> = emptyList(),
    val hotSearches: List<String> = emptyList(),
    val error: String? = null
)
