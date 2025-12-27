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
        
        // 检查未闭合的标签
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
                        // 检查是否已存在同名文件（决定是否创建新版本）
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
                    
                    currentHtmlCode = newHtml  // 更新当前 HTML
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
            
            // 构建增强的提示词
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
     */
    fun developWithStream(
        requirement: String,
        currentHtml: String? = null,
        sessionConfig: SessionConfig? = null,
        model: SavedModel? = null,
        sessionId: String? = null  // 会话ID，用于文件操作
    ): Flow<HtmlAgentEvent> = flow {
        try {
            Log.d(TAG, "developWithStream started with requirement: ${requirement.take(100)}, sessionId: $sessionId")
            
            // 设置当前会话ID
            if (sessionId != null) {
                currentSessionId = sessionId
            }
            
            // 获取 AI 配置
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            if (apiKeys.isEmpty()) {
                emit(HtmlAgentEvent.Error("请先在 AI 设置中配置 API Key"))
                return@flow
            }
            
            // 选择模型
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
            
            // 设置当前 HTML（从最新版本文件读取）
            if (sessionId != null && currentHtml.isNullOrBlank()) {
                val latestFile = projectFileManager.getLatestVersion(sessionId, "index")
                if (latestFile != null) {
                    currentHtmlCode = projectFileManager.readFile(sessionId, latestFile.name) ?: ""
                }
            } else if (!currentHtml.isNullOrBlank()) {
                currentHtmlCode = currentHtml
            }
            
            // 构建系统提示词
            val systemPrompt = buildToolCallingSystemPrompt(sessionConfig, currentHtmlCode.takeIf { it.isNotBlank() })
            val messages = listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to requirement)
            )
            
            // 构建工具定义 - 支持两种方式：代码在参数中或在文本流中
            val tools = listOf(
                mapOf(
                    "type" to "function",
                    "function" to mapOf(
                        "name" to "write_html",
                        "description" to "创建 HTML 文件并写入代码。将完整的 HTML 代码作为 html 参数传入。",
                        "parameters" to mapOf(
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
                    )
                )
            )
            
            Log.d(TAG, "Using tool calling mode with write_html tool")
            
            val htmlBuilder = StringBuilder()
            val thinkingBuilder = StringBuilder()
            val textBuilder = StringBuilder()
            var toolCallStarted = false
            var isCapturingCodeFromText = false  // 从文本流中捕获代码
            var isCapturingCodeFromArgs = false  // 从工具参数中捕获代码
            var codeBlockStarted = false
            
            // 尝试使用工具调用模式
            try {
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
                            
                            // 检测代码块开始 ```html（备选方案：AI 在文本中输出代码）
                            if (!codeBlockStarted && accumulated.contains("```html")) {
                                codeBlockStarted = true
                                isCapturingCodeFromText = true
                                if (!toolCallStarted) {
                                    toolCallStarted = true
                                    emit(HtmlAgentEvent.ToolCallStart("write_html", "text-stream"))
                                }
                                
                                // 提取代码块开始前的文本
                                val codeBlockIndex = accumulated.indexOf("```html")
                                val textBefore = accumulated.substring(0, codeBlockIndex)
                                if (textBefore.isNotBlank()) {
                                    textBuilder.append(textBefore)
                                }
                                
                                // 提取代码块开始后的内容
                                val codeStart = codeBlockIndex + 7
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
                            } else if (isCapturingCodeFromText) {
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
                            if (event.toolName == "write_html" && !toolCallStarted) {
                                toolCallStarted = true
                                isCapturingCodeFromArgs = true
                                htmlBuilder.clear()
                                emit(HtmlAgentEvent.ToolCallStart(event.toolName, event.toolCallId))
                            }
                        }
                        is ToolStreamEvent.ToolArgumentsDelta -> {
                            // 从工具参数中提取 HTML 代码（主要方案）
                            if (isCapturingCodeFromArgs) {
                                val args = event.accumulated
                                // 尝试从 JSON 参数中提取 HTML
                                val htmlContent = extractHtmlFromArgs(args)
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
                            if (event.toolName == "write_html") {
                                // 最终提取完整的 HTML
                                val finalHtml = extractHtmlFromArgs(event.arguments)
                                if (finalHtml.isNotEmpty()) {
                                    htmlBuilder.clear()
                                    htmlBuilder.append(finalHtml)
                                    emit(HtmlAgentEvent.CodeDelta("", htmlBuilder.toString()))
                                }
                                isCapturingCodeFromArgs = false
                            }
                        }
                        is ToolStreamEvent.Done -> {
                            val finalHtml = htmlBuilder.toString().trim()
                            Log.d(TAG, "Stream done, finalHtml length: ${finalHtml.length}")
                            
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
                        is ToolStreamEvent.Error -> {
                            Log.w(TAG, "Tool calling failed: ${event.message}, falling back to simple mode")
                            throw Exception(event.message)
                        }
                    }
                }
            } catch (e: Exception) {
                // 回退到简单流式模式
                Log.w(TAG, "Falling back to simple stream mode: ${e.message}")
                developWithSimpleStream(requirement, currentHtmlCode.takeIf { it.isNotBlank() }, sessionConfig, selectedModel, apiKey).collect { event ->
                    emit(event)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in developWithStream", e)
            emit(HtmlAgentEvent.Error("发生错误: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 从工具参数中提取 HTML 代码
     * 支持多种格式：JSON 对象、直接 HTML、带转义的字符串
     */
    private fun extractHtmlFromArgs(args: String): String {
        if (args.isBlank()) return ""
        
        val trimmed = args.trim()
        
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
                return json.asString
            }
        } catch (e: Exception) {
            // JSON 解析失败，尝试其他方式
        }
        
        // 2. 尝试从不完整的 JSON 中提取（流式传输时 JSON 可能不完整）
        // 查找 "html": " 或 "content": " 或 "code": " 后的内容
        val patterns = listOf(
            "\"html\"\\s*:\\s*\"" to "\"",
            "\"content\"\\s*:\\s*\"" to "\"",
            "\"code\"\\s*:\\s*\"" to "\""
        )
        
        for ((startPattern, _) in patterns) {
            val regex = Regex(startPattern)
            val match = regex.find(trimmed)
            if (match != null) {
                val startIndex = match.range.last + 1
                // 找到对应的结束引号（考虑转义）
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
                
                if (endIndex > startIndex) {
                    val extracted = trimmed.substring(startIndex, endIndex)
                    // 解码转义字符
                    val decoded = extracted
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                    if (decoded.isNotBlank()) {
                        return decoded
                    }
                }
            }
        }
        
        // 3. 如果以 <!DOCTYPE 或 <html 开头，直接作为 HTML
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || 
            trimmed.startsWith("<html", ignoreCase = true) ||
            (trimmed.startsWith("<") && trimmed.contains("</html>", ignoreCase = true))) {
            return trimmed
        }
        
        return ""
    }
    
    /**
     * 构建工具调用模式的系统提示词
     */
    private fun buildToolCallingSystemPrompt(config: SessionConfig?, currentHtml: String?): String {
        return buildString {
            appendLine("你是移动端前端开发专家，为手机APP WebView创建HTML页面。")
            appendLine()
            
            appendLine("# 【强制规则】工具使用")
            appendLine("你必须且只能通过 write_html 工具来提交代码。这是强制要求！")
            appendLine()
            appendLine("## 必须使用工具的情况：")
            appendLine("- 创建新网页")
            appendLine("- 修改现有代码")
            appendLine("- 生成任何 HTML/CSS/JavaScript 代码")
            appendLine()
            appendLine("## 工具调用方式：")
            appendLine("调用 write_html 工具，将完整的 HTML 代码作为 html 参数传入。")
            appendLine()
            appendLine("## 禁止行为：")
            appendLine("- 禁止在回复文本中直接输出代码")
            appendLine("- 禁止使用 ```html 代码块展示代码")
            appendLine("- 禁止说\"这是代码\"然后粘贴代码")
            appendLine()
            appendLine("## 正确示例：")
            appendLine("用户：\"创建一个红色按钮\"")
            appendLine("你应该：调用 write_html 工具，html 参数包含完整的 HTML 代码")
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
                        // 检查是否可能是 HTML 开始的不完整标签
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
            
            // 用户规则
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
    val isEdit: Boolean = false,  // 是否为编辑操作（区分 write_html 和 edit_html）
    val isImageGeneration: Boolean = false,  // 是否为图像生成操作
    val imageData: String? = null,  // 生成的图像 base64 数据
    val syntaxErrors: List<SyntaxError> = emptyList(),
    val fileInfo: ProjectFileInfo? = null  // 创建/修改的文件信息
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
    
    // 文件创建事件
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
    
    // 自动预览触发
    data class AutoPreview(val html: String) : HtmlAgentEvent()
    
    // 全部完成
    data class Completed(val textContent: String, val toolCalls: List<ToolCallInfo>) : HtmlAgentEvent()
    
    // 错误
    data class Error(val message: String) : HtmlAgentEvent()
}
