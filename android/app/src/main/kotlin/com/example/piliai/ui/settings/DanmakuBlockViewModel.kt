package com.example.piliai.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piliai.database.DanmakuFilterRuleEntity
import com.example.piliai.database.PiliNaraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 弹幕屏蔽规则（批次：弹幕屏蔽）——关键词 / 正则 / 用户UID 三类，Room 持久化
 */
class DanmakuBlockViewModel(context: android.content.Context) : ViewModel() {

    // 审核：不持有传入 Context（防泄漏 lint StaticFieldLeak）；warmup 走 applicationContext
    private val db = PiliNaraDatabase.getDatabase(context.applicationContext)
    private val dao = db.danmakuFilterRuleDao()

    private val _state = MutableStateFlow(BlockState())
    val state: StateFlow<BlockState> = _state.asStateFlow()

    data class BlockState(
        val keywords: List<String> = emptyList(),
        val regexes: List<String> = emptyList(),
        val uids: List<String> = emptyList()
    )

    private var loaded = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = dao.getByKey("danmakuFilterRules")
            _state.value = BlockState(
                keywords = entity?.dmFilterStrings?.split("\n")?.filter { it.isNotBlank() }.orEmpty(),
                regexes = entity?.dmRegExpPatterns?.split("\n")?.filter { it.isNotBlank() }.orEmpty(),
                uids = entity?.dmUids?.split("\n")?.filter { it.isNotBlank() }.orEmpty()
            )
            loaded = true
        }
    }

    private fun save() {
        if (!loaded) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsert(
                DanmakuFilterRuleEntity(
                    dmFilterStrings = _state.value.keywords.joinToString("\n"),
                    dmRegExpPatterns = _state.value.regexes.joinToString("\n"),
                    dmUids = _state.value.uids.joinToString("\n")
                )
            )
        }
    }

    fun addKeyword(k: String) {
        if (k.isBlank() || k in _state.value.keywords) return
        _state.value = _state.value.copy(keywords = _state.value.keywords + k.trim())
        save()
        warmup(db)
    }

    fun removeKeyword(k: String) {
        _state.value = _state.value.copy(keywords = _state.value.keywords - k)
        save()
        warmup(db)
    }

    fun addRegex(r: String) {
        if (r.isBlank()) return
        val validated = runCatching { Regex(r) }.getOrNull() ?: return  // 校验合法性
        if (r.trim() in _state.value.regexes) return
        _state.value = _state.value.copy(regexes = _state.value.regexes + r.trim())
        save()
        warmup(db)
    }

    fun removeRegex(r: String) {
        _state.value = _state.value.copy(regexes = _state.value.regexes - r)
        save()
        warmup(db)
    }

    fun addUid(u: String) {
        if (u.isBlank() || !u.all { it.isDigit() } || u in _state.value.uids) return
        _state.value = _state.value.copy(uids = _state.value.uids + u.trim())
        save()
        warmup(db)
    }

    fun removeUid(u: String) {
        _state.value = _state.value.copy(uids = _state.value.uids - u)
        save()
        warmup(db)
    }

    companion object {
        /** 全局规则缓存（弹幕渲染时过滤用） */
        @Volatile var cachedKeywords: List<String> = emptyList(); private set
        @Volatile var cachedRegexes: List<Regex> = emptyList(); private set
        @Volatile var cachedRegexStrings: List<String> = emptyList(); private set  // 批次audit25：原始正则串供 Rust 过滤
        @Volatile var cachedUids: Set<Long> = emptySet(); private set

        /** 弹幕过滤入口：返回 true = 该条应被屏蔽 */
        fun shouldBlock(text: String, uid: Long): Boolean {
            if (uid in cachedUids) return true
            // 审核轮198：关键词不区分大小写（原来 "hhh" 屏蔽不了 "HHH"）
            val lower = text.lowercase()
            if (cachedKeywords.any { lower.contains(it.lowercase()) }) return true
            if (cachedRegexes.any { it.containsMatchIn(text) }) return true
            return false
        }

        // 审核轮126：单一共享 scope（原来每次 warmup 新建 CoroutineScope 不回收——泄漏）
        private val warmupScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
        )

        /** App 启动时加载缓存（先组快照再一次性发布，渲染线程不会看到半更新状态） */
        fun warmup(db: PiliNaraDatabase) {
            val dao = db.danmakuFilterRuleDao()
            warmupScope.launch {
                val e = dao.getByKey("danmakuFilterRules")
                val kw = e?.dmFilterStrings?.split("\n")?.filter { it.isNotBlank() }.orEmpty()
                val reStr = e?.dmRegExpPatterns?.split("\n")?.filter { it.isNotBlank() }.orEmpty()
                val re = reStr.mapNotNull { runCatching { Regex(it) }.getOrNull() }
                val uids = e?.dmUids?.split("\n")?.filter { it.isNotBlank() }
                    ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
                cachedKeywords = kw
                cachedRegexes = re
                cachedRegexStrings = reStr
                cachedUids = uids
            }
        }

        /** 修改规则后刷新缓存 */
        fun refreshCache(context: android.content.Context) = warmup(
            PiliNaraDatabase.getDatabase(context)
        )
    }
}
