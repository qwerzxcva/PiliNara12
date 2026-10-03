package com.example.pilinara.model

import kotlinx.serialization.Serializable

@Serializable
data class Danmaku(
    val mode: Int = 0,
    val fontsize: Int = 0,
    val color: Int = 0,
    val timestamp: Float = 0f,
    val midHash: String = "",
    val content: String = "",
    val age: Long = 0L,
    val pool: Int = 0,
    val id: Long = 0L
) {
    fun displayColor(): String {
        val r = color and 0xFF
        val g = (color shr 8) and 0xFF
        val b = (color shr 16) and 0xFF
        return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
    }
}
