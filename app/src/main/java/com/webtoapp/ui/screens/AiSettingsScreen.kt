package com.webtoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.data.model.*
import kotlinx.coroutines.launch

/**
 * AI 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configManager = remember { AiConfigManager(context) }
    val apiClient = remember { AiApiClient(context) }
    
    // 状态
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    
    var showAddApiKeyDialog by remember { mutableStateOf(false) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    var selectedApiKey by remember { mutableStateOf<ApiKeyConfig?>(null) }
    var editingApiKey by remember { mutableStateOf<ApiKeyConfig?>(null) }
    var editingModel by remember { mutableStateOf<SavedModel?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Keys 区域
            item {
                ApiKeysSection(
                    apiKeys = apiKeys,
                    onAddClick = { showAddApiKeyDialog = true },
                    onEditClick = { editingApiKey = it },
                    onDeleteClick = { key ->
                        scope.launch { configManager.deleteApiKey(key.id) }
                    },
                    onTestClick = { key ->
                        scope.launch {
                            val result = apiClient.testConnection(key)
                            // TODO: 显示测试结果
                        }
                    }
                )
            }
            
            // 已保存的模型区域
            item {
                SavedModelsSection(
                    models = savedModels,
                    apiKeys = apiKeys,
                    onAddClick = { 
                        if (apiKeys.isNotEmpty()) {
                            selectedApiKey = apiKeys.first()
                            showAddModelDialog = true
                        }
                    },
                    onEditClick = { editingModel = it },
                    onDeleteClick = { model ->
                        scope.launch { configManager.deleteSavedModel(model.id) }
                    },
                    onSetDefaultClick = { model ->
                        scope.launch { configManager.setDefaultModel(model.id) }
                    }
                )
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    // 添加 API Key 对话框
    if (showAddApiKeyDialog) {
        AddApiKeyDialog(
            onDismiss = { showAddApiKeyDialog = false },
            onConfirm = { config ->
                scope.launch {
                    configManager.addApiKey(config)
                    showAddApiKeyDialog = false
                }
            },
            onTest = { config ->
                scope.launch { apiClient.testConnection(config) }
            }
        )
    }
    
    // 编辑 API Key 对话框
    editingApiKey?.let { key ->
        AddApiKeyDialog(
            initialConfig = key,
            onDismiss = { editingApiKey = null },
            onConfirm = { config ->
                scope.launch {
                    configManager.updateApiKey(config)
                    editingApiKey = null
                }
            },
            onTest = { config ->
                scope.launch { apiClient.testConnection(config) }
            }
        )
    }
    
    // 添加模型对话框
    if (showAddModelDialog && selectedApiKey != null) {
        AddModelDialog(
            apiKey = selectedApiKey!!,
            apiClient = apiClient,
            onDismiss = { showAddModelDialog = false },
            onConfirm = { model ->
                scope.launch {
                    configManager.saveModel(model)
                    showAddModelDialog = false
                }
            }
        )
    }
    
    // 编辑模型对话框
    editingModel?.let { model ->
        EditModelDialog(
            model = model,
            onDismiss = { editingModel = null },
            onConfirm = { updatedModel ->
                scope.launch {
                    configManager.updateSavedModel(updatedModel)
                    editingModel = null
                }
            }
        )
    }
}

/**
 * API Keys 区域
 */
@Composable
private fun ApiKeysSection(
    apiKeys: List<ApiKeyConfig>,
    onAddClick: () -> Unit,
    onEditClick: (ApiKeyConfig) -> Unit,
    onDeleteClick: (ApiKeyConfig) -> Unit,
    onTestClick: (ApiKeyConfig) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("API 密钥", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, "添加")
                }
            }
            
            if (apiKeys.isEmpty()) {
                Text(
                    "暂无 API 密钥，点击右上角添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                apiKeys.forEach { key ->
                    ApiKeyItem(
                        config = key,
                        onEdit = { onEditClick(key) },
                        onDelete = { onDeleteClick(key) },
                        onTest = { onTestClick(key) }
                    )
                }
            }
        }
    }
}

/**
 * API Key 项
 */
@Composable
private fun ApiKeyItem(
    config: ApiKeyConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiClient = remember { AiApiClient(context) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    config.provider.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "****${config.apiKey.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                testResult?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // 测试按钮
            TextButton(onClick = {
                scope.launch {
                    testResult = "测试中..."
                    val result = apiClient.testConnection(config)
                    testResult = if (result.isSuccess) "✓ 连接成功" else "✗ ${result.exceptionOrNull()?.message}"
                }
            }) {
                Text("测试")
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "更多")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                    )
                }
            }
        }
    }
}

/**
 * 已保存的模型区域
 */
@Composable
private fun SavedModelsSection(
    models: List<SavedModel>,
    apiKeys: List<ApiKeyConfig>,
    onAddClick: () -> Unit,
    onEditClick: (SavedModel) -> Unit,
    onDeleteClick: (SavedModel) -> Unit,
    onSetDefaultClick: (SavedModel) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已保存的模型", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = onAddClick,
                    enabled = apiKeys.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, "添加")
                }
            }
            
            Text(
                "配置模型的能力标签，以便在对应功能中使用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (models.isEmpty()) {
                if (apiKeys.isEmpty()) {
                    Text(
                        "请先添加 API 密钥",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "暂无保存的模型，点击右上角添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                models.forEach { model ->
                    SavedModelItem(
                        model = model,
                        onEdit = { onEditClick(model) },
                        onDelete = { onDeleteClick(model) },
                        onSetDefault = { onSetDefaultClick(model) }
                    )
                }
            }
        }
    }
}

/**
 * 已保存的模型项
 */
@Composable
private fun SavedModelItem(
    model: SavedModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            model.alias ?: model.model.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (model.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "默认",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        "${model.model.provider.displayName} / ${model.model.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        if (!model.isDefault) {
                            DropdownMenuItem(
                                text = { Text("设为默认") },
                                onClick = { showMenu = false; onSetDefault() },
                                leadingIcon = { Icon(Icons.Outlined.Star, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                        )
                    }
                }
            }
            
            // 能力标签
            if (model.capabilities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(model.capabilities) { capability ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                capability.displayName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 添加 API Key 对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddApiKeyDialog(
    initialConfig: ApiKeyConfig? = null,
    onDismiss: () -> Unit,
    onConfirm: (ApiKeyConfig) -> Unit,
    onTest: (ApiKeyConfig) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(initialConfig?.provider ?: AiProvider.GOOGLE) }
    var apiKey by remember { mutableStateOf(initialConfig?.apiKey ?: "") }
    var customBaseUrl by remember { mutableStateOf(initialConfig?.baseUrl ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiClient = remember { AiApiClient(context) }
    val uriHandler = LocalUriHandler.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialConfig != null) "编辑 API 密钥" else "添加 API 密钥") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 供应商选择
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("供应商") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AiProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    selectedProvider = provider
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // 供应商详情卡片
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 描述
                        Text(
                            selectedProvider.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 价格信息
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                selectedProvider.pricing,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 获取 API Key 链接
                        if (selectedProvider.apiKeyUrl.isNotBlank()) {
                            TextButton(
                                onClick = { uriHandler.openUri(selectedProvider.apiKeyUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("获取 API Key", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                
                // API Key 输入
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None 
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Outlined.VisibilityOff 
                                else Icons.Outlined.Visibility,
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 自定义 Base URL（仅 CUSTOM 供应商显示）
                if (selectedProvider == AiProvider.CUSTOM) {
                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = { customBaseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com") },
                        singleLine = true,
                        supportingText = { Text("兼容 OpenAI API 格式的服务地址") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // 测试结果
                testResult?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Row {
                // 测试按钮
                TextButton(
                    onClick = {
                        val config = ApiKeyConfig(
                            id = initialConfig?.id ?: java.util.UUID.randomUUID().toString(),
                            provider = selectedProvider,
                            apiKey = apiKey,
                            baseUrl = if (customBaseUrl.isNotBlank()) customBaseUrl else null
                        )
                        scope.launch {
                            isTesting = true
                            testResult = "测试中..."
                            val result = apiClient.testConnection(config)
                            testResult = if (result.isSuccess) "✓ 连接成功" else "✗ ${result.exceptionOrNull()?.message}"
                            isTesting = false
                        }
                    },
                    enabled = apiKey.isNotBlank() && !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("测试")
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        val config = ApiKeyConfig(
                            id = initialConfig?.id ?: java.util.UUID.randomUUID().toString(),
                            provider = selectedProvider,
                            apiKey = apiKey,
                            baseUrl = if (customBaseUrl.isNotBlank()) customBaseUrl else null
                        )
                        onConfirm(config)
                    },
                    enabled = apiKey.isNotBlank()
                ) {
                    Text("保存")
                }
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
 * 模型排序方式
 */
private enum class ModelSortType(val displayName: String) {
    NAME("名称"),
    CONTEXT("上下文"),
    PRICE_LOW("价格↑"),
    PRICE_HIGH("价格↓")
}

/**
 * 添加模型对话框
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddModelDialog(
    apiKey: ApiKeyConfig,
    apiClient: AiApiClient,
    onDismiss: () -> Unit,
    onConfirm: (SavedModel) -> Unit
) {
    val scope = rememberCoroutineScope()
    var models by remember { mutableStateOf<List<AiModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedModel by remember { mutableStateOf<AiModel?>(null) }
    var customModelId by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var selectedCapabilities by remember { mutableStateOf<Set<ModelCapability>>(setOf(ModelCapability.TEXT)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sortType by remember { mutableStateOf(ModelSortType.NAME) }
    
    // 排序后的模型列表
    val sortedModels = remember(models, sortType) {
        when (sortType) {
            ModelSortType.NAME -> models.sortedBy { it.name }
            ModelSortType.CONTEXT -> models.sortedByDescending { it.contextLength }
            ModelSortType.PRICE_LOW -> models.sortedBy { it.inputPrice }
            ModelSortType.PRICE_HIGH -> models.sortedByDescending { it.inputPrice }
        }
    }
    
    // 加载模型列表
    LaunchedEffect(apiKey) {
        isLoading = true
        val result = apiClient.fetchModels(apiKey)
        if (result.isSuccess) {
            models = result.getOrNull() ?: emptyList()
        } else {
            errorMessage = result.exceptionOrNull()?.message
        }
        isLoading = false
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("添加模型", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "从 ${apiKey.provider.displayName} 添加模型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 模型选择
                        if (models.isNotEmpty()) {
                            // 排序选项
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("选择模型 (${models.size} 个)", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ModelSortType.entries.forEach { type ->
                                        FilterChip(
                                            selected = sortType == type,
                                            onClick = { sortType = type },
                                            label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(28.dp)
                                        )
                                    }
                                }
                            }
                            
                            sortedModels.forEach { model ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedModel = model },
                                    shape = MaterialTheme.shapes.small,
                                    color = if (selectedModel == model) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(model.name, style = MaterialTheme.typography.bodyMedium)
                                            // 显示上下文和价格
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (model.contextLength > 0) {
                                                    Text(
                                                        "${model.contextLength / 1000}K",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (model.inputPrice > 0) {
                                                    Text(
                                                        "$${String.format("%.2f", model.inputPrice)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                } else if (model.inputPrice == 0.0 && model.contextLength > 0) {
                                                    Text(
                                                        "免费",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            model.id,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // 显示能力标签
                                        if (model.capabilities.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                model.capabilities.forEach { cap ->
                                                    Surface(
                                                        shape = MaterialTheme.shapes.extraSmall,
                                                        color = when (cap) {
                                                            ModelCapability.AUDIO -> MaterialTheme.colorScheme.primaryContainer
                                                            ModelCapability.IMAGE -> MaterialTheme.colorScheme.tertiaryContainer
                                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                                        }
                                                    ) {
                                                        Text(
                                                            cap.displayName,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 手动输入模型 ID
                        Divider()
                        Text("或手动输入模型 ID", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = customModelId,
                            onValueChange = { 
                                customModelId = it
                                if (it.isNotBlank()) selectedModel = null
                            },
                            label = { Text("模型 ID") },
                            placeholder = { Text("例如：gpt-4o-mini") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // 别名
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            label = { Text("别名（可选）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // 能力标签选择
                        Text("能力标签", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "选择此模型支持的能力，以便在对应功能中使用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModelCapability.entries.forEach { capability ->
                                FilterChip(
                                    selected = capability in selectedCapabilities,
                                    onClick = {
                                        selectedCapabilities = if (capability in selectedCapabilities) {
                                            selectedCapabilities - capability
                                        } else {
                                            selectedCapabilities + capability
                                        }
                                    },
                                    label = { Text(capability.displayName) }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val model = selectedModel ?: AiModel(
                                id = customModelId,
                                name = customModelId,
                                provider = apiKey.provider,
                                isCustom = true
                            )
                            
                            val savedModel = SavedModel(
                                model = model,
                                apiKeyId = apiKey.id,
                                alias = alias.ifBlank { null },
                                capabilities = selectedCapabilities.toList()
                            )
                            
                            onConfirm(savedModel)
                        },
                        enabled = selectedModel != null || customModelId.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/**
 * 编辑模型对话框
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditModelDialog(
    model: SavedModel,
    onDismiss: () -> Unit,
    onConfirm: (SavedModel) -> Unit
) {
    var alias by remember { mutableStateOf(model.alias ?: "") }
    var selectedCapabilities by remember { mutableStateOf(model.capabilities.toSet()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑模型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "${model.model.provider.displayName} / ${model.model.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("别名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("能力标签", style = MaterialTheme.typography.labelMedium)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModelCapability.entries.forEach { capability ->
                        FilterChip(
                            selected = capability in selectedCapabilities,
                            onClick = {
                                selectedCapabilities = if (capability in selectedCapabilities) {
                                    selectedCapabilities - capability
                                } else {
                                    selectedCapabilities + capability
                                }
                            },
                            label = { Text(capability.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(model.copy(
                        alias = alias.ifBlank { null },
                        capabilities = selectedCapabilities.toList()
                    ))
                }
            ) {
                Text("保存")
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
 * FlowRow 组件（用于标签布局）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
