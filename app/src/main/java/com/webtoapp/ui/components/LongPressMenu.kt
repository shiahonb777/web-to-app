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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.core.webview.LongPressHandler
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * long- press BottomSheet
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
                    // Note
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
        // Imagepreview
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
                    contentDescription = AppStringsProvider.current().longPressMenuImagePreview,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // URL display
        Text(
            text = imageUrl.take(100) + if (imageUrl.length > 100) "..." else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        
        // button
        MenuItemButton(
            icon = Icons.Default.Download,
            text = AppStringsProvider.current().longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = AppStringsProvider.current().longPressMenuCopyImageLink,
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
        // Note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VideoFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = AppStringsProvider.current().longPressMenuVideo,
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        // URL display
        Text(
            text = videoUrl.take(100) + if (videoUrl.length > 100) "..." else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        
        // button
        MenuItemButton(
            icon = Icons.Default.Download,
            text = AppStringsProvider.current().longPressMenuDownloadVideo,
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = AppStringsProvider.current().longPressMenuCopyVideoLink,
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
        // Note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
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
        
        HorizontalDivider()
        
        // button
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = AppStringsProvider.current().longPressMenuCopyLink,
            onClick = {
                onCopyLink()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.OpenInBrowser,
            text = AppStringsProvider.current().longPressMenuOpenInBrowser,
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
        // Imagepreview
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
                    contentDescription = AppStringsProvider.current().longPressMenuImagePreview,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        HorizontalDivider()
        
        // Image
        Text(
            text = AppStringsProvider.current().longPressMenuImage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        
        MenuItemButton(
            icon = Icons.Default.Download,
            text = AppStringsProvider.current().longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = AppStringsProvider.current().longPressMenuCopyImageLink,
            onClick = {
                onCopyImageLink()
                onDismiss()
            }
        )
        
        // ( if �?
        if (linkUrl.isNotBlank()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = AppStringsProvider.current().longPressMenuLink,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            
            MenuItemButton(
                icon = Icons.Default.ContentCopy,
                text = AppStringsProvider.current().longPressMenuCopyLinkAddress,
                onClick = {
                    onCopyLink()
                    onDismiss()
                }
            )
            
            MenuItemButton(
                icon = Icons.Default.OpenInBrowser,
                text = AppStringsProvider.current().longPressMenuOpenInBrowser,
                onClick = {
                    onOpenInBrowser()
                    onDismiss()
                }
            )
        }
    }
}

/**
 * long- press BottomSheet
 * onlysupportsave
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
                    // mode support download, only
                    SimpleLinkOnlyContent(
                        url = result.url,
                        title = AppStringsProvider.current().longPressMenuVideo,
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
        // Imagepreview
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
                    contentDescription = AppStringsProvider.current().longPressMenuImagePreview,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        HorizontalDivider()
        
        // button( onlysave)
        MenuItemButton(
            icon = Icons.Default.Download,
            text = AppStringsProvider.current().longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = AppStringsProvider.current().longPressMenuCopyImageLink,
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
        // Note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
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
        
        HorizontalDivider()
        
        // only
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = AppStringsProvider.current().longPressMenuCopyLink,
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
        // Imagepreview
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
                    contentDescription = AppStringsProvider.current().longPressMenuImagePreview,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        HorizontalDivider()
        
        // Image
        Text(
            text = AppStringsProvider.current().longPressMenuImage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        
        MenuItemButton(
            icon = Icons.Default.Download,
            text = AppStringsProvider.current().longPressMenuSaveImage,
            onClick = {
                onSaveImage()
                onDismiss()
            }
        )
        
        MenuItemButton(
            icon = Icons.Default.ContentCopy,
            text = AppStringsProvider.current().longPressMenuCopyImageLink,
            onClick = {
                onCopyImageLink()
                onDismiss()
            }
        )
        
        // ( only)
        if (linkUrl.isNotBlank()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = AppStringsProvider.current().longPressMenuLink,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            
            MenuItemButton(
                icon = Icons.Default.ContentCopy,
                text = AppStringsProvider.current().longPressMenuCopyLinkAddress,
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

// ==================== iOS ====================

/**
 * iOS long- press- , iPhone
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
    // animationstate
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
    
    // Fullscreen
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
        // iOS card
        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(24.dp)
                .scale(scale)
                .alpha(alpha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Note */ },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column {
                // typedisplay content
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
        // Imagepreview
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
        IosMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuSaveImage, onSaveImage)
        IosHorizontalDivider()
        IosMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyImageLink, onCopyLink, isLast = true)
    }
}

@Composable
private fun IosVideoMenuContent(
    onDownload: () -> Unit,
    onCopyLink: () -> Unit
) {
    Column {
        // Videoiconheader
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
        IosMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuDownloadVideo, onDownload)
        IosHorizontalDivider()
        IosMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyVideoLink, onCopyLink, isLast = true)
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
        // header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f))
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
        IosMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyLink, onCopyLink)
        IosHorizontalDivider()
        IosMenuItem(Icons.Default.OpenInBrowser, AppStringsProvider.current().longPressMenuOpenInBrowser, onOpenInBrowser, isLast = true)
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
        // Imagepreview
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
        IosMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuSaveImage, onSaveImage)
        IosHorizontalDivider()
        IosMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyImageLink, onCopyImageLink)
        IosHorizontalDivider()
        IosMenuItem(Icons.Default.Link, AppStringsProvider.current().longPressMenuCopyLink, onCopyLink)
        IosHorizontalDivider()
        IosMenuItem(Icons.Default.OpenInBrowser, AppStringsProvider.current().longPressMenuOpenInBrowser, onOpenInBrowser, isLast = true)
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
private fun IosHorizontalDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

// Note

/**
 * long- press- button
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
    
    // animationstate
    var visible by remember { mutableStateOf(false) }
    val expandProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "expand"
    )
    
    LaunchedEffect(Unit) { visible = true }
    
    // type
    val menuItems = remember(result) {
        when (result) {
            is LongPressHandler.LongPressResult.Image -> listOf(
                BubbleMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuSaveImage, AppColors.Success) { onSaveImage(result.url); onDismiss() },
                BubbleMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyImageLink, Color(0xFF2196F3)) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Video -> listOf(
                BubbleMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuDownloadVideo, Color(0xFF9C27B0)) { onDownloadVideo(result.url); onDismiss() },
                BubbleMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyVideoLink, Color(0xFF2196F3)) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Link -> listOf(
                BubbleMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyLink, Color(0xFF2196F3)) { onCopyLink(result.url); onDismiss() },
                BubbleMenuItem(Icons.Default.OpenInBrowser, AppStringsProvider.current().longPressMenuOpenInBrowser, AppColors.Warning) { onOpenInBrowser(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.ImageLink -> listOf(
                BubbleMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuSaveImage, AppColors.Success) { onSaveImage(result.imageUrl); onDismiss() },
                BubbleMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyImageLink, Color(0xFF2196F3)) { onCopyLink(result.imageUrl); onDismiss() },
                BubbleMenuItem(Icons.Default.Link, AppStringsProvider.current().longPressMenuCopyLink, Color(0xFF00BCD4)) { onCopyLink(result.linkUrl); onDismiss() },
                BubbleMenuItem(Icons.Default.OpenInBrowser, AppStringsProvider.current().longPressMenuOpenInBrowser, AppColors.Warning) { onOpenInBrowser(result.linkUrl); onDismiss() }
            )
            else -> emptyList()
        }
    }
    
    if (menuItems.isEmpty()) {
        onDismiss()
        return
    }
    
    // Fullscreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f * expandProgress))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        // Note
        val bubbleRadius = 70.dp
        val bubbleSize = 52.dp
        val angleStep = 360f / menuItems.size
        val startAngle = -90f // fromtop
        
        menuItems.forEachIndexed { index, item ->
            val angle = Math.toRadians((startAngle + angleStep * index).toDouble())
            val offsetX = with(density) { (bubbleRadius.toPx() * cos(angle) * expandProgress).roundToInt() }
            val offsetY = with(density) { (bubbleRadius.toPx() * sin(angle) * expandProgress).roundToInt() }
            
            // animation
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
        
        // closebutton
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
                        contentDescription = AppStringsProvider.current().btnCancel,
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
        
        // label
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

// Note

/**
 * long- press- ,
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
    // animationstate
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
    
    // type
    val menuItems = remember(result) {
        when (result) {
            is LongPressHandler.LongPressResult.Image -> listOf(
                ContextMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuSaveImage) { onSaveImage(result.url); onDismiss() },
                ContextMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyImageLink) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Video -> listOf(
                ContextMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuDownloadVideo) { onDownloadVideo(result.url); onDismiss() },
                ContextMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyVideoLink) { onCopyLink(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.Link -> listOf(
                ContextMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyLink) { onCopyLink(result.url); onDismiss() },
                ContextMenuItem(Icons.Default.OpenInBrowser, AppStringsProvider.current().longPressMenuOpenInBrowser) { onOpenInBrowser(result.url); onDismiss() }
            )
            is LongPressHandler.LongPressResult.ImageLink -> listOf(
                ContextMenuItem(Icons.Default.Download, AppStringsProvider.current().longPressMenuSaveImage) { onSaveImage(result.imageUrl); onDismiss() },
                ContextMenuItem(Icons.Default.ContentCopy, AppStringsProvider.current().longPressMenuCopyImageLink) { onCopyLink(result.imageUrl); onDismiss() },
                null, // Note
                ContextMenuItem(Icons.Default.Link, AppStringsProvider.current().longPressMenuCopyLink) { onCopyLink(result.linkUrl); onDismiss() },
                ContextMenuItem(Icons.Default.OpenInBrowser, AppStringsProvider.current().longPressMenuOpenInBrowser) { onOpenInBrowser(result.linkUrl); onDismiss() }
            )
            else -> emptyList()
        }
    }
    
    if (menuItems.isEmpty()) {
        onDismiss()
        return
    }
    
    // Fullscreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        // Note
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
                            // Note
                            HorizontalDivider(
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
