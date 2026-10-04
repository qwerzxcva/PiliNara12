package com.example.pilinara.data.remote

import com.example.pilinara.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

/**
 * Bilibili API Client using Ktor
 */
class BiliApiClient(private val client: HttpClient = BiliHttpClient.client) {
    
    companion object {
        private const val API_BASE = "https://api.bilibili.com"
        private const val WBI_BASE = "https://api.bilibili.com/x/web-interface/nav"
        
        private val commonHeaders = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
    }
    
    // ========== Popular Videos ==========
    
    suspend fun popularVideos(page: Int = 1, pageSize: Int = 20): Result<PopularResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/popular") {
            url {
                parameters.append("ps", pageSize.toString())
                parameters.append("pn", page.toString())
            }
            commonHeaders.forEach { (k, v) -> header(k, v) }
        }.body()
    }
    
    // ========== Video Info ==========
    
    suspend fun getVideoInfo(bvid: String): Result<VideoInfoResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/view") {
            url {
                parameters.append("bvid", bvid)
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== Play URL ==========
    
    suspend fun getPlayUrl(bvid: String, cid: Long, qn: Int = 80): Result<PlayUrlResponse> = runCatching {
        // playurl 必须 wbi 签名，否则 -404/-352（见 docs/bilibili_api_checklist.md §1.2）
        val signed = WbiSigner.sign(
            mapOf(
                "bvid" to bvid,
                "cid" to cid.toString(),
                "qn" to qn.toString(),
                "fnval" to "4048", // DASH + HDR/杜比/8K 等全部位（对齐 PiliPala/PiliPlus）
                "fnver" to "0",
                "fourk" to "1",
                "try_look" to "1"  // 免登录试看（提升匿名可播概率）
            )
        )
        client.get("$API_BASE/x/player/wbi/playurl") {
            url {
                signed.forEach { (k, v) -> parameters.append(k, v) }
            }
            header("Referer", "https://www.bilibili.com")
        }.bodyAsText().let { text ->
            Json { ignoreUnknownKeys = true }
                .decodeFromString<PlayUrlResponse>(text)
                .copy(rawJson = text)
        }
    }
    
    // ========== Write operations（需登录 + csrf） ==========

    /** 点赞/取消点赞（like=1 点赞, 2 取消；返回 code 0 成功） */
    suspend fun like(aid: Long, like: Int = 1): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/web-interface/archive/like",
            mapOf("aid" to aid.toString(), "like" to like.toString())
        )
    }

    /** 投币（multiply=1/2 个币，需先 like=1 一起勾选可传） */
    suspend fun coin(aid: Long, multiply: Int = 1): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/web-interface/coin/add",
            mapOf("aid" to aid.toString(), "multiply" to multiply.toString(), "select_like" to "0")
        )
    }

    /** 收藏/取消收藏到默认收藏夹（需要先知道 target mid 的默认夹 id；deal=1 收藏 2 取消） */
    suspend fun favorite(aid: Long, mediaId: Long, deal: Int = 1): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/v3/fav/resource/deal",
            mapOf(
                "rid" to aid.toString(),
                "type" to "2",
                "add_media_ids" to if (deal == 1) mediaId.toString() else "",
                "del_media_ids" to if (deal == 2) mediaId.toString() else ""
            )
        )
    }

    /** 上报观看历史（progress 秒；sid/cid 可选） */
    suspend fun reportHistory(aid: Long, cid: Long, progress: Long): Result<Map<String, Any>> = runCatching {
        BiliHttpClient.postAuthForm<Map<String, Any>>(
            "https://api.bilibili.com/x/v2/history/report",
            mapOf(
                "aid" to aid.toString(),
                "cid" to cid.toString(),
                "progress" to progress.toString(),
                "type" to "3"
            )
        )
    }

    // ========== Search ==========
    
    suspend fun search(keyword: String, page: Int = 1, order: String = "totalrank"): Result<SearchResponse> = runCatching {
        // wbi 签名接口；缺 buvid3 会 -412 风控（见 docs/bilibili_api_checklist.md）
        AccountSession.ensureBuvid()
        val signed = WbiSigner.sign(
            mapOf("keyword" to keyword, "page" to page.toString(), "order" to order)
        )
        client.get("$API_BASE/x/web-interface/wbi/search/all/v2") {
            url { signed.forEach { (k, v) -> parameters.append(k, v) } }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    suspend fun searchSuggest(keyword: String): Result<SearchSuggest> = runCatching {
        client.get("$API_BASE/suggestion/search") {
            url {
                parameters.append("keyword", keyword)
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== Comments ==========
    
    suspend fun getComments(bvid: String, oid: Long = 0L, page: Int = 1, pageSize: Int = 20): Result<CommentResponse> = runCatching {
        // /x/v2/reply/wbi/main 需 wbi 签名，签名错返回 -403（见 docs §1.5）
        val signed = WbiSigner.sign(
            mapOf(
                "oid" to oid.toString(),
                "type" to "1",
                "mode" to "3",
                "pn" to page.toString(),
                "ps" to pageSize.toString()
            )
        )
        client.get("$API_BASE/x/v2/reply/wbi/main") {
            url { signed.forEach { (k, v) -> parameters.append(k, v) } }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 楼中楼回复 /x/v2/reply/reply（无需 wbi） */
    suspend fun getReplyList(oid: Long, rootRpid: Long, page: Int = 1, pageSize: Int = 20): Result<ReplyListResponse> = runCatching {
        client.get("$API_BASE/x/v2/reply/reply") {
            url {
                parameters.append("oid", oid.toString())
                parameters.append("type", "1")
                parameters.append("root", rootRpid.toString())
                parameters.append("pn", page.toString())
                parameters.append("ps", pageSize.toString())
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 评论点赞 /x/v2/reply/action（csrf）。like: 1 点赞 / 0 取消 */
    suspend fun likeComment(oid: Long, rpid: Long, like: Boolean): Result<Boolean> = runCatching {
        val form = linkedMapOf(
            "oid" to oid.toString(),
            "type" to "1",
            "rpid" to rpid.toString(),
            "action" to if (like) "1" else "0"
        )
        val resp: String = BiliHttpClient.postAuthForm("$API_BASE/x/v2/reply/action", form)
        org.json.JSONObject(resp).optInt("code") == 0
    }

    /** 发评论 /x/v2/reply/add（csrf） */
    suspend fun addComment(oid: Long, message: String, rootRpid: Long = 0L, parentRpid: Long = 0L): Result<Boolean> = runCatching {
        val form = linkedMapOf(
            "oid" to oid.toString(),
            "type" to "1",
            "message" to message,
            "plat" to "1",
            "web_location" to "1315875"
        )
        if (rootRpid > 0L) form["root"] = rootRpid.toString()
        if (parentRpid > 0L) form["parent"] = parentRpid.toString()
        val resp: String = BiliHttpClient.postAuthForm("$API_BASE/x/v2/reply/add", form)
        org.json.JSONObject(resp).optInt("code") == 0
    }
    
    // ========== Danmaku ==========
    
    suspend fun getDanmaku(cid: Long, oid: Long = 0L): Result<DanmakuResponse> = runCatching {
        client.get("$API_BASE/x/v1/dm/list.so") {
            url {
                parameters.append("oid", oid.toString())
                parameters.append("pid", cid.toString())
                parameters.append("type", "1")
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }
    
    // ========== User Info ==========
    
    suspend fun getUserInfo(uid: Long): Result<UserInfoResponse> = runCatching {
        client.get("$API_BASE/x/space/wbi/acc/info") {
            url {
                parameters.append("mid", uid.toString())
            }
            header("Referer", "https://space.bilibili.com")
        }.body()
    }
    
    suspend fun getUserSpace(uid: Long): Result<UserInfoResponse> = runCatching {
        client.get("$API_BASE/x/space/acc/info") {
            url {
                parameters.append("mid", uid.toString())
            }
            header("Referer", "https://space.bilibili.com")
        }.body()
    }
    
    // ========== Dynamics ==========
    
    suspend fun getUserDynamics(uid: Long, offset: Long = 0L): Result<DynamicsResponse> = runCatching {
        client.get("$API_BASE/x/polymer/web-dynamic/v1/feed/space") {
            url {
                parameters.append("host_uid", uid.toString())
                parameters.append("offset", offset.toString())
            }
            header("Referer", "https://t.bilibili.com")
        }.body()
    }
    
    suspend fun getFollowingsFeed(offset: Long = 0L): Result<DynamicsResponse> = runCatching {
        client.get("$API_BASE/x/polymer/web-dynamic/v1/feed/all") {
            url {
                parameters.append("offset", offset.toString())
            }
            header("Referer", "https://t.bilibili.com")
        }.body()
    }
    
    // ========== Live ==========
    
    suspend fun getLiveInfo(roomId: Long): Result<Map<String, Any>> = runCatching {
        client.get("$API_BASE/room/v1/Room/get_info") {
            url {
                parameters.append("room_id", roomId.toString())
            }
            header("Referer", "https://live.bilibili.com")
        }.body()
    }
    
    // ========== Favorites ==========

    suspend fun getFavorites(uid: Long, pageSize: Int = 20, mediaType: String = "video"): Result<Map<String, Any>> = runCatching {
        client.get("$API_BASE/x/v3/fav/folder/created/list-all") {
            url {
                parameters.append("up_mid", uid.toString())
                parameters.append("media_type", mediaType)
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 当前用户收藏夹列表（需登录，用于取默认收藏夹 id） */
    suspend fun getMyFavFolders(): Result<List<FavFolder>> = runCatching {
        val mid = AccountSession.mid
        require(mid > 0L) { "未登录" }
        val resp: FavFolderListResponse = client.get("$API_BASE/x/v3/fav/folder/created/list-all") {
            url { parameters.append("up_mid", mid.toString()) }
            header("Referer", "https://www.bilibili.com")
        }.body()
        resp.data.orEmpty()
    }

    /** 查询视频交互状态（like=1 已赞, coin=1 已投币, favourite=1 已藏；需登录） */
    suspend fun getVideoRelation(aid: Long): Result<Triple<Boolean, Boolean, Boolean>> = runCatching {
        val resp: RelationResponse = client.get("$API_BASE/x/web-interface/archive/relation") {
            url { parameters.append("aid", aid.toString()) }
            header("Referer", "https://www.bilibili.com")
        }.body()
        val d = resp.data
        if (resp.code == 0 && d != null) {
            Triple(d.like == 1, d.coin == 1, d.favourite == 1)
        } else error(resp.message.ifEmpty { "查询交互状态失败" })
    }

    // ========== Dynamic（动态，需登录） ==========

    /**
     * 动态聚合流。首次传 offset=null，后续用上一页返回的 data.offset 翻页。
     * 需 SESSDATA（未登录返回 -101）。
     */
    suspend fun getDynamicFeed(offset: String? = null): Result<DynamicFeedResponse> = runCatching {
        client.get("$API_BASE/x/polymer/web-dynamic/v1/feed/all") {
            url {
                parameters.append("type", "all")
                if (!offset.isNullOrEmpty()) parameters.append("offset", offset)
                parameters.append("web_location", "333.1369")
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    // ========== Video extras（批次A：播放器补全） ==========

    /** 相关视频推荐（无需登录） */
    suspend fun getRelatedVideos(bvid: String): Result<List<RelatedVideo>> = runCatching {
        val resp: RelatedVideoResponse = client.get("$API_BASE/x/web-interface/archive/related") {
            url { parameters.append("bvid", bvid) }
            header("Referer", "https://www.bilibili.com")
        }.body()
        if (resp.code == 0) resp.data.orEmpty() else error(resp.message.ifEmpty { "相关视频获取失败" })
    }

    // ========== Search extras（批次B） ==========

    /** 热搜榜（无需登录） */
    suspend fun getSearchTrending(): Result<List<TrendingItem>> = runCatching {
        val resp: SearchTrendingResponse = client.get("$API_BASE/x/v2/search/trending/ranking") {
            url { parameters.append("limit", "20") }
            header("Referer", "https://search.bilibili.com")
        }.body()
        if (resp.code == 0) {
            (resp.data?.top_list.orEmpty()) + resp.data?.list.orEmpty()
        } else error(resp.message.ifEmpty { "热搜获取失败" })
    }

    /**
     * 分类搜索：video（视频）/ bili_user（用户）/ live（直播）。
     * order: totalrank(默认)/click/pubdate/danmaku/stow
     * duration: 0全部/10分钟内/30分钟内/60分钟内
     * 需要 wbi 签名 + buvid3。
     */
    suspend fun searchByType(
        keyword: String,
        searchType: String = "video",
        page: Int = 1,
        order: String = "",
        duration: Int = 0
    ): Result<SearchResultResponse> = runCatching {
        AccountSession.ensureBuvid()
        val params = mutableMapOf(
            "search_type" to searchType,
            "keyword" to keyword,
            "page" to page.toString(),
            "page_size" to "20"
        )
        if (order.isNotEmpty()) params["order"] = order
        if (duration > 0) params["duration"] = duration.toString()
        val signed = WbiSigner.sign(params)
        client.get("$API_BASE/x/web-interface/wbi/search/type") {
            url { signed.forEach { (k, v) -> parameters.append(k, v) } }
            header("Referer", "https://search.bilibili.com")
        }.body()
    }

    /**
     * 通用 GET：给路径 + 追加查询参数，反序列化为 reified T。
     * 用于收藏夹/历史等带具体模型的新接口，避免每个都加 wrapper。
     * 注意：inline 函数不能访问私有成员，故直接用公开的 BiliHttpClient.client。
     */
    suspend inline fun <reified T : Any> getFavoritesRaw(
        path: String,
        noinline params: io.ktor.http.ParametersBuilder.() -> Unit = {}
    ): T = BiliHttpClient.client.get("https://api.bilibili.com/$path") {
        // URLBuilder.parameters 就是 ParametersBuilder，应用 lambda
        url.parameters.apply(params)
        header("Referer", "https://www.bilibili.com")
    }.body()

    // ========== UP 主空间（批次C） ==========

    /** 空间主页信息（wbi 签名） */
    suspend fun getSpaceInfo(mid: Long): Result<SpaceInfoResponse> = runCatching {
        val signed = WbiSigner.sign(mapOf("mid" to mid.toString()))
        client.get("$API_BASE/x/space/wbi/acc/info") {
            url { signed.forEach { (k, v) -> parameters.append(k, v) } }
            header("Referer", "https://space.bilibili.com/$mid")
        }.body<SpaceInfoResponse>()
    }

    /** 粉丝/关注数（无需签名） */
    suspend fun getRelationStat(mid: Long): Result<RelationStatResponse> = runCatching {
        client.get("$API_BASE/x/relation/stat") {
            url { parameters.append("vmid", mid.toString()) }
            header("Referer", "https://space.bilibili.com/$mid")
        }.body<RelationStatResponse>()
    }

    /** 投稿列表（wbi 签名 + 分页），order: pubdate 最新 / click 最多播放 */
    suspend fun getSpaceArchives(
        mid: Long,
        page: Int = 1,
        order: String = "pubdate",
        keyword: String = ""
    ): Result<SpaceArchiveResponse> = runCatching {
        val params = buildMap {
            put("mid", mid.toString())
            put("pn", page.toString())
            put("ps", "30")
            put("order", order)
            if (keyword.isNotEmpty()) put("keyword", keyword)
        }
        val signed = WbiSigner.sign(params)
        client.get("$API_BASE/x/space/wbi/arc/search") {
            url { signed.forEach { (k, v) -> parameters.append(k, v) } }
            header("Referer", "https://space.bilibili.com/$mid")
        }.body<SpaceArchiveResponse>()
    }

    /** 关注/取关 UP 主（csrf）。act: 1 关注 / 2 取关 */
    suspend fun modifyFollow(mid: Long, follow: Boolean): Result<Boolean> = runCatching {
        val form = linkedMapOf(
            "fid" to mid.toString(),
            "act" to if (follow) "1" else "2",
            "re_src" to "11"
        )
        val resp: String = BiliHttpClient.postAuthForm(
            "$API_BASE/x/relation/modify", form
        )
        org.json.JSONObject(resp).optInt("code") == 0
    }

    /** 直播列表（分页，按人气） */
    suspend fun getLiveList(page: Int = 1, pageSize: Int = 30): Result<LiveListResponse> = runCatching {
        client.get("https://api.live.bilibili.com/xlive/app-interface/v2/second/getList") {
            url {
                parameters.append("platform", "web")
                parameters.append("parent_area_id", "0")
                parameters.append("area_id", "0")
                parameters.append("sort_type", "")
                parameters.append("page", page.toString())
                parameters.append("page_size", pageSize.toString())
            }
            header("Referer", "https://live.bilibili.com")
        }.body()
    }

    // ========== 番剧/影视（批次D） ==========

    /** 番剧详情（season_id 或 ep_id）。注意：pgc 接口在 api.bilibili.com，返回字段直属 result */
    suspend fun getPgcSeason(seasonId: Long = 0L, epId: Long = 0L): Result<PgcSeasonResponse> = runCatching {
        client.get("$API_BASE/pgc/view/web/season") {
            url {
                if (seasonId > 0) parameters.append("season_id", seasonId.toString())
                if (epId > 0) parameters.append("ep_id", epId.toString())
            }
            header("Referer", "https://www.bilibili.com/bangumi/")
        }.body()
    }

    /** pgc 索引分类列表（批次L2）：season_type 1番剧 2电影 3纪录片 4国创 5电视剧 7综艺 */
    suspend fun getPgcIndex(
        seasonType: Int,
        page: Int,
        order: String = "update"
    ): Result<PgcIndexResponse> = runCatching {
        client.get("$API_BASE/pgc/season/index/result") {
            url {
                parameters.append("season_type", seasonType.toString())
                parameters.append("type", "1")
                parameters.append("order", order)
                parameters.append("page", page.toString())
                parameters.append("pagesize", "20")
            }
            header("Referer", "https://www.bilibili.com/")
        }.body()
    }

    /** 进度条缩略图雪碧图（批次L3 storyboard） */
    suspend fun getVideoShot(bvid: String, cid: Long): Result<VideoShotResponse> = runCatching {
        client.get("$API_BASE/x/player/videoshot") {
            url {
                parameters.append("bvid", bvid)
                parameters.append("cid", cid.toString())
                parameters.append("index", "1")
            }
            header("Referer", "https://www.bilibili.com/")
        }.body()
    }

    /** 番剧播放地址（pgc playurl，ep_id + cid） */
    suspend fun getPgcPlayUrl(epId: Long, cid: Long, qn: Int = 80): Result<PgcPlayUrlResponse> = runCatching {
        val signed = WbiSigner.sign(
            mapOf(
                "ep_id" to epId.toString(),
                "cid" to cid.toString(),
                "qn" to qn.toString(),
                "fnval" to "4048",
                "fnver" to "0",
                "fourk" to "1"
            )
        )
        client.get("$API_BASE/pgc/player/web/playurl") {
            url { signed.forEach { (k, v) -> parameters.append(k, v) } }
            header("Referer", "https://www.bilibili.com/bangumi/")
        }.body()
    }

    /** 追番/取消追番（csrf）。season_id 或 ep_id 二选一 */
    suspend fun followBangumi(seasonId: Long = 0L, epId: Long = 0L, follow: Boolean): Result<Boolean> = runCatching {
        val form = linkedMapOf(
            "season_id" to seasonId.toString(),
            "ep_id" to epId.toString()
        )
        val path = if (follow) "$API_BASE/pgc/app/follow/add" else "$API_BASE/pgc/app/follow/del"
        val resp: String = BiliHttpClient.postAuthForm(path, form)
        org.json.JSONObject(resp).optInt("code") == 0
    }

    // ========== 关注/粉丝 + 消息（批次H） ==========

    /** 关注列表（需登录） */
    suspend fun getFollowings(mid: Long, page: Int = 1, pageSize: Int = 50): Result<FollowingsResponse> = runCatching {
        client.get("$API_BASE/x/relation/followings") {
            url {
                parameters.append("vmid", mid.toString())
                parameters.append("pn", page.toString())
                parameters.append("ps", pageSize.toString())
            }
            header("Referer", "https://space.bilibili.com/$mid")
        }.body()
    }

    /** 粉丝列表（需登录） */
    suspend fun getFollowers(mid: Long, page: Int = 1, pageSize: Int = 50): Result<FollowingsResponse> = runCatching {
        client.get("$API_BASE/x/relation/followers") {
            url {
                parameters.append("vmid", mid.toString())
                parameters.append("pn", page.toString())
                parameters.append("ps", pageSize.toString())
            }
            header("Referer", "https://space.bilibili.com/$mid")
        }.body()
    }

    /** 未读消息数（需登录） */
    suspend fun getMsgUnread(): Result<MsgUnreadResponse> = runCatching {
        client.get("$API_BASE/x/msgfeed/unread") {
            url {
                parameters.append("platform", "web")
                parameters.append("build", "0")
                parameters.append("mobi_app", "web")
            }
            header("Referer", "https://message.bilibili.com")
        }.body()
    }

    /** 回复我的（需登录） */
    suspend fun getMsgReply(page: Int = 1): Result<MsgFeedResponse> = runCatching {
        client.get("$API_BASE/x/msgfeed/reply") {
            url {
                parameters.append("platform", "web")
                parameters.append("build", "0")
                parameters.append("mobi_app", "web")
                parameters.append("pn", page.toString())
            }
            header("Referer", "https://message.bilibili.com")
        }.body()
    }

    /** @我的（需登录） */
    suspend fun getMsgAt(page: Int = 1): Result<MsgFeedResponse> = runCatching {
        client.get("$API_BASE/x/msgfeed/at") {
            url {
                parameters.append("platform", "web")
                parameters.append("build", "0")
                parameters.append("mobi_app", "web")
                parameters.append("pn", page.toString())
            }
            header("Referer", "https://message.bilibili.com")
        }.body()
    }

    /** 收到的赞（需登录） */
    suspend fun getMsgLike(page: Int = 1): Result<MsgFeedResponse> = runCatching {
        client.get("$API_BASE/x/msgfeed/like") {
            url {
                parameters.append("platform", "web")
                parameters.append("build", "0")
                parameters.append("mobi_app", "web")
                parameters.append("pn", page.toString())
            }
            header("Referer", "https://message.bilibili.com")
        }.body()
    }

    // ========== 直播（批次F） ==========

    /** 直播间播放信息（含 HLS/FLV 流地址）。roomId 支持短号 */
    suspend fun getLivePlayInfo(roomId: Long): Result<LivePlayInfoResponse> = runCatching {
        client.get("https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo") {
            url {
                parameters.append("room_id", roomId.toString())
                parameters.append("protocol", "0,1")     // 0=flv 1=hls
                parameters.append("format", "0,1,2")     // flv/ts/fmp4
                parameters.append("codec", "0,1")        // avc/hevc
                parameters.append("qn", "10000")         // 最高画质
            }
            header("Referer", "https://live.bilibili.com")
        }.body()
    }

    /** 直播间信息（标题/分区/人气） */
    suspend fun getLiveRoomInfo(roomId: Long): Result<LiveRoomInfoResponse> = runCatching {
        client.get("https://api.live.bilibili.com/xlive/web-interface/v1/index/getInfoByRoom") {
            url { parameters.append("room_id", roomId.toString()) }
            header("Referer", "https://live.bilibili.com")
        }.body()
    }

    /** 直播人气入口上报（进入直播间必须调，否则风控） */
    suspend fun liveRoomEntryAction(roomId: Long): Result<Boolean> = runCatching {
        val resp: String = BiliHttpClient.postAuthForm(
            "https://api.live.bilibili.com/xlive/web-room/v1/index/roomEntryAction",
            mapOf("room_id" to roomId.toString())
        )
        org.json.JSONObject(resp).optInt("code") == 0
    }

    // ========== 直播弹幕 WebSocket（批次J） ==========

    /** 弹幕连接信息：token + host 列表（wss://host:wssPort/sub） */
    suspend fun getDanmuInfo(roomId: Long): Result<DanmuInfoResponse> = runCatching {
        client.get("https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo") {
            url {
                parameters.append("id", roomId.toString())
                parameters.append("type", "0")
            }
            header("Referer", "https://live.bilibili.com/")
        }.body()
    }

    /** 发送直播弹幕（csrf） */
    suspend fun sendLiveDanmaku(roomId: Long, message: String): Result<Boolean> = runCatching {
        val resp: String = BiliHttpClient.postAuthForm(
            "https://api.live.bilibili.com/xlive/web-room/v1/index/SendMsg",
            linkedMapOf(
                "bubble" to "0",
                "msg" to message,
                "color" to "16777215",
                "mode" to "1",
                "fontsize" to "25",
                "rnd" to (System.currentTimeMillis() / 1000).toString(),
                "roomid" to roomId.toString(),
                "csrf_token" to ""
            )
        )
        org.json.JSONObject(resp).optInt("code") == 0
    }

    // ========== 播放器字幕（批次K） ==========

    /** /x/player/v2 —— 字幕列表等播放器附加信息 */
    suspend fun getPlayerV2(bvid: String, cid: Long): Result<PlayerV2Response> = runCatching {
        client.get("$API_BASE/x/player/v2") {
            url {
                parameters.append("bvid", bvid)
                parameters.append("cid", cid.toString())
            }
            header("Referer", "https://www.bilibili.com/")
        }.body()
    }

    /** 拉取字幕内容 JSON（subtitle_url，协议相对 // 开头） */
    suspend fun fetchSubtitleBody(url: String): Result<SubtitleBody> = runCatching {
        val full = if (url.startsWith("//")) "https:$url" else url
        client.get(full) {
            header("Referer", "https://www.bilibili.com/")
        }.body()
    }

    // ========== History / 稍后再看（批次G） ==========

    /** 观看历史（cursor 分页，需登录）。max/view_at 取上一页 cursor 传回 */
    suspend fun getHistoryCursor(
        max: Long = 0L,
        viewAt: Long = 0L,
        ps: Int = 20
    ): Result<HistoryResponse> = runCatching {
        client.get("$API_BASE/x/web-interface/history/cursor") {
            url {
                parameters.append("ps", ps.toString())
                if (max > 0) parameters.append("max", max.toString())
                if (viewAt > 0) parameters.append("view_at", viewAt.toString())
            }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 稍后再看列表 /x/v2/history/toview（需登录） */
    suspend fun getToView(): Result<ToViewResponse> = runCatching {
        client.get("$API_BASE/x/v2/history/toview") {
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    /** 添加稍后再看（csrf） */
    suspend fun addToView(aid: Long): Result<Boolean> = runCatching {
        val resp: String = BiliHttpClient.postAuthForm(
            "$API_BASE/x/v2/history/toview/add",
            mapOf("aid" to aid.toString())
        )
        org.json.JSONObject(resp).optInt("code") == 0
    }

    /** 删除稍后再看（csrf） */
    suspend fun delToView(aid: Long): Result<Boolean> = runCatching {
        val resp: String = BiliHttpClient.postAuthForm(
            "$API_BASE/x/v2/history/toview/del",
            mapOf("aid" to aid.toString())
        )
        org.json.JSONObject(resp).optInt("code") == 0
    }

    /** 删除单条历史（csrf）。kid = business:oid，如 archive:123456 */
    suspend fun delHistory(kid: String): Result<Boolean> = runCatching {
        val resp: String = BiliHttpClient.postAuthForm(
            "$API_BASE/x/v2/history/delete",
            mapOf("kid" to kid)
        )
        org.json.JSONObject(resp).optInt("code") == 0
    }
    
    // ========== 私聊会话（批次：私聊） ==========

    /** 私聊会话列表（Web API，需登录） */
    suspend fun getMySessions(endTs: Long = 0L): Result<SessionListResponse> = runCatching {
        client.get("$API_BASE/bili-api/v1/web/session/list") {
            url {
                parameters.append("session_type", "1")
                parameters.append("group_id", "0")
                parameters.append("build", "0")
                parameters.append("mobi_app", "web")
                if (endTs > 0) parameters.append("end_ts", endTs.toString())
            }
            header("Referer", "https://message.bilibili.com/")
        }.body()
    }

    /** 私聊消息记录（talker = 对方 mid） */
    suspend fun fetchSessionMsgs(talkerId: Long, sessionTs: Long = 0L): Result<SessionMsgsResponse> = runCatching {
        client.get("$API_BASE/bili-api/v1/web/session/poll") {
            url {
                parameters.append("talker_id", talkerId.toString())
                parameters.append("session_type", "1")
                parameters.append("build", "0")
                parameters.append("web_location", "0")
                if (sessionTs > 0) parameters.append("session_ts", sessionTs.toString())
            }
            header("Referer", "https://message.bilibili.com/")
        }.body()
    }

    /** 发送私聊消息（csrf）；sender_uid = 自己 mid */
    suspend fun sendPrivateMsg(senderUid: Long, receiverId: Long, content: String): Result<Boolean> = runCatching {
        val payload = org.json.JSONObject().put("content", content).toString()
        val resp: String = BiliHttpClient.postAuthForm(
            "https://api.vc.bilibili.com/web_im/v1/web_im/send_msg",
            linkedMapOf(
                "msg[sender_uid]" to senderUid.toString(),
                "msg[receiver_id]" to receiverId.toString(),
                "msg[receiver_type]" to "1",
                "msg[msg_type]" to "1",
                "msg[msg_status]" to "0",
                "msg[content]" to payload,
                "msg[timestamp]" to (System.currentTimeMillis() / 1000).toString(),
                "msg[new_device_token]" to "",
                "from_firework" to "0",
                "build" to "0",
                "mobi_app" to "web"
            )
        )
        org.json.JSONObject(resp).optInt("code") == 0
    }

    // ========== 表情包（批次：表情包） ==========

    /** 大表情包详情 /x/emote/package（1=小黄脸，244=小黄脸动态） */
    suspend fun getEmotePackage(id: Long): Result<EmotePackageResponse> = runCatching {
        client.get("$API_BASE/x/emote/package") {
            url { parameters.append("id", id.toString()) }
            header("Referer", "https://www.bilibili.com")
        }.body()
    }

    // ========== WBI Signature ==========
    
    suspend fun withWbi(url: String, params: Map<String, String>): String {
        // Simplified WBI signing - in production would fetch mixin key and sign
        return buildString {
            append(url)
            params.forEach { (k, v) ->
                append("&$k=$v")
            }
        }
    }
}
