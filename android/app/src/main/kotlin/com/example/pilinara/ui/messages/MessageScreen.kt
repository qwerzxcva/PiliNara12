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
 * Message Screen
 * Replaces Flutter message page
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
                    IconButton(onClick = { /* scan */ }) {
                        Icon(Icons.Default.QrCode, contentDescription = "扫一扫")
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
            
            // Message list
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
        Pair("系统通知", "您的视频已通过审核"),
        Pair("活动通知", "新用户注册赠送大会员"),
        Pair("系统通知", "密码修改成功"),
        Pair("活动通知", "双十一活动即将开始"),
        Pair("系统通知", "实名认证已完成")
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(messages) { (title, content) ->
            MessageItem(title = title, content = content)
        }
    }
}

@Composable
fun PrivateMessages() {
    val contacts = listOf(
        Pair("用户A", "刚才在吗？"),
        Pair("用户B", "好的，明天见"),
        Pair("用户C", "视频收到了，谢谢！"),
        Pair("用户D", "直播什么时候开始？"),
        Pair("用户E", "评论已回复")
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(contacts) { (name, lastMessage) ->
            ContactItem(name = name, lastMessage = lastMessage)
        }
    }
}

@Composable
fun MessageItem(title: String, content: String) {
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
            Text("10:30", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ContactItem(name: String, lastMessage: String) {
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge(text = "3")
                }
                Text(lastMessage, style = MaterialTheme.typography.bodyMedium)
            }
            
            Text("昨天", style = MaterialTheme.typography.labelSmall)
        }
    }
}
