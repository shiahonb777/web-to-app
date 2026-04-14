package com.webtoapp.core.ai.coding

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

private val Context.aiCodingDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_coding")

class AiCodingStorage(private val context: Context) {

    private val gson = com.webtoapp.util.GsonProvider.gson

    companion object {
        private val KEY_SESSIONS = stringPreferencesKey("sessions")
        private val KEY_CURRENT_SESSION_ID = stringPreferencesKey("current_session_id")
        private val KEY_CODE_LIBRARY = stringPreferencesKey("code_library")
        private val KEY_CHECKPOINTS = stringPreferencesKey("conversation_checkpoints")
        private const val AI_CODING_DIR = "AiCoding"
        private const val SESSIONS_DIR = "sessions"
        private const val PROJECTS_DIR = "projects"
        private const val IMAGES_DIR = "images"
        private const val CODE_LIBRARY_DIR = "code_library"
    }

    val sessionsFlow: Flow<List<AiCodingSession>> = context.aiCodingDataStore.data.map { prefs ->
        val json = prefs[KEY_SESSIONS] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<AiCodingSession>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val currentSessionIdFlow: Flow<String?> = context.aiCodingDataStore.data.map { prefs ->
        prefs[KEY_CURRENT_SESSION_ID]
    }
    
    private val projectFileManager = ProjectFileManager(context)

    suspend fun createSession(title: String = "", codingType: AiCodingType = AiCodingType.HTML): AiCodingSession {
        val sessionId = UUID.randomUUID().toString()
        val projectDir = projectFileManager.getSessionProjectDir(sessionId)
        
        val session = AiCodingSession(
            id = sessionId,
            title = title,
            projectDir = projectDir.absolutePath,
            codingType = codingType
        )
        
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            sessions.add(0, session)  // Note.
            prefs[KEY_SESSIONS] = gson.toJson(sessions)
            prefs[KEY_CURRENT_SESSION_ID] = session.id
        }
        return session
    }
    
    fun getProjectFileManager(): ProjectFileManager = projectFileManager

    suspend fun updateSession(session: AiCodingSession) {
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val index = sessions.indexOfFirst { it.id == session.id }
            if (index >= 0) {
                sessions[index] = session.copy(updatedAt = System.currentTimeMillis())
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
    }

    suspend fun deleteSession(sessionId: String) {
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).filter { it.id != sessionId }
            prefs[KEY_SESSIONS] = gson.toJson(sessions)
            
            if (prefs[KEY_CURRENT_SESSION_ID] == sessionId) {
                prefs.remove(KEY_CURRENT_SESSION_ID)
            }
        }
        
        deleteSessionFiles(sessionId)
    }

    suspend fun setCurrentSession(sessionId: String) {
        context.aiCodingDataStore.edit { prefs ->
            prefs[KEY_CURRENT_SESSION_ID] = sessionId
        }
    }

    suspend fun getSession(sessionId: String): AiCodingSession? {
        return sessionsFlow.first().find { it.id == sessionId }
    }

    suspend fun getCurrentSession(): AiCodingSession? {
        val currentId = currentSessionIdFlow.first() ?: return null
        return getSession(currentId)
    }
    suspend fun addMessage(sessionId: String, message: AiCodingMessage): AiCodingSession? {
        var updatedSession: AiCodingSession? = null
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val index = sessions.indexOfFirst { it.id == sessionId }
            if (index >= 0) {
                val session = sessions[index]
                val newMessages = session.messages + message
                val updated = session.copy(
                    messages = newMessages,
                    updatedAt = System.currentTimeMillis()
                )
                updatedSession = updated
                sessions[index] = updated
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
        return updatedSession
    }

    suspend fun updateMessage(sessionId: String, message: AiCodingMessage): AiCodingSession? {
        var updatedSession: AiCodingSession? = null
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val newMessages = session.messages.map { 
                    if (it.id == message.id) message else it 
                }
                val updated = session.copy(
                    messages = newMessages,
                    updatedAt = System.currentTimeMillis()
                )
                updatedSession = updated
                sessions[sessionIndex] = updated
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
        return updatedSession
    }

    suspend fun editUserMessage(
        sessionId: String, 
        messageId: String, 
        newContent: String,
        newImages: List<String> = emptyList()
    ): AiCodingSession? {
        var updatedSession: AiCodingSession? = null
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val messageIndex = session.messages.indexOfFirst { it.id == messageId }
                
                if (messageIndex >= 0) {
                    val oldMessage = session.messages[messageIndex]
                    
                    val checkpoint = createCheckpointFromSession(session, messageIndex, "编辑前自动保存")
                    
                    val editedMessage = oldMessage.copy(
                        content = newContent,
                        images = newImages,
                        isEdited = true,
                        originalContent = oldMessage.originalContent ?: oldMessage.content
                    )
                    
                    val newMessages = session.messages.subList(0, messageIndex) + editedMessage
                    
                    val newCheckpoints = session.checkpoints + checkpoint
                    
                    val updated = session.copy(
                        messages = newMessages,
                        checkpoints = newCheckpoints,
                        currentCheckpointIndex = newCheckpoints.size - 1,
                        updatedAt = System.currentTimeMillis()
                    )
                    updatedSession = updated
                    sessions[sessionIndex] = updated
                    prefs[KEY_SESSIONS] = gson.toJson(sessions)
                }
            }
        }
        return updatedSession
    }

    suspend fun createCheckpoint(
        sessionId: String, 
        name: String,
        description: String = ""
    ): ProjectCheckpoint? {
        var checkpoint: ProjectCheckpoint? = null
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val cp = createCheckpointFromSession(session, session.messages.size, name, description)
                checkpoint = cp
                val newCheckpoints = session.checkpoints + cp
                val updatedSession = session.copy(
                    checkpoints = newCheckpoints,
                    currentCheckpointIndex = newCheckpoints.size - 1,
                    updatedAt = System.currentTimeMillis()
                )
                sessions[sessionIndex] = updatedSession
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
        return checkpoint
    }

    suspend fun rollbackToCheckpoint(sessionId: String, checkpointId: String): AiCodingSession? {
        var updatedSession: AiCodingSession? = null
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val checkpointIndex = session.checkpoints.indexOfFirst { it.id == checkpointId }
                
                if (checkpointIndex >= 0) {
                    val checkpoint = session.checkpoints[checkpointIndex]
                    
                    val restoredMessages = session.messages.take(checkpoint.messageIndex)
                    
                    val updated = session.copy(
                        messages = restoredMessages,
                        currentCheckpointIndex = checkpointIndex,
                        updatedAt = System.currentTimeMillis()
                    )
                    updatedSession = updated
                    sessions[sessionIndex] = updated
                    prefs[KEY_SESSIONS] = gson.toJson(sessions)
                }
            }
        }
        return updatedSession
    }

    suspend fun deleteCheckpoint(sessionId: String, checkpointId: String) {
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val newCheckpoints = session.checkpoints.filter { it.id != checkpointId }
                val updatedSession = session.copy(
                    checkpoints = newCheckpoints,
                    currentCheckpointIndex = minOf(session.currentCheckpointIndex, newCheckpoints.size - 1),
                    updatedAt = System.currentTimeMillis()
                )
                sessions[sessionIndex] = updatedSession
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
    }

    private fun createCheckpointFromSession(
        session: AiCodingSession,
        messageIndex: Int,
        name: String,
        description: String = ""
    ): ProjectCheckpoint {
        val files = mutableListOf<ProjectFile>()
        session.messages.take(messageIndex).forEach { message ->
            message.codeBlocks.forEach { block ->
                val filename = block.filename?.takeIf { it.isNotBlank() } ?: "index.${block.language}"
                val type = when (block.language.lowercase()) {
                    "html" -> ProjectFileType.HTML
                    "css" -> ProjectFileType.CSS
                    "javascript", "js" -> ProjectFileType.JS
                    "svg" -> ProjectFileType.SVG
                    "json" -> ProjectFileType.JSON
                    else -> ProjectFileType.OTHER
                }
                val existingIndex = files.indexOfFirst { it.name == filename }
                if (existingIndex >= 0) {
                    files[existingIndex] = ProjectFile(filename, block.content, type)
                } else {
                    files.add(ProjectFile(filename, block.content, type))
                }
            }
        }
        
        return ProjectCheckpoint(
            name = name,
            description = description,
            messageIndex = messageIndex,
            files = files
        )
    }

    fun getAiCodingDir(): File {
        val dir = File(context.getExternalFilesDir(null), AI_CODING_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getProjectsDir(): File {
        val dir = File(getAiCodingDir(), PROJECTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getImagesDir(): File {
        val dir = File(getAiCodingDir(), IMAGES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveProject(
        config: SaveConfig,
        files: List<ProjectFile>
    ): Result<File> {
        return try {
            val targetDir = if (config.createFolder) {
                File(config.directory, config.projectName)
            } else {
                File(config.directory)
            }
            
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            files.forEach { file ->
                val targetFile = File(targetDir, file.name)
                
                targetFile.parentFile?.mkdirs()
                
                if (targetFile.exists() && !config.overwrite) {
                    throw Exception("文件已存在: ${file.name}")
                }
                
                targetFile.writeText(file.content)
            }
            
            Result.success(targetDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveForPreview(content: String, filename: String = "preview.html"): File {
        val previewDir = File(getAiCodingDir(), "preview")
        if (!previewDir.exists()) previewDir.mkdirs()
        
        val file = File(previewDir, filename)
        file.writeText(content)
        
        AppLogger.d("AiCodingStorage", "saveForPreview: path=${file.absolutePath}, contentLength=${content.length}")
        
        val scriptMatches = CodeBlockParser.findScriptTags(content)
        AppLogger.d("AiCodingStorage", "Saved HTML has ${scriptMatches.size} script tags")
        scriptMatches.forEachIndexed { index, match ->
            val scriptContent = match.groupValues[2]
            AppLogger.d("AiCodingStorage", "  Saved Script $index: length=${scriptContent.length}, preview=${scriptContent.take(100).replace("\n", "\\n")}")
        }
        
        return file
    }

    fun saveImage(imageBytes: ByteArray, filename: String? = null): File {
        val actualFilename = filename ?: "${UUID.randomUUID()}.png"
        val file = File(getImagesDir(), actualFilename)
        file.writeBytes(imageBytes)
        return file
    }

    suspend fun copyImageToStorage(sourcePath: String): String {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return sourcePath
        
        val filename = "${UUID.randomUUID()}_${sourceFile.name}"
        val targetFile = File(getImagesDir(), filename)
        sourceFile.copyTo(targetFile, overwrite = true)
        return targetFile.absolutePath
    }

    private fun deleteSessionFiles(sessionId: String) {
        val sessionDir = File(getAiCodingDir(), "$SESSIONS_DIR/$sessionId")
        if (sessionDir.exists()) {
            sessionDir.deleteRecursively()
        }
    }

    fun getAvailableSaveDirectories(): List<Pair<String, File>> {
        val dirs = mutableListOf<Pair<String, File>>()
        
        dirs.add("应用目录" to getProjectsDir())
        
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let {
            if (it.exists() || it.mkdirs()) {
                dirs.add("下载目录" to it)
            }
        }
        
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let {
            if (it.exists() || it.mkdirs()) {
                dirs.add("文档目录" to it)
            }
        }
        
        return dirs
    }

    val codeLibraryFlow: Flow<List<CodeLibraryItem>> = context.aiCodingDataStore.data.map { prefs ->
        val json = prefs[KEY_CODE_LIBRARY] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<CodeLibraryItem>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToCodeLibrary(
        sessionId: String,
        messageId: String,
        userPrompt: String,
        codeBlocks: List<CodeBlock>,
        conversationContext: String = ""
    ): CodeLibraryItem? {
        if (codeBlocks.isEmpty()) return null
        
        val previewHtml = CodeBlockParser.mergeToSingleHtml(codeBlocks)
        val files = codeBlocks.map { block ->
            val filename = block.filename?.takeIf { it.isNotBlank() } ?: "index.${block.language}"
            val type = when (block.language.lowercase()) {
                "html" -> ProjectFileType.HTML
                "css" -> ProjectFileType.CSS
                "javascript", "js" -> ProjectFileType.JS
                else -> ProjectFileType.OTHER
            }
            ProjectFile(filename, block.content, type)
        }
        
        val title = extractTitle(userPrompt)
        val item = CodeLibraryItem(
            sessionId = sessionId,
            messageId = messageId,
            title = title,
            description = userPrompt.take(100),
            files = files,
            previewHtml = previewHtml,
            conversationContext = conversationContext,
            userPrompt = userPrompt
        )
        
        context.aiCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).toMutableList()
            library.add(0, item)
            prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
        }
        
        saveCodeLibraryFiles(item)
        
        return item
    }

    suspend fun getCodeLibraryItem(itemId: String): CodeLibraryItem? {
        return codeLibraryFlow.first().find { it.id == itemId }
    }

    suspend fun updateCodeLibraryItem(item: CodeLibraryItem) {
        context.aiCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).toMutableList()
            val index = library.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                library[index] = item
                prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
            }
        }
    }

    suspend fun deleteCodeLibraryItem(itemId: String) {
        context.aiCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).filter { it.id != itemId }
            prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
        }
        deleteCodeLibraryFiles(itemId)
    }

    suspend fun toggleFavorite(itemId: String) {
        context.aiCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).toMutableList()
            val index = library.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                library[index] = library[index].copy(isFavorite = !library[index].isFavorite)
                prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
            }
        }
    }

    fun exportToProjectLibrary(item: CodeLibraryItem, projectName: String): Result<File> {
        return saveProject(
            SaveConfig(
                directory = getProjectsDir().absolutePath,
                projectName = projectName,
                createFolder = true,
                overwrite = true
            ),
            item.files
        )
    }

    fun getCodeLibraryDir(): File {
        val dir = File(getAiCodingDir(), CODE_LIBRARY_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun saveCodeLibraryFiles(item: CodeLibraryItem) {
        val itemDir = File(getCodeLibraryDir(), item.id)
        if (!itemDir.exists()) itemDir.mkdirs()
        
        File(itemDir, "preview.html").writeText(item.previewHtml)
        
        item.files.forEach { file ->
            File(itemDir, file.name).writeText(file.content)
        }
    }

    private fun deleteCodeLibraryFiles(itemId: String) {
        val itemDir = File(getCodeLibraryDir(), itemId)
        if (itemDir.exists()) {
            itemDir.deleteRecursively()
        }
    }

    fun getCodeLibraryPreviewFile(itemId: String): File? {
        val file = File(getCodeLibraryDir(), "$itemId/preview.html")
        return if (file.exists()) file else null
    }

    val checkpointsFlow: Flow<List<ConversationCheckpoint>> = context.aiCodingDataStore.data.map { prefs ->
        val json = prefs[KEY_CHECKPOINTS] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<ConversationCheckpoint>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createConversationCheckpoint(
        sessionId: String,
        name: String = "自动保存"
    ): ConversationCheckpoint? {
        val session = getSession(sessionId) ?: return null
        
        val codeLibrary = codeLibraryFlow.first()
        val relatedLibraryIds = codeLibrary
            .filter { it.sessionId == sessionId }
            .map { it.id }
        
        val checkpoint = ConversationCheckpoint(
            sessionId = sessionId,
            name = name,
            messageCount = session.messages.size,
            messages = session.messages,
            codeLibraryIds = relatedLibraryIds,
            config = session.config
        )
        
        context.aiCodingDataStore.edit { prefs ->
            val checkpoints = getCheckpoints(prefs).toMutableList()
            val sessionCheckpoints = checkpoints.filter { it.sessionId == sessionId }
            if (sessionCheckpoints.size >= 10) {
                val oldest = sessionCheckpoints.minByOrNull { it.timestamp }
                oldest?.let { old -> checkpoints.removeAll { it.id == old.id } }
            }
            checkpoints.add(0, checkpoint)
            prefs[KEY_CHECKPOINTS] = gson.toJson(checkpoints)
        }
        
        return checkpoint
    }

    suspend fun getSessionCheckpoints(sessionId: String): List<ConversationCheckpoint> {
        return checkpointsFlow.first()
            .filter { it.sessionId == sessionId }
            .sortedByDescending { it.timestamp }
    }

    suspend fun rollbackToConversationCheckpoint(checkpointId: String): AiCodingSession? {
        val checkpoints = checkpointsFlow.first()
        val checkpoint = checkpoints.find { it.id == checkpointId } ?: return null
        
        context.aiCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == checkpoint.sessionId }
            if (sessionIndex >= 0) {
                val updatedSession = sessions[sessionIndex].copy(
                    messages = checkpoint.messages,
                    config = checkpoint.config,
                    updatedAt = System.currentTimeMillis()
                )
                sessions[sessionIndex] = updatedSession
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
            
            val library = getCodeLibrary(prefs).toMutableList()
            val itemsToRemove = library.filter { item ->
                item.sessionId == checkpoint.sessionId && 
                item.createdAt > checkpoint.timestamp &&
                !checkpoint.codeLibraryIds.contains(item.id)
            }
            itemsToRemove.forEach { item ->
                library.removeAll { it.id == item.id }
                deleteCodeLibraryFiles(item.id)
            }
            prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
        }
        
        return getSession(checkpoint.sessionId)
    }

    suspend fun deleteConversationCheckpoint(checkpointId: String) {
        context.aiCodingDataStore.edit { prefs ->
            val checkpoints = getCheckpoints(prefs).filter { it.id != checkpointId }
            prefs[KEY_CHECKPOINTS] = gson.toJson(checkpoints)
        }
    }

    private fun getSessions(prefs: Preferences): List<AiCodingSession> {
        val json = prefs[KEY_SESSIONS] ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<AiCodingSession>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCodeLibrary(prefs: Preferences): List<CodeLibraryItem> {
        val json = prefs[KEY_CODE_LIBRARY] ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<CodeLibraryItem>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCheckpoints(prefs: Preferences): List<ConversationCheckpoint> {
        val json = prefs[KEY_CHECKPOINTS] ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<ConversationCheckpoint>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractTitle(prompt: String): String {
        // Note.
        val firstLine = prompt.lines().firstOrNull()?.take(30) ?: "未命名项目"
        return if (firstLine.length < prompt.length) "$firstLine..." else firstLine
    }
}

/**
 * AI.
 * 
 * Note.
 * Note.
 * Note.
 * Note.
 * Note.
 * Note.
 */
object CodeBlockParser {
    
    /**
     * Note.
     * 
     * Note.
     * 1. ```language\ncode``` ( ).
     * 2. `language\ncode` ( AI ).
     * 3. ```language code``` ( ).
     * 4. ```\ncode``` ( ).
     * 
     * Note.
     */
    private val codeBlockRegex = Regex(
        """```(\w*)[ \t]*\r?\n?([\s\S]*?)```""",
        RegexOption.MULTILINE
    )
    
    // Note.
    private val singleBacktickRegex = Regex(
        """`\s*(html|css|js|javascript)\s*[\r\n]?([\s\S]*?)`(?!`)""",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )
    
    // File.
    // [^\n]+ [^\n]+?
    // File.
    private val filenameCommentRegex = Regex(
        """^(?:<!--\s*文件名[:：]\s*([^\n]+)\s*-->\s*\r?\n|/\*\s*文件名[:：]\s*([^\n]+)\s*\*/\s*\r?\n|//\s*文件名[:：]\s*([^\n]+)\s*\r?\n|#\s*文件名[:：]\s*([^\n]+)\s*\r?\n)""",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )
    
    private val thinkingRegex = Regex(
        """<thinking>([\s\S]*?)</thinking>|```thinking[ \t]*\r?\n?([\s\S]*?)```""",
        RegexOption.MULTILINE
    )
    
    private val imageGenRegex = Regex(
        """```image-gen[ \t]*\r?\n?([\s\S]*?)```""",
        RegexOption.MULTILINE
    )
    
    // --- Regex ---.
    
    // filenamePatterns (parseResponseLegacy per-code-block)
    private val FILENAME_PATTERNS = listOf(
        Regex("""^<!--\s*文件名[:：]\s*([^\n>]+?)\s*-->\s*\r?\n""", RegexOption.IGNORE_CASE),
        Regex("""^/\*\s*文件名[:：]\s*([^\n*]+?)\s*\*/\s*\r?\n""", RegexOption.IGNORE_CASE),
        Regex("""^//\s*文件名[:：]\s*(\S+)\s*\r?\n""", RegexOption.IGNORE_CASE),
        Regex("""^#\s*文件名[:：]\s*(\S+)\s*\r?\n""", RegexOption.IGNORE_CASE)
    )
    private val EXCESS_NEWLINES_REGEX = Regex("\n{3,}")
    
    // preprocessSingleBackticks
    private val BACKSLASH_CODE_BLOCK_REGEX = Regex(
        """\\[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)\\(?=\s|$)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val MULTI_BACKSLASH_CODE_BLOCK_REGEX = Regex(
        """\\+[ \t]*\\*[ \t]*(html|css|js|javascript)([\s\S]*?)\\+[ \t]*\\*""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val SINGLE_BACKTICK_CODE_BLOCK_REGEX = Regex(
        """(?<!`)`[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)`(?!`)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val UNCLOSED_BACKTICK_REGEX = Regex(
        """(?<!`)`[ \t]*(html|css|js|javascript)[ \t]*([^`]+)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )
    private val HTML_EXTRACT_REGEX = Regex(
        """(<!DOCTYPE\s+html[\s\S]*?</html>)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    
    // parseCodeBlocksFallback
    private val FALLBACK_TRIPLE_BACKTICK_REGEX = Regex("""```\s*(\w*)\s*([\s\S]*?)```""")
    private val FALLBACK_SINGLE_BACKTICK_REGEX = Regex(
        """`[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)`(?!`)""",
        RegexOption.IGNORE_CASE
    )
    private val FALLBACK_UNCLOSED_BACKTICK_REGEX = Regex(
        """`[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)(?=`[ \t]*(?:html|css|js|javascript)|$)""",
        RegexOption.IGNORE_CASE
    )
    
    // parseCodeBlocksByContent
    private val COMPLETE_HTML_PATTERN = Regex(
        """(<!DOCTYPE[\s\S]*?</html>|<html[\s\S]*?</html>)""",
        RegexOption.IGNORE_CASE
    )
    private val CSS_CONTENT_PATTERN = Regex(
        """(/\*[\s\S]*?\*/\s*)?([\w\-\.\#\[\]:\s,]+\s*\{[\s\S]*?\}[\s\S]*?)(?=\n\n|\z|/\*|//\s*文件名)"""
    )
    private val JS_CONTENT_PATTERN = Regex(
        """(//[^\n]*\n)?((?:function\s+\w+|const\s+\w+|let\s+\w+|var\s+\w+|document\.|window\.)[\s\S]*?)(?=\n\n\n|\z)"""
    )
    
    // fixJsNewlines
    private val JS_COMMENT_NEWLINE_REGEX = Regex("""(?<!:)(//[^\n]*?)(\s*)(const|let|var|function|if|else|for|while|switch|return|document|window|console|\$|\[|{)""")
    private val CALLBACK_END_REGEX = Regex("""\}\);(\s*)(?!\s*\n)(?=\S)""")
    private val BLOCK_END_SEMICOLON_REGEX = Regex("""\};(\s*)(?!\s*\n)(?=\S)""")
    private val BRACE_CLOSE_REGEX = Regex("""\}(\s*)(?!\s*[\n;,)\]])(?=\s*(?:const|let|var|function|if|else|for|while|switch|return|document|window|console|//|\$))""")
    private val STATEMENT_END_REGEX = Regex(""";(\s*)(?!\s*\n)(?=\s*(?:const|let|var|function|if|else|for|while|switch|return|document|window|console|//|\$|[a-zA-Z_]))""")
    
    // fixInlineJsSyntax / fixJsSyntaxErrors
    private val SCRIPT_TAG_REGEX = Regex("""(<script[^>]*>)([\s\S]*?)(</script>)""", RegexOption.IGNORE_CASE)
    private val ALERT_CHINESE_REGEX = Regex("""(alert|confirm|prompt)\s*\(\s*([^'"`\(\)]+[\u4e00-\u9fa5][^'"`\(\)]*)\s*\)""")
    private val CONSOLE_LOG_CHINESE_REGEX = Regex("""(console\.log)\s*\(\s*([^'"`\(\)]+[\u4e00-\u9fa5][^'"`\(\)]*)\s*\)""")
    
    // Gson singleton
    private val gson = com.webtoapp.util.GsonProvider.gson
    
    // cleanCodeBlockContent Regex.
    private val CLEAN_FILENAME_PATTERNS: Map<String, List<Regex>> = mapOf(
        "js" to listOf(
            Regex("""^//\s*文件名[:：]\s*[\w\-.]+\.js\s*""", RegexOption.IGNORE_CASE),
            Regex("""^/\*\s*文件名[:：]\s*[\w\-.]+\.js\s*\*/\s*""", RegexOption.IGNORE_CASE)
        ),
        "javascript" to listOf(
            Regex("""^//\s*文件名[:：]\s*[\w\-.]+\.js\s*""", RegexOption.IGNORE_CASE),
            Regex("""^/\*\s*文件名[:：]\s*[\w\-.]+\.js\s*\*/\s*""", RegexOption.IGNORE_CASE)
        ),
        "css" to listOf(
            Regex("""^/\*\s*文件名[:：]\s*[\w\-.]+\.css\s*\*/\s*""", RegexOption.IGNORE_CASE)
        ),
        "html" to listOf(
            Regex("""^<!--\s*文件名[:：]\s*[\w\-.]+\.html?\s*-->\s*""", RegexOption.IGNORE_CASE)
        )
    )
    
    // mergeToSingleHtmlLegacy
    private val INLINE_SCRIPT_REGEX = Regex("""<script(?![^>]*\bsrc\s*=)[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
    private val EXTERNAL_CSS_LINK_REGEX = Regex("""<link[^>]*href=["'](?!http)[^"']*\.css["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val EXTERNAL_JS_SCRIPT_REGEX = Regex("""<script[^>]*src=["'](?!http)[^"']*\.js["'][^>]*></script>""", RegexOption.IGNORE_CASE)
    private val EXTERNAL_JS_REF_REGEX = Regex("""<script[^>]*src=["'](?!http)([^"']*\.js)["'][^>]*></script>""", RegexOption.IGNORE_CASE)

    /** (AiCodingStorage.saveForPreview) SCRIPT_TAG_REGEX.*/
    fun findScriptTags(html: String): List<MatchResult> = SCRIPT_TAG_REGEX.findAll(html).toList()

    /**
     * Note.
     * 
     * AI.
     * 1. `js// File : script.js...`.
     * 2. \ html... \ \html...\.
     * Note.
     * 
     * Note.
     */
    private fun preprocessSingleBackticks(response: String): String {
        var result = response
        
        AppLogger.d("CodeBlockParser", "preprocessSingleBackticks: checking for non-standard code block patterns")
        AppLogger.d("CodeBlockParser", "Input preview: ${response.take(200).replace("\n", "\\n")}")
        
        // 1. \ html... \ \html...\.
        // AI.
        val backslashMatches = BACKSLASH_CODE_BLOCK_REGEX.findAll(result).toList()
        if (backslashMatches.isNotEmpty()) {
            AppLogger.d("CodeBlockParser", "Found ${backslashMatches.size} backslash-style code blocks")
            backslashMatches.reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                if (content.length > 20) {
                    AppLogger.d("CodeBlockParser", "Converting backslash block: language=$language, contentLength=${content.length}")
                    val replacement = "```$language\n$content\n```"
                    result = result.replaceRange(match.range, replacement)
                }
            }
        }
        
        // 2. \\ \ html... \\ \.
        val multiBackslashMatches = MULTI_BACKSLASH_CODE_BLOCK_REGEX.findAll(result).toList()
        if (multiBackslashMatches.isNotEmpty()) {
            AppLogger.d("CodeBlockParser", "Found ${multiBackslashMatches.size} multi-backslash code blocks")
            multiBackslashMatches.reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                if (content.length > 20) {
                    AppLogger.d("CodeBlockParser", "Converting multi-backslash block: language=$language, contentLength=${content.length}")
                    val replacement = "```$language\n$content\n```"
                    result = result.replaceRange(match.range, replacement)
                }
            }
        }
        
        // 3. `language...` ` language...`.
        val matches = SINGLE_BACKTICK_CODE_BLOCK_REGEX.findAll(result).toList()
        
        if (matches.isNotEmpty()) {
            AppLogger.d("CodeBlockParser", "Found ${matches.size} single-backtick code blocks, converting to triple-backtick format")
            
            // Note.
            matches.reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                
                AppLogger.d("CodeBlockParser", "Converting single-backtick block: language=$language, contentLength=${content.length}, preview=${content.take(50)}")
                
                // Note.
                val replacement = "```$language\n$content\n```"
                result = result.replaceRange(match.range, replacement)
            }
        } else {
            AppLogger.d("CodeBlockParser", "No single-backtick code blocks found")
            
            // Check.
            // `language... ( ).
            UNCLOSED_BACKTICK_REGEX.findAll(result).toList().reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                
                if (content.length > 50) { // Note.
                    AppLogger.d("CodeBlockParser", "Found unclosed single-backtick block: language=$language, contentLength=${content.length}")
                    
                    // Note.
                    val replacement = "```$language\n$content\n```"
                    result = result.replaceRange(match.range, replacement)
                }
            }
        }
        
        // 4. HTML.
        if (!result.contains("```") && result.contains("<!DOCTYPE html", ignoreCase = true)) {
            AppLogger.d("CodeBlockParser", "No code block markers found, but HTML content detected, attempting direct extraction")
            
            // <!DOCTYPE html> </html>.
            HTML_EXTRACT_REGEX.find(result)?.let { match ->
                val htmlContent = match.groupValues[1]
                AppLogger.d("CodeBlockParser", "Extracted HTML content directly, length=${htmlContent.length}")
                
                // HTML.
                val beforeHtml = result.substring(0, match.range.first)
                val afterHtml = if (match.range.last + 1 < result.length) result.substring(match.range.last + 1) else ""
                result = "$beforeHtml\n```html\n$htmlContent\n```\n$afterHtml"
            }
        }
        
        return result
    }

    /**
     * AI -.
     * Tool Calling.
     * backward compatible.
     */
    fun parseResponse(response: String): ParsedAiResponse {
        AppLogger.d("CodeBlockParser", "parseResponse: input length=${response.length}")
        
        // Legacy.
        return parseResponseLegacy(response)
    }
    
    /**
     * AI -.
     */
    fun parseResponseLegacy(response: String): ParsedAiResponse {
        var thinking: String? = null
        val codeBlocks = mutableListOf<CodeBlock>()
        val imageRequests = mutableListOf<ImageGenerationRequest>()
        
        AppLogger.d("CodeBlockParser", "parseResponseLegacy: input length=${response.length}")
        
        // Note.
        val processedResponse = preprocessSingleBackticks(response)
        var textContent = processedResponse
        
        // Note.
        thinkingRegex.find(processedResponse)?.let { match ->
            thinking = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
            textContent = textContent.replace(match.value, "")
        }
        
        // Note.
        imageGenRegex.findAll(processedResponse).forEach { match ->
            try {
                val json = match.groupValues[1].trim()
                val request = gson.fromJson(json, ImageGenerationRequest::class.java)
                imageRequests.add(request)
            } catch (e: Exception) {
                // Note.
            }
            textContent = textContent.replace(match.value, "[图像生成请求]")
        }
        
        // Note.
        val matchedRanges = mutableListOf<IntRange>()
        codeBlockRegex.findAll(processedResponse).forEach { match ->
            matchedRanges.add(match.range)
            val rawLanguage = match.groupValues[1].trim().lowercase()
            var rawContent = match.groupValues[2]
            
            // thinking image-gen.
            if (rawLanguage == "thinking" || rawLanguage == "image-gen") {
                return@forEach
            }
            
            // Note.
            // Note.
            var filename: String? = null
            
            // Check.
            for (pattern in FILENAME_PATTERNS) {
                pattern.find(rawContent)?.let { filenameMatch ->
                    val extractedFilename = filenameMatch.groupValues[1].trim()
                    // Verify.
                    if (extractedFilename.contains(".") && extractedFilename.length > 2) {
                        filename = extractedFilename
                        // Note.
                        rawContent = rawContent.substring(filenameMatch.range.last + 1)
                        AppLogger.d("CodeBlockParser", "Extracted filename: $filename from pattern: ${pattern.pattern}")
                    }
                    return@let
                }
                if (filename != null) break
            }
            
            val content = rawContent.trim()
            
            if (content.isNotEmpty()) {
                // Note.
                val language = inferLanguage(rawLanguage, content)
                
                // Generate.
                val actualFilename = filename?.takeIf { it.isNotBlank() } ?: getDefaultFilename(language)
                
                AppLogger.d("CodeBlockParser", "Parsed code block: language=$language, filename=$actualFilename, contentLength=${content.length}")
                AppLogger.d("CodeBlockParser", "Content start: ${content.take(50).replace("\n", "\\n")}")
                
                codeBlocks.add(
                    CodeBlock(
                        language = language,
                        filename = actualFilename,
                        content = content,
                        isComplete = isCompleteCode(language, content)
                    )
                )
            }
        }
        
        // Note.
        if (codeBlocks.isEmpty()) {
            parseCodeBlocksFallback(processedResponse, codeBlocks)
        }
        
        // Cleanup.
        textContent = codeBlockRegex.replace(textContent, "").trim()
        // Note.
        textContent = textContent.replace(EXCESS_NEWLINES_REGEX, "\n\n")
        
        return ParsedAiResponse(
            textContent = textContent,
            thinking = thinking,
            codeBlocks = codeBlocks,
            imageRequests = imageRequests
        )
    }
    
    /**
     * Note.
     * Note.
     */
    private fun parseCodeBlocksFallback(response: String, codeBlocks: MutableList<CodeBlock>) {
        AppLogger.d("CodeBlockParser", "parseCodeBlocksFallback: trying fallback parsing")
        
        var foundAny = false
        
        // Note.
        FALLBACK_TRIPLE_BACKTICK_REGEX.findAll(response).forEach { match ->
            val rawLanguage = match.groupValues[1].trim().lowercase()
            val rawContent = match.groupValues[2].trim()
            
            if (rawLanguage == "thinking" || rawLanguage == "image-gen") {
                return@forEach
            }
            
            if (rawContent.isNotEmpty() && rawContent.length > 10) {
                val language = inferLanguage(rawLanguage, rawContent)
                val filename = getDefaultFilename(language)
                
                codeBlocks.add(
                    CodeBlock(
                        language = language,
                        filename = filename,
                        content = rawContent,
                        isComplete = isCompleteCode(language, rawContent)
                    )
                )
                foundAny = true
            }
        }
        
        // Note.
        if (!foundAny) {
            FALLBACK_SINGLE_BACKTICK_REGEX.findAll(response).forEach { match ->
                val rawLanguage = match.groupValues[1].trim().lowercase()
                val rawContent = match.groupValues[2].trim()
                
                if (rawContent.isNotEmpty() && rawContent.length > 50) {
                    val language = inferLanguage(rawLanguage, rawContent)
                    val filename = getDefaultFilename(language)
                    
                    codeBlocks.add(
                        CodeBlock(
                            language = language,
                            filename = filename,
                            content = rawContent,
                            isComplete = isCompleteCode(language, rawContent)
                        )
                    )
                    foundAny = true
                    AppLogger.d("CodeBlockParser", "Fallback2 found: language=$language, contentLength=${rawContent.length}")
                }
            }
        }
        
        // Note.
        if (!foundAny) {
            AppLogger.d("CodeBlockParser", "Trying fallbackRegex3 for unclosed code blocks")
            FALLBACK_UNCLOSED_BACKTICK_REGEX.findAll(response).forEach { match ->
                val rawLanguage = match.groupValues[1].trim().lowercase()
                val rawContent = match.groupValues[2].trim()
                
                if (rawContent.isNotEmpty() && rawContent.length > 50) {
                    val language = inferLanguage(rawLanguage, rawContent)
                    val filename = getDefaultFilename(language)
                    
                    codeBlocks.add(
                        CodeBlock(
                            language = language,
                            filename = filename,
                            content = rawContent,
                            isComplete = isCompleteCode(language, rawContent)
                        )
                    )
                    foundAny = true
                    AppLogger.d("CodeBlockParser", "Fallback3 found: language=$language, contentLength=${rawContent.length}")
                }
            }
        }
        
        // Note.
        if (!foundAny) {
            AppLogger.d("CodeBlockParser", "Trying content-based parsing")
            parseCodeBlocksByContent(response, codeBlocks)
        }
    }
    
    /**
     * Note.
     * Note.
     */
    private fun parseCodeBlocksByContent(response: String, codeBlocks: MutableList<CodeBlock>) {
        AppLogger.d("CodeBlockParser", "parseCodeBlocksByContent: attempting content-based extraction")
        
        // Find HTML.
        // 1. HTML <!DOCTYPE <html </html>.
        var htmlContent: String? = null
        
        COMPLETE_HTML_PATTERN.find(response)?.let { match ->
            htmlContent = match.groupValues[1].trim()
            AppLogger.d("CodeBlockParser", "Found complete HTML, length=${htmlContent?.length}")
        }
        
        // 2. HTML HTML.
        if (htmlContent == null) {
            // Find <!DOCTYPE html>.
            val doctypeIndex = response.indexOf("<!DOCTYPE", ignoreCase = true)
            if (doctypeIndex >= 0) {
                // <!DOCTYPE </html> </body> </script>.
                val endHtmlIndex = response.indexOf("</html>", doctypeIndex, ignoreCase = true)
                val endBodyIndex = response.indexOf("</body>", doctypeIndex, ignoreCase = true)
                val lastScriptEndIndex = response.lastIndexOf("</script>", ignoreCase = true)
                
                val endIndex = when {
                    endHtmlIndex >= 0 -> endHtmlIndex + 7 // </html>.
                    endBodyIndex >= 0 -> endBodyIndex + 7 // </body> </html>.
                    lastScriptEndIndex >= doctypeIndex -> lastScriptEndIndex + 9 // </script>.
                    else -> response.length
                }
                
                htmlContent = response.substring(doctypeIndex, endIndex).trim()
                
                // </body> </html>.
                htmlContent?.let { html ->
                    var fixed = html
                    if (!fixed.contains("</body>", ignoreCase = true)) {
                        fixed = "$fixed\n</body>"
                    }
                    if (!fixed.contains("</html>", ignoreCase = true)) {
                        fixed = "$fixed\n</html>"
                    }
                    htmlContent = fixed
                }
                
                AppLogger.d("CodeBlockParser", "Extracted partial HTML from DOCTYPE, length=${htmlContent?.length}")
            }
        }
        
        // 3. <html>.
        if (htmlContent == null) {
            val htmlTagIndex = response.indexOf("<html", ignoreCase = true)
            if (htmlTagIndex >= 0) {
                val endIndex = response.indexOf("</html>", htmlTagIndex, ignoreCase = true)
                htmlContent = if (endIndex >= 0) {
                    response.substring(htmlTagIndex, endIndex + 7)
                } else {
                    response.substring(htmlTagIndex) + "\n</html>"
                }
                AppLogger.d("CodeBlockParser", "Extracted HTML from <html> tag, length=${htmlContent?.length}")
            }
        }
        
        htmlContent?.let { html ->
            if (html.length > 50) {
                codeBlocks.add(
                    CodeBlock(
                        language = "html",
                        filename = "index.html",
                        content = html,
                        isComplete = html.contains("</html>", ignoreCase = true)
                    )
                )
                AppLogger.d("CodeBlockParser", "Added HTML code block, length=${html.length}")
            }
        }
        
        // Find CSS.
        val cssMatches = CSS_CONTENT_PATTERN.findAll(response)
        val cssContent = StringBuilder()
        cssMatches.forEach { match ->
            val content = match.value.trim()
            // CSS JS.
            if (content.contains("{") && content.contains("}") && 
                content.contains(":") && content.contains(";") &&
                !content.contains("function") && !content.contains("=>") &&
                !content.contains("const ") && !content.contains("let ")) {
                cssContent.append(content).append("\n\n")
            }
        }
        if (cssContent.isNotEmpty() && cssContent.length > 50) {
            codeBlocks.add(
                CodeBlock(
                    language = "css",
                    filename = "styles.css",
                    content = cssContent.toString().trim(),
                    isComplete = true
                )
            )
        }
        
        // Find JS.
        val jsMatches = JS_CONTENT_PATTERN.findAll(response)
        val jsContent = StringBuilder()
        jsMatches.forEach { match ->
            val content = match.value.trim()
            if (content.length > 30) {
                jsContent.append(content).append("\n\n")
            }
        }
        if (jsContent.isNotEmpty() && jsContent.length > 50) {
            codeBlocks.add(
                CodeBlock(
                    language = "js",
                    filename = "script.js",
                    content = jsContent.toString().trim(),
                    isComplete = true
                )
            )
        }
    }
    
    /**
     * Note.
     */
    private fun inferLanguage(rawLanguage: String, content: String): String {
        return when {
            rawLanguage.isNotEmpty() -> when (rawLanguage) {
                "javascript" -> "js"
                "htm" -> "html"
                "stylesheet" -> "css"
                else -> rawLanguage
            }
            // Note.
            content.contains("<!DOCTYPE", ignoreCase = true) || 
            content.contains("<html", ignoreCase = true) ||
            (content.trimStart().startsWith("<") && content.contains(">")) -> "html"
            content.trimStart().startsWith("{") && content.contains(":") -> {
                // CSS JSON.
                if (content.contains("\"") && !content.contains(";")) "json" else "css"
            }
            content.contains("function") || content.contains("const ") || 
            content.contains("let ") || content.contains("var ") ||
            content.contains("=>") -> "js"
            content.contains("{") && content.contains(";") -> "css"
            else -> "html"  // Default html.
        }
    }
    
    /**
     * Note.
     */
    private fun getDefaultFilename(language: String): String {
        return when (language) {
            "html" -> "index.html"
            "css" -> "styles.css"
            "js" -> "script.js"
            "svg" -> "image.svg"
            "json" -> "data.json"
            else -> "file.$language"
        }
    }
    
    /**
     * Note.
     */
    private fun isCompleteCode(language: String, content: String): Boolean {
        return when (language) {
            "html" -> content.contains("<!DOCTYPE", ignoreCase = true) || 
                     content.contains("<html", ignoreCase = true)
            "css" -> true  // CSS.
            "js" -> true
            else -> true
        }
    }
    
    /**
     * Note.
     * 
     * Note.
     * - // File : script.js (JS ).
     * comment.
     * file name comment.
     */
    private fun cleanCodeBlockContent(content: String, language: String): String {
        var cleaned = content.trim()
        
        // Note.
        // Note.
        val patterns = CLEAN_FILENAME_PATTERNS[language] ?: emptyList()
        
        for (pattern in patterns) {
            val match = pattern.find(cleaned)
            if (match != null) {
                val matchedText = match.value
                val before = cleaned
                cleaned = cleaned.substring(match.range.last + 1).trim()
                AppLogger.d("CodeBlockParser", "cleanCodeBlockContent [$language]: Removed filename comment '${matchedText.trim()}'")
                AppLogger.d("CodeBlockParser", "cleanCodeBlockContent [$language]: Code now starts with: '${cleaned.take(80).replace("\n", "\\n")}'")
            }
        }
        
        // JS.
        if (language == "js" || language == "javascript") {
            cleaned = fixJsNewlines(cleaned)
        }
        
        return cleaned
    }
    
    /**
     * JS.
     * 
     * AI JS //.
     * // const skillItems = ...
     * 
     * Note.
     * Note.
     * Note.
     */
    private fun fixJsNewlines(code: String): String {
        var result = code
        
        // Check.
        val lineCount = result.count { it == '\n' }
        val codeLength = result.length
        
        // Note.
        if (codeLength > 200 && lineCount < 5) {
            AppLogger.d("CodeBlockParser", "fixJsNewlines: Code appears to be minified (length=$codeLength, lines=$lineCount), attempting to fix")
            
            // Note.
            // Note.
            // URL // ( http://).
            result = result.replace(JS_COMMENT_NEWLINE_REGEX) { match ->
                val comment = match.groupValues[1]
                val code = match.groupValues[3]
                "$comment\n$code"
            }
            
            // Note.
            result = result.replace(CALLBACK_END_REGEX) { "});\n" }
            
            // Note.
            result = result.replace(BLOCK_END_SEMICOLON_REGEX) { "};\n" }
            
            // Note.
            result = result.replace(BRACE_CLOSE_REGEX) { "}\n" }
            
            // 5. ; for.
            // Note.
            result = result.replace(STATEMENT_END_REGEX) { ";\n" }
            
            val newLineCount = result.count { it == '\n' }
            AppLogger.d("CodeBlockParser", "fixJsNewlines: After fix, lines=$newLineCount")
            AppLogger.d("CodeBlockParser", "fixJsNewlines: First 300 chars: ${result.take(300).replace("\n", "\\n")}")
        }
        
        return result
    }
    
    /**
     * HTML JS.
     * 
     * AI JS.
     * - alert( ) alert(' ').
     * Note.
     */
    private fun fixInlineJsSyntax(html: String): String {
        var result = html
        
        // <script> JS.
        result = SCRIPT_TAG_REGEX.replace(result) { match ->
            val openTag = match.groupValues[1]
            var jsContent = match.groupValues[2]
            val closeTag = match.groupValues[3]
            
            // src.
            if (!openTag.contains("src=", ignoreCase = true) && jsContent.isNotBlank()) {
                jsContent = fixJsSyntaxErrors(jsContent)
            }
            
            "$openTag$jsContent$closeTag"
        }
        
        return result
    }
    
    /**
     * JS.
     */
    private fun fixJsSyntaxErrors(js: String): String {
        var result = js
        
        // 1. alert/confirm/prompt.
        // alert( ) -> alert(' ').
        result = result.replace(ALERT_CHINESE_REGEX) { match ->
            val func = match.groupValues[1]
            val content = match.groupValues[2].trim()
            "$func('$content')"
        }
        
        // 2. console.log.
        result = result.replace(CONSOLE_LOG_CHINESE_REGEX) { match ->
            val func = match.groupValues[1]
            val content = match.groupValues[2].trim()
            "$func('$content')"
        }
        
        // Note.
        // Note.
        
        AppLogger.d("CodeBlockParser", "fixJsSyntaxErrors: Applied syntax fixes")
        
        return result
    }
    
    /**
     * HTML.
     * 
     * HTML -.
     */
    fun mergeToSingleHtml(codeBlocks: List<CodeBlock>): String {
        AppLogger.d("CodeBlockParser", "mergeToSingleHtml: ${codeBlocks.size} blocks")
        
        val html = codeBlocks.find { it.language == "html" }?.content
        val css = codeBlocks.filter { it.language == "css" }.joinToString("\n\n") { it.content }
        val js = codeBlocks.filter { it.language == "js" || it.language == "javascript" }.joinToString("\n\n") { it.content }
        
        // HTML CSS/JS.
        if (html != null && (html.contains("<html", ignoreCase = true) || html.contains("<!DOCTYPE", ignoreCase = true))) {
            var result = html
            
            // CSS.
            if (css.isNotBlank() && !html.contains(css.take(50))) {
                val styleTag = "<style>\n$css\n</style>"
                result = insertBefore(result, styleTag, "</head>", "<body")
            }
            
            // JS.
            if (js.isNotBlank() && !html.contains(js.take(50))) {
                val scriptTag = "<script>\n$js\n</script>"
                result = insertBefore(result, scriptTag, "</body>", "</html>")
            }
            
            return result
        }
        
        // HTML.
        return buildHtml(html ?: "", css, js)
    }
    
    private fun insertBefore(html: String, content: String, vararg tags: String): String {
        for (tag in tags) {
            val index = html.indexOf(tag, ignoreCase = true)
            if (index >= 0) {
                return html.substring(0, index) + content + "\n" + html.substring(index)
            }
        }
        return html + "\n" + content
    }
    
    private fun buildHtml(body: String, css: String, js: String): String {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>预览</title>
    ${if (css.isNotBlank()) "<style>\n$css\n</style>" else ""}
</head>
<body>
$body
${if (js.isNotBlank()) "<script>\n$js\n</script>" else ""}
</body>
</html>
        """.trimIndent()
    }
    
    /**
     * HTML -.
     */
    fun mergeToSingleHtmlLegacy(codeBlocks: List<CodeBlock>): String {
        val htmlBlocks = codeBlocks.filter { it.language == "html" }
        val cssBlocks = codeBlocks.filter { it.language == "css" }
        val jsBlocks = codeBlocks.filter { it.language == "js" || it.language == "javascript" }
        
        AppLogger.d("CodeBlockParser", "mergeToSingleHtmlLegacy: htmlBlocks=${htmlBlocks.size}, cssBlocks=${cssBlocks.size}, jsBlocks=${jsBlocks.size}")
        codeBlocks.forEach { block ->
            AppLogger.d("CodeBlockParser", "  Block: language=${block.language}, filename=${block.filename}, contentLength=${block.content.length}")
            // Note.
            AppLogger.d("CodeBlockParser", "  Content preview: ${block.content.take(200).replace("\n", "\\n")}")
        }
        
        // HTML CSS JS.
        val mainHtml = htmlBlocks.find { it.isComplete }?.content
        
        if (mainHtml != null) {
            var result = mainHtml
            
            // HTML script.
            val scriptMatches = SCRIPT_TAG_REGEX.findAll(mainHtml).toList()
            AppLogger.d("CodeBlockParser", "Original HTML has ${scriptMatches.size} script tags")
            scriptMatches.forEachIndexed { index, match ->
                val scriptContent = match.groupValues[2]
                AppLogger.d("CodeBlockParser", "  Script $index: length=${scriptContent.length}, preview=${scriptContent.take(100).replace("\n", "\\n")}")
            }
            
            // Check HTML style script.
            val hasInlineStyle = result.contains("<style", ignoreCase = true)
            // Check script src.
            // <script>...</script> <script src="..."></script>.
            val inlineScriptMatches = INLINE_SCRIPT_REGEX.findAll(result).toList()
            val hasInlineScript = inlineScriptMatches.any { match ->
                val scriptContent = match.groupValues[1].trim()
                scriptContent.isNotEmpty() && scriptContent.length > 5  // Note.
            }
            
            AppLogger.d("CodeBlockParser", "hasInlineStyle=$hasInlineStyle, hasInlineScript=$hasInlineScript")
            if (hasInlineScript) {
                inlineScriptMatches.forEach { match ->
                    AppLogger.d("CodeBlockParser", "Found inline script, length=${match.groupValues[1].length}")
                }
            }
            
            // Note.
            // No.
            if (cssBlocks.isNotEmpty()) {
                result = result.replace(EXTERNAL_CSS_LINK_REGEX, "")
            }
            if (jsBlocks.isNotEmpty()) {
                result = result.replace(EXTERNAL_JS_SCRIPT_REGEX, "")
            }
            
            // </head> CSS CSS HTML.
            // HTML CSS.
            if (cssBlocks.isNotEmpty()) {
                // Cleanup CSS.
                val cleanedCssBlocks = cssBlocks.map { block ->
                    cleanCodeBlockContent(block.content, block.language)
                }
                val cssContent = cleanedCssBlocks.joinToString("\n\n")
                val styleTag = "<style>\n$cssContent\n</style>"
                
                result = when {
                    result.contains("</head>", ignoreCase = true) -> {
                        replaceFirstIgnoreCase(result, "</head>", "$styleTag\n</head>")
                    }
                    result.contains("<body", ignoreCase = true) -> {
                        replaceFirstIgnoreCase(result, "<body", "$styleTag\n<body")
                    }
                    result.contains("<html", ignoreCase = true) -> {
                        // <html> <head> — indexOf <html...>.
                        val htmlTagStart = result.indexOf("<html", ignoreCase = true)
                        val htmlTagEnd = result.indexOf(">", htmlTagStart)
                        if (htmlTagEnd >= 0) {
                            result.substring(0, htmlTagEnd + 1) + "\n<head>\n$styleTag\n</head>" + result.substring(htmlTagEnd + 1)
                        } else {
                            "$styleTag\n$result"
                        }
                    }
                    else -> {
                        // Note.
                        "$styleTag\n$result"
                    }
                }
            }
            
            // </body> JS DOM.
            // JS.
            if (jsBlocks.isNotEmpty()) {
                // Cleanup JS.
                val cleanedJsBlocks = jsBlocks.map { block ->
                    cleanCodeBlockContent(block.content, block.language)
                }
                val jsContent = cleanedJsBlocks.joinToString("\n\n")
                AppLogger.d("CodeBlockParser", "Adding JS content, length=${jsContent.length}")
                AppLogger.d("CodeBlockParser", "JS content preview: ${jsContent.take(200)}...")
                // HTML JS.
                // JS.
                val finalJs = if (hasInlineScript) {
                    // HTML.
                    jsContent
                } else {
                    // JS DOM.
                    wrapJsForDomReady(jsContent)
                }
                val scriptTag = "<script>\n$finalJs\n</script>"
                
                val beforeLength = result.length
                result = when {
                    result.contains("</body>", ignoreCase = true) -> {
                        replaceFirstIgnoreCase(result, "</body>", "$scriptTag\n</body>")
                    }
                    result.contains("</html>", ignoreCase = true) -> {
                        replaceFirstIgnoreCase(result, "</html>", "$scriptTag\n</html>")
                    }
                    else -> {
                        // Note.
                        "$result\n$scriptTag"
                    }
                }
                AppLogger.d("CodeBlockParser", "JS inserted, beforeLength=$beforeLength, afterLength=${result.length}")
            } else {
                AppLogger.d("CodeBlockParser", "No JS blocks to add")
                // Check HTML JS.
                val externalJsMatches = EXTERNAL_JS_REF_REGEX.findAll(result)
                externalJsMatches.forEach { match ->
                    AppLogger.w("CodeBlockParser", "WARNING: HTML references external JS file '${match.groupValues[1]}' but no JS code block found!")
                }
            }
            
            // script.
            val finalScriptMatches = SCRIPT_TAG_REGEX.findAll(result).toList()
            AppLogger.d("CodeBlockParser", "Final HTML has ${finalScriptMatches.size} script tags")
            finalScriptMatches.forEachIndexed { index, match ->
                val scriptContent = match.groupValues[2]
                AppLogger.d("CodeBlockParser", "  Final Script $index: length=${scriptContent.length}")
            }
            
            // JS.
            result = fixInlineJsSyntax(result)
            
            return result
        }
        
        // HTML HTML5.
        val css = cssBlocks.joinToString("\n\n") { it.content }
        val js = jsBlocks.joinToString("\n\n") { it.content }
        val htmlContent = htmlBlocks.joinToString("\n") { it.content }
        
        // JS DOM.
        val wrappedJs = if (js.isNotEmpty()) wrapJsForDomReady(js) else ""
        
        val builtHtml = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>预览</title>
    ${if (css.isNotEmpty()) "<style>\n$css\n</style>" else ""}
</head>
<body>
$htmlContent
${if (wrappedJs.isNotEmpty()) "<script>\n$wrappedJs\n</script>" else ""}
</body>
</html>
        """.trimIndent()
        
        // JS.
        return fixInlineJsSyntax(builtHtml)
    }
    
    /**
     * JS DOM.
     * JS DOM.
     * 
     * Note.
     */
    /**
     * replaceFirst Regex.
     */
    private fun replaceFirstIgnoreCase(source: String, target: String, replacement: String): String {
        val index = source.indexOf(target, ignoreCase = true)
        if (index < 0) return source
        return source.substring(0, index) + replacement + source.substring(index + target.length)
    }
    
    private fun wrapJsForDomReady(jsContent: String): String {
        val trimmedContent = jsContent.trim()
        if (trimmedContent.isEmpty()) return ""
        
        // Check JS DOMContentLoaded window.onload.
        val hasWrapper = trimmedContent.contains("DOMContentLoaded", ignoreCase = true) ||
                        trimmedContent.contains("window.onload", ignoreCase = true) ||
                        trimmedContent.contains("addEventListener('load'", ignoreCase = true) ||
                        trimmedContent.contains("addEventListener(\"load\"", ignoreCase = true) ||
                        trimmedContent.contains("\$(document).ready", ignoreCase = true) ||
                        trimmedContent.contains("\$(function()", ignoreCase = true)
        
        return if (hasWrapper) {
            trimmedContent
        } else {
            // JS.
            // </body> DOM.
            // onclick.
            trimmedContent
        }
    }
}