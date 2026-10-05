package com.example.pilinara.ui.subscribe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.database.SubscribeItemEntity
import com.example.pilinara.database.SubscribeSourceEntity
import com.example.pilinara.data.repository.SubscribeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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

    companion object {
        /** 审核轮15：同时拉取的订阅源上限（防风控限流 + 内存峰值） */
        private const val MAX_CONCURRENT_SYNC = 4
    }

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
            // 审核轮25：用 try/finally 保证 isRefreshing 一定会被复位。
            // 否则协程被取消（ViewModel 清除 / 页面退出）时不会走到末尾的
            // copy(isRefreshing=false)，下拉刷新圈会永远转下去。
            try {
                // 审核轮15：限制并发（最多 4 个源同时拉）。
                // 源很多时全并发会瞬间打出大量请求：既容易触发对端风控/限流，
                // 也会同时持有多个响应体造成内存峰值。这里用 Semaphore 限流，
                // 仍保持并发（不至于串行太慢）。
                val semaphore = kotlinx.coroutines.sync.Semaphore(MAX_CONCURRENT_SYNC)
                val jobs = sources.map { s ->
                    launch {
                        semaphore.withPermit {
                            repository.syncSource(s).onFailure { e ->
                                synchronized(errors) {
                                    errors.add("${s.name}: ${e.message ?: "同步失败"}")
                                }
                            }
                        }
                    }
                }
                jobs.forEach { it.join() }   // 等全部结束再收尾，避免 isRefreshing 闪烁
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                    infoMessage = if (errors.isEmpty()) "刷新完成" else null
                )
            } finally {
                // 无论成功 / 失败 / 取消，都复位刷新态
                if (_state.value.isRefreshing) {
                    _state.value = _state.value.copy(isRefreshing = false)
                }
            }
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

    /**
     * Bangumi 登录（Animeko 移植）
     *
     * 真实调用 api.bgm.tv/v0/me 校验令牌；失败不写入凭据。
     */
    fun loginBangumi(token: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.example.pilinara.data.remote.BangumiSession.loginWithToken(
                    com.example.pilinara.AppContext.get(),
                    token
                )
            }.onSuccess { name ->
                _state.value = _state.value.copy(
                    infoMessage = "Bangumi 登录成功：$name",
                    errorMessage = null
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    errorMessage = e.message ?: "Bangumi 登录失败"
                )
            }
        }
    }

    /** Bangumi 退出登录（清除本地 token） */
    fun logoutBangumi() {
        com.example.pilinara.data.remote.BangumiSession.logout(
            com.example.pilinara.AppContext.get()
        )
        _state.value = _state.value.copy(infoMessage = "已退出 Bangumi 登录")
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun clearInfo() {
        _state.value = _state.value.copy(infoMessage = null)
    }
}
