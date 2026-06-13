package com.webtoapp.ui.shell

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.WebViewConfig

fun buildWebViewConfig(config: ShellConfig): WebViewConfig {

    val isLocalFileApp = config.appType.trim().uppercase() in setOf("HTML", "FRONTEND")

    val webViewConfig = WebViewConfig(
        javaScriptEnabled = config.webViewConfig.javaScriptEnabled,
        domStorageEnabled = config.webViewConfig.domStorageEnabled,
        allowFileAccess = config.webViewConfig.allowFileAccess,
        allowContentAccess = config.webViewConfig.allowContentAccess,
        cacheEnabled = config.webViewConfig.cacheEnabled && !config.webViewConfig.clearBrowsingDataOnLaunch,
        clearBrowsingDataOnLaunch = config.webViewConfig.clearBrowsingDataOnLaunch,
        zoomEnabled = config.webViewConfig.zoomEnabled,
        desktopMode = config.webViewConfig.desktopMode,
        userAgent = config.webViewConfig.userAgent,
        userAgentMode = run {
            val rawMode = config.webViewConfig.userAgentMode
            try {
                val mode = com.webtoapp.data.model.UserAgentMode.valueOf(rawMode)
                AppLogger.d("ShellActivity", "UserAgentMode parsed: '$rawMode' -> ${mode.name}")
                mode
            } catch (e: Exception) {
                AppLogger.e("ShellActivity", "UserAgentMode parse failed: '$rawMode', falling back to DEFAULT", e)
                com.webtoapp.data.model.UserAgentMode.DEFAULT
            }
        },
        customUserAgent = config.webViewConfig.customUserAgent,
        openExternalLinks = config.webViewConfig.openExternalLinks,
        downloadEnabled = true,
        hideToolbar = config.webViewConfig.hideToolbar,
        hideBrowserToolbar = config.webViewConfig.hideBrowserToolbar,
        toolbarShowTitle = config.webViewConfig.toolbarShowTitle,
        toolbarShowUrl = config.webViewConfig.toolbarShowUrl,
        toolbarShowBack = config.webViewConfig.toolbarShowBack,
        toolbarShowForward = config.webViewConfig.toolbarShowForward,
        toolbarShowRefresh = config.webViewConfig.toolbarShowRefresh,
        showStatusBarInFullscreen = config.webViewConfig.showStatusBarInFullscreen,
        showNavigationBarInFullscreen = config.webViewConfig.showNavigationBarInFullscreen,
        showToolbarInFullscreen = config.webViewConfig.showToolbarInFullscreen,
        landscapeMode = config.webViewConfig.landscapeMode,
        orientationMode = try {
            com.webtoapp.data.model.OrientationMode.valueOf(config.webViewConfig.orientationMode)
        } catch (e: Exception) {
            if (config.webViewConfig.landscapeMode) com.webtoapp.data.model.OrientationMode.LANDSCAPE
            else com.webtoapp.data.model.OrientationMode.PORTRAIT
        },

        statusBarColorMode = try { com.webtoapp.data.model.StatusBarColorMode.valueOf(config.webViewConfig.statusBarColorMode) } catch (e: Exception) { com.webtoapp.data.model.StatusBarColorMode.THEME },
        statusBarColor = config.webViewConfig.statusBarColor,
        statusBarDarkIcons = config.webViewConfig.statusBarDarkIcons,
        statusBarBackgroundType = try { com.webtoapp.data.model.StatusBarBackgroundType.valueOf(config.webViewConfig.statusBarBackgroundType) } catch (e: Exception) { com.webtoapp.data.model.StatusBarBackgroundType.COLOR },
        statusBarBackgroundImage = config.webViewConfig.statusBarBackgroundImage,
        statusBarBackgroundAlpha = config.webViewConfig.statusBarBackgroundAlpha,
        statusBarHeightDp = config.webViewConfig.statusBarHeightDp,

        statusBarColorModeDark = try { com.webtoapp.data.model.StatusBarColorMode.valueOf(config.webViewConfig.statusBarColorModeDark) } catch (e: Exception) { com.webtoapp.data.model.StatusBarColorMode.THEME },
        statusBarColorDark = config.webViewConfig.statusBarColorDark,
        statusBarDarkIconsDark = config.webViewConfig.statusBarDarkIconsDark ?: false,
        statusBarBackgroundTypeDark = try { com.webtoapp.data.model.StatusBarBackgroundType.valueOf(config.webViewConfig.statusBarBackgroundTypeDark) } catch (e: Exception) { com.webtoapp.data.model.StatusBarBackgroundType.COLOR },
        statusBarBackgroundImageDark = config.webViewConfig.statusBarBackgroundImageDark,
        statusBarBackgroundAlphaDark = config.webViewConfig.statusBarBackgroundAlphaDark,

        longPressMenuEnabled = config.webViewConfig.longPressMenuEnabled,
        longPressMenuStyle = try { com.webtoapp.data.model.LongPressMenuStyle.valueOf(config.webViewConfig.longPressMenuStyle) } catch (e: Exception) { com.webtoapp.data.model.LongPressMenuStyle.FULL },
        adBlockToggleEnabled = config.webViewConfig.adBlockToggleEnabled,
        popupBlockerEnabled = config.webViewConfig.popupBlockerEnabled,
        popupBlockerToggleEnabled = config.webViewConfig.popupBlockerToggleEnabled,

        initialScale = config.webViewConfig.initialScale,
        viewportMode = try { com.webtoapp.data.model.ViewportMode.valueOf(config.webViewConfig.viewportMode) } catch (e: Exception) { com.webtoapp.data.model.ViewportMode.DEFAULT },
        customViewportWidth = config.webViewConfig.customViewportWidth,
        newWindowBehavior = try { com.webtoapp.data.model.NewWindowBehavior.valueOf(config.webViewConfig.newWindowBehavior) } catch (e: Exception) { com.webtoapp.data.model.NewWindowBehavior.SAME_WINDOW },
        enablePaymentSchemes = config.webViewConfig.enablePaymentSchemes,
        enableShareBridge = config.webViewConfig.enableShareBridge,
        enableZoomPolyfill = config.webViewConfig.enableZoomPolyfill,
        enableCrossOriginIsolation = config.webViewConfig.enableCrossOriginIsolation,
        hideUrlPreview = config.webViewConfig.hideUrlPreview,
        decodeBase64DeepLinks = config.webViewConfig.decodeBase64DeepLinks,
        decodeBase64Mode = try {
            com.webtoapp.data.model.Base64DeepLinkMode.valueOf(config.webViewConfig.decodeBase64Mode)
        } catch (e: Exception) { com.webtoapp.data.model.Base64DeepLinkMode.GESTURE_ONLY },
        mediaAutoplayEnabled = config.webViewConfig.mediaAutoplayEnabled,
        mediaAutoplayScope = try {
            com.webtoapp.data.model.MediaAutoplayScope.valueOf(config.webViewConfig.mediaAutoplayScope)
        } catch (e: Exception) { com.webtoapp.data.model.MediaAutoplayScope.VIDEO_ONLY },
        acceptThirdPartyCookies = config.webViewConfig.acceptThirdPartyCookies,
        thirdPartyCookieMode = try {
            com.webtoapp.data.model.ThirdPartyCookieMode.valueOf(config.webViewConfig.thirdPartyCookieMode)
        } catch (e: Exception) { com.webtoapp.data.model.ThirdPartyCookieMode.SAME_SITE_LAX },
        enableKernelDisguise = config.webViewConfig.enableKernelDisguise,
        kernelDisguiseLevel = try {
            com.webtoapp.data.model.KernelDisguiseLevel.valueOf(config.webViewConfig.kernelDisguiseLevel)
        } catch (e: Exception) { com.webtoapp.data.model.KernelDisguiseLevel.STANDARD },
        kernelFlavor = com.webtoapp.core.kernel.KernelFlavor.fromString(config.webViewConfig.kernelFlavor),
        enableImageRepair = config.webViewConfig.enableImageRepair,
        enableScrollMemory = config.webViewConfig.enableScrollMemory,
        followSystemDarkMode = config.webViewConfig.followSystemDarkMode,
        enableClipboardPolyfill = config.webViewConfig.enableClipboardPolyfill,
        enableNotificationPolyfill = config.webViewConfig.enableNotificationPolyfill,
        geolocationEnabled = config.webViewConfig.geolocationEnabled,
        geolocationAccuracy = try {
            com.webtoapp.data.model.GeolocationAccuracy.valueOf(config.webViewConfig.geolocationAccuracy)
        } catch (e: Exception) { com.webtoapp.data.model.GeolocationAccuracy.COARSE },
        geolocationPolicy = try {
            com.webtoapp.data.model.GeolocationPolicy.valueOf(config.webViewConfig.geolocationPolicy)
        } catch (e: Exception) { com.webtoapp.data.model.GeolocationPolicy.ALWAYS_ASK },
        enableOrientationPolyfill = config.webViewConfig.enableOrientationPolyfill,
        enableCompatPolyfills = config.webViewConfig.enableCompatPolyfills,
        enableNativeBridge = config.webViewConfig.enableNativeBridge,
        nativeBridgeCapabilities = com.webtoapp.data.model.NativeBridgeCapabilities(
            clipboard = config.webViewConfig.nativeBridgeClipboard,
            vibration = config.webViewConfig.nativeBridgeVibration,
            geolocation = config.webViewConfig.nativeBridgeGeolocation,
            brightness = config.webViewConfig.nativeBridgeBrightness,
            notification = config.webViewConfig.nativeBridgeNotification,
            notificationScheduled = config.webViewConfig.nativeBridgeNotificationScheduled,
            notificationPersistent = config.webViewConfig.nativeBridgeNotificationPersistent,
            download = config.webViewConfig.nativeBridgeDownload,
            privateNetwork = config.webViewConfig.nativeBridgePrivateNetwork,
            screenWake = config.webViewConfig.nativeBridgeScreenWake,
        ),
        javaScriptCanOpenWindows = config.webViewConfig.javaScriptCanOpenWindows,
        jsOpenWindowsPolicy = try {
            com.webtoapp.data.model.JsOpenWindowsPolicy.valueOf(config.webViewConfig.jsOpenWindowsPolicy)
        } catch (e: Exception) { com.webtoapp.data.model.JsOpenWindowsPolicy.ALLOW },
        databaseEnabled = config.webViewConfig.databaseEnabled,
        enableCookiePersistence = config.webViewConfig.enableCookiePersistence,
        enablePrivateNetworkBridge = config.webViewConfig.enablePrivateNetworkBridge,
        privateNetworkScope = try {
            com.webtoapp.data.model.PrivateNetworkScope.valueOf(config.webViewConfig.privateNetworkScope)
        } catch (e: Exception) { com.webtoapp.data.model.PrivateNetworkScope.LOCAL_ONLY },
        allowMixedContent = config.webViewConfig.allowMixedContent,
        mixedContentMode = try {
            com.webtoapp.data.model.MixedContentMode.valueOf(config.webViewConfig.mixedContentMode)
        } catch (e: Exception) { com.webtoapp.data.model.MixedContentMode.COMPATIBILITY },
        enableBlobDownloadInterception = config.webViewConfig.enableBlobDownloadInterception,
        blobInterceptScope = try {
            com.webtoapp.data.model.BlobInterceptScope.valueOf(config.webViewConfig.blobInterceptScope)
        } catch (e: Exception) { com.webtoapp.data.model.BlobInterceptScope.ALL },
        blobInterceptThresholdMb = config.webViewConfig.blobInterceptThresholdMb,
        enableCloudflareCompat = config.webViewConfig.enableCloudflareCompat,
        cloudflareCompatMode = try {
            com.webtoapp.data.model.CloudflareCompatMode.valueOf(config.webViewConfig.cloudflareCompatMode)
        } catch (e: Exception) { com.webtoapp.data.model.CloudflareCompatMode.AUTO_DETECT },
        primeUserActivation = config.webViewConfig.primeUserActivation,
        primeUserActivationMode = try {
            com.webtoapp.data.model.PrimeUserActivationMode.valueOf(config.webViewConfig.primeUserActivationMode)
        } catch (e: Exception) { com.webtoapp.data.model.PrimeUserActivationMode.SYNTHETIC_TAP },
        primeUserActivationTiming = try {
            com.webtoapp.data.model.PrimeUserActivationTiming.valueOf(config.webViewConfig.primeUserActivationTiming)
        } catch (e: Exception) { com.webtoapp.data.model.PrimeUserActivationTiming.ON_PAGE_FINISHED },
        fullscreenVideoOrientation = try {
            com.webtoapp.data.model.FullscreenVideoOrientation.valueOf(config.webViewConfig.fullscreenVideoOrientation)
        } catch (e: Exception) { com.webtoapp.data.model.FullscreenVideoOrientation.AUTO_SENSOR_LANDSCAPE },
        failoverEnabled = config.webViewConfig.failoverEnabled,
        failoverUrls = config.webViewConfig.failoverUrls,
        failoverTriggers = com.webtoapp.data.model.FailoverTriggers(
            networkError = config.webViewConfig.failoverTriggerNetworkError,
            http5xx = config.webViewConfig.failoverTriggerHttp5xx,
            http4xx = config.webViewConfig.failoverTriggerHttp4xx,
            timeout = config.webViewConfig.failoverTriggerTimeout,
        ),
        failoverTimeoutSeconds = config.webViewConfig.failoverTimeoutSeconds,
        keepScreenOn = config.webViewConfig.keepScreenOn,
        screenAwakeMode = try { com.webtoapp.data.model.ScreenAwakeMode.valueOf(config.webViewConfig.screenAwakeMode) } catch (e: Exception) { com.webtoapp.data.model.ScreenAwakeMode.OFF },
        screenAwakeTimeoutMinutes = config.webViewConfig.screenAwakeTimeoutMinutes,
        screenBrightness = config.webViewConfig.screenBrightness,
        showFloatingBackButton = config.webViewConfig.showFloatingBackButton,

        keyboardAdjustMode = try {
            com.webtoapp.data.model.KeyboardAdjustMode.valueOf(config.webViewConfig.keyboardAdjustMode)
        } catch (e: Exception) {
            AppLogger.w("ShellActivity", "KeyboardAdjustMode parse failed: '${config.webViewConfig.keyboardAdjustMode}', falling back to RESIZE", e)
            com.webtoapp.data.model.KeyboardAdjustMode.RESIZE
        },

        floatingWindowConfig = com.webtoapp.data.model.FloatingWindowConfig(
            enabled = config.webViewConfig.floatingWindowConfig.enabled,
            windowSizePercent = config.webViewConfig.floatingWindowConfig.windowSizePercent,
            widthPercent = config.webViewConfig.floatingWindowConfig.widthPercent,
            heightPercent = config.webViewConfig.floatingWindowConfig.heightPercent,
            lockAspectRatio = config.webViewConfig.floatingWindowConfig.lockAspectRatio,
            aspectRatioMode = try {
                com.webtoapp.data.model.FloatingWindowAspectRatioMode.valueOf(config.webViewConfig.floatingWindowConfig.aspectRatioMode)
            } catch (e: Exception) {
                if (config.webViewConfig.floatingWindowConfig.lockAspectRatio) {
                    com.webtoapp.data.model.FloatingWindowAspectRatioMode.SCREEN
                } else {
                    com.webtoapp.data.model.FloatingWindowAspectRatioMode.FREE
                }
            },
            customAspectRatioWidth = config.webViewConfig.floatingWindowConfig.customAspectRatioWidth,
            customAspectRatioHeight = config.webViewConfig.floatingWindowConfig.customAspectRatioHeight,
            opacity = config.webViewConfig.floatingWindowConfig.opacity,
            cornerRadius = config.webViewConfig.floatingWindowConfig.cornerRadius,
            borderStyle = try {
                com.webtoapp.data.model.FloatingBorderStyle.valueOf(config.webViewConfig.floatingWindowConfig.borderStyle)
            } catch (e: Exception) {
                com.webtoapp.data.model.FloatingBorderStyle.SUBTLE
            },
            minimizedIconPath = config.webViewConfig.floatingWindowConfig.minimizedIconPath,
            minimizedIconSizePercent = config.webViewConfig.floatingWindowConfig.minimizedIconSizePercent,
            minimizedIconEdgeDocking = config.webViewConfig.floatingWindowConfig.minimizedIconEdgeDocking,
            showTitleBar = config.webViewConfig.floatingWindowConfig.showTitleBar,
            autoHideTitleBar = config.webViewConfig.floatingWindowConfig.autoHideTitleBar,
            startMinimized = config.webViewConfig.floatingWindowConfig.startMinimized,
            rememberPosition = config.webViewConfig.floatingWindowConfig.rememberPosition,
            edgeSnapping = config.webViewConfig.floatingWindowConfig.edgeSnapping,
            showResizeHandle = config.webViewConfig.floatingWindowConfig.showResizeHandle,
            lockPosition = config.webViewConfig.floatingWindowConfig.lockPosition
        ),

        swipeRefreshEnabled = config.webViewConfig.swipeRefreshEnabled,
        fullscreenEnabled = config.webViewConfig.fullscreenEnabled,

        performanceOptimization = config.webViewConfig.performanceOptimization,
        pwaOfflineEnabled = config.webViewConfig.pwaOfflineEnabled && !config.webViewConfig.clearBrowsingDataOnLaunch,
        pwaOfflineStrategy = config.webViewConfig.pwaOfflineStrategy,

        errorPageConfig = com.webtoapp.core.errorpage.ErrorPageConfig(
            mode = try { com.webtoapp.core.errorpage.ErrorPageMode.valueOf(config.webViewConfig.errorPageConfig.mode) } catch (e: Exception) { com.webtoapp.core.errorpage.ErrorPageMode.BUILTIN_STYLE },
            builtInStyle = try { com.webtoapp.core.errorpage.ErrorPageStyle.valueOf(config.webViewConfig.errorPageConfig.builtInStyle) } catch (e: Exception) { com.webtoapp.core.errorpage.ErrorPageStyle.MATERIAL },
            showMiniGame = config.webViewConfig.errorPageConfig.showMiniGame,
            miniGameType = try { com.webtoapp.core.errorpage.MiniGameType.valueOf(config.webViewConfig.errorPageConfig.miniGameType) } catch (e: Exception) { com.webtoapp.core.errorpage.MiniGameType.RANDOM },
            autoRetrySeconds = config.webViewConfig.errorPageConfig.autoRetrySeconds,
            customHtml = config.webViewConfig.errorPageConfig.customHtml.takeIf { it.isNotEmpty() },
            customMediaPath = config.webViewConfig.errorPageConfig.customMediaPath.takeIf { it.isNotEmpty() },
            retryButtonText = config.webViewConfig.errorPageConfig.retryButtonText
        ),

        allowFileAccessFromFileURLs = isLocalFileApp,
        allowUniversalAccessFromFileURLs = isLocalFileApp,

        proxyMode = config.webViewConfig.proxyMode,
        proxyHost = config.webViewConfig.proxyHost,
        proxyPort = config.webViewConfig.proxyPort,
        proxyType = config.webViewConfig.proxyType,
        pacUrl = config.webViewConfig.pacUrl,
        proxyBypassRules = config.webViewConfig.proxyBypassRules,
        proxyUsername = config.webViewConfig.proxyUsername,
        proxyPassword = config.webViewConfig.proxyPassword,
        hostsMappingEnabled = config.webViewConfig.hostsMappingEnabled,
        hostsMappings = config.webViewConfig.hostsMappings.map { entry ->
            com.webtoapp.data.model.HostMappingEntry(
                host = entry.host,
                ip = entry.ip
            )
        },

        dnsMode = config.webViewConfig.dnsMode,
        dnsConfig = com.webtoapp.data.model.DnsConfig(
            provider = config.webViewConfig.dnsConfig.provider,
            customDohUrl = config.webViewConfig.dnsConfig.customDohUrl,
            dohMode = config.webViewConfig.dnsConfig.dohMode,
            bypassSystemDns = config.webViewConfig.dnsConfig.bypassSystemDns,
            echEnabled = config.webViewConfig.dnsConfig.echEnabled
        ),
        injectScripts = config.webViewConfig.injectScripts.map { shellScript ->
            com.webtoapp.data.model.UserScript(
                name = shellScript.name,
                code = shellScript.code,
                enabled = shellScript.enabled,
                runAt = try { ScriptRunTime.valueOf(shellScript.runAt) } catch (e: Exception) { ScriptRunTime.DOCUMENT_END }
            )
        }
    )

    AppLogger.d("ShellActivity", "Constructed WebViewConfig: userAgentMode=${webViewConfig.userAgentMode.name}, customUserAgent=${webViewConfig.customUserAgent}, userAgent=${webViewConfig.userAgent}")
    AppLogger.d("ShellActivity", "InjectScripts: shellScripts=${config.webViewConfig.injectScripts.size}, mapped=${webViewConfig.injectScripts.size}")
    webViewConfig.injectScripts.forEachIndexed { i, script ->
        AppLogger.d("ShellActivity", "  Script[$i]: name='${script.name}', enabled=${script.enabled}, runAt=${script.runAt.name}, codeLength=${script.code.length}")
    }

    return webViewConfig
}
