package com.example.pilinara.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Live Room Screen（批次F）——真实 HLS 直播流 + 房间信息 + 进房上报
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRoomScreen(
    roomId: String,
    onBack: () -> Unit = {},
    viewModel: LiveRoomViewModel = viewModel(
        key = roomId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LiveRoomViewModel(roomId.toLongOrNull() ?: 0L) as T
        }
    )
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 独立轻量 ExoPlayer 播直播流
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(state.playUrl) {
        if (state.playUrl.isNotEmpty()) {
            player.setMediaItem(MediaItem.fromUri(state.playUrl))
            player.prepare()
        }
        onDispose {}
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifEmpty { "直播间 $roomId" }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 播放器区
            Box(
                Modifier.fillMaxWidth().height(230.dp).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (state.playUrl.isNotEmpty()) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = true
                                this.player = player
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else if (state.liveStatus == 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(56.dp), tint = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("主播未开播", color = Color.White)
                    }
                } else if (state.error != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(56.dp), tint = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(state.error ?: "", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.load() }) { Text("重试") }
                    }
                }
            }

            // 信息卡
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        state.title.ifEmpty { "直播间 ${state.roomId}" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.areaName.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(state.areaName, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LiveTv, null, Modifier.size(16.dp),
                            tint = if (state.liveStatus == 1) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when {
                                state.liveStatus == 1 -> "直播中 · ${state.userCount} 人观看"
                                state.liveStatus == 2 -> "轮播中"
                                else -> "未开播"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // 提示：弹幕 websocket 后续批次接入
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                    Text("弹幕区域（websocket 接入开发中）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
