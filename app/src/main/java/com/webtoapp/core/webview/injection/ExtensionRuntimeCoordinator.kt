package com.webtoapp.core.webview

import android.content.Context
import android.webkit.WebView
import com.webtoapp.core.extension.ExtensionFileManager
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ExtensionPanelScript
import com.webtoapp.core.extension.ModuleRunTime
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.ScriptRunTime

internal class ExtensionRuntimeCoordinator(
    private val context: Context,
    private val extensionFileManager: ExtensionFileManager,
    private val state: WebViewSessionState
) {
    fun getActiveModulesForCurrentApp(): List<ExtensionModule> {
        val extensionManager = ExtensionManager.getInstance(context)
        return when {
            state.appExtensionModuleIds.isNotEmpty() -> {
                extensionManager.getModulesByIds(state.appExtensionModuleIds)
            }

            state.allowGlobalModuleFallback -> {
                extensionManager.getEnabledModules()
            }

            else -> emptyList()
        }
    }

    fun hasAnyEnabledModules(): Boolean {
        if (state.embeddedModules.any { it.enabled }) return true
        return getActiveModulesForCurrentApp().isNotEmpty()
    }

    fun buildPanelInitScripts(): List<String> {
        if (!hasAnyEnabledModules()) {
            AppLogger.d("WebViewManager", "No enabled modules, skip panel script injection")
            return emptyList()
        }

        return listOf(
            ExtensionPanelScript.getPanelInitScript(state.extensionFabIcon),
            ExtensionPanelScript.getModuleHelperScript()
        )
    }

    fun initChromeExtensionRuntimes(webView: WebView) {
        state.extensionRuntimes.values.forEach { it.destroy() }
        state.extensionRuntimes.clear()

        try {
            val chromeExtModules = getActiveModulesForCurrentApp().filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                    module.chromeExtId.isNotEmpty() &&
                    module.backgroundScript.isNotEmpty()
            }

            if (chromeExtModules.isEmpty()) return

            val extensionGroups = chromeExtModules.groupBy { it.chromeExtId }
            for ((extId, modules) in extensionGroups) {
                val primaryModule = modules.first()
                val originUrl = com.webtoapp.core.extension.deriveOriginUrl(primaryModule.urlMatches)
                val runtime = com.webtoapp.core.extension.ChromeExtensionRuntime(
                    context = context,
                    extensionId = extId,
                    backgroundScriptPath = primaryModule.backgroundScript,
                    originUrl = originUrl
                )
                runtime.initialize(webView)
                state.extensionRuntimes[extId] = runtime
                AppLogger.d("WebViewManager", "Created background runtime for extension: $extId")
            }

            if (state.extensionRuntimes.isNotEmpty()) {
                val contentBridge = com.webtoapp.core.extension.ContentExtensionBridge(state.extensionRuntimes)
                webView.addJavascriptInterface(
                    contentBridge,
                    com.webtoapp.core.extension.ChromeExtensionRuntime.JS_BRIDGE_NAME
                )
                AppLogger.d("WebViewManager", "Registered WtaExtBridge for ${state.extensionRuntimes.size} extension(s)")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to init Chrome Extension runtimes", e)
        }
    }

    fun injectAllExtensionModules(webView: WebView, url: String, runAt: ScriptRunTime) {
        if (state.embeddedModules.isNotEmpty()) {
            injectEmbeddedModules(webView, url, runAt)
            return
        }

        val moduleRunAt = runAt.toModuleRunTime()
        val allModules = resolveActiveExtensionModules()
        if (allModules.isEmpty()) {
            AppLogger.d("WebViewManager", "injectAllExtensionModules: No active modules (${runAt.name})")
            return
        }

        AppLogger.d("WebViewManager", "injectAllExtensionModules: runAt=${runAt.name}, url=$url, totalModules=${allModules.size}")

        if (runAt == ScriptRunTime.DOCUMENT_START) {
            injectEarlyCss(webView, allModules, url, moduleRunAt)
        }

        val matching = allModules.filter { it.runAt == moduleRunAt && it.matchesUrl(url) }
        val chromeModules = matching.filter {
            it.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                it.chromeExtId.isNotEmpty()
        }
        val userscriptModules = matching.filter {
            it.sourceType == com.webtoapp.core.extension.ModuleSourceType.USERSCRIPT
        }
        val customModules = matching.filter {
            it.sourceType != com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                it.sourceType != com.webtoapp.core.extension.ModuleSourceType.USERSCRIPT
        }

        if (chromeModules.isNotEmpty()) {
            injectChromeExtModules(webView, chromeModules, runAt)
        }
        if (userscriptModules.isNotEmpty()) {
            injectUserscriptModules(webView, userscriptModules)
        }
        if (customModules.isNotEmpty()) {
            injectCustomModules(webView, customModules)
        }

        AppLogger.d(
            "WebViewManager",
            "injectAllExtensionModules: Injected ${chromeModules.size} chrome + ${userscriptModules.size} userscript + ${customModules.size} custom modules (${runAt.name})"
        )

        if (runAt == ScriptRunTime.DOCUMENT_END) {
            registerAllModulesInPanel(webView, allModules, url)
        }
    }

    private fun resolveActiveExtensionModules(): List<ExtensionModule> {
        return when {
            state.appExtensionModuleIds.isNotEmpty() -> {
                ExtensionManager.getInstance(context).getModulesByIds(state.appExtensionModuleIds)
            }

            state.allowGlobalModuleFallback -> {
                ExtensionManager.getInstance(context).getEnabledModules()
            }

            else -> emptyList()
        }
    }

    private fun injectEmbeddedModules(webView: WebView, url: String, runAt: ScriptRunTime) {
        try {
            val targetRunAt = runAt.name
            AppLogger.d("WebViewManager", "injectEmbeddedModules: url=$url, runAt=$targetRunAt, totalModules=${state.embeddedModules.size}")

            val matchingModules = state.embeddedModules.filter { module ->
                module.enabled && module.runAt == targetRunAt && module.matchesUrl(url)
            }

            if (matchingModules.isEmpty()) {
                AppLogger.d("WebViewManager", "injectEmbeddedModules: No matching modules")
                return
            }

            val injectionCode = matchingModules.joinToString("\n\n") { module ->
                """
                // ========== ${module.name} ==========
                (function() {
                    try {
                        ${module.generateExecutableCode()}
                    } catch(__moduleError__) {
                        console.error('[WebToApp Module Error] ${module.name}:', __moduleError__);
                    }
                })();
                """.trimIndent()
            }

            webView.evaluateJavascript(injectionCode, null)
            AppLogger.d("WebViewManager", "Injected ${matchingModules.size} embedded module(s) (${runAt.name})")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Embedded module injection failed", e)
        }
    }

    private fun injectEarlyCss(
        webView: WebView,
        allModules: List<ExtensionModule>,
        url: String,
        currentRunAt: ModuleRunTime
    ) {
        try {
            val earlyCssModules = allModules.filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                    module.chromeExtId.isNotEmpty() &&
                    module.cssCode.isNotBlank() &&
                    module.runAt != currentRunAt &&
                    module.matchesUrl(url)
            }
            if (earlyCssModules.isEmpty()) return

            val cssBuilder = StringBuilder()
            for (module in earlyCssModules) {
                val extId = module.chromeExtId
                val escapedCss = module.cssCode
                    .replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("$", "\\$")
                cssBuilder.appendLine(
                    """
                    (function() {
                        try {
                            var style = document.createElement('style');
                            style.setAttribute('data-wta-ext', '$extId');
                            style.setAttribute('data-wta-early-css', 'true');
                            style.textContent = `$escapedCss`;
                            (document.head || document.documentElement).appendChild(style);
                        } catch(e) { console.warn('[WTA] Early CSS injection error:', e); }
                    })();
                    """.trimIndent()
                )
            }
            webView.evaluateJavascript(cssBuilder.toString(), null)
            AppLogger.d("WebViewManager", "Early CSS injected for ${earlyCssModules.size} Chrome extension module(s)")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Early CSS injection failed", e)
        }
    }

    private fun injectChromeExtModules(
        webView: WebView,
        modules: List<ExtensionModule>,
        runAt: ScriptRunTime
    ) {
        try {
            val extensionGroups = modules.groupBy { it.chromeExtId }
            if (extensionGroups.size == 1) {
                val codeBuilder = StringBuilder()
                if (runAt == ScriptRunTime.DOCUMENT_START) {
                    codeBuilder.appendLine(com.webtoapp.core.extension.ChromeExtensionMobileCompat.generateCompatScript())
                    codeBuilder.appendLine()
                }

                for ((extId, extModules) in extensionGroups) {
                    codeBuilder.appendLine(
                        com.webtoapp.core.extension.ChromeExtensionPolyfill.generatePolyfill(extensionId = extId)
                    )
                    codeBuilder.appendLine()
                    appendChromeExtCss(codeBuilder, extId, extModules)
                    appendChromeExtScripts(codeBuilder, extModules)
                }

                val combinedCode = codeBuilder.toString()
                if (combinedCode.isNotBlank()) {
                    webView.evaluateJavascript(combinedCode, null)
                }
                AppLogger.d("WebViewManager", "Injected Chrome extension polyfills for ${modules.size} module(s) (${runAt.name})")
            } else {
                if (runAt == ScriptRunTime.DOCUMENT_START) {
                    webView.evaluateJavascript(
                        com.webtoapp.core.extension.ChromeExtensionMobileCompat.generateCompatScript(),
                        null
                    )
                }
                for ((extId, extModules) in extensionGroups) {
                    val extBuilder = StringBuilder()
                    extBuilder.appendLine(com.webtoapp.core.extension.ChromeExtensionPolyfill.generatePolyfill(extensionId = extId))
                    appendChromeExtCss(extBuilder, extId, extModules)
                    appendChromeExtScripts(extBuilder, extModules)
                    webView.evaluateJavascript(extBuilder.toString(), null)
                }
                AppLogger.d(
                    "WebViewManager",
                    "Injected Chrome extension polyfills for ${modules.size} module(s) across ${extensionGroups.size} extension(s) (${runAt.name})"
                )
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Chrome extension module injection failed", e)
        }
    }

    private fun appendChromeExtCss(
        builder: StringBuilder,
        extId: String,
        modules: List<ExtensionModule>
    ) {
        modules.filter { it.cssCode.isNotBlank() }.forEach { module ->
            val escapedCss = module.cssCode
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            builder.appendLine(
                """
                // ===== CSS: ${module.name} =====
                (function() {
                    try {
                        var style = document.createElement('style');
                        style.setAttribute('data-wta-ext', '$extId');
                        style.textContent = `$escapedCss`;
                        (document.head || document.documentElement).appendChild(style);
                    } catch(e) {
                        console.error('[WebToApp Chrome Ext CSS] ${module.name}:', e);
                    }
                })();
                """.trimIndent()
            )
            builder.appendLine()
        }
    }

    private fun appendChromeExtScripts(
        builder: StringBuilder,
        modules: List<ExtensionModule>
    ) {
        for (module in modules) {
            if (module.code.isBlank()) continue

            if (module.world == "MAIN") {
                builder.appendLine(
                    """
                    // ===== MAIN world: ${module.name} =====
                    (function() {
                        if (typeof __INTLIFY_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__INTLIFY_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_OPTIONS_API__ === 'undefined') { try { Object.defineProperty(window, '__VUE_OPTIONS_API__', { value: true, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_HYDRATION_MISMATCH_DETAILS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                    })();
                    try {
                        ${module.code}
                    } catch(__extError__) {
                        console.error('[WebToApp Chrome Ext Error] ${module.name} (MAIN):', __extError__);
                    }
                    """.trimIndent()
                )
            } else {
                builder.appendLine(
                    """
                    // ===== ISOLATED world: ${module.name} =====
                    (function() {
                        if (typeof __INTLIFY_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__INTLIFY_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_OPTIONS_API__ === 'undefined') { try { Object.defineProperty(window, '__VUE_OPTIONS_API__', { value: true, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_HYDRATION_MISMATCH_DETAILS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        try {
                            ${module.code}
                        } catch(__extError__) {
                            console.error('[WebToApp Chrome Ext Error] ${module.name}:', __extError__);
                        }
                    })();
                    """.trimIndent()
                )
            }
            builder.appendLine()
        }
    }

    private fun injectUserscriptModules(
        webView: WebView,
        modules: List<ExtensionModule>
    ) {
        try {
            val windowManagerJs = com.webtoapp.core.extension.UserScriptWindowScript.getWindowManagerScript()
            val combinedCode = windowManagerJs + "\n\n" + modules.joinToString("\n\n") { module ->
                val scriptInfo = mapOf(
                    "name" to module.name,
                    "version" to module.version.name,
                    "description" to module.description,
                    "author" to (module.author?.name ?: ""),
                    "namespace" to module.id
                )
                val resolvedResources = module.resources.mapValues { (name, url) ->
                    extensionFileManager.getCachedResource(name, url) ?: url
                }
                val polyfill = com.webtoapp.core.extension.GreasemonkeyBridge.generatePolyfillScript(
                    scriptId = module.id,
                    grants = module.gmGrants,
                    scriptInfo = scriptInfo,
                    resources = resolvedResources
                )
                val requireJs = module.requireUrls.mapNotNull { url ->
                    extensionFileManager.getCachedRequire(url)
                }.joinToString("\n\n")

                """
                // ========== [Userscript] ${module.name} ==========
                (function() {
                    try {
                        $polyfill
                        $requireJs
                        ${module.code}
                    } catch(__usError__) {
                        console.error('[WebToApp Userscript Error] ${module.name}:', __usError__);
                    }
                })();
                """.trimIndent()
            }

            webView.evaluateJavascript(combinedCode, null)
            AppLogger.d("WebViewManager", "Injected ${modules.size} userscript module(s) with GM polyfills")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Userscript module injection failed", e)
        }
    }

    private fun injectCustomModules(
        webView: WebView,
        modules: List<ExtensionModule>
    ) {
        try {
            val injectionCode = modules.joinToString("\n\n") { module ->
                """
                // ========== ${module.name} (${module.version.name}) ==========
                (function() {
                    try {
                        ${module.generateExecutableCode()}
                    } catch(__moduleError__) {
                        console.error('[WebToApp Module Error] ${module.name}:', __moduleError__);
                    }
                })();
                """.trimIndent()
            }

            if (injectionCode.isNotBlank()) {
                webView.evaluateJavascript(injectionCode, null)
                AppLogger.d("WebViewManager", "Injected ${modules.size} custom module(s)")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Custom module injection failed", e)
        }
    }

    private fun registerAllModulesInPanel(
        webView: WebView,
        allModules: List<ExtensionModule>,
        url: String
    ) {
        try {
            if (allModules.isEmpty()) return

            val chromeModules = allModules.filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                    module.chromeExtId.isNotEmpty()
            }
            val nonChromeModules = allModules.filter { module ->
                module.sourceType != com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION
            }

            if (chromeModules.isEmpty() && nonChromeModules.isEmpty()) return

            val registeredExtIds = mutableSetOf<String>()
            val regBuilder = StringBuilder()

            for (module in chromeModules) {
                val extId = module.chromeExtId.ifBlank { module.id }
                if (extId in registeredExtIds) continue
                registeredExtIds.add(extId)

                val extModules = chromeModules.filter {
                    (it.chromeExtId.ifBlank { it.id }) == extId
                }
                val jsName = module.name.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsDesc = module.description.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsVersion = module.version.name.replace("\\", "\\\\").replace("'", "\\'")
                val jsAuthor = (module.author?.name ?: "").replace("\\", "\\\\").replace("'", "\\'")
                val iconHtml = if (module.icon.isNotBlank()) {
                    module.icon.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "")
                } else {
                    ""
                }
                val urlPatterns = extModules.flatMap { it.urlMatches }
                    .filter { !it.exclude }
                    .map { it.pattern.replace("\\", "\\\\").replace("'", "\\'") }
                    .distinct()
                val urlMatchesJs = urlPatterns.joinToString(",") { "'$it'" }
                val perms = extModules.flatMap { it.permissions }
                    .map { it.name }
                    .distinct()
                val permsJs = perms.joinToString(",") { "'$it'" }
                val matchesPage = extModules.any { it.matchesUrl(url) }

                regBuilder.appendLine(
                    """
                    (function() {
                        function _reg() {
                            if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(_reg, 100); return; }
                            __WTA_MODULE_UI__.register({
                                id: '$extId',
                                name: '$jsName',
                                description: '$jsDesc',
                                version: '$jsVersion',
                                author: '$jsAuthor',
                                icon: '$iconHtml',
                                sourceType: 'CHROME_EXTENSION',
                                active: $matchesPage,
                                urlMatches: [${urlMatchesJs}],
                                permissions: [${permsJs}],
                                world: ""${module.world}"",
                                runAt: ""${module.runAt.name}"",
                                runMode: ""${module.runMode.name}""
                            });
                        }
                        setTimeout(_reg, 50);
                    })();
                    """.trimIndent()
                )
            }

            for (module in nonChromeModules) {
                val jsName = module.name.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsDesc = module.description.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsVersion = module.version.name.replace("\\", "\\\\").replace("'", "\\'")
                val jsAuthor = (module.author?.name ?: "").replace("\\", "\\\\").replace("'", "\\'")
                val iconHtml = if (module.icon.isNotBlank()) {
                    module.icon.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "")
                } else {
                    ""
                }
                val matchesPage = module.matchesUrl(url)

                regBuilder.appendLine(
                    """
                    (function() {
                        function _reg() {
                            if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(_reg, 100); return; }
                            __WTA_MODULE_UI__.register({
                                id: ""${module.id.replace("'", "\\" + "'")}"",
                                name: '$jsName',
                                description: '$jsDesc',
                                version: '$jsVersion',
                                author: '$jsAuthor',
                                icon: '$iconHtml',
                                sourceType: ""${module.sourceType.name}"",
                                active: $matchesPage,
                                urlMatches: [],
                                permissions: [],
                                world: ""${module.world}"",
                                runAt: ""${module.runAt.name}"",
                                runMode: ""${module.runMode.name}""
                            });
                        }
                        setTimeout(_reg, 50);
                    })();
                    """.trimIndent()
                )
            }

            if (regBuilder.isNotBlank()) {
                webView.evaluateJavascript(regBuilder.toString(), null)
                AppLogger.d("WebViewManager", "Registered ${registeredExtIds.size} Chrome ext(s) + ${nonChromeModules.size} module(s) in panel")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Panel registration failed", e)
        }
    }

    private fun ScriptRunTime.toModuleRunTime(): ModuleRunTime = when (this) {
        ScriptRunTime.DOCUMENT_START -> ModuleRunTime.DOCUMENT_START
        ScriptRunTime.DOCUMENT_END -> ModuleRunTime.DOCUMENT_END
        ScriptRunTime.DOCUMENT_IDLE -> ModuleRunTime.DOCUMENT_IDLE
    }
}
