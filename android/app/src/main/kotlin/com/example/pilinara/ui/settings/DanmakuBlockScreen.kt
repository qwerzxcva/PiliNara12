package com.example.pilinara.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 弹幕屏蔽规则页（关键词/正则/UID 三 Tab + 添加/删除，Room 持久化）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DanmakuBlockScreen(
    onBack: () -> Unit = {},
    context: android.content.Context = LocalContext.current,
    viewModel: DanmakuBlockViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DanmakuBlockViewModel(context) as T
    })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabs = listOf("关键词", "正则", "用户UID")
    var tab by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var errorHint by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("弹幕屏蔽") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = {
                        tab = i; input = ""; errorHint = null
                    }, text = { Text(t) })
                }
            }

            // 添加行
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; errorHint = null },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(when (tab) {
                            0 -> "输入要屏蔽的关键词"
                            1 -> "输入正则表达式"
                            else -> "输入用户 UID"
                        })
                    },
                    isError = errorHint != null,
                    supportingText = { errorHint?.let { Text(it) } },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    when (tab) {
                        0 -> viewModel.addKeyword(input)
                        1 -> {
                            if (runCatching { Regex(input) }.isFailure) {
                                errorHint = "正则表达式不合法"
                                return@Button
                            }
                            viewModel.addRegex(input)
                        }
                        else -> {
                            if (!input.all { it.isDigit() }) {
                                errorHint = "UID 必须为数字"
                                return@Button
                            }
                            viewModel.addUid(input)
                        }
                    }
                    input = ""
                }) { Text("添加") }
            }

            // 规则列表
            val list = when (tab) {
                0 -> state.keywords; 1 -> state.regexes; else -> state.uids
            }
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("暂无规则", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp)) {
                    items(list, key = { it }) { rule ->
                        ListItem(
                            headlineContent = { Text(rule) },
                            leadingContent = {
                                Icon(
                                    when (tab) { 0 -> Icons.Default.TextFields; 1 -> Icons.Default.Code; else -> Icons.Default.Person },
                                    null, tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    when (tab) {
                                        0 -> viewModel.removeKeyword(rule)
                                        1 -> viewModel.removeRegex(rule)
                                        else -> viewModel.removeUid(rule)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "删除",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
