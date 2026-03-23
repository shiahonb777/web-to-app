package com.webtoapp.core.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 字符串混淆器
 * 
 * 用于在编译时混淆敏感字符串，运行时解密
 * 防止静态分析工具直接提取字符串
 */
object StringObfuscator {
    
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 16  // 128-bit for faster decryption
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    
    // 混淆密钥（编译时生成，每个 APK 不同）
    @Volatile
    private var obfuscationKey: ByteArray? = null
    
    /**
     * 初始化混淆器
     * 
     * @param packageName 包名
     * @param signature 签名哈希
     */
    fun initialize(packageName: String, signature: ByteArray) {
        // 从包名和签名派生混淆密钥
        val combined = packageName.toByteArray() + signature
        obfuscationKey = EnhancedCrypto.HKDF.derive(
            ikm = combined,
            salt = CryptoConstants.HKDF_SALT,
            info = CryptoConstants.HKDF_INFO_OBFUSCATION,
            length = KEY_SIZE
        )
    }
    
    /**
     * 混淆字符串（编译时使用）
     * 
     * @param plaintext 原始字符串
     * @param key 混淆密钥
     * @return Base64 编码的混淆字符串
     */
    fun obfuscate(plaintext: String, key: ByteArray): String {
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key.copyOf(KEY_SIZE), "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // 格式: IV + Ciphertext
        val result = iv + encrypted
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }
    
    /**
     * 解混淆字符串（运行时使用）
     * 
     * @param obfuscated Base64 编码的混淆字符串
     * @return 原始字符串
     */
    fun deobfuscate(obfuscated: String): String {
        val key = obfuscationKey ?: throw IllegalStateException("StringObfuscator not initialized")
        
        val data = Base64.decode(obfuscated, Base64.NO_WRAP)
        require(data.size > IV_SIZE) { "Invalid obfuscated data" }
        
        val iv = data.copyOfRange(0, IV_SIZE)
        val encrypted = data.copyOfRange(IV_SIZE, data.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }
    
    /**
     * 简单 XOR 混淆（用于非敏感字符串，更快）
     */
    fun xorObfuscate(plaintext: String, key: ByteArray): ByteArray {
        val bytes = plaintext.toByteArray(Charsets.UTF_8)
        val result = ByteArray(bytes.size)
        
        for (i in bytes.indices) {
            result[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        
        return result
    }
    
    /**
     * 简单 XOR 解混淆
     */
    fun xorDeobfuscate(obfuscated: ByteArray, key: ByteArray): String {
        val result = ByteArray(obfuscated.size)
        
        for (i in obfuscated.indices) {
            result[i] = (obfuscated[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        
        return String(result, Charsets.UTF_8)
    }
    
    /**
     * 生成混淆后的字符串数组代码
     * 用于代码生成
     */
    fun generateObfuscatedCode(varName: String, plaintext: String, key: ByteArray): String {
        val obfuscated = xorObfuscate(plaintext, key)
        val byteArrayStr = obfuscated.joinToString(", ") { 
            "0x${(it.toInt() and 0xFF).toString(16).padStart(2, '0')}" 
        }
        
        return """
            private val ${varName}_obf = byteArrayOf($byteArrayStr)
            private val $varName: String by lazy { 
                StringObfuscator.xorDeobfuscate(${varName}_obf, obfuscationKey!!) 
            }
        """.trimIndent()
    }
}

/**
 * 混淆字符串包装类
 * 延迟解密，使用后可清除
 */
class ObfuscatedString private constructor(
    private val obfuscatedData: String
) {
    @Volatile
    private var cachedValue: String? = null
    
    companion object {
        /**
         * 创建混淆字符串
         */
        fun of(obfuscated: String): ObfuscatedString {
            return ObfuscatedString(obfuscated)
        }
    }
    
    /**
     * 获取解密后的字符串
     */
    fun get(): String {
        return cachedValue ?: StringObfuscator.deobfuscate(obfuscatedData).also {
            cachedValue = it
        }
    }
    
    /**
     * 清除缓存的明文
     */
    fun clear() {
        cachedValue = null
    }
    
    override fun toString(): String = get()
}
