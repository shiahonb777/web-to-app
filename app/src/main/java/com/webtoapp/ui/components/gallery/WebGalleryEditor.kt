package com.webtoapp.ui.components.gallery

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.WebItemConfig

/**
 * 网址画廊编辑器 - 用于添加和管理多个网址
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebGalleryEditor(
    items: List<GalleryItem>,
    onItemsChange: (List<GalleryItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<GalleryItem?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }
    
    Column(modifier = modifier) {
        // 标题和添加按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "网址列表 (${items.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加网址")
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
                    .clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Language,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击添加网址",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "可添加多个网址，滑动切换",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // 网址列表
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    WebItemCard(
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
    if (showAddDialog) {
        WebItemAddDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, url, desktopMode ->
                val newItem = GalleryItem(
                    title = title,
                    type = GalleryItemType.WEB,
                    path = url,
                    sortOrder = items.size,
                    webConfig = WebItemConfig(desktopMode = desktopMode)
                )
                onItemsChange(items + newItem)
                showAddDialog = false
            }
        )
    }
    
    // 编辑对话框
    if (editingItem != null) {
        WebItemEditDialog(
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
 * 单个网址项卡片
 */
@Composable
private fun WebItemCard(
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
                    Icons.Outlined.Language,
                    null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // 标题和URL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "未命名" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 桌面模式标签
                if (item.webConfig?.desktopMode == true) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "桌面模式",
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
 * 添加网址对话框
 */
@Composable
private fun WebItemAddDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, url: String, desktopMode: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var desktopMode by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加网址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    placeholder = { Text("在顶部显示的名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("网址") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = desktopMode,
                        onCheckedChange = { desktopMode = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("桌面模式", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "以电脑版网页加载",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, url, desktopMode) },
                enabled = url.isNotBlank()
            ) {
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
 * 编辑网址对话框
 */
@Composable
private fun WebItemEditDialog(
    item: GalleryItem,
    onDismiss: () -> Unit,
    onSave: (GalleryItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var url by remember { mutableStateOf(item.path) }
    var desktopMode by remember { mutableStateOf(item.webConfig?.desktopMode ?: false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑网址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    placeholder = { Text("在顶部显示的名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("网址") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = desktopMode,
                        onCheckedChange = { desktopMode = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("桌面模式", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "以电脑版网页加载",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(item.copy(
                        title = title,
                        path = url,
                        webConfig = WebItemConfig(desktopMode = desktopMode)
                    ))
                },
                enabled = url.isNotBlank()
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
