package com.webtoapp.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings

/**
 * 应用主题系统
 * 每个主题都有独特的配色、动画和交互风格
 */

// ==================== 主题定义 ====================

/**
 * 主题类型枚举
 */
enum class AppThemeType(val icon: String) {
    AURORA("AutoAwesome"),
    CYBERPUNK("ElectricBolt"),
    SAKURA("FilterVintage"),
    OCEAN("Waves"),
    FOREST("Forest"),
    GALAXY("Stars"),
    VOLCANO("Whatshot"),
    FROST("AcUnit"),
    SUNSET("WbTwilight"),
    MINIMAL("Minimize"),
    NEON_TOKYO("Nightlife"),
    LAVENDER("Spa");
    
    fun getDisplayName(): String = when (this) {
        AURORA -> Strings.themeAurora
        CYBERPUNK -> Strings.themeCyberpunk
        SAKURA -> Strings.themeSakura
        OCEAN -> Strings.themeOcean
        FOREST -> Strings.themeForest
        GALAXY -> Strings.themeGalaxy
        VOLCANO -> Strings.themeVolcano
        FROST -> Strings.themeFrost
        SUNSET -> Strings.themeSunset
        MINIMAL -> Strings.themeMinimal
        NEON_TOKYO -> Strings.themeNeonTokyo
        LAVENDER -> Strings.themeLavender
    }
    
    fun getDescription(): String = when (this) {
        AURORA -> Strings.themeAuroraDesc
        CYBERPUNK -> Strings.themeCyberpunkDesc
        SAKURA -> Strings.themeSakuraDesc
        OCEAN -> Strings.themeOceanDesc
        FOREST -> Strings.themeForestDesc
        GALAXY -> Strings.themeGalaxyDesc
        VOLCANO -> Strings.themeVolcanoDesc
        FROST -> Strings.themeFrostDesc
        SUNSET -> Strings.themeSunsetDesc
        MINIMAL -> Strings.themeMinimalDesc
        NEON_TOKYO -> Strings.themeNeonTokyoDesc
        LAVENDER -> Strings.themeLavenderDesc
    }
}

/**
 * 动画风格类型
 */
enum class AnimationStyle {
    SMOOTH,
    BOUNCY,
    SNAPPY,
    ELEGANT,
    PLAYFUL,
    DRAMATIC;
    
    fun getDisplayName(): String = when (this) {
        SMOOTH -> Strings.animSmooth
        BOUNCY -> Strings.animBouncy
        SNAPPY -> Strings.animSnappy
        ELEGANT -> Strings.animElegant
        PLAYFUL -> Strings.animPlayful
        DRAMATIC -> Strings.animDramatic
    }
}

/**
 * 交互反馈风格
 */
enum class InteractionStyle {
    RIPPLE,
    GLOW,
    SCALE,
    SHAKE,
    MORPH,
    PARTICLE;
    
    fun getDisplayName(): String = when (this) {
        RIPPLE -> Strings.interRipple
        GLOW -> Strings.interGlow
        SCALE -> Strings.interScale
        SHAKE -> Strings.interShake
        MORPH -> Strings.interMorph
        PARTICLE -> Strings.interParticle
    }
}

/**
 * 主题配置
 */
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

/**
 * 主题渐变色
 */
@Stable
data class ThemeGradients(
    val primary: List<Color>,           // 主渐变
    val secondary: List<Color>,         // 次渐变
    val background: List<Color>,        // 背景渐变
    val accent: List<Color>,            // 强调渐变
    val shimmer: List<Color>            // 闪烁渐变
) {
    val primaryBrush: Brush get() = Brush.linearGradient(primary)
    val secondaryBrush: Brush get() = Brush.linearGradient(secondary)
    val backgroundBrush: Brush get() = Brush.linearGradient(background)
    val accentBrush: Brush get() = Brush.linearGradient(accent)
}

/**
 * 主题特效
 */
@Stable
data class ThemeEffects(
    val glowColor: Color,               // 发光颜色
    val glowRadius: Dp,                 // 发光半径
    val shadowColor: Color,             // 阴影颜色
    val shadowElevation: Dp,            // 阴影高度
    val blurRadius: Dp,                 // 模糊半径
    val particleColor: Color,           // 粒子颜色
    val enableParticles: Boolean,       // Yes否启用粒子
    val enableGlow: Boolean,            // Yes否启用发光
    val enableGlassmorphism: Boolean    // Yes否启用玻璃拟态
)

/**
 * 主题形状
 */
@Stable
data class ThemeShapes(
    val cornerRadius: Dp,               // 圆角大小
    val buttonRadius: Dp,               // 按钮圆角
    val cardRadius: Dp,                 // 卡片圆角
    val dialogRadius: Dp,               // 对话框圆角
    val useRoundedButtons: Boolean,     // Yes否使用圆形按钮
    val useSoftShadows: Boolean         // Yes否使用柔和阴影
)

// ==================== 主题定义 ====================

/**
 * 所有预置主题
 */
object AppThemes {
    
    // ========== 1. 极光梦境 ==========
    val Aurora = AppTheme(
        type = AppThemeType.AURORA,
        lightColors = lightColorScheme(
            primary = Color(0xFF7B68EE),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8E0FF),
            onPrimaryContainer = Color(0xFF1F0057),
            secondary = Color(0xFF00CED1),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFB2F5F7),
            onSecondaryContainer = Color(0xFF003D3E),
            tertiary = Color(0xFFFF69B4),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD9EC),
            onTertiaryContainer = Color(0xFF3D0026),
            background = Color(0xFFF8F5FF),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE7E0F4),
            onSurfaceVariant = Color(0xFF48454E),
            outline = Color(0xFF79757F)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFB8A3FF),
            onPrimary = Color(0xFF2D0080),
            primaryContainer = Color(0xFF5B4FC4),
            onPrimaryContainer = Color(0xFFE8E0FF),
            secondary = Color(0xFF4FFFFF),
            onSecondary = Color(0xFF003738),
            secondaryContainer = Color(0xFF004F50),
            onSecondaryContainer = Color(0xFFB2F5F7),
            tertiary = Color(0xFFFFB1D4),
            onTertiary = Color(0xFF5C0F3E),
            tertiaryContainer = Color(0xFF7D2956),
            onTertiaryContainer = Color(0xFFFFD9EC),
            background = Color(0xFF0D0B14),
            onBackground = Color(0xFFE6E1E9),
            surface = Color(0xFF13111A),
            onSurface = Color(0xFFE6E1E9),
            surfaceVariant = Color(0xFF2D2B36),
            onSurfaceVariant = Color(0xFFCAC4D4)
        ),
        animationStyle = AnimationStyle.SMOOTH,
        interactionStyle = InteractionStyle.GLOW,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF7B68EE), Color(0xFF00CED1), Color(0xFFFF69B4)),
            secondary = listOf(Color(0xFF00CED1), Color(0xFF7B68EE)),
            background = listOf(Color(0xFF1A0A2E), Color(0xFF0D1B2A), Color(0xFF0D0B14)),
            accent = listOf(Color(0xFFFF69B4), Color(0xFF7B68EE)),
            shimmer = listOf(Color(0x4000CED1), Color(0x807B68EE), Color(0x40FF69B4))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFF7B68EE),
            glowRadius = 20.dp,
            shadowColor = Color(0x4000CED1),
            shadowElevation = 12.dp,
            blurRadius = 24.dp,
            particleColor = Color(0xFFE0FFFF),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = true
        ),
        shapes = ThemeShapes(
            cornerRadius = 20.dp,
            buttonRadius = 16.dp,
            cardRadius = 24.dp,
            dialogRadius = 28.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )
    
    // ========== 2. 赛博霓虹 ==========
    val Cyberpunk = AppTheme(
        type = AppThemeType.CYBERPUNK,
        lightColors = lightColorScheme(
            primary = Color(0xFFFF00FF),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFFFFD6FF),
            onPrimaryContainer = Color(0xFF3B003B),
            secondary = Color(0xFF00FFFF),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFFCCFFFF),
            onSecondaryContainer = Color(0xFF003333),
            tertiary = Color(0xFFFFFF00),
            onTertiary = Color.Black,
            background = Color(0xFFF5F5F5),
            onBackground = Color(0xFF1A1A1A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1A1A1A)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFF00FF),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF660066),
            onPrimaryContainer = Color(0xFFFFD6FF),
            secondary = Color(0xFF00FFFF),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF006666),
            onSecondaryContainer = Color(0xFFCCFFFF),
            tertiary = Color(0xFFFFFF00),
            onTertiary = Color.Black,
            tertiaryContainer = Color(0xFF666600),
            background = Color(0xFF0A0A0F),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF121218),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF1E1E28),
            outline = Color(0xFFFF00FF)
        ),
        animationStyle = AnimationStyle.SNAPPY,
        interactionStyle = InteractionStyle.GLOW,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFFFF00FF), Color(0xFF00FFFF)),
            secondary = listOf(Color(0xFF00FFFF), Color(0xFFFFFF00)),
            background = listOf(Color(0xFF0A0A0F), Color(0xFF1A0A20), Color(0xFF0A1A1A)),
            accent = listOf(Color(0xFFFF00FF), Color(0xFFFFFF00), Color(0xFF00FFFF)),
            shimmer = listOf(Color(0x60FF00FF), Color(0x6000FFFF), Color(0x60FFFF00))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFFFF00FF),
            glowRadius = 16.dp,
            shadowColor = Color(0x8000FFFF),
            shadowElevation = 8.dp,
            blurRadius = 12.dp,
            particleColor = Color(0xFFFF00FF),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = false
        ),
        shapes = ThemeShapes(
            cornerRadius = 4.dp,
            buttonRadius = 4.dp,
            cardRadius = 8.dp,
            dialogRadius = 8.dp,
            useRoundedButtons = false,
            useSoftShadows = false
        )
    )
    
    // ========== 3. 樱花物语 ==========
    val Sakura = AppTheme(
        type = AppThemeType.SAKURA,
        lightColors = lightColorScheme(
            primary = Color(0xFFFFB7C5),
            onPrimary = Color(0xFF4A1C2B),
            primaryContainer = Color(0xFFFFE4E9),
            onPrimaryContainer = Color(0xFF3D0D1D),
            secondary = Color(0xFFF8BBD9),
            onSecondary = Color(0xFF4A1C35),
            secondaryContainer = Color(0xFFFFE8F3),
            onSecondaryContainer = Color(0xFF370D27),
            tertiary = Color(0xFFFFC1E3),
            onTertiary = Color(0xFF4A1C3D),
            background = Color(0xFFFFF5F7),
            onBackground = Color(0xFF2D1F24),
            surface = Color(0xFFFFFBFC),
            onSurface = Color(0xFF2D1F24),
            surfaceVariant = Color(0xFFFAE8ED),
            outline = Color(0xFFD4A5B2)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFFB7C5),
            onPrimary = Color(0xFF4A1C2B),
            primaryContainer = Color(0xFF8B4D5E),
            onPrimaryContainer = Color(0xFFFFE4E9),
            secondary = Color(0xFFF8BBD9),
            onSecondary = Color(0xFF4A1C35),
            secondaryContainer = Color(0xFF7A3D58),
            background = Color(0xFF1A1316),
            onBackground = Color(0xFFF5E6EA),
            surface = Color(0xFF211A1D),
            onSurface = Color(0xFFF5E6EA),
            surfaceVariant = Color(0xFF3D2E34)
        ),
        animationStyle = AnimationStyle.ELEGANT,
        interactionStyle = InteractionStyle.PARTICLE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFFFFB7C5), Color(0xFFFFC1E3), Color(0xFFFFE4E9)),
            secondary = listOf(Color(0xFFF8BBD9), Color(0xFFFFCDD2)),
            background = listOf(Color(0xFFFFF5F7), Color(0xFFFFE8ED)),
            accent = listOf(Color(0xFFFF8FAB), Color(0xFFFFB7C5)),
            shimmer = listOf(Color(0x40FFB7C5), Color(0x80FFFFFF), Color(0x40FFC1E3))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFFFFB7C5),
            glowRadius = 24.dp,
            shadowColor = Color(0x30FFB7C5),
            shadowElevation = 16.dp,
            blurRadius = 20.dp,
            particleColor = Color(0xFFFFB7C5),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = true
        ),
        shapes = ThemeShapes(
            cornerRadius = 24.dp,
            buttonRadius = 20.dp,
            cardRadius = 28.dp,
            dialogRadius = 32.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )
    
    // ========== 4. 深海幽蓝 ==========
    val Ocean = AppTheme(
        type = AppThemeType.OCEAN,
        lightColors = lightColorScheme(
            primary = Color(0xFF0077B6),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCAF0F8),
            onPrimaryContainer = Color(0xFF001E2B),
            secondary = Color(0xFF00B4D8),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFADE8F4),
            onSecondaryContainer = Color(0xFF001F2A),
            tertiary = Color(0xFF90E0EF),
            onTertiary = Color(0xFF00303D),
            background = Color(0xFFF0FBFF),
            onBackground = Color(0xFF001E2B),
            surface = Color(0xFFF8FDFF),
            onSurface = Color(0xFF001E2B)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFF48CAE4),
            onPrimary = Color(0xFF00303D),
            primaryContainer = Color(0xFF005677),
            onPrimaryContainer = Color(0xFFCAF0F8),
            secondary = Color(0xFF90E0EF),
            onSecondary = Color(0xFF00303D),
            secondaryContainer = Color(0xFF006480),
            background = Color(0xFF03071E),
            onBackground = Color(0xFFCAF0F8),
            surface = Color(0xFF081C2C),
            onSurface = Color(0xFFCAF0F8),
            surfaceVariant = Color(0xFF0A2942)
        ),
        animationStyle = AnimationStyle.SMOOTH,
        interactionStyle = InteractionStyle.RIPPLE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF03071E), Color(0xFF023E8A), Color(0xFF0077B6)),
            secondary = listOf(Color(0xFF0077B6), Color(0xFF00B4D8), Color(0xFF90E0EF)),
            background = listOf(Color(0xFF03071E), Color(0xFF081C2C)),
            accent = listOf(Color(0xFF48CAE4), Color(0xFF90E0EF)),
            shimmer = listOf(Color(0x2090E0EF), Color(0x6048CAE4), Color(0x20CAF0F8))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFF48CAE4),
            glowRadius = 16.dp,
            shadowColor = Color(0x400077B6),
            shadowElevation = 10.dp,
            blurRadius = 16.dp,
            particleColor = Color(0xFFCAF0F8),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = true
        ),
        shapes = ThemeShapes(
            cornerRadius = 16.dp,
            buttonRadius = 12.dp,
            cardRadius = 20.dp,
            dialogRadius = 24.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )
    
    // ========== 5. 森林晨曦 ==========
    val Forest = AppTheme(
        type = AppThemeType.FOREST,
        lightColors = lightColorScheme(
            primary = Color(0xFF2D6A4F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB7E4C7),
            onPrimaryContainer = Color(0xFF002113),
            secondary = Color(0xFF40916C),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD8F3DC),
            onSecondaryContainer = Color(0xFF002112),
            tertiary = Color(0xFF95D5B2),
            onTertiary = Color(0xFF00391F),
            background = Color(0xFFF0FFF4),
            onBackground = Color(0xFF1B3D2F),
            surface = Color(0xFFF8FFF9),
            onSurface = Color(0xFF1B3D2F)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFF74C69D),
            onPrimary = Color(0xFF003920),
            primaryContainer = Color(0xFF1B4332),
            onPrimaryContainer = Color(0xFFB7E4C7),
            secondary = Color(0xFF95D5B2),
            onSecondary = Color(0xFF003920),
            background = Color(0xFF0D1F16),
            onBackground = Color(0xFFD8F3DC),
            surface = Color(0xFF132A1E),
            onSurface = Color(0xFFD8F3DC),
            surfaceVariant = Color(0xFF1B3D2F)
        ),
        animationStyle = AnimationStyle.ELEGANT,
        interactionStyle = InteractionStyle.SCALE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF40916C)),
            secondary = listOf(Color(0xFF52B788), Color(0xFF74C69D), Color(0xFF95D5B2)),
            background = listOf(Color(0xFF0D1F16), Color(0xFF1B3D2F)),
            accent = listOf(Color(0xFF74C69D), Color(0xFFB7E4C7)),
            shimmer = listOf(Color(0x2095D5B2), Color(0x6074C69D), Color(0x20B7E4C7))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFF74C69D),
            glowRadius = 14.dp,
            shadowColor = Color(0x302D6A4F),
            shadowElevation = 8.dp,
            blurRadius = 12.dp,
            particleColor = Color(0xFFD8F3DC),
            enableParticles = false,
            enableGlow = false,
            enableGlassmorphism = false
        ),
        shapes = ThemeShapes(
            cornerRadius = 12.dp,
            buttonRadius = 8.dp,
            cardRadius = 16.dp,
            dialogRadius = 20.dp,
            useRoundedButtons = false,
            useSoftShadows = true
        )
    )
    
    // ========== 6. 星空银河 ==========
    val Galaxy = AppTheme(
        type = AppThemeType.GALAXY,
        lightColors = lightColorScheme(
            primary = Color(0xFF5C4D7D),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8DDFF),
            onPrimaryContainer = Color(0xFF1A0D3D),
            secondary = Color(0xFF7C5CBF),
            onSecondary = Color.White,
            background = Color(0xFFF8F5FF),
            onBackground = Color(0xFF1C1B2E)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFB8A3FF),
            onPrimary = Color(0xFF2D0080),
            primaryContainer = Color(0xFF4A3B7C),
            onPrimaryContainer = Color(0xFFE8DDFF),
            secondary = Color(0xFFCFBCFF),
            onSecondary = Color(0xFF3A1F7D),
            background = Color(0xFF0B0B1A),
            onBackground = Color(0xFFE6E1F9),
            surface = Color(0xFF12122A),
            onSurface = Color(0xFFE6E1F9),
            surfaceVariant = Color(0xFF1E1E3F)
        ),
        animationStyle = AnimationStyle.DRAMATIC,
        interactionStyle = InteractionStyle.PARTICLE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF0B0B1A), Color(0xFF1A1A3E), Color(0xFF2D2D5E)),
            secondary = listOf(Color(0xFF5C4D7D), Color(0xFF7C5CBF), Color(0xFFB8A3FF)),
            background = listOf(Color(0xFF0B0B1A), Color(0xFF12122A), Color(0xFF1A1A3E)),
            accent = listOf(Color(0xFFFFD700), Color(0xFFFFA500)),
            shimmer = listOf(Color(0x20FFFFFF), Color(0x60FFD700), Color(0x20FFFFFF))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFFB8A3FF),
            glowRadius = 20.dp,
            shadowColor = Color(0x605C4D7D),
            shadowElevation = 14.dp,
            blurRadius = 20.dp,
            particleColor = Color(0xFFFFFFFF),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = true
        ),
        shapes = ThemeShapes(
            cornerRadius = 16.dp,
            buttonRadius = 12.dp,
            cardRadius = 20.dp,
            dialogRadius = 24.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )
    
    // ========== 7. 熔岩之心 ==========
    val Volcano = AppTheme(
        type = AppThemeType.VOLCANO,
        lightColors = lightColorScheme(
            primary = Color(0xFFD32F2F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDAD6),
            onPrimaryContainer = Color(0xFF410002),
            secondary = Color(0xFFFF6B35),
            onSecondary = Color.White,
            tertiary = Color(0xFFFFB74D),
            onTertiary = Color(0xFF3E2700),
            background = Color(0xFFFFF8F6),
            onBackground = Color(0xFF2D1A16)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFF6B6B),
            onPrimary = Color(0xFF690005),
            primaryContainer = Color(0xFF930009),
            onPrimaryContainer = Color(0xFFFFDAD6),
            secondary = Color(0xFFFFAB91),
            onSecondary = Color(0xFF5E1900),
            tertiary = Color(0xFFFFD180),
            background = Color(0xFF1A0A08),
            onBackground = Color(0xFFF9DEDC),
            surface = Color(0xFF251412),
            onSurface = Color(0xFFF9DEDC),
            surfaceVariant = Color(0xFF3D2420)
        ),
        animationStyle = AnimationStyle.BOUNCY,
        interactionStyle = InteractionStyle.GLOW,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFFD32F2F), Color(0xFFFF6B35), Color(0xFFFFB74D)),
            secondary = listOf(Color(0xFFFF6B35), Color(0xFFFF9800)),
            background = listOf(Color(0xFF1A0A08), Color(0xFF2D1A16)),
            accent = listOf(Color(0xFFFFD700), Color(0xFFFF6B35)),
            shimmer = listOf(Color(0x40FF6B35), Color(0x80FFD700), Color(0x40FF6B6B))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFFFF6B35),
            glowRadius = 18.dp,
            shadowColor = Color(0x60D32F2F),
            shadowElevation = 12.dp,
            blurRadius = 16.dp,
            particleColor = Color(0xFFFFD700),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = false
        ),
        shapes = ThemeShapes(
            cornerRadius = 8.dp,
            buttonRadius = 6.dp,
            cardRadius = 12.dp,
            dialogRadius = 16.dp,
            useRoundedButtons = false,
            useSoftShadows = false
        )
    )
    
    // ========== 8. 冰晶之境 ==========
    val Frost = AppTheme(
        type = AppThemeType.FROST,
        lightColors = lightColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF00344A),
            primaryContainer = Color(0xFFE1F5FE),
            onPrimaryContainer = Color(0xFF001F2E),
            secondary = Color(0xFF81D4FA),
            onSecondary = Color(0xFF00354C),
            tertiary = Color(0xFFB3E5FC),
            onTertiary = Color(0xFF003548),
            background = Color(0xFFF5FCFF),
            onBackground = Color(0xFF001E2B),
            surface = Color(0xFFFAFEFF),
            onSurface = Color(0xFF001E2B)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF00344A),
            primaryContainer = Color(0xFF0277BD),
            onPrimaryContainer = Color(0xFFE1F5FE),
            secondary = Color(0xFF81D4FA),
            background = Color(0xFF051A24),
            onBackground = Color(0xFFE1F5FE),
            surface = Color(0xFF0A2533),
            onSurface = Color(0xFFE1F5FE),
            surfaceVariant = Color(0xFF0F3345)
        ),
        animationStyle = AnimationStyle.ELEGANT,
        interactionStyle = InteractionStyle.SCALE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC), Color(0xFF81D4FA)),
            secondary = listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7)),
            background = listOf(Color(0xFF051A24), Color(0xFF0A2533)),
            accent = listOf(Color(0xFFFFFFFF), Color(0xFFE1F5FE)),
            shimmer = listOf(Color(0x40FFFFFF), Color(0x80E1F5FE), Color(0x40B3E5FC))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFF4FC3F7),
            glowRadius = 22.dp,
            shadowColor = Color(0x304FC3F7),
            shadowElevation = 10.dp,
            blurRadius = 24.dp,
            particleColor = Color(0xFFFFFFFF),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = true
        ),
        shapes = ThemeShapes(
            cornerRadius = 20.dp,
            buttonRadius = 16.dp,
            cardRadius = 24.dp,
            dialogRadius = 28.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )
    
    // ========== 9. 紫金黄昏 ==========
    val Sunset = AppTheme(
        type = AppThemeType.SUNSET,
        lightColors = lightColorScheme(
            primary = Color(0xFFE65100),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE0B2),
            onPrimaryContainer = Color(0xFF311B00),
            secondary = Color(0xFFFF8A65),
            onSecondary = Color.White,
            tertiary = Color(0xFF9C27B0),
            onTertiary = Color.White,
            background = Color(0xFFFFF8F0),
            onBackground = Color(0xFF2D1F1A)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFFB74D),
            onPrimary = Color(0xFF4A2800),
            primaryContainer = Color(0xFF6E3B00),
            onPrimaryContainer = Color(0xFFFFE0B2),
            secondary = Color(0xFFFFAB91),
            tertiary = Color(0xFFCE93D8),
            background = Color(0xFF1A1010),
            onBackground = Color(0xFFFFE0B2),
            surface = Color(0xFF251A16),
            onSurface = Color(0xFFFFE0B2),
            surfaceVariant = Color(0xFF3D2D24)
        ),
        animationStyle = AnimationStyle.SMOOTH,
        interactionStyle = InteractionStyle.RIPPLE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF9C27B0), Color(0xFFE65100), Color(0xFFFFB74D)),
            secondary = listOf(Color(0xFFFF8A65), Color(0xFFFFB74D)),
            background = listOf(Color(0xFF1A1010), Color(0xFF2D1F1A)),
            accent = listOf(Color(0xFFCE93D8), Color(0xFFFFB74D)),
            shimmer = listOf(Color(0x40FFB74D), Color(0x80FF8A65), Color(0x409C27B0))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFFFFB74D),
            glowRadius = 16.dp,
            shadowColor = Color(0x40E65100),
            shadowElevation = 10.dp,
            blurRadius = 14.dp,
            particleColor = Color(0xFFFFE0B2),
            enableParticles = false,
            enableGlow = true,
            enableGlassmorphism = false
        ),
        shapes = ThemeShapes(
            cornerRadius = 14.dp,
            buttonRadius = 10.dp,
            cardRadius = 18.dp,
            dialogRadius = 22.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )
    
    // ========== 10. 极简主义 ==========
    val Minimal = AppTheme(
        type = AppThemeType.MINIMAL,
        lightColors = lightColorScheme(
            primary = Color(0xFF212121),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF5F5F5),
            onPrimaryContainer = Color(0xFF212121),
            secondary = Color(0xFF616161),
            onSecondary = Color.White,
            tertiary = Color(0xFF9E9E9E),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF212121),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF212121),
            surfaceVariant = Color(0xFFFAFAFA),
            outline = Color(0xFFE0E0E0)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color(0xFF212121),
            primaryContainer = Color(0xFF424242),
            onPrimaryContainer = Color(0xFFE0E0E0),
            secondary = Color(0xFFBDBDBD),
            background = Color(0xFF121212),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF2C2C2C),
            outline = Color(0xFF424242)
        ),
        animationStyle = AnimationStyle.SNAPPY,
        interactionStyle = InteractionStyle.SCALE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF212121), Color(0xFF424242)),
            secondary = listOf(Color(0xFF616161), Color(0xFF9E9E9E)),
            background = listOf(Color(0xFF121212), Color(0xFF1E1E1E)),
            accent = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0)),
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
    
    // ========== 11. 东京霓虹 ==========
    val NeonTokyo = AppTheme(
        type = AppThemeType.NEON_TOKYO,
        lightColors = lightColorScheme(
            primary = Color(0xFFE91E63),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFCE4EC),
            onPrimaryContainer = Color(0xFF3E0021),
            secondary = Color(0xFF00BCD4),
            onSecondary = Color.White,
            tertiary = Color(0xFF8BC34A),
            background = Color(0xFFFFF5F8),
            onBackground = Color(0xFF1A1A1A)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFF4081),
            onPrimary = Color(0xFF4A0027),
            primaryContainer = Color(0xFF880E4F),
            onPrimaryContainer = Color(0xFFFCE4EC),
            secondary = Color(0xFF00E5FF),
            tertiary = Color(0xFFB2FF59),
            background = Color(0xFF0D0D12),
            onBackground = Color(0xFFEAEAEA),
            surface = Color(0xFF141418),
            onSurface = Color(0xFFEAEAEA),
            surfaceVariant = Color(0xFF1F1F28)
        ),
        animationStyle = AnimationStyle.PLAYFUL,
        interactionStyle = InteractionStyle.GLOW,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7)),
            secondary = listOf(Color(0xFF00BCD4), Color(0xFF00E5FF)),
            background = listOf(Color(0xFF0D0D12), Color(0xFF1A1A24)),
            accent = listOf(Color(0xFFFF4081), Color(0xFF00E5FF)),
            shimmer = listOf(Color(0x40FF4081), Color(0x6000E5FF), Color(0x40B2FF59))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFFFF4081),
            glowRadius = 18.dp,
            shadowColor = Color(0x6000E5FF),
            shadowElevation = 10.dp,
            blurRadius = 16.dp,
            particleColor = Color(0xFFFF4081),
            enableParticles = true,
            enableGlow = true,
            enableGlassmorphism = true
        ),
        shapes = ThemeShapes(
            cornerRadius = 6.dp,
            buttonRadius = 4.dp,
            cardRadius = 10.dp,
            dialogRadius = 12.dp,
            useRoundedButtons = false,
            useSoftShadows = false
        )
    )
    
    // ========== 12. 薰衣草田 ==========
    val Lavender = AppTheme(
        type = AppThemeType.LAVENDER,
        lightColors = lightColorScheme(
            primary = Color(0xFF7E57C2),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE7F6),
            onPrimaryContainer = Color(0xFF1A0D33),
            secondary = Color(0xFF9575CD),
            onSecondary = Color.White,
            tertiary = Color(0xFFB39DDB),
            onTertiary = Color(0xFF1A0D33),
            background = Color(0xFFF8F5FF),
            onBackground = Color(0xFF1A0D33),
            surface = Color(0xFFFCFAFF),
            onSurface = Color(0xFF1A0D33),
            surfaceVariant = Color(0xFFF3EDFA)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFB39DDB),
            onPrimary = Color(0xFF2E1657),
            primaryContainer = Color(0xFF4A2C7A),
            onPrimaryContainer = Color(0xFFEDE7F6),
            secondary = Color(0xFFCE93D8),
            background = Color(0xFF0F0A16),
            onBackground = Color(0xFFEDE7F6),
            surface = Color(0xFF16101F),
            onSurface = Color(0xFFEDE7F6),
            surfaceVariant = Color(0xFF241A32)
        ),
        animationStyle = AnimationStyle.ELEGANT,
        interactionStyle = InteractionStyle.RIPPLE,
        gradients = ThemeGradients(
            primary = listOf(Color(0xFF7E57C2), Color(0xFF9575CD), Color(0xFFB39DDB)),
            secondary = listOf(Color(0xFF9575CD), Color(0xFFCE93D8)),
            background = listOf(Color(0xFF0F0A16), Color(0xFF16101F)),
            accent = listOf(Color(0xFFE1BEE7), Color(0xFFB39DDB)),
            shimmer = listOf(Color(0x30B39DDB), Color(0x60E1BEE7), Color(0x30CE93D8))
        ),
        effects = ThemeEffects(
            glowColor = Color(0xFFB39DDB),
            glowRadius = 18.dp,
            shadowColor = Color(0x307E57C2),
            shadowElevation = 10.dp,
            blurRadius = 18.dp,
            particleColor = Color(0xFFE1BEE7),
            enableParticles = false,
            enableGlow = true,
            enableGlassmorphism = true
        ),
        shapes = ThemeShapes(
            cornerRadius = 18.dp,
            buttonRadius = 14.dp,
            cardRadius = 22.dp,
            dialogRadius = 26.dp,
            useRoundedButtons = true,
            useSoftShadows = true
        )
    )
    
    /**
     * 获取所有主题
     */
    val allThemes = listOf(
        Aurora, Cyberpunk, Sakura, Ocean, Forest, Galaxy,
        Volcano, Frost, Sunset, Minimal, NeonTokyo, Lavender
    )
    
    /**
     * 通过类型获取主题
     */
    fun getTheme(type: AppThemeType): AppTheme = allThemes.first { it.type == type }
    
    /**
     * 默认主题
     */
    val Default = Aurora
}

/**
 * 获取本地化的主题显示名称
 */
fun AppThemeType.getLocalizedDisplayName(): String {
    return when (this) {
        AppThemeType.AURORA -> com.webtoapp.core.i18n.Strings.themeAurora
        AppThemeType.CYBERPUNK -> com.webtoapp.core.i18n.Strings.themeCyberpunk
        AppThemeType.SAKURA -> com.webtoapp.core.i18n.Strings.themeSakura
        AppThemeType.OCEAN -> com.webtoapp.core.i18n.Strings.themeOcean
        AppThemeType.FOREST -> com.webtoapp.core.i18n.Strings.themeForest
        AppThemeType.GALAXY -> com.webtoapp.core.i18n.Strings.themeGalaxy
        AppThemeType.VOLCANO -> com.webtoapp.core.i18n.Strings.themeVolcano
        AppThemeType.FROST -> com.webtoapp.core.i18n.Strings.themeFrost
        AppThemeType.SUNSET -> com.webtoapp.core.i18n.Strings.themeSunset
        AppThemeType.MINIMAL -> com.webtoapp.core.i18n.Strings.themeMinimal
        AppThemeType.NEON_TOKYO -> com.webtoapp.core.i18n.Strings.themeNeonTokyo
        AppThemeType.LAVENDER -> com.webtoapp.core.i18n.Strings.themeLavender
    }
}

/**
 * 获取本地化的主题描述
 */
fun AppThemeType.getLocalizedDescription(): String {
    return when (this) {
        AppThemeType.AURORA -> com.webtoapp.core.i18n.Strings.themeAuroraDesc
        AppThemeType.CYBERPUNK -> com.webtoapp.core.i18n.Strings.themeCyberpunkDesc
        AppThemeType.SAKURA -> com.webtoapp.core.i18n.Strings.themeSakuraDesc
        AppThemeType.OCEAN -> com.webtoapp.core.i18n.Strings.themeOceanDesc
        AppThemeType.FOREST -> com.webtoapp.core.i18n.Strings.themeForestDesc
        AppThemeType.GALAXY -> com.webtoapp.core.i18n.Strings.themeGalaxyDesc
        AppThemeType.VOLCANO -> com.webtoapp.core.i18n.Strings.themeVolcanoDesc
        AppThemeType.FROST -> com.webtoapp.core.i18n.Strings.themeFrostDesc
        AppThemeType.SUNSET -> com.webtoapp.core.i18n.Strings.themeSunsetDesc
        AppThemeType.MINIMAL -> com.webtoapp.core.i18n.Strings.themeMinimalDesc
        AppThemeType.NEON_TOKYO -> com.webtoapp.core.i18n.Strings.themeNeonTokyoDesc
        AppThemeType.LAVENDER -> com.webtoapp.core.i18n.Strings.themeLavenderDesc
    }
}

/**
 * 获取本地化的动画风格显示名称
 */
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

/**
 * 获取本地化的交互风格显示名称
 */
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
