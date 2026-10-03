package com.example.pilinara.database

import android.content.Context

/**
 * 数据库初始化器
 * 提供应用启动时的数据库初始化逻辑
 */
class DatabaseInitializer {
    
    /**
     * 初始化数据库
     * 应在 Application.onCreate() 中调用
     */
    fun initialize(context: Context) {
        // 获取数据库实例以触发创建
        val db = PiliNaraDatabase.getDatabase(context)
        
        // 可以在这里执行初始数据迁移
        val migrationManager = DataMigrationManager(context)
        // migrationManager.migrateAll()
    }
}
