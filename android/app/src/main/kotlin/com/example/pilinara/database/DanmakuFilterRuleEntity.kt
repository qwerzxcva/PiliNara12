package com.example.pilinara.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "danmaku_filter_rule")
data class DanmakuFilterRuleEntity(
    @PrimaryKey
    val key: String = "danmakuFilterRules",
    
    val dmFilterStrings: String? = null,
    val dmRegExpPatterns: String? = null,
    val dmUids: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
