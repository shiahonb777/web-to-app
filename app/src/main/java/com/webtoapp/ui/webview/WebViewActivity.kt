package com.webtoapp.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.activation.ActivationResult
import com.webtoapp.core.bgm.BgmPlayer
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.core.webview.WebViewManager
import com.webtoapp.data.model.SplashConfig
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.data.model.SplashType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.getActivationCodeStrings
import android.content.pm.ActivityInfo
import androidx.activity.enableEdgeToEdge
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

    private fun applyImmersiveFullscreen(enabled: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            if (enabled) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
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
        super.onCreate(savedInstanceState)
        
        // 立即启用边到边全屏模式
        enableEdgeToEdge()

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1)
        val directUrl = intent.getStringExtra(EXTRA_URL)

        setContent {
            WebToAppTheme {
                WebViewScreen(
                    appId = appId,
                    directUrl = directUrl,
                    onWebViewCreated = { webView = it },
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

    // 加载应用配置
    LaunchedEffect(appId, directUrl) {
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
                    val activated = activation.isActivated(appId).first()
                    isActivated = activated
                    isActivationChecked = true
                    if (!activated) {
                        showActivationDialog = true
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
                errorMessage = "SSL安全错误"
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
                DownloadHelper.handleDownload(
                    context = context,
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER
                )
            }
        }
    }

    val webViewManager = remember { WebViewManager(context, adBlocker) }
    
    // 检查是否为画廊模式
    val isGalleryMode = remember(webApp) {
        webApp?.galleryConfig?.items?.isNotEmpty() == true
    }
    
    // 根据应用类型构建目标 URL
    val targetUrl = remember(directUrl, webApp) {
        val app = webApp  // 捕获到局部变量以支持智能转换
        when {
            !directUrl.isNullOrBlank() -> directUrl
            // 画廊模式不使用单一URL
            app?.galleryConfig?.items?.isNotEmpty() == true -> ""
            app?.appType == com.webtoapp.data.model.AppType.HTML -> {
                // HTML 应用：从本地文件目录加载
                val projectId = app.htmlConfig?.projectId ?: ""
                val entryFile = app.htmlConfig?.entryFile ?: "index.html"
                val htmlDir = File(context.filesDir, "html_projects/$projectId")
                "file://${htmlDir.absolutePath}/$entryFile"
            }
            else -> app?.url ?: ""
        }
    }
    
    // 是否隐藏工具栏（全屏模式或画廊模式）
    // 画廊模式下隐藏外层工具栏，因为画廊组件有自己的工具栏
    val hideToolbar = webApp?.webViewConfig?.hideToolbar == true || isGalleryMode

    LaunchedEffect(hideToolbar) {
        onFullscreenModeChanged(hideToolbar)
    }

    Scaffold(
        modifier = if (hideToolbar) Modifier.fillMaxSize() else Modifier,
        topBar = {
            if (!hideToolbar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle.ifEmpty { webApp?.name ?: "WebApp" },
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
                    navigationIcon = {
                        IconButton(onClick = { (context as? AppCompatActivity)?.finish() }) {
                            Icon(Icons.Default.Close, "关闭")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hideToolbar) Modifier else Modifier.padding(padding))
        ) {
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
                        Text("请先激活应用")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showActivationDialog = true }) {
                            Text("输入激活码")
                        }
                    }
                }
            } else if (isGalleryMode && isActivationChecked && webApp?.galleryConfig != null) {
                // 画廊模式：根据类型选择查看器
                val galleryItems = webApp!!.galleryConfig!!.items
                val isHtmlGallery = galleryItems.isNotEmpty() && 
                    galleryItems.first().type == com.webtoapp.data.model.GalleryItemType.HTML
                
                if (isHtmlGallery) {
                    // HTML画廊
                    com.webtoapp.ui.components.gallery.HtmlGalleryViewer(
                        config = webApp!!.galleryConfig!!,
                        onClose = { (context as? AppCompatActivity)?.finish() }
                    )
                } else {
                    // 网页画廊
                    com.webtoapp.ui.components.gallery.WebGalleryViewer(
                        config = webApp!!.galleryConfig!!,
                        onClose = { (context as? AppCompatActivity)?.finish() }
                    )
                }
            } else if (targetUrl.isNotEmpty() && isActivationChecked) {
                // 单网址模式：WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewManager.configureWebView(
                                this,
                                webApp?.webViewConfig ?: com.webtoapp.data.model.WebViewConfig(),
                                webViewCallbacks,
                                webApp?.extensionModuleIds ?: emptyList()
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
                            onWebViewCreated(this)
                            webViewRef = this
                            loadUrl(targetUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }

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
        title = { Text("激活应用") },
        text = {
            Column {
                Text("请输入激活码以继续使用")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                    },
                    label = { Text("激活码") },
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
                        error = "请输入激活码"
                    } else {
                        onActivate(code)
                    }
                }
            ) {
                Text("激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
                    contentDescription = "启动画面",
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
                        text = "跳过",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
