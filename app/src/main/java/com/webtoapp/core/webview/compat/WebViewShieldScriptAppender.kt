package com.webtoapp.core.webview

import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig

internal class WebViewShieldScriptAppender(
    private val adBlocker: AdBlocker,
    private val urlPolicy: WebViewUrlPolicy,
    private val getShields: () -> BrowserShields?
) {
    fun appendShieldAndAdBlockScripts(
        scripts: MutableList<String>,
        pageUrl: String?,
        config: WebViewConfig,
        conservativeMode: Boolean
    ) {
        val shields = getShields()
        val canInjectShieldsJs = !conservativeMode
        if (canInjectShieldsJs && !config.disableShields && shields?.isEnabled() == true && shields.getConfig().gpcEnabled) {
            scripts.add(shields.gpcInjector.generateScript())
        }
        if (canInjectShieldsJs && !config.disableShields && shields?.isEnabled() == true && shields.getConfig().cookieConsentBlock) {
            scripts.add(shields.cookieConsentBlocker.generateScript())
            shields.stats.recordCookieConsentBlocked()
        }
        if (canInjectShieldsJs && !config.disableShields && shields?.isEnabled() == true) {
            val referrerPolicy = shields.getConfig().referrerPolicy.value
            scripts.add(
                """
                // Shields: Referrer Policy
                (function() {
                    'use strict';
                    if (window.__webtoapp_referrer_policy__) return;
                    window.__webtoapp_referrer_policy__ = true;
                    var meta = document.createElement('meta');
                    meta.name = 'referrer';
                    meta.content = '$referrerPolicy';
                    (document.head || document.documentElement).appendChild(meta);
                    console.log('[WebToApp Shields] Referrer policy set:', '$referrerPolicy');
                })();
                """.trimIndent()
            )
        }
        if (canInjectShieldsJs && adBlocker.isEnabled()) {
            val adPageHost = urlPolicy.extractHostFromUrl(pageUrl).orEmpty()
            if (adPageHost.isNotEmpty()) {
                val cosmeticCss = adBlocker.getCosmeticFilterCss(adPageHost)
                if (cosmeticCss.isNotEmpty()) {
                    val escapedCss = cosmeticCss
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                    scripts.add(
                        """
                        // AdBlocker: Cosmetic element hiding
                        (function() {
                            'use strict';
                            if (window.__wta_cosmetic_filters__) return;
                            window.__wta_cosmetic_filters__ = true;
                            try {
                                var style = document.createElement('style');
                                style.setAttribute('type', 'text/css');
                                style.setAttribute('data-wta', 'cosmetic');
                                style.textContent = '$escapedCss';
                                (document.head || document.documentElement).appendChild(style);
                            } catch(e) { console.warn('[WTA] Cosmetic filter injection error:', e); }
                        })();
                        """.trimIndent()
                    )
                    AppLogger.d("WebViewManager", "Cosmetic filters injected for: $adPageHost")
                }
                val antiAdblockScript = adBlocker.getAntiAdblockScript(adPageHost)
                if (antiAdblockScript.isNotEmpty()) {
                    scripts.add(antiAdblockScript)
                }
            }
        }
    }
}
