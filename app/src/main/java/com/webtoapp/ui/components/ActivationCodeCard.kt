package com.webtoapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.activation.ActivationCode
import com.webtoapp.core.activation.ActivationCodeType
import java.util.concurrent.TimeUnit

/**
 * 激活码配置卡片（新版本 - 支持多种类型）
 */
@Composable
fun ActivationCodeCard(
    enabled: Boolean,
    activationCodes: List<ActivationCode>,
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<ActivationCode>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Key,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "激活码验证",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                Text(
                    text = "启用后，用户需要输入正确的激活码才能使用应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 添加激活码按钮
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, "添加")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加激活码")
                }

                // 激活码列表
                if (activationCodes.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activationCodes.forEachIndexed { index, code ->
                            ActivationCodeItem(
                                code = code,
                                onDelete = {
                                    onCodesChange(activationCodes.filterIndexed { i, _ -> i != index })
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "暂无激活码，点击上方按钮添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    // 添加激活码对话框
    if (showAddDialog) {
        AddActivationCodeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code ->
                onCodesChange(activationCodes + code)
                showAddDialog = false
            }
        )
    }
}

/**
 * 激活码项显示
 */
@Composable
private fun ActivationCodeItem(
    code: ActivationCode,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = code.code,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = code.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 显示限制信息
            when (code.type) {
                ActivationCodeType.TIME_LIMITED -> {
                    code.timeLimitMs?.let { timeLimit ->
                        val days = TimeUnit.MILLISECONDS.toDays(timeLimit)
                        val hours = TimeUnit.MILLISECONDS.toHours(timeLimit) % 24
                        Text(
                            text = "有效期：${days}天${if (hours > 0) " ${hours}小时" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ActivationCodeType.USAGE_LIMITED -> {
                    code.usageLimit?.let { limit ->
                        Text(
                            text = "使用次数：$limit 次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ActivationCodeType.COMBINED -> {
                    val info = buildString {
                        code.timeLimitMs?.let { timeLimit ->
                            val days = TimeUnit.MILLISECONDS.toDays(timeLimit)
                            append("有效期：${days}天")
                        }
                        code.usageLimit?.let { limit ->
                            if (isNotEmpty()) append(" | ")
                            append("使用次数：$limit 次")
                        }
                    }
                    if (info.isNotEmpty()) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ActivationCodeType.DEVICE_BOUND -> {
                    Text(
                        text = "设备绑定：已启用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = "永久有效",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 显示备注
            code.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = "备注：$note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 添加激活码对话框
 */
@Composable
private fun AddActivationCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (ActivationCode) -> Unit
) {
    var codeType by remember { mutableStateOf(ActivationCodeType.PERMANENT) }
    var timeLimitDays by remember { mutableStateOf("7") }
    var usageLimit by remember { mutableStateOf("100") }
    var note by remember { mutableStateOf("") }
    var customCode by remember { mutableStateOf("") }
    var useCustomCode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加激活码") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 激活码类型选择
                Text(
                    text = "激活码类型",
                    style = MaterialTheme.typography.labelMedium
                )
                ActivationCodeType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { codeType = type },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = codeType == type,
                            onClick = { codeType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = type.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 自定义激活码
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useCustomCode,
                        onCheckedChange = { useCustomCode = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("使用自定义激活码")
                }

                if (useCustomCode) {
                    OutlinedTextField(
                        value = customCode,
                        onValueChange = { customCode = it },
                        label = { Text("激活码") },
                        placeholder = { Text("例如：XXXX-XXXX-XXXX-XXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // 时间限制配置
                if (codeType == ActivationCodeType.TIME_LIMITED || 
                    codeType == ActivationCodeType.COMBINED) {
                    OutlinedTextField(
                        value = timeLimitDays,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                timeLimitDays = it
                            }
                        },
                        label = { Text("有效期（天）") },
                        placeholder = { Text("例如：7") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // 使用次数限制配置
                if (codeType == ActivationCodeType.USAGE_LIMITED || 
                    codeType == ActivationCodeType.COMBINED) {
                    OutlinedTextField(
                        value = usageLimit,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                usageLimit = it
                            }
                        },
                        label = { Text("使用次数") },
                        placeholder = { Text("例如：100") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("例如：VIP用户专用") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeLimitMs = when (codeType) {
                        ActivationCodeType.TIME_LIMITED, ActivationCodeType.COMBINED -> {
                            timeLimitDays.toLongOrNull()?.let { 
                                TimeUnit.DAYS.toMillis(it) 
                            }
                        }
                        else -> null
                    }

                    val usageLimitInt = when (codeType) {
                        ActivationCodeType.USAGE_LIMITED, ActivationCodeType.COMBINED -> {
                            usageLimit.toIntOrNull()
                        }
                        else -> null
                    }

                    val code = if (useCustomCode && customCode.isNotBlank()) {
                        ActivationCode(
                            code = customCode.trim(),
                            type = codeType,
                            timeLimitMs = timeLimitMs,
                            usageLimit = usageLimitInt,
                            note = note.takeIf { it.isNotBlank() }
                        )
                    } else {
                        // 生成随机激活码
                        val activationManager = com.webtoapp.WebToAppApplication.getInstance()
                            .activationManager
                        activationManager.generateActivationCode(
                            type = codeType,
                            timeLimitMs = timeLimitMs,
                            usageLimit = usageLimitInt,
                            note = note.takeIf { it.isNotBlank() }
                        )
                    }

                    onConfirm(code)
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

