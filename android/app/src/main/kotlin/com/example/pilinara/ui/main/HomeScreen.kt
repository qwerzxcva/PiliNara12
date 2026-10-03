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
import com.example.pilinara.ui.theme.PiliNaraTheme

/**
 * Home Screen - Main feed
 * Replaces Flutter home page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onVideoClick: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("推荐", "关注", "直播", "动态")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PiliNara") },
                actions = {
                    IconButton(onClick = { /* search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { /* scan */ }) {
                        Icon(Icons.Default.QrCode, contentDescription = "扫一扫")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    NavigationItem("首页", Icons.Default.Home, 0),
                    NavigationItem("动态", Icons.Default.Feed, 1),
                    NavigationItem("发布", Icons.Default.Add, 2),
                    NavigationItem("我的", Icons.Default.Person, 3)
                )
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == item.index,
                        onClick = { selectedTab = item.index }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> HomeFeed(onVideoClick = onVideoClick)
                1 -> FollowFeed()
                2 -> LiveFeed()
                3 -> DynamicFeed()
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val index: Int
)

@Composable
fun HomeFeed(onVideoClick: (String) -> Unit) {
    // TODO: Implement video feed with waterfall layout
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(20) { index ->
            VideoCard(
                title = "视频标题 $index",
                author = "UP主 $index",
                thumbnail = "https://example.com/thumb$index.jpg",
                duration = "10:25",
                views = "${(index + 1) * 10}万",
                onClick = { onVideoClick("BV1234567890") }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun VideoCard(
    title: String,
    author: String,
    thumbnail: String,
    duration: String,
    views: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail placeholder
            AsyncImage(
                model = thumbnail,
                contentDescription = title,
                modifier = Modifier.fillMaxSize()
            )
            
            // Duration badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
            ) {
                Text(
                    text = duration,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Title and info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = author, style = MaterialTheme.typography.bodySmall)
                    Text(text = "·", style = MaterialTheme.typography.bodySmall)
                    Text(text = views, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun FollowFeed() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("关注页面 - 待实现")
    }
}

@Composable
fun LiveFeed() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("直播页面 - 待实现")
    }
}

@Composable
fun DynamicFeed() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("动态页面 - 待实现")
    }
}
