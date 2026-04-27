package com.webtoapp.core.engine.shields

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HttpsUpgraderExtendedTest {

    @Rule @JvmField
    val koinRule = com.webtoapp.util.KoinCleanupRule()

    // ═══════════════════════════════════════════
    // tryHttpFallback
    // ═══════════════════════════════════════════

    @Test
    fun `tryHttpFallback returns http URL for https URL`() {
        val upgrader = HttpsUpgrader()
        val fallback = upgrader.tryHttpFallback("https://example.com/page")

        assertThat(fallback).isEqualTo("http://example.com/page")
    }

    @Test
    fun `tryHttpFallback returns null for non-https URL`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryHttpFallback("http://example.com")).isNull()
        assertThat(upgrader.tryHttpFallback(null)).isNull()
    }

    @Test
    fun `tryHttpFallback returns null on second attempt for same domain`() {
        val upgrader = HttpsUpgrader()
        val first = upgrader.tryHttpFallback("https://example.com/a")
        val second = upgrader.tryHttpFallback("https://example.com/b")

        assertThat(first).isEqualTo("http://example.com/a")
        assertThat(second).isNull()
    }

    @Test
    fun `tryHttpFallback allows different domains independently`() {
        val upgrader = HttpsUpgrader()
        val a = upgrader.tryHttpFallback("https://a.com/page")
        val b = upgrader.tryHttpFallback("https://b.com/page")

        assertThat(a).isEqualTo("http://a.com/page")
        assertThat(b).isEqualTo("http://b.com/page")
    }

    // ═══════════════════════════════════════════
    // shouldSkip — .local / .localhost 域名
    // ═══════════════════════════════════════════

    @Test
    fun `tryUpgrade skips dot-local domains`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://myserver.local")).isNull()
    }

    @Test
    fun `tryUpgrade skips dot-localhost domains`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://dev.localhost")).isNull()
    }

    // ═══════════════════════════════════════════
    // shouldSkip — 127.0.0.1 / 0.0.0.0 / ::1
    // ═══════════════════════════════════════════

    @Test
    fun `tryUpgrade skips 127-0-0-1`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://127.0.0.1:8080")).isNull()
    }

    @Test
    fun `tryUpgrade skips 0-0-0-0`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://0.0.0.0:3000")).isNull()
    }

    // ═══════════════════════════════════════════
    // shouldSkip — 172.16-31.x 内网 IP
    // ═══════════════════════════════════════════

    @Test
    fun `tryUpgrade skips 172-16-x private IP`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://172.16.0.1")).isNull()
    }

    @Test
    fun `tryUpgrade skips 172-31-x private IP`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://172.31.255.255")).isNull()
    }

    @Test
    fun `tryUpgrade skips all IP addresses including public ones`() {
        // IP_REGEX matches any IPv4 address, so all IPs are skipped
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.tryUpgrade("http://172.32.0.1")).isNull()
        assertThat(upgrader.tryUpgrade("http://8.8.8.8")).isNull()
    }

    // ═══════════════════════════════════════════
    // onSslError — null input
    // ═══════════════════════════════════════════

    @Test
    fun `onSslError returns null for null input`() {
        val upgrader = HttpsUpgrader()

        assertThat(upgrader.onSslError(null)).isNull()
    }

    @Test
    fun `onSslError returns null for non-pending domain`() {
        val upgrader = HttpsUpgrader()

        // No tryUpgrade was called, so nothing is pending
        assertThat(upgrader.onSslError("https://example.com")).isNull()
    }
}
