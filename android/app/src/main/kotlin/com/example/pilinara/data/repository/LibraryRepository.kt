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

    /** 某个收藏夹内的视频 */
    suspend fun favResources(mediaId: Long, pn: Int = 1, ps: Int = 20): Result<FavResourceListResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.getFavoritesRaw("x/v3/fav/resource/list") {
                    append("media_id", mediaId.toString())
                    append("pn", pn.toString())
                    append("ps", ps.toString())
                }
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
}
