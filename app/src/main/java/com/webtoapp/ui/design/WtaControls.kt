package com.webtoapp.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Text input with a filled, edgeless look that replaces the default M3
 * outlined field. Instead of a loud rectangular outline, we use a subtle
 * filled container plus a bottom accent line that animates in when focused.
 * This keeps focus state crystal clear without adding visual noise.
 *
 * The ghost border only appears in error state (where the user really needs
 * to notice).
 */
@Composable
fun WtaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = LocalTextStyle.current,
    shape: Shape = RoundedCornerShape(
        topStart = WtaRadius.Control,
        topEnd = WtaRadius.Control,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = MaterialTheme.colorScheme
    val isFocused by interactionSource.collectIsFocusedAsState()

    val indicatorColor by animateColorAsState(
        targetValue = when {
            isError -> colors.error
            isFocused -> colors.primary
            else -> Color.Transparent
        },
        animationSpec = WtaMotion.standardTween(),
        label = "wtaTextFieldIndicator"
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(WtaSize.Icon)) }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh.copy(alpha = 0.9f),
            unfocusedContainerColor = colors.surfaceContainerHigh.copy(alpha = 0.6f),
            disabledContainerColor = colors.surfaceContainer.copy(alpha = 0.5f),
            errorContainerColor = colors.errorContainer.copy(alpha = 0.5f),
            focusedIndicatorColor = indicatorColor,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = indicatorColor,
            cursorColor = colors.primary,
            focusedLabelColor = colors.onSurface,
            unfocusedLabelColor = colors.onSurfaceVariant,
            errorLabelColor = colors.error,
            focusedLeadingIconColor = colors.onSurface,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
            focusedTrailingIconColor = colors.onSurface,
            unfocusedTrailingIconColor = colors.onSurfaceVariant
        )
    )
}

/**
 * Toggle primitive that replaces the M3 Switch with iOS-style physics.
 *
 * The key to the iOS switch feel:
 *  - On press: thumb stretches horizontally ~15% (squish effect)
 *  - On toggle: thumb slides with a bouncy spring that overshoots slightly
 *  - On release: thumb snaps back to circle with elastic spring
 *
 * We use M3 Switch as the base but layer spring-driven scale animations
 * on top to achieve the physical feel without reimplementing the entire
 * accessibility and state management layer.
 */
@Composable
fun WtaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme
    val isPressed by interactionSource.collectIsPressedAsState()

    val onChange: (Boolean) -> Unit = { next ->
        if (enabled) performHaptic(view)
        onCheckedChange(next)
    }

    // Thumb stretches wider on press (iOS squish effect)
    val stretchX by animateFloatAsState(
        targetValue = if (isPressed) 1.12f else 1f,
        animationSpec = if (isPressed) WtaMotion.pressSpring() else WtaMotion.bouncySpring(),
        label = "wtaSwitchStretchX"
    )
    val stretchY by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = if (isPressed) WtaMotion.pressSpring() else WtaMotion.bouncySpring(),
        label = "wtaSwitchStretchY"
    )

    Switch(
        checked = checked,
        onCheckedChange = onChange,
        modifier = modifier.graphicsLayer {
            scaleX = stretchX
            scaleY = stretchY
        },
        enabled = enabled,
        interactionSource = interactionSource,
        thumbContent = thumbContent,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = colors.surface,
            uncheckedTrackColor = colors.surfaceContainerHighest,
            uncheckedBorderColor = colors.outlineVariant.copy(alpha = 0.6f),
            disabledCheckedThumbColor = colors.onPrimary.copy(alpha = 0.7f),
            disabledCheckedTrackColor = colors.primary.copy(alpha = 0.4f),
            disabledUncheckedThumbColor = colors.surface.copy(alpha = 0.7f),
            disabledUncheckedTrackColor = colors.surfaceContainerHighest.copy(alpha = 0.4f)
        )
    )
}

/**
 * Selectable chip with a soft filled container on selection rather than an
 * outlined border. Looks closer to iOS segmented controls and plays nicer
 * with a monochrome palette than tinted-outline chips.
 */
@Composable
fun WtaChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    showSelectedCheck: Boolean = true
) {
    WtaChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        showSelectedCheck = showSelectedCheck
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun WtaChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    showSelectedCheck: Boolean = true,
    label: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val hapticClick = rememberHapticClick(onClick)

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.95f)
        else colors.surfaceContainerHigh.copy(alpha = 0.7f),
        animationSpec = WtaMotion.standardTween(),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = WtaMotion.standardTween(),
        label = "chipContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.97f,
        animationSpec = WtaMotion.bouncySpring(),
        label = "chipScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .wtaPressScale(interactionSource, pressedScale = 0.94f)
            .clip(RoundedCornerShape(WtaRadius.Chip))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = hapticClick
            ),
        shape = RoundedCornerShape(WtaRadius.Chip),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected && showSelectedCheck) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Spacer(Modifier.width(6.dp))
            } else if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Spacer(Modifier.width(6.dp))
            }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                label()
            }
        }
    }
}
