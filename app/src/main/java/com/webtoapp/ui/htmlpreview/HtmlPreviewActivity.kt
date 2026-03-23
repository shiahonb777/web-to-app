package com.webtoapp.ui.htmlpreview

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.WebToAppTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * HTML预览Activity
 * 用于预览AI生成的HTML代码
 */
class HtmlPreviewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_HTML_CONTENT = "extra_html_content"
        const val EXTRA_TITLE = "extra_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "预览"
        
        setContent {
            WebToAppTheme { _ ->
                HtmlPreviewScreen(
                    filePath = filePath,
                    htmlContent = htmlContent,
                    title = title,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HtmlPreviewScreen(
    filePath: String?,
    htmlContent: String?,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showDevTools by remember { mutableStateOf(false) }
    var consoleMessages by remember { mutableStateOf<List<ConsoleLogEntry>>(emptyList()) }
    var isDevToolsExpanded by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var sourceCode by remember { mutableStateOf("") }
    
    // 读取源代码
    LaunchedEffect(filePath, htmlContent) {
        sourceCode = when {
            filePath != null -> try { File(filePath).readText() } catch (e: Exception) { "无法读取文件" }
            htmlContent != null -> htmlContent
            else -> ""
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        if (currentUrl.isNotEmpty()) {
                            Text(
                                currentUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    // 查看源代码
                    IconButton(onClick = { showSourceDialog = true }) {
                        Icon(Icons.Outlined.Description, "查看源代码")
                    }
                    // Refresh
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    // 开发者工具
                    IconButton(onClick = { showDevTools = !showDevTools }) {
                        BadgedBox(
                            badge = {
                                if (consoleMessages.any { it.level == ConsoleLevel.ERROR }) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(
                                if (showDevTools) Icons.Filled.Code else Icons.Outlined.Code,
                                "开发者工具"
                            )
                        }
                    }
                    // 在浏览器中打开
                    IconButton(onClick = {
                        filePath?.let { path ->
                            try {
                                val file = File(path)
                                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                } else {
                                    Uri.fromFile(file)
                                }
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "text/html")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(intent, "选择浏览器"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "${Strings.cannotOpenInBrowser}: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Toast.makeText(context, Strings.noFilePathAvailable, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Outlined.OpenInBrowser, "在浏览器中打开")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Load进度条
            if (isLoading) {
                LinearProgressIndicator(
                    progress = loadProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // WebView
            Box(modifier = Modifier.weight(1f)) {
                val activity = context as? ComponentActivity
                val lifecycleScope = activity?.lifecycleScope
                
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this
                            // SetWebView背景为白色，避免继承主题颜色
                            setBackgroundColor(android.graphics.Color.WHITE)
                            
                            // 添加下载桥接（支持 Blob/Data URL 下载）
                            lifecycleScope?.let { scope ->
                                val downloadBridge = com.webtoapp.core.webview.DownloadBridge(ctx, scope)
                                addJavascriptInterface(downloadBridge, com.webtoapp.core.webview.DownloadBridge.JS_INTERFACE_NAME)
                            }
                            
                            setupWebView(
                                onProgressChanged = { progress ->
                                    loadProgress = progress
                                    isLoading = progress < 100
                                },
                                onPageStarted = { url ->
                                    currentUrl = url
                                    isLoading = true
                                },
                                onPageFinished = {
                                    isLoading = false
                                },
                                onConsoleMessage = { entry ->
                                    consoleMessages = consoleMessages + entry
                                }
                            )
                            
                            // Load内容
                            // 使用 HTTPS baseURL 来允许加载 CDN 资源
                            // 这是解决 file:// 协议无法加载外部脚本的关键
                            when {
                                filePath != null -> {
                                    val file = File(filePath)
                                    val htmlContent = file.readText()
                                    val baseDir = file.parentFile?.absolutePath ?: ""
                                    
                                    // 使用虚拟 HTTPS baseURL，让 WebView 认为页面来自网络
                                    // Local资源通过 shouldInterceptRequest 拦截处理
                                    loadDataWithBaseURL(
                                        "https://localhost/__local__/$baseDir/",
                                        htmlContent,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                                htmlContent != null -> {
                                    loadDataWithBaseURL(
                                        "https://localhost/__local__/",
                                        htmlContent,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)  // 确保容器背景也是白色
                )
            }
            
            // 开发者工具面板
            AnimatedVisibility(
                visible = showDevTools,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                DevToolsPanel(
                    consoleMessages = consoleMessages,
                    isExpanded = isDevToolsExpanded,
                    onExpandToggle = { isDevToolsExpanded = !isDevToolsExpanded },
                    onClear = { consoleMessages = emptyList() },
                    onRunScript = { script ->
                        webView?.evaluateJavascript(script) { result ->
                            consoleMessages = consoleMessages + ConsoleLogEntry(
                                level = ConsoleLevel.LOG,
                                message = "=> $result",
                                source = "eval",
                                lineNumber = 0,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                    },
                    modifier = if (isDevToolsExpanded) Modifier.fillMaxHeight(0.6f) else Modifier.heightIn(max = 200.dp)
                )
            }
        }
    }
    
    // 源代码查看对话框
    if (showSourceDialog) {
        SourceCodeDialog(
            sourceCode = sourceCode,
            onDismiss = { showSourceDialog = false }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(
    onProgressChanged: (Int) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: () -> Unit,
    onConsoleMessage: (ConsoleLogEntry) -> Unit
) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false
        setSupportZoom(true)
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        // Allow本地文件访问（HTML中的相对路径资源，如 JS/CSS 文件）
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = true
        // 确保 JavaScript 可以正常执行
        javaScriptCanOpenWindowsAutomatically = true
        // Support数据库
        databaseEnabled = true
    }
    
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { onPageStarted(it) }
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished()
            
            // Inject下载桥接脚本（支持 Blob/Data URL 下载）
            view?.evaluateJavascript(com.webtoapp.core.webview.DownloadBridge.getInjectionScript(), null)
            
            // Debug：检查 JavaScript 是否可用
            view?.evaluateJavascript("""
                (function() {
                    console.log('[DEBUG] JavaScript is working!');
                    console.log('[DEBUG] Document ready state: ' + document.readyState);
                    console.log('[DEBUG] Script tags count: ' + document.getElementsByTagName('script').length);
                    console.log('[DEBUG] Vue available: ' + (typeof Vue !== 'undefined'));
                    console.log('[DEBUG] React available: ' + (typeof React !== 'undefined'));
                    var scripts = document.getElementsByTagName('script');
                    for (var i = 0; i < scripts.length; i++) {
                        var script = scripts[i];
                        var src = script.src || '(inline)';
                        var contentLength = script.textContent ? script.textContent.length : 0;
                        console.log('[DEBUG] Script ' + i + ': src=' + src + ', contentLength=' + contentLength);
                    }
                    return 'JS check complete';
                })();
            """.trimIndent()) { result ->
                Log.d("HtmlPreviewActivity", "JS check result: $result")
            }
        }
        
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false  // 在WebView内处理所有链接
        }
        
        /**
         * 拦截资源请求
         * - 本地资源（通过虚拟 baseURL 请求）：从文件系统加载
         * - 外部资源（CDN 等）：正常网络请求
         */
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null
            
            Log.d("HtmlPreviewActivity", "shouldInterceptRequest: $url")
            
            // Check是否是本地资源请求（通过虚拟 baseURL）
            if (url.startsWith("https://localhost/__local__/")) {
                val localPath = url.removePrefix("https://localhost/__local__/")
                Log.d("HtmlPreviewActivity", "Loading local resource: $localPath")
                
                return try {
                    val file = java.io.File(localPath)
                    if (file.exists() && file.isFile) {
                        val mimeType = getMimeTypeForFile(localPath)
                        val inputStream = java.io.FileInputStream(file)
                        WebResourceResponse(mimeType, "UTF-8", inputStream)
                    } else {
                        Log.w("HtmlPreviewActivity", "Local file not found: $localPath")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("HtmlPreviewActivity", "Error loading local resource: $localPath", e)
                    null
                }
            }
            
            // External资源（CDN 等）：返回 null 让系统正常处理网络请求
            return null
        }
        
        private fun getMimeTypeForFile(path: String): String {
            val extension = path.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "html", "htm" -> "text/html"
                "css" -> "text/css"
                "js" -> "application/javascript"
                "json" -> "application/json"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "gif" -> "image/gif"
                "svg" -> "image/svg+xml"
                "woff" -> "font/woff"
                "woff2" -> "font/woff2"
                "ttf" -> "font/ttf"
                else -> "application/octet-stream"
            }
        }
    }
    
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgressChanged(newProgress)
        }
        
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            consoleMessage?.let {
                val level = when (it.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> ConsoleLevel.ERROR
                    ConsoleMessage.MessageLevel.WARNING -> ConsoleLevel.WARNING
                    ConsoleMessage.MessageLevel.LOG -> ConsoleLevel.LOG
                    ConsoleMessage.MessageLevel.DEBUG -> ConsoleLevel.DEBUG
                    else -> ConsoleLevel.INFO
                }
                onConsoleMessage(ConsoleLogEntry(
                    level = level,
                    message = it.message(),
                    source = it.sourceId() ?: "unknown",
                    lineNumber = it.lineNumber(),
                    timestamp = System.currentTimeMillis()
                ))
            }
            return true
        }
        
        // Support全屏视频等
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)
        }
        
        override fun onHideCustomView() {
            super.onHideCustomView()
        }
    }
}

// 控制台日志级别
enum class ConsoleLevel {
    LOG, INFO, WARNING, ERROR, DEBUG
}

// 控制台日志条目
data class ConsoleLogEntry(
    val level: ConsoleLevel,
    val message: String,
    val source: String,
    val lineNumber: Int,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevToolsPanel(
    consoleMessages: List<ConsoleLogEntry>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onClear: () -> Unit,
    onRunScript: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var scriptInput by remember { mutableStateOf("") }
    var selectedMessage by remember { mutableStateOf<ConsoleLogEntry?>(null) }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    
    // Auto滚动到底部
    LaunchedEffect(consoleMessages.size) {
        if (consoleMessages.isNotEmpty()) {
            listState.animateScrollToItem(consoleMessages.size - 1)
        }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E),
        tonalElevation = 8.dp
    ) {
        Column {
            // 头部工具栏
            Surface(
                color = Color(0xFF2D2D2D),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            Strings.console,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        // Error/警告计数
                        val errorCount = consoleMessages.count { it.level == ConsoleLevel.ERROR }
                        val warnCount = consoleMessages.count { it.level == ConsoleLevel.WARNING }
                        if (errorCount > 0) {
                            Surface(
                                color = Color(0xFFCF6679),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "$errorCount",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                        if (warnCount > 0) {
                            Surface(
                                color = Color(0xFFFFB74D),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "$warnCount",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Copy全部
                        IconButton(
                            onClick = {
                                val allLogs = consoleMessages.joinToString("\n") { entry ->
                                    "[${timeFormat.format(Date(entry.timestamp))}] [${entry.level}] ${entry.message} (${entry.source}:${entry.lineNumber})"
                                }
                                clipboardManager.setText(AnnotatedString(allLogs))
                                Toast.makeText(context, Strings.copiedAllLogs, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = Strings.copyAll,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // 清空
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = Strings.clear,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // Expand/收起
                        IconButton(
                            onClick = onExpandToggle,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = if (isExpanded) Strings.collapse else Strings.expand,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // 控制台消息列表
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (consoleMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            Strings.noConsoleMessages,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(consoleMessages) { entry ->
                            ConsoleLogItem(
                                entry = entry,
                                timeFormat = timeFormat,
                                isSelected = selectedMessage == entry,
                                onClick = { selectedMessage = if (selectedMessage == entry) null else entry },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(entry.message))
                                    Toast.makeText(context, Strings.copied, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
            
            // Script输入区
            Surface(
                color = Color(0xFF2D2D2D),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ">",
                        color = Color(0xFF4FC3F7),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedTextField(
                        value = scriptInput,
                        onValueChange = { scriptInput = it },
                        placeholder = { 
                            Text(
                                Strings.inputJavaScriptExpression,
                                color = Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4FC3F7),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = {
                            if (scriptInput.isNotBlank()) {
                                onRunScript(scriptInput)
                                scriptInput = ""
                            }
                        },
                        enabled = scriptInput.isNotBlank(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFF4FC3F7),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = Strings.run)
                    }
                }
            }
        }
    }
    
    // 消息详情对话框
    selectedMessage?.let { entry ->
        MessageDetailDialog(
            entry = entry,
            timeFormat = timeFormat,
            onDismiss = { selectedMessage = null }
        )
    }
}

@Composable
private fun ConsoleLogItem(
    entry: ConsoleLogEntry,
    timeFormat: SimpleDateFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit
) {
    val backgroundColor = when (entry.level) {
        ConsoleLevel.ERROR -> Color(0xFF4A1A1A)
        ConsoleLevel.WARNING -> Color(0xFF4A3A1A)
        else -> if (isSelected) Color(0xFF3A3A3A) else Color.Transparent
    }
    
    val textColor = when (entry.level) {
        ConsoleLevel.ERROR -> Color(0xFFCF6679)
        ConsoleLevel.WARNING -> Color(0xFFFFB74D)
        ConsoleLevel.DEBUG -> Color(0xFF81C784)
        else -> Color.White.copy(alpha = 0.9f)
    }
    
    val icon = when (entry.level) {
        ConsoleLevel.ERROR -> "❌"
        ConsoleLevel.WARNING -> "⚠️"
        ConsoleLevel.DEBUG -> "🔍"
        ConsoleLevel.INFO -> "ℹ️"
        ConsoleLevel.LOG -> "📝"
    }
    
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Text(
                icon,
                modifier = Modifier.padding(end = 8.dp),
                fontSize = 12.sp
            )
            
            // 消息内容
            Column(modifier = Modifier.weight(1f)) {
                SelectionContainer {
                    Text(
                        entry.message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = textColor
                    )
                }
                
                // 来源信息
                Text(
                    "${entry.source}:${entry.lineNumber} • ${timeFormat.format(Date(entry.timestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Copy按钮
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = Strings.copy,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageDetailDialog(
    entry: ConsoleLogEntry,
    timeFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column {
                // 头部
                Surface(
                    color = Color(0xFF2D2D2D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.logDetails,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Row {
                            IconButton(onClick = {
                                val fullLog = """
Level: ${entry.level}
Time: ${timeFormat.format(Date(entry.timestamp))}
Source: ${entry.source}:${entry.lineNumber}

Message:
${entry.message}
                                """.trimIndent()
                                clipboardManager.setText(AnnotatedString(fullLog))
                                Toast.makeText(context, Strings.copiedFullLog, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = Strings.copy, tint = Color.White)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = Strings.close, tint = Color.White)
                            }
                        }
                    }
                }
                
                // 内容
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 元信息
                    InfoRow(Strings.level, entry.level.name)
                    InfoRow(Strings.time, timeFormat.format(Date(entry.timestamp)))
                    InfoRow(Strings.source, "${entry.source}:${entry.lineNumber}")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        Strings.messageContent,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 消息内容（可选择复制）
                    Surface(
                        color = Color(0xFF2D2D2D),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                entry.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 22.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

@Composable
private fun SourceCodeDialog(
    sourceCode: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column {
                // 头部
                Surface(
                    color = Color(0xFF2D2D2D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.sourceCode,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Row {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(sourceCode))
                                Toast.makeText(context, Strings.copiedSourceCode, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = Strings.copy, tint = Color.White)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = Strings.close, tint = Color.White)
                            }
                        }
                    }
                }
                
                // 源代码内容
                val lines = sourceCode.lines()
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lines.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color.Transparent else Color(0xFF252525))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            // 行号
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.width(40.dp)
                            )
                            // 代码
                            SelectionContainer {
                                Text(
                                    lines[index],
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
