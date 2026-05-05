package com.webtoapp.ui.shell

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.components.EdgeSwipeRefreshLayout







@Composable
fun ShellContentRouter(
    appType: String,
    config: ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    deepLinkUrl: String?,

    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onActivityFinish: () -> Unit
) {
    when {
        appType == "IMAGE" || appType == "VIDEO" -> {

            MediaContentDisplay(
                isVideo = appType == "VIDEO",
                mediaConfig = config.mediaConfig
            )
        }
        appType == "GALLERY" -> {

            AppLogger.d("ShellScreen", "进入 GALLERY 分支，显示 ShellGalleryPlayer")
            ShellGalleryPlayer(
                galleryConfig = config.galleryConfig,
                onBack = onActivityFinish
            )
        }
        appType == "WORDPRESS" -> {

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
            HtmlFrontendShellMode(
                config = config,
                webViewConfig = webViewConfig,
                webViewCallbacks = webViewCallbacks,
                webViewManager = webViewManager,
                onWebViewCreated = onWebViewCreated,
                onWebViewRefUpdated = onWebViewRefUpdated,
                swipeRefreshEnabled = swipeRefreshEnabled,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        else -> {

            val resolvedUrl = normalizeShellTargetUrlForSecurity(deepLinkUrl ?: config.targetUrl)
            AppLogger.d("ShellScreen", "进入 WebView 分支 (else)，加载 URL: $resolvedUrl")

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
                                config.extensionFabIcon, allowGlobalModuleFallback = false,
                                browserDisguiseConfig = config.browserDisguiseConfig,
                                deviceDisguiseConfig = config.deviceDisguiseConfig)



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

                            if (tag == "state_restored") {
                                reload()
                            } else {
                                loadUrl(resolvedUrl)
                            }
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





@Composable
fun ShellLocalFileWebView(
    config: ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    targetUrl: String,
    enableJavaScript: Boolean,
    enableLocalStorage: Boolean,

    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit
) {

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
                        config.extensionFabIcon, allowGlobalModuleFallback = false,
                        browserDisguiseConfig = config.browserDisguiseConfig,
                        deviceDisguiseConfig = config.deviceDisguiseConfig)


                    settings.apply {
                        javaScriptEnabled = enableJavaScript
                        domStorageEnabled = enableLocalStorage
                        allowFileAccess = true
                        allowContentAccess = true

                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true

                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }



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

                    if (tag == "state_restored") {
                        reload()
                    } else {
                        loadUrl(targetUrl)
                    }
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

