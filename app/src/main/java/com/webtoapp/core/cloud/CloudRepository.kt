package com.webtoapp.core.cloud

import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.auth.TokenManager
import com.webtoapp.core.logging.AppLogger

/**
 * 云服务仓库
 *
 * 封装 CloudApiClient，提供业务逻辑和缓存。
 */
class CloudRepository(
    private val cloudApi: CloudApiClient,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val TAG = "CloudRepository"
    }

    // ─── 激活码 ───

    suspend fun redeemCode(code: String): AuthResult<RedeemResult> {
        val trimmed = code.trim().uppercase()
        if (trimmed.isBlank()) return AuthResult.Error("请输入激活码")
        if (trimmed.length < 4) return AuthResult.Error("激活码格式不正确")
        return cloudApi.redeemCode(trimmed)
    }

    suspend fun previewRedeemCode(code: String): AuthResult<RedeemPreview> {
        val trimmed = code.trim().uppercase()
        if (trimmed.isBlank()) return AuthResult.Error("请输入激活码")
        if (trimmed.length < 4) return AuthResult.Error("激活码格式不正确")
        return cloudApi.previewRedeemCode(trimmed)
    }

    suspend fun getActivationHistory() = cloudApi.getActivationHistory()

    // ─── 设备管理 ───

    suspend fun getDevices() = cloudApi.getDevices()

    suspend fun removeDevice(deviceId: Int) = cloudApi.removeDevice(deviceId)

    // ─── 公告 ───

    suspend fun getAnnouncements(appVersion: String? = null) =
        cloudApi.getAnnouncements(appVersion)

    // ─── 更新检测 ───

    suspend fun checkAppUpdate(currentVersionCode: Int) =
        cloudApi.checkAppUpdate(currentVersionCode)

    // ─── 远程配置 ───

    suspend fun getRemoteConfig() = cloudApi.getRemoteConfig()

    // ─── 项目管理 ───

    suspend fun listProjects() = cloudApi.listProjects()

    suspend fun getMyPublishedModules() = cloudApi.getMyPublishedModules()

    suspend fun createProject(name: String, description: String? = null,
                              githubRepo: String? = null, giteeRepo: String? = null) =
        cloudApi.createProject(name, description, githubRepo, giteeRepo)

    suspend fun deleteProject(projectId: Int) = cloudApi.deleteProject(projectId)

    suspend fun publishVersion(
        projectId: Int,
        apkFile: java.io.File,
        versionCode: Int,
        versionName: String,
        title: String? = null,
        changelog: String? = null,
        uploadTo: String = "github"
    ) = cloudApi.publishVersion(projectId, apkFile, versionCode, versionName, title, changelog, uploadTo)

    suspend fun listVersions(projectId: Int) = cloudApi.listVersions(projectId)

    // ─── Direct Upload (Client → GitHub) ───

    suspend fun requestUploadToken(projectId: Int, versionCode: Int, versionName: String,
                                    title: String?, changelog: String?, fileName: String) =
        cloudApi.requestUploadToken(projectId, versionCode, versionName, title, changelog, fileName)

    suspend fun directUploadToGithub(uploadInfo: DirectUploadToken, apkFile: java.io.File,
                                      onProgress: (Float) -> Unit = {}) =
        cloudApi.directUploadToGithub(uploadInfo, apkFile, onProgress)

    suspend fun confirmDirectUpload(projectId: Int, versionCode: Int, versionName: String,
                                     title: String?, changelog: String?,
                                     downloadUrl: String, fileSize: Long) =
        cloudApi.confirmDirectUpload(projectId, versionCode, versionName, title, changelog, downloadUrl, fileSize)

    suspend fun getAnalytics(projectId: Int, days: Int = 7) =
        cloudApi.getAnalytics(projectId, days)

    // ─── 项目激活码 ───

    suspend fun generateProjectCodes(projectId: Int, count: Int = 10, maxUses: Int = 1, prefix: String = "") =
        cloudApi.generateProjectCodes(projectId, count, maxUses, prefix)

    suspend fun listProjectCodes(projectId: Int, status: String? = null, page: Int = 1) =
        cloudApi.listProjectCodes(projectId, status, page)

    // ─── 项目公告 ───

    suspend fun createProjectAnnouncement(projectId: Int, title: String, content: String, priority: Int = 0) =
        cloudApi.createProjectAnnouncement(projectId, title, content, priority)

    suspend fun listProjectAnnouncements(projectId: Int) =
        cloudApi.listProjectAnnouncements(projectId)

    suspend fun updateProjectAnnouncement(projectId: Int, annId: Int, title: String? = null,
                                           content: String? = null, isActive: Boolean? = null) =
        cloudApi.updateProjectAnnouncement(projectId, annId, title, content, isActive)

    suspend fun deleteProjectAnnouncement(projectId: Int, annId: Int) =
        cloudApi.deleteProjectAnnouncement(projectId, annId)

    // ─── 项目远程配置 ───

    suspend fun createProjectConfig(projectId: Int, key: String, value: String, description: String? = null) =
        cloudApi.createProjectConfig(projectId, key, value, description)

    suspend fun listProjectConfigs(projectId: Int) =
        cloudApi.listProjectConfigs(projectId)

    suspend fun updateProjectConfig(projectId: Int, cfgId: Int, value: String? = null,
                                     description: String? = null, isActive: Boolean? = null) =
        cloudApi.updateProjectConfig(projectId, cfgId, value, description, isActive)

    suspend fun deleteProjectConfig(projectId: Int, cfgId: Int) =
        cloudApi.deleteProjectConfig(projectId, cfgId)

    // ─── Webhooks ───

    suspend fun createWebhook(projectId: Int, url: String, events: List<String>, secret: String? = null) =
        cloudApi.createWebhook(projectId, url, events, secret)

    suspend fun listWebhooks(projectId: Int) =
        cloudApi.listWebhooks(projectId)

    suspend fun deleteWebhook(projectId: Int, webhookId: Int) =
        cloudApi.deleteWebhook(projectId, webhookId)

    // ─── Manifest 同步 ───

    suspend fun uploadManifest(projectId: Int, manifestJson: String, manifestVersion: Int) =
        cloudApi.uploadManifest(projectId, manifestJson, manifestVersion)

    suspend fun downloadManifest(projectId: Int) =
        cloudApi.downloadManifest(projectId)

    // ─── 模块市场 ───

    suspend fun listStoreModules(category: String? = null, search: String? = null,
                                  sort: String = "downloads", order: String = "desc",
                                  page: Int = 1, size: Int = 20) =
        cloudApi.listStoreModules(category, search, sort, order, page, size)

    suspend fun publishModule(name: String, description: String, icon: String?,
                               category: String?, tags: String?, versionName: String?,
                               versionCode: Int, shareCode: String) =
        cloudApi.publishModule(name, description, icon, category, tags, versionName, versionCode, shareCode)

    suspend fun downloadStoreModule(moduleId: Int) =
        cloudApi.downloadStoreModule(moduleId)

    // ─── 远程脚本 ───

    suspend fun listRemoteScripts(projectId: Int) =
        cloudApi.listRemoteScripts(projectId)

    suspend fun createRemoteScript(projectId: Int, name: String, code: String,
                                    description: String? = null, runAt: String = "document_end",
                                    urlPattern: String? = null, priority: Int = 0) =
        cloudApi.createRemoteScript(projectId, name, code, description, runAt, urlPattern, priority)

    suspend fun updateRemoteScript(projectId: Int, scriptId: Int, fields: Map<String, Any?>) =
        cloudApi.updateRemoteScript(projectId, scriptId, fields)

    suspend fun deleteRemoteScript(projectId: Int, scriptId: Int) =
        cloudApi.deleteRemoteScript(projectId, scriptId)

    // ─── 备份 ───

    suspend fun listBackups(projectId: Int) =
        cloudApi.listBackups(projectId)

    suspend fun createBackup(projectId: Int, platform: String, zipFile: java.io.File) =
        cloudApi.createBackup(projectId, platform, zipFile)

    suspend fun downloadBackup(projectId: Int, backupId: Int) =
        cloudApi.downloadBackup(projectId, backupId)

    // ─── 项目更新 ───

    suspend fun updateProject(projectId: Int, name: String? = null, description: String? = null,
                              githubRepo: String? = null, giteeRepo: String? = null) =
        cloudApi.updateProject(projectId, name, description, githubRepo, giteeRepo)

    // ─── R2 云存储发布 ───

    suspend fun publishVersionR2(projectId: Int, apkFile: java.io.File, versionCode: Int,
                                  versionName: String, title: String? = null,
                                  changelog: String? = null) =
        cloudApi.publishVersionR2(projectId, apkFile, versionCode, versionName, title, changelog)

    // ─── FCM 推送 ───

    suspend fun registerPushToken(projectId: Int, fcmToken: String, deviceId: String) =
        cloudApi.registerPushToken(projectId, fcmToken, deviceId)

    suspend fun sendPushNotification(projectId: Int, title: String, body: String,
                                      targetType: String = "all", targetUserIds: List<Int>? = null) =
        cloudApi.sendPushNotification(projectId, title, body, targetType, targetUserIds)

    suspend fun getPushHistory(projectId: Int, page: Int = 1) =
        cloudApi.getPushHistory(projectId, page)

    // ─── 细化分析 ───

    suspend fun getAnalyticsOverview(projectId: Int) =
        cloudApi.getAnalyticsOverview(projectId)

    suspend fun getAnalyticsTrend(projectId: Int, days: Int = 7) =
        cloudApi.getAnalyticsTrend(projectId, days)

    suspend fun getAnalyticsGeo(projectId: Int) =
        cloudApi.getAnalyticsGeo(projectId)

    suspend fun getAnalyticsDevices(projectId: Int) =
        cloudApi.getAnalyticsDevices(projectId)

    suspend fun getAnalyticsVersions(projectId: Int) =
        cloudApi.getAnalyticsVersions(projectId)

    // ─── 状态 ───

    fun isLoggedIn(): Boolean = tokenManager.getAccessToken() != null
}
