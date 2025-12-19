package com.webtoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图标存储工具类 - 将图片复制到应用私有目录实现持久化
 */
object IconStorage {

    private const val ICONS_DIR = "app_icons"

    /**
     * 将 content:// URI 的图片复制到私有目录
     * @return 本地文件路径，失败返回 null
     */
    fun saveIconFromUri(context: Context, uri: Uri): String? {
        return try {
            // 创建图标目录
            val iconsDir = File(context.filesDir, ICONS_DIR)
            if (!iconsDir.exists()) {
                iconsDir.mkdirs()
            }

            // 读取图片
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 生成唯一文件名
            val fileName = "icon_${UUID.randomUUID()}.png"
            val iconFile = File(iconsDir, fileName)

            // 压缩并保存图片（限制尺寸为 512x512，足够作为图标使用）
            val scaledBitmap = scaleBitmap(bitmap, 512)
            FileOutputStream(iconFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // 回收原始 bitmap
            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
            }

            iconFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 删除图标文件
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
     * 检查图标文件是否存在
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
}
