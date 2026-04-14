package com.webtoapp.ui.screens.aimodule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumFilterChip

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.agent.*
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.components.aimodule.*
import kotlinx.coroutines.flow.collectLatest

/**
 * AI module( refactored)
 * 
 * Note
 * ModelSelector: select
 * StreamingMessageBubble: message
 * ToolCallCard: callcard
 * CodePreviewPanel: codepreviewpanel
 * 
 * Requirements: 4.1, 4.2, 4.4, 4.5, 4.7
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModuleDeveloperScreen(
    onNavigateBack: () -> Unit,
    onModuleCreated: (ExtensionModule) -> Unit,
    onNavigateToAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    
    // ViewModel
    val viewModel: AiModuleDeveloperViewModel = viewModel(
        factory = AiModuleDeveloperViewModel.Factory(context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // scrollstate
    val listState = rememberLazyListState()
    
    // Handle
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AiModuleDeveloperEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is AiModuleDeveloperEvent.ScrollToBottom -> {
                    if (uiState.shouldAutoScroll) {
                        val itemCount = listState.layoutInfo.totalItemsCount
                        if (itemCount > 0) {
                            listState.animateScrollToItem(itemCount - 1)
                        }
                    }
                }
                is AiModuleDeveloperEvent.ModuleCreated -> {
                    // Modulecreatesuccess, handle
                }
                is AiModuleDeveloperEvent.NavigateToAiSettings -> {
                    onNavigateToAiSettings()
                }
                is AiModuleDeveloperEvent.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(event.content))
                }
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(AppStringsProvider.current().aiModuleDeveloper, fontWeight = FontWeight.Bold)
                        // state
                        if (uiState.isDeveloping) {
                            Spacer(modifier = Modifier.width(4.dp))
                            StreamingStatusBadge(state = uiState.agentState)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                actions = {
                    // Resetbutton
                    if (uiState.messages.isNotEmpty() || uiState.generatedModule != null) {
                        IconButton(onClick = { viewModel.resetState() }) {
                            Icon(Icons.Default.Refresh, AppStringsProvider.current().btnReset)
                        }
                    }
                    // helpbutton
                    IconButton(onClick = { viewModel.toggleHelpDialog(true) }) {
                        Icon(Icons.AutoMirrored.Outlined.Help, AppStringsProvider.current().help)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // contentarea
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // welcome
                if (uiState.showWelcome) {
                    item { WelcomeCard() }
                    item { FeatureChips() }
                    item {
                        ExampleRequirements(
                            onSelect = { example -> viewModel.updateUserInput(example) }
                        )
                    }
                }
                
                // message
                items(uiState.messages, key = { it.id }) { message ->
                    ConversationMessageItem(
                        message = message,
                        onToolCallExpand = { /* optional: handle callexpand */ }
                    )
                }
                
                // outputincontent
                if (uiState.isStreaming) {
                    // content
                    if (uiState.thinkingContent.isNotBlank()) {
                        item {
                            ThinkingBlock(
                                content = uiState.thinkingContent,
                                isStreaming = true
                            )
                        }
                    }
                    
                    // content
                    if (uiState.streamingContent.isNotBlank()) {
                        item {
                            AssistantMessageBubble(
                                content = uiState.streamingContent,
                                isStreaming = true
                            )
                        }
                    }
                    
                    // current call
                    if (uiState.currentToolCalls.isNotEmpty()) {
                        item {
                            ToolCallGroup(toolCalls = uiState.currentToolCalls)
                        }
                    }
                    
                    // stateindicator
                    item {
                        StreamingStatusCard(state = uiState.agentState)
                    }
                }
                
                // Generate modulepreview
                if (uiState.generatedModule != null && !uiState.isStreaming) {
                    item {
                        GeneratedModuleSection(
                            module = uiState.generatedModule!!,
                            editedJsCode = uiState.editedJsCode,
                            editedCssCode = uiState.editedCssCode,
                            hasEdits = uiState.hasEdits,
                            onJsCodeChange = { viewModel.updateJsCode(it) },
                            onCssCodeChange = { viewModel.updateCssCode(it) },
                            onCopy = { viewModel.copyCode(it) },
                            onValidate = { viewModel.validateCode() },
                            onSave = { viewModel.saveModule(onModuleCreated) }
                        )
                    }
                }
                
                // Error
                if (uiState.error != null) {
                    item {
                        com.webtoapp.ui.components.aimodule.ErrorCard(
                            error = uiState.error!!,
                            onRetry = { viewModel.retry() },
                            onRetryWithDifferentModel = { viewModel.retryWithDifferentModel() },
                            onShowRawResponse = { /* Handled internally by ErrorCard */ },
                            onGoToSettings = { viewModel.navigateToAiSettings() },
                            onManualEdit = { /* Focus on code editor if available */ },
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
                
                // bottom
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
            
            // inputarea
            InputSection(
                userInput = uiState.userInput,
                onInputChange = { viewModel.updateUserInput(it) },
                selectedModel = uiState.selectedModel,
                availableModels = uiState.availableModels,
                onModelSelected = { viewModel.selectModel(it) },
                onConfigureModels = { viewModel.navigateToAiSettings() },
                selectedCategory = uiState.selectedCategory,
                onCategoryClick = { viewModel.toggleCategorySelector(true) },
                onClearCategory = { viewModel.selectCategory(null) },
                isDeveloping = uiState.isDeveloping,
                onSend = {
                    focusManager.clearFocus()
                    viewModel.startDevelopment()
                }
            )
        }
    }
    
    // selectdialog
    if (uiState.showCategorySelector) {
        CategorySelectorDialog(
            selectedCategory = uiState.selectedCategory,
            onSelect = { viewModel.selectCategory(it) },
            onDismiss = { viewModel.toggleCategorySelector(false) }
        )
    }
    
    // helpdialog
    if (uiState.showHelpDialog) {
        HelpDialog(onDismiss = { viewModel.toggleHelpDialog(false) })
    }
}


/**
 * message
 */
@Composable
private fun ConversationMessageItem(
    message: ConversationMessage,
    onToolCallExpand: (ToolCallInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (message.role) {
            MessageRole.USER -> {
                UserMessageBubble(content = message.content)
            }
            MessageRole.ASSISTANT -> {
                // content
                if (!message.thinkingContent.isNullOrBlank()) {
                    ThinkingBlock(
                        content = message.thinkingContent,
                        isStreaming = message.isStreaming
                    )
                }
                
                // messagecontent
                if (message.content.isNotBlank()) {
                    AssistantMessageBubble(
                        content = message.content,
                        isStreaming = message.isStreaming
                    )
                }
                
                // call
                if (message.toolCalls.isNotEmpty()) {
                    ToolCallGroup(toolCalls = message.toolCalls)
                }
            }
            else -> {
                // Systemmessageor message
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * inputarea
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputSection(
    userInput: String,
    onInputChange: (String) -> Unit,
    selectedModel: com.webtoapp.data.model.SavedModel?,
    availableModels: List<com.webtoapp.data.model.SavedModel>,
    onModelSelected: (com.webtoapp.data.model.SavedModel) -> Unit,
    onConfigureModels: () -> Unit,
    selectedCategory: ModuleCategory?,
    onCategoryClick: () -> Unit,
    onClearCategory: () -> Unit,
    isDeveloping: Boolean,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // select
            ModelSelector(
                selectedModel = selectedModel,
                availableModels = availableModels,
                onModelSelected = onModelSelected,
                onConfigureClick = onConfigureModels
            )
            
            // select
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Category,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    AppStringsProvider.current().categoryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                PremiumFilterChip(
                    selected = selectedCategory != null,
                    onClick = onCategoryClick,
                    label = { 
                        Text(
                            selectedCategory?.let { "${it.icon} ${it.getDisplayName()}" } ?: AppStringsProvider.current().autoDetectCategory,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
                
                if (selectedCategory != null) {
                    IconButton(
                        onClick = onClearCategory,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = AppStringsProvider.current().clear,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // input
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        AppStringsProvider.current().inputPlaceholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                minLines = 2,
                maxLines = 4,
                enabled = !isDeveloping,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSend() }),
                trailingIcon = {
                    IconButton(onClick = onSend, enabled = !isDeveloping && userInput.isNotBlank()) {
                        if (isDeveloping) {
                            SendingIndicator()
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = AppStringsProvider.current().startDevelopment,
                                tint = if (userInput.isNotBlank()) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * modulearea
 */
@Composable
private fun GeneratedModuleSection(
    module: GeneratedModuleData,
    editedJsCode: String,
    editedCssCode: String,
    hasEdits: Boolean,
    onJsCodeChange: (String) -> Unit,
    onCssCodeChange: (String) -> Unit,
    onCopy: (String) -> Unit,
    onValidate: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module card
        ModuleInfoCard(module = module)
        
        // codepreviewpanel
        CodePreviewPanel(
            jsCode = editedJsCode.ifBlank { module.jsCode },
            cssCode = editedCssCode.ifBlank { module.cssCode },
            onJsCodeChange = onJsCodeChange,
            onCssCodeChange = onCssCodeChange,
            onCopy = onCopy,
            onValidate = onValidate,
            onSave = onSave,
            isEditable = true
        )
        
        // edithint
        if (hasEdits) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        AppStringsProvider.current().codeModifiedHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * module card
 */
@Composable
private fun ModuleInfoCard(module: GeneratedModuleData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Success
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        AppStringsProvider.current().moduleGeneratedSuccess,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Module
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 2.dp
                ) {
                    Icon(
                        com.webtoapp.util.SvgIconMapper.getIcon(module.icon),
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        module.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        module.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // statelabel
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (module.syntaxValid) {
                    StatusChip(iconVector = Icons.Outlined.CheckCircle, text = AppStringsProvider.current().syntaxCorrect, color = MaterialTheme.colorScheme.primary)
                }
                if (module.securitySafe) {
                    StatusChip(iconVector = Icons.Outlined.Lock, text = AppStringsProvider.current().secureStatus, color = MaterialTheme.colorScheme.secondary)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                module.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * statelabel
 */
@Composable
private fun StatusChip(iconVector: ImageVector, text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                iconVector,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}


/**
 * statecard
 */
@Composable
private fun StreamingStatusCard(state: AgentState) {
    val stateIcon: ImageVector = when (state) {
        AgentState.THINKING -> Icons.Outlined.Psychology
        AgentState.GENERATING -> Icons.Outlined.AutoAwesome
        AgentState.TOOL_CALLING -> Icons.Outlined.Build
        AgentState.SYNTAX_CHECKING -> Icons.Outlined.Search
        AgentState.FIXING -> Icons.Outlined.Healing
        AgentState.SECURITY_SCANNING -> Icons.Outlined.Lock
        else -> Icons.Outlined.HourglassEmpty
    }
    val stateText = when (state) {
        AgentState.THINKING -> AppStringsProvider.current().analyzingRequirements
        AgentState.GENERATING -> AppStringsProvider.current().generatingCodeStatus
        AgentState.TOOL_CALLING -> AppStringsProvider.current().executingToolCalls
        AgentState.SYNTAX_CHECKING -> AppStringsProvider.current().syntaxCheckingStatus
        AgentState.FIXING -> AppStringsProvider.current().fixingIssuesStatus
        AgentState.SECURITY_SCANNING -> AppStringsProvider.current().securityScanningStatus
        else -> AppStringsProvider.current().processing
    }
    val color = when (state) {
        AgentState.THINKING, AgentState.GENERATING -> MaterialTheme.colorScheme.primary
        AgentState.TOOL_CALLING, AgentState.FIXING -> MaterialTheme.colorScheme.tertiary
        AgentState.SYNTAX_CHECKING, AgentState.SECURITY_SCANNING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                stateIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Text(
                stateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
            StreamingDots(color = color)
        }
    }
}

/**
 * state
 */
@Composable
private fun StreamingStatusBadge(state: AgentState) {
    val text = when (state) {
        AgentState.THINKING -> AppStringsProvider.current().statusAnalyzing
        AgentState.GENERATING -> AppStringsProvider.current().statusGenerating
        AgentState.TOOL_CALLING -> AppStringsProvider.current().statusExecuting
        AgentState.SYNTAX_CHECKING -> AppStringsProvider.current().statusChecking
        AgentState.FIXING -> AppStringsProvider.current().statusFixing
        AgentState.SECURITY_SCANNING -> AppStringsProvider.current().statusScanning
        else -> AppStringsProvider.current().statusProcessing
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeAlpha"
    )
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.2f)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * animation
 */
@Composable
private fun StreamingDots(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streamingDots")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size((6 * scale).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = scale))
            )
        }
    }
}

/**
 * indicator
 */
@Composable
private fun SendingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sending")
    
    Row(
        modifier = modifier.size(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 100
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 300,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sendDot$index"
            )
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .offset(y = offsetY.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ==================== welcome ====================

/**
 * welcomecard
 */
@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AI icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                AppStringsProvider.current().aiAssistant,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                AppStringsProvider.current().aiAssistantDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * label
 */
@Composable
private fun FeatureChips() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureChip(iconVector = Icons.Outlined.Search, text = AppStringsProvider.current().syntaxCheck)
        FeatureChip(iconVector = Icons.Outlined.Lock, text = AppStringsProvider.current().securityScan)
        FeatureChip(iconVector = Icons.Outlined.Healing, text = AppStringsProvider.current().autoFix)
        FeatureChip(iconVector = Icons.Outlined.Inventory2, text = AppStringsProvider.current().codeTemplate)
        FeatureChip(iconVector = Icons.Outlined.Science, text = AppStringsProvider.current().instantTest)
    }
}

@Composable
private fun FeatureChip(iconVector: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                iconVector,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Note
 */
@Composable
private fun ExampleRequirements(onSelect: (String) -> Unit) {
    val examples = listOf(
        Icons.Outlined.Block to AppStringsProvider.current().exampleBlockAds,
        Icons.Outlined.DarkMode to AppStringsProvider.current().exampleDarkMode,
        Icons.Outlined.SwipeDown to AppStringsProvider.current().exampleAutoScroll,
        Icons.Outlined.ContentCopy to AppStringsProvider.current().exampleUnlockCopy,
        Icons.Outlined.FastForward to AppStringsProvider.current().exampleVideoSpeed,
        Icons.Outlined.KeyboardArrowUp to AppStringsProvider.current().exampleBackToTop
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                AppStringsProvider.current().tryTheseExamples,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        examples.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (icon, text) ->
                    Surface(
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .clickable { onSelect(text) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                }
            }
        }
    }
}


// ==================== dialog ====================

/**
 * selectdialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelectorDialog(
    selectedCategory: ModuleCategory?,
    onSelect: (ModuleCategory?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(AppStringsProvider.current().selectModuleCategory)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    CategoryItem(
                        iconVector = Icons.Outlined.SmartToy,
                        name = AppStringsProvider.current().autoDetect,
                        description = AppStringsProvider.current().autoDetectCategoryHint,
                        selected = selectedCategory == null,
                        onClick = { onSelect(null) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                items(ModuleCategory.values().toList()) { category ->
                    CategoryItem(
                        icon = category.icon,
                        name = category.getDisplayName(),
                        description = category.getDescription(),
                        selected = selectedCategory == category,
                        onClick = { onSelect(category) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}

@Composable
private fun CategoryItem(
    iconVector: ImageVector? = null,
    icon: String = "",
    name: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
        else 
            Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (iconVector != null) {
                Icon(
                    iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    com.webtoapp.util.SvgIconMapper.getIcon(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * helpdialog
 */
@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Help,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(AppStringsProvider.current().usageHelp)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HelpSection(
                        iconVector = Icons.AutoMirrored.Outlined.Chat,
                        title = AppStringsProvider.current().helpHowToUse,
                        content = AppStringsProvider.current().helpHowToUseContent
                    )
                }
                
                item {
                    HelpSection(
                        iconVector = Icons.Outlined.Edit,
                        title = AppStringsProvider.current().helpRequirementTips,
                        content = AppStringsProvider.current().helpRequirementTipsContent
                    )
                }
                
                item {
                    HelpSection(
                        iconVector = Icons.Outlined.SmartToy,
                        title = AppStringsProvider.current().helpModelSelection,
                        content = AppStringsProvider.current().helpModelSelectionContent
                    )
                }
                
                item {
                    HelpSection(
                        iconVector = Icons.Outlined.FolderOpen,
                        title = AppStringsProvider.current().helpCategorySelection,
                        content = AppStringsProvider.current().helpCategorySelectionContent
                    )
                }
                
                item {
                    HelpSection(
                        iconVector = Icons.Outlined.Search,
                        title = AppStringsProvider.current().helpAutoCheck,
                        content = AppStringsProvider.current().helpAutoCheckContent
                    )
                }
                
                item {
                    HelpSection(
                        iconVector = Icons.Outlined.Edit,
                        title = AppStringsProvider.current().helpCodeEdit,
                        content = AppStringsProvider.current().helpCodeEditContent
                    )
                }
                
                item {
                    HelpSection(
                        iconVector = Icons.Outlined.Save,
                        title = AppStringsProvider.current().helpSaveModule,
                        content = AppStringsProvider.current().helpSaveModuleContent
                    )
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(AppStringsProvider.current().iUnderstand)
            }
        }
    )
}

@Composable
private fun HelpSection(iconVector: ImageVector, title: String, content: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                iconVector,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
