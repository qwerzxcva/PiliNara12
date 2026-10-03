package com.example.pilinara.data.repository

import com.example.pilinara.data.model.VideoItem
import com.example.pilinara.data.remote.BiliApiClient

/**
 * 首页数据仓库：热门视频流（对应 Dart 侧 VideoServer.hotVideoList）
 * popular 接口按页返回，pn 从 1 开始
 */
class HomeRepository(
    private val api: BiliApiClient = BiliApiClient(),
) {
    private var currentPage = 0
    private val items = mutableListOf<VideoItem>()

    suspend fun refresh(): Result<List<VideoItem>> {
        currentPage = 1
        return api.popularVideos(page = currentPage).map { resp ->
            if (resp.code == 0) {
                items.clear()
                items += resp.data?.list.orEmpty()
                items.toList()
            } else {
                throw IllegalStateException(resp.message ?: "code=${resp.code}")
            }
        }
    }

    suspend fun loadMore(): Result<List<VideoItem>> {
        if (currentPage == 0) return refresh()
        val next = currentPage + 1
        return api.popularVideos(page = next).map { resp ->
            if (resp.code == 0) {
                currentPage = next
                val fresh = resp.data?.list.orEmpty().filter { new -> items.none { it.aid == new.aid } }
                items += fresh
                items.toList()
            } else {
                throw IllegalStateException(resp.message ?: "code=${resp.code}")
            }
        }
    }
}
