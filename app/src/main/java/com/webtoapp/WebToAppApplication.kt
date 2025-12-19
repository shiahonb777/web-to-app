package com.webtoapp

import android.app.Application
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.repository.WebAppRepository

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

    // Shell 模式管理器
    val shellModeManager: ShellModeManager by lazy {
        ShellModeManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private lateinit var instance: WebToAppApplication

        fun getInstance(): WebToAppApplication = instance

        val repository: WebAppRepository
            get() = instance.webAppRepository

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
