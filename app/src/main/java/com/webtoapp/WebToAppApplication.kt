package com.webtoapp

import android.app.Application
import android.content.ComponentCallbacks2
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.startup.AppStartupManager
import com.webtoapp.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point.
 *
 * Starts Koin and forwards lifecycle events to the startup manager.
 */
class WebToAppApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startupManager: AppStartupManager by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@WebToAppApplication)
            modules(appModules)
        }

        startupManager.initialize(appScope)
    }

    override fun onTerminate() {
        startupManager.shutdown(appScope)
        super.onTerminate()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AppLogger.w("WebToAppApplication", "onLowMemory triggered")
        startupManager.clearCaches()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val levelName = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        AppLogger.d("WebToAppApplication", "onTrimMemory: $levelName")

        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            startupManager.clearCaches()
        }
    }
}
