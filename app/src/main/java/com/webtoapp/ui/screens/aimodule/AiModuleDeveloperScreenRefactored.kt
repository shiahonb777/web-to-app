package com.webtoapp.ui.screens.aimodule

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import com.webtoapp.ui.components.aimodule.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AI 模块开发器界面（重构版）
 * 
 * 使用新组件重构的聊天式界面，集成：
 * - ModelSelector: 模型选择器
 * - StreamingMessageBubble: 流式消息气泡
 * - ToolCallCard: 工具调用卡片
 * - CodePreviewPanel: 代码预览面板
 * 
 * Requirements: 4.1, 4.2, 4.4, 4.5, 4.7
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModuleDeveloperScreenRefactored(
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
    val uiState by viewModel.uiState.collectAsState()
    
    // 滚动状态
    val listState = rememberLazyListState()
    
    // 处理一次性事件
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
                    // 模块创建成功，由外部处理
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
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🤖", fontSize = 24.sp)
                        Text("AI 模块开发", fontWeight = FontWeight.Bold)
                        // 状态徽章
                        if (uiState.isDeveloping) {
                            Spacer(modifier = Modifier.width(4.dp))
                            StreamingStatusBadge(state = uiState.agentState)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 重置按钮
                    if (uiState.messages.isNotEmpty() || uiState.generatedModule != null) {
                        IconButton(onClick = { viewModel.resetState() }) {
                            Icon(Icons.Default.Refresh, "重新开始")
                        }
                    }
                    // 帮助按钮
                    IconButton(onClick = { viewModel.toggleHelpDialog(true) }) {
                        Icon(Icons.Outlined.Help, "帮助")
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
            // 主内容区域
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 欢迎界面
                if (uiState.showWelcome) {
                    item { WelcomeCard() }
                    item { FeatureChips() }
                    item {
                        ExampleRequirements(
                            onSelect = { example -> viewModel.updateUserInput(example) }
                        )
                    }
                }
                
                // 对话消息
                items(uiState.messages, key = { it.id }) { message ->
                    ConversationMessageItem(
                        message = message,
                        onToolCallExpand = { /* 可选：处理工具调用展开 */ }
                    )
                }
                
                // 流式输出中的内容
                if (uiState.isStreaming) {
                    // 思考内容
                    if (uiState.thinkingContent.isNotBlank()) {
                        item {
                            ThinkingBlock(
                                content = uiState.thinkingContent,
                                isStreaming = true
                            )
                        }
                    }
                    
                    // 流式内容
                    if (uiState.streamingContent.isNotBlank()) {
                        item {
                            AssistantMessageBubble(
                                content = uiState.streamingContent,
                                isStreaming = true
                            )
                        }
                    }
                    
                    // 当前工具调用
                    if (uiState.currentToolCalls.isNotEmpty()) {
                        item {
                            ToolCallGroup(toolCalls = uiState.currentToolCalls)
                        }
                    }
                    
                    // 状态指示器
                    item {
                        StreamingStatusCard(state = uiState.agentState)
                    }
                }
                
                // 生成的模块预览
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
                
                // 错误信息
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
                
                // 底部间距
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
            
            // 输入区域
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
    
    // 分类选择对话框
    if (uiState.showCategorySelector) {
        CategorySelectorDialog(
            selectedCategory = uiState.selectedCategory,
            onSelect = { viewModel.selectCategory(it) },
            onDismiss = { viewModel.toggleCategorySelector(false) }
        )
    }
    
    // 帮助对话框
    if (uiState.showHelpDialog) {
        HelpDialog(onDismiss = { viewModel.toggleHelpDialog(false) })
    }
}


/**
 * 对话消息项
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
                // 思考内容
                if (!message.thinkingContent.isNullOrBlank()) {
                    ThinkingBlock(
                        content = message.thinkingContent,
                        isStreaming = message.isStreaming
                    )
                }
                
                // 消息内容
                if (message.content.isNotBlank()) {
                    AssistantMessageBubble(
                        content = message.content,
                        isStreaming = message.isStreaming
                    )
                }
                
                // 工具调用
                if (message.toolCalls.isNotEmpty()) {
                    ToolCallGroup(toolCalls = message.toolCalls)
                }
            }
            else -> {
                // 系统消息或工具消息
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
 * 输入区域组件
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
            // 模型选择器
            ModelSelector(
                selectedModel = selectedModel,
                availableModels = availableModels,
                onModelSelected = onModelSelected,
                onConfigureClick = onConfigureModels
            )
            
            // 分类选择
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
                    "分类:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                FilterChip(
                    selected = selectedCategory != null,
                    onClick = onCategoryClick,
                    label = { 
                        Text(
                            selectedCategory?.let { "${it.icon} ${it.displayName}" } ?: "🤖 自动识别",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
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
                            contentDescription = "清除",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 输入框
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        "描述你想要的功能，例如：屏蔽网页上的广告弹窗...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                minLines = 2,
                maxLines = 4,
                enabled = !isDeveloping,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSend() }),
                trailingIcon = {
                    IconButton(
                        onClick = onSend,
                        enabled = !isDeveloping && userInput.isNotBlank()
                    ) {
                        if (isDeveloping) {
                            SendingIndicator()
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "开始开发",
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
 * 生成的模块区域
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
        // 模块信息卡片
        ModuleInfoCard(module = module)
        
        // 代码预览面板
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
        
        // 编辑提示
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
                        "代码已修改，保存时将使用修改后的版本",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * 模块信息卡片
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
            // 成功标识
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
                        "模块生成成功",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 模块信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        module.icon,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 24.sp
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
            
            // 状态标签
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (module.syntaxValid) {
                    StatusChip(icon = "✓", text = "语法正确", color = MaterialTheme.colorScheme.primary)
                }
                if (module.securitySafe) {
                    StatusChip(icon = "🔒", text = "安全", color = MaterialTheme.colorScheme.secondary)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 描述
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
 * 状态标签
 */
@Composable
private fun StatusChip(icon: String, text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 12.sp)
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}


/**
 * 流式状态卡片
 */
@Composable
private fun StreamingStatusCard(state: AgentState) {
    val (icon, text, color) = when (state) {
        AgentState.THINKING -> Triple("🤔", "正在分析需求...", MaterialTheme.colorScheme.primary)
        AgentState.GENERATING -> Triple("✨", "生成代码中...", MaterialTheme.colorScheme.primary)
        AgentState.TOOL_CALLING -> Triple("🔧", "执行工具调用...", MaterialTheme.colorScheme.tertiary)
        AgentState.SYNTAX_CHECKING -> Triple("🔍", "语法检查中...", MaterialTheme.colorScheme.secondary)
        AgentState.FIXING -> Triple("🩹", "修复检测到的问题...", MaterialTheme.colorScheme.tertiary)
        AgentState.SECURITY_SCANNING -> Triple("🔒", "安全扫描中...", MaterialTheme.colorScheme.secondary)
        else -> Triple("⏳", "处理中...", MaterialTheme.colorScheme.primary)
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
            Text(icon, fontSize = 24.sp)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Spacer(modifier = Modifier.weight(1f))
            StreamingDots(color = color)
        }
    }
}

/**
 * 流式状态徽章
 */
@Composable
private fun StreamingStatusBadge(state: AgentState) {
    val text = when (state) {
        AgentState.THINKING -> "分析中"
        AgentState.GENERATING -> "生成中"
        AgentState.TOOL_CALLING -> "执行中"
        AgentState.SYNTAX_CHECKING -> "检查中"
        AgentState.FIXING -> "修复中"
        AgentState.SECURITY_SCANNING -> "扫描中"
        else -> "处理中"
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
 * 流式动画点
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
 * 发送中指示器
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

// ==================== 欢迎界面组件 ====================

/**
 * 欢迎卡片
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
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 40.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "AI 模块开发助手",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "用自然语言描述你想要的功能\nAI 将自动生成扩展模块代码",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 功能特性标签
 */
@Composable
private fun FeatureChips() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureChip(icon = "🔍", text = "语法检查")
        FeatureChip(icon = "🔒", text = "安全扫描")
        FeatureChip(icon = "🩹", text = "自动修复")
        FeatureChip(icon = "📦", text = "代码模板")
        FeatureChip(icon = "🧪", text = "即时测试")
    }
}

@Composable
private fun FeatureChip(icon: String, text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, fontSize = 14.sp)
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * 示例需求
 */
@Composable
private fun ExampleRequirements(onSelect: (String) -> Unit) {
    val examples = listOf(
        "🚫" to "屏蔽网页上的广告弹窗和横幅",
        "🌙" to "为网页添加深色模式",
        "📜" to "自动滚动页面，方便阅读长文章",
        "📋" to "解除网页的复制限制",
        "⏩" to "为视频添加倍速播放控制",
        "⬆️" to "添加返回顶部悬浮按钮"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("💡", fontSize = 18.sp)
            Text(
                "试试这些示例",
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
                            .weight(1f)
                            .clickable { onSelect(text) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                            Text(icon, fontSize = 16.sp)
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
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


// ==================== 对话框组件 ====================

/**
 * 分类选择对话框
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
                Text("📂", fontSize = 24.sp)
                Text("选择模块分类")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    CategoryItem(
                        icon = "🤖",
                        name = "自动识别",
                        description = "让 AI 根据需求自动选择分类",
                        selected = selectedCategory == null,
                        onClick = { onSelect(null) }
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                items(ModuleCategory.values().toList()) { category ->
                    CategoryItem(
                        icon = category.icon,
                        name = category.displayName,
                        description = category.description,
                        selected = selectedCategory == category,
                        onClick = { onSelect(category) }
                    )
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

@Composable
private fun CategoryItem(
    icon: String,
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
            Text(icon, fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
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
 * 帮助对话框
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
                Text("❓", fontSize = 24.sp)
                Text("使用帮助")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HelpSection(
                        icon = "💬",
                        title = "如何使用",
                        content = "在输入框中用自然语言描述你想要的功能，AI 会自动分析需求并生成对应的扩展模块代码。"
                    )
                }
                
                item {
                    HelpSection(
                        icon = "📝",
                        title = "需求描述技巧",
                        content = "• 描述具体的功能效果\n• 说明目标网站或页面类型\n• 可以参考示例需求的写法"
                    )
                }
                
                item {
                    HelpSection(
                        icon = "🤖",
                        title = "模型选择",
                        content = "可以选择不同的 AI 模型来生成代码。不同模型可能有不同的效果和速度。"
                    )
                }
                
                item {
                    HelpSection(
                        icon = "📂",
                        title = "分类选择",
                        content = "可以手动选择模块分类，也可以让 AI 自动识别。手动选择可以让生成的代码更精准。"
                    )
                }
                
                item {
                    HelpSection(
                        icon = "🔍",
                        title = "自动检查",
                        content = "AI 会自动进行语法检查和安全扫描，确保生成的代码可以正常运行且没有安全隐患。"
                    )
                }
                
                item {
                    HelpSection(
                        icon = "✏️",
                        title = "代码编辑",
                        content = "生成的代码可以直接编辑修改，保存时会使用修改后的版本。"
                    )
                }
                
                item {
                    HelpSection(
                        icon = "💾",
                        title = "保存模块",
                        content = "生成完成后，点击「保存」将其添加到你的模块库中，之后可以在创建应用时使用。"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("我知道了")
            }
        }
    )
}

@Composable
private fun HelpSection(icon: String, title: String, content: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icon, fontSize = 18.sp)
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
