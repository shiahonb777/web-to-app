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
import com.webtoapp.core.i18n.InitializeLanguage
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

/**
 * Home screen - 应用列表
 */
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
    // Initialize多语言
    InitializeLanguage()
    
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 分类相关状态
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

    // Scope 和 Snackbar
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
                        // Search框 - 限制宽度
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
                        
                        // 光标闪烁
                        var cursorVisible by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(530)
                                cursorVisible = !cursorVisible
                            }
                        }
                        
                        // 打字机主循环
                        LaunchedEffect(loopTick) {
                            // 逐字打出 (100ms/字)
                            charIndex = 0
                            val fullText = typewriterTexts[textIndex]
                            for (i in 1..fullText.length) {
                                delay(100)
                                charIndex = i
                            }
                            
                            // 打完停留 2 秒
                            delay(2000)
                            
                            // 如果用户点击了暂停，等待 3 秒
                            if (userPaused) {
                                delay(3000)
                                userPaused = false
                            }
                            
                            // 逐字删除 (50ms/字)
                            for (i in fullText.length - 1 downTo 0) {
                                delay(50)
                                charIndex = i
                            }
                            
                            // 删除完停留 0.4 秒
                            delay(400)
                            
                            // 切换到下一段
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
                            // 橙色闪烁光标
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
                    // 深/浅色模式切换按钮（带圆形揭示动画）
                    val context = LocalContext.current
                    val themeManager = remember { ThemeManager.getInstance(context) }
                    val darkModeState by themeManager.darkModeFlow.collectAsStateWithLifecycle()
                    val isDarkNow = darkModeState == ThemeManager.DarkModeSettings.DARK
                    
                    // 获取圆形揭示动画状态
                    val revealState = LocalThemeRevealState.current
                    val view = LocalView.current
                    val activity = context as? android.app.Activity
                    
                    // 记录按钮在屏幕上的中心坐标
                    var buttonCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    
                    IconButton(
                        onClick = {
                            val switchToDark = !isDarkNow
                            
                            if (revealState != null) {
                                // ★ 可打断：不再检查 isAnimating
                                // 如果动画进行中，triggerReveal 会自动取消旧动画、重新截图、重新开始
                                revealState.triggerReveal(
                                    center = buttonCenter,
                                    switchToDark = switchToDark,
                                    view = view,
                                    window = activity?.window
                                ) {
                                    // 截图完成后切换主题
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
                                // Fallback: 无动画直接切换
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
                    
                    // 语言选择按钮
                    LanguageSelectorButton(
                        onLanguageChanged = {
                            // 语言更改后会自动触发重组
                            scope.launch {
                                snackbarHostState.showSnackbar(Strings.msgLanguageChanged)
                            }
                        }
                    )
                    
                    // Search按钮
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
                // 弹簧滑入动画
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
            // 分类标签栏
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
                    // Empty状态
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        onCreateApp = onCreateApp
                    )
                } else {
                    // App列表
                    val listContext = LocalContext.current
                    val sharedExporter = remember { com.webtoapp.core.export.AppExporter(listContext) }
                    val sharedApkBuilder = remember { ApkBuilder(listContext) }
                    val sharedScope = rememberCoroutineScope()
                    
                    // 健康状态数据
                    val healthMonitor: com.webtoapp.core.stats.AppHealthMonitor? = remember { 
                        try { org.koin.java.KoinJavaComponent.get(com.webtoapp.core.stats.AppHealthMonitor::class.java) } 
                        catch (e: Exception) { null }
                    }
                    val healthRecordsState = healthMonitor?.allHealthRecords?.collectAsState(initial = emptyList<com.webtoapp.core.stats.AppHealthRecord>())
                    val healthRecords: List<com.webtoapp.core.stats.AppHealthRecord> = healthRecordsState?.value ?: emptyList()
                    val healthMap = remember(healthRecords) { healthRecords.associateBy { it.appId } }
                    
                    // 截图服务
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
                        ImageLoader.Builder(listContext.applicationContext)
                            .components {
                                add(VideoFrameDecoder.Factory())
                            }
                            .build()
                    }
                    
                    // 截图版本跟踪（用于强制 Coil 重新加载图片）
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
                    
                    // 仅首次加载：串行为没有缓存截图的应用截一次，之后由用户手动点击刷新
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

                        // 交错入场动画
                        StaggeredAnimatedItem(index = index) {
                        
                        // 滑动删除
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    selectedApp = app
                                    showDeleteDialog = true
                                    false  // 不真正移除，让对话框确认
                                } else false
                            }
                        )
                        
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                // 删除背景
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
                            onLongClick = { selectedApp = app },
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
                                            // 使用 FileProvider 分享 APK
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
                        } // SwipeToDismissBox
                        } // StaggeredAnimatedItem
                    }

                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                }
            }

                    // ========== Create App Bottom Bar ==========
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Expanded create menu
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

                        // Main create button
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
            } // Column
    }

    // Build APK 对话框
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

    // Delete确认对话框
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
    
    // 分类编辑对话框
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
    
    // 移动到分类对话框
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
    
    // 批量导入对话框
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
/**
 * 侧边栏菜单项
 */
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

/**
 * 应用卡片
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppCard(
    app: WebApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onEditCore: () -> Unit = {},  // Class型专用编辑（核心配置）
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
            // Icon + 健康状态指示灯
            Box {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
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
            // 健康状态指示灯（仅 WEB 类型且有状态时显示）
            if (healthStatus != null && healthStatus != com.webtoapp.core.stats.HealthStatus.UNKNOWN) {
                val dotColor = when (healthStatus) {
                    com.webtoapp.core.stats.HealthStatus.ONLINE -> AppColors.Success
                    com.webtoapp.core.stats.HealthStatus.SLOW -> androidx.compose.ui.graphics.Color(0xFFFFC107)
                    com.webtoapp.core.stats.HealthStatus.OFFLINE -> androidx.compose.ui.graphics.Color(0xFFF44336)
                    else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
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
            } // Box

            Spacer(modifier = Modifier.width(16.dp))

            // Info
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
                        com.webtoapp.data.model.AppType.IMAGE -> {
                            app.mediaConfig?.mediaPath?.let { java.io.File(it).name } ?: app.url
                        }
                        com.webtoapp.data.model.AppType.VIDEO -> {
                            app.mediaConfig?.mediaPath?.let { java.io.File(it).name } ?: app.url
                        }
                        com.webtoapp.data.model.AppType.HTML -> {
                            app.htmlConfig?.entryFile?.takeIf { it.isNotBlank() } ?: "index.html"
                        }
                        com.webtoapp.data.model.AppType.FRONTEND -> {
                            // 显示入口文件或项目目录
                            app.htmlConfig?.entryFile?.takeIf { it.isNotBlank() }
                                ?: app.htmlConfig?.projectDir?.let { java.io.File(it).name }
                                ?: "index.html"
                        }
                        com.webtoapp.data.model.AppType.GALLERY -> {
                            // 显示媒体数量统计
                            val config = app.galleryConfig
                            if (config != null && config.items.isNotEmpty()) {
                                val imageCount = config.items.count { it.type == com.webtoapp.data.model.GalleryItemType.IMAGE }
                                val videoCount = config.items.count { it.type == com.webtoapp.data.model.GalleryItemType.VIDEO }
                                buildString {
                                    if (imageCount > 0) append("$imageCount ${Strings.galleryImages}")
                                    if (imageCount > 0 && videoCount > 0) append(", ")
                                    if (videoCount > 0) append("$videoCount ${Strings.galleryVideos}")
                                }
                            } else {
                                Strings.galleryEmpty
                            }
                        }
                        else -> app.url
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 功能标签
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // App类型标签
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

            // 网页快照缩略图（仅 WEB 类型显示，无截图时显示占位符）
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .width(30.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onCaptureScreenshot != null) {
                        val clickMessage = "thumbnail tapped: appId=${app.id}, hasHandler=${onCaptureScreenshot != null}, hasPath=${screenshotPath != null}, version=$screenshotVersion, loading=$isScreenshotLoading"
                        com.webtoapp.core.logging.AppLogger.i("ScreenshotFlow", clickMessage)
                        android.util.Log.i("ScreenshotFlow", clickMessage)
                        onCaptureScreenshot?.invoke()
                    },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                if (screenshotPath != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(java.io.File(screenshotPath))
                                .setParameter("v", screenshotVersion)
                                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .crossfade(true)
                                .build(),
                            imageLoader = previewImageLoader,
                            contentDescription = Strings.btnPreview,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
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

            // 菜单
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = Strings.more)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Web page应用的"Edit"已包含所有配置，不需要单独的"编辑核心配置"
                    if (app.appType == com.webtoapp.data.model.AppType.WEB) {
                        // Web page应用：只显示一个"Edit"按钮
                        DropdownMenuItem(
                            text = { Text(Strings.btnEdit) },
                            onClick = {
                                expanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                    } else {
                        // 其他类型：显示"编辑核心配置"和"编辑通用配置"
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

/**
 * 功能标签
 */
@Composable
fun FeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * 应用类型标签
 */
@Composable
fun AppTypeChip(appType: com.webtoapp.data.model.AppType) {
    val (icon, label, containerColor) = when (appType) {
        com.webtoapp.data.model.AppType.WEB -> Triple(
            Icons.Outlined.Public,
            Strings.appTypeWeb,
            MaterialTheme.colorScheme.primaryContainer
        )
        com.webtoapp.data.model.AppType.IMAGE -> Triple(
            Icons.Outlined.Image,
            Strings.appTypeImage,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        com.webtoapp.data.model.AppType.VIDEO -> Triple(
            Icons.Outlined.VideoLibrary,
            Strings.appTypeVideo,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        com.webtoapp.data.model.AppType.HTML -> Triple(
            Icons.Outlined.Html,
            Strings.appTypeHtml,
            MaterialTheme.colorScheme.secondaryContainer
        )
        com.webtoapp.data.model.AppType.GALLERY -> Triple(
            Icons.Outlined.PhotoLibrary,
            Strings.appTypeGallery,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        com.webtoapp.data.model.AppType.FRONTEND -> Triple(
            Icons.Outlined.Rocket,
            Strings.appTypeFrontend,
            MaterialTheme.colorScheme.primaryContainer
        )
        com.webtoapp.data.model.AppType.WORDPRESS -> Triple(
            Icons.Outlined.Newspaper,
            Strings.appTypeWordPress,
            MaterialTheme.colorScheme.primaryContainer
        )
        com.webtoapp.data.model.AppType.NODEJS_APP -> Triple(
            Icons.Outlined.Terminal,
            Strings.appTypeNodeJs,
            MaterialTheme.colorScheme.secondaryContainer
        )
        com.webtoapp.data.model.AppType.PHP_APP -> Triple(
            Icons.Outlined.DataObject,
            Strings.appTypePhp,
            MaterialTheme.colorScheme.secondaryContainer
        )
        com.webtoapp.data.model.AppType.PYTHON_APP -> Triple(
            Icons.Outlined.Psychology,
            Strings.appTypePython,
            MaterialTheme.colorScheme.secondaryContainer
        )
        com.webtoapp.data.model.AppType.GO_APP -> Triple(
            Icons.Outlined.Speed,
            Strings.appTypeGo,
            MaterialTheme.colorScheme.primaryContainer
        )
        com.webtoapp.data.model.AppType.MULTI_WEB -> Triple(
            Icons.Outlined.Language,
            Strings.appTypeMultiWeb,
            MaterialTheme.colorScheme.primaryContainer
        )
    }
    
    val contentColor = when (appType) {
        com.webtoapp.data.model.AppType.WEB,
        com.webtoapp.data.model.AppType.FRONTEND,
        com.webtoapp.data.model.AppType.WORDPRESS -> MaterialTheme.colorScheme.onPrimaryContainer
        com.webtoapp.data.model.AppType.IMAGE,
        com.webtoapp.data.model.AppType.VIDEO,
        com.webtoapp.data.model.AppType.GALLERY -> MaterialTheme.colorScheme.onTertiaryContainer
        com.webtoapp.data.model.AppType.HTML,
        com.webtoapp.data.model.AppType.NODEJS_APP,
        com.webtoapp.data.model.AppType.PHP_APP,
        com.webtoapp.data.model.AppType.PYTHON_APP -> MaterialTheme.colorScheme.onSecondaryContainer
        com.webtoapp.data.model.AppType.GO_APP,
        com.webtoapp.data.model.AppType.MULTI_WEB -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

/**
 * 空状态
 */
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

/**
 * 构建 APK 对话框
 */
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
    
    // Encryption配置状态
    var encryptionConfig by remember { 
        mutableStateOf(webApp.apkExportConfig?.encryptionConfig ?: com.webtoapp.data.model.ApkEncryptionConfig()) 
    }
    
    // 软件加固配置状态
    var hardeningConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.hardeningConfig ?: com.webtoapp.data.model.AppHardeningConfig())
    }
    
    // 独立环境/多开配置状态
    var isolationConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.isolationConfig ?: com.webtoapp.core.isolation.IsolationConfig())
    }
    
    // 后台运行配置状态
    var backgroundRunEnabled by remember {
        mutableStateOf(webApp.apkExportConfig?.backgroundRunEnabled ?: false)
    }
    var backgroundRunConfig by remember {
        mutableStateOf(webApp.apkExportConfig?.backgroundRunConfig ?: com.webtoapp.data.model.BackgroundRunExportConfig())
    }
    
    // 浏览器引擎配置状态
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
                // App信息
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
                
                // Encryption配置
                com.webtoapp.ui.components.EncryptionConfigCard(
                    config = encryptionConfig,
                    onConfigChange = { encryptionConfig = it }
                )
                
                // 软件加固配置
                com.webtoapp.ui.components.HardeningConfigCard(
                    config = hardeningConfig,
                    onConfigChange = { hardeningConfig = it }
                )
                
                // 独立环境/多开配置
                com.webtoapp.ui.components.IsolationConfigCard(
                    config = isolationConfig,
                    onConfigChange = { isolationConfig = it }
                )
                
                // 后台运行配置
                com.webtoapp.ui.components.BackgroundRunConfigCard(
                    enabled = backgroundRunEnabled,
                    config = backgroundRunConfig,
                    onEnabledChange = { backgroundRunEnabled = it },
                    onConfigChange = { backgroundRunConfig = it }
                )
                
                // 浏览器引擎选择
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

                // 进度
                if (isBuilding) {
                    Spacer(Modifier.height(12.dp))
                    
                    // 动画进度环
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress / 100f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "buildProgress"
                    )
                    
                    // 脉冲发光
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
                        // 圆形进度
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
                
                // APK 分析报告
                analysisReport?.let { report ->
                    HorizontalDivider()
                    
                    // 标题 + 总大小
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
                    
                    // 分类体积条
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
                        // Progress bar
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
                    
                    // 优化建议
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
                            // 将加密配置、隔离配置和后台运行配置应用到 WebApp
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
                                    // 直接安装
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

/**
 * 浏览器引擎选择卡片（用于构建对话框）
 */
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
        
        // System WebView
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
        
        // GeckoView
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
