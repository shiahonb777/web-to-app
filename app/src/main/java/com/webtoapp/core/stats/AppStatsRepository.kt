package com.webtoapp.core.stats

import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow





class AppStatsRepository(private val dao: AppUsageStatsDao) {

    companion object {
        private const val TAG = "AppStatsRepository"
    }



    val allStats: Flow<List<AppUsageStats>> = dao.getAllStats()

    fun getStatsByAppId(appId: Long): Flow<AppUsageStats?> = dao.getStatsByAppIdFlow(appId)

    fun getMostUsedApps(limit: Int = 10): Flow<List<AppUsageStats>> = dao.getMostUsedApps(limit)

    fun getMostTimeSpentApps(limit: Int = 10): Flow<List<AppUsageStats>> = dao.getMostTimeSpentApps(limit)

    fun getRecentlyUsedApps(since: Long): Flow<List<AppUsageStats>> = dao.getRecentlyUsedApps(since)




    suspend fun recordLaunch(appId: Long) {
        try {
            val existing = dao.getStatsByAppId(appId)
            if (existing == null) {
                dao.insert(AppUsageStats(
                    appId = appId,
                    launchCount = 1,
                    lastUsedAt = System.currentTimeMillis()
                ))
            } else {
                dao.incrementLaunchCount(appId)
            }
            AppLogger.d(TAG, "记录启动: appId=$appId")
        } catch (e: Exception) {
            AppLogger.e(TAG, "记录启动失败: ${e.message}")
        }
    }




    suspend fun recordUsageDuration(appId: Long, durationMs: Long) {
        if (durationMs <= 0) return
        try {
            val existing = dao.getStatsByAppId(appId)
            if (existing == null) {
                dao.insert(AppUsageStats(
                    appId = appId,
                    totalUsageMs = durationMs,
                    lastSessionDurationMs = durationMs,
                    lastUsedAt = System.currentTimeMillis()
                ))
            } else {
                dao.addUsageDuration(appId, durationMs)
            }
            AppLogger.d(TAG, "记录使用时长: appId=$appId, duration=${durationMs}ms")
        } catch (e: Exception) {
            AppLogger.e(TAG, "记录使用时长失败: ${e.message}")
        }
    }




    suspend fun getOverallStats(): OverallStats {
        return try {
            OverallStats(
                totalLaunchCount = dao.getTotalLaunchCount(),
                totalUsageMs = dao.getTotalUsageMs(),
                activeAppCount = dao.getActiveAppCount()
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "获取汇总统计失败: ${e.message}")
            OverallStats()
        }
    }



    fun getAllLatestHealthRecords(): Flow<List<AppHealthRecord>> = dao.getAllLatestHealthRecords()

    fun getLatestHealthRecord(appId: Long): Flow<AppHealthRecord?> = dao.getLatestHealthRecordFlow(appId)

    fun getHealthHistory(appId: Long, since: Long): Flow<List<AppHealthRecord>> = dao.getHealthHistory(appId, since)

    suspend fun saveHealthRecord(record: AppHealthRecord) {
        try {
            dao.insertHealthRecord(record)
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存健康记录失败: ${e.message}")
        }
    }

    suspend fun getRecentHealthRecords(appId: Long, limit: Int = 50): List<AppHealthRecord> {
        return try {
            dao.getRecentHealthRecords(appId, limit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "获取健康记录失败: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUptimePercent(appId: Long): Float {
        val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        return dao.getUptimePercent(appId, since) ?: 0f
    }




    suspend fun cleanupOldHealthRecords() {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        dao.cleanupOldRecords(sevenDaysAgo)
    }
}




data class OverallStats(
    val totalLaunchCount: Int = 0,
    val totalUsageMs: Long = 0,
    val activeAppCount: Int = 0
) {
    val formattedTotalUsage: String
        get() {
            val totalSeconds = totalUsageMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
            }
        }
}
