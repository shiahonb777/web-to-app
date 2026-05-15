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
        cacheEnabled = config.webViewConfig.cacheEnabled,
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
        disableShields = config.webViewConfig.disableShields,
        decodeBase64DeepLinks = config.webViewConfig.decodeBase64DeepLinks,
        mediaAutoplayEnabled = config.webViewConfig.mediaAutoplayEnabled,
        acceptThirdPartyCookies = config.webViewConfig.acceptThirdPartyCookies,
        enableKernelDisguise = config.webViewConfig.enableKernelDisguise,
        enableImageRepair = config.webViewConfig.enableImageRepair,
        enableScrollMemory = config.webViewConfig.enableScrollMemory,
        enableHttpsUpgrade = config.webViewConfig.enableHttpsUpgrade,
        enableOAuthExternalRedirect = config.webViewConfig.enableOAuthExternalRedirect,
        enableClipboardPolyfill = config.webViewConfig.enableClipboardPolyfill,
        enableNotificationPolyfill = config.webViewConfig.enableNotificationPolyfill,
        safeBrowsingEnabled = config.webViewConfig.safeBrowsingEnabled,
        geolocationEnabled = config.webViewConfig.geolocationEnabled,
        enableOrientationPolyfill = config.webViewConfig.enableOrientationPolyfill,
        enableCompatPolyfills = config.webViewConfig.enableCompatPolyfills,
        enableNativeBridge = config.webViewConfig.enableNativeBridge,
        javaScriptCanOpenWindows = config.webViewConfig.javaScriptCanOpenWindows,
        databaseEnabled = config.webViewConfig.databaseEnabled,
        enableCookiePersistence = config.webViewConfig.enableCookiePersistence,
        enablePrivateNetworkBridge = config.webViewConfig.enablePrivateNetworkBridge,
        allowMixedContent = config.webViewConfig.allowMixedContent,
        enableGpc = config.webViewConfig.enableGpc,
        enableCookieConsentBlock = config.webViewConfig.enableCookieConsentBlock,
        enableReferrerPolicy = config.webViewConfig.enableReferrerPolicy,
        enableTrackerBlocking = config.webViewConfig.enableTrackerBlocking,
        enableBlobDownloadInterception = config.webViewConfig.enableBlobDownloadInterception,
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
            opacity = config.webViewConfig.floatingWindowConfig.opacity,
            cornerRadius = config.webViewConfig.floatingWindowConfig.cornerRadius,
            borderStyle = try {
                com.webtoapp.data.model.FloatingBorderStyle.valueOf(config.webViewConfig.floatingWindowConfig.borderStyle)
            } catch (e: Exception) {
                com.webtoapp.data.model.FloatingBorderStyle.SUBTLE
            },
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
        pwaOfflineEnabled = config.webViewConfig.pwaOfflineEnabled,
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
            bypassSystemDns = config.webViewConfig.dnsConfig.bypassSystemDns
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
