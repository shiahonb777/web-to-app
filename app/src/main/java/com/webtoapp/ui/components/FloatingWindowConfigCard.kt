package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.FloatingWindowConfig
import com.webtoapp.data.model.FloatingBorderStyle
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import kotlin.math.roundToInt

/**
 * floating windowconfigcard- advanced
 * area: , ,
 * preview, expand, select
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FloatingWindowConfigCard(
    config: FloatingWindowConfig,
    onConfigChange: (FloatingWindowConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvanced by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    
    // animation
    val arrowRotation by animateFloatAsState(
        targetValue = if (showAdvanced) 180f else 0f,
        animationSpec = tween(300),
        label = "arrowRotation"
    )

    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // header: icon + +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (config.enabled) primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PictureInPicture,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (config.enabled) primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            AppStringsProvider.current().floatingWindowTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                PremiumSwitch(
                    checked = config.enabled,
                    onCheckedChange = { onConfigChange(config.copy(enabled = it)) }
                )
            }

            // expandpanel( only display)
            AnimatedVisibility(
                visible = config.enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // ════════════════════════════════════════
                    // area 1
                    // ════════════════════════════════════════
                    SectionHeader(
                        icon = Icons.Outlined.Straighten,
                        title = AppStringsProvider.current().fwSectionSize
                    )

                    // Note
                    SliderWithLabel(
                        label = AppStringsProvider.current().fwWidthLabel,
                        value = config.widthPercent,
                        valueRange = 30f..100f,
                        steps = 13,
                        onValueChange = { newWidth ->
                            if (config.lockAspectRatio) {
                                onConfigChange(config.copy(
                                    widthPercent = newWidth,
                                    heightPercent = newWidth,
                                    windowSizePercent = newWidth
                                ))
                            } else {
                                onConfigChange(config.copy(
                                    widthPercent = newWidth,
                                    windowSizePercent = newWidth
                                ))
                            }
                        }
                    )

                    // ( only)
                    AnimatedVisibility(
                        visible = !config.lockAspectRatio,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        SliderWithLabel(
                            label = AppStringsProvider.current().fwHeightLabel,
                            value = config.heightPercent,
                            valueRange = 30f..100f,
                            steps = 13,
                            onValueChange = { newHeight ->
                                onConfigChange(config.copy(heightPercent = newHeight))
                            }
                        )
                    }

                    // Note
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AspectRatio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                AppStringsProvider.current().fwLockAspectRatio,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        PremiumSwitch(
                            checked = config.lockAspectRatio,
                            onCheckedChange = { locked ->
                                if (locked) {
                                    // sync
                                    onConfigChange(config.copy(
                                        lockAspectRatio = true,
                                        heightPercent = config.widthPercent
                                    ))
                                } else {
                                    onConfigChange(config.copy(lockAspectRatio = false))
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    // ════════════════════════════════════════
                    // area 2
                    // ════════════════════════════════════════
                    SectionHeader(
                        icon = Icons.Outlined.Palette,
                        title = AppStringsProvider.current().fwSectionAppearance
                    )

                    // Note
                    SliderWithLabel(
                        label = AppStringsProvider.current().floatingWindowOpacity,
                        value = config.opacity,
                        valueRange = 30f..100f,
                        steps = 6,
                        onValueChange = { onConfigChange(config.copy(opacity = it)) }
                    )

                    // Note
                    SliderWithLabel(
                        label = AppStringsProvider.current().fwCornerRadius,
                        value = config.cornerRadius,
                        valueRange = 0f..32f,
                        steps = 7,
                        suffix = "dp",
                        onValueChange = { onConfigChange(config.copy(cornerRadius = it)) }
                    )

                    // select
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = AppStringsProvider.current().fwBorderStyle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data class BorderOption(
                            val style: FloatingBorderStyle,
                            val icon: ImageVector,
                            val label: String
                        )
                        val borderOptions = listOf(
                            BorderOption(FloatingBorderStyle.NONE, Icons.Outlined.DoNotDisturb, AppStringsProvider.current().fwBorderNone),
                            BorderOption(FloatingBorderStyle.SUBTLE, Icons.Outlined.CropSquare, AppStringsProvider.current().fwBorderSubtle),
                            BorderOption(FloatingBorderStyle.GLOW, Icons.Outlined.AutoAwesome, AppStringsProvider.current().fwBorderGlow),
                            BorderOption(FloatingBorderStyle.ACCENT, Icons.Outlined.Palette, AppStringsProvider.current().fwBorderAccent)
                        )
                        borderOptions.forEach { option ->
                            val isSelected = config.borderStyle == option.style
                            FilterChip(
                                selected = isSelected,
                                onClick = { onConfigChange(config.copy(borderStyle = option.style)) },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        option.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    // ════════════════════════════════════════
                    // area 3
                    // ════════════════════════════════════════
                    SectionHeader(
                        icon = Icons.Outlined.Tune,
                        title = AppStringsProvider.current().fwSectionBehavior
                    )

                    // display
                    ToggleRow(
                        title = AppStringsProvider.current().floatingWindowShowTitleBar,
                        subtitle = AppStringsProvider.current().floatingWindowShowTitleBarDesc,
                        checked = config.showTitleBar,
                        onCheckedChange = { onConfigChange(config.copy(showTitleBar = it)) }
                    )

                    // hide
                    AnimatedVisibility(
                        visible = config.showTitleBar,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        ToggleRow(
                            title = AppStringsProvider.current().fwAutoHideTitleBar,
                            subtitle = AppStringsProvider.current().fwAutoHideTitleBarDesc,
                            checked = config.autoHideTitleBar,
                            onCheckedChange = { onConfigChange(config.copy(autoHideTitleBar = it)) },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Note
                    ToggleRow(
                        title = AppStringsProvider.current().fwEdgeSnapping,
                        subtitle = AppStringsProvider.current().fwEdgeSnappingDesc,
                        checked = config.edgeSnapping,
                        onCheckedChange = { onConfigChange(config.copy(edgeSnapping = it)) }
                    )

                    // Note
                    ToggleRow(
                        title = AppStringsProvider.current().fwResizeHandle,
                        subtitle = AppStringsProvider.current().fwResizeHandleDesc,
                        checked = config.showResizeHandle,
                        onCheckedChange = { onConfigChange(config.copy(showResizeHandle = it)) }
                    )

                    // expand
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showAdvanced = !showAdvanced },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showAdvanced) AppStringsProvider.current().hideAdvanced else AppStringsProvider.current().showAdvanced,
                                style = MaterialTheme.typography.labelMedium,
                                color = primary
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation)
                            )
                        }
                    }

                    // advancedsettings
                    AnimatedVisibility(
                        visible = showAdvanced,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Note
                            ToggleRow(
                                title = AppStringsProvider.current().floatingWindowStartMinimized,
                                subtitle = AppStringsProvider.current().floatingWindowStartMinimizedDesc,
                                checked = config.startMinimized,
                                onCheckedChange = { onConfigChange(config.copy(startMinimized = it)) }
                            )

                            // Note
                            ToggleRow(
                                title = AppStringsProvider.current().floatingWindowRememberPosition,
                                subtitle = AppStringsProvider.current().floatingWindowRememberPositionDesc,
                                checked = config.rememberPosition,
                                onCheckedChange = { onConfigChange(config.copy(rememberPosition = it)) }
                            )

                            // Note
                            ToggleRow(
                                title = AppStringsProvider.current().fwLockPosition,
                                subtitle = AppStringsProvider.current().fwLockPositionDesc,
                                checked = config.lockPosition,
                                onCheckedChange = { onConfigChange(config.copy(lockPosition = it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// Note
// ══════════════════════════════════════════

/**
 * label display
 */
@Composable
private fun SliderWithLabel(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Int) -> Unit,
    suffix: String = "%"
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = primary.copy(alpha = 0.1f)
            ) {
                Text(
                    "${value}${suffix}",
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Note
 */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(1.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PremiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * area( icon +, )
 */
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = primary
        )
    }
}
