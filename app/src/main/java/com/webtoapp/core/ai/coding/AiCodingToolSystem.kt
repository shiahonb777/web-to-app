package com.webtoapp.core.ai.coding

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.google.gson.JsonParser
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.ToolStreamEvent as ApiToolStreamEvent
import com.webtoapp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

/**
 * HTML 编程 AI 工具系统
 * 
 * 实现完整的 Agent 工具链：
 * 1. 工具定义与注册
 * 2. 提示词工程（系统提示词 + 工具描述）
 * 3. 上下文工程（对话历史 + 代码状态 + 工具结果）
 * 4. ReAct 循环（推理 -> 行动 -> 观察）
 * 5. 流式工具调用解析
 * 
 * 修复：
 * - 添加超时保护
 * - 改进 ReAct 循环的终止条件
 * - 优化工具结果消息格式
 */
class AiCodingToolSystem(private val context: Context) {
    
    private val gson = com.webtoapp.util.GsonProvider.gson
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    
    companion object {
        private const val TAG = "AiCodingToolSystem"
        private const val MAX_TOOL_ITERATIONS = 5  // Max工具调用轮次
        private const val STREAM_TIMEOUT_MS = 90_000L  // 90秒超时
        private val TAG_PATTERN_REGEX = Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>")
        private val TOOL_CALL_BLOCK_REGEX = Regex("""```tool_call\s*([\s\S]*?)```""")
        
        // extractSection 预编译 Regex
        private val SECTION_REGEX_MAP: Map<String, Regex> = listOf("head", "body", "style", "script")
            .associateWith { tag -> Regex("<$tag[^>]*>([\\s\\S]*?)</$tag>", RegexOption.IGNORE_CASE) }
        
        // checkSyntax 自闭合标签集合
        private val SELF_CLOSING_TAGS = setOf(
            "br", "hr", "img", "input", "meta", "link",
            "area", "base", "col", "embed", "param", "source", "track", "wbr"
        )
    }
    
    // ==================== 工具定义 ====================
    
    /**
     * 所有可用工具
     */
    private val tools = listOf(
        ToolDefinition(
            name = "write_html",
            description = "创建或完全重写 HTML 页面。输出完整的 HTML 代码，包含 DOCTYPE、head、body。适用于首次创建或需要大幅修改时。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "content" to mapOf(
                        "type" to "string",
                        "description" to "完整的 HTML 代码"
                    ),
                    "description" to mapOf(
                        "type" to "string",
                        "description" to "简要说明这次修改做了什么"
                    )
                ),
                "required" to listOf("content")
            )
        ),
        ToolDefinition(
            name = "edit_html",
            description = "编辑现有 HTML 代码的指定部分。支持替换、插入、删除操作。适合小范围精确修改。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "operation" to mapOf(
                        "type" to "string",
                        "enum" to listOf("replace", "insert_before", "insert_after", "delete"),
                        "description" to "操作类型"
                    ),
                    "target" to mapOf(
                        "type" to "string",
                        "description" to "要定位的目标代码片段（必须精确匹配）"
                    ),
                    "content" to mapOf(
                        "type" to "string",
                        "description" to "新的代码内容（delete 操作时可省略）"
                    ),
                    "description" to mapOf(
                        "type" to "string",
                        "description" to "简要说明这次修改做了什么"
                    )
                ),
                "required" to listOf("operation", "target")
            )
        ),
        ToolDefinition(
            name = "read_current_code",
            description = "读取当前的 HTML 代码。在修改前调用此工具了解当前代码状态。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "section" to mapOf(
                        "type" to "string",
                        "enum" to listOf("all", "head", "body", "style", "script"),
                        "description" to "要读取的部分，默认 all"
                    )
                ),
                "required" to emptyList<String>()
            )
        ),
        ToolDefinition(
            name = "check_syntax",
            description = "检查代码语法错误。返回错误列表和位置信息。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "language" to mapOf(
                        "type" to "string",
                        "enum" to listOf("html", "css", "javascript", "auto"),
                        "description" to "代码语言，auto 自动检测"
                    )
                ),
                "required" to emptyList<String>()
            )
        ),
        ToolDefinition(
            name = "preview",
            description = "预览当前 HTML 页面。返回预览状态。",
            parameters = mapOf(
                "type" to "object",
                "properties" to emptyMap<String, Any>(),
                "required" to emptyList<String>()
            )
        )
    )
    
    // ==================== 上下文管理 ====================
    
    /**
     * Agent 上下文
     */
    data class AgentContext(
        var currentHtml: String = "",
        val conversationHistory: MutableList<ConversationMessage> = mutableListOf(),
        val toolCallHistory: MutableList<ToolCallRecord> = mutableListOf(),
        var lastError: String? = null,
        var iterationCount: Int = 0
    ) {
        fun addUserMessage(content: String) {
            conversationHistory.add(ConversationMessage("user", content))
        }
        
        fun addAssistantMessage(content: String, toolCalls: List<ToolCall>? = null) {
            conversationHistory.add(ConversationMessage("assistant", content, toolCalls))
        }
        
        fun addToolResult(toolCallId: String, result: String) {
            conversationHistory.add(ConversationMessage("tool", result, toolCallId = toolCallId))
        }
        
        fun recordToolCall(call: ToolCall, result: ToolExecutionResult) {
            toolCallHistory.add(ToolCallRecord(call, result))
        }
        
        fun reset() {
            currentHtml = ""
            conversationHistory.clear()
            toolCallHistory.clear()
            lastError = null
            iterationCount = 0
        }
    }
    
    data class ConversationMessage(
        val role: String,
        val content: String,
        val toolCalls: List<ToolCall>? = null,
        val toolCallId: String? = null
    )
    
    data class ToolCall(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val arguments: String
    )
    
    data class ToolCallRecord(
        val call: ToolCall,
        val result: ToolExecutionResult,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // ==================== 提示词工程 ====================
    
    /**
     * 构建系统提示词（包含工具描述）
     */
    private fun buildSystemPrompt(config: SessionConfig): String {
        val toolsDescription = tools.joinToString("\n\n") { tool ->
            """
### ${tool.name}
${tool.description}

参数：
```json
${gson.toJson(tool.parameters)}
```
            """.trimIndent()
        }
        
        return """
# 角色

你是一个专业的移动端 HTML 开发 Agent。你可以通过调用工具来创建和修改 HTML 页面。

---

# 工作流程

你遵循 ReAct（推理-行动-观察）模式工作：

1. **推理 (Reasoning)**：分析用户需求，思考需要做什么
2. **行动 (Action)**：调用合适的工具执行操作
3. **观察 (Observation)**：查看工具执行结果
4. **循环**：根据结果决定是否需要继续操作

---

# 可用工具

$toolsDescription

---

# 工具调用格式

当你需要调用工具时，使用以下 JSON 格式：

```tool_call
{
  "name": "工具名称",
  "arguments": {
    "参数名": "参数值"
  }
}
```

你可以在一次回复中调用多个工具，每个工具调用使用单独的 ```tool_call``` 代码块。

---

# 核心规则

## 代码质量要求
1. 所有 HTML 必须适配移动端 WebView
2. 使用 viewport meta 标签
3. 使用相对单位（vw, vh, %, rem）
4. 触摸目标最小 44x44px
5. 适配安全区域（刘海屏）

## 工具使用策略
1. **首次创建**：使用 `write_html` 生成完整页面
2. **小修改**：使用 `edit_html` 精确修改
3. **大修改**：使用 `write_html` 重写
4. **修改前**：可用 `read_current_code` 了解当前状态
5. **修改后**：可用 `check_syntax` 检查语法

## 输出规范
1. 先简要说明你的思路
2. 然后调用工具执行
3. 工具调用后等待结果
4. 根据结果决定下一步

---

# 用户自定义规则

${config.getEffectiveRules().joinToString("\n") { "- $it" }.ifEmpty { "（无）" }}
        """.trimIndent()
    }
    
    /**
     * 构建消息列表（包含上下文）
     */
    private fun buildMessages(
        agentContext: AgentContext,
        config: SessionConfig
    ): List<Map<String, Any>> {
        val messages = mutableListOf<Map<String, Any>>()
        
        // System消息
        messages.add(mapOf("role" to "system", "content" to buildSystemPrompt(config)))
        
        // 如果有当前代码，添加上下文
        if (agentContext.currentHtml.isNotBlank()) {
            messages.add(mapOf(
                "role" to "system",
                "content" to """
当前 HTML 代码状态：
```html
${agentContext.currentHtml}
```
                """.trimIndent()
            ))
        }
        
        // 添加对话历史
        agentContext.conversationHistory.forEach { msg ->
            when (msg.role) {
                "user" -> messages.add(mapOf("role" to "user", "content" to msg.content))
                "assistant" -> {
                    if (msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
                        // 带工具调用的助手消息
                        messages.add(mapOf(
                            "role" to "assistant",
                            "content" to msg.content,
                            "tool_calls" to msg.toolCalls.map { call ->
                                mapOf(
                                    "id" to call.id,
                                    "type" to "function",
                                    "function" to mapOf(
                                        "name" to call.name,
                                        "arguments" to call.arguments
                                    )
                                )
                            }
                        ))
                    } else {
                        messages.add(mapOf("role" to "assistant", "content" to msg.content))
                    }
                }
                "tool" -> {
                    messages.add(mapOf(
                        "role" to "tool",
                        "tool_call_id" to (msg.toolCallId ?: ""),
                        "content" to msg.content
                    ))
                }
            }
        }
        
        return messages
    }
    
    // ==================== 工具执行 ====================
    
    /**
     * 执行工具调用
     * 返回 AiCodingAgent 中定义的 ToolExecutionResult
     */
    fun executeToolCall(
        toolName: String,
        arguments: Map<String, Any?>,
        agentContext: AgentContext
    ): ToolExecutionResult {
        return try {
            when (toolName) {
                "write_html" -> {
                    val content = arguments["content"] as? String ?: ""
                    @Suppress("UNUSED_VARIABLE")
                    val description = arguments["description"] as? String ?: "创建 HTML"
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = content,
                        isHtml = true
                    )
                }
                
                "edit_html" -> {
                    val operation = arguments["operation"] as? String ?: "replace"
                    val target = arguments["target"] as? String ?: ""
                    val content = arguments["content"] as? String ?: ""
                    
                    if (agentContext.currentHtml.isBlank()) {
                        return ToolExecutionResult(
                            success = false,
                            toolName = toolName,
                            result = "❌ 当前没有 HTML 代码，请先使用 write_html 创建"
                        )
                    }
                    
                    if (!agentContext.currentHtml.contains(target)) {
                        return ToolExecutionResult(
                            success = false,
                            toolName = toolName,
                            result = "❌ 找不到目标代码片段，请确保 target 与现有代码完全匹配"
                        )
                    }
                    
                    val newHtml = when (operation) {
                        "replace" -> agentContext.currentHtml.replace(target, content)
                        "insert_before" -> agentContext.currentHtml.replace(target, content + target)
                        "insert_after" -> agentContext.currentHtml.replace(target, target + content)
                        "delete" -> agentContext.currentHtml.replace(target, "")
                        else -> return ToolExecutionResult(
                            success = false,
                            toolName = toolName,
                            result = "❌ 未知操作类型: $operation"
                        )
                    }
                    
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = newHtml,
                        isHtml = true,
                        isEdit = true
                    )
                }
                
                "read_current_code" -> {
                    val section = arguments["section"] as? String ?: "all"
                    val code = when (section) {
                        "head" -> extractSection(agentContext.currentHtml, "head")
                        "body" -> extractSection(agentContext.currentHtml, "body")
                        "style" -> extractSection(agentContext.currentHtml, "style")
                        "script" -> extractSection(agentContext.currentHtml, "script")
                        else -> agentContext.currentHtml
                    }
                    
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = if (code.isBlank()) "当前没有代码" else code
                    )
                }
                
                "check_syntax" -> {
                    val errors = checkSyntax(agentContext.currentHtml)
                    val resultMsg = if (errors.isEmpty()) {
                        "✅ 语法检查通过"
                    } else {
                        "⚠️ 发现 ${errors.size} 个问题:\n${errors.joinToString("\n") { "- $it" }}"
                    }
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = resultMsg
                    )
                }
                
                "preview" -> {
                    ToolExecutionResult(
                        success = true,
                        toolName = toolName,
                        result = "🔍 预览已触发"
                    )
                }
                
                else -> ToolExecutionResult(
                    success = false,
                    toolName = toolName,
                    result = "❌ 未知工具: $toolName"
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Tool execution failed: $toolName", e)
            ToolExecutionResult(
                success = false,
                toolName = toolName,
                result = "❌ 工具执行失败: ${e.message}"
            )
        }
    }
    
    /**
     * 检查工具结果是否触发预览
     */
    fun shouldTriggerPreview(toolName: String): Boolean = toolName == "preview"
    
    private fun extractSection(html: String, tag: String): String {
        val pattern = SECTION_REGEX_MAP[tag] ?: return ""
        return pattern.find(html)?.groupValues?.get(1)?.trim() ?: ""
    }
    
    private fun checkSyntax(html: String): List<String> {
        val errors = mutableListOf<String>()
        // 简单的语法检查
        val tagStack = mutableListOf<String>()
        
        TAG_PATTERN_REGEX.findAll(html).forEach { match ->
            val isClosing = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].lowercase()
            val isSelfClosing = match.groupValues[3].endsWith("/") || tagName in SELF_CLOSING_TAGS
            
            if (!isSelfClosing) {
                if (isClosing) {
                    if (tagStack.isNotEmpty() && tagStack.last() == tagName) {
                        tagStack.removeAt(tagStack.lastIndex)
                    } else if (tagStack.contains(tagName)) {
                        errors.add("标签 <$tagName> 嵌套错误")
                    } else {
                        errors.add("多余的闭合标签 </$tagName>")
                    }
                } else {
                    tagStack.add(tagName)
                }
            }
        }
        
        tagStack.forEach { tag ->
            errors.add("标签 <$tag> 未闭合")
        }
        
        return errors
    }
    
    // ==================== ReAct 循环 ====================
    
    /**
     * 执行 Agent 对话（带工具调用的 ReAct 循环）
     * 
     * 修复：
     * - 添加超时保护
     * - 改进循环终止条件
     * - 优化错误处理
     */
    fun chat(
        userMessage: String,
        agentContext: AgentContext,
        config: SessionConfig,
        apiKey: ApiKeyConfig,
        model: SavedModel
    ): Flow<AgentEvent> = flow {
        agentContext.addUserMessage(userMessage)
        agentContext.iterationCount = 0
        
        emit(AgentEvent.Started)
        
        // ReAct 循环
        while (agentContext.iterationCount < MAX_TOOL_ITERATIONS) {
            agentContext.iterationCount++
            AppLogger.d(TAG, "ReAct iteration ${agentContext.iterationCount}/$MAX_TOOL_ITERATIONS")
            
            val messages = buildMessages(agentContext, config)
            val contentBuilder = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()
            var currentToolName = ""
            val currentToolArgs = StringBuilder()
            
            // 调用 AI
            emit(AgentEvent.Thinking("正在思考..."))
            
            try {
                // 带超时的 AI 调用
                withTimeout(STREAM_TIMEOUT_MS) {
                    aiClient.chatStreamWithTools(
                        apiKey = apiKey,
                        model = model.model,
                        messages = messages.map { 
                            mapOf("role" to (it["role"] as String), "content" to (it["content"] as String))
                        },
                        tools = tools.map { it.toOpenAIFormat() }
                    ).collect { event ->
                        when (event) {
                            is ApiToolStreamEvent.Started -> {
                                // 流开始
                            }
                            is ApiToolStreamEvent.TextDelta -> {
                                contentBuilder.clear()
                                contentBuilder.append(event.accumulated)
                                emit(AgentEvent.Content(event.delta, event.accumulated))
                            }
                            is ApiToolStreamEvent.ThinkingDelta -> {
                                emit(AgentEvent.Thinking(event.accumulated))
                            }
                            is ApiToolStreamEvent.ToolCallStart -> {
                                currentToolName = event.toolName
                                currentToolArgs.clear()
                                emit(AgentEvent.ToolCallStart(event.toolName, event.toolCallId))
                            }
                            is ApiToolStreamEvent.ToolArgumentsDelta -> {
                                currentToolArgs.clear()
                                currentToolArgs.append(event.accumulated)
                                // 实时显示工具参数（如 HTML 代码）
                                emit(AgentEvent.ToolArgumentsStreaming(
                                    currentToolName,
                                    event.toolCallId,
                                    event.delta,
                                    event.accumulated
                                ))
                            }
                            is ApiToolStreamEvent.ToolCallComplete -> {
                                toolCalls.add(ToolCall(
                                    id = event.toolCallId,
                                    name = event.toolName,
                                    arguments = event.arguments
                                ))
                                emit(AgentEvent.ToolCallComplete(event.toolName, event.toolCallId, event.arguments))
                            }
                            is ApiToolStreamEvent.Done -> {
                                // stream completed
                            }
                            is ApiToolStreamEvent.Error -> {
                                // 如果原生工具调用失败，尝试从文本中解析
                                val textToolCalls = parseToolCallsFromText(contentBuilder.toString())
                                if (textToolCalls.isNotEmpty()) {
                                    toolCalls.addAll(textToolCalls)
                                } else {
                                    throw Exception(event.message)
                                }
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                AppLogger.e(TAG, "AI call timeout")
                emit(AgentEvent.Error("请求超时，请重试"))
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "AI call failed", e)
                // 尝试从已收到的文本中解析工具调用
                val textToolCalls = parseToolCallsFromText(contentBuilder.toString())
                if (textToolCalls.isNotEmpty()) {
                    toolCalls.addAll(textToolCalls)
                } else {
                    emit(AgentEvent.Error("AI 调用失败: ${e.message}"))
                    break
                }
            }
            
            // 记录助手消息
            agentContext.addAssistantMessage(
                contentBuilder.toString(),
                if (toolCalls.isNotEmpty()) toolCalls else null
            )
            
            // 如果没有工具调用，结束循环
            if (toolCalls.isEmpty()) {
                AppLogger.d(TAG, "No tool calls, ending ReAct loop")
                emit(AgentEvent.Completed(contentBuilder.toString(), agentContext.currentHtml))
                break
            }
            
            // Execute工具调用
            var hasHtmlUpdate = false
            for (call in toolCalls) {
                emit(AgentEvent.ToolExecuting(call.name, call.id))
                
                val arguments = try {
                    @Suppress("UNCHECKED_CAST")
                    gson.fromJson(call.arguments, Map::class.java) as Map<String, Any?>
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Failed to parse tool arguments: ${e.message}")
                    mapOf<String, Any?>()
                }
                
                val result = executeToolCall(call.name, arguments, agentContext)
                agentContext.recordToolCall(call, result)
                
                // Update HTML 状态
                if (result.isHtml && result.result is String) {
                    agentContext.currentHtml = result.result
                    hasHtmlUpdate = true
                }
                
                // 添加工具结果到上下文
                val resultMessage = buildToolResultMessage(result)
                agentContext.addToolResult(call.id, resultMessage)
                
                emit(AgentEvent.ToolResult(call.name, call.id, result))
                
                // 如果触发预览
                if (shouldTriggerPreview(call.name)) {
                    emit(AgentEvent.PreviewRequested(agentContext.currentHtml))
                }
            }
            
            // 如果有 HTML 更新且是 write_html 工具，可能不需要继续循环
            if (hasHtmlUpdate && toolCalls.any { it.name == "write_html" }) {
                AppLogger.d(TAG, "HTML updated via write_html, completing")
                emit(AgentEvent.HtmlUpdated(agentContext.currentHtml, "HTML 已更新"))
                emit(AgentEvent.Completed(contentBuilder.toString(), agentContext.currentHtml))
                break
            }
        }
        
        if (agentContext.iterationCount >= MAX_TOOL_ITERATIONS) {
            emit(AgentEvent.Warning("达到最大工具调用次数限制"))
            emit(AgentEvent.Completed("", agentContext.currentHtml))
        }
        
    }.flowOn(Dispatchers.IO)
    
    /**
     * 从文本中解析工具调用（用于不支持原生工具调用的模型）
     */
    private fun parseToolCallsFromText(text: String): List<ToolCall> {
        val calls = mutableListOf<ToolCall>()
        
        TOOL_CALL_BLOCK_REGEX.findAll(text).forEach { match ->
            try {
                val json = JsonParser.parseString(match.groupValues[1]).asJsonObject
                val name = json.get("name")?.asString ?: return@forEach
                val arguments = json.getAsJsonObject("arguments")?.toString() ?: "{}"
                calls.add(ToolCall(name = name, arguments = arguments))
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to parse tool call: ${e.message}")
            }
        }
        
        return calls
    }
    
    private fun buildToolResultMessage(result: ToolExecutionResult): String {
        return result.result
    }
}

// ==================== 数据类 ====================

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
) {
    fun toOpenAIFormat(): Map<String, Any> = mapOf(
        "type" to "function",
        "function" to mapOf(
            "name" to name,
            "description" to description,
            "parameters" to parameters
        )
    )
}

sealed class AgentEvent {
    object Started : AgentEvent()
    data class Thinking(val message: String) : AgentEvent()
    data class Content(val delta: String, val accumulated: String) : AgentEvent()
    data class ToolCallStart(val toolName: String, val callId: String) : AgentEvent()
    data class ToolArgumentsStreaming(val toolName: String, val callId: String, val delta: String, val accumulated: String) : AgentEvent()
    data class ToolCallComplete(val toolName: String, val callId: String, val arguments: String) : AgentEvent()
    data class ToolExecuting(val toolName: String, val callId: String) : AgentEvent()
    data class ToolResult(val toolName: String, val callId: String, val result: ToolExecutionResult) : AgentEvent()
    data class PreviewRequested(val html: String) : AgentEvent()
    data class HtmlUpdated(val html: String, val description: String) : AgentEvent()
    data class Warning(val message: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    data class Completed(val response: String, val finalHtml: String) : AgentEvent()
}
