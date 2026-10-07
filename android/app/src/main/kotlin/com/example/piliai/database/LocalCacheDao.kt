package com.example.piliai.database

import androidx.room.*

@Dao
interface LocalCacheDao {
    @Query("SELECT * FROM local_cache WHERE key = :key")
    suspend fun getByKey(key: String): LocalCacheEntity?
    
    @Query("SELECT * FROM local_cache")
    suspend fun getAll(): List<LocalCacheEntity>
    
    @Upsert
    suspend fun upsert(entity: LocalCacheEntity)
    
    @Delete
    suspend fun delete(entity: LocalCacheEntity)
    
    @Query("DELETE FROM local_cache WHERE key IN (:keys)")
    suspend fun deleteByKeyList(keys: List<String>)
    
    @Query("DELETE FROM local_cache")
    suspend fun deleteAll()
    
    @Transaction
    suspend fun upsertAll(entities: List<LocalCacheEntity>) {
        entities.forEach { upsert(it) }
    }
}
