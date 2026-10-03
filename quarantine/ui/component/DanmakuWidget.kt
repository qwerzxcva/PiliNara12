package com.example.pilinara.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.pilinara.utils.StorageManager

/**
 * Danmaku settings widget
 * Replaces Flutter danmaku settings
 */
@Composable
fun DanmakuSettingsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    settings: DanmakuSettings = DanmakuSettings()
) {
    if (!visible) return
    
    var showAdvanced by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("弹幕设置", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("显示弹幕", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { /* TODO: Update setting */ }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Opacity slider
            Text("透明度: ${"%.0f".format(settings.opacity * 100)}%", 
                style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.opacity,
                onValueChange = { /* TODO: Update opacity */ },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Font size
            Text("字体大小: ${settings.fontSize.toInt()}sp", 
                style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { /* TODO: Update font size */ },
                valueRange = 10f..40f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Speed
            Text("速度: ${"%.1f".format(settings.speed)}", 
                style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.speed,
                onValueChange = { /* TODO: Update speed */ },
                valueRange = 0.5f..3f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Area options
            Text("显示区域", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("顶部", "底部", "滚动", "高级", "字幕").forEach { area ->
                    FilterChip(
                        selected = settings.areas.contains(area),
                        onClick = { /* toggle area */ },
                        label = { Text(area) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Advanced toggle
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showAdvanced) "收起高级选项" else "显示高级选项")
            }
            
            if (showAdvanced) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Font weight
                Text("字体粗细", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("常规", "加粗").forEach { weight ->
                        FilterChip(
                            selected = settings.fontWeight == weight,
                            onClick = { /* set weight */ },
                            label = { Text(weight) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stroke
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("描边", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.strokeEnabled,
                        onCheckedChange = { /* toggle stroke */ }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Merge settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("弹幕合并", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.mergeEnabled,
                        onCheckedChange = { /* toggle merge */ }
                    )
                }
                
                if (settings.mergeEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("合并窗口: ${settings.mergeWindow}s", 
                        style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = settings.mergeWindow.toFloat(),
                        onValueChange = { /* update merge window */ },
                        valueRange = 1f..10f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save button
            Button(
                onClick = { 
                    /* save settings */
                    onDismiss() 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}

data class DanmakuSettings(
    val enabled: Boolean = true,
    val opacity: Float = 1.0f,
    val fontSize: Float = 25f,
    val speed: Float = 1.0f,
    val areas: Set<String> = setOf("顶部", "底部", "滚动"),
    val fontWeight: String = "常规",
    val strokeEnabled: Boolean = true,
    val mergeEnabled: Boolean = true,
    val mergeWindow: Float = 5f
)
