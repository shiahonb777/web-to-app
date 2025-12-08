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
                // 注入自动翻译脚本
                if (config.autoTranslate) {
                    view?.let { injectAutoTranslateScript(it) }
                }
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
     * 注入自动翻译脚本 - 多源翻译 API + 伪装 Header 突破限速
     */
    private fun injectAutoTranslateScript(webView: WebView) {
        val translateScript = """
            (function() {
                if (window._autoTranslateInjected) return;
                window._autoTranslateInjected = true;
                
                // 翻译缓存（避免重复翻译）
                var translateCache = {};
                
                // 随机 User-Agent 池
                var userAgents = [
                    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
                    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Safari/605.1.15',
                    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0',
                    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/119.0.0.0 Safari/537.36',
                    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148'
                ];
                
                // 随机邮箱池（MyMemory 使用邮箱标识用户，伪装不同用户）
                var emails = [
                    'user' + Math.floor(Math.random()*9999) + '@gmail.com',
                    'test' + Math.floor(Math.random()*9999) + '@outlook.com',
                    'app' + Math.floor(Math.random()*9999) + '@yahoo.com',
                    'dev' + Math.floor(Math.random()*9999) + '@qq.com',
                    'web' + Math.floor(Math.random()*9999) + '@163.com'
                ];
                
                function randomItem(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
                function randomDelay(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }
                
                // 翻译 API 源列表（多源轮询 + 自动故障转移）
                var apiSources = [
                    {
                        name: 'MyMemory',
                        translate: function(text, callback) {
                            var email = randomItem(emails);
                            var url = 'https://api.mymemory.translated.net/get?q=' + 
                                encodeURIComponent(text) + '&langpair=en|zh-CN&de=' + email;
                            fetchWithHeaders(url, function(data) {
                                if (data && data.responseStatus === 200 && data.responseData) {
                                    callback(data.responseData.translatedText);
                                } else { callback(null); }
                            });
                        }
                    },
                    {
                        name: 'LibreTranslate',
                        translate: function(text, callback) {
                            // 公共 LibreTranslate 实例
                            var mirrors = [
                                'https://libretranslate.de/translate',
                                'https://translate.argosopentech.com/translate',
                                'https://translate.terraprint.co/translate'
                            ];
                            var url = randomItem(mirrors);
                            fetchPost(url, {
                                q: text,
                                source: 'en',
                                target: 'zh',
                                format: 'text'
                            }, function(data) {
                                if (data && data.translatedText) {
                                    callback(data.translatedText);
                                } else { callback(null); }
                            });
                        }
                    },
                    {
                        name: 'Lingva',
                        translate: function(text, callback) {
                            // Lingva 是 Google 翻译的开源前端代理
                            var mirrors = [
                                'https://lingva.ml/api/v1/en/zh/',
                                'https://lingva.pussthecat.org/api/v1/en/zh/',
                                'https://translate.plausibility.cloud/api/v1/en/zh/'
                            ];
                            var url = randomItem(mirrors) + encodeURIComponent(text);
                            fetchWithHeaders(url, function(data) {
                                if (data && data.translation) {
                                    callback(data.translation);
                                } else { callback(null); }
                            });
                        }
                    }
                ];
                
                var currentSourceIndex = 0;
                var sourceFailCount = {};
                
                // 带伪装 Header 的 GET 请求
                function fetchWithHeaders(url, callback) {
                    fetch(url, {
                        method: 'GET',
                        headers: {
                            'Accept': 'application/json, text/plain, */*',
                            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
                            'Cache-Control': 'no-cache',
                            'Pragma': 'no-cache'
                        },
                        mode: 'cors',
                        cache: 'no-store'
                    })
                    .then(function(r) { return r.json(); })
                    .then(callback)
                    .catch(function() { callback(null); });
                }
                
                // POST 请求
                function fetchPost(url, body, callback) {
                    fetch(url, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify(body),
                        mode: 'cors'
                    })
                    .then(function(r) { return r.json(); })
                    .then(callback)
                    .catch(function() { callback(null); });
                }
                
                // 智能翻译（自动切换源）
                function smartTranslate(text, callback, retryCount) {
                    retryCount = retryCount || 0;
                    if (retryCount >= apiSources.length) {
                        callback(text); // 所有源都失败，返回原文
                        return;
                    }
                    
                    // 检查缓存
                    if (translateCache[text]) {
                        callback(translateCache[text]);
                        return;
                    }
                    
                    var source = apiSources[currentSourceIndex];
                    source.translate(text, function(result) {
                        if (result && result !== text) {
                            translateCache[text] = result;
                            sourceFailCount[source.name] = 0;
                            callback(result);
                        } else {
                            // 当前源失败，切换到下一个
                            sourceFailCount[source.name] = (sourceFailCount[source.name] || 0) + 1;
                            if (sourceFailCount[source.name] >= 3) {
                                currentSourceIndex = (currentSourceIndex + 1) % apiSources.length;
                                console.log('[AutoTranslate] 切换到源: ' + apiSources[currentSourceIndex].name);
                            }
                            smartTranslate(text, callback, retryCount + 1);
                        }
                    });
                }
                
                // 收集待翻译文本
                function collectTextNodes() {
                    var nodes = [];
                    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
                        acceptNode: function(node) {
                            var text = node.textContent.trim();
                            if (!text || text.length < 2) return NodeFilter.FILTER_REJECT;
                            var parent = node.parentElement;
                            if (!parent) return NodeFilter.FILTER_REJECT;
                            var tag = parent.tagName;
                            if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT' || tag === 'CODE' || tag === 'PRE') {
                                return NodeFilter.FILTER_REJECT;
                            }
                            // 检测是否包含英文（需要翻译）
                            if (/[a-zA-Z]{2,}/.test(text) && !/^[\u4e00-\u9fff\s，。！？、：；""''（）]+$/.test(text)) {
                                return NodeFilter.FILTER_ACCEPT;
                            }
                            return NodeFilter.FILTER_REJECT;
                        }
                    });
                    while (walker.nextNode()) nodes.push(walker.currentNode);
                    return nodes;
                }
                
                // 节流队列翻译
                function translateWithThrottle(nodes) {
                    if (nodes.length === 0) {
                        hideHint();
                        return;
                    }
                    
                    var total = nodes.length;
                    var completed = 0;
                    var concurrency = 3; // 并发数
                    var queue = nodes.slice();
                    var active = 0;
                    
                    function processNext() {
                        if (queue.length === 0 && active === 0) {
                            hideHint();
                            console.log('[AutoTranslate] 完成，共翻译 ' + completed + ' 个节点');
                            return;
                        }
                        
                        while (active < concurrency && queue.length > 0) {
                            active++;
                            var node = queue.shift();
                            var text = node.textContent.trim();
                            
                            // 随机延迟（50-200ms），模拟人工请求
                            setTimeout(function(n, t) {
                                smartTranslate(t, function(result) {
                                    if (result && result !== t) {
                                        n.textContent = result;
                                    }
                                    completed++;
                                    active--;
                                    updateHint('翻译中 ' + completed + '/' + total);
                                    processNext();
                                });
                            }.bind(null, node, text), randomDelay(50, 200));
                        }
                    }
                    
                    processNext();
                }
                
                // 提示 UI
                function showHint(msg) {
                    var h = document.getElementById('_tr_hint');
                    if (!h) {
                        h = document.createElement('div');
                        h.id = '_tr_hint';
                        h.style.cssText = 'position:fixed;top:10px;right:10px;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;padding:10px 18px;border-radius:8px;font-size:13px;z-index:999999;box-shadow:0 4px 15px rgba(0,0,0,0.2);font-family:system-ui,sans-serif;';
                        document.body.appendChild(h);
                    }
                    h.textContent = msg;
                }
                function updateHint(msg) { var h = document.getElementById('_tr_hint'); if (h) h.textContent = msg; }
                function hideHint() {
                    var h = document.getElementById('_tr_hint');
                    if (h) { h.style.opacity = '0'; h.style.transition = 'opacity 0.3s'; setTimeout(function() { h.remove(); }, 300); }
                }
                
                // 启动
                function start() {
                    showHint('正在分析页面...');
                    setTimeout(function() {
                        var nodes = collectTextNodes();
                        if (nodes.length === 0) {
                            updateHint('无需翻译');
                            setTimeout(hideHint, 1000);
                            return;
                        }
                        console.log('[AutoTranslate] 发现 ' + nodes.length + ' 个待翻译节点');
                        updateHint('翻译中 0/' + nodes.length);
                        translateWithThrottle(nodes);
                    }, 300);
                }
                
                if (document.readyState === 'complete') start();
                else window.addEventListener('load', start);
            })();
        """.trimIndent()
        
        try {
            webView.evaluateJavascript(translateScript, null)
            android.util.Log.d("WebViewManager", "自动翻译脚本已注入")
        } catch (e: Exception) {
            android.util.Log.e("WebViewManager", "自动翻译脚本注入失败", e)
        }
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
