package com.example.pilinara.ui.settings
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.heightIn
import org.json.JSONObject as JsonObj
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pilinara.ui.theme.ACCENT_OPTIONS
import com.example.pilinara.ui.theme.accentFromHex

/**
 * 设置页（真实持久化版）——播放/弹幕/主题，DataStore 存储
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDanmakuBlockClick: () -> Unit = {}
) {
    // 审核轮106：用全局 Application 而非 LocalContext（Activity 旋转重建时 factory 不会捕获旧 Activity）
    val viewModel: SettingsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(com.example.pilinara.AppContext.get()) as T
    })
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 弹窗选择器状态
    var showQualityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRendererDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ===== 播放设置 =====
            item { SectionTitle("播放设置") }

            item {
                SettingSwitch(
                    title = "自动播放",
                    subtitle = "进入页面后自动播放视频",
                    icon = Icons.Default.PlayArrow,
                    checked = state.autoPlay,
                    onChange = { viewModel.setAutoPlay(it) }
                )
            }
            // ===== Kazumi 特性：低延迟音频 + 渲染器 =====
            item {
                SettingSwitch(
                    title = "低延迟音频",
                    subtitle = "降低音画延迟（弱网下可能卡顿）",
                    icon = Icons.Default.Speed,
                    checked = state.lowLatencyAudio,
                    onChange = { viewModel.setLowLatencyAudio(it) }
                )
            }
            item {
                SettingRow(
                    title = "视频渲染器",
                    subtitle = if (state.renderer == 1) "TextureView（可合成动画）" else "SurfaceView（性能最佳）",
                    icon = Icons.AutoMirrored.Filled.ViewQuilt,
                    onClick = { showRendererDialog = true }
                )
            }
            item {
                SettingRow(
                    title = "默认清晰度",
                    subtitle = qualityLabel(state.videoQuality),
                    icon = Icons.Default.HighQuality,
                    onClick = { showQualityDialog = true }
                )
            }

            // ===== 弹幕设置 =====
            item { SectionTitle("弹幕设置") }

            item {
                SettingSwitch(
                    title = "弹幕显示",
                    subtitle = "显示视频弹幕",
                    icon = Icons.Default.Movie,
                    checked = state.danmakuEnabled,
                    onChange = { viewModel.setDanmakuEnabled(it) }
                )
            }
            item {
                SettingSlider(
                    title = "弹幕不透明度",
                    icon = Icons.Default.Opacity,
                    value = state.danmakuOpacity,
                    onValueChange = { viewModel.setDanmakuOpacity(it) }
                )
            }
            item {
                SettingSlider(
                    title = "弹幕字号",
                    icon = Icons.Default.TextFields,
                    value = state.danmakuFontSize.toFloat(),
                    valueRange = 12f..40f,
                    steps = 13,
                    displayValue = "${state.danmakuFontSize}",
                    onValueChange = { viewModel.setDanmakuFontSize(it.toInt()) }
                )
            }
            item {
                SettingSlider(
                    title = "弹幕速度",
                    icon = Icons.Default.Speed,
                    value = state.danmakuSpeed,
                    valueRange = 0.5f..2.5f,
                    displayValue = String.format(Locale.ROOT, "%.1fx", state.danmakuSpeed),
                    onValueChange = { viewModel.setDanmakuSpeed(it) }
                )
            }
            item {
                SettingRow(
                    title = "弹幕屏蔽",
                    subtitle = "关键词 / 正则 / 用户 UID 规则",
                    icon = Icons.Default.Block,
                    onClick = onDanmakuBlockClick
                )
            }
            item {
                // 审核轮204：黑名单管理（PiliPlus blacklist 对齐）
                var showBlacklist by remember { mutableStateOf(false) }
                SettingRow(
                    title = "黑名单管理",
                    subtitle = "查看/移除已拉黑用户",
                    icon = Icons.Default.GppBad,
                    onClick = { showBlacklist = true }
                )
                if (showBlacklist) {
                    BlacklistDialog(onDismiss = { showBlacklist = false })
                }
            }

            // ===== 主题 =====
            item { SectionTitle("外观") }

            item {
                SettingRow(
                    title = "主题模式",
                    subtitle = themeLabel(state.themeMode),
                    icon = if (state.themeMode == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingRow(
                    title = "主题颜色",
                    subtitle = accentFromHex(state.accentColor).label,
                    icon = Icons.Default.Palette,
                    onClick = { showAccentDialog = true }
                )
            }
            item {
                SettingSwitch(
                    title = "AMOLED 纯黑",
                    subtitle = "暗色下使用纯黑背景，省电",
                    icon = Icons.Default.DarkMode,
                    checked = state.amoled,
                    onChange = { viewModel.setAmoled(it) }
                )
            }

            // ===== 关于 =====
            item { SectionTitle("关于") }
            item {
                SettingRow(
                    title = "关于 PiliNara",
                    subtitle = "Kotlin+Rust 版 v1.0 · Flutter 版功能移植中",
                    icon = Icons.Default.Info,
                    onClick = { showAboutDialog = true }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // 清晰度选择弹窗
    if (showQualityDialog) {
        val options = listOf("auto" to "自动", "1080p" to "1080P 高清", "720p" to "720P 高清", "480p" to "480P 清晰")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("默认清晰度") },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.setVideoQuality(value)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.videoQuality == value,
                                onClick = {
                                    viewModel.setVideoQuality(value)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 主题选择弹窗
    if (showThemeDialog) {
        val options = listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("主题模式") },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(value)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeMode == value,
                                onClick = {
                                    viewModel.setThemeMode(value)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 主题颜色弹窗
    if (showAccentDialog) {
        AccentColorDialog(
            current = state.accentColor,
            onPick = { viewModel.setAccentColor(it) },
            onDismiss = { showAccentDialog = false }
        )
    }

    // 关于弹窗
    if (showAboutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAboutDialog = false }) {
                    androidx.compose.material3.Text("知道了")
                }
            },
            title = { androidx.compose.material3.Text("关于 PiliNara") },
            text = {
                androidx.compose.material3.Text(
                    "PiliNara —— B站第三方客户端\n\n" +
                        "架构：Kotlin + Jetpack Compose + Rust native (ARM64)\n" +
                        "网络：Ktor + Wbi 签名\n" +
                        "存储：Room + DataStore\n" +
                        "播放：Media3 ExoPlayer\n\n" +
                        "本项目为 Flutter 原版的功能移植实现。"
                )
            }
        )
    }

    // Kazumi：渲染器选择弹窗
    if (showRendererDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRendererDialog = false },
            title = { Text("视频渲染器") },
            text = {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setRenderer(0)
                                showRendererDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = state.renderer == 0,
                            onClick = { viewModel.setRenderer(0); showRendererDialog = false }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("SurfaceView", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "性能最佳、功耗最低（默认）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setRenderer(1)
                                showRendererDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = state.renderer == 1,
                            onClick = { viewModel.setRenderer(1); showRendererDialog = false }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("TextureView", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "支持圆角/动画合成，性能略低",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRendererDialog = false }) { Text("取消") }
            }
        )
    }
}

private fun qualityLabel(v: String) = when (v) {
    "1080p" -> "1080P 高清"; "720p" -> "720P 高清"; "480p" -> "480P 清晰"; else -> "自动"
}

private fun themeLabel(v: String) = when (v) {
    "light" -> "浅色"; "dark" -> "深色"; else -> "跟随系统"
}

/** 主题颜色选择弹窗（20 色板） */
@Composable
private fun AccentColorDialog(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题颜色") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ACCENT_OPTIONS.size) { idx ->
                    val opt = ACCENT_OPTIONS[idx]
                    val selected = opt.hex.equals(current, ignoreCase = true) ||
                        (current.isEmpty() && idx == 0)
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(color = opt.color, shape = MaterialTheme.shapes.small)
                            .clickable {
                                onPick(if (idx == 0) "" else opt.hex)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, "已选", tint = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { if (subtitle.isNotEmpty()) Text(subtitle) },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { if (subtitle.isNotEmpty()) Text(subtitle) },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onChange)
        }
    )
}

@Composable
private fun SettingSlider(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    displayValue: String? = null,
    onValueChange: (Float) -> Unit
) {
    ListItem(
        headlineContent = {
            Column {
                Row {
                    Text(title)
                    Spacer(Modifier.weight(1f))
                    Text(
                        displayValue ?: String.format(Locale.ROOT, "%.0f%%", value * 100),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps
                )
            }
        },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
    )
}


/** 审核轮204：黑名单管理对话框（列表 + 分页 + 移除） */
@Composable
private fun BlacklistDialog(onDismiss: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var items by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var pn by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf<String?>(null) }

    suspend fun load(page: Int) {
        loading = true
        com.example.pilinara.data.remote.BiliApiClient().getBlacklist(page)
            .onSuccess { resp ->
                if (resp.optInt("code") == 0) {
                    val list = resp.optJSONObject("data")?.optJSONArray("list")
                    total = resp.optJSONObject("data")?.optInt("total") ?: 0
                    items = if (page == 1) {
                        (0 until (list?.length() ?: 0)).map { list!!.getJSONObject(it) }
                    } else items + (0 until (list?.length() ?: 0)).map { list!!.getJSONObject(it) }
                    pn = page
                } else msg = "加载失败 code=${resp.optInt("code")}"
            }.onFailure { msg = "加载失败: ${it.message}" }
        loading = false
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { load(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("黑名单（$total 人）") },
        text = {
            Column(Modifier.heightIn(max = 400.dp)) {
                if (loading && items.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() }
                }
                msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!loading && items.isEmpty() && msg == null) Text("黑名单为空")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(items) { _, u ->
                        val mid = u.optLong("mid")
                        val uname = u.optString("uname", "用户$mid")
                        val face = u.optString("face")
                        ListItem(
                            headlineContent = { Text(uname, maxLines = 1) },
                            leadingContent = {
                                coil.compose.AsyncImage(
                                    model = face,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            trailingContent = {
                                TextButton(onClick = {
                                    scope.launch {
                                        com.example.pilinara.data.remote.BiliApiClient()
                                            .modifyBlacklist(mid, 2)
                                            .onSuccess { ok ->
                                                if (ok) {
                                                    items = items.filterNot { it.optLong("mid") == mid }
                                                    total = (total - 1).coerceAtLeast(0)
                                                    msg = "已将 $uname 移出黑名单"
                                                } else msg = "移除失败（需登录）"
                                            }.onFailure { msg = "移除失败: ${it.message}" }
                                    }
                                }) { Text("移除") }
                            }
                        )
                    }
                    if (items.size < total) {
                        item {
                            TextButton(onClick = { scope.launch { load(pn + 1) } }) {
                                Text("加载更多（${items.size}/$total）")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
