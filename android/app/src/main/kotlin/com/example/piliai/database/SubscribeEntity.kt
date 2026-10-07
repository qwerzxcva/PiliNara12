package com.example.piliai.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 订阅源（Animeko「添加订阅源链接」移植）
 *
 * 一条记录 = 一个外部订阅源（Bangumi 剧集源 / RSS / 自定义 JSON）。
 * 用户粘贴链接后保存，订阅页拉取其条目渲染封面与标题，播放时再从
 * 条目链接解析真实播放地址，交给 PiliNara 播放器（Media3 ExoPlayer）。
 */
@Entity(
    tableName = "subscribe_source",
    indices = [Index(value = ["url"], unique = true)]
)
data class SubscribeSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String = "",              // 源显示名（用户可改 / 解析自标题）
    val url: String = "",               // 订阅源链接（唯一）
    val type: Int = TYPE_BANGUMI,       // 源类型
    val cover: String = "",             // 源封面（可选）
    val lastSyncAt: Long = 0L,          // 上次成功同步时间
    val lastError: String? = null,      // 上次同步错误（成功时置空）
    val enabled: Boolean = true,        // 是否启用（停用后不参与订阅页聚合）
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_BANGUMI = 0   // Bangumi 剧集源
        const val TYPE_RSS = 1       // RSS / Atom
        const val TYPE_JSON = 2      // 自定义 JSON 源
    }

    val typeLabel: String
        get() = when (type) {
            TYPE_RSS -> "RSS"
            TYPE_JSON -> "JSON"
            else -> "Bangumi"
        }
}

@Dao
interface SubscribeSourceDao {
    @Query("SELECT * FROM subscribe_source ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SubscribeSourceEntity>>

    @Query("SELECT * FROM subscribe_source WHERE enabled = 1 ORDER BY createdAt DESC")
    fun observeEnabled(): Flow<List<SubscribeSourceEntity>>

    @Query("SELECT * FROM subscribe_source WHERE id = :id")
    suspend fun getById(id: Long): SubscribeSourceEntity?

    @Query("SELECT * FROM subscribe_source WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): SubscribeSourceEntity?

    @Upsert
    suspend fun upsert(entity: SubscribeSourceEntity): Long

    @Query("DELETE FROM subscribe_source WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subscribe_source WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("UPDATE subscribe_source SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query(
        "UPDATE subscribe_source SET name = :name, cover = :cover, " +
            "lastSyncAt = :at, lastError = :err WHERE id = :id"
    )
    suspend fun updateSyncResult(id: Long, name: String, cover: String, at: Long, err: String?)
}

/**
 * 订阅源解析出的条目（供订阅页渲染封面/标题，播放时取地址）
 *
 * 说明：条目是远端数据的快照，按 (sourceId, link) 唯一，刷新时 Upsert 覆盖。
 */
@Entity(
    tableName = "subscribe_item",
    indices = [Index(value = ["sourceId", "link"], unique = true)]
)
data class SubscribeItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sourceId: Long = 0L,            // 所属订阅源
    val title: String = "",
    val cover: String = "",             // 封面图 URL（订阅页渲染用）
    val link: String = "",              // 条目链接（取播放地址用）
    val desc: String = "",              // 简介/内容摘要
    val pubAt: Long = 0L,               // 发布时间（毫秒，0=未知）
    val episode: String = "",           // 分集标签（如 "第3集"）
    val sourceName: String = "",        // 冗余源名（列表分组/展示，避免联表）
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface SubscribeItemDao {
    @Query("SELECT * FROM subscribe_item ORDER BY pubAt DESC, updatedAt DESC")
    fun observeAll(): Flow<List<SubscribeItemEntity>>

    @Query("SELECT * FROM subscribe_item WHERE sourceId = :sourceId ORDER BY pubAt DESC")
    fun observeBySource(sourceId: Long): Flow<List<SubscribeItemEntity>>

    /**
     * 一次性取该源全部条目（审核轮22：同步时查既有主键用，
     * 避免 Upsert 每次新建 id=0 的行而撞唯一索引）
     */
    @Query("SELECT * FROM subscribe_item WHERE sourceId = :sourceId")
    suspend fun getBySourceOnce(sourceId: Long): List<SubscribeItemEntity>

    @Query("SELECT * FROM subscribe_item WHERE id = :id")
    suspend fun getById(id: Long): SubscribeItemEntity?

    @Query("SELECT * FROM subscribe_item WHERE link = :link LIMIT 1")
    suspend fun getByLink(link: String): SubscribeItemEntity?

    @Upsert
    suspend fun upsert(entity: SubscribeItemEntity): Long

    @Upsert
    suspend fun upsertAll(list: List<SubscribeItemEntity>)

    @Query("DELETE FROM subscribe_item WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: Long)

    @Query("DELETE FROM subscribe_item WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
