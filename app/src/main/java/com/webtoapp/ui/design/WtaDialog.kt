package com.webtoapp.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Unified dialog for the Wta design system.
 *
 * Why this exists:
 *  - Consistent shape ([WtaRadius.Dialog]) across every dialog in the app.
 *  - Physics-driven entrance: scale-only bounce spring (no fade).
 *  - Icon sits in a tinted rounded plate matching the rest of the design
 *    language, rather than floating unstyled above the title.
 *  - Sensible max width so dialogs never stretch to edge on tablets.
 */
@Composable
fun WtaAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    title: String? = null,
    text: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.9f,
        animationSpec = WtaMotion.bouncySpring(),
        label = "wtaDialogScale"
    )

    val resolvedIconTint = iconTint ?: MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier
            .widthIn(max = 400.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        dismissButton = dismissButton,
        icon = icon?.let { iconVector ->
            {
                Box(
                    modifier = Modifier
                        .size(WtaSize.IconPlateLarge)
                        .clip(RoundedCornerShape(WtaRadius.IconPlate))
                        .background(resolvedIconTint.copy(alpha = WtaAlpha.MutedContainer)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = resolvedIconTint,
                        modifier = Modifier.size(WtaSize.IconLarge)
                    )
                }
            }
        },
        title = title?.let { titleText ->
            {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = when {
            content != null -> {
                {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap),
                        content = content
                    )
                }
            }
            text != null -> {
                {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> null
        },
        shape = RoundedCornerShape(WtaRadius.Dialog),
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = resolvedIconTint,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        properties = properties,
    )
}
