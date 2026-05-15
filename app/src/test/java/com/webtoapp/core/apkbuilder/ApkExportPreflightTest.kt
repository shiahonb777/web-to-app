package com.webtoapp.core.apkbuilder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.data.model.NetworkTrustConfig
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.WordPressConfig
import com.webtoapp.ui.shell.buildPackagedHtmlShellEntryUrl
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApkExportPreflightTest {

    @Rule @JvmField
    val koinRule = com.webtoapp.util.KoinCleanupRule()

    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `frontend app with saved files passes without source project directory`() {
        val index = temp.newFile("index.html").apply {
            writeText("<html></html>")
        }
        val app = WebApp(
            name = "Frontend",
            url = "",
            appType = AppType.FRONTEND,
            htmlConfig = HtmlConfig(
                entryFile = "index.html",
                files = listOf(HtmlFile("index.html", index.absolutePath, HtmlFileType.HTML))
            )
        )

        val report = ApkExportPreflight.check(context, app)

        assertThat(report.errors.map { it.key }).doesNotContain("htmlFiles")
        assertThat(report.passed).isTrue()
    }

    @Test
    fun `frontend app without source project directory targets saved html asset`() {
        val index = temp.newFile("index.html").apply {
            writeText("<html></html>")
        }
        val app = WebApp(
            name = "Frontend",
            url = "",
            appType = AppType.FRONTEND,
            htmlConfig = HtmlConfig(
                entryFile = "index.html",
                files = listOf(HtmlFile("index.html", index.absolutePath, HtmlFileType.HTML))
            )
        )

        val config = app.toApkConfig("com.example.frontend", context)

        assertThat(config.targetUrl).isEqualTo(
            buildPackagedHtmlShellEntryUrl("com.example.frontend", "index.html")
        )
    }

    @Test
    fun `frontend app with source project directory targets frontend asset`() {
        val app = WebApp(
            name = "Frontend",
            url = "",
            appType = AppType.FRONTEND,
            htmlConfig = HtmlConfig(
                projectDir = temp.newFolder("frontend").absolutePath,
                entryFile = "index.html"
            )
        )

        val config = app.toApkConfig("com.example.frontend", context)

        assertThat(config.targetUrl).isEqualTo(
            buildPackagedHtmlShellEntryUrl("com.example.frontend", "index.html")
        )
    }

    @Test
    fun `html app targets stable packaged loopback entry url`() {
        val app = WebApp(
            name = "Html",
            url = "",
            appType = AppType.HTML,
            htmlConfig = HtmlConfig(entryFile = "main/index.html")
        )

        val config = app.toApkConfig("com.example.frontend", context)

        assertThat(config.targetUrl).isEqualTo(
            buildPackagedHtmlShellEntryUrl("com.example.frontend", "main/index.html")
        )
    }

    @Test
    fun `wordpress app exports full runtime configuration`() {
        val app = WebApp(
            name = "WordPress",
            url = "",
            appType = AppType.WORDPRESS,
            wordpressConfig = WordPressConfig(
                projectId = "wp1",
                projectName = "My WP",
                siteTitle = "My Site",
                adminUser = "owner",
                adminEmail = "owner@example.com",
                adminPassword = "secret",
                themeName = "twentytwentyfour",
                plugins = listOf("woocommerce", "seo"),
                activePlugins = listOf("woocommerce"),
                permalinkStructure = "postname",
                siteLanguage = "zh_CN",
                autoInstall = true,
                sourceType = "SAMPLE",
                phpPort = 8088,
                landscapeMode = true
            )
        )

        val config = app.toApkConfig("com.example.wp", context)

        assertThat(config.targetUrl).isEqualTo("wordpress://localhost")
        assertThat(config.wordpressSiteTitle).isEqualTo("My Site")
        assertThat(config.wordpressAdminUser).isEqualTo("owner")
        assertThat(config.wordpressAdminEmail).isEqualTo("owner@example.com")
        assertThat(config.wordpressAdminPassword).isEqualTo("secret")
        assertThat(config.wordpressThemeName).isEqualTo("twentytwentyfour")
        assertThat(config.wordpressPlugins).containsExactly("woocommerce", "seo").inOrder()
        assertThat(config.wordpressActivePlugins).containsExactly("woocommerce")
        assertThat(config.wordpressPermalinkStructure).isEqualTo("postname")
        assertThat(config.wordpressSiteLanguage).isEqualTo("zh_CN")
        assertThat(config.wordpressAutoInstall).isTrue()
        assertThat(config.wordpressPhpPort).isEqualTo(8088)
        assertThat(config.wordpressLandscapeMode).isTrue()
    }

    @Test
    fun `network trust without any anchor is blocking error`() {
        val app = WebApp(
            name = "No Trust",
            url = "https://example.com",
            apkExportConfig = ApkExportConfig(
                networkTrustConfig = NetworkTrustConfig(
                    trustSystemCa = false,
                    trustUserCa = false,
                    customCaCertificates = emptyList()
                )
            )
        )

        val report = ApkExportPreflight.check(context, app)

        assertThat(report.passed).isFalse()
        assertThat(report.errors.map { it.key }).contains("networkTrust")
    }
}
