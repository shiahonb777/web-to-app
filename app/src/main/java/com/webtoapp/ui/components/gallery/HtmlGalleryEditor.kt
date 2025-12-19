package com.webtoapp.ui.components.gallery

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.HtmlItemConfig
import java.io.File

/**
 * HTML画廊编辑器 - 用于添加和管理多个HTML项目
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlGalleryEditor(
    items: List<GalleryItem>,
    onItemsChange: (List<GalleryItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<GalleryItem?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }
    
    // 临时存储新添加的HTML文件信息
    var pendingHtmlPath by remember { mutableStateOf<String?>(null) }
    var pendingHtmlName by remember { mutableStateOf<String?>(null) }
    
    // HTML文件选择器
    val htmlPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it)
            val tempFile = copyUriToTempFile(context, it, fileName)
            if (tempFile != null && fileName != null) {
                pendingHtmlPath = tempFile.absolutePath
                pendingHtmlName = fileName
                showAddDialog = true
            }
        }
    }
    
    Column(modifier = modifier) {
        // 标题和添加按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HTML项目列表 (${items.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            FilledTonalButton(onClick = { htmlPickerLauncher.launch("text/html") }) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加HTML")
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        if (items.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { htmlPickerLauncher.launch("text/html") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Code,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击添加HTML文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "可添加多个HTML页面，滑动切换",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // 项目列表
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    HtmlItemCard(
                        item = item,
                        index = index,
                        onEdit = {
                            editingItem = item
                            editingIndex = index
                        },
                        onDelete = {
                            onItemsChange(items.filterIndexed { i, _ -> i != index })
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newList = items.toMutableList()
                                newList[index] = newList[index - 1].also {
                                    newList[index - 1] = newList[index]
                                }
                                onItemsChange(newList)
                            }
                        },
                        onMoveDown = {
                            if (index < items.size - 1) {
                                val newList = items.toMutableList()
                                newList[index] = newList[index + 1].also {
                                    newList[index + 1] = newList[index]
                                }
                                onItemsChange(newList)
                            }
                        },
                        canMoveUp = index > 0,
                        canMoveDown = index < items.size - 1
                    )
                }
            }
        }
    }
    
    // 添加对话框
    if (showAddDialog && pendingHtmlPath != null) {
        HtmlItemAddDialog(
            fileName = pendingHtmlName ?: "index.html",
            onDismiss = { 
                showAddDialog = false
                pendingHtmlPath = null
                pendingHtmlName = null
            },
            onAdd = { title, enableJs, enableStorage ->
                val newItem = GalleryItem(
                    title = title,
                    type = GalleryItemType.HTML,
                    path = pendingHtmlPath!!,
                    sortOrder = items.size,
                    htmlConfig = HtmlItemConfig(
                        entryFile = pendingHtmlName ?: "index.html",
                        enableJavaScript = enableJs,
                        enableLocalStorage = enableStorage
                    )
                )
                onItemsChange(items + newItem)
                showAddDialog = false
                pendingHtmlPath = null
                pendingHtmlName = null
            }
        )
    }
    
    // 编辑对话框
    if (editingItem != null) {
        HtmlItemEditDialog(
            item = editingItem!!,
            onDismiss = { 
                editingItem = null
                editingIndex = -1
            },
            onSave = { updatedItem ->
                val newList = items.toMutableList()
                newList[editingIndex] = updatedItem
                onItemsChange(newList)
                editingItem = null
                editingIndex = -1
            }
        )
    }
}

/**
 * 单个HTML项卡片
 */
@Composable
private fun HtmlItemCard(
    item: GalleryItem,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(24.dp)
            )
            
            Spacer(Modifier.width(8.dp))
            
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Code,
                    null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // 标题和文件名
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "未命名" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.htmlConfig?.entryFile ?: "index.html",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // JS启用标签
                if (item.htmlConfig?.enableJavaScript == true) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "JS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // 操作按钮
            Row {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "上移", Modifier.size(20.dp))
                }
                
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "下移", Modifier.size(20.dp))
                }
                
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Edit, "编辑", Modifier.size(20.dp))
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Delete, "删除",
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 添加HTML项对话框
 */
@Composable
private fun HtmlItemAddDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onAdd: (title: String, enableJs: Boolean, enableStorage: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(fileName.substringBeforeLast(".")) }
    var enableJs by remember { mutableStateOf(true) }
    var enableStorage by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加HTML页面") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "文件: $fileName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("页面标题") },
                    placeholder = { Text("在顶部显示的名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = enableJs,
                        onCheckedChange = { enableJs = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("启用 JavaScript")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = enableStorage,
                        onCheckedChange = { enableStorage = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("启用本地存储")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title, enableJs, enableStorage) }) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 编辑HTML项对话框
 */
@Composable
private fun HtmlItemEditDialog(
    item: GalleryItem,
    onDismiss: () -> Unit,
    onSave: (GalleryItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var enableJs by remember { mutableStateOf(item.htmlConfig?.enableJavaScript ?: true) }
    var enableStorage by remember { mutableStateOf(item.htmlConfig?.enableLocalStorage ?: true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑HTML页面") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "文件: ${item.htmlConfig?.entryFile ?: "index.html"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("页面标题") },
                    placeholder = { Text("在顶部显示的名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = enableJs,
                        onCheckedChange = { enableJs = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("启用 JavaScript")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = enableStorage,
                        onCheckedChange = { enableStorage = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("启用本地存储")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(item.copy(
                        title = title,
                        htmlConfig = HtmlItemConfig(
                            entryFile = item.htmlConfig?.entryFile ?: "index.html",
                            enableJavaScript = enableJs,
                            enableLocalStorage = enableStorage
                        )
                    ))
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 从Uri获取文件名
 */
private fun getFileName(context: android.content.Context, uri: Uri): String? {
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
private fun copyUriToTempFile(
    context: android.content.Context,
    uri: Uri,
    fileName: String?
): File? {
    return try {
        val tempDir = File(context.cacheDir, "html_gallery_temp").apply { mkdirs() }
        val targetFile = File(tempDir, fileName ?: "file_${System.currentTimeMillis()}.html")
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
