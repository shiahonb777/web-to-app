package com.webtoapp.ui.shell

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.components.EdgeSwipeRefreshLayout

/**
 * 根据 appType 路由到不同的内容显示组件
 *
 * 支持的类型：IMAGE, VIDEO, GALLERY, WORDPRESS, NODEJS_APP, PHP_APP,
 * PYTHON_APP, GO_APP, HTML, FRONTEND, 以及默认的 WEB 模式
 */
@Composable
fun ShellContentRouter(
    appType: String,
    config: ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    deepLinkUrl: String?,
    // 下拉刷新
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onActivityFinish: () -> Unit
) {
    when {
        appType == "IMAGE" || appType == "VIDEO" -> {
            // 单媒体应用模式
            MediaContentDisplay(
                isVideo = appType == "VIDEO",
                mediaConfig = config.mediaConfig
            )
        }
        appType == "GALLERY" -> {
            // Gallery 画廊应用模式
            AppLogger.d("ShellScreen", "进入 GALLERY 分支，显示 ShellGalleryPlayer")
            ShellGalleryPlayer(
                galleryConfig = config.galleryConfig,
                onBack = onActivityFinish
            )
        }
        appType == "WORDPRESS" -> {
            // WordPress 应用模式 - 从 assets 提取到私有目录，启动 PHP 服务器
            WordPressShellMode(
                config = config,
                webViewConfig = webViewConfig,
                webViewCallbacks = webViewCallbacks,
                webViewManager = webViewManager,
                onWebViewCreated = onWebViewCreated,
                swipeRefreshEnabled = swipeRefreshEnabled,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        appType == "NODEJS_APP" -> {
            val nodejsMode = config.nodejsConfig.mode
            if (nodejsMode == "STATIC") {
                // 静态模式：直接加载 assets 中的静态文件
                ShellLocalFileWebView(
                    config = config,
                    webViewConfig = webViewConfig,
                    webViewCallbacks = webViewCallbacks,
                    webViewManager = webViewManager,
                    targetUrl = config.targetUrl,
                    enableJavaScript = true,
                    enableLocalStorage = true,
                    swipeRefreshEnabled = swipeRefreshEnabled,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onWebViewCreated = onWebViewCreated,
                    onWebViewRefUpdated = onWebViewRefUpdated
                )
            } else {
                // 后端/全栈模式：提取并启动 Node.js 服务器
                NodeJsShellMode(
                    config = config,
                    webViewConfig = webViewConfig,
                    webViewCallbacks = webViewCallbacks,
                    webViewManager = webViewManager,
                    onWebViewCreated = onWebViewCreated,
                    swipeRefreshEnabled = swipeRefreshEnabled,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
            }
        }
        appType == "PHP_APP" -> {
            PhpAppShellMode(
                config = config,
                webViewConfig = webViewConfig,
                webViewCallbacks = webViewCallbacks,
                webViewManager = webViewManager,
                onWebViewCreated = onWebViewCreated,
                swipeRefreshEnabled = swipeRefreshEnabled,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        appType == "PYTHON_APP" -> {
            AppLogger.i("ShellScreen", "进入 PYTHON_APP 分支，启动 PythonAppShellMode")
            AppLogger.i("ShellScreen", "pythonAppConfig: framework=${config.pythonAppConfig.framework}, entry=${config.pythonAppConfig.entryFile}, module=${config.pythonAppConfig.entryModule}, server=${config.pythonAppConfig.serverType}, port=${config.pythonAppConfig.port}")
            PythonAppShellMode(
                config = config,
                webViewConfig = webViewConfig,
                webViewCallbacks = webViewCallbacks,
                webViewManager = webViewManager,
                onWebViewCreated = onWebViewCreated,
                swipeRefreshEnabled = swipeRefreshEnabled,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        appType == "GO_APP" -> {
            AppLogger.i("ShellScreen", "进入 GO_APP 分支，启动 GoAppShellMode")
            GoAppShellMode(
                config = config,
                webViewConfig = webViewConfig,
                webViewCallbacks = webViewCallbacks,
                webViewManager = webViewManager,
                onWebViewCreated = onWebViewCreated,
                swipeRefreshEnabled = swipeRefreshEnabled,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        appType == "MULTI_WEB" -> {
            AppLogger.i("ShellScreen", "进入 MULTI_WEB 分支，启动 MultiWebShellMode, mode=${config.multiWebConfig.displayMode}, sites=${config.multiWebConfig.sites.size}")
            MultiWebShellMode(
                config = config,
                webViewConfig = webViewConfig,
                webViewCallbacks = webViewCallbacks,
                webViewManager = webViewManager,
                onWebViewCreated = onWebViewCreated,
                swipeRefreshEnabled = swipeRefreshEnabled,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        appType == "HTML" || appType == "FRONTEND" -> {
            // HTML/前端应用模式 - 加载嵌入在 APK assets 中的 HTML 文件
            val htmlEntryFile = config.htmlConfig.getValidEntryFile()
            val htmlUrl = "file:///android_asset/html/$htmlEntryFile"
            ShellLocalFileWebView(
                config = config,
                webViewConfig = webViewConfig,
                webViewCallbacks = webViewCallbacks,
                webViewManager = webViewManager,
                targetUrl = htmlUrl,
                enableJavaScript = config.htmlConfig.enableJavaScript,
                enableLocalStorage = config.htmlConfig.enableLocalStorage,
                swipeRefreshEnabled = swipeRefreshEnabled,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                onWebViewCreated = onWebViewCreated,
                onWebViewRefUpdated = onWebViewRefUpdated
            )
        }
        else -> {
            // WebView（网页应用）
            val resolvedUrl = normalizeShellTargetUrlForSecurity(deepLinkUrl ?: config.targetUrl)
            AppLogger.d("ShellScreen", "进入 WebView 分支 (else)，加载 URL: $resolvedUrl")
            // 用于 setOnChildScrollUpCallback 的 WebView 引用
            var swipeChildWebView: WebView? = null
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
                            swipeChildWebView?.reload()
                        }
                        setOnChildScrollUpCallback { _, child ->
                            val wv = child as? WebView ?: return@setOnChildScrollUpCallback false
                            wv.scrollY > 0
                        }
                        val createdWebView = WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewManager.configureWebView(
                                this,
                                webViewConfig,
                                webViewCallbacks,
                                config.extensionModuleIds,
                                config.embeddedExtensionModules,
                                config.extensionFabIcon, allowGlobalModuleFallback = false)

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
                                false // 不消费事件，让 WebView 继续处理
                            }
                            setOnLongClickListener {
                                webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                            }

                            onWebViewCreated(this)
                            onWebViewRefUpdated(this)
                            loadUrl(resolvedUrl)
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
                    if (!isRefreshing && swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 本地文件 WebView - 用于 HTML/前端应用和 Node.js 静态模式
 * 共享相同的文件访问权限配置和长按监听器逻辑
 */
@Composable
fun ShellLocalFileWebView(
    config: ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    targetUrl: String,
    enableJavaScript: Boolean,
    enableLocalStorage: Boolean,
    // 下拉刷新
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit
) {
    // 用于 setOnChildScrollUpCallback 的 WebView 引用
    var swipeChildWebView: WebView? = null
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
                    swipeChildWebView?.reload()
                }
                setOnChildScrollUpCallback { _, child ->
                    val wv = child as? WebView ?: return@setOnChildScrollUpCallback false
                    wv.scrollY > 0
                }
                val createdWebView = WebView(ctx).apply {
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
                        config.embeddedExtensionModules,
                        config.extensionFabIcon, allowGlobalModuleFallback = false)
                    // 然后覆盖本地文件特定的设置（必须在 configureWebView 之后）
                    // 因为 configureWebView 会将 allowFileAccessFromFileURLs 设为 false
                    settings.apply {
                        javaScriptEnabled = enableJavaScript
                        domStorageEnabled = enableLocalStorage
                        allowFileAccess = true
                        allowContentAccess = true
                        // Allow本地文件访问（HTML中的相对路径资源，如 JS/CSS 文件）
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                        // Allow混合内容（HTTPS 页面加载 HTTP 资源，以及 file:// Page访问网络）
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
                        false
                    }
                    setOnLongClickListener {
                        webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                    }

                    onWebViewCreated(this)
                    onWebViewRefUpdated(this)
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
            if (!isRefreshing && swipeLayout.isRefreshing) {
                swipeLayout.isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
