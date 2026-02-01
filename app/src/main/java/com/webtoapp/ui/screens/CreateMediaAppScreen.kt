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
import com.webtoapp.ui.components.AppNameTextFieldSimple
import com.webtoapp.ui.components.EnhancedElevatedCard
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
        mediaUri: Uri?,
        mediaConfig: MediaConfig?,
        iconUri: Uri?,
        themeType: String
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

    // 单媒体模式
    var mediaUri by remember { mutableStateOf<Uri?>(null) }

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
            // 媒体类型选择
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
                        // 图片选项
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

                        // 视频选项
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

            // 选择媒体文件
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
                                    contentDescription = Strings.selectedImage,
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

            // 应用信息
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.labelAppInfo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 应用名称（带随机按钮）
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

            // 显示配置
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
