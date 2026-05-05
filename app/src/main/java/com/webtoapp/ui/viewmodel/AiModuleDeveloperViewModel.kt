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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.extension.*
import com.webtoapp.core.extension.agent.*
import com.webtoapp.data.model.SavedModel
import com.webtoapp.ui.components.aimodule.filterModelsForModuleDevelopment
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


private val Context.moduleDeveloperDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "module_developer_prefs"
)






class AiModuleDeveloperViewModel(application: Application) : AndroidViewModel(application) {

    private val agentEngine = ModuleAgentEngine(application)
    private val extensionManager = ExtensionManager.getInstance(application)
    private val aiConfigManager = AiConfigManager(application)

    companion object {
        private val KEY_SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
    }


    private val _uiState = MutableStateFlow(AiDeveloperUiState())
    val uiState: StateFlow<AiDeveloperUiState> = _uiState.asStateFlow()


    val agentState = agentEngine.sessionState


    val availableModels: StateFlow<List<SavedModel>> = aiConfigManager.savedModelsFlow
        .map { models -> filterModelsForModuleDevelopment(models) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    private val _selectedModel = MutableStateFlow<SavedModel?>(null)
    val selectedModel: StateFlow<SavedModel?> = _selectedModel.asStateFlow()

    init {

        loadSelectedModel()
    }




    private fun loadSelectedModel() {
        viewModelScope.launch {

            combine(
                getApplication<Application>().moduleDeveloperDataStore.data
                    .map { prefs -> prefs[KEY_SELECTED_MODEL_ID] },
                availableModels
            ) { savedModelId, models ->

                val savedModel = savedModelId?.let { id ->
                    models.find { it.id == id }
                }

                savedModel ?: models.find { it.isDefault } ?: models.firstOrNull()
            }.collect { model ->
                _selectedModel.value = model
            }
        }
    }




    fun selectModel(model: SavedModel) {
        _selectedModel.value = model
        viewModelScope.launch {
            saveSelectedModelId(model.id)
        }
    }




    private suspend fun saveSelectedModelId(modelId: String) {
        getApplication<Application>().moduleDeveloperDataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL_ID] = modelId
        }
    }




    fun getPersistedModelId(): Flow<String?> {
        return getApplication<Application>().moduleDeveloperDataStore.data
            .map { prefs -> prefs[KEY_SELECTED_MODEL_ID] }
    }




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
                    onError(e.message ?: Strings.saveFailed)
                }
        }
    }




    fun reset() {
        _uiState.value = AiDeveloperUiState()
    }




    fun updateInput(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }




    fun updateCategory(category: ModuleCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}




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
