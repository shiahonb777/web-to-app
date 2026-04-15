package com.webtoapp.ui.screens.aimodule

import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.agent.*
import com.webtoapp.data.model.SavedModel

/**
 * AI module UI state
 * 
 * UI state, for AiModuleDeveloperScreen display
 * 
 * Requirements: 4.1
 */
data class AiModuleDeveloperUiState(
    // ==================== select ====================
    /**
     * current in
     */
    val selectedModel: SavedModel? = null,
    
    /**
     * list( support MODULE_DEVELOPMENT)
     */
    val availableModels: List<SavedModel> = emptyList(),
    
    /**
     * display select
     */
    val showModelSelector: Boolean = false,
    
    // ==================== input ====================
    /**
     * userinput
     */
    val userInput: String = "",
    
    /**
     * inmodule
     */
    val selectedCategory: ModuleCategory? = null,
    
    /**
     * display select
     */
    val showCategorySelector: Boolean = false,
    
    // Note
    /**
     * messagelist
     */
    val messages: List<ConversationMessage> = emptyList(),
    
    /**
     * output
     */
    val isStreaming: Boolean = false,
    
    /**
     * current output content
     */
    val streamingContent: String = "",
    
    /**
     * current content
     */
    val thinkingContent: String = "",
    
    // ==================== call ====================
    /**
     * current calllist
     */
    val currentToolCalls: List<ToolCallInfo> = emptyList(),
    
    // Note
    /**
     * module
     */
    val generatedModule: GeneratedModuleData? = null,
    
    /**
     * edit JavaScript code
     */
    val editedJsCode: String = "",
    
    /**
     * edit CSS code
     */
    val editedCssCode: String = "",
    
    /**
     * save edit
     */
    val hasEdits: Boolean = false,
    
    // ==================== state ====================
    /**
     * Agent currentstate
     */
    val agentState: AgentState = AgentState.IDLE,
    
    /**
     * error
     */
    val error: ErrorInfo? = null,
    
    // ==================== scroll ====================
    /**
     * scroll bottom
     */
    val shouldAutoScroll: Boolean = true,
    
    // ==================== dialog ====================
    /**
     * displayhelpdialog
     */
    val showHelpDialog: Boolean = false
) {
    /**
     * ( , , errorstate)
     */
    val isDeveloping: Boolean
        get() = agentState != AgentState.IDLE && 
                agentState != AgentState.COMPLETED && 
                agentState != AgentState.ERROR
    
    /**
     * message
     */
    val canSend: Boolean
        get() = userInput.isNotBlank() && !isDeveloping
    
    /**
     * displaywelcome
     */
    val showWelcome: Boolean
        get() = messages.isEmpty() && generatedModule == null && error == null && !isDeveloping
    
    /**
     * save code( prefer edit code)
     */
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

/**
 * message
 * 
 * for display message
 */
data class ConversationMessage(
    /**
     * message ID
     */
    val id: String = java.util.UUID.randomUUID().toString(),
    
    /**
     * message
     */
    val role: MessageRole,
    
    /**
     * messagecontent
     */
    val content: String,
    
    /**
     * content( only AI message)
     */
    val thinkingContent: String? = null,
    
    /**
     * calllist( only AI message)
     */
    val toolCalls: List<ToolCallInfo> = emptyList(),
    
    /**
     * module( only AI message)
     */
    val generatedModule: GeneratedModuleData? = null,
    
    /**
     * Note
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * output
     */
    val isStreaming: Boolean = false
)

/**
 * error
 * 
 * showerrorstate
 */
data class ErrorInfo(
    /**
     * errormessage
     */
    val message: String,
    
    /**
     * error
     */
    val code: String? = null,
    
    /**
     * Note
     */
    val recoverable: Boolean = true,
    
    /**
     * ( for)
     */
    val rawResponse: String? = null
)

/**
 * UI
 * 
 * for ViewModel UI
 */
sealed class AiModuleDeveloperEvent {
    /**
     * display Toast message
     */
    data class ShowToast(val message: String) : AiModuleDeveloperEvent()
    
    /**
     * scroll bottom
     */
    object ScrollToBottom : AiModuleDeveloperEvent()
    
    /**
     * modulecreatesuccess
     */
    data class ModuleCreated(val moduleId: String) : AiModuleDeveloperEvent()
    
    /**
     * AI settings
     */
    object NavigateToAiSettings : AiModuleDeveloperEvent()
    
    /**
     * Note
     */
    data class CopyToClipboard(val content: String) : AiModuleDeveloperEvent()
}
