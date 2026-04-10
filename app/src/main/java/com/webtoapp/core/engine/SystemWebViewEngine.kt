package com.webtoapp.core.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.*
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.core.webview.WebViewManager
import com.webtoapp.data.model.WebViewConfig

/**
 * 系统 WebView 引擎实现
 * 委托给现有的 WebViewManager，保持完全兼容
 */
class SystemWebViewEngine(
    private val context: Context,
    private val adBlocker: AdBlocker
) : BrowserEngine {

    override val engineType = EngineType.SYSTEM_WEBVIEW

    private var webView: WebView? = null
    private val webViewManager = WebViewManager(context, adBlocker)

    override fun createView(
        context: Context,
        config: WebViewConfig,
        callback: BrowserEngineCallback
    ): View {
        val wv = WebView(context)

        // 桥接 BrowserEngineCallback → WebViewCallbacks
        val bridgeCallbacks = object : WebViewCallbacks {
            override fun onPageStarted(url: String?) = callback.onPageStarted(url)
            override fun onPageFinished(url: String?) = callback.onPageFinished(url)
            override fun onProgressChanged(progress: Int) = callback.onProgressChanged(progress)
            override fun onTitleChanged(title: String?) = callback.onTitleChanged(title)
            override fun onIconReceived(icon: Bitmap?) = callback.onIconReceived(icon)
            override fun onError(errorCode: Int, description: String) = callback.onError(errorCode, description)
            override fun onSslError(error: String) = callback.onSslError(error)
            override fun onExternalLink(url: String) = callback.onExternalLink(url)

            override fun onShowCustomView(view: View?, cb: WebChromeClient.CustomViewCallback?) {
                callback.onShowCustomView(view, cb)
            }

            override fun onHideCustomView() = callback.onHideCustomView()

            override fun onGeolocationPermission(origin: String?, cb: GeolocationPermissions.Callback?) {
                // GeckoView 不需要此回调，仅 SystemWebView 使用
                // 直接允许（保持原有行为）
                cb?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // 保持原有行为：直接授权
                request?.grant(request.resources)
            }

            override fun onShowFileChooser(
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                // 默认不处理文件选择（由 Activity 级别处理）
                return false
            }

            override fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) {
                callback.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
            }

            override fun onLongPress(webView: WebView, x: Float, y: Float): Boolean = false

            override fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {
                callback.onConsoleMessage(level, message, sourceId, lineNumber)
            }

            override fun onNewWindow(resultMsg: android.os.Message?) {
                callback.onNewWindow(resultMsg)
            }
        }

        webViewManager.configureWebView(wv, config, bridgeCallbacks)
        webView = wv
        return wv
    }

    override fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {
        webView?.evaluateJavascript(script) { result ->
            resultCallback?.invoke(result)
        }
    }

    override fun canGoBack(): Boolean = webView?.canGoBack() ?: false
    override fun goBack() { webView?.goBack() }
    override fun canGoForward(): Boolean = webView?.canGoForward() ?: false
    override fun goForward() { webView?.goForward() }
    override fun reload() { webView?.reload() }
    override fun stopLoading() { webView?.stopLoading() }
    override fun getCurrentUrl(): String? = webView?.url
    override fun getTitle(): String? = webView?.title
    override fun getView(): View? = webView

    override fun destroy() {
        webView?.apply {
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        webView?.clearCache(includeDiskFiles)
    }

    override fun clearHistory() {
        webView?.clearHistory()
    }

    override fun getShields(): com.webtoapp.core.engine.shields.BrowserShields? {
        return webViewManager.getShields()
    }

    override fun toggleReaderMode(enabled: Boolean) {
        val shields = getShields() ?: return
        if (enabled) {
            evaluateJavascript(shields.readerMode.generateExtractScript()) { result ->
                if (result != null && result != "null" && result != "\"null\"") {
                    try {
                        val jsonStr = com.webtoapp.util.GsonProvider.gson.fromJson(result, String::class.java)
                        val obj = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
                        val title = obj.get("title")?.asString ?: ""
                        val siteName = obj.get("siteName")?.asString ?: ""
                        val author = obj.get("author")?.asString ?: ""
                        val content = obj.get("content")?.asString ?: ""
                        val url = obj.get("url")?.asString ?: ""
                        
                        val readerHtml = shields.readerMode.generateReaderHtml(
                            title, siteName, author, content, url
                        )
                        webView?.loadDataWithBaseURL(url, readerHtml, "text/html", "UTF-8", url)
                    } catch (e: Exception) {
                        com.webtoapp.core.logging.AppLogger.e("SystemWebViewEngine", "Reader mode parse failed", e)
                    }
                }
            }
        } else {
            webView?.reload()
        }
    }

    /**
     * 获取底层 WebViewManager（用于需要直接访问的场景，如扩展模块注入）
     */
    fun getWebViewManager(): WebViewManager = webViewManager

    /**
     * 获取底层 WebView 实例
     */
    fun getWebView(): WebView? = webView
}
