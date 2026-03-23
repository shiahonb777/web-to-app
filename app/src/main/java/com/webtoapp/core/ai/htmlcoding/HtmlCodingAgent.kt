package com.webtoapp.core.ai.htmlcoding

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.ToolStreamEvent
import com.webtoapp.core.ai.ToolCallInfo
import com.webtoapp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.regex.Pattern

/**
 * HTML 编程 Agent
 * 
 * 使用流式 Tool Calling 方式让 AI 创建/修改 HTML 代码
 * 文件直接写入项目文件夹，支持版本迭代
 */
class HtmlCodingAgent(private val context: Context) {
    
    private val gson = Gson()
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    private val projectFileManager = ProjectFileManager(context)
    
    // 当前会话ID（用于文件操作）
    private var currentSessionId: String? = null
    
    // 控制台日志存储
    private val consoleLogs = mutableListOf<ConsoleLogEntry>()
    
    // 语法错误存储
    private val syntaxErrors = mutableListOf<SyntaxError>()
    
    companion object {
        private const val TAG = "HtmlCodingAgent"
        private const val STREAM_TIMEOUT_MS = 120_000L  // 2分钟超时
    }
    
    // 当前 HTML 代码（用于编辑操作，兼容旧逻辑）
    private var currentHtmlCode: String = ""
    
    /**
     * 设置当前会话ID
     */
    fun setSessionId(sessionId: String) {
        currentSessionId = sessionId
    }
    
    /**
     * 设置当前 HTML 代码
     */
    fun setCurrentHtml(html: String) {
        currentHtmlCode = html
    }
    
    /**
     * 获取当前 HTML 代码
     */
    fun getCurrentHtml(): String = currentHtmlCode
    
    // ==================== 工具定义 ====================
    
    /**
     * 获取所有可用工具
     */
    fun getAllTools(): List<HtmlTool> = listOf(
        HtmlTool(
            type = HtmlToolType.WRITE_HTML,
            name = "write_html",
            description = "创建 HTML 文件并写入代码。将完整的 HTML 代码作为 html 参数传入。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "html" to mapOf(
                        "type" to "string",
                        "description" to "完整的 HTML 代码，包含 <!DOCTYPE html> 声明"
                    ),
                    "filename" to mapOf(
                        "type" to "string",
                        "description" to "文件名，默认为 index.html"
                    )
                ),
                "required" to listOf("html")
            )
        ),
        HtmlTool(
            type = HtmlToolType.EDIT_HTML,
            name = "edit_html",
            description = "编辑现有 HTML 代码的指定部分。支持替换、插入、删除操作。适合小范围修改，避免重写整个文件。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "operation" to mapOf(
                        "type" to "string",
                        "enum" to listOf("replace", "insert_before", "insert_after", "delete"),
                        "description" to "操作类型：replace=替换, insert_before=在目标前插入, insert_after=在目标后插入, delete=删除"
                    ),
                    "target" to mapOf(
                        "type" to "string",
                        "description" to "要定位的目标代码片段（必须精确匹配现有代码）"
                    ),
                    "content" to mapOf(
                        "type" to "string",
                        "description" to "新的代码内容（delete 操作时可省略）"
                    )
                ),
                "required" to listOf("operation", "target")
            )
        ),
        HtmlTool(
            type = HtmlToolType.GET_CONSOLE_LOGS,
            name = "get_console_logs",
            description = "获取页面运行时的控制台日志，包括 console.log、console.error、console.warn 输出和 JavaScript 运行时错误。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "filter" to mapOf(
                        "type" to "string",
                        "enum" to listOf("all", "error", "warn", "log"),
                        "description" to "日志过滤类型：all=全部, error=仅错误, warn=仅警告, log=仅普通日志"
                    )
                ),
                "required" to emptyList<String>()
            )
        ),
        HtmlTool(
            type = HtmlToolType.CHECK_SYNTAX,
            name = "check_syntax",
            description = "检查 HTML/CSS/JavaScript 代码的语法错误，返回错误列表和位置信息。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "code" to mapOf(
                        "type" to "string",
                        "description" to "要检查的代码内容"
                    ),
                    "language" to mapOf(
                        "type" to "string",
                        "enum" to listOf("html", "css", "javascript"),
                        "description" to "代码语言类型"
                    )
                ),
                "required" to listOf("code")
            )
        ),
        HtmlTool(
            type = HtmlToolType.AUTO_FIX,
            name = "auto_fix",
            description = "根据语法检查结果和控制台错误，自动修复代码问题。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "code" to mapOf(
                        "type" to "string",
                        "description" to "需要修复的原始代码"
                    ),
                    "errors" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "description" to "需要修复的错误描述列表"
                    )
                ),
                "required" to listOf("code", "errors")
            )
        ),
        HtmlTool(
            type = HtmlToolType.GENERATE_IMAGE,
            name = "generate_image",
            description = "使用 AI 生成图像。生成的图像会以 base64 格式返回，可直接嵌入到 HTML 的 img 标签中。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "prompt" to mapOf(
                        "type" to "string",
                        "description" to "图像生成提示词，详细描述想要生成的图像内容、风格、颜色等"
                    ),
                    "style" to mapOf(
                        "type" to "string",
                        "enum" to listOf("realistic", "cartoon", "illustration", "icon", "abstract", "minimalist"),
                        "description" to "图像风格：realistic=写实, cartoon=卡通, illustration=插画, icon=图标, abstract=抽象, minimalist=极简"
                    ),
                    "size" to mapOf(
                        "type" to "string",
                        "enum" to listOf("small", "medium", "large"),
                        "description" to "图像尺寸：small=256x256, medium=512x512, large=1024x1024"
                    )
                ),
                "required" to listOf("prompt")
            )
        )
    )

    
    /**
     * 根据配置获取启用的工具
     */
    fun getEnabledTools(config: SessionConfig?): List<HtmlTool> {
        val enabledTypes = config?.enabledTools ?: setOf(HtmlToolType.WRITE_HTML)
        val hasImageModel = !config?.imageModelId.isNullOrBlank()
        
        return getAllTools().filter { tool ->
            tool.type in enabledTypes && 
            // 如果工具需要图像模型，检查是否已配置
            (!tool.type.requiresImageModel || hasImageModel)
        }
    }
    
    // ==================== 控制台日志管理 ====================
    
    /**
     * 添加控制台日志
     */
    fun addConsoleLog(entry: ConsoleLogEntry) {
        consoleLogs.add(entry)
        // 保留最近 100 条
        if (consoleLogs.size > 100) {
            consoleLogs.removeAt(0)
        }
    }
    
    /**
     * 清空控制台日志
     */
    fun clearConsoleLogs() {
        consoleLogs.clear()
    }
    
    /**
     * 获取控制台日志
     */
    fun getConsoleLogs(filter: String = "all"): List<ConsoleLogEntry> {
        return when (filter) {
            "error" -> consoleLogs.filter { it.level == ConsoleLogLevel.ERROR }
            "warn" -> consoleLogs.filter { it.level == ConsoleLogLevel.WARN }
            "log" -> consoleLogs.filter { it.level == ConsoleLogLevel.LOG }
            else -> consoleLogs.toList()
        }
    }
    
    // ==================== 语法错误管理 ====================
    
    /**
     * 添加语法错误
     */
    fun addSyntaxError(error: SyntaxError) {
        syntaxErrors.add(error)
    }
    
    /**
     * 清空语法错误
     */
    fun clearSyntaxErrors() {
        syntaxErrors.clear()
    }
    
    /**
     * 获取语法错误
     */
    fun getSyntaxErrors(): List<SyntaxError> = syntaxErrors.toList()

    
    // ==================== 语法检查 ====================
    
    /**
     * 检查代码语法
     */
    fun checkSyntax(code: String, language: String = "html"): List<SyntaxError> {
        clearSyntaxErrors()
        val errors = when (language.lowercase()) {
            "html", "htm" -> checkHtmlSyntax(code)
            "css" -> checkCssSyntax(code)
            "javascript", "js" -> checkJavaScriptSyntax(code)
            else -> {
                // 对于完整 HTML 文件，检查所有部分
                val htmlErrors = checkHtmlSyntax(code)
                val cssErrors = extractAndCheckCss(code)
                val jsErrors = extractAndCheckJs(code)
                htmlErrors + cssErrors + jsErrors
            }
        }
        errors.forEach { addSyntaxError(it) }
        return errors
    }
    
    /**
     * 检查 HTML 语法
     */
    private fun checkHtmlSyntax(code: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val lines = code.lines()
        
        // Check未闭合的标签
        val tagStack = mutableListOf<Pair<String, Int>>()
        val selfClosingTags = setOf("br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "param", "source", "track", "wbr")
        
        val tagPattern = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*(/?)>")
        
        lines.forEachIndexed { lineNum, line ->
            val matcher = tagPattern.matcher(line)
            while (matcher.find()) {
                val isClosing = matcher.group(1) == "/"
                val tagName = matcher.group(2)?.lowercase() ?: continue
                val isSelfClosing = matcher.group(3) == "/" || tagName in selfClosingTags
                
                if (!isSelfClosing) {
                    if (isClosing) {
                        if (tagStack.isNotEmpty() && tagStack.last().first == tagName) {
                            tagStack.removeAt(tagStack.lastIndex)
                        } else if (tagStack.any { it.first == tagName }) {
                            // 找到匹配但不是最近的，可能有未闭合标签
                            val unclosed = tagStack.takeLastWhile { it.first != tagName }
                            unclosed.forEach { (tag, ln) ->
                                errors.add(SyntaxError(
                                    type = "html",
                                    message = "标签 <$tag> 未正确闭合",
                                    line = ln + 1,
                                    column = 0,
                                    severity = ErrorSeverity.ERROR
                                ))
                            }
                            tagStack.removeAll { it.first in unclosed.map { u -> u.first } || it.first == tagName }
                        } else {
                            errors.add(SyntaxError(
                                type = "html",
                                message = "意外的闭合标签 </$tagName>",
                                line = lineNum + 1,
                                column = matcher.start(),
                                severity = ErrorSeverity.ERROR
                            ))
                        }
                    } else {
                        tagStack.add(tagName to lineNum)
                    }
                }
            }
        }
        
        // 报告未闭合的标签
        tagStack.forEach { (tag, lineNum) ->
            errors.add(SyntaxError(
                type = "html",
                message = "标签 <$tag> 未闭合",
                line = lineNum + 1,
                column = 0,
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return errors
    }

    
    /**
     * 检查 CSS 语法
     */
    private fun checkCssSyntax(code: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val lines = code.lines()
        
        var braceCount = 0
        var inString = false
        var stringChar = ' '
        
        lines.forEachIndexed { lineNum, line ->
            var i = 0
            while (i < line.length) {
                val char = line[i]
                
                if (inString) {
                    if (char == stringChar && (i == 0 || line[i-1] != '\\')) {
                        inString = false
                    }
                } else {
                    when (char) {
                        '"', '\'' -> {
                            inString = true
                            stringChar = char
                        }
                        '{' -> braceCount++
                        '}' -> {
                            braceCount--
                            if (braceCount < 0) {
                                errors.add(SyntaxError(
                                    type = "css",
                                    message = "多余的闭合大括号 '}'",
                                    line = lineNum + 1,
                                    column = i,
                                    severity = ErrorSeverity.ERROR
                                ))
                                braceCount = 0
                            }
                        }
                    }
                }
                i++
            }
        }
        
        if (braceCount > 0) {
            errors.add(SyntaxError(
                type = "css",
                message = "缺少 $braceCount 个闭合大括号 '}'",
                line = lines.size,
                column = 0,
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return errors
    }
    
    /**
     * 检查 JavaScript 语法
     */
    private fun checkJavaScriptSyntax(code: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val lines = code.lines()
        
        var braceCount = 0
        var parenCount = 0
        var bracketCount = 0
        var inString = false
        var stringChar = ' '
        var inMultiLineComment = false
        
        lines.forEachIndexed { lineNum, line ->
            var i = 0
            while (i < line.length) {
                // 跳过单行注释
                if (!inString && !inMultiLineComment && i + 1 < line.length && line[i] == '/' && line[i+1] == '/') {
                    break
                }
                
                // 多行注释开始
                if (!inString && !inMultiLineComment && i + 1 < line.length && line[i] == '/' && line[i+1] == '*') {
                    inMultiLineComment = true
                    i += 2
                    continue
                }
                
                // 多行注释结束
                if (inMultiLineComment && i + 1 < line.length && line[i] == '*' && line[i+1] == '/') {
                    inMultiLineComment = false
                    i += 2
                    continue
                }
                
                if (inMultiLineComment) {
                    i++
                    continue
                }
                
                val char = line[i]
                
                if (inString) {
                    if (char == stringChar && (i == 0 || line[i-1] != '\\')) {
                        inString = false
                    }
                } else {
                    when (char) {
                        '"', '\'', '`' -> {
                            inString = true
                            stringChar = char
                        }
                        '{' -> braceCount++
                        '}' -> braceCount--
                        '(' -> parenCount++
                        ')' -> parenCount--
                        '[' -> bracketCount++
                        ']' -> bracketCount--
                    }
                }
                i++
            }
        }
        
        if (braceCount != 0) {
            errors.add(SyntaxError(
                type = "javascript",
                message = if (braceCount > 0) "缺少 $braceCount 个闭合大括号 '}'" else "多余 ${-braceCount} 个闭合大括号 '}'",
                line = lines.size,
                column = 0,
                severity = ErrorSeverity.ERROR
            ))
        }
        
        if (parenCount != 0) {
            errors.add(SyntaxError(
                type = "javascript",
                message = if (parenCount > 0) "缺少 $parenCount 个闭合小括号 ')'" else "多余 ${-parenCount} 个闭合小括号 ')'",
                line = lines.size,
                column = 0,
                severity = ErrorSeverity.ERROR
            ))
        }
        
        if (bracketCount != 0) {
            errors.add(SyntaxError(
                type = "javascript",
                message = if (bracketCount > 0) "缺少 $bracketCount 个闭合方括号 ']'" else "多余 ${-bracketCount} 个闭合方括号 ']'",
                line = lines.size,
                column = 0,
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return errors
    }

    
    /**
     * 从 HTML 中提取并检查 CSS
     */
    private fun extractAndCheckCss(html: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val stylePattern = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE)
        val matcher = stylePattern.matcher(html)
        
        while (matcher.find()) {
            val css = matcher.group(1) ?: continue
            val cssErrors = checkCssSyntax(css)
            // 调整行号（找到 style 标签在原文中的位置）
            val styleStart = html.substring(0, matcher.start()).count { it == '\n' }
            cssErrors.forEach { error ->
                errors.add(error.copy(line = error.line + styleStart))
            }
        }
        
        return errors
    }
    
    /**
     * 从 HTML 中提取并检查 JavaScript
     */
    private fun extractAndCheckJs(html: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val scriptPattern = Pattern.compile("<script[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE)
        val matcher = scriptPattern.matcher(html)
        
        while (matcher.find()) {
            val js = matcher.group(1) ?: continue
            if (js.isBlank()) continue
            val jsErrors = checkJavaScriptSyntax(js)
            // 调整行号
            val scriptStart = html.substring(0, matcher.start()).count { it == '\n' }
            jsErrors.forEach { error ->
                errors.add(error.copy(line = error.line + scriptStart))
            }
        }
        
        return errors
    }
    
    // ==================== 工具执行 ====================
    
    /**
     * 从工具参数中提取 HTML 内容
     * 支持多种格式：
     * 1. JSON 对象 {"html": "..."} 或 {"content": "..."}
     * 2. 直接的 HTML 字符串
     * 3. JSON 字符串（带转义）
     */
    private fun extractHtmlContent(arguments: String): String {
        val trimmed = arguments.trim()
        
        // 1. 尝试作为 JSON 对象解析
        try {
            val json = JsonParser.parseString(trimmed)
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                // 尝试不同的字段名
                val content = obj.get("html")?.asString 
                    ?: obj.get("content")?.asString
                    ?: obj.get("code")?.asString
                if (!content.isNullOrBlank()) {
                    return content
                }
            } else if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
                // JSON 字符串
                return json.asString
            }
        } catch (e: Exception) {
            Log.d(TAG, "Not a valid JSON, trying as raw HTML: ${e.message}")
        }
        
        // 2. 如果以 <!DOCTYPE 或 <html 开头，直接作为 HTML
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || 
            trimmed.startsWith("<html", ignoreCase = true) ||
            trimmed.startsWith("<", ignoreCase = true)) {
            return trimmed
        }
        
        // 3. 尝试去掉外层引号
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            val unquoted = trimmed.substring(1, trimmed.length - 1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\\", "\\")
            return unquoted
        }
        
        // 4. 返回原始字符串
        return trimmed
    }

    /**
     * 执行工具调用
     */
    fun executeToolCall(toolName: String, arguments: String): ToolExecutionResult {
        Log.d(TAG, "executeToolCall: $toolName, args length: ${arguments.length}, args preview: ${arguments.take(200)}")
        
        return try {
            when (toolName) {
                "write_html" -> {
                    // 提取 HTML 内容和文件名
                    val content = extractHtmlContent(arguments)
                    val filename = try {
                        val json = JsonParser.parseString(arguments).asJsonObject
                        json.get("filename")?.asString ?: "index.html"
                    } catch (e: Exception) {
                        "index.html"
                    }
                    
                    if (content.isBlank()) {
                        Log.e(TAG, "write_html: content is blank, raw args: $arguments")
                        return ToolExecutionResult(
                            success = false,
                            toolName = toolName,
                            result = "错误：HTML 内容为空"
                        )
                    }
                    
                    // 写入文件到项目文件夹
                    val sessionId = currentSessionId
                    if (sessionId != null) {
                        // Check是否已存在同名文件（决定是否创建新版本）
                        val existingFiles = projectFileManager.listFiles(sessionId)
                        val baseName = filename.substringBeforeLast(".")
                        val hasExisting = existingFiles.any { it.getBaseName() == baseName }
                        
                        val fileInfo = projectFileManager.createFile(
                            sessionId = sessionId,
                            filename = filename,
                            content = content,
                            createNewVersion = hasExisting
                        )
                        
                        Log.d(TAG, "write_html: file created at ${fileInfo.path}, version=${fileInfo.version}")
                        
                        currentHtmlCode = content
                        ToolExecutionResult(
                            success = true,
                            toolName = toolName,
                            result = content,
                            isHtml = true,
                            fileInfo = fileInfo
                        )
                    } else {
                        // 兼容旧逻辑：没有 sessionId 时只更新内存
                        currentHtmlCode = content
                        Log.d(TAG, "write_html: no sessionId, only updating memory")
                        ToolExecutionResult(
                            success = true,
                            toolName = toolName,
                            result = content,
                            isHtml = true
                        )
                    }
                }
                "edit_html" -> {
                    val json = JsonParser.parseString(arguments).asJsonObject
                    val operation = json.get("operation")?.asString ?: "replace"
                    val target = json.get("target")?.asString ?: ""
                    val content = json.get("content")?.asString ?: ""
                    
                    if (currentHtmlCode.isBlank()) {
                        return ToolExecutionResult(
                            success = false,
                            toolName = toolName,
                            result = "错误：当前没有 HTML 代码可编辑。请先使用 write_html 创建页面。"
                        )
                    }
                    
                    if (target.isBlank()) {
                        return ToolExecutionResult(
                            success = false,
                            toolName = toolName,
                            result = "错误：target 参数不能为空"
                        )
                    }
                    
                    if (!currentHtmlCode.contains(target)) {
                        return ToolExecutionResult(
                            success = false,
                            toolName = toolName,
                            result = "错误：在当前代码中找不到目标片段。请确保 target 与现有代码完全匹配（包括空格和换行）。"
                        )
                    }
                    
                    val newHtml = when (operation) {
                        "replace" -> {
                            if (content.isBlank()) {
                                return ToolExecutionResult(
                                    success = false,
                                    toolName = toolName,
                                    result = "错误：replace 操作需要提供 content 参数"
                                )
                            }
                            currentHtmlCode.replace(target, content)
                        }
                        "insert_before" -> {
                            if (content.isBlank()) {
                                return ToolExecutionResult(
                                    success = false,
                                    toolName = toolName,
                                    result = "错误：insert_before 操作需要提供 content 参数"
                                )
                            }
                            currentHtmlCode.replace(target, content + target)
                        }
                        "insert_after" -> {
                            if (content.isBlank()) {
                                return ToolExecutionResult(
                                    success = false,
                                    toolName = toolName,
                                    result = "错误：insert_after 操作需要提供 content 参数"
                                )
                            }
                            currentHtmlCode.replace(target, target + content)
                        }
                        "delete" -> {
                            currentHtmlCode.replace(target, "")
                        }
                        else -> {
                            return ToolExecutionResult(
                                success = false,
                                toolName = toolName,
                                result = "错误：未知操作类型 '$operation'，支持的操作：replace, insert_before, insert_after, delete"
                            )
                        }
                    }
                    
                    currentHtmlCode = newHtml  // Update当前 HTML
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = newHtml,
                        isHtml = true,
                        isEdit = true
                    )
                }
                "get_console_logs" -> {
                    val json = JsonParser.parseString(arguments).asJsonObject
                    val filter = json.get("filter")?.asString ?: "all"
                    val logs = getConsoleLogs(filter)
                    val result = if (logs.isEmpty()) {
                        "控制台暂无日志"
                    } else {
                        logs.joinToString("\n") { log ->
                            "[${log.level.name}] ${log.message}" + 
                            (log.source?.let { " (来源: $it:${log.lineNumber})" } ?: "")
                        }
                    }
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = result
                    )
                }
                "check_syntax" -> {
                    val json = JsonParser.parseString(arguments).asJsonObject
                    val code = json.get("code")?.asString ?: ""
                    val language = json.get("language")?.asString ?: "html"
                    val errors = checkSyntax(code, language)
                    val result = if (errors.isEmpty()) {
                        "语法检查通过，未发现错误"
                    } else {
                        "发现 ${errors.size} 个问题:\n" + errors.joinToString("\n") { error ->
                            "- [${error.severity.name}] 第${error.line}行: ${error.message}"
                        }
                    }
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = result,
                        syntaxErrors = errors
                    )
                }
                "auto_fix" -> {
                    // auto_fix 工具实际上是让 AI 根据错误信息修复代码
                    // 这里只返回确认信息，实际修复由 AI 完成
                    val json = JsonParser.parseString(arguments).asJsonObject
                    val code = json.get("code")?.asString ?: ""
                    val errorsArray = json.getAsJsonArray("errors")
                    val errorsList = errorsArray?.map { it.asString } ?: emptyList()
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = "已收到修复请求，需要修复 ${errorsList.size} 个问题。请使用 write_html 工具输出修复后的代码。"
                    )
                }
                "generate_image" -> {
                    // 图像生成需要异步处理，这里返回一个标记
                    val json = JsonParser.parseString(arguments).asJsonObject
                    val prompt = json.get("prompt")?.asString ?: ""
                    val style = json.get("style")?.asString ?: "illustration"
                    val size = json.get("size")?.asString ?: "medium"
                    
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = "IMAGE_GENERATION_PENDING:$prompt|$style|$size",
                        isImageGeneration = true
                    )
                }
                else -> {
                    ToolExecutionResult(
                        success = false,
                        toolName = toolName,
                        result = "未知工具: $toolName"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution failed: $toolName", e)
            ToolExecutionResult(
                success = false,
                toolName = toolName,
                result = "工具执行失败: ${e.message}"
            )
        }
    }
    
    /**
     * 执行图像生成
     */
    suspend fun executeImageGeneration(
        prompt: String,
        style: String,
        size: String,
        sessionConfig: SessionConfig?
    ): ToolExecutionResult {
        return try {
            val imageModelId = sessionConfig?.imageModelId
            if (imageModelId.isNullOrBlank()) {
                return ToolExecutionResult(
                    success = false,
                    toolName = "generate_image",
                    result = "错误：未配置图像生成模型"
                )
            }
            
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            val imageModel = savedModels.find { it.id == imageModelId }
            if (imageModel == null) {
                return ToolExecutionResult(
                    success = false,
                    toolName = "generate_image",
                    result = "错误：找不到配置的图像模型"
                )
            }
            
            val apiKey = apiKeys.find { it.id == imageModel.apiKeyId }
            if (apiKey == null) {
                return ToolExecutionResult(
                    success = false,
                    toolName = "generate_image",
                    result = "错误：找不到图像模型对应的 API Key"
                )
            }
            
            // Build增强的提示词
            val enhancedPrompt = buildImagePrompt(prompt, style)
            val dimensions = getSizeDimensions(size)
            
            // 调用图像生成 API
            val result = aiClient.generateImage(
                context = context,
                prompt = enhancedPrompt,
                apiKey = apiKey,
                model = imageModel,
                width = dimensions.first,
                height = dimensions.second
            )
            
            result.fold(
                onSuccess = { imageBase64 ->
                    ToolExecutionResult(
                        success = true,
                        toolName = "generate_image",
                        result = "data:image/png;base64,$imageBase64",
                        isImageGeneration = true,
                        imageData = imageBase64
                    )
                },
                onFailure = { error ->
                    ToolExecutionResult(
                        success = false,
                        toolName = "generate_image",
                        result = "图像生成失败: ${error.message}"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            ToolExecutionResult(
                success = false,
                toolName = "generate_image",
                result = "图像生成失败: ${e.message}"
            )
        }
    }
    
    /**
     * 构建图像生成提示词
     */
    private fun buildImagePrompt(prompt: String, style: String): String {
        val styleHint = when (style) {
            "realistic" -> "photorealistic, high quality, detailed"
            "cartoon" -> "cartoon style, colorful, fun"
            "illustration" -> "digital illustration, artistic, clean lines"
            "icon" -> "simple icon, flat design, minimal, centered"
            "abstract" -> "abstract art, creative, artistic"
            "minimalist" -> "minimalist, simple, clean, white space"
            else -> "digital illustration"
        }
        return "$prompt, $styleHint"
    }
    
    /**
     * 获取尺寸
     */
    private fun getSizeDimensions(size: String): Pair<Int, Int> {
        return when (size) {
            "small" -> 256 to 256
            "medium" -> 512 to 512
            "large" -> 1024 to 1024
            else -> 512 to 512
        }
    }
    
    // ==================== 流式开发 ====================
    
    /**
     * 流式开发 HTML
     * 文件直接写入项目文件夹，支持版本迭代
     * 
     * 修复：
     * 1. 使用 emitAll 替代嵌套 collect
     * 2. 添加超时保护
     * 3. 简化 HTML 提取逻辑
     */
    fun developWithStream(
        requirement: String,
        currentHtml: String? = null,
        sessionConfig: SessionConfig? = null,
        model: SavedModel? = null,
        sessionId: String? = null  // SessionID，用于文件操作
    ): Flow<HtmlAgentEvent> = flow {
        try {
            Log.d(TAG, "developWithStream started with requirement: ${requirement.take(100)}, sessionId: $sessionId")
            
            // Set当前会话ID
            if (sessionId != null) {
                currentSessionId = sessionId
            }
            
            // Get AI 配置
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            if (apiKeys.isEmpty()) {
                emit(HtmlAgentEvent.Error("请先在 AI 设置中配置 API Key"))
                return@flow
            }
            
            // Select模型
            val selectedModel = selectModel(model, savedModels)
            if (selectedModel == null) {
                emit(HtmlAgentEvent.Error("请先在 AI 设置中添加并保存模型"))
                return@flow
            }
            
            Log.d(TAG, "Using model: ${selectedModel.model.id}")
            
            val apiKey = apiKeys.find { it.id == selectedModel.apiKeyId }
            if (apiKey == null) {
                emit(HtmlAgentEvent.Error("找不到模型对应的 API Key"))
                return@flow
            }
            
            emit(HtmlAgentEvent.StateChange(HtmlAgentState.GENERATING))
            
            // Set当前 HTML（从最新版本文件读取）
            if (sessionId != null && currentHtml.isNullOrBlank()) {
                val latestFile = projectFileManager.getLatestVersion(sessionId, "index")
                if (latestFile != null) {
                    currentHtmlCode = projectFileManager.readFile(sessionId, latestFile.name) ?: ""
                }
            } else if (!currentHtml.isNullOrBlank()) {
                currentHtmlCode = currentHtml
            }
            
            // Check模型是否支持工具调用
            // 某些模型/平台不支持 OpenAI 格式的工具调用
            val modelId = selectedModel.model.id.lowercase()
            val providerName = apiKey.provider.name.lowercase()
            val baseUrl = (apiKey.baseUrl ?: "").lowercase()
            
            // 检测不支持工具调用的情况
            val supportsToolCalling = when {
                // SiliconFlow 平台的某些模型可能不支持工具调用
                baseUrl.contains("siliconflow") -> {
                    // 只有明确支持工具调用的模型才使用
                    modelId.contains("gpt") || modelId.contains("claude") || modelId.contains("glm-4")
                }
                // Qwen 模型在某些平台可能不支持
                modelId.contains("qwen") && !modelId.contains("qwen-max") -> {
                    // Qwen-Max 支持工具调用，其他 Qwen 模型可能不支持
                    false
                }
                // DeepSeek Coder 可能不支持工具调用
                modelId.contains("deepseek") && modelId.contains("coder") -> false
                // Default假设支持
                else -> true
            }
            
            Log.d(TAG, "Model: $modelId, Provider: $providerName, BaseUrl: $baseUrl, SupportsToolCalling: $supportsToolCalling")
            
            // 尝试使用工具调用模式，失败则回退到简单模式
            var useSimpleMode = !supportsToolCalling  // 如果不支持工具调用，直接使用简单模式
            var toolCallError: String? = null
            
            if (!useSimpleMode) {
                try {
                    // 带超时的工具调用模式
                    withTimeout(STREAM_TIMEOUT_MS) {
                        developWithToolCalling(requirement, sessionConfig, selectedModel, apiKey)
                            .collect { event -> emit(event) }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Tool calling timeout, falling back to simple mode")
                    useSimpleMode = true
                    toolCallError = "请求超时"
                } catch (e: Exception) {
                    Log.w(TAG, "Tool calling failed: ${e.message}, falling back to simple mode")
                    useSimpleMode = true
                    toolCallError = e.message
                }
            } else {
                Log.d(TAG, "Skipping tool calling mode (not supported), using simple mode directly")
            }
            
            // 回退到简单模式
            if (useSimpleMode) {
                Log.d(TAG, "Using simple stream mode as fallback")
                try {
                    withTimeout(STREAM_TIMEOUT_MS) {
                        developWithSimpleStreamInternal(requirement, currentHtmlCode.takeIf { it.isNotBlank() }, sessionConfig, selectedModel, apiKey)
                            .collect { event -> emit(event) }
                    }
                } catch (e: TimeoutCancellationException) {
                    emit(HtmlAgentEvent.Error("请求超时，请检查网络连接后重试"))
                } catch (e: Exception) {
                    emit(HtmlAgentEvent.Error("发生错误: ${e.message ?: toolCallError ?: "未知错误"}"))
                }
            }
            
        } catch (e: CancellationException) {
            throw e  // 重新抛出取消异常
        } catch (e: Exception) {
            Log.e(TAG, "Error in developWithStream", e)
            emit(HtmlAgentEvent.Error("发生错误: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 使用工具调用模式开发 HTML（内部方法）
     */
    private fun developWithToolCalling(
        requirement: String,
        sessionConfig: SessionConfig?,
        selectedModel: SavedModel,
        apiKey: ApiKeyConfig
    ): Flow<HtmlAgentEvent> = flow {
        // Get启用的工具
        val enabledTools = getEnabledTools(sessionConfig)
        
        // Build系统提示词（包含启用的工具描述）
        val systemPrompt = buildToolCallingSystemPrompt(sessionConfig, currentHtmlCode.takeIf { it.isNotBlank() }, enabledTools)
        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to requirement)
        )
        
        // 将启用的工具转换为 OpenAI 格式
        val tools = enabledTools.map { it.toOpenAiFormat() }
        
        Log.d(TAG, "Using tool calling mode with ${enabledTools.size} tools: ${enabledTools.map { it.name }}")
        
        val htmlBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()
        val textBuilder = StringBuilder()
        var toolCallStarted = false
        var isCapturingCodeFromText = false
        var isCapturingCodeFromArgs = false
        var codeBlockStarted = false
        var streamCompleted = false
        
        // 跟踪当前工具调用
        var currentToolName = ""
        var currentToolCallId = ""
        val currentToolArgs = StringBuilder()
        val pendingToolCalls = mutableListOf<Triple<String, String, String>>() // (toolName, toolCallId, arguments)
        
        aiClient.chatStreamWithTools(
            apiKey = apiKey,
            model = selectedModel.model,
            messages = messages,
            tools = tools,
            temperature = 0.7f
        ).collect { event ->
            when (event) {
                is ToolStreamEvent.Started -> {
                    emit(HtmlAgentEvent.StateChange(HtmlAgentState.GENERATING))
                }
                is ToolStreamEvent.ThinkingDelta -> {
                    thinkingBuilder.append(event.delta)
                    emit(HtmlAgentEvent.ThinkingDelta(event.delta, event.accumulated))
                }
                is ToolStreamEvent.TextDelta -> {
                    val accumulated = event.accumulated
                    
                    // 如果已经从工具参数获取了代码，文本流只作为普通文本处理
                    if (isCapturingCodeFromArgs) {
                        textBuilder.append(event.delta)
                        emit(HtmlAgentEvent.TextDelta(event.delta, textBuilder.toString()))
                        return@collect
                    }
                    
                    // 检测代码块开始 ```html 或 ```（备选方案：AI 在文本中输出代码）
                    if (!codeBlockStarted) {
                        // 检测 ```html 代码块
                        val htmlCodeBlockIndex = accumulated.indexOf("```html", ignoreCase = true)
                        // 检测 ``` 代码块（后面跟着 HTML 内容）
                        val genericCodeBlockIndex = if (htmlCodeBlockIndex < 0) {
                            val idx = accumulated.indexOf("```")
                            // Check ``` 后面是否是 HTML 内容
                            if (idx >= 0) {
                                val afterBlock = accumulated.substring(idx + 3).trimStart()
                                if (afterBlock.startsWith("<!DOCTYPE", ignoreCase = true) || 
                                    afterBlock.startsWith("<html", ignoreCase = true) ||
                                    afterBlock.startsWith("<head", ignoreCase = true) ||
                                    afterBlock.startsWith("<body", ignoreCase = true)) {
                                    idx
                                } else -1
                            } else -1
                        } else -1
                        
                        val codeBlockIndex = if (htmlCodeBlockIndex >= 0) htmlCodeBlockIndex else genericCodeBlockIndex
                        val codeBlockMarkerLength = if (htmlCodeBlockIndex >= 0) 7 else 3  // "```html" vs "```"
                        
                        if (codeBlockIndex >= 0) {
                            codeBlockStarted = true
                            isCapturingCodeFromText = true
                            if (!toolCallStarted) {
                                toolCallStarted = true
                                emit(HtmlAgentEvent.ToolCallStart("write_html", "text-stream"))
                            }
                            
                            // 提取代码块开始前的文本
                            val textBefore = accumulated.substring(0, codeBlockIndex)
                            if (textBefore.isNotBlank()) {
                                textBuilder.append(textBefore)
                            }
                            
                            // 提取代码块开始后的内容
                            val codeStart = codeBlockIndex + codeBlockMarkerLength
                            val actualStart = if (codeStart < accumulated.length && accumulated[codeStart] == '\n') {
                                codeStart + 1
                            } else {
                                codeStart
                            }
                            if (actualStart < accumulated.length) {
                                val codeContent = accumulated.substring(actualStart)
                                val endIndex = codeContent.indexOf("```")
                                val code = if (endIndex >= 0) {
                                    isCapturingCodeFromText = false
                                    codeContent.substring(0, endIndex)
                                } else {
                                    codeContent
                                }
                                if (code.isNotEmpty()) {
                                    htmlBuilder.clear()
                                    htmlBuilder.append(code)
                                    emit(HtmlAgentEvent.CodeDelta(code, htmlBuilder.toString()))
                                }
                            }
                            return@collect
                        }
                        
                        // 检测直接的 HTML 内容（不带代码块标记）
                        val doctypeIndex = accumulated.indexOf("<!DOCTYPE", ignoreCase = true)
                        val htmlTagIndex = accumulated.indexOf("<html", ignoreCase = true)
                        val directHtmlIndex = when {
                            doctypeIndex >= 0 && htmlTagIndex >= 0 -> minOf(doctypeIndex, htmlTagIndex)
                            doctypeIndex >= 0 -> doctypeIndex
                            htmlTagIndex >= 0 -> htmlTagIndex
                            else -> -1
                        }
                        
                        if (directHtmlIndex >= 0) {
                            codeBlockStarted = true
                            isCapturingCodeFromText = true
                            if (!toolCallStarted) {
                                toolCallStarted = true
                                emit(HtmlAgentEvent.ToolCallStart("write_html", "text-stream"))
                            }
                            
                            // 提取 HTML 开始前的文本
                            val textBefore = accumulated.substring(0, directHtmlIndex)
                            if (textBefore.isNotBlank()) {
                                textBuilder.append(textBefore)
                            }
                            
                            // 提取 HTML 内容
                            val htmlContent = accumulated.substring(directHtmlIndex)
                            if (htmlContent.isNotEmpty()) {
                                htmlBuilder.clear()
                                htmlBuilder.append(htmlContent)
                                emit(HtmlAgentEvent.CodeDelta(htmlContent, htmlBuilder.toString()))
                            }
                            return@collect
                        }
                    }
                    
                    if (isCapturingCodeFromText) {
                        // 检测代码块结束 ```
                        if (event.delta.contains("```")) {
                            val remainingCode = event.delta.substringBefore("```")
                            if (remainingCode.isNotEmpty()) {
                                htmlBuilder.append(remainingCode)
                                emit(HtmlAgentEvent.CodeDelta(remainingCode, htmlBuilder.toString()))
                            }
                            isCapturingCodeFromText = false
                            // 代码块后的文本
                            val textAfter = event.delta.substringAfter("```", "")
                            if (textAfter.isNotBlank()) {
                                textBuilder.append(textAfter)
                                emit(HtmlAgentEvent.TextDelta(textAfter, textBuilder.toString()))
                            }
                        } else {
                            htmlBuilder.append(event.delta)
                            emit(HtmlAgentEvent.CodeDelta(event.delta, htmlBuilder.toString()))
                        }
                    } else {
                        // 普通文本
                        textBuilder.append(event.delta)
                        emit(HtmlAgentEvent.TextDelta(event.delta, textBuilder.toString()))
                    }
                }
                is ToolStreamEvent.ToolCallStart -> {
                    Log.d(TAG, "Tool call started: ${event.toolName}")
                    currentToolName = event.toolName
                    currentToolCallId = event.toolCallId
                    currentToolArgs.clear()
                    toolCallStarted = true
                    emit(HtmlAgentEvent.ToolCallStart(event.toolName, event.toolCallId))
                    
                    // 只有 write_html 和 edit_html 需要捕获代码
                    if (event.toolName == "write_html" || event.toolName == "edit_html") {
                        isCapturingCodeFromArgs = true
                        htmlBuilder.clear()
                    }
                }
                is ToolStreamEvent.ToolArgumentsDelta -> {
                    currentToolArgs.clear()
                    currentToolArgs.append(event.accumulated)
                    
                    // 只有 write_html 需要实时显示代码
                    if (currentToolName == "write_html" && isCapturingCodeFromArgs) {
                        val args = event.accumulated
                        // 尝试从 JSON 参数中提取 HTML
                        val htmlContent = extractHtmlFromArgsIncremental(args)
                        if (htmlContent.isNotEmpty() && htmlContent != htmlBuilder.toString()) {
                            val delta = if (htmlBuilder.isEmpty()) {
                                htmlContent
                            } else {
                                // 计算增量
                                if (htmlContent.startsWith(htmlBuilder.toString())) {
                                    htmlContent.substring(htmlBuilder.length)
                                } else {
                                    htmlContent
                                }
                            }
                            htmlBuilder.clear()
                            htmlBuilder.append(htmlContent)
                            if (delta.isNotEmpty()) {
                                emit(HtmlAgentEvent.CodeDelta(delta, htmlBuilder.toString()))
                            }
                        }
                    }
                }
                is ToolStreamEvent.ToolCallComplete -> {
                    Log.d(TAG, "Tool call complete: ${event.toolName}, args length: ${event.arguments.length}")
                    
                    // 记录工具调用，稍后统一执行
                    pendingToolCalls.add(Triple(event.toolName, event.toolCallId, event.arguments))
                    
                    // 对于 write_html，提取最终 HTML
                    if (event.toolName == "write_html") {
                        val finalHtml = extractHtmlFromArgsFinal(event.arguments)
                        if (finalHtml.isNotEmpty()) {
                            htmlBuilder.clear()
                            htmlBuilder.append(finalHtml)
                            emit(HtmlAgentEvent.CodeDelta("", htmlBuilder.toString()))
                        }
                        isCapturingCodeFromArgs = false
                    }
                    
                    // Reset当前工具状态
                    currentToolName = ""
                    currentToolCallId = ""
                    currentToolArgs.clear()
                }
                is ToolStreamEvent.Done -> {
                    streamCompleted = true
                    var finalHtml = htmlBuilder.toString().trim()
                    val finalText = textBuilder.toString().trim()
                    Log.d(TAG, "Stream done, finalHtml length: ${finalHtml.length}, finalText length: ${finalText.length}, pendingToolCalls: ${pendingToolCalls.size}")
                    
                    // 如果没有从工具调用获取到 HTML，尝试从文本内容中提取
                    // 这是为了兼容不支持原生工具调用的模型（如某些 Qwen 模型）
                    if (finalHtml.isEmpty() && finalText.isNotEmpty()) {
                        Log.d(TAG, "No HTML from tool calls, trying to extract from text content")
                        val extractedHtml = extractHtmlFromText(finalText)
                        if (extractedHtml.isNotEmpty()) {
                            Log.d(TAG, "Extracted HTML from text, length: ${extractedHtml.length}")
                            finalHtml = extractedHtml
                            htmlBuilder.clear()
                            htmlBuilder.append(finalHtml)
                        }
                    }
                    
                    // 如果有待处理的工具调用，执行它们
                    if (pendingToolCalls.isNotEmpty()) {
                        for ((toolName, _, arguments) in pendingToolCalls) {
                            Log.d(TAG, "Executing pending tool call: $toolName")
                            
                            when (toolName) {
                                "write_html" -> {
                                    // write_html 已经在上面处理了 HTML 提取
                                    if (finalHtml.isNotEmpty()) {
                                        currentHtmlCode = finalHtml
                                        
                                        // 写入文件到项目文件夹
                                        var fileInfo: ProjectFileInfo? = null
                                        val sid = currentSessionId
                                        if (sid != null) {
                                            val existingFiles = projectFileManager.listFiles(sid)
                                            val hasExisting = existingFiles.any { it.getBaseName() == "index" }
                                            
                                            fileInfo = projectFileManager.createFile(
                                                sessionId = sid,
                                                filename = "index.html",
                                                content = finalHtml,
                                                createNewVersion = hasExisting
                                            )
                                            
                                            Log.d(TAG, "File created: ${fileInfo.name}, version=${fileInfo.version}")
                                            emit(HtmlAgentEvent.FileCreated(fileInfo, isNewVersion = hasExisting))
                                        }
                                        
                                        val result = ToolExecutionResult(
                                            success = true,
                                            toolName = "write_html",
                                            result = finalHtml,
                                            isHtml = true,
                                            fileInfo = fileInfo
                                        )
                                        emit(HtmlAgentEvent.ToolExecuted(result))
                                        emit(HtmlAgentEvent.HtmlComplete(finalHtml))
                                    }
                                }
                                "edit_html", "get_console_logs", "check_syntax", "auto_fix" -> {
                                    // Execute其他工具
                                    val result = executeToolCall(toolName, arguments)
                                    emit(HtmlAgentEvent.ToolExecuted(result))
                                    
                                    // If it is edit_html 且成功，更新 HTML
                                    if (toolName == "edit_html" && result.success && result.isHtml) {
                                        currentHtmlCode = result.result
                                        emit(HtmlAgentEvent.HtmlComplete(result.result))
                                        
                                        // 写入文件
                                        val sid = currentSessionId
                                        if (sid != null) {
                                            val existingFiles = projectFileManager.listFiles(sid)
                                            val hasExisting = existingFiles.any { it.getBaseName() == "index" }
                                            
                                            val fileInfo = projectFileManager.createFile(
                                                sessionId = sid,
                                                filename = "index.html",
                                                content = result.result,
                                                createNewVersion = hasExisting
                                            )
                                            emit(HtmlAgentEvent.FileCreated(fileInfo, isNewVersion = hasExisting))
                                        }
                                    }
                                }
                                "generate_image" -> {
                                    // 图像生成需要异步处理
                                    val result = executeToolCall(toolName, arguments)
                                    if (result.isImageGeneration && result.result.startsWith("IMAGE_GENERATION_PENDING:")) {
                                        // Parse图像生成参数
                                        val params = result.result.removePrefix("IMAGE_GENERATION_PENDING:").split("|")
                                        if (params.size >= 3) {
                                            val prompt = params[0]
                                            val style = params[1]
                                            val size = params[2]
                                            emit(HtmlAgentEvent.ImageGenerating(prompt))
                                            
                                            // Execute图像生成
                                            val imageResult = executeImageGeneration(prompt, style, size, sessionConfig)
                                            emit(HtmlAgentEvent.ToolExecuted(imageResult))
                                            
                                            if (imageResult.success && imageResult.imageData != null) {
                                                emit(HtmlAgentEvent.ImageGenerated(imageResult.imageData, prompt))
                                            }
                                        }
                                    } else {
                                        emit(HtmlAgentEvent.ToolExecuted(result))
                                    }
                                }
                                else -> {
                                    Log.w(TAG, "Unknown tool: $toolName")
                                }
                            }
                        }
                    }
                    
                    // 如果没有工具调用但有文本内容
                    if (pendingToolCalls.isEmpty() && finalText.isNotEmpty()) {
                        Log.d(TAG, "No tool calls, but has text content")
                        
                        // 如果从文本中提取到了 HTML，执行写入操作
                        if (finalHtml.isNotEmpty()) {
                            Log.d(TAG, "Executing write_html from extracted HTML")
                            currentHtmlCode = finalHtml
                            
                            // 写入文件到项目文件夹
                            var fileInfo: ProjectFileInfo? = null
                            val sid = currentSessionId
                            if (sid != null) {
                                val existingFiles = projectFileManager.listFiles(sid)
                                val hasExisting = existingFiles.any { it.getBaseName() == "index" }
                                
                                fileInfo = projectFileManager.createFile(
                                    sessionId = sid,
                                    filename = "index.html",
                                    content = finalHtml,
                                    createNewVersion = hasExisting
                                )
                                
                                Log.d(TAG, "File created from extracted HTML: ${fileInfo.name}, version=${fileInfo.version}")
                                emit(HtmlAgentEvent.FileCreated(fileInfo, isNewVersion = hasExisting))
                            }
                            
                            val result = ToolExecutionResult(
                                success = true,
                                toolName = "write_html",
                                result = finalHtml,
                                isHtml = true,
                                fileInfo = fileInfo
                            )
                            emit(HtmlAgentEvent.ToolExecuted(result))
                            emit(HtmlAgentEvent.HtmlComplete(finalHtml))
                        } else {
                            // AI 返回了文字但没有代码，检查是否是"承诺要做但没做"的情况
                            val textLower = finalText.lowercase()
                            val isPromiseWithoutAction = textLower.contains("我来") || 
                                textLower.contains("我将") || 
                                textLower.contains("我会") ||
                                textLower.contains("让我") ||
                                textLower.contains("i will") ||
                                textLower.contains("i'll") ||
                                textLower.contains("let me")
                            
                            if (isPromiseWithoutAction) {
                                Log.w(TAG, "AI promised to create but didn't output code, triggering fallback")
                                throw Exception("AI 承诺创建但未输出代码，触发回退")
                            }
                            // 如果只是普通对话（如问候、解释等），允许通过
                            Log.d(TAG, "AI returned text without code, treating as conversation")
                        }
                    }
                    
                    // 如果工具调用模式没有返回任何有效内容，抛出异常触发回退
                    if (pendingToolCalls.isEmpty() && finalHtml.isEmpty() && finalText.isEmpty()) {
                        Log.w(TAG, "Tool calling returned empty content, will fallback to simple mode")
                        throw Exception("工具调用返回空内容")
                    }
                    
                    emit(HtmlAgentEvent.StateChange(HtmlAgentState.COMPLETED))
                    emit(HtmlAgentEvent.Completed(textBuilder.toString(), emptyList()))
                }
                is ToolStreamEvent.Error -> {
                    throw Exception(event.message)
                }
            }
        }
        
        // 如果流没有正常完成，抛出异常
        if (!streamCompleted) {
            throw Exception("流式响应未正常完成")
        }
    }
    
    /**
     * 从工具参数中提取 HTML 内容（增量模式，用于流式传输）
     * 简化版本，专注于从不完整 JSON 中提取
     */
    private fun extractHtmlFromArgsIncremental(args: String): String {
        if (args.isBlank()) return ""
        
        val trimmed = args.trim()
        
        // Find "html": " 后的内容（流式传输时 JSON 可能不完整）
        val htmlKeyIndex = trimmed.indexOf("\"html\"")
        if (htmlKeyIndex < 0) return ""
        
        // 找到冒号后的引号
        val colonIndex = trimmed.indexOf(':', htmlKeyIndex + 6)
        if (colonIndex < 0) return ""
        
        val quoteIndex = trimmed.indexOf('"', colonIndex + 1)
        if (quoteIndex < 0) return ""
        
        // 提取引号后的内容
        val startIndex = quoteIndex + 1
        if (startIndex >= trimmed.length) return ""
        
        // 找到结束引号（考虑转义）
        var endIndex = startIndex
        var escaped = false
        while (endIndex < trimmed.length) {
            val c = trimmed[endIndex]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                break
            }
            endIndex++
        }
        
        if (endIndex <= startIndex) return ""
        
        // 解码转义字符
        val extracted = trimmed.substring(startIndex, endIndex)
        return decodeJsonString(extracted)
    }
    
    /**
     * 从工具参数中提取 HTML 内容（最终模式，用于完整 JSON）
     */
    private fun extractHtmlFromArgsFinal(args: String): String {
        if (args.isBlank()) return ""
        
        val trimmed = args.trim()
        
        // 1. 尝试作为完整 JSON 对象解析
        try {
            val json = JsonParser.parseString(trimmed)
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                val content = obj.get("html")?.asString 
                    ?: obj.get("content")?.asString
                    ?: obj.get("code")?.asString
                if (!content.isNullOrBlank()) {
                    return content
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "JSON parse failed, trying incremental extraction: ${e.message}")
        }
        
        // 2. 回退到增量提取
        return extractHtmlFromArgsIncremental(trimmed)
    }
    
    /**
     * 解码 JSON 字符串中的转义字符
     */
    private fun decodeJsonString(str: String): String {
        return str
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\"", "\"")
            .replace("\\\\/", "/")
            .replace("\\\\", "\\")
    }
    
    /**
     * 从文本内容中提取 HTML 代码
     * 用于兼容不支持原生工具调用的模型
     * 
     * 支持以下格式：
     * 1. ```html ... ``` 代码块
     * 2. 直接的 <!DOCTYPE html> ... </html> 内容
     * 3. 直接的 <html> ... </html> 内容
     */
    private fun extractHtmlFromText(text: String): String {
        if (text.isBlank()) return ""
        
        // 1. 尝试从 ```html 代码块中提取
        val codeBlockPattern = Pattern.compile("```html\\s*\\n?([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE)
        val codeBlockMatcher = codeBlockPattern.matcher(text)
        if (codeBlockMatcher.find()) {
            val extracted = codeBlockMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                Log.d(TAG, "Extracted HTML from code block, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 2. 尝试从 ``` 代码块中提取（不带语言标记）
        val genericCodeBlockPattern = Pattern.compile("```\\s*\\n?([\\s\\S]*?)```")
        val genericMatcher = genericCodeBlockPattern.matcher(text)
        while (genericMatcher.find()) {
            val extracted = genericMatcher.group(1)?.trim() ?: ""
            // Check是否是 HTML 内容
            if (extracted.contains("<!DOCTYPE", ignoreCase = true) || 
                extracted.contains("<html", ignoreCase = true)) {
                Log.d(TAG, "Extracted HTML from generic code block, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 3. 尝试直接提取 <!DOCTYPE html> ... </html>
        val doctypePattern = Pattern.compile("(<!DOCTYPE\\s+html[\\s\\S]*?</html>)", Pattern.CASE_INSENSITIVE)
        val doctypeMatcher = doctypePattern.matcher(text)
        if (doctypeMatcher.find()) {
            val extracted = doctypeMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                Log.d(TAG, "Extracted HTML from DOCTYPE pattern, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 4. 尝试直接提取 <html> ... </html>
        val htmlPattern = Pattern.compile("(<html[\\s\\S]*?</html>)", Pattern.CASE_INSENSITIVE)
        val htmlMatcher = htmlPattern.matcher(text)
        if (htmlMatcher.find()) {
            val extracted = htmlMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                Log.d(TAG, "Extracted HTML from html tag pattern, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 5. 如果文本本身就是以 <!DOCTYPE 或 <html 开头，直接返回
        val trimmedText = text.trim()
        if (trimmedText.startsWith("<!DOCTYPE", ignoreCase = true) || 
            trimmedText.startsWith("<html", ignoreCase = true)) {
            Log.d(TAG, "Text itself is HTML, length: ${trimmedText.length}")
            return trimmedText
        }
        
        return ""
    }
    
    /**
     * 构建工具调用模式的系统提示词
     * @param config 会话配置
     * @param currentHtml 当前 HTML 代码
     * @param enabledTools 启用的工具列表
     */
    private fun buildToolCallingSystemPrompt(config: SessionConfig?, currentHtml: String?, enabledTools: List<HtmlTool> = emptyList()): String {
        val hasWriteHtml = enabledTools.any { it.name == "write_html" }
        val hasEditHtml = enabledTools.any { it.name == "edit_html" }
        val hasCheckSyntax = enabledTools.any { it.name == "check_syntax" }
        val hasGetConsoleLogs = enabledTools.any { it.name == "get_console_logs" }
        val hasAutoFix = enabledTools.any { it.name == "auto_fix" }
        val hasGenerateImage = enabledTools.any { it.name == "generate_image" }
        
        return buildString {
            appendLine("你是移动端前端开发专家，为手机APP WebView创建HTML页面。")
            appendLine()
            
            // 【最重要的规则】直接执行，不要解释
            appendLine("# 【最重要规则】直接执行")
            appendLine("- 当用户要求创建/修改网页时，**立即**调用工具或输出代码")
            appendLine("- **禁止**先说\"我来为您创建...\"然后不输出代码")
            appendLine("- **禁止**只解释要做什么而不实际执行")
            appendLine("- 如果要创建网页，必须在回复中包含完整的 HTML 代码")
            appendLine()
            
            // 工具使用规则
            appendLine("# 工具使用")
            if (enabledTools.isNotEmpty()) {
                appendLine("可用工具：")
                enabledTools.forEach { tool ->
                    appendLine("- **${tool.name}**: ${tool.description}")
                }
                appendLine()
            }
            
            appendLine("## 必须使用工具的情况：")
            appendLine("- 创建新网页 → 使用 write_html")
            if (hasEditHtml) {
                appendLine("- 小范围修改代码 → 使用 edit_html")
            }
            if (hasCheckSyntax) {
                appendLine("- 检查代码语法 → 使用 check_syntax")
            }
            if (hasGetConsoleLogs) {
                appendLine("- 查看运行时错误 → 使用 get_console_logs")
            }
            if (hasAutoFix) {
                appendLine("- 自动修复错误 → 使用 auto_fix")
            }
            if (hasGenerateImage) {
                appendLine("- 生成图像 → 使用 generate_image")
            }
            appendLine()
            
            // 工具详细说明
            appendLine("## 工具详细说明")
            appendLine()
            
            if (hasWriteHtml) {
                appendLine("### write_html")
                appendLine("创建或完全重写 HTML 页面。将完整的 HTML 代码作为 html 参数传入。")
                appendLine("参数：")
                appendLine("- html (必需): 完整的 HTML 代码，包含 <!DOCTYPE html> 声明")
                appendLine("- filename (可选): 文件名，默认为 index.html")
                appendLine()
            }
            
            if (hasEditHtml) {
                appendLine("### edit_html")
                appendLine("编辑现有 HTML 代码的指定部分。适合小范围精确修改。")
                appendLine("参数：")
                appendLine("- operation (必需): 操作类型 - replace/insert_before/insert_after/delete")
                appendLine("- target (必需): 要定位的目标代码片段（必须精确匹配现有代码）")
                appendLine("- content (可选): 新的代码内容（delete 操作时可省略）")
                appendLine()
            }
            
            if (hasCheckSyntax) {
                appendLine("### check_syntax")
                appendLine("检查代码语法错误，返回错误列表和位置信息。")
                appendLine("参数：")
                appendLine("- code (必需): 要检查的代码内容")
                appendLine("- language (可选): 代码语言 - html/css/javascript")
                appendLine()
            }
            
            if (hasGetConsoleLogs) {
                appendLine("### get_console_logs")
                appendLine("获取页面运行时的控制台日志和 JavaScript 错误。")
                appendLine("参数：")
                appendLine("- filter (可选): 日志过滤类型 - all/error/warn/log")
                appendLine()
            }
            
            if (hasAutoFix) {
                appendLine("### auto_fix")
                appendLine("根据错误信息自动修复代码问题。")
                appendLine("参数：")
                appendLine("- code (必需): 需要修复的原始代码")
                appendLine("- errors (必需): 需要修复的错误描述列表")
                appendLine()
            }
            
            if (hasGenerateImage) {
                appendLine("### generate_image")
                appendLine("使用 AI 生成图像，返回 base64 格式可直接嵌入 HTML。")
                appendLine("参数：")
                appendLine("- prompt (必需): 图像描述")
                appendLine("- style (可选): 风格 - realistic/cartoon/illustration/icon/abstract/minimalist")
                appendLine("- size (可选): 尺寸 - small/medium/large")
                appendLine()
            }
            
            appendLine("## 输出方式")
            appendLine("- 优先使用工具调用（如 write_html）来输出代码")
            appendLine("- 如果无法使用工具调用，可以使用 ```html 代码块输出完整 HTML 代码")
            appendLine("- 不要在普通文本中混入代码片段")
            appendLine()
            
            appendLine("# 回复格式")
            appendLine("使用 Markdown 格式回复：**粗体**、*斜体*、`代码`、列表、> 引用")
            appendLine("只有在用户纯粹聊天或提问（不涉及代码生成）时，才直接文字回复")
            appendLine()
            
            appendLine("# 代码规范")
            appendLine("- 完整的单文件 HTML，CSS/JS 内嵌")
            appendLine("- 必须包含 viewport meta 标签")
            appendLine("- 使用相对单位(vw/vh/%/rem)")
            appendLine("- 代码完整，禁止省略")
            appendLine()
            
            if (!currentHtml.isNullOrBlank()) {
                // 限制 HTML 代码长度，避免超出 API token 限制
                val maxHtmlLength = 8000  // 约 2000-3000 tokens
                val truncatedHtml = if (currentHtml.length > maxHtmlLength) {
                    val truncated = currentHtml.take(maxHtmlLength)
                    "$truncated\n\n... [代码已截断，共 ${currentHtml.length} 字符] ..."
                } else {
                    currentHtml
                }
                appendLine("# 当前代码")
                appendLine("用户已有以下代码，修改时请在此基础上：")
                appendLine("```html")
                appendLine(truncatedHtml)
                appendLine("```")
                appendLine()
            }
            
            config?.rules?.takeIf { it.isNotEmpty() }?.let { rules ->
                appendLine("# 用户自定义规则")
                rules.forEachIndexed { i, r -> appendLine("${i+1}. $r") }
            }
        }.trimEnd()
    }
    
    /**
     * 简单流式模式（回退方案，从文本中提取 HTML）
     * 内部方法，不再嵌套 collect
     */
    private fun developWithSimpleStreamInternal(
        requirement: String,
        currentHtml: String?,
        sessionConfig: SessionConfig?,
        selectedModel: SavedModel,
        apiKey: ApiKeyConfig
    ): Flow<HtmlAgentEvent> = flow {
        val systemPrompt = buildSimpleSystemPrompt(sessionConfig, currentHtml)
        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to requirement)
        )
        
        Log.d(TAG, "Using simple stream mode (fallback)")
        
        val htmlBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()
        val textBuilder = StringBuilder()
        val pendingBuffer = StringBuilder()
        var isCapturingHtml = false
        var htmlStarted = false
        var streamCompleted = false
        
        aiClient.chatStream(
            apiKey = apiKey,
            model = selectedModel.model,
            messages = messages,
            temperature = 0.7f
        ).collect { event ->
            when (event) {
                is com.webtoapp.core.ai.StreamEvent.Started -> {
                    emit(HtmlAgentEvent.StateChange(HtmlAgentState.GENERATING))
                }
                is com.webtoapp.core.ai.StreamEvent.Thinking -> {
                    thinkingBuilder.append(event.content)
                    emit(HtmlAgentEvent.ThinkingDelta(event.content, thinkingBuilder.toString()))
                }
                is com.webtoapp.core.ai.StreamEvent.Content -> {
                    val content = event.delta
                    val accumulated = event.accumulated
                    
                    // 检测 HTML 开始
                    if (!htmlStarted && (accumulated.contains("<!DOCTYPE", ignoreCase = true) || 
                        accumulated.contains("<html", ignoreCase = true))) {
                        htmlStarted = true
                        isCapturingHtml = true
                        // 找到 HTML 开始位置
                        val htmlStart = accumulated.indexOf("<!DOCTYPE", ignoreCase = true)
                            .takeIf { it >= 0 } 
                            ?: accumulated.indexOf("<html", ignoreCase = true)
                        if (htmlStart >= 0) {
                            pendingBuffer.clear()
                            val textBeforeHtml = accumulated.substring(0, htmlStart)
                            if (textBeforeHtml.isNotBlank()) {
                                textBuilder.clear()
                                textBuilder.append(textBeforeHtml)
                            }
                            htmlBuilder.append(accumulated.substring(htmlStart))
                            emit(HtmlAgentEvent.ToolCallStart("write_html", "auto"))
                            emit(HtmlAgentEvent.CodeDelta(accumulated.substring(htmlStart), htmlBuilder.toString()))
                        }
                    } else if (isCapturingHtml) {
                        htmlBuilder.append(content)
                        emit(HtmlAgentEvent.CodeDelta(content, htmlBuilder.toString()))
                    } else {
                        // Check是否可能是 HTML 开始的不完整标签
                        val combinedText = pendingBuffer.toString() + content
                        val potentialHtmlStart = combinedText.lastIndexOf('<')
                        
                        if (potentialHtmlStart >= 0) {
                            val afterLessThan = combinedText.substring(potentialHtmlStart)
                            val couldBeDoctype = "<!DOCTYPE".startsWith(afterLessThan, ignoreCase = true)
                            val couldBeHtml = "<html".startsWith(afterLessThan, ignoreCase = true)
                            
                            if ((couldBeDoctype || couldBeHtml) && afterLessThan.length < 9) {
                                val safeText = combinedText.substring(0, potentialHtmlStart)
                                if (safeText.isNotEmpty()) {
                                    textBuilder.append(safeText)
                                    emit(HtmlAgentEvent.TextDelta(safeText, textBuilder.toString()))
                                }
                                pendingBuffer.clear()
                                pendingBuffer.append(afterLessThan)
                            } else {
                                if (pendingBuffer.isNotEmpty()) {
                                    textBuilder.append(pendingBuffer)
                                    emit(HtmlAgentEvent.TextDelta(pendingBuffer.toString(), textBuilder.toString()))
                                    pendingBuffer.clear()
                                }
                                textBuilder.append(content)
                                emit(HtmlAgentEvent.TextDelta(content, textBuilder.toString()))
                            }
                        } else {
                            if (pendingBuffer.isNotEmpty()) {
                                textBuilder.append(pendingBuffer)
                                emit(HtmlAgentEvent.TextDelta(pendingBuffer.toString(), textBuilder.toString()))
                                pendingBuffer.clear()
                            }
                            textBuilder.append(content)
                            emit(HtmlAgentEvent.TextDelta(content, textBuilder.toString()))
                        }
                    }
                }
                is com.webtoapp.core.ai.StreamEvent.Done -> {
                    streamCompleted = true
                    if (pendingBuffer.isNotEmpty()) {
                        textBuilder.append(pendingBuffer)
                        pendingBuffer.clear()
                    }
                    
                    val finalHtml = htmlBuilder.toString().trim()
                    
                    if (finalHtml.isNotEmpty()) {
                        currentHtmlCode = finalHtml
                        
                        // 写入文件到项目文件夹
                        var fileInfo: ProjectFileInfo? = null
                        val sid = currentSessionId
                        if (sid != null) {
                            val existingFiles = projectFileManager.listFiles(sid)
                            val hasExisting = existingFiles.any { it.getBaseName() == "index" }
                            
                            fileInfo = projectFileManager.createFile(
                                sessionId = sid,
                                filename = "index.html",
                                content = finalHtml,
                                createNewVersion = hasExisting
                            )
                            
                            Log.d(TAG, "SimpleStream: File created: ${fileInfo.name}, version=${fileInfo.version}")
                            emit(HtmlAgentEvent.FileCreated(fileInfo, isNewVersion = hasExisting))
                        }
                        
                        val result = ToolExecutionResult(
                            success = true,
                            toolName = "write_html",
                            result = finalHtml,
                            isHtml = true,
                            fileInfo = fileInfo
                        )
                        emit(HtmlAgentEvent.ToolExecuted(result))
                        emit(HtmlAgentEvent.HtmlComplete(finalHtml))
                    }
                    
                    emit(HtmlAgentEvent.StateChange(HtmlAgentState.COMPLETED))
                    emit(HtmlAgentEvent.Completed(textBuilder.toString(), emptyList()))
                }
                is com.webtoapp.core.ai.StreamEvent.Error -> {
                    throw Exception(event.message)
                }
            }
        }
        
        if (!streamCompleted) {
            throw Exception("流式响应未正常完成")
        }
    }
    
    /**
     * 简单流式模式（公开方法，保持backward compatible）
     */
    private fun developWithSimpleStream(
        requirement: String,
        currentHtml: String?,
        sessionConfig: SessionConfig?,
        selectedModel: SavedModel,
        apiKey: ApiKeyConfig
    ): Flow<HtmlAgentEvent> = flow {
        val systemPrompt = buildSimpleSystemPrompt(sessionConfig, currentHtml)
        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to requirement)
        )
        
        Log.d(TAG, "Using simple stream mode (fallback)")
        
        val htmlBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()
        val textBuilder = StringBuilder()
        val pendingBuffer = StringBuilder()
        var isCapturingHtml = false
        var htmlStarted = false
        
        aiClient.chatStream(
            apiKey = apiKey,
            model = selectedModel.model,
            messages = messages,
            temperature = 0.7f
        ).collect { event ->
            when (event) {
                is com.webtoapp.core.ai.StreamEvent.Started -> {
                    emit(HtmlAgentEvent.StateChange(HtmlAgentState.GENERATING))
                }
                is com.webtoapp.core.ai.StreamEvent.Thinking -> {
                    thinkingBuilder.append(event.content)
                    emit(HtmlAgentEvent.ThinkingDelta(event.content, thinkingBuilder.toString()))
                }
                is com.webtoapp.core.ai.StreamEvent.Content -> {
                    val content = event.delta
                    val accumulated = event.accumulated
                    
                    // 检测 HTML 开始
                    if (!htmlStarted && (accumulated.contains("<!DOCTYPE", ignoreCase = true) || 
                        accumulated.contains("<html", ignoreCase = true))) {
                        htmlStarted = true
                        isCapturingHtml = true
                        // 找到 HTML 开始位置
                        val htmlStart = accumulated.indexOf("<!DOCTYPE", ignoreCase = true)
                            .takeIf { it >= 0 } 
                            ?: accumulated.indexOf("<html", ignoreCase = true)
                        if (htmlStart >= 0) {
                            pendingBuffer.clear()
                            val textBeforeHtml = accumulated.substring(0, htmlStart)
                            if (textBeforeHtml.isNotBlank()) {
                                textBuilder.clear()
                                textBuilder.append(textBeforeHtml)
                            }
                            htmlBuilder.append(accumulated.substring(htmlStart))
                            emit(HtmlAgentEvent.ToolCallStart("write_html", "auto"))
                            emit(HtmlAgentEvent.CodeDelta(accumulated.substring(htmlStart), htmlBuilder.toString()))
                        }
                    } else if (isCapturingHtml) {
                        htmlBuilder.append(content)
                        emit(HtmlAgentEvent.CodeDelta(content, htmlBuilder.toString()))
                    } else {
                        // Check是否可能是 HTML 开始的不完整标签
                        val combinedText = pendingBuffer.toString() + content
                        val potentialHtmlStart = combinedText.lastIndexOf('<')
                        
                        if (potentialHtmlStart >= 0) {
                            val afterLessThan = combinedText.substring(potentialHtmlStart)
                            val couldBeDoctype = "<!DOCTYPE".startsWith(afterLessThan, ignoreCase = true)
                            val couldBeHtml = "<html".startsWith(afterLessThan, ignoreCase = true)
                            
                            if ((couldBeDoctype || couldBeHtml) && afterLessThan.length < 9) {
                                val safeText = combinedText.substring(0, potentialHtmlStart)
                                if (safeText.isNotEmpty()) {
                                    textBuilder.append(safeText)
                                    emit(HtmlAgentEvent.TextDelta(safeText, textBuilder.toString()))
                                }
                                pendingBuffer.clear()
                                pendingBuffer.append(afterLessThan)
                            } else {
                                if (pendingBuffer.isNotEmpty()) {
                                    textBuilder.append(pendingBuffer)
                                    emit(HtmlAgentEvent.TextDelta(pendingBuffer.toString(), textBuilder.toString()))
                                    pendingBuffer.clear()
                                }
                                textBuilder.append(content)
                                emit(HtmlAgentEvent.TextDelta(content, textBuilder.toString()))
                            }
                        } else {
                            if (pendingBuffer.isNotEmpty()) {
                                textBuilder.append(pendingBuffer)
                                emit(HtmlAgentEvent.TextDelta(pendingBuffer.toString(), textBuilder.toString()))
                                pendingBuffer.clear()
                            }
                            textBuilder.append(content)
                            emit(HtmlAgentEvent.TextDelta(content, textBuilder.toString()))
                        }
                    }
                }
                is com.webtoapp.core.ai.StreamEvent.Done -> {
                    if (pendingBuffer.isNotEmpty()) {
                        textBuilder.append(pendingBuffer)
                        pendingBuffer.clear()
                    }
                    
                    val finalHtml = htmlBuilder.toString().trim()
                    
                    if (finalHtml.isNotEmpty()) {
                        currentHtmlCode = finalHtml
                        
                        // 写入文件到项目文件夹（与工具调用模式保持一致）
                        var fileInfo: ProjectFileInfo? = null
                        val sid = currentSessionId
                        if (sid != null) {
                            val existingFiles = projectFileManager.listFiles(sid)
                            val hasExisting = existingFiles.any { it.getBaseName() == "index" }
                            
                            fileInfo = projectFileManager.createFile(
                                sessionId = sid,
                                filename = "index.html",
                                content = finalHtml,
                                createNewVersion = hasExisting
                            )
                            
                            Log.d(TAG, "SimpleStream: File created: ${fileInfo.name}, version=${fileInfo.version}")
                            
                            // 发送文件创建事件
                            emit(HtmlAgentEvent.FileCreated(fileInfo, isNewVersion = hasExisting))
                        }
                        
                        val result = ToolExecutionResult(
                            success = true,
                            toolName = "write_html",
                            result = finalHtml,
                            isHtml = true,
                            fileInfo = fileInfo
                        )
                        emit(HtmlAgentEvent.ToolExecuted(result))
                        emit(HtmlAgentEvent.HtmlComplete(finalHtml))
                    }
                    
                    emit(HtmlAgentEvent.StateChange(HtmlAgentState.COMPLETED))
                    emit(HtmlAgentEvent.Completed(textBuilder.toString(), emptyList()))
                }
                is com.webtoapp.core.ai.StreamEvent.Error -> {
                    emit(HtmlAgentEvent.Error(event.message))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    
    /**
     * 构建简化的系统提示词（回退模式，从文本中提取 HTML）
     */
    private fun buildSimpleSystemPrompt(config: SessionConfig?, currentHtml: String?): String {
        return buildString {
            appendLine("你是移动端前端开发专家，为手机APP WebView创建HTML页面。")
            appendLine()
            
            appendLine("# 回复规则")
            appendLine("1. 使用 Markdown 格式回复（**粗体**、*斜体*、`代码`、列表等）")
            appendLine("2. 回复简洁专业")
            appendLine()
            
            appendLine("# 代码输出规则")
            appendLine("当用户要求创建/修改网页时：")
            appendLine("1. 直接输出完整 HTML 代码，以 `<!DOCTYPE html>` 开头")
            appendLine("2. 禁止使用 markdown 代码块包裹（如 \\`\\`\\`html）")
            appendLine("3. 代码前后可以有简短说明")
            appendLine()
            
            appendLine("# 代码规范")
            appendLine("- 单个完整HTML文件，CSS/JS内嵌")
            appendLine("- 必须包含 viewport meta 标签")
            appendLine("- 使用相对单位(vw/vh/%/rem)")
            appendLine("- 代码完整，禁止省略")
            appendLine()
            
            if (!currentHtml.isNullOrBlank()) {
                // 限制 HTML 代码长度，避免超出 API token 限制
                val maxHtmlLength = 8000  // 约 2000-3000 tokens
                val truncatedHtml = if (currentHtml.length > maxHtmlLength) {
                    val truncated = currentHtml.take(maxHtmlLength)
                    "$truncated\n\n... [代码已截断，共 ${currentHtml.length} 字符] ..."
                } else {
                    currentHtml
                }
                appendLine("# 当前代码")
                appendLine("用户已有以下代码，修改时请在此基础上：")
                appendLine(truncatedHtml)
                appendLine()
            }
            
            config?.rules?.takeIf { it.isNotEmpty() }?.let { rules ->
                appendLine("# 用户自定义规则")
                rules.forEachIndexed { i, r -> appendLine("${i+1}. $r") }
            }
        }.trimEnd()
    }

    
    /**
     * 构建系统提示词（保留用于其他功能）
     */
    private fun buildSystemPrompt(config: SessionConfig?): String {
        val enabledTools = getEnabledTools(config)
        val hasWriteHtml = enabledTools.any { it.name == "write_html" }
        val hasEditHtml = enabledTools.any { it.name == "edit_html" }
        val hasGenerateImage = enabledTools.any { it.name == "generate_image" }
        val hasDebugTools = enabledTools.any { it.name == "get_console_logs" }
        
        return buildString {
            // 角色定义
            appendLine("你是移动端前端开发专家，为手机APP WebView创建HTML页面。")
            appendLine("你友好、专业，能够回答用户的问题并帮助他们创建网页。")
            appendLine()
            
            // 对话规则
            appendLine("# 对话规则")
            appendLine("1. 当用户打招呼或闲聊时，正常回复，不需要调用工具")
            appendLine("2. 当用户询问技术问题时，直接用文字回答")
            appendLine("3. 只有当用户明确要求创建、修改或生成网页/代码时，才调用工具")
            appendLine()
            
            // 工具使用规则
            appendLine("# 工具使用规则")
            appendLine("当需要生成代码时：")
            appendLine("1. 先调用 write_html 工具（无需参数）")
            appendLine("2. 然后直接输出完整的 HTML 代码")
            appendLine("3. 禁止使用 markdown 代码块（如 ```html）")
            appendLine("4. 禁止在代码前后添加任何解释文字")
            appendLine()
            
            // 代码规则
            appendLine("# 代码规则")
            appendLine("1. 输出单个完整HTML文件，CSS和JS内嵌在<style>和<script>标签中")
            appendLine("2. 必须包含: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
            appendLine("3. 使用相对单位(vw/vh/%/rem)，禁止固定像素宽度如width:375px")
            appendLine("4. 可点击元素最小44x44px")
            appendLine("5. 代码必须完整，禁止用...或注释省略")
            appendLine()
            
            // 工具说明
            if (hasWriteHtml) {
                appendLine("# write_html 工具使用示例")
                appendLine("调用 write_html 工具，然后直接输出 HTML 代码：")
                appendLine("<!DOCTYPE html>")
                appendLine("<html>...")
                appendLine()
            }
            
            if (hasEditHtml) {
                appendLine("# edit_html 工具")
                appendLine("用于小范围修改。operation: replace/insert_before/insert_after/delete")
                appendLine()
            }
            
            if (hasGenerateImage) {
                appendLine("# generate_image 工具")
                appendLine("生成图像。参数：prompt, style, size")
                appendLine()
            }
            
            // User规则
            config?.rules?.takeIf { it.isNotEmpty() }?.let { rules ->
                appendLine("# 用户规则")
                rules.forEachIndexed { i, r -> appendLine("${i+1}. $r") }
            }
        }.trimEnd()
    }
    
    /**
     * 构建消息列表
     */
    private fun buildMessages(
        systemPrompt: String,
        requirement: String,
        currentHtml: String?
    ): List<Map<String, String>> {
        val messages = mutableListOf<Map<String, String>>()
        
        messages.add(mapOf("role" to "system", "content" to systemPrompt))
        
        val userMessage = buildString {
            append(requirement)
            if (!currentHtml.isNullOrBlank()) {
                append("\n\n当前页面代码：\n```html\n$currentHtml\n```\n请在此基础上修改。")
            }
        }
        
        messages.add(mapOf("role" to "user", "content" to userMessage))
        
        return messages
    }
    
    /**
     * 选择模型
     */
    private suspend fun selectModel(
        preferredModel: SavedModel?,
        savedModels: List<SavedModel>
    ): SavedModel? {
        if (preferredModel != null) return preferredModel
        
        val htmlCodingModels = savedModels.filter { it.supportsFeature(AiFeature.HTML_CODING) }
        val defaultModelId = aiConfigManager.defaultModelIdFlow.first()
        
        return htmlCodingModels.find { it.id == defaultModelId }
            ?: htmlCodingModels.firstOrNull()
            ?: savedModels.find { it.id == defaultModelId }
            ?: savedModels.firstOrNull()
    }
}


// ==================== 数据类 ====================

/**
 * HTML 工具定义
 */
data class HtmlTool(
    val type: HtmlToolType,
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
) {
    fun toOpenAiFormat(): Map<String, Any> = mapOf(
        "type" to "function",
        "function" to mapOf(
            "name" to name,
            "description" to description,
            "parameters" to parameters
        )
    )
}

/**
 * 控制台日志级别
 */
enum class ConsoleLogLevel {
    LOG, WARN, ERROR, INFO, DEBUG
}

/**
 * 控制台日志条目
 */
data class ConsoleLogEntry(
    val level: ConsoleLogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String? = null,
    val lineNumber: Int? = null
)

/**
 * 错误严重程度
 */
enum class ErrorSeverity {
    ERROR, WARNING, INFO
}

/**
 * 语法错误
 */
data class SyntaxError(
    val type: String,           // html, css, javascript
    val message: String,
    val line: Int,
    val column: Int,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * 工具执行结果
 */
data class ToolExecutionResult(
    val success: Boolean,
    val toolName: String,
    val result: String,
    val isHtml: Boolean = false,
    val isEdit: Boolean = false,  // Yes否为编辑操作（区分 write_html 和 edit_html）
    val isImageGeneration: Boolean = false,  // Yes否为图像生成操作
    val imageData: String? = null,  // Generate的图像 base64 数据
    val syntaxErrors: List<SyntaxError> = emptyList(),
    val fileInfo: ProjectFileInfo? = null  // Create/修改的文件信息
)

/**
 * Agent 状态
 */
enum class HtmlAgentState {
    IDLE,
    GENERATING,
    COMPLETED,
    ERROR
}

/**
 * Agent 事件 - 支持流式输出
 */
sealed class HtmlAgentEvent {
    // 状态变化
    data class StateChange(val state: HtmlAgentState) : HtmlAgentEvent()
    
    // File创建事件
    data class FileCreated(
        val fileInfo: ProjectFileInfo,
        val isNewVersion: Boolean
    ) : HtmlAgentEvent()
    
    // 文本内容增量
    data class TextDelta(val delta: String, val accumulated: String) : HtmlAgentEvent()
    
    // 思考内容增量
    data class ThinkingDelta(val delta: String, val accumulated: String) : HtmlAgentEvent()
    
    // 工具调用开始
    data class ToolCallStart(val toolName: String, val toolCallId: String) : HtmlAgentEvent()
    
    // 代码增量（实时显示生成的 HTML）
    data class CodeDelta(val delta: String, val accumulated: String) : HtmlAgentEvent()
    
    // 工具执行完成
    data class ToolExecuted(val result: ToolExecutionResult) : HtmlAgentEvent()
    
    // 图像生成开始
    data class ImageGenerating(val prompt: String) : HtmlAgentEvent()
    
    // 图像生成完成
    data class ImageGenerated(val imageData: String, val prompt: String) : HtmlAgentEvent()
    
    // HTML 生成完成
    data class HtmlComplete(val html: String) : HtmlAgentEvent()
    
    // Auto预览触发
    data class AutoPreview(val html: String) : HtmlAgentEvent()
    
    // All完成
    data class Completed(val textContent: String, val toolCalls: List<ToolCallInfo>) : HtmlAgentEvent()
    
    // Error
    data class Error(val message: String) : HtmlAgentEvent()
}
