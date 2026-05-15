package com.webtoapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings


enum class AppThemeType(val icon: String) {
    KIMI_NO_NAWA("Minimize");

    fun getDisplayName(): String = Strings.themeKimiNoNawa
    fun getDescription(): String = Strings.themeKimiNoNawaDesc
}

enum class AnimationStyle {
    SMOOTH, BOUNCY, SNAPPY, ELEGANT, PLAYFUL, DRAMATIC;

    fun getDisplayName(): String = when (this) {
        SMOOTH -> Strings.animSmooth
        BOUNCY -> Strings.animBouncy
        SNAPPY -> Strings.animSnappy
        ELEGANT -> Strings.animElegant
        PLAYFUL -> Strings.animPlayful
        DRAMATIC -> Strings.animDramatic
    }
}

enum class InteractionStyle {
    RIPPLE, GLOW, SCALE, SHAKE, MORPH, PARTICLE;

    fun getDisplayName(): String = when (this) {
        RIPPLE -> Strings.interRipple
        GLOW -> Strings.interGlow
        SCALE -> Strings.interScale
        SHAKE -> Strings.interShake
        MORPH -> Strings.interMorph
        PARTICLE -> Strings.interParticle
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


object AppThemes {

    /**
     * Single monochrome theme. Calibrated for a refined editorial look:
     *  - Near-black instead of pure black in dark mode for a softer OLED-friendly base
     *  - Distinct surfaceContainer steps so stacked cards read as layers
     *  - Generous corner radius and subtle ambient shadow for a modern, tactile feel
     */
    val KimiNoNawa = AppTheme(
        type = AppThemeType.KIMI_NO_NAWA,
        lightColors = lightColorScheme(
            primary = Color(0xFF111113),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE8E8EB),
            onPrimaryContainer = Color(0xFF111113),
            inversePrimary = Color(0xFFD5D5D8),

            secondary = Color(0xFF3E3E40),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFEEEEF1),
            onSecondaryContainer = Color(0xFF1A1A1D),

            tertiary = Color(0xFF6E6E71),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFEFEFF2),
            onTertiaryContainer = Color(0xFF1A1A1D),

            error = Color(0xFF8A1D1D),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9E4E4),
            onErrorContainer = Color(0xFF3B0F0F),

            background = Color(0xFFF7F7F9),
            onBackground = Color(0xFF111113),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF111113),
            surfaceVariant = Color(0xFFEEEEF1),
            onSurfaceVariant = Color(0xFF5A5A5F),

            surfaceTint = Color(0xFF111113),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFEAEAEE),
            surfaceContainer = Color(0xFFF3F3F5),
            surfaceContainerLow = Color(0xFFF7F7F9),
            surfaceContainerHigh = Color(0xFFEDEDF0),
            surfaceContainerHighest = Color(0xFFE5E5E9),
            surfaceContainerLowest = Color(0xFFFFFFFF),

            outline = Color(0xFFBEBEC3),
            outlineVariant = Color(0xFFDFDFE3),

            inverseSurface = Color(0xFF29292C),
            inverseOnSurface = Color(0xFFF2F2F4),
            scrim = Color(0xFF000000)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFF2F2F4),
            onPrimary = Color(0xFF111113),
            primaryContainer = Color(0xFF2A2A2E),
            onPrimaryContainer = Color(0xFFE4E4E7),
            inversePrimary = Color(0xFF5A5A5E),

            secondary = Color(0xFFBDBDC1),
            onSecondary = Color(0xFF111113),
            secondaryContainer = Color(0xFF242428),
            onSecondaryContainer = Color(0xFFDCDCE0),

            tertiary = Color(0xFF8A8A8E),
            onTertiary = Color(0xFF111113),
            tertiaryContainer = Color(0xFF232327),
            onTertiaryContainer = Color(0xFFD4D4D8),

            error = Color(0xFFE7A3A3),
            onError = Color(0xFF1A0808),
            errorContainer = Color(0xFF3B1515),
            onErrorContainer = Color(0xFFF4D4D4),

            background = Color(0xFF0A0A0C),
            onBackground = Color(0xFFEBEBEE),
            surface = Color(0xFF121215),
            onSurface = Color(0xFFEBEBEE),
            surfaceVariant = Color(0xFF1E1E22),
            onSurfaceVariant = Color(0xFFB2B2B7),

            surfaceTint = Color(0xFFF2F2F4),
            surfaceBright = Color(0xFF2F2F33),
            surfaceDim = Color(0xFF0A0A0C),
            surfaceContainer = Color(0xFF17171B),
            surfaceContainerLow = Color(0xFF131317),
            surfaceContainerHigh = Color(0xFF1D1D21),
            surfaceContainerHighest = Color(0xFF25252A),
            surfaceContainerLowest = Color(0xFF08080A),

            outline = Color(0xFF4A4A4F),
            outlineVariant = Color(0xFF2C2C30),

            inverseSurface = Color(0xFFE6E6E9),
            inverseOnSurface = Color(0xFF111113),
            scrim = Color(0xFF000000)
        ),
        animationStyle = AnimationStyle.SNAPPY,
        interactionStyle = InteractionStyle.SCALE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF111113), Color(0xFF2A2A2E)),
            secondary = listOf(Color(0xFF474749), Color(0xFF7A7A7D)),
            background = listOf(Color(0xFF0B0B0E), Color(0xFF111114)),
            accent = listOf(Color(0xFFF2F2F4), Color(0xFFD5D5D8)),
            shimmer = listOf(Color(0x08FFFFFF), Color(0x22FFFFFF), Color(0x08FFFFFF))
        ),
        effects = ThemeEffects(
            glowColor = Color.Transparent,
            glowRadius = 0.dp,
            shadowColor = Color(0x14000000),
            shadowElevation = 2.dp,
            blurRadius = 0.dp,
            particleColor = Color.Transparent,
            enableParticles = false,
            enableGlow = false,
            enableGlassmorphism = false
        ),
        shapes = ThemeShapes(
            cornerRadius = 14.dp,
            buttonRadius = 10.dp,
            cardRadius = 14.dp,
            dialogRadius = 20.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )

    val allThemes = listOf(KimiNoNawa)
    fun getTheme(type: AppThemeType): AppTheme = KimiNoNawa
    val Default = KimiNoNawa
}

fun AppThemeType.getLocalizedDisplayName(): String {
    return Strings.themeKimiNoNawa
}

fun AppThemeType.getLocalizedDescription(): String {
    return Strings.themeKimiNoNawaDesc
}

fun AnimationStyle.getLocalizedDisplayName(): String {
    return when (this) {
        AnimationStyle.SMOOTH -> Strings.animSmooth
        AnimationStyle.BOUNCY -> Strings.animBouncy
        AnimationStyle.SNAPPY -> Strings.animSnappy
        AnimationStyle.ELEGANT -> Strings.animElegant
        AnimationStyle.PLAYFUL -> Strings.animPlayful
        AnimationStyle.DRAMATIC -> Strings.animDramatic
    }
}

fun InteractionStyle.getLocalizedDisplayName(): String {
    return when (this) {
        InteractionStyle.RIPPLE -> Strings.interRipple
        InteractionStyle.GLOW -> Strings.interGlow
        InteractionStyle.SCALE -> Strings.interScale
        InteractionStyle.SHAKE -> Strings.interShake
        InteractionStyle.MORPH -> Strings.interMorph
        InteractionStyle.PARTICLE -> Strings.interParticle
    }
}
