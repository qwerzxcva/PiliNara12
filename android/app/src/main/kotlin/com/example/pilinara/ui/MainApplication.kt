package com.example.pilinara

import android.app.Application
import com.example.pilinara.database.DatabaseInitializer

/**
 * Main Application class
 * Initializes all native and database components
 */
class MainApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        DatabaseInitializer().initialize(this)

        // 进程级 Application 上下文，供无 Context 层（LoginRepository 等）取 Room
        AppContext.init(this)
        
        // Load native libraries
        try {
            System.loadLibrary("pilinara_native")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("PiliNaraApp", "Failed to load native library: ${e.message}")
        }
    }
}

/** 全局 Application 上下文（ViewModel 之外的地方需要 Context 时使用） */
object AppContext {
    @Volatile
    private var app: Application? = null

    fun init(application: Application) { app = application }

    fun get(): Application =
        app ?: error("AppContext not initialized — MainApplication.onCreate 未执行")
}
