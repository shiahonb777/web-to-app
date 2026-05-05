package com.webtoapp.core.engine

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig


data class ProxyConfig(
    val mode: String = "NONE",
    val host: String = "",
    val port: Int = 0,
    val type: String = "HTTP",
    val pacUrl: String = "",
    val username: String = "",
    val password: String = ""
)


class GeckoViewEngine(
    private val context: Context
) : BrowserEngine {

    companion object {
        private const val TAG = "GeckoViewEngine"

        fun applyDnsConfig(config: com.webtoapp.data.model.DnsConfig) {
            AppLogger.d(TAG, "GeckoView is not bundled in this build; DNS config ignored for GeckoView")
        }

        fun applyProxyConfig(config: ProxyConfig) {
            AppLogger.d(TAG, "GeckoView is not bundled in this build; proxy config ignored for GeckoView")
        }
    }

    override val engineType = EngineType.GECKOVIEW

    private var view: FrameLayout? = null

    override fun createView(
        context: Context,
        config: WebViewConfig,
        callback: BrowserEngineCallback
    ): View {
        callback.onError(-1, "GeckoView is not bundled in this build")
        return FrameLayout(context).also { view = it }
    }

    override fun loadUrl(url: String) {}

    override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {
        resultCallback?.invoke(null)
    }

    override fun canGoBack(): Boolean = false

    override fun goBack() {}

    override fun canGoForward(): Boolean = false

    override fun goForward() {}

    override fun reload() {}

    override fun stopLoading() {}

    override fun getCurrentUrl(): String? = null

    override fun getTitle(): String? = null

    override fun getView(): View? = view

    override fun destroy() {
        view = null
    }

    override fun clearCache(includeDiskFiles: Boolean) {}

    override fun clearHistory() {}
}
