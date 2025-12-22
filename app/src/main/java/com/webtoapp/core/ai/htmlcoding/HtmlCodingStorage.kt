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
        
        // 文件存储目录
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

    /**
     * 创建新会话
     */
    suspend fun createSession(title: String = "新对话"): HtmlCodingSession {
        val session = HtmlCodingSession(title = title)
        context.htmlCodingDataStore.edit { prefs ->
            val sessions = getSessions(prefs).toMutableList()
            sessions.add(0, session)  // 新会话放在最前面
            prefs[KEY_SESSIONS] = gson.toJson(sessions)
            prefs[KEY_CURRENT_SESSION_ID] = session.id
        }
        return session
    }

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
        
        // 删除会话相关文件
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
                    
                    // 创建检查点（保存编辑前的状态）
                    val checkpoint = createCheckpointFromSession(session, messageIndex, "编辑前自动保存")
                    
                    // 更新消息内容，保留原始内容
                    val editedMessage = oldMessage.copy(
                        content = newContent,
                        images = newImages,
                        isEdited = true,
                        originalContent = oldMessage.originalContent ?: oldMessage.content
                    )
                    
                    // 删除该消息之后的所有消息（回滚）
                    val newMessages = session.messages.subList(0, messageIndex) + editedMessage
                    
                    // 更新检查点列表
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
                val filename = block.filename ?: "index.${block.language}"
                val type = when (block.language.lowercase()) {
                    "html" -> ProjectFileType.HTML
                    "css" -> ProjectFileType.CSS
                    "javascript", "js" -> ProjectFileType.JS
                    "svg" -> ProjectFileType.SVG
                    "json" -> ProjectFileType.JSON
                    else -> ProjectFileType.OTHER
                }
                // 更新或添加文件
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
     * 获取项目目录
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
                
                // 创建父目录
                targetFile.parentFile?.mkdirs()
                
                // 检查是否覆盖
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
        
        // 应用私有目录
        dirs.add("应用目录" to getProjectsDir())
        
        // 下载目录
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
            val filename = block.filename ?: "index.${block.language}"
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
        
        // 保存预览文件
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
        // 删除相关文件
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
        
        // 保存预览HTML
        File(itemDir, "preview.html").writeText(item.previewHtml)
        
        // 保存各个文件
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
        
        // 获取当前会话关联的代码库项目ID
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
    
    // 单反引号代码块（备用）
    private val singleBacktickRegex = Regex(
        """`\s*(\w+)\s*\r?\n([\s\S]*?)`(?!`)""",
        RegexOption.MULTILINE
    )
    
    // 文件名注释的正则表达式（支持多种格式）
    private val filenameCommentRegex = Regex(
        """^(?:<!--\s*文件名[:：]\s*([^\n]+?)\s*-->\s*\r?\n?|/\*\s*文件名[:：]\s*([^\n]+?)\s*\*/\s*\r?\n?|//\s*文件名[:：]\s*([^\n]+?)\s*\r?\n?|#\s*文件名[:：]\s*([^\n]+?)\s*\r?\n?)""",
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
     * 解析AI响应
     */
    fun parseResponse(response: String): ParsedAiResponse {
        var textContent = response
        var thinking: String? = null
        val codeBlocks = mutableListOf<CodeBlock>()
        val imageRequests = mutableListOf<ImageGenerationRequest>()
        
        // 提取思考内容
        thinkingRegex.find(response)?.let { match ->
            thinking = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
            textContent = textContent.replace(match.value, "")
        }
        
        // 提取图像生成请求
        imageGenRegex.findAll(response).forEach { match ->
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
        codeBlockRegex.findAll(response).forEach { match ->
            matchedRanges.add(match.range)
            val rawLanguage = match.groupValues[1].trim().lowercase()
            var rawContent = match.groupValues[2]
            
            // 跳过 thinking 和 image-gen 块
            if (rawLanguage == "thinking" || rawLanguage == "image-gen") {
                return@forEach
            }
            
            // 尝试从内容开头提取文件名注释
            var filename: String? = null
            filenameCommentRegex.find(rawContent)?.let { filenameMatch ->
                filename = filenameMatch.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.trim()
                // 移除文件名注释行
                rawContent = rawContent.substring(filenameMatch.range.last + 1)
            }
            
            val content = rawContent.trim()
            
            if (content.isNotEmpty()) {
                // 根据语言标识符或内容推断语言类型
                val language = inferLanguage(rawLanguage, content)
                
                // 生成默认文件名
                val actualFilename = filename?.takeIf { it.isNotBlank() } ?: getDefaultFilename(language)
                
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
            parseCodeBlocksFallback(response, codeBlocks)
        }
        
        // 清理文本内容中的代码块，避免重复输出
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
        // 备用正则1：更宽松的三反引号匹配
        val fallbackRegex1 = Regex("""```\s*(\w*)\s*([\s\S]*?)```""")
        
        // 备用正则2：单反引号匹配（某些AI会这样输出）
        val fallbackRegex2 = Regex("""`\s*(\w+)\s*([\s\S]*?)`(?!`)""")
        
        // 备用正则3：基于语言标识符的分段匹配（处理没有结束标记的情况）
        // 匹配 `html、`css、`js 等开头的代码块
        val fallbackRegex3 = Regex("""`(html|css|js|javascript)\s*([\s\S]*?)(?=`(?:html|css|js|javascript)|$)""", RegexOption.IGNORE_CASE)
        
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
                }
            }
        }
        
        // 如果还是没找到，尝试基于内容特征的智能分割
        if (!foundAny) {
            parseCodeBlocksByContent(response, codeBlocks)
        }
    }
    
    /**
     * 基于内容特征的智能代码块分割
     * 当所有正则都失败时，尝试根据代码特征识别代码块
     */
    private fun parseCodeBlocksByContent(response: String, codeBlocks: MutableList<CodeBlock>) {
        // 查找 HTML 内容（以 <!DOCTYPE 或 <html 开头）
        val htmlPattern = Regex("""(<!DOCTYPE[\s\S]*?</html>|<html[\s\S]*?</html>)""", RegexOption.IGNORE_CASE)
        htmlPattern.find(response)?.let { match ->
            val content = match.groupValues[1].trim()
            if (content.length > 50) {
                codeBlocks.add(
                    CodeBlock(
                        language = "html",
                        filename = "index.html",
                        content = content,
                        isComplete = true
                    )
                )
            }
        }
        
        // 查找 CSS 内容（包含选择器和属性）
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
        
        // 查找 JS 内容（包含函数定义或变量声明）
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
            else -> "html"  // 默认为 html
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
     * 合并多个代码块为完整HTML文件
     * 
     * 修复问题：
     * 1. 确保 CSS 和 JS 正确插入到 HTML 中
     * 2. 处理 HTML 结构不规范的情况
     * 3. 避免重复插入已存在的内联样式/脚本
     */
    fun mergeToSingleHtml(codeBlocks: List<CodeBlock>): String {
        val htmlBlocks = codeBlocks.filter { it.language == "html" }
        val cssBlocks = codeBlocks.filter { it.language == "css" }
        val jsBlocks = codeBlocks.filter { it.language == "js" || it.language == "javascript" }
        
        // 如果有完整的HTML，尝试合并CSS和JS
        val mainHtml = htmlBlocks.find { it.isComplete }?.content
        
        if (mainHtml != null) {
            var result = mainHtml
            
            // 检查 HTML 中是否已经有内联的 style 和 script 标签
            val hasInlineStyle = result.contains("<style", ignoreCase = true)
            val hasInlineScript = result.contains("<script", ignoreCase = true) && 
                                  !result.contains("<script[^>]*src=".toRegex(RegexOption.IGNORE_CASE))
            
            // 移除 HTML 中已有的外部 CSS/JS 引用（因为我们会内联它们）
            // 这样可以避免文件找不到的问题
            result = result.replace(Regex("""<link[^>]*href=["'](?!http)[^"']*\.css["'][^>]*>""", RegexOption.IGNORE_CASE), "")
            result = result.replace(Regex("""<script[^>]*src=["'](?!http)[^"']*\.js["'][^>]*></script>""", RegexOption.IGNORE_CASE), "")
            
            // 在</head>前插入CSS（如果有独立的CSS块且HTML中没有相同内容）
            // 注意：如果 HTML 已经有内联样式，仍然添加额外的 CSS 块（它们可能是补充样式）
            if (cssBlocks.isNotEmpty()) {
                val cssContent = cssBlocks.joinToString("\n\n") { it.content }
                val styleTag = "<style>\n$cssContent\n</style>"
                
                result = when {
                    result.contains("</head>", ignoreCase = true) -> {
                        result.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$styleTag\n</head>")
                    }
                    result.contains("<body", ignoreCase = true) -> {
                        result.replaceFirst(Regex("<body", RegexOption.IGNORE_CASE), "$styleTag\n<body")
                    }
                    result.contains("<html", ignoreCase = true) -> {
                        // 在 <html> 后插入 <head> 和样式
                        result.replaceFirst(Regex("(<html[^>]*>)", RegexOption.IGNORE_CASE), "$1\n<head>\n$styleTag\n</head>")
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
                val jsContent = jsBlocks.joinToString("\n\n") { it.content }
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
                
                result = when {
                    result.contains("</body>", ignoreCase = true) -> {
                        result.replaceFirst(Regex("</body>", RegexOption.IGNORE_CASE), "$scriptTag\n</body>")
                    }
                    result.contains("</html>", ignoreCase = true) -> {
                        result.replaceFirst(Regex("</html>", RegexOption.IGNORE_CASE), "$scriptTag\n</html>")
                    }
                    else -> {
                        // 在末尾添加
                        "$result\n$scriptTag"
                    }
                }
            }
            
            return result
        }
        
        // 没有完整HTML，构建一个标准的 HTML5 文档
        val css = cssBlocks.joinToString("\n\n") { it.content }
        val js = jsBlocks.joinToString("\n\n") { it.content }
        val htmlContent = htmlBlocks.joinToString("\n") { it.content }
        
        // 包装 JS 确保 DOM 加载完成后执行
        val wrappedJs = if (js.isNotEmpty()) wrapJsForDomReady(js) else ""
        
        return """
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
        
        // 检查 JS 是否已经有 DOMContentLoaded 或 window.onload 包装
        val hasWrapper = trimmedContent.contains("DOMContentLoaded", ignoreCase = true) ||
                        trimmedContent.contains("window.onload", ignoreCase = true) ||
                        trimmedContent.contains("addEventListener('load'", ignoreCase = true) ||
                        trimmedContent.contains("addEventListener(\"load\"", ignoreCase = true) ||
                        trimmedContent.contains("\$(document).ready", ignoreCase = true) ||
                        trimmedContent.contains("\$(function()", ignoreCase = true)
        
        return if (hasWrapper) {
            trimmedContent
        } else {
            // 直接使用 DOMContentLoaded 包装，不使用 IIFE
            // 这样可以保持全局作用域的函数定义可访问
            """
// 确保 DOM 加载完成后执行
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
$trimmedContent
    });
} else {
    // DOM 已经加载完成，直接执行
    (function() {
$trimmedContent
    })();
}
            """.trimIndent()
        }
    }
}
