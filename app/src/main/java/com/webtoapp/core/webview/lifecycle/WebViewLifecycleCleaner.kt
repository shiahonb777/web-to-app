package com.webtoapp.core.webview

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.webtoapp.core.logging.AppLogger

internal class WebViewLifecycleCleaner(
    private val state: WebViewSessionState
) {
    fun destroyWebView(webView: WebView) {
        try {
            state.managedWebViews.remove(webView)

            webView.apply {
                stopLoading()
                clearHistory()
                webChromeClient = null
                webViewClient = object : WebViewClient() {}
                removeJavascriptInterface("NativeBridge")
                removeJavascriptInterface("DownloadBridge")
                removeJavascriptInterface("NativeShareBridge")
                removeJavascriptInterface(com.webtoapp.core.extension.GreasemonkeyBridge.JS_INTERFACE_NAME)
                removeJavascriptInterface(com.webtoapp.core.extension.ChromeExtensionRuntime.JS_BRIDGE_NAME)
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }

            AppLogger.d("WebViewManager", "WebView resources cleaned up")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to cleanup WebView", e)
        }
    }

    fun destroyAll() {
        state.managedWebViews.keys.toList().forEach(::destroyWebView)
        state.managedWebViews.clear()
        state.gmBridge?.destroy()
        state.gmBridge = null
        state.extensionRuntimes.values.forEach { it.destroy() }
        state.extensionRuntimes.clear()
    }
}
