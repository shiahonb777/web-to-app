package com.webtoapp.core.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EncryptionConfigExtendedTest {

    // ═══════════════════════════════════════════
    // EncryptionLevel 枚举完整性
    // ═══════════════════════════════════════════

    @Test
    fun `EncryptionLevel has all expected values`() {
        assertThat(EncryptionLevel.values()).asList().containsExactly(
            EncryptionLevel.FAST,
            EncryptionLevel.STANDARD,
            EncryptionLevel.HIGH,
            EncryptionLevel.PARANOID
        )
    }

    @Test
    fun `EncryptionLevel iterations are monotonically increasing`() {
        val levels = EncryptionLevel.values()
        for (i in 1 until levels.size) {
            assertThat(levels[i].iterations).isGreaterThan(levels[i - 1].iterations)
        }
    }

    @Test
    fun `STANDARD level has correct iterations`() {
        assertThat(EncryptionLevel.STANDARD.iterations).isGreaterThan(EncryptionLevel.FAST.iterations)
        assertThat(EncryptionLevel.STANDARD.iterations).isLessThan(EncryptionLevel.HIGH.iterations)
    }

    @Test
    fun `HIGH level has correct iterations`() {
        assertThat(EncryptionLevel.HIGH.iterations).isGreaterThan(EncryptionLevel.STANDARD.iterations)
        assertThat(EncryptionLevel.HIGH.iterations).isLessThan(EncryptionLevel.PARANOID.iterations)
    }

    // ═══════════════════════════════════════════
    // shouldEncrypt — 更多边界情况
    // ═══════════════════════════════════════════

    @Test
    fun `shouldEncrypt returns false for all categories when all disabled`() {
        val config = EncryptionConfig(
            enabled = true,
            encryptConfig = false,
            encryptHtml = false,
            encryptMedia = false,
            encryptSplash = false,
            encryptBgm = false
        )

        assertThat(config.shouldEncrypt("app_config.json")).isFalse()
        assertThat(config.shouldEncrypt("html/index.html")).isFalse()
        assertThat(config.shouldEncrypt("image.png")).isFalse()
        assertThat(config.shouldEncrypt("splash.mp4")).isFalse()
        assertThat(config.shouldEncrypt("bgm.mp3")).isFalse()
    }

    @Test
    fun `shouldEncrypt handles encryptBgm correctly`() {
        val config = EncryptionConfig(
            enabled = true,
            encryptBgm = true,
            encryptMedia = false
        )

        assertThat(config.shouldEncrypt("bgm/theme.mp3")).isTrue()
        assertThat(config.shouldEncrypt("media/video.mp4")).isFalse()
    }

    // ═══════════════════════════════════════════
    // hasSecurityProtection — 逐项测试
    // ═══════════════════════════════════════════

    @Test
    fun `hasSecurityProtection returns true for enableIntegrityCheck`() {
        val config = EncryptionConfig(enabled = true, enableIntegrityCheck = true)
        assertThat(config.hasSecurityProtection()).isTrue()
    }

    @Test
    fun `hasSecurityProtection returns true for enableAntiTamper`() {
        val config = EncryptionConfig(enabled = true, enableAntiTamper = true)
        assertThat(config.hasSecurityProtection()).isTrue()
    }

    @Test
    fun `hasSecurityProtection returns true for enableRootDetection`() {
        val config = EncryptionConfig(enabled = true, enableRootDetection = true)
        assertThat(config.hasSecurityProtection()).isTrue()
    }

    @Test
    fun `hasSecurityProtection returns true for enableEmulatorDetection`() {
        val config = EncryptionConfig(enabled = true, enableEmulatorDetection = true)
        assertThat(config.hasSecurityProtection()).isTrue()
    }

    @Test
    fun `hasSecurityProtection returns true for enableRuntimeProtection`() {
        val config = EncryptionConfig(enabled = true, enableRuntimeProtection = true)
        assertThat(config.hasSecurityProtection()).isTrue()
    }
}
