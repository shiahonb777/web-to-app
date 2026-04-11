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

    // WordPress 预览流程
    LaunchedEffect(webApp, isActivated, isActivationChecked, wpRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.WORDPRESS) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect
        
        wordPressPreviewState = WordPressPreviewState.CheckingDeps
        
        // 1. 检查/下载依赖
        if (!WordPressDependencyManager.isAllReady(context)) {
            wordPressPreviewState = WordPressPreviewState.Downloading
            val success = WordPressDependencyManager.downloadAllDependencies(context)
            if (!success) {
                wordPressPreviewState = WordPressPreviewState.Error(Strings.wpDownloadFailed)
                return@LaunchedEffect
            }
        }
        
        // 2. 确保项目存在
        var projectId = app.wordpressConfig?.projectId ?: ""
        val projectDir = if (projectId.isNotEmpty()) {
            WordPressManager.getProjectDir(context, projectId)
        } else null
        
        if (projectDir == null || !projectDir.exists() || !File(projectDir, "wp-includes/version.php").exists()) {
            wordPressPreviewState = WordPressPreviewState.CreatingProject
            val newId = WordPressManager.createProject(
                context = context,
                siteTitle = app.wordpressConfig?.siteTitle ?: "My Site",
                adminUser = app.wordpressConfig?.adminUser ?: "admin",
                adminEmail = app.wordpressConfig?.adminEmail ?: ""
            )
            if (newId == null) {
                wordPressPreviewState = WordPressPreviewState.Error(Strings.wpProjectCreateFailed)
                return@LaunchedEffect
            }
            projectId = newId
            // 更新 App 配置中的 projectId
            val updatedConfig = (app.wordpressConfig ?: WordPressConfig()).copy(projectId = newId)
            repository.updateWebApp(app.copy(wordpressConfig = updatedConfig))
            webApp = app.copy(wordpressConfig = updatedConfig)
        }
        
        // 3. 启动 PHP 服务器（先确保 SQLite db.php drop-in 存在）
        wordPressPreviewState = WordPressPreviewState.StartingServer
        val wpDir = WordPressManager.getProjectDir(context, projectId)
        WordPressManager.ensureDbPhpExists(context, wpDir)
        val port = phpRuntime.startServer(wpDir.absolutePath, app.wordpressConfig?.phpPort ?: 0)
        
        if (port > 0) {
            val url = "http://127.0.0.1:$port/"
            // 自动完成 WordPress 安装（首次创建项目时数据库为空）
            WordPressManager.autoInstallIfNeeded(
                baseUrl = "http://127.0.0.1:$port",
                siteTitle = app.wordpressConfig?.siteTitle?.takeIf { it.isNotBlank() } ?: "My Site",
                adminUser = app.wordpressConfig?.adminUser?.takeIf { it.isNotBlank() } ?: "admin",
                adminEmail = app.wordpressConfig?.adminEmail?.takeIf { it.isNotBlank() } ?: "admin@localhost.local"
            )
            wordPressPreviewState = WordPressPreviewState.Ready(url)
            delay(200)  // 等待 WebView factory 完成
            webViewRef?.loadUrl(url)
        } else {
            wordPressPreviewState = WordPressPreviewState.Error(Strings.wpServerError)
        }
    }
    
    // WordPress: 清理 PHP 服务器
    DisposableEffect(phpRuntime) {
        onDispose {
            phpRuntime.stopServer()
        }
    }
    
    // PHP 应用预览流程
    LaunchedEffect(webApp, isActivated, isActivationChecked, phpAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.PHP_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect
        
        AppLogger.i("PhpAppPreview", "开始 PHP 应用预览流程, appId=$appId, phpAppConfig=${app.phpAppConfig}")
        
        val config = app.phpAppConfig
        if (config == null) {
            AppLogger.e("PhpAppPreview", "phpAppConfig 为 null，无法启动预览")
            phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppProjectNotFound)
            return@LaunchedEffect
        }
        
        phpAppPreviewState = PhpAppPreviewState.CheckingDeps
        AppLogger.i("PhpAppPreview", "检查 PHP 依赖, isPhpReady=${WordPressDependencyManager.isPhpReady(context)}")
        
        // 1. 检查/下载 PHP 二进制依赖
        if (!WordPressDependencyManager.isPhpReady(context)) {
            phpAppPreviewState = PhpAppPreviewState.Downloading
            val success = WordPressDependencyManager.downloadPhpDependency(context)
            if (!success) {
                phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppDownloadFailed)
                return@LaunchedEffect
            }
        }
        
        // 2. 检查项目目录是否存在
        val projectId = config.projectId
        AppLogger.i("PhpAppPreview", "projectId='$projectId', docRoot='${config.documentRoot}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("PhpAppPreview", "projectId 为空")
            phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppProjectNotFound)
            return@LaunchedEffect
        }
        val projectDir = phpAppRuntime.getProjectDir(projectId)
        AppLogger.i("PhpAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppProjectNotFound)
            return@LaunchedEffect
        }
        
        // 列出项目文件（调试）
        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("PhpAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }
        
        // 3. 自动检测框架和 document root（当配置不正确时自动修正）
        var actualDocRoot = config.documentRoot
        var actualEntryFile = config.entryFile
        
        // 检查入口文件是否存在，不存在则重新检测
        var actualProjectDir = projectDir
        val docRootDir = if (actualDocRoot.isNotBlank()) File(projectDir, actualDocRoot) else projectDir
        if (!File(docRootDir, actualEntryFile).exists()) {
            AppLogger.i("PhpAppPreview", "入口文件不存在，尝试自动检测框架...")
            
            // 先在当前目录检测
            var detectedFramework = phpAppRuntime.detectFramework(projectDir)
            var detectedDocRoot = phpAppRuntime.detectDocumentRoot(projectDir, detectedFramework)
            var detectedEntry = phpAppRuntime.detectEntryFile(projectDir, detectedDocRoot)
            
            // 如果仍然找不到入口文件，扫描子目录（处理 ZIP 导入嵌套目录的情况）
            val detectedDocRootDir = if (detectedDocRoot.isNotBlank()) File(projectDir, detectedDocRoot) else projectDir
            if (!File(detectedDocRootDir, detectedEntry).exists()) {
                AppLogger.i("PhpAppPreview", "根目录未找到入口文件，扫描子目录...")
                val phpSubDir = projectDir.listFiles()
                    ?.filter { it.isDirectory && it.name != "__MACOSX" && !it.name.startsWith("._") }
                    ?.firstOrNull { sub -> sub.listFiles()?.any { it.isFile && it.extension == "php" } == true }
                
                if (phpSubDir != null) {
                    AppLogger.i("PhpAppPreview", "找到 PHP 子目录: ${phpSubDir.name}")
                    actualProjectDir = phpSubDir
                    detectedFramework = phpAppRuntime.detectFramework(phpSubDir)
                    detectedDocRoot = phpAppRuntime.detectDocumentRoot(phpSubDir, detectedFramework)
                    detectedEntry = phpAppRuntime.detectEntryFile(phpSubDir, detectedDocRoot)
                }
            }
            
            AppLogger.i("PhpAppPreview", "自动检测: framework=$detectedFramework, docRoot='$detectedDocRoot', entry='$detectedEntry', projectDir=${actualProjectDir.name}")
            actualDocRoot = detectedDocRoot
            actualEntryFile = detectedEntry
        }
        
        // 4. 启动 PHP 服务器
        phpAppPreviewState = PhpAppPreviewState.StartingServer
        AppLogger.i("PhpAppPreview", "启动 PHP 服务器: docRoot='$actualDocRoot', entry='$actualEntryFile'")
        val port = phpAppRuntime.startServer(
            projectDir = actualProjectDir.absolutePath,
            documentRoot = actualDocRoot,
            entryFile = actualEntryFile,
            port = config.phpPort,
            envVars = config.envVars
        )
        
        if (port > 0) {
            val url = "http://127.0.0.1:$port/"
            AppLogger.i("PhpAppPreview", "PHP 服务器已启动: $url")
            phpAppPreviewState = PhpAppPreviewState.Ready(url)
            delay(200)  // 等待 WebView factory 完成
            webViewRef?.loadUrl(url)
        } else {
            AppLogger.e("PhpAppPreview", "PHP 服务器启动失败, port=$port, serverState=${phpAppRuntime.serverState.value}")
            val errorDetail = when (val state = phpAppRuntime.serverState.value) {
                is PhpAppRuntime.ServerState.Error -> state.message
                else -> Strings.phpAppServerError
            }
            phpAppPreviewState = PhpAppPreviewState.Error(errorDetail)
        }
    }
    
    // PHP 应用: 清理 PHP 服务器
    DisposableEffect(phpAppRuntime) {
        onDispose {
            phpAppRuntime.stopServer()
        }
    }
    
    // Python 应用预览流程
    LaunchedEffect(webApp, isActivated, isActivationChecked, pythonAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.PYTHON_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect
        
        val config = app.pythonAppConfig
        if (config == null) {
            AppLogger.e("PythonAppPreview", "pythonAppConfig 为 null")
            pythonAppPreviewState = PythonAppPreviewState.Error(Strings.pyProjectNotFound)
            return@LaunchedEffect
        }
        
        AppLogger.i("PythonAppPreview", "开始 Python 应用预览流程, appId=$appId, config=$config")
        pythonAppPreviewState = PythonAppPreviewState.Starting
        
        // 检查项目目录
        val projectId = config.projectId
        AppLogger.i("PythonAppPreview", "projectId='$projectId', framework='${config.framework}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("PythonAppPreview", "projectId 为空")
            pythonAppPreviewState = PythonAppPreviewState.Error(Strings.pyProjectNotFound)
            return@LaunchedEffect
        }
        val projectDir = pythonRuntime.getProjectDir(projectId)
        AppLogger.i("PythonAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            pythonAppPreviewState = PythonAppPreviewState.Error(Strings.pyProjectNotFound)
            return@LaunchedEffect
        }
        
        // 列出项目文件（调试）
        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("PythonAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }
        
        // 自动检测入口文件 — 处理 ZIP 导入嵌套目录的情况
        var actualProjectDir = projectDir
        var actualEntryFile = config.entryFile.ifBlank { "app.py" }
        var actualFramework = config.framework.ifBlank { "raw" }
        
        if (!File(actualProjectDir, actualEntryFile).exists()) {
            AppLogger.i("PythonAppPreview", "入口文件不存在: $actualEntryFile，尝试自动检测...")
            
            // 先在当前目录重新检测
            val detectedFramework = pythonRuntime.detectFramework(projectDir)
            val detectedEntry = pythonRuntime.detectEntryFile(projectDir, detectedFramework)
            
            if (File(projectDir, detectedEntry).exists()) {
                AppLogger.i("PythonAppPreview", "自动检测到: framework=$detectedFramework, entry=$detectedEntry")
                actualFramework = detectedFramework
                actualEntryFile = detectedEntry
            } else {
                // 扫描子目录（处理 ZIP 导入嵌套目录: project.zip/project-name/app.py）
                AppLogger.i("PythonAppPreview", "根目录未找到入口文件，扫描子目录...")
                val pySubDir = projectDir.listFiles()
                    ?.filter { it.isDirectory && it.name != "__MACOSX" && it.name != "__pycache__" && !it.name.startsWith("._") && it.name != "venv" && it.name != ".venv" && it.name != ".git" }
                    ?.firstOrNull { sub ->
                        sub.listFiles()?.any { it.isFile && it.extension == "py" } == true
                    }
                
                if (pySubDir != null) {
                    AppLogger.i("PythonAppPreview", "找到 Python 子目录: ${pySubDir.name}")
                    actualProjectDir = pySubDir
                    actualFramework = pythonRuntime.detectFramework(pySubDir)
                    actualEntryFile = pythonRuntime.detectEntryFile(pySubDir, actualFramework)
                    AppLogger.i("PythonAppPreview", "子目录检测: framework=$actualFramework, entry=$actualEntryFile")
                }
            }
        }
        
        AppLogger.i("PythonAppPreview", "最终配置: projectDir=${actualProjectDir.absolutePath}, framework=$actualFramework, entry=$actualEntryFile")
        
        // 查找可用的静态产物目录或项目根目录
        try {
            val candidates = listOf("dist", "build", "public", "static", "www", "templates", "")
            var docRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) actualProjectDir else File(actualProjectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("PythonAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    docRoot = candidate
                    AppLogger.i("PythonAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }
            
            if (docRoot != null) {
                val url = pythonHttpServer.start(docRoot)
                AppLogger.i("PythonAppPreview", "LocalHttpServer 已启动: $url")
                pythonAppPreviewState = PythonAppPreviewState.Ready(url)
                delay(200)
                webViewRef?.loadUrl(url)
            } else if (pythonRuntime.isPythonAvailable()) {
                // Python 运行时可用 — 启动实际的 Python 服务器
                AppLogger.i("PythonAppPreview", "Python 运行时可用，启动后端服务器")
                pythonAppPreviewState = PythonAppPreviewState.StartingServer
                
                val serverPort = pythonRuntime.startServer(
                    projectDir = actualProjectDir.absolutePath,
                    entryFile = actualEntryFile,
                    framework = actualFramework,
                    port = config.serverPort,
                    envVars = config.envVars,
                    installDeps = config.hasPipDeps
                )
                
                if (serverPort > 0) {
                    val serverUrl = "http://127.0.0.1:$serverPort"
                    AppLogger.i("PythonAppPreview", "Python 服务器已启动: $serverUrl")
                    pythonAppPreviewState = PythonAppPreviewState.Ready(serverUrl)
                    delay(200)
                    webViewRef?.loadUrl(serverUrl)
                } else {
                    AppLogger.e("PythonAppPreview", "Python 服务器启动失败，回退到预览模式")
                    // 回退到静态预览
                    val url = pythonHttpServer.start(actualProjectDir)
                    File(actualProjectDir, "_preview_.html").delete()
                    val previewHtml = pythonRuntime.generatePreviewHtml(
                        projectDir = actualProjectDir,
                        framework = actualFramework,
                        entryFile = actualEntryFile
                    )
                    val previewFile = File(actualProjectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    pythonAppPreviewState = PythonAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            } else {
                // Python 运行时不可用 — 生成静态预览页面
                AppLogger.w("PythonAppPreview", "Python 运行时不可用，生成项目预览页面")
                val url = pythonHttpServer.start(actualProjectDir)
                File(actualProjectDir, "_preview_.html").delete()
                
                val htmlFiles = actualProjectDir.walkTopDown().filter { it.extension == "html" && it.name != "_preview_.html" }.take(1).toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(actualProjectDir).path
                    val targetUrl = "$url/$relPath"
                    pythonAppPreviewState = PythonAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                } else {
                    val previewHtml = pythonRuntime.generatePreviewHtml(
                        projectDir = actualProjectDir,
                        framework = actualFramework,
                        entryFile = actualEntryFile
                    )
                    val previewFile = File(actualProjectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    pythonAppPreviewState = PythonAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("PythonAppPreview", "启动预览失败", e)
            pythonAppPreviewState = PythonAppPreviewState.Error(e.message ?: Strings.pyPreviewFailed)
        }
    }
    
    // Python 应用: 清理 HTTP 服务器和 Python 进程
    DisposableEffect(pythonHttpServer) {
        onDispose {
            pythonHttpServer.stop()
            pythonRuntime.stopServer()
        }
    }
    
    // Node.js 应用预览流程
    LaunchedEffect(webApp, isActivated, isActivationChecked, nodeJsAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.NODEJS_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect
        
        val config = app.nodejsConfig
        if (config == null) {
            AppLogger.e("NodeJsAppPreview", "nodejsConfig 为 null")
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(Strings.nodeProjectNotFound)
            return@LaunchedEffect
        }
        
        AppLogger.i("NodeJsAppPreview", "开始 Node.js 应用预览流程, appId=$appId, config=$config")
        nodeJsAppPreviewState = NodeJsAppPreviewState.Starting
        
        // 检查项目目录
        val projectId = config.projectId
        AppLogger.i("NodeJsAppPreview", "projectId='$projectId', framework='${config.framework}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("NodeJsAppPreview", "projectId 为空")
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(Strings.nodeProjectNotFound)
            return@LaunchedEffect
        }
        val projectDir = nodeRuntime.getProjectDir(projectId)
        AppLogger.i("NodeJsAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(Strings.nodeProjectNotFound)
            return@LaunchedEffect
        }
        
        // 列出项目文件（调试）
        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("NodeJsAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }
        
        // 查找可用的静态产物目录或项目根目录
        try {
            val candidates = listOf("dist", "build", "public", "static", "www", "")
            var foundDocRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) projectDir else File(projectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("NodeJsAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    foundDocRoot = candidate
                    AppLogger.i("NodeJsAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }
            
            val docRoot = foundDocRoot
            if (docRoot != null) {
                val url = nodeHttpServer.start(docRoot)
                AppLogger.i("NodeJsAppPreview", "LocalHttpServer 已启动: $url")
                nodeJsAppPreviewState = NodeJsAppPreviewState.Ready(url)
                delay(200)
                webViewRef?.loadUrl(url)
            } else {
                // 没有找到 index.html，尝试直接在项目根启动 HTTP 服务器
                AppLogger.w("NodeJsAppPreview", "未找到 index.html，尝试在项目根启动 HTTP 服务器")
                val url = nodeHttpServer.start(projectDir)
                AppLogger.i("NodeJsAppPreview", "LocalHttpServer 在项目根启动: $url")
                
                // 删除旧的预览缓存，确保重新生成
                File(projectDir, "_preview_.html").delete()
                
                // 检查是否有任何 HTML 文件（排除预览缓存）
                val htmlFiles = projectDir.walkTopDown().filter { it.extension == "html" && it.name != "_preview_.html" }.take(1).toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(projectDir).path
                    val targetUrl = "$url/$relPath"
                    AppLogger.i("NodeJsAppPreview", "找到 HTML 文件: $relPath, URL=$targetUrl")
                    nodeJsAppPreviewState = NodeJsAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                } else {
                    // 没有静态 HTML 文件 — 生成项目预览页面（展示源码和项目信息）
                    AppLogger.i("NodeJsAppPreview", "无静态 HTML，生成项目预览页面")
                    val previewHtml = nodeRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        entryFile = config.entryFile
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    AppLogger.i("NodeJsAppPreview", "预览页面已生成: $targetUrl")
                    nodeJsAppPreviewState = NodeJsAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("NodeJsAppPreview", "启动预览失败", e)
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(e.message ?: Strings.nodePreviewFailed)
        }
    }
    
    // Node.js 应用: 清理 HTTP 服务器
    DisposableEffect(nodeHttpServer) {
        onDispose {
            nodeHttpServer.stop()
        }
    }
    
    // Go 应用预览流程
    LaunchedEffect(webApp, isActivated, isActivationChecked, goAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.GO_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect
        
        val config = app.goAppConfig
        if (config == null) {
            AppLogger.e("GoAppPreview", "goAppConfig 为 null")
            goAppPreviewState = GoAppPreviewState.Error(Strings.goProjectNotFound)
            return@LaunchedEffect
        }
        
        AppLogger.i("GoAppPreview", "开始 Go 应用预览流程, appId=$appId, config=$config")
        goAppPreviewState = GoAppPreviewState.Starting
        
        // 检查项目目录
        val projectId = config.projectId
        AppLogger.i("GoAppPreview", "projectId='$projectId', framework='${config.framework}', binary='${config.binaryName}'")
        if (projectId.isBlank()) {
            AppLogger.e("GoAppPreview", "projectId 为空")
            goAppPreviewState = GoAppPreviewState.Error(Strings.goProjectNotFound)
            return@LaunchedEffect
        }
        val projectDir = goRuntime.getProjectDir(projectId)
        AppLogger.i("GoAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            goAppPreviewState = GoAppPreviewState.Error(Strings.goProjectNotFound)
            return@LaunchedEffect
        }
        
        // 列出项目文件（调试）
        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("GoAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }
        
        // 查找可用的静态产物目录或项目根目录
        try {
            val candidates = listOf("dist", "build", "public", "static", "web", "www", "")
            var foundDocRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) projectDir else File(projectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("GoAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    foundDocRoot = candidate
                    AppLogger.i("GoAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }
            
            val docRoot = foundDocRoot
            if (docRoot != null) {
                val url = goHttpServer.start(docRoot)
                AppLogger.i("GoAppPreview", "LocalHttpServer 已启动: $url")
                goAppPreviewState = GoAppPreviewState.Ready(url)
                delay(200)
                webViewRef?.loadUrl(url)
            } else if (config.binaryName.isNotBlank() && goRuntime.detectBinary(projectDir) != null) {
                // 有预编译二进制 — 启动实际的 Go 服务器
                AppLogger.i("GoAppPreview", "检测到 Go 二进制，启动后端服务器")
                goAppPreviewState = GoAppPreviewState.StartingServer
                
                val serverPort = goRuntime.startServer(
                    projectDir = projectDir.absolutePath,
                    binaryName = config.binaryName,
                    port = config.serverPort,
                    envVars = config.envVars
                )
                
                if (serverPort > 0) {
                    val serverUrl = "http://127.0.0.1:$serverPort"
                    AppLogger.i("GoAppPreview", "Go 服务器已启动: $serverUrl")
                    goAppPreviewState = GoAppPreviewState.Ready(serverUrl)
                    delay(200)
                    webViewRef?.loadUrl(serverUrl)
                } else {
                    AppLogger.e("GoAppPreview", "Go 服务器启动失败，回退到预览模式")
                    val url = goHttpServer.start(projectDir)
                    File(projectDir, "_preview_.html").delete()
                    val previewHtml = goRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        binaryName = config.binaryName
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    goAppPreviewState = GoAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            } else {
                // 无二进制或无法执行 — 生成静态预览页面
                AppLogger.w("GoAppPreview", "无可执行二进制，生成项目预览页面")
                val url = goHttpServer.start(projectDir)
                File(projectDir, "_preview_.html").delete()
                
                val htmlFiles = projectDir.walkTopDown().filter { it.extension == "html" && it.name != "_preview_.html" }.take(1).toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(projectDir).path
                    val targetUrl = "$url/$relPath"
                    goAppPreviewState = GoAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                } else {
                    val previewHtml = goRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        binaryName = config.binaryName
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    goAppPreviewState = GoAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("GoAppPreview", "启动预览失败", e)
            goAppPreviewState = GoAppPreviewState.Error(e.message ?: Strings.goPreviewFailed)
        }
    }
    
    // Go 应用: 清理 HTTP 服务器和 Go 进程
    DisposableEffect(goHttpServer) {
        onDispose {
            goHttpServer.stop()
            goRuntime.stopServer()
        }
    }

    fun scheduleStrictHostFallbackProbe(url: String?, source: String, delayMs: Long) {
        if (!STRICT_HOST_AUTO_EXTERNAL_FALLBACK_ENABLED) return
        if (strictHostFallbackTriggered || !shouldSkipLongPressEnhancer(url)) return
        val expectedUrl = url

        webViewRef?.postDelayed({
            val activeWebView = webViewRef ?: return@postDelayed
            if (strictHostFallbackTriggered) return@postDelayed

            val current = activeWebView.url
            if (expectedUrl != null && expectedUrl != current) return@postDelayed

            val probeScript = """
                (function() {
                    try {
                        var body = document.body;
                        var root = document.documentElement;
                        if (!body) return JSON.stringify({blank:true, reason:'no-body'});
                        var text = (body.innerText || '').replace(/\s+/g, '');
                        var textLength = text.length;
                        var height = Math.max(body.scrollHeight || 0, root ? (root.scrollHeight || 0) : 0);
                        var nodeCount = body.querySelectorAll('*').length;
                        var videoCount = document.querySelectorAll('video').length;
                        var imgCount = document.images ? document.images.length : 0;
                        var blank = height < 900 && textLength < 80 && nodeCount < 120 && videoCount === 0 && imgCount < 5;
                        return JSON.stringify({
                            blank: blank,
                            height: height,
                            textLength: textLength,
                            nodeCount: nodeCount,
                            videoCount: videoCount,
                            imgCount: imgCount
                        });
                    } catch (e) {
                        return JSON.stringify({blank:false, error:String(e)});
                    }
                })();
            """.trimIndent()

            activeWebView.evaluateJavascript(probeScript) { raw ->
                if (strictHostFallbackTriggered) return@evaluateJavascript
                val decoded = decodeEvaluateJavascriptString(raw)
                if (!shouldFallbackToExternalForStrictHost(decoded)) return@evaluateJavascript
                strictHostFallbackTriggered = true
                AppLogger.w("WebViewActivity", "Strict host blank-page probe ($source) triggered external fallback: $expectedUrl metrics=$decoded")
                val fallbackUrl = expectedUrl ?: activeWebView.url.orEmpty()
                if (fallbackUrl.isNotBlank()) {
                    val safeUrl = normalizeExternalUrlForIntent(fallbackUrl)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }, delayMs)
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
                // 使用系统下载管理器下载到 Download 文件夹
                // Media文件会自动保存到相册
                DownloadHelper.handleDownload(
                    context = context,
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                    scope = scope,
                    onBlobDownload = { blobUrl, filename ->
                        val safeBlobUrl = org.json.JSONObject.quote(blobUrl)
                        val safeFilename = org.json.JSONObject.quote(filename)
                        // 通过 WebView 执行 JS 来处理 Blob/Data URL 下载
                        // 大文件使用分块处理避免 DOM 冻结
                        webViewRef?.evaluateJavascript("""
                            (function() {
                                try {
                                    const blobUrl = $safeBlobUrl;
                                    const filename = $safeFilename;
                                    const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024;
                                    const CHUNK_SIZE = 1024 * 1024;
                                    
                                    function uint8ToBase64(u8) {
                                        const S = 8192; const p = [];
                                        for (let i = 0; i < u8.length; i += S) p.push(String.fromCharCode.apply(null, u8.subarray(i, i + S)));
                                        return btoa(p.join(''));
                                    }
                                    
                                    function processChunked(blob, fname) {
                                        const mimeType = blob.type || 'application/octet-stream';
                                        if (!window.AndroidDownload || !window.AndroidDownload.startChunkedDownload) {
                                            processSmall(blob, fname); return;
                                        }
                                        const did = window.AndroidDownload.startChunkedDownload(fname, mimeType, blob.size);
                                        let off = 0, ci = 0; const tc = Math.ceil(blob.size / CHUNK_SIZE);
                                        function next() {
                                            if (off >= blob.size) { window.AndroidDownload.finishChunkedDownload(did); return; }
                                            blob.slice(off, off + CHUNK_SIZE).arrayBuffer().then(function(ab) {
                                                window.AndroidDownload.appendChunk(did, uint8ToBase64(new Uint8Array(ab)), ci, tc);
                                                off += CHUNK_SIZE; ci++;
                                                setTimeout(next, 0);
                                            });
                                        }
                                        next();
                                    }
                                    
                                    function processSmall(blob, fname) {
                                        const reader = new FileReader();
                                        reader.onloadend = function() {
                                            const base64Data = reader.result.split(',')[1];
                                            const mimeType = blob.type || 'application/octet-stream';
                                            if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                                window.AndroidDownload.saveBase64File(base64Data, fname, mimeType);
                                            }
                                        };
                                        reader.readAsDataURL(blob);
                                    }
                                    
                                    if (blobUrl.startsWith('data:')) {
                                        const parts = blobUrl.split(',');
                                        const meta = parts[0];
                                        const base64Data = parts[1];
                                        const mimeMatch = meta.match(/data:([^;]+)/);
                                        const mimeType = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
                                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                            window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                                        }
                                    } else if (blobUrl.startsWith('blob:')) {
                                        fetch(blobUrl)
                                            .then(function(r) { return r.blob(); })
                                            .then(function(blob) {
                                                if (blob.size > LARGE_FILE_THRESHOLD) {
                                                    processChunked(blob, filename);
                                                } else {
                                                    processSmall(blob, filename);
                                                }
                                            })
                                            .catch(function(err) {
                                                console.error('[DownloadHelper] Blob fetch failed:', err);
                                                if (window.AndroidDownload && window.AndroidDownload.showToast) {
                                                    window.AndroidDownload.showToast('${Strings.downloadFailedWithReason}' + err.message);
                                                }
                                            });
                                    }
                                } catch(e) {
                                    console.error('[DownloadHelper] Error:', e);
                                }
                            })();
                        """.trimIndent(), null)
                    }
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
