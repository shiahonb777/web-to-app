package com.webtoapp.core.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig

internal class WebViewNavigationHandler(
    private val context: Context,
    private val urlPolicy: WebViewUrlPolicy
) {
    fun applyPreloadPolicyForUrl(
        webView: WebView,
        pageUrl: String?,
        currentConfig: WebViewConfig?,
        currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
    ) {
        resetStrictHostSessionState(webView, pageUrl)
        applyStrictHostRuntimePolicy(webView, pageUrl, currentConfig, currentDeviceDisguiseConfig)
    }

    fun applyStrictHostRuntimePolicy(
        webView: WebView,
        pageUrl: String?,
        currentConfig: WebViewConfig?,
        currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
    ) {
        if (!urlPolicy.shouldUseScriptlessMode(pageUrl)) return

        val settings = webView.settings
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.removeJavascriptInterface("NativeShareBridge")
        webView.removeJavascriptInterface("AndroidDownload")
        webView.removeJavascriptInterface("NativeBridge")
        applyRequestedWithHeaderAllowListForStrictHost(settings)

        val desktopRequested = UserAgentResolver(context).isDesktopUaRequested(currentConfig, currentDeviceDisguiseConfig)
        val strictMobileUA = WebViewManager.STRICT_COMPAT_MOBILE_USER_AGENT ?: WebViewManager.STRICT_COMPAT_MOBILE_UA_FALLBACK
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

    fun shouldBypassAggressiveNetworkHooks(request: android.webkit.WebResourceRequest, requestUrl: String): Boolean {
        return request.isForMainFrame && urlPolicy.shouldUseScriptlessMode(requestUrl)
    }

    fun handleSpecialUrl(
        url: String,
        isUserGesture: Boolean,
        currentMainFrameUrl: String?,
        currentConfig: WebViewConfig?,
        managedWebViews: Collection<WebView>
    ): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false

        if (scheme == "http" || scheme == "https") {
            return false
        }

        if (scheme == "file") {
            val currentUrl = currentMainFrameUrl
            if (currentUrl != null && currentUrl.startsWith("file://")) {
                AppLogger.d("WebViewManager", "Allowing file:// same-origin navigation: $url")
                return false
            }
        }

        if (scheme in WebViewManager.BLOCKED_SPECIAL_SCHEMES) {
            AppLogger.w("WebViewManager", "Blocked dangerous scheme navigation: $scheme")
            return true
        }

        if (!isUserGesture &&
            (urlPolicy.shouldUseScriptlessMode(currentMainFrameUrl ?: url) || isBackgroundBridgeScheme(uri))) {
            AppLogger.d("WebViewManager", "Ignore non-user special scheme in strict mode: $url")
            return true
        }

        val paymentSchemesEnabled = currentConfig?.enablePaymentSchemes ?: true
        if (!paymentSchemesEnabled && scheme in WebViewManager.PAYMENT_SCHEMES) {
            AppLogger.w("WebViewManager", "Payment scheme blocked by config: $scheme")
            return true
        }

        AppLogger.d("WebViewManager", "Handling special URL: $url (scheme=$scheme)")

        return try {
            val intent = when (scheme) {
                "intent" -> {
                    try {
                        val parsedIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        val targetScheme = parsedIntent.data?.scheme?.lowercase()
                        if (targetScheme in WebViewManager.BLOCKED_SPECIAL_SCHEMES) {
                            AppLogger.w("WebViewManager", "Blocked dangerous target scheme in intent:// URL: $targetScheme")
                            null
                        } else if (!paymentSchemesEnabled && targetScheme in WebViewManager.PAYMENT_SCHEMES) {
                            AppLogger.w("WebViewManager", "Payment target scheme blocked by config in intent:// URL: $targetScheme")
                            null
                        } else {
                            parsedIntent.apply {
                                dataString?.let { original ->
                                    val safeUrl = urlPolicy.normalizeHttpUrlForSecurity(original)
                                    if (safeUrl != original) {
                                        data = Uri.parse(safeUrl)
                                    }
                                }
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                selector?.addCategory(Intent.CATEGORY_BROWSABLE)
                            }
                        }
                    } catch (e: java.net.URISyntaxException) {
                        AppLogger.e("WebViewManager", "Invalid intent URI: $url", e)
                        null
                    }
                }
                else -> Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            if (intent != null) {
                val fallbackUrl = if (scheme == "intent") {
                    sanitizeFallbackUrl(intent.getStringExtra("browser_fallback_url"))
                } else {
                    null
                }

                try {
                    val resolveInfo = context.packageManager.resolveActivity(
                        intent,
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )

                    if (resolveInfo != null) {
                        AppLogger.d("WebViewManager", "Resolved activity: ${resolveInfo.activityInfo?.packageName}")
                        context.startActivity(intent)
                        return true
                    }

                    AppLogger.d("WebViewManager", "resolveActivity returned null, trying direct launch")
                    context.startActivity(intent)
                    return true
                } catch (e: android.content.ActivityNotFoundException) {
                    AppLogger.w("WebViewManager", "No activity found for intent", e)
                    if (!fallbackUrl.isNullOrEmpty()) {
                        AppLogger.d("WebViewManager", "Using fallback URL: $fallbackUrl")
                        managedWebViews.firstOrNull()?.loadUrl(fallbackUrl)
                        return true
                    }
                    return true
                } catch (e: SecurityException) {
                    AppLogger.e("WebViewManager", "Security exception launching intent", e)
                    if (!fallbackUrl.isNullOrEmpty()) {
                        AppLogger.d("WebViewManager", "Using fallback URL after security error: $fallbackUrl")
                        managedWebViews.firstOrNull()?.loadUrl(fallbackUrl)
                        return true
                    }
                    return true
                }
            }

            true
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "Error handling special URL: $scheme", e)
            true
        }
    }

    fun isExternalUrl(targetUrl: String, currentUrl: String?): Boolean {
        if (currentUrl == null) return false
        val targetHost = runCatching { Uri.parse(targetUrl).host?.lowercase() }.getOrNull() ?: return false
        val currentHost = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull() ?: return false
        return !targetHost.endsWith(currentHost) && !currentHost.endsWith(targetHost)
    }

    fun openInSystemBrowser(url: String) {
        try {
            val safeUrl = urlPolicy.normalizeHttpUrlForSecurity(url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Cannot open system browser: $url", e)
        }
    }

    fun openInCustomTab(url: String) {
        try {
            val safeUrl = urlPolicy.normalizeHttpUrlForSecurity(url)
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(safeUrl))
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "CustomTab launch failed, fallback to system browser: $url", e)
            openInSystemBrowser(url)
        }
    }

    private fun resetStrictHostSessionState(webView: WebView, pageUrl: String?) {
        if (!urlPolicy.shouldUseScriptlessMode(pageUrl)) return

        webView.clearCache(true)
        webView.clearHistory()

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()

        val origins = buildStrictHostOrigins(pageUrl)
        if (origins.isNotEmpty()) {
            val webStorage = WebStorage.getInstance()
            origins.forEach { origin -> webStorage.deleteOrigin(origin) }
        }

        AppLogger.d("WebViewManager", "Strict host session reset applied for $pageUrl")
    }

    private fun buildStrictHostOrigins(pageUrl: String?): Set<String> {
        val host = urlPolicy.extractHostFromUrl(pageUrl) ?: return emptySet()
        val baseHost = host.removePrefix("www.")
        val hosts = linkedSetOf(host, baseHost, "www.$baseHost")
        return hosts
            .filter { it.isNotBlank() }
            .flatMap { targetHost -> listOf("https://$targetHost", "http://$targetHost") }
            .toSet()
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

    private fun isBackgroundBridgeScheme(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()
        if (scheme !in setOf("bytedance", "snssdk", "douyin")) return false
        return host == "dispatch_message" || path.contains("dispatch_message")
    }

    private fun sanitizeFallbackUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            AppLogger.w("WebViewManager", "Ignoring non-http(s) fallback URL in intent:// payload")
            return null
        }
        return urlPolicy.normalizeHttpUrlForSecurity(trimmed)
    }
}
