package com.example.pilinara.data.repository

import android.content.Context
import com.example.pilinara.database.DownloadItemEntity
import com.example.pilinara.data.remote.BiliApiClient
import com.example.pilinara.database.PiliNaraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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
                dao.upsert(DownloadItemEntity(bvid = bvid, title = title, cover = cover,
                    ownerName = ownerName, state = DownloadItemEntity.STATE_RUNNING))

                val api = BiliApiClient()
                val repo = VideoRepository(api)
                // 1) 详情取 cid
                val detail = repo.getVideoDetail(bvid).getOrNull()?.data
                    ?: throw IllegalStateException("详情获取失败")
                val cid = detail.cid
                val duration = detail.duration.toLong()
                // 2) playurl + Rust 选流
                val play = repo.getPlayUrl(bvid, cid, qn = 64).getOrNull()
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

    // ---------- 基础下载（带进度回调 0..1） ----------

    private suspend fun downloadTo(
        url: String,
        target: File,
        onProgress: suspend (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.setRequestProperty("Referer", "https://www.bilibili.com/")
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
        conn.inputStream.use { input ->
            java.io.FileOutputStream(target).use { out ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                var done = 0L
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
