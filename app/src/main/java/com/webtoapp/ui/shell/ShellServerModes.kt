package com.webtoapp.ui.shell

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.ui.components.EdgeSwipeRefreshLayout
import com.webtoapp.data.model.WebViewConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * WordPress Shell mode- from APK assets PHP + WordPress fileand local
 */
@Composable
fun WordPressShellMode(
    config: ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current

    // state
    var phase by remember { mutableStateOf("extracting") } // extracting | starting | ready | error
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val phpRuntime = remember { com.webtoapp.core.wordpress.WordPressPhpRuntime(context) }

    // + PHP
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // 1. PHP path
                // prefer nativeLibraryDir/libphp. so( Android 15+ executepath)
                // fallback from assets( APK)
                val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
                if (!nativePhp.exists()) {
                    val abi = com.webtoapp.core.wordpress.WordPressDependencyManager.getDeviceAbi()
                    val phpDir = com.webtoapp.core.wordpress.WordPressDependencyManager.getPhpDir(context)
                    val phpBinary = File(phpDir, "php")
                    if (!phpBinary.exists() || !phpBinary.canExecute()) {
                        AppLogger.i("WordPressShell", "提取 PHP 二进制: assets/php/$abi/php")
                        context.assets.open("php/$abi/php").use { input ->
                            phpBinary.outputStream().use { output -> input.copyTo(output) }
                        }
                        phpBinary.setExecutable(true)
                    }
                }

                // 2. WordPress file directory( only)
                val wpDir = File(context.filesDir, "wordpress_site")
                val marker = File(wpDir, ".wp_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "wordpress",
                    configVersionCode = config.versionCode,
                    extra = config.wordpressConfig.siteTitle
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("WordPressShell", "提取 WordPress 文件到 ${wpDir.absolutePath}")
                    wpDir.deleteRecursively()
                    extractAssetsRecursive(context, "wordpress", wpDir)
                    writeExtractionMarker(marker, extractionToken)
                }

                // 3. PHP
                phase = "starting"
                val requestPort = config.wordpressConfig.phpPort
                val port = phpRuntime.startServer(wpDir.absolutePath, requestPort)

                if (port > 0) {
                    serverUrl = "http://127.0.0.1:$port"
                    phase = "ready"
                    AppLogger.i("WordPressShell", "WordPress 就绪: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = AppStringsProvider.current().phpStartFailed
                }
            } catch (e: Exception) {
                AppLogger.e("WordPressShell", "WordPress Shell 启动失败", e)
                phase = "error"
                errorMsg = e.message ?: AppStringsProvider.current().unknownError
            }
        }
    }

    // PHP
    DisposableEffect(phpRuntime) {
        onDispose { phpRuntime.stopServer() }
    }

    // ---- UI ----
    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
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
                                // WordPress localhost load, content
                                settings.mixedContentMode =
                                    android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                // long- press
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
                                loadUrl(url)
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

            "extracting", "starting" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (phase == "extracting") AppStringsProvider.current().wpCheckingDeps
                                   else AppStringsProvider.current().wpStartingServer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            "error" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMsg ?: AppStringsProvider.current().wpStartFailed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Node. js app Shell mode
 * JNI load libnode. so, Node. js, WebView load localhost
 */
@Composable
fun NodeJsShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current

    var phase by remember { mutableStateOf("extracting") } // extracting | starting | ready | error
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val nodeRuntime = remember { com.webtoapp.core.nodejs.NodeRuntime(context) }

    // + Node. js
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // 1. check libnode. so
                val nodePath = com.webtoapp.core.nodejs.NodeDependencyManager.getNodeLibraryPath(context)
                AppLogger.i("NodeJsShell", "nativeLibraryDir: ${context.applicationInfo.nativeLibraryDir}")
                com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "nativeLibraryDir: ${context.applicationInfo.nativeLibraryDir}")
                
                if (nodePath == null) {
                    AppLogger.e("NodeJsShell", "libnode.so 未找到")
                    com.webtoapp.core.shell.ShellLogger.e("NodeJsShell", "libnode.so 未找到")
                    phase = "error"
                    errorMsg = AppStringsProvider.current().nodeRuntimeNotFound
                    return@withContext
                }
                
                AppLogger.i("NodeJsShell", "libnode.so 路径: $nodePath (size=${java.io.File(nodePath).length()})")
                com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "libnode.so 路径: $nodePath (size=${java.io.File(nodePath).length()})")

                // 2. itemfile directory( only)
                val projectDir = File(context.filesDir, "nodejs_site")
                val marker = File(projectDir, ".nodejs_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "nodejs",
                    configVersionCode = config.versionCode,
                    extra = "${config.nodejsConfig.mode}|${config.nodejsConfig.entryFile}"
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("NodeJsShell", "提取 Node.js 项目文件到 ${projectDir.absolutePath}")
                    com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "提取 Node.js 项目文件到 ${projectDir.absolutePath}")
                    projectDir.deleteRecursively()
                    extractAssetsRecursive(context, "nodejs_app", projectDir)
                    writeExtractionMarker(marker, extractionToken)
                }

                // 3. settings
                val envVars = config.nodejsConfig.envVars.toMutableMap()
                val requestPort = config.nodejsConfig.port.takeIf { it > 0 }
                if (requestPort != null && !envVars.containsKey("PORT")) {
                    envVars["PORT"] = requestPort.toString()
                }

                // 4. Node. js( JNI)
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
                    AppLogger.i("NodeJsShell", "Node.js 就绪: $serverUrl")
                    com.webtoapp.core.shell.ShellLogger.i("NodeJsShell", "Node.js 就绪: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = (nodeRuntime.serverState.value as? com.webtoapp.core.nodejs.NodeRuntime.ServerState.Error)
                        ?.message
                        ?: AppStringsProvider.current().nodeServerStartFailed
                    com.webtoapp.core.shell.ShellLogger.e("NodeJsShell", "Node.js 服务器启动失败")
                }
            } catch (e: Exception) {
                AppLogger.e("NodeJsShell", "Node.js Shell 启动失败", e)
                com.webtoapp.core.shell.ShellLogger.e("NodeJsShell", "Node.js Shell 启动失败", e)
                phase = "error"
                errorMsg = e.message ?: AppStringsProvider.current().unknownError
            }
        }
    }

    // Node. js
    DisposableEffect(nodeRuntime) {
        onDispose { nodeRuntime.stopServer() }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
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
                                    this, webViewConfig, webViewCallbacks,
                                    config.extensionModuleIds, config.embeddedExtensionModules,
                                    config.extensionFabIcon, allowGlobalModuleFallback = false)
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
                                loadUrl(url)
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

            "extracting", "starting" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (phase == "extracting") AppStringsProvider.current().preparingNodeEnv
                                   else AppStringsProvider.current().startingNodeServer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            "error" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMsg ?: AppStringsProvider.current().nodeStartFailed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * PHP app Shell mode- from APK assets PHP fileand local PHP
 */
@Composable
fun PhpAppShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // 1. PHP path
                // prefer nativeLibraryDir/libphp. so( Android 15+ executepath)
                // fallback from assets( APK)
                val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
                if (!nativePhp.exists()) {
                    // APK: from assets DependencyManager path
                    val abi = com.webtoapp.core.wordpress.WordPressDependencyManager.getDeviceAbi()
                    val phpDir = com.webtoapp.core.wordpress.WordPressDependencyManager.getPhpDir(context)
                    val phpBinary = File(phpDir, "php")
                    if (!phpBinary.exists() || !phpBinary.canExecute()) {
                        context.assets.open("php/$abi/php").use { input ->
                            phpBinary.outputStream().use { output -> input.copyTo(output) }
                        }
                        phpBinary.setExecutable(true)
                    }
                }

                // 2. PHP itemfile
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

                // 3. PHP
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
                    errorMsg = AppStringsProvider.current().phpStartFailed
                }
            } catch (e: Exception) {
                phase = "error"
                errorMsg = e.message ?: AppStringsProvider.current().unknownError
            }
        }
    }

    DisposableEffect(phpRuntime) { onDispose { phpRuntime.stopServer() } }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                var swipeChildWebView: WebView? = null
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
                                webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false)
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
                                onWebViewCreated(this); webViewRef = this; loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (phase == "extracting") AppStringsProvider.current().wpCheckingDeps else AppStringsProvider.current().wpStartingServer)
                    }
                }
            }
            "error" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMsg ?: AppStringsProvider.current().phpStartFailed, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * Python app Shell mode- from APK assets Python fileand PythonRuntime Python
 */
@Composable
fun PythonAppShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val pyConfig = config.pythonAppConfig
                
                // 1. Python( from assets/python_runtime/ python_deps/python/)
                // Python lib/$abi/libpython3. so nativeLibraryDir
                val pythonHome = com.webtoapp.core.python.PythonDependencyManager.getPythonDir(context)
                val runtimeMarker = File(pythonHome, ".runtime_extracted")
                if (!runtimeMarker.exists()) {
                    AppLogger.i("PythonShell", "首次运行，提取 Python 标准库到 ${pythonHome.absolutePath}")
                    try {
                        val assetChildren = context.assets.list("python_runtime")
                        if (assetChildren != null && assetChildren.isNotEmpty()) {
                            extractAssetsRecursive(context, "python_runtime", pythonHome)
                            runtimeMarker.writeText("extracted")
                            AppLogger.i("PythonShell", "Python 标准库提取完成")
                        } else {
                            AppLogger.w("PythonShell", "assets/python_runtime 不存在或为空，尝试 legacy assets/python/ 路径")
                            // Legacy fallback: from assets/python/$abi/python3
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
                                    AppLogger.i("PythonShell", "Legacy: Python 二进制已提取到 ${pythonBin.absolutePath}")
                                }
                            } catch (e: Exception) {
                                AppLogger.w("PythonShell", "Legacy Python 二进制提取失败: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("PythonShell", "提取 Python 标准库失败", e)
                    }
                } else {
                    AppLogger.i("PythonShell", "Python 标准库已提取，跳过")
                }
                
                // 2. check Python run
                if (!pythonRuntime.isPythonAvailable()) {
                    AppLogger.w("PythonShell", "Python 运行时不可用，回退到预览模式")
                    // fallback: load _preview_. html
                    val projectDir = File(context.filesDir, "python_app_site")
                    projectDir.mkdirs()
                    extractAssetsRecursive(context, "python_app", projectDir)
                    val previewHtml = File(projectDir, "_preview_.html")
                    if (previewHtml.exists()) {
                        serverUrl = "file://${previewHtml.absolutePath}"
                        phase = "ready"
                        AppLogger.i("PythonShell", "回退到预览模式: $serverUrl")
                    } else {
                        phase = "error"
                        errorMsg = AppStringsProvider.current().pythonRuntimeNotFound
                    }
                    return@withContext
                }
                AppLogger.i("PythonShell", "Python 运行时已就绪")

                // 3. itemfile directory( only)
                val projectDir = File(context.filesDir, "python_app_site")
                val marker = File(projectDir, ".python_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "python_app",
                    configVersionCode = config.versionCode,
                    extra = "${pyConfig.framework}|${pyConfig.entryFile}|${pyConfig.entryModule}"
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("PythonShell", "提取 Python 项目文件到 ${projectDir.absolutePath}")
                    projectDir.deleteRecursively()
                    extractAssetsRecursive(context, "python_app", projectDir)
                    writeExtractionMarker(marker, extractionToken)
                } else {
                    AppLogger.i("PythonShell", "已存在提取标记, 跳过提取")
                }

                // 4. Python( PythonRuntime)
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
                    serverUrl = "http://127.0.0.1:$port"
                    phase = "ready"
                    AppLogger.i("PythonShell", "Python 服务器就绪: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = (pythonRuntime.serverState.value as? com.webtoapp.core.python.PythonRuntime.ServerState.Error)
                        ?.message
                        ?: AppStringsProvider.current().pythonServerStartFailed
                    AppLogger.e("PythonShell", "Python 服务器启动失败: $errorMsg")
                }
            } catch (e: Exception) {
                AppLogger.e("PythonShell", "Python Shell 启动失败", e)
                phase = "error"
                errorMsg = e.message ?: AppStringsProvider.current().unknownError
            }
        }
    }

    DisposableEffect(pythonRuntime) { onDispose { pythonRuntime.stopServer() } }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
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
                                webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false)
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
                                onWebViewCreated(this); webViewRef = this; loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (phase) {
                                "extracting" -> AppStringsProvider.current().preparingPythonEnv
                                else -> AppStringsProvider.current().startingPythonServer
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            "error" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            AppStringsProvider.current().pythonServerTimeout,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorMsg ?: AppStringsProvider.current().pythonStartFailed,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Go app Shell mode- from APK assets Go itemfileand GoRuntime
 */
@Composable
fun GoAppShellMode(
    config: com.webtoapp.core.shell.ShellConfig,
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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val goConfig = config.goAppConfig

                // 1. itemfile directory( only)
                val projectDir = File(context.filesDir, "go_app_site")
                val marker = File(projectDir, ".go_extracted")
                val extractionToken = buildExtractionToken(
                    context = context,
                    scope = "go_app",
                    configVersionCode = config.versionCode,
                    extra = "${goConfig.framework}|${goConfig.binaryName}|${goConfig.staticDir}"
                )

                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("GoShell", "提取 Go 项目文件到 ${projectDir.absolutePath}")
                    projectDir.deleteRecursively()
                    extractAssetsRecursive(context, "go_app", projectDir)
                    writeExtractionMarker(marker, extractionToken)
                } else {
                    AppLogger.i("GoShell", "已存在提取标记, 跳过提取")
                }

                // 2. Go
                val binaryName = goConfig.binaryName.ifEmpty {
                    goRuntime.detectBinary(projectDir) ?: ""
                }
                if (binaryName.isEmpty()) {
                    AppLogger.w("GoShell", "未检测到 Go 二进制，回退到预览模式")
                    val previewHtml = File(projectDir, "_preview_.html")
                    if (previewHtml.exists()) {
                        serverUrl = "file://${previewHtml.absolutePath}"
                        phase = "ready"
                        AppLogger.i("GoShell", "回退到预览模式: $serverUrl")
                    } else {
                        phase = "error"
                        errorMsg = AppStringsProvider.current().goBinaryNotFound
                    }
                    return@withContext
                }
                AppLogger.i("GoShell", "检测到 Go 二进制: $binaryName")

                // 3. Go( GoRuntime)
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
                    AppLogger.i("GoShell", "Go 服务器就绪: $serverUrl")
                } else {
                    phase = "error"
                    errorMsg = (goRuntime.serverState.value as? com.webtoapp.core.golang.GoRuntime.ServerState.Error)
                        ?.message
                        ?: AppStringsProvider.current().goServerStartFailed
                    AppLogger.e("GoShell", "Go 服务器启动失败: $errorMsg")
                }
            } catch (e: Exception) {
                AppLogger.e("GoShell", "Go Shell 启动失败", e)
                phase = "error"
                errorMsg = e.message ?: AppStringsProvider.current().unknownError
            }
        }
    }

    DisposableEffect(goRuntime) { onDispose { goRuntime.stopServer() } }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
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
                                webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false)
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
                                onWebViewCreated(this); webViewRef = this; loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (phase) {
                                "extracting" -> AppStringsProvider.current().preparingGoEnv
                                else -> AppStringsProvider.current().startingGoServer
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            "error" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMsg ?: AppStringsProvider.current().goStartFailed, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * Shell mode( for)
 * from assets itemfileand LocalHttpServer + WebView load
 */
@Composable
fun ServerAppShellMode(
    appType: String,
    config: com.webtoapp.core.shell.ShellConfig,
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
                
                AppLogger.i("ServerAppShellMode", "========== $appType Shell 启动 ==========")
                AppLogger.i("ServerAppShellMode", "assetDir=$assetDir, siteDir=${siteDir.absolutePath}")
                AppLogger.i("ServerAppShellMode", "marker exists=${marker.exists()}")
                
                if (appType == "PYTHON_APP") {
                    val pyConfig = config.pythonAppConfig
                    AppLogger.i("ServerAppShellMode", "Python配置: framework=${pyConfig.framework}, entryFile=${pyConfig.entryFile}, entryModule=${pyConfig.entryModule}, serverType=${pyConfig.serverType}, port=${pyConfig.port}")
                    AppLogger.i("ServerAppShellMode", "Python环境变量: ${pyConfig.envVars}")
                } else {
                    val goConfig = config.goAppConfig
                    AppLogger.i("ServerAppShellMode", "Go配置: framework=${goConfig.framework}, binaryName=${goConfig.binaryName}, port=${goConfig.port}, staticDir=${goConfig.staticDir}")
                }
                
                // assets in content
                try {
                    val assetChildren = context.assets.list(assetDir)
                    AppLogger.i("ServerAppShellMode", "assets/$assetDir 内容 (${assetChildren?.size ?: 0} 项): ${assetChildren?.joinToString()}")
                    if (assetChildren.isNullOrEmpty()) {
                        AppLogger.e("ServerAppShellMode", "assets/$assetDir 为空或不存在!")
                    }
                } catch (e: Exception) {
                    AppLogger.e("ServerAppShellMode", "列出 assets/$assetDir 失败", e)
                }
                
                if (shouldReextractAssets(marker, extractionToken)) {
                    AppLogger.i("ServerAppShellMode", "首次提取: 删除旧目录并从 assets 提取...")
                    siteDir.deleteRecursively()
                    extractAssetsRecursive(context, assetDir, siteDir)
                    writeExtractionMarker(marker, extractionToken)
                    AppLogger.i("ServerAppShellMode", "提取完成, marker 已创建")
                } else {
                    AppLogger.i("ServerAppShellMode", "已存在提取标记, 跳过提取")
                }
                
                // directory( 30)
                val fileList = StringBuilder()
                siteDir.walkTopDown().take(30).forEach { f ->
                    val rel = f.relativeTo(siteDir).path
                    val info = if (f.isDirectory) "[DIR]" else "(${f.length()} bytes)"
                    fileList.appendLine("  $rel $info")
                }
                val totalFiles = siteDir.walkTopDown().filter { it.isFile }.count()
                AppLogger.i("ServerAppShellMode", "提取后目录结构 ($totalFiles 个文件):\n$fileList")
                
                phase = "starting"

                // LocalHttpServer loaditem
                val candidates = listOf("dist", "build", "public", "static", "www", "")
                var docRoot: File? = null
                for (dir in candidates) {
                    val candidate = if (dir.isEmpty()) siteDir else File(siteDir, dir)
                    val indexExists = File(candidate, "index.html").exists()
                    val isDir = candidate.isDirectory
                    AppLogger.d("ServerAppShellMode", "检查候选目录: '$dir' -> ${candidate.absolutePath}, isDir=$isDir, hasIndex=$indexExists")
                    if (isDir && indexExists) {
                        docRoot = candidate
                        AppLogger.i("ServerAppShellMode", "找到 docRoot: ${candidate.absolutePath}")
                        break
                    }
                }
                
                if (docRoot != null) {
                    serverUrl = httpServer.start(docRoot)
                    phase = "ready"
                    AppLogger.i("ServerAppShellMode", "LocalHttpServer 已启动, URL=$serverUrl")
                } else {
                    AppLogger.w("ServerAppShellMode", "所有候选目录均未找到 index.html")
                    // index. html, file: // load
                    serverUrl = "file://${siteDir.absolutePath}/index.html"
                    if (File(siteDir, "index.html").exists()) {
                        phase = "ready"
                        AppLogger.i("ServerAppShellMode", "回退到 file:// 模式: $serverUrl")
                    } else {
                        phase = "error"
                        errorMsg = AppStringsProvider.current().serverStartFailed
                        AppLogger.e("ServerAppShellMode", "最终失败: siteDir 中也没有 index.html")
                        AppLogger.e("ServerAppShellMode", "Python/Go 后端应用需要对应运行时才能提供页面，当前实现仅支持静态前端产物")
                    }
                }
                
                AppLogger.i("ServerAppShellMode", "========== 最终状态: phase=$phase, serverUrl=$serverUrl, error=$errorMsg ==========")
            } catch (e: Exception) {
                phase = "error"
                errorMsg = e.message ?: AppStringsProvider.current().serverStartFailed
                AppLogger.e("ServerAppShellMode", "启动异常", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (phase) {
            "ready" -> {
                val url = serverUrl ?: return@Box
                AppLogger.i("ServerAppShellMode", "WebView ready, 即将加载 URL: $url")
                var swipeChildWebView: WebView? = null
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
                                webViewManager.configureWebView(this, webViewConfig, webViewCallbacks, config.extensionModuleIds, config.embeddedExtensionModules, config.extensionFabIcon, allowGlobalModuleFallback = false)
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
                                AppLogger.i("ServerAppShellMode", "WebView created, calling loadUrl('$url')")
                                loadUrl(url)
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
            "extracting", "starting" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (phase == "extracting") AppStringsProvider.current().preparingEnv else AppStringsProvider.current().startingServer)
                    }
                }
            }
            "error" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMsg ?: AppStringsProvider.current().serverStartFailed, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
