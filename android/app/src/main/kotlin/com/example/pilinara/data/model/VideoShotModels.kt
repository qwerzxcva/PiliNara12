package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/**
 * 批次L3：storyboard 视频进度条缩略图预览
 * API: /x/player/videoshot?bvid=&cid=&index=1
 * image:雪碧图 URL 列表（每张 imgXLen×imgYLen 格，每格 imgXSize×imgYSize px）
 * index:每秒对应的雪碧图格序号
 */
@Serializable
data class VideoShotResponse(
    val code: Int = 0,
    val message: String = "",
    val data: VideoShotData? = null
)

@Serializable
data class VideoShotData(
    @kotlinx.serialization.SerialName("img_x_len") val imgXLen: Int = 0,
    @kotlinx.serialization.SerialName("img_y_len") val imgYLen: Int = 0,
    @kotlinx.serialization.SerialName("img_x_size") val imgXSize: Float = 0f,
    @kotlinx.serialization.SerialName("img_y_size") val imgYSize: Float = 0f,
    val image: List<String> = emptyList(),
    val index: List<Int> = emptyList()
) {
    val totalPerImage: Int get() = imgXLen * imgYLen

    /** 按播放秒数取 (雪碧图URL, 格子x, 格子y)；index[k] = 第k秒的格序号 */
    fun frameAt(second: Long): Triple<String, Int, Int>? {
        if (index.isEmpty() || image.isEmpty() || imgXLen <= 0 || imgYLen <= 0) return null
        val s = second.toInt().coerceIn(0, index.size - 1)
        val cell = index[s]
        if (cell <= 0) return null
        val imgIdx = (cell - 1) / totalPerImage
        val within = (cell - 1) % totalPerImage
        if (imgIdx !in image.indices) return null
        val col = within % imgXLen
        val row = within / imgXLen
        val url = image[imgIdx].let { if (it.startsWith("//")) "https:$it" else it }
        return Triple(url, col, row)
    }
}
