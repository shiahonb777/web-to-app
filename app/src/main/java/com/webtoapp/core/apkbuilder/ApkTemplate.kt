 package com.webtoapp.core.apkbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.util.GsonProvider
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.LrcShellTheme
import java.io.*
import java.util.zip.*/**
 * Note: brief English comment.
 * Note: brief English comment.
 */
class ApkTemplate(private val context: Context) {

    companion object {
        // Note: brief English comment.
        private const val TEMPLATE_APK = "template/webview_shell.apk"
        
        // Note: brief English comment.
        const val CONFIG_PATH = "assets/app_config.json"
        
        // Note: brief English comment.
        val ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher.png" to 192
        )
        
        // Note: brief English comment.
        val ROUND_ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher_round.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher_round.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher_round.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher_round.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher_round.png" to 192
        )
    }

    private val gson = GsonProvider.gson

    // Note: brief English comment.
    private val templateDir = File(context.cacheDir, "apk_templates")

    init {
        templateDir.mkdirs()
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun getTemplateApk(): File? {
        val templateFile = File(templateDir, "webview_shell.apk")
        
        // Note: brief English comment.
        if (templateFile.exists()) {
            return templateFile
        }

        // Note: brief English comment.
        return try {
            context.assets.open(TEMPLATE_APK).use { input ->
                FileOutputStream(templateFile).use { output ->
                    input.copyTo(output)
                }
            }
            templateFile
        } catch (e: Exception) {
            // Note: brief English comment.
            null
        }
    }

    /**
     * Note: brief English comment.
     */
    fun hasTemplate(): Boolean {
        return try {
            context.assets.open(TEMPLATE_APK).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Note: brief English comment.
     */
    fun createConfigJson(config: ApkConfig): String {
        return """
        {
            "appName": "${escapeJson(config.appName)}",
            "packageName": "${escapeJson(config.packageName)}",
            "targetUrl": "${escapeJson(config.targetUrl)}",
            "versionCode": ${config.versionCode},
            "versionName": "${escapeJson(config.versionName)}",
            "activationEnabled": ${config.activationEnabled},
            "activationCodes": [${config.activationCodes.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "activationRequireEveryTime": ${config.activationRequireEveryTime},
            "activationDialogTitle": "${escapeJson(config.activationDialogTitle)}",
            "activationDialogSubtitle": "${escapeJson(config.activationDialogSubtitle)}",
            "activationDialogInputLabel": "${escapeJson(config.activationDialogInputLabel)}",
            "activationDialogButtonText": "${escapeJson(config.activationDialogButtonText)}",
            "adBlockEnabled": ${config.adBlockEnabled},
            "adBlockRules": [${config.adBlockRules.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "announcementEnabled": ${config.announcementEnabled},
            "announcementTitle": "${escapeJson(config.announcementTitle)}",
            "announcementContent": "${escapeJson(config.announcementContent)}",
            "announcementLink": "${escapeJson(config.announcementLink)}",
            "announcementLinkText": "${escapeJson(config.announcementLinkText)}",
            "announcementTemplate": "${config.announcementTemplate}",
            "announcementShowEmoji": ${config.announcementShowEmoji},
            "announcementAnimationEnabled": ${config.announcementAnimationEnabled},
            "announcementShowOnce": ${config.announcementShowOnce},
            "announcementRequireConfirmation": ${config.announcementRequireConfirmation},
            "announcementAllowNeverShow": ${config.announcementAllowNeverShow},
            "splashEnabled": ${config.splashEnabled},
            "splashType": "${config.splashType}",
            "splashDuration": ${config.splashDuration},
            "splashClickToSkip": ${config.splashClickToSkip},
            "splashVideoStartMs": ${config.splashVideoStartMs},
            "splashVideoEndMs": ${config.splashVideoEndMs},
            "splashLandscape": ${config.splashLandscape},
            "splashFillScreen": ${config.splashFillScreen},
            "splashEnableAudio": ${config.splashEnableAudio},
            "webViewConfig": {
                "javaScriptEnabled": ${config.javaScriptEnabled},
                "domStorageEnabled": ${config.domStorageEnabled},
                "zoomEnabled": ${config.zoomEnabled},
                "desktopMode": ${config.desktopMode},
                "userAgent": ${config.userAgent?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "userAgentMode": "${config.userAgentMode}",
                "customUserAgent": ${config.customUserAgent?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "orientationMode": "${config.orientationMode}",
                "keyboardAdjustMode": "${config.keyboardAdjustMode}",
                "swipeRefreshEnabled": ${config.swipeRefreshEnabled},
                "fullscreenEnabled": ${config.fullscreenEnabled},
                "hideToolbar": ${config.hideToolbar},
                "hideBrowserToolbar": ${config.hideBrowserToolbar},
                "showStatusBarInFullscreen": ${config.showStatusBarInFullscreen},
                "showNavigationBarInFullscreen": ${config.showNavigationBarInFullscreen},
                "showToolbarInFullscreen": ${config.showToolbarInFullscreen},
                "landscapeMode": ${config.landscapeMode},
                "injectScripts": [${config.injectScripts.joinToString(",") { script ->
                    """{"name":"${escapeJson(script.name)}","code":"${escapeJson(script.code)}","enabled":${script.enabled},"runAt":"${script.runAt.name}"}"""
                }}],
                "statusBarColorMode": "${config.statusBarColorMode}",
                "statusBarColor": ${config.statusBarColor?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "statusBarDarkIcons": ${config.statusBarDarkIcons ?: "null"},
                "statusBarBackgroundType": "${config.statusBarBackgroundType}",
                "statusBarBackgroundImage": ${if (config.statusBarBackgroundType == "IMAGE" && !config.statusBarBackgroundImage.isNullOrEmpty()) "\"statusbar_background.png\"" else "null"},
                "statusBarBackgroundAlpha": ${config.statusBarBackgroundAlpha},
                "statusBarHeightDp": ${config.statusBarHeightDp},
                "statusBarColorModeDark": "${config.statusBarColorModeDark}",
                "statusBarColorDark": ${config.statusBarColorDark?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "statusBarDarkIconsDark": ${config.statusBarDarkIconsDark ?: "null"},
                "statusBarBackgroundTypeDark": "${config.statusBarBackgroundTypeDark}",
                "statusBarBackgroundImageDark": ${if (config.statusBarBackgroundTypeDark == "IMAGE" && !config.statusBarBackgroundImageDark.isNullOrEmpty()) "\"statusbar_background_dark.png\"" else "null"},
                "statusBarBackgroundAlphaDark": ${config.statusBarBackgroundAlphaDark},
                "longPressMenuEnabled": ${config.longPressMenuEnabled},
                "longPressMenuStyle": "${config.longPressMenuStyle}",
                "adBlockToggleEnabled": ${config.adBlockToggleEnabled},
                "popupBlockerEnabled": ${config.popupBlockerEnabled},
                "popupBlockerToggleEnabled": ${config.popupBlockerToggleEnabled},
                "openExternalLinks": ${config.openExternalLinks},
                "initialScale": ${config.initialScale},
                "viewportMode": "${config.viewportMode}",
                "newWindowBehavior": "${config.newWindowBehavior}",
                "enablePaymentSchemes": ${config.enablePaymentSchemes},
                "enableShareBridge": ${config.enableShareBridge},
                "enableZoomPolyfill": ${config.enableZoomPolyfill},
                "enableCrossOriginIsolation": ${config.enableCrossOriginIsolation},
                "disableShields": ${config.disableShields},
                "keepScreenOn": ${config.keepScreenOn},
                "screenAwakeMode": "${config.screenAwakeMode}",
                "screenAwakeTimeoutMinutes": ${config.screenAwakeTimeoutMinutes},
                "screenBrightness": ${config.screenBrightness},
                "performanceOptimization": ${config.performanceOptimization},
                "pwaOfflineEnabled": ${config.pwaOfflineEnabled},
                "pwaOfflineStrategy": "${config.pwaOfflineStrategy}",
                "showFloatingBackButton": ${config.showFloatingBackButton},
                "floatingWindowConfig": {
                    "enabled": ${config.floatingWindowEnabled},
                    "windowSizePercent": ${config.floatingWindowSizePercent},
                    "widthPercent": ${config.floatingWindowWidthPercent},
                    "heightPercent": ${config.floatingWindowHeightPercent},
                    "lockAspectRatio": ${config.floatingWindowLockAspectRatio},
                    "opacity": ${config.floatingWindowOpacity},
                    "cornerRadius": ${config.floatingWindowCornerRadius},
                    "borderStyle": "${config.floatingWindowBorderStyle}",
                    "showTitleBar": ${config.floatingWindowShowTitleBar},
                    "autoHideTitleBar": ${config.floatingWindowAutoHideTitleBar},
                    "startMinimized": ${config.floatingWindowStartMinimized},
                    "rememberPosition": ${config.floatingWindowRememberPosition},
                    "edgeSnapping": ${config.floatingWindowEdgeSnapping},
                    "showResizeHandle": ${config.floatingWindowShowResizeHandle},
                    "lockPosition": ${config.floatingWindowLockPosition}
                },
                "errorPageConfig": {
                    "mode": "${config.errorPageMode}",
                    "builtInStyle": "${config.errorPageBuiltInStyle}",
                    "showMiniGame": ${config.errorPageShowMiniGame},
                    "miniGameType": "${config.errorPageMiniGameType}",
                    "autoRetrySeconds": ${config.errorPageAutoRetrySeconds}
                }
            },
            "appType": "${config.appType}",
            "mediaConfig": {
                "enableAudio": ${config.mediaEnableAudio},
                "loop": ${config.mediaLoop},
                "autoPlay": ${config.mediaAutoPlay},
                "fillScreen": ${config.mediaFillScreen},
                "landscape": ${config.mediaLandscape},
                "keepScreenOn": ${config.mediaKeepScreenOn}
            },
            "htmlConfig": {
                "entryFile": "${escapeJson(config.htmlEntryFile)}",
                "enableJavaScript": ${config.htmlEnableJavaScript},
                "enableLocalStorage": ${config.htmlEnableLocalStorage},
                "landscapeMode": ${config.htmlLandscapeMode}
            },
            "galleryConfig": {
                "items": [${config.galleryItems.joinToString(",") { item ->
                    """{"id":"${escapeJson(item.id)}","assetPath":"${escapeJson(item.assetPath)}","type":"${item.type}","name":"${escapeJson(item.name)}","duration":${item.duration},"thumbnailPath":${item.thumbnailPath?.let { "\"${escapeJson(it)}\"" } ?: "null"}}"""
                }}],
                "playMode": "${config.galleryPlayMode}",
                "imageInterval": ${config.galleryImageInterval},
                "loop": ${config.galleryLoop},
                "autoPlay": ${config.galleryAutoPlay},
                "backgroundColor": "${escapeJson(config.galleryBackgroundColor)}",
                "showThumbnailBar": ${config.galleryShowThumbnailBar},
                "showMediaInfo": ${config.galleryShowMediaInfo},
                "orientation": "${config.galleryOrientation}",
                "enableAudio": ${config.galleryEnableAudio},
                "videoAutoNext": ${config.galleryVideoAutoNext}
            },
            "bgmEnabled": ${config.bgmEnabled},
            "bgmPlaylist": [${config.bgmPlaylist.joinToString(",") { item ->
                """{"id":"${escapeJson(item.id)}","name":"${escapeJson(item.name)}","assetPath":"${escapeJson(item.assetPath)}","lrcAssetPath":${item.lrcAssetPath?.let { "\"${escapeJson(it)}\"" } ?: "null"},"sortOrder":${item.sortOrder}}"""
            }}],
            "bgmPlayMode": "${config.bgmPlayMode}",
            "bgmVolume": ${config.bgmVolume},
            "bgmAutoPlay": ${config.bgmAutoPlay},
            "bgmShowLyrics": ${config.bgmShowLyrics},
            "bgmLrcTheme": ${config.bgmLrcTheme?.let { theme ->
                """{"id":"${escapeJson(theme.id)}","name":"${escapeJson(theme.name)}","fontSize":${theme.fontSize},"textColor":"${escapeJson(theme.textColor)}","highlightColor":"${escapeJson(theme.highlightColor)}","backgroundColor":"${escapeJson(theme.backgroundColor)}","animationType":"${theme.animationType}","position":"${theme.position}"}"""
            } ?: "null"},
            "themeType": "${config.themeType}",
            "darkMode": "${config.darkMode}",
            "translateEnabled": ${config.translateEnabled},
            "translateTargetLanguage": "${config.translateTargetLanguage}",
            "translateShowButton": ${config.translateShowButton},
            "extensionFabIcon": "${escapeJson(config.extensionFabIcon)}",
            "extensionModuleIds": [${config.extensionModuleIds.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "embeddedExtensionModules": [${config.embeddedExtensionModules.joinToString(",") { module ->
                """{"id":"${escapeJson(module.id)}","name":"${escapeJson(module.name)}","description":"${escapeJson(module.description)}","icon":"${escapeJson(module.icon)}","category":"${module.category}","code":"${escapeJson(module.code)}","cssCode":"${escapeJson(module.cssCode)}","runAt":"${module.runAt}","urlMatches":[${module.urlMatches.joinToString(",") { rule ->
                    """{"pattern":"${escapeJson(rule.pattern)}","isRegex":${rule.isRegex},"exclude":${rule.exclude}}"""
                }}],"configValues":{${module.configValues.entries.joinToString(",") { (k, v) ->
                    "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
                }}},"enabled":${module.enabled}}"""
            }}],
            "autoStartConfig": ${if (config.bootStartEnabled || config.scheduledStartEnabled) {
                """{"bootStartEnabled":${config.bootStartEnabled},"scheduledStartEnabled":${config.scheduledStartEnabled},"scheduledTime":"${config.scheduledTime}","scheduledDays":[${config.scheduledDays.joinToString(",")}]}"""
            } else "null"},
            "forcedRunConfig": ${gson.toJson(config.forcedRunConfig)},
            "isolationEnabled": ${config.isolationEnabled},
            "isolationConfig": ${if (config.isolationEnabled && config.isolationConfig != null) {
                val ic = config.isolationConfig
                val fc = ic.fingerprintConfig
                val hc = ic.headerConfig
                val ipc = ic.ipSpoofConfig
                """{"enabled":${ic.enabled},"fingerprintConfig":{"randomize":${fc.randomize},"regenerateOnLaunch":${fc.regenerateOnLaunch},"customUserAgent":${fc.customUserAgent?.let { "\"${escapeJson(it)}\"" } ?: "null"},"randomUserAgent":${fc.randomUserAgent},"fingerprintId":"${escapeJson(fc.fingerprintId)}"},"headerConfig":{"enabled":${hc.enabled},"randomizeOnRequest":${hc.randomizeOnRequest},"dnt":${hc.dnt},"spoofClientHints":${hc.spoofClientHints},"refererPolicy":"${hc.refererPolicy.name}"},"ipSpoofConfig":{"enabled":${ipc.enabled},"spoofMethod":"${ipc.spoofMethod.name}","customIp":${ipc.customIp?.let { "\"${escapeJson(it)}\"" } ?: "null"},"randomIpRange":"${ipc.randomIpRange.name}","searchKeyword":${ipc.searchKeyword?.let { "\"${escapeJson(it)}\"" } ?: "null"},"xForwardedFor":${ipc.xForwardedFor},"xRealIp":${ipc.xRealIp},"clientIp":${ipc.clientIp}},"storageIsolation":${ic.storageIsolation},"blockWebRTC":${ic.blockWebRTC},"protectCanvas":${ic.protectCanvas},"protectAudio":${ic.protectAudio},"protectWebGL":${ic.protectWebGL},"protectFonts":${ic.protectFonts},"spoofTimezone":${ic.spoofTimezone},"customTimezone":${ic.customTimezone?.let { "\"${escapeJson(it)}\"" } ?: "null"},"spoofLanguage":${ic.spoofLanguage},"customLanguage":${ic.customLanguage?.let { "\"${escapeJson(it)}\"" } ?: "null"},"spoofScreen":${ic.spoofScreen},"customScreenWidth":${ic.customScreenWidth ?: "null"},"customScreenHeight":${ic.customScreenHeight ?: "null"}}"""
            } else "null"},
            "backgroundRunEnabled": ${config.backgroundRunEnabled},
            "backgroundRunConfig": ${if (config.backgroundRunEnabled && config.backgroundRunConfig != null) {
                val bc = config.backgroundRunConfig
                """{"notificationTitle":"${escapeJson(bc.notificationTitle)}","notificationContent":"${escapeJson(bc.notificationContent)}","showNotification":${bc.showNotification},"keepCpuAwake":${bc.keepCpuAwake}}"""
            } else "null"},
            "blackTechConfig": ${gson.toJson(config.blackTechConfig)},
            "disguiseConfig": ${gson.toJson(config.disguiseConfig)},
            "language": "${config.language}",
            "engineType": "${config.engineType}",
            "wordpressConfig": {
                "siteTitle": "${escapeJson(config.wordpressSiteTitle)}",
                "phpPort": ${config.wordpressPhpPort},
                "landscapeMode": ${config.wordpressLandscapeMode}
            },
            "nodejsConfig": {
                "mode": "${config.nodejsMode}",
                "port": ${config.nodejsPort},
                "entryFile": "${escapeJson(config.nodejsEntryFile)}",
                "envVars": {${config.nodejsEnvVars.entries.joinToString(",") { (k, v) ->
                    "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
                }}},
                "landscapeMode": ${config.nodejsLandscapeMode}
            },
            "deepLinkEnabled": ${config.deepLinkEnabled},
            "deepLinkHosts": [${config.deepLinkHosts.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "phpAppConfig": {
                "framework": "${escapeJson(config.phpAppFramework)}",
                "documentRoot": "${escapeJson(config.phpAppDocumentRoot)}",
                "entryFile": "${escapeJson(config.phpAppEntryFile)}",
                "port": ${config.phpAppPort},
                "envVars": {${config.phpAppEnvVars.entries.joinToString(",") { (k, v) ->
                    "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
                }}},
                "landscapeMode": ${config.phpAppLandscapeMode}
            },
            "pythonAppConfig": {
                "framework": "${escapeJson(config.pythonAppFramework)}",
                "entryFile": "${escapeJson(config.pythonAppEntryFile)}",
                "entryModule": "${escapeJson(config.pythonAppEntryModule)}",
                "serverType": "${escapeJson(config.pythonAppServerType)}",
                "port": ${config.pythonAppPort},
                "envVars": {${config.pythonAppEnvVars.entries.joinToString(",") { (k, v) ->
                    "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
                }}},
                "landscapeMode": ${config.pythonAppLandscapeMode}
            },
            "goAppConfig": {
                "framework": "${escapeJson(config.goAppFramework)}",
                "binaryName": "${escapeJson(config.goAppBinaryName)}",
                "port": ${config.goAppPort},
                "staticDir": "${escapeJson(config.goAppStaticDir)}",
                "envVars": {${config.goAppEnvVars.entries.joinToString(",") { (k, v) ->
                    "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
                }}},
                "landscapeMode": ${config.goAppLandscapeMode}
            },
            "multiWebConfig": {
                "sites": [${config.multiWebSites.joinToString(",") { site ->
                    """{"id":"${escapeJson(site.id)}","name":"${escapeJson(site.name)}","url":"${escapeJson(site.url)}","iconEmoji":"${escapeJson(site.iconEmoji)}","category":"${escapeJson(site.category)}","cssSelector":"${escapeJson(site.cssSelector)}","linkSelector":"${escapeJson(site.linkSelector)}","enabled":${site.enabled}}"""
                }}],
                "displayMode": "${escapeJson(config.multiWebDisplayMode)}",
                "refreshInterval": ${config.multiWebRefreshInterval},
                "showSiteIcons": ${config.multiWebShowSiteIcons},
                "landscapeMode": ${config.multiWebLandscapeMode}
            },
            "cloudSdkConfig": ${if (config.cloudSdkConfig.enabled) {
                val sdk = config.cloudSdkConfig
                """{"enabled":true,"projectKey":"${escapeJson(sdk.projectKey)}","apiBaseUrl":"${escapeJson(sdk.apiBaseUrl)}","updateCheckEnabled":${sdk.updateCheckEnabled},"announcementEnabled":${sdk.announcementEnabled},"remoteConfigEnabled":${sdk.remoteConfigEnabled},"activationCodeEnabled":${sdk.activationCodeEnabled},"statsReportEnabled":${sdk.statsReportEnabled},"fcmPushEnabled":${sdk.fcmPushEnabled},"remoteScriptEnabled":${sdk.remoteScriptEnabled},"updateCheckInterval":${sdk.updateCheckInterval},"forceUpdateEnabled":${sdk.forceUpdateEnabled},"updateDialogTitle":"${escapeJson(sdk.updateDialogTitle)}","updateDialogButtonText":"${escapeJson(sdk.updateDialogButtonText)}","announcementTemplate":"${sdk.announcementTemplate}","announcementShowOnce":${sdk.announcementShowOnce},"statsReportInterval":${sdk.statsReportInterval},"reportCrashes":${sdk.reportCrashes},"activationBindDevice":${sdk.activationBindDevice},"activationDialogTitle":"${escapeJson(sdk.activationDialogTitle)}","activationDialogSubtitle":"${escapeJson(sdk.activationDialogSubtitle)}","fcmSenderId":"${escapeJson(sdk.fcmSenderId)}","fcmChannelId":"${escapeJson(sdk.fcmChannelId)}","fcmChannelName":"${escapeJson(sdk.fcmChannelName)}"}"""
            } else "{\"enabled\":false}"}
        }
        """.trimIndent()
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun escapeJson(str: String): String {
        val sb = StringBuilder()
        for (char in str) {
            when (char) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f") // form feed
                else -> {
                    // Note: brief English comment.
                    if (char.code < 32) {
                        sb.append("\\u${char.code.toString(16).padStart(4, '0')}")
                    } else {
                        sb.append(char)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Note: brief English comment.
     */
    fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
        if (scaled != bitmap) {
            scaled.recycle()
        }
        return baos.toByteArray()
    }

    /**
     * Note: brief English comment.
     */
    fun loadBitmap(iconPath: String): Bitmap? {
        return try {
            if (iconPath.startsWith("/")) {
                BitmapFactory.decodeFile(iconPath)
            } else if (iconPath.startsWith("content://")) {
                context.contentResolver.openInputStream(android.net.Uri.parse(iconPath))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else {
                BitmapFactory.decodeFile(iconPath)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     *
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun createAdaptiveForegroundIcon(bitmap: Bitmap, size: Int): ByteArray {
        // Note: brief English comment.
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        
        // Note: brief English comment.
        val safeZoneSize = (size * 72f / 108f).toInt()
        val padding = (size - safeZoneSize) / 2
        
        // Note: brief English comment.
        val scaled = Bitmap.createScaledBitmap(bitmap, safeZoneSize, safeZoneSize, true)
        
        // Note: brief English comment.
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(scaled, padding.toFloat(), padding.toFloat(), paint)
        
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        
        if (scaled != bitmap) scaled.recycle()
        output.recycle()
        
        return baos.toByteArray()
    }

    /**
     * Note: brief English comment.
     */
    fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // Note: brief English comment.
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)
        
        // Note: brief English comment.
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        
        if (scaled != bitmap) scaled.recycle()
        output.recycle()
        
        return baos.toByteArray()
    }

    /**
     * Note: brief English comment.
     */
    fun clearCache() {
        templateDir.listFiles()?.forEach { it.delete() }
    }
}

/**
 * Note: brief English comment.
 */
data class ApkConfig(
    val appName: String,
    val packageName: String,
    val targetUrl: String,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val iconPath: String? = null,
    
    // Note: brief English comment.
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val activationRequireEveryTime: Boolean = false,
    val activationDialogTitle: String = "",
    val activationDialogSubtitle: String = "",
    val activationDialogInputLabel: String = "",
    val activationDialogButtonText: String = "",
    
    // Note: brief English comment.
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),
    
    // Announcement
    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementContent: String = "",
    val announcementLink: String = "",
    val announcementLinkText: String = "",
    val announcementTemplate: String = "XIAOHONGSHU",
    val announcementShowEmoji: Boolean = true,
    val announcementAnimationEnabled: Boolean = true,
    val announcementShowOnce: Boolean = true,
    val announcementRequireConfirmation: Boolean = false,
    val announcementAllowNeverShow: Boolean = false,
    
    // Note: brief English comment.
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val zoomEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgent: String? = null,
    val userAgentMode: String = "DEFAULT", // User-Agent 模式: DEFAULT, CHROME_MOBILE, CHROME_DESKTOP, SAFARI_MOBILE, SAFARI_DESKTOP, FIREFOX_MOBILE, FIREFOX_DESKTOP, EDGE_MOBILE, EDGE_DESKTOP, CUSTOM
    val customUserAgent: String? = null, // Custom User-Agent（仅 CUSTOM 模式使用）
    val hideToolbar: Boolean = false,
    val hideBrowserToolbar: Boolean = false,
    val showStatusBarInFullscreen: Boolean = false,  // Fullscreen模式下是否显示状态栏
    val showNavigationBarInFullscreen: Boolean = false,  // Fullscreen模式下是否显示导航栏
    val showToolbarInFullscreen: Boolean = false,  // Fullscreen模式下是否显示顶部导航栏
    val landscapeMode: Boolean = false, // [已废弃] 向后兼容
    val orientationMode: String = "PORTRAIT", // 屏幕方向模式: PORTRAIT, LANDSCAPE, REVERSE_PORTRAIT, REVERSE_LANDSCAPE, SENSOR_PORTRAIT, SENSOR_LANDSCAPE, AUTO
    val injectScripts: List<com.webtoapp.data.model.UserScript> = emptyList(), // User注入脚本
    
    // Note: brief English comment.
    val statusBarColorMode: String = "THEME", // THEME, TRANSPARENT, CUSTOM
    val statusBarColor: String? = null, // Custom状态栏颜色
    val statusBarDarkIcons: Boolean? = null, // Status bar图标颜色
    val statusBarBackgroundType: String = "COLOR", // COLOR, IMAGE
    val statusBarBackgroundImage: String? = null, // Cropped image path
    val statusBarBackgroundAlpha: Float = 1.0f, // Alpha 0.0-1.0
    val statusBarHeightDp: Int = 0, // Custom高度dp（0=系统默认）
    // Note: brief English comment.
    val statusBarColorModeDark: String = "THEME",
    val statusBarColorDark: String? = null,
    val statusBarDarkIconsDark: Boolean? = null,
    val statusBarBackgroundTypeDark: String = "COLOR",
    val statusBarBackgroundImageDark: String? = null,
    val statusBarBackgroundAlphaDark: Float = 1.0f,
    val longPressMenuEnabled: Boolean = true, // Yes否启用长按菜单
    val longPressMenuStyle: String = "FULL", // DISABLED, SIMPLE, FULL
    val adBlockToggleEnabled: Boolean = false, // Allow用户在运行时切换广告拦截开关
    val popupBlockerEnabled: Boolean = true, // 启用弹窗拦截器
    val popupBlockerToggleEnabled: Boolean = false, // Allow用户在运行时切换弹窗拦截开关
    val openExternalLinks: Boolean = false, // External链接是否在浏览器打开
    
    // Note: brief English comment.
    val initialScale: Int = 0, // Initial scale (0-200, 0=自动)
    val viewportMode: String = "DEFAULT", // DEFAULT, FIT_SCREEN, DESKTOP
    val newWindowBehavior: String = "SAME_WINDOW", // SAME_WINDOW, EXTERNAL_BROWSER, POPUP_WINDOW, BLOCK
    val enablePaymentSchemes: Boolean = true, // Enable支付宝/微信等支付 scheme 拦截
    val enableShareBridge: Boolean = true, // Enable navigator.share 桥接
    val enableZoomPolyfill: Boolean = true, // Enable CSS zoom polyfill
    val enableCrossOriginIsolation: Boolean = false, // 启用跨域隔离（SharedArrayBuffer/FFmpeg.wasm 支持）
    val disableShields: Boolean = false, // 禁用 BrowserShields（游戏/需要完整网络功能的应用）
    val keepScreenOn: Boolean = false, // [向后兼容] 保持屏幕常亮
    val screenAwakeMode: String = "OFF", // 屏幕常亮模式: OFF, ALWAYS, TIMED
    val screenAwakeTimeoutMinutes: Int = 30, // 定时常亮时长（分钟）
    val screenBrightness: Int = -1, // 屏幕亮度：-1=跟随系统, 0-100=自定义百分比
    val keyboardAdjustMode: String = "RESIZE", // 键盘调整模式: RESIZE, NOTHING
    val showFloatingBackButton: Boolean = true, // 全屏模式下是否显示悬浮返回按钮
    val swipeRefreshEnabled: Boolean = true, // 下拉刷新
    val fullscreenEnabled: Boolean = true, // 视频全屏
    val performanceOptimization: Boolean = false, // 运行时性能优化脚本
    val pwaOfflineEnabled: Boolean = false, // PWA Service Worker 离线缓存
    val pwaOfflineStrategy: String = "NETWORK_FIRST", // CACHE_FIRST, NETWORK_FIRST, STALE_WHILE_REVALIDATE
    
    // Note: brief English comment.
    val errorPageMode: String = "BUILTIN_STYLE", // DEFAULT, BUILTIN_STYLE, CUSTOM_HTML, CUSTOM_MEDIA
    val errorPageBuiltInStyle: String = "MATERIAL", // MATERIAL, SATELLITE, OCEAN, FOREST, MINIMAL, NEON
    val errorPageShowMiniGame: Boolean = false,
    val errorPageMiniGameType: String = "RANDOM",
    val errorPageAutoRetrySeconds: Int = 15,
    
    // Note: brief English comment.
    val floatingWindowEnabled: Boolean = false,
    val floatingWindowSizePercent: Int = 80,     // [向后兼容] 窗口大小百分比 50-100
    val floatingWindowWidthPercent: Int = 80,    // 独立宽度百分比 30-100
    val floatingWindowHeightPercent: Int = 80,   // 独立高度百分比 30-100
    val floatingWindowLockAspectRatio: Boolean = true, // 锁定宽高比
    val floatingWindowOpacity: Int = 100,         // 透明度百分比 30-100
    val floatingWindowCornerRadius: Int = 16,     // 圆角半径 dp (0-32)
    val floatingWindowBorderStyle: String = "SUBTLE", // NONE, SUBTLE, GLOW, ACCENT
    val floatingWindowShowTitleBar: Boolean = true,
    val floatingWindowAutoHideTitleBar: Boolean = false,
    val floatingWindowStartMinimized: Boolean = false,
    val floatingWindowRememberPosition: Boolean = true,
    val floatingWindowEdgeSnapping: Boolean = true,
    val floatingWindowShowResizeHandle: Boolean = true,
    val floatingWindowLockPosition: Boolean = false,
    
    // Note: brief English comment.
    val splashEnabled: Boolean = false,
    val splashType: String = "IMAGE",      // "IMAGE" or "VIDEO"
    val splashDuration: Int = 3,           // Show时长（秒）
    val splashClickToSkip: Boolean = true, // Yes否允许点击跳过
    val splashVideoStartMs: Long = 0,      // Video裁剪起始（毫秒）
    val splashVideoEndMs: Long = 5000,     // Video裁剪结束（毫秒）
    val splashLandscape: Boolean = false,  // Yes否横屏显示
    val splashFillScreen: Boolean = true,  // Yes否自动放大铺满屏幕
    val splashEnableAudio: Boolean = false, // Yes否启用视频音频
    
    // Note: brief English comment.
    val appType: String = "WEB",           // "WEB", "IMAGE", "VIDEO", "HTML"
    val mediaEnableAudio: Boolean = true,  // Video是否启用音频
    val mediaLoop: Boolean = true,         // Yes否循环播放
    val mediaAutoPlay: Boolean = true,     // Yes否自动播放
    val mediaFillScreen: Boolean = true,   // Yes否铺满屏幕
    val mediaLandscape: Boolean = false,   // Yes否横屏显示
    val mediaKeepScreenOn: Boolean = true, // 保持屏幕常亮
    
    // Note: brief English comment.
    val htmlEntryFile: String = "index.html",  // HTML入口文件名
    val htmlEnableJavaScript: Boolean = true,  // Yes否启用JavaScript
    val htmlEnableLocalStorage: Boolean = true, // Yes否启用本地存储
    val htmlLandscapeMode: Boolean = false,    // HTML应用横屏模式
    
    // Note: brief English comment.
    val galleryItems: List<GalleryShellItemConfig> = emptyList(),
    val galleryPlayMode: String = "SEQUENTIAL",
    val galleryImageInterval: Int = 3,
    val galleryLoop: Boolean = true,
    val galleryAutoPlay: Boolean = false,
    val galleryBackgroundColor: String = "#000000",
    val galleryShowThumbnailBar: Boolean = true,
    val galleryShowMediaInfo: Boolean = true,
    val galleryOrientation: String = "PORTRAIT",
    val galleryEnableAudio: Boolean = true,
    val galleryVideoAutoNext: Boolean = true,
    
    // Note: brief English comment.
    val bgmEnabled: Boolean = false,       // Yes否启用背景音乐
    val bgmPlaylist: List<BgmShellItem> = emptyList(), // Play列表
    val bgmPlayMode: String = "LOOP",      // Play模式: LOOP, SEQUENTIAL, SHUFFLE
    val bgmVolume: Float = 0.5f,           // Volume (0.0-1.0)
    val bgmAutoPlay: Boolean = true,       // Yes否自动播放
    val bgmShowLyrics: Boolean = true,     // Yes否显示歌词
    val bgmLrcTheme: LrcShellTheme? = null, // Lyrics主题
    
    // Note: brief English comment.
    val themeType: String = "AURORA",      // Theme类型
    val darkMode: String = "SYSTEM",       // Dark mode: SYSTEM, LIGHT, DARK
    
    // Note: brief English comment.
    val translateEnabled: Boolean = false,        // Yes否启用自动翻译
    val translateTargetLanguage: String = "zh-CN", // 目标语言: zh-CN, en, ja, ar
    val translateShowButton: Boolean = true,      // Yes否显示翻译按钮
    
    // Note: brief English comment.
    val extensionModuleIds: List<String> = emptyList(), // Enable的扩展模块ID列表
    val embeddedExtensionModules: List<EmbeddedExtensionModule> = emptyList(), // 嵌入的扩展模块完整数据
    val extensionFabIcon: String = "", // 扩展模块悬浮按钮自定义图标
    
    // Note: brief English comment.
    val autoStartEnabled: Boolean = false,
    val bootStartEnabled: Boolean = false,
    val scheduledStartEnabled: Boolean = false,
    val scheduledTime: String = "08:00",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),

    // Note: brief English comment.
    val forcedRunConfig: ForcedRunConfig? = null,
    
    // Note: brief English comment.
    val isolationEnabled: Boolean = false,
    val isolationConfig: com.webtoapp.core.isolation.IsolationConfig? = null,
    
    // Note: brief English comment.
    val backgroundRunEnabled: Boolean = false,
    val backgroundRunConfig: BackgroundRunConfig? = null,
    
    // Note: brief English comment.
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,
    
    // Note: brief English comment.
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,
    
    // Note: brief English comment.
    val language: String = "CHINESE",  // CHINESE, ENGLISH, ARABIC
    
    // Note: brief English comment.
    val engineType: String = "SYSTEM_WEBVIEW",  // SYSTEM_WEBVIEW, GECKOVIEW
    
    // Note: brief English comment.
    val deepLinkEnabled: Boolean = false,       // 是否启用链接打开
    val deepLinkHosts: List<String> = emptyList(), // 匹配的域名列表
    
    // Note: brief English comment.
    val wordpressSiteTitle: String = "",       // 站点标题
    val wordpressPhpPort: Int = 0,             // PHP 服务器端口（0=自动分配）
    val wordpressLandscapeMode: Boolean = false, // 横屏模式
    
    // Note: brief English comment.
    val nodejsMode: String = "STATIC",         // STATIC, BACKEND, FULLSTACK
    val nodejsPort: Int = 0,                   // Node.js 服务器端口（0=自动分配）
    val nodejsEntryFile: String = "",          // 入口文件（backend 模式需要）
    val nodejsEnvVars: Map<String, String> = emptyMap(), // 环境变量
    val nodejsLandscapeMode: Boolean = false,  // 横屏模式
    
    // Note: brief English comment.
    val phpAppFramework: String = "",           // 框架名称
    val phpAppDocumentRoot: String = "",        // Web 根目录
    val phpAppEntryFile: String = "index.php",  // 入口文件
    val phpAppPort: Int = 0,                    // PHP 端口
    val phpAppEnvVars: Map<String, String> = emptyMap(),
    val phpAppLandscapeMode: Boolean = false,
    
    // Note: brief English comment.
    val pythonAppFramework: String = "",
    val pythonAppEntryFile: String = "app.py",
    val pythonAppEntryModule: String = "",
    val pythonAppServerType: String = "builtin",
    val pythonAppPort: Int = 0,
    val pythonAppEnvVars: Map<String, String> = emptyMap(),
    val pythonAppLandscapeMode: Boolean = false,
    
    // Note: brief English comment.
    val goAppFramework: String = "",
    val goAppBinaryName: String = "",
    val goAppPort: Int = 0,
    val goAppStaticDir: String = "",
    val goAppEnvVars: Map<String, String> = emptyMap(),
    val goAppLandscapeMode: Boolean = false,
    
    // Note: brief English comment.
    val multiWebSites: List<com.webtoapp.core.shell.MultiWebSiteShellConfig> = emptyList(),
    val multiWebDisplayMode: String = "TABS",
    val multiWebRefreshInterval: Int = 30,
    val multiWebShowSiteIcons: Boolean = true,
    val multiWebLandscapeMode: Boolean = false,
    
    // Note: brief English comment.
    val cloudSdkConfig: com.webtoapp.core.shell.CloudSdkConfig = com.webtoapp.core.shell.CloudSdkConfig()
)

/**
 * Note: brief English comment.
 */
data class BackgroundRunConfig(
    val notificationTitle: String = "",
    val notificationContent: String = "",
    val showNotification: Boolean = true,
    val keepCpuAwake: Boolean = true
)

/**
 * Note: brief English comment.
 */
data class GalleryShellItemConfig(
    val id: String,
    val assetPath: String,  // assets/gallery/item_0.{png|mp4}
    val type: String,       // IMAGE or VIDEO
    val name: String,
    val duration: Long = 0,
    val thumbnailPath: String? = null  // assets/gallery/thumb_0.jpg
)

/**
 * Note: brief English comment.
 * Note: brief English comment.
 */
data class EmbeddedExtensionModule(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "package",
    val category: String = "OTHER",
    val code: String = "",
    val cssCode: String = "",
    val runAt: String = "DOCUMENT_END",
    val urlMatches: List<EmbeddedUrlMatchRule> = emptyList(),
    val configValues: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)

/**
 * Note: brief English comment.
 */
data class EmbeddedUrlMatchRule(
    val pattern: String,
    val isRegex: Boolean = false,
    val exclude: Boolean = false
)
