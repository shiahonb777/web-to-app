package com.webtoapp.core.extension

import android.content.Context
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModulePresetManagerTest {

    private lateinit var context: Context
    private lateinit var manager: ModulePresetManager
    private lateinit var presetFile: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        manager = ModulePresetManager.getInstance(context)
        presetFile = File(context.filesDir, "extension_modules/module_presets.json")
        presetFile.parentFile?.mkdirs()
        presetFile.delete()
    }

    @After
    fun tearDown() {
        presetFile.delete()
    }

    @Test
    fun `built in presets are always available`() {
        val builtIns = manager.getBuiltInPresets()

        assertThat(builtIns).isNotEmpty()
        assertThat(builtIns.all { it.builtIn }).isTrue()
        assertThat(builtIns.map { it.id }).contains("preset-reading")
    }

    @Test
    fun `create save update and delete user preset`() {
        val created = manager.createPresetFromSelection(
            name = "My Preset",
            description = "desc",
            icon = "✨",
            moduleIds = setOf("builtin-dark-mode", "builtin-auto-scroll")
        ).getOrThrow()

        val loaded = manager.getUserPresets()
        assertThat(loaded.map { it.id }).contains(created.id)

        val updated = created.copy(name = "My Preset V2")
        manager.savePreset(updated).getOrThrow()
        val reloaded = manager.getUserPresets().first { it.id == created.id }
        assertThat(reloaded.name).isEqualTo("My Preset V2")

        manager.deletePreset(created.id).getOrThrow()
        assertThat(manager.getUserPresets().map { it.id }).doesNotContain(created.id)
    }

    @Test
    fun `getAllPresets merges built in and user presets`() {
        val created = manager.createPresetFromSelection(
            name = "Custom",
            description = "",
            icon = "📦",
            moduleIds = setOf("builtin-dark-mode")
        ).getOrThrow()

        val all = manager.getAllPresets()

        assertThat(all.map { it.id }).contains(created.id)
        assertThat(all.any { it.builtIn }).isTrue()
    }
}
