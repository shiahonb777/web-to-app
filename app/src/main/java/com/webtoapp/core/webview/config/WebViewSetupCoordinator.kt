package com.webtoapp.core.webview

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.engine.shields.ThirdPartyCookiePolicy
import com.webtoapp.core.errorpage.ErrorPageManager
import com.webtoapp.core.errorpage.ErrorPageMode
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig

internal class WebViewSetupCoordinator(
    private val context: android.content.Context,
    private val state: WebViewManagerRuntimeState,
    private val settingsConfigurator: WebViewSettingsConfigurator,
    private val userAgentResolver: UserAgentResolver,
    private val extensionRuntimeCoordinator: ExtensionRuntimeCoordinator,
    private val getActiveModulesForCurrentApp: () -> List<com.webtoapp.core.extension.ExtensionModule>,
    private val createWebViewClient: (WebViewConfig, WebViewCallbacks) -> WebViewClient,
    private val createWebChromeClient: (WebViewConfig, WebViewCallbacks) -> WebChromeClient,
    private val openInCustomTab: (String) -> Unit
) {
    fun configureWebView(
        webView: WebView,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        extensionModuleIds: List<String>,
        embeddedExtensionModules: List<com.webtoapp.core.shell.EmbeddedShellModule>,
        extensionFabIcon: String,
        allowGlobalModuleFallback: Boolean,
        browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig?,
        deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?,
        shields: BrowserShields,
        currentLanguage: String,
        resolveUserAgent: (WebViewConfig) -> String?,
        onErrorPageManagerChange: (ErrorPageManager?) -> Unit
    ) {
        state.currentConfig = config
        state.cachedBrowserDisguiseConfig = browserDisguiseConfig
        state.cachedBrowserDisguiseJs = if (browserDisguiseConfig?.enabled == true) {
            com.webtoapp.core.disguise.BrowserDisguiseJsGenerator.generate(browserDisguiseConfig).also { js ->
                val coverage = com.webtoapp.core.disguise.BrowserDisguiseConfig.calculateCoverage(browserDisguiseConfig)
                AppLogger.d(
                    "WebViewManager",
                    "Browser Disguise JS cached: ${js.length} chars, coverage=${"%,.0f".format(coverage * 100)}%"
                )
            }
        } else {
            null
        }
        state.appExtensionModuleIds = extensionModuleIds
        state.embeddedModules = embeddedExtensionModules
        state.allowGlobalModuleFallback = allowGlobalModuleFallback
        state.extensionFabIcon = extensionFabIcon
        state.currentDeviceDisguiseConfig = deviceDisguiseConfig

        onErrorPageManagerChange(
            if (config.errorPageConfig.mode != ErrorPageMode.DEFAULT) {
                ErrorPageManager(config.errorPageConfig.copy(language = currentLanguage))
            } else {
                null
            }
        )

        AppLogger.d(
            "WebViewManager",
            "configureWebView: extensionModuleIds=${extensionModuleIds.size}, embeddedModules=${embeddedExtensionModules.size}"
        )
        embeddedExtensionModules.forEach { module ->
            AppLogger.d(
                "WebViewManager",
                "  Embedded module: id=${module.id}, name=${module.name}, enabled=${module.enabled}, runAt=${module.runAt}"
            )
        }

        state.managedWebViews[webView] = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val shieldsActive = shields.isEnabled() && !config.disableShields
        val cookiePolicy = shields.getConfig().thirdPartyCookiePolicy
        cookieManager.setAcceptThirdPartyCookies(
            webView,
            !shieldsActive || cookiePolicy == ThirdPartyCookiePolicy.ALLOW_ALL
        )
        cookieManager.flush()
        AppLogger.d("WebViewManager", "Cookie persistence enabled (disableShields=${config.disableShields})")

        val isDesktopModeRequested = userAgentResolver.isDesktopUaRequested(config, deviceDisguiseConfig)
        val preferLandscapeEmbeddedViewport = config.landscapeMode && !isDesktopModeRequested
        val effectiveUserAgent = resolveUserAgent(config)
        val hasActiveChromeExt = getActiveModulesForCurrentApp().any { module ->
            module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.chromeExtId.isNotEmpty()
        }

        webView.apply {
            settingsConfigurator.apply(
                webView = this,
                config = config,
                effectiveUserAgent = effectiveUserAgent,
                isDesktopModeRequested = isDesktopModeRequested,
                preferLandscapeEmbeddedViewport = preferLandscapeEmbeddedViewport,
                hasActiveChromeExtension = hasActiveChromeExt,
                desktopUserAgent = WebViewManager.DESKTOP_USER_AGENT ?: WebViewManager.DESKTOP_USER_AGENT_FALLBACK
            )

            webViewClient = createWebViewClient(config, callbacks)
            webChromeClient = createWebChromeClient(config, callbacks)

            if (config.downloadEnabled) {
                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    callbacks.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
                }
            }

            if (config.enableShareBridge) {
                addJavascriptInterface(ShareBridge(context), "NativeShareBridge")
            }

            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onOAuthBlocked(url: String) {
                    AppLogger.w("WebViewManager", "OAuth block detected via JS bridge — redirecting to CCT: $url")
                    webView.post {
                        webView.stopLoading()
                        if (webView.canGoBack()) webView.goBack()
                        openInCustomTab(url)
                    }
                }
            }, "NativeOAuthBridge")

            state.gmBridge?.destroy()
            val bridge = com.webtoapp.core.extension.GreasemonkeyBridge(context) { webView }
            state.gmBridge = bridge
            addJavascriptInterface(bridge, com.webtoapp.core.extension.GreasemonkeyBridge.JS_INTERFACE_NAME)

            extensionRuntimeCoordinator.initChromeExtensionRuntimes(webView)
            com.webtoapp.core.kernel.BrowserKernel.configureWebView(webView)
        }
    }
}
