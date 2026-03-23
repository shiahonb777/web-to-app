package com.webtoapp.core.extension.agent

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.webtoapp.core.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Agent 工具执行器
 * 
 * 负责执行 Agent 调用的各种工具，支持：
 * - 单个工具执行
 * - 工具链执行（按顺序执行多个工具）
 * - 工具结果传递（前一个工具的输出可作为后一个工具的输入）
 * 
 * Requirements: 5.5
 */
class AgentToolExecutor(private val context: Context) {
    
    private val gson = Gson()
    private val extensionManager = ExtensionManager.getInstance(context)
    
    /**
     * 执行工具调用
     */
    suspend fun execute(request: ToolCallRequest): ToolCallResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = when (request.toolName) {
                "syntax_check" -> executeSyntaxCheck(request.arguments)
                "lint_code" -> executeLintCode(request.arguments)
                "security_scan" -> executeSecurityScan(request.arguments)
                "fix_error" -> executeFixError(request.arguments)
                "get_templates" -> executeGetTemplates(request.arguments)
                "get_snippets" -> executeGetSnippets(request.arguments)
                "validate_config" -> executeValidateConfig(request.arguments)
                "create_module" -> executeCreateModule(request.arguments)
                else -> throw IllegalArgumentException("未知工具: ${request.toolName}")
            }

            ToolCallResult(
                callId = request.callId,
                toolName = request.toolName,
                success = true,
                result = result,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ToolCallResult(
                callId = request.callId,
                toolName = request.toolName,
                success = false,
                result = null,
                error = e.message ?: "执行失败",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * 执行工具链
     * 
     * 按顺序执行多个工具调用，支持工具间的数据传递。
     * 前一个工具的输出可以通过 {{previous_result}} 占位符传递给后一个工具。
     * 
     * @param tools 要执行的工具调用列表
     * @param stopOnFailure 是否在工具执行失败时停止链式执行，默认为 true
     * @return Flow<ToolChainEvent> 工具链执行事件流
     * 
     * Requirements: 5.5
     */
    fun executeChain(
        tools: List<ToolCallRequest>,
        stopOnFailure: Boolean = true
    ): Flow<ToolChainEvent> = flow {
        if (tools.isEmpty()) {
            emit(ToolChainEvent.ChainCompleted(emptyList()))
            return@flow
        }
        
        emit(ToolChainEvent.ChainStarted(tools.size))
        
        val results = mutableListOf<ToolCallResult>()
        var previousResult: Any? = null
        
        for ((index, request) in tools.withIndex()) {
            // Handle参数中的占位符，支持前一个工具结果的传递
            val processedRequest = processRequestWithPreviousResult(request, previousResult)
            
            emit(ToolChainEvent.ToolStarted(index, processedRequest))
            
            val result = execute(processedRequest)
            results.add(result)
            
            emit(ToolChainEvent.ToolCompleted(index, result))
            
            if (!result.success && stopOnFailure) {
                emit(ToolChainEvent.ChainFailed(
                    failedToolIndex = index,
                    error = result.error ?: "工具执行失败",
                    completedResults = results
                ))
                return@flow
            }
            
            // Save当前结果供下一个工具使用
            previousResult = result.result
        }
        
        emit(ToolChainEvent.ChainCompleted(results))
    }.flowOn(Dispatchers.IO)
    
    /**
     * 执行语法检查和自动修复链
     * 
     * 这是一个预定义的工具链，用于：
     * 1. 执行语法检查
     * 2. 如果有错误，尝试自动修复
     * 3. 重新检查修复后的代码
     * 
     * @param code 要检查的代码
     * @param language 代码语言 (javascript/css)
     * @param maxFixAttempts 最大修复尝试次数，默认为 3
     * @return Flow<ToolChainEvent> 工具链执行事件流
     * 
     * Requirements: 5.3, 5.4, 5.5
     */
    fun executeSyntaxCheckAndFixChain(
        code: String,
        language: String = "javascript",
        maxFixAttempts: Int = 3
    ): Flow<ToolChainEvent> = flow {
        var currentCode = code
        var attemptCount = 0
        val allResults = mutableListOf<ToolCallResult>()
        
        emit(ToolChainEvent.ChainStarted(maxFixAttempts * 2)) // 最多 check + fix 循环
        
        while (attemptCount < maxFixAttempts) {
            // 步骤 1: 语法检查
            val checkRequest = ToolCallRequest(
                toolName = "syntax_check",
                arguments = mapOf("code" to currentCode, "language" to language)
            )
            
            emit(ToolChainEvent.ToolStarted(attemptCount * 2, checkRequest))
            val checkResult = execute(checkRequest)
            allResults.add(checkResult)
            emit(ToolChainEvent.ToolCompleted(attemptCount * 2, checkResult))
            
            if (!checkResult.success) {
                emit(ToolChainEvent.ChainFailed(
                    failedToolIndex = attemptCount * 2,
                    error = checkResult.error ?: "语法检查执行失败",
                    completedResults = allResults
                ))
                return@flow
            }
            
            val syntaxResult = checkResult.result as? SyntaxCheckResult
            
            // 如果语法正确，完成链式执行
            if (syntaxResult == null || syntaxResult.valid) {
                emit(ToolChainEvent.ChainCompleted(allResults))
                return@flow
            }
            
            // 步骤 2: 尝试修复
            attemptCount++
            
            if (attemptCount >= maxFixAttempts) {
                // 达到最大尝试次数，返回最后的检查结果
                emit(ToolChainEvent.ChainFailed(
                    failedToolIndex = attemptCount * 2 - 1,
                    error = "已达到最大自动修复次数 ($maxFixAttempts 次)，仍有语法错误",
                    completedResults = allResults
                ))
                return@flow
            }
            
            val fixRequest = ToolCallRequest(
                toolName = "fix_error",
                arguments = mapOf(
                    "code" to currentCode,
                    "language" to language,
                    "errors" to syntaxResult.errors.map { 
                        mapOf(
                            "line" to it.line,
                            "message" to it.message,
                            "suggestion" to it.suggestion
                        )
                    }
                )
            )
            
            emit(ToolChainEvent.ToolStarted(attemptCount * 2 - 1, fixRequest))
            val fixResult = execute(fixRequest)
            allResults.add(fixResult)
            emit(ToolChainEvent.ToolCompleted(attemptCount * 2 - 1, fixResult))
            
            if (!fixResult.success) {
                emit(ToolChainEvent.ChainFailed(
                    failedToolIndex = attemptCount * 2 - 1,
                    error = fixResult.error ?: "自动修复执行失败",
                    completedResults = allResults
                ))
                return@flow
            }
            
            // Update代码为修复后的版本
            val fixResultMap = fixResult.result as? Map<*, *>
            currentCode = fixResultMap?.get("fixed_code") as? String ?: currentCode
        }
        
        emit(ToolChainEvent.ChainCompleted(allResults))
    }.flowOn(Dispatchers.IO)
    
    /**
     * 处理请求参数中的占位符
     * 
     * 支持的占位符：
     * - {{previous_result}}: 前一个工具的完整结果
     * - {{previous_result.field}}: 前一个工具结果中的特定字段
     */
    private fun processRequestWithPreviousResult(
        request: ToolCallRequest,
        previousResult: Any?
    ): ToolCallRequest {
        if (previousResult == null) {
            return request
        }
        
        val processedArgs = request.arguments.mapValues { (_, value) ->
            when (value) {
                is String -> processStringPlaceholder(value, previousResult)
                else -> value
            }
        }
        
        return request.copy(arguments = processedArgs)
    }
    
    /**
     * 处理字符串中的占位符
     */
    private fun processStringPlaceholder(value: String, previousResult: Any?): Any? {
        return when {
            value == "{{previous_result}}" -> previousResult
            value.startsWith("{{previous_result.") && value.endsWith("}}") -> {
                val fieldPath = value.removePrefix("{{previous_result.").removeSuffix("}}")
                extractFieldFromResult(previousResult, fieldPath)
            }
            value.contains("{{previous_result}}") -> {
                value.replace("{{previous_result}}", previousResult.toString())
            }
            else -> value
        }
    }
    
    /**
     * 从结果中提取指定字段
     */
    private fun extractFieldFromResult(result: Any?, fieldPath: String): Any? {
        if (result == null) return null
        
        return when (result) {
            is Map<*, *> -> {
                val parts = fieldPath.split(".", limit = 2)
                val value = result[parts[0]]
                if (parts.size > 1 && value != null) {
                    extractFieldFromResult(value, parts[1])
                } else {
                    value
                }
            }
            is SyntaxCheckResult -> {
                when (fieldPath) {
                    "valid" -> result.valid
                    "errors" -> result.errors
                    "warnings" -> result.warnings
                    else -> null
                }
            }
            is SecurityScanResult -> {
                when (fieldPath) {
                    "safe" -> result.safe
                    "issues" -> result.issues
                    "riskLevel" -> result.riskLevel
                    else -> null
                }
            }
            else -> null
        }
    }
    
    /**
     * 语法检查
     */
    private suspend fun executeSyntaxCheck(args: Map<String, Any?>): SyntaxCheckResult {
        val code = args["code"] as? String ?: throw IllegalArgumentException("缺少 code 参数")
        val language = args["language"] as? String ?: "javascript"
        
        return when (language.lowercase()) {
            "javascript", "js" -> checkJavaScriptSyntax(code)
            "css" -> checkCssSyntax(code)
            else -> throw IllegalArgumentException("不支持的语言: $language")
        }
    }
    
    /**
     * JavaScript 语法检查
     */
    private fun checkJavaScriptSyntax(code: String): SyntaxCheckResult {
        val errors = mutableListOf<CodeError>()
        val warnings = mutableListOf<CodeWarning>()
        
        // 基础语法检查规则
        val lines = code.lines()
        var braceCount = 0
        var parenCount = 0
        var bracketCount = 0
        var inString = false
        var stringChar = ' '
        var inComment = false
        var inMultiLineComment = false
        
        lines.forEachIndexed { lineIndex, line ->
            val lineNum = lineIndex + 1
            var i = 0
            
            while (i < line.length) {
                val char = line[i]
                val nextChar = if (i + 1 < line.length) line[i + 1] else ' '
                
                // Handle注释
                if (!inString) {
                    if (char == '/' && nextChar == '/') {
                        break // 单行注释，跳过剩余行
                    }
                    if (char == '/' && nextChar == '*') {
                        inMultiLineComment = true
                        i += 2
                        continue
                    }
                    if (inMultiLineComment && char == '*' && nextChar == '/') {
                        inMultiLineComment = false
                        i += 2
                        continue
                    }
                }
                
                if (inMultiLineComment) {
                    i++
                    continue
                }
                
                // Handle字符串
                if ((char == '"' || char == '\'' || char == '`') && (i == 0 || line[i-1] != '\\')) {
                    if (!inString) {
                        inString = true
                        stringChar = char
                    } else if (char == stringChar) {
                        inString = false
                    }
                }
                
                if (!inString) {
                    when (char) {
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

            // Check常见错误模式
            checkJsLineErrors(line, lineNum, errors, warnings)
        }
        
        // Check括号匹配
        if (braceCount != 0) {
            errors.add(CodeError(
                line = lines.size,
                column = 0,
                message = "大括号不匹配，${if (braceCount > 0) "缺少 $braceCount 个 }" else "多余 ${-braceCount} 个 }"}",
                severity = ErrorSeverity.ERROR,
                suggestion = "检查所有 { } 是否正确配对"
            ))
        }
        if (parenCount != 0) {
            errors.add(CodeError(
                line = lines.size,
                column = 0,
                message = "小括号不匹配，${if (parenCount > 0) "缺少 $parenCount 个 )" else "多余 ${-parenCount} 个 )"}",
                severity = ErrorSeverity.ERROR,
                suggestion = "检查所有 ( ) 是否正确配对"
            ))
        }
        if (bracketCount != 0) {
            errors.add(CodeError(
                line = lines.size,
                column = 0,
                message = "方括号不匹配，${if (bracketCount > 0) "缺少 $bracketCount 个 ]" else "多余 ${-bracketCount} 个 ]"}",
                severity = ErrorSeverity.ERROR,
                suggestion = "检查所有 [ ] 是否正确配对"
            ))
        }
        
        return SyntaxCheckResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 检查 JS 行级错误
     */
    private fun checkJsLineErrors(line: String, lineNum: Int, errors: MutableList<CodeError>, warnings: MutableList<CodeWarning>) {
        val trimmed = line.trim()
        
        // Check var 使用（建议用 let/const）
        if (trimmed.startsWith("var ")) {
            warnings.add(CodeWarning(
                line = lineNum,
                column = line.indexOf("var"),
                message = "建议使用 let 或 const 代替 var",
                rule = "no-var"
            ))
        }
        
        // Check == 使用（建议用 ===）
        val eqMatch = Regex("[^=!<>]==[^=]").find(line)
        if (eqMatch != null) {
            warnings.add(CodeWarning(
                line = lineNum,
                column = eqMatch.range.first + 1,
                message = "建议使用 === 代替 ==",
                rule = "eqeqeq"
            ))
        }
        
        // Check eval 使用
        if (trimmed.contains("eval(")) {
            warnings.add(CodeWarning(
                line = lineNum,
                column = line.indexOf("eval("),
                message = "避免使用 eval()，存在安全风险",
                rule = "no-eval"
            ))
        }
        
        // Check document.write
        if (trimmed.contains("document.write(")) {
            warnings.add(CodeWarning(
                line = lineNum,
                column = line.indexOf("document.write("),
                message = "避免使用 document.write()，可能导致页面问题",
                rule = "no-document-write"
            ))
        }
        
        // Check console.log（生产代码警告）
        if (trimmed.contains("console.log(")) {
            warnings.add(CodeWarning(
                line = lineNum,
                column = line.indexOf("console.log("),
                message = "生产代码中建议移除 console.log",
                rule = "no-console"
            ))
        }
    }

    /**
     * CSS 语法检查
     */
    private fun checkCssSyntax(code: String): SyntaxCheckResult {
        val errors = mutableListOf<CodeError>()
        val warnings = mutableListOf<CodeWarning>()
        val lines = code.lines()
        
        var braceCount = 0
        var inComment = false
        
        lines.forEachIndexed { lineIndex, line ->
            val lineNum = lineIndex + 1
            var i = 0
            
            while (i < line.length) {
                val char = line[i]
                val nextChar = if (i + 1 < line.length) line[i + 1] else ' '
                
                // Handle注释
                if (char == '/' && nextChar == '*') {
                    inComment = true
                    i += 2
                    continue
                }
                if (inComment && char == '*' && nextChar == '/') {
                    inComment = false
                    i += 2
                    continue
                }
                
                if (!inComment) {
                    when (char) {
                        '{' -> braceCount++
                        '}' -> braceCount--
                    }
                }
                i++
            }
            
            // Check常见 CSS 问题
            checkCssLineErrors(line, lineNum, errors, warnings)
        }
        
        if (braceCount != 0) {
            errors.add(CodeError(
                line = lines.size,
                column = 0,
                message = "大括号不匹配",
                severity = ErrorSeverity.ERROR
            ))
        }
        
        return SyntaxCheckResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun checkCssLineErrors(line: String, lineNum: Int, errors: MutableList<CodeError>, warnings: MutableList<CodeWarning>) {
        // Check缺少分号
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && 
            !trimmed.endsWith("{") && 
            !trimmed.endsWith("}") && 
            !trimmed.endsWith(",") &&
            !trimmed.startsWith("/*") &&
            !trimmed.endsWith("*/") &&
            !trimmed.startsWith("@") &&
            trimmed.contains(":") &&
            !trimmed.endsWith(";")) {
            warnings.add(CodeWarning(
                line = lineNum,
                column = line.length,
                message = "CSS 属性可能缺少分号",
                rule = "declaration-block-trailing-semicolon"
            ))
        }
        
        // Check !important 滥用
        if (trimmed.contains("!important")) {
            warnings.add(CodeWarning(
                line = lineNum,
                column = line.indexOf("!important"),
                message = "尽量避免过度使用 !important",
                rule = "declaration-no-important"
            ))
        }
    }
    
    /**
     * 代码规范检查
     */
    private suspend fun executeLintCode(args: Map<String, Any?>): Map<String, Any> {
        val code = args["code"] as? String ?: throw IllegalArgumentException("缺少 code 参数")
        val language = args["language"] as? String ?: "javascript"
        
        val syntaxResult = executeSyntaxCheck(args)
        val suggestions = mutableListOf<String>()
        
        // 额外的代码风格建议
        if (language == "javascript") {
            if (!code.contains("'use strict'") && !code.contains("\"use strict\"")) {
                suggestions.add("建议在代码开头添加 'use strict' 启用严格模式")
            }
            if (code.contains("function ") && !code.contains("=>")) {
                suggestions.add("考虑使用箭头函数简化代码")
            }
            if (code.lines().any { it.length > 120 }) {
                suggestions.add("部分行超过 120 字符，建议拆分以提高可读性")
            }
        }
        
        return mapOf(
            "syntax_result" to syntaxResult,
            "suggestions" to suggestions,
            "score" to calculateCodeScore(syntaxResult, suggestions.size)
        )
    }
    
    private fun calculateCodeScore(syntaxResult: SyntaxCheckResult, suggestionCount: Int): Int {
        var score = 100
        score -= syntaxResult.errors.size * 20
        score -= syntaxResult.warnings.size * 5
        score -= suggestionCount * 3
        return maxOf(0, minOf(100, score))
    }

    /**
     * 安全扫描
     */
    private suspend fun executeSecurityScan(args: Map<String, Any?>): SecurityScanResult {
        val code = args["code"] as? String ?: throw IllegalArgumentException("缺少 code 参数")
        val issues = mutableListOf<SecurityIssue>()
        val lines = code.lines()
        
        // Security检查规则
        val securityPatterns = listOf(
            Triple("eval\\s*\\(", "使用 eval() 执行动态代码", "避免使用 eval()，使用更安全的替代方案"),
            Triple("innerHTML\\s*=", "直接设置 innerHTML 可能导致 XSS", "使用 textContent 或 DOMPurify 清理 HTML"),
            Triple("document\\.write\\s*\\(", "document.write 可能被滥用", "使用 DOM API 操作页面"),
            Triple("new\\s+Function\\s*\\(", "动态创建函数存在安全风险", "避免动态创建函数"),
            Triple("location\\s*=|location\\.href\\s*=", "URL 跳转可能被利用", "验证跳转目标 URL"),
            Triple("localStorage|sessionStorage", "存储敏感数据需谨慎", "不要在本地存储中保存敏感信息"),
            Triple("XMLHttpRequest|fetch\\s*\\(", "网络请求需注意安全", "确保请求目标可信，处理 CORS"),
            Triple("postMessage\\s*\\(", "跨窗口通信需验证来源", "验证 message 事件的 origin"),
            Triple("\\$\\{.*\\}", "模板字符串注入风险", "确保插入的变量已经过验证"),
            Triple("atob\\s*\\(|btoa\\s*\\(", "Base64 不是加密", "Base64 仅用于编码，不要用于安全目的")
        )
        
        lines.forEachIndexed { lineIndex, line ->
            val lineNum = lineIndex + 1
            securityPatterns.forEach { (pattern, description, recommendation) ->
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                val match = regex.find(line)
                if (match != null) {
                    val severity = when {
                        pattern.contains("eval") || pattern.contains("innerHTML") -> RiskLevel.HIGH
                        pattern.contains("Function") || pattern.contains("document.write") -> RiskLevel.MEDIUM
                        else -> RiskLevel.LOW
                    }
                    issues.add(SecurityIssue(
                        type = pattern.substringBefore("\\"),
                        description = description,
                        line = lineNum,
                        severity = severity,
                        recommendation = recommendation
                    ))
                }
            }
        }
        
        val riskLevel = when {
            issues.any { it.severity == RiskLevel.CRITICAL } -> RiskLevel.CRITICAL
            issues.any { it.severity == RiskLevel.HIGH } -> RiskLevel.HIGH
            issues.any { it.severity == RiskLevel.MEDIUM } -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        
        return SecurityScanResult(
            safe = issues.none { it.severity == RiskLevel.HIGH || it.severity == RiskLevel.CRITICAL },
            issues = issues,
            riskLevel = riskLevel
        )
    }
    
    /**
     * 修复错误
     */
    private suspend fun executeFixError(args: Map<String, Any?>): Map<String, Any> {
        val code = args["code"] as? String ?: throw IllegalArgumentException("缺少 code 参数")
        val language = args["language"] as? String ?: "javascript"
        
        var fixedCode = code
        val fixes = mutableListOf<String>()
        
        if (language == "javascript") {
            // Auto修复 var -> let
            if (fixedCode.contains("var ")) {
                fixedCode = fixedCode.replace(Regex("\\bvar\\s+"), "let ")
                fixes.add("将 var 替换为 let")
            }
            
            // Auto修复 == -> ===
            fixedCode = fixedCode.replace(Regex("([^=!<>])===([^=])")) { match ->
                match.value // 保持 === 不变
            }
            fixedCode = fixedCode.replace(Regex("([^=!<>])==([^=])")) { match ->
                "${match.groupValues[1]}===${match.groupValues[2]}"
            }
            if (code != fixedCode && fixes.none { it.contains("===") }) {
                fixes.add("将 == 替换为 ===")
            }
        }
        
        return mapOf(
            "original_code" to code,
            "fixed_code" to fixedCode,
            "fixes_applied" to fixes,
            "fix_count" to fixes.size
        )
    }

    /**
     * 获取模板
     */
    private suspend fun executeGetTemplates(args: Map<String, Any?>): List<Map<String, Any>> {
        val category = args["category"] as? String
        val keywords = (args["keywords"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        
        val allTemplates = ModuleTemplates.getAll()
        
        val filtered = allTemplates.filter { template ->
            val matchesCategory = category == null || 
                template.category.name.equals(category, ignoreCase = true) ||
                template.category.getDisplayName().contains(category, ignoreCase = true)
            
            val matchesKeywords = keywords.isEmpty() || keywords.any { keyword ->
                template.name.contains(keyword, ignoreCase = true) ||
                template.description.contains(keyword, ignoreCase = true)
            }
            
            matchesCategory && matchesKeywords
        }
        
        return filtered.take(5).map { template ->
            mapOf(
                "id" to template.id,
                "name" to template.name,
                "description" to template.description,
                "icon" to template.icon,
                "category" to template.category.getDisplayName(),
                "code_preview" to template.code.take(200) + if (template.code.length > 200) "..." else ""
            )
        }
    }
    
    /**
     * 获取代码片段
     */
    private suspend fun executeGetSnippets(args: Map<String, Any?>): List<Map<String, Any>> {
        val query = args["query"] as? String ?: ""
        val category = args["category"] as? String
        
        val results = if (query.isNotBlank()) {
            CodeSnippets.search(query)
        } else if (category != null) {
            CodeSnippets.getByCategory(category)?.snippets ?: emptyList()
        } else {
            CodeSnippets.getPopular()
        }
        
        return results.take(10).map { snippet ->
            mapOf(
                "id" to snippet.id,
                "name" to snippet.name,
                "description" to snippet.description,
                "code" to snippet.code,
                "tags" to snippet.tags
            )
        }
    }
    
    /**
     * 验证配置
     */
    private suspend fun executeValidateConfig(args: Map<String, Any?>): ModuleValidationResult {
        val configItems = args["config_items"] as? List<*> ?: emptyList<Any>()
        val configValues = args["config_values"] as? Map<*, *> ?: emptyMap<String, Any>()
        val issues = mutableListOf<ValidationIssue>()
        
        configItems.forEach { item ->
            if (item is Map<*, *>) {
                val key = item["key"] as? String
                val name = item["name"] as? String
                val required = item["required"] as? Boolean ?: false
                
                if (key.isNullOrBlank()) {
                    issues.add(ValidationIssue(
                        field = "config_item",
                        message = "配置项缺少 key",
                        severity = ErrorSeverity.ERROR
                    ))
                }
                
                if (name.isNullOrBlank()) {
                    issues.add(ValidationIssue(
                        field = "config_item.$key",
                        message = "配置项缺少显示名称",
                        severity = ErrorSeverity.WARNING
                    ))
                }
                
                if (required && key != null && !configValues.containsKey(key)) {
                    issues.add(ValidationIssue(
                        field = key,
                        message = "必填配置项 '$name' 未设置值",
                        severity = ErrorSeverity.ERROR
                    ))
                }
            }
        }
        
        return ModuleValidationResult(
            valid = issues.none { it.severity == ErrorSeverity.ERROR },
            issues = issues
        )
    }
    
    /**
     * 创建模块
     */
    private suspend fun executeCreateModule(args: Map<String, Any?>): Map<String, Any> = withContext(Dispatchers.IO) {
        val name = args["name"] as? String ?: throw IllegalArgumentException("缺少模块名称")
        val description = args["description"] as? String ?: ""
        val icon = args["icon"] as? String ?: "📦"
        val categoryStr = args["category"] as? String ?: "OTHER"
        val jsCode = args["js_code"] as? String ?: ""
        val cssCode = args["css_code"] as? String ?: ""
        val runAtStr = args["run_at"] as? String ?: "DOCUMENT_END"
        
        val category = try {
            ModuleCategory.valueOf(categoryStr.uppercase())
        } catch (e: Exception) {
            ModuleCategory.OTHER
        }
        
        val runAt = try {
            ModuleRunTime.valueOf(runAtStr.uppercase())
        } catch (e: Exception) {
            ModuleRunTime.DOCUMENT_END
        }
        
        val module = ExtensionModule(
            name = name,
            description = description,
            icon = icon,
            category = category,
            code = jsCode,
            cssCode = cssCode,
            runAt = runAt,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            enabled = true,
            builtIn = false
        )
        
        val result = extensionManager.addModule(module)
        
        if (result.isSuccess) {
            mapOf(
                "success" to true,
                "module_id" to module.id,
                "message" to "模块创建成功"
            )
        } else {
            mapOf(
                "success" to false,
                "error" to (result.exceptionOrNull()?.message ?: "创建失败")
            )
        }
    }
}
