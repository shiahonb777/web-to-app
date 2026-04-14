package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.BackgroundRunExportConfig

/**
 * runconfigcard
 * forconfigapp run
 */
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
            // Note
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
                            AppStringsProvider.current().backgroundRunTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!enabled) {
                            Text(
                                AppStringsProvider.current().notEnabled,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            // Expand config
            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            AppStringsProvider.current().backgroundRunShowNotification,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        PremiumSwitch(
                            checked = config.showNotification,
                            onCheckedChange = { onConfigChange(config.copy(showNotification = it)) }
                        )
                    }

                    // CPU
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            AppStringsProvider.current().backgroundRunKeepCpuAwake,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        PremiumSwitch(
                            checked = config.keepCpuAwake,
                            onCheckedChange = { onConfigChange(config.copy(keepCpuAwake = it)) }
                        )
                    }
                    
                    // Expand settings
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (expanded) AppStringsProvider.current().hideAdvanced else AppStringsProvider.current().showAdvanced)
                    }
                    
                    // advancedsettings
                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Custom
                            PremiumTextField(
                                value = config.notificationTitle,
                                onValueChange = { onConfigChange(config.copy(notificationTitle = it)) },
                                label = { Text(AppStringsProvider.current().backgroundRunNotificationTitle) },
                                placeholder = { Text(AppStringsProvider.current().backgroundRunNotificationTitlePlaceholder) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            // Custom content
                            PremiumTextField(
                                value = config.notificationContent,
                                onValueChange = { onConfigChange(config.copy(notificationContent = it)) },
                                label = { Text(AppStringsProvider.current().backgroundRunNotificationContent) },
                                placeholder = { Text(AppStringsProvider.current().backgroundRunNotificationContentPlaceholder) },
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
