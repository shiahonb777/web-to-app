package com.webtoapp.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.data.dao.WebAppSummary
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




@OptIn(FlowPreview::class)
class MainViewModel(
    application: Application,
    private val repository: WebAppRepository,
    private val categoryRepository: AppCategoryRepository
) : AndroidViewModel(application) {


    val webApps: StateFlow<List<WebApp>> = repository.allWebApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    val webAppSummaries: StateFlow<List<WebAppSummary>> = repository.allWebAppSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    val categories: StateFlow<List<AppCategory>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()


    private val _currentApp = MutableStateFlow<WebApp?>(null)
    val currentApp: StateFlow<WebApp?> = _currentApp.asStateFlow()


    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()


    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()


    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()


    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    private val _pwaAnalysisState = MutableStateFlow<PwaAnalysisState>(PwaAnalysisState.Idle)
    val pwaAnalysisState: StateFlow<PwaAnalysisState> = _pwaAnalysisState.asStateFlow()




    override fun onCleared() {
        super.onCleared()

        _editState.value.iconBitmap?.recycle()

        _editState.value = EditState()
        _currentApp.value = null
    }

    val filteredApps: StateFlow<List<WebApp>> = combine(
        webApps,
        searchQuery.debounce(300),
        selectedCategoryId
    ) { apps, query, categoryId ->
        var filtered = apps


        filtered = when (categoryId) {
            null -> filtered
            -1L -> filtered.filter { it.categoryId == null }
            else -> filtered.filter { it.categoryId == categoryId }
        }


        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    val filteredSummaries: StateFlow<List<WebAppSummary>> = combine(
        webAppSummaries,
        searchQuery.debounce(300),
        selectedCategoryId
    ) { summaries, query, categoryId ->
        var filtered = summaries

        filtered = when (categoryId) {
            null -> filtered
            -1L -> filtered.filter { it.categoryId == null }
            else -> filtered.filter { it.categoryId == categoryId }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    suspend fun getWebApp(id: Long): WebApp? = repository.getWebApp(id)


    fun deleteAppById(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteWebAppById(id)
                withContext(Dispatchers.IO) {
                    com.webtoapp.core.script.UserScriptStorage.deleteScriptsForApp(
                        getApplication(), id
                    )
                }
                _uiState.value = UiState.Success(Strings.appDeleted)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: Strings.deleteFailed)
            }
        }
    }


    fun moveAppToCategoryById(id: Long, categoryId: Long?) {
        viewModelScope.launch {
            try {
                val webApp = repository.getWebApp(id) ?: return@launch
                repository.updateWebApp(webApp.copy(categoryId = categoryId))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: Strings.saveFailed)
            }
        }
    }




    fun createNewApp() {
        _editState.value = EditState()
        _currentApp.value = null
        _uiState.value = UiState.Idle
        _hasUnsavedChanges.value = false
    }




    fun editApp(webApp: WebApp) {
        _currentApp.value = webApp
        _uiState.value = UiState.Idle
        _editState.value = webApp.toEditState()
        _hasUnsavedChanges.value = false



        if (webApp.webViewConfig.injectScripts.any {
            com.webtoapp.core.script.UserScriptStorage.isFileReference(it.code)
        }) {
            viewModelScope.launch {
                val resolvedScripts = withContext(Dispatchers.IO) {
                    com.webtoapp.core.script.UserScriptStorage.internalizeScripts(
                        getApplication(), webApp.webViewConfig.injectScripts
                    )
                }
                val currentState = _editState.value
                _editState.value = currentState.copy(
                    webViewConfig = currentState.webViewConfig.copy(injectScripts = resolvedScripts)
                )
            }
        }
    }




    fun updateEditState(update: EditState.() -> EditState) {
        _editState.value = _editState.value.update()
        _hasUnsavedChanges.value = true
    }








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




    fun applyPwaResult(result: PwaAnalysisResult) {
        viewModelScope.launch {

            result.suggestedName?.let { name ->
                if (name.isNotBlank() && _editState.value.name.isBlank()) {
                    updateEditState { copy(name = name) }
                }
            }


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
                    "minimal-ui" -> {  }
                    "browser" -> {  }
                }
            }


            if (result.source == PwaDataSource.MANIFEST) {
                updateEditState {
                    copy(webViewConfig = webViewConfig.copy(pwaOfflineEnabled = true))
                }
            }


            val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, _editState.value.url)
            if (hosts.isNotEmpty()) {
                updateEditState {
                    copy(apkExportConfig = apkExportConfig.copy(
                        deepLinkEnabled = true,
                        customDeepLinkHosts = hosts
                    ))
                }
            }


            result.startUrl?.let { startUrl ->
                val currentUrl = _editState.value.url.trim()
                if (startUrl != currentUrl && startUrl.isNotBlank()) {

                    val currentHost = PwaAnalyzer.extractHost(currentUrl)
                    val startHost = PwaAnalyzer.extractHost(startUrl)
                    if (currentHost == startHost) {
                        updateEditState { copy(url = startUrl) }
                    }
                }
            }
        }
    }




    fun resetPwaState() {
        _pwaAnalysisState.value = PwaAnalysisState.Idle
    }




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




    fun handleIconSelected(uri: Uri) {
        viewModelScope.launch {
            val oldPath = _editState.value.savedIconPath
            val savedPath = withContext(Dispatchers.IO) {
                val path = IconStorage.saveIconFromUri(getApplication(), uri)

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




    fun saveApp() {
        viewModelScope.launch {
            var state = _editState.value


            if (!validateInput(state)) return@launch

            state = _editState.value

            _uiState.value = UiState.Loading

            try {
                val webApp = buildDraftWebApp(
                    state = state,
                    iconPath = state.savedIconPath ?: state.iconUri?.toString(),
                    fallbackName = null,
                    categoryId = _selectedCategoryId.value?.takeIf { it > 0 },
                    appIdForExternalization = _currentApp.value?.id ?: 0L,
                )
                AppLogger.d("MainViewModel", "saveApp: activationEnabled=${state.activationEnabled}, " +
                    "activationCodeList.size=${state.activationCodeList.size}")

                if (_currentApp.value != null) {
                    repository.updateWebApp(webApp)
                } else {
                    val newId = repository.createWebApp(webApp)

                    if (newId > 0 && state.webViewConfig.injectScripts.any { it.code.length > com.webtoapp.core.script.UserScriptStorage.EXTERNAL_STORAGE_THRESHOLD }) {
                        val reExternalized = state.webViewConfig.copy(
                            injectScripts = com.webtoapp.core.script.UserScriptStorage.externalizeScripts(
                                getApplication(), newId, state.webViewConfig.injectScripts
                            )
                        )
                        repository.updateWebApp(webApp.copy(id = newId, webViewConfig = reExternalized))
                    }
                }

                _uiState.value = UiState.Success(Strings.appSavedSuccessfully)
                resetEditState()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: Strings.saveFailed)
            }
        }
    }




    fun deleteApp(webApp: WebApp) {
        viewModelScope.launch {
            try {
                repository.deleteWebApp(webApp)

                withContext(Dispatchers.IO) {
                    com.webtoapp.core.script.UserScriptStorage.deleteScriptsForApp(
                        getApplication(), webApp.id
                    )
                }
                _uiState.value = UiState.Success(Strings.appDeleted)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: Strings.deleteFailed)
            }
        }
    }




    fun search(query: String) {
        _searchQuery.value = query
    }




    fun resetUiState() {
        _uiState.value = UiState.Idle
    }




    private fun resetEditState() {
        _editState.value = EditState()
        _currentApp.value = null
        _hasUnsavedChanges.value = false
    }










    suspend fun saveAndPreview(): Long? {
        val state = _editState.value


        if (state.name.isBlank() && state.url.isBlank()) return null

        return try {
            val webApp = buildDraftWebApp(
                state = state,
                iconPath = state.savedIconPath ?: state.iconUri?.toString(),
                fallbackName = "Preview",
                categoryId = _selectedCategoryId.value?.takeIf { it > 0 },
                appIdForExternalization = _currentApp.value?.id ?: 0L,
            )

            val savedAppId: Long
            if (_currentApp.value != null) {

                repository.updateWebApp(webApp)
                savedAppId = webApp.id
                _currentApp.value = webApp
            } else {

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




    private fun validateInput(state: EditState): Boolean {
        return when {
            state.name.isBlank() -> {
                _uiState.value = UiState.Error(Strings.pleaseEnterAppName)
                false
            }

            state.appType == AppType.WEB && state.url.isBlank() -> {
                _uiState.value = UiState.Error(Strings.pleaseEnterWebsiteUrl)
                false
            }
            state.appType == AppType.WEB && !isValidUrl(state.url) -> {
                _uiState.value = UiState.Error(Strings.pleaseEnterValidUrl)
                false
            }




            state.appType == AppType.WEB && isInsecureRemoteHttpUrl(state.url) && !state.allowHttp -> {
                AppLogger.w("MainViewModel", "Auto-allowing insecure HTTP URL: ${state.url}")
                _editState.value = state.copy(allowHttp = true)
                true
            }

            state.appType == AppType.HTML && (state.htmlConfig?.files?.isEmpty() != false) -> {
                _uiState.value = UiState.Error(Strings.pleaseSelectHtmlFile)
                false
            }

            (state.appType == AppType.IMAGE || state.appType == AppType.VIDEO) && state.url.isBlank() -> {
                _uiState.value = UiState.Error(Strings.mediaFilePathEmpty)
                false
            }
            else -> true
        }
    }






    private fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        return try {

            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val uri = Uri.parse(trimmed)
                !uri.host.isNullOrBlank()
            } else {


                val host = trimmed.split("/").first()
                host.isNotBlank() && !host.contains(" ") &&
                    (host.contains(".") || host == "localhost" || isIpAddress(host))
            }
        } catch (e: Exception) {
            false
        }
    }




    private fun isIpAddress(host: String): Boolean {
        val parts = host.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            part.toIntOrNull()?.let { it in 0..255 } ?: false
        }
    }






    private fun normalizeUrl(url: String, appType: AppType, allowHttp: Boolean = false): String {
        val trimmed = url.trim()
        if (appType != AppType.WEB) return trimmed


        return if (allowHttp && trimmed.startsWith("http://", ignoreCase = true)) {
            trimmed
        } else {
            val withScheme = ensureWebUrlScheme(trimmed)
            upgradeRemoteHttpToHttps(withScheme)
        }
    }

    private suspend fun buildDraftWebApp(
        state: EditState,
        iconPath: String?,
        fallbackName: String?,
        categoryId: Long?,
        appIdForExternalization: Long,
    ): WebApp {
        val currentThemeType = getCurrentThemeType()
        val externalizedConfig = if (state.webViewConfig.injectScripts.any { it.code.length > com.webtoapp.core.script.UserScriptStorage.EXTERNAL_STORAGE_THRESHOLD }) {
            withContext(Dispatchers.IO) {
                state.webViewConfig.copy(
                    injectScripts = com.webtoapp.core.script.UserScriptStorage.externalizeScripts(
                        getApplication(), appIdForExternalization, state.webViewConfig.injectScripts
                    )
                )
            }
        } else {
            state.webViewConfig
        }

        val payload = state.toDraftPayload(
            normalizedUrl = normalizeUrl(state.url, state.appType, state.allowHttp),
            iconPath = iconPath,
            extensionModuleIds = state.extensionModuleIds,
            currentThemeType = currentThemeType,
            externalizedWebViewConfig = externalizedConfig,
        )
        return _currentApp.value.applyDraft(
            editState = state,
            payload = payload,
            overrides = DraftBuildOverrides(
                appNameFallback = fallbackName,
                categoryId = categoryId,
            ),
        )
    }






    private suspend fun getCurrentThemeType(): String {
        val themeManager = ThemeManager.getInstance(getApplication())
        return themeManager.themeTypeFlow.first().name
    }





    private suspend fun saveIconIfPresent(iconUri: Uri?): String? {
        return iconUri?.let { uri ->
            withContext(Dispatchers.IO) {
                IconStorage.saveIconFromUri(getApplication(), uri)
            }
        }
    }










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
                    ?: return@launch

                withContext(Dispatchers.IO) { repository.createWebApp(webApp) }
                _uiState.value = UiState.Success(Strings.appCreatedSuccessfully.replaceFirst("%s", typeName))
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Failed to save $typeName app", e)
                _uiState.value = UiState.Error(Strings.creationFailed.replaceFirst("%s", e.message ?: ""))
            }
        }
    }










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

    private fun hasReadableHtmlEntry(files: List<HtmlFile>): Boolean {
        return files.any { file ->
            (file.type == HtmlFileType.HTML || file.name.endsWith(".html", ignoreCase = true)) &&
                java.io.File(file.path).let { it.exists() && it.isFile && it.canRead() && it.length() > 0L }
        }
    }






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

        val savedHtmlConfig = htmlConfig?.copy(
            projectId = projectId,
            files = savedHtmlFiles
        )
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






    fun saveFrontendApp(
        name: String,
        outputPath: String,
        iconUri: Uri?,
        framework: String
    ) = createApp(framework, iconUri) { savedIconPath, currentThemeType, categoryId ->
        val context = getApplication<Application>()
        val projectId = HtmlStorage.generateProjectId()
        val savedFiles = copyBuildOutputToStorage(context, outputPath, projectId)

        if (!hasReadableHtmlEntry(savedFiles)) {
            AppLogger.e("MainViewModel", "No readable HTML entry found in frontend build output: $outputPath")
            withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, projectId) }
            _uiState.value = UiState.Error("Frontend build output has no readable HTML entry")
            return@createApp null
        }

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




    fun saveMultiWebApp(
        name: String,
        multiWebConfig: MultiWebConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = createApp("Multi-Site", iconUri) { savedIconPath, currentThemeType, categoryId ->
        val context = getApplication<Application>()
        val localSites = multiWebConfig.sites.filter { it.type == "LOCAL" && it.localFileUri.isNotBlank() }
        val existingHtmlSites = multiWebConfig.sites.filter { it.type == "EXISTING" && it.sourceProjectId.isNotBlank() }
        val projectId = if (localSites.isNotEmpty() && multiWebConfig.projectId.isBlank()) {
            HtmlStorage.generateProjectId()
        } else if (existingHtmlSites.isNotEmpty() && localSites.isEmpty() && multiWebConfig.projectId.isBlank()) {
            existingHtmlSites.first().sourceProjectId
        } else multiWebConfig.projectId.ifBlank { if (localSites.isNotEmpty()) HtmlStorage.generateProjectId() else "" }


        val updatedSites = if (localSites.isNotEmpty() && projectId.isNotBlank()) {
            multiWebConfig.sites.map { site ->
                if (site.type == "LOCAL" && site.localFileUri.isNotBlank()) {
                    val uri = Uri.parse(site.localFileUri)
                    val fileName = site.localFilePath.ifBlank { "page.html" }
                    val savedPath = HtmlStorage.saveHtmlFile(context, uri, fileName, projectId)
                    if (savedPath != null) {
                        site.copy(localFilePath = fileName.trimStart('/'), localFileUri = "")
                    } else site
                } else site
            }
        } else multiWebConfig.sites

        WebApp(
            name = name.ifBlank { "Multi-Site App" },
            url = updatedSites.firstOrNull()?.getEffectiveUrl() ?: "",
            iconPath = savedIconPath,
            appType = AppType.MULTI_WEB,
            multiWebConfig = multiWebConfig.copy(sites = updatedSites, projectId = projectId),
            themeType = currentThemeType,
            categoryId = categoryId
        )
    }




    fun updateMultiWebApp(
        appId: Long,
        name: String,
        multiWebConfig: MultiWebConfig,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "Multi-Site", iconUri) { existingApp, savedIconPath ->
        val context = getApplication<Application>()
        val localSites = multiWebConfig.sites.filter { it.type == "LOCAL" && it.localFileUri.isNotBlank() }
        val existingHtmlSites = multiWebConfig.sites.filter { it.type == "EXISTING" && it.sourceProjectId.isNotBlank() }
        val projectId = multiWebConfig.projectId.ifBlank {
            existingApp.multiWebConfig?.projectId ?: if (existingHtmlSites.isNotEmpty()) existingHtmlSites.first().sourceProjectId else ""
        }


        val updatedSites = multiWebConfig.sites.map { site ->
            if (site.type == "LOCAL" && site.localFileUri.isNotBlank() && projectId.isNotBlank()) {
                val uri = Uri.parse(site.localFileUri)
                val fileName = site.localFilePath.ifBlank { "page.html" }
                val savedPath = HtmlStorage.saveHtmlFile(context, uri, fileName, projectId)
                if (savedPath != null) {
                    site.copy(localFilePath = fileName.trimStart('/'), localFileUri = "")
                } else site
            } else site
        }

        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            url = updatedSites.firstOrNull()?.getEffectiveUrl() ?: existingApp.url,
            iconPath = savedIconPath,
            multiWebConfig = multiWebConfig.copy(sites = updatedSites, projectId = projectId),
            updatedAt = System.currentTimeMillis()
        )
    }




    fun updateFrontendApp(
        appId: Long,
        name: String,
        outputPath: String?,
        iconUri: Uri?,
        framework: String
    ) = updateApp(appId, "Frontend", iconUri) { existingApp, savedIconPath ->
        val context = getApplication<Application>()

        val htmlConfig = if (!outputPath.isNullOrEmpty()) {
            val projectId = existingApp.htmlConfig?.projectId ?: HtmlStorage.generateProjectId()
            val savedFiles = copyBuildOutputToStorage(context, outputPath, projectId)
            if (!hasReadableHtmlEntry(savedFiles)) {
                AppLogger.e("MainViewModel", "No readable HTML entry found in frontend build output: $outputPath")
                _uiState.value = UiState.Error("Frontend build output has no readable HTML entry")
                throw IllegalStateException("Frontend build output has no readable HTML entry")
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

        existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            htmlConfig = htmlConfig,
            updatedAt = System.currentTimeMillis()
        )
    }




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







    fun updateHtmlApp(
        appId: Long,
        name: String,
        htmlConfig: HtmlConfig?,
        iconUri: Uri?,
        themeType: String = "AURORA"
    ) = updateApp(appId, "HTML", iconUri) { existingApp, savedIconPath ->
        val context = getApplication<Application>()


        val finalHtmlConfig = if (htmlConfig != null && htmlConfig != existingApp.htmlConfig) {
            AppLogger.d("MainViewModel", "HTML files changed, re-processing...")


            existingApp.htmlConfig?.projectId?.let { oldProjectId ->
                withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, oldProjectId) }
            }


            val projectId = HtmlStorage.generateProjectId()
            val savedHtmlFiles = processAndSaveHtmlFiles(context, htmlConfig.files, projectId)


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

















    fun saveScrapedWebsiteApp(
        name: String,
        url: String,
        iconUri: Uri?,
        maxDepth: Int = 3,
        downloadCdnResources: Boolean = true,
        followLinks: Boolean = true,
        maxFiles: Int = 500,
        maxTotalSizeMb: Int = 200,
        skipPatterns: String = "",
        timeoutSeconds: Int = 30,
        onProgress: (com.webtoapp.core.scraper.WebsiteScraper.ScrapeProgress) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val context = getApplication<Application>()
                val scraper = com.webtoapp.core.scraper.WebsiteScraper(context)

                val skipPatternList = if (skipPatterns.isBlank()) emptyList()
                    else skipPatterns.split(",", " ", ";").map { it.trim() }.filter { it.isNotEmpty() }

                val config = com.webtoapp.core.scraper.WebsiteScraper.ScrapeConfig(
                    url = url,
                    maxDepth = maxDepth,
                    downloadCdnResources = downloadCdnResources,
                    followLinks = followLinks,
                    maxFiles = maxFiles,
                    maxTotalSize = maxTotalSizeMb.toLong() * 1024 * 1024,
                    skipPatterns = skipPatternList,
                    timeoutSeconds = timeoutSeconds
                )

                val result = scraper.scrape(config, onProgress)

                when (result) {
                    is com.webtoapp.core.scraper.WebsiteScraper.ScrapeResult.Success -> {

                        val currentThemeType = getCurrentThemeType()
                        val savedIconPath = saveIconIfPresent(iconUri)
                        val categoryId = _selectedCategoryId.value?.takeIf { it > 0 }


                        val projectId = HtmlStorage.generateProjectId()
                        val savedFiles = copyBuildOutputToStorage(
                            context,
                            result.projectDir.absolutePath,
                            projectId
                        )

                        if (savedFiles.none { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) }) {
                            AppLogger.e("MainViewModel", "No HTML files in scraped result")
                            withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, projectId) }
                            _uiState.value = UiState.Error(Strings.scrapeNoHtmlFound)
                            return@launch
                        }

                        val webApp = WebApp(
                            name = name.ifBlank {
                                try { java.net.URL(url).host } catch (e: Exception) { "Offline Site" }
                            },
                            url = url,
                            iconPath = savedIconPath,
                            appType = AppType.HTML,
                            htmlConfig = HtmlConfig(
                                projectId = projectId,
                                entryFile = result.entryFile,
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

                        withContext(Dispatchers.IO) { repository.createWebApp(webApp) }


                        scraper.deleteScrapedSite(result.projectDir.name)

                        val sizeKb = result.totalSize / 1024
                        _uiState.value = UiState.Success(
                            Strings.scrapePackSuccess.format(result.totalFiles, sizeKb)
                        )
                    }
                    is com.webtoapp.core.scraper.WebsiteScraper.ScrapeResult.Error -> {
                        _uiState.value = UiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Website scrape failed", e)
                _uiState.value = UiState.Error(Strings.scrapePackFailed.format(e.message))
            }
        }
    }







    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }




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




    fun updateCategory(category: AppCategory) {
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(Strings.failedUpdateCategory.replaceFirst("%s", e.message ?: ""))
            }
        }
    }





    fun deleteCategory(category: AppCategory) {
        viewModelScope.launch {
            try {

                repository.clearCategoryId(category.id)

                categoryRepository.deleteCategory(category)

                if (_selectedCategoryId.value == category.id) {
                    _selectedCategoryId.value = null
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(Strings.failedDeleteCategory.replaceFirst("%s", e.message ?: ""))
            }
        }
    }




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
