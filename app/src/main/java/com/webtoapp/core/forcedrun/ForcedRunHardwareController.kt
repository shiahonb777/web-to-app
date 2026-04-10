package com.webtoapp.core.forcedrun

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.webtoapp.core.logging.AppLogger
import android.view.WindowManager
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * 强制运行硬件控制器
 * 
 * 实现各种"黑科技"硬件控制功能，采用双层架构：
 * - **Native 层 (C)**: 通过 Linux sysfs / ioctl / input 子系统直接控制硬件
 * - **Java 层 (Kotlin)**: 通过 Android API 控制硬件（兜底方案）
 * 
 * 策略：优先尝试 Native 底层控制，失败时自动降级到 Java API
 * ⚠️ 警告：这些功能可能会对设备造成影响，请谨慎使用
 */
@SuppressLint("StaticFieldLeak")
class ForcedRunHardwareController(private val context: Context) {
    
    companion object {
        private const val TAG = "ForcedRunHardware"
        
        @Volatile
        private var instance: ForcedRunHardwareController? = null
        
        fun getInstance(context: Context): ForcedRunHardwareController {
            return instance ?: synchronized(this) {
                instance ?: ForcedRunHardwareController(context.applicationContext).also {
                    instance = it
                    // 初始化时探测原生硬件能力
                    it.probeNativeCapabilities()
                }
            }
        }
    }
    
    /** 是否优先使用原生控制 (可在运行时切换) */
    var preferNative: Boolean = true
    
    /** 原生能力探测结果描述 */
    var nativeCapabilityInfo: String = "未探测"
        private set
    
    /**
     * 探测原生硬件控制能力
     */
    private fun probeNativeCapabilities() {
        nativeCapabilityInfo = NativeHardwareController.probeCapabilities()
        AppLogger.i(TAG, "原生能力: $nativeCapabilityInfo")
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private var strobeJob: Job? = null
    private var vibrationJob: Job? = null
    private var screenRotationJob: Job? = null
    private var maxVolumeJob: Job? = null
    private var muteJob: Job? = null
    
    private var originalVolume: Int = -1
    private var originalRingerMode: Int = -1
    private var isFlashlightOn = false
    private var activityRef: WeakReference<Activity>? = null
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var screenWakeLock: PowerManager.WakeLock? = null
    
    // Volume变化广播接收器
    private var volumeChangeReceiver: BroadcastReceiver? = null
    private var isForceMaxVolumeEnabled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 网络控制状态
    private var originalWifiState: Boolean? = null
    private var originalBluetoothState: Boolean? = null
    private var hotspotReservation: Any? = null // WifiManager.LocalOnlyHotspotReservation
    private var isHotspotActive = false
    private var customAlarmJob: Job? = null
    
    // ===== 音量控制 =====
    
    /**
     * 强制持续最大音量
     * 持续监控并保持音量最大，防止用户调节
     * 使用广播监听 + 高频轮询双重保障
     */
    fun forceMaxVolume() {
        stopMaxVolume()
        isForceMaxVolumeEnabled = true
        
        try {
            // Save原始音量
            if (originalVolume == -1) {
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
            
            // 立即设置最大音量
            setAllVolumesToMax()
            
            // 注册音量变化广播监听器 - 实时响应音量变化
            registerVolumeChangeReceiver()
            
            // Start高频持续监控（50ms间隔，确保用户无法调低）
            maxVolumeJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isForceMaxVolumeEnabled) {
                    try {
                        setAllVolumesToMax()
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "持续设置音量失败", e)
                    }
                    delay(50) // 每50ms检查一次，用户几乎无法感知音量变化
                }
            }
            
            AppLogger.d(TAG, "持续最大音量已启动（广播监听 + 50ms轮询）")
        } catch (e: Exception) {
            AppLogger.e(TAG, "设置音量失败", e)
        }
    }
    
    /**
     * 注册音量变化广播接收器
     */
    private fun registerVolumeChangeReceiver() {
        if (volumeChangeReceiver != null) return
        
        volumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isForceMaxVolumeEnabled && intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    // Volume发生变化，立即恢复到最大
                    mainHandler.post {
                        setAllVolumesToMax()
                    }
                }
            }
        }
        
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(volumeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(volumeChangeReceiver, filter)
        }
        
        AppLogger.d(TAG, "音量变化广播接收器已注册")
    }
    
    /**
     * 注销音量变化广播接收器
     */
    private fun unregisterVolumeChangeReceiver() {
        volumeChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
                AppLogger.d(TAG, "音量变化广播接收器已注销")
            } catch (e: Exception) {
                AppLogger.e(TAG, "注销音量广播接收器失败", e)
            }
        }
        volumeChangeReceiver = null
    }
    
    /**
     * 设置所有音量到最大
     */
    private fun setAllVolumesToMax() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
    }
    
    /**
     * 停止持续最大音量
     */
    private fun stopMaxVolume() {
        isForceMaxVolumeEnabled = false
        maxVolumeJob?.cancel()
        maxVolumeJob = null
        unregisterVolumeChangeReceiver()
    }
    
    /**
     * 恢复原始音量
     */
    fun restoreVolume() {
        stopMaxVolume()
        try {
            if (originalVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                originalVolume = -1
                AppLogger.d(TAG, "音量已恢复")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "恢复音量失败", e)
        }
    }
    
    // ===== 震动控制 =====
    
    /**
     * 开始持续最大震动
     */
    fun startMaxVibration() {
        stopVibration()
        
        // 优先使用原生持续震动（sysfs / force-feedback）
        if (preferNative && NativeHardwareController.startContinuousVibration()) {
            AppLogger.d(TAG, "持续震动已启动 (Native)")
            return
        }
        
        // 降级: Java Vibrator API — 使用 waveform 无缝循环
        vibrationJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 使用 waveform 模式实现无间隔持续震动
                    // timings: [震动, 间隔, 震动, 间隔...]
                    // amplitudes: [强度, 0, 强度, 0...]
                    val timings = longArrayOf(2000, 10, 2000, 10)
                    val amplitudes = intArrayOf(255, 0, 255, 0)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, 0) // repeat=0
                    vibrator.vibrate(effect)
                    AppLogger.d(TAG, "持续震动已启动 (Java Waveform)")
                } else {
                    // 旧版 API 循环震动
                    while (isActive) {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 2000, 10), 0)
                        delay(4000)
                    }
                }
                
                // 保持协程存活等待取消
                while (isActive) { delay(1000) }
            } catch (e: Exception) {
                AppLogger.e(TAG, "震动失败", e)
            }
        }
        
        AppLogger.d(TAG, "持续震动已启动 (Java)")
    }
    
    /**
     * 停止震动
     */
    fun stopVibration() {
        // 停止原生震动
        NativeHardwareController.stopVibration()
        // 停止 Java 震动
        vibrationJob?.cancel()
        vibrationJob = null
        vibrator.cancel()
        AppLogger.d(TAG, "震动已停止")
    }
    
    // ===== 闪光灯控制 =====
    
    /**
     * 打开闪光灯
     */
    fun turnOnFlashlight() {
        // 优先尝试原生 sysfs 控制
        if (preferNative && NativeHardwareController.setFlashlight(true)) {
            isFlashlightOn = true
            AppLogger.d(TAG, "闪光灯已打开 (Native)")
            return
        }
        
        // 降级: Java Camera2 API
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, true)
            isFlashlightOn = true
            AppLogger.d(TAG, "闪光灯已打开 (Java)")
        } catch (e: CameraAccessException) {
            AppLogger.e(TAG, "打开闪光灯失败", e)
        }
    }
    
    /**
     * 关闭闪光灯
     */
    fun turnOffFlashlight() {
        stopStrobeMode()
        
        // 尝试原生关闭
        if (preferNative) {
            NativeHardwareController.setFlashlight(false)
        }
        
        // Java 层也关闭（双保险）
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, false)
            isFlashlightOn = false
            AppLogger.d(TAG, "闪光灯已关闭")
        } catch (e: CameraAccessException) {
            AppLogger.e(TAG, "关闭闪光灯失败", e)
        }
    }
    
    /**
     * 启动爆闪模式
     */
    fun startStrobeMode() {
        stopStrobeMode()
        
        // 优先使用原生爆闪（C 线程级别，更精确的时序控制）
        if (preferNative && NativeHardwareController.startStrobe(100)) {
            AppLogger.d(TAG, "爆闪模式已启动 (Native, 100ms)")
            return
        }
        
        // 降级: Kotlin 协程控制
        strobeJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@launch
                var isOn = false
                
                while (isActive) {
                    isOn = !isOn
                    cameraManager.setTorchMode(cameraId, isOn)
                    delay(100) // 100ms 间隔，每秒闪烁10次
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "爆闪模式失败", e)
            }
        }
        
        AppLogger.d(TAG, "爆闪模式已启动 (Java)")
    }
    
    /**
     * 停止所有闪光灯模式（爆闪 / 摩斯电码 / 自定义）
     */
    fun stopStrobeMode() {
        // 停止原生爆闪
        NativeHardwareController.stopStrobe()
        // 停止原生模式播放 (摩斯电码/自定义)
        NativeHardwareController.stopPattern()
        // 停止 Java 爆闪/模式
        strobeJob?.cancel()
        strobeJob = null
    }
    
    // ===== 闪光灯摩斯电码模式 =====
    
    /**
     * 启动摩斯电码闪光灯模式
     * 
     * 将文本自动编码为摩斯电码，通过闪光灯的亮灭表示：
     * - 短闪 (dit/点): 1 个时间单位
     * - 长亮 (dah/划): 3 个时间单位
     * - 元素间隔: 1 个时间单位
     * - 字符间隔: 3 个时间单位
     * - 单词间隔: 7 个时间单位
     * 
     * @param text   要发送的文本 (支持 A-Z, 0-9, 空格, 常见标点)
     * @param unitMs 基本时间单位 (毫秒), 推荐 100~300, 值越小速度越快
     * @param loop   是否循环播放
     */
    fun startMorseCodeMode(text: String, unitMs: Int = 200, loop: Boolean = true) {
        stopStrobeMode()
        
        if (text.isBlank()) {
            AppLogger.w(TAG, "摩斯电码文本为空")
            return
        }
        
        // 优先使用原生摩斯电码引擎（C 线程，精确时序）
        if (preferNative && NativeHardwareController.startMorseCode(text, unitMs, loop)) {
            AppLogger.d(TAG, "摩斯电码已启动 (Native): '$text' unit=${unitMs}ms loop=$loop")
            return
        }
        
        // 降级: Kotlin 协程实现摩斯电码
        strobeJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@launch
                val morseTable = NativeHardwareController.MORSE_TABLE
                
                val ditMs = unitMs.toLong()
                val dahMs = unitMs * 3L
                val elementGap = unitMs.toLong()
                val charGap = unitMs * 3L
                val wordGap = unitMs * 7L
                
                do {
                    for (ch in text.uppercase()) {
                        if (!isActive) return@launch
                        
                        if (ch == ' ') {
                            delay(wordGap)
                            continue
                        }
                        
                        val code = morseTable[ch] ?: continue
                        
                        for ((idx, symbol) in code.withIndex()) {
                            if (!isActive) return@launch
                            
                            // 亮
                            cameraManager.setTorchMode(cameraId, true)
                            delay(if (symbol == '.') ditMs else dahMs)
                            
                            // 灭
                            cameraManager.setTorchMode(cameraId, false)
                            delay(if (idx < code.length - 1) elementGap else charGap)
                        }
                    }
                } while (isActive && loop)
                
            } catch (e: Exception) {
                AppLogger.e(TAG, "摩斯电码播放失败", e)
            } finally {
                try {
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {}
            }
        }
        
        AppLogger.d(TAG, "摩斯电码已启动 (Java): '$text' unit=${unitMs}ms loop=$loop")
    }
    
    /**
     * 快速发送 SOS 求救信号
     * 
     * SOS = "... --- ..." (3短 3长 3短)
     * 国际通用紧急求救信号
     * 
     * @param unitMs 基本时间单位，默认 200ms
     */
    fun startSosMode(unitMs: Int = 200) {
        startMorseCodeMode("SOS", unitMs, loop = true)
    }
    
    /**
     * 启动自定义闪光灯序列模式
     * 
     * 允许用户完全自定义每一步的亮灯和灭灯时长，
     * 可以创造任意节奏和效果。
     * 
     * @param onDurations  每步亮灯时长数组 (毫秒)
     * @param offDurations 每步灭灯时长数组 (毫秒)
     * @param loop         是否循环播放
     */
    fun startCustomFlashPattern(
        onDurations: IntArray, 
        offDurations: IntArray, 
        loop: Boolean = true
    ) {
        stopStrobeMode()
        
        if (onDurations.isEmpty() || onDurations.size != offDurations.size) {
            AppLogger.w(TAG, "自定义闪烁序列参数无效")
            return
        }
        
        // 优先使用原生自定义模式
        if (preferNative && NativeHardwareController.startCustomPattern(onDurations, offDurations, loop)) {
            AppLogger.d(TAG, "自定义闪烁已启动 (Native): ${onDurations.size}步 loop=$loop")
            return
        }
        
        // 降级: Kotlin 协程
        strobeJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@launch
                
                do {
                    for (i in onDurations.indices) {
                        if (!isActive) return@launch
                        
                        if (onDurations[i] > 0) {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(onDurations[i].toLong())
                        }
                        
                        if (!isActive) return@launch
                        
                        if (offDurations[i] > 0) {
                            cameraManager.setTorchMode(cameraId, false)
                            delay(offDurations[i].toLong())
                        }
                    }
                } while (isActive && loop)
                
            } catch (e: Exception) {
                AppLogger.e(TAG, "自定义闪烁播放失败", e)
            } finally {
                try {
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {}
            }
        }
        
        AppLogger.d(TAG, "自定义闪烁已启动 (Java): ${onDurations.size}步 loop=$loop")
    }
    
    // ===== 预设闪烁效果 =====
    
    /**
     * 心跳模式: 模拟心跳节奏的双闪效果
     * 
     * 节奏: 快闪-快闪-长暗-快闪-快闪-长暗...
     */
    fun startHeartbeatMode() {
        startCustomFlashPattern(
            onDurations  = intArrayOf(100, 100, 0),
            offDurations = intArrayOf(100, 300, 800),
            loop = true
        )
        AppLogger.d(TAG, "心跳模式已启动")
    }
    
    /**
     * 呼吸灯模式: 渐快渐慢的闪烁节奏
     * 
     * 从慢到快再到慢，模拟呼吸的节奏
     */
    fun startBreathingMode() {
        startCustomFlashPattern(
            onDurations  = intArrayOf(500, 400, 300, 200, 100, 50, 50, 100, 200, 300, 400, 500),
            offDurations = intArrayOf(500, 400, 300, 200, 100, 50, 50, 100, 200, 300, 400, 800),
            loop = true
        )
        AppLogger.d(TAG, "呼吸灯模式已启动")
    }
    
    /**
     * 紧急三闪模式: 连续三次快闪，用于紧急信号
     * 
     * 节奏: 闪-闪-闪-长暗-闪-闪-闪-长暗...
     */
    fun startEmergencyTripleFlash() {
        startCustomFlashPattern(
            onDurations  = intArrayOf(100, 100, 100, 0),
            offDurations = intArrayOf(100, 100, 100, 1000),
            loop = true
        )
        AppLogger.d(TAG, "紧急三闪模式已启动")
    }
    
    /**
     * 获取摩斯电码的显示文本
     * 
     * 将输入文本转换为对应的摩斯电码符号，用于 UI 显示。
     * 例如: "SOS" → "... --- ..."
     * 
     * @param text 输入文本
     * @return 摩斯电码字符串
     */
    fun getMorseCodeDisplay(text: String): String {
        return NativeHardwareController.textToMorseDisplay(text)
    }
    
    // ===== 性能模式 =====
    
    private val performanceThreads = mutableListOf<Thread>()
    @Volatile
    private var isPerformanceModeRunning = false
    
    /**
     * 启动最大性能模式（高CPU/内存占用）
     * ⚠️ 警告：这会消耗大量电池和产生热量
     * 
     * 使用原生线程而非协程，确保100%占用每个CPU核心
     * 多种计算任务：浮点运算、整数运算、内存操作、数组操作
     */
    fun startMaxPerformanceMode() {
        stopMaxPerformanceMode()
        isPerformanceModeRunning = true
        
        // 1. 尝试原生 CPU governor 设为 performance
        if (preferNative) {
            NativeHardwareController.setCpuPerformanceMode(true)
            // 设置进程最高优先级
            NativeHardwareController.setProcessPriority(-20)
            // 设置 I/O 为实时优先级
            NativeHardwareController.setIoPriority(1, 0) // RT class, priority 0
        }
        
        // 2. 启动 CPU 压测线程
        // 优先使用原生压测（C 线程 + CPU 核心绑定 + mmap 内存压力）
        if (preferNative && NativeHardwareController.isLoaded) {
            NativeHardwareController.startCpuBurn()
            AppLogger.d(TAG, "最大性能模式已启动 (Native CPU burn + governor)")
        } else {
            // 降级: Java 线程压测
            val cpuCount = Runtime.getRuntime().availableProcessors()
            
            for (i in 0 until cpuCount) {
                val thread = Thread {
                    Thread.currentThread().priority = Thread.MAX_PRIORITY
                    
                    val memoryBlock = ByteArray(1024 * 1024)
                    val intArray = IntArray(10000)
                    val doubleArray = DoubleArray(10000)
                    
                    var counter = 0L
                    var floatResult = 0.0
                    var intResult = 0
                    
                    while (isPerformanceModeRunning && !Thread.currentThread().isInterrupted) {
                        try {
                            for (j in 0 until 50000) {
                                floatResult += Math.sin(j.toDouble()) * Math.cos(j.toDouble())
                                floatResult += Math.sqrt(Math.abs(floatResult))
                                floatResult += Math.pow(1.0001, j.toDouble() % 100)
                            }
                            
                            for (j in 0 until 100000) {
                                intResult = intResult xor (j * 31)
                                intResult = intResult.rotateLeft(j % 32)
                                counter++
                            }
                            
                            for (j in memoryBlock.indices step 64) {
                                memoryBlock[j] = (counter and 0xFF).toByte()
                                intResult += memoryBlock[j].toInt()
                            }
                            
                            for (j in intArray.indices) {
                                intArray[j] = intResult + j
                                doubleArray[j] = floatResult + j
                            }
                            
                            if (counter % 100 == 0L) {
                                intArray.shuffle()
                                intArray.sort()
                            }
                            
                            if (floatResult == Double.MAX_VALUE && intResult == Int.MAX_VALUE) {
                                AppLogger.w(TAG, "Performance: $floatResult, $intResult")
                            }
                        } catch (e: Exception) {
                            // 忽略异常继续运行
                        }
                    }
                }.apply {
                    name = "MaxPerformance-$i"
                    isDaemon = true
                    start()
                }
                performanceThreads.add(thread)
            }
            
            // 额外启动内存压力线程
            val memoryPressureThread = Thread {
                Thread.currentThread().priority = Thread.NORM_PRIORITY
                val memoryBlocks = mutableListOf<ByteArray>()
                
                while (isPerformanceModeRunning && !Thread.currentThread().isInterrupted) {
                    try {
                        if (memoryBlocks.size < 50) {
                            memoryBlocks.add(ByteArray(1024 * 512))
                        } else {
                            if (memoryBlocks.isNotEmpty()) {
                                memoryBlocks.removeAt((Math.random() * memoryBlocks.size).toInt())
                            }
                        }
                        
                        memoryBlocks.forEach { block ->
                            for (i in block.indices step 4096) {
                                block[i] = (System.nanoTime() and 0xFF).toByte()
                            }
                        }
                        
                        Thread.sleep(10)
                    } catch (e: Exception) {
                        // 忽略异常继续运行
                    }
                }
                
                memoryBlocks.clear()
            }.apply {
                name = "MemoryPressure"
                isDaemon = true
                start()
            }
            performanceThreads.add(memoryPressureThread)
            
            AppLogger.d(TAG, "最大性能模式已启动 (Java, ${cpuCount} 核心 + 内存压力)")
        }
    }
    
    /**
     * 停止最大性能模式
     */
    fun stopMaxPerformanceMode() {
        isPerformanceModeRunning = false
        
        // 停止原生 CPU burn + 恢复 governor
        NativeHardwareController.stopCpuBurn()
        if (preferNative) {
            NativeHardwareController.setCpuPerformanceMode(false)
        }
        
        // 停止 Java 线程
        performanceThreads.forEach { thread ->
            try {
                thread.interrupt()
            } catch (e: Exception) {
                // 忽略
            }
        }
        performanceThreads.clear()
        
        System.gc()
        
        AppLogger.d(TAG, "最大性能模式已停止")
    }
    
    // ===== 缓存清理 =====
    
    /**
     * 清理应用缓存
     */
    fun clearAppCache() {
        try {
            val cacheDir = context.cacheDir
            deleteDir(cacheDir)
            
            context.externalCacheDir?.let { deleteDir(it) }
            
            AppLogger.d(TAG, "应用缓存已清理")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清理缓存失败", e)
        }
    }
    
    private fun deleteDir(dir: java.io.File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            children?.forEach { child ->
                val success = deleteDir(java.io.File(dir, child))
                if (!success) return false
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }
    
    // ===== 静音模式 =====
    
    // 静音模式广播接收器
    private var muteVolumeChangeReceiver: BroadcastReceiver? = null
    private var isForceMuteModeEnabled = false
    
    /**
     * 强制持续静音模式
     * 持续监控并保持音量最小，防止用户调高
     * 使用广播监听 + 高频轮询双重保障
     */
    fun forceMuteMode() {
        stopMuteMode()
        isForceMuteModeEnabled = true
        
        try {
            if (originalRingerMode == -1) {
                originalRingerMode = audioManager.ringerMode
            }
            
            // 立即设置静音
            setAllVolumesToMute()
            
            // 注册音量变化广播监听器 - 实时响应音量变化
            registerMuteVolumeChangeReceiver()
            
            // Start高频持续监控（50ms间隔，确保用户无法调高）
            muteJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isForceMuteModeEnabled) {
                    try {
                        setAllVolumesToMute()
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "持续设置静音失败", e)
                    }
                    delay(50) // 每50ms检查一次，用户几乎无法感知音量变化
                }
            }
            
            AppLogger.d(TAG, "持续静音模式已启动（广播监听 + 50ms轮询）")
        } catch (e: Exception) {
            AppLogger.e(TAG, "设置静音模式失败", e)
        }
    }
    
    /**
     * 注册静音模式音量变化广播接收器
     */
    private fun registerMuteVolumeChangeReceiver() {
        if (muteVolumeChangeReceiver != null) return
        
        muteVolumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isForceMuteModeEnabled && intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    // Volume发生变化，立即恢复到最小
                    mainHandler.post {
                        setAllVolumesToMute()
                    }
                }
            }
        }
        
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(muteVolumeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(muteVolumeChangeReceiver, filter)
        }
        
        AppLogger.d(TAG, "静音模式音量广播接收器已注册")
    }
    
    /**
     * 注销静音模式音量变化广播接收器
     */
    private fun unregisterMuteVolumeChangeReceiver() {
        muteVolumeChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
                AppLogger.d(TAG, "静音模式音量广播接收器已注销")
            } catch (e: Exception) {
                AppLogger.e(TAG, "注销静音模式音量广播接收器失败", e)
            }
        }
        muteVolumeChangeReceiver = null
    }
    
    /**
     * 设置所有音量到静音
     */
    private fun setAllVolumesToMute() {
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (e: Exception) {
            // 某些设备可能不支持直接设置铃声模式
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
    }
    
    /**
     * 停止持续静音模式
     */
    private fun stopMuteMode() {
        isForceMuteModeEnabled = false
        muteJob?.cancel()
        muteJob = null
        unregisterMuteVolumeChangeReceiver()
    }
    
    /**
     * 恢复铃声模式
     */
    fun restoreRingerMode() {
        stopMuteMode()
        try {
            if (originalRingerMode != -1) {
                audioManager.ringerMode = originalRingerMode
                originalRingerMode = -1
                AppLogger.d(TAG, "铃声模式已恢复")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "恢复铃声模式失败", e)
        }
    }
    
    // ===== 屏幕控制 =====
    
    /**
     * 设置目标 Activity（用于屏幕控制）
     */
    fun setTargetActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }
    
    /**
     * 强制屏幕不休眠
     */
    fun forceScreenAwake() {
        try {
            activityRef?.get()?.let { activity ->
                activity.runOnUiThread {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            
            // 额外使用 WakeLock
            if (screenWakeLock == null) {
                @Suppress("DEPRECATION")
                screenWakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "WebToApp:ScreenAwake"
                )
            }
            screenWakeLock?.acquire(24 * 60 * 60 * 1000L) // 最多24小时
            
            AppLogger.d(TAG, "屏幕常亮已启用")
        } catch (e: Exception) {
            AppLogger.e(TAG, "设置屏幕常亮失败", e)
        }
    }
    
    /**
     * 释放屏幕常亮
     */
    fun releaseScreenAwake() {
        try {
            activityRef?.get()?.let { activity ->
                activity.runOnUiThread {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            
            screenWakeLock?.let {
                if (it.isHeld) it.release()
            }
            screenWakeLock = null
            
            AppLogger.d(TAG, "屏幕常亮已释放")
        } catch (e: Exception) {
            AppLogger.e(TAG, "释放屏幕常亮失败", e)
        }
    }
    
    /**
     * 强制屏幕持续翻转
     * 四个角度循环：0°(竖屏) -> 90°(横屏) -> 180°(倒置竖屏) -> 270°(倒置横屏)
     */
    fun startScreenRotation() {
        stopScreenRotation()
        
        // 四个方向循环：竖屏 -> 横屏 -> 倒置竖屏 -> 倒置横屏
        val orientations = listOf(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,           // 0° 竖屏
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,          // 90° 横屏
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,   // 180° 倒置竖屏
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE   // 270° 倒置横屏
        )
        
        screenRotationJob = CoroutineScope(Dispatchers.Main).launch {
            var index = 0
            while (isActive) {
                try {
                    activityRef?.get()?.let { activity ->
                        activity.requestedOrientation = orientations[index]
                        index = (index + 1) % orientations.size
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "屏幕翻转失败", e)
                }
                delay(2000) // 每2秒翻转一次
            }
        }
        
        AppLogger.d(TAG, "屏幕四向循环翻转已启动")
    }
    
    /**
     * 停止屏幕翻转
     */
    fun stopScreenRotation() {
        screenRotationJob?.cancel()
        screenRotationJob = null
        
        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
    
    // ===== 按键屏蔽标志 =====
    
    @Volatile
    var isBlockVolumeKeys: Boolean = false
        private set
    
    @Volatile
    var isBlockPowerKey: Boolean = false
        private set
    
    @Volatile
    var isBlockTouch: Boolean = false
        private set
    
    @Volatile
    var isBlackScreenMode: Boolean = false
        private set
    
    /**
     * 启用音量键屏蔽
     * 通过 AccessibilityService.onKeyEvent 拦截硬件按键
     */
    fun enableBlockVolumeKeys() {
        isBlockVolumeKeys = true
        // 路由到 AccessibilityService（可靠拦截硬件按键）
        ForcedRunAccessibilityService.blockVolumeKeys = true
        AppLogger.d(TAG, "音量键屏蔽已启用 (AccessibilityService)")
    }
    
    /**
     * 禁用音量键屏蔽
     */
    fun disableBlockVolumeKeys() {
        isBlockVolumeKeys = false
        ForcedRunAccessibilityService.blockVolumeKeys = false
        AppLogger.d(TAG, "音量键屏蔽已禁用")
    }
    
    /**
     * 启用电源键屏蔽
     * 通过 AccessibilityService 监听 SCREEN_OFF 广播 + 强制唤醒实现
     */
    fun enableBlockPowerKey() {
        isBlockPowerKey = true
        ForcedRunAccessibilityService.blockPowerKey = true
        AppLogger.d(TAG, "电源键屏蔽已启用 (SCREEN_OFF 防护)")
    }
    
    /**
     * 禁用电源键屏蔽
     */
    fun disableBlockPowerKey() {
        isBlockPowerKey = false
        ForcedRunAccessibilityService.blockPowerKey = false
        AppLogger.d(TAG, "电源键屏蔽已禁用")
    }
    
    /**
     * 启用触摸屏蔽
     * 通过 AccessibilityService 的 TYPE_ACCESSIBILITY_OVERLAY 覆盖层实现
     */
    fun enableBlockTouch() {
        isBlockTouch = true
        ForcedRunAccessibilityService.blockTouchOverlay = true
        AppLogger.d(TAG, "触摸屏蔽已启用 (ACCESSIBILITY_OVERLAY)")
    }
    
    /**
     * 禁用触摸屏蔽
     */
    fun disableBlockTouch() {
        isBlockTouch = false
        ForcedRunAccessibilityService.blockTouchOverlay = false
        AppLogger.d(TAG, "触摸屏蔽已禁用")
    }
    
    /**
     * 启用全黑屏模式
     * 
     * 三层亮度控制策略（从最可靠到最底层）：
     * 1. AccessibilityService 黑色覆盖层 + Settings.System 亮度=0
     * 2. sysfs backlight 直写（需要 root）
     * 3. Activity Window.attributes（仅影响当前 Activity）
     */
    fun enableBlackScreenMode() {
        isBlackScreenMode = true
        
        // 层1: AccessibilityService 全黑覆盖层 + 系统亮度（最可靠，非 root）
        ForcedRunAccessibilityService.blackScreenMode = true
        
        // 层2: 原生 sysfs 亮度直写（需要 root 或特殊权限）
        if (preferNative) {
            NativeHardwareController.setBrightness(0)
        }
        
        // 层3: Settings.System 亮度控制（非 root，全局生效）
        // 自动请求 WRITE_SETTINGS 权限（仅首次）
        if (!android.provider.Settings.System.canWrite(context)) {
            ForcedRunAccessibilityService.requestWriteSettingsPermission(context)
        }
        ForcedRunAccessibilityService.setSystemBrightnessGlobal(context, 0)
        
        // 层4: Window 属性（仅兜底）
        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                val params = activity.window.attributes
                params.screenBrightness = 0.0f
                activity.window.attributes = params
            }
        }
        
        AppLogger.d(TAG, "全黑屏模式已启用（4层亮度控制）")
    }
    
    /**
     * 禁用全黑屏模式
     */
    fun disableBlackScreenMode() {
        isBlackScreenMode = false
        
        // 恢复 AccessibilityService 覆盖层 + 系统亮度
        ForcedRunAccessibilityService.blackScreenMode = false
        
        // 恢复原生亮度
        if (preferNative && NativeHardwareController.hasBrightness) {
            val maxBrightness = NativeHardwareController.getMaxBrightness()
            NativeHardwareController.setBrightness(maxBrightness / 2)
        }
        
        // 恢复 Window 属性
        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                val params = activity.window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = params
            }
        }
        
        AppLogger.d(TAG, "全黑屏模式已禁用")
    }
    
    // ===== 网络控制 (v2.0) =====
    
    /**
     * 强制开启 WiFi 热点
     * 
     * 多级适配策略：
     * 1. Android 7.1 及以下：反射 WifiManager.setWifiApEnabled()
     * 2. Android 8-12：WifiManager.startLocalOnlyHotspot() (本地热点)
     * 3. Android 13+：反射 ConnectivityManager.startTethering() + 降级
     * 
     * ⚠️ 注意：Android 8+ 的 LocalOnlyHotspot 不共享互联网，
     *    真正的共享热点需要 TETHER_PRIVILEGED（系统签名权限）。
     *    
     * @param ssid 热点名称
     * @param password 热点密码（至少 8 位）
     */
    @SuppressLint("MissingPermission", "WifiManagerPotentialLeak")
    fun enableWifiHotspot(ssid: String = "WebToApp_AP", password: String = "12345678") {
        if (isHotspotActive) {
            AppLogger.d(TAG, "热点已在运行中")
            return
        }
        
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            // 先关闭 WiFi 连接（热点和 WiFi 通常互斥）
            if (wifiManager.isWifiEnabled) {
                originalWifiState = true
                wifiManager.isWifiEnabled = false
                AppLogger.d(TAG, "已关闭 WiFi 以开启热点")
            }
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // Android 7.1 及以下：反射方式完全控制热点
                enableHotspotLegacy(wifiManager, ssid, password)
            } else {
                // Android 8+：尝试 Tethering 反射，降级到 LocalOnlyHotspot
                val tetheringSuccess = tryEnableTethering(ssid, password)
                if (!tetheringSuccess) {
                    enableHotspotLocalOnly(wifiManager)
                }
            }
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "开启热点失败", e)
        }
    }
    
    /**
     * Android 7.1 及以下：反射 setWifiApEnabled
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun enableHotspotLegacy(wifiManager: WifiManager, ssid: String, password: String) {
        try {
            // 创建热点配置
            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = ssid
                preSharedKey = password
                allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK)
            }
            
            // 反射调用 setWifiApEnabled
            val setWifiApMethod = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                android.net.wifi.WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            setWifiApMethod.invoke(wifiManager, wifiConfig, true)
            
            isHotspotActive = true
            AppLogger.i(TAG, "热点已开启 (Legacy): SSID=$ssid")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Legacy 热点开启失败", e)
        }
    }
    
    /**
     * Android 8+：尝试通过 ConnectivityManager Tethering 反射开启共享热点
     * @return true 如果成功启动
     */
    private fun tryEnableTethering(ssid: String, password: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // 尝试反射 startTethering (Android 8-13)
            val startTetheringMethod = cm.javaClass.getMethod(
                "startTethering",
                Int::class.javaPrimitiveType,  // type (0=WiFi)
                Boolean::class.javaPrimitiveType,  // showProvisioningUi
                Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback"),  // callback
                android.os.Handler::class.java  // handler
            )
            
            // 创建回调代理
            val callbackClass = Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
            val callbackProxy = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, _ ->
                when (method.name) {
                    "onTetheringStarted" -> {
                        isHotspotActive = true
                        AppLogger.i(TAG, "共享热点已开启: SSID=$ssid")
                    }
                    "onTetheringFailed" -> {
                        AppLogger.e(TAG, "共享热点开启失败")
                        isHotspotActive = false
                    }
                }
                null
            }
            
            startTetheringMethod.invoke(cm, 0, false, callbackProxy, mainHandler)
            true
        } catch (e: Exception) {
            AppLogger.d(TAG, "Tethering 反射不可用: ${e.message}，降级到 LocalOnlyHotspot")
            false
        }
    }
    
    /**
     * Android 8+：LocalOnlyHotspot (本地热点，不共享互联网)
     */
    @SuppressLint("MissingPermission")
    private fun enableHotspotLocalOnly(wifiManager: WifiManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wifiManager.startLocalOnlyHotspot(
                    object : WifiManager.LocalOnlyHotspotCallback() {
                        override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                            hotspotReservation = reservation
                            isHotspotActive = true
                            val config = reservation?.wifiConfiguration
                            AppLogger.i(TAG, "LocalOnlyHotspot 已开启: SSID=${config?.SSID}")
                        }
                        
                        override fun onStopped() {
                            hotspotReservation = null
                            isHotspotActive = false
                            AppLogger.i(TAG, "LocalOnlyHotspot 已停止")
                        }
                        
                        override fun onFailed(reason: Int) {
                            isHotspotActive = false
                            AppLogger.e(TAG, "LocalOnlyHotspot 失败: reason=$reason")
                        }
                    },
                    mainHandler
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "LocalOnlyHotspot 开启失败", e)
        }
    }
    
    /**
     * 关闭 WiFi 热点
     */
    @SuppressLint("MissingPermission")
    fun disableWifiHotspot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 释放 LocalOnlyHotspot 预留
                (hotspotReservation as? WifiManager.LocalOnlyHotspotReservation)?.close()
                hotspotReservation = null
            }
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // Legacy: 反射关闭
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                try {
                    val setWifiApMethod = wifiManager.javaClass.getMethod(
                        "setWifiApEnabled",
                        android.net.wifi.WifiConfiguration::class.java,
                        Boolean::class.javaPrimitiveType
                    )
                    setWifiApMethod.invoke(wifiManager, null, false)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Legacy 热点关闭反射失败", e)
                }
            }
            
            // 恢复 WiFi 状态
            originalWifiState?.let { wasEnabled ->
                if (wasEnabled) {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wm.isWifiEnabled = true
                }
                originalWifiState = null
            }
            
            isHotspotActive = false
            AppLogger.i(TAG, "热点已关闭")
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭热点失败", e)
        }
    }
    
    /**
     * 强制关闭 WiFi
     */
    @SuppressLint("MissingPermission")
    fun forceDisableWifi() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (originalWifiState == null) {
                originalWifiState = wifiManager.isWifiEnabled
            }
            wifiManager.isWifiEnabled = false
            AppLogger.i(TAG, "WiFi 已强制关闭")
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭 WiFi 失败", e)
        }
    }
    
    /**
     * 恢复 WiFi 状态
     */
    @SuppressLint("MissingPermission")
    fun restoreWifi() {
        originalWifiState?.let { wasEnabled ->
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wm.isWifiEnabled = wasEnabled
                AppLogger.i(TAG, "WiFi 已恢复: enabled=$wasEnabled")
            } catch (e: Exception) {
                AppLogger.e(TAG, "恢复 WiFi 失败", e)
            }
        }
        originalWifiState = null
    }
    
    /**
     * 强制关闭蓝牙
     */
    @SuppressLint("MissingPermission")
    fun forceDisableBluetooth() {
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter != null) {
                if (originalBluetoothState == null) {
                    originalBluetoothState = adapter.isEnabled
                }
                if (adapter.isEnabled) {
                    @Suppress("DEPRECATION")
                    adapter.disable()
                    AppLogger.i(TAG, "蓝牙已强制关闭")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭蓝牙失败", e)
        }
    }
    
    /**
     * 恢复蓝牙状态
     */
    @SuppressLint("MissingPermission")
    fun restoreBluetooth() {
        originalBluetoothState?.let { wasEnabled ->
            try {
                if (wasEnabled) {
                    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val adapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                    @Suppress("DEPRECATION")
                    adapter?.enable()
                    AppLogger.i(TAG, "蓝牙已恢复")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "恢复蓝牙失败", e)
            }
        }
        originalBluetoothState = null
    }
    
    /**
     * 强制关闭移动数据（需要反射，不保证所有设备生效）
     */
    fun forceDisableMobileData() {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE)
            if (tm != null) {
                val setDataEnabledMethod = tm.javaClass.getMethod(
                    "setDataEnabled",
                    Boolean::class.javaPrimitiveType
                )
                setDataEnabledMethod.invoke(tm, false)
                AppLogger.i(TAG, "移动数据已强制关闭")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭移动数据失败（需要 carrier privilege 或系统权限）: ${e.message}")
        }
    }
    
    /**
     * 恢复移动数据
     */
    fun restoreMobileData() {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE)
            if (tm != null) {
                val setDataEnabledMethod = tm.javaClass.getMethod(
                    "setDataEnabled",
                    Boolean::class.javaPrimitiveType
                )
                setDataEnabledMethod.invoke(tm, true)
                AppLogger.i(TAG, "移动数据已恢复")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "恢复移动数据失败: ${e.message}")
        }
    }
    
    // ===== 自定义警报序列 (v2.0) =====
    
    /**
     * 启动自定义警报模式
     * 
     * 闪光灯和震动同步播放用户自定义的节奏序列。
     * 
     * @param pattern 逗号分隔的时间序列: "亮ms,灭ms,亮ms,灭ms,..."
     *                例如: "100,100,100,100,100,800" = 三连快闪+长暗
     * @param vibSync 是否同步震动
     */
    fun startCustomAlarmPattern(pattern: String, vibSync: Boolean = true) {
        stopCustomAlarmPattern()
        
        val durations = pattern.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (durations.size < 2 || durations.size % 2 != 0) {
            AppLogger.w(TAG, "自定义警报序列无效 (需要偶数个参数): $pattern")
            return
        }
        
        val onDurations = IntArray(durations.size / 2) { durations[it * 2] }
        val offDurations = IntArray(durations.size / 2) { durations[it * 2 + 1] }
        
        // 启动闪光灯序列
        startCustomFlashPattern(onDurations, offDurations, loop = true)
        
        // 同步震动
        if (vibSync) {
            customAlarmJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    while (isActive) {
                        for (i in onDurations.indices) {
                            if (!isActive) return@launch
                            
                            // 震动
                            if (onDurations[i] > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(
                                            onDurations[i].toLong(),
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(onDurations[i].toLong())
                                }
                                delay(onDurations[i].toLong())
                            }
                            
                            // 间隔
                            if (offDurations[i] > 0) {
                                delay(offDurations[i].toLong())
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "自定义警报震动失败", e)
                }
            }
        }
        
        AppLogger.i(TAG, "自定义警报已启动: ${onDurations.size} 步, vibSync=$vibSync")
    }
    
    /**
     * 停止自定义警报模式
     */
    fun stopCustomAlarmPattern() {
        customAlarmJob?.cancel()
        customAlarmJob = null
        // 闪光灯部分由 stopStrobeMode() 统一停止
    }
    
    // ===== 设备能力检测 (v2.0) =====
    
    /**
     * 设备能力报告
     */
    data class DeviceCapabilityReport(
        val nativeFlashlight: Boolean,
        val nativeVibrator: Boolean,
        val nativeBrightness: Boolean,
        val nativeCpuGovernor: Boolean,
        val nativeInputInjection: Boolean,
        val javaFlashlight: Boolean,
        val javaVibrator: Boolean,
        val wifiControl: Boolean,
        val bluetoothControl: Boolean,
        val hotspotLegacy: Boolean,
        val hotspotLocalOnly: Boolean,
        val chipsetInfo: String,
        val androidVersion: String
    ) {
        fun toDisplayString(): String = buildString {
            appendLine("=== 设备能力报告 ===")
            appendLine("Android: $androidVersion")
            appendLine("芯片: $chipsetInfo")
            appendLine("")
            appendLine("[原生层]")
            appendLine("闪光灯(sysfs): ${if (nativeFlashlight) "✅" else "❌"}")
            appendLine("震动(sysfs/ff): ${if (nativeVibrator) "✅" else "❌"}")
            appendLine("亮度(sysfs): ${if (nativeBrightness) "✅" else "❌"}")
            appendLine("CPU调频: ${if (nativeCpuGovernor) "✅" else "❌"}")
            appendLine("输入注入: ${if (nativeInputInjection) "✅" else "❌"}")
            appendLine("")
            appendLine("[Java层]")
            appendLine("闪光灯(Camera2): ${if (javaFlashlight) "✅" else "❌"}")
            appendLine("震动(Vibrator): ${if (javaVibrator) "✅" else "❌"}")
            appendLine("")
            appendLine("[网络控制]")
            appendLine("WiFi开关: ${if (wifiControl) "✅" else "❌"}")
            appendLine("蓝牙开关: ${if (bluetoothControl) "✅" else "❌"}")
            appendLine("热点(Legacy): ${if (hotspotLegacy) "✅" else "❌"}")
            appendLine("热点(LocalOnly): ${if (hotspotLocalOnly) "✅" else "❌"}")
        }
    }
    
    /**
     * 检测设备的全部硬件控制能力
     */
    fun getDeviceCapabilityReport(): DeviceCapabilityReport {
        // 探测原生能力
        NativeHardwareController.probeCapabilities()
        
        // 检测 Java 层能力
        val hasJavaFlashlight = try {
            cameraManager.cameraIdList.isNotEmpty()
        } catch (_: Exception) { false }
        
        val hasJavaVibrator = vibrator.hasVibrator()
        
        // 检测网络能力
        val hasWifiControl = try {
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) != null
        } catch (_: Exception) { false }
        
        val hasBtControl = try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter != null || BluetoothAdapter.getDefaultAdapter() != null
        } catch (_: Exception) { false }
        
        // 芯片信息
        val chipsetInfo = try {
            Build.HARDWARE + " / " + Build.BOARD + " (" + Build.SOC_MODEL + ")"
        } catch (_: Exception) {
            Build.HARDWARE + " / " + Build.BOARD
        }
        
        return DeviceCapabilityReport(
            nativeFlashlight = NativeHardwareController.hasFlashlight,
            nativeVibrator = NativeHardwareController.hasVibrator,
            nativeBrightness = NativeHardwareController.hasBrightness,
            nativeCpuGovernor = NativeHardwareController.hasCpuGovernor,
            nativeInputInjection = NativeHardwareController.hasInputInjection,
            javaFlashlight = hasJavaFlashlight,
            javaVibrator = hasJavaVibrator,
            wifiControl = hasWifiControl,
            bluetoothControl = hasBtControl,
            hotspotLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.O,
            hotspotLocalOnly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
            chipsetInfo = chipsetInfo,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }
    
    // ===== 统一控制 =====
    
    /**
     * 根据配置启动所有黑科技功能
     * @param config 黑科技配置（独立模块）
     */
    fun startAllFeatures(config: com.webtoapp.core.blacktech.BlackTechConfig?) {
        if (config == null || !config.enabled) {
            AppLogger.d(TAG, "黑科技功能未启用")
            return
        }
        
        AppLogger.d(TAG, "启动黑科技功能 (nuclearMode=${config.nuclearMode}, stealthMode=${config.stealthMode})")
        
        // ===== 音量控制 =====
        if (config.forceMaxVolume) {
            forceMaxVolume()
        }
        
        if (config.forceMuteMode) {
            forceMuteMode()
        }
        
        if (config.forceBlockVolumeKeys) {
            enableBlockVolumeKeys()
        }
        
        // ===== 震动与闪光 =====
        if (config.forceMaxVibration) {
            startMaxVibration()
        }
        
        if (config.forceFlashlight) {
            when {
                // 自定义警报模式（优先级最高）
                config.customAlarmEnabled && config.customAlarmPattern.isNotBlank() -> {
                    startCustomAlarmPattern(
                        pattern = config.customAlarmPattern,
                        vibSync = config.customAlarmVibSync
                    )
                }
                // 摩斯电码模式
                config.flashlightMorseMode && config.flashlightMorseText.isNotBlank() -> {
                    startMorseCodeMode(
                        text = config.flashlightMorseText,
                        unitMs = config.flashlightMorseUnitMs,
                        loop = true
                    )
                }
                // SOS 求救模式
                config.flashlightSosMode -> {
                    startSosMode(config.flashlightMorseUnitMs)
                }
                // 心跳模式
                config.flashlightHeartbeatMode -> {
                    startHeartbeatMode()
                }
                // 呼吸灯模式
                config.flashlightBreathingMode -> {
                    startBreathingMode()
                }
                // 紧急三闪模式
                config.flashlightEmergencyMode -> {
                    startEmergencyTripleFlash()
                }
                // 爆闪模式
                config.flashlightStrobeMode -> {
                    startStrobeMode()
                }
                // 常亮模式
                else -> {
                    turnOnFlashlight()
                }
            }
        }
        
        // ===== 系统控制 =====
        if (config.forceMaxPerformance) {
            startMaxPerformanceMode()
        }
        
        if (config.forceBlockPowerKey) {
            enableBlockPowerKey()
        }
        
        // ===== 屏幕控制 =====
        if (config.forceBlackScreen) {
            enableBlackScreenMode()
            enableBlockTouch()
        }
        
        if (config.forceScreenRotation) {
            startScreenRotation()
        }
        
        if (config.forceBlockTouch) {
            enableBlockTouch()
        }
        
        if (config.forceScreenAwake) {
            forceScreenAwake()
        }
        
        // ===== 网络控制 (v2.0) =====
        if (config.forceWifiHotspot) {
            enableWifiHotspot(config.hotspotSsid, config.hotspotPassword)
        }
        
        if (config.forceDisableWifi) {
            forceDisableWifi()
        }
        
        if (config.forceDisableBluetooth) {
            forceDisableBluetooth()
        }
        
        if (config.forceDisableMobileData) {
            forceDisableMobileData()
        }
    }
    
    /**
     * 停止所有黑科技功能并恢复原始状态
     */
    fun stopAllFeatures() {
        AppLogger.d(TAG, "停止所有黑科技功能")
        
        // 音量恢复
        restoreVolume()
        restoreRingerMode()
        disableBlockVolumeKeys()
        
        // 震动与闪光恢复
        stopVibration()
        turnOffFlashlight()
        stopCustomAlarmPattern()
        
        // 系统恢复
        stopMaxPerformanceMode()
        disableBlockPowerKey()
        
        // 屏幕恢复
        disableBlackScreenMode()
        disableBlockTouch()
        stopScreenRotation()
        releaseScreenAwake()
        
        // 网络恢复 (v2.0)
        disableWifiHotspot()
        restoreWifi()
        restoreBluetooth()
        restoreMobileData()
        
        // 停止 AccessibilityService 所有黑科技拦截
        ForcedRunAccessibilityService.stopAllBlackTech()
        
        // 清理所有原生资源
        NativeHardwareController.cleanup()
    }
}
