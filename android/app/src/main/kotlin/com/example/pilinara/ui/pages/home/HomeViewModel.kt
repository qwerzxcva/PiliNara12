package com.example.pilinara.ui.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.data.model.VideoItem
import com.example.pilinara.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val items: List<VideoItem>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repo: HomeRepository = HomeRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // 批次L29：首页顶部大卡轮播
    private val _topRcmd = MutableStateFlow<List<com.example.pilinara.data.model.TopRcmdItem>>(emptyList())
    val topRcmd: StateFlow<List<com.example.pilinara.data.model.TopRcmdItem>> = _topRcmd.asStateFlow()

    private var loadingMore = false

    fun refresh() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            // 轮播并行拉取（失败静默不阻塞首页）
            launch {
                com.example.pilinara.data.remote.BiliApiClient().getTopRcmd()
                    .onSuccess { resp ->
                        if (resp.code == 0) _topRcmd.value =
                            resp.data?.item.orEmpty().filter { it.goto == "av" && it.bvid.isNotEmpty() }
                    }
            }
            repo.refresh()
                .onSuccess { _state.value = HomeUiState.Success(it) }
                .onFailure { _state.value = HomeUiState.Error(it.message ?: "网络请求失败") }
        }
    }

    fun loadMore() {
        val current = (_state.value as? HomeUiState.Success)?.items ?: return
        if (loadingMore) return
        loadingMore = true
        viewModelScope.launch {
            repo.loadMore(current.size / 20 + 1)
                .onSuccess { _state.value = HomeUiState.Success(it) }
                .onFailure { /* 加载更多失败静默保留当前列表 */ }
            loadingMore = false
        }
    }

    init {
        refresh()
    }
}
