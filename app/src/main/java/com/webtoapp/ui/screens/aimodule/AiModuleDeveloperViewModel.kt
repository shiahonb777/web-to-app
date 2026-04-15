package com.webtoapp.ui.screens.aimodule

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.agent.*
import com.webtoapp.data.model.AiFeature
import com.webtoapp.data.model.SavedModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// DataStore for session persistence
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_module_developer_session"
)

/**
 * AI module ViewModel
 * 
 * management AI module state
 * EnhancedAgentEngine handle
 * 
 * Requirements: 2.1, 4.8, 8.6
 */
class AiModuleDeveloperViewModel(
    private val application: Application,
    private val extensionManager: ExtensionManager,
) : ViewModel() {
    
    // Note
    private val aiConfigManager = AiConfigManager(application)
    private val agentEngine = EnhancedAgentEngine(application)
    private val gson = com.webtoapp.util.GsonProvider.gson
    
    // DataStore keys for session persistence
    private object SessionKeys {
        val USER_INPUT = stringPreferencesKey("user_input")
        val SELECTED_CATEGORY = stringPreferencesKey("selected_category")
        val MESSAGES = stringPreferencesKey("messages")
        val GENERATED_MODULE = stringPreferencesKey("generated_module")
        val EDITED_JS_CODE = stringPreferencesKey("edited_js_code")
        val EDITED_CSS_CODE = stringPreferencesKey("edited_css_code")
        val HAS_INTERRUPTED_SESSION = booleanPreferencesKey("has_interrupted_session")
        val LAST_SESSION_TIMESTAMP = longPreferencesKey("last_session_timestamp")
    }
    
    // UI state
    private val _uiState = MutableStateFlow(AiModuleDeveloperUiState())
    val uiState: StateFlow<AiModuleDeveloperUiState> = _uiState.asStateFlow()
    
    // Note
    private val _events = MutableSharedFlow<AiModuleDeveloperEvent>()
    val events: SharedFlow<AiModuleDeveloperEvent> = _events.asSharedFlow()
    
    // Yes session
    private val _hasInterruptedSession = MutableStateFlow(false)
    val hasInterruptedSession: StateFlow<Boolean> = _hasInterruptedSession.asStateFlow()
    
    init {
        // Load
        loadAvailableModels()
        // Loaddefault
        loadDefaultModel()
        // Check session
        checkForInterruptedSession()
    }
    
    /**
     * loadsupportmodule list
     */
    private fun loadAvailableModels() {
        viewModelScope.launch {
            aiConfigManager.getModelsByFeature(AiFeature.MODULE_DEVELOPMENT)
                .collect { models ->
                    _uiState.update { it.copy(availableModels = models) }
                }
        }
    }
    
    /**
     * loaddefault
     */
    private fun loadDefaultModel() {
        viewModelScope.launch {
            aiConfigManager.getDefaultModelForFeature(AiFeature.MODULE_DEVELOPMENT)
                .collect { model ->
                    if (_uiState.value.selectedModel == null && model != null) {
                        _uiState.update { it.copy(selectedModel = model) }
                    }
                }
        }
    }
    
    // ==================== userinput ====================
    
    /**
     * updateuserinput
     */
    fun updateUserInput(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }
    
    /**
     * select
     */
    fun selectModel(model: SavedModel) {
        _uiState.update { it.copy(selectedModel = model, showModelSelector = false) }
        // select
        viewModelScope.launch {
            aiConfigManager.setDefaultModel(model.id)
        }
    }
    
    /**
     * switch select display
     */
    fun toggleModelSelector(show: Boolean) {
        _uiState.update { it.copy(showModelSelector = show) }
    }
    
    /**
     * Select category
     */
    fun selectCategory(category: ModuleCategory?) {
        _uiState.update { it.copy(selectedCategory = category, showCategorySelector = false) }
    }
    
    /**
     * switch select display
     */
    fun toggleCategorySelector(show: Boolean) {
        _uiState.update { it.copy(showCategorySelector = show) }
    }
    
    /**
     * switchhelpdialogdisplay
     */
    fun toggleHelpDialog(show: Boolean) {
        _uiState.update { it.copy(showHelpDialog = show) }
    }
    
    // Note
    
    /**
     * Note
     */
    fun startDevelopment() {
        val state = _uiState.value
        
        if (state.userInput.isBlank()) {
            viewModelScope.launch {
                _events.emit(AiModuleDeveloperEvent.ShowToast("请输入功能需求"))
            }
            return
        }
        
        // usermessage list
        val userMessage = ConversationMessage(
            role = MessageRole.USER,
            content = state.userInput
        )
        
        _uiState.update { 
            it.copy(
                messages = it.messages + userMessage,
                isStreaming = true,
                streamingContent = "",
                thinkingContent = "",
                currentToolCalls = emptyList(),
                error = null,
                shouldAutoScroll = true
            )
        }
        
        // Savesessionstate( for)
        saveSessionState()
        
        // Start
        viewModelScope.launch {
            agentEngine.developWithStream(
                requirement = state.userInput,
                model = state.selectedModel,
                category = state.selectedCategory
            ).collect { event ->
                handleAgentStreamEvent(event)
            }
        }
    }

    
    /**
     * handle Agent
     * 
     * Requirements: 2.1, 4.8
     */
    private suspend fun handleAgentStreamEvent(event: AgentStreamEvent) {
        when (event) {
            is AgentStreamEvent.StateChange -> {
                _uiState.update { it.copy(agentState = event.state) }
            }
            
            is AgentStreamEvent.Thinking -> {
                _uiState.update { 
                    it.copy(thinkingContent = event.fullContent)
                }
                _events.emit(AiModuleDeveloperEvent.ScrollToBottom)
            }
            
            is AgentStreamEvent.Content -> {
                _uiState.update { 
                    it.copy(streamingContent = event.fullContent)
                }
                _events.emit(AiModuleDeveloperEvent.ScrollToBottom)
            }
            
            is AgentStreamEvent.ToolStart -> {
                _uiState.update { 
                    it.copy(currentToolCalls = it.currentToolCalls + event.toolCall)
                }
                _events.emit(AiModuleDeveloperEvent.ScrollToBottom)
            }
            
            is AgentStreamEvent.ToolComplete -> {
                _uiState.update { state ->
                    val updatedToolCalls = state.currentToolCalls.map { toolCall ->
                        if (toolCall.callId == event.toolCall.callId) {
                            event.toolCall
                        } else {
                            toolCall
                        }
                    }
                    state.copy(currentToolCalls = updatedToolCalls)
                }
            }
            
            is AgentStreamEvent.ModuleGenerated -> {
                _uiState.update { 
                    it.copy(
                        generatedModule = event.module,
                        editedJsCode = event.module.jsCode,
                        editedCssCode = event.module.cssCode,
                        hasEdits = false
                    )
                }
                _events.emit(AiModuleDeveloperEvent.ScrollToBottom)
            }
            
            is AgentStreamEvent.Error -> {
                val errorInfo = ErrorInfo(
                    message = event.message,
                    code = event.code,
                    recoverable = event.recoverable,
                    rawResponse = event.rawResponse
                )
                _uiState.update { 
                    it.copy(
                        error = errorInfo,
                        isStreaming = false,
                        agentState = AgentState.ERROR
                        // clear userInput, userinput( Requirements: 8. 5)
                    )
                }
                // Savesessionstate( Requirements: 8. 6)
                saveSessionState()
                _events.emit(AiModuleDeveloperEvent.ShowToast(event.message))
            }
            
            is AgentStreamEvent.Completed -> {
                // Create AI message
                val state = _uiState.value
                val assistantMessage = ConversationMessage(
                    role = MessageRole.ASSISTANT,
                    content = state.streamingContent,
                    thinkingContent = state.thinkingContent.takeIf { it.isNotBlank() },
                    toolCalls = state.currentToolCalls,
                    generatedModule = event.module,
                    isStreaming = false
                )
                
                _uiState.update { 
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isStreaming = false,
                        streamingContent = "",
                        thinkingContent = "",
                        userInput = "", // input
                        generatedModule = event.module,
                        editedJsCode = event.module.jsCode,
                        editedCssCode = event.module.cssCode,
                        hasEdits = false,
                        agentState = AgentState.COMPLETED
                    )
                }
                // Savesessionstate( module)
                saveSessionState()
                _events.emit(AiModuleDeveloperEvent.ShowToast("模块生成成功"))
            }
        }
    }
    
    // ==================== codeedit ====================
    
    /**
     * update JavaScript code
     */
    fun updateJsCode(code: String) {
        _uiState.update { 
            it.copy(
                editedJsCode = code,
                hasEdits = code != it.generatedModule?.jsCode
            )
        }
    }
    
    /**
     * update CSS code
     */
    fun updateCssCode(code: String) {
        _uiState.update { 
            it.copy(
                editedCssCode = code,
                hasEdits = code != it.generatedModule?.cssCode
            )
        }
    }
    
    /**
     * verifycode
     */
    fun validateCode() {
        val state = _uiState.value
        val jsCode = state.editedJsCode.ifBlank { state.generatedModule?.jsCode ?: "" }
        
        if (jsCode.isBlank()) {
            viewModelScope.launch {
                _events.emit(AiModuleDeveloperEvent.ShowToast("没有代码可验证"))
            }
            return
        }
        
        viewModelScope.launch {
            agentEngine.performSyntaxCheckAndAutoFix(jsCode)
                .collect { event ->
                    handleAgentStreamEvent(event)
                }
        }
    }
    
    /**
     * code
     */
    fun copyCode(code: String) {
        viewModelScope.launch {
            _events.emit(AiModuleDeveloperEvent.CopyToClipboard(code))
            _events.emit(AiModuleDeveloperEvent.ShowToast("已复制到剪贴板"))
        }
    }
    
    // ==================== modulesave ====================
    
    /**
     * savemodule
     * 
     * Requirements: 7. 7- edit code
     */
    fun saveModule(onSuccess: (ExtensionModule) -> Unit) {
        val state = _uiState.value
        val module = state.generatedModule ?: return
        
        // Getsave code( prefer edit code)
        val (jsCode, cssCode) = state.getCodeForSave()
        
        // Create module
        val finalModule = module.copy(
            jsCode = jsCode,
            cssCode = cssCode
        ).toExtensionModule()
        
        viewModelScope.launch {
            extensionManager.addModule(finalModule)
                .onSuccess { savedModule ->
                    _events.emit(AiModuleDeveloperEvent.ShowToast("模块已保存"))
                    _events.emit(AiModuleDeveloperEvent.ModuleCreated(savedModule.id))
                    onSuccess(savedModule)
                }
                .onFailure { e ->
                    _events.emit(AiModuleDeveloperEvent.ShowToast("保存失败: ${e.message}"))
                }
        }
    }
    
    // ==================== reset ====================
    
    /**
     * resetstate
     */
    fun resetState() {
        agentEngine.reset()
        _uiState.update { 
            AiModuleDeveloperUiState(
                selectedModel = it.selectedModel,
                availableModels = it.availableModels
            )
        }
        // clearsave sessionstate
        viewModelScope.launch {
            clearSessionState()
        }
    }
    
    /**
     * Note
     * 
     * Requirements: 8. 4, 8. 5- userinputand
     */
    fun retry() {
        val state = _uiState.value
        // userinput, clearerrorstate
        _uiState.update { 
            it.copy(
                error = null,
                agentState = AgentState.IDLE,
                isStreaming = false
            )
        }
        // if userinput,
        if (state.userInput.isNotBlank()) {
            startDevelopment()
        }
    }
    
    /**
     * Note
     * 
     * Requirements: 8. 4
     */
    fun retryWithDifferentModel() {
        val state = _uiState.value
        val currentModel = state.selectedModel
        val availableModels = state.availableModels
        
        // Note
        val currentIndex = availableModels.indexOfFirst { it.id == currentModel?.id }
        val nextModel = if (currentIndex >= 0 && availableModels.size > 1) {
            availableModels[(currentIndex + 1) % availableModels.size]
        } else {
            availableModels.firstOrNull { it.id != currentModel?.id }
        }
        
        if (nextModel != null) {
            // switch and
            _uiState.update { 
                it.copy(
                    selectedModel = nextModel,
                    error = null,
                    agentState = AgentState.IDLE,
                    isStreaming = false
                )
            }
            
            viewModelScope.launch {
                _events.emit(AiModuleDeveloperEvent.ShowToast("已切换到 ${nextModel.alias ?: nextModel.model.name}"))
            }
            
            // if userinput,
            if (state.userInput.isNotBlank()) {
                startDevelopment()
            }
        } else {
            viewModelScope.launch {
                _events.emit(AiModuleDeveloperEvent.ShowToast("没有其他可用模型"))
            }
        }
    }
    
    /**
     * clearerrorstate
     * 
     * Requirements: 8. 5- userinput
     */
    fun dismissError() {
        _uiState.update { 
            it.copy(
                error = null,
                agentState = AgentState.IDLE
                // clear userInput, userinput
            )
        }
    }
    
    /**
     * userinput
     * forfrom usermessage input
     * 
     * Requirements: 8.5
     */
    fun restoreLastUserInput() {
        val lastUserMessage = _uiState.value.messages
            .lastOrNull { it.role == MessageRole.USER }
        
        if (lastUserMessage != null) {
            _uiState.update { 
                it.copy(userInput = lastUserMessage.content)
            }
        }
    }
    
    // ==================== scroll ====================
    
    /**
     * settings scroll
     */
    fun setAutoScroll(enabled: Boolean) {
        _uiState.update { it.copy(shouldAutoScroll = enabled) }
    }
    
    /**
     * clearerrorstate
     * 
     * Requirements: 8. 4- usercloseerrorhint
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * AI settings
     */
    fun navigateToAiSettings() {
        viewModelScope.launch {
            _events.emit(AiModuleDeveloperEvent.NavigateToAiSettings)
        }
    }
    
    // ==================== session ====================
    
    /**
     * check session
     * 
     * Requirements: 8.6
     */
    private fun checkForInterruptedSession() {
        viewModelScope.launch {
            application.sessionDataStore.data.first().let { prefs ->
                val hasSession = prefs[SessionKeys.HAS_INTERRUPTED_SESSION] ?: false
                val lastTimestamp = prefs[SessionKeys.LAST_SESSION_TIMESTAMP] ?: 0L
                
                // 24 session
                val isRecent = System.currentTimeMillis() - lastTimestamp < 24 * 60 * 60 * 1000
                
                _hasInterruptedSession.value = hasSession && isRecent
            }
        }
    }
    
    /**
     * savecurrentsessionstate
     * call,
     * 
     * Requirements: 8.6
     */
    private fun saveSessionState() {
        viewModelScope.launch {
            val state = _uiState.value
            
            application.sessionDataStore.edit { prefs ->
                prefs[SessionKeys.USER_INPUT] = state.userInput
                prefs[SessionKeys.SELECTED_CATEGORY] = state.selectedCategory?.name ?: ""
                prefs[SessionKeys.MESSAGES] = gson.toJson(state.messages)
                prefs[SessionKeys.GENERATED_MODULE] = state.generatedModule?.let { gson.toJson(it) } ?: ""
                prefs[SessionKeys.EDITED_JS_CODE] = state.editedJsCode
                prefs[SessionKeys.EDITED_CSS_CODE] = state.editedCssCode
                prefs[SessionKeys.HAS_INTERRUPTED_SESSION] = state.isDeveloping || state.messages.isNotEmpty()
                prefs[SessionKeys.LAST_SESSION_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }
    
    /**
     * session
     * 
     * Requirements: 8.6
     */
    fun resumeInterruptedSession() {
        viewModelScope.launch {
            try {
                application.sessionDataStore.data.first().let { prefs ->
                    val userInput = prefs[SessionKeys.USER_INPUT] ?: ""
                    val categoryName = prefs[SessionKeys.SELECTED_CATEGORY] ?: ""
                    val messagesJson = prefs[SessionKeys.MESSAGES] ?: "[]"
                    val moduleJson = prefs[SessionKeys.GENERATED_MODULE] ?: ""
                    val editedJsCode = prefs[SessionKeys.EDITED_JS_CODE] ?: ""
                    val editedCssCode = prefs[SessionKeys.EDITED_CSS_CODE] ?: ""
                    
                    // Parsemessagelist
                    val messagesType = object : TypeToken<List<ConversationMessage>>() {}.type
                    val messages: List<ConversationMessage> = try {
                        gson.fromJson(messagesJson, messagesType) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    // Parse module
                    val generatedModule: GeneratedModuleData? = if (moduleJson.isNotBlank()) {
                        try {
                            gson.fromJson(moduleJson, GeneratedModuleData::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                    
                    // Parse
                    val category = if (categoryName.isNotBlank()) {
                        try {
                            ModuleCategory.valueOf(categoryName)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                    
                    // state
                    _uiState.update { 
                        it.copy(
                            userInput = userInput,
                            selectedCategory = category,
                            messages = messages,
                            generatedModule = generatedModule,
                            editedJsCode = editedJsCode.ifBlank { generatedModule?.jsCode ?: "" },
                            editedCssCode = editedCssCode.ifBlank { generatedModule?.cssCode ?: "" },
                            hasEdits = editedJsCode.isNotBlank() || editedCssCode.isNotBlank(),
                            agentState = AgentState.IDLE
                        )
                    }
                    
                    // clear
                    _hasInterruptedSession.value = false
                    
                    _events.emit(AiModuleDeveloperEvent.ShowToast("已恢复上次会话"))
                }
            } catch (e: Exception) {
                _events.emit(AiModuleDeveloperEvent.ShowToast("恢复会话失败: ${e.message}"))
            }
        }
    }
    
    /**
     * session
     * 
     * Requirements: 8.6
     */
    fun discardInterruptedSession() {
        viewModelScope.launch {
            clearSessionState()
            _hasInterruptedSession.value = false
            _events.emit(AiModuleDeveloperEvent.ShowToast("已放弃上次会话"))
        }
    }
    
    /**
     * clearsave sessionstate
     */
    private suspend fun clearSessionState() {
        application.sessionDataStore.edit { prefs ->
            prefs[SessionKeys.USER_INPUT] = ""
            prefs[SessionKeys.SELECTED_CATEGORY] = ""
            prefs[SessionKeys.MESSAGES] = "[]"
            prefs[SessionKeys.GENERATED_MODULE] = ""
            prefs[SessionKeys.EDITED_JS_CODE] = ""
            prefs[SessionKeys.EDITED_CSS_CODE] = ""
            prefs[SessionKeys.HAS_INTERRUPTED_SESSION] = false
            prefs[SessionKeys.LAST_SESSION_TIMESTAMP] = 0L
        }
    }
    
    /**
     * ViewModel
     */
    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext as Application

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AiModuleDeveloperViewModel::class.java)) {
                val extensionManager: ExtensionManager =
                    org.koin.java.KoinJavaComponent.get(ExtensionManager::class.java)
                return AiModuleDeveloperViewModel(appContext, extensionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
