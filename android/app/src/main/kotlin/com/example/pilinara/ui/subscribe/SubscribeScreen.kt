package com.example.pilinara.ui.subscribe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pilinara.database.SubscribeItemEntity
import com.example.pilinara.database.SubscribeSourceEntity

/**
 * 订阅页（Animeko「订阅」移植）
 *
 * - 顶部：刷新 / 添加订阅源
 * - 内容：订阅源解析出的条目网格（封面 + 标题 + 分集）
 * - 点击条目：解析播放地址 → 交给 PiliNara 播放器
 * - 管理：长按/源卡片可停用、删除
 */
/**
 * 订阅页 ViewModel 工厂：注入 Room DAO（ViewModel 不持有 Context，避免泄漏）
 */
class SubscribeViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscribeViewModel::class.java)) {
            val db = com.example.pilinara.database.PiliNaraDatabase
                .getDatabase(com.example.pilinara.AppContext.get())
            return SubscribeViewModel(
                com.example.pilinara.data.repository.SubscribeRepository(
                    db.subscribeSourceDao(),
                    db.subscribeItemDao()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscribeScreen(
    viewModel: SubscribeViewModel = viewModel(factory = SubscribeViewModelFactory()),
    onPlay: (url: String, title: String, cover: String) -> Unit,
    onBack: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }

    // 错误/提示统一走 Snackbar，展示后清空避免重复弹出
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("订阅") },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加订阅源")
                    }
                    IconButton(onClick = { showManageDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "管理订阅源")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.items.isEmpty() && !state.isRefreshing -> {
                    EmptySubscribeHint(onAdd = { showAddDialog = true })
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            SubscribeItemCard(item = item) {
                                // 审核轮8：不是所有条目都有可播放直链。
                                // RSS 的 <link> 常是网页而非媒体；只有 enclosure/直链
                                // 才能直接交给 ExoPlayer。这里先判定可否播放，
                                // 不能播放时给出明确提示，而不是把网页 URL 丢给播放器
                                // 导致「点了没反应/一直转圈」。
                                if (isLikelyPlayable(item.link)) {
                                    onPlay(item.link, item.title, item.cover)
                                } else {
                                    viewModel.reportNotPlayable(item.title)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { url, name, type ->
                viewModel.addSource(url, name, type)
                showAddDialog = false
            }
        )
    }

    if (showManageDialog) {
        ManageSourcesDialog(
            sources = state.sources,
            onDismiss = { showManageDialog = false },
            onToggle = { id, enabled -> viewModel.toggleSource(id, enabled) },
            onDelete = { id -> viewModel.removeSource(id) }
        )
    }
}

/**
 * 审核轮8：判断链接是否「可能可直接播放」
 *
 * ExoPlayer 能吃：直链媒体（mp4/m4a/webm/...）、HLS(m3u8)、DASH(mpd)。
 * 吃不了：普通网页（.html / 无扩展名的详情页）。
 *
 * 注意：这是启发式判断（看 URL 后缀与路径），不发起网络请求。
 * 判定为可播放不代表一定能播（可能 403/风控），但能避免把明显是网页的
 * URL 丢进播放器导致「点了没反应」。
 */
private fun isLikelyPlayable(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    // HLS / DASH 清单
    if (lower.contains(".m3u8") || lower.contains(".mpd")) return true
    // 常见媒体直链后缀
    val mediaExt = listOf(
        ".mp4", ".m4v", ".webm", ".mkv", ".flv",
        ".mp3", ".m4a", ".aac", ".flac", ".ogg", ".wav", ".ts"
    )
    val path = runCatching { android.net.Uri.parse(url).path ?: "" }.getOrDefault("")
    if (mediaExt.any { path.lowercase().endsWith(it) }) return true
    // 明显是网页
    if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".php")) return false
    // 无扩展名：视为详情页，不可直接播放
    return false
}

@Composable
private fun EmptySubscribeHint(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "还没有订阅内容",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "添加订阅源链接后，这里会显示可播放的条目",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd) { Text("添加订阅源") }
        }
    }
}

@Composable
private fun SubscribeItemCard(item: SubscribeItemEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            Box {
                AsyncImage(
                    model = item.cover.ifBlank { null },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
                if (item.episode.isNotBlank()) {
                    Text(
                        text = item.episode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                    )
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.sourceName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, name: String, type: Int) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SubscribeSourceEntity.TYPE_BANGUMI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅源") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("订阅源链接") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == SubscribeSourceEntity.TYPE_BANGUMI,
                        onClick = { type = SubscribeSourceEntity.TYPE_BANGUMI },
                        label = { Text("Bangumi") }
                    )
                    FilterChip(
                        selected = type == SubscribeSourceEntity.TYPE_RSS,
                        onClick = { type = SubscribeSourceEntity.TYPE_RSS },
                        label = { Text("RSS") }
                    )
                    FilterChip(
                        selected = type == SubscribeSourceEntity.TYPE_JSON,
                        onClick = { type = SubscribeSourceEntity.TYPE_JSON },
                        label = { Text("JSON") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url, name, type) },
                enabled = url.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ManageSourcesDialog(
    sources: List<SubscribeSourceEntity>,
    onDismiss: () -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理订阅源") },
        text = {
            if (sources.isEmpty()) {
                Text("暂无订阅源", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column {
                    sources.forEach { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.name.ifBlank { s.url },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    s.typeLabel + if (s.lastError != null) " · ${s.lastError}" else "",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = s.enabled, onCheckedChange = { onToggle(s.id, it) })
                            IconButton(onClick = { onDelete(s.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
