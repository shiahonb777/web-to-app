package com.webtoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 媒体文件存储工具
 * 用于保存图片/视频转APP的媒体文件
 * 支持单媒体（兼容旧版）和多媒体画廊
 */
object MediaStorage {
    
    private const val TAG = "MediaStorage"
    private const val MEDIA_DIR = "media_apps"
    private const val GALLERY_DIR = "gallery_apps"
    private const val THUMBNAIL_DIR = "thumbnails"
    
    // 缩略图尺寸
    private const val THUMBNAIL_WIDTH = 300
    private const val THUMBNAIL_HEIGHT = 300
    private const val THUMBNAIL_QUALITY = 80
    
    // ==================== 旧版单媒体支持 ====================
    
    /**
     * 保存媒体文件到应用私有目录（兼容旧版）
     * @return 保存后的文件路径，失败返回 null
     */
    fun saveMedia(context: Context, uri: Uri, isVideo: Boolean): String? {
        return try {
            val mediaDir = File(context.filesDir, MEDIA_DIR)
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
            
            val extension = if (isVideo) "mp4" else "png"
            val fileName = "media_${UUID.randomUUID()}.$extension"
            val destFile = File(mediaDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete media file
     */
    fun deleteMedia(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取媒体文件
     */
    fun getMediaFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists()) file else null
    }
    
    // ==================== 新版画廊多媒体支持 ====================
    
    /**
     * 获取画廊媒体文件目录
     */
    private fun getGalleryDir(context: Context): File {
        val dir = File(context.filesDir, GALLERY_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /**
     * 获取缩略图目录
     */
    private fun getThumbnailDir(context: Context): File {
        val dir = File(context.filesDir, THUMBNAIL_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /**
     * 保存画廊媒体文件（单个）
     * @return SavedMediaInfo 包含保存路径、缩略图路径、媒体信息
     */
    suspend fun saveGalleryMedia(
        context: Context, 
        uri: Uri, 
        type: GalleryItemType
    ): SavedMediaInfo? = withContext(Dispatchers.IO) {
        try {
            val galleryDir = getGalleryDir(context)
            
            // 确定扩展名
            val extension = getExtensionFromUri(context, uri, type)
            val fileName = "gallery_${UUID.randomUUID()}.$extension"
            val destFile = File(galleryDir, fileName)
            
            // Copy文件
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            
            // Get媒体信息
            val mediaInfo = getMediaInfo(destFile, type)
            
            // Generate缩略图
            val thumbnailPath = generateThumbnail(context, destFile, type)
            
            SavedMediaInfo(
                path = destFile.absolutePath,
                thumbnailPath = thumbnailPath,
                width = mediaInfo.width,
                height = mediaInfo.height,
                duration = mediaInfo.duration,
                fileSize = destFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save gallery media: ${e.message}", e)
            null
        }
    }
    
    /**
     * 批量保存画廊媒体文件
     * @return 成功保存的 GalleryItem 列表
     */
    suspend fun saveGalleryMediaBatch(
        context: Context,
        items: List<Pair<Uri, GalleryItemType>>
    ): List<GalleryItem> = withContext(Dispatchers.IO) {
        items.mapIndexedNotNull { index, (uri, type) ->
            val info = saveGalleryMedia(context, uri, type)
            if (info != null) {
                // 介 uri 获取文件名
                val displayName = getDisplayNameFromUri(context, uri)
                
                GalleryItem(
                    path = info.path,
                    type = type,
                    name = displayName,
                    thumbnailPath = info.thumbnailPath,
                    duration = info.duration,
                    width = info.width,
                    height = info.height,
                    fileSize = info.fileSize,
                    sortIndex = index
                )
            } else {
                null
            }
        }
    }
    
    /**
     * 删除画廊媒体文件（包括缩略图）
     */
    fun deleteGalleryMedia(item: GalleryItem): Boolean {
        var success = true
        
        // Delete主文件
        try {
            File(item.path).delete()
        } catch (e: Exception) {
            success = false
        }
        
        // Delete缩略图
        item.thumbnailPath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                // 缩略图删除失败不影响整体结果
            }
        }
        
        return success
    }
    
    /**
     * 批量删除画廊媒体
     */
    fun deleteGalleryMediaBatch(items: List<GalleryItem>): Int {
        var count = 0
        items.forEach { item ->
            if (deleteGalleryMedia(item)) count++
        }
        return count
    }
    
    // ==================== 缩略图生成 ====================
    
    /**
     * 生成缩略图
     * @return 缩略图路径，失败返回 null
     */
    private fun generateThumbnail(
        context: Context, 
        file: File, 
        type: GalleryItemType
    ): String? {
        return try {
            val thumbnailDir = getThumbnailDir(context)
            val thumbnailFile = File(thumbnailDir, "thumb_${file.nameWithoutExtension}.jpg")
            
            val bitmap = when (type) {
                GalleryItemType.IMAGE -> generateImageThumbnail(file)
                GalleryItemType.VIDEO -> generateVideoThumbnail(file)
            }
            
            bitmap?.let {
                FileOutputStream(thumbnailFile).use { fos ->
                    it.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, fos)
                }
                it.recycle()
                thumbnailFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate thumbnail: ${e.message}", e)
            null
        }
    }
    
    /**
     * 生成图片缩略图
     */
    private fun generateImageThumbnail(file: File): Bitmap? {
        // 先获取图片尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        // 计算采样率
        val sampleSize = calculateInSampleSize(options.outWidth, options.outHeight)
        
        // 解码缩略图
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        
        // Zoom到目标尺寸
        return scaleBitmap(bitmap)
    }
    
    /**
     * 生成视频缩略图
     */
    private fun generateVideoThumbnail(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            // Get第1秒的帧，如果视频小于1秒则获取第一帧
            val frame = retriever.getFrameAtTime(1000000) // 1秒 = 1000000微秒
                ?: retriever.getFrameAtTime(0)
            frame?.let { scaleBitmap(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate video thumbnail: ${e.message}", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    
    /**
     * 计算采样率
     */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        if (height > THUMBNAIL_HEIGHT || width > THUMBNAIL_WIDTH) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= THUMBNAIL_HEIGHT &&
                   (halfWidth / inSampleSize) >= THUMBNAIL_WIDTH) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    /**
     * 缩放 Bitmap 到缩略图尺寸
     */
    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val ratio = minOf(
            THUMBNAIL_WIDTH.toFloat() / bitmap.width,
            THUMBNAIL_HEIGHT.toFloat() / bitmap.height
        )
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        
        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }
    
    // ==================== 媒体信息获取 ====================
    
    /**
     * 获取媒体文件信息
     */
    private fun getMediaInfo(file: File, type: GalleryItemType): MediaInfo {
        return when (type) {
            GalleryItemType.IMAGE -> getImageInfo(file)
            GalleryItemType.VIDEO -> getVideoInfo(file)
        }
    }
    
    /**
     * 获取图片信息
     */
    private fun getImageInfo(file: File): MediaInfo {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return MediaInfo(
            width = options.outWidth,
            height = options.outHeight,
            duration = 0
        )
    }
    
    /**
     * 获取视频信息
     */
    private fun getVideoInfo(file: File): MediaInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            
            MediaInfo(width, height, duration)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video info: ${e.message}", e)
            MediaInfo(0, 0, 0)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 介 Uri 获取文件扩展名
     */
    private fun getExtensionFromUri(
        context: Context, 
        uri: Uri, 
        type: GalleryItemType
    ): String {
        // 尝试从 MIME 类型获取
        val mimeType = context.contentResolver.getType(uri)
        val extension = mimeType?.let {
            when {
                it.contains("jpeg") || it.contains("jpg") -> "jpg"
                it.contains("png") -> "png"
                it.contains("gif") -> "gif"
                it.contains("webp") -> "webp"
                it.contains("mp4") -> "mp4"
                it.contains("webm") -> "webm"
                it.contains("avi") -> "avi"
                it.contains("mkv") -> "mkv"
                it.contains("mov") -> "mov"
                else -> null
            }
        }
        
        // 如果无法获取，使用默认扩展名
        return extension ?: when (type) {
            GalleryItemType.IMAGE -> "jpg"
            GalleryItemType.VIDEO -> "mp4"
        }
    }
    
    /**
     * 介 Uri 获取显示名称
     */
    private fun getDisplayNameFromUri(context: Context, uri: Uri): String {
        // 尝试介 ContentResolver 获取文件名
        context.contentResolver.query(
            uri, 
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        // 移除扩展名
                        return name.substringBeforeLast(".")
                    }
                }
            }
        }
        
        // 备选：介路径获取
        return uri.lastPathSegment?.substringBeforeLast(".") ?: "Media"
    }
    
    /**
     * 检查 Uri 是否为视频
     */
    fun isVideoUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }
    
    /**
     * 检查 Uri 是否为图片
     */
    fun isImageUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("image/") == true
    }
    
    /**
     * 根据 Uri 获取媒体类型
     */
    fun getMediaType(context: Context, uri: Uri): GalleryItemType? {
        return when {
            isVideoUri(context, uri) -> GalleryItemType.VIDEO
            isImageUri(context, uri) -> GalleryItemType.IMAGE
            else -> null
        }
    }
    
    /**
     * 清理所有画廊文件（谨慎使用）
     */
    fun clearAllGalleryFiles(context: Context) {
        getGalleryDir(context).deleteRecursively()
        getThumbnailDir(context).deleteRecursively()
    }
}

/**
 * 保存媒体信息结果
 */
data class SavedMediaInfo(
    val path: String,              // Save后的文件路径
    val thumbnailPath: String?,    // 缩略图路径
    val width: Int,                // Media宽度
    val height: Int,               // Media高度
    val duration: Long,            // Video时长（毫秒）
    val fileSize: Long             // File大小
)

/**
 * 媒体信息
 */
private data class MediaInfo(
    val width: Int,
    val height: Int,
    val duration: Long
)
