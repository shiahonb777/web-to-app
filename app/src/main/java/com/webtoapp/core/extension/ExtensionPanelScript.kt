package com.webtoapp.core.extension

import com.webtoapp.core.extension.panel.PanelHelperScript
import com.webtoapp.core.extension.panel.PanelIcons
import com.webtoapp.core.extension.panel.PanelScripts

object ExtensionPanelScript {
    fun getPanelInitScript(fabIcon: String = ""): String {
        val icon = PanelIcons.resolveFabIcon(fabIcon)
        return PanelScripts.buildPanelScript(icon)
    }

    fun getModuleHelperScript(): String = PanelHelperScript.helperScript
}
