package com.webtoapp.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.* // ═══════════════════════════════════════════════════════════
// Liquid Drop Physics Tab Bar
// iOS fluid indicator + metaball morphing
// ═══════════════════════════════════════════════════════════

/**
 * Note
 */
private object LiquidPhysics {
    // Note
    val PositionSpring = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessMediumLow
    )
    // Note
    val StretchSpring = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = Spring.StiffnessMedium
    )
    // icon
    val IconBounce = spring<Float>(
        dampingRatio = 0.45f,
        stiffness = Spring.StiffnessHigh
    )
    // Note
    val LabelFade = tween<Float>(220, easing = FastOutSlowInEasing)
}

data class LiquidTabItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

/**
 * Tab Bar
 *
 * Note
 * 1. indicator- from Tab,
 * 2. - ( overshoot)
 * 3. icon + scale up
 * 4.
 */
@Composable
fun LiquidTabBar(
    tabs: List<LiquidTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 64.dp,
) {

    // animation

    // indicator X( 0. . tabCount- 1)
    val targetPosition = selectedIndex.toFloat()
    val animatedPosition by animateFloatAsState(
        targetValue = targetPosition,
        animationSpec = LiquidPhysics.PositionSpring,
        label = "liquidPos"
    )

    // Note
    val velocity = abs(animatedPosition - targetPosition)
    val effectiveStretch = (velocity * 2.2f).coerceIn(0f, 1f)

    // Note
    val primaryColor = MaterialTheme.colorScheme.primary
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        .navigationBarsPadding()
        .height(barHeight)
    ) {

        // Note
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                Modifier
                    .matchParentSize()
                    .blur(24.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    .background(surfaceColor.copy(alpha = 0.78f))
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .background(surfaceColor.copy(alpha = 0.96f))
            )
        }

        // top
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .align(Alignment.TopCenter)
                .background(outlineVariant.copy(alpha = 0.2f))
        )

        // indicator Canvas
        Canvas(
            Modifier.matchParentSize()
        ) {
            val tabWidth = size.width / tabs.size
            val centerX = tabWidth * (animatedPosition + 0.5f)
            val centerY = size.height * 0.38f  // indicator,

            // indicator- ,
            val pillWidth = tabWidth * 0.52f * (1f + effectiveStretch * 0.4f)
            val pillHeight = tabWidth * 0.52f * (1f - effectiveStretch * 0.1f)

            // indicator- gradient
            val indicatorRect = Rect(
                centerX - pillWidth / 2f,
                centerY - pillHeight / 2f,
                centerX + pillWidth / 2f,
                centerY + pillHeight / 2f
            )

            val indicatorPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        indicatorRect,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillHeight / 2f)
                    )
                )
            }

            drawPath(
                path = indicatorPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        indicatorColor.copy(alpha = 0.6f),
                        indicatorColor.copy(alpha = 0.3f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = pillWidth / 1.5f
                )
            )
        }

        // Tab icon +
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex

                // icon
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 0.92f,
                    animationSpec = LiquidPhysics.IconBounce,
                    label = "iconScale$index"
                )
                // icon
                val iconOffsetY by animateFloatAsState(
                    targetValue = if (isSelected) -3f else 2f,
                    animationSpec = LiquidPhysics.IconBounce,
                    label = "iconY$index"
                )
                // Note
                val labelAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.55f,
                    animationSpec = LiquidPhysics.LabelFade,
                    label = "labelAlpha$index"
                )
                // iconcolor
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) primaryColor else onSurfaceVariant.copy(alpha = 0.6f),
                    animationSpec = tween(250),
                    label = "iconColor$index"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) onTabSelected(index)
                        }
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = iconColor,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                translationY = iconOffsetY
                            }
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        color = if (isSelected) primaryColor.copy(alpha = labelAlpha)
                               else onSurfaceVariant.copy(alpha = labelAlpha),
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            this.alpha = labelAlpha
                        }
                    )
                }
            }
        }
    }
}
