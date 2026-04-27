package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.NotificationExportConfig
import com.webtoapp.data.model.NotificationType

/**
 * 通知配置卡片
 * 支持两种通知类型：Web API（本地通知）和 Polling（轮询通知）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationConfigCard(
    enabled: Boolean,
    config: NotificationExportConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (NotificationExportConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
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
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.notificationConfigTitle,
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
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            // 启用后展示配置
            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 通知类型选择
                    Text(
                        Strings.notificationTypeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotificationTypeChip(
                            selected = config.type == NotificationType.WEB_API,
                            label = Strings.notificationTypeWebApi,
                            onClick = { onConfigChange(config.copy(type = NotificationType.WEB_API)) }
                        )
                        NotificationTypeChip(
                            selected = config.type == NotificationType.POLLING,
                            label = Strings.notificationTypePolling,
                            onClick = { onConfigChange(config.copy(type = NotificationType.POLLING)) }
                        )
                    }

                    // Web API 类型说明
                    if (config.type == NotificationType.WEB_API) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                Strings.notificationWebApiDesc,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Polling 类型配置
                    if (config.type == NotificationType.POLLING) {
                        // 轮询 URL
                        PremiumTextField(
                            value = config.pollUrl,
                            onValueChange = { onConfigChange(config.copy(pollUrl = it)) },
                            label = { Text(Strings.notificationPollUrl) },
                            placeholder = { Text(Strings.notificationPollUrlPlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // 轮询间隔
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                Strings.notificationPollInterval,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            var intervalText by remember(config.pollIntervalMinutes) {
                                mutableStateOf(config.pollIntervalMinutes.toString())
                            }
                            OutlinedTextField(
                                value = intervalText,
                                onValueChange = { newText ->
                                    intervalText = newText
                                    newText.toIntOrNull()?.let { num ->
                                        onConfigChange(config.copy(pollIntervalMinutes = num.coerceAtLeast(5)))
                                    }
                                },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                        }
                        Text(
                            Strings.notificationPollIntervalHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 展开更多设置
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (expanded) Strings.hideAdvanced else Strings.showAdvanced)
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // 请求方法
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        Strings.notificationPollMethod,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = config.pollMethod == "GET",
                                            onClick = { onConfigChange(config.copy(pollMethod = "GET")) },
                                            label = { Text("GET") }
                                        )
                                        FilterChip(
                                            selected = config.pollMethod == "POST",
                                            onClick = { onConfigChange(config.copy(pollMethod = "POST")) },
                                            label = { Text("POST") }
                                        )
                                    }
                                }

                                // 自定义 Headers
                                PremiumTextField(
                                    value = config.pollHeaders,
                                    onValueChange = { onConfigChange(config.copy(pollHeaders = it)) },
                                    label = { Text(Strings.notificationPollHeaders) },
                                    placeholder = { Text(Strings.notificationPollHeadersPlaceholder) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4
                                )

                                // 点击跳转 URL
                                PremiumTextField(
                                    value = config.clickUrl,
                                    onValueChange = { onConfigChange(config.copy(clickUrl = it)) },
                                    label = { Text(Strings.notificationClickUrl) },
                                    placeholder = { Text(Strings.notificationClickUrlPlaceholder) },
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
}

@Composable
private fun NotificationTypeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}
