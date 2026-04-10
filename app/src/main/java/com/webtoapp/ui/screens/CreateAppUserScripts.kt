package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import androidx.compose.ui.graphics.Color

/**
 * 用户脚本配置区域
 */
@Composable
fun UserScriptsSection(
    scripts: List<UserScript>,
    onScriptsChange: (List<UserScript>) -> Unit
) {
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingScript by remember { mutableStateOf<UserScript?>(null) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    
    Column {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Code,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = Strings.userScripts,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = Strings.userScriptsDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = {
                    editingScript = null
                    editingIndex = -1
                    showEditorDialog = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    Strings.addScript,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Script列表
        if (scripts.isEmpty()) {
            Text(
                text = Strings.noScripts,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            scripts.forEachIndexed { index, script ->
                UserScriptItem(
                    script = script,
                    onEdit = {
                        editingScript = script
                        editingIndex = index
                        showEditorDialog = true
                    },
                    onDelete = {
                        onScriptsChange(scripts.filterIndexed { i, _ -> i != index })
                    },
                    onToggle = { enabled ->
                        onScriptsChange(scripts.mapIndexed { i, s ->
                            if (i == index) s.copy(enabled = enabled) else s
                        })
                    }
                )
                if (index < scripts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
    
    // Script编辑对话框
    if (showEditorDialog) {
        UserScriptEditorDialog(
            script = editingScript,
            onDismiss = { showEditorDialog = false },
            onSave = { script ->
                if (editingIndex >= 0) {
                    // 编辑现有脚本
                    onScriptsChange(scripts.mapIndexed { i, s ->
                        if (i == editingIndex) script else s
                    })
                } else {
                    // 添加新脚本
                    onScriptsChange(scripts + script)
                }
                showEditorDialog = false
            }
        )
    }
}

/**
 * 单个脚本项
 */
@Composable
fun UserScriptItem(
    script: UserScript,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = script.name.ifBlank { Strings.userScripts },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = when (script.runAt) {
                    ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStart
                    ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEnd
                    ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdle
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            PremiumSwitch(
                checked = script.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Edit,
                    Strings.btnEdit,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    Strings.btnDelete,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 脚本编辑对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptEditorDialog(
    script: UserScript?,
    onDismiss: () -> Unit,
    onSave: (UserScript) -> Unit
) {
    var name by remember { mutableStateOf(script?.name ?: "") }
    var code by remember { mutableStateOf(script?.code ?: "") }
    var runAt by remember { mutableStateOf(script?.runAt ?: ScriptRunTime.DOCUMENT_END) }
    var enabled by remember { mutableStateOf(script?.enabled ?: true) }
    var runAtExpanded by remember { mutableStateOf(false) }
    
    var nameError by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }
    
    val isEdit = script != null
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // 大代码阈值：超过此长度显示只读摘要而不是可编辑输入框
    val largeCodeThreshold = 5000
    val isLargeCode = code.length > largeCodeThreshold
    
    // JS 文件导入
    val jsFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
                if (content.isNotEmpty()) {
                    code = content
                    codeError = false
                    // 自动填充文件名作为脚本名称（仅当名称为空时）
                    if (name.isBlank()) {
                        val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""
                        if (fileName.isNotBlank()) name = fileName
                    }
                }
            } catch (e: Exception) { android.util.Log.w("CreateApp", "Failed to read script file", e) }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) Strings.editScript else Strings.addScript) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Script名称
                PremiumTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = false
                    },
                    label = { Text(Strings.scriptName) },
                    placeholder = { Text(Strings.scriptNamePlaceholder) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(Strings.scriptNameRequired, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 运行时机选择
                ExposedDropdownMenuBox(
                    expanded = runAtExpanded,
                    onExpandedChange = { runAtExpanded = it }
                ) {
                    PremiumTextField(
                        value = when (runAt) {
                            ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStart
                            ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEnd
                            ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdle
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.scriptRunAt) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = runAtExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = runAtExpanded,
                        onDismissRequest = { runAtExpanded = false }
                    ) {
                        ScriptRunTime.values().forEach { time ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(when (time) {
                                            ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStart
                                            ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEnd
                                            ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdle
                                        })
                                        Text(
                                            text = when (time) {
                                                ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStartDesc
                                                ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEndDesc
                                                ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdleDesc
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    runAt = time
                                    runAtExpanded = false
                                },
                                leadingIcon = {
                                    if (time == runAt) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Enable开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.scriptEnabled, style = MaterialTheme.typography.bodyMedium)
                    PremiumSwitch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
                
                // 导入 JS 文件按钮
                PremiumOutlinedButton(
                    onClick = {
                        jsFilePickerLauncher.launch(arrayOf(
                            "application/javascript",
                            "application/x-javascript",
                            "text/javascript",
                            "text/plain",
                            "*/*"
                        ))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.scriptImportFile)
                }
                
                // Script代码 —— 根据代码大小切换显示模式
                if (isLargeCode) {
                    // 大代码：只读摘要卡片（避免 TextField 卡死）
                    val lineCount = code.count { it == '\n' } + 1
                    val sizeText = if (code.length > 1024) "%.1f KB".format(code.length / 1024f) else "${code.length} B"
                    val preview = code.lineSequence().take(6).joinToString("\n")
                    
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.scriptFileLoaded.format(lineCount, sizeText),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { code = "" },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.scriptClearCode, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = preview + if (lineCount > 6) "\n..." else "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 8,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // 小代码：正常可编辑输入框
                    PremiumTextField(
                        value = code,
                        onValueChange = { 
                            code = it
                            codeError = false
                        },
                        label = { Text(Strings.scriptCode) },
                        placeholder = { Text(Strings.scriptCodePlaceholder) },
                        minLines = 6,
                        maxLines = 12,
                        isError = codeError,
                        supportingText = if (codeError) {
                            { Text(Strings.scriptCodeRequired, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = name.isBlank()
                    codeError = code.isBlank()
                    
                    if (!nameError && !codeError) {
                        onSave(UserScript(
                            name = name,
                            code = code,
                            enabled = enabled,
                            runAt = runAt
                        ))
                    }
                }
            ) {
                Text(Strings.btnSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
