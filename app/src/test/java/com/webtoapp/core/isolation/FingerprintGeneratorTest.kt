package com.webtoapp.core.isolation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FingerprintGeneratorTest {

    @Test
    fun `generateFingerprint is deterministic for same seed`() {
        val first = FingerprintGenerator.generateFingerprint(seed = "demo-seed")
        val second = FingerprintGenerator.generateFingerprint(seed = "demo-seed")

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `generateFingerprint values stay in expected ranges`() {
        val fp = FingerprintGenerator.generateFingerprint(seed = "range-seed")

        assertThat(fp.screenWidth).isAtLeast(1000)
        assertThat(fp.screenHeight).isAtLeast(700)
        assertThat(listOf(24, 30, 32)).contains(fp.colorDepth)
        assertThat(listOf(4, 6, 8, 10, 12, 16, 20, 24)).contains(fp.hardwareConcurrency)
        assertThat(listOf(4, 8, 16, 32)).contains(fp.deviceMemory)
        assertThat(fp.canvasNoise).isAtLeast(0f)
        assertThat(fp.canvasNoise).isLessThan(0.0001f)
        assertThat(fp.audioNoise).isAtLeast(0f)
        assertThat(fp.audioNoise).isLessThan(0.0001f)
    }

    @Test
    fun `generateIpByCountry and random search ip produce valid ipv4`() {
        val japanIp = FingerprintGenerator.generateIpByCountry("japan")
        val fallbackIp = FingerprintGenerator.generateRandomIp(IpRange.SEARCH, null)
        val ipv4Pattern = Regex("""\d{1,3}(\.\d{1,3}){3}""")

        assertThat(ipv4Pattern.matches(japanIp)).isTrue()
        assertThat(ipv4Pattern.matches(fallbackIp)).isTrue()
    }

    @Test
    fun `supported countries include key entries`() {
        val countries = FingerprintGenerator.getSupportedCountries()

        assertThat(countries).contains("中国")
        assertThat(countries).contains("美国")
        assertThat(countries).contains("欧洲")
        assertThat(countries).contains("亚洲")
    }
}
