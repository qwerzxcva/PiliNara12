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
import androidx.compose.ui.unit.dp

/**
 * Settings Screen
 * Replaces Flutter settings page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var darkMode by remember { mutableStateOf(false) }
    var autoPlay by remember { mutableStateOf(false) }
    var danmakuEnabled by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Video section
            sectionHeader("视频播放")
            
            item {
                SettingRow(
                    icon = Icons.Default.PlayArrow,
                    title = "自动播放",
                    switch = autoPlay,
                    onSwitchChange = { autoPlay = it }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.VideoLibrary,
                    title = "默认画质",
                    value = "1080P",
                    onClick = { /* show quality dialog */ }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.Speed,
                    title = "播放速度",
                    value = "1.0x",
                    onClick = { /* show speed dialog */ }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.Subtitles,
                    title = "字幕设置",
                    onClick = { /* navigate to subtitle settings */ }
                )
            }
            
            // Danmaku section
            sectionHeader("弹幕设置")
            
            item {
                SettingRow(
                    icon = Icons.Default.Movie,
                    title = "弹幕开关",
                    switch = danmakuEnabled,
                    onSwitchChange = { danmakuEnabled = it }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.FontDownload,
                    title = "弹幕字体",
                    onClick = { /* show font dialog */ }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.Opacity,
                    title = "弹幕透明度",
                    value = "100%",
                    onClick = { /* show opacity slider */ }
                )
            }
            
            // Appearance section
            sectionHeader("外观设置")
            
            item {
                SettingRow(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    switch = darkMode,
                    onSwitchChange = { darkMode = it }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.TextFields,
                    title = "字体大小",
                    value = "标准",
                    onClick = { /* show font size dialog */ }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.Colorize,
                    title = "主题颜色",
                    onClick = { /* show color picker */ }
                )
            }
            
            // About section
            sectionHeader("关于")
            
            item {
                SettingRow(
                    icon = Icons.Default.Info,
                    title = "关于我们",
                    onClick = { /* navigate to about */ }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.Feedback,
                    title = "意见反馈",
                    onClick = { /* open feedback form */ }
                )
            }
            item {
                SettingRow(
                    icon = Icons.Default.Star,
                    title = "评分评分",
                    onClick = { /* rate app */ }
                )
            }
        }
    }
}

@Composable
fun sectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    switch: Boolean? = null,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        value?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        switch?.let {
            Switch(
                checked = it,
                onCheckedChange = onSwitchChange
            )
        }
    }
}
