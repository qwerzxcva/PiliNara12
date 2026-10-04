package com.example.pilinara.data.remote

import android.util.Log
import com.example.pilinara.data.model.LiveDanmakuMsg
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * 直播弹幕 WebSocket 客户端（批次J）。
 *
 * B站弹幕协议（对标 Flutter 参照 tcp/live.dart）：
 * - 包头 16 字节大端：totalLen u32@0 | headerLen u16@4 | protoVer u16@6 | op u32@8 | seq u32@12
 * - op 2=客户端心跳  3=心跳回复(人气值在body)  5=业务消息(JSON)  7=认证  8=认证成功
 * - protoVer 0/1=裸 JSON  2=zlib 压缩（解压后内含多个包）
 * - auth body: {"uid":0,"roomid":X,"protover":2,"platform":"web","type":2,"key":token}
 */
class LiveDanmakuWsClient(
    private val roomId: Long,
    private val token: String,
    private val hosts: List<Pair<String, Int>>  // (host, wssPort)
) {
    companion object {
        private const val TAG = "LiveDanmakuWs"
        private const val OP_HEARTBEAT = 2
        private const val OP_HEARTBEAT_REPLY = 3
        private const val OP_MESSAGE = 5
        private const val OP_AUTH = 7
        private const val OP_AUTH_REPLY = 8
        private const val PROTO_PLAIN = 0
        private const val PROTO_HEARTBEAT = 1  // 心跳回复虽 ver=1 但 body 是裸 4 字节人气值
        private const val PROTO_ZLIB = 2
    }

    /** 连接状态 */
    sealed interface State {
        data object Idle : State
        data object Connecting : State
        data object Authenticated : State
        data object Closed : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _danmaku = MutableSharedFlow<LiveDanmakuMsg>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val danmaku: SharedFlow<LiveDanmakuMsg> = _danmaku.asSharedFlow()

    private val _chat = MutableSharedFlow<LiveDanmakuMsg>(
        replay = 0, extraBufferCapacity = 128, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    /** 聊天区列表（含用户名/徽章），弹幕渲染与聊天列表共用来源 */
    val chat: SharedFlow<LiveDanmakuMsg> = _chat.asSharedFlow()

    /** 人气值（心跳回复 body 前 4 字节 u32） */
    private val _popularity = MutableStateFlow(0L)
    val popularity: StateFlow<Long> = _popularity.asStateFlow()

    private var session: DefaultWebSocketSession? = null
    private var heartbeatJob: Job? = null
    private var scope: CoroutineScope? = null
    @Volatile private var closed = false

    private val wsHttpClient = HttpClient(OkHttp) {
        install(WebSockets)
    }

    // ---------- 协议编解码 ----------

    private fun pack(op: Int, body: ByteArray = ByteArray(0), protoVer: Int = 1): ByteArray {
        val buf = ByteBuffer.allocate(16 + body.size)
        buf.putInt(16 + body.size)      // totalLen
        buf.putShort(16)                // headerLen
        buf.putShort(protoVer.toShort())
        buf.putInt(op)
        buf.putInt(1)                   // seq
        buf.put(body)
        return buf.array()
    }

    private fun authBody(): ByteArray {
        val uid = try { AccountSession.mid } catch (_: Exception) { 0L }
        val json = """{"uid":$uid,"roomid":$roomId,"protover":2,"platform":"web","type":2,"key":"$token"}"""
        return json.toByteArray(Charsets.UTF_8)
    }

    // ---------- 数据包解析 ----------

    private fun parsePackets(data: ByteArray) {
        var offset = 0
        while (offset + 16 <= data.size) {
            val buf = ByteBuffer.wrap(data, offset, data.size - offset)
            val totalLen = buf.int
            val headerLen = buf.short.toInt()
            val protoVer = buf.short.toInt()
            val op = buf.int
            buf.int // seq
            if (totalLen < headerLen || offset + totalLen > data.size) break

            val body = data.copyOfRange(offset + headerLen, offset + totalLen)
            handlePacket(protoVer, op, body)
            offset += totalLen
        }
    }

    private fun handlePacket(protoVer: Int, op: Int, body: ByteArray) {
        when (op) {
            OP_HEARTBEAT_REPLY -> {
                if (body.size >= 4) {
                    _popularity.value = ((body[0].toLong() and 0xFF) shl 24) or
                        ((body[1].toLong() and 0xFF) shl 16) or
                        ((body[2].toLong() and 0xFF) shl 8) or
                        (body[3].toLong() and 0xFF)
                }
            }
            OP_AUTH_REPLY -> {
                _state.value = State.Authenticated
                Log.i(TAG, "认证成功 room=$roomId")
            }
            OP_MESSAGE -> when (protoVer) {
                PROTO_PLAIN, PROTO_HEARTBEAT -> handleJsonBody(body)
                PROTO_ZLIB -> try {
                    val inflater = Inflater()
                    inflater.setInput(body)
                    val out = ByteArrayOutputStream()
                    val tmp = ByteArray(4096)
                    InflaterInputStream(object : java.io.InputStream() {
                        override fun read(): Int = throw UnsupportedOperationException()
                        override fun read(b: ByteArray, off: Int, len: Int): Int = inflater.inflate(b, off, len)
                    }).use { ins ->
                        while (true) {
                            val n = ins.read(tmp)
                            if (n <= 0) break
                            out.write(tmp, 0, n)
                            if (out.size() > 4 * 1024 * 1024) break  // 安全上限
                        }
                    }
                    parsePackets(out.toByteArray())
                } catch (e: Exception) {
                    Log.w(TAG, "zlib 解压失败: ${e.message}")
                }
            }
        }
    }

    private fun handleJsonBody(body: ByteArray) {
        try {
            val obj = org.json.JSONObject(String(body, Charsets.UTF_8))
            dispatchCmd(obj)
        } catch (_: Exception) {}
    }

    /** 业务消息分发（DANMU_MSG 等；结构对齐 Flutter controller._danmakuListener） */
    private fun dispatchCmd(obj: org.json.JSONObject) {
        when (obj.optString("cmd")) {
            "DANMU_MSG" -> {
                try {
                    val info = obj.getJSONArray("info")
                    val text = info.optString(1)
                    if (text.isBlank()) return
                    val meta = info.optJSONArray(0) ?: return
                    val content = meta.optJSONObject(15)
                    val user = content?.optJSONObject("user")
                    val uid = user?.optLong("uid") ?: meta.optLong(7, 0L)
                    val name = user?.optJSONObject("base")?.optString("name")
                        ?: runCatching {
                            info.optJSONArray(2)?.optString(1).orEmpty()
                        }.getOrDefault("")
                    val extra = runCatching {
                        content?.optString("extra")?.let { org.json.JSONObject(it) }
                    }.getOrNull()
                    val color = extra?.optInt("color", 0xFFFFFF) ?: 0xFFFFFF
                    val mode = extra?.optInt("mode", 1) ?: 1
                    val medal = user?.optJSONObject("medal")
                    val msg = LiveDanmakuMsg(
                        uid = uid,
                        name = name,
                        text = text,
                        color = color or 0xFF000000.toInt(),
                        mode = mode,
                        isEmote = extra?.optInt("dm_type", 0) == 1,
                        medalName = medal?.optString("name")?.takeIf { it.isNotBlank() },
                        medalLevel = medal?.optInt("level", 0) ?: 0
                    )
                    _danmaku.tryEmit(msg)
                    _chat.tryEmit(msg)
                } catch (e: Exception) {
                    Log.w(TAG, "DANMU_MSG 解析失败: ${e.message}")
                }
            }
            // 其余 cmd（进房/礼物/SC 等）后续批次按需扩展
        }
    }

    // ---------- 连接生命周期 ----------

    fun connect() {
        if (closed || hosts.isEmpty()) return
        _state.value = State.Connecting
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope?.launch {
            try {
                val (host, port) = hosts.first()
                val s = wsHttpClient.webSocketSession(
                    urlString = "wss://$host:$port/sub"
                ) {
                    headers.append("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
                    headers.append("Origin", "https://live.bilibili.com")
                }
                session = s
                // 认证包
                s.send(Frame.Binary(true, pack(OP_AUTH, authBody(), protoVer = 1)))
                startHeartbeat(s)
                for (frame in s.incoming) {
                    if (frame is Frame.Binary) parsePackets(frame.data)
                }
                _state.value = State.Closed
            } catch (e: Exception) {
                if (!closed) {
                    Log.w(TAG, "WS 失败: ${e.message}")
                    _state.value = State.Failed(e.message ?: "连接失败")
                }
            }
        }
    }

    private fun startHeartbeat(s: DefaultWebSocketSession) {
        heartbeatJob?.cancel()
        heartbeatJob = scope?.launch {
            while (isActive && !closed) {
                delay(30_000)
                try {
                    s.send(io.ktor.websocket.Frame.Binary(true, pack(OP_HEARTBEAT)))
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun close() {
        closed = true
        heartbeatJob?.cancel()
        val s = session
        val sc = scope
        session = null
        scope = null
        sc?.launch { try { s?.close() } catch (_: Exception) {} }
        sc?.cancel()
        try { wsHttpClient.close() } catch (_: Exception) {}
        _state.value = State.Closed
    }
}
