package com.webtoapp.core.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec











object EnhancedCrypto {

    private const val TAG = "EnhancedCrypto"
    private const val KEYSTORE_ALIAS = "WebToApp_MasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"


    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128


    private const val STREAM_CHUNK_SIZE = 64 * 1024

    private val secureRandom = SecureRandom()





    object HKDF {
        private const val HASH_LEN = 32





        fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
            val actualSalt = salt ?: ByteArray(HASH_LEN)
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(actualSalt, HMAC_ALGORITHM))
            return mac.doFinal(ikm)
        }





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

                keyBytes.fill(0)
                isCleared = true
            }
        }

        protected fun finalize() {
            close()
        }
    }




    fun getOrCreateMasterKey(context: Context): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)


            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
                return entry?.secretKey
            }

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
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get/create master key", e)
            null
        }
    }





    fun deriveAppKey(
        packageName: String,
        signature: ByteArray,
        additionalEntropy: ByteArray? = null
    ): SecureKeyContainer {

        val ikm = ByteArrayOutputStream().apply {
            write(packageName.toByteArray())
            write(signature)
            additionalEntropy?.let { write(it) }
        }.toByteArray()


        val salt = "WebToApp:KeyDerivation:v2".toByteArray()


        val info = "AES-256-GCM:AppEncryption".toByteArray()


        val keyBytes = HKDF.derive(ikm, salt, info, 32)

        return SecureKeyContainer(keyBytes)
    }




    fun deriveSubKey(
        masterKey: ByteArray,
        purpose: String,
        context: ByteArray = ByteArray(0)
    ): SecureKeyContainer {
        val info = ByteArrayOutputStream().apply {
            write(purpose.toByteArray())
            write(0)
            write(context)
        }.toByteArray()

        val keyBytes = HKDF.expand(masterKey, info, 32)
        return SecureKeyContainer(keyBytes)
    }




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


        return iv + ciphertext
    }




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





    fun encryptStream(
        input: InputStream,
        output: OutputStream,
        key: SecretKey,
        associatedData: ByteArray? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }


        output.write(iv)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        associatedData?.let { cipher.updateAAD(it) }

        val buffer = ByteArray(STREAM_CHUNK_SIZE)
        var totalRead = 0L
        var bytesRead: Int


        val totalSize = input.available().toLong()

        while (input.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null && encrypted.isNotEmpty()) {
                output.write(encrypted)
            }
            totalRead += bytesRead
            onProgress?.invoke(totalRead, totalSize)
        }


        val finalBlock = cipher.doFinal()
        output.write(finalBlock)

        return totalRead
    }







    fun decryptStream(
        input: InputStream,
        output: OutputStream,
        key: SecretKey,
        associatedData: ByteArray? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long {

        val iv = ByteArray(IV_SIZE)
        val ivRead = input.read(iv)
        require(ivRead == IV_SIZE) { "Failed to read IV" }

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        associatedData?.let { cipher.updateAAD(it) }


        val encryptedData = input.readBytes()
        val maxDecryptSize = 50L * 1024 * 1024
        require(encryptedData.size <= maxDecryptSize) {
            "Encrypted data too large for in-memory decryption: ${encryptedData.size} bytes. " +
            "Consider using chunked encryption for files > ${maxDecryptSize / 1024 / 1024}MB."
        }
        val decrypted = cipher.doFinal(encryptedData)

        output.write(decrypted)
        onProgress?.invoke(decrypted.size.toLong(), decrypted.size.toLong())

        return decrypted.size.toLong()
    }




    fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return mac.doFinal(data)
    }




    fun secureEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }




    fun randomBytes(size: Int): ByteArray {
        return ByteArray(size).also { secureRandom.nextBytes(it) }
    }




    fun secureWipe(data: ByteArray) {
        data.fill(0)

        secureRandom.nextBytes(data)
        data.fill(0)
    }
}





class KeyWrapper(private val context: Context) {

    private val masterKey: SecretKey? = EnhancedCrypto.getOrCreateMasterKey(context)




    fun wrap(keyToWrap: SecretKey): ByteArray? {
        val master = masterKey ?: return null

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.WRAP_MODE, master)

            val iv = cipher.iv
            val wrapped = cipher.wrap(keyToWrap)


            ByteArray(1 + iv.size + wrapped.size).apply {
                this[0] = iv.size.toByte()
                System.arraycopy(iv, 0, this, 1, iv.size)
                System.arraycopy(wrapped, 0, this, 1 + iv.size, wrapped.size)
            }
        } catch (e: Exception) {
            AppLogger.e("KeyWrapper", "Failed to wrap key", e)
            null
        }
    }




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
            AppLogger.e("KeyWrapper", "Failed to unwrap key", e)
            null
        }
    }
}





data class EncryptedFileHeader(
    val magic: Int = MAGIC,
    val version: Int = VERSION,
    val algorithm: Int = ALG_AES_256_GCM,
    val flags: Int = 0,
    val metadataSize: Int = 0,
    val metadata: ByteArray = ByteArray(0)
) {
    companion object {
        const val MAGIC = 0x57544132
        const val VERSION = 2
        const val ALG_AES_256_GCM = 1
        const val HEADER_SIZE = 16

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


        result[0] = (magic shr 24).toByte()
        result[1] = (magic shr 16).toByte()
        result[2] = (magic shr 8).toByte()
        result[3] = magic.toByte()


        result[4] = (version shr 8).toByte()
        result[5] = version.toByte()


        result[6] = (algorithm shr 8).toByte()
        result[7] = algorithm.toByte()


        result[8] = (flags shr 8).toByte()
        result[9] = flags.toByte()


        result[10] = (metadata.size shr 24).toByte()
        result[11] = (metadata.size shr 16).toByte()
        result[12] = (metadata.size shr 8).toByte()
        result[13] = metadata.size.toByte()


        result[14] = 0
        result[15] = 0


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
