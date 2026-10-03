package com.example.pilinara.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Common UI Components
 * Replaces Flutter common widgets
 */

// Loading Dialog
@Composable
fun LoadingDialog(show: Boolean, message: String = "加载中...") {
    if (show) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(message) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(message)
                }
            },
            confirmButton = {}
        )
    }
}

// Error Dialog
@Composable
fun ErrorDialog(
    show: Boolean,
    title: String = "错误",
    message: String,
    onRetry: (() -> Unit)? = null
) {
    if (show) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { onDismissRequest() }) {
                    Text("确定")
                }
            },
            dismissButton = {
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        )
    }
}

// Empty State
@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.SearchOff,
    title: String = "暂无内容",
    subtitle: String = "这里什么都没有哦~",
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

// Shimmer Loading
@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium
) {
    val alpha = animateFloatAsState(
        targetValue = if (LocalLifecycleOwner.current.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.STARTED) 1f else 0.4f,
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = shape
            )
    )
}

// Card Loading
@Composable
fun CardLoadingList(count: Int = 5) {
    Column {
        repeat(count) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(100.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail placeholder
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.medium
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerLoading(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerLoading(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// Infinite Scroll List
@Composable
fun InfiniteScrollList(
    items: List<Any>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    itemContent: @Composable (Any) -> Unit
) {
    var pastTextState by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items.size) { index ->
            itemContent(items[index])
        }
        
        if (isLoading || hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("加载中...")
                        }
                    } else if (pastTextState) {
                        Text("没有更多了")
                    }
                }
            }
        }
    }
}
