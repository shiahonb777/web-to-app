package com.webtoapp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.IconLibraryItem
import com.webtoapp.util.IconLibraryStorage
import kotlinx.coroutines.launch

/**
 * 图标库对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconLibraryDialog(
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit,  // 返回图标文件路径
    onOpenAiGenerator: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val icons by IconLibraryStorage.iconsFlow.collectAsState(initial = emptyList())
    
    // Initialize图标库
    LaunchedEffect(Unit) {
        IconLibraryStorage.initialize(context)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Collections,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.iconLibrary, style = MaterialTheme.typography.headlineSmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, Strings.closeDialog)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    Strings.selectIconOrGenerate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // AI 生成按钮
                OutlinedCard(
                    onClick = {
                        onDismiss()
                        onOpenAiGenerator()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.aiGenerateIcon,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                Strings.useAiToGenerateIcon,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Icon网格
                if (icons.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Collections,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Strings.iconLibraryEmpty,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                Strings.iconLibraryEmptyHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Text(
                        "${Strings.savedIcons} (${icons.size})",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(icons) { icon ->
                            IconGridItem(
                                icon = icon,
                                onClick = { onSelectIcon(icon.path) },
                                onDelete = {
                                    scope.launch {
                                        IconLibraryStorage.delete(context, icon)
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.btnCancel)
                    }
                }
            }
        }
    }
}

/**
 * 图标网格项
 */
@Composable
private fun IconGridItem(
    icon: IconLibraryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = icon.path,
            contentDescription = icon.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // AI生成标记
        if (icon.isAiGenerated) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Delete按钮
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                Strings.deleteAction,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
    
    // Delete确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(Strings.deleteIcon) },
            text = { Text(Strings.deleteIconConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text(Strings.deleteAction, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
}
