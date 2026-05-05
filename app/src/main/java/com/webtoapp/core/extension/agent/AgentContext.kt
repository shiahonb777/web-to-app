package com.webtoapp.core.extension.agent

import com.google.gson.annotations.SerializedName
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.Strings













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
        IDLE -> Strings.agentStateIdle
        THINKING -> Strings.agentStateThinking
        GENERATING -> Strings.agentStateGenerating
        TOOL_CALLING -> Strings.agentStateToolCalling
        SYNTAX_CHECKING -> Strings.agentStateSyntaxCheck
        FIXING -> Strings.agentStateFixing
        SECURITY_SCANNING -> Strings.agentStateSecurityScan
        COMPLETED -> Strings.agentStateCompleted
        ERROR -> Strings.agentStateError
    }
}





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




enum class ToolStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED
}








sealed class AgentStreamEvent {





    data class StateChange(val state: AgentState) : AgentStreamEvent()








    data class Thinking(
        val content: String,
        val fullContent: String
    ) : AgentStreamEvent()








    data class Content(
        val delta: String,
        val fullContent: String
    ) : AgentStreamEvent()







    data class ToolStart(val toolCall: ToolCallInfo) : AgentStreamEvent()







    data class ToolComplete(val toolCall: ToolCallInfo) : AgentStreamEvent()







    data class ModuleGenerated(val module: GeneratedModuleData) : AgentStreamEvent()










    data class Error(
        val message: String,
        val code: String? = null,
        val recoverable: Boolean = true,
        val rawResponse: String? = null
    ) : AgentStreamEvent()







    data class Completed(val module: GeneratedModuleData) : AgentStreamEvent()
}






enum class AgentSessionState {
    IDLE,
    THINKING,
    PLANNING,
    EXECUTING,
    GENERATING,
    REVIEWING,
    FIXING,
    COMPLETED,
    ERROR
}




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
        ANALYSIS -> Strings.thoughtAnalysis
        PLANNING -> Strings.thoughtPlanning
        TOOL_CALL -> Strings.thoughtToolCall
        TOOL_RESULT -> Strings.thoughtToolResult
        GENERATION -> Strings.thoughtGeneration
        REVIEW -> Strings.thoughtReview
        FIX -> Strings.thoughtFix
        CONCLUSION -> Strings.thoughtConclusion
        ERROR -> Strings.agentStateError
    }
}





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




enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}




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



    fun addUserMessage(content: String) {
        messages.add(AgentMessage(
            role = MessageRole.USER,
            content = content
        ))
    }




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




    fun addThought(type: ThoughtType, content: String) {
        currentThoughts.add(AgentThought(
            step = currentThoughts.size + 1,
            type = type,
            content = content
        ))
    }




    fun clearCurrentThoughts() {
        currentThoughts.clear()
    }




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




    fun getRecentToolResults(): List<ToolCallResult> {
        return messages.lastOrNull()?.toolResults ?: emptyList()
    }




    fun canContinue(): Boolean {
        return iterationCount < maxIterations && state != AgentSessionState.ERROR
    }
}




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
