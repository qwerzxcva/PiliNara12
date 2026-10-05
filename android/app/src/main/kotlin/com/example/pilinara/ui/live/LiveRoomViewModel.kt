package com.example.pilinara.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pilinara.data.model.LiveRoomInfoData
import com.example.pilinara.data.remote.BiliApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 直播间 ViewModel（批次F）——真实 API
 * getRoomPlayInfo（HLS 流地址解析）+ getInfoByRoom（标题/人气）+ roomEntryAction（进房上报）
 */
class LiveRoomViewModel(private val roomIdArg: Long) : ViewModel() {

    private val api = BiliApiClient()

    private val _state = MutableStateFlow(LiveState())
    val state: StateFlow<LiveState> = _state.asStateFlow()

    data class LiveState(
        val roomId: Long = 0L,          // 解析后的真实房间号
        val title: String = "",
        val liveStatus: Int = 0,        // 0未开播 1直播中
        val userCount: Long = 0L,
        val likeTotal: Long = 0L,       // WS 点赞总数（批次L6）
        val watchedCount: Long = 0L,    // WS 看过人数（批次L6）
        val areaName: String = "",
        val playUrl: String = "",       // m3u8 地址（HLS fmp4/ts 优先，ExoPlayer 原生支持）
        val isLoading: Boolean = true,
        val error: String? = null
    )

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // 房间信息（标题/状态/人气；同时解析短号→真实 room_id）
            api.getLiveRoomInfo(roomIdArg).onSuccess { resp ->
                resp.data?.let { info ->
                    _state.value = _state.value.copy(
                        roomId = info.roomId,
                        title = info.title,
                        liveStatus = info.liveStatus,
                        userCount = info.userCount,
                        areaName = "${info.parentAreaName} · ${info.areaName}"
                    )
                }
            }

            // 播放信息
            api.getLivePlayInfo(roomIdArg).onSuccess { resp ->
                if (resp.code == 0) {
                    val data = resp.data
                    val realRoomId = data?.roomId ?: roomIdArg
                    val url = pickStream(data?.playurlInfo?.playurl)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        roomId = realRoomId,
                        playUrl = url,
                        liveStatus = data?.liveStatus ?: _state.value.liveStatus,
                        error = if (url.isEmpty() && (data?.liveStatus ?: 1) == 1)
                            "未取到流地址" else null
                    )
                    // 进房上报（真实房间号）
                    if (realRoomId > 0) {
                        api.liveRoomEntryAction(realRoomId)
                        connectDanmaku(realRoomId)   // 直播弹幕 WS（批次J）
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false, error = resp.message.ifEmpty { "直播间加载失败" }
                    )
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /** 从 playurl 树中挑流：HLS(fmp4/ts) > FLV，avc > hevc，取第一个 url_info host 拼 URL */
    private fun pickStream(playurl: com.example.pilinara.data.model.LivePlayurl?): String {
        if (playurl == null) return ""
        val streams = playurl.streams
        // 优先 HLS
        val hls = streams.firstOrNull { it.protocolName == "hls" }
        val stream = hls ?: streams.firstOrNull() ?: return ""
        for (format in stream.format) {
            for (codec in format.codec) {
                if (codec.codecName != "avc") continue  // ExoPlayer 兼容性优先 avc
                val info = codec.url_info.firstOrNull() ?: continue
                return info.host + codec.baseUrl + info.extra
            }
        }
        // avc 没有则任意 codec
        val anyCodec = stream.format.firstOrNull()?.codec?.firstOrNull() ?: return ""
        val info = anyCodec.url_info.firstOrNull() ?: return ""
        return info.host + anyCodec.baseUrl + info.extra
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }

    // ==================== 直播弹幕 WebSocket（批次J） ====================

    private var wsClient: com.example.pilinara.data.remote.LiveDanmakuWsClient? = null

    private val _chatMessages = MutableStateFlow<List<com.example.pilinara.data.model.LiveDanmakuMsg>>(emptyList())
    val chatMessages: StateFlow<List<com.example.pilinara.data.model.LiveDanmakuMsg>> = _chatMessages.asStateFlow()

    /** 聊天列表中的 SC（醒目留言），与弹幕合并展示（批次L4） */
    private val _superChats = MutableStateFlow<List<com.example.pilinara.data.model.LiveSuperChat>>(emptyList())
    val superChats: StateFlow<List<com.example.pilinara.data.model.LiveSuperChat>> = _superChats.asStateFlow()

    /** 最近礼物消息（顶部飘条） */
    private val _gifts = MutableStateFlow<List<com.example.pilinara.data.model.LiveGift>>(emptyList())
    val gifts: StateFlow<List<com.example.pilinara.data.model.LiveGift>> = _gifts.asStateFlow()

    private val _popularity = MutableStateFlow(0L)
    val popularity: StateFlow<Long> = _popularity.asStateFlow()

    private val _wsState = MutableStateFlow<com.example.pilinara.data.remote.LiveDanmakuWsClient.State?>(
        com.example.pilinara.data.remote.LiveDanmakuWsClient.State.Idle
    )
    val wsState: StateFlow<com.example.pilinara.data.remote.LiveDanmakuWsClient.State?> = _wsState.asStateFlow()

    /** load 成功、拿到真实房间号后调用 */
    private fun connectDanmaku(realRoomId: Long) {
        if (realRoomId <= 0L) return
        viewModelScope.launch {
            api.getDanmuInfo(realRoomId).onSuccess { resp ->
                val data = resp.data ?: return@launch
                val client = com.example.pilinara.data.remote.LiveDanmakuWsClient(
                    roomId = realRoomId,
                    token = data.token,
                    hosts = data.hostList.map { it.host to it.wssPort }
                )
                wsClient = client
                launch {
                    client.state.collect { _wsState.value = it }
                }
                launch {
                    client.popularity.collect { _popularity.value = it }
                }
                launch {
                    client.chat.collect { msg ->
                        _chatMessages.value = (_chatMessages.value + msg).takeLast(80)
                    }
                }
                launch {
                    client.superChat.collect { sc ->
                        _superChats.value = (_superChats.value + sc).takeLast(20)
                    }
                }
                launch {
                    client.gift.collect { g ->
                        _gifts.value = (_gifts.value + g).takeLast(20)
                    }
                }
                launch {
                    client.likeTotal.collect { _state.value = _state.value.copy(likeTotal = it) }
                }
                launch {
                    client.watched.collect { _state.value = _state.value.copy(watchedCount = it) }
                }
                launch {
                    client.welcome.collect { name ->
                        val msg = com.example.pilinara.data.model.LiveDanmakuMsg(
                            uid = 0L, name = "系统", text = "欢迎 $name 进入直播间"
                        )
                        _chatMessages.value = (_chatMessages.value + msg).takeLast(80)
                    }
                }
                launch {
                    client.likeMsg.collect { name ->
                        val msg = com.example.pilinara.data.model.LiveDanmakuMsg(
                            uid = 0L, name = "系统", text = "$name 点了个赞"
                        )
                        _chatMessages.value = (_chatMessages.value + msg).takeLast(80)
                    }
                }
                client.connect()
            }
        }
    }

    /** 发送直播弹幕（需登录），成功后本地追加一条 */
    fun sendDanmaku(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            api.sendLiveDanmaku(_state.value.roomId, text.trim()).onSuccess { ok ->
                if (ok) {
                    val msg = com.example.pilinara.data.model.LiveDanmakuMsg(
                        uid = com.example.pilinara.data.remote.AccountSession.mid,
                        name = "我",
                        text = text.trim()
                    )
                    _chatMessages.value = (_chatMessages.value + msg).takeLast(80)
                } else {
                    _state.value = _state.value.copy(error = "发送失败（可能需要登录或粉丝牌）")
                }
            }.onFailure {
                _state.value = _state.value.copy(error = "发送失败: ${it.message}")
            }
        }
    }

    override fun onCleared() {
        wsClient?.close()
        super.onCleared()
    }
}
