package com.webtoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 应用图标存储工具
 * 负责将用户选择的图标复制到应用私有目录，并返回本地绝对路径
 */
object IconStorage {

    private const val ICON_DIR = "app_icons"
    private const val MAX_ICON_SIZE = 512
    private const val TAG = "IconStorage"

    /**
     * 从 Uri 保存图标到应用私有目录
     * 返回保存后的绝对路径，失败返回 null
     */
    fun saveIconFromUri(context: Context, uri: Uri): String? {
        return try {
            val iconDir = File(context.filesDir, ICON_DIR)
            if (!iconDir.exists()) {
                iconDir.mkdirs()
            }

            // 解码为 Bitmap
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = inputStream.use { BitmapFactory.decodeStream(it) }
            if (original == null) {
                Log.e(TAG, "decode bitmap failed: $uri")
                return null
            }

            // 限制最大尺寸，避免过大图像
            val bitmap = scaleBitmap(original, MAX_ICON_SIZE)

            val fileName = "icon_${UUID.randomUUID()}.png"
            val destFile = File(iconDir, fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (bitmap !== original) {
                original.recycle()
            }

            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "saveIconFromUri error: ${e.message}", e)
            null
        }
    }

    /**
     * 删除图标文件
     */
    fun deleteIcon(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取图标文件
     */
    fun getIconFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
