package com.webtoapp.core.forcedrun

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
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
    private var performanceJob: Job? = null
    private var screenRotationJob: Job? = null
    private var maxVolumeJob: Job? = null
    private var muteJob: Job? = null
    
    private var originalVolume: Int = -1
    private var originalRingerMode: Int = -1
    private var isFlashlightOn = false
    private var activityRef: WeakReference<Activity>? = null
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var screenWakeLock: PowerManager.WakeLock? = null
    
    // ===== 音量控制 =====
    
    /**
     * 强制持续最大音量
     * 持续监控并保持音量最大，防止用户调节
     */
    fun forceMaxVolume() {
        stopMaxVolume()
        
        try {
            // 保存原始音量
            if (originalVolume == -1) {
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
            
            // 立即设置最大音量
            setAllVolumesToMax()
            
            // 启动持续监控
            maxVolumeJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    try {
                        setAllVolumesToMax()
                    } catch (e: Exception) {
                        Log.e(TAG, "持续设置音量失败", e)
                    }
                    delay(500) // 每500ms检查一次
                }
            }
            
            Log.d(TAG, "持续最大音量已启动")
        } catch (e: Exception) {
            Log.e(TAG, "设置音量失败", e)
        }
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
        maxVolumeJob?.cancel()
        maxVolumeJob = null
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
    
    /**
     * 启动最大性能模式（高CPU占用）
     * ⚠️ 警告：这会消耗大量电池和产生热量
     */
    fun startMaxPerformanceMode() {
        stopMaxPerformanceMode()
        
        val cpuCount = Runtime.getRuntime().availableProcessors()
        
        performanceJob = CoroutineScope(Dispatchers.Default).launch {
            val jobs = (1..cpuCount).map {
                launch {
                    while (isActive) {
                        // 执行一些计算密集型操作
                        var x = 0.0
                        for (i in 0 until 100000) {
                            x += Math.sin(i.toDouble()) * Math.cos(i.toDouble())
                        }
                        // 稍微让出一点CPU，避免完全卡死
                        yield()
                    }
                }
            }
            jobs.forEach { it.join() }
        }
        
        Log.d(TAG, "最大性能模式已启动 (${cpuCount} 核心)")
    }
    
    /**
     * 停止最大性能模式
     */
    fun stopMaxPerformanceMode() {
        performanceJob?.cancel()
        performanceJob = null
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
    
    /**
     * 强制持续静音模式
     * 持续监控并保持静音，防止用户调节
     */
    fun forceMuteMode() {
        stopMuteMode()
        
        try {
            if (originalRingerMode == -1) {
                originalRingerMode = audioManager.ringerMode
            }
            
            // 立即设置静音
            setAllVolumesToMute()
            
            // 启动持续监控
            muteJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    try {
                        setAllVolumesToMute()
                    } catch (e: Exception) {
                        Log.e(TAG, "持续设置静音失败", e)
                    }
                    delay(500) // 每500ms检查一次
                }
            }
            
            Log.d(TAG, "持续静音模式已启动")
        } catch (e: Exception) {
            Log.e(TAG, "设置静音模式失败", e)
        }
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
        muteJob?.cancel()
        muteJob = null
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
     */
    fun startScreenRotation() {
        stopScreenRotation()
        
        screenRotationJob = CoroutineScope(Dispatchers.Main).launch {
            var isLandscape = false
            while (isActive) {
                try {
                    activityRef?.get()?.let { activity ->
                        activity.requestedOrientation = if (isLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                        isLandscape = !isLandscape
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "屏幕翻转失败", e)
                }
                delay(2000) // 每2秒翻转一次
            }
        }
        
        Log.d(TAG, "屏幕持续翻转已启动")
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
     */
    fun startAllFeatures(config: ForcedRunConfig) {
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
