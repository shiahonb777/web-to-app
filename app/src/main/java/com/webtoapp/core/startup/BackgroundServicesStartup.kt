package com.webtoapp.core.startup

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.data.repository.WebAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BackgroundServicesStartup(
    private val webAppRepository: WebAppRepository,
    private val healthMonitor: AppHealthMonitor,
    @Suppress("unused") private val screenshotService: com.webtoapp.core.stats.WebsiteScreenshotService,
) {

    fun initialize(appScope: CoroutineScope) {
        appScope.launch {
            try {
                healthMonitor.startMonitoring(webAppRepository.httpWebApps)
                AppLogger.i("WebToAppApplication", "Health monitor started")
            } catch (e: Exception) {
                AppLogger.e("WebToAppApplication", "Background services start failed", e)
            }
        }
    }

    fun shutdown() {
        try {
            healthMonitor.destroy()
        } catch (_: Exception) {
        }
    }
}
