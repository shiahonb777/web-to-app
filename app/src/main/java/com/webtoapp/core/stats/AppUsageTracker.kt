package com.webtoapp.core.stats

import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用使用追踪器
 * 追踪每个 App 的启动和使用时长
 */
class AppUsageTracker(
    private val repository: AppStatsRepository
) {
    companion object {
        private const val TAG = "AppUsageTracker"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 正在使用中的会话：appId → 开始时间
    private val activeSessions = ConcurrentHashMap<Long, Long>()
    
    /**
     * 记录应用启动
     * 在 WebViewActivity / ShellActivity 的 onCreate 中调用
     */
    fun trackLaunch(appId: Long) {
        activeSessions[appId] = System.currentTimeMillis()
        scope.launch {
            repository.recordLaunch(appId)
        }
        AppLogger.d(TAG, "trackLaunch: appId=$appId")
    }
    
    /**
     * 记录应用关闭
     * 在 WebViewActivity / ShellActivity 的 onDestroy 中调用
     */
    fun trackClose(appId: Long) {
        val startTime = activeSessions.remove(appId) ?: return
        val duration = System.currentTimeMillis() - startTime
        if (duration > 1000) { // 忽略不到 1 秒的会话
            scope.launch {
                repository.recordUsageDuration(appId, duration)
            }
            AppLogger.d(TAG, "trackClose: appId=$appId, duration=${duration}ms")
        }
    }
    
    /**
     * 记录应用暂停（进入后台）
     * 在 onPause 中调用，保存当前会话时长
     */
    fun trackPause(appId: Long) {
        val startTime = activeSessions[appId] ?: return
        val duration = System.currentTimeMillis() - startTime
        if (duration > 1000) {
            scope.launch {
                repository.recordUsageDuration(appId, duration)
            }
        }
        // 重置开始时间
        activeSessions[appId] = System.currentTimeMillis()
    }
    
    /**
     * 记录应用恢复（从后台回来）
     * 在 onResume 中调用
     */
    fun trackResume(appId: Long) {
        activeSessions[appId] = System.currentTimeMillis()
    }
}
