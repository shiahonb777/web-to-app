package com.webtoapp.core.engine.shields

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HttpsUpgraderTest {

    @Test
    fun `tryUpgrade upgrades remote http urls`() {
        val upgrader = HttpsUpgrader()

        val upgraded = upgrader.tryUpgrade("http://example.com/path?q=1")

        assertThat(upgraded).isEqualTo("https://example.com/path?q=1")
    }

    @Test
    fun `tryUpgrade skips localhost private and ip hosts`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://localhost:8080")).isNull()
        assertThat(upgrader.tryUpgrade("http://10.0.2.2:8080")).isNull()
        assertThat(upgrader.tryUpgrade("http://192.168.1.10")).isNull()
        assertThat(upgrader.tryUpgrade("http://8.8.8.8")).isNull()
        assertThat(upgrader.tryUpgrade("https://example.com")).isNull()
    }

    @Test
    fun `onSslError marks domain as failed and prevents future upgrades`() {
        val upgrader = HttpsUpgrader()
        val upgraded = upgrader.tryUpgrade("http://example.com/login")

        val fallback = upgrader.onSslError(upgraded)

        assertThat(fallback).isEqualTo("http://example.com/login")
        assertThat(upgrader.getFailedDomainCount()).isEqualTo(1)
        assertThat(upgrader.tryUpgrade("http://example.com/again")).isNull()
    }

    @Test
    fun `clearFailedDomains allows upgrade again after ssl fallback`() {
        val upgrader = HttpsUpgrader()
        val upgraded = upgrader.tryUpgrade("http://example.com")
        upgrader.onSslError(upgraded)

        upgrader.clearFailedDomains()

        assertThat(upgrader.getFailedDomainCount()).isEqualTo(0)
        assertThat(upgrader.tryUpgrade("http://example.com")).isEqualTo("https://example.com")
    }

    @Test
    fun `onPageStarted clears pending upgrades`() {
        val upgrader = HttpsUpgrader()
        val upgraded = upgrader.tryUpgrade("http://pending.example.com")

        upgrader.onPageStarted()

        assertThat(upgrader.onSslError(upgraded)).isNull()
        assertThat(upgrader.getFailedDomainCount()).isEqualTo(0)
    }
}
