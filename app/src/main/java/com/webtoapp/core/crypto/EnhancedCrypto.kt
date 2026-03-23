package com.webtoapp.core.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 增强版加密系统
 * 
 * 特性：
 * - HKDF 密钥派生
 * - Android Keystore 集成
 * - 内存安全保护
 * - 流式加密支持
 * - 多层密钥保护
 */
object EnhancedCrypto {
    
    private const val TAG = "EnhancedCrypto"
    private const val KEYSTORE_ALIAS = "WebToApp_MasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    
    // Encryption参数
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    
    // 流式加密块大小
    private const val STREAM_CHUNK_SIZE = 64 * 1024 // 64KB
    
    private val secureRandom = SecureRandom()
    
    /**
     * HKDF (HMAC-based Key Derivation Function) 实现
     * RFC 5869
     */
    object HKDF {
        private const val HASH_LEN = 32 // SHA-256
        
        /**
         * HKDF-Extract
         * 从输入密钥材料提取伪随机密钥
         */
        fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
            val actualSalt = salt ?: ByteArray(HASH_LEN)
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(actualSalt, HMAC_ALGORITHM))
            return mac.doFinal(ikm)
        }
        
        /**
         * HKDF-Expand
         * 将伪随机密钥扩展为所需长度
         */
        fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
            require(length <= 255 * HASH_LEN) { "Output length too large" }
            
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(prk, HMAC_ALGORITHM))
            
            val output = ByteArrayOutputStream()
            var t = ByteArray(0)
            var i = 1
            
            while (output.size() < length) {
                mac.reset()
                mac.update(t)
                mac.update(info)
                mac.update(i.toByte())
                t = mac.doFinal()
                output.write(t)
                i++
            }
            
            return output.toByteArray().copyOf(length)
        }
        
        /**
         * 完整的 HKDF 派生
         */
        fun derive(
            ikm: ByteArray,
            salt: ByteArray? = null,
            info: ByteArray = ByteArray(0),
            length: Int = 32
        ): ByteArray {
            val prk = extract(salt, ikm)
            return expand(prk, info, length)
        }
    }
    
    /**
     * 安全密钥容器
     * 提供内存保护，使用后自动清除
     */
    class SecureKeyContainer(private val keyBytes: ByteArray) : AutoCloseable {
        @Volatile
        private var isCleared = false
        
        fun getKey(): SecretKey {
            check(!isCleared) { "Key has been cleared" }
            return SecretKeySpec(keyBytes, "AES")
        }
        
        fun getBytes(): ByteArray {
            check(!isCleared) { "Key has been cleared" }
            return keyBytes.copyOf()
        }
        
        override fun close() {
            if (!isCleared) {
                // Security清除密钥
                keyBytes.fill(0)
                isCleared = true
            }
        }
        
        protected fun finalize() {
            close()
        }
    }
    
    /**
     * 从 Android Keystore 获取或创建主密钥
     */
    fun getOrCreateMasterKey(context: Context): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            // Check是否已存在
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
                return entry?.secretKey
            }
            
            // Create新密钥
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                
                val spec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE)
                    .setUserAuthenticationRequired(false)
                    .build()
                
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            } else {
                // Android 5.x 回退方案
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get/create master key", e)
            null
        }
    }
    
    /**
     * 派生应用专用密钥
     * 使用 HKDF 从多个因素派生
     */
    fun deriveAppKey(
        packageName: String,
        signature: ByteArray,
        additionalEntropy: ByteArray? = null
    ): SecureKeyContainer {
        // 组合输入密钥材料
        val ikm = ByteArrayOutputStream().apply {
            write(packageName.toByteArray())
            write(signature)
            additionalEntropy?.let { write(it) }
        }.toByteArray()
        
        // 使用固定盐
        val salt = "WebToApp:KeyDerivation:v2".toByteArray()
        
        // 上下文信息
        val info = "AES-256-GCM:AppEncryption".toByteArray()
        
        // 派生密钥
        val keyBytes = HKDF.derive(ikm, salt, info, 32)
        
        return SecureKeyContainer(keyBytes)
    }
    
    /**
     * 派生子密钥（用于不同用途）
     */
    fun deriveSubKey(
        masterKey: ByteArray,
        purpose: String,
        context: ByteArray = ByteArray(0)
    ): SecureKeyContainer {
        val info = ByteArrayOutputStream().apply {
            write(purpose.toByteArray())
            write(0) // 分隔符
            write(context)
        }.toByteArray()
        
        val keyBytes = HKDF.expand(masterKey, info, 32)
        return SecureKeyContainer(keyBytes)
    }
    
    /**
     * 加密数据（带关联数据）
     */
    fun encrypt(
        plaintext: ByteArray,
        key: SecretKey,
        associatedData: ByteArray? = null
    ): ByteArray {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        
        associatedData?.let { cipher.updateAAD(it) }
        
        val ciphertext = cipher.doFinal(plaintext)
        
        // 格式: IV + Ciphertext
        return iv + ciphertext
    }
    
    /**
     * 解密数据
     */
    fun decrypt(
        ciphertext: ByteArray,
        key: SecretKey,
        associatedData: ByteArray? = null
    ): ByteArray {
        require(ciphertext.size > IV_SIZE) { "Ciphertext too short" }
        
        val iv = ciphertext.copyOfRange(0, IV_SIZE)
        val encrypted = ciphertext.copyOfRange(IV_SIZE, ciphertext.size)
        
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        associatedData?.let { cipher.updateAAD(it) }
        
        return cipher.doFinal(encrypted)
    }
    
    /**
     * 流式加密
     * 适用于大文件，避免内存溢出
     */
    fun encryptStream(
        input: InputStream,
        output: OutputStream,
        key: SecretKey,
        associatedData: ByteArray? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        
        // 写入 IV
        output.write(iv)
        
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        
        associatedData?.let { cipher.updateAAD(it) }
        
        val buffer = ByteArray(STREAM_CHUNK_SIZE)
        var totalRead = 0L
        var bytesRead: Int
        
        // 先读取全部数据计算总大小（可选）
        val totalSize = input.available().toLong()
        
        while (input.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null && encrypted.isNotEmpty()) {
                output.write(encrypted)
            }
            totalRead += bytesRead
            onProgress?.invoke(totalRead, totalSize)
        }
        
        // 写入最终块（包含认证标签）
        val finalBlock = cipher.doFinal()
        output.write(finalBlock)
        
        return totalRead
    }
    
    /**
     * 流式解密
     */
    fun decryptStream(
        input: InputStream,
        output: OutputStream,
        key: SecretKey,
        associatedData: ByteArray? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long {
        // 读取 IV
        val iv = ByteArray(IV_SIZE)
        val ivRead = input.read(iv)
        require(ivRead == IV_SIZE) { "Failed to read IV" }
        
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        associatedData?.let { cipher.updateAAD(it) }
        
        // 对于 GCM 模式，需要一次性读取所有数据
        // 因为认证标签在末尾
        val encryptedData = input.readBytes()
        val decrypted = cipher.doFinal(encryptedData)
        
        output.write(decrypted)
        onProgress?.invoke(decrypted.size.toLong(), decrypted.size.toLong())
        
        return decrypted.size.toLong()
    }
    
    /**
     * 计算 HMAC
     */
    fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return mac.doFinal(data)
    }
    
    /**
     * 安全比较（防止时序攻击）
     */
    fun secureEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
    
    /**
     * 生成安全随机字节
     */
    fun randomBytes(size: Int): ByteArray {
        return ByteArray(size).also { secureRandom.nextBytes(it) }
    }
    
    /**
     * 安全清除字节数组
     */
    fun secureWipe(data: ByteArray) {
        data.fill(0)
        // 额外的随机覆盖
        secureRandom.nextBytes(data)
        data.fill(0)
    }
}

/**
 * 多层密钥保护
 * 使用密钥包装技术保护敏感密钥
 */
class KeyWrapper(private val context: Context) {
    
    private val masterKey: SecretKey? = EnhancedCrypto.getOrCreateMasterKey(context)
    
    /**
     * 包装（加密）密钥
     */
    fun wrap(keyToWrap: SecretKey): ByteArray? {
        val master = masterKey ?: return null
        
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.WRAP_MODE, master)
            
            val iv = cipher.iv
            val wrapped = cipher.wrap(keyToWrap)
            
            // 格式: IV长度(1) + IV + 包装密钥
            ByteArray(1 + iv.size + wrapped.size).apply {
                this[0] = iv.size.toByte()
                System.arraycopy(iv, 0, this, 1, iv.size)
                System.arraycopy(wrapped, 0, this, 1 + iv.size, wrapped.size)
            }
        } catch (e: Exception) {
            Log.e("KeyWrapper", "Failed to wrap key", e)
            null
        }
    }
    
    /**
     * 解包（解密）密钥
     */
    fun unwrap(wrappedKey: ByteArray): SecretKey? {
        val master = masterKey ?: return null
        
        return try {
            val ivLen = wrappedKey[0].toInt() and 0xFF
            val iv = wrappedKey.copyOfRange(1, 1 + ivLen)
            val wrapped = wrappedKey.copyOfRange(1 + ivLen, wrappedKey.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.UNWRAP_MODE, master, spec)
            
            cipher.unwrap(wrapped, "AES", Cipher.SECRET_KEY) as SecretKey
        } catch (e: Exception) {
            Log.e("KeyWrapper", "Failed to unwrap key", e)
            null
        }
    }
}

/**
 * 加密文件头
 * 包含版本、算法信息和元数据
 */
data class EncryptedFileHeader(
    val magic: Int = MAGIC,
    val version: Int = VERSION,
    val algorithm: Int = ALG_AES_256_GCM,
    val flags: Int = 0,
    val metadataSize: Int = 0,
    val metadata: ByteArray = ByteArray(0)
) {
    companion object {
        const val MAGIC = 0x57544132 // "WTA2"
        const val VERSION = 2
        const val ALG_AES_256_GCM = 1
        const val HEADER_SIZE = 16 // 不含元数据
        
        const val FLAG_HAS_AAD = 0x01
        const val FLAG_COMPRESSED = 0x02
        const val FLAG_CHUNKED = 0x04
        
        fun parse(data: ByteArray): EncryptedFileHeader? {
            if (data.size < HEADER_SIZE) return null
            
            val magic = (data[0].toInt() and 0xFF shl 24) or
                    (data[1].toInt() and 0xFF shl 16) or
                    (data[2].toInt() and 0xFF shl 8) or
                    (data[3].toInt() and 0xFF)
            
            if (magic != MAGIC) return null
            
            val version = (data[4].toInt() and 0xFF shl 8) or (data[5].toInt() and 0xFF)
            val algorithm = (data[6].toInt() and 0xFF shl 8) or (data[7].toInt() and 0xFF)
            val flags = (data[8].toInt() and 0xFF shl 8) or (data[9].toInt() and 0xFF)
            val metadataSize = (data[10].toInt() and 0xFF shl 24) or
                    (data[11].toInt() and 0xFF shl 16) or
                    (data[12].toInt() and 0xFF shl 8) or
                    (data[13].toInt() and 0xFF)
            
            val metadata = if (metadataSize > 0 && data.size >= HEADER_SIZE + metadataSize) {
                data.copyOfRange(HEADER_SIZE, HEADER_SIZE + metadataSize)
            } else {
                ByteArray(0)
            }
            
            return EncryptedFileHeader(magic, version, algorithm, flags, metadataSize, metadata)
        }
    }
    
    fun toByteArray(): ByteArray {
        val result = ByteArray(HEADER_SIZE + metadata.size)
        
        // Magic (4 bytes)
        result[0] = (magic shr 24).toByte()
        result[1] = (magic shr 16).toByte()
        result[2] = (magic shr 8).toByte()
        result[3] = magic.toByte()
        
        // Version (2 bytes)
        result[4] = (version shr 8).toByte()
        result[5] = version.toByte()
        
        // Algorithm (2 bytes)
        result[6] = (algorithm shr 8).toByte()
        result[7] = algorithm.toByte()
        
        // Flags (2 bytes)
        result[8] = (flags shr 8).toByte()
        result[9] = flags.toByte()
        
        // Metadata size (4 bytes)
        result[10] = (metadata.size shr 24).toByte()
        result[11] = (metadata.size shr 16).toByte()
        result[12] = (metadata.size shr 8).toByte()
        result[13] = metadata.size.toByte()
        
        // Reserved (2 bytes)
        result[14] = 0
        result[15] = 0
        
        // Metadata
        if (metadata.isNotEmpty()) {
            System.arraycopy(metadata, 0, result, HEADER_SIZE, metadata.size)
        }
        
        return result
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedFileHeader
        return magic == other.magic && version == other.version &&
                algorithm == other.algorithm && flags == other.flags &&
                metadata.contentEquals(other.metadata)
    }
    
    override fun hashCode(): Int {
        var result = magic
        result = 31 * result + version
        result = 31 * result + algorithm
        result = 31 * result + flags
        result = 31 * result + metadata.contentHashCode()
        return result
    }
}
