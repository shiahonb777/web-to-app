package com.webtoapp.core.extension.agent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.extension.*
import com.webtoapp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 模块开发 Agent 引擎
 * 
 * 核心 Agent 实现，包含：
 * - 完整的提示词工程
 * - 上下文管理
 * - 工具链调用
 * - 迭代式开发流程
 * - 流式输出支持
 */
class ModuleAgentEngine(private val context: Context) {
    
    private val gson = Gson()
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    private val toolExecutor = AgentToolExecutor(context)
    
    // 当前会话
    private var currentSession: AgentSession? = null
    
    // 状态流
    private val _sessionState = MutableStateFlow<AgentSessionState>(AgentSessionState.IDLE)
    val sessionState: StateFlow<AgentSessionState> = _sessionState.asStateFlow()
    
    // 思考流（用于流式输出）
    private val _thoughtStream = MutableSharedFlow<AgentThought>(replay = 0)
    val thoughtStream: SharedFlow<AgentThought> = _thoughtStream.asSharedFlow()
    
    // Generate的模块流
    private val _moduleStream = MutableSharedFlow<GeneratedModuleData>(replay = 0)
    val moduleStream: SharedFlow<GeneratedModuleData> = _moduleStream.asSharedFlow()
    
    // Error流
    private val _errorStream = MutableSharedFlow<String>(replay = 0)
    val errorStream: SharedFlow<String> = _errorStream.asSharedFlow()

    /**
     * 开始新的开发会话
     */
    fun startSession(config: AgentConfig = AgentConfig()): AgentSession {
        currentSession = AgentSession(maxIterations = config.maxIterations)
        _sessionState.value = AgentSessionState.IDLE
        return currentSession!!
    }
    
    /**
     * 获取当前会话
     */
    fun getCurrentSession(): AgentSession? = currentSession
    
    /**
     * 执行开发任务
     * 
     * @param requirement 用户需求描述
     * @param category 可选的模块分类
     * @param existingCode 可选的现有代码（用于修改）
     */
    suspend fun develop(
        requirement: String,
        category: ModuleCategory? = null,
        existingCode: String? = null
    ): Flow<AgentEvent> = flow {
        val session = currentSession ?: startSession()
        session.addUserMessage(requirement)
        
        try {
            // Get AI 配置
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            if (apiKeys.isEmpty()) {
                emit(AgentEvent.Error("请先在 AI 设置中配置 API Key"))
                return@flow
            }
            
            // 优先使用支持模块开发功能的模型
            val moduleDevModels = savedModels.filter { it.supportsFeature(AiFeature.MODULE_DEVELOPMENT) }
            val defaultModelId = aiConfigManager.defaultModelIdFlow.first()
            
            val savedModel = moduleDevModels.find { it.id == defaultModelId }
                ?: moduleDevModels.firstOrNull()
                ?: savedModels.find { it.id == defaultModelId }
                ?: savedModels.firstOrNull()
            
            if (savedModel == null) {
                emit(AgentEvent.Error("请先在 AI 设置中添加并保存模型"))
                return@flow
            }
            
            val apiKey = apiKeys.find { it.id == savedModel.apiKeyId }
            if (apiKey == null) {
                emit(AgentEvent.Error("找不到模型对应的 API Key"))
                return@flow
            }
            
            // Start开发流程
            emit(AgentEvent.StateChange(AgentSessionState.THINKING))
            _sessionState.value = AgentSessionState.THINKING
            
            // 第一步：分析需求
            emit(AgentEvent.Thought(AgentThought(1, ThoughtType.ANALYSIS, "正在分析需求: $requirement")))
            
            // Build系统提示词
            val systemPrompt = buildSystemPrompt(category, existingCode)
            
            // 第二步：规划开发步骤
            emit(AgentEvent.StateChange(AgentSessionState.PLANNING))
            _sessionState.value = AgentSessionState.PLANNING
            emit(AgentEvent.Thought(AgentThought(2, ThoughtType.PLANNING, "制定开发计划...")))
            
            // 调用 AI 生成代码
            // Requirements: 2.5, 3.1, 3.2 - 使用具体状态消息替代通用加载提示
            emit(AgentEvent.StateChange(AgentSessionState.GENERATING))
            _sessionState.value = AgentSessionState.GENERATING
            emit(AgentEvent.Thought(AgentThought(3, ThoughtType.GENERATION, "调用 AI 模型生成扩展模块代码...")))
            
            val messages = buildMessages(systemPrompt, requirement, category, existingCode)
            val aiResponse = aiClient.chat(apiKey, savedModel.model, messages)
            
            if (aiResponse.isFailure) {
                emit(AgentEvent.Error("AI 调用失败: ${aiResponse.exceptionOrNull()?.message}"))
                return@flow
            }
            
            val responseText = aiResponse.getOrNull() ?: ""
            emit(AgentEvent.Thought(AgentThought(4, ThoughtType.GENERATION, "代码生成完成，正在解析...")))
            
            // Parse生成的模块
            val generatedModule = parseGeneratedModule(responseText)
            if (generatedModule == null) {
                emit(AgentEvent.Error("无法解析 AI 生成的代码"))
                return@flow
            }
            
            session.workingModule = generatedModule
            emit(AgentEvent.ModuleGenerated(generatedModule))
            
            // 第三步：语法检查
            emit(AgentEvent.StateChange(AgentSessionState.REVIEWING))
            _sessionState.value = AgentSessionState.REVIEWING
            emit(AgentEvent.Thought(AgentThought(5, ThoughtType.REVIEW, "正在检查代码语法...")))
            
            val syntaxResult = toolExecutor.execute(ToolCallRequest(
                toolName = "syntax_check",
                arguments = mapOf("code" to generatedModule.jsCode, "language" to "javascript")
            ))
            
            emit(AgentEvent.ToolResult(syntaxResult))
            
            val syntaxCheck = syntaxResult.result as? SyntaxCheckResult
            if (syntaxCheck != null && !syntaxCheck.valid) {
                emit(AgentEvent.Thought(AgentThought(6, ThoughtType.REVIEW, 
                    "发现 ${syntaxCheck.errors.size} 个语法错误，${syntaxCheck.warnings.size} 个警告")))
                
                // 尝试自动修复
                emit(AgentEvent.StateChange(AgentSessionState.FIXING))
                _sessionState.value = AgentSessionState.FIXING
                emit(AgentEvent.Thought(AgentThought(7, ThoughtType.FIX, "正在尝试自动修复...")))
                
                val fixedModule = tryFixErrors(generatedModule, syntaxCheck, apiKey, savedModel)
                if (fixedModule != null) {
                    session.workingModule = fixedModule
                    emit(AgentEvent.ModuleGenerated(fixedModule))
                    emit(AgentEvent.Thought(AgentThought(8, ThoughtType.FIX, "错误已修复")))
                }
            } else {
                emit(AgentEvent.Thought(AgentThought(6, ThoughtType.REVIEW, "语法检查通过 ✓")))
            }

            // 第四步：安全扫描
            emit(AgentEvent.Thought(AgentThought(9, ThoughtType.REVIEW, "正在进行安全扫描...")))
            
            val securityResult = toolExecutor.execute(ToolCallRequest(
                toolName = "security_scan",
                arguments = mapOf("code" to (session.workingModule?.jsCode ?: ""))
            ))
            
            emit(AgentEvent.ToolResult(securityResult))
            
            val securityScan = securityResult.result as? SecurityScanResult
            if (securityScan != null) {
                val updatedModule = session.workingModule?.copy(
                    securitySafe = securityScan.safe
                )
                if (updatedModule != null) {
                    session.workingModule = updatedModule
                }
                
                if (!securityScan.safe) {
                    emit(AgentEvent.Thought(AgentThought(10, ThoughtType.REVIEW, 
                        "⚠️ 发现 ${securityScan.issues.size} 个安全问题，风险等级: ${securityScan.riskLevel}")))
                } else {
                    emit(AgentEvent.Thought(AgentThought(10, ThoughtType.REVIEW, "安全扫描通过 ✓")))
                }
            }
            
            // Done
            emit(AgentEvent.StateChange(AgentSessionState.COMPLETED))
            _sessionState.value = AgentSessionState.COMPLETED
            
            val finalModule = session.workingModule
            if (finalModule != null) {
                emit(AgentEvent.Thought(AgentThought(11, ThoughtType.CONCLUSION, 
                    "✅ 模块「${finalModule.name}」开发完成！")))
                emit(AgentEvent.Completed(finalModule))
                
                // Save到会话
                session.addAssistantMessage(
                    content = "已成功生成模块「${finalModule.name}」",
                    thoughts = session.currentThoughts.toList(),
                    generatedModule = finalModule
                )
            }
            
        } catch (e: Exception) {
            emit(AgentEvent.StateChange(AgentSessionState.ERROR))
            _sessionState.value = AgentSessionState.ERROR
            emit(AgentEvent.Error(e.message ?: "未知错误"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 构建系统提示词
     */
    private fun buildSystemPrompt(category: ModuleCategory?, existingCode: String?): String {
        val categoryHint = category?.let {
            """
## 目标分类
用户希望创建「${it.getDisplayName()}」类型的模块。
分类说明：${it.getDescription()}
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
        
        return """
你是一个专业的 WebToApp 扩展模块开发专家。你的任务是根据用户需求生成高质量的扩展模块代码。

## 扩展模块系统说明
WebToApp 扩展模块是注入到网页中执行的 JavaScript/CSS 代码，类似于浏览器扩展或油猴脚本。
模块会在 WebView 加载网页时自动注入执行。

## 可用的内置 API
```javascript
// Get用户配置值
getConfig(key: string, defaultValue: any): any

// Module信息对象
__MODULE_INFO__ = { id: string, name: string, version: string }

// User配置值对象
__MODULE_CONFIG__ = { [key: string]: any }
```

## 代码规范要求
1. 使用 'use strict' 严格模式
2. 代码已被包装在 IIFE 中，无需再次包装
3. 使用 const/let 而非 var
4. 使用 === 而非 ==
5. 添加适当的错误处理 try-catch
6. 使用 MutationObserver 监听动态内容
7. 避免使用 eval、document.write 等不安全函数
8. 添加清晰的注释说明

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
        
        // System消息
        messages.add(mapOf("role" to "system", "content" to systemPrompt))
        
        // User消息
        val userMessage = buildString {
            append("请根据以下需求开发一个扩展模块：\n\n")
            append("**需求描述**：$requirement\n")
            
            if (category != null) {
                append("\n**目标分类**：${category.getDisplayName()}\n")
            }
            
            if (!existingCode.isNullOrBlank()) {
                append("\n**现有代码**（请在此基础上修改）：\n```javascript\n$existingCode\n```\n")
            }
            
            append("\n请生成完整的模块代码，并确保代码质量和安全性。")
        }
        
        messages.add(mapOf("role" to "user", "content" to userMessage))
        
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
    private fun parseConfigItems(json: JsonObject): List<Map<String, Any>> {
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
     * 尝试修复错误
     */
    private suspend fun tryFixErrors(
        module: GeneratedModuleData,
        syntaxResult: SyntaxCheckResult,
        apiKey: ApiKeyConfig,
        savedModel: SavedModel
    ): GeneratedModuleData? {
        // Build修复提示
        val errorMessages = syntaxResult.errors.joinToString("\n") { error ->
            "- 第 ${error.line} 行: ${error.message}"
        }
        
        val fixPrompt = """
请修复以下 JavaScript 代码中的语法错误：

**错误列表**：
$errorMessages

**原始代码**：
```javascript
${module.jsCode}
```

请只输出修复后的代码，使用 ```javascript 代码块包裹。
        """.trimIndent()
        
        val messages = listOf(
            mapOf("role" to "system", "content" to "你是一个 JavaScript 代码修复专家。请修复代码中的语法错误，保持原有功能不变。"),
            mapOf("role" to "user", "content" to fixPrompt)
        )
        
        val response = aiClient.chat(apiKey, savedModel.model, messages)
        
        if (response.isSuccess) {
            val fixedCode = response.getOrNull() ?: return null
            val codePattern = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)\\s*```")
            val code = codePattern.find(fixedCode)?.groupValues?.get(1) ?: fixedCode
            
            return module.copy(jsCode = code.trim())
        }
        
        return null
    }
}

/**
 * Agent 事件
 */
sealed class AgentEvent {
    data class StateChange(val state: AgentSessionState) : AgentEvent()
    data class Thought(val thought: AgentThought) : AgentEvent()
    data class ToolResult(val result: ToolCallResult) : AgentEvent()
    data class ModuleGenerated(val module: GeneratedModuleData) : AgentEvent()
    data class Completed(val module: GeneratedModuleData) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}
