package com.webtoapp.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.*

/**
 * 主题化组件
 * 根据当前主题应用不同的视觉效果
 */

// ==================== 主题化按钮 ====================

/**
 * 渐变按钮 - 根据主题动画风格应用不同效果
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val animSettings = LocalAnimationSettings.current
    val view = LocalView.current
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 根据主题动画风格计算缩放目标值
    val targetScale = if (isPressed && animSettings.enabled) {
        when (theme.animationStyle) {
            AnimationStyle.BOUNCY -> 0.88f
            AnimationStyle.SNAPPY -> 0.94f
            AnimationStyle.PLAYFUL -> 0.85f
            AnimationStyle.DRAMATIC -> 0.90f
            else -> 0.96f
        }
    } else 1f
    
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = getSpringSpec(theme.animationStyle, animSettings.speedMultiplier),
        label = "buttonScale"
    )
    
    // 旋转效果 - 仅用于 PLAYFUL 风格
    val rotation by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled && theme.animationStyle == AnimationStyle.PLAYFUL) -3f else 0f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium),
        label = "buttonRotation"
    )
    
    // 发光动画
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled && theme.effects.enableGlow) 0.5f else 0.3f,
        animationSpec = tween(150),
        label = "glowAlpha"
    )
    
    Surface(
        onClick = {
            if (animSettings.hapticsEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            onClick()
        },
        modifier = modifier
            .scale(scale)
            .graphicsLayer { rotationZ = rotation }
            .then(
                if (animSettings.enabled && theme.effects.enableGlow) {
                    Modifier.drawBehind {
                        drawCircle(
                            color = theme.effects.glowColor.copy(alpha = glowAlpha),
                            radius = size.maxDimension / 2 + theme.effects.glowRadius.toPx() * 0.5f,
                            center = center
                        )
                    }
                } else Modifier
            ),
        enabled = enabled,
        shape = RoundedCornerShape(theme.shapes.buttonRadius),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(theme.gradients.primary),
                    shape = RoundedCornerShape(theme.shapes.buttonRadius)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * 发光按钮
 */
@Composable
fun GlowingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val animSettings = LocalAnimationSettings.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier.then(
            if (animSettings.enabled && theme.effects.enableGlow) {
                Modifier.drawBehind {
                    drawCircle(
                        color = glowColor.copy(alpha = glowAlpha),
                        radius = size.maxDimension / 2 + 12.dp.toPx()
                    )
                }
            } else Modifier
        ),
        enabled = enabled,
        shape = RoundedCornerShape(theme.shapes.buttonRadius)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// ==================== 主题化卡片 ====================

/**
 * 玻璃拟态卡片
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.1f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    blurRadius: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val animSettings = LocalAnimationSettings.current
    
    val useGlassmorphism = theme.effects.enableGlassmorphism && animSettings.enabled
    
    Surface(
        modifier = modifier
            .then(
                if (useGlassmorphism && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blurRadius)
                } else Modifier
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(theme.shapes.cardRadius)
            ),
        shape = RoundedCornerShape(theme.shapes.cardRadius),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * 渐变边框卡片
 */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>? = null,
    borderWidth: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val colors = gradientColors ?: theme.gradients.accent
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(colors),
                shape = RoundedCornerShape(theme.shapes.cardRadius)
            )
            .padding(borderWidth)
    ) {
        Surface(
            shape = RoundedCornerShape(theme.shapes.cardRadius - borderWidth),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

/**
 * 悬浮卡片（带阴影动画）- 根据主题动画风格应用不同效果
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val animSettings = LocalAnimationSettings.current
    val view = LocalView.current
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 根据主题风格计算阴影变化
    val (pressedElevation, normalElevation) = when (theme.animationStyle) {
        AnimationStyle.DRAMATIC -> 1f to 16f
        AnimationStyle.ELEGANT -> 4f to 12f
        AnimationStyle.SNAPPY -> 0f to 4f  // SNAPPY 风格使用较小阴影
        else -> 2f to 8f
    }
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) pressedElevation else normalElevation,
        animationSpec = getSpringSpec(theme.animationStyle, animSettings.speedMultiplier),
        label = "elevation"
    )
    
    // 根据主题风格计算缩放
    val targetScale = if (isPressed && animSettings.enabled) {
        when (theme.animationStyle) {
            AnimationStyle.BOUNCY -> 0.94f
            AnimationStyle.SNAPPY -> 0.97f
            AnimationStyle.PLAYFUL -> 0.92f
            AnimationStyle.DRAMATIC -> 0.95f
            else -> 0.98f
        }
    } else 1f
    
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = getSpringSpec(theme.animationStyle, animSettings.speedMultiplier),
        label = "scale"
    )
    
    // 旋转效果 - 仅用于 PLAYFUL 风格
    val rotation by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled && theme.animationStyle == AnimationStyle.PLAYFUL) -2f else 0f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "rotation"
    )
    
    Card(
        onClick = {
            if (animSettings.hapticsEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            onClick()
        },
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                shadowElevation = elevation.dp.toPx()
                rotationZ = rotation
            }
            .then(
                if (animSettings.enabled && theme.effects.enableGlow && isPressed) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = theme.effects.glowColor.copy(alpha = 0.2f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                theme.shapes.cardRadius.toPx() + 4.dp.toPx()
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                size.width + 8.dp.toPx(),
                                size.height + 8.dp.toPx()
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(-4.dp.toPx(), -4.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(theme.shapes.cardRadius),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ==================== 主题化背景 ====================

/**
 * 渐变背景
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val backgroundColors = colors ?: theme.gradients.background
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(backgroundColors)),
        content = content
    )
}

/**
 * 动态渐变背景
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val animSettings = LocalAnimationSettings.current
    val backgroundColors = colors ?: theme.gradients.background
    
    val infiniteTransition = rememberInfiniteTransition(label = "gradientBg")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (animSettings.enabled) {
                    val start = Offset(size.width * offset, 0f)
                    val end = Offset(size.width * (1 - offset), size.height)
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = backgroundColors,
                            start = start,
                            end = end
                        )
                    )
                } else {
                    drawRect(brush = Brush.linearGradient(backgroundColors))
                }
            },
        content = content
    )
}

// ==================== 主题化指示器 ====================

/**
 * 主题化加载指示器
 */
@Composable
fun ThemedLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val theme = LocalAppTheme.current
    val animSettings = LocalAnimationSettings.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000 * animSettings.speedMultiplier).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 发光效果
        if (theme.effects.enableGlow && animSettings.enabled) {
            Box(
                modifier = Modifier
                    .size(size + 16.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    theme.effects.glowColor.copy(alpha = 0f),
                                    theme.effects.glowColor.copy(alpha = 0.5f),
                                    theme.effects.glowColor.copy(alpha = 0f)
                                )
                            ),
                            radius = this.size.minDimension / 2
                        )
                    }
            )
        }
        
        CircularProgressIndicator(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation * 0.3f },
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
    }
}

/**
 * 脉冲点指示器
 */
@Composable
fun PulsingDotIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotCount: Int = 3,
    dotSize: Dp = 8.dp
) {
    val animSettings = LocalAnimationSettings.current
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSize / 2)
    ) {
        repeat(dotCount) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (600 * animSettings.speedMultiplier).toInt(),
                        delayMillis = index * 150,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotScale$index"
            )
            
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(if (animSettings.enabled) scale else 1f)
                    .clip(CircleShape)
                    .background(color.copy(alpha = scale))
            )
        }
    }
}

// ==================== 主题化分隔线 ====================

/**
 * 渐变分隔线
 */
@Composable
fun GradientDivider(
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    thickness: Dp = 1.dp
) {
    val theme = LocalAppTheme.current
    val dividerColors = colors ?: listOf(
        Color.Transparent,
        theme.gradients.accent.first(),
        theme.gradients.accent.last(),
        Color.Transparent
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(Brush.horizontalGradient(dividerColors))
    )
}

// ==================== 主题化徽章 ====================

/**
 * 发光徽章
 */
@Composable
fun GlowingBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val theme = LocalAppTheme.current
    val animSettings = LocalAnimationSettings.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeGlow"
    )
    
    Surface(
        modifier = modifier.then(
            if (animSettings.enabled && theme.effects.enableGlow) {
                Modifier.drawBehind {
                    drawCircle(
                        color = backgroundColor.copy(alpha = glowAlpha),
                        radius = size.maxDimension / 2 + 4.dp.toPx()
                    )
                }
            } else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
