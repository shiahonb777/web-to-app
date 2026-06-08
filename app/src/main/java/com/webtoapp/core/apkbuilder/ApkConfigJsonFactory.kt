package com.webtoapp.core.apkbuilder

internal object ApkConfigJsonFactory {
    const val SCHEMA_VERSION = 1
    const val FLOATING_WINDOW_MINIMIZED_ICON_ASSET = "floating_window_minimized_icon.png"

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
        "appName" to meta.appName,
        "packageName" to meta.packageName,
        "targetUrl" to meta.targetUrl,
        "htmlUsesFileScheme" to meta.htmlUsesFileScheme,
        "loggingEnabled" to meta.loggingEnabled,
        "versionCode" to meta.versionCode,
        "versionName" to meta.versionName,
        "activationEnabled" to activation.enabled,
        "activationCodes" to activation.codes,
        "activationRequireEveryTime" to activation.requireEveryTime,
        "activationDialogTitle" to activation.dialogTitle,
        "activationDialogSubtitle" to activation.dialogSubtitle,
        "activationDialogInputLabel" to activation.dialogInputLabel,
        "activationDialogButtonText" to activation.dialogButtonText,
        "activationRemoteEnabled" to activation.remoteEnabled,
        "activationRemoteVerifyUrl" to activation.remoteVerifyUrl,
        "activationRemotePublicKey" to activation.remotePublicKey,
        "activationRemoteOfflinePolicy" to activation.remoteOfflinePolicy,
        "adBlockEnabled" to adBlock.enabled,
        "adBlockRules" to adBlock.rules,
        "announcementEnabled" to announcement.enabled,
        "announcementTitle" to announcement.title,
        "announcementContent" to announcement.content,
        "announcementContentIsHtml" to announcement.contentIsHtml,
        "announcementLink" to announcement.link,
        "announcementLinkText" to announcement.linkText,
        "announcementTemplate" to announcement.template,
        "announcementShowEmoji" to announcement.showEmoji,
        "announcementAnimationEnabled" to announcement.animationEnabled,
        "announcementShowOnce" to announcement.showOnce,
        "announcementRequireConfirmation" to announcement.requireConfirmation,
        "announcementAllowNeverShow" to announcement.allowNeverShow,
        "announcementTriggerOnLaunch" to announcement.triggerOnLaunch,
        "announcementTriggerOnNoNetwork" to announcement.triggerOnNoNetwork,
        "announcementTriggerIntervalMinutes" to announcement.triggerIntervalMinutes,
        "adsEnabled" to ads.enabled,
        "adBannerEnabled" to ads.bannerEnabled,
        "adBannerId" to ads.bannerId,
        "adInterstitialEnabled" to ads.interstitialEnabled,
        "adInterstitialId" to ads.interstitialId,
        "adSplashEnabled" to ads.splashEnabled,
        "adSplashId" to ads.splashId,
        "splashEnabled" to splash.enabled,
        "splashType" to splash.type,
        "splashDuration" to splash.duration,
        "splashClickToSkip" to splash.clickToSkip,
        "splashVideoStartMs" to splash.videoStartMs,
        "splashVideoEndMs" to splash.videoEndMs,
        "splashLandscape" to splash.landscape,
        "splashFillScreen" to splash.fillScreen,
        "splashEnableAudio" to splash.enableAudio,
        "webViewConfig" to webViewConfigPayload(),
        "appType" to meta.appType,
        "mediaConfig" to mediaConfigPayload(),
        "htmlConfig" to htmlConfigPayload(),
        "galleryConfig" to galleryConfigPayload(),
        "bgmEnabled" to bgm.enabled,
        "bgmPlaylist" to bgm.playlist,
        "bgmPlayMode" to bgm.playMode,
        "bgmVolume" to bgm.volume,
        "bgmAutoPlay" to bgm.autoPlay,
        "bgmShowLyrics" to bgm.showLyrics,
        "bgmLrcTheme" to bgm.lrcTheme,
        "themeType" to meta.themeType,
        "darkMode" to meta.darkMode,
        "translateEnabled" to translate.enabled,
        "translateTargetLanguage" to translate.targetLanguage,
        "translateShowButton" to translate.showButton,
        "extensionEnabled" to extension.enabled,
        "extensionFabIcon" to extension.fabIcon,
        "extensionModuleIds" to extension.moduleIds,
        "embeddedExtensionModules" to extension.embeddedModules.map { it.toPayload() },
        "autoStartConfig" to autoStartConfigPayload(),
        "forcedRunConfig" to optionalServices.forcedRunConfig,
        "isolationEnabled" to optionalServices.isolationEnabled,
        "isolationConfig" to isolationConfigPayload(),
        "backgroundRunEnabled" to optionalServices.backgroundRunEnabled,
        "backgroundRunConfig" to backgroundRunConfigPayload(),
        "notificationEnabled" to optionalServices.notificationEnabled,
        "notificationConfig" to notificationConfigPayload(),
        "hardeningEnabled" to optionalServices.hardeningEnabled,
        "hardeningThreatResponse" to optionalServices.hardeningThreatResponse,
        "blackTechConfig" to disguise.blackTechConfig,
        "disguiseConfig" to disguise.disguiseConfig,
        "browserDisguiseConfig" to disguise.browserDisguiseConfig,
        "deviceDisguiseConfig" to disguise.deviceDisguiseConfig,
        "language" to meta.language,
        "engineType" to meta.engineType,
        "networkTrustConfig" to networkTrustConfigPayload(),
        "wordpressConfig" to wordpressConfigPayload(),
        "nodejsConfig" to nodejsConfigPayload(),
        "deepLinkEnabled" to deepLink.enabled,
        "deepLinkHosts" to deepLink.hosts,
        "phpAppConfig" to phpAppConfigPayload(),
        "pythonAppConfig" to pythonAppConfigPayload(),
        "goAppConfig" to goAppConfigPayload(),
        "multiWebConfig" to multiWebConfigPayload()
    )

    internal fun ApkConfig.toEncryptedStubPayload(): Map<String, Any?> = linkedMapOf(
        "schemaVersion" to SCHEMA_VERSION,
        "appName" to meta.appName,
        "packageName" to meta.packageName,
        "targetUrl" to "",
        "appType" to "",
        "versionCode" to 0,
        "versionName" to "",
        "activationEnabled" to activation.enabled,
        "activationRequireEveryTime" to activation.requireEveryTime,
        "webViewConfig" to emptyMap<String, Any?>()
    )

    private fun ApkConfig.webViewConfigPayload(): Map<String, Any?> = linkedMapOf(
        "javaScriptEnabled" to webView.javaScriptEnabled,
        "domStorageEnabled" to webView.domStorageEnabled,
        "allowFileAccess" to webView.allowFileAccess,
        "allowContentAccess" to webView.allowContentAccess,
        "cacheEnabled" to webView.cacheEnabled,
        "clearBrowsingDataOnLaunch" to webView.clearBrowsingDataOnLaunch,
        "zoomEnabled" to webView.zoomEnabled,
        "desktopMode" to webView.desktopMode,
        "userAgent" to webView.userAgent,
        "userAgentMode" to webView.userAgentMode,
        "customUserAgent" to webView.customUserAgent,
        "orientationMode" to webView.orientationMode,
        "keyboardAdjustMode" to webView.keyboardAdjustMode,
        "swipeRefreshEnabled" to webView.swipeRefreshEnabled,
        "fullscreenEnabled" to webView.fullscreenEnabled,
        "hideToolbar" to webView.hideToolbar,
        "hideBrowserToolbar" to webView.hideBrowserToolbar,
        "toolbarShowTitle" to webView.toolbarShowTitle,
        "toolbarShowUrl" to webView.toolbarShowUrl,
        "toolbarShowBack" to webView.toolbarShowBack,
        "toolbarShowForward" to webView.toolbarShowForward,
        "toolbarShowRefresh" to webView.toolbarShowRefresh,
        "showStatusBarInFullscreen" to webView.showStatusBarInFullscreen,
        "showNavigationBarInFullscreen" to webView.showNavigationBarInFullscreen,
        "showToolbarInFullscreen" to webView.showToolbarInFullscreen,
        "landscapeMode" to webView.landscapeMode,
        "injectScripts" to webView.injectScripts.map { script ->
            linkedMapOf(
                "name" to script.name,
                "code" to script.code,
                "enabled" to script.enabled,
                "runAt" to script.runAt.name
            )
        },
        "statusBarColorMode" to statusBar.colorMode,
        "statusBarColor" to statusBar.color,
        "statusBarDarkIcons" to statusBar.darkIcons,
        "statusBarBackgroundType" to statusBar.backgroundType,
        "statusBarBackgroundImage" to if (statusBar.backgroundType == "IMAGE" && !statusBar.backgroundImage.isNullOrEmpty()) {
            "statusbar_background.png"
        } else null,
        "statusBarBackgroundAlpha" to statusBar.backgroundAlpha,
        "statusBarHeightDp" to statusBar.heightDp,
        "statusBarColorModeDark" to statusBar.colorModeDark,
        "statusBarColorDark" to statusBar.colorDark,
        "statusBarDarkIconsDark" to statusBar.darkIconsDark,
        "statusBarBackgroundTypeDark" to statusBar.backgroundTypeDark,
        "statusBarBackgroundImageDark" to if (statusBar.backgroundTypeDark == "IMAGE" && !statusBar.backgroundImageDark.isNullOrEmpty()) {
            "statusbar_background_dark.png"
        } else null,
        "statusBarBackgroundAlphaDark" to statusBar.backgroundAlphaDark,
        "longPressMenuEnabled" to webView.longPressMenuEnabled,
        "longPressMenuStyle" to webView.longPressMenuStyle,
        "adBlockToggleEnabled" to webView.adBlockToggleEnabled,
        "popupBlockerEnabled" to webView.popupBlockerEnabled,
        "popupBlockerToggleEnabled" to webView.popupBlockerToggleEnabled,
        "openExternalLinks" to webView.openExternalLinks,
        "initialScale" to webViewBehavior.initialScale,
        "viewportMode" to webViewBehavior.viewportMode,
        "customViewportWidth" to webViewBehavior.customViewportWidth,
        "newWindowBehavior" to webViewBehavior.newWindowBehavior,
        "enablePaymentSchemes" to webViewBehavior.enablePaymentSchemes,
        "enableShareBridge" to webViewBehavior.enableShareBridge,
        "enableZoomPolyfill" to webViewBehavior.enableZoomPolyfill,
        "enableCrossOriginIsolation" to webViewBehavior.enableCrossOriginIsolation,
        "hideUrlPreview" to webViewBehavior.hideUrlPreview,
        "decodeBase64DeepLinks" to webViewBehavior.decodeBase64DeepLinks,
        "decodeBase64Mode" to webViewBehavior.decodeBase64Mode,
        "mediaAutoplayEnabled" to webViewBehavior.mediaAutoplayEnabled,
        "mediaAutoplayScope" to webViewBehavior.mediaAutoplayScope,
        "acceptThirdPartyCookies" to webViewBehavior.acceptThirdPartyCookies,
        "thirdPartyCookieMode" to webViewBehavior.thirdPartyCookieMode,
        "enableKernelDisguise" to webViewBehavior.enableKernelDisguise,
        "kernelDisguiseLevel" to webViewBehavior.kernelDisguiseLevel,
        "kernelFlavor" to webViewBehavior.kernelFlavor,
        "enableImageRepair" to webViewBehavior.enableImageRepair,
        "enableScrollMemory" to webViewBehavior.enableScrollMemory,
        "followSystemDarkMode" to webViewBehavior.followSystemDarkMode,
        "enableClipboardPolyfill" to webViewBehavior.enableClipboardPolyfill,
        "enableNotificationPolyfill" to webViewBehavior.enableNotificationPolyfill,
        "geolocationEnabled" to webViewBehavior.geolocationEnabled,
        "geolocationAccuracy" to webViewBehavior.geolocationAccuracy,
        "geolocationPolicy" to webViewBehavior.geolocationPolicy,
        "enableOrientationPolyfill" to webViewBehavior.enableOrientationPolyfill,
        "enableCompatPolyfills" to webViewBehavior.enableCompatPolyfills,
        "enableNativeBridge" to webViewBehavior.enableNativeBridge,
        "nativeBridgeClipboard" to webViewBehavior.nativeBridgeClipboard,
        "nativeBridgeVibration" to webViewBehavior.nativeBridgeVibration,
        "nativeBridgeGeolocation" to webViewBehavior.nativeBridgeGeolocation,
        "nativeBridgeBrightness" to webViewBehavior.nativeBridgeBrightness,
        "nativeBridgeNotification" to webViewBehavior.nativeBridgeNotification,
        "nativeBridgeDownload" to webViewBehavior.nativeBridgeDownload,
        "nativeBridgePrivateNetwork" to webViewBehavior.nativeBridgePrivateNetwork,
        "nativeBridgeScreenWake" to webViewBehavior.nativeBridgeScreenWake,
        "javaScriptCanOpenWindows" to webViewBehavior.javaScriptCanOpenWindows,
        "jsOpenWindowsPolicy" to webViewBehavior.jsOpenWindowsPolicy,
        "databaseEnabled" to webViewBehavior.databaseEnabled,
        "enableCookiePersistence" to webViewBehavior.enableCookiePersistence,
        "enablePrivateNetworkBridge" to webViewBehavior.enablePrivateNetworkBridge,
        "privateNetworkScope" to webViewBehavior.privateNetworkScope,
        "allowMixedContent" to webViewBehavior.allowMixedContent,
        "mixedContentMode" to webViewBehavior.mixedContentMode,
        "enableBlobDownloadInterception" to webViewBehavior.enableBlobDownloadInterception,
        "blobInterceptScope" to webViewBehavior.blobInterceptScope,
        "blobInterceptThresholdMb" to webViewBehavior.blobInterceptThresholdMb,
        "enableCloudflareCompat" to webViewBehavior.enableCloudflareCompat,
        "cloudflareCompatMode" to webViewBehavior.cloudflareCompatMode,
        "primeUserActivation" to webViewBehavior.primeUserActivation,
        "primeUserActivationMode" to webViewBehavior.primeUserActivationMode,
        "primeUserActivationTiming" to webViewBehavior.primeUserActivationTiming,
        "fullscreenVideoOrientation" to webViewBehavior.fullscreenVideoOrientation,
        "failoverEnabled" to webViewBehavior.failoverEnabled,
        "failoverUrls" to webViewBehavior.failoverUrls,
        "failoverTriggerNetworkError" to webViewBehavior.failoverTriggerNetworkError,
        "failoverTriggerHttp5xx" to webViewBehavior.failoverTriggerHttp5xx,
        "failoverTriggerHttp4xx" to webViewBehavior.failoverTriggerHttp4xx,
        "failoverTriggerTimeout" to webViewBehavior.failoverTriggerTimeout,
        "failoverTimeoutSeconds" to webViewBehavior.failoverTimeoutSeconds,
        "keepScreenOn" to screenAwake.keepScreenOn,
        "screenAwakeMode" to screenAwake.mode,
        "screenAwakeTimeoutMinutes" to screenAwake.timeoutMinutes,
        "screenBrightness" to screenAwake.brightness,
        "performanceOptimization" to webView.performanceOptimization,
        "pwaOfflineEnabled" to webView.pwaOfflineEnabled,
        "pwaOfflineStrategy" to webView.pwaOfflineStrategy,
        "proxyMode" to proxy.mode,
        "proxyHost" to proxy.host,
        "proxyPort" to proxy.port,
        "proxyType" to proxy.type,
        "pacUrl" to proxy.pacUrl,
        "proxyBypassRules" to proxy.bypassRules,
        "proxyUsername" to proxy.username,
        "proxyPassword" to proxy.password,
        "hostsMappingEnabled" to proxy.hostsMappingEnabled,
        "hostsMappings" to proxy.hostsMappings.map { entry ->
            linkedMapOf(
                "host" to entry.host,
                "ip" to entry.ip
            )
        },
        "dnsMode" to dns.mode,
        "dnsConfig" to dns.config,
        "showFloatingBackButton" to webView.showFloatingBackButton,
        "floatingWindowConfig" to floatingWindowConfigPayload(),
        "errorPageConfig" to errorPageConfigPayload()
    )

    private fun ApkConfig.floatingWindowConfigPayload(): Map<String, Any?> = linkedMapOf(
        "enabled" to floatingWindow.enabled,
        "windowSizePercent" to floatingWindow.windowSizePercent,
        "widthPercent" to floatingWindow.widthPercent,
        "heightPercent" to floatingWindow.heightPercent,
        "lockAspectRatio" to floatingWindow.lockAspectRatio,
        "aspectRatioMode" to floatingWindow.aspectRatioMode,
        "customAspectRatioWidth" to floatingWindow.customAspectRatioWidth,
        "customAspectRatioHeight" to floatingWindow.customAspectRatioHeight,
        "opacity" to floatingWindow.opacity,
        "cornerRadius" to floatingWindow.cornerRadius,
        "borderStyle" to floatingWindow.borderStyle,
        "minimizedIconPath" to if (!floatingWindow.minimizedIconPath.isNullOrEmpty()) {
            FLOATING_WINDOW_MINIMIZED_ICON_ASSET
        } else null,
        "showTitleBar" to floatingWindow.showTitleBar,
        "autoHideTitleBar" to floatingWindow.autoHideTitleBar,
        "startMinimized" to floatingWindow.startMinimized,
        "rememberPosition" to floatingWindow.rememberPosition,
        "edgeSnapping" to floatingWindow.edgeSnapping,
        "showResizeHandle" to floatingWindow.showResizeHandle,
        "lockPosition" to floatingWindow.lockPosition
    )

    private fun ApkConfig.errorPageConfigPayload(): Map<String, Any?> = linkedMapOf(
        "mode" to errorPage.mode,
        "builtInStyle" to errorPage.builtInStyle,
        "showMiniGame" to errorPage.showMiniGame,
        "miniGameType" to errorPage.miniGameType,
        "autoRetrySeconds" to errorPage.autoRetrySeconds,
        "customHtml" to errorPage.customHtml,
        "customMediaPath" to errorPage.customMediaPath,
        "retryButtonText" to errorPage.retryButtonText
    )

    private fun ApkConfig.mediaConfigPayload(): Map<String, Any?> = linkedMapOf(
        "enableAudio" to media.enableAudio,
        "loop" to media.loop,
        "autoPlay" to media.autoPlay,
        "fillScreen" to media.fillScreen,
        "landscape" to media.landscape,
        "keepScreenOn" to media.keepScreenOn
    )

    private fun ApkConfig.htmlConfigPayload(): Map<String, Any?> = linkedMapOf(
        "entryFile" to html.entryFile,
        "enableJavaScript" to html.enableJavaScript,
        "enableLocalStorage" to html.enableLocalStorage,
        "landscapeMode" to html.landscapeMode
    )

    private fun ApkConfig.galleryConfigPayload(): Map<String, Any?> = linkedMapOf(
        "items" to gallery.items,
        "playMode" to gallery.playMode,
        "imageInterval" to gallery.imageInterval,
        "loop" to gallery.loop,
        "autoPlay" to gallery.autoPlay,
        "backgroundColor" to gallery.backgroundColor,
        "showThumbnailBar" to gallery.showThumbnailBar,
        "showMediaInfo" to gallery.showMediaInfo,
        "orientation" to gallery.orientation,
        "enableAudio" to gallery.enableAudio,
        "videoAutoNext" to gallery.videoAutoNext,
        "shuffleOnLoop" to gallery.shuffleOnLoop,
        "defaultView" to gallery.defaultView,
        "gridColumns" to gallery.gridColumns,
        "sortOrder" to gallery.sortOrder,
        "rememberPosition" to gallery.rememberPosition
    )

    private fun ApkConfig.autoStartConfigPayload(): Map<String, Any?>? =
        if (autoStart.bootStartEnabled || autoStart.scheduledStartEnabled) {
            linkedMapOf(
                "bootStartEnabled" to autoStart.bootStartEnabled,
                "scheduledStartEnabled" to autoStart.scheduledStartEnabled,
                "scheduledTime" to autoStart.scheduledTime,
                "scheduledDays" to autoStart.scheduledDays
            )
        } else null

    private fun ApkConfig.isolationConfigPayload(): Map<String, Any?>? {
        val ic = optionalServices.isolationConfig ?: return null
        if (!optionalServices.isolationEnabled) return null
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
        optionalServices.backgroundRunConfig.takeIf { optionalServices.backgroundRunEnabled }

    private fun ApkConfig.notificationConfigPayload(): NotificationConfig? =
        optionalServices.notificationConfig.takeIf { optionalServices.notificationEnabled }

    private fun ApkConfig.networkTrustConfigPayload(): Map<String, Any?> = linkedMapOf(
        "trustSystemCa" to meta.networkTrustConfig.trustSystemCa,
        "trustUserCa" to meta.networkTrustConfig.trustUserCa,
        "customCaCertificates" to meta.networkTrustConfig.customCaCertificates.map {
            linkedMapOf(
                "id" to it.id,
                "displayName" to it.displayName,
                "sha256" to it.sha256
            )
        },
        "cleartextTrafficPermitted" to meta.networkTrustConfig.cleartextTrafficPermitted
    )

    private fun ApkConfig.wordpressConfigPayload(): Map<String, Any?> = linkedMapOf(
        "siteTitle" to wordpress.siteTitle,
        "adminUser" to wordpress.adminUser,
        "adminEmail" to wordpress.adminEmail,
        "adminPassword" to wordpress.adminPassword,
        "themeName" to wordpress.themeName,
        "plugins" to wordpress.plugins,
        "activePlugins" to wordpress.activePlugins,
        "permalinkStructure" to wordpress.permalinkStructure,
        "siteLanguage" to wordpress.siteLanguage,
        "autoInstall" to wordpress.autoInstall,
        "phpPort" to wordpress.phpPort,
        "landscapeMode" to wordpress.landscapeMode
    )

    private fun ApkConfig.nodejsConfigPayload(): Map<String, Any?> = linkedMapOf(
        "mode" to nodejs.mode,
        "port" to nodejs.port,
        "entryFile" to nodejs.entryFile,
        "envVars" to nodejs.envVars,
        "landscapeMode" to nodejs.landscapeMode
    )

    private fun ApkConfig.phpAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to phpApp.framework,
        "documentRoot" to phpApp.documentRoot,
        "entryFile" to phpApp.entryFile,
        "port" to phpApp.port,
        "envVars" to phpApp.envVars,
        "landscapeMode" to phpApp.landscapeMode
    )

    private fun ApkConfig.pythonAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to pythonApp.framework,
        "entryFile" to pythonApp.entryFile,
        "entryModule" to pythonApp.entryModule,
        "serverType" to pythonApp.serverType,
        "port" to pythonApp.port,
        "envVars" to pythonApp.envVars,
        "landscapeMode" to pythonApp.landscapeMode
    )

    private fun ApkConfig.goAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to goApp.framework,
        "binaryName" to goApp.binaryName,
        "targetArch" to goApp.targetArch,
        "port" to goApp.port,
        "staticDir" to goApp.staticDir,
        "envVars" to goApp.envVars,
        "landscapeMode" to goApp.landscapeMode
    )

    private fun ApkConfig.multiWebConfigPayload(): Map<String, Any?> = linkedMapOf(
        "sites" to multiWeb.sites,
        "displayMode" to multiWeb.displayMode,
        "refreshInterval" to multiWeb.refreshInterval,
        "showSiteIcons" to multiWeb.showSiteIcons,
        "landscapeMode" to multiWeb.landscapeMode,
        "projectId" to multiWeb.projectId
    )

    private fun EmbeddedExtensionModule.toPayload(): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "icon" to icon,
        "category" to category,
        "versionName" to versionName,
        "authorName" to authorName,
        "code" to code,
        "cssCode" to cssCode,
        "runAt" to runAt,
        "sourceType" to sourceType,
        "runMode" to runMode,
        "uiConfig" to uiConfig,
        "urlMatches" to urlMatches,
        "configValues" to configValues,
        "configItemCount" to configItemCount,
        "gmGrants" to gmGrants,
        "requireUrls" to requireUrls,
        "requireContents" to requireContents,
        "resources" to resources,
        "noframes" to noframes,
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
        val appType = config.meta.appType.trim().uppercase()
        val error = when {
            appType == "HTML" || appType == "FRONTEND" -> validateHtmlEntry(config.html.entryFile)
            appType in serverBackedAppTypes -> null
            config.meta.targetUrl.isBlank() -> "targetUrl must not be blank for appType=$appType"
            else -> null
        }

        require(error == null) {
            "Invalid APK shell config for ${config.meta.packageName}: $error"
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
