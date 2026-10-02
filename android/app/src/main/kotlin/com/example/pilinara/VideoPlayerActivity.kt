package com.example.pilinara

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * Native Android video player activity using Media3 ExoPlayer.
 * This replaces the Flutter-based video player for Android ARMv8.
 */
class VideoPlayerActivity : FlutterActivity() {
    
    companion object {
        private const val CHANNEL = "com.pilinara/video_player"
        
        fun newInstance(context: Context, videoUrl: String, headers: Map<String, String> = emptyMap()): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra("video_url", videoUrl)
                putExtra("headers", headers.toTypedArray())
                putExtra("autoplay", true)
            }
        }
    }
    
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var methodChannel: MethodChannel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        playerView = PlayerView(this).apply {
            resizeMode = com.google.android.material.util.FadeOnClickListener(this)
            useController = true
        }
        
        setContentView(playerView)
        
        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(com.google.android.exoplayer2.C.WAKE_MODE_LOCAL)
            .build()
        
        playerView.player = player
        
        // Setup method channel for Flutter communication
        methodChannel = MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger ?: return, CHANNEL)
        
        // Prepare media
        val videoUrl = intent.getStringExtra("video_url") ?: return
        val headers = parseHeaders(intent)
        
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(videoUrl))
            .setHttpUserAgent("Mozilla/5.0 BiliDroid/2.0.1")
            .setHttpHeaders(headers)
            .build()
        
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = intent.getBooleanExtra("autoplay", true)
        
        // Setup player listeners
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        methodChannel.invokeMethod("onComplete", null)
                    }
                    Player.STATE_BUFFERING -> {
                        methodChannel.invokeMethod("onBuffering", mapOf("buffering" to true))
                    }
                    Player.STATE_READY -> {
                        methodChannel.invokeMethod("onReady", mapOf(
                            "duration" to player.duration,
                            "width" to (player.videoSize.width ?: 0),
                            "height" to (player.videoSize.height ?: 0)
                        ))
                    }
                }
            }
            
            override fun onIsPlayingChanged(playing: Boolean) {
                methodChannel.invokeMethod("onPlayStateChanged", mapOf("playing" to playing))
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                methodChannel.invokeMethod("onPositionChanged", mapOf(
                    "position" to player.currentPosition
                ))
            }
        })
        
        // Setup method call handler
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "play" -> {
                    player.play()
                    result.success(null)
                }
                "pause" -> {
                    player.pause()
                    result.success(null)
                }
                "seek" -> {
                    val position = call.argument<Long>("position") ?: 0L
                    player.seekTo(position)
                    result.success(null)
                }
                "setVolume" -> {
                    val volume = call.argument<Double>("volume") ?: 1.0
                    player.volume = volume.toFloat()
                    result.success(null)
                }
                "setSpeed" -> {
                    val speed = call.argument<Float>("speed") ?: 1.0f
                    player.setPlaybackSpeed(speed)
                    result.success(null)
                }
                "release" -> {
                    player.release()
                    finish()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!player.isPlaying) {
            player.prepare()
            player.playWhenReady = true
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (player.isPlaying) {
            player.pause()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
    
    private fun parseHeaders(intent: Intent): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val headerArray = intent.getStringArrayExtra("headers") ?: return headers
        for (i in headerArray.indices step 2) {
            if (i + 1 < headerArray.size) {
                headers[headerArray[i]] = headerArray[i + 1]
            }
        }
        return headers
    }
}
