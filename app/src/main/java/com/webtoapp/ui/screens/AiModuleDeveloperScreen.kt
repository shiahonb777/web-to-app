package com.webtoapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

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
    val agentState by agentEngine.sessionState.collectAsState()
    var thoughts by remember { mutableStateOf<List<AgentThought>>(emptyList()) }
    var generatedModule by remember { mutableStateOf<GeneratedModuleData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var toolResults by remember { mutableStateOf<List<ToolCallResult>>(emptyList()) }
    
    // 滚动状态
    val listState = rememberLazyListState()
    
    // 是否正在开发
    val isDeveloping = agentState != AgentSessionState.IDLE && 
                       agentState != AgentSessionState.COMPLETED && 
                       agentState != AgentSessionState.ERROR
    
    // 开始开发
    fun startDevelopment() {
        if (userInput.isBlank()) {
            Toast.makeText(context, "请输入功能需求", Toast.LENGTH_SHORT).show()
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
                
                // 自动滚动到底部
                if (thoughts.isNotEmpty()) {
                    listState.animateScrollToItem(thoughts.size - 1)
                }
            }
        }
    }
    
    // 重置状态
    fun resetState() {
        userInput = ""
        selectedCategory = null
        thoughts = emptyList()
        generatedModule = null
        errorMessage = null
        toolResults = emptyList()
    }
    
    // 保存模块
    fun saveModule() {
        val module = generatedModule?.toExtensionModule() ?: return
        
        scope.launch {
            extensionManager.addModule(module).onSuccess {
                Toast.makeText(context, "✅ 模块已保存", Toast.LENGTH_SHORT).show()
                onModuleCreated(it)
            }.onFailure { e ->
                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Text(
                            "🤖",
                            fontSize = 24.sp
                        )
                        Text(
                            "AI 模块开发",
                            fontWeight = FontWeight.Bold
                        )
                        if (isDeveloping) {
                            Spacer(modifier = Modifier.width(4.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                    if (thoughts.isNotEmpty() || generatedModule != null) {
                        IconButton(onClick = { resetState() }) {
                            Icon(Icons.Default.Refresh, "重新开始")
                        }
                    }
                    // 帮助按钮
                    IconButton(onClick = { showHelpDialog = true }) {
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
                
                // 当前状态指示器
                if (isDeveloping) {
                    item {
                        DevelopingStatusCard(state = agentState)
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
                
                // 生成的模块
                if (generatedModule != null) {
                    item {
                        GeneratedModuleCard(
                            module = generatedModule!!,
                            onSave = { saveModule() },
                            onEdit = { 
                                Toast.makeText(context, "即将跳转到模块编辑器", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                
                // 错误信息
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
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
            // 动画图标
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
                // 填充空位
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 开发状态卡片
 */
@Composable
private fun DevelopingStatusCard(state: AgentSessionState) {
    val (icon, text, color) = when (state) {
        AgentSessionState.THINKING -> Triple("🤔", "正在分析需求...", MaterialTheme.colorScheme.primary)
        AgentSessionState.PLANNING -> Triple("📋", "制定开发计划...", MaterialTheme.colorScheme.secondary)
        AgentSessionState.EXECUTING -> Triple("⚙️", "执行工具调用...", MaterialTheme.colorScheme.tertiary)
        AgentSessionState.GENERATING -> Triple("✨", "生成代码中...", MaterialTheme.colorScheme.primary)
        AgentSessionState.REVIEWING -> Triple("👁️", "审查代码质量...", MaterialTheme.colorScheme.secondary)
        AgentSessionState.FIXING -> Triple("🩹", "修复检测到的问题...", MaterialTheme.colorScheme.tertiary)
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
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = color
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
        ThoughtType.TOOL_RESULT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                // 图标
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        thought.type.icon,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 18.sp
                    )
                }
                
                // 内容
                Column(modifier = Modifier.weight(1f)) {
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
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else 
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tool?.type?.icon ?: "🔧", fontSize = 18.sp)
                Text(
                    tool?.type?.displayName ?: result.toolName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
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
                                "成功",
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
                                "失败",
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                        Text(
                            module.icon,
                            modifier = Modifier.padding(14.dp),
                            fontSize = 28.sp
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
                        icon = "✓",
                        text = "语法正确",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (module.securitySafe) {
                    StatusChip(
                        icon = "🔒",
                        text = "安全",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 描述
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
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("编辑")
                }
                
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存模块")
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
                        "${code.lines().size} 行",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (expanded) "收起" else "展开",
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
    Card(
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "开发失败",
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
            
            Button(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重试")
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
                        icon = "💾",
                        title = "保存模块",
                        content = "生成完成后，点击「保存模块」将其添加到你的模块库中，之后可以在创建应用时使用。"
                    )
                }
                
                item {
                    HelpSection(
                        icon = "⚠️",
                        title = "注意事项",
                        content = "• 需要配置 AI API 密钥才能使用\n• 复杂功能可能需要多次调整\n• 建议在测试页面验证效果"
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
