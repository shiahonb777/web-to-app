package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import android.os.Environment
import com.webtoapp.core.i18n.Strings
import java.io.File
import java.io.InputStreamReader
import com.google.gson.stream.JsonReader
import java.io.InputStream

/**
 * 扩展模块管理器
 * 
 * 负责模块的增删改查、导入导出、存储管理
 */
@SuppressLint("StaticFieldLeak")
class ExtensionManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ExtensionManager"
        private const val MODULES_DIR = "extension_modules"
        private const val MODULES_FILE = "modules.json"
        private const val BUILTIN_STATES_FILE = "builtin_states.json"  // Built-in模块启用状态
        private const val MODULE_FILE_EXTENSION = ".wtamod"  // WebToApp Module
        private const val PACKAGE_FILE_EXTENSION = ".wtapkg" // WebToApp Package
        
        // Pre-compiled regex for sanitizing file names (avoid recreating per call)
        private val SAFE_FILENAME_REGEX = Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]")
        
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
                AppLogger.i(TAG, "Unknown enum value: ${json.asString}, using default: $defaultValue")
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
        .registerTypeAdapter(ConfigItemType::class.java, enumDeserializer(ConfigItemType.TEXT))
        .registerTypeAdapter(ModulePermission::class.java, enumDeserializer(ModulePermission.DOM_ACCESS))
        .registerTypeAdapter(ModuleTrigger::class.java, enumDeserializer(ModuleTrigger.AUTO))
        .registerTypeAdapter(ModuleSourceType::class.java, enumDeserializer(ModuleSourceType.CUSTOM))
        .create()
    private val saveMutex = Mutex()
    private val modulesDir: File by lazy {
        File(context.filesDir, MODULES_DIR).apply { mkdirs() }
    }
    
    // Module列表状态
    private val _modules = MutableStateFlow<List<ExtensionModule>>(emptyList())
    val modules: StateFlow<List<ExtensionModule>> = _modules.asStateFlow()
    
    // Built-in模块
    private val _builtInModules = MutableStateFlow<List<ExtensionModule>>(emptyList())
    val builtInModules: StateFlow<List<ExtensionModule>> = _builtInModules.asStateFlow()
    
    // Cached combined module list (rebuilt only when _modules or _builtInModules change)
    @Volatile
    private var _allModulesCache: List<ExtensionModule> = emptyList()
    
    // Loading state for UI to observe
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Background scope for async initialization
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        // Built-in modules are lightweight — load synchronously
        loadBuiltInModules()
        rebuildAllModulesCache()
        
        // User modules may be very large (10+MB code) — load async to prevent ANR
        initScope.launch {
            loadModulesAsync()
            rebuildAllModulesCache()
            _isLoading.value = false
            AppLogger.d(TAG, "Async module loading completed")
        }
    }
    
    /**
     * Rebuild the cached combined module list.
     * Must be called whenever _modules or _builtInModules changes.
     */
    private fun rebuildAllModulesCache() {
        // User modules with same ID as built-in modules take precedence (user edited a built-in)
        val userModuleIds = _modules.value.map { it.id }.toSet()
        val filteredBuiltIn = _builtInModules.value.filter { it.id !in userModuleIds }
        _allModulesCache = filteredBuiltIn + _modules.value
    }
    
    /**
     * 异步加载所有用户模块
     * 初始加载时不加载代码内容（代码懒加载），仅加载元数据
     * 自动迁移旧版内联代码到独立文件
     */
    private suspend fun loadModulesAsync() = withContext(Dispatchers.IO) {
        try {
            val file = File(modulesDir, MODULES_FILE)
            if (file.exists()) {
                val json = file.readText()
                if (json.isBlank()) {
                    AppLogger.d(TAG, "Modules file is empty")
                    _modules.value = emptyList()
                    return@withContext
                }
                val loadedModules: List<ExtensionModule>? = parseModulesJson(json)
                    ?: run {
                        AppLogger.e(TAG, "JSON parsing failed, attempting recovery")
                        val backupFile = File(modulesDir, "modules_backup_${System.currentTimeMillis()}.json")
                        file.copyTo(backupFile, overwrite = true)
                        AppLogger.i(TAG, "Corrupted modules backed up to: ${backupFile.name}")
                        null
                    }
                
                val modules = loadedModules?.filterNotNull() ?: emptyList()
                
                // Migration: if any module has inline code or codeFiles, extract to separate files
                var needsMigration = false
                val migratedModules = modules.map { module ->
                    var m = module
                    if (m.code.isNotBlank()) {
                        saveModuleCode(m.id, m.code)
                        needsMigration = true
                        m = m.copy(code = "")  // Strip from in-memory (will be lazy-loaded)
                    }
                    if (m.cssCode.isNotBlank()) {
                        saveModuleCss(m.id, m.cssCode)
                        needsMigration = true
                        m = m.copy(cssCode = "")
                    }
                    if (m.codeFiles.isNotEmpty()) {
                        saveModuleCodeFiles(m.id, m.codeFiles)
                        needsMigration = true
                        m = m.copy(codeFiles = emptyMap())
                    }
                    m
                }
                
                // Rewrite modules.json without code if migration happened
                if (needsMigration) {
                    AppLogger.i(TAG, "Migrating ${modules.size} modules: stripping inline code to separate files")
                    file.writeText(gson.toJson(migratedModules))
                }
                
                // Store modules WITHOUT code in memory (lazy-load code on demand)
                _modules.value = migratedModules
                AppLogger.d(TAG, "Loaded ${_modules.value.size} modules (code lazy-loaded)")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load modules", e)
            _modules.value = emptyList()
        }
    }
    
    /**
     * 加载内置模块
     */
    private fun loadBuiltInModules() {
        val builtInStates = loadBuiltInStates()
        
        // 加载标准内置模块
        val standardModules = BuiltInModules.getAll()
        
        // 加载内置 Chrome 扩展模块
        val chromeExtModules = try {
            BuiltInChromeExtensions.getAll(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load built-in Chrome extensions", e)
            emptyList()
        }
        
        _builtInModules.value = (standardModules + chromeExtModules).map { module ->
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
        rebuildAllModulesCache()
        AppLogger.d(TAG, "Reloaded built-in modules for language change")
    }
    
    /**
     * 加载内置模块的启用状态
     */
    private fun loadBuiltInStates(): Map<String, Boolean> {
        return try {
            val file = File(modulesDir, BUILTIN_STATES_FILE)
            if (file.exists()) {
                val json = file.readText()
                parseBuiltInStatesJson(json) ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load built-in states", e)
            emptyMap()
        }
    }
    
    /**
     * 保存内置模块的启用状态
     */
    internal fun parseModulesJson(json: String): List<ExtensionModule>? {
        return try {
            val parsed = JsonParser.parseString(json)
            if (parsed.isJsonNull) return emptyList()
            if (!parsed.isJsonArray) return null

            val jsonArray = parsed.asJsonArray
            val result = jsonArray.mapNotNull { element ->
                try {
                    gson.fromJson(element, ExtensionModule::class.java)
                } catch (_: Exception) {
                    null
                }
            }

            if (jsonArray.size() > 0 && result.isEmpty()) null else result
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseBuiltInStatesJson(json: String): Map<String, Boolean>? {
        return try {
            val parsed = JsonParser.parseString(json)
            if (parsed.isJsonNull) return emptyMap()
            if (!parsed.isJsonObject) return null

            val jsonObject = parsed.asJsonObject
            val result = buildMap {
                jsonObject.entrySet().forEach { (key, value) ->
                    try {
                        if (!value.isJsonNull) {
                            put(key, value.asBoolean)
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            if (jsonObject.size() > 0 && result.isEmpty()) null else result
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun saveBuiltInStates() = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            try {
                val states = _builtInModules.value.associate { it.id to it.enabled }
                val file = File(modulesDir, BUILTIN_STATES_FILE)
                file.writeText(gson.toJson(states))
                AppLogger.d(TAG, "Saved built-in states")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to save built-in states", e)
            }
        }
    }
    
    /**
     * 保存模块列表
     * 代码内容单独存储为文件，modules.json 只保存轻量元数据
     * 这避免了每次保存都序列化所有模块的完整代码（可能几十 MB）
     */
    private suspend fun saveModules() = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            try {
                // Save code/cssCode/codeFiles to separate files, strip from JSON
                val strippedModules = _modules.value.map { module ->
                    // Save code to individual file
                    if (module.code.isNotBlank()) {
                        saveModuleCode(module.id, module.code)
                    }
                    if (module.cssCode.isNotBlank()) {
                        saveModuleCss(module.id, module.cssCode)
                    }
                    // Save codeFiles to subdirectory
                    if (module.codeFiles.isNotEmpty()) {
                        saveModuleCodeFiles(module.id, module.codeFiles)
                    }
                    // Strip code from JSON to keep modules.json small
                    module.copy(code = "", cssCode = "", codeFiles = emptyMap())
                }
                val file = File(modulesDir, MODULES_FILE)
                val json = gson.toJson(strippedModules)
                file.writeText(json)
                AppLogger.d(TAG, "Saved ${_modules.value.size} modules (code in separate files)")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to save modules", e)
            }
        }
    }
    
    // ==================== Separate Code File Storage ====================
    
    private fun saveModuleCode(moduleId: String, code: String) {
        try {
            File(modulesDir, "code_$moduleId.js").writeText(code)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save code for module $moduleId", e)
        }
    }
    
    private fun saveModuleCss(moduleId: String, css: String) {
        try {
            File(modulesDir, "css_$moduleId.css").writeText(css)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save CSS for module $moduleId", e)
        }
    }
    
    private fun loadModuleCode(moduleId: String): String {
        return try {
            val file = File(modulesDir, "code_$moduleId.js")
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load code for module $moduleId", e)
            ""
        }
    }
    
    private fun loadModuleCss(moduleId: String): String {
        return try {
            val file = File(modulesDir, "css_$moduleId.css")
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load CSS for module $moduleId", e)
            ""
        }
    }
    
    private fun deleteModuleCodeFiles(moduleId: String) {
        try {
            File(modulesDir, "code_$moduleId.js").delete()
            File(modulesDir, "css_$moduleId.css").delete()
            // Delete multi-file directory
            val codeFilesDir = File(modulesDir, "codefiles_$moduleId")
            if (codeFilesDir.exists()) {
                codeFilesDir.deleteRecursively()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete code files for module $moduleId", e)
        }
    }
    
    // ==================== Multi-file Code Storage ====================
    
    /**
     * 保存模块的多文件代码到独立子目录
     * 目录结构: extension_modules/codefiles_<moduleId>/<relativePath>
     */
    private fun saveModuleCodeFiles(moduleId: String, codeFiles: Map<String, String>) {
        try {
            val dir = File(modulesDir, "codefiles_$moduleId")
            // Clean existing files first to handle renames/deletions
            if (dir.exists()) dir.deleteRecursively()
            dir.mkdirs()
            
            codeFiles.forEach { (relativePath, content) ->
                val file = File(dir, relativePath)
                file.parentFile?.mkdirs()
                file.writeText(content)
            }
            AppLogger.d(TAG, "Saved ${codeFiles.size} code files for module $moduleId")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save code files for module $moduleId", e)
        }
    }
    
    /**
     * 从磁盘加载模块的多文件代码
     */
    private fun loadModuleCodeFiles(moduleId: String): Map<String, String> {
        return try {
            val dir = File(modulesDir, "codefiles_$moduleId")
            if (!dir.exists() || !dir.isDirectory) return emptyMap()
            
            val result = linkedMapOf<String, String>()
            dir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val relativePath = file.relativeTo(dir).path
                    result[relativePath] = file.readText()
                }
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load code files for module $moduleId", e)
            emptyMap()
        }
    }
    
    /**
     * 按需加载模块代码（懒加载）
     * 内存中的模块默认不含代码，需要时从文件加载
     * 用于注入、导出、编辑等需要代码的场景
     */
    fun ensureCodeLoaded(module: ExtensionModule): ExtensionModule {
        // If code is already loaded, return as-is
        if (module.code.isNotBlank() || module.cssCode.isNotBlank() || module.codeFiles.isNotEmpty()) return module
        
        val code = loadModuleCode(module.id)
        val css = loadModuleCss(module.id)
        val codeFiles = loadModuleCodeFiles(module.id)
        
        return if (code.isNotBlank() || css.isNotBlank() || codeFiles.isNotEmpty()) {
            module.copy(code = code, cssCode = css, codeFiles = codeFiles)
        } else {
            module
        }
    }
    
    /**
     * 获取所有模块（内置 + 用户）
     */
    fun getAllModules(): List<ExtensionModule> {
        return _allModulesCache
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
            rebuildAllModulesCache()
            saveModules()
            
            Result.success(newModule)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to add module", e)
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
            rebuildAllModulesCache()
            saveModules()
            deleteModuleCodeFiles(moduleId)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 切换模块启用状态
     */
    suspend fun toggleModule(moduleId: String): Result<Boolean> {
        return try {
            // User modules take precedence (may be an edited copy of a built-in module)
            val userModule = _modules.value.find { it.id == moduleId }
            if (userModule != null) {
                val updatedModule = userModule.copy(enabled = !userModule.enabled, updatedAt = System.currentTimeMillis())
                _modules.value = _modules.value.map { if (it.id == moduleId) updatedModule else it }
                rebuildAllModulesCache()
                saveModules()
                return Result.success(updatedModule.enabled)
            }
            
            // 检查是否是内置模块
            val builtInModule = _builtInModules.value.find { it.id == moduleId }
            if (builtInModule != null) {
                val updatedModule = builtInModule.copy(enabled = !builtInModule.enabled)
                _builtInModules.value = _builtInModules.value.map { 
                    if (it.id == moduleId) updatedModule else it 
                }
                rebuildAllModulesCache()
                saveBuiltInStates()
                return Result.success(updatedModule.enabled)
            }
            
            Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to toggle module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 更新模块配置
     */
    suspend fun updateModuleConfig(moduleId: String, configValues: Map<String, String>): Result<Unit> {
        return try {
            val module = _modules.value.find { it.id == moduleId }
                ?: return Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
            
            val updatedModule = module.copy(
                configValues = configValues,
                updatedAt = System.currentTimeMillis()
            )
            _modules.value = _modules.value.map { if (it.id == moduleId) updatedModule else it }
            rebuildAllModulesCache()
            saveModules()
            
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update module config", e)
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
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
            
            // Ensure code is loaded before export
            val fullModule = ensureCodeLoaded(module)
            val fileName = "${fullModule.name.replace(SAFE_FILENAME_REGEX, "_")}$MODULE_FILE_EXTENSION"
            val file = File(context.cacheDir, fileName)
            file.writeText(fullModule.toJson())
            
            Result.success(file)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export module", e)
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
            // Ensure code is loaded for all modules before export
            val modulesToExport = getAllModules()
                .filter { it.id in moduleIds }
                .map { ensureCodeLoaded(it) }
            if (modulesToExport.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException(Strings.errNoModulesToExport))
            }
            
            val pkg = ModulePackage(
                name = packageName,
                description = description,
                author = author,
                modules = modulesToExport
            )
            
            val fileName = "${packageName.replace(SAFE_FILENAME_REGEX, "_")}$PACKAGE_FILE_EXTENSION"
            val file = File(context.cacheDir, fileName)
            file.writeText(pkg.toJson())
            
            Result.success(file)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export module package", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从文件导入模块
     * 使用流式 JsonReader 避免将整个文件读入内存（大型模块可能 10+MB）
     */
    suspend fun importModule(inputStream: InputStream): Result<ExtensionModule> = withContext(Dispatchers.IO) {
        try {
            val module = try {
                val reader = JsonReader(InputStreamReader(inputStream.buffered()))
                reader.isLenient = true
                gson.fromJson<ExtensionModule>(reader, ExtensionModule::class.java)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Stream-based parsing failed", e)
                null
            } ?: return@withContext Result.failure(IllegalArgumentException(Strings.errInvalidModuleFile))
            
            // Generate新 ID 避免冲突
            val importedModule = module.copy(
                id = java.util.UUID.randomUUID().toString(),
                builtIn = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            addModule(importedModule)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to import module", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从分享码导入模块
     */
    suspend fun importFromShareCode(shareCode: String): Result<ExtensionModule> {
        return try {
            val module = ExtensionModule.fromShareCode(shareCode)
                ?: return Result.failure(IllegalArgumentException(Strings.errInvalidShareCode))
            
            val importedModule = module.copy(
                id = java.util.UUID.randomUUID().toString(),
                builtIn = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            addModule(importedModule)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to import from share code", e)
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
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errInvalidModulePackage))
            
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
            AppLogger.e(TAG, "Failed to import module package", e)
            Result.failure(e)
        }
    }
    
    /**
     * 分享模块（生成分享 Intent）
     */
    fun shareModule(moduleId: String): Intent? {
        val rawModule = getAllModules().find { it.id == moduleId } ?: return null
        val module = ensureCodeLoaded(rawModule)
        
        val shareText = """
            ${Strings.shareModuleTitle}
            
            ${Strings.shareModuleName}：${module.name}
            ${if (module.description.isNotBlank()) "${Strings.shareModuleDesc}：${module.description}" else ""}
            ${Strings.shareModuleCategory}：${module.category.getDisplayName()}
            ${Strings.shareModuleVersion}：${module.version.name}
            
            ${Strings.shareModuleCode}：
            ${module.toShareCode()}
            
            ${Strings.shareModuleHowTo}
        """.trimIndent()
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${Strings.shareModuleSubject} - ${module.name}")
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
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
            
            // Ensure code is loaded before export
            val fullModule = ensureCodeLoaded(module)
            val fileName = "${fullModule.name.replace(SAFE_FILENAME_REGEX, "_")}$MODULE_FILE_EXTENSION"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            file.writeText(fullModule.toJson())
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export module to Downloads", e)
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
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
            
            // Ensure code is loaded before export
            val fullModule = ensureCodeLoaded(module)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(fullModule.toJson().toByteArray())
            } ?: return@withContext Result.failure(IllegalStateException(Strings.errCannotOpenOutputStream))
            
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export module to URI", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取模块导出文件名
     */
    fun getModuleExportFileName(moduleId: String): String? {
        val module = getAllModules().find { it.id == moduleId } ?: return null
        return "${module.name.replace(SAFE_FILENAME_REGEX, "_")}$MODULE_FILE_EXTENSION"
    }
    
    // ==================== 模块执行 ====================
    
    /**
     * 生成指定 URL 的所有模块注入代码
     * 每个模块独立包装，错误隔离
     */
    fun generateInjectionCode(url: String, runAt: ModuleRunTime): String {
        // Skip CHROME_EXTENSION and USERSCRIPT modules — they are handled by their own
        // polyfill injection functions (injectChromeExtensionPolyfills / injectGreasemonkeyPolyfills)
        val matchingModules = getModulesForUrl(url).filter {
            it.runAt == runAt &&
            it.sourceType != ModuleSourceType.CHROME_EXTENSION &&
            it.sourceType != ModuleSourceType.USERSCRIPT
        }
        if (matchingModules.isEmpty()) return ""
        
        return matchingModules.joinToString("\n\n") { module ->
            val loadedModule = ensureCodeLoaded(module)
            """
            // ========== ${loadedModule.name} (${loadedModule.version.name}) ==========
            (function() {
                try {
                    ${loadedModule.generateExecutableCode()}
                } catch(__moduleError__) {
                    console.error('[WebToApp Module Error] ${loadedModule.name}:', __moduleError__);
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
        // Skip CHROME_EXTENSION and USERSCRIPT modules — they are handled by their own
        // polyfill injection functions (injectChromeExtensionPolyfills / injectGreasemonkeyPolyfills)
        val targetModules = allModules.filter { module ->
            module.id in moduleIds && 
            module.runAt == runAt && 
            module.matchesUrl(url) &&
            module.sourceType != ModuleSourceType.CHROME_EXTENSION &&
            module.sourceType != ModuleSourceType.USERSCRIPT
        }
        
        if (targetModules.isEmpty()) return ""
        
        return targetModules.joinToString("\n\n") { module ->
            val loadedModule = ensureCodeLoaded(module)
            """
            // ========== ${loadedModule.name} (${loadedModule.version.name}) ==========
            (function() {
                try {
                    ${loadedModule.generateExecutableCode()}
                } catch(__moduleError__) {
                    console.error('[WebToApp Module Error] ${loadedModule.name}:', __moduleError__);
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
        return moduleIds.mapNotNull { id -> 
            allModules.find { it.id == id }?.let { ensureCodeLoaded(it) }
        }
    }
    
    /**
     * 复制模块
     */
    suspend fun duplicateModule(moduleId: String): Result<ExtensionModule> {
        val module = _modules.value.find { it.id == moduleId }
            ?: return Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
        
        val duplicated = module.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${module.name} (${Strings.moduleCopySuffix})",
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
