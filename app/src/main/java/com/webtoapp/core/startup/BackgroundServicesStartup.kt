package com.webtoapp.core.startup

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.data.model.AppType
import com.webtoapp.data.repository.WebAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackgroundServicesStartup(
    private val webAppRepository: WebAppRepository,
    private val healthMonitor: AppHealthMonitor,
    private val screenshotService: WebsiteScreenshotService,
) {

    fun initialize(appScope: CoroutineScope) {
        appScope.launch {
            try {
                healthMonitor.startMonitoring(webAppRepository.allWebApps)
                AppLogger.i("WebToAppApplication", "Health monitor started")

                val apps = webAppRepository.allWebApps.first()
                val webApps = apps.filter {
                    it.appType == AppType.WEB &&
                        it.url.startsWith("http") &&
                        it.iconPath == null &&
                        !screenshotService.hasScreenshot(it.id)
                }
                if (webApps.isNotEmpty()) {
                    AppLogger.i("WebToAppApplication", "Auto-screenshot: ${webApps.size} apps need screenshots")
                    for (app in webApps.take(5)) {
                        try {
                            screenshotService.captureScreenshot(app.id, app.url)
                        } catch (e: Exception) {
                            AppLogger.w("WebToAppApplication", "Screenshot failed for ${app.name}: ${e.message}")
                        }
                    }
                }
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
