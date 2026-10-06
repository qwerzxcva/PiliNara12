package com.example.pilinara.data.repository

import com.example.pilinara.data.model.*
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 收藏夹 + 历史记录仓储
 */
class LibraryRepository(private val api: BiliApiClient = BiliApiClient()) {

    /** 用户的公开收藏夹列表 */
    suspend fun favFolders(mid: Long): Result<FavFolderListResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.getFavoritesRaw("x/v3/fav/folder/created/list-all") {
                    append("up_mid", mid.toString())
                }
            }
        }

    /** 某个收藏夹内的视频（带 pn 分页） */
    // 审核轮203：收藏夹排序（PiliPlus fav_sort；B站 API 原生参数）
    // order: "" 默认 / "view" 播放量 / "pubtime" 收藏时间倒序最新
    suspend fun favResources(mediaId: Long, pn: Int = 1, ps: Int = 20, order: String = ""): Result<FavResourceListResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.getFavoritesRaw("x/v3/fav/resource/list") {
                    append("media_id", mediaId.toString())
                    append("pn", pn.toString())
                    append("ps", ps.toString())
                    if (order.isNotBlank()) append("order", order)
                }
            }
        }

    /** 收藏夹内容无限分页（返回本页 items + 是否还有更多） */
    suspend fun favResourcesPage(
        mediaId: Long, pn: Int, ps: Int = 20, order: String = ""
    ): Result<Pair<List<com.example.pilinara.data.model.FavMedia>, Boolean>> =
        withContext(Dispatchers.IO) {
            favResources(mediaId, pn, ps, order).map { resp ->
                val items = resp.data?.medias.orEmpty()
                // has_more：本页满页即认为可能还有更多
                Pair(items, items.size >= ps)
            }
        }

    /** 观看历史（需登录 cookie） */
    suspend fun history(ps: Int = 20): Result<HistoryResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.getFavoritesRaw("x/web-interface/history/cursor") {
                    append("ps", ps.toString())
                }
            }
        }

    /** 批次L21：搜索观看历史（需登录 cookie） */
    suspend fun searchHistory(keyword: String, ps: Int = 20): Result<HistoryResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.getFavoritesRaw("x/web-interface/history/search") {
                    append("business", "archive")
                    append("keyword", keyword)
                    append("pn", "1")
                    append("ps", ps.toString())
                }
            }
        }
}
