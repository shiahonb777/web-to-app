package com.webtoapp.core.stats

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit





class AppHealthMonitor(
    private val context: Context,
    private val repository: AppStatsRepository
) {
    companion object {
        private const val TAG = "AppHealthMonitor"
        private const val CHECK_INTERVAL_MS = 30 * 60 * 1000L
        private const val SLOW_THRESHOLD_MS = 2000L
        private const val TIMEOUT_MS = 5000L

        @Volatile
        private var INSTANCE: AppHealthMonitor? = null

        fun getInstance(context: Context, repository: AppStatsRepository): AppHealthMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppHealthMonitor(context.applicationContext, repository).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()


    val allHealthRecords: Flow<List<AppHealthRecord>> = repository.getAllLatestHealthRecords()




    suspend fun checkUrl(appId: Long, url: String): AppHealthRecord {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36")
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseTime = System.currentTimeMillis() - startTime
                val statusCode = response.code
                response.close()

                val status = when {
                    statusCode in 200..399 && responseTime < SLOW_THRESHOLD_MS -> HealthStatus.ONLINE
                    statusCode in 200..399 -> HealthStatus.SLOW
                    else -> HealthStatus.OFFLINE
                }

                val record = AppHealthRecord(
                    appId = appId,
                    url = url,
                    status = status,
                    responseTimeMs = responseTime,
                    httpStatusCode = statusCode
                )
                repository.saveHealthRecord(record)
                AppLogger.d(TAG, "健康检测: $url → $status (${responseTime}ms, HTTP $statusCode)")
                record
            } catch (e: Exception) {
                val record = AppHealthRecord(
                    appId = appId,
                    url = url,
                    status = HealthStatus.OFFLINE,
                    responseTimeMs = -1,
                    errorMessage = e.message
                )
                repository.saveHealthRecord(record)
                AppLogger.w(TAG, "健康检测失败: $url → ${e.message}")
                record
            }
        }
    }




    suspend fun checkApps(apps: List<WebApp>) {
        val webApps = apps.filter { it.appType == AppType.WEB && it.url.startsWith("http") }
        if (webApps.isEmpty()) return

        AppLogger.i(TAG, "开始批量健康检测: ${webApps.size} 个应用")


        val semaphore = kotlinx.coroutines.sync.Semaphore(5)
        coroutineScope {
            webApps.map { app ->
                async {
                    semaphore.withPermit {
                        checkUrl(app.id, app.url)
                    }
                }
            }.awaitAll()
        }


        repository.cleanupOldHealthRecords()
        AppLogger.i(TAG, "批量健康检测完成")
    }




    fun startMonitoring(appsFlow: Flow<List<WebApp>>) {
        stopMonitoring()
        monitorJob = scope.launch {
            while (isActive) {
                val apps = appsFlow.first()
                checkApps(apps)
                delay(CHECK_INTERVAL_MS)
            }
        }
        AppLogger.i(TAG, "健康监控已启动")
    }




    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }





    fun destroy() {
        stopMonitoring()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }




    suspend fun getHealthSummary(appId: Long): AppHealthSummary? {
        val latest = repository.getRecentHealthRecords(appId, 1).firstOrNull() ?: return null
        val uptime = repository.getUptimePercent(appId)
        return AppHealthSummary(
            appId = appId,
            latestStatus = latest.status,
            latestResponseTimeMs = latest.responseTimeMs,
            lastCheckedAt = latest.checkedAt,
            uptimePercent = uptime
        )
    }
}
