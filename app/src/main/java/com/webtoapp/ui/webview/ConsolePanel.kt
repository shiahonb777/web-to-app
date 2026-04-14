package com.webtoapp.ui.webview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.AppColors

// ========== related ==========

/**
 * Note
 */
enum class ConsoleLevel {
    LOG, INFO, WARNING, ERROR, DEBUG
}

/**
 * Note
 */
data class ConsoleLogEntry(
    val level: ConsoleLevel,
    val message: String,
    val source: String,
    val lineNumber: Int,
    val timestamp: Long
)

/**
 * panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsolePanel(
    consoleMessages: List<ConsoleLogEntry>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onClear: () -> Unit,
    onRunScript: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scriptInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()) }
    
    // Themecolor
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    
    // Autoscroll bottom
    LaunchedEffect(consoleMessages.size) {
        if (consoleMessages.isNotEmpty()) {
            listState.animateScrollToItem(consoleMessages.size - 1)
        }
    }
    
    // , ensure
    val panelHeight = if (isExpanded) 350.dp else 200.dp
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight),
        color = surfaceColor,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // header
            Surface(
                color = surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            Strings.console,
                            style = MaterialTheme.typography.titleSmall,
                            color = onSurface
                        )
                        // Error/warning
                        val errorCount = consoleMessages.count { it.level == ConsoleLevel.ERROR }
                        val warnCount = consoleMessages.count { it.level == ConsoleLevel.WARNING }
                        if (errorCount > 0) {
                            Badge(containerColor = errorColor) {
                                Text("$errorCount")
                            }
                        }
                        if (warnCount > 0) {
                            Badge(containerColor = Color(0xFFFFB74D)) {
                                Text("$warnCount", color = Color.Black)
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Copyall
                        IconButton(
                            onClick = {
                                val allLogs = consoleMessages.joinToString("\n") { entry ->
                                    "[${timeFormat.format(java.util.Date(entry.timestamp))}] [${entry.level}] ${entry.message}"
                                }
                                clipboardManager.setText(AnnotatedString(allLogs))
                                Toast.makeText(context, Strings.copiedAllLogs, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = Strings.copy, tint = onSurfaceVariant)
                        }
                        // Note
                        IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Delete, contentDescription = Strings.clean, tint = onSurfaceVariant)
                        }
                        // Expand/
                        IconButton(onClick = onExpandToggle, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                if (isExpanded) Strings.close else Strings.more,
                                tint = onSurfaceVariant
                            )
                        }
                        // close
                        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, Strings.close, tint = onSurfaceVariant)
                        }
                    }
                }
            }
            
            // messagelist
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true)
            ) {
                if (consoleMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                Strings.noConsoleMessages,
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(consoleMessages.size) { index ->
                            val entry = consoleMessages[index]
                            ConsoleLogItem(
                                entry = entry,
                                timeFormat = timeFormat,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(entry.message))
                                    Toast.makeText(context, Strings.msgCopied, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
            
            // Scriptinput
            Surface(
                color = surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ">",
                        color = primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedTextField(
                        value = scriptInput,
                        onValueChange = { scriptInput = it },
                        placeholder = { 
                            Text(
                                Strings.inputJavaScript,
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = {
                            if (scriptInput.isNotBlank()) {
                                onRunScript(scriptInput)
                                scriptInput = ""
                            }
                        },
                        enabled = scriptInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = Strings.run)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleLogItem(
    entry: ConsoleLogEntry,
    timeFormat: java.text.SimpleDateFormat,
    onCopy: () -> Unit
) {
    val errorColor = MaterialTheme.colorScheme.error
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    val backgroundColor = when (entry.level) {
        ConsoleLevel.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ConsoleLevel.WARNING -> Color(0xFFFFB74D).copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    
    val textColor = when (entry.level) {
        ConsoleLevel.ERROR -> errorColor
        ConsoleLevel.WARNING -> AppColors.Warning
        ConsoleLevel.DEBUG -> AppColors.Success
        else -> onSurface
    }
    
    val iconVector = when (entry.level) {
        ConsoleLevel.ERROR -> Icons.Filled.Error
        ConsoleLevel.WARNING -> Icons.Filled.Warning
        ConsoleLevel.DEBUG -> Icons.Filled.BugReport
        ConsoleLevel.INFO -> Icons.Filled.Info
        ConsoleLevel.LOG -> Icons.Filled.Description
    }
    
    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Icon(
                iconVector,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(14.dp),
                tint = textColor
            )
            
            // messagecontent
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                SelectionContainer {
                    Text(
                        entry.message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = textColor
                    )
                }
                
                // Note
                Text(
                    "${entry.source}:${entry.lineNumber} • ${timeFormat.format(java.util.Date(entry.timestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Copybutton
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    "复制",
                    tint = onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
