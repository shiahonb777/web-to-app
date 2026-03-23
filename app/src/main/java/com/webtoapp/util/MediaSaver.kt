package com.webtoapp.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

/**
 * 媒体文件保存工具类
 * 支持将图片和视频保存到系统相册，确保在图库 App 中可见
 */
object MediaSaver {
    
    private const val APP_FOLDER = "WebToApp"
    private const val DEFAULT_BUFFER_SIZE = 8192
    
    /**
     * 媒体类型
     */
    enum class MediaType {
        IMAGE,
        VIDEO
    }
    
    /**
     * 保存结果
     */
    sealed class SaveResult {
        data class Success(val uri: Uri, val path: String) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
    
    /**
     * 下载进度回调
     */
    interface ProgressCallback {
        /**
         * 进度更新
         * @param bytesDownloaded 已下载字节数
         * @param totalBytes 总字节数 (-1 表示未知)
         * @param progress 进度百分比 (0-100, -1 表示未知)
         */
        fun onProgress(bytesDownloaded: Long, totalBytes: Long, progress: Int)
        
        /**
         * 下载开始
         */
        fun onStart(totalBytes: Long) {}
        
        /**
         * 下载完成
         */
        fun onComplete() {}
        
        /**
         * 下载失败
         */
        fun onError(message: String) {}
    }
    
    /**
     * 简化的进度回调 (仅关注进度百分比)
     */
    fun interface SimpleProgressCallback {
        fun onProgress(progress: Int)
    }
    
    /**
     * 将 SimpleProgressCallback 转换为 ProgressCallback
     */
    private fun SimpleProgressCallback.toProgressCallback(): ProgressCallback {
        return object : ProgressCallback {
            override fun onProgress(bytesDownloaded: Long, totalBytes: Long, progress: Int) {
                this@toProgressCallback.onProgress(progress)
            }
        }
    }
    
    /**
     * 根据 MIME 类型判断媒体类型
     */
    fun getMediaType(mimeType: String?): MediaType? {
        return when {
            mimeType?.startsWith("image/") == true -> MediaType.IMAGE
            mimeType?.startsWith("video/") == true -> MediaType.VIDEO
            else -> null
        }
    }
    
    /**
     * 根据文件扩展名判断媒体类型
     */
    fun getMediaTypeByExtension(fileName: String): MediaType? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> MediaType.IMAGE
            "mp4", "webm", "mkv", "avi", "mov", "3gp", "m4v", "flv" -> MediaType.VIDEO
            else -> null
        }
    }
    
    /**
     * 从 URL 下载并保存媒体文件到相册
     */
    suspend fun saveFromUrl(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String? = null,
        progressCallback: ProgressCallback? = null
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            val actualMimeType = mimeType ?: connection.contentType ?: guessMimeType(fileName)
            val mediaType = getMediaType(actualMimeType) ?: getMediaTypeByExtension(fileName)
                ?: return@withContext SaveResult.Error("不支持的媒体类型")
            
            val contentLength = connection.contentLengthLong
            progressCallback?.onStart(contentLength)
            
            connection.getInputStream().use { inputStream ->
                saveToGalleryWithProgress(context, inputStream, fileName, actualMimeType, mediaType, contentLength, progressCallback)
            }
        } catch (e: Exception) {
            progressCallback?.onError(e.message ?: "未知错误")
            SaveResult.Error("下载失败: ${e.message}")
        }
    }
    
    /**
     * 从 URL 下载并保存媒体文件到相册 (简化版进度回调)
     */
    suspend fun saveFromUrl(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String? = null,
        onProgress: SimpleProgressCallback
    ): SaveResult = saveFromUrl(context, url, fileName, mimeType, onProgress.toProgressCallback())
    
    /**
     * 从 URL 下载并保存媒体文件到相册（支持自定义 Headers 和进度回调）
     */
    suspend fun saveFromUrlWithHeaders(
        context: Context,
        url: String,
        fileName: String,
        headers: Map<String, String> = emptyMap(),
        mimeType: String? = null,
        progressCallback: ProgressCallback? = null
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            
            // 添加自定义 Headers
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            // 添加常用 Headers
            if (!headers.containsKey("User-Agent")) {
                connection.setRequestProperty("User-Agent", 
                    "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
            }
            
            val responseCode = connection.responseCode
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                progressCallback?.onError("HTTP 错误: $responseCode")
                return@withContext SaveResult.Error("HTTP 错误: $responseCode")
            }
            
            val actualMimeType = mimeType ?: connection.contentType ?: guessMimeType(fileName)
            val mediaType = getMediaType(actualMimeType) ?: getMediaTypeByExtension(fileName)
                ?: return@withContext SaveResult.Error("不支持的媒体类型")
            
            val contentLength = connection.contentLengthLong
            progressCallback?.onStart(contentLength)
            
            connection.inputStream.use { inputStream ->
                saveToGalleryWithProgress(context, inputStream, fileName, actualMimeType, mediaType, contentLength, progressCallback)
            }
        } catch (e: Exception) {
            progressCallback?.onError(e.message ?: "未知错误")
            SaveResult.Error("下载失败: ${e.message}")
        }
    }
    
    /**
     * 从本地文件保存到相册
     */
    suspend fun saveFromFile(
        context: Context,
        file: File,
        mimeType: String? = null
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext SaveResult.Error("File not found")
            }
            
            val fileName = file.name
            val actualMimeType = mimeType ?: guessMimeType(fileName)
            val mediaType = getMediaType(actualMimeType) ?: getMediaTypeByExtension(fileName)
                ?: return@withContext SaveResult.Error("不支持的媒体类型")
            
            FileInputStream(file).use { inputStream ->
                saveToGallery(context, inputStream, fileName, actualMimeType, mediaType)
            }
        } catch (e: Exception) {
            SaveResult.Error("保存失败: ${e.message}")
        }
    }
    
    /**
     * 从字节数组保存到相册
     */
    suspend fun saveFromBytes(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val mediaType = getMediaType(mimeType) ?: getMediaTypeByExtension(fileName)
                ?: return@withContext SaveResult.Error("不支持的媒体类型")
            
            bytes.inputStream().use { inputStream ->
                saveToGallery(context, inputStream, fileName, mimeType, mediaType)
            }
        } catch (e: Exception) {
            SaveResult.Error("保存失败: ${e.message}")
        }
    }

    
    /**
     * 保存媒体文件到系统相册
     */
    private fun saveToGallery(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        mediaType: MediaType
    ): SaveResult {
        return saveToGalleryWithProgress(context, inputStream, fileName, mimeType, mediaType, -1, null)
    }
    
    /**
     * 保存媒体文件到系统相册 (带进度回调)
     */
    private fun saveToGalleryWithProgress(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        totalBytes: Long,
        progressCallback: ProgressCallback?
    ): SaveResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToGalleryQWithProgress(context, inputStream, fileName, mimeType, mediaType, totalBytes, progressCallback)
        } else {
            saveToGalleryLegacyWithProgress(context, inputStream, fileName, mimeType, mediaType, totalBytes, progressCallback)
        }
    }
    
    /**
     * 带进度的流复制
     */
    private fun copyWithProgress(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalBytes: Long,
        progressCallback: ProgressCallback?
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead = 0L
        var lastReportedProgress = -1
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            
            if (progressCallback != null) {
                val progress = if (totalBytes > 0) {
                    ((totalBytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                } else {
                    -1
                }
                
                // 只在进度变化时回调，避免频繁调用
                if (progress != lastReportedProgress) {
                    progressCallback.onProgress(totalBytesRead, totalBytes, progress)
                    lastReportedProgress = progress
                }
            }
        }
        
        return totalBytesRead
    }
    
    /**
     * Android 10+ 使用 MediaStore API 保存 (带进度回调)
     */
    private fun saveToGalleryQWithProgress(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        totalBytes: Long,
        progressCallback: ProgressCallback?
    ): SaveResult {
        val (contentUri, relativePath) = when (mediaType) {
            MediaType.IMAGE -> {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI to 
                    "${Environment.DIRECTORY_PICTURES}/$APP_FOLDER"
            }
            MediaType.VIDEO -> {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI to 
                    "${Environment.DIRECTORY_MOVIES}/$APP_FOLDER"
            }
        }
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val uri = context.contentResolver.insert(contentUri, contentValues)
            ?: return SaveResult.Error("无法创建媒体文件")
        
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                copyWithProgress(inputStream, outputStream, totalBytes, progressCallback)
            } ?: return SaveResult.Error("无法写入文件")
            
            // 标记文件写入完成
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            
            progressCallback?.onComplete()
            SaveResult.Success(uri, "$relativePath/$fileName")
        } catch (e: Exception) {
            // 出错时删除未完成的文件
            context.contentResolver.delete(uri, null, null)
            progressCallback?.onError(e.message ?: "写入失败")
            SaveResult.Error("写入失败: ${e.message}")
        }
    }
    
    /**
     * Android 9 及以下使用传统文件 API 保存 (带进度回调)
     */
    private fun saveToGalleryLegacyWithProgress(
        context: Context,
        inputStream: InputStream,
        fileName: String,
        mimeType: String,
        mediaType: MediaType,
        totalBytes: Long,
        progressCallback: ProgressCallback?
    ): SaveResult {
        val baseDir = when (mediaType) {
            MediaType.IMAGE -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            MediaType.VIDEO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        }
        
        val appDir = File(baseDir, APP_FOLDER)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        // Handle文件名冲突
        var targetFile = File(appDir, fileName)
        var counter = 1
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        
        while (targetFile.exists()) {
            val newName = if (ext.isNotEmpty()) "${nameWithoutExt}_$counter.$ext" else "${nameWithoutExt}_$counter"
            targetFile = File(appDir, newName)
            counter++
        }
        
        return try {
            targetFile.outputStream().use { outputStream ->
                copyWithProgress(inputStream, outputStream, totalBytes, progressCallback)
            }
            
            // 通知媒体库扫描新文件
            val contentUri = when (mediaType) {
                MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, targetFile.absolutePath)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                when (mediaType) {
                    MediaType.IMAGE -> {
                        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    }
                    MediaType.VIDEO -> {
                        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    }
                }
            }
            
            val uri = context.contentResolver.insert(contentUri, values)
                ?: Uri.fromFile(targetFile)
            
            progressCallback?.onComplete()
            SaveResult.Success(uri, targetFile.absolutePath)
        } catch (e: Exception) {
            progressCallback?.onError(e.message ?: "Save failed")
            SaveResult.Error("保存失败: ${e.message}")
        }
    }
    
    /**
     * 根据文件名猜测 MIME 类型
     */
    private fun guessMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                else -> "application/octet-stream"
            }
    }
    
    /**
     * 检查是否为媒体文件（图片或视频）
     */
    fun isMediaFile(mimeType: String?, fileName: String? = null): Boolean {
        if (getMediaType(mimeType) != null) return true
        if (fileName != null && getMediaTypeByExtension(fileName) != null) return true
        return false
    }
}
