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
import com.webtoapp.data.model.NewWindowBehavior
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.WebViewConfig
import java.io.ByteArrayInputStream

/**
 * WebView Manager - Configure and manage WebView
 */
class WebViewManager(
    private val context: Context,
    private val adBlocker: AdBlocker
) {
    
    companion object {
        // Desktop Chrome User-Agent
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        
        // Payment/Social App URL Scheme list
        private val PAYMENT_SCHEMES = setOf(
            "alipay", "alipays",           // Alipay
            "weixin", "wechat",             // WeChat
            "mqq", "mqqapi", "mqqwpa",      // QQ
            "taobao",                        // Taobao
            "tmall",                         // Tmall
            "jd", "openapp.jdmobile",       // JD.com
            "pinduoduo",                     // Pinduoduo
            "meituan", "imeituan",          // Meituan
            "eleme",                         // Ele.me
            "dianping",                      // Dianping
            "sinaweibo", "weibo",           // Weibo
            "bilibili",                      // Bilibili
            "douyin",                        // Douyin/TikTok
            "snssdk",                        // ByteDance
            "bytedance"                      // ByteDance
        )
    }
    
    // App configured extension module ID list
    private var appExtensionModuleIds: List<String> = emptyList()
    
    // Embedded extension module data (for Shell mode)
    private var embeddedModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList()
    
    // Track configured WebViews for resource cleanup
    private val managedWebViews = java.util.WeakHashMap<WebView, Boolean>()

    /**
     * Configure WebView
     * @param webView WebView instance
     * @param config WebView configuration
     * @param callbacks Callback interface
     * @param extensionModuleIds App configured extension module ID list (optional)
     * @param embeddedExtensionModules Embedded extension module data (for Shell mode, optional)
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(
        webView: WebView,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        extensionModuleIds: List<String> = emptyList(),
        embeddedExtensionModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList()
    ) {
        // Save config reference
        this.currentConfig = config
        // Save extension module ID list
        this.appExtensionModuleIds = extensionModuleIds
        // Save embedded module data
        this.embeddedModules = embeddedExtensionModules
        
        // Debug log：Confirm extension module config
        android.util.Log.d("WebViewManager", "configureWebView: extensionModuleIds=${extensionModuleIds.size}, embeddedModules=${embeddedExtensionModules.size}")
        embeddedExtensionModules.forEach { module ->
            android.util.Log.d("WebViewManager", "  Embedded module: id=${module.id}, name=${module.name}, enabled=${module.enabled}, runAt=${module.runAt}")
        }
        
        // Track this WebView
        managedWebViews[webView] = true
        
        // ============ Cookie 持久化配置 ============
        // Enable cookies and third-party cookies for login persistence
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }
        // Ensure cookies are persisted to disk
        cookieManager.flush()
        android.util.Log.d("WebViewManager", "Cookie persistence enabled")
        
        webView.apply {
            settings.apply {
                // JavaScript
                javaScriptEnabled = config.javaScriptEnabled
                javaScriptCanOpenWindowsAutomatically = true

                // DOM storage
                domStorageEnabled = config.domStorageEnabled
                databaseEnabled = true

                // File access
                allowFileAccess = config.allowFileAccess
                allowContentAccess = config.allowContentAccess

                // Cache
                cacheMode = if (config.cacheEnabled) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_NO_CACHE
                }

                // Zoom
                setSupportZoom(config.zoomEnabled)
                builtInZoomControls = config.zoomEnabled
                displayZoomControls = false

                // Viewport
                useWideViewPort = true
                loadWithOverviewMode = true

                // User Agent config
                // Priority: userAgentMode > desktopMode (backward compatible) > userAgent (legacy field)
                val effectiveUserAgent = resolveUserAgent(config)
                if (effectiveUserAgent != null) {
                    userAgentString = effectiveUserAgent
                    android.util.Log.d("WebViewManager", "User-Agent set: ${effectiveUserAgent.take(80)}...")
                }

                // Desktop mode viewport settings (independent of User-Agent)
                val isDesktopMode = config.userAgentMode in listOf(
                    UserAgentMode.CHROME_DESKTOP,
                    UserAgentMode.SAFARI_DESKTOP,
                    UserAgentMode.FIREFOX_DESKTOP,
                    UserAgentMode.EDGE_DESKTOP
                ) || config.desktopMode
                
                if (isDesktopMode) {
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    // Set default zoom level to fit desktop pages
                    textZoom = 100
                }

                // Mixed content - Allow HTTPS pages to load HTTP resources
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // Other settings
                mediaPlaybackRequiresUserGesture = false
                
                // Allow file:// protocol pages to load external resources (CDN, etc.)
                // Required for local frontend projects loading CDN
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            // Scrollbar
            isScrollbarFadingEnabled = true
            scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

            // Hardware acceleration
            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            
            // ============ Compatibility Enhancements ============
            
            // Initial scale (fix CSS zoom not working in WebView)
            if (config.initialScale > 0) {
                setInitialScale(config.initialScale)
                android.util.Log.d("WebViewManager", "Set initial scale: ${config.initialScale}%")
            }
            
            // Support window.open / target="_blank"
            settings.setSupportMultipleWindows(config.newWindowBehavior != NewWindowBehavior.SAME_WINDOW)

            // WebViewClient
            webViewClient = createWebViewClient(config, callbacks)

            // WebChromeClient
            webChromeClient = createWebChromeClient(config, callbacks)
            
            // Download listener
            if (config.downloadEnabled) {
                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    callbacks.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
                }
            }
            
            // Inject JavaScript bridge (navigator.share, etc.)
            if (config.enableShareBridge) {
                addJavascriptInterface(ShareBridge(context), "NativeShareBridge")
            }
            
            // ============ 键盘输入支持 ============
            // 设置焦点属性，确保不需要触屏交互也能使用键盘
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
    }
    
    /**
     * Parse User-Agent config
     * Priority: userAgentMode > desktopMode (backward compatible) > userAgent (legacy field)
     * @return Effective User-Agent string, or null if using system default
     */
    private fun resolveUserAgent(config: WebViewConfig): String? {
        // 1. Priority: use userAgentMode
        when (config.userAgentMode) {
            UserAgentMode.DEFAULT -> {
                // Continue to check other config
            }
            UserAgentMode.CUSTOM -> {
                // Custom mode: use customUserAgent
                return config.customUserAgent?.takeIf { it.isNotBlank() }
            }
            else -> {
                // Use preset User-Agent
                return config.userAgentMode.userAgentString
            }
        }
        
        // 2. Backward compatible: check desktopMode
        if (config.desktopMode) {
            return DESKTOP_USER_AGENT
        }
        
        // 3. Backward compatible: check legacy userAgent field
        return config.userAgent?.takeIf { it.isNotBlank() }
    }

    /**
     * Create WebViewClient
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
                    
                    // Debug log: record all requests
                    android.util.Log.d("WebViewManager", "shouldInterceptRequest: $url")
                    
                    // Handle local resource requests (via virtual baseURL)
                    // This is for supporting CDN loading using loadDataWithBaseURL
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
                    
                    // Only handle local asset requests, let external network requests pass through
                    if (url.startsWith("file:///android_asset/")) {
                        val assetPath = url.removePrefix("file:///android_asset/")
                        return loadEncryptedAsset(assetPath)
                    }
                    
                    // External requests: only block ads for non-local requests
                    if ((url.startsWith("http://") || url.startsWith("https://")) &&
                        !url.startsWith("https://localhost/__local__/") &&
                        adBlocker.isEnabled() && adBlocker.shouldBlock(it)) {
                        android.util.Log.d("WebViewManager", "Ad blocked: $url")
                        return adBlocker.createEmptyResponse()
                    }
                    
                    // Cross-Origin Isolation support (for SharedArrayBuffer / FFmpeg.wasm)
                    if (config.enableCrossOriginIsolation && 
                        (url.startsWith("http://") || url.startsWith("https://"))) {
                        return fetchWithCrossOriginHeaders(it)
                    }
                }
                // Return null to let system handle (including external network requests)
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // Handle special protocols
                if (handleSpecialUrl(url)) {
                    return true
                }

                // External link handling
                if (config.openExternalLinks && isExternalUrl(url, view?.url)) {
                    callbacks.onExternalLink(url)
                    return true
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                callbacks.onPageStarted(url)
                // Inject DOCUMENT_START scripts (use passed url parameter, as webView.url might still be old value)
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_START, url) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject DOCUMENT_END scripts
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_END, url) }
                callbacks.onPageFinished(url)
                // Inject DOCUMENT_IDLE scripts (delayed execution)
                view?.postDelayed({
                    injectScripts(view, config.injectScripts, ScriptRunTime.DOCUMENT_IDLE, view.url)
                }, 500)
                
                // Persist cookies to disk after page load
                // This ensures login state is saved
                CookieManager.getInstance().flush()
                
                // 确保页面加载后 WebView 仍有焦点，支持键盘输入
                view?.requestFocus()
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
                // By default reject insecure SSL connections
                handler?.cancel()
                callbacks.onSslError(error?.toString() ?: "SSL Error")
            }
        }
    }
    
    /**
     * Fetch resource with Cross-Origin Isolation headers
     * Required for SharedArrayBuffer / FFmpeg.wasm support
     * 
     * @param request Original WebResourceRequest
     * @return WebResourceResponse with COOP/COEP headers, or null on error
     */
    private fun fetchWithCrossOriginHeaders(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            
            // Copy request headers
            request.requestHeaders?.forEach { (key, value) ->
                if (key.lowercase() !in listOf("host", "connection")) {
                    connection.setRequestProperty(key, value)
                }
            }
            
            connection.requestMethod = request.method ?: "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            val mimeType = connection.contentType?.split(";")?.firstOrNull() ?: "application/octet-stream"
            val encoding = connection.contentEncoding ?: "UTF-8"
            
            // Build response headers with Cross-Origin Isolation
            val responseHeaders = mutableMapOf<String, String>()
            connection.headerFields?.forEach { (key, values) ->
                if (key != null && values.isNotEmpty()) {
                    responseHeaders[key] = values.first()
                }
            }
            
            // Add Cross-Origin Isolation headers (required for SharedArrayBuffer)
            responseHeaders["Cross-Origin-Opener-Policy"] = "same-origin"
            responseHeaders["Cross-Origin-Embedder-Policy"] = "require-corp"
            // Also add CORS headers for sub-resources
            responseHeaders["Cross-Origin-Resource-Policy"] = "cross-origin"
            
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: ByteArrayInputStream(ByteArray(0))
            }
            
            android.util.Log.d("WebViewManager", "CrossOriginIsolation fetch: $url -> $responseCode")
            
            WebResourceResponse(
                mimeType,
                encoding,
                responseCode,
                connection.responseMessage ?: "OK",
                responseHeaders,
                inputStream
            )
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "CrossOriginIsolation fetch failed: ${request.url}", e)
            null // Let system handle on error
        }
    }
    
    /**
     * Load encrypted asset resource
     * If encrypted, decrypt and return; otherwise return original
     * 
     * @param assetPath asset path (without file:///android_asset/ prefix)
     * @return WebResourceResponse or null (let system handle)
     */
    private fun loadEncryptedAsset(assetPath: String): WebResourceResponse? {
        return try {
            val secureLoader = SecureAssetLoader.getInstance(context)
            
            // Check if resource exists (encrypted or unencrypted)
            if (!secureLoader.assetExists(assetPath)) {
                android.util.Log.d("WebViewManager", "Resource not found: $assetPath")
                return null
            }
            
            // Load resource (auto-handle encrypted/unencrypted)
            val data = secureLoader.loadAsset(assetPath)
            val mimeType = getMimeType(assetPath)
            val encoding = if (isTextMimeType(mimeType)) "UTF-8" else null
            
            android.util.Log.d("WebViewManager", "Load resource: $assetPath (${data.size} bytes, $mimeType)")
            
            WebResourceResponse(
                mimeType,
                encoding,
                ByteArrayInputStream(data)
            )
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Failed to load resource: $assetPath", e)
            null
        }
    }
    
    /**
     * Get MIME type by file extension
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
     * Check if MIME type is text
     */
    private fun isTextMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("text/") ||
               mimeType == "application/javascript" ||
               mimeType == "application/json" ||
               mimeType == "application/xml" ||
               mimeType == "image/svg+xml"
    }

    /**
     * Create WebChromeClient
     */
    private fun createWebChromeClient(config: WebViewConfig, callbacks: WebViewCallbacks): WebChromeClient {
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
                android.util.Log.d("WebViewManager", "onPermissionRequest called: ${request?.resources?.joinToString()}")
                if (request != null) {
                    callbacks.onPermissionRequest(request)
                } else {
                    android.util.Log.w("WebViewManager", "onPermissionRequest: request is null!")
                }
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

            // File chooser
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return callbacks.onShowFileChooser(filePathCallback, fileChooserParams)
            }
            
            // Handle window.open / target="_blank"
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                if (view == null) return false
                
                // Try to get clicked link URL
                val href = view.hitTestResult?.extra
                
                android.util.Log.d("WebViewManager", "onCreateWindow: href=$href, behavior=${config.newWindowBehavior}")
                
                // Save reference to original WebView
                val originalWebView = view
                
                return when (config.newWindowBehavior) {
                    NewWindowBehavior.SAME_WINDOW -> {
                        // Open in current window - extract new window URL and load
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {
                            // Create temporary WebView to get URL
                            val tempWebView = WebView(context)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString()
                                    if (url != null) {
                                        // Load in original WebView
                                        originalWebView.loadUrl(url)
                                        tempView?.destroy()
                                    }
                                    return true
                                }
                            }
                            transport.webView = tempWebView
                            resultMsg.sendToTarget()
                        }
                        true
                    }
                    NewWindowBehavior.EXTERNAL_BROWSER -> {
                        // Open in external browser
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {
                            val tempWebView = WebView(context)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString()
                                    if (url != null) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.util.Log.e("WebViewManager", "Cannot open external browser: $url", e)
                                        }
                                        tempView?.destroy()
                                    }
                                    return true
                                }
                            }
                            transport.webView = tempWebView
                            resultMsg.sendToTarget()
                        }
                        true
                    }
                    NewWindowBehavior.POPUP_WINDOW -> {
                        // Popup new window - call callback to let app handle
                        callbacks.onNewWindow(resultMsg)
                        true
                    }
                    NewWindowBehavior.BLOCK -> {
                        // Block opening
                        false
                    }
                }
            }
            
            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                android.util.Log.d("WebViewManager", "onCloseWindow")
            }
        }
    }

    /**
     * Handle special URLs (tel, mailto, sms, third-party apps, etc.)
     */
    private fun handleSpecialUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false
        
        // http/https handled by WebView
        if (scheme == "http" || scheme == "https") {
            return false
        }
        
        android.util.Log.d("WebViewManager", "Handling special URL: $url (scheme=$scheme)")
        
        return try {
            val intent = when (scheme) {
                "intent" -> {
                    // intent:// URLs need Intent.parseUri to parse
                    // Common format used by download managers like 1DM, ADM
                    try {
                        android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            // Add BROWSABLE category for security and compatibility
                            addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                            // Also add to selector if present
                            selector?.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                        }
                    } catch (e: java.net.URISyntaxException) {
                        android.util.Log.e("WebViewManager", "Invalid intent URI: $url", e)
                        null
                    }
                }
                else -> {
                    // Other protocols (tel:, mailto:, sms:, etc.) use ACTION_VIEW
                    android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            
            if (intent != null) {
                // Get fallback URL first (for intent:// scheme)
                val fallbackUrl = if (scheme == "intent") {
                    intent.getStringExtra("browser_fallback_url")
                } else null
                
                // Try to launch the intent
                // On Android 11+, resolveActivity may return null due to package visibility
                // So we try to launch directly and catch ActivityNotFoundException
                try {
                    // First check if we can resolve it (works for declared packages in queries)
                    val resolveInfo = context.packageManager.resolveActivity(
                        intent, 
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )
                    
                    if (resolveInfo != null) {
                        android.util.Log.d("WebViewManager", "Resolved activity: ${resolveInfo.activityInfo?.packageName}")
                        context.startActivity(intent)
                        return true
                    }
                    
                    // If resolveActivity returns null, still try to launch
                    // This handles cases where the target app isn't in queries but can still be launched
                    android.util.Log.d("WebViewManager", "resolveActivity returned null, trying direct launch")
                    context.startActivity(intent)
                    return true
                    
                } catch (e: android.content.ActivityNotFoundException) {
                    android.util.Log.w("WebViewManager", "No activity found for intent", e)
                    // Use fallback URL if available
                    if (!fallbackUrl.isNullOrEmpty()) {
                        android.util.Log.d("WebViewManager", "Using fallback URL: $fallbackUrl")
                        // Load fallback URL in WebView
                        managedWebViews.keys.firstOrNull()?.loadUrl(fallbackUrl)
                        return true
                    }
                    // No fallback, return true to prevent ERR_UNKNOWN_URL_SCHEME
                    return true
                } catch (e: SecurityException) {
                    android.util.Log.e("WebViewManager", "Security exception launching intent", e)
                    // Use fallback URL if available
                    if (!fallbackUrl.isNullOrEmpty()) {
                        android.util.Log.d("WebViewManager", "Using fallback URL after security error: $fallbackUrl")
                        managedWebViews.keys.firstOrNull()?.loadUrl(fallbackUrl)
                        return true
                    }
                    return true
                }
            }
            true
        } catch (e: Exception) {
            // No app can handle this protocol, fail silently
            android.util.Log.w("WebViewManager", "Error handling special URL: $scheme", e)
            true // Return true to prevent WebView loading, avoid ERR_UNKNOWN_URL_SCHEME
        }
    }

    /**
     * Check if URL is external link
     */
    private fun isExternalUrl(targetUrl: String, currentUrl: String?): Boolean {
        if (currentUrl == null) return false
        val targetHost = Uri.parse(targetUrl).host ?: return false
        val currentHost = Uri.parse(currentUrl).host ?: return false
        return !targetHost.endsWith(currentHost) && !currentHost.endsWith(targetHost)
    }
    
    /**
     * Clean up WebView resources to prevent memory leak
     * Should be called when Activity/Fragment is destroyed
     */
    fun destroyWebView(webView: WebView) {
        try {
            managedWebViews.remove(webView)
            
            webView.apply {
                // Stop loading
                stopLoading()
                
                // Clear history and cache
                clearHistory()
                
                // Remove all callbacks
                webChromeClient = null
                webViewClient = object : WebViewClient() {}
                
                // Clear JavaScript interfaces
                removeJavascriptInterface("NativeBridge")
                removeJavascriptInterface("DownloadBridge")
                removeJavascriptInterface("NativeShareBridge")
                
                // Load blank page to release resources
                loadUrl("about:blank")
                
                // Remove from parent view
                (parent as? android.view.ViewGroup)?.removeView(this)
                
                // Destroy WebView
                destroy()
            }
            
            android.util.Log.d("WebViewManager", "WebView resources cleaned up")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Failed to cleanup WebView", e)
        }
    }
    
    /**
     * Clean up all managed WebViews
     */
    fun destroyAll() {
        managedWebViews.keys.toList().forEach { webView ->
            destroyWebView(webView)
        }
        managedWebViews.clear()
    }
    // Save config reference (for script injection)
    private var currentConfig: WebViewConfig? = null
    
    /**
     * Inject user scripts
     * @param webView WebView instance
     * @param scripts User script list
     * @param runAt Run timing
     * @param pageUrl Current page URL (optional, gets from webView if not provided)
     */
    private fun injectScripts(webView: WebView, scripts: List<com.webtoapp.data.model.UserScript>, runAt: ScriptRunTime, pageUrl: String? = null) {
        // Inject download bridge script at DOCUMENT_START (ensure earliest injection)
        if (runAt == ScriptRunTime.DOCUMENT_START) {
            injectDownloadBridgeScript(webView)
            // Inject unified extension panel script
            injectExtensionPanelScript(webView)
            // Inject isolation environment script (earliest injection to ensure fingerprint spoofing works)
            injectIsolationScript(webView)
            // Inject browser compatibility scripts
            injectCompatibilityScripts(webView)
        }
        
        // Inject user custom scripts
        scripts.filter { it.enabled && it.runAt == runAt && it.code.isNotBlank() }
            .forEach { script ->
                try {
                    // Wrap script, add error handling
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
                    android.util.Log.d("WebViewManager", "Inject script: ${script.name} (${runAt.name})")
                } catch (e: Exception) {
                    android.util.Log.e("WebViewManager", "Script injection failed: ${script.name}", e)
                }
            }
        
        // Inject extension module code
        // Prioritize passed pageUrl, because webView.url might still be old value at onPageStarted
        val url = pageUrl ?: webView.url ?: ""
        
        // Debug log
        android.util.Log.d("WebViewManager", "injectScripts: runAt=${runAt.name}, url=$url, embeddedModules=${embeddedModules.size}, appExtensionModuleIds=${appExtensionModuleIds.size}")
        
        // Prioritize embedded module data (Shell mode)
        if (embeddedModules.isNotEmpty()) {
            injectEmbeddedModules(webView, url, runAt)
        } else if (appExtensionModuleIds.isNotEmpty()) {
            // Use app configured extension modules
            injectSpecificModules(webView, url, runAt, appExtensionModuleIds)
        } else {
            // Use globally enabled extension modules
            injectExtensionModules(webView, url, runAt)
        }
    }
    
    /**
     * Inject embedded extension module code (Shell mode only)
     * Each module runs independently, one error does not affect others
     */
    private fun injectEmbeddedModules(webView: WebView, url: String, runAt: ScriptRunTime) {
        try {
            val targetRunAt = runAt.name
            
            // Debug log: show state before filtering
            android.util.Log.d("WebViewManager", "injectEmbeddedModules: url=$url, runAt=$targetRunAt, totalModules=${embeddedModules.size}")
            
            val matchingModules = embeddedModules.filter { module ->
                val enabledMatch = module.enabled
                val runAtMatch = module.runAt == targetRunAt
                val urlMatch = module.matchesUrl(url)
                
                // Debug log: show each module's match status
                android.util.Log.d("WebViewManager", "  Module[${module.name}]: enabled=$enabledMatch, runAt=${module.runAt}==$targetRunAt?$runAtMatch, urlMatch=$urlMatch")
                
                enabledMatch && runAtMatch && urlMatch
            }
            
            if (matchingModules.isEmpty()) {
                android.util.Log.d("WebViewManager", "injectEmbeddedModules: No matching modules")
                return
            }
            
            // Each module wrapped independently, error isolation
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
            android.util.Log.d("WebViewManager", "Inject embedded extension module code (${runAt.name}), module count: ${matchingModules.size}")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Embedded extension module injection failed", e)
        }
    }
    
    /**
     * Inject download bridge script
     * Intercept Blob/Data URL downloads and forward to native code
     */
    private fun injectDownloadBridgeScript(webView: WebView) {
        try {
            val script = DownloadBridge.getInjectionScript()
            webView.evaluateJavascript(script, null)
            android.util.Log.d("WebViewManager", "Download bridge script injected")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Download bridge script injection failed", e)
        }
    }
    
    /**
     * Inject unified extension panel script
     * Provide unified UI panel, all extension module UI displayed in this panel
     * Only inject when extension modules are enabled
     */
    private fun injectExtensionPanelScript(webView: WebView) {
        // Check if any extension modules need to display
        val hasEmbeddedModules = embeddedModules.any { it.enabled }
        val hasAppModules = appExtensionModuleIds.isNotEmpty()
        val hasGlobalModules = try {
            ExtensionManager.getInstance(context).getEnabledModules().isNotEmpty()
        } catch (e: Exception) {
            false
        }
        
        // 如果没有任何扩展模块，不注入面板脚本
        if (!hasEmbeddedModules && !hasAppModules && !hasGlobalModules) {
            android.util.Log.d("WebViewManager", "No enabled extension modules, skip panel script injection")
            return
        }
        
        try {
            // Inject面板初始化脚本
            val panelScript = ExtensionPanelScript.getPanelInitScript()
            webView.evaluateJavascript(panelScript, null)
            
            // Inject模块辅助脚本
            val helperScript = ExtensionPanelScript.getModuleHelperScript()
            webView.evaluateJavascript(helperScript, null)
            
            android.util.Log.d("WebViewManager", "Extension panel script injected")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Extension panel script injection failed", e)
        }
    }
    
    /**
     * Inject isolation environment script
     * For anti-detection, fingerprint spoofing, etc.
     */
    private fun injectIsolationScript(webView: WebView) {
        try {
            val isolationManager = com.webtoapp.core.isolation.IsolationManager.getInstance(context)
            val script = isolationManager.generateIsolationScript()
            
            if (script.isNotEmpty()) {
                webView.evaluateJavascript(script, null)
                android.util.Log.d("WebViewManager", "Isolation script injected")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Isolation script injection failed", e)
        }
    }
    
    /**
     * Inject browser compatibility scripts
     * Fix differences between Android WebView and browsers
     */
    private fun injectCompatibilityScripts(webView: WebView) {
        val config = currentConfig ?: return
        
        try {
            val scripts = mutableListOf<String>()
            
            // 1. CSS zoom polyfill - convert zoom to transform: scale()
            if (config.enableZoomPolyfill) {
                scripts.add("""
                    // CSS zoom polyfill for Android WebView
                    (function() {
                        'use strict';
                        
                        // 标记 polyfill 已加载
                        if (window.__webtoapp_zoom_polyfill__) return;
                        window.__webtoapp_zoom_polyfill__ = true;
                        
                        // 存储元素原始宽度
                        var originalWidths = new WeakMap();
                        
                        function convertZoomToTransform(el) {
                            if (!el || !el.style) return;
                            
                            var zoom = el.style.zoom;
                            if (zoom && zoom !== '1' && zoom !== 'normal' && zoom !== 'initial' && zoom !== '') {
                                var scale = parseFloat(zoom);
                                if (zoom.indexOf('%') !== -1) {
                                    scale = parseFloat(zoom) / 100;
                                }
                                if (!isNaN(scale) && scale > 0 && scale !== 1) {
                                    // 保存原始宽度
                                    if (!originalWidths.has(el)) {
                                        originalWidths.set(el, el.style.width || '');
                                    }
                                    // 清除 zoom 并应用 transform
                                    el.style.zoom = '';
                                    el.style.transform = 'scale(' + scale + ')';
                                    el.style.transformOrigin = 'top left';
                                    // 缩小时需要扩展宽度以避免内容被裁切
                                    if (scale < 1) {
                                        el.style.width = (100 / scale) + '%';
                                    }
                                    console.log('[WebToApp] Converted zoom to transform:', scale, 'for element:', el.tagName);
                                }
                            }
                        }
                        
                        // MutationObserver 监听 style 属性变化
                        var observer = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                                    convertZoomToTransform(mutation.target);
                                }
                                if (mutation.addedNodes) {
                                    mutation.addedNodes.forEach(function(node) {
                                        if (node.nodeType === 1) {
                                            convertZoomToTransform(node);
                                            // 也检查子元素
                                            if (node.querySelectorAll) {
                                                node.querySelectorAll('*').forEach(function(child) {
                                                    convertZoomToTransform(child);
                                                });
                                            }
                                        }
                                    });
                                }
                            });
                        });
                        
                        // 设置 observer 的函数
                        function setupObserver() {
                            if (document.documentElement) {
                                observer.observe(document.documentElement, {
                                    attributes: true,
                                    childList: true,
                                    subtree: true,
                                    attributeFilter: ['style']
                                });
                                // 初始扫描
                                if (document.body) {
                                    convertZoomToTransform(document.body);
                                    document.body.querySelectorAll('*').forEach(function(el) {
                                        convertZoomToTransform(el);
                                    });
                                }
                                console.log('[WebToApp] CSS zoom observer started');
                            }
                        }
                        
                        // DOM 就绪后设置 observer
                        if (document.readyState === 'loading') {
                            document.addEventListener('DOMContentLoaded', setupObserver);
                        } else {
                            setupObserver();
                        }
                        
                        // Override CSSStyleDeclaration.zoom setter（最关键的拦截）
                        try {
                            var zoomDescriptor = Object.getOwnPropertyDescriptor(CSSStyleDeclaration.prototype, 'zoom');
                            Object.defineProperty(CSSStyleDeclaration.prototype, 'zoom', {
                                set: function(value) {
                                    console.log('[WebToApp] zoom setter called with:', value);
                                    if (value && value !== '1' && value !== 'normal' && value !== 'initial' && value !== '') {
                                        var scale = parseFloat(value);
                                        if (String(value).indexOf('%') !== -1) {
                                            scale = parseFloat(value) / 100;
                                        }
                                        if (!isNaN(scale) && scale > 0 && scale !== 1) {
                                            this.transform = 'scale(' + scale + ')';
                                            this.transformOrigin = 'top left';
                                            if (scale < 1) {
                                                this.width = (100 / scale) + '%';
                                            }
                                            console.log('[WebToApp] Intercepted zoom set, converted to transform:', scale);
                                            return;
                                        }
                                    }
                                    // 重置为默认
                                    if (value === '' || value === '1' || value === 'normal' || value === 'initial') {
                                        this.transform = '';
                                        this.transformOrigin = '';
                                    }
                                    if (zoomDescriptor && zoomDescriptor.set) {
                                        zoomDescriptor.set.call(this, value);
                                    }
                                },
                                get: function() {
                                    // 返回基于 transform 计算的 zoom 值
                                    var transform = this.transform;
                                    if (transform && transform.indexOf('scale(') !== -1) {
                                        var match = transform.match(/scale\(([\d.]+)\)/);
                                        if (match) {
                                            return match[1];
                                        }
                                    }
                                    if (zoomDescriptor && zoomDescriptor.get) {
                                        return zoomDescriptor.get.call(this);
                                    }
                                    return '1';
                                },
                                configurable: true
                            });
                            console.log('[WebToApp] zoom setter override installed');
                        } catch(e) {
                            console.warn('[WebToApp] Failed to override zoom setter:', e);
                        }
                        
                        console.log('[WebToApp] CSS zoom polyfill loaded');
                    })();
                """.trimIndent())
            }
            
            // 2. navigator.share polyfill
            if (config.enableShareBridge) {
                scripts.add("""
                    // navigator.share polyfill for Android WebView
                    (function() {
                        'use strict';
                        
                        if (typeof NativeShareBridge !== 'undefined') {
                            // Implement navigator.share
                            navigator.share = function(data) {
                                return new Promise(function(resolve, reject) {
                                    try {
                                        var title = data.title || '';
                                        var text = data.text || '';
                                        var url = data.url || '';
                                        NativeShareBridge.shareText(title, text, url);
                                        resolve();
                                    } catch(e) {
                                        reject(e);
                                    }
                                });
                            };
                            
                            // Implement navigator.canShare
                            navigator.canShare = function(data) {
                                // Basic support for text and url
                                if (!data) return false;
                                if (data.files) return false; // File sharing not yet supported
                                return true;
                            };
                            
                            console.log('[WebToApp] navigator.share polyfill loaded');
                        }
                    })();
                """.trimIndent())
            }
            
            // 3. Hide link URL preview (tooltip)
            // This removes the small URL preview popup when hovering/long-pressing links
            scripts.add("""
                // Hide link URL preview for privacy
                (function() {
                    'use strict';
                    
                    // Inject CSS to disable touch callout and user select on links
                    var style = document.createElement('style');
                    style.id = 'webtoapp-hide-url-preview';
                    style.textContent = `
                        a {
                            -webkit-touch-callout: none !important;
                            -webkit-user-select: none !important;
                        }
                    `;
                    (document.head || document.documentElement).appendChild(style);
                    
                    // Remove title attribute from all links to hide URL tooltips
                    function removeAllTitles() {
                        document.querySelectorAll('a[title]').forEach(function(link) {
                            link.removeAttribute('title');
                        });
                    }
                    
                    // Initial removal
                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', removeAllTitles);
                    } else {
                        removeAllTitles();
                    }
                    
                    // Watch for dynamically added links
                    var titleObserver = new MutationObserver(function(mutations) {
                        mutations.forEach(function(mutation) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeType === 1) {
                                    if (node.tagName === 'A' && node.hasAttribute('title')) {
                                        node.removeAttribute('title');
                                    }
                                    node.querySelectorAll && node.querySelectorAll('a[title]').forEach(function(link) {
                                        link.removeAttribute('title');
                                    });
                                }
                            });
                        });
                    });
                    
                    if (document.body) {
                        titleObserver.observe(document.body, { childList: true, subtree: true });
                    } else {
                        document.addEventListener('DOMContentLoaded', function() {
                            titleObserver.observe(document.body, { childList: true, subtree: true });
                        });
                    }
                    
                    // Intercept setAttribute to prevent title from being set on links
                    var originalSetAttribute = Element.prototype.setAttribute;
                    Element.prototype.setAttribute = function(name, value) {
                        if (this.tagName === 'A' && name.toLowerCase() === 'title') {
                            return; // Block setting title on links
                        }
                        return originalSetAttribute.call(this, name, value);
                    };
                    
                    console.log('[WebToApp] Link URL preview hidden');
                })();
            """.trimIndent())
            
            // 4. Popup Blocker
            if (config.popupBlockerEnabled) {
                scripts.add("""
                    // Popup Blocker - blocks unwanted popups and redirects
                    (function() {
                        'use strict';
                        
                        // Track if popup blocker is enabled (can be toggled at runtime)
                        window.__webtoapp_popup_blocker_enabled__ = true;
                        
                        var blockedCount = 0;
                        var allowedDomains = []; // Can be configured later
                        
                        // Store original functions
                        var originalOpen = window.open;
                        var originalAlert = window.alert;
                        var originalConfirm = window.confirm;
                        
                        // Helper to check if URL is suspicious
                        function isSuspiciousUrl(url) {
                            if (!url) return true;
                            var lowerUrl = url.toLowerCase();
                            // Common ad/popup patterns
                            var suspiciousPatterns = [
                                'doubleclick', 'googlesyndication', 'googleadservices',
                                'facebook.com/tr', 'analytics', 'tracker',
                                'popup', 'popunder', 'clickunder',
                                'adserver', 'adservice', 'adsense',
                                'javascript:void', 'about:blank',
                                'data:text/html'
                            ];
                            return suspiciousPatterns.some(function(pattern) {
                                return lowerUrl.indexOf(pattern) !== -1;
                            });
                        }
                        
                        // Helper to check if domain is allowed
                        function isDomainAllowed(url) {
                            if (!url || allowedDomains.length === 0) return false;
                            try {
                                var urlObj = new URL(url, window.location.href);
                                return allowedDomains.some(function(domain) {
                                    return urlObj.hostname.indexOf(domain) !== -1;
                                });
                            } catch(e) {
                                return false;
                            }
                        }
                        
                        // Override window.open
                        window.open = function(url, target, features) {
                            if (!window.__webtoapp_popup_blocker_enabled__) {
                                return originalOpen.apply(window, arguments);
                            }
                            
                            // Allow same-origin and allowed domains
                            var isSameOrigin = false;
                            try {
                                if (url) {
                                    var urlObj = new URL(url, window.location.href);
                                    isSameOrigin = urlObj.origin === window.location.origin;
                                }
                            } catch(e) {}
                            
                            // Block conditions
                            var shouldBlock = false;
                            
                            // Block about:blank and javascript: URLs (common popup tricks)
                            if (!url || url === 'about:blank' || url.indexOf('javascript:') === 0) {
                                shouldBlock = true;
                            }
                            // Block suspicious URLs
                            else if (isSuspiciousUrl(url) && !isSameOrigin && !isDomainAllowed(url)) {
                                shouldBlock = true;
                            }
                            
                            if (shouldBlock) {
                                blockedCount++;
                                console.log('[WebToApp PopupBlocker] Blocked popup #' + blockedCount + ':', url || '(empty)');
                                // Return fake window object to prevent errors
                                return {
                                    closed: true,
                                    close: function() {},
                                    focus: function() {},
                                    blur: function() {},
                                    postMessage: function() {},
                                    location: { href: '' },
                                    document: { write: function() {}, close: function() {} }
                                };
                            }
                            
                            // Allow legitimate popups
                            var result = originalOpen.apply(window, arguments);
                            if (!result) {
                                return {
                                    closed: false,
                                    close: function() {},
                                    focus: function() {},
                                    blur: function() {},
                                    postMessage: function() {},
                                    location: { href: url || '' }
                                };
                            }
                            return result;
                        };
                        
                        // Block popup triggers via setTimeout/setInterval with very short delays
                        var originalSetTimeout = window.setTimeout;
                        var originalSetInterval = window.setInterval;
                        
                        window.setTimeout = function(fn, delay) {
                            // Block immediate timeouts that might be popup triggers
                            if (delay === 0 && typeof fn === 'string' && fn.indexOf('open(') !== -1) {
                                console.log('[WebToApp PopupBlocker] Blocked setTimeout popup trigger');
                                return 0;
                            }
                            return originalSetTimeout.apply(window, arguments);
                        };
                        
                        // Prevent popunders via document.write
                        var originalDocWrite = document.write;
                        var originalDocWriteln = document.writeln;
                        
                        function blockSuspiciousWrite(content) {
                            if (!window.__webtoapp_popup_blocker_enabled__) return false;
                            if (typeof content !== 'string') return false;
                            var lowerContent = content.toLowerCase();
                            // Block writes that create popups or redirects
                            if (lowerContent.indexOf('window.open') !== -1 ||
                                lowerContent.indexOf('location.href') !== -1 ||
                                lowerContent.indexOf('location.replace') !== -1) {
                                console.log('[WebToApp PopupBlocker] Blocked suspicious document.write');
                                return true;
                            }
                            return false;
                        }
                        
                        document.write = function() {
                            if (arguments.length > 0 && blockSuspiciousWrite(arguments[0])) {
                                return;
                            }
                            return originalDocWrite.apply(document, arguments);
                        };
                        
                        document.writeln = function() {
                            if (arguments.length > 0 && blockSuspiciousWrite(arguments[0])) {
                                return;
                            }
                            return originalDocWriteln.apply(document, arguments);
                        };
                        
                        // Expose toggle function
                        window.__webtoapp_toggle_popup_blocker__ = function(enabled) {
                            window.__webtoapp_popup_blocker_enabled__ = enabled;
                            console.log('[WebToApp PopupBlocker] ' + (enabled ? 'Enabled' : 'Disabled'));
                        };
                        
                        // Expose stats
                        window.__webtoapp_popup_blocker_stats__ = function() {
                            return { blocked: blockedCount, enabled: window.__webtoapp_popup_blocker_enabled__ };
                        };
                        
                        console.log('[WebToApp] Popup blocker loaded');
                    })();
                """.trimIndent())
            }
            
            // 5. Other compatibility fixes
            scripts.add("""
                // Compatibility fixes
                (function() {
                    'use strict';
                    
                    // Fix window.open return value (fallback for when popup blocker is disabled)
                    if (!window.__webtoapp_popup_blocker_enabled__) {
                        var originalOpen = window.open;
                        window.open = function(url, target, features) {
                            var result = originalOpen.call(window, url, target, features);
                            // If returns null, return proxy object to prevent script errors
                            if (!result) {
                                return {
                                    closed: false,
                                    close: function() {},
                                    focus: function() {},
                                    blur: function() {},
                                    postMessage: function() {},
                                    location: { href: url || '' }
                                };
                            }
                            return result;
                        };
                    }
                    
                    // Fix requestIdleCallback (some WebViews don't support)
                    if (!window.requestIdleCallback) {
                        window.requestIdleCallback = function(callback, options) {
                            var timeout = (options && options.timeout) || 1;
                            var start = Date.now();
                            return setTimeout(function() {
                                callback({
                                    didTimeout: false,
                                    timeRemaining: function() {
                                        return Math.max(0, 50 - (Date.now() - start));
                                    }
                                });
                            }, timeout);
                        };
                        window.cancelIdleCallback = function(id) {
                            clearTimeout(id);
                        };
                    }
                    
                    // Fix ResizeObserver (some old WebViews don't support)
                    if (!window.ResizeObserver) {
                        window.ResizeObserver = function(callback) {
                            this.callback = callback;
                            this.elements = [];
                        };
                        window.ResizeObserver.prototype.observe = function(el) {
                            this.elements.push(el);
                        };
                        window.ResizeObserver.prototype.unobserve = function(el) {
                            this.elements = this.elements.filter(function(e) { return e !== el; });
                        };
                        window.ResizeObserver.prototype.disconnect = function() {
                            this.elements = [];
                        };
                    }
                    
                    console.log('[WebToApp] Compatibility fixes loaded');
                })();
            """.trimIndent())
            
            // Execute all compatibility scripts
            val combinedScript = scripts.joinToString("\n\n")
            webView.evaluateJavascript(combinedScript, null)
            android.util.Log.d("WebViewManager", "Browser compatibility scripts injected")
            
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Compatibility script injection failed", e)
        }
    }
    
    /**
     * Inject extension module code
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
                android.util.Log.d("WebViewManager", "Inject extension module code (${runAt.name})")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Extension module injection failed", e)
        }
    }
    
    /**
     * Inject specified extension module code (for app configured modules)
     * @param webView WebView instance
     * @param url Current page URL
     * @param runAt Run timing
     * @param moduleIds Module ID list to inject
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
                android.util.Log.d("WebViewManager", "Inject specified extension module code (${runAt.name}), module count: ${moduleIds.size}")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "Specified extension module injection failed", e)
        }
    }
}

/**
 * WebView callback interface
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
     * Download request callback
     * @param url Download URL
     * @param userAgent User-Agent
     * @param contentDisposition Content-Disposition header
     * @param mimeType MIME type
     * @param contentLength File size
     */
    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    )
    
    /**
     * Long press event callback
     * @param webView WebView instance
     * @param x Long press X coordinate
     * @param y Long press Y coordinate
     * @return Whether this event is consumed
     */
    fun onLongPress(webView: WebView, x: Float, y: Float): Boolean = false
    
    /**
     * Console message callback
     * @param level Log level
     * @param message Message content
     * @param sourceId Source file
     * @param lineNumber Line number
     */
    fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {}
    
    /**
     * New window request callback (window.open / target="_blank")
     * @param resultMsg Message for passing to new window
     */
    fun onNewWindow(resultMsg: android.os.Message?) {}
}

/**
 * navigator.share bridge class
 * Provide system share functionality
 */
class ShareBridge(private val context: Context) {
    
    /**
     * Share text
     */
    @JavascriptInterface
    fun shareText(title: String?, text: String?, url: String?) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                if (!title.isNullOrEmpty()) {
                    putExtra(android.content.Intent.EXTRA_SUBJECT, title)
                }
                val shareText = buildString {
                    if (!text.isNullOrEmpty()) append(text)
                    if (!url.isNullOrEmpty()) {
                        if (isNotEmpty()) append("\n")
                        append(url)
                    }
                }
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            val chooser = android.content.Intent.createChooser(intent, title ?: "Share")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("ShareBridge", "Share failed", e)
        }
    }
    
    /**
     * Check if sharing is supported
     */
    @JavascriptInterface
    fun canShare(): Boolean = true
}
