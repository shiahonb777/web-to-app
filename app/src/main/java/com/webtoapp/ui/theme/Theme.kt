package com.webtoapp.ui.theme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp






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
    primary = Color(0xFF111113),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDEDEF),
    onPrimaryContainer = Color(0xFF111113),
    secondary = Color(0xFF474749),
    onSecondary = Color.White,
    tertiary = Color(0xFF7A7A7D),
    error = Color(0xFF8A1D1D),
    onError = Color.White,
    errorContainer = Color(0xFFF9E4E4),
    onErrorContainer = Color(0xFF3B0F0F),
    background = Color(0xFFFBFBFC),
    onBackground = Color(0xFF111113),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111113),
    surfaceVariant = Color(0xFFF1F1F3),
    onSurfaceVariant = Color(0xFF55555A),
    surfaceContainer = Color(0xFFF6F6F8),
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainerHigh = Color(0xFFEFEFF1),
    surfaceContainerHighest = Color(0xFFE8E8EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFFCACACE),
    outlineVariant = Color(0xFFE4E4E7)
)


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2F2F4),
    onPrimary = Color(0xFF111113),
    primaryContainer = Color(0xFF2A2A2E),
    onPrimaryContainer = Color(0xFFE4E4E7),
    secondary = Color(0xFFB8B8BC),
    error = Color(0xFFE7A3A3),
    onError = Color(0xFF1A0808),
    errorContainer = Color(0xFF3B1515),
    onErrorContainer = Color(0xFFF4D4D4),
    background = Color(0xFF0B0B0E),
    onBackground = Color(0xFFE6E6E9),
    surface = Color(0xFF111114),
    onSurface = Color(0xFFE6E6E9),
    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFFB0B0B4),
    surfaceContainer = Color(0xFF17171A),
    surfaceContainerLow = Color(0xFF121215),
    surfaceContainerHigh = Color(0xFF1C1C20),
    surfaceContainerHighest = Color(0xFF242428),
    surfaceContainerLowest = Color(0xFF08080A),
    outline = Color(0xFF45454A),
    outlineVariant = Color(0xFF2B2B2F)
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
        // extraSmall: chips and small pills
        extraSmall = RoundedCornerShape(6.dp),
        // small: buttons and compact inputs
        small = RoundedCornerShape(10.dp),
        // medium: inner surfaces
        medium = RoundedCornerShape(12.dp),
        // large: cards and bottom sheets
        large = RoundedCornerShape(14.dp),
        // extraLarge: dialogs and modals
        extraLarge = RoundedCornerShape(20.dp)
    )

    CompositionLocalProvider(
        LocalAppTheme provides currentTheme,
        LocalAnimationSettings provides animationSettings,
        LocalIsDarkTheme provides useDarkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WtaTypography,
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
            typography = WtaTypography,
            content = content
        )
    }
}





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
        // extraSmall: chips and small pills
        extraSmall = RoundedCornerShape(6.dp),
        // small: buttons and compact inputs
        small = RoundedCornerShape(10.dp),
        // medium: inner surfaces
        medium = RoundedCornerShape(12.dp),
        // large: cards and bottom sheets
        large = RoundedCornerShape(14.dp),
        // extraLarge: dialogs and modals
        extraLarge = RoundedCornerShape(20.dp)
    )

    CompositionLocalProvider(
        LocalAppTheme provides currentTheme,
        LocalAnimationSettings provides animationSettings,
        LocalIsDarkTheme provides useDarkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WtaTypography,
            shapes = themeShapes,
            content = content
        )
    }
}
