package com.webtoapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.BorderStroke
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumFilterChip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.window.Dialog
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color

/**
 * AI settings
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
    
    // state
    val apiKeys by configManager.apiKeysFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val savedModels by configManager.savedModelsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showAddApiKeyDialog by remember { mutableStateOf(false) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    var selectedApiKey by remember { mutableStateOf<ApiKeyConfig?>(null) }
    var editingApiKey by remember { mutableStateOf<ApiKeyConfig?>(null) }
    var editingModel by remember { mutableStateOf<SavedModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().aiSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Keys area
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
                            result.onSuccess {
                                snackbarHostState.showSnackbar(
                                    message = "[OK] ${key.provider.name} 连接成功",
                                    duration = SnackbarDuration.Short
                                )
                            }.onFailure { error ->
                                snackbarHostState.showSnackbar(
                                    message = "[FAIL] 连接失败: ${error.message}",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                )
            }
            
            // Saved area
            item {
                SavedModelsSection(
                    models = savedModels,
                    apiKeys = apiKeys,
                    onAddClick = { 
                        if (apiKeys.isNotEmpty()) {
                            selectedApiKey = null  // user dialog select
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
    
    // API Key dialog
    if (showAddApiKeyDialog) {
        AddApiKeyDialog(
            onDismiss = { showAddApiKeyDialog = false },
            onConfirm = { config ->
                scope.launch {
                    if (configManager.addApiKey(config)) {
                        showAddApiKeyDialog = false
                    } else {
                        snackbarHostState.showSnackbar(
                            message = AppStringsProvider.current().saveFailed,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onTest = { config ->
                scope.launch { apiClient.testConnection(config) }
            }
        )
    }
    
    // edit API Key dialog
    editingApiKey?.let { key ->
        AddApiKeyDialog(
            initialConfig = key,
            onDismiss = { editingApiKey = null },
            onConfirm = { config ->
                scope.launch {
                    if (configManager.updateApiKey(config)) {
                        editingApiKey = null
                    } else {
                        snackbarHostState.showSnackbar(
                            message = AppStringsProvider.current().saveFailed,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onTest = { config ->
                scope.launch { apiClient.testConnection(config) }
            }
        )
    }
    
    // dialog
    if (showAddModelDialog && apiKeys.isNotEmpty()) {
        AddModelDialog(
            apiKeys = apiKeys,
            initialApiKey = selectedApiKey ?: apiKeys.first(),
            apiClient = apiClient,
            onDismiss = { showAddModelDialog = false },
            onConfirm = { models ->
                scope.launch {
                    var allSaved = true
                    models.forEach { model ->
                        if (!configManager.saveModel(model)) {
                            allSaved = false
                        }
                    }
                    if (allSaved) {
                        showAddModelDialog = false
                    } else {
                        snackbarHostState.showSnackbar(
                            message = AppStringsProvider.current().saveFailed,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
    }
    
    // edit dialog
    editingModel?.let { model ->
        EditModelDialog(
            model = model,
            onDismiss = { editingModel = null },
            onConfirm = { updatedModel ->
                scope.launch {
                    if (configManager.updateSavedModel(updatedModel)) {
                        editingModel = null
                    } else {
                        snackbarHostState.showSnackbar(
                            message = AppStringsProvider.current().saveFailed,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
    }
        }
}

/**
 * API Keys area
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
                Text(AppStringsProvider.current().apiKeys, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, AppStringsProvider.current().add)
                }
            }
            
            if (apiKeys.isEmpty()) {
                Text(
                    AppStringsProvider.current().noApiKeysHint,
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
 * API Key
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
    
    val isDark = com.webtoapp.ui.theme.LocalIsDarkTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
        border = BorderStroke(0.5.dp, if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                        color = if (it.startsWith("[OK]")) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // button
            TextButton(onClick = {
                scope.launch {
                    testResult = AppStringsProvider.current().testing
                    val result = apiClient.testConnection(config)
                    testResult = if (result.isSuccess) AppStringsProvider.current().connectionSuccess else "[FAIL] ${result.exceptionOrNull()?.message}"
                }
            }) {
                Text(AppStringsProvider.current().test)
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, AppStringsProvider.current().more)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(AppStringsProvider.current().edit) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStringsProvider.current().btnDelete) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                    )
                }
            }
        }
    }
}

/**
 * save area
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
                Text(AppStringsProvider.current().savedModels, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = onAddClick,
                    enabled = apiKeys.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, AppStringsProvider.current().add)
                }
            }
            
            Text(
                AppStringsProvider.current().configModelCapabilities,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (models.isEmpty()) {
                if (apiKeys.isEmpty()) {
                    Text(
                        AppStringsProvider.current().pleaseAddApiKeyFirst,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        AppStringsProvider.current().noSavedModelsHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // create API Key ID
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
 * save
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
    
    val isDark = com.webtoapp.ui.theme.LocalIsDarkTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
        border = BorderStroke(0.5.dp, if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                                    AppStringsProvider.current().defaultLabel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    // display API Key
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
                        Icon(Icons.Default.MoreVert, AppStringsProvider.current().more)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(AppStringsProvider.current().edit) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        if (!model.isDefault) {
                            DropdownMenuItem(
                                text = { Text(AppStringsProvider.current().setAsDefault) },
                                onClick = { showMenu = false; onSetDefault() },
                                leadingIcon = { Icon(Icons.Outlined.Star, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(AppStringsProvider.current().btnDelete) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                        )
                    }
                }
            }
            
            // label
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
            
            // Showsupport
            val supportedFeatures = model.getSupportedFeatures()
            if (supportedFeatures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${AppStringsProvider.current().availableFor}: ${supportedFeatures.joinToString("、") { it.displayName }}",
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
 * API Key dialog
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
        title = { Text(if (initialConfig != null) AppStringsProvider.current().editApiKey else AppStringsProvider.current().addApiKey) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // select
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStringsProvider.current().provider) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val groupedProviders = AiProvider.entries.groupBy { it.category }
                        val categoryOrder = listOf(
                            ProviderCategory.RECOMMENDED,
                            ProviderCategory.INTERNATIONAL,
                            ProviderCategory.AGGREGATOR,
                            ProviderCategory.CHINESE,
                            ProviderCategory.SELF_HOSTED,
                            ProviderCategory.CUSTOM
                        )
                        categoryOrder.forEach { category ->
                            val providers = groupedProviders[category] ?: return@forEach
                            // Note
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        category.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            providers.forEach { provider ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "  ${provider.displayName}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        selectedProvider = provider
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
                        
                        // Note
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
                        
                        // Get API Key
                        if (selectedProvider.apiKeyUrl.isNotBlank()) {
                            TextButton(
                                onClick = { uriHandler.openUri(selectedProvider.apiKeyUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStringsProvider.current().getApiKey, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                
                // API Key input( local optional)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (selectedProvider.requiresApiKey) "API Key" else "API Key (${AppStringsProvider.current().optionalLabel})") },
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
                
                // input( for API Key)
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(AppStringsProvider.current().aliasOptional) },
                    placeholder = { Text(AppStringsProvider.current().apiKeyAliasPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Custom Base URL( CUSTOM display)
                if (selectedProvider.allowCustomBaseUrl) {
                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = { customBaseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com") },
                        singleLine = true,
                        supportingText = { Text(AppStringsProvider.current().openAiCompatibleHint) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // API select
                    var formatExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedApiFormat.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStringsProvider.current().apiFormat) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    
                    // advanced expand/
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
                        Text(AppStringsProvider.current().advancedOptions)
                    }
                    
                    // advanced
                    AnimatedVisibility(visible = showAdvancedOptions) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = customModelsEndpoint,
                                onValueChange = { customModelsEndpoint = it },
                                label = { Text(AppStringsProvider.current().modelsEndpoint) },
                                placeholder = { Text("/v1/models") },
                                singleLine = true,
                                supportingText = { Text(AppStringsProvider.current().modelsEndpointHint) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = customChatEndpoint,
                                onValueChange = { customChatEndpoint = it },
                                label = { Text(AppStringsProvider.current().chatEndpoint) },
                                placeholder = { Text("/v1/chat/completions") },
                                singleLine = true,
                                supportingText = { Text(AppStringsProvider.current().chatEndpointHint) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Note
                testResult?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("[OK]")) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Row {
                // button
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
                            testResult = AppStringsProvider.current().testing
                            val result = apiClient.testConnection(config)
                            testResult = if (result.isSuccess) AppStringsProvider.current().connectionSuccess else "[FAIL] ${result.exceptionOrNull()?.message}"
                            isTesting = false
                        }
                    },
                    enabled = (apiKey.isNotBlank() || !selectedProvider.requiresApiKey) && !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(AppStringsProvider.current().test)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                PremiumButton(
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
                    enabled = apiKey.isNotBlank() || !selectedProvider.requiresApiKey
                ) {
                    Text(AppStringsProvider.current().btnSave)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}

/**
 * Note
 */
private enum class ModelSortType(val displayName: String) {
    NAME(AppStringsProvider.current().sortByName),
    CONTEXT(AppStringsProvider.current().sortByContext),
    PRICE_LOW(AppStringsProvider.current().sortByPriceLow),
    PRICE_HIGH(AppStringsProvider.current().sortByPriceHigh)
}

/**
 * dialog
 * supportselect API Key
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
    var selectedModels by remember { mutableStateOf<Set<AiModel>>(emptySet()) }  // support
    var customModelId by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var selectedCapabilities by remember { mutableStateOf<Set<ModelCapability>>(setOf(ModelCapability.TEXT)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sortType by remember { mutableStateOf(ModelSortType.NAME) }
    var isBatchMode by remember { mutableStateOf(false) }  // mode
    var searchQuery by remember { mutableStateOf("") }  // Note
    
    // list
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
    
    // Load list
    LaunchedEffect(selectedApiKey) {
        isLoading = true
        selectedModels = emptySet()  // switch API Key select
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
                Text(AppStringsProvider.current().addModel, style = MaterialTheme.typography.headlineSmall)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // API Key select
                var apiKeyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = apiKeyExpanded,
                    onExpandedChange = { apiKeyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedApiKey.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStringsProvider.current().selectApiKey) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(apiKeyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                        // select
                        if (models.isNotEmpty()) {
                            // Note
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(AppStringsProvider.current().searchModels) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = AppStringsProvider.current().clear)
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
                            
                            // Sort modeswitch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${AppStringsProvider.current().selectModel} (${filteredAndSortedModels.size}/${models.size})", 
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // modeswitch
                                    PremiumFilterChip(
                                        selected = isBatchMode,
                                        onClick = { 
                                            isBatchMode = !isBatchMode
                                            if (!isBatchMode) selectedModels = emptySet()
                                        },
                                        label = { Text(AppStringsProvider.current().batchSelectModels, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                            
                            // Note
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(ModelSortType.entries.size) { index ->
                                    val type = ModelSortType.entries[index]
                                    PremiumFilterChip(
                                        selected = sortType == type,
                                        onClick = { sortType = type },
                                        label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                            
                            // ( mode)
                            if (isBatchMode && selectedModels.isNotEmpty()) {
                                Text(
                                    AppStringsProvider.current().selectedModelsCount.format(selectedModels.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // hint
                            if (filteredAndSortedModels.isEmpty() && searchQuery.isNotEmpty()) {
                                Text(
                                    AppStringsProvider.current().noSearchResults,
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
                                                customModelId = ""  // input
                                            }
                                        },
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // modedisplay
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
                                        
                                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                                                            "$${String.format(java.util.Locale.getDefault(), "%.2f", model.inputPrice)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.tertiary
                                                        )
                                                    } else if (model.inputPrice == 0.0 && model.contextLength > 0) {
                                                        Text(
                                                            AppStringsProvider.current().free,
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
                        
                        // input ID( only mode)
                        if (!isBatchMode) {
                            HorizontalDivider()
                            Text(AppStringsProvider.current().orManualInputModelId, style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = customModelId,
                                onValueChange = { 
                                    customModelId = it
                                    if (it.isNotBlank()) selectedModels = emptySet()
                                },
                                label = { Text(AppStringsProvider.current().modelId) },
                                placeholder = { Text(AppStringsProvider.current().modelIdPlaceholder) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Note
                            OutlinedTextField(
                                value = alias,
                                onValueChange = { alias = it },
                                label = { Text(AppStringsProvider.current().aliasOptional) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // labelselect
                            Text(AppStringsProvider.current().capabilityTags, style = MaterialTheme.typography.labelMedium)
                            Text(
                                AppStringsProvider.current().selectCapabilitiesHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ModelCapability.entries.forEach { capability ->
                                    PremiumFilterChip(
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
                        Text(AppStringsProvider.current().btnCancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumButton(
                        onClick = {
                            val modelsToSave = if (isBatchMode && selectedModels.isNotEmpty()) {
                                // mode: in create SavedModel
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
                                // mode
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
                            AppStringsProvider.current().addSelectedModels 
                        else 
                            AppStringsProvider.current().btnSave)
                    }
                }
            }
        }
    }
}

/**
 * edit dialog
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
        mutableStateOf(model.featureMappings.mapValues { it.value.toSet() })
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
                Text(AppStringsProvider.current().editModel, style = MaterialTheme.typography.headlineSmall)
                
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
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text(AppStringsProvider.current().alias) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // labelselect
                    Text(AppStringsProvider.current().capabilityTags, style = MaterialTheme.typography.labelMedium)
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModelCapability.entries.forEach { capability ->
                            PremiumFilterChip(
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
                    
                    // config
                    if (selectedCapabilities.isNotEmpty()) {
                        HorizontalDivider()
                        
                        Text(
                            AppStringsProvider.current().featureSceneConfig,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            AppStringsProvider.current().selectFeaturesForCapability,
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
                                        this[capability] = features.toSet()
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
                        Text(AppStringsProvider.current().btnCancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumButton(
                        onClick = {
                            onConfirm(model.copy(
                                alias = alias.ifBlank { null },
                                capabilities = selectedCapabilities.toList(),
                                featureMappings = featureMappings.mapValues { it.value.toSet() }
                            ))
                        }
                    ) {
                        Text(AppStringsProvider.current().btnSave)
                    }
                }
            }
        }
    }
}

/**
 * label configcard
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
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        capability.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        AppStringsProvider.current().selectedCount.format(selectedFeatures.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) AppStringsProvider.current().collapse else AppStringsProvider.current().expand
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        AppStringsProvider.current().selectCapabilitiesForFeatures,
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
                            PremiumFilterChip(
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
                                        feature.displayName + if (isDefault) " *" else "",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = if (feature in selectedFeatures) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    // Note
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                // default
                                val defaults = AiFeature.entries.filter { 
                                    it.defaultCapabilities.contains(capability) 
                                }.toSet()
                                onFeaturesChanged(defaults)
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(AppStringsProvider.current().restoreDefault, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { onFeaturesChanged(AiFeature.entries.toSet()) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(AppStringsProvider.current().selectAll, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { onFeaturesChanged(emptySet()) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(AppStringsProvider.current().clearAll, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * FlowRow( forlabel)
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
