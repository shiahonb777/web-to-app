package com.webtoapp.core.engine.shields

/**
 * GPC Global Privacy Control.
 *
 * JS navigator.globalPrivacyControl true.
 * HTTP Sec-GPC Header.
 *
 * user.
 * Firefox Brave DuckDuckGo.
 */
class GpcInjector {

    /**
     * generate.
     * read page.
     */
    fun generateScript(): String {
        return """
            // Global Privacy Control (GPC) — W3C Draft
            (function() {
                'use strict';
                if (window.__webtoapp_gpc_injected__) return;
                window.__webtoapp_gpc_injected__ = true;
                
                try {
                    // Set navigator.globalPrivacyControl
                    Object.defineProperty(navigator, 'globalPrivacyControl', {
                        value: true,
                        writable: false,
                        configurable: false,
                        enumerable: true
                    });
                    
                    // Also set on Navigator.prototype for completeness
                    Object.defineProperty(Navigator.prototype, 'globalPrivacyControl', {
                        value: true,
                        writable: false,
                        configurable: true,
                        enumerable: true
                    });
                    
                    // Set Do Not Track (DNT) — legacy but still checked by some sites
                    Object.defineProperty(navigator, 'doNotTrack', {
                        value: '1',
                        writable: false,
                        configurable: true,
                        enumerable: true
                    });
                    
                    console.log('[WebToApp Shields] GPC enabled: navigator.globalPrivacyControl =', navigator.globalPrivacyControl);
                } catch(e) {
                    console.error('[WebToApp Shields] GPC injection failed:', e);
                }
            })();
        """.trimIndent()
    }

    /**
     * HTTP GPC Headers.
     */
    fun getHeaders(): Map<String, String> = mapOf(
        "Sec-GPC" to "1",
        "DNT" to "1"
    )
}