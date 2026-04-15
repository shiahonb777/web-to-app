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
 * Icon storage helper -
 */
object IconStorage {

    private const val TAG = "IconStorage"
    private const val ICONS_DIR = "app_icons"
    private const val DEFAULT_ICON_SIZE = 512
    private const val BUFFER_SIZE = 8192

    /**
     * content:// URI
     * inSampleSize
     * @return result
     */
    fun saveIconFromUri(context: Context, uri: Uri): String? {
        return try {
            val iconsDir = getIconsDir(context)

            // ：，
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            
            // ： inSampleSize
            options.inSampleSize = calculateInSampleSize(options, DEFAULT_ICON_SIZE, DEFAULT_ICON_SIZE)
            options.inJustDecodeBounds = false
            
            // ：
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            val fileName = "icon_${UUID.randomUUID()}.png"
            val iconFile = File(iconsDir, fileName)

            val scaledBitmap = scaleBitmap(bitmap, DEFAULT_ICON_SIZE)
            FileOutputStream(iconFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()

            iconFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * Bitmap
     */
    fun saveIconFromBitmap(context: Context, bitmap: Bitmap): String? {
        return try {
            val iconsDir = getIconsDir(context)
            val fileName = "icon_${UUID.randomUUID()}.png"
            val iconFile = File(iconsDir, fileName)
            
            val scaledBitmap = scaleBitmap(bitmap, DEFAULT_ICON_SIZE)
            FileOutputStream(iconFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            if (scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }
            
            iconFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * Note.
     */
    fun saveIconFromBytes(context: Context, bytes: ByteArray): String? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val result = saveIconFromBitmap(context, bitmap)
            bitmap.recycle()
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }

    /**
     * Delete icon file
     */
    fun deleteIcon(iconPath: String?): Boolean {
        if (iconPath.isNullOrBlank()) return false
        return try {
            val file = File(iconPath)
            if (file.exists() && file.absolutePath.contains(ICONS_DIR)) {
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
    fun deleteIcons(iconPaths: List<String?>): Int {
        var deletedCount = 0
        iconPaths.forEach { path ->
            if (deleteIcon(path)) deletedCount++
        }
        return deletedCount
    }

    /**
     * Note.
     */
    fun iconExists(iconPath: String?): Boolean {
        if (iconPath.isNullOrBlank()) return false
        return try {
            File(iconPath).exists()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Note.
     */
    private fun getIconsDir(context: Context): File {
        val iconsDir = File(context.filesDir, ICONS_DIR)
        if (!iconsDir.exists()) {
            iconsDir.mkdirs()
        }
        return iconsDir
    }
    
    /**
     * Note.
     */
    fun getStorageStats(context: Context): StorageStats {
        val iconsDir = File(context.filesDir, ICONS_DIR)
        if (!iconsDir.exists()) {
            return StorageStats(0, 0L)
        }
        
        val files = iconsDir.listFiles() ?: emptyArray()
        val totalSize = files.sumOf { it.length() }
        return StorageStats(files.size, totalSize)
    }
    
    /**
     * （）
     */
    fun cleanupUnusedIcons(context: Context, usedIconPaths: Set<String>): Int {
        val iconsDir = File(context.filesDir, ICONS_DIR)
        if (!iconsDir.exists()) return 0
        
        var deletedCount = 0
        iconsDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in usedIconPaths) {
                if (file.delete()) deletedCount++
            }
        }
        return deletedCount
    }
    
    /**
     * Note.
     */
    fun clearAll(context: Context): Boolean {
        return try {
            val iconsDir = File(context.filesDir, ICONS_DIR)
            iconsDir.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * inSampleSize
     * inSampleSize >=
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
    data class StorageStats(
        val fileCount: Int,
        val totalSizeBytes: Long
    ) {
        val totalSizeMB: Float get() = totalSizeBytes / (1024f * 1024f)
    }
}
