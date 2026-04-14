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
 * Theme manager.
 * Handles theme switching and persistence.
 * Uses StateFlow to keep state stable across recomposition.
 */
@SuppressLint("StaticFieldLeak")
class ThemeManager(private val context: Context) {
    
    // Use a dedicated CoroutineScope for StateFlow
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    companion object {
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")
        private val KEY_ENABLE_PARTICLES = booleanPreferencesKey("enable_particles")
        private val KEY_ENABLE_HAPTICS = booleanPreferencesKey("enable_haptics")
        private val KEY_ENABLE_SOUND = booleanPreferencesKey("enable_sound")
        private val KEY_ANIMATION_SPEED = stringPreferencesKey("animation_speed")
    }
    
    /**
     * Dark mode setting.
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
     * Animation speed.
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
    // Use StateFlow instead of Flow to keep latest state on recomposition
    
    /**
     * Current theme type StateFlow (fixed to default theme).
     */
    val themeTypeFlow: StateFlow<AppThemeType> = kotlinx.coroutines.flow.MutableStateFlow(AppThemeType.KIMI_NO_NAWA)
    
    /**
     * Dark mode StateFlow.
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
     * Animation-enabled StateFlow.
     */
    val enableAnimationsFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_ANIMATIONS] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * Particle-enabled StateFlow.
     */
    val enableParticlesFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_PARTICLES] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * Haptic-enabled StateFlow.
     */
    val enableHapticsFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_HAPTICS] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * Sound-enabled StateFlow.
     */
    val enableSoundFlow: StateFlow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_ENABLE_SOUND] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    
    /**
     * Animation speed StateFlow.
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
    
    // ==================== Setters ====================

    /**
     * Set dark mode.
     */
    suspend fun setDarkMode(mode: DarkModeSettings) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = mode.name
        }
    }
    
    /**
     * Set animation enabled.
     */
    suspend fun setEnableAnimations(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_ANIMATIONS] = enabled
        }
    }
    
    /**
     * Set particle effects enabled.
     */
    suspend fun setEnableParticles(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_PARTICLES] = enabled
        }
    }
    
    /**
     * Set haptic feedback enabled.
     */
    suspend fun setEnableHaptics(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_HAPTICS] = enabled
        }
    }
    
    /**
     * Set sound effects enabled.
     */
    suspend fun setEnableSound(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ENABLE_SOUND] = enabled
        }
    }
    
    /**
     * Set animation speed.
     */
    suspend fun setAnimationSpeed(speed: AnimationSpeed) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_ANIMATION_SPEED] = speed.name
        }
    }
}
