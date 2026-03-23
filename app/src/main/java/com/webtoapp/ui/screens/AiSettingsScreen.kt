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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.window.Dialog
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
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
                title = { Text(Strings.aiSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
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
            
            // Saved的模型区域
            item {
                SavedModelsSection(
                    models = savedModels,
                    apiKeys = apiKeys,
                    onAddClick = { 
                        if (apiKeys.isNotEmpty()) {
                            selectedApiKey = null  // 让用户在对话框中选择
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
    if (showAddModelDialog && apiKeys.isNotEmpty()) {
        AddModelDialog(
            apiKeys = apiKeys,
            initialApiKey = selectedApiKey ?: apiKeys.first(),
            apiClient = apiClient,
            onDismiss = { showAddModelDialog = false },
            onConfirm = { models ->
                scope.launch {
                    models.forEach { model ->
                        configManager.saveModel(model)
                    }
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
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Strings.apiKeys, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, Strings.add)
                }
            }
            
            if (apiKeys.isEmpty()) {
                Text(
                    Strings.noApiKeysHint,
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
                    testResult = Strings.testing
                    val result = apiClient.testConnection(config)
                    testResult = if (result.isSuccess) Strings.connectionSuccess else "✗ ${result.exceptionOrNull()?.message}"
                }
            }) {
                Text(Strings.test)
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, Strings.more)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.edit) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.btnDelete) },
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
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Strings.savedModels, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = onAddClick,
                    enabled = apiKeys.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, Strings.add)
                }
            }
            
            Text(
                Strings.configModelCapabilities,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (models.isEmpty()) {
                if (apiKeys.isEmpty()) {
                    Text(
                        Strings.pleaseAddApiKeyFirst,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        Strings.noSavedModelsHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 创建 API Key ID 到名称的映射
                val apiKeyMap = apiKeys.associateBy { it.id }
                
                models.forEach { model ->
                    val apiKeyName = apiKeyMap[model.apiKeyId]?.displayName
                    SavedModelItem(
                        model = model,
                        apiKeyName = apiKeyName,
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
    apiKeyName: String? = null,
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
                                    Strings.defaultLabel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    // 显示 API Key 名称和模型信息
                    Text(
                        if (apiKeyName != null) 
                            "$apiKeyName · ${model.model.id}" 
                        else 
                            "${model.model.provider.displayName} / ${model.model.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, Strings.more)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.edit) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        if (!model.isDefault) {
                            DropdownMenuItem(
                                text = { Text(Strings.setAsDefault) },
                                onClick = { showMenu = false; onSetDefault() },
                                leadingIcon = { Icon(Icons.Outlined.Star, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(Strings.btnDelete) },
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
            
            // Show支持的功能场景
            val supportedFeatures = model.getSupportedFeatures()
            if (supportedFeatures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${Strings.availableFor}: ${supportedFeatures.joinToString("、") { it.displayName }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
    var customModelsEndpoint by remember { mutableStateOf(initialConfig?.customModelsEndpoint ?: "") }
    var customChatEndpoint by remember { mutableStateOf(initialConfig?.customChatEndpoint ?: "") }
    var selectedApiFormat by remember { mutableStateOf(initialConfig?.apiFormat ?: ApiFormat.OPENAI_COMPATIBLE) }
    var alias by remember { mutableStateOf(initialConfig?.alias ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(initialConfig?.customModelsEndpoint != null || initialConfig?.customChatEndpoint != null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiClient = remember { AiApiClient(context) }
    val uriHandler = LocalUriHandler.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialConfig != null) Strings.editApiKey else Strings.addApiKey) },
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
                        label = { Text(Strings.provider) },
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
                        // Description
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
                        
                        // Get API Key 链接
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
                                Text(Strings.getApiKey, style = MaterialTheme.typography.labelMedium)
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
                
                // 别名输入（用于识别多个相同供应商的 API Key）
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(Strings.aliasOptional) },
                    placeholder = { Text(Strings.apiKeyAliasPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Custom Base URL（仅 CUSTOM 供应商显示）
                if (selectedProvider == AiProvider.CUSTOM) {
                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = { customBaseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com") },
                        singleLine = true,
                        supportingText = { Text(Strings.openAiCompatibleHint) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // API 格式选择
                    var formatExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedApiFormat.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Strings.apiFormat) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = formatExpanded,
                            onDismissRequest = { formatExpanded = false }
                        ) {
                            ApiFormat.entries.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.displayName) },
                                    onClick = {
                                        selectedApiFormat = format
                                        formatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // 高级选项展开/收起
                    TextButton(
                        onClick = { showAdvancedOptions = !showAdvancedOptions },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.advancedOptions)
                    }
                    
                    // 高级选项：自定义端点
                    AnimatedVisibility(visible = showAdvancedOptions) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = customModelsEndpoint,
                                onValueChange = { customModelsEndpoint = it },
                                label = { Text(Strings.modelsEndpoint) },
                                placeholder = { Text("/v1/models") },
                                singleLine = true,
                                supportingText = { Text(Strings.modelsEndpointHint) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = customChatEndpoint,
                                onValueChange = { customChatEndpoint = it },
                                label = { Text(Strings.chatEndpoint) },
                                placeholder = { Text("/v1/chat/completions") },
                                singleLine = true,
                                supportingText = { Text(Strings.chatEndpointHint) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
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
                            baseUrl = if (customBaseUrl.isNotBlank()) customBaseUrl else null,
                            customModelsEndpoint = if (customModelsEndpoint.isNotBlank()) customModelsEndpoint else null,
                            customChatEndpoint = if (customChatEndpoint.isNotBlank()) customChatEndpoint else null,
                            apiFormat = selectedApiFormat,
                            alias = if (alias.isNotBlank()) alias else null
                        )
                        scope.launch {
                            isTesting = true
                            testResult = Strings.testing
                            val result = apiClient.testConnection(config)
                            testResult = if (result.isSuccess) Strings.connectionSuccess else "✗ ${result.exceptionOrNull()?.message}"
                            isTesting = false
                        }
                    },
                    enabled = apiKey.isNotBlank() && !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(Strings.test)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        val config = ApiKeyConfig(
                            id = initialConfig?.id ?: java.util.UUID.randomUUID().toString(),
                            provider = selectedProvider,
                            apiKey = apiKey,
                            baseUrl = if (customBaseUrl.isNotBlank()) customBaseUrl else null,
                            customModelsEndpoint = if (customModelsEndpoint.isNotBlank()) customModelsEndpoint else null,
                            customChatEndpoint = if (customChatEndpoint.isNotBlank()) customChatEndpoint else null,
                            apiFormat = selectedApiFormat,
                            alias = if (alias.isNotBlank()) alias else null
                        )
                        onConfirm(config)
                    },
                    enabled = apiKey.isNotBlank()
                ) {
                    Text(Strings.btnSave)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

/**
 * 模型排序方式
 */
private enum class ModelSortType(val displayName: String) {
    NAME(Strings.sortByName),
    CONTEXT(Strings.sortByContext),
    PRICE_LOW(Strings.sortByPriceLow),
    PRICE_HIGH(Strings.sortByPriceHigh)
}

/**
 * 添加模型对话框
 * 支持选择 API Key 和批量添加模型
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddModelDialog(
    apiKeys: List<ApiKeyConfig>,
    initialApiKey: ApiKeyConfig,
    apiClient: AiApiClient,
    onDismiss: () -> Unit,
    onConfirm: (List<SavedModel>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedApiKey by remember { mutableStateOf(initialApiKey) }
    var models by remember { mutableStateOf<List<AiModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedModels by remember { mutableStateOf<Set<AiModel>>(emptySet()) }  // 支持多选
    var customModelId by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var selectedCapabilities by remember { mutableStateOf<Set<ModelCapability>>(setOf(ModelCapability.TEXT)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sortType by remember { mutableStateOf(ModelSortType.NAME) }
    var isBatchMode by remember { mutableStateOf(false) }  // 批量模式
    var searchQuery by remember { mutableStateOf("") }  // 搜索关键词
    
    // 过滤和排序后的模型列表
    val filteredAndSortedModels = remember(models, sortType, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            models
        } else {
            val query = searchQuery.lowercase()
            models.filter { 
                it.name.lowercase().contains(query) || 
                it.id.lowercase().contains(query) 
            }
        }
        when (sortType) {
            ModelSortType.NAME -> filtered.sortedBy { it.name }
            ModelSortType.CONTEXT -> filtered.sortedByDescending { it.contextLength }
            ModelSortType.PRICE_LOW -> filtered.sortedBy { it.inputPrice }
            ModelSortType.PRICE_HIGH -> filtered.sortedByDescending { it.inputPrice }
        }
    }
    
    // Load模型列表
    LaunchedEffect(selectedApiKey) {
        isLoading = true
        selectedModels = emptySet()  // 切换 API Key 时清空选择
        val result = apiClient.fetchModels(selectedApiKey)
        if (result.isSuccess) {
            models = result.getOrNull() ?: emptyList()
            errorMessage = null
        } else {
            models = emptyList()
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
                Text(Strings.addModel, style = MaterialTheme.typography.headlineSmall)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // API Key 选择器
                var apiKeyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = apiKeyExpanded,
                    onExpandedChange = { apiKeyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedApiKey.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Strings.selectApiKey) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(apiKeyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = apiKeyExpanded,
                        onDismissRequest = { apiKeyExpanded = false }
                    ) {
                        apiKeys.forEach { key ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(key.displayName)
                                        Text(
                                            "****${key.apiKey.takeLast(4)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedApiKey = key
                                    apiKeyExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 模型选择
                        if (models.isNotEmpty()) {
                            // 搜索框
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(Strings.searchModels) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = Strings.clear)
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                            
                            // Sort选项和批量模式切换
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${Strings.selectModel} (${filteredAndSortedModels.size}/${models.size})", 
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 批量模式切换
                                    FilterChip(
                                        selected = isBatchMode,
                                        onClick = { 
                                            isBatchMode = !isBatchMode
                                            if (!isBatchMode) selectedModels = emptySet()
                                        },
                                        label = { Text(Strings.batchSelectModels, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                            
                            // 排序选项
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(ModelSortType.entries.size) { index ->
                                    val type = ModelSortType.entries[index]
                                    FilterChip(
                                        selected = sortType == type,
                                        onClick = { sortType = type },
                                        label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                            
                            // 已选模型计数（批量模式）
                            if (isBatchMode && selectedModels.isNotEmpty()) {
                                Text(
                                    Strings.selectedModelsCount.format(selectedModels.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 无搜索结果提示
                            if (filteredAndSortedModels.isEmpty() && searchQuery.isNotEmpty()) {
                                Text(
                                    Strings.noSearchResults,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                            
                            filteredAndSortedModels.forEach { model ->
                                val isSelected = if (isBatchMode) model in selectedModels else selectedModels.size == 1 && model in selectedModels
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            if (isBatchMode) {
                                                selectedModels = if (model in selectedModels) {
                                                    selectedModels - model
                                                } else {
                                                    selectedModels + model
                                                }
                                            } else {
                                                selectedModels = setOf(model)
                                                customModelId = ""  // 清空手动输入
                                            }
                                        },
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 批量模式显示复选框
                                        if (isBatchMode) {
                                            Checkbox(
                                                checked = model in selectedModels,
                                                onCheckedChange = { checked ->
                                                    selectedModels = if (checked) {
                                                        selectedModels + model
                                                    } else {
                                                        selectedModels - model
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(model.name, style = MaterialTheme.typography.bodyMedium)
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
                                                            Strings.free,
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
                                            if (model.capabilities.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier.padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    model.capabilities.take(3).forEach { cap ->
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
                        }
                        
                        // 手动输入模型 ID（仅非批量模式）
                        if (!isBatchMode) {
                            Divider()
                            Text(Strings.orManualInputModelId, style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = customModelId,
                                onValueChange = { 
                                    customModelId = it
                                    if (it.isNotBlank()) selectedModels = emptySet()
                                },
                                label = { Text(Strings.modelId) },
                                placeholder = { Text(Strings.modelIdPlaceholder) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // 别名
                            OutlinedTextField(
                                value = alias,
                                onValueChange = { alias = it },
                                label = { Text(Strings.aliasOptional) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // 能力标签选择
                            Text(Strings.capabilityTags, style = MaterialTheme.typography.labelMedium)
                            Text(
                                Strings.selectCapabilitiesHint,
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
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.btnCancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val modelsToSave = if (isBatchMode && selectedModels.isNotEmpty()) {
                                // 批量模式：为每个选中的模型创建 SavedModel
                                selectedModels.map { model ->
                                    val capabilities = model.capabilities.toSet().ifEmpty { setOf(ModelCapability.TEXT) }
                                    val defaultMappings = capabilities.associateWith { capability ->
                                        AiFeature.entries.filter { feature ->
                                            feature.defaultCapabilities.contains(capability)
                                        }.toSet()
                                    }
                                    SavedModel(
                                        model = model,
                                        apiKeyId = selectedApiKey.id,
                                        alias = null,
                                        capabilities = capabilities.toList(),
                                        featureMappings = defaultMappings
                                    )
                                }
                            } else {
                                // 单个模式
                                val model = selectedModels.firstOrNull() ?: AiModel(
                                    id = customModelId,
                                    name = customModelId,
                                    provider = selectedApiKey.provider,
                                    isCustom = true
                                )
                                
                                val defaultMappings = selectedCapabilities.associateWith { capability ->
                                    AiFeature.entries.filter { feature ->
                                        feature.defaultCapabilities.contains(capability)
                                    }.toSet()
                                }
                                
                                listOf(SavedModel(
                                    model = model,
                                    apiKeyId = selectedApiKey.id,
                                    alias = alias.ifBlank { null },
                                    capabilities = selectedCapabilities.toList(),
                                    featureMappings = defaultMappings
                                ))
                            }
                            
                            onConfirm(modelsToSave)
                        },
                        enabled = (isBatchMode && selectedModels.isNotEmpty()) || 
                                  (!isBatchMode && (selectedModels.isNotEmpty() || customModelId.isNotBlank()))
                    ) {
                        Text(if (isBatchMode && selectedModels.size > 1) 
                            Strings.addSelectedModels 
                        else 
                            Strings.btnSave)
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
    var featureMappings by remember { 
        mutableStateOf(model.featureMappings.mapValues { it.value.toMutableSet() }.toMutableMap())
    }
    var expandedCapability by remember { mutableStateOf<ModelCapability?>(null) }
    
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
                Text(Strings.editModel, style = MaterialTheme.typography.headlineSmall)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "${model.model.provider.displayName} / ${model.model.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text(Strings.alias) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 能力标签选择
                    Text(Strings.capabilityTags, style = MaterialTheme.typography.labelMedium)
                    
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
                    
                    // 功能场景映射配置
                    if (selectedCapabilities.isNotEmpty()) {
                        Divider()
                        
                        Text(
                            Strings.featureSceneConfig,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            Strings.selectFeaturesForCapability,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        selectedCapabilities.forEach { capability ->
                            CapabilityFeatureCard(
                                capability = capability,
                                selectedFeatures = featureMappings[capability] 
                                    ?: AiFeature.entries.filter { it.defaultCapabilities.contains(capability) }.toSet(),
                                isExpanded = expandedCapability == capability,
                                onExpandToggle = {
                                    expandedCapability = if (expandedCapability == capability) null else capability
                                },
                                onFeaturesChanged = { features ->
                                    featureMappings = featureMappings.toMutableMap().apply {
                                        this[capability] = features.toMutableSet()
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.btnCancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(model.copy(
                                alias = alias.ifBlank { null },
                                capabilities = selectedCapabilities.toList(),
                                featureMappings = featureMappings.mapValues { it.value.toSet() }
                            ))
                        }
                    ) {
                        Text(Strings.btnSave)
                    }
                }
            }
        }
    }
}

/**
 * 能力标签对应功能场景的配置卡片
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CapabilityFeatureCard(
    capability: ModelCapability,
    selectedFeatures: Set<AiFeature>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onFeaturesChanged: (Set<AiFeature>) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        capability.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        Strings.selectedCount.format(selectedFeatures.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) Strings.collapse else Strings.expand
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        Strings.selectCapabilitiesForFeatures,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiFeature.entries.forEach { feature ->
                            val isDefault = feature.defaultCapabilities.contains(capability)
                            FilterChip(
                                selected = feature in selectedFeatures,
                                onClick = {
                                    val newFeatures = if (feature in selectedFeatures) {
                                        selectedFeatures - feature
                                    } else {
                                        selectedFeatures + feature
                                    }
                                    onFeaturesChanged(newFeatures)
                                },
                                label = { 
                                    Text(
                                        feature.displayName + if (isDefault) " ★" else "",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = if (feature in selectedFeatures) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    // 快捷操作
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                // 恢复默认
                                val defaults = AiFeature.entries.filter { 
                                    it.defaultCapabilities.contains(capability) 
                                }.toSet()
                                onFeaturesChanged(defaults)
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(Strings.restoreDefault, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { onFeaturesChanged(AiFeature.entries.toSet()) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(Strings.selectAll, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { onFeaturesChanged(emptySet()) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(Strings.clearAll, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
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
