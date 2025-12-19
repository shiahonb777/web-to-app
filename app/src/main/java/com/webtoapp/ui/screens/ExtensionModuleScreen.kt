package com.webtoapp.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.extension.*
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
    var showShareCodeDialog by remember { mutableStateOf(false) }
    var shareCodeInput by remember { mutableStateOf("") }
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val result = extensionManager.importModule(stream)
                        result.onSuccess { module ->
                            Toast.makeText(context, "导入成功: ${module.name}", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 过滤模块
    val filteredModules = remember(modules, builtInModules, selectedCategory, searchQuery) {
        val allModules = builtInModules + modules
        allModules.filter { module ->
            val matchesCategory = selectedCategory == null || module.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                module.name.contains(searchQuery, ignoreCase = true) ||
                module.description.contains(searchQuery, ignoreCase = true) ||
                module.tags.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扩展模块") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "导入")
                    }
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "新建")
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
                        Text("AI 开发")
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
                        Text("手动创建")
                    }
                }
                
                // 主 FAB
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded }
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "创建模块"
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
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索模块...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // 分类筛选
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("全部") },
                        leadingIcon = if (selectedCategory == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
                items(ModuleCategory.values().toList()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text("${category.icon} ${category.displayName}") },
                        leadingIcon = if (selectedCategory == category) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
            
            // 统计信息
            val stats = remember(modules, builtInModules) { extensionManager.getStatistics() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "共 ${stats.totalCount} 个模块",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "已启用 ${stats.enabledCount} 个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 模块列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredModules, key = { it.id }) { module ->
                    ModuleCard(
                        module = module,
                        onToggle = {
                            scope.launch {
                                extensionManager.toggleModule(module.id)
                            }
                        },
                        onEdit = { onNavigateToEditor(module.id) },
                        onShare = {
                            extensionManager.shareModule(module.id)?.let { intent ->
                                context.startActivity(Intent.createChooser(intent, "分享模块"))
                            }
                        },
                        onDelete = {
                            scope.launch {
                                extensionManager.deleteModule(module.id)
                                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDuplicate = {
                            scope.launch {
                                extensionManager.duplicateModule(module.id).onSuccess {
                                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                
                if (filteredModules.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📦", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isNotBlank()) "没有找到匹配的模块" else "暂无模块",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { onNavigateToEditor(null) }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("创建第一个模块")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 导入对话框
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入模块") },
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
                                Text("从文件导入", fontWeight = FontWeight.Medium)
                                Text(
                                    "选择 .wtamod 或 .wtapkg 文件",
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
                                showShareCodeDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("从分享码导入", fontWeight = FontWeight.Medium)
                                Text(
                                    "粘贴他人分享的模块代码",
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
                    Text("取消")
                }
            }
        )
    }
    
    // 分享码导入对话框
    if (showShareCodeDialog) {
        val clipboardManager = LocalClipboardManager.current
        
        AlertDialog(
            onDismissRequest = { showShareCodeDialog = false },
            title = { Text("从分享码导入") },
            text = {
                Column {
                    OutlinedTextField(
                        value = shareCodeInput,
                        onValueChange = { shareCodeInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分享码") },
                        placeholder = { Text("粘贴分享码...") },
                        minLines = 3,
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let {
                                shareCodeInput = it
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("从剪贴板粘贴")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            extensionManager.importFromShareCode(shareCodeInput).onSuccess { module ->
                                Toast.makeText(context, "导入成功: ${module.name}", Toast.LENGTH_SHORT).show()
                                showShareCodeDialog = false
                                shareCodeInput = ""
                            }.onFailure { e ->
                                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = shareCodeInput.isNotBlank()
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareCodeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}


/**
 * 模块卡片组件
 */
@Composable
fun ModuleCard(
    module: ExtensionModule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(module.icon, fontSize = 24.sp)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                module.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (module.builtIn) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        "内置",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            "v${module.version.name} · ${module.category.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 开关和菜单
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = module.enabled,
                        onCheckedChange = { onToggle() }
                    )
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("复制") },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("分享") },
                                onClick = {
                                    showMenu = false
                                    onShare()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("复制分享码") },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(AnnotatedString(module.toShareCode()))
                                    Toast.makeText(context, "分享码已复制", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                            )
                            if (!module.builtIn) {
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // 描述
            if (module.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(module.tags.take(5)) { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "#$tag",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // URL 匹配规则提示
            if (module.urlMatches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "仅在 ${module.urlMatches.size} 个匹配规则的网站生效",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 权限提示
            val dangerousPermissions = module.permissions.filter { it.dangerous }
            if (dangerousPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "需要敏感权限: ${dangerousPermissions.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
