package com.webtoapp.core.extension.agent

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.extension.*

/**
 * Agent 上下文管理
 * 
 * 管理 Agent 的对话历史、工作状态、生成的代码等上下文信息
 */

/**
 * Agent 会话状态
 */
enum class AgentSessionState {
    IDLE,           // 空闲
    THINKING,       // 思考中
    PLANNING,       // 规划中
    EXECUTING,      // 执行工具中
    GENERATING,     // 生成代码中
    REVIEWING,      // 审查代码中
    FIXING,         // 修复错误中
    COMPLETED,      // 完成
    ERROR           // 错误
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
enum class ThoughtType(val displayName: String, val icon: String) {
    ANALYSIS("需求分析", "🔍"),
    PLANNING("制定计划", "📋"),
    TOOL_CALL("调用工具", "🔧"),
    TOOL_RESULT("工具结果", "📊"),
    GENERATION("生成代码", "✨"),
    REVIEW("代码审查", "👁️"),
    FIX("修复问题", "🩹"),
    CONCLUSION("总结", "✅"),
    ERROR("错误", "❌")
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
    USER,       // 用户消息
    ASSISTANT,  // AI 助手消息
    SYSTEM,     // 系统消息
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
            icon = icon.ifBlank { "📦" },
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
