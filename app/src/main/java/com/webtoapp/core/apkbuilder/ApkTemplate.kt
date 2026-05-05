 package com.webtoapp.core.apkbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.LrcShellTheme
import java.io.*
import java.util.zip.*





class ApkTemplate(private val context: Context) {

    companion object {

        private const val TEMPLATE_APK = "template/webview_shell.apk"


        const val CONFIG_PATH = "assets/app_config.json"


        val ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher.png" to 192
        )


        val ROUND_ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher_round.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher_round.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher_round.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher_round.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher_round.png" to 192
        )
    }

    private val templateDir = File(context.cacheDir, "apk_templates")

    init {
        templateDir.mkdirs()
    }





    fun getTemplateApk(): File? {
        val templateFile = File(templateDir, "webview_shell.apk")


        if (templateFile.exists()) {
            return templateFile
        }


        return try {
            context.assets.open(TEMPLATE_APK).use { input ->
                FileOutputStream(templateFile).use { output ->
                    input.copyTo(output)
                }
            }
            templateFile
        } catch (e: Exception) {

            null
        }
    }




    fun hasTemplate(): Boolean {
        return try {
            context.assets.open(TEMPLATE_APK).close()
            true
        } catch (e: Exception) {
            false
        }
    }




    fun createConfigJson(config: ApkConfig): String =
        ApkConfigJsonFactory.create(config)

    fun createEncryptedStubJson(config: ApkConfig): String =
        ApkConfigJsonFactory.createEncryptedStub(config)

    fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
        if (scaled != bitmap) {
            scaled.recycle()
        }
        return baos.toByteArray()
    }




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












    fun createAdaptiveForegroundIcon(bitmap: Bitmap, size: Int): ByteArray {

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)


        val safeZoneSize = (size * 72f / 108f).toInt()
        val padding = (size - safeZoneSize) / 2


        val scaled = Bitmap.createScaledBitmap(bitmap, safeZoneSize, safeZoneSize, true)


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




    fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }


        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)


        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)

        if (scaled != bitmap) scaled.recycle()
        output.recycle()

        return baos.toByteArray()
    }




    fun clearCache() {
        templateDir.listFiles()?.forEach { it.delete() }
    }
}


internal object ApkConfigJsonFactory {
    const val SCHEMA_VERSION = 1

    private val configGson: com.google.gson.Gson by lazy {
        com.google.gson.GsonBuilder()
            .enableComplexMapKeySerialization()
            .serializeNulls()
            .create()
    }

    fun create(config: ApkConfig, gson: com.google.gson.Gson = configGson): String {
        ApkConfigValidator.requireValid(config)
        return gson.toJson(config.toShellPayload())
    }

    fun createEncryptedStub(config: ApkConfig, gson: com.google.gson.Gson = configGson): String =
        gson.toJson(config.toEncryptedStubPayload())

    internal fun ApkConfig.toShellPayload(): Map<String, Any?> = linkedMapOf(
        "schemaVersion" to SCHEMA_VERSION,
        "appName" to appName,
        "packageName" to packageName,
        "targetUrl" to targetUrl,
        "versionCode" to versionCode,
        "versionName" to versionName,
        "activationEnabled" to activationEnabled,
        "activationCodes" to activationCodes,
        "activationRequireEveryTime" to activationRequireEveryTime,
        "activationDialogTitle" to activationDialogTitle,
        "activationDialogSubtitle" to activationDialogSubtitle,
        "activationDialogInputLabel" to activationDialogInputLabel,
        "activationDialogButtonText" to activationDialogButtonText,
        "adBlockEnabled" to adBlockEnabled,
        "adBlockRules" to adBlockRules,
        "announcementEnabled" to announcementEnabled,
        "announcementTitle" to announcementTitle,
        "announcementContent" to announcementContent,
        "announcementLink" to announcementLink,
        "announcementLinkText" to announcementLinkText,
        "announcementTemplate" to announcementTemplate,
        "announcementShowEmoji" to announcementShowEmoji,
        "announcementAnimationEnabled" to announcementAnimationEnabled,
        "announcementShowOnce" to announcementShowOnce,
        "announcementRequireConfirmation" to announcementRequireConfirmation,
        "announcementAllowNeverShow" to announcementAllowNeverShow,
        "splashEnabled" to splashEnabled,
        "splashType" to splashType,
        "splashDuration" to splashDuration,
        "splashClickToSkip" to splashClickToSkip,
        "splashVideoStartMs" to splashVideoStartMs,
        "splashVideoEndMs" to splashVideoEndMs,
        "splashLandscape" to splashLandscape,
        "splashFillScreen" to splashFillScreen,
        "splashEnableAudio" to splashEnableAudio,
        "webViewConfig" to webViewConfigPayload(),
        "appType" to appType,
        "mediaConfig" to mediaConfigPayload(),
        "htmlConfig" to htmlConfigPayload(),
        "galleryConfig" to galleryConfigPayload(),
        "bgmEnabled" to bgmEnabled,
        "bgmPlaylist" to bgmPlaylist,
        "bgmPlayMode" to bgmPlayMode,
        "bgmVolume" to bgmVolume,
        "bgmAutoPlay" to bgmAutoPlay,
        "bgmShowLyrics" to bgmShowLyrics,
        "bgmLrcTheme" to bgmLrcTheme,
        "themeType" to themeType,
        "darkMode" to darkMode,
        "translateEnabled" to translateEnabled,
        "translateTargetLanguage" to translateTargetLanguage,
        "translateShowButton" to translateShowButton,
        "extensionFabIcon" to extensionFabIcon,
        "extensionModuleIds" to extensionModuleIds,
        "embeddedExtensionModules" to embeddedExtensionModules.map { it.toPayload() },
        "autoStartConfig" to autoStartConfigPayload(),
        "forcedRunConfig" to forcedRunConfig,
        "isolationEnabled" to isolationEnabled,
        "isolationConfig" to isolationConfigPayload(),
        "backgroundRunEnabled" to backgroundRunEnabled,
        "backgroundRunConfig" to backgroundRunConfigPayload(),
        "notificationEnabled" to notificationEnabled,
        "notificationConfig" to notificationConfigPayload(),
        "blackTechConfig" to blackTechConfig,
        "disguiseConfig" to disguiseConfig,
        "browserDisguiseConfig" to browserDisguiseConfig,
        "deviceDisguiseConfig" to deviceDisguiseConfig,
        "language" to language,
        "engineType" to engineType,
        "networkTrustConfig" to networkTrustConfigPayload(),
        "wordpressConfig" to wordpressConfigPayload(),
        "nodejsConfig" to nodejsConfigPayload(),
        "deepLinkEnabled" to deepLinkEnabled,
        "deepLinkHosts" to deepLinkHosts,
        "phpAppConfig" to phpAppConfigPayload(),
        "pythonAppConfig" to pythonAppConfigPayload(),
        "goAppConfig" to goAppConfigPayload(),
        "multiWebConfig" to multiWebConfigPayload(),
        "cloudSdkConfig" to cloudSdkConfigPayload()
    )

    internal fun ApkConfig.toEncryptedStubPayload(): Map<String, Any?> = linkedMapOf(
        "schemaVersion" to SCHEMA_VERSION,
        "appName" to appName,
        "packageName" to packageName,
        "targetUrl" to "",
        "appType" to "",
        "versionCode" to 0,
        "versionName" to "",
        "webViewConfig" to emptyMap<String, Any?>()
    )

    private fun ApkConfig.webViewConfigPayload(): Map<String, Any?> = linkedMapOf(
        "javaScriptEnabled" to javaScriptEnabled,
        "domStorageEnabled" to domStorageEnabled,
        "allowFileAccess" to allowFileAccess,
        "allowContentAccess" to allowContentAccess,
        "cacheEnabled" to cacheEnabled,
        "zoomEnabled" to zoomEnabled,
        "desktopMode" to desktopMode,
        "userAgent" to userAgent,
        "userAgentMode" to userAgentMode,
        "customUserAgent" to customUserAgent,
        "orientationMode" to orientationMode,
        "keyboardAdjustMode" to keyboardAdjustMode,
        "swipeRefreshEnabled" to swipeRefreshEnabled,
        "fullscreenEnabled" to fullscreenEnabled,
        "hideToolbar" to hideToolbar,
        "hideBrowserToolbar" to hideBrowserToolbar,
        "showStatusBarInFullscreen" to showStatusBarInFullscreen,
        "showNavigationBarInFullscreen" to showNavigationBarInFullscreen,
        "showToolbarInFullscreen" to showToolbarInFullscreen,
        "landscapeMode" to landscapeMode,
        "injectScripts" to injectScripts.map { script ->
            linkedMapOf(
                "name" to script.name,
                "code" to script.code,
                "enabled" to script.enabled,
                "runAt" to script.runAt.name
            )
        },
        "statusBarColorMode" to statusBarColorMode,
        "statusBarColor" to statusBarColor,
        "statusBarDarkIcons" to statusBarDarkIcons,
        "statusBarBackgroundType" to statusBarBackgroundType,
        "statusBarBackgroundImage" to if (statusBarBackgroundType == "IMAGE" && !statusBarBackgroundImage.isNullOrEmpty()) {
            "statusbar_background.png"
        } else null,
        "statusBarBackgroundAlpha" to statusBarBackgroundAlpha,
        "statusBarHeightDp" to statusBarHeightDp,
        "statusBarColorModeDark" to statusBarColorModeDark,
        "statusBarColorDark" to statusBarColorDark,
        "statusBarDarkIconsDark" to statusBarDarkIconsDark,
        "statusBarBackgroundTypeDark" to statusBarBackgroundTypeDark,
        "statusBarBackgroundImageDark" to if (statusBarBackgroundTypeDark == "IMAGE" && !statusBarBackgroundImageDark.isNullOrEmpty()) {
            "statusbar_background_dark.png"
        } else null,
        "statusBarBackgroundAlphaDark" to statusBarBackgroundAlphaDark,
        "longPressMenuEnabled" to longPressMenuEnabled,
        "longPressMenuStyle" to longPressMenuStyle,
        "adBlockToggleEnabled" to adBlockToggleEnabled,
        "popupBlockerEnabled" to popupBlockerEnabled,
        "popupBlockerToggleEnabled" to popupBlockerToggleEnabled,
        "openExternalLinks" to openExternalLinks,
        "initialScale" to initialScale,
        "viewportMode" to viewportMode,
        "customViewportWidth" to customViewportWidth,
        "newWindowBehavior" to newWindowBehavior,
        "enablePaymentSchemes" to enablePaymentSchemes,
        "enableShareBridge" to enableShareBridge,
        "enableZoomPolyfill" to enableZoomPolyfill,
        "enableCrossOriginIsolation" to enableCrossOriginIsolation,
        "hideUrlPreview" to hideUrlPreview,
        "disableShields" to disableShields,
        "keepScreenOn" to keepScreenOn,
        "screenAwakeMode" to screenAwakeMode,
        "screenAwakeTimeoutMinutes" to screenAwakeTimeoutMinutes,
        "screenBrightness" to screenBrightness,
        "performanceOptimization" to performanceOptimization,
        "pwaOfflineEnabled" to pwaOfflineEnabled,
        "pwaOfflineStrategy" to pwaOfflineStrategy,
        "proxyMode" to proxyMode,
        "proxyHost" to proxyHost,
        "proxyPort" to proxyPort,
        "proxyType" to proxyType,
        "pacUrl" to pacUrl,
        "proxyBypassRules" to proxyBypassRules,
        "proxyUsername" to proxyUsername,
        "proxyPassword" to proxyPassword,
        "dnsMode" to dnsMode,
        "dnsConfig" to dnsConfig,
        "showFloatingBackButton" to showFloatingBackButton,
        "floatingWindowConfig" to floatingWindowConfigPayload(),
        "errorPageConfig" to errorPageConfigPayload()
    )

    private fun ApkConfig.floatingWindowConfigPayload(): Map<String, Any?> = linkedMapOf(
        "enabled" to floatingWindowEnabled,
        "windowSizePercent" to floatingWindowSizePercent,
        "widthPercent" to floatingWindowWidthPercent,
        "heightPercent" to floatingWindowHeightPercent,
        "lockAspectRatio" to floatingWindowLockAspectRatio,
        "opacity" to floatingWindowOpacity,
        "cornerRadius" to floatingWindowCornerRadius,
        "borderStyle" to floatingWindowBorderStyle,
        "showTitleBar" to floatingWindowShowTitleBar,
        "autoHideTitleBar" to floatingWindowAutoHideTitleBar,
        "startMinimized" to floatingWindowStartMinimized,
        "rememberPosition" to floatingWindowRememberPosition,
        "edgeSnapping" to floatingWindowEdgeSnapping,
        "showResizeHandle" to floatingWindowShowResizeHandle,
        "lockPosition" to floatingWindowLockPosition
    )

    private fun ApkConfig.errorPageConfigPayload(): Map<String, Any?> = linkedMapOf(
        "mode" to errorPageMode,
        "builtInStyle" to errorPageBuiltInStyle,
        "showMiniGame" to errorPageShowMiniGame,
        "miniGameType" to errorPageMiniGameType,
        "autoRetrySeconds" to errorPageAutoRetrySeconds,
        "customHtml" to errorPageCustomHtml,
        "customMediaPath" to errorPageCustomMediaPath,
        "retryButtonText" to errorPageRetryButtonText
    )

    private fun ApkConfig.mediaConfigPayload(): Map<String, Any?> = linkedMapOf(
        "enableAudio" to mediaEnableAudio,
        "loop" to mediaLoop,
        "autoPlay" to mediaAutoPlay,
        "fillScreen" to mediaFillScreen,
        "landscape" to mediaLandscape,
        "keepScreenOn" to mediaKeepScreenOn
    )

    private fun ApkConfig.htmlConfigPayload(): Map<String, Any?> = linkedMapOf(
        "entryFile" to htmlEntryFile,
        "enableJavaScript" to htmlEnableJavaScript,
        "enableLocalStorage" to htmlEnableLocalStorage,
        "landscapeMode" to htmlLandscapeMode
    )

    private fun ApkConfig.galleryConfigPayload(): Map<String, Any?> = linkedMapOf(
        "items" to galleryItems,
        "playMode" to galleryPlayMode,
        "imageInterval" to galleryImageInterval,
        "loop" to galleryLoop,
        "autoPlay" to galleryAutoPlay,
        "backgroundColor" to galleryBackgroundColor,
        "showThumbnailBar" to galleryShowThumbnailBar,
        "showMediaInfo" to galleryShowMediaInfo,
        "orientation" to galleryOrientation,
        "enableAudio" to galleryEnableAudio,
        "videoAutoNext" to galleryVideoAutoNext
    )

    private fun ApkConfig.autoStartConfigPayload(): Map<String, Any?>? =
        if (bootStartEnabled || scheduledStartEnabled) {
            linkedMapOf(
                "bootStartEnabled" to bootStartEnabled,
                "scheduledStartEnabled" to scheduledStartEnabled,
                "scheduledTime" to scheduledTime,
                "scheduledDays" to scheduledDays
            )
        } else null

    private fun ApkConfig.isolationConfigPayload(): Map<String, Any?>? {
        val ic = isolationConfig ?: return null
        if (!isolationEnabled) return null
        return linkedMapOf(
            "enabled" to ic.enabled,
            "fingerprintConfig" to linkedMapOf(
                "randomize" to ic.fingerprintConfig.randomize,
                "regenerateOnLaunch" to ic.fingerprintConfig.regenerateOnLaunch,
                "customUserAgent" to ic.fingerprintConfig.customUserAgent,
                "randomUserAgent" to ic.fingerprintConfig.randomUserAgent,
                "fingerprintId" to ic.fingerprintConfig.fingerprintId
            ),
            "headerConfig" to linkedMapOf(
                "enabled" to ic.headerConfig.enabled,
                "randomizeOnRequest" to ic.headerConfig.randomizeOnRequest,
                "dnt" to ic.headerConfig.dnt,
                "spoofClientHints" to ic.headerConfig.spoofClientHints,
                "refererPolicy" to ic.headerConfig.refererPolicy.name
            ),
            "ipSpoofConfig" to linkedMapOf(
                "enabled" to ic.ipSpoofConfig.enabled,
                "spoofMethod" to ic.ipSpoofConfig.spoofMethod.name,
                "customIp" to ic.ipSpoofConfig.customIp,
                "randomIpRange" to ic.ipSpoofConfig.randomIpRange.name,
                "searchKeyword" to ic.ipSpoofConfig.searchKeyword,
                "xForwardedFor" to ic.ipSpoofConfig.xForwardedFor,
                "xRealIp" to ic.ipSpoofConfig.xRealIp,
                "clientIp" to ic.ipSpoofConfig.clientIp
            ),
            "storageIsolation" to ic.storageIsolation,
            "blockWebRTC" to ic.blockWebRTC,
            "protectCanvas" to ic.protectCanvas,
            "protectAudio" to ic.protectAudio,
            "protectWebGL" to ic.protectWebGL,
            "protectFonts" to ic.protectFonts,
            "spoofTimezone" to ic.spoofTimezone,
            "customTimezone" to ic.customTimezone,
            "spoofLanguage" to ic.spoofLanguage,
            "customLanguage" to ic.customLanguage,
            "spoofScreen" to ic.spoofScreen,
            "customScreenWidth" to ic.customScreenWidth,
            "customScreenHeight" to ic.customScreenHeight
        )
    }

    private fun ApkConfig.backgroundRunConfigPayload(): BackgroundRunConfig? =
        backgroundRunConfig.takeIf { backgroundRunEnabled }

    private fun ApkConfig.notificationConfigPayload(): NotificationConfig? =
        notificationConfig.takeIf { notificationEnabled }

    private fun ApkConfig.networkTrustConfigPayload(): Map<String, Any?> = linkedMapOf(
        "trustSystemCa" to networkTrustConfig.trustSystemCa,
        "trustUserCa" to networkTrustConfig.trustUserCa,
        "customCaCertificates" to networkTrustConfig.customCaCertificates.map {
            linkedMapOf(
                "id" to it.id,
                "displayName" to it.displayName,
                "sha256" to it.sha256
            )
        },
        "cleartextTrafficPermitted" to networkTrustConfig.cleartextTrafficPermitted
    )

    private fun ApkConfig.wordpressConfigPayload(): Map<String, Any?> = linkedMapOf(
        "siteTitle" to wordpressSiteTitle,
        "adminUser" to wordpressAdminUser,
        "adminEmail" to wordpressAdminEmail,
        "adminPassword" to wordpressAdminPassword,
        "themeName" to wordpressThemeName,
        "plugins" to wordpressPlugins,
        "activePlugins" to wordpressActivePlugins,
        "permalinkStructure" to wordpressPermalinkStructure,
        "siteLanguage" to wordpressSiteLanguage,
        "autoInstall" to wordpressAutoInstall,
        "phpPort" to wordpressPhpPort,
        "landscapeMode" to wordpressLandscapeMode
    )

    private fun ApkConfig.nodejsConfigPayload(): Map<String, Any?> = linkedMapOf(
        "mode" to nodejsMode,
        "port" to nodejsPort,
        "entryFile" to nodejsEntryFile,
        "envVars" to nodejsEnvVars,
        "landscapeMode" to nodejsLandscapeMode
    )

    private fun ApkConfig.phpAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to phpAppFramework,
        "documentRoot" to phpAppDocumentRoot,
        "entryFile" to phpAppEntryFile,
        "port" to phpAppPort,
        "envVars" to phpAppEnvVars,
        "landscapeMode" to phpAppLandscapeMode
    )

    private fun ApkConfig.pythonAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to pythonAppFramework,
        "entryFile" to pythonAppEntryFile,
        "entryModule" to pythonAppEntryModule,
        "serverType" to pythonAppServerType,
        "port" to pythonAppPort,
        "envVars" to pythonAppEnvVars,
        "landscapeMode" to pythonAppLandscapeMode
    )

    private fun ApkConfig.goAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to goAppFramework,
        "binaryName" to goAppBinaryName,
        "port" to goAppPort,
        "staticDir" to goAppStaticDir,
        "envVars" to goAppEnvVars,
        "landscapeMode" to goAppLandscapeMode
    )

    private fun ApkConfig.multiWebConfigPayload(): Map<String, Any?> = linkedMapOf(
        "sites" to multiWebSites,
        "displayMode" to multiWebDisplayMode,
        "refreshInterval" to multiWebRefreshInterval,
        "showSiteIcons" to multiWebShowSiteIcons,
        "landscapeMode" to multiWebLandscapeMode,
        "projectId" to multiWebProjectId
    )

    private fun ApkConfig.cloudSdkConfigPayload(): Any =
        if (cloudSdkConfig.enabled) {
            cloudSdkConfig
        } else {
            linkedMapOf("enabled" to false)
        }

    private fun EmbeddedExtensionModule.toPayload(): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "icon" to icon,
        "category" to category,
        "code" to code,
        "cssCode" to cssCode,
        "runAt" to runAt,
        "urlMatches" to urlMatches,
        "configValues" to configValues,
        "enabled" to enabled
    )
}

internal object ApkConfigValidator {
    private val serverBackedAppTypes = setOf(
        "IMAGE",
        "VIDEO",
        "GALLERY",
        "WORDPRESS",
        "NODEJS_APP",
        "PHP_APP",
        "PYTHON_APP",
        "GO_APP",
        "MULTI_WEB"
    )

    fun requireValid(config: ApkConfig) {
        val appType = config.appType.trim().uppercase()
        val error = when {
            appType == "HTML" || appType == "FRONTEND" -> validateHtmlEntry(config.htmlEntryFile)
            appType in serverBackedAppTypes -> null
            config.targetUrl.isBlank() -> "targetUrl must not be blank for appType=$appType"
            else -> null
        }

        require(error == null) {
            "Invalid APK shell config for ${config.packageName}: $error"
        }
    }

    private fun validateHtmlEntry(entryFile: String): String? {
        val normalized = entryFile.trim()
        return when {
            normalized.isBlank() -> "htmlConfig.entryFile must not be blank"
            normalized.substringBeforeLast(".").isBlank() -> {
                "htmlConfig.entryFile must include a filename before the extension"
            }
            else -> null
        }
    }
}




data class ApkConfig(
    val appName: String,
    val packageName: String,
    val targetUrl: String,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val iconPath: String? = null,
    val runtimePermissions: com.webtoapp.data.model.ApkRuntimePermissions = com.webtoapp.data.model.ApkRuntimePermissions(),
    val networkTrustConfig: com.webtoapp.data.model.NetworkTrustConfig = com.webtoapp.data.model.NetworkTrustConfig(),


    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val activationRequireEveryTime: Boolean = false,
    val activationDialogTitle: String = "",
    val activationDialogSubtitle: String = "",
    val activationDialogInputLabel: String = "",
    val activationDialogButtonText: String = "",


    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),


    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementContent: String = "",
    val announcementLink: String = "",
    val announcementLinkText: String = "",
    val announcementTemplate: String = "MINIMAL",
    val announcementShowEmoji: Boolean = true,
    val announcementAnimationEnabled: Boolean = true,
    val announcementShowOnce: Boolean = true,
    val announcementRequireConfirmation: Boolean = false,
    val announcementAllowNeverShow: Boolean = false,


    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = true,
    val cacheEnabled: Boolean = true,
    val zoomEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgent: String? = null,
    val userAgentMode: String = "DEFAULT",
    val customUserAgent: String? = null,
    val hideToolbar: Boolean = false,
    val hideBrowserToolbar: Boolean = false,
    val showStatusBarInFullscreen: Boolean = false,
    val showNavigationBarInFullscreen: Boolean = false,
    val showToolbarInFullscreen: Boolean = false,
    val landscapeMode: Boolean = false,
    val orientationMode: String = "PORTRAIT",
    val injectScripts: List<com.webtoapp.data.model.UserScript> = emptyList(),


    val statusBarColorMode: String = "THEME",
    val statusBarColor: String? = null,
    val statusBarDarkIcons: Boolean? = null,
    val statusBarBackgroundType: String = "COLOR",
    val statusBarBackgroundImage: String? = null,
    val statusBarBackgroundAlpha: Float = 1.0f,
    val statusBarHeightDp: Int = 0,

    val statusBarColorModeDark: String = "THEME",
    val statusBarColorDark: String? = null,
    val statusBarDarkIconsDark: Boolean? = null,
    val statusBarBackgroundTypeDark: String = "COLOR",
    val statusBarBackgroundImageDark: String? = null,
    val statusBarBackgroundAlphaDark: Float = 1.0f,
    val longPressMenuEnabled: Boolean = true,
    val longPressMenuStyle: String = "FULL",
    val adBlockToggleEnabled: Boolean = false,
    val popupBlockerEnabled: Boolean = false,
    val popupBlockerToggleEnabled: Boolean = false,
    val openExternalLinks: Boolean = false,


    val initialScale: Int = 0,
    val viewportMode: String = "DEFAULT",
    val customViewportWidth: Int = 0,
    val newWindowBehavior: String = "SAME_WINDOW",
    val enablePaymentSchemes: Boolean = true,
    val enableShareBridge: Boolean = true,
    val enableZoomPolyfill: Boolean = true,
    val enableCrossOriginIsolation: Boolean = false,
    val hideUrlPreview: Boolean = false,
    val disableShields: Boolean = true,
    val keepScreenOn: Boolean = false,
    val screenAwakeMode: String = "OFF",
    val screenAwakeTimeoutMinutes: Int = 30,
    val screenBrightness: Int = -1,
    val keyboardAdjustMode: String = "RESIZE",
    val showFloatingBackButton: Boolean = true,
    val swipeRefreshEnabled: Boolean = true,
    val fullscreenEnabled: Boolean = true,
    val performanceOptimization: Boolean = false,
    val pwaOfflineEnabled: Boolean = false,
    val pwaOfflineStrategy: String = "NETWORK_FIRST",


    val proxyMode: String = "NONE",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyType: String = "HTTP",
    val pacUrl: String = "",
    val proxyBypassRules: List<String> = emptyList(),
    val proxyUsername: String = "",
    val proxyPassword: String = "",


    val dnsMode: String = "SYSTEM",
    val dnsConfig: DnsApkConfig = DnsApkConfig(),


    val errorPageMode: String = "BUILTIN_STYLE",
    val errorPageBuiltInStyle: String = "MATERIAL",
    val errorPageShowMiniGame: Boolean = false,
    val errorPageMiniGameType: String = "RANDOM",
    val errorPageAutoRetrySeconds: Int = 15,
    val errorPageCustomHtml: String = "",
    val errorPageCustomMediaPath: String = "",
    val errorPageRetryButtonText: String = "",


    val floatingWindowEnabled: Boolean = false,
    val floatingWindowSizePercent: Int = 80,
    val floatingWindowWidthPercent: Int = 80,
    val floatingWindowHeightPercent: Int = 80,
    val floatingWindowLockAspectRatio: Boolean = true,
    val floatingWindowOpacity: Int = 100,
    val floatingWindowCornerRadius: Int = 16,
    val floatingWindowBorderStyle: String = "SUBTLE",
    val floatingWindowShowTitleBar: Boolean = true,
    val floatingWindowAutoHideTitleBar: Boolean = false,
    val floatingWindowStartMinimized: Boolean = false,
    val floatingWindowRememberPosition: Boolean = true,
    val floatingWindowEdgeSnapping: Boolean = true,
    val floatingWindowShowResizeHandle: Boolean = true,
    val floatingWindowLockPosition: Boolean = false,


    val splashEnabled: Boolean = false,
    val splashType: String = "IMAGE",
    val splashDuration: Int = 3,
    val splashClickToSkip: Boolean = true,
    val splashVideoStartMs: Long = 0,
    val splashVideoEndMs: Long = 5000,
    val splashLandscape: Boolean = false,
    val splashFillScreen: Boolean = true,
    val splashEnableAudio: Boolean = false,


    val appType: String = "WEB",
    val mediaEnableAudio: Boolean = true,
    val mediaLoop: Boolean = true,
    val mediaAutoPlay: Boolean = true,
    val mediaFillScreen: Boolean = true,
    val mediaLandscape: Boolean = false,
    val mediaKeepScreenOn: Boolean = true,


    val htmlEntryFile: String = "index.html",
    val htmlEnableJavaScript: Boolean = true,
    val htmlEnableLocalStorage: Boolean = true,
    val htmlLandscapeMode: Boolean = false,


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


    val bgmEnabled: Boolean = false,
    val bgmPlaylist: List<BgmShellItem> = emptyList(),
    val bgmPlayMode: String = "LOOP",
    val bgmVolume: Float = 0.5f,
    val bgmAutoPlay: Boolean = true,
    val bgmShowLyrics: Boolean = true,
    val bgmLrcTheme: LrcShellTheme? = null,


    val themeType: String = "AURORA",
    val darkMode: String = "SYSTEM",


    val translateEnabled: Boolean = false,
    val translateTargetLanguage: String = "zh-CN",
    val translateShowButton: Boolean = true,


    val extensionModuleIds: List<String> = emptyList(),
    val embeddedExtensionModules: List<EmbeddedExtensionModule> = emptyList(),
    val extensionFabIcon: String = "",


    val autoStartEnabled: Boolean = false,
    val bootStartEnabled: Boolean = false,
    val scheduledStartEnabled: Boolean = false,
    val scheduledTime: String = "08:00",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),


    val forcedRunConfig: ForcedRunConfig? = null,


    val isolationEnabled: Boolean = false,
    val isolationConfig: com.webtoapp.core.isolation.IsolationConfig? = null,


    val backgroundRunEnabled: Boolean = false,
    val backgroundRunConfig: BackgroundRunConfig? = null,


    val notificationEnabled: Boolean = false,
    val notificationConfig: NotificationConfig? = null,


    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,


    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,


    val browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null,


    val deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null,


    val language: String = "CHINESE",


    val engineType: String = "SYSTEM_WEBVIEW",


    val deepLinkEnabled: Boolean = false,
    val deepLinkHosts: List<String> = emptyList(),


    val wordpressSiteTitle: String = "",
    val wordpressAdminUser: String = "admin",
    val wordpressAdminEmail: String = "",
    val wordpressAdminPassword: String = "admin",
    val wordpressThemeName: String = "",
    val wordpressPlugins: List<String> = emptyList(),
    val wordpressActivePlugins: List<String> = emptyList(),
    val wordpressPermalinkStructure: String = "/%postname%/",
    val wordpressSiteLanguage: String = "zh_CN",
    val wordpressAutoInstall: Boolean = true,
    val wordpressPhpPort: Int = 0,
    val wordpressLandscapeMode: Boolean = false,


    val nodejsMode: String = "STATIC",
    val nodejsPort: Int = 0,
    val nodejsEntryFile: String = "",
    val nodejsEnvVars: Map<String, String> = emptyMap(),
    val nodejsLandscapeMode: Boolean = false,


    val phpAppFramework: String = "",
    val phpAppDocumentRoot: String = "",
    val phpAppEntryFile: String = "index.php",
    val phpAppPort: Int = 0,
    val phpAppEnvVars: Map<String, String> = emptyMap(),
    val phpAppLandscapeMode: Boolean = false,


    val pythonAppFramework: String = "",
    val pythonAppEntryFile: String = "app.py",
    val pythonAppEntryModule: String = "",
    val pythonAppServerType: String = "builtin",
    val pythonAppPort: Int = 0,
    val pythonAppEnvVars: Map<String, String> = emptyMap(),
    val pythonAppLandscapeMode: Boolean = false,


    val goAppFramework: String = "",
    val goAppBinaryName: String = "",
    val goAppPort: Int = 0,
    val goAppStaticDir: String = "",
    val goAppEnvVars: Map<String, String> = emptyMap(),
    val goAppLandscapeMode: Boolean = false,


    val multiWebSites: List<com.webtoapp.core.shell.MultiWebSiteShellConfig> = emptyList(),
    val multiWebDisplayMode: String = "TABS",
    val multiWebRefreshInterval: Int = 30,
    val multiWebShowSiteIcons: Boolean = true,
    val multiWebLandscapeMode: Boolean = false,
    val multiWebProjectId: String = "",


    val cloudSdkConfig: com.webtoapp.core.shell.CloudSdkConfig = com.webtoapp.core.shell.CloudSdkConfig()
)




data class BackgroundRunConfig(
    val notificationTitle: String = "",
    val notificationContent: String = "",
    val showNotification: Boolean = true,
    val keepCpuAwake: Boolean = true
)




data class NotificationConfig(
    val type: String = "none",
    val pollUrl: String = "",
    val pollIntervalMinutes: Int = 15,
    val pollMethod: String = "GET",
    val pollHeaders: String = "",
    val clickUrl: String = ""
)




data class GalleryShellItemConfig(
    val id: String,
    val assetPath: String,
    val type: String,
    val name: String,
    val duration: Long = 0,
    val thumbnailPath: String? = null
)





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




data class EmbeddedUrlMatchRule(
    val pattern: String,
    val isRegex: Boolean = false,
    val exclude: Boolean = false
)




data class DnsApkConfig(
    val provider: String = "cloudflare",
    val customDohUrl: String = "",
    val dohMode: String = "automatic",
    val bypassSystemDns: Boolean = false
)
