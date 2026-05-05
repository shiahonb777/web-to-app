package com.webtoapp.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebAppModelExtendedTest {

    @Test
    fun `WebApp default has WEB app type`() {
        val app = WebApp(name = "", url = "")
        assertThat(app.appType).isEqualTo(AppType.WEB)
    }

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

    @Test
    fun `ApkEncryptionConfig DISABLED preset is not enabled`() {
        assertThat(ApkEncryptionConfig.DISABLED.enabled).isFalse()
    }

    @Test
    fun `ApkEncryptionConfig enabled maps to maximum crypto protection`() {
        val mapped = ApkEncryptionConfig(enabled = true).toEncryptionConfig()

        assertThat(mapped.enabled).isTrue()
        assertThat(mapped.shouldEncrypt("app_config.json")).isTrue()
        assertThat(mapped.shouldEncrypt("html/index.html")).isTrue()
        assertThat(mapped.getKeyDerivationIterations()).isEqualTo(100000)
    }

    @Test
    fun `ApkEncryptionConfig disabled maps to disabled crypto config`() {
        val mapped = ApkEncryptionConfig.DISABLED.toEncryptionConfig()

        assertThat(mapped.enabled).isFalse()
        assertThat(mapped.shouldEncrypt("app_config.json")).isFalse()
    }
}
