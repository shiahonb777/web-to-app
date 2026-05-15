package com.webtoapp.ui.design

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.design.WtaSwitch
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import com.webtoapp.core.i18n.Strings

@Composable
fun WtaSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    level: WtaCapabilityLevel = WtaCapabilityLevel.Common,
    headerStyle: WtaSectionHeaderStyle = WtaSectionHeaderStyle.Prominent,
    collapsible: Boolean = level != WtaCapabilityLevel.Common,
    initiallyExpanded: Boolean = level == WtaCapabilityLevel.Common,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    capabilityTags: List<String> = emptyList(),
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var internalExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val isExpanded = expanded ?: internalExpanded
    val canToggle = collapsible
    val setExpanded: (Boolean) -> Unit = { newValue ->
        if (expanded == null) {
            internalExpanded = newValue
        }
        onExpandedChange?.invoke(newValue)
    }
    val accent = when (level) {
        WtaCapabilityLevel.Common -> MaterialTheme.colorScheme.primary
        WtaCapabilityLevel.Advanced -> MaterialTheme.colorScheme.secondary
        WtaCapabilityLevel.Lab -> MaterialTheme.colorScheme.tertiary
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            when (headerStyle) {
                WtaSectionHeaderStyle.Prominent -> WtaSpacing.CardGap
                WtaSectionHeaderStyle.Quiet -> WtaSpacing.ContentGap
                WtaSectionHeaderStyle.Hidden -> WtaSpacing.CardGap
            }
        )
    ) {
        if (headerStyle != WtaSectionHeaderStyle.Hidden || canToggle || description != null) {
            WtaSectionHeader(
                title = title,
                description = description,
                level = level,
                accent = accent,
                headerStyle = headerStyle,
                canToggle = canToggle,
                isExpanded = isExpanded,
                onToggle = { setExpanded(!isExpanded) },
                capabilityTags = capabilityTags,
                trailing = trailing
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = WtaMotion.settleSpring()),
            exit = shrinkVertically(animationSpec = WtaMotion.snapSpring())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap),
                content = content
            )
        }
    }
}

@Composable
private fun WtaSectionHeader(
    title: String,
    description: String?,
    level: WtaCapabilityLevel,
    accent: Color,
    headerStyle: WtaSectionHeaderStyle,
    canToggle: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    capabilityTags: List<String>,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WtaRadius.Control))
            .then(
                if (canToggle) {
                    Modifier.clickable { onToggle() }
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = if (headerStyle == WtaSectionHeaderStyle.Quiet) 4.dp else 2.dp,
                vertical = if (headerStyle == WtaSectionHeaderStyle.Quiet) 0.dp else 2.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = if (headerStyle == WtaSectionHeaderStyle.Quiet) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = if (headerStyle == WtaSectionHeaderStyle.Quiet) {
                        FontWeight.Medium
                    } else {
                        FontWeight.SemiBold
                    },
                    color = if (headerStyle == WtaSectionHeaderStyle.Quiet) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (capabilityTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        capabilityTags.forEach { tag ->
                            WtaCapabilityPill(text = tag, color = accent)
                        }
                    }
                } else if (level != WtaCapabilityLevel.Common) {
                    Spacer(modifier = Modifier.width(8.dp))
                    WtaCapabilityPill(level = level, color = accent)
                }
            }
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        trailing()

        if (canToggle) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(WtaSize.TouchTarget)
            ) {
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) Strings.collapse else Strings.expand,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WtaCapabilityPill(
    level: WtaCapabilityLevel,
    color: Color
) {
    val text = when (level) {
        WtaCapabilityLevel.Common -> Strings.common
        WtaCapabilityLevel.Advanced -> Strings.advancedOptions
        WtaCapabilityLevel.Lab -> Strings.lab
    }
    WtaCapabilityPill(
        text = text,
        color = color,
        leadingIcon = level == WtaCapabilityLevel.Lab
    )
}

@Composable
private fun WtaCapabilityPill(
    text: String,
    color: Color,
    leadingIcon: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(WtaRadius.Button),
        color = color.copy(alpha = WtaAlpha.MutedContainer),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon) {
                Icon(
                    Icons.Outlined.Science,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun WtaSettingCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    WtaCard(
        modifier = modifier.fillMaxWidth(),
        tone = WtaCardTone.Surface,
        contentPadding = contentPadding,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WtaSettingCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    WtaCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        tone = WtaCardTone.Surface,
        enabled = enabled,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun WtaSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    tone: WtaRowTone = WtaRowTone.Normal,
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 3,
    trailingMaxWidth: Dp = WtaSize.RowTrailingMaxWidth,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = WtaSpacing.RowHorizontal,
        vertical = WtaSpacing.RowVertical
    ),
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val accentColor = when (tone) {
        WtaRowTone.Normal -> MaterialTheme.colorScheme.primary
        WtaRowTone.Danger -> MaterialTheme.colorScheme.error
    }
    val contentColor = if (enabled) {
        when (tone) {
            WtaRowTone.Normal -> MaterialTheme.colorScheme.onSurface
            WtaRowTone.Danger -> MaterialTheme.colorScheme.error
        }
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = WtaAlpha.Disabled)
    }
    val rowModifier = modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(WtaRadius.Control))
                    .clickable(enabled = enabled, onClick = onClick)
            } else {
                Modifier
            }
        )
        .heightIn(min = WtaSize.RowMinHeight)
        .padding(contentPadding)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null || iconContent != null) {
                Box(
                    modifier = Modifier
                        .size(WtaSize.IconPlate)
                        .clip(RoundedCornerShape(WtaRadius.IconPlate))
                        .background(accentColor.copy(alpha = WtaAlpha.MutedContainer)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconContent != null) {
                        iconContent()
                    } else if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(WtaSize.Icon),
                            tint = accentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(WtaSpacing.IconTextGap))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else WtaAlpha.Disabled
                        ),
                        maxLines = subtitleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(start = WtaSpacing.ContentGap)
                    .widthIn(max = trailingMaxWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                content = trailing
            )
        }
    }
}

@Composable
fun WtaToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    WtaSettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) }
    ) {
        WtaSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun WtaChoiceRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isExpanded: Boolean? = null,
    onClick: () -> Unit
) {
    WtaSettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        onClick = onClick
    ) {
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        // 当 isExpanded 被传入时，箭头带旋转动画（展开 180°）
        val rotation by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isExpanded == true) 180f else 0f,
            animationSpec = WtaMotion.settleSpring(),
            label = "expandArrow"
        )
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
fun WtaTextFieldRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WtaSpacing.RowHorizontal, vertical = WtaSpacing.RowVertical),
        verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        WtaTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = placeholder,
            singleLine = singleLine,
            shape = RoundedCornerShape(WtaRadius.Control)
        )
    }
}

@Composable
fun WtaSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueLabel: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WtaSpacing.RowHorizontal, vertical = WtaSpacing.RowVertical)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!valueLabel.isNullOrBlank()) {
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled
        )
    }
}

@Composable
fun WtaDangerRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Outlined.Warning,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    WtaSettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        tone = WtaRowTone.Danger,
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = Strings.tagSecurity,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun WtaActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WtaRadius.Card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = WtaAlpha.Divider))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun WtaEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Outlined.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    WtaSettingCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (!message.isNullOrBlank()) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (actionLabel != null && onAction != null) {
                WtaButton(
                    onClick = onAction,
                    text = actionLabel,
                    variant = WtaButtonVariant.Tonal,
                    size = WtaButtonSize.Small
                )
            }
        }
    }
}

@Composable
fun WtaStatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: WtaStatusTone = WtaStatusTone.Info,
    title: String? = null,
    titleMaxLines: Int = 2,
    messageMaxLines: Int = 4,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val container = when (tone) {
        WtaStatusTone.Info -> colors.secondaryContainer
        WtaStatusTone.Success -> colors.primaryContainer
        WtaStatusTone.Warning -> colors.tertiaryContainer
        WtaStatusTone.Error -> colors.errorContainer
    }
    val content = when (tone) {
        WtaStatusTone.Info -> colors.onSecondaryContainer
        WtaStatusTone.Success -> colors.onPrimaryContainer
        WtaStatusTone.Warning -> colors.onTertiaryContainer
        WtaStatusTone.Error -> colors.onErrorContainer
    }
    val icon = when (tone) {
        WtaStatusTone.Error -> Icons.Outlined.ErrorOutline
        WtaStatusTone.Warning -> Icons.Outlined.Warning
        else -> Icons.Outlined.Info
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WtaRadius.Card),
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, content.copy(alpha = WtaAlpha.PressedContainer))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(WtaSize.Icon))
            Spacer(modifier = Modifier.width(WtaSpacing.IconTextGap))
            Column(modifier = Modifier.weight(1f)) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = messageMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                WtaButton(
                    onClick = onAction,
                    text = actionLabel,
                    variant = WtaButtonVariant.Outlined,
                    size = WtaButtonSize.Small,
                    modifier = Modifier.widthIn(max = WtaSize.BannerActionMaxWidth)
                )
            }
        }
    }
}

@Composable
fun WtaBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WtaRadius.Button))
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WtaSectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = WtaSpacing.RowHorizontal),
        thickness = DividerDefaults.Thickness,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = WtaAlpha.Divider)
    )
}
