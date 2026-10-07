package com.example.piliai.data.model

import kotlinx.serialization.Serializable

/**
 * 批次L22：视频 AI 总结（/x/web-interface/view/conclusion/get，wbi+登录）
 */
@Serializable
data class AiConclusionResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: AiConclusionData? = null
)

@Serializable
data class AiConclusionData(
    val code: Int = 0,
    val model_result: AiModelResult? = null
)

@Serializable
data class AiModelResult(
    val result_type: Int = 0,
    val summary: String = "",
    val outline: List<AiOutline> = emptyList()
)

@Serializable
data class AiOutline(
    val title: String = "",
    val timestamp: Long = 0L,
    val end_timestamp: Long = 0L,
    val part_outline: List<AiPartOutline> = emptyList()
)

@Serializable
data class AiPartOutline(
    val content: String = "",
    val timestamp: Long = 0L
)
