package com.example.pilinara.playback
import java.util.Locale

import android.app.PictureInPictureParams
import android.os.Build
import android.content.Context
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pilinara.danmaku.DanmakuView
import com.example.pilinara.data.model.formatCount
import com.example.pilinara.utils.toHttpsUrl

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    bvid: String = "",
    cid: Long = 0L,
    epId: Long = 0L,
    local: Boolean = false,
    title: String = "",
    onBack: () -> Unit = {},
    onOpenComments: (bvid: String) -> Unit = {},
    onSearchTag: (String) -> Unit = {},
    viewModel: VideoPlayerViewModel = viewModel(
        factory = VideoPlayerViewModelFactory(LocalContext.current)
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showControls by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var isInPiP by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showPartSheet by remember { mutableStateOf(false) }
    var showDanmakuSheet by remember { mutableStateOf(false) }
    var showRelatedSheet by remember { mutableStateOf(false) }
    var showIntroSheet by remember { mutableStateOf(false) }
    var showCoinSheet by remember { mutableStateOf(false) }
    var showFavSheet by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    LaunchedEffect(videoUrl, epId, local) {
        viewModel.loadVideo(videoUrl, bvid, cid, epId, local)
    }

    // 播放中每 15 秒上报一次历史进度（需登录）
    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            kotlinx.coroutines.delay(15_000L)
            viewModel.reportProgress()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 审核43：前后台切换——App 不可见时自动暂停，回前台恢复
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                        viewModel.pause()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        // Video Surface
        AndroidView(
            factory = { ctx -> createPlayerView(ctx, viewModel.player) },
            modifier = Modifier.fillMaxSize()
        )
        
        // Danmaku Overlay（批次L14 真实渲染接线）
        var dmViewRef by remember { mutableStateOf<com.example.pilinara.danmaku.DanmakuView?>(null) }
        AndroidView(
            factory = { ctx ->
                com.example.pilinara.danmaku.DanmakuView(ctx).also { dmViewRef = it }
            },
            update = { v ->
                v.alphaFactor = state.danmakuAlpha
                v.scaleFactor = state.danmakuScale
                v.setRunning(state.isPlaying)
            },
            modifier = Modifier.fillMaxSize()
        )
        // 已发送到渲染层的弹幕 id（避免重复 add）
        val dispatchedIds = remember { mutableSetOf<String>() }
        LaunchedEffect(state.isPlaying, state.currentTime, state.danmakuOn) {
            val v = dmViewRef ?: return@LaunchedEffect
            if (!state.danmakuOn) { v.clearAll(); return@LaunchedEffect }
            // 窗口内未分发的弹幕 → add
            for (e in viewModel.getDanmakuAtTime(state.currentTime)) {
                if (dispatchedIds.add(e.id.ifEmpty { "${e.timestamp}_${e.content}" })) {
                    v.add(e.content, e.color, e.fontSize)
                }
            }
        }
        // seek/切P 时重置去重集合并清屏
        LaunchedEffect(state.currentTime) {
            if (state.currentTime < 3000L) dispatchedIds.clear()
        }

        // 字幕层（批次K）
        val subtitle by viewModel.currentSubtitle.collectAsStateWithLifecycle()
        // 批次L41：视频章节
        val viewPoints by viewModel.viewPoints.collectAsStateWithLifecycle()
        LaunchedEffect(state.isPlaying, state.currentTime) {
            viewModel.updateSubtitleAt(state.currentTime)
        }
        if (subtitle.isNotEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(bottom = 64.dp)
                ) {
                    Text(
                        subtitle,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Row {
                        // 清晰度
                        if (state.qualities.isNotEmpty()) {
                            Text(
                                state.qualities.firstOrNull { it.qn == state.currentQn }?.label
                                    ?: "清晰度",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clickable { showQualityMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        // 分P
                        if (state.partCount > 1) {
                            Text(
                                "P${state.currentPart}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clickable { showPartSheet = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
                            Icon(Icons.Default.Speed, "Speed", tint = Color.White)
                        }
                        IconButton(onClick = { showDanmakuSheet = true }) {
                            Icon(
                                if (state.danmakuOn) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                                "Danmaku", tint = Color.White
                            )
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

                // Seek bar（拖动时显示 storyboard 缩略图预览，批次L3）
                var isSeeking by remember { mutableStateOf(false) }
                var seekPreviewSec by remember { mutableLongStateOf(0L) }
                Box {
                    if (isSeeking) {
                        val frame = viewModel.shotFrameAt(seekPreviewSec)
                        val shot = viewModel.videoShot.collectAsStateWithLifecycle().value
                        if (frame != null && shot != null && shot.imgXLen > 0 && shot.imgYLen > 0) {
                            // 雪碧图整图按格位偏移裁剪显示
                            val cellW = shot.imgXSize.toFloat()
                            val cellH = shot.imgYSize.toFloat()
                            val scale = 160f / cellW
                            // 审核：无需 constraints scope，用 Box（消除 lint UnusedBoxWithConstraintsScope）
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 44.dp)
                                    .size(width = 160.dp, height = 90.dp)
                            ) {
                                coil.compose.AsyncImage(
                                    model = frame.first.toHttpsUrl(),
                                    contentDescription = "预览",
                                    modifier = Modifier
                                        .size(
                                            width = (shot.imgXLen * cellW * scale).dp,
                                            height = (shot.imgYLen * cellH * scale).dp
                                        )
                                        .graphicsLayer {
                                            translationX = -frame.second * cellW * scale * density
                                            translationY = -frame.third * cellH * scale * density
                                        },
                                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
                                )
                            }
                        }
                    }
                    // 批次L42：弹幕密度热力曲线（高能进度条），画在进度条上方
                    val heatCurve by viewModel.heatCurve.collectAsStateWithLifecycle()
                    if (heatCurve.isNotEmpty()) {
                        androidx.compose.foundation.Canvas(
                            Modifier.fillMaxWidth().height(28.dp)
                        ) {
                            val n = heatCurve.size
                            val bw = size.width / n
                            heatCurve.forEachIndexed { i, v ->
                                val h = (v.coerceIn(0f, 1f)) * size.height
                                drawRect(
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f + 0.4f * v),
                                    topLeft = androidx.compose.ui.geometry.Offset(i * bw, size.height - h),
                                    size = androidx.compose.ui.geometry.Size(bw * 0.9f, h)
                                )
                            }
                        }
                    }
                    Slider(
                        value = if (state.duration > 0) state.currentTime.toFloat() / state.duration else 0f,
                        onValueChange = { fraction ->
                            isSeeking = true
                            seekPreviewSec = ((state.duration * fraction) / 1000).toLong()
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            viewModel.seekTo(seekPreviewSec * 1000)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
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
                        count = formatCount(state.likeCount),
                        onClick = { viewModel.toggleLike() },
                        tintColor = if (state.isLiked) Color(0xFF00A1D6) else Color.White
                    )
                    EngagementButton(
                        icon = Icons.Default.MonetizationOn,
                        label = "投币",
                        count = formatCount(state.coinCountTotal),
                        onClick = { showCoinSheet = true },
                        tintColor = Color.White
                    )
                    EngagementButton(
                        icon = if (state.isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        label = "收藏",
                        count = formatCount(state.favCount),
                        onClick = {
                            viewModel.loadFavFolders()
                            showFavSheet = true
                        },
                        tintColor = if (state.isFavorited) Color(0xFFFF6B9D) else Color.White
                    )
                    EngagementButton(
                        icon = Icons.AutoMirrored.Filled.Comment,
                        label = "评论", onClick = { onOpenComments(bvid) },
                        tintColor = Color.White
                    )
                    EngagementButton(
                        icon = Icons.Default.AutoAwesome,
                        label = "相关",
                        count = if (state.related.isNotEmpty()) state.related.size.toString() else "",
                        onClick = { showRelatedSheet = true },
                        tintColor = Color.White
                    )
                    EngagementButton(
                        icon = Icons.Default.Schedule,
                        label = "稍后看", onClick = { viewModel.addToWatchLater() },
                        tintColor = Color.White
                    )
                    // 离线下载（批次I 接线：发起队列）
                    EngagementButton(
                        icon = Icons.Default.Download,
                        label = "下载",
                        onClick = { viewModel.downloadCurrent(context.applicationContext) },
                        tintColor = Color.White
                    )
                    EngagementButton(
                        icon = Icons.Default.Info,
                        label = "简介",
                        onClick = {
                            viewModel.loadAiConclusion()
                            showIntroSheet = true
                        },
                        tintColor = Color.White
                    )
                    // 分享（复制链接到剪贴板）
                    EngagementButton(
                        icon = Icons.Default.Share,
                        label = "分享",
                        onClick = {
                            val link = if (bvid.startsWith("ep"))
                                "https://www.bilibili.com/bangumi/play/$bvid"
                            else "https://www.bilibili.com/video/$bvid"
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("link", link))
                            viewModel.notifyShared(link)
                        },
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
        
        // Quality menu
        DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
            state.qualities.forEach { q ->
                DropdownMenuItem(
                    text = {
                        Text(
                            q.label,
                            color = if (state.currentQn == q.qn) MaterialTheme.colorScheme.primary else Color.Black
                        )
                    },
                    onClick = { viewModel.switchQuality(q.qn); showQualityMenu = false }
                )
            }
        }

        // 分P 选择面板
        if (showPartSheet) {
            ModalBottomSheet(onDismissRequest = { showPartSheet = false }) {
                Text(
                    "选集（共 ${state.partCount} P）",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items((1..state.partCount).toList()) { p ->
                        val partTitle = viewModel.pagesSnapshot().getOrNull(p - 1)?.part ?: "第 $p 集"
                        ListItem(
                            headlineContent = { Text("P$p · $partTitle", maxLines = 1) },
                            trailingContent = {
                                IconButton(onClick = {
                                    viewModel.downloadPart(context.applicationContext, p - 1)
                                }) {
                                    Icon(Icons.Default.Download, "下载本P",
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.playPart(p - 1); showPartSheet = false
                            },
                            colors = if (p == state.currentPart)
                                ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else ListItemDefaults.colors()
                        )
                    }
                }
            }
        }

        // 弹幕设置面板
        if (showDanmakuSheet) {
            ModalBottomSheet(onDismissRequest = { showDanmakuSheet = false }) {
                Column(Modifier.padding(16.dp)) {
                    Text("弹幕设置", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    // 发送弹幕（批次L5）
                    var dmInput by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = dmInput,
                            onValueChange = { dmInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("发个弹幕见证当下…") },
                            maxLines = 2,
                            shape = MaterialTheme.shapes.large
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (dmInput.isNotBlank()) {
                                    viewModel.sendDanmaku(dmInput)
                                    dmInput = ""
                                }
                            },
                            enabled = dmInput.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "发送弹幕")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("显示弹幕", modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.danmakuOn,
                            onCheckedChange = { viewModel.danmakuEnabled = it }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("透明度 ${(state.danmakuAlpha * 100).toInt()}%")
                    Slider(
                        value = state.danmakuAlpha,
                        onValueChange = { viewModel.setDanmakuAlpha(it) },
                        valueRange = 0.1f..1f
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("大小 ${"%.1f".format(state.danmakuScale)}x")
                    Slider(
                        value = state.danmakuScale,
                        onValueChange = { viewModel.setDanmakuScale(it) },
                        valueRange = 0.5f..2f
                    )
                    // 字幕选择（批次L9）
                    val tracks by viewModel.subtitleTracks.collectAsStateWithLifecycle()
                    val selectedSubId by viewModel.selectedSubtitleId.collectAsStateWithLifecycle()
                    if (tracks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("字幕", style = MaterialTheme.typography.labelLarge)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedSubId == -1L,
                                onClick = { viewModel.selectSubtitle(-1L) },
                                label = { Text("关闭") }
                            )
                            tracks.forEach { (id, name, _) ->
                                FilterChip(
                                    selected = selectedSubId == id,
                                    onClick = { viewModel.selectSubtitle(id) },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // 收藏夹选择面板（批次L20）
        if (showFavSheet) {
            ModalBottomSheet(onDismissRequest = { showFavSheet = false }) {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("选择收藏夹", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    viewModel.favFolders.forEach { f ->
                        ListItem(
                            headlineContent = { Text(f.second) },
                            supportingContent = { Text("${f.third} 个内容") },
                            leadingContent = {
                                Icon(Icons.Filled.Folder, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.favoriteTo(f.first)
                                showFavSheet = false
                            }
                        )
                    }
                    if (viewModel.favFolders.isEmpty()) {
                        Text("未获取到收藏夹（需登录）", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // 投币面板（批次L19：1/2 枚选择）
        if (showCoinSheet) {
            ModalBottomSheet(onDismissRequest = { showCoinSheet = false }) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("为 UP 主投币", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(1, 2).forEach { n ->
                            Button(
                                onClick = {
                                    viewModel.coinOnce(n)
                                    showCoinSheet = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("投 $n 枚")
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // 简介面板（批次L18）
        if (showIntroSheet) {
            ModalBottomSheet(onDismissRequest = { showIntroSheet = false }) {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(viewModel.videoTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildString {
                            append(viewModel.videoOwner)
                            viewModel.videoPubdate.takeIf { it > 0 }?.let {
                                append(" · ")
                                append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
                                    .format(java.util.Date(it * 1000)))
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 批次L40：实时在线人数
                    if (state.onlineCount > 0L) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "👁 ${state.onlineCount} 人正在看",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 批次L41：视频章节（view_points，点击跳转时间点）
                    if (viewPoints.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("章节", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        viewPoints.forEach { vp ->
                            Text(
                                "[${java.text.SimpleDateFormat("mm:ss", java.util.Locale.CHINA).format(java.util.Date((vp.from * 1000).toLong()))}] ${vp.content}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.seekTo((vp.from * 1000).toLong()) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    if (viewModel.videoTags.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.videoTags.take(12).forEach { tag ->
                                SuggestionChip(
                                    onClick = { onSearchTag(tag) },
                                    label = { Text(tag, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                    if (viewModel.videoDesc.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            viewModel.videoDesc,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    // AI 总结（批次L22，需登录，失败不显示）
                    if (viewModel.aiSummary.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("AI 视频总结", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(viewModel.aiSummary, style = MaterialTheme.typography.bodySmall)
                        viewModel.aiOutline.forEach { (ts, title) ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "%02d:%02d  %s".format(ts / 60, ts % 60, title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "AI 总结加载中或不可用（需登录）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // 相关视频面板
        if (showRelatedSheet) {
            ModalBottomSheet(onDismissRequest = { showRelatedSheet = false }) {
                Text(
                    "相关推荐",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(state.related, key = { it.bvid }) { r ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.playRelated(r); showRelatedSheet = false
                                }
                                .padding(12.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = r.pic.toHttpsUrl(),
                                contentDescription = r.title,
                                modifier = Modifier.width(120.dp).height(68.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    r.title, maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${r.author} · ${r.viewText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { viewModel.commitGestureSeek() },
                        onDrag = { change, drag ->
                            change.consume()
                            val horizontal = kotlin.math.abs(drag.x) > kotlin.math.abs(drag.y)
                            if (horizontal) {
                                viewModel.onGestureSeek(drag.x, size.width.toFloat())
                            } else {
                                viewModel.onVerticalDrag(
                                    change.position.x < size.width / 2,
                                    drag.y, size.height.toFloat()
                                )
                            }
                        }
                    )
                }
        )

        // 手势提示浮层（快进/音量/亮度）
        if (state.gestureSeekDeltaMs != 0L) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(color = Color.Black.copy(0.6f), shape = MaterialTheme.shapes.medium) {
                    Text(
                        (if (state.gestureSeekDeltaMs > 0) "快进 " else "快退 ") +
                            "${kotlin.math.abs(state.gestureSeekDeltaMs) / 1000}s",
                        color = Color.White, modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
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
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
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

/** 审核：PlayerView 配置集中于此并显式 OptIn Media3 UnstableApi（消除 lint UnsafeOptInUsageError） */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun createPlayerView(
    ctx: android.content.Context,
    player: androidx.media3.exoplayer.ExoPlayer?
): androidx.media3.ui.PlayerView {
    // Kazumi 特性：渲染器切换（SurfaceView 默认 / TextureView）
    //
    // 注意：Media3 1.5.1 的 PlayerView 没有公开 setSurfaceType（javap 已核实），
    // surface_type 只能在 inflate 时通过 XML 属性生效。
    // 因此这里按偏好选择对应布局 inflate。
    // DataStore 是异步的，故读 RendererPrefs 的进程内同步缓存，
    // 避免在 view 工厂里 runBlocking（冷启动卡帧）。
    val layout = if (com.example.pilinara.utils.RendererPrefs.useTextureView) {
        com.example.pilinara.R.layout.player_view_texture
    } else {
        com.example.pilinara.R.layout.player_view_surface
    }
    val view = android.view.LayoutInflater.from(ctx).inflate(layout, null, false)
        as androidx.media3.ui.PlayerView
    view.apply {
        this.player = player
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        setShowNextButton(false)
        setShowPreviousButton(false)
        controllerAutoShow = true
        controllerShowTimeoutMs = 3000
    }
    return view
}
