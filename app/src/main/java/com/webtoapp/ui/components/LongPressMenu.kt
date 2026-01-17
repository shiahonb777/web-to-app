package com.webtoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.webview.LongPressHandler

/**
 * 长按菜单 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPressMenuSheet(
    result: LongPressHandler.LongPressResult,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onSaveImage: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            when (result) {
                is LongPressHandler.LongPressResult.Image -> {
                    ImageMenuContent(
                        imageUrl = result.url,
                        onSaveImage = { onSaveImage(result.url) },
                        onCopyLink = { onCopyLink(result.url) },
                        onDismiss = onDismiss
                    )
                }
                
                is LongPressHandler.LongPressResult.Video -> {
                    VideoMenuContent(
                        videoUrl = result.url,
                        onDownload = { onDownloadVideo(result.url) },
                        onCopyLink = { onCopyLink(result.url) },
                        onDismiss = onDismiss
                    )
                }
                
                is LongPressHandler.LongPressResult.Link -> {
                    LinkMenuContent(
                        linkUrl = result.url,
                        linkTitle = result.title,
                        onCopyLink = { onCopyLink(result.url) },
                        onOpenInBrowser = { onOpenInBrowser(result.url) },
                        onDismiss = onDismiss
                    )
                }
                
                is LongPressHandler.LongPressResult.ImageLink -> {
                    ImageLinkMenuContent(
                        imageUrl = result.imageUrl,
                        linkUrl = result.linkUrl,
                        onSaveImage = { onSaveImage(result.imageUrl) },
                        onCopyImageLink = { onCopyLink(result.imageUrl) },
                        onCopyLink = { onCopyLink(result.linkUrl) },
                        onOpenInBrowser = { onOpenInBrowser(result.linkUrl) },
                        onDismiss = onDismiss
                    )
                }
                
                else -> {
                    // 不应该到这里
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun ImageMenuContent(
    imageUrl: String,
    onSaveImage: () -> Unit,
    onCopyLink: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 图片预览
        if (!imageUrl.startsWith("blob:")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "图片预览",
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // URL 显示
        Text(
            text = imageUrl.take(100) + if (imageUrl.length > 100) "..." else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        
        // 操作按钮
        MenuItemButton(
            icon = Icons.Default.Download,
            text = "保存图片",
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = "复制图片链接",
            onClick = {
                onCopyLink()
                onDismiss()
            }
        )
    }
}

@Composable
private fun VideoMenuContent(
    videoUrl: String,
    onDownload: () -> Unit,
    onCopyLink: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.VideoFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "视频",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        // URL 显示
        Text(
            text = videoUrl.take(100) + if (videoUrl.length > 100) "..." else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        
        // 操作按钮
        MenuItemButton(
            icon = Icons.Default.Download,
            text = "下载视频",
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = "复制视频链接",
            onClick = {
                onCopyLink()
                onDismiss()
            }
        )
    }
}

@Composable
private fun LinkMenuContent(
    linkUrl: String,
    linkTitle: String?,
    onCopyLink: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                if (!linkTitle.isNullOrBlank()) {
                    Text(
                        text = linkTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = linkUrl.take(80) + if (linkUrl.length > 80) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Divider()
        
        // 操作按钮
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = "复制链接",
            onClick = {
                onCopyLink()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.OpenInBrowser,
            text = "在浏览器中打开",
            onClick = {
                onOpenInBrowser()
                onDismiss()
            }
        )
    }
}

@Composable
private fun ImageLinkMenuContent(
    imageUrl: String,
    linkUrl: String,
    onSaveImage: () -> Unit,
    onCopyImageLink: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 图片预览
        if (!imageUrl.startsWith("blob:")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "图片预览",
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Divider()
        
        // 图片操作
        Text(
            text = "图片",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        
        MenuItemButton(
            icon = Icons.Default.Download,
            text = "保存图片",
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = "复制图片链接",
            onClick = {
                onCopyImageLink()
                onDismiss()
            }
        )
        
        // 链接操作（如果有�?
        if (linkUrl.isNotBlank()) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "链接",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            
            MenuItemButton(
                icon = Icons.Default.ContentCopy,
                text = "复制链接地址",
                onClick = {
                    onCopyLink()
                    onDismiss()
                }
            )
            
            MenuItemButton(
                icon = Icons.Default.OpenInBrowser,
                text = "在浏览器中打开",
                onClick = {
                    onOpenInBrowser()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun MenuItemButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
