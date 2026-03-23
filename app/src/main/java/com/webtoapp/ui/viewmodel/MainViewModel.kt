package com.webtoapp.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.WebToAppApplication
import com.webtoapp.data.model.*
import com.webtoapp.data.repository.AppCategoryRepository
import com.webtoapp.ui.theme.ThemeManager
import com.webtoapp.util.BgmStorage
import com.webtoapp.util.HtmlStorage
import com.webtoapp.util.MediaStorage
import com.webtoapp.util.SplashStorage
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.util.IconStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * Main screen ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WebAppRepository = WebToAppApplication.repository
    private val categoryRepository: AppCategoryRepository = WebToAppApplication.categoryRepository

    // All apps list
    val webApps: StateFlow<List<WebApp>> = repository.allWebApps
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // All categories list
    val categories: StateFlow<List<AppCategory>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Currently selected category ID (null = all, -1 = uncategorized)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // Currently editing app
    private val _currentApp = MutableStateFlow<WebApp?>(null)
    val currentApp: StateFlow<WebApp?> = _currentApp.asStateFlow()

    // Edit state
    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    /**
     * Clean up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        // Cleanup Bitmap resources in edit state
        _editState.value.iconBitmap?.recycle()
        // Reset state
        _editState.value = EditState()
        _currentApp.value = null
    }

    val filteredApps: StateFlow<List<WebApp>> = combine(
        webApps,
        searchQuery,
        selectedCategoryId
    ) { apps, query, categoryId ->
        var filtered = apps
        
        // Category filter
        filtered = when (categoryId) {
            null -> filtered // All
            -1L -> filtered.filter { it.categoryId == null } // Uncategorized
            else -> filtered.filter { it.categoryId == categoryId } // Specified category
        }
        
        // Search filter
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }
        
        filtered
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Create new app
     */
    fun createNewApp() {
        _editState.value = EditState()
        _currentApp.value = null
        _uiState.value = UiState.Idle  // Reset UI state, avoid showing old error messages
    }

    /**
     * Edit existing app
     */
    fun editApp(webApp: WebApp) {
        _currentApp.value = webApp
        _uiState.value = UiState.Idle  // Reset UI state, avoid showing old error messages
        
        _editState.value = EditState(
            name = webApp.name,
            url = webApp.url,
            iconUri = webApp.iconPath?.let { Uri.parse(it) },
            savedIconPath = webApp.iconPath,  // Keep existing local path
            appType = webApp.appType,  // Keep app type
            mediaConfig = webApp.mediaConfig,  // Keep media config
            htmlConfig = webApp.htmlConfig,  // Keep HTML config
            activationEnabled = webApp.activationEnabled,
            activationCodes = webApp.activationCodes,
            activationCodeList = webApp.activationCodeList,
            activationRequireEveryTime = webApp.activationRequireEveryTime,
            adsEnabled = webApp.adsEnabled,
            adConfig = webApp.adConfig ?: AdConfig(),
            announcementEnabled = webApp.announcementEnabled,
            announcement = webApp.announcement ?: Announcement(),
            adBlockEnabled = webApp.adBlockEnabled,
            adBlockRules = webApp.adBlockRules,
            webViewConfig = webApp.webViewConfig,
            splashEnabled = webApp.splashEnabled,
            splashConfig = webApp.splashConfig ?: SplashConfig(),
            splashMediaUri = webApp.splashConfig?.mediaPath?.let { Uri.parse(it) },
            savedSplashPath = webApp.splashConfig?.mediaPath,
            bgmEnabled = webApp.bgmEnabled,
            bgmConfig = webApp.bgmConfig ?: BgmConfig(),
            apkExportConfig = webApp.apkExportConfig ?: ApkExportConfig(),
            themeType = webApp.themeType,
            translateEnabled = webApp.translateEnabled,
            translateConfig = webApp.translateConfig ?: TranslateConfig(),
            extensionModuleEnabled = webApp.extensionModuleIds.isNotEmpty(),
            extensionModuleIds = webApp.extensionModuleIds.toSet(),
            autoStartConfig = webApp.autoStartConfig,
            forcedRunConfig = webApp.forcedRunConfig,
            blackTechConfig = webApp.blackTechConfig,
            disguiseConfig = webApp.disguiseConfig
        )
    }

    /**
     * Update edit state
     */
    fun updateEditState(update: EditState.() -> EditState) {
        _editState.value = _editState.value.update()
    }

    /**
     * Handle selected icon URI - copy to private directory for persistence
     */
    fun handleIconSelected(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                IconStorage.saveIconFromUri(getApplication(), uri)
            }
            if (savedPath != null) {
                // Delete old icon (if exists and different)
                val oldPath = _editState.value.savedIconPath
                if (oldPath != null && oldPath != savedPath) {
                    withContext(Dispatchers.IO) {
                        IconStorage.deleteIcon(oldPath)
                    }
                }
                // Update state, save local path
                _editState.value = _editState.value.copy(
                    iconUri = Uri.parse(savedPath),
                    savedIconPath = savedPath
                )
            } else {
                _uiState.value = UiState.Error("Failed to save icon, please retry")
            }
        }
    }

    /**
     * Handle selected splash media - copy to private directory for persistence
     */
    fun handleSplashMediaSelected(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                SplashStorage.saveMediaFromUri(getApplication(), uri, isVideo)
            }
            if (savedPath != null) {
                // Delete old media file (if exists and different)
                val oldPath = _editState.value.savedSplashPath
                if (oldPath != null && oldPath != savedPath) {
                    withContext(Dispatchers.IO) {
                        SplashStorage.deleteMedia(oldPath)
                    }
                }
                // Update state
                val newType = if (isVideo) SplashType.VIDEO else SplashType.IMAGE
                _editState.value = _editState.value.copy(
                    splashMediaUri = Uri.parse(savedPath),
                    savedSplashPath = savedPath,
                    splashConfig = _editState.value.splashConfig.copy(type = newType)
                )
            } else {
                _uiState.value = UiState.Error("Failed to save splash, please retry")
            }
        }
    }

    /**
     * Clear splash media
     */
    fun clearSplashMedia() {
        viewModelScope.launch {
            val oldPath = _editState.value.savedSplashPath
            if (oldPath != null) {
                withContext(Dispatchers.IO) {
                    SplashStorage.deleteMedia(oldPath)
                }
            }
            _editState.value = _editState.value.copy(
                splashMediaUri = null,
                savedSplashPath = null
            )
        }
    }

    /**
     * Save app
     */
    fun saveApp() {
        viewModelScope.launch {
            val state = _editState.value

            // Verify
            if (!validateInput(state)) return@launch

            _uiState.value = UiState.Loading

            try {
                // Use persistent local path
                val iconPath = state.savedIconPath ?: state.iconUri?.toString()
                
                // Build splash config
                val splashConfig = if (state.splashEnabled && state.savedSplashPath != null) {
                    state.splashConfig.copy(mediaPath = state.savedSplashPath)
                } else {
                    null
                }
                
                // Build BGM config
                val bgmConfig = if (state.bgmEnabled && state.bgmConfig.playlist.isNotEmpty()) {
                    state.bgmConfig
                } else {
                    null
                }

                // Build APK export config (only save when has custom values)
                val apkExportConfig = state.apkExportConfig.let { config ->
                    if (config.customPackageName.isNullOrBlank() && 
                        config.customVersionName.isNullOrBlank() && 
                        config.customVersionCode == null) {
                        null
                    } else {
                        config
                    }
                }

                // Build translate config
                val translateConfig = if (state.translateEnabled) state.translateConfig else null

                // Get current theme type from ThemeManager
                val themeManager = ThemeManager.getInstance(getApplication())
                val currentThemeType = themeManager.themeTypeFlow.first().name

                // Convert new format activation codes to string list (for compatibility)
                val activationCodeStrings = state.activationCodeList.map { it.toJson() } + state.activationCodes
                
                // Build extension module ID list
                val extensionModuleIds = if (state.extensionModuleEnabled) {
                    state.extensionModuleIds.toList()
                } else {
                    emptyList()
                }
                
                val webApp = _currentApp.value?.copy(
                    name = state.name,
                    url = normalizeUrl(state.url),
                    iconPath = iconPath,
                    appType = state.appType,  // Keep app type
                    mediaConfig = state.mediaConfig,  // Keep media config
                    htmlConfig = state.htmlConfig,  // Keep HTML config
                    activationEnabled = state.activationEnabled,
                    activationCodes = activationCodeStrings,
                    activationCodeList = state.activationCodeList,
                    activationRequireEveryTime = state.activationRequireEveryTime,
                    adsEnabled = state.adsEnabled,
                    adConfig = state.adConfig,
                    announcementEnabled = state.announcementEnabled,
                    announcement = state.announcement,
                    adBlockEnabled = state.adBlockEnabled,
                    adBlockRules = state.adBlockRules,
                    webViewConfig = state.webViewConfig,
                    splashEnabled = state.splashEnabled,
                    splashConfig = splashConfig,
                    bgmEnabled = state.bgmEnabled,
                    bgmConfig = bgmConfig,
                    apkExportConfig = apkExportConfig,
                    themeType = currentThemeType,
                    translateEnabled = state.translateEnabled,
                    translateConfig = translateConfig,
                    extensionModuleIds = extensionModuleIds,
                    autoStartConfig = state.autoStartConfig,
                    forcedRunConfig = state.forcedRunConfig,
                    blackTechConfig = state.blackTechConfig,
                    disguiseConfig = state.disguiseConfig
                ) ?: run {
                    // Convert new format activation codes to string list (for compatibility)
                    val activationCodeStrings = state.activationCodeList.map { it.toJson() } + state.activationCodes
                    
                    // If a specific category is selected (not all, not uncategorized), auto-categorize to it
                    val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }
                    
                    WebApp(
                        name = state.name,
                        url = normalizeUrl(state.url),
                        iconPath = iconPath,
                        appType = AppType.WEB,  // Default type
                        activationEnabled = state.activationEnabled,
                        activationCodes = activationCodeStrings,
                        activationCodeList = state.activationCodeList,
                        activationRequireEveryTime = state.activationRequireEveryTime,
                        adsEnabled = state.adsEnabled,
                        adConfig = state.adConfig,
                        announcementEnabled = state.announcementEnabled,
                        announcement = state.announcement,
                        adBlockEnabled = state.adBlockEnabled,
                        adBlockRules = state.adBlockRules,
                        webViewConfig = state.webViewConfig,
                        splashEnabled = state.splashEnabled,
                        splashConfig = splashConfig,
                        bgmEnabled = state.bgmEnabled,
                        bgmConfig = bgmConfig,
                        apkExportConfig = apkExportConfig,
                        themeType = currentThemeType,
                        translateEnabled = state.translateEnabled,
                        translateConfig = translateConfig,
                        extensionModuleIds = extensionModuleIds,
                        autoStartConfig = state.autoStartConfig,
                        forcedRunConfig = state.forcedRunConfig,
                        blackTechConfig = state.blackTechConfig,
                        disguiseConfig = state.disguiseConfig,
                        categoryId = categoryId
                    )
                }

                if (_currentApp.value != null) {
                    repository.updateWebApp(webApp)
                } else {
                    repository.createWebApp(webApp)
                }

                _uiState.value = UiState.Success("App saved successfully")
                resetEditState()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Save failed")
            }
        }
    }

    /**
     * Delete app
     */
    fun deleteApp(webApp: WebApp) {
        viewModelScope.launch {
            try {
                repository.deleteWebApp(webApp)
                _uiState.value = UiState.Success("App deleted")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Delete failed")
            }
        }
    }

    /**
     * Search
     */
    fun search(query: String) {
        _searchQuery.value = query
    }

    /**
     * Reset UI state
     */
    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    /**
     * Reset edit state
     */
    private fun resetEditState() {
        _editState.value = EditState()
        _currentApp.value = null
    }

    /**
     * Validate input
     */
    private fun validateInput(state: EditState): Boolean {
        return when {
            state.name.isBlank() -> {
                _uiState.value = UiState.Error("Please enter app name")
                false
            }
            // Only WEB type needs URL validation
            state.appType == AppType.WEB && state.url.isBlank() -> {
                _uiState.value = UiState.Error("Please enter website URL")
                false
            }
            state.appType == AppType.WEB && !isValidUrl(state.url) -> {
                _uiState.value = UiState.Error("Please enter a valid URL")
                false
            }
            // HTML type needs HTML files
            state.appType == AppType.HTML && (state.htmlConfig?.files?.isEmpty() != false) -> {
                _uiState.value = UiState.Error("Please select HTML file")
                false
            }
            // IMAGE/VIDEO type needs media file path
            (state.appType == AppType.IMAGE || state.appType == AppType.VIDEO) && state.url.isBlank() -> {
                _uiState.value = UiState.Error("Media file path cannot be empty")
                false
            }
            else -> true
        }
    }

    /**
     * Validate URL
     * Allow user input URL with or without protocol
     */
    private fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        return try {
            // If has protocol, parse and validate directly
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val uri = Uri.parse(trimmed)
                !uri.host.isNullOrBlank()
            } else {
                // No protocol, only check if contains domain format (at least one dot)
                trimmed.contains(".") && !trimmed.contains(" ")
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Normalize URL
     * Do not auto-complete protocol, keep user original input
     */
    private fun normalizeUrl(url: String): String {
        return url.trim()
    }
    
    /**
     * Save media app (image/video to APP)
     */
    fun saveMediaApp(
        name: String,
        appType: AppType,
        mediaUri: Uri?,
        mediaConfig: MediaConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                val isVideo = appType == AppType.VIDEO
                
                // Get current theme type from ThemeManager
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // Save icon (if exists)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // Save media file
                val savedMediaPath = mediaUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        MediaStorage.saveMedia(context, uri, isVideo)
                    }
                }
                
                if (savedMediaPath == null) {
                    _uiState.value = UiState.Error("Failed to save media file")
                    return@launch
                }
                
                // If a specific category is selected (not all, not uncategorized), auto-categorize to it
                val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }
                
                val webApp = WebApp(
                    name = name.ifBlank { if (isVideo) "Video App" else "Image App" },
                    url = savedMediaPath,
                    iconPath = savedIconPath,
                    appType = appType,
                    mediaConfig = mediaConfig?.copy(mediaPath = savedMediaPath),
                    activationEnabled = false,
                    activationCodes = emptyList(),
                    activationCodeList = emptyList(),
                    bgmEnabled = false,
                    bgmConfig = BgmConfig(),
                    themeType = currentThemeType,
                    categoryId = categoryId
                )
                
                // Save to database
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success("Media app created successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Creation failed: ${e.message}")
            }
        }
    }
    
    /**
     * Save media gallery app (multiple images/videos)
     */
    fun saveGalleryApp(
        name: String,
        galleryConfig: GalleryConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                
                // Get current theme type from ThemeManager
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // Save icon (if exists)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                if (galleryConfig == null || galleryConfig.items.isEmpty()) {
                    _uiState.value = UiState.Error("Please add at least one media file")
                    return@launch
                }
                
                // If a specific category is selected (not all, not uncategorized), auto-categorize to it
                val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }
                
                val webApp = WebApp(
                    name = name.ifBlank { "Media Gallery" },
                    url = "",  // Gallery app does not need URL
                    iconPath = savedIconPath,
                    appType = AppType.GALLERY,
                    galleryConfig = galleryConfig,
                    activationEnabled = false,
                    activationCodes = emptyList(),
                    activationCodeList = emptyList(),
                    bgmEnabled = false,
                    bgmConfig = BgmConfig(),
                    themeType = currentThemeType,
                    categoryId = categoryId
                )
                
                // Save to database
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success("Media gallery created successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Creation failed: ${e.message}")
            }
        }
    }
    
    /**
     * Save HTML app (local HTML+CSS+JS to APP)
     * 
     * Important: Inline CSS and JS into HTML files to ensure proper loading in WebView
     */
    fun saveHtmlApp(
        name: String,
        htmlConfig: HtmlConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                
                // Get current theme type from ThemeManager
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // Save icon (if exists)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // Single HTML mode - use HtmlProjectProcessor to handle files
                val projectId = HtmlStorage.generateProjectId()
                val savedHtmlFiles = withContext(Dispatchers.IO) {
                    val files = htmlConfig?.files ?: emptyList()
                    
                    // Categorize files
                    val htmlFiles = files.filter { 
                        it.type == com.webtoapp.data.model.HtmlFileType.HTML || 
                        it.name.endsWith(".html", ignoreCase = true) || 
                        it.name.endsWith(".htm", ignoreCase = true)
                    }
                    val cssFiles = files.filter { 
                        it.type == com.webtoapp.data.model.HtmlFileType.CSS || 
                        it.name.endsWith(".css", ignoreCase = true)
                    }
                    val jsFiles = files.filter { 
                        it.type == com.webtoapp.data.model.HtmlFileType.JS || 
                        it.name.endsWith(".js", ignoreCase = true)
                    }
                    val otherFiles = files.filter { file ->
                        file !in htmlFiles && file !in cssFiles && file !in jsFiles
                    }
                    
                    android.util.Log.d("MainViewModel", "HTML file classification: HTML=${htmlFiles.size}, CSS=${cssFiles.size}, JS=${jsFiles.size}, Other=${otherFiles.size}")
                    
                    // Read CSS content
                    val cssContent = cssFiles.mapNotNull { cssFile ->
                        try {
                            val file = java.io.File(cssFile.path)
                            if (file.exists() && file.canRead()) {
                                com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(file, null)
                            } else null
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "Failed to read CSS file: ${cssFile.path}", e)
                            null
                        }
                    }.joinToString("\n\n")
                    
                    // Read JS content
                    val jsContent = jsFiles.mapNotNull { jsFile ->
                        try {
                            val file = java.io.File(jsFile.path)
                            if (file.exists() && file.canRead()) {
                                com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(file, null)
                            } else null
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "Failed to read JS file: ${jsFile.path}", e)
                            null
                        }
                    }.joinToString("\n\n")
                    
                    android.util.Log.d("MainViewModel", "CSS content length: ${cssContent.length}, JS content length: ${jsContent.length}")
                    
                    // Handle HTML files, inline CSS and JS
                    val processedHtmlFiles = htmlFiles.mapNotNull { htmlFile ->
                        try {
                            val sourceFile = java.io.File(htmlFile.path)
                            if (!sourceFile.exists() || !sourceFile.canRead()) {
                                android.util.Log.e("MainViewModel", "HTML file does not exist or cannot be read: ${htmlFile.path}")
                                return@mapNotNull null
                            }
                            
                            // Read HTML content
                            var htmlContent = com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(sourceFile, null)
                            
                            // Use HtmlProjectProcessor to process HTML content (inline CSS/JS)
                            htmlContent = com.webtoapp.util.HtmlProjectProcessor.processHtmlContent(
                                htmlContent = htmlContent,
                                cssContent = cssContent.takeIf { it.isNotBlank() },
                                jsContent = jsContent.takeIf { it.isNotBlank() },
                                fixPaths = true
                            )
                            
                            // Save processed HTML file
                            val savedPath = HtmlStorage.saveProcessedHtml(
                                context, htmlContent, htmlFile.name, projectId
                            )
                            
                            if (savedPath != null) {
                                android.util.Log.d("MainViewModel", "HTML file saved (inlined CSS/JS): ${htmlFile.name}")
                                htmlFile.copy(path = savedPath)
                            } else {
                                android.util.Log.e("MainViewModel", "Cannot save processed HTML file: ${htmlFile.name}")
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "Failed to process HTML file: ${htmlFile.path}", e)
                            null
                        }
                    }
                    
                    // Save other files (images, fonts, etc.)
                    val savedOtherFiles = otherFiles.mapNotNull { file ->
                        val savedPath = HtmlStorage.saveFromTempFile(
                            context, file.path, file.name, projectId
                        )
                        if (savedPath != null) {
                            file.copy(path = savedPath)
                        } else null
                    }
                    
                    // Return all saved files (only HTML and others, CSS/JS already inlined)
                    processedHtmlFiles + savedOtherFiles
                }
                
                // 检查是否有成功保存的 HTML 文件
                if (savedHtmlFiles.none { it.type == com.webtoapp.data.model.HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) }) {
                    android.util.Log.e("MainViewModel", "No HTML files were saved successfully. savedHtmlFiles=$savedHtmlFiles")
                    _uiState.value = UiState.Error("保存失败：无法处理 HTML 文件，请检查文件是否有效")
                    // 清理已创建的项目目录
                    withContext(Dispatchers.IO) {
                        HtmlStorage.deleteProject(context, projectId)
                    }
                    return@launch
                }
                
                val savedHtmlConfig = htmlConfig?.copy(
                    projectId = projectId,
                    files = savedHtmlFiles
                )
                
                android.util.Log.d("MainViewModel", "HTML app saved successfully: projectId=$projectId, files=${savedHtmlFiles.size}")
                
                // Cleanup temp files
                withContext(Dispatchers.IO) {
                    HtmlStorage.clearTempFiles(context)
                }
                
                // If a specific category is selected (not all, not uncategorized), auto-categorize to it
                val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }
                
                val webApp = WebApp(
                    name = name.ifBlank { "HTML App" },
                    url = "",
                    iconPath = savedIconPath,
                    appType = AppType.HTML,
                    htmlConfig = savedHtmlConfig,
                    activationEnabled = false,
                    activationCodes = emptyList(),
                    activationCodeList = emptyList(),
                    bgmEnabled = false,
                    bgmConfig = BgmConfig(),
                    themeType = currentThemeType,
                    categoryId = categoryId
                )
                
                // Save to database
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success("HTML app created successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Creation failed: ${e.message}")
            }
        }
    }
    
    /**
     * Save frontend project app (Vue/React/Node.js etc)
     * 
     * Save built static files as HTML app
     */
    fun saveFrontendApp(
        name: String,
        outputPath: String,
        iconUri: Uri?,
        framework: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                
                // Get current theme type from ThemeManager
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // Save icon (if exists)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // Generate project ID
                val projectId = HtmlStorage.generateProjectId()
                
                // Copy build output to app storage
                val savedFiles = withContext(Dispatchers.IO) {
                    val outputDir = java.io.File(outputPath)
                    if (!outputDir.exists() || !outputDir.isDirectory) {
                        throw Exception("Build output directory not found: $outputPath")
                    }
                    
                    val files = mutableListOf<com.webtoapp.data.model.HtmlFile>()
                    
                    // Recursively copy all files
                    outputDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(outputDir).path
                            val savedPath = HtmlStorage.saveFromTempFile(
                                context, file.absolutePath, relativePath, projectId
                            )
                            if (savedPath != null) {
                                val fileType = when {
                                    file.name.endsWith(".html", ignoreCase = true) ||
                                    file.name.endsWith(".htm", ignoreCase = true) -> 
                                        com.webtoapp.data.model.HtmlFileType.HTML
                                    file.name.endsWith(".css", ignoreCase = true) -> 
                                        com.webtoapp.data.model.HtmlFileType.CSS
                                    file.name.endsWith(".js", ignoreCase = true) -> 
                                        com.webtoapp.data.model.HtmlFileType.JS
                                    else -> com.webtoapp.data.model.HtmlFileType.OTHER
                                }
                                files.add(com.webtoapp.data.model.HtmlFile(
                                    name = relativePath,
                                    path = savedPath,
                                    type = fileType
                                ))
                            }
                        }
                    }
                    
                    files
                }
                
                // Create HTML config
                val htmlConfig = HtmlConfig(
                    projectId = projectId,
                    files = savedFiles,
                    enableJavaScript = true,
                    enableLocalStorage = true
                )
                
                // If a specific category is selected (not all, not uncategorized), auto-categorize to it
                val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }
                
                val webApp = WebApp(
                    name = name.ifBlank { "$framework App" },
                    url = "",
                    iconPath = savedIconPath,
                    appType = AppType.FRONTEND,
                    htmlConfig = htmlConfig,
                    activationEnabled = false,
                    activationCodes = emptyList(),
                    activationCodeList = emptyList(),
                    bgmEnabled = false,
                    bgmConfig = BgmConfig(),
                    themeType = currentThemeType,
                    categoryId = categoryId
                )
                
                // Save to database
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success("$framework app created successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Creation failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update frontend project app
     */
    fun updateFrontendApp(
        appId: Long,
        name: String,
        outputPath: String?,
        iconUri: Uri?,
        framework: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                val existingApp = withContext(Dispatchers.IO) {
                    repository.getWebAppById(appId).first()
                } ?: throw Exception("App not found")
                
                // Save icon (if new icon)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                } ?: existingApp.iconPath
                
                // If there is new build output, copy files
                val htmlConfig = if (!outputPath.isNullOrEmpty()) {
                    val projectId = existingApp.htmlConfig?.projectId ?: HtmlStorage.generateProjectId()
                    
                    val savedFiles = withContext(Dispatchers.IO) {
                        val outputDir = java.io.File(outputPath)
                        if (!outputDir.exists() || !outputDir.isDirectory) {
                            throw Exception("Build output directory not found: $outputPath")
                        }
                        
                        val files = mutableListOf<com.webtoapp.data.model.HtmlFile>()
                        
                        outputDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relativePath = file.relativeTo(outputDir).path
                                val savedPath = HtmlStorage.saveFromTempFile(
                                    context, file.absolutePath, relativePath, projectId
                                )
                                if (savedPath != null) {
                                    val fileType = when {
                                        file.name.endsWith(".html", ignoreCase = true) ||
                                        file.name.endsWith(".htm", ignoreCase = true) -> 
                                            com.webtoapp.data.model.HtmlFileType.HTML
                                        file.name.endsWith(".css", ignoreCase = true) -> 
                                            com.webtoapp.data.model.HtmlFileType.CSS
                                        file.name.endsWith(".js", ignoreCase = true) -> 
                                            com.webtoapp.data.model.HtmlFileType.JS
                                        else -> com.webtoapp.data.model.HtmlFileType.OTHER
                                    }
                                    files.add(com.webtoapp.data.model.HtmlFile(
                                        name = relativePath,
                                        path = savedPath,
                                        type = fileType
                                    ))
                                }
                            }
                        }
                        
                        files
                    }
                    
                    HtmlConfig(
                        projectId = projectId,
                        files = savedFiles,
                        enableJavaScript = true,
                        enableLocalStorage = true
                    )
                } else {
                    existingApp.htmlConfig
                }
                
                val updatedApp = existingApp.copy(
                    name = name.ifBlank { existingApp.name },
                    iconPath = savedIconPath,
                    htmlConfig = htmlConfig,
                    updatedAt = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.IO) {
                    repository.updateWebApp(updatedApp)
                }
                
                _uiState.value = UiState.Success("Frontend project updated successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Update failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update media app (image/video)
     */
    fun updateMediaApp(
        appId: Long,
        name: String,
        appType: AppType,
        mediaUri: Uri?,
        mediaConfig: MediaConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                val existingApp = withContext(Dispatchers.IO) {
                    repository.getWebAppById(appId).first()
                } ?: throw Exception("App not found")
                
                val isVideo = appType == AppType.VIDEO
                
                // Save icon (if new icon)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                } ?: existingApp.iconPath
                
                // Save media file (if new media)
                val savedMediaPath = mediaUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        MediaStorage.saveMedia(context, uri, isVideo)
                    }
                } ?: existingApp.url
                
                val updatedApp = existingApp.copy(
                    name = name.ifBlank { existingApp.name },
                    url = savedMediaPath,
                    iconPath = savedIconPath,
                    appType = appType,
                    mediaConfig = mediaConfig?.copy(mediaPath = savedMediaPath) ?: existingApp.mediaConfig,
                    updatedAt = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.IO) {
                    repository.updateWebApp(updatedApp)
                }
                
                _uiState.value = UiState.Success("Media app updated successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Update failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update media gallery app
     */
    fun updateGalleryApp(
        appId: Long,
        name: String,
        galleryConfig: GalleryConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                val existingApp = withContext(Dispatchers.IO) {
                    repository.getWebAppById(appId).first()
                } ?: throw Exception("App not found")
                
                // Save icon (if new icon)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                } ?: existingApp.iconPath
                
                if (galleryConfig == null || galleryConfig.items.isEmpty()) {
                    _uiState.value = UiState.Error("Please add at least one media file")
                    return@launch
                }
                
                val updatedApp = existingApp.copy(
                    name = name.ifBlank { existingApp.name },
                    iconPath = savedIconPath,
                    galleryConfig = galleryConfig,
                    updatedAt = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.IO) {
                    repository.updateWebApp(updatedApp)
                }
                
                _uiState.value = UiState.Success("Media gallery updated successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Update failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update HTML app
     */
    fun updateHtmlApp(
        appId: Long,
        name: String,
        htmlConfig: HtmlConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                val existingApp = withContext(Dispatchers.IO) {
                    repository.getWebAppById(appId).first()
                } ?: throw Exception("App not found")
                
                // Save icon (if new icon)
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                } ?: existingApp.iconPath
                
                val updatedApp = existingApp.copy(
                    name = name.ifBlank { existingApp.name },
                    iconPath = savedIconPath,
                    htmlConfig = htmlConfig ?: existingApp.htmlConfig,
                    updatedAt = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.IO) {
                    repository.updateWebApp(updatedApp)
                }
                
                _uiState.value = UiState.Success("HTML app updated successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Update failed: ${e.message}")
            }
        }
    }
    
    // ==================== Category Management ====================
    
    /**
     * Select category
     * @param categoryId null = all, -1 = uncategorized, other = specified category ID
     */
    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }
    
    /**
     * Create category
     */
    fun createCategory(name: String, icon: String = "📁", color: String = "#6200EE") {
        viewModelScope.launch {
            try {
                val category = AppCategory(
                    name = name,
                    icon = icon,
                    color = color,
                    sortOrder = categories.value.size
                )
                categoryRepository.createCategory(category)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to create category: ${e.message}")
            }
        }
    }
    
    /**
     * Update category
     */
    fun updateCategory(category: AppCategory) {
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to update category: ${e.message}")
            }
        }
    }
    
    /**
     * Delete category
     * After deletion, apps under this category will become uncategorized
     */
    fun deleteCategory(category: AppCategory) {
        viewModelScope.launch {
            try {
                // Set apps under this category to uncategorized
                val appsInCategory = webApps.value.filter { it.categoryId == category.id }
                appsInCategory.forEach { app ->
                    repository.updateWebApp(app.copy(categoryId = null))
                }
                // Delete category
                categoryRepository.deleteCategory(category)
                // If current selection is this category, switch to all
                if (_selectedCategoryId.value == category.id) {
                    _selectedCategoryId.value = null
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete category: ${e.message}")
            }
        }
    }
    
    /**
     * Change app category
     */
    fun moveAppToCategory(webApp: WebApp, categoryId: Long?) {
        viewModelScope.launch {
            try {
                repository.updateWebApp(webApp.copy(categoryId = categoryId))
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Move failed: ${e.message}")
            }
        }
    }
}

/**
 * Edit state
 */
data class EditState(
    val name: String = "",
    val url: String = "",
    val iconUri: Uri? = null,           // For UI display
    val savedIconPath: String? = null,  // Persistent local file path
    val iconBitmap: Bitmap? = null,
    
    // App type (to distinguish WEB/HTML/IMAGE/VIDEO)
    val appType: AppType = AppType.WEB,
    
    // Media app config (IMAGE/VIDEO type)
    val mediaConfig: MediaConfig? = null,
    
    // HTML app config (HTML type)
    val htmlConfig: HtmlConfig? = null,

    // Activation code
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),  // Old format (for compatibility)
    val activationCodeList: List<com.webtoapp.core.activation.ActivationCode> = emptyList(),  // New format
    val activationRequireEveryTime: Boolean = false,  // Whether to require verification every time

    // Ad
    val adsEnabled: Boolean = false,
    val adConfig: AdConfig = AdConfig(),

    // Announcement
    val announcementEnabled: Boolean = false,
    val announcement: Announcement = Announcement(),

    // Ad blocking
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),

    // WebView config
    val webViewConfig: WebViewConfig = WebViewConfig(),

    // Splash screen
    val splashEnabled: Boolean = false,
    val splashConfig: SplashConfig = SplashConfig(),
    val splashMediaUri: Uri? = null,        // For UI display
    val savedSplashPath: String? = null,    // Persistent local file path
    
    // Background music
    val bgmEnabled: Boolean = false,
    val bgmConfig: BgmConfig = BgmConfig(),
    
    // APK export config (only effective when packaging APK)
    val apkExportConfig: ApkExportConfig = ApkExportConfig(),
    
    // Theme config (for exported app UI style)
    val themeType: String = "AURORA",
    
    // Web page auto-translate config
    val translateEnabled: Boolean = false,
    val translateConfig: TranslateConfig = TranslateConfig(),
    
    // Extension modules
    val extensionModuleEnabled: Boolean = false,
    val extensionModuleIds: Set<String> = emptySet(),
    
    // Auto-start config
    val autoStartConfig: AutoStartConfig? = null,
    
    // Forced run config
    val forcedRunConfig: com.webtoapp.core.forcedrun.ForcedRunConfig? = null,
    
    // Black tech feature config (independent module)
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,
    
    // App disguise config (independent module)
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null
)

/**
 * UI state
 */
sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}
