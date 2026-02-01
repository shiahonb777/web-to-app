package com.webtoapp.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.activation.ActivationResult
import com.webtoapp.core.bgm.BgmPlayer
import com.webtoapp.core.webview.LocalHttpServer
import com.webtoapp.core.webview.LongPressHandler
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.core.webview.WebViewManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.SplashConfig
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.data.model.SplashType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.getActivationCodeStrings
import com.webtoapp.ui.components.LongPressMenuSheet
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.util.DownloadHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * WebView容器Activity - 用于预览和运行WebApp
 */
class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_APP_ID = "app_id"
        private const val EXTRA_URL = "url"
        private const val EXTRA_TEST_URL = "test_url"
        private const val EXTRA_TEST_MODULE_IDS = "test_module_ids"

        fun start(context: Context, appId: Long) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_APP_ID, appId)
            })
        }

        fun startWithUrl(context: Context, url: String) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            })
        }

        /**
         * 启动测试模式 - 用于测试扩展模块
         */
        fun startForTest(context: Context, testUrl: String, moduleIds: List<String>) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_TEST_URL, testUrl)
                putStringArrayListExtra(EXTRA_TEST_MODULE_IDS, ArrayList(moduleIds))
            })
        }
    }

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // 权限请求相关
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null

    private var immersiveFullscreenEnabled: Boolean = false
    private var showStatusBarInFullscreen: Boolean = false  // 全屏模式下是否显示状态栏

    // 视频全屏前的屏幕方向
    private var originalOrientationBeforeFullscreen: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    // 状态栏配置缓存
    private var statusBarColorMode: com.webtoapp.data.model.StatusBarColorMode = com.webtoapp.data.model.StatusBarColorMode.THEME
    private var statusBarCustomColor: String? = null
    private var statusBarDarkIcons: Boolean? = null
    private var statusBarBackgroundType: com.webtoapp.data.model.StatusBarBackgroundType = com.webtoapp.data.model.StatusBarBackgroundType.COLOR

    /**
     * 应用状态栏颜色配置
     *
     * @param colorMode 颜色模式
     * @param customColor 自定义颜色（仅 CUSTOM 模式生效）
     * @param darkIcons 图标颜色：true=深色图标，false=浅色图标，null=自动
     * @param isDarkTheme 当前是否为深色主题
     */
    private fun applyStatusBarColor(
        colorMode: com.webtoapp.data.model.StatusBarColorMode,
        customColor: String?,
        darkIcons: Boolean?,
        isDarkTheme: Boolean
    ) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        when (colorMode) {
            com.webtoapp.data.model.StatusBarColorMode.TRANSPARENT -> {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                // 自动判断图标颜色
                val useDarkIcons = darkIcons ?: !isDarkTheme
                controller.isAppearanceLightStatusBars = useDarkIcons
            }
            com.webtoapp.data.model.StatusBarColorMode.CUSTOM -> {
                // 自定义颜色
                val color = try {
                    android.graphics.Color.parseColor(customColor ?: "#FFFFFF")
                } catch (e: Exception) {
                    android.graphics.Color.WHITE
                }
                window.statusBarColor = color

                // 根据颜色亮度自动判断图标颜色，或使用用户指定的
                val useDarkIcons = darkIcons ?: isColorLight(color)
                controller.isAppearanceLightStatusBars = useDarkIcons
            }
            else -> {
                // THEME 模式：跟随主题
                if (isDarkTheme) {
                    window.statusBarColor = android.graphics.Color.parseColor("#1C1B1F")
                    controller.isAppearanceLightStatusBars = false
                } else {
                    window.statusBarColor = android.graphics.Color.parseColor("#FFFBFE")
                    controller.isAppearanceLightStatusBars = true
                }
            }
        }

        controller.isAppearanceLightNavigationBars = controller.isAppearanceLightStatusBars
    }

    /**
     * 判断颜色是否为浅色
     */
    private fun isColorLight(color: Int): Boolean {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance > 0.5
    }

    /**
     * 应用沉浸式全屏模式
     *
     * @param enabled 是否启用沉浸式模式
     * @param hideNavBar 是否同时隐藏导航栏（视频全屏时为 true）
     * @param isDarkTheme 当前是否为深色主题
     */
    private fun applyImmersiveFullscreen(enabled: Boolean, hideNavBar: Boolean = true, isDarkTheme: Boolean = false) {
        try {
            // 支持刘海屏/挖孔屏
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                if (enabled) {
                    // 沉浸式模式
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT

                    // 根据配置决定是否显示状态栏
                    if (showStatusBarInFullscreen) {
                        // 全屏模式但显示状态栏：内容延伸到状态栏区域
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        controller.show(WindowInsetsCompat.Type.statusBars())

                        // 如果是图片背景，状态栏设为透明，让 StatusBarOverlay 组件显示图片
                        if (statusBarBackgroundType == com.webtoapp.data.model.StatusBarBackgroundType.IMAGE) {
                            window.statusBarColor = android.graphics.Color.TRANSPARENT
                            val useDarkIcons = statusBarDarkIcons ?: !isDarkTheme
                            controller.isAppearanceLightStatusBars = useDarkIcons
                        } else {
                            // 纯色背景：直接设置系统状态栏颜色
                            when (statusBarColorMode) {
                                com.webtoapp.data.model.StatusBarColorMode.CUSTOM -> {
                                    val color = try {
                                        android.graphics.Color.parseColor(statusBarCustomColor ?: "#000000")
                                    } catch (e: Exception) {
                                        android.graphics.Color.BLACK
                                    }
                                    window.statusBarColor = color
                                    val useDarkIcons = statusBarDarkIcons ?: isColorLight(color)
                                    controller.isAppearanceLightStatusBars = useDarkIcons
                                }
                                com.webtoapp.data.model.StatusBarColorMode.TRANSPARENT -> {
                                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                                    val useDarkIcons = statusBarDarkIcons ?: !isDarkTheme
                                    controller.isAppearanceLightStatusBars = useDarkIcons
                                }
                                else -> {
                                    // THEME 模式：跟随主题
                                    if (isDarkTheme) {
                                        window.statusBarColor = android.graphics.Color.parseColor("#1C1B1F")
                                        controller.isAppearanceLightStatusBars = false
                                    } else {
                                        window.statusBarColor = android.graphics.Color.parseColor("#FFFBFE")
                                        controller.isAppearanceLightStatusBars = true
                                    }
                                }
                            }
                        }
                    } else {
                        // 完全沉浸式：隐藏状态栏
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                    }

                    if (hideNavBar) {
                        controller.hide(WindowInsetsCompat.Type.navigationBars())
                    }
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    // 非沉浸式模式：显示系统栏，应用状态栏颜色配置
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT

                    applyStatusBarColor(statusBarColorMode, statusBarCustomColor, statusBarDarkIcons, isDarkTheme)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("WebViewActivity", "applyImmersiveFullscreen failed", e)
        }
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    // 权限请求launcher（用于摄像头、麦克风等）
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        pendingPermissionRequest?.let { request ->
            if (allGranted) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
            pendingPermissionRequest = null
        }
    }

    // 位置权限请求launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        pendingGeolocationCallback?.invoke(pendingGeolocationOrigin, granted, false)
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
    }

    // 通知权限请求launcher（Android 13+）
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.d("WebViewActivity", "Notification permission granted")
        } else {
            android.util.Log.d("WebViewActivity", "Notification permission denied")
        }
    }

    /**
     * 请求通知权限（Android 13+）
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 处理WebView权限请求，先请求Android系统权限
     */
    fun handlePermissionRequest(request: PermissionRequest) {
        val resources = request.resources
        val androidPermissions = mutableListOf<String>()

        resources.forEach { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    androidPermissions.add(android.Manifest.permission.CAMERA)
                }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    androidPermissions.add(android.Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        if (androidPermissions.isEmpty()) {
            // 不需要Android权限，直接授权WebView
            request.grant(resources)
        } else {
            // 需要先请求Android权限
            pendingPermissionRequest = request
            permissionLauncher.launch(androidPermissions.toTypedArray())
        }
    }

    /**
     * 处理地理位置权限请求
     */
    fun handleGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?) {
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        locationPermissionLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启用边到边显示（让内容延伸到系统栏区域）
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            android.util.Log.w("WebViewActivity", "enableEdgeToEdge failed", e)
        }

        super.onCreate(savedInstanceState)

        // 请求通知权限（Android 13+），用于显示下载进度和完成通知
        requestNotificationPermissionIfNeeded()

        // 初始化时不启用沉浸式模式，等待 WebApp 配置加载后再根据 hideToolbar 决定
        // 这样可以确保非全屏模式下状态栏正常显示
        immersiveFullscreenEnabled = false
        applyImmersiveFullscreen(immersiveFullscreenEnabled)

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1)
        val directUrl = intent.getStringExtra(EXTRA_URL)

        // 测试模式参数
        val testUrl = intent.getStringExtra(EXTRA_TEST_URL)
        val testModuleIds = intent.getStringArrayListExtra(EXTRA_TEST_MODULE_IDS)

        setContent {
            WebToAppTheme { isDarkTheme ->
                // 当主题变化时更新状态栏颜色
                LaunchedEffect(isDarkTheme, statusBarColorMode) {
                    if (!immersiveFullscreenEnabled) {
                        applyStatusBarColor(statusBarColorMode, statusBarCustomColor, statusBarDarkIcons, isDarkTheme)
                    }
                }

                WebViewScreen(
                    appId = appId,
                    directUrl = directUrl,
                    testUrl = testUrl,
                    testModuleIds = testModuleIds,
                    onStatusBarConfigChanged = { colorMode, customColor, darkIcons, showStatusBar, backgroundType ->
                        // 更新状态栏配置
                        statusBarColorMode = colorMode
                        statusBarCustomColor = customColor
                        statusBarDarkIcons = darkIcons
                        showStatusBarInFullscreen = showStatusBar
                        statusBarBackgroundType = backgroundType
                    },
                    onWebViewCreated = { wv ->
                        webView = wv
                        // 添加下载桥接（支持 Blob/Data URL 下载）
                        val downloadBridge = com.webtoapp.core.webview.DownloadBridge(this@WebViewActivity, lifecycleScope)
                        wv.addJavascriptInterface(downloadBridge, com.webtoapp.core.webview.DownloadBridge.JS_INTERFACE_NAME)
                        // 添加原生能力桥接（供扩展模块调用）
                        val nativeBridge = com.webtoapp.core.webview.NativeBridge(this@WebViewActivity, lifecycleScope)
                        wv.addJavascriptInterface(nativeBridge, com.webtoapp.core.webview.NativeBridge.JS_INTERFACE_NAME)
                    },
                    onFileChooser = { callback, _ ->
                        filePathCallback = callback
                        fileChooserLauncher.launch("*/*")
                        true
                    },
                    onShowCustomView = { view, callback ->
                        customView = view
                        customViewCallback = callback
                        showCustomView(view)
                    },
                    onHideCustomView = {
                        hideCustomView()
                    },
                    onFullscreenModeChanged = { enabled ->
                        immersiveFullscreenEnabled = enabled
                        if (customView == null) {
                            applyImmersiveFullscreen(enabled)
                        }
                    }
                )
            }
        }

        // 返回键处理
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> hideCustomView()
                    webView?.canGoBack() == true -> webView?.goBack()
                    else -> finish()
                }
            }
        })
    }

    private fun showCustomView(view: View) {
        // 保存当前屏幕方向，进入横屏全屏模式
        originalOrientationBeforeFullscreen = requestedOrientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val decorView = window.decorView as FrameLayout
        decorView.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        applyImmersiveFullscreen(true)
    }

    private fun hideCustomView() {
        customView?.let { view ->
            val decorView = window.decorView as FrameLayout
            decorView.removeView(view)
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null

            // 恢复原来的屏幕方向
            requestedOrientation = originalOrientationBeforeFullscreen
            originalOrientationBeforeFullscreen = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            applyImmersiveFullscreen(immersiveFullscreenEnabled)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen(customView != null || immersiveFullscreenEnabled)
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    appId: Long,
    directUrl: String?,
    testUrl: String? = null,
    testModuleIds: List<String>? = null,
    onStatusBarConfigChanged: ((com.webtoapp.data.model.StatusBarColorMode, String?, Boolean?, Boolean, com.webtoapp.data.model.StatusBarBackgroundType) -> Unit)? = null,
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

    // 是否为测试模式
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
    // 激活状态：默认未激活，防止 WebView 在检查完成前加载
    var isActivated by remember { mutableStateOf(false) }
    // 激活检查是否完成
    var isActivationChecked by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 启动画面状态
    var showSplash by remember { mutableStateOf(false) }
    var splashCountdown by remember { mutableIntStateOf(0) }
    var originalOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

    // 背景音乐播放器
    val bgmPlayer = remember { BgmPlayer(context) }

    // WebView引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 长按菜单状态
    var showLongPressMenu by remember { mutableStateOf(false) }
    var longPressResult by remember { mutableStateOf<LongPressHandler.LongPressResult?>(null) }
    val scope = rememberCoroutineScope()
    val longPressHandler = remember { LongPressHandler(context, scope) }

    // 控制台状态
    var showConsole by remember { mutableStateOf(false) }
    var consoleMessages by remember { mutableStateOf<List<ConsoleLogEntry>>(emptyList()) }

    // 状态栏背景配置（用于预览时显示）
    var statusBarBackgroundType by remember { mutableStateOf("COLOR") }
    var statusBarBackgroundColor by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundImage by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundAlpha by remember { mutableFloatStateOf(1.0f) }
    var statusBarHeightDp by remember { mutableIntStateOf(0) }

    // 当 webApp 加载完成后，通知状态栏配置并更新本地状态
    LaunchedEffect(webApp) {
        webApp?.let { app ->
            onStatusBarConfigChanged?.invoke(
                app.webViewConfig.statusBarColorMode,
                app.webViewConfig.statusBarColor,
                app.webViewConfig.statusBarDarkIcons,
                app.webViewConfig.showStatusBarInFullscreen,
                app.webViewConfig.statusBarBackgroundType
            )
            // 更新状态栏背景配置
            statusBarBackgroundType = app.webViewConfig.statusBarBackgroundType.name
            statusBarBackgroundColor = app.webViewConfig.statusBarColor
            statusBarBackgroundImage = app.webViewConfig.statusBarBackgroundImage
            statusBarBackgroundAlpha = app.webViewConfig.statusBarBackgroundAlpha
            statusBarHeightDp = app.webViewConfig.statusBarHeightDp
        }
    }

    // 加载应用配置
    LaunchedEffect(appId, directUrl, testUrl) {
        // 测试模式：直接标记为已激活，不需要加载应用配置
        if (isTestMode) {
            isActivated = true
            isActivationChecked = true
            return@LaunchedEffect
        }

        // 如果是直接URL模式，不需要激活检查
        if (!directUrl.isNullOrBlank()) {
            isActivated = true
            isActivationChecked = true
            return@LaunchedEffect
        }

        if (appId > 0) {
            val app = repository.getWebApp(appId)
            webApp = app
            if (app != null) {
                // 配置广告拦截
                if (app.adBlockEnabled) {
                    adBlocker.initialize(app.adBlockRules, useDefaultRules = true)
                    adBlocker.setEnabled(true)
                }

                // 检查激活状态
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

                // 检查公告
                if (app.announcementEnabled && isActivated) {
                    val shouldShow = announcement.shouldShowAnnouncement(appId, app.announcement)
                    showAnnouncementDialog = shouldShow
                }

                // 检查启动画面
                if (app.splashEnabled && app.splashConfig != null && isActivated) {
                    val mediaPath = app.splashConfig.mediaPath
                    if (mediaPath != null && File(mediaPath).exists()) {
                        showSplash = true
                        splashCountdown = app.splashConfig.duration

                        // 处理横屏显示
                        if (app.splashConfig.orientation == SplashOrientation.LANDSCAPE) {
                            originalOrientation = activity.requestedOrientation
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }
                }

                // 初始化背景音乐
                if (app.bgmEnabled && app.bgmConfig != null && isActivated) {
                    bgmPlayer.initialize(app.bgmConfig)
                }

                // 设置横屏模式
                if (app.webViewConfig.landscapeMode) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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

    // 释放背景音乐播放器
    DisposableEffect(Unit) {
        onDispose {
            bgmPlayer.release()
        }
    }

    // 启动画面倒计时（仅用于图片类型，视频类型由播放器控制）
    LaunchedEffect(showSplash, splashCountdown) {
        // 视频类型不使用倒计时，由视频播放器控制结束
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

    // WebView回调
    val webViewCallbacks = remember {
        object : WebViewCallbacks {
            override fun onPageStarted(url: String?) {
                isLoading = true
                currentUrl = url ?: ""
            }

            override fun onPageFinished(url: String?) {
                isLoading = false
                isRefreshing = false
                currentUrl = url ?: ""
                webViewRef?.let {
                    canGoBack = it.canGoBack()
                    canGoForward = it.canGoForward()

                    // 注入长按增强脚本（绕过小红书等网站的长按限制）
                    longPressHandler.injectLongPressEnhancer(it)
                }
            }

            override fun onProgressChanged(progress: Int) {
                loadProgress = progress
            }

            override fun onTitleChanged(title: String?) {
                pageTitle = title ?: ""
            }

            override fun onIconReceived(icon: Bitmap?) {}

            override fun onError(errorCode: Int, description: String) {
                errorMessage = description
                isLoading = false
            }

            override fun onSslError(error: String) {
                errorMessage = Strings.sslSecurityError
            }

            override fun onExternalLink(url: String) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
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
                // 使用系统下载管理器下载到 Download 文件夹
                // 媒体文件会自动保存到相册
                DownloadHelper.handleDownload(
                    context = context,
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                    scope = scope
                )
            }

            override fun onLongPress(webView: WebView, x: Float, y: Float): Boolean {
                // 先同步检查 hitTestResult，判断是否需要拦截
                val hitResult = webView.hitTestResult
                val type = hitResult.type

                // 如果是编辑框或未知类型，不拦截，让 WebView 处理默认的文字选择
                if (type == WebView.HitTestResult.EDIT_TEXT_TYPE ||
                    type == WebView.HitTestResult.UNKNOWN_TYPE) {
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
                return when (type) {
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
        }
    }

    val webViewManager = remember { WebViewManager(context, adBlocker) }

    // 本地 HTTP 服务器
    val localHttpServer = remember { LocalHttpServer.getInstance(context) }

    // 根据应用类型构建目标 URL
    val targetUrl = remember(directUrl, webApp, testUrl) {
        val app = webApp  // 捕获到局部变量以支持智能转换
        when {
            // 测试模式优先
            !testUrl.isNullOrBlank() -> testUrl
            !directUrl.isNullOrBlank() -> directUrl
            app?.appType == com.webtoapp.data.model.AppType.HTML -> {
                // HTML 应用：启动本地 HTTP 服务器
                val projectId = app.htmlConfig?.projectId ?: ""
                val entryFile = app.htmlConfig?.getValidEntryFile() ?: "index.html"
                val htmlDir = File(context.filesDir, "html_projects/$projectId")

                if (htmlDir.exists()) {
                    try {
                        // 启动本地服务器并获取 URL
                        val baseUrl = localHttpServer.start(htmlDir)
                        "$baseUrl/$entryFile"
                    } catch (e: Exception) {
                        Log.e("WebViewActivity", "Failed to start local server", e)
                        // 降级到 file:// 协议
                        "file://${htmlDir.absolutePath}/$entryFile"
                    }
                } else {
                    ""
                }
            }
            else -> app?.url ?: ""
        }
    }

    // 清理：停止本地服务器
    DisposableEffect(Unit) {
        onDispose {
            // 注意：不在这里停止服务器，因为可能有多个 WebView 使用
            // localHttpServer.stop()
        }
    }

    // 是否隐藏工具栏（全屏模式）- 测试模式下始终显示工具栏
    val hideToolbar = !isTestMode && webApp?.webViewConfig?.hideToolbar == true

    LaunchedEffect(hideToolbar) {
        onFullscreenModeChanged(hideToolbar)
    }

    // 外层 Box 用于放置状态栏覆盖层（需要在 Scaffold 外部才能正确覆盖状态栏区域）
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        // 在沉浸式模式下，不添加任何内边距
        contentWindowInsets = if (hideToolbar) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        modifier = if (hideToolbar) Modifier.fillMaxSize().imePadding() else Modifier.imePadding(),
        topBar = {
            if (!hideToolbar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isTestMode) {
                                    Strings.moduleTestTitle
                                } else {
                                    pageTitle.ifEmpty { webApp?.name ?: "WebApp" }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            if (isTestMode && !testModuleIds.isNullOrEmpty()) {
                                Text(
                                    text = Strings.testModuleCount.format(testModuleIds.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            } else if (currentUrl.isNotEmpty()) {
                                Text(
                                    text = currentUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { (context as? AppCompatActivity)?.finish() }) {
                            Icon(Icons.Default.Close, Strings.close)
                        }
                    },
                    actions = {
                        // 控制台按钮
                        IconButton(onClick = { showConsole = !showConsole }) {
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
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.Default.ArrowBack, Strings.back)
                        }
                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.Default.ArrowForward, Strings.navForward)
                        }
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, Strings.refresh)
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
        // 全屏模式 + 显示状态栏时，需要给内容添加状态栏高度的 padding，避免被遮挡
        val context = LocalContext.current
        val density = LocalDensity.current

        // 获取系统状态栏高度
        val systemStatusBarHeightDp = remember {
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                with(density) { context.resources.getDimensionPixelSize(resourceId).toDp() }
            } else {
                24.dp
            }
        }

        // 计算实际需要的状态栏 padding（使用自定义高度或系统默认高度）
        val actualStatusBarPadding = if (statusBarHeightDp > 0) statusBarHeightDp.dp else systemStatusBarHeightDp

        val contentModifier = when {
            hideToolbar && webApp?.webViewConfig?.showStatusBarInFullscreen == true -> {
                // 全屏模式但显示状态栏：内容需要在状态栏下方
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
                    progress = loadProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 激活检查中，显示加载状态
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
                        Button(onClick = { showActivationDialog = true }) {
                            Text(Strings.enterActivationCode)
                        }
                    }
                }
            } else if (targetUrl.isNotEmpty() && isActivationChecked) {
                // 控制台展开状态
                var isConsoleExpanded by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxSize()) {
                    // WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
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
                                    moduleIds
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
                                setOnTouchListener { _, event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN,
                                        MotionEvent.ACTION_MOVE -> {
                                            lastTouchX = event.x
                                            lastTouchY = event.y
                                        }
                                    }
                                    false // 不消费事件，让 WebView 继续处理
                                }
                                setOnLongClickListener {
                                    webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                                }

                                onWebViewCreated(this)
                                webViewRef = this

                                // 加载目标 URL
                                // HTML 应用通过 LocalHttpServer 提供 http://localhost:PORT 的 URL
                                // 这样可以正常加载外部 CDN 资源
                                loadUrl(targetUrl)
                            }
                        },
                        modifier = Modifier.weight(1f)
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
            }

            // 错误提示
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
                        Text(error, modifier = Modifier.weight(1f))
                        TextButton(onClick = { errorMessage = null }) {
                            Text(Strings.close)
                        }
                    }
                }
            }

            // 注意：状态栏覆盖层已移到 Scaffold 外部
        }
    }

    // 状态栏背景覆盖层（在全屏模式下显示状态栏时）
    // 放在 Scaffold 外部，才能正确覆盖在状态栏区域
    if (hideToolbar && webApp?.webViewConfig?.showStatusBarInFullscreen == true) {
        com.webtoapp.ui.components.StatusBarOverlay(
            show = true,
            backgroundType = statusBarBackgroundType,
            backgroundColor = statusBarBackgroundColor,
            backgroundImagePath = statusBarBackgroundImage,
            alpha = statusBarBackgroundAlpha,
            heightDp = statusBarHeightDp,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
    } // 关闭外层 Box

    // 激活码对话框
    if (showActivationDialog) {
        val activationStatus = remember {
            kotlinx.coroutines.runBlocking {
                try {
                    activation.getActivationStatus(appId)
                } catch (e: Exception) {
                    null
                }
            }
        }

        com.webtoapp.ui.components.EnhancedActivationDialog(
            onDismiss = { showActivationDialog = false },
            onActivate = { code ->
                val allCodes = webApp?.getActivationCodeStrings() ?: emptyList()
                return@EnhancedActivationDialog activation.verifyActivationCode(appId, code, allCodes)
            },
            activationStatus = activationStatus
        )

        // 监听激活状态变化
        LaunchedEffect(Unit) {
            activation.isActivated(appId).collect { activated ->
                if (activated) {
                    isActivated = true
                    showActivationDialog = false
                    // 检查公告
                    if (webApp?.announcementEnabled == true) {
                        val shouldShow = announcement.shouldShowAnnouncement(appId, webApp?.announcement)
                        showAnnouncementDialog = shouldShow
                    }
                }
            }
        }
    }

    // 公告对话框 - 使用模板系统
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
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

    // 启动画面覆盖层
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
                // 播放完成回调（始终需要）
                onComplete = closeSplash
            )
        }
    }

    // 长按菜单
    if (showLongPressMenu && longPressResult != null) {
        LongPressMenuSheet(
            result = longPressResult!!,
            onDismiss = {
                showLongPressMenu = false
                longPressResult = null
            },
            onCopyLink = { url ->
                longPressHandler.copyToClipboard(url)
            },
            onSaveImage = { url ->
                longPressHandler.saveImage(url) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onDownloadVideo = { url ->
                longPressHandler.downloadVideo(url) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onOpenInBrowser = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun ActivationDialog(
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.activateApp) },
        text = {
            Column {
                Text(Strings.enterCodeToContinue)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                    },
                    label = { Text(Strings.activationCode) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        error = Strings.enterActivationCode
                    } else {
                        onActivate(code)
                    }
                }
            ) {
                Text(Strings.activate)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

/**
 * 启动画面覆盖层
 * 支持图片和视频（含裁剪播放）
 */
@Composable
fun SplashOverlay(
    splashConfig: SplashConfig,
    countdown: Int,
    onSkip: (() -> Unit)?,           // 点击跳过回调
    onComplete: (() -> Unit)? = null // 播放完成回调
) {
    val context = LocalContext.current
    val mediaPath = splashConfig.mediaPath ?: return

    // 视频裁剪相关
    val videoStartMs = splashConfig.videoStartMs
    val videoEndMs = splashConfig.videoEndMs
    val videoDurationMs = videoEndMs - videoStartMs
    val contentScaleMode = if (splashConfig.fillScreen) ContentScale.Crop else ContentScale.Fit

    // 视频剩余时间（用于动态倒计时显示）
    var videoRemainingMs by remember { mutableLongStateOf(videoDurationMs) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (onSkip != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSkip() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (splashConfig.type) {
            SplashType.IMAGE -> {
                // 图片启动画面
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(File(mediaPath))
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = Strings.splashScreen,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScaleMode
                )
            }
            SplashType.VIDEO -> {
                // 视频启动画面 - 支持裁剪播放
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }

                // 监控播放进度，到达结束时间时停止
                // 仅在播放器准备就绪后开始监控
                LaunchedEffect(isPlayerReady) {
                    if (!isPlayerReady) return@LaunchedEffect
                    mediaPlayer?.let { mp ->
                        // 等待播放器真正开始播放
                        while (!mp.isPlaying) {
                            delay(50)
                            // 如果播放器被释放则退出
                            if (mediaPlayer == null) return@LaunchedEffect
                        }
                        // 监控播放进度并更新剩余时间
                        while (mp.isPlaying) {
                            val currentPos = mp.currentPosition
                            // 更新剩余时间用于倒计时显示
                            videoRemainingMs = (videoEndMs - currentPos).coerceAtLeast(0L)
                            if (currentPos >= videoEndMs) {
                                mp.pause()
                                // 使用 onComplete 回调
                                onComplete?.invoke()
                                break
                            }
                            delay(100) // 100ms 更新一次倒计时显示
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        android.view.SurfaceView(ctx).apply {
                            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                    try {
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(mediaPath)
                                            setSurface(holder.surface)
                                            // 根据配置决定是否启用音频
                                            val volume = if (splashConfig.enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = false
                                            setOnPreparedListener {
                                                // 跳到裁剪起始位置
                                                seekTo(videoStartMs.toInt())
                                                start()
                                                isPlayerReady = true
                                            }
                                            setOnCompletionListener { onComplete?.invoke() }
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        onComplete?.invoke()
                                    }
                                }
                                override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, ht: Int) {}
                                override fun surfaceDestroyed(h: android.view.SurfaceHolder) {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 组件销毁时释放 MediaPlayer
                DisposableEffect(Unit) {
                    onDispose {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }
            }
        }

        // 倒计时/跳过提示
        // 视频使用动态剩余时间，图片使用传入的 countdown
        val displayTime = if (splashConfig.type == SplashType.VIDEO) {
            ((videoRemainingMs + 999) / 1000).toInt()
        } else {
            countdown
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = MaterialTheme.shapes.small,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (displayTime > 0) {
                    Text(
                        text = "${displayTime}s",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (onSkip != null) {
                    if (displayTime > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "|",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = Strings.skip,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


// ========== 控制台相关组件 ==========

/**
 * 控制台日志级别
 */
enum class ConsoleLevel {
    LOG, INFO, WARNING, ERROR, DEBUG
}

/**
 * 控制台日志条目
 */
data class ConsoleLogEntry(
    val level: ConsoleLevel,
    val message: String,
    val source: String,
    val lineNumber: Int,
    val timestamp: Long
)

/**
 * 控制台面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsolePanel(
    consoleMessages: List<ConsoleLogEntry>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onClear: () -> Unit,
    onRunScript: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scriptInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()) }

    // 主题颜色
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainer = MaterialTheme.colorScheme.errorContainer

    // 自动滚动到底部
    LaunchedEffect(consoleMessages.size) {
        if (consoleMessages.isNotEmpty()) {
            listState.animateScrollToItem(consoleMessages.size - 1)
        }
    }

    // 固定高度，确保可以滑动
    val panelHeight = if (isExpanded) 350.dp else 200.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight),
        color = surfaceColor,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 头部工具栏
            Surface(
                color = surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            Strings.console,
                            style = MaterialTheme.typography.titleSmall,
                            color = onSurface
                        )
                        // 错误/警告计数
                        val errorCount = consoleMessages.count { it.level == ConsoleLevel.ERROR }
                        val warnCount = consoleMessages.count { it.level == ConsoleLevel.WARNING }
                        if (errorCount > 0) {
                            Badge(containerColor = errorColor) {
                                Text("$errorCount")
                            }
                        }
                        if (warnCount > 0) {
                            Badge(containerColor = Color(0xFFFFB74D)) {
                                Text("$warnCount", color = Color.Black)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 复制全部
                        IconButton(
                            onClick = {
                                val allLogs = consoleMessages.joinToString("\n") { entry ->
                                    "[${timeFormat.format(java.util.Date(entry.timestamp))}] [${entry.level}] ${entry.message}"
                                }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(allLogs))
                                Toast.makeText(context, Strings.copiedAllLogs, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, Strings.copy, tint = onSurfaceVariant)
                        }
                        // 清空
                        IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Delete, Strings.clean, tint = onSurfaceVariant)
                        }
                        // 展开/收起
                        IconButton(onClick = onExpandToggle, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                if (isExpanded) Strings.close else Strings.more,
                                tint = onSurfaceVariant
                            )
                        }
                        // 关闭
                        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, Strings.close, tint = onSurfaceVariant)
                        }
                    }
                }
            }

            // 控制台消息列表
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (consoleMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Strings.noConsoleMessages,
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(consoleMessages.size) { index ->
                            val entry = consoleMessages[index]
                            ConsoleLogItem(
                                entry = entry,
                                timeFormat = timeFormat,
                                onCopy = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(entry.message))
                                    Toast.makeText(context, Strings.msgCopied, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // 脚本输入区
            Surface(
                color = surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ">",
                        color = primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedTextField(
                        value = scriptInput,
                        onValueChange = { scriptInput = it },
                        placeholder = {
                            Text(
                                Strings.jsConsoleInputHint,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = {
                            if (scriptInput.isNotBlank()) {
                                onRunScript(scriptInput)
                                scriptInput = ""
                            }
                        },
                        enabled = scriptInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.PlayArrow, Strings.run)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleLogItem(
    entry: ConsoleLogEntry,
    timeFormat: java.text.SimpleDateFormat,
    onCopy: () -> Unit
) {
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val errorColor = MaterialTheme.colorScheme.error
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val backgroundColor = when (entry.level) {
        ConsoleLevel.ERROR -> errorContainer.copy(alpha = 0.3f)
        ConsoleLevel.WARNING -> Color(0xFFFFB74D).copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val textColor = when (entry.level) {
        ConsoleLevel.ERROR -> errorColor
        ConsoleLevel.WARNING -> Color(0xFFFF9800)
        ConsoleLevel.DEBUG -> Color(0xFF4CAF50)
        else -> onSurface
    }

    val icon = when (entry.level) {
        ConsoleLevel.ERROR -> "❌"
        ConsoleLevel.WARNING -> "⚠️"
        ConsoleLevel.DEBUG -> "🔍"
        ConsoleLevel.INFO -> "ℹ️"
        ConsoleLevel.LOG -> "📝"
    }

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 图标
            Text(
                icon,
                modifier = Modifier.padding(end = 8.dp),
                fontSize = 14.sp
            )

            // 消息内容
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        entry.message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = textColor
                    )
                }

                // 来源信息
                Text(
                    "${entry.source}:${entry.lineNumber} • ${timeFormat.format(java.util.Date(entry.timestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 复制按钮
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    Strings.copy,
                    tint = onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
