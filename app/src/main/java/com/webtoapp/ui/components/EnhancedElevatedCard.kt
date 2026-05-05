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


    val glassFill = containerColor ?: if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.White.copy(alpha = 0.85f)


    val borderColor = if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.Black.copy(alpha = 0.05f)



    val shadowColor = if (isDark)
        Color.Black.copy(alpha = 0.35f)
    else
        Color.Black.copy(alpha = 0.08f)

    Surface(
        modifier = modifier

            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))

            .background(glassFill, RoundedCornerShape(cornerRadius))

            .border(0.5.dp, borderColor, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column { content() }
    }
}





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


    val scale by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardPressScale"
    )


    val glassFill = containerColor ?: if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.White.copy(alpha = 0.85f)


    val borderColor = if (isDark)
        Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)
    else
        Color.Black.copy(alpha = if (isPressed) 0.08f else 0.05f)



    val shadowColor = if (isDark)
        Color.Black.copy(alpha = 0.35f)
    else
        Color.Black.copy(alpha = 0.08f)

    Surface(
        onClick = onClick,
        modifier = modifier

            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }

            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))

            .background(glassFill, RoundedCornerShape(cornerRadius))

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


    val scale by animateFloatAsState(
        targetValue = if (isPressed && animSettings.enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardPressScale"
    )


    val backgroundColor = containerColor ?: if (isDark)
        Color.White.copy(alpha = 0.05f)
    else
        Color.White.copy(alpha = 0.95f)


    val borderColor = if (isDark)
        Color.White.copy(alpha = if (isPressed) 0.2f else 0.12f)
    else
        Color.Black.copy(alpha = if (isPressed) 0.12f else 0.08f)

    Surface(
        onClick = onClick,
        modifier = modifier

            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cornerRadius))

            .background(backgroundColor, RoundedCornerShape(cornerRadius))

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
