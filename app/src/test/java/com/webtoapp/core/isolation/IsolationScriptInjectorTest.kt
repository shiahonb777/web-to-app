package com.webtoapp.core.isolation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IsolationScriptInjectorTest {

    private val fingerprint = FingerprintGenerator.generateFingerprint(seed = "injector-seed")

    @Test
    fun `returns empty script when isolation disabled`() {
        val script = IsolationScriptInjector.generateIsolationScript(
            config = IsolationConfig(enabled = false),
            fingerprint = fingerprint
        )

        assertThat(script).isEmpty()
    }

    @Test
    fun `default enabled config contains major protection blocks`() {
        val script = IsolationScriptInjector.generateIsolationScript(
            config = IsolationConfig(enabled = true),
            fingerprint = fingerprint
        )

        assertThat(script).contains("Navigator 伪造")
        assertThat(script).contains("Canvas 指纹防护")
        assertThat(script).contains("WebGL 指纹防护")
        assertThat(script).contains("AudioContext 指纹防护")
        assertThat(script).contains("WebRTC 防泄漏")
    }

    @Test
    fun `script respects optional spoof screen and timezone values`() {
        val config = IsolationConfig(
            enabled = true,
            protectCanvas = false,
            protectWebGL = false,
            protectAudio = false,
            blockWebRTC = false,
            protectFonts = true,
            spoofScreen = true,
            customScreenWidth = 1111,
            customScreenHeight = 777,
            spoofTimezone = true,
            customTimezone = "Europe/Paris"
        )

        val script = IsolationScriptInjector.generateIsolationScript(config, fingerprint)

        assertThat(script).contains("字体指纹防护")
        assertThat(script).contains("屏幕分辨率伪装")
        assertThat(script).contains("return 1111")
        assertThat(script).contains("return 777")
        assertThat(script).contains("Europe/Paris")
    }

    @Test
    fun `script omits navigator block when fingerprint randomization is disabled`() {
        val config = IsolationConfig(
            enabled = true,
            fingerprintConfig = FingerprintConfig(randomize = false),
            protectCanvas = false,
            protectWebGL = false,
            protectAudio = false,
            blockWebRTC = false,
            protectFonts = false,
            spoofScreen = false,
            spoofTimezone = false
        )

        val script = IsolationScriptInjector.generateIsolationScript(config, fingerprint)

        assertThat(script).doesNotContain("Navigator 伪造")
        assertThat(script).doesNotContain("Canvas 指纹防护")
        assertThat(script).doesNotContain("WebGL 指纹防护")
        assertThat(script).doesNotContain("AudioContext 指纹防护")
        assertThat(script).doesNotContain("WebRTC 防泄漏")
    }
}
