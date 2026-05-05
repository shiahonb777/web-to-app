package com.webtoapp

import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.image.OptimizedImageLoader
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.core.startup.AppStartupManager
import com.webtoapp.core.startup.BackgroundServicesStartup
import com.webtoapp.core.startup.LegacyHttpUrlMigrationStartup
import com.webtoapp.core.startup.LoggingStartup
import com.webtoapp.core.startup.RuntimeWarmupStartup
import com.webtoapp.core.startup.SecurityStartup
import com.webtoapp.core.startup.ShellRuntimeStartup
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.data.repository.AppCategoryRepository
import com.webtoapp.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level




class WebToAppApplication : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appStartupManager: AppStartupManager? = null
    private var shellActivationManager: ActivationManager? = null
    private var shellAnnouncementManager: AnnouncementManager? = null
    private var shellAdBlocker: AdBlocker? = null
    private var shellModeManagerLocal: ShellModeManager? = null


    val database: AppDatabase by inject()
    val webAppRepository: WebAppRepository by inject()
    val appCategoryRepository: AppCategoryRepository by inject()
    val activationManager: ActivationManager by inject()
    val announcementManager: AnnouncementManager by inject()
    val adBlocker: AdBlocker by inject()
    val shellModeManager: ShellModeManager by inject()
    val billingManager: com.webtoapp.core.billing.BillingManager by inject()
    val healthMonitor: com.webtoapp.core.stats.AppHealthMonitor by inject()
    val screenshotService: com.webtoapp.core.stats.WebsiteScreenshotService by inject()

    override fun onCreate() {
        super.onCreate()
        instance = this
        Strings.initialize(this)

        if (BuildConfig.SHELL_RUNTIME_ONLY) {
            initShellRuntime()
            AppLogger.system("Application", "onCreate completed (shell)")
            return
        }


        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@WebToAppApplication)
                modules(appModules)
            }
        }

        appStartupManager = AppStartupManager(
            loggingStartup = LoggingStartup(this),
            shellRuntimeStartup = ShellRuntimeStartup(
                shellModeManager = shellModeManager,
                activationManager = activationManager,
                announcementManager = announcementManager,
                adBlocker = adBlocker,
            ),
            securityStartup = SecurityStartup(this),
            runtimeWarmupStartup = RuntimeWarmupStartup(
                billingManager = billingManager,
                appContext = this,
            ),
            legacyHttpUrlMigrationStartup = LegacyHttpUrlMigrationStartup(
                context = this,
                webAppRepository = webAppRepository,
            ),
            backgroundServicesStartup = BackgroundServicesStartup(
                webAppRepository = webAppRepository,
                healthMonitor = healthMonitor,
                screenshotService = screenshotService,
            ),
            database = database,
        ).also { it.initialize(appScope) }
    }

    override fun onTerminate() {
        if (BuildConfig.SHELL_RUNTIME_ONLY) {
            cleanupSingletons()
            AppLogger.shutdown()
        } else {
            appStartupManager?.shutdown(appScope)
        }
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
        if (BuildConfig.SHELL_RUNTIME_ONLY) {
            try {
                com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
                com.webtoapp.util.HtmlProjectProcessor.clearEncodingCache()
                AppLogger.i("WebToAppApplication", "App caches cleared")
            } catch (e: Exception) {
                AppLogger.e("WebToAppApplication", "Failed to clear app caches", e)
            }
            return
        }

        appStartupManager?.clearCaches()
    }




    private fun cleanupSingletons() {
        try {
            AppLogger.d("WebToAppApplication", "Cleaning up singleton resources...")

            if (BuildConfig.SHELL_RUNTIME_ONLY) {
                shellActivationManager = null
                shellAnnouncementManager = null
                shellAdBlocker = null
                shellModeManagerLocal = null
                AppLogger.i("WebToAppApplication", "Shell runtime resources cleaned up")
                return
            }
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




    override fun newImageLoader(): ImageLoader = OptimizedImageLoader.get(this)

    companion object {
        private lateinit var instance: WebToAppApplication

        fun getInstance(): WebToAppApplication = instance




        @Deprecated("编辑器侧请使用 Koin 注入: val repo: WebAppRepository by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(WebAppRepository::class.java)"))
        val repository: WebAppRepository
            get() = instance.webAppRepository

        @Deprecated("编辑器侧请使用 Koin 注入: val repo: AppCategoryRepository by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(AppCategoryRepository::class.java)"))
        val categoryRepository: AppCategoryRepository
            get() = instance.appCategoryRepository

        @Deprecated("编辑器侧请使用 Koin 注入: val mgr: ActivationManager by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(ActivationManager::class.java)"))
        val activation: ActivationManager
            get() = if (BuildConfig.SHELL_RUNTIME_ONLY) {
                instance.shellActivationManager ?: error("Shell ActivationManager unavailable")
            } else {
                instance.activationManager
            }

        @Deprecated("编辑器侧请使用 Koin 注入: val mgr: AnnouncementManager by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(AnnouncementManager::class.java)"))
        val announcement: AnnouncementManager
            get() = if (BuildConfig.SHELL_RUNTIME_ONLY) {
                instance.shellAnnouncementManager ?: error("Shell AnnouncementManager unavailable")
            } else {
                instance.announcementManager
            }

        @Deprecated("编辑器侧请使用 Koin 注入: val blocker: AdBlocker by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(AdBlocker::class.java)"))
        val adBlock: AdBlocker
            get() = if (BuildConfig.SHELL_RUNTIME_ONLY) {
                instance.shellAdBlocker ?: error("Shell AdBlocker unavailable")
            } else {
                instance.adBlocker
            }

        @Deprecated("编辑器侧请使用 Koin 注入: val mgr: ShellModeManager by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(ShellModeManager::class.java)"))
        val shellMode: ShellModeManager
            get() = if (BuildConfig.SHELL_RUNTIME_ONLY) {
                instance.shellModeManagerLocal ?: error("ShellModeManager unavailable")
            } else {
                instance.shellModeManager
            }
    }
}
