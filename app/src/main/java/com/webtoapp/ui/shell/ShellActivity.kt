package com.webtoapp.ui.shell

import com.webtoapp.core.logging.AppLogger
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.ShellTheme
import com.webtoapp.ui.theme.LocalIsDarkTheme
import com.webtoapp.core.webview.TranslateBridge
import com.webtoapp.data.model.KeyboardAdjustMode
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.floatingwindow.FloatingWindowService
import com.webtoapp.core.shell.CloudSdkManager
import com.webtoapp.core.shell.ShellRuntimeServices
import com.webtoapp.ui.shared.WindowHelper

/**
 * Shell Activity- for WebApp run
 * from app_config. json configanddisplay WebView
 */
class ShellActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    
    // Deep link URL from intent
    private var deepLinkUrl = mutableStateOf<String?>(null)
    
    // handle( ShellPermissionDelegate. kt)
    val permissionDelegate = ShellPermissionDelegate(this)

    private var immersiveFullscreenEnabled: Boolean = false
    private var showStatusBarInFullscreen: Boolean = false  // Fullscreenmode displaystatus bar
    private var showNavigationBarInFullscreen: Boolean = false  // Fullscreenmode display
    private var translateBridge: TranslateBridge? = null
    
    // Video
    private var originalOrientationBeforeFullscreen: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    
    // Status barconfig
    private var statusBarColorMode: String = "THEME"
    private var statusBarCustomColor: String? = null
    private var statusBarDarkIcons: Boolean? = null
    private var statusBarBackgroundType: String = "COLOR"
    private var statusBarBackgroundImage: String? = null
    private var statusBarBackgroundAlpha: Float = 1.0f
    private var statusBarHeightDp: Int = 0
    // Status bar modeconfig
    private var statusBarColorModeDark: String = "THEME"
    private var statusBarCustomColorDark: String? = null
    private var statusBarDarkIconsDark: Boolean? = null
    private var statusBarBackgroundTypeDark: String = "COLOR"
    private var statusBarBackgroundImageDark: String? = null
    private var statusBarBackgroundAlphaDark: Float = 1.0f
    private var forceHideSystemUi: Boolean = false
    // current state( from Compose sync, for onWindowFocusChanged Activity)
    private var currentIsDarkTheme: Boolean = false
    private var keyboardAdjustMode: KeyboardAdjustMode = KeyboardAdjustMode.RESIZE  // keyboard mode
    private var forcedRunConfig: ForcedRunConfig? = null
    private val forcedRunManager by lazy { ForcedRunManager.getInstance(this) }
    
    // SDK manager( export APK run)
    internal var cloudSdkManager: CloudSdkManager? = null

    private fun applyStatusBarColor(
        colorMode: String,
        customColor: String?,
        darkIcons: Boolean?,
        isDarkTheme: Boolean
    ) = WindowHelper.applyStatusBarColor(this, colorMode, customColor, darkIcons, isDarkTheme)

    private fun applyImmersiveFullscreen(enabled: Boolean, hideNavBar: Boolean? = null, isDarkTheme: Boolean = currentIsDarkTheme) {
        val shouldHideNavBar = hideNavBar ?: !showNavigationBarInFullscreen
        // / mode status barconfig
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
            forceHideSystemUi = forceHideSystemUi,
            statusBarColorMode = effectiveColorMode,
            statusBarCustomColor = effectiveCustomColor,
            statusBarDarkIcons = effectiveDarkIcons,
            statusBarBgType = effectiveBgType,
            keyboardAdjustMode = keyboardAdjustMode,
            tag = "ShellActivity"
        )
    }

    fun onForcedRunStateChanged(active: Boolean, config: ForcedRunConfig?) {
        forcedRunConfig = config
        forceHideSystemUi = active && config?.blockSystemUI == true
        
        AppLogger.d("ShellActivity", "强制运行状态变化: active=$active, protection=${config?.protectionLevel}")
        
        if (active) {
            // Note
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Lock Task Mode( , user)
            try {
                startLockTask()
            } catch (e: Exception) {
                AppLogger.w("ShellActivity", "startLockTask failed (expected without device admin)", e)
            }
            
            // ForcedRunManager AccessibilityService GuardService
            // startLockTask
            
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            try {
                stopLockTask()
            } catch (e: Exception) {
                AppLogger.w("ShellActivity", "stopLockTask failed", e)
            }
        }
        
        applyImmersiveFullscreen(customView != null || immersiveFullscreenEnabled || forceHideSystemUi)
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
        val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
        
        // Check
        if (hardwareController.isBlockVolumeKeys) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            }
        }
        
        // Check( : Activity intercept)
        if (hardwareController.isBlockPowerKey && event.keyCode == KeyEvent.KEYCODE_POWER) {
            return true
        }
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (forcedRunManager.handleKeyEvent(event.keyCode)) {
                return true
            }
        }

        if (shouldForwardKeyToWebView(event) && webView?.dispatchKeyEvent(event) == true) {
            return true
        }

        return super.dispatchKeyEvent(event)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
        
        // Note
        if (hardwareController.isBlockVolumeKeys) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Check
        val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
        if (hardwareController.isBlockTouch) {
            // Note
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
    
    // API: ShellPermissionDelegate
    fun handlePermissionRequest(request: PermissionRequest) = permissionDelegate.handlePermissionRequest(request)
    fun handleGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?) = permissionDelegate.handleGeolocationPermission(origin, callback)
    
    fun handleDownloadWithPermission(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) = permissionDelegate.handleDownloadWithPermission(url, userAgent, contentDisposition, mimeType, contentLength, webView)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize system( initialize)
        ShellActivityInit.initLogger(this)
        
        // Enable display( content system area)
        try {
            enableEdgeToEdge()
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "enableEdgeToEdge 成功")
        } catch (e: Exception) {
            AppLogger.w("ShellActivity", "enableEdgeToEdge failed", e)
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "enableEdgeToEdge 失败", e)
        }
        
        super.onCreate(savedInstanceState)

        val config = ShellRuntimeServices.shellMode.getConfig()
        if (config == null) {
            AppLogger.e("ShellActivity", "配置加载失败，无法启动应用")
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "配置加载失败，无法启动应用")
            Toast.makeText(this, Strings.appConfigLoadFailed, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "配置加载成功: ${config.appName}")
        AppLogger.d("ShellActivity", "WebView UA config from shell: userAgentMode=${config.webViewConfig.userAgentMode}, customUserAgent=${config.webViewConfig.customUserAgent}, userAgent=${config.webViewConfig.userAgent}")
        
        // initialize SDK( updatecheck, announcement, remoteconfig, )
        if (config.cloudSdkConfig.isValid()) {
            cloudSdkManager = CloudSdkManager(this, config.cloudSdkConfig).also {
                it.initialize(this)
                AppLogger.i("ShellActivity", "Cloud SDK initialized for project: ${config.cloudSdkConfig.projectKey}")
            }
        }
        
        forcedRunConfig = config.forcedRunConfig
        
        // config
        com.webtoapp.core.shell.ShellLogger.logFeature("Config", "加载配置", buildString {
            append("强制运行=${config.forcedRunConfig?.enabled ?: false}, ")
            append("后台运行=${config.backgroundRunEnabled}, ")
            append("独立环境=${config.isolationEnabled}")
        })
        
        // initialize system( ShellActivityInit. kt)
        ShellActivityInit.initForcedRunManager(this, config, forcedRunManager, ::onForcedRunStateChanged)
        ShellActivityInit.initAutoStart(this, config)
        ShellActivityInit.initIsolation(this, config)
        ShellActivityInit.initBackgroundService(this, config)
        ShellActivityInit.setTaskDescription(this, config.appName)
        
        // Request( Android 13+)
        try {
            permissionDelegate.requestNotificationPermissionIfNeeded()
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "通知权限请求完成")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "通知权限请求失败", e)
        }
        
        // status barconfig
        com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "开始读取状态栏配置")
        statusBarColorMode = config.webViewConfig.statusBarColorMode
        statusBarCustomColor = config.webViewConfig.statusBarColor
        statusBarDarkIcons = config.webViewConfig.statusBarDarkIcons
        statusBarBackgroundType = config.webViewConfig.statusBarBackgroundType
        statusBarBackgroundImage = config.webViewConfig.statusBarBackgroundImage
        statusBarBackgroundAlpha = config.webViewConfig.statusBarBackgroundAlpha
        statusBarHeightDp = config.webViewConfig.statusBarHeightDp
        // modestatus barconfig
        statusBarColorModeDark = config.webViewConfig.statusBarColorModeDark
        statusBarCustomColorDark = config.webViewConfig.statusBarColorDark
        statusBarDarkIconsDark = config.webViewConfig.statusBarDarkIconsDark
        statusBarBackgroundTypeDark = config.webViewConfig.statusBarBackgroundTypeDark
        statusBarBackgroundImageDark = config.webViewConfig.statusBarBackgroundImageDark
        statusBarBackgroundAlphaDark = config.webViewConfig.statusBarBackgroundAlphaDark
        showStatusBarInFullscreen = config.webViewConfig.showStatusBarInFullscreen
        showNavigationBarInFullscreen = config.webViewConfig.showNavigationBarInFullscreen
        // keyboard mode
        keyboardAdjustMode = try {
            KeyboardAdjustMode.valueOf(config.webViewConfig.keyboardAdjustMode)
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "键盘调整模式解析失败: ${config.webViewConfig.keyboardAdjustMode}, 使用默认值 RESIZE")
            KeyboardAdjustMode.RESIZE
        }

        // state( applyImmersiveFullscreen onWindowFocusChanged)
        currentIsDarkTheme = when (config.darkMode.uppercase()) {
            "LIGHT" -> false
            "DARK" -> true
            else -> {
                // SYSTEM: systemsettings
                val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }

        // config mode
        // hideToolbar=true( hidestatus bar) , displaystatus bar
        immersiveFullscreenEnabled = config.webViewConfig.hideToolbar
        try {
            applyImmersiveFullscreen(immersiveFullscreenEnabled, isDarkTheme = currentIsDarkTheme)
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "沉浸式全屏模式: $immersiveFullscreenEnabled, isDark=$currentIsDarkTheme")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "应用沉浸式全屏失败", e)
        }
        
        // ( support mode)
        val shellAwakeMode = config.webViewConfig.screenAwakeMode.uppercase()
        when (shellAwakeMode) {
            "ALWAYS" -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕常亮: 始终常亮模式")
            }
            "TIMED" -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val timeoutMinutes = config.webViewConfig.screenAwakeTimeoutMinutes
                val timeoutMs = (if (timeoutMinutes > 0) timeoutMinutes else 30) * 60 * 1000L
                android.os.Handler(mainLooper).postDelayed({
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕常亮: 定时 ${timeoutMinutes} 分钟已到，恢复系统息屏")
                }, timeoutMs)
                com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕常亮: 定时模式 ${timeoutMinutes} 分钟")
            }
            else -> {
                // OFF or: keepScreenOn
                if (config.webViewConfig.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕常亮: 已启用（向后兼容模式）")
                }
            }
        }
        
        // Note
        val shellBrightness = config.webViewConfig.screenBrightness
        if (shellBrightness in 0..100) {
            val lp = window.attributes
            lp.screenBrightness = shellBrightness / 100f
            window.attributes = lp
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕亮度: ${shellBrightness}%")
        }
        
        // floating windowmode
        val floatingWindowConfig = config.webViewConfig.floatingWindowConfig
        if (floatingWindowConfig.enabled) {
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "悬浮窗配置: size=${floatingWindowConfig.windowSizePercent}%, opacity=${floatingWindowConfig.opacity}%")
            if (FloatingWindowService.canDrawOverlays(this)) {
                // Note
                val fwConfig = com.webtoapp.data.model.FloatingWindowConfig(
                    enabled = true,
                    windowSizePercent = floatingWindowConfig.windowSizePercent,
                    opacity = floatingWindowConfig.opacity,
                    showTitleBar = floatingWindowConfig.showTitleBar,
                    startMinimized = floatingWindowConfig.startMinimized,
                    rememberPosition = floatingWindowConfig.rememberPosition
                )
                val intent = Intent(this, FloatingWindowService::class.java).apply {
                    putExtra(FloatingWindowService.EXTRA_ACTION, FloatingWindowService.ACTION_SHOW)
                    putExtra(FloatingWindowService.EXTRA_CONFIG, com.webtoapp.util.GsonProvider.gson.toJson(fwConfig))
                    putExtra(FloatingWindowService.EXTRA_URL, config.targetUrl)
                    putExtra(FloatingWindowService.EXTRA_APP_NAME, config.appName)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "悬浮窗服务已启动")
            } else {
                // , user
                com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "悬浮窗权限未授予，引导用户授权")
                Toast.makeText(this, Strings.floatingWindowPermissionRequired, Toast.LENGTH_LONG).show()
                FloatingWindowService.requestOverlayPermission(this)
            }
        }

        // Check deep link intent data（with domain validation）
        val intentUrl = intent?.data?.toString()
        if (!intentUrl.isNullOrBlank() && intent?.action == Intent.ACTION_VIEW) {
            val safeUrl = normalizeShellTargetUrlForSecurity(intentUrl)
            val validatedUrl = if (config.deepLinkEnabled) {
                validateDeepLinkUrl(safeUrl, config.deepLinkHosts, config.targetUrl)
            } else safeUrl
            deepLinkUrl.value = validatedUrl
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "收到 Deep Link: $validatedUrl (原始: $intentUrl)")
        }
        
        com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "setContent 开始，主题=${config.themeType}")
        
        setContent {
            ShellTheme(
                themeTypeName = config.themeType,
                darkModeSetting = config.darkMode
            ) {
                // Getcurrent state
                val isDarkTheme = com.webtoapp.ui.theme.LocalIsDarkTheme.current

                // sync state Activity( onWindowFocusChanged)
                SideEffect {
                    currentIsDarkTheme = isDarkTheme
                }

                // when updatestatus barcolor( / modeselect config)
                LaunchedEffect(isDarkTheme, statusBarColorMode, statusBarColorModeDark) {
                    if (!immersiveFullscreenEnabled) {
                        val effectiveColorMode = if (isDarkTheme) statusBarColorModeDark else statusBarColorMode
                        val effectiveCustomColor = if (isDarkTheme) statusBarCustomColorDark else statusBarCustomColor
                        val effectiveDarkIcons = if (isDarkTheme) statusBarDarkIconsDark else statusBarDarkIcons
                        applyStatusBarColor(effectiveColorMode, effectiveCustomColor, effectiveDarkIcons, isDarkTheme)
                    }
                }

                ShellScreen(
                    config = config,
                    deepLinkUrl = deepLinkUrl.value,
                    onWebViewCreated = { wv ->
                        try {
                            webView = wv
                            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "WebView 创建成功")
                            // Note
                            translateBridge = TranslateBridge(wv, lifecycleScope)
                            wv.addJavascriptInterface(translateBridge!!, TranslateBridge.JS_INTERFACE_NAME)
                            // download( support Blob/Data URL download)
                            val downloadBridge = com.webtoapp.core.webview.DownloadBridge(this@ShellActivity, lifecycleScope)
                            wv.addJavascriptInterface(downloadBridge, com.webtoapp.core.webview.DownloadBridge.JS_INTERFACE_NAME)
                            // ( modulecall)
                            val nativeBridge = com.webtoapp.core.webview.NativeBridge(this@ShellActivity, lifecycleScope)
                            wv.addJavascriptInterface(nativeBridge, com.webtoapp.core.webview.NativeBridge.JS_INTERFACE_NAME)
                            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "JS 桥接接口注册完成")
                        } catch (e: Exception) {
                            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "WebView 初始化失败", e)
                        }
                    },
                    onFileChooser = { callback, params ->
                        permissionDelegate.handleFileChooser(callback, params)
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
                    },
                    onForcedRunStateChanged = { active, forcedConfig ->
                        onForcedRunStateChanged(active, forcedConfig)
                    },
                    // Status barconfig( / modeselect config)
                    statusBarBackgroundType = if (isDarkTheme) statusBarBackgroundTypeDark else statusBarBackgroundType,
                    statusBarBackgroundColor = if (isDarkTheme) statusBarCustomColorDark else statusBarCustomColor,
                    statusBarBackgroundImage = if (isDarkTheme) statusBarBackgroundImageDark else statusBarBackgroundImage,
                    statusBarBackgroundAlpha = if (isDarkTheme) statusBarBackgroundAlphaDark else statusBarBackgroundAlpha,
                    statusBarHeightDp = statusBarHeightDp
                )
            }
        }

        // back handle( ShellActivityInit. kt)
        onBackPressedDispatcher.addCallback(this, ShellActivityInit.createBackPressedCallback(
            activity = this,
            forcedRunManager = forcedRunManager,
            getCustomView = { customView },
            getWebView = { webView },
            hideCustomView = ::hideCustomView
        ))
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (customView != null || immersiveFullscreenEnabled || forceHideSystemUi) {
                applyImmersiveFullscreen(true, isDarkTheme = currentIsDarkTheme)
            } else {
                // mode: appstatus barcolor( / mode)
                val effectiveColorMode = if (currentIsDarkTheme) statusBarColorModeDark else statusBarColorMode
                val effectiveCustomColor = if (currentIsDarkTheme) statusBarCustomColorDark else statusBarCustomColor
                val effectiveDarkIcons = if (currentIsDarkTheme) statusBarDarkIconsDark else statusBarDarkIcons
                applyStatusBarColor(effectiveColorMode, effectiveCustomColor, effectiveDarkIcons, currentIsDarkTheme)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val url = intent?.data?.toString()
        if (!url.isNullOrBlank() && intent?.action == Intent.ACTION_VIEW) {
            val safeUrl = normalizeShellTargetUrlForSecurity(url)
            // verify Deep Link
            val config = ShellRuntimeServices.shellMode.getConfig()
            val validatedUrl = if (config?.deepLinkEnabled == true) {
                validateDeepLinkUrl(safeUrl, config.deepLinkHosts, config.targetUrl)
            } else safeUrl
            deepLinkUrl.value = validatedUrl
            // Directly load URL in existing WebView
            webView?.loadUrl(validatedUrl)
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "onNewIntent Deep Link: $validatedUrl (原始: $url)")
        }
    }
    
    override fun onResume() {
        super.onResume()
        webView?.onResume()
        webView?.resumeTimers()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onResume - WebView resumed")
    }
    
    override fun onPause() {
        super.onPause()
        // WebView JS, CPU
        webView?.onPause()
        webView?.pauseTimers()
        // Persist cookies when app goes to background
        android.webkit.CookieManager.getInstance().flush()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onPause - WebView paused, cookies flushed")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            webView?.clearCache(false)
            com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "Memory pressure (level=$level), cleared WebView cache")
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            webView?.freeMemory()
            System.gc()
            com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "Critical memory pressure, freed WebView memory")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        webView?.clearCache(false)
        webView?.freeMemory()
        System.gc()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "Low memory, cleared cache and freed WebView memory")
    }

    override fun onDestroy() {
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onDestroy")
        
        // SDK
        cloudSdkManager?.destroy()
        cloudSdkManager = null
        
        // Persist cookies & WebStorage before destroying WebView
        android.webkit.CookieManager.getInstance().flush()
        // WebView: load, View,
        webView?.let { wv ->
            wv.stopLoading()
            // about: blank
            // destroy loadUrl( "about: blank") WebView switch origin,
            // Android version andmapcurrent localStorage, H5
            wv.onPause()
            wv.pauseTimers()
            wv.webChromeClient = null
            // from View,
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
        webView = null
        super.onDestroy()
    }
}

// ShellScreen composable ShellScreen. kt
