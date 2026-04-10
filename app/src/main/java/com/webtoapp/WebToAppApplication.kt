package com.webtoapp

import android.app.Application
import android.content.ComponentCallbacks2
import android.webkit.WebView
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.crypto.SecurityInitializer
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.model.AppType
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.data.repository.AppCategoryRepository
import com.webtoapp.di.appModules
import com.webtoapp.util.isInsecureRemoteHttpUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.android.ext.koin.androidLogger

/**
 * Application class - Global dependency management
 */
class WebToAppApplication : Application() {
    
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Koin-managed dependencies (lazy inject)
    val database: AppDatabase by inject()
    val webAppRepository: WebAppRepository by inject()
    val appCategoryRepository: AppCategoryRepository by inject()
    val activationManager: ActivationManager by inject()
    val announcementManager: AnnouncementManager by inject()
    val adBlocker: AdBlocker by inject()
    val shellModeManager: ShellModeManager by inject()
    val billingManager: com.webtoapp.core.billing.BillingManager by inject()

    override fun onCreate() {
        super.onCreate()
        instance = this

        runCatching {
            WebView.enableSlowWholeDocumentDraw()
        }.onFailure {
            android.util.Log.w("WebToAppApplication", "enableSlowWholeDocumentDraw failed", it)
        }
        
        // 初始化 Koin 依赖注入
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@WebToAppApplication)
            modules(appModules)
        }
        
        // 初始化运行日志系统（最先初始化，以便记录后续所有日志）
        try {
            AppLogger.init(this)
            AppLogger.system("Application", "onCreate started")
        } catch (e: Exception) {
            android.util.Log.e("WebToAppApplication", "AppLogger initialization failed", e)
        }
        
        // Preload Shell mode check (catch possible initialization errors)
        try {
            val isShell = shellModeManager.isShellMode()
            AppLogger.i("WebToAppApplication", "Shell mode pre-check: $isShell")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Shell mode pre-check failed", e)
        } catch (e: Error) {
            AppLogger.e("WebToAppApplication", "Shell mode pre-check critical error", Error(e))
        }

        // Initialize runtime security protection.
        try {
            val initialized = SecurityInitializer.initialize(this) { result ->
                AppLogger.w(
                    "WebToAppApplication",
                    "Security threat detected: level=${result.threatLevel}, block=${result.shouldBlock}, threats=${result.threats}"
                )
            }
            AppLogger.i("WebToAppApplication", "Security initializer status: $initialized")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Security initialization failed", e)
        } catch (e: Error) {
            AppLogger.e("WebToAppApplication", "Security initialization critical error", Error(e))
        }
        
        migrateLegacyInsecureWebUrls()
        
        // 预热 WebView 引擎（异步，不阻塞启动）
        com.webtoapp.core.webview.WebViewPool.prewarm(this)
        
        // C 级系统层极致性能优化（异步: CPU亲和性, FD上限, 线程优先级, 温度监控）
        com.webtoapp.core.perf.SystemPerfOptimizer.initSystem(this)
        // I/O 预读: 将 WebView 二进制/数据库提前加载到内核页缓存
        com.webtoapp.core.perf.SystemPerfOptimizer.readaheadCriticalFiles(this)
        
        // 预初始化 BillingManager（异步连接 Google Play，避免导航时卡顿）
        try {
            billingManager.connect()
            AppLogger.i("WebToAppApplication", "BillingManager connect initiated")
        } catch (e: Exception) {
            AppLogger.w("WebToAppApplication", "BillingManager connect failed: ${e.message}")
        }
        
        // 启动健康监控 + 自动截图
        startBackgroundServices()
        
        AppLogger.system("Application", "onCreate completed")
    }
    
    override fun onTerminate() {
        AppLogger.system("Application", "onTerminate started")
        
        // Cancel application-scoped coroutines first
        appScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        
        // 断开 Billing 连接
        try { billingManager.disconnect() } catch (_: Exception) {}
        
        // Cleanup all singleton resources
        cleanupSingletons()
        
        // 关闭日志系统（最后关闭）
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
        
        // Do not clear WebStorage automatically.
        // WebStorage.deleteAllData() wipes LocalStorage/WebSQL and can break login sessions.
        
        // 在后台或内存紧张时清理更多缓存
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            clearAppCaches()
        }
    }
    
    /**
     * Clear app-level caches to free memory
     */
    private fun clearAppCaches() {
        try {
            // Do not clear cookies automatically on memory trim.
            // onTrimMemory(TRIM_MEMORY_BACKGROUND) is called whenever app goes to background,
            // clearing cookies here will log users out unexpectedly.
            com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
            com.webtoapp.util.HtmlProjectProcessor.clearEncodingCache()
            AppLogger.i("WebToAppApplication", "App caches cleared")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Failed to clear app caches", e)
        }
    }
    
    /**
     * Cleanup all singleton resources
     */
    private fun cleanupSingletons() {
        try {
            AppLogger.d("WebToAppApplication", "Cleaning up singleton resources...")
            
            // 销毁健康监控（释放协程资源）
            try {
                val healthMonitor: com.webtoapp.core.stats.AppHealthMonitor by inject()
                healthMonitor.destroy()
            } catch (_: Exception) {}
            
            // 释放所有端口
            PortManager.releaseAll()
            
            com.webtoapp.util.OfflineManager.release()
            com.webtoapp.core.extension.ExtensionManager.release()
            com.webtoapp.util.DownloadNotificationManager.release()
            com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
            com.webtoapp.core.webview.WebViewPool.release()
            com.webtoapp.core.perf.SystemPerfOptimizer.release()
            SecurityInitializer.shutdown()
            AppDatabase.closeDatabase()
            AppLogger.i("WebToAppApplication", "Singleton resources cleaned up")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Failed to cleanup singleton resources", e)
        }
    }
    
    /**
     * 启动后台服务：健康监控 + 自动截图
     */
    private fun startBackgroundServices() {
        appScope.launch {
            try {
                // 启动健康监控（定期检测 WEB 应用状态）
                val healthMonitor: com.webtoapp.core.stats.AppHealthMonitor by inject()
                healthMonitor.startMonitoring(webAppRepository.allWebApps)
                AppLogger.i("WebToAppApplication", "Health monitor started")
                
                // 自动截图：为没有截图的 WEB 应用捕获截图
                val screenshotService: com.webtoapp.core.stats.WebsiteScreenshotService by inject()
                val apps = webAppRepository.allWebApps.first()
                val webApps = apps.filter { 
                    it.appType == AppType.WEB && 
                    it.url.startsWith("http") && 
                    it.iconPath == null && 
                    !screenshotService.hasScreenshot(it.id)
                }
                if (webApps.isNotEmpty()) {
                    AppLogger.i("WebToAppApplication", "Auto-screenshot: ${webApps.size} apps need screenshots")
                    for (app in webApps.take(5)) { // 每次最多截图 5 个，避免卡顿
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
    
    private fun migrateLegacyInsecureWebUrls() {
        appScope.launch {
            val prefs = getSharedPreferences("security_migrations", MODE_PRIVATE)
            if (prefs.getBoolean(KEY_HTTP_URL_MIGRATED, false)) return@launch
            
            runCatching {
                val allApps = webAppRepository.allWebApps.first()
                val appsToUpdate = allApps
                    .filter { it.appType == AppType.WEB && isInsecureRemoteHttpUrl(it.url) }
                    .map { app ->
                        val upgradedUrl = app.url.replaceFirst(Regex("(?i)^http://"), "https://")
                        app.copy(url = upgradedUrl)
                    }
                
                if (appsToUpdate.isNotEmpty()) {
                    webAppRepository.updateWebApps(appsToUpdate)
                    AppLogger.i(
                        "WebToAppApplication",
                        "Migrated ${appsToUpdate.size} legacy insecure HTTP web URL(s) to HTTPS"
                    )
                }
                
                prefs.edit().putBoolean(KEY_HTTP_URL_MIGRATED, true).apply()
            }.onFailure { e ->
                AppLogger.e("WebToAppApplication", "Failed to migrate legacy HTTP URLs", Exception(e))
            }
        }
    }
    
    companion object {
        private const val KEY_HTTP_URL_MIGRATED = "legacy_http_url_migrated_v1"

        private lateinit var instance: WebToAppApplication

        fun getInstance(): WebToAppApplication = instance

        // === 以下 getter 仅供 Shell 模式使用（Shell APK 不含 Koin）===
        // 编辑器侧新代码请使用 Koin 注入：val repo: WebAppRepository by inject()

        @Deprecated("编辑器侧请使用 Koin 注入: val repo: WebAppRepository by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(WebAppRepository::class.java)"))
        val repository: WebAppRepository
            get() = instance.webAppRepository
        
        @Deprecated("编辑器侧请使用 Koin 注入: val repo: AppCategoryRepository by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(AppCategoryRepository::class.java)"))
        val categoryRepository: AppCategoryRepository
            get() = instance.appCategoryRepository

        @Deprecated("编辑器侧请使用 Koin 注入: val mgr: ActivationManager by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(ActivationManager::class.java)"))
        val activation: ActivationManager
            get() = instance.activationManager

        @Deprecated("编辑器侧请使用 Koin 注入: val mgr: AnnouncementManager by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(AnnouncementManager::class.java)"))
        val announcement: AnnouncementManager
            get() = instance.announcementManager

        @Deprecated("编辑器侧请使用 Koin 注入: val blocker: AdBlocker by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(AdBlocker::class.java)"))
        val adBlock: AdBlocker
            get() = instance.adBlocker

        @Deprecated("编辑器侧请使用 Koin 注入: val mgr: ShellModeManager by inject()", ReplaceWith("org.koin.java.KoinJavaComponent.get(ShellModeManager::class.java)"))
        val shellMode: ShellModeManager
            get() = instance.shellModeManager
    }
}
