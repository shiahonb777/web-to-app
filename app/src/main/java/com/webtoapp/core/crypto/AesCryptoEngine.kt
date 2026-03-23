package com.webtoapp.core.crypto

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 加密引擎
 * 提供高强度的对称加密功能
 */
object AesCryptoEngine {
    
    private const val TAG = "AesCryptoEngine"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    
    // Key缓存（用于频繁加解密场景）
    private val keyCache = ConcurrentHashMap<String, CachedKey>()
    private const val KEY_CACHE_TTL_MS = 5 * 60 * 1000L // 5分钟过期
    private const val MAX_CACHE_SIZE = 10
    
    private val secureRandom = SecureRandom()
    
    /**
     * 缓存的密钥
     */
    private data class CachedKey(
        val key: SecretKey,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > KEY_CACHE_TTL_MS
    }
    
    /**
     * 加密数据
     * 
     * @param plainData 明文数据
     * @param password 密码（用于派生密钥）
     * @param associatedData 关联数据（用于 GCM 认证，可选）
     * @return 加密后的数据（包含头部信息）
     */
    fun encrypt(
        plainData: ByteArray,
        password: String,
        associatedData: ByteArray? = null
    ): ByteArray {
        try {
            // Generate随机盐和 IV
            val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
            val iv = ByteArray(CryptoConstants.AES_GCM_IV_SIZE).also { secureRandom.nextBytes(it) }
            
            // 派生密钥
            val secretKey = deriveKey(password, salt)
            
            // Initialize加密器
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(CryptoConstants.AES_GCM_TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            // 添加关联数据（如果有）
            associatedData?.let { cipher.updateAAD(it) }
            
            // Encryption
            val encryptedData = cipher.doFinal(plainData)
            
            // Build输出：Header + Salt + IV + EncryptedData
            return buildEncryptedPackage(salt, iv, encryptedData, associatedData != null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw CryptoException("加密失败: ${e.message}", e)
        }
    }
    
    /**
     * 解密数据
     * 
     * @param encryptedPackage 加密数据包（包含头部信息）
     * @param password 密码
     * @param associatedData 关联数据（必须与加密时一致）
     * @return 解密后的明文数据
     */
    fun decrypt(
        encryptedPackage: ByteArray,
        password: String,
        associatedData: ByteArray? = null
    ): ByteArray {
        try {
            // Parse加密包
            val (salt, iv, encryptedData, hasAAD) = parseEncryptedPackage(encryptedPackage)
            
            // Verify关联数据一致性
            if (hasAAD && associatedData == null) {
                throw CryptoException("缺少关联数据")
            }
            
            // 派生密钥
            val secretKey = deriveKey(password, salt)
            
            // Initialize解密器
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(CryptoConstants.AES_GCM_TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            // 添加关联数据（如果有）
            associatedData?.let { cipher.updateAAD(it) }
            
            // Decryption
            return cipher.doFinal(encryptedData)
            
        } catch (e: CryptoException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw CryptoException("解密失败: ${e.message}", e)
        }
    }
    
    /**
     * 使用预派生的密钥加密（性能优化）
     */
    fun encryptWithKey(
        plainData: ByteArray,
        secretKey: SecretKey,
        associatedData: ByteArray? = null
    ): ByteArray {
        try {
            val iv = ByteArray(CryptoConstants.AES_GCM_IV_SIZE).also { secureRandom.nextBytes(it) }
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(CryptoConstants.AES_GCM_TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            associatedData?.let { cipher.updateAAD(it) }
            
            val encryptedData = cipher.doFinal(plainData)
            
            // 简化格式：IV + EncryptedData（不含 salt，因为密钥已预派生）
            return ByteArrayOutputStream().use { baos ->
                DataOutputStream(baos).use { dos ->
                    dos.write(iv)
                    dos.write(encryptedData)
                }
                baos.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw CryptoException("加密失败: ${e.message}", e)
        }
    }
    
    /**
     * 使用预派生的密钥解密
     */
    fun decryptWithKey(
        encryptedPackage: ByteArray,
        secretKey: SecretKey,
        associatedData: ByteArray? = null
    ): ByteArray {
        try {
            if (encryptedPackage.size < CryptoConstants.AES_GCM_IV_SIZE) {
                throw CryptoException("加密数据太短")
            }
            
            val iv = encryptedPackage.copyOfRange(0, CryptoConstants.AES_GCM_IV_SIZE)
            val encryptedData = encryptedPackage.copyOfRange(CryptoConstants.AES_GCM_IV_SIZE, encryptedPackage.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(CryptoConstants.AES_GCM_TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            associatedData?.let { cipher.updateAAD(it) }
            
            return cipher.doFinal(encryptedData)
        } catch (e: CryptoException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw CryptoException("解密失败: ${e.message}", e)
        }
    }
    
    /**
     * 从密码派生密钥
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        return deriveKey(password, salt, CryptoConstants.PBKDF2_ITERATIONS)
    }
    
    /**
     * 从密码派生密钥（指定迭代次数）
     */
    fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            CryptoConstants.AES_KEY_SIZE
        )
        
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, KEY_ALGORITHM)
        } finally {
            // Security清理密码字符数组
            spec.clearPassword()
        }
    }
    
    /**
     * 从密码派生密钥（带缓存）
     * 适用于需要频繁加解密的场景
     */
    fun deriveKeyWithCache(password: String, salt: ByteArray): SecretKey {
        val cacheKey = password.hashCode().toString() + ":" + salt.contentHashCode()
        
        // Cleanup过期缓存
        cleanExpiredCache()
        
        // Check缓存
        keyCache[cacheKey]?.let { cached ->
            if (!cached.isExpired()) {
                return cached.key
            }
            keyCache.remove(cacheKey)
        }
        
        // 派生新密钥
        val key = deriveKey(password, salt)
        
        // Cache密钥（限制缓存大小）
        if (keyCache.size < MAX_CACHE_SIZE) {
            keyCache[cacheKey] = CachedKey(key)
        }
        
        return key
    }
    
    /**
     * 清理过期的密钥缓存
     */
    private fun cleanExpiredCache() {
        val expiredKeys = keyCache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { keyCache.remove(it) }
    }
    
    /**
     * 清除所有密钥缓存
     */
    fun clearKeyCache() {
        keyCache.clear()
    }
    
    /**
     * 从密码和包名派生密钥（用于 APK 加密）
     */
    fun deriveKeyFromPackage(packageName: String, signature: ByteArray): SecretKey {
        return deriveKeyFromPackage(packageName, signature, CryptoConstants.PBKDF2_ITERATIONS)
    }
    
    /**
     * 从密码和包名派生密钥（指定迭代次数）
     */
    fun deriveKeyFromPackage(packageName: String, signature: ByteArray, iterations: Int): SecretKey {
        // 组合包名和签名作为密码
        val password = packageName + ":" + signature.toHexString()
        
        // 使用固定盐 + 包名哈希
        val salt = CryptoConstants.KEY_DERIVATION_SALT + packageName.toByteArray().sha256().copyOf(16)
        
        return deriveKey(password, salt, iterations)
    }
    
    /**
     * 检查数据是否为加密格式
     */
    fun isEncrypted(data: ByteArray): Boolean {
        if (data.size < 8) return false
        
        return try {
            ByteArrayInputStream(data).use { bais ->
                DataInputStream(bais).use { dis ->
                    val magic = dis.readInt()
                    magic == CryptoConstants.ENCRYPTED_HEADER_MAGIC
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 构建加密数据包
     * 格式：Magic(4) + Version(1) + Flags(1) + SaltLen(2) + Salt + IVLen(2) + IV + Data
     */
    private fun buildEncryptedPackage(
        salt: ByteArray,
        iv: ByteArray,
        encryptedData: ByteArray,
        hasAAD: Boolean
    ): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                // 写入魔数
                dos.writeInt(CryptoConstants.ENCRYPTED_HEADER_MAGIC)
                // 写入版本
                dos.writeByte(CryptoConstants.ENCRYPTED_HEADER_VERSION)
                // 写入标志位（bit 0: hasAAD）
                dos.writeByte(if (hasAAD) 1 else 0)
                // 写入盐
                dos.writeShort(salt.size)
                dos.write(salt)
                // 写入 IV
                dos.writeShort(iv.size)
                dos.write(iv)
                // 写入加密数据
                dos.write(encryptedData)
            }
            baos.toByteArray()
        }
    }
    
    /**
     * 解析加密数据包
     */
    private fun parseEncryptedPackage(data: ByteArray): EncryptedPackage {
        return ByteArrayInputStream(data).use { bais ->
            DataInputStream(bais).use { dis ->
                // 读取魔数
                val magic = dis.readInt()
                if (magic != CryptoConstants.ENCRYPTED_HEADER_MAGIC) {
                    throw CryptoException("无效的加密文件格式")
                }
                
                // 读取版本
                val version = dis.readByte().toInt()
                if (version > CryptoConstants.ENCRYPTED_HEADER_VERSION) {
                    throw CryptoException("不支持的加密版本: $version")
                }
                
                // 读取标志位
                val flags = dis.readByte().toInt()
                val hasAAD = (flags and 1) != 0
                
                // 读取盐
                val saltLen = dis.readShort().toInt()
                val salt = ByteArray(saltLen)
                dis.readFully(salt)
                
                // 读取 IV
                val ivLen = dis.readShort().toInt()
                val iv = ByteArray(ivLen)
                dis.readFully(iv)
                
                // 读取加密数据
                val encryptedData = dis.readBytes()
                
                EncryptedPackage(salt, iv, encryptedData, hasAAD)
            }
        }
    }
    
    private data class EncryptedPackage(
        val salt: ByteArray,
        val iv: ByteArray,
        val encryptedData: ByteArray,
        val hasAAD: Boolean
    )
}

/**
 * 加密异常
 */
class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * ByteArray 扩展函数
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun ByteArray.sha256(): ByteArray {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(this)
}
