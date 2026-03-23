package com.webtoapp.ui.screens.aimodule

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
 * AI 模块开发器 ViewModel
 * 
 * 管理 AI 模块开发界面的状态和业务逻辑
 * 集成 EnhancedAgentEngine 处理流式事件
 * 
 * Requirements: 2.1, 4.8, 8.6
 */
class AiModuleDeveloperViewModel(
    private val context: Context
) : ViewModel() {
    
    // 依赖
    private val aiConfigManager = AiConfigManager(context)
    private val agentEngine = EnhancedAgentEngine(context)
    private val extensionManager = ExtensionManager.getInstance(context)
    private val gson = Gson()
    
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
    
    // UI 状态
    private val _uiState = MutableStateFlow(AiModuleDeveloperUiState())
    val uiState: StateFlow<AiModuleDeveloperUiState> = _uiState.asStateFlow()
    
    // 一次性事件
    private val _events = MutableSharedFlow<AiModuleDeveloperEvent>()
    val events: SharedFlow<AiModuleDeveloperEvent> = _events.asSharedFlow()
    
    // Yes否有可恢复的会话
    private val _hasInterruptedSession = MutableStateFlow(false)
    val hasInterruptedSession: StateFlow<Boolean> = _hasInterruptedSession.asStateFlow()
    
    init {
        // Load可用模型
        loadAvailableModels()
        // Load默认模型
        loadDefaultModel()
        // Check是否有中断的会话
        checkForInterruptedSession()
    }
    
    /**
     * 加载支持模块开发的模型列表
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
     * 加载默认模型
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
    
    // ==================== 用户输入操作 ====================
    
    /**
     * 更新用户输入
     */
    fun updateUserInput(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }
    
    /**
     * 选择模型
     */
    fun selectModel(model: SavedModel) {
        _uiState.update { it.copy(selectedModel = model, showModelSelector = false) }
        // 持久化选择
        viewModelScope.launch {
            aiConfigManager.setDefaultModel(model.id)
        }
    }
    
    /**
     * 切换模型选择器显示
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
     * 切换分类选择器显示
     */
    fun toggleCategorySelector(show: Boolean) {
        _uiState.update { it.copy(showCategorySelector = show) }
    }
    
    /**
     * 切换帮助对话框显示
     */
    fun toggleHelpDialog(show: Boolean) {
        _uiState.update { it.copy(showHelpDialog = show) }
    }
    
    // ==================== 开发操作 ====================
    
    /**
     * 开始开发
     */
    fun startDevelopment() {
        val state = _uiState.value
        
        if (state.userInput.isBlank()) {
            viewModelScope.launch {
                _events.emit(AiModuleDeveloperEvent.ShowToast("请输入功能需求"))
            }
            return
        }
        
        // 添加用户消息到对话列表
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
        
        // Save会话状态（用于恢复）
        saveSessionState()
        
        // Start流式开发
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
     * 处理 Agent 流式事件
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
                        // 注意：不清除 userInput，保留用户输入 (Requirements: 8.5)
                    )
                }
                // Save会话状态以便恢复 (Requirements: 8.6)
                saveSessionState()
                _events.emit(AiModuleDeveloperEvent.ShowToast(event.message))
            }
            
            is AgentStreamEvent.Completed -> {
                // Create AI 响应消息
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
                        userInput = "", // 清空输入
                        generatedModule = event.module,
                        editedJsCode = event.module.jsCode,
                        editedCssCode = event.module.cssCode,
                        hasEdits = false,
                        agentState = AgentState.COMPLETED
                    )
                }
                // Save会话状态（包含生成的模块）
                saveSessionState()
                _events.emit(AiModuleDeveloperEvent.ShowToast("✅ 模块生成成功"))
            }
        }
    }
    
    // ==================== 代码编辑操作 ====================
    
    /**
     * 更新 JavaScript 代码
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
     * 更新 CSS 代码
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
     * 验证代码
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
     * 复制代码到剪贴板
     */
    fun copyCode(code: String) {
        viewModelScope.launch {
            _events.emit(AiModuleDeveloperEvent.CopyToClipboard(code))
            _events.emit(AiModuleDeveloperEvent.ShowToast("已复制到剪贴板"))
        }
    }
    
    // ==================== 模块保存操作 ====================
    
    /**
     * 保存模块
     * 
     * Requirements: 7.7 - 使用编辑后的代码
     */
    fun saveModule(onSuccess: (ExtensionModule) -> Unit) {
        val state = _uiState.value
        val module = state.generatedModule ?: return
        
        // Get用于保存的代码（优先使用编辑后的代码）
        val (jsCode, cssCode) = state.getCodeForSave()
        
        // Create最终模块
        val finalModule = module.copy(
            jsCode = jsCode,
            cssCode = cssCode
        ).toExtensionModule()
        
        viewModelScope.launch {
            extensionManager.addModule(finalModule)
                .onSuccess { savedModule ->
                    _events.emit(AiModuleDeveloperEvent.ShowToast("✅ 模块已保存"))
                    _events.emit(AiModuleDeveloperEvent.ModuleCreated(savedModule.id))
                    onSuccess(savedModule)
                }
                .onFailure { e ->
                    _events.emit(AiModuleDeveloperEvent.ShowToast("保存失败: ${e.message}"))
                }
        }
    }
    
    // ==================== 重置操作 ====================
    
    /**
     * 重置状态
     */
    fun resetState() {
        agentEngine.reset()
        _uiState.update { 
            AiModuleDeveloperUiState(
                selectedModel = it.selectedModel,
                availableModels = it.availableModels
            )
        }
        // 清除保存的会话状态
        viewModelScope.launch {
            clearSessionState()
        }
    }
    
    /**
     * 重试开发
     * 
     * Requirements: 8.4, 8.5 - 保留用户输入并重试
     */
    fun retry() {
        val state = _uiState.value
        // 保留用户输入，清除错误状态
        _uiState.update { 
            it.copy(
                error = null,
                agentState = AgentState.IDLE,
                isStreaming = false
            )
        }
        // 如果有用户输入，重新开始开发
        if (state.userInput.isNotBlank()) {
            startDevelopment()
        }
    }
    
    /**
     * 使用不同模型重试
     * 
     * Requirements: 8.4 - 提供换模型重试选项
     */
    fun retryWithDifferentModel() {
        val state = _uiState.value
        val currentModel = state.selectedModel
        val availableModels = state.availableModels
        
        // 找到下一个可用模型
        val currentIndex = availableModels.indexOfFirst { it.id == currentModel?.id }
        val nextModel = if (currentIndex >= 0 && availableModels.size > 1) {
            availableModels[(currentIndex + 1) % availableModels.size]
        } else {
            availableModels.firstOrNull { it.id != currentModel?.id }
        }
        
        if (nextModel != null) {
            // 切换模型并重试
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
            
            // 如果有用户输入，重新开始开发
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
     * 清除错误状态
     * 
     * Requirements: 8.5 - 保留用户输入
     */
    fun dismissError() {
        _uiState.update { 
            it.copy(
                error = null,
                agentState = AgentState.IDLE
                // 注意：不清除 userInput，保留用户输入
            )
        }
    }
    
    /**
     * 恢复用户输入
     * 用于从最后一条用户消息恢复输入
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
    
    // ==================== 滚动控制 ====================
    
    /**
     * 设置自动滚动
     */
    fun setAutoScroll(enabled: Boolean) {
        _uiState.update { it.copy(shouldAutoScroll = enabled) }
    }
    
    /**
     * 清除错误状态
     * 
     * Requirements: 8.4 - 允许用户关闭错误提示
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * 导航到 AI 设置
     */
    fun navigateToAiSettings() {
        viewModelScope.launch {
            _events.emit(AiModuleDeveloperEvent.NavigateToAiSettings)
        }
    }
    
    // ==================== 会话持久化和恢复 ====================
    
    /**
     * 检查是否有中断的会话
     * 
     * Requirements: 8.6
     */
    private fun checkForInterruptedSession() {
        viewModelScope.launch {
            context.sessionDataStore.data.first().let { prefs ->
                val hasSession = prefs[SessionKeys.HAS_INTERRUPTED_SESSION] ?: false
                val lastTimestamp = prefs[SessionKeys.LAST_SESSION_TIMESTAMP] ?: 0L
                
                // 只恢复24小时内的会话
                val isRecent = System.currentTimeMillis() - lastTimestamp < 24 * 60 * 60 * 1000
                
                _hasInterruptedSession.value = hasSession && isRecent
            }
        }
    }
    
    /**
     * 保存当前会话状态
     * 在开发过程中定期调用，以便在中断时恢复
     * 
     * Requirements: 8.6
     */
    private fun saveSessionState() {
        viewModelScope.launch {
            val state = _uiState.value
            
            context.sessionDataStore.edit { prefs ->
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
     * 恢复中断的会话
     * 
     * Requirements: 8.6
     */
    fun resumeInterruptedSession() {
        viewModelScope.launch {
            try {
                context.sessionDataStore.data.first().let { prefs ->
                    val userInput = prefs[SessionKeys.USER_INPUT] ?: ""
                    val categoryName = prefs[SessionKeys.SELECTED_CATEGORY] ?: ""
                    val messagesJson = prefs[SessionKeys.MESSAGES] ?: "[]"
                    val moduleJson = prefs[SessionKeys.GENERATED_MODULE] ?: ""
                    val editedJsCode = prefs[SessionKeys.EDITED_JS_CODE] ?: ""
                    val editedCssCode = prefs[SessionKeys.EDITED_CSS_CODE] ?: ""
                    
                    // Parse消息列表
                    val messagesType = object : TypeToken<List<ConversationMessage>>() {}.type
                    val messages: List<ConversationMessage> = try {
                        gson.fromJson(messagesJson, messagesType) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    // Parse生成的模块
                    val generatedModule: GeneratedModuleData? = if (moduleJson.isNotBlank()) {
                        try {
                            gson.fromJson(moduleJson, GeneratedModuleData::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                    
                    // Parse分类
                    val category = if (categoryName.isNotBlank()) {
                        try {
                            ModuleCategory.valueOf(categoryName)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                    
                    // 恢复状态
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
                    
                    // 清除中断标记
                    _hasInterruptedSession.value = false
                    
                    _events.emit(AiModuleDeveloperEvent.ShowToast("已恢复上次会话"))
                }
            } catch (e: Exception) {
                _events.emit(AiModuleDeveloperEvent.ShowToast("恢复会话失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 放弃中断的会话
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
     * 清除保存的会话状态
     */
    private suspend fun clearSessionState() {
        context.sessionDataStore.edit { prefs ->
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
     * ViewModel 工厂
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AiModuleDeveloperViewModel::class.java)) {
                return AiModuleDeveloperViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
