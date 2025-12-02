package com.webtoapp.ui.shell

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
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
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.data.model.Announcement
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.UserScript
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.ui.webview.ActivationDialog
import com.webtoapp.ui.webview.AnnouncementDialog
import kotlinx.coroutines.delay
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

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = WebToAppApplication.shellMode.getConfig()
        if (config == null) {
            finish()
            return
        }

        setContent {
            WebToAppTheme {
                ShellScreen(
                    config = config,
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
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun hideCustomView() {
        customView?.let { view ->
            val decorView = window.decorView as FrameLayout
            decorView.removeView(view)
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
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
fun ShellScreen(
    config: ShellConfig,
    onWebViewCreated: (WebView) -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val activation = WebToAppApplication.activation
    val announcement = WebToAppApplication.announcement
    val adBlocker = WebToAppApplication.adBlock

    // 状态
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var isActivated by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // 同步检查启动画面配置（必须在 WebView 初始化之前）
    val splashMediaExists = remember {
        if (config.splashEnabled) {
            val extension = if (config.splashType == "VIDEO") "mp4" else "png"
            try {
                context.assets.open("splash_media.$extension").close()
                android.util.Log.d("ShellActivity", "同步检查: 启动画面媒体存在")
                true
            } catch (e: Exception) {
                android.util.Log.e("ShellActivity", "同步检查: 启动画面媒体不存在", e)
                false
            }
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

    // 初始化配置
    LaunchedEffect(Unit) {
        // 配置广告拦截
        if (config.adBlockEnabled) {
            adBlocker.initialize(config.adBlockRules, useDefaultRules = true)
            adBlocker.setEnabled(true)
        }

        // 检查激活状态
        if (config.activationEnabled) {
            // Shell 模式使用固定 ID
            isActivated = activation.getActivationStatus(-1L)
            if (!isActivated) {
                showActivationDialog = true
            }
        }

        // 检查公告
        if (config.announcementEnabled && isActivated && config.announcementTitle.isNotEmpty()) {
            val ann = Announcement(
                title = config.announcementTitle,
                content = config.announcementContent,
                linkUrl = config.announcementLink.ifEmpty { null }
            )
            showAnnouncementDialog = announcement.shouldShowAnnouncement(-1L, ann)
        }

        // 启动画面已在同步初始化阶段处理
        android.util.Log.d("ShellActivity", "LaunchedEffect: showSplash=$showSplash, splashCountdown=$splashCountdown")
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

    // WebView回调
    val webViewCallbacks = remember {
        object : WebViewCallbacks {
            override fun onPageStarted(url: String?) {
                isLoading = true
                currentUrl = url ?: ""
            }

            override fun onPageFinished(url: String?) {
                isLoading = false
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
                callback?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onShowFileChooser(
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                return onFileChooser(filePathCallback, fileChooserParams)
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
    Box(modifier = Modifier.fillMaxSize()) {
    
    Scaffold(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

            // 未激活提示
            if (!isActivated && config.activationEnabled) {
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
            } else if (config.appType == "IMAGE" || config.appType == "VIDEO") {
                // 媒体应用模式
                MediaContentDisplay(
                    isVideo = config.appType == "VIDEO",
                    mediaConfig = config.mediaConfig
                )
            } else {
                // WebView
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
                                webViewCallbacks
                            )
                            onWebViewCreated(this)
                            webViewRef = this
                            loadUrl(config.targetUrl)
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
                                    linkUrl = config.announcementLink.ifEmpty { null }
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

    // 公告对话框
    if (showAnnouncementDialog && config.announcementTitle.isNotEmpty()) {
        AnnouncementDialog(
            title = config.announcementTitle,
            content = config.announcementContent,
            linkUrl = config.announcementLink.ifEmpty { null },
            linkText = null,
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
                // 视频启动画面（支持裁剪播放）
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }
                
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
                                        val afd = ctx.assets.openFd(assetPath)
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                            setSurface(holder.surface)
                                            // 根据配置决定是否启用音频
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
            // 视频播放
            var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            
            AndroidView(
                factory = { ctx ->
                    android.view.SurfaceView(ctx).apply {
                        holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                try {
                                    val afd = ctx.assets.openFd("media_content.mp4")
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
