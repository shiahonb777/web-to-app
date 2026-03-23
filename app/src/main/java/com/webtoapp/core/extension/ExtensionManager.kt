package com.webtoapp.core.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type

/**
 * 扩展模块管理器
 * 
 * 负责模块的增删改查、导入导出、存储管理
 */
class ExtensionManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ExtensionManager"
        private const val MODULES_DIR = "extension_modules"
        private const val MODULES_FILE = "modules.json"
        private const val BUILTIN_STATES_FILE = "builtin_states.json"  // Built-in模块启用状态
        private const val MODULE_FILE_EXTENSION = ".wtamod"  // WebToApp Module
        private const val PACKAGE_FILE_EXTENSION = ".wtapkg" // WebToApp Package
        
        @Volatile
        private var INSTANCE: ExtensionManager? = null
        
        fun getInstance(context: Context): ExtensionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExtensionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * 释放单例实例
         * 通常在 Application.onTerminate 或测试时调用
         */
        fun release() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
    
    // 使用容错性更强的 Gson 配置
    // Custom deserializer for enums that falls back to default value on error
    private inline fun <reified T : Enum<T>> enumDeserializer(defaultValue: T): JsonDeserializer<T> {
        return JsonDeserializer { json, _, _ ->
            try {
                enumValueOf<T>(json.asString)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown enum value: ${json.asString}, using default: $defaultValue")
                defaultValue
            }
        }
    }
    
    private val gson: Gson = GsonBuilder()
        .setLenient()  // 容许非标准 JSON
        .serializeNulls()  // 序列化 null 值
        // Register enum type adapters with fallback values
        .registerTypeAdapter(ModuleCategory::class.java, enumDeserializer(ModuleCategory.OTHER))
        .registerTypeAdapter(ModuleRunTime::class.java, enumDeserializer(ModuleRunTime.DOCUMENT_END))
        .registerTypeAdapter(ModuleUiType::class.java, enumDeserializer(ModuleUiType.FLOATING_BUTTON))
        .registerTypeAdapter(UiPosition::class.java, enumDeserializer(UiPosition.BOTTOM_RIGHT))
        .registerTypeAdapter(ToolbarOrientation::class.java, enumDeserializer(ToolbarOrientation.HORIZONTAL))
        .registerTypeAdapter(SidebarPosition::class.java, enumDeserializer(SidebarPosition.RIGHT))
        .registerTypeAdapter(ConfigItemType::class.java, enumDeserializer(ConfigItemType.TEXT))
        .registerTypeAdapter(ModulePermission::class.java, enumDeserializer(ModulePermission.DOM_ACCESS))
        .registerTypeAdapter(ModuleTrigger::class.java, enumDeserializer(ModuleTrigger.AUTO))
        .create()
    private val modulesDir: File by lazy {
        File(context.filesDir, MODULES_DIR).apply { mkdirs() }
    }
    
    // Module列表状态
    private val _modules = MutableStateFlow<List<ExtensionModule>>(emptyList())
    val modules: StateFlow<List<ExtensionModule>> = _modules.asStateFlow()
    
    // Built-in模块
    private val _builtInModules = MutableStateFlow<List<ExtensionModule>>(emptyList())
    val builtInModules: StateFlow<List<ExtensionModule>> = _builtInModules.asStateFlow()
    
    init {
        // Initialize时加载模块
        loadModules()
        loadBuiltInModules()
    }
    
    /**
     * 加载所有用户模块
     */
    private fun loadModules() {
        try {
            val file = File(modulesDir, MODULES_FILE)
            if (file.exists()) {
                val json = file.readText()
                if (json.isBlank()) {
                    Log.d(TAG, "Modules file is empty")
                    _modules.value = emptyList()
                    return
                }
                val type = object : TypeToken<List<ExtensionModule>>() {}.type
                val loadedModules: List<ExtensionModule>? = try {
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    Log.e(TAG, "JSON parsing failed, attempting recovery", e)
                    // 尝试恢复：备份损坏文件并重置
                    val backupFile = File(modulesDir, "modules_backup_${System.currentTimeMillis()}.json")
                    file.copyTo(backupFile, overwrite = true)
                    Log.w(TAG, "Corrupted modules backed up to: ${backupFile.name}")
                    null
                }
                _modules.value = loadedModules?.filterNotNull() ?: emptyList()
                Log.d(TAG, "Loaded ${_modules.value.size} modules")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load modules", e)
            _modules.value = emptyList()
        }
    }
    
    /**
     * 加载内置模块
     */
    private fun loadBuiltInModules() {
        val builtInStates = loadBuiltInStates()
        _builtInModules.value = BuiltInModules.getAll().map { module ->
            // App保存的启用状态
            val savedEnabled = builtInStates[module.id]
            if (savedEnabled != null) {
                module.copy(enabled = savedEnabled)
            } else {
                module
            }
        }
    }
    
    /**
     * 重新加载内置模块（语言切换时调用）
     */
    fun reloadBuiltInModules() {
        loadBuiltInModules()
        Log.d(TAG, "Reloaded built-in modules for language change")
    }
    
    /**
     * 加载内置模块的启用状态
     */
    private fun loadBuiltInStates(): Map<String, Boolean> {
        return try {
            val file = File(modulesDir, BUILTIN_STATES_FILE)
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<Map<String, Boolean>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load built-in states", e)
            emptyMap()
        }
    }
    
    /**
     * 保存内置模块的启用状态
     */
    private suspend fun saveBuiltInStates() = withContext(Dispatchers.IO) {
        try {
            val states = _builtInModules.value.associate { it.id to it.enabled }
            val file = File(modulesDir, BUILTIN_STATES_FILE)
            file.writeText(gson.toJson(states))
            Log.d(TAG, "Saved built-in states")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save built-in states", e)
        }
    }
    
    /**
     * 保存模块列表
     */
    private suspend fun saveModules() = withContext(Dispatchers.IO) {
        try {
            val file = File(modulesDir, MODULES_FILE)
            val json = gson.toJson(_modules.value)
            file.writeText(json)
            Log.d(TAG, "Saved ${_modules.value.size} modules")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save modules", e)
        }
    }
    
    /**
     * 获取所有模块（内置 + 用户）
     */
    fun getAllModules(): List<ExtensionModule> {
        return _builtInModules.value + _modules.value
    }
    
    /**
     * 获取启用的模块
     */
    fun getEnabledModules(): List<ExtensionModule> {
        return getAllModules().filter { it.enabled }
    }
    
    /**
     * 根据 URL 获取匹配的模块
     */
    fun getModulesForUrl(url: String): List<ExtensionModule> {
        return getEnabledModules().filter { it.matchesUrl(url) }
    }
    
    /**
     * 根据分类获取模块
     */
    fun getModulesByCategory(category: ModuleCategory): List<ExtensionModule> {
        return getAllModules().filter { it.category == category }
    }
    
    /**
     * Search模块
     */
    fun searchModules(query: String): List<ExtensionModule> {
        val lowerQuery = query.lowercase()
        return getAllModules().filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery) ||
            it.tags.any { tag -> tag.lowercase().contains(lowerQuery) }
        }
    }
    
    /**
     * 添加模块
     */
    suspend fun addModule(module: ExtensionModule): Result<ExtensionModule> {
        return try {
            // Verify模块
            val errors = module.validate()
            if (errors.isNotEmpty()) {
                return Result.failure(IllegalArgumentException(errors.joinToString("\n")))
            }
            
            // Check是否已存在同名模块
            val existing = _modules.value.find { it.id == module.id }
            val newModule = if (existing != null) {
                // Update现有模块
                module.copy(updatedAt = System.currentTimeMillis())
            } else {
                module
            }
            
            val updatedList = _modules.value.filter { it.id != newModule.id } + newModule
            _modules.value = updatedList
            saveModules()
            
            Result.success(newModule)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 更新模块
     */
    suspend fun updateModule(module: ExtensionModule): Result<ExtensionModule> {
        return addModule(module.copy(updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * 删除模块
     */
    suspend fun deleteModule(moduleId: String): Result<Unit> {
        return try {
            _modules.value = _modules.value.filter { it.id != moduleId }
            saveModules()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 切换模块启用状态
     */
    suspend fun toggleModule(moduleId: String): Result<Boolean> {
        return try {
            // 先检查是否是内置模块
            val builtInModule = _builtInModules.value.find { it.id == moduleId }
            if (builtInModule != null) {
                val updatedModule = builtInModule.copy(enabled = !builtInModule.enabled)
                _builtInModules.value = _builtInModules.value.map { 
                    if (it.id == moduleId) updatedModule else it 
                }
                saveBuiltInStates()
                return Result.success(updatedModule.enabled)
            }
            
            // User模块
            val module = _modules.value.find { it.id == moduleId }
                ?: return Result.failure(IllegalArgumentException("模块不存在"))
            
            val updatedModule = module.copy(enabled = !module.enabled, updatedAt = System.currentTimeMillis())
            _modules.value = _modules.value.map { if (it.id == moduleId) updatedModule else it }
            saveModules()
            
            Result.success(updatedModule.enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 更新模块配置
     */
    suspend fun updateModuleConfig(moduleId: String, configValues: Map<String, String>): Result<Unit> {
        return try {
            val module = _modules.value.find { it.id == moduleId }
                ?: return Result.failure(IllegalArgumentException("模块不存在"))
            
            val updatedModule = module.copy(
                configValues = configValues,
                updatedAt = System.currentTimeMillis()
            )
            _modules.value = _modules.value.map { if (it.id == moduleId) updatedModule else it }
            saveModules()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update module config", e)
            Result.failure(e)
        }
    }

    
    // ==================== 导入导出功能 ====================
    
    /**
     * 导出单个模块为文件
     */
    suspend fun exportModule(moduleId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val module = getAllModules().find { it.id == moduleId }
                ?: return@withContext Result.failure(IllegalArgumentException("模块不存在"))
            
            val fileName = "${module.name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")}$MODULE_FILE_EXTENSION"
            val file = File(context.cacheDir, fileName)
            file.writeText(module.toJson())
            
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 导出多个模块为包
     */
    suspend fun exportModulePackage(
        moduleIds: List<String>,
        packageName: String,
        description: String = "",
        author: ModuleAuthor? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val modulesToExport = getAllModules().filter { it.id in moduleIds }
            if (modulesToExport.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("没有找到要导出的模块"))
            }
            
            val pkg = ModulePackage(
                name = packageName,
                description = description,
                author = author,
                modules = modulesToExport
            )
            
            val fileName = "${packageName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")}$PACKAGE_FILE_EXTENSION"
            val file = File(context.cacheDir, fileName)
            file.writeText(pkg.toJson())
            
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export module package", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从文件导入模块
     */
    suspend fun importModule(inputStream: InputStream): Result<ExtensionModule> = withContext(Dispatchers.IO) {
        try {
            val json = inputStream.bufferedReader().readText()
            val module = ExtensionModule.fromJson(json)
                ?: return@withContext Result.failure(IllegalArgumentException("无效的模块文件"))
            
            // Generate新 ID 避免冲突
            val importedModule = module.copy(
                id = java.util.UUID.randomUUID().toString(),
                builtIn = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            addModule(importedModule)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从分享码导入模块
     */
    suspend fun importFromShareCode(shareCode: String): Result<ExtensionModule> {
        return try {
            val module = ExtensionModule.fromShareCode(shareCode)
                ?: return Result.failure(IllegalArgumentException("无效的分享码"))
            
            val importedModule = module.copy(
                id = java.util.UUID.randomUUID().toString(),
                builtIn = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            addModule(importedModule)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import from share code", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从文件导入模块包
     */
    suspend fun importModulePackage(inputStream: InputStream): Result<List<ExtensionModule>> = withContext(Dispatchers.IO) {
        try {
            val json = inputStream.bufferedReader().readText()
            val pkg = ModulePackage.fromJson(json)
                ?: return@withContext Result.failure(IllegalArgumentException("无效的模块包文件"))
            
            val importedModules = mutableListOf<ExtensionModule>()
            for (module in pkg.modules) {
                val importedModule = module.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    builtIn = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                addModule(importedModule).onSuccess {
                    importedModules.add(it)
                }
            }
            
            Result.success(importedModules)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import module package", e)
            Result.failure(e)
        }
    }
    
    /**
     * 分享模块（生成分享 Intent）
     */
    fun shareModule(moduleId: String): Intent? {
        val module = getAllModules().find { it.id == moduleId } ?: return null
        
        val shareText = """
            📦 WebToApp 扩展模块分享
            
            模块名称：${module.name}
            ${if (module.description.isNotBlank()) "描述：${module.description}" else ""}
            分类：${module.category.getDisplayName()}
            版本：${module.version.name}
            
            分享码：
            ${module.toShareCode()}
            
            使用方法：复制分享码，在 WebToApp 扩展模块页面点击"导入" -> "从分享码导入"
        """.trimIndent()
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "WebToApp 扩展模块 - ${module.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }
    
    /**
     * 分享模块文件
     */
    suspend fun shareModuleFile(moduleId: String): Intent? = withContext(Dispatchers.IO) {
        val result = exportModule(moduleId)
        result.getOrNull()?.let { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    /**
     * 导出模块到默认的 Downloads 文件夹
     * @return 返回导出的文件路径
     */
    suspend fun exportModuleToDownloads(moduleId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val module = getAllModules().find { it.id == moduleId }
                ?: return@withContext Result.failure(IllegalArgumentException("模块不存在"))
            
            val fileName = "${module.name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")}$MODULE_FILE_EXTENSION"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            file.writeText(module.toJson())
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export module to Downloads", e)
            Result.failure(e)
        }
    }
    
    /**
     * 导出模块到指定 URI（用于 SAF 文件选择器）
     * @param moduleId 模块 ID
     * @param uri 用户选择的保存位置 URI
     */
    suspend fun exportModuleToUri(moduleId: String, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val module = getAllModules().find { it.id == moduleId }
                ?: return@withContext Result.failure(IllegalArgumentException("模块不存在"))
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(module.toJson().toByteArray())
            } ?: return@withContext Result.failure(IllegalStateException("无法打开输出流"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export module to URI", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取模块导出文件名
     */
    fun getModuleExportFileName(moduleId: String): String? {
        val module = getAllModules().find { it.id == moduleId } ?: return null
        return "${module.name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")}$MODULE_FILE_EXTENSION"
    }
    
    // ==================== 模块执行 ====================
    
    /**
     * 生成指定 URL 的所有模块注入代码
     * 每个模块独立包装，错误隔离
     */
    fun generateInjectionCode(url: String, runAt: ModuleRunTime): String {
        val matchingModules = getModulesForUrl(url).filter { it.runAt == runAt }
        if (matchingModules.isEmpty()) return ""
        
        return matchingModules.joinToString("\n\n") { module ->
            """
            // ========== ${module.name} (${module.version.name}) ==========
            (function() {
                try {
                    ${module.generateExecutableCode()}
                } catch(__moduleError__) {
                    console.error('[WebToApp Module Error] ${module.name}:', __moduleError__);
                }
            })();
            """.trimIndent()
        }
    }
    
    /**
     * 生成指定模块ID列表的注入代码
     * 每个模块独立包装，错误隔离
     * @param url 当前页面URL
     * @param runAt 运行时机
     * @param moduleIds 要注入的模块ID列表
     */
    fun generateInjectionCodeForModules(url: String, runAt: ModuleRunTime, moduleIds: List<String>): String {
        if (moduleIds.isEmpty()) return ""
        
        val allModules = getAllModules()
        // 注意：这里不检查 module.enabled，因为模块的启用状态由每个应用的配置控制
        // 如果应用配置中包含此模块ID，则无条件注入（只要匹配URL和运行时机）
        val targetModules = allModules.filter { module ->
            module.id in moduleIds && 
            module.runAt == runAt && 
            module.matchesUrl(url)
        }
        
        if (targetModules.isEmpty()) return ""
        
        return targetModules.joinToString("\n\n") { module ->
            """
            // ========== ${module.name} (${module.version.name}) ==========
            (function() {
                try {
                    ${module.generateExecutableCode()}
                } catch(__moduleError__) {
                    console.error('[WebToApp Module Error] ${module.name}:', __moduleError__);
                }
            })();
            """.trimIndent()
        }
    }
    
    /**
     * 根据ID获取模块
     */
    fun getModuleById(moduleId: String): ExtensionModule? {
        return getAllModules().find { it.id == moduleId }
    }
    
    /**
     * 根据ID列表获取模块
     */
    fun getModulesByIds(moduleIds: List<String>): List<ExtensionModule> {
        val allModules = getAllModules()
        return moduleIds.mapNotNull { id -> allModules.find { it.id == id } }
    }
    
    /**
     * 复制模块
     */
    suspend fun duplicateModule(moduleId: String): Result<ExtensionModule> {
        val module = _modules.value.find { it.id == moduleId }
            ?: return Result.failure(IllegalArgumentException("模块不存在"))
        
        val duplicated = module.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${module.name} (副本)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        return addModule(duplicated)
    }
    
    /**
     * 获取模块统计信息
     */
    fun getStatistics(): ModuleStatistics {
        val all = getAllModules()
        val user = _modules.value
        val builtIn = _builtInModules.value
        
        return ModuleStatistics(
            totalCount = all.size,
            userCount = user.size,
            builtInCount = builtIn.size,
            enabledCount = all.count { it.enabled },
            categoryStats = ModuleCategory.values().associateWith { cat ->
                all.count { it.category == cat }
            }
        )
    }
}

/**
 * 模块统计信息
 */
data class ModuleStatistics(
    val totalCount: Int,
    val userCount: Int,
    val builtInCount: Int,
    val enabledCount: Int,
    val categoryStats: Map<ModuleCategory, Int>
)
