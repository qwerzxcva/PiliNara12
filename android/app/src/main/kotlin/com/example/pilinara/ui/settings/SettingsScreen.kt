package com.example.pilinara.ui.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SettingItem(
    val title: String,
    val subtitle: String = "",
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit = {},
    val toggleState: Boolean? = null,
    val onToggle: ((Boolean) -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var darkMode by remember { mutableStateOf(false) }
    var autoPlay by remember { mutableStateOf(false) }
    var danmakuEnabled by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 播放设置
            item {
                SectionTitle("播放设置")
            }
            
            item {
                SettingRow(
                    title = "自动播放",
                    subtitle = "进入页面后自动播放视频",
                    icon = Icons.Default.PlayArrow,
                    toggleState = autoPlay,
                    onToggle = { autoPlay = it }
                )
            }
            
            item {
                SettingRow(
                    title = "弹幕显示",
                    subtitle = "显示视频弹幕",
                    icon = Icons.Default.Movie,
                    toggleState = danmakuEnabled,
                    onToggle = { danmakuEnabled = it }
                )
            }
            
            item {
                SettingRow(
                    title = "倍速播放",
                    subtitle = "支持 0.5x - 2.0x 倍速",
                    icon = Icons.Default.Speed,
                    onClick = { /* TODO: Open speed settings */ }
                )
            }
            
            // 显示设置
            item {
                SectionTitle("显示设置")
            }
            
            item {
                SettingRow(
                    title = "深色模式",
                    subtitle = "切换深色/浅色主题",
                    icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    toggleState = darkMode,
                    onToggle = { darkMode = it }
                )
            }
            
            item {
                SettingRow(
                    title = "字体大小",
                    subtitle = "调整应用字体大小",
                    icon = Icons.Default.TextFields,
                    onClick = { /* TODO: Open font size settings */ }
                )
            }
            
            item {
                SettingRow(
                    title = "视频清晰度",
                    subtitle = "默认播放清晰度",
                    icon = Icons.Default.HighQuality,
                    onClick = { /* TODO: Open quality settings */ }
                )
            }
            
            // 存储设置
            item {
                SectionTitle("存储设置")
            }
            
            item {
                SettingRow(
                    title = "清除缓存",
                    subtitle = "已使用 0 MB",
                    icon = Icons.Default.Delete,
                    onClick = { /* TODO: Clear cache */ }
                )
            }
            
            item {
                SettingRow(
                    title = "下载管理",
                    subtitle = "查看和管理下载内容",
                    icon = Icons.Default.Download,
                    onClick = { /* TODO: Open download manager */ }
                )
            }
            
            // 关于
            item {
                SectionTitle("关于")
            }
            
            item {
                SettingRow(
                    title = "关于我们",
                    subtitle = "PiliNara v1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO: Open about page */ }
                )
            }
            
            item {
                SettingRow(
                    title = "意见反馈",
                    subtitle = "向我们提交建议",
                    icon = Icons.Default.Feedback,
                    onClick = { /* TODO: Open feedback form */ }
                )
            }
            
            item {
                SettingRow(
                    title = "评分评分",
                    subtitle = "给我们评分",
                    icon = Icons.Default.Star,
                    onClick = { /* TODO: Rate app */ }
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {},
    toggleState: Boolean? = null,
    onToggle: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (toggleState != null && onToggle != null) {
                Switch(
                    checked = toggleState,
                    onCheckedChange = onToggle
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
