package com.example.pilinara.ui.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Message Screen - Replaces Flutter message page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("消息", "私信")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content
            when (selectedTab) {
                0 -> SystemMessages()
                1 -> PrivateMessages()
            }
        }
    }
}

@Composable
fun SystemMessages() {
    val messages = listOf(
        Pair("系统通知", "您的视频已通过审核", "10:30"),
        Pair("活动通知", "新用户注册赠送大会员", "昨天"),
        Pair("系统通知", "密码修改成功", "昨天"),
        Pair("活动通知", "双十一活动即将开始", "3天前"),
        Pair("系统通知", "实名认证已完成", "1周前")
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(messages) { (title, content, time) ->
            MessageItem(title = title, content = content, time = time)
        }
    }
}

@Composable
fun PrivateMessages() {
    val contacts = listOf(
        Pair("用户A", "刚才在吗？", "10:30", 3),
        Pair("用户B", "好的，明天见", "昨天", 0),
        Pair("用户C", "视频收到了，谢谢！", "昨天", 0),
        Pair("用户D", "直播什么时候开始？", "3天前", 1),
        Pair("用户E", "评论已回复", "1周前", 0)
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(contacts) { (name, lastMessage, time, unread) ->
            ContactItem(name = name, lastMessage = lastMessage, time = time, unread = unread)
        }
    }
}

@Composable
fun MessageItem(title: String, content: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(content, style = MaterialTheme.typography.bodyMedium)
            }
            Text(time, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ContactItem(name: String, lastMessage: String, time: String, unread: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.circle
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    if (unread > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(unread = unread)
                    }
                }
                Text(lastMessage, style = MaterialTheme.typography.bodyMedium)
            }
            
            Text(time, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun Badge(unread: Int) {
    Surface(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.error
    ) {
        Text(
            text = unread.toString(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError
        )
    }
}
