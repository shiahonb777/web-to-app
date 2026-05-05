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





class ShellActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null


    private var deepLinkUrl = mutableStateOf<String?>(null)


    val permissionDelegate = ShellPermissionDelegate(this)

    private var immersiveFullscreenEnabled: Boolean = false
    private var showStatusBarInFullscreen: Boolean = false
    private var showNavigationBarInFullscreen: Boolean = false
    private var translateBridge: TranslateBridge? = null


    private var originalOrientationBeforeFullscreen: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED


    private var statusBarColorMode: String = "THEME"
    private var statusBarCustomColor: String? = null
    private var statusBarDarkIcons: Boolean? = null
    private var statusBarBackgroundType: String = "COLOR"
    private var statusBarBackgroundImage: String? = null
    private var statusBarBackgroundAlpha: Float = 1.0f
    private var statusBarHeightDp: Int = 0

    private var statusBarColorModeDark: String = "THEME"
    private var statusBarCustomColorDark: String? = null
    private var statusBarDarkIconsDark: Boolean? = null
    private var statusBarBackgroundTypeDark: String = "COLOR"
    private var statusBarBackgroundImageDark: String? = null
    private var statusBarBackgroundAlphaDark: Float = 1.0f
    private var currentIsDarkTheme: Boolean = false
    private var forceHideSystemUi: Boolean = false
    private var keyboardAdjustMode: KeyboardAdjustMode = KeyboardAdjustMode.RESIZE
    private var forcedRunConfig: ForcedRunConfig? = null
    private val forcedRunManager by lazy { ForcedRunManager.getInstance(this) }


    private var pendingFloatingWindowLaunch = false


    private var webViewStateBundle: Bundle? = null


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

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


            try {
                startLockTask()
            } catch (e: Exception) {
                AppLogger.w("ShellActivity", "startLockTask failed (expected without device admin)", e)
            }




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


        if (hardwareController.isBlockVolumeKeys) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            }
        }


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

        val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
        if (hardwareController.isBlockTouch) {

            return true
        }
        return super.dispatchTouchEvent(ev)
    }


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

        ShellActivityInit.initLogger(this)


        try {
            enableEdgeToEdge()
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "enableEdgeToEdge 成功")
        } catch (e: Exception) {
            AppLogger.w("ShellActivity", "enableEdgeToEdge failed", e)
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "enableEdgeToEdge 失败", e)
        }

        super.onCreate(savedInstanceState)


        savedInstanceState?.let { webViewStateBundle = it }


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


        if (config.cloudSdkConfig.isValid()) {
            cloudSdkManager = CloudSdkManager(this, config.cloudSdkConfig).also {
                it.initialize(this)
                AppLogger.i("ShellActivity", "Cloud SDK initialized for runtime: ${config.cloudSdkConfig.resolvedRuntimeKey()}")
            }
        }

        forcedRunConfig = config.forcedRunConfig


        com.webtoapp.core.shell.ShellLogger.logFeature("Config", "加载配置", buildString {
            append("强制运行=${config.forcedRunConfig?.enabled ?: false}, ")
            append("后台运行=${config.backgroundRunEnabled}, ")
            append("独立环境=${config.isolationEnabled}")
        })


        ShellActivityInit.initForcedRunManager(this, config, forcedRunManager, ::onForcedRunStateChanged)
        ShellActivityInit.initAutoStart(this, config)
        ShellActivityInit.initIsolation(this, config)
        ShellActivityInit.initBackgroundService(this, config)
        ShellActivityInit.initNotificationService(this, config)
        ShellActivityInit.setTaskDescription(this, config.appName)


        try {
            permissionDelegate.requestNotificationPermissionIfNeeded()
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "通知权限请求完成")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "通知权限请求失败", e)
        }


        com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "开始读取状态栏配置")
        statusBarColorMode = config.webViewConfig.statusBarColorMode
        statusBarCustomColor = config.webViewConfig.statusBarColor
        statusBarDarkIcons = config.webViewConfig.statusBarDarkIcons
        statusBarBackgroundType = config.webViewConfig.statusBarBackgroundType
        statusBarBackgroundImage = config.webViewConfig.statusBarBackgroundImage
        statusBarBackgroundAlpha = config.webViewConfig.statusBarBackgroundAlpha
        statusBarHeightDp = config.webViewConfig.statusBarHeightDp

        statusBarColorModeDark = config.webViewConfig.statusBarColorModeDark
        statusBarCustomColorDark = config.webViewConfig.statusBarColorDark
        statusBarDarkIconsDark = config.webViewConfig.statusBarDarkIconsDark
        statusBarBackgroundTypeDark = config.webViewConfig.statusBarBackgroundTypeDark
        statusBarBackgroundImageDark = config.webViewConfig.statusBarBackgroundImageDark
        statusBarBackgroundAlphaDark = config.webViewConfig.statusBarBackgroundAlphaDark
        showStatusBarInFullscreen = config.webViewConfig.showStatusBarInFullscreen
        showNavigationBarInFullscreen = config.webViewConfig.showNavigationBarInFullscreen

        keyboardAdjustMode = try {
            KeyboardAdjustMode.valueOf(config.webViewConfig.keyboardAdjustMode)
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "键盘调整模式解析失败: ${config.webViewConfig.keyboardAdjustMode}, 使用默认值 RESIZE")
            KeyboardAdjustMode.RESIZE
        }



        immersiveFullscreenEnabled = config.webViewConfig.hideToolbar
        try {
            applyImmersiveFullscreen(immersiveFullscreenEnabled)
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "沉浸式全屏模式: $immersiveFullscreenEnabled")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "应用沉浸式全屏失败", e)
        }


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

                if (config.webViewConfig.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕常亮: 已启用（向后兼容模式）")
                }
            }
        }


        val shellBrightness = config.webViewConfig.screenBrightness
        if (shellBrightness in 0..100) {
            val lp = window.attributes
            lp.screenBrightness = shellBrightness / 100f
            window.attributes = lp
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "屏幕亮度: ${shellBrightness}%")
        }


        val floatingWindowConfig = config.webViewConfig.floatingWindowConfig
        if (floatingWindowConfig.enabled) {
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "悬浮窗配置: size=${floatingWindowConfig.windowSizePercent}%, opacity=${floatingWindowConfig.opacity}%")
            if (FloatingWindowService.canDrawOverlays(this)) {

                launchFloatingWindowAndFinish(config)
                return
            } else {

                pendingFloatingWindowLaunch = true
                com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "悬浮窗权限未授予，引导用户授权")
                Toast.makeText(this, Strings.floatingWindowPermissionRequired, Toast.LENGTH_LONG).show()
                FloatingWindowService.requestOverlayPermission(this)
            }
        }


        val intentUrl = intent?.data?.toString()
        if (!intentUrl.isNullOrBlank() && intent?.action == Intent.ACTION_VIEW) {
            val safeUrl = normalizeShellTargetUrlForSecurity(intentUrl)
            val validatedUrl = if (config.deepLinkEnabled || config.deepLinkHosts.isNotEmpty()) {
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

                val isDarkTheme = com.webtoapp.ui.theme.LocalIsDarkTheme.current

                currentIsDarkTheme = isDarkTheme


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


                            wv.resumeTimers()
                            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "WebView 创建成功, timers resumed")


                            val savedState = webViewStateBundle
                            if (savedState != null) {
                                val restored = wv.restoreState(savedState)
                                webViewStateBundle = null
                                if (restored != null) {

                                    wv.tag = "state_restored"
                                    com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "WebView state restored from saved bundle")
                                }
                            }

                            translateBridge = TranslateBridge(wv, lifecycleScope)
                            wv.addJavascriptInterface(translateBridge!!, TranslateBridge.JS_INTERFACE_NAME)

                            val downloadBridge = com.webtoapp.core.webview.DownloadBridge(this@ShellActivity, lifecycleScope)
                            wv.addJavascriptInterface(downloadBridge, com.webtoapp.core.webview.DownloadBridge.JS_INTERFACE_NAME)

                            val nativeBridge = com.webtoapp.core.webview.NativeBridge(this@ShellActivity, lifecycleScope) { wv }
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

                    statusBarBackgroundType = statusBarBackgroundType,
                    statusBarBackgroundColor = statusBarCustomColor,
                    statusBarBackgroundImage = statusBarBackgroundImage,
                    statusBarBackgroundAlpha = statusBarBackgroundAlpha,
                    statusBarHeightDp = statusBarHeightDp,

                    statusBarBackgroundTypeDark = statusBarBackgroundTypeDark,
                    statusBarBackgroundColorDark = statusBarCustomColorDark,
                    statusBarBackgroundImageDark = statusBarBackgroundImageDark,
                    statusBarBackgroundAlphaDark = statusBarBackgroundAlphaDark
                )
            }
        }


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


        val notificationClickUrl = intent?.getStringExtra("notification_click_url")
        if (!notificationClickUrl.isNullOrBlank()) {
            val baseUrl = WebToAppApplication.shellMode.getConfig()?.targetUrl ?: ""
            val fullUrl = if (notificationClickUrl.startsWith("http://") || notificationClickUrl.startsWith("https://")) notificationClickUrl
                          else baseUrl.trimEnd('/') + "/" + notificationClickUrl.trimStart('/')

            if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://")) {
                com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "Notification click URL rejected (unsafe scheme): $fullUrl")
                return
            }
            val safeUrl = normalizeShellTargetUrlForSecurity(fullUrl)
            webView?.loadUrl(safeUrl)
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "Notification click URL: $safeUrl")
            return
        }

        val url = intent?.data?.toString()
        if (!url.isNullOrBlank() && intent?.action == Intent.ACTION_VIEW) {
            val config = WebToAppApplication.shellMode.getConfig()
            val safeUrl = normalizeShellTargetUrlForSecurity(url)
            val validatedUrl = if (config != null && (config.deepLinkEnabled || config.deepLinkHosts.isNotEmpty())) {
                validateDeepLinkUrl(safeUrl, config.deepLinkHosts, config.targetUrl)
            } else {
                safeUrl
            }
            if (!validatedUrl.startsWith("http://") && !validatedUrl.startsWith("https://")) return
            deepLinkUrl.value = validatedUrl

            webView?.loadUrl(validatedUrl)
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "onNewIntent Deep Link: $validatedUrl (原始: $url)")
        }
    }

    override fun onResume() {
        super.onResume()


        if (pendingFloatingWindowLaunch) {
            val config = WebToAppApplication.shellMode.getConfig()
            if (config != null && FloatingWindowService.canDrawOverlays(this)) {
                pendingFloatingWindowLaunch = false
                launchFloatingWindowAndFinish(config)
                return
            }

        }

        webView?.onResume()


        webView?.resumeTimers()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onResume - WebView resumed, timers resumed")
    }

    override fun onPause() {
        super.onPause()



        webView?.onPause()

        android.webkit.CookieManager.getInstance().flush()
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onPause - WebView paused, cookies flushed")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)




        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "Memory pressure (level=$level), skipped manual GC")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()



        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "Low memory, skipped manual GC")
    }

    override fun onDestroy() {
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onDestroy")


        cloudSdkManager?.destroy()
        cloudSdkManager = null


        android.webkit.CookieManager.getInstance().flush()

        webView?.let { wv ->
            wv.stopLoading()



            wv.onPause()
            wv.pauseTimers()
            wv.webChromeClient = null

            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
        webView = null
        super.onDestroy()
    }





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

                    val config = WebToAppApplication.shellMode.getConfig()
                    if (config == null) {
                        Toast.makeText(this, Strings.wrongPasswordCannotDecrypt, Toast.LENGTH_LONG).show()
                        finish()
                    } else {

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
