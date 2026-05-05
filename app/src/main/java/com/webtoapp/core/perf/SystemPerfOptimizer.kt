package com.webtoapp.core.perf

import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
































object SystemPerfOptimizer {

    private const val TAG = "SysPerfOpt"

    @Volatile
    private var initialized = false

    @Volatile
    private var systemOptimized = false

    @Volatile
    private var isThrottling = false


    @Volatile
    private var cachedProfile: SystemProfile? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())




    data class SystemProfile(
        val numCores: Int,
        val numBigCores: Int,
        val numLittleCores: Int,
        val totalRamMb: Int,
        val freeRamMb: Int,
        val maxCpuFreqMhz: Int,
        val maxThermalTempC: Int,
        val fdLimit: Int,
        val uptimeSec: Int
    ) {

        val performanceTier: Int
            get() = when {
                totalRamMb >= 6000 && maxCpuFreqMhz >= 2500 && numBigCores >= 4 -> 2
                totalRamMb >= 3000 && maxCpuFreqMhz >= 1800 -> 1
                else -> 0
            }

        val isLowEnd: Boolean get() = performanceTier == 0
        val isHighEnd: Boolean get() = performanceTier == 2
    }












    fun initSystem(context: Context) {
        if (initialized) return
        initialized = true

        scope.launch {
            try {

                if (NativeSysOptimizer.isAvailable()) {
                    val count = NativeSysOptimizer.optimizeSystem()
                    AppLogger.i(TAG, "System optimization: $count items succeeded")
                    systemOptimized = true


                    cachedProfile = NativeSysOptimizer.getSystemProfile()
                    cachedProfile?.let { prof ->
                        AppLogger.i(TAG, "System profile: " +
                            "${prof.numCores} cores (${prof.numBigCores}B+${prof.numLittleCores}L), " +
                            "${prof.totalRamMb}MB RAM, " +
                            "${prof.maxCpuFreqMhz}MHz max, " +
                            "${prof.maxThermalTempC}°C, " +
                            "tier=${prof.performanceTier}")
                    }
                } else {
                    AppLogger.w(TAG, "Native system optimizer not available")
                }


                mainHandler.post {
                    boostUiThread()
                }


                startThermalMonitor()

            } catch (e: Exception) {
                AppLogger.e(TAG, "System init failed", e)
            }
        }
    }




    fun optimizeActivity(activity: Activity) {
        try {

            activity.window.apply {
                addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                }
            }


            val profile = cachedProfile
            if (profile != null && profile.isLowEnd) {
                try {
                    activity.window.setWindowAnimations(0)
                    AppLogger.d(TAG, "Window animations disabled (low-end device)")
                } catch (e: Exception) {

                }
            }


            if (NativeSysOptimizer.isAvailable()) {
                NativeSysOptimizer.bindToBigCores()
            }

            AppLogger.d(TAG, "Activity optimized: ${activity.javaClass.simpleName}")

        } catch (e: Exception) {
            AppLogger.w(TAG, "Activity optimization failed", e)
        }
    }







    fun optimizeWebViewRuntime(webView: WebView) {
        try {
            val profile = cachedProfile

            webView.settings.apply {

                when (profile?.performanceTier) {
                    0 -> {

                        @Suppress("DEPRECATION")
                        setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            offscreenPreRaster = false
                        }

                    }
                    1 -> {

                        @Suppress("DEPRECATION")
                        setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            offscreenPreRaster = true
                        }
                    }
                    2 -> {

                        @Suppress("DEPRECATION")
                        setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            offscreenPreRaster = true
                        }
                    }
                }
            }


            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)


            if (isThrottling) {
                webView.setLayerType(View.LAYER_TYPE_NONE, null)
                AppLogger.w(TAG, "WebView GPU layer disabled (thermal throttling)")
            }

        } catch (e: Exception) {
            AppLogger.w(TAG, "WebView runtime optimization failed", e)
        }
    }







    private fun startThermalMonitor() {
        scope.launch {
            while (isActive) {
                try {
                    delay(30_000)

                    if (!NativeSysOptimizer.isAvailable()) continue

                    val temp = NativeSysOptimizer.getMaxThermalTemp()
                    if (temp > 0) {
                        val wasThrottling = isThrottling
                        isThrottling = temp >= 80

                        if (isThrottling && !wasThrottling) {
                            AppLogger.w(TAG, "Thermal throttling ON: ${temp}°C")

                            mainHandler.post {

                            }
                        } else if (!isThrottling && wasThrottling) {
                            AppLogger.i(TAG, "Thermal throttling OFF: ${temp}°C")
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }




    private fun boostUiThread() {
        try {

            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            AppLogger.d(TAG, "UI thread priority: URGENT_DISPLAY")
        } catch (e: Exception) {
            AppLogger.w(TAG, "UI thread boost failed", e)
        }
    }







    fun getOptimalCacheSize(context: Context): Long {
        val profile = cachedProfile
        val baseSize = when (profile?.performanceTier) {
            0 -> 50L * 1024 * 1024
            1 -> 100L * 1024 * 1024
            2 -> 200L * 1024 * 1024
            else -> 100L * 1024 * 1024
        }


        return try {
            val cacheDir = context.cacheDir
            val usableSpace = cacheDir.usableSpace
            minOf(baseSize, usableSpace / 10)
        } catch (e: Exception) {
            baseSize
        }
    }







    fun readaheadCriticalFiles(context: Context) {
        if (!NativeSysOptimizer.isAvailable()) return

        scope.launch {
            try {

                val webviewPath = "/system/app/WebViewGoogle/WebViewGoogle.apk"
                val webviewAlt = "/system/app/Chrome/Chrome.apk"
                if (java.io.File(webviewPath).exists()) {
                    NativeSysOptimizer.readaheadFile(webviewPath)
                } else if (java.io.File(webviewAlt).exists()) {
                    NativeSysOptimizer.readaheadFile(webviewAlt)
                }


                val dbPath = context.getDatabasePath("webtoapp.db")?.absolutePath
                if (dbPath != null && java.io.File(dbPath).exists()) {
                    NativeSysOptimizer.readaheadFile(dbPath)

                    val walPath = "$dbPath-wal"
                    if (java.io.File(walPath).exists()) {
                        NativeSysOptimizer.readaheadFile(walPath)
                    }
                }

                AppLogger.d(TAG, "Critical file readahead completed")
            } catch (e: Exception) {

            }
        }
    }




    fun getProfile(): SystemProfile? = cachedProfile


    fun isThermalThrottling(): Boolean = isThrottling


    fun getPerformanceTier(): Int = cachedProfile?.performanceTier ?: 1




    fun release() {
        scope.cancel()
        AppLogger.d(TAG, "SystemPerfOptimizer released")
    }
}

