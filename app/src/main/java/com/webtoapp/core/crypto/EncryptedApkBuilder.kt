package com.webtoapp.core.crypto

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.apkbuilder.ApkConfig
import com.webtoapp.data.model.LrcData
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.SecretKey





class EncryptedApkBuilder(private val context: Context) {

    companion object {
        private const val TAG = "EncryptedApkBuilder"
    }

    private val gson = com.webtoapp.util.GsonProvider.gson
    private val keyManager = KeyManager.getInstance(context)




    fun writeEncryptedConfig(
        zipOut: ZipOutputStream,
        config: ApkConfig,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val configJson = gson.toJson(config)
        val assetPath = CryptoConstants.CONFIG_FILE

        if (encryptionConfig.enabled) {
            AppLogger.d(TAG, "加密配置文件: $assetPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encryptJson(configJson, assetPath)
            writeEntryDeflated(zipOut, "assets/${assetPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            AppLogger.d(TAG, "写入明文配置文件: $assetPath")
            writeEntryDeflated(zipOut, "assets/$assetPath", configJson.toByteArray(Charsets.UTF_8))
        }
    }




    fun writeEncryptedHtml(
        zipOut: ZipOutputStream,
        htmlContent: String,
        assetPath: String,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val fullPath = if (assetPath.startsWith("html/")) assetPath else "html/$assetPath"

        if (encryptionConfig.enabled) {
            AppLogger.d(TAG, "加密 HTML 文件: $fullPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encryptText(htmlContent, fullPath)
            writeEntryDeflated(zipOut, "assets/${fullPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            AppLogger.d(TAG, "写入明文 HTML 文件: $fullPath")
            writeEntryDeflated(zipOut, "assets/$fullPath", htmlContent.toByteArray(Charsets.UTF_8))
        }
    }





    fun writeEncryptedMedia(
        zipOut: ZipOutputStream,
        mediaData: ByteArray,
        assetPath: String,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey,
        useStored: Boolean = true
    ) {
        if (encryptionConfig.enabled) {
            AppLogger.d(TAG, "加密媒体文件: $assetPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encrypt(mediaData, assetPath)

            writeEntryDeflated(zipOut, "assets/${assetPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            AppLogger.d(TAG, "写入明文媒体文件: $assetPath")
            if (useStored) {
                writeEntryStored(zipOut, "assets/$assetPath", mediaData)
            } else {
                writeEntryDeflated(zipOut, "assets/$assetPath", mediaData)
            }
        }
    }




    fun writeEncryptedSplash(
        zipOut: ZipOutputStream,
        splashData: ByteArray,
        splashType: String,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val extension = if (splashType == "VIDEO") "mp4" else "png"
        val assetPath = "splash_media.$extension"

        if (encryptionConfig.enabled) {
            AppLogger.d(TAG, "加密启动画面: $assetPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encrypt(splashData, assetPath)
            writeEntryDeflated(zipOut, "assets/${assetPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            AppLogger.d(TAG, "写入明文启动画面: $assetPath")
            writeEntryStored(zipOut, "assets/$assetPath", splashData)
        }
    }




    fun writeEncryptedBgm(
        zipOut: ZipOutputStream,
        bgmData: ByteArray,
        index: Int,
        lrcData: LrcData?,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val bgmPath = "bgm/bgm_$index.mp3"

        if (encryptionConfig.enabled) {
            AppLogger.d(TAG, "加密 BGM: $bgmPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encrypt(bgmData, bgmPath)
            writeEntryDeflated(zipOut, "assets/${bgmPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            AppLogger.d(TAG, "写入明文 BGM: $bgmPath")
            writeEntryStored(zipOut, "assets/$bgmPath", bgmData)
        }


        if (lrcData != null && lrcData.lines.isNotEmpty()) {
            val lrcContent = convertLrcDataToString(lrcData)
            val lrcPath = "bgm/bgm_$index.lrc"

            if (encryptionConfig.enabled) {
                val encryptor = AssetEncryptor(secretKey)
                val encryptedLrc = encryptor.encryptText(lrcContent, lrcPath)
                writeEntryDeflated(zipOut, "assets/${lrcPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedLrc)
            } else {
                writeEntryDeflated(zipOut, "assets/$lrcPath", lrcContent.toByteArray(Charsets.UTF_8))
            }
        }
    }





    fun generateEncryptionKey(packageName: String, encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED): SecretKey {
        val signatureHash = keyManager.getSignatureHashForBuild()
        return keyManager.generateKeyForPackage(
            packageName, signatureHash,
            encryptionConfig.customPassword
        )
    }






    fun writeEncryptionMetadata(
        zipOut: ZipOutputStream,
        encryptionConfig: EncryptionConfig,
        packageName: String,
        signatureHash: ByteArray? = null
    ) {
        if (!encryptionConfig.enabled) return





        val metadata = EncryptionMetadata(
            version = CryptoConstants.ENCRYPTED_HEADER_VERSION,
            packageName = packageName,
            signatureHash = "",
            usesCustomPassword = !encryptionConfig.customPassword.isNullOrBlank()
        )

        val metadataJson = gson.toJson(metadata)
        writeEntryDeflated(zipOut, "assets/encryption_meta.json", metadataJson.toByteArray(Charsets.UTF_8))

        AppLogger.d(TAG, "写入加密元数据 (signatureHash 已省略, usesCustomPassword=${metadata.usesCustomPassword})")
    }




    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.DEFLATED
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }




    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()

        val crc = CRC32()
        crc.update(data)
        entry.crc = crc.value

        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }




    private fun convertLrcDataToString(lrcData: LrcData): String {
        val sb = StringBuilder()

        lrcData.title?.let { sb.appendLine("[ti:$it]") }
        lrcData.artist?.let { sb.appendLine("[ar:$it]") }
        lrcData.album?.let { sb.appendLine("[al:$it]") }

        lrcData.lines.forEach { line ->
            val minutes = line.startTime / 60000
            val seconds = (line.startTime % 60000) / 1000
            val millis = (line.startTime % 1000) / 10
            sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, millis, line.text))
        }

        return sb.toString()
    }
}





data class EncryptionMetadata(
    val version: Int,
    val packageName: String,
    val signatureHash: String = "",
    val usesCustomPassword: Boolean = false
)
