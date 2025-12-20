package com.webtoapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.StreamEvent
import com.webtoapp.core.ai.htmlcoding.*
import com.webtoapp.data.model.AiFeature
import com.webtoapp.data.model.ModelCapability
import com.webtoapp.data.model.SavedModel
import com.webtoapp.ui.components.htmlcoding.*
import com.webtoapp.ui.htmlpreview.HtmlPreviewActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * HTML编程AI主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlCodingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 管理器
    val storage = remember { HtmlCodingStorage(context) }
    val configManager = remember { AiConfigManager(context) }
    val apiClient = remember { AiApiClient(context) }
    
    // 状态 - 使用try-catch包装防止异常导致白屏
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    
    val sessions by storage.sessionsFlow.collectAsState(initial = emptyList())
    val currentSessionId by storage.currentSessionIdFlow.collectAsState(initial = null)
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())
    
    // 标记加载完成
    LaunchedEffect(sessions) {
        isLoading = false
    }
    
    // 筛选模型 - 使用功能场景筛选
    val textModels = savedModels.filter { it.supportsFeature(AiFeature.HTML_CODING) }
    val imageModels = savedModels.filter { it.supportsFeature(AiFeature.HTML_CODING_IMAGE) }
    
    // 当前会话
    var currentSession by remember { mutableStateOf<HtmlCodingSession?>(null) }
    LaunchedEffect(currentSessionId, sessions) {
        currentSession = sessions.find { it.id == currentSessionId }
    }
    
    // UI状态
    var inputText by remember { mutableStateOf("") }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var chatState by remember { mutableStateOf<ChatState>(ChatState.Idle) }
    var streamingContent by remember { mutableStateOf("") }
    var streamingThinking by remember { mutableStateOf("") }
    var showDrawer by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showStylesSheet by remember { mutableStateOf(false) }
    var showTutorialSheet by remember { mutableStateOf(false) }
    var showCheckpointsSheet by remember { mutableStateOf(false) }
    var showCodeLibrarySheet by remember { mutableStateOf(false) }
    var showConversationCheckpointsSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<HtmlCodingMessage?>(null) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File(storage.getImagesDir(), "temp_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (attachedImages.size < 3) {
                    attachedImages = attachedImages + tempFile.absolutePath
                }
            }
        }
    }
    
    // 列表滚动状态
    val listState = rememberLazyListState()
    
    // 自动滚动到底部
    LaunchedEffect(currentSession?.messages?.size) {
        currentSession?.messages?.size?.let { size ->
            if (size > 0) {
                listState.animateScrollToItem(size - 1)
            }
        }
    }
    
    // 发送消息函数
    fun sendMessage() {
        if (inputText.isBlank() || chatState !is ChatState.Idle) return
        
        val session = currentSession ?: return
        val config = session.config
        
        // 检查模型配置
        if (config.textModelId == null) {
            Toast.makeText(context, "请先选择文本模型", Toast.LENGTH_SHORT).show()
            showConfigSheet = true
            return
        }
        
        scope.launch {
            // 创建用户消息
            val userMessage = HtmlCodingMessage(
                role = MessageRole.USER,
                content = inputText,
                images = attachedImages
            )
            
            // 添加到会话
            val updatedSession = storage.addMessage(session.id, userMessage)
            currentSession = updatedSession
            
            // 清空输入
            val sentText = inputText
            val sentImages = attachedImages
            inputText = ""
            attachedImages = emptyList()
            
            // 设置加载状态
            chatState = ChatState.Loading
            
            try {
                // 获取模型和API Key
                val textModel = savedModels.find { it.id == config.textModelId }
                val imageModel = config.imageModelId?.let { id -> savedModels.find { it.id == id } }
                val apiKey = textModel?.let { model ->
                    apiKeys.find { it.id == model.apiKeyId }
                }
                
                if (textModel == null || apiKey == null) {
                    throw Exception("模型或API Key配置无效")
                }
                
                // 构建系统提示词
                val systemPrompt = HtmlCodingPrompts.buildSystemPrompt(
                    config = config,
                    hasImageModel = imageModel != null,
                    selectedTemplate = config.selectedTemplateId?.let { HtmlCodingPrompts.getTemplateById(it) },
                    selectedStyle = config.selectedStyleId?.let { HtmlCodingPrompts.getStyleById(it) }
                )
                
                // 构建消息历史
                val messages = buildList {
                    add(mapOf("role" to "system", "content" to systemPrompt))
                    updatedSession?.messages?.forEach { msg ->
                        val role = when (msg.role) {
                            MessageRole.USER -> "user"
                            MessageRole.ASSISTANT -> "assistant"
                            MessageRole.SYSTEM -> "system"
                        }
                        add(mapOf("role" to role, "content" to msg.content))
                    }
                }
                
                // 重置流式状态
                streamingContent = ""
                streamingThinking = ""
                
                // 使用流式API
                val thinkingBuilder = StringBuilder()
                var finalContent = ""
                
                apiClient.chatStream(
                    apiKey = apiKey,
                    model = textModel.model,
                    messages = messages,
                    temperature = config.temperature
                ).collect { event ->
                    when (event) {
                        is StreamEvent.Started -> {
                            chatState = ChatState.Streaming("")
                        }
                        is StreamEvent.Thinking -> {
                            thinkingBuilder.append(event.content)
                            streamingThinking = thinkingBuilder.toString()
                        }
                        is StreamEvent.Content -> {
                            streamingContent = event.accumulated
                            finalContent = event.accumulated
                        }
                        is StreamEvent.Done -> {
                            finalContent = event.fullContent
                            
                            // 解析最终响应
                            val parsed = CodeBlockParser.parseResponse(finalContent)
                            
                            // 创建AI消息
                            val aiMessage = HtmlCodingMessage(
                                role = MessageRole.ASSISTANT,
                                content = parsed.textContent,
                                thinking = if (thinkingBuilder.isNotEmpty()) thinkingBuilder.toString() else parsed.thinking,
                                codeBlocks = parsed.codeBlocks
                            )
                            
                            // 添加到会话
                            val finalSession = storage.addMessage(session.id, aiMessage)
                            currentSession = finalSession
                            
                            // 自动保存到代码库（如果有代码块）
                            if (parsed.codeBlocks.isNotEmpty()) {
                                storage.addToCodeLibrary(
                                    sessionId = session.id,
                                    messageId = aiMessage.id,
                                    userPrompt = userMessage.content,
                                    codeBlocks = parsed.codeBlocks,
                                    conversationContext = session.messages.takeLast(3).joinToString("\n") { 
                                        "${it.role}: ${it.content.take(100)}" 
                                    }
                                )
                            }
                            
                            // 创建对话检查点
                            storage.createConversationCheckpoint(
                                sessionId = session.id,
                                name = "对话 #${(finalSession?.messages?.size ?: 0) / 2}"
                            )
                            
                            // 清空流式状态
                            streamingContent = ""
                            streamingThinking = ""
                            
                            // 处理图像生成请求
                            if (imageModel != null && parsed.imageRequests.isNotEmpty()) {
                                val imageApiKey = apiKeys.find { it.id == imageModel.apiKeyId }
                                if (imageApiKey != null) {
                                    parsed.imageRequests.forEach { request ->
                                        chatState = ChatState.GeneratingImage(request.prompt)
                                    }
                                }
                            }
                        }
                        is StreamEvent.Error -> {
                            throw Exception(event.message)
                        }
                    }
                }
                
                chatState = ChatState.Idle
            } catch (e: Exception) {
                chatState = ChatState.Error(e.message ?: "发送失败")
                Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_LONG).show()
                chatState = ChatState.Idle
            }
        }
    }
    
    // 预览HTML
    fun previewHtml(codeBlock: CodeBlock) {
        scope.launch {
            try {
                // 如果是完整HTML，直接保存
                val htmlContent = if (codeBlock.isComplete) {
                    codeBlock.content
                } else {
                    // 否则合并所有代码块
                    currentSession?.messages
                        ?.flatMap { it.codeBlocks }
                        ?.let { CodeBlockParser.mergeToSingleHtml(it) }
                        ?: codeBlock.content
                }
                
                val file = storage.saveForPreview(htmlContent)
                
                // 启动预览Activity
                val intent = Intent(context, HtmlPreviewActivity::class.java).apply {
                    putExtra(HtmlPreviewActivity.EXTRA_FILE_PATH, file.absolutePath)
                    putExtra(HtmlPreviewActivity.EXTRA_TITLE, codeBlock.filename ?: "预览")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "预览失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 创建新会话
    fun createNewSession() {
        scope.launch {
            val session = storage.createSession()
            currentSession = session
            showDrawer = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentSession?.title ?: "HTML编程助手",
                            style = MaterialTheme.typography.titleMedium
                        )
                        currentSession?.let {
                            Text(
                                "${it.messages.size} 条消息",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 代码库
                    IconButton(onClick = { showCodeLibrarySheet = true }) {
                        Icon(Icons.Outlined.Folder, "代码库")
                    }
                    // 检查点回退
                    IconButton(onClick = { showConversationCheckpointsSheet = true }) {
                        Icon(Icons.Outlined.Restore, "回退")
                    }
                    // 模板
                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(Icons.Outlined.Palette, "模板")
                    }
                    // 设置
                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Outlined.Settings, "设置")
                    }
                    // 侧边栏
                    IconButton(onClick = { showDrawer = true }) {
                        Icon(Icons.Default.Menu, "会话列表")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputArea(
                value = inputText,
                onValueChange = { inputText = it },
                images = attachedImages,
                onAddImage = { imagePickerLauncher.launch("image/*") },
                onRemoveImage = { index ->
                    attachedImages = attachedImages.toMutableList().apply { removeAt(index) }
                },
                onSend = { sendMessage() },
                isLoading = chatState !is ChatState.Idle
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 加载状态显示
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "加载中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (currentSession == null) {
                // 无会话时显示欢迎界面
                WelcomeContent(
                    onNewChat = { createNewSession() },
                    onSelectTemplate = { showTemplatesSheet = true },
                    onOpenTutorial = { showTutorialSheet = true },
                    quickPrompts = HtmlCodingTutorial.quickPrompts,
                    onQuickPrompt = { prompt ->
                        scope.launch {
                            val session = storage.createSession()
                            currentSession = session
                            inputText = prompt
                        }
                    }
                )
            } else {
                // 消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = currentSession?.messages ?: emptyList(),
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            onEditClick = {
                                if (message.role == MessageRole.USER) {
                                    editingMessage = message
                                }
                            },
                            onPreviewCode = { codeBlock -> previewHtml(codeBlock) },
                            onCopyCode = {
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    
                    // 加载状态
                    if (chatState is ChatState.Loading) {
                        item {
                            LoadingIndicator()
                        }
                    }
                    
                    // 流式输出显示
                    if (chatState is ChatState.Streaming || streamingContent.isNotEmpty() || streamingThinking.isNotEmpty()) {
                        item {
                            StreamingMessageBubble(
                                thinking = streamingThinking,
                                content = streamingContent
                            )
                        }
                    }
                    
                    if (chatState is ChatState.GeneratingImage) {
                        item {
                            ImageGeneratingIndicator(
                                prompt = (chatState as ChatState.GeneratingImage).prompt
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 侧边抽屉 - 会话列表
    if (showDrawer) {
        ModalDrawerSheet(
            modifier = Modifier.width(280.dp)
        ) {
            SessionDrawerContent(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSessionClick = { sessionId ->
                    scope.launch {
                        storage.setCurrentSession(sessionId)
                        showDrawer = false
                    }
                },
                onDeleteSession = { sessionId ->
                    scope.launch {
                        storage.deleteSession(sessionId)
                    }
                },
                onNewSession = { createNewSession() },
                onDismiss = { showDrawer = false }
            )
        }
    }
    
    // 配置底部弹窗
    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false }
        ) {
            currentSession?.let { session ->
                ConfigPanel(
                    config = session.config,
                    onConfigChange = { newConfig ->
                        scope.launch {
                            storage.updateSession(session.copy(config = newConfig))
                            currentSession = session.copy(config = newConfig)
                        }
                    },
                    textModels = textModels,
                    imageModels = imageModels,
                    rulesTemplates = HtmlCodingPrompts.rulesTemplates,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } ?: run {
                Text(
                    "请先创建或选择一个会话",
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
    
    // 模板选择弹窗
    if (showTemplatesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplatesSheet = false }
        ) {
            TemplatesSheetContent(
                templates = HtmlCodingPrompts.styleTemplates,
                styles = HtmlCodingPrompts.styleReferences,
                selectedTemplateId = currentSession?.config?.selectedTemplateId,
                selectedStyleId = currentSession?.config?.selectedStyleId,
                onTemplateSelect = { templateId ->
                    scope.launch {
                        val session = currentSession ?: storage.createSession()
                        val newConfig = session.config.copy(selectedTemplateId = templateId)
                        storage.updateSession(session.copy(config = newConfig))
                        currentSession = session.copy(config = newConfig)
                    }
                },
                onStyleSelect = { styleId ->
                    scope.launch {
                        val session = currentSession ?: storage.createSession()
                        val newConfig = session.config.copy(selectedStyleId = styleId)
                        storage.updateSession(session.copy(config = newConfig))
                        currentSession = session.copy(config = newConfig)
                    }
                },
                onDismiss = { showTemplatesSheet = false }
            )
        }
    }
    
    // 教程弹窗
    if (showTutorialSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTutorialSheet = false }
        ) {
            TutorialSheetContent(
                chapters = HtmlCodingTutorial.chapters,
                onDismiss = { showTutorialSheet = false }
            )
        }
    }
    
    // 版本检查点弹窗
    if (showCheckpointsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckpointsSheet = false }
        ) {
            currentSession?.let { session ->
                CheckpointsSheetContent(
                    checkpoints = session.checkpoints,
                    currentIndex = session.currentCheckpointIndex,
                    onRestore = { checkpointId ->
                        scope.launch {
                            storage.rollbackToCheckpoint(session.id, checkpointId)?.let {
                                currentSession = it
                            }
                        }
                    },
                    onDelete = { checkpointId ->
                        scope.launch {
                            storage.deleteCheckpoint(session.id, checkpointId)
                            currentSession = storage.getSession(session.id)
                        }
                    },
                    onCreateCheckpoint = {
                        scope.launch {
                            storage.createCheckpoint(session.id, "手动保存 ${session.checkpoints.size + 1}")
                            currentSession = storage.getSession(session.id)
                        }
                    },
                    onSaveProject = { showSaveDialog = true },
                    onDismiss = { showCheckpointsSheet = false }
                )
            }
        }
    }
    
    // 编辑消息对话框
    editingMessage?.let { message ->
        EditMessageDialog(
            message = message,
            onDismiss = { editingMessage = null },
            onConfirm = { newContent, newImages ->
                scope.launch {
                    currentSession?.let { session ->
                        storage.editUserMessage(session.id, message.id, newContent, newImages)?.let {
                            currentSession = it
                        }
                    }
                    editingMessage = null
                }
            }
        )
    }
    
    // 保存项目对话框
    if (showSaveDialog) {
        SaveProjectDialog(
            storage = storage,
            files = currentSession?.checkpoints?.lastOrNull()?.files ?: emptyList(),
            onDismiss = { showSaveDialog = false },
            onSaved = { path ->
                Toast.makeText(context, "已保存到: $path", Toast.LENGTH_LONG).show()
                showSaveDialog = false
            }
        )
    }
    
    // 代码库弹窗
    if (showCodeLibrarySheet) {
        val codeLibrary by storage.codeLibraryFlow.collectAsState(initial = emptyList())
        
        ModalBottomSheet(
            onDismissRequest = { showCodeLibrarySheet = false }
        ) {
            CodeLibrarySheetContent(
                items = codeLibrary,
                onPreview = { item ->
                    // 预览代码
                    scope.launch {
                        val file = storage.getCodeLibraryPreviewFile(item.id)
                        if (file != null) {
                            val intent = Intent(context, HtmlPreviewActivity::class.java).apply {
                                putExtra(HtmlPreviewActivity.EXTRA_FILE_PATH, file.absolutePath)
                                putExtra(HtmlPreviewActivity.EXTRA_TITLE, item.title)
                            }
                            context.startActivity(intent)
                        }
                    }
                },
                onUseContent = { item ->
                    // 使用内容（填入输入框）
                    inputText = "基于这个代码继续开发:\n${item.userPrompt}"
                    showCodeLibrarySheet = false
                },
                onExportToProject = { item ->
                    // 导出到项目库
                    scope.launch {
                        val result = storage.exportToProjectLibrary(item, item.title.take(20))
                        if (result.isSuccess) {
                            Toast.makeText(context, "已导出到项目库", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "导出失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onToggleFavorite = { item ->
                    scope.launch { storage.toggleFavorite(item.id) }
                },
                onDelete = { item ->
                    scope.launch { storage.deleteCodeLibraryItem(item.id) }
                },
                onDismiss = { showCodeLibrarySheet = false }
            )
        }
    }
    
    // 对话检查点回退弹窗
    // 将状态提升到外部，避免在 ModalBottomSheet 内部使用 LaunchedEffect
    var conversationCheckpoints by remember { mutableStateOf<List<ConversationCheckpoint>>(emptyList()) }
    
    // 在显示弹窗时加载检查点
    LaunchedEffect(showConversationCheckpointsSheet, currentSession?.id) {
        if (showConversationCheckpointsSheet) {
            currentSession?.id?.let { sessionId ->
                conversationCheckpoints = storage.getSessionCheckpoints(sessionId)
            }
        }
    }
    
    if (showConversationCheckpointsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConversationCheckpointsSheet = false }
        ) {
            ConversationCheckpointsSheetContent(
                checkpoints = conversationCheckpoints,
                onRollback = { checkpoint ->
                    scope.launch {
                        storage.rollbackToConversationCheckpoint(checkpoint.id)?.let {
                            currentSession = it
                            Toast.makeText(context, "已回退到: ${checkpoint.name}", Toast.LENGTH_SHORT).show()
                        }
                        showConversationCheckpointsSheet = false
                    }
                },
                onDelete = { checkpoint ->
                    scope.launch {
                        storage.deleteConversationCheckpoint(checkpoint.id)
                        currentSession?.id?.let { sessionId ->
                            conversationCheckpoints = storage.getSessionCheckpoints(sessionId)
                        }
                    }
                },
                onDismiss = { showConversationCheckpointsSheet = false }
            )
        }
    }
}

// ==================== 子组件 ====================

@Composable
private fun WelcomeContent(
    onNewChat: () -> Unit,
    onSelectTemplate: () -> Unit,
    onOpenTutorial: () -> Unit,
    quickPrompts: List<String>,
    onQuickPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Logo区域
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Filled.Code,
                contentDescription = null,
                modifier = Modifier.padding(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "HTML 编程助手",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "AI 帮你快速生成精美网页",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 主操作按钮
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始新对话", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 辅助操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSelectTemplate,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("模板")
            }
            
            OutlinedButton(
                onClick = onOpenTutorial,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.School, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("教程")
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 快速提示词标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "快速开始",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 快速提示词卡片 - 两列网格布局
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            quickPrompts.take(4).chunked(2).forEach { rowPrompts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPrompts.forEach { prompt ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuickPrompt(prompt) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // 如果只有一个元素，添加空白占位
                    if (rowPrompts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "AI 正在思考...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ImageGeneratingIndicator(prompt: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                "正在生成图像...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                prompt.take(50) + if (prompt.length > 50) "..." else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SessionDrawerContent(
    sessions: List<HtmlCodingSession>,
    currentSessionId: String?,
    onSessionClick: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        // 头部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("对话历史", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "关闭")
            }
        }
        
        // 新建按钮
        FilledTonalButton(
            onClick = onNewSession,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("新建对话")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        
        // 会话列表
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions, key = { it.id }) { session ->
                SessionListItem(
                    session = session,
                    isSelected = session.id == currentSessionId,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { onDeleteSession(session.id) }
                )
            }
            
            if (sessions.isEmpty()) {
                item {
                    Text(
                        "暂无对话记录",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplatesSheetContent(
    templates: List<StyleTemplate>,
    styles: List<StyleReference>,
    selectedTemplateId: String?,
    selectedStyleId: String?,
    onTemplateSelect: (String?) -> Unit,
    onStyleSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "选择风格模板", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // 当前选中提示
            if (selectedTemplateId != null || selectedStyleId != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "已选择",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "选择一个风格模板，AI将根据该风格生成代码",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 分类标签
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("设计模板") },
                icon = { Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("风格参考") },
                icon = { Icon(Icons.Outlined.Style, null, modifier = Modifier.size(18.dp)) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedTab) {
            0 -> {
                // 模板网格 - 使用 LazyVerticalGrid 更好展示
                Text(
                    "共 ${templates.size} 个模板",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(templates) { template ->
                        StyleTemplateCard(
                            template = template,
                            isSelected = template.id == selectedTemplateId,
                            onClick = {
                                onTemplateSelect(
                                    if (template.id == selectedTemplateId) null else template.id
                                )
                            }
                        )
                    }
                }
            }
            1 -> {
                // 风格参考列表
                Text(
                    "共 ${styles.size} 个风格参考",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(styles) { style ->
                        StyleReferenceCard(
                            style = style,
                            isSelected = style.id == selectedStyleId,
                            onClick = {
                                onStyleSelect(
                                    if (style.id == selectedStyleId) null else style.id
                                )
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TutorialSheetContent(
    chapters: List<HtmlCodingTutorial.TutorialChapter>,
    onDismiss: () -> Unit
) {
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var selectedSectionIndex by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "使用教程", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${chapters.size} 章节",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedChapterId == null) {
            // 章节列表
            if (chapters.isEmpty()) {
                // 空状态提示
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无教程内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(chapters) { chapter ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedChapterId = chapter.id
                                    selectedSectionIndex = 0
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        Icons.Outlined.MenuBook, 
                                        null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        chapter.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "${chapter.sections.size} 个小节",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 章节内容
            val chapter = chapters.find { it.id == selectedChapterId }
            chapter?.let {
                // 返回按钮
                Surface(
                    modifier = Modifier
                        .clickable { selectedChapterId = null },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            it.title, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 小节标签
                ScrollableTabRow(
                    selectedTabIndex = selectedSectionIndex,
                    edgePadding = 0.dp
                ) {
                    it.sections.forEachIndexed { index, section ->
                        Tab(
                            selected = index == selectedSectionIndex,
                            onClick = { selectedSectionIndex = index },
                            text = { Text(section.title, maxLines = 1) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 内容
                val section = it.sections.getOrNull(selectedSectionIndex)
                section?.let { sec ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 内容文本
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                sec.content,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                lineHeight = 24.sp
                            )
                        }
                        
                        // 代码示例
                        sec.codeExample?.let { code ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "代码示例",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E1E)
                            ) {
                                Text(
                                    code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD4D4D4),
                                    modifier = Modifier.padding(16.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        
                        // 提示
                        if (sec.tips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Lightbulb,
                                            null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "小贴士",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    sec.tips.forEach { tip ->
                                        Text(
                                            "• $tip",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckpointsSheetContent(
    checkpoints: List<ProjectCheckpoint>,
    currentIndex: Int,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCreateCheckpoint: () -> Unit,
    onSaveProject: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("版本管理", style = MaterialTheme.typography.titleLarge)
            Row {
                FilledTonalButton(onClick = onCreateCheckpoint) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存版本")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSaveProject) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (checkpoints.isEmpty()) {
            Text(
                "暂无保存的版本\n对话中自动创建检查点，或手动保存版本",
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                itemsIndexed(checkpoints.reversed()) { index, checkpoint ->
                    CheckpointListItem(
                        checkpoint = checkpoint,
                        isCurrent = (checkpoints.size - 1 - index) == currentIndex,
                        onRestore = { onRestore(checkpoint.id) },
                        onDelete = { onDelete(checkpoint.id) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun EditMessageDialog(
    message: HtmlCodingMessage,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var editedContent by remember { mutableStateOf(message.content) }
    var editedImages by remember { mutableStateOf(message.images) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑消息") },
        text = {
            Column {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 10
                )
                
                if (editedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${editedImages.size} 张图片",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ 编辑后，该消息之后的对话将被删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(editedContent, editedImages) },
                enabled = editedContent.isNotBlank()
            ) {
                Text("重新发送")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SaveProjectDialog(
    storage: HtmlCodingStorage,
    files: List<ProjectFile>,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("my-html-project") }
    var selectedDirIndex by remember { mutableStateOf(0) }
    var createFolder by remember { mutableStateOf(true) }
    
    val availableDirs = remember { storage.getAvailableSaveDirectories() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存项目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("项目名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("保存位置", style = MaterialTheme.typography.labelMedium)
                
                availableDirs.forEachIndexed { index, (name, _) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDirIndex = index }
                    ) {
                        RadioButton(
                            selected = index == selectedDirIndex,
                            onClick = { selectedDirIndex = index }
                        )
                        Text(name)
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = createFolder,
                        onCheckedChange = { createFolder = it }
                    )
                    Text("创建项目文件夹")
                }
                
                Text(
                    "将保存 ${files.size} 个文件",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val (_, dir) = availableDirs[selectedDirIndex]
                    val config = SaveConfig(
                        directory = dir.absolutePath,
                        projectName = projectName,
                        createFolder = createFolder,
                        overwrite = true
                    )
                    val result = storage.saveProject(config, files)
                    result.onSuccess { savedDir ->
                        onSaved(savedDir.absolutePath)
                    }.onFailure { e ->
                        // 错误处理在调用处
                    }
                },
                enabled = projectName.isNotBlank() && files.isNotEmpty()
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
 * 代码库面板内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeLibrarySheetContent(
    items: List<CodeLibraryItem>,
    onPreview: (CodeLibraryItem) -> Unit,
    onUseContent: (CodeLibraryItem) -> Unit,
    onExportToProject: (CodeLibraryItem) -> Unit,
    onToggleFavorite: (CodeLibraryItem) -> Unit,
    onDelete: (CodeLibraryItem) -> Unit,
    onDismiss: () -> Unit
) {
    var filterFavorites by remember { mutableStateOf(false) }
    val filteredItems = if (filterFavorites) items.filter { it.isFavorite } else items
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "代码库",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                FilterChip(
                    selected = filterFavorites,
                    onClick = { filterFavorites = !filterFavorites },
                    label = { Text("收藏") },
                    leadingIcon = if (filterFavorites) {
                        { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "AI生成的代码会自动保存到这里",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (filterFavorites) "暂无收藏" else "代码库为空",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(filteredItems) { item ->
                    CodeLibraryItemCard(
                        item = item,
                        onPreview = { onPreview(item) },
                        onUseContent = { onUseContent(item) },
                        onExportToProject = { onExportToProject(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 代码库项目卡片
 */
@Composable
private fun CodeLibraryItemCard(
    item: CodeLibraryItem,
    onPreview: () -> Unit,
    onUseContent: () -> Unit,
    onExportToProject: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isFavorite) {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    dateFormat.format(java.util.Date(item.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                item.userPrompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 文件标签
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item.files.take(3).forEach { file ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (item.files.size > 3) {
                    Text(
                        "+${item.files.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("预览", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onUseContent,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("使用", style = MaterialTheme.typography.labelMedium)
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (item.isFavorite) "取消收藏" else "收藏") },
                            onClick = { onToggleFavorite(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    if (item.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                    null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导出到项目库") },
                            onClick = { onExportToProject(); showMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.FolderCopy, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = Color(0xFFE53935)) },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFE53935)) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 对话检查点面板内容
 */
@Composable
private fun ConversationCheckpointsSheetContent(
    checkpoints: List<ConversationCheckpoint>,
    onRollback: (ConversationCheckpoint) -> Unit,
    onDelete: (ConversationCheckpoint) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "对话检查点",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "回退到之前的对话状态，同时恢复代码库",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (checkpoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.History,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无检查点", color = MaterialTheme.colorScheme.outline)
                    Text(
                        "每次对话后会自动创建检查点",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(checkpoints) { checkpoint ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    checkpoint.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        dateFormat.format(java.util.Date(checkpoint.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            "${checkpoint.messageCount} 条消息",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (checkpoint.codeLibraryIds.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                "${checkpoint.codeLibraryIds.size} 个代码",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Row {
                                IconButton(onClick = { onRollback(checkpoint) }) {
                                    Icon(
                                        Icons.Outlined.Restore,
                                        "回退",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDelete(checkpoint) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
