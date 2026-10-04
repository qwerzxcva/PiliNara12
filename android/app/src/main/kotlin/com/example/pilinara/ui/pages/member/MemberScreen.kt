package com.example.pilinara.ui.pages.member

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onBack: () -> Unit = {},
    viewModel: MemberViewModel = viewModel(factory = MemberViewModelFactory(mid))
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

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
                        model = state.info?.face,
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

            // ===== 排序切换 =====
            item {
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

            when {
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
                model = video.pic,
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
