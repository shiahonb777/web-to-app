package com.webtoapp.core.webview

import com.webtoapp.data.model.WebViewConfig

internal class WebViewPolyfillScriptProvider {
    private val zoomPolyfillScriptProvider = WebViewZoomPolyfillScriptProvider()
    private val bridgePolyfillScriptProvider = WebViewBridgePolyfillScriptProvider()
    private val miscCompatibilityScriptProvider = WebViewMiscCompatibilityScriptProvider()

    fun zoomPolyfillScript(config: WebViewConfig, conservativeMode: Boolean): String? {
        return zoomPolyfillScriptProvider.zoomPolyfillScript(config, conservativeMode)
    }

    fun shareBridgeScript(config: WebViewConfig, conservativeMode: Boolean): String? {
        return bridgePolyfillScriptProvider.shareBridgeScript(config, conservativeMode)
    }

    fun clipboardPolyfillScript(conservativeMode: Boolean): String? {
        return bridgePolyfillScriptProvider.clipboardPolyfillScript(conservativeMode)
    }

    fun otherCompatibilityFixesScript(): String {
        return miscCompatibilityScriptProvider.otherCompatibilityFixesScript()
    }
}
