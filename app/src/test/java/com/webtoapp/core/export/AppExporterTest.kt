package com.webtoapp.core.export

import android.content.Context
import android.os.Environment
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.webapp.config.Announcement
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppExporterTest {

    private lateinit var context: Context
    private lateinit var exportRoot: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        exportRoot = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WebToApp")
        exportRoot.deleteRecursively()
    }

    @After
    fun tearDown() {
        exportRoot.deleteRecursively()
    }

    @Test
    fun `exportAsTemplate creates runnable project skeleton`() {
        val webApp = WebApp(
            name = "HTTP Demo",
            url = "http://example.com"
        )

        val result = AppExporter(context).exportAsTemplate(webApp)
        assertThat(result).isInstanceOf(ExportResult.Success::class.java)

        val projectDir = File((result as ExportResult.Success).path)
        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml")
        val themes = File(projectDir, "app/src/main/res/values/themes.xml")
        val appConfig = File(projectDir, "app/src/main/java/com/webtoapp/generated/AppConfig.kt")
        val networkConfig = File(projectDir, "app/src/main/res/xml/network_security_config.xml")
        val mainActivity = projectDir.walkTopDown().firstOrNull { it.name == "MainActivity.kt" }

        assertThat(manifest.exists()).isTrue()
        assertThat(themes.exists()).isTrue()
        assertThat(appConfig.exists()).isTrue()
        assertThat(networkConfig.exists()).isTrue()
        assertThat(mainActivity).isNotNull()
        assertThat(manifest.readText()).contains("Theme.WebToApp.Template")
        assertThat(manifest.readText()).contains("android:usesCleartextTraffic=\"true\"")
        assertThat(networkConfig.readText()).contains("cleartextTrafficPermitted=\"true\"")
    }

    @Test
    fun `exportAsTemplate escapes generated kotlin and xml strings`() {
        val webApp = WebApp(
            name = "He said \"Hi\" & <tag>",
            url = "https://example.com?q=\"1\"&name=\$demo",
            activationEnabled = true,
            activationCodes = listOf("A\"B", "Line\nBreak"),
            announcementEnabled = true,
            announcement = Announcement(
                title = "Title \"A\"",
                content = "Body\nLine",
                linkUrl = "https://example.com?a=1&b=2"
            ),
            adBlockEnabled = true,
            adBlockRules = listOf("rule\"one")
        )

        val result = AppExporter(context).exportAsTemplate(webApp)
        assertThat(result).isInstanceOf(ExportResult.Success::class.java)

        val projectDir = File((result as ExportResult.Success).path)
        val appConfig = File(projectDir, "app/src/main/java/com/webtoapp/generated/AppConfig.kt").readText()
        val stringsXml = File(projectDir, "app/src/main/res/values/strings.xml").readText()

        assertThat(appConfig).contains("const val APP_NAME = \"He said \\\"Hi\\\" & <tag>\"")
        assertThat(appConfig).contains("const val TARGET_URL = \"https://example.com?q=\\\"1\\\"&name=\\\$demo\"")
        assertThat(appConfig).contains("listOf(\"A\\\"B\", \"Line\\nBreak\")")
        assertThat(appConfig).contains("const val ANNOUNCEMENT_TITLE = \"Title \\\"A\\\"\"")
        assertThat(appConfig).contains("const val ANNOUNCEMENT_CONTENT = \"Body\\nLine\"")
        assertThat(stringsXml).contains("He said &quot;Hi&quot; &amp; &lt;tag&gt;")
    }
}
