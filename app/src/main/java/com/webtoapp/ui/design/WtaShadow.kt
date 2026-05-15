package com.webtoapp.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.LocalIsDarkTheme

/**
 * Soft, layered shadow that approximates the diffuse feeling of light scattering
 * across a surface. Material's default elevation shadow draws a single hard
 * drop shadow which looks fine on high-contrast backgrounds but feels heavy
 * on our monochrome palette.
 *
 * We can't blur arbitrarily on Compose (RenderEffect is API 31+) so instead we
 * stack two shadows: one tight and tinted, one wider and lighter. This gives
 * cards a "floating on velvet" feel in light mode, and a faint lift in dark
 * mode without the inner ring artefact you normally see.
 *
 * [level] maps to [WtaElevation]: 0 is flat, higher levels cast bigger, more
 * diffuse shadows.
 */
@Composable
fun Modifier.wtaSoftShadow(
    shape: Shape,
    level: Dp = WtaElevation.Level2
): Modifier {
    val (close, far) = wtaShadowColors()
    val base = level.value.coerceIn(0f, 24f)
    if (base <= 0f) return this

    // Tight close-up shadow: adds definition at the object edge.
    val closeRadius = (base * 0.6f).dp
    // Wider ambient shadow: adds depth and softness.
    val farRadius = (base * 1.8f).dp
    return this
        .shadow(
            elevation = farRadius,
            shape = shape,
            ambientColor = far,
            spotColor = far,
            clip = false
        )
        .shadow(
            elevation = closeRadius,
            shape = shape,
            ambientColor = close,
            spotColor = close,
            clip = false
        )
}

/**
 * Shadow colors tuned for the monochrome theme. Light mode uses warm neutral
 * blacks; dark mode uses a near-black that is visibly darker than the
 * background so depth is preserved on OLED displays.
 */
@Composable
@ReadOnlyComposable
private fun wtaShadowColors(): Pair<Color, Color> {
    val isDark = LocalIsDarkTheme.current
    return if (isDark) {
        Color(0xBB000000) to Color(0x66000000)
    } else {
        Color(0x1A000000) to Color(0x0E000000)
    }
}
