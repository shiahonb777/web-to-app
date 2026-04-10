package com.webtoapp.core.crypto

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.security.SecureRandom

/**
 * 优化版 Native 加密引擎 JNI 桥接
 *
 * 相比原始 NativeCrypto 的改进：
 *
 * 1. **恒定时间 AES S-Box** — 防止缓存时序攻击
 * 2. **标准 PBKDF2-HMAC-SHA256** — 修复原始错误的密钥派生
 * 3. **HKDF (RFC 5869)** — 现代密钥派生函数
 * 4. **4 块并行 AES-GCM-CTR** — 提升吞吐量
 * 5. **双向加密支持** — 同时暴露 encrypt 和 decrypt
 * 6. **ARMv8 硬件 AES 检测** — 自动利用 CPU 加速
 *
 * 使用策略：
 * - 优先使用本优化引擎执行加解密
 * - 如果 native 库不可用，回退到 Java 的 AesCryptoEngine
 * - 密钥派生使用标准 PBKDF2，确保与 Java 层的密钥兼容
 */
object NativeCryptoOptimized {

    private const val TAG = "NativeCryptoOpt"

    @Volatile
    private var isLoaded = false

    @Volatile
    private var isAvailable = false

    @Volatile
    private var hasHwAes = false

    private val secureRandom = SecureRandom()

    init {
        try {
            System.loadLibrary("crypto_optimized")
            isAvailable = true
            isLoaded = true
            
            nativeInit()
            hasHwAes = nativeHasHwAes()
            
            AppLogger.i(TAG, "Optimized crypto engine loaded (HW AES: $hasHwAes)")
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.w(TAG, "Optimized crypto engine not available: ${e.message}")
            isLoaded = true
            isAvailable = false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize optimized crypto engine", e)
            isLoaded = true
            isAvailable = false
        }
    }

    /**
     * 检查优化引擎是否可用
     */
    fun isAvailable(): Boolean = isAvailable

    /**
     * 检查是否拥有硬件 AES 加速
     */
    fun hasHardwareAes(): Boolean = hasHwAes

    // ==================== AES-256-GCM 加解密 ====================

    /**
     * AES-256-GCM 加密
     *
     * @param plaintext 明文数据
     * @param key       32 字节密钥
     * @param aad       关联数据 (可选)
     * @return IV(12) + 密文 + Tag(16)，失败返回 null
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray, aad: ByteArray? = null): ByteArray? {
        if (!isAvailable) return null
        if (key.size != 32) {
            AppLogger.e(TAG, "Invalid key size: ${key.size}, expected 32")
            return null
        }

        return try {
            val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
            val encrypted = nativeEncrypt(plaintext, key, iv, aad)
            if (encrypted != null) {
                // 返回格式: IV(12) + encrypted(密文+tag)
                iv + encrypted
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native encryption failed", e)
            null
        }
    }

    /**
     * AES-256-GCM 解密
     *
     * @param data IV(12) + 密文 + Tag(16)
     * @param key  32 字节密钥
     * @param aad  关联数据 (可选)
     * @return 明文数据，认证失败返回 null
     */
    fun decrypt(data: ByteArray, key: ByteArray, aad: ByteArray? = null): ByteArray? {
        if (!isAvailable) return null
        if (key.size != 32) return null
        if (data.size < 12 + 16) return null // IV + tag 最小

        return try {
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)
            nativeDecrypt(ciphertext, key, iv, aad)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native decryption failed", e)
            null
        }
    }

    // ==================== 密钥派生 ====================

    /**
     * PBKDF2-HMAC-SHA256 密钥派生 (RFC 2898 标准实现)
     *
     * 相比原始 C++ 实现：
     * - 使用真正的 HMAC-SHA256 作为 PRF
     * - 完全符合 RFC 2898
     * - 输出与 Java 的 SecretKeyFactory PBKDF2WithHmacSHA256 兼容
     *
     * @param password   密码
     * @param salt       盐值
     * @param iterations 迭代次数
     * @param keyLength  输出密钥长度 (字节)
     * @return 派生密钥
     */
    fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int = 32): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativePbkdf2(password, salt, iterations, keyLength)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native PBKDF2 failed", e)
            null
        }
    }

    /**
     * HKDF 密钥派生 (RFC 5869)
     *
     * @param ikm    输入密钥材料
     * @param salt   盐值 (可选)
     * @param info   上下文信息
     * @param length 输出长度
     * @return 派生密钥
     */
    fun hkdf(ikm: ByteArray, salt: ByteArray? = null, info: ByteArray = ByteArray(0), length: Int = 32): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativeHkdf(ikm, salt, info, length)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native HKDF failed", e)
            null
        }
    }

    // ==================== 哈希 ====================

    /**
     * SHA-256 哈希
     */
    fun sha256(data: ByteArray): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativeSha256(data)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native SHA-256 failed", e)
            null
        }
    }

    /**
     * HMAC-SHA256
     */
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativeHmacSha256(key, data)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native HMAC-SHA256 failed", e)
            null
        }
    }

    // ==================== 高级 API ====================

    /**
     * 使用预派生密钥加密 (与 AesCryptoEngine.encryptWithKey 兼容)
     * 输出格式: IV(12) + Ciphertext + Tag(16)
     */
    fun encryptWithKey(plainData: ByteArray, keyBytes: ByteArray, associatedData: ByteArray? = null): ByteArray? {
        return encrypt(plainData, keyBytes, associatedData)
    }

    /**
     * 使用预派生密钥解密 (与 AesCryptoEngine.decryptWithKey 兼容)
     * 输入格式: IV(12) + Ciphertext + Tag(16)
     */
    fun decryptWithKey(encryptedPackage: ByteArray, keyBytes: ByteArray, associatedData: ByteArray? = null): ByteArray? {
        return decrypt(encryptedPackage, keyBytes, associatedData)
    }

    /**
     * 从包名和签名派生密钥 (与 AesCryptoEngine.deriveKeyFromPackage 兼容)
     */
    fun deriveKeyFromPackage(packageName: String, signature: ByteArray, iterations: Int = CryptoConstants.PBKDF2_ITERATIONS): ByteArray? {
        if (!isAvailable) return null

        val sigHash = sha256(signature) ?: return null
        val password = (packageName + ":" + sigHash.toHexString()).toByteArray()

        val baseSalt = CryptoConstants.KEY_DERIVATION_SALT
        val pkgHash = sha256(packageName.toByteArray()) ?: return null
        val salt = baseSalt + pkgHash.copyOf(16)

        return pbkdf2(password, salt, iterations, 32)
    }

    // ==================== Native 方法 ====================

    private external fun nativeInit(): Boolean
    private external fun nativeHasHwAes(): Boolean
    private external fun nativeEncrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray, aad: ByteArray?): ByteArray?
    private external fun nativeDecrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray, aad: ByteArray?): ByteArray?
    private external fun nativePbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray?
    private external fun nativeHkdf(ikm: ByteArray, salt: ByteArray?, info: ByteArray?, length: Int): ByteArray?
    private external fun nativeSha256(data: ByteArray): ByteArray?
    private external fun nativeHmacSha256(key: ByteArray, data: ByteArray): ByteArray?
}
