package com.webtoapp.core.extension.agent

import com.google.gson.annotations.SerializedName
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * Agent 上下文管理
 * 
 * 管理 Agent 的对话历史、工作状态、生成的代码等上下文信息
 */

// ==================== Agent 流式事件类型 ====================

/**
 * Agent 状态枚举
 * 用于表示 Agent 当前的工作状态
 */
enum class AgentState(val icon: String) {
    IDLE("pause"),
    THINKING("thinking"),
    GENERATING("auto_awesome"),
    TOOL_CALLING("wrench"),
    SYNTAX_CHECKING("search"),
    FIXING("build"),
    SECURITY_SCANNING("lock"),
    COMPLETED("check"),
    ERROR("error");

    val displayName: String get() = when (this) {
        IDLE -> AppStringsProvider.current().agentStateIdle
        THINKING -> AppStringsProvider.current().agentStateThinking
        GENERATING -> AppStringsProvider.current().agentStateGenerating
        TOOL_CALLING -> AppStringsProvider.current().agentStateToolCalling
        SYNTAX_CHECKING -> AppStringsProvider.current().agentStateSyntaxCheck
        FIXING -> AppStringsProvider.current().agentStateFixing
        SECURITY_SCANNING -> AppStringsProvider.current().agentStateSecurityScan
        COMPLETED -> AppStringsProvider.current().agentStateCompleted
        ERROR -> AppStringsProvider.current().agentStateError
    }
}

/**
 * 工具调用信息
 * 用于在 UI 中展示工具调用的详细信息
 */
data class ToolCallInfo(
    @SerializedName("tool_name")
    val toolName: String,
    @SerializedName("tool_icon")
    val toolIcon: String = "wrench",
    @SerializedName("parameters")
    val parameters: Map<String, Any?> = emptyMap(),
    @SerializedName("status")
    val status: ToolStatus = ToolStatus.PENDING,
    @SerializedName("result")
    val result: Any? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("execution_time_ms")
    val executionTimeMs: Long = 0,
    @SerializedName("call_id")
    val callId: String = java.util.UUID.randomUUID().toString()
) {
    companion object {
        /**
         * 从 ToolCallRequest 创建 ToolCallInfo
         */
        fun fromRequest(request: ToolCallRequest): ToolCallInfo {
            val toolType = AgentToolType.entries.find { 
                it.name.equals(request.toolName, ignoreCase = true) ||
                request.toolName.equals(it.name.lowercase().replace("_", ""), ignoreCase = true)
            }
            return ToolCallInfo(
                toolName = request.toolName,
                toolIcon = toolType?.icon ?: "wrench",
                parameters = request.arguments,
                status = ToolStatus.PENDING,
                callId = request.callId
            )
        }
        
        /**
         * 从 ToolCallResult 更新 ToolCallInfo
         */
        fun fromResult(info: ToolCallInfo, result: ToolCallResult): ToolCallInfo {
            return info.copy(
                status = if (result.success) ToolStatus.SUCCESS else ToolStatus.FAILED,
                result = result.result,
                error = result.error,
                executionTimeMs = result.executionTimeMs
            )
        }
    }
}

/**
 * 工具执行状态
 */
enum class ToolStatus {
    PENDING,    // 等待执行
    EXECUTING,  // Execute中
    SUCCESS,    // Execute成功
    FAILED      // Execute失败
}

/**
 * Agent 流式事件
 * 
 * 用于实时传递 Agent 开发过程中的各种事件，支持流式输出和工具调用可视化
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 5.8
 */
sealed class AgentStreamEvent {
    
    /**
     * 状态变化事件
     * 当 Agent 状态发生变化时触发
     */
    data class StateChange(val state: AgentState) : AgentStreamEvent()
    
    /**
     * 思考内容事件（流式）
     * 当 AI 正在思考或推理时触发，用于显示思考过程
     * 
     * @param content 本次增量的思考内容
     * @param fullContent 累积的完整思考内容
     */
    data class Thinking(
        val content: String,
        val fullContent: String
    ) : AgentStreamEvent()
    
    /**
     * 生成内容事件（流式）
     * 当 AI 生成内容时触发，用于实时显示生成的文本
     * 
     * @param delta 本次增量的内容
     * @param fullContent 累积的完整内容
     */
    data class Content(
        val delta: String,
        val fullContent: String
    ) : AgentStreamEvent()
    
    /**
     * 工具调用开始事件
     * 当开始执行工具调用时触发
     * 
     * @param toolCall 工具调用信息
     */
    data class ToolStart(val toolCall: ToolCallInfo) : AgentStreamEvent()
    
    /**
     * 工具调用完成事件
     * 当工具调用执行完成时触发
     * 
     * @param toolCall 包含执行结果的工具调用信息
     */
    data class ToolComplete(val toolCall: ToolCallInfo) : AgentStreamEvent()
    
    /**
     * 模块生成事件
     * 当成功解析出模块数据时触发
     * 
     * @param module 生成的模块数据
     */
    data class ModuleGenerated(val module: GeneratedModuleData) : AgentStreamEvent()
    
    /**
     * 错误事件
     * 当发生错误时触发
     * 
     * @param message 错误消息
     * @param code 错误码（可选）
     * @param recoverable 是否可恢复
     * @param rawResponse 原始响应（用于调试）
     */
    data class Error(
        val message: String,
        val code: String? = null,
        val recoverable: Boolean = true,
        val rawResponse: String? = null
    ) : AgentStreamEvent()
    
    /**
     * 完成事件
     * 当整个开发流程完成时触发
     * 
     * @param module 最终生成的模块数据
     */
    data class Completed(val module: GeneratedModuleData) : AgentStreamEvent()
}

// ==================== 原有代码 ====================

/**
 * Agent 会话状态
 */
enum class AgentSessionState {
    IDLE,           // Empty闲
    THINKING,       // 思考中
    PLANNING,       // 规划中
    EXECUTING,      // Execute工具中
    GENERATING,     // Generate代码中
    REVIEWING,      // 审查代码中
    FIXING,         // 修复错误中
    COMPLETED,      // Done
    ERROR           // Error
}

/**
 * Agent 思考步骤
 */
data class AgentThought(
    @SerializedName("step")
    val step: Int,
    @SerializedName("type")
    val type: ThoughtType,
    @SerializedName("content")
    val content: String,
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 思考类型
 */
enum class ThoughtType(val icon: String) {
    ANALYSIS("search"),
    PLANNING("clipboard"),
    TOOL_CALL("wrench"),
    TOOL_RESULT("analytics"),
    GENERATION("auto_awesome"),
    REVIEW("visibility"),
    FIX("build"),
    CONCLUSION("check"),
    ERROR("error");

    val displayName: String get() = when (this) {
        ANALYSIS -> AppStringsProvider.current().thoughtAnalysis
        PLANNING -> AppStringsProvider.current().thoughtPlanning
        TOOL_CALL -> AppStringsProvider.current().thoughtToolCall
        TOOL_RESULT -> AppStringsProvider.current().thoughtToolResult
        GENERATION -> AppStringsProvider.current().thoughtGeneration
        REVIEW -> AppStringsProvider.current().thoughtReview
        FIX -> AppStringsProvider.current().thoughtFix
        CONCLUSION -> AppStringsProvider.current().thoughtConclusion
        ERROR -> AppStringsProvider.current().agentStateError
    }
}


/**
 * Agent 消息
 */
data class AgentMessage(
    @SerializedName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("role")
    val role: MessageRole,
    @SerializedName("content")
    val content: String,
    @SerializedName("thoughts")
    val thoughts: List<AgentThought> = emptyList(),
    @SerializedName("tool_calls")
    val toolCalls: List<ToolCallRequest> = emptyList(),
    @SerializedName("tool_results")
    val toolResults: List<ToolCallResult> = emptyList(),
    @SerializedName("generated_module")
    val generatedModule: GeneratedModuleData? = null,
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 消息角色
 */
enum class MessageRole {
    USER,       // User消息
    ASSISTANT,  // AI 助手消息
    SYSTEM,     // System消息
    TOOL        // 工具消息
}

/**
 * 生成的模块数据
 */
data class GeneratedModuleData(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("icon")
    val icon: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("js_code")
    val jsCode: String,
    @SerializedName("css_code")
    val cssCode: String = "",
    @SerializedName("config_items")
    val configItems: List<Map<String, Any>> = emptyList(),
    @SerializedName("url_matches")
    val urlMatches: List<String> = emptyList(),
    @SerializedName("run_at")
    val runAt: String = "DOCUMENT_END",
    @SerializedName("syntax_valid")
    val syntaxValid: Boolean = true,
    @SerializedName("security_safe")
    val securitySafe: Boolean = true
) {
    /**
     * 转换为 ExtensionModule
     */
    fun toExtensionModule(): ExtensionModule {
        val cat = try {
            ModuleCategory.valueOf(category.uppercase())
        } catch (e: Exception) {
            ModuleCategory.OTHER
        }
        
        val runTime = try {
            ModuleRunTime.valueOf(runAt.uppercase())
        } catch (e: Exception) {
            ModuleRunTime.DOCUMENT_END
        }
        
        return ExtensionModule(
            name = name,
            description = description,
            icon = icon.ifBlank { "package" },
            category = cat,
            code = jsCode,
            cssCode = cssCode,
            runAt = runTime,
            urlMatches = urlMatches.map { UrlMatchRule(it) },
            configItems = configItems.map { item ->
                ModuleConfigItem(
                    key = item["key"] as? String ?: "",
                    name = item["name"] as? String ?: "",
                    description = item["description"] as? String ?: "",
                    type = try {
                        ConfigItemType.valueOf((item["type"] as? String ?: "TEXT").uppercase())
                    } catch (e: Exception) {
                        ConfigItemType.TEXT
                    },
                    defaultValue = item["defaultValue"] as? String ?: "",
                    options = (item["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )
            },
            permissions = listOf(ModulePermission.DOM_ACCESS),
            enabled = true,
            builtIn = false
        )
    }
}

/**
 * Agent 会话上下文
 */
data class AgentSession(
    @SerializedName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("state")
    var state: AgentSessionState = AgentSessionState.IDLE,
    @SerializedName("messages")
    val messages: MutableList<AgentMessage> = mutableListOf(),
    @SerializedName("current_thoughts")
    val currentThoughts: MutableList<AgentThought> = mutableListOf(),
    @SerializedName("working_module")
    var workingModule: GeneratedModuleData? = null,
    @SerializedName("iteration_count")
    var iterationCount: Int = 0,
    @SerializedName("max_iterations")
    val maxIterations: Int = 5,
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 添加用户消息
     */
    fun addUserMessage(content: String) {
        messages.add(AgentMessage(
            role = MessageRole.USER,
            content = content
        ))
    }
    
    /**
     * 添加助手消息
     */
    fun addAssistantMessage(
        content: String,
        thoughts: List<AgentThought> = emptyList(),
        toolCalls: List<ToolCallRequest> = emptyList(),
        toolResults: List<ToolCallResult> = emptyList(),
        generatedModule: GeneratedModuleData? = null
    ) {
        messages.add(AgentMessage(
            role = MessageRole.ASSISTANT,
            content = content,
            thoughts = thoughts,
            toolCalls = toolCalls,
            toolResults = toolResults,
            generatedModule = generatedModule
        ))
    }
    
    /**
     * 添加思考步骤
     */
    fun addThought(type: ThoughtType, content: String) {
        currentThoughts.add(AgentThought(
            step = currentThoughts.size + 1,
            type = type,
            content = content
        ))
    }
    
    /**
     * 清空当前思考
     */
    fun clearCurrentThoughts() {
        currentThoughts.clear()
    }
    
    /**
     * 获取对话历史（用于 AI 上下文）
     */
    fun getConversationHistory(): List<Map<String, String>> {
        return messages.map { msg ->
            mapOf(
                "role" to when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                    MessageRole.TOOL -> "tool"
                },
                "content" to msg.content
            )
        }
    }
    
    /**
     * 获取最近的工具调用结果
     */
    fun getRecentToolResults(): List<ToolCallResult> {
        return messages.lastOrNull()?.toolResults ?: emptyList()
    }
    
    /**
     * 是否可以继续迭代
     */
    fun canContinue(): Boolean {
        return iterationCount < maxIterations && state != AgentSessionState.ERROR
    }
}

/**
 * Agent 配置
 */
data class AgentConfig(
    @SerializedName("max_iterations")
    val maxIterations: Int = 5,
    @SerializedName("auto_fix_errors")
    val autoFixErrors: Boolean = true,
    @SerializedName("auto_security_scan")
    val autoSecurityScan: Boolean = true,
    @SerializedName("verbose_thinking")
    val verboseThinking: Boolean = true,
    @SerializedName("temperature")
    val temperature: Float = 0.7f
)
