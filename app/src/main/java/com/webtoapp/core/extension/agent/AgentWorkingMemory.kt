package com.webtoapp.core.extension.agent

import com.google.gson.annotations.SerializedName

/**
 * Agent 工作记忆
 * 
 * 管理 Agent 在开发过程中的上下文信息，包括：
 * - 对话历史
 * - 当前模块状态
 * - 工具调用历史
 * - 迭代计数
 * 
 * Requirements: 5.1, 5.6
 */
data class AgentWorkingMemory(
    /**
     * 对话历史
     * 存储与 AI 的完整对话记录
     */
    @SerializedName("conversation_history")
    val conversationHistory: MutableList<AgentMessage> = mutableListOf(),
    
    /**
     * 当前正在开发的模块
     */
    @SerializedName("current_module")
    var currentModule: GeneratedModuleData? = null,
    
    /**
     * 当前需求描述
     */
    @SerializedName("current_requirement")
    var currentRequirement: String = "",
    
    /**
     * 迭代计数
     * 用于跟踪 ReAct 循环的迭代次数
     */
    @SerializedName("iteration_count")
    var iterationCount: Int = 0,
    
    /**
     * 工具调用历史
     * 记录所有工具调用的详细信息
     */
    @SerializedName("tool_call_history")
    val toolCallHistory: MutableList<ToolCallInfo> = mutableListOf(),
    
    /**
     * 最后一次错误信息
     */
    @SerializedName("last_error")
    var lastError: String? = null,
    
    /**
     * 自动修复尝试次数
     */
    @SerializedName("fix_attempt_count")
    var fixAttemptCount: Int = 0,
    
    /**
     * 最大自动修复尝试次数
     */
    @SerializedName("max_fix_attempts")
    val maxFixAttempts: Int = 3
) {
    
    /**
     * 添加用户消息
     */
    fun addUserMessage(content: String) {
        conversationHistory.add(AgentMessage(
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
     * 添加系统消息
     */
    fun addSystemMessage(content: String) {
        conversationHistory.add(AgentMessage(
            role = MessageRole.SYSTEM,
            content = content
        ))
    }
    
    /**
     * 添加工具消息
     */
    fun addToolMessage(content: String, toolResults: List<ToolCallResult> = emptyList()) {
        conversationHistory.add(AgentMessage(
            role = MessageRole.TOOL,
            content = content,
            toolResults = toolResults
        ))
    }
    
    /**
     * 记录工具调用
     */
    fun recordToolCall(toolCall: ToolCallInfo) {
        toolCallHistory.add(toolCall)
    }
    
    /**
     * 更新工具调用结果
     */
    fun updateToolCallResult(callId: String, result: ToolCallResult) {
        val index = toolCallHistory.indexOfFirst { it.callId == callId }
        if (index >= 0) {
            val original = toolCallHistory[index]
            toolCallHistory[index] = ToolCallInfo.fromResult(original, result)
        }
    }
    
    /**
     * 更新当前模块
     */
    fun updateModule(module: GeneratedModuleData) {
        currentModule = module
    }
    
    /**
     * 获取用于 AI 上下文的对话历史
     * 转换为 AI API 所需的消息格式
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
     * 获取最近的工具调用结果
     */
    fun getRecentToolResults(): List<ToolCallResult> {
        return conversationHistory.lastOrNull()?.toolResults ?: emptyList()
    }
    
    /**
     * 获取最近的工具调用信息
     */
    fun getRecentToolCalls(): List<ToolCallInfo> {
        return toolCallHistory.takeLast(5)
    }
    
    /**
     * 检查是否可以继续自动修复
     */
    fun canAttemptFix(): Boolean {
        return fixAttemptCount < maxFixAttempts
    }
    
    /**
     * 增加修复尝试计数
     */
    fun incrementFixAttempt() {
        fixAttemptCount++
    }
    
    /**
     * 重置修复尝试计数
     */
    fun resetFixAttempts() {
        fixAttemptCount = 0
    }
    
    /**
     * 增加迭代计数
     */
    fun incrementIteration() {
        iterationCount++
    }
    
    /**
     * 检查是否可以继续迭代
     */
    fun canContinueIteration(maxIterations: Int = 5): Boolean {
        return iterationCount < maxIterations
    }
    
    /**
     * 重置工作记忆
     * 清空所有状态，准备新的开发任务
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
     * 获取对话摘要
     * 用于调试和日志
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
     * 检查语法检查工具是否已被调用
     */
    fun hasSyntaxCheckBeenCalled(): Boolean {
        return toolCallHistory.any { it.toolName == "syntax_check" }
    }
    
    /**
     * 获取语法检查结果
     */
    fun getSyntaxCheckResults(): List<ToolCallInfo> {
        return toolCallHistory.filter { it.toolName == "syntax_check" }
    }
}
