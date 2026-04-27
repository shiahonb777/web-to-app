package com.webtoapp.core.engine

import com.webtoapp.core.i18n.Strings

/**
 * 浏览器引擎类型
 */
enum class EngineType(
    val displayName: String,
    /** 引擎原生库预估大小 (单架构, MB) */
    val estimatedSizeMb: Int,
    /** 是否需要额外下载 */
    val requiresDownload: Boolean
) {
    /** 系统 WebView（默认，零额外体积） */
    SYSTEM_WEBVIEW(
        displayName = "System WebView",
        estimatedSizeMb = 0,
        requiresDownload = false
    ),

    /** Mozilla GeckoView (Firefox 内核) */
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
