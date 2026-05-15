package com.webtoapp.core.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RuntimeWarmupStartup(
    private val appContext: android.content.Context,
) {

    fun initialize(appScope: CoroutineScope) {
        appScope.launch {
            com.webtoapp.core.perf.SystemPerfOptimizer.initSystem(appContext)
            com.webtoapp.core.perf.SystemPerfOptimizer.readaheadCriticalFiles(appContext)
        }
    }

    fun shutdown() {
        com.webtoapp.core.webview.WebViewPool.release()
        com.webtoapp.core.perf.SystemPerfOptimizer.release()
    }
}
