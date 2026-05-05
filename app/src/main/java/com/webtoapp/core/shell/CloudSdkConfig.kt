package com.webtoapp.core.shell

import com.google.gson.annotations.SerializedName















data class CloudSdkConfig(

    @SerializedName("enabled")
    val enabled: Boolean = false,


    @SerializedName(value = "runtimeKey")
    val runtimeKey: String = "",


    @SerializedName("projectKey")
    val projectKey: String = "",


    @SerializedName("apiBaseUrl")
    val apiBaseUrl: String = DEFAULT_API_BASE_URL,




    @SerializedName("updateCheckEnabled")
    val updateCheckEnabled: Boolean = true,


    @SerializedName("announcementEnabled")
    val announcementEnabled: Boolean = true,


    @SerializedName("remoteConfigEnabled")
    val remoteConfigEnabled: Boolean = true,


    @SerializedName("activationCodeEnabled")
    val activationCodeEnabled: Boolean = false,


    @SerializedName("statsReportEnabled")
    val statsReportEnabled: Boolean = true,


    @SerializedName("fcmPushEnabled")
    val fcmPushEnabled: Boolean = false,


    @SerializedName("remoteScriptEnabled")
    val remoteScriptEnabled: Boolean = false,




    @SerializedName("updateCheckInterval")
    val updateCheckInterval: Int = 3600,


    @SerializedName("forceUpdateEnabled")
    val forceUpdateEnabled: Boolean = false,


    @SerializedName("updateDialogTitle")
    val updateDialogTitle: String = "",


    @SerializedName("updateDialogButtonText")
    val updateDialogButtonText: String = "",




    @SerializedName("announcementTemplate")
    val announcementTemplate: String = "MATERIAL",


    @SerializedName("announcementShowOnce")
    val announcementShowOnce: Boolean = true,




    @SerializedName("statsReportInterval")
    val statsReportInterval: Int = 3600,


    @SerializedName("reportCrashes")
    val reportCrashes: Boolean = true,




    @SerializedName("activationBindDevice")
    val activationBindDevice: Boolean = true,


    @SerializedName("activationDialogTitle")
    val activationDialogTitle: String = "",


    @SerializedName("activationDialogSubtitle")
    val activationDialogSubtitle: String = "",




    @SerializedName("fcmSenderId")
    val fcmSenderId: String = "",


    @SerializedName("fcmChannelId")
    val fcmChannelId: String = "cloud_push",


    @SerializedName("fcmChannelName")
    val fcmChannelName: String = "推送通知",




    @SerializedName("inAppDownload")
    val inAppDownload: Boolean = true,


    @SerializedName("showDownloadNotification")
    val showDownloadNotification: Boolean = true,


    @SerializedName("autoInstallAfterDownload")
    val autoInstallAfterDownload: Boolean = true
) {
    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.shiaho.sbs"


        val DISABLED = CloudSdkConfig(enabled = false)
    }


    fun resolvedRuntimeKey(): String = runtimeKey.trim()


    fun isValid(): Boolean = enabled && resolvedRuntimeKey().isNotBlank()


    fun getSdkApiUrl(endpoint: String): String {
        val base = apiBaseUrl.trimEnd('/')
        return "$base/api/v1/sdk/${resolvedRuntimeKey()}/$endpoint"
    }
}
