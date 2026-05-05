package com.webtoapp.core.forcedrun

import com.webtoapp.core.logging.AppLogger














object NativeHardwareController {

    private const val TAG = "NativeHwCtrl"


    var isLoaded: Boolean = false
        private set


    private var capabilityFlags: Int = 0
    @Volatile
    private var capabilityProbeCompleted: Boolean = false
    @Volatile
    private var capabilitySummaryCache: String = "未探测"


    private const val CAP_FLASHLIGHT     = 1 shl 0
    private const val CAP_VIBRATOR       = 1 shl 1
    private const val CAP_BRIGHTNESS     = 1 shl 2
    private const val CAP_CPU_GOVERNOR   = 1 shl 3
    private const val CAP_INPUT_INJECT   = 1 shl 4




    val hasFlashlight: Boolean get() = isLoaded && (capabilityFlags and CAP_FLASHLIGHT) != 0


    val hasVibrator: Boolean get() = isLoaded && (capabilityFlags and CAP_VIBRATOR) != 0


    val hasBrightness: Boolean get() = isLoaded && (capabilityFlags and CAP_BRIGHTNESS) != 0


    val hasCpuGovernor: Boolean get() = isLoaded && (capabilityFlags and CAP_CPU_GOVERNOR) != 0


    val hasInputInjection: Boolean get() = isLoaded && (capabilityFlags and CAP_INPUT_INJECT) != 0

    init {
        try {
            System.loadLibrary("hardware_control")
            isLoaded = true
            AppLogger.i(TAG, "Native hardware_control 库加载成功")
        } catch (e: UnsatisfiedLinkError) {
            isLoaded = false
            AppLogger.w(TAG, "Native hardware_control 库加载失败: ${e.message}")
        }
    }







    fun probeCapabilities(forceRefresh: Boolean = false): String {
        if (!isLoaded) return "Native 库未加载"

        if (!forceRefresh && capabilityProbeCompleted) {
            return capabilitySummaryCache
        }

        capabilityFlags = nativeProbeCapabilities()
        capabilityProbeCompleted = true

        val caps = buildList {
            if (hasFlashlight) add("闪光灯(sysfs)")
            if (hasVibrator) add("震动(sysfs/ff)")
            if (hasBrightness) add("亮度(sysfs)")
            if (hasCpuGovernor) add("CPU调频")
            if (hasInputInjection) add("输入注入")
        }

        val result = if (caps.isEmpty()) {
            "无原生能力可用（无 root 或 sysfs 不可写）"
        } else {
            "可用能力: ${caps.joinToString(", ")}"
        }

        capabilitySummaryCache = result
        AppLogger.i(TAG, result)
        return result
    }




    fun setFlashlight(on: Boolean): Boolean {
        if (!hasFlashlight) return false
        return try {
            nativeSetFlashlight(on).also {
                AppLogger.d(TAG, "原生闪光灯${if (on) "打开" else "关闭"}: $it")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生闪光灯控制异常", e)
            false
        }
    }


    fun startStrobe(intervalMs: Int = 100): Boolean {
        if (!hasFlashlight) return false
        return try {
            nativeStartStrobe(intervalMs).also {
                AppLogger.d(TAG, "原生爆闪启动 (间隔=${intervalMs}ms): $it")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生爆闪启动异常", e)
            false
        }
    }


    fun stopStrobe() {
        if (!isLoaded) return
        try {
            nativeStopStrobe()
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生爆闪停止异常", e)
        }
    }














    fun startMorseCode(text: String, unitMs: Int = 200, loop: Boolean = true): Boolean {
        if (!hasFlashlight) return false
        if (text.isBlank()) return false
        return try {
            nativeStartMorseCode(text, unitMs, loop).also {
                AppLogger.d(TAG, "原生摩斯电码启动 (text='$text', unit=${unitMs}ms, loop=$loop): $it")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生摩斯电码启动异常", e)
            false
        }
    }









    fun startCustomPattern(
        onDurations: IntArray,
        offDurations: IntArray,
        loop: Boolean = true
    ): Boolean {
        if (!hasFlashlight) return false
        if (onDurations.isEmpty() || onDurations.size != offDurations.size) return false
        return try {
            nativeStartCustomPattern(onDurations, offDurations, onDurations.size, loop).also {
                AppLogger.d(TAG, "原生自定义闪烁启动 (${onDurations.size}步, loop=$loop): $it")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生自定义闪烁启动异常", e)
            false
        }
    }




    fun stopPattern() {
        if (!isLoaded) return
        try {
            nativeStopPattern()
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生模式停止异常", e)
        }
    }




    val MORSE_TABLE: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...",
        ':' to "---...", ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.",
        '-' to "-....-", '\"' to ".-..-.", '@' to ".--.-."
    )





    fun textToMorseDisplay(text: String): String {
        return text.uppercase().map { ch ->
            if (ch == ' ') "/"
            else MORSE_TABLE[ch] ?: "?"
        }.joinToString(" ")
    }




    fun vibrate(durationMs: Int): Boolean {
        if (!hasVibrator) return false
        return try {
            nativeVibrate(durationMs)
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生震动异常", e)
            false
        }
    }


    fun startContinuousVibration(): Boolean {
        if (!hasVibrator) return false
        return try {
            nativeStartContinuousVibration().also {
                AppLogger.d(TAG, "原生持续震动启动: $it")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生持续震动异常", e)
            false
        }
    }


    fun stopVibration() {
        if (!isLoaded) return
        try {
            nativeStopVibration()
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生震动停止异常", e)
        }
    }




    fun setBrightness(level: Int): Boolean {
        if (!hasBrightness) return false
        return try {
            nativeSetBrightness(level)
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生亮度设置异常", e)
            false
        }
    }


    fun getMaxBrightness(): Int {
        if (!hasBrightness) return 255
        return try {
            nativeGetMaxBrightness()
        } catch (e: Exception) {
            255
        }
    }




    fun setCpuPerformanceMode(enable: Boolean): Boolean {
        if (!hasCpuGovernor) return false
        return try {
            nativeSetCpuPerformanceMode(enable).also {
                AppLogger.d(TAG, "CPU性能模式${if (enable) "启用" else "禁用"}: $it")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "CPU调频设置异常", e)
            false
        }
    }


    fun startCpuBurn() {
        if (!isLoaded) return
        try {
            nativeStartCpuBurn()
            AppLogger.d(TAG, "原生CPU压测已启动")
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生CPU压测启动异常", e)
        }
    }


    fun stopCpuBurn() {
        if (!isLoaded) return
        try {
            nativeStopCpuBurn()
            AppLogger.d(TAG, "原生CPU压测已停止")
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生CPU压测停止异常", e)
        }
    }




    fun injectVolumeKey(volumeUp: Boolean): Boolean {
        if (!hasInputInjection) return false
        return try {
            nativeInjectVolumeKey(volumeUp)
        } catch (e: Exception) {
            AppLogger.e(TAG, "输入注入异常", e)
            false
        }
    }


    fun injectPowerKey(): Boolean {
        if (!hasInputInjection) return false
        return try {
            nativeInjectPowerKey()
        } catch (e: Exception) {
            AppLogger.e(TAG, "电源键注入异常", e)
            false
        }
    }




    fun setProcessPriority(priority: Int): Boolean {
        if (!isLoaded) return false
        return try {
            nativeSetProcessPriority(priority)
        } catch (e: Exception) {
            AppLogger.e(TAG, "进程优先级设置异常", e)
            false
        }
    }






    fun setIoPriority(ioClass: Int, ioPriority: Int): Boolean {
        if (!isLoaded) return false
        return try {
            nativeSetIoPriority(ioClass, ioPriority)
        } catch (e: Exception) {
            AppLogger.e(TAG, "IO优先级设置异常", e)
            false
        }
    }




    fun cleanup() {
        if (!isLoaded) return
        try {
            nativeCleanup()
            AppLogger.i(TAG, "原生资源已清理")
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生资源清理异常", e)
        }
    }



    private external fun nativeProbeCapabilities(): Int


    private external fun nativeSetFlashlight(on: Boolean): Boolean
    private external fun nativeStartStrobe(intervalMs: Int): Boolean
    private external fun nativeStopStrobe()


    private external fun nativeStartMorseCode(text: String, unitMs: Int, loop: Boolean): Boolean
    private external fun nativeStartCustomPattern(onDurations: IntArray, offDurations: IntArray, count: Int, loop: Boolean): Boolean
    private external fun nativeStopPattern()


    private external fun nativeVibrate(durationMs: Int): Boolean
    private external fun nativeStartContinuousVibration(): Boolean
    private external fun nativeStopVibration()


    private external fun nativeSetBrightness(level: Int): Boolean
    private external fun nativeGetMaxBrightness(): Int


    private external fun nativeSetCpuPerformanceMode(enable: Boolean): Boolean
    private external fun nativeStartCpuBurn()
    private external fun nativeStopCpuBurn()


    private external fun nativeInjectVolumeKey(volumeUp: Boolean): Boolean
    private external fun nativeInjectPowerKey(): Boolean


    private external fun nativeSetProcessPriority(priority: Int): Boolean
    private external fun nativeSetIoPriority(ioClass: Int, ioPriority: Int): Boolean


    private external fun nativeCleanup()
}
