package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.BackgroundRunExportConfig

/**
 * 后台运行配置卡片
 * 用于配置应用退出后继续在后台运行
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
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            Strings.backgroundRunTitle,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            Strings.backgroundRunDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            // Expand的详细配置
            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show通知开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.backgroundRunShowNotification,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                Strings.backgroundRunShowNotificationDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.showNotification,
                            onCheckedChange = { onConfigChange(config.copy(showNotification = it)) }
                        )
                    }
                    
                    // 保持CPU唤醒开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.backgroundRunKeepCpuAwake,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                Strings.backgroundRunKeepCpuAwakeDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.keepCpuAwake,
                            onCheckedChange = { onConfigChange(config.copy(keepCpuAwake = it)) }
                        )
                    }
                    
                    // Expand更多设置
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (expanded) Strings.hideAdvanced else Strings.showAdvanced)
                    }
                    
                    // 高级设置
                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Custom通知标题
                            OutlinedTextField(
                                value = config.notificationTitle,
                                onValueChange = { onConfigChange(config.copy(notificationTitle = it)) },
                                label = { Text(Strings.backgroundRunNotificationTitle) },
                                placeholder = { Text(Strings.backgroundRunNotificationTitlePlaceholder) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            // Custom通知内容
                            OutlinedTextField(
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
