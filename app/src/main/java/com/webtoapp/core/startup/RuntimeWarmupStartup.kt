package com.webtoapp.core.startup

import com.webtoapp.core.billing.BillingManager
import com.webtoapp.core.cloud.GitHubHostsDns
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RuntimeWarmupStartup(
    private val billingManager: BillingManager,
    private val appContext: android.content.Context,
) {

    fun initialize(appScope: CoroutineScope) {



        appScope.launch {
            com.webtoapp.core.perf.SystemPerfOptimizer.initSystem(appContext)
            com.webtoapp.core.perf.SystemPerfOptimizer.readaheadCriticalFiles(appContext)
        }
        appScope.launch {
            GitHubHostsDns.refreshAsync(appContext)
        }
        appScope.launch {
            try {
                billingManager.connect()
                AppLogger.i("WebToAppApplication", "BillingManager connect initiated")
            } catch (e: Exception) {
                AppLogger.w("WebToAppApplication", "BillingManager connect failed: ${e.message}")
            }
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
