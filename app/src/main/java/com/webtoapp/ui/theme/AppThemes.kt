package com.webtoapp.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * App theme system (minimal palette).
 */

// ==================== Theme Definitions ====================

enum class AppThemeType(val icon: String) {
    KIMI_NO_NAWA("Minimize");
    
    fun getDisplayName(): String = AppStringsProvider.current().themeKimiNoNawa
    fun getDescription(): String = AppStringsProvider.current().themeKimiNoNawaDesc
}

enum class AnimationStyle {
    SMOOTH, BOUNCY, SNAPPY, ELEGANT, PLAYFUL, DRAMATIC;
    
    fun getDisplayName(): String = when (this) {
        SMOOTH -> AppStringsProvider.current().animSmooth
        BOUNCY -> AppStringsProvider.current().animBouncy
        SNAPPY -> AppStringsProvider.current().animSnappy
        ELEGANT -> AppStringsProvider.current().animElegant
        PLAYFUL -> AppStringsProvider.current().animPlayful
        DRAMATIC -> AppStringsProvider.current().animDramatic
    }
}

enum class InteractionStyle {
    RIPPLE, GLOW, SCALE, SHAKE, MORPH, PARTICLE;
    
    fun getDisplayName(): String = when (this) {
        RIPPLE -> AppStringsProvider.current().interRipple
        GLOW -> AppStringsProvider.current().interGlow
        SCALE -> AppStringsProvider.current().interScale
        SHAKE -> AppStringsProvider.current().interShake
        MORPH -> AppStringsProvider.current().interMorph
        PARTICLE -> AppStringsProvider.current().interParticle
    }
}

@Stable
data class AppTheme(
    val type: AppThemeType,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme,
    val animationStyle: AnimationStyle,
    val interactionStyle: InteractionStyle,
    val gradients: ThemeGradients,
    val effects: ThemeEffects,
    val shapes: ThemeShapes
)

@Stable
data class ThemeGradients(
    val primary: List<Color>,
    val secondary: List<Color>,
    val background: List<Color>,
    val accent: List<Color>,
    val shimmer: List<Color>
) {
    val primaryBrush: Brush get() = Brush.linearGradient(primary)
    val secondaryBrush: Brush get() = Brush.linearGradient(secondary)
    val backgroundBrush: Brush get() = Brush.linearGradient(background)
    val accentBrush: Brush get() = Brush.linearGradient(accent)
}

@Stable
data class ThemeEffects(
    val glowColor: Color,
    val glowRadius: Dp,
    val shadowColor: Color,
    val shadowElevation: Dp,
    val blurRadius: Dp,
    val particleColor: Color,
    val enableParticles: Boolean,
    val enableGlow: Boolean,
    val enableGlassmorphism: Boolean
)

@Stable
data class ThemeShapes(
    val cornerRadius: Dp,
    val buttonRadius: Dp,
    val cardRadius: Dp,
    val dialogRadius: Dp,
    val useRoundedButtons: Boolean,
    val useSoftShadows: Boolean
)

// ==================== Theme Definitions ====================

object AppThemes {
    
    // ========== Minimalism ==========
    val KimiNoNawa = AppTheme(
        type = AppThemeType.KIMI_NO_NAWA,
        lightColors = lightColorScheme(
            // Primary palette: pure black/white base
            primary = Color(0xFF1A1A1A),            // #212121
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF0F0F0),   // Comment
            onPrimaryContainer = Color(0xFF1A1A1A),
            inversePrimary = Color(0xFFD0D0D0),

            // Secondary palette
            secondary = Color(0xFF505050),           // Comment
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF5F5F5),
            onSecondaryContainer = Color(0xFF1A1A1A),

            // Tertiary palette
            tertiary = Color(0xFF8A8A8A),            // Comment
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFF5F5F5),
            onTertiaryContainer = Color(0xFF1A1A1A),

            // Error palette
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFCE4EC),
            onErrorContainer = Color(0xFF410002),

            // Background & surface (white base + fine gray levels)
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1A1A1A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFF5F5F5),      // Comment
            onSurfaceVariant = Color(0xFF5C5C5C),    // Comment

            // Surface container levels (clear visual boundaries)
            surfaceTint = Color(0xFF1A1A1A),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFF0F0F0),
            surfaceContainer = Color(0xFFF7F7F7),        // Comment
            surfaceContainerLow = Color(0xFFFAFAFA),     // Comment
            surfaceContainerHigh = Color(0xFFF0F0F0),    // Comment
            surfaceContainerHighest = Color(0xFFEAEAEA), // Comment
            surfaceContainerLowest = Color(0xFFFFFFFF),   // Comment

            // Divider & outline
            outline = Color(0xFFD5D5D5),             // Comment
            outlineVariant = Color(0xFFE8E8E8),      // Comment

            // Inverse colors & scrim
            inverseSurface = Color(0xFF2F2F2F),
            inverseOnSurface = Color(0xFFF0F0F0),
            scrim = Color(0xFF000000)
        ),
        darkColors = darkColorScheme(
            // Primary palette: white primary on black background
            primary = Color(0xFFEEEEEE),             // Comment
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFF303030),    // Comment
            onPrimaryContainer = Color(0xFFE0E0E0),
            inversePrimary = Color(0xFF505050),

            // Secondary palette
            secondary = Color(0xFFB0B0B0),           // Comment
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = Color(0xFF2A2A2A),
            onSecondaryContainer = Color(0xFFD8D8D8),

            // Tertiary palette
            tertiary = Color(0xFF808080),
            onTertiary = Color(0xFF1A1A1A),
            tertiaryContainer = Color(0xFF2A2A2A),
            onTertiaryContainer = Color(0xFFD0D0D0),

            // Error palette
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),

            // Background & surface (black base + fine gray levels)
            background = Color(0xFF000000),           // Comment
            onBackground = Color(0xFFE3E3E3),
            surface = Color(0xFF0A0A0A),              // Comment
            onSurface = Color(0xFFE3E3E3),
            surfaceVariant = Color(0xFF1E1E1E),
            onSurfaceVariant = Color(0xFFA8A8A8),

            // Surface container levels (clear dark-level transitions)
            surfaceTint = Color(0xFFEEEEEE),
            surfaceBright = Color(0xFF3A3A3A),
            surfaceDim = Color(0xFF000000),
            surfaceContainer = Color(0xFF141414),         // Comment
            surfaceContainerLow = Color(0xFF0E0E0E),     // Comment
            surfaceContainerHigh = Color(0xFF1C1C1C),    // Comment
            surfaceContainerHighest = Color(0xFF262626),  // Comment
            surfaceContainerLowest = Color(0xFF050505),   // Comment

            // Divider & outline
            outline = Color(0xFF3A3A3A),              // Comment
            outlineVariant = Color(0xFF2A2A2A),       // Comment

            // Inverse colors & scrim
            inverseSurface = Color(0xFFE3E3E3),
            inverseOnSurface = Color(0xFF1A1A1A),
            scrim = Color(0xFF000000)
        ),
        animationStyle = AnimationStyle.SNAPPY,
        interactionStyle = InteractionStyle.SCALE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF1A1A1A), Color(0xFF3A3A3A)),
            secondary = listOf(Color(0xFF505050), Color(0xFF8A8A8A)),
            background = listOf(Color(0xFF000000), Color(0xFF0A0A0A)),
            accent = listOf(Color(0xFFEEEEEE), Color(0xFFD0D0D0)),
            shimmer = listOf(Color(0x10FFFFFF), Color(0x30FFFFFF), Color(0x10FFFFFF))
        ),
        effects = ThemeEffects(
            glowColor = Color.Transparent,
            glowRadius = 0.dp,
            shadowColor = Color(0x10000000),
            shadowElevation = 4.dp,
            blurRadius = 0.dp,
            particleColor = Color.Transparent,
            enableParticles = false,
            enableGlow = false,
            enableGlassmorphism = false
        ),
        shapes = ThemeShapes(
            cornerRadius = 8.dp,
            buttonRadius = 4.dp,
            cardRadius = 8.dp,
            dialogRadius = 12.dp,
            useRoundedButtons = false,
            useSoftShadows = false
        )
    )
    
    val allThemes = listOf(KimiNoNawa)
    fun getTheme(type: AppThemeType): AppTheme = KimiNoNawa
    val Default = KimiNoNawa
}

fun AppThemeType.getLocalizedDisplayName(): String {
    return com.webtoapp.core.i18n.AppStringsProvider.current().themeKimiNoNawa
}

fun AppThemeType.getLocalizedDescription(): String {
    return com.webtoapp.core.i18n.AppStringsProvider.current().themeKimiNoNawaDesc
}

fun AnimationStyle.getLocalizedDisplayName(): String {
    return when (this) {
        AnimationStyle.SMOOTH -> com.webtoapp.core.i18n.AppStringsProvider.current().animSmooth
        AnimationStyle.BOUNCY -> com.webtoapp.core.i18n.AppStringsProvider.current().animBouncy
        AnimationStyle.SNAPPY -> com.webtoapp.core.i18n.AppStringsProvider.current().animSnappy
        AnimationStyle.ELEGANT -> com.webtoapp.core.i18n.AppStringsProvider.current().animElegant
        AnimationStyle.PLAYFUL -> com.webtoapp.core.i18n.AppStringsProvider.current().animPlayful
        AnimationStyle.DRAMATIC -> com.webtoapp.core.i18n.AppStringsProvider.current().animDramatic
    }
}

fun InteractionStyle.getLocalizedDisplayName(): String {
    return when (this) {
        InteractionStyle.RIPPLE -> com.webtoapp.core.i18n.AppStringsProvider.current().interRipple
        InteractionStyle.GLOW -> com.webtoapp.core.i18n.AppStringsProvider.current().interGlow
        InteractionStyle.SCALE -> com.webtoapp.core.i18n.AppStringsProvider.current().interScale
        InteractionStyle.SHAKE -> com.webtoapp.core.i18n.AppStringsProvider.current().interShake
        InteractionStyle.MORPH -> com.webtoapp.core.i18n.AppStringsProvider.current().interMorph
        InteractionStyle.PARTICLE -> com.webtoapp.core.i18n.AppStringsProvider.current().interParticle
    }
}
