package com.example.pilinara.playback

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Playback statistics service
 * Tracks watch time, video progress, etc.
 */
class PlaybackStatsService(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val stats = ConcurrentHashMap<String, VideoStat>()
    private val saveFile = File(context.filesDir, "playback_stats.json")
    private var saveJob: Job? = null
    
    data class VideoStat(
        val bvid: String,
        val cid: Long,
        val progressMs: Long = 0L,
        val durationMs: Long = 0L,
        val lastPlayed: Long = System.currentTimeMillis(),
        val playCount: Int = 0,
        val likes: Int = 0,
        val coins: Int = 0,
        val favorites: Int = 0,
        val comments: Int = 0
    )
    
    companion object {
        private const val SAVE_INTERVAL_MS = 30000L // 30 seconds
    }
    
    init {
        loadStats()
        startAutoSave()
        
        // Listen to app lifecycle
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                saveStats()
            }
            
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                saveStats()
            }
        })
    }
    
    fun updateProgress(bvid: String, cid: Long, progressMs: Long, durationMs: Long) {
        val key = "$bvid:$cid"
        val current = stats.getOrPut(key) { VideoStat(bvid = bvid, cid = cid) }
        stats[key] = current.copy(
            progressMs = progressMs,
            durationMs = durationMs,
            lastPlayed = System.currentTimeMillis()
        )
        
        scheduleSave()
    }
    
    fun markPlayed(bvid: String, cid: Long, durationMs: Long) {
        val key = "$bvid:$cid"
        val current = stats.getOrPut(key) { VideoStat(bvid = bvid, cid = cid) }
        stats[key] = current.copy(
            playCount = current.playCount + 1,
            durationMs = durationMs,
            lastPlayed = System.currentTimeMillis()
        )
    }
    
    fun addLike(bvid: String, cid: Long, count: Int = 1) {
        val key = "$bvid:$cid"
        val current = stats[key] ?: return
        stats[key] = current.copy(likes = current.likes + count)
    }
    
    fun addCoin(bvid: String, cid: String, count: Int = 1) {
        val key = "$bvid:$cid"
        val current = stats[key] ?: return
        stats[key] = current.copy(coins = current.coins + count)
    }
    
    fun addFavorite(bvid: String, cid: String) {
        val key = "$bvid:$cid"
        val current = stats[key] ?: return
        stats[key] = current.copy(favorites = current.favorites + 1)
    }
    
    fun getProgress(bvid: String, cid: Long): Long? {
        return stats["$bvid:$cid"]?.progressMs
    }
    
    fun isWatched(bvid: String, cid: Long, threshold: Double = 0.8): Boolean {
        val stat = stats["$bvid:$cid"] ?: return false
        return stat.durationMs > 0 && stat.progressMs >= stat.durationMs * threshold
    }
    
    fun getHistory(limit: Int = 50): List<VideoStat> {
        return stats.values
            .sortedByDescending { it.lastPlayed }
            .take(limit)
    }
    
    fun clearHistory() {
        stats.clear()
        saveStats()
    }
    
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_INTERVAL_MS)
            saveStats()
        }
    }
    
    private fun startAutoSave() {
        scope.launch {
            while (true) {
                delay(SAVE_INTERVAL_MS)
                saveStats()
            }
        }
    }
    
    private fun saveStats() {
        try {
            val json = Json.encodeToString(stats.values.toList())
            saveFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadStats() {
        try {
            if (saveFile.exists()) {
                val json = saveFile.readText()
                val list = Json.decodeFromString<List<VideoStat>>(json)
                stats.clear()
                list.forEach { stat ->
                    stats["${stat.bvid}:${stat.cid}"] = stat
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun close() {
        saveJob?.cancel()
        saveStats()
    }
}
