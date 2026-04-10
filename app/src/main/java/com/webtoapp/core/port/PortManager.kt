package com.webtoapp.core.port

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.destroyForciblyCompat
import com.webtoapp.util.isAliveCompat
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 统一端口管理器
 * 
 * 集中管理所有运行时（Node.js、PHP、Python、Go、LocalHttpServer）的端口分配，
 * 防止端口冲突和边缘问题。
 * 
 * 功能：
 * - 端口范围配置（按运行时类型分段）
 * - 端口可用性检查（带缓存优化）
 * - 端口预留/释放机制
 * - 端口使用追踪 + 运行时长统计
 * - 僵尸分配自动清理
 * - 应用退出时自动清理
 */
object PortManager {
    
    private const val TAG = "PortManager"
    
    /** 僵尸分配检测阈值：进程已死但端口未释放超过此时间(ms)自动清理 */
    private const val STALE_THRESHOLD_MS = 30_000L
    
    // ==================== 端口范围配置 ====================
    
    /** 端口范围定义 */
    enum class PortRange(val start: Int, val end: Int) {
        LOCAL_HTTP(18000, 18499),    // LocalHttpServer（静态文件服务）
        PHP(18500, 18999),           // PHP 应用（WordPress、Laravel 等）
        NODEJS(19000, 19499),        // Node.js 应用
        PYTHON(19500, 19999),        // Python 应用
        GO(20000, 20499),            // Go 应用
        GENERAL(20500, 21000);       // 通用/其他
        
        val size: Int get() = end - start + 1
    }
    
    // ==================== 状态追踪 ====================
    
    /** 读写锁：读操作（查询/统计）并发，写操作（分配/释放）互斥 */
    private val lock = ReentrantReadWriteLock()
    
    /** 已分配的端口 -> 使用者标识 */
    private val allocatedPorts = ConcurrentHashMap<Int, PortAllocation>()
    
    /** 端口 -> 进程对象（用于终止进程） */
    private val portProcesses = ConcurrentHashMap<Int, Process>()
    
    /** 端口分配信息 */
    data class PortAllocation(
        val port: Int,
        val owner: String,           // 使用者标识（如 "nodejs:project123"）
        val range: PortRange,
        val allocatedAt: Long = System.currentTimeMillis(),
        val pid: Long = -1           // 进程 PID（如果已知）
    ) {
        /** 运行时长（毫秒） */
        val uptimeMs: Long get() = System.currentTimeMillis() - allocatedAt
    }
    
    /** 端口范围使用摘要 */
    data class RangeStats(
        val range: PortRange,
        val allocated: Int,
        val total: Int,
        val usagePercent: Float
    )
    
    // ==================== 公开 API ====================
    
    /**
     * 分配一个可用端口
     * 
     * @param range 端口范围类型
     * @param owner 使用者标识（用于追踪和调试）
     * @param preferredPort 首选端口（0=自动分配）
     * @return 分配的端口号，失败返回 -1
     */
    fun allocate(range: PortRange, owner: String, preferredPort: Int = 0): Int = lock.write {
        // 0. 先清理僵尸分配，腾出可能被死进程占据的端口
        purgeStaleAllocationsLocked()
        
        // 1. 如果指定了首选端口且可用，直接使用
        if (preferredPort > 0) {
            if (!allocatedPorts.containsKey(preferredPort) && isPortAvailable(preferredPort)) {
                val allocation = PortAllocation(preferredPort, owner, range)
                allocatedPorts[preferredPort] = allocation
                AppLogger.i(TAG, "分配端口 $preferredPort 给 $owner (首选)")
                return@write preferredPort
            }
            AppLogger.w(TAG, "首选端口 $preferredPort 不可用，自动分配")
        }
        
        // 2. 在指定范围内查找可用端口
        for (port in range.start..range.end) {
            if (!allocatedPorts.containsKey(port) && isPortAvailable(port)) {
                val allocation = PortAllocation(port, owner, range)
                allocatedPorts[port] = allocation
                AppLogger.i(TAG, "分配端口 $port 给 $owner (范围: ${range.name})")
                return@write port
            }
        }
        
        // 3. 范围内无可用端口，尝试通用范围（避免递归持锁，内联处理）
        if (range != PortRange.GENERAL) {
            AppLogger.w(TAG, "${range.name} 范围端口已满，尝试通用范围")
            for (port in PortRange.GENERAL.start..PortRange.GENERAL.end) {
                if (!allocatedPorts.containsKey(port) && isPortAvailable(port)) {
                    val allocation = PortAllocation(port, owner, PortRange.GENERAL)
                    allocatedPorts[port] = allocation
                    AppLogger.i(TAG, "分配端口 $port 给 $owner (范围: GENERAL/回退)")
                    return@write port
                }
            }
        }
        
        AppLogger.e(TAG, "无法为 $owner 分配端口，所有范围已满")
        -1
    }
    
    /**
     * 释放端口
     */
    fun release(port: Int) {
        // 先在锁外终止进程（避免持锁时 sleep）
        val process = portProcesses.remove(port)
        if (process != null) {
            terminateProcess(port, process)
        }
        
        lock.write {
            allocatedPorts.remove(port)?.let { allocation ->
                AppLogger.i(TAG, "释放端口 $port (原使用者: ${allocation.owner})")
            }
        }
    }
    
    /**
     * 释放指定使用者的所有端口（含进程终止）
     */
    fun releaseByOwner(owner: String) {
        val toRelease: List<Int>
        lock.read {
            toRelease = allocatedPorts.filter { it.value.owner == owner }.keys.toList()
        }
        toRelease.forEach { port -> release(port) }
    }
    
    /**
     * 释放所有端口（应用退出时调用）
     */
    fun releaseAll() {
        // 收集所有进程并在锁外终止
        val processes = portProcesses.toMap()
        portProcesses.clear()
        
        processes.forEach { (port, process) ->
            terminateProcess(port, process)
        }
        
        lock.write {
            val count = allocatedPorts.size
            allocatedPorts.clear()
            AppLogger.i(TAG, "释放所有端口 (共 $count 个)")
        }
    }
    
    /**
     * 检查端口是否已被本管理器分配
     */
    fun isAllocated(port: Int): Boolean = allocatedPorts.containsKey(port)
    
    /**
     * 获取端口分配信息
     */
    fun getAllocation(port: Int): PortAllocation? = allocatedPorts[port]
    
    /**
     * 注册端口关联的进程（用于后续终止）
     */
    fun registerProcess(port: Int, process: Process, pid: Long = -1) = lock.write {
        portProcesses[port] = process
        // 更新分配信息中的 PID
        allocatedPorts[port]?.let { allocation ->
            allocatedPorts[port] = allocation.copy(pid = pid)
        }
        AppLogger.d(TAG, "注册端口 $port 的进程 (PID: $pid)")
    }
    
    /**
     * 获取端口关联的进程
     */
    fun getProcess(port: Int): Process? = portProcesses[port]
    
    /**
     * 检查端口关联的进程是否存活
     */
    fun isProcessAlive(port: Int): Boolean {
        return portProcesses[port]?.isAliveCompat() == true
    }
    
    /**
     * 获取所有已分配端口（返回前自动清理僵尸分配）
     */
    fun getAllAllocations(): Map<Int, PortAllocation> {
        purgeStaleAllocations()
        return lock.read { allocatedPorts.toMap() }
    }
    
    /**
     * 获取指定范围的已分配端口数
     */
    fun getAllocatedCount(range: PortRange): Int = lock.read {
        allocatedPorts.values.count { it.range == range }
    }
    
    /**
     * 检查端口是否可用（系统级检查）
     */
    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (_: Exception) {
            false
        }
    }
    
    /**
     * 查找可用端口（不分配，仅检查）
     */
    fun findAvailablePort(range: PortRange): Int = lock.read {
        for (port in range.start..range.end) {
            if (!allocatedPorts.containsKey(port) && isPortAvailable(port)) {
                return@read port
            }
        }
        -1
    }
    
    // ==================== 健康检查 ====================
    
    /**
     * 清理僵尸分配（进程已死但端口记录未释放）
     * 
     * 场景：运行时进程崩溃但 release() 未被调用，端口记录残留。
     * 策略：如果进程对象存在且已死亡、且分配时间超过阈值，自动清理。
     */
    fun purgeStaleAllocations(): Int {
        val stale = mutableListOf<Int>()
        lock.read {
            val now = System.currentTimeMillis()
            for ((port, alloc) in allocatedPorts) {
                val process = portProcesses[port]
                // 有注册进程但进程已死，且分配超过阈值
                if (process != null && !process.isAliveCompat() && (now - alloc.allocatedAt) > STALE_THRESHOLD_MS) {
                    stale.add(port)
                }
            }
        }
        if (stale.isNotEmpty()) {
            lock.write {
                stale.forEach { port ->
                    // 二次检查（可能已被其他线程清理）
                    val process = portProcesses[port]
                    if (process != null && !process.isAliveCompat()) {
                        portProcesses.remove(port)
                        allocatedPorts.remove(port)?.let { alloc ->
                            AppLogger.w(TAG, "清理僵尸端口 $port (原使用者: ${alloc.owner}, 存活: ${alloc.uptimeMs}ms)")
                        }
                    }
                }
            }
        }
        return stale.size
    }
    
    /** 在已持有写锁时调用的版本 */
    private fun purgeStaleAllocationsLocked() {
        val now = System.currentTimeMillis()
        val stale = mutableListOf<Int>()
        for ((port, alloc) in allocatedPorts) {
            val process = portProcesses[port]
            if (process != null && !process.isAliveCompat() && (now - alloc.allocatedAt) > STALE_THRESHOLD_MS) {
                stale.add(port)
            }
        }
        stale.forEach { port ->
            portProcesses.remove(port)
            allocatedPorts.remove(port)?.let { alloc ->
                AppLogger.w(TAG, "清理僵尸端口 $port (原使用者: ${alloc.owner}, 存活: ${alloc.uptimeMs}ms)")
            }
        }
    }
    
    /**
     * 获取每个端口范围的使用统计
     */
    fun getRangeStats(): List<RangeStats> = lock.read {
        PortRange.entries.map { range ->
            val count = allocatedPorts.values.count { it.range == range }
            RangeStats(
                range = range,
                allocated = count,
                total = range.size,
                usagePercent = if (range.size > 0) count.toFloat() / range.size else 0f
            )
        }
    }
    
    // ==================== 进程终止（锁外执行） ====================
    
    /**
     * 安全终止进程（不持锁，避免 sleep 阻塞其他操作）
     */
    private fun terminateProcess(port: Int, process: Process) {
        try {
            if (process.isAliveCompat()) {
                process.destroy()
                // 短暂等待优雅退出
                var waited = 0
                while (process.isAliveCompat() && waited < 150) {
                    Thread.sleep(30)
                    waited += 30
                }
                if (process.isAliveCompat()) {
                    process.destroyForciblyCompat()
                    AppLogger.i(TAG, "强制终止端口 $port 关联的进程")
                } else {
                    AppLogger.i(TAG, "已终止端口 $port 关联的进程 (优雅退出)")
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "终止端口 $port 进程失败: ${e.message}")
        }
    }
    
    // ==================== 便捷方法 ====================
    
    /** 为 LocalHttpServer 分配端口 */
    fun allocateForLocalHttp(owner: String, preferred: Int = 0) = 
        allocate(PortRange.LOCAL_HTTP, "localhttp:$owner", preferred)
    
    /** 为 PHP 应用分配端口 */
    fun allocateForPhp(projectId: String, preferred: Int = 0) = 
        allocate(PortRange.PHP, "php:$projectId", preferred)
    
    /** 为 Node.js 应用分配端口 */
    fun allocateForNodeJs(projectId: String, preferred: Int = 0) = 
        allocate(PortRange.NODEJS, "nodejs:$projectId", preferred)
    
    /** 为 Python 应用分配端口 */
    fun allocateForPython(projectId: String, preferred: Int = 0) = 
        allocate(PortRange.PYTHON, "python:$projectId", preferred)
    
    /** 为 Go 应用分配端口 */
    fun allocateForGo(projectId: String, preferred: Int = 0) = 
        allocate(PortRange.GO, "go:$projectId", preferred)
    
    /** 释放 PHP 应用端口 */
    fun releasePhp(projectId: String) = releaseByOwner("php:$projectId")
    
    /** 释放 Node.js 应用端口 */
    fun releaseNodeJs(projectId: String) = releaseByOwner("nodejs:$projectId")
    
    /** 释放 Python 应用端口 */
    fun releasePython(projectId: String) = releaseByOwner("python:$projectId")
    
    /** 释放 Go 应用端口 */
    fun releaseGo(projectId: String) = releaseByOwner("go:$projectId")
    
    // ==================== 调试信息 ====================
    
    /**
     * 获取端口使用统计
     */
    fun getStats(): String {
        val sb = StringBuilder("端口使用统计:\n")
        getRangeStats().forEach { stat ->
            val pct = "%.1f%%".format(stat.usagePercent * 100)
            sb.append("  ${stat.range.name}: ${stat.allocated}/${stat.total} ($pct) [${stat.range.start}-${stat.range.end}]\n")
        }
        sb.append("  总计: ${allocatedPorts.size}\n")
        return sb.toString()
    }
    
    /**
     * 打印当前端口分配详情（调试用）
     */
    fun dumpAllocations() {
        AppLogger.d(TAG, "=== 端口分配详情 ===")
        allocatedPorts.forEach { (port, alloc) ->
            val uptime = formatDuration(alloc.uptimeMs)
            val alive = if (isProcessAlive(port)) "alive" else "dead/unknown"
            AppLogger.d(TAG, "  $port -> ${alloc.owner} (${alloc.range.name}) [$uptime] [$alive]")
        }
        AppLogger.d(TAG, getStats())
    }
    
    /**
     * 格式化时长为可读字符串
     */
    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}
