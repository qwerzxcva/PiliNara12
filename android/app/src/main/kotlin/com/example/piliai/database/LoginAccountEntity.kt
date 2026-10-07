package com.example.piliai.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "login_account")
data class LoginAccountEntity(
    @PrimaryKey
    val mid: String,
    
    val cookiesJson: String,
    val accessKey: String? = null,
    val refresh: String? = null,
    val typeIndexes: String? = null,
    val isLogin: Boolean = true,
    val activated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
