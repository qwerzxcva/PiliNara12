package com.example.pilinara.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale

/**
 * Live Screen
 * Replaces Flutter live room page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen() {
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "网游", "手游", "娱乐", "知识", "体育")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("直播") },
                actions = {
                    IconButton(onClick = { /* search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Category tabs
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                categories.forEach { category ->
                    TextButton(onClick = { selectedCategory = category }) {
                        Text(category)
                    }
                }
            }
            
            // Live grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(20) { index ->
                    LiveCard(
                        title = "直播标题 $index",
                        broadcaster = "主播$name$index",
                        thumbnail = "https://example.com/live$index.jpg",
                        viewerCount = "${(index + 1) * 10}万",
                        isLive = true,
                        tags = listOf("网游", "竞技")
                    )
                }
            }
        }
    }
}

@Composable
fun LiveCard(
    title: String,
    broadcaster: String,
    thumbnail: String,
    viewerCount: String,
    isLive: Boolean,
    tags: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            // Thumbnail placeholder
            Surface(
                modifier = Modifier.fillMaxWidth(),
                height = 180.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp))
                }
            }
            
            // Live badge
            if (isLive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = "直播中",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
            
            // Viewer count
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "👁 $viewerCount",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = broadcaster,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}
