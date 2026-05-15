package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import com.webtoapp.core.logging.AppLogger













class ExtensionPopupManager(
    private val context: Context,
    private val extensionId: String,
    private val popupPath: String,
    private val runtime: ChromeExtensionRuntime? = null,
    private val targetWebViewProvider: (() -> WebView?)? = null,
    private val manifestJson: String = "{}",
    private val openPopupHandler: (String, String) -> Unit = { _, _ -> },
    private val openOptionsPageHandler: (String, String) -> Unit = { _, _ -> }
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

            addJavascriptInterface(
                ContentExtensionBridge(
                    runtimes = runtime?.let { mapOf(extensionId to it) } ?: emptyMap(),
                    currentWebViewProvider = targetWebViewProvider,
                    openPopupHandler = openPopupHandler,
                    openOptionsPageHandler = openOptionsPageHandler
                ),
                ChromeExtensionRuntime.JS_BRIDGE_NAME
            )


            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    if (ExtensionResourceInterceptor.isExtensionResourceUrl(url)) {
                        return ExtensionResourceInterceptor.interceptAny(context, url)
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
        if (!loadPopupHtmlWithPolyfill(webView, popupUrl)) {
            webView.loadUrl(popupUrl)
        }

        popupWebView = webView
        AppLogger.d(TAG, "Created popup WebView for: $extensionId ($popupUrl)")
        return webView
    }




    private fun injectPolyfill(webView: WebView?) {
        webView ?: return
        val polyfill = ChromeExtensionPolyfill.generatePolyfill(
            extensionId = extensionId,
            manifestJson = manifestJson,
            isBackground = false
        )
        webView.evaluateJavascript(polyfill, null)
    }

    private fun loadPopupHtmlWithPolyfill(webView: WebView, popupUrl: String): Boolean {
        return try {
            val response = ExtensionResourceInterceptor.intercept(context, popupUrl) ?: return false
            val mimeType = response.mimeType ?: return false
            if (!mimeType.equals("text/html", ignoreCase = true)) return false
            val html = response.data?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: return false
            val polyfill = ChromeExtensionPolyfill.generatePolyfill(
                extensionId = extensionId,
                manifestJson = manifestJson,
                isBackground = false
            )
            val injectedHtml = injectPolyfillIntoHtml(html, polyfill)
            webView.loadDataWithBaseURL(popupUrl, injectedHtml, "text/html", "UTF-8", popupUrl)
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to preload popup HTML for $extensionId", e)
            false
        }
    }

    private fun injectPolyfillIntoHtml(html: String, polyfill: String): String {
        val scriptTag = "<script>\n$polyfill\n</script>"
        return when {
            html.contains("</head>", ignoreCase = true) ->
                html.replaceFirst(Regex("(?i)</head>"), "$scriptTag</head>")
            html.contains("<body", ignoreCase = true) ->
                html.replaceFirst(Regex("(?i)<body([^>]*)>"), "<body$1>$scriptTag")
            else -> "$scriptTag$html"
        }
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
