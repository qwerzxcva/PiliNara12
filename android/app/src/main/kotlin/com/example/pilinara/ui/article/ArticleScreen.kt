package com.example.pilinara.ui.article

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Article Screen
 * Replaces Flutter article page with markdown/html rendering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(articleId: String) {
    var isLoading by remember { mutableStateOf(true) }
    var article by remember { mutableStateOf<Article?>(null) }
    
    LaunchedEffect(articleId) {
        // TODO: Fetch article from API
        isLoading = false
        article = Article(
            id = articleId,
            title = "这是文章标题",
            author = "作者名",
            content = """
                # 文章正文
                
                这是一段文章内容。

                ## 章节标题

                这里是正文内容...

                **加粗文字** 和 *斜体文字*

                - 列表项1
                - 列表项2
                - 列表项3
            """.trimIndent(),
            likeCount = 1234,
            coinCount = 567,
            favoriteCount = 890,
            commentCount = 123,
            createTime = "2024-01-01"
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文章") },
                actions = {
                    IconButton(onClick = { /* share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (article != null) {
            ArticleContent(article = article!!, modifier = Modifier.padding(padding))
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("加载失败")
            }
        }
    }
}

data class Article(
    val id: String,
    val title: String,
    val author: String,
    val content: String,
    val likeCount: Int,
    val coinCount: Int,
    val favoriteCount: Int,
    val commentCount: Int,
    val createTime: String
)

@Composable
fun ArticleContent(article: Article, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Author and stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.circle
                    ) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(article.author, style = MaterialTheme.typography.bodyMedium)
                }
                Text(article.createTime, style = MaterialTheme.typography.labelSmall)
            }
        }
        
        // Stats row
        item {
            ActionStats(
                likes = article.likeCount,
                coins = article.coinCount,
                favorites = article.favoriteCount,
                comments = article.commentCount
            )
        }
        
        // Content
        item {
            ArticleContentText(text = article.content)
        }
    }
}

@Composable
fun ActionStats(likes: Int, coins: Int, favorites: Int, comments: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatItem(icon = Icons.Default.Favorite, count = likes)
        StatItem(icon = Icons.Default.AttachmentMoney, count = coins)
        StatItem(icon = Icons.Default.FavoriteBorder, count = favorites)
        StatItem(icon = Icons.Default.Comment, count = comments)
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ArticleContentText(text: String) {
    // Simple markdown-like rendering
    // In production, use a proper rich text library
    text.split("\n\n").forEach { paragraph ->
        if (paragraph.startsWith("# ")) {
            Text(
                text = paragraph.removePrefix("# ").trim(),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp)
            )
        } else if (paragraph.startsWith("## ")) {
            Text(
                text = paragraph.removePrefix("## ").trim(),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
            )
        } else if (paragraph.startsWith("- ")) {
            Text(
                text = "  • ${paragraph.removePrefix("- ").trim()}",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = paragraph.trim(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
