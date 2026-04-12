package com.webtoapp.core.apkbuilder

import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.crypto.EncryptionConfig
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.util.zip.ZipOutputStream

internal object SplashAssetWriter {

    fun addToAssets(
        zipOut: ZipOutputStream,
        mediaPath: String,
        splashType: String,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed splash media: path=$mediaPath, type=$splashType, encrypt=${encryptionConfig.encryptSplash}")

        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            AppLogger.e("ApkBuilder", "Splash media file does not exist: $mediaPath")
            return
        }
        if (!mediaFile.canRead()) {
            AppLogger.e("ApkBuilder", "Splash media file cannot be read: $mediaPath")
            return
        }
        if (mediaFile.length() == 0L) {
            AppLogger.e("ApkBuilder", "Splash media file is empty: $mediaPath")
            return
        }

        val extension = if (splashType == "VIDEO") "mp4" else "png"
        val assetPath = "splash_media.$extension"
        val isVideo = splashType == "VIDEO"
        val largeFileThreshold = 10 * 1024 * 1024L

        try {
            if (encryptionConfig.encryptSplash && encryptor != null) {
                val encryptedData = if (isVideo && mediaFile.length() > largeFileThreshold) {
                    AppLogger.d("ApkBuilder", "Splash large video encryption mode: ${mediaFile.length() / 1024 / 1024} MB")
                    AssetEncryptionSupport.encryptLargeFile(mediaFile, assetPath, encryptor)
                } else {
                    encryptor.encrypt(mediaFile.readBytes(), assetPath)
                }
                ZipUtils.writeEntryDeflated(zipOut, "assets/${assetPath}.enc", encryptedData)
                AppLogger.d("ApkBuilder", "Splash media encrypted and embedded: assets/${assetPath}.enc (${encryptedData.size} bytes)")
                return
            }

            if (isVideo && mediaFile.length() > largeFileThreshold) {
                AppLogger.d("ApkBuilder", "Splash large video streaming write mode: ${mediaFile.length() / 1024 / 1024} MB")
                ZipUtils.writeEntryStoredStreaming(zipOut, "assets/$assetPath", mediaFile)
            } else {
                val mediaBytes = mediaFile.readBytes()
                ZipUtils.writeEntryStoredSimple(zipOut, "assets/$assetPath", mediaBytes)
                AppLogger.d("ApkBuilder", "Splash media embedded(STORED): assets/$assetPath (${mediaBytes.size} bytes)")
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed splash media: ${e.message}", e)
        }
    }
}
