package com.webtoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.enhanced.LocalEnhancedBackgroundEnabled

/**
 * 自适应 ElevatedCard
 * 在强化模式下使用纯 Box + background，完全避免任何边框/阴影
 */
@Composable
fun EnhancedElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isEnhanced = LocalEnhancedBackgroundEnabled.current
    
    if (isEnhanced) {
        // 强化模式：使用纯 Box + background，完全无边框无阴影
        val bgColor = containerColor ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        Box(
            modifier = modifier
                .clip(shape)
                .background(bgColor)
        ) {
            Column(content = content)
        }
    } else {
        // 普通模式：使用 ElevatedCard
        ElevatedCard(
            modifier = modifier,
            shape = shape,
            colors = if (containerColor != null) {
                CardDefaults.elevatedCardColors(containerColor = containerColor)
            } else {
                colors
            },
            elevation = elevation,
            content = content
        )
    }
}

/**
 * 可点击的自适应 ElevatedCard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    containerColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val isEnhanced = LocalEnhancedBackgroundEnabled.current
    
    if (isEnhanced) {
        // 强化模式：使用纯 Box + background + clickable
        val bgColor = containerColor ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        Box(
            modifier = modifier
                .clip(shape)
                .background(bgColor)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    onClick = onClick
                )
        ) {
            Column(content = content)
        }
    } else {
        // 普通模式：使用 ElevatedCard
        ElevatedCard(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = if (containerColor != null) {
                CardDefaults.elevatedCardColors(containerColor = containerColor)
            } else {
                colors
            },
            elevation = elevation,
            interactionSource = interactionSource,
            content = content
        )
    }
}
