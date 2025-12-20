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
    private const val MAX_IMAGE_SIZE = 1920 // 最大图片尺寸

    /**
     * 将 content:// URI 的媒体复制到私有目录
     * @param isVideo 是否为视频文件
     * @return 本地文件路径，失败返回 null
     */
    fun saveMediaFromUri(context: Context, uri: Uri, isVideo: Boolean): String? {
        return try {
            // 创建媒体目录
            val splashDir = File(context.filesDir, SPLASH_DIR)
            if (!splashDir.exists()) {
                splashDir.mkdirs()
            }

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
        // 复制视频文件
        val fileName = "splash_${UUID.randomUUID()}.mp4"
        val videoFile = File(splashDir, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(videoFile).use { output ->
                    input.copyTo(output)
                }
            }
            videoFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存图片文件
     */
    private fun saveImageFromUri(context: Context, uri: Uri, splashDir: File): String? {
        // 读取图片
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (bitmap == null) return null

        // 生成唯一文件名
        val fileName = "splash_${UUID.randomUUID()}.png"
        val imageFile = File(splashDir, fileName)

        // 压缩并保存图片（限制尺寸）
        val scaledBitmap = scaleBitmap(bitmap, MAX_IMAGE_SIZE)
        FileOutputStream(imageFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // 回收原始 bitmap
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }

        return imageFile.absolutePath
    }

    /**
     * 删除媒体文件
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
        return path.endsWith(".mp4", ignoreCase = true) ||
               path.endsWith(".webm", ignoreCase = true) ||
               path.endsWith(".3gp", ignoreCase = true)
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
    fun clearAll(context: Context) {
        val splashDir = File(context.filesDir, SPLASH_DIR)
        splashDir.listFiles()?.forEach { it.delete() }
    }
}
