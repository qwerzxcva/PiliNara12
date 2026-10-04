package com.example.pilinara.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 仓库 ViewModel - 管理历史、收藏、离线缓存数据
 */
class LibraryViewModel : ViewModel() {
    
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()
    
    private val _favoriteItems = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favoriteItems: StateFlow<List<FavoriteItem>> = _favoriteItems.asStateFlow()
    
    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * 加载历史记录
     */
    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: 从 Room 数据库加载
            _isLoading.value = false
        }
    }
    
    /**
     * 加载收藏夹
     */
    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: 从 Room 数据库加载
            _isLoading.value = false
        }
    }
    
    /**
     * 加载下载列表
     */
    fun loadDownloads() {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: 从本地存储加载
            _isLoading.value = false
        }
    }
    
    /**
     * 添加到历史
     */
    fun addToHistory(item: HistoryItem) {
        viewModelScope.launch {
            val current = _historyItems.value.toMutableList()
            current.add(0, item)
            _historyItems.value = current.take(100) // 保留最近100条
        }
    }
    
    /**
     * 添加收藏
     */
    fun addToFavorites(item: FavoriteItem) {
        viewModelScope.launch {
            val current = _favoriteItems.value.toMutableList()
            current.add(0, item)
            _favoriteItems.value = current
        }
    }
    
    /**
     * 移除收藏
     */
    fun removeFromFavorites(id: Long) {
        viewModelScope.launch {
            val current = _favoriteItems.value.filter { it.id != id }
            _favoriteItems.value = current
        }
    }
    
    /**
     * 添加下载任务
     */
    fun addDownload(item: DownloadItem) {
        viewModelScope.launch {
            val current = _downloadItems.value.toMutableList()
            current.add(0, item)
            _downloadItems.value = current
        }
    }
    
    /**
     * 更新下载进度
     */
    fun updateDownloadProgress(id: Long, progress: Int) {
        viewModelScope.launch {
            val current = _downloadItems.value.map { item ->
                if (item.id == id) item.copy(progress = progress) else item
            }
            _downloadItems.value = current
        }
    }
    
    /**
     * 删除下载项
     */
    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            val current = _downloadItems.value.filter { it.id != id }
            _downloadItems.value = current
        }
    }
    
    /**
     * 清空历史
     */
    fun clearHistory() {
        viewModelScope.launch {
            _historyItems.value = emptyList()
        }
    }
    
    /**
     * 清空下载
     */
    fun clearDownloads() {
        viewModelScope.launch {
            _downloadItems.value = emptyList()
        }
    }
    
    init {
        loadHistory()
        loadFavorites()
        loadDownloads()
    }
}
