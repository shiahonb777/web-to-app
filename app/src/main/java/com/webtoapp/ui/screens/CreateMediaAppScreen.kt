package com.webtoapp.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.MediaConfig
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.ui.components.*
import kotlinx.coroutines.flow.first
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * create/edit app( / APP)
 * 
 * Note
 * type Hero area( = gradient, = gradient)
 * file display( file, , )
 * panel( , , )
 * select
 * colorselect
 * Note
 * settings( , )
 * preview
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMediaAppScreen(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long? = null,
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
    
    // editmodeload app
    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = webAppRepository.getWebAppById(existingAppId).first()
        }
    }
    val scrollState = rememberScrollState()
    
    // App
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }
    
    // Mediatype
    var mediaType by remember { mutableStateOf(AppType.IMAGE) }
    
    // mode
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    
    // Mediaconfig
    var enableAudio by remember { mutableStateOf(true) }
    var loop by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(true) }
    var fillScreen by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(SplashOrientation.PORTRAIT) }
    var backgroundColor by remember { mutableStateOf("#000000") }
    
    // file
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileSize by remember { mutableStateOf<Long?>(null) }
    var fileMimeType by remember { mutableStateOf<String?>(null) }
    
    // Note
    var brightness by remember { mutableFloatStateOf(1.0f) }
    var contrast by remember { mutableFloatStateOf(1.0f) }
    var saturation by remember { mutableFloatStateOf(1.0f) }
    
    // Note
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    
    // Note
    var keepScreenOn by remember { mutableStateOf(true) }
    
    // config
    var swipeDismiss by remember { mutableStateOf(true) }
    var doubleTapZoom by remember { mutableStateOf(true) }
    
    // Themeconfig
    var themeType by remember { mutableStateOf("AURORA") }
    
    // Note
    val brandColor = remember(mediaType) {
        if (mediaType == AppType.IMAGE) Color(0xFF7C4DFF) else Color(0xFFFF5252)
    }
    val brandGradient = remember(mediaType) {
        if (mediaType == AppType.IMAGE)
            listOf(Color(0xFF7C4DFF).copy(alpha = 0.15f), Color(0xFF448AFF).copy(alpha = 0.05f))
        else
            listOf(Color(0xFFFF5252).copy(alpha = 0.15f), Color(0xFFFF6E40).copy(alpha = 0.05f))
    }
    
    // editmode: load app UIstate
    LaunchedEffect(existingApp) {
        existingApp?.let { app ->
            appName = app.name
            appIconPath = app.iconPath
            mediaType = app.appType
            app.mediaConfig?.let { config ->
                if (config.mediaPath.isNotBlank()) {
                    val file = java.io.File(config.mediaPath)
                    if (file.exists()) {
                        mediaUri = Uri.fromFile(file)
                    } else if (config.mediaPath.startsWith("content://") || config.mediaPath.startsWith("file://")) {
                        mediaUri = Uri.parse(config.mediaPath)
                    }
                }
                enableAudio = config.enableAudio
                loop = config.loop
                autoPlay = config.autoPlay
                fillScreen = config.fillScreen
                orientation = config.orientation
                backgroundColor = config.backgroundColor
                keepScreenOn = config.keepScreenOn
            }
            themeType = app.themeType
        }
    }
    
    // file
    fun readFileMetadata(uri: Uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) fileName = cursor.getString(nameIdx)
                    if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                }
            }
            fileMimeType = context.contentResolver.getType(uri)
        } catch (e: Exception) {
            // failed
        }
    }
    
    // Fileselect
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { mediaUri = it; readFileMetadata(it) } }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { mediaUri = it; readFileMetadata(it) } }
    
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    // create
    val canCreate = mediaUri != null
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().createMediaAppTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            mediaUri?.let { uri ->
                                onCreated(
                                    appName.ifBlank { AppStringsProvider.current().createMediaApp },
                                    mediaType,
                                    uri,
                                    MediaConfig(
                                        mediaPath = uri.toString(),
                                        enableAudio = enableAudio,
                                        loop = loop,
                                        autoPlay = autoPlay,
                                        fillScreen = fillScreen,
                                        orientation = orientation,
                                        backgroundColor = backgroundColor,
                                        keepScreenOn = keepScreenOn
                                    ),
                                    finalIconUri,
                                    themeType
                                )
                            }
                        },
                        enabled = canCreate
                    ) {
                        Text(AppStringsProvider.current().btnCreate)
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== 1. type Hero ==========
            MediaHeroSection(
                mediaType = mediaType,
                brandColor = brandColor,
                brandGradient = brandGradient,
                fileName = fileName,
                fileSize = fileSize,
                fileMimeType = fileMimeType
            )
            
            // ========== 2. typeselect ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStringsProvider.current().selectMediaType,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MediaTypeOption(
                            icon = Icons.Outlined.Image,
                            label = AppStringsProvider.current().image,
                            selected = mediaType == AppType.IMAGE,
                            onClick = {
                                mediaType = AppType.IMAGE
                                mediaUri = null
                                fileName = null
                                fileSize = null
                                fileMimeType = null
                            },
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                        MediaTypeOption(
                            icon = Icons.Outlined.Videocam,
                            label = AppStringsProvider.current().video,
                            selected = mediaType == AppType.VIDEO,
                            onClick = {
                                mediaType = AppType.VIDEO
                                mediaUri = null
                                fileName = null
                                fileSize = null
                                fileMimeType = null
                            },
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                    }
                }
            }
            
            // ========== 3. fileselect( preview) ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(brandColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(
                            if (mediaType == AppType.IMAGE) Icons.Outlined.Image else Icons.Outlined.Videocam,
                            null, tint = brandColor, modifier = Modifier.size(22.dp)
                        ) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (mediaType == AppType.IMAGE) AppStringsProvider.current().selectImage else AppStringsProvider.current().selectVideo,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (mediaUri != null) Color.Black
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 2.dp,
                                color = if (mediaUri != null) brandColor
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
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
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PlayCircle, null,
                                        modifier = Modifier.size(64.dp),
                                        tint = brandColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = AppStringsProvider.current().videoSelected,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = brandColor
                                    )
                                }
                            }
                            // Filename label
                            fileName?.let { name ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (mediaType == AppType.IMAGE) Icons.Outlined.AddPhotoAlternate
                                    else Icons.Outlined.VideoLibrary,
                                    null, modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (mediaType == AppType.IMAGE) AppStringsProvider.current().clickToSelectImage else AppStringsProvider.current().clickToSelectVideo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Enhanced: file metadata
                    if (mediaUri != null && (fileSize != null || fileMimeType != null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MediaFileInfoRow(
                            fileSize = fileSize,
                            mimeType = fileMimeType,
                            brandColor = brandColor
                        )
                    }
                }
            }
            
            // ========== 4. App Info ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStringsProvider.current().labelAppInfo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AppNameTextFieldSimple(
                        value = appName,
                        onValueChange = { appName = it },
                        placeholder = AppStringsProvider.current().createMediaApp
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
            
            // ========== 5. Display Settings ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(brandColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Tune, null, tint = brandColor, modifier = Modifier.size(22.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(AppStringsProvider.current().labelDisplaySettings, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SettingsRow(title = AppStringsProvider.current().fillScreen, subtitle = AppStringsProvider.current().fillScreenHint) {
                        PremiumSwitch(
                            checked = fillScreen,
                            onCheckedChange = { fillScreen = it },
                        )
                    }
                    
                    SettingsRow(title = AppStringsProvider.current().landscapeMode, subtitle = AppStringsProvider.current().landscapeModeHint) {
                        PremiumSwitch(
                            checked = orientation == SplashOrientation.LANDSCAPE,
                            onCheckedChange = {
                                orientation = if (it) SplashOrientation.LANDSCAPE else SplashOrientation.PORTRAIT
                            },
                        )
                    }
                    
                    // Enhanced: keep screen on
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsRow(title = AppStringsProvider.current().mediaScreenLock, subtitle = AppStringsProvider.current().mediaScreenLockHint) {
                        PremiumSwitch(
                            checked = keepScreenOn,
                            onCheckedChange = { keepScreenOn = it },
                        )
                    }
                    
                    if (mediaType == AppType.VIDEO) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        SettingsRow(title = AppStringsProvider.current().enableAudio, subtitle = AppStringsProvider.current().enableAudioHint) {
                            PremiumSwitch(
                                checked = enableAudio,
                                onCheckedChange = { enableAudio = it },
                            )
                        }
                        SettingsRow(title = AppStringsProvider.current().loopPlay, subtitle = AppStringsProvider.current().loopPlayHint) {
                            PremiumSwitch(
                                checked = loop,
                                onCheckedChange = { loop = it },
                            )
                        }
                        SettingsRow(title = AppStringsProvider.current().autoPlay, subtitle = AppStringsProvider.current().autoPlayHint) {
                            PremiumSwitch(
                                checked = autoPlay,
                                onCheckedChange = { autoPlay = it },
                            )
                        }
                    }
                }
            }
            
            // ========== 6. Video Speed (Video Only) ==========
            if (mediaType == AppType.VIDEO && mediaUri != null) {
                MediaPlaybackSpeedCard(
                    speed = playbackSpeed,
                    onSpeedChange = { playbackSpeed = it },
                    brandColor = brandColor
                )
            }
            
            // ========== 7. Image Adjustments (Image Only) ==========
            if (mediaType == AppType.IMAGE && mediaUri != null) {
                MediaImageAdjustCard(
                    brightness = brightness,
                    onBrightnessChange = { brightness = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it },
                    saturation = saturation,
                    onSaturationChange = { saturation = it },
                    onReset = { brightness = 1.0f; contrast = 1.0f; saturation = 1.0f },
                    brandColor = brandColor
                )
            }
            
            // ========== 8. Background Color ==========
            if (mediaUri != null) {
                MediaBackgroundColorCard(
                    selected = backgroundColor,
                    onSelect = { backgroundColor = it },
                    brandColor = brandColor
                )
            }
            
            // ========== 9. Gesture Settings ==========
            if (mediaUri != null) {
                MediaGestureCard(
                    swipeDismiss = swipeDismiss,
                    onSwipeDismissChange = { swipeDismiss = it },
                    doubleTapZoom = doubleTapZoom,
                    onDoubleTapZoomChange = { doubleTapZoom = it },
                    isImage = mediaType == AppType.IMAGE,
                    brandColor = brandColor
                )
            }
            
            // Hint message
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = brandColor.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, null, modifier = Modifier.size(20.dp), tint = brandColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = AppStringsProvider.current().mediaAppHint.replace(
                            "%s",
                            if (mediaType == AppType.IMAGE) AppStringsProvider.current().fullscreenDisplayImage else AppStringsProvider.current().fullscreenPlayVideo
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
        }
}

// ==================== Private Composable Components ====================

/**
 * Hero area
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaHeroSection(
    mediaType: AppType,
    brandColor: Color,
    brandGradient: List<Color>,
    fileName: String?,
    fileSize: Long?,
    fileMimeType: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(colors = brandGradient),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = brandColor.copy(alpha = 0.15f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            if (mediaType == AppType.IMAGE) Icons.Outlined.Image else Icons.Outlined.Videocam,
                            null, modifier = Modifier.size(32.dp), tint = brandColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = if (mediaType == AppType.IMAGE) AppStringsProvider.current().mediaImageInfo else AppStringsProvider.current().mediaVideoInfo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                    Text(
                        text = if (mediaType == AppType.IMAGE)
                            AppStringsProvider.current().clickToSelectImage
                        else AppStringsProvider.current().clickToSelectVideo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fileName != null || fileSize != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // label
                            fileMimeType?.let { mime ->
                                val ext = mime.substringAfter("/").uppercase()
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = brandColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = ext,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = brandColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            // label
                            fileSize?.let { size ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = brandColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = formatFileSize(size),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = brandColor,
                                        fontFamily = FontFamily.Monospace
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

/**
 * file
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaFileInfoRow(
    fileSize: Long?,
    mimeType: String?,
    brandColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = brandColor.copy(alpha = 0.06f)
    ) {
        FlowRow(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            fileSize?.let { size ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Storage, null, modifier = Modifier.size(14.dp), tint = brandColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${AppStringsProvider.current().mediaFileSize}: ${formatFileSize(size)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            mimeType?.let { mime ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, null, modifier = Modifier.size(14.dp), tint = brandColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${AppStringsProvider.current().mediaFormat}: ${mime.substringAfter("/")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaPlaybackSpeedCard(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    brandColor: Color
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(brandColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Speed, null, tint = brandColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().mediaPlaybackSpeed, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                speeds.forEach { s ->
                    val isSelected = speed == s
                    PremiumFilterChip(
                        selected = isSelected,
                        onClick = { onSpeedChange(s) },
                        label = {
                            Text(
                                "${s}x",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                }
            }
        }
    }
}

/**
 * card
 */
@Composable
private fun MediaImageAdjustCard(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit,
    brandColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(brandColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Tune, null, tint = brandColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().mediaImageAdjust, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                TextButton(onClick = onReset) {
                    Text(AppStringsProvider.current().mediaReset, style = MaterialTheme.typography.labelSmall, color = brandColor)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                AppStringsProvider.current().mediaImageAdjustHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Note
            MediaSliderRow(
                label = AppStringsProvider.current().mediaBrightness,
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = 0.5f..2.0f,
                brandColor = brandColor
            )
            // Note
            MediaSliderRow(
                label = AppStringsProvider.current().mediaContrast,
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = 0.5f..2.0f,
                brandColor = brandColor
            )
            // Note
            MediaSliderRow(
                label = AppStringsProvider.current().mediaSaturation,
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = 0.0f..2.0f,
                brandColor = brandColor
            )
        }
    }
}

/**
 * Note
 */
@Composable
private fun MediaSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    brandColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                String.format(java.util.Locale.getDefault(), "%.1f", value),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = brandColor,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = brandColor,
                activeTrackColor = brandColor
            )
        )
    }
}

/**
 * colorselectcard
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaBackgroundColorCard(
    selected: String,
    onSelect: (String) -> Unit,
    brandColor: Color
) {
    val colors = listOf(
        "#000000" to "Black",
        "#FFFFFF" to "White",
        "#1A1A2E" to "Dark Blue",
        "#16213E" to "Navy",
        "#1B1B1B" to "Charcoal",
        "#2D2D2D" to "Dark Gray",
        "#0D1117" to "GitHub Dark",
        "#282C34" to "VS Code"
    )
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(brandColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Palette, null, tint = brandColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().mediaBackgroundColor, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colors.forEach { (hex, label) ->
                    val isSelected = selected == hex
                    val color = Color(android.graphics.Color.parseColor(hex))
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = color,
                        border = if (isSelected)
                            androidx.compose.foundation.BorderStroke(3.dp, brandColor)
                        else
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = { onSelect(hex) }
                    ) {
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Check, null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (hex == "#FFFFFF") Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * settingscard
 */
@Composable
private fun MediaGestureCard(
    swipeDismiss: Boolean,
    onSwipeDismissChange: (Boolean) -> Unit,
    doubleTapZoom: Boolean,
    onDoubleTapZoomChange: (Boolean) -> Unit,
    isImage: Boolean,
    brandColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(brandColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.TouchApp, null, tint = brandColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().mediaGestureConfig, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsRow(title = AppStringsProvider.current().mediaSwipeDismiss, subtitle = AppStringsProvider.current().mediaSwipeDismissHint) {
                PremiumSwitch(
                    checked = swipeDismiss,
                    onCheckedChange = onSwipeDismissChange,
                )
            }
            
            if (isImage) {
                SettingsRow(title = AppStringsProvider.current().mediaDoubleTapZoom, subtitle = AppStringsProvider.current().mediaDoubleTapZoomHint) {
                    PremiumSwitch(
                        checked = doubleTapZoom,
                        onCheckedChange = onDoubleTapZoomChange,
                    )
                }
            }
        }
    }
}

/**
 * type card
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
                icon, null, modifier = Modifier.size(32.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * settings
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
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
 * file
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${String.format(java.util.Locale.getDefault(), "%.1f", bytes.toDouble() / 1024 / 1024)} MB"
        else -> "${String.format(java.util.Locale.getDefault(), "%.2f", bytes.toDouble() / 1024 / 1024 / 1024)} GB"
    }
}
