package com.webtoapp.core.extension

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExtensionManagerTest {

    @Rule @JvmField
    val koinRule = com.webtoapp.util.KoinCleanupRule()

    private lateinit var context: Context
    private lateinit var modulesDir: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        modulesDir = File(context.filesDir, "extension_modules")
        modulesDir.deleteRecursively()
        modulesDir.mkdirs()
        ExtensionManager.release()
    }

    @After
    fun tearDown() {
        ExtensionManager.release()
        modulesDir.deleteRecursively()
    }

    private suspend fun awaitLoaded(manager: ExtensionManager) {
        withTimeout(5_000) {
            while (manager.isLoading.value) {
                delay(20)
            }
        }
    }

    @Test
    fun `addModule persists across manager reload`() = runTest {
        val manager = ExtensionManager.getInstance(context)
        awaitLoaded(manager)

        val module = ExtensionModule(
            id = "user-module-1",
            name = "Persisted Module",
            code = "window.__persisted = true",
            configValues = mapOf("token" to "abc")
        )

        manager.addModule(module).getOrThrow()

        ExtensionManager.release()
        val reloadedManager = ExtensionManager.getInstance(context)
        awaitLoaded(reloadedManager)

        val stored = reloadedManager.getAllModules()
            .filterNot { it.builtIn }
            .single { it.id == "user-module-1" }
        val loaded = reloadedManager.ensureCodeLoaded(stored)

        assertThat(loaded.name).isEqualTo("Persisted Module")
        assertThat(loaded.code).contains("window.__persisted = true")
        assertThat(loaded.configValues).containsEntry("token", "abc")
    }

    @Test
    fun `parseModulesJson skips malformed items`() = runTest {
        val manager = ExtensionManager.getInstance(context)
        awaitLoaded(manager)

        val validJson = ExtensionModule(
            id = "valid-module",
            name = "Valid Module",
            code = "console.log('ok')"
        ).toJson()
        val encoded = "[${validJson},\"broken-item\"]"

        val decoded = manager.parseModulesJson(encoded)

        assertThat(decoded).isNotNull()
        assertThat(decoded!!.map { it.id }).containsExactly("valid-module")
    }

    @Test
    fun `parseBuiltInStatesJson skips malformed entries`() = runTest {
        val manager = ExtensionManager.getInstance(context)
        awaitLoaded(manager)

        val decoded = manager.parseBuiltInStatesJson("""
            {
              "builtin-dark-mode": true,
              "broken": {"oops": 1},
              "builtin-auto-scroll": false
            }
        """.trimIndent())

        assertThat(decoded).isNotNull()
        assertThat(decoded).containsExactly(
            "builtin-dark-mode", true,
            "builtin-auto-scroll", false
        )
    }
}
