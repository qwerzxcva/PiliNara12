package com.example.pilinara.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource

/**
 * ExoPlayer helper for handling different stream types
 */
class ExoPlayerHelper(private val context: Context) {
    
    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(android.content.Context.RECEIVE_BOOT_COMPLETED)
        .build()
    
    fun initialize() {
        // Setup player listeners
        player.addListener(object : androidx.media3.exoplayer.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    ExoPlayer.STATE_BUFFERING -> {
                        // Show buffering indicator
                    }
                    ExoPlayer.STATE_READY -> {
                        // Ready to play
                    }
                    ExoPlayer.STATE_ENDED -> {
                        // Playback ended
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Handle play/pause state
            }
        })
    }
    
    fun prepare(url: String, isHls: Boolean = false, isDash: Boolean = false) {
        val mediaItem = MediaItem.fromUri(url)
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setUserAgent("PiliNara/1.0")
        )
        
        val mediaSource: MediaSource = when {
            isHls -> HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            isDash -> DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            else -> androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }
        
        player.setMediaSource(mediaSource)
        player.prepare()
    }
    
    fun play() {
        player.play()
    }
    
    fun pause() {
        player.pause()
    }
    
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }
    
    fun setVolume(volume: Float) {
        player.volume = volume
    }
    
    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackParameters(
            androidx.media3.common.PlaybackParameters(speed)
        )
    }
    
    fun release() {
        player.release()
    }
    
    val isPlaying: Boolean get() = player.isPlaying
    val currentPosition: Long get() = player.currentPosition
    val duration: Long get() = player.duration
    val volume: Float get() = player.volume
}
