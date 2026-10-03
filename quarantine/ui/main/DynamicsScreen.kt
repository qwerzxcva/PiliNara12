package com.example.pilinara.ui.main

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
 * Dynamics Screen
 * Replaces Flutter dynamics page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicsScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("全部", "投稿", "直播", "粉丝动向")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("动态") },
                actions = {
                    IconButton(onClick = { /* publish */ }) {
                        Icon(Icons.Default.Add, contentDescription = "发布")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tab row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Dynamic list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(15) { index ->
                    DynamicCard(
                        author = "UP主${index + 1}",
                        avatar = "https://example.com/avatar$index.jpg",
                        content = "这是一条动态内容，描述了一些有趣的事情...",
                        mediaCount = if (index % 3 == 0) 3 else 0,
                        time = "${index + 1}小时前",
                        likeCount = (index + 1) * 100,
                        commentCount = (index + 1) * 10,
                        shareCount = (index + 1) * 5
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicCard(
    author: String,
    avatar: String,
    content: String,
    mediaCount: Int,
    time: String,
    likeCount: Int,
    commentCount: Int,
    shareCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.circle
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Author and time
                Column {
                    Text(author, style = MaterialTheme.typography.titleSmall)
                    Text(time, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content
            Text(content, style = MaterialTheme.typography.bodyMedium)
            
            // Media grid (if any)
            if (mediaCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(minOf(mediaCount, 9)) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Actions
            ActionRow(
                likeCount = likeCount,
                commentCount = commentCount,
                shareCount = shareCount
            )
        }
    }
}

@Composable
fun ActionRow(likeCount: Int, commentCount: Int, shareCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ActionButton(icon = Icons.Default.Favorite, count = likeCount)
        ActionButton(icon = Icons.Default.Comment, count = commentCount)
        ActionButton(icon = Icons.Default.Share, count = shareCount)
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelSmall)
    }
}
