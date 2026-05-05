package com.webtoapp.ui.screens.aimodule

import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.agent.*
import com.webtoapp.data.model.SavedModel








data class AiModuleDeveloperUiState(




    val selectedModel: SavedModel? = null,




    val availableModels: List<SavedModel> = emptyList(),




    val showModelSelector: Boolean = false,





    val userInput: String = "",




    val selectedCategory: ModuleCategory? = null,




    val showCategorySelector: Boolean = false,





    val messages: List<ConversationMessage> = emptyList(),




    val isStreaming: Boolean = false,




    val streamingContent: String = "",




    val thinkingContent: String = "",





    val currentToolCalls: List<ToolCallInfo> = emptyList(),





    val generatedModule: GeneratedModuleData? = null,




    val editedJsCode: String = "",




    val editedCssCode: String = "",




    val hasEdits: Boolean = false,





    val agentState: AgentState = AgentState.IDLE,




    val error: ErrorInfo? = null,





    val shouldAutoScroll: Boolean = true,





    val showHelpDialog: Boolean = false
) {



    val isDeveloping: Boolean
        get() = agentState != AgentState.IDLE &&
                agentState != AgentState.COMPLETED &&
                agentState != AgentState.ERROR




    val canSend: Boolean
        get() = userInput.isNotBlank() && !isDeveloping




    val showWelcome: Boolean
        get() = messages.isEmpty() && generatedModule == null && error == null && !isDeveloping




    fun getCodeForSave(): Pair<String, String> {
        val jsCode = if (hasEdits && editedJsCode.isNotBlank()) {
            editedJsCode
        } else {
            generatedModule?.jsCode ?: ""
        }

        val cssCode = if (hasEdits && editedCssCode.isNotBlank()) {
            editedCssCode
        } else {
            generatedModule?.cssCode ?: ""
        }

        return jsCode to cssCode
    }
}






data class ConversationMessage(



    val id: String = java.util.UUID.randomUUID().toString(),




    val role: MessageRole,




    val content: String,




    val thinkingContent: String? = null,




    val toolCalls: List<ToolCallInfo> = emptyList(),




    val generatedModule: GeneratedModuleData? = null,




    val timestamp: Long = System.currentTimeMillis(),




    val isStreaming: Boolean = false
)






data class ErrorInfo(



    val message: String,




    val code: String? = null,




    val recoverable: Boolean = true,




    val rawResponse: String? = null
)






sealed class AiModuleDeveloperEvent {



    data class ShowToast(val message: String) : AiModuleDeveloperEvent()




    object ScrollToBottom : AiModuleDeveloperEvent()




    data class ModuleCreated(val moduleId: String) : AiModuleDeveloperEvent()




    object NavigateToAiSettings : AiModuleDeveloperEvent()




    data class CopyToClipboard(val content: String) : AiModuleDeveloperEvent()
}
