package com.webtoapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.extension.*
import com.webtoapp.core.extension.agent.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AI 模块开发器 ViewModel
 * 
 * 管理 Agent 开发流程的状态
 */
class AiModuleDeveloperViewModel(application: Application) : AndroidViewModel(application) {
    
    private val agentEngine = ModuleAgentEngine(application)
    private val extensionManager = ExtensionManager.getInstance(application)
    
    // UI 状态
    private val _uiState = MutableStateFlow(AiDeveloperUiState())
    val uiState: StateFlow<AiDeveloperUiState> = _uiState.asStateFlow()
    
    // Agent 状态
    val agentState = agentEngine.sessionState
    
    /**
     * 开始开发
     */
    fun startDevelopment(
        requirement: String,
        category: ModuleCategory? = null
    ) {
        if (requirement.isBlank()) return
        
        _uiState.update { it.copy(
            isLoading = true,
            thoughts = emptyList(),
            generatedModule = null,
            errorMessage = null,
            toolResults = emptyList()
        )}
        
        viewModelScope.launch {
            agentEngine.develop(requirement, category).collect { event ->
                when (event) {
                    is AgentEvent.StateChange -> {
                        _uiState.update { it.copy(currentState = event.state) }
                    }
                    is AgentEvent.Thought -> {
                        _uiState.update { it.copy(
                            thoughts = it.thoughts + event.thought
                        )}
                    }
                    is AgentEvent.ToolResult -> {
                        _uiState.update { it.copy(
                            toolResults = it.toolResults + event.result
                        )}
                    }
                    is AgentEvent.ModuleGenerated -> {
                        _uiState.update { it.copy(
                            generatedModule = event.module
                        )}
                    }
                    is AgentEvent.Completed -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            generatedModule = event.module,
                            currentState = AgentSessionState.COMPLETED
                        )}
                    }
                    is AgentEvent.Error -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            errorMessage = event.message,
                            currentState = AgentSessionState.ERROR
                        )}
                    }
                }
            }
        }
    }

    /**
     * 保存生成的模块
     */
    fun saveModule(onSuccess: (ExtensionModule) -> Unit, onError: (String) -> Unit) {
        val moduleData = _uiState.value.generatedModule ?: return
        
        viewModelScope.launch {
            val module = moduleData.toExtensionModule()
            extensionManager.addModule(module)
                .onSuccess { savedModule ->
                    _uiState.update { it.copy(moduleSaved = true) }
                    onSuccess(savedModule)
                }
                .onFailure { e ->
                    onError(e.message ?: "保存失败")
                }
        }
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        _uiState.value = AiDeveloperUiState()
    }
    
    /**
     * 更新用户输入
     */
    fun updateInput(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }
    
    /**
     * 更新选中的分类
     */
    fun updateCategory(category: ModuleCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}

/**
 * UI 状态
 */
data class AiDeveloperUiState(
    val userInput: String = "",
    val selectedCategory: ModuleCategory? = null,
    val isLoading: Boolean = false,
    val currentState: AgentSessionState = AgentSessionState.IDLE,
    val thoughts: List<AgentThought> = emptyList(),
    val toolResults: List<ToolCallResult> = emptyList(),
    val generatedModule: GeneratedModuleData? = null,
    val errorMessage: String? = null,
    val moduleSaved: Boolean = false
) {
    val isDeveloping: Boolean
        get() = isLoading || (currentState != AgentSessionState.IDLE && 
                             currentState != AgentSessionState.COMPLETED && 
                             currentState != AgentSessionState.ERROR)
    
    val canStartDevelopment: Boolean
        get() = userInput.isNotBlank() && !isDeveloping
    
    val hasResult: Boolean
        get() = generatedModule != null || errorMessage != null
}
