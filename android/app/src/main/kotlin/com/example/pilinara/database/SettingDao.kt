package com.example.pilinara.database

import androidx.room.*

@Dao
interface SettingDao {
    @Query("SELECT * FROM setting WHERE key = :key")
    suspend fun getByKey(key: String): SettingEntity?
    
    @Query("SELECT * FROM setting")
    suspend fun getAll(): List<SettingEntity>
    
    @Upsert
    suspend fun upsert(entity: SettingEntity)
    
    @Delete
    suspend fun delete(entity: SettingEntity)
    
    @Query("DELETE FROM setting WHERE key IN (:keys)")
    suspend fun deleteByKeyList(keys: List<String>)
    
    @Query("DELETE FROM setting")
    suspend fun deleteAll()
    
    @Transaction
    suspend fun upsertAll(entities: List<SettingEntity>) {
        entities.forEach { upsert(it) }
    }
}
