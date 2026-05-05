package com.webtoapp.core.stats

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.webtoapp.data.model.WebApp





@Entity(
    tableName = "app_usage_stats",
    indices = [
        Index(value = ["appId"], unique = true),
        Index(value = ["lastUsedAt"]),
        Index(value = ["launchCount"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = WebApp::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppUsageStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appId: Long,
    val launchCount: Int = 0,
    val totalUsageMs: Long = 0,
    val lastUsedAt: Long = 0,
    val lastSessionDurationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
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


    val formattedLastUsed: String
        get() {
            if (lastUsedAt == 0L) return "从未使用"
            val diff = System.currentTimeMillis() - lastUsedAt
            val minutes = diff / 60_000
            val hours = minutes / 60
            val days = hours / 24
            return when {
                minutes < 1 -> "刚刚"
                minutes < 60 -> "${minutes}分钟前"
                hours < 24 -> "${hours}小时前"
                days < 30 -> "${days}天前"
                else -> "${days / 30}个月前"
            }
        }
}




enum class HealthStatus {
    UNKNOWN,
    ONLINE,
    SLOW,
    OFFLINE
}




@Entity(
    tableName = "app_health_records",
    indices = [
        Index(value = ["appId"]),
        Index(value = ["checkedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = WebApp::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppHealthRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appId: Long,
    val url: String,
    val status: HealthStatus = HealthStatus.UNKNOWN,
    val responseTimeMs: Long = 0,
    val httpStatusCode: Int = 0,
    val errorMessage: String? = null,
    val checkedAt: Long = System.currentTimeMillis()
)




data class AppHealthSummary(
    val appId: Long,
    val latestStatus: HealthStatus,
    val latestResponseTimeMs: Long,
    val lastCheckedAt: Long,
    val uptimePercent: Float
)
