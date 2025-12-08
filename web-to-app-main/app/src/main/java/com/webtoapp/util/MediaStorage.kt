package com.webtoapp.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 媒体文件存储工具
 * 用于保存图片/视频转APP的媒体文件
 */
object MediaStorage {
    
    private const val MEDIA_DIR = "media_apps"
    
    /**
     * 保存媒体文件到应用私有目录
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
     * 删除媒体文件
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
}
