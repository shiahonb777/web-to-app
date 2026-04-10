package com.webtoapp.core.crypto

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertThrows
import org.junit.Test

class EnhancedCryptoTest {

    private val secretKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    @Test
    fun `hkdf derive is deterministic with same input`() {
        val input = "ikm".toByteArray()
        val salt = "salt".toByteArray()
        val info = "info".toByteArray()

        val first = EnhancedCrypto.HKDF.derive(input, salt, info, 32)
        val second = EnhancedCrypto.HKDF.derive(input, salt, info, 32)

        assertThat(first.contentEquals(second)).isTrue()
        assertThat(first.size).isEqualTo(32)
    }

    @Test
    fun `hkdf derive changes when info changes`() {
        val ikm = "ikm".toByteArray()
        val salt = "salt".toByteArray()

        val keyA = EnhancedCrypto.HKDF.derive(ikm, salt, "A".toByteArray(), 32)
        val keyB = EnhancedCrypto.HKDF.derive(ikm, salt, "B".toByteArray(), 32)

        assertThat(keyA.contentEquals(keyB)).isFalse()
    }

    @Test
    fun `encrypt and decrypt roundtrip with aad`() {
        val plaintext = "Hello WebToApp".toByteArray()
        val aad = "metadata".toByteArray()

        val encrypted = EnhancedCrypto.encrypt(plaintext, secretKey, aad)
        val decrypted = EnhancedCrypto.decrypt(encrypted, secretKey, aad)

        assertThat(decrypted.contentEquals(plaintext)).isTrue()
    }

    @Test
    fun `decrypt fails when aad mismatches`() {
        val plaintext = "secret".toByteArray()
        val encrypted = EnhancedCrypto.encrypt(plaintext, secretKey, "aad-A".toByteArray())

        assertThrows(AEADBadTagException::class.java) {
            EnhancedCrypto.decrypt(encrypted, secretKey, "aad-B".toByteArray())
        }
    }

    @Test
    fun `encryptStream and decryptStream support large payload`() {
        val source = ByteArray(200_000) { ((it * 31) % 251).toByte() }
        val aad = "stream-aad".toByteArray()

        val encryptedOut = ByteArrayOutputStream()
        EnhancedCrypto.encryptStream(
            input = ByteArrayInputStream(source),
            output = encryptedOut,
            key = secretKey,
            associatedData = aad
        )

        val decryptedOut = ByteArrayOutputStream()
        EnhancedCrypto.decryptStream(
            input = ByteArrayInputStream(encryptedOut.toByteArray()),
            output = decryptedOut,
            key = secretKey,
            associatedData = aad
        )

        assertThat(decryptedOut.toByteArray().contentEquals(source)).isTrue()
    }

    @Test
    fun `secure key container cannot be used after close`() {
        val container = EnhancedCrypto.SecureKeyContainer(ByteArray(32) { 7 })
        assertThat(container.getBytes().size).isEqualTo(32)

        container.close()

        assertThrows(IllegalStateException::class.java) {
            container.getBytes()
        }
    }
}

