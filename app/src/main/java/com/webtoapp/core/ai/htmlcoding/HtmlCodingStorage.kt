package com.webtoapp.core.ai.htmlcoding

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

private val Context.htmlCodingDataStore: DataStore<Preferences> by preferencesDataStore(name = "html_coding")

/**
 * HTML编程AI - 存储管理
 * 负责会话历史、版本控制（检查点）、文件保存
 */
class HtmlCodingStorage(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val KEY_SESSIONS = stringPreferencesKey("sessions")
        private val KEY_CURRENT_SESSION_ID = stringPreferencesKey("current_session_id")
        private val KEY_CODE_LIBRARY = stringPreferencesKey("code_library")
        private val KEY_CHECKPOINTS = stringPreferencesKey("conversation_checkpoints")
        
        // File存储目录
        private const val HTML_CODING_DIR = "HtmlCoding"
        private const val SESSIONS_DIR = "sessions"
        private const val PROJECTS_DIR = "projects"
        private const val IMAGES_DIR = "images"
        private const val CODE_LIBRARY_DIR = "code_library"
    }

    // ==================== 会话管理 ====================

    /**
     * 所有会话列表 Flow
     */
    val sessionsFlow: Flow<List<HtmlCodingSession>> = context.htmlCodingDataStore.data.map { prefs ->
        val json = prefs[KEY_SESSIONS] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<HtmlCodingSession>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 当前会话ID Flow
     */
    val currentSessionIdFlow: Flow<String?> = context.htmlCodingDataStore.data.map { prefs ->
        prefs[KEY_CURRENT_SESSION_ID]
    }
    
    // 项目文件管理器
    private val projectFileManager = ProjectFileManager(context)

    /**
     * 创建新会话（同时创建项目文件夹）
     */
    suspend fun createSession(title: String = ""): HtmlCodingSession {
        val sessionId = UUID.randomUUID().toString()
        // Create项目文件夹
        val projectDir = projectFileManager.getSessionProjectDir(sessionId)
        
        val session = HtmlCodingSession(
            id = sessionId,
            title = title,
            projectDir = projectDir.absolutePath
        )
        
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            sessions.add(0, session)  // 新会话放在最前面
            prefs[KEY_SESSIONS] = gson.toJson(sessions)
            prefs[KEY_CURRENT_SESSION_ID] = session.id
        }
        return session
    }
    
    /**
     * 获取项目文件管理器
     */
    fun getProjectFileManager(): ProjectFileManager = projectFileManager

    /**
     * 更新会话
     */
    suspend fun updateSession(session: HtmlCodingSession) {
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val index = sessions.indexOfFirst { it.id == session.id }
            if (index >= 0) {
                sessions[index] = session.copy(updatedAt = System.currentTimeMillis())
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
    }

    /**
     * 删除会话
     */
    suspend fun deleteSession(sessionId: String) {
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).filter { it.id != sessionId }
            prefs[KEY_SESSIONS] = gson.toJson(sessions)
            
            // 如果删除的是当前会话，清除当前会话ID
            if (prefs[KEY_CURRENT_SESSION_ID] == sessionId) {
                prefs.remove(KEY_CURRENT_SESSION_ID)
            }
        }
        
        // Delete会话相关文件
        deleteSessionFiles(sessionId)
    }

    /**
     * 设置当前会话
     */
    suspend fun setCurrentSession(sessionId: String) {
        context.htmlCodingDataStore.edit { prefs ->
            prefs[KEY_CURRENT_SESSION_ID] = sessionId
        }
    }

    /**
     * 获取会话
     */
    suspend fun getSession(sessionId: String): HtmlCodingSession? {
        return sessionsFlow.first().find { it.id == sessionId }
    }

    /**
     * 获取当前会话
     */
    suspend fun getCurrentSession(): HtmlCodingSession? {
        val currentId = currentSessionIdFlow.first() ?: return null
        return getSession(currentId)
    }

    // ==================== 消息管理 ====================

    /**
     * 添加消息到会话
     */
    suspend fun addMessage(sessionId: String, message: HtmlCodingMessage): HtmlCodingSession? {
        var updatedSession: HtmlCodingSession? = null
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val index = sessions.indexOfFirst { it.id == sessionId }
            if (index >= 0) {
                val session = sessions[index]
                val newMessages = session.messages + message
                updatedSession = session.copy(
                    messages = newMessages,
                    updatedAt = System.currentTimeMillis()
                )
                sessions[index] = updatedSession!!
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
        return updatedSession
    }

    /**
     * 更新消息（用于流式更新或编辑）
     */
    suspend fun updateMessage(sessionId: String, message: HtmlCodingMessage): HtmlCodingSession? {
        var updatedSession: HtmlCodingSession? = null
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val newMessages = session.messages.map { 
                    if (it.id == message.id) message else it 
                }
                updatedSession = session.copy(
                    messages = newMessages,
                    updatedAt = System.currentTimeMillis()
                )
                sessions[sessionIndex] = updatedSession!!
                prefs[KEY_SESSIONS] = gson.toJson(sessions)
            }
        }
        return updatedSession
    }

    /**
     * 编辑用户消息（重编辑功能）
     * 会创建一个检查点并回滚后续消息
     */
    suspend fun editUserMessage(
        sessionId: String, 
        messageId: String, 
        newContent: String,
        newImages: List<String> = emptyList()
    ): HtmlCodingSession? {
        var updatedSession: HtmlCodingSession? = null
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val messageIndex = session.messages.indexOfFirst { it.id == messageId }
                
                if (messageIndex >= 0) {
                    val oldMessage = session.messages[messageIndex]
                    
                    // Create检查点（保存编辑前的状态）
                    val checkpoint = createCheckpointFromSession(session, messageIndex, "编辑前自动保存")
                    
                    // Update消息内容，保留原始内容
                    val editedMessage = oldMessage.copy(
                        content = newContent,
                        images = newImages,
                        isEdited = true,
                        originalContent = oldMessage.originalContent ?: oldMessage.content
                    )
                    
                    // Delete该消息之后的所有消息（回滚）
                    val newMessages = session.messages.subList(0, messageIndex) + editedMessage
                    
                    // Update检查点列表
                    val newCheckpoints = session.checkpoints + checkpoint
                    
                    updatedSession = session.copy(
                        messages = newMessages,
                        checkpoints = newCheckpoints,
                        currentCheckpointIndex = newCheckpoints.size - 1,
                        updatedAt = System.currentTimeMillis()
                    )
                    sessions[sessionIndex] = updatedSession!!
                    prefs[KEY_SESSIONS] = gson.toJson(sessions)
                }
            }
        }
        return updatedSession
    }

    // ==================== 版本控制（检查点） ====================

    /**
     * 创建检查点
     */
    suspend fun createCheckpoint(
        sessionId: String, 
        name: String,
        description: String = ""
    ): ProjectCheckpoint? {
        var checkpoint: ProjectCheckpoint? = null
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                checkpoint = createCheckpointFromSession(session, session.messages.size, name, description)
                
                val newCheckpoints = session.checkpoints + checkpoint!!
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

    /**
     * 回滚到检查点
     */
    suspend fun rollbackToCheckpoint(sessionId: String, checkpointId: String): HtmlCodingSession? {
        var updatedSession: HtmlCodingSession? = null
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                val checkpointIndex = session.checkpoints.indexOfFirst { it.id == checkpointId }
                
                if (checkpointIndex >= 0) {
                    val checkpoint = session.checkpoints[checkpointIndex]
                    
                    // 回滚消息到检查点位置
                    val restoredMessages = session.messages.take(checkpoint.messageIndex)
                    
                    updatedSession = session.copy(
                        messages = restoredMessages,
                        currentCheckpointIndex = checkpointIndex,
                        updatedAt = System.currentTimeMillis()
                    )
                    sessions[sessionIndex] = updatedSession!!
                    prefs[KEY_SESSIONS] = gson.toJson(sessions)
                }
            }
        }
        return updatedSession
    }

    /**
     * 删除检查点
     */
    suspend fun deleteCheckpoint(sessionId: String, checkpointId: String) {
        context.htmlCodingDataStore.edit { prefs ->
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

    /**
     * 从会话创建检查点
     */
    private fun createCheckpointFromSession(
        session: HtmlCodingSession,
        messageIndex: Int,
        name: String,
        description: String = ""
    ): ProjectCheckpoint {
        // 从消息中提取所有代码文件
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
                // Update或添加文件
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

    // ==================== 文件操作 ====================

    /**
     * 获取HTML编程根目录
     */
    fun getHtmlCodingDir(): File {
        val dir = File(context.getExternalFilesDir(null), HTML_CODING_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get project directory
     */
    fun getProjectsDir(): File {
        val dir = File(getHtmlCodingDir(), PROJECTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取图片目录
     */
    fun getImagesDir(): File {
        val dir = File(getHtmlCodingDir(), IMAGES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 保存项目到指定目录
     */
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
                
                // Create父目录
                targetFile.parentFile?.mkdirs()
                
                // Check是否覆盖
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

    /**
     * 保存单个HTML文件用于预览
     */
    fun saveForPreview(content: String, filename: String = "preview.html"): File {
        val previewDir = File(getHtmlCodingDir(), "preview")
        if (!previewDir.exists()) previewDir.mkdirs()
        
        val file = File(previewDir, filename)
        file.writeText(content)
        
        // 输出保存的文件路径和内容长度用于调试
        android.util.Log.d("HtmlCodingStorage", "saveForPreview: path=${file.absolutePath}, contentLength=${content.length}")
        
        // Check保存的内容中是否有 script 标签
        val scriptRegex = Regex("""<script[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
        val scriptMatches = scriptRegex.findAll(content).toList()
        android.util.Log.d("HtmlCodingStorage", "Saved HTML has ${scriptMatches.size} script tags")
        scriptMatches.forEachIndexed { index, match ->
            val scriptContent = match.groupValues[1]
            android.util.Log.d("HtmlCodingStorage", "  Saved Script $index: length=${scriptContent.length}, preview=${scriptContent.take(100).replace("\n", "\\n")}")
        }
        
        return file
    }

    /**
     * 保存图片
     */
    fun saveImage(imageBytes: ByteArray, filename: String? = null): File {
        val actualFilename = filename ?: "${UUID.randomUUID()}.png"
        val file = File(getImagesDir(), actualFilename)
        file.writeBytes(imageBytes)
        return file
    }

    /**
     * 复制用户选择的图片到存储目录
     */
    suspend fun copyImageToStorage(sourcePath: String): String {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return sourcePath
        
        val filename = "${UUID.randomUUID()}_${sourceFile.name}"
        val targetFile = File(getImagesDir(), filename)
        sourceFile.copyTo(targetFile, overwrite = true)
        return targetFile.absolutePath
    }

    /**
     * 删除会话相关文件
     */
    private fun deleteSessionFiles(sessionId: String) {
        val sessionDir = File(getHtmlCodingDir(), "$SESSIONS_DIR/$sessionId")
        if (sessionDir.exists()) {
            sessionDir.deleteRecursively()
        }
    }

    /**
     * 获取可用的保存目录列表
     */
    fun getAvailableSaveDirectories(): List<Pair<String, File>> {
        val dirs = mutableListOf<Pair<String, File>>()
        
        // App私有目录
        dirs.add("应用目录" to getProjectsDir())
        
        // Download目录
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let {
            if (it.exists() || it.mkdirs()) {
                dirs.add("下载目录" to it)
            }
        }
        
        // 文档目录
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let {
            if (it.exists() || it.mkdirs()) {
                dirs.add("文档目录" to it)
            }
        }
        
        return dirs
    }

    // ==================== 代码库管理 ====================

    /**
     * 代码库列表 Flow
     */
    val codeLibraryFlow: Flow<List<CodeLibraryItem>> = context.htmlCodingDataStore.data.map { prefs ->
        val json = prefs[KEY_CODE_LIBRARY] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<CodeLibraryItem>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加到代码库（AI输出时自动调用）
     */
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
        
        context.htmlCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).toMutableList()
            library.add(0, item)
            prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
        }
        
        // Save预览文件
        saveCodeLibraryFiles(item)
        
        return item
    }

    /**
     * 获取代码库项目
     */
    suspend fun getCodeLibraryItem(itemId: String): CodeLibraryItem? {
        return codeLibraryFlow.first().find { it.id == itemId }
    }

    /**
     * 更新代码库项目
     */
    suspend fun updateCodeLibraryItem(item: CodeLibraryItem) {
        context.htmlCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).toMutableList()
            val index = library.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                library[index] = item
                prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
            }
        }
    }

    /**
     * 删除代码库项目
     */
    suspend fun deleteCodeLibraryItem(itemId: String) {
        context.htmlCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).filter { it.id != itemId }
            prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
        }
        // Delete相关文件
        deleteCodeLibraryFiles(itemId)
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(itemId: String) {
        context.htmlCodingDataStore.edit { prefs ->
            val library = getCodeLibrary(prefs).toMutableList()
            val index = library.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                library[index] = library[index].copy(isFavorite = !library[index].isFavorite)
                prefs[KEY_CODE_LIBRARY] = gson.toJson(library)
            }
        }
    }

    /**
     * 导出到项目库
     */
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

    /**
     * 获取代码库目录
     */
    fun getCodeLibraryDir(): File {
        val dir = File(getHtmlCodingDir(), CODE_LIBRARY_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 保存代码库项目文件
     */
    private fun saveCodeLibraryFiles(item: CodeLibraryItem) {
        val itemDir = File(getCodeLibraryDir(), item.id)
        if (!itemDir.exists()) itemDir.mkdirs()
        
        // Save预览HTML
        File(itemDir, "preview.html").writeText(item.previewHtml)
        
        // Save各个文件
        item.files.forEach { file ->
            File(itemDir, file.name).writeText(file.content)
        }
    }

    /**
     * 删除代码库项目文件
     */
    private fun deleteCodeLibraryFiles(itemId: String) {
        val itemDir = File(getCodeLibraryDir(), itemId)
        if (itemDir.exists()) {
            itemDir.deleteRecursively()
        }
    }

    /**
     * 获取代码库预览文件
     */
    fun getCodeLibraryPreviewFile(itemId: String): File? {
        val file = File(getCodeLibraryDir(), "$itemId/preview.html")
        return if (file.exists()) file else null
    }

    // ==================== 增强检查点管理 ====================

    /**
     * 检查点列表 Flow
     */
    val checkpointsFlow: Flow<List<ConversationCheckpoint>> = context.htmlCodingDataStore.data.map { prefs ->
        val json = prefs[KEY_CHECKPOINTS] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<ConversationCheckpoint>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 创建对话检查点（每次对话后自动调用）
     */
    suspend fun createConversationCheckpoint(
        sessionId: String,
        name: String = "自动保存"
    ): ConversationCheckpoint? {
        val session = getSession(sessionId) ?: return null
        
        // Get当前会话关联的代码库项目ID
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
        
        context.htmlCodingDataStore.edit { prefs ->
            val checkpoints = getCheckpoints(prefs).toMutableList()
            // 同一会话只保留最近10个检查点
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

    /**
     * 获取会话的检查点列表
     */
    suspend fun getSessionCheckpoints(sessionId: String): List<ConversationCheckpoint> {
        return checkpointsFlow.first()
            .filter { it.sessionId == sessionId }
            .sortedByDescending { it.timestamp }
    }

    /**
     * 回退到检查点（完整回退：消息 + 代码库）
     */
    suspend fun rollbackToConversationCheckpoint(checkpointId: String): HtmlCodingSession? {
        val checkpoints = checkpointsFlow.first()
        val checkpoint = checkpoints.find { it.id == checkpointId } ?: return null
        
        // 回退会话消息
        context.htmlCodingDataStore.edit { prefs ->
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
            
            // 回退代码库：删除检查点之后创建的项目
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

    /**
     * 删除检查点
     */
    suspend fun deleteConversationCheckpoint(checkpointId: String) {
        context.htmlCodingDataStore.edit { prefs ->
            val checkpoints = getCheckpoints(prefs).filter { it.id != checkpointId }
            prefs[KEY_CHECKPOINTS] = gson.toJson(checkpoints)
        }
    }

    // ==================== 辅助方法 ====================

    private fun getSessions(prefs: Preferences): List<HtmlCodingSession> {
        val json = prefs[KEY_SESSIONS] ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<HtmlCodingSession>>() {}.type)
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
        // 从用户提问中提取标题
        val firstLine = prompt.lines().firstOrNull()?.take(30) ?: "未命名项目"
        return if (firstLine.length < prompt.length) "$firstLine..." else firstLine
    }
}

/**
 * 从AI响应中解析代码块
 * 
 * 修复问题：
 * 1. 正则表达式必须要求语言标识符后有换行符，避免语言和代码内容混淆
 * 2. 使用更精确的正则表达式匹配代码块
 * 3. 确保文件名不会和代码内容拼接
 * 4. 支持没有语言标识符的代码块
 * 5. 支持多种代码块格式（带/不带文件名注释）
 */
object CodeBlockParser {
    
    /**
     * 代码块正则表达式
     * 
     * 支持多种格式：
     * 1. ```language\ncode``` (标准三反引号)
     * 2. `language\ncode` (单反引号，某些AI会这样输出)
     * 3. ```language code``` (语言后直接是代码)
     * 4. ```\ncode``` (无语言标识)
     * 
     * 关键：使用非贪婪匹配，确保正确匹配结束的反引号
     */
    private val codeBlockRegex = Regex(
        """```(\w*)[ \t]*\r?\n?([\s\S]*?)```""",
        RegexOption.MULTILINE
    )
    
    // 单反引号代码块（备用）- 改进版，支持语言标识符后有或没有换行
    private val singleBacktickRegex = Regex(
        """`\s*(html|css|js|javascript)\s*[\r\n]?([\s\S]*?)`(?!`)""",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )
    
    // File名注释的正则表达式（支持多种格式）
    // 修复：使用贪婪匹配 [^\n]+ 而不是非贪婪 [^\n]+?，确保匹配完整的文件名
    // File名必须以换行符结束
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

    /**
     * 预处理：将各种非标准代码块格式转换为三反引号格式
     * 
     * 某些 AI 会输出非标准格式的代码块，如：
     * 1. 单反引号格式：`js// File名: script.js...`
     * 2. 反斜杠格式：\ html... \ 或 \html...\
     * 3. 未闭合的代码块
     * 
     * 这个函数将它们转换为标准的三反引号格式
     */
    private fun preprocessSingleBackticks(response: String): String {
        var result = response
        
        android.util.Log.d("CodeBlockParser", "preprocessSingleBackticks: checking for non-standard code block patterns")
        android.util.Log.d("CodeBlockParser", "Input preview: ${response.take(200).replace("\n", "\\n")}")
        
        // 1. 处理反斜杠格式的代码块：\ html... \ 或 \html...\
        // 这种情况可能是 AI 试图输出反引号但被转义了
        val backslashPattern = Regex(
            """\\[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)\\(?=\s|$)""",
            setOf(RegexOption.IGNORE_CASE)
        )
        
        val backslashMatches = backslashPattern.findAll(result).toList()
        if (backslashMatches.isNotEmpty()) {
            android.util.Log.d("CodeBlockParser", "Found ${backslashMatches.size} backslash-style code blocks")
            backslashMatches.reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                if (content.length > 20) {
                    android.util.Log.d("CodeBlockParser", "Converting backslash block: language=$language, contentLength=${content.length}")
                    val replacement = "```$language\n$content\n```"
                    result = result.replaceRange(match.range, replacement)
                }
            }
        }
        
        // 2. 处理多个反斜杠格式：\\ \ html... \\ \ 或类似
        val multiBackslashPattern = Regex(
            """\\+[ \t]*\\*[ \t]*(html|css|js|javascript)([\s\S]*?)\\+[ \t]*\\*""",
            setOf(RegexOption.IGNORE_CASE)
        )
        
        val multiBackslashMatches = multiBackslashPattern.findAll(result).toList()
        if (multiBackslashMatches.isNotEmpty()) {
            android.util.Log.d("CodeBlockParser", "Found ${multiBackslashMatches.size} multi-backslash code blocks")
            multiBackslashMatches.reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                if (content.length > 20) {
                    android.util.Log.d("CodeBlockParser", "Converting multi-backslash block: language=$language, contentLength=${content.length}")
                    val replacement = "```$language\n$content\n```"
                    result = result.replaceRange(match.range, replacement)
                }
            }
        }
        
        // 3. 匹配单反引号代码块：`language...` 或 `  language...`
        val singleBacktickPattern = Regex(
            """(?<!`)`[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)`(?!`)""",
            setOf(RegexOption.IGNORE_CASE)
        )
        
        val matches = singleBacktickPattern.findAll(result).toList()
        
        if (matches.isNotEmpty()) {
            android.util.Log.d("CodeBlockParser", "Found ${matches.size} single-backtick code blocks, converting to triple-backtick format")
            
            // 从后往前替换，避免索引偏移问题
            matches.reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                
                android.util.Log.d("CodeBlockParser", "Converting single-backtick block: language=$language, contentLength=${content.length}, preview=${content.take(50)}")
                
                // 转换为三反引号格式
                val replacement = "```$language\n$content\n```"
                result = result.replaceRange(match.range, replacement)
            }
        } else {
            android.util.Log.d("CodeBlockParser", "No single-backtick code blocks found")
            
            // Check是否有未闭合的单反引号代码块
            // 格式：`language... (没有结束的反引号)
            val unclosedPattern = Regex(
                """(?<!`)`[ \t]*(html|css|js|javascript)[ \t]*([^`]+)$""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
            )
            
            unclosedPattern.findAll(result).toList().reversed().forEach { match ->
                val language = match.groupValues[1].lowercase()
                val content = match.groupValues[2].trim()
                
                if (content.length > 50) { // 只处理有实际内容的代码块
                    android.util.Log.d("CodeBlockParser", "Found unclosed single-backtick block: language=$language, contentLength=${content.length}")
                    
                    // 转换为三反引号格式
                    val replacement = "```$language\n$content\n```"
                    result = result.replaceRange(match.range, replacement)
                }
            }
        }
        
        // 4. 如果没有找到任何代码块标记，但内容包含完整的 HTML 结构，直接提取
        if (!result.contains("```") && result.contains("<!DOCTYPE html", ignoreCase = true)) {
            android.util.Log.d("CodeBlockParser", "No code block markers found, but HTML content detected, attempting direct extraction")
            
            // 尝试提取 <!DOCTYPE html> 到 </html> 之间的内容
            val htmlExtractPattern = Regex(
                """(<!DOCTYPE\s+html[\s\S]*?</html>)""",
                setOf(RegexOption.IGNORE_CASE)
            )
            
            htmlExtractPattern.find(result)?.let { match ->
                val htmlContent = match.groupValues[1]
                android.util.Log.d("CodeBlockParser", "Extracted HTML content directly, length=${htmlContent.length}")
                
                // 将提取的 HTML 包装成代码块
                val beforeHtml = result.substring(0, match.range.first)
                val afterHtml = if (match.range.last + 1 < result.length) result.substring(match.range.last + 1) else ""
                result = "$beforeHtml\n```html\n$htmlContent\n```\n$afterHtml"
            }
        }
        
        return result
    }

    /**
     * 解析AI响应 - 简化版
     * 注意：使用 Tool Calling 方式时，此方法不再需要
     * 保留此方法仅用于backward compatible
     */
    fun parseResponse(response: String): ParsedAiResponse {
        android.util.Log.d("CodeBlockParser", "parseResponse: input length=${response.length}")
        
        // 直接使用 Legacy 解析方法
        return parseResponseLegacy(response)
    }
    
    /**
     * 解析AI响应 - 完整版（保留旧逻辑作为备用）
     */
    fun parseResponseLegacy(response: String): ParsedAiResponse {
        var thinking: String? = null
        val codeBlocks = mutableListOf<CodeBlock>()
        val imageRequests = mutableListOf<ImageGenerationRequest>()
        
        android.util.Log.d("CodeBlockParser", "parseResponseLegacy: input length=${response.length}")
        
        // 预处理：将单反引号代码块转换为三反引号格式
        val processedResponse = preprocessSingleBackticks(response)
        var textContent = processedResponse
        
        // 提取思考内容
        thinkingRegex.find(processedResponse)?.let { match ->
            thinking = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
            textContent = textContent.replace(match.value, "")
        }
        
        // 提取图像生成请求
        imageGenRegex.findAll(processedResponse).forEach { match ->
            try {
                val json = match.groupValues[1].trim()
                val request = Gson().fromJson(json, ImageGenerationRequest::class.java)
                imageRequests.add(request)
            } catch (e: Exception) {
                // 忽略解析错误
            }
            textContent = textContent.replace(match.value, "[图像生成请求]")
        }
        
        // 提取代码块 - 使用主正则
        val matchedRanges = mutableListOf<IntRange>()
        codeBlockRegex.findAll(processedResponse).forEach { match ->
            matchedRanges.add(match.range)
            val rawLanguage = match.groupValues[1].trim().lowercase()
            var rawContent = match.groupValues[2]
            
            // 跳过 thinking 和 image-gen 块
            if (rawLanguage == "thinking" || rawLanguage == "image-gen") {
                return@forEach
            }
            
            // 尝试从内容开头提取文件名注释
            // 注意：文件名注释必须独占一行，后面必须有换行符
            var filename: String? = null
            
            // Check是否有文件名注释（必须以换行符结束）
            val filenamePatterns = listOf(
                // HTML 注释格式: <!-- 文件名: xxx.html -->
                Regex("""^<!--\s*文件名[:：]\s*([^\n>]+?)\s*-->\s*\r?\n""", RegexOption.IGNORE_CASE),
                // CSS/JS 块注释格式: /* 文件名: xxx.css */
                Regex("""^/\*\s*文件名[:：]\s*([^\n*]+?)\s*\*/\s*\r?\n""", RegexOption.IGNORE_CASE),
                // JS 单行注释格式: // File名: xxx.js （必须后面有换行）
                Regex("""^//\s*文件名[:：]\s*(\S+)\s*\r?\n""", RegexOption.IGNORE_CASE),
                // Python/Shell 注释格式: # 文件名: xxx
                Regex("""^#\s*文件名[:：]\s*(\S+)\s*\r?\n""", RegexOption.IGNORE_CASE)
            )
            
            for (pattern in filenamePatterns) {
                pattern.find(rawContent)?.let { filenameMatch ->
                    val extractedFilename = filenameMatch.groupValues[1].trim()
                    // Verify文件名格式（应该包含扩展名）
                    if (extractedFilename.contains(".") && extractedFilename.length > 2) {
                        filename = extractedFilename
                        // 移除文件名注释行
                        rawContent = rawContent.substring(filenameMatch.range.last + 1)
                        android.util.Log.d("CodeBlockParser", "Extracted filename: $filename from pattern: ${pattern.pattern}")
                    }
                    return@let
                }
                if (filename != null) break
            }
            
            val content = rawContent.trim()
            
            if (content.isNotEmpty()) {
                // 根据语言标识符或内容推断语言类型
                val language = inferLanguage(rawLanguage, content)
                
                // Generate默认文件名
                val actualFilename = filename?.takeIf { it.isNotBlank() } ?: getDefaultFilename(language)
                
                android.util.Log.d("CodeBlockParser", "Parsed code block: language=$language, filename=$actualFilename, contentLength=${content.length}")
                android.util.Log.d("CodeBlockParser", "Content start: ${content.take(50).replace("\n", "\\n")}")
                
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
        
        // 如果主正则没有匹配到任何代码块，尝试备用解析
        if (codeBlocks.isEmpty()) {
            parseCodeBlocksFallback(processedResponse, codeBlocks)
        }
        
        // Cleanup文本内容中的代码块，避免重复输出
        textContent = codeBlockRegex.replace(textContent, "").trim()
        // 移除多余的空行
        textContent = textContent.replace(Regex("\n{3,}"), "\n\n")
        
        return ParsedAiResponse(
            textContent = textContent,
            thinking = thinking,
            codeBlocks = codeBlocks,
            imageRequests = imageRequests
        )
    }
    
    /**
     * 备用代码块解析方法
     * 当主正则失败时使用，采用更宽松的匹配策略
     */
    private fun parseCodeBlocksFallback(response: String, codeBlocks: MutableList<CodeBlock>) {
        android.util.Log.d("CodeBlockParser", "parseCodeBlocksFallback: trying fallback parsing")
        
        // 备用正则1：更宽松的三反引号匹配
        val fallbackRegex1 = Regex("""```\s*(\w*)\s*([\s\S]*?)```""")
        
        // 备用正则2：单反引号匹配（某些AI会这样输出）- 改进版
        // Support `  js...` 格式（语言标识符前有空格）
        val fallbackRegex2 = Regex("""`[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)`(?!`)""", RegexOption.IGNORE_CASE)
        
        // 备用正则3：基于语言标识符的分段匹配（处理没有结束标记的情况）
        // 匹配 `html、`css、`js 等开头的代码块，一直到下一个代码块或文本结束
        val fallbackRegex3 = Regex("""`[ \t]*(html|css|js|javascript)[ \t]*([\s\S]*?)(?=`[ \t]*(?:html|css|js|javascript)|$)""", RegexOption.IGNORE_CASE)
        
        var foundAny = false
        
        // 尝试备用正则1
        fallbackRegex1.findAll(response).forEach { match ->
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
        
        // 如果备用正则1没找到，尝试备用正则2（单反引号）
        if (!foundAny) {
            fallbackRegex2.findAll(response).forEach { match ->
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
                    android.util.Log.d("CodeBlockParser", "Fallback2 found: language=$language, contentLength=${rawContent.length}")
                }
            }
        }
        
        // 如果备用正则2没找到，尝试备用正则3（处理没有结束标记的情况）
        if (!foundAny) {
            android.util.Log.d("CodeBlockParser", "Trying fallbackRegex3 for unclosed code blocks")
            fallbackRegex3.findAll(response).forEach { match ->
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
                    android.util.Log.d("CodeBlockParser", "Fallback3 found: language=$language, contentLength=${rawContent.length}")
                }
            }
        }
        
        // 如果还是没找到，尝试基于内容特征的智能分割
        if (!foundAny) {
            android.util.Log.d("CodeBlockParser", "Trying content-based parsing")
            parseCodeBlocksByContent(response, codeBlocks)
        }
    }
    
    /**
     * 基于内容特征的智能代码块分割
     * 当所有正则都失败时，尝试根据代码特征识别代码块
     */
    private fun parseCodeBlocksByContent(response: String, codeBlocks: MutableList<CodeBlock>) {
        android.util.Log.d("CodeBlockParser", "parseCodeBlocksByContent: attempting content-based extraction")
        
        // Find HTML 内容
        // 1. 首先尝试完整的 HTML（以 <!DOCTYPE 或 <html 开头，以 </html> 结尾）
        var htmlContent: String? = null
        
        val completeHtmlPattern = Regex("""(<!DOCTYPE[\s\S]*?</html>|<html[\s\S]*?</html>)""", RegexOption.IGNORE_CASE)
        completeHtmlPattern.find(response)?.let { match ->
            htmlContent = match.groupValues[1].trim()
            android.util.Log.d("CodeBlockParser", "Found complete HTML, length=${htmlContent?.length}")
        }
        
        // 2. 如果没有找到完整的 HTML，尝试提取不完整的 HTML（可能缺少结束标签）
        if (htmlContent == null) {
            // Find以 <!DOCTYPE html> 开头的内容
            val doctypeIndex = response.indexOf("<!DOCTYPE", ignoreCase = true)
            if (doctypeIndex >= 0) {
                // 从 <!DOCTYPE 开始，尝试找到 </html> 或 </body> 或 </script>
                val endHtmlIndex = response.indexOf("</html>", doctypeIndex, ignoreCase = true)
                val endBodyIndex = response.indexOf("</body>", doctypeIndex, ignoreCase = true)
                val lastScriptEndIndex = response.lastIndexOf("</script>", ignoreCase = true)
                
                val endIndex = when {
                    endHtmlIndex >= 0 -> endHtmlIndex + 7 // </html> 长度
                    endBodyIndex >= 0 -> endBodyIndex + 7 // </body> 长度，需要补充 </html>
                    lastScriptEndIndex >= doctypeIndex -> lastScriptEndIndex + 9 // </script> 长度
                    else -> response.length
                }
                
                htmlContent = response.substring(doctypeIndex, endIndex).trim()
                
                // 如果缺少 </body> 或 </html>，补充它们
                if (htmlContent != null) {
                    if (!htmlContent!!.contains("</body>", ignoreCase = true)) {
                        htmlContent = htmlContent + "\n</body>"
                    }
                    if (!htmlContent!!.contains("</html>", ignoreCase = true)) {
                        htmlContent = htmlContent + "\n</html>"
                    }
                }
                
                android.util.Log.d("CodeBlockParser", "Extracted partial HTML from DOCTYPE, length=${htmlContent?.length}")
            }
        }
        
        // 3. 如果还是没有，尝试查找 <html> 开头的内容
        if (htmlContent == null) {
            val htmlTagIndex = response.indexOf("<html", ignoreCase = true)
            if (htmlTagIndex >= 0) {
                val endIndex = response.indexOf("</html>", htmlTagIndex, ignoreCase = true)
                htmlContent = if (endIndex >= 0) {
                    response.substring(htmlTagIndex, endIndex + 7)
                } else {
                    response.substring(htmlTagIndex) + "\n</html>"
                }
                android.util.Log.d("CodeBlockParser", "Extracted HTML from <html> tag, length=${htmlContent?.length}")
            }
        }
        
        if (htmlContent != null && htmlContent!!.length > 50) {
            codeBlocks.add(
                CodeBlock(
                    language = "html",
                    filename = "index.html",
                    content = htmlContent!!,
                    isComplete = htmlContent!!.contains("</html>", ignoreCase = true)
                )
            )
            android.util.Log.d("CodeBlockParser", "Added HTML code block, length=${htmlContent!!.length}")
        }
        
        // Find CSS 内容（包含选择器和属性）
        val cssPattern = Regex("""(/\*[\s\S]*?\*/\s*)?([\w\-\.\#\[\]:\s,]+\s*\{[\s\S]*?\}[\s\S]*?)(?=\n\n|\z|/\*|//\s*文件名)""")
        val cssMatches = cssPattern.findAll(response)
        val cssContent = StringBuilder()
        cssMatches.forEach { match ->
            val content = match.value.trim()
            // 确保是 CSS 而不是 JS 对象
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
        
        // Find JS 内容（包含函数定义或变量声明）
        val jsPattern = Regex("""(//[^\n]*\n)?((?:function\s+\w+|const\s+\w+|let\s+\w+|var\s+\w+|document\.|window\.)[\s\S]*?)(?=\n\n\n|\z)""")
        val jsMatches = jsPattern.findAll(response)
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
     * 推断语言类型
     */
    private fun inferLanguage(rawLanguage: String, content: String): String {
        return when {
            rawLanguage.isNotEmpty() -> when (rawLanguage) {
                "javascript" -> "js"
                "htm" -> "html"
                "stylesheet" -> "css"
                else -> rawLanguage
            }
            // 如果没有语言标识，尝试从内容推断
            content.contains("<!DOCTYPE", ignoreCase = true) || 
            content.contains("<html", ignoreCase = true) ||
            (content.trimStart().startsWith("<") && content.contains(">")) -> "html"
            content.trimStart().startsWith("{") && content.contains(":") -> {
                // 可能是 CSS 或 JSON
                if (content.contains("\"") && !content.contains(";")) "json" else "css"
            }
            content.contains("function") || content.contains("const ") || 
            content.contains("let ") || content.contains("var ") ||
            content.contains("=>") -> "js"
            content.contains("{") && content.contains(";") -> "css"
            else -> "html"  // Default为 html
        }
    }
    
    /**
     * 获取默认文件名
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
     * 检查代码是否完整
     */
    private fun isCompleteCode(language: String, content: String): Boolean {
        return when (language) {
            "html" -> content.contains("<!DOCTYPE", ignoreCase = true) || 
                     content.contains("<html", ignoreCase = true)
            "css" -> true  // CSS片段通常都是完整的
            "js" -> true
            else -> true
        }
    }
    
    /**
     * 清理代码块内容，移除开头的文件名注释
     * 
     * 处理以下格式：
     * - // File名: script.js (JS 单行注释，可能没有换行)
     * - /* 文件名: styles.css */ (CSS 块注释)
     * - <!-- 文件名: index.html --> (HTML 注释)
     */
    private fun cleanCodeBlockContent(content: String, language: String): String {
        var cleaned = content.trim()
        
        // 根据语言类型，移除开头的文件名注释
        // 注意：文件名必须是有效格式（字母数字下划线横线 + 扩展名），避免误匹配代码
        val patterns = when (language) {
            "js", "javascript" -> listOf(
                // JS 单行注释格式 - 文件名必须以 .js 结尾，后面可以有换行或空格
                // 使用 [\w\-.]+ 匹配文件名，确保以 .js 结尾
                Regex("""^//\s*文件名[:：]\s*[\w\-.]+\.js\s*""", RegexOption.IGNORE_CASE),
                // JS 块注释格式
                Regex("""^/\*\s*文件名[:：]\s*[\w\-.]+\.js\s*\*/\s*""", RegexOption.IGNORE_CASE)
            )
            "css" -> listOf(
                // CSS 块注释格式 - 文件名必须以 .css 结尾
                Regex("""^/\*\s*文件名[:：]\s*[\w\-.]+\.css\s*\*/\s*""", RegexOption.IGNORE_CASE)
            )
            "html" -> listOf(
                // HTML 注释格式 - 文件名必须以 .html 或 .htm 结尾
                Regex("""^<!--\s*文件名[:：]\s*[\w\-.]+\.html?\s*-->\s*""", RegexOption.IGNORE_CASE)
            )
            else -> emptyList()
        }
        
        for (pattern in patterns) {
            val match = pattern.find(cleaned)
            if (match != null) {
                val matchedText = match.value
                val before = cleaned
                cleaned = cleaned.substring(match.range.last + 1).trim()
                android.util.Log.d("CodeBlockParser", "cleanCodeBlockContent [$language]: Removed filename comment '${matchedText.trim()}'")
                android.util.Log.d("CodeBlockParser", "cleanCodeBlockContent [$language]: Code now starts with: '${cleaned.take(80).replace("\n", "\\n")}'")
            }
        }
        
        // 对于 JS 代码，修复缺失的换行符
        if (language == "js" || language == "javascript") {
            cleaned = fixJsNewlines(cleaned)
        }
        
        return cleaned
    }
    
    /**
     * 修复 JS 代码中缺失的换行符
     * 
     * AI 有时会输出没有换行的 JS 代码，导致单行注释 // 把后面的代码也注释掉
     * 例如：//技能项点击效果 const skillItems = ...
     * 
     * 这个函数会在以下位置添加换行符：
     * 1. 单行注释后面（如果后面紧跟代码）
     * 2. 语句结束符后面（; } 等）
     */
    private fun fixJsNewlines(code: String): String {
        var result = code
        
        // Check代码是否缺少换行符（如果整个代码只有很少的换行符，说明需要修复）
        val lineCount = result.count { it == '\n' }
        val codeLength = result.length
        
        // 如果代码长度超过 200 但换行符少于 5 个，说明可能需要修复
        if (codeLength > 200 && lineCount < 5) {
            android.util.Log.d("CodeBlockParser", "fixJsNewlines: Code appears to be minified (length=$codeLength, lines=$lineCount), attempting to fix")
            
            // 1. 在单行注释后添加换行（// 注释内容 后面如果紧跟代码）
            // 匹配 // 开头的注释，后面紧跟非换行的代码
            // 注意：要避免匹配 URL 中的 // (如 http://)
            result = result.replace(Regex("""(?<!:)(//[^\n]*?)(\s*)(const|let|var|function|if|else|for|while|switch|return|document|window|console|\$|\[|{)""")) { match ->
                val comment = match.groupValues[1]
                val code = match.groupValues[3]
                "$comment\n$code"
            }
            
            // 2. 在 }); 后添加换行（常见的回调结束）
            result = result.replace(Regex("""\}\);(\s*)(?!\s*\n)(?=\S)""")) { match ->
                "});\n"
            }
            
            // 3. 在 }; 后添加换行
            result = result.replace(Regex("""\};(\s*)(?!\s*\n)(?=\S)""")) { match ->
                "};\n"
            }
            
            // 4. 在单独的 } 后添加换行（函数/块结束）
            result = result.replace(Regex("""\}(\s*)(?!\s*[\n;,)\]])(?=\s*(?:const|let|var|function|if|else|for|while|switch|return|document|window|console|//|\$))""")) { match ->
                "}\n"
            }
            
            // 5. 在 ; 后添加换行（语句结束，但不是 for 循环中的分号）
            // 这个比较复杂，只处理明显的语句结束
            result = result.replace(Regex(""";(\s*)(?!\s*\n)(?=\s*(?:const|let|var|function|if|else|for|while|switch|return|document|window|console|//|\$|[a-zA-Z_]))""")) { match ->
                ";\n"
            }
            
            val newLineCount = result.count { it == '\n' }
            android.util.Log.d("CodeBlockParser", "fixJsNewlines: After fix, lines=$newLineCount")
            android.util.Log.d("CodeBlockParser", "fixJsNewlines: First 300 chars: ${result.take(300).replace("\n", "\\n")}")
        }
        
        return result
    }
    
    /**
     * 修复 HTML 中内联 JS 的常见语法错误
     * 
     * AI 有时会输出有语法错误的 JS 代码，比如：
     * - alert(游戏结束) 应该是 alert('游戏结束')
     * - 缺少字符串引号
     */
    private fun fixInlineJsSyntax(html: String): String {
        var result = html
        
        // 找到所有 <script> 标签并修复其中的 JS
        val scriptRegex = Regex("""(<script[^>]*>)([\s\S]*?)(</script>)""", RegexOption.IGNORE_CASE)
        result = scriptRegex.replace(result) { match ->
            val openTag = match.groupValues[1]
            var jsContent = match.groupValues[2]
            val closeTag = match.groupValues[3]
            
            // 只处理内联脚本（没有 src 属性的）
            if (!openTag.contains("src=", ignoreCase = true) && jsContent.isNotBlank()) {
                jsContent = fixJsSyntaxErrors(jsContent)
            }
            
            "$openTag$jsContent$closeTag"
        }
        
        return result
    }
    
    /**
     * 修复 JS 代码中的常见语法错误
     */
    private fun fixJsSyntaxErrors(js: String): String {
        var result = js
        
        // 1. 修复 alert/confirm/prompt 中缺少引号的中文字符串
        // 例如：alert(游戏结束) -> alert('游戏结束')
        result = result.replace(Regex("""(alert|confirm|prompt)\s*\(\s*([^'"`\(\)]+[\u4e00-\u9fa5][^'"`\(\)]*)\s*\)""")) { match ->
            val func = match.groupValues[1]
            val content = match.groupValues[2].trim()
            "$func('$content')"
        }
        
        // 2. 修复 console.log 中缺少引号的中文字符串
        result = result.replace(Regex("""(console\.log)\s*\(\s*([^'"`\(\)]+[\u4e00-\u9fa5][^'"`\(\)]*)\s*\)""")) { match ->
            val func = match.groupValues[1]
            val content = match.groupValues[2].trim()
            "$func('$content')"
        }
        
        // 3. 修复模板字符串中的问题（如果有的话）
        // 这个比较复杂，暂时跳过
        
        android.util.Log.d("CodeBlockParser", "fixJsSyntaxErrors: Applied syntax fixes")
        
        return result
    }
    
    /**
     * 合并多个代码块为完整HTML文件
     * 
     * 合并代码块为单个 HTML - 简化版
     */
    fun mergeToSingleHtml(codeBlocks: List<CodeBlock>): String {
        android.util.Log.d("CodeBlockParser", "mergeToSingleHtml: ${codeBlocks.size} blocks")
        
        val html = codeBlocks.find { it.language == "html" }?.content
        val css = codeBlocks.filter { it.language == "css" }.joinToString("\n\n") { it.content }
        val js = codeBlocks.filter { it.language == "js" || it.language == "javascript" }.joinToString("\n\n") { it.content }
        
        // 如果有完整的 HTML，直接使用并插入 CSS/JS
        if (html != null && (html.contains("<html", ignoreCase = true) || html.contains("<!DOCTYPE", ignoreCase = true))) {
            var result = html
            
            // 插入 CSS
            if (css.isNotBlank() && !html.contains(css.take(50))) {
                val styleTag = "<style>\n$css\n</style>"
                result = insertBefore(result, styleTag, "</head>", "<body")
            }
            
            // 插入 JS
            if (js.isNotBlank() && !html.contains(js.take(50))) {
                val scriptTag = "<script>\n$js\n</script>"
                result = insertBefore(result, scriptTag, "</body>", "</html>")
            }
            
            return result
        }
        
        // 没有完整 HTML，构建一个
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
     * 合并代码块为单个 HTML - 旧版（保留作为备用）
     */
    fun mergeToSingleHtmlLegacy(codeBlocks: List<CodeBlock>): String {
        val htmlBlocks = codeBlocks.filter { it.language == "html" }
        val cssBlocks = codeBlocks.filter { it.language == "css" }
        val jsBlocks = codeBlocks.filter { it.language == "js" || it.language == "javascript" }
        
        android.util.Log.d("CodeBlockParser", "mergeToSingleHtmlLegacy: htmlBlocks=${htmlBlocks.size}, cssBlocks=${cssBlocks.size}, jsBlocks=${jsBlocks.size}")
        codeBlocks.forEach { block ->
            android.util.Log.d("CodeBlockParser", "  Block: language=${block.language}, filename=${block.filename}, contentLength=${block.content.length}")
            // 输出代码块内容的前200个字符用于调试
            android.util.Log.d("CodeBlockParser", "  Content preview: ${block.content.take(200).replace("\n", "\\n")}")
        }
        
        // 如果有完整的HTML，尝试合并CSS和JS
        val mainHtml = htmlBlocks.find { it.isComplete }?.content
        
        if (mainHtml != null) {
            var result = mainHtml
            
            // 输出原始 HTML 中的 script 标签内容用于调试
            val scriptTagRegex = Regex("""<script[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val scriptMatches = scriptTagRegex.findAll(mainHtml).toList()
            android.util.Log.d("CodeBlockParser", "Original HTML has ${scriptMatches.size} script tags")
            scriptMatches.forEachIndexed { index, match ->
                val scriptContent = match.groupValues[1]
                android.util.Log.d("CodeBlockParser", "  Script $index: length=${scriptContent.length}, preview=${scriptContent.take(100).replace("\n", "\\n")}")
            }
            
            // Check HTML 中是否已经有内联的 style 和 script 标签
            val hasInlineStyle = result.contains("<style", ignoreCase = true)
            // Check是否有真正的内联脚本（有内容的 script 标签，而不是空的或只有 src 的）
            // 匹配 <script>...</script> 但排除 <script src="..."></script>
            val inlineScriptRegex = Regex("""<script(?![^>]*\bsrc\s*=)[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val inlineScriptMatches = inlineScriptRegex.findAll(result).toList()
            val hasInlineScript = inlineScriptMatches.any { match ->
                val scriptContent = match.groupValues[1].trim()
                scriptContent.isNotEmpty() && scriptContent.length > 5  // 至少有一些实际代码
            }
            
            android.util.Log.d("CodeBlockParser", "hasInlineStyle=$hasInlineStyle, hasInlineScript=$hasInlineScript")
            if (hasInlineScript) {
                inlineScriptMatches.forEach { match ->
                    android.util.Log.d("CodeBlockParser", "Found inline script, length=${match.groupValues[1].length}")
                }
            }
            
            // 只有当有对应的代码块时，才移除外部引用
            // No则保留外部引用（虽然可能无法加载，但至少不会丢失信息）
            if (cssBlocks.isNotEmpty()) {
                result = result.replace(Regex("""<link[^>]*href=["'](?!http)[^"']*\.css["'][^>]*>""", RegexOption.IGNORE_CASE), "")
            }
            if (jsBlocks.isNotEmpty()) {
                result = result.replace(Regex("""<script[^>]*src=["'](?!http)[^"']*\.js["'][^>]*></script>""", RegexOption.IGNORE_CASE), "")
            }
            
            // 在</head>前插入CSS（如果有独立的CSS块且HTML中没有相同内容）
            // 注意：如果 HTML 已经有内联样式，仍然添加额外的 CSS 块（它们可能是补充样式）
            if (cssBlocks.isNotEmpty()) {
                // Cleanup每个 CSS 代码块开头的文件名注释
                val cleanedCssBlocks = cssBlocks.map { block ->
                    cleanCodeBlockContent(block.content, block.language)
                }
                val cssContent = cleanedCssBlocks.joinToString("\n\n")
                val styleTag = "<style>\n$cssContent\n</style>"
                // 使用 Regex.escapeReplacement 转义替换字符串，避免 ${...} 被解析为命名组
                val escapedStyleTag = Regex.escapeReplacement(styleTag)
                
                result = when {
                    result.contains("</head>", ignoreCase = true) -> {
                        result.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$escapedStyleTag\n</head>")
                    }
                    result.contains("<body", ignoreCase = true) -> {
                        result.replaceFirst(Regex("<body", RegexOption.IGNORE_CASE), "$escapedStyleTag\n<body")
                    }
                    result.contains("<html", ignoreCase = true) -> {
                        // 在 <html> 后插入 <head> 和样式
                        result.replaceFirst(Regex("(<html[^>]*>)", RegexOption.IGNORE_CASE), "\$1\n<head>\n$escapedStyleTag\n</head>")
                    }
                    else -> {
                        // 没有标准结构，在开头添加
                        "$styleTag\n$result"
                    }
                }
            }
            
            // 在</body>前插入JS（确保 DOM 加载完成后执行）
            // 只有当有独立的 JS 代码块时才添加
            if (jsBlocks.isNotEmpty()) {
                // Cleanup每个 JS 代码块开头的文件名注释
                val cleanedJsBlocks = jsBlocks.map { block ->
                    cleanCodeBlockContent(block.content, block.language)
                }
                val jsContent = cleanedJsBlocks.joinToString("\n\n")
                android.util.Log.d("CodeBlockParser", "Adding JS content, length=${jsContent.length}")
                android.util.Log.d("CodeBlockParser", "JS content preview: ${jsContent.take(200)}...")
                // 如果 HTML 中已经有内联脚本，不要包装 JS（避免重复包装）
                // 直接添加额外的 JS 代码
                val finalJs = if (hasInlineScript) {
                    // HTML 已有内联脚本，直接添加额外代码（不包装）
                    jsContent
                } else {
                    // 没有内联脚本，包装 JS 确保 DOM 加载完成后执行
                    wrapJsForDomReady(jsContent)
                }
                val scriptTag = "<script>\n$finalJs\n</script>"
                // 使用 Regex.escapeReplacement 转义替换字符串，避免 ${...} 被解析为命名组
                val escapedScriptTag = Regex.escapeReplacement(scriptTag)
                
                val beforeLength = result.length
                result = when {
                    result.contains("</body>", ignoreCase = true) -> {
                        result.replaceFirst(Regex("</body>", RegexOption.IGNORE_CASE), "$escapedScriptTag\n</body>")
                    }
                    result.contains("</html>", ignoreCase = true) -> {
                        result.replaceFirst(Regex("</html>", RegexOption.IGNORE_CASE), "$escapedScriptTag\n</html>")
                    }
                    else -> {
                        // 在末尾添加
                        "$result\n$scriptTag"
                    }
                }
                android.util.Log.d("CodeBlockParser", "JS inserted, beforeLength=$beforeLength, afterLength=${result.length}")
            } else {
                android.util.Log.d("CodeBlockParser", "No JS blocks to add")
                // Check HTML 中是否有外部 JS 引用但没有对应的代码块
                val externalJsRegex = Regex("""<script[^>]*src=["'](?!http)([^"']*\.js)["'][^>]*></script>""", RegexOption.IGNORE_CASE)
                val externalJsMatches = externalJsRegex.findAll(result)
                externalJsMatches.forEach { match ->
                    android.util.Log.w("CodeBlockParser", "WARNING: HTML references external JS file '${match.groupValues[1]}' but no JS code block found!")
                }
            }
            
            // 输出最终结果中的 script 标签用于调试
            val finalScriptRegex = Regex("""<script[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val finalScriptMatches = finalScriptRegex.findAll(result).toList()
            android.util.Log.d("CodeBlockParser", "Final HTML has ${finalScriptMatches.size} script tags")
            finalScriptMatches.forEachIndexed { index, match ->
                val scriptContent = match.groupValues[1]
                android.util.Log.d("CodeBlockParser", "  Final Script $index: length=${scriptContent.length}")
            }
            
            // 修复内联 JS 中的语法错误
            result = fixInlineJsSyntax(result)
            
            return result
        }
        
        // 没有完整HTML，构建一个标准的 HTML5 文档
        val css = cssBlocks.joinToString("\n\n") { it.content }
        val js = jsBlocks.joinToString("\n\n") { it.content }
        val htmlContent = htmlBlocks.joinToString("\n") { it.content }
        
        // 包装 JS 确保 DOM 加载完成后执行
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
        
        // 修复内联 JS 中的语法错误
        return fixInlineJsSyntax(builtHtml)
    }
    
    /**
     * 包装 JS 代码，确保在 DOM 加载完成后执行
     * 避免 JS 执行时 DOM 元素还未创建的问题
     * 
     * 修复：使用更安全的包装方式，保持全局作用域的函数可访问
     */
    private fun wrapJsForDomReady(jsContent: String): String {
        val trimmedContent = jsContent.trim()
        if (trimmedContent.isEmpty()) return ""
        
        // Check JS 是否已经有 DOMContentLoaded 或 window.onload 包装
        val hasWrapper = trimmedContent.contains("DOMContentLoaded", ignoreCase = true) ||
                        trimmedContent.contains("window.onload", ignoreCase = true) ||
                        trimmedContent.contains("addEventListener('load'", ignoreCase = true) ||
                        trimmedContent.contains("addEventListener(\"load\"", ignoreCase = true) ||
                        trimmedContent.contains("\$(document).ready", ignoreCase = true) ||
                        trimmedContent.contains("\$(function()", ignoreCase = true)
        
        return if (hasWrapper) {
            trimmedContent
        } else {
            // 不再包装 JS 代码，直接返回原始内容
            // 因为脚本标签放在 </body> 前，此时 DOM 已经加载完成
            // 包装会导致函数定义不在全局作用域，无法被 onclick 等属性调用
            trimmedContent
        }
    }
}
