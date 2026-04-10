package com.webtoapp.core.perf

import com.webtoapp.core.logging.AppLogger

/**
 * C 级系统优化器 JNI 桥接
 *
 * 提供系统层面的底层性能调优:
 * - CPU 拓扑检测 (big.LITTLE)
 * - 线程亲和性 (绑定大核)
 * - 进程优先级 (nice, OOM score)
 * - I/O 预读
 * - 温度监控
 * - 系统 profile
 */
object NativeSysOptimizer {

    private const val TAG = "NativeSysOpt"

    @Volatile
    private var available = false

    init {
        try {
            System.loadLibrary("sys_optimizer")
            available = true
            AppLogger.i(TAG, "Native system optimizer loaded")
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.w(TAG, "System optimizer not available: ${e.message}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "System optimizer init failed", e)
        }
    }

    fun isAvailable(): Boolean = available

    /**
     * 一键系统优化
     * @return 成功的优化项数量
     */
    fun optimizeSystem(): Int {
        if (!available) return 0
        return try {
            nativeOptimizeSystem()
        } catch (e: Exception) {
            AppLogger.e(TAG, "optimizeSystem failed", e)
            0
        }
    }

    /**
     * 将当前线程绑定到大核
     */
    fun bindToBigCores(): Boolean {
        if (!available) return false
        return try {
            nativeBindToBigCores()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 提升线程优先级
     * @param tid 线程 ID (0=当前线程)
     * @param nice 优先级 (-20 最高, 19 最低)
     */
    fun boostThread(tid: Int = 0, nice: Int = -8): Boolean {
        if (!available) return false
        return try {
            nativeBoostThread(tid, nice)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 预读文件到页缓存
     */
    fun readaheadFile(path: String) {
        if (!available) return
        try {
            nativeReadaheadFile(path)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * 获取 CPU 最高温度 (°C)
     */
    fun getMaxThermalTemp(): Int {
        if (!available) return 0
        return try {
            nativeGetMaxThermalTemp()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取完整系统 profile
     */
    fun getSystemProfile(): SystemPerfOptimizer.SystemProfile? {
        if (!available) return null
        return try {
            val arr = nativeGetSystemProfile() ?: return null
            if (arr.size < 9) return null
            SystemPerfOptimizer.SystemProfile(
                numCores = arr[0],
                numBigCores = arr[1],
                numLittleCores = arr[2],
                totalRamMb = arr[3],
                freeRamMb = arr[4],
                maxCpuFreqMhz = arr[5],
                maxThermalTempC = arr[6],
                fdLimit = arr[7],
                uptimeSec = arr[8]
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "getSystemProfile failed", e)
            null
        }
    }

    /**
     * 获取 CPU 拓扑字符串
     * 格式: "coreId:freqKhz:isBig,..."
     */
    fun getCpuTopology(): String? {
        if (!available) return null
        return try {
            nativeGetCpuTopology()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取温度信息字符串
     * 格式: "zoneId:tempMilliC:type,..."
     */
    fun getThermalInfo(): String? {
        if (!available) return null
        return try {
            nativeGetThermalInfo()
        } catch (e: Exception) {
            null
        }
    }

    // ==================== Native 方法 ====================

    private external fun nativeOptimizeSystem(): Int
    private external fun nativeBindToBigCores(): Boolean
    private external fun nativeBoostThread(tid: Int, nice: Int): Boolean
    private external fun nativeReadaheadFile(path: String)
    private external fun nativeGetMaxThermalTemp(): Int
    private external fun nativeGetSystemProfile(): IntArray?
    private external fun nativeGetCpuTopology(): String?
    private external fun nativeGetThermalInfo(): String?
}
