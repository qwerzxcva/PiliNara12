package com.example.piliai.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piliai.utils.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置 ViewModel（批次：设置页扩展）——全部真实持久化到 DataStore
 */
class SettingsViewModel(context: Context) : ViewModel() {

    // 审核27：同上，持有 applicationContext 防 Activity 泄漏
    private val storage = StorageManager(context.applicationContext)

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    data class SettingsState(
        // 播放
        val autoPlay: Boolean = false,
        val lowLatencyAudio: Boolean = false,  // Kazumi：低延迟音频
        val renderer: Int = 0,                 // Kazumi：渲染器 0=SurfaceView 1=TextureView
        val videoQuality: String = "auto",   // auto/1080p/720p/480p
        val playbackSpeed: Float = 1.0f,
        val bufferDurationMs: Int = 16_000,  // VOD 缓冲时长（LoadControl，移植自 piliplus）
        // 弹幕
        val danmakuEnabled: Boolean = true,
        val danmakuOpacity: Float = 1.0f,
        val danmakuFontSize: Int = 25,
        val danmakuSpeed: Float = 1.0f,
        // 主题
        val themeMode: String = "system",     // system/light/dark
        val accentColor: String = "",         // 十六进制，空=默认绿
        val amoled: Boolean = false           // AMOLED 纯黑（仅暗色生效）
    )

    init {
        viewModelScope.launch {
            storage.autoPlayFlow.collect { _state.value = _state.value.copy(autoPlay = it) }
        }
        viewModelScope.launch {
            storage.lowLatencyAudioFlow.collect { _state.value = _state.value.copy(lowLatencyAudio = it) }
        }
        viewModelScope.launch {
            storage.rendererFlow.collect { _state.value = _state.value.copy(renderer = it) }
        }
        viewModelScope.launch {
            storage.videoQualityFlow.collect { _state.value = _state.value.copy(videoQuality = it) }
        }
        viewModelScope.launch {
            storage.danmakuEnabledFlow.collect { _state.value = _state.value.copy(danmakuEnabled = it) }
        }
        viewModelScope.launch {
            storage.danmakuOpacityFlow.collect { _state.value = _state.value.copy(danmakuOpacity = it) }
        }
        viewModelScope.launch {
            storage.danmakuFontSizeFlow.collect { _state.value = _state.value.copy(danmakuFontSize = it) }
        }
        viewModelScope.launch {
            storage.danmakuSpeedFlow.collect { _state.value = _state.value.copy(danmakuSpeed = it) }
        }
        viewModelScope.launch {
            storage.themeModeFlow.collect { _state.value = _state.value.copy(themeMode = it) }
        }
        viewModelScope.launch {
            storage.accentColorFlow.collect { _state.value = _state.value.copy(accentColor = it) }
        }
        viewModelScope.launch {
            storage.amoledFlow.collect { _state.value = _state.value.copy(amoled = it) }
        }
        viewModelScope.launch {
            storage.bufferDurationFlow.collect { _state.value = _state.value.copy(bufferDurationMs = it) }
        }
    }

    fun setAutoPlay(v: Boolean) = viewModelScope.launch { storage.setAutoPlay(v) }
    fun setLowLatencyAudio(v: Boolean) = viewModelScope.launch { storage.setLowLatencyAudio(v) }
    fun setRenderer(v: Int) = viewModelScope.launch {
        com.example.piliai.utils.RendererPrefs.update(v)
        storage.setRenderer(v)
    }
    fun setBufferDuration(v: Int) = viewModelScope.launch {
        com.example.piliai.utils.RendererPrefs.updateBufferDuration(v)
        storage.setBufferDuration(v)
    }
    fun setVideoQuality(q: String) = viewModelScope.launch { storage.setVideoQuality(q) }
    fun setDanmakuEnabled(v: Boolean) = viewModelScope.launch { storage.setDanmakuEnabled(v) }
    fun setDanmakuOpacity(v: Float) = viewModelScope.launch { storage.setDanmakuOpacity(v) }
    fun setDanmakuFontSize(v: Int) = viewModelScope.launch { storage.setDanmakuFontSize(v) }
    fun setDanmakuSpeed(v: Float) = viewModelScope.launch { storage.setDanmakuSpeed(v) }
    fun setThemeMode(m: String) = viewModelScope.launch { storage.setThemeMode(m) }
    fun setAccentColor(hex: String) = viewModelScope.launch { storage.setAccentColor(hex) }
    fun setAmoled(v: Boolean) = viewModelScope.launch { storage.setAmoled(v) }
}
