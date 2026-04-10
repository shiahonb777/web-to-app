package com.webtoapp.core.crypto

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.google.gson.annotations.SerializedName
import java.io.InputStream
import javax.crypto.SecretKey

/**
 * 资源解密器
 * 用于在运行时解密加密的资源文件
 */
class AssetDecryptor(private val context: Context) {
    
    companion object {
        private const val TAG = "AssetDecryptor"
        private const val ENCRYPTION_META_FILE = "encryption_meta.json"
    }
    
    private val gson = com.webtoapp.util.GsonProvider.gson
    private val keyManager = KeyManager.getInstance(context)
    @Volatile
    private var cachedKey: SecretKey? = null
    @Volatile
    private var cachedMetadata: EncryptionMetadataRuntime? = null
    
    /**
     * 获取解密密钥
     * 优先使用 encryption_meta.json 中存储的签名哈希
     */
    private fun getKey(): SecretKey {
        cachedKey?.let { return it }
        
        synchronized(this) {
            cachedKey?.let { return it }
            
            val key = try {
                // 尝试从元数据文件获取签名哈希
                val metadata = loadEncryptionMetadata()
                if (metadata != null && metadata.signatureHash.isNotBlank()) {
                    AppLogger.d(TAG, "使用元数据中的签名哈希派生密钥")
                    val signatureHash = metadata.signatureHash.hexToByteArray()
                    if (signatureHash.isNotEmpty()) {
                        keyManager.generateKeyForPackage(metadata.packageName, signatureHash)
                    } else {
                        AppLogger.w(TAG, "签名哈希转换失败，使用当前应用签名派生密钥")
                        keyManager.getAppKey()
                    }
                } else {
                    AppLogger.d(TAG, "元数据不存在，使用当前应用签名派生密钥")
                    keyManager.getAppKey()
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "获取密钥失败，使用当前应用签名派生密钥", e)
                keyManager.getAppKey()
            }
            
            cachedKey = key
            return key
        }
    }
    
    /**
     * 加载加密元数据（不加密存储）
     */
    private fun loadEncryptionMetadata(): EncryptionMetadataRuntime? {
        cachedMetadata?.let { return it }
        
        return try {
            val metaJson = context.assets.open(ENCRYPTION_META_FILE).use { 
                it.bufferedReader().readText() 
            }
            val metadata = gson.fromJson(metaJson, EncryptionMetadataRuntime::class.java)
            cachedMetadata = metadata
            AppLogger.d(TAG, "加载加密元数据成功")
            metadata
        } catch (e: Exception) {
            AppLogger.d(TAG, "加密元数据不存在或无效: ${e.message}")
            null
        }
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     */
    private fun String.hexToByteArray(): ByteArray {
        if (length % 2 != 0) {
            AppLogger.w(TAG, "十六进制字符串长度不是偶数: $length")
            return ByteArray(0)
        }
        return try {
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            AppLogger.e(TAG, "十六进制字符串转换失败", e)
            ByteArray(0)
        }
    }
    
    /**
     * 解密资源数据
     * 
     * @param encryptedData 加密的数据
     * @return 解密后的数据
     */
    fun decrypt(encryptedData: ByteArray): ByteArray {
        return try {
            // Parse加密资源包
            val (assetPath, encrypted) = parseEncryptedAsset(encryptedData)
            
            AppLogger.d(TAG, "解密资源: $assetPath")
            
            // Decryption
            val decrypted = AesCryptoEngine.decryptWithKey(
                encryptedPackage = encrypted,
                secretKey = getKey(),
                associatedData = assetPath.toByteArray(Charsets.UTF_8)
            )
            
            AppLogger.d(TAG, "解密完成: $assetPath")
            decrypted
            
        } catch (e: CryptoException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Decryption failed", e)
            throw CryptoException("解密失败: ${e.message}", e)
        }
    }
    
    /**
     * 解密为文本
     */
    fun decryptToString(encryptedData: ByteArray): String {
        return String(decrypt(encryptedData), Charsets.UTF_8)
    }
    
    /**
     * 从 assets 加载并解密资源
     * 
     * @param assetPath 资源路径（不含 .enc 后缀）
     * @return 解密后的数据，如果资源不存在或未加密则返回原始数据
     */
    fun loadAsset(assetPath: String): ByteArray {
        // 首先尝试加载加密版本
        val encryptedPath = assetPath + CryptoConstants.ENCRYPTED_EXTENSION
        
        // 尝试加载加密版本
        val hasEncrypted = try {
            context.assets.open(encryptedPath).close()
            true
        } catch (e: Exception) {
            false
        }
        
        if (hasEncrypted) {
            try {
                val encryptedData = context.assets.open(encryptedPath).use { it.readBytes() }
                AppLogger.d(TAG, "加载加密资源: $encryptedPath")
                return try {
                    decrypt(encryptedData)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "解密失败，尝试加载明文备份: $assetPath", e)
                    // 解密失败，尝试加载明文备份（修复：加密构建时同时写入了明文兜底）
                    try {
                        loadOriginalAsset(assetPath)
                    } catch (e2: Exception) {
                        AppLogger.e(TAG, "明文备份也不存在: $assetPath", e2)
                        // 重新抛出原始解密异常，让调用方知道真正的失败原因
                        throw CryptoException("解密失败且无明文备份: $assetPath (原因: ${e.message})", e)
                    }
                }
            } catch (e: CryptoException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "读取加密文件失败: $encryptedPath", e)
                // 加密文件读取异常，回退到明文
            }
        }
        
        // 没有加密版本或读取失败，加载原始版本
        AppLogger.d(TAG, "加载原始资源: $assetPath")
        return loadOriginalAsset(assetPath)
    }
    
    /**
     * 加载原始（未加密）资源
     */
    private fun loadOriginalAsset(assetPath: String): ByteArray {
        return try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Resource not found: $assetPath", e)
            throw CryptoException("Resource not found: $assetPath", e)
        }
    }
    
    /**
     * 从 assets 加载并解密为文本
     */
    fun loadAssetAsString(assetPath: String): String {
        return String(loadAsset(assetPath), Charsets.UTF_8)
    }
    
    /**
     * 获取资源输入流（自动处理加密）
     * 注意：对于加密资源，会先完全解密到内存
     */
    fun openAsset(assetPath: String): InputStream {
        val data = loadAsset(assetPath)
        return data.inputStream()
    }
    
    /**
     * Check if resource exists (encrypted or not)
     */
    fun assetExists(assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath + CryptoConstants.ENCRYPTED_EXTENSION).close()
            true
        } catch (e: Exception) {
            try {
                context.assets.open(assetPath).close()
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
    
    /**
     * 检查资源是否为加密格式
     */
    fun isEncrypted(assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath + CryptoConstants.ENCRYPTED_EXTENSION).close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 解析加密资源包
     * 格式：[4 bytes: path length][path bytes][encrypted data]
     */
    private fun parseEncryptedAsset(data: ByteArray): Pair<String, ByteArray> {
        if (data.size < 4) {
            throw CryptoException("加密数据太短")
        }
        
        // 读取路径长度（大端序）
        val pathLength = ((data[0].toInt() and 0xFF) shl 24) or
                ((data[1].toInt() and 0xFF) shl 16) or
                ((data[2].toInt() and 0xFF) shl 8) or
                (data[3].toInt() and 0xFF)
        
        if (pathLength < 0 || pathLength > 1024) {
            throw CryptoException("无效的路径长度: $pathLength")
        }
        
        if (data.size < 4 + pathLength) {
            throw CryptoException("加密数据不完整")
        }
        
        // 读取路径
        val pathBytes = data.copyOfRange(4, 4 + pathLength)
        val assetPath = String(pathBytes, Charsets.UTF_8)
        
        // 读取加密数据
        val encryptedData = data.copyOfRange(4 + pathLength, data.size)
        
        return assetPath to encryptedData
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cachedKey = null
        cachedMetadata = null
        keyManager.clearCache()
    }
}

/**
 * 运行时加密元数据（从 encryption_meta.json 读取）
 */
data class EncryptionMetadataRuntime(
    @SerializedName("version")
    val version: Int = 1,
    
    @SerializedName("encryptConfig")
    val encryptConfig: Boolean = true,
    
    @SerializedName("encryptHtml")
    val encryptHtml: Boolean = true,
    
    @SerializedName("encryptMedia")
    val encryptMedia: Boolean = false,
    
    @SerializedName("encryptSplash")
    val encryptSplash: Boolean = false,
    
    @SerializedName("encryptBgm")
    val encryptBgm: Boolean = false,
    
    @SerializedName("packageName")
    val packageName: String = "",
    
    @SerializedName("signatureHash")
    val signatureHash: String = ""
)
