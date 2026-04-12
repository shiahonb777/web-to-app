package com.webtoapp.core.webview

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.WebViewConfig

internal data class DynamicUserAgents(
    val desktopUserAgent: String,
    val strictCompatMobileUserAgent: String
)

internal class UserAgentResolver(
    private val context: Context
) {
    fun ensureDynamicUserAgents(
        desktopUserAgent: String?,
        strictCompatMobileUserAgent: String?
    ): DynamicUserAgents {
        if (desktopUserAgent != null && strictCompatMobileUserAgent != null) {
            return DynamicUserAgents(desktopUserAgent, strictCompatMobileUserAgent)
        }

        return try {
            val defaultUA = WebSettings.getDefaultUserAgent(context)
            val chromeVersion = Regex("""Chrome/(\d+\.\d+\.\d+\.\d+)""").find(defaultUA)
                ?.groupValues?.get(1) ?: "130.0.0.0"
            val dynamicDesktopUa =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            val dynamicStrictMobileUa =
                "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
            AppLogger.d("WebViewManager", "Dynamic UA initialized: Chrome/$chromeVersion")
            DynamicUserAgents(dynamicDesktopUa, dynamicStrictMobileUa)
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "Failed to extract Chrome version, using fallback")
            DynamicUserAgents(
                WebViewManager.DESKTOP_USER_AGENT_FALLBACK,
                WebViewManager.STRICT_COMPAT_MOBILE_UA_FALLBACK
            )
        }
    }

    fun resolveUserAgent(
        config: WebViewConfig,
        deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?,
        desktopUserAgent: String
    ): String? {
        AppLogger.d(
            "WebViewManager",
            "resolveUserAgent: userAgentMode=${config.userAgentMode}, customUserAgent=${config.customUserAgent?.take(30)}, desktopMode=${config.desktopMode}"
        )

        if (deviceDisguiseConfig != null && deviceDisguiseConfig.enabled) {
            val ua = deviceDisguiseConfig.generateUserAgent()
            if (ua.isNotBlank()) {
                AppLogger.d("WebViewManager", "resolveUserAgent: DeviceDisguise -> ${ua.take(80)}")
                return ua
            }
        }

        when (config.userAgentMode) {
            UserAgentMode.DEFAULT -> Unit
            UserAgentMode.CUSTOM -> {
                val ua = config.customUserAgent?.takeIf { it.isNotBlank() }
                AppLogger.d("WebViewManager", "resolveUserAgent: CUSTOM mode -> ${ua?.take(60) ?: "null"}")
                return ua
            }
            else -> {
                val ua = config.userAgentMode.userAgentString
                AppLogger.d("WebViewManager", "resolveUserAgent: ${config.userAgentMode.name} mode -> ${ua?.take(60) ?: "null"}")
                return ua
            }
        }

        if (config.desktopMode) {
            AppLogger.d("WebViewManager", "resolveUserAgent: desktopMode fallback")
            return desktopUserAgent
        }

        val legacyUa = config.userAgent?.takeIf { it.isNotBlank() }
        AppLogger.d("WebViewManager", "resolveUserAgent: DEFAULT mode, legacyUA=${legacyUa?.take(60) ?: "null"}")
        return legacyUa
    }

    fun isDesktopUaRequested(
        config: WebViewConfig?,
        deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
    ): Boolean {
        val cfg = config ?: return false
        return cfg.desktopMode ||
            cfg.userAgentMode in WebViewManager.DESKTOP_UA_MODES ||
            (deviceDisguiseConfig?.requiresDesktopViewport() == true)
    }
}
