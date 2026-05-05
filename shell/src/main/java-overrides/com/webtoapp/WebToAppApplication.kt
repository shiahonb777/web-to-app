package com.webtoapp

import android.app.Application
import android.content.ComponentCallbacks2
import android.webkit.WebView
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellModeManager









class WebToAppApplication : Application() {

    private var shellActivationManager: ActivationManager? = null
    private var shellAnnouncementManager: AnnouncementManager? = null
    private var shellAdBlocker: AdBlocker? = null
    private var shellModeManagerLocal: ShellModeManager? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        runCatching {
            WebView.enableSlowWholeDocumentDraw()
        }.onFailure {
            android.util.Log.w("WebToAppApplication", "enableSlowWholeDocumentDraw failed", it)
        }

        try {
            AppLogger.init(this)
            AppLogger.system("Application", "onCreate started (shell)")
        } catch (e: Exception) {
            android.util.Log.e("WebToAppApplication", "AppLogger initialization failed", e)
        }


        initShellRuntime()


        com.webtoapp.core.perf.SystemPerfOptimizer.initSystem(this)
        com.webtoapp.core.perf.SystemPerfOptimizer.readaheadCriticalFiles(this)


        com.webtoapp.core.webview.WebViewPool.prewarm(this)

        AppLogger.system("Application", "onCreate completed (shell)")
    }

    override fun onTerminate() {
        AppLogger.system("Application", "onTerminate started (shell)")
        cleanupSingletons()
        AppLogger.shutdown()
        super.onTerminate()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AppLogger.w("WebToAppApplication", "onLowMemory triggered")
        clearAppCaches()
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
            clearAppCaches()
        }
    }

    private fun clearAppCaches() {
        try {
            com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
            com.webtoapp.util.HtmlProjectProcessor.clearEncodingCache()
            AppLogger.i("WebToAppApplication", "App caches cleared")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Failed to clear app caches", e)
        }
    }

    private fun cleanupSingletons() {
        try {
            AppLogger.d("WebToAppApplication", "Cleaning up singleton resources...")
            shellActivationManager = null
            shellAnnouncementManager = null
            shellAdBlocker = null
            shellModeManagerLocal = null

            com.webtoapp.core.extension.ExtensionManager.release()
            com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
            com.webtoapp.core.webview.WebViewPool.release()
            com.webtoapp.core.perf.SystemPerfOptimizer.release()
            com.webtoapp.core.port.PortManager.releaseAll()
            com.webtoapp.util.DownloadNotificationManager.release()

            AppLogger.i("WebToAppApplication", "Shell runtime resources cleaned up")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Failed to cleanup singleton resources", e)
        }
    }

    private fun initShellRuntime() {
        shellModeManagerLocal = ShellModeManager(this)
        shellActivationManager = ActivationManager(this)
        shellAnnouncementManager = AnnouncementManager(this)
        shellAdBlocker = AdBlocker()

        try {
            val isShell = shellModeManagerLocal?.isShellMode() == true
            AppLogger.i("WebToAppApplication", "Dedicated shell runtime initialized: shellMode=$isShell")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Shell runtime initialization failed", e)
        }
    }

    companion object {
        private lateinit var instance: WebToAppApplication

        fun getInstance(): WebToAppApplication = instance

        val activation: ActivationManager
            get() = instance.shellActivationManager ?: error("Shell ActivationManager unavailable")

        val announcement: AnnouncementManager
            get() = instance.shellAnnouncementManager ?: error("Shell AnnouncementManager unavailable")

        val adBlock: AdBlocker
            get() = instance.shellAdBlocker ?: error("Shell AdBlocker unavailable")

        val shellMode: ShellModeManager
            get() = instance.shellModeManagerLocal ?: error("ShellModeManager unavailable")
    }
}
