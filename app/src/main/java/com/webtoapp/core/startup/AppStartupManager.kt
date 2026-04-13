package com.webtoapp.core.startup

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.shell.ShellRuntimeServices
import com.webtoapp.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class AppStartupManager(
    private val loggingStartup: LoggingStartup,
    private val shellRuntimeStartup: ShellRuntimeStartup,
    private val securityStartup: SecurityStartup,
    private val runtimeWarmupStartup: RuntimeWarmupStartup,
    private val legacyHttpUrlMigrationStartup: LegacyHttpUrlMigrationStartup,
    private val backgroundServicesStartup: BackgroundServicesStartup,
    private val database: AppDatabase,
) {

    fun initialize(appScope: CoroutineScope) {
        loggingStartup.initialize()
        shellRuntimeStartup.initialize()
        securityStartup.initialize()
        legacyHttpUrlMigrationStartup.initialize(appScope)
        runtimeWarmupStartup.initialize()
        backgroundServicesStartup.initialize(appScope)
        AppLogger.system("Application", "onCreate completed")
    }

    fun clearCaches() {
        try {
            com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
            com.webtoapp.util.HtmlProjectProcessor.clearEncodingCache()
            AppLogger.i("WebToAppApplication", "App caches cleared")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Failed to clear app caches", e)
        }
    }

    fun shutdown(appScope: CoroutineScope) {
        AppLogger.system("Application", "onTerminate started")

        appScope.coroutineContext[Job]?.cancel()
        backgroundServicesStartup.shutdown()
        PortManager.releaseAll()
        com.webtoapp.util.OfflineManager.release()
        com.webtoapp.core.extension.ExtensionManager.release()
        com.webtoapp.util.DownloadNotificationManager.release()
        com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
        runtimeWarmupStartup.shutdown()
        securityStartup.shutdown()
        database.close()
        ShellRuntimeServices.reset()
        loggingStartup.shutdown()
    }
}
