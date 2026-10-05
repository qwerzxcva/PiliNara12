package com.example.pilinara.data.repository

import android.content.Context
import com.example.pilinara.database.DownloadItemEntity
import com.example.pilinara.data.remote.BiliApiClient
import com.example.pilinara.data.model.toParsed
import com.example.pilinara.database.PiliNaraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * 离线下载管理器（批次I）：
 * 详情取 cid → playurl(wbi+Rust 选流取 video/audio baseUrl) → 双流下载到
 * PiliNara/Downloads/{bvid}/ → Room 记录进度/状态 → 离线播放页直接播本地文件。
 */
object DownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    private fun dao(ctx: Context) = PiliNaraDatabase.getDatabase(ctx).downloadItemDao()

    fun observeAll(ctx: Context): Flow<List<DownloadItemEntity>> =
        dao(ctx).observeAll()

    fun observeDone(ctx: Context): Flow<List<DownloadItemEntity>> =
        dao(ctx).observeAll().map { list -> list.filter { it.state == DownloadItemEntity.STATE_DONE } }

    suspend fun isDownloaded(ctx: Context, bvid: String): Boolean =
        dao(ctx).getByBvid(bvid)?.state == DownloadItemEntity.STATE_DONE

    fun download(
        context: Context,
        bvid: String,
        title: String = "",
        cover: String = "",
        ownerName: String = ""
    ) {
        if (running.containsKey(bvid)) return
        val appCtx = context.applicationContext
        running[bvid] = scope.launch {
            val dao = dao(appCtx)
            try {
                // 恢复/重试时保留原记录（进度/cid/标题），仅置为进行中
                val prev = dao.getByBvid(bvid)
                dao.upsert((prev ?: DownloadItemEntity(bvid = bvid)).copy(
                    title = title.ifEmpty { prev?.title.orEmpty() },
                    cover = cover.ifEmpty { prev?.cover.orEmpty() },
                    ownerName = ownerName.ifEmpty { prev?.ownerName.orEmpty() },
                    state = DownloadItemEntity.STATE_RUNNING, error = null
                ))

                val api = BiliApiClient()
                val repo = VideoRepository(api)
                // 1) 详情取 cid
                val detail = repo.getVideoDetail(bvid).getOrNull()?.data
                    ?: throw IllegalStateException("详情获取失败")
                val cid = detail.cid
                val duration = detail.duration.toLong()
                // 2) playurl + Rust 选流（清晰度跟随 DataStore 设置）
                val qn = com.example.pilinara.utils.StorageManager(appCtx).videoQualityFlow.first()
                    .let { when (it) { "1080p" -> 80; "720p" -> 64; "480p" -> 32; else -> 64 } }
                val play = repo.getPlayUrl(bvid, cid, qn = qn).getOrNull()
                    ?: throw IllegalStateException("playurl 获取失败")
                val videoUrl = play.second
                val audioUrl = play.third
                if (videoUrl.isNullOrEmpty()) throw IllegalStateException("未取到视频流")

                // 3) 目录
                val dir = File(File(appCtx.getExternalFilesDir(null), "PiliNara/Downloads"), bvid)
                if (!dir.exists()) dir.mkdirs()

                // 4) 双流下载（video 必需，audio 可选）
                val videoFile = File(dir, "video.m4s")
                downloadTo(videoUrl, videoFile) { p ->
                    launch { dao.upsert(current(dao, bvid).copy(progress = p * 0.8f)) }
                }
                val audioFile = File(dir, "audio.m4s")
                var audioSize = 0L
                if (!audioUrl.isNullOrEmpty()) {
                    downloadTo(audioUrl, audioFile) { p ->
                        launch { dao.upsert(current(dao, bvid).copy(progress = 0.8f + p * 0.2f)) }
                    }
                    audioSize = audioFile.length()
                }

                dao.upsert(DownloadItemEntity(
                    bvid = bvid, cid = cid, title = title.ifEmpty { detail.title },
                    cover = cover.ifEmpty { detail.pic }, ownerName = ownerName.ifEmpty { detail.owner?.name.orEmpty() },
                    durationSec = duration,
                    videoPath = videoFile.absolutePath,
                    audioPath = if (audioSize > 0) audioFile.absolutePath else "",
                    videoSize = videoFile.length(), audioSize = audioSize,
                    state = DownloadItemEntity.STATE_DONE, progress = 1f
                ))
            } catch (e: Exception) {
                dao.upsert(current(dao, bvid).copy(
                    state = DownloadItemEntity.STATE_FAILED, error = e.message
                ))
            } finally {
                running.remove(bvid)
            }
        }
    }

    private suspend fun current(dao: com.example.pilinara.database.DownloadItemDao, bvid: String) =
        dao.getByBvid(bvid) ?: DownloadItemEntity(bvid = bvid)

    fun cancel(context: Context, bvid: String) {
        running.remove(bvid)?.cancel()
        scope.launch {
            val item = dao(context).getByBvid(bvid)
            if (item != null && item.state != DownloadItemEntity.STATE_DONE) {
                dao(context).upsert(item.copy(state = DownloadItemEntity.STATE_FAILED, error = "已取消"))
            }
        }
    }

    /** 暂停：缓存当前流 URL 供续传（流 URL 有时效，resume 时若过期会重新 playurl） */
    fun pause(context: Context, bvid: String) {
        running.remove(bvid)?.cancel()
        scope.launch {
            val d = dao(context)
            val item = d.getByBvid(bvid) ?: return@launch
            if (item.state == DownloadItemEntity.STATE_RUNNING) {
                d.upsert(item.copy(state = DownloadItemEntity.STATE_PAUSED, error = "已暂停"))
            }
        }
    }

    /** 恢复：断点续传（HTTP Range 追加），URL 过期则重走 playurl */
    fun resume(context: Context, bvid: String) {
        download(context, bvid) // download 内部检测半成品文件走 Range 续传
    }

    /**
     * 批量下载指定分P（批次L10）：调用方已持有 cid/标题（分P面板/番剧选集）。
     * 存储键 bvid_pN；单P仍走 download()（键=bvid）。
     */
    fun downloadPart(
        context: Context,
        bvid: String,
        cid: Long,
        page: Int,
        pageLabel: String,
        durationSec: Long = 0L,
        title: String = "",
        cover: String = "",
        ownerName: String = ""
    ) {
        val key = "${bvid}_p$page"
        if (running.containsKey(key)) return
        val appCtx = context.applicationContext
        running[key] = scope.launch {
            val dao = dao(appCtx)
            try {
                val prev = dao.getByBvid(key)
                dao.upsert((prev ?: DownloadItemEntity(bvid = key)).copy(
                    cid = cid, title = title, cover = cover, ownerName = ownerName,
                    durationSec = durationSec, pageLabel = pageLabel,
                    state = DownloadItemEntity.STATE_RUNNING, error = null
                ))
                val qn = com.example.pilinara.utils.StorageManager(appCtx).videoQualityFlow.first()
                    .let { when (it) { "1080p" -> 80; "720p" -> 64; "480p" -> 32; else -> 64 } }
                val play = VideoRepository(BiliApiClient()).getPlayUrl(bvid, cid, qn = qn).getOrNull()
                    ?: throw IllegalStateException("playurl 获取失败")
                val videoUrl = play.second
                val audioUrl = play.third
                if (videoUrl.isNullOrEmpty()) throw IllegalStateException("未取到视频流")

                val dir = File(File(appCtx.getExternalFilesDir(null), "PiliNara/Downloads"), key)
                if (!dir.exists()) dir.mkdirs()

                val videoFile = File(dir, "video.m4s")
                downloadTo(videoUrl, videoFile) { p ->
                    launch { dao.upsert(current(dao, key).copy(progress = p * 0.8f)) }
                }
                val audioFile = File(dir, "audio.m4s")
                var audioSize = 0L
                if (!audioUrl.isNullOrEmpty()) {
                    downloadTo(audioUrl, audioFile) { p ->
                        launch { dao.upsert(current(dao, key).copy(progress = 0.8f + p * 0.2f)) }
                    }
                    audioSize = audioFile.length()
                }
                dao.upsert(current(dao, key).copy(
                    videoPath = videoFile.absolutePath,
                    audioPath = if (audioSize > 0) audioFile.absolutePath else "",
                    videoSize = videoFile.length(), audioSize = audioSize,
                    state = DownloadItemEntity.STATE_DONE, progress = 1f, error = null
                ))
                // 弹幕离线（批次L11）：下载完成后抓取弹幕存为本地 JSON
                runCatching {
                    val cid0 = dao.getByBvid(key)?.cid ?: 0L
                    if (cid0 > 0L) {
                        val resp = BiliApiClient().getDanmaku(cid0).getOrNull()
                        val list = resp?.data.orEmpty().map { d ->
                            val pp = d.toParsed()
                            mapOf("t" to (pp.timestamp * 1000).toLong(), "c" to pp.content,
                                "col" to (pp.color or 0xFF000000.toInt()), "fs" to pp.fontSize)
                        }
                        File(dir, "danmaku.json").writeText(
                            com.google.gson.Gson().toJson(list)
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                dao.upsert(current(dao, key).copy(state = DownloadItemEntity.STATE_PAUSED))
                throw e
            } catch (e: Exception) {
                dao.upsert(current(dao, key).copy(
                    state = DownloadItemEntity.STATE_FAILED, error = e.message
                ))
            } finally {
                running.remove(key)
            }
        }
    }

    /**
     * 批量下载番剧分集（批次L13）：bvid 传 "ep{id}" 虚拟键，走 pgc playurl。
     * 存储键 ep{id}_p{page}。
     */
    fun downloadPgcPart(
        context: Context,
        epId: Long,
        cid: Long,
        page: Int,
        pageLabel: String,
        durationSec: Long = 0L,
        title: String = "",
        cover: String = "",
        ownerName: String = ""
    ) {
        val key = "ep${epId}_p$page"
        if (running.containsKey(key)) return
        val appCtx = context.applicationContext
        running[key] = scope.launch {
            val dao = dao(appCtx)
            try {
                val prev = dao.getByBvid(key)
                dao.upsert((prev ?: DownloadItemEntity(bvid = key)).copy(
                    cid = cid, title = title, cover = cover, ownerName = ownerName,
                    durationSec = durationSec, pageLabel = pageLabel,
                    state = DownloadItemEntity.STATE_RUNNING, error = null
                ))
                val qn = com.example.pilinara.utils.StorageManager(appCtx).videoQualityFlow.first()
                    .let { when (it) { "1080p" -> 80; "720p" -> 64; "480p" -> 32; else -> 64 } }
                val play = VideoRepository(BiliApiClient()).getPgcPlayUrl(epId, cid, qn = qn).getOrNull()
                    ?: throw IllegalStateException("番剧 playurl 获取失败")
                val videoUrl = play.second
                val audioUrl = play.third
                if (videoUrl.isNullOrEmpty()) throw IllegalStateException("未取到视频流（可能为大会员专享）")

                val dir = File(File(appCtx.getExternalFilesDir(null), "PiliNara/Downloads"), key)
                if (!dir.exists()) dir.mkdirs()

                val videoFile = File(dir, "video.m4s")
                downloadTo(videoUrl, videoFile) { p ->
                    launch { dao.upsert(current(dao, key).copy(progress = p * 0.8f)) }
                }
                val audioFile = File(dir, "audio.m4s")
                var audioSize = 0L
                if (!audioUrl.isNullOrEmpty()) {
                    downloadTo(audioUrl, audioFile) { p ->
                        launch { dao.upsert(current(dao, key).copy(progress = 0.8f + p * 0.2f)) }
                    }
                    audioSize = audioFile.length()
                }
                dao.upsert(current(dao, key).copy(
                    videoPath = videoFile.absolutePath,
                    audioPath = if (audioSize > 0) audioFile.absolutePath else "",
                    videoSize = videoFile.length(), audioSize = audioSize,
                    state = DownloadItemEntity.STATE_DONE, progress = 1f, error = null
                ))
                runCatching {
                    val resp = BiliApiClient().getDanmaku(cid).getOrNull()
                    val list = resp?.data.orEmpty().map { d ->
                        val pp = d.toParsed()
                        mapOf("t" to (pp.timestamp * 1000).toLong(), "c" to pp.content,
                            "col" to (pp.color or 0xFF000000.toInt()), "fs" to pp.fontSize)
                    }
                    File(dir, "danmaku.json").writeText(com.google.gson.Gson().toJson(list))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                dao.upsert(current(dao, key).copy(state = DownloadItemEntity.STATE_PAUSED))
                throw e
            } catch (e: Exception) {
                dao.upsert(current(dao, key).copy(
                    state = DownloadItemEntity.STATE_FAILED, error = e.message
                ))
            } finally {
                running.remove(key)
            }
        }
    }

    /** 删除离线项（记录 + 文件） */
    fun delete(context: Context, bvid: String) {
        cancel(context, bvid)
        scope.launch {
            val d = dao(context)
            val item = d.getByBvid(bvid)
            d.delete(bvid)
            item?.videoPath?.let { File(it).parentFile?.deleteRecursively() }
        }
    }

    /** 离线播放用的本地 URI 对 */
    data class LocalPlayback(val videoPath: String, val audioPath: String)

    suspend fun getLocalPlayback(context: Context, bvid: String): LocalPlayback? {
        val item = dao(context).getByBvid(bvid) ?: return null
        if (item.state != DownloadItemEntity.STATE_DONE) return null
        return LocalPlayback(item.videoPath, item.audioPath)
    }

    /** 读取离线弹幕（批次L11：下载时缓存的 danmaku.json） */
    fun localDanmaku(videoPath: String): List<Map<String, Any?>> {
        val f = File(File(videoPath).parentFile, "danmaku.json")
        if (!f.exists()) return emptyList()
        return runCatching {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                java.util.List::class.java,
                com.google.gson.reflect.TypeToken.getParameterized(
                    java.util.Map::class.java, String::class.java, Any::class.java
                ).type
            ).type
            @Suppress("UNCHECKED_CAST")
            com.google.gson.Gson().fromJson<List<Map<String, Any?>>>(f.readText(), type)
        }.getOrDefault(emptyList())
    }

    // ---------- 基础下载（带进度回调 0..1） ----------

    private suspend fun downloadTo(
        url: String,
        target: File,
        onProgress: suspend (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        var already = if (target.exists()) target.length() else 0L
        val conn = URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.setRequestProperty("Referer", "https://www.bilibili.com/")
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
        var resumeMode = false
        if (already > 0) {
            conn.setRequestProperty("Range", "bytes=$already-")
            resumeMode = conn.responseCode == 206
            if (!resumeMode) already = 0L  // 服务器不支持 Range → 重下
        }
        val total = conn.contentLengthLong.takeIf { it > 0 }?.let { it + already } ?: -1L
        if (!resumeMode) target.delete()
        conn.inputStream.use { input ->
            java.io.FileOutputStream(target, resumeMode).use { out ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                var done = already
                var lastPct = -1
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                    done += read
                    if (total > 0) {
                        val pct = (done * 100 / total).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            onProgress(pct / 100f)
                        }
                    }
                }
            }
        }
    }
}
