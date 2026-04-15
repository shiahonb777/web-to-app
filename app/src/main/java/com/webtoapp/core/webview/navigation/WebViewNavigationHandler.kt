package com.webtoapp.core.webview

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig

internal class WebViewNavigationHandler(
    private val urlPolicy: WebViewUrlPolicy,
    private val strictHostRuntimePolicy: StrictHostRuntimePolicy,
    private val specialUrlHandler: SpecialUrlHandler,
    private val getCurrentMainFrameUrl: () -> String?,
    private val getCurrentConfig: () -> WebViewConfig?,
    private val getManagedWebViews: () -> Collection<WebView>,
    private val getShields: () -> BrowserShields?
) {
    fun handleShouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        val isUserGesture = request.hasGesture()
        if (request.isForMainFrame) {
            AppLogger.d("WebViewManager", "Main-frame navigation request: $url")
        }

        if (request.isForMainFrame && OAuthCompatEngine.shouldRedirectToCustomTab(url)) {
            val provider = OAuthCompatEngine.getProviderType(url)
            AppLogger.i("WebViewManager", "Google OAuth detected [$provider] — redirecting to Chrome Custom Tab: $url")
            view?.stopLoading()
            specialUrlHandler.openInCustomTab(url)
            return true
        }

        if (request.isForMainFrame && OAuthCompatEngine.isOAuthUrl(url)) {
            val provider = OAuthCompatEngine.getProviderType(url)
            AppLogger.d("WebViewManager", "OAuth detected [$provider] — allowing in-WebView with kernel disguise: $url")
        }

        val currentMainFrameUrl = getCurrentMainFrameUrl()
        val isSameOriginHttp = isSameOriginHttpNavigation(currentMainFrameUrl, url)
        if (!isSameOriginHttp && !config.disableShields) {
            val secureUrl = urlPolicy.upgradeInsecureHttpUrl(url)
            if (secureUrl != null) {
                view?.loadUrl(secureUrl)
                AppLogger.d("WebViewManager", "Auto-upgraded insecure HTTP navigation: $url -> $secureUrl")
                return true
            }
        }

        val shields = getShields()
        if (!config.disableShields && shields?.isEnabled() == true && shields.getConfig().httpsUpgrade) {
            val upgradedUrl = shields.httpsUpgrader.tryUpgrade(url)
            if (upgradedUrl != null) {
                shields.stats.recordHttpsUpgrade()
                view?.loadUrl(upgradedUrl)
                return true
            }
        }

        if (specialUrlHandler.handleSpecialUrl(
                url = url,
                isUserGesture = isUserGesture,
                currentMainFrameUrl = currentMainFrameUrl,
                currentConfig = getCurrentConfig(),
                managedWebViews = getManagedWebViews(),
                shouldUseScriptlessMode = strictHostRuntimePolicy::shouldUseScriptlessMode
            )
        ) {
            return true
        }

        if (config.openExternalLinks && isExternalUrl(url, view?.url)) {
            callbacks.onExternalLink(url)
            return true
        }

        return false
    }

    fun applyPreloadPolicyForUrl(
        webView: WebView,
        pageUrl: String?,
        currentConfig: WebViewConfig?,
        currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
    ) {
        strictHostRuntimePolicy.applyPreloadPolicyForUrl(
            webView = webView,
            pageUrl = pageUrl,
            currentConfig = currentConfig,
            currentDeviceDisguiseConfig = currentDeviceDisguiseConfig
        )
    }

    fun isExternalUrl(targetUrl: String, currentUrl: String?): Boolean {
        if (currentUrl == null) return false
        val targetHost = runCatching { Uri.parse(targetUrl).host?.lowercase() }.getOrNull() ?: return false
        val currentHost = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull() ?: return false
        return !targetHost.endsWith(currentHost) && !currentHost.endsWith(targetHost)
    }

    private fun isSameOriginHttpNavigation(currentUrl: String?, targetUrl: String): Boolean {
        if (currentUrl == null || !currentUrl.startsWith("http://", ignoreCase = true)) return false
        val currentHost = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull()
        val targetHost = runCatching { Uri.parse(targetUrl).host?.lowercase() }.getOrNull()
        return currentHost != null && targetHost != null && currentHost == targetHost
    }
}
