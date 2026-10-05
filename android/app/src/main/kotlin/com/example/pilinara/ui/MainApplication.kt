package com.example.pilinara

import android.app.Application
import com.example.pilinara.data.remote.AccountSession
import com.example.pilinara.database.DatabaseInitializer
import kotlinx.coroutines.launch

/**
 * Main Application class
 * Initializes all native and database components
 */
class MainApplication : Application() {

    // 审核5：Application 级常驻作用域（SupervisorJob 防子协程异常互相取消）
    private val appScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        DatabaseInitializer().initialize(this)

        // 批次审核40：全局兜底——未捕获协程异常记日志防静默崩溃
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            android.util.Log.e("PiliNaraCrash", "Uncaught on ${t.name}", e)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        // 进程级 Application 上下文，供无 Context 层（LoginRepository 等）取 Room
        AppContext.init(this)

        // 弹幕屏蔽规则启动 warmup（缓存到内存供渲染过滤）
        com.example.pilinara.ui.settings.DanmakuBlockViewModel.warmup(
            com.example.pilinara.database.PiliNaraDatabase.getDatabase(this)
        )

        // 匿名启动即取 buvid3（风控接口强依赖），协程后台执行
        appScope.launch {
            AccountSession.ensureBuvid()
        }

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
