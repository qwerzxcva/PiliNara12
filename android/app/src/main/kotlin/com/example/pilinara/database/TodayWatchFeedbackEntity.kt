package com.example.pilinara.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "today_watch_feedback")
data class TodayWatchFeedbackEntity(
    @PrimaryKey
    val key: String = "today_watch_feedback_v1",
    
    val dislikedBvidsJson: String? = null,
    val dislikedCreatorMidsJson: String? = null,
    val dislikedKeywordsJson: String? = null,
    val recentDislikedVideosJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
