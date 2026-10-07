package com.example.piliai

/**
 * Native audio normalization backed by Rust implementation.
 * Replaces the Kotlin AudioNormalizationProcessor with a Rust dynaudnorm-style implementation.
 */
internal object AudioNativeLib {

    /** 审核实测：.so 缺失时不崩溃 */
    val loaded: Boolean = runCatching {
        System.loadLibrary("pilinara_native"); true
    }.getOrDefault(false)

    init {
        if (!loaded) android.util.Log.w("PiliNative", "pilinara_native.so 不可用，音频归一化跳过")
    }

    private external fun normalize(input: ByteArray, channels: Int): ByteArray

    /**
     * Normalize interleaved 16-bit PCM audio samples.
     * @param samples interleaved PCM samples (2 bytes per sample)
     * @param channels number of audio channels
     * @return normalized samples in the same format
     */
    fun normalizeAudio(samples: ByteArray, channels: Int): ByteArray {
        return normalize(samples, channels)
    }
}
