package com.webtoapp.core.extension.agent

import com.google.gson.annotations.SerializedName












data class AgentWorkingMemory(




    @SerializedName("conversation_history")
    val conversationHistory: MutableList<AgentMessage> = mutableListOf(),




    @SerializedName("current_module")
    var currentModule: GeneratedModuleData? = null,




    @SerializedName("current_requirement")
    var currentRequirement: String = "",





    @SerializedName("iteration_count")
    var iterationCount: Int = 0,





    @SerializedName("tool_call_history")
    val toolCallHistory: MutableList<ToolCallInfo> = mutableListOf(),




    @SerializedName("last_error")
    var lastError: String? = null,




    @SerializedName("fix_attempt_count")
    var fixAttemptCount: Int = 0,




    @SerializedName("max_fix_attempts")
    val maxFixAttempts: Int = 3
) {




    fun addUserMessage(content: String) {
        conversationHistory.add(AgentMessage(
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
        conversationHistory.add(AgentMessage(
            role = MessageRole.ASSISTANT,
            content = content,
            thoughts = thoughts,
            toolCalls = toolCalls,
            toolResults = toolResults,
            generatedModule = generatedModule
        ))
    }




    fun addSystemMessage(content: String) {
        conversationHistory.add(AgentMessage(
            role = MessageRole.SYSTEM,
            content = content
        ))
    }




    fun addToolMessage(content: String, toolResults: List<ToolCallResult> = emptyList()) {
        conversationHistory.add(AgentMessage(
            role = MessageRole.TOOL,
            content = content,
            toolResults = toolResults
        ))
    }




    fun recordToolCall(toolCall: ToolCallInfo) {
        toolCallHistory.add(toolCall)
    }




    fun updateToolCallResult(callId: String, result: ToolCallResult) {
        val index = toolCallHistory.indexOfFirst { it.callId == callId }
        if (index >= 0) {
            val original = toolCallHistory[index]
            toolCallHistory[index] = ToolCallInfo.fromResult(original, result)
        }
    }




    fun updateModule(module: GeneratedModuleData) {
        currentModule = module
    }





    fun getContextForAi(): List<Map<String, String>> {
        return conversationHistory.map { msg ->
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
        return conversationHistory.lastOrNull()?.toolResults ?: emptyList()
    }




    fun getRecentToolCalls(): List<ToolCallInfo> {
        return toolCallHistory.takeLast(5)
    }




    fun canAttemptFix(): Boolean {
        return fixAttemptCount < maxFixAttempts
    }




    fun incrementFixAttempt() {
        fixAttemptCount++
    }




    fun resetFixAttempts() {
        fixAttemptCount = 0
    }




    fun incrementIteration() {
        iterationCount++
    }




    fun canContinueIteration(maxIterations: Int = 5): Boolean {
        return iterationCount < maxIterations
    }





    fun reset() {
        conversationHistory.clear()
        currentModule = null
        currentRequirement = ""
        iterationCount = 0
        toolCallHistory.clear()
        lastError = null
        fixAttemptCount = 0
    }





    fun getSummary(): String {
        return buildString {
            appendLine("=== Agent Working Memory Summary ===")
            appendLine("Requirement: ${currentRequirement.take(100)}...")
            appendLine("Conversation messages: ${conversationHistory.size}")
            appendLine("Tool calls: ${toolCallHistory.size}")
            appendLine("Iterations: $iterationCount")
            appendLine("Fix attempts: $fixAttemptCount")
            appendLine("Current module: ${currentModule?.name ?: "None"}")
            appendLine("Last error: ${lastError ?: "None"}")
        }
    }




    fun hasSyntaxCheckBeenCalled(): Boolean {
        return toolCallHistory.any { it.toolName == "syntax_check" }
    }




    fun getSyntaxCheckResults(): List<ToolCallInfo> {
        return toolCallHistory.filter { it.toolName == "syntax_check" }
    }
}
