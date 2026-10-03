package com.example.pilinara.playback

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoPlayerViewModel(private val context: Context) : ViewModel(), Player.Listener {
    
    private var _player: ExoPlayer? = null
    val player: ExoPlayer? get() = _player
    
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    private val _danmakuQueue = mutableListOf<DanmakuEvent>()
    
    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentTime: Long = 0L,
        val duration: Long = 0L,
        val volume: Float = 1.0f,
        val playbackSpeed: Float = 1.0f,
        val isMuted: Boolean = false,
        val isBuffering: Boolean = false,
        val error: String? = null,
        val position: Int = 0
    )
    
    data class DanmakuEvent(
        val id: String,
        val timestamp: Long,
        val content: String,
        val color: Int,
        val fontSize: Int
    )
    
    init {
        _player = ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(android.os.PowerManager.PARTIAL_WAKE_LOCK)
            .build()
        _player?.addListener(this)
    }
    
    fun loadVideo(uri: String, bvid: String = "", cid: Long = 0L) {
        viewModelScope.launch {
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(uri))
                    .setMediaId("$bvid:$cid")
                    .build()
                _player?.setMediaItem(mediaItem)
                _player?.prepare()
                _player?.playWhenReady = false
                _state.value = _state.value.copy(
                    isPlaying = false, isBuffering = true,
                    error = null, currentTime = 0L, duration = 0L
                )
            } catch (e: Exception) {
                setError("Failed to load video: ${e.message}")
            }
        }
    }
    
    fun play() {
        viewModelScope.launch {
            _player?.play()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }
    
    fun pause() {
        viewModelScope.launch {
            _player?.pause()
            _state.value = _state.value.copy(isPlaying = false)
        }
    }
    
    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            _player?.seekTo(positionMs)
            _state.value = _state.value.copy(currentTime = positionMs)
        }
    }
    
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        viewModelScope.launch {
            _player?.volume = clamped
            _state.value = _state.value.copy(
                volume = clamped, isMuted = clamped == 0f
            )
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        val normalizedSpeed = when {
            speed <= 0.5f -> 0.5f
            speed <= 1.0f -> 1.0f
            speed <= 2.0f -> speed
            else -> 2.0f
        }
        viewModelScope.launch {
            _player?.playbackParameters = androidx.media3.common.PlaybackParameters(
                normalizedSpeed, _player?.playbackParameters?.pitch ?: 1.0f
            )
            _state.value = _state.value.copy(playbackSpeed = normalizedSpeed)
        }
    }
    
    fun toggleMute() {
        viewModelScope.launch {
            val newMuted = !_state.value.isMuted
            _player?.isMuted = newMuted
            _state.value = if (newMuted) {
                _state.value.copy(isMuted = true, volume = 0f)
            } else {
                _state.value.copy(isMuted = false, volume = 1f)
            }
        }
    }
    
    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }
    
    fun seekRelative(offsetMs: Long) {
        val current = _state.value.currentTime
        val duration = _state.value.duration
        seekTo((current + offsetMs).coerceIn(0L, duration))
    }
    
    fun setError(error: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = error)
        }
    }
    
    fun addDanmakuEvents(events: List<DanmakuEvent>) {
        _danmakuQueue.clear()
        _danmakuQueue.addAll(events)
    }
    
    fun getDanmakuAtTime(currentTimeMs: Long): List<DanmakuEvent> {
        return _danmakuQueue.filter { event ->
            event.timestamp <= currentTimeMs && 
            event.timestamp > currentTimeMs - 3000L
        }
    }
    
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying)
    }
    
    override fun onIsBufferingChanged(isBuffering: Boolean) {
        _state.value = _state.value.copy(isBuffering = isBuffering)
    }
    
    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_BUFFERING -> _state.value = _state.value.copy(isBuffering = true)
            Player.STATE_READY -> _state.value = _state.value.copy(
                isBuffering = false, duration = _player?.duration ?: 0L
            )
            Player.STATE_ENDED -> _state.value = _state.value.copy(isPlaying = false)
        }
    }
    
    override fun onPlayerError(error: androidx.media3.common.MediaError) {
        setError(error.errorDetailsString ?: "Playback error")
    }
    
    override fun onPositionDiscontinuity(oldPosition: Long, newPosition: Long, reason: Int) {
        _state.value = _state.value.copy(currentTime = newPosition)
    }
    
    override fun onCleared() {
        super.onCleared()
        _player?.removeListener(this)
        _player?.release()
        _player = null
    }
    
    fun getCurrentTime(): Long = _state.value.currentTime
    fun getDuration(): Long = _state.value.duration
    fun getPositionPercent(): Int {
        val duration = _state.value.duration
        return if (duration == 0L) 0 else ((_state.value.currentTime * 100) / duration).toInt()
    }
}
