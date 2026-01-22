package com.webtoapp

import android.app.Application
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.data.repository.AppCategoryRepository

/**
 * Application类 - 全局依赖管理
 */
class WebToAppApplication : Application() {

    // 延迟初始化数据库
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    // Repository
    val webAppRepository: WebAppRepository by lazy {
        WebAppRepository(database.webAppDao())
    }
    
    val appCategoryRepository: AppCategoryRepository by lazy {
        AppCategoryRepository(database.appCategoryDao())
    }

    // 核心管理器
    val activationManager: ActivationManager by lazy {
        ActivationManager(this)
    }

    val announcementManager: AnnouncementManager by lazy {
        AnnouncementManager(this)
    }

    val adBlocker: AdBlocker by lazy {
        AdBlocker()
    }

    // Shell 模式管理器（添加异常保护）
    val shellModeManager: ShellModeManager by lazy {
        try {
            ShellModeManager(this)
        } catch (e: Exception) {
            android.util.Log.e("WebToAppApplication", "ShellModeManager 初始化失败", e)
            // 返回一个新实例，让它在后续调用时再次尝试
            ShellModeManager(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 预加载 Shell 模式检查（捕获可能的初始化错误）
        try {
            val isShell = shellModeManager.isShellMode()
            android.util.Log.d("WebToAppApplication", "Shell 模式预检查: $isShell")
        } catch (e: Exception) {
            android.util.Log.e("WebToAppApplication", "Shell 模式预检查失败", e)
        } catch (e: Error) {
            android.util.Log.e("WebToAppApplication", "Shell 模式预检查发生严重错误", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // 清理所有单例资源
        cleanupSingletons()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // 低内存时清理缓存
        try {
            com.webtoapp.util.CacheManager.clearCookies()
            com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
            com.webtoapp.util.HtmlProjectProcessor.clearEncodingCache()
            adBlocker.clearPatternCache()
            android.util.Log.d("WebToAppApplication", "低内存，已清理部分缓存")
        } catch (e: Exception) {
            android.util.Log.e("WebToAppApplication", "低内存清理失败", e)
        }
    }
    
    /**
     * 清理所有单例资源
     */
    private fun cleanupSingletons() {
        try {
            com.webtoapp.util.OfflineManager.release()
            com.webtoapp.core.extension.ExtensionManager.release()
            com.webtoapp.util.DownloadNotificationManager.release()
            com.webtoapp.core.crypto.AesCryptoEngine.clearKeyCache()
            AppDatabase.closeDatabase()
            android.util.Log.d("WebToAppApplication", "单例资源已清理")
        } catch (e: Exception) {
            android.util.Log.e("WebToAppApplication", "清理单例资源失败", e)
        }
    }

    companion object {
        private lateinit var instance: WebToAppApplication

        fun getInstance(): WebToAppApplication = instance

        val repository: WebAppRepository
            get() = instance.webAppRepository
        
        val categoryRepository: AppCategoryRepository
            get() = instance.appCategoryRepository

        val activation: ActivationManager
            get() = instance.activationManager

        val announcement: AnnouncementManager
            get() = instance.announcementManager

        val adBlock: AdBlocker
            get() = instance.adBlocker

        val shellMode: ShellModeManager
            get() = instance.shellModeManager
    }
}
