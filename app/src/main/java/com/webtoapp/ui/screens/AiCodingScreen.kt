package com.webtoapp.ui.screens

import android.content.ComponentName
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.components.PremiumFilterChip
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.webtoapp.core.ai.coding.*
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.AiFeature
import com.webtoapp.ui.components.coding.*
import com.webtoapp.ui.codepreview.HtmlPreviewActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox

/**
 * AI- unifiedsupport7 apptype
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCodingScreen(
    onBack: () -> Unit,
    onExportToProject: ((List<ProjectFile>, String, AiCodingType) -> Unit)? = null,
    onNavigateToAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // manager
    val storage = remember { AiCodingStorage(context) }
    val configManager = remember { AiConfigManager(context) }
    
    // current in type
    var selectedCodingType by remember { mutableStateOf(AiCodingType.HTML) }
    
    // state
    var isLoading by remember { mutableStateOf(true) }
    
    val sessions by storage.sessionsFlow.collectAsState(initial = emptyList())
    val currentSessionId by storage.currentSessionIdFlow.collectAsState(initial = null)
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())
    
    LaunchedEffect(sessions) {
        isLoading = false
    }
    
    // filter
    val textModels = savedModels.filter { it.supportsFeature(AiFeature.AI_CODING) }
    val imageModels = savedModels.filter { it.supportsFeature(AiFeature.AI_CODING_IMAGE) }
    
    // currentsession
    var currentSession by remember { mutableStateOf<AiCodingSession?>(null) }
    
    // whensession sync codingType
    LaunchedEffect(currentSession?.id) {
        currentSession?.let { session ->
            selectedCodingType = session.codingType
        }
    }
    
    // from sessions Flow sync currentSession
    LaunchedEffect(currentSessionId, sessions) {
        val sessionFromFlow = sessions.find { it.id == currentSessionId }
        if (sessionFromFlow != null) {
            val shouldUpdate = currentSession == null || 
                currentSession?.id != sessionFromFlow.id ||
                sessionFromFlow.updatedAt > (currentSession?.updatedAt ?: 0)
            
            if (shouldUpdate) {
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
    var generatingSessionId by remember { mutableStateOf<String?>(null) }
    
    // itemfilerelatedstate
    val projectFileManager = remember { ProjectFileManager(context) }
    var projectFiles by remember { mutableStateOf<List<ProjectFileInfo>>(emptyList()) }
    var selectedProjectFile by remember { mutableStateOf<ProjectFileInfo?>(null) }
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var isFilesPanelExpanded by remember { mutableStateOf(true) }
    
    // codeeditstate
    var isEditingCode by remember { mutableStateOf(false) }
    var editingCodeContent by remember { mutableStateOf("") }
    
    // whensession refreshitemfilelist
    LaunchedEffect(currentSessionId) {
        currentSessionId?.let { sessionId ->
            projectFiles = projectFileManager.listFiles(sessionId)
            selectedProjectFile = null
            selectedFileContent = null
            isEditingCode = false
        } ?: run {
            projectFiles = emptyList()
            selectedProjectFile = null
            selectedFileContent = null
            isEditingCode = false
        }
    }
    
    fun refreshProjectFiles() {
        currentSessionId?.let { sessionId ->
            projectFiles = projectFileManager.listFiles(sessionId)
        }
    }
    
    var eventCollectorJob by remember { mutableStateOf<Job?>(null) }
    var currentServiceConnection by remember { mutableStateOf<ServiceConnection?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }
    
    // Sessionswitch state
    LaunchedEffect(currentSessionId) {
        if (currentSessionId != generatingSessionId) {
            streamingContent = ""
            streamingThinking = ""
            if (chatState is ChatState.Streaming || chatState is ChatState.GeneratingImage) {
                chatState = ChatState.Idle
            }
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
    
    val listState = rememberLazyListState()
    
    // Autoscroll bottom
    LaunchedEffect(currentSession?.messages?.size) {
        currentSession?.messages?.size?.let { size ->
            if (size > 0) {
                listState.animateScrollToItem(size - 1)
            }
        }
    }
    
    // message
    fun sendMessage() {
        if (inputText.isBlank()) return
        if (chatState !is ChatState.Idle) return
        
        scope.launch {
            val session = currentSession ?: run {
                val newSession = storage.createSession(codingType = selectedCodingType)
                currentSession = newSession
                newSession
            }
            val config = session.config
            
            if (config.textModelId == null) {
                Toast.makeText(context, AppStringsProvider.current().pleaseSelectTextModel, Toast.LENGTH_SHORT).show()
                showConfigSheet = true
                return@launch
            }
            
            val userMessage = AiCodingMessage(
                role = MessageRole.USER,
                content = inputText,
                images = attachedImages
            )
            
            val updatedSession = storage.addMessage(session.id, userMessage)
            currentSession = updatedSession
            
            val sentText = inputText
            inputText = ""
            attachedImages = emptyList()
            
            chatState = ChatState.Streaming("")
            
            val targetSessionId = session.id
            generatingSessionId = targetSessionId
            
            try {
                val textModel = savedModels.find { it.id == config.textModelId }
                if (textModel == null) {
                    throw Exception(AppStringsProvider.current().modelConfigInvalid)
                }
                
                val currentHtml = updatedSession?.messages
                    ?.flatMap { it.codeBlocks }
                    ?.lastOrNull { it.language == "html" }
                    ?.content
                
                streamingContent = ""
                streamingThinking = ""
                
                val serviceIntent = Intent(context, AiGenerationService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
                
                eventCollectorJob?.cancel()
                eventCollectorJob = null
                
                if (isServiceBound && currentServiceConnection != null) {
                    try {
                        context.unbindService(currentServiceConnection!!)
                    } catch (e: Exception) {
                        AppLogger.e("AiCodingScreen", "Error unbinding previous service", e)
                    }
                    isServiceBound = false
                    currentServiceConnection = null
                }
                
                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val service = (binder as AiGenerationService.LocalBinder).getService()
                        isServiceBound = true
                        
                        eventCollectorJob = scope.launch {
                            val thinkingBuilder = StringBuilder()
                            var finalHtmlContent: String? = null
                            var textContent = ""
                            
                            fun shouldUpdateUI(): Boolean = currentSessionId == targetSessionId
                            
                            service.eventFlow.collect { event ->
                                when (event) {
                                    is HtmlAgentEvent.StateChange -> {
                                        if (shouldUpdateUI()) {
                                            when (event.state) {
                                                HtmlAgentState.GENERATING -> chatState = ChatState.Streaming("")
                                                HtmlAgentState.COMPLETED -> { }
                                                HtmlAgentState.ERROR -> { }
                                                HtmlAgentState.IDLE -> { }
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
                                    is HtmlAgentEvent.ToolCallStart -> { }
                                    is HtmlAgentEvent.CodeDelta -> {
                                        if (shouldUpdateUI()) {
                                            val lang = session.codingType.getPrimaryLanguage()
                                            streamingContent = "${AppStringsProvider.current().generatingCode}\n\n```$lang\n${event.accumulated}\n```"
                                        }
                                    }
                                    is HtmlAgentEvent.ToolExecuted -> {
                                        if (!event.result.success && shouldUpdateUI()) {
                                            val toolError = when (event.result.toolName) {
                                                "generate_image" -> "\uD83D\uDDBC\uFE0F ${AppStringsProvider.current().imageGenerationFailed}: ${event.result.result}"
                                                else -> "\u26A0\uFE0F ${event.result.toolName}: ${event.result.result}"
                                            }
                                            streamingContent = toolError
                                        }
                                    }
                                    is HtmlAgentEvent.FileCreated -> {
                                        if (shouldUpdateUI()) {
                                            projectFiles = projectFileManager.listFiles(targetSessionId)
                                        }
                                    }
                                    is HtmlAgentEvent.HtmlComplete -> {
                                        finalHtmlContent = event.html
                                    }
                                    is HtmlAgentEvent.AutoPreview -> { }
                                    is HtmlAgentEvent.ImageGenerating -> {
                                        if (shouldUpdateUI()) {
                                            chatState = ChatState.GeneratingImage(event.prompt)
                                        }
                                    }
                                    is HtmlAgentEvent.ImageGenerated -> { }
                                    is HtmlAgentEvent.Completed -> {
                                        val finalTextContent = event.textContent.ifBlank { textContent }
                                        val thinkingContent = thinkingBuilder.toString()
                                        
                                        val codeBlocks = if (finalHtmlContent != null) {
                                            listOf(CodeBlock(
                                                language = session.codingType.getPrimaryLanguage(),
                                                content = finalHtmlContent!!,
                                                filename = session.codingType.defaultEntryFile,
                                                isComplete = true
                                            ))
                                        } else {
                                            emptyList()
                                        }
                                        
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
                                                    appendLine()
                                                    appendLine(AppStringsProvider.current().possibleReasons)
                                                    appendLine(AppStringsProvider.current().apiFormatIncompatible)
                                                    appendLine(AppStringsProvider.current().modelNotSupported)
                                                    appendLine(AppStringsProvider.current().apiKeyQuotaInsufficient)
                                                    appendLine()
                                                    appendLine(AppStringsProvider.current().suggestionChangeModel)
                                                }
                                                debugInfo
                                            }
                                        }
                                        
                                        val aiMessage = AiCodingMessage(
                                            role = MessageRole.ASSISTANT,
                                            content = messageContent,
                                            thinking = thinkingContent.takeIf { it.isNotBlank() },
                                            codeBlocks = codeBlocks
                                        )
                                        
                                        val finalSession = storage.addMessage(targetSessionId, aiMessage)
                                        
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
                                        
                                        if (generatingSessionId == targetSessionId) {
                                            generatingSessionId = null
                                        }
                                        
                                        if (shouldUpdateUI()) {
                                            streamingContent = ""
                                            streamingThinking = ""
                                            chatState = ChatState.Idle
                                        }
                                        
                                        if (isServiceBound && currentServiceConnection != null) {
                                            try {
                                                context.unbindService(currentServiceConnection!!)
                                                isServiceBound = false
                                                currentServiceConnection = null
                                            } catch (e: Exception) {
                                                AppLogger.e("AiCodingScreen", "Error unbinding service", e)
                                            }
                                        }
                                    }
                                    is HtmlAgentEvent.Error -> {
                                        if (generatingSessionId == targetSessionId) {
                                            generatingSessionId = null
                                        }
                                        
                                        val errorMessage = AiCodingMessage(
                                            role = MessageRole.ASSISTANT,
                                            content = "${AppStringsProvider.current().errorPrefix}: ${event.message}",
                                            thinking = null,
                                            codeBlocks = emptyList()
                                        )
                                        val updatedSess = storage.addMessage(targetSessionId, errorMessage)
                                        
                                        if (shouldUpdateUI()) {
                                            currentSession = updatedSess
                                            chatState = ChatState.Idle
                                            streamingContent = ""
                                            streamingThinking = ""
                                        }
                                        
                                        if (isServiceBound && currentServiceConnection != null) {
                                            try {
                                                context.unbindService(currentServiceConnection!!)
                                                isServiceBound = false
                                                currentServiceConnection = null
                                            } catch (e: Exception) {
                                                AppLogger.e("AiCodingScreen", "Error unbinding service", e)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        service.startGeneration(
                            requirement = sentText,
                            currentHtml = currentHtml,
                            sessionConfig = config,
                            model = textModel,
                            sessionId = targetSessionId
                        )
                    }
                    
                    override fun onServiceDisconnected(name: ComponentName?) {
                        isServiceBound = false
                        currentServiceConnection = null
                    }
                }
                
                currentServiceConnection = serviceConnection
                context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                
            } catch (e: Exception) {
                chatState = ChatState.Error(e.message ?: AppStringsProvider.current().sendFailed)
                Toast.makeText(context, "${AppStringsProvider.current().errorPrefix}: ${e.message}", Toast.LENGTH_LONG).show()
                chatState = ChatState.Idle
            }
        }
    }
    
    // preview( only HTML/Frontend support)
    fun previewCode(codeBlock: CodeBlock) {
        if (!selectedCodingType.supportPreview) {
            Toast.makeText(context, AppStringsProvider.current().previewNotSupported, Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                val htmlContent = codeBlock.content
                val file = storage.saveForPreview(htmlContent)
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
    
    // previewitemfile( only HTML/Frontend)
    fun previewProjectFile(fileInfo: ProjectFileInfo) {
        if (!selectedCodingType.supportPreview) {
            Toast.makeText(context, AppStringsProvider.current().previewNotSupported, Toast.LENGTH_SHORT).show()
            return
        }
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
    
    // selectitemfileandloadcontent
    fun selectProjectFile(fileInfo: ProjectFileInfo) {
        scope.launch {
            selectedProjectFile = fileInfo
            isEditingCode = false
            currentSessionId?.let { sessionId ->
                selectedFileContent = projectFileManager.readFile(sessionId, fileInfo.name)
                editingCodeContent = selectedFileContent ?: ""
            }
        }
    }
    
    // saveedit code
    fun saveEditedCode() {
        scope.launch {
            val file = selectedProjectFile ?: return@launch
            val sessionId = currentSessionId ?: return@launch
            try {
                projectFileManager.createFile(sessionId, file.name, editingCodeContent, createNewVersion = false)
                selectedFileContent = editingCodeContent
                isEditingCode = false
                refreshProjectFiles()
                Toast.makeText(context, AppStringsProvider.current().fileSaved, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "${AppStringsProvider.current().errorPrefix}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // downloadcode local
    fun downloadCode(codeBlock: CodeBlock) {
        scope.launch {
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val filename = codeBlock.filename?.takeIf { it.isNotBlank() } ?: "code.${codeBlock.language}"
                val file = File(downloadDir, filename)
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
    
    // export code item
    fun exportAllToProject() {
        scope.launch {
            try {
                // prefer itemfile infile
                val sessionId = currentSessionId
                val files = if (sessionId != null && projectFiles.isNotEmpty()) {
                    projectFiles.map { fileInfo ->
                        val content = projectFileManager.readFile(sessionId, fileInfo.name) ?: ""
                        val type = when (fileInfo.type) {
                            ProjectFileType.HTML -> ProjectFileType.HTML
                            ProjectFileType.CSS -> ProjectFileType.CSS
                            ProjectFileType.JS -> ProjectFileType.JS
                            else -> fileInfo.type
                        }
                        ProjectFile(fileInfo.name, content, type)
                    }
                } else {
                    // fallback frommessage code
                    val allCodeBlocks = currentSession?.messages
                        ?.flatMap { it.codeBlocks }
                        ?: emptyList()
                    
                    if (allCodeBlocks.isEmpty()) {
                        Toast.makeText(context, AppStringsProvider.current().noCodeToExport, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    allCodeBlocks.map { block ->
                        val filename = block.filename?.takeIf { it.isNotBlank() } ?: selectedCodingType.defaultEntryFile
                        ProjectFile(filename, block.content, ProjectFileType.OTHER)
                    }
                }
                
                if (onExportToProject != null) {
                    val projectName = currentSession?.title?.take(20) ?: AppStringsProvider.current().aiGeneratedProject
                    onExportToProject.invoke(files, projectName, selectedCodingType)
                    Toast.makeText(context, AppStringsProvider.current().exportedToHtmlProject, Toast.LENGTH_SHORT).show()
                } else {
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
    
    // download file local
    fun downloadAllFiles() {
        scope.launch {
            try {
                val sessionId = currentSessionId ?: return@launch
                if (projectFiles.isEmpty()) {
                    Toast.makeText(context, AppStringsProvider.current().noCodeToExport, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "AiCoding_${currentSession?.title?.take(20) ?: System.currentTimeMillis()}"
                )
                downloadDir.mkdirs()
                projectFiles.forEach { fileInfo ->
                    val content = projectFileManager.readFile(sessionId, fileInfo.name) ?: ""
                    File(downloadDir, fileInfo.name).writeText(content)
                }
                Toast.makeText(context, AppStringsProvider.current().savedToPath.replace("%s", downloadDir.absolutePath), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "${AppStringsProvider.current().downloadFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun createNewSession() {
        scope.launch {
            val session = storage.createSession(codingType = selectedCodingType)
            currentSession = session
            showDrawer = false
        }
    }

    // ==================== UI ====================
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentSession?.title?.takeIf { it.isNotBlank() } ?: AppStringsProvider.current().aiCodingAssistant,
                            style = MaterialTheme.typography.titleMedium
                        )
                        currentSession?.let {
                            Text(
                                "${it.codingType.icon} ${it.codingType.getDisplayName()} · ${AppStringsProvider.current().messagesCount.replace("%d", "${it.messages.size}")}",
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
                    // checkpointfallback
                    IconButton(onClick = { showConversationCheckpointsSheet = true }) {
                        Icon(Icons.Outlined.Restore, AppStringsProvider.current().rollback)
                    }
                    // settings
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
        Box(
            modifier = Modifier
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier,
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
                // Welcome screen with type selection
                AiCodingWelcomeContent(
                    selectedType = selectedCodingType,
                    onTypeSelect = { selectedCodingType = it },
                    onNewChat = { createNewSession() },
                    onQuickPrompt = { prompt ->
                        scope.launch {
                            val session = storage.createSession(codingType = selectedCodingType)
                            currentSession = session
                            inputText = prompt
                        }
                    },
                    onSelectTemplate = { showTemplatesSheet = true },
                    onOpenTutorial = { showTutorialSheet = true },
                    onNavigateToAiSettings = onNavigateToAiSettings
                )
            } else {
                // Main content area
                Box(modifier = Modifier.fillMaxSize()) {
                    // Message list
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
                                onPreviewCode = { codeBlock ->
                                    if (selectedCodingType.supportPreview) {
                                        previewCode(codeBlock)
                                    }
                                },
                                onCopyCode = {
                                    Toast.makeText(context, AppStringsProvider.current().msgCopied, Toast.LENGTH_SHORT).show()
                                },
                                onDownloadCode = { codeBlock -> downloadCode(codeBlock) },
                                onExportToProject = { exportAllToProject() }
                            )
                        }
                        
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
                                AiCodingImageGeneratingIndicator(
                                    prompt = (chatState as ChatState.GeneratingImage).prompt
                                )
                            }
                        }
                    }
                    
                    // Bottom panel - project files + code editor
                    if (currentSession != null) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            // Code edit/preview panel
                            AnimatedVisibility(
                                visible = selectedProjectFile != null && selectedFileContent != null,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                AiCodingFileEditorPanel(
                                    file = selectedProjectFile,
                                    content = if (isEditingCode) editingCodeContent else (selectedFileContent ?: ""),
                                    isEditing = isEditingCode,
                                    canPreview = selectedCodingType.supportPreview && 
                                        selectedProjectFile?.type == ProjectFileType.HTML,
                                    onContentChange = { editingCodeContent = it },
                                    onClose = {
                                        selectedProjectFile = null
                                        selectedFileContent = null
                                        isEditingCode = false
                                    },
                                    onToggleEdit = {
                                        if (isEditingCode) {
                                            saveEditedCode()
                                        } else {
                                            editingCodeContent = selectedFileContent ?: ""
                                            isEditingCode = true
                                        }
                                    },
                                    onPreview = {
                                        selectedProjectFile?.let { previewProjectFile(it) }
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            
                            // Project file tree panel
                            AiCodingDirectoryTreePanel(
                                files = projectFiles,
                                selectedFile = selectedProjectFile,
                                codingType = selectedCodingType,
                                onFileClick = { fileInfo -> selectProjectFile(fileInfo) },
                                onPreviewClick = { fileInfo ->
                                    if (selectedCodingType.supportPreview) {
                                        previewProjectFile(fileInfo)
                                    }
                                },
                                onRefresh = { refreshProjectFiles() },
                                onExportAll = { exportAllToProject() },
                                onDownloadAll = { downloadAllFiles() },
                                isExpanded = isFilesPanelExpanded,
                                onExpandChange = { isFilesPanelExpanded = it }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // ==================== Sheets / Drawer ====================
    
    // Side drawer - session list
    if (showDrawer) {
        // Semi-transparent overlay, click to close
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    showDrawer = false
                }
        )
        // Session panel sliding in from the right
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                AiCodingSessionDrawerContent(
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
                    onCodingTypeChange = { type ->
                        selectedCodingType = type
                    },
                    onDismiss = { showDrawer = false }
                )
            }
        }
    }
    
    // Configuration bottom sheet
    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false }
        ) {
            val sessionForConfig = currentSession ?: run {
                var newSession by remember { mutableStateOf<AiCodingSession?>(null) }
                LaunchedEffect(Unit) {
                    newSession = storage.createSession(codingType = selectedCodingType)
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
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    // Template selection sheet
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
                        val session = currentSession ?: storage.createSession(codingType = selectedCodingType)
                        val newConfig = session.config.copy(selectedTemplateId = templateId)
                        storage.updateSession(session.copy(config = newConfig))
                        currentSession = session.copy(config = newConfig)
                    }
                },
                onStyleSelect = { styleId ->
                    scope.launch {
                        val session = currentSession ?: storage.createSession(codingType = selectedCodingType)
                        val newConfig = session.config.copy(selectedStyleId = styleId)
                        storage.updateSession(session.copy(config = newConfig))
                        currentSession = session.copy(config = newConfig)
                    }
                },
                onDismiss = { showTemplatesSheet = false }
            )
        }
    }
    
    // Tutorial sheet
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
    
    // Version checkpoint sheet
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
                                chatState = ChatState.Idle
                                streamingContent = ""
                                streamingThinking = ""
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
    
    // Edit message dialog
    editingMessage?.let { message ->
        EditMessageDialog(
            message = message,
            onDismiss = { editingMessage = null },
            onConfirm = { newContent, newImages ->
                scope.launch {
                    currentSession?.let { session ->
                        storage.editUserMessage(session.id, message.id, newContent, newImages)?.let {
                            currentSession = it
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
    
    // Save project dialog
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
    
    // Snippet library sheet
    if (showCodeLibrarySheet) {
        val codeLibrary by storage.codeLibraryFlow.collectAsState(initial = emptyList())
        
        ModalBottomSheet(
            onDismissRequest = { showCodeLibrarySheet = false }
        ) {
            CodeLibrarySheetContent(
                items = codeLibrary,
                onPreview = { item ->
                    if (selectedCodingType.supportPreview) {
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
                    } else {
                        Toast.makeText(context, AppStringsProvider.current().previewNotSupported, Toast.LENGTH_SHORT).show()
                    }
                },
                onUseContent = { item ->
                    inputText = "${AppStringsProvider.current().continueDevBasedOnCode}\n${item.userPrompt}"
                    showCodeLibrarySheet = false
                },
                onExportToProject = { item ->
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
    
    // Conversation checkpoint rollback sheet
    var conversationCheckpoints by remember { mutableStateOf<List<ConversationCheckpoint>>(emptyList()) }
    
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
                        storage.rollbackToConversationCheckpoint(checkpoint.id)?.let { restoredSession ->
                            chatState = ChatState.Idle
                            streamingContent = ""
                            streamingThinking = ""
                            generatingSessionId = null
                            
                            val lastMessage = restoredSession.messages.lastOrNull()
                            
                            if (lastMessage?.role == MessageRole.USER) {
                                inputText = lastMessage.content
                                attachedImages = lastMessage.images
                                
                                val sessionWithoutLastUserMessage = restoredSession.copy(
                                    messages = restoredSession.messages.dropLast(1),
                                    updatedAt = System.currentTimeMillis() + 1000
                                )
                                currentSession = sessionWithoutLastUserMessage
                                storage.updateSession(sessionWithoutLastUserMessage)
                                
                                Toast.makeText(context, AppStringsProvider.current().rolledBackWithInputHint.replace("%s", checkpoint.name), Toast.LENGTH_LONG).show()
                            } else {
                                currentSession = restoredSession
                                Toast.makeText(context, AppStringsProvider.current().rolledBackTo.replace("%s", checkpoint.name), Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
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
}

// ==================== Child Components ====================

/**
 * welcome- apptypeselect
 */
@Composable
private fun AiCodingWelcomeContent(
    selectedType: AiCodingType,
    onTypeSelect: (AiCodingType) -> Unit,
    onNewChat: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onSelectTemplate: () -> Unit,
    onOpenTutorial: () -> Unit,
    onNavigateToAiSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Logoarea
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
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            AppStringsProvider.current().aiCodingAssistant,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            AppStringsProvider.current().aiCodingWelcome,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // apptypeselect
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                AppStringsProvider.current().selectCodingType,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // apptype
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AiCodingType.values().toList().chunked(2).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTypes.forEach { type ->
                        val isSelected = type == selectedType
                        Surface(
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .clickable { onTypeSelect(type) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            tonalElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(com.webtoapp.util.SvgIconMapper.getIcon(type.icon), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        type.getDisplayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    type.getDescription(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (rowTypes.size == 1) {
                        Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // button
        PremiumButton(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "${AppStringsProvider.current().startNewConversation} (${selectedType.getDisplayName()})",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                AppStringsProvider.current().tryAskingPrompts,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // hint card
        val examplePrompts = selectedType.getExamplePrompts()
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            examplePrompts.forEach { prompt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickPrompt(prompt) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            com.webtoapp.util.SvgIconMapper.getIcon(selectedType.icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumOutlinedButton(
                onClick = onSelectTemplate,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStringsProvider.current().templates)
            }
            
            PremiumOutlinedButton(
                onClick = onOpenTutorial,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.School, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStringsProvider.current().tutorial)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * indicator
 */
@Composable
private fun AiCodingImageGeneratingIndicator(prompt: String) {
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
                AppStringsProvider.current().generatingImage,
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

/**
 * sessionlistsidebar- typeswitch
 */
@Composable
private fun AiCodingSessionDrawerContent(
    sessions: List<AiCodingSession>,
    currentSessionId: String?,
    onSessionClick: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onCodingTypeChange: (AiCodingType) -> Unit,
    onDismiss: () -> Unit
) {
    // typefilter
    var filterType by remember { mutableStateOf<AiCodingType?>(null) }
    val filteredSessions = if (filterType != null) {
        sessions.filter { it.codingType == filterType }
    } else {
        sessions
    }
    
    Column(modifier = Modifier.fillMaxHeight()) {
        // header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(AppStringsProvider.current().conversationHistory, style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, AppStringsProvider.current().close)
            }
        }
        
        // typefilter scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // "all" filter
            PremiumFilterChip(
                selected = filterType == null,
                onClick = { filterType = null },
                label = { Text(AppStringsProvider.current().allFilter, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(32.dp)
            )
            AiCodingType.values().forEach { type ->
                PremiumFilterChip(
                    selected = filterType == type,
                    onClick = { filterType = if (filterType == type) null else type },
                    label = { Text(type.getDisplayName(), style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(com.webtoapp.util.SvgIconMapper.getIcon(type.icon), contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // newbutton
        FilledTonalButton(
            onClick = {
                filterType?.let { onCodingTypeChange(it) }
                onNewSession()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (filterType != null) {
                    "${AppStringsProvider.current().newConversation} (${filterType!!.getDisplayName()})"
                } else {
                    AppStringsProvider.current().newConversation
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        
        // sessionlist
        LazyColumn(modifier = Modifier.weight(weight = 1f, fill = true)) {
            items(filteredSessions, key = { it.id }) { session ->
                AiCodingSessionListItem(
                    session = session,
                    isSelected = session.id == currentSessionId,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { onDeleteSession(session.id) }
                )
            }
            
            if (filteredSessions.isEmpty()) {
                item {
                    Text(
                        AppStringsProvider.current().noConversationRecords,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * sessionlist- display type
 */
@Composable
private fun AiCodingSessionListItem(
    session: AiCodingSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // typeicon
            Icon(com.webtoapp.util.SvgIconMapper.getIcon(session.codingType.icon), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    session.title.takeIf { it.isNotBlank() } ?: AppStringsProvider.current().newConversation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${session.codingType.getDisplayName()} · ${AppStringsProvider.current().messagesCount.replace("%d", "${session.messages.size}")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
            
            // deletebutton
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(AppStringsProvider.current().deleteConfirmTitle) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(AppStringsProvider.current().delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(AppStringsProvider.current().cancel)
                }
            }
        )
    }
}

/**
 * itemfilefile treepanel
 */
@Composable
private fun AiCodingDirectoryTreePanel(
    files: List<ProjectFileInfo>,
    selectedFile: ProjectFileInfo?,
    codingType: AiCodingType,
    onFileClick: (ProjectFileInfo) -> Unit,
    onPreviewClick: (ProjectFileInfo) -> Unit,
    onRefresh: () -> Unit,
    onExportAll: () -> Unit,
    onDownloadAll: () -> Unit,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    if (files.isEmpty()) return
    
    var showMoreMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Column {
            // header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!isExpanded) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${AppStringsProvider.current().projectFiles} (${files.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                    }
                    
                    Box {
                        IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.MoreVert, null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(AppStringsProvider.current().exportToProject) },
                                onClick = {
                                    showMoreMenu = false
                                    onExportAll()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Upload, null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text(AppStringsProvider.current().downloadAllFiles) },
                                onClick = {
                                    showMoreMenu = false
                                    onDownloadAll()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
            
            // filelist( expand)
            AnimatedVisibility(visible = isExpanded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    items(files, key = { it.name }) { fileInfo ->
                        val isSelected = fileInfo.name == selectedFile?.name
                        val fileIcon = getFileIcon(fileInfo)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFileClick(fileInfo) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Note
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // fileicon
                            Icon(com.webtoapp.util.SvgIconMapper.getIcon(fileIcon), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // file
                            Text(
                                fileInfo.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(weight = 1f, fill = true),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // file
                            Text(
                                fileInfo.formatSize(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            // previewbutton( onlysupportpreview type+HTMLfile)
                            if (codingType.supportPreview && fileInfo.type == ProjectFileType.HTML) {
                                IconButton(
                                    onClick = { onPreviewClick(fileInfo) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
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

/**
 * file icon
 */
private fun getFileIcon(fileInfo: ProjectFileInfo): String {
    return when (fileInfo.type) {
        ProjectFileType.HTML -> "html"
        ProjectFileType.CSS -> "palette"
        ProjectFileType.JS -> "bolt"
        ProjectFileType.JSON -> "clipboard"
        ProjectFileType.SVG -> "image"
        ProjectFileType.IMAGE -> "image"
        ProjectFileType.OTHER -> {
            when (fileInfo.name.substringAfterLast(".").lowercase()) {
                "py" -> "python"
                "go" -> "golang"
                "php" -> "php"
                "ts", "tsx" -> "code"
                "jsx" -> "code"
                "md" -> "edit_note"
                "yaml", "yml" -> "settings"
                "env" -> "lock"
                "sql" -> "analytics"
                "txt" -> "file"
                "mod", "sum" -> "package"
                "cfg", "ini" -> "settings"
                else -> "file"
            }
        }
    }
}

/**
 * codeedit/previewpanel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiCodingFileEditorPanel(
    file: ProjectFileInfo?,
    content: String,
    isEditing: Boolean,
    canPreview: Boolean,
    onContentChange: (String) -> Unit,
    onClose: () -> Unit,
    onToggleEdit: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (file == null) return
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
        ) {
            // header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(com.webtoapp.util.SvgIconMapper.getIcon(getFileIcon(file)), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        file.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    if (isEditing) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                AppStringsProvider.current().editCode,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                Row {
                    // previewbutton
                    if (canPreview) {
                        IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.PlayArrow, AppStringsProvider.current().preview, modifier = Modifier.size(18.dp))
                        }
                    }
                    // edit/saveswitch
                    IconButton(onClick = onToggleEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isEditing) Icons.Outlined.Save else Icons.Outlined.Edit,
                            if (isEditing) AppStringsProvider.current().saveFile else AppStringsProvider.current().editCode,
                            modifier = Modifier.size(18.dp),
                            tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // close
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, AppStringsProvider.current().close, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            HorizontalDivider()
            
            // codecontent
            if (isEditing) {
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = true)
                        .padding(4.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = true)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
