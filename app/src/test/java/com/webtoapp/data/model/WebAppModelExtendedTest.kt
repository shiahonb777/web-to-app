package com.webtoapp.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebAppModelExtendedTest {

    // ═══════════════════════════════════════════
    // WebApp 默认值
    // ═══════════════════════════════════════════

    @Test
    fun `WebApp default has WEB app type`() {
        val app = WebApp(name = "", url = "")
        assertThat(app.appType).isEqualTo(AppType.WEB)
    }

    // ═══════════════════════════════════════════
    // AppType 枚举
    // ═══════════════════════════════════════════

    @Test
    fun `AppType has all expected values`() {
        assertThat(AppType.values()).asList().containsExactly(
            AppType.WEB,
            AppType.IMAGE,
            AppType.VIDEO,
            AppType.HTML,
            AppType.GALLERY,
            AppType.FRONTEND,
            AppType.WORDPRESS,
            AppType.NODEJS_APP,
            AppType.PHP_APP,
            AppType.PYTHON_APP,
            AppType.GO_APP,
            AppType.MULTI_WEB
        )
    }

    // ═══════════════════════════════════════════
    // ApkArchitecture
    // ═══════════════════════════════════════════

    @Test
    fun `ApkArchitecture has UNIVERSAL ARM64 ARM32`() {
        assertThat(ApkArchitecture.values()).asList().containsExactly(
            ApkArchitecture.UNIVERSAL,
            ApkArchitecture.ARM64,
            ApkArchitecture.ARM32
        )
    }

    @Test
    fun `ApkArchitecture UNIVERSAL has all ABIs`() {
        assertThat(ApkArchitecture.UNIVERSAL.abiFilters).hasSize(4)
    }

    @Test
    fun `ApkArchitecture ARM64 has arm64 and x86_64`() {
        assertThat(ApkArchitecture.ARM64.abiFilters).containsExactly("arm64-v8a", "x86_64")
    }

    @Test
    fun `ApkArchitecture ARM32 has armeabi and x86`() {
        assertThat(ApkArchitecture.ARM32.abiFilters).containsExactly("armeabi-v7a", "x86")
    }

    @Test
    fun `ApkArchitecture fromName falls back to UNIVERSAL for unknown`() {
        assertThat(ApkArchitecture.fromName("UNKNOWN")).isEqualTo(ApkArchitecture.UNIVERSAL)
    }

    @Test
    fun `ApkArchitecture fromName matches exact name`() {
        assertThat(ApkArchitecture.fromName("ARM64")).isEqualTo(ApkArchitecture.ARM64)
        assertThat(ApkArchitecture.fromName("ARM32")).isEqualTo(ApkArchitecture.ARM32)
    }

    // ═══════════════════════════════════════════
    // HtmlConfig
    // ═══════════════════════════════════════════

    @Test
    fun `HtmlConfig getValidEntryFile returns custom entry when valid`() {
        val config = HtmlConfig(entryFile = "main.html")
        assertThat(config.getValidEntryFile()).isEqualTo("main.html")
    }

    @Test
    fun `HtmlConfig getValidEntryFile returns index for blank entry`() {
        val config = HtmlConfig(entryFile = "")
        assertThat(config.getValidEntryFile()).isEqualTo("index.html")
    }

    // ═══════════════════════════════════════════
    // GalleryConfig — 空项目列表
    // ═══════════════════════════════════════════

    @Test
    fun `GalleryConfig with empty items has zero counts`() {
        val config = GalleryConfig(items = emptyList())
        assertThat(config.imageCount).isEqualTo(0)
        assertThat(config.videoCount).isEqualTo(0)
        assertThat(config.totalCount).isEqualTo(0)
    }

    @Test
    fun `GalleryConfig getItemsByCategory returns empty for non-existent category`() {
        val config = GalleryConfig(items = emptyList())
        assertThat(config.getItemsByCategory("nonexistent")).isEmpty()
    }

    // ═══════════════════════════════════════════
    // ActivationCode 边界
    // ═══════════════════════════════════════════

    @Test
    fun `getAllActivationCodes with empty lists returns empty`() {
        val app = WebApp(name = "Test", url = "https://test.com",
            activationCodeList = emptyList(), activationCodes = emptyList())
        assertThat(app.getAllActivationCodes()).isEmpty()
    }

    @Test
    fun `getActivationCodeStrings with empty list returns empty`() {
        val app = WebApp(name = "Test", url = "https://test.com",
            activationCodeList = emptyList())
        assertThat(app.getActivationCodeStrings()).isEmpty()
    }

    // ═══════════════════════════════════════════
    // ApkEncryptionConfig — EncryptionLevel iterations
    // ═══════════════════════════════════════════

    @Test
    fun `ApkEncryptionConfig EncryptionLevel iterations are correct`() {
        assertThat(ApkEncryptionConfig.EncryptionLevel.FAST.iterations).isEqualTo(5000)
        assertThat(ApkEncryptionConfig.EncryptionLevel.STANDARD.iterations).isEqualTo(10000)
        assertThat(ApkEncryptionConfig.EncryptionLevel.HIGH.iterations).isEqualTo(50000)
        assertThat(ApkEncryptionConfig.EncryptionLevel.PARANOID.iterations).isEqualTo(100000)
    }

    @Test
    fun `ApkEncryptionConfig DISABLED preset is not enabled`() {
        assertThat(ApkEncryptionConfig.DISABLED.enabled).isFalse()
    }

    @Test
    fun `ApkEncryptionConfig BASIC preset encrypts config and html`() {
        assertThat(ApkEncryptionConfig.BASIC.enabled).isTrue()
        assertThat(ApkEncryptionConfig.BASIC.encryptConfig).isTrue()
        assertThat(ApkEncryptionConfig.BASIC.encryptHtml).isTrue()
        assertThat(ApkEncryptionConfig.BASIC.encryptMedia).isFalse()
        assertThat(ApkEncryptionConfig.BASIC.enableIntegrityCheck).isTrue()
    }
}
