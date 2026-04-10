package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.ActivationCodeCard
import com.webtoapp.ui.components.AppNameTextField
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.AutoStartCard
import com.webtoapp.ui.components.BgmCard
import com.webtoapp.ui.components.*
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.viewmodel.UiState
import com.webtoapp.util.AppConstants
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.ui.webview.WebViewActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.core.pwa.PwaAnalysisResult
import com.webtoapp.core.pwa.PwaAnalysisState
import com.webtoapp.core.pwa.PwaDataSource

// Pre-compiled regex for package name validation (avoid allocation during recomposition)
private val PACKAGE_NAME_REGEX = AppConstants.PACKAGE_NAME_REGEX

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
    val context = LocalContext.current
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    val coroutineScope = rememberCoroutineScope()
    var isPreviewSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) Strings.editApp else Strings.createApp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    // 缓存式预览按钮 — 先保存再用正式路径打开
                    IconButton(
                        onClick = {
                            if (isPreviewSaving) return@IconButton
                            isPreviewSaving = true
                            coroutineScope.launch {
                                try {
                                    val appId = viewModel.saveAndPreview()
                                    if (appId != null && appId > 0) {
                                        WebViewActivity.start(context, appId)
                                    } else {
                                        snackbarHostState.showSnackbar(Strings.saveFailed)
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(Strings.saveFailed)
                                } finally {
                                    isPreviewSaving = false
                                }
                            }
                        },
                        enabled = (editState.url.isNotBlank() || editState.name.isNotBlank()) && !isPreviewSaving
                    ) {
                        if (isPreviewSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = Strings.btnPreview
                            )
                        }
                    }
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
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
                    .fillMaxSize()
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

                // PWA 自动感知（仅 WEB 类型显示）
                if (editState.appType == AppType.WEB) {
                    PwaAnalysisSection(
                        viewModel = viewModel,
                        editState = editState
                    )
                }

            // Activation码设置
            ActivationCodeCard(
                enabled = editState.activationEnabled,
                activationCodes = editState.activationCodeList,
                requireEveryTime = editState.activationRequireEveryTime,
                dialogConfig = editState.activationDialogConfig,
                onEnabledChange = { viewModel.updateEditState { copy(activationEnabled = it) } },
                onCodesChange = { viewModel.updateEditState { copy(activationCodeList = it) } },
                onRequireEveryTimeChange = { viewModel.updateEditState { copy(activationRequireEveryTime = it) } },
                onDialogConfigChange = { viewModel.updateEditState { copy(activationDialogConfig = it) } }
            )

            // Announcement设置
            AnnouncementCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(announcementEnabled = it) } },
                onAnnouncementChange = { viewModel.updateEditState { copy(announcement = it) } }
            )
            
            if (hasConfiguredLegacyAds(editState)) {
                LegacyAdCapabilityWarningCard()
            }

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
                extensionFabIcon = editState.extensionFabIcon,
                onEnabledChange = { viewModel.updateEditState { copy(extensionModuleEnabled = it) } },
                onModuleIdsChange = { viewModel.updateEditState { copy(extensionModuleIds = it) } },
                onFabIconChange = { viewModel.updateEditState { copy(extensionFabIcon = it) } }
            )

            // Fullscreen模式
            FullscreenModeCard(
                enabled = editState.webViewConfig.hideToolbar,
                showStatusBar = editState.webViewConfig.showStatusBarInFullscreen,
                showNavigationBar = editState.webViewConfig.showNavigationBarInFullscreen,
                showToolbar = editState.webViewConfig.showToolbarInFullscreen,
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
                onShowNavigationBarChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(showNavigationBarInFullscreen = it))
                    }
                },
                onShowToolbarChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(showToolbarInFullscreen = it))
                    }
                },
                onWebViewConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(webViewConfig = newConfig)
                    }
                }
            )

            // 屏幕方向模式
            LandscapeModeCard(
                enabled = editState.webViewConfig.landscapeMode,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(landscapeMode = it))
                    }
                },
                orientationMode = editState.webViewConfig.orientationMode,
                onOrientationModeChange = { mode ->
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(
                            orientationMode = mode,
                            landscapeMode = mode in listOf(
                                com.webtoapp.data.model.OrientationMode.LANDSCAPE,
                                com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE,
                                com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE
                            )
                        ))
                    }
                }
            )
            
            // 保持屏幕常亮
            KeepScreenOnCard(
                screenAwakeMode = editState.webViewConfig.screenAwakeMode,
                onScreenAwakeModeChange = { mode ->
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(
                            screenAwakeMode = mode,
                            keepScreenOn = mode != com.webtoapp.data.model.ScreenAwakeMode.OFF
                        ))
                    }
                },
                screenAwakeTimeoutMinutes = editState.webViewConfig.screenAwakeTimeoutMinutes,
                onScreenAwakeTimeoutChange = { minutes ->
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(screenAwakeTimeoutMinutes = minutes))
                    }
                },
                screenBrightness = editState.webViewConfig.screenBrightness,
                onScreenBrightnessChange = { brightness ->
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(screenBrightness = brightness))
                    }
                }
            )
            
            // 悬浮小窗模式
            FloatingWindowConfigCard(
                config = editState.webViewConfig.floatingWindowConfig,
                onConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(floatingWindowConfig = newConfig))
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

            // 设备伪装（独立一级功能）
            DeviceDisguiseCard(
                config = editState.deviceDisguiseConfig,
                onConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(deviceDisguiseConfig = newConfig)
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
                EnhancedElevatedCard(
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
}

private fun hasConfiguredLegacyAds(editState: EditState): Boolean {
    val config = editState.adConfig
    return editState.adsEnabled ||
        config.bannerId.isNotBlank() ||
        config.interstitialId.isNotBlank() ||
        config.splashId.isNotBlank()
}

@Composable
private fun LegacyAdCapabilityWarningCard() {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "广告 SDK 尚未集成，当前广告配置不会生效。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
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
                    PremiumTextField(
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
                    
                    EnhancedElevatedCard(
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
                    
                    EnhancedElevatedCard(
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
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                AppType.WORDPRESS -> {
                    // WordPress 应用显示类型信息
                    EnhancedElevatedCard(
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
                                Icons.Outlined.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Strings.appTypeWordPress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "PHP + SQLite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                AppType.GALLERY -> {
                    // 画廊应用有独立的编辑界面，此处显示简要信息
                    EnhancedElevatedCard(
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
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                AppType.NODEJS_APP -> {
                    // Node.js 应用显示类型信息
                    EnhancedElevatedCard(
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
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Strings.appTypeNodeJs,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Node.js Runtime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                AppType.PHP_APP, AppType.PYTHON_APP, AppType.GO_APP -> {
                    val (label, desc) = when (editState.appType) {
                        AppType.PHP_APP -> Strings.appTypePhp to "PHP Runtime"
                        AppType.PYTHON_APP -> Strings.appTypePython to "Python Runtime"
                        AppType.GO_APP -> Strings.appTypeGo to "Go Binary"
                        else -> "" to ""
                    }
                    EnhancedElevatedCard(
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
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                AppType.MULTI_WEB -> {
                    // 多站点聚合应用：简化处理，使用 Web URL 输入
                    PremiumTextField(
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (editState.activationEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Key,
                            null,
                            tint = if (editState.activationEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.activationCodeVerify,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PremiumSwitch(
                    checked = editState.activationEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = editState.activationEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    PremiumTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        placeholder = { Text(Strings.inputActivationCode) },
                        singleLine = true,
                        modifier = Modifier.weight(weight = 1f, fill = true)
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
                            modifier = Modifier.weight(weight = 1f, fill = true)
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
}


/**
 * PWA 自动感知区域
 */
@Composable
fun PwaAnalysisSection(
    viewModel: MainViewModel,
    editState: EditState
) {
    val pwaState by viewModel.pwaAnalysisState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showResultCard by remember { mutableStateOf(false) }

    // 当分析成功时显示结果卡片
    LaunchedEffect(pwaState) {
        if (pwaState is PwaAnalysisState.Success) {
            showResultCard = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 分析按钮
        val isAnalyzing = pwaState is PwaAnalysisState.Analyzing
        
        FilledTonalButton(
            onClick = {
                showResultCard = false
                viewModel.analyzePwa(editState.url)
            },
            enabled = editState.url.isNotBlank() && !isAnalyzing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.pwaAnalyzing)
            } else {
                Icon(Icons.Outlined.TravelExplore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.pwaAnalyzeButton)
            }
        }

        // 错误提示
        AnimatedVisibility(
            visible = pwaState is PwaAnalysisState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val error = (pwaState as? PwaAnalysisState.Error)?.message ?: ""
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${Strings.pwaAnalysisFailed}: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // 分析结果卡片
        AnimatedVisibility(
            visible = showResultCard && pwaState is PwaAnalysisState.Success,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            val result = (pwaState as? PwaAnalysisState.Success)?.result
            if (result != null) {
                PwaResultCard(
                    result = result,
                    onApply = {
                        viewModel.applyPwaResult(result)
                        showResultCard = false
                        viewModel.resetPwaState()
                    },
                    onDismiss = {
                        showResultCard = false
                        viewModel.resetPwaState()
                    }
                )
            }
        }
    }
}

/**
 * PWA 分析结果卡片
 */
@Composable
private fun PwaResultCard(
    result: PwaAnalysisResult,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (result.isPwa)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (result.isPwa) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = if (result.isPwa)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.isPwa) Strings.pwaDetected else Strings.pwaNoneDetected,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (result.isPwa)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 数据来源
            Text(
                text = when (result.source) {
                    PwaDataSource.MANIFEST -> Strings.pwaSourceManifest
                    PwaDataSource.META_TAGS -> Strings.pwaSourceMeta
                    PwaDataSource.NONE -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 检测到的信息
            result.suggestedName?.let { name ->
                PwaInfoRow(label = Strings.pwaName, value = name)
            }

            result.suggestedIconUrl?.let { url ->
                PwaInfoRow(label = Strings.pwaIcon, value = url.takeLast(60))
            }

            result.suggestedThemeColor?.let { color ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Strings.pwaThemeColor}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 色块预览
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(color))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(parsedColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = color,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            result.suggestedDisplay?.let { display ->
                PwaInfoRow(label = Strings.pwaDisplayMode, value = display)
            }

            result.suggestedOrientation?.let { orientation ->
                PwaInfoRow(label = Strings.pwaOrientation, value = orientation)
            }

            result.startUrl?.let { url ->
                PwaInfoRow(label = Strings.pwaStartUrl, value = url.takeLast(80))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 一键应用按钮
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.pwaApplyAll)
            }
        }
    }
}

/**
 * PWA 信息行
 */
@Composable
private fun PwaInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                PremiumTextField(
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
 * 网页自动翻译配置卡片 — 多引擎 / 20 种语言
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateCard(
    enabled: Boolean,
    config: TranslateConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (TranslateConfig) -> Unit
) {
    var langExpanded by remember { mutableStateOf(false) }
    var engineExpanded by remember { mutableStateOf(false) }
    
    // 所有支持的语言（从 enum 直接生成，使用原生语言名称）
    val languageOptions = TranslateLanguage.entries.toList()
    
    // 翻译引擎选项
    val engineOptions = TranslateEngine.entries.toList()

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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Translate,
                            null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.autoTranslate,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Strings.autoTranslateHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 目标语言选择（20 种语言）
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = it }
                ) {
                    PremiumTextField(
                        value = "${config.targetLanguage.displayName} (${config.targetLanguage.code})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.translateTargetLanguage) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        languageOptions.forEach { language ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(language.displayName)
                                        Text(
                                            text = language.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onConfigChange(config.copy(targetLanguage = language))
                                    langExpanded = false
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
                
                // 翻译引擎选择
                ExposedDropdownMenuBox(
                    expanded = engineExpanded,
                    onExpandedChange = { engineExpanded = it }
                ) {
                    PremiumTextField(
                        value = config.preferredEngine.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.translateEngine) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engineExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = engineExpanded,
                        onDismissRequest = { engineExpanded = false }
                    ) {
                        engineOptions.forEach { engine ->
                            DropdownMenuItem(
                                text = { Text(engine.displayName) },
                                onClick = {
                                    onConfigChange(config.copy(preferredEngine = engine))
                                    engineExpanded = false
                                },
                                leadingIcon = {
                                    if (engine == config.preferredEngine) {
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
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(Strings.showTranslateButton, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = Strings.showTranslateButtonHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PremiumSwitch(
                        checked = config.showFloatingButton,
                        onCheckedChange = { onConfigChange(config.copy(showFloatingButton = it)) }
                    )
                }
                
                // 自动翻译选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(Strings.autoTranslateOnLoad, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = Strings.autoTranslateOnLoadHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PremiumSwitch(
                        checked = config.autoTranslateOnLoad,
                        onCheckedChange = { onConfigChange(config.copy(autoTranslateOnLoad = it)) }
                    )
                }
              }
            }
        }
    }
}

