package com.webtoapp.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.QrCodeShareDialog
import kotlinx.coroutines.launch

/**
 * 扩展模块管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionModuleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,  // null 表示新建
    onNavigateToAiDeveloper: () -> Unit = {}  // AI 开发器入口
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    
    val modules by extensionManager.modules.collectAsState()
    val builtInModules by extensionManager.builtInModules.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    
    // File选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val result = extensionManager.importModule(stream)
                        result.onSuccess { module ->
                            Toast.makeText(context, "${Strings.msgImportSuccess}: ${module.name}", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(context, "${Strings.msgImportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "${Strings.msgImportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 二维码图片选择器
    val qrCodeImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val qrContent = QrCodeUtils.decodeQrCode(bitmap)
                            if (qrContent != null) {
                                extensionManager.importFromShareCode(qrContent).onSuccess { module ->
                                    Toast.makeText(context, "${Strings.msgImportSuccess}: ${module.name}", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "${Strings.msgImportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, Strings.qrCodeNotFound, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, Strings.imageLoadFailed, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "${Strings.msgImportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Filter模块 - 直接计算而非使用 remember，确保 StateFlow 更新时 UI 正确响应
    val filteredModules = (builtInModules + modules).filter { module ->
        val matchesCategory = selectedCategory == null || module.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
            module.name.contains(searchQuery, ignoreCase = true) ||
            module.description.contains(searchQuery, ignoreCase = true) ||
            module.tags.any { it.contains(searchQuery, ignoreCase = true) }
        matchesCategory && matchesSearch
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.extensionModule) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = Strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = Strings.btnImport)
                    }
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = Strings.add)
                    }
                }
            )
        },
        floatingActionButton = {
            var fabExpanded by remember { mutableStateOf(false) }
            
            Column(horizontalAlignment = Alignment.End) {
                // AI 开发按钮
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            onNavigateToAiDeveloper()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.aiDevelop)
                    }
                }
                
                // 手动创建按钮
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            onNavigateToEditor(null)
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.manualCreate)
                    }
                }
                
                // 主 FAB
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded }
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = Strings.createModule
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search栏 - MD3 SearchBar 风格
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(Strings.searchModules) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = Strings.clear)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )
            
            // 分类筛选器 - 使用 FilterChip
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(Strings.all) },
                        leadingIcon = if (selectedCategory == null) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
                items(ModuleCategory.values().toList()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text("${category.icon} ${category.getDisplayName()}") },
                        leadingIcon = if (selectedCategory == category) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
            
            // 统计信息 - MD3 简洁风格
            val stats = extensionManager.getStatistics()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Outlined.Extension,
                    value = stats.totalCount.toString(),
                    label = Strings.totalModulesLabel
                )
                StatItem(
                    icon = Icons.Outlined.Verified,
                    value = stats.builtInCount.toString(),
                    label = Strings.builtInLabel
                )
                StatItem(
                    icon = Icons.Outlined.Build,
                    value = stats.userCount.toString(),
                    label = Strings.customLabel
                )
            }
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Module列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredModules, key = { it.id }) { module ->
                    ModuleCard(
                        module = module,
                        extensionManager = extensionManager,
                        onEdit = { onNavigateToEditor(module.id) },
                        onDelete = {
                            scope.launch {
                                extensionManager.deleteModule(module.id)
                                Toast.makeText(context, Strings.deleted, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                
                // 美化的空状态界面
                if (filteredModules.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 装饰性图标
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            if (searchQuery.isNotBlank()) "🔍" else "📦",
                                            fontSize = 48.sp
                                        )
                                    }
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        if (searchQuery.isNotBlank()) Strings.noModulesFound else Strings.noModulesYet,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (searchQuery.isNotBlank()) 
                                            Strings.tryDifferentSearch 
                                        else 
                                            Strings.createModuleHint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                if (searchQuery.isBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // AI创建按钮
                                        FilledTonalButton(
                                            onClick = { onNavigateToAiDeveloper() },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(Strings.aiDevelop)
                                        }
                                        
                                        // 手动创建按钮
                                        Button(
                                            onClick = { onNavigateToEditor(null) }
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(Strings.createFirstModule)
                                        }
                                    }
                                } else {
                                    TextButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Strings.clearSearch)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
    
    // Import对话框
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.importModule) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportDialog = false
                                filePickerLauncher.launch("*/*")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Strings.importFromFile, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.selectWtamodFile,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportDialog = false
                                qrCodeImagePickerLauncher.launch("image/*")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Strings.importFromQrImage, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.selectQrImageHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
    
}


/**
 * 模块卡片组件 - MD3 原生风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    module: ExtensionModule,
    extensionManager: ExtensionManager,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showMenu by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    // SAF 文件创建器
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                extensionManager.exportModuleToUri(module.id, it).onSuccess {
                    Toast.makeText(context, Strings.exportSuccess, Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Storage权限请求器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission已授予，执行导出
            scope.launch {
                extensionManager.exportModuleToDownloads(module.id).onSuccess { path ->
                    Toast.makeText(context, "${Strings.exportSuccess}: $path", Toast.LENGTH_LONG).show()
                }.onFailure { e ->
                    Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "需要存储权限才能导出", Toast.LENGTH_SHORT).show()
        }
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部：图标、名称、徽章、菜单
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        module.icon,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    // Name行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Built-in标签
                        if (module.builtIn) {
                            AssistChip(
                                onClick = { },
                                label = { Text(Strings.builtIn) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 分类和版本
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${module.category.icon} ${module.category.getDisplayName()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "v${module.version.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                // 菜单按钮
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = Strings.more)
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.btnEdit) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.export) },
                            onClick = { showMenu = false; showExportDialog = true },
                            leadingIcon = { Icon(Icons.Outlined.FileUpload, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.share) },
                            onClick = { showMenu = false; showQrCodeDialog = true },
                            leadingIcon = { Icon(Icons.Outlined.Share, null) }
                        )
                        if (!module.builtIn) {
                            Divider()
                            DropdownMenuItem(
                                text = { Text(Strings.btnDelete, color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
            
            // Description
            if (module.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 标签
            if (module.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(module.tags.take(5)) { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
            
            // 底部信息
            val hasUrlMatches = module.urlMatches.isNotEmpty()
            val dangerousPermissions = module.permissions.filter { it.dangerous }
            
            if (hasUrlMatches || dangerousPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (hasUrlMatches) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Language,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                Strings.onlyEffectiveOnMatchingSites.format(module.urlMatches.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (dangerousPermissions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                Strings.requiresSensitivePermissions.format(
                                    dangerousPermissions.joinToString { it.displayName }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showQrCodeDialog) {
        QrCodeShareDialog(
            module = module,
            shareCode = module.toShareCode(),
            onDismiss = { showQrCodeDialog = false }
        )
    }
    
    // Export选项对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(Strings.exportModule) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Save到 Downloads
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                // Check存储权限
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    // Android 9 及以下需要权限
                                    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                        scope.launch {
                                            extensionManager.exportModuleToDownloads(module.id).onSuccess { path ->
                                                Toast.makeText(context, "${Strings.exportSuccess}\n$path", Toast.LENGTH_LONG).show()
                                            }.onFailure { e ->
                                                Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        permissionLauncher.launch(permission)
                                    }
                                } else {
                                    // Android 10+ 不需要权限
                                    scope.launch {
                                        extensionManager.exportModuleToDownloads(module.id).onSuccess { path ->
                                            Toast.makeText(context, "${Strings.exportSuccess}\n$path", Toast.LENGTH_LONG).show()
                                        }.onFailure { e ->
                                            Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Strings.exportToDownloads, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.exportToDownloadsHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Custom存储路径
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                // 打开 SAF 文件选择器
                                val fileName = extensionManager.getModuleExportFileName(module.id) ?: "module.wtamod"
                                createFileLauncher.launch(fileName)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Strings.exportToCustomPath, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.exportToCustomPathHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
}

/**
 * Statistics项 - MD3 简洁风格
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
