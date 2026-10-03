package com.example.pilinara.database

import androidx.room.*

@Dao
interface TodayWatchFeedbackDao {
    @Query("SELECT * FROM today_watch_feedback WHERE key = :key")
    suspend fun getByKey(key: String): TodayWatchFeedbackEntity?
    
    @Upsert
    suspend fun upsert(entity: TodayWatchFeedbackEntity)
    
    @Delete
    suspend fun delete(entity: TodayWatchFeedbackEntity)
}
