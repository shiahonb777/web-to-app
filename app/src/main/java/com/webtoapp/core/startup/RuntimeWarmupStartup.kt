package com.webtoapp.core.startup

import android.webkit.WebView
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.core.logging.AppLogger

class RuntimeWarmupStartup(
    private val billingManager: BillingManager,
    private val appContext: android.content.Context,
) {

    fun initialize() {
        runCatching {
            WebView.enableSlowWholeDocumentDraw()
        }.onFailure {
            android.util.Log.w("WebToAppApplication", "enableSlowWholeDocumentDraw failed", it)
        }

        com.webtoapp.core.webview.WebViewPool.prewarm(appContext)
        com.webtoapp.core.perf.SystemPerfOptimizer.initSystem(appContext)
        com.webtoapp.core.perf.SystemPerfOptimizer.readaheadCriticalFiles(appContext)

        try {
            billingManager.connect()
            AppLogger.i("WebToAppApplication", "BillingManager connect initiated")
        } catch (e: Exception) {
            AppLogger.w("WebToAppApplication", "BillingManager connect failed: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            billingManager.disconnect()
        } catch (_: Exception) {
        }
        com.webtoapp.core.webview.WebViewPool.release()
        com.webtoapp.core.perf.SystemPerfOptimizer.release()
    }
}
