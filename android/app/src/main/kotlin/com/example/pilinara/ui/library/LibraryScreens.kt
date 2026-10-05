package com.example.pilinara.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.pilinara.data.remote.BiliApiClient
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

    // 历史游标（cursor 分页，批次G）
    private var historyMax: Long = 0L
    private var historyViewAt: Long = 0L
    private var historyHasMore = false

    private val _toView = MutableStateFlow<List<HistoryItem>>(emptyList())
    val toView: StateFlow<List<HistoryItem>> = _toView.asStateFlow()

    fun loadHistory(force: Boolean = false) {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repo.history()
                .onSuccess { resp ->
                    _history.value = resp.data?.list.orEmpty()
                    historyMax = resp.data?.cursor?.max ?: 0L
                    historyViewAt = resp.data?.cursor?.view_at ?: 0L
                    historyHasMore = (resp.data?.cursor?.ps ?: 0) >= 20
                }
                .onFailure { _error.value = it.message ?: "加载历史失败" }
            _loading.value = false
        }
    }

    /** 历史 cursor 翻页 */
    fun loadMoreHistory() {
        if (_loading.value || !historyHasMore) return
        viewModelScope.launch {
            val api = BiliApiClient()
            api.getHistoryCursor(max = historyMax, viewAt = historyViewAt)
                .onSuccess { resp ->
                    val list = resp.data?.list.orEmpty()
                    if (list.isNotEmpty()) {
                        _history.value = _history.value + list
                        historyMax = resp.data?.cursor?.max ?: historyMax
                        historyViewAt = resp.data?.cursor?.view_at ?: historyViewAt
                        historyHasMore = list.size >= 20
                    } else historyHasMore = false
                }
        }
    }

    /** 删除单条历史 */
    fun delHistory(item: HistoryItem) {
        val kid = "${item.business}:${item.history?.oid ?: item.aid}"
        viewModelScope.launch {
            BiliApiClient().delHistory(kid).onSuccess { ok ->
                if (ok) _history.value = _history.value.filterNot { it.aid == item.aid }
            }
        }
    }

    /** 稍后再看列表 */
    fun loadToView() {
        viewModelScope.launch {
            BiliApiClient().getToView().onSuccess { resp ->
                if (resp.code == 0) {
                    _toView.value = resp.data.map { it.toHistoryItem() }
                }
            }
        }
    }

    fun delToView(aid: Long) {
        viewModelScope.launch {
            BiliApiClient().delToView(aid).onSuccess { ok ->
                if (ok) _toView.value = _toView.value.filterNot { it.aid == aid }
            }
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

    // 收藏夹内容无限分页（批次L8）
    private var favPn = 1
    private var favLoadingMore = false
    private var favHasMore = true
    private val _favHasMore = MutableStateFlow(true)
    val favHasMoreFlow: StateFlow<Boolean> = _favHasMore.asStateFlow()

    fun loadMoreMedias(mediaId: Long) {
        if (favLoadingMore || !favHasMore) return
        favLoadingMore = true
        viewModelScope.launch {
            repo.favResourcesPage(mediaId, ++favPn).onSuccess { (items, more) ->
                val known = _medias.value.map { it.id }.toSet()
                _medias.value = _medias.value + items.filterNot { it.id in known }
                favHasMore = more
                _favHasMore.value = more
            }.onFailure { favPn-- }
            favLoadingMore = false
        }
    }

    fun resetFavPage() { favPn = 1; favHasMore = true; _favHasMore.value = true }
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

    LaunchedEffect(mediaId) { viewModel.resetFavPage(); viewModel.loadMedias(mediaId) }

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
                    // 无限分页：滚到倒数第 3 个时加载下一页
                    if (medias.size >= 20) {
                        item {
                            LaunchedEffect(medias.size) { viewModel.loadMoreMedias(mediaId) }
                            Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 观看历史 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
                    items(history, key = { "${it.aid}-${it.history?.cid}" }) { item ->
                        MediaRow(
                            cover = item.cover,
                            title = item.title,
                            subtitle = "${item.author_name} · ${formatDur(item.duration)}" +
                                if (item.progress > 0 && item.duration > 0)
                                    " · 已看 ${item.progress * 100 / item.duration}%" else "",
                            onClick = { onOpenVideo(item.bvid, item.history?.cid ?: 0L) },
                            onLongClick = { viewModel.delHistory(item) }
                        )
                        if (item == history.last() && item != history.first()) {
                            // 触底加载更多（cursor 分页）
                            LaunchedEffect(history.size) { viewModel.loadMoreHistory() }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaRow(
    cover: String, title: String, subtitle: String,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(
        onClick = onClick, onLongClick = onLongClick
    )) {
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

/** 稍后再看（批次G） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToViewScreen(
    onBack: () -> Unit = {},
    onOpenVideo: (String, Long) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = viewModel()
) {
    val toView by viewModel.toView.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadToView() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("稍后再看") },
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
            onRefresh = { viewModel.loadToView() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when {
                loading && toView.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                toView.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("暂无稍后再看视频")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(toView, key = { it.aid }) { item ->
                        MediaRow(
                            cover = item.cover,
                            title = item.title,
                            subtitle = "${item.author_name} · ${formatDur(item.duration)}",
                            onClick = { onOpenVideo(item.bvid, item.history?.cid ?: 0L) },
                            onLongClick = { viewModel.delToView(item.aid) }
                        )
                    }
                }
            }
        }
    }
}
