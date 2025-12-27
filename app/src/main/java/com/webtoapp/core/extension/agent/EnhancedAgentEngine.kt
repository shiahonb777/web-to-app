package com.webtoapp.core.extension.agent

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.StreamEvent
import com.webtoapp.core.extension.*
import com.webtoapp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 增强版 Agent 引擎
 * 
 * 核心 Agent 实现，支持：
 * - 流式输出 (developWithStream)
 * - ReAct 循环
 * - 工具链调用
 * - 自动修复（最多3次，使用迭代而非递归）
 * - 上下文保持
 * - 超时保护
 * 
 * Requirements: 2.1, 2.6, 5.2
 */
class EnhancedAgentEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedAgentEngine"
        private const val STREAM_TIMEOUT_MS = 120_000L  // 2分钟超时
        private const val MAX_FIX_ATTEMPTS = 3
    }
    
    private val gson = Gson()
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    private val toolExecutor = AgentToolExecutor(context)
    
    // 工作记忆
    val workingMemory = AgentWorkingMemory()
    
    // 当前状态
    private val _currentState = MutableStateFlow(AgentState.IDLE)
    val currentState: StateFlow<AgentState> = _currentState.asStateFlow()
    
    /**
     * 使用流式输出进行模块开发
     * 
     * @param requirement 用户需求描述
     * @param model 指定使用的模型（可选）
     * @param category 模块分类（可选）
     * @param existingCode 现有代码（用于修改）
     * @return Flow<AgentStreamEvent> 流式事件流
     */
    fun developWithStream(
        requirement: String,
        model: SavedModel? = null,
        category: ModuleCategory? = null,
        existingCode: String? = null
    ): Flow<AgentStreamEvent> = flow {
        // 初始化工作记忆
        workingMemory.currentRequirement = requirement
        workingMemory.addUserMessage(requirement)
        workingMemory.resetFixAttempts()  // 重置修复计数
        
        try {
            // 获取 AI 配置
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            if (apiKeys.isEmpty()) {
                emit(AgentStreamEvent.Error("请先在 AI 设置中配置 API Key", code = "NO_API_KEY"))
                return@flow
            }
            
            // 选择模型
            val selectedModel = selectModel(model, savedModels)
            if (selectedModel == null) {
                emit(AgentStreamEvent.Error("请先在 AI 设置中添加并保存模型", code = "NO_MODEL"))
                return@flow
            }
            
            val apiKey = apiKeys.find { it.id == selectedModel.apiKeyId }
            if (apiKey == null) {
                emit(AgentStreamEvent.Error("找不到模型对应的 API Key", code = "NO_API_KEY_FOR_MODEL"))
                return@flow
            }
            
            // 开始开发流程
            emit(AgentStreamEvent.StateChange(AgentState.THINKING))
            _currentState.value = AgentState.THINKING
            
            // 构建系统提示词和消息
            val systemPrompt = buildSystemPrompt(category, existingCode)
            val messages = buildMessages(systemPrompt, requirement, category, existingCode)
            
            // 使用流式 API 调用（带超时保护）
            emit(AgentStreamEvent.StateChange(AgentState.GENERATING))
            _currentState.value = AgentState.GENERATING
            
            val contentBuilder = StringBuilder()
            val thinkingBuilder = StringBuilder()
            var streamCompleted = false
            
            try {
                withTimeout(STREAM_TIMEOUT_MS) {
                    aiClient.chatStream(apiKey, selectedModel.model, messages)
                        .collect { event ->
                            when (event) {
                                is StreamEvent.Started -> {
                                    Log.d(TAG, "Stream started")
                                }
                                is StreamEvent.Thinking -> {
                                    thinkingBuilder.append(event.content)
                                    emit(AgentStreamEvent.Thinking(event.content, thinkingBuilder.toString()))
                                }
                                is StreamEvent.Content -> {
                                    contentBuilder.clear()
                                    contentBuilder.append(event.accumulated)
                                    emit(AgentStreamEvent.Content(event.delta, event.accumulated))
                                }
                                is StreamEvent.Done -> {
                                    streamCompleted = true
                                    Log.d(TAG, "Stream done, content length: ${event.fullContent.length}")
                                }
                                is StreamEvent.Error -> {
                                    throw Exception(event.message)
                                }
                            }
                        }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Stream timeout after ${STREAM_TIMEOUT_MS}ms")
                emit(AgentStreamEvent.Error(
                    message = "请求超时，请检查网络连接后重试",
                    code = "TIMEOUT",
                    recoverable = true,
                    rawResponse = contentBuilder.toString().takeIf { it.isNotEmpty() }
                ))
                return@flow
            }
            
            if (!streamCompleted || contentBuilder.isEmpty()) {
                emit(AgentStreamEvent.Error(
                    message = "AI 响应为空，请重试",
                    code = "EMPTY_RESPONSE",
                    recoverable = true
                ))
                return@flow
            }
            
            // 流完成，处理生成的内容
            val responseText = contentBuilder.toString()
            processGeneratedContentIterative(responseText, apiKey, selectedModel, category)
                .collect { agentEvent -> emit(agentEvent) }
            
        } catch (e: CancellationException) {
            throw e  // 重新抛出取消异常
        } catch (e: Exception) {
            Log.e(TAG, "Error in developWithStream", e)
            emit(AgentStreamEvent.StateChange(AgentState.ERROR))
            _currentState.value = AgentState.ERROR
            workingMemory.lastError = e.message
            emit(AgentStreamEvent.Error(
                message = e.message ?: "未知错误",
                recoverable = true
            ))
        }
    }.flowOn(Dispatchers.IO)

    
    /**
     * 处理生成的内容（迭代版本，避免递归 Flow 问题）
     * 解析模块、执行语法检查、自动修复
     */
    private fun processGeneratedContentIterative(
        responseText: String,
        apiKey: ApiKeyConfig,
        savedModel: SavedModel,
        category: ModuleCategory?
    ): Flow<AgentStreamEvent> = flow {
        // 解析生成的模块
        val parsedModule = parseGeneratedModule(responseText)
        if (parsedModule == null) {
            emit(AgentStreamEvent.Error(
                message = "无法解析 AI 生成的代码",
                rawResponse = responseText
            ))
            return@flow
        }
        
        var currentModule: GeneratedModuleData = parsedModule
        workingMemory.updateModule(currentModule)
        emit(AgentStreamEvent.ModuleGenerated(currentModule))
        
        // 语法检查和自动修复循环（迭代而非递归）
        var fixAttempt = 0
        var syntaxValid = false
        
        while (fixAttempt <= MAX_FIX_ATTEMPTS && !syntaxValid) {
            // 执行语法检查
            emit(AgentStreamEvent.StateChange(AgentState.SYNTAX_CHECKING))
            _currentState.value = AgentState.SYNTAX_CHECKING
            
            val syntaxCheckRequest = ToolCallRequest(
                toolName = "syntax_check",
                arguments = mapOf("code" to currentModule.jsCode, "language" to "javascript")
            )
            
            val syntaxToolInfo = ToolCallInfo.fromRequest(syntaxCheckRequest)
                .copy(status = ToolStatus.EXECUTING)
            emit(AgentStreamEvent.ToolStart(syntaxToolInfo))
            workingMemory.recordToolCall(syntaxToolInfo)
            
            val syntaxResult = toolExecutor.execute(syntaxCheckRequest)
            val completedSyntaxInfo = ToolCallInfo.fromResult(syntaxToolInfo, syntaxResult)
            emit(AgentStreamEvent.ToolComplete(completedSyntaxInfo))
            workingMemory.updateToolCallResult(syntaxToolInfo.callId, syntaxResult)
            
            val syntaxCheck = syntaxResult.result as? SyntaxCheckResult
            
            if (syntaxCheck == null || syntaxCheck.valid) {
                syntaxValid = true
                Log.d(TAG, "Syntax check passed")
            } else {
                // 语法有错误，尝试修复
                fixAttempt++
                
                if (fixAttempt > MAX_FIX_ATTEMPTS) {
                    // 达到最大修复次数
                    val errorMessage = buildAutoFixLimitErrorMessage(syntaxCheck)
                    emit(AgentStreamEvent.Error(
                        message = errorMessage,
                        code = "MAX_FIX_ATTEMPTS_REACHED",
                        recoverable = true
                    ))
                    break
                }
                
                Log.d(TAG, "Syntax errors found, attempting fix $fixAttempt/$MAX_FIX_ATTEMPTS")
                
                // 尝试修复
                emit(AgentStreamEvent.StateChange(AgentState.FIXING))
                _currentState.value = AgentState.FIXING
                
                val fixedModule = tryFixSyntaxErrors(currentModule, syntaxCheck, apiKey, savedModel, fixAttempt)
                
                if (fixedModule != null) {
                    currentModule = fixedModule
                    workingMemory.updateModule(currentModule)
                    emit(AgentStreamEvent.ModuleGenerated(currentModule))
                } else {
                    // 修复失败
                    emit(AgentStreamEvent.Error(
                        message = "自动修复失败，请手动检查代码",
                        code = "AUTO_FIX_FAILED",
                        recoverable = true
                    ))
                    break
                }
            }
        }
        
        // 执行安全扫描
        emit(AgentStreamEvent.StateChange(AgentState.SECURITY_SCANNING))
        _currentState.value = AgentState.SECURITY_SCANNING
        
        val securityRequest = ToolCallRequest(
            toolName = "security_scan",
            arguments = mapOf("code" to currentModule.jsCode)
        )
        
        val securityToolInfo = ToolCallInfo.fromRequest(securityRequest)
            .copy(status = ToolStatus.EXECUTING)
        emit(AgentStreamEvent.ToolStart(securityToolInfo))
        workingMemory.recordToolCall(securityToolInfo)
        
        val securityResult = toolExecutor.execute(securityRequest)
        val completedSecurityInfo = ToolCallInfo.fromResult(securityToolInfo, securityResult)
        emit(AgentStreamEvent.ToolComplete(completedSecurityInfo))
        workingMemory.updateToolCallResult(securityToolInfo.callId, securityResult)
        
        val securityScan = securityResult.result as? SecurityScanResult
        val finalModule = currentModule.copy(
            securitySafe = securityScan?.safe ?: true
        )
        workingMemory.updateModule(finalModule)
        
        // 完成
        emit(AgentStreamEvent.StateChange(AgentState.COMPLETED))
        _currentState.value = AgentState.COMPLETED
        
        // 保存到对话历史
        workingMemory.addAssistantMessage(
            content = "已成功生成模块「${finalModule.name}」",
            generatedModule = finalModule
        )
        
        emit(AgentStreamEvent.Completed(finalModule))
    }
    
    /**
     * 尝试修复语法错误（单次修复，不递归）
     * 
     * @return 修复后的模块，如果修复失败返回 null
     */
    private suspend fun tryFixSyntaxErrors(
        module: GeneratedModuleData,
        syntaxResult: SyntaxCheckResult,
        apiKey: ApiKeyConfig,
        savedModel: SavedModel,
        attemptNumber: Int
    ): GeneratedModuleData? {
        val errorMessages = syntaxResult.errors.joinToString("\n") { error ->
            "- 第 ${error.line} 行, 第 ${error.column} 列: ${error.message}" +
                (error.suggestion?.let { "\n  建议: $it" } ?: "")
        }
        
        val fixPrompt = """
请修复以下 JavaScript 代码中的语法错误（第 $attemptNumber/$MAX_FIX_ATTEMPTS 次尝试）：

**错误列表**：
$errorMessages

**原始代码**：
```javascript
${module.jsCode}
```

请只输出修复后的完整代码，使用 ```javascript 代码块包裹。
不要添加任何解释，只输出代码。
        """.trimIndent()
        
        val messages = listOf(
            mapOf("role" to "system", "content" to "你是一个 JavaScript 代码修复专家。请修复代码中的语法错误，保持原有功能不变。只输出修复后的代码，不要添加任何解释。"),
            mapOf("role" to "user", "content" to fixPrompt)
        )
        
        return try {
            val response = withTimeout(60_000) {
                aiClient.chat(apiKey, savedModel.model, messages)
            }
            
            if (response.isSuccess) {
                val fixedCode = response.getOrNull() ?: return null
                
                // 提取代码块
                val codePattern = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)\\s*```")
                val code = codePattern.find(fixedCode)?.groupValues?.get(1) ?: fixedCode
                
                if (code.isBlank()) return null
                
                module.copy(jsCode = code.trim())
            } else {
                Log.e(TAG, "Fix request failed: ${response.exceptionOrNull()?.message}")
                null
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Fix request timeout")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Fix request error", e)
            null
        }
    }
    
    /**
     * 构建自动修复达到限制的错误消息
     */
    private fun buildAutoFixLimitErrorMessage(syntaxResult: SyntaxCheckResult): String {
        val errorSummary = syntaxResult.errors.take(3).joinToString("\n") { error ->
            "  - 第 ${error.line} 行: ${error.message}"
        }
        val moreErrors = if (syntaxResult.errors.size > 3) {
            "\n  ... 还有 ${syntaxResult.errors.size - 3} 个错误"
        } else ""
        
        return """
已达到最大自动修复次数 (${MAX_FIX_ATTEMPTS}次)，代码仍有语法错误，请手动修复：
$errorSummary$moreErrors
        """.trimIndent()
    }
    
    /**
     * 使用工具链执行语法检查和自动修复
     * 
     * 这是一个更高级的自动修复方法，使用 AgentToolExecutor 的工具链功能
     * 
     * Requirements: 5.3, 5.4, 5.5
     */
    fun performSyntaxCheckAndAutoFix(
        code: String,
        language: String = "javascript"
    ): Flow<AgentStreamEvent> = flow {
        emit(AgentStreamEvent.StateChange(AgentState.SYNTAX_CHECKING))
        _currentState.value = AgentState.SYNTAX_CHECKING
        
        toolExecutor.executeSyntaxCheckAndFixChain(
            code = code,
            language = language,
            maxFixAttempts = workingMemory.maxFixAttempts
        ).collect { chainEvent ->
            when (chainEvent) {
                is ToolChainEvent.ChainStarted -> {
                    // 链开始，不需要特殊处理
                }
                is ToolChainEvent.ToolStarted -> {
                    val toolInfo = ToolCallInfo.fromRequest(chainEvent.request)
                        .copy(status = ToolStatus.EXECUTING)
                    emit(AgentStreamEvent.ToolStart(toolInfo))
                    workingMemory.recordToolCall(toolInfo)
                }
                is ToolChainEvent.ToolCompleted -> {
                    val toolInfo = ToolCallInfo(
                        toolName = chainEvent.result.toolName,
                        callId = chainEvent.result.callId,
                        status = if (chainEvent.result.success) ToolStatus.SUCCESS else ToolStatus.FAILED,
                        result = chainEvent.result.result,
                        error = chainEvent.result.error,
                        executionTimeMs = chainEvent.result.executionTimeMs
                    )
                    emit(AgentStreamEvent.ToolComplete(toolInfo))
                }
                is ToolChainEvent.ChainCompleted -> {
                    emit(AgentStreamEvent.StateChange(AgentState.COMPLETED))
                    _currentState.value = AgentState.COMPLETED
                }
                is ToolChainEvent.ChainFailed -> {
                    emit(AgentStreamEvent.StateChange(AgentState.ERROR))
                    _currentState.value = AgentState.ERROR
                    emit(AgentStreamEvent.Error(
                        message = chainEvent.error,
                        code = "TOOL_CHAIN_FAILED",
                        recoverable = true
                    ))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 选择模型
     */
    private suspend fun selectModel(
        preferredModel: SavedModel?,
        savedModels: List<SavedModel>
    ): SavedModel? {
        // 如果指定了模型，直接使用
        if (preferredModel != null) {
            return preferredModel
        }
        
        // 优先使用支持模块开发功能的模型
        val moduleDevModels = savedModels.filter { it.supportsFeature(AiFeature.MODULE_DEVELOPMENT) }
        val defaultModelId = aiConfigManager.defaultModelIdFlow.first()
        
        return moduleDevModels.find { it.id == defaultModelId }
            ?: moduleDevModels.firstOrNull()
            ?: savedModels.find { it.id == defaultModelId }
            ?: savedModels.firstOrNull()
    }

    
    /**
     * 构建系统提示词
     */
    private fun buildSystemPrompt(category: ModuleCategory?, existingCode: String?): String {
        val categoryHint = category?.let {
            """
## 目标分类
用户希望创建「${it.displayName}」类型的模块。
分类说明：${it.description}
            """.trimIndent()
        } ?: ""
        
        val existingCodeHint = existingCode?.let {
            """
## 现有代码
用户提供了现有代码，请在此基础上进行修改或优化：
```javascript
$it
```
            """.trimIndent()
        } ?: ""
        
        // 获取 NativeBridge API 文档
        val nativeBridgeApi = com.webtoapp.core.webview.NativeBridge.getApiDocumentation()
        
        return """
你是一个专业的 WebToApp 扩展模块开发专家。你的任务是根据用户需求生成高质量的扩展模块代码。

## 扩展模块系统说明
WebToApp 扩展模块是注入到网页中执行的 JavaScript/CSS 代码，类似于浏览器扩展或油猴脚本。
模块会在 WebView 加载网页时自动注入执行。

## 可用的内置 API

### 模块配置 API
```javascript
// 获取用户配置值
getConfig(key: string, defaultValue: any): any

// 模块信息对象
__MODULE_INFO__ = { id: string, name: string, version: string }

// 用户配置值对象
__MODULE_CONFIG__ = { [key: string]: any }
```

$nativeBridgeApi

## 代码规范要求
1. 使用 'use strict' 严格模式
2. 代码已被包装在 IIFE 中，无需再次包装
3. 使用 const/let 而非 var
4. 使用 === 而非 ==
5. 添加适当的错误处理 try-catch
6. 使用 MutationObserver 监听动态内容
7. 避免使用 eval、document.write 等不安全函数
8. 添加清晰的注释说明
9. 优先使用 NativeBridge API 实现原生功能（如保存图片、分享、震动等）

## 模块分类
可用分类：CONTENT_FILTER(内容过滤), CONTENT_ENHANCE(内容增强), STYLE_MODIFIER(样式修改), 
THEME(主题美化), FUNCTION_ENHANCE(功能增强), AUTOMATION(自动化), NAVIGATION(导航辅助),
DATA_EXTRACT(数据提取), MEDIA(媒体处理), VIDEO(视频增强), IMAGE(图片处理), 
SECURITY(安全隐私), DEVELOPER(开发调试), OTHER(其他)

## 执行时机
- DOCUMENT_START: DOM 未就绪时执行，适合拦截请求
- DOCUMENT_END: DOM 加载完成后执行（推荐）
- DOCUMENT_IDLE: 页面完全加载后执行

$categoryHint

$existingCodeHint

## 输出格式要求
请严格按照以下 JSON 格式输出，不要添加任何其他内容：

```json
{
  "name": "模块名称（简洁明了）",
  "description": "模块功能描述（一句话说明）",
  "icon": "适合的emoji图标",
  "category": "分类名称（如 CONTENT_FILTER）",
  "run_at": "执行时机（如 DOCUMENT_END）",
  "js_code": "JavaScript代码（转义后的字符串）",
  "css_code": "CSS代码（如果需要，否则为空字符串）",
  "config_items": [
    {
      "key": "配置键名",
      "name": "显示名称",
      "description": "配置说明",
      "type": "TEXT|NUMBER|BOOLEAN|SELECT|TEXTAREA",
      "defaultValue": "默认值",
      "options": ["选项1", "选项2"]
    }
  ],
  "url_matches": ["匹配的URL模式，如 *://*.example.com/*"]
}
```

## 重要提示
1. js_code 中的代码必须是可直接执行的，不需要 IIFE 包装
2. 字符串中的特殊字符需要正确转义
3. 如果用户没有指定 URL 匹配规则，url_matches 留空数组表示匹配所有网站
4. config_items 用于让用户自定义模块行为，如果不需要配置项则留空数组
5. 当需要保存图片/视频、分享、复制、震动等原生功能时，使用 NativeBridge API
        """.trimIndent()
    }

    /**
     * 构建消息列表
     */
    private fun buildMessages(
        systemPrompt: String,
        requirement: String,
        category: ModuleCategory?,
        existingCode: String?
    ): List<Map<String, String>> {
        val messages = mutableListOf<Map<String, String>>()
        
        // 系统消息
        messages.add(mapOf("role" to "system", "content" to systemPrompt))
        
        // 添加对话历史（保持上下文）
        workingMemory.getContextForAi()
            .filter { it["role"] != "system" }
            .forEach { messages.add(it) }
        
        // 如果对话历史为空，添加用户消息
        if (workingMemory.conversationHistory.isEmpty()) {
            val userMessage = buildString {
                append("请根据以下需求开发一个扩展模块：\n\n")
                append("**需求描述**：$requirement\n")
                
                if (category != null) {
                    append("\n**目标分类**：${category.displayName}\n")
                }
                
                if (!existingCode.isNullOrBlank()) {
                    append("\n**现有代码**（请在此基础上修改）：\n```javascript\n$existingCode\n```\n")
                }
                
                append("\n请生成完整的模块代码，并确保代码质量和安全性。")
            }
            
            messages.add(mapOf("role" to "user", "content" to userMessage))
        }
        
        return messages
    }
    
    /**
     * 解析生成的模块
     */
    private fun parseGeneratedModule(response: String): GeneratedModuleData? {
        return try {
            // 尝试提取 JSON 块
            val jsonPattern = Regex("```json\\s*([\\s\\S]*?)\\s*```")
            val jsonMatch = jsonPattern.find(response)
            
            val jsonStr = if (jsonMatch != null) {
                jsonMatch.groupValues[1]
            } else {
                // 尝试直接解析
                response.trim()
            }
            
            val json = JsonParser.parseString(jsonStr).asJsonObject
            
            GeneratedModuleData(
                name = json.get("name")?.asString ?: "AI 生成模块",
                description = json.get("description")?.asString ?: "",
                icon = json.get("icon")?.asString ?: "🤖",
                category = json.get("category")?.asString ?: "OTHER",
                jsCode = json.get("js_code")?.asString ?: json.get("jsCode")?.asString ?: "",
                cssCode = json.get("css_code")?.asString ?: json.get("cssCode")?.asString ?: "",
                configItems = parseConfigItems(json),
                urlMatches = json.getAsJsonArray("url_matches")?.map { it.asString } ?: emptyList(),
                runAt = json.get("run_at")?.asString ?: json.get("runAt")?.asString ?: "DOCUMENT_END"
            )
        } catch (e: Exception) {
            // 尝试从纯代码响应中提取
            extractCodeFromResponse(response)
        }
    }
    
    /**
     * 解析配置项
     */
    private fun parseConfigItems(json: com.google.gson.JsonObject): List<Map<String, Any>> {
        val items = json.getAsJsonArray("config_items") ?: json.getAsJsonArray("configItems")
        return items?.mapNotNull { item ->
            try {
                val obj = item.asJsonObject
                mapOf(
                    "key" to (obj.get("key")?.asString ?: ""),
                    "name" to (obj.get("name")?.asString ?: ""),
                    "description" to (obj.get("description")?.asString ?: ""),
                    "type" to (obj.get("type")?.asString ?: "TEXT"),
                    "defaultValue" to (obj.get("defaultValue")?.asString ?: obj.get("default_value")?.asString ?: ""),
                    "options" to (obj.getAsJsonArray("options")?.map { it.asString } ?: emptyList<String>())
                )
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }
    
    /**
     * 从纯代码响应中提取
     */
    private fun extractCodeFromResponse(response: String): GeneratedModuleData? {
        val jsPattern = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)\\s*```")
        val cssPattern = Regex("```css\\s*([\\s\\S]*?)\\s*```")
        
        val jsCode = jsPattern.find(response)?.groupValues?.get(1)
        val cssCode = cssPattern.find(response)?.groupValues?.get(1) ?: ""
        
        if (jsCode.isNullOrBlank()) {
            return null
        }
        
        return GeneratedModuleData(
            name = "AI 生成模块",
            description = "由 AI 根据需求生成的扩展模块",
            icon = "🤖",
            category = "OTHER",
            jsCode = jsCode,
            cssCode = cssCode
        )
    }
    
    /**
     * 执行工具链
     * 按顺序执行多个工具调用
     */
    fun executeToolChain(
        tools: List<ToolCallRequest>
    ): Flow<AgentStreamEvent> = flow {
        for (request in tools) {
            val toolInfo = ToolCallInfo.fromRequest(request)
                .copy(status = ToolStatus.EXECUTING)
            emit(AgentStreamEvent.ToolStart(toolInfo))
            workingMemory.recordToolCall(toolInfo)
            
            val result = toolExecutor.execute(request)
            val completedInfo = ToolCallInfo.fromResult(toolInfo, result)
            emit(AgentStreamEvent.ToolComplete(completedInfo))
            workingMemory.updateToolCallResult(toolInfo.callId, result)
            
            // 如果工具执行失败，停止链式执行
            if (!result.success) {
                emit(AgentStreamEvent.Error(
                    message = "工具 ${request.toolName} 执行失败: ${result.error}",
                    recoverable = true
                ))
                break
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 重置引擎状态
     */
    fun reset() {
        workingMemory.reset()
        _currentState.value = AgentState.IDLE
    }
}
