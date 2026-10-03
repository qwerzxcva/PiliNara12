package com.example.pilinara.ui.comments

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

/**
 * Comment Screen - Replaces Flutter comment page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    bvid: String,
    onBack: () -> Unit = {}
) {
    var sortBy by remember { mutableStateOf("hot") }
    val sortOptions = listOf("热门", "最新")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("评论 ($bvid)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
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
            // Sort tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                sortOptions.forEach { option ->
                    FilterChip(
                        selected = sortBy == option.lowercase(),
                        onClick = { sortBy = option.lowercase() },
                        label = { Text(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            
            // Comment count
            Text(
                text = "共 1,234 条评论",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(8.dp)
            )
            
            // Comment list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top comments (sorted by likes)
                items(commentData.take(10)) { comment ->
                    CommentItem(comment = comment)
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: CommentItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = MaterialTheme.shapes.circle
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // User info
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.username, style = MaterialTheme.typography.titleSmall)
                        if (comment.isVip) {
                            Badge(text = "大会员")
                        }
                    }
                    Text(comment.time, style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    if (comment.likes > 0) {
                        Text(comment.likes.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Footer actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CommentActionButton(
                    icon = Icons.Default.Reply,
                    label = "回复"
                )
                CommentActionButton(
                    icon = Icons.Default.ThumbUp,
                    label = "点赞"
                )
                if (comment.isAuthor) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "UP主",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Replies
            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                comment.replies.forEach { reply ->
                    ReplyItem(reply = reply)
                }
            }
        }
    }
}

@Composable
fun ReplyItem(reply: ReplyItem) {
    Row(
        modifier = Modifier.padding(start = 44.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("@", style = MaterialTheme.typography.bodySmall)
        Text(reply.username, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        Text(": ", style = MaterialTheme.typography.bodySmall)
        Text(reply.content, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.weight(1f))
        Text(reply.time, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun CommentActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// Data classes and sample data
data class CommentItem(
    val id: Long,
    val username: String,
    val content: String,
    val time: String,
    val likes: Int = 0,
    val isVip: Boolean = false,
    val isAuthor: Boolean = false,
    val replies: List<ReplyItem> = emptyList()
)

data class ReplyItem(
    val username: String,
    val content: String,
    val time: String
)

// Sample comment data
val commentData = listOf(
    CommentItem(
        id = 1,
        username = "用户A",
        content = "这个视频太棒了！期待更多这样的内容！",
        time = "2小时前",
        likes = 1234,
        isVip = true
    ),
    CommentItem(
        id = 2,
        username = "用户B",
        content = "UP主辛苦了，制作质量很高",
        time = "5小时前",
        likes = 567,
        isAuthor = true
    ),
    CommentItem(
        id = 3,
        username = "用户C",
        content = "学到了，感谢分享！",
        time = "1天前",
        likes = 89,
        replies = listOf(
            ReplyItem("UP主", "谢谢支持！记得一键三连~", "1天前")
        )
    ),
    CommentItem(
        id = 4,
        username = "用户D",
        content = "什么时候出下一期？",
        time = "2天前",
        likes = 45
    ),
    CommentItem(
        id = 5,
        username = "用户E",
        content = "质量在线，继续加油！",
        time = "3天前",
        likes = 23,
        isVip = true
    )
)
