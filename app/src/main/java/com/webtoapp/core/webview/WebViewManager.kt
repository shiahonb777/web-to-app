package com.webtoapp.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.*
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.crypto.SecureAssetLoader
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ExtensionPanelScript
import com.webtoapp.core.extension.ModuleRunTime
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.UserScript
import com.webtoapp.data.model.WebViewConfig
import java.io.ByteArrayInputStream

/**
 * WebView管理器 - 配置和管理WebView
 */
class WebViewManager(
    private val context: Context,
    private val adBlocker: AdBlocker
) {
    
    companion object {
        // 桌面版 Chrome User-Agent
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    // 应用配置的扩展模块ID列表
    private var appExtensionModuleIds: List<String> = emptyList()
    
    // 嵌入的扩展模块数据（Shell 模式使用）
    private var embeddedModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList()
    
    // 跟踪已配置的 WebView，用于资源清理
    private val managedWebViews = java.util.WeakHashMap<WebView, Boolean>()

    /**
     * 配置WebView
     * @param webView WebView实例
     * @param config WebView配置
     * @param callbacks 回调接口
     * @param extensionModuleIds 应用配置的扩展模块ID列表（可选）
     * @param embeddedExtensionModules 嵌入的扩展模块数据（Shell模式使用，可选）
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(
        webView: WebView,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        extensionModuleIds: List<String> = emptyList(),
        embeddedExtensionModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList()
    ) {
        // 保存扩展模块ID列表
        this.appExtensionModuleIds = extensionModuleIds
        // 保存嵌入的模块数据
        this.embeddedModules = embeddedExtensionModules
        
        // 调试日志：确认扩展模块配置
        android.util.Log.d("WebViewManager", "configureWebView: extensionModuleIds=${extensionModuleIds.size}, embeddedModules=${embeddedExtensionModules.size}")
        embeddedExtensionModules.forEach { module ->
            android.util.Log.d("WebViewManager", "  嵌入模块: id=${module.id}, name=${module.name}, enabled=${module.enabled}, runAt=${module.runAt}")
        }
        
        // 跟踪此 WebView
        managedWebViews[webView] = true
        
        webView.apply {
            settings.apply {
                // JavaScript
                javaScriptEnabled = config.javaScriptEnabled
                javaScriptCanOpenWindowsAutomatically = true

                // DOM存储
                domStorageEnabled = config.domStorageEnabled
                databaseEnabled = true

                // 文件访问
                allowFileAccess = config.allowFileAccess
                allowContentAccess = config.allowContentAccess

                // 缓存
                cacheMode = if (config.cacheEnabled) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_NO_CACHE
                }

                // 缩放
                setSupportZoom(config.zoomEnabled)
                builtInZoomControls = config.zoomEnabled
                displayZoomControls = false

                // 视口
                useWideViewPort = true
                loadWithOverviewMode = true

                // User Agent
                config.userAgent?.let { ua ->
                    userAgentString = ua
                }

                // 桌面模式 - 使用完整的桌面版 User-Agent
                if (config.desktopMode) {
                    userAgentString = DESKTOP_USER_AGENT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    // 设置默认缩放级别以适应桌面版页面
                    textZoom = 100
                }

                // 混合内容 - 允许 HTTPS 页面加载 HTTP 资源和请求 HTTP 接口
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // 其他设置
                mediaPlaybackRequiresUserGesture = false
                
                // 允许 file:// 协议的页面加载外部资源（CDN 等）
                // 这对于本地前端项目加载框架 CDN 是必需的
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            // 滚动条
            isScrollbarFadingEnabled = true
            scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

            // 硬件加速
            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

            // WebViewClient
            webViewClient = createWebViewClient(config, callbacks)

            // WebChromeClient
            webChromeClient = createWebChromeClient(callbacks)
            
            // 下载监听器
            if (config.downloadEnabled) {
                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    callbacks.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
                }
            }
        }
    }

    /**
     * 创建WebViewClient
     */
    private fun createWebViewClient(
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ): WebViewClient {
        return object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.let {
                    val url = it.url?.toString() ?: ""
                    
                    // 调试日志：记录所有请求
                    android.util.Log.d("WebViewManager", "shouldInterceptRequest: $url")
                    
                    // 处理本地资源请求（通过虚拟 baseURL）
                    // 这是为了支持 CDN 加载而使用 loadDataWithBaseURL 的方案
                    if (url.startsWith("https://localhost/__local__/")) {
                        val localPath = url.removePrefix("https://localhost/__local__/")
                        android.util.Log.d("WebViewManager", "Loading local resource: $localPath")
                        
                        return try {
                            val file = java.io.File(localPath)
                            if (file.exists() && file.isFile) {
                                val mimeType = getMimeType(localPath)
                                val inputStream = java.io.FileInputStream(file)
                                WebResourceResponse(mimeType, "UTF-8", inputStream)
                            } else {
                                android.util.Log.w("WebViewManager", "Local file not found: $localPath")
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WebViewManager", "Error loading local resource: $localPath", e)
                            null
                        }
                    }
                    
                    // 只处理本地 asset 请求，外部网络请求直接放行
                    if (url.startsWith("file:///android_asset/")) {
                        val assetPath = url.removePrefix("file:///android_asset/")
                        return loadEncryptedAsset(assetPath)
                    }
                    
                    // 外部请求：只对非本地请求进行广告拦截
                    if ((url.startsWith("http://") || url.startsWith("https://")) &&
                        !url.startsWith("https://localhost/__local__/") &&
                        adBlocker.isEnabled() && adBlocker.shouldBlock(it)) {
                        android.util.Log.d("WebViewManager", "广告拦截: $url")
                        return adBlocker.createEmptyResponse()
                    }
                }
                // 返回 null 让系统处理（包括外部网络请求）
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // 处理特殊协议
                if (handleSpecialUrl(url)) {
                    return true
                }

                // 外部链接处理
                if (config.openExternalLinks && isExternalUrl(url, view?.url)) {
                    callbacks.onExternalLink(url)
                    return true
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                callbacks.onPageStarted(url)
                // 注入 DOCUMENT_START 脚本（使用传入的 url 参数，因为此时 webView.url 可能还是旧值）
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_START, url) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 注入 DOCUMENT_END 脚本
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_END, url) }
                callbacks.onPageFinished(url)
                // 注入 DOCUMENT_IDLE 脚本（延迟执行）
                view?.postDelayed({
                    injectScripts(view, config.injectScripts, ScriptRunTime.DOCUMENT_IDLE, view.url)
                }, 500)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    callbacks.onError(
                        error?.errorCode ?: -1,
                        error?.description?.toString() ?: "Unknown error"
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                // 默认拒绝不安全的SSL连接
                handler?.cancel()
                callbacks.onSslError(error?.toString() ?: "SSL Error")
            }
        }
    }
    
    /**
     * 加载加密的 asset 资源
     * 如果资源被加密，则解密后返回；否则返回原始资源
     * 
     * @param assetPath asset 路径（不含 file:///android_asset/ 前缀）
     * @return WebResourceResponse 或 null（让系统处理）
     */
    private fun loadEncryptedAsset(assetPath: String): WebResourceResponse? {
        return try {
            val secureLoader = SecureAssetLoader.getInstance(context)
            
            // 检查资源是否存在（加密或未加密）
            if (!secureLoader.assetExists(assetPath)) {
                android.util.Log.d("WebViewManager", "资源不存在: $assetPath")
                return null
            }
            
            // 加载资源（自动处理加密/未加密）
            val data = secureLoader.loadAsset(assetPath)
            val mimeType = getMimeType(assetPath)
            val encoding = if (isTextMimeType(mimeType)) "UTF-8" else null
            
            android.util.Log.d("WebViewManager", "加载资源: $assetPath (${data.size} bytes, $mimeType)")
            
            WebResourceResponse(
                mimeType,
                encoding,
                ByteArrayInputStream(data)
            )
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "加载资源失败: $assetPath", e)
            null
        }
    }
    
    /**
     * 根据文件扩展名获取 MIME 类型
     */
    private fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "txt" -> "text/plain"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "eot" -> "application/vnd.ms-fontobject"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * 判断是否为文本类型的 MIME
     */
    private fun isTextMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("text/") ||
               mimeType == "application/javascript" ||
               mimeType == "application/json" ||
               mimeType == "application/xml" ||
               mimeType == "image/svg+xml"
    }

    /**
     * 创建WebChromeClient
     */
    private fun createWebChromeClient(callbacks: WebViewCallbacks): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                callbacks.onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                callbacks.onTitleChanged(title)
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                callbacks.onIconReceived(icon)
            }

            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                callbacks.onShowCustomView(view, callback)
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                callbacks.onHideCustomView()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callbacks.onGeolocationPermission(origin, callback)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                callbacks.onPermissionRequest(request)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val level = when (it.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> 4
                        ConsoleMessage.MessageLevel.WARNING -> 3
                        ConsoleMessage.MessageLevel.LOG -> 1
                        ConsoleMessage.MessageLevel.DEBUG -> 0
                        else -> 2
                    }
                    callbacks.onConsoleMessage(
                        level,
                        it.message() ?: "",
                        it.sourceId() ?: "unknown",
                        it.lineNumber()
                    )
                }
                return true
            }

            // 文件选择
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return callbacks.onShowFileChooser(filePathCallback, fileChooserParams)
            }
        }
    }

    /**
     * 处理特殊URL（电话、邮件、短信、第三方App等）
     */
    private fun handleSpecialUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false
        
        // http/https 由 WebView 处理
        if (scheme == "http" || scheme == "https") {
            return false
        }
        
        // 所有非 http/https 协议都尝试用外部应用打开
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // 没有应用能处理此协议，静默失败
            android.util.Log.w("WebViewManager", "No app to handle scheme: $scheme")
            true // 返回true阻止WebView加载，避免ERR_UNKNOWN_URL_SCHEME
        }
    }

    /**
     * 判断是否为外部链接
     */
    private fun isExternalUrl(targetUrl: String, currentUrl: String?): Boolean {
        if (currentUrl == null) return false
        val targetHost = Uri.parse(targetUrl).host ?: return false
        val currentHost = Uri.parse(currentUrl).host ?: return false
        return !targetHost.endsWith(currentHost) && !currentHost.endsWith(targetHost)
    }
    
    /**
     * 清理 WebView 资源，防止内存泄漏
     * 应在 Activity/Fragment 销毁时调用
     */
    fun destroyWebView(webView: WebView) {
        try {
            managedWebViews.remove(webView)
            
            webView.apply {
                // 停止加载
                stopLoading()
                
                // 清除历史和缓存
                clearHistory()
                
                // 移除所有回调
                webChromeClient = null
                webViewClient = object : WebViewClient() {}
                
                // 清除 JavaScript 接口
                removeJavascriptInterface("NativeBridge")
                removeJavascriptInterface("DownloadBridge")
                
                // 加载空白页面释放资源
                loadUrl("about:blank")
                
                // 从父视图移除
                (parent as? android.view.ViewGroup)?.removeView(this)
                
                // 销毁 WebView
                destroy()
            }
            
            android.util.Log.d("WebViewManager", "WebView 资源已清理")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "清理 WebView 失败", e)
        }
    }
    
    /**
     * 清理所有管理的 WebView
     */
    fun destroyAll() {
        managedWebViews.keys.toList().forEach { webView ->
            destroyWebView(webView)
        }
        managedWebViews.clear()
    }
    
    /**
     * 注入用户脚本
     * @param webView WebView实例
     * @param scripts 用户脚本列表
     * @param runAt 运行时机
     * @param pageUrl 当前页面URL（可选，如果不提供则从webView获取）
     */
    private fun injectScripts(webView: WebView, scripts: List<UserScript>, runAt: ScriptRunTime, pageUrl: String? = null) {
        // 在 DOCUMENT_START 时注入下载桥接脚本（确保最早注入）
        if (runAt == ScriptRunTime.DOCUMENT_START) {
            injectDownloadBridgeScript(webView)
            // 注入统一扩展面板脚本
            injectExtensionPanelScript(webView)
            // 注入隔离环境脚本（最早注入以确保指纹伪造生效）
            injectIsolationScript(webView)
        }
        
        // 注入用户自定义脚本
        scripts.filter { it.enabled && it.runAt == runAt && it.code.isNotBlank() }
            .forEach { script ->
                try {
                    // 包装脚本，添加错误处理
                    val wrappedCode = """
                        (function() {
                            try {
                                ${script.code}
                            } catch(e) {
                                console.error('[UserScript: ${script.name}] Error:', e);
                            }
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(wrappedCode, null)
                    android.util.Log.d("WebViewManager", "注入脚本: ${script.name} (${runAt.name})")
                } catch (e: Exception) {
                    android.util.Log.e("WebViewManager", "脚本注入失败: ${script.name}", e)
                }
            }
        
        // 注入扩展模块代码
        // 优先使用传入的 pageUrl，因为在 onPageStarted 时 webView.url 可能还是旧值
        val url = pageUrl ?: webView.url ?: ""
        
        // 调试日志
        android.util.Log.d("WebViewManager", "injectScripts: runAt=${runAt.name}, url=$url, embeddedModules=${embeddedModules.size}, appExtensionModuleIds=${appExtensionModuleIds.size}")
        
        // 优先使用嵌入的模块数据（Shell 模式）
        if (embeddedModules.isNotEmpty()) {
            injectEmbeddedModules(webView, url, runAt)
        } else if (appExtensionModuleIds.isNotEmpty()) {
            // 使用应用配置的扩展模块
            injectSpecificModules(webView, url, runAt, appExtensionModuleIds)
        } else {
            // 使用全局启用的扩展模块
            injectExtensionModules(webView, url, runAt)
        }
    }
    
    /**
     * 注入嵌入的扩展模块代码（Shell 模式专用）
     * 每个模块独立执行，一个模块出错不影响其他模块
     */
    private fun injectEmbeddedModules(webView: WebView, url: String, runAt: ScriptRunTime) {
        try {
            val targetRunAt = runAt.name
            
            // 调试日志：显示过滤前的状态
            android.util.Log.d("WebViewManager", "injectEmbeddedModules: url=$url, runAt=$targetRunAt, totalModules=${embeddedModules.size}")
            
            val matchingModules = embeddedModules.filter { module ->
                val enabledMatch = module.enabled
                val runAtMatch = module.runAt == targetRunAt
                val urlMatch = module.matchesUrl(url)
                
                // 调试日志：显示每个模块的匹配情况
                android.util.Log.d("WebViewManager", "  模块[${module.name}]: enabled=$enabledMatch, runAt=${module.runAt}==$targetRunAt?$runAtMatch, urlMatch=$urlMatch")
                
                enabledMatch && runAtMatch && urlMatch
            }
            
            if (matchingModules.isEmpty()) {
                android.util.Log.d("WebViewManager", "injectEmbeddedModules: 没有匹配的模块")
                return
            }
            
            // 每个模块独立包装，错误隔离
            val injectionCode = matchingModules.joinToString("\n\n") { module ->
                """
                // ========== ${module.name} ==========
                (function() {
                    try {
                        ${module.generateExecutableCode()}
                    } catch(__moduleError__) {
                        console.error('[WebToApp Module Error] ${module.name}:', __moduleError__);
                    }
                })();
                """.trimIndent()
            }
            
            webView.evaluateJavascript(injectionCode, null)
            android.util.Log.d("WebViewManager", "注入嵌入扩展模块代码 (${runAt.name}), 模块数: ${matchingModules.size}")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "嵌入扩展模块注入失败", e)
        }
    }
    
    /**
     * 注入下载桥接脚本
     * 用于拦截 Blob/Data URL 下载并转发给原生代码处理
     */
    private fun injectDownloadBridgeScript(webView: WebView) {
        try {
            val script = DownloadBridge.getInjectionScript()
            webView.evaluateJavascript(script, null)
            android.util.Log.d("WebViewManager", "下载桥接脚本已注入")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "下载桥接脚本注入失败", e)
        }
    }
    
    /**
     * 注入统一扩展面板脚本
     * 提供美观的统一 UI 面板，所有扩展模块的 UI 都在此面板中显示
     * 只在有扩展模块启用时才注入
     */
    private fun injectExtensionPanelScript(webView: WebView) {
        // 检查是否有扩展模块需要显示
        val hasEmbeddedModules = embeddedModules.any { it.enabled }
        val hasAppModules = appExtensionModuleIds.isNotEmpty()
        val hasGlobalModules = try {
            ExtensionManager.getInstance(context).getEnabledModules().isNotEmpty()
        } catch (e: Exception) {
            false
        }
        
        // 如果没有任何扩展模块，不注入面板脚本
        if (!hasEmbeddedModules && !hasAppModules && !hasGlobalModules) {
            android.util.Log.d("WebViewManager", "没有启用的扩展模块，跳过面板脚本注入")
            return
        }
        
        try {
            // 注入面板初始化脚本
            val panelScript = ExtensionPanelScript.getPanelInitScript()
            webView.evaluateJavascript(panelScript, null)
            
            // 注入模块辅助脚本
            val helperScript = ExtensionPanelScript.getModuleHelperScript()
            webView.evaluateJavascript(helperScript, null)
            
            android.util.Log.d("WebViewManager", "统一扩展面板脚本已注入")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "扩展面板脚本注入失败", e)
        }
    }
    
    /**
     * 注入隔离环境脚本
     * 用于防检测、指纹伪造等功能
     */
    private fun injectIsolationScript(webView: WebView) {
        try {
            val isolationManager = com.webtoapp.core.isolation.IsolationManager.getInstance(context)
            val script = isolationManager.generateIsolationScript()
            
            if (script.isNotEmpty()) {
                webView.evaluateJavascript(script, null)
                android.util.Log.d("WebViewManager", "隔离环境脚本已注入")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "隔离环境脚本注入失败", e)
        }
    }
    
    /**
     * 注入扩展模块代码
     */
    private fun injectExtensionModules(webView: WebView, url: String, runAt: ScriptRunTime) {
        try {
            val extensionManager = ExtensionManager.getInstance(context)
            val moduleRunAt = when (runAt) {
                ScriptRunTime.DOCUMENT_START -> ModuleRunTime.DOCUMENT_START
                ScriptRunTime.DOCUMENT_END -> ModuleRunTime.DOCUMENT_END
                ScriptRunTime.DOCUMENT_IDLE -> ModuleRunTime.DOCUMENT_IDLE
            }
            
            val injectionCode = extensionManager.generateInjectionCode(url, moduleRunAt)
            if (injectionCode.isNotBlank()) {
                webView.evaluateJavascript(injectionCode, null)
                android.util.Log.d("WebViewManager", "注入扩展模块代码 (${runAt.name})")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "扩展模块注入失败", e)
        }
    }
    
    /**
     * 注入指定的扩展模块代码（用于应用配置的模块）
     * @param webView WebView实例
     * @param url 当前页面URL
     * @param runAt 运行时机
     * @param moduleIds 要注入的模块ID列表
     */
    fun injectSpecificModules(webView: WebView, url: String, runAt: ScriptRunTime, moduleIds: List<String>) {
        if (moduleIds.isEmpty()) return
        
        try {
            val extensionManager = ExtensionManager.getInstance(context)
            val moduleRunAt = when (runAt) {
                ScriptRunTime.DOCUMENT_START -> ModuleRunTime.DOCUMENT_START
                ScriptRunTime.DOCUMENT_END -> ModuleRunTime.DOCUMENT_END
                ScriptRunTime.DOCUMENT_IDLE -> ModuleRunTime.DOCUMENT_IDLE
            }
            
            val injectionCode = extensionManager.generateInjectionCodeForModules(url, moduleRunAt, moduleIds)
            if (injectionCode.isNotBlank()) {
                webView.evaluateJavascript(injectionCode, null)
                android.util.Log.d("WebViewManager", "注入指定扩展模块代码 (${runAt.name}), 模块数: ${moduleIds.size}")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "指定扩展模块注入失败", e)
        }
    }
}

/**
 * WebView回调接口
 */
interface WebViewCallbacks {
    fun onPageStarted(url: String?)
    fun onPageFinished(url: String?)
    fun onProgressChanged(progress: Int)
    fun onTitleChanged(title: String?)
    fun onIconReceived(icon: Bitmap?)
    fun onError(errorCode: Int, description: String)
    fun onSslError(error: String)
    fun onExternalLink(url: String)
    fun onShowCustomView(view: android.view.View?, callback: WebChromeClient.CustomViewCallback?)
    fun onHideCustomView()
    fun onGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?)
    fun onPermissionRequest(request: PermissionRequest?)
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean
    
    /**
     * 下载请求回调
     * @param url 下载链接
     * @param userAgent User-Agent
     * @param contentDisposition Content-Disposition 头
     * @param mimeType MIME类型
     * @param contentLength 文件大小
     */
    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    )
    
    /**
     * 长按事件回调
     * @param webView WebView实例
     * @param x 长按位置X坐标
     * @param y 长按位置Y坐标
     * @return 是否消费此事件
     */
    fun onLongPress(webView: WebView, x: Float, y: Float): Boolean = false
    
    /**
     * 控制台消息回调
     * @param level 日志级别
     * @param message 消息内容
     * @param sourceId 来源文件
     * @param lineNumber 行号
     */
    fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {}
}
