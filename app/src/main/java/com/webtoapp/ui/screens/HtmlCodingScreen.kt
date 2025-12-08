package com.webtoapp.ui.screens

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.htmlcoding.*
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
    
    // 状态
    val sessions by storage.sessionsFlow.collectAsState(initial = emptyList())
    val currentSessionId by storage.currentSessionIdFlow.collectAsState(initial = null)
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())
    
    // 筛选模型（能力标签为空时视为通用模型，不做筛选）
    val textModels = savedModels.filter { 
        it.capabilities.isEmpty() || it.capabilities.contains(ModelCapability.TEXT) 
    }
    val imageModels = savedModels.filter { 
        it.capabilities.contains(ModelCapability.IMAGE_GENERATION) 
    }
    
    // 当前会话
    var currentSession by remember { mutableStateOf<HtmlCodingSession?>(null) }
    LaunchedEffect(currentSessionId, sessions) {
        currentSession = sessions.find { it.id == currentSessionId }
    }
    
    // UI状态
    var inputText by remember { mutableStateOf("") }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 每个会话独立的聊天状态 (sessionId -> SessionChatState)
    val sessionStates = remember { mutableStateMapOf<String, SessionChatState>() }
    
    // 获取当前会话的状态
    val currentChatState = currentSessionId?.let { sessionStates[it] }
        ?: SessionChatState(sessionId = currentSessionId ?: "")
    
    var showDrawer by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showStylesSheet by remember { mutableStateOf(false) }
    var showTutorialSheet by remember { mutableStateOf(false) }
    var showCheckpointsSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<HtmlCodingMessage?>(null) }
    
    // 导出相关状态
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingExportFilename by remember { mutableStateOf("export.html") }
    
    // 文件保存选择器
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        output.write(pendingExportContent?.toByteArray() ?: byteArrayOf())
                    }
                    Toast.makeText(context, "文件已保存", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    pendingExportContent = null
                }
            }
        }
    }
    
    // 保存到 Download 目录
    fun saveToDownload(content: String, filename: String) {
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 使用 MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, filename)
                        put(MediaStore.Downloads.MIME_TYPE, "text/html")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { output ->
                            output.write(content.toByteArray())
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                    }
                    Toast.makeText(context, "已保存到 Download/$filename", Toast.LENGTH_LONG).show()
                } else {
                    // Android 9 及以下
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadDir, filename)
                    file.writeText(content)
                    Toast.makeText(context, "已保存到 ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
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
    
    // 判断是否在底部附近（允许一定误差）
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) true
            else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 2  // 在最后2个item内视为底部
            }
        }
    }
    
    // 智能滚动：仅当用户在底部时且有新消息时才自动滚动
    // 只监听 messages.size，避免流式输出时频繁触发
    LaunchedEffect(currentSession?.messages?.size) {
        if (isAtBottom) {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }
    
    // 更新会话状态的辅助函数
    fun updateSessionState(sessionId: String, update: (SessionChatState) -> SessionChatState) {
        val current = sessionStates[sessionId] ?: SessionChatState(sessionId = sessionId)
        sessionStates[sessionId] = update(current)
    }
    
    // 发送消息函数 - 使用流式输出，会话独立状态
    fun sendMessage() {
        val session = currentSession ?: return
        val sessionId = session.id
        val config = session.config
        
        // 检查当前会话是否已在处理中
        val state = sessionStates[sessionId]
        if (state?.chatState !is ChatState.Idle && state?.chatState != null) {
            Toast.makeText(context, "当前会话正在处理中", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (inputText.isBlank()) return
        
        // 检查模型配置
        if (config.textModelId == null) {
            Toast.makeText(context, "请先选择文本模型", Toast.LENGTH_SHORT).show()
            showConfigSheet = true
            return
        }
        
        val messageContent = inputText
        val messageImages = attachedImages
        
        // 清空输入
        inputText = ""
        attachedImages = emptyList()
        
        // 在后台针对该会话运行请求
        scope.launch {
            // 创建用户消息
            val userMessage = HtmlCodingMessage(
                role = MessageRole.USER,
                content = messageContent,
                images = messageImages
            )
            
            // 添加到会话
            val updatedSession = storage.addMessage(sessionId, userMessage)
            if (currentSessionId == sessionId) {
                currentSession = updatedSession
            }
            
            // 设置该会话的流式状态
            updateSessionState(sessionId) {
                it.copy(
                    chatState = ChatState.Streaming("", ""),
                    streamingContent = "",
                    streamingThinking = ""
                )
            }
            
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
                
                // 使用流式API - 分别收集正文和思考内容
                val fullContent = StringBuilder()
                val fullThinking = StringBuilder()
                
                apiClient.chatStream(
                    apiKey = apiKey,
                    model = textModel.model,
                    messages = messages,
                    temperature = config.temperature
                ).collect { delta ->
                    // 分别处理正文和思考内容
                    delta.content?.let { fullContent.append(it) }
                    delta.reasoning?.let { fullThinking.append(it) }
                    
                    // 更新该会话的状态
                    updateSessionState(sessionId) {
                        it.copy(
                            streamingContent = fullContent.toString(),
                            streamingThinking = fullThinking.toString(),
                            chatState = ChatState.Streaming(
                                partialContent = fullContent.toString(),
                                thinkingContent = fullThinking.toString()
                            )
                        )
                    }
                }
                
                // 流式输出完成，解析完整响应
                val content = fullContent.toString()
                val thinking = fullThinking.toString().takeIf { it.isNotBlank() }
                
                // 调试日志
                android.util.Log.d("HtmlCoding", "流式完成 - 内容长度: ${content.length}, 思考长度: ${thinking?.length ?: 0}")
                if (content.length < 500) {
                    android.util.Log.d("HtmlCoding", "完整内容: $content")
                } else {
                    android.util.Log.d("HtmlCoding", "内容前500字: ${content.take(500)}...")
                }
                
                // 即使 content 为空也尝试解析（可能只有代码块）
                val parsed = CodeBlockParser.parseResponse(content)
                
                // 检查是否有任何实际内容
                val hasContent = parsed.textContent.isNotBlank() || 
                                 parsed.codeBlocks.isNotEmpty() || 
                                 thinking != null ||
                                 parsed.thinking != null
                
                android.util.Log.d("HtmlCoding", "解析结果 - textContent: ${parsed.textContent.length}, codeBlocks: ${parsed.codeBlocks.size}, hasContent: $hasContent")
                
                if (hasContent) {
                    // 创建AI消息 - 使用流式收集的思考内容
                    val aiMessage = HtmlCodingMessage(
                        role = MessageRole.ASSISTANT,
                        content = parsed.textContent,
                        thinking = thinking ?: parsed.thinking,  // 优先使用流式收集的思考内容
                        codeBlocks = parsed.codeBlocks
                    )
                    
                    // 添加到会话
                    val finalSession = storage.addMessage(sessionId, aiMessage)
                    if (currentSessionId == sessionId) {
                        currentSession = finalSession
                    }
                    
                    // 处理图像生成请求
                    if (imageModel != null && parsed.imageRequests.isNotEmpty()) {
                        val imageApiKey = apiKeys.find { it.id == imageModel.apiKeyId }
                        if (imageApiKey != null) {
                            parsed.imageRequests.forEach { request ->
                                updateSessionState(sessionId) {
                                    it.copy(chatState = ChatState.GeneratingImage(request.prompt))
                                }
                                // TODO: 实现图像生成调用
                            }
                        }
                    }
                } else {
                    // 完全没有内容时显示详细提示
                    android.util.Log.w("HtmlCoding", "AI 未返回内容 - 原始内容为空: ${content.isEmpty()}")
                    val errorMsg = if (content.isEmpty()) {
                        "AI 未返回任何内容，请检查网络或模型配置"
                    } else {
                        "AI 响应解析失败，请重试"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
                
                // 完成，重置状态
                updateSessionState(sessionId) {
                    it.copy(
                        chatState = ChatState.Idle,
                        streamingContent = "",
                        streamingThinking = ""
                    )
                }
            } catch (e: Exception) {
                updateSessionState(sessionId) {
                    it.copy(
                        chatState = ChatState.Error(e.message ?: "发送失败"),
                        streamingContent = "",
                        streamingThinking = ""
                    )
                }
                Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_LONG).show()
                
                // 延迟后重置为Idle
                kotlinx.coroutines.delay(2000)
                updateSessionState(sessionId) {
                    it.copy(chatState = ChatState.Idle)
                }
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
    
    // 导出HTML文件 - 显示选择对话框
    fun exportHtml(codeBlock: CodeBlock) {
        // 准备导出内容
        val htmlContent = if (codeBlock.isComplete) {
            codeBlock.content
        } else {
            currentSession?.messages
                ?.flatMap { it.codeBlocks }
                ?.let { CodeBlockParser.mergeToSingleHtml(it) }
                ?: codeBlock.content
        }
        
        val filename = codeBlock.filename ?: "export_${System.currentTimeMillis()}.html"
        
        pendingExportContent = htmlContent
        pendingExportFilename = filename
        showExportDialog = true
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
                    // 教程
                    IconButton(onClick = { showTutorialSheet = true }) {
                        Icon(Icons.Outlined.School, "教程")
                    }
                    // 模板
                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(Icons.Outlined.Palette, "模板")
                    }
                    // 历史/版本
                    IconButton(onClick = { showCheckpointsSheet = true }) {
                        Icon(Icons.Outlined.History, "版本")
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
                isLoading = currentChatState.chatState !is ChatState.Idle
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentSession == null) {
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
                            },
                            onExportCode = { codeBlock -> exportHtml(codeBlock) }
                        )
                    }
                    
                    // 流式输出显示 - 使用当前会话的状态
                    val streamState = currentChatState.chatState
                    if (streamState is ChatState.Streaming) {
                        item(key = "streaming") {
                            StreamingMessageBubble(
                                content = currentChatState.streamingContent,
                                thinking = currentChatState.streamingThinking
                            )
                        }
                    }
                    
                    // 加载状态
                    if (streamState is ChatState.Loading) {
                        item {
                            LoadingIndicator()
                        }
                    }
                    
                    if (streamState is ChatState.GeneratingImage) {
                        item {
                            ImageGeneratingIndicator(
                                prompt = streamState.prompt
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
                    currentSession?.let { session ->
                        scope.launch {
                            val newConfig = session.config.copy(selectedTemplateId = templateId)
                            storage.updateSession(session.copy(config = newConfig))
                            currentSession = session.copy(config = newConfig)
                        }
                    }
                },
                onStyleSelect = { styleId ->
                    currentSession?.let { session ->
                        scope.launch {
                            val newConfig = session.config.copy(selectedStyleId = styleId)
                            storage.updateSession(session.copy(config = newConfig))
                            currentSession = session.copy(config = newConfig)
                        }
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
                        val sessionId = session.id
                        val config = session.config
                        
                        // 检查当前会话是否已在处理中
                        val state = sessionStates[sessionId]
                        if (state?.chatState !is ChatState.Idle && state?.chatState != null) {
                            Toast.makeText(context, "当前会话正在处理中", Toast.LENGTH_SHORT).show()
                            editingMessage = null
                            return@launch
                        }
                        
                        // 检查模型配置
                        if (config.textModelId == null) {
                            Toast.makeText(context, "请先选择文本模型", Toast.LENGTH_SHORT).show()
                            showConfigSheet = true
                            editingMessage = null
                            return@launch
                        }
                        
                        // 编辑消息并回滚后续消息
                        val editedSession = storage.editUserMessage(sessionId, message.id, newContent, newImages)
                        if (editedSession == null) {
                            Toast.makeText(context, "编辑消息失败", Toast.LENGTH_SHORT).show()
                            editingMessage = null
                            return@launch
                        }
                        currentSession = editedSession
                        editingMessage = null
                        
                        // 设置该会话的流式状态
                        updateSessionState(sessionId) {
                            it.copy(
                                chatState = ChatState.Streaming("", ""),
                                streamingContent = "",
                                streamingThinking = ""
                            )
                        }
                        
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
                            
                            // 构建消息历史（使用编辑后的会话）
                            val messages = buildList {
                                add(mapOf("role" to "system", "content" to systemPrompt))
                                editedSession.messages.forEach { msg ->
                                    val role = when (msg.role) {
                                        MessageRole.USER -> "user"
                                        MessageRole.ASSISTANT -> "assistant"
                                        MessageRole.SYSTEM -> "system"
                                    }
                                    add(mapOf("role" to role, "content" to msg.content))
                                }
                            }
                            
                            // 使用流式API - 分别收集正文和思考内容
                            val fullContent = StringBuilder()
                            val fullThinking = StringBuilder()
                            
                            apiClient.chatStream(
                                apiKey = apiKey,
                                model = textModel.model,
                                messages = messages,
                                temperature = config.temperature
                            ).collect { delta ->
                                delta.content?.let { fullContent.append(it) }
                                delta.reasoning?.let { fullThinking.append(it) }
                                
                                updateSessionState(sessionId) {
                                    it.copy(
                                        streamingContent = fullContent.toString(),
                                        streamingThinking = fullThinking.toString(),
                                        chatState = ChatState.Streaming(
                                            partialContent = fullContent.toString(),
                                            thinkingContent = fullThinking.toString()
                                        )
                                    )
                                }
                            }
                            
                            // 流式输出完成，解析完整响应
                            val content = fullContent.toString()
                            val thinking = fullThinking.toString().takeIf { it.isNotBlank() }
                            
                            // 调试日志
                            android.util.Log.d("HtmlCoding", "编辑重发完成 - 内容长度: ${content.length}")
                            
                            // 即使 content 为空也尝试解析
                            val parsed = CodeBlockParser.parseResponse(content)
                            
                            val hasContent = parsed.textContent.isNotBlank() || 
                                             parsed.codeBlocks.isNotEmpty() || 
                                             thinking != null ||
                                             parsed.thinking != null
                            
                            if (hasContent) {
                                val aiMessage = HtmlCodingMessage(
                                    role = MessageRole.ASSISTANT,
                                    content = parsed.textContent,
                                    thinking = thinking ?: parsed.thinking,
                                    codeBlocks = parsed.codeBlocks
                                )
                                
                                val finalSession = storage.addMessage(sessionId, aiMessage)
                                if (currentSessionId == sessionId) {
                                    currentSession = finalSession
                                }
                            } else {
                                val errorMsg = if (content.isEmpty()) {
                                    "AI 未返回任何内容，请检查网络或模型配置"
                                } else {
                                    "AI 响应解析失败，请重试"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                            
                            updateSessionState(sessionId) {
                                it.copy(chatState = ChatState.Idle, streamingContent = "", streamingThinking = "")
                            }
                        } catch (e: Exception) {
                            updateSessionState(sessionId) {
                                it.copy(chatState = ChatState.Error(e.message ?: "重新发送失败"), streamingContent = "", streamingThinking = "")
                            }
                            Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_LONG).show()
                            kotlinx.coroutines.delay(2000)
                            updateSessionState(sessionId) { it.copy(chatState = ChatState.Idle) }
                        }
                    }
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
    
    // 导出选择对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { 
                showExportDialog = false
                pendingExportContent = null
            },
            icon = {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
            },
            title = {
                Text("导出 HTML 文件")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "文件名: $pendingExportFilename",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "选择保存方式:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 保存到 Download 目录按钮
                    Button(
                        onClick = {
                            pendingExportContent?.let { content ->
                                saveToDownload(content, pendingExportFilename)
                            }
                            showExportDialog = false
                            pendingExportContent = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存到 Download 目录")
                    }
                    
                    // 选择位置按钮
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            saveFileLauncher.launch(pendingExportFilename)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择其他位置...")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showExportDialog = false
                        pendingExportContent = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            Icons.Filled.Code,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "HTML 编程助手",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "AI 帮你快速生成网页",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 操作按钮 - 使用wrap布局避免溢出
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 主按钮
            FilledTonalButton(
                onClick = onNewChat,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始新对话")
            }
            
            // 次要按钮行
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectTemplate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Palette, contentDescription = "模板")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("选模板")
                }
                
                OutlinedButton(
                    onClick = onOpenTutorial,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.School, contentDescription = "教程")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("使用教程")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 快速提示词
        Text(
            "快速开始",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        quickPrompts.take(4).forEach { prompt ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onQuickPrompt(prompt) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                    maxLines = 2
                )
            }
        }
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var selectedStyleCategory by remember { mutableStateOf<StyleReferenceCategory?>(null) }
    
    // 获取所有模板分类
    val templateCategories = remember(templates) {
        templates.map { it.category }.distinct()
    }
    
    // 获取所有风格参考分类
    val styleCategories = remember(styles) {
        styles.map { it.category }.distinct()
    }
    
    // 筛选后的模板
    val filteredTemplates = remember(templates, selectedCategory) {
        if (selectedCategory == null) templates
        else templates.filter { it.category == selectedCategory }
    }
    
    // 筛选后的风格
    val filteredStyles = remember(styles, selectedStyleCategory) {
        if (selectedStyleCategory == null) styles
        else styles.filter { it.category == selectedStyleCategory }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp)
    ) {
        // 顶部标题栏
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
            
            // 已选模板提示
            selectedTemplateId?.let { id ->
                templates.find { it.id == id }?.let { template ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                template.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab切换
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("设计模板 (${templates.size})")
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("风格参考 (${styles.size})")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        when (selectedTab) {
            0 -> {
                // 分类筛选标签
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("全部") },
                            leadingIcon = if (selectedCategory == null) {
                                { Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    items(templateCategories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { 
                                selectedCategory = if (selectedCategory == category) null else category 
                            },
                            label = { Text(category.displayName) },
                            leadingIcon = if (selectedCategory == category) {
                                { Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 模板数量提示
                Text(
                    "共 ${filteredTemplates.size} 个模板",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 模板网格 - 使用LazyVerticalGrid替代LazyRow
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 170.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
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
                // 风格参考分类筛选
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStyleCategory == null,
                            onClick = { selectedStyleCategory = null },
                            label = { Text("全部") },
                            leadingIcon = if (selectedStyleCategory == null) {
                                { Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    items(styleCategories) { category ->
                        FilterChip(
                            selected = selectedStyleCategory == category,
                            onClick = { 
                                selectedStyleCategory = if (selectedStyleCategory == category) null else category 
                            },
                            label = { Text(category.displayName) },
                            leadingIcon = if (selectedStyleCategory == category) {
                                { Icon(Icons.Filled.Done, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "共 ${filteredStyles.size} 个风格参考",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 风格列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredStyles, key = { it.id }) { style ->
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 底部操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onTemplateSelect(null)
                    onStyleSelect(null)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("清除选择")
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("确定")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
            .heightIn(max = 500.dp)
            .padding(16.dp)
    ) {
        Text("使用教程", style = MaterialTheme.typography.titleLarge)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedChapterId == null) {
            // 章节列表
            LazyColumn {
                items(chapters) { chapter ->
                    ListItem(
                        headlineContent = { Text(chapter.title) },
                        leadingContent = {
                            Icon(Icons.Outlined.MenuBook, null)
                        },
                        supportingContent = {
                            Text("${chapter.sections.size} 节")
                        },
                        modifier = Modifier.clickable {
                            selectedChapterId = chapter.id
                            selectedSectionIndex = 0
                        }
                    )
                }
            }
        } else {
            // 章节内容
            val chapter = chapters.find { it.id == selectedChapterId }
            chapter?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedChapterId = null }
                ) {
                    Icon(Icons.Default.ArrowBack, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it.title, style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 小节标签
                ScrollableTabRow(selectedTabIndex = selectedSectionIndex) {
                    it.sections.forEachIndexed { index, section ->
                        Tab(
                            selected = index == selectedSectionIndex,
                            onClick = { selectedSectionIndex = index },
                            text = { Text(section.title, maxLines = 1) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 内容
                val section = it.sections.getOrNull(selectedSectionIndex)
                section?.let { sec ->
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            sec.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        sec.codeExample?.let { code ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    code,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        
                        if (sec.tips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("💡 提示", style = MaterialTheme.typography.titleSmall)
                            sec.tips.forEach { tip ->
                                Text("• $tip", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
