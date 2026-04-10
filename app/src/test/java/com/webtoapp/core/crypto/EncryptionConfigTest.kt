package com.webtoapp.core.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EncryptionConfigTest {

    @Test
    fun `shouldEncrypt returns false when encryption disabled`() {
        val config = EncryptionConfig(enabled = false)

        assertThat(config.shouldEncrypt("app_config.json")).isFalse()
        assertThat(config.shouldEncrypt("html/index.html")).isFalse()
        assertThat(config.shouldEncrypt("media_content.mp4")).isFalse()
    }

    @Test
    fun `shouldEncrypt follows configured categories when enabled`() {
        val config = EncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = false,
            encryptSplash = true,
            encryptBgm = false
        )

        assertThat(config.shouldEncrypt(CryptoConstants.CONFIG_FILE)).isTrue()
        assertThat(config.shouldEncrypt("html/index.html")).isTrue()
        assertThat(config.shouldEncrypt("styles/app.css")).isTrue()
        assertThat(config.shouldEncrypt("scripts/app.js")).isTrue()
        assertThat(config.shouldEncrypt("splash_media.mp4")).isTrue()
        assertThat(config.shouldEncrypt("bgm/theme.mp3")).isFalse()
        assertThat(config.shouldEncrypt("media_content.mp4")).isFalse()
    }

    @Test
    fun `media detection works for standalone media extensions`() {
        val config = EncryptionConfig(
            enabled = true,
            encryptConfig = false,
            encryptHtml = false,
            encryptMedia = true,
            encryptSplash = false,
            encryptBgm = false
        )

        assertThat(config.shouldEncrypt("cover.png")).isTrue()
        assertThat(config.shouldEncrypt("video.webm")).isFalse()
        assertThat(config.shouldEncrypt("audio.mp3")).isTrue()
    }

    @Test
    fun `key derivation iterations follow selected level`() {
        val fast = EncryptionConfig(enabled = true, encryptionLevel = EncryptionLevel.FAST)
        val paranoid = EncryptionConfig(enabled = true, encryptionLevel = EncryptionLevel.PARANOID)

        assertThat(fast.getKeyDerivationIterations()).isEqualTo(EncryptionLevel.FAST.iterations)
        assertThat(paranoid.getKeyDerivationIterations()).isEqualTo(EncryptionLevel.PARANOID.iterations)
    }

    @Test
    fun `hasSecurityProtection checks all security toggles`() {
        val none = EncryptionConfig(
            enabled = true,
            enableIntegrityCheck = false,
            enableAntiDebug = false,
            enableAntiTamper = false,
            enableRootDetection = false,
            enableEmulatorDetection = false,
            enableRuntimeProtection = false
        )
        val oneEnabled = none.copy(enableAntiDebug = true)

        assertThat(none.hasSecurityProtection()).isFalse()
        assertThat(oneEnabled.hasSecurityProtection()).isTrue()
    }
}

