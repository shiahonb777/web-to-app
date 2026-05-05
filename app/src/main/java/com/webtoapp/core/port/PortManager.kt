package com.webtoapp.core.port

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.destroyGracefullyCompat
import com.webtoapp.util.destroyForciblyCompat
import com.webtoapp.util.isAliveCompat
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write















object PortManager {

    private const val TAG = "PortManager"


    private const val STALE_THRESHOLD_MS = 30_000L




    enum class PortRange(val start: Int, val end: Int) {
        LOCAL_HTTP(18000, 18499),
        PHP(18500, 18999),
        NODEJS(19000, 19499),
        PYTHON(19500, 19999),
        GO(20000, 20499),
        GENERAL(20500, 21000);

        val size: Int get() = end - start + 1
    }




    private val lock = ReentrantReadWriteLock()


    private val allocatedPorts = ConcurrentHashMap<Int, PortAllocation>()


    private val portProcesses = ConcurrentHashMap<Int, Process>()


    data class PortAllocation(
        val port: Int,
        val owner: String,
        val range: PortRange,
        val allocatedAt: Long = System.currentTimeMillis(),
        val pid: Long = -1
    ) {

        val uptimeMs: Long get() = System.currentTimeMillis() - allocatedAt
    }


    data class RangeStats(
        val range: PortRange,
        val allocated: Int,
        val total: Int,
        val usagePercent: Float
    )











    fun allocate(range: PortRange, owner: String, preferredPort: Int = 0): Int = lock.write {

        purgeStaleAllocationsLocked()


        if (preferredPort > 0) {
            if (!allocatedPorts.containsKey(preferredPort) && isPortAvailable(preferredPort)) {
                val allocation = PortAllocation(preferredPort, owner, range)
                allocatedPorts[preferredPort] = allocation
                AppLogger.i(TAG, "分配端口 $preferredPort 给 $owner (首选)")
                return@write preferredPort
            }
            AppLogger.w(TAG, "首选端口 $preferredPort 不可用，自动分配")
        }


        for (port in range.start..range.end) {
            if (!allocatedPorts.containsKey(port) && isPortAvailable(port)) {
                val allocation = PortAllocation(port, owner, range)
                allocatedPorts[port] = allocation
                AppLogger.i(TAG, "分配端口 $port 给 $owner (范围: ${range.name})")
                return@write port
            }
        }


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




    fun release(port: Int) {

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




    fun releaseByOwner(owner: String) {
        val toRelease: List<Int>
        lock.read {
            toRelease = allocatedPorts.filter { it.value.owner == owner }.keys.toList()
        }
        toRelease.forEach { port -> release(port) }
    }




    fun releaseAll() {

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




    fun isAllocated(port: Int): Boolean = allocatedPorts.containsKey(port)




    fun getAllocation(port: Int): PortAllocation? = allocatedPorts[port]




    fun registerProcess(port: Int, process: Process, pid: Long = -1) = lock.write {
        portProcesses[port] = process

        allocatedPorts[port]?.let { allocation ->
            allocatedPorts[port] = allocation.copy(pid = pid)
        }
        AppLogger.d(TAG, "注册端口 $port 的进程 (PID: $pid)")
    }




    fun getProcess(port: Int): Process? = portProcesses[port]




    fun isProcessAlive(port: Int): Boolean {
        return portProcesses[port]?.isAliveCompat() == true
    }




    fun getAllAllocations(): Map<Int, PortAllocation> {
        purgeStaleAllocations()
        return lock.read { allocatedPorts.toMap() }
    }




    fun getAllocatedCount(range: PortRange): Int = lock.read {
        allocatedPorts.values.count { it.range == range }
    }




    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (_: Exception) {
            false
        }
    }




    fun findAvailablePort(range: PortRange): Int = lock.read {
        for (port in range.start..range.end) {
            if (!allocatedPorts.containsKey(port) && isPortAvailable(port)) {
                return@read port
            }
        }
        -1
    }









    fun purgeStaleAllocations(): Int {
        val stale = mutableListOf<Int>()
        lock.read {
            val now = System.currentTimeMillis()
            for ((port, alloc) in allocatedPorts) {
                val process = portProcesses[port]

                if (process != null && !process.isAliveCompat() && (now - alloc.allocatedAt) > STALE_THRESHOLD_MS) {
                    stale.add(port)
                }
            }
        }
        if (stale.isNotEmpty()) {
            lock.write {
                stale.forEach { port ->

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






    private fun terminateProcess(port: Int, process: Process) {
        try {
            if (process.isAliveCompat()) {
                val exitedGracefully = process.destroyGracefullyCompat(timeoutMs = 150L)
                if (!exitedGracefully && process.isAliveCompat()) {
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




    fun allocateForLocalHttp(owner: String, preferred: Int = 0) =
        allocate(PortRange.LOCAL_HTTP, "localhttp:$owner", preferred)


    fun allocateForPhp(projectId: String, preferred: Int = 0) =
        allocate(PortRange.PHP, "php:$projectId", preferred)


    fun allocateForNodeJs(projectId: String, preferred: Int = 0) =
        allocate(PortRange.NODEJS, "nodejs:$projectId", preferred)


    fun allocateForPython(projectId: String, preferred: Int = 0) =
        allocate(PortRange.PYTHON, "python:$projectId", preferred)


    fun allocateForGo(projectId: String, preferred: Int = 0) =
        allocate(PortRange.GO, "go:$projectId", preferred)


    fun releasePhp(projectId: String) = releaseByOwner("php:$projectId")


    fun releaseNodeJs(projectId: String) = releaseByOwner("nodejs:$projectId")


    fun releasePython(projectId: String) = releaseByOwner("python:$projectId")


    fun releaseGo(projectId: String) = releaseByOwner("go:$projectId")






    fun getStats(): String {
        val sb = StringBuilder("端口使用统计:\n")
        getRangeStats().forEach { stat ->
            val pct = "%.1f%%".format(stat.usagePercent * 100)
            sb.append("  ${stat.range.name}: ${stat.allocated}/${stat.total} ($pct) [${stat.range.start}-${stat.range.end}]\n")
        }
        sb.append("  总计: ${allocatedPorts.size}\n")
        return sb.toString()
    }




    fun dumpAllocations() {
        AppLogger.d(TAG, "=== 端口分配详情 ===")
        allocatedPorts.forEach { (port, alloc) ->
            val uptime = formatDuration(alloc.uptimeMs)
            val alive = if (isProcessAlive(port)) "alive" else "dead/unknown"
            AppLogger.d(TAG, "  $port -> ${alloc.owner} (${alloc.range.name}) [$uptime] [$alive]")
        }
        AppLogger.d(TAG, getStats())
    }




    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}
