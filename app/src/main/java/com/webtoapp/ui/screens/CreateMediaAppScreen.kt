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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.MediaConfig
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.ui.components.*
import kotlinx.coroutines.flow.first

/**
 * 创建/编辑媒体应用页面（图片/视频转APP）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMediaAppScreen(
    existingAppId: Long? = null,  // 编辑模式时传入已有应用ID
    onBack: () -> Unit,
    onCreated: (
        name: String,
        appType: AppType,
        mediaUri: Uri?,
        mediaConfig: MediaConfig?,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = existingAppId != null
    
    // 编辑模式时加载已有应用数据
    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = com.webtoapp.WebToAppApplication.repository
                .getWebAppById(existingAppId)
                .first()
        }
    }
    val scrollState = rememberScrollState()
    
    // App信息
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }
    
    // Media类型
    var mediaType by remember { mutableStateOf(AppType.IMAGE) }
    
    // 单媒体模式
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    
    // Media配置
    var enableAudio by remember { mutableStateOf(true) }
    var loop by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(true) }
    var fillScreen by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(SplashOrientation.PORTRAIT) }
    
    // Theme配置
    var themeType by remember { mutableStateOf("AURORA") }
    
    // 编辑模式：加载现有应用数据到UI状态
    LaunchedEffect(existingApp) {
        existingApp?.let { app ->
            // 加载基本信息
            appName = app.name
            appIconPath = app.iconPath
            
            // 加载媒体类型
            mediaType = app.appType
            
            // 加载媒体配置
            app.mediaConfig?.let { config ->
                // 尝试加载媒体文件
                if (config.mediaPath.isNotBlank()) {
                    // 检查是否是文件路径
                    val file = java.io.File(config.mediaPath)
                    if (file.exists()) {
                        mediaUri = Uri.fromFile(file)
                    } else if (config.mediaPath.startsWith("content://") || config.mediaPath.startsWith("file://")) {
                        mediaUri = Uri.parse(config.mediaPath)
                    }
                }
                
                // 加载配置选项
                enableAudio = config.enableAudio
                loop = config.loop
                autoPlay = config.autoPlay
                fillScreen = config.fillScreen
                orientation = config.orientation
            }
            
            // 加载主题
            themeType = app.themeType
        }
    }
    
    // File选择器
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
    val canCreate = mediaUri != null
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.createMediaAppTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            mediaUri?.let { uri ->
                                onCreated(
                                    appName.ifBlank { Strings.createMediaApp },
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
                                    themeType
                                )
                            }
                        },
                        enabled = canCreate
                    ) {
                        Text(Strings.btnCreate)
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
            // Media类型选择
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.selectMediaType,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Image选项
                        MediaTypeOption(
                            icon = Icons.Outlined.Image,
                            label = Strings.image,
                            selected = mediaType == AppType.IMAGE,
                            onClick = {
                                mediaType = AppType.IMAGE
                                mediaUri = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Video选项
                        MediaTypeOption(
                            icon = Icons.Outlined.Videocam,
                            label = Strings.video,
                            selected = mediaType == AppType.VIDEO,
                            onClick = {
                                mediaType = AppType.VIDEO
                                mediaUri = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Select媒体文件
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (mediaType == AppType.IMAGE) Strings.selectImage else Strings.selectVideo,
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
                                        text = Strings.videoSelected,
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
                                    text = if (mediaType == AppType.IMAGE) Strings.clickToSelectImage else Strings.clickToSelectVideo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // App信息
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.labelAppInfo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // App名称（带随机按钮）
                    AppNameTextFieldSimple(
                        value = appName,
                        onValueChange = { appName = it },
                        placeholder = Strings.createMediaApp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
            
            // Show配置
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.labelDisplaySettings,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SettingsRow(
                        title = Strings.fillScreen,
                        subtitle = Strings.fillScreenHint
                    ) {
                        Switch(
                            checked = fillScreen,
                            onCheckedChange = { fillScreen = it }
                        )
                    }
                    
                    SettingsRow(
                        title = Strings.landscapeMode,
                        subtitle = Strings.landscapeModeHint
                    ) {
                        Switch(
                            checked = orientation == SplashOrientation.LANDSCAPE,
                            onCheckedChange = { 
                                orientation = if (it) SplashOrientation.LANDSCAPE else SplashOrientation.PORTRAIT
                            }
                        )
                    }
                    
                    if (mediaType == AppType.VIDEO) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsRow(
                            title = Strings.enableAudio,
                            subtitle = Strings.enableAudioHint
                        ) {
                            Switch(
                                checked = enableAudio,
                                onCheckedChange = { enableAudio = it }
                            )
                        }
                        
                        SettingsRow(
                            title = Strings.loopPlay,
                            subtitle = Strings.loopPlayHint
                        ) {
                            Switch(
                                checked = loop,
                                onCheckedChange = { loop = it }
                            )
                        }
                        
                        SettingsRow(
                            title = Strings.autoPlay,
                            subtitle = Strings.autoPlayHint
                        ) {
                            Switch(
                                checked = autoPlay,
                                onCheckedChange = { autoPlay = it }
                            )
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
                        text = Strings.mediaAppHint.replace("%s", 
                            if (mediaType == AppType.IMAGE) Strings.fullscreenDisplayImage else Strings.fullscreenPlayVideo
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
