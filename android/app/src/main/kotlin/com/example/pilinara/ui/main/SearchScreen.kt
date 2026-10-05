package com.example.pilinara.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pilinara.data.model.SearchResultItem
import com.example.pilinara.utils.toHttpsUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    onVideoClick: (String) -> Unit = {},
    onUserClick: (Long) -> Unit = {},
    onLiveClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.searchState.collectAsState()
    var searchQuery by remember { mutableStateOf(initialQuery) }
    LaunchedEffect(Unit) { if (initialQuery.isNotBlank()) { viewModel.setSearchKeyword(initialQuery); viewModel.performSearch() } }
    var dropdown by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadTrending() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 搜索输入
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.setSearchKeyword(it)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("搜索视频、UP主、直播...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""; viewModel.clearSearch()
                            }) { Icon(Icons.Default.Clear, "清除") }
                        }
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) viewModel.performSearch()
                        }) { Icon(Icons.Default.Send, "搜索") }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // 建议联想
            if (state.suggestions.isNotEmpty() && state.results.isEmpty()) {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(state.suggestions) { s ->
                        ListItem(
                            headlineContent = { Text(s.uname) },
                            leadingContent = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.clickable {
                                searchQuery = s.uname
                                viewModel.setSearchKeyword(s.uname)
                                viewModel.performSearch()
                            }
                        )
                    }
                }
                return@Column
            }

            // 结果区：分类 + 排序过滤条
            if (state.results.isNotEmpty() || state.isSearching) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.searchType == "video",
                        onClick = { viewModel.setFilter(searchType = "video") },
                        label = { Text("视频") }
                    )
                    FilterChip(
                        selected = state.searchType == "bili_user",
                        onClick = { viewModel.setFilter(searchType = "bili_user") },
                        label = { Text("用户") }
                    )
                    FilterChip(
                        selected = state.searchType == "live",
                        onClick = { viewModel.setFilter(searchType = "live") },
                        label = { Text("直播") }
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showFilter = !showFilter }) {
                        Icon(Icons.Default.Tune, "筛选")
                    }
                    if (showFilter) {
                        DropdownMenu(expanded = showFilter, onDismissRequest = { showFilter = false }) {
                            listOf(
                                "" to "综合排序", "click" to "最多播放",
                                "pubdate" to "最新发布", "danmaku" to "最多弹幕"
                            ).forEach { (v, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { viewModel.setFilter(order = v); showFilter = false }
                                )
                            }
                        }
                    }
                }
            }

            when {
                state.isSearching && state.results.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null && state.results.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("搜索失败: ${state.error}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.performSearch() }) { Text("重试") }
                    }
                }

                state.results.isNotEmpty() -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.results) { item ->
                            SearchResultRow(
                                item = item,
                                onVideoClick = onVideoClick,
                                onUserClick = onUserClick,
                                onLiveClick = onLiveClick
                            )
                        }
                        if (state.hasMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                    LaunchedEffect(state.results.size) { viewModel.loadMore() }
                                    CircularProgressIndicator(Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }

                // 首屏：热搜 + 历史
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    if (state.history.isNotEmpty()) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("搜索历史", style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.clearHistory() }) { Text("清空") }
                            }
                        }
                        items(state.history) { h ->
                            ListItem(
                                headlineContent = { Text(h) },
                                leadingContent = { Icon(Icons.Default.History, null) },
                                modifier = Modifier.clickable {
                                    searchQuery = h
                                    viewModel.setSearchKeyword(h)
                                    viewModel.performSearch()
                                }
                            )
                        }
                    }
                    item {
                        Text(
                            "热门搜索",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp, 12.dp)
                        )
                    }
                    items(state.trending) { t ->
                        ListItem(
                            headlineContent = {
                                Text(t.show_name.ifEmpty { t.keyword }, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                if (t.recommend_reason != null) Text(t.recommend_reason)
                            },
                            modifier = Modifier.clickable {
                                searchQuery = t.keyword
                                viewModel.setSearchKeyword(t.keyword)
                                viewModel.performSearch()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    onLiveClick: (Long) -> Unit = {}
) {
    when (item.resultType) {
        "bili_user" -> Row(
            Modifier.fillMaxWidth()
                .clickable { onUserClick(item.mid) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.uimage ?: item.pic.toHttpsUrl(),
                contentDescription = item.author,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(50)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(item.author.ifEmpty { item.title }, fontWeight = FontWeight.SemiBold)
                Text("${item.play} 粉丝", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        "live" -> Row(
            Modifier.fillMaxWidth()
                .clickable(enabled = item.roomid > 0) { onLiveClick(item.roomid) }  // 审核20：直播结果补跳转
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.pic.toHttpsUrl(),
                contentDescription = item.title,
                modifier = Modifier.width(140.dp).height(88.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${item.author} · ${item.online} 在线",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        else -> {  // video
            Row(
                Modifier.fillMaxWidth()
                    .clickable { if (item.bvid.isNotEmpty()) onVideoClick(item.bvid) }
                    .padding(12.dp)
            ) {
                AsyncImage(
                    model = item.pic.toHttpsUrl(),
                    contentDescription = item.title,
                    modifier = Modifier.width(140.dp).height(88.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title.replace(Regex("<[^>]+>"), ""),  // 去 <em> 高亮标签
                        fontWeight = FontWeight.Medium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${item.author} · ${item.duration}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "播放 ${item.play} · 弹幕 ${item.danmaku}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
