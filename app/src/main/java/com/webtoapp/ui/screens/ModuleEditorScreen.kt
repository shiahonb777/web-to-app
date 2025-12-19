package com.webtoapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import com.webtoapp.core.extension.*
import kotlinx.coroutines.launch

/**
 * 模块编辑器页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleEditorScreen(
    moduleId: String?,  // null 表示新建
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    
    // 加载现有模块或创建新模块
    val existingModule = remember(moduleId) {
        moduleId?.let { id ->
            extensionManager.getAllModules().find { it.id == id }
        }
    }
    
    // 编辑状态
    var name by remember { mutableStateOf(existingModule?.name ?: "") }
    var description by remember { mutableStateOf(existingModule?.description ?: "") }
    var icon by remember { mutableStateOf(existingModule?.icon ?: "📦") }
    var category by remember { mutableStateOf(existingModule?.category ?: ModuleCategory.OTHER) }
    var tags by remember { mutableStateOf(existingModule?.tags?.joinToString(", ") ?: "") }
    var code by remember { mutableStateOf(existingModule?.code ?: "") }
    var cssCode by remember { mutableStateOf(existingModule?.cssCode ?: "") }
    var runAt by remember { mutableStateOf(existingModule?.runAt ?: ModuleRunTime.DOCUMENT_END) }
    var urlMatches by remember { mutableStateOf(existingModule?.urlMatches ?: emptyList()) }
    var permissions by remember { mutableStateOf(existingModule?.permissions?.toSet() ?: emptySet()) }
    var configItems by remember { mutableStateOf(existingModule?.configItems ?: emptyList()) }
    var versionName by remember { mutableStateOf(existingModule?.version?.name ?: "1.0.0") }
    var authorName by remember { mutableStateOf(existingModule?.author?.name ?: "") }
    
    var currentTab by remember { mutableIntStateOf(0) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showRunAtDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showUrlMatchDialog by remember { mutableStateOf(false) }
    var showConfigItemDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    
    val tabs = listOf("基本信息", "代码", "高级设置")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (moduleId == null) "创建模块" else "编辑模块") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    // 模板按钮（仅新建时显示）
                    if (moduleId == null) {
                        IconButton(onClick = { showTemplateDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "使用模板")
                        }
                    }
                    TextButton(
                        onClick = {
                            // 验证并保存
                            if (name.isBlank()) {
                                Toast.makeText(context, "请输入模块名称", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            if (code.isBlank() && cssCode.isBlank()) {
                                Toast.makeText(context, "请输入代码内容", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            
                            val module = ExtensionModule(
                                id = existingModule?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                icon = icon,
                                category = category,
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                version = ModuleVersion(
                                    code = (existingModule?.version?.code ?: 0) + 1,
                                    name = versionName
                                ),
                                author = if (authorName.isNotBlank()) ModuleAuthor(authorName) else null,
                                code = code,
                                cssCode = cssCode,
                                runAt = runAt,
                                urlMatches = urlMatches,
                                permissions = permissions.toList(),
                                configItems = configItems,
                                enabled = existingModule?.enabled ?: true,
                                builtIn = false,
                                createdAt = existingModule?.createdAt ?: System.currentTimeMillis()
                            )
                            
                            scope.launch {
                                extensionManager.addModule(module).onSuccess {
                                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }.onFailure { e ->
                                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab 栏
            TabRow(selectedTabIndex = currentTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (currentTab) {
                0 -> BasicInfoTab(
                    name = name,
                    onNameChange = { name = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    icon = icon,
                    onIconClick = { showIconPicker = true },
                    category = category,
                    onCategoryClick = { showCategoryDialog = true },
                    tags = tags,
                    onTagsChange = { tags = it },
                    versionName = versionName,
                    onVersionNameChange = { versionName = it },
                    authorName = authorName,
                    onAuthorNameChange = { authorName = it }
                )
                1 -> CodeTab(
                    code = code,
                    onCodeChange = { code = it },
                    cssCode = cssCode,
                    onCssCodeChange = { cssCode = it }
                )
                2 -> AdvancedTab(
                    runAt = runAt,
                    onRunAtClick = { showRunAtDialog = true },
                    permissions = permissions,
                    onPermissionsClick = { showPermissionsDialog = true },
                    urlMatches = urlMatches,
                    onUrlMatchesClick = { showUrlMatchDialog = true },
                    configItems = configItems,
                    onConfigItemsClick = { showConfigItemDialog = true }
                )
            }
        }
    }
    
    // 分类选择对话框
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("选择分类") },
            text = {
                LazyColumn {
                    items(ModuleCategory.values().toList()) { cat ->
                        ListItem(
                            headlineContent = { Text("${cat.icon} ${cat.displayName}") },
                            supportingContent = { Text(cat.description) },
                            modifier = Modifier.clickable {
                                category = cat
                                showCategoryDialog = false
                            },
                            trailingContent = {
                                if (category == cat) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // 执行时机选择对话框
    if (showRunAtDialog) {
        AlertDialog(
            onDismissRequest = { showRunAtDialog = false },
            title = { Text("执行时机") },
            text = {
                Column {
                    ModuleRunTime.values().forEach { time ->
                        ListItem(
                            headlineContent = { Text(time.displayName) },
                            supportingContent = { Text(time.description) },
                            modifier = Modifier.clickable {
                                runAt = time
                                showRunAtDialog = false
                            },
                            trailingContent = {
                                if (runAt == time) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // 权限选择对话框
    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = { Text("所需权限") },
            text = {
                LazyColumn {
                    items(ModulePermission.values().toList()) { perm ->
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(perm.displayName)
                                    if (perm.dangerous) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                "敏感",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            },
                            supportingContent = { Text(perm.description) },
                            trailingContent = {
                                Checkbox(
                                    checked = perm in permissions,
                                    onCheckedChange = {
                                        permissions = if (it) permissions + perm else permissions - perm
                                    }
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 图标选择对话框
    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = icon,
            onIconSelected = {
                icon = it
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
    
    // URL 匹配规则对话框
    if (showUrlMatchDialog) {
        UrlMatchDialog(
            urlMatches = urlMatches,
            onUrlMatchesChange = { urlMatches = it },
            onDismiss = { showUrlMatchDialog = false }
        )
    }
    
    // 配置项对话框
    if (showConfigItemDialog) {
        ConfigItemsDialog(
            configItems = configItems,
            onConfigItemsChange = { configItems = it },
            onDismiss = { showConfigItemDialog = false }
        )
    }
    
    // 模板选择对话框
    if (showTemplateDialog) {
         TemplateSelectionDialog(
            onTemplateSelected = { template ->
                // 应用模板
                name = template.name
                description = template.description
                icon = template.icon
                category = template.category
                code = template.code
                cssCode = template.cssCode
                configItems = template.configItems
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicInfoTab(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    icon: String,
    onIconClick: () -> Unit,
    category: ModuleCategory,
    onCategoryClick: () -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    versionName: String,
    onVersionNameChange: (String) -> Unit,
    authorName: String,
    onAuthorNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 图标和名称
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onIconClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 32.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("模块名称 *") },
                placeholder = { Text("输入模块名称") },
                singleLine = true
            )
        }
        
        // 描述
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("描述") },
            placeholder = { Text("简要描述模块功能") },
            minLines = 2,
            maxLines = 4
        )
        
        // 分类
        OutlinedCard(
            onClick = onCategoryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("分类") },
                supportingContent = { Text("${category.icon} ${category.displayName}") },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // 标签
        OutlinedTextField(
            value = tags,
            onValueChange = onTagsChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("标签") },
            placeholder = { Text("用逗号分隔，如：广告, 屏蔽, 工具") },
            singleLine = true
        )
        
        Divider()
        
        // 版本和作者
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = versionName,
                onValueChange = onVersionNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("版本") },
                placeholder = { Text("1.0.0") },
                singleLine = true
            )
            
            OutlinedTextField(
                value = authorName,
                onValueChange = onAuthorNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("作者") },
                placeholder = { Text("你的名字") },
                singleLine = true
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeTab(
    code: String,
    onCodeChange: (String) -> Unit,
    cssCode: String,
    onCssCodeChange: (String) -> Unit
) {
    var showJsTab by remember { mutableStateOf(true) }
    var showCodeSnippetSelector by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // JS/CSS 切换 + 代码块按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showJsTab,
                    onClick = { showJsTab = true },
                    label = { Text("JavaScript") },
                    leadingIcon = if (showJsTab) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = !showJsTab,
                    onClick = { showJsTab = false },
                    label = { Text("CSS") },
                    leadingIcon = if (!showJsTab) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
            
            // 代码块库按钮
            if (showJsTab) {
                FilledTonalButton(
                    onClick = { showCodeSnippetSelector = true },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("代码块")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 代码提示
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (showJsTab) "💡 可用函数" else "💡 CSS 提示",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (showJsTab) {
                        "• getConfig(key, defaultValue) - 获取用户配置\n" +
                        "• __MODULE_INFO__ - 模块信息对象\n" +
                        "• __MODULE_CONFIG__ - 配置值对象"
                    } else {
                        "• CSS 会自动注入到页面 <head>\n" +
                        "• 使用 !important 确保样式生效"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 代码编辑器
        OutlinedTextField(
            value = if (showJsTab) code else cssCode,
            onValueChange = { if (showJsTab) onCodeChange(it) else onCssCodeChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text(if (showJsTab) "JavaScript 代码" else "CSS 代码") },
            placeholder = {
                Text(
                    if (showJsTab) {
                        "// 在这里编写 JavaScript 代码\n" +
                        "console.log('Hello from module!');"
                    } else {
                        "/* 在这里编写 CSS 样式 */\n" +
                        ".ad-banner {\n" +
                        "    display: none !important;\n" +
                        "}"
                    }
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        )
    }
    
    // 代码块选择器对话框
    if (showCodeSnippetSelector) {
        com.webtoapp.ui.components.CodeSnippetSelectorDialog(
            onDismiss = { showCodeSnippetSelector = false },
            onSelect = { snippet ->
                // 插入代码到当前位置
                val newCode = if (code.isBlank()) {
                    snippet.code
                } else {
                    code + "\n\n" + snippet.code
                }
                onCodeChange(newCode)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedTab(
    runAt: ModuleRunTime,
    onRunAtClick: () -> Unit,
    permissions: Set<ModulePermission>,
    onPermissionsClick: () -> Unit,
    urlMatches: List<UrlMatchRule>,
    onUrlMatchesClick: () -> Unit,
    configItems: List<ModuleConfigItem>,
    onConfigItemsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 执行时机
        OutlinedCard(
            onClick = onRunAtClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("执行时机") },
                supportingContent = { Text(runAt.displayName) },
                leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // 权限
        OutlinedCard(
            onClick = onPermissionsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("所需权限") },
                supportingContent = {
                    Text(
                        if (permissions.isEmpty()) "无特殊权限"
                        else permissions.joinToString { it.displayName }
                    )
                },
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // URL 匹配
        OutlinedCard(
            onClick = onUrlMatchesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("URL 匹配规则") },
                supportingContent = {
                    Text(
                        if (urlMatches.isEmpty()) "匹配所有网站"
                        else "${urlMatches.size} 条规则"
                    )
                },
                leadingContent = { Icon(Icons.Default.Link, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // 配置项
        OutlinedCard(
            onClick = onConfigItemsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("用户配置项") },
                supportingContent = {
                    Text(
                        if (configItems.isEmpty()) "无可配置项"
                        else "${configItems.size} 个配置项"
                    )
                },
                leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // 帮助信息
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📚 开发指南",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• URL 匹配：留空则在所有网站生效\n" +
                    "• 配置项：让用户自定义模块行为\n" +
                    "• 权限声明：告知用户模块需要的能力\n" +
                    "• 执行时机：控制代码何时运行",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 图标选择对话框
 */
@Composable
fun IconPickerDialog(
    currentIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val icons = listOf(
        "📦", "🚫", "🎨", "⚡", "📊", "🖱️", "🎬", "🔒", "🛠️",
        "🌙", "📜", "📋", "🖼️", "⏩", "🛡️", "📖", "🔤", "🌐",
        "🎯", "💡", "🔧", "⚙️", "🎮", "🎵", "📱", "💻", "🌟",
        "🔥", "💎", "🎁", "🏆", "🎪", "🎭", "🎨", "🎬", "📸"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标") },
        text = {
            Column {
                icons.chunked(6).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { emoji ->
                            Surface(
                                onClick = { onIconSelected(emoji) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (emoji == currentIcon)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    emoji,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * URL 匹配规则对话框
 */
@Composable
fun UrlMatchDialog(
    urlMatches: List<UrlMatchRule>,
    onUrlMatchesChange: (List<UrlMatchRule>) -> Unit,
    onDismiss: () -> Unit
) {
    var newPattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var isExclude by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("URL 匹配规则") },
        text = {
            Column {
                // 现有规则
                if (urlMatches.isNotEmpty()) {
                    urlMatches.forEachIndexed { index, rule ->
                        ListItem(
                            headlineContent = { Text(rule.pattern) },
                            supportingContent = {
                                Text(
                                    buildString {
                                        if (rule.isRegex) append("正则 ")
                                        if (rule.exclude) append("排除")
                                        else append("包含")
                                    }
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    onUrlMatchesChange(urlMatches.filterIndexed { i, _ -> i != index })
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        )
                    }
                    Divider()
                }
                
                // 添加新规则
                OutlinedTextField(
                    value = newPattern,
                    onValueChange = { newPattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL 模式") },
                    placeholder = { Text("*.example.com/*") },
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                        Text("正则表达式")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isExclude, onCheckedChange = { isExclude = it })
                        Text("排除规则")
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        if (newPattern.isNotBlank()) {
                            onUrlMatchesChange(urlMatches + UrlMatchRule(newPattern, isRegex, isExclude))
                            newPattern = ""
                            isRegex = false
                            isExclude = false
                        }
                    },
                    enabled = newPattern.isNotBlank()
                ) {
                    Text("添加")
                }
                TextButton(onClick = onDismiss) {
                    Text("完成")
                }
            }
        }
    )
}

/**
 * 配置项管理对话框
 */
@Composable
fun ConfigItemsDialog(
    configItems: List<ModuleConfigItem>,
    onConfigItemsChange: (List<ModuleConfigItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("用户配置项") },
        text = {
            Column {
                if (configItems.isEmpty()) {
                    Text(
                        "暂无配置项\n添加配置项让用户可以自定义模块行为",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    configItems.forEachIndexed { index, item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text("${item.type.name} · ${item.key}") },
                            trailingContent = {
                                IconButton(onClick = {
                                    onConfigItemsChange(configItems.filterIndexed { i, _ -> i != index })
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加")
                }
                TextButton(onClick = onDismiss) {
                    Text("完成")
                }
            }
        }
    )
    
    if (showAddDialog) {
        AddConfigItemDialog(
            onAdd = { item ->
                onConfigItemsChange(configItems + item)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConfigItemDialog(
    onAdd: (ModuleConfigItem) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ConfigItemType.TEXT) }
    var defaultValue by remember { mutableStateOf("") }
    var required by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加配置项") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("键名 *") },
                    placeholder = { Text("如: fontSize") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("显示名称 *") },
                    placeholder = { Text("如: 字体大小") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("说明") },
                    placeholder = { Text("配置项的说明文字") },
                    singleLine = true
                )
                
                // 类型选择
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("类型") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ConfigItemType.values().forEach { itemType ->
                            DropdownMenuItem(
                                text = { Text(itemType.name) },
                                onClick = {
                                    type = itemType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = defaultValue,
                    onValueChange = { defaultValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("默认值") },
                    singleLine = true
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = required, onCheckedChange = { required = it })
                    Text("必填项")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(ModuleConfigItem(
                        key = key,
                        name = name,
                        description = description,
                        type = type,
                        defaultValue = defaultValue,
                        required = required
                    ))
                },
                enabled = key.isNotBlank() && name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}



/**
 * 模板选择对话框
 */
@Composable
fun TemplateSelectionDialog(
    onTemplateSelected: (ModuleTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val templates = remember { ModuleTemplates.getAll() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模板") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    Surface(
                        onClick = { onTemplateSelected(template) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(template.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    template.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    template.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
