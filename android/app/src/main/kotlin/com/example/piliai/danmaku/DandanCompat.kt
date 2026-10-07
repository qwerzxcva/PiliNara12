package com.example.piliai.danmaku

import com.example.piliai.data.model.ParsedDanmaku

/**
 * 将 [DandanComment] 转换为 PiliAI 内部 [ParsedDanmaku] 格式。
 *
 * 弹幕类型映射（DanDan → PiliAI）：
 * - DanDan type=1 (滚动) → mode=1, reverse=false
 * - DanDan type=2 (顶部) → mode=2
 * - DanDan type=3 (底部) → mode=3
 * - DanDan type=4 (字幕) → mode=4
 *
 * 颜色：DanDan 返回十进制（如 7706950），需转成 Android ARGB（0xFFRRGGBB）。
 */
fun DandanComment.toParsedDanmaku(): ParsedDanmaku = ParsedDanmaku(
    mode = when (type) {
        2 -> 2   // 顶
        3 -> 3   // 底
        4 -> 4   // 字幕
        else -> 1 // 滚动（含 reverse）
    },
    fontSize = 25, // DanDan 不提供字号，用默认
    color = color and 0x00FFFFFF or 0xFF000000.toInt(), // 转 ARGB
    timestamp = time.toFloat(),
    pool = 0,
    uid = 0L, // DanDan 弹幕不携带用户 ID
    content = message,
)
