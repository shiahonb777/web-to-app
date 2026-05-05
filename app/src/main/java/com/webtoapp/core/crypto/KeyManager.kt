package com.webtoapp.core.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey





class KeyManager(private val context: Context) {

    companion object {
        private const val TAG = "KeyManager"

        @Volatile
        private var INSTANCE: KeyManager? = null





        fun getInstance(context: Context): KeyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }


    @Volatile
    private var cachedKey: SecretKey? = null

    @Volatile
    private var cachedIterations: Int = CryptoConstants.PBKDF2_ITERATIONS


    private val keysByIterations = ConcurrentHashMap<Int, SecretKey>()





    fun getAppKey(): SecretKey {
        cachedKey?.let { return it }

        synchronized(this) {
            cachedKey?.let { return it }

            val packageName = context.packageName
            val signature = getAppSignature()

            AppLogger.d(TAG, "派生应用密钥: package=$packageName")

            val key = AesCryptoEngine.deriveKeyFromPackage(packageName, signature)
            cachedKey = key
            return key
        }
    }




    fun getAppKey(iterations: Int): SecretKey {

        keysByIterations[iterations]?.let { return it }

        synchronized(this) {
            keysByIterations[iterations]?.let { return it }

            val packageName = context.packageName
            val signature = getAppSignature()

            AppLogger.d(TAG, "派生应用密钥: package=$packageName, iterations=$iterations")

            val key = AesCryptoEngine.deriveKeyFromPackage(packageName, signature, iterations)
            keysByIterations[iterations] = key

            cachedKey = key
            cachedIterations = iterations
            return key
        }
    }




    fun generateKeyForPackage(
        packageName: String,
        signatureHash: ByteArray
    ): SecretKey {
        AppLogger.d(TAG, "为包名生成密钥: $packageName")
        return AesCryptoEngine.deriveKeyFromPackage(
            packageName,
            signatureHash,
            100000
        )
    }





    fun generateKeyForPackage(
        packageName: String,
        signatureHash: ByteArray,
        customPassword: String?
    ): SecretKey {
        AppLogger.d(TAG, "为包名生成密钥: $packageName, hasCustomPassword=${!customPassword.isNullOrBlank()}")
        return AesCryptoEngine.deriveKeyFromPackage(
            packageName,
            signatureHash,
            100000,
            customPassword
        )
    }




    fun deriveKeyWithHKDF(
        packageName: String,
        signatureHash: ByteArray,
        additionalEntropy: ByteArray? = null
    ): EnhancedCrypto.SecureKeyContainer {
        return EnhancedCrypto.deriveAppKey(packageName, signatureHash, additionalEntropy)
    }




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
                AppLogger.w(TAG, "无法获取应用签名，使用默认值")
                return getDefaultSignature()
            }


            val signature = signatures[0]
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())

        } catch (e: Exception) {
            AppLogger.e(TAG, "获取应用签名失败", e)
            getDefaultSignature()
        }
    }





    fun getSignatureHashForBuild(): ByteArray {
        return getAppSignature()
    }




    fun verifySignature(expectedHash: ByteArray): Boolean {
        val currentHash = getAppSignature()
        return MessageDigest.isEqual(currentHash, expectedHash)
    }




    fun clearCache() {
        cachedKey = null
        cachedIterations = CryptoConstants.PBKDF2_ITERATIONS
        keysByIterations.clear()
    }




    private fun getDefaultSignature(): ByteArray {


        AppLogger.e(TAG, "SECURITY: 无法获取真实应用签名，使用后备签名。加密强度已降低！")

        val packageHash = context.packageName.toByteArray().sha256()

        val deviceInfo = "${Build.MANUFACTURER}:${Build.MODEL}:${Build.FINGERPRINT}"
        val deviceHash = deviceInfo.toByteArray().sha256()


        return (packageHash + deviceHash).sha256()
    }
}






data class KeyDerivationParams(
    val packageName: String,
    val signatureHash: ByteArray,
    val iterations: Int = CryptoConstants.PBKDF2_ITERATIONS,
    val customPassword: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeyDerivationParams
        return packageName == other.packageName &&
                signatureHash.contentEquals(other.signatureHash) &&
                iterations == other.iterations &&
                customPassword == other.customPassword
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + signatureHash.contentHashCode()
        result = 31 * result + iterations
        result = 31 * result + (customPassword?.hashCode() ?: 0)
        return result
    }
}
