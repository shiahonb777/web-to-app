package com.webtoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.MediaConfig
import com.webtoapp.data.model.MediaItemConfig
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.ui.components.IconPickerWithLibrary
import com.webtoapp.ui.components.gallery.GalleryConfigCard
import com.webtoapp.ui.components.gallery.GalleryItemEditor

/**
 * 创建媒体应用页面（图片/视频转APP）
 * 支持单个媒体或多媒体画廊模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMediaAppScreen(
    onBack: () -> Unit,
    onCreated: (
        name: String,
        appType: AppType,
        mediaUri: Uri?,
        mediaConfig: MediaConfig?,
        iconUri: Uri?,
        themeType: String,
        galleryConfig: GalleryConfig?
    ) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // 应用信息
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }
    
    // 媒体类型
    var mediaType by remember { mutableStateOf(AppType.IMAGE) }
    
    // 模式选择：单个媒体 vs 多媒体画廊
    var isGalleryMode by remember { mutableStateOf(false) }
    
    // 单媒体模式
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    
    // 多媒体画廊模式
    var galleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var galleryConfig by remember { mutableStateOf(GalleryConfig()) }
    
    // 媒体配置
    var enableAudio by remember { mutableStateOf(true) }
    var loop by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(true) }
    var fillScreen by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(SplashOrientation.PORTRAIT) }
    
    // 主题配置
    var themeType by remember { mutableStateOf("AURORA") }
    
    // 文件选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { mediaUri = it } }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { mediaUri = it } }
    
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    // 判断是否可以创建
    val canCreate = if (isGalleryMode) {
        galleryItems.isNotEmpty()
    } else {
        mediaUri != null
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建媒体应用") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            
                            if (isGalleryMode) {
                                // 画廊模式
                                onCreated(
                                    appName.ifBlank { "媒体画廊" },
                                    mediaType,
                                    null,
                                    null,
                                    finalIconUri,
                                    themeType,
                                    galleryConfig.copy(items = galleryItems)
                                )
                            } else {
                                // 单媒体模式
                                mediaUri?.let { uri ->
                                    onCreated(
                                        appName.ifBlank { "媒体应用" },
                                        mediaType,
                                        uri,
                                        MediaConfig(
                                            mediaPath = uri.toString(),
                                            enableAudio = enableAudio,
                                            loop = loop,
                                            autoPlay = autoPlay,
                                            fillScreen = fillScreen,
                                            orientation = orientation
                                        ),
                                        finalIconUri,
                                        themeType,
                                        null
                                    )
                                }
                            }
                        },
                        enabled = canCreate
                    ) {
                        Text("创建")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 媒体类型选择
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "选择媒体类型",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 图片选项
                        MediaTypeOption(
                            icon = Icons.Outlined.Image,
                            label = "图片",
                            selected = mediaType == AppType.IMAGE,
                            onClick = {
                                mediaType = AppType.IMAGE
                                mediaUri = null
                                galleryItems = emptyList()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 视频选项
                        MediaTypeOption(
                            icon = Icons.Outlined.Videocam,
                            label = "视频",
                            selected = mediaType == AppType.VIDEO,
                            onClick = {
                                mediaType = AppType.VIDEO
                                mediaUri = null
                                galleryItems = emptyList()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // 模式选择：单个 vs 多个（画廊）
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "选择模式",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 单个媒体
                        MediaTypeOption(
                            icon = if (mediaType == AppType.IMAGE) Icons.Outlined.Photo else Icons.Outlined.OndemandVideo,
                            label = "单个${if (mediaType == AppType.IMAGE) "图片" else "视频"}",
                            selected = !isGalleryMode,
                            onClick = { isGalleryMode = false },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 多媒体画廊
                        MediaTypeOption(
                            icon = Icons.Outlined.Collections,
                            label = "多媒体画廊",
                            selected = isGalleryMode,
                            onClick = { isGalleryMode = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (isGalleryMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 画廊模式支持添加多个${if (mediaType == AppType.IMAGE) "图片" else "视频"}，可左右滑动切换，每个项目可自定义标题",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 选择媒体文件
            if (isGalleryMode) {
                // 画廊模式：多媒体选择
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        GalleryItemEditor(
                            items = galleryItems,
                            itemType = if (mediaType == AppType.IMAGE) GalleryItemType.IMAGE else GalleryItemType.VIDEO,
                            onItemsChange = { galleryItems = it }
                        )
                    }
                }
                
                // 画廊配置
                if (galleryItems.isNotEmpty()) {
                    GalleryConfigCard(
                        config = galleryConfig,
                        onConfigChange = { galleryConfig = it }
                    )
                }
            } else {
                // 单媒体模式
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (mediaType == AppType.IMAGE) "选择图片" else "选择视频",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 2.dp,
                                    color = if (mediaUri != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    if (mediaType == AppType.IMAGE) {
                                        imagePickerLauncher.launch("image/*")
                                    } else {
                                        videoPickerLauncher.launch("video/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (mediaUri != null) {
                                if (mediaType == AppType.IMAGE) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(mediaUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "选中的图片",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 视频缩略图
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "视频已选择",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        if (mediaType == AppType.IMAGE) 
                                            Icons.Outlined.AddPhotoAlternate 
                                        else 
                                            Icons.Outlined.VideoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "点击选择${if (mediaType == AppType.IMAGE) "图片" else "视频"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 应用信息
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "应用信息",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("应用名称") },
                        placeholder = { Text("我的媒体应用") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 应用图标（带图标库功能）
                    IconPickerWithLibrary(
                        iconUri = appIcon,
                        iconPath = appIconPath,
                        onSelectFromGallery = { iconPickerLauncher.launch("image/*") },
                        onSelectFromLibrary = { path -> 
                            appIconPath = path 
                            appIcon = null
                        }
                    )
                }
            }
            
            // 显示配置（仅单媒体模式）
            if (!isGalleryMode) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "显示设置",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 铺满屏幕
                        SettingsRow(
                            title = "铺满屏幕",
                            subtitle = "自动裁剪以填满整个屏幕"
                        ) {
                            Switch(
                                checked = fillScreen,
                                onCheckedChange = { fillScreen = it }
                            )
                        }
                        
                        // 屏幕方向
                        SettingsRow(
                            title = "横屏显示",
                            subtitle = "以横屏模式显示内容"
                        ) {
                            Switch(
                                checked = orientation == SplashOrientation.LANDSCAPE,
                                onCheckedChange = { 
                                    orientation = if (it) SplashOrientation.LANDSCAPE else SplashOrientation.PORTRAIT
                                }
                            )
                        }
                        
                        // 视频特有配置
                        if (mediaType == AppType.VIDEO) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            SettingsRow(
                                title = "启用音频",
                                subtitle = "播放视频时包含声音"
                            ) {
                                Switch(
                                    checked = enableAudio,
                                    onCheckedChange = { enableAudio = it }
                                )
                            }
                            
                            SettingsRow(
                                title = "循环播放",
                                subtitle = "视频结束后自动重新播放"
                            ) {
                                Switch(
                                    checked = loop,
                                    onCheckedChange = { loop = it }
                                )
                            }
                            
                            SettingsRow(
                                title = "自动播放",
                                subtitle = "打开应用时自动开始播放"
                            ) {
                                Switch(
                                    checked = autoPlay,
                                    onCheckedChange = { autoPlay = it }
                                )
                            }
                        }
                    }
                }
            }
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "创建的应用将${if (mediaType == AppType.IMAGE) "全屏显示您选择的图片" else "全屏播放您选择的视频"}，适合用作数字相框、广告展示或视频壁纸。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // 功能提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "💡 激活码验证、背景音乐等功能可在创建项目后，通过项目管理界面点击「编辑」进行添加和配置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 媒体类型选项卡片
 */
@Composable
fun MediaTypeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 设置项行
 */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}


