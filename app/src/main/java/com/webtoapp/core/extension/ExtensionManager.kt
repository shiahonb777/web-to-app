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
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import android.os.Environment
import com.webtoapp.core.i18n.Strings
import java.io.File
import java.io.InputStream

/**
 * Extension module manager.
 *
 * Handles CRUD, import/export, and storage for modules.
 */
@SuppressLint("StaticFieldLeak")
class ExtensionManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ExtensionManager"
        private const val MODULES_DIR = "extension_modules"
        private const val MODULES_FILE = "modules.json"
        private const val BUILTIN_STATES_FILE = "builtin_states.json"  // Built-in module enabled states
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
         * Releases the singleton instance.
         * Typically invoked from Application.onTerminate or tests.
         */
        fun release() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
    
    // Use a forgiving Gson configuration
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
        .setLenient()  // Allow non-standard JSON
        .serializeNulls()  // Serialize null values
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
        .registerTypeAdapter(ModuleSourceType::class.java, enumDeserializer(ModuleSourceType.CUSTOM))
        .create()
    private val saveMutex = Mutex()
    private val modulesDir: File by lazy {
        File(context.filesDir, MODULES_DIR).apply { mkdirs() }
    }
    
    // Module list state
    private val _modules = MutableStateFlow<List<ExtensionModule>>(emptyList())
    val modules: StateFlow<List<ExtensionModule>> = _modules.asStateFlow()
    
    // Built-in modules
    private val _builtInModules = MutableStateFlow<List<ExtensionModule>>(emptyList())
    val builtInModules: StateFlow<List<ExtensionModule>> = _builtInModules.asStateFlow()
    
    // Cached combined module list (rebuilt only when _modules or _builtInModules change)
    @Volatile
    private var _allModulesCache: List<ExtensionModule> = emptyList()
    
    init {
        // Load modules during initialization
        loadModules()
        loadBuiltInModules()
        rebuildAllModulesCache()
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
     * Load all user modules.
     */
    private fun loadModules() {
        try {
            val file = File(modulesDir, MODULES_FILE)
            if (file.exists()) {
                val json = file.readText()
                if (json.isBlank()) {
                    AppLogger.d(TAG, "Modules file is empty")
                    _modules.value = emptyList()
                    return
                }
                val type = object : TypeToken<List<ExtensionModule>>() {}.type
                val loadedModules: List<ExtensionModule>? = try {
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "JSON parsing failed, attempting recovery", e)
                    // Attempt recovery: back up the corrupted file and reset
                    val backupFile = File(modulesDir, "modules_backup_${System.currentTimeMillis()}.json")
                    file.copyTo(backupFile, overwrite = true)
                    AppLogger.i(TAG, "Corrupted modules backed up to: ${backupFile.name}")
                    null
                }
                _modules.value = loadedModules?.filterNotNull() ?: emptyList()
                AppLogger.d(TAG, "Loaded ${_modules.value.size} modules")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load modules", e)
            _modules.value = emptyList()
        }
    }
    
    /**
     * Load built-in modules.
     */
    private fun loadBuiltInModules() {
        val builtInStates = loadBuiltInStates()
        
        // Load standard built-in modules
        val standardModules = BuiltInModules.getAll()
        
        // Load built-in Chrome extension modules
        val chromeExtModules = try {
            BuiltInChromeExtensions.getAll(context)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load built-in Chrome extensions", e)
            emptyList()
        }
        
        _builtInModules.value = (standardModules + chromeExtModules).map { module ->
            // App-saved enable states
            val savedEnabled = builtInStates[module.id]
            if (savedEnabled != null) {
                module.copy(enabled = savedEnabled)
            } else {
                module
            }
        }
    }
    
    /**
     * Reload built-in modules after language changes.
     */
    fun reloadBuiltInModules() {
        loadBuiltInModules()
        rebuildAllModulesCache()
        AppLogger.d(TAG, "Reloaded built-in modules for language change")
    }
    
    /**
     * Load built-in module enable states.
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
            AppLogger.e(TAG, "Failed to load built-in states", e)
            emptyMap()
        }
    }
    
    /**
     * Persist built-in module enable states.
     */
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
     * Persist the module list.
     */
    private suspend fun saveModules() = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            try {
                val file = File(modulesDir, MODULES_FILE)
                val json = gson.toJson(_modules.value)
                file.writeText(json)
                AppLogger.d(TAG, "Saved ${_modules.value.size} modules")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to save modules", e)
            }
        }
    }
    
    /**
     * Return all modules (built-in + user).
     */
    fun getAllModules(): List<ExtensionModule> {
        return _allModulesCache
    }
    
    /**
     * Return enabled modules.
     */
    fun getEnabledModules(): List<ExtensionModule> {
        return getAllModules().filter { it.enabled }
    }
    
    /**
     * Find modules matching a URL.
     */
    fun getModulesForUrl(url: String): List<ExtensionModule> {
        return getEnabledModules().filter { it.matchesUrl(url) }
    }
    
    /**
     * Get modules by category.
     */
    fun getModulesByCategory(category: ModuleCategory): List<ExtensionModule> {
        return getAllModules().filter { it.category == category }
    }
    
    /**
     * Search modules.
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
     */
    suspend fun addModule(module: ExtensionModule): Result<ExtensionModule> {
        return try {
            // Verify.
            val errors = module.validate()
            if (errors.isNotEmpty()) {
                return Result.failure(IllegalArgumentException(errors.joinToString("\n")))
            }
            
            // Check is in.
            val existing = _modules.value.find { it.id == module.id }
            val newModule = if (existing != null) {
                // Update.
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
     */
    suspend fun updateModule(module: ExtensionModule): Result<ExtensionModule> {
        return addModule(module.copy(updatedAt = System.currentTimeMillis()))
    }
    
    /**
     */
    suspend fun deleteModule(moduleId: String): Result<Unit> {
        return try {
            _modules.value = _modules.value.filter { it.id != moduleId }
            rebuildAllModulesCache()
            saveModules()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete module", e)
            Result.failure(e)
        }
    }
    
    /**
     * use.
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
            
            // Check is is.
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
     * config.
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

    
    
    /**
     * single as.
     */
    suspend fun exportModule(moduleId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val module = getAllModules().find { it.id == moduleId }
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
            
            val fileName = "${module.name.replace(SAFE_FILENAME_REGEX, "_")}$MODULE_FILE_EXTENSION"
            val file = File(context.cacheDir, fileName)
            file.writeText(module.toJson())
            
            Result.success(file)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export module", e)
            Result.failure(e)
        }
    }
    
    /**
     * multiple as.
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
     * from.
     */
    suspend fun importModule(inputStream: InputStream): Result<ExtensionModule> = withContext(Dispatchers.IO) {
        try {
            val json = inputStream.bufferedReader().readText()
            val module = ExtensionModule.fromJson(json)
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errInvalidModuleFile))
            
            // Generate ID.
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
     * from.
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
     * from.
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
     */
    fun shareModule(moduleId: String): Intent? {
        val module = getAllModules().find { it.id == moduleId } ?: return null
        
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
     * to Downloads.
     * @return.
     */
    suspend fun exportModuleToDownloads(moduleId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val module = getAllModules().find { it.id == moduleId }
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
            
            val fileName = "${module.name.replace(SAFE_FILENAME_REGEX, "_")}$MODULE_FILE_EXTENSION"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            file.writeText(module.toJson())
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export module to Downloads", e)
            Result.failure(e)
        }
    }
    
    /**
     * to URI.
     * @param moduleId ID.
     * @param uri use Save URI.
     */
    suspend fun exportModuleToUri(moduleId: String, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val module = getAllModules().find { it.id == moduleId }
                ?: return@withContext Result.failure(IllegalArgumentException(Strings.errModuleNotFound))
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(module.toJson().toByteArray())
            } ?: return@withContext Result.failure(IllegalStateException(Strings.errCannotOpenOutputStream))
            
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to export module to URI", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get.
     */
    fun getModuleExportFileName(moduleId: String): String? {
        val module = getAllModules().find { it.id == moduleId } ?: return null
        return "${module.name.replace(SAFE_FILENAME_REGEX, "_")}$MODULE_FILE_EXTENSION"
    }
    
    
    /**
     * URL.
     * .
     */
    fun generateInjectionCode(url: String, runAt: ModuleRunTime): String {
        // Skip CHROME_EXTENSION and USERSCRIPT modules they are handled by their own
        // polyfill injection functions (injectChromeExtensionPolyfills / injectGreasemonkeyPolyfills)
        val matchingModules = getModulesForUrl(url).filter {
            it.runAt == runAt &&
            it.sourceType != ModuleSourceType.CHROME_EXTENSION &&
            it.sourceType != ModuleSourceType.USERSCRIPT
        }
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
     * ID.
     * .
     * @param url before URL.
     * @param runAt when.
     * @param moduleIds ID.
     */
    fun generateInjectionCodeForModules(url: String, runAt: ModuleRunTime, moduleIds: List<String>): String {
        if (moduleIds.isEmpty()) return ""
        
        val allModules = getAllModules()
        // not Check module.enabled as use use config.
        // use config in ID.
        // Skip CHROME_EXTENSION and USERSCRIPT modules they are handled by their own
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
     * IDGet.
     */
    fun getModuleById(moduleId: String): ExtensionModule? {
        return getAllModules().find { it.id == moduleId }
    }
    
    /**
     * ID Get.
     */
    fun getModulesByIds(moduleIds: List<String>): List<ExtensionModule> {
        val allModules = getAllModules()
        return moduleIds.mapNotNull { id -> allModules.find { it.id == id } }
    }
    
    /**
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
     * Get.
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
 */
data class ModuleStatistics(
    val totalCount: Int,
    val userCount: Int,
    val builtInCount: Int,
    val enabledCount: Int,
    val categoryStats: Map<ModuleCategory, Int>
)
