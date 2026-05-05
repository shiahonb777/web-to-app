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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AppCategory
import com.webtoapp.data.model.WebApp




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

        item {
            PremiumFilterChip(
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


        item {
            PremiumFilterChip(
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

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(MaterialTheme.shapes.small)
                        .combinedClickable(
                            onClick = { onCategorySelected(category.id) },
                            onLongClick = { showCategoryMenu = category }
                        )
                )


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




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorDialog(
    category: AppCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String) -> Unit
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var icon by remember(category) { mutableStateOf(category?.icon ?: "folder") }


    val presetIcons = listOf(
        "folder", "folder_open", "phone_android", "computer", "gaming", "music_note", "movie", "menu_book",
        "newspaper", "work", "shopping_bag", "heart", "star", "fire", "lightbulb", "auto_awesome",
        "home", "directions_car", "flight", "directions_boat", "public", "palette", "dark_mode", "light_mode"
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
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.categoryName) },
                    placeholder = { Text(Strings.categoryNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )


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
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, icon)
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




@Composable
fun MoveToCategoryDialog(
    appName: String,
    currentCategoryId: Long?,
    categories: List<AppCategory>,
    onDismiss: () -> Unit,
    onMoveToCategory: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.moveToCategory) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))


                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (currentCategoryId == null)
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
                        Text(Strings.uncategorized)
                        Spacer(Modifier.weight(weight = 1f, fill = true))
                        if (currentCategoryId == null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }


                categories.forEach { category ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (currentCategoryId == category.id)
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
                            if (currentCategoryId == category.id) {
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
