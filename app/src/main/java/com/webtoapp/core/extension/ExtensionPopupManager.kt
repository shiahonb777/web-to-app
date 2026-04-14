package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import com.webtoapp.core.logging.AppLogger

/**
 * Chrome Extension Popup manager.
 *
 * Supports popup.html extension in Dialog/WebView in .
 * BewlyCat not use popup as use extension .
 *
 * use .
 * 1. ExtensionPopupManager.
 * 2. use createPopupWebView() Getconfig WebView.
 * 3. WebView Dialog or BottomSheet in.
 * 4. after use destroy() Release.
 */
class ExtensionPopupManager(
    private val context: Context,
    private val extensionId: String,
    private val popupPath: String, // e.g. popup.html.
    private val runtime: ChromeExtensionRuntime? = null // Note.
) {
    companion object {
        private const val TAG = "ExtPopupManager"
    }

    private var popupWebView: WebView? = null

    /**
     * and config popup WebView.
     * use WebView to level in .
     *
     * @return config WebView popup.html.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun createPopupWebView(): WebView {
        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
            }

            // Cookie.
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            // intercept chrome-extension:// URL.
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
                    // polyfill.
                    injectPolyfill(view)
                    AppLogger.d(TAG, "Popup loaded for: $extensionId")
                }
            }
        }

        // popup.html.
        val popupUrl = "chrome-extension://$extensionId/$popupPath"
        webView.loadUrl(popupUrl)

        popupWebView = webView
        AppLogger.d(TAG, "Created popup WebView for: $extensionId ($popupUrl)")
        return webView
    }

    /**
     * Chrome Extension polyfill to popup WebView.
     */
    private fun injectPolyfill(webView: WebView?) {
        webView ?: return
        val polyfill = ChromeExtensionPolyfill.generatePolyfill(
            extensionId = extensionId,
            isBackground = false // Popup content script.
        )
        webView.evaluateJavascript(polyfill, null)
    }

    /**
     * popup WebViewRelease.
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