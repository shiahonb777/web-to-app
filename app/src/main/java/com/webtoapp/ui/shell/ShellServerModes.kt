package com.webtoapp.ui.shell

import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.ui.components.EdgeSwipeRefreshLayout
import com.webtoapp.data.model.WebViewConfig
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun WordPressShellMode(
    config: ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current

    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val phpRuntime = remember { com.webtoapp.core.wordpress.WordPressPhpRuntime(context) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {

                if (!com.webtoapp.core.wordpress.WordPressDependencyManager.isPhpReady(context)) {
                    AppLogger.e(
                        "WordPressShell",
                        "PHP runtime missing: nativeLibraryDir=${context.applicationInfo.nativeLibraryDir}"
                    )
                    phase = "error"
                    errorMsg = Strings.phpStartFailed
                    return@withContext
                }

                val wpDir = File(context.filesDir, "wordpress_site")
                val marker = File(wpDir, ".wp_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "wordpress",
                    configVersionCode = config.versionCode,
                    extra = config.wordpressConfig.siteTitle
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("WordPressShell", "Extracting WordPress files to ${wpDir.absolutePath}")
                    wpDir.deleteRecursively()
                    extractAssetsRecursive(context, "wordpress", wpDir)
                    writeExtractionMarker(marker, extractionToken)
                }

                phase = "starting"
                val requestPort = config.wordpressConfig.phpPort
                val port = phpRuntime.startServer(wpDir.absolutePath, requestPort)

                if (port > 0) {
                    serverUrl = "http://127.0.0.1:$port"
                    phase = "installing"
                    if (config.wordpressConfig.autoInstall) {
                        com.webtoapp.core.wordpress.WordPressManager.autoInstallIfNeeded(
                            baseUrl = serverUrl!!,
                            siteTitle = config.wordpressConfig.siteTitle,
                            adminUser = config.wordpressConfig.adminUser,
                            adminPassword = config.wordpressConfig.adminPassword,
                            adminEmail = config.wordpressConfig.adminEmail.ifBlank { "admin@localhost.local" },
                            siteLanguage = config.wordpressConfig.siteLanguage
                        )
                    }
                    phase = "configuring"
                    com.webtoapp.core.wordpress.WordPressManager.applyRuntimeConfig(
                        phpBinary = phpRuntime.getPhpBinaryPath(),
                        projectDir = wpDir,
                        siteTitle = config.wordpressConfig.siteTitle,
                        permalinkStructure = config.wordpressConfig.permalinkStructure,
                        siteLanguage = config.wordpressConfig.siteLanguage,
                        themeName = config.wordpressConfig.themeName,
                        activePlugins = config.wordpressConfig.activePlugins
                    )
                    phase = "ready"
                    AppLogger.i("WordPressShell", "WordPress ready: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = Strings.phpStartFailed
                }
            } catch (e: Exception) {
                AppLogger.e("WordPressShell", "WordPress Shell Launch failed", e)
                phase = "error"
                errorMsg = e.message ?: Strings.unknownError
                errorThrowable = e
            }
        }
    }

    DisposableEffect(phpRuntime) {
        onDispose { phpRuntime.stopServer() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                var swipeChildWebView: WebView? = null
                key(webViewRecreationKey) {
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
                                        extensionEnabled = config.extensionEnabled,
                                        browserDisguiseConfig = config.browserDisguiseConfig,
                                        deviceDisguiseConfig = config.deviceDisguiseConfig)

                                    settings.mixedContentMode =
                                        android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

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
                                    webViewRef = this
                                    if (tag != "state_restored") loadUrl(url)
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

            "extracting", "starting" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (phase == "extracting") Strings.wpCheckingDeps
                                   else Strings.wpStartingServer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = "WordPress",
                    message = errorMsg ?: Strings.wpStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}

@Composable
fun HtmlFrontendShellMode(
    config: ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }
    var targetUrl by remember { mutableStateOf<String?>(null) }
    val stableHttpPort = remember(config.packageName) {
        com.webtoapp.core.webview.LocalHttpServer.stablePortForPackageName(config.packageName)
    }
    val httpServer = remember(stableHttpPort) {
        com.webtoapp.core.webview.LocalHttpServer(context, stableHttpPort)
    }

    DisposableEffect(httpServer) {
        onDispose { httpServer.stop() }
    }

    val effectiveEntryFile = config.htmlConfig.getValidEntryFile()

    LaunchedEffect(config.versionCode, effectiveEntryFile, config.webViewConfig.enableCrossOriginIsolation) {
        withContext(Dispatchers.IO) {
            try {
                val siteDir = File(context.filesDir, "html_shell_site")
                val marker = File(siteDir, ".html_extracted")
                val configuredEntryFile = config.htmlConfig.getValidEntryFile()
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "html",
                    configVersionCode = config.versionCode,
                    extra = "${config.appType}|$configuredEntryFile|coi=${config.webViewConfig.enableCrossOriginIsolation}"
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("HtmlShell", "Extracting HTML assets to ${siteDir.absolutePath}")
                    siteDir.deleteRecursively()
                    extractAssetsRecursive(context, "html", siteDir)
                    writeExtractionMarker(marker, extractionToken)
                }

                phase = "starting"
                val resolvedEntry = resolveExtractedHtmlEntry(siteDir, effectiveEntryFile)

                val requiresHttpServer = !config.htmlUsesFileScheme

                if (requiresHttpServer) {

                    val shouldEnableIsolation = config.webViewConfig.enableCrossOriginIsolation ||
                        com.webtoapp.core.webview.LocalHttpServer.shouldEnableCrossOriginIsolation(siteDir)
                    val baseUrl = httpServer.start(siteDir, enableCrossOriginIsolation = shouldEnableIsolation)
                    targetUrl = buildLocalHttpTargetUrl(baseUrl, resolvedEntry)
                    AppLogger.i(
                        "HtmlShell",
                        "HTML Shell ready (HTTP server): url=$targetUrl, entry=$resolvedEntry, port=$stableHttpPort, crossOriginIsolation=$shouldEnableIsolation"
                    )
                } else {

                    httpServer.stop()
                    val normalizedEntry = resolvedEntry.removePrefix("/").ifBlank { "index.html" }
                    val entryFileObj = File(siteDir, normalizedEntry)
                    targetUrl = android.net.Uri.fromFile(entryFileObj).toString()
                    AppLogger.i(
                        "HtmlShell",
                        "HTML Shell ready (file://): url=$targetUrl, entry=$resolvedEntry (packaged as pure-static, running offline without local server)"
                    )
                }
                phase = "ready"
            } catch (e: Exception) {
                AppLogger.e("HtmlShell", "HTML Shell Launch failed", e)
                phase = "error"
                errorMsg = e.message ?: Strings.serverStartFailed
                errorThrowable = e
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = targetUrl ?: return@Box
                ShellLocalFileWebView(
                    config = config,
                    webViewRecreationKey = webViewRecreationKey,
                    webViewConfig = webViewConfig,
                    webViewCallbacks = webViewCallbacks,
                    webViewManager = webViewManager,
                    targetUrl = url,
                    enableJavaScript = config.htmlConfig.enableJavaScript,
                    enableLocalStorage = config.htmlConfig.enableLocalStorage,
                    swipeRefreshEnabled = swipeRefreshEnabled,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onWebViewCreated = onWebViewCreated,
                    onWebViewRefUpdated = onWebViewRefUpdated
                )
            }

            "extracting", "starting" -> {

                var showLoadingUi by remember { mutableStateOf(false) }
                LaunchedEffect(phase) {
                    delay(600)
                    showLoadingUi = true
                }
                if (showLoadingUi) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

            }

            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = "HTML",
                    message = errorMsg ?: Strings.serverStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}

private fun resolveExtractedHtmlEntry(siteDir: File, configuredEntry: String): String {
    val normalizedConfiguredEntry = configuredEntry.removePrefix("/")
    if (normalizedConfiguredEntry.isNotBlank() && File(siteDir, normalizedConfiguredEntry).exists()) {
        return normalizedConfiguredEntry
    }

    val preferredFallbacks = listOf("index.html", "index.htm", "main.html")
    preferredFallbacks.firstOrNull { File(siteDir, it).exists() }?.let { return it }

    return siteDir.walkTopDown()
        .filter { it.isFile }
        .firstOrNull {
            it.extension.equals("html", ignoreCase = true) ||
                it.extension.equals("htm", ignoreCase = true)
        }
        ?.relativeTo(siteDir)
        ?.invariantSeparatorsPath
        ?: normalizedConfiguredEntry.ifBlank { "index.html" }
}

private fun resolveStaticDocRoot(siteDir: File): File {
    val candidates = listOf("dist", "build", "public", "static", "www", "")
    for (dir in candidates) {
        val candidate = if (dir.isEmpty()) siteDir else File(siteDir, dir)
        if (candidate.isDirectory && File(candidate, "index.html").exists()) {
            return candidate
        }
    }
    return siteDir
}

@Composable
fun NodeJsStaticShellMode(
    config: ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }
    var targetUrl by remember { mutableStateOf<String?>(null) }
    val stableHttpPort = remember(config.packageName) {
        com.webtoapp.core.webview.LocalHttpServer.stablePortForPackageName(config.packageName)
    }
    val httpServer = remember(stableHttpPort) {
        com.webtoapp.core.webview.LocalHttpServer(context, stableHttpPort)
    }

    DisposableEffect(httpServer) {
        onDispose { httpServer.stop() }
    }

    LaunchedEffect(config.versionCode) {
        withContext(Dispatchers.IO) {
            try {
                val siteDir = File(context.filesDir, "nodejs_static_site")
                val marker = File(siteDir, ".nodejs_static_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "nodejs_static",
                    configVersionCode = config.versionCode,
                    extra = config.nodejsConfig.entryFile
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("NodeJsStaticShell", "Extracting nodejs static assets to ${siteDir.absolutePath}")
                    siteDir.deleteRecursively()
                    extractAssetsRecursive(context, "nodejs_app", siteDir)
                    writeExtractionMarker(marker, extractionToken)
                }

                phase = "starting"
                val docRoot = resolveStaticDocRoot(siteDir)
                val resolvedEntry = resolveExtractedHtmlEntry(docRoot, config.nodejsConfig.entryFile)
                val baseUrl = httpServer.start(docRoot)
                targetUrl = buildLocalHttpTargetUrl(baseUrl, resolvedEntry)
                AppLogger.i(
                    "NodeJsStaticShell",
                    "Node static ready (HTTP server): url=$targetUrl, docRoot=${docRoot.absolutePath}, port=$stableHttpPort"
                )
                phase = "ready"
            } catch (e: Exception) {
                AppLogger.e("NodeJsStaticShell", "Node static shell launch failed", e)
                phase = "error"
                errorMsg = e.message ?: Strings.serverStartFailed
                errorThrowable = e
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = targetUrl ?: return@Box
                ShellLocalFileWebView(
                    config = config,
                    webViewRecreationKey = webViewRecreationKey,
                    webViewConfig = webViewConfig,
                    webViewCallbacks = webViewCallbacks,
                    webViewManager = webViewManager,
                    targetUrl = url,
                    enableJavaScript = true,
                    enableLocalStorage = true,
                    swipeRefreshEnabled = swipeRefreshEnabled,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onWebViewCreated = onWebViewCreated,
                    onWebViewRefUpdated = onWebViewRefUpdated
                )
            }

            "extracting", "starting" -> {
                var showLoadingUi by remember { mutableStateOf(false) }
                LaunchedEffect(phase) {
                    delay(600)
                    showLoadingUi = true
                }
                if (showLoadingUi) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = "Node.js (static)",
                    message = errorMsg ?: Strings.serverStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}

private fun buildLocalHttpTargetUrl(baseUrl: String, relativePath: String): String {
    val normalizedPath = relativePath.removePrefix("/").ifBlank { "index.html" }
    return "$baseUrl/${Uri.encode(normalizedPath, "/")}"
}

@Composable
fun NodeJsShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current

    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val nodeRuntime = remember { com.webtoapp.core.nodejs.NodeRuntime(context) }
    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {

                val nodePath = com.webtoapp.core.nodejs.NodeDependencyManager.getNodeLibraryPath(context)
                AppLogger.i("NodeJsShell", "nativeLibraryDir: ${context.applicationInfo.nativeLibraryDir}")
                com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "nativeLibraryDir: ${context.applicationInfo.nativeLibraryDir}")

                if (nodePath == null) {
                    AppLogger.e("NodeJsShell", "libnode.so not found")
                    com.webtoapp.core.shell.ShellLogger.e("NodeJsShell", "libnode.so 未找到")
                    phase = "error"
                    errorMsg = Strings.nodeRuntimeNotFound
                    return@withContext
                }

                AppLogger.i("NodeJsShell", "libnode.so path: $nodePath (size=${java.io.File(nodePath).length()})")
                com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "libnode.so 路径: $nodePath (size=${java.io.File(nodePath).length()})")

                val projectDir = File(context.filesDir, "nodejs_site")
                val marker = File(projectDir, ".nodejs_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "nodejs",
                    configVersionCode = config.versionCode,
                    extra = "${config.nodejsConfig.mode}|${config.nodejsConfig.entryFile}"
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("NodeJsShell", "Extracting Node.js project files to ${projectDir.absolutePath}")
                    com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "提取 Node.js 项目文件到 ${projectDir.absolutePath}")
                    projectDir.deleteRecursively()
                    extractAssetsRecursive(context, "nodejs_app", projectDir)
                    writeExtractionMarker(marker, extractionToken)
                }

                val envVars = config.nodejsConfig.envVars.toMutableMap()
                val requestPort = config.nodejsConfig.port.takeIf { it > 0 }
                if (requestPort != null && !envVars.containsKey("PORT")) {
                    envVars["PORT"] = requestPort.toString()
                }

                phase = "starting"
                val entryFile = config.nodejsConfig.entryFile.ifEmpty { "index.js" }
                val port = nodeRuntime.startServer(
                    projectDir = projectDir.absolutePath,
                    entryFile = entryFile,
                    port = requestPort ?: 0,
                    envVars = envVars
                )

                if (port > 0) {
                    serverUrl = "http://127.0.0.1:$port"
                    phase = "ready"
                    AppLogger.i("NodeJsShell", "Node.js ready: $serverUrl")
                    com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "Node.js 就绪: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = (nodeRuntime.serverState.value as? com.webtoapp.core.nodejs.NodeRuntime.ServerState.Error)
                        ?.message
                        ?: Strings.nodeServerStartFailed
                    com.webtoapp.core.shell.ShellLogger.e("NodeJsShell", "Node.js 服务器启动失败")
                }
            } catch (e: Exception) {
                AppLogger.e("NodeJsShell", "Node.js Shell Launch failed", e)
                com.webtoapp.core.shell.ShellLogger.e("NodeJsShell", "Node.js Shell 启动失败", e)
                phase = "error"
                errorMsg = e.message ?: Strings.unknownError
                errorThrowable = e
            }
        }
    }

    DisposableEffect(nodeRuntime) {
        onDispose { nodeRuntime.stopServer() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                var swipeChildWebView: WebView? = null
                key(webViewRecreationKey) {
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
                                        this, webViewConfig, webViewCallbacks,
                                        config.extensionModuleIds, config.embeddedExtensionModules,
                                        config.extensionFabIcon, allowGlobalModuleFallback = false,
                                        extensionEnabled = config.extensionEnabled,
                                        browserDisguiseConfig = config.browserDisguiseConfig,
                                        deviceDisguiseConfig = config.deviceDisguiseConfig)
                                    settings.mixedContentMode =
                                        android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                    var lastTouchX = 0f
                                    var lastTouchY = 0f
                                    setOnTouchListener { view, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                lastTouchX = event.x; lastTouchY = event.y
                                            }
                                            MotionEvent.ACTION_UP -> view.performClick()
                                        }
                                        false
                                    }
                                    setOnLongClickListener {
                                        webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                                    }

                                    onWebViewCreated(this)
                                    webViewRef = this
                                    if (tag != "state_restored") loadUrl(url)
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

            "extracting", "starting" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (phase == "extracting") Strings.preparingNodeEnv
                                   else Strings.startingNodeServer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = "Node.js",
                    message = errorMsg ?: Strings.nodeStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}

@Composable
fun PhpAppShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val phpRuntime = remember { com.webtoapp.core.php.PhpAppRuntime(context) }
    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {

                if (!com.webtoapp.core.wordpress.WordPressDependencyManager.isPhpReady(context)) {
                    AppLogger.e(
                        "PhpAppShell",
                        "PHP runtime missing: nativeLibraryDir=${context.applicationInfo.nativeLibraryDir}"
                    )
                    phase = "error"
                    errorMsg = Strings.phpStartFailed
                    return@withContext
                }

                val phpProjectDir = File(context.filesDir, "php_app_site")
                val marker = File(phpProjectDir, ".php_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "php_app",
                    configVersionCode = config.versionCode,
                    extra = "${config.phpAppConfig.documentRoot}|${config.phpAppConfig.entryFile}"
                )
                if (shouldReextractAssets(marker, extractionToken)) {
                    phpProjectDir.deleteRecursively()
                    extractAssetsRecursive(context, "php_app", phpProjectDir)
                    writeExtractionMarker(marker, extractionToken)
                }

                phase = "starting"
                val docRoot = config.phpAppConfig.documentRoot
                val entryFile = config.phpAppConfig.entryFile.ifBlank { "index.php" }
                val port = phpRuntime.startServer(
                    projectDir = phpProjectDir.absolutePath,
                    documentRoot = docRoot,
                    entryFile = entryFile,
                    port = config.phpAppConfig.port,
                    envVars = config.phpAppConfig.envVars
                )

                if (port > 0) {
                    serverUrl = "http://127.0.0.1:$port"
                    phase = "ready"
                } else {
                    phase = "error"
                    errorMsg = Strings.phpStartFailed
                }
            } catch (e: Exception) {
                phase = "error"
                errorMsg = e.message ?: Strings.unknownError
                errorThrowable = e
            }
        }
    }

    DisposableEffect(phpRuntime) { onDispose { phpRuntime.stopServer() } }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                var swipeChildWebView: WebView? = null
                key(webViewRecreationKey) {
                    AndroidView(
                        factory = { ctx ->
                            EdgeSwipeRefreshLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                    webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false, extensionEnabled = config.extensionEnabled, browserDisguiseConfig = config.browserDisguiseConfig, deviceDisguiseConfig = config.deviceDisguiseConfig)
                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    var lastTouchX = 0f; var lastTouchY = 0f
                                    setOnTouchListener { view, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                lastTouchX = event.x
                                                lastTouchY = event.y
                                            }
                                            MotionEvent.ACTION_UP -> view.performClick()
                                        }
                                        false
                                    }
                                    setOnLongClickListener { webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY) }
                                    onWebViewCreated(this); webViewRef = this; if (tag != "state_restored") loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (phase == "extracting") Strings.wpCheckingDeps else Strings.wpStartingServer)
                    }
                }
            }
            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = "PHP",
                    message = errorMsg ?: Strings.phpStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}

@Composable
fun PythonAppShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val pythonRuntime = remember { com.webtoapp.core.python.PythonRuntime(context) }
    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val pyConfig = config.pythonAppConfig

                val pythonHome = com.webtoapp.core.python.PythonDependencyManager.getPythonDir(context)
                val runtimeMarker = File(pythonHome, ".runtime_extracted")
                if (!runtimeMarker.exists()) {
                    AppLogger.i("PythonShell", "First run, extracting Python stdlib to ${pythonHome.absolutePath}")
                    try {
                        val assetChildren = context.assets.list("python_runtime")
                        if (assetChildren != null && assetChildren.isNotEmpty()) {
                            extractAssetsRecursive(context, "python_runtime", pythonHome)
                            runtimeMarker.writeText("extracted")
                            AppLogger.i("PythonShell", "Python stdlib extraction complete")
                        } else {
                            AppLogger.w("PythonShell", "assets/python_runtime missing or empty, trying legacy assets/python/ path")

                            val abi = com.webtoapp.core.python.PythonDependencyManager.getDeviceAbi()
                            try {
                                val binDir = File(pythonHome, "bin")
                                binDir.mkdirs()
                                val pythonBin = File(binDir, "python3")
                                if (!pythonBin.exists()) {
                                    context.assets.open("python/$abi/python3").use { input ->
                                        pythonBin.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    pythonBin.setExecutable(true)
                                    AppLogger.i("PythonShell", "Legacy: Python binary extracted to ${pythonBin.absolutePath}")
                                }
                            } catch (e: Exception) {
                                AppLogger.w("PythonShell", "Legacy Python binary extraction failed: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("PythonShell", "Failed to extract Python stdlib", e)
                    }
                } else {
                    AppLogger.i("PythonShell", "Python stdlib already extracted, skipping")
                }

                if (!pythonRuntime.isPythonAvailable()) {
                    AppLogger.w("PythonShell", "Python runtime unavailable, falling back to preview mode")

                    val projectDir = File(context.filesDir, "python_app_site")
                    projectDir.mkdirs()
                    extractAssetsRecursive(context, "python_app", projectDir)
                    val previewHtml = File(projectDir, "_preview_.html")
                    if (previewHtml.exists()) {
                        serverUrl = "file://${previewHtml.absolutePath}"
                        phase = "ready"
                        AppLogger.i("PythonShell", "Falling back to preview mode: $serverUrl")
                    } else {
                        phase = "error"
                        errorMsg = Strings.pythonRuntimeNotFound
                    }
                    return@withContext
                }
                AppLogger.i("PythonShell", "Python runtime ready")

                val projectDir = File(context.filesDir, "python_app_site")
                val marker = File(projectDir, ".python_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "python_app",
                    configVersionCode = config.versionCode,
                    extra = "${pyConfig.framework}|${pyConfig.entryFile}|${pyConfig.entryModule}"
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("PythonShell", "Extracting Python project files to ${projectDir.absolutePath}")
                    projectDir.deleteRecursively()
                    extractAssetsRecursive(context, "python_app", projectDir)
                    writeExtractionMarker(marker, extractionToken)
                } else {
                    AppLogger.i("PythonShell", "Extraction marker exists, skipping extraction")
                }

                phase = "starting"
                val entryFile = pyConfig.entryFile.ifEmpty { "app.py" }
                val framework = pyConfig.framework.ifEmpty { "flask" }
                val port = pythonRuntime.startServer(
                    projectDir = projectDir.absolutePath,
                    entryFile = entryFile,
                    framework = framework,
                    port = pyConfig.port,
                    envVars = pyConfig.envVars,
                    installDeps = true
                )

                if (port > 0) {
                    delay(200)
                    serverUrl = "http://127.0.0.1:$port"
                    phase = "ready"
                    AppLogger.i("PythonShell", "Python server ready: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = (pythonRuntime.serverState.value as? com.webtoapp.core.python.PythonRuntime.ServerState.Error)
                        ?.message
                        ?: Strings.pythonServerStartFailed
                    AppLogger.e("PythonShell", "Python server failed to start: $errorMsg")
                }
            } catch (e: Exception) {
                AppLogger.e("PythonShell", "Python Shell Launch failed", e)
                phase = "error"
                errorMsg = e.message ?: Strings.unknownError
                errorThrowable = e
            }
        }
    }

    DisposableEffect(pythonRuntime) { onDispose { pythonRuntime.stopServer() } }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                var swipeChildWebView: WebView? = null
                key(webViewRecreationKey) {
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
                                    webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false, extensionEnabled = config.extensionEnabled, browserDisguiseConfig = config.browserDisguiseConfig, deviceDisguiseConfig = config.deviceDisguiseConfig)
                                    settings.apply {
                                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        @Suppress("DEPRECATION")
                                        allowFileAccessFromFileURLs = true
                                        @Suppress("DEPRECATION")
                                        allowUniversalAccessFromFileURLs = true
                                    }
                                    var lastTouchX = 0f; var lastTouchY = 0f
                                    setOnTouchListener { view, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                lastTouchX = event.x; lastTouchY = event.y
                                            }
                                            MotionEvent.ACTION_UP -> view.performClick()
                                        }
                                        false
                                    }
                                    setOnLongClickListener { webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY) }
                                    onWebViewCreated(this); webViewRef = this; if (tag != "state_restored") loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (phase) {
                                "extracting" -> Strings.preparingPythonEnv
                                else -> Strings.startingPythonServer
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = "Python",
                    message = errorMsg ?: Strings.pythonStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}

@Composable
fun GoAppShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val goRuntime = remember { com.webtoapp.core.golang.GoRuntime(context) }
    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val goConfig = config.goAppConfig

                val projectDir = File(context.filesDir, "go_app_site")
                val marker = File(projectDir, ".go_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "go_app",
                    configVersionCode = config.versionCode,
                    extra = "${goConfig.framework}|${goConfig.binaryName}|${goConfig.staticDir}"
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("GoShell", "Extracting Go project files to ${projectDir.absolutePath}")
                    projectDir.deleteRecursively()
                    extractAssetsRecursive(context, "go_app", projectDir)
                    writeExtractionMarker(marker, extractionToken)
                } else {
                    AppLogger.i("GoShell", "Extraction marker exists, skipping extraction")
                }

                projectDir.walkTopDown()
                    .filter { it.isFile && it.length() > 1000 }
                    .forEach { file ->
                        val elfInfo = com.webtoapp.core.golang.GoDependencyManager.parseElf(file)
                        if (elfInfo.isValid) {
                            file.setExecutable(true, false)
                        }
                    }

                val binaryName = goConfig.binaryName.ifEmpty {
                    goRuntime.detectBinary(projectDir) ?: ""
                }
                if (binaryName.isEmpty()) {
                    AppLogger.w("GoShell", "Go binary not detected, falling back to preview mode")
                    val previewHtml = File(projectDir, "_preview_.html")
                    if (previewHtml.exists()) {
                        serverUrl = "file://${previewHtml.absolutePath}"
                        phase = "ready"
                        AppLogger.i("GoShell", "Falling back to preview mode: $serverUrl")
                    } else {
                        phase = "error"
                        errorMsg = Strings.goBinaryNotFound
                    }
                    return@withContext
                }
                AppLogger.i("GoShell", "Go binary detected: $binaryName")

                phase = "starting"
                val port = goRuntime.startServer(
                    projectDir = projectDir.absolutePath,
                    binaryName = binaryName,
                    port = goConfig.port,
                    envVars = goConfig.envVars
                )

                if (port > 0) {
                    serverUrl = "http://127.0.0.1:$port"
                    phase = "ready"
                    AppLogger.i("GoShell", "Go server ready: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = (goRuntime.serverState.value as? com.webtoapp.core.golang.GoRuntime.ServerState.Error)
                        ?.message
                        ?: Strings.goServerStartFailed
                    AppLogger.e("GoShell", "Go server failed to start: $errorMsg")
                }
            } catch (e: Exception) {
                AppLogger.e("GoShell", "Go Shell Launch failed", e)
                phase = "error"
                errorMsg = e.message ?: Strings.unknownError
                errorThrowable = e
            }
        }
    }

    DisposableEffect(goRuntime) { onDispose { goRuntime.stopServer() } }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                var swipeChildWebView: WebView? = null
                key(webViewRecreationKey) {
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
                                    webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false, extensionEnabled = config.extensionEnabled, browserDisguiseConfig = config.browserDisguiseConfig, deviceDisguiseConfig = config.deviceDisguiseConfig)
                                    settings.apply {
                                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        @Suppress("DEPRECATION")
                                        allowFileAccessFromFileURLs = true
                                        @Suppress("DEPRECATION")
                                        allowUniversalAccessFromFileURLs = true
                                    }
                                    var lastTouchX = 0f; var lastTouchY = 0f
                                    setOnTouchListener { view, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                lastTouchX = event.x; lastTouchY = event.y
                                            }
                                            MotionEvent.ACTION_UP -> view.performClick()
                                        }
                                        false
                                    }
                                    setOnLongClickListener { webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY) }
                                    onWebViewCreated(this); webViewRef = this; if (tag != "state_restored") loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (phase) {
                                "extracting" -> Strings.goBinaryDetection
                                else -> Strings.startingGoServer
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = "Go",
                    message = errorMsg ?: Strings.goStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}

@Composable
fun ServerAppShellMode(
    appType: String,
    config: com.webtoapp.core.shell.ShellConfig,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf("extracting") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    val httpServer = remember { com.webtoapp.core.webview.LocalHttpServer(context) }

    DisposableEffect(httpServer) { onDispose { httpServer.stop() } }

    var errorThrowable by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val assetDir = if (appType == "PYTHON_APP") "python_app" else "go_app"
                val siteDir = File(context.filesDir, "${assetDir}_site")
                val marker = File(siteDir, ".extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = assetDir,
                    configVersionCode = config.versionCode,
                    extra = if (appType == "PYTHON_APP") {
                        "${config.pythonAppConfig.framework}|${config.pythonAppConfig.entryFile}|${config.pythonAppConfig.entryModule}"
                    } else {
                        "${config.goAppConfig.framework}|${config.goAppConfig.binaryName}|${config.goAppConfig.staticDir}"
                    }
                )

                AppLogger.i("ServerAppShellMode", "========== $appType Shell start ==========")
                AppLogger.i("ServerAppShellMode", "assetDir=$assetDir, siteDir=${siteDir.absolutePath}")
                AppLogger.i("ServerAppShellMode", "marker exists=${marker.exists()}")

                if (appType == "PYTHON_APP") {
                    val pyConfig = config.pythonAppConfig
                    AppLogger.i("ServerAppShellMode", "Python config: framework=${pyConfig.framework}, entryFile=${pyConfig.entryFile}, entryModule=${pyConfig.entryModule}, serverType=${pyConfig.serverType}, port=${pyConfig.port}")
                    AppLogger.i("ServerAppShellMode", "Python env vars: ${pyConfig.envVars}")
                } else {
                    val goConfig = config.goAppConfig
                    AppLogger.i("ServerAppShellMode", "Go config: framework=${goConfig.framework}, binaryName=${goConfig.binaryName}, port=${goConfig.port}, staticDir=${goConfig.staticDir}")
                }

                try {
                    val assetChildren = context.assets.list(assetDir)
                    AppLogger.i("ServerAppShellMode", "assets/$assetDir contents (${assetChildren?.size ?: 0} items): ${assetChildren?.joinToString()}")
                    if (assetChildren.isNullOrEmpty()) {
                        AppLogger.e("ServerAppShellMode", "assets/$assetDir is empty or missing!")
                    }
                } catch (e: Exception) {
                    AppLogger.e("ServerAppShellMode", "Listing assets/$assetDir failed", e)
                }

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("ServerAppShellMode", "First extract: removing old directory and extracting from assets...")
                    siteDir.deleteRecursively()
                    extractAssetsRecursive(context, assetDir, siteDir)
                    writeExtractionMarker(marker, extractionToken)
                    AppLogger.i("ServerAppShellMode", "Extraction complete, marker created")
                } else {
                    AppLogger.i("ServerAppShellMode", "Extraction marker exists, skipping extraction")
                }

                val fileList = StringBuilder()
                siteDir.walkTopDown().take(30).forEach { f ->
                    val rel = f.relativeTo(siteDir).path
                    val info = if (f.isDirectory) "[DIR]" else "(${f.length()} bytes)"
                    fileList.appendLine("  $rel $info")
                }
                val totalFiles = siteDir.walkTopDown().filter { it.isFile }.count()
                AppLogger.i("ServerAppShellMode", "Post-extraction directory structure ($totalFiles files):\n$fileList")

                phase = "starting"

                val candidates = listOf("dist", "build", "public", "static", "www", "")
                var docRoot: File? = null
                for (dir in candidates) {
                    val candidate = if (dir.isEmpty()) siteDir else File(siteDir, dir)
                    val indexExists = File(candidate, "index.html").exists()
                    val isDir = candidate.isDirectory
                    AppLogger.d("ServerAppShellMode", "Checking candidate directory: '$dir' -> ${candidate.absolutePath}, isDir=$isDir, hasIndex=$indexExists")
                    if (isDir && indexExists) {
                        docRoot = candidate
                        AppLogger.i("ServerAppShellMode", "Found docRoot: ${candidate.absolutePath}")
                        break
                    }
                }

                if (docRoot != null) {
                    serverUrl = httpServer.start(docRoot)
                    phase = "ready"
                    AppLogger.i("ServerAppShellMode", "LocalHttpServer started, URL=$serverUrl")
                } else {
                    AppLogger.w("ServerAppShellMode", "No candidate directory contains index.html")

                    serverUrl = "file://${siteDir.absolutePath}/index.html"
                    if (File(siteDir, "index.html").exists()) {
                        phase = "ready"
                        AppLogger.i("ServerAppShellMode", "Falling back to file:// mode: $serverUrl")
                    } else {
                        phase = "error"
                        errorMsg = Strings.serverStartFailed
                        AppLogger.e("ServerAppShellMode", "Final failure: siteDir also lacks index.html")
                        AppLogger.e("ServerAppShellMode", "Python/Go backend apps need their runtime to serve pages; this implementation only supports static frontend artefacts")
                    }
                }

                AppLogger.i("ServerAppShellMode", "========== Final status: phase=$phase, serverUrl=$serverUrl, error=$errorMsg ==========")
            } catch (e: Exception) {
                phase = "error"
                errorMsg = e.message ?: Strings.serverStartFailed
                errorThrowable = e
                AppLogger.e("ServerAppShellMode", "Start exception", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                AppLogger.i("ServerAppShellMode", "WebView ready, about to load URL: $url")
                var swipeChildWebView: WebView? = null
                key(webViewRecreationKey) {
                    AndroidView(
                        factory = { ctx ->
                            EdgeSwipeRefreshLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                    webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false, extensionEnabled = config.extensionEnabled, browserDisguiseConfig = config.browserDisguiseConfig, deviceDisguiseConfig = config.deviceDisguiseConfig)
                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.apply {
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        @Suppress("DEPRECATION")
                                        allowFileAccessFromFileURLs = true
                                        @Suppress("DEPRECATION")
                                        allowUniversalAccessFromFileURLs = true
                                    }
                                    webChromeClient = object : android.webkit.WebChromeClient() {
                                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                            AppLogger.d("ServerAppShellMode", "Console [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                                            return super.onConsoleMessage(consoleMessage)
                                        }
                                    }
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                            AppLogger.i("ServerAppShellMode", "onPageStarted: $pageUrl")
                                            super.onPageStarted(view, pageUrl, favicon)
                                        }
                                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                            AppLogger.i("ServerAppShellMode", "onPageFinished: $pageUrl, title='${view?.title}'")
                                            super.onPageFinished(view, pageUrl)
                                        }
                                        override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                            AppLogger.e("ServerAppShellMode", "onReceivedError: url=${request?.url}, code=${error?.errorCode}, desc=${error?.description}")
                                            super.onReceivedError(view, request, error)
                                        }
                                        override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                            AppLogger.e("ServerAppShellMode", "onReceivedHttpError: url=${request?.url}, status=${errorResponse?.statusCode}, reason=${errorResponse?.reasonPhrase}")
                                            super.onReceivedHttpError(view, request, errorResponse)
                                        }
                                    }
                                    onWebViewCreated(this)
                                    AppLogger.i("ServerAppShellMode", "WebView created, tag=$tag")
                                    if (tag != "state_restored") loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (phase == "extracting") Strings.preparingEnv else Strings.startingServer)
                    }
                }
            }
            "error" -> {
                ShellErrorScreen(
                    config = config,
                    mode = appType,
                    message = errorMsg ?: Strings.serverStartFailed,
                    throwable = errorThrowable
                )
            }
        }
    }
}
