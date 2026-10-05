package com.example.pilinara.ui.pages.article

import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.pilinara.data.model.ArticleData
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 批次L23：专栏文章阅读页 ViewModel
 */
class ArticleViewModel : ViewModel() {
    private val _article = MutableStateFlow<ArticleData?>(null)
    val article: StateFlow<ArticleData?> = _article.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(id: Long) {
        if (_article.value != null) return
        viewModelScope.launch {
            BiliApiClient().getArticleView(id)
                .onSuccess { resp ->
                    if (resp.code == 0 && resp.data != null) _article.value = resp.data
                    else _error.value = resp.message ?: "文章加载失败"
                }
                .onFailure { _error.value = it.message ?: "文章加载失败" }
        }
    }
}

/**
 * 专栏文章阅读页（批次L23）：标题/作者/统计/正文 HTML 渲染
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    articleId: Long,
    onBack: () -> Unit = {}
) {
    val vm: ArticleViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    LaunchedEffect(articleId) { vm.load(articleId) }
    val article by vm.article.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("专栏", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            article == null && error == null -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text(error ?: "")
            }
            else -> article?.let { a ->
                Column(
                    Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(a.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            buildString {
                                append(a.author?.name.orEmpty())
                                if (a.publishTime > 0) {
                                    append(" · ")
                                    append(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                                        .format(java.util.Date(a.publishTime * 1000)))
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    a.stats?.let { s ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "阅读 ${s.view} · 点赞 ${s.like} · 评论 ${s.reply} · 收藏 ${s.favorite}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (a.bannerUrl.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        AsyncImage(
                            model = a.bannerUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // 正文 HTML（/x/article/view content 为 HTML 片段）
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                textSize = 15f
                                setTextColor(android.graphics.Color.DKGRAY)
                                setLineSpacing(4f, 1.1f)
                            }
                        },
                        update = { tv ->
                            tv.text = if (android.os.Build.VERSION.SDK_INT >= 24)
                                Html.fromHtml(a.content, Html.FROM_HTML_MODE_COMPACT)
                            else @Suppress("DEPRECATION") Html.fromHtml(a.content)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
