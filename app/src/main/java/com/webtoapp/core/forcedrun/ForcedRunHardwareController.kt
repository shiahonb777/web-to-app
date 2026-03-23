package com.webtoapp.core.forcedrun

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * 强制运行硬件控制器
 * 
 * 实现各种"黑科技"硬件控制功能
 * ⚠️ 警告：这些功能可能会对设备造成影响，请谨慎使用
 */
class ForcedRunHardwareController(private val context: Context) {
    
    companion object {
        private const val TAG = "ForcedRunHardware"
        
        @Volatile
        private var instance: ForcedRunHardwareController? = null
        
        fun getInstance(context: Context): ForcedRunHardwareController {
            return instance ?: synchronized(this) {
                instance ?: ForcedRunHardwareController(context.applicationContext).also { instance = it }
            }
        }
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
                        Log.e(TAG, "持续设置音量失败", e)
                    }
                    delay(50) // 每50ms检查一次，用户几乎无法感知音量变化
                }
            }
            
            Log.d(TAG, "持续最大音量已启动（广播监听 + 50ms轮询）")
        } catch (e: Exception) {
            Log.e(TAG, "设置音量失败", e)
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
        
        Log.d(TAG, "音量变化广播接收器已注册")
    }
    
    /**
     * 注销音量变化广播接收器
     */
    private fun unregisterVolumeChangeReceiver() {
        volumeChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
                Log.d(TAG, "音量变化广播接收器已注销")
            } catch (e: Exception) {
                Log.e(TAG, "注销音量广播接收器失败", e)
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
                Log.d(TAG, "音量已恢复")
            }
        } catch (e: Exception) {
            Log.e(TAG, "恢复音量失败", e)
        }
    }
    
    // ===== 震动控制 =====
    
    /**
     * 开始持续最大震动
     */
    fun startMaxVibration() {
        stopVibration()
        
        vibrationJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                while (isActive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // 使用最大振幅持续震动
                        val effect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(1000)
                    }
                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "震动失败", e)
            }
        }
        
        Log.d(TAG, "持续震动已启动")
    }
    
    /**
     * 停止震动
     */
    fun stopVibration() {
        vibrationJob?.cancel()
        vibrationJob = null
        vibrator.cancel()
        Log.d(TAG, "震动已停止")
    }
    
    // ===== 闪光灯控制 =====
    
    /**
     * 打开闪光灯
     */
    fun turnOnFlashlight() {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, true)
            isFlashlightOn = true
            Log.d(TAG, "闪光灯已打开")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "打开闪光灯失败", e)
        }
    }
    
    /**
     * 关闭闪光灯
     */
    fun turnOffFlashlight() {
        try {
            stopStrobeMode()
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, false)
            isFlashlightOn = false
            Log.d(TAG, "闪光灯已关闭")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "关闭闪光灯失败", e)
        }
    }
    
    /**
     * 启动爆闪模式
     */
    fun startStrobeMode() {
        stopStrobeMode()
        
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
                Log.e(TAG, "爆闪模式失败", e)
            }
        }
        
        Log.d(TAG, "爆闪模式已启动")
    }
    
    /**
     * 停止爆闪模式
     */
    fun stopStrobeMode() {
        strobeJob?.cancel()
        strobeJob = null
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
        
        val cpuCount = Runtime.getRuntime().availableProcessors()
        
        // 为每个CPU核心创建一个高优先级线程
        for (i in 0 until cpuCount) {
            val thread = Thread {
                Thread.currentThread().priority = Thread.MAX_PRIORITY
                
                // 分配内存块用于内存压力测试
                val memoryBlock = ByteArray(1024 * 1024) // 1MB per thread
                val intArray = IntArray(10000)
                val doubleArray = DoubleArray(10000)
                
                var counter = 0L
                var floatResult = 0.0
                var intResult = 0
                
                while (isPerformanceModeRunning && !Thread.currentThread().isInterrupted) {
                    try {
                        // 1. 浮点密集计算（触发FPU）
                        for (j in 0 until 50000) {
                            floatResult += Math.sin(j.toDouble()) * Math.cos(j.toDouble())
                            floatResult += Math.sqrt(Math.abs(floatResult))
                            floatResult += Math.pow(1.0001, j.toDouble() % 100)
                        }
                        
                        // 2. 整数密集计算（触发ALU）
                        for (j in 0 until 100000) {
                            intResult = intResult xor (j * 31)
                            intResult = intResult.rotateLeft(j % 32)
                            counter++
                        }
                        
                        // 3. 内存读写操作（触发内存带宽）
                        for (j in memoryBlock.indices step 64) {
                            memoryBlock[j] = (counter and 0xFF).toByte()
                            intResult += memoryBlock[j].toInt()
                        }
                        
                        // 4. 数组操作（缓存压力）
                        for (j in intArray.indices) {
                            intArray[j] = intResult + j
                            doubleArray[j] = floatResult + j
                        }
                        
                        // 5. 排序操作（复杂计算）
                        if (counter % 100 == 0L) {
                            intArray.shuffle()
                            intArray.sort()
                        }
                        
                        // 防止编译器优化掉计算结果
                        if (floatResult == Double.MAX_VALUE && intResult == Int.MAX_VALUE) {
                            Log.v(TAG, "Performance: $floatResult, $intResult")
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
                    // 持续分配和释放内存，保持内存压力
                    if (memoryBlocks.size < 50) {
                        memoryBlocks.add(ByteArray(1024 * 512)) // 512KB blocks
                    } else {
                        // Shuffle释放一些块
                        if (memoryBlocks.isNotEmpty()) {
                            memoryBlocks.removeAt((Math.random() * memoryBlocks.size).toInt())
                        }
                    }
                    
                    // 访问内存块以确保不被优化
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
        
        Log.d(TAG, "最大性能模式已启动 (${cpuCount} CPU核心 + 内存压力线程)")
    }
    
    /**
     * 停止最大性能模式
     */
    fun stopMaxPerformanceMode() {
        isPerformanceModeRunning = false
        
        performanceThreads.forEach { thread ->
            try {
                thread.interrupt()
            } catch (e: Exception) {
                // 忽略
            }
        }
        performanceThreads.clear()
        
        // 建议GC回收内存
        System.gc()
        
        Log.d(TAG, "最大性能模式已停止")
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
            
            Log.d(TAG, "应用缓存已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理缓存失败", e)
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
                        Log.e(TAG, "持续设置静音失败", e)
                    }
                    delay(50) // 每50ms检查一次，用户几乎无法感知音量变化
                }
            }
            
            Log.d(TAG, "持续静音模式已启动（广播监听 + 50ms轮询）")
        } catch (e: Exception) {
            Log.e(TAG, "设置静音模式失败", e)
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
        
        Log.d(TAG, "静音模式音量广播接收器已注册")
    }
    
    /**
     * 注销静音模式音量变化广播接收器
     */
    private fun unregisterMuteVolumeChangeReceiver() {
        muteVolumeChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
                Log.d(TAG, "静音模式音量广播接收器已注销")
            } catch (e: Exception) {
                Log.e(TAG, "注销静音模式音量广播接收器失败", e)
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
                Log.d(TAG, "铃声模式已恢复")
            }
        } catch (e: Exception) {
            Log.e(TAG, "恢复铃声模式失败", e)
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
                screenWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "WebToApp:ScreenAwake"
                )
            }
            screenWakeLock?.acquire(Long.MAX_VALUE)
            
            Log.d(TAG, "屏幕常亮已启用")
        } catch (e: Exception) {
            Log.e(TAG, "设置屏幕常亮失败", e)
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
            
            Log.d(TAG, "屏幕常亮已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放屏幕常亮失败", e)
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
                    Log.e(TAG, "屏幕翻转失败", e)
                }
                delay(2000) // 每2秒翻转一次
            }
        }
        
        Log.d(TAG, "屏幕四向循环翻转已启动")
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
     */
    fun enableBlockVolumeKeys() {
        isBlockVolumeKeys = true
        Log.d(TAG, "音量键屏蔽已启用")
    }
    
    /**
     * 禁用音量键屏蔽
     */
    fun disableBlockVolumeKeys() {
        isBlockVolumeKeys = false
        Log.d(TAG, "音量键屏蔽已禁用")
    }
    
    /**
     * 启用电源键屏蔽（通过辅助功能服务实现）
     */
    fun enableBlockPowerKey() {
        isBlockPowerKey = true
        Log.d(TAG, "电源键屏蔽已启用（需要辅助功能服务）")
    }
    
    /**
     * 禁用电源键屏蔽
     */
    fun disableBlockPowerKey() {
        isBlockPowerKey = false
        Log.d(TAG, "电源键屏蔽已禁用")
    }
    
    /**
     * 启用触摸屏蔽
     */
    fun enableBlockTouch() {
        isBlockTouch = true
        Log.d(TAG, "触摸屏蔽已启用")
    }
    
    /**
     * 禁用触摸屏蔽
     */
    fun disableBlockTouch() {
        isBlockTouch = false
        Log.d(TAG, "触摸屏蔽已禁用")
    }
    
    /**
     * 启用全黑屏模式
     */
    fun enableBlackScreenMode() {
        isBlackScreenMode = true
        
        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                val params = activity.window.attributes
                params.screenBrightness = 0.0f
                activity.window.attributes = params
            }
        }
        
        Log.d(TAG, "全黑屏模式已启用")
    }
    
    /**
     * 禁用全黑屏模式
     */
    fun disableBlackScreenMode() {
        isBlackScreenMode = false
        
        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                val params = activity.window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = params
            }
        }
        
        Log.d(TAG, "全黑屏模式已禁用")
    }
    
    // ===== 统一控制 =====
    
    /**
     * 根据配置启动所有黑科技功能
     * @param config 黑科技配置（独立模块）
     */
    fun startAllFeatures(config: com.webtoapp.core.blacktech.BlackTechConfig?) {
        if (config == null || !config.enabled) {
            Log.d(TAG, "黑科技功能未启用")
            return
        }
        
        Log.d(TAG, "启动黑科技功能")
        
        if (config.forceMaxVolume) {
            forceMaxVolume()
        }
        
        if (config.forceMaxVibration) {
            startMaxVibration()
        }
        
        if (config.forceFlashlight) {
            if (config.flashlightStrobeMode) {
                startStrobeMode()
            } else {
                turnOnFlashlight()
            }
        }
        
        if (config.forceMaxPerformance) {
            startMaxPerformanceMode()
        }
        
        // 新增黑科技功能
        if (config.forceMuteMode) {
            forceMuteMode()
        }
        
        if (config.forceBlockVolumeKeys) {
            enableBlockVolumeKeys()
        }
        
        if (config.forceBlockPowerKey) {
            enableBlockPowerKey()
        }
        
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
    }
    
    /**
     * 停止所有黑科技功能并恢复原始状态
     */
    fun stopAllFeatures() {
        Log.d(TAG, "停止所有黑科技功能")
        
        restoreVolume()
        stopVibration()
        turnOffFlashlight()
        stopMaxPerformanceMode()
        
        // 新增功能恢复
        restoreRingerMode()
        stopScreenRotation()
        disableBlockVolumeKeys()
        disableBlockPowerKey()
        disableBlockTouch()
        disableBlackScreenMode()
    }
}
