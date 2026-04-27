package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.util.zip.ZipOutputStream

internal object StatusBarAssetWriter {

    fun addToAssets(zipOut: ZipOutputStream, imagePath: String) {
        AppLogger.d("ApkBuilder", "Preparing to embed status bar background: path=$imagePath")

        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            AppLogger.e("ApkBuilder", "Status bar background image does not exist: $imagePath")
            return
        }
        if (!imageFile.canRead()) {
            AppLogger.e("ApkBuilder", "Status bar background image cannot be read: $imagePath")
            return
        }

        try {
            val imageBytes = imageFile.readBytes()
            if (imageBytes.isEmpty()) {
                AppLogger.e("ApkBuilder", "Status bar background image is empty: $imagePath")
                return
            }

            ZipUtils.writeEntryDeflated(zipOut, "assets/statusbar_background.png", imageBytes)
            AppLogger.d("ApkBuilder", "Status bar background embedded: assets/statusbar_background.png (${imageBytes.size} bytes)")
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed status bar background: ${e.message}", e)
        }
    }
}
