package com.example.piliai.ui.pages.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.piliai.data.model.VideoItem
import com.example.piliai.ui.components.*
import com.example.piliai.utils.toHttpsUrl

/**
 * Kototoro 风格首页。
 *
 * 重构原因：原 UI 太丑，没有实现 Kototoro 那种顶级设计。
 * 核心改动：
 * 1. 顶栏：搜索/消息/我的平齐一行（方便左右滑动切换）
 * 2. Hero 轮播：240dp 高、20dp 圆角、100dp 渐变遮罩、胶囊指示器
 * 3. 今日推荐：横向大卡片流（280dp 宽、12dp 间距）
 * 4. 视频推荐流：大卡片（信息叠在封面上，不在图下）
 * 5. 所有卡片 20dp 大圆角（Kototoro 规范）
 *
 * 设计规范来源：/tmp/src/kototoro-ui（Material You + 大圆角卡片）。
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVideoClick: (bvid: String, cid: Long) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onMineClick: () -> Unit = {},
    onRankClick: () -> Unit = {},
    onHotMoreClick: () -> Unit = {},
    onZoneClick: () -> Unit = {},
    onBangumiClick: () -> Unit = {},
    onStoryClick: () -> Unit = {},
    onTodayWatchClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 今日推荐：进入首页后异步加载，不阻塞首屏
    val todayWatchViewModel: com.example.piliai.todaywatch.TodayWatchViewModel = viewModel()
    val todayWatchState by todayWatchViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { todayWatchViewModel.loadIfNeeded() }

    // 触底自动加载下一页
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        topBar = {
            // Kototoro 风格顶栏：搜索/消息/我的平齐一行
            KototoroTopBar(
                title = "piliAI",
                onSearchClick = onSearchClick,
                onMessageClick = onMessageClick,
                onMineClick = onMineClick,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state is HomeUiState.Loading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (val s = state) {
                is HomeUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HomeUiState.Success -> {
                    val topRcmd by viewModel.topRcmd.collectAsStateWithLifecycle()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Hero 轮播（Kototoro 风格）
                        if (topRcmd.isNotEmpty()) {
                            item {
                                HeroCarousel(
                                    items = topRcmd,
                                    onItemClick = { onVideoClick(it.bvid, it.cid) },
                                )
                            }
                        }

                        // 今日推荐 Section（横向大卡片流）
                        val tw = todayWatchState
                        if (tw is com.example.piliai.todaywatch.TodayWatchViewModel.UiState.Success) {
                            item {
                                KototoroSectionHeader(
                                    title = "今日推荐",
                                    subtitle = "为你精选",
                                    onSeeAllClick = onTodayWatchClick,
                                )
                            }
                            item {
                                KototoroHorizontalCardList(
                                    items = tw.plan.videoQueue,
                                ) { video ->
                                    KototoroVideoCard(
                                        title = video.title,
                                        cover = video.cover.toHttpsUrl(),
                                        upName = video.ownerName,
                                        playCount = video.playCountText,
                                        duration = video.durationText,
                                        onClick = { onVideoClick(video.bvid, 0L) },
                                    )
                                }
                            }
                        }

                        // 视频推荐流（大卡片，信息叠在封面上）
                        item {
                            KototoroSectionHeader(
                                title = "推荐",
                                subtitle = "热门视频",
                                onSeeAllClick = onHotMoreClick,
                            )
                        }
                        items(s.items) { card ->
                            Box(Modifier.padding(horizontal = 16.dp)) {
                                KototoroVideoCard(
                                    title = card.title,
                                    cover = card.pic.toHttpsUrl(),
                                    upName = card.author,
                                    playCount = card.viewCountText,
                                    duration = card.durationText,
                                    onClick = { onVideoClick(card.bvid, card.cid) },
                                )
                            }
                        }
                    }
                }
                is HomeUiState.Error -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "加载失败：${s.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kototoro 风格 Hero 轮播：
 * - 高度 240dp、底部圆角 20dp
 * - 100dp 渐变遮罩保证标题可读
 * - 胶囊指示器（选中 16×6，未选中 6×6）
 */
@Composable
private fun HeroCarousel(
    items: List<com.example.piliai.data.model.TopRcmdItem>,
    onItemClick: (com.example.piliai.data.model.TopRcmdItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(240.dp),
            pageSpacing = 0.dp,
        ) { page ->
            val item = items[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onItemClick(item) }
            ) {
                AsyncImage(
                    model = item.pic.ifBlank { item.cover }.toHttpsUrl(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // 100dp 渐变遮罩（底部）
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                            )
                        )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val upName = item.owner?.name ?: item.name
                    if (upName.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = upName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        // 胶囊指示器
        if (items.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(items.size) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .width(if (selected) 16.dp else 6.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}
