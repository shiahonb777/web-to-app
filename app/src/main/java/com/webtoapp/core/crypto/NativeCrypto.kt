package com.webtoapp.core.crypto

import android.content.Context
import android.util.Log

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
                Log.i(TAG, "Native crypto library loaded successfully")
                true
            } catch (e: UnsatisfiedLinkError) {
                // 在模拟器或某些设备上可能没有对应架构的 native 库
                // 这是正常情况，会自动回退到 Java 实现
                Log.w(TAG, "Native crypto library not available: ${e.message}")
                Log.w(TAG, "This is normal on emulators or some devices, using Java fallback")
                false
            } catch (e: SecurityException) {
                Log.w(TAG, "Security exception loading native library: ${e.message}")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading native crypto library", e)
                false
            } catch (e: Error) {
                // 捕获所有 Error（包括 NoClassDefFoundError, ExceptionInInitializerError 等）
                Log.e(TAG, "Critical error loading native crypto library: ${e.message}")
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
            Log.w(TAG, "Native library not available, using Java fallback")
            return false
        }
        
        return try {
            init(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize native crypto", e)
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
            Log.w(TAG, "Native library not available")
            return null
        }
        
        return try {
            val keyManager = KeyManager(context)
            val packageName = context.packageName
            val signature = keyManager.getAppSignature()
            
            decrypt(encrypted, packageName, signature)
        } catch (e: Exception) {
            Log.e(TAG, "Native decryption failed", e)
            null
        }
    }
    
    /**
     * 验证完整性
     */
    fun checkIntegrity(context: Context): Boolean {
        if (!isAvailable) {
            Log.w(TAG, "Native library not available, skipping integrity check")
            return true  // 如果 Native 不可用，跳过检查
        }
        
        return try {
            verifyIntegrity(context)
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed", e)
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
            Log.e(TAG, "Failed to get signature hash", e)
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
                Log.e(TAG, "Failed to clear cache", e)
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
 * 优先使用 Native 解密，失败时回退到 Java 实现
 */
class HybridDecryptor(private val context: Context) {
    
    private val javaDecryptor = AssetDecryptor(context)
    private val nativeAvailable: Boolean
    
    init {
        // Security地检查 Native 库是否可用
        nativeAvailable = try {
            NativeCrypto.isNativeAvailable().also { available ->
                if (available) {
                    NativeCrypto.initialize(context)
                }
            }
        } catch (e: Exception) {
            Log.w("HybridDecryptor", "Failed to check native availability: ${e.message}")
            false
        } catch (e: Error) {
            Log.e("HybridDecryptor", "Critical error checking native availability: ${e.message}")
            false
        }
    }
    
    /**
     * 解密数据
     */
    fun decrypt(encrypted: ByteArray): ByteArray {
        // 优先尝试 Native 解密
        if (nativeAvailable) {
            try {
                val result = NativeCrypto.decryptData(encrypted, context)
                if (result != null) {
                    return result
                }
                Log.w("HybridDecryptor", "Native decryption returned null, falling back to Java")
            } catch (e: Exception) {
                Log.w("HybridDecryptor", "Native decryption failed: ${e.message}, falling back to Java")
            } catch (e: Error) {
                Log.e("HybridDecryptor", "Critical error in native decryption: ${e.message}, falling back to Java")
            }
        }
        
        // 回退到 Java 实现
        return javaDecryptor.decrypt(encrypted)
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
            Log.w("HybridDecryptor", "Integrity check failed: ${e.message}")
            true  // Check失败时默认通过，避免阻止正常使用
        }
    }
}
