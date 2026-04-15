package com.webtoapp.ui.theme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.koinInject

// ==================== Theme State ====================

/**
 * CompositionLocal for the current theme.
 */
val LocalAppTheme = staticCompositionLocalOf { AppThemes.Default }

/**
 * CompositionLocal for animation settings.
 */
data class AnimationSettings(
    val enabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val speedMultiplier: Float = 1f
)

val LocalAnimationSettings = staticCompositionLocalOf { AnimationSettings() }

// ==================== Default Color Schemes ====================

// Light Theme Colors (minimal)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F0F0),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF505050),
    onSecondary = Color.White,
    tertiary = Color(0xFF8A8A8A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFCE4EC),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainer = Color(0xFFF7F7F7),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainerHigh = Color(0xFFF0F0F0),
    surfaceContainerHighest = Color(0xFFEAEAEA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFFD5D5D5),
    outlineVariant = Color(0xFFE8E8E8)
)

// Dark Theme Colors (minimal dark)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEEEEEE),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFFB0B0B0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFA8A8A8),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),
    surfaceContainerLowest = Color(0xFF050505),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A)
)

// ==================== Theme Entrypoints ====================

/**
 * CompositionLocal for whether dark theme is active.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * App theme entry (simplified, no isDarkTheme callback).
 * Supports custom themes and dynamic colors.
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
 * App theme entry.
 * Supports custom themes and dynamic colors.
 * @param content Receives a Boolean indicating whether dark theme is active.
 */
@Composable
fun WebToAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Default
    content: @Composable (isDarkTheme: Boolean) -> Unit
) {
    val context = LocalContext.current
    val themeManager: ThemeManager = koinInject()
    
    // Collect theme settings; StateFlow keeps cached state across recomposition
    val themeType by themeManager.themeTypeFlow.collectAsStateWithLifecycle()
    val darkModeSetting by themeManager.darkModeFlow.collectAsStateWithLifecycle()
    val enableAnimations by themeManager.enableAnimationsFlow.collectAsStateWithLifecycle()
    val enableParticles by themeManager.enableParticlesFlow.collectAsStateWithLifecycle()
    val enableHaptics by themeManager.enableHapticsFlow.collectAsStateWithLifecycle()
    val enableSound by themeManager.enableSoundFlow.collectAsStateWithLifecycle()
    val animationSpeed by themeManager.animationSpeedFlow.collectAsStateWithLifecycle()
    
    // Resolve whether to use dark mode
    val useDarkTheme = when (darkModeSetting) {
        ThemeManager.DarkModeSettings.SYSTEM -> darkTheme
        ThemeManager.DarkModeSettings.LIGHT -> false
        ThemeManager.DarkModeSettings.DARK -> true
    }
    
    // Get current theme
    val currentTheme = AppThemes.getTheme(themeType)
    
    // Resolve color scheme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        useDarkTheme -> currentTheme.darkColors
        else -> currentTheme.lightColors
    }
    
    // Animation settings
    val animationSettings = AnimationSettings(
        enabled = enableAnimations,
        particlesEnabled = enableParticles,
        hapticsEnabled = enableHaptics,
        soundEnabled = enableSound,
        speedMultiplier = animationSpeed.multiplier
    )

    // Build Material3 Shapes from theme shape config
    val themeShapes = Shapes(
        extraSmall = RoundedCornerShape(currentTheme.shapes.cornerRadius * 0.25f),
        small = RoundedCornerShape(currentTheme.shapes.buttonRadius),
        medium = RoundedCornerShape(currentTheme.shapes.cardRadius * 0.75f),
        large = RoundedCornerShape(currentTheme.shapes.cardRadius),
        extraLarge = RoundedCornerShape(currentTheme.shapes.dialogRadius)
    )

    CompositionLocalProvider(
        LocalAppTheme provides currentTheme,
        LocalAnimationSettings provides animationSettings,
        LocalIsDarkTheme provides useDarkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = themeShapes,
            content = { content(useDarkTheme) }
        )
    }
}

/**
 * Simplified theme entry (for previews or unmanaged theme scenarios).
 */
@Composable
fun WebToAppThemeSimple(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    CompositionLocalProvider(
        LocalAppTheme provides AppThemes.Default,
        LocalAnimationSettings provides AnimationSettings(),
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
 * Shell mode theme entry.
 * Applies theme by theme type from config.
 */
@Composable
fun ShellTheme(
    themeTypeName: String = "KIMI_NO_NAWA",
    darkModeSetting: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    
    // Parse theme type
    val themeType = try {
        AppThemeType.valueOf(themeTypeName)
    } catch (e: Exception) {
        AppThemeType.KIMI_NO_NAWA
    }
    
    // Resolve whether to use dark mode
    val useDarkTheme = when (darkModeSetting) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDarkTheme // SYSTEM
    }
    
    // Get current theme
    val currentTheme = AppThemes.getTheme(themeType)
    
    // Resolve color scheme
    val colorScheme = if (useDarkTheme) currentTheme.darkColors else currentTheme.lightColors
    
    // Animation settings (Shell mode uses defaults)
    val animationSettings = AnimationSettings(
        enabled = true,
        particlesEnabled = currentTheme.effects.enableParticles,
        hapticsEnabled = true,
        speedMultiplier = 1f
    )

    val themeShapes = Shapes(
        extraSmall = RoundedCornerShape(currentTheme.shapes.cornerRadius * 0.25f),
        small = RoundedCornerShape(currentTheme.shapes.buttonRadius),
        medium = RoundedCornerShape(currentTheme.shapes.cardRadius * 0.75f),
        large = RoundedCornerShape(currentTheme.shapes.cardRadius),
        extraLarge = RoundedCornerShape(currentTheme.shapes.dialogRadius)
    )

    CompositionLocalProvider(
        LocalAppTheme provides currentTheme,
        LocalAnimationSettings provides animationSettings,
        LocalIsDarkTheme provides useDarkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = themeShapes,
            content = content
        )
    }
}
