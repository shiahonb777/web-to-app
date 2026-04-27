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
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.ShellTheme
import com.webtoapp.ui.theme.LocalIsDarkTheme
import com.webtoapp.core.webview.TranslateBridge
import com.webtoapp.data.model.KeyboardAdjustMode
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.floatingwindow.FloatingWindowService
import com.webtoapp.core.shell.CloudSdkManager
import com.webtoapp.ui.shared.WindowHelper

/**
 * Shell Activity - 用于独立 WebApp 运行
 * 从 app_config.json 读取配置并显示 WebView
 */
class ShellActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    
    // Deep link URL from intent
    private var deepLinkUrl = mutableStateOf<String?>(null)
    
    // 权限处理委托（逻辑已提取到 ShellPermissionDelegate.kt）
    val permissionDelegate = ShellPermissionDelegate(this)

    private var immersiveFullscreenEnabled: Boolean = false
    private var showStatusBarInFullscreen: Boolean = false  // Fullscreen模式下是否显示状态栏
    private var showNavigationBarInFullscreen: Boolean = false  // Fullscreen模式下是否显示导航栏
    private var translateBridge: TranslateBridge? = null
    
    // Video全屏前的屏幕方向
    private var originalOrientationBeforeFullscreen: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    
    // Status bar配置缓存
    private var statusBarColorMode: String = "THEME"
    private var statusBarCustomColor: String? = null
    private var statusBarDarkIcons: Boolean? = null
    private var statusBarBackgroundType: String = "COLOR"
    private var statusBarBackgroundImage: String? = null
    private var statusBarBackgroundAlpha: Float = 1.0f
    private var statusBarHeightDp: Int = 0
    // Dark mode status bar config cache
    private var statusBarColorModeDark: String = "THEME"
    private var statusBarCustomColorDark: String? = null
    private var statusBarDarkIconsDark: Boolean? = null
    private var statusBarBackgroundTypeDark: String = "COLOR"
    private var statusBarBackgroundImageDark: String? = null
    private var statusBarBackgroundAlphaDark: Float = 1.0f
    private var currentIsDarkTheme: Boolean = false
    private var forceHideSystemUi: Boolean = false
    private var keyboardAdjustMode: KeyboardAdjustMode = KeyboardAdjustMode.RESIZE  // 键盘调整模式
    private var forcedRunConfig: ForcedRunConfig? = null
    private val forcedRunManager by lazy { ForcedRunManager.getInstance(this) }
    
    // 悬浮窗模式：等待权限授予后重试
    private var pendingFloatingWindowLaunch = false

    // WebView 状态保存（后台返回时不重新加载页面）
    private var webViewStateBundle: Bundle? = null
    
    // 云 SDK 管理器（导出 APK 内的云服务运行时）
    internal var cloudSdkManager: CloudSdkManager? = null

    private fun applyStatusBarColor(
        colorMode: String,
        customColor: String?,
        darkIcons: Boolean?,
        isDarkTheme: Boolean
    ) = WindowHelper.applyStatusBarColor(this, colorMode, customColor, darkIcons, isDarkTheme)

    private fun applyImmersiveFullscreen(enabled: Boolean, hideNavBar: Boolean? = null, isDarkTheme: Boolean = false) {
        val shouldHideNavBar = hideNavBar ?: !showNavigationBarInFullscreen
        WindowHelper.applyImmersiveFullscreen(
            activity = this,
            enabled = enabled,
            hideNavBar = shouldHideNavBar,
            isDarkTheme = isDarkTheme,
            showStatusBar = showStatusBarInFullscreen,
            forceHideSystemUi = forceHideSystemUi,
            statusBarColorMode = statusBarColorMode,
            statusBarCustomColor = statusBarCustomColor,
            statusBarDarkIcons = statusBarDarkIcons,
            statusBarBgType = statusBarBackgroundType,
            keyboardAdjustMode = keyboardAdjustMode,
            tag = "ShellActivity"
        )
    }

    fun onForcedRunStateChanged(active: Boolean, config: ForcedRunConfig?) {
        forcedRunConfig = config
        forceHideSystemUi = active && config?.blockSystemUI == true
        
        AppLogger.d("ShellActivity", "强制运行状态变化: active=$active, protection=${config?.protectionLevel}")
        
        if (active) {
            // 保持屏幕常亮
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // 尝试 Lock Task Mode（作为额外防护层，可能需要用户确认）
            try {
                startLockTask()
            } catch (e: Exception) {
                AppLogger.w("ShellActivity", "startLockTask failed (expected without device admin)", e)
            }
            
            // 注意：真正的防护由 ForcedRunManager 启动的 AccessibilityService 和 GuardService 提供
            // 这里的 startLockTask 只是额外的防护层
            
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
        
        // Check是否屏蔽音量键
        if (hardwareController.isBlockVolumeKeys) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            }
        }
        
        // Check是否屏蔽电源键（注意：电源键在 Activity 中拦截有限）
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
        
        // 屏蔽音量键
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
        // Check是否启用了触摸屏蔽
        val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
        if (hardwareController.isBlockTouch) {
            // 屏蔽所有触摸事件
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
    
    // 公共 API 方法：委托到 ShellPermissionDelegate
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
        // Initialize日志系统（尽早初始化以捕获崩溃）
        ShellActivityInit.initLogger(this)
        
        // Enable边到边显示（让内容延伸到系统栏区域）
        try {
            enableEdgeToEdge()
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "enableEdgeToEdge 成功")
        } catch (e: Exception) {
            AppLogger.w("ShellActivity", "enableEdgeToEdge failed", e)
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "enableEdgeToEdge 失败", e)
        }
        
        super.onCreate(savedInstanceState)

        // 恢复 WebView 状态（系统后台杀死 Activity 后重建时）
        savedInstanceState?.let { webViewStateBundle = it }

        // SECURITY: 检查是否需要自定义密码才能解密配置
        if (WebToAppApplication.shellMode.requiresCustomPassword()) {
            showPasswordDialog()
            return
        }

        val config = WebToAppApplication.shellMode.getConfig()
        if (config == null) {
            AppLogger.e("ShellActivity", "配置加载失败，无法启动应用")
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "配置加载失败，无法启动应用")
            Toast.makeText(this, Strings.appConfigLoadFailed, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "配置加载成功: ${config.appName}")
        AppLogger.d("ShellActivity", "WebView UA config from shell: userAgentMode=${config.webViewConfig.userAgentMode}, customUserAgent=${config.webViewConfig.customUserAgent}, userAgent=${config.webViewConfig.userAgent}")
        
        // 初始化云 SDK（更新检查、公告、远程配置、统计上报）
        if (config.cloudSdkConfig.isValid()) {
            cloudSdkManager = CloudSdkManager(this, config.cloudSdkConfig).also {
                it.initialize(this)
                AppLogger.i("ShellActivity", "Cloud SDK initialized for project: ${config.cloudSdkConfig.projectKey}")
            }
        }
        
        forcedRunConfig = config.forcedRunConfig
        
        // 记录配置详情
        com.webtoapp.core.shell.ShellLogger.logFeature("Config", "加载配置", buildString {
            append("强制运行=${config.forcedRunConfig?.enabled ?: false}, ")
            append("后台运行=${config.backgroundRunEnabled}, ")
            append("独立环境=${config.isolationEnabled}")
        })
        
        // 初始化各子系统（逻辑已提取到 ShellActivityInit.kt）
        ShellActivityInit.initForcedRunManager(this, config, forcedRunManager, ::onForcedRunStateChanged)
        ShellActivityInit.initAutoStart(this, config)
        ShellActivityInit.initIsolation(this, config)
        ShellActivityInit.initBackgroundService(this, config)
        ShellActivityInit.initNotificationService(this, config)
        ShellActivityInit.setTaskDescription(this, config.appName)
        
        // Request通知权限（Android 13+）
        try {
            permissionDelegate.requestNotificationPermissionIfNeeded()
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "通知权限请求完成")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "通知权限请求失败", e)
        }
        
        // 读取状态栏配置
        com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "开始读取状态栏配置")
        statusBarColorMode = config.webViewConfig.statusBarColorMode
        statusBarCustomColor = config.webViewConfig.statusBarColor
        statusBarDarkIcons = config.webViewConfig.statusBarDarkIcons
        statusBarBackgroundType = config.webViewConfig.statusBarBackgroundType
        statusBarBackgroundImage = config.webViewConfig.statusBarBackgroundImage
        statusBarBackgroundAlpha = config.webViewConfig.statusBarBackgroundAlpha
        statusBarHeightDp = config.webViewConfig.statusBarHeightDp
        // Dark mode status bar config
        statusBarColorModeDark = config.webViewConfig.statusBarColorModeDark
        statusBarCustomColorDark = config.webViewConfig.statusBarColorDark
        statusBarDarkIconsDark = config.webViewConfig.statusBarDarkIconsDark
        statusBarBackgroundTypeDark = config.webViewConfig.statusBarBackgroundTypeDark
        statusBarBackgroundImageDark = config.webViewConfig.statusBarBackgroundImageDark
        statusBarBackgroundAlphaDark = config.webViewConfig.statusBarBackgroundAlphaDark
        showStatusBarInFullscreen = config.webViewConfig.showStatusBarInFullscreen
        showNavigationBarInFullscreen = config.webViewConfig.showNavigationBarInFullscreen
        // 读取键盘调整模式
        keyboardAdjustMode = try {
            KeyboardAdjustMode.valueOf(config.webViewConfig.keyboardAdjustMode)
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "键盘调整模式解析失败: ${config.webViewConfig.keyboardAdjustMode}, 使用默认值 RESIZE")
            KeyboardAdjustMode.RESIZE
        }

        // 根据配置决定是否启用沉浸式全屏模式
        // hideToolbar=true 时启用沉浸式（隐藏状态栏），否则显示状态栏
        immersiveFullscreenEnabled = config.webViewConfig.hideToolbar
        try {
            applyImmersiveFullscreen(immersiveFullscreenEnabled)
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "沉浸式全屏模式: $immersiveFullscreenEnabled")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "应用沉浸式全屏失败", e)
        }
        
        // 保持屏幕常亮（支持三种模式）
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
                // OFF 或未知值：向后兼容旧版 keepScreenOn 布尔值
                if (config.webViewConfig.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕常亮: 已启用（向后兼容模式）")
                }
            }
        }
        
        // 自定义屏幕亮度
        val shellBrightness = config.webViewConfig.screenBrightness
        if (shellBrightness in 0..100) {
            val lp = window.attributes
            lp.screenBrightness = shellBrightness / 100f
            window.attributes = lp
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕亮度: ${shellBrightness}%")
        }
        
        // 悬浮小窗模式
        val floatingWindowConfig = config.webViewConfig.floatingWindowConfig
        if (floatingWindowConfig.enabled) {
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "悬浮窗配置: size=${floatingWindowConfig.windowSizePercent}%, opacity=${floatingWindowConfig.opacity}%")
            if (FloatingWindowService.canDrawOverlays(this)) {
                // 有权限，启动悬浮窗服务并结束主 Activity（避免双窗口问题）
                launchFloatingWindowAndFinish(config)
                return
            } else {
                // 无权限，引导用户授权，等 onResume 重试
                pendingFloatingWindowLaunch = true
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
                // Get当前主题状态
                val isDarkTheme = com.webtoapp.ui.theme.LocalIsDarkTheme.current
                // Sync isDarkTheme to Activity level for onWindowFocusChanged
                currentIsDarkTheme = isDarkTheme
                
                // 当主题变化时更新状态栏颜色（根据当前主题选择对应配置）
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
                            // onDestroy 中的 pauseTimers() 是进程级静态方法，若 Activity 被系统重建而非用户销毁，
                            // 定时器仍处于暂停状态，必须显式恢复
                            wv.resumeTimers()
                            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "WebView 创建成功, timers resumed")
                            // 恢复 WebView 状态（系统后台杀死 Activity 后重建时）
                            // restoreState 必须在 loadUrl 之前调用
                            val savedState = webViewStateBundle
                            if (savedState != null) {
                                val restored = wv.restoreState(savedState)
                                webViewStateBundle = null
                                if (restored != null) {
                                    // 标记已恢复状态，让 loadUrl 跳过加载
                                    wv.tag = "state_restored"
                                    com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "WebView state restored from saved bundle")
                                }
                            }
                            // 添加翻译桥接
                            translateBridge = TranslateBridge(wv, lifecycleScope)
                            wv.addJavascriptInterface(translateBridge!!, TranslateBridge.JS_INTERFACE_NAME)
                            // 添加下载桥接（支持 Blob/Data URL 下载）
                            val downloadBridge = com.webtoapp.core.webview.DownloadBridge(this@ShellActivity, lifecycleScope)
                            wv.addJavascriptInterface(downloadBridge, com.webtoapp.core.webview.DownloadBridge.JS_INTERFACE_NAME)
                            // 添加原生能力桥接（供扩展模块调用）
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
                    // Status bar配置
                    statusBarBackgroundType = statusBarBackgroundType,
                    statusBarBackgroundColor = statusBarCustomColor,
                    statusBarBackgroundImage = statusBarBackgroundImage,
                    statusBarBackgroundAlpha = statusBarBackgroundAlpha,
                    statusBarHeightDp = statusBarHeightDp,
                    // Dark mode status bar配置
                    statusBarBackgroundTypeDark = statusBarBackgroundTypeDark,
                    statusBarBackgroundColorDark = statusBarCustomColorDark,
                    statusBarBackgroundImageDark = statusBarBackgroundImageDark,
                    statusBarBackgroundAlphaDark = statusBarBackgroundAlphaDark
                )
            }
        }

        // 返回键处理（逻辑已提取到 ShellActivityInit.kt）
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

    /**
     * 启动悬浮窗服务并立即结束主 Activity
     * 避免悬浮窗和全屏窗口同时存在的双窗口问题
     */
    private fun launchFloatingWindowAndFinish(config: com.webtoapp.core.shell.ShellConfig) {
        val floatingWindowConfig = config.webViewConfig.floatingWindowConfig
        val fwConfig = com.webtoapp.data.model.FloatingWindowConfig(
            enabled = true,
            windowSizePercent = floatingWindowConfig.windowSizePercent,
            widthPercent = floatingWindowConfig.widthPercent,
            heightPercent = floatingWindowConfig.heightPercent,
            lockAspectRatio = floatingWindowConfig.lockAspectRatio,
            opacity = floatingWindowConfig.opacity,
            cornerRadius = floatingWindowConfig.cornerRadius,
            borderStyle = try {
                com.webtoapp.data.model.FloatingBorderStyle.valueOf(floatingWindowConfig.borderStyle)
            } catch (e: Exception) {
                com.webtoapp.data.model.FloatingBorderStyle.SUBTLE
            },
            showTitleBar = floatingWindowConfig.showTitleBar,
            autoHideTitleBar = floatingWindowConfig.autoHideTitleBar,
            startMinimized = floatingWindowConfig.startMinimized,
            rememberPosition = floatingWindowConfig.rememberPosition,
            edgeSnapping = floatingWindowConfig.edgeSnapping,
            showResizeHandle = floatingWindowConfig.showResizeHandle,
            lockPosition = floatingWindowConfig.lockPosition
        )
        val intent = Intent(this, FloatingWindowService::class.java).apply {
            putExtra(FloatingWindowService.EXTRA_ACTION, FloatingWindowService.ACTION_SHOW)
            putExtra(FloatingWindowService.EXTRA_CONFIG, com.webtoapp.util.GsonProvider.gson.toJson(fwConfig))
            putExtra(FloatingWindowService.EXTRA_URL, config.targetUrl)
            putExtra(FloatingWindowService.EXTRA_APP_NAME, config.appName)
            putExtra(FloatingWindowService.EXTRA_TRANSLATE_ENABLED, config.translateEnabled)
            putExtra(FloatingWindowService.EXTRA_TRANSLATE_TARGET_LANGUAGE, config.translateTargetLanguage)
            putExtra(FloatingWindowService.EXTRA_TRANSLATE_SHOW_BUTTON, config.translateShowButton)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "悬浮窗服务已启动，关闭主 Activity")
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (customView != null || immersiveFullscreenEnabled || forceHideSystemUi) {
                applyImmersiveFullscreen(true, isDarkTheme = currentIsDarkTheme)
            } else {
                val effectiveColorMode = if (currentIsDarkTheme) statusBarColorModeDark else statusBarColorMode
                val effectiveCustomColor = if (currentIsDarkTheme) statusBarCustomColorDark else statusBarCustomColor
                val effectiveDarkIcons = if (currentIsDarkTheme) statusBarDarkIconsDark else statusBarDarkIcons
                applyStatusBarColor(effectiveColorMode, effectiveCustomColor, effectiveDarkIcons, currentIsDarkTheme)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onSaveInstanceState - WebView state saved")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        
        // Handle notification click URL (from NotificationPollingService)
        val notificationClickUrl = intent?.getStringExtra("notification_click_url")
        if (!notificationClickUrl.isNullOrBlank()) {
            val baseUrl = WebToAppApplication.shellMode.getConfig()?.targetUrl ?: ""
            val fullUrl = if (notificationClickUrl.startsWith("http")) notificationClickUrl
                          else baseUrl.trimEnd('/') + "/" + notificationClickUrl.trimStart('/')
            webView?.loadUrl(fullUrl)
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "Notification click URL: $fullUrl")
            return
        }
        
        val url = intent?.data?.toString()
        if (!url.isNullOrBlank() && intent?.action == Intent.ACTION_VIEW) {
            val safeUrl = normalizeShellTargetUrlForSecurity(url)
            val validatedUrl = if (safeUrl.startsWith("http://") || safeUrl.startsWith("https://")) safeUrl else return
            deepLinkUrl.value = validatedUrl
            // Directly load URL in existing WebView
            webView?.loadUrl(validatedUrl)
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "onNewIntent Deep Link: $validatedUrl (原始: $url)")
        }
    }
    
    override fun onResume() {
        super.onResume()

        // 悬浮窗模式：用户从权限设置页面返回后，检查权限并启动悬浮窗
        if (pendingFloatingWindowLaunch) {
            val config = WebToAppApplication.shellMode.getConfig()
            if (config != null && FloatingWindowService.canDrawOverlays(this)) {
                pendingFloatingWindowLaunch = false
                launchFloatingWindowAndFinish(config)
                return
            }
            // 用户未授权，继续以普通窗口模式运行
        }

        webView?.onResume()
        // onDestroy 中的 pauseTimers() 是进程级静态方法，若 Activity 被系统重建而非用户销毁，
        // 定时器仍处于暂停状态，必须显式恢复
        webView?.resumeTimers()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onResume - WebView resumed, timers resumed")
    }

    override fun onPause() {
        super.onPause()
        // 暂停 WebView 渲染，释放 CPU
        // 注意：不调用 pauseTimers()，因为它是进程级静态方法，会暂停所有 WebView 的 JS 定时器，
        // 导致返回前台时页面状态丢失（SPA 应用尤甚）
        webView?.onPause()
        // Persist cookies when app goes to background
        android.webkit.CookieManager.getInstance().flush()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onPause - WebView paused, cookies flushed")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // ★ FIX: 不再在内存压力时调用 clearCache() / freeMemory()
        // 这些方法会销毁 WebView 的缓存和内部内存，导致页面重新加载
        // 滚动时触发内存回收 → 缓存清除 → 页面重载 → 再次滚动 → 循环
        // 仅使用 System.gc() 作为温和的内存压力提示
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            System.gc()
            com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "Memory pressure (level=$level), requested GC (no cache clear)")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // ★ FIX: 不再调用 clearCache() / freeMemory()
        // freeMemory() 会使当前渲染的页面失效，导致用户可见的重新加载
        // clearCache(false) 清除磁盘+内存缓存，后续资源请求必须重新网络获取
        System.gc()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "Low memory, requested GC (no cache clear / freeMemory)")
    }

    override fun onDestroy() {
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onDestroy")
        
        // 释放云 SDK 资源
        cloudSdkManager?.destroy()
        cloudSdkManager = null
        
        // Persist cookies & WebStorage before destroying WebView
        android.webkit.CookieManager.getInstance().flush()
        // 完整清理 WebView：先停止加载，移除父 View，再销毁
        webView?.let { wv ->
            wv.stopLoading()
            // 注意：不再导航到 about:blank
            // 在 destroy 前 loadUrl("about:blank") 会导致 WebView 切换 origin，
            // 某些 Android 版本来不及将当前页面的 localStorage 刷盘，造成 H5 游戏存档丢失
            wv.onPause()
            wv.pauseTimers()
            wv.webChromeClient = null
            // 从父 View 移除，防止内存泄漏
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
        webView = null
        super.onDestroy()
    }
    
    /**
     * SECURITY: 显示密码输入对话框
     * 当 APK 使用自定义密码加密时，需要用户输入密码才能解密配置
     */
    private fun showPasswordDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = Strings.enterEncryptionPassword
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(Strings.passwordVerification)
            .setMessage(Strings.appEncryptedMessage)
            .setView(editText)
            .setPositiveButton(Strings.btnConfirm) { _, _ ->
                val password = editText.text.toString()
                if (password.isNotBlank()) {
                    WebToAppApplication.shellMode.setCustomPassword(password)
                    // 重新尝试加载配置
                    val config = WebToAppApplication.shellMode.getConfig()
                    if (config == null) {
                        Toast.makeText(this, Strings.wrongPasswordCannotDecrypt, Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        // 密码正确，重新执行 onCreate 后续逻辑
                        recreate()
                    }
                } else {
                    Toast.makeText(this, Strings.passwordCannotBeEmpty, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(Strings.btnExit) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }
}

// ShellScreen composable 已提取到 ShellScreen.kt
