package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.apkbuilder.ApkBuilder
import com.webtoapp.core.apkbuilder.BuildResult
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.viewmodel.UiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主页 - 应用列表
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onCreateApp: () -> Unit,
    onCreateMediaApp: () -> Unit = {},
    onCreateHtmlApp: () -> Unit = {},
    onEditApp: (WebApp) -> Unit,
    onPreviewApp: (WebApp) -> Unit,
    onOpenAppModifier: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
    onOpenHtmlCoding: () -> Unit = {},
    onOpenThemeSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val apps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

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
                        // 搜索框 - 限制宽度
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = { Text("搜索...", style = MaterialTheme.typography.bodyMedium) },
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
                    // 搜索按钮
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.search("")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索",
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
                                contentDescription = "更多",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("AI HTML编程") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenHtmlCoding()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Code, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("主题设置") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenThemeSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("AI 设置") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenAiSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("应用修改器") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenAppModifier()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AppShortcut, null, modifier = Modifier.size(20.dp))
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("关于") },
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
                // 展开的菜单项
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 创建HTML应用
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateHtmlApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.Code, "HTML应用")
                        }
                        // 创建媒体应用
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateMediaApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.Image, "媒体应用")
                        }
                        // 创建网页应用
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onCreateApp()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Outlined.Language, "网页应用")
                        }
                    }
                }
                // 主 FAB
                ExtendedFloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    icon = { 
                        Icon(
                            if (showFabMenu) Icons.Default.Close else Icons.Default.Add, 
                            "创建"
                        ) 
                    },
                    text = { Text(if (showFabMenu) "关闭" else "创建应用") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (apps.isEmpty()) {
                // 空状态
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    onCreateApp = onCreateApp
                )
            } else {
                // 应用列表
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
                            onDelete = {
                                selectedApp = app
                                showDeleteDialog = true
                            },
                            onCreateShortcut = {
                                scope.launch {
                                    when (val result = exporter.createShortcut(app)) {
                                        is com.webtoapp.core.export.ShortcutResult.Success -> {
                                            snackbarHostState.showSnackbar("快捷方式创建成功")
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
                                            snackbarHostState.showSnackbar("项目已导出到: ${result.path}")
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

    // 构建 APK 对话框
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

    // 删除确认对话框
    if (showDeleteDialog && selectedApp != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedApp = null
            },
            title = { Text("删除应用") },
            text = { Text("确定要删除「${selectedApp?.name}」吗？此操作无法撤销。") },
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
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedApp = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 应用卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    app: WebApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateShortcut: () -> Unit = {},
    onExport: () -> Unit = {},
    onBuildApk: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    expanded = true
                }
            ),
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
            // 图标
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

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 功能标签
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (app.activationEnabled) {
                        FeatureChip(icon = Icons.Outlined.Key, label = "激活码")
                    }
                    if (app.adBlockEnabled) {
                        FeatureChip(icon = Icons.Outlined.Block, label = "拦截")
                    }
                    if (app.announcementEnabled) {
                        FeatureChip(icon = Icons.Outlined.Announcement, label = "公告")
                    }
                }
            }

            // 菜单
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            expanded = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("创建快捷方式") },
                        onClick = {
                            expanded = false
                            onCreateShortcut()
                        },
                        leadingIcon = { Icon(Icons.Outlined.AppShortcut, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("构建 APK") },
                        onClick = {
                            expanded = false
                            onBuildApk()
                        },
                        leadingIcon = { Icon(Icons.Outlined.InstallMobile, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("导出项目") },
                        onClick = {
                            expanded = false
                            onExport()
                        },
                        leadingIcon = { Icon(Icons.Outlined.FileDownload, null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("删除") },
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
            text = "暂无应用",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮创建您的第一个应用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateApp) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建应用")
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
    var progressText by remember { mutableStateOf("准备中...") }

    AlertDialog(
        onDismissRequest = { if (!isBuilding) onDismiss() },
        title = { Text("构建 APK") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 应用信息
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
                            webApp.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Divider()
                
                Text(
                    "将为「${webApp.name}」构建独立的 APK 安装包。",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "构建完成后可直接安装到设备上，无需创建快捷方式。",
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
                            val result = apkBuilder.buildApk(webApp) { p, t ->
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
                                    onResult("构建失败: ${result.message}")
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Build, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("开始构建")
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        dismissButton = {
            if (!isBuilding) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
