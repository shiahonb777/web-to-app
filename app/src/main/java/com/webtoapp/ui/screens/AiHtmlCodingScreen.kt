package com.webtoapp.ui.screens

import android.content.ComponentName
import com.webtoapp.core.logging.AppLogger
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.AiGenerationService
import com.webtoapp.core.ai.coding.*
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.AiFeature
import com.webtoapp.ui.components.coding.*
import com.webtoapp.ui.codepreview.HtmlPreviewActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import java.io.File
import androidx.compose.ui.graphics.Color

/**
 * HTML AI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHtmlCodingScreen(
    onBack: () -> Unit,
    onExportToHtmlProject: ((List<ProjectFile>, String) -> Unit)? = null,  // Export HTMLitem
    onNavigateToAiSettings: () -> Unit = {}  // AIsettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // manager
    val storage = remember { AiCodingStorage(context) }
    val configManager = remember { AiConfigManager(context) }
    // state- try- catch
    var isLoading by remember { mutableStateOf(true) }
    
    val sessions by storage.sessionsFlow.collectAsState(initial = emptyList())
    val currentSessionId by storage.currentSessionIdFlow.collectAsState(initial = null)
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())
    
    // load
    LaunchedEffect(sessions) {
        isLoading = false
    }
    
    // filter- filter
    val textModels = savedModels.filter { it.supportsFeature(AiFeature.AI_CODING) }
    val imageModels = savedModels.filter { it.supportsFeature(AiFeature.AI_CODING_IMAGE) }
    
    // currentsession
    var currentSession by remember { mutableStateOf<AiCodingSession?>(null) }
    
    // from sessions Flow sync currentSession
    // when sessions update sync, local
    LaunchedEffect(currentSessionId, sessions) {
        val sessionFromFlow = sessions.find { it.id == currentSessionId }
        // when Flow insession localupdate sync
        if (sessionFromFlow != null) {
            val shouldUpdate = currentSession == null || 
                currentSession?.id != sessionFromFlow.id ||
                sessionFromFlow.updatedAt > (currentSession?.updatedAt ?: 0)
            
            AppLogger.d("AiHtmlCodingScreen", "LaunchedEffect: sessionFromFlow=${sessionFromFlow.id}, currentSession=${currentSession?.id}, shouldUpdate=$shouldUpdate, flowUpdatedAt=${sessionFromFlow.updatedAt}, localUpdatedAt=${currentSession?.updatedAt}")
            
            if (shouldUpdate) {
                AppLogger.d("AiHtmlCodingScreen", "LaunchedEffect: Updating currentSession from Flow")
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
    
    // current sessionID, for session output
    var generatingSessionId by remember { mutableStateOf<String?>(null) }
    
    // itemfilerelatedstate
    val projectFileManager = remember { ProjectFileManager(context) }
    var projectFiles by remember { mutableStateOf<List<ProjectFileInfo>>(emptyList()) }
    var selectedProjectFile by remember { mutableStateOf<ProjectFileInfo?>(null) }
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var isFilesPanelExpanded by remember { mutableStateOf(true) }
    
    // whensession refreshitemfilelist
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
    
    // Refreshitemfilelist
    fun refreshProjectFiles() {
        currentSessionId?.let { sessionId ->
            projectFiles = projectFileManager.listFiles(sessionId)
        }
    }
    
    // current Job, for
    var eventCollectorJob by remember { mutableStateOf<Job?>(null) }
    
    // current, for
    var currentServiceConnection by remember { mutableStateOf<ServiceConnection?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }
    
    // Sessionswitch, currentsession state( )
    LaunchedEffect(currentSessionId) {
        // ifswitch session session, display
        if (currentSessionId != generatingSessionId) {
            streamingContent = ""
            streamingThinking = ""
            // Reset state( UI) , run
            if (chatState is ChatState.Streaming || chatState is ChatState.GeneratingImage) {
                chatState = ChatState.Idle
            }
            AppLogger.d("AiHtmlCodingScreen", "Session switched: cleared streaming state, currentSessionId=$currentSessionId, generatingSessionId=$generatingSessionId")
        }
    }
    
    var showDrawer by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showTutorialSheet by remember { mutableStateOf(false) }
    var showCheckpointsSheet by remember { mutableStateOf(false) }
    var showCodeLibrarySheet by remember { mutableStateOf(false) }
    var showConversationCheckpointsSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<AiCodingMessage?>(null) }
    
    // Imageselect
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
    
    // Listscrollstate
    val listState = rememberLazyListState()
    
    // Autoscroll bottom
    LaunchedEffect(currentSession?.messages?.size) {
        currentSession?.messages?.size?.let { size ->
            if (size > 0) {
                listState.animateScrollToItem(size - 1)
            }
        }
    }
    
    // message- run
    fun sendMessage() {
        AppLogger.d("AiHtmlCodingScreen", "sendMessage called: inputText='${inputText.take(50)}', chatState=$chatState, currentSession=${currentSession?.id}, generatingSessionId=$generatingSessionId")
        
        if (inputText.isBlank()) {
            AppLogger.d("AiHtmlCodingScreen", "sendMessage: inputText is blank, returning")
            return
        }
        if (chatState !is ChatState.Idle) {
            AppLogger.d("AiHtmlCodingScreen", "sendMessage: chatState is not Idle ($chatState), returning")
            return
        }
        
        scope.launch {
            // if currentsession, create session
            val session = currentSession ?: run {
                val newSession = storage.createSession()
                currentSession = newSession
                newSession
            }
            val config = session.config
            
            // Check config
            if (config.textModelId == null) {
                Toast.makeText(context, AppStringsProvider.current().pleaseSelectTextModel, Toast.LENGTH_SHORT).show()
                showConfigSheet = true
                return@launch
            }
            
            // Createusermessage
            val userMessage = AiCodingMessage(
                role = MessageRole.USER,
                content = inputText,
                images = attachedImages
            )
            
            // session
            val updatedSession = storage.addMessage(session.id, userMessage)
            currentSession = updatedSession
            
            // input
            val sentText = inputText
            inputText = ""
            attachedImages = emptyList()
            
            // state
            chatState = ChatState.Streaming("")
            
            // sessionID
            val targetSessionId = session.id
            generatingSessionId = targetSessionId
            
            try {
                // Get
                val textModel = savedModels.find { it.id == config.textModelId }
                if (textModel == null) {
                    throw Exception(AppStringsProvider.current().modelConfigInvalid)
                }
                
                // SetcurrentHTML( foredit_html)
                val currentHtml = updatedSession?.messages
                    ?.flatMap { it.codeBlocks }
                    ?.lastOrNull { it.language == "html" }
                    ?.content
                
                // Reset state
                streamingContent = ""
                streamingThinking = ""
                
                // Start
                val serviceIntent = Intent(context, AiGenerationService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
                
                // Cancel Job
                eventCollectorJob?.cancel()
                eventCollectorJob = null
                AppLogger.d("AiHtmlCodingScreen", "Cancelled previous event collector job")
                
                // ( if)
                if (isServiceBound && currentServiceConnection != null) {
                    try {
                        context.unbindService(currentServiceConnection!!)
                        AppLogger.d("AiHtmlCodingScreen", "Unbound previous service connection")
                    } catch (e: Exception) {
                        AppLogger.e("AiHtmlCodingScreen", "Error unbinding previous service", e)
                    }
                    isServiceBound = false
                    currentServiceConnection = null
                }
                
                // and
                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val service = (binder as AiGenerationService.LocalBinder).getService()
                        isServiceBound = true
                        
                        // Listen- save Job
                        eventCollectorJob = scope.launch {
                            val thinkingBuilder = StringBuilder()
                            var finalHtmlContent: String? = null
                            var textContent = ""
                            
                            // Check updateUI( currentsessionwith session)
                            fun shouldUpdateUI(): Boolean = currentSessionId == targetSessionId
                            
                            AppLogger.d("AiHtmlCodingScreen", "Started event collector for session: $targetSessionId")
                            
                            service.eventFlow.collect { event ->
                                AppLogger.d("AiHtmlCodingScreen", "Received event: ${event::class.simpleName} for session: $targetSessionId")
                                when (event) {
                                    is HtmlAgentEvent.StateChange -> {
                                        if (shouldUpdateUI()) {
                                            when (event.state) {
                                                HtmlAgentState.GENERATING -> chatState = ChatState.Streaming("")
                                                HtmlAgentState.COMPLETED -> { /* handle */ }
                                                HtmlAgentState.ERROR -> { /* error Error handle */ }
                                                HtmlAgentState.IDLE -> { /* Note */ }
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
                                        AppLogger.d("AiHtmlCodingScreen", "Tool call started: ${event.toolName}")
                                    }
                                    is HtmlAgentEvent.CodeDelta -> {
                                        if (shouldUpdateUI()) {
                                            streamingContent = "${AppStringsProvider.current().generatingCode}\n\n```html\n${event.accumulated}\n```"
                                        }
                                    }
                                    is HtmlAgentEvent.ToolExecuted -> {
                                        AppLogger.d("AiHtmlCodingScreen", "Tool executed: ${event.result.toolName}, success=${event.result.success}")
                                        // if executefailed, displayerror( failed)
                                        if (!event.result.success && shouldUpdateUI()) {
                                            val toolError = when (event.result.toolName) {
                                                "generate_image" -> "\uD83D\uDDBC\uFE0F ${AppStringsProvider.current().imageGenerationFailed}: ${event.result.result}"
                                                else -> "\u26A0\uFE0F ${event.result.toolName}: ${event.result.result}"
                                            }
                                            streamingContent = toolError
                                        }
                                    }
                                    is HtmlAgentEvent.FileCreated -> {
                                        AppLogger.d("AiHtmlCodingScreen", "File created: ${event.fileInfo.name}, version=${event.fileInfo.version}")
                                        // Refreshitemfilelist
                                        if (shouldUpdateUI()) {
                                            projectFiles = projectFileManager.listFiles(targetSessionId)
                                        }
                                    }
                                    is HtmlAgentEvent.HtmlComplete -> {
                                        finalHtmlContent = event.html
                                        AppLogger.d("AiHtmlCodingScreen", "HTML complete, length=${event.html.length}")
                                    }
                                    is HtmlAgentEvent.AutoPreview -> { }
                                    is HtmlAgentEvent.ImageGenerating -> {
                                        if (shouldUpdateUI()) {
                                            chatState = ChatState.GeneratingImage(event.prompt)
                                        }
                                    }
                                    is HtmlAgentEvent.ImageGenerated -> {
                                        AppLogger.d("AiHtmlCodingScreen", "Image generated for: ${event.prompt}")
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
                                        
                                        // if textcontent code, AI back
                                        // Show helpuser
                                        val messageContent = when {
                                            finalTextContent.isNotBlank() -> finalTextContent
                                            codeBlocks.isNotEmpty() -> AppStringsProvider.current().codeGenerated
                                            else -> {
                                                val debugInfo = buildString {
                                                    appendLine(AppStringsProvider.current().aiNoValidResponse)
                                                    appendLine()
                                                    appendLine(AppStringsProvider.current().debugInfo)
                                                    appendLine("• ${AppStringsProvider.current().textContent}: ${if (event.textContent.isBlank()) AppStringsProvider.current().emptyText else "'${event.textContent.take(100)}...'"}")
                                                    appendLine("• ${AppStringsProvider.current().streamContent}: ${if (textContent.isBlank()) AppStringsProvider.current().emptyText else "'${textContent.take(100)}...'"}")
                                                    appendLine("• ${AppStringsProvider.current().thinkingContent}: ${if (thinkingContent.isBlank()) AppStringsProvider.current().emptyText else "'${thinkingContent.take(100)}...'"}")
                                                    appendLine("• ${AppStringsProvider.current().htmlCode}: ${if (finalHtmlContent == null) AppStringsProvider.current().emptyText else "${finalHtmlContent!!.length}${AppStringsProvider.current().characters}"}")
                                                    appendLine()
                                                    appendLine(AppStringsProvider.current().possibleReasons)
                                                    appendLine(AppStringsProvider.current().apiFormatIncompatible)
                                                    appendLine(AppStringsProvider.current().modelNotSupported)
                                                    appendLine(AppStringsProvider.current().apiKeyQuotaInsufficient)
                                                    appendLine()
                                                    appendLine(AppStringsProvider.current().suggestionChangeModel)
                                                }
                                                AppLogger.w("AiHtmlCodingScreen", debugInfo)
                                                debugInfo
                                            }
                                        }
                                        
                                        val aiMessage = AiCodingMessage(
                                            role = MessageRole.ASSISTANT,
                                            content = messageContent,
                                            thinking = thinkingContent.takeIf { it.isNotBlank() },
                                            codeBlocks = codeBlocks
                                        )
                                        
                                        // currentdisplay session, savemessage session
                                        val finalSession = storage.addMessage(targetSessionId, aiMessage)
                                        
                                        // currentdisplay session updateUI
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
                                            name = AppStringsProvider.current().conversationCheckpoint.replace("%d", "${(finalSession?.messages?.size ?: 0) / 2}")
                                        )
                                        
                                        // Cleanup state
                                        if (generatingSessionId == targetSessionId) {
                                            generatingSessionId = null
                                        }
                                        
                                        // currentdisplay session state
                                        if (shouldUpdateUI()) {
                                            streamingContent = ""
                                            streamingThinking = ""
                                            chatState = ChatState.Idle
                                        }
                                        
                                        // Note
                                        if (isServiceBound && currentServiceConnection != null) {
                                            try {
                                                context.unbindService(currentServiceConnection!!)
                                                isServiceBound = false
                                                currentServiceConnection = null
                                                AppLogger.d("AiHtmlCodingScreen", "Service unbound after completion")
                                            } catch (e: Exception) {
                                                AppLogger.e("AiHtmlCodingScreen", "Error unbinding service", e)
                                            }
                                        }
                                    }
                                    is HtmlAgentEvent.Error -> {
                                        // Cleanup state
                                        if (generatingSessionId == targetSessionId) {
                                            generatingSessionId = null
                                        }
                                        
                                        // maperrormessagesave session, user
                                        val errorMessage = AiCodingMessage(
                                            role = MessageRole.ASSISTANT,
                                            content = "${AppStringsProvider.current().errorPrefix}: ${event.message}",
                                            thinking = null,
                                            codeBlocks = emptyList()
                                        )
                                        val updatedSession = storage.addMessage(targetSessionId, errorMessage)
                                        
                                        // currentdisplay session updateUI
                                        if (shouldUpdateUI()) {
                                            currentSession = updatedSession
                                            chatState = ChatState.Idle
                                            streamingContent = ""
                                            streamingThinking = ""
                                        } else {
                                            // session, display
                                            AppLogger.w("AiHtmlCodingScreen", "Background session $targetSessionId error: ${event.message}")
                                        }
                                        
                                        // Note
                                        if (isServiceBound && currentServiceConnection != null) {
                                            try {
                                                context.unbindService(currentServiceConnection!!)
                                                isServiceBound = false
                                                currentServiceConnection = null
                                                AppLogger.d("AiHtmlCodingScreen", "Service unbound after error")
                                            } catch (e: Exception) {
                                                AppLogger.e("AiHtmlCodingScreen", "Error unbinding service", e)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Start
                        service.startGeneration(
                            requirement = sentText,
                            currentHtml = currentHtml,
                            sessionConfig = config,
                            model = textModel,
                            sessionId = targetSessionId  // sessionIDforfile
                        )
                    }
                    
                    override fun onServiceDisconnected(name: ComponentName?) {
                        isServiceBound = false
                        currentServiceConnection = null
                    }
                }
                
                // Save
                currentServiceConnection = serviceConnection
                context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                
            } catch (e: Exception) {
                chatState = ChatState.Error(e.message ?: AppStringsProvider.current().sendFailed)
                Toast.makeText(context, "${AppStringsProvider.current().errorPrefix}: ${e.message}", Toast.LENGTH_LONG).show()
                chatState = ChatState.Idle
            }
        }
    }
    
    // previewHTML
    fun previewHtml(codeBlock: CodeBlock) {
        scope.launch {
            try {
                // code content, and
                val htmlContent = codeBlock.content
                
                val file = storage.saveForPreview(htmlContent)
                
                // StartpreviewActivity
                val intent = Intent(context, HtmlPreviewActivity::class.java).apply {
                    putExtra(HtmlPreviewActivity.EXTRA_FILE_PATH, file.absolutePath)
                    putExtra(HtmlPreviewActivity.EXTRA_TITLE, codeBlock.filename?.takeIf { it.isNotBlank() } ?: AppStringsProvider.current().preview)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "${AppStringsProvider.current().previewFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // previewitemfile
    fun previewProjectFile(fileInfo: ProjectFileInfo) {
        scope.launch {
            try {
                val intent = Intent(context, HtmlPreviewActivity::class.java).apply {
                    putExtra(HtmlPreviewActivity.EXTRA_FILE_PATH, fileInfo.path)
                    putExtra(HtmlPreviewActivity.EXTRA_TITLE, fileInfo.name)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "${AppStringsProvider.current().previewFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Selectitemfileandloadcontent
    fun selectProjectFile(fileInfo: ProjectFileInfo) {
        scope.launch {
            selectedProjectFile = fileInfo
            currentSessionId?.let { sessionId ->
                selectedFileContent = projectFileManager.readFile(sessionId, fileInfo.name)
            }
        }
    }
    
    // Downloadcode local( downloaddirectory)
    fun downloadCode(codeBlock: CodeBlock) {
        scope.launch {
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val filename = codeBlock.filename?.takeIf { it.isNotBlank() } ?: "code.${codeBlock.language}"
                val file = File(downloadDir, filename)
                
                // iffile,
                val actualFile = if (file.exists()) {
                    val timestamp = System.currentTimeMillis()
                    val nameWithoutExt = filename.substringBeforeLast(".")
                    val ext = filename.substringAfterLast(".", "")
                    File(downloadDir, "${nameWithoutExt}_$timestamp.$ext")
                } else {
                    file
                }
                
                actualFile.writeText(codeBlock.content)
                Toast.makeText(context, AppStringsProvider.current().savedToPath.replace("%s", actualFile.absolutePath), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "${AppStringsProvider.current().downloadFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Export code HTMLitem
    fun exportAllToHtmlProject() {
        scope.launch {
            try {
                val allCodeBlocks = currentSession?.messages
                    ?.flatMap { it.codeBlocks }
                    ?: emptyList()
                
                if (allCodeBlocks.isEmpty()) {
                    Toast.makeText(context, AppStringsProvider.current().noCodeToExport, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // ProjectFile list
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
                
                // export HTMLitem
                if (onExportToHtmlProject != null) {
                    val projectName = currentSession?.title?.take(20) ?: AppStringsProvider.current().aiGeneratedProject
                    onExportToHtmlProject.invoke(files, projectName)
                    Toast.makeText(context, AppStringsProvider.current().exportedToHtmlProject, Toast.LENGTH_SHORT).show()
                } else {
                    // if, save localitemdirectory
                    val projectName = currentSession?.title?.take(20) ?: "${AppStringsProvider.current().aiGeneratedProject}_${System.currentTimeMillis()}"
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
                        Toast.makeText(context, AppStringsProvider.current().savedToPath.replace("%s", dir.absolutePath), Toast.LENGTH_LONG).show()
                    }.onFailure { e ->
                        Toast.makeText(context, "${AppStringsProvider.current().exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${AppStringsProvider.current().exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Create session
    fun createNewSession() {
        scope.launch {
            val session = storage.createSession()
            currentSession = session
            showDrawer = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentSession?.title ?: AppStringsProvider.current().htmlCodingAssistant,
                            style = MaterialTheme.typography.titleMedium
                        )
                        currentSession?.let {
                            Text(
                                AppStringsProvider.current().messagesCount.replace("%d", "${it.messages.size}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showDrawer) {
                            showDrawer = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                actions = {
                    // repository
                    IconButton(onClick = { showCodeLibrarySheet = true }) {
                        Icon(Icons.Outlined.Folder, AppStringsProvider.current().codeLibrary)
                    }
                    // Check fallback
                    IconButton(onClick = { showConversationCheckpointsSheet = true }) {
                        Icon(Icons.Outlined.Restore, AppStringsProvider.current().rollback)
                    }
                    // Note
                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(Icons.Outlined.Palette, AppStringsProvider.current().templates)
                    }
                    // Set
                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Outlined.Settings, AppStringsProvider.current().settings)
                    }
                    // sidebar( switch)
                    IconButton(onClick = { showDrawer = !showDrawer }) {
                        Icon(Icons.Default.Menu, AppStringsProvider.current().sessionList)
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
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Load
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            AppStringsProvider.current().msgLoading,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (currentSession == null) {
                // Comment
                WelcomeContent(
                    onNewChat = { createNewSession() },
                    onSelectTemplate = { showTemplatesSheet = true },
                    onOpenTutorial = { showTutorialSheet = true },
                    quickPrompts = AiCodingTutorial.quickPrompts,
                    onQuickPrompt = { prompt ->
                        scope.launch {
                            val session = storage.createSession()
                            currentSession = session
                            inputText = prompt
                        }
                    }
                )
            } else {
                // - Box
                Box(modifier = Modifier.fillMaxSize()) {
                    // Comment
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
                                    Toast.makeText(context, AppStringsProvider.current().msgCopied, Toast.LENGTH_SHORT).show()
                                },
                                onDownloadCode = { codeBlock -> downloadCode(codeBlock) },
                                onExportToProject = { exportAllToHtmlProject() }
                            )
                        }
                        
                        
                        // Comment
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
                    
                    // -
                    if (currentSession != null) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            // File
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
                            
                            // Comment
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
    
    // -
    if (showDrawer) {
        // Comment
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    showDrawer = false
                }
        )
        // Comment
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
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
    }
    
    // Configure
    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false }
        ) {
            // Comment
            val sessionForConfig = currentSession ?: run {
                // LaunchedEffect
                var newSession by remember { mutableStateOf<AiCodingSession?>(null) }
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
                    rulesTemplates = AiCodingPrompts.rulesTemplates,
                    onNavigateToAiSettings = {
                        showConfigSheet = false
                        onNavigateToAiSettings()
                    },
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } ?: run {
                // Comment
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    // Comment
    if (showTemplatesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplatesSheet = false }
        ) {
            TemplatesSheetContent(
                templates = AiCodingPrompts.styleTemplates,
                styles = AiCodingPrompts.styleReferences,
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
    
    // Comment
    if (showTutorialSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTutorialSheet = false }
        ) {
            TutorialSheetContent(
                chapters = AiCodingTutorial.chapters,
                onDismiss = { showTutorialSheet = false }
            )
        }
    }
    
    // Version
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
                                // Reset
                                chatState = ChatState.Idle
                                streamingContent = ""
                                streamingThinking = ""
                                // ID
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
                            storage.createCheckpoint(session.id, AppStringsProvider.current().manualSave.replace("%d", "${session.checkpoints.size + 1}"))
                            currentSession = storage.getSession(session.id)
                        }
                    },
                    onSaveProject = { showSaveDialog = true },
                    onDismiss = { showCheckpointsSheet = false }
                )
            }
        }
    }
    
    // Comment
    editingMessage?.let { message ->
        EditMessageDialog(
            message = message,
            onDismiss = { editingMessage = null },
            onConfirm = { newContent, newImages ->
                scope.launch {
                    currentSession?.let { session ->
                        storage.editUserMessage(session.id, message.id, newContent, newImages)?.let {
                            currentSession = it
                            // Reset
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
    
    // Save
    if (showSaveDialog) {
        SaveProjectDialog(
            storage = storage,
            files = currentSession?.checkpoints?.lastOrNull()?.files ?: emptyList(),
            onDismiss = { showSaveDialog = false },
            onSaved = { path ->
                Toast.makeText(context, AppStringsProvider.current().savedToPath.replace("%s", path), Toast.LENGTH_LONG).show()
                showSaveDialog = false
            }
        )
    }
    
    // Comment
    if (showCodeLibrarySheet) {
        val codeLibrary by storage.codeLibraryFlow.collectAsState(initial = emptyList())
        
        ModalBottomSheet(
            onDismissRequest = { showCodeLibrarySheet = false }
        ) {
            CodeLibrarySheetContent(
                items = codeLibrary,
                onPreview = { item ->
                    // Comment
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
                    // Comment
                    inputText = "${AppStringsProvider.current().continueDevBasedOnCode}\n${item.userPrompt}"
                    showCodeLibrarySheet = false
                },
                onExportToProject = { item ->
                    // Export
                    scope.launch {
                        val result = storage.exportToProjectLibrary(item, item.title.take(20))
                        if (result.isSuccess) {
                            Toast.makeText(context, AppStringsProvider.current().exportedToProjectLibrary, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "${AppStringsProvider.current().exportFailed}: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
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
    
    // Comment
    // ModalBottomSheet LaunchedEffect
    var conversationCheckpoints by remember { mutableStateOf<List<ConversationCheckpoint>>(emptyList()) }
    
    // Comment
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
                        AppLogger.d("AiHtmlCodingScreen", "Rolling back to checkpoint: ${checkpoint.id}, name: ${checkpoint.name}")
                        AppLogger.d("AiHtmlCodingScreen", "Before rollback: chatState=$chatState, generatingSessionId=$generatingSessionId")
                        
                        storage.rollbackToConversationCheckpoint(checkpoint.id)?.let { restoredSession ->
                            AppLogger.d("AiHtmlCodingScreen", "Rollback successful, restoredSession: ${restoredSession.id}, messages: ${restoredSession.messages.size}")
                            
                            // Reset
                            chatState = ChatState.Idle
                            streamingContent = ""
                            streamingThinking = ""
                            // ID
                            generatingSessionId = null
                            
                            // Check
                            val lastMessage = restoredSession.messages.lastOrNull()
                            AppLogger.d("AiHtmlCodingScreen", "Last message role: ${lastMessage?.role}, content: ${lastMessage?.content?.take(50)}")
                            
                            if (lastMessage?.role == MessageRole.USER) {
                                // Comment
                                inputText = lastMessage.content
                                attachedImages = lastMessage.images
                                AppLogger.d("AiHtmlCodingScreen", "Set inputText to: ${inputText.take(50)}")
                                
                                // Comment
                                // User
                                val sessionWithoutLastUserMessage = restoredSession.copy(
                                    messages = restoredSession.messages.dropLast(1),
                                    updatedAt = System.currentTimeMillis() + 1000 // Ensure timestamp updates to avoid Flow overwrite
                                )
                                currentSession = sessionWithoutLastUserMessage
                                // Comment
                                storage.updateSession(sessionWithoutLastUserMessage)
                                AppLogger.d("AiHtmlCodingScreen", "Updated currentSession, messages: ${sessionWithoutLastUserMessage.messages.size}")
                                
                                Toast.makeText(context, AppStringsProvider.current().rolledBackWithInputHint.replace("%s", checkpoint.name), Toast.LENGTH_LONG).show()
                            } else {
                                currentSession = restoredSession
                                Toast.makeText(context, AppStringsProvider.current().rolledBackTo.replace("%s", checkpoint.name), Toast.LENGTH_SHORT).show()
                            }
                            
                            AppLogger.d("AiHtmlCodingScreen", "After rollback: chatState=$chatState, inputText='${inputText.take(50)}'")
                        } ?: run {
                            AppLogger.e("AiHtmlCodingScreen", "Rollback failed: rollbackToConversationCheckpoint returned null")
                            Toast.makeText(context, AppStringsProvider.current().rollbackFailed, Toast.LENGTH_SHORT).show()
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
