package com.webtoapp.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.WebToAppApplication
import com.webtoapp.data.model.*
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
    
    /**
     * ViewModel 销毁时清理资源
     */
    override fun onCleared() {
        super.onCleared()
        // 清理编辑状态中的 Bitmap 资源
        _editState.value.iconBitmap?.recycle()
        // 重置状态
        _editState.value = EditState()
        _currentApp.value = null
    }

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
        
        _editState.value = EditState(
            name = webApp.name,
            url = webApp.url,
            iconUri = webApp.iconPath?.let { Uri.parse(it) },
            savedIconPath = webApp.iconPath,  // 保留已有的本地路径
            appType = webApp.appType,  // 保留应用类型
            mediaConfig = webApp.mediaConfig,  // 保留媒体配置
            htmlConfig = webApp.htmlConfig,  // 保留HTML配置
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
            forcedRunConfig = webApp.forcedRunConfig
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
                
                // 构建扩展模块ID列表
                val extensionModuleIds = if (state.extensionModuleEnabled) {
                    state.extensionModuleIds.toList()
                } else {
                    emptyList()
                }
                
                val webApp = _currentApp.value?.copy(
                    name = state.name,
                    url = normalizeUrl(state.url),
                    iconPath = iconPath,
                    appType = state.appType,  // 保留应用类型
                    mediaConfig = state.mediaConfig,  // 保留媒体配置
                    htmlConfig = state.htmlConfig,  // 保留HTML配置
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
                    forcedRunConfig = state.forcedRunConfig
                ) ?: run {
                    // 将新格式激活码转换为字符串列表（用于兼容性）
                    val activationCodeStrings = state.activationCodeList.map { it.toJson() } + state.activationCodes
                    
                    WebApp(
                        name = state.name,
                        url = normalizeUrl(state.url),
                        iconPath = iconPath,
                        appType = AppType.WEB,  // 默认类型
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
                        forcedRunConfig = state.forcedRunConfig
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
            // 只有 WEB 类型需要验证 URL
            state.appType == AppType.WEB && state.url.isBlank() -> {
                _uiState.value = UiState.Error("请输入网站地址")
                false
            }
            state.appType == AppType.WEB && !isValidUrl(state.url) -> {
                _uiState.value = UiState.Error("请输入有效的网址")
                false
            }
            // HTML 类型需要有 HTML 文件
            state.appType == AppType.HTML && (state.htmlConfig?.files?.isEmpty() != false) -> {
                _uiState.value = UiState.Error("请选择 HTML 文件")
                false
            }
            // IMAGE/VIDEO 类型需要有媒体文件路径
            (state.appType == AppType.IMAGE || state.appType == AppType.VIDEO) && state.url.isBlank() -> {
                _uiState.value = UiState.Error("媒体文件路径不能为空")
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
                
                // 从 ThemeManager 获取当前主题类型
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // 保存图标（如果有）
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // 保存媒体文件
                val savedMediaPath = mediaUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        MediaStorage.saveMedia(context, uri, isVideo)
                    }
                }
                
                if (savedMediaPath == null) {
                    _uiState.value = UiState.Error("媒体文件保存失败")
                    return@launch
                }
                
                val webApp = WebApp(
                    name = name.ifBlank { if (isVideo) "视频应用" else "图片应用" },
                    url = savedMediaPath,
                    iconPath = savedIconPath,
                    appType = appType,
                    mediaConfig = mediaConfig?.copy(mediaPath = savedMediaPath),
                    activationEnabled = false,
                    activationCodes = emptyList(),
                    activationCodeList = emptyList(),
                    bgmEnabled = false,
                    bgmConfig = BgmConfig(),
                    themeType = currentThemeType
                )
                
                // 保存到数据库
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success("媒体应用创建成功")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("创建失败: ${e.message}")
            }
        }
    }
    
    /**
     * 保存HTML应用（本地HTML+CSS+JS转APP）
     * 
     * 重要：将 CSS 和 JS 内联到 HTML 文件中，确保在 WebView 中正确加载
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
                
                // 从 ThemeManager 获取当前主题类型
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // 保存图标（如果有）
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // 单HTML模式 - 使用 HtmlProjectProcessor 处理文件
                val projectId = HtmlStorage.generateProjectId()
                val savedHtmlFiles = withContext(Dispatchers.IO) {
                    val files = htmlConfig?.files ?: emptyList()
                    
                    // 分类文件
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
                    
                    android.util.Log.d("MainViewModel", "HTML文件分类: HTML=${htmlFiles.size}, CSS=${cssFiles.size}, JS=${jsFiles.size}, Other=${otherFiles.size}")
                    
                    // 读取 CSS 内容
                    val cssContent = cssFiles.mapNotNull { cssFile ->
                        try {
                            val file = java.io.File(cssFile.path)
                            if (file.exists() && file.canRead()) {
                                com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(file, null)
                            } else null
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "读取 CSS 文件失败: ${cssFile.path}", e)
                            null
                        }
                    }.joinToString("\n\n")
                    
                    // 读取 JS 内容
                    val jsContent = jsFiles.mapNotNull { jsFile ->
                        try {
                            val file = java.io.File(jsFile.path)
                            if (file.exists() && file.canRead()) {
                                com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(file, null)
                            } else null
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "读取 JS 文件失败: ${jsFile.path}", e)
                            null
                        }
                    }.joinToString("\n\n")
                    
                    android.util.Log.d("MainViewModel", "CSS 内容长度: ${cssContent.length}, JS 内容长度: ${jsContent.length}")
                    
                    // 处理 HTML 文件，内联 CSS 和 JS
                    val processedHtmlFiles = htmlFiles.mapNotNull { htmlFile ->
                        try {
                            val sourceFile = java.io.File(htmlFile.path)
                            if (!sourceFile.exists() || !sourceFile.canRead()) {
                                android.util.Log.e("MainViewModel", "HTML 文件不存在或无法读取: ${htmlFile.path}")
                                return@mapNotNull null
                            }
                            
                            // 读取 HTML 内容
                            var htmlContent = com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(sourceFile, null)
                            
                            // 使用 HtmlProjectProcessor 处理 HTML 内容（内联 CSS/JS）
                            htmlContent = com.webtoapp.util.HtmlProjectProcessor.processHtmlContent(
                                htmlContent = htmlContent,
                                cssContent = cssContent.takeIf { it.isNotBlank() },
                                jsContent = jsContent.takeIf { it.isNotBlank() },
                                fixPaths = true
                            )
                            
                            // 保存处理后的 HTML 文件
                            val savedPath = HtmlStorage.saveProcessedHtml(
                                context, htmlContent, htmlFile.name, projectId
                            )
                            
                            if (savedPath != null) {
                                android.util.Log.d("MainViewModel", "HTML 文件已保存(内联CSS/JS): ${htmlFile.name}")
                                htmlFile.copy(path = savedPath)
                            } else {
                                android.util.Log.e("MainViewModel", "无法保存处理后的 HTML 文件: ${htmlFile.name}")
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "处理 HTML 文件失败: ${htmlFile.path}", e)
                            null
                        }
                    }
                    
                    // 保存其他文件（图片、字体等）
                    val savedOtherFiles = otherFiles.mapNotNull { file ->
                        val savedPath = HtmlStorage.saveFromTempFile(
                            context, file.path, file.name, projectId
                        )
                        if (savedPath != null) {
                            file.copy(path = savedPath)
                        } else null
                    }
                    
                    // 返回所有保存的文件（只包含 HTML 和其他文件，CSS/JS 已内联）
                    processedHtmlFiles + savedOtherFiles
                }
                
                val savedHtmlConfig = htmlConfig?.copy(
                    projectId = projectId,
                    files = savedHtmlFiles
                )
                
                // 清理临时文件
                withContext(Dispatchers.IO) {
                    HtmlStorage.clearTempFiles(context)
                }
                
                val webApp = WebApp(
                    name = name.ifBlank { "HTML应用" },
                    url = "",
                    iconPath = savedIconPath,
                    appType = AppType.HTML,
                    htmlConfig = savedHtmlConfig,
                    activationEnabled = false,
                    activationCodes = emptyList(),
                    activationCodeList = emptyList(),
                    bgmEnabled = false,
                    bgmConfig = BgmConfig(),
                    themeType = currentThemeType
                )
                
                // 保存到数据库
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success("HTML应用创建成功")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("创建失败: ${e.message}")
            }
        }
    }
    
    /**
     * 保存前端项目应用（Vue/React/Node.js 等）
     * 
     * 将构建后的静态文件作为 HTML 应用保存
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
                
                // 从 ThemeManager 获取当前主题类型
                val themeManager = ThemeManager.getInstance(context)
                val currentThemeType = themeManager.themeTypeFlow.first().name
                
                // 保存图标（如果有）
                val savedIconPath = iconUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        IconStorage.saveIconFromUri(context, uri)
                    }
                }
                
                // 生成项目 ID
                val projectId = HtmlStorage.generateProjectId()
                
                // 复制构建输出到应用存储
                val savedFiles = withContext(Dispatchers.IO) {
                    val outputDir = java.io.File(outputPath)
                    if (!outputDir.exists() || !outputDir.isDirectory) {
                        throw Exception("构建输出目录不存在: $outputPath")
                    }
                    
                    val files = mutableListOf<com.webtoapp.data.model.HtmlFile>()
                    
                    // 递归复制所有文件
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
                
                // 创建 HTML 配置
                val htmlConfig = HtmlConfig(
                    projectId = projectId,
                    files = savedFiles,
                    enableJavaScript = true,
                    enableLocalStorage = true
                )
                
                val webApp = WebApp(
                    name = name.ifBlank { "$framework 应用" },
                    url = "",
                    iconPath = savedIconPath,
                    appType = AppType.HTML,
                    htmlConfig = htmlConfig,
                    activationEnabled = false,
                    activationCodes = emptyList(),
                    activationCodeList = emptyList(),
                    bgmEnabled = false,
                    bgmConfig = BgmConfig(),
                    themeType = currentThemeType
                )
                
                // 保存到数据库
                withContext(Dispatchers.IO) {
                    repository.createWebApp(webApp)
                }
                
                _uiState.value = UiState.Success("$framework 应用创建成功")
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
    
    // 应用类型（用于区分 WEB/HTML/IMAGE/VIDEO）
    val appType: AppType = AppType.WEB,
    
    // 媒体应用配置（IMAGE/VIDEO 类型）
    val mediaConfig: MediaConfig? = null,
    
    // HTML应用配置（HTML 类型）
    val htmlConfig: HtmlConfig? = null,

    // 激活码
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),  // 旧格式（兼容性）
    val activationCodeList: List<com.webtoapp.core.activation.ActivationCode> = emptyList(),  // 新格式
    val activationRequireEveryTime: Boolean = false,  // 是否每次启动都需要验证

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
    
    // 扩展模块
    val extensionModuleEnabled: Boolean = false,
    val extensionModuleIds: Set<String> = emptySet(),
    
    // 自启动配置
    val autoStartConfig: AutoStartConfig? = null,
    
    // 强制运行配置
    val forcedRunConfig: com.webtoapp.core.forcedrun.ForcedRunConfig? = null
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
