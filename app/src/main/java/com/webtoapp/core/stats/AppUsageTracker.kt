package com.webtoapp.core.stats

import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap





class AppUsageTracker(
    private val repository: AppStatsRepository
) {
    companion object {
        private const val TAG = "AppUsageTracker"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    private val activeSessions = ConcurrentHashMap<Long, Long>()





    fun trackLaunch(appId: Long) {
        activeSessions[appId] = System.currentTimeMillis()
        scope.launch {
            repository.recordLaunch(appId)
        }
        AppLogger.d(TAG, "trackLaunch: appId=$appId")
    }





    fun trackClose(appId: Long) {
        val startTime = activeSessions.remove(appId) ?: return
        val duration = System.currentTimeMillis() - startTime
        if (duration > 1000) {
            scope.launch {
                repository.recordUsageDuration(appId, duration)
            }
            AppLogger.d(TAG, "trackClose: appId=$appId, duration=${duration}ms")
        }
    }





    fun trackPause(appId: Long) {
        val startTime = activeSessions[appId] ?: return
        val duration = System.currentTimeMillis() - startTime
        if (duration > 1000) {
            scope.launch {
                repository.recordUsageDuration(appId, duration)
            }
        }

        activeSessions[appId] = System.currentTimeMillis()
    }





    fun trackResume(appId: Long) {
        activeSessions[appId] = System.currentTimeMillis()
    }
}
