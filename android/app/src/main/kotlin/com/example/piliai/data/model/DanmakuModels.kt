package com.example.piliai.data.model

import kotlinx.serialization.Serializable

/**
 * Danmaku models for Bilibili API
 */

@Serializable
data class DanmakuResponse(
    val code: Int = 0,
    val data: List<DanmakuElement>? = null
)

@Serializable
data class DanmakuElement(
    val p: String = "",  // params: mode;fontsize;color;timestamp;pool;userid;rawcode
    val m: String = "",  // rendered text
    val c: String = ""   // comma-separated params
)

data class ParsedDanmaku(
    val mode: Int = 1,      // 1=scroll, 2=top, 3=bottom, 4=subtitle, 5=reverse
    val fontSize: Int = 25,
    val color: Int = 0xFFFFFF,
    val timestamp: Float = 0f,
    val pool: Int = 0,
    val uid: Long = 0L,
    val content: String = ""
)

fun DanmakuElement.toParsed(): ParsedDanmaku {
    val parts = c.split(";")
    return ParsedDanmaku(
        mode = parts.getOrNull(0)?.toIntOrNull() ?: 1,
        fontSize = parts.getOrNull(1)?.toIntOrNull() ?: 25,
        color = parts.getOrNull(2)?.toIntOrNull() ?: 0xFFFFFF,
        timestamp = parts.getOrNull(3)?.toFloatOrNull() ?: 0f,
        pool = parts.getOrNull(4)?.toIntOrNull() ?: 0,
        uid = parts.getOrNull(5)?.toLongOrNull() ?: 0L,
        content = m
    )
}
