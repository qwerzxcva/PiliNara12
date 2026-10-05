package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/** 批次L40：播放页实时在线人数 /x/player/online/total（匿名可用） */
@Serializable
data class OnlineTotalResponse(
    val code: Int = 0,
    val message: String = "",
    val data: OnlineTotalData? = null
)

@Serializable
data class OnlineTotalData(
    val total: String = "0",
    val count: String = "0"
)
