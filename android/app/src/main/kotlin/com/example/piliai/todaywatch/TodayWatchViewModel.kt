package com.example.piliai.todaywatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.piliai.database.PiliNaraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 「今日推荐」ViewModel：异步生成 Plan，不阻塞首屏。
 *
 * 状态机：Idle → Loading → Success(plan) / Hidden
 * - 未登录或候选为空 → Hidden（不显示 Section，避免空占位）
 */
class TodayWatchViewModel(app: Application) : AndroidViewModel(app) {

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Success(val plan: TodayWatchPlan) : UiState
        data object Hidden : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    private val repository: TodayWatchRepository by lazy {
        TodayWatchRepository(PiliNaraDatabase.getDatabase(app))
    }
    private var loaded = false

    /** 进入首页时调用一次；loaded 防重复触发 */
    fun loadIfNeeded() {
        if (loaded) return
        loaded = true
        refresh()
    }

    fun refresh() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.buildPlan()
                .onSuccess { plan ->
                    _state.value = if (plan.videoQueue.isEmpty()) UiState.Hidden else UiState.Success(plan)
                }
                .onFailure {
                    _state.value = UiState.Hidden   // 失败静默隐藏，不影响首页主体
                }
        }
    }

    /** 点踩后重新生成 */
    fun dislike(bvid: String, ownerMid: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.dislike(bvid, ownerMid)
            refresh()
        }
    }
}
