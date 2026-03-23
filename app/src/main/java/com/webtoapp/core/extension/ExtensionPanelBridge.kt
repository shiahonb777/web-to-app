package com.webtoapp.core.extension

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 扩展模块面板桥接
 * 
 * 提供统一的扩展模块 UI 管理，支持多种UI类型
 * UI类型包括：悬浮按钮、工具栏、侧边栏、底部栏、悬浮面板、迷你按钮、自定义UI
 */
class ExtensionPanelBridge(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val JS_INTERFACE_NAME = "ExtensionPanel"
    }
    
    // 已注册的模块 UI 信息
    private val registeredModules = mutableMapOf<String, ModuleUIInfo>()
    
    // 当前活跃的模块ID
    private var activeModuleId: String? = null
    
    // WebView 引用
    private var webViewRef: WebView? = null

    /**
     * 模块 UI 信息
     */
    data class ModuleUIInfo(
        val id: String,
        val name: String,
        val icon: String,
        val color: String,
        val uiType: String = "FLOATING_BUTTON",
        val hasPanel: Boolean = false,
        val panelHtml: String = "",
        val uiConfigJson: String = "{}"
    )
    
    fun setWebView(webView: WebView) {
        webViewRef = webView
    }
    
    /**
     * 注册模块到面板
     * @param moduleId 模块ID
     * @param name 模块名称
     * @param icon 模块图标（emoji）
     * @param color 主题色（十六进制）
     */
    @JavascriptInterface
    fun registerModule(moduleId: String, name: String, icon: String, color: String) {
        scope.launch(Dispatchers.Main) {
            registeredModules[moduleId] = ModuleUIInfo(
                id = moduleId,
                name = name,
                icon = icon,
                color = color
            )
            updatePanelUI()
        }
    }
    
    /**
     * 注册模块（带UI配置）
     * @param moduleId 模块ID
     * @param name 模块名称
     * @param icon 模块图标（emoji）
     * @param uiType UI类型
     * @param uiConfigJson UI配置JSON
     */
    @JavascriptInterface
    fun registerModuleWithConfig(
        moduleId: String, 
        name: String, 
        icon: String, 
        uiType: String,
        uiConfigJson: String
    ) {
        scope.launch(Dispatchers.Main) {
            registeredModules[moduleId] = ModuleUIInfo(
                id = moduleId,
                name = name,
                icon = icon,
                color = "",
                uiType = uiType,
                uiConfigJson = uiConfigJson
            )
        }
    }
    
    /**
     * 注册模块面板内容
     * @param moduleId 模块ID
     * @param panelHtml 面板HTML内容
     */
    @JavascriptInterface
    fun registerModulePanel(moduleId: String, panelHtml: String) {
        scope.launch(Dispatchers.Main) {
            registeredModules[moduleId]?.let { info ->
                registeredModules[moduleId] = info.copy(hasPanel = true, panelHtml = panelHtml)
            }
        }
    }

    /**
     * 显示模块面板
     */
    @JavascriptInterface
    fun showModulePanel(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            activeModuleId = moduleId
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.showModulePanel('$moduleId')",
                null
            )
        }
    }
    
    /**
     * 隐藏面板
     */
    @JavascriptInterface
    fun hidePanel() {
        scope.launch(Dispatchers.Main) {
            activeModuleId = null
            webViewRef?.evaluateJavascript("__WTA_PANEL__.hidePanel()", null)
        }
    }
    
    /**
     * 显示 Toast 提示
     */
    @JavascriptInterface
    fun showToast(message: String, duration: Int = 2000) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.showToast('${message.replace("'", "\\'")}', $duration)",
                null
            )
        }
    }
    
    // ==================== 侧边栏控制 ====================
    
    /**
     * 显示侧边栏
     */
    @JavascriptInterface
    fun showSidebar(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.showSidebar('$moduleId')",
                null
            )
        }
    }
    
    /**
     * 隐藏侧边栏
     */
    @JavascriptInterface
    fun hideSidebar(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.hideSidebar('$moduleId')",
                null
            )
        }
    }
    
    /**
     * 切换侧边栏
     */
    @JavascriptInterface
    fun toggleSidebar(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.toggleSidebar('$moduleId')",
                null
            )
        }
    }
    
    // ==================== 悬浮面板控制 ====================
    
    /**
     * 显示悬浮面板
     */
    @JavascriptInterface
    fun showFloatingPanel(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.showFloatingPanel('$moduleId')",
                null
            )
        }
    }
    
    /**
     * 隐藏悬浮面板
     */
    @JavascriptInterface
    fun hideFloatingPanel(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.hideFloatingPanel('$moduleId')",
                null
            )
        }
    }
    
    /**
     * 更新悬浮面板内容
     */
    @JavascriptInterface
    fun updateFloatingPanelContent(moduleId: String, html: String) {
        scope.launch(Dispatchers.Main) {
            val escapedHtml = html.replace("\\", "\\\\").replace("'", "\\'")
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.updateFloatingPanelContent('$moduleId', '$escapedHtml')",
                null
            )
        }
    }
    
    // ==================== 底部栏控制 ====================
    
    /**
     * 设置底部栏可见性
     */
    @JavascriptInterface
    fun setBottomBarVisible(visible: Boolean) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.setBottomBarVisible($visible)",
                null
            )
        }
    }
    
    // ==================== 工具栏控制 ====================
    
    /**
     * 切换工具栏折叠状态
     */
    @JavascriptInterface
    fun toggleToolbarCollapse(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.toggleToolbarCollapse('$moduleId')",
                null
            )
        }
    }
    
    // ==================== 迷你按钮控制 ====================
    
    /**
     * 更新迷你按钮徽章
     */
    @JavascriptInterface
    fun updateMiniButtonBadge(moduleId: String, count: Int) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.updateMiniButtonBadge('$moduleId', $count)",
                null
            )
        }
    }
    
    /**
     * 显示迷你按钮弹出面板
     */
    @JavascriptInterface
    fun showMiniButtonPanel(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.showMiniButtonPanel('$moduleId')",
                null
            )
        }
    }
    
    /**
     * 隐藏迷你按钮弹出面板
     */
    @JavascriptInterface
    fun hideMiniButtonPanel(moduleId: String) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.hideMiniButtonPanel('$moduleId')",
                null
            )
        }
    }
    
    // ==================== 自定义UI控制 ====================
    
    /**
     * 更新自定义UI内容
     */
    @JavascriptInterface
    fun updateCustomUI(moduleId: String, html: String) {
        scope.launch(Dispatchers.Main) {
            val escapedHtml = html.replace("\\", "\\\\").replace("'", "\\'")
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.updateCustomUI('$moduleId', '$escapedHtml')",
                null
            )
        }
    }
    
    // ==================== 通用方法 ====================
    
    /**
     * 更新面板 UI
     */
    private fun updatePanelUI() {
        webViewRef?.evaluateJavascript("__WTA_PANEL__.updateModules()", null)
    }
    
    /**
     * 设置FAB可见性
     */
    @JavascriptInterface
    fun setFabVisible(visible: Boolean) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.setFabVisible($visible)",
                null
            )
        }
    }
    
    /**
     * 设置FAB位置
     */
    @JavascriptInterface
    fun setFabPosition(bottom: Int, right: Int) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.setFabPosition($bottom, $right)",
                null
            )
        }
    }
    
    /**
     * 获取已注册模块列表（JSON格式）
     */
    @JavascriptInterface
    fun getRegisteredModules(): String {
        val modules = registeredModules.values.map { info ->
            """{"id":"${info.id}","name":"${info.name}","icon":"${info.icon}","uiType":"${info.uiType}","hasPanel":${info.hasPanel}}"""
        }
        return "[${modules.joinToString(",")}]"
    }
    
    /**
     * 获取支持的UI类型列表
     */
    @JavascriptInterface
    fun getSupportedUITypes(): String {
        return """["FLOATING_BUTTON","FLOATING_TOOLBAR","SIDEBAR","BOTTOM_BAR","FLOATING_PANEL","MINI_BUTTON","CUSTOM"]"""
    }
    
    /**
     * 获取支持的位置列表
     */
    @JavascriptInterface
    fun getSupportedPositions(): String {
        return """["TOP_LEFT","TOP_CENTER","TOP_RIGHT","CENTER_LEFT","CENTER","CENTER_RIGHT","BOTTOM_LEFT","BOTTOM_CENTER","BOTTOM_RIGHT"]"""
    }
}
