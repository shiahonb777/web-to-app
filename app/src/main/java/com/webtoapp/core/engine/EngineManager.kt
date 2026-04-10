package com.webtoapp.core.engine

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.engine.download.EngineFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 浏览器引擎管理器（单例）
 * 负责引擎选择、实例创建、下载状态管理
 */
@SuppressLint("StaticFieldLeak")
class EngineManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: EngineManager? = null

        fun getInstance(context: Context): EngineManager {
            return instance ?: synchronized(this) {
                instance ?: EngineManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val fileManager = EngineFileManager(context)

    /** 当前选中的引擎类型（用于 APK 导出设置） */
    private val _selectedEngine = MutableStateFlow(EngineType.SYSTEM_WEBVIEW)
    val selectedEngine: StateFlow<EngineType> = _selectedEngine.asStateFlow()

    /**
     * 设置选中的引擎类型
     */
    fun selectEngine(type: EngineType) {
        _selectedEngine.value = type
    }

    /**
     * 创建指定类型的引擎实例
     * @param type 引擎类型
     * @param adBlocker 广告拦截器（仅 SystemWebView 需要）
     */
    fun createEngine(type: EngineType, adBlocker: AdBlocker): BrowserEngine {
        return when (type) {
            EngineType.SYSTEM_WEBVIEW -> SystemWebViewEngine(context, adBlocker)
            EngineType.GECKOVIEW -> GeckoViewEngine(context)
        }
    }

    /**
     * 检查引擎是否可用（已下载或不需要下载）
     */
    fun isEngineAvailable(type: EngineType): Boolean {
        return when (type) {
            EngineType.SYSTEM_WEBVIEW -> true // 系统 WebView 始终可用
            EngineType.GECKOVIEW -> fileManager.isEngineDownloaded(EngineType.GECKOVIEW)
        }
    }

    /**
     * 获取引擎下载状态描述
     */
    fun getEngineStatus(type: EngineType): EngineStatus {
        return when {
            !type.requiresDownload -> EngineStatus.READY
            fileManager.isEngineDownloaded(type) -> {
                val version = fileManager.getDownloadedVersion(type)
                EngineStatus.DOWNLOADED(version ?: "unknown")
            }
            else -> EngineStatus.NOT_DOWNLOADED
        }
    }

    /**
     * 获取引擎文件占用磁盘空间 (bytes)
     */
    fun getEngineSize(type: EngineType): Long {
        return fileManager.getEngineSize(type)
    }

    /**
     * 删除已下载的引擎文件
     */
    fun deleteEngine(type: EngineType): Boolean {
        return fileManager.deleteEngineFiles(type)
    }
}

/**
 * 引擎状态
 */
sealed class EngineStatus {
    /** 随时可用（不需要下载） */
    data object READY : EngineStatus()

    /** 已下载 */
    data class DOWNLOADED(val version: String) : EngineStatus()

    /** 未下载 */
    data object NOT_DOWNLOADED : EngineStatus()
}
