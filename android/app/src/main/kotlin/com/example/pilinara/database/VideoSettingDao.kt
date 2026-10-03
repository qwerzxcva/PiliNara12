package com.example.pilinara.database

import androidx.room.*

@Dao
interface VideoSettingDao {
    @Query("SELECT * FROM video_setting WHERE key = :key")
    suspend fun getByKey(key: String): VideoSettingEntity?
    
    @Query("SELECT * FROM video_setting")
    suspend fun getAll(): List<VideoSettingEntity>
    
    @Upsert
    suspend fun upsert(entity: VideoSettingEntity)
    
    @Delete
    suspend fun delete(entity: VideoSettingEntity)
    
    @Query("DELETE FROM video_setting WHERE key IN (:keys)")
    suspend fun deleteByKeyList(keys: List<String>)
    
    @Query("DELETE FROM video_setting")
    suspend fun deleteAll()
    
    @Transaction
    suspend fun upsertAll(entities: List<VideoSettingEntity>) {
        entities.forEach { upsert(it) }
    }
}
