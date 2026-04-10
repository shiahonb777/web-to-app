package com.webtoapp.core.engine.shields

/**
 * GPC (Global Privacy Control) 注入器
 *
 * - JS 层：设置 navigator.globalPrivacyControl = true
 * - HTTP 层：添加 Sec-GPC: 1 Header
 *
 * GPC 是 W3C 标准草案，告知网站用户不同意个人数据的销售或共享
 * 已被 Firefox、Brave、DuckDuckGo 等浏览器支持
 */
class GpcInjector {

    /**
     * 生成 GPC JavaScript 注入代码
     * 在 DOCUMENT_START 时注入，确保页面 JS 能读取到
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
     * 获取需要添加到 HTTP 请求的 GPC Headers
     */
    fun getHeaders(): Map<String, String> = mapOf(
        "Sec-GPC" to "1",
        "DNT" to "1"
    )
}
