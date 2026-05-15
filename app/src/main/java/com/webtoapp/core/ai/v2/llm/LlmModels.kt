package com.webtoapp.core.ai.v2.llm

import com.google.gson.JsonElement
import com.webtoapp.data.model.AiModel
import com.webtoapp.data.model.ApiKeyConfig

data class ChatRequest(
    val apiKey: ApiKeyConfig,
    val model: AiModel,
    val messages: List<LlmMessage>,
    val tools: List<ToolDeclaration> = emptyList(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 8192,
    val useTools: Boolean = true
)

data class LlmMessage(
    val role: Role,
    val content: String = "",
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolCallId: String? = null,
    val name: String? = null
) {
    enum class Role { SYSTEM, USER, ASSISTANT, TOOL }
}

data class LlmToolCall(val id: String, val name: String, val argumentsJson: String)

data class ToolDeclaration(val name: String, val description: String, val parametersSchema: JsonElement)

sealed class LlmEvent {
    object Started : LlmEvent()
    data class TextDelta(val delta: String) : LlmEvent()
    data class ThinkingDelta(val delta: String) : LlmEvent()
    data class ToolCallBegin(val id: String, val name: String) : LlmEvent()
    data class ToolCallArgsDelta(val id: String, val argsDelta: String) : LlmEvent()
    data class ToolCallEnd(val id: String, val name: String, val argumentsJson: String) : LlmEvent()
    data class Done(val finishReason: FinishReason) : LlmEvent()
    data class Error(val message: String, val recoverable: Boolean = false) : LlmEvent()
}

enum class FinishReason { STOP, TOOL_CALLS, LENGTH, OTHER }
