package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BedtimeOff
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.ScreenAwakeMode
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumSwitch

@Composable
fun KeepScreenOnCard(
    screenAwakeMode: ScreenAwakeMode,
    onScreenAwakeModeChange: (ScreenAwakeMode) -> Unit,
    screenAwakeTimeoutMinutes: Int,
    onScreenAwakeTimeoutChange: (Int) -> Unit,
    screenBrightness: Int,
    onScreenBrightnessChange: (Int) -> Unit
) {
    val isEnabled = screenAwakeMode != ScreenAwakeMode.OFF
    val primary = MaterialTheme.colorScheme.primary

    data class AwakeModeOption(
        val mode: ScreenAwakeMode,
        val icon: ImageVector,
        val title: String,
        val subtitle: String
    )

    val modeOptions = listOf(
        AwakeModeOption(ScreenAwakeMode.OFF, Icons.Outlined.BedtimeOff, Strings.screenAwakeOff, Strings.screenAwakeOffDesc),
        AwakeModeOption(ScreenAwakeMode.ALWAYS, Icons.Outlined.LightMode, Strings.screenAwakeAlways, Strings.screenAwakeAlwaysDesc),
        AwakeModeOption(ScreenAwakeMode.TIMED, Icons.Outlined.Timer, Strings.screenAwakeTimed, Strings.screenAwakeTimedDesc)
    )

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    SystemCardFeatureIcon(
                        icon = Icons.Outlined.Lightbulb,
                        enabled = isEnabled,
                        activeColor = primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.keepScreenOnLabel,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                PremiumSwitch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        onScreenAwakeModeChange(if (checked) ScreenAwakeMode.ALWAYS else ScreenAwakeMode.OFF)
                    }
                )
            }

            SystemCardExpandContent(visible = isEnabled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = Strings.screenAwakeModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        modeOptions.filter { it.mode != ScreenAwakeMode.OFF }.forEach { option ->
                            val isSelected = screenAwakeMode == option.mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onScreenAwakeModeChange(option.mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.5.dp, primary.copy(alpha = 0.3f)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = option.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = screenAwakeMode == ScreenAwakeMode.TIMED,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.screenAwakeTimeoutLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = Strings.screenAwakeTimeoutValue(screenAwakeTimeoutMinutes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = screenAwakeTimeoutMinutes.toFloat(),
                                onValueChange = { onScreenAwakeTimeoutChange(it.toInt()) },
                                valueRange = 5f..120f,
                                steps = 22,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10, 30, 60, 120).forEach { minutes ->
                                    val isPresetSelected = screenAwakeTimeoutMinutes == minutes
                                    FilterChip(
                                        selected = isPresetSelected,
                                        onClick = { onScreenAwakeTimeoutChange(minutes) },
                                        label = { Text(Strings.screenAwakeTimeoutValue(minutes), style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.BrightnessLow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.screenBrightnessLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Text(
                            text = if (screenBrightness < 0) Strings.screenBrightnessAuto else "${screenBrightness}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAuto = screenBrightness < 0
                        FilterChip(
                            selected = isAuto,
                            onClick = { onScreenBrightnessChange(-1) },
                            label = { Text(Strings.screenBrightnessAuto) },
                            leadingIcon = if (isAuto) {
                                { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isAuto,
                            onClick = { if (isAuto) onScreenBrightnessChange(80) },
                            label = { Text(Strings.screenBrightnessManual) },
                            leadingIcon = if (!isAuto) {
                                { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = screenBrightness >= 0,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BrightnessLow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Slider(
                                value = (if (screenBrightness < 0) 80 else screenBrightness).toFloat(),
                                onValueChange = { onScreenBrightnessChange(it.toInt()) },
                                valueRange = 5f..100f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Icon(
                                imageVector = Icons.Outlined.BrightnessHigh,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (screenAwakeMode == ScreenAwakeMode.ALWAYS) Color(0xFFFFF3E0)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (screenAwakeMode == ScreenAwakeMode.ALWAYS) Icons.Outlined.BatteryAlert else Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (screenAwakeMode == ScreenAwakeMode.ALWAYS) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (screenAwakeMode) {
                                    ScreenAwakeMode.ALWAYS -> Strings.screenAwakeBatteryWarning
                                    ScreenAwakeMode.TIMED -> Strings.screenAwakeTimedHint
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (screenAwakeMode == ScreenAwakeMode.ALWAYS) Color(0xFFE65100)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
