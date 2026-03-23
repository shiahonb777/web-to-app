package com.webtoapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ==================== 主题状态 ====================

/**
 * 当前主题的 CompositionLocal
 */
val LocalAppTheme = staticCompositionLocalOf { AppThemes.Default }

/**
 * 动画设置的 CompositionLocal
 */
data class AnimationSettings(
    val enabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val speedMultiplier: Float = 1f,
    val enhancedMode: Boolean = false
)

val LocalAnimationSettings = staticCompositionLocalOf { AnimationSettings() }

// ==================== 默认配色 ====================

// Light Theme Colors (备用)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

// Dark Theme Colors (备用)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ==================== 主题入口 ====================

/**
 * 当前是否为深色主题的 CompositionLocal
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * 应用主题入口（简化版，不需要 isDarkTheme 回调）
 * 支持自定义主题和动态取色
 */
@Composable
fun WebToAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WebToAppTheme(darkTheme, dynamicColor) { _ ->
        content()
    }
}

/**
 * 应用主题入口
 * 支持自定义主题和动态取色
 * @param content 接收一个 Boolean 参数表示当前是否为深色主题
 */
@Composable
fun WebToAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Default关闭动态取色以使用自定义主题
    content: @Composable (isDarkTheme: Boolean) -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    
    // 收集主题设置 - StateFlow 已缓存状态，不会在重组时重置
    val themeType by themeManager.themeTypeFlow.collectAsState()
    val darkModeSetting by themeManager.darkModeFlow.collectAsState()
    val uiMode by themeManager.uiModeFlow.collectAsState()
    val enableAnimations by themeManager.enableAnimationsFlow.collectAsState()
    val enableParticles by themeManager.enableParticlesFlow.collectAsState()
    val enableHaptics by themeManager.enableHapticsFlow.collectAsState()
    val enableSound by themeManager.enableSoundFlow.collectAsState()
    val animationSpeed by themeManager.animationSpeedFlow.collectAsState()
    
    // 确定是否使用暗色模式
    val useDarkTheme = when (darkModeSetting) {
        ThemeManager.DarkModeSettings.SYSTEM -> darkTheme
        ThemeManager.DarkModeSettings.LIGHT -> false
        ThemeManager.DarkModeSettings.DARK -> true
    }
    
    // Get当前主题
    val currentTheme = AppThemes.getTheme(themeType)
    
    // 确定配色方案
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        useDarkTheme -> currentTheme.darkColors
        else -> currentTheme.lightColors
    }
    
    // 动画设置
    val animationSettings = AnimationSettings(
        enabled = enableAnimations,
        particlesEnabled = enableParticles,
        hapticsEnabled = enableHaptics,
        soundEnabled = enableSound,
        speedMultiplier = animationSpeed.multiplier,
        enhancedMode = uiMode == ThemeManager.UiMode.ENHANCED
    )

    CompositionLocalProvider(
        LocalAppTheme provides currentTheme,
        LocalAnimationSettings provides animationSettings,
        LocalIsDarkTheme provides useDarkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = { content(useDarkTheme) }
        )
    }
}

/**
 * 简化版主题入口（用于预览或不需要主题管理的场景）
 */
@Composable
fun WebToAppThemeSimple(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    CompositionLocalProvider(
        LocalAppTheme provides AppThemes.Default,
        LocalAnimationSettings provides AnimationSettings()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val Typography = Typography()

/**
 * Shell 模式主题入口
 * 根据配置文件中的主题类型应用对应主题
 */
@Composable
fun ShellTheme(
    themeTypeName: String = "AURORA",
    darkModeSetting: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    
    // Parse主题类型
    val themeType = try {
        AppThemeType.valueOf(themeTypeName)
    } catch (e: Exception) {
        AppThemeType.AURORA
    }
    
    // 确定是否使用暗色模式
    val useDarkTheme = when (darkModeSetting) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDarkTheme // SYSTEM
    }
    
    // Get当前主题
    val currentTheme = AppThemes.getTheme(themeType)
    
    // 确定配色方案
    val colorScheme = if (useDarkTheme) currentTheme.darkColors else currentTheme.lightColors
    
    // 动画设置（Shell 模式使用默认设置）
    val animationSettings = AnimationSettings(
        enabled = true,
        particlesEnabled = currentTheme.effects.enableParticles,
        hapticsEnabled = true,
        speedMultiplier = 1f
    )

    CompositionLocalProvider(
        LocalAppTheme provides currentTheme,
        LocalAnimationSettings provides animationSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
