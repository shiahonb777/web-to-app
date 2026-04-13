package com.webtoapp.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

/**
 * 主题管理器
 * 管理主题切换和持久化
 * 使用 StateFlow 确保状态在重组时保持一致
 */
@SuppressLint("StaticFieldLeak")
class ThemeManager(private val context: Context) {
    
    // 使用独立的 CoroutineScope 管理 StateFlow
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    companion object {
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")
        private val KEY_ENABLE_PARTICLES = booleanPreferencesKey("enable_particles")
        private val KEY_ENABLE_HAPTICS = booleanPreferencesKey("enable_haptics")
        private val KEY_ENABLE_SOUND = booleanPreferencesKey("enable_sound")
        private val KEY_ANIMATION_SPEED = stringPreferencesKey("animation_speed")
        
        @Volatile
        private var instance: ThemeManager? = null
        
        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * 暗色模式设置
     */
    enum class DarkModeSettings {
        SYSTEM,
        LIGHT,
        DARK;
        
        fun getDisplayName(): String = when (this) {
            SYSTEM -> Strings.followSystem
            LIGHT -> Strings.alwaysLight
            DARK -> Strings.alwaysDark
        }
    }
    
    /**
     * 动画速度
     */
    enum class AnimationSpeed(val multiplier: Float) {
        SLOW(1.5f),
        NORMAL(1.0f),
        FAST(0.7f),
        INSTANT(0.3f);
        
        fun getDisplayName(): String = when (this) {
            SLOW -> Strings.speedSlow
            NORMAL -> Strings.speedNormal
            FAST -> Strings.speedFast
            INSTANT -> Strings.speedInstant
        }
    }
    
    // ==================== StateFlows ====================
    // 使用 StateFlow 而非 Flow，确保在重组时保持最新状态
    
    /**
     * 当前主题类型 StateFlow（固定为默认主题）
     */
    val themeTypeFlow: StateFlow<AppThemeType> = kotlinx.coroutines.flow.MutableStateFlow(AppThemeType.KIMI_NO_NAWA)
    
    /**
     * 暗色模式设置 StateFlow
     */
    val darkModeFlow: StateFlow<DarkModeSettings> = context.themeDataStore.data.map { prefs ->
        val modeName = prefs[KEY_DARK_MODE] ?: DarkModeSettings.SYSTEM.name
        try {
            DarkModeSettings.valueOf(modeName)
        } catch (e: Exception) {
            DarkModeSettings.SYSTEM
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = DarkModeSettings.SYSTEM
    )
    
    /**
     * 是否启用动画 StateFlow
     */
    val enableAnimationsFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_ANIMATIONS] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * 是否启用粒子效果 StateFlow
     */
    val enableParticlesFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_PARTICLES] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * 是否启用触觉反馈 StateFlow
     */
    val enableHapticsFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_HAPTICS] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * 是否启用音效 StateFlow
     */
    val enableSoundFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_SOUND] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * 动画速度 StateFlow
     */
    val animationSpeedFlow: StateFlow<AnimationSpeed> = context.themeDataStore.data.map { prefs ->
        val speedName = prefs[KEY_ANIMATION_SPEED] ?: AnimationSpeed.NORMAL.name
        try {
            AnimationSpeed.valueOf(speedName)
        } catch (e: Exception) {
            AnimationSpeed.NORMAL
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AnimationSpeed.NORMAL
    )
    
    // ==================== 设置方法 ====================

    /**
     * 设置暗色模式
     */
    suspend fun setDarkMode(mode: DarkModeSettings) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = mode.name
        }
    }
    
    /**
     * 设置是否启用动画
     */
    suspend fun setEnableAnimations(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_ANIMATIONS] = enabled
        }
    }
    
    /**
     * 设置是否启用粒子效果
     */
    suspend fun setEnableParticles(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_PARTICLES] = enabled
        }
    }
    
    /**
     * 设置是否启用触觉反馈
     */
    suspend fun setEnableHaptics(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_HAPTICS] = enabled
        }
    }
    
    /**
     * 设置是否启用音效
     */
    suspend fun setEnableSound(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_SOUND] = enabled
        }
    }
    
    /**
     * 设置动画速度
     */
    suspend fun setAnimationSpeed(speed: AnimationSpeed) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ANIMATION_SPEED] = speed.name
        }
    }
}
