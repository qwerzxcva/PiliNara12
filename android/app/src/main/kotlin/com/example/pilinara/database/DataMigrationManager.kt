package com.example.pilinara.database

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据迁移管理器
 * 用于将 Flutter Hive 数据迁移到 Android Room
 */
class DataMigrationManager(private val context: Context) {
    
    private val database: PiliNaraDatabase = PiliNaraDatabase.getDatabase(context)
    private val gson = Gson()
    
    /**
     * 将 Hive UserInfoData JSON 转换为 Room 实体
     */
    fun convertUserInfoToJson(userInfo: Map<String, Any?>): String {
        return gson.toJson(userInfo)
    }
    
    /**
     * 解析用户信息 JSON
     */
    fun parseUserInfoJson(json: String): Map<String, Any?> {
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * 迁移用户信息
     */
    suspend fun migrateUserInfo(userInfoMap: Map<String, Any?>) {
        val entity = UserInfoEntity(
            key = "userInfoCache",
            isLogin = userInfoMap["isLogin"] as? Boolean,
            mid = userInfoMap["mid"] as? Int,
            uname = userInfoMap["uname"] as? String,
            face = userInfoMap["face"] as? String,
            vipStatus = userInfoMap["vipStatus"] as? Int,
            vipType = userInfoMap["vipType"] as? Int,
            money = userInfoMap["money"] as? Double,
            scores = userInfoMap["scores"] as? Int
        )
        database.userInfoDao().upsert(entity)
    }
    
    /**
     * 迁移登录账号
     */
    suspend fun migrateLoginAccount(mid: String, cookiesJson: String, accessKey: String? = null, refresh: String? = null) {
        val entity = LoginAccountEntity(
            mid = mid,
            cookiesJson = cookiesJson,
            accessKey = accessKey,
            refresh = refresh
        )
        database.loginAccountDao().upsert(entity)
    }
    
    /**
     * 迁移设置项
     */
    suspend fun migrateSetting(key: String, value: Any?) {
        when (value) {
            is String -> {
                val entity = SettingEntity(key = key, valueString = value, valueType = "string")
                database.settingDao().upsert(entity)
            }
            is Int -> {
                val entity = SettingEntity(key = key, valueInt = value, valueType = "int")
                database.settingDao().upsert(entity)
            }
            is Boolean -> {
                val entity = SettingEntity(key = key, valueBool = value, valueType = "bool")
                database.settingDao().upsert(entity)
            }
            is Double -> {
                val entity = SettingEntity(key = key, valueDouble = value, valueType = "double")
                database.settingDao().upsert(entity)
            }
            else -> {
                val entity = SettingEntity(key = key, valueJson = gson.toJson(value), valueType = "json")
                database.settingDao().upsert(entity)
            }
        }
    }
}
