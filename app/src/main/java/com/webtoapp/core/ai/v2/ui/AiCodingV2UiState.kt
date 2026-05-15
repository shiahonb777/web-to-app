package com.webtoapp.core.ai.v2.ui

import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.coding.ProjectFileInfo
import com.webtoapp.core.ai.v2.data.AgentSession
import com.webtoapp.core.ai.v2.data.RecordedToolCall

data class AiCodingV2UiState(
    val phase: Phase = Phase.Idle,
    val sessions: List<AgentSession> = emptyList(),
    val currentSession: AgentSession? = null,
    val codingType: AiCodingType = AiCodingType.HTML,
    val projectFiles: List<ProjectFileInfo> = emptyList(),
    val selectedFilePath: String? = null,
    val selectedFileContent: String? = null,
    val streamingText: String = "",
    val streamingThinking: String = "",
    val pendingToolCalls: List<RecordedToolCall> = emptyList(),
    val showFallbackHint: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val configState: ConfigState = ConfigState()
) {
    enum class Phase { Idle, Submitting, Streaming, AwaitingTool, Error }
    val canSend: Boolean get() = phase == Phase.Idle && configState.textModelId != null
}

data class ConfigState(
    val textModelId: String? = null,
    val imageModelId: String? = null,
    val temperature: Float = 0.7f,
    val rules: List<String> = emptyList(),
    val maxTurns: Int = 6,
    val availableTextModelIds: List<Pair<String, String>> = emptyList(),
    val availableImageModelIds: List<Pair<String, String>> = emptyList()
)
