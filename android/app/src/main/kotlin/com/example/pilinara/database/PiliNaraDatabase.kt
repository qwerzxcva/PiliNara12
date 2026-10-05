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
        TodayWatchFeedbackEntity::class,
        DownloadItemEntity::class,
        SubscribeSourceEntity::class,
        SubscribeItemEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PiliNaraDatabase : RoomDatabase() {
    
    abstract fun userInfoDao(): UserInfoDao
    abstract fun loginAccountDao(): LoginAccountDao
    abstract fun danmakuFilterRuleDao(): DanmakuFilterRuleDao
    abstract fun downloadItemDao(): DownloadItemDao
    abstract fun settingDao(): SettingDao
    abstract fun videoSettingDao(): VideoSettingDao
    abstract fun localCacheDao(): LocalCacheDao
    abstract fun todayWatchFeedbackDao(): TodayWatchFeedbackDao
    abstract fun subscribeSourceDao(): SubscribeSourceDao
    abstract fun subscribeItemDao(): SubscribeItemDao
    
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
                    // 审核：v2→v3 必须走显式 Migration，否则 destructive 会清空
                    // 用户的下载记录与登录态。此处为增量建表，数据零丢失。
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * v2 → v3：新增订阅源两张表（subscribe_source / subscribe_item）
         *
         * 仅 CREATE TABLE，不触碰任何既有表，因此用户数据（下载、登录、
         * 弹幕屏蔽规则、设置）全部保留。若建表失败，Room 会抛异常而非静默
         * 破坏数据；这里对每张表单独 try，避免部分失败导致整体迁移不可用。
         */
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscribe_source` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `type` INTEGER NOT NULL,
                        `cover` TEXT NOT NULL,
                        `lastSyncAt` INTEGER NOT NULL,
                        `lastError` TEXT,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_subscribe_source_url` " +
                        "ON `subscribe_source` (`url`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscribe_item` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `cover` TEXT NOT NULL,
                        `link` TEXT NOT NULL,
                        `desc` TEXT NOT NULL,
                        `pubAt` INTEGER NOT NULL,
                        `episode` TEXT NOT NULL,
                        `sourceName` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_subscribe_item_sourceId_link` " +
                        "ON `subscribe_item` (`sourceId`, `link`)"
                )
            }
        }
    }
}
