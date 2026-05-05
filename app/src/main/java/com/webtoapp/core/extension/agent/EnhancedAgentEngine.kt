package com.webtoapp.core.extension.agent

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.google.gson.JsonParser
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.StreamEvent
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.i18n.AiPromptManager
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.LanguageManager
import com.webtoapp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*














class EnhancedAgentEngine(private val context: Context) {

    companion object {
        private const val TAG = "EnhancedAgentEngine"
        private const val STREAM_TIMEOUT_MS = 120_000L
        private const val MAX_FIX_ATTEMPTS = 3


        private val JSON_BLOCK_REGEX = Regex("```json\\s*([\\s\\S]*?)\\s*```")
        private val JS_BLOCK_REGEX = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)\\s*```")
        private val CSS_BLOCK_REGEX = Regex("```css\\s*([\\s\\S]*?)\\s*```")
    }

    private val gson = com.webtoapp.util.GsonProvider.gson
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    private val toolExecutor = AgentToolExecutor(context)


    val workingMemory = AgentWorkingMemory()


    private val _currentState = MutableStateFlow(AgentState.IDLE)
    val currentState: StateFlow<AgentState> = _currentState.asStateFlow()










    fun developWithStream(
        requirement: String,
        model: SavedModel? = null,
        category: ModuleCategory? = null,
        existingCode: String? = null
    ): Flow<AgentStreamEvent> = flow {

        workingMemory.currentRequirement = requirement
        workingMemory.addUserMessage(requirement)
        workingMemory.resetFixAttempts()

        try {

            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()

            if (apiKeys.isEmpty()) {
                emit(AgentStreamEvent.Error(Strings.aiErrorNoApiKey, code = "NO_API_KEY"))
                return@flow
            }


            val selectedModel = selectModel(model, savedModels)
            if (selectedModel == null) {
                emit(AgentStreamEvent.Error(Strings.aiErrorNoModel, code = "NO_MODEL"))
                return@flow
            }

            val apiKey = apiKeys.find { it.id == selectedModel.apiKeyId }
            if (apiKey == null) {
                emit(AgentStreamEvent.Error(Strings.aiErrorNoApiKeyForModel, code = "NO_API_KEY_FOR_MODEL"))
                return@flow
            }


            emit(AgentStreamEvent.StateChange(AgentState.THINKING))
            _currentState.value = AgentState.THINKING


            val systemPrompt = buildSystemPrompt(category, existingCode)
            val messages = buildMessages(systemPrompt, requirement, category, existingCode)


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
                                    AppLogger.w(TAG, "Stream started")
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
                                    AppLogger.w(TAG, "Stream done, content length: ${event.fullContent.length}")
                                }
                                is StreamEvent.Error -> {
                                    throw Exception(event.message)
                                }
                            }
                        }
                }
            } catch (e: TimeoutCancellationException) {
                AppLogger.e(TAG, "Stream timeout after ${STREAM_TIMEOUT_MS}ms")
                emit(AgentStreamEvent.Error(
                    message = Strings.agentRequestTimeout,
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


            val responseText = contentBuilder.toString()
            processGeneratedContentIterative(responseText, apiKey, selectedModel, category)
                .collect { agentEvent -> emit(agentEvent) }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error in developWithStream", e)
            emit(AgentStreamEvent.StateChange(AgentState.ERROR))
            _currentState.value = AgentState.ERROR
            workingMemory.lastError = e.message
            emit(AgentStreamEvent.Error(
                message = e.message ?: Strings.aiErrorUnknown,
                recoverable = true
            ))
        }
    }.flowOn(Dispatchers.IO)






    private fun processGeneratedContentIterative(
        responseText: String,
        apiKey: ApiKeyConfig,
        savedModel: SavedModel,
        category: ModuleCategory?
    ): Flow<AgentStreamEvent> = flow {

        val parsedModule = parseGeneratedModule(responseText)
        if (parsedModule == null) {
            emit(AgentStreamEvent.Error(
                message = Strings.agentParseFailed,
                rawResponse = responseText
            ))
            return@flow
        }

        var currentModule: GeneratedModuleData = parsedModule
        workingMemory.updateModule(currentModule)
        emit(AgentStreamEvent.ModuleGenerated(currentModule))


        var fixAttempt = 0
        var syntaxValid = false

        while (fixAttempt <= MAX_FIX_ATTEMPTS && !syntaxValid) {

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
                AppLogger.w(TAG, "Syntax check passed")
            } else {

                fixAttempt++

                if (fixAttempt > MAX_FIX_ATTEMPTS) {

                    val errorMessage = buildAutoFixLimitErrorMessage(syntaxCheck)
                    emit(AgentStreamEvent.Error(
                        message = errorMessage,
                        code = "MAX_FIX_ATTEMPTS_REACHED",
                        recoverable = true
                    ))
                    break
                }

                AppLogger.w(TAG, "Syntax errors found, attempting fix $fixAttempt/$MAX_FIX_ATTEMPTS")


                emit(AgentStreamEvent.StateChange(AgentState.FIXING))
                _currentState.value = AgentState.FIXING

                val fixedModule = tryFixSyntaxErrors(currentModule, syntaxCheck, apiKey, savedModel, fixAttempt)

                if (fixedModule != null) {
                    currentModule = fixedModule
                    workingMemory.updateModule(currentModule)
                    emit(AgentStreamEvent.ModuleGenerated(currentModule))
                } else {

                    emit(AgentStreamEvent.Error(
                        message = Strings.agentAutoFixFailed,
                        code = "AUTO_FIX_FAILED",
                        recoverable = true
                    ))
                    break
                }
            }
        }


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


        emit(AgentStreamEvent.StateChange(AgentState.COMPLETED))
        _currentState.value = AgentState.COMPLETED


        workingMemory.addAssistantMessage(
            content = Strings.agentModuleGenerated.replace("%s", finalModule.name),
            generatedModule = finalModule
        )

        emit(AgentStreamEvent.Completed(finalModule))
    }






    private suspend fun tryFixSyntaxErrors(
        module: GeneratedModuleData,
        syntaxResult: SyntaxCheckResult,
        apiKey: ApiKeyConfig,
        savedModel: SavedModel,
        attemptNumber: Int
    ): GeneratedModuleData? {

        val languageManager = LanguageManager.getInstance(context)
        val currentLanguage = languageManager.getCurrentLanguage()

        val errorMessages = syntaxResult.errors.joinToString("\n") { error ->
            "- Line ${error.line}, Column ${error.column}: ${error.message}" +
                (error.suggestion?.let { "\n  Suggestion: $it" } ?: "")
        }

        val fixPrompt = AiPromptManager.getCodeFixPrompt(
            language = currentLanguage,
            errorMessages = errorMessages,
            code = module.jsCode,
            attempt = attemptNumber,
            maxAttempts = MAX_FIX_ATTEMPTS
        )

        val systemPrompt = AiPromptManager.getCodeFixSystemPrompt(currentLanguage)

        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to fixPrompt)
        )

        return try {
            val response = withTimeout(60_000) {
                aiClient.chat(apiKey, savedModel.model, messages)
            }

            if (response.isSuccess) {
                val fixedCode = response.getOrNull() ?: return null


                val code = JS_BLOCK_REGEX.find(fixedCode)?.groupValues?.get(1) ?: fixedCode

                if (code.isBlank()) return null

                module.copy(jsCode = code.trim())
            } else {
                AppLogger.e(TAG, "Fix request failed: ${response.exceptionOrNull()?.message}")
                null
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.e(TAG, "Fix request timeout")
            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Fix request error", e)
            null
        }
    }




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




    private suspend fun selectModel(
        preferredModel: SavedModel?,
        savedModels: List<SavedModel>
    ): SavedModel? {

        if (preferredModel != null) {
            return preferredModel
        }


        val moduleDevModels = savedModels.filter { it.supportsFeature(AiFeature.MODULE_DEVELOPMENT) }
        val defaultModelId = aiConfigManager.defaultModelIdFlow.first()

        return moduleDevModels.find { it.id == defaultModelId }
            ?: moduleDevModels.firstOrNull()
            ?: savedModels.find { it.id == defaultModelId }
            ?: savedModels.firstOrNull()
    }





    private suspend fun buildSystemPrompt(category: ModuleCategory?, existingCode: String?): String {

        val languageManager = LanguageManager.getInstance(context)
        val currentLanguage = languageManager.getCurrentLanguage()

        val categoryHint = category?.let {
            when (currentLanguage) {
                AppLanguage.CHINESE -> """
## 目标分类
用户希望创建「${it.getDisplayName()}」类型的模块。
分类说明：${it.getDescription()}
                """.trimIndent()
                AppLanguage.ENGLISH -> """
## Target Category
User wants to create a "${it.getDisplayName()}" type module.
Category description: ${it.getDescription()}
                """.trimIndent()
                AppLanguage.ARABIC -> """
## الفئة المستهدفة
يريد المستخدم إنشاء وحدة من نوع "${it.getDisplayName()}".
وصف الفئة: ${it.getDescription()}
                """.trimIndent()
            }
        } ?: ""

        val existingCodeHint = existingCode?.let {
            when (currentLanguage) {
                AppLanguage.CHINESE -> """
## 现有代码
用户提供了现有代码，请在此基础上进行修改或优化：
```javascript
$it
```
                """.trimIndent()
                AppLanguage.ENGLISH -> """
## Existing Code
User provided existing code, please modify or optimize based on this:
```javascript
$it
```
                """.trimIndent()
                AppLanguage.ARABIC -> """
## الكود الحالي
قدم المستخدم كودًا موجودًا، يرجى التعديل أو التحسين بناءً على هذا:
```javascript
$it
```
                """.trimIndent()
            }
        } ?: ""


        val nativeBridgeApi = com.webtoapp.core.webview.NativeBridge.getApiDocumentation()

        return AiPromptManager.getModuleDevelopmentSystemPrompt(
            language = currentLanguage,
            categoryHint = categoryHint,
            existingCodeHint = existingCodeHint,
            nativeBridgeApi = nativeBridgeApi
        )
    }




    private suspend fun buildMessages(
        systemPrompt: String,
        requirement: String,
        category: ModuleCategory?,
        existingCode: String?
    ): List<Map<String, String>> {
        val messages = mutableListOf<Map<String, String>>()


        val languageManager = LanguageManager.getInstance(context)
        val currentLanguage = languageManager.getCurrentLanguage()


        messages.add(mapOf("role" to "system", "content" to systemPrompt))


        workingMemory.getContextForAi()
            .filter { it["role"] != "system" }
            .forEach { messages.add(it) }


        if (workingMemory.conversationHistory.isEmpty()) {
            val userMessage = AiPromptManager.getUserMessageTemplate(
                language = currentLanguage,
                requirement = requirement,
                categoryName = category?.getDisplayName(),
                existingCode = existingCode
            )

            messages.add(mapOf("role" to "user", "content" to userMessage))
        }

        return messages
    }




    private fun parseGeneratedModule(response: String): GeneratedModuleData? {
        return try {

            val jsonMatch = JSON_BLOCK_REGEX.find(response)

            val jsonStr = if (jsonMatch != null) {
                jsonMatch.groupValues[1]
            } else {

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

            extractCodeFromResponse(response)
        }
    }




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




    private fun extractCodeFromResponse(response: String): GeneratedModuleData? {
        val jsCode = JS_BLOCK_REGEX.find(response)?.groupValues?.get(1)
        val cssCode = CSS_BLOCK_REGEX.find(response)?.groupValues?.get(1) ?: ""

        if (jsCode.isNullOrBlank()) {
            return null
        }

        return GeneratedModuleData(
            name = Strings.aiGeneratedModule,
            description = Strings.aiGeneratedModuleDesc,
            icon = "smart_toy",
            category = "OTHER",
            jsCode = jsCode,
            cssCode = cssCode
        )
    }





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


            if (!result.success) {
                emit(AgentStreamEvent.Error(
                    message = String.format(Strings.agentToolFailed, request.toolName, result.error),
                    recoverable = true
                ))
                break
            }
        }
    }.flowOn(Dispatchers.IO)




    fun reset() {
        workingMemory.reset()
        _currentState.value = AgentState.IDLE
    }
}
