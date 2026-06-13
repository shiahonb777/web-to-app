package com.webtoapp.core.golang

import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.i18n.AppLanguage
import java.util.Locale
import org.junit.After
import org.junit.Test

class GoToolchainManagerUrlTest {

    private val expectedTunaPath =
        "/termux/apt/termux-main/pool/main/g/golang/golang_3%3A1.26.3_aarch64.deb"

    private val expectedOfficialPath =
        "/apt/termux-main/pool/main/g/golang/golang_3%3A1.26.3_aarch64.deb"
    private val originalLocale: Locale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `prefer china mirror true yields CN mirrors first then official fallback`() {
        val urls = GoToolchainManager.selectGoDebUrls(preferChinaMirror = true)

        assertThat(urls).isNotEmpty()
        assertThat(urls.first()).contains("tsinghua.edu.cn")
        val official = "https://packages.termux.dev" + expectedOfficialPath
        assertThat(urls).contains(official)
        assertThat(urls.indexOfFirst { it.contains("packages.termux.dev") })
            .isEqualTo(urls.size - 1)
    }

    @Test
    fun `prefer china mirror false yields official source only`() {
        val urls = GoToolchainManager.selectGoDebUrls(preferChinaMirror = false)

        assertThat(urls).hasSize(1)
        assertThat(urls.first()).contains("packages.termux.dev")
    }

    @Test
    fun `chinese app language is enough to prefer china mirror`() {
        Locale.setDefault(Locale.US)
        assertThat(GoToolchainManager.shouldPreferChinaMirror(AppLanguage.CHINESE)).isTrue()
    }

    @Test
    fun `system zh-CN locale is enough to prefer china mirror even with english app lang`() {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        assertThat(GoToolchainManager.shouldPreferChinaMirror(AppLanguage.ENGLISH)).isTrue()
    }

    @Test
    fun `english app and english system locale skips china mirror`() {
        Locale.setDefault(Locale.US)
        assertThat(GoToolchainManager.shouldPreferChinaMirror(AppLanguage.ENGLISH)).isFalse()
    }

    @Test
    fun `arabic app and arabic system locale skips china mirror`() {
        Locale.setDefault(Locale("ar"))
        assertThat(GoToolchainManager.shouldPreferChinaMirror(AppLanguage.ARABIC)).isFalse()
    }

    @Test
    fun `tuna mirror URL contains expected termux path`() {
        val urls = GoToolchainManager.selectGoDebUrls(preferChinaMirror = true)

        assertThat(urls).isNotEmpty()
        val tuna = urls.first { it.contains("tsinghua.edu.cn") }
        assertThat(tuna).endsWith(expectedTunaPath)
    }

    @Test
    fun `official source URL contains expected apt path`() {
        val urls = GoToolchainManager.selectGoDebUrls(preferChinaMirror = true)
        val official = urls.first { it.contains("packages.termux.dev") }
        assertThat(official).endsWith(expectedOfficialPath)
    }
}
