package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.data.model.AutoStartConfig

/**
 * 自启动配置卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoStartCard(
    config: AutoStartConfig?,
    onConfigChange: (AutoStartConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(config != null && (config.bootStartEnabled || config.scheduledStartEnabled)) }
    var bootStartEnabled by remember(config) { mutableStateOf(config?.bootStartEnabled ?: false) }
    var scheduledStartEnabled by remember(config) { mutableStateOf(config?.scheduledStartEnabled ?: false) }
    var scheduledTime by remember(config) { mutableStateOf(config?.scheduledTime ?: "08:00") }
    var scheduledDays by remember(config) { mutableStateOf(config?.scheduledDays ?: listOf(1,2,3,4,5,6,7)) }
    
    // 时间选择对话框
    var showTimePicker by remember { mutableStateOf(false) }
    
    fun updateConfig() {
        if (!bootStartEnabled && !scheduledStartEnabled) {
            onConfigChange(null)
        } else {
            onConfigChange(AutoStartConfig(
                bootStartEnabled = bootStartEnabled,
                scheduledStartEnabled = scheduledStartEnabled,
                scheduledTime = scheduledTime,
                scheduledDays = scheduledDays
            ))
        }
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PowerSettingsNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "自启动设置",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (bootStartEnabled || scheduledStartEnabled) "已配置" else "未启用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // 开机自启动
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "开机自启动",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "设备开机后自动启动此应用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bootStartEnabled,
                            onCheckedChange = {
                                bootStartEnabled = it
                                updateConfig()
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 定时自启动
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "定时自启动",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "在指定时间自动启动此应用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = scheduledStartEnabled,
                            onCheckedChange = {
                                scheduledStartEnabled = it
                                updateConfig()
                            }
                        )
                    }
                    
                    // 定时启动详细配置
                    AnimatedVisibility(visible = scheduledStartEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // 时间选择
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("启动时间")
                                    }
                                    Text(
                                        scheduledTime,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 星期选择
                            Text(
                                "启动日期",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
                                dayNames.forEachIndexed { index, name ->
                                    val day = index + 1
                                    val isSelected = scheduledDays.contains(day)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scheduledDays = if (isSelected) {
                                                scheduledDays - day
                                            } else {
                                                scheduledDays + day
                                            }
                                            updateConfig()
                                        },
                                        label = { Text(name) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 提示信息
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "自启动功能仅在导出的 APK 中生效。部分手机需要在系统设置中授予自启动权限。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 时间选择对话框
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = scheduledTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                scheduledTime = time
                updateConfig()
                showTimePicker = false
            }
        )
    }
}

/**
 * 时间选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择启动时间") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    onConfirm("$hour:$minute")
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
