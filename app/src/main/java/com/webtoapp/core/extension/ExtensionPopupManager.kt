package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import com.webtoapp.core.logging.AppLogger

/**
 * Chrome Extension Popup 管理器
 *
 * 支持有 popup.html 的扩展在 Dialog/WebView 中加载弹出页面。
 * BewlyCat 不使用 popup，此类为通用扩展框架预留。
 *
 * 使用方法：
 * 1. 创建 ExtensionPopupManager 实例
 * 2. 调用 createPopupWebView() 获取配置好的 WebView
 * 3. 将 WebView 放入 Dialog 或 BottomSheet 中显示
 * 4. 完成后调用 destroy() 释放资源
 */
class ExtensionPopupManager(
    private val context: Context,
    private val extensionId: String,
    private val popupPath: String, // 相对路径, e.g. "popup.html"
    private val runtime: ChromeExtensionRuntime? = null // 可选：用于消息桥接
) {
    companion object {
        private const val TAG = "ExtPopupManager"
    }

    private var popupWebView: WebView? = null

    /**
     * 创建并配置 popup WebView。
     * 调用者负责将 WebView 添加到视图层级中（如 Dialog）。
     *
     * @return 配置好的 WebView，已加载 popup.html
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun createPopupWebView(): WebView {
        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
            }

            // Cookie 共享
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            // 拦截 chrome-extension:// URL
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    if (ExtensionResourceInterceptor.isChromeExtensionUrl(url)) {
                        return ExtensionResourceInterceptor.intercept(context, url)
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 注入 polyfill
                    injectPolyfill(view)
                    AppLogger.d(TAG, "Popup loaded for: $extensionId")
                }
            }
        }

        // 加载 popup.html（通过 chrome-extension:// URL）
        val popupUrl = "chrome-extension://$extensionId/$popupPath"
        webView.loadUrl(popupUrl)

        popupWebView = webView
        AppLogger.d(TAG, "Created popup WebView for: $extensionId ($popupUrl)")
        return webView
    }

    /**
     * 注入 Chrome Extension polyfill 到 popup WebView
     */
    private fun injectPolyfill(webView: WebView?) {
        webView ?: return
        val polyfill = ChromeExtensionPolyfill.generatePolyfill(
            extensionId = extensionId,
            isBackground = false // Popup 行为类似 content script
        )
        webView.evaluateJavascript(polyfill, null)
    }

    /**
     * 销毁 popup WebView，释放资源
     */
    fun destroy() {
        popupWebView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
        }
        popupWebView = null
        AppLogger.d(TAG, "Popup WebView destroyed for: $extensionId")
    }
}
