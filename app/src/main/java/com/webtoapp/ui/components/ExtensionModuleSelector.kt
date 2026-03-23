package com.webtoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.Strings

/**
 * Extension module selector卡片
 * 
 * 用于在创建应用页面中选择要启用的扩展模块
 */
@Composable
fun ExtensionModuleSelectorCard(
    selectedModuleIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    val modules by extensionManager.modules.collectAsState()
    val builtInModules by extensionManager.builtInModules.collectAsState()
    
    var expanded by remember { mutableStateOf(false) }
    var showModuleDialog by remember { mutableStateOf(false) }
    
    val allModules = builtInModules + modules
    val enabledModules = allModules.filter { it.id in selectedModuleIds }
    
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.extensionModuleTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (enabledModules.isEmpty()) Strings.noModuleSelected
                            else Strings.modulesSelected.format(enabledModules.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = { showModuleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = Strings.addModule)
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) Strings.collapse else Strings.expand
                        )
                    }
                }
            }
            
            // 已选模块列表
            if (expanded && enabledModules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                enabledModules.forEach { module ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(module.icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    module.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    module.category.getDisplayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                onSelectionChange(selectedModuleIds - module.id)
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // 快速提示
            if (enabledModules.isEmpty() && expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Strings.extensionModuleHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Module选择对话框
    if (showModuleDialog) {
        ModuleSelectionDialog(
            allModules = allModules,
            selectedIds = selectedModuleIds,
            onSelectionChange = onSelectionChange,
            onDismiss = { showModuleDialog = false }
        )
    }
}

/**
 * 模块选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleSelectionDialog(
    allModules: List<ExtensionModule>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    
    // 直接计算过滤后的模块列表，确保列表更新时 UI 正确响应
    val filteredModules = allModules.filter { module ->
        val matchesSearch = searchQuery.isBlank() ||
            module.name.contains(searchQuery, ignoreCase = true) ||
            module.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || module.category == selectedCategory
        matchesSearch && matchesCategory
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.selectExtensionModules) },
        text = {
            Column {
                // Search框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(Strings.searchModulesPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 分类筛选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(Strings.filterAll, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = selectedCategory == ModuleCategory.CONTENT_FILTER,
                        onClick = { 
                            selectedCategory = if (selectedCategory == ModuleCategory.CONTENT_FILTER) null 
                                              else ModuleCategory.CONTENT_FILTER 
                        },
                        label = { Text(Strings.filterContent, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = selectedCategory == ModuleCategory.STYLE_MODIFIER,
                        onClick = { 
                            selectedCategory = if (selectedCategory == ModuleCategory.STYLE_MODIFIER) null 
                                              else ModuleCategory.STYLE_MODIFIER 
                        },
                        label = { Text(Strings.filterStyle, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = selectedCategory == ModuleCategory.FUNCTION_ENHANCE,
                        onClick = { 
                            selectedCategory = if (selectedCategory == ModuleCategory.FUNCTION_ENHANCE) null 
                                              else ModuleCategory.FUNCTION_ENHANCE 
                        },
                        label = { Text(Strings.filterFunction, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Module列表
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredModules) { module ->
                        val isSelected = module.id in selectedIds
                        
                        Surface(
                            onClick = {
                                onSelectionChange(
                                    if (isSelected) selectedIds - module.id
                                    else selectedIds + module.id
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else 
                                MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(module.icon, fontSize = 24.sp)
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            module.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (module.builtIn) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    Strings.builtIn,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                    if (module.description.isNotBlank()) {
                                        Text(
                                            module.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        onSelectionChange(
                                            if (it) selectedIds + module.id
                                            else selectedIds - module.id
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    if (filteredModules.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    Strings.noMatchingModules,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(Strings.done)
            }
        },
        dismissButton = {
            TextButton(onClick = { onSelectionChange(emptySet()) }) {
                Text(Strings.clearSelection)
            }
        }
    )
}

/**
 * 简化版模块选择器 - 用于快速选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickModuleSelector(
    selectedModuleIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    val builtInModules by extensionManager.builtInModules.collectAsState()
    
    // 只显示常用的内置模块
    val quickModules = builtInModules.take(5)
    
    Column(modifier = modifier) {
        Text(
            Strings.quickEnable,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickModules.forEach { module ->
                val isSelected = module.id in selectedModuleIds
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectionChange(
                            if (isSelected) selectedModuleIds - module.id
                            else selectedModuleIds + module.id
                        )
                    },
                    label = { Text(module.icon + " " + module.name.take(4)) }
                )
            }
        }
    }
}
