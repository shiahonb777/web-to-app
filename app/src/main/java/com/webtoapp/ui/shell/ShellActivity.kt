package com.webtoapp.ui.shell

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.LongPressHandler
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.data.model.Announcement
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.LrcLine
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.UserScript
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.components.ForcedRunCountdownOverlay
import com.webtoapp.ui.components.LongPressMenuSheet
import com.webtoapp.ui.components.VirtualNavigationBar
import com.webtoapp.ui.components.StatusBarOverlay
import com.webtoapp.ui.components.announcement.AnnouncementDialog
import com.webtoapp.ui.theme.ShellTheme
import com.webtoapp.ui.theme.LocalIsDarkTheme
import com.webtoapp.ui.splash.ActivationDialog
import com.webtoapp.util.DownloadHelper
import com.webtoapp.core.webview.TranslateBridge
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.forcedrun.ForcedRunMode
import com.webtoapp.core.forcedrun.ForcedRunPermissionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

/**
 * Shell Activity - 用于独立 WebApp 运行
 * 从 app_config.json 读取配置并显示 WebView
 */
class ShellActivity : AppCompatActivity() {

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
    private var translateBridge: TranslateBridge? = null
    
    // 视频全屏前的屏幕方向
    private var originalOrientationBeforeFullscreen: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    
    // 状态栏配置缓存
    private var statusBarColorMode: String = "THEME"
    private var statusBarCustomColor: String? = null
    private var statusBarDarkIcons: Boolean? = null
    private var statusBarBackgroundType: String = "COLOR"
    private var statusBarBackgroundImage: String? = null
    private var statusBarBackgroundAlpha: Float = 1.0f
    private var statusBarHeightDp: Int = 0
    private var forceHideSystemUi: Boolean = false
    private var forcedRunConfig: ForcedRunConfig? = null
    private val forcedRunManager by lazy { ForcedRunManager.getInstance(this) }

    /**
     * 应用状态栏颜色配置
     * 
     * @param colorMode 颜色模式：THEME（跟随主题）、TRANSPARENT（透明）、CUSTOM（自定义）
     * @param customColor 自定义颜色（仅 CUSTOM 模式生效）
     * @param darkIcons 图标颜色：true=深色图标，false=浅色图标，null=自动
     * @param isDarkTheme 当前是否为深色主题
     */
    private fun applyStatusBarColor(
        colorMode: String,
        customColor: String?,
        darkIcons: Boolean?,
        isDarkTheme: Boolean
    ) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        
        when (colorMode) {
            "TRANSPARENT" -> {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                // 自动判断图标颜色
                val useDarkIcons = darkIcons ?: !isDarkTheme
                controller.isAppearanceLightStatusBars = useDarkIcons
            }
            "CUSTOM" -> {
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
                // 深色主题用深色背景+浅色图标，浅色主题用浅色背景+深色图标
                if (isDarkTheme) {
                    window.statusBarColor = android.graphics.Color.parseColor("#1C1B1F") // Material3 深色背景
                    controller.isAppearanceLightStatusBars = false
                } else {
                    window.statusBarColor = android.graphics.Color.parseColor("#FFFBFE") // Material3 浅色背景
                    controller.isAppearanceLightStatusBars = true
                }
            }
        }
        
        // 导航栏也跟随设置
        controller.isAppearanceLightNavigationBars = controller.isAppearanceLightStatusBars
    }
    
    /**
     * 判断颜色是否为浅色（用于自动选择图标颜色）
     */
    private fun isColorLight(color: Int): Boolean {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        // 使用相对亮度公式
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance > 0.5
    }

    /**
     * 应用沉浸式全屏模式
     * 
     * @param enabled 是否启用沉浸式模式
     * @param hideNavBar 是否同时隐藏导航栏（视频全屏时为 true）
     * @param isDarkTheme 当前是否为深色主题（用于状态栏颜色）
     */
    private fun applyImmersiveFullscreen(enabled: Boolean, hideNavBar: Boolean = true, isDarkTheme: Boolean = false) {
        try {
            // 支持刘海屏/挖孔屏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = 
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                if (enabled) {
                    // 沉浸式模式
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    
                    // 根据配置决定是否显示状态栏
                    val shouldShowStatusBar = if (forceHideSystemUi) false else showStatusBarInFullscreen
                    if (shouldShowStatusBar) {
                        // 全屏模式但显示状态栏：内容延伸到状态栏区域
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        controller.show(WindowInsetsCompat.Type.statusBars())
                        
                        // 如果是图片背景，状态栏设为透明，让 StatusBarOverlay 组件显示图片
                        if (statusBarBackgroundType == "IMAGE") {
                            window.statusBarColor = android.graphics.Color.TRANSPARENT
                            val useDarkIcons = statusBarDarkIcons ?: !isDarkTheme
                            controller.isAppearanceLightStatusBars = useDarkIcons
                        } else {
                            // 纯色背景：直接设置系统状态栏颜色
                            when (statusBarColorMode) {
                                "CUSTOM" -> {
                                    val color = try {
                                        android.graphics.Color.parseColor(statusBarCustomColor ?: "#000000")
                                    } catch (e: Exception) {
                                        android.graphics.Color.BLACK
                                    }
                                    window.statusBarColor = color
                                    val useDarkIcons = statusBarDarkIcons ?: isColorLight(color)
                                    controller.isAppearanceLightStatusBars = useDarkIcons
                                }
                                "TRANSPARENT" -> {
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
                    
                    if (hideNavBar || forceHideSystemUi) {
                        // 同时隐藏导航栏（完全沉浸式）
                        controller.hide(WindowInsetsCompat.Type.navigationBars())
                    }
                    // 从边缘滑动时临时显示系统栏
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    // 非沉浸式模式：显示系统栏，应用状态栏颜色配置
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    
                    // 应用状态栏颜色配置
                    applyStatusBarColor(statusBarColorMode, statusBarCustomColor, statusBarDarkIcons, isDarkTheme)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ShellActivity", "applyImmersiveFullscreen failed", e)
        }
    }

    fun onForcedRunStateChanged(active: Boolean, config: ForcedRunConfig?) {
        forcedRunConfig = config
        forceHideSystemUi = active && config?.blockSystemUI == true
        
        android.util.Log.d("ShellActivity", "强制运行状态变化: active=$active, protection=${config?.protectionLevel}")
        
        if (active) {
            // 保持屏幕常亮
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // 尝试 Lock Task Mode（作为额外防护层，可能需要用户确认）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    startLockTask()
                } catch (e: Exception) {
                    android.util.Log.w("ShellActivity", "startLockTask failed (expected without device admin)", e)
                }
            }
            
            // 注意：真正的防护由 ForcedRunManager 启动的 AccessibilityService 和 GuardService 提供
            // 这里的 startLockTask 只是额外的防护层
            
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    stopLockTask()
                } catch (e: Exception) {
                    android.util.Log.w("ShellActivity", "stopLockTask failed", e)
                }
            }
        }
        
        applyImmersiveFullscreen(customView != null || immersiveFullscreenEnabled || forceHideSystemUi)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
        
        // 检查是否屏蔽音量键
        if (hardwareController.isBlockVolumeKeys) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            }
        }
        
        // 检查是否屏蔽电源键（注意：电源键在 Activity 中拦截有限）
        if (hardwareController.isBlockPowerKey && event.keyCode == KeyEvent.KEYCODE_POWER) {
            return true
        }
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (forcedRunManager.handleKeyEvent(event.keyCode)) {
                return true
            }
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
        // 检查是否启用了触摸屏蔽
        val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
        if (hardwareController.isBlockTouch) {
            // 屏蔽所有触摸事件
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
    
    // 待下载信息（权限请求后使用）
    private var pendingDownload: PendingDownload? = null
    
    private data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String,
        val contentLength: Long
    )

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }
    
    // 存储权限请求
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 权限已授予，执行下载
            pendingDownload?.let { download ->
                DownloadHelper.handleDownload(
                    context = this,
                    url = download.url,
                    userAgent = download.userAgent,
                    contentDisposition = download.contentDisposition,
                    mimeType = download.mimeType,
                    contentLength = download.contentLength,
                    method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                    scope = lifecycleScope
                )
            }
        } else {
            Toast.makeText(this, Strings.storagePermissionRequired, Toast.LENGTH_SHORT).show()
            // 尝试使用浏览器下载
            pendingDownload?.let { download ->
                DownloadHelper.openInBrowser(this, download.url)
            }
        }
        pendingDownload = null
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
            android.util.Log.d("ShellActivity", "通知权限已授予")
        } else {
            android.util.Log.d("ShellActivity", "通知权限被拒绝")
        }
    }
    
    /**
     * 请求通知权限（Android 13+）
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    
    /**
     * 处理下载（带权限检查）
     */
    fun handleDownloadWithPermission(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        // Android 10+ 不需要存储权限即可使用 DownloadManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DownloadHelper.handleDownload(
                context = this,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                scope = lifecycleScope
            )
            return
        }
        
        // Android 9 及以下需要检查存储权限
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            DownloadHelper.handleDownload(
                context = this,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                scope = lifecycleScope
            )
        } else {
            // 保存下载信息，请求权限
            pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType, contentLength)
            storagePermissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 初始化日志系统（尽早初始化以捕获崩溃）
        try {
            val tempConfig = WebToAppApplication.shellMode.getConfig()
            // 获取应用版本号
            val versionName = try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) { "1.0.0" }
            
            com.webtoapp.core.shell.ShellLogger.init(
                context = this,
                appName = tempConfig?.appName ?: "ShellApp",
                appVersion = versionName
            )
            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "onCreate 开始")
        } catch (e: Exception) {
            android.util.Log.e("ShellActivity", "日志系统初始化失败", e)
        }
        
        // 启用边到边显示（让内容延伸到系统栏区域）
        try {
            enableEdgeToEdge()
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "enableEdgeToEdge 成功")
        } catch (e: Exception) {
            android.util.Log.w("ShellActivity", "enableEdgeToEdge failed", e)
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "enableEdgeToEdge 失败", e)
        }
        
        super.onCreate(savedInstanceState)

        val config = WebToAppApplication.shellMode.getConfig()
        if (config == null) {
            // 配置加载失败，显示错误信息后退出
            android.util.Log.e("ShellActivity", "配置加载失败，无法启动应用")
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "配置加载失败，无法启动应用")
            Toast.makeText(this, Strings.appConfigLoadFailed, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "配置加载成功: ${config.appName}")
        
        forcedRunConfig = config.forcedRunConfig
        
        // 记录配置详情
        com.webtoapp.core.shell.ShellLogger.logFeature("Config", "加载配置", buildString {
            append("强制运行=${config.forcedRunConfig?.enabled ?: false}, ")
            append("后台运行=${config.backgroundRunEnabled}, ")
            append("独立环境=${config.isolationEnabled}")
        })
        
        // 设置硬件控制器的目标 Activity（用于屏幕翻转、黑屏等功能）
        try {
            val hardwareController = com.webtoapp.core.forcedrun.ForcedRunHardwareController.getInstance(this)
            hardwareController.setTargetActivity(this)
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "硬件控制器初始化成功")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "硬件控制器初始化失败", e)
        }
        
        // 初始化强制运行管理器
        if (config.forcedRunConfig?.enabled == true) {
            try {
                // 设置目标 Activity（用于辅助功能服务拉回）
                forcedRunManager.setTargetActivity(
                    packageName = packageName,
                    activityClass = this::class.java.name
                )
                
                // 设置状态变化回调
                forcedRunManager.setOnStateChangedCallback { active, forcedConfig ->
                    runOnUiThread {
                        onForcedRunStateChanged(active, forcedConfig)
                    }
                }
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "强制运行管理器初始化成功")
            } catch (e: Exception) {
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "强制运行管理器初始化失败", e)
            }
        }

        // 注册自启动配置（如果启用）
        config.autoStartConfig?.let { autoStartConfig ->
            try {
                val autoStartManager = com.webtoapp.core.autostart.AutoStartManager(this)
                // 设置开机自启动
                autoStartManager.setBootStart(0L, autoStartConfig.bootStartEnabled)
                // 设置定时自启动
                if (autoStartConfig.scheduledStartEnabled) {
                    autoStartManager.setScheduledStart(
                        appId = 0L,
                        enabled = true,
                        time = autoStartConfig.scheduledTime,
                        days = autoStartConfig.scheduledDays
                    )
                }
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "自启动配置已注册: 开机=${autoStartConfig.bootStartEnabled}, 定时=${autoStartConfig.scheduledStartEnabled}")
            } catch (e: Exception) {
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "自启动配置注册失败", e)
            }
        }
        
        // 初始化独立环境/多开配置（如果启用）
        if (config.isolationEnabled && config.isolationConfig != null) {
            try {
                val isolationConfig = config.isolationConfig.toIsolationConfig()
                val isolationManager = com.webtoapp.core.isolation.IsolationManager.getInstance(this)
                isolationManager.initialize(isolationConfig)
                android.util.Log.d("ShellActivity", "独立环境已初始化: enabled=${isolationConfig.enabled}")
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "独立环境已初始化")
            } catch (e: Exception) {
                android.util.Log.e("ShellActivity", "独立环境初始化失败", e)
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "独立环境初始化失败", e)
            }
        }
        
        // 初始化后台运行服务（如果启用）
        if (config.backgroundRunEnabled) {
            try {
                val bgConfig = config.backgroundRunConfig
                com.webtoapp.core.background.BackgroundRunService.start(
                    context = this,
                    appName = config.appName,
                    notificationTitle = bgConfig?.notificationTitle?.ifEmpty { null },
                    notificationContent = bgConfig?.notificationContent?.ifEmpty { null },
                    showNotification = bgConfig?.showNotification ?: true,
                    keepCpuAwake = bgConfig?.keepCpuAwake ?: true
                )
                android.util.Log.d("ShellActivity", "后台运行服务已启动")
                com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "后台运行服务已启动")
            } catch (e: Exception) {
                com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "后台运行服务启动失败", e)
            }
        }
        
        // 设置任务列表中显示的应用名称（修复双重名称显示问题）
        // 使用 setTaskDescription 明确设置任务描述，避免系统自动拼接 Application label 和 Activity label
        try {
            @Suppress("DEPRECATION")
            setTaskDescription(android.app.ActivityManager.TaskDescription(config.appName))
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.w("ShellActivity", "setTaskDescription 失败", e)
        }
        
        // 请求通知权限（Android 13+），用于显示下载进度和完成通知
        try {
            requestNotificationPermissionIfNeeded()
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
        showStatusBarInFullscreen = config.webViewConfig.showStatusBarInFullscreen
        
        // 根据配置决定是否启用沉浸式全屏模式
        // hideToolbar=true 时启用沉浸式（隐藏状态栏），否则显示状态栏
        immersiveFullscreenEnabled = config.webViewConfig.hideToolbar
        try {
            applyImmersiveFullscreen(immersiveFullscreenEnabled)
            com.webtoapp.core.shell.ShellLogger.d("ShellActivity", "沉浸式全屏模式: $immersiveFullscreenEnabled")
        } catch (e: Exception) {
            com.webtoapp.core.shell.ShellLogger.e("ShellActivity", "应用沉浸式全屏失败", e)
        }

        com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "setContent 开始，主题=${config.themeType}")
        
        setContent {
            ShellTheme(
                themeTypeName = config.themeType,
                darkModeSetting = config.darkMode
            ) {
                // 获取当前主题状态
                val isDarkTheme = com.webtoapp.ui.theme.LocalIsDarkTheme.current
                
                // 当主题变化时更新状态栏颜色
                LaunchedEffect(isDarkTheme, statusBarColorMode) {
                    if (!immersiveFullscreenEnabled) {
                        applyStatusBarColor(statusBarColorMode, statusBarCustomColor, statusBarDarkIcons, isDarkTheme)
                    }
                }
                
                ShellScreen(
                    config = config,
                    onWebViewCreated = { wv ->
                        try {
                            webView = wv
                            com.webtoapp.core.shell.ShellLogger.i("ShellActivity", "WebView 创建成功")
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
                    },
                    onForcedRunStateChanged = { active, forcedConfig ->
                        onForcedRunStateChanged(active, forcedConfig)
                    },
                    // 状态栏配置
                    statusBarBackgroundType = statusBarBackgroundType,
                    statusBarBackgroundColor = statusBarCustomColor,
                    statusBarBackgroundImage = statusBarBackgroundImage,
                    statusBarBackgroundAlpha = statusBarBackgroundAlpha,
                    statusBarHeightDp = statusBarHeightDp
                )
            }
        }

        // 返回键处理
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (forcedRunManager.handleKeyEvent(KeyEvent.KEYCODE_BACK)) {
                    Toast.makeText(this@ShellActivity, Strings.cannotExitDuringForcedRun, Toast.LENGTH_SHORT).show()
                    return
                }
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
            applyImmersiveFullscreen(customView != null || immersiveFullscreenEnabled || forceHideSystemUi)
        }
    }

    override fun onDestroy() {
        com.webtoapp.core.shell.ShellLogger.logLifecycle("ShellActivity", "onDestroy")
        // 只销毁 WebView，不清理存储数据（保留 localStorage 等）
        webView?.destroy()
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    config: ShellConfig,
    onWebViewCreated: (WebView) -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onFullscreenModeChanged: (Boolean) -> Unit,
    onForcedRunStateChanged: (Boolean, ForcedRunConfig?) -> Unit,
    // 状态栏配置
    statusBarBackgroundType: String = "COLOR",
    statusBarBackgroundColor: String? = null,
    statusBarBackgroundImage: String? = null,
    statusBarBackgroundAlpha: Float = 1.0f,
    statusBarHeightDp: Int = 0
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val activation = WebToAppApplication.activation
    val announcement = WebToAppApplication.announcement
    val adBlocker = WebToAppApplication.adBlock
    val forcedRunManager = remember { ForcedRunManager.getInstance(context) }
    val forcedRunActive by forcedRunManager.isInForcedRunMode.collectAsState()
    val forcedRunRemainingMs by forcedRunManager.remainingTimeMs.collectAsState()
    var forcedRunBlocked by remember { mutableStateOf(false) }
    var forcedRunBlockedMessage by remember { mutableStateOf("") }
    
    // 强制运行权限引导状态
    var showForcedRunPermissionDialog by remember { mutableStateOf(false) }
    var forcedRunPermissionChecked by remember { mutableStateOf(false) }

    // 状态
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    // 激活状态：如果启用了激活码，默认未激活，防止 WebView 在检查完成前加载
    var isActivated by remember { mutableStateOf(!config.activationEnabled) }
    // 激活检查是否完成（用于显示加载状态）
    var isActivationChecked by remember { mutableStateOf(!config.activationEnabled) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

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
            android.util.Log.d("ShellActivity", "同步检查: 启动画面媒体 encrypted=$hasEncrypted, normal=$hasNormal, exists=$exists")
            exists
        } else false
    }
    
    // 启动画面状态 - 根据配置同步初始化
    var showSplash by remember { mutableStateOf(config.splashEnabled && splashMediaExists) }
    var splashCountdown by remember { mutableIntStateOf(if (config.splashEnabled && splashMediaExists) config.splashDuration else 0) }
    var originalOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    
    // 处理启动画面横屏
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
    val scope = rememberCoroutineScope()
    val longPressHandler = remember { LongPressHandler(context, scope) }

    // 初始化配置
    LaunchedEffect(Unit) {
        // 配置广告拦截
        if (config.adBlockEnabled) {
            adBlocker.initialize(config.adBlockRules, useDefaultRules = true)
            adBlocker.setEnabled(true)
        }

        // 检查激活状态
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

        // 检查公告
        if (config.announcementEnabled && isActivated && config.announcementTitle.isNotEmpty()) {
            val ann = Announcement(
                title = config.announcementTitle,
                content = config.announcementContent,
                linkUrl = config.announcementLink.ifEmpty { null },
                showOnce = config.announcementShowOnce
            )
            showAnnouncementDialog = announcement.shouldShowAnnouncement(-1L, ann)
        }

        // 设置横屏模式（Web应用或HTML应用）
        if (config.webViewConfig.landscapeMode || 
            (config.appType == "HTML" && config.htmlConfig.landscapeMode)) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // 启动画面已在同步初始化阶段处理
        android.util.Log.d("ShellActivity", "LaunchedEffect: showSplash=$showSplash, splashCountdown=$splashCountdown")
        
        // 检查强制运行权限
        val forcedConfig = config.forcedRunConfig
        if (forcedConfig?.enabled == true && !forcedRunPermissionChecked) {
            val protectionLevel = forcedConfig.protectionLevel
            val permissionStatus = ForcedRunManager.checkProtectionPermissions(context, protectionLevel)
            
            android.util.Log.d("ShellActivity", "强制运行权限检查: level=$protectionLevel, " +
                "hasAccessibility=${permissionStatus.hasAccessibility}, " +
                "hasUsageStats=${permissionStatus.hasUsageStats}, " +
                "isFullyGranted=${permissionStatus.isFullyGranted}")
            
            if (!permissionStatus.isFullyGranted) {
                // 显示权限引导对话框
                showForcedRunPermissionDialog = true
            }
            forcedRunPermissionChecked = true
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun updateForcedRunState() {
        val forcedConfig = config.forcedRunConfig
        if (forcedConfig?.enabled != true || !isActivated) {
            forcedRunBlocked = false
            if (forcedRunActive) {
                forcedRunManager.stopForcedRunMode()
            }
            return
        }

        if (!forcedRunManager.canEnterApp(forcedConfig)) {
            val waitMs = forcedRunManager.getTimeUntilNextAccess(forcedConfig)
            val waitText = if (waitMs > 0) formatDuration(waitMs) else ""
            forcedRunBlockedMessage = if (waitText.isNotEmpty()) {
                "当前不在允许进入时间，请稍后再试（剩余 $waitText）。"
            } else {
                "当前不在允许进入时间，请稍后再试。"
            }
            forcedRunBlocked = true
            if (forcedRunActive) {
                forcedRunManager.stopForcedRunMode()
            }
            return
        }

        forcedRunBlocked = false
        val shouldStart = when (forcedConfig.mode) {
            ForcedRunMode.COUNTDOWN -> true
            else -> forcedRunManager.isInForcedRunPeriod(forcedConfig)
        }

        if (shouldStart && !forcedRunActive) {
            forcedRunManager.startForcedRunMode(forcedConfig, -1L)
        } else if (!shouldStart && forcedRunActive) {
            forcedRunManager.stopForcedRunMode()
        }
    }

    LaunchedEffect(isActivated, config.forcedRunConfig) {
        while (true) {
            updateForcedRunState()
            delay(60_000L)
        }
    }

    LaunchedEffect(forcedRunActive, config.forcedRunConfig) {
        onForcedRunStateChanged(forcedRunActive, config.forcedRunConfig)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (forcedRunActive) {
                forcedRunManager.stopForcedRunMode()
            }
        }
    }

    // 启动画面倒计时（仅用于图片类型，视频类型由播放器控制）
    LaunchedEffect(showSplash, splashCountdown) {
        // 视频类型不使用倒计时，由视频播放器控制结束
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
    
    // ===== 背景音乐播放器 =====
    var bgmPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentBgmIndex by remember { mutableIntStateOf(0) }
    var isBgmPlaying by remember { mutableStateOf(false) }
    
    // ===== 歌词显示 =====
    var currentLrcData by remember { mutableStateOf<LrcData?>(null) }
    var currentLrcLineIndex by remember { mutableIntStateOf(-1) }
    var bgmCurrentPosition by remember { mutableLongStateOf(0L) }
    
    // 解析 LRC 文本（必须定义在 loadLrcForCurrentBgm 之前）
    fun parseLrcText(text: String): LrcData? {
        val lines = mutableListOf<LrcLine>()
        val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        
        text.lines().forEach { line ->
            timeRegex.find(line)?.let { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0
                val seconds = match.groupValues[2].toLongOrNull() ?: 0
                val millis = match.groupValues[3].let {
                    if (it.length == 2) it.toLong() * 10 else it.toLong()
                }
                val lyricText = match.groupValues[4].trim()
                
                if (lyricText.isNotEmpty()) {
                    val startTime = minutes * 60000 + seconds * 1000 + millis
                    lines.add(LrcLine(startTime = startTime, endTime = startTime + 5000, text = lyricText))
                }
            }
        }
        
        // 计算结束时间
        for (i in 0 until lines.size - 1) {
            lines[i] = lines[i].copy(endTime = lines[i + 1].startTime)
        }
        
        return if (lines.isNotEmpty()) LrcData(lines = lines) else null
    }
    
    // 加载当前 BGM 的 LRC 数据
    fun loadLrcForCurrentBgm(bgmIndex: Int) {
        if (!config.bgmShowLyrics) {
            currentLrcData = null
            return
        }
        
        val bgmItem = config.bgmPlaylist.getOrNull(bgmIndex) ?: return
        val lrcPath = bgmItem.lrcAssetPath ?: return
        
        try {
            val lrcAssetPath = lrcPath.removePrefix("assets/")
            val lrcText = context.assets.open(lrcAssetPath).bufferedReader().readText()
            currentLrcData = parseLrcText(lrcText)
            currentLrcLineIndex = -1
            android.util.Log.d("ShellActivity", "LRC 加载成功: $lrcPath, ${currentLrcData?.lines?.size} 行")
        } catch (e: Exception) {
            android.util.Log.e("ShellActivity", "加载 LRC 失败: $lrcPath", e)
            currentLrcData = null
        }
    }
    
    // 初始化并播放 BGM
    LaunchedEffect(config.bgmEnabled) {
        if (config.bgmEnabled && config.bgmPlaylist.isNotEmpty()) {
            try {
                // 创建播放器
                val player = MediaPlayer()
                val firstItem = config.bgmPlaylist.first()
                val assetPath = firstItem.assetPath.removePrefix("assets/")
                
                val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                
                player.setVolume(config.bgmVolume, config.bgmVolume)
                player.isLooping = config.bgmPlayMode == "LOOP" && config.bgmPlaylist.size == 1
                
                player.setOnCompletionListener {
                    // 播放下一首
                    val nextIndex = when (config.bgmPlayMode) {
                        "SHUFFLE" -> (0 until config.bgmPlaylist.size).random()
                        "SEQUENTIAL" -> if (currentBgmIndex + 1 < config.bgmPlaylist.size) currentBgmIndex + 1 else -1
                        else -> (currentBgmIndex + 1) % config.bgmPlaylist.size // LOOP
                    }
                    
                    if (nextIndex >= 0 && nextIndex < config.bgmPlaylist.size) {
                        currentBgmIndex = nextIndex
                        try {
                            player.reset()
                            val nextItem = config.bgmPlaylist[nextIndex]
                            val nextAssetPath = nextItem.assetPath.removePrefix("assets/")
                            val nextAfd = context.assets.openFd(nextAssetPath)
                            player.setDataSource(nextAfd.fileDescriptor, nextAfd.startOffset, nextAfd.length)
                            nextAfd.close()
                            player.prepare()
                            player.start()
                            
                            // 加载新歌曲的歌词
                            loadLrcForCurrentBgm(nextIndex)
                        } catch (e: Exception) {
                            android.util.Log.e("ShellActivity", "播放下一首 BGM 失败", e)
                        }
                    }
                }
                
                player.prepare()
                
                // 自动播放
                if (config.bgmAutoPlay) {
                    player.start()
                    isBgmPlaying = true
                }
                
                bgmPlayer = player
                
                // 加载第一首歌的歌词
                loadLrcForCurrentBgm(0)
                
                android.util.Log.d("ShellActivity", "BGM 播放器初始化成功: ${firstItem.name}")
            } catch (e: Exception) {
                android.util.Log.e("ShellActivity", "初始化 BGM 播放器失败", e)
            }
        }
    }
    
    // 更新歌词显示（追踪播放进度）
    LaunchedEffect(isBgmPlaying, currentLrcData) {
        if (!isBgmPlaying || currentLrcData == null) return@LaunchedEffect
        
        while (isBgmPlaying && currentLrcData != null) {
            bgmPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        bgmCurrentPosition = mp.currentPosition.toLong()
                        
                        // 查找当前应显示的歌词行
                        val lrcData = currentLrcData
                        if (lrcData != null) {
                            val newIndex = lrcData.lines.indexOfLast { it.startTime <= bgmCurrentPosition }
                            if (newIndex != currentLrcLineIndex) {
                                currentLrcLineIndex = newIndex
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 忽略播放器状态异常
                }
            }
            delay(100)
        }
    }
    
    // 清理 BGM 播放器
    DisposableEffect(Unit) {
        onDispose {
            bgmPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            bgmPlayer = null
        }
    }

    // WebView回调
    val webViewCallbacks = remember {
        object : WebViewCallbacks {
            override fun onPageStarted(url: String?) {
                isLoading = true
                currentUrl = url ?: ""
                com.webtoapp.core.shell.ShellLogger.logWebView("开始加载", url ?: "")
            }

            override fun onPageFinished(url: String?) {
                isLoading = false
                currentUrl = url ?: ""
                com.webtoapp.core.shell.ShellLogger.logWebView("加载完成", url ?: "")
                webViewRef?.let {
                    canGoBack = it.canGoBack()
                    canGoForward = it.canGoForward()
                    
                    // 注入自动翻译脚本
                    if (config.translateEnabled) {
                        injectTranslateScript(it, config.translateTargetLanguage, config.translateShowButton)
                    }
                    
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
                com.webtoapp.core.shell.ShellLogger.logWebView("加载错误", currentUrl, "errorCode=$errorCode, description=$description")
            }

            override fun onSslError(error: String) {
                errorMessage = "SSL安全错误"
                com.webtoapp.core.shell.ShellLogger.logWebView("SSL错误", currentUrl, error)
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
                (context as? ShellActivity)?.handleGeolocationPermission(origin, callback)
                    ?: callback?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // 通过Activity请求Android系统权限（摄像头、麦克风等）
                request?.let { req ->
                    (context as? ShellActivity)?.handlePermissionRequest(req)
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
                // 检查并请求存储权限后下载
                (context as? ShellActivity)?.handleDownloadWithPermission(
                    url, userAgent, contentDisposition, mimeType, contentLength
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
        }
    }

    // 转换配置（包含用户脚本）
    val webViewConfig = WebViewConfig(
        javaScriptEnabled = config.webViewConfig.javaScriptEnabled,
        domStorageEnabled = config.webViewConfig.domStorageEnabled,
        zoomEnabled = config.webViewConfig.zoomEnabled,
        desktopMode = config.webViewConfig.desktopMode,
        userAgent = config.webViewConfig.userAgent,
        downloadEnabled = true, // 确保下载功能始终启用
        injectScripts = config.webViewConfig.injectScripts.map { shellScript ->
            UserScript(
                name = shellScript.name,
                code = shellScript.code,
                enabled = shellScript.enabled,
                runAt = try {
                    ScriptRunTime.valueOf(shellScript.runAt)
                } catch (e: Exception) {
                    ScriptRunTime.DOCUMENT_END
                }
            )
        }
    )

    val webViewManager = remember { 
        com.webtoapp.core.webview.WebViewManager(context, adBlocker)
    }

    // 是否隐藏工具栏（全屏模式）
    val hideToolbar = config.webViewConfig.hideToolbar

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
    
    Scaffold(
        // 在沉浸式模式下，不添加任何内边距
        contentWindowInsets = if (hideToolbar) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        modifier = Modifier.imePadding(),
        topBar = {
            if (!hideToolbar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle.ifEmpty { config.appName },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            if (currentUrl.isNotEmpty()) {
                                Text(
                                    text = currentUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.Default.ArrowBack, "后退")
                        }
                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.Default.ArrowForward, "前进")
                        }
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, "刷新")
                        }
                    }
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
            hideToolbar && config.webViewConfig.showStatusBarInFullscreen -> {
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
            if (!isActivationChecked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // 未激活提示
            else if (!isActivated && config.activationEnabled) {
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
                        Text("请先激活应用")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showActivationDialog = true }) {
                            Text("输入激活码")
                        }
                    }
                }
            } else if (forcedRunBlocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(forcedRunBlockedMessage)
                    }
                }
            } else if (config.appType == "IMAGE" || config.appType == "VIDEO") {
                // 单媒体应用模式
                MediaContentDisplay(
                    isVideo = config.appType == "VIDEO",
                    mediaConfig = config.mediaConfig
                )
            } else if (config.appType == "HTML") {
                // HTML应用模式 - 加载嵌入在 APK assets 中的 HTML 文件
                val htmlEntryFile = config.htmlConfig.getValidEntryFile()
                val htmlUrl = "file:///android_asset/html/$htmlEntryFile"
                
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            // 先调用 configureWebView 进行基础配置
                            webViewManager.configureWebView(
                                this,
                                webViewConfig,
                                webViewCallbacks,
                                config.extensionModuleIds,
                                config.embeddedExtensionModules
                            )
                            // 然后覆盖 HTML 应用特定的设置（必须在 configureWebView 之后）
                            // 因为 configureWebView 会将 allowFileAccessFromFileURLs 设为 false
                            settings.apply {
                                javaScriptEnabled = config.htmlConfig.enableJavaScript
                                domStorageEnabled = config.htmlConfig.enableLocalStorage
                                allowFileAccess = true
                                allowContentAccess = true
                                // 允许本地文件访问（HTML中的相对路径资源，如 JS/CSS 文件）
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = true
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = true
                                // 允许混合内容（HTTPS 页面加载 HTTP 资源，以及 file:// 页面访问网络）
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
                                false
                            }
                            setOnLongClickListener {
                                webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                            }
                            
                            onWebViewCreated(this)
                            webViewRef = this
                            loadUrl(htmlUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // WebView（网页应用）
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewManager.configureWebView(
                                this,
                                webViewConfig,
                                webViewCallbacks,
                                config.extensionModuleIds,
                                config.embeddedExtensionModules
                            )
                            
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
                            loadUrl(config.targetUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 歌词显示
            if (config.bgmShowLyrics && currentLrcData != null && currentLrcLineIndex >= 0) {
                val lrcTheme = config.bgmLrcTheme
                val bgColor = try {
                    Color(android.graphics.Color.parseColor(lrcTheme?.backgroundColor ?: "#80000000"))
                } catch (e: Exception) {
                    Color.Black.copy(alpha = 0.5f)
                }
                val textColor = try {
                    Color(android.graphics.Color.parseColor(lrcTheme?.highlightColor ?: "#FFD700"))
                } catch (e: Exception) {
                    Color.Yellow
                }
                
                Box(
                    modifier = Modifier
                        .align(
                            when (lrcTheme?.position) {
                                "TOP" -> Alignment.TopCenter
                                "CENTER" -> Alignment.Center
                                else -> Alignment.BottomCenter
                            }
                        )
                        .padding(16.dp)
                        .background(bgColor, shape = MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentLrcData!!.lines[currentLrcLineIndex].text,
                        color = textColor,
                        fontSize = (lrcTheme?.fontSize ?: 16f).sp
                    )
                }
            }

            // 强制运行倒计时和密码退出组件
            if (forcedRunActive && config.forcedRunConfig?.showCountdown == true) {
                ForcedRunCountdownOverlay(
                    remainingMs = forcedRunRemainingMs,
                    allowEmergencyExit = config.forcedRunConfig?.allowEmergencyExit == true,
                    emergencyPassword = config.forcedRunConfig?.emergencyPassword,
                    onEmergencyExit = {
                        forcedRunManager.stopForcedRunMode()
                        activity.finish()
                    }
                )
            }

            // 错误提示
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = if (forcedRunActive) 56.dp else 0.dp),
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
                            Text("关闭")
                        }
                    }
                }
            }
            
            // 虚拟导航栏 - 仅在强制运行模式下显示
            VirtualNavigationBar(
                visible = forcedRunActive,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBack = { webViewRef?.goBack() },
                onForward = { webViewRef?.goForward() },
                onRefresh = { webViewRef?.reload() },
                onHome = { 
                    // 返回主页
                    val homeUrl = when {
                        config.appType == "HTML" -> "file:///android_asset/html/${config.htmlConfig.getValidEntryFile()}"
                        else -> config.targetUrl
                    }
                    webViewRef?.loadUrl(homeUrl)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // 激活码对话框
    if (showActivationDialog) {
        ActivationDialog(
            onDismiss = { showActivationDialog = false },
            onActivate = { code ->
                val scope = (context as? AppCompatActivity)?.lifecycleScope
                scope?.launch {
                    val result = activation.verifyActivationCode(
                        -1L,
                        code,
                        config.activationCodes
                    )
                    when (result) {
                        is ActivationResult.Success -> {
                            isActivated = true
                            showActivationDialog = false
                            // 检查公告
                            if (config.announcementEnabled && config.announcementTitle.isNotEmpty()) {
                                val ann = Announcement(
                                    title = config.announcementTitle,
                                    content = config.announcementContent,
                                    linkUrl = config.announcementLink.ifEmpty { null },
                                    showOnce = config.announcementShowOnce
                                )
                                showAnnouncementDialog = announcement.shouldShowAnnouncement(-1L, ann)
                            }
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    // 公告对话框 - 使用模板系统
    if (showAnnouncementDialog && config.announcementTitle.isNotEmpty()) {
        // 构建 Announcement 对象
        val shellAnnouncement = com.webtoapp.data.model.Announcement(
            title = config.announcementTitle,
            content = config.announcementContent,
            linkUrl = config.announcementLink.ifEmpty { null },
            linkText = config.announcementLinkText.ifEmpty { null },
            template = try {
                com.webtoapp.data.model.AnnouncementTemplateType.valueOf(config.announcementTemplate)
            } catch (e: Exception) {
                com.webtoapp.data.model.AnnouncementTemplateType.XIAOHONGSHU
            },
            showEmoji = config.announcementShowEmoji,
            animationEnabled = config.announcementAnimationEnabled
        )
        
        com.webtoapp.ui.components.announcement.AnnouncementDialog(
            config = com.webtoapp.ui.components.announcement.AnnouncementConfig(
                announcement = shellAnnouncement,
                template = com.webtoapp.ui.components.announcement.AnnouncementTemplate.valueOf(
                    shellAnnouncement.template.name
                ),
                showEmoji = shellAnnouncement.showEmoji,
                animationEnabled = shellAnnouncement.animationEnabled
            ),
            onDismiss = {
                showAnnouncementDialog = false
                val scope = (context as? AppCompatActivity)?.lifecycleScope
                scope?.launch {
                    announcement.markAnnouncementShown(-1L, 1)
                }
            },
            onLinkClick = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        )
    }
    
    // 强制运行权限引导对话框
    if (showForcedRunPermissionDialog && config.forcedRunConfig != null) {
        ForcedRunPermissionDialog(
            protectionLevel = config.forcedRunConfig.protectionLevel,
            onDismiss = { 
                showForcedRunPermissionDialog = false 
            },
            onContinueAnyway = {
                // 用户选择跳过，降级防护继续使用
                showForcedRunPermissionDialog = false
                android.util.Log.w("ShellActivity", "用户跳过权限授权，强制运行防护将降级")
            },
            onAllPermissionsGranted = {
                // 所有权限已授权
                showForcedRunPermissionDialog = false
                android.util.Log.d("ShellActivity", "强制运行权限已全部授权")
                // 重新启动强制运行以应用新权限
                if (forcedRunActive) {
                    forcedRunManager.stopForcedRunMode()
                    config.forcedRunConfig?.let { cfg ->
                        forcedRunManager.startForcedRunMode(cfg, -1L)
                    }
                }
            }
        )
    }

    // 启动画面覆盖层（在 Box 内，覆盖在 Scaffold 之上）
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
            // 播放完成回调（始终需要）
            onComplete = closeSplash
        )
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
    
    // 状态栏背景覆盖层（在全屏模式下显示状态栏时）
    // 放在 Box 内部最上层，覆盖在所有内容之上，使用 align 固定在顶部
    if (hideToolbar && config.webViewConfig.showStatusBarInFullscreen) {
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
}

/**
 * Shell 模式启动画面覆盖层（从 assets 加载媒体，支持视频裁剪）
 */
@Composable
fun ShellSplashOverlay(
    splashType: String,
    countdown: Int,
    videoStartMs: Long = 0,
    videoEndMs: Long = 5000,
    fillScreen: Boolean = true,
    enableAudio: Boolean = false,    // 是否启用视频音频
    onSkip: (() -> Unit)?,           // 点击跳过回调
    onComplete: (() -> Unit)? = null // 播放完成回调
) {
    val context = LocalContext.current
    val extension = if (splashType == "VIDEO") "mp4" else "png"
    val assetPath = "splash_media.$extension"
    val videoDurationMs = videoEndMs - videoStartMs
    val contentScaleMode = if (fillScreen) ContentScale.Crop else ContentScale.Fit
    
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
        when (splashType) {
            "IMAGE" -> {
                // 图片启动画面（从 assets 加载）
                // 使用 file:///android_asset/ 前缀加载 assets 中的图片
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data("file:///android_asset/$assetPath")
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "启动画面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScaleMode
                )
            }
            "VIDEO" -> {
                // 视频启动画面（支持裁剪播放，支持加密）
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }
                var tempVideoFile by remember { mutableStateOf<java.io.File?>(null) }
                
                // 监控播放进度
                // 仅在播放器准备就绪后开始监控
                LaunchedEffect(isPlayerReady) {
                    if (!isPlayerReady) return@LaunchedEffect
                    mediaPlayer?.let { mp ->
                        // 等待播放器真正开始播放
                        while (!mp.isPlaying) {
                            delay(50)
                            if (mediaPlayer == null) return@LaunchedEffect
                        }
                        // 监控播放进度并更新剩余时间
                        while (mp.isPlaying) {
                            val currentPos = mp.currentPosition
                            // 更新剩余时间用于倒计时显示
                            videoRemainingMs = (videoEndMs - currentPos).coerceAtLeast(0L)
                            if (currentPos >= videoEndMs) {
                                mp.pause()
                                // 使用 onComplete 回调，因为这是播放完成
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
                                        // 检查是否存在加密版本
                                        val encryptedPath = "$assetPath.enc"
                                        val hasEncrypted = try {
                                            ctx.assets.open(encryptedPath).use { true }
                                        } catch (e: Exception) { false }
                                        
                                        if (hasEncrypted) {
                                            // 加密视频：解密到临时文件后播放
                                            android.util.Log.d("ShellSplash", "检测到加密启动画面视频")
                                            val decryptor = com.webtoapp.core.crypto.AssetDecryptor(ctx)
                                            val decryptedData = decryptor.loadAsset(assetPath)
                                            val tempFile = java.io.File(ctx.cacheDir, "splash_video_${System.currentTimeMillis()}.mp4")
                                            tempFile.writeBytes(decryptedData)
                                            tempVideoFile = tempFile
                                            
                                            mediaPlayer = android.media.MediaPlayer().apply {
                                                setDataSource(tempFile.absolutePath)
                                                setSurface(holder.surface)
                                                val volume = if (enableAudio) 1f else 0f
                                                setVolume(volume, volume)
                                                isLooping = false
                                                setOnPreparedListener {
                                                    seekTo(videoStartMs.toInt())
                                                    start()
                                                    isPlayerReady = true
                                                }
                                                setOnCompletionListener { onComplete?.invoke() }
                                                prepareAsync()
                                            }
                                        } else {
                                            // 非加密视频：直接使用 openFd
                                            val afd = ctx.assets.openFd(assetPath)
                                            mediaPlayer = android.media.MediaPlayer().apply {
                                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                                setSurface(holder.surface)
                                                val volume = if (enableAudio) 1f else 0f
                                                setVolume(volume, volume)
                                                isLooping = false
                                                setOnPreparedListener {
                                                    seekTo(videoStartMs.toInt())
                                                    start()
                                                    isPlayerReady = true
                                                }
                                                setOnCompletionListener { onComplete?.invoke() }
                                                prepareAsync()
                                            }
                                            afd.close()
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
                
                DisposableEffect(Unit) {
                    onDispose {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        // 清理临时文件
                        tempVideoFile?.delete()
                        tempVideoFile = null
                    }
                }
            }
        }

        // 倒计时/跳过提示
        // 视频使用动态剩余时间，图片使用传入的 countdown
        val displayTime = if (splashType == "VIDEO") ((videoRemainingMs + 999) / 1000).toInt() else countdown
        
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
                        text = "跳过",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 媒体内容显示组件（Shell 模式下的图片/视频展示）
 */
@Composable
fun MediaContentDisplay(
    isVideo: Boolean,
    mediaConfig: com.webtoapp.core.shell.MediaShellConfig
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            // 视频播放（支持加密）
            var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            var tempVideoFile by remember { mutableStateOf<java.io.File?>(null) }
            val assetPath = "media_content.mp4"
            
            AndroidView(
                factory = { ctx ->
                    android.view.SurfaceView(ctx).apply {
                        holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                try {
                                    // 检查是否存在加密版本
                                    val encryptedPath = "$assetPath.enc"
                                    val hasEncrypted = try {
                                        ctx.assets.open(encryptedPath).use { true }
                                    } catch (e: Exception) { false }
                                    
                                    if (hasEncrypted) {
                                        // 加密视频：解密到临时文件后播放
                                        android.util.Log.d("MediaContent", "检测到加密媒体视频")
                                        val decryptor = com.webtoapp.core.crypto.AssetDecryptor(ctx)
                                        val decryptedData = decryptor.loadAsset(assetPath)
                                        val tempFile = java.io.File(ctx.cacheDir, "media_video_${System.currentTimeMillis()}.mp4")
                                        tempFile.writeBytes(decryptedData)
                                        tempVideoFile = tempFile
                                        
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(tempFile.absolutePath)
                                            setSurface(holder.surface)
                                            val volume = if (mediaConfig.enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = mediaConfig.loop
                                            setOnPreparedListener {
                                                if (mediaConfig.autoPlay) start()
                                            }
                                            prepareAsync()
                                        }
                                    } else {
                                        // 非加密视频：直接使用 openFd
                                        val afd = ctx.assets.openFd(assetPath)
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                            setSurface(holder.surface)
                                            val volume = if (mediaConfig.enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = mediaConfig.loop
                                            setOnPreparedListener {
                                                if (mediaConfig.autoPlay) start()
                                            }
                                            prepareAsync()
                                        }
                                        afd.close()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                            
                            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                mediaPlayer?.release()
                                mediaPlayer = null
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            DisposableEffect(Unit) {
                onDispose {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    // 清理临时文件
                    tempVideoFile?.delete()
                    tempVideoFile = null
                }
            }
        } else {
            // 图片显示
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data("file:///android_asset/media_content.png")
                    .crossfade(true)
                    .build()
            )
            
            Image(
                painter = painter,
                contentDescription = "媒体内容",
                modifier = Modifier.fillMaxSize(),
                contentScale = if (mediaConfig.fillScreen) 
                    androidx.compose.ui.layout.ContentScale.Crop 
                else 
                    androidx.compose.ui.layout.ContentScale.Fit
            )
        }
    }
}

/**
 * 注入网页自动翻译脚本
 * 使用Native桥接调用Google Translate API，避免CORS限制
 */
private fun injectTranslateScript(webView: android.webkit.WebView, targetLanguage: String, showButton: Boolean) {
    val translateScript = """
        (function() {
            if (window._translateInjected) return;
            window._translateInjected = true;
            
            var targetLang = '$targetLanguage';
            var showBtn = $showButton;
            var pendingCallbacks = {};
            var callbackIdCounter = 0;
            
            // Native翻译回调处理
            window._translateCallback = function(callbackId, resultsJson, error) {
                var cb = pendingCallbacks[callbackId];
                if (cb) {
                    delete pendingCallbacks[callbackId];
                    if (error) {
                        cb.reject(error);
                    } else {
                        try {
                            cb.resolve(JSON.parse(resultsJson));
                        } catch(e) {
                            cb.reject(e.message);
                        }
                    }
                }
            };
            
            // 调用Native翻译
            function nativeTranslate(texts) {
                return new Promise(function(resolve, reject) {
                    var callbackId = 'cb_' + (++callbackIdCounter);
                    pendingCallbacks[callbackId] = { resolve: resolve, reject: reject };
                    
                    if (window._nativeTranslate && window._nativeTranslate.translate) {
                        window._nativeTranslate.translate(JSON.stringify(texts), targetLang, callbackId);
                    } else {
                        // 降级：使用fetch（可能有CORS问题）
                        fallbackTranslate(texts, callbackId);
                    }
                });
            }
            
            // 降级翻译方案
            function fallbackTranslate(texts, callbackId) {
                var url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + targetLang + '&dt=t&q=' + encodeURIComponent(texts.join('\n'));
                fetch(url)
                    .then(function(r) { return r.json(); })
                    .then(function(data) {
                        if (data && data[0]) {
                            var translations = data[0].map(function(item) { return item[0]; });
                            var combined = translations.join('').split('\n');
                            window._translateCallback(callbackId, JSON.stringify(combined), null);
                        } else {
                            window._translateCallback(callbackId, null, 'Invalid response');
                        }
                    })
                    .catch(function(e) {
                        window._translateCallback(callbackId, null, e.message);
                    });
            }
            
            // 创建翻译按钮
            if (showBtn) {
                var btn = document.createElement('div');
                btn.id = '_translate_btn';
                btn.innerHTML = '🌐 翻译';
                btn.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:999999;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#fff;padding:12px 20px;border-radius:25px;font-size:14px;font-weight:bold;cursor:pointer;box-shadow:0 4px 15px rgba(102,126,234,0.4);transition:all 0.3s ease;';
                btn.onclick = function() { translatePage(); };
                document.body.appendChild(btn);
            }
            
            // 翻译页面函数
            async function translatePage() {
                var texts = [];
                var elements = [];
                
                // 收集需要翻译的文本节点
                var walker = document.createTreeWalker(
                    document.body,
                    NodeFilter.SHOW_TEXT,
                    { acceptNode: function(node) {
                        var parent = node.parentNode;
                        if (!parent) return NodeFilter.FILTER_REJECT;
                        var tag = parent.tagName;
                        if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT') return NodeFilter.FILTER_REJECT;
                        var text = node.textContent.trim();
                        if (text.length < 2) return NodeFilter.FILTER_REJECT;
                        if (/^[\s\d\p{P}]+$/u.test(text)) return NodeFilter.FILTER_REJECT;
                        return NodeFilter.FILTER_ACCEPT;
                    }}
                );
                
                while (walker.nextNode()) {
                    var text = walker.currentNode.textContent.trim();
                    if (text && texts.indexOf(text) === -1) {
                        texts.push(text);
                        elements.push(walker.currentNode);
                    }
                }
                
                if (texts.length === 0) return;
                
                // 更新按钮状态
                if (showBtn) {
                    var btn = document.getElementById('_translate_btn');
                    if (btn) btn.innerHTML = '⏳ 翻译中...';
                }
                
                // 分批翻译
                var batchSize = 20;
                for (var i = 0; i < texts.length; i += batchSize) {
                    var batch = texts.slice(i, i + batchSize);
                    var batchElements = elements.slice(i, i + batchSize);
                    
                    try {
                        var results = await nativeTranslate(batch);
                        for (var j = 0; j < batchElements.length && j < results.length; j++) {
                            if (results[j] && results[j].trim()) {
                                batchElements[j].textContent = results[j];
                            }
                        }
                    } catch(e) {
                        console.log('Translate batch error:', e);
                    }
                }
                
                if (showBtn) {
                    var btn = document.getElementById('_translate_btn');
                    if (btn) btn.innerHTML = '✅ 已翻译';
                }
            }
            
            // 自动翻译
            setTimeout(translatePage, 1500);
        })();
    """.trimIndent()
    
    webView.evaluateJavascript(translateScript, null)
}
