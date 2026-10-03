package com.example.pilinara.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_info")
data class UserInfoEntity(
    @PrimaryKey
    val key: String = "userInfoCache",
    
    val isLogin: Boolean? = null,
    val emailVerified: Int? = null,
    val face: String? = null,
    val levelInfoCurrentLevel: Int? = null,
    val levelInfoCurrentMin: Int? = null,
    val levelInfoCurrentExp: Int? = null,
    val levelInfoNextExp: Int? = null,
    val mid: Int? = null,
    val mobileVerified: Int? = null,
    val money: Double? = null,
    val moral: Int? = null,
    val scores: Int? = null,
    val uname: String? = null,
    val vipDueDate: Int? = null,
    val vipStatus: Int? = null,
    val vipType: Int? = null,
    val vipPayType: Int? = null,
    val vipThemeType: Int? = null,
    val vipAvatarSub: Int? = null,
    val vipNicknameColor: String? = null,
    val hasShop: Boolean? = null,
    val shopUrl: String? = null,
    val isSeniorMember: Int? = null,
    val official: String? = null,
    val officialVerify: String? = null,
    val pendant: String? = null,
    val vipLabel: String? = null,
    val wallet: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
