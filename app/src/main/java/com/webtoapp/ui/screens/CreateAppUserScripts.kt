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
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import androidx.compose.ui.graphics.Color

/**
 * user configarea
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
        // Note
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
                        text = AppStringsProvider.current().userScripts,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = AppStringsProvider.current().userScriptsDesc,
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
                    AppStringsProvider.current().addScript,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Scriptlist
        if (scripts.isEmpty()) {
            Text(
                text = AppStringsProvider.current().noScripts,
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
    
    // Scripteditdialog
    if (showEditorDialog) {
        UserScriptEditorDialog(
            script = editingScript,
            onDismiss = { showEditorDialog = false },
            onSave = { script ->
                if (editingIndex >= 0) {
                    // edit
                    onScriptsChange(scripts.mapIndexed { i, s ->
                        if (i == editingIndex) script else s
                    })
                } else {
                    // Note
                    onScriptsChange(scripts + script)
                }
                showEditorDialog = false
            }
        )
    }
}

/**
 * Note
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
                text = script.name.ifBlank { AppStringsProvider.current().userScripts },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = when (script.runAt) {
                    ScriptRunTime.DOCUMENT_START -> AppStringsProvider.current().runTimeDocStart
                    ScriptRunTime.DOCUMENT_END -> AppStringsProvider.current().runTimeDocEnd
                    ScriptRunTime.DOCUMENT_IDLE -> AppStringsProvider.current().runTimeDocIdle
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
                    AppStringsProvider.current().btnEdit,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    AppStringsProvider.current().btnDelete,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * editdialog
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
    
    // code: display editinput
    val largeCodeThreshold = 5000
    val isLargeCode = code.length > largeCodeThreshold
    
    // JS fileimport
    val jsFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
                if (content.isNotEmpty()) {
                    code = content
                    codeError = false
                    // file( onlywhen)
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
        title = { Text(if (isEdit) AppStringsProvider.current().editScript else AppStringsProvider.current().addScript) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Script
                PremiumTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = false
                    },
                    label = { Text(AppStringsProvider.current().scriptName) },
                    placeholder = { Text(AppStringsProvider.current().scriptNamePlaceholder) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(AppStringsProvider.current().scriptNameRequired, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // run select
                ExposedDropdownMenuBox(
                    expanded = runAtExpanded,
                    onExpandedChange = { runAtExpanded = it }
                ) {
                    PremiumTextField(
                        value = when (runAt) {
                            ScriptRunTime.DOCUMENT_START -> AppStringsProvider.current().runTimeDocStart
                            ScriptRunTime.DOCUMENT_END -> AppStringsProvider.current().runTimeDocEnd
                            ScriptRunTime.DOCUMENT_IDLE -> AppStringsProvider.current().runTimeDocIdle
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStringsProvider.current().scriptRunAt) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = runAtExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                                            ScriptRunTime.DOCUMENT_START -> AppStringsProvider.current().runTimeDocStart
                                            ScriptRunTime.DOCUMENT_END -> AppStringsProvider.current().runTimeDocEnd
                                            ScriptRunTime.DOCUMENT_IDLE -> AppStringsProvider.current().runTimeDocIdle
                                        })
                                        Text(
                                            text = when (time) {
                                                ScriptRunTime.DOCUMENT_START -> AppStringsProvider.current().runTimeDocStartDesc
                                                ScriptRunTime.DOCUMENT_END -> AppStringsProvider.current().runTimeDocEndDesc
                                                ScriptRunTime.DOCUMENT_IDLE -> AppStringsProvider.current().runTimeDocIdleDesc
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
                
                // Enable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStringsProvider.current().scriptEnabled, style = MaterialTheme.typography.bodyMedium)
                    PremiumSwitch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
                
                // import JS filebutton
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
                    Text(AppStringsProvider.current().scriptImportFile)
                }
                
                // Script code - switch display mode by code size
                if (isLargeCode) {
                    // Large code: read-only summary card
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
                                    text = AppStringsProvider.current().scriptFileLoaded.format(lineCount, sizeText),
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
                                    Text(AppStringsProvider.current().scriptClearCode, style = MaterialTheme.typography.labelSmall)
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
                    // Small code: editable input field
                    PremiumTextField(
                        value = code,
                        onValueChange = { 
                            code = it
                            codeError = false
                        },
                        label = { Text(AppStringsProvider.current().scriptCode) },
                        placeholder = { Text(AppStringsProvider.current().scriptCodePlaceholder) },
                        minLines = 6,
                        maxLines = 12,
                        isError = codeError,
                        supportingText = if (codeError) {
                            { Text(AppStringsProvider.current().scriptCodeRequired, color = MaterialTheme.colorScheme.error) }
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
                Text(AppStringsProvider.current().btnSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}
