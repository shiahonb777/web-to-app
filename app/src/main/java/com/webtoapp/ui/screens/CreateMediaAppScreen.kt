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
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.MediaConfig
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.ui.components.BgmCard
import com.webtoapp.ui.components.IconPickerWithLibrary

/**
 * 创建媒体应用页面（图片/视频转APP）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMediaAppScreen(
    onBack: () -> Unit,
    onCreated: (
        name: String,
        appType: AppType,
        mediaUri: Uri,
        mediaConfig: MediaConfig,
        iconUri: Uri?,
        activationEnabled: Boolean,
        activationCodes: List<String>,
        bgmEnabled: Boolean,
        bgmConfig: BgmConfig
    ) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // 应用信息
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }  // 图标库选择的路径
    
    // 媒体类型和内容
    var mediaType by remember { mutableStateOf(AppType.IMAGE) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    
    // 媒体配置
    var enableAudio by remember { mutableStateOf(true) }
    var loop by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(true) }
    var fillScreen by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(SplashOrientation.PORTRAIT) }
    
    // 激活码
    var activationEnabled by remember { mutableStateOf(false) }
    var activationCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 背景音乐
    var bgmEnabled by remember { mutableStateOf(false) }
    var bgmConfig by remember { mutableStateOf(BgmConfig()) }
    
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
                            mediaUri?.let { uri ->
                                // 处理图标：优先使用图标库路径，否则使用相册选择的Uri
                                val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
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
                                    activationEnabled,
                                    activationCodes,
                                    bgmEnabled,
                                    bgmConfig
                                )
                            }
                        },
                        enabled = mediaUri != null
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
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // 选择媒体文件
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
            
            // 显示配置
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
            
            // 激活码设置
            MediaActivationCard(
                enabled = activationEnabled,
                codes = activationCodes,
                onEnabledChange = { activationEnabled = it },
                onCodesChange = { activationCodes = it }
            )
            
            // 背景音乐
            BgmCard(
                enabled = bgmEnabled,
                config = bgmConfig,
                onEnabledChange = { bgmEnabled = it },
                onConfigChange = { bgmConfig = it }
            )
            
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

/**
 * 媒体应用激活码设置卡片（简化版）
 */
@Composable
fun MediaActivationCard(
    enabled: Boolean,
    codes: List<String>,
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<String>) -> Unit
) {
    var newCode by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        Icons.Outlined.Key,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "激活码验证",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                Text(
                    text = "启用后，用户需要输入正确的激活码才能使用应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 添加激活码
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        placeholder = { Text("输入激活码") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (newCode.isNotBlank()) {
                                onCodesChange(codes + newCode)
                                newCode = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "添加")
                    }
                }

                // 激活码列表
                codes.forEachIndexed { index, code ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onCodesChange(codes.filterIndexed { i, _ -> i != index })
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
