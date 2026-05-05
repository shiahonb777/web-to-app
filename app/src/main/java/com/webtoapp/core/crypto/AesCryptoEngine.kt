package com.webtoapp.core.crypto

import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
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





object AesCryptoEngine {

    private const val TAG = "AesCryptoEngine"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"


    private val keyCache = ConcurrentHashMap<String, CachedKey>()
    private const val KEY_CACHE_TTL_MS = 5 * 60 * 1000L
    private const val MAX_CACHE_SIZE = 10

    private val secureRandom = SecureRandom()




    private data class CachedKey(
        val key: SecretKey,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > KEY_CACHE_TTL_MS
    }









    fun encrypt(
        plainData: ByteArray,
        password: String,
        associatedData: ByteArray? = null
    ): ByteArray {
        try {

            val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
            val iv = ByteArray(CryptoConstants.AES_GCM_IV_SIZE).also { secureRandom.nextBytes(it) }


            val secretKey = deriveKey(password, salt)


            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(CryptoConstants.AES_GCM_TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)


            associatedData?.let { cipher.updateAAD(it) }


            val encryptedData = cipher.doFinal(plainData)


            return buildEncryptedPackage(salt, iv, encryptedData, associatedData != null)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Encryption failed", e)
            throw CryptoException(Strings.cryptoEncryptFailed.format(e.message), e)
        }
    }









    fun decrypt(
        encryptedPackage: ByteArray,
        password: String,
        associatedData: ByteArray? = null
    ): ByteArray {
        try {

            val (salt, iv, encryptedData, hasAAD) = parseEncryptedPackage(encryptedPackage)


            if (hasAAD && associatedData == null) {
                throw CryptoException("缺少关联数据")
            }


            val secretKey = deriveKey(password, salt)


            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(CryptoConstants.AES_GCM_TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)


            associatedData?.let { cipher.updateAAD(it) }


            return cipher.doFinal(encryptedData)

        } catch (e: CryptoException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Decryption failed", e)
            throw CryptoException(Strings.cryptoDecryptFailed.format(e.message), e)
        }
    }




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


            return ByteArrayOutputStream().use { baos ->
                DataOutputStream(baos).use { dos ->
                    dos.write(iv)
                    dos.write(encryptedData)
                }
                baos.toByteArray()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Encryption failed", e)
            throw CryptoException(Strings.cryptoEncryptFailed.format(e.message), e)
        }
    }




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
            AppLogger.e(TAG, "Decryption failed", e)
            throw CryptoException(Strings.cryptoDecryptFailed.format(e.message), e)
        }
    }




    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        return deriveKey(password, salt, CryptoConstants.PBKDF2_ITERATIONS)
    }




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

            spec.clearPassword()
        }
    }





    fun deriveKeyWithCache(password: String, salt: ByteArray): SecretKey {

        val cacheKey = (password + ":" + salt.toHexString()).toByteArray().sha256().toHexString()


        cleanExpiredCache()


        keyCache[cacheKey]?.let { cached ->
            if (!cached.isExpired()) {
                return cached.key
            }
            keyCache.remove(cacheKey)
        }


        val key = deriveKey(password, salt)


        if (keyCache.size < MAX_CACHE_SIZE) {
            keyCache.putIfAbsent(cacheKey, CachedKey(key))
        }

        return key
    }




    private fun cleanExpiredCache() {
        val expiredKeys = keyCache.entries
            .filter { it.value.isExpired() }
            .map { it.key }

        expiredKeys.forEach { keyCache.remove(it) }
    }




    fun clearKeyCache() {
        keyCache.clear()
    }




    fun deriveKeyFromPackage(packageName: String, signature: ByteArray): SecretKey {
        return deriveKeyFromPackage(packageName, signature, CryptoConstants.PBKDF2_ITERATIONS, null)
    }




    fun deriveKeyFromPackage(packageName: String, signature: ByteArray, iterations: Int): SecretKey {
        return deriveKeyFromPackage(packageName, signature, iterations, null)
    }





    fun deriveKeyFromPackage(
        packageName: String,
        signature: ByteArray,
        iterations: Int,
        customPassword: String?
    ): SecretKey {


        val password = if (!customPassword.isNullOrBlank()) {
            packageName + ":" + signature.toHexString() + ":" + customPassword
        } else {
            packageName + ":" + signature.toHexString()
        }



        val saltInput = if (!customPassword.isNullOrBlank()) {
            packageName.toByteArray() + signature + customPassword.toByteArray()
        } else {
            packageName.toByteArray() + signature
        }
        val salt = saltInput.sha256().copyOf(32)

        return deriveKey(password, salt, iterations)
    }




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





    private fun buildEncryptedPackage(
        salt: ByteArray,
        iv: ByteArray,
        encryptedData: ByteArray,
        hasAAD: Boolean
    ): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->

                dos.writeInt(CryptoConstants.ENCRYPTED_HEADER_MAGIC)

                dos.writeByte(CryptoConstants.ENCRYPTED_HEADER_VERSION)

                dos.writeByte(if (hasAAD) 1 else 0)

                dos.writeShort(salt.size)
                dos.write(salt)

                dos.writeShort(iv.size)
                dos.write(iv)

                dos.write(encryptedData)
            }
            baos.toByteArray()
        }
    }




    private fun parseEncryptedPackage(data: ByteArray): EncryptedPackage {
        return ByteArrayInputStream(data).use { bais ->
            DataInputStream(bais).use { dis ->

                val magic = dis.readInt()
                if (magic != CryptoConstants.ENCRYPTED_HEADER_MAGIC) {
                    throw CryptoException("无效的加密文件格式")
                }


                val version = dis.readByte().toInt()
                if (version > CryptoConstants.ENCRYPTED_HEADER_VERSION) {
                    throw CryptoException(Strings.cryptoUnsupportedVersion.format(version))
                }


                val flags = dis.readByte().toInt()
                val hasAAD = (flags and 1) != 0


                val saltLen = dis.readShort().toInt()
                if (saltLen < 0 || saltLen > 64) {
                    throw CryptoException("Invalid salt length: $saltLen (expected 0-64)")
                }
                val salt = ByteArray(saltLen)
                dis.readFully(salt)


                val ivLen = dis.readShort().toInt()
                if (ivLen < 0 || ivLen > 32) {
                    throw CryptoException("Invalid IV length: $ivLen (expected 0-32)")
                }
                val iv = ByteArray(ivLen)
                dis.readFully(iv)


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




class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)




fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun ByteArray.sha256(): ByteArray {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(this)
}
