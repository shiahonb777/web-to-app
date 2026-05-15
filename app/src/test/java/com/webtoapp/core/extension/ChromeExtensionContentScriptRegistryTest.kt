package com.webtoapp.core.extension

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChromeExtensionContentScriptRegistryTest {

    private lateinit var context: android.content.Context
    private lateinit var extRoot: File
    private lateinit var extDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ChromeExtensionContentScriptRegistry.initialize(context)
        ChromeExtensionContentScriptRegistry.clearAllForTest()

        extRoot = File(context.filesDir, "extensions").apply { mkdirs() }
        extDir = File(extRoot, "ext-a").apply { mkdirs() }
        File(extDir, "script.js").writeText("window.__dyn = (window.__dyn || 0) + 1;")
        File(extDir, "style.css").writeText("body { color: red; }")
    }

    @After
    fun tearDown() {
        ChromeExtensionContentScriptRegistry.clearAllForTest()
        extRoot.deleteRecursively()
    }

    @Test
    fun `registered content scripts build extension modules`() {
        ChromeExtensionContentScriptRegistry.registerContentScripts(
            "ext-a",
            """
            [
              {
                "id": "dynamic-script",
                "matches": ["https://example.com/*"],
                "js": ["script.js"],
                "css": ["style.css"],
                "runAt": "document_start",
                "world": "MAIN"
              }
            ]
            """.trimIndent()
        )

        val modules = ChromeExtensionContentScriptRegistry.buildModules(context, "ext-a")

        assertThat(modules).hasSize(1)
        val module = modules.first()
        assertThat(module.id).isEqualTo("ext-a_regcs_dynamic-script")
        assertThat(module.runAt).isEqualTo(ModuleRunTime.DOCUMENT_START)
        assertThat(module.world).isEqualTo("MAIN")
        assertThat(module.cssCode).contains("color: red")
        assertThat(module.code).contains("__dyn")
        assertThat(module.runMode).isEqualTo(ModuleRunMode.AUTO)
        assertThat(module.shouldRegisterInPanel()).isTrue()
    }

    @Test
    fun `get and unregister content scripts respect ids filter`() {
        ChromeExtensionContentScriptRegistry.registerContentScripts(
            "ext-a",
            """
            [
              {
                "id": "one",
                "matches": ["https://example.com/*"],
                "js": ["script.js"]
              },
              {
                "id": "two",
                "matches": ["https://example.com/*"],
                "css": ["style.css"]
              }
            ]
            """.trimIndent()
        )

        val filtered = ChromeExtensionContentScriptRegistry.getRegisteredContentScriptsJson(
            "ext-a",
            """{"ids":["two"]}"""
        )
        assertThat(filtered).contains("\"id\":\"two\"")
        assertThat(filtered).doesNotContain("\"id\":\"one\"")

        ChromeExtensionContentScriptRegistry.unregisterContentScripts(
            "ext-a",
            """{"ids":["one"]}"""
        )

        val remainingModules = ChromeExtensionContentScriptRegistry.buildModules(context, "ext-a")
        assertThat(remainingModules).hasSize(1)
        assertThat(remainingModules.first().id).isEqualTo("ext-a_regcs_two")
    }
}
