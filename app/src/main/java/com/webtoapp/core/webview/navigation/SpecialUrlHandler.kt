package com.webtoapp.core.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.NewWindowBehavior
import com.webtoapp.data.model.WebViewConfig

internal class SpecialUrlHandler(
    private val context: Context,
    private val urlPolicy: WebViewUrlPolicy
) {
    fun handleSpecialUrl(
        url: String,
        isUserGesture: Boolean,
        currentMainFrameUrl: String?,
        currentConfig: WebViewConfig?,
        managedWebViews: Collection<WebView>,
        shouldUseScriptlessMode: (String?) -> Boolean
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

        if (!isUserGesture && (shouldUseScriptlessMode(currentMainFrameUrl ?: url) || isBackgroundBridgeScheme(uri))) {
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
                "intent" -> buildIntentScheme(url, paymentSchemesEnabled)
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
                } catch (e: ActivityNotFoundException) {
                    AppLogger.w("WebViewManager", "No activity found for intent", e)
                    if (!fallbackUrl.isNullOrEmpty()) {
                        AppLogger.d("WebViewManager", "Using fallback URL: $fallbackUrl")
                        managedWebViews.firstOrNull()?.loadUrl(fallbackUrl)
                    }
                    return true
                } catch (e: SecurityException) {
                    AppLogger.e("WebViewManager", "Security exception launching intent", e)
                    if (!fallbackUrl.isNullOrEmpty()) {
                        AppLogger.d("WebViewManager", "Using fallback URL after security error: $fallbackUrl")
                        managedWebViews.firstOrNull()?.loadUrl(fallbackUrl)
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

    fun openInSystemBrowser(url: String) {
        try {
            val safeUrl = urlPolicy.normalizeHttpUrlForSecurity(url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to open system browser for OAuth: $url", e)
        }
    }

    fun openInCustomTab(url: String) {
        try {
            val safeUrl = urlPolicy.normalizeHttpUrlForSecurity(url)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, Uri.parse(safeUrl))
            AppLogger.i("WebViewManager", "Opened OAuth URL in Chrome Custom Tab: ${url.take(80)}")
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "Chrome Custom Tab failed, falling back to system browser", e)
            openInSystemBrowser(url)
        }
    }

    fun handleNewWindowRequest(
        view: WebView,
        resultMsg: Message?,
        behavior: NewWindowBehavior,
        href: String?,
        onPopupWindow: (Message?) -> Unit
    ): Boolean {
        AppLogger.d("WebViewManager", "onCreateWindow: href=$href, behavior=$behavior")
        return when (behavior) {
            NewWindowBehavior.SAME_WINDOW -> {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                transport.webView = createTempWebView { url ->
                    view.loadUrl(urlPolicy.normalizeHttpUrlForSecurity(url))
                }
                resultMsg.sendToTarget()
                true
            }

            NewWindowBehavior.EXTERNAL_BROWSER -> {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                transport.webView = createTempWebView(::openInSystemBrowser)
                resultMsg.sendToTarget()
                true
            }

            NewWindowBehavior.POPUP_WINDOW -> {
                onPopupWindow(resultMsg)
                true
            }

            NewWindowBehavior.BLOCK -> false
        }
    }

    private fun buildIntentScheme(url: String, paymentSchemesEnabled: Boolean): Intent? {
        return try {
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

    private fun createTempWebView(onResolvedUrl: (String) -> Unit): WebView {
        return WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return true
                    runCatching { onResolvedUrl(url) }
                        .onFailure { error ->
                            AppLogger.e("WebViewManager", "Failed to handle popup url: $url", error)
                        }
                    tempView?.destroy()
                    return true
                }
            }
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
