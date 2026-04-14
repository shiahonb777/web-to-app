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
 * appType contentdisplay
 *
 * support type: IMAGE, VIDEO, GALLERY, WORDPRESS, NODEJS_APP, PHP_APP,
 * PYTHON_APP, GO_APP, HTML, FRONTEND, anddefault WEB mode
 */
@Composable
fun ShellContentRouter(
    appType: String,
    config: ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    deepLinkUrl: String?,
    // pull- to- refresh
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onActivityFinish: () -> Unit
) {
    when {
        appType == "IMAGE" || appType == "VIDEO" -> {
            // appmode
            MediaContentDisplay(
                isVideo = appType == "VIDEO",
                mediaConfig = config.mediaConfig
            )
        }
        appType == "GALLERY" -> {
            // Gallery appmode
            AppLogger.d("ShellScreen", "进入 GALLERY 分支，显示 ShellGalleryPlayer")
            ShellGalleryPlayer(
                galleryConfig = config.galleryConfig,
                onBack = onActivityFinish
            )
        }
        appType == "WORDPRESS" -> {
            // WordPress appmode- from assets directory, PHP
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
                // mode: load assets in file
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
                // / mode: and Node. js
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
            // HTML/ appmode- load APK assets in HTML file
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
            // WebView( app)
            val resolvedUrl = normalizeShellTargetUrlForSecurity(deepLinkUrl ?: config.targetUrl)
            AppLogger.d("ShellScreen", "进入 WebView 分支 (else)，加载 URL: $resolvedUrl")
            // for setOnChildScrollUpCallback WebView
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

                            // long- press
                            // , ensurelong- press
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
                                false // , WebView handle
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
 * localfile WebView- for HTML/ app Node. js mode
 * file config long- press
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
    // pull- to- refresh
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit
) {
    // for setOnChildScrollUpCallback WebView
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
                    // call configureWebView config
                    webViewManager.configureWebView(
                        this,
                        webViewConfig,
                        webViewCallbacks,
                        config.extensionModuleIds,
                        config.embeddedExtensionModules,
                        config.extensionFabIcon, allowGlobalModuleFallback = false)
                    // localfile settings( configureWebView)
                    // configureWebView map allowFileAccessFromFileURLs false
                    settings.apply {
                        javaScriptEnabled = enableJavaScript
                        domStorageEnabled = enableLocalStorage
                        allowFileAccess = true
                        allowContentAccess = true
                        // Allowlocalfile( HTMLin path, JS/CSS file)
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                        // Allow content( HTTPS load HTTP, and file: // Page network)
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    // long- press
                    // , ensurelong- press
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
