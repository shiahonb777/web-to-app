package com.webtoapp.ui.webview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.webtoapp.core.logging.AppLogger
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.webtoapp.ui.components.EdgeSwipeRefreshLayout
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.bgm.BgmPlayer
import com.webtoapp.core.webview.LocalHttpServer
import com.webtoapp.core.webview.LongPressHandler
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.core.webview.WebViewManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.KeyboardAdjustMode
import com.webtoapp.data.model.LongPressMenuStyle
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.data.model.SplashType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.getActivationCodeStrings
import android.content.pm.ActivityInfo
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.util.DownloadHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.webtoapp.ui.shared.WindowHelper
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.core.wordpress.WordPressPhpRuntime
import com.webtoapp.core.wordpress.WordPressManager
import com.webtoapp.data.model.WordPressConfig
import com.webtoapp.core.php.PhpAppRuntime
import com.webtoapp.core.stats.AppUsageTracker
import androidx.compose.ui.text.style.TextOverflow

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    appId: Long,
    directUrl: String?,
    previewApp: com.webtoapp.data.model.WebApp? = null,
    testUrl: String? = null,
    testModuleIds: List<String>? = null,
    onStatusBarConfigChanged: ((com.webtoapp.data.model.StatusBarColorMode, String?, Boolean?, Boolean, com.webtoapp.data.model.StatusBarBackgroundType, com.webtoapp.data.model.StatusBarColorMode, String?, Boolean?, com.webtoapp.data.model.StatusBarBackgroundType) -> Unit)? = null,
    onWebViewCreated: (WebView) -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onFullscreenModeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val repository = WebToAppApplication.repository
    val activation = WebToAppApplication.activation
    val announcement = WebToAppApplication.announcement
    val adBlocker = WebToAppApplication.adBlock
    
    // Yes否为测试模式
    val isTestMode = !testUrl.isNullOrBlank()

    // 状态
    var webApp by remember { mutableStateOf<WebApp?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    // Activation状态：默认未激活，防止 WebView 在检查完成前加载
    var isActivated by remember { mutableStateOf(false) }
    // Activation检查是否完成
    var isActivationChecked by remember { mutableStateOf(false) }
    // 当渲染进程被杀死时自增，强制 AndroidView 重建
    var webViewRecreationKey by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var adCapabilityNoticeShown by remember { mutableStateOf(false) }
    var strictHostFallbackTriggered by remember { mutableStateOf(false) }
    
    // Start画面状态
    var showSplash by remember { mutableStateOf(false) }
    var splashCountdown by remember { mutableIntStateOf(0) }
    var originalOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

    // Background music播放器
    val bgmPlayer = remember { BgmPlayer(context) }

    // WebView引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // 通过 JavaScript 追踪页面真实滚动位置（线程安全，JS Interface 在后台线程回调）
    val jsScrollTop = remember { AtomicInteger(0) }
    val scrollBridge = remember {
        object {
            @android.webkit.JavascriptInterface
            fun onScroll(y: Int) {
                jsScrollTop.set(y)
            }
        }
    }
    
    // 长按菜单状态
    var showLongPressMenu by remember { mutableStateOf(false) }
    var longPressResult by remember { mutableStateOf<LongPressHandler.LongPressResult?>(null) }
    var longPressTouchX by remember { mutableFloatStateOf(0f) }
    var longPressTouchY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val longPressHandler = remember { LongPressHandler(context, scope) }
    
    // 控制台状态
    var showConsole by remember { mutableStateOf(false) }
    var consoleMessages by remember { mutableStateOf<List<ConsoleLogEntry>>(emptyList()) }
    
    // Status bar背景配置（用于预览时显示）
    var statusBarBackgroundType by remember { mutableStateOf("COLOR") }
    var statusBarBackgroundColor by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundImage by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundAlpha by remember { mutableFloatStateOf(1.0f) }
    var statusBarHeightDp by remember { mutableIntStateOf(0) }
    // Status bar深色模式背景配置
    var statusBarBackgroundTypeDarkLocal by remember { mutableStateOf("COLOR") }
    var statusBarBackgroundColorDark by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundImageDark by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundAlphaDark by remember { mutableFloatStateOf(1.0f) }
    
    // WordPress 预览状态
    var wordPressPreviewState by remember { mutableStateOf<WordPressPreviewState>(WordPressPreviewState.Idle) }
    val phpRuntime = remember { WordPressPhpRuntime(context) }
    val wpDownloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var wpRetryTrigger by remember { mutableIntStateOf(0) }
    
    // PHP 应用预览状态
    var phpAppPreviewState by remember { mutableStateOf<PhpAppPreviewState>(PhpAppPreviewState.Idle) }
    val phpAppRuntime = remember { PhpAppRuntime(context) }
    val phpAppDownloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var phpAppRetryTrigger by remember { mutableIntStateOf(0) }
    
    // Python 应用预览状态
    var pythonAppPreviewState by remember { mutableStateOf<PythonAppPreviewState>(PythonAppPreviewState.Idle) }
    val pythonRuntime = remember { com.webtoapp.core.python.PythonRuntime(context) }
    val pythonHttpServer = remember { com.webtoapp.core.webview.LocalHttpServer(context) }
    var pythonAppRetryTrigger by remember { mutableIntStateOf(0) }
    
    // Node.js 应用预览状态
    var nodeJsAppPreviewState by remember { mutableStateOf<NodeJsAppPreviewState>(NodeJsAppPreviewState.Idle) }
    val nodeRuntime = remember { com.webtoapp.core.nodejs.NodeRuntime(context) }
    val nodeHttpServer = remember { com.webtoapp.core.webview.LocalHttpServer(context) }
    var nodeJsAppRetryTrigger by remember { mutableIntStateOf(0) }
    
    // Go 应用预览状态
    var goAppPreviewState by remember { mutableStateOf<GoAppPreviewState>(GoAppPreviewState.Idle) }
    val goRuntime = remember { com.webtoapp.core.golang.GoRuntime(context) }
    val goHttpServer = remember { com.webtoapp.core.webview.LocalHttpServer(context) }
    var goAppRetryTrigger by remember { mutableIntStateOf(0) }
    
    // 当 webApp 加载完成后，通知状态栏配置并更新本地状态
    LaunchedEffect(webApp) {
        webApp?.let { app ->
            onStatusBarConfigChanged?.invoke(
                app.webViewConfig.statusBarColorMode,
                app.webViewConfig.statusBarColor,
                app.webViewConfig.statusBarDarkIcons,
                app.webViewConfig.showStatusBarInFullscreen,
                app.webViewConfig.statusBarBackgroundType,
                app.webViewConfig.statusBarColorModeDark,
                app.webViewConfig.statusBarColorDark,
                app.webViewConfig.statusBarDarkIconsDark,
                app.webViewConfig.statusBarBackgroundTypeDark
            )
            // Update state栏背景配置
            statusBarBackgroundType = app.webViewConfig.statusBarBackgroundType.name
            statusBarBackgroundColor = app.webViewConfig.statusBarColor
            statusBarBackgroundImage = app.webViewConfig.statusBarBackgroundImage
            statusBarBackgroundAlpha = app.webViewConfig.statusBarBackgroundAlpha
            statusBarHeightDp = app.webViewConfig.statusBarHeightDp
            // Update深色模式状态栏背景配置
            statusBarBackgroundTypeDarkLocal = app.webViewConfig.statusBarBackgroundTypeDark.name
            statusBarBackgroundColorDark = app.webViewConfig.statusBarColorDark
            statusBarBackgroundImageDark = app.webViewConfig.statusBarBackgroundImageDark
            statusBarBackgroundAlphaDark = app.webViewConfig.statusBarBackgroundAlphaDark
            // Update导航栏配置和键盘调整模式
            (context as? WebViewActivity)?.let { activity ->
                activity.showNavigationBarInFullscreen = app.webViewConfig.showNavigationBarInFullscreen
                activity.keyboardAdjustMode = app.webViewConfig.keyboardAdjustMode
            }

            if (!adCapabilityNoticeShown && hasConfiguredAds(app)) {
                adCapabilityNoticeShown = true
                AppLogger.w(
                    "WebViewActivity",
                    "Ad config detected for appId=${app.id}, but AdManager is placeholder-only and no ad SDK is integrated"
                )
                Toast.makeText(context, Strings.adSdkNotIntegrated, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Load应用配置
    LaunchedEffect(appId, directUrl, testUrl, previewApp) {
        // 测试模式：直接标记为已激活，不需要加载应用配置
        if (isTestMode) {
            isActivated = true
            isActivationChecked = true
            return@LaunchedEffect
        }
        
        // 预览模式：使用传入的完整 WebApp 配置（与保存后打开一致）
        // 跳过激活码检查、启动画面、背景音乐、公告（预览不需要这些）
        if (previewApp != null) {
            webApp = previewApp
            isActivated = true
            isActivationChecked = true
            
            // 应用广告拦截（预览时也生效）
            if (previewApp.adBlockEnabled) {
                adBlocker.initialize(previewApp.adBlockRules, useDefaultRules = true)
                adBlocker.setEnabled(true)
            }
            
            // 设置屏幕方向模式
            when (previewApp.webViewConfig.orientationMode) {
                com.webtoapp.data.model.OrientationMode.LANDSCAPE -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                com.webtoapp.data.model.OrientationMode.AUTO -> {
                    // Auto rotation: respects the system auto-rotate setting
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                }
                com.webtoapp.data.model.OrientationMode.PORTRAIT -> {
                    if (!com.webtoapp.util.TvUtils.isTv(context)) {
                        @android.annotation.SuppressLint("SourceLockedOrientationActivity")
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                else -> { /* keep default */ }
            }
            
            // 保持屏幕常亮
            if (previewApp.webViewConfig.screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.ALWAYS) {
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            return@LaunchedEffect
        }
        
        // If it is直接URL模式，不需要激活检查
        if (!directUrl.isNullOrBlank()) {
            isActivated = true
            isActivationChecked = true
            return@LaunchedEffect
        }
        
        if (appId > 0) {
            val app = repository.getWebApp(appId)
            webApp = app
            if (app != null) {
                // Configure广告拦截
                if (app.adBlockEnabled) {
                    adBlocker.initialize(app.adBlockRules, useDefaultRules = true)
                    adBlocker.setEnabled(true)
                }

                // Check激活状态
                if (app.activationEnabled) {
                    // 如果配置为每次都需要验证，则重置激活状态
                    if (app.activationRequireEveryTime) {
                        activation.resetActivation(appId)
                        isActivated = false
                        isActivationChecked = true
                        showActivationDialog = true
                    } else {
                        val activated = activation.isActivated(appId).first()
                        isActivated = activated
                        isActivationChecked = true
                        if (!activated) {
                            showActivationDialog = true
                        }
                    }
                } else {
                    // 未启用激活码，直接标记为已激活
                    isActivated = true
                    isActivationChecked = true
                }

                // Check公告（启动时触发）
                if (app.announcementEnabled && isActivated && app.announcement?.triggerOnLaunch == true) {
                    val shouldShow = announcement.shouldShowAnnouncementForTrigger(
                        appId, 
                        app.announcement,
                        isLaunch = true
                    )
                    showAnnouncementDialog = shouldShow
                }

                // Check启动画面
                if (app.splashEnabled && app.splashConfig != null && isActivated) {
                    val mediaPath = app.splashConfig.mediaPath
                    if (mediaPath != null && File(mediaPath).exists()) {
                        showSplash = true
                        splashCountdown = app.splashConfig.duration
                        
                        // Handle横屏显示
                        if (app.splashConfig.orientation == SplashOrientation.LANDSCAPE) {
                            originalOrientation = activity.requestedOrientation
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }
                }
                
                // Initialize背景音乐
                if (app.bgmEnabled && app.bgmConfig != null && isActivated) {
                    bgmPlayer.initialize(app.bgmConfig)
                }
                
                // 设置屏幕方向模式
                when (app.webViewConfig.orientationMode) {
                    com.webtoapp.data.model.OrientationMode.LANDSCAPE -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    }
                    com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    }
                    com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                    com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                    com.webtoapp.data.model.OrientationMode.AUTO -> {
                        // Auto rotation: respects the system auto-rotate setting
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                    }
                    com.webtoapp.data.model.OrientationMode.PORTRAIT -> {
                        if (com.webtoapp.util.TvUtils.isTv(context)) {
                            // TV 设备不锁定方向，保持默认横屏
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            // 手机/平板锁定为竖屏模式
                            @android.annotation.SuppressLint("SourceLockedOrientationActivity")
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                }
                
                // 保持屏幕常亮（支持三种模式）
                val awakeMode = app.webViewConfig.screenAwakeMode
                when (awakeMode) {
                    com.webtoapp.data.model.ScreenAwakeMode.ALWAYS -> {
                        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    com.webtoapp.data.model.ScreenAwakeMode.TIMED -> {
                        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        // 定时后移除常亮标志
                        val timeoutMs = app.webViewConfig.screenAwakeTimeoutMinutes * 60 * 1000L
                        kotlinx.coroutines.MainScope().launch {
                            kotlinx.coroutines.delay(timeoutMs)
                            activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                    com.webtoapp.data.model.ScreenAwakeMode.OFF -> {
                        // 向后兼容：如果旧版 keepScreenOn=true 但新版 mode=OFF，仍然保持常亮
                        if (app.webViewConfig.keepScreenOn) {
                            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }
                
                // 自定义屏幕亮度
                val brightness = app.webViewConfig.screenBrightness
                if (brightness in 0..100) {
                    val lp = activity.window.attributes
                    lp.screenBrightness = brightness / 100f
                    activity.window.attributes = lp
                }
            } else {
                // app 不存在，直接标记为已激活
                isActivated = true
                isActivationChecked = true
            }
        } else {
            // appId 无效，直接标记为已激活
            isActivated = true
            isActivationChecked = true
        }
    }
    
    // 释放背景音乐播放器和停止网络监听
    DisposableEffect(Unit) {
        // Start网络监听（如果需要）
        if (webApp?.announcementEnabled == true && webApp?.announcement?.triggerOnNoNetwork == true) {
            announcement.startNetworkMonitoring()
        }
        
        onDispose {
            bgmPlayer.release()
            announcement.stopNetworkMonitoring()
        }
    }
    
    // Network状态监听 - 无网络时触发公告
    val networkAvailable by announcement.isNetworkAvailable.collectAsStateWithLifecycle()
    var lastNetworkState by remember { mutableStateOf(true) }
    
    LaunchedEffect(networkAvailable, webApp, isActivated) {
        // 当网络从有变无时触发
        if (lastNetworkState && !networkAvailable && isActivated) {
            val app = webApp
            if (app != null && app.announcementEnabled && app.announcement?.triggerOnNoNetwork == true) {
                val shouldShow = announcement.shouldShowAnnouncementForTrigger(
                    appId,
                    app.announcement,
                    isNoNetwork = true
                )
                if (shouldShow && !showAnnouncementDialog) {
                    showAnnouncementDialog = true
                }
            }
        }
        lastNetworkState = networkAvailable
    }
    
    // 定时间隔触发公告
    LaunchedEffect(webApp, isActivated) {
        val app = webApp ?: return@LaunchedEffect
        if (!isActivated) return@LaunchedEffect
        
        val intervalMinutes = app.announcement?.triggerIntervalMinutes ?: 0
        if (!app.announcementEnabled || intervalMinutes <= 0) return@LaunchedEffect
        
        // 如果配置了启动时也触发，则重置定时器
        if (app.announcement?.triggerIntervalIncludeLaunch == true) {
            announcement.resetIntervalTrigger(appId)
        }
        
        // 定时检查
        while (true) {
            delay(intervalMinutes * 60 * 1000L)
            
            if (announcement.shouldTriggerIntervalAnnouncement(appId, app.announcement)) {
                val shouldShow = announcement.shouldShowAnnouncementForTrigger(
                    appId,
                    app.announcement,
                    isInterval = true
                )
                if (shouldShow && !showAnnouncementDialog) {
                    showAnnouncementDialog = true
                    announcement.markIntervalTrigger(appId)
                }
            }
        }
    }

    // Start画面倒计时（仅用于图片类型，视频类型由播放器控制）
    LaunchedEffect(showSplash, splashCountdown) {
        // Video类型不使用倒计时，由视频播放器控制结束
        if (webApp?.splashConfig?.type == SplashType.VIDEO) return@LaunchedEffect
        
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

    WordPressPreviewCoordinator(
        webApp = webApp,
        isActivated = isActivated,
        isActivationChecked = isActivationChecked,
        retryTrigger = wpRetryTrigger,
        context = context,
        repository = repository,
        phpRuntime = phpRuntime,
        onWebAppChanged = { webApp = it },
        onStateChanged = { wordPressPreviewState = it },
        loadUrl = { url -> webViewRef?.loadUrl(url) }
    )
    
    PhpAppPreviewCoordinator(
        appId = appId,
        webApp = webApp,
        isActivated = isActivated,
        isActivationChecked = isActivationChecked,
        retryTrigger = phpAppRetryTrigger,
        context = context,
        phpAppRuntime = phpAppRuntime,
        onStateChanged = { phpAppPreviewState = it },
        loadUrl = { url -> webViewRef?.loadUrl(url) }
    )
    
    PythonAppPreviewCoordinator(
        appId = appId,
        webApp = webApp,
        isActivated = isActivated,
        isActivationChecked = isActivationChecked,
        retryTrigger = pythonAppRetryTrigger,
        pythonRuntime = pythonRuntime,
        pythonHttpServer = pythonHttpServer,
        onStateChanged = { pythonAppPreviewState = it },
        loadUrl = { url -> webViewRef?.loadUrl(url) }
    )
    
    NodeJsAppPreviewCoordinator(
        appId = appId,
        webApp = webApp,
        isActivated = isActivated,
        isActivationChecked = isActivationChecked,
        retryTrigger = nodeJsAppRetryTrigger,
        nodeRuntime = nodeRuntime,
        nodeHttpServer = nodeHttpServer,
        onStateChanged = { nodeJsAppPreviewState = it },
        loadUrl = { url -> webViewRef?.loadUrl(url) }
    )
    
    GoAppPreviewCoordinator(
        appId = appId,
        webApp = webApp,
        isActivated = isActivated,
        isActivationChecked = isActivationChecked,
        retryTrigger = goAppRetryTrigger,
        goRuntime = goRuntime,
        goHttpServer = goHttpServer,
        onStateChanged = { goAppPreviewState = it },
        loadUrl = { url -> webViewRef?.loadUrl(url) }
    )

    fun scheduleStrictHostFallbackProbe(url: String?, source: String, delayMs: Long) {
        com.webtoapp.ui.webview.scheduleStrictHostFallbackProbe(
            context = context,
            url = url,
            source = source,
            delayMs = delayMs,
            webViewProvider = { webViewRef },
            isTriggered = { strictHostFallbackTriggered },
            markTriggered = { strictHostFallbackTriggered = true }
        )
    }
    
    // WebView回调
    val webViewCallbacks = remember {
        object : WebViewCallbacks {
            override fun onPageStarted(url: String?) {
                // Skip about:blank to avoid flashing it in the toolbar during
                // WebView init and cleanup transitions
                if (url == "about:blank") return
                isLoading = true
                currentUrl = url ?: ""
                jsScrollTop.set(0)
                if (!shouldSkipLongPressEnhancer(url)) {
                    strictHostFallbackTriggered = false
                } else {
                    scheduleStrictHostFallbackProbe(url, "page_started", 5500L)
                }
            }

            override fun onPageCommitVisible(url: String?) {
                scheduleStrictHostFallbackProbe(url, "commit_visible", 2200L)
            }

            override fun onUrlChanged(webView: WebView?, url: String?) {
                // SPA navigation (pushState/replaceState) — update nav state in real time
                webView?.let {
                    canGoBack = it.canGoBack()
                    canGoForward = it.canGoForward()
                }
                if (url != null) currentUrl = url
            }

            override fun onPageFinished(url: String?) {
                if (url == "about:blank") return
                isLoading = false
                isRefreshing = false
                currentUrl = url ?: ""
                webViewRef?.let {
                    canGoBack = it.canGoBack()
                    canGoForward = it.canGoForward()
                    
                    // 注入滚动位置追踪脚本，用于下拉刷新判断
                    it.evaluateJavascript("""
                        (function(){
                            if(window._wtaScrollTrackerInstalled) return;
                            window._wtaScrollTrackerInstalled = true;
                            function report(){
                                var y = Math.round(Math.max(
                                    window.pageYOffset || 0,
                                    document.documentElement ? document.documentElement.scrollTop : 0,
                                    document.body ? document.body.scrollTop : 0
                                ));
                                try { _wtaScrollBridge.onScroll(y); } catch(e) { /* bridge unavailable */ }
                            }
                            window.addEventListener('scroll', report, {passive:true, capture:true});
                            document.addEventListener('scroll', report, {passive:true, capture:true});
                            report();
                        })();
                    """.trimIndent(), null)
                    
                    // Inject长按增强脚本（绕过小红书等网站的长按限制）
                    if (!shouldSkipLongPressEnhancer(url)) {
                        longPressHandler.injectLongPressEnhancer(it)
                    } else {
                        AppLogger.d("WebViewActivity", "Skip long-press enhancer for strict compatibility host: $url")
                    }
                }
                scheduleStrictHostFallbackProbe(url, "page_finished", 1200L)
            }

            override fun onProgressChanged(progress: Int) {
                loadProgress = progress
            }

            override fun onTitleChanged(title: String?) {
                if (title == "about:blank" || title.isNullOrBlank()) return
                pageTitle = title
            }

            override fun onIconReceived(icon: Bitmap?) {}

            override fun onError(errorCode: Int, description: String) {
                errorMessage = description
                isLoading = false
                isRefreshing = false
            }

            override fun onSslError(error: String) {
                errorMessage = context.getString(com.webtoapp.R.string.webview_ssl_error)
            }

            override fun onExternalLink(url: String) {
                try {
                    val safeUrl = normalizeExternalUrlForIntent(url)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    AppLogger.w("WebViewActivity", "No app to handle external link: $url", e)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(com.webtoapp.R.string.webview_cannot_open_link),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                view?.let { onShowCustomView(it, callback) }
            }

            override fun onHideCustomView() {
                onHideCustomView()
            }

            override fun onGeolocationPermission(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                // 通过Activity请求Android位置权限
                (activity as? WebViewActivity)?.handleGeolocationPermission(origin, callback)
                    ?: callback?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // 通过Activity请求Android系统权限（摄像头、麦克风等）
                request?.let { req ->
                    (activity as? WebViewActivity)?.handlePermissionRequest(req)
                        ?: req.grant(req.resources)
                }
            }

            override fun onShowFileChooser(
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                return onFileChooser(filePathCallback, fileChooserParams)
            }
            
            override fun onDownloadStart(
                url: String,
                userAgent: String,
                contentDisposition: String,
                mimeType: String,
                contentLength: Long
            ) {
                handleWebViewDownload(
                    context = context,
                    scope = scope,
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    webViewProvider = { webViewRef }
                )
            }
            
            override fun onLongPress(webView: WebView, x: Float, y: Float): Boolean {
                // 无论长按菜单是否启用，都先检查是否长按了链接
                // 如果是链接，始终拦截以隐藏系统默认的链接预览弹窗
                val hitResult = webView.hitTestResult
                val hitType = hitResult.type
                val isLink = hitType == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                             hitType == WebView.HitTestResult.ANCHOR_TYPE
                
                // Check长按菜单是否启用
                val menuEnabled = webApp?.webViewConfig?.longPressMenuEnabled ?: true
                if (!menuEnabled) {
                    return isLink // 链接长按始终拦截以隐藏预览弹窗
                }
                
                // If it is编辑框或未知类型，不拦截，让 WebView 处理默认的文字选择
                if (hitType == WebView.HitTestResult.EDIT_TEXT_TYPE ||
                    hitType == WebView.HitTestResult.UNKNOWN_TYPE) {
                    return false
                }
                
                // 通过 JS 获取长按元素详情
                longPressHandler.getLongPressDetails(webView, x, y) { result ->
                    when (result) {
                        is LongPressHandler.LongPressResult.Image,
                        is LongPressHandler.LongPressResult.Video,
                        is LongPressHandler.LongPressResult.Link,
                        is LongPressHandler.LongPressResult.ImageLink -> {
                            longPressResult = result
                            longPressTouchX = x
                            longPressTouchY = y
                            showLongPressMenu = true
                        }
                        is LongPressHandler.LongPressResult.Text,
                        is LongPressHandler.LongPressResult.None -> {
                            // 文字或空白区域，不显示菜单
                            // 注意：由于已经返回 true 拦截了事件，这里无法触发默认选择
                            // 但对于图片/视频/链接场景，这是正确的行为
                        }
                    }
                }
                
                // 对于图片、链接等类型，拦截事件显示自定义菜单
                return when (hitType) {
                    WebView.HitTestResult.IMAGE_TYPE,
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                    WebView.HitTestResult.SRC_ANCHOR_TYPE,
                    WebView.HitTestResult.ANCHOR_TYPE -> true
                    else -> false  // 其他情况不拦截，允许默认的文字选择
                }
            }
            
            override fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {
                val consoleLevel = when (level) {
                    0 -> ConsoleLevel.DEBUG
                    1 -> ConsoleLevel.LOG
                    2 -> ConsoleLevel.INFO
                    3 -> ConsoleLevel.WARNING
                    4 -> ConsoleLevel.ERROR
                    else -> ConsoleLevel.LOG
                }
                consoleMessages = consoleMessages + ConsoleLogEntry(
                    level = consoleLevel,
                    message = message,
                    source = sourceId,
                    lineNumber = lineNumber,
                    timestamp = System.currentTimeMillis()
                )
            }
            
            override fun onRenderProcessGone(didCrash: Boolean) {
                AppLogger.w("WebViewActivity", "Render process gone (crash=$didCrash), triggering WebView recreation")
                webViewRef = null
                errorMessage = null
                // 自增 key 强制 AndroidView 重新创建并加载 URL
                webViewRecreationKey++
            }
        }
    }

    val webViewManager = remember { WebViewManager(context, adBlocker) }
    
    // Local HTTP 服务器
    val localHttpServer = remember { LocalHttpServer.getInstance(context) }
    
    // 根据应用类型构建目标 URL
    val targetUrl = remember(directUrl, webApp, testUrl) {
        val app = webApp  // 捕获到局部变量以支持智能转换
        when {
            // 测试模式优先
            !testUrl.isNullOrBlank() -> normalizeWebUrlForSecurity(testUrl)
            !directUrl.isNullOrBlank() -> normalizeWebUrlForSecurity(directUrl)
            app?.appType == com.webtoapp.data.model.AppType.WORDPRESS -> {
                // WordPress 应用：先显示 about:blank，PHP 服务器就绪后通过 LaunchedEffect 动态加载
                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.PHP_APP -> {
                // PHP 应用：先显示 about:blank，PHP 服务器就绪后通过 LaunchedEffect 动态加载
                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.PYTHON_APP -> {
                // Python 应用：先显示 about:blank，服务器就绪后通过 LaunchedEffect 动态加载
                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.NODEJS_APP -> {
                // Node.js 应用：先显示 about:blank，服务器就绪后通过 LaunchedEffect 动态加载
                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.GO_APP -> {
                // Go 应用：先显示 about:blank，服务器就绪后通过 LaunchedEffect 动态加载
                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.MULTI_WEB -> {
                // 多站点聚合：加载第一个站点的 URL
                app.multiWebConfig?.sites?.firstOrNull { it.enabled && it.url.isNotBlank() }?.url ?: "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.HTML ||
            app?.appType == com.webtoapp.data.model.AppType.FRONTEND -> {
                // HTML/FRONTEND 应用：启动本地 HTTP 服务器
                val projectId = app.htmlConfig?.projectId ?: ""
                val entryFile = app.htmlConfig?.getValidEntryFile() ?: "index.html"
                val htmlDir = File(context.filesDir, "html_projects/$projectId")
                
                // 调试日志
                AppLogger.d("WebViewActivity", "========== HTML App Debug Info ==========")
                AppLogger.d("WebViewActivity", "projectId: '$projectId'")
                AppLogger.d("WebViewActivity", "entryFile: '$entryFile'")
                AppLogger.d("WebViewActivity", "htmlDir: ${htmlDir.absolutePath}")
                AppLogger.d("WebViewActivity", "htmlDir.exists(): ${htmlDir.exists()}")
                AppLogger.d("WebViewActivity", "htmlConfig: ${app.htmlConfig}")
                AppLogger.d("WebViewActivity", "htmlConfig.files: ${app.htmlConfig?.files}")
                
                // 列出目录内容
                if (htmlDir.exists()) {
                    val files = htmlDir.listFiles()
                    AppLogger.d("WebViewActivity", "目录文件列表 (${files?.size ?: 0} 个):")
                    files?.forEach { file ->
                        AppLogger.d("WebViewActivity", "  - ${file.name} (${file.length()} bytes)")
                    }
                    
                    // 检查入口文件是否存在
                    val entryFilePath = File(htmlDir, entryFile)
                    AppLogger.d("WebViewActivity", "入口文件路径: ${entryFilePath.absolutePath}")
                    AppLogger.d("WebViewActivity", "入口文件存在: ${entryFilePath.exists()}")
                }
                AppLogger.d("WebViewActivity", "=========================================")
                
                if (htmlDir.exists()) {
                    try {
                        // Start本地服务器并获取 URL
                        val baseUrl = localHttpServer.start(htmlDir)
                        val targetUrl = "$baseUrl/$entryFile"
                        AppLogger.d("WebViewActivity", "目标 URL: $targetUrl")
                        targetUrl
                    } catch (e: Exception) {
                        AppLogger.e("WebViewActivity", "启动本地服务器失败", e)
                        // 降级到 file:// 协议
                        "file://${htmlDir.absolutePath}/$entryFile"
                    }
                } else {
                    AppLogger.w("WebViewActivity", "HTML项目目录不存在: ${htmlDir.absolutePath}")
                    ""
                }
            }
            else -> normalizeWebUrlForSecurity(app?.url)
        }
    }
    
    // Cleanup：停止本地服务器
    DisposableEffect(Unit) {
        onDispose {
            // 注意：不在这里停止服务器，因为可能有多个 WebView 使用
            // localHttpServer.stop()
        }
    }
    
    // Yes否隐藏工具栏（全屏模式）- 测试模式下始终显示工具栏
    val hideToolbar = !isTestMode && webApp?.webViewConfig?.hideToolbar == true
    val hideBrowserToolbar = !isTestMode && webApp?.webViewConfig?.hideBrowserToolbar == true
    // 是否在全屏模式下显示顶部导航栏
    val showToolbarInPreview = when {
        isTestMode -> true
        hideBrowserToolbar -> false
        hideToolbar -> webApp?.webViewConfig?.showToolbarInFullscreen == true
        else -> true
    }
    
    LaunchedEffect(hideToolbar) {
        onFullscreenModeChanged(hideToolbar)
    }

    // 外层 Box 用于放置状态栏覆盖层（需要在 Scaffold 外部才能正确覆盖状态栏区域）
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        // 在沉浸式模式下，不添加任何内边距（除非显示toolbar）
        contentWindowInsets = if (hideToolbar && !showToolbarInPreview) WindowInsets(0) else if (hideToolbar && showToolbarInPreview) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        modifier = if (hideToolbar && !showToolbarInPreview) Modifier.fillMaxSize() else if (hideToolbar) Modifier.fillMaxSize() else Modifier,
        topBar = {
            if (showToolbarInPreview) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isTestMode) "模块测试" else pageTitle.ifEmpty { webApp?.name ?: "WebApp" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isTestMode && !testModuleIds.isNullOrEmpty()) {
                                Text(
                                    text = Strings.testingModules.format(testModuleIds.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            } else if (currentUrl.isNotEmpty()) {
                                Text(
                                    text = currentUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            IconButton(onClick = {
                                webViewRef?.let { wv ->
                                    val list = wv.copyBackForwardList()
                                    val prev = list.getItemAtIndex(list.currentIndex - 1)?.url
                                    if (prev == "about:blank") {
                                        (context as? AppCompatActivity)?.finish()
                                    } else {
                                        wv.goBack()
                                    }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        } else {
                            IconButton(onClick = { (context as? AppCompatActivity)?.finish() }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                    },
                    actions = {
                        // 控制台按钮
                        IconButton(
                            onClick = { showConsole = !showConsole },
                            modifier = Modifier.size(48.dp).offset(x = (-4).dp)
                        ) {
                            Box(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                                BadgedBox(
                                    badge = {
                                        val errorCount = consoleMessages.count { it.level == ConsoleLevel.ERROR }
                                        if (errorCount > 0) {
                                            Badge { Text("$errorCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (showConsole) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                                        Strings.console
                                    )
                                }
                            }
                        }
                        // 三点菜单（后退、前进、刷新）
                        Box {
                            var showToolbarMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showToolbarMenu = true }) {
                                Icon(Icons.Default.MoreVert, "更多")
                            }
                            DropdownMenu(
                                expanded = showToolbarMenu,
                                onDismissRequest = { showToolbarMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(Strings.goBack) },
                                    onClick = {
                                        showToolbarMenu = false
                                        webViewRef?.let { wv ->
                                            val list = wv.copyBackForwardList()
                                            val prev = list.getItemAtIndex(list.currentIndex - 1)?.url
                                            if (prev == "about:blank") {
                                                (context as? AppCompatActivity)?.finish()
                                            } else {
                                                wv.goBack()
                                            }
                                        }
                                    },
                                    enabled = canGoBack,
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(Strings.goForward) },
                                    onClick = {
                                        showToolbarMenu = false
                                        webViewRef?.goForward()
                                    },
                                    enabled = canGoForward,
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(Strings.refresh) },
                                    onClick = {
                                        showToolbarMenu = false
                                        webViewRef?.reload()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { padding ->
        // 计算内容的 padding
        // Fullscreen模式 + 显示状态栏时，需要给内容添加状态栏高度的 padding，避免被遮挡
        val density = LocalDensity.current

        val topInsetPx = WindowInsets.statusBars.getTop(density)
        val systemStatusBarHeightDp = if (topInsetPx > 0) {
            with(density) { topInsetPx.toDp() }
        } else {
            24.dp
        }
        
        // 计算实际需要的状态栏 padding（使用自定义高度或系统默认高度）
        val actualStatusBarPadding = if (statusBarHeightDp > 0) statusBarHeightDp.dp else systemStatusBarHeightDp
        
        val contentModifier = when {
            hideToolbar && showToolbarInPreview -> {
                // Fullscreen模式但显示toolbar：使用 Scaffold 的 padding（toolbar高度）
                Modifier.fillMaxSize().padding(padding)
            }
            hideToolbar && webApp?.webViewConfig?.showStatusBarInFullscreen == true -> {
                // Fullscreen模式但显示状态栏：内容需要在状态栏下方
                // 使用自定义高度或系统默认高度作为顶部 padding
                Modifier.fillMaxSize().padding(top = actualStatusBarPadding)
            }
            hideToolbar -> {
                // 完全全屏模式：内容铺满整个屏幕
                Modifier.fillMaxSize()
            }
            else -> {
                // 非全屏模式：使用 Scaffold 的 padding
                Modifier.fillMaxSize().padding(padding)
            }
        }
        
        Box(modifier = contentModifier) {
            // 进度条
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Activation检查中，显示加载状态
            if (!isActivationChecked && webApp?.activationEnabled == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // 未激活提示
            else if (!isActivated && webApp?.activationEnabled == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(Strings.pleaseActivateApp)
                        Spacer(modifier = Modifier.height(16.dp))
                        PremiumButton(onClick = { showActivationDialog = true }) {
                            Text(Strings.enterActivationCode)
                        }
                    }
                }
            } else if (targetUrl.isNotEmpty() && isActivationChecked) {
                // 使用 key 包裹：当渲染进程被杀死后 webViewRecreationKey 自增，
                // 强制整个 WebView 子树重建，自动重新加载页面
                key(webViewRecreationKey) {
                // 控制台展开状态
                var isConsoleExpanded by remember { mutableStateOf(false) }
                val swipeRefreshEnabled = webApp?.webViewConfig?.swipeRefreshEnabled != false
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // WebView
                    AndroidView(
                        factory = { ctx ->
                            EdgeSwipeRefreshLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setColorSchemeColors(
                                    android.graphics.Color.parseColor("#6750A4"),
                                    android.graphics.Color.parseColor("#7F67BE")
                                )
                                isEnabled = swipeRefreshEnabled
                                setOnRefreshListener {
                                    isRefreshing = true
                                    webViewRef?.reload()
                                }

                                var swipeChildWebView: WebView? = null
                                setOnChildScrollUpCallback { _, _ ->
                                    val wv = swipeChildWebView ?: return@setOnChildScrollUpCallback false
                                    wv.scrollY > 0 || jsScrollTop.get() > 0
                                }

                                val createdWebView = WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    // 测试模式使用测试模块ID，否则使用应用配置的模块ID
                                    val moduleIds = if (isTestMode && !testModuleIds.isNullOrEmpty()) {
                                        testModuleIds
                                    } else {
                                        webApp?.extensionModuleIds ?: emptyList()
                                    }
                                    webViewManager.configureWebView(
                                        this,
                                        webApp?.webViewConfig ?: com.webtoapp.data.model.WebViewConfig(),
                                        webViewCallbacks,
                                        moduleIds,
                                        emptyList(), // embeddedExtensionModules
                                        webApp?.extensionFabIcon.orEmpty(),
                                        allowGlobalModuleFallback = false,
                                        deviceDisguiseConfig = webApp?.deviceDisguiseConfig
                                    )
                                    // HTML 应用需要额外配置以支持本地文件访问
                                    val currentApp = webApp
                                    if (currentApp?.appType == com.webtoapp.data.model.AppType.HTML) {
                                        settings.apply {
                                            allowFileAccess = true
                                            allowContentAccess = true
                                            @Suppress("DEPRECATION")
                                            allowFileAccessFromFileURLs = true
                                            @Suppress("DEPRECATION")
                                            allowUniversalAccessFromFileURLs = true
                                            javaScriptEnabled = currentApp.htmlConfig?.enableJavaScript ?: true
                                            domStorageEnabled = currentApp.htmlConfig?.enableLocalStorage ?: true
                                        }
                                    }

                                    // 添加长按监听器
                                    // 持续跟踪触摸位置，确保长按时使用最新坐标
                                    var lastTouchX = 0f
                                    var lastTouchY = 0f
                                    setOnTouchListener { view, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN,
                                            MotionEvent.ACTION_MOVE -> {
                                                lastTouchX = event.x
                                                lastTouchY = event.y
                                            }
                                            MotionEvent.ACTION_UP -> view.performClick()
                                        }
                                        false // 不消费事件，让 WebView 继续处理（包括JavaScript点击事件）
                                    }
                                    setOnLongClickListener {
                                        webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                                    }

                                    addJavascriptInterface(scrollBridge, "_wtaScrollBridge")
                                    onWebViewCreated(this)

                                    if (shouldSkipLongPressEnhancer(targetUrl)) {
                                        webViewManager.applyPreloadPolicyForUrl(this, targetUrl)
                                        AppLogger.d("WebViewActivity", "Strict host pre-load policy applied for $targetUrl")
                                    }

                                    webViewRef = this

                                    // Load目标 URL
                                    // HTML 应用通过 LocalHttpServer 提供 http://localhost:PORT 的 URL
                                    // 这样可以正常加载外部 CDN 资源
                                    loadUrl(targetUrl)
                                }

                                swipeChildWebView = createdWebView
                                addView(createdWebView)
                            }
                        },
                        update = { swipeLayout ->
                            swipeLayout.isEnabled = swipeRefreshEnabled
                            if (swipeLayout.isRefreshing != isRefreshing) {
                                swipeLayout.isRefreshing = isRefreshing
                            }
                        },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    
                    // 控制台面板
                    AnimatedVisibility(
                        visible = showConsole,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        ConsolePanel(
                            consoleMessages = consoleMessages,
                            isExpanded = isConsoleExpanded,
                            onExpandToggle = { isConsoleExpanded = !isConsoleExpanded },
                            onClear = { consoleMessages = emptyList() },
                            onRunScript = { script ->
                                webViewRef?.evaluateJavascript(script) { result ->
                                    consoleMessages = consoleMessages + ConsoleLogEntry(
                                        level = ConsoleLevel.LOG,
                                        message = "=> $result",
                                        source = "eval",
                                        lineNumber = 0,
                                        timestamp = System.currentTimeMillis()
                                    )
                                }
                            },
                            onClose = { showConsole = false }
                        )
                    }
                }
                } // key(webViewRecreationKey)
            } else if (webApp == null) {
                // webApp 尚未从数据库加载完成，显示加载指示器
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // WordPress 加载覆盖层
            val isWordPressLoading = webApp?.appType == com.webtoapp.data.model.AppType.WORDPRESS &&
                wordPressPreviewState !is WordPressPreviewState.Ready &&
                wordPressPreviewState !is WordPressPreviewState.Idle
            if (isWordPressLoading) {
                WordPressLoadingOverlay(
                    state = wordPressPreviewState,
                    downloadState = wpDownloadState,
                    onRetry = { wpRetryTrigger++ }
                )
            }

            // PHP 应用加载覆盖层
            val isPhpAppLoading = webApp?.appType == com.webtoapp.data.model.AppType.PHP_APP &&
                phpAppPreviewState !is PhpAppPreviewState.Ready &&
                phpAppPreviewState !is PhpAppPreviewState.Idle
            if (isPhpAppLoading) {
                PhpAppLoadingOverlay(
                    state = phpAppPreviewState,
                    downloadState = phpAppDownloadState,
                    onRetry = { phpAppRetryTrigger++ }
                )
            }

            // Python 应用加载覆盖层
            val isPythonAppLoading = webApp?.appType == com.webtoapp.data.model.AppType.PYTHON_APP &&
                pythonAppPreviewState !is PythonAppPreviewState.Ready &&
                pythonAppPreviewState !is PythonAppPreviewState.Idle
            if (isPythonAppLoading) {
                PythonAppLoadingOverlay(
                    state = pythonAppPreviewState,
                    onRetry = { pythonAppRetryTrigger++ }
                )
            }

            // Go 应用加载覆盖层
            val isGoAppLoading = webApp?.appType == com.webtoapp.data.model.AppType.GO_APP &&
                goAppPreviewState !is GoAppPreviewState.Ready &&
                goAppPreviewState !is GoAppPreviewState.Idle
            if (isGoAppLoading) {
                SimpleAppLoadingOverlay(
                    isStarting = goAppPreviewState is GoAppPreviewState.Starting || goAppPreviewState is GoAppPreviewState.StartingServer,
                    startingText = Strings.goStartingPreview,
                    errorMessage = (goAppPreviewState as? GoAppPreviewState.Error)?.message,
                    onRetry = { goAppRetryTrigger++ }
                )
            }

            // 全屏模式下的悬浮返回按钮（当工具栏隐藏且可以后退时显示）
            // 如果用户选择了显示toolbar则不需要悬浮按钮
            // 自动淡出：显示后 3 秒开始淡化，点击时重置透明度
            if ((hideToolbar || hideBrowserToolbar) && !showToolbarInPreview && canGoBack) {
                var fabAlpha by remember { mutableFloatStateOf(0.9f) }
                var fadeKey by remember { mutableIntStateOf(0) }
                
                LaunchedEffect(canGoBack, fadeKey) {
                    fabAlpha = 0.9f
                    delay(3000L)
                    // 渐变淡出到 0.25
                    val steps = 20
                    val stepDelay = 30L
                    for (i in 1..steps) {
                        fabAlpha = 0.9f - (0.65f * i / steps)
                        delay(stepDelay)
                    }
                }
                
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = {
                        fadeKey++ // 重置淡出计时
                        webViewRef?.let { wv ->
                            val list = wv.copyBackForwardList()
                            val prev = list.getItemAtIndex(list.currentIndex - 1)?.url
                            if (prev == "about:blank") {
                                (context as? AppCompatActivity)?.finish()
                            } else {
                                wv.goBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = actualStatusBarPadding + 8.dp)
                        .graphicsLayer { alpha = fabAlpha },
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.cdBack)
                }
            }

            // Error提示
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(error, modifier = Modifier.weight(weight = 1f, fill = true))
                        TextButton(onClick = { errorMessage = null }) {
                            Text(Strings.close)
                        }
                    }
                }
            }
            
            // 注意：状态栏覆盖层已移到 Scaffold 外部
        }
    }
    
    // Status bar背景覆盖层（根据深色/浅色模式选择对应配置）
    // Show overlay when: fullscreen with status bar visible, OR non-fullscreen with custom status bar config
    val isDarkThemeForOverlay = com.webtoapp.ui.theme.LocalIsDarkTheme.current
    val effectiveStatusBarBgType = if (isDarkThemeForOverlay) statusBarBackgroundTypeDarkLocal else statusBarBackgroundType
    val effectiveStatusBarBgColor = if (isDarkThemeForOverlay) statusBarBackgroundColorDark else statusBarBackgroundColor
    val effectiveStatusBarBgImage = if (isDarkThemeForOverlay) statusBarBackgroundImageDark else statusBarBackgroundImage
    val effectiveStatusBarBgAlpha = if (isDarkThemeForOverlay) statusBarBackgroundAlphaDark else statusBarBackgroundAlpha
    val hasCustomStatusBar = effectiveStatusBarBgType != "COLOR" || effectiveStatusBarBgColor != null || statusBarHeightDp > 0
    val showStatusBarOverlay = (hideToolbar && webApp?.webViewConfig?.showStatusBarInFullscreen == true) || (!hideToolbar && hasCustomStatusBar)
    if (showStatusBarOverlay) {
        // Force status bar icon color to match overlay background on every recomposition
        val isLightOverlayBackground = remember(effectiveStatusBarBgColor) {
            if (effectiveStatusBarBgColor != null) {
                try {
                    val color = android.graphics.Color.parseColor(
                        if (effectiveStatusBarBgColor!!.startsWith("#")) effectiveStatusBarBgColor else "#$effectiveStatusBarBgColor"
                    )
                    com.webtoapp.ui.shared.WindowHelper.isColorLight(color)
                } catch (e: Exception) { false }
            } else false
        }
        SideEffect {
            val activity = context as? android.app.Activity ?: return@SideEffect
            val controller = androidx.core.view.WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            controller.isAppearanceLightStatusBars = isLightOverlayBackground
        }
        com.webtoapp.ui.components.StatusBarOverlay(
            show = true,
            backgroundType = effectiveStatusBarBgType,
            backgroundColor = effectiveStatusBarBgColor,
            backgroundImagePath = effectiveStatusBarBgImage,
            alpha = effectiveStatusBarBgAlpha,
            heightDp = statusBarHeightDp,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
    } // 关闭外层 Box

    // Activation码对话框
    if (showActivationDialog) {
        val activationStatus by androidx.compose.runtime.produceState<com.webtoapp.core.activation.ActivationStatus?>(initialValue = null) {
            value = try {
                activation.getActivationStatus(appId)
            } catch (e: Exception) {
                null
            }
        }
        
        com.webtoapp.ui.components.EnhancedActivationDialog(
            onDismiss = { showActivationDialog = false },
            onActivate = { code ->
                val allCodes = webApp?.activationCodeList ?: emptyList()
                // 同时包含旧格式 activationCodes 中的遗留数据
                val legacyCodes = webApp?.activationCodes
                    ?.filter { !it.trimStart().startsWith("{") }
                    ?.map { com.webtoapp.core.activation.ActivationCode.fromLegacyString(it) }
                    ?: emptyList()
                val combinedCodes = allCodes + legacyCodes
                return@EnhancedActivationDialog activation.verifyActivationCodeWithObjects(appId, code, combinedCodes)
            },
            activationStatus = activationStatus,
            customTitle = webApp?.activationDialogConfig?.title ?: "",
            customSubtitle = webApp?.activationDialogConfig?.subtitle ?: "",
            customInputLabel = webApp?.activationDialogConfig?.inputLabel ?: "",
            customButtonText = webApp?.activationDialogConfig?.buttonText ?: ""
        )
        
        // Listen激活状态变化
        LaunchedEffect(Unit) {
            activation.isActivated(appId).collect { activated ->
                if (activated) {
                    isActivated = true
                    showActivationDialog = false
                    // Check公告
                    if (webApp?.announcementEnabled == true) {
                        val shouldShow = announcement.shouldShowAnnouncement(appId, webApp?.announcement)
                        showAnnouncementDialog = shouldShow
                    }
                }
            }
        }
    }

    // Announcement对话框 - 使用模板系统
    if (showAnnouncementDialog && webApp?.announcement != null) {
        val ann = webApp!!.announcement!!
com.webtoapp.ui.components.announcement.AnnouncementDialog(
            config = com.webtoapp.ui.components.announcement.AnnouncementConfig(
                announcement = ann,
                template = com.webtoapp.ui.components.announcement.AnnouncementTemplate.valueOf(
                    ann.template.name
                ),
                showEmoji = ann.showEmoji,
                animationEnabled = ann.animationEnabled
            ),
            onDismiss = {
                showAnnouncementDialog = false
                val scope = (context as? AppCompatActivity)?.lifecycleScope
                scope?.launch {
                    announcement.markAnnouncementShown(appId, ann.version)
                }
            },
            onLinkClick = { url ->
                try {
                    val safeUrl = normalizeExternalUrlForIntent(url)
                    if (safeUrl.isBlank()) {
                        Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            },
            onNeverShowChecked = { checked ->
                if (checked) {
                    val scope = (context as? AppCompatActivity)?.lifecycleScope
                    scope?.launch {
                        announcement.markNeverShow(appId)
                    }
                }
            }
        )
    }

    // 关闭启动画面的回调
    val closeSplash = {
        showSplash = false
        // 恢复原始方向
        if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity.requestedOrientation = originalOrientation
            originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    // Start画面覆盖层
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        webApp?.splashConfig?.let { splashConfig ->
            SplashOverlay(
                splashConfig = splashConfig,
                countdown = splashCountdown,
                // 点击跳过（仅当启用时）
                onSkip = if (splashConfig.clickToSkip) { closeSplash } else null,
                // Play完成回调（始终需要）
                onComplete = closeSplash
            )
        }
    }
    
    // 长按菜单
    if (showLongPressMenu && longPressResult != null) {
        WebViewLongPressMenu(
            menuStyle = webApp?.webViewConfig?.longPressMenuStyle ?: LongPressMenuStyle.FULL,
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
}
