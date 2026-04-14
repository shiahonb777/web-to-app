package com.webtoapp.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * Collapsible settings card with spring expansion.
 * 
 * Collapsed by default: header, icon, and optional trailing summary.
 * Tap to expand content with a spring animation.
 *
 * @param title Card title.
 * @param icon Header icon.
 * @param startExpanded Whether the card starts expanded.
 * @param trailing Right-side header content (for example Switch).
 * @param content Collapsible content area.
 */
@Composable
fun CollapsibleConfigCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    startExpanded: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(startExpanded) }

    // Arrow rotation animation
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "arrowRotation"
    )

    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Header row (always visible, toggles expansion)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Trailing content (Switch, etc.)
                if (trailing != null) {
                    trailing()
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Collapse arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = arrowRotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Collapsible content area
            AnimatedVisibility(
                visible = expanded,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}
