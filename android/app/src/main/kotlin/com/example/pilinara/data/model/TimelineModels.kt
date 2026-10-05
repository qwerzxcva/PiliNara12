package com.example.pilinara.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** 批次L30：番剧时间表 /pgc/web/timeline（匿名可用，实测 code 0） */
@Serializable
data class TimelineResponse(
    val code: Int = 0,
    val message: String? = null,
    val result: List<TimelineDay> = emptyList()
)

@Serializable
data class TimelineDay(
    val date: String = "",
    @SerialName("date_ts") val dateTs: Long = 0L,
    @SerialName("day_of_week") val dayOfWeek: Int = 0,
    val episodes: List<TimelineEp> = emptyList()
)

@Serializable
data class TimelineEp(
    val season_id: Int = 0,
    val episode_id: Int = 0,
    val title: String = "",
    @SerialName("season_title") val seasonTitle: String = "",
    val cover: String = "",
    val pub_index: String = "",
    @SerialName("pub_time") val pubTime: String = "",
    val delay: Int = 0,
    @SerialName("delay_reason") val delayReason: String? = null
)
