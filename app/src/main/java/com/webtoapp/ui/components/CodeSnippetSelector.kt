package com.webtoapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.extension.CodeSnippet
import com.webtoapp.core.extension.CodeSnippetCategory
import com.webtoapp.core.extension.CodeSnippets

/**
 * 代码块选择器对话框
 * 
 * 提供分类浏览、搜索、预览和插入代码块的功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeSnippetSelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (CodeSnippet) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CodeSnippetCategory?>(null) }
    var previewSnippet by remember { mutableStateOf<CodeSnippet?>(null) }
    
    val allCategories = remember { CodeSnippets.getAll() }
    val popularSnippets = remember { CodeSnippets.getPopular() }
    
    // Search结果
    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else CodeSnippets.search(searchQuery)
    }
    
    // 当前显示的代码块
    val displaySnippets = when {
        searchQuery.isNotBlank() -> searchResults
        selectedCategory != null -> selectedCategory!!.snippets
        else -> popularSnippets
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // 标题栏
                TopAppBar(
                    title = { Text(com.webtoapp.core.i18n.Strings.codeBlockLibraryTitle) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, com.webtoapp.core.i18n.Strings.close)
                        }
                    },
                    actions = {
                        // 统计信息
                        Text(
                            "${allCategories.size} ${com.webtoapp.core.i18n.Strings.categoriesAndBlocks.split("·")[0].trim()} · ${allCategories.sumOf { it.snippets.size }} ${com.webtoapp.core.i18n.Strings.categoriesAndBlocks.split("·")[1].trim()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Search框
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            if (it.isNotBlank()) selectedCategory = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(com.webtoapp.core.i18n.Strings.searchCodeBlocksPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, com.webtoapp.core.i18n.Strings.clear)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 分类标签
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 热门
                        FilterChip(
                            selected = selectedCategory == null && searchQuery.isBlank(),
                            onClick = { 
                                selectedCategory = null
                                searchQuery = ""
                            },
                            label = { Text(com.webtoapp.core.i18n.Strings.hotTag) },
                            leadingIcon = if (selectedCategory == null && searchQuery.isBlank()) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                        
                        // 分类
                        allCategories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { 
                                    selectedCategory = if (selectedCategory == category) null else category
                                    searchQuery = ""
                                },
                                label = { Text("${category.icon} ${category.name}") },
                                leadingIcon = if (selectedCategory == category) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 当前分类描述
                    selectedCategory?.let { category ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${category.description} · ${com.webtoapp.core.i18n.Strings.codeBlocksCount.format(category.snippets.size)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Search结果提示
                    if (searchQuery.isNotBlank()) {
                        Text(
                            com.webtoapp.core.i18n.Strings.foundResults.format(searchResults.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // 代码块列表
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displaySnippets) { snippet ->
                            CodeSnippetItem(
                                snippet = snippet,
                                onClick = { previewSnippet = snippet },
                                onInsert = {
                                    onSelect(snippet)
                                    onDismiss()
                                }
                            )
                        }
                        
                        if (displaySnippets.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.SearchOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            com.webtoapp.core.i18n.Strings.noMatchingCodeBlocks,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
    
    // 代码预览对话框
    previewSnippet?.let { snippet ->
        CodeSnippetPreviewDialog(
            snippet = snippet,
            onDismiss = { previewSnippet = null },
            onInsert = {
                onSelect(snippet)
                onDismiss()
            }
        )
    }
}

/**
 * 代码块列表项
 */
@Composable
private fun CodeSnippetItem(
    snippet: CodeSnippet,
    onClick: () -> Unit,
    onInsert: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    snippet.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    snippet.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 标签
                if (snippet.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        snippet.tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            // 插入按钮
            FilledTonalIconButton(
                onClick = onInsert,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = com.webtoapp.core.i18n.Strings.insert,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


/**
 * 代码块预览对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeSnippetPreviewDialog(
    snippet: CodeSnippet,
    onDismiss: () -> Unit,
    onInsert: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // 标题栏
                TopAppBar(
                    title = { 
                        Column {
                            Text(snippet.name)
                            Text(
                                snippet.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, com.webtoapp.core.i18n.Strings.back)
                        }
                    }
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 标签
                    if (snippet.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            snippet.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        "#$tag",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // 代码预览
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column {
                            // 代码头部
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "JavaScript",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                TextButton(
                                    onClick = { 
                                        // TODO: 复制到剪贴板
                                        copied = true
                                    }
                                ) {
                                    Icon(
                                        if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (copied) com.webtoapp.core.i18n.Strings.copied else com.webtoapp.core.i18n.Strings.copy)
                                }
                            }
                            
                            Divider()
                            
                            // 代码内容
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                item {
                                    Text(
                                        snippet.code,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 20.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(com.webtoapp.core.i18n.Strings.btnCancel)
                        }
                        
                        Button(
                            onClick = onInsert,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(com.webtoapp.core.i18n.Strings.insertCode)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 代码块快速选择卡片
 * 用于模块编辑器中快速插入代码
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeSnippetQuickPicker(
    onSelect: (CodeSnippet) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFullSelector by remember { mutableStateOf(false) }
    val popularSnippets = remember { CodeSnippets.getPopular() }
    
    EnhancedElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        com.webtoapp.core.i18n.Strings.codeBlockLibraryTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                TextButton(onClick = { showFullSelector = true }) {
                    Text(com.webtoapp.core.i18n.Strings.browseAll)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Text(
                com.webtoapp.core.i18n.Strings.quickInsertCodeSnippets,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 热门代码块
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularSnippets.forEach { snippet ->
                    SuggestionChip(
                        onClick = { onSelect(snippet) },
                        label = { Text(snippet.name, maxLines = 1) },
                        icon = {
                            Icon(
                                Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
    
    // 完整选择器对话框
    if (showFullSelector) {
        CodeSnippetSelectorDialog(
            onDismiss = { showFullSelector = false },
            onSelect = onSelect
        )
    }
}

/**
 * 分类代码块网格
 * 按分类展示代码块
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeSnippetCategoryGrid(
    onSelect: (CodeSnippet) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { CodeSnippets.getAll() }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isExpanded = expandedCategory == category.id
            
            EnhancedElevatedCard(
                onClick = { 
                    expandedCategory = if (isExpanded) null else category.id
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.icon, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    com.webtoapp.core.i18n.Strings.codeBlocksCount.format(category.snippets.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                category.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Divider()
                            
                            category.snippets.forEach { snippet ->
                                Surface(
                                    onClick = { onSelect(snippet) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                snippet.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                snippet.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = com.webtoapp.core.i18n.Strings.insert,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
