package com.webtoapp.data.model

/**
 * Cloud project linking configuration (editor side).
 *
 * Persisted in the editor database, not shipped to exported apps.
 */
data class CloudAppConfig(
    val enabled: Boolean = false,
    val projectId: Int = 0,
    val projectKey: String = "",
    val projectName: String = "",
    val updateCheckEnabled: Boolean = true,
    val announcementEnabled: Boolean = true,
    val remoteConfigEnabled: Boolean = true,
    val activationCodeEnabled: Boolean = false,
    val statsReportEnabled: Boolean = true,
    val fcmPushEnabled: Boolean = false,
    val remoteScriptEnabled: Boolean = false,
    val reportCrashes: Boolean = true,
    val updateCheckInterval: Int = 3600,
    val forceUpdateEnabled: Boolean = false,
    val statsReportInterval: Int = 3600,
) {
    fun toCloudSdkConfig(): com.webtoapp.core.shell.CloudSdkConfig {
        return com.webtoapp.core.shell.CloudSdkConfig(
            enabled = enabled && projectKey.isNotBlank(),
            projectKey = projectKey,
            updateCheckEnabled = updateCheckEnabled,
            announcementEnabled = announcementEnabled,
            remoteConfigEnabled = remoteConfigEnabled,
            activationCodeEnabled = activationCodeEnabled,
            statsReportEnabled = statsReportEnabled,
            fcmPushEnabled = fcmPushEnabled,
            remoteScriptEnabled = remoteScriptEnabled,
            updateCheckInterval = updateCheckInterval,
            forceUpdateEnabled = forceUpdateEnabled,
            statsReportInterval = statsReportInterval,
            reportCrashes = reportCrashes,
        )
    }
}
