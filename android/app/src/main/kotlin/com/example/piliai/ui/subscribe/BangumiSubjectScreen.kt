package com.example.piliai.ui.subscribe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.piliai.data.remote.BangumiApi

/**
 * 订阅条目的 Bangumi 介绍页：封面、评分、开播日期与简介。
 * 视频本身仍来自订阅源，本页只展示 Bangumi 的公开资料。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BangumiSubjectScreen(subjectId: Long, onBack: () -> Unit) {
    var subject by remember { mutableStateOf<BangumiApi.Subject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subjectId) {
        runCatching { BangumiApi.subject(subjectId) }
            .onSuccess { subject = it }
            .onFailure { error = it.message ?: "加载失败" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject?.title ?: "条目详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            subject == null && error == null -> CircularProgressIndicator(Modifier.padding(padding).padding(24.dp))
            error != null -> Text(error!!, Modifier.padding(padding).padding(24.dp))
            else -> {
                val s = subject!!
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                ) {
                    if (s.cover.isNotBlank()) {
                        AsyncImage(
                            model = s.cover,
                            contentDescription = s.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(240.dp)
                        )
                    }
                    Column(Modifier.padding(16.dp)) {
                        Text(s.title, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildString {
                                if (s.rating.score > 0) append("评分 ${s.rating.score}　")
                                if (s.rating.rank > 0) append("排名 ${s.rating.rank}　")
                                if (s.date.isNotBlank()) append(s.date)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (s.summary.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(s.summary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
