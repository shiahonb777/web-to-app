package com.webtoapp.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The single button primitive used across the app. Painted by hand rather
 * than delegating to Material's Button family so we can own the feedback
 * animation end-to-end: no ripple, filled container colour, spring-based
 * press scale, subtle alpha dip on press, and consistent haptic.
 */
@Composable
fun WtaButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    variant: WtaButtonVariant = WtaButtonVariant.Primary,
    size: WtaButtonSize = WtaButtonSize.Medium,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues? = null
) {
    WtaButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        contentPadding = contentPadding
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(WtaSize.IconSmall))
            Spacer(modifier = Modifier.width(WtaSpacing.Small))
        }
        Text(text = text)
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(WtaSpacing.Small))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(WtaSize.IconSmall))
        }
    }
}

@Composable
fun WtaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: WtaButtonVariant = WtaButtonVariant.Primary,
    size: WtaButtonSize = WtaButtonSize.Medium,
    enabled: Boolean = true,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticClick = rememberHapticClick(onClick)
    val isPressed by interactionSource.collectIsPressedAsState()
    val spec = variant.colors(enabled = enabled, pressed = isPressed)
    val heightMin = when (size) {
        WtaButtonSize.Small -> WtaSize.ButtonHeightSmall
        WtaButtonSize.Medium -> WtaSize.ButtonHeightMedium
        WtaButtonSize.Large -> WtaSize.ButtonHeightLarge
    }
    val padding = contentPadding ?: when (size) {
        WtaButtonSize.Small -> PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        WtaButtonSize.Medium -> PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        WtaButtonSize.Large -> PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    }
    val shape: Shape = RoundedCornerShape(WtaRadius.Button)

    val animatedContainer by animateColorAsState(
        targetValue = spec.container,
        animationSpec = WtaMotion.standardTween(durationMillis = WtaMotion.DurationQuick),
        label = "wtaButtonContainer"
    )

    val borderModifier = if (spec.border != null) Modifier.border(spec.border, shape) else Modifier

    Box(
        modifier = modifier
            .heightIn(min = heightMin)
            .clip(shape)
            .background(animatedContainer, shape)
            .then(borderModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = hapticClick
            )
            .wtaPressScale(interactionSource, pressedScale = 0.96f)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides spec.content) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

/**
 * Icon-only button with haptic feedback and consistent sizing. Stays on
 * Material IconButton because there's little value in reimplementing its
 * touch target sizing.
 */
@Composable
fun WtaIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tonal: Boolean = false,
    colors: IconButtonColors = if (tonal) IconButtonDefaults.filledTonalIconButtonColors()
    else IconButtonDefaults.iconButtonColors()
) {
    val hapticClick = rememberHapticClick(onClick)
    IconButton(
        onClick = hapticClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(WtaSize.Icon))
    }
}

private data class WtaButtonColorSpec(
    val container: Color,
    val content: Color,
    val border: BorderStroke? = null
)

@Composable
private fun WtaButtonVariant.colors(enabled: Boolean, pressed: Boolean): WtaButtonColorSpec {
    val cs = MaterialTheme.colorScheme
    val disabledContent = cs.onSurface.copy(alpha = WtaAlpha.Disabled)
    val disabledContainer = cs.onSurface.copy(alpha = 0.12f)

    return when (this) {
        WtaButtonVariant.Primary -> WtaButtonColorSpec(
            container = if (!enabled) disabledContainer
            else if (pressed) cs.primary.copy(alpha = 0.88f)
            else cs.primary,
            content = if (enabled) cs.onPrimary else disabledContent
        )
        WtaButtonVariant.Tonal -> WtaButtonColorSpec(
            container = if (!enabled) disabledContainer
            else if (pressed) cs.secondaryContainer.copy(alpha = 0.7f)
            else cs.secondaryContainer,
            content = if (enabled) cs.onSecondaryContainer else disabledContent
        )
        WtaButtonVariant.Outlined -> WtaButtonColorSpec(
            container = if (pressed && enabled) cs.primary.copy(alpha = 0.08f) else Color.Transparent,
            content = if (enabled) cs.primary else disabledContent,
            border = BorderStroke(
                1.dp,
                if (enabled) cs.outline.copy(alpha = 0.6f) else cs.outline.copy(alpha = 0.2f)
            )
        )
        WtaButtonVariant.Text -> WtaButtonColorSpec(
            container = if (pressed && enabled) cs.primary.copy(alpha = 0.08f) else Color.Transparent,
            content = if (enabled) cs.primary else disabledContent
        )
        WtaButtonVariant.Destructive -> WtaButtonColorSpec(
            container = if (!enabled) disabledContainer
            else if (pressed) cs.error.copy(alpha = 0.88f)
            else cs.error,
            content = if (enabled) cs.onError else disabledContent
        )
    }
}
