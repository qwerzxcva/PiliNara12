package com.example.pilinara.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * PiliNara 主数据库
 * 替代 Flutter 侧的 Hive CE 存储
 */
@Database(
    entities = [
        UserInfoEntity::class,
        LoginAccountEntity::class,
        DanmakuFilterRuleEntity::class,
        SettingEntity::class,
        VideoSettingEntity::class,
        LocalCacheEntity::class,
        TodayWatchFeedbackEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PiliNaraDatabase : RoomDatabase() {
    
    abstract fun userInfoDao(): UserInfoDao
    abstract fun loginAccountDao(): LoginAccountDao
    abstract fun danmakuFilterRuleDao(): DanmakuFilterRuleDao
    abstract fun settingDao(): SettingDao
    abstract fun videoSettingDao(): VideoSettingDao
    abstract fun localCacheDao(): LocalCacheDao
    abstract fun todayWatchFeedbackDao(): TodayWatchFeedbackDao
    
    companion object {
        @Volatile
        private var INSTANCE: PiliNaraDatabase? = null
        
        fun getDatabase(context: Context): PiliNaraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PiliNaraDatabase::class.java,
                    "pilinara_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
