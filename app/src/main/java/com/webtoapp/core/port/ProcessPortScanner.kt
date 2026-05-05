package com.webtoapp.core.port

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL











object ProcessPortScanner {

    private const val TAG = "ProcessPortScanner"


    private const val HEALTH_CHECK_TIMEOUT_MS = 800




    data class RunningService(
        val port: Int,
        val pid: Long,
        val type: ServiceType,
        val owner: String,
        val url: String,
        val isResponding: Boolean,
        val processName: String = "",
        val allocatedAt: Long = 0,
        val responseTimeMs: Long = -1
    )




    enum class ServiceType(val label: String, val color: Long) {
        LOCAL_HTTP("静态服务", 0xFF4CAF50),
        NODEJS("Node.js", 0xFF8BC34A),
        PHP("PHP", 0xFF9C27B0),
        PYTHON("Python", 0xFF2196F3),
        GO("Go", 0xFF00BCD4),
        UNKNOWN("未知", 0xFF9E9E9E)
    }




    private data class HealthCheckResult(
        val port: Int,
        val responding: Boolean,
        val responseTimeMs: Long
    )






    suspend fun scanAllPorts(context: Context): List<RunningService> = withContext(Dispatchers.IO) {
        val allocations = PortManager.getAllAllocations()
        if (allocations.isEmpty()) return@withContext emptyList()


        val healthResults: Map<Int, HealthCheckResult> = coroutineScope {
            allocations.keys.map { port ->
                async { checkPortHealth(port) }
            }.awaitAll().associateBy { it.port }
        }

        allocations.map { (port, allocation) ->
            val health = healthResults[port]
            val processAlive = PortManager.isProcessAlive(port)
            val type = inferServiceType(allocation.owner)
            val httpResponding = health?.responding == true

            RunningService(
                port = port,
                pid = allocation.pid,
                type = type,
                owner = extractOwnerName(allocation.owner),
                url = "http://127.0.0.1:$port",
                isResponding = httpResponding || processAlive,
                processName = if (processAlive) getProcessNameFromType(type) else "",
                allocatedAt = allocation.allocatedAt,
                responseTimeMs = health?.responseTimeMs ?: -1
            )
        }.sortedBy { it.port }
    }




    private fun getProcessNameFromType(type: ServiceType): String {
        return when (type) {
            ServiceType.NODEJS -> "node"
            ServiceType.PHP -> "php"
            ServiceType.PYTHON -> "python"
            ServiceType.GO -> "go"
            ServiceType.LOCAL_HTTP -> "http-server"
            ServiceType.UNKNOWN -> ""
        }
    }




    private fun checkPortHealth(port: Int): HealthCheckResult {
        var conn: HttpURLConnection? = null
        return try {
            val start = System.nanoTime()
            val url = URL("http://127.0.0.1:$port/")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = HEALTH_CHECK_TIMEOUT_MS
                readTimeout = HEALTH_CHECK_TIMEOUT_MS
                requestMethod = "HEAD"
                instanceFollowRedirects = false
            }
            val code = conn.responseCode
            val elapsed = (System.nanoTime() - start) / 1_000_000
            HealthCheckResult(port, code in 100..599, elapsed)
        } catch (_: Exception) {
            HealthCheckResult(port, false, -1)
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }




    private fun inferServiceType(owner: String): ServiceType {
        return when {
            owner.startsWith("localhttp:") -> ServiceType.LOCAL_HTTP
            owner.startsWith("nodejs:") -> ServiceType.NODEJS
            owner.startsWith("php:") -> ServiceType.PHP
            owner.startsWith("python:") -> ServiceType.PYTHON
            owner.startsWith("go:") -> ServiceType.GO
            else -> ServiceType.UNKNOWN
        }
    }




    private fun extractOwnerName(owner: String): String {
        return owner.substringAfter(":", owner)
    }






    suspend fun killProcess(port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            PortManager.release(port)
            AppLogger.i(TAG, "已释放端口 $port")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "终止端口 $port 的进程失败", e)
            false
        }
    }




    suspend fun killAllProcesses(context: Context): Int = withContext(Dispatchers.IO) {
        val allocations = PortManager.getAllAllocations()
        val count = allocations.size


        PortManager.releaseAll()

        AppLogger.i(TAG, "已终止 $count 个服务")
        count
    }
}
