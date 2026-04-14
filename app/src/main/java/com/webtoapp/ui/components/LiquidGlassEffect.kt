package com.webtoapp.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.*
import androidx.compose.ui.graphics.Color

// ==================== macOS / iOS system ====================

/**
 * Modifier: macOS
 *
 * ( macOS Sonoma / iOS 18)
 * 1. /- , /
 * 2. ( 0. 5dp)
 * 3. top gradient- from
 * 4. - Material elevation
 * 5. ( Android 12+)
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 24.dp,
    tintAlpha: Float = 0.08f,
    borderAlpha: Float = 0.18f,
    shadowElevation: Dp = 8.dp,
    enableHighlight: Boolean = true
): Modifier {
    val isDark = LocalIsDarkTheme.current

    // macOS, +
    val fillColor = if (isDark)
        Color.White.copy(alpha = tintAlpha * 0.8f)
    else
        Color.White.copy(alpha = 0.78f)       // Note


    val borderColor = if (isDark)
        Color.White.copy(alpha = borderAlpha * 0.5f)
    else
        Color.White.copy(alpha = borderAlpha * 1.5f)

    val shadowColor = if (isDark)
        Color.Black.copy(alpha = 0.45f)
    else
        Color.Black.copy(alpha = 0.06f)

    val shape = RoundedCornerShape(cornerRadius)

    return this
        // 1.
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        // 2. ( blur! Compose blur content)
        .background(fillColor, shape)
        // 3.
        .border(
            width = 0.5.dp,
            color = borderColor,
            shape = shape
        )
}

/**
 * Modifier: macOS gradient
 * , card content " "
 */
@Composable
fun Modifier.glassBackground(): Modifier {
    val theme = LocalAppTheme.current
    val isDark = LocalIsDarkTheme.current

    val bgColors = if (isDark) {
        // gradient
        theme.gradients.background.ifEmpty {
            listOf(Color(0xFF0C0A14), Color(0xFF1A1030), Color(0xFF261840))
        }
    } else {
        // gradient( , ! )
        val primary = theme.lightColors.primary
        val tertiary = theme.lightColors.tertiary
        listOf(
            primary.copy(alpha = 0.06f).compositeOver(Color(0xFFF8F6FA)),
            tertiary.copy(alpha = 0.04f).compositeOver(Color(0xFFFAF8FC)),
            primary.copy(alpha = 0.03f).compositeOver(Color(0xFFF6F4F8))
        )
    }

    return this.background(
        brush = Brush.verticalGradient(bgColors)
    )
}

/**
 * Modifier: iOS
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMediumLow
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animSettings = LocalAnimationSettings.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        ),
        label = "pressScale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Modifier: + animation
 */
@Composable
fun Modifier.fadeSlideIn(
    delayMillis: Int = 0,
    durationMillis: Int = 450
): Modifier {
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { appeared = true }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "fadeIn"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slideUp"
    )

    return this.graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}

/**
 * Modifier: list
 */
@Composable
fun Modifier.staggeredFadeIn(
    index: Int,
    staggerDelay: Int = 50,
    baseDuration: Int = 400
): Modifier {
    return this.fadeSlideIn(
        delayMillis = index * staggerDelay,
        durationMillis = baseDuration
    )
}
