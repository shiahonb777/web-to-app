package com.webtoapp.core.webview

import android.webkit.WebView
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig

internal class WebViewCompatibilityScriptCoordinator(
    adBlocker: AdBlocker,
    urlPolicy: WebViewUrlPolicy,
    getShields: () -> BrowserShields?
) {
    private val polyfillScriptProvider = WebViewPolyfillScriptProvider()
    private val interactionScriptProvider = WebViewInteractionScriptProvider()
    private val shieldScriptAppender = WebViewShieldScriptAppender(
        adBlocker = adBlocker,
        urlPolicy = urlPolicy,
        getShields = getShields
    )

    fun injectCompatibilityScripts(
        webView: WebView,
        pageUrl: String?,
        config: WebViewConfig,
        conservativeMode: Boolean
    ) {
        try {
            val scripts = buildList {
                if (conservativeMode) {
                    AppLogger.d("WebViewManager", "Compatibility safe mode enabled for remote page: $pageUrl")
                }

                polyfillScriptProvider.zoomPolyfillScript(config, conservativeMode)?.let(::add)
                polyfillScriptProvider.shareBridgeScript(config, conservativeMode)?.let(::add)
                polyfillScriptProvider.clipboardPolyfillScript(conservativeMode)?.let(::add)
                interactionScriptProvider.hideLinkPreviewScript(conservativeMode)?.let(::add)
                interactionScriptProvider.popupBlockerScript(config, conservativeMode)?.let(::add)
                add(polyfillScriptProvider.otherCompatibilityFixesScript())
            }.toMutableList()

            shieldScriptAppender.appendShieldAndAdBlockScripts(scripts, pageUrl, config, conservativeMode)

            webView.evaluateJavascript(scripts.joinToString("\n\n"), null)
            AppLogger.d("WebViewManager", "Browser compatibility scripts injected")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Compatibility script injection failed", e)
        }
    }
}
