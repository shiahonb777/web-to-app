package com.webtoapp.core.crypto

import android.content.Context
import com.webtoapp.core.logging.AppLogger





object NativeCrypto {

    private const val TAG = "NativeCrypto"


    @Volatile
    private var isLoaded = false


    @Volatile
    private var isAvailable = false





    fun load(): Boolean {
        if (isLoaded) return isAvailable

        synchronized(this) {
            if (isLoaded) return isAvailable

            isAvailable = try {
                System.loadLibrary("crypto_engine")
                AppLogger.i(TAG, "Native crypto library loaded successfully")
                true
            } catch (e: UnsatisfiedLinkError) {


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

                AppLogger.e(TAG, "Critical error loading native crypto library: ${e.message}")
                false
            }

            isLoaded = true
        }

        return isAvailable
    }




    fun isNativeAvailable(): Boolean {
        load()
        return isAvailable
    }




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




    fun checkIntegrity(context: Context): Boolean {
        if (!isAvailable) {
            AppLogger.w(TAG, "Native library not available, skipping integrity check")
            return true
        }

        return try {
            verifyIntegrity(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Integrity check failed", e)
            false
        }
    }




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




    fun clear() {
        if (isAvailable) {
            try {
                clearCache()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to clear cache", e)
            }
        }
    }






    private external fun init(context: Context): Boolean




    private external fun decrypt(
        encrypted: ByteArray,
        packageName: String,
        signature: ByteArray
    ): ByteArray?




    private external fun verifyIntegrity(context: Context): Boolean




    private external fun clearCache()




    private external fun getSignatureHash(context: Context): String
}








class HybridDecryptor(private val context: Context) {

    private val javaDecryptor = AssetDecryptor(context)
    private val nativeAvailable: Boolean
    private val optimizedAvailable: Boolean

    init {

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




    fun decrypt(encrypted: ByteArray): ByteArray {

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


        return javaDecryptor.decrypt(encrypted)
    }





    private fun decryptWithOptimized(encrypted: ByteArray): ByteArray? {
        if (encrypted.size < 4) return null


        val pathLen = ((encrypted[0].toInt() and 0xFF) shl 24) or
                      ((encrypted[1].toInt() and 0xFF) shl 16) or
                      ((encrypted[2].toInt() and 0xFF) shl 8) or
                       (encrypted[3].toInt() and 0xFF)

        if (pathLen < 0 || pathLen > 1024) return null
        if (encrypted.size < 4 + pathLen + 12 + 16) return null

        val aad = encrypted.copyOfRange(4, 4 + pathLen)
        val encPart = encrypted.copyOfRange(4 + pathLen, encrypted.size)


        val keyManager = KeyManager.getInstance(context)
        val key = keyManager.getAppKey()
        val keyBytes = key.encoded

        return NativeCryptoOptimized.decryptWithKey(encPart, keyBytes, aad)
    }




    fun loadAsset(assetPath: String): ByteArray {
        return javaDecryptor.loadAsset(assetPath)
    }




    fun verifyIntegrity(): Boolean {
        return try {
            if (nativeAvailable) {
                NativeCrypto.checkIntegrity(context)
            } else {

                IntegrityChecker(context).quickCheck()
            }
        } catch (e: Exception) {
            AppLogger.w("HybridDecryptor", "Integrity check failed: ${e.message}")
            false
        }
    }
}
