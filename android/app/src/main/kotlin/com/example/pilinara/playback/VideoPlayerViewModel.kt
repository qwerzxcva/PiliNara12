package com.example.pilinara.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Video Player ViewModel
 * Replaces Flutter media_kit controller
 */
class VideoPlayerViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentTime: Long = 0L,
        val duration: Long = 0L,
        val volume: Float = 1.0f,
        val playbackSpeed: Float = 1.0f,
        val isMuted: Boolean = false,
        val isBuffering: Boolean = false,
        val error: String? = null
    )
    
    fun play() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPlaying = true)
            // TODO: Start Media3 ExoPlayer
        }
    }
    
    fun pause() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPlaying = false)
            // TODO: Pause Media3 ExoPlayer
        }
    }
    
    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(currentTime = positionMs)
            // TODO: Seek in Media3 ExoPlayer
        }
    }
    
    fun setVolume(volume: Float) {
        viewModelScope.launch {
            _state.value = _state.value.copy(volume = volume.coerceIn(0f, 1f))
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            _state.value = _state.value.copy(playbackSpeed = speed)
            // TODO: Update Media3 ExoPlayer speed
        }
    }
    
    fun toggleMute() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMuted = !_state.value.isMuted)
        }
    }
    
    fun setError(error: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = error)
        }
    }
}
