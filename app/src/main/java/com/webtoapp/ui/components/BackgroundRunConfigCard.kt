package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.design.WtaSwitch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.webtoapp.core.background.BackgroundRunService
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.BackgroundRunExportConfig





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundRunConfigCard(
    enabled: Boolean,
    config: BackgroundRunExportConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (BackgroundRunExportConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.backgroundRunTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!enabled) {
                            Text(
                                Strings.notEnabled,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                WtaSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }


            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.backgroundRunShowNotification,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        WtaSwitch(
                            checked = config.showNotification,
                            onCheckedChange = { onConfigChange(config.copy(showNotification = it)) }
                        )
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.backgroundRunKeepCpuAwake,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        WtaSwitch(
                            checked = config.keepCpuAwake,
                            onCheckedChange = { onConfigChange(config.copy(keepCpuAwake = it)) }
                        )
                    }


                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = { BackgroundRunService.requestIgnoreBatteryOptimizations(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.BatteryStd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                Strings.backgroundRunBatteryOptimization,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                Strings.backgroundRunBatteryOptimizationDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }


                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (expanded) Strings.hideAdvanced else Strings.showAdvanced)
                    }


                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            PremiumTextField(
                                value = config.notificationTitle,
                                onValueChange = { onConfigChange(config.copy(notificationTitle = it)) },
                                label = { Text(Strings.backgroundRunNotificationTitle) },
                                placeholder = { Text(Strings.backgroundRunNotificationTitlePlaceholder) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )


                            PremiumTextField(
                                value = config.notificationContent,
                                onValueChange = { onConfigChange(config.copy(notificationContent = it)) },
                                label = { Text(Strings.backgroundRunNotificationContent) },
                                placeholder = { Text(Strings.backgroundRunNotificationContentPlaceholder) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }
}
