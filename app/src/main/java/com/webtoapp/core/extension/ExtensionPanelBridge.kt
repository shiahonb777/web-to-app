package com.webtoapp.core.extension

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch






class ExtensionPanelBridge(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val JS_INTERFACE_NAME = "ExtensionPanel"
    }


    private val registeredModules = mutableMapOf<String, ModuleUIInfo>()


    private var activeModuleId: String? = null


    private var webViewRef: WebView? = null




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







    @JavascriptInterface
    fun registerModuleWithConfig(
        moduleId: String,
        name: String,
        icon: String
    ) {
        scope.launch(Dispatchers.Main) {
            registeredModules[moduleId] = ModuleUIInfo(
                id = moduleId,
                name = name,
                icon = icon,
                color = ""
            )
        }
    }






    @JavascriptInterface
    fun registerModulePanel(moduleId: String, panelHtml: String) {
        scope.launch(Dispatchers.Main) {
            registeredModules[moduleId]?.let { info ->
                registeredModules[moduleId] = info.copy(hasPanel = true, panelHtml = panelHtml)
            }
        }
    }




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




    @JavascriptInterface
    fun hidePanel() {
        scope.launch(Dispatchers.Main) {
            activeModuleId = null
            webViewRef?.evaluateJavascript("__WTA_PANEL__.hidePanel()", null)
        }
    }




    @JavascriptInterface
    fun showToast(message: String, duration: Int = 2000) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.showToast('${message.replace("'", "\\'")}', $duration)",
                null
            )
        }
    }






    private fun updatePanelUI() {
        webViewRef?.evaluateJavascript("__WTA_PANEL__.updateModules()", null)
    }




    @JavascriptInterface
    fun setFabVisible(visible: Boolean) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.setFabVisible($visible)",
                null
            )
        }
    }




    @JavascriptInterface
    fun setFabPosition(bottom: Int, right: Int) {
        scope.launch(Dispatchers.Main) {
            webViewRef?.evaluateJavascript(
                "__WTA_PANEL__.setFabPosition($bottom, $right)",
                null
            )
        }
    }




    @JavascriptInterface
    fun getRegisteredModules(): String {
        val modules = registeredModules.values.map { info ->
            """{"id":"${info.id}","name":"${info.name}","icon":"${info.icon}","hasPanel":${info.hasPanel}}"""
        }
        return "[${modules.joinToString(",")}]"
    }




    @JavascriptInterface
    fun getSupportedUITypes(): String {
        return """["FLOATING_BUTTON"]"""
    }
}
