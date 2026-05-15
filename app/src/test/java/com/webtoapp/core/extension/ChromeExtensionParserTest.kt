package com.webtoapp.core.extension

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChromeExtensionParserTest {

    @Test
    fun `parseFromDirectory reports unsupported extensions without content scripts`() {
        val dir = createTempDirectory(prefix = "chrome-ext-no-content-").toFile()
        try {
            File(dir, "manifest.json").writeText(
                """
                {
                  "manifest_version": 3,
                  "name": "Popup Only",
                  "version": "1.0.0",
                  "action": {
                    "default_popup": "popup.html"
                  }
                }
                """.trimIndent()
            )

            val result = ChromeExtensionParser.parseFromDirectory(dir)

            assertThat(result.isValid).isTrue()
            assertThat(result.modules).hasSize(1)
            assertThat(result.modules.first().popupPath).isEqualTo("popup.html")
            assertThat(result.modules.first().manifestJson).contains("\"default_popup\"")
            assertThat(result.warnings).contains(
                "This extension has no content_scripts. Imported as popup/options UI only."
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `parseFromDirectory extracts popup options and manifest for content script extensions`() {
        val dir = createTempDirectory(prefix = "chrome-ext-popup-options-").toFile()
        try {
            File(dir, "content.js").writeText("console.log('hello')")
            File(dir, "manifest.json").writeText(
                """
                {
                  "manifest_version": 3,
                  "name": "Rich Extension",
                  "version": "2.0.0",
                  "description": "test",
                  "action": {
                    "default_popup": "popup.html"
                  },
                  "options_ui": {
                    "page": "options.html"
                  },
                  "content_scripts": [
                    {
                      "matches": ["https://example.com/*"],
                      "js": ["content.js"]
                    }
                  ]
                }
                """.trimIndent()
            )

            val result = ChromeExtensionParser.parseFromDirectory(dir)

            assertThat(result.isValid).isTrue()
            assertThat(result.modules).hasSize(1)
            val module = result.modules.first()
            assertThat(module.popupPath).isEqualTo("popup.html")
            assertThat(module.optionsPagePath).isEqualTo("options.html")
            assertThat(module.manifestJson).contains("\"options_ui\"")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `parseFromDirectory creates options only synthetic module`() {
        val dir = createTempDirectory(prefix = "chrome-ext-options-only-").toFile()
        try {
            File(dir, "manifest.json").writeText(
                """
                {
                  "manifest_version": 2,
                  "name": "Options Only",
                  "version": "1.2.3",
                  "options_page": "options.html"
                }
                """.trimIndent()
            )

            val result = ChromeExtensionParser.parseFromDirectory(dir)

            assertThat(result.isValid).isTrue()
            assertThat(result.modules).hasSize(1)
            val module = result.modules.first()
            assertThat(module.popupPath).isEmpty()
            assertThat(module.optionsPagePath).isEqualTo("options.html")
            assertThat(module.manifestJson).contains("\"options_page\"")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `parseFromDirectory creates background only synthetic module`() {
        val dir = createTempDirectory(prefix = "chrome-ext-background-only-").toFile()
        try {
            File(dir, "background.js").writeText("console.log('bg')")
            File(dir, "manifest.json").writeText(
                """
                {
                  "manifest_version": 3,
                  "name": "Background Only",
                  "version": "3.0.0",
                  "background": {
                    "service_worker": "background.js"
                  }
                }
                """.trimIndent()
            )

            val result = ChromeExtensionParser.parseFromDirectory(dir)

            assertThat(result.isValid).isTrue()
            assertThat(result.modules).hasSize(1)
            val module = result.modules.first()
            assertThat(module.backgroundScript).isEqualTo("background.js")
            assertThat(module.popupPath).isEmpty()
            assertThat(module.optionsPagePath).isEmpty()
            assertThat(result.warnings).contains(
                "This extension has no content_scripts. Imported as background/declarativeNetRequest runtime only."
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `parseFromDirectory creates declarative net request only synthetic module`() {
        val dir = createTempDirectory(prefix = "chrome-ext-dnr-only-").toFile()
        try {
            File(dir, "rules.json").writeText("[]")
            File(dir, "manifest.json").writeText(
                """
                {
                  "manifest_version": 3,
                  "name": "DNR Only",
                  "version": "1.0.0",
                  "permissions": ["declarativeNetRequest"],
                  "declarative_net_request": {
                    "rule_resources": [
                      { "id": "ruleset_1", "enabled": true, "path": "rules.json" }
                    ]
                  }
                }
                """.trimIndent()
            )

            val result = ChromeExtensionParser.parseFromDirectory(dir)

            assertThat(result.isValid).isTrue()
            assertThat(result.modules).hasSize(1)
            val module = result.modules.first()
            assertThat(module.backgroundScript).isEmpty()
            assertThat(module.manifestJson).contains("\"declarative_net_request\"")
            assertThat(result.warnings).contains(
                "This extension has no content_scripts. Imported as background/declarativeNetRequest runtime only."
            )
        } finally {
            dir.deleteRecursively()
        }
    }
}
