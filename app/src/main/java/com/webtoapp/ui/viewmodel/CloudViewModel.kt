package com.webtoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch






class CloudViewModel(
    private val cloudRepo: CloudRepository,
    private val installedTracker: InstalledItemsTracker
) : ViewModel() {



    private val _redeemState = MutableStateFlow<FormState>(FormState.Idle)
    val redeemState: StateFlow<FormState> = _redeemState.asStateFlow()

    private val _activationHistory = MutableStateFlow<List<ActivationRecord>>(emptyList())
    val activationHistory: StateFlow<List<ActivationRecord>> = _activationHistory.asStateFlow()



    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    private val _devicesLoading = MutableStateFlow(false)
    val devicesLoading: StateFlow<Boolean> = _devicesLoading.asStateFlow()



    private val _announcements = MutableStateFlow<List<AnnouncementData>>(emptyList())
    val announcements: StateFlow<List<AnnouncementData>> = _announcements.asStateFlow()



    private val _projects = MutableStateFlow<List<CloudProject>>(emptyList())
    val projects: StateFlow<List<CloudProject>> = _projects.asStateFlow()

    private val _projectsLoading = MutableStateFlow(false)
    val projectsLoading: StateFlow<Boolean> = _projectsLoading.asStateFlow()



    private val _myPublishedModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val myPublishedModules: StateFlow<List<StoreModuleInfo>> = _myPublishedModules.asStateFlow()

    private val _myPublishedLoading = MutableStateFlow(false)
    val myPublishedLoading: StateFlow<Boolean> = _myPublishedLoading.asStateFlow()



    private val _analytics = MutableStateFlow<AnalyticsData?>(null)
    val analytics: StateFlow<AnalyticsData?> = _analytics.asStateFlow()



    private val _versions = MutableStateFlow<List<ProjectVersion>>(emptyList())
    val versions: StateFlow<List<ProjectVersion>> = _versions.asStateFlow()

    private val _publishLoading = MutableStateFlow(false)
    val publishLoading: StateFlow<Boolean> = _publishLoading.asStateFlow()

    private val _publishProgress = MutableStateFlow(0f)
    val publishProgress: StateFlow<Float> = _publishProgress.asStateFlow()

    private val _publishErrorReport = MutableStateFlow<PublishErrorReport?>(null)
    val publishErrorReport: StateFlow<PublishErrorReport?> = _publishErrorReport.asStateFlow()



    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }
    fun clearPublishErrorReport() { _publishErrorReport.value = null }





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
                    loadDevices()
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }





    fun loadAnnouncements(appVersion: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.getAnnouncements(appVersion)) {
                is AuthResult.Success -> _announcements.value = result.data
                is AuthResult.Error -> {}
            }
        }
    }





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
                is AuthResult.Error -> {}
            }
            _myPublishedLoading.value = false
        }
    }

    fun createProject(name: String, description: String? = null,
                      githubRepo: String? = null, giteeRepo: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.createProject(name, description, githubRepo, giteeRepo)) {
                is AuthResult.Success -> {
                    _message.value = Strings.projectCreatedSuccess
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


    fun publishVersion(projectId: Int, apkFile: java.io.File, versionCode: Int,
                       versionName: String, title: String? = null,
                       changelog: String? = null, uploadTo: String = "github") {
        _publishLoading.value = true
        _publishErrorReport.value = null
        viewModelScope.launch {
            _message.value = Strings.uploadingApkToServer
            when (val result = cloudRepo.publishVersion(
                projectId, apkFile, versionCode, versionName, title, changelog, uploadTo
            )) {
                is AuthResult.Success -> {
                    _message.value = Strings.versionPublishSuccess
                    loadVersions(projectId)
                }
                is AuthResult.Error -> {
                    _publishErrorReport.value = buildPublishErrorReport(
                        stage = Strings.serverUploadStage,
                        summary = result.message,
                        projectId = projectId,
                        apkFile = apkFile,
                        versionCode = versionCode,
                        versionName = versionName,
                        uploadTo = uploadTo
                    )
                    _message.value = result.message
                }
            }
            _publishLoading.value = false
        }
    }







    fun publishVersionDirect(projectId: Int, apkFile: java.io.File, versionCode: Int,
                              versionName: String, title: String? = null,
                              changelog: String? = null, uploadTo: String = "github") {
        _publishLoading.value = true
        _publishProgress.value = 0f
        _publishErrorReport.value = null
        viewModelScope.launch {
            try {
                if (uploadTo != "github") {
                    val report = buildPublishErrorReport(
                        stage = Strings.paramValidationStage,
                        summary = Strings.directUploadOnlyGithub.format(uploadTo),
                        projectId = projectId,
                        apkFile = apkFile,
                        versionCode = versionCode,
                        versionName = versionName,
                        uploadTo = uploadTo
                    )
                    _publishErrorReport.value = report
                    _message.value = report.summary
                    _publishLoading.value = false
                    return@launch
                }


                _message.value = Strings.gettingUploadToken
                val tokenResult = cloudRepo.requestUploadToken(
                    projectId, versionCode, versionName, title, changelog, apkFile.name
                )
                when (tokenResult) {
                    is AuthResult.Error -> {
                        _publishErrorReport.value = buildPublishErrorReport(
                            stage = Strings.getUploadTokenStage,
                            summary = tokenResult.message,
                            projectId = projectId,
                            apkFile = apkFile,
                            versionCode = versionCode,
                            versionName = versionName,
                            uploadTo = uploadTo
                        )
                        _message.value = tokenResult.message
                        _publishLoading.value = false
                        return@launch
                    }
                    is AuthResult.Success -> {
                        val uploadInfo = tokenResult.data


                        _message.value = Strings.directUploadingGithub
                        val uploadResult = cloudRepo.directUploadToGithub(
                            uploadInfo, apkFile
                        ) { progress ->
                            _publishProgress.value = progress
                        }

                        when (uploadResult) {
                            is AuthResult.Error -> {
                                _publishErrorReport.value = buildPublishErrorReport(
                                    stage = Strings.directGithubStage,
                                    summary = uploadResult.message,
                                    projectId = projectId,
                                    apkFile = apkFile,
                                    versionCode = versionCode,
                                    versionName = versionName,
                                    uploadTo = uploadTo
                                )
                                _message.value = uploadResult.message
                                return@launch
                            }
                            is AuthResult.Success -> {
                                val downloadUrl = uploadResult.data


                                _message.value = Strings.confirmingVersion
                                val confirmResult = cloudRepo.confirmDirectUpload(
                                    projectId, versionCode, versionName, title, changelog,
                                    downloadUrl, apkFile.length()
                                )

                                when (confirmResult) {
                                    is AuthResult.Success -> {
                                        _message.value = Strings.versionPublishDirectSuccess
                                        loadVersions(projectId)
                                    }
                                    is AuthResult.Error -> {
                                        val summary = Strings.apkUploadedServerRecordFailed.format(confirmResult.message)
                                        _publishErrorReport.value = buildPublishErrorReport(
                                            stage = Strings.confirmVersionStage,
                                            summary = summary,
                                            projectId = projectId,
                                            apkFile = apkFile,
                                            versionCode = versionCode,
                                            versionName = versionName,
                                            uploadTo = uploadTo
                                        )
                                        _message.value = summary
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val summary = Strings.publishFailedSummary.format(e.message)
                _publishErrorReport.value = buildPublishErrorReport(
                    stage = Strings.uncaughtExceptionStage,
                    summary = summary,
                    projectId = projectId,
                    apkFile = apkFile,
                    versionCode = versionCode,
                    versionName = versionName,
                    uploadTo = uploadTo,
                    throwable = e
                )
                _message.value = summary
            }
            _publishLoading.value = false
            _publishProgress.value = 0f
        }
    }

    private fun buildPublishErrorReport(
        stage: String,
        summary: String,
        projectId: Int,
        apkFile: java.io.File,
        versionCode: Int,
        versionName: String,
        uploadTo: String,
        throwable: Throwable? = null
    ): PublishErrorReport {
        throwable?.let { AppLogger.e("CloudViewModel", "Publish failed at $stage", it) }
        val details = buildString {
            appendLine("WebToApp Publish Failure")
            appendLine("stage: $stage")
            appendLine("projectId: $projectId")
            appendLine("uploadTo: $uploadTo")
            appendLine("versionCode: $versionCode")
            appendLine("versionName: $versionName")
            appendLine("apkName: ${apkFile.name}")
            appendLine("apkPath: ${apkFile.absolutePath}")
            appendLine("apkSize: ${apkFile.length()}")
            appendLine("summary: $summary")
            if (throwable != null) {
                appendLine()
                appendLine("exception:")
                appendLine(android.util.Log.getStackTraceString(throwable))
            }
            appendLine()
            appendLine("recent_logs:")
            append(AppLogger.getRecentLogTail())
        }
        return PublishErrorReport(
            title = Strings.publishFailedTitle,
            summary = summary,
            stage = stage,
            details = details
        )
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
                    _message.value = Strings.activationCodesGenerated.format(result.data.size)
                    loadProjectCodes(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }





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
                    _message.value = Strings.announcementCreated
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
                    _message.value = Strings.configAdded
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
                    _message.value = Strings.cloudWebhookCreated
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
            _message.value = Strings.backingUp
            when (val result = cloudRepo.createBackup(projectId, platform, zipFile)) {
                is AuthResult.Success -> {
                    _message.value = if (result.data.status == "success") Strings.backupSuccess else Strings.backupFailed.format(result.data.status)
                    loadBackups(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
            _backupLoading.value = false
        }
    }





    private val _manifestSyncState = MutableStateFlow(ManifestSyncState())
    val manifestSyncState: StateFlow<ManifestSyncState> = _manifestSyncState.asStateFlow()

    fun uploadManifest(projectId: Int, manifestJson: String, forceOverwrite: Boolean = false) {
        viewModelScope.launch {
            _manifestSyncState.value = _manifestSyncState.value.copy(syncing = true)

            val currentVersion = if (forceOverwrite) 0 else _manifestSyncState.value.localVersion
            when (val result = cloudRepo.uploadManifest(projectId, manifestJson, currentVersion)) {
                is AuthResult.Success -> {
                    if (result.data.conflict) {
                        _message.value = Strings.syncConflict
                        _manifestSyncState.value = _manifestSyncState.value.copy(
                            syncing = false, hasConflict = true,
                            cloudVersion = result.data.manifestVersion
                        )
                    } else {
                        _message.value = Strings.manifestSynced
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
                is AuthResult.Error -> {  }
            }
        }
    }





    private val _storeModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val storeModules: StateFlow<List<StoreModuleInfo>> = _storeModules.asStateFlow()

    private val _storeLoading = MutableStateFlow(false)
    val storeLoading: StateFlow<Boolean> = _storeLoading.asStateFlow()

    private val _storeTotal = MutableStateFlow(0)
    val storeTotal: StateFlow<Int> = _storeTotal.asStateFlow()

    private val _featuredModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val featuredModules: StateFlow<List<StoreModuleInfo>> = _featuredModules.asStateFlow()


    private val _updatableModuleIds = MutableStateFlow<Set<Int>>(emptySet())
    val updatableModuleIds: StateFlow<Set<Int>> = _updatableModuleIds.asStateFlow()

    fun checkModuleUpdates() {
        val installedIds = installedTracker.getInstalledIds().mapNotNull { it.toIntOrNull() }
        if (installedIds.isEmpty()) return
        viewModelScope.launch {
            val updatable = mutableSetOf<Int>()
            installedIds.forEach { id ->
                val localCode = installedTracker.getInstalledVersionCode(id)
                try {
                    when (val result = cloudRepo.getStoreModuleById(id)) {
                        is AuthResult.Success -> {
                            if (result.data.versionCode > localCode) updatable.add(id)
                        }
                        else -> {}
                    }
                } catch (_: Exception) {}
            }
            _updatableModuleIds.value = updatable
        }
    }

    fun loadFeaturedModules() {
        viewModelScope.launch {
            when (val result = cloudRepo.getFeaturedModules(page = 1, size = 10)) {
                is AuthResult.Success -> _featuredModules.value = result.data
                is AuthResult.Error -> {}
            }
        }
    }

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
            _message.value = Strings.publishingModule
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
                is AuthResult.Success -> _message.value = Strings.modulePublishedToStore
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
                    _message.value = Strings.scriptCreated
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
                    _message.value = Strings.scriptUpdated
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
                    _message.value = Strings.scriptDeleted
                    loadRemoteScripts(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }





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
                    _previewState.value = FormState.Success(Strings.previewSuccess)
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





    fun updateProject(projectId: Int, name: String? = null, description: String? = null,
                      githubRepo: String? = null, giteeRepo: String? = null) {
        viewModelScope.launch {
            when (val result = cloudRepo.updateProject(projectId, name, description, githubRepo, giteeRepo)) {
                is AuthResult.Success -> {
                    _message.value = Strings.projectUpdated
                    loadProjects()
                }
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }





    fun publishVersionR2(projectId: Int, apkFile: java.io.File, versionCode: Int,
                          versionName: String, title: String? = null, changelog: String? = null) {
        _publishLoading.value = true
        viewModelScope.launch {
            _message.value = Strings.uploadingApkToR2
            when (val result = cloudRepo.publishVersionR2(projectId, apkFile, versionCode, versionName, title, changelog)) {
                is AuthResult.Success -> {
                    _message.value = Strings.versionPublishedToR2
                    loadVersions(projectId)
                }
                is AuthResult.Error -> _message.value = result.message
            }
            _publishLoading.value = false
        }
    }





    private val _pushHistory = MutableStateFlow<List<PushHistoryItem>>(emptyList())
    val pushHistory: StateFlow<List<PushHistoryItem>> = _pushHistory.asStateFlow()

    fun registerPushToken(projectId: Int, fcmToken: String, deviceId: String) {
        viewModelScope.launch {
            when (val result = cloudRepo.registerPushToken(projectId, fcmToken, deviceId)) {
                is AuthResult.Success -> {  }
                is AuthResult.Error -> {  }
            }
        }
    }

    fun sendPush(projectId: Int, title: String, body: String,
                 targetType: String = "all", targetUserIds: List<Int>? = null) {
        viewModelScope.launch {
            _message.value = Strings.sendingPush
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
                _message.value = e.message ?: Strings.pushHistoryLoadFailed
            }
        }
    }





    fun downloadBackup(projectId: Int, backupId: Int, onUrl: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = cloudRepo.downloadBackup(projectId, backupId)) {
                is AuthResult.Success -> onUrl(result.data)
                is AuthResult.Error -> _message.value = result.message
            }
        }
    }
}

data class PublishErrorReport(
    val title: String,
    val summary: String,
    val stage: String,
    val details: String
)

data class ManifestSyncState(
    val syncing: Boolean = false,
    val localVersion: Int = 0,
    val cloudVersion: Int = 0,
    val lastSyncedAt: String? = null,
    val hasConflict: Boolean = false,
    val hasCloudManifest: Boolean = false
)
