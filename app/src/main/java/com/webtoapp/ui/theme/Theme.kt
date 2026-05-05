package com.webtoapp.ui.theme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext






val LocalAppTheme = staticCompositionLocalOf { AppThemes.Default }




data class AnimationSettings(
    val enabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val speedMultiplier: Float = 1f
)

val LocalAnimationSettings = staticCompositionLocalOf { AnimationSettings() }




private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F0F0),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF505050),
    onSecondary = Color.White,
    tertiary = Color(0xFF8A8A8A),
    error = Color(0xFF2A2A2A),
    onError = Color.White,
    errorContainer = Color(0xFFEAEAEA),
    onErrorContainer = Color(0xFF1A1A1A),
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


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEEEEEE),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFFB0B0B0),
    error = Color(0xFFDADADA),
    onError = Color(0xFF1A1A1A),
    errorContainer = Color(0xFF303030),
    onErrorContainer = Color(0xFFE0E0E0),
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






val LocalIsDarkTheme = staticCompositionLocalOf { false }





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






@Composable
fun WebToAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable (isDarkTheme: Boolean) -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }


    val themeType by themeManager.themeTypeFlow.collectAsStateWithLifecycle()
    val darkModeSetting by themeManager.darkModeFlow.collectAsStateWithLifecycle()
    val enableAnimations by themeManager.enableAnimationsFlow.collectAsStateWithLifecycle()
    val enableParticles by themeManager.enableParticlesFlow.collectAsStateWithLifecycle()
    val enableHaptics by themeManager.enableHapticsFlow.collectAsStateWithLifecycle()
    val enableSound by themeManager.enableSoundFlow.collectAsStateWithLifecycle()
    val animationSpeed by themeManager.animationSpeedFlow.collectAsStateWithLifecycle()


    val useDarkTheme = when (darkModeSetting) {
        ThemeManager.DarkModeSettings.SYSTEM -> darkTheme
        ThemeManager.DarkModeSettings.LIGHT -> false
        ThemeManager.DarkModeSettings.DARK -> true
    }


    val currentTheme = AppThemes.getTheme(themeType)


    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        useDarkTheme -> currentTheme.darkColors
        else -> currentTheme.lightColors
    }


    val animationSettings = AnimationSettings(
        enabled = enableAnimations,
        particlesEnabled = enableParticles,
        hapticsEnabled = enableHaptics,
        soundEnabled = enableSound,
        speedMultiplier = animationSpeed.multiplier
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
            content = { content(useDarkTheme) }
        )
    }
}




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





@Composable
fun ShellTheme(
    themeTypeName: String = "KIMI_NO_NAWA",
    darkModeSetting: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()


    val themeType = try {
        AppThemeType.valueOf(themeTypeName)
    } catch (e: Exception) {
        AppThemeType.KIMI_NO_NAWA
    }


    val useDarkTheme = when (darkModeSetting) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDarkTheme
    }


    val currentTheme = AppThemes.getTheme(themeType)


    val colorScheme = if (useDarkTheme) currentTheme.darkColors else currentTheme.lightColors


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
