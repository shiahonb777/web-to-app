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
class ChromeExtensionScriptingBridgeTest {

    private lateinit var context: android.content.Context
    private lateinit var extRoot: File
    private lateinit var extDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        extRoot = File(context.filesDir, "extensions").apply { mkdirs() }
        extDir = File(extRoot, "ext-script").apply { mkdirs() }
        File(extDir, "first.js").writeText("window.__first = true;")
        File(extDir, "second.js").writeText("window.__second = true;")
        File(extDir, "base.css").writeText("body { color: red; }")
        File(extDir, "theme.css").writeText(".card { color: blue; }")
    }

    @After
    fun tearDown() {
        extRoot.deleteRecursively()
    }

    @Test
    fun `resolveExecutionSourceForTest loads js files in order`() {
        val source = ChromeExtensionScriptingBridge.resolveExecutionSourceForTest(
            context = context,
            extensionId = "ext-script",
            injectionJson = """{"files":["first.js","second.js"]}"""
        )

        assertThat(source).contains("// === first.js ===")
        assertThat(source).contains("window.__first = true;")
        assertThat(source).contains("// === second.js ===")
        assertThat(source).contains("window.__second = true;")
        assertThat(source).contains("return undefined;")
        assertThat(source.indexOf("first.js")).isLessThan(source.indexOf("second.js"))
    }

    @Test
    fun `resolveCssTextForTest concatenates css files for stable style identity`() {
        val css = ChromeExtensionScriptingBridge.resolveCssTextForTest(
            context = context,
            extensionId = "ext-script",
            injectionJson = """{"files":["base.css","theme.css"]}"""
        )

        assertThat(css).contains("/* === base.css === */")
        assertThat(css).contains("body { color: red; }")
        assertThat(css).contains("/* === theme.css === */")
        assertThat(css).contains(".card { color: blue; }")
        assertThat(css.indexOf("base.css")).isLessThan(css.indexOf("theme.css"))
    }
}
