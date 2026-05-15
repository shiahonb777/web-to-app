package com.webtoapp.core.ai.v2.agent

import com.webtoapp.core.ai.v2.tool.ToolFileChange
import com.webtoapp.core.ai.v2.tool.ToolResult

sealed class AgentEvent {
    object Started : AgentEvent()
    data class TextDelta(val delta: String, val accumulated: String) : AgentEvent()
    data class ThinkingDelta(val delta: String, val accumulated: String) : AgentEvent()
    data class ToolStarted(val toolCallId: String, val name: String) : AgentEvent()
    data class ToolArgsDelta(val toolCallId: String, val delta: String) : AgentEvent()
    data class ToolFinished(val toolCallId: String, val name: String, val arguments: String, val result: ToolResult) : AgentEvent()
    data class FileChanged(val change: ToolFileChange) : AgentEvent()
    object FallbackTriggered : AgentEvent()
    data class Completed(val summaryText: String, val toolCallCount: Int) : AgentEvent()
    data class Failed(val message: String) : AgentEvent()
}
