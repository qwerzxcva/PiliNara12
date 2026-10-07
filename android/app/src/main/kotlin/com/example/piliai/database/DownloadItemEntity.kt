package com.example.piliai.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 离线下载记录（批次I）
 */
@Entity(tableName = "download_item")
data class DownloadItemEntity(
    @PrimaryKey
    val bvid: String,
    val cid: Long = 0L,
    val title: String = "",
    val cover: String = "",
    val ownerName: String = "",
    val durationSec: Long = 0L,
    val videoPath: String = "",     // 相对 Downloads 目录
    val audioPath: String = "",
    val videoSize: Long = 0L,
    val audioSize: Long = 0L,
    val state: Int = STATE_PENDING, // 0待下载 1进行中 2完成 3失败 4暂停
    val progress: Float = 0f,
    val error: String? = null,
    val videoUrlCache: String = "", // 暂停时缓存的视频流 URL（续传用，有时效）
    val audioUrlCache: String = "",
    val pageLabel: String = "",     // 多P/番剧分集标签（"P2" / "第3集"），空=单P
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATE_PENDING = 0
        const val STATE_RUNNING = 1
        const val STATE_DONE = 2
        const val STATE_FAILED = 3
        const val STATE_PAUSED = 4  // 已暂停（断点续传：videoUrl 缓存于 record）
    }
}

@Dao
interface DownloadItemDao {
    @Query("SELECT * FROM download_item ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM download_item WHERE bvid = :bvid")
    suspend fun getByBvid(bvid: String): DownloadItemEntity?

    @Upsert
    suspend fun upsert(item: DownloadItemEntity)

    @Query("DELETE FROM download_item WHERE bvid = :bvid")
    suspend fun delete(bvid: String)
}
