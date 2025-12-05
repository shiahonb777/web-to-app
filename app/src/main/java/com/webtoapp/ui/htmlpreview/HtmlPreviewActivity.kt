package com.webtoapp.ui.htmlpreview

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.ui.theme.WebToAppTheme
import java.io.File

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
            WebToAppTheme {
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
    var consoleMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    
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
                        Icon(Icons.Default.Close, "关闭")
                    }
                },
                actions = {
                    // 刷新
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                    // 开发者工具
                    IconButton(onClick = { showDevTools = !showDevTools }) {
                        Icon(
                            if (showDevTools) Icons.Filled.Code else Icons.Outlined.Code,
                            "开发者工具"
                        )
                    }
                    // 在浏览器中打开
                    IconButton(onClick = {
                        filePath?.let {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.fromFile(File(it)), "text/html")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法在外部浏览器中打开", Toast.LENGTH_SHORT).show()
                            }
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
            // 加载进度条
            if (isLoading) {
                LinearProgressIndicator(
                    progress = loadProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // WebView
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this
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
                                onConsoleMessage = { message ->
                                    consoleMessages = consoleMessages + message
                                }
                            )
                            
                            // 加载内容
                            when {
                                filePath != null -> {
                                    loadUrl("file://$filePath")
                                }
                                htmlContent != null -> {
                                    loadDataWithBaseURL(
                                        "file:///android_asset/",
                                        htmlContent,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 开发者工具面板
            if (showDevTools) {
                DevToolsPanel(
                    consoleMessages = consoleMessages,
                    onClear = { consoleMessages = emptyList() },
                    onRunScript = { script ->
                        webView?.evaluateJavascript(script) { result ->
                            consoleMessages = consoleMessages + "=> $result"
                        }
                    },
                    modifier = Modifier.heightIn(max = 200.dp)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(
    onProgressChanged: (Int) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: () -> Unit,
    onConsoleMessage: (String) -> Unit
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
    }
    
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { onPageStarted(it) }
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished()
        }
        
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false  // 在WebView内处理所有链接
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
                    ConsoleMessage.MessageLevel.ERROR -> "❌"
                    ConsoleMessage.MessageLevel.WARNING -> "⚠️"
                    ConsoleMessage.MessageLevel.LOG -> "📝"
                    ConsoleMessage.MessageLevel.DEBUG -> "🔍"
                    else -> "ℹ️"
                }
                onConsoleMessage("$level ${it.message()} (${it.lineNumber()})")
            }
            return true
        }
        
        // 支持全屏视频等
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)
        }
        
        override fun onHideCustomView() {
            super.onHideCustomView()
        }
    }
}

@Composable
private fun DevToolsPanel(
    consoleMessages: List<String>,
    onClear: () -> Unit,
    onRunScript: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var scriptInput by remember { mutableStateOf("") }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Console",
                    style = MaterialTheme.typography.labelMedium
                )
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        "清空",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // 控制台消息
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                if (consoleMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无控制台消息",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        items(consoleMessages.size) { index ->
                            Text(
                                consoleMessages[index],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // 脚本输入
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = scriptInput,
                    onValueChange = { scriptInput = it },
                    placeholder = { Text("输入JavaScript...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilledTonalIconButton(
                    onClick = {
                        if (scriptInput.isNotBlank()) {
                            onRunScript(scriptInput)
                            scriptInput = ""
                        }
                    },
                    enabled = scriptInput.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, "运行")
                }
            }
        }
    }
}
