package com.webtoapp.ui.shell

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.LongPressHandler
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.Announcement
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.util.TvUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Shell 模式主屏幕 Composable
 *
 * 包含所有 UI 状态声明、初始化逻辑、以及各子组件的组合。
 * 从 ShellActivity.kt 中提取。
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    config: ShellConfig,
    deepLinkUrl: String? = null,
    onWebViewCreated: (WebView) -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onFullscreenModeChanged: (Boolean) -> Unit,
    onForcedRunStateChanged: (Boolean, ForcedRunConfig?) -> Unit,
    // Status bar配置
    statusBarBackgroundType: String = "COLOR",
    statusBarBackgroundColor: String? = null,
    statusBarBackgroundImage: String? = null,
    statusBarBackgroundAlpha: Float = 1.0f,
    statusBarHeightDp: Int = 0,
    // Dark mode status bar配置
    statusBarBackgroundTypeDark: String = "COLOR",
    statusBarBackgroundColorDark: String? = null,
    statusBarBackgroundImageDark: String? = null,
    statusBarBackgroundAlphaDark: Float = 1.0f
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as android.app.Activity
    val activation = WebToAppApplication.activation
    val announcement = WebToAppApplication.announcement
    val adBlocker = WebToAppApplication.adBlock
    // 强制运行状态管理（逻辑已提取到 ShellForcedRunState.kt）
    val forcedRunState = rememberForcedRunState(context)
    val forcedRunActive = forcedRunState.forcedRunActive
    val forcedRunRemainingMs = forcedRunState.forcedRunRemainingMs
    val forcedRunBlocked = forcedRunState.forcedRunBlocked
    val forcedRunBlockedMessage = forcedRunState.forcedRunBlockedMessage

    // Normalize appType (avoid case/whitespace issues)
    val appType = config.appType.trim().uppercase()
    // 调试：打印 appType
    AppLogger.d("ShellScreen", "appType='${config.appType}' (normalized='$appType'), targetUrl='${config.targetUrl}'")
    
    // 状态
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    
    // Activation状态：如果启用了激活码，默认未激活，防止 WebView 在检查完成前加载
    var isActivated by remember { mutableStateOf(!config.activationEnabled) }
    // Activation检查是否完成（用于显示加载状态）
    var isActivationChecked by remember { mutableStateOf(!config.activationEnabled) }
    // 当渲染进程被杀死时自增，强制 AndroidView 重建
    var webViewRecreationKey by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 同步检查启动画面配置（必须在 WebView 初始化之前）
    // 同时检查加密和非加密版本
    val splashMediaExists = remember {
        if (config.splashEnabled) {
            val extension = if (config.splashType == "VIDEO") "mp4" else "png"
            val assetPath = "splash_media.$extension"
            val encryptedPath = "$assetPath.enc"
            
            // 先检查加密版本
            val hasEncrypted = try {
                context.assets.open(encryptedPath).close()
                true
            } catch (e: Exception) { false }
            
            // 再检查非加密版本
            val hasNormal = try {
                context.assets.open(assetPath).close()
                true
            } catch (e: Exception) { false }
            
            val exists = hasEncrypted || hasNormal
            AppLogger.d("ShellActivity", "同步检查: 启动画面媒体 encrypted=$hasEncrypted, normal=$hasNormal, exists=$exists")
            exists
        } else false
    }
    
    // Start画面状态 - 根据配置同步初始化
    var showSplash by remember { mutableStateOf(config.splashEnabled && splashMediaExists) }
    var splashCountdown by remember { mutableIntStateOf(if (config.splashEnabled && splashMediaExists) config.splashDuration else 0) }
    var originalOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    
    // Handle启动画面横屏
    LaunchedEffect(showSplash) {
        if (showSplash && config.splashLandscape) {
            originalOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    // WebView引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // 长按菜单状态
    var showLongPressMenu by remember { mutableStateOf(false) }
    var longPressResult by remember { mutableStateOf<LongPressHandler.LongPressResult?>(null) }
    var longPressTouchX by remember { mutableFloatStateOf(0f) }
    var longPressTouchY by remember { mutableFloatStateOf(0f) }
    val longPressHandler = remember { LongPressHandler(context, scope) }

    // Initialize配置
    LaunchedEffect(Unit) {
        // 设置界面语言（根据 APK 打包时的配置）
        try {
            val appLanguage = when (config.language.uppercase()) {
                "ENGLISH" -> com.webtoapp.core.i18n.AppLanguage.ENGLISH
                "ARABIC" -> com.webtoapp.core.i18n.AppLanguage.ARABIC
                else -> com.webtoapp.core.i18n.AppLanguage.CHINESE
            }
            Strings.setLanguage(appLanguage)
            AppLogger.d("ShellActivity", "设置界面语言: ${config.language} -> $appLanguage")
        } catch (e: Exception) {
            AppLogger.e("ShellActivity", "设置语言失败", e)
        }
        
        // Configure广告拦截
        if (config.adBlockEnabled) {
            adBlocker.initialize(config.adBlockRules, useDefaultRules = true)
            adBlocker.setEnabled(true)
        }

        // Check激活状态
        if (config.activationEnabled) {
            // 如果配置为每次都需要验证，则重置激活状态
            if (config.activationRequireEveryTime) {
                activation.resetActivation(-1L)
                isActivated = false
                isActivationChecked = true
                showActivationDialog = true
            } else {
                // Shell 模式使用固定 ID
                val activated = activation.isActivated(-1L).first()
                isActivated = activated
                isActivationChecked = true
                if (!activated) {
                    showActivationDialog = true
                }
            }
        }

        // Check公告
        if (config.announcementEnabled && isActivated && config.announcementTitle.isNotEmpty()) {
            val ann = Announcement(
                title = config.announcementTitle,
                content = config.announcementContent,
                linkUrl = config.announcementLink.ifEmpty { null },
                showOnce = config.announcementShowOnce
            )
            showAnnouncementDialog = announcement.shouldShowAnnouncement(-1L, ann)
        }

        // Set屏幕方向（根据应用类型判断）
        // ★ Issue #66 fix: ALL app types now use webViewConfig.orientationMode as primary source.
        // Previously, non-WEB types (PHP, Node.js, Python, Go, WordPress, HTML, etc.)
        // only read their type-specific landscapeMode boolean, which meant AUTO rotation
        // and all sensor modes were silently ignored.
        //
        // Resolution order:
        // 1. webViewConfig.orientationMode (supports all 7 modes)
        // 2. Type-specific landscapeMode boolean (backward compat for APKs built before orientationMode existed)
        val validOrientationModes = setOf("PORTRAIT", "LANDSCAPE", "REVERSE_PORTRAIT", "REVERSE_LANDSCAPE", "SENSOR_PORTRAIT", "SENSOR_LANDSCAPE", "AUTO")
        val orientModeFromConfig = config.webViewConfig.orientationMode.uppercase()
        val resolvedOrientationMode = if (orientModeFromConfig in validOrientationModes && orientModeFromConfig != "PORTRAIT") {
            // ★ orientationMode explicitly set to a non-default value → use it for ALL app types
            orientModeFromConfig
        } else {
            // orientationMode is PORTRAIT (default) or invalid → check type-specific landscapeMode
            // for backward compatibility with APKs built before orientationMode was added
            val typeSpecificLandscape = when (appType) {
                "HTML", "FRONTEND" -> config.htmlConfig.landscapeMode
                "IMAGE", "VIDEO" -> config.mediaConfig.landscape
                "GALLERY" -> config.galleryConfig.orientation.uppercase() == "LANDSCAPE"
                "WORDPRESS" -> config.wordpressConfig.landscapeMode
                "NODEJS_APP" -> config.nodejsConfig.landscapeMode
                "PHP_APP" -> config.phpAppConfig.landscapeMode
                "PYTHON_APP" -> config.pythonAppConfig.landscapeMode
                "GO_APP" -> config.goAppConfig.landscapeMode
                "MULTI_WEB" -> config.multiWebConfig.landscapeMode
                else -> config.webViewConfig.landscapeMode
            }
            if (typeSpecificLandscape) "LANDSCAPE" else "PORTRAIT"
        }
        
        when (resolvedOrientationMode) {
            "LANDSCAPE" -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            "REVERSE_PORTRAIT" -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
            "REVERSE_LANDSCAPE" -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            "SENSOR_PORTRAIT" -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
            "SENSOR_LANDSCAPE" -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            "AUTO" -> {
                // ★ 自动旋转：跟随系统自动旋转设置（尊重用户偏好），平板设备友好
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            }
            else -> {
                if (TvUtils.isTv(context)) {
                    // TV 设备不锁定方向，保持默认横屏
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else {
                    // 手机/平板锁定为竖屏模式
                    @SuppressLint("SourceLockedOrientationActivity")
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }

        // Start画面已在同步初始化阶段处理
        AppLogger.d("ShellActivity", "LaunchedEffect: showSplash=$showSplash, splashCountdown=$splashCountdown")
        
    }

    // 强制运行副作用管理（逻辑已提取到 ShellForcedRunState.kt）
    ForcedRunEffects(
        state = forcedRunState,
        config = config.forcedRunConfig,
        isActivated = isActivated,
        context = context,
        onForcedRunStateChanged = onForcedRunStateChanged
    )

    // Start画面倒计时（仅用于图片类型，视频类型由播放器控制）
    LaunchedEffect(showSplash, splashCountdown) {
        // Video类型不使用倒计时，由视频播放器控制结束
        if (config.splashType == "VIDEO") return@LaunchedEffect
        
        if (showSplash && splashCountdown > 0) {
            delay(1000L)
            splashCountdown--
        } else if (showSplash && splashCountdown <= 0) {
            showSplash = false
            // 恢复原始方向
            if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                activity.requestedOrientation = originalOrientation
                originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
    
    // ===== 背景音乐播放器（逻辑已提取到 ShellBgmPlayer.kt）=====
    val bgmState = rememberBgmPlayerState(context, config)

    // WebView回调（逻辑已提取到 ShellWebViewCallbacks.kt）
    val webViewCallbacks = remember {
        createShellWebViewCallbacks(
            context = context,
            config = config,
            webViewRefProvider = { webViewRef },
            currentUrlProvider = { currentUrl },
            longPressHandler = longPressHandler,
            handleShowCustomView = onShowCustomView,
            handleHideCustomView = onHideCustomView,
            handleFileChooser = onFileChooser,
            updateLoading = { isLoading = it },
            updateUrl = { currentUrl = it },
            updateTitle = { pageTitle = it },
            updateProgress = { loadProgress = it },
            updateError = { errorMessage = it },
            updateNavigation = { back, forward -> canGoBack = back; canGoForward = forward },
            updateWebViewRef = { webViewRef = it },
            notifyRecreationKeyIncrement = { webViewRecreationKey++ },
            notifyLongPressMenu = { result, x, y ->
                longPressResult = result
                longPressTouchX = x
                longPressTouchY = y
                showLongPressMenu = true
            }
        )
    }

    // 转换配置（逻辑已提取到 ShellWebViewConfig.kt）
    val webViewConfig = buildWebViewConfig(config)

    val webViewManager = remember { 
        com.webtoapp.core.webview.WebViewManager(context, adBlocker)
    }

    // Yes否隐藏工具栏（全屏模式 = 沉浸式）
    val hideToolbar = config.webViewConfig.hideToolbar
    // 仅隐藏浏览器工具栏（不触发沉浸式，保留系统状态栏和导航栏）
    val hideBrowserToolbar = config.webViewConfig.hideBrowserToolbar
    // 下拉刷新开关
    val swipeRefreshEnabled = config.webViewConfig.swipeRefreshEnabled

    LaunchedEffect(hideToolbar) {
        onFullscreenModeChanged(hideToolbar)
    }
    
    // 关闭启动画面的回调（提前定义）
    val closeSplash = {
        showSplash = false
        // 恢复原始方向
        if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity.requestedOrientation = originalOrientation
            originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 整体容器，确保启动画面覆盖在 Scaffold 之上
    // 使用 fillMaxSize 确保内容铺满整个屏幕（包括状态栏区域）
    Box(modifier = Modifier.fillMaxSize()) {
    
    // Scaffold 布局（逻辑已提取到 ShellScaffoldLayout.kt）
    ShellScaffoldLayout(
        config = config,
        appType = appType,
        hideToolbar = hideToolbar,
        hideBrowserToolbar = hideBrowserToolbar,
        isLoading = isLoading,
        loadProgress = loadProgress,
        pageTitle = pageTitle,
        currentUrl = currentUrl,
        errorMessage = errorMessage,
        isActivationChecked = isActivationChecked,
        isActivated = isActivated,
        forcedRunActive = forcedRunActive,
        forcedRunBlocked = forcedRunBlocked,
        forcedRunBlockedMessage = forcedRunBlockedMessage,
        forcedRunRemainingMs = forcedRunRemainingMs,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        webViewRecreationKey = webViewRecreationKey,
        webViewRef = webViewRef,
        webViewConfig = webViewConfig,
        webViewCallbacks = webViewCallbacks,
        webViewManager = webViewManager,
        deepLinkUrl = deepLinkUrl,
        bgmState = bgmState,
        swipeRefreshEnabled = swipeRefreshEnabled,
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = false },
        onWebViewCreated = onWebViewCreated,
        onWebViewRefUpdated = { webViewRef = it },
        onShowActivationDialog = { showActivationDialog = true },
        onErrorDismiss = { errorMessage = null },
        onActivityFinish = { activity.finish() },
        statusBarHeightDp = statusBarHeightDp
    )

    // Activation码对话框（逻辑已提取到 ShellDialogs.kt）
    if (showActivationDialog) {
        ShellActivationDialog(
            config = config,
            onDismiss = { showActivationDialog = false },
            onActivated = {
                isActivated = true
                showActivationDialog = false
                // Check公告
                if (config.announcementEnabled && config.announcementTitle.isNotEmpty()) {
                    val ann = Announcement(
                        title = config.announcementTitle,
                        content = config.announcementContent,
                        linkUrl = config.announcementLink.ifEmpty { null },
                        showOnce = config.announcementShowOnce
                    )
                    showAnnouncementDialog = kotlinx.coroutines.runBlocking { announcement.shouldShowAnnouncement(-1L, ann) }
                }
            }
        )
    }

    // Announcement对话框（逻辑已提取到 ShellDialogs.kt）
    if (showAnnouncementDialog && config.announcementTitle.isNotEmpty()) {
        ShellAnnouncementDialog(
            config = config,
            onDismiss = { showAnnouncementDialog = false }
        )
    }
    
    // 强制运行权限引导对话框（逻辑已提取到 ShellDialogs.kt）
    if (forcedRunState.showForcedRunPermissionDialog && config.forcedRunConfig != null) {
        ShellForcedRunPermissionDialog(
            config = config,
            forcedRunActive = forcedRunActive,
            onDismiss = { forcedRunState.showForcedRunPermissionDialog = false }
        )
    }

    // Start画面覆盖层（在 Box 内，覆盖在 Scaffold 之上）
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        ShellSplashOverlay(
            splashType = config.splashType,
            countdown = splashCountdown,
            videoStartMs = config.splashVideoStartMs,
            videoEndMs = config.splashVideoEndMs,
            fillScreen = config.splashFillScreen,
            enableAudio = config.splashEnableAudio,
            // 点击跳过（仅当启用时）
            onSkip = if (config.splashClickToSkip) { closeSplash } else null,
            // Play完成回调（始终需要）
            onComplete = closeSplash
        )
    }
    
    // 长按菜单（逻辑已提取到 ShellLongPressMenu.kt）
    if (showLongPressMenu && longPressResult != null) {
        ShellLongPressMenu(
            menuStyle = config.webViewConfig.longPressMenuStyle,
            result = longPressResult!!,
            touchX = longPressTouchX,
            touchY = longPressTouchY,
            longPressHandler = longPressHandler,
            onDismiss = {
                showLongPressMenu = false
                longPressResult = null
            }
        )
    }
    
    // Status bar背景覆盖层
    // 放在 Box 内部最上层，覆盖在所有内容之上，使用 align 固定在顶部
    // 全屏模式 + 非全屏模式（有自定义配置时）都显示覆盖层
    val isDarkTheme = com.webtoapp.ui.theme.LocalIsDarkTheme.current
    val effectiveBgType = if (isDarkTheme) statusBarBackgroundTypeDark else statusBarBackgroundType
    val effectiveBgColor = if (isDarkTheme) statusBarBackgroundColorDark else statusBarBackgroundColor
    val effectiveBgImage = if (isDarkTheme) statusBarBackgroundImageDark else statusBarBackgroundImage
    val effectiveBgAlpha = if (isDarkTheme) statusBarBackgroundAlphaDark else statusBarBackgroundAlpha
    val showOverlay = (hideToolbar && config.webViewConfig.showStatusBarInFullscreen) ||
            (!hideToolbar && (effectiveBgType != "COLOR" || effectiveBgColor != null))
    if (showOverlay) {
        com.webtoapp.ui.components.StatusBarOverlay(
            show = true,
            backgroundType = effectiveBgType,
            backgroundColor = effectiveBgColor,
            backgroundImagePath = effectiveBgImage,
            alpha = effectiveBgAlpha,
            heightDp = statusBarHeightDp,
            modifier = Modifier.align(Alignment.TopStart)
        )
        // Force status bar icon color based on overlay background brightness
        val view = activity.window.decorView
        val insetsController = androidx.core.view.WindowInsetsControllerCompat(activity.window, view)
        val isLightOverlay = effectiveBgType == "COLOR" && effectiveBgColor != null && run {
            try {
                val color = android.graphics.Color.parseColor(effectiveBgColor)
                val luminance = (0.299 * android.graphics.Color.red(color) +
                        0.587 * android.graphics.Color.green(color) +
                        0.114 * android.graphics.Color.blue(color)) / 255.0
                luminance > 0.5
            } catch (e: Exception) { false }
        }
        insetsController.isAppearanceLightStatusBars = isLightOverlay
    }
    
    } // 关闭外层 Box
}
