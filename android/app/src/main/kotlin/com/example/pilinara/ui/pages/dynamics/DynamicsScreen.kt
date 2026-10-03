package com.example.pilinara.ui.pages.dynamics

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicsScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("关注", "发现", "直播")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("动态") },
                actions = {
                    IconButton(onClick = {}) {
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
            when (selectedTab) {
                0 -> FollowDynamics()
                1 -> DiscoverDynamics()
                2 -> LiveDynamics()
            }
        }
    }
}

@Composable
fun FollowDynamics() {
    // Implemented with sample data for demo
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(5) { index ->
            DynamicCard(
                author = "UP主${index + 1}",
                time = "${index + 1}小时前",
                content = "这是一条动态内容，描述了一些有趣的事情...",
                likes = (index + 1) * 100,
                comments = (index + 1) * 10
            )
        }
    }
}

@Composable
fun DiscoverDynamics() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("发现页面", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun LiveDynamics() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("直播页面", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun DynamicCard(
    author: String,
    time: String,
    content: String,
    likes: Int,
    comments: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(author, style = MaterialTheme.typography.titleSmall)
                    Text(time, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content
            Text(content, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ActionButton(icon = Icons.Default.Favorite, count = likes)
                ActionButton(icon = Icons.Default.Comment, count = comments)
                ActionButton(icon = Icons.Default.Share, count = 0)
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        if (count > 0) {
            Text(count.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
