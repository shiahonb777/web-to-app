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
 * HTML Agent.
 * 
 * Tool Calling AI / HTML.
 * Note.
 */
class AiCodingAgent(private val context: Context) {
    
    private val gson = com.webtoapp.util.GsonProvider.gson
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    private val projectFileManager = ProjectFileManager(context)
    
    // ID.
    private var currentSessionId: String? = null
    
    // Note.
    private val consoleLogs = mutableListOf<ConsoleLogEntry>()
    
    // Note.
    private val syntaxErrors = mutableListOf<SyntaxError>()
    
    companion object {
        private const val TAG = "AiCodingAgent"
        private const val STREAM_TIMEOUT_MS = 120_000L  // Note.
    }
    
    // HTML.
    private var currentHtmlCode: String = ""
    
    /**
     * ID.
     */
    fun setSessionId(sessionId: String) {
        currentSessionId = sessionId
    }
    
    /**
     * HTML.
     */
    fun setCurrentHtml(html: String) {
        currentHtmlCode = html
    }
    
    /**
     * HTML.
     */
    fun getCurrentHtml(): String = currentHtmlCode
    
    // ==================== ====================
    
    /**
     * Note.
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
     * Note.
     */
    fun getEnabledTools(config: SessionConfig?): List<HtmlTool> {
        val enabledTypes = config?.enabledTools ?: setOf(AiCodingToolType.WRITE_HTML)
        val hasImageModel = !config?.imageModelId.isNullOrBlank()
        
        return getAllTools().filter { tool ->
            tool.type in enabledTypes && 
            // Note.
            (!tool.type.requiresImageModel || hasImageModel)
        }
    }
    
    // ==================== ====================
    
    /**
     * Note.
     */
    fun addConsoleLog(entry: ConsoleLogEntry) {
        consoleLogs.add(entry)
        // Note.
        if (consoleLogs.size > 100) {
            consoleLogs.removeAt(0)
        }
    }
    
    /**
     * Note.
     */
    fun clearConsoleLogs() {
        consoleLogs.clear()
    }
    
    /**
     * Note.
     */
    fun getConsoleLogs(filter: String = "all"): List<ConsoleLogEntry> {
        return when (filter) {
            "error" -> consoleLogs.filter { it.level == ConsoleLogLevel.ERROR }
            "warn" -> consoleLogs.filter { it.level == ConsoleLogLevel.WARN }
            "log" -> consoleLogs.filter { it.level == ConsoleLogLevel.LOG }
            else -> consoleLogs.toList()
        }
    }
    
    // ==================== ====================
    
    /**
     * Note.
     */
    fun addSyntaxError(error: SyntaxError) {
        syntaxErrors.add(error)
    }
    
    /**
     * Note.
     */
    fun clearSyntaxErrors() {
        syntaxErrors.clear()
    }
    
    /**
     * Note.
     */
    fun getSyntaxErrors(): List<SyntaxError> = syntaxErrors.toList()

    
    // ==================== ====================
    
    /**
     * Note.
     */
    fun checkSyntax(code: String, language: String = "html"): List<SyntaxError> {
        clearSyntaxErrors()
        val errors = when (language.lowercase()) {
            "html", "htm" -> checkHtmlSyntax(code)
            "css" -> checkCssSyntax(code)
            "javascript", "js" -> checkJavaScriptSyntax(code)
            else -> {
                // HTML.
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
     * HTML.
     */
    private fun checkHtmlSyntax(code: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val lines = code.lines()
        
        // Check.
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
                            // Note.
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
        
        // Note.
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
     * CSS.
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
     * JavaScript.
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
                // Note.
                if (!inString && !inMultiLineComment && i + 1 < line.length && line[i] == '/' && line[i+1] == '/') {
                    break
                }
                
                // Note.
                if (!inString && !inMultiLineComment && i + 1 < line.length && line[i] == '/' && line[i+1] == '*') {
                    inMultiLineComment = true
                    i += 2
                    continue
                }
                
                // Note.
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
     * HTML CSS.
     */
    private fun extractAndCheckCss(html: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val stylePattern = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE)
        val matcher = stylePattern.matcher(html)
        
        while (matcher.find()) {
            val css = matcher.group(1) ?: continue
            val cssErrors = checkCssSyntax(css)
            // style.
            val styleStart = html.substring(0, matcher.start()).count { it == '\n' }
            cssErrors.forEach { error ->
                errors.add(error.copy(line = error.line + styleStart))
            }
        }
        
        return errors
    }
    
    /**
     * HTML JavaScript.
     */
    private fun extractAndCheckJs(html: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val scriptPattern = Pattern.compile("<script[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE)
        val matcher = scriptPattern.matcher(html)
        
        while (matcher.find()) {
            val js = matcher.group(1) ?: continue
            if (js.isBlank()) continue
            val jsErrors = checkJavaScriptSyntax(js)
            // Note.
            val scriptStart = html.substring(0, matcher.start()).count { it == '\n' }
            jsErrors.forEach { error ->
                errors.add(error.copy(line = error.line + scriptStart))
            }
        }
        
        return errors
    }
    
    // ==================== ====================
    
    /**
     * HTML.
     * Note.
     * 1. JSON {"html": "..."} {"content": "..."}.
     * 2. HTML.
     * 3. JSON.
     */
    private fun extractHtmlContent(arguments: String): String {
        val trimmed = arguments.trim()
        
        // 1. JSON.
        try {
            val json = JsonParser.parseString(trimmed)
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                // Note.
                val content = obj.get("html")?.asString 
                    ?: obj.get("content")?.asString
                    ?: obj.get("code")?.asString
                if (!content.isNullOrBlank()) {
                    return content
                }
            } else if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
                // JSON.
                return json.asString
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Not a valid JSON, trying as raw HTML: ${e.message}")
        }
        
        // 2. <!DOCTYPE <html HTML.
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || 
            trimmed.startsWith("<html", ignoreCase = true) ||
            trimmed.startsWith("<", ignoreCase = true)) {
            return trimmed
        }
        
        // Note.
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            val unquoted = trimmed.substring(1, trimmed.length - 1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\\", "\\")
            return unquoted
        }
        
        // Note.
        return trimmed
    }

    /**
     * Note.
     */
    fun executeToolCall(toolName: String, arguments: String): ToolExecutionResult {
        AppLogger.d(TAG, "executeToolCall: $toolName, args length: ${arguments.length}, args preview: ${arguments.take(200)}")
        
        return try {
            when (toolName) {
                "write_html" -> {
                    // HTML.
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
                    
                    // Note.
                    val sessionId = currentSessionId
                    if (sessionId != null) {
                        // Check.
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
                        // sessionId.
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
                        // fuzzy matching.
                        val normalizedTarget = target.replace(Regex("\\s+"), " ").trim()
                        val normalizedCode = currentHtmlCode.replace(Regex("\\s+"), " ").trim()
                        if (!normalizedCode.contains(normalizedTarget)) {
                            // target.
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
                                        // Note.
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
                                // Note.
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
                            
                            // AI.
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
                        // Note.
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
                    
                    currentHtmlCode = newHtml  // Update HTML.
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
                    // auto_fix AI check_syntax + write_html.
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
                    // Note.
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
     * Note.
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
            
            // Build.
            val enhancedPrompt = buildImagePrompt(prompt, style)
            val dimensions = getSizeDimensions(size)
            
            // API.
            val result = aiClient.generateImage(
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
     * Note.
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
     * Note.
     */
    private fun getSizeDimensions(size: String): Pair<Int, Int> {
        return when (size) {
            "small" -> 256 to 256
            "medium" -> 512 to 512
            "large" -> 1024 to 1024
            else -> 512 to 512
        }
    }
    
    // ==================== ====================
    
    /**
     * HTML.
     * Note.
     * 
     * @return Pair<ToolExecutionResult, ProjectFileInfo?>.
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
    
    // ==================== ====================
    
    /**
     * HTML.
     * Note.
     * ReAct.
     */
    fun developWithStream(
        requirement: String,
        currentHtml: String? = null,
        sessionConfig: SessionConfig? = null,
        model: SavedModel? = null,
        sessionId: String? = null  // SessionID.
    ): Flow<HtmlAgentEvent> = flow {
        try {
            AppLogger.d(TAG, "developWithStream started with requirement: ${requirement.take(100)}, sessionId: $sessionId")
            
            // Set ID.
            if (sessionId != null) {
                currentSessionId = sessionId
            }
            
            // Get AI Config.
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            if (apiKeys.isEmpty()) {
                emit(HtmlAgentEvent.Error("请先在 AI 设置中配置 API Key"))
                return@flow
            }
            
            // Select.
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
            
            // Set HTML.
            if (sessionId != null && currentHtml.isNullOrBlank()) {
                val latestFile = projectFileManager.getLatestVersion(sessionId, "index")
                if (latestFile != null) {
                    currentHtmlCode = projectFileManager.readFile(sessionId, latestFile.name) ?: ""
                }
            } else if (!currentHtml.isNullOrBlank()) {
                currentHtmlCode = currentHtml
            }
            
            // Check.
            // LLM OpenAI API.
            // Note.
            val modelId = selectedModel.model.id.lowercase()
            val providerName = apiKey.provider.name.lowercase()
            val baseUrl = (apiKey.baseUrl ?: "").lowercase()
            
            // OpenAI.
            val knownNoToolCalling = setOf(
                "deepseek-coder", "deepseek-coder-v2",        // DeepSeek Coder.
                "yi-coder",                                     // Yi Coder
                "codestral",                                    // Mistral Codestral
            )
            val supportsToolCalling = knownNoToolCalling.none { modelId.contains(it) }
            
            AppLogger.d(TAG, "Model: $modelId, Provider: $providerName, BaseUrl: $baseUrl, SupportsToolCalling: $supportsToolCalling")
            
            // Note.
            var useSimpleMode = !supportsToolCalling  // Note.
            var toolCallError: String? = null
            
            if (!useSimpleMode) {
                try {
                    // Note.
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
            
            // Note.
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
            throw e  // Note.
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error in developWithStream", e)
            emit(HtmlAgentEvent.Error(Strings.errorOccurredPrefix.replace("%s", e.message ?: Strings.unknownError)))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * HTML.
     * 
     * ReAct Reasoning-Action-Observation.
     * 1. AI.
     * 2. observation AI.
     * 3. AI observation.
     * 4. MAX_REACT_TURNS.
     */
    private fun developWithToolCalling(
        requirement: String,
        sessionConfig: SessionConfig?,
        selectedModel: SavedModel,
        apiKey: ApiKeyConfig
    ): Flow<HtmlAgentEvent> = flow {
        val MAX_REACT_TURNS = 5  // ReAct.
        
        // Get.
        val enabledTools = getEnabledTools(sessionConfig)
        
        // Build.
        val systemPrompt = buildToolCallingSystemPrompt(sessionConfig, currentHtmlCode.takeIf { it.isNotBlank() }, enabledTools)
        
        // OpenAI.
        val tools = enabledTools.map { it.toOpenAiFormat() }
        
        AppLogger.d(TAG, "ReAct loop: ${enabledTools.size} tools: ${enabledTools.map { it.name }}")
        
        // Note.
        val conversationMessages = mutableListOf<Map<String, Any>>(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to requirement)
        )
        
        // ReAct.
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
            
            // Note.
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
                        
                        // Note.
                        if (isCapturingCodeFromArgs) {
                            textBuilder.append(event.delta)
                            emit(HtmlAgentEvent.TextDelta(event.delta, textBuilder.toString()))
                            return@collect
                        }
                        
                        // ```html HTML.
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
                            
                            // HTML.
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
            
            // Note.
            
            if (pendingToolCalls.isNotEmpty()) {
                // → ReAct: Observation.
                
                // assistant.
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
                
                // tool.
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
                            // Note.
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
                            // read_current_code, check_syntax, get_console_logs, auto_fix.
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
                
                // write_html edit_html.
                // write/edit check_syntax.
                val hasNonWriteTools = pendingToolCalls.any { (name, _, _) -> 
                    name != "write_html" && name != "edit_html" && name != "generate_image" 
                }
                
                if (hasWriteOrEdit && !hasNonWriteTools) {
                    // Note.
                    reachedFinalResponse = true
                }
                // ReAct AI.
                
            } else {
                // → AI.
                reachedFinalResponse = true
                
                // HTML.
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
                    // Note.
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
     * HTML.
     * JSON.
     */
    private fun extractHtmlFromArgsIncremental(args: String): String {
        if (args.isBlank()) return ""
        
        val trimmed = args.trim()
        
        // Find "html": " JSON.
        val htmlKeyIndex = trimmed.indexOf("\"html\"")
        if (htmlKeyIndex < 0) return ""
        
        // Note.
        val colonIndex = trimmed.indexOf(':', htmlKeyIndex + 6)
        if (colonIndex < 0) return ""
        
        val quoteIndex = trimmed.indexOf('"', colonIndex + 1)
        if (quoteIndex < 0) return ""
        
        // Note.
        val startIndex = quoteIndex + 1
        if (startIndex >= trimmed.length) return ""
        
        // Note.
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
        
        // Note.
        val extracted = trimmed.substring(startIndex, endIndex)
        return decodeJsonString(extracted)
    }
    
    /**
     * HTML JSON.
     */
    private fun extractHtmlFromArgsFinal(args: String): String {
        if (args.isBlank()) return ""
        
        val trimmed = args.trim()
        
        // 1. JSON.
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
        
        // Note.
        return extractHtmlFromArgsIncremental(trimmed)
    }
    
    /**
     * JSON.
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
     * HTML.
     * Note.
     * 
     * Note.
     * 1. ```html ... ```.
     * 2. <!DOCTYPE html> ... </html>.
     * 3. <html> ... </html>.
     */
    private fun extractHtmlFromText(text: String): String {
        if (text.isBlank()) return ""
        
        // 1. ```html.
        val codeBlockPattern = Pattern.compile("```html\\s*\\n?([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE)
        val codeBlockMatcher = codeBlockPattern.matcher(text)
        if (codeBlockMatcher.find()) {
            val extracted = codeBlockMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                AppLogger.d(TAG, "Extracted HTML from code block, length: ${extracted.length}")
                return extracted
            }
        }
        
        // Note.
        val genericCodeBlockPattern = Pattern.compile("```\\s*\\n?([\\s\\S]*?)```")
        val genericMatcher = genericCodeBlockPattern.matcher(text)
        while (genericMatcher.find()) {
            val extracted = genericMatcher.group(1)?.trim() ?: ""
            // Check HTML.
            if (extracted.contains("<!DOCTYPE", ignoreCase = true) || 
                extracted.contains("<html", ignoreCase = true)) {
                AppLogger.d(TAG, "Extracted HTML from generic code block, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 3. <!DOCTYPE html> ... </html>.
        val doctypePattern = Pattern.compile("(<!DOCTYPE\\s+html[\\s\\S]*?</html>)", Pattern.CASE_INSENSITIVE)
        val doctypeMatcher = doctypePattern.matcher(text)
        if (doctypeMatcher.find()) {
            val extracted = doctypeMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                AppLogger.d(TAG, "Extracted HTML from DOCTYPE pattern, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 4. <html> ... </html>.
        val htmlPattern = Pattern.compile("(<html[\\s\\S]*?</html>)", Pattern.CASE_INSENSITIVE)
        val htmlMatcher = htmlPattern.matcher(text)
        if (htmlMatcher.find()) {
            val extracted = htmlMatcher.group(1)?.trim() ?: ""
            if (extracted.isNotEmpty()) {
                AppLogger.d(TAG, "Extracted HTML from html tag pattern, length: ${extracted.length}")
                return extracted
            }
        }
        
        // 5. <!DOCTYPE <html.
        val trimmedText = text.trim()
        if (trimmedText.startsWith("<!DOCTYPE", ignoreCase = true) || 
            trimmedText.startsWith("<html", ignoreCase = true)) {
            AppLogger.d(TAG, "Text itself is HTML, length: ${trimmedText.length}")
            return trimmedText
        }
        
        return ""
    }
    
    /**
     * Note.
     * 
     * Note.
     * - tools.
     * - AI.
     * - ReAct AI.
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
     * HTML.
     * collect.
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
                    
                    // HTML.
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
     * HTML.
     * SimplePrompts.
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
     * Note.
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


// ==================== ====================

/**
 * HTML.
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
 * Note.
 */
enum class ConsoleLogLevel {
    LOG, WARN, ERROR, INFO, DEBUG
}

/**
 * Note.
 */
data class ConsoleLogEntry(
    val level: ConsoleLogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String? = null,
    val lineNumber: Int? = null
)

/**
 * Note.
 */
enum class ErrorSeverity {
    ERROR, WARNING, INFO
}

/**
 * Note.
 */
data class SyntaxError(
    val type: String,           // html, css, javascript
    val message: String,
    val line: Int,
    val column: Int,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Note.
 */
data class ToolExecutionResult(
    val success: Boolean,
    val toolName: String,
    val result: String,
    val isHtml: Boolean = false,
    val isEdit: Boolean = false,  // Yes write_html edit_html.
    val isImageGeneration: Boolean = false,  // Yes.
    val imageData: String? = null,  // Generate base64.
    val syntaxErrors: List<SyntaxError> = emptyList(),
    val fileInfo: ProjectFileInfo? = null  // Create/.
)

/**
 * Agent.
 */
enum class HtmlAgentState {
    IDLE,
    GENERATING,
    COMPLETED,
    ERROR
}

/**
 * Agent -.
 */
sealed class HtmlAgentEvent {
    // Note.
    data class StateChange(val state: HtmlAgentState) : HtmlAgentEvent()
    
    // File.
    data class FileCreated(
        val fileInfo: ProjectFileInfo,
        val isNewVersion: Boolean
    ) : HtmlAgentEvent()
    
    // Note.
    data class TextDelta(val delta: String, val accumulated: String) : HtmlAgentEvent()
    
    // Note.
    data class ThinkingDelta(val delta: String, val accumulated: String) : HtmlAgentEvent()
    
    // Note.
    data class ToolCallStart(val toolName: String, val toolCallId: String) : HtmlAgentEvent()
    
    // HTML.
    data class CodeDelta(val delta: String, val accumulated: String) : HtmlAgentEvent()
    
    // Note.
    data class ToolExecuted(val result: ToolExecutionResult) : HtmlAgentEvent()
    
    // Note.
    data class ImageGenerating(val prompt: String) : HtmlAgentEvent()
    
    // Note.
    data class ImageGenerated(val imageData: String, val prompt: String) : HtmlAgentEvent()
    
    // HTML.
    data class HtmlComplete(val html: String) : HtmlAgentEvent()
    
    // Auto.
    data class AutoPreview(val html: String) : HtmlAgentEvent()
    
    // All.
    data class Completed(val textContent: String, val toolCalls: List<ToolCallInfo>) : HtmlAgentEvent()
    
    // Error
    data class Error(val message: String) : HtmlAgentEvent()
}
