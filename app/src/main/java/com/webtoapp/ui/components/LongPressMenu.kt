package com.webtoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.webview.LongPressHandler
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
        // Image预览
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
                    contentDescription = Strings.longPressMenuImagePreview,
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
            text = Strings.longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = Strings.longPressMenuCopyImageLink,
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
                text = Strings.longPressMenuVideo,
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
            text = Strings.longPressMenuDownloadVideo,
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = Strings.longPressMenuCopyVideoLink,
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
            text = Strings.longPressMenuCopyLink,
            onClick = {
                onCopyLink()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.OpenInBrowser,
            text = Strings.longPressMenuOpenInBrowser,
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
        // Image预览
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
                    contentDescription = Strings.longPressMenuImagePreview,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Divider()
        
        // Image操作
        Text(
            text = Strings.longPressMenuImage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        
        MenuItemButton(
            icon = Icons.Default.Download,
            text = Strings.longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = Strings.longPressMenuCopyImageLink,
            onClick = {
                onCopyImageLink()
                onDismiss()
            }
        )
        
        // 链接操作（如果有�?
        if (linkUrl.isNotBlank()) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = Strings.longPressMenuLink,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            
            MenuItemButton(
                icon = Icons.Default.ContentCopy,
                text = Strings.longPressMenuCopyLinkAddress,
                onClick = {
                    onCopyLink()
                    onDismiss()
                }
            )
            
            MenuItemButton(
                icon = Icons.Default.OpenInBrowser,
                text = Strings.longPressMenuOpenInBrowser,
                onClick = {
                    onOpenInBrowser()
                    onDismiss()
                }
            )
        }
    }
}

/**
 * 简洁版长按菜单 BottomSheet
 * 仅支持保存图片和复制链接
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleLongPressMenuSheet(
    result: LongPressHandler.LongPressResult,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onSaveImage: (String) -> Unit
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
                    SimpleImageMenuContent(
                        imageUrl = result.url,
                        onSaveImage = { onSaveImage(result.url) },
                        onCopyLink = { onCopyLink(result.url) },
                        onDismiss = onDismiss
                    )
                }
                
                is LongPressHandler.LongPressResult.Video -> {
                    // 简洁模式不支持视频下载，仅复制链接
                    SimpleLinkOnlyContent(
                        url = result.url,
                        title = Strings.longPressMenuVideo,
                        icon = Icons.Default.VideoFile,
                        onCopyLink = { onCopyLink(result.url) },
                        onDismiss = onDismiss
                    )
                }
                
                is LongPressHandler.LongPressResult.Link -> {
                    SimpleLinkOnlyContent(
                        url = result.url,
                        title = result.title,
                        icon = Icons.Default.Link,
                        onCopyLink = { onCopyLink(result.url) },
                        onDismiss = onDismiss
                    )
                }
                
                is LongPressHandler.LongPressResult.ImageLink -> {
                    SimpleImageLinkMenuContent(
                        imageUrl = result.imageUrl,
                        linkUrl = result.linkUrl,
                        onSaveImage = { onSaveImage(result.imageUrl) },
                        onCopyImageLink = { onCopyLink(result.imageUrl) },
                        onCopyLink = { onCopyLink(result.linkUrl) },
                        onDismiss = onDismiss
                    )
                }
                
                else -> {
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SimpleImageMenuContent(
    imageUrl: String,
    onSaveImage: () -> Unit,
    onCopyLink: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Image预览
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
                    contentDescription = Strings.longPressMenuImagePreview,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Divider()
        
        // 操作按钮（简洁版仅保存图片和复制链接）
        MenuItemButton(
            icon = Icons.Default.Download,
            text = Strings.longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = Strings.longPressMenuCopyImageLink,
            onClick = {
                onCopyLink()
                onDismiss()
            }
        )
    }
}

@Composable
private fun SimpleLinkOnlyContent(
    url: String,
    title: String?,
    icon: ImageVector,
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
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = url.take(80) + if (url.length > 80) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Divider()
        
        // 简洁版仅复制链接
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = Strings.longPressMenuCopyLink,
            onClick = {
                onCopyLink()
                onDismiss()
            }
        )
    }
}

@Composable
private fun SimpleImageLinkMenuContent(
    imageUrl: String,
    linkUrl: String,
    onSaveImage: () -> Unit,
    onCopyImageLink: () -> Unit,
    onCopyLink: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Image预览
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
                    contentDescription = Strings.longPressMenuImagePreview,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Divider()
        
        // Image操作
        Text(
            text = Strings.longPressMenuImage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        
        MenuItemButton(
            icon = Icons.Default.Download,
            text = Strings.longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = Strings.longPressMenuCopyImageLink,
            onClick = {
                onCopyImageLink()
                onDismiss()
            }
        )
        
        // 链接操作（简洁版仅复制链接）
        if (linkUrl.isNotBlank()) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = Strings.longPressMenuLink,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            
            MenuItemButton(
                icon = Icons.Default.ContentCopy,
                text = Strings.longPressMenuCopyLinkAddress,
                onClick = {
                    onCopyLink()
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

// ==================== iOS 风格菜单 ====================

/**
 * iOS 风格长按菜单 - 毛玻璃背景，类似 iPhone 体验
 */
@Composable
fun IosStyleLongPressMenu(
    result: LongPressHandler.LongPressResult,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onSaveImage: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    // 动画状态
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "alpha"
    )
    
    LaunchedEffect(Unit) { visible = true }
    
    // Fullscreen遮罩层
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f * alpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // iOS 风格卡片
        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(24.dp)
                .scale(scale)
                .alpha(alpha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* 阻止点击穿透 */ },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column {
                // 根据类型显示不同内容
                when (result) {
                    is LongPressHandler.LongPressResult.Image -> {
                        IosImageMenuContent(
                            imageUrl = result.url,
                            onSaveImage = { onSaveImage(result.url); onDismiss() },
                            onCopyLink = { onCopyLink(result.url); onDismiss() }
                        )
                    }
                    is LongPressHandler.LongPressResult.Video -> {
                        IosVideoMenuContent(
                            onDownload = { onDownloadVideo(result.url); onDismiss() },
                            onCopyLink = { onCopyLink(result.url); onDismiss() }
                        )
                    }
                    is LongPressHandler.LongPressResult.Link -> {
                        IosLinkMenuContent(
                            linkUrl = result.url,
                            linkTitle = result.title,
                            onCopyLink = { onCopyLink(result.url); onDismiss() },
                            onOpenInBrowser = { onOpenInBrowser(result.url); onDismiss() }
                        )
                    }
                    is LongPressHandler.LongPressResult.ImageLink -> {
                        IosImageLinkMenuContent(
                            imageUrl = result.imageUrl,
                            onSaveImage = { onSaveImage(result.imageUrl); onDismiss() },
                            onCopyImageLink = { onCopyLink(result.imageUrl); onDismiss() },
                            onCopyLink = { onCopyLink(result.linkUrl); onDismiss() },
                            onOpenInBrowser = { onOpenInBrowser(result.linkUrl); onDismiss() }
                        )
                    }
                    else -> onDismiss()
                }
            }
        }
    }
}

@Composable
private fun IosImageMenuContent(
    imageUrl: String,
    onSaveImage: () -> Unit,
    onCopyLink: () -> Unit
) {
    val context = LocalContext.current
    Column {
        // Image预览
        if (!imageUrl.startsWith("blob:")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        IosMenuItem(Icons.Default.Download, Strings.longPressMenuSaveImage, onSaveImage)
        IosDivider()
        IosMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyImageLink, onCopyLink, isLast = true)
    }
}

@Composable
private fun IosVideoMenuContent(
    onDownload: () -> Unit,
    onCopyLink: () -> Unit
) {
    Column {
        // Video图标头部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VideoFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        IosMenuItem(Icons.Default.Download, Strings.longPressMenuDownloadVideo, onDownload)
        IosDivider()
        IosMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyVideoLink, onCopyLink, isLast = true)
    }
}

@Composable
private fun IosLinkMenuContent(
    linkUrl: String,
    linkTitle: String?,
    onCopyLink: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    Column {
        // 链接信息头部
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            if (!linkTitle.isNullOrBlank()) {
                Text(
                    text = linkTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = linkUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IosMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyLink, onCopyLink)
        IosDivider()
        IosMenuItem(Icons.Default.OpenInBrowser, Strings.longPressMenuOpenInBrowser, onOpenInBrowser, isLast = true)
    }
}

@Composable
private fun IosImageLinkMenuContent(
    imageUrl: String,
    onSaveImage: () -> Unit,
    onCopyImageLink: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    val context = LocalContext.current
    Column {
        // Image预览
        if (!imageUrl.startsWith("blob:")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        IosMenuItem(Icons.Default.Download, Strings.longPressMenuSaveImage, onSaveImage)
        IosDivider()
        IosMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyImageLink, onCopyImageLink)
        IosDivider()
        IosMenuItem(Icons.Default.Link, Strings.longPressMenuCopyLink, onCopyLink)
        IosDivider()
        IosMenuItem(Icons.Default.OpenInBrowser, Strings.longPressMenuOpenInBrowser, onOpenInBrowser, isLast = true)
    }
}

@Composable
private fun IosMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") isLast: Boolean = false,
    destructive: Boolean = false
) {
    val textColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Icon(
                icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IosDivider() {
    Divider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

// ==================== 悬浮气泡菜单 ====================

/**
 * 悬浮气泡风格长按菜单 - 圆形按钮环绕布局
 */
@Composable
fun FloatingBubbleLongPressMenu(
    result: LongPressHandler.LongPressResult,
    touchX: Float,
    touchY: Float,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onSaveImage: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    val density = LocalDensity.current
    
    // 动画状态
    var visible by remember { mutableStateOf(false) }
    val expandProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "expand"
    )
    
    LaunchedEffect(Unit) { visible = true }
    
    // 根据类型确定菜单项
    val menuItems = remember(result) {
        when (result) {
            is LongPressHandler.LongPressResult.Image -> listOf(
                BubbleMenuItem(Icons.Default.Download, Strings.longPressMenuSaveImage, Color(0xFF4CAF50)) { onSaveImage(result.url); onDismiss() },
                BubbleMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyImageLink, Color(0xFF2196F3)) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Video -> listOf(
                BubbleMenuItem(Icons.Default.Download, Strings.longPressMenuDownloadVideo, Color(0xFF9C27B0)) { onDownloadVideo(result.url); onDismiss() },
                BubbleMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyVideoLink, Color(0xFF2196F3)) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Link -> listOf(
                BubbleMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyLink, Color(0xFF2196F3)) { onCopyLink(result.url); onDismiss() },
                BubbleMenuItem(Icons.Default.OpenInBrowser, Strings.longPressMenuOpenInBrowser, Color(0xFFFF9800)) { onOpenInBrowser(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.ImageLink -> listOf(
                BubbleMenuItem(Icons.Default.Download, Strings.longPressMenuSaveImage, Color(0xFF4CAF50)) { onSaveImage(result.imageUrl); onDismiss() },
                BubbleMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyImageLink, Color(0xFF2196F3)) { onCopyLink(result.imageUrl); onDismiss() },
                BubbleMenuItem(Icons.Default.Link, Strings.longPressMenuCopyLink, Color(0xFF00BCD4)) { onCopyLink(result.linkUrl); onDismiss() },
                BubbleMenuItem(Icons.Default.OpenInBrowser, Strings.longPressMenuOpenInBrowser, Color(0xFFFF9800)) { onOpenInBrowser(result.linkUrl); onDismiss() }
            )
            else -> emptyList()
        }
    }
    
    if (menuItems.isEmpty()) {
        onDismiss()
        return
    }
    
    // Fullscreen遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f * expandProgress))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        // 气泡布局
        val bubbleRadius = 70.dp
        val bubbleSize = 52.dp
        val angleStep = 360f / menuItems.size
        val startAngle = -90f // 从顶部开始
        
        menuItems.forEachIndexed { index, item ->
            val angle = Math.toRadians((startAngle + angleStep * index).toDouble())
            val offsetX = with(density) { (bubbleRadius.toPx() * cos(angle) * expandProgress).roundToInt() }
            val offsetY = with(density) { (bubbleRadius.toPx() * sin(angle) * expandProgress).roundToInt() }
            
            // 延迟动画
            val itemScale by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = 400f
                ),
                label = "itemScale$index"
            )
            
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    x = (touchX + offsetX - with(density) { bubbleSize.toPx() / 2 }).roundToInt(),
                    y = (touchY + offsetY - with(density) { bubbleSize.toPx() / 2 }).roundToInt()
                ),
                properties = PopupProperties(clippingEnabled = false)
            ) {
                FloatingBubble(
                    item = item,
                    size = bubbleSize,
                    scale = itemScale
                )
            }
        }
        
        // 中心关闭按钮
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(
                x = (touchX - with(density) { 24.dp.toPx() }).roundToInt(),
                y = (touchY - with(density) { 24.dp.toPx() }).roundToInt()
            )
        ) {
            Surface(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .scale(expandProgress),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = Strings.btnCancel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private data class BubbleMenuItem(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun FloatingBubble(
    item: BubbleMenuItem,
    size: androidx.compose.ui.unit.Dp,
    scale: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = item.onClick,
            modifier = Modifier
                .size(size)
                .scale(scale),
            shape = CircleShape,
            color = item.color,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 标签
        if (scale > 0.5f) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f * scale),
                modifier = Modifier.alpha(scale)
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

// ==================== 右键菜单风格 ====================

/**
 * 右键菜单风格长按菜单 - 类似桌面端，紧凑高效
 */
@Composable
@Suppress("UNUSED_VARIABLE")
fun ContextMenuLongPressMenu(
    result: LongPressHandler.LongPressResult,
    touchX: Float,
    touchY: Float,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onSaveImage: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    // 动画状态
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(150),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(150),
        label = "alpha"
    )
    
    LaunchedEffect(Unit) { visible = true }
    
    // 根据类型确定菜单项
    val menuItems = remember(result) {
        when (result) {
            is LongPressHandler.LongPressResult.Image -> listOf(
                ContextMenuItem(Icons.Default.Download, Strings.longPressMenuSaveImage) { onSaveImage(result.url); onDismiss() },
                ContextMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyImageLink) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Video -> listOf(
                ContextMenuItem(Icons.Default.Download, Strings.longPressMenuDownloadVideo) { onDownloadVideo(result.url); onDismiss() },
                ContextMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyVideoLink) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Link -> listOf(
                ContextMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyLink) { onCopyLink(result.url); onDismiss() },
                ContextMenuItem(Icons.Default.OpenInBrowser, Strings.longPressMenuOpenInBrowser) { onOpenInBrowser(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.ImageLink -> listOf(
                ContextMenuItem(Icons.Default.Download, Strings.longPressMenuSaveImage) { onSaveImage(result.imageUrl); onDismiss() },
                ContextMenuItem(Icons.Default.ContentCopy, Strings.longPressMenuCopyImageLink) { onCopyLink(result.imageUrl); onDismiss() },
                null, // 分割线
                ContextMenuItem(Icons.Default.Link, Strings.longPressMenuCopyLink) { onCopyLink(result.linkUrl); onDismiss() },
                ContextMenuItem(Icons.Default.OpenInBrowser, Strings.longPressMenuOpenInBrowser) { onOpenInBrowser(result.linkUrl); onDismiss() }
            )
            else -> emptyList()
        }
    }
    
    if (menuItems.isEmpty()) {
        onDismiss()
        return
    }
    
    // Fullscreen遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        // 计算菜单位置（避免超出屏幕）
        val menuWidth = 200.dp
        val itemHeight = 40.dp
        val menuHeight = itemHeight * menuItems.filterNotNull().size + 8.dp * (menuItems.count { it == null })
        
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(
                x = touchX.roundToInt(),
                y = touchY.roundToInt()
            ),
            properties = PopupProperties(clippingEnabled = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(menuWidth)
                    .scale(scale, scale)
                    .alpha(alpha),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    menuItems.forEach { item ->
                        if (item == null) {
                            // 分割线
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        } else {
                            ContextMenuItemRow(
                                icon = item.icon,
                                text = item.label,
                                onClick = item.onClick
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ContextMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun ContextMenuItemRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
