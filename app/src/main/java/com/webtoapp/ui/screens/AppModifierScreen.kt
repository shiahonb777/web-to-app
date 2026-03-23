package com.webtoapp.ui.screens

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.appmodifier.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.ui.components.*
import com.webtoapp.util.SplashStorage
import kotlinx.coroutines.launch
import java.io.File

/**
 * 应用修改器主页面 - 应用列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModifierScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val appListProvider = remember { AppListProvider(context) }
    val appCloner = remember { AppCloner(context) }
    
    // 状态
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(AppFilterType.USER) }
    var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    
    // Load应用列表
    LaunchedEffect(filterType, searchQuery) {
        isLoading = true
        apps = appListProvider.getInstalledApps(filterType, searchQuery)
        isLoading = false
    }
    
    // 如果选中了应用，显示修改页面
    if (selectedApp != null) {
        AppModifyFullScreen(
            app = selectedApp!!,
            appCloner = appCloner,
            onBack = { selectedApp = null },
            onResult = { result ->
                selectedApp = null
                scope.launch {
                    when (result) {
                        is AppModifyResult.ShortcutSuccess -> {
                            snackbarHostState.showSnackbar(Strings.shortcutCreated)
                        }
                        is AppModifyResult.CloneSuccess -> {
                            snackbarHostState.showSnackbar(Strings.cloneSuccess)
                        }
                        is AppModifyResult.Error -> {
                            snackbarHostState.showSnackbar("${Strings.failed}: ${result.message}")
                        }
                    }
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.appIconModifier) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Strings.searchApps) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, Strings.clear)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 筛选标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == AppFilterType.USER,
                    onClick = { filterType = AppFilterType.USER },
                    label = { Text(Strings.userApps) },
                    leadingIcon = if (filterType == AppFilterType.USER) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == AppFilterType.SYSTEM,
                    onClick = { filterType = AppFilterType.SYSTEM },
                    label = { Text(Strings.systemApps) },
                    leadingIcon = if (filterType == AppFilterType.SYSTEM) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == AppFilterType.ALL,
                    onClick = { filterType = AppFilterType.ALL },
                    label = { Text(Strings.all) },
                    leadingIcon = if (filterType == AppFilterType.ALL) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // App数量
            Text(
                text = Strings.totalFilesCount.replace("%d", apps.size.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // App列表
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            onClick = { selectedApp = app }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 应用列表项
 */
@Composable
fun AppListItem(
    app: InstalledAppInfo,
    onClick: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            app.icon?.let { drawable ->
                Image(
                    bitmap = drawable.toBitmap(56, 56).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            } ?: Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Android,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "v${app.versionName} · ${app.formattedSize}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 应用修改全屏页面（和 CreateAppScreen 类似的布局）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModifyFullScreen(
    app: InstalledAppInfo,
    appCloner: AppCloner,
    onBack: () -> Unit,
    onResult: (AppModifyResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // 基本信息状态
    var newAppName by remember { mutableStateOf(app.appName) }
    var newIconUri by remember { mutableStateOf<Uri?>(null) }
    var newIconPath by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var progressText by remember { mutableStateOf("") }
    
    // Start画面配置状态
    var splashEnabled by remember { mutableStateOf(false) }
    var splashType by remember { mutableStateOf("IMAGE") }
    var splashPath by remember { mutableStateOf<String?>(null) }
    var splashDuration by remember { mutableIntStateOf(3) }
    var splashClickToSkip by remember { mutableStateOf(true) }
    var splashLandscape by remember { mutableStateOf(false) }
    var splashFillScreen by remember { mutableStateOf(true) }
    var splashVideoStartMs by remember { mutableLongStateOf(0L) }
    var splashVideoEndMs by remember { mutableLongStateOf(5000L) }
    var splashVideoDurationMs by remember { mutableLongStateOf(0L) }
    var splashEnableAudio by remember { mutableStateOf(false) }
    
    // Activation码配置状态
    var activationEnabled by remember { mutableStateOf(false) }
    var activationCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var activationRequireEveryTime by remember { mutableStateOf(false) }
    var newActivationCode by remember { mutableStateOf("") }
    
    // Announcement配置状态
    var announcementEnabled by remember { mutableStateOf(false) }
    var announcementTitle by remember { mutableStateOf("") }
    var announcementContent by remember { mutableStateOf("") }
    var announcementLink by remember { mutableStateOf("") }
    
    // Background music配置状态
    var bgmEnabled by remember { mutableStateOf(false) }
    var bgmConfig by remember { mutableStateOf(BgmConfig()) }
    
    // Image选择器（相册选择）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            newIconUri = it
            newIconPath = null // 清除图标库路径
        }
    }
    
    // Start画面图片选择器
    val splashImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val savedPath = SplashStorage.saveMediaFromUri(context, it, isVideo = false)
                if (savedPath != null) {
                    splashPath = savedPath
                    splashType = "IMAGE"
                    splashEnabled = true
                }
            }
        }
    }
    
    // Start画面视频选择器
    val splashVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val savedPath = SplashStorage.saveMediaFromUri(context, it, isVideo = true)
                if (savedPath != null) {
                    splashPath = savedPath
                    splashType = "VIDEO"
                    splashEnabled = true
                    splashVideoStartMs = 0L
                    splashVideoEndMs = 5000L
                    splashVideoDurationMs = 0L
                }
            }
        }
    }
    
    // Build配置并执行操作
    fun buildConfig(): AppModifyConfig {
        return AppModifyConfig(
            originalApp = app,
            newAppName = newAppName,
            newIconPath = newIconPath ?: newIconUri?.toString(),
            splashEnabled = splashEnabled && splashPath != null,
            splashType = splashType,
            splashPath = splashPath,
            splashDuration = splashDuration,
            splashClickToSkip = splashClickToSkip,
            splashVideoStartMs = splashVideoStartMs,
            splashVideoEndMs = splashVideoEndMs,
            splashLandscape = splashLandscape,
            splashFillScreen = splashFillScreen,
            splashEnableAudio = splashEnableAudio,
            activationEnabled = activationEnabled,
            activationCodes = activationCodes,
            activationRequireEveryTime = activationRequireEveryTime,
            announcementEnabled = announcementEnabled,
            announcementTitle = announcementTitle,
            announcementContent = announcementContent,
            announcementLink = announcementLink.ifBlank { null },
            bgmEnabled = bgmEnabled,
            bgmConfig = if (bgmEnabled) bgmConfig else null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.modifyApp) },
                navigationIcon = {
                    IconButton(onClick = { if (!isProcessing) onBack() }) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    if (!isProcessing) {
                        // 克隆安装按钮（仅当没有自定义图标时可用）
                        if (newIconUri == null && newIconPath == null) {
                            TextButton(
                                onClick = {
                                    isProcessing = true
                                    scope.launch {
                                        try {
                                            val result = appCloner.cloneAndInstall(buildConfig()) { p, t ->
                                                scope.launch {
                                                    progress = p
                                                    progressText = t
                                                }
                                            }
                                            onResult(result)
                                        } catch (e: Exception) {
                                            onResult(AppModifyResult.Error(e.message ?: Strings.failed))
                                        }
                                    }
                                },
                                enabled = newAppName.isNotBlank()
                            ) {
                                Text(Strings.cloneInstall)
                            }
                        }
                        // 快捷方式按钮
                        TextButton(
                            onClick = {
                                isProcessing = true
                                scope.launch {
                                    try {
                                        val result = appCloner.createModifiedShortcut(buildConfig()) { p, t ->
                                            scope.launch {
                                                progress = p
                                                progressText = t
                                            }
                                        }
                                        onResult(result)
                                    } catch (e: Exception) {
                                        onResult(AppModifyResult.Error(e.message ?: Strings.failed))
                                    }
                                }
                            },
                            enabled = newAppName.isNotBlank()
                        ) {
                            Text(Strings.btnShortcut)
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
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
            // 进度条
            if (isProcessing) {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 原应用信息卡片
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    app.icon?.let { drawable ->
                        Image(
                            bitmap = drawable.toBitmap(56, 56).asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    } ?: Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Android, null)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(Strings.originalApp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(app.appName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${app.packageName} · v${app.versionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 基本信息卡片
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(Strings.labelBasicInfo, style = MaterialTheme.typography.titleMedium)
                    
                    // Icon选择（带图标库功能）
                    IconPickerWithLibrary(
                        iconUri = newIconUri,
                        iconPath = newIconPath,
                        onSelectFromGallery = { imagePickerLauncher.launch("image/*") },
                        onSelectFromLibrary = { path ->
                            newIconPath = path
                            newIconUri = null
                        }
                    )
                    
                    // 清除自定义图标按钮
                    if (newIconUri != null || newIconPath != null) {
                        TextButton(
                            onClick = { 
                                newIconUri = null
                                newIconPath = null 
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(Strings.useOriginalIcon, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    // 新名称（带随机按钮）
                    AppNameTextFieldSimple(
                        value = newAppName,
                        onValueChange = { newAppName = it }
                    )
                }
            }
            
            // Activation码设置卡片
            ActivationCard(
                enabled = activationEnabled,
                codes = activationCodes,
                requireEveryTime = activationRequireEveryTime,
                onEnabledChange = { activationEnabled = it },
                onCodesChange = { activationCodes = it },
                onRequireEveryTimeChange = { activationRequireEveryTime = it }
            )
            
            // Announcement设置卡片
            AnnouncementCardForModifier(
                enabled = announcementEnabled,
                title = announcementTitle,
                content = announcementContent,
                link = announcementLink,
                onEnabledChange = { announcementEnabled = it },
                onTitleChange = { announcementTitle = it },
                onContentChange = { announcementContent = it },
                onLinkChange = { announcementLink = it }
            )
            
            // Start画面设置卡片
            SplashCardForModifier(
                enabled = splashEnabled,
                splashType = splashType,
                splashPath = splashPath,
                duration = splashDuration,
                clickToSkip = splashClickToSkip,
                landscape = splashLandscape,
                fillScreen = splashFillScreen,
                enableAudio = splashEnableAudio,
                videoStartMs = splashVideoStartMs,
                videoEndMs = splashVideoEndMs,
                videoDurationMs = splashVideoDurationMs,
                onEnabledChange = { splashEnabled = it },
                onSelectImage = { splashImagePickerLauncher.launch("image/*") },
                onSelectVideo = { splashVideoPickerLauncher.launch("video/*") },
                onClearMedia = { 
                    splashPath = null
                    splashEnabled = false 
                },
                onDurationChange = { splashDuration = it },
                onClickToSkipChange = { splashClickToSkip = it },
                onLandscapeChange = { splashLandscape = it },
                onFillScreenChange = { splashFillScreen = it },
                onEnableAudioChange = { splashEnableAudio = it },
                onVideoTrimChange = { start, end, total ->
                    splashVideoStartMs = start
                    splashVideoEndMs = end
                    splashVideoDurationMs = total
                }
            )
            
            // Background music卡片
            BgmCard(
                enabled = bgmEnabled,
                config = bgmConfig,
                onEnabledChange = { bgmEnabled = it },
                onConfigChange = { bgmConfig = it }
            )
            
            // 提示信息
            WarningCard(message = Strings.cloneInstallWarning)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 激活码卡片（用于修改器）
 */
@Composable
private fun ActivationCard(
    enabled: Boolean,
    codes: List<String>,
    requireEveryTime: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<String>) -> Unit,
    onRequireEveryTimeChange: (Boolean) -> Unit
) {
    var newCode by remember { mutableStateOf("") }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CollapsibleCardHeader(
                icon = Icons.Outlined.Key,
                title = Strings.activationCodeVerify,
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    Strings.activationCodeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 每次启动都需要验证选项
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Strings.requireEveryLaunch, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (requireEveryTime) Strings.requireEveryLaunchHintOn else Strings.requireEveryLaunchHintOff,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = requireEveryTime, onCheckedChange = onRequireEveryTimeChange)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        placeholder = { Text(Strings.inputActivationCode) },
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
                        Icon(Icons.Default.Add, Strings.add)
                    }
                }
                
                codes.forEachIndexed { index, code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(code, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onCodesChange(codes.filterIndexed { i, _ -> i != index }) }) {
                            Icon(Icons.Outlined.Delete, Strings.btnDelete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 公告卡片（用于修改器）
 */
@Composable
private fun AnnouncementCardForModifier(
    enabled: Boolean,
    title: String,
    content: String,
    link: String,
    onEnabledChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onLinkChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CollapsibleCardHeader(
                icon = Icons.Outlined.Announcement,
                title = Strings.popupAnnouncement,
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(Strings.announcementTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text(Strings.announcementContent) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = link,
                    onValueChange = onLinkChange,
                    label = { Text(Strings.linkUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 启动画面卡片（用于修改器）
 */
@Composable
private fun SplashCardForModifier(
    enabled: Boolean,
    splashType: String,
    splashPath: String?,
    duration: Int,
    clickToSkip: Boolean,
    landscape: Boolean,
    fillScreen: Boolean,
    enableAudio: Boolean,
    videoStartMs: Long,
    videoEndMs: Long,
    videoDurationMs: Long,
    onEnabledChange: (Boolean) -> Unit,
    onSelectImage: () -> Unit,
    onSelectVideo: () -> Unit,
    onClearMedia: () -> Unit,
    onDurationChange: (Int) -> Unit,
    onClickToSkipChange: (Boolean) -> Unit,
    onLandscapeChange: (Boolean) -> Unit,
    onFillScreenChange: (Boolean) -> Unit,
    onEnableAudioChange: (Boolean) -> Unit,
    onVideoTrimChange: (Long, Long, Long) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CollapsibleCardHeader(
                icon = Icons.Outlined.PlayCircle,
                title = Strings.splashScreen,
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onSelectImage, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Image, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(Strings.selectImage)
                    }
                    OutlinedButton(onClick = onSelectVideo, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.VideoFile, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(Strings.selectVideo)
                    }
                }
                
                if (splashPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (splashType == "IMAGE") Icons.Outlined.Image else Icons.Outlined.VideoFile,
                                null, Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (splashType == "IMAGE") Strings.image else Strings.video, style = MaterialTheme.typography.bodyMedium)
                                Text(File(splashPath).name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            IconButton(onClick = onClearMedia) {
                                Icon(Icons.Default.Clear, Strings.clear)
                            }
                        }
                    }
                    
                    if (splashType == "VIDEO") {
                        VideoTrimmer(
                            videoPath = splashPath,
                            startMs = videoStartMs,
                            endMs = videoEndMs,
                            videoDurationMs = videoDurationMs,
                            onTrimChange = onVideoTrimChange
                        )
                    }
                    
                    if (splashType == "IMAGE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${Strings.splashScreen}: $duration ${Strings.seconds}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = duration.toFloat(),
                            onValueChange = { onDurationChange(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    SettingsSwitchRow(Strings.allowClickToSkip, clickToSkip, onClickToSkipChange)
                    SettingsSwitchRow(Strings.landscapeMode, landscape, onLandscapeChange)
                    SettingsSwitchRow(Strings.fillScreen, fillScreen, onFillScreenChange)
                    if (splashType == "VIDEO") {
                        SettingsSwitchRow(Strings.enableAudioLabel, enableAudio, onEnableAudioChange)
                    }
                }
            }
        }
    }
}

