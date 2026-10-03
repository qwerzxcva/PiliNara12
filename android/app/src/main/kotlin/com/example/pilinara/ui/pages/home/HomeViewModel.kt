package com.example.pilinara.ui.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.data.model.VideoItem
import com.example.pilinara.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {
    
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        refresh()
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getPopularVideos().collect { result ->
                result.onSuccess { items ->
                    _videos.value = items
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
            
            _isLoading.value = false
        }
    }
    
    fun loadMore() {
        // Implement pagination
    }
}
