package com.webtoapp.core.forcedrun

import com.webtoapp.core.logging.AppLogger

/**
 * 原生硬件控制器 — JNI 桥接层
 * 
 * 通过 C 原生代码直接操作 Linux sysfs / ioctl / input 子系统，
 * 绕过 Android Java API 的限制，在更多设备上实现底层硬件控制。
 * 
 * 使用方式：
 * 1. 调用 [probeCapabilities] 探测设备支持的原生能力
 * 2. 通过 [hasXxx] 属性判断某项功能是否可用
 * 3. 优先使用原生方法，失败时降级到 Java API
 * 
 * ⚠️ 部分功能需要 root 权限才能使用 sysfs 节点
 */
object NativeHardwareController {
    
    private const val TAG = "NativeHwCtrl"
    
    /** Native 库是否成功加载 */
    var isLoaded: Boolean = false
        private set
    
    /** 能力探测结果位掩码 */
    private var capabilityFlags: Int = 0
    
    // 能力位定义 (与 C 层一致)
    private const val CAP_FLASHLIGHT     = 1 shl 0
    private const val CAP_VIBRATOR       = 1 shl 1
    private const val CAP_BRIGHTNESS     = 1 shl 2
    private const val CAP_CPU_GOVERNOR   = 1 shl 3
    private const val CAP_INPUT_INJECT   = 1 shl 4
    
    // ===== 公开能力查询 =====
    
    /** 原生闪光灯控制是否可用 */
    val hasFlashlight: Boolean get() = isLoaded && (capabilityFlags and CAP_FLASHLIGHT) != 0
    
    /** 原生震动控制是否可用 */
    val hasVibrator: Boolean get() = isLoaded && (capabilityFlags and CAP_VIBRATOR) != 0
    
    /** 原生亮度控制是否可用 */
    val hasBrightness: Boolean get() = isLoaded && (capabilityFlags and CAP_BRIGHTNESS) != 0
    
    /** 原生 CPU 调频控制是否可用 */
    val hasCpuGovernor: Boolean get() = isLoaded && (capabilityFlags and CAP_CPU_GOVERNOR) != 0
    
    /** 原生输入事件注入是否可用 */
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
    
    /**
     * 探测设备的原生硬件控制能力
     * 
     * 应在应用启动时调用一次，结果会被缓存。
     * 返回一个能力描述的可读字符串。
     */
    fun probeCapabilities(): String {
        if (!isLoaded) return "Native 库未加载"
        
        capabilityFlags = nativeProbeCapabilities()
        
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
        
        AppLogger.i(TAG, result)
        return result
    }
    
    // ===== 闪光灯 =====
    
    /** 原生闪光灯开关 */
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
    
    /** 原生爆闪模式 */
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
    
    /** 停止爆闪 */
    fun stopStrobe() {
        if (!isLoaded) return
        try {
            nativeStopStrobe()
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生爆闪停止异常", e)
        }
    }
    
    // ===== 闪光灯摩斯电码 / 自定义模式 =====
    
    /**
     * 播放摩斯电码
     * 
     * 将文本转换为摩斯电码并通过闪光灯播放。
     * 支持字母(A-Z)、数字(0-9)、空格和常见标点。
     * 
     * @param text 要发送的文本内容
     * @param unitMs 基本时间单位 (毫秒)。dit=1单位, dah=3单位, 推荐 100~300
     * @param loop 是否循环播放
     * @return 是否成功启动
     */
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
    
    /**
     * 播放自定义闪烁序列
     * 
     * @param onDurations 每个步骤闪光灯亮的时长 (ms)
     * @param offDurations 每个步骤闪光灯灭的时长 (ms)
     * @param loop 是否循环播放
     * @return 是否成功启动
     */
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
    
    /**
     * 停止模式播放 (摩斯电码 / 自定义序列)
     */
    fun stopPattern() {
        if (!isLoaded) return
        try {
            nativeStopPattern()
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生模式停止异常", e)
        }
    }
    
    // ===== 摩斯电码辅助工具 =====
    
    /** 国际摩斯电码映射表 (用于 UI 显示和 Java 层降级) */
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
    
    /**
     * 将文本转换为可显示的摩斯电码字符串
     * 例如: "SOS" -> "... --- ..."
     */
    fun textToMorseDisplay(text: String): String {
        return text.uppercase().map { ch ->
            if (ch == ' ') "/" 
            else MORSE_TABLE[ch] ?: "?"
        }.joinToString(" ")
    }
    
    // ===== 震动 =====
    
    /** 原生震动 */
    fun vibrate(durationMs: Int): Boolean {
        if (!hasVibrator) return false
        return try {
            nativeVibrate(durationMs)
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生震动异常", e)
            false
        }
    }
    
    /** 原生持续震动 */
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
    
    /** 停止原生震动 */
    fun stopVibration() {
        if (!isLoaded) return
        try {
            nativeStopVibration()
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生震动停止异常", e)
        }
    }
    
    // ===== 屏幕亮度 =====
    
    /** 原生设置屏幕亮度 (0 ~ maxBrightness) */
    fun setBrightness(level: Int): Boolean {
        if (!hasBrightness) return false
        return try {
            nativeSetBrightness(level)
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生亮度设置异常", e)
            false
        }
    }
    
    /** 获取最大亮度值 */
    fun getMaxBrightness(): Int {
        if (!hasBrightness) return 255
        return try {
            nativeGetMaxBrightness()
        } catch (e: Exception) {
            255
        }
    }
    
    // ===== CPU 性能 =====
    
    /** 设置 CPU governor (需要 root) */
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
    
    /** 启动原生 CPU 压测（绑定核心 + mmap 内存压力）*/
    fun startCpuBurn() {
        if (!isLoaded) return
        try {
            nativeStartCpuBurn()
            AppLogger.d(TAG, "原生CPU压测已启动")
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生CPU压测启动异常", e)
        }
    }
    
    /** 停止原生 CPU 压测 */
    fun stopCpuBurn() {
        if (!isLoaded) return
        try {
            nativeStopCpuBurn()
            AppLogger.d(TAG, "原生CPU压测已停止")
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生CPU压测停止异常", e)
        }
    }
    
    // ===== 输入事件注入 =====
    
    /** 注入音量键事件 (需要 root 或 input 组权限) */
    fun injectVolumeKey(volumeUp: Boolean): Boolean {
        if (!hasInputInjection) return false
        return try {
            nativeInjectVolumeKey(volumeUp)
        } catch (e: Exception) {
            AppLogger.e(TAG, "输入注入异常", e)
            false
        }
    }
    
    /** 注入电源键事件 (需要 root 或 input 组权限) */
    fun injectPowerKey(): Boolean {
        if (!hasInputInjection) return false
        return try {
            nativeInjectPowerKey()
        } catch (e: Exception) {
            AppLogger.e(TAG, "电源键注入异常", e)
            false
        }
    }
    
    // ===== 进程优先级 =====
    
    /** 设置进程优先级 (-20 最高 到 19 最低) */
    fun setProcessPriority(priority: Int): Boolean {
        if (!isLoaded) return false
        return try {
            nativeSetProcessPriority(priority)
        } catch (e: Exception) {
            AppLogger.e(TAG, "进程优先级设置异常", e)
            false
        }
    }
    
    /** 
     * 设置 I/O 调度优先级
     * @param ioClass 1=RT(实时), 2=BE(最佳努力), 3=IDLE(空闲)
     * @param ioPriority 0-7
     */
    fun setIoPriority(ioClass: Int, ioPriority: Int): Boolean {
        if (!isLoaded) return false
        return try {
            nativeSetIoPriority(ioClass, ioPriority)
        } catch (e: Exception) {
            AppLogger.e(TAG, "IO优先级设置异常", e)
            false
        }
    }
    
    // ===== 统一清理 =====
    
    /** 清理所有原生资源，恢复初始状态 */
    fun cleanup() {
        if (!isLoaded) return
        try {
            nativeCleanup()
            AppLogger.i(TAG, "原生资源已清理")
        } catch (e: Exception) {
            AppLogger.e(TAG, "原生资源清理异常", e)
        }
    }
    
    // ===== Native 声明 =====
    
    private external fun nativeProbeCapabilities(): Int
    
    // 闪光灯
    private external fun nativeSetFlashlight(on: Boolean): Boolean
    private external fun nativeStartStrobe(intervalMs: Int): Boolean
    private external fun nativeStopStrobe()
    
    // 闪光灯摩斯电码 / 自定义模式
    private external fun nativeStartMorseCode(text: String, unitMs: Int, loop: Boolean): Boolean
    private external fun nativeStartCustomPattern(onDurations: IntArray, offDurations: IntArray, count: Int, loop: Boolean): Boolean
    private external fun nativeStopPattern()
    
    // 震动
    private external fun nativeVibrate(durationMs: Int): Boolean
    private external fun nativeStartContinuousVibration(): Boolean
    private external fun nativeStopVibration()
    
    // 亮度
    private external fun nativeSetBrightness(level: Int): Boolean
    private external fun nativeGetMaxBrightness(): Int
    
    // CPU
    private external fun nativeSetCpuPerformanceMode(enable: Boolean): Boolean
    private external fun nativeStartCpuBurn()
    private external fun nativeStopCpuBurn()
    
    // 输入注入
    private external fun nativeInjectVolumeKey(volumeUp: Boolean): Boolean
    private external fun nativeInjectPowerKey(): Boolean
    
    // 进程优先级
    private external fun nativeSetProcessPriority(priority: Int): Boolean
    private external fun nativeSetIoPriority(ioClass: Int, ioPriority: Int): Boolean
    
    // 清理
    private external fun nativeCleanup()
}
