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


    val KimiNoNawa = AppTheme(
        type = AppThemeType.KIMI_NO_NAWA,
        lightColors = lightColorScheme(

            primary = Color(0xFF1A1A1A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF0F0F0),
            onPrimaryContainer = Color(0xFF1A1A1A),
            inversePrimary = Color(0xFFD0D0D0),


            secondary = Color(0xFF505050),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF5F5F5),
            onSecondaryContainer = Color(0xFF1A1A1A),


            tertiary = Color(0xFF8A8A8A),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFF5F5F5),
            onTertiaryContainer = Color(0xFF1A1A1A),


            error = Color(0xFF2A2A2A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFEAEAEA),
            onErrorContainer = Color(0xFF1A1A1A),


            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1A1A1A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFF5C5C5C),


            surfaceTint = Color(0xFF1A1A1A),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFF0F0F0),
            surfaceContainer = Color(0xFFF7F7F7),
            surfaceContainerLow = Color(0xFFFAFAFA),
            surfaceContainerHigh = Color(0xFFF0F0F0),
            surfaceContainerHighest = Color(0xFFEAEAEA),
            surfaceContainerLowest = Color(0xFFFFFFFF),


            outline = Color(0xFFD5D5D5),
            outlineVariant = Color(0xFFE8E8E8),


            inverseSurface = Color(0xFF2F2F2F),
            inverseOnSurface = Color(0xFFF0F0F0),
            scrim = Color(0xFF000000)
        ),
        darkColors = darkColorScheme(

            primary = Color(0xFFEEEEEE),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFF303030),
            onPrimaryContainer = Color(0xFFE0E0E0),
            inversePrimary = Color(0xFF505050),


            secondary = Color(0xFFB0B0B0),
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = Color(0xFF2A2A2A),
            onSecondaryContainer = Color(0xFFD8D8D8),


            tertiary = Color(0xFF808080),
            onTertiary = Color(0xFF1A1A1A),
            tertiaryContainer = Color(0xFF2A2A2A),
            onTertiaryContainer = Color(0xFFD0D0D0),


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


            surfaceTint = Color(0xFFEEEEEE),
            surfaceBright = Color(0xFF3A3A3A),
            surfaceDim = Color(0xFF000000),
            surfaceContainer = Color(0xFF141414),
            surfaceContainerLow = Color(0xFF0E0E0E),
            surfaceContainerHigh = Color(0xFF1C1C1C),
            surfaceContainerHighest = Color(0xFF262626),
            surfaceContainerLowest = Color(0xFF050505),


            outline = Color(0xFF3A3A3A),
            outlineVariant = Color(0xFF2A2A2A),


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
    return com.webtoapp.core.i18n.Strings.themeKimiNoNawa
}

fun AppThemeType.getLocalizedDescription(): String {
    return com.webtoapp.core.i18n.Strings.themeKimiNoNawaDesc
}

fun AnimationStyle.getLocalizedDisplayName(): String {
    return when (this) {
        AnimationStyle.SMOOTH -> com.webtoapp.core.i18n.Strings.animSmooth
        AnimationStyle.BOUNCY -> com.webtoapp.core.i18n.Strings.animBouncy
        AnimationStyle.SNAPPY -> com.webtoapp.core.i18n.Strings.animSnappy
        AnimationStyle.ELEGANT -> com.webtoapp.core.i18n.Strings.animElegant
        AnimationStyle.PLAYFUL -> com.webtoapp.core.i18n.Strings.animPlayful
        AnimationStyle.DRAMATIC -> com.webtoapp.core.i18n.Strings.animDramatic
    }
}

fun InteractionStyle.getLocalizedDisplayName(): String {
    return when (this) {
        InteractionStyle.RIPPLE -> com.webtoapp.core.i18n.Strings.interRipple
        InteractionStyle.GLOW -> com.webtoapp.core.i18n.Strings.interGlow
        InteractionStyle.SCALE -> com.webtoapp.core.i18n.Strings.interScale
        InteractionStyle.SHAKE -> com.webtoapp.core.i18n.Strings.interShake
        InteractionStyle.MORPH -> com.webtoapp.core.i18n.Strings.interMorph
        InteractionStyle.PARTICLE -> com.webtoapp.core.i18n.Strings.interParticle
    }
}
