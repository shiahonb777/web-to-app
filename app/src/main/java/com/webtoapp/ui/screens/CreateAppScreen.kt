package com.webtoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.VideoTrimmer
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.viewmodel.UiState
import com.webtoapp.util.SplashStorage

/**
 * 创建/编辑应用页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppScreen(
    viewModel: MainViewModel,
    isEdit: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // 处理保存结果
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onSaved()
            viewModel.resetUiState()
        }
    }

    // 图片选择器 - 选择后复制到私有目录实现持久化
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handleIconSelected(it)
        }
    }

    // 启动画面图片选择器
    val splashImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handleSplashMediaSelected(it, isVideo = false)
        }
    }

    // 启动画面视频选择器
    val splashVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handleSplashMediaSelected(it, isVideo = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑应用" else "创建应用") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveApp() },
                        enabled = uiState !is UiState.Loading
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
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
            // 基本信息卡片
            BasicInfoCard(
                editState = editState,
                onNameChange = { viewModel.updateEditState { copy(name = it) } },
                onUrlChange = { viewModel.updateEditState { copy(url = it) } },
                onSelectIcon = { imagePickerLauncher.launch("image/*") }
            )

            // 激活码设置
            ActivationCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(activationEnabled = it) } },
                onCodesChange = { viewModel.updateEditState { copy(activationCodes = it) } }
            )

            // 公告设置
            AnnouncementCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(announcementEnabled = it) } },
                onAnnouncementChange = { viewModel.updateEditState { copy(announcement = it) } }
            )

            // 广告拦截设置
            AdBlockCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(adBlockEnabled = it) } },
                onRulesChange = { viewModel.updateEditState { copy(adBlockRules = it) } }
            )

            // 访问电脑版
            DesktopModeCard(
                enabled = editState.webViewConfig.desktopMode,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(desktopMode = it))
                    }
                }
            )

            // 全屏模式
            FullscreenModeCard(
                enabled = editState.webViewConfig.hideToolbar,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(hideToolbar = it))
                    }
                }
            )

            // 启动画面
            SplashScreenCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(splashEnabled = it) } },
                onSelectImage = { splashImagePickerLauncher.launch("image/*") },
                onSelectVideo = { splashVideoPickerLauncher.launch("video/*") },
                onDurationChange = { 
                    viewModel.updateEditState { 
                        copy(splashConfig = splashConfig.copy(duration = it)) 
                    } 
                },
                onClickToSkipChange = {
                    viewModel.updateEditState {
                        copy(splashConfig = splashConfig.copy(clickToSkip = it))
                    }
                },
                onVideoTrimChange = { startMs, endMs, totalDurationMs ->
                    viewModel.updateEditState {
                        copy(splashConfig = splashConfig.copy(
                            videoStartMs = startMs,
                            videoEndMs = endMs,
                            videoDurationMs = totalDurationMs
                        ))
                    }
                },
                onClearMedia = { viewModel.clearSplashMedia() }
            )

            // WebView高级设置
            WebViewConfigCard(
                config = editState.webViewConfig,
                onConfigChange = { viewModel.updateEditState { copy(webViewConfig = it) } }
            )

            // 错误提示
            if (uiState is UiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = (uiState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 基本信息卡片
 */
@Composable
fun BasicInfoCard(
    editState: EditState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSelectIcon: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleMedium
            )

            // 图标选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { onSelectIcon() },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (editState.iconUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(editState.iconUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "应用图标",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "选择图标",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "应用图标",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "点击选择图片作为图标",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 应用名称
            OutlinedTextField(
                value = editState.name,
                onValueChange = onNameChange,
                label = { Text("应用名称") },
                placeholder = { Text("输入应用显示名称") },
                leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // 网站地址
            OutlinedTextField(
                value = editState.url,
                onValueChange = onUrlChange,
                label = { Text("网站地址") },
                placeholder = { Text("https://example.com") },
                leadingIcon = { Icon(Icons.Outlined.Link, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                )
            )
        }
    }
}

/**
 * 激活码设置卡片
 */
@Composable
fun ActivationCard(
    editState: EditState,
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
                    checked = editState.activationEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (editState.activationEnabled) {
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
                                onCodesChange(editState.activationCodes + newCode)
                                newCode = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "添加")
                    }
                }

                // 激活码列表
                editState.activationCodes.forEachIndexed { index, code ->
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
                                onCodesChange(editState.activationCodes.filterIndexed { i, _ -> i != index })
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

/**
 * 公告设置卡片
 */
@Composable
fun AnnouncementCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onAnnouncementChange: (Announcement) -> Unit
) {
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
                        Icons.Outlined.Announcement,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "弹窗公告",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = editState.announcementEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (editState.announcementEnabled) {
                OutlinedTextField(
                    value = editState.announcement.title,
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(title = it))
                    },
                    label = { Text("公告标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editState.announcement.content,
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(content = it))
                    },
                    label = { Text("公告内容") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editState.announcement.linkUrl ?: "",
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(linkUrl = it.ifBlank { null }))
                    },
                    label = { Text("链接地址（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("仅显示一次")
                    Switch(
                        checked = editState.announcement.showOnce,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(showOnce = it))
                        }
                    )
                }
            }
        }
    }
}

/**
 * 广告拦截卡片
 */
@Composable
fun AdBlockCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<String>) -> Unit
) {
    var newRule by remember { mutableStateOf("") }

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
                        Icons.Outlined.Block,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "广告拦截",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = editState.adBlockEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (editState.adBlockEnabled) {
                Text(
                    text = "启用后将自动拦截网页中的广告内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "自定义拦截规则（可选）",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newRule,
                        onValueChange = { newRule = it },
                        placeholder = { Text("如：ads.example.com") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (newRule.isNotBlank()) {
                                onRulesChange(editState.adBlockRules + newRule)
                                newRule = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "添加")
                    }
                }

                editState.adBlockRules.forEachIndexed { index, rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onRulesChange(editState.adBlockRules.filterIndexed { i, _ -> i != index })
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

/**
 * WebView配置卡片
 */
@Composable
fun WebViewConfigCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "高级设置",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                SettingsSwitch(
                    title = "JavaScript",
                    subtitle = "启用JavaScript执行",
                    checked = config.javaScriptEnabled,
                    onCheckedChange = { onConfigChange(config.copy(javaScriptEnabled = it)) }
                )

                SettingsSwitch(
                    title = "DOM存储",
                    subtitle = "启用本地存储功能",
                    checked = config.domStorageEnabled,
                    onCheckedChange = { onConfigChange(config.copy(domStorageEnabled = it)) }
                )

                SettingsSwitch(
                    title = "缩放功能",
                    subtitle = "允许用户缩放页面",
                    checked = config.zoomEnabled,
                    onCheckedChange = { onConfigChange(config.copy(zoomEnabled = it)) }
                )

                SettingsSwitch(
                    title = "下拉刷新",
                    subtitle = "允许下拉刷新页面",
                    checked = config.swipeRefreshEnabled,
                    onCheckedChange = { onConfigChange(config.copy(swipeRefreshEnabled = it)) }
                )

                SettingsSwitch(
                    title = "桌面模式",
                    subtitle = "以桌面版网页模式加载",
                    checked = config.desktopMode,
                    onCheckedChange = { onConfigChange(config.copy(desktopMode = it)) }
                )

                SettingsSwitch(
                    title = "全屏视频",
                    subtitle = "允许视频全屏播放",
                    checked = config.fullscreenEnabled,
                    onCheckedChange = { onConfigChange(config.copy(fullscreenEnabled = it)) }
                )

                SettingsSwitch(
                    title = "外部链接",
                    subtitle = "在浏览器中打开外部链接",
                    checked = config.openExternalLinks,
                    onCheckedChange = { onConfigChange(config.copy(openExternalLinks = it)) }
                )
            }
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 访问电脑版卡片
 */
@Composable
fun DesktopModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Computer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "访问电脑版",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "强制使用桌面版网站",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

/**
 * 全屏模式卡片
 */
@Composable
fun FullscreenModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Fullscreen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "全屏模式",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "隐藏工具栏，无浏览器特征",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

/**
 * 检查媒体文件是否存在
 */
fun checkMediaExists(context: android.content.Context, uri: android.net.Uri?, savedPath: String?): Boolean {
    // 优先检查保存的路径
    if (!savedPath.isNullOrEmpty()) {
        return java.io.File(savedPath).exists()
    }
    // 检查 URI
    if (uri != null) {
        return try {
            context.contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    return false
}

/**
 * 启动画面设置卡片
 */
@Composable
fun SplashScreenCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onSelectImage: () -> Unit,
    onSelectVideo: () -> Unit,
    onDurationChange: (Int) -> Unit,
    onClickToSkipChange: (Boolean) -> Unit,
    onVideoTrimChange: (startMs: Long, endMs: Long, totalDurationMs: Long) -> Unit,
    onClearMedia: () -> Unit
) {
    val context = LocalContext.current
    
    // 检查媒体文件是否存在
    val mediaExists = remember(editState.splashMediaUri, editState.savedSplashPath) {
        checkMediaExists(context, editState.splashMediaUri, editState.savedSplashPath)
    }
    
    // 如果媒体不存在但 URI 非空，自动清除
    LaunchedEffect(mediaExists, editState.splashMediaUri) {
        if (!mediaExists && editState.splashMediaUri != null) {
            onClearMedia()
        }
    }
    
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PlayCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "启动画面",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = editState.splashEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (editState.splashEnabled) {
                Text(
                    text = "设置应用启动时显示的图片或视频（视频限5秒内）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 媒体预览区域
                if (editState.splashMediaUri != null && mediaExists) {
                    if (editState.splashConfig.type == SplashType.VIDEO) {
                        // 视频裁剪器
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "视频裁剪",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                TextButton(onClick = onClearMedia) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("移除", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            
                            VideoTrimmer(
                                videoPath = editState.savedSplashPath ?: editState.splashMediaUri.toString(),
                                startMs = editState.splashConfig.videoStartMs,
                                endMs = editState.splashConfig.videoEndMs,
                                videoDurationMs = editState.splashConfig.videoDurationMs,
                                onTrimChange = onVideoTrimChange
                            )
                        }
                    } else {
                        // 图片预览
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(editState.splashMediaUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "启动画面预览",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // 删除按钮
                                IconButton(
                                    onClick = onClearMedia,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "移除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 空状态 - 选择媒体
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.medium
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击下方按钮选择图片或视频",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 选择按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择图片")
                    }
                    OutlinedButton(
                        onClick = onSelectVideo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.VideoFile, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择视频")
                    }
                }

                // 显示时长设置（仅图片显示，视频使用裁剪范围）
                if (editState.splashConfig.type == SplashType.IMAGE || editState.splashMediaUri == null) {
                    Column {
                        Text(
                            text = "显示时长：${editState.splashConfig.duration} 秒",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = editState.splashConfig.duration.toFloat(),
                            onValueChange = { onDurationChange(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 点击跳过设置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("允许点击跳过", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "用户可点击屏幕跳过启动画面",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editState.splashConfig.clickToSkip,
                        onCheckedChange = onClickToSkipChange
                    )
                }
            }
        }
    }
}
