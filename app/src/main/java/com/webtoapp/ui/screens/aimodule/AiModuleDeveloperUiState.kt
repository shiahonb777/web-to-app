package com.webtoapp.ui.screens.aimodule

import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.agent.*
import com.webtoapp.data.model.SavedModel

/**
 * AI 模块开发器 UI 状态
 * 
 * 包含所有 UI 状态字段，用于驱动 AiModuleDeveloperScreen 的显示
 * 
 * Requirements: 4.1
 */
data class AiModuleDeveloperUiState(
    // ==================== 模型选择 ====================
    /**
     * 当前选中的模型
     */
    val selectedModel: SavedModel? = null,
    
    /**
     * 可用的模型列表（已过滤支持 MODULE_DEVELOPMENT 的模型）
     */
    val availableModels: List<SavedModel> = emptyList(),
    
    /**
     * 是否显示模型选择器下拉菜单
     */
    val showModelSelector: Boolean = false,
    
    // ==================== 输入 ====================
    /**
     * 用户输入的需求描述
     */
    val userInput: String = "",
    
    /**
     * 选中的模块分类
     */
    val selectedCategory: ModuleCategory? = null,
    
    /**
     * 是否显示分类选择器
     */
    val showCategorySelector: Boolean = false,
    
    // ==================== 对话 ====================
    /**
     * 对话消息列表
     */
    val messages: List<ConversationMessage> = emptyList(),
    
    /**
     * 是否正在流式输出
     */
    val isStreaming: Boolean = false,
    
    /**
     * 当前流式输出的内容
     */
    val streamingContent: String = "",
    
    /**
     * 当前思考内容
     */
    val thinkingContent: String = "",
    
    // ==================== 工具调用 ====================
    /**
     * 当前工具调用列表
     */
    val currentToolCalls: List<ToolCallInfo> = emptyList(),
    
    // ==================== 生成结果 ====================
    /**
     * 生成的模块数据
     */
    val generatedModule: GeneratedModuleData? = null,
    
    /**
     * 编辑后的 JavaScript 代码
     */
    val editedJsCode: String = "",
    
    /**
     * 编辑后的 CSS 代码
     */
    val editedCssCode: String = "",
    
    /**
     * 是否有未保存的编辑
     */
    val hasEdits: Boolean = false,
    
    // ==================== 状态 ====================
    /**
     * Agent 当前状态
     */
    val agentState: AgentState = AgentState.IDLE,
    
    /**
     * 错误信息
     */
    val error: ErrorInfo? = null,
    
    // ==================== 滚动 ====================
    /**
     * 是否应该自动滚动到底部
     */
    val shouldAutoScroll: Boolean = true,
    
    // ==================== 对话框 ====================
    /**
     * 是否显示帮助对话框
     */
    val showHelpDialog: Boolean = false
) {
    /**
     * 是否正在开发中（非空闲、非完成、非错误状态）
     */
    val isDeveloping: Boolean
        get() = agentState != AgentState.IDLE && 
                agentState != AgentState.COMPLETED && 
                agentState != AgentState.ERROR
    
    /**
     * 是否可以发送消息
     */
    val canSend: Boolean
        get() = userInput.isNotBlank() && !isDeveloping
    
    /**
     * 是否显示欢迎界面
     */
    val showWelcome: Boolean
        get() = messages.isEmpty() && generatedModule == null && error == null && !isDeveloping
    
    /**
     * 获取用于保存的代码（优先使用编辑后的代码）
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
 * 对话消息
 * 
 * 用于在聊天界面显示的消息
 */
data class ConversationMessage(
    /**
     * 消息 ID
     */
    val id: String = java.util.UUID.randomUUID().toString(),
    
    /**
     * 消息角色
     */
    val role: MessageRole,
    
    /**
     * 消息内容
     */
    val content: String,
    
    /**
     * 思考内容（仅 AI 消息）
     */
    val thinkingContent: String? = null,
    
    /**
     * 工具调用列表（仅 AI 消息）
     */
    val toolCalls: List<ToolCallInfo> = emptyList(),
    
    /**
     * 生成的模块（仅 AI 消息）
     */
    val generatedModule: GeneratedModuleData? = null,
    
    /**
     * 时间戳
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * 是否正在流式输出
     */
    val isStreaming: Boolean = false
)

/**
 * 错误信息
 * 
 * 用于显示错误状态和恢复选项
 */
data class ErrorInfo(
    /**
     * 错误消息
     */
    val message: String,
    
    /**
     * 错误码
     */
    val code: String? = null,
    
    /**
     * 是否可恢复
     */
    val recoverable: Boolean = true,
    
    /**
     * 原始响应（用于调试）
     */
    val rawResponse: String? = null
)

/**
 * UI 事件
 * 
 * 用于 ViewModel 向 UI 发送一次性事件
 */
sealed class AiModuleDeveloperEvent {
    /**
     * 显示 Toast 消息
     */
    data class ShowToast(val message: String) : AiModuleDeveloperEvent()
    
    /**
     * 滚动到底部
     */
    object ScrollToBottom : AiModuleDeveloperEvent()
    
    /**
     * 模块创建成功
     */
    data class ModuleCreated(val moduleId: String) : AiModuleDeveloperEvent()
    
    /**
     * 导航到 AI 设置
     */
    object NavigateToAiSettings : AiModuleDeveloperEvent()
    
    /**
     * 复制到剪贴板
     */
    data class CopyToClipboard(val content: String) : AiModuleDeveloperEvent()
}
