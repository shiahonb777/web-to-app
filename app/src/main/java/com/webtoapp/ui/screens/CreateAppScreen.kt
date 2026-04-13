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
    activationManager: com.webtoapp.core.activation.ActivationManager,
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
                activationManager = activationManager,
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
            )

            // Status Bar Style Config (standalone, works without fullscreen)
            StatusBarStyleCard(
                webViewConfig = editState.webViewConfig,
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



