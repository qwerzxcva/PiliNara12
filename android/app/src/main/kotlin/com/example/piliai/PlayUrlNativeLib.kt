package com.example.piliai

/**
 * Rust 侧 playurl DASH 流选择（stage ⑤）。
 * 输入 playurl 响应 JSON，返回选中的 {video, audio, duration} JSON；失败返回 null。
 */
internal object PlayUrlNativeLib {

    /** 审核实测：.so 缺失时不崩溃，select() 返回 null 走 Kotlin 回退 */
    val loaded: Boolean = runCatching {
        System.loadLibrary("pilinara_native"); true
    }.getOrDefault(false)

    init {
        if (!loaded) android.util.Log.w("PiliNative", "pilinara_native.so 不可用，DASH 流选择回退 Kotlin")
    }

    private external fun selectStreams(body: String, targetQn: Int): String?

    /**
     * 从 playurl 响应里选最优 video+audio 流。
     * @return JSON 字符串（serde_json Stream 结构），null 表示解析/选择失败（调用方回退到 Kotlin 逻辑）
     */
    fun select(body: String, targetQn: Int = 80): String? = runCatching {
        selectStreams(body, targetQn)
    }.getOrNull()
}
