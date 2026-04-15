package com.webtoapp.core.webview

import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.WebViewConfig

internal class ScriptInjectionCoordinator(
    private val context: android.content.Context,
    private val getCurrentConfig: () -> WebViewConfig?,
    private val buildPanelInitScripts: () -> List<String>,
    private val shouldUseConservativeScriptMode: (String?) -> Boolean,
    private val shouldUseScriptlessMode: (String?) -> Boolean,
    private val injectCompatibilityScripts: (WebView, String?, Boolean) -> Unit,
    private val injectAllExtensionModules: (WebView, String, ScriptRunTime) -> Unit
) {
    fun injectScripts(
        webView: WebView,
        scripts: List<com.webtoapp.data.model.UserScript>,
        runAt: ScriptRunTime,
        pageUrl: String? = null
    ) {
        val url = pageUrl ?: webView.url ?: ""
        val conservativeMode = shouldUseConservativeScriptMode(url)
        val scriptlessMode = shouldUseScriptlessMode(url)

        if (runAt == ScriptRunTime.DOCUMENT_START) {
            if (!conservativeMode && getCurrentConfig()?.downloadEnabled == true) {
                injectDownloadBridgeScript(webView)
            } else if (conservativeMode) {
                AppLogger.d("WebViewManager", "Skip download bridge for conservative page: $url")
            }

            if (!scriptlessMode) {
                injectExtensionPanelScript(webView)
            }

            if (!conservativeMode) {
                injectIsolationScript(webView)
            }

            if (!scriptlessMode) {
                injectCompatibilityScripts(webView, url, conservativeMode)
            } else {
                AppLogger.d("WebViewManager", "Scriptless mode enabled for strict host: $url")
            }
        }

        if (scriptlessMode) {
            AppLogger.d("WebViewManager", "Scriptless mode: skip user/module injections (${runAt.name})")
            return
        }

        scripts.filter { it.enabled && it.runAt == runAt && it.code.isNotBlank() }
            .forEach { script ->
                try {
                    val wrappedCode = """
                        (function() {
                            try {
                                ${script.code}
                            } catch(e) {
                                console.error('[UserScript: ${script.name}] Error:', e);
                            }
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(wrappedCode, null)
                    AppLogger.d("WebViewManager", "Inject script: ${script.name} (${runAt.name})")
                } catch (e: Exception) {
                    AppLogger.e("WebViewManager", "Script injection failed: ${script.name}", e)
                }
            }

        injectAllExtensionModules(webView, url, runAt)
    }

    fun handlePageStarted(webView: WebView, url: String?, config: WebViewConfig) {
        webView.evaluateJavascript(WebViewManager.CLIPBOARD_POLYFILL_JS, null)
        webView.evaluateJavascript(WebViewManager.SCROLL_SAVE_JS, null)
        if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
            webView.evaluateJavascript(WebViewManager.VIEWPORT_FIT_SCREEN_JS, null)
        }
        if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
            val customWidth = config.customViewportWidth.coerceIn(320, 3840)
            val customJs = WebViewManager.VIEWPORT_CUSTOM_JS.replace("CUSTOM_WIDTH_PLACEHOLDER", customWidth.toString())
            webView.evaluateJavascript(customJs, null)
        }
        com.webtoapp.core.perf.NativePerfEngine.injectPerfOptimizations(
            webView,
            com.webtoapp.core.perf.NativePerfEngine.Phase.DOCUMENT_START
        )
        injectScripts(webView, config.injectScripts, ScriptRunTime.DOCUMENT_START, url)
    }

    fun handlePageFinished(
        webView: WebView,
        url: String?,
        config: WebViewConfig,
        cookieFlushRunnable: Runnable,
        extractHostFromUrl: (String?) -> String?,
        adBlockerCssProvider: (String) -> String
    ) {
        injectScripts(webView, config.injectScripts, ScriptRunTime.DOCUMENT_END, url)
        if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
            webView.evaluateJavascript("window.__wtaViewportFitApplied=false;", null)
            webView.evaluateJavascript(WebViewManager.VIEWPORT_FIT_SCREEN_JS, null)
        }
        if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
            val customWidth = config.customViewportWidth.coerceIn(320, 3840)
            val customJs = WebViewManager.VIEWPORT_CUSTOM_JS.replace("CUSTOM_WIDTH_PLACEHOLDER", customWidth.toString())
            webView.evaluateJavascript("window.__wtaViewportCustomApplied=false;", null)
            webView.evaluateJavascript(customJs, null)
        }
        com.webtoapp.core.perf.NativePerfEngine.injectPerfOptimizations(
            webView,
            com.webtoapp.core.perf.NativePerfEngine.Phase.DOCUMENT_END
        )
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            webView.evaluateJavascript(WebViewManager.IMAGE_REPAIR_JS, null)
        }
        webView.evaluateJavascript(WebViewManager.SCROLL_RESTORE_JS, null)
        if (url != null && OAuthCompatEngine.isOAuthUrl(url)) {
            webView.evaluateJavascript(OAuthCompatEngine.getOAuthBlockDetectionJs(), null)
            AppLogger.d("WebViewManager", "OAuth block detection JS injected for: $url")
        }

        val canInjectAdBlockEnd = !config.disableShields
        if (canInjectAdBlockEnd && url != null) {
            webView.postDelayed({
                if (webView.url == url) {
                    val pageHost = extractHostFromUrl(url) ?: ""
                    if (pageHost.isNotEmpty()) {
                        val cosmeticCss = adBlockerCssProvider(pageHost)
                        if (cosmeticCss.isNotEmpty()) {
                            val escapedCss = cosmeticCss
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", "\\n")
                                .replace("\r", "")
                            webView.evaluateJavascript("""
                                (function() {
                                    'use strict';
                                    if (window.__wta_cosmetic_observer__) return;
                                    window.__wta_cosmetic_observer__ = true;
                                    if (!document.querySelector('style[data-wta="cosmetic"]')) {
                                        var style = document.createElement('style');
                                        style.setAttribute('type', 'text/css');
                                        style.setAttribute('data-wta', 'cosmetic');
                                        style.textContent = '$escapedCss';
                                        (document.head || document.documentElement).appendChild(style);
                                    }
                                    var selectors = '$escapedCss'.match(/([^{]+)\{/g);
                                    if (selectors && selectors.length > 0) {
                                        var selectorList = selectors.map(function(s) {
                                            return s.replace(/\s*\{${'$'}/, '').trim();
                                        }).join(',');
                                        var pending = false;
                                        var observer = new MutationObserver(function() {
                                            if (pending) return;
                                            pending = true;
                                            (window.requestIdleCallback || setTimeout)(function() {
                                                pending = false;
                                                try {
                                                    var els = document.querySelectorAll(selectorList);
                                                    for (var i = 0; i < els.length; i++) {
                                                        if (els[i].style.display !== 'none') {
                                                            els[i].style.setProperty('display', 'none', 'important');
                                                            els[i].style.setProperty('visibility', 'hidden', 'important');
                                                        }
                                                    }
                                                } catch(e) {}
                                            }, { timeout: 100 });
                                        });
                                        observer.observe(document.documentElement, {
                                            childList: true, subtree: true
                                        });
                                        setTimeout(function() { observer.disconnect(); }, 30000);
                                    }
                                })();
                            """.trimIndent(), null)
                            AppLogger.d("WebViewManager", "DOCUMENT_END cosmetic observer injected for: $pageHost")
                        }
                    }
                }
            }, 200)
        }

        val finishedUrl = url
        webView.postDelayed({
            if (webView.url == finishedUrl) {
                injectScripts(webView, config.injectScripts, ScriptRunTime.DOCUMENT_IDLE, webView.url)
            }
        }, 500)

        if (config.performanceOptimization) {
            webView.postDelayed({
                if (webView.url == finishedUrl) {
                    val perfScript = com.webtoapp.core.linux.PerformanceOptimizer.generatePerformanceScript()
                    webView.evaluateJavascript(perfScript, null)
                    AppLogger.d("WebViewManager", "Performance optimization script injected")
                }
            }, 300)
        }

        if (config.pwaOfflineEnabled) {
            webView.postDelayed({
                if (webView.url == finishedUrl) {
                    val strategy = try {
                        PwaOfflineSupport.CacheStrategy.valueOf(config.pwaOfflineStrategy)
                    } catch (_: Exception) {
                        PwaOfflineSupport.CacheStrategy.NETWORK_FIRST
                    }
                    val offlineConfig = PwaOfflineSupport.OfflineConfig(
                        enabled = true,
                        strategy = strategy
                    )
                    PwaOfflineSupport.injectServiceWorker(webView, offlineConfig)
                }
            }, 800)
        }

        webView.removeCallbacks(cookieFlushRunnable)
        webView.postDelayed(cookieFlushRunnable, 3000)
        webView.requestFocus()
    }

    private fun injectDownloadBridgeScript(webView: WebView) {
        try {
            val script = DownloadBridge.getInjectionScript()
            webView.evaluateJavascript(script, null)
            AppLogger.d("WebViewManager", "Download bridge script injected")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Download bridge script injection failed", e)
        }
    }

    private fun injectExtensionPanelScript(webView: WebView) {
        try {
            val scripts = buildPanelInitScripts()
            if (scripts.isEmpty()) return
            scripts.forEach { script ->
                webView.evaluateJavascript(script, null)
            }
            AppLogger.d("WebViewManager", "Extension panel script injected")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Extension panel script injection failed", e)
        }
    }

    private fun injectIsolationScript(webView: WebView) {
        try {
            val isolationManager = com.webtoapp.core.isolation.IsolationManager.getInstance(context)
            val script = isolationManager.generateIsolationScript()
            if (script.isNotEmpty()) {
                webView.evaluateJavascript(script, null)
                AppLogger.d("WebViewManager", "Isolation script injected")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Isolation script injection failed", e)
        }
    }
}
