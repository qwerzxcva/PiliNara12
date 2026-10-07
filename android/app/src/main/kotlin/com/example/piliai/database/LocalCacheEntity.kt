package com.example.piliai.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_cache")
data class LocalCacheEntity(
    @PrimaryKey
    val key: String,
    
    val valueString: String? = null,
    val valueInt: Int? = null,
    val valueLong: Long? = null,
    val valueDouble: Double? = null,
    val valueBool: Boolean? = null,
    val valueJson: String? = null,
    val valueType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
