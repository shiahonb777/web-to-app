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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AiFeature
import com.webtoapp.ui.components.coding.*
import com.webtoapp.ui.codepreview.HtmlPreviewActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import java.io.File
import androidx.compose.ui.graphics.Color




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHtmlCodingScreen(
    onBack: () -> Unit,
    onExportToHtmlProject: ((List<ProjectFile>, String) -> Unit)? = null,
    onNavigateToAiSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    val storage = remember { AiCodingStorage(context) }
    val configManager = remember { AiConfigManager(context) }
    val apiClient = remember { AiApiClient(context) }
    val htmlAgent = remember { AiCodingAgent(context) }


    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val sessions by storage.sessionsFlow.collectAsState(initial = emptyList())
    val currentSessionId by storage.currentSessionIdFlow.collectAsState(initial = null)
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())


    LaunchedEffect(sessions) {
        isLoading = false
    }


    val textModels = savedModels.filter { it.supportsFeature(AiFeature.AI_CODING) }
    val imageModels = savedModels.filter { it.supportsFeature(AiFeature.AI_CODING_IMAGE) }


    var currentSession by remember { mutableStateOf<AiCodingSession?>(null) }



    LaunchedEffect(currentSessionId, sessions) {
        val sessionFromFlow = sessions.find { it.id == currentSessionId }

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


    var inputText by remember { mutableStateOf("") }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var chatState by remember { mutableStateOf<ChatState>(ChatState.Idle) }
    var streamingContent by remember { mutableStateOf("") }
    var streamingThinking by remember { mutableStateOf("") }


    var generatingSessionId by remember { mutableStateOf<String?>(null) }


    val projectFileManager = remember { ProjectFileManager(context) }
    var projectFiles by remember { mutableStateOf<List<ProjectFileInfo>>(emptyList()) }
    var selectedProjectFile by remember { mutableStateOf<ProjectFileInfo?>(null) }
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var isFilesPanelExpanded by remember { mutableStateOf(true) }


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


    fun refreshProjectFiles() {
        currentSessionId?.let { sessionId ->
            projectFiles = projectFileManager.listFiles(sessionId)
        }
    }


    var eventCollectorJob by remember { mutableStateOf<Job?>(null) }


    var currentServiceConnection by remember { mutableStateOf<ServiceConnection?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }


    LaunchedEffect(currentSessionId) {

        if (currentSessionId != generatingSessionId) {
            streamingContent = ""
            streamingThinking = ""

            if (chatState is ChatState.Streaming || chatState is ChatState.GeneratingImage) {
                chatState = ChatState.Idle
            }
            AppLogger.d("AiHtmlCodingScreen", "Session switched: cleared streaming state, currentSessionId=$currentSessionId, generatingSessionId=$generatingSessionId")
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
    var editingMessage by remember { mutableStateOf<AiCodingMessage?>(null) }


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


    LaunchedEffect(currentSession?.messages?.size) {
        currentSession?.messages?.size?.let { size ->
            if (size > 0) {
                listState.animateScrollToItem(size - 1)
            }
        }
    }


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

            val session = currentSession ?: run {
                val newSession = storage.createSession()
                currentSession = newSession
                newSession
            }
            val config = session.config


            if (config.textModelId == null) {
                Toast.makeText(context, Strings.pleaseSelectTextModel, Toast.LENGTH_SHORT).show()
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
                    throw Exception(Strings.modelConfigInvalid)
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
                AppLogger.d("AiHtmlCodingScreen", "Cancelled previous event collector job")


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


                val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val service = (binder as AiGenerationService.LocalBinder).getService()
                        isServiceBound = true


                        eventCollectorJob = scope.launch {
                            val thinkingBuilder = StringBuilder()
                            var finalHtmlContent: String? = null
                            var textContent = ""


                            fun shouldUpdateUI(): Boolean = currentSessionId == targetSessionId

                            AppLogger.d("AiHtmlCodingScreen", "Started event collector for session: $targetSessionId")

                            service.eventFlow.collect { event ->
                                AppLogger.d("AiHtmlCodingScreen", "Received event: ${event::class.simpleName} for session: $targetSessionId")
                                when (event) {
                                    is HtmlAgentEvent.StateChange -> {
                                        if (shouldUpdateUI()) {
                                            when (event.state) {
                                                HtmlAgentState.GENERATING -> chatState = ChatState.Streaming("")
                                                HtmlAgentState.COMPLETED -> {  }
                                                HtmlAgentState.ERROR -> {  }
                                                HtmlAgentState.IDLE -> {  }
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
                                            streamingContent = "${Strings.generatingCode}\n\n```html\n${event.accumulated}\n```"
                                        }
                                    }
                                    is HtmlAgentEvent.ToolExecuted -> {
                                        AppLogger.d("AiHtmlCodingScreen", "Tool executed: ${event.result.toolName}, success=${event.result.success}")

                                        if (!event.result.success && shouldUpdateUI()) {
                                            val toolError = when (event.result.toolName) {
                                                "generate_image" -> "\uD83D\uDDBC\uFE0F ${Strings.imageGenerationFailed}: ${event.result.result}"
                                                else -> "\u26A0\uFE0F ${event.result.toolName}: ${event.result.result}"
                                            }
                                            streamingContent = toolError
                                        }
                                    }
                                    is HtmlAgentEvent.FileCreated -> {
                                        AppLogger.d("AiHtmlCodingScreen", "File created: ${event.fileInfo.name}, version=${event.fileInfo.version}")

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
                                            name = Strings.conversationCheckpoint.replace("%d", "${(finalSession?.messages?.size ?: 0) / 2}")
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
                                                AppLogger.d("AiHtmlCodingScreen", "Service unbound after completion")
                                            } catch (e: Exception) {
                                                AppLogger.e("AiHtmlCodingScreen", "Error unbinding service", e)
                                            }
                                        }
                                    }
                                    is HtmlAgentEvent.Error -> {

                                        if (generatingSessionId == targetSessionId) {
                                            generatingSessionId = null
                                        }


                                        val errorMessage = AiCodingMessage(
                                            role = MessageRole.ASSISTANT,
                                            content = "${Strings.errorPrefix}: ${event.message}",
                                            thinking = null,
                                            codeBlocks = emptyList()
                                        )
                                        val updatedSession = storage.addMessage(targetSessionId, errorMessage)


                                        if (shouldUpdateUI()) {
                                            currentSession = updatedSession
                                            chatState = ChatState.Idle
                                            streamingContent = ""
                                            streamingThinking = ""
                                        } else {

                                            AppLogger.w("AiHtmlCodingScreen", "Background session $targetSessionId error: ${event.message}")
                                        }


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
                chatState = ChatState.Error(e.message ?: Strings.sendFailed)
                Toast.makeText(context, "${Strings.errorPrefix}: ${e.message}", Toast.LENGTH_LONG).show()
                chatState = ChatState.Idle
            }
        }
    }


    fun previewHtml(codeBlock: CodeBlock) {
        scope.launch {
            try {

                val htmlContent = codeBlock.content

                val file = storage.saveForPreview(htmlContent)


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


    fun selectProjectFile(fileInfo: ProjectFileInfo) {
        scope.launch {
            selectedProjectFile = fileInfo
            currentSessionId?.let { sessionId ->
                selectedFileContent = projectFileManager.readFile(sessionId, fileInfo.name)
            }
        }
    }


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
                Toast.makeText(context, Strings.savedToPath.replace("%s", actualFile.absolutePath), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "${Strings.downloadFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


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


                if (onExportToHtmlProject != null) {
                    val projectName = currentSession?.title?.take(20) ?: Strings.aiGeneratedProject
                    onExportToHtmlProject.invoke(files, projectName)
                    Toast.makeText(context, Strings.exportedToHtmlProject, Toast.LENGTH_SHORT).show()
                } else {

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
                    IconButton(onClick = {
                        if (showDrawer) {
                            showDrawer = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {

                    IconButton(onClick = { showCodeLibrarySheet = true }) {
                        Icon(Icons.Outlined.Folder, Strings.codeLibrary)
                    }

                    IconButton(onClick = { showConversationCheckpointsSheet = true }) {
                        Icon(Icons.Outlined.Restore, Strings.rollback)
                    }

                    IconButton(onClick = { showTemplatesSheet = true }) {
                        Icon(Icons.Outlined.Palette, Strings.templates)
                    }

                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Outlined.Settings, Strings.settings)
                    }

                    IconButton(onClick = { showDrawer = !showDrawer }) {
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
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

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

                Box(modifier = Modifier.fillMaxSize()) {

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


                    if (currentSession != null) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {

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


    if (showDrawer) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    showDrawer = false
                }
        )

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


    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false }
        ) {

            val sessionForConfig = currentSession ?: run {

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

                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }


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


    if (showCodeLibrarySheet) {
        val codeLibrary by storage.codeLibraryFlow.collectAsState(initial = emptyList())

        ModalBottomSheet(
            onDismissRequest = { showCodeLibrarySheet = false }
        ) {
            CodeLibrarySheetContent(
                items = codeLibrary,
                onPreview = { item ->

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

                    inputText = "${Strings.continueDevBasedOnCode}\n${item.userPrompt}"
                    showCodeLibrarySheet = false
                },
                onExportToProject = { item ->

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
                        AppLogger.d("AiHtmlCodingScreen", "Rolling back to checkpoint: ${checkpoint.id}, name: ${checkpoint.name}")
                        AppLogger.d("AiHtmlCodingScreen", "Before rollback: chatState=$chatState, generatingSessionId=$generatingSessionId")

                        storage.rollbackToConversationCheckpoint(checkpoint.id)?.let { restoredSession ->
                            AppLogger.d("AiHtmlCodingScreen", "Rollback successful, restoredSession: ${restoredSession.id}, messages: ${restoredSession.messages.size}")


                            chatState = ChatState.Idle
                            streamingContent = ""
                            streamingThinking = ""

                            generatingSessionId = null


                            val lastMessage = restoredSession.messages.lastOrNull()
                            AppLogger.d("AiHtmlCodingScreen", "Last message role: ${lastMessage?.role}, content: ${lastMessage?.content?.take(50)}")

                            if (lastMessage?.role == MessageRole.USER) {

                                inputText = lastMessage.content
                                attachedImages = lastMessage.images
                                AppLogger.d("AiHtmlCodingScreen", "Set inputText to: ${inputText.take(50)}")



                                val sessionWithoutLastUserMessage = restoredSession.copy(
                                    messages = restoredSession.messages.dropLast(1),
                                    updatedAt = System.currentTimeMillis() + 1000
                                )
                                currentSession = sessionWithoutLastUserMessage

                                storage.updateSession(sessionWithoutLastUserMessage)
                                AppLogger.d("AiHtmlCodingScreen", "Updated currentSession, messages: ${sessionWithoutLastUserMessage.messages.size}")

                                Toast.makeText(context, Strings.rolledBackWithInputHint.replace("%s", checkpoint.name), Toast.LENGTH_LONG).show()
                            } else {
                                currentSession = restoredSession
                                Toast.makeText(context, Strings.rolledBackTo.replace("%s", checkpoint.name), Toast.LENGTH_SHORT).show()
                            }

                            AppLogger.d("AiHtmlCodingScreen", "After rollback: chatState=$chatState, inputText='${inputText.take(50)}'")
                        } ?: run {
                            AppLogger.e("AiHtmlCodingScreen", "Rollback failed: rollbackToConversationCheckpoint returned null")
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
