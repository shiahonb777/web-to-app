package com.webtoapp.core.webview

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.WebViewConfig

internal class StrictHostRuntimePolicy(
    private val context: Context,
    private val urlPolicy: WebViewUrlPolicy
) {
    fun shouldUseConservativeScriptMode(pageUrl: String?): Boolean {
        return urlPolicy.shouldUseConservativeScriptMode(pageUrl)
    }

    fun shouldUseScriptlessMode(pageUrl: String?): Boolean {
        return urlPolicy.shouldUseScriptlessMode(pageUrl)
    }

    fun shouldBypassAggressiveNetworkHooks(
        request: WebResourceRequest,
        requestUrl: String,
        currentMainFrameUrl: String?
    ): Boolean {
        if (request.isForMainFrame) {
            return shouldUseScriptlessMode(requestUrl)
        }

        val topLevelUrl = currentMainFrameUrl
            ?: request.requestHeaders["Referer"]
            ?: request.requestHeaders["referer"]
            ?: return false

        return shouldUseScriptlessMode(topLevelUrl)
    }

    fun applyPreloadPolicyForUrl(
        webView: WebView,
        pageUrl: String?,
        currentConfig: WebViewConfig?,
        currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
    ) {
        resetStrictHostSessionState(webView, pageUrl)
        applyStrictHostRuntimePolicy(
            webView = webView,
            pageUrl = pageUrl,
            currentConfig = currentConfig,
            currentDeviceDisguiseConfig = currentDeviceDisguiseConfig
        )
    }

    fun resetStrictHostSessionState(webView: WebView, pageUrl: String?) {
        if (!shouldUseScriptlessMode(pageUrl)) return

        webView.clearCache(true)
        webView.clearHistory()

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()

        val origins = buildStrictHostOrigins(pageUrl)
        if (origins.isNotEmpty()) {
            val webStorage = WebStorage.getInstance()
            origins.forEach { origin ->
                webStorage.deleteOrigin(origin)
            }
        }

        AppLogger.d("WebViewManager", "Strict host session reset applied for $pageUrl")
    }

    fun buildStrictHostOrigins(pageUrl: String?): Set<String> {
        val host = urlPolicy.extractHostFromUrl(pageUrl) ?: return emptySet()
        val baseHost = host.removePrefix("www.")
        val hosts = linkedSetOf(host, baseHost, "www.$baseHost")
        return hosts
            .filter { it.isNotBlank() }
            .flatMap { targetHost -> listOf("https://$targetHost", "http://$targetHost") }
            .toSet()
    }

    fun applyStrictHostRuntimePolicy(
        webView: WebView,
        pageUrl: String?,
        currentConfig: WebViewConfig?,
        currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
    ) {
        if (!shouldUseScriptlessMode(pageUrl)) return

        val settings = webView.settings
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.removeJavascriptInterface("NativeShareBridge")
        webView.removeJavascriptInterface("AndroidDownload")
        webView.removeJavascriptInterface("NativeBridge")
        applyRequestedWithHeaderAllowListForStrictHost(settings)

        val desktopRequested = isDesktopUaRequested(currentConfig, currentDeviceDisguiseConfig)
        val strictMobileUA = WebViewManager.STRICT_COMPAT_MOBILE_USER_AGENT
            ?: WebViewManager.STRICT_COMPAT_MOBILE_UA_FALLBACK
        if (!desktopRequested && settings.userAgentString != strictMobileUA) {
            settings.userAgentString = strictMobileUA
            AppLogger.d("WebViewManager", "Strict host policy: force strict mobile UA for $pageUrl")
        } else if (desktopRequested) {
            AppLogger.d("WebViewManager", "Strict host policy: keep desktop UA by user request for $pageUrl")
        }

        AppLogger.d(
            "WebViewManager",
            "Strict host runtime policy applied: url=$pageUrl, thirdPartyCookie=true, jsInterfacesRemoved=true"
        )
    }

    private fun isDesktopUaRequested(
        config: WebViewConfig?,
        currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
    ): Boolean {
        val cfg = config ?: return false
        return cfg.desktopMode ||
            cfg.userAgentMode in setOf(
                UserAgentMode.CHROME_DESKTOP,
                UserAgentMode.SAFARI_DESKTOP,
                UserAgentMode.FIREFOX_DESKTOP,
                UserAgentMode.EDGE_DESKTOP
            ) ||
            (currentDeviceDisguiseConfig?.requiresDesktopViewport() == true)
    }

    private fun applyRequestedWithHeaderAllowListForStrictHost(settings: WebSettings) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) return
        runCatching {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
            AppLogger.d("WebViewManager", "Strict host policy: X-Requested-With header disabled")
        }.onFailure { error ->
            AppLogger.w("WebViewManager", "Failed to disable X-Requested-With header allow-list", error)
        }
    }
}
