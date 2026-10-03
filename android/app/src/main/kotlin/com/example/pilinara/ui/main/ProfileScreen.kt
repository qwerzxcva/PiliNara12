package com.example.pilinara.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Profile Screen
 * Replaces Flutter profile page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    var isLoggedIn by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    IconButton(onClick = { /* settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoggedIn) {
                UserHeader()
                UserStats()
                UserActions()
                UserTabs()
            } else {
                LoginPrompt()
            }
        }
    }
}

@Composable
fun UserHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "头像", modifier = Modifier.size(40.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text("用户名", style = MaterialTheme.typography.titleLarge)
                Text("UID: 12345678", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(text = "Lv.6")
                    Badge(text = "大会员")
                }
            }
            
            // Edit button
            Button(onClick = { /* edit profile */ }) {
                Text("编辑资料")
            }
        }
    }
}

@Composable
fun UserStats() {
    val stats = listOf(
        Pair("关注", "128"),
        Pair("粉丝", "3.2万"),
        Pair("获赞", "15.6万"),
        Pair("播放", "89.3万")
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        stats.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun UserActions() {
    val actions = listOf(
        Pair("历史", Icons.Default.History),
        Pair("收藏", Icons.Default.Favorite),
        Pair("稍后再看", Icons.Default.PlayArrow),
        Pair("离线缓存", Icons.Default.Download)
    )
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(actions) { (label, icon) ->
            ActionButton(icon = icon, label = label)
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun UserTabs() {
    TabRow(
        selectedTabIndex = 0,
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf("投稿", "动态", "直播").forEachIndexed { index, title ->
            Tab(
                selected = index == 0,
                onClick = { /* switch tab */ },
                text = { Text(title) }
            )
        }
    }
}

@Composable
fun LoginPrompt() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("登录后享受更多功能", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* login */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("登录 / 注册")
        }
    }
}

@Composable
fun Badge(text: String) {
    Badge(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
