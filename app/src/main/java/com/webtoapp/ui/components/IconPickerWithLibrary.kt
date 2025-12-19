package com.webtoapp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

/**
 * 带图标库功能的图标选择器
 * 
 * @param iconUri 当前选中的图标 Uri（来自相册选择）
 * @param iconPath 当前选中的图标路径（来自图标库）
 * @param onSelectFromGallery 从相册选择图标的回调
 * @param onSelectFromLibrary 从图标库选择图标的回调（返回文件路径）
 */
@Composable
fun IconPickerWithLibrary(
    iconUri: Uri? = null,
    iconPath: String? = null,
    onSelectFromGallery: () -> Unit,
    onSelectFromLibrary: (String) -> Unit
) {
    val context = LocalContext.current
    var showLibraryDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    
    // 判断是否有图标
    val hasIcon = iconUri != null || iconPath != null
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标预览
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = 2.dp,
                    color = if (hasIcon) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { onSelectFromGallery() },
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            when {
                iconUri != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(iconUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "应用图标",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                iconPath != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(iconPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = "应用图标",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "选择图标",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "应用图标",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "点击选择或使用下方按钮",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 功能按钮行
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 图标库按钮
                FilledTonalButton(
                    onClick = { showLibraryDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Collections,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("图标库", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    
    // 图标库对话框
    if (showLibraryDialog) {
        IconLibraryDialog(
            onDismiss = { showLibraryDialog = false },
            onSelectIcon = { path ->
                onSelectFromLibrary(path)
                showLibraryDialog = false
            },
            onOpenAiGenerator = { showAiGeneratorDialog = true }
        )
    }
    
    // AI 生成对话框
    if (showAiGeneratorDialog) {
        IconGeneratorDialog(
            onDismiss = { showAiGeneratorDialog = false },
            onIconGenerated = { path ->
                onSelectFromLibrary(path)
                showAiGeneratorDialog = false
            }
        )
    }
}
