package com.example.pilinara.ui.pages.member

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.example.pilinara.data.model.SpaceVideoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.pilinara.utils.toHttpsUrl

/** MemberViewModel 需要 mid 构造参数 → Factory */
class MemberViewModelFactory(private val mid: Long) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MemberViewModel(mid) as T
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberScreen(
    mid: Long,
    onOpenVideo: (String, Long) -> Unit = { _, _ -> },
    onOpenFollowList: (Long, Boolean) -> Unit = { _, _ -> },
    onOpenArticle: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MemberViewModel = viewModel(factory = MemberViewModelFactory(mid))
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(0) }  // 0=投稿 1=专栏

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
                title = { Text(state.info?.name ?: "用户空间", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ===== 头部：头像/昵称/签名/数据 =====
            item {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.info?.face.toHttpsUrl(),
                        contentDescription = state.info?.name,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.info?.name ?: "加载中...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Lv.${state.info?.level ?: 0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!state.info?.sign.isNullOrEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                state.info!!.sign,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Button(onClick = { viewModel.toggleFollow() }) {
                        Text(if (state.isFollowing) "已关注" else "关注")
                    }
                }
            }

            // ===== 数据条：粉丝/关注/投稿数 =====
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCell("粉丝", formatCount(state.stat?.follower ?: 0),
                        onClick = { onOpenFollowList(mid, true) })
                    StatCell("关注", formatCount(state.stat?.following ?: 0),
                        onClick = { onOpenFollowList(mid, false) })
                    StatCell("投稿", "${state.videos.size}")
                }
            }

            // ===== 批次L38：代表作横滑（UP 精选置顶，最多 3 条）=====
            if (state.masterpieces.isNotEmpty()) item {
                Column {
                    Text(
                        "代表作",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.masterpieces, key = { it.aid }) { m ->
                            Card(
                                modifier = Modifier.width(200.dp).clickable {
                                    onOpenVideo(m.bvid, 0L)
                                }
                            ) {
                                Column {
                                    AsyncImage(
                                        model = m.pic.toHttpsUrl(),
                                        contentDescription = m.title,
                                        modifier = Modifier.fillMaxWidth().height(110.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(Modifier.padding(8.dp)) {
                                        Text(
                                            m.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${formatCount(m.playCount)}播放 · ${formatCount(m.danmakuCount)}弹幕",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== Tab：投稿 / 专栏（批次L24）=====
            item {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("投稿") })
                    Tab(
                        selected = tab == 1,
                        onClick = {
                            tab = 1
                            if (state.articles.isEmpty()) viewModel.loadArticles(1)
                        },
                        text = { Text("专栏") }
                    )
                    Tab(
                        selected = tab == 2,
                        onClick = {
                            tab = 2
                            if (state.seasons.isEmpty()) viewModel.loadSeasons()
                        },
                        text = { Text("合集") }
                    )
                }
            }

            // ===== 排序切换（仅投稿页显示）=====
            if (tab == 0) item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.order == "pubdate",
                        onClick = { viewModel.setOrder("pubdate") },
                        label = { Text("最新") }
                    )
                    FilterChip(
                        selected = state.order == "click",
                        onClick = { viewModel.setOrder("click") },
                        label = { Text("最多播放") }
                    )
                }
            }

            if (tab == 0) when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.videos.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                        Text("加载失败: ${state.error}", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    items(state.videos, key = { it.bvid.ifEmpty { "aid${it.aid}" } }) { video ->
                        SpaceVideoRow(video) { bvid ->
                            onOpenVideo(bvid, 0L)   // cid 由播放器详情页加载
                        }
                    }
                    if (state.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                LaunchedEffect(state.videos.size) { viewModel.loadMore() }
                                CircularProgressIndicator(Modifier.size(22.dp))
                            }
                        }
                    } else if (state.videos.isNotEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                                Text(
                                    "没有更多了",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ===== 合集列表（批次L27）=====
            if (tab == 2) {
                if (state.seasonsLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.seasons.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                            Text("该 UP 主暂无合集", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    // 打开的合集：显示合集内视频列表
                    if (state.openSeasonMeta != null) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${state.openSeasonMeta?.name ?: state.openSeasonMeta?.title} (${state.seasonVideosTotal})",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.closeSeason() }) { Text("收起") }
                            }
                        }
                        if (state.seasonVideosLoading) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(22.dp))
                                }
                            }
                        }
                        items(state.seasonVideos, key = { "sv${it.bvid}" }) { v ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    .clickable { onOpenVideo(v.bvid, v.cid) }
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = v.pic.toHttpsUrl(),
                                        contentDescription = v.title,
                                        modifier = Modifier.width(120.dp).aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        v.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.seasons, key = { s ->
                            "s${s.meta?.seasonId ?: 0}-${s.meta?.seriesId ?: 0}"
                        }) { s ->
                            val meta = s.meta
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    .clickable { viewModel.openSeason(s) }
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = meta?.cover.toHttpsUrl(),
                                        contentDescription = meta?.name,
                                        modifier = Modifier.width(110.dp).aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            meta?.name ?: meta?.title ?: "",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "共 ${meta?.total ?: 0} 个视频",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== 专栏列表（批次L24）=====
            if (tab == 1) {
                if (state.articles.isEmpty() && state.articlesLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.articles.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                            Text("该 UP 主暂无专栏", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.articles, key = { it.id }) { art ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                .clickable { onOpenArticle(art.id) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(art.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                if (art.summary.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        art.summary, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${art.stats?.view ?: 0} 阅读 · ${art.stats?.reply ?: 0} 评论",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (state.articleHasMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                LaunchedEffect(state.articles.size) { viewModel.loadArticles(state.articlePage + 1) }
                                CircularProgressIndicator(Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpaceVideoRow(video: SpaceVideoItem, onClick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { if (video.bvid.isNotEmpty()) onClick(video.bvid) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box {
            AsyncImage(
                model = video.pic.toHttpsUrl(),
                contentDescription = video.title,
                modifier = Modifier.width(140.dp).height(88.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                video.length,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "播放 ${formatCount(video.play)} · 弹幕 ${formatCount(video.videoReview)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                formatDate(video.created),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCount(n: Long): String = when {
    n >= 100_000_000 -> String.format("%.1f亿", n / 100_000_000.0)
    n >= 10_000 -> String.format("%.1f万", n / 10_000.0)
    else -> n.toString()
}

private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }
private fun formatDate(ts: Long): String =
    if (ts <= 0) "" else dateFormat.format(Date(ts * 1000))
