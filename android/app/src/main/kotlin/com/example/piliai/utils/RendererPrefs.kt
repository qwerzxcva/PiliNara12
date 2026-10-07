package com.example.piliai.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 渲染器偏好的进程内同步缓存（Kazumi「渲染器切换」移植）
 *
 * 背景：DataStore 只能异步读，而 PlayerView 的 factory 在主线程同步执行，
 * 在 factory 里 runBlocking 会卡帧（尤其冷启动时 DataStore 首次读涉及磁盘 IO）。
 *
 * 方案：用一个进程级单例缓存，由 Application 启动时异步预热一次；
 * 之后设置变更时由 ViewModel 调用 setRenderer 更新缓存，保证
 * 「下次进入播放器即生效」且主线程零阻塞。
 */
object RendererPrefs {

    @Volatile
    var useTextureView: Boolean = false
        private set

    /** Kazumi：低延迟音频（同步缓存，供 ExoPlayer.Builder 读取） */
    @Volatile
    var lowLatencyAudio: Boolean = false
        private set

    private var inited = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Application 启动时调用：异步预热 + 持续同步 */
    fun init(context: android.content.Context) {
        if (inited) return
        inited = true
        val storage = StorageManager(context.applicationContext)
        scope.launch {
            storage.rendererFlow.collectLatest { v ->
                useTextureView = (v == 1)
            }
        }
        scope.launch {
            storage.lowLatencyAudioFlow.collectLatest { v ->
                lowLatencyAudio = v
            }
        }
    }

    /** 设置变更时同步更新缓存（下次创建 PlayerView 生效） */
    fun update(v: Int) {
        useTextureView = (v == 1)
    }

    /** 更新低延迟音频缓存 */
    fun updateLowLatency(v: Boolean) {
        lowLatencyAudio = v
    }
}
