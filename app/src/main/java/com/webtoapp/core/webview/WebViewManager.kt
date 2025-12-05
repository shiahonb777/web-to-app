package com.webtoapp.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.*
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.UserScript
import com.webtoapp.data.model.WebViewConfig

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

    /**
     * 配置WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(
        webView: WebView,
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ) {
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

                // 混合内容
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // 其他设置
                mediaPlaybackRequiresUserGesture = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

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
                    // 广告拦截
                    if (adBlocker.isEnabled() && adBlocker.shouldBlock(it)) {
                        return adBlocker.createEmptyResponse()
                    }
                }
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
                // 注入 DOCUMENT_START 脚本
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_START) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 注入 DOCUMENT_END 脚本
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_END) }
                callbacks.onPageFinished(url)
                // 注入 DOCUMENT_IDLE 脚本（延迟执行）
                view?.postDelayed({
                    injectScripts(view, config.injectScripts, ScriptRunTime.DOCUMENT_IDLE)
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
                // 可用于调试
                return super.onConsoleMessage(consoleMessage)
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
     * 注入用户脚本
     */
    private fun injectScripts(webView: WebView, scripts: List<UserScript>, runAt: ScriptRunTime) {
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
}
