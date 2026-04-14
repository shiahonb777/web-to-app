package com.webtoapp.core.engine

import android.content.Context
import android.view.View
import com.webtoapp.data.model.WebViewConfig

/**
 * Note.
 * SystemWebView GeckoView.
 */
interface BrowserEngine {

    /** Note. */
    val engineType: EngineType

    /**
     * config.
     * @param context Android Context
     * config.
     * param callback.
     * config.
     */
    fun createView(
        context: Context,
        config: WebViewConfig,
        callback: BrowserEngineCallback
    ): View

    /**
     * load.
     * load.
     */
    fun loadUrl(url: String)

    /**
     * JavaScript.
     * param script JS.
     * param resultCallback.
     */
    fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)? = null)

    /**
     * Note.
     */
    fun canGoBack(): Boolean

    /**
     * Note.
     */
    fun goBack()

    /**
     * Note.
     */
    fun canGoForward(): Boolean

    /**
     * Note.
     */
    fun goForward()

    /**
     * load.
     */
    fun reload()

    /**
     * load stop.
     */
    fun stopLoading()

    /**
     * URL.
     */
    fun getCurrentUrl(): String?

    /**
     * page.
     */
    fun getTitle(): String?

    /**
     * View.
     */
    fun getView(): View?

    /**
     * Note.
     */
    fun requestFocus() {
        getView()?.requestFocus()
    }

    /**
     * asset.
     */
    fun destroy()

    /**
     * cache.
     * cache.
     */
    fun clearCache(includeDiskFiles: Boolean = true)

    /**
     * Note.
     */
    fun clearHistory()

    /**
     * protection.
     * return BrowserShields null.
     */
    fun getShields(): com.webtoapp.core.engine.shields.BrowserShields? = null

    /**
     * Note.
     * param enabled.
     */
    fun toggleReaderMode(enabled: Boolean) {}
}