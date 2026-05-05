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
    fun `shouldEncrypt returns true for every asset when encryption enabled`() {
        val config = EncryptionConfig(enabled = true)

        assertThat(config.shouldEncrypt(CryptoConstants.CONFIG_FILE)).isTrue()
        assertThat(config.shouldEncrypt("html/index.html")).isTrue()
        assertThat(config.shouldEncrypt("styles/app.css")).isTrue()
        assertThat(config.shouldEncrypt("scripts/app.js")).isTrue()
        assertThat(config.shouldEncrypt("splash_media.mp4")).isTrue()
        assertThat(config.shouldEncrypt("bgm/theme.mp3")).isTrue()
        assertThat(config.shouldEncrypt("media_content.mp4")).isTrue()
    }

    @Test
    fun `shouldEncrypt does not special case media extensions`() {
        val config = EncryptionConfig(enabled = true)

        assertThat(config.shouldEncrypt("cover.png")).isTrue()
        assertThat(config.shouldEncrypt("video.webm")).isTrue()
        assertThat(config.shouldEncrypt("audio.mp3")).isTrue()
    }

    @Test
    fun `key derivation uses maximum fixed iterations`() {
        val config = EncryptionConfig(enabled = true)

        assertThat(config.getKeyDerivationIterations()).isEqualTo(100000)
    }

    @Test
    fun `hasSecurityProtection follows enabled state`() {
        assertThat(EncryptionConfig(enabled = false).hasSecurityProtection()).isFalse()
        assertThat(EncryptionConfig(enabled = true).hasSecurityProtection()).isTrue()
    }
}
