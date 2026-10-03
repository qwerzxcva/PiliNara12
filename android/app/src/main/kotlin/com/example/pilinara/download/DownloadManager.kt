package com.example.pilinara.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Download manager for videos and files
 * Replaces Flutter dio download with progress
 */
class DownloadManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val downloads = mutableMapOf<String, DownloadTask>()
    private var notificationManager: NotificationManager? = null
    
    companion object {
        private const val CHANNEL_ID = "pilinara_downloads"
        private const val NOTIFICATION_ID_BASE = 2000
    }
    
    init {
        createNotificationChannel()
    }
    
    data class DownloadTask(
        val id: String,
        val url: String,
        val filename: String,
        val destination: File,
        val progress: Int = 0,
        val status: DownloadStatus = DownloadStatus.PENDING,
        val errorMessage: String? = null,
        val totalBytes: Long = 0L,
        val downloadedBytes: Long = 0L
    )
    
    enum class DownloadStatus {
        PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
    }
    
    fun startDownload(
        id: String,
        url: String,
        filename: String,
        destinationDir: String? = null
    ): DownloadTask {
        val destination = File(destinationDir ?: getDownloadDir(), filename)
        val task = DownloadTask(id = id, url = url, filename = filename, destination = destination)
        downloads[id] = task
        
        scope.launch {
            downloadFile(task)
        }
        
        return task
    }
    
    fun pauseDownload(id: String) {
        val task = downloads[id] ?: return
        downloads[id] = task.copy(status = DownloadStatus.PAUSED)
    }
    
    fun cancelDownload(id: String) {
        val task = downloads[id] ?: return
        task.destination.delete()
        downloads.remove(id)
    }
    
    private suspend fun downloadFile(task: DownloadTask) {
        downloads[task.id] = task.copy(status = DownloadStatus.DOWNLOADING)
        showNotification(task, "下载中... ${task.progress}%")
        
        try {
            val url = URL(task.url)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            
            val totalSize = connection.contentLengthLong
            val inputStream: InputStream = connection.inputStream
            val outputStream = task.destination.outputStream()
            
            val buffer = ByteArray(8192)
            var downloaded = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                
                val progress = if (totalSize > 0) {
                    (downloaded * 100 / totalSize).toInt()
                } else 0
                
                downloads[task.id] = task.copy(
                    progress = progress,
                    downloadedBytes = downloaded,
                    totalBytes = totalSize
                )
                
                if (progress % 10 == 0) {
                    showNotification(task, "下载中... $progress%")
                }
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            downloads[task.id] = task.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100
            )
            showNotification(task, "下载完成")
            
        } catch (e: Exception) {
            Log.e("DownloadManager", "Download failed: ${e.message}")
            downloads[task.id] = task.copy(
                status = DownloadStatus.FAILED,
                errorMessage = e.message
            )
            showNotification(task, "下载失败: ${e.message}")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "下载服务",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun showNotification(task: DownloadTask, text: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, task.id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("PiliNara 下载")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(task.status == DownloadStatus.DOWNLOADING)
            .build()
        
        notificationManager?.notify(NOTIFICATION_ID_BASE + task.id.hashCode(), notification)
    }
    
    private fun getDownloadDir(): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(externalDir, "PiliNara").also {
            if (!it.exists()) it.mkdirs()
        }
    }
    
    fun getDownload(id: String): DownloadTask? = downloads[id]
    
    fun getAllDownloads(): List<DownloadTask> = downloads.values.toList()
    
    fun clearCompleted() {
        downloads.values.removeAll { it.status == DownloadStatus.COMPLETED }
    }
}
