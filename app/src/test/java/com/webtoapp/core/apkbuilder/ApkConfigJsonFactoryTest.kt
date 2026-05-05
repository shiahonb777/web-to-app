package com.webtoapp.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.data.model.CustomCaCertificate
import com.webtoapp.data.model.NetworkTrustConfig
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.UserScript
import com.webtoapp.util.GsonProvider
import org.junit.Test

class ApkConfigJsonFactoryTest {

    @Test
    fun `create emits valid json with escaped user content`() {
        val config = ApkConfig(
            appName = "Quote \" Slash \\ Line\nTab\t",
            packageName = "com.example.generated",
            targetUrl = "https://example.com/path?name=\"web\\app\"",
            versionCode = 7,
            versionName = "1.2.\"beta\"",
            activationCodes = listOf("alpha\"one", "line\ncode"),
            announcementContent = "Hello \"world\"\n<script>alert('\\u2028')</script>",
            userAgent = "Mozilla/5.0 \"Custom\"",
            customUserAgent = "Custom\\Agent\nNext",
            injectScripts = listOf(
                UserScript(
                    name = "boot \"script\"",
                    code = "const msg = \"hello\\\\n\";\nconsole.log(msg);",
                    enabled = true,
                    runAt = ScriptRunTime.DOCUMENT_START
                )
            ),
            statusBarBackgroundType = "IMAGE",
            statusBarBackgroundImage = "/tmp/light background.png",
            statusBarBackgroundTypeDark = "IMAGE",
            statusBarBackgroundImageDark = "/tmp/dark background.png",
            networkTrustConfig = NetworkTrustConfig(
                trustUserCa = true,
                customCaCertificates = listOf(
                    CustomCaCertificate(
                        id = "ca1",
                        displayName = "Dev CA",
                        filePath = "/private/dev-ca.pem",
                        sha256 = "abc123"
                    )
                )
            ),
            bgmEnabled = true,
            bgmPlaylist = listOf(
                BgmShellItem(
                    id = "track\"1",
                    name = "Night\nDrive",
                    assetPath = "assets/bgm/night drive.mp3",
                    lrcAssetPath = "assets/bgm/night drive.lrc",
                    sortOrder = 3
                )
            )
        )

        val json = ApkConfigJsonFactory.create(config)
        val root = JsonParser.parseString(json).asJsonObject
        val webView = root.getAsJsonObject("webViewConfig")

        assertThat(root.get("schemaVersion").asInt).isEqualTo(ApkConfigJsonFactory.SCHEMA_VERSION)
        assertThat(root.get("appName").asString).isEqualTo(config.appName)
        assertThat(root.get("targetUrl").asString).isEqualTo(config.targetUrl)
        assertThat(webView.get("customUserAgent").asString).isEqualTo(config.customUserAgent)
        assertThat(webView.get("statusBarBackgroundImage").asString).isEqualTo("statusbar_background.png")
        assertThat(webView.get("statusBarBackgroundImageDark").asString).isEqualTo("statusbar_background_dark.png")
        assertThat(webView.getAsJsonArray("injectScripts")[0].asJsonObject.get("code").asString)
            .isEqualTo(config.injectScripts.single().code)
        val networkTrust = root.getAsJsonObject("networkTrustConfig")
        assertThat(networkTrust.get("trustUserCa").asBoolean).isTrue()
        assertThat(networkTrust.getAsJsonArray("customCaCertificates")[0].asJsonObject.get("sha256").asString)
            .isEqualTo("abc123")
        assertThat(networkTrust.getAsJsonArray("customCaCertificates")[0].asJsonObject.has("filePath")).isFalse()
        assertThat(root.getAsJsonArray("bgmPlaylist")[0].asJsonObject.get("name").asString)
            .isEqualTo("Night\nDrive")
    }

    @Test
    fun `create output is consumable by ShellConfig`() {
        val config = ApkConfig(
            appName = "Shell Ready",
            packageName = "com.example.shellready",
            targetUrl = "https://example.com",
            javaScriptEnabled = false,
            injectScripts = listOf(
                UserScript(
                    name = "start",
                    code = "window.__ready = true;",
                    runAt = ScriptRunTime.DOCUMENT_IDLE
                )
            ),
            bootStartEnabled = true,
            scheduledStartEnabled = true,
            scheduledTime = "07:30",
            scheduledDays = listOf(1, 3, 5)
        )

        val shellConfig = GsonProvider.gson.fromJson(
            ApkConfigJsonFactory.create(config),
            ShellConfig::class.java
        )

        assertThat(shellConfig.appName).isEqualTo(config.appName)
        assertThat(shellConfig.webViewConfig.javaScriptEnabled).isFalse()
        assertThat(shellConfig.webViewConfig.injectScripts.single().runAt).isEqualTo("DOCUMENT_IDLE")
        assertThat(shellConfig.autoStartConfig?.scheduledTime).isEqualTo("07:30")
        assertThat(shellConfig.autoStartConfig?.scheduledDays).containsExactly(1, 3, 5).inOrder()
    }

    @Test
    fun `create rejects web config with blank target url`() {
        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ApkConfigJsonFactory.create(
                ApkConfig(
                    appName = "Broken Web",
                    packageName = "com.example.brokenweb",
                    targetUrl = "   ",
                    appType = "WEB"
                )
            )
        }

        assertThat(error).hasMessageThat().contains("targetUrl must not be blank")
    }

    @Test
    fun `create rejects html config with invalid entry file`() {
        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ApkConfigJsonFactory.create(
                ApkConfig(
                    appName = "Broken Html",
                    packageName = "com.example.brokenhtml",
                    targetUrl = "",
                    appType = "HTML",
                    htmlEntryFile = ".html"
                )
            )
        }

        assertThat(error).hasMessageThat().contains("htmlConfig.entryFile")
    }

    @Test
    fun `create allows server backed app type without target url`() {
        val root = JsonParser.parseString(
            ApkConfigJsonFactory.create(
                ApkConfig(
                    appName = "Node App",
                    packageName = "com.example.nodeapp",
                    targetUrl = "",
                    appType = "NODEJS_APP"
                )
            )
        ).asJsonObject

        assertThat(root.get("appType").asString).isEqualTo("NODEJS_APP")
        assertThat(root.get("targetUrl").asString).isEmpty()
    }

    @Test
    fun `disabled optional services keep legacy null payloads`() {
        val config = ApkConfig(
            appName = "Optional Off",
            packageName = "com.example.optionaloff",
            targetUrl = "https://example.com",
            backgroundRunEnabled = false,
            backgroundRunConfig = BackgroundRunConfig(
                notificationTitle = "Should not leak"
            ),
            notificationEnabled = false,
            notificationConfig = NotificationConfig(
                pollUrl = "https://example.com/poll"
            )
        )

        val root = JsonParser.parseString(ApkConfigJsonFactory.create(config)).asJsonObject

        assertThat(root.get("backgroundRunConfig").isJsonNull).isTrue()
        assertThat(root.get("notificationConfig").isJsonNull).isTrue()
    }

    @Test
    fun `encrypted stub keeps only public placeholder fields`() {
        val config = ApkConfig(
            appName = "Private \"App\"",
            packageName = "com.example.private",
            targetUrl = "https://secret.example.com/token?value=hidden",
            versionCode = 99,
            versionName = "9.9.9",
            customUserAgent = "SensitiveAgent/1.0",
            activationCodes = listOf("SECRET-CODE")
        )

        val json = ApkConfigJsonFactory.createEncryptedStub(config)
        val root = JsonParser.parseString(json).asJsonObject

        assertThat(root.get("schemaVersion").asInt).isEqualTo(ApkConfigJsonFactory.SCHEMA_VERSION)
        assertThat(root.get("appName").asString).isEqualTo(config.appName)
        assertThat(root.get("packageName").asString).isEqualTo(config.packageName)
        assertThat(root.get("targetUrl").asString).isEmpty()
        assertThat(root.get("appType").asString).isEmpty()
        assertThat(root.get("versionCode").asInt).isEqualTo(0)
        assertThat(root.get("versionName").asString).isEmpty()
        assertThat(root.getAsJsonObject("webViewConfig").entrySet()).isEmpty()
        assertThat(json).doesNotContain("secret.example.com")
        assertThat(json).doesNotContain("SECRET-CODE")
        assertThat(json).doesNotContain("SensitiveAgent")
    }

    @Test
    fun `embedded modules serialize nested rules and config values structurally`() {
        val config = ApkConfig(
            appName = "Modules",
            packageName = "com.example.modules",
            targetUrl = "https://example.com",
            embeddedExtensionModules = listOf(
                EmbeddedExtensionModule(
                    id = "module\"one",
                    name = "Module\nOne",
                    code = "console.log(\"safe json\");",
                    cssCode = "body::before { content: \"x\"; }",
                    urlMatches = listOf(
                        EmbeddedUrlMatchRule(
                            pattern = "https://example.com/*",
                            isRegex = false,
                            exclude = false
                        )
                    ),
                    configValues = mapOf(
                        "selector\"key" to ".card[data-x=\"1\"]",
                        "message" to "line\nbreak"
                    )
                )
            )
        )

        val module = JsonParser.parseString(ApkConfigJsonFactory.create(config))
            .asJsonObject
            .getAsJsonArray("embeddedExtensionModules")[0]
            .asJsonObject

        assertThat(module.get("id").asString).isEqualTo("module\"one")
        assertThat(module.get("name").asString).isEqualTo("Module\nOne")
        assertThat(module.getAsJsonArray("urlMatches")[0].asJsonObject.get("pattern").asString)
            .isEqualTo("https://example.com/*")
        assertThat(module.getAsJsonObject("configValues").get("selector\"key").asString)
            .isEqualTo(".card[data-x=\"1\"]")
    }
}
