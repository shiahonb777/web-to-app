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
        
        // 文件存储目录
        private const val HTML_CODING_DIR = "HtmlCoding"
        private const val SESSIONS_DIR = "sessions"
        private const val PROJECTS_DIR = "projects"
        private const val IMAGES_DIR = "images"
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

    // ==================== 辅助方法 ====================

    private fun getSessions(prefs: Preferences): List<HtmlCodingSession> {
        val json = prefs[KEY_SESSIONS] ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<HtmlCodingSession>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * 从AI响应中解析代码块
 */
object CodeBlockParser {
    
    private val codeBlockRegex = Regex(
        """```(\w+)?(?:\s*\n)?(?:<!--\s*文件名:\s*(.+?)\s*-->|/\*\s*文件名:\s*(.+?)\s*\*/|//\s*文件名:\s*(.+?)\s*\n)?\s*([\s\S]*?)```""",
        RegexOption.MULTILINE
    )
    
    private val thinkingRegex = Regex(
        """<thinking>([\s\S]*?)</thinking>|```thinking\s*([\s\S]*?)```""",
        RegexOption.MULTILINE
    )
    
    private val imageGenRegex = Regex(
        """```image-gen\s*([\s\S]*?)```""",
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
        
        // 提取代码块
        codeBlockRegex.findAll(response).forEach { match ->
            val language = match.groupValues[1].ifEmpty { "html" }
            val filename = match.groupValues[2].ifEmpty { 
                match.groupValues[3].ifEmpty { 
                    match.groupValues[4].ifEmpty { null } 
                } 
            }
            val content = match.groupValues[5].trim()
            
            if (content.isNotEmpty()) {
                codeBlocks.add(
                    CodeBlock(
                        language = language,
                        filename = filename,
                        content = content,
                        isComplete = isCompleteCode(language, content)
                    )
                )
            }
        }
        
        // 清理文本内容中的代码块
        textContent = codeBlockRegex.replace(textContent, "").trim()
        
        return ParsedAiResponse(
            textContent = textContent,
            thinking = thinking,
            codeBlocks = codeBlocks,
            imageRequests = imageRequests
        )
    }
    
    /**
     * 检查代码是否完整
     */
    private fun isCompleteCode(language: String, content: String): Boolean {
        return when (language.lowercase()) {
            "html" -> content.contains("<!DOCTYPE", ignoreCase = true) || 
                     content.contains("<html", ignoreCase = true)
            "css" -> true  // CSS片段通常都是完整的
            "javascript", "js" -> true
            else -> true
        }
    }
    
    /**
     * 合并多个代码块为完整HTML文件
     */
    fun mergeToSingleHtml(codeBlocks: List<CodeBlock>): String {
        val htmlBlocks = codeBlocks.filter { it.language == "html" }
        val cssBlocks = codeBlocks.filter { it.language == "css" }
        val jsBlocks = codeBlocks.filter { it.language == "javascript" || it.language == "js" }
        
        // 如果有完整的HTML，尝试合并CSS和JS
        val mainHtml = htmlBlocks.find { it.isComplete }?.content
        
        if (mainHtml != null) {
            var result = mainHtml
            
            // 在</head>前插入CSS
            if (cssBlocks.isNotEmpty()) {
                val cssContent = cssBlocks.joinToString("\n") { it.content }
                val styleTag = "<style>\n$cssContent\n</style>"
                result = if (result.contains("</head>")) {
                    result.replace("</head>", "$styleTag\n</head>")
                } else {
                    result.replace("<body", "$styleTag\n<body")
                }
            }
            
            // 在</body>前插入JS
            if (jsBlocks.isNotEmpty()) {
                val jsContent = jsBlocks.joinToString("\n") { it.content }
                val scriptTag = "<script>\n$jsContent\n</script>"
                result = if (result.contains("</body>")) {
                    result.replace("</body>", "$scriptTag\n</body>")
                } else {
                    result + scriptTag
                }
            }
            
            return result
        }
        
        // 没有完整HTML，构建一个
        val css = cssBlocks.joinToString("\n") { it.content }
        val js = jsBlocks.joinToString("\n") { it.content }
        val htmlContent = htmlBlocks.joinToString("\n") { it.content }
        
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>预览</title>
    ${if (css.isNotEmpty()) "<style>\n$css\n</style>" else ""}
</head>
<body>
$htmlContent
${if (js.isNotEmpty()) "<script>\n$js\n</script>" else ""}
</body>
</html>
        """.trimIndent()
    }
}
