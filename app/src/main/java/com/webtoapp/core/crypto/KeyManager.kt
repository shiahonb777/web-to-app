package com.webtoapp.core.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.security.MessageDigest
import javax.crypto.SecretKey

/**
 * 密钥管理器
 * 负责生成、派生和管理加密密钥
 */
class KeyManager(private val context: Context) {
    
    companion object {
        private const val TAG = "KeyManager"
    }
    
    // Cache的密钥（避免重复派生）
    @Volatile
    private var cachedKey: SecretKey? = null
    
    @Volatile
    private var cachedIterations: Int = CryptoConstants.PBKDF2_ITERATIONS
    
    /**
     * 获取当前应用的加密密钥
     * 基于包名和签名派生，确保每个 APK 有唯一密钥
     */
    fun getAppKey(): SecretKey {
        cachedKey?.let { return it }
        
        synchronized(this) {
            cachedKey?.let { return it }
            
            val packageName = context.packageName
            val signature = getAppSignature()
            
            Log.d(TAG, "派生应用密钥: package=$packageName, sigHash=${signature.sha256().toHexString().take(16)}...")
            
            val key = AesCryptoEngine.deriveKeyFromPackage(packageName, signature)
            cachedKey = key
            return key
        }
    }
    
    /**
     * 获取当前应用的加密密钥（指定迭代次数）
     */
    fun getAppKey(iterations: Int): SecretKey {
        if (cachedKey != null && cachedIterations == iterations) {
            return cachedKey!!
        }
        
        synchronized(this) {
            if (cachedKey != null && cachedIterations == iterations) {
                return cachedKey!!
            }
            
            val packageName = context.packageName
            val signature = getAppSignature()
            
            Log.d(TAG, "派生应用密钥: package=$packageName, iterations=$iterations")
            
            val key = AesCryptoEngine.deriveKeyFromPackage(packageName, signature, iterations)
            cachedKey = key
            cachedIterations = iterations
            return key
        }
    }
    
    /**
     * 为指定包名生成加密密钥（用于打包时）
     */
    fun generateKeyForPackage(packageName: String, signatureHash: ByteArray): SecretKey {
        Log.d(TAG, "为包名生成密钥: $packageName")
        return AesCryptoEngine.deriveKeyFromPackage(packageName, signatureHash)
    }
    
    /**
     * 为指定包名生成加密密钥（指定加密级别）
     */
    fun generateKeyForPackage(
        packageName: String, 
        signatureHash: ByteArray,
        encryptionLevel: EncryptionLevel
    ): SecretKey {
        Log.d(TAG, "为包名生成密钥: $packageName, level=${encryptionLevel.name}")
        return AesCryptoEngine.deriveKeyFromPackage(
            packageName, 
            signatureHash, 
            encryptionLevel.iterations
        )
    }
    
    /**
     * 使用增强版 HKDF 派生密钥
     */
    fun deriveKeyWithHKDF(
        packageName: String,
        signatureHash: ByteArray,
        additionalEntropy: ByteArray? = null
    ): EnhancedCrypto.SecureKeyContainer {
        return EnhancedCrypto.deriveAppKey(packageName, signatureHash, additionalEntropy)
    }
    
    /**
     * Get app签名
     */
    @Suppress("DEPRECATION")
    fun getAppSignature(): ByteArray {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures.isNullOrEmpty()) {
                Log.w(TAG, "无法获取应用签名，使用默认值")
                return getDefaultSignature()
            }
            
            // 使用第一个签名的 SHA-256 哈希
            val signature = signatures[0]
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            
        } catch (e: Exception) {
            Log.e(TAG, "获取应用签名失败", e)
            getDefaultSignature()
        }
    }
    
    /**
     * 获取用于打包的签名哈希
     * 这个值会在打包时嵌入到 APK 中
     */
    fun getSignatureHashForBuild(): ByteArray {
        return getAppSignature()
    }
    
    /**
     * 验证当前应用签名是否与预期一致
     */
    fun verifySignature(expectedHash: ByteArray): Boolean {
        val currentHash = getAppSignature()
        return MessageDigest.isEqual(currentHash, expectedHash)
    }
    
    /**
     * 清除缓存的密钥
     */
    fun clearCache() {
        cachedKey = null
        cachedIterations = CryptoConstants.PBKDF2_ITERATIONS
    }
    
    /**
     * 默认签名（用于调试或签名获取失败时）
     */
    private fun getDefaultSignature(): ByteArray {
        // 使用包名的哈希作为后备
        val packageHash = context.packageName.toByteArray().sha256()
        // 添加一些设备特征
        val deviceInfo = "${Build.MANUFACTURER}:${Build.MODEL}:${Build.FINGERPRINT}"
        val deviceHash = deviceInfo.toByteArray().sha256()
        
        // 组合两个哈希
        return (packageHash + deviceHash).sha256()
    }
}

/**
 * 密钥派生参数
 * 用于在打包时传递密钥信息
 */
data class KeyDerivationParams(
    val packageName: String,
    val signatureHash: ByteArray,
    val salt: ByteArray = CryptoConstants.KEY_DERIVATION_SALT,
    val iterations: Int = CryptoConstants.PBKDF2_ITERATIONS,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeyDerivationParams
        return packageName == other.packageName &&
                signatureHash.contentEquals(other.signatureHash) &&
                salt.contentEquals(other.salt) &&
                iterations == other.iterations &&
                encryptionLevel == other.encryptionLevel
    }
    
    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + signatureHash.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iterations
        result = 31 * result + encryptionLevel.hashCode()
        return result
    }
}
