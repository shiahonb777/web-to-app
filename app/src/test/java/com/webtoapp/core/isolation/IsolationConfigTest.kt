package com.webtoapp.core.isolation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IsolationConfigTest {

    @Test
    fun `toJson and fromJson preserve isolation config`() {
        val original = IsolationConfig(
            enabled = true,
            fingerprintConfig = FingerprintConfig(
                randomize = true,
                customUserAgent = "UA",
                hardwareConcurrency = 8
            ),
            headerConfig = HeaderConfig(
                enabled = true,
                customHeaders = mapOf("X-Test" to "1"),
                refererPolicy = RefererPolicy.SAME_ORIGIN
            ),
            ipSpoofConfig = IpSpoofConfig(
                enabled = true,
                randomIpRange = IpRange.GLOBAL
            ),
            spoofTimezone = true,
            customTimezone = "Asia/Tokyo",
            spoofLanguage = true,
            customLanguage = "ja-JP",
            spoofScreen = true,
            customScreenWidth = 1920,
            customScreenHeight = 1080
        )

        val restored = IsolationConfig.fromJson(original.toJson())

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `fromJson returns null for malformed input`() {
        val parsed = IsolationConfig.fromJson("{invalid-json")

        assertThat(parsed).isNull()
    }

    @Test
    fun `predefined presets expose expected security levels`() {
        assertThat(IsolationConfig.DISABLED.enabled).isFalse()

        assertThat(IsolationConfig.BASIC.enabled).isTrue()
        assertThat(IsolationConfig.BASIC.storageIsolation).isTrue()
        assertThat(IsolationConfig.BASIC.protectCanvas).isTrue()
        assertThat(IsolationConfig.BASIC.protectAudio).isFalse()

        assertThat(IsolationConfig.STANDARD.enabled).isTrue()
        assertThat(IsolationConfig.STANDARD.headerConfig.enabled).isTrue()
        assertThat(IsolationConfig.STANDARD.protectAudio).isTrue()

        assertThat(IsolationConfig.MAXIMUM.enabled).isTrue()
        assertThat(IsolationConfig.MAXIMUM.ipSpoofConfig.enabled).isTrue()
        assertThat(IsolationConfig.MAXIMUM.spoofTimezone).isTrue()
        assertThat(IsolationConfig.MAXIMUM.spoofLanguage).isTrue()
        assertThat(IsolationConfig.MAXIMUM.spoofScreen).isTrue()
    }
}
