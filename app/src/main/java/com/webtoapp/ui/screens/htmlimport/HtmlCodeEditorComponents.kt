package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import android.provider.DocumentsContract
import com.webtoapp.core.logging.AppLogger
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.HtmlProjectOptimizer
import com.webtoapp.core.linux.NativeNodeEngine
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.*
import com.webtoapp.util.HtmlProjectProcessor
import com.webtoapp.util.ZipProjectImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * 文件槽位组件
 */
@Composable
internal fun FileSlot(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    file: HtmlFile?,
    required: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (file != null) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            )
            .border(
                width = 1.dp,
                color = if (file != null)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else if (required)
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (file != null) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (required) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (file != null) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = Strings.clickToSelectFile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (file != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = Strings.clearFile,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 带编辑按钮的文件槽位组件
 */
@Composable
internal fun FileSlotWithEditor(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    file: HtmlFile?,
    required: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (file != null) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            )
            .border(
                width = 1.dp,
                color = if (file != null)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else if (required)
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (file != null) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (required) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (file != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${Strings.orWriteDirectly}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "${Strings.clickToSelectFile} ${Strings.orWriteDirectly}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Edit button
        IconButton(
            onClick = { onEdit() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = Strings.editCode,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        if (file != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = Strings.clearFile,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 全屏代码编辑器对话框
 */
@Composable
internal fun CodeEditorDialog(
    fileType: HtmlFileType,
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var codeText by remember { mutableStateOf(initialContent) }
    val isModified = codeText != initialContent
    
    val title = when (fileType) {
        HtmlFileType.HTML -> "HTML"
        HtmlFileType.CSS -> "CSS"
        HtmlFileType.JS -> "JavaScript"
        else -> Strings.codeEditorTitle
    }
    
    val placeholder = when (fileType) {
        HtmlFileType.HTML -> Strings.htmlCodePlaceholder
        HtmlFileType.CSS -> Strings.cssCodePlaceholder
        HtmlFileType.JS -> Strings.jsCodePlaceholder
        else -> ""
    }
    
    // Accent color for syntax label
    val accentColor = when (fileType) {
        HtmlFileType.HTML -> Color(0xFFE44D26)
        HtmlFileType.CSS -> Color(0xFF264DE4)
        HtmlFileType.JS -> Color(0xFFF7DF1E)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Dialog(
        onDismissRequest = {
            if (!isModified) onDismiss()
            // If modified, user must explicitly save or discard
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isModified
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            color = Color(0xFF1E1E1E), // VS Code dark bg
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ==================== Top Bar ====================
                Surface(
                    color = Color(0xFF252526),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isModified) {
                                // Just dismiss without saving when back is pressed
                                // The user clicked X, they probably want to cancel
                            }
                            onDismiss()
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = Strings.close,
                                tint = Color(0xFFCCCCCC)
                            )
                        }
                        
                        // File type badge
                        Surface(
                            color = accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = Strings.codeEditorTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFCCCCCC),
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isModified) {
                            Surface(
                                color = Color(0xFF4EC9B0).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "●",
                                    color = Color(0xFF4EC9B0),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        // Save button
                        TextButton(
                            onClick = { onSave(codeText) },
                            enabled = codeText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = null,
                                tint = if (codeText.isNotBlank()) Color(0xFF4EC9B0) else Color(0xFF666666),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = Strings.saveFile,
                                color = if (codeText.isNotBlank()) Color(0xFF4EC9B0) else Color(0xFF666666)
                            )
                        }
                    }
                }
                
                // ==================== Code Editor Area ====================
                val scrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Line numbers
                        val lineCount = maxOf(codeText.count { it == '\n' } + 1, 1)
                        Column(
                            modifier = Modifier
                                .width(44.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF1E1E1E))
                                .verticalScroll(scrollState)
                                .padding(end = 8.dp, top = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = Color(0xFF858585)
                                    )
                                )
                            }
                        }
                        
                        // Vertical divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF333333))
                        )
                        
                        // Code input area
                        BasicTextField(
                            value = codeText,
                            onValueChange = { codeText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFFD4D4D4)
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .horizontalScroll(horizontalScrollState)
                                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (codeText.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                color = Color(0xFF555555)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                
                // ==================== Bottom Status Bar ====================
                Surface(
                    color = Color(0xFF007ACC)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically

                    ) {
                        val lineCount = codeText.count { it == '\n' } + 1
                        val charCount = codeText.length
                        Text(
                            text = "$lineCount ${if (lineCount == 1) "line" else "lines"}, $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 从Uri获取文件名
 */
internal fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}

/**
 * 复制Uri内容到临时文件
 */
internal fun copyUriToTempFile(
    context: android.content.Context,
    uri: Uri,
    fileName: String?
): File? {
    return try {
        val tempDir = File(context.cacheDir, "html_temp").apply { mkdirs() }
        val targetFile = File(tempDir, fileName ?: "file_${System.currentTimeMillis()}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    } catch (e: Exception) {
        AppLogger.e("CreateHtmlAppScreen", "Operation failed", e)
        null
    }
}

/**
 * 根据文件名获取文件类型
 */
internal fun getFileType(fileName: String): HtmlFileType {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "html", "htm" -> HtmlFileType.HTML
        "css" -> HtmlFileType.CSS
        "js" -> HtmlFileType.JS
        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico" -> HtmlFileType.IMAGE
        "ttf", "otf", "woff", "woff2", "eot" -> HtmlFileType.FONT
        else -> HtmlFileType.OTHER
    }
}
