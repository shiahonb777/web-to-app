package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.components.PremiumFilterChip

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.extension.*
import com.webtoapp.core.extension.agent.*
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * AI 模块开发器界面
 * 
 * 提供可视化的 Agent 开发体验，包括：
 * - 自然语言输入
 * - 流式思考过程展示
 * - 实时代码生成
 * - 工具调用可视化
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModuleDeveloperScreen(
    onNavigateBack: () -> Unit,
    onModuleCreated: (ExtensionModule) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Agent 引擎
    val agentEngine = remember { ModuleAgentEngine(context) }
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    
    // UI 状态
    var userInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    var showCategorySelector by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // Agent 状态
    val agentState by agentEngine.sessionState.collectAsStateWithLifecycle()
    var thoughts by remember { mutableStateOf<List<AgentThought>>(emptyList()) }
    var generatedModule by remember { mutableStateOf<GeneratedModuleData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var toolResults by remember { mutableStateOf<List<ToolCallResult>>(emptyList()) }
    
    // 滚动状态
    val listState = rememberLazyListState()
    
    // Yes否正在开发
    val isDeveloping = agentState != AgentSessionState.IDLE && 
                       agentState != AgentSessionState.COMPLETED && 
                       agentState != AgentSessionState.ERROR
    
    // Start开发
    fun startDevelopment() {
        if (userInput.isBlank()) {
            Toast.makeText(context, Strings.pleaseEnterRequirement, Toast.LENGTH_SHORT).show()
            return
        }
        
        focusManager.clearFocus()
        thoughts = emptyList()
        generatedModule = null
        errorMessage = null
        toolResults = emptyList()
        
        scope.launch {
            agentEngine.develop(userInput, selectedCategory).collect { event ->
                when (event) {
                    is AgentEvent.Thought -> {
                        thoughts = thoughts + event.thought
                    }
                    is AgentEvent.ModuleGenerated -> {
                        generatedModule = event.module
                    }
                    is AgentEvent.ToolResult -> {
                        toolResults = toolResults + event.result
                    }
                    is AgentEvent.Completed -> {
                        generatedModule = event.module
                    }
                    is AgentEvent.Error -> {
                        errorMessage = event.message
                    }
                    else -> {}
                }
                
                // Auto滚动到底部
                if (thoughts.isNotEmpty()) {
                    listState.animateScrollToItem(thoughts.size - 1)
                }
            }
        }
    }
    
    // Reset state
    fun resetState() {
        userInput = ""
        selectedCategory = null
        thoughts = emptyList()
        generatedModule = null
        errorMessage = null
        toolResults = emptyList()
    }
    
    // Save模块
    fun saveModule() {
        val module = generatedModule?.toExtensionModule() ?: return
        
        scope.launch {
            extensionManager.addModule(module).onSuccess {
                Toast.makeText(context, Strings.saveSuccess, Toast.LENGTH_SHORT).show()
                onModuleCreated(it)
            }.onFailure { e ->
                Toast.makeText(context, "${Strings.saveFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
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
                        Text(
                            Strings.aiModuleDevelopment,
                            fontWeight = FontWeight.Bold
                        )
                        // 移除通用加载指示器，改为在内容区域显示具体状态
                        // Requirements: 2.5, 3.1, 3.2
                        if (isDeveloping) {
                            Spacer(modifier = Modifier.width(4.dp))
                            StreamingStatusBadge(state = agentState)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    // Reset按钮
                    if (thoughts.isNotEmpty() || generatedModule != null) {
                        IconButton(onClick = { resetState() }) {
                            Icon(Icons.Default.Refresh, Strings.restart)
                        }
                    }
                    // 帮助按钮
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.Help, Strings.help)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // 主内容区域
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 欢迎卡片
                if (thoughts.isEmpty() && generatedModule == null && errorMessage == null) {
                    item {
                        WelcomeCard()
                    }
                    
                    // 功能特性
                    item {
                        FeatureChips()
                    }
                    
                    // 示例需求
                    item {
                        ExampleRequirements(
                            onSelect = { example ->
                                userInput = example
                            }
                        )
                    }
                }
                
                // 当前状态指示器 - 使用流式内容显示替代通用加载指示器
                // Requirements: 2.5, 3.1, 3.2
                if (isDeveloping) {
                    item {
                        StreamingStatusCard(state = agentState)
                    }
                }
                
                // 思考过程
                items(thoughts) { thought ->
                    ThoughtCard(thought = thought)
                }
                
                // 工具调用结果
                items(toolResults) { result ->
                    ToolResultCard(result = result)
                }
                
                // Generate的模块
                if (generatedModule != null) {
                    item {
                        GeneratedModuleCard(
                            module = generatedModule!!,
                            onSave = { saveModule() },
                            onEdit = { 
                                Toast.makeText(context, Strings.jumpToModuleEditor, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                
                // Error信息
                if (errorMessage != null) {
                    item {
                        ErrorCard(
                            message = errorMessage!!,
                            onRetry = { startDevelopment() }
                        )
                    }
                }
                
                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // 输入区域
            InputSection(
                userInput = userInput,
                onInputChange = { userInput = it },
                selectedCategory = selectedCategory,
                onCategoryClick = { showCategorySelector = true },
                onClearCategory = { selectedCategory = null },
                isDeveloping = isDeveloping,
                onSend = { startDevelopment() }
            )
        }
        }
    }
    
    // 分类选择对话框
    if (showCategorySelector) {
        CategorySelectorDialog(
            selectedCategory = selectedCategory,
            onSelect = { 
                selectedCategory = it
                showCategorySelector = false
            },
            onDismiss = { showCategorySelector = false }
        )
    }
    
    // 帮助对话框
    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
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
                    Strings.categoryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                PremiumFilterChip(
                    selected = selectedCategory != null,
                    onClick = onCategoryClick,
                    label = { 
                        Text(
                            selectedCategory?.let { "${it.icon} ${it.getDisplayName()}" } ?: Strings.autoDetectCategory,
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
                            contentDescription = Strings.clear,
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
                        Strings.inputPlaceholder,
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
                            // 使用动画点替代 CircularProgressIndicator
                            // Requirements: 2.5, 3.1, 3.2
                            SendingIndicator()
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = Strings.startDevelopment,
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
 * 欢迎卡片
 */
@Composable
private fun WelcomeCard() {
    EnhancedElevatedCard(
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
            // AI 图标
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
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                Strings.aiAssistant,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                Strings.aiAssistantDesc,
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
        AiFeatureChip(iconVector = Icons.Outlined.Search, text = Strings.syntaxCheck)
        AiFeatureChip(iconVector = Icons.Outlined.Lock, text = Strings.securityScan)
        AiFeatureChip(iconVector = Icons.Outlined.Build, text = Strings.autoFix)
        AiFeatureChip(iconVector = Icons.Outlined.Inventory2, text = Strings.codeTemplate)
        AiFeatureChip(iconVector = Icons.Outlined.Science, text = Strings.instantTest)
    }
}

@Composable
private fun AiFeatureChip(iconVector: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(iconVector, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
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
        Icons.Outlined.Block to Strings.exampleBlockAds,
        Icons.Outlined.DarkMode to Strings.exampleDarkMode,
        Icons.Outlined.SwipeDown to Strings.exampleAutoScroll,
        Icons.Outlined.ContentCopy to Strings.exampleUnlockCopy,
        Icons.Outlined.FastForward to Strings.exampleVideoSpeed,
        Icons.Outlined.KeyboardArrowUp to Strings.exampleBackToTop
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                Strings.tryTheseExamples,
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
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // 填充空位
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                }
            }
        }
    }
}

/**
 * 流式状态卡片
 * 
 * 替代原有的 DevelopingStatusCard，移除通用加载指示器
 * 使用具体的状态消息和动画效果
 * 
 * Requirements: 2.5, 3.1, 3.2
 */
@Composable
private fun StreamingStatusCard(state: AgentSessionState) {
    val (iconVector, text, color) = when (state) {
        AgentSessionState.THINKING -> Triple(Icons.Outlined.Psychology, Strings.analyzingRequirements, MaterialTheme.colorScheme.primary)
        AgentSessionState.PLANNING -> Triple(Icons.Outlined.Assignment, Strings.planningDevelopment, MaterialTheme.colorScheme.secondary)
        AgentSessionState.EXECUTING -> Triple(Icons.Outlined.Settings, Strings.executingToolCalls, MaterialTheme.colorScheme.tertiary)
        AgentSessionState.GENERATING -> Triple(Icons.Outlined.AutoAwesome, Strings.generatingCodeStatus, MaterialTheme.colorScheme.primary)
        AgentSessionState.REVIEWING -> Triple(Icons.Outlined.Visibility, Strings.reviewingCodeQuality, MaterialTheme.colorScheme.secondary)
        AgentSessionState.FIXING -> Triple(Icons.Outlined.Build, Strings.fixingDetectedIssues, MaterialTheme.colorScheme.tertiary)
        else -> Triple(Icons.Outlined.HourglassEmpty, Strings.statusProcessing, MaterialTheme.colorScheme.primary)
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
            Icon(iconVector, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
            // 使用打字动画替代 CircularProgressIndicator
            StreamingDots(color = color)
        }
    }
}

/**
 * 流式状态徽章
 * 
 * 用于顶部栏显示当前状态，替代 CircularProgressIndicator
 * 
 * Requirements: 2.5, 3.1, 3.2
 */
@Composable
private fun StreamingStatusBadge(state: AgentSessionState) {
    val text = when (state) {
        AgentSessionState.THINKING -> Strings.statusAnalyzing
        AgentSessionState.PLANNING -> Strings.statusPlanning
        AgentSessionState.EXECUTING -> Strings.statusExecuting
        AgentSessionState.GENERATING -> Strings.statusGenerating
        AgentSessionState.REVIEWING -> Strings.statusReviewing
        AgentSessionState.FIXING -> Strings.statusFixing
        else -> Strings.statusProcessing
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
 * 
 * 替代 CircularProgressIndicator 的动画效果
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
 * 开发状态卡片 (保留用于兼容性，但已弃用)
 * @deprecated 使用 StreamingStatusCard 替代
 */
@Deprecated("Use StreamingStatusCard instead", ReplaceWith("StreamingStatusCard(state)"))
@Composable
private fun DevelopingStatusCard(state: AgentSessionState) {
    StreamingStatusCard(state)
}

/**
 * 发送中指示器
 * 
 * 用于输入框中替代 CircularProgressIndicator
 * 显示三个跳动的点表示正在处理
 * 
 * Requirements: 2.5, 3.1, 3.2
 */
@Composable
private fun SendingIndicator(
    modifier: Modifier = Modifier
) {
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

/**
 * 思考卡片
 */
@Composable
private fun ThoughtCard(thought: AgentThought) {
    val backgroundColor = when (thought.type) {
        ThoughtType.ANALYSIS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ThoughtType.PLANNING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ThoughtType.TOOL_CALL -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ThoughtType.TOOL_RESULT -> if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
        ThoughtType.GENERATION -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ThoughtType.REVIEW -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ThoughtType.FIX -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ThoughtType.CONCLUSION -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ThoughtType.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Icon(
                        com.webtoapp.util.SvgIconMapper.getIcon(thought.type.icon),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 内容
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        thought.type.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        thought.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
                
                // 步骤编号
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${thought.step}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 工具结果卡片
 */
@Composable
private fun ToolResultCard(result: ToolCallResult) {
    val tool = AgentTools.getToolByName(result.toolName)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (result.success) 
            if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
        else 
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(com.webtoapp.util.SvgIconMapper.getIcon(tool?.type?.icon ?: "wrench"), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    tool?.type?.displayName ?: result.toolName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                if (result.success) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                Strings.success,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                Strings.failed,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Text(
                    "${result.executionTimeMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!result.success && result.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    result.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


/**
 * 生成的模块卡片
 */
@Composable
private fun GeneratedModuleCard(
    module: GeneratedModuleData,
    onSave: () -> Unit,
    onEdit: () -> Unit
) {
    var showCode by remember { mutableStateOf(false) }
    var showCss by remember { mutableStateOf(false) }
    
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Success标识
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
                        Strings.moduleGeneratedSuccess,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            com.webtoapp.util.SvgIconMapper.getIcon(module.icon),
                            contentDescription = null,
                            modifier = Modifier.padding(14.dp).size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            module.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 状态标签
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (module.syntaxValid) {
                    StatusChip(
                        icon = "check",
                        text = Strings.syntaxCorrect,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (module.securitySafe) {
                    StatusChip(
                        icon = "lock",
                        text = Strings.safe,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                module.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // JavaScript 代码预览
            CodePreviewSection(
                title = "JavaScript",
                code = module.jsCode,
                expanded = showCode,
                onToggle = { showCode = !showCode }
            )
            
            // CSS 代码（如果有）
            if (module.cssCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                CodePreviewSection(
                    title = "CSS",
                    code = module.cssCode,
                    expanded = showCss,
                    onToggle = { showCss = !showCss }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumOutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.edit)
                }
                
                PremiumButton(
                    onClick = onSave,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.saveModule)
                }
            }
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
            Icon(com.webtoapp.util.SvgIconMapper.getIcon(icon), contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * 代码预览区域
 */
@Composable
private fun CodePreviewSection(
    title: String,
    code: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            // 代码头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${code.lines().size} ${Strings.lines}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (expanded) Strings.collapse else Strings.expand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 代码内容
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1E1E)
            ) {
                val displayCode = if (expanded) {
                    code
                } else {
                    code.lines().take(5).joinToString("\n") + 
                        if (code.lines().size > 5) "\n// ..." else ""
                }
                
                Text(
                    displayCode,
                    modifier = Modifier
                        .padding(14.dp)
                        .then(if (expanded) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD4D4D4),
                        lineHeight = 18.sp
                    ),
                    maxLines = if (expanded) Int.MAX_VALUE else 6
                )
            }
        }
    }
}

/**
 * 错误卡片
 */
@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        Strings.developmentFailed,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PremiumButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Strings.retry)
            }
        }
    }
}

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
                Icon(com.webtoapp.util.SvgIconMapper.getIcon("folder"), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text(Strings.selectModuleCategory)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    CategoryItem(
                        icon = "smart_toy",
                        name = Strings.autoDetect,
                        description = Strings.autoDetectCategoryHint,
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
                Text(Strings.btnCancel)
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
            Icon(com.webtoapp.util.SvgIconMapper.getIcon(icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
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
                Icon(com.webtoapp.util.SvgIconMapper.getIcon("help"), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text(Strings.usageHelp)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HelpSection(
                        icon = "chat",
                        title = Strings.howToUse,
                        content = Strings.howToUseContent
                    )
                }
                
                item {
                    HelpSection(
                        icon = "edit_note",
                        title = Strings.requirementTips,
                        content = Strings.requirementTipsContent
                    )
                }
                
                item {
                    HelpSection(
                        icon = "folder",
                        title = Strings.categorySelection,
                        content = Strings.categorySelectionContent
                    )
                }
                
                item {
                    HelpSection(
                        icon = "search",
                        title = Strings.autoCheck,
                        content = Strings.autoCheckContent
                    )
                }
                
                item {
                    HelpSection(
                        icon = "save",
                        title = Strings.saveModuleTitle,
                        content = Strings.saveModuleContent
                    )
                }
                
                item {
                    HelpSection(
                        icon = "warning",
                        title = Strings.notes,
                        content = Strings.notesContent
                    )
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(Strings.gotIt)
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
            Icon(com.webtoapp.util.SvgIconMapper.getIcon(icon), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
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
