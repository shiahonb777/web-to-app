package com.webtoapp.core.crypto

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.webtoapp.core.apkbuilder.ApkConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.LrcData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.crypto.SecretKey

/**
 * 加密 APK 构建器
 * 在原有 ApkBuilder 基础上添加资源加密功能
 */
class EncryptedApkBuilder(private val context: Context) {
    
    companion object {
        private const val TAG = "EncryptedApkBuilder"
    }
    
    private val gson = Gson()
    private val keyManager = KeyManager(context)
    
    /**
     * 加密并写入配置文件
     */
    fun writeEncryptedConfig(
        zipOut: ZipOutputStream,
        config: ApkConfig,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val configJson = gson.toJson(config)
        val assetPath = CryptoConstants.CONFIG_FILE
        
        if (encryptionConfig.encryptConfig) {
            Log.d(TAG, "加密配置文件: $assetPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encryptJson(configJson, assetPath)
            writeEntryDeflated(zipOut, "assets/${assetPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            Log.d(TAG, "写入明文配置文件: $assetPath")
            writeEntryDeflated(zipOut, "assets/$assetPath", configJson.toByteArray(Charsets.UTF_8))
        }
    }
    
    /**
     * 加密并写入 HTML 文件
     */
    fun writeEncryptedHtml(
        zipOut: ZipOutputStream,
        htmlContent: String,
        assetPath: String,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val fullPath = if (assetPath.startsWith("html/")) assetPath else "html/$assetPath"
        
        if (encryptionConfig.encryptHtml) {
            Log.d(TAG, "加密 HTML 文件: $fullPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encryptText(htmlContent, fullPath)
            writeEntryDeflated(zipOut, "assets/${fullPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            Log.d(TAG, "写入明文 HTML 文件: $fullPath")
            writeEntryDeflated(zipOut, "assets/$fullPath", htmlContent.toByteArray(Charsets.UTF_8))
        }
    }
    
    /**
     * 加密并写入媒体文件
     * 注意：加密的媒体文件无法使用 AssetManager.openFd()，需要先解密到临时文件
     */
    fun writeEncryptedMedia(
        zipOut: ZipOutputStream,
        mediaData: ByteArray,
        assetPath: String,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey,
        useStored: Boolean = true  // Yes否使用 STORED 方式（用于需要 openFd 的文件）
    ) {
        if (encryptionConfig.encryptMedia) {
            Log.d(TAG, "加密媒体文件: $assetPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encrypt(mediaData, assetPath)
            // Encryption后的文件使用 DEFLATED 压缩（因为已经无法直接 openFd 了）
            writeEntryDeflated(zipOut, "assets/${assetPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            Log.d(TAG, "写入明文媒体文件: $assetPath")
            if (useStored) {
                writeEntryStored(zipOut, "assets/$assetPath", mediaData)
            } else {
                writeEntryDeflated(zipOut, "assets/$assetPath", mediaData)
            }
        }
    }
    
    /**
     * 加密并写入启动画面
     */
    fun writeEncryptedSplash(
        zipOut: ZipOutputStream,
        splashData: ByteArray,
        splashType: String,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val extension = if (splashType == "VIDEO") "mp4" else "png"
        val assetPath = "splash_media.$extension"
        
        if (encryptionConfig.encryptSplash) {
            Log.d(TAG, "加密启动画面: $assetPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encrypt(splashData, assetPath)
            writeEntryDeflated(zipOut, "assets/${assetPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            Log.d(TAG, "写入明文启动画面: $assetPath")
            writeEntryStored(zipOut, "assets/$assetPath", splashData)
        }
    }
    
    /**
     * 加密并写入 BGM 文件
     */
    fun writeEncryptedBgm(
        zipOut: ZipOutputStream,
        bgmData: ByteArray,
        index: Int,
        lrcData: LrcData?,
        encryptionConfig: EncryptionConfig,
        secretKey: SecretKey
    ) {
        val bgmPath = "bgm/bgm_$index.mp3"
        
        if (encryptionConfig.encryptBgm) {
            Log.d(TAG, "加密 BGM: $bgmPath")
            val encryptor = AssetEncryptor(secretKey)
            val encryptedData = encryptor.encrypt(bgmData, bgmPath)
            writeEntryDeflated(zipOut, "assets/${bgmPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedData)
        } else {
            Log.d(TAG, "写入明文 BGM: $bgmPath")
            writeEntryStored(zipOut, "assets/$bgmPath", bgmData)
        }
        
        // LRC 歌词文件（通常较小，可以加密）
        if (lrcData != null && lrcData.lines.isNotEmpty()) {
            val lrcContent = convertLrcDataToString(lrcData)
            val lrcPath = "bgm/bgm_$index.lrc"
            
            if (encryptionConfig.encryptBgm) {
                val encryptor = AssetEncryptor(secretKey)
                val encryptedLrc = encryptor.encryptText(lrcContent, lrcPath)
                writeEntryDeflated(zipOut, "assets/${lrcPath}${CryptoConstants.ENCRYPTED_EXTENSION}", encryptedLrc)
            } else {
                writeEntryDeflated(zipOut, "assets/$lrcPath", lrcContent.toByteArray(Charsets.UTF_8))
            }
        }
    }
    
    /**
     * 生成加密密钥
     * 基于目标包名和签名生成
     */
    fun generateEncryptionKey(packageName: String): SecretKey {
        val signatureHash = keyManager.getSignatureHashForBuild()
        return keyManager.generateKeyForPackage(packageName, signatureHash)
    }
    
    /**
     * 写入加密元数据
     * 包含加密配置信息，供运行时使用
     * @param signatureHash 用于签名 APK 的证书哈希（从 JarSigner 获取）
     */
    fun writeEncryptionMetadata(
        zipOut: ZipOutputStream,
        encryptionConfig: EncryptionConfig,
        packageName: String,
        signatureHash: ByteArray? = null
    ) {
        if (!encryptionConfig.enabled) return
        
        // 使用传入的签名哈希，如果没有则使用当前应用的签名
        val sigHash = signatureHash ?: keyManager.getSignatureHashForBuild()
        
        val metadata = EncryptionMetadata(
            version = CryptoConstants.ENCRYPTED_HEADER_VERSION,
            encryptConfig = encryptionConfig.encryptConfig,
            encryptHtml = encryptionConfig.encryptHtml,
            encryptMedia = encryptionConfig.encryptMedia,
            encryptSplash = encryptionConfig.encryptSplash,
            encryptBgm = encryptionConfig.encryptBgm,
            packageName = packageName,
            signatureHash = sigHash.toHexString(),
            // Security保护配置
            enableIntegrityCheck = encryptionConfig.enableIntegrityCheck,
            enableAntiDebug = encryptionConfig.enableAntiDebug,
            enableAntiTamper = encryptionConfig.enableAntiTamper,
            enableRootDetection = encryptionConfig.enableRootDetection,
            enableEmulatorDetection = encryptionConfig.enableEmulatorDetection,
            enableRuntimeProtection = encryptionConfig.enableRuntimeProtection,
            blockOnThreat = encryptionConfig.blockOnThreat,
            encryptionLevel = encryptionConfig.encryptionLevel.name
        )
        
        val metadataJson = gson.toJson(metadata)
        writeEntryDeflated(zipOut, "assets/encryption_meta.json", metadataJson.toByteArray(Charsets.UTF_8))
        
        Log.d(TAG, "写入加密元数据: $metadata")
    }
    
    /**
     * 写入条目（DEFLATED 压缩）
     */
    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.DEFLATED
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }
    
    /**
     * 写入条目（STORED 未压缩）
     */
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
    
    /**
     * 将 LrcData 转换为 LRC 格式字符串
     */
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

/**
 * 加密元数据
 * 存储在 APK 中，供运行时读取
 */
data class EncryptionMetadata(
    val version: Int,
    val encryptConfig: Boolean,
    val encryptHtml: Boolean,
    val encryptMedia: Boolean,
    val encryptSplash: Boolean,
    val encryptBgm: Boolean,
    val packageName: String,
    val signatureHash: String,
    // Security保护配置
    val enableIntegrityCheck: Boolean = true,
    val enableAntiDebug: Boolean = true,
    val enableAntiTamper: Boolean = true,
    val enableRootDetection: Boolean = false,
    val enableEmulatorDetection: Boolean = false,
    val enableRuntimeProtection: Boolean = true,
    val blockOnThreat: Boolean = false,
    val encryptionLevel: String = "STANDARD"
)
