package com.webtoapp.core.crypto

import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import javax.crypto.SecretKey





class AssetEncryptor(private val secretKey: SecretKey) {

    companion object {
        private const val TAG = "AssetEncryptor"
    }








    fun encrypt(data: ByteArray, assetPath: String): ByteArray {
        AppLogger.d(TAG, "加密资源: $assetPath")

        return try {
            val aad = assetPath.toByteArray(Charsets.UTF_8)






            val encrypted = AesCryptoEngine.encryptWithKey(
                plainData = data,
                secretKey = secretKey,
                associatedData = aad
            )


            val result = buildEncryptedAsset(encrypted, assetPath)
            AppLogger.d(TAG, "加密完成: $assetPath (native: ${NativeCryptoOptimized.isAvailable()})")
            result

        } catch (e: Exception) {
            AppLogger.e(TAG, "加密资源失败: $assetPath", e)
            throw CryptoException(Strings.cryptoEncryptAssetFailed.format(assetPath), e)
        }
    }




    fun encryptText(text: String, assetPath: String): ByteArray {
        return encrypt(text.toByteArray(Charsets.UTF_8), assetPath)
    }




    fun encryptJson(json: String, assetPath: String): ByteArray {
        return encryptText(json, assetPath)
    }




    fun encryptBatch(assets: Map<String, ByteArray>): Map<String, ByteArray> {
        AppLogger.d(TAG, "批量加密 ${assets.size} 个资源")

        return assets.mapValues { (path, data) ->
            encrypt(data, path)
        }
    }





    private fun buildEncryptedAsset(encryptedData: ByteArray, assetPath: String): ByteArray {
        val pathBytes = assetPath.toByteArray(Charsets.UTF_8)
        val pathLength = pathBytes.size

        return ByteArray(4 + pathLength + encryptedData.size).apply {

            this[0] = ((pathLength shr 24) and 0xFF).toByte()
            this[1] = ((pathLength shr 16) and 0xFF).toByte()
            this[2] = ((pathLength shr 8) and 0xFF).toByte()
            this[3] = (pathLength and 0xFF).toByte()


            System.arraycopy(pathBytes, 0, this, 4, pathLength)


            System.arraycopy(encryptedData, 0, this, 4 + pathLength, encryptedData.size)
        }
    }
}









data class EncryptionConfig(
    val enabled: Boolean = false,
    val customPassword: String? = null
) {
    companion object {
        private const val PBKDF2_ITERATIONS = 100000

        val DISABLED = EncryptionConfig(enabled = false)

        val MAXIMUM = EncryptionConfig(enabled = true)
    }

    fun shouldEncrypt(assetPath: String): Boolean {
        if (!enabled) return false
        return true
    }

    fun getKeyDerivationIterations(): Int = PBKDF2_ITERATIONS

    fun hasSecurityProtection(): Boolean = enabled
}
