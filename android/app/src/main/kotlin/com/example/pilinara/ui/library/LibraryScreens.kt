package com.example.pilinara.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pilinara.data.model.FavFolder
import com.example.pilinara.data.model.FavMedia
import com.example.pilinara.data.model.HistoryItem
import com.example.pilinara.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repo: LibraryRepository = LibraryRepository()
) : ViewModel() {

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _folders = MutableStateFlow<List<FavFolder>>(emptyList())
    val folders: StateFlow<List<FavFolder>> = _folders.asStateFlow()

    private val _medias = MutableStateFlow<List<FavMedia>>(emptyList())
    val medias: StateFlow<List<FavMedia>> = _medias.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadHistory() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repo.history()
                .onSuccess { _history.value = it.data?.list.orEmpty() }
                .onFailure { _error.value = it.message ?: "加载历史失败" }
            _loading.value = false
        }
    }

    fun loadFolders(mid: Long) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repo.favFolders(mid)
                .onSuccess { _folders.value = it.data.orEmpty() }
                .onFailure { _error.value = it.message ?: "加载收藏夹失败" }
            _loading.value = false
        }
    }

    fun loadMedias(mediaId: Long, force: Boolean = false) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repo.favResources(mediaId)
                .onSuccess { _medias.value = it.data?.medias.orEmpty() }
                .onFailure { _error.value = it.message ?: "加载收藏内容失败" }
            _loading.value = false
        }
    }
}

/** 收藏夹列表 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    mid: Long,
    onBack: () -> Unit = {},
    onOpenFolder: (Long) -> Unit = {},
    viewModel: LibraryViewModel = viewModel()
) {
    val folders by viewModel.folders.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(mid) { viewModel.loadFolders(mid) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading && folders.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null && folders.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text(error ?: "")
            }
            folders.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text("暂无收藏夹")
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders, key = { it.id }) { folder ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenFolder(folder.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(folder.title, fontWeight = FontWeight.Bold)
                                Text("${folder.media_count} 个内容",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 收藏夹内的视频 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavMediaScreen(
    mediaId: Long,
    onBack: () -> Unit = {},
    onOpenVideo: (bvid: String, cid: Long) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = viewModel()
) {
    val medias by viewModel.medias.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(mediaId) { viewModel.loadMedias(mediaId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏内容") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { viewModel.loadMedias(mediaId, force = true) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when {
                loading && medias.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && medias.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(error ?: "")
                }
                medias.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("暂无内容")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(medias, key = { it.id }) { media ->
                        MediaRow(
                            cover = media.cover,
                            title = media.title,
                            subtitle = "${media.upper?.name.orEmpty()} · ${formatDur(media.duration)}",
                            onClick = { onOpenVideo(media.bvid, 0L) }
                        )
                    }
                }
            }
        }
    }
}

/** 观看历史 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    onOpenVideo: (bvid: String, cid: Long) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = viewModel()
) {
    val history by viewModel.history.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("观看历史") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { viewModel.loadHistory() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when {
                loading && history.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && history.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("${error}\n（历史记录需要登录后查看）")
                }
                history.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("暂无观看记录")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.aid }) { item ->
                        MediaRow(
                            cover = item.cover,
                            title = item.title,
                            subtitle = "${item.author_name} · ${formatDur(item.duration)}" +
                                if (item.progress > 0 && item.duration > 0)
                                    " · 已看 ${item.progress * 100 / item.duration}%" else "",
                            onClick = { onOpenVideo(item.bvid, item.history?.cid ?: 0L) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(cover: String, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = cover,
                contentDescription = title,
                modifier = Modifier
                    .width(120.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatDur(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
