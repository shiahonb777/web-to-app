package com.webtoapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.extension.CodeSnippet
import com.webtoapp.core.extension.CodeSnippetCategory
import com.webtoapp.core.extension.CodeSnippets
import androidx.compose.ui.graphics.Color

/**
 * code select dialog
 * 
 * , , preview code
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
    
    // Search
    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else CodeSnippets.search(searchQuery)
    }
    
    // currentdisplay code
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
                // Note
                TopAppBar(
                    title = { Text(com.webtoapp.core.i18n.AppStringsProvider.current().codeBlockLibraryTitle) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, com.webtoapp.core.i18n.AppStringsProvider.current().close)
                        }
                    },
                    actions = {
                        // Note
                        Text(
                            "${allCategories.size} ${com.webtoapp.core.i18n.AppStringsProvider.current().categoriesAndBlocks.split("·")[0].trim()} · ${allCategories.sumOf { it.snippets.size }} ${com.webtoapp.core.i18n.AppStringsProvider.current().categoriesAndBlocks.split("·")[1].trim()}",
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
                    // Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            if (it.isNotBlank()) selectedCategory = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(com.webtoapp.core.i18n.AppStringsProvider.current().searchCodeBlocksPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, com.webtoapp.core.i18n.AppStringsProvider.current().clear)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Note
                        PremiumFilterChip(
                            selected = selectedCategory == null && searchQuery.isBlank(),
                            onClick = { 
                                selectedCategory = null
                                searchQuery = ""
                            },
                            label = { Text(com.webtoapp.core.i18n.AppStringsProvider.current().hotTag) },
                            leadingIcon = if (selectedCategory == null && searchQuery.isBlank()) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                        
                        // Note
                        allCategories.forEach { category ->
                            PremiumFilterChip(
                                selected = selectedCategory == category,
                                onClick = { 
                                    selectedCategory = if (selectedCategory == category) null else category
                                    searchQuery = ""
                                },
                                label = { Text(category.name) },
                                leadingIcon = if (selectedCategory == category) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // current
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
                                Icon(com.webtoapp.util.SvgIconMapper.getIcon(category.icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${category.description} · ${com.webtoapp.core.i18n.AppStringsProvider.current().codeBlocksCount.format(category.snippets.size)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Search hint
                    if (searchQuery.isNotBlank()) {
                        Text(
                            com.webtoapp.core.i18n.AppStringsProvider.current().foundResults.format(searchResults.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // code list
                    LazyColumn(
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displaySnippets, key = { it.id }) { snippet ->
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
                                            com.webtoapp.core.i18n.AppStringsProvider.current().noMatchingCodeBlocks,
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
    
    // codepreviewdialog
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
 * code list
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
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                
                // label
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
            
            // button
            FilledTonalIconButton(
                onClick = onInsert,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = com.webtoapp.core.i18n.AppStringsProvider.current().insert,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


/**
 * code previewdialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeSnippetPreviewDialog(
    snippet: CodeSnippet,
    onDismiss: () -> Unit,
    onInsert: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
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
                // Note
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, com.webtoapp.core.i18n.AppStringsProvider.current().back)
                        }
                    }
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // label
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
                    
                    // codepreview
                    Surface(
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column {
                            // codeheader
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
                                        clipboardManager.setText(AnnotatedString(snippet.code))
                                        copied = true
                                    }
                                ) {
                                    Icon(
                                        if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (copied) com.webtoapp.core.i18n.AppStringsProvider.current().copied else com.webtoapp.core.i18n.AppStringsProvider.current().copy)
                                }
                            }
                            
                            HorizontalDivider()
                            
                            // codecontent
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
                    
                    // button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        ) {
                            Text(com.webtoapp.core.i18n.AppStringsProvider.current().btnCancel)
                        }
                        
                        PremiumButton(
                            onClick = onInsert,
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(com.webtoapp.core.i18n.AppStringsProvider.current().insertCode)
                        }
                    }
                }
            }
        }
    }
}

/**
 * code selectcard
 * formoduleedit code
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        com.webtoapp.core.i18n.AppStringsProvider.current().codeBlockLibraryTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                TextButton(onClick = { showFullSelector = true }) {
                    Text(com.webtoapp.core.i18n.AppStringsProvider.current().browseAll)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Text(
                com.webtoapp.core.i18n.AppStringsProvider.current().quickInsertCodeSnippets,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // code
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
    
    // select dialog
    if (showFullSelector) {
        CodeSnippetSelectorDialog(
            onDismiss = { showFullSelector = false },
            onSelect = onSelect
        )
    }
}

/**
 * code
 * code
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
                            Icon(com.webtoapp.util.SvgIconMapper.getIcon(category.icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    com.webtoapp.core.i18n.AppStringsProvider.current().codeBlocksCount.format(category.snippets.size),
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
                            
                            HorizontalDivider()
                            
                            category.snippets.forEach { snippet ->
                                Surface(
                                    onClick = { onSelect(snippet) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                                            contentDescription = com.webtoapp.core.i18n.AppStringsProvider.current().insert,
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
