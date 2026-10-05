package com.example.pilinara.ui.pages.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.data.model.RelationStat
import com.example.pilinara.data.model.SpaceInfo
import com.example.pilinara.data.model.SpaceVideoItem
import com.example.pilinara.data.remote.AccountSession
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UP 主空间 ViewModel（批次C）
 * 空间信息 + 粉丝/关注数 + 投稿列表（分页） + 关注切换
 */
class MemberViewModel(private val mid: Long) : ViewModel() {

    private val api = BiliApiClient()

    private val _state = MutableStateFlow(MemberState(mid = mid))
    val state: StateFlow<MemberState> = _state.asStateFlow()

    data class MemberState(
        val mid: Long = 0L,
        val info: SpaceInfo? = null,
        val stat: RelationStat? = null,
        val videos: List<SpaceVideoItem> = emptyList(),
        val page: Int = 1,
        val hasMore: Boolean = false,
        val order: String = "pubdate",   // pubdate / click
        val isFollowing: Boolean = false,
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        // 批次L24：专栏
        val articles: List<com.example.pilinara.data.model.SpaceArticleItem> = emptyList(),
        val articlePage: Int = 1,
        val articleHasMore: Boolean = false,
        val articlesLoading: Boolean = false,
        // 批次L27：合集/系列
        val seasons: List<com.example.pilinara.data.model.SeasonSeriesEntry> = emptyList(),
        val seasonsLoading: Boolean = false,
        // 当前打开合集的视频（展开显示）
        val seasonVideos: List<com.example.pilinara.data.model.SeasonArchiveItem> = emptyList(),
        val seasonVideosTotal: Int = 0,
        val openSeasonMeta: com.example.pilinara.data.model.SeasonSeriesMeta? = null,
        val seasonVideoPage: Int = 1,
        val seasonVideosLoading: Boolean = false
    )

    init {
        loadInfo()
        loadVideos(1)
    }

    fun loadInfo() {
        viewModelScope.launch {
            api.getSpaceInfo(mid).onSuccess { resp ->
                if (resp.code == 0) {
                    _state.value = _state.value.copy(info = resp.data)
                } else {
                    _state.value = _state.value.copy(error = resp.message.ifEmpty { "空间信息加载失败" })
                }
            }
            api.getRelationStat(mid).onSuccess { resp ->
                if (resp.code == 0) {
                    _state.value = _state.value.copy(stat = resp.data)
                }
            }
        }
    }

    fun loadVideos(page: Int) {
        if (page == 1) {
            _state.value = _state.value.copy(isLoading = true, error = null)
        } else {
            _state.value = _state.value.copy(isLoadingMore = true)
        }
        viewModelScope.launch {
            api.getSpaceArchives(mid, page, _state.value.order).onSuccess { resp ->
                if (resp.code == 0) {
                    val list = resp.data?.list?.vlist.orEmpty()
                    val total = resp.data?.page?.count ?: 0L
                    val merged = if (page == 1) list else _state.value.videos + list
                    _state.value = _state.value.copy(
                        isLoading = false, isLoadingMore = false,
                        videos = merged, page = page,
                        hasMore = merged.size < total
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false, isLoadingMore = false,
                        error = resp.message.ifEmpty { "投稿加载失败" }
                    )
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false, isLoadingMore = false, error = e.message
                )
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.hasMore) return
        loadVideos(s.page + 1)
    }

    /** 批次L24：加载专栏列表（分页） */
    fun loadArticles(page: Int = 1) {
        if (_state.value.articlesLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(articlesLoading = true)
            api.getSpaceArticles(mid, page)
                .onSuccess { resp ->
                    if (resp.code == 0) {
                        val list = resp.data?.articles.orEmpty()
                        val merged = if (page == 1) list else _state.value.articles + list
                        _state.value = _state.value.copy(
                            articles = merged, articlePage = page,
                            articleHasMore = merged.size < (resp.data?.count ?: 0),
                            articlesLoading = false
                        )
                    } else _state.value = _state.value.copy(articlesLoading = false)
                }
                .onFailure { _state.value = _state.value.copy(articlesLoading = false) }
        }
    }

    /** 批次L27：加载合集/系列列表 */
    fun loadSeasons() {
        if (_state.value.seasonsLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(seasonsLoading = true)
            api.getSeasonsSeries(mid)
                .onSuccess { resp ->
                    if (resp.code == 0) {
                        val all = (resp.data?.itemsLists?.seasonsList.orEmpty() +
                            resp.data?.itemsLists?.seriesList.orEmpty())
                        _state.value = _state.value.copy(seasons = all, seasonsLoading = false)
                    } else _state.value = _state.value.copy(seasonsLoading = false)
                }
                .onFailure { _state.value = _state.value.copy(seasonsLoading = false) }
        }
    }

    /** 批次L27：打开合集（拉取视频列表第一页） */
    fun openSeason(entry: com.example.pilinara.data.model.SeasonSeriesEntry) {
        val meta = entry.meta ?: return
        if (meta.seasonId == 0L && meta.seriesId == 0L) return
        viewModelScope.launch {
            _state.value = _state.value.copy(seasonVideosLoading = true, openSeasonMeta = meta,
                seasonVideos = emptyList(), seasonVideoPage = 1)
            if (meta.seasonId > 0L) {
                api.getSeasonArchives(mid, meta.seasonId)
                    .onSuccess { resp ->
                        if (resp.code == 0) _state.value = _state.value.copy(
                            seasonVideos = resp.data?.archives.orEmpty(),
                            seasonVideosTotal = resp.data?.total ?: 0,
                            seasonVideosLoading = false
                        ) else _state.value = _state.value.copy(seasonVideosLoading = false)
                    }
                    .onFailure { _state.value = _state.value.copy(seasonVideosLoading = false) }
            } else {
                // series 走 seasons_archives_list 的 series_id 参数（同接口 series_id 别名）
                api.getSeriesArchives(mid, meta.seriesId)
                    .onSuccess { resp ->
                        if (resp.code == 0) _state.value = _state.value.copy(
                            seasonVideos = resp.data?.archives.orEmpty(),
                            seasonVideosTotal = resp.data?.total ?: 0,
                            seasonVideosLoading = false
                        ) else _state.value = _state.value.copy(seasonVideosLoading = false)
                    }
                    .onFailure { _state.value = _state.value.copy(seasonVideosLoading = false) }
            }
        }
    }

    fun closeSeason() {
        _state.value = _state.value.copy(openSeasonMeta = null, seasonVideos = emptyList())
    }

    fun setOrder(order: String) {
        if (_state.value.order == order) return
        _state.value = _state.value.copy(order = order, videos = emptyList())
        loadVideos(1)
    }

    fun toggleFollow() {
        if (!AccountSession.isLogin) {
            _state.value = _state.value.copy(error = "请先登录")
            return
        }
        val target = !_state.value.isFollowing
        viewModelScope.launch {
            api.modifyFollow(mid, target).onSuccess { ok ->
                if (ok) {
                    _state.value = _state.value.copy(isFollowing = target)
                    loadInfo()  // 刷新粉丝数
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }
}
