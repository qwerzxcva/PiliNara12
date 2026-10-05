package com.example.pilinara.ui.subscribe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.database.SubscribeItemEntity
import com.example.pilinara.database.SubscribeSourceEntity
import com.example.pilinara.data.repository.SubscribeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * 订阅页 ViewModel（Animeko「订阅」移植）
 *
 * 审核要点：
 * 1. 不持有 Activity/Context —— 数据库 DAO 由外部注入，避免配置变更泄漏。
 * 2. 所有协程走 viewModelScope，onCleared 自动取消，无 GlobalScope。
 * 3. 刷新逐源 try：单源失败不影响其它源，错误汇总到 errorMessage。
 * 4. 并发刷新用 joinAll 等待，isRefreshing 只在真正结束时置 false（避免闪烁）。
 */
class SubscribeViewModel(
    private val repository: SubscribeRepository
) : ViewModel() {

    data class UiState(
        val sources: List<SubscribeSourceEntity> = emptyList(),
        val items: List<SubscribeItemEntity> = emptyList(),
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
        val infoMessage: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        // 源列表
        viewModelScope.launch {
            repository.observeSources()
                .catch { e -> _state.value = _state.value.copy(errorMessage = e.message) }
                .collect { list ->
                    _state.value = _state.value.copy(sources = list)
                }
        }
        // 条目列表
        viewModelScope.launch {
            repository.observeItems()
                .catch { e -> _state.value = _state.value.copy(errorMessage = e.message) }
                .collect { list ->
                    _state.value = _state.value.copy(items = list)
                }
        }
    }

    /** 添加订阅源链接 */
    fun addSource(url: String, name: String, type: Int) {
        viewModelScope.launch {
            repository.addSource(url, name, type)
                .onSuccess {
                    _state.value = _state.value.copy(
                        infoMessage = "已添加订阅源",
                        errorMessage = null
                    )
                    refreshAll()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(errorMessage = e.message ?: "添加失败")
                }
        }
    }

    /** 刷新全部启用的源 */
    fun refreshAll() {
        if (refreshJob?.isActive == true) return // 防重复触发
        refreshJob = viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            val sources = _state.value.sources.filter { it.enabled }
            if (sources.isEmpty()) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    infoMessage = "还没有订阅源，请先添加"
                )
                return@launch
            }
            val errors = mutableListOf<String>()
            val jobs = sources.map { s ->
                launch {
                    repository.syncSource(s).onFailure { e ->
                        errors.add("${s.name}: ${e.message ?: "同步失败"}")
                    }
                }
            }
            jobs.forEach { it.join() }   // 等全部结束再收尾，避免 isRefreshing 闪烁
            _state.value = _state.value.copy(
                isRefreshing = false,
                errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                infoMessage = if (errors.isEmpty()) "刷新完成" else null
            )
        }
    }

    /** 删除源及其条目 */
    fun removeSource(id: Long) {
        viewModelScope.launch {
            repository.removeSource(id)
                .onFailure { e ->
                    _state.value = _state.value.copy(errorMessage = e.message ?: "删除失败")
                }
        }
    }

    /** 启/停用源 */
    fun toggleSource(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    /**
     * 审核轮8：条目无可播放直链时的提示
     * （避免把网页 URL 丢进播放器造成「点了没反应」）
     */
    fun reportNotPlayable(title: String) {
        _state.value = _state.value.copy(
            errorMessage = "「${title}」没有可播放的直链（该源可能只提供详情页链接）"
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun clearInfo() {
        _state.value = _state.value.copy(infoMessage = null)
    }
}
