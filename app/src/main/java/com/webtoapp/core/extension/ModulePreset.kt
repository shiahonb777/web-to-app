package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.reflect.TypeToken
import com.webtoapp.core.i18n.Strings
import java.io.File

/**
 * 模块方案 - 预设的模块组合
 * 
 * 用户可以保存常用的模块组合为方案，方便快速应用
 */
data class ModulePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String = "📦",
    val moduleIds: List<String>,
    val builtIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 模块方案管理器
 */
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
    }
    
    private val gson = com.webtoapp.util.GsonProvider.gson
    private val presetsFile: File by lazy {
        File(context.filesDir, "extension_modules/$PRESETS_FILE").apply {
            parentFile?.mkdirs()
        }
    }
    
    /**
     * 获取所有方案（内置 + 用户）
     */
    fun getAllPresets(): List<ModulePreset> {
        return getBuiltInPresets() + getUserPresets()
    }
    
    /**
     * 获取内置方案
     */
    fun getBuiltInPresets(): List<ModulePreset> = listOf(
        ModulePreset(
            id = "preset-reading",
            name = Strings.presetReading,
            description = Strings.presetReadingDesc,
            icon = "📖",
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
            description = Strings.presetAdblockDesc,
            icon = "🛡️",
            moduleIds = listOf(
                "builtin-adblocker-enhanced",
                "builtin-element-blocker"
            ),
            builtIn = true
        ),
        ModulePreset(
            id = "preset-media",
            name = Strings.presetMedia,
            description = Strings.presetMediaDesc,
            icon = "🎬",
            moduleIds = listOf(
                "builtin-video-speed",
                "builtin-image-downloader"
            ),
            builtIn = true
        ),
        ModulePreset(
            id = "preset-utility",
            name = Strings.presetUtility,
            description = Strings.presetUtilityDesc,
            icon = "🔧",
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
            description = Strings.presetNightDesc,
            icon = "🌙",
            moduleIds = listOf(
                "builtin-dark-mode",
                "builtin-night-shield"
            ),
            builtIn = true
        )
    )
    
    /**
     * 获取用户自定义方案
     */
    fun getUserPresets(): List<ModulePreset> {
        return try {
            if (presetsFile.exists()) {
                val json = presetsFile.readText()
                val type = object : TypeToken<List<ModulePreset>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 保存用户方案
     */
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
    
    /**
     * 删除用户方案
     */
    fun deletePreset(presetId: String): Result<Unit> {
        return try {
            val presets = getUserPresets().filter { it.id != presetId }
            presetsFile.writeText(gson.toJson(presets))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从当前选择创建方案
     */
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
