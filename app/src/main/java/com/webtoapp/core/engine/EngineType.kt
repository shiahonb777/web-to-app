package com.webtoapp.core.engine

/**
 * Note.
 */
enum class EngineType(
    val displayName: String,
    val description: String,
    /** MB. */
    val estimatedSizeMb: Int,
    /** download. */
    val requiresDownload: Boolean
) {
    /** default system. */
    SYSTEM_WEBVIEW(
        displayName = "System WebView",
        description = "使用设备内置的 Android WebView 引擎",
        estimatedSizeMb = 0,
        requiresDownload = false
    ),

    /** Mozilla GeckoView Firefox. */
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