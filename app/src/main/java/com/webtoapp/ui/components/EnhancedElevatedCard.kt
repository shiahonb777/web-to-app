package com.webtoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.LocalAnimationSettings
import com.webtoapp.ui.theme.LocalAppTheme
import com.webtoapp.ui.theme.LocalIsDarkTheme

/**
 * macOS / iOS 风格自适应卡片（静态版本）
 *
 * 玻璃质感通过以下方式实现（不模糊内容！）：
 * - 半透明填充（亮色 78% 白 / 暗色 12% 白）
 * - 极细白色边框 0.5dp
 * - 顶部内侧高光渐变（模拟玻璃曲面光泽）
 * - 柔和扩散阴影
 */
@Composable
fun EnhancedElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalAppTheme.current.shapes.cardRadius),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val isDark = LocalIsDarkTheme.current
    val cornerRadius = theme.shapes.cardRadius

    // macOS 玻璃填充色 — 半透明，让底层背景隐约透过
    val glassFill = containerColor ?: if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.White.copy(alpha = 0.85f)

    // 极细边框 — macOS 玻璃面板的标志性特征
    val borderColor = if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.Black.copy(alpha = 0.05f)


    // 柔和阴影色
    val shadowColor = if (isDark)
        Color.Black.copy(alpha = 0.35f)
    else
        Color.Black.copy(alpha = 0.08f)

    Surface(
        modifier = modifier
            // 1. 柔和扩散阴影（不是 Material 硬边 elevation）
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))
            // 2. 半透明填充 — 核心！
            .background(glassFill, RoundedCornerShape(cornerRadius))
            // 3. 极细边框
            .border(0.5.dp, borderColor, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,       // 关键：Surface 本身不上色
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column { content() }
    }
}

/**
 * macOS / iOS 风格自适应卡片（可点击版本）
 * 带 spring 回弹按压动效
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(LocalAppTheme.current.shapes.cardRadius),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    containerColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val isDark = LocalIsDarkTheme.current
    val animSettings = LocalAnimationSettings.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius = theme.shapes.cardRadius

    // spring 回弹缩放
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardPressScale"
    )

    // 玻璃填充色
    val glassFill = containerColor ?: if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.White.copy(alpha = 0.85f)

    // 边框 — 按压时略微加亮
    val borderColor = if (isDark)
        Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)
    else
        Color.Black.copy(alpha = if (isPressed) 0.08f else 0.05f)


    // 阴影
    val shadowColor = if (isDark)
        Color.Black.copy(alpha = 0.35f)
    else
        Color.Black.copy(alpha = 0.08f)

    Surface(
        onClick = onClick,
        modifier = modifier
            // spring 回弹
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // 柔和阴影（按压时减弱 = 贴近桌面的感觉）
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))
            // 半透明填充
            .background(glassFill, RoundedCornerShape(cornerRadius))
            // 极细边框
            .border(0.5.dp, borderColor, RoundedCornerShape(cornerRadius)),
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        interactionSource = interactionSource
    ) {
        Column { content() }
    }
}

/**
 * macOS / iOS 风格自适应卡片（Outlined 可点击版本）
 * 带 spring 回弹按压动效
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedOutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(LocalAppTheme.current.shapes.cardRadius),
    containerColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val isDark = LocalIsDarkTheme.current
    val animSettings = LocalAnimationSettings.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius = theme.shapes.cardRadius

    // spring 回弹缩放
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardPressScale"
    )

    // 背景填充色
    val backgroundColor = containerColor ?: if (isDark)
        Color.White.copy(alpha = 0.05f)
    else
        Color.White.copy(alpha = 0.95f)

    // 边框颜色
    val borderColor = if (isDark)
        Color.White.copy(alpha = if (isPressed) 0.2f else 0.12f)
    else
        Color.Black.copy(alpha = if (isPressed) 0.12f else 0.08f)

    Surface(
        onClick = onClick,
        modifier = modifier
            // spring 回弹
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cornerRadius))
            // 背景填充
            .background(backgroundColor, RoundedCornerShape(cornerRadius))
            // 边框
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        interactionSource = interactionSource
    ) {
        Column { content() }
    }
}
