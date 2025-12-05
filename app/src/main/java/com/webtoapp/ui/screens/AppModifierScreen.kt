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
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.ui.components.BgmCard
import com.webtoapp.ui.components.IconPickerWithLibrary
import com.webtoapp.ui.components.VideoTrimmer
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
    
    // 加载应用列表
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
                            snackbarHostState.showSnackbar("快捷方式创建成功")
                        }
                        is AppModifyResult.CloneSuccess -> {
                            snackbarHostState.showSnackbar("克隆成功，请确认安装")
                        }
                        is AppModifyResult.Error -> {
                            snackbarHostState.showSnackbar("失败: ${result.message}")
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
                title = { Text("应用图标修改") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索应用...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "清除")
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
                    label = { Text("用户应用") },
                    leadingIcon = if (filterType == AppFilterType.USER) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == AppFilterType.SYSTEM,
                    onClick = { filterType = AppFilterType.SYSTEM },
                    label = { Text("系统应用") },
                    leadingIcon = if (filterType == AppFilterType.SYSTEM) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == AppFilterType.ALL,
                    onClick = { filterType = AppFilterType.ALL },
                    label = { Text("全部") },
                    leadingIcon = if (filterType == AppFilterType.ALL) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 应用数量
            Text(
                text = "共 ${apps.size} 个应用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 应用列表
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
    ElevatedCard(
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
            // 图标
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
    
    // 启动画面配置状态
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
    
    // 激活码配置状态
    var activationEnabled by remember { mutableStateOf(false) }
    var activationCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var newActivationCode by remember { mutableStateOf("") }
    
    // 公告配置状态
    var announcementEnabled by remember { mutableStateOf(false) }
    var announcementTitle by remember { mutableStateOf("") }
    var announcementContent by remember { mutableStateOf("") }
    var announcementLink by remember { mutableStateOf("") }
    
    // 背景音乐配置状态
    var bgmEnabled by remember { mutableStateOf(false) }
    var bgmConfig by remember { mutableStateOf(BgmConfig()) }
    
    // 图片选择器（相册选择）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            newIconUri = it
            newIconPath = null // 清除图标库路径
        }
    }
    
    // 启动画面图片选择器
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
    
    // 启动画面视频选择器
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
    
    // 构建配置并执行操作
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
                title = { Text("修改应用") },
                navigationIcon = {
                    IconButton(onClick = { if (!isProcessing) onBack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
                                            onResult(AppModifyResult.Error(e.message ?: "克隆失败"))
                                        }
                                    }
                                },
                                enabled = newAppName.isNotBlank()
                            ) {
                                Text("克隆安装")
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
                                        onResult(AppModifyResult.Error(e.message ?: "创建快捷方式失败"))
                                    }
                                }
                            },
                            enabled = newAppName.isNotBlank()
                        ) {
                            Text("快捷方式")
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
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        Text("原应用", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("基本信息", style = MaterialTheme.typography.titleMedium)
                    
                    // 图标选择（带图标库功能）
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
                            Text("使用原图标", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    // 新名称
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("应用名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // 激活码设置卡片
            ActivationCard(
                enabled = activationEnabled,
                codes = activationCodes,
                onEnabledChange = { activationEnabled = it },
                onCodesChange = { activationCodes = it }
            )
            
            // 公告设置卡片
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
            
            // 启动画面设置卡片
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
            
            // 背景音乐卡片
            BgmCard(
                enabled = bgmEnabled,
                config = bgmConfig,
                onEnabledChange = { bgmEnabled = it },
                onConfigChange = { bgmConfig = it }
            )
            
            // 提示信息
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Warning,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "克隆安装仅适用于无签名校验的应用，兼容性较差。建议优先使用「快捷方式」功能。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
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
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<String>) -> Unit
) {
    var newCode by remember { mutableStateOf("") }
    
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Key, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("激活码验证", style = MaterialTheme.typography.titleMedium)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "启用后，用户需要输入正确的激活码才能使用应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
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
                            Icon(Icons.Outlined.Delete, "删除", tint = MaterialTheme.colorScheme.error)
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Announcement, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("弹窗公告", style = MaterialTheme.typography.titleMedium)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("公告标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("公告内容") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = link,
                    onValueChange = onLinkChange,
                    label = { Text("链接地址（可选）") },
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PlayCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("启动画面", style = MaterialTheme.typography.titleMedium)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onSelectImage, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Image, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("选择图片")
                    }
                    OutlinedButton(onClick = onSelectVideo, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.VideoFile, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("选择视频")
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
                                Text(if (splashType == "IMAGE") "已选择图片" else "已选择视频", style = MaterialTheme.typography.bodyMedium)
                                Text(File(splashPath).name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            IconButton(onClick = onClearMedia) {
                                Icon(Icons.Default.Clear, "清除")
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
                        Text("显示时长：$duration 秒", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = duration.toFloat(),
                            onValueChange = { onDurationChange(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    SettingsSwitchRow("允许点击跳过", clickToSkip, onClickToSkipChange)
                    SettingsSwitchRow("横屏显示", landscape, onLandscapeChange)
                    SettingsSwitchRow("铺满屏幕", fillScreen, onFillScreenChange)
                    if (splashType == "VIDEO") {
                        SettingsSwitchRow("启用音频", enableAudio, onEnableAudioChange)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
