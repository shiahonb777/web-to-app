package com.webtoapp.core.ai.coding

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.google.gson.JsonParser
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.ToolStreamEvent
import com.webtoapp.core.ai.ToolCallInfo
import com.webtoapp.core.i18n.Strings
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
class AiCodingAgent(private val context: Context) {
    
    private val gson = com.webtoapp.util.GsonProvider.gson
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
        private const val TAG = "AiCodingAgent"
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
            type = AiCodingToolType.WRITE_HTML,
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
            type = AiCodingToolType.EDIT_HTML,
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
            type = AiCodingToolType.GET_CONSOLE_LOGS,
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
            type = AiCodingToolType.CHECK_SYNTAX,
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
            type = AiCodingToolType.READ_CURRENT_CODE,
            name = "read_current_code",
            description = "读取当前 HTML 代码的完整内容。在修改代码前调用此工具，了解现有代码结构和内容，确保编辑操作准确。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "include_line_numbers" to mapOf(
                        "type" to "boolean",
                        "description" to "是否在输出中包含行号，便于定位代码位置。默认为 true"
                    )
                )
            )
        ),
        HtmlTool(
            type = AiCodingToolType.GENERATE_IMAGE,
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
        val enabledTypes = config?.enabledTools ?: setOf(AiCodingToolType.WRITE_HTML)
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
                                    message = Strings.tagNotProperlyClosed.replace("%s", tag),
                                    line = ln + 1,
                                    column = 0,
                                    severity = ErrorSeverity.ERROR
                                ))
                            }
                            tagStack.removeAll { it.first in unclosed.map { u -> u.first } || it.first == tagName }
                        } else {
                            errors.add(SyntaxError(
                                type = "html",
                                message = Strings.unexpectedClosingTag.replace("%s", tagName),
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
                message = Strings.tagNotClosed.replace("%s", tag),
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
                                    message = Strings.extraClosingBrace,
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
                message = Strings.missingClosingBraces.replace("%d", braceCount.toString()),
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
                message = if (braceCount > 0) Strings.missingClosingBraces.replace("%d", braceCount.toString()) else Strings.extraClosingBraces.replace("%d", (-braceCount).toString()),
                line = lines.size,
                column = 0,
                severity = ErrorSeverity.ERROR
            ))
        }
        
        if (parenCount != 0) {
            errors.add(SyntaxError(
                type = "javascript",
                message = if (parenCount > 0) Strings.missingClosingParens.replace("%d", parenCount.toString()) else Strings.extraClosingParens.replace("%d", (-parenCount).toString()),
                line = lines.size,
                column = 0,
                severity = ErrorSeverity.ERROR
            ))
        }
        
        if (bracketCount != 0) {
            errors.add(SyntaxError(
                type = "javascript",
                message = if (bracketCount > 0) Strings.missingClosingBrackets.replace("%d", bracketCount.toString()) else Strings.extraClosingBrackets.replace("%d", (-bracketCount).toString()),
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
            AppLogger.d(TAG, "Not a valid JSON, trying as raw HTML: ${e.message}")
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
        AppLogger.d(TAG, "executeToolCall: $toolName, args length: ${arguments.length}, args preview: ${arguments.take(200)}")
        
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
                        AppLogger.e(TAG, "write_html: content is blank, raw args: $arguments")
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
                        
                        AppLogger.d(TAG, "write_html: file created at ${fileInfo.path}, version=${fileInfo.version}")
                        
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
                        AppLogger.d(TAG, "write_html: no sessionId, only updating memory")
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
                        // 尝试空白字符归一化匹配（fuzzy matching）
                        val normalizedTarget = target.replace(Regex("\\s+"), " ").trim()
                        val normalizedCode = currentHtmlCode.replace(Regex("\\s+"), " ").trim()
                        if (!normalizedCode.contains(normalizedTarget)) {
                            // 尝试行级匹配：提取 target 的关键行，在代码中寻找最佳匹配区域
                            val targetLines = target.lines().filter { it.isNotBlank() }
                            val codeLines = currentHtmlCode.lines()
                            var bestMatchStart = -1
                            var bestMatchEnd = -1
                            var bestScore = 0
                            
                            if (targetLines.isNotEmpty()) {
                                val firstTargetLine = targetLines.first().trim()
                                val lastTargetLine = targetLines.last().trim()
                                
                                for (i in codeLines.indices) {
                                    if (codeLines[i].trim() == firstTargetLine) {
                                        // 找到首行匹配，向下搜索尾行
                                        for (j in i until minOf(i + targetLines.size + 5, codeLines.size)) {
                                            if (codeLines[j].trim() == lastTargetLine) {
                                                val matchedCount = targetLines.count { tl ->
                                                    codeLines.subList(i, j + 1).any { cl -> cl.trim() == tl.trim() }
                                                }
                                                if (matchedCount > bestScore) {
                                                    bestScore = matchedCount
                                                    bestMatchStart = i
                                                    bestMatchEnd = j
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (bestMatchStart >= 0 && bestScore >= targetLines.size * 0.7) {
                                // 使用模糊匹配找到的区域进行替换
                                val matchedRegion = codeLines.subList(bestMatchStart, bestMatchEnd + 1).joinToString("\n")
                                AppLogger.d(TAG, "edit_html: fuzzy match found at lines $bestMatchStart-$bestMatchEnd (score: $bestScore/${targetLines.size})")
                                val newHtml = when (operation) {
                                    "replace" -> currentHtmlCode.replace(matchedRegion, content)
                                    "insert_before" -> currentHtmlCode.replace(matchedRegion, content + "\n" + matchedRegion)
                                    "insert_after" -> currentHtmlCode.replace(matchedRegion, matchedRegion + "\n" + content)
                                    "delete" -> currentHtmlCode.replace(matchedRegion, "")
                                    else -> return ToolExecutionResult(success = false, toolName = toolName, result = "未知操作: $operation")
                                }
                                currentHtmlCode = newHtml
                                return ToolExecutionResult(
                                    success = true,
                                    toolName = toolName,
                                    result = newHtml,
                                    isHtml = true,
                                    isEdit = true
                                )
                            }
                            
                            // 提供带行号的当前代码帮助AI定位
                            val codeWithLines = currentHtmlCode.lines().mapIndexed { idx, line -> 
                                "${idx + 1}: $line" 
                            }.joinToString("\n")
                            val preview = if (codeWithLines.length > 2000) codeWithLines.take(2000) + "\n..." else codeWithLines
                            return ToolExecutionResult(
                                success = false,
                                toolName = toolName,
                                result = "错误：在当前代码中找不到目标片段。请先调用 read_current_code 查看现有代码，确保 target 精确匹配。\n\n当前代码预览：\n$preview"
                            )
                        }
                        // 空白归一化匹配成功，使用归一化后的替换
                        AppLogger.d(TAG, "edit_html: using whitespace-normalized matching")
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
                "read_current_code" -> {
                    if (currentHtmlCode.isBlank()) {
                        ToolExecutionResult(
                            success = true,
                            toolName = toolName,
                            result = "当前没有 HTML 代码。请使用 write_html 创建新页面。"
                        )
                    } else {
                        val includeLineNumbers = try {
                            val json = JsonParser.parseString(arguments).asJsonObject
                            json.get("include_line_numbers")?.asBoolean ?: true
                        } catch (e: Exception) { true }
                        
                        val codeOutput = if (includeLineNumbers) {
                            currentHtmlCode.lines().mapIndexed { idx, line ->
                                "${idx + 1}| $line"
                            }.joinToString("\n")
                        } else {
                            currentHtmlCode
                        }
                        
                        ToolExecutionResult(
                            success = true,
                            toolName = toolName,
                            result = "当前 HTML 代码（共 ${currentHtmlCode.lines().size} 行，${currentHtmlCode.length} 字符）：\n\n$codeOutput"
                        )
                    }
                }
                "auto_fix" -> {
                    // auto_fix 保留向后兼容，实际引导 AI 使用 check_syntax + write_html
                    val json = JsonParser.parseString(arguments).asJsonObject
                    val code = json.get("code")?.asString ?: currentHtmlCode
                    val errors = checkSyntax(code)
                    if (errors.isEmpty()) {
                        ToolExecutionResult(
                            success = true,
                            toolName = toolName,
                            result = "语法检查通过，未发现需要修复的错误。"
                        )
                    } else {
                        ToolExecutionResult(
                            success = true,
                            toolName = toolName,
                            result = "发现 ${errors.size} 个问题：\n" + errors.joinToString("\n") { 
                                "- [${it.severity.name}] 第${it.line}行: ${it.message}" 
                            } + "\n\n请使用 write_html 或 edit_html 修复这些问题。"
                        )
                    }
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
            AppLogger.e(TAG, "Tool execution failed: $toolName", e)
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
            AppLogger.e(TAG, "Image generation failed", e)
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
    
    // ==================== 项目文件写入辅助 ====================
    
    /**
     * 将 HTML 代码写入项目文件夹并发出相关事件
     * 统一的文件写入逻辑，减少代码重复
     * 
     * @return Pair<ToolExecutionResult, ProjectFileInfo?> 工具执行结果和文件信息
     */
    private suspend fun FlowCollector<HtmlAgentEvent>.writeHtmlToProject(
        html: String,
        filename: String = "index.html"
    ): Pair<ToolExecutionResult, ProjectFileInfo?> {
        currentHtmlCode = html
        
        var fileInfo: ProjectFileInfo? = null
        val sid = currentSessionId
        if (sid != null) {
            val baseName = filename.substringBeforeLast(".")
            val existingFiles = projectFileManager.listFiles(sid)
            val hasExisting = existingFiles.any { it.getBaseName() == baseName }
            
            fileInfo = projectFileManager.createFile(
                sessionId = sid,
                filename = filename,
                content = html,
                createNewVersion = hasExisting
            )
            
            AppLogger.d(TAG, "writeHtmlToProject: ${fileInfo.name}, version=${fileInfo.version}")
            emit(HtmlAgentEvent.FileCreated(fileInfo, isNewVersion = hasExisting))
        }
        
        val result = ToolExecutionResult(
            success = true,
            toolName = "write_html",
            result = html,
            isHtml = true,
            fileInfo = fileInfo
        )
        emit(HtmlAgentEvent.ToolExecuted(result))
        emit(HtmlAgentEvent.HtmlComplete(html))
        
        return Pair(result, fileInfo)
    }
    
    // ==================== 流式开发 ====================
    
    /**
     * 流式开发 HTML
     * 文件直接写入项目文件夹，支持版本迭代
     * 支持两种模式：工具调用模式（ReAct 多轮）和简单模式（文本提取）
     */
    fun developWithStream(
        requirement: String,
        currentHtml: String? = null,
        sessionConfig: SessionConfig? = null,
        model: SavedModel? = null,
        sessionId: String? = null  // SessionID，用于文件操作
    ): Flow<HtmlAgentEvent> = flow {
        try {
            AppLogger.d(TAG, "developWithStream started with requirement: ${requirement.take(100)}, sessionId: $sessionId")
            
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
            
            AppLogger.d(TAG, "Using model: ${selectedModel.model.id}")
            
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
            // 大多数现代 LLM 通过 OpenAI 兼容 API 支持工具调用
            // 只有明确已知不支持的模型才跳过
            val modelId = selectedModel.model.id.lowercase()
            val providerName = apiKey.provider.name.lowercase()
            val baseUrl = (apiKey.baseUrl ?: "").lowercase()
            
            // 已知不支持 OpenAI 工具调用格式的模型
            val knownNoToolCalling = setOf(
                "deepseek-coder", "deepseek-coder-v2",        // DeepSeek Coder 系列
                "yi-coder",                                     // Yi Coder
                "codestral",                                    // Mistral Codestral
            )
            val supportsToolCalling = knownNoToolCalling.none { modelId.contains(it) }
            
            AppLogger.d(TAG, "Model: $modelId, Provider: $providerName, BaseUrl: $baseUrl, SupportsToolCalling: $supportsToolCalling")
            
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
                    AppLogger.w(TAG, "Tool calling timeout, falling back to simple mode")
                    useSimpleMode = true
                    toolCallError = "请求超时"
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Tool calling failed: ${e.message}, falling back to simple mode")
                    useSimpleMode = true
                    toolCallError = e.message
                }
            } else {
                AppLogger.d(TAG, "Skipping tool calling mode (not supported), using simple mode directly")
            }
            
            // 回退到简单模式
            if (useSimpleMode) {
                AppLogger.d(TAG, "Using simple stream mode as fallback")
                try {
                    withTimeout(STREAM_TIMEOUT_MS) {
                        developWithSimpleStreamInternal(requirement, currentHtmlCode.takeIf { it.isNotBlank() }, sessionConfig, selectedModel, apiKey)
                            .collect { event -> emit(event) }
                    }
                } catch (e: TimeoutCancellationException) {
                    emit(HtmlAgentEvent.Error(Strings.requestTimeoutRetry))
                } catch (e: Exception) {
                    emit(HtmlAgentEvent.Error(Strings.errorOccurredPrefix.replace("%s", e.message ?: toolCallError ?: Strings.unknownError)))
                }
            }
            
        } catch (e: CancellationException) {
            throw e  // 重新抛出取消异常
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error in developWithStream", e)
            emit(HtmlAgentEvent.Error(Strings.errorOccurredPrefix.replace("%s", e.message ?: Strings.unknownError)))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 使用工具调用模式开发 HTML（内部方法）
     * 
     * 实现真正的 ReAct（Reasoning-Action-Observation）多轮循环：
     * 1. AI 接收用户需求，决定使用哪些工具
     * 2. 执行工具调用，将结果作为 observation 反馈给 AI
     * 3. AI 根据 observation 决定下一步：继续调用工具或输出最终结果
     * 4. 最多执行 MAX_REACT_TURNS 轮，防止无限循环
     */
    private fun developWithToolCalling(
        requirement: String,
        sessionConfig: SessionConfig?,
        selectedModel: SavedModel,
        apiKey: ApiKeyConfig
    ): Flow<HtmlAgentEvent> = flow {
        val MAX_REACT_TURNS = 5  // 最大 ReAct 迭代次数
        
        // Get启用的工具
        val enabledTools = getEnabledTools(sessionConfig)
        
        // Build系统提示词
        val systemPrompt = buildToolCallingSystemPrompt(sessionConfig, currentHtmlCode.takeIf { it.isNotBlank() }, enabledTools)
        
        // 将启用的工具转换为 OpenAI 格式
        val tools = enabledTools.map { it.toOpenAiFormat() }
        
        AppLogger.d(TAG, "ReAct loop: ${enabledTools.size} tools: ${enabledTools.map { it.name }}")
        
        // 对话消息列表（支持多轮）
        val conversationMessages = mutableListOf<Map<String, Any>>(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to requirement)
        )
        
        // ReAct 多轮循环
        var turnCount = 0
        var reachedFinalResponse = false
        val allToolCalls = mutableListOf<ToolCallInfo>()
        
        while (turnCount < MAX_REACT_TURNS && !reachedFinalResponse) {
            turnCount++
            AppLogger.d(TAG, "ReAct turn $turnCount/$MAX_REACT_TURNS")
            
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
            val pendingToolCalls = mutableListOf<Triple<String, String, String>>()
            
            aiClient.chatStreamWithTools(
                apiKey = apiKey,
                model = selectedModel.model,
                messages = conversationMessages,
                tools = tools,
                temperature = sessionConfig?.temperature ?: 0.7f
            ).collect { event ->
                when (event) {
                    is ToolStreamEvent.Started -> {
                        if (turnCount == 1) {
                            emit(HtmlAgentEvent.StateChange(HtmlAgentState.GENERATING))
                        }
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
                        
                        // 检测代码块 ```html 或直接 HTML 内容（备选方案）
                        if (!codeBlockStarted) {
                            val htmlCodeBlockIndex = accumulated.indexOf("```html", ignoreCase = true)
                            val genericCodeBlockIndex = if (htmlCodeBlockIndex < 0) {
                                val idx = accumulated.indexOf("```")
                                if (idx >= 0) {
                                    val afterBlock = accumulated.substring(idx + 3).trimStart()
                                    if (afterBlock.startsWith("<!DOCTYPE", ignoreCase = true) || 
                                        afterBlock.startsWith("<html", ignoreCase = true) ||
                                        afterBlock.startsWith("<head", ignoreCase = true) ||
                                        afterBlock.startsWith("<body", ignoreCase = true)) idx else -1
                                } else -1
                            } else -1
                            
                            val codeBlockIndex = if (htmlCodeBlockIndex >= 0) htmlCodeBlockIndex else genericCodeBlockIndex
                            val codeBlockMarkerLength = if (htmlCodeBlockIndex >= 0) 7 else 3
                            
                            if (codeBlockIndex >= 0) {
                                codeBlockStarted = true
                                isCapturingCodeFromText = true
                                if (!toolCallStarted) {
                                    toolCallStarted = true
                                    emit(HtmlAgentEvent.ToolCallStart("write_html", "text-stream"))
                                }
                                val textBefore = accumulated.substring(0, codeBlockIndex)
                                if (textBefore.isNotBlank()) textBuilder.append(textBefore)
                                
                                val codeStart = codeBlockIndex + codeBlockMarkerLength
                                val actualStart = if (codeStart < accumulated.length && accumulated[codeStart] == '\n') codeStart + 1 else codeStart
                                if (actualStart < accumulated.length) {
                                    val codeContent = accumulated.substring(actualStart)
                                    val endIndex = codeContent.indexOf("```")
                                    val code = if (endIndex >= 0) { isCapturingCodeFromText = false; codeContent.substring(0, endIndex) } else codeContent
                                    if (code.isNotEmpty()) {
                                        htmlBuilder.clear()
                                        htmlBuilder.append(code)
                                        emit(HtmlAgentEvent.CodeDelta(code, htmlBuilder.toString()))
                                    }
                                }
                                return@collect
                            }
                            
                            // 检测直接的 HTML 内容
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
                                val textBefore = accumulated.substring(0, directHtmlIndex)
                                if (textBefore.isNotBlank()) textBuilder.append(textBefore)
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
                            if (event.delta.contains("```")) {
                                val remainingCode = event.delta.substringBefore("```")
                                if (remainingCode.isNotEmpty()) {
                                    htmlBuilder.append(remainingCode)
                                    emit(HtmlAgentEvent.CodeDelta(remainingCode, htmlBuilder.toString()))
                                }
                                isCapturingCodeFromText = false
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
                            textBuilder.append(event.delta)
                            emit(HtmlAgentEvent.TextDelta(event.delta, textBuilder.toString()))
                        }
                    }
                    is ToolStreamEvent.ToolCallStart -> {
                        AppLogger.d(TAG, "Tool call started: ${event.toolName}")
                        currentToolName = event.toolName
                        currentToolCallId = event.toolCallId
                        currentToolArgs.clear()
                        toolCallStarted = true
                        emit(HtmlAgentEvent.ToolCallStart(event.toolName, event.toolCallId))
                        
                        if (event.toolName == "write_html" || event.toolName == "edit_html") {
                            isCapturingCodeFromArgs = true
                            htmlBuilder.clear()
                        }
                    }
                    is ToolStreamEvent.ToolArgumentsDelta -> {
                        currentToolArgs.clear()
                        currentToolArgs.append(event.accumulated)
                        
                        if (currentToolName == "write_html" && isCapturingCodeFromArgs) {
                            val htmlContent = extractHtmlFromArgsIncremental(event.accumulated)
                            if (htmlContent.isNotEmpty() && htmlContent != htmlBuilder.toString()) {
                                val delta = if (htmlBuilder.isEmpty()) htmlContent
                                else if (htmlContent.startsWith(htmlBuilder.toString())) htmlContent.substring(htmlBuilder.length)
                                else htmlContent
                                htmlBuilder.clear()
                                htmlBuilder.append(htmlContent)
                                if (delta.isNotEmpty()) emit(HtmlAgentEvent.CodeDelta(delta, htmlBuilder.toString()))
                            }
                        }
                    }
                    is ToolStreamEvent.ToolCallComplete -> {
                        AppLogger.d(TAG, "Tool call complete: ${event.toolName}, args length: ${event.arguments.length}")
                        pendingToolCalls.add(Triple(event.toolName, event.toolCallId, event.arguments))
                        
                        if (event.toolName == "write_html") {
                            val finalHtml = extractHtmlFromArgsFinal(event.arguments)
                            if (finalHtml.isNotEmpty()) {
                                htmlBuilder.clear()
                                htmlBuilder.append(finalHtml)
                                emit(HtmlAgentEvent.CodeDelta("", htmlBuilder.toString()))
                            }
                            isCapturingCodeFromArgs = false
                        }
                        
                        currentToolName = ""
                        currentToolCallId = ""
                        currentToolArgs.clear()
                    }
                    is ToolStreamEvent.Done -> {
                        streamCompleted = true
                    }
                    is ToolStreamEvent.Error -> {
                        throw Exception(event.message)
                    }
                }
            }
            
            if (!streamCompleted) {
                throw Exception(Strings.streamResponseIncomplete)
            }
            
            val finalHtmlFromStream = htmlBuilder.toString().trim()
            val finalText = textBuilder.toString().trim()
            
            AppLogger.d(TAG, "Turn $turnCount done: html=${finalHtmlFromStream.length}, text=${finalText.length}, toolCalls=${pendingToolCalls.size}")
            
            // === 处理本轮结果 ===
            
            if (pendingToolCalls.isNotEmpty()) {
                // 有工具调用 → 执行工具并添加结果到对话历史（ReAct: Observation）
                
                // 构建 assistant 消息（包含工具调用）
                val assistantToolCalls = pendingToolCalls.mapIndexed { idx, (toolName, toolCallId, arguments) ->
                    mapOf(
                        "id" to toolCallId.ifBlank { "call_$idx" },
                        "type" to "function",
                        "function" to mapOf("name" to toolName, "arguments" to arguments)
                    )
                }
                val assistantMsg = mutableMapOf<String, Any>(
                    "role" to "assistant"
                )
                if (finalText.isNotEmpty()) assistantMsg["content"] = finalText
                assistantMsg["tool_calls"] = assistantToolCalls
                conversationMessages.add(assistantMsg)
                
                // 执行每个工具调用并添加 tool 消息
                var hasWriteOrEdit = false
                
                for ((toolName, toolCallId, arguments) in pendingToolCalls) {
                    AppLogger.d(TAG, "ReAct executing tool: $toolName")
                    
                    when (toolName) {
                        "write_html" -> {
                            val html = if (finalHtmlFromStream.isNotEmpty()) finalHtmlFromStream else extractHtmlContent(arguments)
                            if (html.isNotEmpty()) {
                                writeHtmlToProject(html)
                                hasWriteOrEdit = true
                            }
                            allToolCalls.add(ToolCallInfo(toolCallId, toolName, arguments))
                            // 添加工具结果到对话
                            conversationMessages.add(mapOf(
                                "role" to "tool",
                                "tool_call_id" to toolCallId.ifBlank { "call_0" },
                                "content" to if (html.isNotEmpty()) "HTML 文件已成功写入（${html.length} 字符）" else "错误：HTML 内容为空"
                            ))
                        }
                        "edit_html" -> {
                            val result = executeToolCall(toolName, arguments)
                            emit(HtmlAgentEvent.ToolExecuted(result))
                            if (result.success && result.isHtml) {
                                writeHtmlToProject(result.result)
                                hasWriteOrEdit = true
                            }
                            allToolCalls.add(ToolCallInfo(toolCallId, toolName, arguments))
                            conversationMessages.add(mapOf(
                                "role" to "tool",
                                "tool_call_id" to toolCallId.ifBlank { "call_0" },
                                "content" to if (result.success) "编辑成功" else result.result
                            ))
                        }
                        "generate_image" -> {
                            allToolCalls.add(ToolCallInfo(toolCallId, toolName, arguments))
                            val result = executeToolCall(toolName, arguments)
                            if (result.isImageGeneration && result.result.startsWith("IMAGE_GENERATION_PENDING:")) {
                                val params = result.result.removePrefix("IMAGE_GENERATION_PENDING:").split("|")
                                if (params.size >= 3) {
                                    emit(HtmlAgentEvent.ImageGenerating(params[0]))
                                    val imageResult = executeImageGeneration(params[0], params[1], params[2], sessionConfig)
                                    emit(HtmlAgentEvent.ToolExecuted(imageResult))
                                    if (imageResult.success && imageResult.imageData != null) {
                                        emit(HtmlAgentEvent.ImageGenerated(imageResult.imageData, params[0]))
                                    }
                                    conversationMessages.add(mapOf(
                                        "role" to "tool",
                                        "tool_call_id" to toolCallId.ifBlank { "call_0" },
                                        "content" to if (imageResult.success) "图像已生成：${imageResult.result}" else "图像生成失败：${imageResult.result}"
                                    ))
                                }
                            } else {
                                emit(HtmlAgentEvent.ToolExecuted(result))
                                conversationMessages.add(mapOf(
                                    "role" to "tool",
                                    "tool_call_id" to toolCallId.ifBlank { "call_0" },
                                    "content" to result.result
                                ))
                            }
                        }
                        else -> {
                            // read_current_code, check_syntax, get_console_logs, auto_fix 等
                            val result = executeToolCall(toolName, arguments)
                            emit(HtmlAgentEvent.ToolExecuted(result))
                            allToolCalls.add(ToolCallInfo(toolCallId, toolName, arguments))
                            conversationMessages.add(mapOf(
                                "role" to "tool",
                                "tool_call_id" to toolCallId.ifBlank { "call_0" },
                                "content" to result.result
                            ))
                        }
                    }
                }
                
                // 如果本轮包含 write_html 或 edit_html 且是最后一轮或下一轮不需要继续
                // 对于 write/edit，默认完成（除非工具链中还有后续操作如 check_syntax）
                val hasNonWriteTools = pendingToolCalls.any { (name, _, _) -> 
                    name != "write_html" && name != "edit_html" && name != "generate_image" 
                }
                
                if (hasWriteOrEdit && !hasNonWriteTools) {
                    // 只有写入/编辑操作，不需要继续循环
                    reachedFinalResponse = true
                }
                // 否则继续 ReAct 循环（AI 会看到工具结果并决定下一步）
                
            } else {
                // 没有工具调用 → AI 返回了纯文本响应
                reachedFinalResponse = true
                
                // 尝试从文本中提取 HTML（兼容不支持原生工具调用的模型）
                var finalHtml = finalHtmlFromStream
                if (finalHtml.isEmpty() && finalText.isNotEmpty()) {
                    val extractedHtml = extractHtmlFromText(finalText)
                    if (extractedHtml.isNotEmpty()) {
                        AppLogger.d(TAG, "Extracted HTML from text, length: ${extractedHtml.length}")
                        finalHtml = extractedHtml
                    }
                }
                
                if (finalHtml.isNotEmpty()) {
                    writeHtmlToProject(finalHtml)
                } else if (finalText.isNotEmpty()) {
                    // 检查是否是"承诺要做但没做"的情况
                    val textLower = finalText.lowercase()
                    val isPromiseWithoutAction = textLower.contains("我来") || 
                        textLower.contains("我将") || textLower.contains("我会") ||
                        textLower.contains("让我") || textLower.contains("i will") ||
                        textLower.contains("i'll") || textLower.contains("let me")
                    
                    if (isPromiseWithoutAction) {
                        AppLogger.w(TAG, "AI promised to create but didn't output code, triggering fallback")
                        throw Exception("AI 承诺创建但未输出代码，触发回退")
                    }
                } else if (finalHtml.isEmpty() && finalText.isEmpty()) {
                    throw Exception("工具调用返回空内容")
                }
            }
        }
        
        if (turnCount >= MAX_REACT_TURNS && !reachedFinalResponse) {
            AppLogger.w(TAG, "ReAct loop reached max turns ($MAX_REACT_TURNS)")
        }
        
        emit(HtmlAgentEvent.StateChange(HtmlAgentState.COMPLETED))
        emit(HtmlAgentEvent.Completed(allToolCalls.joinToString("\n") { it.name }, allToolCalls))
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
            AppLogger.d(TAG, "JSON parse failed, trying incremental extraction: ${e.message}")
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
                AppLogger.d(TAG, "Extracted HTML from code block, length: ${extracted.length}")
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
                AppLogger.d(TAG, "Extracted HTML from generic code block, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 3. 尝试直接提取 <!DOCTYPE html> ... </html>
        val doctypePattern = Pattern.compile("(<!DOCTYPE\\s+html[\\s\\S]*?</html>)", Pattern.CASE_INSENSITIVE)
        val doctypeMatcher = doctypePattern.matcher(text)
        if (doctypeMatcher.find()) {
            val extracted = doctypeMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                AppLogger.d(TAG, "Extracted HTML from DOCTYPE pattern, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 4. 尝试直接提取 <html> ... </html>
        val htmlPattern = Pattern.compile("(<html[\\s\\S]*?</html>)", Pattern.CASE_INSENSITIVE)
        val htmlMatcher = htmlPattern.matcher(text)
        if (htmlMatcher.find()) {
            val extracted = htmlMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                AppLogger.d(TAG, "Extracted HTML from html tag pattern, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 5. 如果文本本身就是以 <!DOCTYPE 或 <html 开头，直接返回
        val trimmedText = text.trim()
        if (trimmedText.startsWith("<!DOCTYPE", ignoreCase = true) || 
            trimmedText.startsWith("<html", ignoreCase = true)) {
            AppLogger.d(TAG, "Text itself is HTML, length: ${trimmedText.length}")
            return trimmedText
        }
        
        return ""
    }
    
    /**
     * 构建工具调用模式的系统提示词
     * 
     * 设计原则：
     * - 简洁：不重复工具描述（已在 tools 参数中），只提供工作流指导
     * - 结构化：清晰的决策树，AI 知道何时用哪个工具
     * - ReAct 友好：引导 AI 使用多轮工具调用完成复杂任务
     */
    private fun buildToolCallingSystemPrompt(config: SessionConfig?, currentHtml: String?, enabledTools: List<HtmlTool> = emptyList()): String {
        val hasEditHtml = enabledTools.any { it.name == "edit_html" }
        val hasReadCode = enabledTools.any { it.name == "read_current_code" }
        val hasCheckSyntax = enabledTools.any { it.name == "check_syntax" }
        val hasGenerateImage = enabledTools.any { it.name == "generate_image" }
        val hasExistingCode = !currentHtml.isNullOrBlank()
        val currentLang = Strings.currentLanguage.value
        val isEnglish = currentLang == com.webtoapp.core.i18n.AppLanguage.ENGLISH
        val isArabic = currentLang == com.webtoapp.core.i18n.AppLanguage.ARABIC
        
        return buildString {
            if (isArabic) {
                appendLine("أنت خبير تطوير واجهات أمامية للجوال، تقوم بإنشاء صفحات HTML في WebView لتطبيقات الجوال. تنفذ العمليات عبر استدعاء الأدوات.")
            } else if (isEnglish) {
                appendLine("You are a mobile frontend expert, creating HTML pages in mobile APP WebView. You execute operations through tool calls.")
            } else {
                appendLine("你是移动端前端开发专家，在手机 APP WebView 中创建 HTML 页面。你通过工具调用来执行操作。")
            }
            appendLine()
            
            if (isArabic) {
                appendLine("# قواعد السلوك")
                appendLine("1. عندما يطلب المستخدم إنشاء/تعديل صفحة ويب، نفذ الأدوات فوراً ولا تصف الخطة فقط")
                appendLine("2. الكود يجب أن يكون كاملاً، لا تحذف أي جزء باستخدام ... أو التعليقات")
                appendLine("3. عندما يدردش المستخدم أو يسأل سؤالاً، أجب بالنص مباشرة بدون استدعاء أدوات")
                appendLine("4. استخدم تنسيق Markdown للردود النصية")
            } else if (isEnglish) {
                appendLine("# Behavior Rules")
                appendLine("1. When user asks to create/modify a webpage, immediately call tools to execute, don't just describe the plan")
                appendLine("2. Code must be complete, never omit any part with ... or comments")
                appendLine("3. When user chats or asks questions, answer with text directly, no tool calls needed")
                appendLine("4. Use Markdown format for text responses")
            } else {
                appendLine("# 行为规则")
                appendLine("1. 用户要求创建/修改网页时，立即调用工具执行，不要只描述计划")
                appendLine("2. 代码必须完整，禁止用 ... 或注释省略任何部分")
                appendLine("3. 用户闲聊或提问时，直接用文字回答，不需要调用工具")
                appendLine("4. 使用 Markdown 格式回复文字内容")
            }
            appendLine()
            
            if (isArabic) {
                appendLine("# سير العمل")
                appendLine()
                appendLine("## إنشاء صفحة جديدة")
                appendLine("→ استدعِ write_html مباشرة مع كود HTML الكامل")
            } else if (isEnglish) {
                appendLine("# Workflow")
                appendLine()
                appendLine("## Create New Page")
                appendLine("→ Directly call write_html with complete HTML code")
            } else {
                appendLine("# 工作流")
                appendLine()
                appendLine("## 创建新页面")
                appendLine("→ 直接调用 write_html，传入完整 HTML 代码")
            }
            appendLine()
            
            if (hasEditHtml || hasReadCode) {
                if (isArabic) {
                    appendLine("## تعديل صفحة موجودة")
                    if (hasReadCode && hasEditHtml) {
                        appendLine("→ استدعِ read_current_code أولاً لعرض الكود الحالي")
                        appendLine("→ تعديلات صغيرة: استخدم edit_html (يتطلب target مطابقاً للكود الحالي)")
                        appendLine("→ إعادة كتابة كبيرة: استخدم write_html لكتابة كود جديد كامل")
                    } else if (hasEditHtml) {
                        appendLine("→ تعديلات صغيرة: استخدم edit_html")
                        appendLine("→ إعادة كتابة كبيرة: استخدم write_html")
                    }
                } else if (isEnglish) {
                    appendLine("## Modify Existing Page")
                    if (hasReadCode && hasEditHtml) {
                        appendLine("→ First call read_current_code to view existing code")
                        appendLine("→ Small changes: use edit_html (target must exactly match existing code)")
                        appendLine("→ Large rewrite: use write_html to output complete new code")
                    } else if (hasEditHtml) {
                        appendLine("→ Small changes: use edit_html")
                        appendLine("→ Large rewrite: use write_html")
                    }
                } else {
                    appendLine("## 修改现有页面")
                    if (hasReadCode && hasEditHtml) {
                        appendLine("→ 先调用 read_current_code 查看现有代码")
                        appendLine("→ 小范围修改：用 edit_html（需要 target 精确匹配现有代码）")
                        appendLine("→ 大范围重写：用 write_html 输出完整新代码")
                    } else if (hasEditHtml) {
                        appendLine("→ 小范围修改：用 edit_html")
                        appendLine("→ 大范围重写：用 write_html")
                    }
                }
                appendLine()
            }
            
            if (hasCheckSyntax) {
                if (isArabic) {
                    appendLine("## تصحيح الأخطاء")
                    appendLine("→ استدعِ check_syntax للتحقق من أخطاء الصياغة")
                    appendLine("→ إصلاح باستخدام edit_html أو write_html بناءً على النتائج")
                } else if (isEnglish) {
                    appendLine("## Debug Code")
                    appendLine("→ Call check_syntax to check for syntax errors")
                    appendLine("→ Fix using edit_html or write_html based on results")
                } else {
                    appendLine("## 调试代码")
                    appendLine("→ 调用 check_syntax 检查语法错误")
                    appendLine("→ 根据结果用 edit_html 或 write_html 修复")
                }
                appendLine()
            }
            
            if (hasGenerateImage) {
                if (isArabic) {
                    appendLine("## توليد الصور")
                    appendLine("→ استدعِ generate_image، النتيجة base64 يمكن استخدامها مباشرة في <img src=\"data:image/png;base64,...\">")
                } else if (isEnglish) {
                    appendLine("## Generate Image")
                    appendLine("→ Call generate_image, returned base64 can be used directly in <img src=\"data:image/png;base64,...\">")
                } else {
                    appendLine("## 生成图像")
                    appendLine("→ 调用 generate_image，返回的 base64 可直接用于 <img src=\"data:image/png;base64,...\">")
                }
                appendLine()
            }
            
            if (isArabic) {
                appendLine("# معايير الكود")
                appendLine("- ملف HTML واحد، CSS في <style>، JS في <script>")
                appendLine("- يجب تضمين: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
                appendLine("- استخدم وحدات نسبية (vw/vh/%/rem)، تجنب العرض الثابت بالبكسل")
                appendLine("- منطقة لمس العناصر القابلة للنقر بحد أدنى 44x44px")
            } else if (isEnglish) {
                appendLine("# Code Standards")
                appendLine("- Single-file HTML, CSS in <style>, JS in <script> tags")
                appendLine("- Must include: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
                appendLine("- Use relative units (vw/vh/%/rem), avoid fixed pixel widths")
                appendLine("- Clickable elements minimum 44x44px touch area")
            } else {
                appendLine("# 代码规范")
                appendLine("- 单文件 HTML，CSS 在 <style>、JS 在 <script> 标签内")
                appendLine("- 必须包含: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
                appendLine("- 使用相对单位 (vw/vh/%/rem)，禁止固定像素宽度")
                appendLine("- 可点击元素最小 44x44px 触摸区域")
            }
            appendLine()
            
            if (hasExistingCode) {
                val html = currentHtml!! // safe: hasExistingCode guarantees non-null
                if (isArabic) {
                    appendLine("# حالة الكود الحالي")
                    appendLine("المستخدم لديه كود HTML (${html.length} حرف، ${html.lines().size} سطر).")
                    if (hasReadCode) {
                        appendLine("قبل التعديل، استدعِ read_current_code لعرض الكود الكامل.")
                    }
                } else if (isEnglish) {
                    appendLine("# Current Code State")
                    appendLine("User has existing HTML code (${html.length} chars, ${html.lines().size} lines).")
                    if (hasReadCode) {
                        appendLine("Before modifying, call read_current_code to view complete code.")
                    }
                } else {
                    appendLine("# 当前代码状态")
                    appendLine("用户已有 HTML 代码（${html.length} 字符，${html.lines().size} 行）。")
                    if (hasReadCode) {
                        appendLine("修改前请先调用 read_current_code 查看完整代码。")
                    }
                }
                if (!hasReadCode) {
                    val maxHtmlLength = 6000
                    val truncatedHtml = if (html.length > maxHtmlLength) {
                        if (isArabic) html.take(maxHtmlLength) + "\n... [تم الاقتطاع، إجمالي ${html.length} حرف]"
                        else if (isEnglish) html.take(maxHtmlLength) + "\n... [truncated, total ${html.length} chars]"
                        else html.take(maxHtmlLength) + "\n... [已截断，共 ${html.length} 字符]"
                    } else {
                        html
                    }
                    appendLine("```html")
                    appendLine(truncatedHtml)
                    appendLine("```")
                }
                appendLine()
            } else {
                if (isArabic) {
                    appendLine("# حالة الكود الحالي")
                    appendLine("لا يوجد كود حالياً، يجب الإنشاء من الصفر.")
                } else if (isEnglish) {
                    appendLine("# Current Code State")
                    appendLine("No existing code, need to create from scratch.")
                } else {
                    appendLine("# 当前代码状态")
                    appendLine("暂无现有代码，需要从头创建。")
                }
                appendLine()
            }
            
            config?.rules?.takeIf { it.isNotEmpty() }?.let { rules ->
                if (isArabic) {
                    appendLine("# قواعد المستخدم المخصصة")
                } else if (isEnglish) {
                    appendLine("# User Custom Rules")
                } else {
                    appendLine("# 用户自定义规则")
                }
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
        
        AppLogger.d(TAG, "Using simple stream mode (fallback)")
        
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
            temperature = sessionConfig?.temperature ?: 0.7f
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
                        writeHtmlToProject(finalHtml)
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
            throw Exception(Strings.streamResponseIncomplete)
        }
    }
    
    
    /**
     * 构建简化的系统提示词（回退模式，从文本中提取 HTML）
     * 与 SimplePrompts 保持一致的规范，但添加当前代码上下文和用户规则
     */
    private fun buildSimpleSystemPrompt(config: SessionConfig?, currentHtml: String?): String {
        val currentLang = Strings.currentLanguage.value
        val isEnglish = currentLang == com.webtoapp.core.i18n.AppLanguage.ENGLISH
        val isArabic = currentLang == com.webtoapp.core.i18n.AppLanguage.ARABIC
        
        return buildString {
            if (isArabic) {
                appendLine("أنت خبير تطوير واجهات أمامية للجوال، تقوم بإنشاء صفحات HTML في WebView لتطبيقات الجوال.")
                appendLine()
                appendLine("# قواعد السلوك")
                appendLine("1. عندما يطلب المستخدم إنشاء/تعديل صفحة ويب، أخرج كود HTML الكامل مباشرة بدءاً من <!DOCTYPE html>")
                appendLine("2. لا تغلف الكود في كتل \\`\\`\\`html")
                appendLine("3. يمكن إضافة شرح مختصر قبل وبعد الكود")
                appendLine("4. أجب بتنسيق Markdown للدردشة والأسئلة")
                appendLine()
                appendLine("# معايير الكود")
                appendLine("- ملف HTML واحد، CSS في <style>، JS في <script>")
                appendLine("- يجب تضمين: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
                appendLine("- استخدم وحدات نسبية (vw/vh/%/rem)، تجنب العرض الثابت بالبكسل")
                appendLine("- منطقة لمس العناصر القابلة للنقر بحد أدنى 44x44px")
                appendLine("- الكود يجب أن يكون كاملاً، لا تحذف أي جزء")
            } else if (isEnglish) {
                appendLine("You are a mobile frontend expert, creating HTML pages in mobile APP WebView.")
                appendLine()
                appendLine("# Behavior Rules")
                appendLine("1. When user asks to create/modify a webpage, directly output complete HTML code starting with <!DOCTYPE html>")
                appendLine("2. Do not wrap code in \\`\\`\\`html code blocks")
                appendLine("3. Brief explanations before and after code are fine")
                appendLine("4. Answer with Markdown format for chat and questions")
                appendLine()
                appendLine("# Code Standards")
                appendLine("- Single-file HTML, CSS in <style>, JS in <script> tags")
                appendLine("- Must include: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
                appendLine("- Use relative units (vw/vh/%/rem), avoid fixed pixel widths")
                appendLine("- Clickable elements minimum 44x44px touch area")
                appendLine("- Code must be complete, never omit any part")
            } else {
                appendLine("你是移动端前端开发专家，在手机 APP WebView 中创建 HTML 页面。")
                appendLine()
                appendLine("# 行为规则")
                appendLine("1. 用户要求创建/修改网页时，直接输出完整 HTML 代码，以 <!DOCTYPE html> 开头")
                appendLine("2. 禁止使用 \\`\\`\\`html 代码块包裹代码")
                appendLine("3. 代码前后可有简短说明文字")
                appendLine("4. 闲聊或提问时用 Markdown 格式文字回答")
                appendLine()
                appendLine("# 代码规范")
                appendLine("- 单文件 HTML，CSS 在 <style>、JS 在 <script> 标签内")
                appendLine("- 必须包含: <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
                appendLine("- 使用相对单位 (vw/vh/%/rem)，禁止固定像素宽度")
                appendLine("- 可点击元素最小 44x44px 触摸区域")
                appendLine("- 代码完整，禁止用 ... 或注释省略任何部分")
            }
            appendLine()
            
            if (!currentHtml.isNullOrBlank()) {
                val maxHtmlLength = 6000
                val truncatedHtml = if (currentHtml.length > maxHtmlLength) {
                    if (isArabic) currentHtml.take(maxHtmlLength) + "\n... [تم الاقتطاع، إجمالي ${currentHtml.length} حرف]"
                    else if (isEnglish) currentHtml.take(maxHtmlLength) + "\n... [truncated, total ${currentHtml.length} chars]"
                    else currentHtml.take(maxHtmlLength) + "\n... [已截断，共 ${currentHtml.length} 字符]"
                } else {
                    currentHtml
                }
                if (isArabic) {
                    appendLine("# الكود الحالي")
                    appendLine("المستخدم لديه الكود التالي (${currentHtml.length} حرف)، قم بالتعديل على أساسه:")
                } else if (isEnglish) {
                    appendLine("# Current Code")
                    appendLine("User has the following code (${currentHtml.length} chars), modify based on it:")
                } else {
                    appendLine("# 当前代码")
                    appendLine("用户已有以下代码（${currentHtml.length} 字符），修改时在此基础上：")
                }
                appendLine(truncatedHtml)
                appendLine()
            }
            
            config?.rules?.takeIf { it.isNotEmpty() }?.let { rules ->
                if (isArabic) appendLine("# قواعد المستخدم المخصصة")
                else if (isEnglish) appendLine("# User Custom Rules")
                else appendLine("# 用户自定义规则")
                rules.forEachIndexed { i, r -> appendLine("${i+1}. $r") }
            }
        }.trimEnd()
    }

    
    /**
     * 选择模型
     */
    private suspend fun selectModel(
        preferredModel: SavedModel?,
        savedModels: List<SavedModel>
    ): SavedModel? {
        if (preferredModel != null) return preferredModel
        
        val aiCodingModels = savedModels.filter { it.supportsFeature(AiFeature.AI_CODING) }
        val defaultModelId = aiConfigManager.defaultModelIdFlow.first()
        
        return aiCodingModels.find { it.id == defaultModelId }
            ?: aiCodingModels.firstOrNull()
            ?: savedModels.find { it.id == defaultModelId }
            ?: savedModels.firstOrNull()
    }
}


// ==================== 数据类 ====================

/**
 * HTML 工具定义
 */
data class HtmlTool(
    val type: AiCodingToolType,
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
