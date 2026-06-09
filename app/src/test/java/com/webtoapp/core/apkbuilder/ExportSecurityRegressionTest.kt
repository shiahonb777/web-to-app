package com.webtoapp.core.apkbuilder

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.Announcement
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.AutoStartConfig
import com.webtoapp.data.model.FloatingWindowConfig
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlLoadMode
import com.webtoapp.data.model.NativeBridgeCapabilities
import com.webtoapp.data.model.NodeJsConfig
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.playstore.aab.axml.AxmlToProtoXml
import java.io.File
import java.util.zip.ZipFile
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExportSecurityRegressionTest {

    @Rule @JvmField
    val koinRule = com.webtoapp.util.KoinCleanupRule()

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `default generated launcher icon uses black background and white first character`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "generateDefaultIcon",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }

        val bitmap = method.invoke(builder, "首页", "LAVENDER") as Bitmap

        assertThat(bitmap.getPixel(bitmap.width / 2, 20)).isEqualTo(0xFF000000.toInt())
        assertThat(hasWhiteIconPixel(bitmap)).isTrue()

        bitmap.recycle()
    }

    @Test
    fun `default generated launcher icon initial keeps first unicode character`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "getDefaultIconInitial",
            String::class.java
        ).apply { isAccessible = true }

        assertThat(method.invoke(builder, "首页")).isEqualTo("首")
        assertThat(method.invoke(builder, " web")).isEqualTo("W")
        assertThat(method.invoke(builder, "")).isEqualTo("A")
    }

    @Test
    fun `plain web export prefers dedicated shell template`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = CompositeTemplateProvider.default(context)
        val config = WebApp(
            name = "Zenbox",
            url = "https://example.com",
            appType = AppType.WEB
        ).toApkConfig("com.example.zenbox", context)

        val template = provider.getTemplateFor(config)

        assertThat(template).isNotNull()
        assertThat(template!!.name).isEqualTo("shell.apk")
        assertThat(template.absolutePath).contains("shell_templates")
    }

    @Test
    fun `asset template provider does not silently fall back for plain web exports`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = AssetTemplateProvider(context)
        val config = WebApp(
            name = "Zenbox",
            url = "https://example.com",
            appType = AppType.WEB
        ).toApkConfig("com.example.zenbox", context)

        assertThat(provider.supports(config)).isTrue()
        assertThat(provider.allowFallbackOnMissing).isFalse()
    }

    @Test
    fun `plain web export only injects network permissions by default`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredPermissions",
            ApkConfig::class.java
        ).apply { isAccessible = true }

        val config = WebApp(
            name = "Zenbox",
            url = "https://example.com",
            appType = AppType.WEB,
            apkExportConfig = ApkExportConfig()
        ).toApkConfig("com.example.zenbox", context)

        @Suppress("UNCHECKED_CAST")
        val permissions = method.invoke(builder, config) as List<String>

        assertThat(permissions).containsExactly(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        ).inOrder()
    }

    private fun hasWhiteIconPixel(bitmap: Bitmap): Boolean {
        val min = bitmap.width / 4
        val max = bitmap.width * 3 / 4
        for (y in min until max) {
            for (x in min until max) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = pixel ushr 24 and 0xFF
                val red = pixel ushr 16 and 0xFF
                val green = pixel ushr 8 and 0xFF
                val blue = pixel and 0xFF
                if (alpha > 200 && red > 220 && green > 220 && blue > 220) {
                    return true
                }
            }
        }
        return false
    }

    @Test
    fun `runtime permissions are only injected when explicitly enabled`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredPermissions",
            ApkConfig::class.java
        ).apply { isAccessible = true }

        val config = WebApp(
            name = "Recorder",
            url = "https://example.com",
            appType = AppType.WEB,
            apkExportConfig = ApkExportConfig(
                runtimePermissions = com.webtoapp.data.model.ApkRuntimePermissions(
                    camera = true,
                    microphone = true,
                    location = true,
                    notifications = true
                )
            )
        ).toApkConfig("com.example.recorder", context)

        @Suppress("UNCHECKED_CAST")
        val permissions = method.invoke(builder, config) as List<String>

        assertThat(permissions).containsExactly(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.FOREGROUND_SERVICE_LOCATION",
            "android.permission.FOREGROUND_SERVICE_CAMERA",
            "android.permission.FOREGROUND_SERVICE_MICROPHONE"
        ).inOrder()
    }

    @Test
    fun `plain web export only keeps baseline runtime components`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredComponents",
            ApkConfig::class.java
        ).apply { isAccessible = true }

        val config = WebApp(
            name = "Zenbox",
            url = "https://example.com",
            appType = AppType.WEB,
            apkExportConfig = ApkExportConfig()
        ).toApkConfig("com.example.zenbox", context)

        @Suppress("UNCHECKED_CAST")
        val components = method.invoke(builder, config) as Set<String>

        assertThat(components).containsExactly(
            "com.webtoapp.WebToAppApplication",
            "com.webtoapp.ui.MainActivity",
            "com.webtoapp.ui.shell.ShellActivity",
            "androidx.core.content.FileProvider"
        ).inOrder()
    }

    @Test
    fun `runtime components are only kept when explicitly enabled`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredComponents",
            ApkConfig::class.java
        ).apply { isAccessible = true }

        val config = WebApp(
            name = "Runtime",
            url = "",
            appType = AppType.NODEJS_APP,
            nodejsConfig = NodeJsConfig(projectId = "node", projectName = "Node"),
            webViewConfig = WebViewConfig(
                floatingWindowConfig = FloatingWindowConfig(enabled = true),
                enableNativeBridge = true,
                nativeBridgeCapabilities = NativeBridgeCapabilities(notification = true)
            ),
            apkExportConfig = ApkExportConfig(
                backgroundRunEnabled = true,
                notificationEnabled = true
            ),
            autoStartConfig = AutoStartConfig(
                bootStartEnabled = true,
                scheduledStartEnabled = true
            ),
            forcedRunConfig = ForcedRunConfig(enabled = true)
        ).toApkConfig("com.example.runtime", context)

        @Suppress("UNCHECKED_CAST")
        val components = method.invoke(builder, config) as Set<String>

        assertThat(components).containsAtLeast(
            "com.webtoapp.core.nodejs.NodeService",
            "com.webtoapp.core.background.BackgroundRunService",
            "com.webtoapp.core.notification.NotificationPollingService",
            "com.webtoapp.core.notification.BridgeAlarmReceiver",
            "com.webtoapp.core.floatingwindow.FloatingWindowService",
            "com.webtoapp.core.forcedrun.ForcedRunGuardService",
            "com.webtoapp.core.forcedrun.ForcedRunAccessibilityService",
            "com.webtoapp.core.forcedrun.ForcedRunReceiver",
            "com.webtoapp.core.autostart.BootReceiver",
            "com.webtoapp.core.autostart.ScheduledStartReceiver",
            "com.webtoapp.core.port.PortQueryReceiver",
            "com.webtoapp.core.port.PortReleaseReceiver"
        )
    }

    @Test
    fun `geckoview export keeps gecko process components`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredComponents",
            ApkConfig::class.java
        ).apply { isAccessible = true }

        val config = WebApp(
            name = "Firefox",
            url = "https://example.com",
            appType = AppType.WEB,
            apkExportConfig = ApkExportConfig(engineType = "GECKOVIEW")
        ).toApkConfig("com.example.firefox", context)

        @Suppress("UNCHECKED_CAST")
        val components = method.invoke(builder, config) as Set<String>

        assertThat(components).contains("org.mozilla.gecko.media.MediaManager")
        assertThat(components).contains("org.mozilla.gecko.process.GeckoChildProcessServices\$tab0")
        assertThat(components).contains("org.mozilla.gecko.process.GeckoChildProcessServices\$tab39")
    }

    @Test
    fun `plain web manifest prunes unused sensitive runtime components`() {
        val template = File("src/main/assets/template/webview_shell.apk")
        assumeTrue(
            "shell template not built - run ':app:syncShellTemplateApk' first",
            template.exists()
        )

        val axmlBytes = ZipFile(template).use { zip ->
            val entry = zip.getEntry("AndroidManifest.xml")
                ?: error("AndroidManifest.xml missing from shell template")
            zip.getInputStream(entry).readBytes()
        }

        val modified = AxmlRebuilder().expandAndModifyFull(
            axmlData = axmlBytes,
            originalPackage = "com.webtoapp",
            newPackage = "com.example.zenbox",
            versionCode = 1,
            versionName = "1.0",
            appName = "Zenbox",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE"
            ),
            requiredComponents = setOf(
                "com.webtoapp.WebToAppApplication",
                "com.webtoapp.ui.MainActivity",
                "com.webtoapp.ui.shell.ShellActivity",
                "androidx.core.content.FileProvider"
            )
        )

        val application = AxmlToProtoXml.convert(modified)
            .element
            .childList
            .filter { it.hasElement() }
            .map { it.element }
            .single { it.name == "application" }
        val componentNames = application.childList
            .filter { it.hasElement() }
            .map { it.element }
            .filter { it.name in setOf("service", "receiver", "provider") }
            .mapNotNull { element ->
                element.attributeList.firstOrNull { it.name == "name" }?.value
            }

        assertThat(componentNames).contains("androidx.core.content.FileProvider")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.background.BackgroundRunService")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.notification.NotificationPollingService")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.notification.BridgeAlarmReceiver")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.floatingwindow.FloatingWindowService")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.forcedrun.ForcedRunGuardService")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.forcedrun.ForcedRunAccessibilityService")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.forcedrun.ForcedRunReceiver")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.nodejs.NodeService")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.autostart.BootReceiver")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.autostart.ScheduledStartReceiver")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.port.PortQueryReceiver")
        assertThat(componentNames).doesNotContain("com.webtoapp.core.port.PortReleaseReceiver")
        assertThat(componentNames).doesNotContain("org.mozilla.gecko.media.MediaManager")
        assertThat(componentNames).doesNotContain("org.mozilla.gecko.process.GeckoChildProcessServices\$tab0")
    }

    @Test
    fun `file scheme html export omits network permissions`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredPermissions",
            ApkConfig::class.java
        ).apply { isAccessible = true }
        val projectDir = temp.newFolder("plain-html")
        File(projectDir, "index.html").writeText("<html><body>Offline</body></html>")

        val config = WebApp(
            name = "Offline",
            url = "",
            appType = AppType.HTML,
            htmlConfig = HtmlConfig(
                projectDir = projectDir.absolutePath,
                entryFile = "index.html"
            ),
            apkExportConfig = ApkExportConfig()
        ).toApkConfig("com.example.offline", context)

        @Suppress("UNCHECKED_CAST")
        val permissions = method.invoke(builder, config) as List<String>

        assertThat(config.htmlUsesFileScheme).isTrue()
        assertThat(permissions).doesNotContain("android.permission.INTERNET")
        assertThat(permissions).doesNotContain("android.permission.ACCESS_NETWORK_STATE")
    }

    @Test
    fun `local http html export keeps network permissions`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredPermissions",
            ApkConfig::class.java
        ).apply { isAccessible = true }

        val config = WebApp(
            name = "Game",
            url = "",
            appType = AppType.HTML,
            htmlConfig = HtmlConfig(
                entryFile = "index.html",
                loadMode = HtmlLoadMode.LOCAL_HTTP
            ),
            apkExportConfig = ApkExportConfig()
        ).toApkConfig("com.example.game", context)

        @Suppress("UNCHECKED_CAST")
        val permissions = method.invoke(builder, config) as List<String>

        assertThat(config.htmlUsesFileScheme).isFalse()
        assertThat(permissions).contains("android.permission.INTERNET")
        assertThat(permissions).contains("android.permission.ACCESS_NETWORK_STATE")
    }

    @Test
    fun `file scheme html with remote announcement link keeps network permissions`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val builder = ApkBuilder(context)
        val method = ApkBuilder::class.java.getDeclaredMethod(
            "buildRequiredPermissions",
            ApkConfig::class.java
        ).apply { isAccessible = true }
        val projectDir = temp.newFolder("announcement-html")
        File(projectDir, "index.html").writeText("<html><body>Offline</body></html>")

        val config = WebApp(
            name = "Offline",
            url = "",
            appType = AppType.HTML,
            htmlConfig = HtmlConfig(
                projectDir = projectDir.absolutePath,
                entryFile = "index.html"
            ),
            announcementEnabled = true,
            announcement = Announcement(
                title = "Update",
                linkUrl = "https://example.com/update"
            ),
            apkExportConfig = ApkExportConfig()
        ).toApkConfig("com.example.announcement", context)

        @Suppress("UNCHECKED_CAST")
        val permissions = method.invoke(builder, config) as List<String>

        assertThat(config.htmlUsesFileScheme).isTrue()
        assertThat(permissions).contains("android.permission.INTERNET")
        assertThat(permissions).contains("android.permission.ACCESS_NETWORK_STATE")
    }
}
