package com.webtoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.activation.ActivationResult
import com.webtoapp.core.activation.ActivationStatus
import com.webtoapp.core.i18n.Strings
import java.text.SimpleDateFormat
import java.util.*

/**
 * 增强版激活码对话框 - 显示激活状态信息
 */
@Composable
fun EnhancedActivationDialog(
    onDismiss: () -> Unit,
    onActivate: suspend (String) -> ActivationResult,
    activationStatus: ActivationStatus? = null
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var activationResult by remember { mutableStateOf<ActivationResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.activateApp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show当前激活状态
                activationStatus?.let { status ->
                    if (status.isActivated) {
                        ActivationStatusCard(status = status)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Text(Strings.enterActivationCodeToContinue)
                
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                        activationResult = null
                    },
                    label = { Text(Strings.inputActivationCode) },
                    placeholder = { Text(Strings.activationCodeExample) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Show激活结果
                activationResult?.let { result ->
                    when (result) {
                        is ActivationResult.Success -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    Strings.success,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.activationSuccess,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is ActivationResult.Invalid -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    Strings.error,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is ActivationResult.DeviceMismatch -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    Strings.warning,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.activationCodeBoundToOtherDevice,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is ActivationResult.Expired -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    Strings.warning,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.activationCodeExpired,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is ActivationResult.UsageExceeded -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    Strings.warning,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.activationCodeUsageExceeded,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is ActivationResult.AlreadyActivated -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    Strings.info,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.appAlreadyActivated,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is ActivationResult.Empty -> {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        error = Strings.pleaseEnterActivationCode
                    } else {
                        isLoading = true
                        error = null
                        activationResult = null
                    }
                },
                enabled = !isLoading && code.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(Strings.activate)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )

    // Handle激活逻辑
    LaunchedEffect(isLoading) {
        if (isLoading && code.isNotBlank()) {
            val result = onActivate(code)
            activationResult = result
            isLoading = false
            
            if (result is ActivationResult.Success) {
                // 延迟关闭对话框，让用户看到成功消息
                kotlinx.coroutines.delay(1000)
                onDismiss()
            }
        }
    }
}

/**
 * 激活状态信息卡片
 */
@Composable
private fun ActivationStatusCard(status: ActivationStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isValid) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (status.isValid) Strings.activated else Strings.activationExpired,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (status.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                if (status.isValid) {
                    Icon(
                        Icons.Default.CheckCircle,
                        Strings.enabled,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        Strings.disabled,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Show激活时间
            status.activatedTime?.let { time ->
                Text(
                    text = "${Strings.activationTime}：${formatTime(time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            // Show过期时间
            status.expireTime?.let { expireTime ->
                val remaining = status. remainingTimeMs
                if (remaining != null && remaining > 0) {
                    val days = remaining / (24 * 60 * 60 * 1000)
                    val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                    Text(
                        text = "${Strings.remainingTime}：${days}${Strings.days}${hours}${Strings.hours}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.isValid) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                } else {
                    Text(
                        text = "${Strings.expireTime}：${formatTime(expireTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Show使用次数
            status.usageLimit?.let { limit ->
                val remaining = status.remainingUsage
                Text(
                    text = if (remaining != null && remaining > 0) {
                        "${Strings.remainingUsage}：$remaining / $limit"
                    } else {
                        "${Strings.usageCount}：${status.usageCount} / $limit"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            // Show设备绑定信息
            status.deviceId?.let {
                Text(
                    text = Strings.deviceBound,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

