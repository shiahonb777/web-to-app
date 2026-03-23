package com.webtoapp.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
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
import androidx.core.content.ContextCompat
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.AiGenerationService
import com.webtoapp.core.ai.StreamEvent
import com.webtoapp.core.ai.htmlcoding.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AiFeature
import com.webtoapp.data.model.ModelCapability
import com.webtoapp.data.model.SavedModel
import com.webtoapp.ui.components.htmlcoding.*
import com.webtoapp.ui.htmlpreview.HtmlPreviewActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * HTML编程AI主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlCodingScreen(
    onBack: () -> Unit,
    onExportToHtmlProject: ((List<ProjectFile>, String) -> Unit)? = null,  // Export到HTML项目的回调
    onNavigateToAiSettings: () -> Unit = {}  // 导航到AI设置的回调
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 管理器
    val storage = remember { HtmlCodingStorage(context) }
    val configManager = remember { AiConfigManager(context) }
    val apiClient = remember { AiApiClient(context) }
    val htmlAgent = remember { HtmlCodingAgent(context) }
    
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
    
    // 从 sessions Flow 同步 currentSession
    // 注意：只有当 sessions 更新时才同步，避免覆盖本地修改
    LaunchedEffect(currentSessionId, sessions) {
        val sessionFromFlow = sessions.find { it.id == currentSessionId }
        // 只有当 Flow 中的会话比本地更新时才同步
        if (sessionFromFlow != null) {
            val shouldUpdate = currentSession == null || 
                currentSession?.id != sessionFromFlow.id ||
                sessionFromFlow.updatedAt > (currentSession?.updatedAt ?: 0)
            
            android.util.Log.d("HtmlCodingScreen", "LaunchedEffect: sessionFromFlow=${sessionFromFlow.id}, currentSession=${currentSession?.id}, shouldUpdate=$shouldUpdate, flowUpdatedAt=${sessionFromFlow.updatedAt}, localUpdatedAt=${currentSession?.updatedAt}")
            
            if (shouldUpdate) {
                android.util.Log.d("HtmlCodingScreen", "LaunchedEffect: Updating currentSession from Flow")
                currentSession = sessionFromFlow
            }
        } else if (currentSessionId == null) {
            currentSession = null
        }
    }
    
    // UI state
    var inputText by remember { mutableStateOf("") }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var chatState by remember { mutableStateOf<ChatState>(ChatState.Idle) }
    var streamingContent by remember { mutableStateOf("") }
    var streamingThinking by remember { mutableStateOf("") }
    
    // 记录当前正在生成的会话ID，用于隔离不同会话的流式输出
    var generatingSessionId by remember { mutableStateOf<String?>(null) }
    
    // 项目文件相关状态
    val projectFileManager = remember { ProjectFileManager(context) }
    var projectFiles by remember { mutableStateOf<List<ProjectFileInfo>>(emptyList()) }
    var selectedProjectFile by remember { mutableStateOf<ProjectFileInfo?>(null) }
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var isFilesPanelExpanded by remember { mutableStateOf(true) }
    
    // 当会话变化时刷新项目文件列表
    LaunchedEffect(currentSessionId) {
        currentSessionId?.let { sessionId ->
            projectFiles = projectFileManager.listFiles(sessionId)
            selectedProjectFile = null
            selectedFileContent = null
        } ?: run {
            projectFiles = emptyList()
            selectedProjectFile = null
            selectedFileContent = null
        }
    }
    
    // Refresh项目文件列表的函数
    fun refreshProjectFiles() {
        currentSessionId?.let { sessionId ->
            projectFiles = projectFileManager.listFiles(sessionId)
        }
    }
    
    // 当前事件收集 Job，用于在新任务开始时取消旧的收集
    var eventCollectorJob by remember { mutableStateOf<Job?>(null) }
    
    // 当前服务连接引用，用于在新任务开始时解绑旧的连接
    var currentServiceConnection by remember { mutableStateOf<ServiceConnection?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }
    
    // Session切换时，清空当前会话的流式状态（但不中断后台任务）
    LaunchedEffect(currentSessionId) {
        // 如果切换到的会话不是正在生成的会话，清空流式显示
        if (currentSessionId != generatingSessionId) {
            streamingContent = ""
            streamingThinking = ""
            // Reset聊天状态为空闲（UI层面），后台任务继续运行
            if (chatState is ChatState.Streaming || chatState is ChatState.GeneratingImage) {
                chatState = ChatState.Idle
            }
            android.util.Log.d("HtmlCodingScreen", "Session switched: cleared streaming state, currentSessionId=$currentSessionId, generatingSessionId=$generatingSessionId")
        }
    }
    
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
    
    // Image选择器
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
    
    // List滚动状态
    val listState = rememberLazyListState()
    
    // Auto滚动到底部
    LaunchedEffect(currentSession?.messages?.size) {
        currentSession?.messages?.size?.let { size ->
            if (size > 0) {
                listState.animateScrollToItem(size - 1)
            }
        }
    }
    
    // 发送消息函数 - 使用前台服务保持后台运行
    fun sendMessage() {
        android.util.Log.d("HtmlCodingScreen", "sendMessage called: inputText='${inputText.take(50)}', chatState=$chatState, currentSession=${currentSession?.id}, generatingSessionId=$generatingSessionId")
        
        if (inputText.isBlank()) {
            android.util.Log.d("HtmlCodingScreen", "sendMessage: inputText is blank, returning")
            return
        }
        if (chatState !is ChatState.Idle) {
            android.util.Log.d("HtmlCodingScreen", "sendMessage: chatState is not Idle ($chatState), returning")
            return
        }
        
        scope.launch {
            // 如果没有当前会话，先创建一个新会话
            val session = currentSession ?: run {
                val newSession = storage.createSession()
                currentSession = newSession
                newSession
            }
            val config = session.config
            
            // Check模型配置
            if (config.textModelId == null) {
                Toast.makeText(context, Strings.pleaseSelectTextModel, Toast.LENGTH_SHORT).show()
                showConfigSheet = true
                return@launch
            }
            
            // Create用户消息
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
            inputText = ""
            attachedImages = emptyList()
            
            // 进入流式状态
            chatState = ChatState.Streaming("")
            
            // 记录正在生成的会话ID
            val targetSessionId = session.id
            generatingSessionId = targetSessionId
            
            try {
                // Get模型
                val textModel = savedModels.find { it.id == config.textModelId }
                if (textModel == null) {
                    throw Exception(Strings.modelConfigInvalid)
                }
                
                // Set当前HTML（用于edit_html工具）
                val currentHtml = updatedSession?.messages
                    ?.flatMap { it.codeBlocks }
                    ?.lastOrNull { it.language == "html" }
                    ?.content
                
                // Reset流式状态
                streamingContent = ""
                streamingThinking = ""
                
                // Start前台服务
                val serviceIntent = Intent(context, AiGenerationService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
                
                // Cancel之前的事件收集 Job
                eventCollectorJob?.cancel()
                eventCollectorJob = null
                android.util.Log.d("HtmlCodingScreen", "Cancelled previous event collector job")
                
                // 解绑之前的服务连接（如果存在）
                if (isServiceBound && currentServiceConnection != null) {
                    try {
                        context.unbindService(currentServiceConnection!!)
                        android.util.Log.d("HtmlCodingScreen", "Unbound previous service connection")
                    } catch (e: Exception) {
                        android.util.Log.e("HtmlCodingScreen", "Error unbinding previous service", e)
                    }
                    isServiceBound = false
                    currentServiceConnection = null
                }
                
                // 绑定服务并开始生成
                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val service = (binder as AiGenerationService.LocalBinder).getService()
                        isServiceBound = true
                        
                        // Listen服务事件 - 保存 Job 引用以便后续取消
                        eventCollectorJob = scope.launch {
                            val thinkingBuilder = StringBuilder()
                            var finalHtmlContent: String? = null
                            var textContent = ""
                            
                            // Check是否应该更新UI（当前会话与生成会话匹配）
                            fun shouldUpdateUI(): Boolean = currentSessionId == targetSessionId
                            
                            android.util.Log.d("HtmlCodingScreen", "Started event collector for session: $targetSessionId")
                            
                            service.eventFlow.collect { event ->
                                android.util.Log.d("HtmlCodingScreen", "Received event: ${event::class.simpleName} for session: $targetSessionId")
                                when (event) {
                                    is HtmlAgentEvent.StateChange -> {
                                        if (shouldUpdateUI()) {
                                            when (event.state) {
                                                HtmlAgentState.GENERATING -> chatState = ChatState.Streaming("")
                                                HtmlAgentState.COMPLETED -> { /* 稍后处理 */ }
                                                HtmlAgentState.ERROR -> { /* 错误在Error事件中处理 */ }
                                                HtmlAgentState.IDLE -> { /* 空闲 */ }
                                            }
                                        }
                                    }
                                    is HtmlAgentEvent.TextDelta -> {
                                        textContent = event.accumulated
                                        if (shouldUpdateUI()) {
                                            streamingContent = event.accumulated
                                        }
                                    }
                                    is HtmlAgentEvent.ThinkingDelta -> {
                                        thinkingBuilder.clear()
                                        thinkingBuilder.append(event.accumulated)
                                        if (shouldUpdateUI()) {
                                            streamingThinking = event.accumulated
                                        }
                                    }
                                    is HtmlAgentEvent.ToolCallStart -> {
                                        android.util.Log.d("HtmlCodingScreen", "Tool call started: ${event.toolName}")
                                    }
                                    is HtmlAgentEvent.CodeDelta -> {
                                        if (shouldUpdateUI()) {
                                            streamingContent = "${Strings.generatingCode}\n\n```html\n${event.accumulated}\n```"
                                        }
                                    }
                                    is HtmlAgentEvent.ToolExecuted -> {
                                        android.util.Log.d("HtmlCodingScreen", "Tool executed: ${event.result.toolName}, success=${event.result.success}")
                                        // 如果工具执行失败，显示错误信息（特别是图像生成失败）
                                        if (!event.result.success && shouldUpdateUI()) {
                                            val toolError = when (event.result.toolName) {
                                                "generate_image" -> "\uD83D\uDDBC\uFE0F ${Strings.imageGenerationFailed}: ${event.result.result}"
                                                else -> "\u26A0\uFE0F ${event.result.toolName}: ${event.result.result}"
                                            }
                                            streamingContent = toolError
                                        }
                                    }
                                    is HtmlAgentEvent.FileCreated -> {
                                        android.util.Log.d("HtmlCodingScreen", "File created: ${event.fileInfo.name}, version=${event.fileInfo.version}")
                                        // Refresh项目文件列表
                                        if (shouldUpdateUI()) {
                                            projectFiles = projectFileManager.listFiles(targetSessionId)
                                        }
                                    }
                                    is HtmlAgentEvent.HtmlComplete -> {
                                        finalHtmlContent = event.html
                                        android.util.Log.d("HtmlCodingScreen", "HTML complete, length=${event.html.length}")
                                    }
                                    is HtmlAgentEvent.AutoPreview -> { }
                                    is HtmlAgentEvent.ImageGenerating -> {
                                        if (shouldUpdateUI()) {
                                            chatState = ChatState.GeneratingImage(event.prompt)
                                        }
                                    }
                                    is HtmlAgentEvent.ImageGenerated -> {
                                        android.util.Log.d("HtmlCodingScreen", "Image generated for: ${event.prompt}")
                                    }
                                    is HtmlAgentEvent.Completed -> {
                                        val finalTextContent = event.textContent.ifBlank { textContent }
                                        val thinkingContent = thinkingBuilder.toString()
                                        
                                        val codeBlocks = if (finalHtmlContent != null) {
                                            listOf(CodeBlock(
                                                language = "html",
                                                content = finalHtmlContent!!,
                                                filename = "index.html",
                                                isComplete = true
                                            ))
                                        } else {
                                            emptyList()
                                        }
                                        
                                        // 如果没有文本内容也没有代码，说明 AI 没有返回有效响应
                                        // Show详细的调试信息帮助用户诊断问题
                                        val messageContent = when {
                                            finalTextContent.isNotBlank() -> finalTextContent
                                            codeBlocks.isNotEmpty() -> Strings.codeGenerated
                                            else -> {
                                                val debugInfo = buildString {
                                                    appendLine(Strings.aiNoValidResponse)
                                                    appendLine()
                                                    appendLine(Strings.debugInfo)
                                                    appendLine("• ${Strings.textContent}: ${if (event.textContent.isBlank()) Strings.emptyText else "'${event.textContent.take(100)}...'"}")
                                                    appendLine("• ${Strings.streamContent}: ${if (textContent.isBlank()) Strings.emptyText else "'${textContent.take(100)}...'"}")
                                                    appendLine("• ${Strings.thinkingContent}: ${if (thinkingContent.isBlank()) Strings.emptyText else "'${thinkingContent.take(100)}...'"}")
                                                    appendLine("• ${Strings.htmlCode}: ${if (finalHtmlContent == null) Strings.emptyText else "${finalHtmlContent!!.length}${Strings.characters}"}")
                                                    appendLine()
                                                    appendLine(Strings.possibleReasons)
                                                    appendLine(Strings.apiFormatIncompatible)
                                                    appendLine(Strings.modelNotSupported)
                                                    appendLine(Strings.apiKeyQuotaInsufficient)
                                                    appendLine()
                                                    appendLine(Strings.suggestionChangeModel)
                                                }
                                                android.util.Log.w("HtmlCodingScreen", debugInfo)
                                                debugInfo
                                            }
                                        }
                                        
                                        val aiMessage = HtmlCodingMessage(
                                            role = MessageRole.ASSISTANT,
                                            content = messageContent,
                                            thinking = thinkingContent.takeIf { it.isNotBlank() },
                                            codeBlocks = codeBlocks
                                        )
                                        
                                        // 无论当前显示哪个会话，都要保存消息到目标会话
                                        val finalSession = storage.addMessage(targetSessionId, aiMessage)
                                        
                                        // 只有当前显示的是目标会话时才更新UI
                                        if (shouldUpdateUI()) {
                                            currentSession = finalSession
                                        }
                                        
                                        if (codeBlocks.isNotEmpty()) {
                                            storage.addToCodeLibrary(
                                                sessionId = targetSessionId,
                                                messageId = aiMessage.id,
                                                userPrompt = userMessage.content,
                                                codeBlocks = codeBlocks,
                                                conversationContext = session.messages.takeLast(3).joinToString("\n") { 
                                                    "${it.role}: ${it.content.take(100)}" 
                                                }
                                            )
                                        }
                                        
                                        storage.createConversationCheckpoint(
                                            sessionId = targetSessionId,
                                            name = Strings.conversationCheckpoint.replace("%d", "${(finalSession?.messages?.size ?: 0) / 2}")
                                        )
                                        
                                        // Cleanup生成状态
                                        if (generatingSessionId == targetSessionId) {
                                            generatingSessionId = null
                                        }
                                        
                                        // 只有当前显示的是目标会话时才清空流式状态
                                        if (shouldUpdateUI()) {
                                            streamingContent = ""
                                            streamingThinking = ""
                                            chatState = ChatState.Idle
                                        }
                                        
                                        // 解绑服务
                                        if (isServiceBound && currentServiceConnection != null) {
                                            try {
                                                context.unbindService(currentServiceConnection!!)
                                                isServiceBound = false
                                                currentServiceConnection = null
                                                android.util.Log.d("HtmlCodingScreen", "Service unbound after completion")
                                            } catch (e: Exception) {
                                                android.util.Log.e("HtmlCodingScreen", "Error unbinding service", e)
                                            }
                                        }
                                    }
                                    is HtmlAgentEvent.Error -> {
                                        // Cleanup生成状态
                                        if (generatingSessionId == targetSessionId) {
                                            generatingSessionId = null
                                        }
                                        
                                        // 将错误消息保存到会话中，方便用户查看和复制
                                        val errorMessage = HtmlCodingMessage(
                                            role = MessageRole.ASSISTANT,
                                            content = "❌ ${Strings.errorPrefix}: ${event.message}",
                                            thinking = null,
                                            codeBlocks = emptyList()
                                        )
                                        val updatedSession = storage.addMessage(targetSessionId, errorMessage)
                                        
                                        // 只有当前显示的是目标会话时才更新UI
                                        if (shouldUpdateUI()) {
                                            currentSession = updatedSession
                                            chatState = ChatState.Idle
                                            streamingContent = ""
                                            streamingThinking = ""
                                        } else {
                                            // 后台会话出错，显示通知
                                            android.util.Log.w("HtmlCodingScreen", "Background session $targetSessionId error: ${event.message}")
                                        }
                                        
                                        // 解绑服务
                                        if (isServiceBound && currentServiceConnection != null) {
                                            try {
                                                context.unbindService(currentServiceConnection!!)
                                                isServiceBound = false
                                                currentServiceConnection = null
                                                android.util.Log.d("HtmlCodingScreen", "Service unbound after error")
                                            } catch (e: Exception) {
                                                android.util.Log.e("HtmlCodingScreen", "Error unbinding service", e)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Start生成
                        service.startGeneration(
                            requirement = sentText,
                            currentHtml = currentHtml,
                            sessionConfig = config,
                            model = textModel,
                            sessionId = targetSessionId  // 传递会话ID用于文件操作
                        )
                    }
                    
                    override fun onServiceDisconnected(name: ComponentName?) {
                        isServiceBound = false
                        currentServiceConnection = null
                    }
                }
                
                // Save服务连接引用
                currentServiceConnection = serviceConnection
                context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                
            } catch (e: Exception) {
                chatState = ChatState.Error(e.message ?: Strings.sendFailed)
                Toast.makeText(context, "${Strings.errorPrefix}: ${e.message}", Toast.LENGTH_LONG).show()
                chatState = ChatState.Idle
            }
        }
    }
    
    // 预览HTML
    fun previewHtml(codeBlock: CodeBlock) {
        scope.launch {
            try {
                // 直接使用代码块的内容，不再合并
                val htmlContent = codeBlock.content
                
                val file = storage.saveForPreview(htmlContent)
                
                // Start预览Activity
                val intent = Intent(context, HtmlPreviewActivity::class.java).apply {
                    putExtra(HtmlPreviewActivity.EXTRA_FILE_PATH, file.absolutePath)
                    putExtra(HtmlPreviewActivity.EXTRA_TITLE, codeBlock.filename?.takeIf { it.isNotBlank() } ?: Strings.preview)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "${Strings.previewFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 预览项目文件
    fun previewProjectFile(fileInfo: ProjectFileInfo) {
        scope.launch {
            try {
                val intent = Intent(context, HtmlPreviewActivity::class.java).apply {
                    putExtra(HtmlPreviewActivity.EXTRA_FILE_PATH, fileInfo.path)
                    putExtra(HtmlPreviewActivity.EXTRA_TITLE, fileInfo.name)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "${Strings.previewFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Select项目文件并加载内容
    fun selectProjectFile(fileInfo: ProjectFileInfo) {
        scope.launch {
            selectedProjectFile = fileInfo
            currentSessionId?.let { sessionId ->
                selectedFileContent = projectFileManager.readFile(sessionId, fileInfo.name)
            }
        }
    }
    
    // Download代码到本地（下载目录）
    fun downloadCode(codeBlock: CodeBlock) {
        scope.launch {
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val filename = codeBlock.filename?.takeIf { it.isNotBlank() } ?: "code.${codeBlock.language}"
                val file = File(downloadDir, filename)
                
                // 如果文件已存在，添加时间戳
                val actualFile = if (file.exists()) {
                    val timestamp = System.currentTimeMillis()
                    val nameWithoutExt = filename.substringBeforeLast(".")
                    val ext = filename.substringAfterLast(".", "")
                    File(downloadDir, "${nameWithoutExt}_$timestamp.$ext")
                } else {
                    file
                }
                
                actualFile.writeText(codeBlock.content)
                Toast.makeText(context, Strings.savedToPath.replace("%s", actualFile.absolutePath), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "${Strings.downloadFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Export所有代码到HTML项目
    fun exportAllToHtmlProject() {
        scope.launch {
            try {
                val allCodeBlocks = currentSession?.messages
                    ?.flatMap { it.codeBlocks }
                    ?: emptyList()
                
                if (allCodeBlocks.isEmpty()) {
                    Toast.makeText(context, Strings.noCodeToExport, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // 转换为 ProjectFile 列表
                val files = allCodeBlocks.map { block ->
                    val filename = block.filename?.takeIf { it.isNotBlank() } ?: when (block.language) {
                        "html" -> "index.html"
                        "css" -> "style.css"
                        "js" -> "script.js"
                        else -> "file.${block.language}"
                    }
                    val type = when (block.language) {
                        "html" -> ProjectFileType.HTML
                        "css" -> ProjectFileType.CSS
                        "js" -> ProjectFileType.JS
                        else -> ProjectFileType.OTHER
                    }
                    ProjectFile(filename, block.content, type)
                }
                
                // 使用回调导出到HTML项目
                if (onExportToHtmlProject != null) {
                    val projectName = currentSession?.title?.take(20) ?: Strings.aiGeneratedProject
                    onExportToHtmlProject.invoke(files, projectName)
                    Toast.makeText(context, Strings.exportedToHtmlProject, Toast.LENGTH_SHORT).show()
                } else {
                    // 如果没有回调，保存到本地项目目录
                    val projectName = currentSession?.title?.take(20) ?: "${Strings.aiGeneratedProject}_${System.currentTimeMillis()}"
                    val result = storage.saveProject(
                        SaveConfig(
                            directory = storage.getProjectsDir().absolutePath,
                            projectName = projectName,
                            createFolder = true,
                            overwrite = true
                        ),
                        files
                    )
                    result.onSuccess { dir ->
                        Toast.makeText(context, Strings.savedToPath.replace("%s", dir.absolutePath), Toast.LENGTH_LONG).show()
                    }.onFailure { e ->
                        Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Create新会话
    fun createNewSession() {
        scope.launch {
            val session = storage.createSession()
            currentSession = session
            showDrawer = false
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentSession?.title ?: Strings.htmlCodingAssistant,
                            style = MaterialTheme.typography.titleMedium
                        )
                        currentSession?.let {
                            Text(
                                Strings.messagesCount.replace("%d", "${it.messages.size}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    // 代码库
                    IconButton(onClick = { showCodeLibrarySheet = true }) {
                        Icon(Icons.Outlined.Folder, Strings.codeLibrary)
                    }
                    // Check点回退
                    IconButton(onClick = { showConversationCheckpointsSheet = true }) {
                        Icon(Icons.Outlined.Restore, Strings.rollback)
                    }
                    // 模板
                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(Icons.Outlined.Palette, Strings.templates)
                    }
                    // Set
                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Outlined.Settings, Strings.settings)
                    }
                    // 侧边栏
                    IconButton(onClick = { showDrawer = true }) {
                        Icon(Icons.Default.Menu, Strings.sessionList)
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
            // Load状态显示
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            Strings.msgLoading,
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
                // 主内容区域 - 使用 Box 叠加消息列表和文件面板
                Box(modifier = Modifier.fillMaxSize()) {
                    // 消息列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (projectFiles.isNotEmpty() && isFilesPanelExpanded) 200.dp else 60.dp),
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
                                    Toast.makeText(context, Strings.msgCopied, Toast.LENGTH_SHORT).show()
                                },
                                onDownloadCode = { codeBlock -> downloadCode(codeBlock) },
                                onExportToProject = { exportAllToHtmlProject() }
                            )
                        }
                        
                        
                        // 流式输出显示（仅在收到首个分片后渲染）
                        if (streamingContent.isNotEmpty() || streamingThinking.isNotEmpty()) {
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
                    
                    // 项目文件面板 - 底部固定
                    if (currentSession != null) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            // File预览面板（选中文件时显示）
                            AnimatedVisibility(
                                visible = selectedProjectFile != null && selectedFileContent != null,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                FilePreviewPanel(
                                    file = selectedProjectFile,
                                    content = selectedFileContent,
                                    onClose = {
                                        selectedProjectFile = null
                                        selectedFileContent = null
                                    },
                                    onPreview = {
                                        selectedProjectFile?.let { previewProjectFile(it) }
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            
                            // 项目文件列表面板
                            ProjectFilesPanel(
                                files = projectFiles,
                                selectedFile = selectedProjectFile,
                                onFileClick = { fileInfo -> selectProjectFile(fileInfo) },
                                onPreviewClick = { fileInfo -> previewProjectFile(fileInfo) },
                                onRefresh = { refreshProjectFiles() },
                                isExpanded = isFilesPanelExpanded,
                                onExpandChange = { isFilesPanelExpanded = it }
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
    
    // Configure底部弹窗
    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false }
        ) {
            // 如果没有当前会话，先创建一个
            val sessionForConfig = currentSession ?: run {
                // 使用 LaunchedEffect 创建会话
                var newSession by remember { mutableStateOf<HtmlCodingSession?>(null) }
                LaunchedEffect(Unit) {
                    newSession = storage.createSession()
                    currentSession = newSession
                }
                newSession
            }
            
            sessionForConfig?.let { session ->
                ConfigPanel(
                    config = session.config,
                    onConfigChange = { newConfig ->
                        scope.launch {
                            val updatedSession = session.copy(config = newConfig, updatedAt = System.currentTimeMillis())
                            storage.updateSession(updatedSession)
                            currentSession = updatedSession
                        }
                    },
                    textModels = textModels,
                    imageModels = imageModels,
                    rulesTemplates = HtmlCodingPrompts.rulesTemplates,
                    onNavigateToAiSettings = {
                        showConfigSheet = false
                        onNavigateToAiSettings()
                    },
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } ?: run {
                // 正在创建会话，显示加载状态
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
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
    
    // Version检查点弹窗
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
                                // Reset聊天状态，确保回退后可以正常发送消息
                                chatState = ChatState.Idle
                                streamingContent = ""
                                streamingThinking = ""
                                // 清除正在生成的会话ID，允许新的消息发送
                                generatingSessionId = null
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
                            storage.createCheckpoint(session.id, Strings.manualSave.replace("%d", "${session.checkpoints.size + 1}"))
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
                            // Reset聊天状态，确保编辑后可以正常发送消息
                            chatState = ChatState.Idle
                            streamingContent = ""
                            streamingThinking = ""
                        }
                    }
                    editingMessage = null
                }
            }
        )
    }
    
    // Save项目对话框
    if (showSaveDialog) {
        SaveProjectDialog(
            storage = storage,
            files = currentSession?.checkpoints?.lastOrNull()?.files ?: emptyList(),
            onDismiss = { showSaveDialog = false },
            onSaved = { path ->
                Toast.makeText(context, Strings.savedToPath.replace("%s", path), Toast.LENGTH_LONG).show()
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
                    inputText = "${Strings.continueDevBasedOnCode}\n${item.userPrompt}"
                    showCodeLibrarySheet = false
                },
                onExportToProject = { item ->
                    // Export到项目库
                    scope.launch {
                        val result = storage.exportToProjectLibrary(item, item.title.take(20))
                        if (result.isSuccess) {
                            Toast.makeText(context, Strings.exportedToProjectLibrary, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "${Strings.exportFailed}: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
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
                        android.util.Log.d("HtmlCodingScreen", "Rolling back to checkpoint: ${checkpoint.id}, name: ${checkpoint.name}")
                        android.util.Log.d("HtmlCodingScreen", "Before rollback: chatState=$chatState, generatingSessionId=$generatingSessionId")
                        
                        storage.rollbackToConversationCheckpoint(checkpoint.id)?.let { restoredSession ->
                            android.util.Log.d("HtmlCodingScreen", "Rollback successful, restoredSession: ${restoredSession.id}, messages: ${restoredSession.messages.size}")
                            
                            // Reset聊天状态，确保回退后可以正常发送消息
                            chatState = ChatState.Idle
                            streamingContent = ""
                            streamingThinking = ""
                            // 清除正在生成的会话ID，允许新的消息发送
                            generatingSessionId = null
                            
                            // Check最后一条消息是否是用户消息
                            val lastMessage = restoredSession.messages.lastOrNull()
                            android.util.Log.d("HtmlCodingScreen", "Last message role: ${lastMessage?.role}, content: ${lastMessage?.content?.take(50)}")
                            
                            if (lastMessage?.role == MessageRole.USER) {
                                // 将最后一条用户消息填入输入框，方便用户重新发送
                                inputText = lastMessage.content
                                attachedImages = lastMessage.images
                                android.util.Log.d("HtmlCodingScreen", "Set inputText to: ${inputText.take(50)}")
                                
                                // 重要：从会话中移除这条用户消息，避免重复添加
                                // User点击发送时会重新添加这条消息
                                val sessionWithoutLastUserMessage = restoredSession.copy(
                                    messages = restoredSession.messages.dropLast(1),
                                    updatedAt = System.currentTimeMillis() + 1000 // 确保时间戳更新，防止被 Flow 覆盖
                                )
                                currentSession = sessionWithoutLastUserMessage
                                // 同步更新存储
                                storage.updateSession(sessionWithoutLastUserMessage)
                                android.util.Log.d("HtmlCodingScreen", "Updated currentSession, messages: ${sessionWithoutLastUserMessage.messages.size}")
                                
                                Toast.makeText(context, Strings.rolledBackWithInputHint.replace("%s", checkpoint.name), Toast.LENGTH_LONG).show()
                            } else {
                                currentSession = restoredSession
                                Toast.makeText(context, Strings.rolledBackTo.replace("%s", checkpoint.name), Toast.LENGTH_SHORT).show()
                            }
                            
                            android.util.Log.d("HtmlCodingScreen", "After rollback: chatState=$chatState, inputText='${inputText.take(50)}'")
                        } ?: run {
                            android.util.Log.e("HtmlCodingScreen", "Rollback failed: rollbackToConversationCheckpoint returned null")
                            Toast.makeText(context, Strings.rollbackFailed, Toast.LENGTH_SHORT).show()
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
            Strings.htmlCodingAssistant,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.aiHelpsGenerateWebpage,
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
            Text(Strings.startNewConversation, style = MaterialTheme.typography.titleMedium)
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
                Text(Strings.templates)
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
                Text(Strings.tutorial)
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
                Strings.quickStart,
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
            Strings.aiThinking,
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
                Strings.generatingImage,
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
            Text(Strings.conversationHistory, style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, Strings.close)
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
            Text(Strings.newConversation)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        
        // Session列表
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
                        Strings.noConversationRecords,
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
                Strings.selectStyleTemplate, 
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
                        text = Strings.selected,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.selectTemplateHint,
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
                text = { Text(Strings.designTemplates) },
                icon = { Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(Strings.styleReferences) },
                icon = { Icon(Icons.Outlined.Style, null, modifier = Modifier.size(18.dp)) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedTab) {
            0 -> {
                // 模板网格 - 使用 LazyVerticalGrid 更好展示
                Text(
                    Strings.totalTemplates.replace("%d", "${templates.size}"),
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
                    Strings.totalStyleReferences.replace("%d", "${styles.size}"),
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
                Strings.usageTutorial, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                Strings.chapters.replace("%d", "${chapters.size}"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedChapterId == null) {
            // 章节列表
            if (chapters.isEmpty()) {
                // Empty状态提示
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        Strings.noTutorialContent,
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
                                        Strings.sections.replace("%d", "${chapter.sections.size}"),
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
                                Strings.codeExample,
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
                                            Strings.tips,
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
            Text(Strings.versionManagement, style = MaterialTheme.typography.titleLarge)
            Row {
                FilledTonalButton(onClick = onCreateCheckpoint) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.saveVersion)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSaveProject) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.export)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (checkpoints.isEmpty()) {
            Text(
                Strings.noSavedVersions,
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
        title = { Text(Strings.editMessage) },
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
                        Strings.imagesCount.replace("%d", "${editedImages.size}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    Strings.editWarning,
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
                Text(Strings.resend)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
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
        title = { Text(Strings.saveProject) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(Strings.projectName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(Strings.saveLocation, style = MaterialTheme.typography.labelMedium)
                
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
                    Text(Strings.createProjectFolder)
                }
                
                Text(
                    Strings.willSaveFiles.replace("%d", "${files.size}"),
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
                        // Error handling在调用处
                    }
                },
                enabled = projectName.isNotBlank() && files.isNotEmpty()
            ) {
                Text(Strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
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
                Strings.codeLibrary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                FilterChip(
                    selected = filterFavorites,
                    onClick = { filterFavorites = !filterFavorites },
                    label = { Text(Strings.favorites) },
                    leadingIcon = if (filterFavorites) {
                        { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.aiCodeAutoSaved,
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
                        if (filterFavorites) Strings.noFavorites else Strings.codeLibraryEmpty,
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
            
            // File标签
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
                    Text(Strings.preview, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onUseContent,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.use, style = MaterialTheme.typography.labelMedium)
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
                            text = { Text(if (item.isFavorite) Strings.unfavorite else Strings.favorite) },
                            onClick = { onToggleFavorite(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    if (item.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                    null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.exportToProjectLibrary) },
                            onClick = { onExportToProject(); showMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.FolderCopy, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.delete, color = Color(0xFFE53935)) },
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
            Strings.conversationCheckpoints,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.rollbackHint,
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
                    Text(Strings.noCheckpoints, color = MaterialTheme.colorScheme.outline)
                    Text(
                        Strings.autoCreateCheckpointHint,
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
                                            Strings.messagesCount.replace("%d", "${checkpoint.messageCount}"),
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
                                                Strings.codesCount.replace("%d", "${checkpoint.codeLibraryIds.size}"),
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
                                        Strings.rollback,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDelete(checkpoint) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        Strings.delete,
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
