package com.webtoapp.core.extension

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.*
import com.webtoapp.core.logging.AppLogger
import org.json.JSONObject

/**
 * Chrome Extension Background Script when.
 *
 * WebView extension background script.
 * use loadDataWithBaseURL origin .
 * - background script fetch() CORS issue.
 * - CookieManager WebView cookie.
 *
 * .
 * Content script WtaExtBridge.postMessageToBackground() WebView onMessage.
 * Background script WtaExtBridge.postMessageToContent() WebView resolve.
 */
class ChromeExtensionRuntime(
    private val context: Context,
    private val extensionId: String,
    private val backgroundScriptPath: String, // e.g. background/index.js.
    private val originUrl: String
) {
    companion object {
        private const val TAG = "ChromeExtRuntime"
        const val JS_BRIDGE_NAME = "WtaExtBridge"
    }

    private var backgroundWebView: WebView? = null
    private var mainWebView: WebView? = null
    private var isInitialized = false

    /**
     * background when .
     * in use .
     *
     * @param mainWebView WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun initialize(mainWebView: WebView) {
        if (isInitialized) return
        this.mainWebView = mainWebView

        val bgWebView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
            }

            // Cookie CookieManager single .
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            // native bridge.
            addJavascriptInterface(BackgroundBridge(), JS_BRIDGE_NAME)

            // WebViewClientintercept chrome-extension:// URL +.
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
                    AppLogger.i(TAG, "Background WebView loaded for: $extensionId")
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        AppLogger.e(TAG, "Background WebView error: ${error?.description}")
                    }
                }
            }
        }

        // background polyfill.
        val polyfill = ChromeExtensionPolyfill.generatePolyfill(
            extensionId = extensionId,
            isBackground = true
        )

        // HTML shell polyfill <script src> background script.
        val html = buildBackgroundHtml(polyfill)
        bgWebView.loadDataWithBaseURL(originUrl, html, "text/html", "UTF-8", null)

        backgroundWebView = bgWebView
        isInitialized = true
        AppLogger.i(TAG, "Initialized runtime for: $extensionId (origin: $originUrl, script: $backgroundScriptPath)")
    }

    /**
     * background HTML.
     */
    private fun buildBackgroundHtml(polyfill: String): String {
        // use chrome-extension:// URL use background script.
        // ExtensionResourceInterceptor intercept and from assets.
        val scriptUrl = "chrome-extension://$extensionId/$backgroundScriptPath"
        return """<!DOCTYPE html>
<html>
<head><title>BG: $extensionId</title></head>
<body>
<script>
$polyfill
</script>
<script src="$scriptUrl"></script>
</body>
</html>"""
    }

    /**
     * from content script to background script.
     * WebView ContentExtensionBridge use .
     * can in use .
     */
    fun deliverToBackground(msgJson: String) {
        val bgWebView = backgroundWebView ?: return
        val escaped = JSONObject.quote(msgJson)
        Handler(Looper.getMainLooper()).post {
            bgWebView.evaluateJavascript(
                "if(window.__WTA_DELIVER_TO_BACKGROUND__)window.__WTA_DELIVER_TO_BACKGROUND__($escaped);",
                null
            )
        }
    }

    /**
     * from background script content script.
     * BackgroundBridge use .
     * can in use .
     */
    fun deliverToContent(responseJson: String) {
        val mWebView = mainWebView ?: return
        val escaped = JSONObject.quote(responseJson)
        Handler(Looper.getMainLooper()).post {
            mWebView.evaluateJavascript(
                "if(window.__WTA_DELIVER_RESPONSE__)window.__WTA_DELIVER_RESPONSE__($escaped);",
                null
            )
        }
    }

    /**
     * from background script to content script.
     * BackgroundBridge.sendMessageToTab() use .
     */
    fun deliverMessageToContent(msgJson: String) {
        val mWebView = mainWebView ?: return
        val escaped = JSONObject.quote(msgJson)
        Handler(Looper.getMainLooper()).post {
            mWebView.evaluateJavascript(
                "if(window.__WTA_DELIVER_TO_CONTENT__)window.__WTA_DELIVER_TO_CONTENT__($escaped);",
                null
            )
        }
    }

    /**
     * Port to background script.
     */
    fun deliverPortMessageToBackground(msgJson: String) {
        val bgWebView = backgroundWebView ?: return
        val escaped = JSONObject.quote(msgJson)
        Handler(Looper.getMainLooper()).post {
            bgWebView.evaluateJavascript(
                "if(window.__WTA_PORT_MESSAGE__)window.__WTA_PORT_MESSAGE__($escaped);",
                null
            )
        }
    }

    /**
     * Port to content script.
     */
    fun deliverPortMessageToContent(msgJson: String) {
        val mWebView = mainWebView ?: return
        val escaped = JSONObject.quote(msgJson)
        Handler(Looper.getMainLooper()).post {
            mWebView.evaluateJavascript(
                "if(window.__WTA_PORT_MESSAGE__)window.__WTA_PORT_MESSAGE__($escaped);",
                null
            )
        }
    }

    /**
     * Port to background script.
     */
    fun deliverPortDisconnectToBackground(portId: String) {
        val bgWebView = backgroundWebView ?: return
        val escaped = JSONObject.quote(portId)
        Handler(Looper.getMainLooper()).post {
            bgWebView.evaluateJavascript(
                "if(window.__WTA_PORT_DISCONNECT__)window.__WTA_PORT_DISCONNECT__($escaped);",
                null
            )
        }
    }

    /**
     * Port to content script.
     */
    fun deliverPortDisconnectToContent(portId: String) {
        val mWebView = mainWebView ?: return
        val escaped = JSONObject.quote(portId)
        Handler(Looper.getMainLooper()).post {
            mWebView.evaluateJavascript(
                "if(window.__WTA_PORT_DISCONNECT__)window.__WTA_PORT_DISCONNECT__($escaped);",
                null
            )
        }
    }

    /**
     * background WebViewRelease.
     */
    fun destroy() {
        backgroundWebView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.removeJavascriptInterface(JS_BRIDGE_NAME)
            wv.destroy()
        }
        backgroundWebView = null
        mainWebView = null
        isInitialized = false
        AppLogger.i(TAG, "Runtime destroyed for: $extensionId")
    }

    /**
     * Background WebView native bridge.
     * Background script WtaExtBridge.postMessageToContent() .
     * WtaExtBridge.nativeFetch() CORS HTTP request.
     */
    inner class BackgroundBridge {
        @JavascriptInterface
        fun postMessageToContent(responseJson: String) {
            AppLogger.d(TAG, "BG→Content [$extensionId]: ${responseJson.take(200)}")
            deliverToContent(responseJson)
        }

        @JavascriptInterface
        fun log(level: String, message: String) {
            when (level) {
                "e" -> AppLogger.e(TAG, "[BG:$extensionId] $message")
                "w" -> AppLogger.w(TAG, "[BG:$extensionId] $message")
                else -> AppLogger.d(TAG, "[BG:$extensionId] $message")
            }
        }

        /**
         * Android HttpURLConnection HTTP request CORS .
         * Save cookie.
         * @return JSON {ok, status, statusText, headers, body}.
         */
        @JavascriptInterface
        fun nativeFetch(url: String, method: String, headersJson: String, body: String): String {
            return performNativeFetch(url, method, headersJson, body, originUrl)
        }

        /**
         * Get URL cookie.
         * @return cookie "name1=value1; name2=value2".
         */
        @JavascriptInterface
        fun getCookies(url: String): String {
            return CookieManager.getInstance().getCookie(url) ?: ""
        }

        /**
         * cookie.
         * @param url URL.
         * @param cookie cookie.
         */
        @JavascriptInterface
        fun setCookieValue(url: String, cookie: String) {
            CookieManager.getInstance().setCookie(url, cookie)
            CookieManager.getInstance().flush()
        }

        // ===== Phase K: Cross-WebView storage sync =====

        @JavascriptInterface
        fun syncStorageGet(key: String): String {
            return ExtensionStorageSync.get(extensionId, key)
        }

        @JavascriptInterface
        fun syncStorageSet(key: String, value: String) {
            ExtensionStorageSync.set(extensionId, key, value)
        }

        @JavascriptInterface
        fun syncStorageRemove(key: String) {
            ExtensionStorageSync.remove(extensionId, key)
        }

        @JavascriptInterface
        fun syncStorageGetAll(): String {
            return ExtensionStorageSync.getAll(extensionId)
        }

        @JavascriptInterface
        fun syncStorageClear() {
            ExtensionStorageSync.clear(extensionId)
        }

        // ===== Phase G: webRequest / declarativeNetRequest =====

        @JavascriptInterface
        fun registerWebRequestFilter(extId: String, urlPatternsJson: String, resourceTypesJson: String, blocking: Boolean) {
            WebRequestBridge.registerFilter(extId, urlPatternsJson, resourceTypesJson, blocking)
        }

        @JavascriptInterface
        fun updateDnrDynamicRules(extId: String, addRulesJson: String, removeRuleIdsJson: String) {
            DeclarativeNetRequestEngine.updateDynamicRules(extId, addRulesJson, removeRuleIdsJson)
        }

        @JavascriptInterface
        fun updateDnrSessionRules(extId: String, addRulesJson: String, removeRuleIdsJson: String) {
            DeclarativeNetRequestEngine.updateSessionRules(extId, addRulesJson, removeRuleIdsJson)
        }

        @JavascriptInterface
        fun getDnrDynamicRules(extId: String): String {
            return DeclarativeNetRequestEngine.getDynamicRulesJson(extId)
        }

        // ===== Phase H: tabs.sendMessage + Port =====

        @JavascriptInterface
        fun sendMessageToTab(extId: String, msgJson: String) {
            AppLogger.d(TAG, "BG→Tab [$extId]: ${msgJson.take(200)}")
            deliverMessageToContent(msgJson)
        }

        @JavascriptInterface
        fun openPort(extId: String, name: String): String {
            val portId = "port_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
            AppLogger.d(TAG, "Port opened: $portId (name=$name, ext=$extId)")
            return portId
        }

        @JavascriptInterface
        fun portPostMessage(portId: String, direction: String, msgJson: String) {
            AppLogger.d(TAG, "Port message [$portId] $direction: ${msgJson.take(200)}")
            if (direction == "toContent") {
                deliverPortMessageToContent(msgJson)
            } else {
                deliverPortMessageToBackground(msgJson)
            }
        }

        @JavascriptInterface
        fun portDisconnect(portId: String, direction: String) {
            AppLogger.d(TAG, "Port disconnect [$portId] $direction")
            if (direction == "toContent") {
                deliverPortDisconnectToContent(portId)
            } else {
                deliverPortDisconnectToBackground(portId)
            }
        }

        // ===== Phase J: Downloads =====

        @JavascriptInterface
        fun startDownload(url: String, filename: String, headersJson: String): String {
            return try {
                val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                if (filename.isNotEmpty()) {
                    request.setDestinationInExternalPublicDir(
                        android.os.Environment.DIRECTORY_DOWNLOADS, filename
                    )
                }
                request.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val downloadId = dm.enqueue(request)
                AppLogger.d(TAG, "Download started: $url (id=$downloadId)")
                downloadId.toString()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Download failed: $url", e)
                "-1"
            }
        }
    }
}

/**
 * WebView native bridge.
 * Content script WtaExtBridge.postMessageToBackground() .
 * WtaExtBridge.nativeFetch() CORS HTTP request.
 */
class ContentExtensionBridge(
    private val runtimes: Map<String, ChromeExtensionRuntime>
) {
    @JavascriptInterface
    fun postMessageToBackground(extId: String, msgJson: String) {
        AppLogger.d("ChromeExtRuntime", "Content→BG [$extId]: ${msgJson.take(200)}")
        runtimes[extId]?.deliverToBackground(msgJson)
    }

    /**
     * Android HttpURLConnection HTTP request CORS.
     */
    @JavascriptInterface
    fun nativeFetch(url: String, method: String, headersJson: String, body: String): String {
        return performNativeFetch(url, method, headersJson, body, null)
    }

    @JavascriptInterface
    fun getCookies(url: String): String {
        return android.webkit.CookieManager.getInstance().getCookie(url) ?: ""
    }

    @JavascriptInterface
    fun setCookieValue(url: String, cookie: String) {
        android.webkit.CookieManager.getInstance().setCookie(url, cookie)
        android.webkit.CookieManager.getInstance().flush()
    }

    // ===== Phase K: Cross-WebView storage sync =====

    @JavascriptInterface
    fun syncStorageGet(key: String): String {
        // Content bridge uses the first runtime's extension ID or a fallback
        return ExtensionStorageSync.get("", key)
    }

    @JavascriptInterface
    fun syncStorageSet(key: String, value: String) {
        ExtensionStorageSync.set("", key, value)
    }

    @JavascriptInterface
    fun syncStorageRemove(key: String) {
        ExtensionStorageSync.remove("", key)
    }

    @JavascriptInterface
    fun syncStorageGetAll(): String {
        return ExtensionStorageSync.getAll("")
    }

    @JavascriptInterface
    fun syncStorageClear() {
        ExtensionStorageSync.clear("")
    }

    // ===== Phase G: webRequest / declarativeNetRequest =====

    @JavascriptInterface
    fun registerWebRequestFilter(extId: String, urlPatternsJson: String, resourceTypesJson: String, blocking: Boolean) {
        WebRequestBridge.registerFilter(extId, urlPatternsJson, resourceTypesJson, blocking)
    }

    @JavascriptInterface
    fun updateDnrDynamicRules(extId: String, addRulesJson: String, removeRuleIdsJson: String) {
        DeclarativeNetRequestEngine.updateDynamicRules(extId, addRulesJson, removeRuleIdsJson)
    }

    @JavascriptInterface
    fun updateDnrSessionRules(extId: String, addRulesJson: String, removeRuleIdsJson: String) {
        DeclarativeNetRequestEngine.updateSessionRules(extId, addRulesJson, removeRuleIdsJson)
    }

    @JavascriptInterface
    fun getDnrDynamicRules(extId: String): String {
        return DeclarativeNetRequestEngine.getDynamicRulesJson(extId)
    }

    // ===== Phase H: Port messaging =====

    @JavascriptInterface
    fun openPort(extId: String, name: String): String {
        val portId = "port_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
        // Notify background of new connection
        val portInfo = org.json.JSONObject().apply {
            put("portId", portId)
            put("name", name)
            put("extId", extId)
            put("event", "connect")
        }.toString()
        runtimes[extId]?.deliverPortMessageToBackground(portInfo)
        return portId
    }

    @JavascriptInterface
    fun portPostMessage(portId: String, direction: String, msgJson: String) {
        // Route based on direction
        if (direction == "toBackground") {
            runtimes.values.firstOrNull()?.deliverPortMessageToBackground(msgJson)
        } else {
            runtimes.values.firstOrNull()?.deliverPortMessageToContent(msgJson)
        }
    }

    @JavascriptInterface
    fun portDisconnect(portId: String, direction: String) {
        if (direction == "toBackground") {
            runtimes.values.firstOrNull()?.deliverPortDisconnectToBackground(portId)
        } else {
            runtimes.values.firstOrNull()?.deliverPortDisconnectToContent(portId)
        }
    }
}

/**
 * Android HttpURLConnection HTTP request CORS .
 * Save CookieManager in cookie.
 *
 * @return JSON {ok, status, statusText, headers, body}.
 */
internal fun performNativeFetch(
    url: String,
    method: String,
    headersJson: String,
    body: String,
    refererOrigin: String?
): String {
    return try {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = method.uppercase()
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true

        // from CookieManager Get cookie.
        val cookies = android.webkit.CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrEmpty()) {
            connection.setRequestProperty("Cookie", cookies)
        }

        // request.
        try {
            val headers = org.json.JSONObject(headersJson)
            val iter = headers.keys()
            while (iter.hasNext()) {
                val key = iter.next()
                connection.setRequestProperty(key, headers.getString(key))
            }
        } catch (e: Exception) { AppLogger.w("ChromeExtRuntime", "Failed to parse custom request headers", e) }

        // Referer / Origin.
        if (refererOrigin != null) {
            if (connection.getRequestProperty("Referer") == null) {
                connection.setRequestProperty("Referer", refererOrigin)
            }
            if (connection.getRequestProperty("Origin") == null) {
                connection.setRequestProperty("Origin", refererOrigin.trimEnd('/'))
            }
        }

        // request.
        if (body.isNotEmpty() && method.uppercase() in listOf("POST", "PUT", "PATCH")) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }

        val status = connection.responseCode
        val statusText = connection.responseMessage ?: ""

        val respHeaders = org.json.JSONObject()
        connection.headerFields?.forEach { (key, values) ->
            if (key != null && values.isNotEmpty()) {
                respHeaders.put(key.lowercase(), values.joinToString(", "))
            }
        }

        val respBody = try {
            (if (status >= 400) connection.errorStream else connection.inputStream)
                ?.bufferedReader()?.readText() ?: ""
        } catch (_: Exception) { "" }

        // Save in Set-Cookie.
        connection.headerFields?.get("Set-Cookie")?.forEach { cookie ->
            android.webkit.CookieManager.getInstance().setCookie(url, cookie)
        }
        android.webkit.CookieManager.getInstance().flush()

        connection.disconnect()

        org.json.JSONObject().apply {
            put("ok", status in 200..299)
            put("status", status)
            put("statusText", statusText)
            put("headers", respHeaders)
            put("body", respBody)
        }.toString()
    } catch (e: Exception) {
        AppLogger.e("NativeFetch", "Error fetching $url: ${e.message}")
        org.json.JSONObject().apply {
            put("ok", false)
            put("status", 0)
            put("statusText", "Network Error: ${e.message}")
            put("headers", org.json.JSONObject())
            put("body", "")
        }.toString()
    }
}

/**
 * from extension URL rules origin URL.
 * use loadDataWithBaseURL background WebView origin.
 */
fun deriveOriginUrl(urlMatches: List<UrlMatchRule>): String {
    val firstMatch = urlMatches.firstOrNull { !it.exclude }?.pattern ?: return "about:blank"
    val url = firstMatch
        .replace("*://", "https://")
        .replace("/*", "/")
        .replace("*.", "www.")
    return try {
        val uri = Uri.parse(url)
        "${uri.scheme}://${uri.host}/"
    } catch (e: Exception) {
        "about:blank"
    }
}