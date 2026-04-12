package com.webtoapp.core.apkbuilder

import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.logging.AppLogger
import java.io.File

internal object AssetEncryptionSupport {

    fun encryptLargeFile(file: File, assetName: String, encryptor: AssetEncryptor): ByteArray {
        val fileSize = file.length()
        val maxEncryptSize = 100L * 1024 * 1024
        if (fileSize > maxEncryptSize) {
            AppLogger.w(
                "ApkBuilder",
                "WARNING: Encrypting very large file ($assetName, ${fileSize / 1024 / 1024}MB). " +
                    "May cause high memory usage. Consider disabling encryption for large media files."
            )
        }
        return encryptor.encrypt(file.readBytes(), assetName)
    }
}
