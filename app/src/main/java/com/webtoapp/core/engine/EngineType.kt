package com.webtoapp.core.engine

/**
 * 浏览器引擎类型
 */
enum class EngineType(
    val displayName: String,
    val description: String,
    /** 引擎原生库预估大小 (单架构, MB) */
    val estimatedSizeMb: Int,
    /** 是否需要额外下载 */
    val requiresDownload: Boolean
) {
    /** 系统 WebView（默认，零额外体积） */
    SYSTEM_WEBVIEW(
        displayName = "System WebView",
        description = "使用设备内置的 Android WebView 引擎",
        estimatedSizeMb = 0,
        requiresDownload = false
    ),

    /** Mozilla GeckoView (Firefox 内核) */
    GECKOVIEW(
        displayName = "GeckoView (Firefox)",
        description = "内嵌 Firefox 引擎，内置隐私保护与广告拦截",
        estimatedSizeMb = 55,
        requiresDownload = true
    );

    companion object {
        fun fromString(value: String): EngineType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SYSTEM_WEBVIEW
        }
    }
}
