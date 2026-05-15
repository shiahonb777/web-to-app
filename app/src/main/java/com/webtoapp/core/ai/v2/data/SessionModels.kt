package com.webtoapp.core.ai.v2.data

import com.webtoapp.core.ai.coding.AiCodingType
import java.util.UUID

data class AgentSession(val id: String = UUID.randomUUID().toString(), val title: String = "", val codingType: AiCodingType = AiCodingType.HTML, val messages: List<AgentMessage> = emptyList(), val config: AgentSessionConfig = AgentSessionConfig(), val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis())
data class AgentSessionConfig(val textModelId: String? = null, val imageModelId: String? = null, val temperature: Float = 0.7f, val rules: List<String> = emptyList(), val maxTurns: Int = 6)
data class AgentMessage(val id: String = UUID.randomUUID().toString(), val role: Role, val content: String, val thinking: String? = null, val toolCalls: List<RecordedToolCall> = emptyList(), val producedFiles: List<String> = emptyList(), val attachments: List<String> = emptyList(), val timestamp: Long = System.currentTimeMillis(), val isError: Boolean = false) { enum class Role { USER, ASSISTANT, SYSTEM } }
data class RecordedToolCall(val toolCallId: String, val name: String, val argumentsJson: String, val resultPreview: String, val ok: Boolean)
