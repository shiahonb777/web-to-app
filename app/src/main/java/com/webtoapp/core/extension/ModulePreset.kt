package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.JsonParser
import com.webtoapp.core.i18n.Strings
import java.io.File






data class ModulePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String = "📦",
    val moduleIds: List<String>,
    val builtIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)




@SuppressLint("StaticFieldLeak")
class ModulePresetManager private constructor(private val context: Context) {

    companion object {
        private const val PRESETS_FILE = "module_presets.json"

        @Volatile
        private var INSTANCE: ModulePresetManager? = null

        fun getInstance(context: Context): ModulePresetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModulePresetManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun release() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }

    private val gson = com.webtoapp.util.GsonProvider.gson
    private val presetsFile: File by lazy {
        File(context.filesDir, "extension_modules/$PRESETS_FILE").apply {
            parentFile?.mkdirs()
        }
    }




    fun getAllPresets(): List<ModulePreset> {
        return getBuiltInPresets() + getUserPresets()
    }




    fun getBuiltInPresets(): List<ModulePreset> = listOf(
        ModulePreset(
            id = "preset-reading",
            name = Strings.presetReading,
            description = "",
            icon = "auto_stories",
            moduleIds = listOf(
                "builtin-dark-mode",
                "builtin-reading-mode",
                "builtin-custom-font",
                "builtin-auto-scroll"
            ),
            builtIn = true
        ),
        ModulePreset(
            id = "preset-adblock",
            name = Strings.presetAdblock,
            description = "",
            icon = "shield",
            moduleIds = listOf(
                "builtin-adblocker-enhanced",
                "builtin-element-blocker"
            ),
            builtIn = true
        ),
        ModulePreset(
            id = "preset-media",
            name = Strings.presetMedia,
            description = "",
            icon = "play_circle",
            moduleIds = listOf(
                "builtin-video-speed",
                "builtin-image-downloader"
            ),
            builtIn = true
        ),
        ModulePreset(
            id = "preset-utility",
            name = Strings.presetUtility,
            description = "",
            icon = "build",
            moduleIds = listOf(
                "builtin-copy-protection-remover",
                "builtin-translate-helper",
                "builtin-scroll-to-top"
            ),
            builtIn = true
        ),
        ModulePreset(
            id = "preset-night",
            name = Strings.presetNight,
            description = "",
            icon = "dark_mode",
            moduleIds = listOf(
                "builtin-dark-mode",
                "builtin-night-shield"
            ),
            builtIn = true
        )
    )




    fun getUserPresets(): List<ModulePreset> {
        return try {
            if (presetsFile.exists()) {
                val json = presetsFile.readText()
                parsePresetList(json) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun parsePresetList(json: String): List<ModulePreset>? {
        return try {
            val parsed = JsonParser.parseString(json)
            if (parsed.isJsonNull) return emptyList()
            if (!parsed.isJsonArray) return null

            val jsonArray = parsed.asJsonArray
            val result = jsonArray.mapNotNull { element ->
                try {
                    gson.fromJson(element, ModulePreset::class.java)
                } catch (_: Exception) {
                    null
                }
            }

            if (jsonArray.size() > 0 && result.isEmpty()) null else result
        } catch (_: Exception) {
            null
        }
    }




    fun savePreset(preset: ModulePreset): Result<ModulePreset> {
        return try {
            val presets = getUserPresets().toMutableList()
            val existingIndex = presets.indexOfFirst { it.id == preset.id }
            if (existingIndex >= 0) {
                presets[existingIndex] = preset
            } else {
                presets.add(preset)
            }
            presetsFile.writeText(gson.toJson(presets))
            Result.success(preset)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    fun deletePreset(presetId: String): Result<Unit> {
        return try {
            val presets = getUserPresets().filter { it.id != presetId }
            presetsFile.writeText(gson.toJson(presets))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    fun createPresetFromSelection(
        name: String,
        description: String,
        icon: String,
        moduleIds: Set<String>
    ): Result<ModulePreset> {
        val preset = ModulePreset(
            name = name,
            description = description,
            icon = icon,
            moduleIds = moduleIds.toList()
        )
        return savePreset(preset)
    }
}
