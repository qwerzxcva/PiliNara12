@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.piliai.piliplus

import androidx.media3.exoplayer.DefaultLoadControl

/**
 * VOD 缓冲策略工厂（移植自 piliplus ExoPlayerPlugin.createLoadControl，去除 Flutter 依赖）。
 *
 * 背景：main 里 [Media3BufferPolicy] / [resolveMedia3BufferPolicy] 一直是孤儿代码
 * （0 引用），播放器从未设置 LoadControl，等于始终用 Media3 默认缓冲。
 * 本工厂把策略真正接到 ExoPlayer.Builder.setLoadControl()。
 *
 * 语义（与上游一致）：
 * - 直播返回 null（延迟策略无法从 VOD 缓冲偏好推断，保留 Media3 默认）
 * - 小的字节目标不允许在 Media3 拿到足够可播放数据前停止加载
 * - 时间优先于体积阈值（prioritizeTimeOverSizeThresholds）
 */
internal object Media3LoadControlFactory {

    /** 默认目标缓冲字节数（4 MiB，与上游 DEFAULT_TARGET_BUFFER_BYTES 一致） */
    const val DEFAULT_TARGET_BUFFER_BYTES = 4 * 1024 * 1024

    /** 默认缓冲时长（16 秒，与上游 DEFAULT_BUFFER_DURATION_MS 一致） */
    const val DEFAULT_BUFFER_DURATION_MS = 16_000

    /**
     * 构建 VOD LoadControl。
     *
     * @param targetBufferBytes 目标缓冲字节数（会被抬到至少 64 KiB）
     * @param bufferDurationMs 期望缓冲时长（会被抬到至少 500 ms）
     * @param isLive 直播时返回 Media3 默认 LoadControl
     */
    fun create(
        targetBufferBytes: Int = DEFAULT_TARGET_BUFFER_BYTES,
        bufferDurationMs: Int = DEFAULT_BUFFER_DURATION_MS,
        isLive: Boolean = false,
    ): DefaultLoadControl {
        val policy = resolveMedia3BufferPolicy(targetBufferBytes, bufferDurationMs, isLive)
            ?: return DefaultLoadControl()
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                policy.minBufferMs,
                policy.maxBufferMs,
                policy.bufferForPlaybackMs,
                policy.bufferForPlaybackAfterRebufferMs,
            )
            .setTargetBufferBytes(policy.targetBufferBytes)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(policy.backBufferDurationMs, false)
            .build()
    }

    /** 供设置页/调试展示当前策略（与上游 description() 同义） */
    fun describe(targetBufferBytes: Int, bufferDurationMs: Int, isLive: Boolean): String {
        val policy = resolveMedia3BufferPolicy(targetBufferBytes, bufferDurationMs, isLive)
            ?: return "live/default（保留 Media3 默认缓冲）"
        return "target=%.2f MiB, min=%d ms, max=%d ms, playback=%d ms, rebuffer=%d ms, back=%d ms"
            .format(
                java.util.Locale.US,
                policy.targetBufferBytes / 1048576.0,
                policy.minBufferMs, policy.maxBufferMs,
                policy.bufferForPlaybackMs, policy.bufferForPlaybackAfterRebufferMs,
                policy.backBufferDurationMs,
            )
    }
}
