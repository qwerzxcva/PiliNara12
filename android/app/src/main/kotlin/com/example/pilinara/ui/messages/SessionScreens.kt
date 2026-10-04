package com.example.pilinara.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pilinara.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tsFmt = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)

/**
 * 私聊会话列表页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onBack: () -> Unit = {},
    onGoLogin: () -> Unit = {},
    onOpenChat: (Long) -> Unit = {}
) {
    val vm: SessionListViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(state.error) {
        // 未登录交给 UI 提示
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("私聊") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.error == "请先登录" -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("登录后查看私聊")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onGoLogin) { Text("去登录") }
                }
            }
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.sessions.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("暂无私聊会话", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(state.sessions, key = { it.talkerId }) { s ->
                    ListItem(
                        headlineContent = {
                            Text(s.accountInfo?.name ?: "用户${s.talkerId}", fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            Text(s.lastText(), maxLines = 1, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingContent = {
                            AsyncImage(
                                model = s.accountInfo?.face,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    tsFmt.format(Date((s.lastMsg?.timestamp ?: 0) * 1000)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (s.unreadCount > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Badge { Text("${s.unreadCount}") }
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpenChat(s.talkerId) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * 单聊页（消息气泡 + 发送框）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    talkerId: Long,
    talkerName: String = "",
    onBack: () -> Unit = {},
    onGoLogin: () -> Unit = {},
    vm: ChatViewModel = viewModel(key = "chat_$talkerId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(talkerId) as T
        })
) {
    val state by vm.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(talkerName.ifBlank { "对话" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("发送消息…") },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.large
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (input.isNotBlank() && !state.sending) {
                                vm.send(input); input = ""
                            }
                        },
                        enabled = input.isNotBlank() && !state.sending
                    ) {
                        if (state.sending) CircularProgressIndicator(Modifier.size(20.dp))
                        else Icon(Icons.AutoMirrored.Filled.Send, "发送")
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.error == "请先登录" -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("登录后查看消息")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onGoLogin) { Text("去登录") }
                }
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { m ->
                    val mine = m.senderUid == state.myMid
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                    ) {
                        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                            Surface(
                                color = if (mine) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(
                                    topStart = 12.dp, topEnd = 12.dp,
                                    bottomStart = if (mine) 12.dp else 4.dp,
                                    bottomEnd = if (mine) 4.dp else 12.dp
                                )
                            ) {
                                // 图片消息（msg_type=2）→ 显示图片；文本消息 → 文字
                                val img = m.msgImage()
                                if (img != null) {
                                    coil.compose.AsyncImage(
                                        model = img.url,
                                        contentDescription = "图片消息",
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(
                                                width = 180.dp,
                                                height = (180f * img.height / img.width.coerceAtLeast(1)).dp.coerceIn(80.dp, 260.dp)
                                            ),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        m.msgText(),
                                        modifier = Modifier.padding(10.dp),
                                        color = if (mine) MaterialTheme.colorScheme.onPrimary
                                                else Color.Unspecified,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            Text(
                                tsFmt.format(Date(m.timestamp * 1000)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
