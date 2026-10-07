package com.example.piliai.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * File utilities for download and storage
 */
object FileUtils {
    
    suspend fun downloadFile(
        context: Context,
        url: String,
        filename: String,
        destinationDir: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = destinationDir?.let { File(it) } ?: getDownloadDir(context)
            if (!dir.exists()) dir.mkdirs()
            
            val file = File(dir, filename)
            val urlObj = java.net.URL(url)
            val connection = urlObj.openConnection() as java.net.HttpURLConnection
            
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.useCaches = false
            
            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(file)
            
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getDownloadDir(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        return File(externalDir, "PiliNara/Downloads").also {
            if (!it.exists()) it.mkdirs()
        }
    }
    
    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, "PiliNara").also {
            if (!it.exists()) it.mkdirs()
        }
    }
    
    fun getMimeType(uri: Uri): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }
    
    suspend fun deleteRecursive(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { it.deleteRecursively() }
            }
            file.delete()
        }
    }
    
    fun calculateSize(file: File): Long {
        if (!file.exists()) return 0L
        return if (file.isDirectory) {
            file.listFiles()?.sumOf { it.length() } ?: 0L
        } else {
            file.length()
        }
    }
}
