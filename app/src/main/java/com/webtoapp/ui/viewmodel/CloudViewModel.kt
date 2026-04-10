package com.webtoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 云服务 ViewModel
 *
 * 管理激活码兑换、设备管理、公告、项目等 UI 状态
 */
class CloudViewModel(private val cloudRepo: CloudRepository) : ViewModel() {

    // ─── 激活码 ───

    private val _redeemState = MutableStateFlow<FormState>(FormState.Idle)
    val redeemState: StateFlow<FormState> = _redeemState.asStateFlow()

    private val _activationHistory = MutableStateFlow<List<ActivationRecord>>(emptyList())
    val activationHistory: StateFlow<List<ActivationRecord>> = _activationHistory.asStateFlow()

    // ─── 设备 ───

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    private val _devicesLoading = MutableStateFlow(false)
    val devicesLoading: StateFlow<Boolean> = _devicesLoading.asStateFlow()

    // ─── 公告 ───

    private val _announcements = MutableStateFlow<List<AnnouncementData>>(emptyList())
    val announcements: StateFlow<List<AnnouncementData>> = _announcements.asStateFlow()

    // ─── 项目 ───

    private val _projects = MutableStateFlow<List<CloudProject>>(emptyList())
    val projects: StateFlow<List<CloudProject>> = _projects.asStateFlow()

    private val _projectsLoading = MutableStateFlow(false)
    val projectsLoading: StateFlow<Boolean> = _projectsLoading.asStateFlow()

    // ─── 我的发布 ───

    private val _myPublishedModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val myPublishedModules: StateFlow<List<StoreModuleInfo>> = _myPublishedModules.asStateFlow()

    private val _myPublishedLoading = MutableStateFlow(false)
    val myPublishedLoading: StateFlow<Boolean> = _myPublishedLoading.asStateFlow()

    // ─── 分析 ───

    private val _analytics = MutableStateFlow<AnalyticsData?>(null)
    val analytics: StateFlow<AnalyticsData?> = _analytics.asStateFlow()

    // ─── 版本 ───

    private val _versions = MutableStateFlow<List<ProjectVersion>>(emptyList())
    val versions: StateFlow<List<ProjectVersion>> = _versions.asStateFlow()

    private val _publishLoading = MutableStateFlow(false)
    val publishLoading: StateFlow<Boolean> = _publishLoading.asStateFlow()

    private val _publishProgress = MutableStateFlow(0f)
    val publishProgress: StateFlow<Float> = _publishProgress.asStateFlow()

    // ─── 通用 ───

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    // ═══════════════════════════════════════════
    // ACTIVATION CODE
    // ═══════════════════════════════════════════

    fun redeemCode(code: String) {
        if (_redeemState.value is FormState.Loading) return
        _redeemState.value = FormState.Loading

        viewModelScope.launch {
            when (val result = cloudRepo.redeemCode(code)) {
                is AuthResult.Success -> {
                    _redeemState.value = FormState.Success(result.data.message)
                    _message.value = result.data.message
                }
                is AuthResult.Error -> {
                    _redeemState.value = FormState.Error(result.message)
                }
            }
        }
    }

    fun resetRedeemState() { _redeemState.value = FormState.Idle }

    fun loadActivationHistory() {
        viewModelScope.launch {
            when (val result = cloudRepo.getActivationHistory()) {
                is AuthResult.Success -> _activationHistory.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // DEVICE MANAGEMENT
    // ═══════════════════════════════════════════

    fun loadDevices() {
        _devicesLoading.value = true
        viewModelScope.launch {
            when (val result = cloudRepo.getDevices()) {
                is AuthResult.Success -> _devices.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
            _devicesLoading.value = false
        }
    }

    fun removeDevice(deviceId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.removeDevice(deviceId)) {
                is AuthResult.Success -> {
                    _message.value = result.data
                    loadDevices() // 刷新列表
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // ANNOUNCEMENTS
    // ═══════════════════════════════════════════

    fun loadAnnouncements(appVersion: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.getAnnouncements(appVersion)) {
                is AuthResult.Success -> _announcements.value = result.data
                is AuthResult.Error -> {} // 静默失败
            }
        }
    }

    // ═══════════════════════════════════════════
    // PROJECT MANAGEMENT
    // ═══════════════════════════════════════════

    fun loadProjects() {
        _projectsLoading.value = true
        viewModelScope.launch {
            when (val result = cloudRepo.listProjects()) {
                is AuthResult.Success -> _projects.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
            _projectsLoading.value = false
        }
    }

    fun loadMyPublishedModules() {
        _myPublishedLoading.value = true
        viewModelScope.launch {
            when (val result = cloudRepo.getMyPublishedModules()) {
                is AuthResult.Success -> _myPublishedModules.value = result.data.first
                is AuthResult.Error -> {} // silent fail
            }
            _myPublishedLoading.value = false
        }
    }

    fun createProject(name: String, description: String? = null,
                      githubRepo: String? = null, giteeRepo: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.createProject(name, description, githubRepo, giteeRepo)) {
                is AuthResult.Success -> {
                    _message.value = "项目创建成功"
                    loadProjects()
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun deleteProject(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.deleteProject(projectId)) {
                is AuthResult.Success -> {
                    _message.value = result.data
                    loadProjects()
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    /** Server-side upload (APK → our server → GitHub) */
    fun publishVersion(projectId: Int, apkFile: java.io.File, versionCode: Int,
                       versionName: String, title: String? = null,
                       changelog: String? = null, uploadTo: String = "github") {
        _publishLoading.value = true
        viewModelScope.launch {
            _message.value = "正在上传 APK 到服务器..."
            when (val result = cloudRepo.publishVersion(
                projectId, apkFile, versionCode, versionName, title, changelog, uploadTo
            )) {
                is AuthResult.Success -> {
                    _message.value = "版本发布成功！APK 已上传到 GitHub"
                    loadVersions(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
            _publishLoading.value = false
        }
    }

    /**
     * Direct upload: APK goes from user's device → GitHub directly.
     * Server only provides a temporary token (1h, scoped).
     *
     * Flow: requestToken → directUpload → confirmVersion
     */
    fun publishVersionDirect(projectId: Int, apkFile: java.io.File, versionCode: Int,
                              versionName: String, title: String? = null,
                              changelog: String? = null, uploadTo: String = "github") {
        
        // 智能路由：如果是纯 Gitee 或 两者都传，直接走服务器中转（国内服务器传国内 Gitee 最稳，选两者时也只需传给服务器一次）
        if (uploadTo == "gitee" || uploadTo == "both") {
            publishVersion(projectId, apkFile, versionCode, versionName, title, changelog, uploadTo)
            return
        }

        _publishLoading.value = true
        _publishProgress.value = 0f
        viewModelScope.launch {
            try {
                // Step 1: Get temporary upload token from our server
                _message.value = "正在获取上传令牌..."
                val tokenResult = cloudRepo.requestUploadToken(
                    projectId, versionCode, versionName, title, changelog, apkFile.name
                )
                when (tokenResult) {
                    is AuthResult.Error -> {
                        // Fallback: if GitHub App not configured, use server relay
                        if (tokenResult.message.contains("503") || tokenResult.message.contains("not configured")) {
                            _message.value = "直传未配置，改用服务器中转..."
                            publishVersion(projectId, apkFile, versionCode, versionName, title, changelog)
                            return@launch
                        }
                        _message.value = tokenResult.message
                        _publishLoading.value = false
                        return@launch
                    }
                    is AuthResult.Success -> {
                        val uploadInfo = tokenResult.data

                        // Step 2: Upload APK directly to GitHub
                        _message.value = "正在直传 APK 到 GitHub..."
                        val uploadResult = cloudRepo.directUploadToGithub(
                            uploadInfo, apkFile
                        ) { progress ->
                            _publishProgress.value = progress
                        }

                        when (uploadResult) {
                            is AuthResult.Error -> {
                                _message.value = "直连 GitHub 失败，切换服务器中转..."
                                // 国内网络有可能连不上 GitHub，自动回退到服务器上传
                                publishVersion(projectId, apkFile, versionCode, versionName, title, changelog, "github")
                                return@launch
                            }
                            is AuthResult.Success -> {
                                val downloadUrl = uploadResult.data

                                // Step 3: Confirm to our server
                                _message.value = "正在确认版本..."
                                val confirmResult = cloudRepo.confirmDirectUpload(
                                    projectId, versionCode, versionName, title, changelog,
                                    downloadUrl, apkFile.length()
                                )

                                when (confirmResult) {
                                    is AuthResult.Success -> {
                                        _message.value = "✅ 版本发布成功！APK 已直传到 GitHub"
                                        loadVersions(projectId)
                                    }
                                    is AuthResult.Error -> {
                                        _message.value = "APK 已上传但记录失败: ${confirmResult.message}"
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _message.value = "发布失败: ${e.message}"
            }
            _publishLoading.value = false
            _publishProgress.value = 0f
        }
    }

    fun loadVersions(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.listVersions(projectId)) {
                is AuthResult.Success -> _versions.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun loadAnalytics(projectId: Int, days: Int = 7) {
        viewModelScope.launch {
            when (val result = cloudRepo.getAnalytics(projectId, days)) {
                is AuthResult.Success -> _analytics.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // PROJECT ACTIVATION CODES
    // ═══════════════════════════════════════════

    private val _projectCodes = MutableStateFlow<List<ProjectActivationCode>>(emptyList())
    val projectCodes: StateFlow<List<ProjectActivationCode>> = _projectCodes.asStateFlow()

    fun loadProjectCodes(projectId: Int, status: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.listProjectCodes(projectId, status)) {
                is AuthResult.Success -> _projectCodes.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun generateProjectCodes(projectId: Int, count: Int = 10, maxUses: Int = 1, prefix: String = "") {
        viewModelScope.launch {
            when (val result = cloudRepo.generateProjectCodes(projectId, count, maxUses, prefix)) {
                is AuthResult.Success -> {
                    _message.value = "生成了 ${result.data.size} 个激活码"
                    loadProjectCodes(projectId) // 刷新列表
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // PROJECT ANNOUNCEMENTS
    // ═══════════════════════════════════════════

    private val _projectAnnouncements = MutableStateFlow<List<ProjectAnnouncement>>(emptyList())
    val projectAnnouncements: StateFlow<List<ProjectAnnouncement>> = _projectAnnouncements.asStateFlow()

    fun loadProjectAnnouncements(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.listProjectAnnouncements(projectId)) {
                is AuthResult.Success -> _projectAnnouncements.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun createProjectAnnouncement(projectId: Int, title: String, content: String, priority: Int = 0) {
        viewModelScope.launch {
            when (val result = cloudRepo.createProjectAnnouncement(projectId, title, content, priority)) {
                is AuthResult.Success -> {
                    _message.value = "公告已创建"
                    loadProjectAnnouncements(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun deleteProjectAnnouncement(projectId: Int, annId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.deleteProjectAnnouncement(projectId, annId)) {
                is AuthResult.Success -> {
                    _message.value = result.data
                    loadProjectAnnouncements(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // PROJECT REMOTE CONFIGS
    // ═══════════════════════════════════════════

    private val _projectConfigs = MutableStateFlow<List<ProjectConfig>>(emptyList())
    val projectConfigs: StateFlow<List<ProjectConfig>> = _projectConfigs.asStateFlow()

    fun loadProjectConfigs(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.listProjectConfigs(projectId)) {
                is AuthResult.Success -> _projectConfigs.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun createProjectConfig(projectId: Int, key: String, value: String, description: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.createProjectConfig(projectId, key, value, description)) {
                is AuthResult.Success -> {
                    _message.value = "配置已添加"
                    loadProjectConfigs(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun deleteProjectConfig(projectId: Int, cfgId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.deleteProjectConfig(projectId, cfgId)) {
                is AuthResult.Success -> {
                    _message.value = result.data
                    loadProjectConfigs(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // WEBHOOKS
    // ═══════════════════════════════════════════

    private val _webhooks = MutableStateFlow<List<ProjectWebhook>>(emptyList())
    val webhooks: StateFlow<List<ProjectWebhook>> = _webhooks.asStateFlow()

    fun loadWebhooks(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.listWebhooks(projectId)) {
                is AuthResult.Success -> _webhooks.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun createWebhook(projectId: Int, url: String, events: List<String>, secret: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.createWebhook(projectId, url, events, secret)) {
                is AuthResult.Success -> {
                    _message.value = "Webhook 已创建"
                    loadWebhooks(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun deleteWebhook(projectId: Int, webhookId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.deleteWebhook(projectId, webhookId)) {
                is AuthResult.Success -> {
                    _message.value = result.data
                    loadWebhooks(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // BACKUPS
    // ═══════════════════════════════════════════

    private val _backups = MutableStateFlow<List<BackupRecord>>(emptyList())
    val backups: StateFlow<List<BackupRecord>> = _backups.asStateFlow()

    private val _backupLoading = MutableStateFlow(false)
    val backupLoading: StateFlow<Boolean> = _backupLoading.asStateFlow()

    fun loadBackups(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.listBackups(projectId)) {
                is AuthResult.Success -> _backups.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun createBackup(projectId: Int, platform: String, zipFile: java.io.File) {
        _backupLoading.value = true
        viewModelScope.launch {
            _message.value = "正在备份..."
            when (val result = cloudRepo.createBackup(projectId, platform, zipFile)) {
                is AuthResult.Success -> {
                    _message.value = if (result.data.status == "success") "备份成功" else "备份失败: ${result.data.status}"
                    loadBackups(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
            _backupLoading.value = false
        }
    }

    // ═══════════════════════════════════════════
    // MANIFEST SYNC
    // ═══════════════════════════════════════════

    private val _manifestSyncState = MutableStateFlow(ManifestSyncState())
    val manifestSyncState: StateFlow<ManifestSyncState> = _manifestSyncState.asStateFlow()

    fun uploadManifest(projectId: Int, manifestJson: String, forceOverwrite: Boolean = false) {
        viewModelScope.launch {
            _manifestSyncState.value = _manifestSyncState.value.copy(syncing = true)
            // forceOverwrite: 传 version=0 跳过服务端的乐观并发检查
            val currentVersion = if (forceOverwrite) 0 else _manifestSyncState.value.localVersion
            when (val result = cloudRepo.uploadManifest(projectId, manifestJson, currentVersion)) {
                is AuthResult.Success -> {
                    if (result.data.conflict) {
                        _message.value = "同步冲突：云端有更新的版本，请先拉取"
                        _manifestSyncState.value = _manifestSyncState.value.copy(
                            syncing = false, hasConflict = true,
                            cloudVersion = result.data.manifestVersion
                        )
                    } else {
                        _message.value = "Manifest 已同步到云端"
                        _manifestSyncState.value = _manifestSyncState.value.copy(
                            syncing = false, hasConflict = false,
                            localVersion = result.data.manifestVersion,
                            cloudVersion = result.data.manifestVersion,
                            lastSyncedAt = result.data.syncedAt
                        )
                    }
                }
                is AuthResult.Error -> {
                    _message.value = result.message
                    _manifestSyncState.value = _manifestSyncState.value.copy(syncing = false)
                }
            }
        }
    }

    fun downloadManifest(projectId: Int, onResult: (ManifestDownloadResult) -> Unit) {
        viewModelScope.launch {
            _manifestSyncState.value = _manifestSyncState.value.copy(syncing = true)
            when (val result = cloudRepo.downloadManifest(projectId)) {
                is AuthResult.Success -> {
                    _manifestSyncState.value = _manifestSyncState.value.copy(
                        syncing = false, hasConflict = false,
                        cloudVersion = result.data.manifestVersion,
                        localVersion = result.data.manifestVersion,
                        lastSyncedAt = result.data.syncedAt
                    )
                    onResult(result.data)
                }
                is AuthResult.Error -> {
                    _message.value = result.message
                    _manifestSyncState.value = _manifestSyncState.value.copy(syncing = false)
                }
            }
        }
    }

    fun checkManifestStatus(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.downloadManifest(projectId)) {
                is AuthResult.Success -> {
                    _manifestSyncState.value = _manifestSyncState.value.copy(
                        cloudVersion = result.data.manifestVersion,
                        lastSyncedAt = result.data.syncedAt,
                        hasCloudManifest = result.data.manifestJson != null
                    )
                }
                is AuthResult.Error -> { /* silent */ }
            }
        }
    }

    // ═══════════════════════════════════════════
    // MODULE STORE
    // ═══════════════════════════════════════════

    private val _storeModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val storeModules: StateFlow<List<StoreModuleInfo>> = _storeModules.asStateFlow()

    private val _storeLoading = MutableStateFlow(false)
    val storeLoading: StateFlow<Boolean> = _storeLoading.asStateFlow()

    private val _storeTotal = MutableStateFlow(0)
    val storeTotal: StateFlow<Int> = _storeTotal.asStateFlow()

    fun loadStoreModules(category: String? = null, search: String? = null, sort: String = "downloads", order: String = "desc", page: Int = 1) {
        _storeLoading.value = true
        viewModelScope.launch {
            when (val result = cloudRepo.listStoreModules(category, search, sort, order, page)) {
                is AuthResult.Success -> {
                    _storeModules.value = result.data.first
                    _storeTotal.value = result.data.second
                }
                is AuthResult.Error -> _message.value = result.message
            }
            _storeLoading.value = false
        }
    }

    fun publishModule(module: com.webtoapp.core.extension.ExtensionModule) {
        viewModelScope.launch {
            _message.value = "发布中..."
            val shareCode = module.toShareCode()
            when (val result = cloudRepo.publishModule(
                name = module.name,
                description = module.description,
                icon = module.icon,
                category = module.category.name,
                tags = module.tags.joinToString(","),
                versionName = module.version.name,
                versionCode = module.version.code,
                shareCode = shareCode
            )) {
                is AuthResult.Success -> _message.value = "模块已发布到市场"
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun downloadStoreModule(moduleId: Int, onResult: (String) -> Unit, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.downloadStoreModule(moduleId)) {
                is AuthResult.Success -> onResult(result.data)
                is AuthResult.Error -> {
                    _message.value = result.message
                    onError?.invoke(result.message)
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // REMOTE SCRIPTS
    // ═══════════════════════════════════════════

    private val _remoteScripts = MutableStateFlow<List<RemoteScriptInfo>>(emptyList())
    val remoteScripts: StateFlow<List<RemoteScriptInfo>> = _remoteScripts.asStateFlow()

    fun loadRemoteScripts(projectId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.listRemoteScripts(projectId)) {
                is AuthResult.Success -> _remoteScripts.value = result.data
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun createRemoteScript(projectId: Int, name: String, code: String,
                           description: String? = null, runAt: String = "document_end",
                           urlPattern: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.createRemoteScript(projectId, name, code, description, runAt, urlPattern)) {
                is AuthResult.Success -> {
                    _message.value = "脚本已创建"
                    loadRemoteScripts(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun toggleRemoteScript(projectId: Int, scriptId: Int, isActive: Boolean) {
        viewModelScope.launch {
            when (val result = cloudRepo.updateRemoteScript(projectId, scriptId, mapOf("is_active" to isActive))) {
                is AuthResult.Success -> loadRemoteScripts(projectId)
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun updateRemoteScript(projectId: Int, scriptId: Int, fields: Map<String, Any?>) {
        viewModelScope.launch {
            when (val result = cloudRepo.updateRemoteScript(projectId, scriptId, fields)) {
                is AuthResult.Success -> {
                    _message.value = "脚本已更新"
                    loadRemoteScripts(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun deleteRemoteScript(projectId: Int, scriptId: Int) {
        viewModelScope.launch {
            when (val result = cloudRepo.deleteRemoteScript(projectId, scriptId)) {
                is AuthResult.Success -> {
                    _message.value = "脚本已删除"
                    loadRemoteScripts(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // ACTIVATION CODE PREVIEW
    // ═══════════════════════════════════════════

    private val _previewState = MutableStateFlow<FormState>(FormState.Idle)
    val previewState: StateFlow<FormState> = _previewState.asStateFlow()

    private val _redeemPreview = MutableStateFlow<RedeemPreview?>(null)
    val redeemPreview: StateFlow<RedeemPreview?> = _redeemPreview.asStateFlow()

    fun previewRedeem(code: String) {
        if (_previewState.value is FormState.Loading) return
        _previewState.value = FormState.Loading
        viewModelScope.launch {
            when (val result = cloudRepo.previewRedeemCode(code)) {
                is AuthResult.Success -> {
                    _redeemPreview.value = result.data
                    _previewState.value = FormState.Success("预览成功")
                }
                is AuthResult.Error -> {
                    _previewState.value = FormState.Error(result.message)
                }
            }
        }
    }

    fun resetPreviewState() {
        _previewState.value = FormState.Idle
        _redeemPreview.value = null
    }

    // ═══════════════════════════════════════════
    // PROJECT UPDATE
    // ═══════════════════════════════════════════

    fun updateProject(projectId: Int, name: String? = null, description: String? = null,
                      githubRepo: String? = null, giteeRepo: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.updateProject(projectId, name, description, githubRepo, giteeRepo)) {
                is AuthResult.Success -> {
                    _message.value = "项目已更新"
                    loadProjects()
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    // ═══════════════════════════════════════════
    // R2 CLOUD STORAGE PUBLISH
    // ═══════════════════════════════════════════

    fun publishVersionR2(projectId: Int, apkFile: java.io.File, versionCode: Int,
                          versionName: String, title: String? = null, changelog: String? = null) {
        _publishLoading.value = true
        viewModelScope.launch {
            _message.value = "正在上传 APK 到 R2 云存储..."
            when (val result = cloudRepo.publishVersionR2(projectId, apkFile, versionCode, versionName, title, changelog)) {
                is AuthResult.Success -> {
                    _message.value = "✅ 版本已发布到 R2 云存储 (CDN 加速)"
                    loadVersions(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
            _publishLoading.value = false
        }
    }

    // ═══════════════════════════════════════════
    // FCM PUSH
    // ═══════════════════════════════════════════

    private val _pushHistory = MutableStateFlow<List<PushHistoryItem>>(emptyList())
    val pushHistory: StateFlow<List<PushHistoryItem>> = _pushHistory.asStateFlow()

    fun registerPushToken(projectId: Int, fcmToken: String, deviceId: String) {
        viewModelScope.launch {
            when (val result = cloudRepo.registerPushToken(projectId, fcmToken, deviceId)) {
                is AuthResult.Success -> { /* silent success */ }
                is AuthResult.Error -> { /* silent fail */ }
            }
        }
    }

    fun sendPush(projectId: Int, title: String, body: String,
                 targetType: String = "all", targetUserIds: List<Int>? = null) {
        viewModelScope.launch {
            _message.value = "正在发送推送..."
            when (val result = cloudRepo.sendPushNotification(projectId, title, body, targetType, targetUserIds)) {
                is AuthResult.Success -> {
                    _message.value = result.data
                    loadPushHistory(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }

    fun loadPushHistory(projectId: Int, page: Int = 1) {
        viewModelScope.launch {
            val result = cloudRepo.getPushHistory(projectId, page)
            result.onSuccess { response ->
                _pushHistory.value = response.records
            }.onFailure { e ->
                _message.value = e.message ?: "加载推送历史失败"
            }
        }
    }

    // ═══════════════════════════════════════════
    // BACKUP DOWNLOAD
    // ═══════════════════════════════════════════

    fun downloadBackup(projectId: Int, backupId: Int, onUrl: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = cloudRepo.downloadBackup(projectId, backupId)) {
                is AuthResult.Success -> onUrl(result.data)
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }
}

data class ManifestSyncState(
    val syncing: Boolean = false,
    val localVersion: Int = 0,
    val cloudVersion: Int = 0,
    val lastSyncedAt: String? = null,
    val hasConflict: Boolean = false,
    val hasCloudManifest: Boolean = false
)
