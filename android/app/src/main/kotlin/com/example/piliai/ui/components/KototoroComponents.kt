package com.example.piliai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Kototoro 风格组件库。
 *
 * 设计规范来源：/tmp/src/kototoro-ui（Material You + 大圆角卡片）。
 * 核心 token：
 * - 主色：默认绿 #5CB67B（可选 19 种 Material You 颜色）
 * - 圆角：卡片 20dp（大）、按钮 6dp（中）、标签 4dp（小）
 * - 间距：卡片间距 8dp、内边距 12dp
 * - 风格：大圆角 + 分层 surface + 渐变遮罩
 */

/**
 * Kototoro 风格大卡片（视频推荐流用）。
 *
 * 特点：
 * - 20dp 大圆角
 * - 16:9 封面 + 底部渐变遮罩
 * - 标题/UP主/播放量叠在封面上（Kototoro 风格：信息在图上，不在图下）
 * - 卡片间距 8dp
 */
@Composable
fun KototoroVideoCard(
    title: String,
    cover: String,
    upName: String,
    playCount: String,
    duration: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp), // Kototoro 大圆角
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box {
            // 封面
            AsyncImage(
                model = cover,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
            )
            // 底部渐变遮罩（保证文字可读）
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )
            // 信息区（叠在封面上）
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                // UP 主 + 播放量
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = upName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "$playCount 次观看",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            // 时长标签（右上角）
            if (duration.isNotBlank()) {
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp) // Kototoro 小圆角标签
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Kototoro 风格横向滚动卡片列表（今日推荐/热门等）。
 *
 * 特点：
 * - 卡片宽度 280dp（大屏）/ 240dp（小屏）
 * - 卡片间距 12dp
 * - 横向滚动
 */
@Composable
fun <T> KototoroHorizontalCardList(
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(items.size) { index ->
            Box(Modifier.width(280.dp)) {
                itemContent(items[index])
            }
        }
    }
}

/**
 * Kototoro 风格 Section 标题（「今日推荐」「热门」等）。
 *
 * 特点：
 * - 左侧标题 + 右侧「查看全部 >」链接
 * - 标题 18sp 加粗
 * - 间距 16dp
 */
@Composable
fun KototoroSectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        onSeeAllClick?.let {
            TextButton(onClick = it) {
                Text(
                    text = "查看全部 >",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Kototoro 风格顶栏（搜索/消息/我的平齐一行）。
 *
 * 特点：
 * - 左侧 Logo/标题
 * - 右侧 3 个图标按钮平齐一行（搜索、消息、我的）
 * - 高度 56dp
 * - 背景色 = surface
 */
@Composable
fun KototoroTopBar(
    title: String,
    onSearchClick: () -> Unit,
    onMessageClick: () -> Unit,
    onMineClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // 右侧 3 个图标按钮平齐一行
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onMessageClick) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.Notifications,
                        contentDescription = "消息",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onMineClick) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.Person,
                        contentDescription = "我的",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
