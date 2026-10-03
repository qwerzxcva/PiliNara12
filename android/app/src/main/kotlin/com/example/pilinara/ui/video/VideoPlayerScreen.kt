package com.example.pilinara.ui.video

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.pilinara.R
import com.example.pilinara.playback.VideoPlayerViewModel
import com.example.pilinara.ui.danmaku.DanmakuOverlay

/**
 * Video Player Screen
 * Replaces Flutter video player page with media_kit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(bvid: String, cid: Long) {
    val viewModel: VideoPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Player state
    var isPlaying by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    
    // ExoPlayer
    val player = remember {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
    
    // Danmaku overlay
    val danmakuOverlay = remember { DanmakuOverlay(context) }
    
    LaunchedEffect(bvid, cid) {
        // Load video
        val mediaItem = MediaItem.fromUri("https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=$cid")
        player.setMediaItem(mediaItem)
        player.prepare()
        
        player.addListener(object : androidx.media3.exoplayer.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                viewModel._state.value = viewModel.state.value.copy(isPlaying = playing)
            }
            
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    ExoPlayer.STATE_BUFFERING -> {
                        viewModel._state.value = viewModel.state.value.copy(isBuffering = true)
                    }
                    ExoPlayer.STATE_READY -> {
                        viewModel._state.value = viewModel.state.value.copy(isBuffering = false)
                    }
                }
            }
        })
    }
    
    // Fullscreen handling
    if (isFullscreen) {
        LaunchedEffect(Unit) {
            // Set fullscreen
            android.view.WindowInsetsControllerCompat(
                (context as android.app.Activity).window,
                (context as android.app.Activity).window.decorView
            ).hide(android.view.WindowInsets.Type.systemBars())
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Danmaku overlay
        AndroidView(
            factory = { ctx -> danmakuOverlay },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
        )
        
        // Controls overlay
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                    Text("视频标题", style = MaterialTheme.typography.titleMedium)
                    Row {
                        IconButton(onClick = { /* danmaku settings */ }) {
                            Icon(Icons.Default.Movie, contentDescription = "弹幕")
                        }
                        IconButton(onClick = { /* share */ }) {
                            Icon(Icons.Default.Share, contentDescription = "分享")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom controls
                Column {
                    // Progress bar
                    Slider(
                        value = state.currentTime.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..player.duration.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(onClick = { 
                            if (isPlaying) player.pause() else player.play()
                        }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放"
                            )
                        }
                        IconButton(onClick = { /* dislike */ }) {
                            Icon(Icons.Default.ThumbDown, contentDescription = "不喜欢")
                        }
                        IconButton(onClick = { /* coin */ }) {
                            Icon(Icons.Default.AttachmentMoney, contentDescription = "投币")
                        }
                        IconButton(onClick = { /* favorite */ }) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "收藏")
                        }
                        IconButton(onClick = { /* comment */ }) {
                            Icon(Icons.Default.Comment, contentDescription = "评论")
                        }
                        IconButton(onClick = { /* danmaku toggle */ }) {
                            Icon(Icons.Default.Movie, contentDescription = "弹幕")
                        }
                    }
                }
            }
        }
        
        // Buffering indicator
        if (state.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
