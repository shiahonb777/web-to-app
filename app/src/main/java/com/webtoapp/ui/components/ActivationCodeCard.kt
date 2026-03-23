package com.webtoapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.activation.ActivationCode
import com.webtoapp.core.activation.ActivationCodeType
import com.webtoapp.core.i18n.Strings
import java.util.concurrent.TimeUnit

/**
 * 激活码配置卡片（新版本 - 支持多种类型）
 */
@Composable
fun ActivationCodeCard(
    enabled: Boolean,
    activationCodes: List<ActivationCode>,
    requireEveryTime: Boolean = false,
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<ActivationCode>) -> Unit,
    onRequireEveryTimeChange: (Boolean) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        text = Strings.activationCodeVerify,
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
                    text = Strings.activationCodeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 每次启动都需要验证选项
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.requireEveryLaunch,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (requireEveryTime) Strings.requireEveryLaunchHintOn else Strings.requireEveryLaunchHintOff,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = requireEveryTime,
                        onCheckedChange = onRequireEveryTimeChange
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 添加激活码按钮
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, Strings.add)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.addActivationCode)
                }

                // Activation码列表
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
                        text = Strings.noActivationCodes,
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivationCodeItem(
    code: ActivationCode,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopiedToast by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show复制成功提示
    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            snackbarHostState.showSnackbar(
                message = Strings.activationCodeCopied,
                duration = SnackbarDuration.Short
            )
            showCopiedToast = false
        }
    }
    
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
                    // Activation码文本 - 支持长按复制和点击复制按钮
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code.code))
                                showCopiedToast = true
                            },
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(code.code))
                                showCopiedToast = true
                            }
                        )
                    ) {
                        Text(
                            text = code.code,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Copy按钮
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code.code))
                                showCopiedToast = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = Strings.copyActivationCode,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getActivationTypeName(code.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        Strings.btnDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Show限制信息
            when (code.type) {
                ActivationCodeType.TIME_LIMITED -> {
                    code.timeLimitMs?.let { timeLimit ->
                        val days = TimeUnit.MILLISECONDS.toDays(timeLimit)
                        val hours = TimeUnit.MILLISECONDS.toHours(timeLimit) % 24
                        Text(
                            text = "${Strings.validityPeriod}：${days}${Strings.days}${if (hours > 0) " ${hours}${Strings.hours}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ActivationCodeType.USAGE_LIMITED -> {
                    code.usageLimit?.let { limit ->
                        Text(
                            text = "${Strings.usageCount}：$limit ${Strings.times}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ActivationCodeType.COMBINED -> {
                    val info = buildString {
                        code.timeLimitMs?.let { timeLimit ->
                            val days = TimeUnit.MILLISECONDS.toDays(timeLimit)
                            append("${Strings.validityPeriod}：${days}${Strings.days}")
                        }
                        code.usageLimit?.let { limit ->
                            if (isNotEmpty()) append(" | ")
                            append("${Strings.usageCount}：$limit ${Strings.times}")
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
                        text = Strings.deviceBound,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = Strings.permanentValid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Show备注
            code.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = "${Strings.note}：$note",
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
        title = { Text(Strings.addActivationCode) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Activation码类型选择
                Text(
                    text = Strings.activationCodeType,
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
                                text = getActivationTypeName(type),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = getActivationTypeDesc(type),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Custom激活码
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useCustomCode,
                        onCheckedChange = { useCustomCode = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.useCustomCode)
                }

                if (useCustomCode) {
                    OutlinedTextField(
                        value = customCode,
                        onValueChange = { customCode = it },
                        label = { Text(Strings.inputActivationCode) },
                        placeholder = { Text(Strings.activationCodeExample) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Time限制配置
                if (codeType == ActivationCodeType.TIME_LIMITED || 
                    codeType == ActivationCodeType.COMBINED) {
                    OutlinedTextField(
                        value = timeLimitDays,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                timeLimitDays = it
                            }
                        },
                        label = { Text(Strings.validityDays) },
                        placeholder = { Text("7") },
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
                        label = { Text(Strings.usageCount) },
                        placeholder = { Text("100") },
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
                    label = { Text(Strings.noteOptional) },
                    placeholder = { Text(Strings.vipUserOnly) },
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
                        // Generate随机激活码
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
                Text(Strings.btnOk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

/**
 * 获取激活码类型的翻译名称
 */
private fun getActivationTypeName(type: ActivationCodeType): String = when (type) {
    ActivationCodeType.PERMANENT -> Strings.activationTypePermanent
    ActivationCodeType.TIME_LIMITED -> Strings.activationTypeTimeLimited
    ActivationCodeType.USAGE_LIMITED -> Strings.activationTypeUsageLimited
    ActivationCodeType.DEVICE_BOUND -> Strings.activationTypeDeviceBound
    ActivationCodeType.COMBINED -> Strings.activationTypeCombined
}

/**
 * 获取激活码类型的翻译描述
 */
private fun getActivationTypeDesc(type: ActivationCodeType): String = when (type) {
    ActivationCodeType.PERMANENT -> Strings.activationTypePermanentDesc
    ActivationCodeType.TIME_LIMITED -> Strings.activationTypeTimeLimitedDesc
    ActivationCodeType.USAGE_LIMITED -> Strings.activationTypeUsageLimitedDesc
    ActivationCodeType.DEVICE_BOUND -> Strings.activationTypeDeviceBoundDesc
    ActivationCodeType.COMBINED -> Strings.activationTypeCombinedDesc
}

