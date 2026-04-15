package com.webtoapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.AppCategory
import com.webtoapp.data.model.WebApp

/**
 * label
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
        // "all" label
        item {
            PremiumFilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(AppStringsProvider.current().allApps) },
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
        
        // " " label
        item {
            PremiumFilterChip(
                selected = selectedCategoryId == -1L,
                onClick = { onCategorySelected(-1L) },
                label = { Text(AppStringsProvider.current().uncategorized) },
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
        
        // User( supportlong- press /delete)
        items(categories, key = { it.id }) { category ->
            Box {
                PremiumFilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(com.webtoapp.util.SvgIconMapper.getIcon(category.icon), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
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
                    }
                )
                // handle long- press( FilterChip intercept, combinedClickable long- press)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(MaterialTheme.shapes.small)
                        .combinedClickable(
                            onClick = { onCategorySelected(category.id) },
                            onLongClick = { showCategoryMenu = category }
                        )
                )
                
                // long- press: / delete
                DropdownMenu(
                    expanded = showCategoryMenu == category,
                    onDismissRequest = { showCategoryMenu = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(AppStringsProvider.current().editCategory) },
                        onClick = {
                            showCategoryMenu = null
                            onEditCategory(category)
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStringsProvider.current().deleteCategory) },
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
        
        // button
        item {
            AssistChip(
                onClick = onAddCategory,
                label = { Text(AppStringsProvider.current().addCategory) },
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
    
    // Delete dialog
    showDeleteConfirm?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(AppStringsProvider.current().deleteCategory) },
            text = { Text(AppStringsProvider.current().deleteCategoryConfirm) },
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
                    Text(AppStringsProvider.current().btnDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(AppStringsProvider.current().btnCancel)
                }
            }
        )
    }
}

/**
 * editdialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorDialog(
    category: AppCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, color: String) -> Unit
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var icon by remember(category) { mutableStateOf(category?.icon ?: "folder") }
    var selectedColor by remember(category) { 
        val hexColor = category?.color ?: "#6200EE"
        // map color Long, ensurewith color
        val parsedColor = android.graphics.Color.parseColor(hexColor)
        mutableLongStateOf((parsedColor.toLong() and 0xFFFFFFFFL) or 0xFF000000L)
    }
    
    // color
    val presetColors = listOf(
        0xFF6200EE, 0xFF3700B3, 0xFF03DAC6, 0xFF018786,
        0xFFBB86FC, 0xFF6200EA, 0xFFFF6D00, 0xFFFFAB00,
        0xFF00C853, 0xFF64DD17, 0xFFFF1744, 0xFFD50000,
        0xFF2979FF, 0xFF304FFE, 0xFFFF4081, 0xFFC51162
    )
    
    // icon
    val presetIcons = listOf(
        "folder", "folder_open", "phone_android", "computer", "gaming", "music_note", "movie", "menu_book",
        "newspaper", "work", "shopping_bag", "heart", "star", "fire", "lightbulb", "auto_awesome",
        "home", "directions_car", "flight", "directions_boat", "public", "palette", "dark_mode", "light_mode"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (category == null) AppStringsProvider.current().addCategory else AppStringsProvider.current().editCategory
            ) 
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nameinput
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStringsProvider.current().categoryName) },
                    placeholder = { Text(AppStringsProvider.current().categoryNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Iconselect
                Text(AppStringsProvider.current().categoryIcon, style = MaterialTheme.typography.labelMedium)
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
                                        Icon(
                                            com.webtoapp.util.SvgIconMapper.getIcon(presetIcon),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (icon == presetIcon)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Colorselect
                Text(AppStringsProvider.current().categoryColor, style = MaterialTheme.typography.labelMedium)
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
            PremiumButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val colorHex = String.format(java.util.Locale.getDefault(), "#%06X", (0xFFFFFF and selectedColor.toInt()))
                        onSave(name, icon, colorHex)
                    }
                },
                enabled = name.isNotBlank()
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

/**
 * Move to categorydialog
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
        title = { Text(AppStringsProvider.current().moveToCategory) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Note
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
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(AppStringsProvider.current().uncategorized)
                        Spacer(Modifier.weight(weight = 1f, fill = true))
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
                
                // User list
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
                            Icon(com.webtoapp.util.SvgIconMapper.getIcon(category.icon), contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(category.name)
                            Spacer(Modifier.weight(weight = 1f, fill = true))
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
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}
