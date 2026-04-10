package com.webtoapp.ui.shell

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.WebViewConfig

/**
 * 从 ShellConfig 构建 WebViewConfig
 * 将 Shell 配置中的 WebView 相关设置转换为 WebViewConfig 数据类
 */
fun buildWebViewConfig(config: ShellConfig): WebViewConfig {
    // HTML/FRONTEND 应用需要允许本地文件访问（加载 CSS/JS 等资源）
    // WEB 应用加载远程 URL，必须保持 false 以防止恶意网页读取本地文件
    val isLocalFileApp = config.appType.trim().uppercase() in setOf("HTML", "FRONTEND")

    val webViewConfig = WebViewConfig(
        javaScriptEnabled = config.webViewConfig.javaScriptEnabled,
        domStorageEnabled = config.webViewConfig.domStorageEnabled,
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
        downloadEnabled = true, // 确保下载功能始终启用
        hideToolbar = config.webViewConfig.hideToolbar,
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
        // 状态栏配置
        statusBarColorMode = try { com.webtoapp.data.model.StatusBarColorMode.valueOf(config.webViewConfig.statusBarColorMode) } catch (e: Exception) { com.webtoapp.data.model.StatusBarColorMode.THEME },
        statusBarColor = config.webViewConfig.statusBarColor,
        statusBarDarkIcons = config.webViewConfig.statusBarDarkIcons,
        statusBarBackgroundType = try { com.webtoapp.data.model.StatusBarBackgroundType.valueOf(config.webViewConfig.statusBarBackgroundType) } catch (e: Exception) { com.webtoapp.data.model.StatusBarBackgroundType.COLOR },
        statusBarBackgroundImage = config.webViewConfig.statusBarBackgroundImage,
        statusBarBackgroundAlpha = config.webViewConfig.statusBarBackgroundAlpha,
        statusBarHeightDp = config.webViewConfig.statusBarHeightDp,
        // 长按菜单配置
        longPressMenuEnabled = config.webViewConfig.longPressMenuEnabled,
        longPressMenuStyle = try { com.webtoapp.data.model.LongPressMenuStyle.valueOf(config.webViewConfig.longPressMenuStyle) } catch (e: Exception) { com.webtoapp.data.model.LongPressMenuStyle.FULL },
        adBlockToggleEnabled = config.webViewConfig.adBlockToggleEnabled,
        popupBlockerEnabled = config.webViewConfig.popupBlockerEnabled,
        popupBlockerToggleEnabled = config.webViewConfig.popupBlockerToggleEnabled,
        // 浏览器兼容性增强配置
        initialScale = config.webViewConfig.initialScale,
        viewportMode = try { com.webtoapp.data.model.ViewportMode.valueOf(config.webViewConfig.viewportMode) } catch (e: Exception) { com.webtoapp.data.model.ViewportMode.DEFAULT },
        newWindowBehavior = try { com.webtoapp.data.model.NewWindowBehavior.valueOf(config.webViewConfig.newWindowBehavior) } catch (e: Exception) { com.webtoapp.data.model.NewWindowBehavior.SAME_WINDOW },
        enablePaymentSchemes = config.webViewConfig.enablePaymentSchemes,
        enableShareBridge = config.webViewConfig.enableShareBridge,
        enableZoomPolyfill = config.webViewConfig.enableZoomPolyfill,
        enableCrossOriginIsolation = config.webViewConfig.enableCrossOriginIsolation,
        disableShields = config.webViewConfig.disableShields,
        keepScreenOn = config.webViewConfig.keepScreenOn,
        showFloatingBackButton = config.webViewConfig.showFloatingBackButton,
        blockSystemNavigationGesture = config.webViewConfig.blockSystemNavigationGesture,
        // 键盘调整模式
        keyboardAdjustMode = try {
            com.webtoapp.data.model.KeyboardAdjustMode.valueOf(config.webViewConfig.keyboardAdjustMode)
        } catch (e: Exception) {
            AppLogger.w("ShellActivity", "KeyboardAdjustMode parse failed: '${config.webViewConfig.keyboardAdjustMode}', falling back to RESIZE", e)
            com.webtoapp.data.model.KeyboardAdjustMode.RESIZE
        },
        // 悬浮小窗配置
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
        // 下拉刷新 / 视频全屏
        swipeRefreshEnabled = config.webViewConfig.swipeRefreshEnabled,
        fullscreenEnabled = config.webViewConfig.fullscreenEnabled,
        // 性能优化 / PWA 离线
        performanceOptimization = config.webViewConfig.performanceOptimization,
        pwaOfflineEnabled = config.webViewConfig.pwaOfflineEnabled,
        pwaOfflineStrategy = config.webViewConfig.pwaOfflineStrategy,
        // 网络错误页配置
        errorPageConfig = com.webtoapp.core.errorpage.ErrorPageConfig(
            mode = try { com.webtoapp.core.errorpage.ErrorPageMode.valueOf(config.webViewConfig.errorPageConfig.mode) } catch (e: Exception) { com.webtoapp.core.errorpage.ErrorPageMode.BUILTIN_STYLE },
            builtInStyle = try { com.webtoapp.core.errorpage.ErrorPageStyle.valueOf(config.webViewConfig.errorPageConfig.builtInStyle) } catch (e: Exception) { com.webtoapp.core.errorpage.ErrorPageStyle.MATERIAL },
            showMiniGame = config.webViewConfig.errorPageConfig.showMiniGame,
            miniGameType = try { com.webtoapp.core.errorpage.MiniGameType.valueOf(config.webViewConfig.errorPageConfig.miniGameType) } catch (e: Exception) { com.webtoapp.core.errorpage.MiniGameType.RANDOM },
            autoRetrySeconds = config.webViewConfig.errorPageConfig.autoRetrySeconds
        ),
        // 本地文件访问配置 - HTML/FRONTEND 应用必须启用
        allowFileAccessFromFileURLs = isLocalFileApp,
        allowUniversalAccessFromFileURLs = isLocalFileApp,
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
