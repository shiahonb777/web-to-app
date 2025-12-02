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
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.core.webview.WebViewManager
import com.webtoapp.data.model.SplashConfig
import com.webtoapp.data.model.SplashType
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.theme.WebToAppTheme
import kotlinx.coroutines.delay
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

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
fun WebViewScreen(
    appId: Long,
    directUrl: String?,
    onWebViewCreated: (WebView) -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit
) {
    val context = LocalContext.current
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
    var isActivated by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 启动画面状态
    var showSplash by remember { mutableStateOf(false) }
    var splashCountdown by remember { mutableIntStateOf(0) }

    // WebView引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 加载应用配置
    LaunchedEffect(appId) {
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
                    isActivated = activation.getActivationStatus(appId)
                    if (!isActivated) {
                        showActivationDialog = true
                    }
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
                    }
                }
            }
        }
    }

    // 启动画面倒计时
    LaunchedEffect(showSplash, splashCountdown) {
        if (showSplash && splashCountdown > 0) {
            delay(1000L)
            splashCountdown--
        } else if (showSplash && splashCountdown <= 0) {
            showSplash = false
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

    val webViewManager = remember { WebViewManager(context, adBlocker) }
    val targetUrl = directUrl ?: webApp?.url ?: ""
    
    // 是否隐藏工具栏（全屏模式）
    val hideToolbar = webApp?.webViewConfig?.hideToolbar == true

    Scaffold(
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
            if (!isActivated && webApp?.activationEnabled == true) {
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
            } else if (targetUrl.isNotEmpty()) {
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
                                webApp?.webViewConfig ?: com.webtoapp.data.model.WebViewConfig(),
                                webViewCallbacks
                            )
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
        ActivationDialog(
            onDismiss = { showActivationDialog = false },
            onActivate = { code ->
                val scope = (context as? AppCompatActivity)?.lifecycleScope
                scope?.launch {
                    val result = activation.verifyActivationCode(
                        appId,
                        code,
                        webApp?.activationCodes ?: emptyList()
                    )
                    when (result) {
                        is ActivationResult.Success -> {
                            isActivated = true
                            showActivationDialog = false
                            // 检查公告
                            if (webApp?.announcementEnabled == true) {
                                val shouldShow = announcement.shouldShowAnnouncement(appId, webApp?.announcement)
                                showAnnouncementDialog = shouldShow
                            }
                        }
                        else -> {
                            // 显示错误
                        }
                    }
                }
            }
        )
    }

    // 公告对话框
    if (showAnnouncementDialog && webApp?.announcement != null) {
        val ann = webApp!!.announcement!!
        AnnouncementDialog(
            title = ann.title,
            content = ann.content,
            linkUrl = ann.linkUrl,
            linkText = ann.linkText,
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
                onSkip = if (splashConfig.clickToSkip) {
                    { showSplash = false }
                } else null
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

@Composable
fun AnnouncementDialog(
    title: String,
    content: String,
    linkUrl: String?,
    linkText: String?,
    onDismiss: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(content)
                if (!linkUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onLinkClick(linkUrl) }) {
                        Text(linkText ?: "查看详情")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("我知道了")
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
    onSkip: (() -> Unit)?
) {
    val context = LocalContext.current
    val mediaPath = splashConfig.mediaPath ?: return

    // 视频裁剪相关
    val videoStartMs = splashConfig.videoStartMs
    val videoEndMs = splashConfig.videoEndMs
    val videoDurationMs = videoEndMs - videoStartMs

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
            )
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
                    contentScale = ContentScale.Crop
                )
            }
            SplashType.VIDEO -> {
                // 视频启动画面 - 支持裁剪播放
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                
                // 监控播放进度，到达结束时间时停止
                LaunchedEffect(mediaPlayer) {
                    mediaPlayer?.let { mp ->
                        while (mp.isPlaying) {
                            if (mp.currentPosition >= videoEndMs) {
                                mp.pause()
                                onSkip?.invoke()
                                break
                            }
                            delay(100)
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
                                            setVolume(0f, 0f) // 静音
                                            isLooping = false
                                            setOnPreparedListener { 
                                                // 跳到裁剪起始位置
                                                seekTo(videoStartMs.toInt())
                                                start() 
                                            }
                                            setOnCompletionListener { onSkip?.invoke() }
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        onSkip?.invoke()
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
        val displayCountdown = if (splashConfig.type == SplashType.VIDEO) {
            // 视频使用裁剪时长
            ((videoDurationMs - (countdown * 1000L).coerceAtMost(videoDurationMs)) / 1000).toInt()
                .let { (videoDurationMs / 1000).toInt() - it }
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
                if (displayCountdown > 0 || countdown > 0) {
                    Text(
                        text = "${if (splashConfig.type == SplashType.VIDEO) 
                            (videoDurationMs / 1000).toInt() else countdown}s",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (onSkip != null) {
                    if (displayCountdown > 0 || countdown > 0) {
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
