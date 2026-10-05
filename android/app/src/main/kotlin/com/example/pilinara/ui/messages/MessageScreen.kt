package com.example.pilinara.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.pilinara.data.model.MsgFeedItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.pilinara.utils.toHttpsUrl

private val timeFormat by lazy { SimpleDateFormat("MM-dd HH:mm", Locale.CHINA) }

/**
 * Message Screen（批次H）——回复/@/赞 消息流 + 未读数（真实 API）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    onGoLogin: () -> Unit = {},
    onOpenVideo: (String, Long) -> Unit = { _, _ -> },
    onOpenUser: (Long) -> Unit = {},
    onOpenSessions: () -> Unit = {},
    viewModel: MessageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val tabs = listOf("回复", "@我的", "收到的赞")

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
                title = { Text("消息中心") },
                actions = {
                    IconButton(onClick = onOpenSessions) {
                        Icon(Icons.Default.ChatBubbleOutline, "私聊")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!state.isLogin) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("登录后查看消息", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onGoLogin) { Text("去登录") }
                    }
                }
                return@Column
            }

            // Tab + 未读角标
            TabRow(selectedTabIndex = state.tab) {
                val unread = listOf(state.unreadReply, state.unreadAt, state.unreadLike)
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.tab == index,
                        onClick = { viewModel.setTab(index) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                if (unread[index] > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            unread[index].toString(),
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            when {
                state.isLoading && state.items.isEmpty() -> Box(
                    Modifier.fillMaxSize(), Alignment.Center
                ) { CircularProgressIndicator() }

                state.items.isEmpty() -> Box(
                    Modifier.fillMaxSize(), Alignment.Center
                ) { Text("暂无消息", color = MaterialTheme.colorScheme.onSurfaceVariant) }

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        MsgRow(item, onOpenVideo, onOpenUser)
                    }
                    if (state.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) {
                                LaunchedEffect(state.items.size) { viewModel.loadMore() }
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
private fun MsgRow(
    item: MsgFeedItem,
    onOpenVideo: (String, Long) -> Unit,
    onOpenUser: (Long) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable {
                val bvid = item.replyContent?.uriBvid.orEmpty()
                if (bvid.isNotEmpty()) onOpenVideo(bvid, 0L)
                else item.user?.mid?.let(onOpenUser)
            }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = item.user?.face.toHttpsUrl(),
            contentDescription = item.user?.uname,
            modifier = Modifier.size(42.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.user?.uname ?: "",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    item.time.takeIf { it > 0 }?.let { timeFormat.format(Date(it * 1000)) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                item.replyContent?.message ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            item.replyContent?.sourceContent?.takeIf { it.isNotEmpty() }?.let { src ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "回复内容: $src",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (item.counts > 1) {
            Text(
                "x${item.counts}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
