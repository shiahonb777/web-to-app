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
        title = { Text("激活应用") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 显示当前激活状态
                activationStatus?.let { status ->
                    if (status.isActivated) {
                        ActivationStatusCard(status = status)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Text("请输入激活码以继续使用")
                
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                        activationResult = null
                    },
                    label = { Text("激活码") },
                    placeholder = { Text("例如：XXXX-XXXX-XXXX-XXXX") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // 显示激活结果
                activationResult?.let { result ->
                    when (result) {
                        is ActivationResult.Success -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "成功",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "激活成功！",
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
                                    "错误",
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
                                    "警告",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "此激活码已绑定到其他设备",
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
                                    "过期",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "激活码已过期",
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
                                    "超限",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "激活码使用次数已用完",
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
                                    "已激活",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "应用已激活",
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
                        error = "请输入激活码"
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
                Text("激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 处理激活逻辑
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
                    text = if (status.isValid) "已激活" else "激活已失效",
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
                        "有效",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        "失效",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 显示激活时间
            status.activatedTime?.let { time ->
                Text(
                    text = "激活时间：${formatTime(time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            // 显示过期时间
            status.expireTime?.let { expireTime ->
                val remaining = status. remainingTimeMs
                if (remaining != null && remaining > 0) {
                    val days = remaining / (24 * 60 * 60 * 1000)
                    val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                    Text(
                        text = "剩余时间：${days}天${hours}小时",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.isValid) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                } else {
                    Text(
                        text = "过期时间：${formatTime(expireTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // 显示使用次数
            status.usageLimit?.let { limit ->
                val remaining = status.remainingUsage
                Text(
                    text = if (remaining != null && remaining > 0) {
                        "剩余次数：$remaining / $limit"
                    } else {
                        "使用次数：${status.usageCount} / $limit"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            // 显示设备绑定信息
            status.deviceId?.let {
                Text(
                    text = "设备绑定：已启用",
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

