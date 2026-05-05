package com.webtoapp.core.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec







object StringObfuscator {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 16
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128


    @Volatile
    private var obfuscationKey: ByteArray? = null







    fun initialize(packageName: String, signature: ByteArray) {

        val combined = packageName.toByteArray() + signature
        obfuscationKey = EnhancedCrypto.HKDF.derive(
            ikm = combined,
            salt = CryptoConstants.HKDF_SALT,
            info = CryptoConstants.HKDF_INFO_OBFUSCATION,
            length = KEY_SIZE
        )
    }








    fun obfuscate(plaintext: String, key: ByteArray): String {
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key.copyOf(KEY_SIZE), "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))


        val result = iv + encrypted
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }







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




    fun xorObfuscate(plaintext: String, key: ByteArray): ByteArray {
        val bytes = plaintext.toByteArray(Charsets.UTF_8)
        val result = ByteArray(bytes.size)

        for (i in bytes.indices) {
            result[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        }

        return result
    }




    fun xorDeobfuscate(obfuscated: ByteArray, key: ByteArray): String {
        val result = ByteArray(obfuscated.size)

        for (i in obfuscated.indices) {
            result[i] = (obfuscated[i].toInt() xor key[i % key.size].toInt()).toByte()
        }

        return String(result, Charsets.UTF_8)
    }





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





class ObfuscatedString private constructor(
    private val obfuscatedData: String
) {
    @Volatile
    private var cachedValue: String? = null

    companion object {



        fun of(obfuscated: String): ObfuscatedString {
            return ObfuscatedString(obfuscated)
        }
    }




    fun get(): String {
        return cachedValue ?: StringObfuscator.deobfuscate(obfuscatedData).also {
            cachedValue = it
        }
    }




    fun clear() {
        cachedValue = null
    }

    override fun toString(): String = get()
}
