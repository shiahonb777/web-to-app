package com.webtoapp.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.extension.*
import com.webtoapp.core.extension.agent.*
import com.webtoapp.data.model.AiFeature
import com.webtoapp.data.model.SavedModel
import com.webtoapp.ui.components.aimodule.filterModelsForModuleDevelopment
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// DataStore for module developer preferences
private val Context.moduleDeveloperDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "module_developer_prefs"
)

/**
 * AI 模块开发器 ViewModel
 * 
 * 管理 Agent 开发流程的状态
 */
class AiModuleDeveloperViewModel(application: Application) : AndroidViewModel(application) {
    
    private val agentEngine = ModuleAgentEngine(application)
    private val extensionManager = ExtensionManager.getInstance(application)
    private val aiConfigManager = AiConfigManager(application)
    
    companion object {
        private val KEY_SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
    }
    
    // UI 状态
    private val _uiState = MutableStateFlow(AiDeveloperUiState())
    val uiState: StateFlow<AiDeveloperUiState> = _uiState.asStateFlow()
    
    // Agent 状态
    val agentState = agentEngine.sessionState
    
    // 可用模型列表（支持 MODULE_DEVELOPMENT 的模型）
    val availableModels: StateFlow<List<SavedModel>> = aiConfigManager.savedModelsFlow
        .map { models -> filterModelsForModuleDevelopment(models) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // 选中的模型
    private val _selectedModel = MutableStateFlow<SavedModel?>(null)
    val selectedModel: StateFlow<SavedModel?> = _selectedModel.asStateFlow()
    
    init {
        // Load持久化的模型选择
        loadSelectedModel()
    }
    
    /**
     * 从 DataStore 加载持久化的模型选择
     */
    private fun loadSelectedModel() {
        viewModelScope.launch {
            // 组合持久化的模型 ID 和可用模型列表
            combine(
                getApplication<Application>().moduleDeveloperDataStore.data
                    .map { prefs -> prefs[KEY_SELECTED_MODEL_ID] },
                availableModels
            ) { savedModelId, models ->
                // 尝试找到保存的模型
                val savedModel = savedModelId?.let { id ->
                    models.find { it.id == id }
                }
                // 如果没有保存的模型或保存的模型不可用，使用默认模型
                savedModel ?: models.find { it.isDefault } ?: models.firstOrNull()
            }.collect { model ->
                _selectedModel.value = model
            }
        }
    }
    
    /**
     * 选择模型并持久化
     */
    fun selectModel(model: SavedModel) {
        _selectedModel.value = model
        viewModelScope.launch {
            saveSelectedModelId(model.id)
        }
    }
    
    /**
     * 保存选中的模型 ID 到 DataStore
     */
    private suspend fun saveSelectedModelId(modelId: String) {
        getApplication<Application>().moduleDeveloperDataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL_ID] = modelId
        }
    }
    
    /**
     * 获取持久化的模型 ID
     */
    fun getPersistedModelId(): Flow<String?> {
        return getApplication<Application>().moduleDeveloperDataStore.data
            .map { prefs -> prefs[KEY_SELECTED_MODEL_ID] }
    }
    
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
                    onError(e.message ?: "Save failed")
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
