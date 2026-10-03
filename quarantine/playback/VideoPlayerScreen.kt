package com.example.pilinara.playback

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.pilinara.R

/**
 * Video Player Screen using Media3 ExoPlayer
 * Replaces Flutter media_kit + mpv
 */
@Composable
fun VideoPlayerScreen(bvid: String) {
    val viewModel: VideoPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Initialize ExoPlayer
    val player = rememberExoPlayer {
        setHandleAudioBecomingNoisy(true)
        setWakeMode(android.content.Context.RECEIVE_BOOT_COMPLETED)
    }
    
    LaunchedEffect(bvid) {
        // Load video
        val mediaItem = MediaItem.fromUri("https://example.com/video/$bvid.mp4")
        player.setMediaItem(mediaItem)
        player.prepare()
    }
    
    // Sync player state with ViewModel
    LaunchedEffect(player.isPlaying) {
        viewModel._state.value = viewModel.state.value.copy(
            isPlaying = player.isPlaying,
            currentTime = player.currentPosition,
            duration = player.duration
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        PlayerView(
            context = context,
            player = player
        ) {
            // TODO: Custom controls
        }
        
        if (state.isBuffering) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
}
