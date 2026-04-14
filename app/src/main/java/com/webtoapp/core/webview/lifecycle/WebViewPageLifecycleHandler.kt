package com.webtoapp.core.webview

import android.graphics.Bitmap
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.errorpage.ErrorPageManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig

internal class WebViewPageLifecycleHandler(
    private val state: WebViewManagerRuntimeState,
    private val adBlocker: AdBlocker,
    private val strictHostRuntimePolicy: StrictHostRuntimePolicy,
    private val scriptInjectionCoordinator: ScriptInjectionCoordinator,
    private val webViewLifecycleCleaner: WebViewLifecycleCleaner,
    private val specialUrlHandler: SpecialUrlHandler,
    private val getErrorPageManager: () -> ErrorPageManager?,
    private val getShields: () -> BrowserShields?,
    private val getCachedBrowserDisguiseConfig: () -> com.webtoapp.core.disguise.BrowserDisguiseConfig?,
    private val getCachedBrowserDisguiseJs: () -> String?,
    private val currentDeviceDisguiseConfig: () -> com.webtoapp.core.disguise.DeviceDisguiseConfig?,
    private val cookieFlushRunnable: Runnable,
    private val urlPolicy: WebViewUrlPolicy,
    private val fileMaxRetries: Int,
    private val fileRetryDelayMs: Long
) {
    fun onPageStarted(
        view: WebView?,
        url: String?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics,
        favicon: Bitmap? = null
    ) {
        state.currentMainFrameUrl = url
        diag.pageStartTime = System.currentTimeMillis()
        diag.requestCount = 0
        diag.blockedCount = 0
        diag.errorCount = 0
        android.util.Log.w("DIAG", "═══ PAGE_STARTED ═══ url=$url")
        android.util.Log.w(
            "DIAG",
            "  config: disableShields=${config.disableShields} adBlockEnabled=${adBlocker.isEnabled()} crossOriginIsolation=${config.enableCrossOriginIsolation}"
        )
        val shields = getShields()
        android.util.Log.w(
            "DIAG",
            "  shields: initialized=${shields != null} enabled=${shields?.isEnabled()?.toString() ?: "N/A"}"
        )
        if (shields?.isEnabled() == true) {
            android.util.Log.w(
                "DIAG",
                "  shields config: trackerBlocking=${shields.getConfig().trackerBlocking} httpsUpgrade=${shields.getConfig().httpsUpgrade}"
            )
        }

        if (url != null) {
            OAuthCompatEngine.getAntiDetectionJs(url)?.let { js ->
                val provider = OAuthCompatEngine.getProviderType(url)
                AppLogger.d("WebViewManager", "Injecting OAuth anti-detection JS [$provider] for: $url")
                view?.evaluateJavascript(js, null)
            }
        }

        if (view != null) {
            strictHostRuntimePolicy.applyStrictHostRuntimePolicy(
                webView = view,
                pageUrl = url,
                currentConfig = state.currentConfig,
                currentDeviceDisguiseConfig = currentDeviceDisguiseConfig()
            )
        }
        callbacks.onPageStarted(url)
        state.lastFailedUrl = null
        if (url != null && url != state.fileRetryUrl) {
            state.fileRetryCount = 0
            state.fileRetryUrl = null
        }
        shields?.onPageStarted(url)
        adBlocker.invalidateCache()
        view?.let { webView ->
            com.webtoapp.core.disguise.BrowserDisguiseEngine.injectOnPageStarted(
                webView = webView,
                url = url,
                disguiseConfig = getCachedBrowserDisguiseConfig(),
                cachedDisguiseJs = getCachedBrowserDisguiseJs(),
                enableDiagnostic = false
            )
            scriptInjectionCoordinator.handlePageStarted(webView, url, config)
        }
    }

    fun onPageCommitVisible(
        url: String?,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics
    ) {
        state.currentMainFrameUrl = url ?: state.currentMainFrameUrl
        val elapsed = System.currentTimeMillis() - diag.pageStartTime
        android.util.Log.w(
            "DIAG",
            "═══ PAGE_COMMIT_VISIBLE ═══ +${elapsed}ms requests=${diag.requestCount} blocked=${diag.blockedCount} url=$url"
        )
        callbacks.onPageCommitVisible(url)
    }

    fun onPageFinished(
        view: WebView?,
        url: String?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics
    ) {
        state.currentMainFrameUrl = url ?: state.currentMainFrameUrl
        val elapsed = System.currentTimeMillis() - diag.pageStartTime
        android.util.Log.w(
            "DIAG",
            "═══ PAGE_FINISHED ═══ +${elapsed}ms requests=${diag.requestCount} blocked=${diag.blockedCount} errors=${diag.errorCount} url=$url"
        )
        if (url != null && url.startsWith("file://")) {
            state.fileRetryCount = 0
            state.fileRetryUrl = null
        }
        view?.let { webView ->
            scriptInjectionCoordinator.handlePageFinished(
                webView = webView,
                url = url,
                config = config,
                cookieFlushRunnable = cookieFlushRunnable,
                extractHostFromUrl = urlPolicy::extractHostFromUrl,
                adBlockerCssProvider = adBlocker::getCosmeticFilterCss
            )
        }
        callbacks.onPageFinished(url)
        getShields()?.onPageFinished(url)
    }

    fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics
    ) {
        val errUrl = request?.url?.toString() ?: "unknown"
        val errCode = error?.errorCode ?: -1
        val errDesc = error?.description?.toString() ?: "unknown"
        val isMain = request?.isForMainFrame == true
        diag.errorCount++
        android.util.Log.w(
            "DIAG",
            "RECV_ERROR [${if (isMain) "MAIN" else "sub"}] code=$errCode desc=$errDesc url=${errUrl.take(120)}"
        )
        if (request?.isForMainFrame != true) return

        val errorCode = error?.errorCode ?: -1
        val rawDescription = error?.description?.toString() ?: "Unknown error"
        val description = normalizeNetworkErrorDescription(rawDescription)
        val failedUrl = request.url?.toString()
        if (failedUrl == null || failedUrl == "about:blank") return

        if (view != null) {
            val upgradedUrl = urlPolicy.upgradeInsecureHttpUrl(failedUrl)
            if (upgradedUrl != null && isCleartextBlockedError(errorCode, rawDescription, description)) {
                AppLogger.d("WebViewManager", "Auto-recover from cleartext block: $failedUrl -> $upgradedUrl")
                view.loadUrl(upgradedUrl)
                return
            }
        }

        if (view != null && failedUrl.startsWith("file://")) {
            val isSameRetry = failedUrl == state.fileRetryUrl
            val currentRetry = if (isSameRetry) state.fileRetryCount else 0
            if (currentRetry < fileMaxRetries) {
                state.fileRetryUrl = failedUrl
                state.fileRetryCount = currentRetry + 1
                AppLogger.d(
                    "WebViewManager",
                    "file:// load failed (code=$errorCode, desc=$rawDescription), auto-retry ${state.fileRetryCount}/$fileMaxRetries after ${fileRetryDelayMs}ms: $failedUrl"
                )
                view.postDelayed({ view.loadUrl(failedUrl) }, fileRetryDelayMs)
                return
            } else {
                AppLogger.w("WebViewManager", "file:// load failed after $fileMaxRetries retries: $failedUrl")
                state.fileRetryCount = 0
                state.fileRetryUrl = null
            }
        }

        val manager = getErrorPageManager()
        if (manager != null && view != null) {
            val errorHtml = manager.generateErrorPage(errorCode, description, failedUrl)
            if (errorHtml != null) {
                state.lastFailedUrl = failedUrl
                view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", failedUrl)
                AppLogger.d("WebViewManager", "Custom error page loaded for: $failedUrl")
                callbacks.onError(errorCode, description)
                return
            }
        }

        callbacks.onError(errorCode, description)
    }

    fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
        callbacks: WebViewCallbacks
    ) {
        if (request?.isForMainFrame != true) return

        val statusCode = errorResponse?.statusCode ?: -1
        val reason = errorResponse?.reasonPhrase?.takeIf { it.isNotBlank() } ?: "HTTP Error"
        val failedUrl = request.url?.toString()
        val description = if (statusCode > 0) "HTTP $statusCode $reason" else reason
        AppLogger.w("WebViewManager", "Main-frame HTTP error: url=$failedUrl code=$statusCode reason=$reason")

        if (failedUrl != null && OAuthCompatEngine.isOAuthBlockedError(statusCode, failedUrl)) {
            val provider = OAuthCompatEngine.getProviderType(failedUrl)
            AppLogger.w(
                "WebViewManager",
                "OAuth [$provider] $statusCode detected — kernel disguise insufficient, falling back to system browser: $failedUrl"
            )
            view?.stopLoading()
            if (view?.canGoBack() == true) {
                view.goBack()
            }
            specialUrlHandler.openInSystemBrowser(failedUrl)
            return
        }

        val manager = getErrorPageManager()
        if (manager != null && view != null && failedUrl != null && failedUrl != "about:blank") {
            val errorHtml = manager.generateErrorPage(statusCode, description, failedUrl)
            if (errorHtml != null) {
                state.lastFailedUrl = failedUrl
                view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", failedUrl)
                AppLogger.d("WebViewManager", "Custom HTTP error page loaded for: $failedUrl, code=$statusCode")
                callbacks.onError(statusCode, description)
                return
            }
        }

        callbacks.onError(statusCode, description)
    }

    fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: android.net.http.SslError?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ) {
        val shields = getShields()
        if (!config.disableShields && shields?.isEnabled() == true) {
            when (shields.getConfig().sslErrorPolicy) {
                com.webtoapp.core.engine.shields.SslErrorPolicy.AUTO_HTTP_FALLBACK -> {
                    var fallbackUrl = shields.httpsUpgrader.onSslError(error?.url)
                    if (fallbackUrl != null) {
                        handler?.cancel()
                        view?.loadUrl(fallbackUrl)
                        AppLogger.d("WebViewManager", "HTTPS upgrade fallback: $fallbackUrl")
                        return
                    }
                    fallbackUrl = shields.httpsUpgrader.tryHttpFallback(error?.url)
                    if (fallbackUrl != null) {
                        handler?.cancel()
                        view?.loadUrl(fallbackUrl)
                        AppLogger.d("WebViewManager", "SSL error fallback to HTTP: $fallbackUrl")
                        return
                    }
                    handler?.cancel()
                    callbacks.onSslError(error?.toString() ?: "SSL Error")
                    return
                }

                com.webtoapp.core.engine.shields.SslErrorPolicy.ASK_USER,
                com.webtoapp.core.engine.shields.SslErrorPolicy.BLOCK -> {
                    handler?.cancel()
                    callbacks.onSslError(error?.toString() ?: "SSL Error")
                    return
                }
            }
        }

        handler?.cancel()
        callbacks.onSslError(error?.toString() ?: "SSL Error")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?,
        callbacks: WebViewCallbacks
    ): Boolean {
        val didCrash = detail?.didCrash() == true
        val reason = if (didCrash) {
            "WebView render process crashed"
        } else {
            "WebView render process was killed"
        }
        AppLogger.e("WebViewManager", "$reason, rendererPriority=${detail?.rendererPriorityAtExit()}")
        view?.let(webViewLifecycleCleaner::destroyWebView)
        callbacks.onError(
            -1003,
            if (didCrash) {
                "WebView render process crashed. Please reopen the page."
            } else {
                "WebView render process was killed due to memory pressure. Please reopen the page."
            }
        )
        callbacks.onRenderProcessGone(didCrash)
        return true
    }

    private fun normalizeNetworkErrorDescription(rawDescription: String): String {
        val normalized = rawDescription.uppercase()
        if (normalized.contains("CLEARTEXT") || normalized.contains("ERR_CLEARTEXT_NOT_PERMITTED")) {
            return "Cleartext HTTP is blocked by security policy. Please use HTTPS."
        }
        return rawDescription
    }

    private fun isCleartextBlockedError(
        errorCode: Int,
        rawDescription: String,
        normalizedDescription: String
    ): Boolean {
        if (errorCode == WebViewClient.ERROR_UNSAFE_RESOURCE) return true
        val merged = "$rawDescription $normalizedDescription".uppercase()
        return merged.contains("CLEARTEXT") ||
            merged.contains("ERR_CLEARTEXT_NOT_PERMITTED") ||
            merged.contains("SECURITY POLICY")
    }
}
