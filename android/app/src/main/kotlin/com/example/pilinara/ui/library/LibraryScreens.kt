package com.example.pilinara.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pilinara.data.repository.LibraryRepository

/**
 * 我的仓库 - 包含历史、收藏、离线缓存
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onVideoClick: (String, Long) -> Unit = { _, _ -> }
) {
    val tabs = listOf("历史", "收藏", "离线缓存")
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的仓库") },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> HistoryTab(viewModel.historyItems.value, onVideoClick)
                1 -> FavoritesTab(viewModel.favoriteItems.value, onVideoClick)
                2 -> DownloadTab(viewModel.downloadItems.value)
            }
        }
    }
}

@Composable
fun HistoryTab(
    items: List<HistoryItem>,
    onVideoClick: (String, Long) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("暂无观看历史", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                HistoryItemCard(item = item, onClick = { onVideoClick(item.bvid, item.cid) })
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .padding(end = 12.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            }
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.author, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("观看时长 ${item.watchDuration}", style = MaterialTheme.typography.labelSmall)
            }
            
            // Progress
            if (item.progress > 0 && item.duration > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    LinearProgressIndicator(
                        progress = item.progress.toFloat() / item.duration,
                        modifier = Modifier.width(60.dp)
                    )
                    Text("${item.progress}s/${item.duration}s", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun FavoritesTab(
    items: List<FavoriteItem>,
    onVideoClick: (String, Long) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("暂无收藏", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                FavoriteItemCard(item = item, onClick = { onVideoClick(item.bvid, item.cid) })
            }
        }
    }
}

@Composable
fun FavoriteItemCard(item: FavoriteItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .padding(end = 12.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null)
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.author, style = MaterialTheme.typography.bodySmall)
                if (item.folderName.isNotEmpty()) {
                    Text("合集: ${item.folderName}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun DownloadTab(items: List<DownloadItem>) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("暂无下载", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                DownloadItemCard(item = item)
            }
        }
    }
}

@Composable
fun DownloadItemCard(item: DownloadItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text("${item.size}MB • ${item.quality}", style = MaterialTheme.typography.labelSmall)
                }
                if (item.status == "已完成") {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                } else if (item.status == "下载中") {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            if (item.status == "下载中") {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = item.progress / 100f)
            }
        }
    }
}

// Data classes
data class HistoryItem(
    val id: Long = 0L,
    val bvid: String = "",
    val cid: Long = 0L,
    val title: String = "",
    val author: String = "",
    val cover: String = "",
    val duration: Long = 0L,
    val progress: Long = 0L,
    val watchDuration: String = "",
    val lastWatchTime: Long = 0L
)

data class FavoriteItem(
    val id: Long = 0L,
    val bvid: String = "",
    val cid: Long = 0L,
    val title: String = "",
    val author: String = "",
    val cover: String = "",
    val folderName: String = "",
    val favoritedTime: Long = 0L
)

data class DownloadItem(
    val id: Long = 0L,
    val bvid: String = "",
    val title: String = "",
    val quality: String = "1080p",
    val size: Int = 0,
    val status: String = "等待中",
    val progress: Int = 0,
    val downloadUrl: String = ""
)
