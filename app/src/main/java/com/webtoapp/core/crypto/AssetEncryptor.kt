package com.webtoapp.core.crypto

import android.util.Log
import javax.crypto.SecretKey

/**
 * 资源加密器
 * 用于在打包 APK 时加密资源文件
 */
class AssetEncryptor(private val secretKey: SecretKey) {
    
    companion object {
        private const val TAG = "AssetEncryptor"
    }
    
    /**
     * 加密资源数据
     * 
     * @param data 原始数据
     * @param assetPath 资源路径（用作关联数据，防止文件被替换）
     * @return 加密后的数据
     */
    fun encrypt(data: ByteArray, assetPath: String): ByteArray {
        Log.d(TAG, "加密资源: $assetPath (${data.size} bytes)")
        
        return try {
            val encrypted = AesCryptoEngine.encryptWithKey(
                plainData = data,
                secretKey = secretKey,
                associatedData = assetPath.toByteArray(Charsets.UTF_8)
            )
            
            // 添加加密标记头
            val result = buildEncryptedAsset(encrypted, assetPath)
            Log.d(TAG, "加密完成: $assetPath -> ${result.size} bytes")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "加密资源失败: $assetPath", e)
            throw CryptoException("加密资源失败: $assetPath", e)
        }
    }
    
    /**
     * 加密文本内容
     */
    fun encryptText(text: String, assetPath: String): ByteArray {
        return encrypt(text.toByteArray(Charsets.UTF_8), assetPath)
    }
    
    /**
     * 加密 JSON 配置
     */
    fun encryptJson(json: String, assetPath: String): ByteArray {
        return encryptText(json, assetPath)
    }
    
    /**
     * 批量加密资源
     */
    fun encryptBatch(assets: Map<String, ByteArray>): Map<String, ByteArray> {
        Log.d(TAG, "批量加密 ${assets.size} 个资源")
        
        return assets.mapValues { (path, data) ->
            encrypt(data, path)
        }
    }
    
    /**
     * 构建加密资源包
     * 格式：[4 bytes: path length][path bytes][encrypted data]
     */
    private fun buildEncryptedAsset(encryptedData: ByteArray, assetPath: String): ByteArray {
        val pathBytes = assetPath.toByteArray(Charsets.UTF_8)
        val pathLength = pathBytes.size
        
        return ByteArray(4 + pathLength + encryptedData.size).apply {
            // 写入路径长度（大端序）
            this[0] = ((pathLength shr 24) and 0xFF).toByte()
            this[1] = ((pathLength shr 16) and 0xFF).toByte()
            this[2] = ((pathLength shr 8) and 0xFF).toByte()
            this[3] = (pathLength and 0xFF).toByte()
            
            // 写入路径
            System.arraycopy(pathBytes, 0, this, 4, pathLength)
            
            // 写入加密数据
            System.arraycopy(encryptedData, 0, this, 4 + pathLength, encryptedData.size)
        }
    }
}

/**
 * 加密强度级别
 */
enum class EncryptionLevel(val iterations: Int, val description: String) {
    FAST(5000, "快速（较低安全性）"),
    STANDARD(10000, "标准（推荐）"),
    HIGH(50000, "高强度（较慢）"),
    PARANOID(100000, "极高强度（很慢）")
}

/**
 * 加密配置
 * 控制哪些资源需要加密以及安全保护选项
 */
data class EncryptionConfig(
    val enabled: Boolean = false,
    val encryptConfig: Boolean = true,      // Encryption app_config.json
    val encryptHtml: Boolean = true,        // Encryption HTML/CSS/JS 文件
    val encryptMedia: Boolean = false,      // Encryption媒体文件（图片/视频/音频）
    val encryptSplash: Boolean = false,     // Encryption启动画面
    val encryptBgm: Boolean = false,        // Encryption背景音乐
    val customPassword: String? = null,     // Custom密码（可选，增加安全性）
    
    // Security保护选项
    val enableIntegrityCheck: Boolean = true,    // Enable完整性检查
    val enableAntiDebug: Boolean = true,         // Enable反调试保护
    val enableAntiTamper: Boolean = true,        // Enable防篡改保护
    val enableRootDetection: Boolean = false,    // Enable Root 检测
    val enableEmulatorDetection: Boolean = false,// Enable模拟器检测
    val obfuscateStrings: Boolean = false,       // 混淆字符串（实验性）
    val encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD,  // Encryption强度
    
    // 运行时保护选项
    val enableRuntimeProtection: Boolean = true, // Enable运行时保护
    val blockOnThreat: Boolean = false,          // 检测到威胁时阻止运行
    val threatCheckInterval: Long = 5000L        // 威胁检测间隔（毫秒）
) {
    companion object {
        /**
         * 默认配置（不加密）
         */
        val DISABLED = EncryptionConfig(enabled = false)
        
        /**
         * 基础加密（仅加密配置和代码）
         */
        val BASIC = EncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = false,
            enableIntegrityCheck = true,
            enableAntiDebug = false,
            enableAntiTamper = false,
            encryptionLevel = EncryptionLevel.STANDARD
        )
        
        /**
         * 完全加密（加密所有资源）
         */
        val FULL = EncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = true,
            encryptSplash = true,
            encryptBgm = true,
            enableIntegrityCheck = true,
            enableAntiDebug = true,
            enableAntiTamper = true,
            encryptionLevel = EncryptionLevel.HIGH
        )
        
        /**
         * 最高安全级别
         */
        val MAXIMUM = EncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = true,
            encryptSplash = true,
            encryptBgm = true,
            enableIntegrityCheck = true,
            enableAntiDebug = true,
            enableAntiTamper = true,
            enableRootDetection = true,
            enableEmulatorDetection = false,
            obfuscateStrings = true,
            encryptionLevel = EncryptionLevel.PARANOID,
            enableRuntimeProtection = true,
            blockOnThreat = true
        )
    }
    
    /**
     * 判断指定资源是否需要加密
     */
    fun shouldEncrypt(assetPath: String): Boolean {
        if (!enabled) return false
        
        return when {
            assetPath == CryptoConstants.CONFIG_FILE -> encryptConfig
            assetPath.startsWith("html/") || assetPath.endsWith(".html") || 
            assetPath.endsWith(".css") || assetPath.endsWith(".js") -> encryptHtml
            assetPath.startsWith("splash_media.") -> encryptSplash
            assetPath.startsWith("bgm/") -> encryptBgm
            assetPath.startsWith("media_content.") -> encryptMedia
            isMediaFile(assetPath) -> encryptMedia
            else -> false
        }
    }
    
    private fun isMediaFile(path: String): Boolean {
        val ext = path.substringAfterLast('.', "").lowercase()
        return ext in setOf("png", "jpg", "jpeg", "gif", "webp", "mp4", "mp3", "wav", "ogg")
    }
    
    /**
     * 获取 PBKDF2 迭代次数
     */
    fun getKeyDerivationIterations(): Int = encryptionLevel.iterations
    
    /**
     * 是否需要任何安全保护
     */
    fun hasSecurityProtection(): Boolean {
        return enableIntegrityCheck || enableAntiDebug || enableAntiTamper || 
               enableRootDetection || enableEmulatorDetection || enableRuntimeProtection
    }
}
