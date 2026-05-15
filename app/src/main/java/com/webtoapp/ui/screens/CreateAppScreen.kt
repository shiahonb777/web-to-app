package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.design.WtaSwitch

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.webtoapp.ui.design.WtaBackground
import com.webtoapp.ui.components.AutoStartCard
import com.webtoapp.ui.components.BgmCard
import com.webtoapp.ui.components.*
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.viewmodel.UiState
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.ui.webview.WebViewActivity
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.design.*
import com.webtoapp.core.pwa.PwaAnalysisResult
import com.webtoapp.core.pwa.PwaAnalysisState
import com.webtoapp.core.pwa.PwaDataSource

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showDiscardDialog by remember { mutableStateOf(false) }


    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onSaved()
            viewModel.resetUiState()
        }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handleIconSelected(it)
        }
    }


    val splashImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.handleSplashMediaSelected(it, isVideo = false)
        }
    }


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

    val canPreview = (editState.url.isNotBlank() || editState.name.isNotBlank()) &&
        !isPreviewSaving &&
        uiState !is UiState.Loading

    fun launchPreview() {
        if (!canPreview) return
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
    }

    WtaScreen(
        title = if (isEdit) Strings.editApp else Strings.createApp,
        snackbarHostState = snackbarHostState,
        onBack = {
            if (hasUnsavedChanges) showDiscardDialog = true else onBack()
        },
        bottomBar = {
            CreateAppBottomBar(
                canPreview = canPreview,
                isPreviewSaving = isPreviewSaving,
                isSaving = uiState is UiState.Loading,
                onPreview = ::launchPreview,
                onSave = { viewModel.saveApp() }
            )
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                )
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {
            if (uiState is UiState.Error) {
                WtaStatusBanner(
                    message = (uiState as UiState.Error).message,
                    tone = WtaStatusTone.Error
                )
            }

            BasicInfoCard(
                editState = editState,
                onNameChange = { viewModel.updateEditState { copy(name = it) } },
                onUrlChange = { viewModel.updateEditState { copy(url = it) } },
                onSelectIcon = { imagePickerLauncher.launch("image/*") },
                onSelectIconFromLibrary = { path ->
                    viewModel.updateEditState { copy(savedIconPath = path, iconUri = null) }
                }
            )

            if (editState.appType == AppType.WEB) {
                PwaAnalysisSection(
                    viewModel = viewModel,
                    editState = editState
                )
            }

            // ── 显示与交互 ──────────────────────────────────────────

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

            HideBrowserToolbarCard(
                enabled = editState.webViewConfig.hideBrowserToolbar,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(hideBrowserToolbar = it))
                    }
                }
            )

            FullscreenModeCard(
                enabled = editState.webViewConfig.hideToolbar,
                showStatusBar = editState.webViewConfig.showStatusBarInFullscreen,
                showNavigationBar = editState.webViewConfig.showNavigationBarInFullscreen,
                hideBrowserToolbarInFullscreen =
                    editState.webViewConfig.hideBrowserToolbar ||
                        !editState.webViewConfig.showToolbarInFullscreen,
                webViewConfig = editState.webViewConfig,
                onEnabledChange = {
                    viewModel.updateEditState {
                        copy(
                            webViewConfig = webViewConfig.copy(
                                hideToolbar = it
                            )
                        )
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
                onHideBrowserToolbarInFullscreenChange = {
                    viewModel.updateEditState {
                        copy(
                            webViewConfig = webViewConfig.copy(
                                showToolbarInFullscreen = !it
                            )
                        )
                    }
                },
                onWebViewConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(webViewConfig = newConfig)
                    }
                }
            )

            LandscapeModeCard(
                enabled = editState.webViewConfig.landscapeMode,
                onEnabledChange = { enabled ->
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(
                            landscapeMode = enabled,
                            orientationMode = if (enabled) com.webtoapp.data.model.OrientationMode.LANDSCAPE
                            else com.webtoapp.data.model.OrientationMode.PORTRAIT
                        ))
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

            FloatingWindowConfigCard(
                config = editState.webViewConfig.floatingWindowConfig,
                onConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(webViewConfig = webViewConfig.copy(floatingWindowConfig = newConfig))
                    }
                }
            )

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

            // ── 启动与多媒体 ────────────────────────────────────────

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

            BgmCard(
                enabled = editState.bgmEnabled,
                config = editState.bgmConfig,
                onEnabledChange = { viewModel.updateEditState { copy(bgmEnabled = it) } },
                onConfigChange = { viewModel.updateEditState { copy(bgmConfig = it) } }
            )

            AnnouncementCard(
                editState = editState,
                onEnabledChange = { viewModel.updateEditState { copy(announcementEnabled = it) } },
                onAnnouncementChange = { viewModel.updateEditState { copy(announcement = it) } }
            )

            // ── 内容增强 ────────────────────────────────────────────

            TranslateCard(
                enabled = editState.translateEnabled,
                config = editState.translateConfig,
                onEnabledChange = { viewModel.updateEditState { copy(translateEnabled = it) } },
                onConfigChange = { viewModel.updateEditState { copy(translateConfig = it) } }
            )

            com.webtoapp.ui.components.ExtensionModuleCard(
                enabled = editState.extensionModuleEnabled,
                selectedModuleIds = editState.extensionModuleIds,
                extensionFabIcon = editState.extensionFabIcon,
                onEnabledChange = { viewModel.updateEditState { copy(extensionModuleEnabled = it) } },
                onModuleIdsChange = { viewModel.updateEditState { copy(extensionModuleIds = it) } },
                onFabIconChange = { viewModel.updateEditState { copy(extensionFabIcon = it) } }
            )

            if (hasConfiguredLegacyAds(editState)) {
                LegacyAdCapabilityWarningCard()
            }

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

            // ── 安全与访问控制 ──────────────────────────────────────

            com.webtoapp.ui.components.DisguiseConfigCard(
                config = editState.disguiseConfig,
                onConfigChange = { viewModel.updateEditState { copy(disguiseConfig = it) } }
            )

            DeviceDisguiseCard(
                config = editState.deviceDisguiseConfig,
                onConfigChange = { newConfig ->
                    viewModel.updateEditState {
                        copy(deviceDisguiseConfig = newConfig)
                    }
                }
            )

            // ── 系统控制 ────────────────────────────────────────────

            AutoStartCard(
                config = editState.autoStartConfig,
                onConfigChange = { viewModel.updateEditState { copy(autoStartConfig = it) } }
            )

            com.webtoapp.ui.components.ForcedRunConfigCard(
                config = editState.forcedRunConfig,
                onConfigChange = { viewModel.updateEditState { copy(forcedRunConfig = it) } }
            )

            com.webtoapp.ui.components.BlackTechConfigCard(
                config = editState.blackTechConfig,
                onConfigChange = { viewModel.updateEditState { copy(blackTechConfig = it) } }
            )

            // ── 高级与导出 ──────────────────────────────────────────

            BrowserAdvancedConfigCard(
                config = editState.webViewConfig,
                onConfigChange = { viewModel.updateEditState { copy(webViewConfig = it) } }
            )

            SpecialSettingsCard(
                config = editState.webViewConfig,
                onConfigChange = { viewModel.updateEditState { copy(webViewConfig = it) } }
            )

            ExportAndPermissionDrawer(
                exportConfig = editState.apkExportConfig,
                onExportConfigChange = { viewModel.updateEditState { copy(apkExportConfig = it) } }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }


    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = { Text(Strings.unsavedChangesTitle) },
            text = { Text(Strings.unsavedChangesMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.discardChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }

}

@Composable
private fun ExportAndPermissionDrawer(
    exportConfig: ApkExportConfig,
    onExportConfigChange: (ApkExportConfig) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    WtaSettingCard {
        Column {
            WtaChoiceRow(
                title = Strings.apkExportConfig,
                subtitle = null,
                icon = Icons.Outlined.SettingsApplications,
                value = "",
                isExpanded = expanded,
                onClick = { expanded = !expanded }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = WtaSpacing.RowHorizontal,
                        vertical = WtaSpacing.ContentGap
                    ),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
                ) {
                    ApkExportSettingsCard(
                        config = exportConfig,
                        onConfigChange = onExportConfigChange,
                        onOpenPermissionConfig = null
                    )

                    PermissionConfigPanel(
                        permissions = exportConfig.runtimePermissions,
                        onPermissionsChange = { permissions ->
                            onExportConfigChange(exportConfig.copy(runtimePermissions = permissions))
                        },
                        showDescription = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateAppBottomBar(
    canPreview: Boolean,
    isPreviewSaving: Boolean,
    isSaving: Boolean,
    onPreview: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPreview,
                enabled = canPreview,
                modifier = Modifier.weight(1f)
            ) {
                if (isPreviewSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.btnPreview)
            }

            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.btnSave)
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
                text = Strings.adSdkNotIntegrated,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }

}




@Composable
fun BasicInfoCard(
    editState: EditState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSelectIcon: () -> Unit,
    onSelectIconFromLibrary: (String) -> Unit = {}
) {
    WtaSettingCard {
        Column(
            modifier = Modifier.padding(vertical = WtaSpacing.ContentGap),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = WtaSpacing.RowHorizontal),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconPickerWithLibrary(
                    iconUri = editState.iconUri,
                    iconPath = editState.savedIconPath,
                    websiteUrl = if (editState.appType == AppType.WEB) editState.url else null,
                    onSelectFromGallery = onSelectIcon,
                    onSelectFromLibrary = onSelectIconFromLibrary
                )


                AppNameTextField(
                    value = editState.name,
                    onValueChange = onNameChange
                )


                when (editState.appType) {
                    AppType.WEB -> {

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

                        val htmlConfig = editState.htmlConfig
                        val fileCount = htmlConfig?.files?.size ?: 0
                        val entryFile = htmlConfig?.entryFile?.takeIf { it.isNotBlank() } ?: "index.html"
                        val isFrontend = editState.appType == AppType.FRONTEND

                        BasicInfoSummaryRow(
                            icon = if (isFrontend) Icons.Outlined.Web else Icons.Outlined.Code,
                            title = if (isFrontend) Strings.frontendApp else Strings.htmlApp,
                            subtitle = "${Strings.entryFile}: $entryFile · ${Strings.totalFilesCount.replace("%d", fileCount.toString())}"
                        )
                    }
                    AppType.IMAGE, AppType.VIDEO -> {

                        val mediaPath = editState.url
                        val isVideo = editState.appType == AppType.VIDEO
                        val fileName = mediaPath.substringAfterLast("/", Strings.unknownFile)

                        BasicInfoSummaryRow(
                            icon = if (isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                            title = if (isVideo) Strings.videoApp else Strings.imageApp,
                            subtitle = fileName
                        )
                    }
                    AppType.WORDPRESS -> {

                        BasicInfoSummaryRow(
                            icon = Icons.Outlined.Language,
                            title = Strings.appTypeWordPress,
                            subtitle = Strings.runtimePhpSqlite
                        )
                    }
                    AppType.GALLERY -> {

                        BasicInfoSummaryRow(
                            icon = Icons.Outlined.PhotoLibrary,
                            title = Strings.galleryApp,
                            subtitle = Strings.galleryMediaList
                        )
                    }
                    AppType.NODEJS_APP -> {

                        BasicInfoSummaryRow(
                            icon = Icons.Outlined.Terminal,
                            title = Strings.appTypeNodeJs,
                            subtitle = Strings.runtimeNodeJs
                        )
                    }
                    AppType.PHP_APP, AppType.PYTHON_APP, AppType.GO_APP -> {
                        val (label, desc) = when (editState.appType) {
                            AppType.PHP_APP -> Strings.appTypePhp to Strings.runtimePhp
                            AppType.PYTHON_APP -> Strings.appTypePython to Strings.runtimePython
                            AppType.GO_APP -> Strings.appTypeGo to Strings.runtimeGoBinary
                            else -> "" to ""
                        }
                        BasicInfoSummaryRow(
                            icon = Icons.Outlined.Terminal,
                            title = label,
                            subtitle = desc
                        )
                    }
                    AppType.MULTI_WEB -> {

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
}

@Composable
private fun BasicInfoSummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    WtaSettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        subtitleMaxLines = 1,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = WtaSpacing.ContentGap)
    )
}





@Composable
fun PwaAnalysisSection(
    viewModel: MainViewModel,
    editState: EditState
) {
    val pwaState by viewModel.pwaAnalysisState.collectAsStateWithLifecycle()
    var showResultCard by remember { mutableStateOf(false) }


    LaunchedEffect(pwaState) {
        if (pwaState is PwaAnalysisState.Success) {
            showResultCard = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        val isAnalyzing = pwaState is PwaAnalysisState.Analyzing

        FilledTonalButton(
            onClick = {
                showResultCard = false
                viewModel.analyzePwa(editState.url)
            },
            enabled = editState.url.isNotBlank() && !isAnalyzing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(WtaRadius.Button)
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


        AnimatedVisibility(
            visible = pwaState is PwaAnalysisState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val error = (pwaState as? PwaAnalysisState.Error)?.message ?: ""
            WtaStatusBanner(
                title = Strings.pwaAnalysisFailed,
                message = error,
                tone = WtaStatusTone.Error
            )
        }


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




@Composable
private fun PwaResultCard(
    result: PwaAnalysisResult,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    WtaSettingCard {
        Column(
            modifier = Modifier.padding(
                horizontal = WtaSpacing.RowHorizontal,
                vertical = WtaSpacing.RowVertical
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

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


            Text(
                text = when (result.source) {
                    PwaDataSource.MANIFEST -> Strings.pwaSourceManifest
                    PwaDataSource.META_TAGS -> Strings.pwaSourceMeta
                    PwaDataSource.NONE -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            WtaSectionDivider(modifier = Modifier.padding(horizontal = 0.dp))


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

                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(color))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(WtaRadius.Button))
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


            FilledTonalButton(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(WtaRadius.Button)
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.pwaApplyAll)
            }
        }
    }
}




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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeCard(
    selectedTheme: String,
    onThemeChange: (String) -> Unit
) {

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


    val languageOptions = TranslateLanguage.entries.toList()


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
                    Column {
                        Text(
                            text = Strings.autoTranslate,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                WtaSwitch(
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
                    WtaSwitch(
                        checked = config.showFloatingButton,
                        onCheckedChange = { onConfigChange(config.copy(showFloatingButton = it)) }
                    )
                }


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
                    WtaSwitch(
                        checked = config.autoTranslateOnLoad,
                        onCheckedChange = { onConfigChange(config.copy(autoTranslateOnLoad = it)) }
                    )
                }
              }
            }
        }
    }
}
