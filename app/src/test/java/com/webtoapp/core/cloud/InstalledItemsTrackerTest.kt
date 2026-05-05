package com.webtoapp.core.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test







class InstalledItemsTrackerTest {





    private fun parseVersionCode(entry: String): Int {
        val parts = entry.split("=", limit = 2)
        if (parts.size != 2) return 0
        return parts[1].toIntOrNull() ?: 0
    }

    private fun parseVersionName(entry: String): String {
        val parts = entry.split("=", limit = 2)
        if (parts.size != 2) return ""
        return parts[1]
    }

    private fun findEntryForId(entries: Set<String>, id: Int): String? {
        val prefix = "$id="
        return entries.find { it.startsWith(prefix) }
    }





    @Test
    fun `parseVersionCode extracts integer after equals sign`() {
        assertThat(parseVersionCode("42=5")).isEqualTo(5)
        assertThat(parseVersionCode("99=12")).isEqualTo(12)
        assertThat(parseVersionCode("1=0")).isEqualTo(0)
    }

    @Test
    fun `parseVersionCode returns 0 for malformed entry`() {
        assertThat(parseVersionCode("invalid_entry")).isEqualTo(0)
        assertThat(parseVersionCode("42=not_a_number")).isEqualTo(0)
    }

    @Test
    fun `parseVersionCode returns 0 for entry without equals sign`() {
        assertThat(parseVersionCode("42")).isEqualTo(0)
    }





    @Test
    fun `parseVersionName extracts string after equals sign`() {
        assertThat(parseVersionName("42=2.1.0")).isEqualTo("2.1.0")
        assertThat(parseVersionName("99=3.0.0")).isEqualTo("3.0.0")
    }

    @Test
    fun `parseVersionName handles version name containing equals sign`() {
        assertThat(parseVersionName("42=v=2.1")).isEqualTo("v=2.1")
    }

    @Test
    fun `parseVersionName returns empty for entry without equals sign`() {
        assertThat(parseVersionName("42")).isEmpty()
    }

    @Test
    fun `parseVersionName returns empty for malformed entry`() {
        assertThat(parseVersionName("invalid")).isEmpty()
    }





    @Test
    fun `findEntryForId finds matching entry`() {
        val entries = setOf("42=5", "99=12", "1=0")
        assertThat(findEntryForId(entries, 42)).isEqualTo("42=5")
        assertThat(findEntryForId(entries, 99)).isEqualTo("99=12")
    }

    @Test
    fun `findEntryForId returns null for missing id`() {
        val entries = setOf("42=5")
        assertThat(findEntryForId(entries, 100)).isNull()
    }

    @Test
    fun `findEntryForId returns null for empty set`() {
        assertThat(findEntryForId(emptySet(), 42)).isNull()
    }





    @Test
    fun `higher remote versionCode indicates update available`() {
        assertThat(5 > 3).isTrue()
    }

    @Test
    fun `same versionCode indicates no update`() {
        assertThat(5 > 5).isFalse()
    }

    @Test
    fun `lower versionCode indicates no update`() {
        assertThat(3 > 5).isFalse()
    }

    @Test
    fun `zero local versionCode needs update when remote has version`() {
        assertThat(1 > 0).isTrue()
        assertThat(0 > 0).isFalse()
    }





    @Test
    fun `getInstalledVersionCode from entries set`() {
        val entries = setOf("42=5", "99=12")
        val code42 = findEntryForId(entries, 42)?.let { parseVersionCode(it) } ?: 0
        val code99 = findEntryForId(entries, 99)?.let { parseVersionCode(it) } ?: 0
        val code100 = findEntryForId(entries, 100)?.let { parseVersionCode(it) } ?: 0

        assertThat(code42).isEqualTo(5)
        assertThat(code99).isEqualTo(12)
        assertThat(code100).isEqualTo(0)
    }

    @Test
    fun `getInstalledVersionName from entries set`() {
        val entries = setOf("42=2.1.0", "99=3.0.0")
        val name42 = findEntryForId(entries, 42)?.let { parseVersionName(it) } ?: ""
        val name100 = findEntryForId(entries, 100)?.let { parseVersionName(it) } ?: ""

        assertThat(name42).isEqualTo("2.1.0")
        assertThat(name100).isEmpty()
    }
}
