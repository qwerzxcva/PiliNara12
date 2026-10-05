package com.example.pilinara.playback

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.pilinara.data.model.formatCount
import com.example.pilinara.data.model.toParsed
import com.example.pilinara.data.remote.AccountSession
import com.example.pilinara.data.remote.BiliApiClient
import com.example.pilinara.data.repository.VideoRepository

class VideoPlayerViewModel(private val context: Context) : ViewModel(), Player.Listener {
    
    private var _player: ExoPlayer? = null
    val player: ExoPlayer? get() = _player
    
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    private val _danmakuQueue = mutableListOf<DanmakuEvent>()
    
    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentTime: Long = 0L,
        val duration: Long = 0L,
        val volume: Float = 1.0f,
        val playbackSpeed: Float = 1.0f,
        val isMuted: Boolean = false,
        val isBuffering: Boolean = false,
        val error: String? = null,
        val position: Int = 0,
        val isLiked: Boolean = false,
        val isFavorited: Boolean = false,
        val coinCount: Int = 0,
        val likeCount: Long = 0L,
        val coinCountTotal: Long = 0L,
        val favCount: Long = 0L,
        // 批次A：播放器补全
        val currentPart: Int = 1,
        val currentPartTitle: String = "",
        val partCount: Int = 1,
        val danmakuOn: Boolean = true,
        val danmakuAlpha: Float = 1f,
        val danmakuScale: Float = 1f,
        val brightness: Float = 0.5f,
        val gestureSeekDeltaMs: Long = 0L,
        val qualities: List<QualityOption> = emptyList(),
        val currentQn: Int = 80,
        val related: List<RelatedItem> = emptyList()
    )
    
    data class DanmakuEvent(
        val id: String,
        val timestamp: Long,
        val content: String,
        val color: Int,
        val fontSize: Int
    )
    
    init {
        _player = ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(android.os.PowerManager.PARTIAL_WAKE_LOCK)
            .build()
        _player?.addListener(this)
        // 读取 DataStore 持久化设置：默认清晰度 + 弹幕开关（真实作用于播放链路）
        val storage = com.example.pilinara.utils.StorageManager(context)
        viewModelScope.launch {
            storage.videoQualityFlow.collect { q ->
                currentQn = when (q) {
                    "1080p" -> 80; "720p" -> 64; "480p" -> 32; else -> currentQn
                }
            }
        }
        viewModelScope.launch {
            storage.danmakuEnabledFlow.collect { on ->
                _state.value = _state.value.copy(danmakuOn = on)
            }
        }
        viewModelScope.launch {
            storage.danmakuOpacityFlow.collect { a ->
                _state.value = _state.value.copy(danmakuAlpha = a)
            }
        }
    }
    
    fun loadVideo(uri: String, bvid: String = "", cid: Long = 0L, epId: Long = 0L, local: Boolean = false) {
        viewModelScope.launch {
            try {
                // 离线播放（批次I）：本地 video.m4s + audio.m4s 合流播放
                if (local && bvid.isNotEmpty()) {
                    val playback = com.example.pilinara.data.repository.DownloadManager
                        .getLocalPlayback(context, bvid)
                    if (playback != null) {
                        _state.value = _state.value.copy(isBuffering = true, error = null)
                        startPlayback(playback.videoPath, playback.audioPath.takeIf { it.isNotEmpty() })
                    } else {
                        setError("离线缓存不存在或未完成")
                    }
                    return@launch
                }
                // 番剧模式：pgc playurl（Rust 已兼容 result.dash）
                if (epId > 0L) {
                    loadPgcEpisode(epId)
                    return@launch
                }
                // 无 URI：走真实解析链路 —— 详情拿 cid → playurl → Rust 选流 → DASH
                if (uri.isEmpty() && bvid.isNotEmpty()) {
                    _state.value = _state.value.copy(isBuffering = true, error = null)
                    resolveAndPlay(bvid, cid)
                } else {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(uri))
                        .setMediaId("$bvid:$cid")
                        .build()
                    _player?.setMediaItem(mediaItem)
                    _player?.prepare()
                    _player?.playWhenReady = false
                    _state.value = _state.value.copy(
                        isPlaying = false, isBuffering = true,
                        error = null, currentTime = 0L, duration = 0L
                    )
                }
            } catch (e: Exception) {
                setError("Failed to load video: ${e.message}")
            }
        }
    }

    /** 番剧剧集播放链路（批次D 补全）：pgc season 详情取 cid + 标题/集数 → pgc playurl → Rust 选流 */
    private suspend fun loadPgcEpisode(epId: Long) {
        val repo = VideoRepository(BiliApiClient())
        _state.value = _state.value.copy(isBuffering = true, error = null)

        val api = BiliApiClient()
        val season = api.getPgcSeason(epId = epId).getOrNull()?.result
        val episode = season?.episodes?.firstOrNull { it.id == epId }
            ?: season?.episodes?.firstOrNull()
        val cid = episode?.cid ?: 0L
        if (cid <= 0L) {
            setError("无法获取剧集 cid（pgc 详情失败）")
            return
        }
        aid = episode?.bvid?.let { repo.getVideoDetail(it).getOrNull()?.data?.aid } ?: 0L

        // 同番剧全部分集作为“分P”面板
        pages = season?.episodes.orEmpty().mapIndexed { idx, ep ->
            PartInfo(cid = ep.cid, page = idx + 1,
                part = listOfNotNull(ep.title.ifBlank { null }, ep.longTitle.ifBlank { null })
                    .joinToString(" ").ifBlank { "第${idx + 1}集" },
                durationSec = ep.duration)
        }
        currentPartIndex = season?.episodes?.indexOfFirst { it.id == epId }?.coerceAtLeast(0) ?: 0
        _state.value = _state.value.copy(
            partCount = pages.size,
            currentPart = currentPartIndex + 1,
            currentPartTitle = pages.getOrNull(currentPartIndex)?.part ?: ""
        )

        this.epId = epId
        this.currentBvid = "ep$epId"
        // 番剧弹幕需要 cid
        loadDanmakuFor(cid)

        val (resp, video, audio) = repo.getPgcPlayUrl(epId, cid, qn = currentQn)
            .getOrElse { setError("番剧 playurl 失败: ${it.message}"); return }
        if (video.isNullOrEmpty()) {
            setError("未解析到番剧流地址（可能为大会员专享）")
            return
        }
        cachedAudioUrl = audio
        dashVideos = resp.result?.dash?.video.orEmpty()
        currentQn = qnOf(resp)
        startPlayback(video, audio)
    }

    private fun qnOf(resp: com.example.pilinara.data.model.PgcPlayUrlResponse): Int =
        resp.result?.quality ?: currentQn

    private suspend fun loadDanmakuFor(cid: Long) {
        val api = BiliApiClient()
        api.getDanmaku(cid).getOrNull()?.let { resp ->
            resp.data?.let { list ->
                addDanmakuEvents(list.map { d ->
                    val p = d.toParsed()
                    DanmakuEvent(
                        id = "",
                        timestamp = (p.timestamp * 1000).toLong(),
                        content = p.content,
                        color = p.color or 0xFF000000.toInt(),
                        fontSize = p.fontSize
                    )
                })
            }
        }
    }

    // ==================== 字幕（批次K） ====================

    private var subtitleCues: List<com.example.pilinara.data.model.SubtitleCue> = emptyList()
    // 可选字幕列表（批次L9：字幕选择 UI）——(id, 语言名, url)
    private val _subtitleTracks = MutableStateFlow<List<Triple<Long, String, String>>>(emptyList())
    val subtitleTracks: StateFlow<List<Triple<Long, String, String>>> = _subtitleTracks.asStateFlow()

    private val _currentSubtitle = MutableStateFlow("")
    val currentSubtitle: StateFlow<String> = _currentSubtitle.asStateFlow()

    // 当前选中的字幕 id（批次L9）
    private val _selectedSubtitleId = MutableStateFlow(-1L)
    val selectedSubtitleId: StateFlow<Long> = _selectedSubtitleId.asStateFlow()

    /** resolveAndPlay 成功后调用：拉字幕列表（保留全部可选字幕，默认取第一条） */
    private fun loadSubtitles(bvid: String, cid: Long) {
        if (bvid.isEmpty() || cid <= 0L || bvid.startsWith("ep")) return
        viewModelScope.launch {
            runCatching {
                val api = BiliApiClient()
                val subs = api.getPlayerV2(bvid, cid).getOrNull()?.data?.subtitle?.subtitles.orEmpty()
                _subtitleTracks.value = subs.map { Triple(it.id, it.langDoc, it.subtitleUrl) }
                val pick = subs.firstOrNull { !it.isLock } ?: return@launch
                selectSubtitle(pick.id)
            }
        }
    }

    /** 选择字幕轨（懒加载 body）；id=-1 关闭字幕 */
    fun selectSubtitle(id: Long) {
        _selectedSubtitleId.value = id
        if (id < 0) {
            subtitleCues = emptyList()
            _currentSubtitle.value = ""
            return
        }
        val url = _subtitleTracks.value.firstOrNull { it.first == id }?.third ?: return
        viewModelScope.launch {
            runCatching {
                val body = BiliApiClient().fetchSubtitleBody(url).getOrNull() ?: return@launch
                subtitleCues = body.body.sortedBy { it.from }
            }
        }
    }

    /** 播放器每帧/每秒调：更新当前字幕文本 */
    fun updateSubtitleAt(currentTimeMs: Long) {
        val sec = currentTimeMs / 1000f
        val cue = subtitleCues.firstOrNull { sec >= it.from && sec <= it.to }
        val text = cue?.content.orEmpty().replace("<br/>", "\n")
        if (text != _currentSubtitle.value) _currentSubtitle.value = text
    }

    /**
     * 真实播放链路：
     * 1) cid 缺失时拉 /x/web-interface/view 取 cid
     * 2) /x/player/wbi/playurl 拿 DASH（Rust 侧 selectStreams 选最优流）
     * 3) video/audio 双流喂给 ExoPlayer（MediaItem sideloaded manifests 不适合分开的 DASH，
     *    这里用 DataSource 级拼接不可行，因此取 ExoPlayer 原生 DASH 需 manifest；
     *    折中方案：优先用 Rust 选出的 video baseUrl 直接播（无声），并单独 track 音频 ——
     *    Media3 支持多 MediaItem 拼接播放（ConcatenatingMediaSource2）
     */
    private suspend fun resolveAndPlay(bvid: String, cidIn: Long) {
        val repo = VideoRepository(BiliApiClient())
        // 1) 补 cid + 分P列表 + aid
        val detail = repo.getVideoDetail(bvid).getOrNull()?.data
        val effectiveCid = cidIn.takeIf { it > 0L }
            ?: detail?.cid
            ?: run {
                setError("无法获取视频 cid（详情接口失败）")
                return
            }
        aid = detail?.aid ?: 0L
        videoTitle = detail?.title.orEmpty()
        videoCover = detail?.pic.orEmpty()
        videoOwner = detail?.owner?.name.orEmpty()
        // 分P 列表（单P视频 pages 只有 1 项）
        pages = (detail?.pages.orEmpty()).map {
            PartInfo(cid = it.cid, page = it.page, part = it.part, durationSec = it.duration)
        }.ifEmpty { listOf(PartInfo(effectiveCid, 1, "", 0L)) }
        currentPartIndex = pages.indexOfFirst { it.cid == effectiveCid }.coerceAtLeast(0)
        _state.value = _state.value.copy(
            partCount = pages.size,
            currentPart = pages[currentPartIndex].page,
            currentPartTitle = pages[currentPartIndex].part
        )
        loadEngagement(aid, bvid)
        loadRelated(bvid)

        // 2) playurl + Rust 流选择
        val (resp, videoUrl2, audioUrl2) = repo.getPlayUrl(bvid, effectiveCid, qn = currentQn)
            .getOrElse {
                setError("playurl 获取失败: ${it.message}")
                return
            }
        this.effectiveCid = effectiveCid
        this.currentBvid = bvid
        cachedAudioUrl = audioUrl2
        if (videoUrl2.isNullOrEmpty()) {
            setError("未解析到视频流地址（可能需要登录后观看）")
            return
        }

        // 3) 缓存清晰度列表（accept_quality + 当前 dash 可选流）
        dashVideos = resp.data?.dash?.video.orEmpty()
        val accepted = resp.data?.acceptDescription.orEmpty()
        val acceptQn = resp.data?.acceptQuality?.map { it.quality } ?: emptyList()
        availableQualities = accepted.zip(acceptQn).map { (desc, q) -> QualityOption(q, desc) }
        currentQn = resp.data?.quality ?: currentQn

        startPlayback(videoUrl2, audioUrl2)
        loadSubtitles(bvid, effectiveCid)
        loadVideoShot(bvid, effectiveCid)
    }

    /** 批次L3：加载进度条缩略图雪碧图（非阻断，失败静默） */
    private val _videoShot = kotlinx.coroutines.flow.MutableStateFlow<com.example.pilinara.data.model.VideoShotData?>(null)
    val videoShot: kotlinx.coroutines.flow.StateFlow<com.example.pilinara.data.model.VideoShotData?> = _videoShot

    private fun loadVideoShot(bvid: String, cid: Long) {
        _videoShot.value = null
        if (bvid.isEmpty() || bvid.startsWith("ep")) return
        viewModelScope.launch {
            BiliApiClient().getVideoShot(bvid, cid).onSuccess { resp ->
                if (resp.code == 0) _videoShot.value = resp.data
            }
        }
    }

    /** 按当前秒数取缩略图帧 (url, col, row) */
    fun shotFrameAt(second: Long): Triple<String, Int, Int>? = _videoShot.value?.frameAt(second)

    /** 组装 MergingMediaSource 并启动播放（可带恢复进度） */
    private fun startPlayback(
        videoUrl: String,
        audioUrl: String?,
        resumePositionMs: Long = 0L,
        playWhenReady: Boolean = true
    ) {
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) PiliNara/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.bilibili.com"))

        val isLocal = !videoUrl.startsWith("http") && !videoUrl.startsWith("//")
        val videoUri = if (videoUrl.startsWith("/")) Uri.fromFile(java.io.File(videoUrl)) else Uri.parse(videoUrl)
        val videoItem = MediaItem.Builder().setUri(videoUri).build()
        val localFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
        val videoSource: androidx.media3.exoplayer.source.MediaSource =
            if (isLocal) {
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(localFactory)
                    .createMediaSource(videoItem)
            } else {
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(videoItem)
            }

        val merged = if (!audioUrl.isNullOrEmpty()) {
            val audioUri = if (audioUrl.startsWith("/")) Uri.fromFile(java.io.File(audioUrl)) else Uri.parse(audioUrl)
            val audioItem = MediaItem.Builder().setUri(audioUri).build()
            val audioSource: androidx.media3.exoplayer.source.MediaSource =
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(
                    if (isLocal) localFactory else dataSourceFactory
                ).createMediaSource(audioItem)
            androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }

        _player?.setMediaSource(merged, resumePositionMs)
        _player?.prepare()
        _player?.playWhenReady = playWhenReady
        _state.value = _state.value.copy(
            isPlaying = playWhenReady, isBuffering = true,
            error = null, duration = 0L,
            qualities = availableQualities, currentQn = currentQn,
            brightness = _state.value.brightness
        )
    }

    fun play() {
        viewModelScope.launch {
            _player?.play()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }
    
    fun pause() {
        viewModelScope.launch {
            _player?.pause()
            _state.value = _state.value.copy(isPlaying = false)
        }
    }
    
    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            _player?.seekTo(positionMs)
            _state.value = _state.value.copy(currentTime = positionMs)
        }
    }
    
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        viewModelScope.launch {
            _player?.volume = clamped
            _state.value = _state.value.copy(
                volume = clamped, isMuted = clamped == 0f
            )
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        val normalizedSpeed = when {
            speed <= 0.5f -> 0.5f
            speed <= 1.0f -> 1.0f
            speed <= 2.0f -> speed
            else -> 2.0f
        }
        viewModelScope.launch {
            _player?.playbackParameters = androidx.media3.common.PlaybackParameters(
                normalizedSpeed, _player?.playbackParameters?.pitch ?: 1.0f
            )
            _state.value = _state.value.copy(playbackSpeed = normalizedSpeed)
        }
    }
    
    fun toggleMute() {
        viewModelScope.launch {
            val newMuted = !_state.value.isMuted
            _player?.volume = if (newMuted) 0f else 1f
            _state.value = if (newMuted) {
                _state.value.copy(isMuted = true, volume = 0f)
            } else {
                _state.value.copy(isMuted = false, volume = 1f)
            }
        }
    }
    
    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }
    
    fun seekRelative(offsetMs: Long) {
        val current = _state.value.currentTime
        val duration = _state.value.duration
        seekTo((current + offsetMs).coerceIn(0L, duration))
    }
    
    fun setError(error: String?) {
        if (error != null) _state.value = _state.value.copy(error = error)
    }

    /** 分享反馈（复制链接成功提示） */
    fun notifyShared(link: String) {
        setError("链接已复制：$link")
    }

    fun addDanmakuEvents(events: List<DanmakuEvent>) {
        _danmakuQueue.clear()
        // 弹幕屏蔽规则过滤（关键词/正则/UID）
        _danmakuQueue.addAll(
            events.filterNot {
                com.example.pilinara.ui.settings.DanmakuBlockViewModel.shouldBlock(it.content, 0L)
            }
        )
    }
    
    fun getDanmakuAtTime(currentTimeMs: Long): List<DanmakuEvent> {
        return _danmakuQueue.filter { event ->
            event.timestamp <= currentTimeMs && 
            event.timestamp > currentTimeMs - 3000L
        }
    }
    
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying)
    }
    
    override fun onIsLoadingChanged(isLoading: Boolean) {
        _state.value = _state.value.copy(isBuffering = isLoading)
    }
    
    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_BUFFERING -> _state.value = _state.value.copy(isBuffering = true)
            Player.STATE_READY -> _state.value = _state.value.copy(
                isBuffering = false, duration = _player?.duration ?: 0L
            )
            Player.STATE_ENDED -> _state.value = _state.value.copy(isPlaying = false)
        }
    }
    
    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        setError(error.message ?: "Playback error")
    }
    
    override fun onPositionDiscontinuity(
        newPosition: androidx.media3.common.Player.PositionInfo,
        oldPosition: androidx.media3.common.Player.PositionInfo,
        reason: Int
    ) {
        _state.value = _state.value.copy(currentTime = newPosition.positionMs)
    }
    
    override fun onCleared() {
        super.onCleared()
        _player?.removeListener(this)
        _player?.release()
        _player = null
    }
    
    fun getCurrentTime(): Long = _state.value.currentTime
    fun getDuration(): Long = _state.value.duration
    fun getPositionPercent(): Int {
        val duration = _state.value.duration
        return if (duration == 0L) 0 else ((_state.value.currentTime * 100) / duration).toInt()
    }

    // ========== 互动（点赞/投币/收藏/历史上报） ==========

    private var aid: Long = 0L

    /** 当前视频标题/封面（供下载等外部操作，批次I 接线） */
    var videoTitle: String = ""
        private set
    var videoCover: String = ""
        private set
    var videoOwner: String = ""
        private set

    /** 发起离线下载（下载按钮 → DownloadManager 队列） */
    fun downloadCurrent(appContext: android.content.Context) {
        val bv = currentBvid
        if (bv.isEmpty() || bv.startsWith("ep")) return
        com.example.pilinara.data.repository.DownloadManager.download(
            appContext, bv,
            title = videoTitle, cover = videoCover, ownerName = videoOwner
        )
    }
    private var epId: Long = 0L
    private var effectiveCid: Long = 0L
    private var currentBvid: String = ""
    private var defaultFavFolderId: Long = 0L
    private val repo by lazy { VideoRepository(BiliApiClient()) }

    /**
     * 登录态下加载互动状态：已赞/已投/已藏（archive/relation）+ 默认收藏夹 id + stat 计数。
     * 失败静默（未登录时 relation 会 -101，直接跳过）。
     */
    private fun loadEngagement(aid: Long, bvid: String) {
        viewModelScope.launch {
            if (!AccountSession.isLogin) return@launch
            // 1) 交互状态
            BiliApiClient().getVideoRelation(aid).onSuccess { (liked, coined, fav) ->
                _state.value = _state.value.copy(
                    isLiked = liked,
                    coinCount = if (coined) 1 else 0,
                    isFavorited = fav
                )
            }
            // 2) 默认收藏夹（第一个）
            runCatching {
                defaultFavFolderId = BiliApiClient().getMyFavFolders().getOrNull()
                    ?.firstOrNull()?.id ?: 0L
            }
            // 3) 真实计数（点赞/投币/收藏数）
            runCatching {
                repo.getVideoDetail(bvid).getOrNull()?.data?.stat?.let { s ->
                    _state.value = _state.value.copy(
                        likeCount = s.like, coinCountTotal = s.coin, favCount = s.favorite
                    )
                }
            }
        }
    }

    /** 发送视频弹幕（批次L5：需登录，插入当前进度点） */
    fun sendDanmaku(text: String, mode: Int = 1, color: Int = 16777215) {
        if (text.isBlank()) return
        viewModelScope.launch {
            if (!AccountSession.isLogin) { setError("请先登录后发弹幕"); return@launch }
            val cid = effectiveCid
            if (cid == 0L || currentBvid.isEmpty()) { setError("弹幕发送失败（视频未就绪）"); return@launch }
            val progress = _player?.currentPosition ?: 0L
            BiliApiClient().sendVideoDanmaku(cid, currentBvid, text.trim(), progress, mode, color)
                .onSuccess { ok ->
                    if (ok) {
                        // 本地立即插入一条，反馈真实感
                        val danmaku = DanmakuEvent(
                            id = "local_${System.currentTimeMillis()}",
                            timestamp = progress,
                            content = text.trim(),
                            color = color,
                            fontSize = 25
                        )
                        _danmakuQueue.add(danmaku)
                        _danmakuQueue.sortBy { it.timestamp }
                        setError("弹幕发送成功")
                    } else setError("弹幕发送失败（可能需要登录或被风控）")
                }
                .onFailure { setError("弹幕发送失败: ${it.message}") }
        }
    }

    /** 加入稍后再看（需登录） */
    fun addToWatchLater() {
        viewModelScope.launch {
            if (!AccountSession.isLogin) { setError("请先登录"); return@launch }
            ensureAid()
            if (aid == 0L) { setError("无法获取视频 aid"); return@launch }
            BiliApiClient().addToView(aid)
                .onSuccess { ok -> setError(if (ok) "已加入稍后再看" else "添加失败") }
                .onFailure { setError("添加失败: ${it.message}") }
        }
    }

    /** 点赞（需登录） */
    fun toggleLike() {        viewModelScope.launch {
            if (!AccountSession.isLogin) {
                setError("请先登录后再点赞")
                return@launch
            }
            ensureAid()
            if (aid == 0L) { setError("无法获取视频 aid"); return@launch }
            val liked = _state.value.isLiked
            repo.likeVideo(aid, if (liked) 2 else 1)
                .onSuccess { ok ->
                    if (ok) _state.value = _state.value.copy(
                        isLiked = !liked,
                        likeCount = _state.value.likeCount + if (liked) -1 else 1
                    )
                    else setError("点赞失败（接口返回非 0）")
                }
                .onFailure { setError("点赞失败: ${it.message}") }
        }
    }

    /** 投币 1 枚（需登录） */
    fun coinOnce() {
        viewModelScope.launch {
            if (!AccountSession.isLogin) {
                setError("请先登录后再投币")
                return@launch
            }
            ensureAid()
            if (aid == 0L) { setError("无法获取视频 aid"); return@launch }
            repo.coinVideo(aid, 1)
                .onSuccess { ok ->
                    if (ok) _state.value = _state.value.copy(
                        coinCount = _state.value.coinCount + 1,
                        coinCountTotal = _state.value.coinCountTotal + 1
                    )
                    else setError("投币失败（余额不足或已投满）")
                }
                .onFailure { setError("投币失败: ${it.message}") }
        }
    }

    /** 收藏（需登录，使用用户默认收藏夹） */
    fun toggleFavorite() {
        viewModelScope.launch {
            if (!AccountSession.isLogin) {
                setError("请先登录后再收藏")
                return@launch
            }
            ensureAid()
            if (aid == 0L) { setError("无法获取视频 aid"); return@launch }
            val fav = _state.value.isFavorited
            val mediaId = defaultFavFolderId
            if (mediaId == 0L && !fav) { setError("未获取到默认收藏夹"); return@launch }
            repo.favoriteVideo(aid, mediaId, if (fav) 2 else 1)
                .onSuccess { ok ->
                    if (ok) _state.value = _state.value.copy(
                        isFavorited = !fav,
                        favCount = _state.value.favCount + if (fav) -1 else 1
                    )
                    else setError("收藏失败（接口返回非 0）")
                }
                .onFailure { setError("收藏失败: ${it.message}") }
        }
    }

    /** 上报历史进度（播放中每 15 秒调一次） */
    fun reportProgress() {
        viewModelScope.launch {
            if (!AccountSession.isLogin || aid == 0L || effectiveCid == 0L) return@launch
            repo.reportHistory(aid, effectiveCid, _state.value.currentTime / 1000)
        }
    }

    private suspend fun ensureAid() {
        if (aid > 0L) return
        // 番剧 ep 模式：bvid 是 "ep{id}" 虚拟值，直接用 pgc 详情里的真实 aid
        if (currentBvid.startsWith("ep")) return
        aid = repo.getVideoDetail(currentBvid).getOrNull()?.data?.aid ?: 0L
    }

    // ========== 批次A：播放器补全（多P/清晰度/相关视频/弹幕设置） ==========

    /** 视频分P列表（来自详情接口 pages[]） */
    data class PartInfo(val cid: Long, val page: Int, val part: String, val durationSec: Long)

    /** 可选清晰度 */
    data class QualityOption(val qn: Int, val label: String)

    /** 相关视频 */
    data class RelatedItem(
        val bvid: String, val cid: Long, val title: String, val pic: String,
        val author: String, val durationText: String, val viewText: String
    )

    private var pages: List<PartInfo> = emptyList()
    private var currentPartIndex: Int = 0
    private var availableQualities: List<QualityOption> = emptyList()
    private var currentQn: Int = 80
    private var dashVideos: List<com.example.pilinara.data.model.StreamInfo> = emptyList()

    /** 当前分P 页码（1-based，单P视频恒为 1） */
    val currentPage: Int get() = pages.getOrNull(currentPartIndex)?.page ?: 1
    val hasMultipleParts: Boolean get() = pages.size > 1

    /** 弹幕开关 + 透明度（UI 直接读写） */
    var danmakuEnabled: Boolean
        get() = _state.value.danmakuOn
        set(v) { _state.value = _state.value.copy(danmakuOn = v) }

    /**
     * 切换分P：重走播放链路（不重取详情）。
     */
    fun playPart(pageIndex: Int) {
        val p = pages.getOrNull(pageIndex) ?: return
        currentPartIndex = pageIndex
        _state.value = _state.value.copy(currentPart = p.page, currentPartTitle = p.part)
        viewModelScope.launch {
            resolveAndPlay(currentBvid, p.cid)
        }
    }

    /**
     * 切换清晰度：从已缓存的 dash.video[] 中按 qn 找流，无缝换源（保持进度）。
     * dash.video[].id 即 qn。
     */
    fun switchQuality(qn: Int) {
        if (qn == currentQn) return
        val target = dashVideos.firstOrNull { it.id == qn } ?: return
        currentQn = qn
        val pos = _player?.currentPosition ?: 0L
        val playing = _state.value.isPlaying
        viewModelScope.launch {
            val audio = bestAudio()
            startPlayback(target.baseUrl ?: return@launch, audio, resumePositionMs = pos, playWhenReady = playing)
        }
    }

    private fun bestAudio(): String? = cachedAudioUrl

    private var cachedAudioUrl: String? = null

    /** 加载相关视频列表 */
    private fun loadRelated(bvid: String) {
        viewModelScope.launch {
            runCatching {
                val list = BiliApiClient().getRelatedVideos(bvid).getOrNull().orEmpty()
                _state.value = _state.value.copy(
                    related = list.map {
                        RelatedItem(
                            bvid = it.bvid, cid = it.cid, title = it.title, pic = it.pic,
                            author = it.owner?.name ?: "",
                            durationText = it.duration.let { s -> "%d:%02d".format(s / 60, s % 60) },
                            viewText = formatCount(it.stat?.view ?: 0L)
                        )
                    }
                )
            }
        }
    }

    /** 播放相关视频（重置状态换源） */
    fun playRelated(item: RelatedItem) {
        _state.value = _state.value.copy(related = _state.value.related, error = null)
        viewModelScope.launch {
            currentBvid = item.bvid
            aid = item.bvid.let { repo.getVideoDetail(it).getOrNull()?.data?.aid ?: 0L }
            loadEngagement(aid, item.bvid)
            loadRelated(item.bvid)
            resolveAndPlay(item.bvid, item.cid)
        }
    }

    /** 弹幕透明度 0..1 */
    fun setDanmakuAlpha(alpha: Float) {
        _state.value = _state.value.copy(danmakuAlpha = alpha.coerceIn(0f, 1f))
    }

    /** 弹幕大小倍率 0.5..2.0 */
    fun setDanmakuScale(scale: Float) {
        _state.value = _state.value.copy(danmakuScale = scale.coerceIn(0.5f, 2f))
    }

    /** 手势进度（UI 层渲染提示条用）：deltaX 横滑快进 */
    fun onGestureSeek(deltaX: Float, widthPx: Float) {
        if (widthPx <= 0f || duration() <= 0L) return
        val ratio = deltaX / widthPx
        val deltaMs = (ratio * duration() * 2).toLong()  // 全屏横滑≈2倍时长
        _state.value = _state.value.copy(gestureSeekDeltaMs = deltaMs)
    }

    fun commitGestureSeek() {
        val d = _state.value.gestureSeekDeltaMs
        if (d != 0L) {
            seekTo((_state.value.currentTime + d).coerceIn(0L, duration()))
            _state.value = _state.value.copy(gestureSeekDeltaMs = 0L)
        }
    }

    /** 左半屏竖滑调系统亮度（0..1），右半屏调音量 */
    fun onVerticalDrag(leftSide: Boolean, deltaY: Float, heightPx: Float) {
        if (heightPx <= 0f) return
        val delta = -deltaY / heightPx
        if (leftSide) {
            val nv = (_state.value.brightness + delta).coerceIn(0.05f, 1f)
            _state.value = _state.value.copy(brightness = nv)
        } else {
            setVolume((_state.value.volume + delta).coerceIn(0f, 1f))
        }
    }

    private fun duration(): Long = _player?.duration?.takeIf { it > 0 } ?: _state.value.duration
}
