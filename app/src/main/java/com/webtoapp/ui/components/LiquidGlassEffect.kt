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

// ==================== macOS / iOS 玻璃质感效果系统 ====================

/**
 * Modifier 扩展：macOS 风格毛玻璃效果
 *
 * 核心视觉特征（参照 macOS Sonoma / iOS 18 控制中心）:
 *   1. 半透明深色/浅色填充 — 不是透明，也不是纯白/纯黑
 *   2. 极细白色边框 (0.5dp) — 在暗色下尤其明显
 *   3. 顶部内侧高光渐变 — 模拟光线从上方照射到玻璃曲面
 *   4. 柔和扩散阴影 — 不是 Material 那种硬边 elevation
 *   5. 高斯模糊背景 (Android 12+)
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

    // macOS 在暗色下用极淡的白色填充，亮色下用极淡的白色 + 更高透明度
    val fillColor = if (isDark)
        Color.White.copy(alpha = tintAlpha * 0.8f)
    else
        Color.White.copy(alpha = 0.78f)       // 亮色：高不透明度磨砂玻璃


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
        // 1. 柔和扩散阴影
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        // 2. 半透明填充（不用 blur！Compose 的 blur 会模糊内容本身）
        .background(fillColor, shape)
        // 3. 极细边框
        .border(
            width = 0.5.dp,
            color = borderColor,
            shape = shape
        )
}

/**
 * Modifier 扩展：macOS 风格的渐变背景底层
 * 为整个页面提供一个有层次感的背景，让上层的玻璃卡片有内容可以"透过"
 */
@Composable
fun Modifier.glassBackground(): Modifier {
    val theme = LocalAppTheme.current
    val isDark = LocalIsDarkTheme.current

    val bgColors = if (isDark) {
        // 暗色：使用主题的深色背景渐变
        theme.gradients.background.ifEmpty {
            listOf(Color(0xFF0C0A14), Color(0xFF1A1030), Color(0xFF261840))
        }
    } else {
        // 亮色：柔和的主题色渐变（不是纯白，而是有色调！）
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
 * Modifier 扩展：iOS 风格按压缩放动效
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
 * Modifier 扩展：出场淡入 + 上滑动画
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
 * Modifier 扩展：列表项交错出场
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
