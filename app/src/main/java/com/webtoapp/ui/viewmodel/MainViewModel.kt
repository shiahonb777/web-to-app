package com.webtoapp.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.data.model.*
import com.webtoapp.data.repository.AppCategoryRepository
import com.webtoapp.ui.theme.ThemeManager
import com.webtoapp.util.HtmlStorage
import com.webtoapp.util.MediaStorage
import com.webtoapp.util.SplashStorage
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.util.IconStorage
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.pwa.PwaAnalyzer
import com.webtoapp.core.pwa.PwaAnalysisResult
import com.webtoapp.core.pwa.PwaAnalysisState
import com.webtoapp.core.pwa.PwaDataSource
import android.content.Context
import com.webtoapp.util.HtmlProjectHelper
import com.webtoapp.util.ensureWebUrlScheme
import com.webtoapp.util.isInsecureRemoteHttpUrl
import com.webtoapp.util.upgradeRemoteHttpToHttps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.net.URL

/**
 * Main screen ViewModel
 */
@OptIn(FlowPreview::class)
class MainViewModel(
    application: Application,
    private val repository: WebAppRepository,
    private val categoryRepository: AppCategoryRepository
) : AndroidViewModel(application) {

    // All apps list
    val webApps: StateFlow<List<WebApp>> = repository.allWebApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    // All categories list
    val categories: StateFlow<List<AppCategory>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
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
    
    // PWA Analysis
    private val _pwaAnalysisState = MutableStateFlow<PwaAnalysisState>(PwaAnalysisState.Idle)
    val pwaAnalysisState: StateFlow<PwaAnalysisState> = _pwaAnalysisState.asStateFlow()
    
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
        searchQuery.debounce(300),
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            activationDialogConfig = webApp.activationDialogConfig ?: com.webtoapp.data.model.ActivationDialogConfig(),
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
            extensionFabIcon = webApp.extensionFabIcon ?: "",
            autoStartConfig = webApp.autoStartConfig,
            forcedRunConfig = webApp.forcedRunConfig,
            blackTechConfig = webApp.blackTechConfig,
            disguiseConfig = webApp.disguiseConfig,
            deviceDisguiseConfig = webApp.deviceDisguiseConfig ?: com.webtoapp.core.disguise.DeviceDisguiseConfig()
        )
    }

    /**
     * Update edit state
     */
    fun updateEditState(update: EditState.() -> EditState) {
        _editState.value = _editState.value.update()
    }

    // ═══════════════════════════════════════════
    //  PWA 自动感知
    // ═══════════════════════════════════════════

    /**
     * 分析网站的 PWA 配置
     */
    fun analyzePwa(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _pwaAnalysisState.value = PwaAnalysisState.Analyzing
            try {
                val result = PwaAnalyzer.analyze(url)
                _pwaAnalysisState.value = if (result.errorMessage != null) {
                    PwaAnalysisState.Error(result.errorMessage)
                } else {
                    PwaAnalysisState.Success(result)
                }
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "PWA analysis error", e)
                _pwaAnalysisState.value = PwaAnalysisState.Error(
                    e.message ?: Strings.pwaAnalysisFailed
                )
            }
        }
    }

    /**
     * 应用 PWA 分析结果到 EditState
     */
    fun applyPwaResult(result: PwaAnalysisResult) {
        viewModelScope.launch {
            // 1. 填充名称
            result.suggestedName?.let { name ->
                if (name.isNotBlank() && _editState.value.name.isBlank()) {
                    updateEditState { copy(name = name) }
                }
            }

            // 2. 下载并设置图标
            result.suggestedIconUrl?.let { iconUrl ->
                try {
                    val savedPath = withContext(Dispatchers.IO) {
                        downloadAndSaveIcon(iconUrl)
                    }
                    if (savedPath != null) {
                        updateEditState { copy(savedIconPath = savedPath, iconUri = null) }
                        AppLogger.i("MainViewModel", "PWA icon downloaded: $savedPath")
                    }
                } catch (e: Exception) {
                    AppLogger.w("MainViewModel", "Failed to download PWA icon: ${e.message}")
                }
            }

            // 3. 主题色 → 状态栏颜色
            result.suggestedThemeColor?.let { color ->
                updateEditState {
                    copy(
                        webViewConfig = webViewConfig.copy(
                            statusBarColorMode = StatusBarColorMode.CUSTOM,
                            statusBarColor = color
                        )
                    )
                }
            }

            // 4. 屏幕方向
            result.suggestedOrientation?.let { orientation ->
                val mode = when (orientation.lowercase()) {
                    "portrait", "portrait-primary", "portrait-secondary" -> OrientationMode.PORTRAIT
                    "landscape", "landscape-primary", "landscape-secondary" -> OrientationMode.LANDSCAPE
                    "any", "natural" -> OrientationMode.AUTO
                    else -> null
                }
                mode?.let {
                    updateEditState {
                        copy(webViewConfig = webViewConfig.copy(orientationMode = it))
                    }
                }
            }

            // 5. 显示模式 → 全屏/隐藏工具栏
            result.suggestedDisplay?.let { display ->
                when (display.lowercase()) {
                    "fullscreen" -> updateEditState {
                        copy(webViewConfig = webViewConfig.copy(
                            hideToolbar = true,
                            fullscreenEnabled = true
                        ))
                    }
                    "standalone" -> updateEditState {
                        copy(webViewConfig = webViewConfig.copy(hideToolbar = true))
                    }
                    "minimal-ui" -> { /* keep default */ }
                    "browser" -> { /* keep default */ }
                }
            }

            // 6. PWA 离线支持 → 如果来源是 manifest，自动启用
            if (result.source == PwaDataSource.MANIFEST) {
                updateEditState {
                    copy(webViewConfig = webViewConfig.copy(pwaOfflineEnabled = true))
                }
            }

            // 7. Deep Link → 自动推断域名
            val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, _editState.value.url)
            if (hosts.isNotEmpty()) {
                updateEditState {
                    copy(apkExportConfig = apkExportConfig.copy(
                        deepLinkEnabled = true,
                        customDeepLinkHosts = hosts
                    ))
                }
            }

            // 8. start_url → 替换 URL（如与用户输入不同）
            result.startUrl?.let { startUrl ->
                val currentUrl = _editState.value.url.trim()
                if (startUrl != currentUrl && startUrl.isNotBlank()) {
                    // 只在 start_url 是当前域名下的 URL 时替换
                    val currentHost = PwaAnalyzer.extractHost(currentUrl)
                    val startHost = PwaAnalyzer.extractHost(startUrl)
                    if (currentHost == startHost) {
                        updateEditState { copy(url = startUrl) }
                    }
                }
            }
        }
    }

    /**
     * 重置 PWA 分析状态
     */
    fun resetPwaState() {
        _pwaAnalysisState.value = PwaAnalysisState.Idle
    }

    /**
     * 下载图标并保存到私有目录
     */
    private fun downloadAndSaveIcon(iconUrl: String): String? {
        return try {
            val conn = URL(iconUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
            try {
                if (conn.responseCode !in 200..299) return null
                val bitmap = BitmapFactory.decodeStream(conn.inputStream) ?: return null
                IconStorage.saveIconFromBitmap(getApplication(), bitmap)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            AppLogger.e("MainViewModel", "Icon download failed: ${e.message}")
            null
        }
    }

    /**
     * Handle selected icon URI - copy to private directory for persistence
     */
    fun handleIconSelected(uri: Uri) {
        viewModelScope.launch {
            val oldPath = _editState.value.savedIconPath
            val savedPath = withContext(Dispatchers.IO) {
                val path = IconStorage.saveIconFromUri(getApplication(), uri)
                // Delete old icon in the same IO block to avoid extra dispatch
                if (path != null && oldPath != null && oldPath != path) {
                    IconStorage.deleteIcon(oldPath)
                }
                path
            }
            if (savedPath != null) {
                _editState.value = _editState.value.copy(
                    iconUri = Uri.parse(savedPath),
                    savedIconPath = savedPath
                )
            } else {
                _uiState.value = UiState.Error(Strings.failedSaveIcon)
            }
        }
    }

    /**
     * Handle selected splash media - copy to private directory for persistence
     */
    fun handleSplashMediaSelected(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            val oldPath = _editState.value.savedSplashPath
            val savedPath = withContext(Dispatchers.IO) {
                val path = SplashStorage.saveMediaFromUri(getApplication(), uri, isVideo)
                if (path != null && oldPath != null && oldPath != path) {
                    SplashStorage.deleteMedia(oldPath)
                }
                path
            }
            if (savedPath != null) {
                val newType = if (isVideo) SplashType.VIDEO else SplashType.IMAGE
                _editState.value = _editState.value.copy(
                    splashMediaUri = Uri.parse(savedPath),
                    savedSplashPath = savedPath,
                    splashConfig = _editState.value.splashConfig.copy(type = newType)
                )
            } else {
                _uiState.value = UiState.Error(Strings.failedSaveSplash)
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

                // Build extension module ID list
                // 直接保存用户选择的模块 ID，不依赖 extensionModuleEnabled 开关
                // 加载时 extensionModuleEnabled 会根据列表是否为空自动推断
                val extensionModuleIds = state.extensionModuleIds.toList()
                
                // Convert new format activation codes to string list (for compatibility)
                // Only include legacy (non-JSON) entries from activationCodes to avoid duplication
                val legacyCodeStrings = state.activationCodes.filter { !it.trimStart().startsWith("{") }
                val activationCodeStrings = state.activationCodeList.map { it.toJson() } + legacyCodeStrings
                
                val webApp = _currentApp.value?.copy(
                    name = state.name,
                    url = normalizeUrl(state.url, state.appType, state.allowHttp),
                    iconPath = iconPath,
                    appType = state.appType,  // Keep app type
                    mediaConfig = state.mediaConfig,  // Keep media config
                    htmlConfig = state.htmlConfig,  // Keep HTML config
                    activationEnabled = state.activationEnabled,
                    activationCodes = activationCodeStrings,
                    activationCodeList = state.activationCodeList,
                    activationRequireEveryTime = state.activationRequireEveryTime,
                    activationDialogConfig = state.activationDialogConfig.let {
                        if (it.title.isBlank() && it.subtitle.isBlank() && it.inputLabel.isBlank() && it.buttonText.isBlank()) null else it
                    },
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
                    extensionFabIcon = state.extensionFabIcon.ifBlank { null },
                    autoStartConfig = state.autoStartConfig,
                    forcedRunConfig = state.forcedRunConfig,
                    blackTechConfig = state.blackTechConfig,
                    disguiseConfig = state.disguiseConfig,
                    deviceDisguiseConfig = state.deviceDisguiseConfig
                ) ?: run {
                    // If a specific category is selected (not all, not uncategorized), auto-categorize to it
                    val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }
                    
                    WebApp(
                        name = state.name,
                        url = normalizeUrl(state.url, AppType.WEB, state.allowHttp),
                        iconPath = iconPath,
                        appType = AppType.WEB,  // Default type
                        activationEnabled = state.activationEnabled,
                        activationCodes = activationCodeStrings,
                        activationCodeList = state.activationCodeList,
                        activationRequireEveryTime = state.activationRequireEveryTime,
                        activationDialogConfig = state.activationDialogConfig.let {
                            if (it.title.isBlank() && it.subtitle.isBlank() && it.inputLabel.isBlank() && it.buttonText.isBlank()) null else it
                        },
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
                        extensionFabIcon = state.extensionFabIcon.ifBlank { null },
                        autoStartConfig = state.autoStartConfig,
                        forcedRunConfig = state.forcedRunConfig,
                        blackTechConfig = state.blackTechConfig,
                        disguiseConfig = state.disguiseConfig,
                        deviceDisguiseConfig = state.deviceDisguiseConfig,
                        categoryId = categoryId
                    )
                }

                if (_currentApp.value != null) {
                    repository.updateWebApp(webApp)
                } else {
                    repository.createWebApp(webApp)
                }

                _uiState.value = UiState.Success(Strings.appSavedSuccessfully)
                resetEditState()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: Strings.saveFailed)
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
                _uiState.value = UiState.Success(Strings.appDeleted)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: Strings.deleteFailed)
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
     * 缓存式预览 — 先保存再打开
     *
     * 将当前编辑状态完整保存到数据库（与 saveApp() 完全一致的逻辑），
     * 然后返回保存后的 appId，由 UI 层通过 WebViewActivity.start() 打开。
     * 这样预览效果与用户真实打开完全一致，包括激活码、启动画面、BGM、公告等。
     *
     * @return 保存后的 appId，或 null 表示保存失败
     */
    suspend fun saveAndPreview(): Long? {
        val state = _editState.value
        
        // 基本校验（宽松版：只检查最基本的 URL/名称）
        if (state.name.isBlank() && state.url.isBlank()) return null
        
        return try {
            val iconPath = state.savedIconPath ?: state.iconUri?.toString()
            
            val splashConfig = if (state.splashEnabled && state.savedSplashPath != null) {
                state.splashConfig.copy(mediaPath = state.savedSplashPath)
            } else null
            
            val bgmConfig = if (state.bgmEnabled && state.bgmConfig.playlist.isNotEmpty()) {
                state.bgmConfig
            } else null
            
            val apkExportConfig = state.apkExportConfig.let { config ->
                if (config.customPackageName.isNullOrBlank() && 
                    config.customVersionName.isNullOrBlank() && 
                    config.customVersionCode == null) null else config
            }
            
            val translateConfig = if (state.translateEnabled) state.translateConfig else null
            val currentThemeType = getCurrentThemeType()
            val extensionModuleIds = state.extensionModuleIds.toList()
            
            val legacyCodeStrings = state.activationCodes.filter { !it.trimStart().startsWith("{") }
            val activationCodeStrings = state.activationCodeList.map { it.toJson() } + legacyCodeStrings
            
            val webApp = _currentApp.value?.copy(
                name = state.name.ifBlank { "Preview" },
                url = normalizeUrl(state.url, state.appType, state.allowHttp),
                iconPath = iconPath,
                appType = state.appType,
                mediaConfig = state.mediaConfig,
                htmlConfig = state.htmlConfig,
                activationEnabled = state.activationEnabled,
                activationCodes = activationCodeStrings,
                activationCodeList = state.activationCodeList,
                activationRequireEveryTime = state.activationRequireEveryTime,
                activationDialogConfig = state.activationDialogConfig.let {
                    if (it.title.isBlank() && it.subtitle.isBlank() && it.inputLabel.isBlank() && it.buttonText.isBlank()) null else it
                },
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
                extensionFabIcon = state.extensionFabIcon.ifBlank { null },
                autoStartConfig = state.autoStartConfig,
                forcedRunConfig = state.forcedRunConfig,
                blackTechConfig = state.blackTechConfig,
                disguiseConfig = state.disguiseConfig,
                deviceDisguiseConfig = state.deviceDisguiseConfig
            ) ?: run {
                val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }
                WebApp(
                    name = state.name.ifBlank { "Preview" },
                    url = normalizeUrl(state.url, state.appType, state.allowHttp),
                    iconPath = iconPath,
                    appType = state.appType,
                    activationEnabled = state.activationEnabled,
                    activationCodes = activationCodeStrings,
                    activationCodeList = state.activationCodeList,
                    activationRequireEveryTime = state.activationRequireEveryTime,
                    activationDialogConfig = state.activationDialogConfig.let {
                        if (it.title.isBlank() && it.subtitle.isBlank() && it.inputLabel.isBlank() && it.buttonText.isBlank()) null else it
                    },
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
                    extensionFabIcon = state.extensionFabIcon.ifBlank { null },
                    autoStartConfig = state.autoStartConfig,
                    forcedRunConfig = state.forcedRunConfig,
                    blackTechConfig = state.blackTechConfig,
                    disguiseConfig = state.disguiseConfig,
                    deviceDisguiseConfig = state.deviceDisguiseConfig,
                    categoryId = categoryId
                )
            }
            
            val savedAppId: Long
            if (_currentApp.value != null) {
                // 已有应用 → 更新
                repository.updateWebApp(webApp)
                savedAppId = webApp.id
                _currentApp.value = webApp
            } else {
                // 新建应用 → 插入，然后将 ViewModel 切换到编辑模式
                savedAppId = repository.createWebApp(webApp)
                val savedApp = webApp.copy(id = savedAppId)
                _currentApp.value = savedApp
            }
            
            savedAppId
        } catch (e: Exception) {
            AppLogger.e("MainViewModel", "saveAndPreview failed", e)
            null
        }
    }

    /**
     * Validate input
     */
    private fun validateInput(state: EditState): Boolean {
        return when {
            state.name.isBlank() -> {
                _uiState.value = UiState.Error(Strings.pleaseEnterAppName)
                false
            }
            // Only WEB type needs URL validation
            state.appType == AppType.WEB && state.url.isBlank() -> {
                _uiState.value = UiState.Error(Strings.pleaseEnterWebsiteUrl)
                false
            }
            state.appType == AppType.WEB && !isValidUrl(state.url) -> {
                _uiState.value = UiState.Error(Strings.pleaseEnterValidUrl)
                false
            }
            // WEB type with insecure HTTP URL needs user confirmation
            state.appType == AppType.WEB && isInsecureRemoteHttpUrl(state.url) && !state.allowHttp -> {
                _uiState.value = UiState.Error(Strings.insecureHttpWarning)
                false
            }
            // HTML type needs HTML files
            state.appType == AppType.HTML && (state.htmlConfig?.files?.isEmpty() != false) -> {
                _uiState.value = UiState.Error(Strings.pleaseSelectHtmlFile)
                false
            }
            // IMAGE/VIDEO type needs media file path
            (state.appType == AppType.IMAGE || state.appType == AppType.VIDEO) && state.url.isBlank() -> {
                _uiState.value = UiState.Error(Strings.mediaFilePathEmpty)
                false
            }
            else -> true
        }
    }

    /**
     * Validate URL
     * Allow user input URL with or without protocol
     * Supports: domains, localhost, IP addresses
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
                // No protocol, check if valid host format
                // Accept: domains (with dot), localhost, IP addresses
                val host = trimmed.split("/").first() // Remove path if any
                host.isNotBlank() && !host.contains(" ") &&
                    (host.contains(".") || host == "localhost" || isIpAddress(host))
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if string is a valid IP address
     */
    private fun isIpAddress(host: String): Boolean {
        val parts = host.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            part.toIntOrNull()?.let { it in 0..255 } ?: false
        }
    }
    
    /**
     * Normalize URL
     * Do not auto-complete protocol, keep user original input
     * Respect user's HTTP choice (don't auto-upgrade to HTTPS if user allows HTTP)
     */
    private fun normalizeUrl(url: String, appType: AppType, allowHttp: Boolean = false): String {
        val trimmed = url.trim()
        if (appType != AppType.WEB) return trimmed

        // If user explicitly allows HTTP, don't auto-upgrade to HTTPS
        return if (allowHttp && trimmed.startsWith("http://", ignoreCase = true)) {
            trimmed
        } else {
            val withScheme = ensureWebUrlScheme(trimmed)
            upgradeRemoteHttpToHttps(withScheme)
        }
    }
    
    // ==================== Common App Save/Update Helpers ====================

    /**
     * Get the current theme type name from ThemeManager.
     */
    private suspend fun getCurrentThemeType(): String {
        val themeManager = ThemeManager.getInstance(getApplication())
        return themeManager.themeTypeFlow.first().name
    }

    /**
     * Save icon from URI to persistent storage, returning the saved path.
     * Returns null if iconUri is null.
     */
    private suspend fun saveIconIfPresent(iconUri: Uri?): String? {
        return iconUri?.let { uri ->
            withContext(Dispatchers.IO) {
                IconStorage.saveIconFromUri(getApplication(), uri)
            }
        }
    }

    /**
     * Common create-app flow: wraps Loading/Success/Error state management,
     * theme resolution, icon saving, category assignment, and DB insert.
     *
     * @param typeName Display name for success/error messages (e.g. "Media", "Node.js")
     * @param iconUri  Optional icon URI to save
     * @param buildApp Lambda that receives (savedIconPath, currentThemeType, categoryId)
     *                 and returns the WebApp to insert. Return null to abort.
     */
    private fun createApp(
        typeName: String,
        iconUri: Uri?,
        buildApp: suspend (savedIconPath: String?, currentThemeType: String, categoryId: Long?) -> WebApp?
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val currentThemeType = getCurrentThemeType()
                val savedIconPath = saveIconIfPresent(iconUri)
                val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }

                val webApp = buildApp(savedIconPath, currentThemeType, categoryId)
                    ?: return@launch  // builder signalled abort (already set uiState)

                withContext(Dispatchers.IO) { repository.createWebApp(webApp) }
                _uiState.value = UiState.Success(Strings.appCreatedSuccessfully.replaceFirst("%s", typeName))
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Failed to save $typeName app", e)
                _uiState.value = UiState.Error(Strings.creationFailed.replaceFirst("%s", e.message ?: ""))
            }
        }
    }

    /**
     * Common update-app flow: wraps Loading/Success/Error state management,
     * existing-app lookup, icon saving, and DB update.
     *
     * @param appId    ID of the existing app to update
     * @param typeName Display name for success/error messages
     * @param iconUri  Optional new icon URI (null = keep existing)
     * @param applyUpdate Lambda that receives (existingApp, savedIconPath) and returns the updated WebApp.
     */
    private fun updateApp(
        appId: Long,
        typeName: String,
        iconUri: Uri?,
        applyUpdate: suspend (existingApp: WebApp, savedIconPath: String?) -> WebApp
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val existingApp = withContext(Dispatchers.IO) {
                    repository.getWebAppById(appId).first()
                } ?: throw Exception("App not found")

                val savedIconPath = saveIconIfPresent(iconUri) ?: existingApp.iconPath
                val updatedApp = applyUpdate(existingApp, savedIconPath)

                withContext(Dispatchers.IO) { repository.updateWebApp(updatedApp) }
                _uiState.value = UiState.Success(Strings.appUpdatedSuccessfully.replaceFirst("%s", typeName))
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Failed to update $typeName app", e)
                _uiState.value = UiState.Error(Strings.updateFailed.replaceFirst("%s", e.message ?: ""))
            }
        }
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
    ) = createApp("Media", iconUri) { savedIconPath, currentThemeType, categoryId ->
        val context = getApplication<Application>()
        val isVideo = appType == AppType.VIDEO
        val savedMediaPath = mediaUri?.let { uri ->
            withContext(Dispatchers.IO) { MediaStorage.saveMedia(context, uri, isVideo) }
        }
        if (savedMediaPath == null) {
            _uiState.value = UiState.Error(Strings.failedSaveMediaFile)
            return@createApp null
        }
        WebApp(
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
        if (galleryConfig == null || galleryConfig.items.isEmpty()) {
            _uiState.value = UiState.Error(Strings.pleaseAddMediaFile)
            return
        }
        createApp("Gallery", iconUri) { savedIconPath, currentThemeType, categoryId ->
            WebApp(
                name = name.ifBlank { "Media Gallery" },
                url = "",
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
        }
    }
    
    // HTML processing delegated to HtmlProjectHelper
    private suspend fun processAndSaveHtmlFiles(
        context: Context,
        files: List<HtmlFile>,
        projectId: String
    ): List<HtmlFile> = HtmlProjectHelper.processAndSaveFiles(context, files, projectId)

    private suspend fun copyBuildOutputToStorage(
        context: Context,
        outputPath: String,
        projectId: String
    ): List<HtmlFile> = HtmlProjectHelper.copyBuildOutputToStorage(context, outputPath, projectId)

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
    ) = createApp("HTML", iconUri) { savedIconPath, currentThemeType, categoryId ->
        val context = getApplication<Application>()
        val projectId = HtmlStorage.generateProjectId()
        val savedHtmlFiles = processAndSaveHtmlFiles(context, htmlConfig?.files ?: emptyList(), projectId)

        if (savedHtmlFiles.none { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) }) {
            AppLogger.e("MainViewModel", "No HTML files were saved successfully. savedHtmlFiles=$savedHtmlFiles")
            withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, projectId) }
            _uiState.value = UiState.Error(Strings.saveFailedCannotProcessHtml)
            return@createApp null
        }

        val savedHtmlConfig = htmlConfig?.copy(projectId = projectId, files = savedHtmlFiles)
        AppLogger.d("MainViewModel", "HTML app saved successfully: projectId=$projectId, files=${savedHtmlFiles.size}")
        withContext(Dispatchers.IO) { HtmlStorage.clearTempFiles(context) }

        WebApp(
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
    }
    
    /**
     * Save ZIP-imported HTML app
     * 
     * Unlike saveHtmlApp, this preserves the full directory structure (no CSS/JS inlining)
     * because ZIP projects may contain complex relative path references between files.
     */
    fun saveZipHtmlApp(
        name: String,
        extractedDir: String,
        entryFile: String,
        iconUri: Uri?,
        enableJavaScript: Boolean = true,
        enableLocalStorage: Boolean = true,
        landscapeMode: Boolean = false
    ) = createApp("HTML", iconUri) { savedIconPath, currentThemeType, categoryId ->
        val context = getApplication<Application>()
        val projectId = HtmlStorage.generateProjectId()
        val savedFiles = copyBuildOutputToStorage(context, extractedDir, projectId)

        if (savedFiles.none { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) }) {
            AppLogger.e("MainViewModel", "No HTML files found in ZIP project")
            withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, projectId) }
            _uiState.value = UiState.Error(Strings.saveFailedNoHtmlInZip)
            return@createApp null
        }

        // Cleanup ZIP temp files
        withContext(Dispatchers.IO) { com.webtoapp.util.ZipProjectImporter.cleanupTempFiles(context) }
        AppLogger.d("MainViewModel", "ZIP HTML app saved: projectId=$projectId, files=${savedFiles.size}, entry=$entryFile")

        WebApp(
            name = name.ifBlank { "HTML App" },
            url = "",
            iconPath = savedIconPath,
            appType = AppType.HTML,
            htmlConfig = HtmlConfig(
                projectId = projectId,
                entryFile = entryFile,
                files = savedFiles,
                enableJavaScript = enableJavaScript,
                enableLocalStorage = enableLocalStorage,
                landscapeMode = landscapeMode
            ),
            activationEnabled = false,
            activationCodes = emptyList(),
            activationCodeList = emptyList(),
            bgmEnabled = false,
            bgmConfig = BgmConfig(),
            themeType = currentThemeType,
            categoryId = categoryId
        )
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
    ) = createApp(framework, iconUri) { savedIconPath, currentThemeType, categoryId ->
        val context = getApplication<Application>()
        val projectId = HtmlStorage.generateProjectId()
        val savedFiles = copyBuildOutputToStorage(context, outputPath, projectId)

        WebApp(
            name = name.ifBlank { "$framework App" },
            url = "",
            iconPath = savedIconPath,
            appType = AppType.FRONTEND,
            htmlConfig = HtmlConfig(
                projectId = projectId,
                files = savedFiles,
                enableJavaScript = true,
                enableLocalStorage = true
            ),
            activationEnabled = false,
            activationCodes = emptyList(),
            activationCodeList = emptyList(),
            bgmEnabled = false,
            bgmConfig = BgmConfig(),
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }
    
    /**
     * Save WordPress app
     */
    fun saveWordPressApp(
        name: String,
        wordpressConfig: WordPressConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = createApp("WordPress", iconUri) { savedIconPath, currentThemeType, categoryId ->
        WebApp(
            name = name.ifBlank { "WordPress App" },
            url = "",
            iconPath = savedIconPath,
            appType = AppType.WORDPRESS,
            wordpressConfig = wordpressConfig,
            activationEnabled = false,
            activationCodes = emptyList(),
            activationCodeList = emptyList(),
            bgmEnabled = false,
            bgmConfig = BgmConfig(),
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }
    
    /**
     * Save Node.js app
     */
    fun saveNodeJsApp(
        name: String,
        nodejsConfig: NodeJsConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = createApp("Node.js", iconUri) { savedIconPath, currentThemeType, categoryId ->
        WebApp(
            name = name.ifBlank { "Node.js App" },
            url = "",
            iconPath = savedIconPath,
            appType = AppType.NODEJS_APP,
            nodejsConfig = nodejsConfig,
            activationEnabled = false,
            activationCodes = emptyList(),
            activationCodeList = emptyList(),
            bgmEnabled = false,
            bgmConfig = BgmConfig(),
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }
    
    /**
     * Update Node.js app
     */
    fun updateNodeJsApp(
        appId: Long,
        name: String,
        nodejsConfig: NodeJsConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "Node.js", iconUri) { existingApp, savedIconPath ->
        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            nodejsConfig = nodejsConfig,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Save PHP app
     */
    fun savePhpApp(
        name: String,
        phpAppConfig: PhpAppConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = createApp("PHP", iconUri) { savedIconPath, currentThemeType, categoryId ->
        WebApp(
            name = name.ifBlank { "PHP App" },
            url = "",
            iconPath = savedIconPath,
            appType = AppType.PHP_APP,
            phpAppConfig = phpAppConfig,
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }
    
    /**
     * Save Python app
     */
    fun savePythonApp(
        name: String,
        pythonAppConfig: PythonAppConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = createApp("Python", iconUri) { savedIconPath, currentThemeType, categoryId ->
        WebApp(
            name = name.ifBlank { "Python App" },
            url = "",
            iconPath = savedIconPath,
            appType = AppType.PYTHON_APP,
            pythonAppConfig = pythonAppConfig,
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }
    
    /**
     * Save Go app
     */
    fun saveGoApp(
        name: String,
        goAppConfig: GoAppConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = createApp("Go", iconUri) { savedIconPath, currentThemeType, categoryId ->
        WebApp(
            name = name.ifBlank { "Go Service" },
            url = "",
            iconPath = savedIconPath,
            appType = AppType.GO_APP,
            goAppConfig = goAppConfig,
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }
    
    /**
     * Update PHP app
     */
    fun updatePhpApp(
        appId: Long,
        name: String,
        phpAppConfig: PhpAppConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "PHP", iconUri) { existingApp, savedIconPath ->
        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            phpAppConfig = phpAppConfig,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Update Python app
     */
    fun updatePythonApp(
        appId: Long,
        name: String,
        pythonAppConfig: PythonAppConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "Python", iconUri) { existingApp, savedIconPath ->
        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            pythonAppConfig = pythonAppConfig,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Update Go app
     */
    fun updateGoApp(
        appId: Long,
        name: String,
        goAppConfig: GoAppConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "Go", iconUri) { existingApp, savedIconPath ->
        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            goAppConfig = goAppConfig,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Save Multi-Web aggregator app
     */
    fun saveMultiWebApp(
        name: String,
        multiWebConfig: MultiWebConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = createApp("Multi-Site", iconUri) { savedIconPath, currentThemeType, categoryId ->
        WebApp(
            name = name.ifBlank { "Multi-Site App" },
            url = multiWebConfig.sites.firstOrNull()?.url ?: "",
            iconPath = savedIconPath,
            appType = AppType.MULTI_WEB,
            multiWebConfig = multiWebConfig,
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }
    
    /**
     * Update Multi-Web aggregator app
     */
    fun updateMultiWebApp(
        appId: Long,
        name: String,
        multiWebConfig: MultiWebConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "Multi-Site", iconUri) { existingApp, savedIconPath ->
        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            url = multiWebConfig.sites.firstOrNull()?.url ?: existingApp.url,
            iconPath = savedIconPath,
            multiWebConfig = multiWebConfig,
            updatedAt = System.currentTimeMillis()
        )
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
    ) = updateApp(appId, "Frontend", iconUri) { existingApp, savedIconPath ->
        val context = getApplication<Application>()
        // If there is new build output, copy files using shared helper
        val htmlConfig = if (!outputPath.isNullOrEmpty()) {
            val projectId = existingApp.htmlConfig?.projectId ?: HtmlStorage.generateProjectId()
            val savedFiles = copyBuildOutputToStorage(context, outputPath, projectId)
            HtmlConfig(
                projectId = projectId,
                files = savedFiles,
                enableJavaScript = true,
                enableLocalStorage = true
            )
        } else {
            existingApp.htmlConfig
        }

        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            htmlConfig = htmlConfig,
            updatedAt = System.currentTimeMillis()
        )
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
    ) = updateApp(appId, "Media", iconUri) { existingApp, savedIconPath ->
        val context = getApplication<Application>()
        val isVideo = appType == AppType.VIDEO

        // Save media file (if new media)
        val savedMediaPath = mediaUri?.let { uri ->
            withContext(Dispatchers.IO) { MediaStorage.saveMedia(context, uri, isVideo) }
        } ?: existingApp.url

        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            url = savedMediaPath,
            iconPath = savedIconPath,
            appType = appType,
            mediaConfig = mediaConfig?.copy(mediaPath = savedMediaPath) ?: existingApp.mediaConfig,
            updatedAt = System.currentTimeMillis()
        )
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
        if (galleryConfig == null || galleryConfig.items.isEmpty()) {
            _uiState.value = UiState.Error(Strings.pleaseAddMediaFile)
            return
        }
        updateApp(appId, "Gallery", iconUri) { existingApp, savedIconPath ->
            existingApp.copy(
                name = name.ifBlank { existingApp.name },
                iconPath = savedIconPath,
                galleryConfig = galleryConfig,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Update HTML app
     * 
     * Important: When HTML files are changed, need to re-process and save them
     * like saveHtmlApp does, otherwise the temp files will be deleted and cause 404
     */
    fun updateHtmlApp(
        appId: Long,
        name: String,
        htmlConfig: HtmlConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "HTML", iconUri) { existingApp, savedIconPath ->
        val context = getApplication<Application>()

        // Check if HTML files have changed
        val finalHtmlConfig = if (htmlConfig != null && htmlConfig != existingApp.htmlConfig) {
            AppLogger.d("MainViewModel", "HTML files changed, re-processing...")

            // Delete old project files if exists
            existingApp.htmlConfig?.projectId?.let { oldProjectId ->
                withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, oldProjectId) }
            }

            // Process and save using shared helper
            val projectId = HtmlStorage.generateProjectId()
            val savedHtmlFiles = processAndSaveHtmlFiles(context, htmlConfig.files, projectId)

            // Check if any HTML files were saved
            if (savedHtmlFiles.none { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) }) {
                AppLogger.e("MainViewModel", "No HTML files were saved successfully in update")
                withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, projectId) }
                throw Exception(Strings.saveFailedCannotProcessHtml)
            }

            withContext(Dispatchers.IO) { HtmlStorage.clearTempFiles(context) }
            htmlConfig.copy(projectId = projectId, files = savedHtmlFiles)
        } else {
            existingApp.htmlConfig
        }

        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            htmlConfig = finalHtmlConfig,
            updatedAt = System.currentTimeMillis()
        )
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
    fun createCategory(name: String, icon: String = "folder", color: String = "#6200EE") {
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
                _uiState.value = UiState.Error(Strings.failedCreateCategory.replaceFirst("%s", e.message ?: ""))
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
                _uiState.value = UiState.Error(Strings.failedUpdateCategory.replaceFirst("%s", e.message ?: ""))
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
                // Batch clear categoryId in one SQL statement (avoids N+1 queries)
                repository.clearCategoryId(category.id)
                // Delete category
                categoryRepository.deleteCategory(category)
                // If current selection is this category, switch to all
                if (_selectedCategoryId.value == category.id) {
                    _selectedCategoryId.value = null
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(Strings.failedDeleteCategory.replaceFirst("%s", e.message ?: ""))
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
                _uiState.value = UiState.Error(Strings.moveFailed.replaceFirst("%s", e.message ?: ""))
            }
        }
    }
}

/**
 * Edit state
 */
@androidx.compose.runtime.Stable
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

    // HTTP 允许（用户确认了解风险）
    val allowHttp: Boolean = false,

    // Activation code
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),  // Old format (for compatibility)
    val activationCodeList: List<com.webtoapp.core.activation.ActivationCode> = emptyList(),  // New format
    val activationRequireEveryTime: Boolean = false,  // Whether to require verification every time
    val activationDialogConfig: com.webtoapp.data.model.ActivationDialogConfig = com.webtoapp.data.model.ActivationDialogConfig(),

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
    val extensionFabIcon: String = "",
    
    // Auto-start config
    val autoStartConfig: AutoStartConfig? = null,
    
    // Forced run config
    val forcedRunConfig: com.webtoapp.core.forcedrun.ForcedRunConfig? = null,
    
    // Black tech feature config (independent module)
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,
    
    // App disguise config (independent module)
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,
    
    // Device disguise config (device type/brand/model UA spoofing)
    val deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig = com.webtoapp.core.disguise.DeviceDisguiseConfig()
)

/**
 * UI state
 */
sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Progress(
        val message: String,
        val current: Int,
        val total: Int
    ) : UiState() {
        val percent: Int get() = if (total > 0) (current * 100 / total).coerceIn(0, 100) else 0
    }
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}
