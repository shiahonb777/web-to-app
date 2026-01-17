package com.webtoapp.core.extension

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 扩展模块面板桥接
 * 
 * 提供统一的扩展模块 UI 管理，避免各模块 UI 重叠
 * 所有模块的 UI 都注册到统一的面板中显示
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
        val hasPanel: Boolean = false,
        val panelHtml: String = ""
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
    
    /**
     * 更新面板 UI
     */
    private fun updatePanelUI() {
        webViewRef?.evaluateJavascript("__WTA_PANEL__.updateModules()", null)
    }
    
    /**
     * 获取已注册模块列表（JSON格式）
     */
    @JavascriptInterface
    fun getRegisteredModules(): String {
        val modules = registeredModules.values.map { info ->
            """{"id":"${info.id}","name":"${info.name}","icon":"${info.icon}","color":"${info.color}","hasPanel":${info.hasPanel}}"""
        }
        return "[${modules.joinToString(",")}]"
    }
}
