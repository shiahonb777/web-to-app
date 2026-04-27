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

        assertThat(script).contains("Navigator property spoofing")
        assertThat(script).contains("Canvas fingerprint")
        assertThat(script).contains("WebGL")
        assertThat(script).contains("AudioContext")
        assertThat(script).contains("WebRTC")
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

        assertThat(script).contains("Font fingerprint protection")
        assertThat(script).contains("Screen/window dimension spoofing")
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

        assertThat(script).doesNotContain("Navigator property spoofing")
        assertThat(script).doesNotContain("Canvas fingerprint")
        assertThat(script).doesNotContain("WebGL")
        assertThat(script).doesNotContain("AudioContext")
        assertThat(script).doesNotContain("WebRTC")
    }
}
