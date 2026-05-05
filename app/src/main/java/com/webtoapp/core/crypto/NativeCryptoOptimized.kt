package com.webtoapp.core.crypto

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.security.SecureRandom


















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




    fun isAvailable(): Boolean = isAvailable




    fun hasHardwareAes(): Boolean = hasHwAes











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

                iv + encrypted
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native encryption failed", e)
            null
        }
    }









    fun decrypt(data: ByteArray, key: ByteArray, aad: ByteArray? = null): ByteArray? {
        if (!isAvailable) return null
        if (key.size != 32) return null
        if (data.size < 12 + 16) return null

        return try {
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)
            nativeDecrypt(ciphertext, key, iv, aad)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native decryption failed", e)
            null
        }
    }

















    fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int = 32): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativePbkdf2(password, salt, iterations, keyLength)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native PBKDF2 failed", e)
            null
        }
    }










    fun hkdf(ikm: ByteArray, salt: ByteArray? = null, info: ByteArray = ByteArray(0), length: Int = 32): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativeHkdf(ikm, salt, info, length)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native HKDF failed", e)
            null
        }
    }






    fun sha256(data: ByteArray): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativeSha256(data)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native SHA-256 failed", e)
            null
        }
    }




    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativeHmacSha256(key, data)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native HMAC-SHA256 failed", e)
            null
        }
    }







    fun encryptWithKey(plainData: ByteArray, keyBytes: ByteArray, associatedData: ByteArray? = null): ByteArray? {
        return encrypt(plainData, keyBytes, associatedData)
    }





    fun decryptWithKey(encryptedPackage: ByteArray, keyBytes: ByteArray, associatedData: ByteArray? = null): ByteArray? {
        return decrypt(encryptedPackage, keyBytes, associatedData)
    }




    fun deriveKeyFromPackage(packageName: String, signature: ByteArray, iterations: Int = CryptoConstants.PBKDF2_ITERATIONS): ByteArray? {
        return deriveKeyFromPackage(packageName, signature, iterations, null)
    }





    fun deriveKeyFromPackage(packageName: String, signature: ByteArray, iterations: Int, customPassword: String?): ByteArray? {
        if (!isAvailable) return null

        val sigHash = sha256(signature) ?: return null
        val password = if (!customPassword.isNullOrBlank()) {
            (packageName + ":" + sigHash.toHexString() + ":" + customPassword).toByteArray()
        } else {
            (packageName + ":" + sigHash.toHexString()).toByteArray()
        }


        val saltInput = if (!customPassword.isNullOrBlank()) {
            packageName.toByteArray() + sigHash + customPassword.toByteArray()
        } else {
            packageName.toByteArray() + sigHash
        }
        val salt = sha256(saltInput) ?: return null

        return pbkdf2(password, salt, iterations, 32)
    }



    private external fun nativeInit(): Boolean
    private external fun nativeHasHwAes(): Boolean
    private external fun nativeEncrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray, aad: ByteArray?): ByteArray?
    private external fun nativeDecrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray, aad: ByteArray?): ByteArray?
    private external fun nativePbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray?
    private external fun nativeHkdf(ikm: ByteArray, salt: ByteArray?, info: ByteArray?, length: Int): ByteArray?
    private external fun nativeSha256(data: ByteArray): ByteArray?
    private external fun nativeHmacSha256(key: ByteArray, data: ByteArray): ByteArray?
}
