package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.R
import com.webtoapp.core.apkbuilder.ApkBuilder
import com.webtoapp.core.apkbuilder.BuildResult
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.InitializeLanguage
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AppCategory
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.components.CategoryEditorDialog
import com.webtoapp.ui.components.CategoryTabRow
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.LanguageSelectorButton
import com.webtoapp.ui.components.MoveToCategoryDialog
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.viewmodel.UiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    onEditApp: (WebApp) -> Unit,
    onEditAppCore: (WebApp) -> Unit = {},  // Class型专用编辑（核心配置）
    onPreviewApp: (WebApp) -> Unit,
    onOpenAppModifier: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
    onOpenHtmlCoding: () -> Unit = {},
    onOpenThemeSettings: () -> Unit = {},
    onOpenBrowserKernel: () -> Unit = {},
    onOpenHostsAdBlock: () -> Unit = {},
    onOpenExtensionModules: () -> Unit = {},
    onOpenLinuxEnvironment: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    // Initialize多语言
    InitializeLanguage()
    
    val apps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // 分类相关状态
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
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

    // 更多菜单状态
    var showMoreMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        // Search框 - 限制宽度
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = { Text(Strings.search, style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .height(48.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        Text(
                            "WebToApp",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
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
                    
                    // 更多菜单
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = Strings.more,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.menuAiHtmlCoding) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenHtmlCoding()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Code, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuThemeSettings) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenThemeSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuBrowserKernel) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenBrowserKernel()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.WebAsset, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuHostsAdBlock) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenHostsAdBlock()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Shield, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuAiSettings) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenAiSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuAppModifier) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenAppModifier()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AppShortcut, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuExtensionModules) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenExtensionModules()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.menuLinuxEnvironment) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenLinuxEnvironment()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Terminal, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(Strings.menuAbout) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenAbout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Info, null, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expand的菜单项
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Create前端项目应用
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateFrontendApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.Rocket, Strings.frontendProject)
                        }
                        // CreateHTML应用
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateHtmlApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.Code, Strings.createHtmlApp)
                        }
                        // Create媒体画廊应用（多图片/视频）
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateGalleryApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, Strings.createGalleryApp)
                        }
                        // Create媒体应用（单图片/视频）
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateMediaApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.Image, Strings.createMediaApp)
                        }
                        // Create网页应用
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.Language, Strings.createWebApp)
                        }
                    }
                }
                // 主 FAB
                ExtendedFloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    icon = { 
                        Icon(
                            if (showFabMenu) Icons.Default.Close else Icons.Default.Add, 
                            Strings.btnCreate
                        ) 
                    },
                    text = { Text(if (showFabMenu) Strings.close else Strings.createApp) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (apps.isEmpty()) {
                    // Empty状态
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        onCreateApp = onCreateApp
                    )
                } else {
                    // App列表
                    LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps, key = { it.id }) { app ->
                        val context = LocalContext.current
                        val exporter = remember { com.webtoapp.core.export.AppExporter(context) }
                        val scope = rememberCoroutineScope()

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
                                    val apkBuilder = ApkBuilder(context)
                                    val result = apkBuilder.buildApk(app) { _, _ -> }
                                    when (result) {
                                        is BuildResult.Success -> {
                                            // 使用 FileProvider 分享 APK
                                            try {
                                                val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    result.apkFile
                                                )
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "application/vnd.android.package-archive"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, apkUri)
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, Strings.shareApkTitle.replace("%s", app.name))
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, Strings.shareApkTitle.replace("%s", app.name)))
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
                            modifier = Modifier.animateItemPlacement()
                        )
                    }

                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
                }
            }
        }
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
        AlertDialog(
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
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
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
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
                        FeatureChip(icon = Icons.Outlined.Announcement, label = Strings.popupAnnouncement)
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
                    Divider()
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
                    Divider()
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
            Icons.Outlined.Language,
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
            Icons.Outlined.Code,
            Strings.appTypeHtml,
            MaterialTheme.colorScheme.secondaryContainer
        )
        com.webtoapp.data.model.AppType.GALLERY -> Triple(
            Icons.Outlined.PhotoLibrary,
            Strings.appTypeGallery,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        com.webtoapp.data.model.AppType.FRONTEND -> Triple(
            Icons.Outlined.Web,
            Strings.appTypeFrontend,
            MaterialTheme.colorScheme.primaryContainer
        )
    }
    
    val contentColor = when (appType) {
        com.webtoapp.data.model.AppType.WEB,
        com.webtoapp.data.model.AppType.FRONTEND -> MaterialTheme.colorScheme.onPrimaryContainer
        com.webtoapp.data.model.AppType.IMAGE,
        com.webtoapp.data.model.AppType.VIDEO,
        com.webtoapp.data.model.AppType.GALLERY -> MaterialTheme.colorScheme.onTertiaryContainer
        com.webtoapp.data.model.AppType.HTML -> MaterialTheme.colorScheme.onSecondaryContainer
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
            modifier = Modifier.size(80.dp),
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
        Button(onClick = onCreateApp) {
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
    var progress by remember { mutableStateOf(0) }
    var progressText by remember { mutableStateOf(Strings.preparing) }
    
    // Encryption配置状态
    var encryptionConfig by remember { 
        mutableStateOf(webApp.apkExportConfig?.encryptionConfig ?: com.webtoapp.data.model.ApkEncryptionConfig()) 
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

    AlertDialog(
        onDismissRequest = { if (!isBuilding) onDismiss() },
        title = { Text(Strings.buildDialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // App信息
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Android,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                
                Divider()
                
                // Encryption配置
                com.webtoapp.ui.components.EncryptionConfigCard(
                    config = encryptionConfig,
                    onConfigChange = { encryptionConfig = it }
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
                
                Divider()
                
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
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$progressText ($progress%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (!isBuilding) {
                Button(
                    onClick = {
                        isBuilding = true
                        scope.launch {
                            // 将加密配置、隔离配置和后台运行配置应用到 WebApp
                            val webAppWithConfig = webApp.copy(
                                apkExportConfig = (webApp.apkExportConfig ?: com.webtoapp.data.model.ApkExportConfig()).copy(
                                    encryptionConfig = encryptionConfig,
                                    isolationConfig = isolationConfig,
                                    backgroundRunEnabled = backgroundRunEnabled,
                                    backgroundRunConfig = backgroundRunConfig
                                )
                            )
                            val result = apkBuilder.buildApk(webAppWithConfig) { p, t ->
                                progress = p
                                progressText = t
                            }
                            when (result) {
                                is BuildResult.Success -> {
                                    // 直接安装
                                    apkBuilder.installApk(result.apkFile)
                                    onResult("APK 构建成功，正在启动安装...")
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
