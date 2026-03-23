package com.webtoapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AppCategory
import com.webtoapp.data.model.WebApp

/**
 * 分类标签栏
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabRow(
    categories: List<AppCategory>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (AppCategory) -> Unit,
    onDeleteCategory: (AppCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCategoryMenu by remember { mutableStateOf<AppCategory?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<AppCategory?>(null) }
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "全部" 标签
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(Strings.allApps) },
                leadingIcon = {
                    if (selectedCategoryId == null) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
        
        // "未分类" 标签
        item {
            FilterChip(
                selected = selectedCategoryId == -1L,
                onClick = { onCategorySelected(-1L) },
                label = { Text(Strings.uncategorized) },
                leadingIcon = {
                    if (selectedCategoryId == -1L) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
        
        // User分类
        items(categories, key = { it.id }) { category ->
            Box {
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.icon)
                            Spacer(Modifier.width(4.dp))
                            Text(category.name)
                        }
                    },
                    leadingIcon = {
                        if (selectedCategoryId == category.id) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = { onCategorySelected(category.id) },
                        onLongClick = { showCategoryMenu = category }
                    )
                )
                
                // 分类操作菜单
                DropdownMenu(
                    expanded = showCategoryMenu == category,
                    onDismissRequest = { showCategoryMenu = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.editCategory) },
                        onClick = {
                            showCategoryMenu = null
                            onEditCategory(category)
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.deleteCategory) },
                        onClick = {
                            showCategoryMenu = null
                            showDeleteConfirm = category
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Outlined.Delete, 
                                null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        }
                    )
                }
            }
        }
        
        // 添加分类按钮
        item {
            AssistChip(
                onClick = onAddCategory,
                label = { Text(Strings.addCategory) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
    
    // Delete确认对话框
    showDeleteConfirm?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(Strings.deleteCategory) },
            text = { Text(Strings.deleteCategoryConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(category)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.btnDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
}

/**
 * 分类编辑对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorDialog(
    category: AppCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, color: String) -> Unit
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var icon by remember(category) { mutableStateOf(category?.icon ?: "📁") }
    var selectedColor by remember(category) { 
        val hexColor = category?.color ?: "#6200EE"
        // 将解析的颜色转换为无符号 Long，确保与预设颜色一致
        val parsedColor = android.graphics.Color.parseColor(hexColor)
        mutableStateOf((parsedColor.toLong() and 0xFFFFFFFFL) or 0xFF000000L)
    }
    
    // 预设颜色
    val presetColors = listOf(
        0xFF6200EE, 0xFF3700B3, 0xFF03DAC6, 0xFF018786,
        0xFFBB86FC, 0xFF6200EA, 0xFFFF6D00, 0xFFFFAB00,
        0xFF00C853, 0xFF64DD17, 0xFFFF1744, 0xFFD50000,
        0xFF2979FF, 0xFF304FFE, 0xFFFF4081, 0xFFC51162
    )
    
    // 预设图标
    val presetIcons = listOf(
        "📁", "📂", "📱", "💻", "🎮", "🎵", "🎥", "📚",
        "📰", "💼", "🛍️", "❤️", "⭐", "🔥", "💡", "🌟",
        "🏠", "🚗", "✈️", "🚢", "🌍", "🌈", "🌙", "☀️"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (category == null) Strings.addCategory else Strings.editCategory
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name输入
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.categoryName) },
                    placeholder = { Text(Strings.categoryNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Icon选择
                Text(Strings.categoryIcon, style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetIcons.chunked(8).forEach { rowIcons ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowIcons.forEach { presetIcon ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (icon == presetIcon) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { icon = presetIcon }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(presetIcon, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Color选择
                Text(Strings.categoryColor, style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetColors.chunked(8).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowColors.forEach { presetColor ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = androidx.compose.ui.graphics.Color(presetColor),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { selectedColor = presetColor }
                                ) {
                                    if (selectedColor == presetColor) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = androidx.compose.ui.graphics.Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val colorHex = String.format("#%06X", (0xFFFFFF and selectedColor.toInt()))
                        onSave(name, icon, colorHex)
                    }
                },
                enabled = name.isNotBlank()
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

/**
 * Move to category对话框
 */
@Composable
fun MoveToCategoryDialog(
    app: WebApp,
    categories: List<AppCategory>,
    onDismiss: () -> Unit,
    onMoveToCategory: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.moveToCategory) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // "未分类" 选项
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (app.categoryId == null) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMoveToCategory(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📂", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(12.dp))
                        Text(Strings.uncategorized)
                        Spacer(Modifier.weight(1f))
                        if (app.categoryId == null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // User分类列表
                categories.forEach { category ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (app.categoryId == category.id) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMoveToCategory(category.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.icon, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            Text(category.name)
                            Spacer(Modifier.weight(1f))
                            if (app.categoryId == category.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
