package com.webtoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * - /
 */
object SplashStorage {

    private const val TAG = "SplashStorage"
    private const val SPLASH_DIR = "splash_media"
    private const val MAX_IMAGE_SIZE = 1920 // Max
    private const val BUFFER_SIZE = 8192
    
    // Support
    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "3gp", "mkv", "avi", "mov")
    // Support
    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

    /**
     * content:// URI
     * @param isVideo parameter
     * @return result
     */
    fun saveMediaFromUri(context: Context, uri: Uri, isVideo: Boolean): String? {
        return try {
            // Create
            val splashDir = getSplashDir(context)

            if (isVideo) {
                saveVideoFromUri(context, uri, splashDir)
            } else {
                saveImageFromUri(context, uri, splashDir)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }

    /**
     * Note.
     */
    private fun saveVideoFromUri(context: Context, uri: Uri, splashDir: File): String? {
        // Copy
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
            AppLogger.e(TAG, "Operation failed", e)
            // Cleanup
            videoFile.delete()
            null
        }
    }

    /**
     * Note.
     */
    private fun saveImageFromUri(context: Context, uri: Uri, splashDir: File): String? {
        // BitmapFactory.Options
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        // Note.
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        
        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
        options.inJustDecodeBounds = false
        
        // Note.
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return null

        // Generate
        val fileName = "splash_${UUID.randomUUID()}.png"
        val imageFile = File(splashDir, fileName)

        return try {
            // Compression（）
            val scaledBitmap = scaleBitmap(bitmap, MAX_IMAGE_SIZE)
            FileOutputStream(imageFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            // bitmap
            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()

            imageFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            bitmap.recycle()
            imageFile.delete()
            null
        }
    }
    
    /**
     * Calculate sample size
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
     * Note.
     */
    fun deleteMediaFiles(mediaPaths: List<String?>): Int {
        var deletedCount = 0
        mediaPaths.forEach { path ->
            if (deleteMedia(path)) deletedCount++
        }
        return deletedCount
    }

    /**
     * Note.
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
     * Note.
     */
    fun isVideoFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in VIDEO_EXTENSIONS
    }
    
    /**
     * Note.
     */
    fun isImageFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }
    
    /**
     * Note.
     */
    private fun getSplashDir(context: Context): File {
        val splashDir = File(context.filesDir, SPLASH_DIR)
        if (!splashDir.exists()) {
            splashDir.mkdirs()
        }
        return splashDir
    }
    
    /**
     * Note.
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
     * Note.
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
     * Bitmap
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
     * Note.
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
     * Note.
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
