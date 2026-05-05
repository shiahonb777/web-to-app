package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import com.webtoapp.core.logging.AppLogger













class ExtensionPopupManager(
    private val context: Context,
    private val extensionId: String,
    private val popupPath: String,
    private val runtime: ChromeExtensionRuntime? = null
) {
    companion object {
        private const val TAG = "ExtPopupManager"
    }

    private var popupWebView: WebView? = null







    @SuppressLint("SetJavaScriptEnabled")
    fun createPopupWebView(): WebView {
        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
            }


            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)


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

                    injectPolyfill(view)
                    AppLogger.d(TAG, "Popup loaded for: $extensionId")
                }
            }
        }


        val popupUrl = "chrome-extension://$extensionId/$popupPath"
        webView.loadUrl(popupUrl)

        popupWebView = webView
        AppLogger.d(TAG, "Created popup WebView for: $extensionId ($popupUrl)")
        return webView
    }




    private fun injectPolyfill(webView: WebView?) {
        webView ?: return
        val polyfill = ChromeExtensionPolyfill.generatePolyfill(
            extensionId = extensionId,
            isBackground = false
        )
        webView.evaluateJavascript(polyfill, null)
    }




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
