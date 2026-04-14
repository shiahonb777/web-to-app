package com.webtoapp.core.extension.agent

import com.google.gson.annotations.SerializedName

/**
 * Agent working memory.
 *
 * manage Agent in in .
 * - chat history.
 * - current module.
 * - use.
 * - iteration count.
 *
 * Requirements: 5.1, 5.6
 */
data class AgentWorkingMemory(
    /**
     * chat history.
     * and AI.
     */
    @SerializedName("conversation_history")
    val conversationHistory: MutableList<AgentMessage> = mutableListOf(),
    
    /**
     * before in.
     */
    @SerializedName("current_module")
    var currentModule: GeneratedModuleData? = null,
    
    /**
     * before.
     */
    @SerializedName("current_requirement")
    var currentRequirement: String = "",
    
    /**
     * iteration count.
     * use ReAct.
     */
    @SerializedName("iteration_count")
    var iterationCount: Int = 0,
    
    /**
     * use.
     * use.
     */
    @SerializedName("tool_call_history")
    val toolCallHistory: MutableList<ToolCallInfo> = mutableListOf(),
    
    /**
     * last error.
     */
    @SerializedName("last_error")
    var lastError: String? = null,
    
    /**
     * auto-fix.
     */
    @SerializedName("fix_attempt_count")
    var fixAttemptCount: Int = 0,
    
    /**
     * large auto-fix.
     */
    @SerializedName("max_fix_attempts")
    val maxFixAttempts: Int = 3
) {
    
    /**
     * Add user message.
     */
    fun addUserMessage(content: String) {
        conversationHistory.add(AgentMessage(
            role = MessageRole.USER,
            content = content
        ))
    }
    
    /**
     * Add assistant message.
     */
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
    
    /**
     * Add system message.
     */
    fun addSystemMessage(content: String) {
        conversationHistory.add(AgentMessage(
            role = MessageRole.SYSTEM,
            content = content
        ))
    }
    
    /**
     * Add tool message.
     */
    fun addToolMessage(content: String, toolResults: List<ToolCallResult> = emptyList()) {
        conversationHistory.add(AgentMessage(
            role = MessageRole.TOOL,
            content = content,
            toolResults = toolResults
        ))
    }
    
    /**
     * Record tool call.
     */
    fun recordToolCall(toolCall: ToolCallInfo) {
        toolCallHistory.add(toolCall)
    }
    
    /**
     * Update tool result.
     */
    fun updateToolCallResult(callId: String, result: ToolCallResult) {
        val index = toolCallHistory.indexOfFirst { it.callId == callId }
        if (index >= 0) {
            val original = toolCallHistory[index]
            toolCallHistory[index] = ToolCallInfo.fromResult(original, result)
        }
    }
    
    /**
     * current module.
     */
    fun updateModule(module: GeneratedModuleData) {
        currentModule = module
    }
    
    /**
     * Get use AI chat history.
     * as AI API.
     */
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
    
    /**
     * Get use.
     */
    fun getRecentToolResults(): List<ToolCallResult> {
        return conversationHistory.lastOrNull()?.toolResults ?: emptyList()
    }
    
    /**
     * Get use.
     */
    fun getRecentToolCalls(): List<ToolCallInfo> {
        return toolCallHistory.takeLast(5)
    }
    
    /**
     * Check is can auto-fix.
     */
    fun canAttemptFix(): Boolean {
        return fixAttemptCount < maxFixAttempts
    }
    
    /**
     * fix.
     */
    fun incrementFixAttempt() {
        fixAttemptCount++
    }
    
    /**
     * Resetfix.
     */
    fun resetFixAttempts() {
        fixAttemptCount = 0
    }
    
    /**
     * iteration count.
     */
    fun incrementIteration() {
        iterationCount++
    }
    
    /**
     * Check is can.
     */
    fun canContinueIteration(maxIterations: Int = 5): Boolean {
        return iterationCount < maxIterations
    }
    
    /**
     * Resetworking memory.
     * .
     */
    fun reset() {
        conversationHistory.clear()
        currentModule = null
        currentRequirement = ""
        iterationCount = 0
        toolCallHistory.clear()
        lastError = null
        fixAttemptCount = 0
    }
    
    /**
     * Get.
     * use.
     */
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
    
    /**
     * Checksyntax check is use.
     */
    fun hasSyntaxCheckBeenCalled(): Boolean {
        return toolCallHistory.any { it.toolName == "syntax_check" }
    }
    
    /**
     * Getsyntax check.
     */
    fun getSyntaxCheckResults(): List<ToolCallInfo> {
        return toolCallHistory.filter { it.toolName == "syntax_check" }
    }
}
