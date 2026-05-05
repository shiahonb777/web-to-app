package com.webtoapp.core.perf

import com.webtoapp.core.logging.AppLogger












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





    fun optimizeSystem(): Int {
        if (!available) return 0
        return try {
            nativeOptimizeSystem()
        } catch (e: Exception) {
            AppLogger.e(TAG, "optimizeSystem failed", e)
            0
        }
    }




    fun bindToBigCores(): Boolean {
        if (!available) return false
        return try {
            nativeBindToBigCores()
        } catch (e: Exception) {
            false
        }
    }






    fun boostThread(tid: Int = 0, nice: Int = -8): Boolean {
        if (!available) return false
        return try {
            nativeBoostThread(tid, nice)
        } catch (e: Exception) {
            false
        }
    }




    fun readaheadFile(path: String) {
        if (!available) return
        try {
            nativeReadaheadFile(path)
        } catch (e: Exception) {

        }
    }




    fun getMaxThermalTemp(): Int {
        if (!available) return 0
        return try {
            nativeGetMaxThermalTemp()
        } catch (e: Exception) {
            0
        }
    }




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





    fun getCpuTopology(): String? {
        if (!available) return null
        return try {
            nativeGetCpuTopology()
        } catch (e: Exception) {
            null
        }
    }





    fun getThermalInfo(): String? {
        if (!available) return null
        return try {
            nativeGetThermalInfo()
        } catch (e: Exception) {
            null
        }
    }



    private external fun nativeOptimizeSystem(): Int
    private external fun nativeBindToBigCores(): Boolean
    private external fun nativeBoostThread(tid: Int, nice: Int): Boolean
    private external fun nativeReadaheadFile(path: String)
    private external fun nativeGetMaxThermalTemp(): Int
    private external fun nativeGetSystemProfile(): IntArray?
    private external fun nativeGetCpuTopology(): String?
    private external fun nativeGetThermalInfo(): String?
}
