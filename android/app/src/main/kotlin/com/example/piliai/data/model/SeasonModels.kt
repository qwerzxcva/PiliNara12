package com.example.piliai.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** 批次L27：UP 主合集/系列（/x/polymer/web-space/seasons_series_list，匿名可用） */
@Serializable
data class SeasonsSeriesResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: SeasonsSeriesData? = null
)

@Serializable
data class SeasonsSeriesData(
    @SerialName("items_lists") val itemsLists: SeasonsSeriesItems? = null
)

@Serializable
data class SeasonsSeriesItems(
    @SerialName("seasons_list") val seasonsList: List<SeasonSeriesEntry> = emptyList(),
    @SerialName("series_list") val seriesList: List<SeasonSeriesEntry> = emptyList()
)

@Serializable
data class SeasonSeriesEntry(
    val meta: SeasonSeriesMeta? = null,
    val archives: List<SeasonArchiveItem> = emptyList(),
    val recent_aids: List<Long> = emptyList()
)

@Serializable
data class SeasonSeriesMeta(
    val category: Int = 0,
    val cover: String = "",
    val description: String = "",
    val mid: Long = 0L,
    val name: String = "",
    @SerialName("season_id") val seasonId: Long = 0L,
    @SerialName("series_id") val seriesId: Long = 0L,
    val total: Int = 0,
    val title: String = ""
)

@Serializable
data class SeasonArchiveItem(
    val aid: Long = 0L,
    val bvid: String = "",
    val cid: Long = 0L,
    val title: String = "",
    val pic: String = "",
    val duration: Long = 0L,
    val pubdate: Long = 0L
)

/** 合集内视频列表（/x/polymer/web-space/seasons_archives_list） */
@Serializable
data class SeasonArchivesResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: SeasonArchivesData? = null
)

@Serializable
data class SeasonArchivesData(
    val archives: List<SeasonArchiveItem> = emptyList(),
    val meta: SeasonSeriesMeta? = null,
    @SerialName("page_num") val pageNum: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val total: Int = 0
)
