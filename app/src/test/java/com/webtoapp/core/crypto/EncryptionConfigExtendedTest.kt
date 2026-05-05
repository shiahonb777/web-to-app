package com.webtoapp.core.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EncryptionConfigExtendedTest {

    @Test
    fun `DISABLED preset turns off encryption`() {
        assertThat(EncryptionConfig.DISABLED.enabled).isFalse()
        assertThat(EncryptionConfig.DISABLED.customPassword).isNull()
    }

    @Test
    fun `MAXIMUM preset turns on encryption`() {
        assertThat(EncryptionConfig.MAXIMUM.enabled).isTrue()
        assertThat(EncryptionConfig.MAXIMUM.customPassword).isNull()
    }

    @Test
    fun `custom password is preserved on copy`() {
        val config = EncryptionConfig.MAXIMUM.copy(customPassword = "vault-key")

        assertThat(config.enabled).isTrue()
        assertThat(config.customPassword).isEqualTo("vault-key")
    }

    @Test
    fun `maximum protection uses fixed high iteration count`() {
        assertThat(EncryptionConfig.MAXIMUM.getKeyDerivationIterations()).isEqualTo(100000)
    }

    @Test
    fun `shouldEncrypt returns false for all categories when disabled`() {
        val config = EncryptionConfig.DISABLED

        assertThat(config.shouldEncrypt("app_config.json")).isFalse()
        assertThat(config.shouldEncrypt("html/index.html")).isFalse()
        assertThat(config.shouldEncrypt("image.png")).isFalse()
        assertThat(config.shouldEncrypt("splash.mp4")).isFalse()
        assertThat(config.shouldEncrypt("bgm.mp3")).isFalse()
    }

    @Test
    fun `shouldEncrypt returns true for bgm and media when enabled`() {
        val config = EncryptionConfig.MAXIMUM

        assertThat(config.shouldEncrypt("bgm/theme.mp3")).isTrue()
        assertThat(config.shouldEncrypt("media/video.mp4")).isTrue()
    }

    @Test
    fun `hasSecurityProtection returns true for maximum preset`() {
        assertThat(EncryptionConfig.MAXIMUM.hasSecurityProtection()).isTrue()
    }
}
