package com.webtoapp.core.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for BatchImportService parsing logic.
 * Replicates the pure parsing algorithm since the service requires Context/Repository.
 */
class BatchImportServiceParseTest {

    private fun parseFromText(text: String): List<TestParsedEntry> {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }
            .mapNotNull { line ->
                when {
                    line.contains("|") -> {
                        val parts = line.split("|", limit = 2)
                        val name = parts[0].trim()
                        val url = parts[1].trim()
                        if (url.startsWith("http://") || url.startsWith("https://"))
                            TestParsedEntry(name.ifBlank { extractName(url) }, url)
                        else null
                    }
                    line.startsWith("http://") || line.startsWith("https://") -> {
                        TestParsedEntry(extractName(line), line)
                    }
                    line.contains(" ") -> {
                        val lastSpace = line.lastIndexOf(" ")
                        val possibleUrl = line.substring(lastSpace + 1).trim()
                        if (possibleUrl.startsWith("http://") || possibleUrl.startsWith("https://")) {
                            val name = line.substring(0, lastSpace).trim()
                            TestParsedEntry(name.ifBlank { extractName(possibleUrl) }, possibleUrl)
                        } else null
                    }
                    else -> null
                }
            }
            .distinctBy { it.url }
    }

    private fun extractName(url: String): String {
        return try {
            val host = java.net.URL(url).host
            host.removePrefix("www.")
                .substringBeforeLast(".")
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            url.take(30)
        }
    }

    data class TestParsedEntry(val name: String, val url: String)

    // ═══════════════════════════════════════════
    // Basic parsing
    // ═══════════════════════════════════════════

    @Test
    fun `parseFromText parses single URL`() {
        val result = parseFromText("https://example.com")
        assertThat(result).hasSize(1)
        assertThat(result[0].url).isEqualTo("https://example.com")
        assertThat(result[0].name).isEqualTo("Example")
    }

    @Test
    fun `parseFromText parses pipe-separated name and URL`() {
        val result = parseFromText("My App|https://example.com")
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("My App")
        assertThat(result[0].url).isEqualTo("https://example.com")
    }

    @Test
    fun `parseFromText parses space-separated name and URL`() {
        val result = parseFromText("My App https://example.com")
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("My App")
        assertThat(result[0].url).isEqualTo("https://example.com")
    }

    @Test
    fun `parseFromText parses multiple URLs`() {
        val text = """
            https://example.com
            https://google.com
            https://github.com
        """.trimIndent()
        val result = parseFromText(text)
        assertThat(result).hasSize(3)
    }

    // ═══════════════════════════════════════════
    // Comment and blank line handling
    // ═══════════════════════════════════════════

    @Test
    fun `parseFromText skips comment lines starting with hash`() {
        val text = """
            # This is a comment
            https://example.com
        """.trimIndent()
        val result = parseFromText(text)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `parseFromText skips comment lines starting with double slash`() {
        val text = """
            // This is a comment
            https://example.com
        """.trimIndent()
        val result = parseFromText(text)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `parseFromText skips blank lines`() {
        val text = "\nhttps://example.com\n\nhttps://google.com\n"
        val result = parseFromText(text)
        assertThat(result).hasSize(2)
    }

    // ═══════════════════════════════════════════
    // Deduplication
    // ═══════════════════════════════════════════

    @Test
    fun `parseFromText deduplicates URLs`() {
        val text = """
            https://example.com
            https://example.com
        """.trimIndent()
        val result = parseFromText(text)
        assertThat(result).hasSize(1)
    }

    // ═══════════════════════════════════════════
    // Invalid input
    // ═══════════════════════════════════════════

    @Test
    fun `parseFromText returns empty for non-URL text`() {
        val result = parseFromText("just some random text")
        assertThat(result).isEmpty()
    }

    @Test
    fun `parseFromText returns empty for empty string`() {
        val result = parseFromText("")
        assertThat(result).isEmpty()
    }

    @Test
    fun `parseFromText ignores pipe format with invalid URL`() {
        val result = parseFromText("My App|not-a-url")
        assertThat(result).isEmpty()
    }

    // ═══════════════════════════════════════════
    // Name extraction
    // ═══════════════════════════════════════════

    @Test
    fun `extractName removes www prefix and TLD`() {
        assertThat(extractName("https://www.example.com")).isEqualTo("Example")
    }

    @Test
    fun `extractName handles bare domain`() {
        assertThat(extractName("https://example.com")).isEqualTo("Example")
    }

    @Test
    fun `extractName capitalizes first letter`() {
        assertThat(extractName("https://github.com")).isEqualTo("Github")
    }
}
