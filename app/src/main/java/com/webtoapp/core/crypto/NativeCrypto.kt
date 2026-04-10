package com.webtoapp.core.crypto

import android.content.Context
import com.webtoapp.core.logging.AppLogger

/**
 * Native 加密引擎 JNI 桥接
 * 提供 Native 层的加密/解密功能
 */
object NativeCrypto {
    
    private const val TAG = "NativeCrypto"
    
    // Native 库是否已加载
    @Volatile
    private var isLoaded = false
    
    // Native 库是否可用
    @Volatile
    private var isAvailable = false
    
    /**
     * 加载 Native 库
     * 在模拟器或某些设备上可能加载失败，此时会回退到 Java 实现
     */
    fun load(): Boolean {
        if (isLoaded) return isAvailable
        
        synchronized(this) {
            if (isLoaded) return isAvailable
            
            isAvailable = try {
                System.loadLibrary("crypto_engine")
                AppLogger.i(TAG, "Native crypto library loaded successfully")
                true
            } catch (e: UnsatisfiedLinkError) {
                // 在模拟器或某些设备上可能没有对应架构的 native 库
                // 这是正常情况，会自动回退到 Java 实现
                AppLogger.w(TAG, "Native crypto library not available: ${e.message}")
                AppLogger.w(TAG, "This is normal on emulators or some devices, using Java fallback")
                false
            } catch (e: SecurityException) {
                AppLogger.w(TAG, "Security exception loading native library: ${e.message}")
                false
            } catch (e: Exception) {
                AppLogger.e(TAG, "Unexpected error loading native crypto library", e)
                false
            } catch (e: Error) {
                // 捕获所有 Error（包括 NoClassDefFoundError, ExceptionInInitializerError 等）
                AppLogger.e(TAG, "Critical error loading native crypto library: ${e.message}")
                false
            }
            
            isLoaded = true
        }
        
        return isAvailable
    }
    
    /**
     * 检查 Native 库是否可用
     */
    fun isNativeAvailable(): Boolean {
        load()
        return isAvailable
    }
    
    /**
     * 初始化加密引擎
     */
    fun initialize(context: Context): Boolean {
        if (!load()) {
            AppLogger.w(TAG, "Native library not available, using Java fallback")
            return false
        }
        
        return try {
            init(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize native crypto", e)
            false
        }
    }
    
    /**
     * 解密数据
     * 
     * @param encrypted 加密的数据
     * @param context 上下文
     * @return 解密后的数据，失败返回 null
     */
    fun decryptData(encrypted: ByteArray, context: Context): ByteArray? {
        if (!isAvailable) {
            AppLogger.w(TAG, "Native library not available")
            return null
        }
        
        return try {
            val keyManager = KeyManager.getInstance(context)
            val packageName = context.packageName
            val signature = keyManager.getAppSignature()
            
            decrypt(encrypted, packageName, signature)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native decryption failed", e)
            null
        }
    }
    
    /**
     * 验证完整性
     */
    fun checkIntegrity(context: Context): Boolean {
        if (!isAvailable) {
            AppLogger.w(TAG, "Native library not available, skipping integrity check")
            return true  // 如果 Native 不可用，跳过检查
        }
        
        return try {
            verifyIntegrity(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Integrity check failed", e)
            false
        }
    }
    
    /**
     * 获取签名哈希
     */
    fun getSignatureHashString(context: Context): String? {
        if (!isAvailable) {
            return null
        }
        
        return try {
            getSignatureHash(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get signature hash", e)
            null
        }
    }
    
    /**
     * 清除缓存
     */
    fun clear() {
        if (isAvailable) {
            try {
                clearCache()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to clear cache", e)
            }
        }
    }
    
    // ==================== Native 方法 ====================
    
    /**
     * 初始化加密引擎
     */
    private external fun init(context: Context): Boolean
    
    /**
     * 解密数据
     */
    private external fun decrypt(
        encrypted: ByteArray,
        packageName: String,
        signature: ByteArray
    ): ByteArray?
    
    /**
     * 验证完整性
     */
    private external fun verifyIntegrity(context: Context): Boolean
    
    /**
     * 清除缓存
     */
    private external fun clearCache()
    
    /**
     * 获取签名哈希
     */
    private external fun getSignatureHash(context: Context): String
}

/**
 * 混合解密器
 * 三层策略：
 * 1. 优化版 Native (恒定时间 AES, 标准 PBKDF2) — 最优先
 * 2. 原始 Native (兼容旧数据) — 备选
 * 3. Java 实现 — 最终回退
 */
class HybridDecryptor(private val context: Context) {
    
    private val javaDecryptor = AssetDecryptor(context)
    private val nativeAvailable: Boolean
    private val optimizedAvailable: Boolean
    
    init {
        // 检查优化版 Native 引擎
        optimizedAvailable = try {
            NativeCryptoOptimized.isAvailable().also { available ->
                if (available) {
                    AppLogger.i("HybridDecryptor", "Using optimized native crypto" +
                        " (HW AES: ${NativeCryptoOptimized.hasHardwareAes()})")
                }
            }
        } catch (e: Exception) {
            AppLogger.w("HybridDecryptor", "Optimized crypto check failed: ${e.message}")
            false
        } catch (e: Error) {
            false
        }
        
        // 检查原始 Native 引擎
        nativeAvailable = try {
            NativeCrypto.isNativeAvailable().also { available ->
                if (available) {
                    NativeCrypto.initialize(context)
                }
            }
        } catch (e: Exception) {
            AppLogger.w("HybridDecryptor", "Failed to check native availability: ${e.message}")
            false
        } catch (e: Error) {
            AppLogger.e("HybridDecryptor", "Critical error checking native availability: ${e.message}")
            false
        }
    }
    
    /**
     * 解密数据 (三层策略)
     */
    fun decrypt(encrypted: ByteArray): ByteArray {
        // 策略 1: 优化版 Native 解密
        if (optimizedAvailable) {
            try {
                val result = decryptWithOptimized(encrypted)
                if (result != null) return result
                AppLogger.w("HybridDecryptor", "Optimized native returned null, trying legacy")
            } catch (e: Exception) {
                AppLogger.w("HybridDecryptor", "Optimized native failed: ${e.message}")
            } catch (e: Error) {
                AppLogger.e("HybridDecryptor", "Optimized native error: ${e.message}")
            }
        }
        
        // 策略 2: 原始 Native 解密
        if (nativeAvailable) {
            try {
                val result = NativeCrypto.decryptData(encrypted, context)
                if (result != null) {
                    return result
                }
                AppLogger.w("HybridDecryptor", "Native decryption returned null, falling back to Java")
            } catch (e: Exception) {
                AppLogger.w("HybridDecryptor", "Native decryption failed: ${e.message}, falling back to Java")
            } catch (e: Error) {
                AppLogger.e("HybridDecryptor", "Critical error in native decryption: ${e.message}, falling back to Java")
            }
        }
        
        // 策略 3: Java 实现
        return javaDecryptor.decrypt(encrypted)
    }
    
    /**
     * 使用优化版引擎解密
     * 解析加密资源包格式: [4 bytes: path_len][path][IV(12)][ciphertext+tag(16)]
     */
    private fun decryptWithOptimized(encrypted: ByteArray): ByteArray? {
        if (encrypted.size < 4) return null
        
        // 读取路径长度
        val pathLen = ((encrypted[0].toInt() and 0xFF) shl 24) or
                      ((encrypted[1].toInt() and 0xFF) shl 16) or
                      ((encrypted[2].toInt() and 0xFF) shl 8) or
                       (encrypted[3].toInt() and 0xFF)
        
        if (pathLen < 0 || pathLen > 1024) return null
        if (encrypted.size < 4 + pathLen + 12 + 16) return null
        
        val aad = encrypted.copyOfRange(4, 4 + pathLen) // path 作为 AAD
        val encPart = encrypted.copyOfRange(4 + pathLen, encrypted.size) // IV + ciphertext + tag
        
        // 获取密钥
        val keyManager = KeyManager.getInstance(context)
        val key = keyManager.getAppKey()
        val keyBytes = key.encoded
        
        return NativeCryptoOptimized.decryptWithKey(encPart, keyBytes, aad)
    }
    
    /**
     * 加载并解密资源
     */
    fun loadAsset(assetPath: String): ByteArray {
        return javaDecryptor.loadAsset(assetPath)
    }
    
    /**
     * 验证完整性
     */
    fun verifyIntegrity(): Boolean {
        return try {
            if (nativeAvailable) {
                NativeCrypto.checkIntegrity(context)
            } else {
                // Java 实现的完整性检查
                IntegrityChecker(context).quickCheck()
            }
        } catch (e: Exception) {
            AppLogger.w("HybridDecryptor", "Integrity check failed: ${e.message}")
            false  // SECURITY: default to fail-closed on integrity check exceptions
        }
    }
}
