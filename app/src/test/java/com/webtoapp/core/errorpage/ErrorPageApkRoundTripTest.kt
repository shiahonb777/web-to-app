package com.webtoapp.core.errorpage

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.apkbuilder.ApkConfig
import com.webtoapp.core.apkbuilder.ApkConfigJsonFactory
import com.webtoapp.core.shell.ErrorPageShellConfig
import org.junit.Test


/**
 * Regression test for issue #76 (Custom Offline Page custom HTML lost in
 * generated APK).
 *
 * The Custom Offline Page feature stores the user-provided HTML in
 * [ErrorPageConfig.customHtml] in the WebToApp creator. Before this fix the
 * APK build pipeline silently dropped `customHtml`, `customMediaPath` and
 * `retryButtonText` in three places (the [ApkConfig] data class, the
 * `errorPageConfigPayload()` JSON serializer and the `ErrorPageShellConfig`
 * Gson wrapper used at runtime in the generated APK), so a generated APK
 * always loaded `customHtml = null` and fell back to the Chromium default
 * net::ERR_INTERNET_DISCONNECTED page.
 *
 * This test asserts that the values survive a full round-trip through the
 * pipeline, so any future omission of those fields will fail the test.
 */
class ErrorPageApkRoundTripTest {

    private val customHtml = """
        <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8">
        <title>Offline</title></head>
        <body><h1>You are offline</h1>
        <p>It's a "test" page with quotes & ampersands.</p>
        </body></html>
    """.trimIndent()

    @Test
    fun `customHtml survives full ApkConfig - JSON - shell pipeline`() {
        val apkConfig = newApkConfig().copy(
            errorPageMode = "CUSTOM_HTML",
            errorPageCustomHtml = customHtml,
            errorPageRetryButtonText = "Try Again"
        )

        val webViewBlock = extractWebViewConfig(apkConfig)
        val errorPageBlock = webViewBlock.getAsJsonObject("errorPageConfig")
        assertThat(errorPageBlock).isNotNull()
        assertThat(errorPageBlock.get("mode").asString).isEqualTo("CUSTOM_HTML")
        assertThat(errorPageBlock.get("customHtml").asString).isEqualTo(customHtml)
        assertThat(errorPageBlock.get("retryButtonText").asString).isEqualTo("Try Again")


        val shellConfig = Gson().fromJson(errorPageBlock, ErrorPageShellConfig::class.java)
        assertThat(shellConfig.mode).isEqualTo("CUSTOM_HTML")
        assertThat(shellConfig.customHtml).isEqualTo(customHtml)
        assertThat(shellConfig.retryButtonText).isEqualTo("Try Again")
        assertThat(shellConfig.customMediaPath).isEmpty()
    }

    @Test
    fun `customMediaPath survives full ApkConfig - JSON - shell pipeline`() {
        val mediaPath = "/storage/emulated/0/MyApp/offline.mp4"
        val apkConfig = newApkConfig().copy(
            errorPageMode = "CUSTOM_MEDIA",
            errorPageCustomMediaPath = mediaPath,
            errorPageRetryButtonText = "Reload"
        )

        val errorPageBlock = extractWebViewConfig(apkConfig).getAsJsonObject("errorPageConfig")
        assertThat(errorPageBlock.get("customMediaPath").asString).isEqualTo(mediaPath)

        val shellConfig = Gson().fromJson(errorPageBlock, ErrorPageShellConfig::class.java)
        assertThat(shellConfig.mode).isEqualTo("CUSTOM_MEDIA")
        assertThat(shellConfig.customMediaPath).isEqualTo(mediaPath)
        assertThat(shellConfig.retryButtonText).isEqualTo("Reload")
        assertThat(shellConfig.customHtml).isEmpty()
    }

    @Test
    fun `default ApkConfig still serializes empty error page custom fields`() {
        val apkConfig = newApkConfig()
        val errorPageBlock = extractWebViewConfig(apkConfig).getAsJsonObject("errorPageConfig")


        assertThat(errorPageBlock.has("customHtml")).isTrue()
        assertThat(errorPageBlock.has("customMediaPath")).isTrue()
        assertThat(errorPageBlock.has("retryButtonText")).isTrue()
        assertThat(errorPageBlock.get("customHtml").asString).isEmpty()
        assertThat(errorPageBlock.get("customMediaPath").asString).isEmpty()
        assertThat(errorPageBlock.get("retryButtonText").asString).isEmpty()
    }

    @Test
    fun `shell ErrorPageConfig drops empty customHtml back to null at runtime`() {

        val shellConfig = ErrorPageShellConfig(
            mode = "CUSTOM_HTML",
            customHtml = customHtml
        )
        val materialized = ErrorPageConfig(
            mode = ErrorPageMode.CUSTOM_HTML,
            customHtml = shellConfig.customHtml.takeIf { it.isNotEmpty() },
            customMediaPath = shellConfig.customMediaPath.takeIf { it.isNotEmpty() },
            retryButtonText = shellConfig.retryButtonText
        )

        val rendered = ErrorPageManager(materialized)
            .generateErrorPage(-2, "ERR_INTERNET_DISCONNECTED", "https://offline.test")

        assertThat(rendered).isEqualTo(customHtml)
    }


    private fun newApkConfig(): ApkConfig = ApkConfig(
        appName = "OfflineFixtureApp",
        packageName = "com.example.test",
        targetUrl = "https://offline.test/",
        versionCode = 1,
        versionName = "1.0"
    )

    private fun extractWebViewConfig(config: ApkConfig): JsonObject {
        val json = ApkConfigJsonFactory.create(config)
        val root = JsonParser.parseString(json).asJsonObject
        return root.getAsJsonObject("webViewConfig")
    }
}
