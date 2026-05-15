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













class ChromeExtensionRuntime(
    private val context: Context,
    private val extensionId: String,
    private val backgroundScriptPath: String,
    private val originUrl: String,
    private val manifestJson: String = "{}"
) {
    companion object {
        private const val TAG = "ChromeExtRuntime"
        const val JS_BRIDGE_NAME = "WtaExtBridge"
    }

    private var backgroundWebView: WebView? = null
    private var mainWebView: WebView? = null
    private var isInitialized = false
    @Volatile
    private var popupPathOverride: String? = null







    @SuppressLint("SetJavaScriptEnabled")
    fun initialize(mainWebView: WebView) {
        if (isInitialized) return
        this.mainWebView = mainWebView
        ExtensionStorageSync.initialize(context)
        ChromeExtensionContentScriptRegistry.initialize(context)
        if (backgroundScriptPath.isBlank()) {
            isInitialized = true
            AppLogger.i(TAG, "Initialized runtime for: $extensionId without background script")
            return
        }

        val bgWebView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
            }


            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)


            addJavascriptInterface(BackgroundBridge(), JS_BRIDGE_NAME)


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


        val polyfill = ChromeExtensionPolyfill.generatePolyfill(
            extensionId = extensionId,
            manifestJson = manifestJson,
            isBackground = true
        )


        val html = buildBackgroundHtml(polyfill)
        bgWebView.loadDataWithBaseURL(originUrl, html, "text/html", "UTF-8", null)

        backgroundWebView = bgWebView
        isInitialized = true
        AppLogger.i(TAG, "Initialized runtime for: $extensionId (origin: $originUrl, script: $backgroundScriptPath)")
    }




    private fun buildBackgroundHtml(polyfill: String): String {
        val scriptUrl = "chrome-extension://$extensionId/$backgroundScriptPath"
        val scriptTypeAttr = if (isModuleBackground(manifestJson)) " type=\"module\"" else ""
        return """<!DOCTYPE html>
<html>
<head><title>BG: $extensionId</title></head>
<body>
<script>
$polyfill
</script>
<script$scriptTypeAttr src="$scriptUrl"></script>
</body>
</html>"""
    }






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




    fun destroy() {
        DeclarativeNetRequestEngine.clearExtension(extensionId)
        WebRequestBridge.unregisterAll(extensionId)
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

    fun getEffectivePopupPath(): String {
        return popupPathOverride?.takeIf { it.isNotBlank() }
            ?: extractDefaultPopupFromManifest(manifestJson)
    }

    fun setPopupPath(path: String?) {
        popupPathOverride = path?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun executeScript(injectionJson: String): String {
        return ChromeExtensionScriptingBridge.executeScript(context, mainWebView, extensionId, injectionJson)
    }

    fun insertCss(injectionJson: String): Boolean {
        return ChromeExtensionScriptingBridge.insertCss(context, mainWebView, extensionId, injectionJson)
    }

    fun removeCss(injectionJson: String): Boolean {
        return ChromeExtensionScriptingBridge.removeCss(context, mainWebView, extensionId, injectionJson)
    }






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






        @JavascriptInterface
        fun nativeFetch(url: String, method: String, headersJson: String, body: String): String {
            return performNativeFetch(url, method, headersJson, body, originUrl)
        }





        @JavascriptInterface
        fun getCookies(url: String): String {
            return CookieManager.getInstance().getCookie(url) ?: ""
        }






        @JavascriptInterface
        fun setCookieValue(url: String, cookie: String) {
            CookieManager.getInstance().setCookie(url, cookie)
            CookieManager.getInstance().flush()
        }



        @JavascriptInterface
        fun syncStorageGet(key: String): String {
            return ExtensionStorageSync.get(extensionId, key, ExtensionStorageSync.Area.SYNC)
        }

        @JavascriptInterface
        fun syncStorageSet(key: String, value: String) {
            ExtensionStorageSync.set(extensionId, key, value, ExtensionStorageSync.Area.SYNC)
        }

        @JavascriptInterface
        fun syncStorageRemove(key: String) {
            ExtensionStorageSync.remove(extensionId, key, ExtensionStorageSync.Area.SYNC)
        }

        @JavascriptInterface
        fun syncStorageGetAll(): String {
            return ExtensionStorageSync.getAll(extensionId, ExtensionStorageSync.Area.SYNC)
        }

        @JavascriptInterface
        fun syncStorageClear() {
            ExtensionStorageSync.clear(extensionId, ExtensionStorageSync.Area.SYNC)
        }

        @JavascriptInterface
        fun storageGet(area: String, key: String): String {
            return ExtensionStorageSync.get(extensionId, key, ExtensionStorageSync.Area.fromWireName(area))
        }

        @JavascriptInterface
        fun storageSet(area: String, key: String, value: String) {
            ExtensionStorageSync.set(extensionId, key, value, ExtensionStorageSync.Area.fromWireName(area))
        }

        @JavascriptInterface
        fun storageRemove(area: String, key: String) {
            ExtensionStorageSync.remove(extensionId, key, ExtensionStorageSync.Area.fromWireName(area))
        }

        @JavascriptInterface
        fun storageGetAll(area: String): String {
            return ExtensionStorageSync.getAll(extensionId, ExtensionStorageSync.Area.fromWireName(area))
        }

        @JavascriptInterface
        fun storageClear(area: String) {
            ExtensionStorageSync.clear(extensionId, ExtensionStorageSync.Area.fromWireName(area))
        }



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

        @JavascriptInterface
        fun getDnrSessionRules(extId: String): String {
            return DeclarativeNetRequestEngine.getSessionRulesJson(extId)
        }

        @JavascriptInterface
        fun getEnabledDnrStaticRulesets(extId: String): String {
            return DeclarativeNetRequestEngine.getEnabledStaticRulesetIdsJson(extId)
        }

        @JavascriptInterface
        fun updateEnabledDnrStaticRulesets(extId: String, enableRulesetIdsJson: String, disableRulesetIdsJson: String) {
            DeclarativeNetRequestEngine.updateEnabledStaticRulesets(extId, enableRulesetIdsJson, disableRulesetIdsJson)
        }

        @JavascriptInterface
        fun getAvailableDnrStaticRuleCount(extId: String): Int {
            return DeclarativeNetRequestEngine.getAvailableStaticRuleCount(extId)
        }

        @JavascriptInterface
        fun registerContentScripts(extId: String, scriptsJson: String) {
            ChromeExtensionContentScriptRegistry.registerContentScripts(extId, scriptsJson)
        }

        @JavascriptInterface
        fun unregisterContentScripts(extId: String, filterJson: String) {
            ChromeExtensionContentScriptRegistry.unregisterContentScripts(extId, filterJson)
        }

        @JavascriptInterface
        fun getRegisteredContentScripts(extId: String, filterJson: String): String {
            return ChromeExtensionContentScriptRegistry.getRegisteredContentScriptsJson(extId, filterJson)
        }

        @JavascriptInterface
        fun executeScript(extId: String, injectionJson: String): String {
            return if (extId == extensionId) executeScript(injectionJson) else resultsJson(null)
        }

        @JavascriptInterface
        fun insertCss(extId: String, injectionJson: String): Boolean {
            return extId == extensionId && insertCss(injectionJson)
        }

        @JavascriptInterface
        fun removeCss(extId: String, injectionJson: String): Boolean {
            return extId == extensionId && removeCss(injectionJson)
        }



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






class ContentExtensionBridge(
    private val runtimes: Map<String, ChromeExtensionRuntime>,
    private val currentWebViewProvider: (() -> WebView?)? = null,
    private val openPopupHandler: (String, String) -> Unit = { _, _ -> },
    private val openOptionsPageHandler: (String, String) -> Unit = { _, _ -> }
) {
    @JavascriptInterface
    fun postMessageToBackground(extId: String, msgJson: String) {
        AppLogger.d("ChromeExtRuntime", "Content→BG [$extId]: ${msgJson.take(200)}")
        runtimes[extId]?.deliverToBackground(msgJson)
    }




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



    @JavascriptInterface
    fun syncStorageGet(key: String): String {
        return extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.get(extId, key, ExtensionStorageSync.Area.SYNC)
        } ?: ""
    }

    @JavascriptInterface
    fun syncStorageGetForExt(extId: String, key: String): String {
        return ExtensionStorageSync.get(extId, key, ExtensionStorageSync.Area.SYNC)
    }

    @JavascriptInterface
    fun syncStorageSet(key: String, value: String) {
        extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.set(extId, key, value, ExtensionStorageSync.Area.SYNC)
        }
    }

    @JavascriptInterface
    fun syncStorageSetForExt(extId: String, key: String, value: String) {
        ExtensionStorageSync.set(extId, key, value, ExtensionStorageSync.Area.SYNC)
    }

    @JavascriptInterface
    fun syncStorageRemove(key: String) {
        extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.remove(extId, key, ExtensionStorageSync.Area.SYNC)
        }
    }

    @JavascriptInterface
    fun syncStorageRemoveForExt(extId: String, key: String) {
        ExtensionStorageSync.remove(extId, key, ExtensionStorageSync.Area.SYNC)
    }

    @JavascriptInterface
    fun syncStorageGetAll(): String {
        return extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.getAll(extId, ExtensionStorageSync.Area.SYNC)
        } ?: "{}"
    }

    @JavascriptInterface
    fun syncStorageGetAllForExt(extId: String): String {
        return ExtensionStorageSync.getAll(extId, ExtensionStorageSync.Area.SYNC)
    }

    @JavascriptInterface
    fun syncStorageClear() {
        extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.clear(extId, ExtensionStorageSync.Area.SYNC)
        }
    }

    @JavascriptInterface
    fun syncStorageClearForExt(extId: String) {
        ExtensionStorageSync.clear(extId, ExtensionStorageSync.Area.SYNC)
    }

    @JavascriptInterface
    fun storageGet(area: String, key: String): String {
        return extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.get(extId, key, ExtensionStorageSync.Area.fromWireName(area))
        } ?: ""
    }

    @JavascriptInterface
    fun storageGetForExt(extId: String, area: String, key: String): String {
        return ExtensionStorageSync.get(extId, key, ExtensionStorageSync.Area.fromWireName(area))
    }

    @JavascriptInterface
    fun storageSet(area: String, key: String, value: String) {
        extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.set(extId, key, value, ExtensionStorageSync.Area.fromWireName(area))
        }
    }

    @JavascriptInterface
    fun storageSetForExt(extId: String, area: String, key: String, value: String) {
        ExtensionStorageSync.set(extId, key, value, ExtensionStorageSync.Area.fromWireName(area))
    }

    @JavascriptInterface
    fun storageRemove(area: String, key: String) {
        extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.remove(extId, key, ExtensionStorageSync.Area.fromWireName(area))
        }
    }

    @JavascriptInterface
    fun storageRemoveForExt(extId: String, area: String, key: String) {
        ExtensionStorageSync.remove(extId, key, ExtensionStorageSync.Area.fromWireName(area))
    }

    @JavascriptInterface
    fun storageGetAll(area: String): String {
        return extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.getAll(extId, ExtensionStorageSync.Area.fromWireName(area))
        } ?: "{}"
    }

    @JavascriptInterface
    fun storageGetAllForExt(extId: String, area: String): String {
        return ExtensionStorageSync.getAll(extId, ExtensionStorageSync.Area.fromWireName(area))
    }

    @JavascriptInterface
    fun storageClear(area: String) {
        extractExtensionIdForCurrentPage()?.let { extId ->
            ExtensionStorageSync.clear(extId, ExtensionStorageSync.Area.fromWireName(area))
        }
    }

    @JavascriptInterface
    fun storageClearForExt(extId: String, area: String) {
        ExtensionStorageSync.clear(extId, ExtensionStorageSync.Area.fromWireName(area))
    }



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

    @JavascriptInterface
    fun getDnrSessionRules(extId: String): String {
        return DeclarativeNetRequestEngine.getSessionRulesJson(extId)
    }

    @JavascriptInterface
    fun getEnabledDnrStaticRulesets(extId: String): String {
        return DeclarativeNetRequestEngine.getEnabledStaticRulesetIdsJson(extId)
    }

    @JavascriptInterface
    fun updateEnabledDnrStaticRulesets(extId: String, enableRulesetIdsJson: String, disableRulesetIdsJson: String) {
        DeclarativeNetRequestEngine.updateEnabledStaticRulesets(extId, enableRulesetIdsJson, disableRulesetIdsJson)
    }

    @JavascriptInterface
    fun getAvailableDnrStaticRuleCount(extId: String): Int {
        return DeclarativeNetRequestEngine.getAvailableStaticRuleCount(extId)
    }

    @JavascriptInterface
    fun registerContentScripts(extId: String, scriptsJson: String) {
        ChromeExtensionContentScriptRegistry.registerContentScripts(extId, scriptsJson)
    }

    @JavascriptInterface
    fun unregisterContentScripts(extId: String, filterJson: String) {
        ChromeExtensionContentScriptRegistry.unregisterContentScripts(extId, filterJson)
    }

    @JavascriptInterface
    fun getRegisteredContentScripts(extId: String, filterJson: String): String {
        return ChromeExtensionContentScriptRegistry.getRegisteredContentScriptsJson(extId, filterJson)
    }

    @JavascriptInterface
    fun executeScript(extId: String, injectionJson: String): String {
        val runtime = runtimes[extId]
        if (runtime != null) {
            return runtime.executeScript(injectionJson)
        }
        val currentWebView = currentWebViewProvider?.invoke()
        return ChromeExtensionScriptingBridge.executeScript(
            currentWebView?.context,
            currentWebView,
            extId,
            injectionJson
        )
    }

    @JavascriptInterface
    fun insertCss(extId: String, injectionJson: String): Boolean {
        val runtime = runtimes[extId]
        return if (runtime != null) {
            runtime.insertCss(injectionJson)
        } else {
            val currentWebView = currentWebViewProvider?.invoke()
            ChromeExtensionScriptingBridge.insertCss(
                currentWebView?.context,
                currentWebView,
                extId,
                injectionJson
            )
        }
    }

    @JavascriptInterface
    fun removeCss(extId: String, injectionJson: String): Boolean {
        val runtime = runtimes[extId]
        return if (runtime != null) {
            runtime.removeCss(injectionJson)
        } else {
            val currentWebView = currentWebViewProvider?.invoke()
            ChromeExtensionScriptingBridge.removeCss(
                currentWebView?.context,
                currentWebView,
                extId,
                injectionJson
            )
        }
    }

    @JavascriptInterface
    fun openPopup(extId: String, popupPath: String) {
        openPopupHandler(extId, popupPath)
    }

    @JavascriptInterface
    fun openOptionsPage(extId: String, optionsPath: String) {
        openOptionsPageHandler(extId, optionsPath)
    }

    @JavascriptInterface
    fun setPopupPath(extId: String, popupPath: String) {
        runtimes[extId]?.setPopupPath(popupPath)
    }

    @JavascriptInterface
    fun getPopupPath(extId: String): String {
        return runtimes[extId]?.getEffectivePopupPath().orEmpty()
    }



    @JavascriptInterface
    fun openPort(extId: String, name: String): String {
        val portId = "port_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"

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

    private fun extractExtensionIdForCurrentPage(): String? {
        if (runtimes.size == 1) return runtimes.keys.firstOrNull()
        return null
    }
}

private fun extractDefaultPopupFromManifest(manifestJson: String?): String {
    if (manifestJson.isNullOrBlank()) return ""
    return try {
        val manifest = JSONObject(manifestJson)
        manifest.optJSONObject("action")?.optString("default_popup", "")?.takeIf { it.isNotBlank() }
            ?: manifest.optJSONObject("browser_action")?.optString("default_popup", "")?.takeIf { it.isNotBlank() }
            ?: manifest.optJSONObject("page_action")?.optString("default_popup", "")?.takeIf { it.isNotBlank() }
            ?: ""
    } catch (_: Exception) {
        ""
    }
}

private fun isModuleBackground(manifestJson: String?): Boolean {
    if (manifestJson.isNullOrBlank()) return false
    return try {
        JSONObject(manifestJson)
            .optJSONObject("background")
            ?.optString("type", "")
            ?.equals("module", ignoreCase = true) == true
    } catch (_: Exception) {
        false
    }
}

private fun resultsJson(valueJson: String?): String {
    val value = valueJson ?: "null"
    return """[{"result":$value}]"""
}







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


        val cookies = android.webkit.CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrEmpty()) {
            connection.setRequestProperty("Cookie", cookies)
        }


        try {
            val headers = org.json.JSONObject(headersJson)
            val iter = headers.keys()
            while (iter.hasNext()) {
                val key = iter.next()
                connection.setRequestProperty(key, headers.getString(key))
            }
        } catch (e: Exception) { AppLogger.w("ChromeExtRuntime", "Failed to parse custom request headers", e) }


        if (refererOrigin != null) {
            if (connection.getRequestProperty("Referer") == null) {
                connection.setRequestProperty("Referer", refererOrigin)
            }
            if (connection.getRequestProperty("Origin") == null) {
                connection.setRequestProperty("Origin", refererOrigin.trimEnd('/'))
            }
        }


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
