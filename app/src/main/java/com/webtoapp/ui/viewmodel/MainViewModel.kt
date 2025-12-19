package com.webtoapp.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.WebToAppApplication
import com.webtoapp.data.model.*
import com.webtoapp.ui.theme.ThemeManager
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
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
 * 主界面ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WebAppRepository = WebToAppApplication.repository

    // 所有应用列表
    val webApps: StateFlow<List<WebApp>> = repository.allWebApps
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 当前编辑的应用
    private val _currentApp = MutableStateFlow<WebApp?>(null)
    val currentApp: StateFlow<WebApp?> = _currentApp.asStateFlow()

    // 编辑状态
    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    // UI状态
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 搜索
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<WebApp>> = combine(
        webApps,
        searchQuery
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.url.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * 创建新应用
     */
    fun createNewApp() {
        _editState.value = EditState()
        _currentApp.value = null
        _uiState.value = UiState.Idle  // 重置 UI 状态，避免显示旧的错误消息
    }

    /**
     * 编辑现有应用
     */
    fun editApp(webApp: WebApp) {
        _currentApp.value = webApp
        _uiState.value = UiState.Idle  // 重置 UI 状态，避免显示旧的错误消息
        
        // 判断是否为画廊模式
        val hasGallery = webApp.galleryConfig != null && webApp.galleryConfig.items.isNotEmpty()
        
        _editState.value = EditState(
            name = webApp.name,
            url = webApp.url,
            iconUri = webApp.iconPath?.let { Uri.parse(it) },
            savedIconPath = webApp.iconPath,  // 保留已有的本地路径
            activationEnabled = webApp.activationEnabled,
            activationCodes = webApp.activationCodes,
            activationCodeList = webApp.activationCodeList,
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
            // 画廊配置
            isGalleryMode = hasGallery,
            galleryItems = webApp.galleryConfig?.items ?: emptyList(),
            galleryConfig = webApp.galleryConfig ?: GalleryConfig()
        )
    }

    /**
     * 更新编辑状态
     */
    fun updateEditState(update: EditState.() -> EditState) {
        _editState.value = _editState.value.update()
    }

    /**
     * 处理选择的图标 URI - 复制到私有目录实现持久化
     */
    fun handleIconSelected(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                IconStorage.saveIconFromUri(getApplication(), uri)
            }
            if (savedPath != null) {
                // 删除旧图标（如果存在且不同）
                val oldPath = _editState.value.savedIconPath
                if (oldPath != null && oldPath != savedPath) {
                    withContext(Dispatchers.IO) {
                        IconStorage.deleteIcon(oldPath)
                    }
                }
                // 更新状态，保存本地路径
                _editState.value = _editState.value.copy(
                    iconUri = Uri.parse(savedPath),
                    savedIconPath = savedPath
                )
            } else {
                _uiState.value = UiState.Error("图标保存失败，请重试")
            }
        }
    }

    /**
     * 处理选择的启动画面媒体 - 复制到私有目录实现持久化
     */
    fun handleSplashMediaSelected(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                SplashStorage.saveMediaFromUri(getApplication(), uri, isVideo)
            }
            if (savedPath != null) {
                // 删除旧媒体文件（如果存在且不同）
                val oldPath = _editState.value.savedSplashPath
                if (oldPath != null && oldPath != savedPath) {
                    withContext(Dispatchers.IO) {
                        SplashStorage.deleteMedia(oldPath)
                    }
                }
                // 更新状态
                val newType = if (isVideo) SplashType.VIDEO else SplashType.IMAGE
                _editState.value = _editState.value.copy(
                    splashMediaUri = Uri.parse(savedPath),
                    savedSplashPath = savedPath,
                    splashConfig = _editState.value.splashConfig.copy(type = newType)
                )
            } else {
                _uiState.value = UiState.Error("启动画面保存失败，请重试")
            }
        }
    }

    /**
     * 清除启动画面媒体
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
     * 保存应用
     */
    fun saveApp() {
        viewModelScope.launch {
            val state = _editState.value

            // 验证
            if (!validateInput(state)) return@launch

            _uiState.value = UiState.Loading

            try {
                // 使用持久化的本地路径
                val iconPath = state.savedIconPath ?: state.iconUri?.toString()
                
                // 构建启动画面配置
                val splashConfig = if (state.splashEnabled && state.savedSplashPath != null) {
                    state.splashConfig.copy(mediaPath = state.savedSplashPath)
                } else {
                    null
                }
                
                // 构建背景音乐配置
                val bgmConfig = if (state.bgmEnabled && state.bgmConfig.playlist.isNotEmpty()) {
                    state.bgmConfig
                } else {
                    null
                }

                // 构建 APK 导出配置（仅当有自定义值时才保存）
                val apkExportConfig = state.apkExportConfig.let { config ->
                    if (config.customPackageName.isNullOrBlank() && 
                        config.customVersionName.isNullOrBlank() && 
                        config.customVersionCode == null) {
                        null
                    } else {
                        config
                    }
                }

                // 构建翻译配置
                val translateConfig = if (state.translateEnabled) state.translateConfig else null

                // 从 ThemeManager 获取当前主题类型
                val themeManager = ThemeManager.getInstance(getApplication())
                val currentThemeType = themeManager.themeTypeFlow.first().name

                // 将新格式激活码转换为字符串列表（用于兼容性）
                val activationCodeStrings = state.activationCodeList.map { it.toJson() } + state.activationCodes
                
                // 构建画廊配置（如果启用）
                val galleryConfig = if (state.isGalleryMode && state.galleryItems.isNotEmpty()) {
                    state.galleryConfig.copy(items = state.galleryItems)
                } else {
                    null
                }
                
                // 构建扩展模块ID列表
                val extensionModuleIds = if (state.extensionModuleEnabled) {
                    state.extensionModuleIds.toList()
                } else {
                    emptyList()
                }
                
                val webApp = _currentApp.value?.copy(
                    name = state.name,
                    url = if (state.isGalleryMode) "" else normalizeUrl(state.url),
                    iconPath = iconPath,
                    activationEnabled = state.activationEnabled,
                    activationCodes = activationCodeStrings,
                    activationCodeList = state.activationCodeList,
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
                    galleryConfig = galleryConfig,
                    extensionModuleIds = extensionModuleIds
                ) ?: run {
                    // 将新格式激活码转换为字符串列表（用于兼容性）
                    val activationCodeStrings = state.activationCodeList.map { it.toJson() } + state.activationCodes
                    
                    WebApp(
                        name = state.name,
                        url = if (state.isGalleryMode) "" else normalizeUrl(state.url),
                        iconPath = iconPath,
                        appType = AppType.WEB,  // 默认类型
                        activationEnabled = state.activationEnabled,
                        activationCodes = activationCodeStrings,
                        activationCodeList = state.activationCodeList,
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
                        galleryConfig = galleryConfig,
                        extensionModuleIds = extensionModuleIds
                    )
                }

                if (_currentApp.value != null) {
                    repository.updateWebApp(webApp)
                } else {
                    repository.createWebApp(webApp)
                }

                _uiState.value = UiState.Success("应用保存成功")
                resetEditState()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "保存失败")
            }
        }
    }

    /**
     * 删除应用
     */
    fun deleteApp(webApp: WebApp) {
        viewModelScope.launch {
            try {
                repository.deleteWebApp(webApp)
                _uiState.value = UiState.Success("应用已删除")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 搜索
     */
    fun search(query: String) {
        _searchQuery.value = query
    }

    /**
     * 重置UI状态
     */
    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    /**
     * 重置编辑状态
     */
    private fun resetEditState() {
        _editState.value = EditState()
        _currentApp.value = null
    }

    /**
     * 验证输入
     */
    private fun validateInput(state: EditState): Boolean {
        return when {
            state.name.isBlank() -> {
                _uiState.value = UiState.Error("请输入应用名称")
                false
            }
            // 画廊模式：检查是否有网址
            state.isGalleryMode && state.galleryItems.isEmpty() -> {
                _uiState.value = UiState.Error("请至少添加一个网址")
                false
            }
            // 单网址模式：检查URL
            !state.isGalleryMode && state.url.isBlank() -> {
                _uiState.value = UiState.Error("请输入网站地址")
                false
            }
            !state.isGalleryMode && !isValidUrl(state.url) -> {
                _uiState.value = UiState.Error("请输入有效的网址")
                false
            }
            else -> true
        }
    }

    /**
     * 验证URL
     * 允许用户输入带或不带协议的URL
     */
    private fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        return try {
            // 如果有协议，直接解析验证
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val uri = Uri.parse(trimmed)
                !uri.host.isNullOrBlank()
            } else {
                // 没有协议，只检查是否包含域名格式（至少有一个点）
                trimmed.contains(".") && !trimmed.contains(" ")
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 标准化URL
     * 不自动补全协议，保留用户原始输入
     */
    private fun normalizeUrl(url: String): String {
        return url.trim()
    }
    
    /**
     * 保存媒体应用（图片/视频转APP）
     * 支持单媒体模式和多媒体画廊模式
     */
    fun saveMediaApp(
        name: String,
        appType: AppType,
        mediaUri: Uri?,
        mediaConfig: MediaConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA",
        galleryConfig: GalleryConfig? = null
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                val isVideo = appType == AppType.VIDEO
                
                // 从 ThemeManager 获取当前主题类型
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // 保存图标（如果有）
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // 判断是画廊模式还是单媒体模式
                val isGalleryMode = galleryConfig != null && galleryConfig.items.isNotEmpty()
                
                val webApp = if (isGalleryMode) {
                    // 画廊模式：保存所有媒体文件
                    val savedItems = galleryConfig!!.items.map { item ->
                        val savedPath = withContext(Dispatchers.IO) {
                            MediaStorage.saveMedia(
                                context, 
                                Uri.parse(item.path), 
                                item.type == GalleryItemType.VIDEO
                            )
                        }
                        item.copy(path = savedPath ?: item.path)
                    }
                    
                    val savedGalleryConfig = galleryConfig.copy(items = savedItems)
                    
                    WebApp(
                        name = name.ifBlank { "媒体画廊" },
                        url = "",  // 画廊模式不使用此字段
                        iconPath = savedIconPath,
                        appType = appType,
                        mediaConfig = null,
                        galleryConfig = savedGalleryConfig,
                        activationEnabled = false,
                        activationCodes = emptyList(),
                        activationCodeList = emptyList(),
                        bgmEnabled = false,
                        bgmConfig = BgmConfig(),
                        themeType = currentThemeType
                    )
                } else {
                    // 单媒体模式
                    val savedMediaPath = mediaUri?.let { uri ->
                        withContext(Dispatchers.IO) {
                            MediaStorage.saveMedia(context, uri, isVideo)
                        }
                    }
                    
                    if (savedMediaPath == null) {
                        _uiState.value = UiState.Error("媒体文件保存失败")
                        return@launch
                    }
                    
                    WebApp(
                        name = name.ifBlank { if (isVideo) "视频应用" else "图片应用" },
                        url = savedMediaPath,
                        iconPath = savedIconPath,
                        appType = appType,
                        mediaConfig = mediaConfig?.copy(mediaPath = savedMediaPath),
                        galleryConfig = null,
                        activationEnabled = false,
                        activationCodes = emptyList(),
                        activationCodeList = emptyList(),
                        bgmEnabled = false,
                        bgmConfig = BgmConfig(),
                        themeType = currentThemeType
                    )
                }
                
                // 保存到数据库
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success(
                    if (isGalleryMode) "媒体画廊创建成功" else "媒体应用创建成功"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("创建失败: ${e.message}")
            }
        }
    }
    
    /**
     * 保存HTML应用（本地HTML+CSS+JS转APP）
     * 支持单HTML模式和多HTML画廊模式
     */
    fun saveHtmlApp(
        name: String,
        htmlConfig: HtmlConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA",
        galleryConfig: GalleryConfig? = null
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val context = getApplication<Application>()
                
                // 从 ThemeManager 获取当前主题类型
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // 保存图标（如果有）
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // 判断是画廊模式还是单HTML模式
                val isGalleryMode = galleryConfig != null && galleryConfig.items.isNotEmpty()
                
                val webApp = if (isGalleryMode) {
                    // 画廊模式：保存所有HTML文件
                    val savedItems = galleryConfig!!.items.map { item ->
                        val savedPath = withContext(Dispatchers.IO) {
                            // 复制HTML文件到持久化目录
                            val projectId = HtmlStorage.generateProjectId()
                            HtmlStorage.saveFromTempFile(
                                context, item.path, item.htmlConfig?.entryFile ?: "index.html", projectId
                            )
                        }
                        item.copy(path = savedPath ?: item.path)
                    }
                    
                    val savedGalleryConfig = galleryConfig.copy(items = savedItems)
                    
                    // 清理临时文件
                    withContext(Dispatchers.IO) {
                        HtmlStorage.clearTempFiles(context)
                        // 清理画廊临时文件
                        java.io.File(context.cacheDir, "html_gallery_temp").deleteRecursively()
                    }
                    
                    WebApp(
                        name = name.ifBlank { "HTML画廊" },
                        url = "",
                        iconPath = savedIconPath,
                        appType = AppType.HTML,
                        htmlConfig = null,
                        galleryConfig = savedGalleryConfig,
                        activationEnabled = false,
                        activationCodes = emptyList(),
                        activationCodeList = emptyList(),
                        bgmEnabled = false,
                        bgmConfig = BgmConfig(),
                        themeType = currentThemeType
                    )
                } else {
                    // 单HTML模式
                    val projectId = HtmlStorage.generateProjectId()
                    val savedHtmlFiles = withContext(Dispatchers.IO) {
                        htmlConfig?.files?.mapNotNull { file ->
                            val savedPath = HtmlStorage.saveFromTempFile(
                                context, file.path, file.name, projectId
                            )
                            if (savedPath != null) {
                                file.copy(path = savedPath)
                            } else {
                                android.util.Log.e("MainViewModel", "无法保存HTML文件: ${file.name}")
                                null
                            }
                        } ?: emptyList()
                    }
                    
                    val savedHtmlConfig = htmlConfig?.copy(
                        projectId = projectId,
                        files = savedHtmlFiles
                    )
                    
                    // 清理临时文件
                    withContext(Dispatchers.IO) {
                        HtmlStorage.clearTempFiles(context)
                    }
                    
                    WebApp(
                        name = name.ifBlank { "HTML应用" },
                        url = "",
                        iconPath = savedIconPath,
                        appType = AppType.HTML,
                        htmlConfig = savedHtmlConfig,
                        galleryConfig = null,
                        activationEnabled = false,
                        activationCodes = emptyList(),
                        activationCodeList = emptyList(),
                        bgmEnabled = false,
                        bgmConfig = BgmConfig(),
                        themeType = currentThemeType
                    )
                }
                
                // 保存到数据库
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success(
                    if (isGalleryMode) "HTML画廊创建成功" else "HTML应用创建成功"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("创建失败: ${e.message}")
            }
        }
    }
}

/**
 * 编辑状态
 */
data class EditState(
    val name: String = "",
    val url: String = "",
    val iconUri: Uri? = null,           // 用于 UI 显示
    val savedIconPath: String? = null,  // 持久化的本地文件路径
    val iconBitmap: Bitmap? = null,

    // 激活码
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),  // 旧格式（兼容性）
    val activationCodeList: List<com.webtoapp.core.activation.ActivationCode> = emptyList(),  // 新格式

    // 广告
    val adsEnabled: Boolean = false,
    val adConfig: AdConfig = AdConfig(),

    // 公告
    val announcementEnabled: Boolean = false,
    val announcement: Announcement = Announcement(),

    // 广告拦截
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),

    // WebView配置
    val webViewConfig: WebViewConfig = WebViewConfig(),

    // 启动画面
    val splashEnabled: Boolean = false,
    val splashConfig: SplashConfig = SplashConfig(),
    val splashMediaUri: Uri? = null,        // 用于 UI 显示
    val savedSplashPath: String? = null,    // 持久化的本地文件路径
    
    // 背景音乐
    val bgmEnabled: Boolean = false,
    val bgmConfig: BgmConfig = BgmConfig(),
    
    // APK 导出配置（仅打包APK时生效）
    val apkExportConfig: ApkExportConfig = ApkExportConfig(),
    
    // 主题配置（用于导出的应用 UI 风格）
    val themeType: String = "AURORA",
    
    // 网页自动翻译配置
    val translateEnabled: Boolean = false,
    val translateConfig: TranslateConfig = TranslateConfig(),
    
    // 画廊模式（多网址）
    val isGalleryMode: Boolean = false,
    val galleryItems: List<GalleryItem> = emptyList(),
    val galleryConfig: GalleryConfig = GalleryConfig(),
    
    // 扩展模块
    val extensionModuleEnabled: Boolean = false,
    val extensionModuleIds: Set<String> = emptySet()
)

/**
 * UI状态
 */
sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}
