package com.webtoapp.core.engine

import com.webtoapp.core.i18n.Strings




enum class EngineType(
    val displayName: String,

    val estimatedSizeMb: Int,

    val requiresDownload: Boolean
) {

    SYSTEM_WEBVIEW(
        displayName = "System WebView",
        estimatedSizeMb = 0,
        requiresDownload = false
    ),


    GECKOVIEW(
        displayName = "GeckoView (Firefox)",
        estimatedSizeMb = 55,
        requiresDownload = true
    );

    val description: String
        get() = when (this) {
            SYSTEM_WEBVIEW -> Strings.engineSystemWebviewDesc
            GECKOVIEW -> Strings.engineGeckoviewDesc
        }

    companion object {
        fun fromString(value: String): EngineType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SYSTEM_WEBVIEW
        }
    }
}
