package com.example.pilinara.playback

import android.app.PictureInPictureParams
import android.os.Build
import android.content.Context
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.pilinara.danmaku.DanmakuView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    bvid: String = "",
    cid: Long = 0L,
    title: String = "",
    onBack: () -> Unit = {},
    onOpenComments: (bvid: String) -> Unit = {},
    viewModel: VideoPlayerViewModel = viewModel(
        factory = VideoPlayerViewModelFactory(LocalContext.current)
    )
) {
    val state by viewModel.state.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var isInPiP by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    LaunchedEffect(videoUrl) {
        viewModel.loadVideo(videoUrl, bvid, cid)
    }

    // 播放中每 15 秒上报一次历史进度（需登录）
    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            kotlinx.coroutines.delay(15_000L)
            viewModel.reportProgress()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = viewModel.player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    controllerAutoShow = true
                    controllerShowTimeoutMs = 3000
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Danmaku Overlay
        AndroidView(
            factory = { ctx -> DanmakuView(ctx) },
            modifier = Modifier.fillMaxSize()
        )
        
        // Error overlay
        state.error?.let { error ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $error", color = Color.Red)
                    Spacer(Modifier.height(8.dp))
                    IconButton(onClick = { viewModel.setError(null) }) {
                        Icon(Icons.Default.Replay, "Retry", tint = Color.White)
                    }
                }
            }
        }
        
        // Buffering indicator
        if (state.isBuffering && state.error == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            }
        }
        
        // Controls
        if (showControls && state.error == null) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Top bar
                Row(modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Row {
                        IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
                            Icon(Icons.Default.Speed, "Speed", tint = Color.White)
                        }
                        IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
                            Icon(
                                if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff 
                                else Icons.AutoMirrored.Filled.VolumeUp,
                                "Volume", tint = Color.White
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = {
                                if (!isInPiP) { context.findActivity()?.enterPiP(); isInPiP = true }
                            }) {
                                Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // Seek bar
                Slider(
                    value = if (state.duration > 0) state.currentTime.toFloat() / state.duration else 0f,
                    onValueChange = { },
                    onValueChangeFinished = { 
                        viewModel.seekTo(0L)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatTime(state.currentTime)} / ${formatTime(state.duration)}", 
                        color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Text("${state.playbackSpeed}x", color = Color.White, 
                        style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(Modifier.weight(1f))
                
                // Play/Pause center button
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .background(Color.Black.copy(0.5f), CircleShape)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause", tint = Color.White, modifier = Modifier.height(40.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Engagement buttons
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    EngagementButton(
                        icon = Icons.Filled.ThumbUp,
                        label = "点赞",
                        count = if (state.isLiked) "1" else "",
                        onClick = { viewModel.toggleLike() },
                        tintColor = if (state.isLiked) Color(0xFF00A1D6) else Color.White
                    )
                    EngagementButton(
                        icon = Icons.Default.MonetizationOn,
                        label = "投币",
                        count = if (state.coinCount > 0) state.coinCount.toString() else "",
                        onClick = { viewModel.coinOnce() },
                        tintColor = Color.White
                    )
                    EngagementButton(
                        icon = if (state.isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        label = "收藏",
                        onClick = { viewModel.toggleFavorite(mediaId = 0L) },  // TODO: 默认收藏夹 mediaId
                        tintColor = if (state.isFavorited) Color(0xFFFF6B9D) else Color.White
                    )
                    EngagementButton(
                        icon = Icons.Default.Comment,
                        label = "评论", onClick = { onOpenComments(bvid) },
                        tintColor = Color.White
                    )
                }
            }
        }
        
        // Speed menu
        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            if (speed == 1.0f) "Normal" else "${speed}x",
                            color = if (state.playbackSpeed == speed) MaterialTheme.colorScheme.primary else Color.Black
                        ) 
                    },
                    onClick = { viewModel.setPlaybackSpeed(speed); showSpeedMenu = false }
                )
            }
        }
        
        // Volume slider overlay
        if (showVolumeSlider) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.7f)),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff 
                        else Icons.AutoMirrored.Filled.VolumeUp,
                        "Volume", tint = Color.White, modifier = Modifier.height(48.dp)
                    )
                    Slider(
                        value = state.volume,
                        onValueChange = { viewModel.setVolume(it) },
                        modifier = Modifier.width(200.dp)
                    )
                    Text("${(state.volume * 100).toInt()}%", color = Color.White)
                }
            }
        }
        
        // Gesture handler
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { tapOffset ->
                            val tapX = tapOffset.x
                            if (tapX < size.width / 2) {
                                viewModel.seekRelative(-10_000L)
                            } else {
                                viewModel.seekRelative(10_000L)
                            }
                        }
                    )
                }
        )
    }
}

@Composable
private fun EngagementButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: String = "",
    onClick: () -> Unit,
    tintColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .background(Color.Black.copy(0.5f), CircleShape)
                .padding(8.dp)
        ) {
            Icon(icon, label, tint = tintColor, modifier = Modifier.height(24.dp))
        }
        if (count.isNotEmpty()) {
            Text(count, color = tintColor, style = MaterialTheme.typography.labelSmall)
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), 
            style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

class VideoPlayerViewModelFactory(
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoPlayerViewModel::class.java)) {
            return VideoPlayerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// Extension for PiP
fun android.app.Activity.enterPiP() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }
}
