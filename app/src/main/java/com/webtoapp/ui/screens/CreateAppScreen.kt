package com.webtoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.ActivationCodeCard
import com.webtoapp.ui.components.AppNameTextField
import com.webtoapp.ui.components.AutoStartCard
import com.webtoapp.ui.components.BgmCard
import com.webtoapp.ui.components.*
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

    // Handle保存结果
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onSaved()
            viewModel.resetUiState()
        }
    }

    // Image选择器 - 选择后复制到私有目录实现持久化
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handleIconSelected(it)
        }
    }

    // Start画面图片选择器
    val splashImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handleSplashMediaSelected(it, isVideo = false)
        }
    }

    // Start画面视频选择器
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

            // Activation码设置
            ActivationCodeCard(
                enabled = editState.activationEnabled,
                activationCodes = editState.activationCodeList,
                requireEveryTime = editState.activationRequireEveryTime,
                onEnabledChange = { viewModel.updateEditState { copy(activationEnabled = it) } },
                onCodesChange = { viewModel.updateEditState { copy(activationCodeList = it) } },
                onRequireEveryTimeChange = { viewModel.updateEditState { copy(activationRequireEveryTime = it) } }
            )

            // Announcement设置
            AnnouncementCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(announcementEnabled = it) } },
                onAnnouncementChange = { viewModel.updateEditState { copy(announcement = it) } }
            )

            // Ad拦截设置
            AdBlockCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(adBlockEnabled = it) } },
                onRulesChange = { viewModel.updateEditState { copy(adBlockRules = it) } },
                onToggleEnabledChange = { 
                    viewModel.updateEditState { 
                        copy(webViewConfig = webViewConfig.copy(adBlockToggleEnabled = it)) 
                    } 
                }
            )

            // 扩展模块设置
            com.webtoapp.ui.components.ExtensionModuleCard(
                enabled = editState.extensionModuleEnabled,
                selectedModuleIds = editState.extensionModuleIds,
                onEnabledChange = { viewModel.updateEditState { copy(extensionModuleEnabled = it) } },
                onModuleIdsChange = { viewModel.updateEditState { copy(extensionModuleIds = it) } }
            )

            // Fullscreen模式
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

            // Landscape模式
            LandscapeModeCard(
                enabled = editState.webViewConfig.landscapeMode,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(landscapeMode = it))
                    }
                }
            )

            // Start画面
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

            // Background music
            BgmCard(
                enabled = editState.bgmEnabled,
                config = editState.bgmConfig,
                onEnabledChange = { viewModel.updateEditState { copy(bgmEnabled = it) } },
                onConfigChange = { viewModel.updateEditState { copy(bgmConfig = it) } }
            )

            // Web page自动翻译
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
            
            // 黑科技功能设置（独立模块）
            com.webtoapp.ui.components.BlackTechConfigCard(
                config = editState.blackTechConfig,
                onConfigChange = { viewModel.updateEditState { copy(blackTechConfig = it) } }
            )
            
            // App伪装设置（独立模块）
            com.webtoapp.ui.components.DisguiseConfigCard(
                config = editState.disguiseConfig,
                onConfigChange = { viewModel.updateEditState { copy(disguiseConfig = it) } }
            )

            // 浏览器伪装（User-Agent）
            UserAgentCard(
                config = editState.webViewConfig,
                onConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(webViewConfig = newConfig)
                    }
                }
            )

            // 长按菜单设置
            LongPressMenuCard(
                style = editState.webViewConfig.longPressMenuStyle,
                onStyleChange = { 
                    viewModel.updateEditState { 
                        copy(webViewConfig = webViewConfig.copy(
                            longPressMenuEnabled = it != LongPressMenuStyle.DISABLED,
                            longPressMenuStyle = it
                        )) 
                    } 
                }
            )

            // WebView高级设置
            WebViewConfigCard(
                config = editState.webViewConfig,
                onConfigChange = { viewModel.updateEditState { copy(webViewConfig = it) } },
                apkExportConfig = editState.apkExportConfig,
                onApkExportConfigChange = { viewModel.updateEditState { copy(apkExportConfig = it) } }
            )

            // Error提示
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

            // Icon选择（带图标库功能）
            IconPickerWithLibrary(
                iconUri = editState.iconUri,
                iconPath = editState.savedIconPath,
                websiteUrl = if (editState.appType == AppType.WEB) editState.url else null,
                onSelectFromGallery = onSelectIcon,
                onSelectFromLibrary = onSelectIconFromLibrary
            )

            // App名称（带随机按钮）
            AppNameTextField(
                value = editState.name,
                onValueChange = onNameChange
            )

            // 根据应用类型显示不同内容
            when (editState.appType) {
                AppType.WEB -> {
                    // Website URL输入框（仅 WEB 类型）
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
                AppType.HTML, AppType.FRONTEND -> {
                    // HTML/前端应用显示文件信息
                    val htmlConfig = editState.htmlConfig
                    val fileCount = htmlConfig?.files?.size ?: 0
                    val entryFile = htmlConfig?.entryFile?.takeIf { it.isNotBlank() } ?: "index.html"
                    val isFrontend = editState.appType == AppType.FRONTEND
                    
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
                                if (isFrontend) Icons.Outlined.Web else Icons.Outlined.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isFrontend) Strings.frontendApp else Strings.htmlApp,
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
                    // Media应用显示文件路径
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
                AppType.GALLERY -> {
                    // 画廊应用有独立的编辑界面，此处显示简要信息
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
                                Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Strings.galleryApp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = Strings.galleryMediaList,
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

                // Activation码列表
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

                // Show频率选择
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
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 触发机制设置
                Text(
                    Strings.announcementTriggerSettings,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Start时触发
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Strings.announcementTriggerOnLaunch, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Strings.announcementTriggerOnLaunchHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editState.announcement.triggerOnLaunch,
                        onCheckedChange = { onAnnouncementChange(editState.announcement.copy(triggerOnLaunch = it)) }
                    )
                }
                
                // 无网络时触发
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Strings.announcementTriggerOnNoNetwork, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Strings.announcementTriggerOnNoNetworkHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editState.announcement.triggerOnNoNetwork,
                        onCheckedChange = { onAnnouncementChange(editState.announcement.copy(triggerOnNoNetwork = it)) }
                    )
                }
                
                // 定时间隔触发
                var intervalExpanded by remember { mutableStateOf(false) }
                val intervalOptions = listOf(0, 1, 3, 5, 10, 15, 30, 60)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Strings.announcementTriggerInterval, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Strings.announcementTriggerIntervalHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    ExposedDropdownMenuBox(
                        expanded = intervalExpanded,
                        onExpandedChange = { intervalExpanded = it },
                        modifier = Modifier.width(120.dp)
                    ) {
                        OutlinedTextField(
                            value = if (editState.announcement.triggerIntervalMinutes == 0) 
                                Strings.announcementIntervalDisabled 
                            else 
                                "${editState.announcement.triggerIntervalMinutes} ${Strings.minutesShort}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = intervalExpanded,
                            onDismissRequest = { intervalExpanded = false }
                        ) {
                            intervalOptions.forEach { interval ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            if (interval == 0) Strings.announcementIntervalDisabled 
                                            else "$interval ${Strings.minutesShort}"
                                        ) 
                                    },
                                    onClick = {
                                        onAnnouncementChange(editState.announcement.copy(triggerIntervalMinutes = interval))
                                        intervalExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Start时也立即触发一次（仅当定时间隔启用时显示）
                if (editState.announcement.triggerIntervalMinutes > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editState.announcement.triggerIntervalIncludeLaunch,
                            onCheckedChange = { onAnnouncementChange(editState.announcement.copy(triggerIntervalIncludeLaunch = it)) }
                        )
                        Text(
                            Strings.announcementTriggerIntervalIncludeLaunch,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                
// 新增选项：勾选确认与不再显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = editState.announcement.requireConfirmation,
                            onCheckedChange = { onAnnouncementChange(editState.announcement.copy(requireConfirmation = it)) }
                        )
                        Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = editState.announcement.allowNeverShow,
                            onCheckedChange = { onAnnouncementChange(editState.announcement.copy(allowNeverShow = it)) }
                        )
                        Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall)
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
 * 长按菜单设置卡片 - 带样式预览
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPressMenuCard(
    style: LongPressMenuStyle,
    onStyleChange: (LongPressMenuStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // 样式选项配置
    data class StyleOption(
        val style: LongPressMenuStyle,
        val name: String,
        val desc: String,
        val icon: ImageVector,
        val previewColor: Color
    )
    
    val styleOptions = listOf(
        StyleOption(LongPressMenuStyle.FULL, Strings.longPressMenuStyleFull, Strings.longPressMenuStyleFullDesc, Icons.Outlined.ViewList, Color(0xFF6366F1)),
        StyleOption(LongPressMenuStyle.SIMPLE, Strings.longPressMenuStyleSimple, Strings.longPressMenuStyleSimpleDesc, Icons.Outlined.ViewAgenda, Color(0xFF22C55E)),
        StyleOption(LongPressMenuStyle.IOS, Strings.longPressMenuStyleIos, Strings.longPressMenuStyleIosDesc, Icons.Outlined.PhoneIphone, Color(0xFF3B82F6)),
        StyleOption(LongPressMenuStyle.FLOATING, Strings.longPressMenuStyleFloating, Strings.longPressMenuStyleFloatingDesc, Icons.Outlined.BubbleChart, Color(0xFFF97316)),
        StyleOption(LongPressMenuStyle.CONTEXT, Strings.longPressMenuStyleContext, Strings.longPressMenuStyleContextDesc, Icons.Outlined.Mouse, Color(0xFF8B5CF6)),
        StyleOption(LongPressMenuStyle.DISABLED, Strings.longPressMenuStyleDisabled, Strings.longPressMenuStyleDisabledDesc, Icons.Outlined.Block, Color(0xFF6B7280))
    )
    
    val selectedOption = styleOptions.find { it.style == style } ?: styleOptions[0]

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行 - 可点击展开/收缩
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.TouchApp,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.longPressMenuSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = selectedOption.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = Strings.longPressMenuSettingsDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 样式选择网格 - 2列
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                styleOptions.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = option.style == style
                            OutlinedCard(
                                onClick = { onStyleChange(option.style) },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) 
                                        option.previewColor.copy(alpha = 0.1f) 
                                    else 
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) option.previewColor else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 样式预览图标
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = option.previewColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            option.icon,
                                            contentDescription = null,
                                            tint = option.previewColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    
                                    // 样式名称
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) option.previewColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // 简短描述
                                    Text(
                                        text = option.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    // 选中指示器
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = option.previewColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // If it is奇数个，填充空白
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
                    // 当前选中样式的详细预览
                    if (style != LongPressMenuStyle.DISABLED) {
                        Divider()
                        Text(
                            text = Strings.longPressMenuPreview,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LongPressMenuStylePreview(style = style, accentColor = selectedOption.previewColor)
                    }
                }
            }
        }
    }
}

/**
 * 长按菜单样式预览组件
 */
@Composable
private fun LongPressMenuStylePreview(
    style: LongPressMenuStyle,
    accentColor: Color
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LongPressMenuStyle.FULL, LongPressMenuStyle.SIMPLE -> {
                // BottomSheet 样式预览
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // 拖拽条
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(32.dp)
                                    .height(4.dp)
                                    .background(
                                        color = onSurfaceColor.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // 菜单项
                            repeat(if (style == LongPressMenuStyle.FULL) 3 else 2) { index ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                color = accentColor.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .background(
                                                color = onSurfaceColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            LongPressMenuStyle.IOS -> {
                // iOS 毛玻璃风格预览
                Surface(
                    modifier = Modifier
                        .width(180.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 8.dp
                ) {
                    Column {
                        repeat(3) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(12.dp)
                                        .background(
                                            color = onSurfaceColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            color = accentColor.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                )
                            }
                            if (index < 2) {
                                Divider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = onSurfaceColor.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
            
            LongPressMenuStyle.FLOATING -> {
                // 悬浮气泡风格预览
                Box(modifier = Modifier.fillMaxSize()) {
                    // 中心气泡
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = CircleShape,
                        color = accentColor,
                        tonalElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // 周围小气泡
                    val bubbleOffsets = listOf(
                        Alignment.TopCenter to (-60).dp,
                        Alignment.CenterStart to (-60).dp,
                        Alignment.CenterEnd to 60.dp,
                        Alignment.BottomCenter to 60.dp
                    )
                    
                    bubbleOffsets.forEachIndexed { index, (alignment, _) ->
                        Surface(
                            modifier = Modifier
                                .align(alignment)
                                .padding(
                                    when (alignment) {
                                        Alignment.TopCenter -> PaddingValues(top = 8.dp)
                                        Alignment.BottomCenter -> PaddingValues(bottom = 8.dp)
                                        Alignment.CenterStart -> PaddingValues(start = 24.dp)
                                        Alignment.CenterEnd -> PaddingValues(end = 24.dp)
                                        else -> PaddingValues(0.dp)
                                    }
                                ),
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.7f - index * 0.1f),
                            tonalElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val icons = listOf(
                                    Icons.Default.Download,
                                    Icons.Default.ContentCopy,
                                    Icons.Default.Share,
                                    Icons.Default.OpenInBrowser
                                )
                                Icon(
                                    icons[index],
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            LongPressMenuStyle.CONTEXT -> {
                // 右键菜单风格预览
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(start = 20.dp, top = 20.dp)
                            .width(140.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            repeat(4) { index ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index == 0) accentColor.copy(alpha = 0.1f) 
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                color = if (index == 0) accentColor else onSurfaceColor.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(10.dp)
                                            .background(
                                                color = onSurfaceColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            LongPressMenuStyle.DISABLED -> {
                // Disable状态不显示预览
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
    onRulesChange: (List<String>) -> Unit,
    onToggleEnabledChange: (Boolean) -> Unit = {}
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
                
                // Allow用户切换广告拦截
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.adBlockToggleEnabled,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Strings.adBlockToggleDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = editState.webViewConfig.adBlockToggleEnabled,
                        onCheckedChange = onToggleEnabledChange
                    )
                }

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
 * WebView configuration卡片
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
                    Column {
                        Text(
                            text = Strings.advancedSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = Strings.webViewAdvancedConfig,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
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
                
                SettingsSwitch(
                    title = Strings.crossOriginIsolationSetting,
                    subtitle = Strings.crossOriginIsolationSettingHint,
                    checked = config.enableCrossOriginIsolation,
                    onCheckedChange = { onConfigChange(config.copy(enableCrossOriginIsolation = it)) }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // User脚本配置
                UserScriptsSection(
                    scripts = config.injectScripts,
                    onScriptsChange = { onConfigChange(config.copy(injectScripts = it)) }
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
}

/**
 * APK 导出配置区域
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
        
        // Custom包名（最大12字符，因为二进制替换限制）
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
        
        // Version名和版本号
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // APK 架构选择
        Text(
            text = Strings.apkArchitecture,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ApkArchitecture.entries.forEach { arch ->
                val isSelected = config.architecture == arch
                FilterChip(
                    selected = isSelected,
                    onClick = { onConfigChange(config.copy(architecture = arch)) },
                    label = { Text(arch.displayName) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        
        Text(
            text = config.architecture.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}




/**
 * 浏览器伪装卡片（User-Agent 配置）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserAgentCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isEnabled = config.userAgentMode != UserAgentMode.DEFAULT
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 卡片头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Security,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.userAgentMode,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isEnabled) config.userAgentMode.displayName else Strings.userAgentDefault,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }
            
            // Expand内容 - 使用 AnimatedVisibility 实现平滑动画
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // 提示文字
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.bypassWebViewDetection,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 浏览器选择
                Text(
                    text = Strings.mobileVersion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 移动版浏览器
                    listOf(
                        UserAgentMode.DEFAULT to Strings.userAgentDefault,
                        UserAgentMode.CHROME_MOBILE to "Chrome",
                        UserAgentMode.SAFARI_MOBILE to "Safari",
                        UserAgentMode.FIREFOX_MOBILE to "Firefox",
                        UserAgentMode.EDGE_MOBILE to "Edge"
                    ).forEach { (mode, name) ->
                        FilterChip(
                            selected = config.userAgentMode == mode,
                            onClick = { onConfigChange(config.copy(userAgentMode = mode)) },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = Strings.desktopVersion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 桌面版浏览器
                    listOf(
                        UserAgentMode.CHROME_DESKTOP to "Chrome",
                        UserAgentMode.SAFARI_DESKTOP to "Safari",
                        UserAgentMode.FIREFOX_DESKTOP to "Firefox",
                        UserAgentMode.EDGE_DESKTOP to "Edge"
                    ).forEach { (mode, name) ->
                        FilterChip(
                            selected = config.userAgentMode == mode,
                            onClick = { onConfigChange(config.copy(userAgentMode = mode)) },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom选项
                FilterChip(
                    selected = config.userAgentMode == UserAgentMode.CUSTOM,
                    onClick = { onConfigChange(config.copy(userAgentMode = UserAgentMode.CUSTOM)) },
                    label = { Text(Strings.userAgentCustom) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Edit,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                
                // Custom输入框
                if (config.userAgentMode == UserAgentMode.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.customUserAgent ?: "",
                        onValueChange = { onConfigChange(config.copy(customUserAgent = it.ifBlank { null })) },
                        label = { Text("User-Agent") },
                        placeholder = { Text(Strings.userAgentCustomHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )
                }
                
                    // Show当前 User-Agent
                    if (config.userAgentMode != UserAgentMode.DEFAULT && config.userAgentMode != UserAgentMode.CUSTOM) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Strings.currentUserAgent,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = config.userAgentMode.userAgentString ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
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
            CollapsibleCardHeader(
                icon = Icons.Outlined.Fullscreen,
                title = Strings.fullscreenMode,
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            
            // Fullscreen模式下显示状态栏选项
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
                
                // Status bar配置（仅在显示状态栏时可用）
                if (showStatusBar) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Status bar配置展开/收起
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
                    
                    // Status bar配置内容
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
    IconSwitchCard(
        title = Strings.landscapeModeLabel,
        icon = Icons.Outlined.ScreenRotation,
        checked = enabled,
        onCheckedChange = onEnabledChange
    )
}

/**
 * 检查媒体文件是否存在
 */
fun checkMediaExists(context: android.content.Context, uri: android.net.Uri?, savedPath: String?): Boolean {
    // 优先检查保存的路径
    if (!savedPath.isNullOrEmpty()) {
        return java.io.File(savedPath).exists()
    }
    // Check URI
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
    
    // Check媒体文件是否存在
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
            CollapsibleCardHeader(
                icon = Icons.Outlined.PlayCircle,
                title = Strings.splashScreen,
                checked = editState.splashEnabled,
                onCheckedChange = onEnabledChange
            )

            if (editState.splashEnabled) {
                Text(
                    text = Strings.splashHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Media预览区域
                if (editState.splashMediaUri != null && mediaExists) {
                    if (editState.splashConfig.type == SplashType.VIDEO) {
                        // Video裁剪器
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
                        // Image预览
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
                                // Delete按钮
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
                    // Empty状态 - 选择媒体
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

                // Select按钮
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
                    // Show时长设置（仅图片显示，视频使用裁剪范围）
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
                    
                    // Show方向设置
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
                    
                    // Enable音频设置（仅视频类型显示）
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
    // Theme选项列表 - 使用本地化名称
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
                
                // Show翻译按钮选项
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

/**
 * 用户脚本配置区域
 */
@Composable
fun UserScriptsSection(
    scripts: List<UserScript>,
    onScriptsChange: (List<UserScript>) -> Unit
) {
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingScript by remember { mutableStateOf<UserScript?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }
    
    Column {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Code,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = Strings.userScripts,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = Strings.userScriptsDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = {
                    editingScript = null
                    editingIndex = -1
                    showEditorDialog = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    Strings.addScript,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Script列表
        if (scripts.isEmpty()) {
            Text(
                text = Strings.noScripts,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            scripts.forEachIndexed { index, script ->
                UserScriptItem(
                    script = script,
                    onEdit = {
                        editingScript = script
                        editingIndex = index
                        showEditorDialog = true
                    },
                    onDelete = {
                        onScriptsChange(scripts.filterIndexed { i, _ -> i != index })
                    },
                    onToggle = { enabled ->
                        onScriptsChange(scripts.mapIndexed { i, s ->
                            if (i == index) s.copy(enabled = enabled) else s
                        })
                    }
                )
                if (index < scripts.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
    
    // Script编辑对话框
    if (showEditorDialog) {
        UserScriptEditorDialog(
            script = editingScript,
            onDismiss = { showEditorDialog = false },
            onSave = { script ->
                if (editingIndex >= 0) {
                    // 编辑现有脚本
                    onScriptsChange(scripts.mapIndexed { i, s ->
                        if (i == editingIndex) script else s
                    })
                } else {
                    // 添加新脚本
                    onScriptsChange(scripts + script)
                }
                showEditorDialog = false
            }
        )
    }
}

/**
 * 单个脚本项
 */
@Composable
fun UserScriptItem(
    script: UserScript,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name.ifBlank { Strings.userScripts },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = when (script.runAt) {
                    ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStart
                    ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEnd
                    ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdle
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = script.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Edit,
                    Strings.btnEdit,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    Strings.btnDelete,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 脚本编辑对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptEditorDialog(
    script: UserScript?,
    onDismiss: () -> Unit,
    onSave: (UserScript) -> Unit
) {
    var name by remember { mutableStateOf(script?.name ?: "") }
    var code by remember { mutableStateOf(script?.code ?: "") }
    var runAt by remember { mutableStateOf(script?.runAt ?: ScriptRunTime.DOCUMENT_END) }
    var enabled by remember { mutableStateOf(script?.enabled ?: true) }
    var runAtExpanded by remember { mutableStateOf(false) }
    
    var nameError by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }
    
    val isEdit = script != null
    val scrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) Strings.editScript else Strings.addScript) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Script名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = false
                    },
                    label = { Text(Strings.scriptName) },
                    placeholder = { Text(Strings.scriptNamePlaceholder) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(Strings.scriptNameRequired, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 运行时机选择
                ExposedDropdownMenuBox(
                    expanded = runAtExpanded,
                    onExpandedChange = { runAtExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (runAt) {
                            ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStart
                            ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEnd
                            ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdle
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.scriptRunAt) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = runAtExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = runAtExpanded,
                        onDismissRequest = { runAtExpanded = false }
                    ) {
                        ScriptRunTime.values().forEach { time ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(when (time) {
                                            ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStart
                                            ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEnd
                                            ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdle
                                        })
                                        Text(
                                            text = when (time) {
                                                ScriptRunTime.DOCUMENT_START -> Strings.runTimeDocStartDesc
                                                ScriptRunTime.DOCUMENT_END -> Strings.runTimeDocEndDesc
                                                ScriptRunTime.DOCUMENT_IDLE -> Strings.runTimeDocIdleDesc
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    runAt = time
                                    runAtExpanded = false
                                },
                                leadingIcon = {
                                    if (time == runAt) {
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
                
                // Enable开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.scriptEnabled, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
                
                // Script代码
                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        code = it
                        codeError = false
                    },
                    label = { Text(Strings.scriptCode) },
                    placeholder = { Text(Strings.scriptCodePlaceholder) },
                    minLines = 6,
                    maxLines = 12,
                    isError = codeError,
                    supportingText = if (codeError) {
                        { Text(Strings.scriptCodeRequired, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = name.isBlank()
                    codeError = code.isBlank()
                    
                    if (!nameError && !codeError) {
                        onSave(UserScript(
                            name = name,
                            code = code,
                            enabled = enabled,
                            runAt = runAt
                        ))
                    }
                }
            ) {
                Text(Strings.btnSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
