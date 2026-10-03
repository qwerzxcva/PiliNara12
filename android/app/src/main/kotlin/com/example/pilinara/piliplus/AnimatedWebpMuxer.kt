package com.example.pilinara.piliplus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Animated WebP muxer using native Rust backend
 * Replaces Flutter mpv WebP conversion
 */
class AnimatedWebpMuxer {
    
    private var encoderPtr: Long = 0
    
    init {
        System.loadLibrary("pilinara_native")
    }
    
    data class Frame(
        val bitmap: Bitmap,
        val durationMs: Int
    )
    
    fun createEncoder(width: Int, height: Int): Long {
        return nativeCreateEncoder(width, height)
    }
    
    fun addFrame(ptr: Long, bitmap: Bitmap, durationMs: Int): Int {
        val byteArray = bitmapToByteArray(bitmap)
        return nativeAddFrame(ptr, byteArray, durationMs, 0, 0)
    }
    
    fun finalize(ptr: Long): ByteArray? {
        return nativeFinalize(ptr)
    }
    
    fun destroy(ptr: Long) {
        // Native cleanup handled by GC
    }
    
    fun encodeAnimatedWebp(frames: List<Frame>, width: Int, height: Int): ByteArray? {
        val ptr = createEncoder(width, height)
        if (ptr == -1L) return null
        
        try {
            frames.forEach { frame ->
                addFrame(ptr, frame.bitmap, frame.durationMs)
            }
            return finalize(ptr)
        } finally {
            destroy(ptr)
        }
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.RGBA_8888, 100, stream)
        return stream.toByteArray()
    }
    
    companion object {
        external fun nativeCreateEncoder(width: Int, height: Int): Long
        external fun nativeAddFrame(ptr: Long, data: ByteArray, durationMs: Int, x: Int, y: Int): Int
        external fun nativeFinalize(ptr: Long): ByteArray?
    }
}
