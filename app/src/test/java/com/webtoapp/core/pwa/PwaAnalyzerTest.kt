package com.webtoapp.core.pwa

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PwaAnalyzerTest {

    // ═══════════════════════════════════════════
    // extractHost
    // ═══════════════════════════════════════════

    @Test
    fun `extractHost returns host from https URL`() {
        assertThat(PwaAnalyzer.extractHost("https://www.example.com/path")).isEqualTo("www.example.com")
    }

    @Test
    fun `extractHost returns host from http URL`() {
        assertThat(PwaAnalyzer.extractHost("http://example.com")).isEqualTo("example.com")
    }

    @Test
    fun `extractHost returns host from URL with port`() {
        assertThat(PwaAnalyzer.extractHost("https://localhost:8080")).isEqualTo("localhost")
    }

    @Test
    fun `extractHost returns host for bare domain via auto-prepend`() {
        // extractHost auto-prepends https://, so bare domains become valid
        assertThat(PwaAnalyzer.extractHost("not-a-url")).isEqualTo("not-a-url")
    }

    @Test
    fun `extractHost auto-prepends https for bare domain`() {
        assertThat(PwaAnalyzer.extractHost("example.com")).isEqualTo("example.com")
    }

    // ═══════════════════════════════════════════
    // suggestDeepLinkHosts
    // ═══════════════════════════════════════════

    @Test
    fun `suggestDeepLinkHosts includes original URL host`() {
        val result = PwaAnalysisResult(isPwa = true, source = PwaDataSource.MANIFEST)
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://www.example.com")
        assertThat(hosts).contains("www.example.com")
    }

    @Test
    fun `suggestDeepLinkHosts includes scope host`() {
        val result = PwaAnalysisResult(
            isPwa = true,
            scope = "https://app.example.com/",
            source = PwaDataSource.MANIFEST
        )
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://www.example.com")
        assertThat(hosts).contains("app.example.com")
    }

    @Test
    fun `suggestDeepLinkHosts includes startUrl host`() {
        val result = PwaAnalysisResult(
            isPwa = true,
            startUrl = "https://m.example.com/home",
            source = PwaDataSource.MANIFEST
        )
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://www.example.com")
        assertThat(hosts).contains("m.example.com")
    }

    @Test
    fun `suggestDeepLinkHosts adds www variant for non-www host`() {
        val result = PwaAnalysisResult(isPwa = true, source = PwaDataSource.MANIFEST)
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://example.com")
        assertThat(hosts).contains("example.com")
        assertThat(hosts).contains("www.example.com")
    }

    @Test
    fun `suggestDeepLinkHosts adds non-www variant for www host`() {
        val result = PwaAnalysisResult(isPwa = true, source = PwaDataSource.MANIFEST)
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://www.example.com")
        assertThat(hosts).contains("www.example.com")
        assertThat(hosts).contains("example.com")
    }

    @Test
    fun `suggestDeepLinkHosts returns sorted list`() {
        val result = PwaAnalysisResult(isPwa = true, source = PwaDataSource.MANIFEST)
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://www.example.com")
        val sorted = hosts.sorted()
        assertThat(hosts).isEqualTo(sorted)
    }

    @Test
    fun `suggestDeepLinkHosts deduplicates hosts`() {
        val result = PwaAnalysisResult(
            isPwa = true,
            scope = "https://www.example.com/",
            startUrl = "https://www.example.com/app",
            source = PwaDataSource.MANIFEST
        )
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://www.example.com")
        // www.example.com should appear only once (plus example.com variant)
        val wwwCount = hosts.count { it == "www.example.com" }
        assertThat(wwwCount).isEqualTo(1)
    }

    @Test
    fun `suggestDeepLinkHosts handles empty result with no scope or startUrl`() {
        val result = PwaAnalysisResult()
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, "https://example.com")
        assertThat(hosts).isNotEmpty()
        assertThat(hosts).contains("example.com")
    }
}
