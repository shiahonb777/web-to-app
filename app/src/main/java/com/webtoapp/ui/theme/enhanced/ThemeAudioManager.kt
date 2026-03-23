package com.webtoapp.ui.theme.enhanced

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ============================================================
 * 高端主题音效与触觉反馈管理器
 * 提供沉浸式的多感官体验
 * ============================================================
 */

/**
 * 触觉反馈类型 - 不同场景使用不同的振动模式
 */
enum class HapticType {
    LIGHT_TAP,          // 轻触 - 10ms
    MEDIUM_TAP,         // 中等触感 - 20ms
    HEAVY_TAP,          // 重触 - 40ms
    DOUBLE_TAP,         // 双击 - 两次短振动
    LONG_PRESS,         // 长按 - 50ms
    SUCCESS,            // Success反馈 - 渐强
    ERROR,              // Error反馈 - 急促
    WARNING,            // Warning - 中等
    SELECTION,          // Select - 极轻
    IMPACT_LIGHT,       // 轻碰撞
    IMPACT_MEDIUM,      // 中碰撞
    IMPACT_HEAVY,       // 重碰撞
    TEXTURE_TICK,       // 纹理滴答
    SOFT_LANDING,       // 软着陆
    RIGID_LANDING,      // 硬着陆
    EXPLOSION,          // 爆炸效果
    WATER_DROP,         // 水滴效果
    HEARTBEAT,          // 心跳效果
    WAVE,               // 波浪效果
    SPARKLE,            // 闪烁效果
}

/**
 * 主题音效类型
 */
enum class ThemeSoundType {
    // 通用
    TAP,
    SWIPE,
    TRANSITION,
    
    // 樱花主题
    SAKURA_PETAL_FALL,
    SAKURA_WIND,
    SAKURA_CHIME,
    
    // 海洋主题
    OCEAN_WAVE,
    OCEAN_BUBBLE,
    OCEAN_SPLASH,
    
    // 极光主题
    AURORA_SHIMMER,
    AURORA_WHOOSH,
    
    // 银河主题
    GALAXY_TWINKLE,
    GALAXY_WARP,
    
    // 赛博朋克主题
    CYBER_GLITCH,
    CYBER_BEEP,
    CYBER_SCAN,
    
    // 火山主题
    VOLCANO_RUMBLE,
    VOLCANO_LAVA,
    VOLCANO_ERUPTION,
    
    // 冰霜主题
    FROST_CRACK,
    FROST_WIND,
    FROST_CRYSTAL,
    
    // 森林主题
    FOREST_BIRDS,
    FOREST_LEAVES,
    FOREST_STREAM,
    
    // 日落主题
    SUNSET_WIND,
    SUNSET_BIRDS,
    
    // 霓虹东京主题
    NEON_RAIN,
    NEON_BUZZ,
    NEON_FLICKER,
    
    // 薰衣草主题
    LAVENDER_BREEZE,
    LAVENDER_BUTTERFLY,
    LAVENDER_CHIME,
    
    // 极简主题
    MINIMAL_CLICK,
    MINIMAL_TONE,
}

/**
 * 高级触觉反馈管理器
 */
class HapticFeedbackManager private constructor(private val context: Context) {
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled
    
    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }
    
    /**
     * 执行触觉反馈
     */
    fun performHaptic(type: HapticType, intensity: Float = 1f) {
        if (!_enabled.value) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.LIGHT_TAP -> VibrationEffect.createOneShot(
                        10, (50 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.MEDIUM_TAP -> VibrationEffect.createOneShot(
                        20, (100 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.HEAVY_TAP -> VibrationEffect.createOneShot(
                        40, (180 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.DOUBLE_TAP -> VibrationEffect.createWaveform(
                        longArrayOf(0, 15, 50, 15), intArrayOf(0, (120 * intensity).toInt(), 0, (120 * intensity).toInt()), -1
                    )
                    HapticType.LONG_PRESS -> VibrationEffect.createOneShot(
                        50, (150 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.SUCCESS -> VibrationEffect.createWaveform(
                        longArrayOf(0, 20, 30, 30), intArrayOf(0, (80 * intensity).toInt(), 0, (160 * intensity).toInt()), -1
                    )
                    HapticType.ERROR -> VibrationEffect.createWaveform(
                        longArrayOf(0, 30, 20, 30, 20, 30), intArrayOf(0, (200 * intensity).toInt(), 0, (200 * intensity).toInt(), 0, (200 * intensity).toInt()), -1
                    )
                    HapticType.WARNING -> VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 30, 40), intArrayOf(0, (150 * intensity).toInt(), 0, (150 * intensity).toInt()), -1
                    )
                    HapticType.SELECTION -> VibrationEffect.createOneShot(
                        5, (30 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.IMPACT_LIGHT -> VibrationEffect.createOneShot(
                        8, (60 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.IMPACT_MEDIUM -> VibrationEffect.createOneShot(
                        15, (120 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.IMPACT_HEAVY -> VibrationEffect.createOneShot(
                        25, (200 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.TEXTURE_TICK -> VibrationEffect.createOneShot(
                        3, (20 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.SOFT_LANDING -> VibrationEffect.createWaveform(
                        longArrayOf(0, 30), intArrayOf(0, (80 * intensity).toInt()), -1
                    )
                    HapticType.RIGID_LANDING -> VibrationEffect.createOneShot(
                        20, (220 * intensity).toInt().coerceIn(1, 255)
                    )
                    HapticType.EXPLOSION -> VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 20, 30, 20, 20), 
                        intArrayOf(0, (255 * intensity).toInt(), (100 * intensity).toInt(), (180 * intensity).toInt(), (50 * intensity).toInt(), (100 * intensity).toInt()), 
                        -1
                    )
                    HapticType.WATER_DROP -> VibrationEffect.createWaveform(
                        longArrayOf(0, 10, 20, 15), intArrayOf(0, (100 * intensity).toInt(), 0, (60 * intensity).toInt()), -1
                    )
                    HapticType.HEARTBEAT -> VibrationEffect.createWaveform(
                        longArrayOf(0, 60, 100, 80, 400), 
                        intArrayOf(0, (180 * intensity).toInt(), 0, (120 * intensity).toInt(), 0), 
                        -1
                    )
                    HapticType.WAVE -> VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 50, 100, 50, 100), 
                        intArrayOf(0, (60 * intensity).toInt(), (100 * intensity).toInt(), (140 * intensity).toInt(), (100 * intensity).toInt(), (60 * intensity).toInt()), 
                        -1
                    )
                    HapticType.SPARKLE -> VibrationEffect.createWaveform(
                        longArrayOf(0, 5, 10, 5, 10, 5, 10, 5), 
                        intArrayOf(0, (80 * intensity).toInt(), 0, (120 * intensity).toInt(), 0, (80 * intensity).toInt(), 0, (40 * intensity).toInt()), 
                        -1
                    )
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate((20 * intensity).toLong())
            }
        } catch (e: Exception) {
            // 忽略振动错误
        }
    }
    
    /**
     * 自定义振动模式
     */
    fun performCustomPattern(pattern: LongArray, amplitudes: IntArray, repeat: Int = -1) {
        if (!_enabled.value) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, amplitudes, repeat)
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            // 忽略
        }
    }
    
    /**
     * 停止振动
     */
    fun cancel() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            // 忽略
        }
    }
    
    companion object {
        @Volatile
        private var instance: HapticFeedbackManager? = null
        
        fun getInstance(context: Context): HapticFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: HapticFeedbackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 主题音效管理器
 * 注意：实际音效文件需要添加到 res/raw 目录
 * 这里提供完整的架构，音效文件可以后续添加
 */
class ThemeSoundManager private constructor(private val context: Context) {
    
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<ThemeSoundType, Int>()
    private val loadedSounds = mutableSetOf<Int>()
    
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled
    
    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume
    
    init {
        initSoundPool()
    }
    
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()
            .apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSounds.add(sampleId)
                    }
                }
            }
        
        // 音效文件加载 - 当音效文件存在时取消注释
        // loadSound(ThemeSoundType.TAP, R.raw.tap)
        // loadSound(ThemeSoundType.SAKURA_PETAL_FALL, R.raw.sakura_petal)
        // ... 其他音效
    }
    
    private fun loadSound(type: ThemeSoundType, resourceId: Int) {
        soundPool?.let { pool ->
            val soundId = pool.load(context, resourceId, 1)
            soundMap[type] = soundId
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }
    
    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
    }
    
    /**
     * 播放音效
     */
    fun playSound(
        type: ThemeSoundType, 
        volumeMultiplier: Float = 1f,
        rate: Float = 1f,
        loop: Int = 0
    ): Int {
        if (!_enabled.value) return -1
        
        val soundId = soundMap[type] ?: return -1
        if (soundId !in loadedSounds) return -1
        
        val actualVolume = _volume.value * volumeMultiplier
        return soundPool?.play(soundId, actualVolume, actualVolume, 1, loop, rate) ?: -1
    }
    
    /**
     * 停止音效
     */
    fun stopSound(streamId: Int) {
        soundPool?.stop(streamId)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        loadedSounds.clear()
    }
    
    companion object {
        @Volatile
        private var instance: ThemeSoundManager? = null
        
        fun getInstance(context: Context): ThemeSoundManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeSoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * CompositionLocal 提供触觉和音效管理器
 */
val LocalHapticManager = staticCompositionLocalOf<HapticFeedbackManager?> { null }
val LocalSoundManager = staticCompositionLocalOf<ThemeSoundManager?> { null }

/**
 * 提供触觉和音效管理器的 Composable
 */
@Composable
fun ProvideThemeAudioHaptic(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticFeedbackManager.getInstance(context) }
    val soundManager = remember { ThemeSoundManager.getInstance(context) }
    
    CompositionLocalProvider(
        LocalHapticManager provides hapticManager,
        LocalSoundManager provides soundManager
    ) {
        content()
    }
}

/**
 * 便捷的触觉反馈 Hook
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    return remember { HapticFeedbackManager.getInstance(context) }
}

/**
 * 便捷的音效 Hook
 */
@Composable
fun rememberThemeSound(): ThemeSoundManager {
    val context = LocalContext.current
    return remember { ThemeSoundManager.getInstance(context) }
}
