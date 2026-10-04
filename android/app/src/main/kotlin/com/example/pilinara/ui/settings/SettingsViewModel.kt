package com.example.pilinara.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.utils.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置 ViewModel（批次：设置页扩展）——全部真实持久化到 DataStore
 */
class SettingsViewModel(context: Context) : ViewModel() {

    private val storage = StorageManager(context)

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    data class SettingsState(
        // 播放
        val autoPlay: Boolean = false,
        val videoQuality: String = "auto",   // auto/1080p/720p/480p
        val playbackSpeed: Float = 1.0f,
        // 弹幕
        val danmakuEnabled: Boolean = true,
        val danmakuOpacity: Float = 1.0f,
        val danmakuFontSize: Int = 25,
        val danmakuSpeed: Float = 1.0f,
        // 主题
        val themeMode: String = "system"     // system/light/dark
    )

    init {
        viewModelScope.launch {
            storage.autoPlayFlow.collect { _state.value = _state.value.copy(autoPlay = it) }
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
    }

    fun setAutoPlay(v: Boolean) = viewModelScope.launch { storage.setAutoPlay(v) }
    fun setVideoQuality(q: String) = viewModelScope.launch { storage.setVideoQuality(q) }
    fun setDanmakuEnabled(v: Boolean) = viewModelScope.launch { storage.setDanmakuEnabled(v) }
    fun setDanmakuOpacity(v: Float) = viewModelScope.launch { storage.setDanmakuOpacity(v) }
    fun setDanmakuFontSize(v: Int) = viewModelScope.launch { storage.setDanmakuFontSize(v) }
    fun setDanmakuSpeed(v: Float) = viewModelScope.launch { storage.setDanmakuSpeed(v) }
    fun setThemeMode(m: String) = viewModelScope.launch { storage.setThemeMode(m) }
}
