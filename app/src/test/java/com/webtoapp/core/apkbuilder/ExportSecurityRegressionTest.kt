package com.webtoapp.core.apkbuilder

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExportSecurityRegressionTest {

    @Rule @JvmField
    val koinRule = com.webtoapp.util.KoinCleanupRule()

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
            "android.permission.POST_NOTIFICATIONS"
        ).inOrder()
    }
}
