package com.webtoapp.ui.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.webtoapp.core.appmodifier.*
import kotlinx.coroutines.launch

/**
 * 应用修改器页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModifierScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val appListProvider = remember { AppListProvider(context) }
    val appCloner = remember { AppCloner(context) }
    
    // 状态
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(AppFilterType.USER) }
    var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var showModifyDialog by remember { mutableStateOf(false) }
    
    // 加载应用列表
    LaunchedEffect(filterType, searchQuery) {
        isLoading = true
        apps = appListProvider.getInstalledApps(filterType, searchQuery)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用图标修改") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索应用...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 筛选标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == AppFilterType.USER,
                    onClick = { filterType = AppFilterType.USER },
                    label = { Text("用户应用") },
                    leadingIcon = if (filterType == AppFilterType.USER) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == AppFilterType.SYSTEM,
                    onClick = { filterType = AppFilterType.SYSTEM },
                    label = { Text("系统应用") },
                    leadingIcon = if (filterType == AppFilterType.SYSTEM) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == AppFilterType.ALL,
                    onClick = { filterType = AppFilterType.ALL },
                    label = { Text("全部") },
                    leadingIcon = if (filterType == AppFilterType.ALL) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 应用数量
            Text(
                text = "共 ${apps.size} 个应用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 应用列表
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            onClick = {
                                selectedApp = app
                                showModifyDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 修改对话框
    if (showModifyDialog && selectedApp != null) {
        AppModifyDialog(
            app = selectedApp!!,
            appCloner = appCloner,
            onDismiss = { 
                showModifyDialog = false
                selectedApp = null
            },
            onResult = { result ->
                showModifyDialog = false
                selectedApp = null
                scope.launch {
                    when (result) {
                        is AppModifyResult.ShortcutSuccess -> {
                            snackbarHostState.showSnackbar("快捷方式创建成功")
                        }
                        is AppModifyResult.CloneSuccess -> {
                            snackbarHostState.showSnackbar("克隆成功，请确认安装")
                        }
                        is AppModifyResult.Error -> {
                            snackbarHostState.showSnackbar("失败: ${result.message}")
                        }
                    }
                }
            }
        )
    }
}

/**
 * 应用列表项
 */
@Composable
fun AppListItem(
    app: InstalledAppInfo,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            app.icon?.let { drawable ->
                Image(
                    bitmap = drawable.toBitmap(56, 56).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            } ?: Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Android,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "v${app.versionName} · ${app.formattedSize}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 应用修改对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModifyDialog(
    app: InstalledAppInfo,
    appCloner: AppCloner,
    onDismiss: () -> Unit,
    onResult: (AppModifyResult) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var newAppName by remember { mutableStateOf(app.appName) }
    var newIconPath by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var progressText by remember { mutableStateOf("") }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { newIconPath = it.toString() }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("修改应用") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 原应用信息
                Row(verticalAlignment = Alignment.CenterVertically) {
                    app.icon?.let { drawable ->
                        Image(
                            bitmap = drawable.toBitmap(48, 48).asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(app.appName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Divider()
                
                // 新图标选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.medium
                            )
                            .clickable { imagePickerLauncher.launch("image/*") },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (newIconPath != null) {
                            AsyncImage(
                                model = newIconPath,
                                contentDescription = "新图标",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    "选择图标",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text("新图标", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "点击选择新图标（可选）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (newIconPath != null) {
                            TextButton(
                                onClick = { newIconPath = null },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("使用原图标", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                // 新名称
                OutlinedTextField(
                    value = newAppName,
                    onValueChange = { newAppName = it },
                    label = { Text("新应用名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 进度
                if (isProcessing) {
                    Column {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isProcessing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (newIconPath == null) {
                        // 克隆安装按钮
                        OutlinedButton(
                            onClick = {
                                isProcessing = true
                                scope.launch {
                                    val config = AppModifyConfig(
                                        originalApp = app,
                                        newAppName = newAppName,
                                        newIconPath = newIconPath
                                    )
                                    val result = appCloner.cloneAndInstall(config) { p, t ->
                                        progress = p
                                        progressText = t
                                    }
                                    onResult(result)
                                }
                            },
                            enabled = newAppName.isNotBlank()
                        ) {
                            Icon(Icons.Outlined.InstallMobile, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("克隆安装")
                        }
                    }

                    // 创建快捷方式按钮
                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                val config = AppModifyConfig(
                                    originalApp = app,
                                    newAppName = newAppName,
                                    newIconPath = newIconPath
                                )
                                val result = appCloner.createModifiedShortcut(config) { p, t ->
                                    progress = p
                                    progressText = t
                                }
                                onResult(result)
                            }
                        },
                        enabled = newAppName.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.AppShortcut, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("快捷方式")
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
