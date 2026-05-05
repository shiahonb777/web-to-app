package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Announcement
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.webtoapp.R
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.webtoapp.core.apkbuilder.ApkBuilder
import com.webtoapp.core.apkbuilder.ApkExportPreflight
import com.webtoapp.core.apkbuilder.ApkExportPreflightReport
import com.webtoapp.core.apkbuilder.BuildResult
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.dao.WebAppSummary
import com.webtoapp.data.model.AppCategory
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.components.CategoryEditorDialog
import com.webtoapp.ui.components.CategoryTabRow
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.ApkExportPreflightPanel
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.LanguageSelectorButton
import com.webtoapp.ui.components.MoveToCategoryDialog
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.theme.LocalAnimationSettings
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.ui.theme.ThemeManager
import com.webtoapp.ui.theme.LocalAppTheme
import com.webtoapp.ui.theme.LocalThemeRevealState
import com.webtoapp.ui.animation.StaggeredAnimatedItem
import com.webtoapp.ui.animation.breathingFloat
import com.webtoapp.ui.animation.AnimatedAlertDialog
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.webtoapp.ui.components.liquidGlass
import com.webtoapp.ui.design.WtaBadge




@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onCreateApp: () -> Unit,
    onCreateMediaApp: () -> Unit = {},
    onCreateGalleryApp: () -> Unit = {},
    onCreateHtmlApp: () -> Unit = {},
    onCreateFrontendApp: () -> Unit = {},
    onCreateNodeJsApp: () -> Unit = {},
    onCreateWordPressApp: () -> Unit = {},
    onCreatePhpApp: () -> Unit = {},
    onCreatePythonApp: () -> Unit = {},
    onCreateGoApp: () -> Unit = {},
    onCreateMultiWebApp: () -> Unit = {},
    onCreateOfflinePack: () -> Unit = {},
    onEditApp: (WebApp) -> Unit,
    onEditAppCore: (WebApp) -> Unit = {},
    onPreviewApp: (WebApp) -> Unit,
    onOpenAppModifier: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
    onOpenAiCoding: () -> Unit = {},
    onOpenAiHtmlCoding: () -> Unit = {},
    onOpenExtensionModules: () -> Unit = {},
    onOpenLinuxEnvironment: () -> Unit = {},
) {
    val apps by viewModel.filteredSummaries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    var showCategoryEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<AppCategory?>(null) }
    var showMoveToCategoryDialog by remember { mutableStateOf(false) }
    var appToMove by remember { mutableStateOf<WebAppSummary?>(null) }

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<WebAppSummary?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBuildDialog by remember { mutableStateOf(false) }
    var buildingApp by remember { mutableStateOf<WebApp?>(null) }
    var shareApkFailureReport by remember { mutableStateOf<BuildFailureReport?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }


    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar((uiState as UiState.Success).message)
                viewModel.resetUiState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar((uiState as UiState.Error).message)
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    val createMenuScrollState = rememberScrollState()

    data class CreateActionItem(
        val label: String,
        val iconRes: Int,
        val onClick: () -> Unit
    )
    val createActionItems = listOf(
        CreateActionItem(Strings.appTypeWeb, R.drawable.ic_type_web, onCreateApp),
        CreateActionItem(Strings.appTypeMultiWeb, R.drawable.ic_type_web, onCreateMultiWebApp),
        CreateActionItem(Strings.appTypeHtml, R.drawable.ic_type_html, onCreateHtmlApp),
        CreateActionItem(Strings.websiteOfflinePack, R.drawable.ic_type_html, onCreateOfflinePack),
        CreateActionItem(Strings.appTypeFrontend, R.drawable.ic_type_frontend, onCreateFrontendApp),
        CreateActionItem(Strings.appTypePhp, R.drawable.ic_type_php, onCreatePhpApp),
        CreateActionItem(Strings.appTypeWordPress, R.drawable.ic_type_wordpress, onCreateWordPressApp),
        CreateActionItem(Strings.appTypeNodeJs, R.drawable.ic_type_nodejs, onCreateNodeJsApp),
        CreateActionItem(Strings.appTypePython, R.drawable.ic_type_python, onCreatePythonApp),
        CreateActionItem(Strings.appTypeGo, R.drawable.ic_type_go, onCreateGoApp),
        CreateActionItem(Strings.createMediaApp, R.drawable.ic_type_media, onCreateMediaApp),
        CreateActionItem(Strings.appTypeGallery, R.drawable.ic_type_gallery, onCreateGalleryApp)
    )

    WtaScreen(
        title = Strings.myApps,
        snackbarHostState = snackbarHostState,
        actions = {
            if (isSearchActive) {
                PremiumTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = { Text(Strings.search, style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    modifier = Modifier
                        .widthIn(max = 180.dp)
                        .height(48.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

                    val context = LocalContext.current
                    val themeManager = remember { ThemeManager.getInstance(context) }
                    val darkModeState by themeManager.darkModeFlow.collectAsStateWithLifecycle()
                    val isDarkNow = darkModeState == ThemeManager.DarkModeSettings.DARK


                    val revealState = LocalThemeRevealState.current
                    val view = LocalView.current
                    val activity = context as? android.app.Activity


                    var buttonCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                    IconButton(
                        onClick = {
                            val switchToDark = !isDarkNow

                            if (revealState != null) {


                                revealState.triggerReveal(
                                    center = buttonCenter,
                                    switchToDark = switchToDark,
                                    view = view,
                                    window = activity?.window
                                ) {

                                    scope.launch {
                                        val newMode = if (switchToDark) {
                                            ThemeManager.DarkModeSettings.DARK
                                        } else {
                                            ThemeManager.DarkModeSettings.LIGHT
                                        }
                                        themeManager.setDarkMode(newMode)
                                    }
                                }
                            } else {

                                scope.launch {
                                    val newMode = if (switchToDark) {
                                        ThemeManager.DarkModeSettings.DARK
                                    } else {
                                        ThemeManager.DarkModeSettings.LIGHT
                                    }
                                    themeManager.setDarkMode(newMode)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .onGloballyPositioned { coords ->
                                val bounds = coords.boundsInRoot()
                                buttonCenter = androidx.compose.ui.geometry.Offset(
                                    bounds.left + bounds.width / 2,
                                    bounds.top + bounds.height / 2
                                )
                            }
                    ) {
                        Icon(
                            imageVector = if (isDarkNow) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                            contentDescription = if (isDarkNow) {
                                stringResource(com.webtoapp.R.string.theme_dark)
                            } else {
                                stringResource(com.webtoapp.R.string.theme_light)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }


                    LanguageSelectorButton(
                        onLanguageChanged = {

                            scope.launch {
                                snackbarHostState.showSnackbar(Strings.msgLanguageChanged)
                            }
                        }
                    )


                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.search("")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = Strings.search,
                            modifier = Modifier.size(20.dp)
                        )
                    }


        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            CategoryTabRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { viewModel.selectCategory(it) },
                onAddCategory = {
                    editingCategory = null
                    showCategoryEditor = true
                },
                onEditCategory = { category ->
                    editingCategory = category
                    showCategoryEditor = true
                },
                onDeleteCategory = { viewModel.deleteCategory(it) }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true)
            ) {
                if (apps.isEmpty()) {

                    EmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        showCreateButton = !showFabMenu,
                        onCreateApp = onCreateApp
                    )
                } else {

                    val listContext = LocalContext.current
                    val sharedExporter = remember { com.webtoapp.core.export.AppExporter(listContext) }
                    val sharedApkBuilder = remember { ApkBuilder(listContext) }
                    val sharedScope = rememberCoroutineScope()


                    val healthMonitor: com.webtoapp.core.stats.AppHealthMonitor? = remember {
                        try { org.koin.java.KoinJavaComponent.get(com.webtoapp.core.stats.AppHealthMonitor::class.java) }
                        catch (e: Exception) { null }
                    }
                    val healthRecordsState = healthMonitor?.allHealthRecords?.collectAsState(initial = emptyList<com.webtoapp.core.stats.AppHealthRecord>())
                    val healthRecords: List<com.webtoapp.core.stats.AppHealthRecord> = healthRecordsState?.value ?: emptyList()
                    val healthMap = remember(healthRecords) { healthRecords.associateBy { it.appId } }


                    fun resolveScreenshotService(): com.webtoapp.core.stats.WebsiteScreenshotService? {
                        return try {
                            org.koin.java.KoinJavaComponent.get(com.webtoapp.core.stats.WebsiteScreenshotService::class.java)
                        } catch (e: Exception) {
                            val resolveMessage = "service resolve failed: ${e.message}"
                            com.webtoapp.core.logging.AppLogger.w("ScreenshotFlow", resolveMessage)
                            android.util.Log.w("ScreenshotFlow", resolveMessage, e)
                            null
                        }
                    }
                    val screenshotService: com.webtoapp.core.stats.WebsiteScreenshotService? = resolveScreenshotService()



                    val previewImageLoader = remember(listContext) {
                        coil.Coil.imageLoader(listContext)
                    }


                    val screenshotVersions = remember { mutableStateMapOf<Long, Int>() }
                    val screenshotLoadingStates = remember { mutableStateMapOf<Long, Boolean>() }

                    val previewSpecs = remember { mutableStateMapOf<Long, AppPreviewSpec>() }
                    LaunchedEffect(apps, listContext) {
                        val currentIds = apps.map { it.id }.toHashSet()
                        val stale = previewSpecs.keys - currentIds
                        if (stale.isNotEmpty()) {
                            stale.forEach { previewSpecs.remove(it) }
                        }
                        val missingIds = apps.mapNotNull { summary ->
                            summary.id.takeIf { it !in previewSpecs }
                        }
                        if (missingIds.isEmpty()) {
                            return@LaunchedEffect
                        }
                        withContext(Dispatchers.IO) {
                            for (id in missingIds) {
                                val webApp = viewModel.getWebApp(id) ?: continue
                                val spec = resolveAppPreviewSpec(listContext.applicationContext, webApp)
                                withContext(Dispatchers.Main) {
                                    previewSpecs[id] = spec
                                }
                            }
                        }
                    }

                    val latestApps = rememberUpdatedState(apps)
                    val latestPreviewSpecs = rememberUpdatedState(previewSpecs.toMap())
                    val captureSignature = remember(previewSpecs.toMap()) {
                        previewSpecs.entries
                            .mapNotNull { (id, spec) -> spec.captureUrl?.let { "$id:$it" } }
                            .sorted()
                            .joinToString("|")
                    }

                    LaunchedEffect(screenshotService, captureSignature) {
                        val svc = screenshotService ?: run {
                            com.webtoapp.core.logging.AppLogger.i(
                                "ScreenshotFlow",
                                "init skipped: service unavailable"
                            )
                            return@LaunchedEffect
                        }
                        if (captureSignature.isEmpty()) {
                            return@LaunchedEffect
                        }

                        delay(500)

                        val appsNow = latestApps.value
                        val specsNow = latestPreviewSpecs.value
                        val captureTargets = appsNow.mapNotNull { app ->
                            specsNow[app.id]?.captureUrl?.let { captureUrl -> app to captureUrl }
                        }
                        com.webtoapp.core.logging.AppLogger.i(
                            "ScreenshotFlow",
                            "init effect: apps=${appsNow.size}, captureTargets=${captureTargets.size}"
                        )
                        for ((app, captureUrl) in captureTargets) {
                            if (svc.hasScreenshot(app.id)) continue
                            screenshotLoadingStates[app.id] = true
                            try {
                                svc.captureScreenshot(app.id, captureUrl)
                                com.webtoapp.core.logging.AppLogger.i(
                                    "ScreenshotFlow",
                                    "initial capture finished: appId=${app.id}, name=${app.name}"
                                )
                            } catch (e: Exception) {
                                com.webtoapp.core.logging.AppLogger.e(
                                    "ScreenshotFlow",
                                    "initial capture exception: appId=${app.id}, error=${e.message}",
                                    e
                                )
                            } finally {
                                screenshotLoadingStates[app.id] = false
                                screenshotVersions[app.id] = (screenshotVersions[app.id] ?: 0) + 1
                            }
                        }
                    }

                    LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        apps,
                        key = { _, app -> app.id },
                        contentType = { _, _ -> "AppCard" }
                    ) { index, app ->
                        val exporter = sharedExporter
                        val scope = sharedScope
                        val previewSpec = previewSpecs[app.id] ?: AppPreviewSpec()


                        StaggeredAnimatedItem(index = index) {


                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    selectedApp = app
                                    showDeleteDialog = true
                                    false
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {

                                val bgAlpha by animateFloatAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f,
                                    label = "dismissBgAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(WtaRadius.Card))
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f * bgAlpha)
                                        )
                                        .padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = Strings.btnDelete,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = bgAlpha),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .graphicsLayer {
                                                scaleX = 0.7f + 0.3f * bgAlpha
                                                scaleY = 0.7f + 0.3f * bgAlpha
                                            }
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {

                        AppCard(
                            app = app,
                            onClick = {
                                scope.launch {
                                    viewModel.getWebApp(app.id)?.let(onPreviewApp)
                                }
                            },
                            onLongClick = { selectedApp = app },
                            onEdit = {
                                scope.launch {
                                    viewModel.getWebApp(app.id)?.let(onEditApp)
                                }
                            },
                            onEditCore = {
                                scope.launch {
                                    viewModel.getWebApp(app.id)?.let(onEditAppCore)
                                }
                            },
                            onDelete = {
                                selectedApp = app
                                showDeleteDialog = true
                            },
                            onCreateShortcut = {
                                scope.launch {
                                    val fullApp = viewModel.getWebApp(app.id) ?: return@launch
                                    when (val result = exporter.createShortcut(fullApp)) {
                                        is com.webtoapp.core.export.ShortcutResult.Success -> {
                                            snackbarHostState.showSnackbar(Strings.shortcutCreatedSuccess)
                                        }
                                        is com.webtoapp.core.export.ShortcutResult.Pending -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                        is com.webtoapp.core.export.ShortcutResult.PermissionRequired -> {
                                            snackbarHostState.showSnackbar(
                                                message = result.message,
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                        is com.webtoapp.core.export.ShortcutResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            },
                            onExport = {
                                scope.launch {
                                    val fullApp = viewModel.getWebApp(app.id) ?: return@launch
                                    when (val result = exporter.exportAsTemplate(fullApp)) {
                                        is com.webtoapp.core.export.ExportResult.Success -> {
                                            snackbarHostState.showSnackbar(Strings.projectExportedTo.replace("%s", result.path))
                                        }
                                        is com.webtoapp.core.export.ExportResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            },
                            onBuildApk = {
                                scope.launch {
                                    val fullApp = viewModel.getWebApp(app.id) ?: return@launch
                                    buildingApp = fullApp
                                    showBuildDialog = true
                                }
                            },
                            onShareApk = {
                                scope.launch {
                                    val fullApp = viewModel.getWebApp(app.id) ?: return@launch
                                    shareApkFailureReport = null
                                    snackbarHostState.showSnackbar(Strings.shareApkBuilding)
                                    val apkBuilder = sharedApkBuilder
                                    val result = apkBuilder.buildApk(fullApp) { _, _ -> }
                                    when (result) {
                                        is BuildResult.Success -> {

                                            try {
                                                val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                                    listContext,
                                                    "${listContext.packageName}.fileprovider",
                                                    result.apkFile
                                                )
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "application/vnd.android.package-archive"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, apkUri)
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, Strings.shareApkTitle.replace("%s", fullApp.name))
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                listContext.startActivity(android.content.Intent.createChooser(shareIntent, Strings.shareApkTitle.replace("%s", fullApp.name)))
                                            } catch (e: Exception) {
                                                shareApkFailureReport = buildActionFailureReport(
                                                    title = Strings.apkShareFailed,
                                                    stage = "share_apk_intent",
                                                    webApp = fullApp,
                                                    summary = Strings.shareApkFailed.replace("%s", e.message ?: "Unknown error"),
                                                    logPath = result.logPath,
                                                    throwable = e,
                                                    extraLines = listOf(
                                                        "apkPath=${result.apkFile.absolutePath}",
                                                        "apkSize=${result.apkFile.length()}"
                                                    )
                                                )
                                            }
                                        }
                                        is BuildResult.Error -> {
                                            shareApkFailureReport = buildActionFailureReport(
                                                title = Strings.apkShareFailed,
                                                stage = result.diagnostic?.stage?.label ?: "build_apk_for_share",
                                                webApp = fullApp,
                                                summary = Strings.shareApkFailed.replace("%s", result.message),
                                                logPath = result.logPath,
                                                extraLines = buildDiagnosticLines(result)
                                            )
                                        }
                                    }
                                }
                            },
                            onMoveToCategory = {
                                appToMove = app
                                showMoveToCategoryDialog = true
                            },
                            healthStatus = healthMap[app.id]?.status,
                            previewImageLoader = previewImageLoader,
                            screenshotPath = if (previewSpec.captureUrl != null) {
                                screenshotService?.let { svc ->
                                    if (svc.hasScreenshot(app.id)) svc.getScreenshotPath(app.id) else null
                                }
                            } else {
                                previewSpec.previewFilePath
                            },
                            screenshotVersion = screenshotVersions[app.id] ?: 0,
                            isScreenshotLoading = screenshotLoadingStates[app.id] == true,
                            onCaptureScreenshot = if (previewSpec.captureUrl != null) {
                                {
                                    val resolvedService = screenshotService ?: resolveScreenshotService()
                                    if (resolvedService == null) {
                                        val unavailableMessage = "manual capture aborted: service unavailable, appId=${app.id}, name=${app.name}"
                                        com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", unavailableMessage)
                                        android.util.Log.i("ScreenshotFlow", unavailableMessage)
                                    } else {
                                        val tapMessage = "HomeScreen callback entered: appId=${app.id}, name=${app.name}, hasScreenshot=${resolvedService.hasScreenshot(app.id)}"
                                        com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", tapMessage)
                                        android.util.Log.i("ScreenshotFlow", tapMessage)
                                        scope.launch {
                                            screenshotLoadingStates[app.id] = true
                                            val startMessage = "manual capture coroutine start: appId=${app.id}, name=${app.name}, target=${previewSpec.captureUrl}"
                                            com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", startMessage)
                                            android.util.Log.i("ScreenshotFlow", startMessage)
                                            try {
                                                com.webtoapp.core.logging.AppLogger.d(
                                                    "HomeScreen",
                                                    "manual screenshot requested: appId=${app.id}, name=${app.name}, target=${previewSpec.captureUrl}"
                                                )
                                                val result = resolvedService.captureScreenshot(app.id, previewSpec.captureUrl)
                                                val finishMessage = "manual capture finished: appId=${app.id}, path=$result, exists=${resolvedService.hasScreenshot(app.id)}"
                                                com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", finishMessage)
                                                android.util.Log.i("ScreenshotFlow", finishMessage)
                                                com.webtoapp.core.logging.AppLogger.d(
                                                    "HomeScreen",
                                                    "manual screenshot finished: appId=${app.id}, path=$result, exists=${resolvedService.hasScreenshot(app.id)}"
                                                )
                                            } catch (e: Exception) {
                                                val errorMessage = "manual capture exception: appId=${app.id}, error=${e.message}"
                                                com.webtoapp.core.logging.AppLogger.e("ScreenshotFlow", errorMessage, e)
                                                android.util.Log.e("ScreenshotFlow", errorMessage, e)
                                            } finally {
                                                screenshotLoadingStates[app.id] = false
                                                screenshotVersions[app.id] = (screenshotVersions[app.id] ?: 0) + 1
                                            }
                                        }
                                    }
                                }
                            } else null,
                            modifier = Modifier.animateItemPlacement()
                        )
                        }
                        }
                    }


                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                }
            }


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {

                        AnimatedVisibility(
                            visible = showFabMenu,
                            enter = expandVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.75f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                expandFrom = Alignment.Bottom
                            ) + fadeIn(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                shrinkTowards = Alignment.Bottom
                            ) + fadeOut(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        ) {
                            EnhancedElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 280.dp)
                                        .verticalScroll(createMenuScrollState)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    createActionItems.chunked(2).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowItems.forEach { item ->
                                                FilledTonalButton(
                                                    onClick = {
                                                        showFabMenu = false
                                                        item.onClick()
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(painterResource(item.iconRes), null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        item.label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                            if (rowItems.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        val fabRotation by animateFloatAsState(
                            targetValue = if (showFabMenu) 135f else 0f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "fabRotation"
                        )

                        FilledTonalButton(
                            onClick = { showFabMenu = !showFabMenu },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(WtaRadius.Button),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                Strings.btnCreate,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = fabRotation }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (showFabMenu) Strings.close else Strings.createApp,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
            }
    }


    if (showBuildDialog && buildingApp != null) {
        BuildApkDialog(
            webApp = buildingApp!!,
            onDismiss = {
                showBuildDialog = false
                buildingApp = null
            },
            onResult = { message ->
                showBuildDialog = false
                buildingApp = null
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    shareApkFailureReport?.let { report ->
        BuildFailureReportDialog(
            report = report,
            onDismiss = { shareApkFailureReport = null }
        )
    }


    if (showDeleteDialog && selectedApp != null) {
        AnimatedAlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedApp = null
            },
            title = { Text(Strings.deleteConfirmTitle) },
            text = { Text(Strings.deleteConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedApp?.let { viewModel.deleteAppById(it.id) }
                        showDeleteDialog = false
                        selectedApp = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.btnDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedApp = null
                }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }


    if (showCategoryEditor) {
        CategoryEditorDialog(
            category = editingCategory,
            onDismiss = {
                showCategoryEditor = false
                editingCategory = null
            },
            onSave = { name, icon ->
                if (editingCategory != null) {
                    viewModel.updateCategory(
                        editingCategory!!.copy(name = name, icon = icon)
                    )
                } else {
                    viewModel.createCategory(name, icon)
                }
                showCategoryEditor = false
                editingCategory = null
            }
        )
    }


    if (showMoveToCategoryDialog && appToMove != null) {
        val summary = appToMove!!
        MoveToCategoryDialog(
            appName = summary.name,
            currentCategoryId = summary.categoryId,
            categories = categories,
            onDismiss = {
                showMoveToCategoryDialog = false
                appToMove = null
            },
            onMoveToCategory = { categoryId ->
                viewModel.moveAppToCategoryById(summary.id, categoryId)
                showMoveToCategoryDialog = false
                appToMove = null
            }
        )
    }


    if (showBatchImportDialog) {
        val importService = remember { org.koin.java.KoinJavaComponent.get<com.webtoapp.core.stats.BatchImportService>(com.webtoapp.core.stats.BatchImportService::class.java) }
        BatchImportDialog(
            importService = importService,
            onDismiss = { showBatchImportDialog = false },
            onImport = { entries ->
                importService.importEntries(entries)
            }
        )
    }

}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh),
        label = "sidebarItemScale"
    )

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(WtaRadius.Control))
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarMenuItem(
    label: String,
    iconPainter: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh),
        label = "sidebarItemScale"
    )

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(WtaRadius.Control))
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}




@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppCard(
    app: WebAppSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onEditCore: () -> Unit = {},
    onDelete: () -> Unit,
    onCreateShortcut: () -> Unit = {},
    onExport: () -> Unit = {},
    onBuildApk: () -> Unit = {},
    onShareApk: () -> Unit = {},
    onMoveToCategory: () -> Unit = {},
    healthStatus: com.webtoapp.core.stats.HealthStatus? = null,
    previewImageLoader: ImageLoader,
    screenshotPath: String? = null,
    screenshotVersion: Int = 0,
    isScreenshotLoading: Boolean = false,
    onCaptureScreenshot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current

    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (app.iconPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(app.iconPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = app.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val defaultIconRes = when (app.appType) {
                        com.webtoapp.data.model.AppType.WEB -> R.drawable.ic_type_web
                        com.webtoapp.data.model.AppType.IMAGE -> R.drawable.ic_type_media
                        com.webtoapp.data.model.AppType.VIDEO -> R.drawable.ic_type_media
                        com.webtoapp.data.model.AppType.HTML -> R.drawable.ic_type_html
                        com.webtoapp.data.model.AppType.GALLERY -> R.drawable.ic_type_gallery
                        com.webtoapp.data.model.AppType.FRONTEND -> R.drawable.ic_type_frontend
                        com.webtoapp.data.model.AppType.WORDPRESS -> R.drawable.ic_type_wordpress
                        com.webtoapp.data.model.AppType.NODEJS_APP -> R.drawable.ic_type_nodejs
                        com.webtoapp.data.model.AppType.PHP_APP -> R.drawable.ic_type_php
                        com.webtoapp.data.model.AppType.PYTHON_APP -> R.drawable.ic_type_python
                        com.webtoapp.data.model.AppType.GO_APP -> R.drawable.ic_type_go
                        com.webtoapp.data.model.AppType.MULTI_WEB -> R.drawable.ic_type_web
                    }
                    Icon(
                        painter = painterResource(defaultIconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (healthStatus != null && healthStatus != com.webtoapp.core.stats.HealthStatus.UNKNOWN) {
                val dotColor = when (healthStatus) {
                    com.webtoapp.core.stats.HealthStatus.ONLINE -> AppColors.Success
                    com.webtoapp.core.stats.HealthStatus.SLOW -> AppColors.Warning
                    com.webtoapp.core.stats.HealthStatus.OFFLINE -> AppColors.Error
                    else -> AppColors.Gray
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(12.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(dotColor)
                )
            }
            }

            Spacer(modifier = Modifier.width(16.dp))


            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (app.appType) {
                        com.webtoapp.data.model.AppType.IMAGE -> app.url.ifBlank { Strings.appTypeImage }
                        com.webtoapp.data.model.AppType.VIDEO -> app.url.ifBlank { Strings.appTypeVideo }
                        com.webtoapp.data.model.AppType.HTML,
                        com.webtoapp.data.model.AppType.FRONTEND -> app.url.ifBlank { Strings.appTypeHtml }
                        com.webtoapp.data.model.AppType.GALLERY -> app.url.ifBlank { Strings.appTypeGallery }
                        else -> app.url
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))


                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                    AppTypeChip(appType = app.appType)
                    if (app.activationEnabled) {
                        FeatureChip(icon = Icons.Outlined.Key, label = Strings.activationCodeVerify)
                    }
                    if (app.adBlockEnabled) {
                        FeatureChip(icon = Icons.Outlined.Block, label = Strings.adBlocking)
                    }
                    if (app.announcementEnabled) {
                        FeatureChip(icon = Icons.Outlined.Info, label = Strings.popupAnnouncement)
                    }
                }
            }


            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(WtaRadius.Card))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = onCaptureScreenshot != null) {
                        val clickMessage = "thumbnail tapped: appId=${app.id}, hasHandler=${onCaptureScreenshot != null}, hasPath=${screenshotPath != null}, version=$screenshotVersion, loading=$isScreenshotLoading"
                        com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", clickMessage)
                        android.util.Log.i("ScreenshotFlow", clickMessage)
                        onCaptureScreenshot?.invoke()
                    }
            ) {
                if (screenshotPath != null) {
                    Box(modifier = Modifier.fillMaxSize()) {


                        val screenshotCacheKey = "wta_shot_${app.id}_v$screenshotVersion"
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(java.io.File(screenshotPath))
                                .memoryCacheKey(screenshotCacheKey)
                                .diskCacheKey(screenshotCacheKey)
                                .crossfade(true)
                                .build(),
                            imageLoader = previewImageLoader,
                            contentDescription = Strings.btnPreview,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(WtaRadius.Card)),
                            contentScale = ContentScale.Crop
                        )
                        if (isScreenshotLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScreenshotLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (onCaptureScreenshot != null) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = Strings.btnPreview,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                painter = painterResource(when (app.appType) {
                                    com.webtoapp.data.model.AppType.WEB -> R.drawable.ic_type_web
                                    com.webtoapp.data.model.AppType.IMAGE -> R.drawable.ic_type_media
                                    com.webtoapp.data.model.AppType.VIDEO -> R.drawable.ic_type_media
                                    com.webtoapp.data.model.AppType.HTML -> R.drawable.ic_type_html
                                    com.webtoapp.data.model.AppType.GALLERY -> R.drawable.ic_type_gallery
                                    com.webtoapp.data.model.AppType.FRONTEND -> R.drawable.ic_type_frontend
                                    com.webtoapp.data.model.AppType.WORDPRESS -> R.drawable.ic_type_wordpress
                                    com.webtoapp.data.model.AppType.NODEJS_APP -> R.drawable.ic_type_nodejs
                                    com.webtoapp.data.model.AppType.PHP_APP -> R.drawable.ic_type_php
                                    com.webtoapp.data.model.AppType.PYTHON_APP -> R.drawable.ic_type_python
                                    com.webtoapp.data.model.AppType.GO_APP -> R.drawable.ic_type_go
                                    com.webtoapp.data.model.AppType.MULTI_WEB -> R.drawable.ic_type_web
                                }),
                                contentDescription = Strings.btnPreview,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }


            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = Strings.more)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {

                    if (app.appType == com.webtoapp.data.model.AppType.WEB) {

                        DropdownMenuItem(
                            text = { Text(Strings.btnEdit) },
                            onClick = {
                                expanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                    } else {

                        DropdownMenuItem(
                            text = { Text(Strings.editCoreConfig) },
                            onClick = {
                                expanded = false
                                onEditCore()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Tune, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.editCommonConfig) },
                            onClick = {
                                expanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Settings, null) }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(Strings.btnShortcut) },
                        onClick = {
                            expanded = false
                            onCreateShortcut()
                        },
                        leadingIcon = { Icon(Icons.Outlined.AppShortcut, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.buildDialogTitle) },
                        onClick = {
                            expanded = false
                            onBuildApk()
                        },
                        leadingIcon = { Icon(Icons.Outlined.InstallMobile, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.shareApk) },
                        onClick = {
                            expanded = false
                            onShareApk()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.btnExport) },
                        onClick = {
                            expanded = false
                            onExport()
                        },
                        leadingIcon = { Icon(Icons.Outlined.FileDownload, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.moveToCategory) },
                        onClick = {
                            expanded = false
                            onMoveToCategory()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Folder, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(Strings.btnDelete) },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}




@Composable
fun FeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    WtaBadge(
        text = label,
        icon = icon,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}




@Composable
fun AppTypeChip(appType: com.webtoapp.data.model.AppType) {
    val (icon, label) = when (appType) {
        com.webtoapp.data.model.AppType.WEB -> Pair(
            Icons.Outlined.Public,
            Strings.appTypeWeb
        )
        com.webtoapp.data.model.AppType.IMAGE -> Pair(
            Icons.Outlined.Image,
            Strings.appTypeImage
        )
        com.webtoapp.data.model.AppType.VIDEO -> Pair(
            Icons.Outlined.VideoLibrary,
            Strings.appTypeVideo
        )
        com.webtoapp.data.model.AppType.HTML -> Pair(
            Icons.Outlined.Html,
            Strings.appTypeHtml
        )
        com.webtoapp.data.model.AppType.GALLERY -> Pair(
            Icons.Outlined.PhotoLibrary,
            Strings.appTypeGallery
        )
        com.webtoapp.data.model.AppType.FRONTEND -> Pair(
            Icons.Outlined.Rocket,
            Strings.appTypeFrontend
        )
        com.webtoapp.data.model.AppType.WORDPRESS -> Pair(
            Icons.Outlined.Newspaper,
            Strings.appTypeWordPress
        )
        com.webtoapp.data.model.AppType.NODEJS_APP -> Pair(
            Icons.Outlined.Terminal,
            Strings.appTypeNodeJs
        )
        com.webtoapp.data.model.AppType.PHP_APP -> Pair(
            Icons.Outlined.DataObject,
            Strings.appTypePhp
        )
        com.webtoapp.data.model.AppType.PYTHON_APP -> Pair(
            Icons.Outlined.Psychology,
            Strings.appTypePython
        )
        com.webtoapp.data.model.AppType.GO_APP -> Pair(
            Icons.Outlined.Speed,
            Strings.appTypeGo
        )
        com.webtoapp.data.model.AppType.MULTI_WEB -> Pair(
            Icons.Outlined.Language,
            Strings.appTypeMultiWeb
        )
    }

    WtaBadge(
        text = label,
        icon = icon,
        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}




@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    showCreateButton: Boolean = true,
    onCreateApp: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.AppShortcut,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .breathingFloat(),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = Strings.msgNoApps,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Strings.emptyStateHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        AnimatedVisibility(visible = showCreateButton) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                PremiumButton(onClick = onCreateApp) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.createApp)
                }
            }
        }
    }
}

private data class BuildFailureReport(
    val title: String,
    val summary: String,
    val details: String
)

private fun readBuildLogTail(path: String?, maxChars: Int = 20000): String {
    return try {
        path
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() && it.isFile }
            ?.readText()
            ?.let { content ->
                if (content.length <= maxChars) content else content.takeLast(maxChars)
            }
    } catch (e: Exception) {
        AppLogger.e("HomeScreen", "读取 APK 构建日志失败", e)
        Strings.readBuildLogFailed.format(e.message ?: "Unknown error")
    } ?: "<build log unavailable>"
}

private fun buildActionFailureReport(
    title: String,
    stage: String,
    webApp: WebApp,
    summary: String,
    logPath: String? = null,
    throwable: Throwable? = null,
    extraLines: List<String> = emptyList()
): BuildFailureReport {
    throwable?.let { AppLogger.e("HomeScreen", "$title failed at $stage", it) }
    val buildLog = readBuildLogTail(logPath)

    val details = buildString {
        appendLine(title)
        appendLine("stage: $stage")
        appendLine("summary: $summary")
        appendLine()
        appendLine("project:")
        appendLine("name=${webApp.name}")
        appendLine("appType=${webApp.appType}")
        appendLine("source=${webApp.url}")
        if (extraLines.isNotEmpty()) {
            appendLine()
            appendLine("context:")
            extraLines.forEach { appendLine(it) }
        }
        appendLine()
        appendLine("log_path:")
        appendLine(logPath ?: "<unavailable>")
        appendLine()
        appendLine("build_log:")
        appendLine(buildLog)
        if (throwable != null) {
            appendLine()
            appendLine("exception:")
            appendLine(android.util.Log.getStackTraceString(throwable))
        }
        appendLine()
        appendLine("recent_logs:")
        append(AppLogger.getRecentLogTail())
    }

    return BuildFailureReport(
        title = title,
        summary = summary,
        details = details
    )
}

private fun buildBuildFailureReport(
    webApp: WebApp,
    error: BuildResult.Error
): BuildFailureReport {
    return buildActionFailureReport(
        title = Strings.apkBuildFailed,
        stage = error.diagnostic?.stage?.label ?: "apk_build",
        webApp = webApp,
        summary = error.message,
        logPath = error.logPath,
        extraLines = buildDiagnosticLines(error)
    )
}

private fun buildDiagnosticLines(error: BuildResult.Error): List<String> {
    val diagnostic = error.diagnostic ?: return emptyList()
    return buildList {
        add("failureStage=${diagnostic.stage.name}")
        add("failureCause=${diagnostic.cause.name}")
        diagnostic.details.forEach { (key, value) ->
            add("$key=$value")
        }
    }
}

@Composable
private fun BuildFailureReportDialog(
    report: BuildFailureReport,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(report.title)
                Text(
                    report.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            EnhancedElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = report.details,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .padding(bottom = 48.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )

                    FilledTonalButton(
                        onClick = { clipboardManager.setText(AnnotatedString(report.details)) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.copy)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.close)
            }
        }
    )
}




@Composable
fun BuildApkDialog(
    webApp: WebApp,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apkBuilder = remember { ApkBuilder(context) }

    var isBuilding by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var progressText by remember { mutableStateOf(Strings.preparing) }
    var analysisReport by remember { mutableStateOf<com.webtoapp.core.apkbuilder.ApkAnalyzer.AnalysisReport?>(null) }
    var buildFailureReport by remember { mutableStateOf<BuildFailureReport?>(null) }
    var preflightReport by remember { mutableStateOf<ApkExportPreflightReport?>(null) }


    var encryptionConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.encryptionConfig ?: com.webtoapp.data.model.ApkEncryptionConfig())
    }


    var hardeningConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.hardeningConfig ?: com.webtoapp.data.model.AppHardeningConfig())
    }


    var isolationConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.isolationConfig ?: com.webtoapp.core.isolation.IsolationConfig())
    }


    var backgroundRunEnabled by remember {
        mutableStateOf(webApp.apkExportConfig?.backgroundRunEnabled ?: false)
    }
    var backgroundRunConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.backgroundRunConfig ?: com.webtoapp.data.model.BackgroundRunExportConfig())
    }


    var notificationEnabled by remember {
        mutableStateOf(webApp.apkExportConfig?.notificationEnabled ?: false)
    }
    var notificationConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.notificationConfig ?: com.webtoapp.data.model.NotificationExportConfig())
    }


    var dnsMode by remember {
        mutableStateOf(webApp.webViewConfig.dnsMode)
    }
    var dnsConfig by remember {
        mutableStateOf(webApp.webViewConfig.dnsConfig)
    }


    var selectedEngineType by remember {
        mutableStateOf(webApp.apkExportConfig?.engineType ?: "SYSTEM_WEBVIEW")
    }
    val engineFileManager = remember { com.webtoapp.core.engine.download.EngineFileManager(context) }
    val isGeckoDownloaded = remember(selectedEngineType) {
        engineFileManager.isEngineDownloaded(com.webtoapp.core.engine.EngineType.GECKOVIEW)
    }

    fun currentBuildConfig(): WebApp {
        return webApp.copy(
            webViewConfig = webApp.webViewConfig.copy(
                dnsMode = dnsMode,
                dnsConfig = dnsConfig
            ),
            apkExportConfig = (webApp.apkExportConfig ?: com.webtoapp.data.model.ApkExportConfig()).copy(
                encryptionConfig = encryptionConfig,
                hardeningConfig = hardeningConfig,
                isolationConfig = isolationConfig,
                backgroundRunEnabled = backgroundRunEnabled,
                backgroundRunConfig = backgroundRunConfig,
                notificationEnabled = notificationEnabled,
                notificationConfig = notificationConfig,
                engineType = selectedEngineType
            )
        )
    }

    LaunchedEffect(
        webApp,
        encryptionConfig,
        hardeningConfig,
        isolationConfig,
        backgroundRunEnabled,
        backgroundRunConfig,
        notificationEnabled,
        notificationConfig,
        dnsMode,
        dnsConfig,
        selectedEngineType
    ) {
        preflightReport = ApkExportPreflight.check(context, currentBuildConfig())
    }

    AnimatedAlertDialog(
        onDismissRequest = { if (!isBuilding) onDismiss() },
        title = { Text(Strings.buildDialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(WtaRadius.Control))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Android,
                            null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(webApp.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            when (webApp.appType) {
                                com.webtoapp.data.model.AppType.IMAGE -> {
                                    webApp.mediaConfig?.mediaPath ?: webApp.url
                                }
                                com.webtoapp.data.model.AppType.VIDEO -> {
                                    webApp.mediaConfig?.mediaPath ?: webApp.url
                                }
                                com.webtoapp.data.model.AppType.HTML -> {
                                    webApp.htmlConfig?.entryFile?.takeIf { it.isNotBlank() } ?: "index.html"
                                }
                                else -> webApp.url
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider()


                com.webtoapp.ui.components.EncryptionConfigCard(
                    config = encryptionConfig,
                    onConfigChange = { encryptionConfig = it }
                )


                com.webtoapp.ui.components.HardeningConfigCard(
                    config = hardeningConfig,
                    onConfigChange = { hardeningConfig = it }
                )


                com.webtoapp.ui.components.IsolationConfigCard(
                    config = isolationConfig,
                    onConfigChange = { isolationConfig = it }
                )


                com.webtoapp.ui.components.BackgroundRunConfigCard(
                    enabled = backgroundRunEnabled,
                    config = backgroundRunConfig,
                    onEnabledChange = { backgroundRunEnabled = it },
                    onConfigChange = { backgroundRunConfig = it }
                )


                com.webtoapp.ui.components.NotificationConfigCard(
                    enabled = notificationEnabled,
                    config = notificationConfig,
                    onEnabledChange = { notificationEnabled = it },
                    onConfigChange = { notificationConfig = it }
                )


                com.webtoapp.ui.components.DnsConfigCard(
                    dnsMode = dnsMode,
                    dnsConfig = dnsConfig,
                    onDnsModeChange = { dnsMode = it },
                    onDnsConfigChange = { dnsConfig = it }
                )


                if (webApp.appType == com.webtoapp.data.model.AppType.WEB) {
                    EngineSelectionCard(
                        selectedEngine = selectedEngineType,
                        isGeckoDownloaded = isGeckoDownloaded,
                        onEngineSelected = { selectedEngineType = it }
                    )
                }

                HorizontalDivider()

                Text(
                    Strings.buildApkForApp.replace("%s", webApp.name),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    Strings.buildCompleteInstallHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                preflightReport?.let { report ->
                    ApkExportPreflightPanel(report = report)
                }


                if (isBuilding) {
                    Spacer(Modifier.height(12.dp))


                    val animatedProgress by animateFloatAsState(
                        targetValue = progress / 100f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "buildProgress"
                    )


                    val pulseAlpha by rememberInfiniteTransition(label = "buildPulse").animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "buildPulseAlpha"
                    )
                    val animPulse by animateFloatAsState(
                        targetValue = pulseAlpha,
                        animationSpec = tween(800),
                        label = "pulseAlpha"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = animPulse)
                            )
                            Text(
                                "${progress}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                progressText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxWidth(),
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                        }
                    }
                }


                analysisReport?.let { report ->
                    HorizontalDivider()


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "APK Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            report.totalSizeFormatted,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(4.dp))


                    report.categories.forEach { cat ->
                        val catColor = try {
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(cat.category.color))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(catColor, RoundedCornerShape(WtaRadius.Button))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                cat.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(weight = 1f, fill = true)
                            )
                            Text(
                                String.format("%.1f%%", cat.percentage),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (cat.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(start = 14.dp)
                                .clip(RoundedCornerShape(WtaRadius.Button)),
                            color = catColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                    }


                    if (report.optimizationHints.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Optimization Hints",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        report.optimizationHints.take(3).forEach { hint ->
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                val icon = when (hint.priority) {
                                    com.webtoapp.core.apkbuilder.ApkAnalyzer.OptimizationHint.Priority.HIGH -> Icons.Outlined.Error
                                    com.webtoapp.core.apkbuilder.ApkAnalyzer.OptimizationHint.Priority.MEDIUM -> Icons.Outlined.Warning
                                    com.webtoapp.core.apkbuilder.ApkAnalyzer.OptimizationHint.Priority.LOW -> Icons.Outlined.Info
                                }
                                val iconColor = when (hint.priority) {
                                    com.webtoapp.core.apkbuilder.ApkAnalyzer.OptimizationHint.Priority.HIGH -> MaterialTheme.colorScheme.error
                                    com.webtoapp.core.apkbuilder.ApkAnalyzer.OptimizationHint.Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                    com.webtoapp.core.apkbuilder.ApkAnalyzer.OptimizationHint.Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Icon(icon, null, Modifier.size(14.dp), tint = iconColor)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    hint.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isBuilding) {
                PremiumButton(
                    onClick = {
                        val webAppWithConfig = currentBuildConfig()
                        val nextPreflight = ApkExportPreflight.check(context, webAppWithConfig)
                        preflightReport = nextPreflight
                        if (nextPreflight.hasErrors) {
                            return@PremiumButton
                        }

                        isBuilding = true
                        scope.launch {
                            val result = apkBuilder.buildApk(webAppWithConfig) { p, t ->
                                progress = p
                                progressText = t
                            }
                            when (result) {
                                is BuildResult.Success -> {
                                    analysisReport = result.analysisReport
                                    buildFailureReport = null
                                    isBuilding = false

                                    apkBuilder.installApk(result.apkFile)
                                    if (result.analysisReport == null) {
                                        onResult("APK 构建成功，正在启动安装...")
                                    }
                                }
                                is BuildResult.Error -> {
                                    buildFailureReport = buildBuildFailureReport(webAppWithConfig, result)
                                    isBuilding = false
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Build, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.btnStartBuild)
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        dismissButton = {
            if (!isBuilding) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.btnCancel)
                }
            }
        }
    )

    buildFailureReport?.let { report ->
        BuildFailureReportDialog(
            report = report,
            onDismiss = { buildFailureReport = null }
        )
    }
}




@Composable
fun EngineSelectionCard(
    selectedEngine: String,
    isGeckoDownloaded: Boolean,
    onEngineSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            Strings.engineSelectTitle,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            Strings.engineSelectDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WtaRadius.Control))
                .clickable { onEngineSelected("SYSTEM_WEBVIEW") }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedEngine == "SYSTEM_WEBVIEW",
                onClick = { onEngineSelected("SYSTEM_WEBVIEW") }
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(weight = 1f, fill = true)) {
                Text(Strings.engineSystemWebView, style = MaterialTheme.typography.bodyMedium)
                Text(
                    Strings.engineSystemWebViewDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WtaRadius.Control))
                .clickable {
                    if (isGeckoDownloaded) onEngineSelected("GECKOVIEW")
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedEngine == "GECKOVIEW",
                onClick = { if (isGeckoDownloaded) onEngineSelected("GECKOVIEW") },
                enabled = isGeckoDownloaded
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(weight = 1f, fill = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Strings.engineGeckoView,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGeckoDownloaded) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isGeckoDownloaded) {
                        Spacer(Modifier.width(6.dp))
                        WtaBadge(
                            text = Strings.engineReady,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (!isGeckoDownloaded) {
                    Text(
                        Strings.engineGeckoNotDownloaded,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        Strings.engineApkSizeWarning.replace("%s", com.webtoapp.core.engine.EngineType.GECKOVIEW.estimatedSizeMb.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
