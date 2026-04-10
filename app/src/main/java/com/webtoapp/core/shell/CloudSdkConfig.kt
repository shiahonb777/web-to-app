package com.webtoapp.core.shell

import com.google.gson.annotations.SerializedName

/**
 * Cloud SDK 配置 — 嵌入导出 APK 的云服务 SDK 参数
 *
 * 当用户将应用关联到云项目后，构建 APK 时会把此配置写入 app_config.json。
 * Shell 模式启动时 CloudSdkManager 读取此配置并初始化各项云服务功能。
 *
 * SDK 功能:
 * 1. 更新检查 — 启动时/定时检查新版本，弹出更新对话框
 * 2. 公告展示 — 获取并展示项目公告
 * 3. 远程配置 — 动态配置键值对（如开关、URL 等）
 * 4. 激活码验证 — 上线验证机制
 * 5. 统计上报 — 设备信息、使用数据、崩溃次数
 * 6. FCM 推送 — (Ultra) 接收实时推送通知
 */
data class CloudSdkConfig(
    /** 是否启用云 SDK */
    @SerializedName("enabled")
    val enabled: Boolean = false,

    /** 云项目唯一标识 (UUID) — 从服务器获取 */
    @SerializedName("projectKey")
    val projectKey: String = "",

    /** API 基础 URL */
    @SerializedName("apiBaseUrl")
    val apiBaseUrl: String = DEFAULT_API_BASE_URL,

    // ─── 功能开关 ───

    /** 启用应用更新检查 */
    @SerializedName("updateCheckEnabled")
    val updateCheckEnabled: Boolean = true,

    /** 启用公告获取 */
    @SerializedName("announcementEnabled")
    val announcementEnabled: Boolean = true,

    /** 启用远程配置 */
    @SerializedName("remoteConfigEnabled")
    val remoteConfigEnabled: Boolean = true,

    /** 启用激活码验证（与原有离线激活码互斥） */
    @SerializedName("activationCodeEnabled")
    val activationCodeEnabled: Boolean = false,

    /** 启用统计上报 */
    @SerializedName("statsReportEnabled")
    val statsReportEnabled: Boolean = true,

    /** 启用 FCM 推送（仅 Ultra） */
    @SerializedName("fcmPushEnabled")
    val fcmPushEnabled: Boolean = false,

    /** 启用远程脚本注入（热更） */
    @SerializedName("remoteScriptEnabled")
    val remoteScriptEnabled: Boolean = false,

    // ─── 更新检查配置 ───

    /** 更新检查间隔（秒），默认 1 小时 */
    @SerializedName("updateCheckInterval")
    val updateCheckInterval: Int = 3600,

    /** 是否启用强制更新逻辑 */
    @SerializedName("forceUpdateEnabled")
    val forceUpdateEnabled: Boolean = false,

    /** 更新弹窗标题（空则使用默认） */
    @SerializedName("updateDialogTitle")
    val updateDialogTitle: String = "",

    /** 更新弹窗确认按钮文字 */
    @SerializedName("updateDialogButtonText")
    val updateDialogButtonText: String = "",

    // ─── 公告配置 ───

    /** 公告展示模板 */
    @SerializedName("announcementTemplate")
    val announcementTemplate: String = "MATERIAL",

    /** 是否只在首次启动展示公告 */
    @SerializedName("announcementShowOnce")
    val announcementShowOnce: Boolean = true,

    // ─── 统计上报配置 ───

    /** 统计上报间隔（秒），默认 1 小时 */
    @SerializedName("statsReportInterval")
    val statsReportInterval: Int = 3600,

    /** 是否上报崩溃信息 */
    @SerializedName("reportCrashes")
    val reportCrashes: Boolean = true,

    // ─── 激活码配置 ───

    /** 激活码验证时是否绑定设备 */
    @SerializedName("activationBindDevice")
    val activationBindDevice: Boolean = true,

    /** 激活码弹窗标题 */
    @SerializedName("activationDialogTitle")
    val activationDialogTitle: String = "",

    /** 激活码弹窗副标题 */
    @SerializedName("activationDialogSubtitle")
    val activationDialogSubtitle: String = "",

    // ─── FCM 推送配置 ───

    /** FCM 项目 Sender ID */
    @SerializedName("fcmSenderId")
    val fcmSenderId: String = "",

    /** FCM 通知渠道 ID */
    @SerializedName("fcmChannelId")
    val fcmChannelId: String = "cloud_push",

    /** FCM 通知渠道名称 */
    @SerializedName("fcmChannelName")
    val fcmChannelName: String = "推送通知",

    // ─── OTA 应用内更新配置 ───

    /** 是否启用应用内下载（否则跳转浏览器） */
    @SerializedName("inAppDownload")
    val inAppDownload: Boolean = true,

    /** 是否在通知栏显示下载进度 */
    @SerializedName("showDownloadNotification")
    val showDownloadNotification: Boolean = true,

    /** 下载完成后是否自动弹出安装 */
    @SerializedName("autoInstallAfterDownload")
    val autoInstallAfterDownload: Boolean = true
) {
    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.shiaho.sbs"

        /** 禁用的默认实例 */
        val DISABLED = CloudSdkConfig(enabled = false)
    }

    /** 是否配置有效 */
    fun isValid(): Boolean = enabled && projectKey.isNotBlank()

    /** 获取完整 SDK API URL */
    fun getSdkApiUrl(endpoint: String): String {
        val base = apiBaseUrl.trimEnd('/')
        return "$base/api/v1/sdk/$projectKey/$endpoint"
    }
}
