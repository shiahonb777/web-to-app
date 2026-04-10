package com.webtoapp.core.stats

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 应用使用统计 DAO
 */
@Dao
interface AppUsageStatsDao {
    
    // ==================== 使用统计 ====================
    
    @Query("SELECT * FROM app_usage_stats WHERE appId = :appId")
    suspend fun getStatsByAppId(appId: Long): AppUsageStats?
    
    @Query("SELECT * FROM app_usage_stats WHERE appId = :appId")
    fun getStatsByAppIdFlow(appId: Long): Flow<AppUsageStats?>
    
    @Query("SELECT * FROM app_usage_stats ORDER BY lastUsedAt DESC")
    fun getAllStats(): Flow<List<AppUsageStats>>
    
    @Query("SELECT * FROM app_usage_stats ORDER BY launchCount DESC LIMIT :limit")
    fun getMostUsedApps(limit: Int = 10): Flow<List<AppUsageStats>>
    
    @Query("SELECT * FROM app_usage_stats ORDER BY totalUsageMs DESC LIMIT :limit")
    fun getMostTimeSpentApps(limit: Int = 10): Flow<List<AppUsageStats>>
    
    @Query("SELECT * FROM app_usage_stats WHERE lastUsedAt > :since ORDER BY lastUsedAt DESC")
    fun getRecentlyUsedApps(since: Long): Flow<List<AppUsageStats>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: AppUsageStats): Long
    
    @Update
    suspend fun update(stats: AppUsageStats)
    
    @Query("DELETE FROM app_usage_stats WHERE appId = :appId")
    suspend fun deleteByAppId(appId: Long)
    
    /**
     * 原子性地增加启动次数并更新最后使用时间
     */
    @Query("""
        UPDATE app_usage_stats 
        SET launchCount = launchCount + 1, lastUsedAt = :timestamp 
        WHERE appId = :appId
    """)
    suspend fun incrementLaunchCount(appId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新使用时长
     */
    @Query("""
        UPDATE app_usage_stats 
        SET totalUsageMs = totalUsageMs + :durationMs, 
            lastSessionDurationMs = :durationMs,
            lastUsedAt = :timestamp 
        WHERE appId = :appId
    """)
    suspend fun addUsageDuration(appId: Long, durationMs: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 汇总统计：总启动次数
     */
    @Query("SELECT COALESCE(SUM(launchCount), 0) FROM app_usage_stats")
    suspend fun getTotalLaunchCount(): Int
    
    /**
     * 汇总统计：总使用时长
     */
    @Query("SELECT COALESCE(SUM(totalUsageMs), 0) FROM app_usage_stats")
    suspend fun getTotalUsageMs(): Long
    
    /**
     * 汇总统计：活跃应用数（至少启动过 1 次）
     */
    @Query("SELECT COUNT(*) FROM app_usage_stats WHERE launchCount > 0")
    suspend fun getActiveAppCount(): Int
    
    // ==================== 健康检测 ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthRecord(record: AppHealthRecord): Long
    
    @Query("SELECT * FROM app_health_records WHERE appId = :appId ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getLatestHealthRecord(appId: Long): AppHealthRecord?
    
    @Query("SELECT * FROM app_health_records WHERE appId = :appId ORDER BY checkedAt DESC LIMIT 1")
    fun getLatestHealthRecordFlow(appId: Long): Flow<AppHealthRecord?>
    
    @Query("SELECT * FROM app_health_records WHERE appId = :appId AND checkedAt > :since ORDER BY checkedAt ASC")
    fun getHealthHistory(appId: Long, since: Long): Flow<List<AppHealthRecord>>
    
    @Query("SELECT * FROM app_health_records WHERE appId = :appId ORDER BY checkedAt DESC LIMIT :limit")
    suspend fun getRecentHealthRecords(appId: Long, limit: Int = 50): List<AppHealthRecord>
    
    /**
     * 获取所有应用的最新健康状态（首页用）
     */
    @Query("""
        SELECT h.* FROM app_health_records h
        INNER JOIN (
            SELECT appId, MAX(checkedAt) as maxCheckedAt 
            FROM app_health_records 
            GROUP BY appId
        ) latest ON h.appId = latest.appId AND h.checkedAt = latest.maxCheckedAt
    """)
    fun getAllLatestHealthRecords(): Flow<List<AppHealthRecord>>
    
    /**
     * 清理过期的健康记录（保留最近 7 天）
     */
    @Query("DELETE FROM app_health_records WHERE checkedAt < :before")
    suspend fun cleanupOldRecords(before: Long)
    
    /**
     * 计算近 24h 在线率
     */
    @Query("""
        SELECT CAST(
            SUM(CASE WHEN status = 'ONLINE' OR status = 'SLOW' THEN 1 ELSE 0 END) AS FLOAT
        ) / COUNT(*) 
        FROM app_health_records 
        WHERE appId = :appId AND checkedAt > :since
    """)
    suspend fun getUptimePercent(appId: Long, since: Long): Float?
}
