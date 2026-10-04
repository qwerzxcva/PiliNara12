package com.example.pilinara.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val wsState by viewModel.wsState.collectAsState()
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
                        val ws = wsState
                        if (ws != null) {
                            Spacer(Modifier.weight(1f))
                            val (label, tint) = when (ws) {
                                is com.example.pilinara.data.remote.LiveDanmakuWsClient.State.Authenticated ->
                                    "弹幕已连接" to MaterialTheme.colorScheme.primary
                                is com.example.pilinara.data.remote.LiveDanmakuWsClient.State.Connecting ->
                                    "弹幕连接中…" to MaterialTheme.colorScheme.onSurfaceVariant
                                is com.example.pilinara.data.remote.LiveDanmakuWsClient.State.Failed ->
                                    "弹幕断开" to MaterialTheme.colorScheme.error
                                else -> "弹幕空闲" to MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
                        }
                    }
                }
            }

            // 弹幕聊天区（批次J）+ SC 醒目留言（批次L4）
            var input by remember { mutableStateOf("") }
            val chat by viewModel.chatMessages.collectAsState()
            val superChats by viewModel.superChats.collectAsState()
            val gifts by viewModel.gifts.collectAsState()
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(chat.size, superChats.size) {
                val total = chat.size + superChats.size
                if (total > 0) listState.animateScrollToItem(total - 1)
            }
            // 顶部最近礼物飘条
            if (gifts.isNotEmpty()) {
                val g = gifts.last()
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        "🎁 ${g.name} 投喂 ${g.giftName} ×${g.num}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // SC 醒目留言卡片（置顶展示，含价格/背景色）
                    items(superChats) { sc ->
                        Surface(
                            color = runCatching {
                                Color(android.graphics.Color.parseColor(sc.backgroundColor))
                            }.getOrDefault(Color(0xFFC0000F)),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "¥${sc.price}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        sc.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(sc.message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    items(chat) { msg ->
                        Row(verticalAlignment = Alignment.Top) {
                            if (msg.medalName != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        "${msg.medalName} ${msg.medalLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                "${msg.name}: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                msg.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(msg.color)
                            )
                        }
                    }
                    if (chat.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                                Text(
                                    when (wsState) {
                                        is com.example.pilinara.data.remote.LiveDanmakuWsClient.State.Authenticated -> "等待弹幕…"
                                        is com.example.pilinara.data.remote.LiveDanmakuWsClient.State.Connecting -> "正在连接弹幕服务器…"
                                        else -> "暂无弹幕"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 发送栏
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("发一条弹幕…") },
                    maxLines = 2,
                    shape = MaterialTheme.shapes.large
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendDanmaku(input); input = ""
                        }
                    },
                    enabled = input.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "发送")
                }
            }
        }
    }
}
