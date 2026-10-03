package com.example.pilinara.database

import androidx.room.*

@Dao
interface DanmakuFilterRuleDao {
    @Query("SELECT * FROM danmaku_filter_rule WHERE key = :key")
    suspend fun getByKey(key: String): DanmakuFilterRuleEntity?
    
    @Upsert
    suspend fun upsert(entity: DanmakuFilterRuleEntity)
    
    @Delete
    suspend fun delete(entity: DanmakuFilterRuleEntity)
}
