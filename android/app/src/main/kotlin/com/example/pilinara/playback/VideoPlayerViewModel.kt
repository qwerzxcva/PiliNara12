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
import com.example.pilinara.data.remote.BiliApiClient
import com.example.pilinara.data.repository.VideoRepository

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
                // 无 URI：走真实解析链路 —— 详情拿 cid → playurl → Rust 选流 → DASH
                if (uri.isEmpty() && bvid.isNotEmpty()) {
                    _state.value = _state.value.copy(isBuffering = true, error = null)
                    resolveAndPlay(bvid, cid)
                } else {
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
                }
            } catch (e: Exception) {
                setError("Failed to load video: ${e.message}")
            }
        }
    }

    /**
     * 真实播放链路：
     * 1) cid 缺失时拉 /x/web-interface/view 取 cid
     * 2) /x/player/wbi/playurl 拿 DASH（Rust 侧 selectStreams 选最优流）
     * 3) video/audio 双流喂给 ExoPlayer（MediaItem sideloaded manifests 不适合分开的 DASH，
     *    这里用 DataSource 级拼接不可行，因此取 ExoPlayer 原生 DASH 需 manifest；
     *    折中方案：优先用 Rust 选出的 video baseUrl 直接播（无声），并单独 track 音频 ——
     *    Media3 支持多 MediaItem 拼接播放（ConcatenatingMediaSource2）
     */
    private suspend fun resolveAndPlay(bvid: String, cidIn: Long) {
        val repo = VideoRepository(BiliApiClient())
        // 1) 补 cid
        val effectiveCid = cidIn.takeIf { it > 0L }
            ?: repo.getVideoInfo(bvid).getOrNull()?.data?.cid
            ?: run {
                setError("无法获取视频 cid（详情接口失败）")
                return
            }

        // 2) playurl + Rust 流选择
        val (_, videoUrl, audioUrl) = repo.getPlayUrl(bvid, effectiveCid, qn = 80)
            .getOrElse {
                setError("playurl 获取失败: ${it.message}")
                return
            }
        if (videoUrl.isNullOrEmpty()) {
            setError("未解析到视频流地址（可能需要登录后观看）")
            return
        }

        // 3) 拼接播放：Media3 的 MergingMediaSource 合并视频轨+音频轨
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) PiliNara/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.bilibili.com"))

        val videoItem = MediaItem.Builder().setUri(Uri.parse(videoUrl)).build()
        val videoSource: androidx.media3.exoplayer.source.MediaSource =
            androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(videoItem)

        val merged = if (!audioUrl.isNullOrEmpty()) {
            val audioItem = MediaItem.Builder().setUri(Uri.parse(audioUrl)).build()
            val audioSource: androidx.media3.exoplayer.source.MediaSource =
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(audioItem)
            androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }

        _player?.setMediaSource(merged)
        _player?.prepare()
        _player?.playWhenReady = true
        _state.value = _state.value.copy(
            isPlaying = true, isBuffering = true,
            error = null, currentTime = 0L, duration = 0L
        )
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
            _player?.volume = if (newMuted) 0f else 1f
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
    
    override fun onIsLoadingChanged(isLoading: Boolean) {
        _state.value = _state.value.copy(isBuffering = isLoading)
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
    
    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        setError(error.message ?: "Playback error")
    }
    
    override fun onPositionDiscontinuity(
        newPosition: androidx.media3.common.Player.PositionInfo,
        oldPosition: androidx.media3.common.Player.PositionInfo,
        reason: Int
    ) {
        _state.value = _state.value.copy(currentTime = newPosition.positionMs)
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
