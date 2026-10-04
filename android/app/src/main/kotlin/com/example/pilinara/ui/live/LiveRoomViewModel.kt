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
                    if (realRoomId > 0) api.liveRoomEntryAction(realRoomId)
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
}
