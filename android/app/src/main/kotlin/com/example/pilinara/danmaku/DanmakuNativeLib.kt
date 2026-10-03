package com.example.pilinara.danmaku

/**
 * Kotlin FFI bindings for Danmaku Native Library
 */
class DanmakuNativeLib {
    
    init {
        System.loadLibrary("pilinara_native")
    }
    
    private var mergerPtr: Long = 0
    
    data class DanmakuConfig(
        val windowSeconds: Double = 5.0,
        val maxDistance: Double = 1.5,
        val maxCosine: Double = 0.95,
        val usePinyin: Boolean = false
    )
    
    fun create(config: DanmakuConfig): Long {
        return nativeCreate(
            config.windowSeconds,
            config.maxDistance,
            config.maxCosine,
            if (config.usePinyin) 1 else 0
        )
    }
    
    fun destroy(ptr: Long) {
        if (ptr != 0L) {
            nativeDestroy(ptr)
        }
    }
    
    fun loadPinyinDict(ptr: Long, dictData: ByteArray): Int {
        return nativeLoadPinyinDict(ptr, dictData)
    }
    
    private external fun nativeCreate(
        windowSeconds: Double,
        maxDistance: Double,
        maxCosine: Double,
        usePinyin: Int
    ): Long
    
    private external fun nativeDestroy(ptr: Long)
    
    private external fun nativeLoadPinyinDict(
        ptr: Long,
        dictData: ByteArray
    ): Int
}
