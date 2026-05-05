package com.webtoapp.core.crypto

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.google.gson.annotations.SerializedName
import java.io.InputStream
import javax.crypto.SecretKey





class AssetDecryptor(private val context: Context) {

    companion object {
        private const val TAG = "AssetDecryptor"
        private const val ENCRYPTION_META_FILE = "encryption_meta.json"
    }

    private val gson = com.webtoapp.util.GsonProvider.gson
    private val keyManager = KeyManager.getInstance(context)
    @Volatile
    private var cachedKey: SecretKey? = null
    @Volatile
    private var cachedMetadata: EncryptionMetadataRuntime? = null


    @Volatile
    private var customPassword: String? = null






    fun setCustomPassword(password: String?) {
        customPassword = password
        cachedKey = null
    }




    fun requiresCustomPassword(): Boolean {
        val metadata = loadEncryptionMetadata()
        return metadata?.usesCustomPassword == true && customPassword.isNullOrBlank()
    }






    private fun getKey(): SecretKey {
        cachedKey?.let { return it }

        synchronized(this) {
            cachedKey?.let { return it }

            val key = try {
                val metadata = loadEncryptionMetadata()


                if (metadata?.usesCustomPassword == true && customPassword.isNullOrBlank()) {
                    throw CryptoException(Strings.cryptoCustomPasswordRequired)
                }



                if (metadata != null && metadata.signatureHash.isNotBlank()) {

                    AppLogger.d(TAG, "旧版 APK 兼容：使用元数据中的签名哈希派生密钥")
                    val signatureHash = metadata.signatureHash.hexToByteArray()
                    if (signatureHash.isNotEmpty()) {
                        keyManager.generateKeyForPackage(metadata.packageName, signatureHash)
                    } else {
                        keyManager.getAppKey()
                    }
                } else {

                    val packageName = metadata?.packageName ?: context.packageName
                    val signature = keyManager.getAppSignature()
                    AppLogger.d(TAG, "使用当前 APK 签名派生密钥 (hasCustomPassword=${!customPassword.isNullOrBlank()})")
                    keyManager.generateKeyForPackage(packageName, signature, customPassword)
                }
            } catch (e: CryptoException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "获取密钥失败，使用当前应用签名派生密钥", e)
                keyManager.getAppKey()
            }

            cachedKey = key
            return key
        }
    }




    private fun loadEncryptionMetadata(): EncryptionMetadataRuntime? {
        cachedMetadata?.let { return it }

        return try {
            val metaJson = context.assets.open(ENCRYPTION_META_FILE).use {
                it.bufferedReader().readText()
            }
            val metadata = gson.fromJson(metaJson, EncryptionMetadataRuntime::class.java)
            cachedMetadata = metadata
            AppLogger.d(TAG, "加载加密元数据成功")
            metadata
        } catch (e: Exception) {
            AppLogger.d(TAG, "加密元数据不存在或无效: ${e.message}")
            null
        }
    }




    private fun String.hexToByteArray(): ByteArray {
        if (length % 2 != 0) {
            AppLogger.w(TAG, "十六进制字符串长度不是偶数: $length")
            return ByteArray(0)
        }
        return try {
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            AppLogger.e(TAG, "十六进制字符串转换失败", e)
            ByteArray(0)
        }
    }







    fun decrypt(encryptedData: ByteArray): ByteArray {
        return try {

            val (assetPath, encrypted) = parseEncryptedAsset(encryptedData)

            AppLogger.d(TAG, "解密资源: $assetPath")


            val decrypted = AesCryptoEngine.decryptWithKey(
                encryptedPackage = encrypted,
                secretKey = getKey(),
                associatedData = assetPath.toByteArray(Charsets.UTF_8)
            )

            AppLogger.d(TAG, "解密完成: $assetPath")
            decrypted

        } catch (e: CryptoException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Decryption failed", e)
            throw CryptoException(Strings.cryptoDecryptFailed.format(e.message), e)
        }
    }




    fun decryptToString(encryptedData: ByteArray): String {
        return String(decrypt(encryptedData), Charsets.UTF_8)
    }







    fun loadAsset(assetPath: String): ByteArray {

        val encryptedPath = assetPath + CryptoConstants.ENCRYPTED_EXTENSION


        val hasEncrypted = try {
            context.assets.open(encryptedPath).close()
            true
        } catch (e: Exception) {
            false
        }

        if (hasEncrypted) {
            try {
                val encryptedData = context.assets.open(encryptedPath).use { it.readBytes() }
                AppLogger.d(TAG, "加载加密资源: $encryptedPath")
                return try {
                    decrypt(encryptedData)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "解密失败，尝试加载明文备份: $assetPath", e)

                    try {
                        loadOriginalAsset(assetPath)
                    } catch (e2: Exception) {
                        AppLogger.e(TAG, "明文备份也不存在: $assetPath", e2)

                        throw CryptoException(Strings.cryptoDecryptFailedNoBackup.format(assetPath, e.message), e)
                    }
                }
            } catch (e: CryptoException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "读取加密文件失败: $encryptedPath", e)

            }
        }


        AppLogger.d(TAG, "加载原始资源: $assetPath")
        return loadOriginalAsset(assetPath)
    }




    private fun loadOriginalAsset(assetPath: String): ByteArray {
        return try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Resource not found: $assetPath", e)
            throw CryptoException("Resource not found: $assetPath", e)
        }
    }




    fun loadAssetAsString(assetPath: String): String {
        return String(loadAsset(assetPath), Charsets.UTF_8)
    }





    fun openAsset(assetPath: String): InputStream {
        val data = loadAsset(assetPath)
        return data.inputStream()
    }




    fun assetExists(assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath + CryptoConstants.ENCRYPTED_EXTENSION).close()
            true
        } catch (e: Exception) {
            try {
                context.assets.open(assetPath).close()
                true
            } catch (e2: Exception) {
                false
            }
        }
    }




    fun isEncrypted(assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath + CryptoConstants.ENCRYPTED_EXTENSION).close()
            true
        } catch (e: Exception) {
            false
        }
    }





    private fun parseEncryptedAsset(data: ByteArray): Pair<String, ByteArray> {
        if (data.size < 4) {
            throw CryptoException("加密数据太短")
        }


        val pathLength = ((data[0].toInt() and 0xFF) shl 24) or
                ((data[1].toInt() and 0xFF) shl 16) or
                ((data[2].toInt() and 0xFF) shl 8) or
                (data[3].toInt() and 0xFF)

        if (pathLength < 0 || pathLength > 1024) {
            throw CryptoException(Strings.cryptoInvalidPathLength.format(pathLength))
        }

        if (data.size < 4 + pathLength) {
            throw CryptoException("加密数据不完整")
        }


        val pathBytes = data.copyOfRange(4, 4 + pathLength)
        val assetPath = String(pathBytes, Charsets.UTF_8)


        val encryptedData = data.copyOfRange(4 + pathLength, data.size)

        return assetPath to encryptedData
    }




    fun clearCache() {
        cachedKey = null
        cachedMetadata = null
        keyManager.clearCache()
    }
}




data class EncryptionMetadataRuntime(
    @SerializedName("version")
    val version: Int = 1,

    @SerializedName("packageName")
    val packageName: String = "",


    @SerializedName("signatureHash")
    val signatureHash: String = "",


    @SerializedName("usesCustomPassword")
    val usesCustomPassword: Boolean = false
)
