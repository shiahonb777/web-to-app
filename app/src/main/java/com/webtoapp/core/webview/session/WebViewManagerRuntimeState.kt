package com.webtoapp.core.webview

import android.webkit.WebView
import com.webtoapp.data.model.WebViewConfig
import java.util.WeakHashMap

internal data class WebViewLoadDiagnostics(
    var pageStartTime: Long = 0L,
    var requestCount: Int = 0,
    var blockedCount: Int = 0,
    var errorCount: Int = 0
)

internal class WebViewManagerRuntimeState {
    var currentConfig: WebViewConfig? = null

    @Volatile
    var currentMainFrameUrl: String? = null

    var appExtensionModuleIds: List<String> = emptyList()
    var embeddedModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList()
    var allowGlobalModuleFallback: Boolean = false
    var extensionFabIcon: String = ""
    var currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null
    var cachedBrowserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null
    var cachedBrowserDisguiseJs: String? = null
    var gmBridge: com.webtoapp.core.extension.GreasemonkeyBridge? = null
    var lastFailedUrl: String? = null
    var fileRetryCount: Int = 0
    var fileRetryUrl: String? = null

    val extensionRuntimes: MutableMap<String, com.webtoapp.core.extension.ChromeExtensionRuntime> =
        mutableMapOf()
    val managedWebViews = WeakHashMap<WebView, Boolean>()
    val diagnostics = WebViewLoadDiagnostics()
}
