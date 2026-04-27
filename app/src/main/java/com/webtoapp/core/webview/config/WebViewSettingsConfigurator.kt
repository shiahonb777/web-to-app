package com.webtoapp.core.webview

import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.NewWindowBehavior
import com.webtoapp.data.model.WebViewConfig

internal class WebViewSettingsConfigurator {
    fun apply(
        webView: WebView,
        config: WebViewConfig,
        effectiveUserAgent: String?,
        isDesktopModeRequested: Boolean,
        preferLandscapeEmbeddedViewport: Boolean,
        hasActiveChromeExtension: Boolean,
        desktopUserAgent: String
    ) {
        webView.settings.apply {
            javaScriptEnabled = config.javaScriptEnabled
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = config.domStorageEnabled
            databaseEnabled = true
            allowFileAccess = config.allowFileAccess
            allowContentAccess = config.allowContentAccess
            cacheMode = if (config.cacheEnabled) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
            setSupportZoom(config.zoomEnabled)
            builtInZoomControls = config.zoomEnabled
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = !preferLandscapeEmbeddedViewport

            if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
                useWideViewPort = true
                loadWithOverviewMode = true
                AppLogger.d("WebViewManager", "ViewportMode.FIT_SCREEN applied: overview fit + JS adaptation")
            } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.DESKTOP) {
                useWideViewPort = true
                loadWithOverviewMode = true
                textZoom = 100
                AppLogger.d("WebViewManager", "ViewportMode.DESKTOP applied")
            } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
                useWideViewPort = true
                loadWithOverviewMode = true
                AppLogger.d("WebViewManager", "ViewportMode.CUSTOM applied: width=${config.customViewportWidth}")
            }

            if (effectiveUserAgent != null) {
                userAgentString = effectiveUserAgent
                AppLogger.d("WebViewManager", "User-Agent set: ${effectiveUserAgent.take(80)}...")
            }

            if (!isDesktopModeRequested && effectiveUserAgent == null && hasActiveChromeExtension) {
                userAgentString = desktopUserAgent
                AppLogger.d("WebViewManager", "Desktop UA auto-enabled for active Chrome extension(s)")
            }

            if (isDesktopModeRequested) {
                useWideViewPort = true
                loadWithOverviewMode = true
                textZoom = 100
            } else if (preferLandscapeEmbeddedViewport) {
                AppLogger.d(
                    "WebViewManager",
                    "Landscape viewport policy applied: disable overview shrink-fit (loadWithOverviewMode=false)"
                )
            }

            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
            @Suppress("DEPRECATION")
            setGeolocationEnabled(true)
            @Suppress("DEPRECATION")
            setGeolocationDatabasePath(webView.context.filesDir.absolutePath)
            allowFileAccessFromFileURLs = config.allowFileAccessFromFileURLs
            allowUniversalAccessFromFileURLs = config.allowUniversalAccessFromFileURLs

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        webView.isScrollbarFadingEnabled = true
        webView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        com.webtoapp.core.perf.NativePerfEngine.optimizeWebViewSettings(webView)

        if (config.initialScale > 0) {
            webView.setInitialScale(config.initialScale)
            AppLogger.d("WebViewManager", "Set initial scale: ${config.initialScale}%")
        } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
            webView.setInitialScale(1)
            AppLogger.d("WebViewManager", "FIT_SCREEN: forced initial scale to 1 (override DPI scaling)")
        } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
            webView.setInitialScale(1)
            AppLogger.d("WebViewManager", "ViewportMode.CUSTOM: forced initial scale to 1 (custom width=${config.customViewportWidth})")
        }

        webView.settings.setSupportMultipleWindows(config.newWindowBehavior != NewWindowBehavior.SAME_WINDOW)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
    }
}
