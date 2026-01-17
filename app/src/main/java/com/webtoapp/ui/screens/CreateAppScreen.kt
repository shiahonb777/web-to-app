package com.webtoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.ActivationCodeCard
import com.webtoapp.ui.components.AutoStartCard
import com.webtoapp.ui.components.BgmCard
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.IconPickerWithLibrary
import com.webtoapp.ui.components.StatusBarConfigCard
import com.webtoapp.ui.components.VideoTrimmer
import com.webtoapp.ui.components.announcement.AnnouncementDialog
import com.webtoapp.ui.components.announcement.AnnouncementConfig
import com.webtoapp.ui.components.announcement.AnnouncementTemplate
import com.webtoapp.ui.components.announcement.AnnouncementTemplateSelector
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
                title = { Text(if (isEdit) Strings.editApp else Strings.createApp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
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
                            Text(Strings.btnSave)
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
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息卡片
            BasicInfoCard(
                editState = editState,
                onNameChange = { viewModel.updateEditState { copy(name = it) } },
                onUrlChange = { viewModel.updateEditState { copy(url = it) } },
                onSelectIcon = { imagePickerLauncher.launch("image/*") },
                onSelectIconFromLibrary = { path ->
                    viewModel.updateEditState { copy(savedIconPath = path, iconUri = null) }
                }
            )

            // 激活码设置
            ActivationCodeCard(
                enabled = editState.activationEnabled,
                activationCodes = editState.activationCodeList,
                requireEveryTime = editState.activationRequireEveryTime,
                onEnabledChange = { viewModel.updateEditState { copy(activationEnabled = it) } },
                onCodesChange = { viewModel.updateEditState { copy(activationCodeList = it) } },
                onRequireEveryTimeChange = { viewModel.updateEditState { copy(activationRequireEveryTime = it) } }
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

            // 扩展模块设置
            com.webtoapp.ui.components.ExtensionModuleCard(
                enabled = editState.extensionModuleEnabled,
                selectedModuleIds = editState.extensionModuleIds,
                onEnabledChange = { viewModel.updateEditState { copy(extensionModuleEnabled = it) } },
                onModuleIdsChange = { viewModel.updateEditState { copy(extensionModuleIds = it) } }
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
                showStatusBar = editState.webViewConfig.showStatusBarInFullscreen,
                webViewConfig = editState.webViewConfig,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(hideToolbar = it))
                    }
                },
                onShowStatusBarChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(showStatusBarInFullscreen = it))
                    }
                },
                onWebViewConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(webViewConfig = newConfig)
                    }
                }
            )

            // 横屏模式
            LandscapeModeCard(
                enabled = editState.webViewConfig.landscapeMode,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(landscapeMode = it))
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
                onOrientationChange = {
                    viewModel.updateEditState {
                        copy(splashConfig = splashConfig.copy(orientation = it))
                    }
                },
                onFillScreenChange = {
                    viewModel.updateEditState {
                        copy(splashConfig = splashConfig.copy(fillScreen = it))
                    }
                },
                onEnableAudioChange = {
                    viewModel.updateEditState {
                        copy(splashConfig = splashConfig.copy(enableAudio = it))
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

            // 背景音乐
            BgmCard(
                enabled = editState.bgmEnabled,
                config = editState.bgmConfig,
                onEnabledChange = { viewModel.updateEditState { copy(bgmEnabled = it) } },
                onConfigChange = { viewModel.updateEditState { copy(bgmConfig = it) } }
            )

            // 网页自动翻译
            TranslateCard(
                enabled = editState.translateEnabled,
                config = editState.translateConfig,
                onEnabledChange = { viewModel.updateEditState { copy(translateEnabled = it) } },
                onConfigChange = { viewModel.updateEditState { copy(translateConfig = it) } }
            )
            
            // 自启动设置
            AutoStartCard(
                config = editState.autoStartConfig,
                onConfigChange = { viewModel.updateEditState { copy(autoStartConfig = it) } }
            )
            
            // 强制运行设置
            com.webtoapp.ui.components.ForcedRunConfigCard(
                config = editState.forcedRunConfig,
                onConfigChange = { viewModel.updateEditState { copy(forcedRunConfig = it) } }
            )

            // WebView高级设置
            WebViewConfigCard(
                config = editState.webViewConfig,
                onConfigChange = { viewModel.updateEditState { copy(webViewConfig = it) } },
                apkExportConfig = editState.apkExportConfig,
                onApkExportConfigChange = { viewModel.updateEditState { copy(apkExportConfig = it) } }
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
    onSelectIcon: () -> Unit,
    onSelectIconFromLibrary: (String) -> Unit = {}
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = Strings.labelBasicInfo,
                style = MaterialTheme.typography.titleMedium
            )

            // 图标选择（带图标库功能）
            IconPickerWithLibrary(
                iconUri = editState.iconUri,
                iconPath = editState.savedIconPath,
                onSelectFromGallery = onSelectIcon,
                onSelectFromLibrary = onSelectIconFromLibrary
            )

            // 应用名称
            OutlinedTextField(
                value = editState.name,
                onValueChange = onNameChange,
                label = { Text(Strings.labelAppName) },
                placeholder = { Text(Strings.inputAppName) },
                leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // 根据应用类型显示不同内容
            when (editState.appType) {
                AppType.WEB -> {
                    // 网站地址输入框（仅 WEB 类型）
                    OutlinedTextField(
                        value = editState.url,
                        onValueChange = onUrlChange,
                        label = { Text(Strings.labelUrl) },
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
                AppType.HTML -> {
                    // HTML 应用显示文件信息
                    val htmlConfig = editState.htmlConfig
                    val fileCount = htmlConfig?.files?.size ?: 0
                    val entryFile = htmlConfig?.entryFile?.takeIf { it.isNotBlank() } ?: "index.html"
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Strings.htmlApp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${Strings.entryFile}: $entryFile · ${Strings.totalFilesCount.replace("%d", fileCount.toString())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                AppType.IMAGE, AppType.VIDEO -> {
                    // 媒体应用显示文件路径
                    val mediaPath = editState.url
                    val isVideo = editState.appType == AppType.VIDEO
                    val fileName = mediaPath.substringAfterLast("/", Strings.unknownFile)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isVideo) Strings.videoApp else Strings.imageApp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1
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
 * 激活码设置卡片
 */
@Composable
fun ActivationCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<String>) -> Unit
) {
    var newCode by remember { mutableStateOf("") }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        text = Strings.activationCodeVerify,
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
                    text = Strings.activationCodeHint,
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
                        placeholder = { Text(Strings.inputActivationCode) },
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
                        Icon(Icons.Default.Add, Strings.add)
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
                                Strings.btnDelete,
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
 * 公告设置卡片 - 支持多种精美模板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onAnnouncementChange: (Announcement) -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    
    // 预览弹窗
    if (showPreview && (editState.announcement.title.isNotBlank() || editState.announcement.content.isNotBlank())) {
        com.webtoapp.ui.components.announcement.AnnouncementDialog(
            config = com.webtoapp.ui.components.announcement.AnnouncementConfig(
                announcement = editState.announcement,
                template = com.webtoapp.ui.components.announcement.AnnouncementTemplate.valueOf(
                    editState.announcement.template.name
                ),
                showEmoji = editState.announcement.showEmoji,
                animationEnabled = editState.announcement.animationEnabled
            ),
            onDismiss = { showPreview = false },
            onLinkClick = { /* 预览模式不处理链接 */ }
        )
    }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        text = Strings.popupAnnouncement,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = editState.announcementEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (editState.announcementEnabled) {
                // 模板选择器
                com.webtoapp.ui.components.announcement.AnnouncementTemplateSelector(
                    selectedTemplate = com.webtoapp.ui.components.announcement.AnnouncementTemplate.valueOf(
                        editState.announcement.template.name
                    ),
                    onTemplateSelected = { template ->
                        onAnnouncementChange(
                            editState.announcement.copy(
                                template = com.webtoapp.data.model.AnnouncementTemplateType.valueOf(template.name)
                            )
                        )
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                OutlinedTextField(
                    value = editState.announcement.title,
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(title = it))
                    },
                    label = { Text(Strings.announcementTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editState.announcement.content,
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(content = it))
                    },
                    label = { Text(Strings.announcementContent) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editState.announcement.linkUrl ?: "",
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(linkUrl = it.ifBlank { null }))
                    },
                    label = { Text(Strings.linkUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!editState.announcement.linkUrl.isNullOrBlank()) {
                    OutlinedTextField(
                        value = editState.announcement.linkText ?: "",
                        onValueChange = {
                            onAnnouncementChange(editState.announcement.copy(linkText = it.ifBlank { null }))
                        },
                        label = { Text(Strings.linkButtonText) },
                        placeholder = { Text(Strings.viewDetails) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 显示频率选择
                Text(
                    Strings.displayFrequency,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = editState.announcement.showOnce,
                        onClick = { onAnnouncementChange(editState.announcement.copy(showOnce = true)) },
                        label = { Text(Strings.showOnce) },
                        leadingIcon = if (editState.announcement.showOnce) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = !editState.announcement.showOnce,
                        onClick = { onAnnouncementChange(editState.announcement.copy(showOnce = false)) },
                        label = { Text(Strings.everyLaunch) },
                        leadingIcon = if (!editState.announcement.showOnce) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
                
                // 高级选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = editState.announcement.showEmoji,
                            onCheckedChange = {
                                onAnnouncementChange(editState.announcement.copy(showEmoji = it))
                            }
                        )
                        Text(Strings.showEmoji, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = editState.announcement.animationEnabled,
                            onCheckedChange = {
                                onAnnouncementChange(editState.announcement.copy(animationEnabled = it))
                            }
                        )
                        Text(Strings.enableAnimation, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // 预览按钮
                OutlinedButton(
                    onClick = { showPreview = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editState.announcement.title.isNotBlank() || editState.announcement.content.isNotBlank()
                ) {
                    Icon(Icons.Outlined.Preview, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.previewAnnouncementEffect)
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

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        text = Strings.adBlocking,
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
                    text = Strings.adBlockDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = Strings.customBlockRules,
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newRule,
                        onValueChange = { newRule = it },
                        placeholder = { Text(Strings.adBlockRuleHint) },
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
                        Icon(Icons.Default.Add, Strings.add)
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
                                Strings.delete,
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
    onConfigChange: (WebViewConfig) -> Unit,
    apkExportConfig: ApkExportConfig = ApkExportConfig(),
    onApkExportConfigChange: (ApkExportConfig) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        text = Strings.advancedSettings,
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
                    subtitle = Strings.enableJavaScript,
                    checked = config.javaScriptEnabled,
                    onCheckedChange = { onConfigChange(config.copy(javaScriptEnabled = it)) }
                )

                SettingsSwitch(
                    title = Strings.domStorageSetting,
                    subtitle = Strings.domStorageSettingHint,
                    checked = config.domStorageEnabled,
                    onCheckedChange = { onConfigChange(config.copy(domStorageEnabled = it)) }
                )

                SettingsSwitch(
                    title = Strings.zoomSetting,
                    subtitle = Strings.zoomSettingHint,
                    checked = config.zoomEnabled,
                    onCheckedChange = { onConfigChange(config.copy(zoomEnabled = it)) }
                )

                SettingsSwitch(
                    title = Strings.swipeRefreshSetting,
                    subtitle = Strings.swipeRefreshSettingHint,
                    checked = config.swipeRefreshEnabled,
                    onCheckedChange = { onConfigChange(config.copy(swipeRefreshEnabled = it)) }
                )

                SettingsSwitch(
                    title = Strings.desktopModeSetting,
                    subtitle = Strings.desktopModeSettingHint,
                    checked = config.desktopMode,
                    onCheckedChange = { onConfigChange(config.copy(desktopMode = it)) }
                )

                SettingsSwitch(
                    title = Strings.fullscreenVideoSetting,
                    subtitle = Strings.fullscreenVideoSettingHint,
                    checked = config.fullscreenEnabled,
                    onCheckedChange = { onConfigChange(config.copy(fullscreenEnabled = it)) }
                )

                SettingsSwitch(
                    title = Strings.externalLinksSetting,
                    subtitle = Strings.externalLinksSettingHint,
                    checked = config.openExternalLinks,
                    onCheckedChange = { onConfigChange(config.copy(openExternalLinks = it)) }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // APK 导出配置
                ApkExportSection(
                    config = apkExportConfig,
                    onConfigChange = onApkExportConfigChange
                )
            }
        }
    }
}

/**
 * APK 导出配置区域
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApkExportSection(
    config: ApkExportConfig,
    onConfigChange: (ApkExportConfig) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val packageNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val versionNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val versionCodeBringIntoViewRequester = remember { BringIntoViewRequester() }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Android,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Strings.apkExportConfig,
                style = MaterialTheme.typography.titleSmall
            )
        }
        
        Text(
            text = Strings.apkConfigNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        
        // 自定义包名（最大12字符，因为二进制替换限制）
        val maxPackageLength = 12
        val packageName = config.customPackageName ?: ""
        val isPackageNameTooLong = packageName.length > maxPackageLength
        val isPackageNameInvalid = packageName.isNotBlank() && 
            !packageName.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
        
        OutlinedTextField(
            value = packageName,
            onValueChange = { 
                onConfigChange(config.copy(customPackageName = it.ifBlank { null }))
            },
            label = { Text(Strings.customPackageName) },
            placeholder = { Text("com.w2a.app") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(packageNameBringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            packageNameBringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            isError = isPackageNameTooLong || isPackageNameInvalid,
            supportingText = { 
                when {
                    isPackageNameTooLong -> Text(
                        Strings.packageNameTooLong.replace("%d", maxPackageLength.toString()).replace("%d", packageName.length.toString()),
                        color = MaterialTheme.colorScheme.error
                    )
                    isPackageNameInvalid -> Text(
                        Strings.packageNameInvalidFormat,
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> Text(Strings.packageNameHint.replace("%d", maxPackageLength.toString())) 
                }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 版本名和版本号
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = config.customVersionName ?: "",
                onValueChange = { 
                    onConfigChange(config.copy(customVersionName = it.ifBlank { null }))
                },
                label = { Text(Strings.versionName) },
                placeholder = { Text("1.0.0") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .bringIntoViewRequester(versionNameBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                versionNameBringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
            )
            
            OutlinedTextField(
                value = config.customVersionCode?.toString() ?: "",
                onValueChange = { input ->
                    val code = input.filter { it.isDigit() }.toIntOrNull()
                    onConfigChange(config.copy(customVersionCode = code))
                },
                label = { Text(Strings.versionCode) },
                placeholder = { Text("1") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .bringIntoViewRequester(versionCodeBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                versionCodeBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
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
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                Text(
                    text = Strings.desktopMode,
                    style = MaterialTheme.typography.titleMedium
                )
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
    showStatusBar: Boolean = false,
    webViewConfig: WebViewConfig = WebViewConfig(),
    onEnabledChange: (Boolean) -> Unit,
    onShowStatusBarChange: (Boolean) -> Unit = {},
    onWebViewConfigChange: (WebViewConfig) -> Unit = {}
) {
    var statusBarConfigExpanded by remember { mutableStateOf(false) }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = Strings.fullscreenMode,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            // 全屏模式下显示状态栏选项
            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.showStatusBar,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Strings.showStatusBarHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showStatusBar,
                        onCheckedChange = onShowStatusBarChange
                    )
                }
                
                // 状态栏配置（仅在显示状态栏时可用）
                if (showStatusBar) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 状态栏配置展开/收起
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { statusBarConfigExpanded = !statusBarConfigExpanded },
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = Strings.statusBarStyleConfigLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Icon(
                                if (statusBarConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    // 状态栏配置内容
                    if (statusBarConfigExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusBarConfigCard(
                            config = webViewConfig,
                            onConfigChange = onWebViewConfigChange
                        )
                    }
                }
            }
        }
    }
}

/**
 * 横屏模式卡片
 */
@Composable
fun LandscapeModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ScreenRotation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Strings.landscapeModeLabel,
                    style = MaterialTheme.typography.titleMedium
                )
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
    onOrientationChange: (SplashOrientation) -> Unit,
    onFillScreenChange: (Boolean) -> Unit,
    onEnableAudioChange: (Boolean) -> Unit,
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
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        text = Strings.splashScreen,
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
                    text = Strings.splashHint,
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
                                    Strings.videoCrop,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                TextButton(onClick = onClearMedia) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(Strings.remove, style = MaterialTheme.typography.labelSmall)
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
                                    contentDescription = Strings.splashPreview,
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
                                        Strings.remove,
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
                                text = Strings.clickToSelectImageOrVideo,
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
                        Text(Strings.selectImage)
                    }
                    OutlinedButton(
                        onClick = onSelectVideo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.VideoFile, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.selectVideo)
                    }
                }

                // 以下设置仅在上传媒体后显示
                if (editState.splashMediaUri != null && mediaExists) {
                    // 显示时长设置（仅图片显示，视频使用裁剪范围）
                    if (editState.splashConfig.type == SplashType.IMAGE) {
                        Column {
                            Text(
                                text = Strings.displayDurationSeconds.replace("%d", editState.splashConfig.duration.toString()),
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
                            Text(Strings.allowSkip, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Strings.allowSkipHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editState.splashConfig.clickToSkip,
                            onCheckedChange = onClickToSkipChange
                        )
                    }
                    
                    // 显示方向设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(Strings.landscapeDisplay, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Strings.landscapeDisplayHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editState.splashConfig.orientation == SplashOrientation.LANDSCAPE,
                            onCheckedChange = { isLandscape ->
                                onOrientationChange(
                                    if (isLandscape) SplashOrientation.LANDSCAPE 
                                    else SplashOrientation.PORTRAIT
                                )
                            }
                        )
                    }
                    
                    // 铺满屏幕设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(Strings.fillScreen, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Strings.fillScreenHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editState.splashConfig.fillScreen,
                            onCheckedChange = onFillScreenChange
                        )
                    }
                    
                    // 启用音频设置（仅视频类型显示）
                    if (editState.splashConfig.type == SplashType.VIDEO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(Strings.enableAudio, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Strings.enableAudioHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = editState.splashConfig.enableAudio,
                                onCheckedChange = onEnableAudioChange
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 导出应用主题选择卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeCard(
    selectedTheme: String,
    onThemeChange: (String) -> Unit
) {
    // 主题选项列表 - 使用本地化名称
    val themeOptions = listOf(
        "AURORA" to Strings.themeAurora,
        "CYBERPUNK" to Strings.themeCyberpunk,
        "SAKURA" to Strings.themeSakura,
        "OCEAN" to Strings.themeOcean,
        "FOREST" to Strings.themeForest,
        "GALAXY" to Strings.themeGalaxy,
        "VOLCANO" to Strings.themeVolcano,
        "FROST" to Strings.themeFrost,
        "SUNSET" to Strings.themeSunset,
        "MINIMAL" to Strings.themeMinimal,
        "NEON_TOKYO" to Strings.themeNeonTokyo,
        "LAVENDER" to Strings.themeLavender
    )
    
    var expanded by remember { mutableStateOf(false) }
    val selectedDisplayName = themeOptions.find { it.first == selectedTheme }?.second ?: Strings.themeAurora

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Palette,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Strings.exportAppTheme,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Text(
                text = Strings.exportAppThemeHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(Strings.selectTheme) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    themeOptions.forEach { (themeKey, themeName) ->
                        DropdownMenuItem(
                            text = { Text(themeName) },
                            onClick = {
                                onThemeChange(themeKey)
                                expanded = false
                            },
                            leadingIcon = {
                                if (themeKey == selectedTheme) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 网页自动翻译配置卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateCard(
    enabled: Boolean,
    config: TranslateConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (TranslateConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // 语言选项 - 使用本地化名称
    val languageOptions = listOf(
        TranslateLanguage.CHINESE to Strings.langChinese,
        TranslateLanguage.ENGLISH to Strings.langEnglish,
        TranslateLanguage.JAPANESE to Strings.langJapanese,
        TranslateLanguage.ARABIC to Strings.langArabic
    )

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                        Icons.Outlined.Translate,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.autoTranslate,
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
                    text = Strings.autoTranslateHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 目标语言选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = languageOptions.find { it.first == config.targetLanguage }?.second ?: config.targetLanguage.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.translateTargetLanguage) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languageOptions.forEach { (language, displayName) ->
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    onConfigChange(config.copy(targetLanguage = language))
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (language == config.targetLanguage) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                
                // 显示翻译按钮选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Strings.showTranslateButton, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = Strings.showTranslateButtonHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.showFloatingButton,
                        onCheckedChange = { onConfigChange(config.copy(showFloatingButton = it)) }
                    )
                }
            }
        }
    }
}
