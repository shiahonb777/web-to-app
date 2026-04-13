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

/**
 * WebView容器Activity - 用于预览和运行WebApp
 */
class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_APP_ID = "app_id"
        private const val EXTRA_URL = "url"
        private const val EXTRA_TEST_URL = "test_url"
        private const val EXTRA_TEST_MODULE_IDS = "test_module_ids"
        private const val EXTRA_PREVIEW_APP_JSON = "preview_app_json"

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
         * 启动预览模式 — 携带完整的 WebApp 配置 JSON
         * 预览效果与保存后打开完全一致（广告拦截、UA伪装、翻译等全部生效）
         */
        fun startPreview(context: Context, webAppJson: String) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_PREVIEW_APP_JSON, webAppJson)
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
    
    // Permission请求相关
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null

    private var immersiveFullscreenEnabled: Boolean = false
    private var showStatusBarInFullscreen: Boolean = false  // Fullscreen模式下是否显示状态栏
    internal var showNavigationBarInFullscreen: Boolean = false  // Fullscreen模式下是否显示导航栏
    
    // Video全屏前的屏幕方向
    private var originalOrientationBeforeFullscreen: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    
    // Status bar配置缓存
    private var statusBarColorMode: com.webtoapp.data.model.StatusBarColorMode = com.webtoapp.data.model.StatusBarColorMode.THEME
    private var statusBarCustomColor: String? = null
    private var statusBarDarkIcons: Boolean? = null
    private var statusBarBackgroundType: com.webtoapp.data.model.StatusBarBackgroundType = com.webtoapp.data.model.StatusBarBackgroundType.COLOR
    // Status bar深色模式配置缓存
    private var statusBarColorModeDark: com.webtoapp.data.model.StatusBarColorMode = com.webtoapp.data.model.StatusBarColorMode.THEME
    private var statusBarCustomColorDark: String? = null
    private var statusBarDarkIconsDark: Boolean? = null
    private var statusBarBackgroundTypeDark: com.webtoapp.data.model.StatusBarBackgroundType = com.webtoapp.data.model.StatusBarBackgroundType.COLOR
    internal var keyboardAdjustMode: KeyboardAdjustMode = KeyboardAdjustMode.RESIZE  // 键盘调整模式
    // 当前深色主题状态（从 Compose 同步，用于 onWindowFocusChanged 等 Activity 级别回调）
    private var currentIsDarkTheme: Boolean = false

    private fun applyStatusBarColor(
        colorMode: com.webtoapp.data.model.StatusBarColorMode,
        customColor: String?,
        darkIcons: Boolean?,
        isDarkTheme: Boolean
    ) = WindowHelper.applyStatusBarColor(this, colorMode.name, customColor, darkIcons, isDarkTheme)

    private fun applyImmersiveFullscreen(enabled: Boolean, hideNavBar: Boolean? = null, isDarkTheme: Boolean = currentIsDarkTheme) {
        val shouldHideNavBar = hideNavBar ?: !showNavigationBarInFullscreen
        // 使用深色/浅色模式对应的状态栏配置
        val effectiveColorMode = if (isDarkTheme) statusBarColorModeDark else statusBarColorMode
        val effectiveCustomColor = if (isDarkTheme) statusBarCustomColorDark else statusBarCustomColor
        val effectiveDarkIcons = if (isDarkTheme) statusBarDarkIconsDark else statusBarDarkIcons
        val effectiveBgType = if (isDarkTheme) statusBarBackgroundTypeDark else statusBarBackgroundType
        WindowHelper.applyImmersiveFullscreen(
            activity = this,
            enabled = enabled,
            hideNavBar = shouldHideNavBar,
            isDarkTheme = isDarkTheme,
            showStatusBar = showStatusBarInFullscreen,
            statusBarColorMode = effectiveColorMode.name,
            statusBarCustomColor = effectiveCustomColor,
            statusBarDarkIcons = effectiveDarkIcons,
            statusBarBgType = effectiveBgType.name,
            keyboardAdjustMode = keyboardAdjustMode,
            tag = "WebViewActivity"
        )
    }

    // 相机拍照临时文件 URI
    private var cameraPhotoUri: android.net.Uri? = null
    
    private val fileChooserActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback
        if (callback == null) return@registerForActivityResult
        
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUris = mutableListOf<android.net.Uri>()
            val data = result.data
            if (data == null || (data.data == null && data.clipData == null)) {
                cameraPhotoUri?.let { resultUris.add(it) }
            } else {
                data.data?.let { resultUris.add(it) }
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { resultUris.add(it) }
                    }
                }
            }
            callback.onReceiveValue(resultUris.toTypedArray())
        } else {
            callback.onReceiveValue(null)
        }
        filePathCallback = null
        cameraPhotoUri = null
    }
    
    // 相机权限请求（文件选择器场景）
    private var pendingFileChooserParams: android.webkit.WebChromeClient.FileChooserParams? = null
    private val cameraForChooserPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        launchFileChooserIntent(pendingFileChooserParams)
        pendingFileChooserParams = null
    }
    
    private fun handleFileChooser(
        callback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
        params: android.webkit.WebChromeClient.FileChooserParams?
    ): Boolean {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        if (callback == null) return false
        
        val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasCam) {
            pendingFileChooserParams = params
            cameraForChooserPermLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            launchFileChooserIntent(params)
        }
        return true
    }
    
    private fun launchFileChooserIntent(params: android.webkit.WebChromeClient.FileChooserParams?) {
        try {
            val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val extraIntents = mutableListOf<android.content.Intent>()
            if (hasCam) {
                try {
                    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    val dir = java.io.File(cacheDir, "camera_photos").apply { mkdirs() }
                    val photoFile = java.io.File.createTempFile("IMG_${ts}_", ".jpg", dir)
                    cameraPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                        this, "${packageName}.fileprovider", photoFile
                    )
                    val camIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
                        addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    if (camIntent.resolveActivity(packageManager) != null) extraIntents.add(camIntent)
                    val vidIntent = android.content.Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
                    if (vidIntent.resolveActivity(packageManager) != null) extraIntents.add(vidIntent)
                } catch (e: Exception) {
                    AppLogger.e("WebViewActivity", "Camera intent failed", e)
                }
            }
            
            val acceptTypes = params?.acceptTypes ?: arrayOf("*/*")
            val mimeType = if (acceptTypes.isNotEmpty() && !acceptTypes[0].isNullOrBlank()) acceptTypes[0] else "*/*"
            val contentIntent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = mimeType
                if (params?.mode == android.webkit.WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                if (acceptTypes.size > 1) {
                    putExtra(android.content.Intent.EXTRA_MIME_TYPES, acceptTypes.filter { !it.isNullOrBlank() }.toTypedArray())
                    type = "*/*"
                }
            }
            val chooser = android.content.Intent.createChooser(contentIntent, null).apply {
                if (extraIntents.isNotEmpty()) putExtra(android.content.Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
            }
            fileChooserActivityLauncher.launch(chooser)
        } catch (e: Exception) {
            AppLogger.e("WebViewActivity", "File chooser launch failed", e)
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }
    
    // Permission请求launcher（用于摄像头、麦克风等）
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
            AppLogger.d("WebViewActivity", "Notification permission granted")
        } else {
            AppLogger.d("WebViewActivity", "Notification permission denied")
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
                    androidPermissions.add(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
                }
                PermissionRequest.RESOURCE_MIDI_SYSEX -> {
                    // MIDI SysEx 不需要额外 Android 运行时权限，直接授权
                }
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                    // Protected Media ID 不需要额外 Android 运行时权限，直接授权
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

    // 使用追踪器
    private var usageTracker: AppUsageTracker? = null
    private var trackedAppId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable边到边显示（让内容延伸到系统栏区域）
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            AppLogger.w("WebViewActivity", "enableEdgeToEdge failed", e)
        }
        
        super.onCreate(savedInstanceState)
        
        // Request通知权限（Android 13+），用于显示下载进度和完成通知
        requestNotificationPermissionIfNeeded()
        
        // Initialize时不启用沉浸式模式，等待 WebApp 配置加载后再根据 hideToolbar 决定
        // 这样可以确保非全屏模式下状态栏正常显示
        immersiveFullscreenEnabled = false
        applyImmersiveFullscreen(immersiveFullscreenEnabled)

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1)
        
        // 使用统计追踪
        if (appId > 0) {
            trackedAppId = appId
            try {
                usageTracker = org.koin.java.KoinJavaComponent.get(AppUsageTracker::class.java)
                usageTracker?.trackLaunch(appId)
            } catch (e: Exception) {
                AppLogger.w("WebViewActivity", "Usage tracker init failed: ${e.message}")
            }
        }
        val directUrl = intent.getStringExtra(EXTRA_URL)
        
        // 测试模式参数
        val testUrl = intent.getStringExtra(EXTRA_TEST_URL)
        val testModuleIds = intent.getStringArrayListExtra(EXTRA_TEST_MODULE_IDS)
        
        // 预览模式：从 JSON 还原完整 WebApp 配置
        val previewAppJson = intent.getStringExtra(EXTRA_PREVIEW_APP_JSON)
        val previewApp: com.webtoapp.data.model.WebApp? = if (!previewAppJson.isNullOrBlank()) {
            try {
                com.google.gson.Gson().fromJson(previewAppJson, com.webtoapp.data.model.WebApp::class.java)
            } catch (e: Exception) {
                AppLogger.w("WebViewActivity", "Failed to parse preview WebApp JSON: ${e.message}")
                null
            }
        } else null

        setContent {
            WebToAppTheme { isDarkTheme ->
                // 同步深色主题状态到 Activity 级别（供 onWindowFocusChanged 使用）
                SideEffect {
                    currentIsDarkTheme = isDarkTheme
                }

                // 当主题变化时更新状态栏颜色（根据深色/浅色模式选择对应配置）
                LaunchedEffect(isDarkTheme, statusBarColorMode, statusBarColorModeDark) {
                    if (!immersiveFullscreenEnabled) {
                        val effectiveColorMode = if (isDarkTheme) statusBarColorModeDark else statusBarColorMode
                        val effectiveCustomColor = if (isDarkTheme) statusBarCustomColorDark else statusBarCustomColor
                        val effectiveDarkIcons = if (isDarkTheme) statusBarDarkIconsDark else statusBarDarkIcons
                        applyStatusBarColor(effectiveColorMode, effectiveCustomColor, effectiveDarkIcons, isDarkTheme)
                    }
                }
                
                WebViewScreen(
                    appId = appId,
                    directUrl = directUrl,
                    previewApp = previewApp,
                    testUrl = testUrl,
                    testModuleIds = testModuleIds,
                    onStatusBarConfigChanged = { colorMode, customColor, darkIcons, showStatusBar, backgroundType, colorModeDark, customColorDark, darkIconsDark, backgroundTypeDark ->
                        // Update state栏配置
                        statusBarColorMode = colorMode
                        statusBarCustomColor = customColor
                        statusBarDarkIcons = darkIcons
                        showStatusBarInFullscreen = showStatusBar
                        statusBarBackgroundType = backgroundType
                        // Update深色模式状态栏配置
                        statusBarColorModeDark = colorModeDark
                        statusBarCustomColorDark = customColorDark
                        statusBarDarkIconsDark = darkIconsDark
                        statusBarBackgroundTypeDark = backgroundTypeDark
                    },
                    onWebViewCreated = { wv -> 
                        webView = wv
                        // Ensure process-level WebView timers are resumed as soon as a new instance is created.
                        // This prevents reopen-after-back pages from staying half-loaded when previous activity paused timers.
                        wv.onResume()
                        wv.resumeTimers()
                        // 添加下载桥接（支持 Blob/Data URL 下载）
                        val downloadBridge = com.webtoapp.core.webview.DownloadBridge(this@WebViewActivity, lifecycleScope)
                        wv.addJavascriptInterface(downloadBridge, com.webtoapp.core.webview.DownloadBridge.JS_INTERFACE_NAME)
                        // 添加原生能力桥接（供扩展模块调用）
                        val nativeBridge = com.webtoapp.core.webview.NativeBridge(this@WebViewActivity, lifecycleScope)
                        wv.addJavascriptInterface(nativeBridge, com.webtoapp.core.webview.NativeBridge.JS_INTERFACE_NAME)
                    },
                    onFileChooser = { callback, params ->
                        handleFileChooser(callback, params)
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
                    else -> {
                        // 先向 WebView 派发 ESC 按键事件，让 JS 脚本有机会处理
                        val wv = webView
                        if (wv != null) {
                            wv.evaluateJavascript("""
                                (function() {
                                    var evt = new KeyboardEvent('keydown', {
                                        key: 'Escape', code: 'Escape',
                                        keyCode: 27, which: 27,
                                        bubbles: true, cancelable: true
                                    });
                                    return !document.dispatchEvent(evt);
                                })();
                            """.trimIndent()) { result ->
                                if (result == "true") {
                                    // JS 脚本调用了 preventDefault()，不执行原生返回
                                    return@evaluateJavascript
                                }
                                // JS 未拦截，执行原生返回行为
                                // Check if going back would land on about:blank (WebView's
                                // initial history entry). If so, finish() instead of showing
                                // the blank page.
                                val backList = wv.copyBackForwardList()
                                val currentIndex = backList.currentIndex
                                if (wv.canGoBack() && currentIndex > 0) {
                                    val prevUrl = backList.getItemAtIndex(currentIndex - 1)?.url
                                    if (prevUrl == "about:blank") {
                                        finish()
                                    } else {
                                        wv.goBack()
                                    }
                                } else {
                                    finish()
                                }
                            }
                        } else {
                            finish()
                        }
                    }
                }
            }
        })
    }

    private fun showCustomView(view: View) {
        originalOrientationBeforeFullscreen = WindowHelper.showCustomView(this, view)
        applyImmersiveFullscreen(true)
    }

    private fun hideCustomView() {
        customView?.let { view ->
            WindowHelper.hideCustomView(this, view, customViewCallback, originalOrientationBeforeFullscreen)
            customView = null
            customViewCallback = null
            originalOrientationBeforeFullscreen = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            applyImmersiveFullscreen(immersiveFullscreenEnabled)
        }
    }

    private fun shouldForwardKeyToWebView(event: KeyEvent): Boolean {
        if (event.isSystem) {
            return false
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_POWER -> false
            else -> true
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (shouldForwardKeyToWebView(event) && webView?.dispatchKeyEvent(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (customView != null || immersiveFullscreenEnabled) {
                applyImmersiveFullscreen(true, isDarkTheme = currentIsDarkTheme)
            } else {
                // 非全屏模式：重新应用状态栏颜色（使用正确的深色/浅色模式值）
                val effectiveColorMode = if (currentIsDarkTheme) statusBarColorModeDark else statusBarColorMode
                val effectiveCustomColor = if (currentIsDarkTheme) statusBarCustomColorDark else statusBarCustomColor
                val effectiveDarkIcons = if (currentIsDarkTheme) statusBarDarkIconsDark else statusBarDarkIcons
                applyStatusBarColor(effectiveColorMode, effectiveCustomColor, effectiveDarkIcons, currentIsDarkTheme)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        if (trackedAppId > 0) usageTracker?.trackResume(trackedAppId)
    }

    override fun onPause() {
        if (trackedAppId > 0) usageTracker?.trackPause(trackedAppId)
        webView?.onPause()
        android.webkit.CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            webView?.clearCache(false)
            com.webtoapp.core.logging.AppLogger.w("WebViewActivity", "Memory pressure (level=$level), cleared WebView cache")
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            webView?.freeMemory()
            System.gc()
            com.webtoapp.core.logging.AppLogger.w("WebViewActivity", "Critical memory pressure, freed WebView memory")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        webView?.clearCache(false)
        webView?.freeMemory()
        System.gc()
        com.webtoapp.core.logging.AppLogger.w("WebViewActivity", "Low memory, cleared cache and freed WebView memory")
    }

    override fun onDestroy() {
        // 使用统计追踪
        if (trackedAppId > 0) usageTracker?.trackClose(trackedAppId)
        
        // 先刷盘 Cookie 和 WebStorage，确保 localStorage/sessionStorage 持久化
        android.webkit.CookieManager.getInstance().flush()
        webView?.let { wv ->
            wv.stopLoading()
            // 注意：不再导航到 about:blank
            // 在 destroy 前 loadUrl("about:blank") 会导致 WebView 切换 origin，
            // 某些 Android 版本来不及将当前页面的 localStorage 刷盘，造成 H5 游戏存档丢失
            wv.onPause()
            wv.webChromeClient = null
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
        webView = null
        super.onDestroy()
    }
}
