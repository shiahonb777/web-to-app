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
    onEditApp: (WebApp) -> Unit,
    onPreviewApp: (WebApp) -> Unit
) {
    val apps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<WebApp?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Snackbar
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = { Text("搜索应用...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        Text("WebToApp")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.search("")
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateApp,
                icon = { Icon(Icons.Default.Add, "创建") },
                text = { Text("创建应用") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
