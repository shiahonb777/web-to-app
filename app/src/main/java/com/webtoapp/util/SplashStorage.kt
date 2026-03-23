package com.webtoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 启动画面媒体存储工具类 - 将图片/视频复制到应用私有目录实现持久化
 */
object SplashStorage {

    private const val SPLASH_DIR = "splash_media"
    private const val MAX_IMAGE_SIZE = 1920 // Max图片尺寸
    private const val BUFFER_SIZE = 8192
    
    // Support的视频格式
    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "3gp", "mkv", "avi", "mov")
    // Support的图片格式
    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

    /**
     * 将 content:// URI 的媒体复制到私有目录
     * @param isVideo 是否为视频文件
     * @return 本地文件路径，失败返回 null
     */
    fun saveMediaFromUri(context: Context, uri: Uri, isVideo: Boolean): String? {
        return try {
            // Create媒体目录
            val splashDir = getSplashDir(context)

            if (isVideo) {
                saveVideoFromUri(context, uri, splashDir)
            } else {
                saveImageFromUri(context, uri, splashDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存视频文件
     */
    private fun saveVideoFromUri(context: Context, uri: Uri, splashDir: File): String? {
        // Copy视频文件
        val fileName = "splash_${UUID.randomUUID()}.mp4"
        val videoFile = File(splashDir, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(videoFile).buffered(BUFFER_SIZE).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }
            videoFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup失败的文件
            videoFile.delete()
            null
        }
    }

    /**
     * 保存图片文件
     */
    private fun saveImageFromUri(context: Context, uri: Uri, splashDir: File): String? {
        // 使用 BitmapFactory.Options 优化内存
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        // 先获取图片尺寸
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        
        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
        options.inJustDecodeBounds = false
        
        // 读取图片
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return null

        // Generate唯一文件名
        val fileName = "splash_${UUID.randomUUID()}.png"
        val imageFile = File(splashDir, fileName)

        return try {
            // Compression并保存图片（限制尺寸）
            val scaledBitmap = scaleBitmap(bitmap, MAX_IMAGE_SIZE)
            FileOutputStream(imageFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            // 回收 bitmap
            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()

            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap.recycle()
            imageFile.delete()
            null
        }
    }
    
    /**
     * 计算采样率
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }

    /**
     * Delete media file
     */
    fun deleteMedia(mediaPath: String?): Boolean {
        if (mediaPath.isNullOrBlank()) return false
        return try {
            val file = File(mediaPath)
            if (file.exists() && file.absolutePath.contains(SPLASH_DIR)) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 批量删除媒体
     */
    fun deleteMediaFiles(mediaPaths: List<String?>): Int {
        var deletedCount = 0
        mediaPaths.forEach { path ->
            if (deleteMedia(path)) deletedCount++
        }
        return deletedCount
    }

    /**
     * 检查媒体文件是否存在
     */
    fun mediaExists(mediaPath: String?): Boolean {
        if (mediaPath.isNullOrBlank()) return false
        return try {
            File(mediaPath).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 判断是否为视频文件
     */
    fun isVideoFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in VIDEO_EXTENSIONS
    }
    
    /**
     * 判断是否为图片文件
     */
    fun isImageFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }
    
    /**
     * 获取启动画面目录
     */
    private fun getSplashDir(context: Context): File {
        val splashDir = File(context.filesDir, SPLASH_DIR)
        if (!splashDir.exists()) {
            splashDir.mkdirs()
        }
        return splashDir
    }
    
    /**
     * 获取存储统计信息
     */
    fun getStorageStats(context: Context): StorageStats {
        val splashDir = File(context.filesDir, SPLASH_DIR)
        if (!splashDir.exists()) {
            return StorageStats(0, 0, 0L)
        }
        
        val files = splashDir.listFiles() ?: emptyArray()
        var imageCount = 0
        var videoCount = 0
        var totalSize = 0L
        
        files.forEach { file ->
            totalSize += file.length()
            if (isVideoFile(file.name)) videoCount++ else imageCount++
        }
        
        return StorageStats(imageCount, videoCount, totalSize)
    }
    
    /**
     * 清理未使用的媒体文件
     */
    fun cleanupUnusedMedia(context: Context, usedMediaPaths: Set<String>): Int {
        val splashDir = File(context.filesDir, SPLASH_DIR)
        if (!splashDir.exists()) return 0
        
        var deletedCount = 0
        splashDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in usedMediaPaths) {
                if (file.delete()) deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * 等比缩放 Bitmap
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 清理所有启动画面媒体
     */
    fun clearAll(context: Context): Boolean {
        return try {
            val splashDir = File(context.filesDir, SPLASH_DIR)
            splashDir.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 存储统计信息
     */
    data class StorageStats(
        val imageCount: Int,
        val videoCount: Int,
        val totalSizeBytes: Long
    ) {
        val totalCount: Int get() = imageCount + videoCount
        val totalSizeMB: Float get() = totalSizeBytes / (1024f * 1024f)
    }
}
