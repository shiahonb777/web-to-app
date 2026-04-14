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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.webtoapp.core.apkbuilder.BuildResult
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AppCategory
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.components.CategoryEditorDialog
import com.webtoapp.ui.components.CategoryTabRow
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.LanguageSelectorButton
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.MoveToCategoryDialog
import com.webtoapp.ui.i18n.InitializeLanguage
import com.webtoapp.ui.screens.home.components.AppCard
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
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import org.koin.compose.koinInject
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.webtoapp.ui.components.liquidGlass

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    healthMonitor: com.webtoapp.core.stats.AppHealthMonitor? = null,
    screenshotService: com.webtoapp.core.stats.WebsiteScreenshotService? = null,
    batchImportService: com.webtoapp.core.stats.BatchImportService,
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
    InitializeLanguage()
    
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    var showCategoryEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<AppCategory?>(null) }
    var showMoveToCategoryDialog by remember { mutableStateOf(false) }
    var appToMove by remember { mutableStateOf<WebApp?>(null) }

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<WebApp?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBuildDialog by remember { mutableStateOf(false) }
    var buildingApp by remember { mutableStateOf<WebApp?>(null) }
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
        CreateActionItem(Strings.appTypeFrontend, R.drawable.ic_type_frontend, onCreateFrontendApp),
        CreateActionItem(Strings.appTypePhp, R.drawable.ic_type_php, onCreatePhpApp),
        CreateActionItem(Strings.appTypeWordPress, R.drawable.ic_type_wordpress, onCreateWordPressApp),
        CreateActionItem(Strings.appTypeNodeJs, R.drawable.ic_type_nodejs, onCreateNodeJsApp),
        CreateActionItem(Strings.appTypePython, R.drawable.ic_type_python, onCreatePythonApp),
        CreateActionItem(Strings.appTypeGo, R.drawable.ic_type_go, onCreateGoApp),
        CreateActionItem(Strings.createMediaApp, R.drawable.ic_type_media, onCreateMediaApp),
        CreateActionItem(Strings.appTypeGallery, R.drawable.ic_type_gallery, onCreateGalleryApp)
    )
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        PremiumTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = { Text(Strings.search, style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .height(48.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val theme = LocalAppTheme.current
                        val gradientColors = theme.gradients.accent.ifEmpty { 
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        }
                        
                        val typewriterTexts = listOf(Strings.typewriterText1, Strings.typewriterText2, Strings.typewriterText3)
                        var textIndex by remember { mutableIntStateOf(0) }
                        var charIndex by remember { mutableIntStateOf(0) }
                        var userPaused by remember { mutableStateOf(false) }
                        var loopTick by remember { mutableIntStateOf(0) }
                        
                        val currentFullText = typewriterTexts[textIndex]
                        val displayText = currentFullText.substring(0, charIndex.coerceAtMost(currentFullText.length))
                        
                        var cursorVisible by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(530)
                                cursorVisible = !cursorVisible
                            }
                        }
                        
                        LaunchedEffect(loopTick) {
                            charIndex = 0
                            val fullText = typewriterTexts[textIndex]
                            for (i in 1..fullText.length) {
                                delay(100)
                                charIndex = i
                            }
                            
                            delay(2000)
                            
                            if (userPaused) {
                                delay(3000)
                                userPaused = false
                            }
                            
                            for (i in fullText.length - 1 downTo 0) {
                                delay(50)
                                charIndex = i
                            }
                            
                            delay(400)
                            
                            textIndex = (textIndex + 1) % typewriterTexts.size
                            loopTick++
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                userPaused = true
                            }
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.titleMedium.merge(
                                    TextStyle(
                                        brush = Brush.linearGradient(gradientColors)
                                    )
                                )
                            )
                            if (cursorVisible) {
                                Spacer(modifier = Modifier.width(1.dp))
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(AppColors.Warning)
                                )
                            }
                        }
                    }
                },
                actions = {
                    val context = LocalContext.current
                    val themeManager: ThemeManager = koinInject()
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
                    

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        },

        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { isVisible = true }
                
                val offsetY by animateFloatAsState(
                    targetValue = if (isVisible) 0f else 100f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "snackbarSlide"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "snackbarAlpha"
                )
                
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.graphicsLayer {
                        translationY = offsetY * density
                        this.alpha = alpha
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        onCreateApp = onCreateApp
                    )
                } else {
                    val listContext = LocalContext.current
                    val sharedExporter = remember { com.webtoapp.core.export.AppExporter(listContext) }
                    val sharedApkBuilder = remember { ApkBuilder(listContext) }
                    val sharedScope = rememberCoroutineScope()
                    
                    val healthRecordsState = healthMonitor?.allHealthRecords?.collectAsState(initial = emptyList<com.webtoapp.core.stats.AppHealthRecord>())
                    val healthRecords: List<com.webtoapp.core.stats.AppHealthRecord> = healthRecordsState?.value ?: emptyList()
                    val healthMap = remember(healthRecords) { healthRecords.associateBy { it.appId } }
                    val previewImageLoader = remember(listContext) {
                        ImageLoader.Builder(listContext.applicationContext)
                            .components {
                                add(VideoFrameDecoder.Factory())
                            }
                            .build()
                    }
                    val screenshotVersions = remember { mutableStateMapOf<Long, Int>() }
                    val screenshotLoadingStates = remember { mutableStateMapOf<Long, Boolean>() }
                    val previewSpecs by produceState(
                        initialValue = emptyMap<Long, AppPreviewSpec>(),
                        apps,
                        listContext
                    ) {
                        value = withContext(Dispatchers.IO) {
                            apps.associate { it.id to resolveAppPreviewSpec(listContext.applicationContext, it) }
                        }
                    }
                    LaunchedEffect(apps, screenshotService, previewSpecs) {
                        val initMessage = "init effect: service=${screenshotService != null}, apps=${apps.size}"
                        com.webtoapp.core.logging.AppLogger.i(
                            "ScreenshotFlow",
                            initMessage
                        )
                        android.util.Log.i("ScreenshotFlow", initMessage)
                        com.webtoapp.core.logging.AppLogger.d(
                            "HomeScreen",
                            "screenshot init start: service=${screenshotService != null}, apps=${apps.size}"
                        )
                        val svc = screenshotService ?: run {
                            com.webtoapp.core.logging.AppLogger.w("HomeScreen", "screenshot init skipped: service unavailable")
                            com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", "init skipped: service unavailable")
                            android.util.Log.i("ScreenshotFlow", "init skipped: service unavailable")
                            return@LaunchedEffect
                        }
                        val captureTargets = apps.mapNotNull { app ->
                            previewSpecs[app.id]?.captureUrl?.let { captureUrl ->
                                app to captureUrl
                            }
                        }
                        com.webtoapp.core.logging.AppLogger.d("HomeScreen", "screenshot init captureTargets=${captureTargets.size}")
                        for ((app, captureUrl) in captureTargets) {
                            if (!svc.hasScreenshot(app.id)) {
                                screenshotLoadingStates[app.id] = true
                                val initialMessage = "initial capture start: appId=${app.id}, name=${app.name}, target=$captureUrl"
                                com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", initialMessage)
                                android.util.Log.i("ScreenshotFlow", initialMessage)
                                try {
                                    com.webtoapp.core.logging.AppLogger.d(
                                        "HomeScreen",
                                        "capturing initial screenshot: appId=${app.id}, name=${app.name}, target=$captureUrl"
                                    )
                                    val result = svc.captureScreenshot(app.id, captureUrl)
                                    val resultMessage = "initial capture finished: appId=${app.id}, path=$result, exists=${svc.hasScreenshot(app.id)}"
                                    com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", resultMessage)
                                    android.util.Log.i("ScreenshotFlow", resultMessage)
                                    com.webtoapp.core.logging.AppLogger.d(
                                        "HomeScreen",
                                        "initial screenshot finished: appId=${app.id}, path=$result, exists=${svc.hasScreenshot(app.id)}"
                                    )
                                } catch (e: Exception) {
                                    val errorMessage = "initial capture exception: appId=${app.id}, error=${e.message}"
                                    com.webtoapp.core.logging.AppLogger.e("ScreenshotFlow", errorMessage, e)
                                    android.util.Log.e("ScreenshotFlow", errorMessage, e)
                                } finally {
                                    screenshotLoadingStates[app.id] = false
                                }
                            } else {
                                com.webtoapp.core.logging.AppLogger.d(
                                    "HomeScreen",
                                    "initial screenshot skipped (cached): appId=${app.id}, path=${svc.getScreenshotPath(app.id)}"
                                )
                            }
                            screenshotVersions[app.id] = (screenshotVersions[app.id] ?: 0) + 1
                        }
                    }
                    
                    LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(apps, key = { _, app -> app.id }) { index, app ->
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
                                val dismissProgress = dismissState.progress
                                val bgAlpha by animateFloatAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f,
                                    label = "dismissBgAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
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
                            onClick = { onPreviewApp(app) },
                            onEdit = { onEditApp(app) },
                            onEditCore = { onEditAppCore(app) },
                            onDelete = {
                                selectedApp = app
                                showDeleteDialog = true
                            },
                            onCreateShortcut = {
                                scope.launch {
                                    when (val result = exporter.createShortcut(app)) {
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
                                    when (val result = exporter.exportAsTemplate(app)) {
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
                                buildingApp = app
                                showBuildDialog = true
                            },
                            onShareApk = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(Strings.shareApkBuilding)
                                    val apkBuilder = sharedApkBuilder
                                    val result = apkBuilder.buildApk(app) { _, _ -> }
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
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, Strings.shareApkTitle.replace("%s", app.name))
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                listContext.startActivity(android.content.Intent.createChooser(shareIntent, Strings.shareApkTitle.replace("%s", app.name)))
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(Strings.shareApkFailed.replace("%s", e.message ?: "Unknown error"))
                                            }
                                        }
                                        is BuildResult.Error -> {
                                            snackbarHostState.showSnackbar(Strings.shareApkFailed.replace("%s", result.message))
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
                                    val resolvedService = screenshotService
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
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
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
                            shape = RoundedCornerShape(14.dp),
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
                        selectedApp?.let { viewModel.deleteApp(it) }
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
            onSave = { name, icon, color ->
                if (editingCategory != null) {
                    viewModel.updateCategory(
                        editingCategory!!.copy(name = name, icon = icon, color = color)
                    )
                } else {
                    viewModel.createCategory(name, icon, color)
                }
                showCategoryEditor = false
                editingCategory = null
            }
        )
    }
    if (showMoveToCategoryDialog && appToMove != null) {
        MoveToCategoryDialog(
            app = appToMove!!,
            categories = categories,
            onDismiss = {
                showMoveToCategoryDialog = false
                appToMove = null
            },
            onMoveToCategory = { categoryId ->
                viewModel.moveAppToCategory(appToMove!!, categoryId)
                showMoveToCategoryDialog = false
                appToMove = null
            }
        )
    }
    if (showBatchImportDialog) {
        BatchImportDialog(
            importService = batchImportService,
            onDismiss = { showBatchImportDialog = false },
            onImport = { entries ->
                batchImportService.importEntries(entries)
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
        Surface(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        Surface(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = iconPainter,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
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
        Spacer(modifier = Modifier.height(24.dp))
        PremiumButton(onClick = onCreateApp) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.createApp)
        }
    }
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
    var selectedEngineType by remember {
        mutableStateOf(webApp.apkExportConfig?.engineType ?: "SYSTEM_WEBVIEW")
    }
    val engineFileManager = remember { com.webtoapp.core.engine.download.EngineFileManager(context) }
    val isGeckoDownloaded = remember(selectedEngineType) {
        engineFileManager.isEngineDownloaded(com.webtoapp.core.engine.EngineType.GECKOVIEW)
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
                            .clip(RoundedCornerShape(12.dp))
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
                    var pulseAlpha by remember { mutableFloatStateOf(0.6f) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(1000)
                            pulseAlpha = if (pulseAlpha > 0.8f) 0.6f else 1f
                        }
                    }
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
                                    .background(catColor, RoundedCornerShape(2.dp))
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
                                .clip(RoundedCornerShape(2.dp)),
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
                        isBuilding = true
                        scope.launch {
                            val webAppWithConfig = webApp.copy(
                                apkExportConfig = (webApp.apkExportConfig ?: com.webtoapp.data.model.ApkExportConfig()).copy(
                                    encryptionConfig = encryptionConfig,
                                    hardeningConfig = hardeningConfig,
                                    isolationConfig = isolationConfig,
                                    backgroundRunEnabled = backgroundRunEnabled,
                                    backgroundRunConfig = backgroundRunConfig,
                                    engineType = selectedEngineType
                                )
                            )
                            val result = apkBuilder.buildApk(webAppWithConfig) { p, t ->
                                progress = p
                                progressText = t
                            }
                            when (result) {
                                is BuildResult.Success -> {
                                    analysisReport = result.analysisReport
                                    isBuilding = false
                                    apkBuilder.installApk(result.apkFile)
                                    if (result.analysisReport == null) {
                                        onResult("APK 构建成功，正在启动安装...")
                                    }
                                }
                                is BuildResult.Error -> {
                                    onResult("${Strings.buildFailed}: ${result.message}")
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
                .clip(MaterialTheme.shapes.small)
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
                .clip(MaterialTheme.shapes.small)
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
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                Strings.engineReady,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
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
