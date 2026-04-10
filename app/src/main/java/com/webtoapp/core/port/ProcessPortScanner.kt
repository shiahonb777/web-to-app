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

/**
 * 进程端口扫描器
 * 
 * 扫描系统中正在监听的端口，识别 WebToApp 相关的运行时进程。
 * 
 * 优化：
 * - 并行 HTTP 健康检查（coroutines），扫描 N 个端口耗时 ≈ 单次超时
 * - 响应时间测量，UI 可展示延迟
 * - 进程终止委托给 PortManager.release()，避免重复逻辑
 */
object ProcessPortScanner {
    
    private const val TAG = "ProcessPortScanner"
    
    /** HTTP 健康检查超时（毫秒） */
    private const val HEALTH_CHECK_TIMEOUT_MS = 800
    
    /**
     * 运行中的服务信息
     */
    data class RunningService(
        val port: Int,
        val pid: Long,
        val type: ServiceType,
        val owner: String,           // 使用者标识（如项目名）
        val url: String,             // 服务 URL
        val isResponding: Boolean,   // 是否响应 HTTP 请求
        val processName: String = "",// 进程名（如 node, php）
        val allocatedAt: Long = 0,   // 分配时间戳
        val responseTimeMs: Long = -1// HTTP 响应时间（毫秒），-1=未检测或无响应
    )
    
    /**
     * 服务类型
     */
    enum class ServiceType(val label: String, val color: Long) {
        LOCAL_HTTP("静态服务", 0xFF4CAF50),   // 绿色
        NODEJS("Node.js", 0xFF8BC34A),        // 浅绿
        PHP("PHP", 0xFF9C27B0),               // 紫色
        PYTHON("Python", 0xFF2196F3),         // 蓝色
        GO("Go", 0xFF00BCD4),                 // 青色
        UNKNOWN("未知", 0xFF9E9E9E)           // 灰色
    }
    
    /**
     * HTTP 健康检查结果
     */
    private data class HealthCheckResult(
        val port: Int,
        val responding: Boolean,
        val responseTimeMs: Long     // -1 如果无响应
    )
    
    /**
     * 扫描所有 WebToApp 端口范围内的活跃服务
     * 
     * 所有端口的 HTTP 健康检查并行执行，总耗时 ≈ max(单次超时)
     */
    suspend fun scanAllPorts(context: Context): List<RunningService> = withContext(Dispatchers.IO) {
        val allocations = PortManager.getAllAllocations()
        if (allocations.isEmpty()) return@withContext emptyList()
        
        // 并行执行所有端口的 HTTP 健康检查
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
    
    /**
     * 根据服务类型推断进程名
     */
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
    
    /**
     * 检查端口健康状态（HTTP 响应 + 响应时间测量）
     */
    private fun checkPortHealth(port: Int): HealthCheckResult {
        var conn: HttpURLConnection? = null
        return try {
            val start = System.nanoTime()
            val url = URL("http://127.0.0.1:$port/")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = HEALTH_CHECK_TIMEOUT_MS
                readTimeout = HEALTH_CHECK_TIMEOUT_MS
                requestMethod = "HEAD"  // HEAD 比 GET 更轻量
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
    
    /**
     * 从 owner 字符串推断服务类型
     */
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
    
    /**
     * 提取 owner 名称（去掉前缀）
     */
    private fun extractOwnerName(owner: String): String {
        return owner.substringAfter(":", owner)
    }
    
    /**
     * 终止指定端口的进程
     * 
     * 委托给 PortManager.release()，它会处理进程终止和记录清理
     */
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
    
    /**
     * 终止所有 WebToApp 相关进程
     */
    suspend fun killAllProcesses(context: Context): Int = withContext(Dispatchers.IO) {
        val allocations = PortManager.getAllAllocations()
        val count = allocations.size
        
        // PortManager.releaseAll() 已处理进程终止 + 记录清理
        PortManager.releaseAll()
        
        AppLogger.i(TAG, "已终止 $count 个服务")
        count
    }
}
