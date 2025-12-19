package com.webtoapp.ui.components.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.MediaItemConfig

/**
 * 画廊项目编辑器 - 用于添加和管理多个媒体项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryItemEditor(
    items: List<GalleryItem>,
    itemType: GalleryItemType,
    onItemsChange: (List<GalleryItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<GalleryItem?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }
    
    // 多选文件选择器
    val multipleMediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapIndexed { index, uri ->
                GalleryItem(
                    title = "项目 ${items.size + index + 1}",
                    type = itemType,
                    path = uri.toString(),
                    sortOrder = items.size + index,
                    mediaConfig = if (itemType == GalleryItemType.VIDEO) {
                        MediaItemConfig()
                    } else null
                )
            }
            onItemsChange(items + newItems)
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
                text = "媒体列表 (${items.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            FilledTonalButton(
                onClick = {
                    val mimeType = when (itemType) {
                        GalleryItemType.IMAGE -> "image/*"
                        GalleryItemType.VIDEO -> "video/*"
                        else -> "*/*"
                    }
                    multipleMediaPicker.launch(mimeType)
                }
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加")
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
                    .clickable {
                        val mimeType = when (itemType) {
                            GalleryItemType.IMAGE -> "image/*"
                            GalleryItemType.VIDEO -> "video/*"
                            else -> "*/*"
                        }
                        multipleMediaPicker.launch(mimeType)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (itemType == GalleryItemType.IMAGE) Icons.Outlined.AddPhotoAlternate
                        else Icons.Outlined.VideoLibrary,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击添加${if (itemType == GalleryItemType.IMAGE) "图片" else "视频"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "支持多选",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // 项目列表
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    GalleryItemCard(
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
    
    // 编辑对话框
    if (editingItem != null) {
        GalleryItemEditDialog(
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
 * 单个画廊项目卡片
 */
@Composable
private fun GalleryItemCard(
    item: GalleryItem,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    val context = LocalContext.current
    
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
            
            // 缩略图
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.type == GalleryItemType.IMAGE) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.path)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.PlayCircle,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // 标题
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "未命名" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description != null) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 操作按钮
            Row {
                // 上移
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        "上移",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 下移
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        "下移",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 编辑
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        "编辑",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 删除
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        "删除",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 编辑画廊项目对话框
 */
@Composable
private fun GalleryItemEditDialog(
    item: GalleryItem,
    onDismiss: () -> Unit,
    onSave: (GalleryItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑项目") },
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    placeholder = { Text("简短描述") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(item.copy(
                        title = title,
                        description = description.ifBlank { null }
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
