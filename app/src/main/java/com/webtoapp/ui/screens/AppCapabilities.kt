package com.webtoapp.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Window
import androidx.compose.ui.graphics.vector.ImageVector
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.isMeaningful
import com.webtoapp.ui.design.WtaCapabilityLevel
import com.webtoapp.ui.viewmodel.EditState

enum class AppCapabilitySection {
    Basic,
    Browser,
    Appearance,
    Run,
    Security,
    Lab,
    Export
}

data class AppCapability(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: AppCapabilitySection,
    val level: WtaCapabilityLevel,
    val icon: ImageVector,
    val keywords: List<String>,
    val configured: Boolean
)

fun buildAppCapabilities(editState: EditState): List<AppCapability> = listOf(
    AppCapability(
        id = "basic",
        title = Strings.capabilityBasicInfo,
        subtitle = Strings.capabilityBasicInfoHint,
        section = AppCapabilitySection.Basic,
        level = WtaCapabilityLevel.Common,
        icon = Icons.Outlined.Android,
        keywords = listOf("名称", "url", "图标", "包名", "基础"),
        configured = editState.name.isNotBlank() || editState.url.isNotBlank()
    ),
    AppCapability(
        id = "ad_block",
        title = Strings.capabilityAdBlock,
        subtitle = Strings.capabilityAdBlockHint,
        section = AppCapabilitySection.Browser,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.Security,
        keywords = listOf("广告", "拦截", "adblock", "规则", "浏览器"),
        configured = editState.adBlockEnabled || editState.adBlockRules.isNotEmpty()
    ),
    AppCapability(
        id = "browser",
        title = Strings.capabilityBrowserBehavior,
        subtitle = Strings.capabilityBrowserBehaviorHint,
        section = AppCapabilitySection.Browser,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.OpenInBrowser,
        keywords = listOf("浏览器", "工具栏", "全屏", "横屏", "长按", "ua", "useragent"),
        configured = editState.webViewConfig.hideToolbar ||
            editState.webViewConfig.hideBrowserToolbar ||
            editState.webViewConfig.landscapeMode
    ),
    AppCapability(
        id = "splash",
        title = Strings.capabilitySplash,
        subtitle = Strings.capabilitySplashHint,
        section = AppCapabilitySection.Appearance,
        level = WtaCapabilityLevel.Common,
        icon = Icons.Outlined.Image,
        keywords = listOf("启动页", "启动图", "splash", "视频", "图片", "外观"),
        configured = editState.splashEnabled
    ),
    AppCapability(
        id = "announcement",
        title = Strings.capabilityAnnouncement,
        subtitle = Strings.capabilityAnnouncementHint,
        section = AppCapabilitySection.Appearance,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.Campaign,
        keywords = listOf("公告", "弹窗", "popup", "announcement", "启动提示", "外观"),
        configured = editState.announcementEnabled
    ),
    AppCapability(
        id = "runtime",
        title = Strings.capabilityRuntimeControl,
        subtitle = Strings.capabilityRuntimeControlHint,
        section = AppCapabilitySection.Run,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.Window,
        keywords = listOf("运行", "常亮", "亮度", "悬浮窗", "后台", "自启动", "开机"),
        configured = editState.webViewConfig.keepScreenOn ||
            editState.webViewConfig.floatingWindowConfig.enabled ||
            editState.autoStartConfig?.bootStartEnabled == true
    ),
    AppCapability(
        id = "activation",
        title = Strings.capabilityActivation,
        subtitle = Strings.capabilityActivationHint,
        section = AppCapabilitySection.Security,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.Key,
        keywords = listOf("激活码", "密码", "授权", "验证", "安全"),
        configured = editState.activationEnabled
    ),
    AppCapability(
        id = "permissions",
        title = Strings.capabilityPermissions,
        subtitle = Strings.capabilityPermissionsHint,
        section = AppCapabilitySection.Export,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.AdminPanelSettings,
        keywords = listOf("权限", "permission", "相机", "定位", "通知", "麦克风"),
        configured = editState.apkExportConfig.runtimePermissions != com.webtoapp.data.model.ApkRuntimePermissions()
    ),
    AppCapability(
        id = "network_trust",
        title = Strings.capabilityNetworkTrust,
        subtitle = Strings.capabilityNetworkTrustHint,
        section = AppCapabilitySection.Export,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.GppGood,
        keywords = listOf("ca", "证书", "ssl", "https", "网络", "信任", "内网", "pem", "crt"),
        configured = editState.apkExportConfig.networkTrustConfig.customCaCertificates.isNotEmpty() ||
            !editState.apkExportConfig.networkTrustConfig.trustUserCa
    ),
    AppCapability(
        id = "export",
        title = Strings.capabilityApkExport,
        subtitle = Strings.capabilityApkExportHint,
        section = AppCapabilitySection.Export,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.UploadFile,
        keywords = listOf("导出", "apk", "包名", "版本", "签名", "架构", "构建"),
        configured = editState.apkExportConfig.isMeaningful()
    ),
    AppCapability(
        id = "lab",
        title = Strings.capabilityLab,
        subtitle = Strings.capabilityLabHint,
        section = AppCapabilitySection.Lab,
        level = WtaCapabilityLevel.Lab,
        icon = Icons.Outlined.Bolt,
        keywords = listOf("lab", "实验", "黑科技", "伪装", "强制运行", "设备", "反检测"),
        configured = editState.forcedRunConfig?.enabled == true ||
            editState.blackTechConfig?.enabled == true ||
            editState.disguiseConfig?.enabled == true
    ),
    AppCapability(
        id = "extension",
        title = Strings.capabilityExtension,
        subtitle = Strings.capabilityExtensionHint,
        section = AppCapabilitySection.Browser,
        level = WtaCapabilityLevel.Advanced,
        icon = Icons.Outlined.Code,
        keywords = listOf("扩展", "模块", "脚本", "extension", "插件"),
        configured = editState.extensionModuleEnabled || editState.extensionModuleIds.isNotEmpty()
    ),
    AppCapability(
        id = "appearance",
        title = Strings.capabilityAppearanceMedia,
        subtitle = Strings.capabilityAppearanceMediaHint,
        section = AppCapabilitySection.Appearance,
        level = WtaCapabilityLevel.Common,
        icon = Icons.Outlined.Layers,
        keywords = listOf("外观", "音乐", "bgm", "翻译", "translate", "视觉"),
        configured = editState.bgmEnabled || editState.translateEnabled || editState.announcementEnabled
    )
)

fun AppCapability.matches(query: String): Boolean {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return true
    return title.lowercase().contains(normalized) ||
        subtitle.lowercase().contains(normalized) ||
        keywords.any { it.lowercase().contains(normalized) }
}
