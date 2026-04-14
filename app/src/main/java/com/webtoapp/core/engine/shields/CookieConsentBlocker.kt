package com.webtoapp.core.engine.shields

/**
 * auto.
 *
 * Note.
 * CSS Cookie.
 * auto.
 * load.
 *
 * Cookie.
 */
class CookieConsentBlocker {

    /**
     * generate script blocker.
     * DOCUMENT_END.
     */
    fun generateScript(): String {
        return """
            // Cookie Consent Auto-Dismiss
            (function() {
                'use strict';
                if (window.__webtoapp_cookie_consent_blocked__) return;
                window.__webtoapp_cookie_consent_blocked__ = true;
                
                // Phase CSS.
                var style = document.createElement('style');
                style.id = 'webtoapp-cookie-consent-block';
                style.textContent = [
                    // === Generic frameworks ===
                    '#cookie-notice',
                    '#cookie-consent',
                    '#cookie-banner',
                    '#cookiebanner',
                    '#cookie-popup',
                    '#cookie-bar',
                    '#cookie-law',
                    '#cookie-warning',
                    '#cookie-accept',
                    '#cookie-policy',
                    '#cookie-info',
                    '#cookie-message',
                    '#cookies-popup',
                    '#gdpr-banner',
                    '#gdpr-consent',
                    '#gdpr-popup',
                    '#privacy-banner',
                    '#consent-banner',
                    '#consent-popup',
                    '#consent-bar',
                    '.cookie-notice',
                    '.cookie-consent',
                    '.cookie-banner',
                    '.cookie-popup',
                    '.cookie-bar',
                    '.cookie-warning',
                    '.cookie-modal',
                    '.cookies-banner',
                    '.gdpr-banner',
                    '.gdpr-popup',
                    '.consent-banner',
                    '.consent-popup',
                    '.consent-bar',
                    '.privacy-banner',
                    '.cc-banner',
                    '.cc-window',
                    '.cc-revoke',
                    '.cc-overlay',
                    // === Popular CMPs ===
                    '#onetrust-banner-sdk',
                    '#onetrust-consent-sdk',
                    '.onetrust-pc-dark-filter',
                    '#CybotCookiebotDialog',
                    '#CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll',
                    '.cmp-container',
                    '.cmp-banner',
                    '#didomi-host',
                    '#didomi-popup',
                    '.didomi-popup-container',
                    '#sp_message_container',
                    '#sp-cc',
                    '.sp-message-open',
                    '#qc-cmp2-container',
                    '#qc-cmp-ui-container',
                    '.evidon-consent-button',
                    '#evidon-banner',
                    '.truste_box_overlay',
                    '#truste-consent-track',
                    '#usercentrics-root',
                    '[data-testid="uc-default-wall"]',
                    '.osano-cm-window',
                    '.js-cookie-consent',
                    '#moove_gdpr_cookie_info_bar',
                    '.pea_cook_wrapper',
                    '#catapult-cookie-bar',
                    '#cookie-law-info-bar',
                    '.cli-modal-backdrop',
                    // === Overlays / backdrops ===
                    '.cookie-overlay',
                    '.consent-overlay',
                    '.gdpr-overlay',
                    '.cmp-overlay'
                ].join(', ') + ' { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; z-index: -1 !important; }';
                (document.head || document.documentElement).appendChild(style);
                
                // Restore body scrolling (many CMPs lock scroll)
                document.documentElement.style.setProperty('overflow', 'auto', 'important');
                if (document.body) {
                    document.body.style.setProperty('overflow', 'auto', 'important');
                }
                
                // auto.
                var rejectPatterns = [
                    // English
                    /reject\s*all/i, /decline\s*all/i, /deny\s*all/i,
                    /refuse\s*all/i, /only\s*necessary/i, /necessary\s*only/i,
                    /essential\s*only/i, /only\s*essential/i,
                    /manage\s*preferences/i, /cookie\s*settings/i,
                    // Chinese
                    /拒绝/i, /仅必要/i, /仅接受必要/i, /关闭/i,
                    // German
                    /alle\s*ablehnen/i, /nur\s*notwendige/i,
                    // French
                    /tout\s*refuser/i, /refuser\s*tout/i,
                    // Spanish
                    /rechazar\s*todo/i
                ];
                
                var acceptPatterns = [
                    // Fallback: if no reject button, accept to dismiss
                    /accept\s*all/i, /accept\s*cookies/i, /agree/i,
                    /i\s*agree/i, /got\s*it/i, /ok/i, /allow\s*all/i,
                    /接受/i, /同意/i, /我同意/i, /知道了/i,
                    /alle\s*akzeptieren/i, /tout\s*accepter/i,
                    /aceptar\s*todo/i
                ];
                
                function tryClickButtons() {
                    var buttons = document.querySelectorAll('button, a[role="button"], [class*="button"], [class*="btn"], input[type="button"], input[type="submit"]');
                    var clicked = false;
                    
                    // First pass: try reject/necessary-only buttons
                    for (var i = 0; i < buttons.length; i++) {
                        var btn = buttons[i];
                        if (!btn.offsetParent) continue; // skip hidden
                        var text = (btn.textContent || btn.value || '').trim();
                        if (text.length > 100) continue; // skip non-button elements
                        
                        for (var j = 0; j < rejectPatterns.length; j++) {
                            if (rejectPatterns[j].test(text)) {
                                btn.click();
                                console.log('[WebToApp Shields] Cookie consent: clicked reject button:', text);
                                return true;
                            }
                        }
                    }
                    
                    // Second pass: accept as fallback (better than leaving modal)
                    for (var i = 0; i < buttons.length; i++) {
                        var btn = buttons[i];
                        if (!btn.offsetParent) continue;
                        var text = (btn.textContent || btn.value || '').trim();
                        if (text.length > 100) continue;
                        
                        for (var j = 0; j < acceptPatterns.length; j++) {
                            if (acceptPatterns[j].test(text)) {
                                btn.click();
                                console.log('[WebToApp Shields] Cookie consent: clicked accept button:', text);
                                return true;
                            }
                        }
                    }
                    
                    return false;
                }
                
                // ========== Phase 3: MutationObserver for dynamic popups ==========
                var attempts = 0;
                var maxAttempts = 10;
                var dismissInterval = null;
                
                function tryDismiss() {
                    if (tryClickButtons()) {
                        // Success — restore scroll
                        document.documentElement.style.removeProperty('overflow');
                        if (document.body) document.body.style.removeProperty('overflow');
                        if (dismissInterval) clearInterval(dismissInterval);
                        return;
                    }
                    attempts++;
                    if (attempts >= maxAttempts && dismissInterval) {
                        clearInterval(dismissInterval);
                    }
                }
                
                // Try immediately
                if (document.readyState !== 'loading') {
                    tryDismiss();
                }
                
                // Retry periodically (catch delayed popups)
                dismissInterval = setInterval(tryDismiss, 1000);
                
                // Also observe DOM for late-loaded banners
                var observer = new MutationObserver(function(mutations) {
                    for (var i = 0; i < mutations.length; i++) {
                        var added = mutations[i].addedNodes;
                        for (var j = 0; j < added.length; j++) {
                            var node = added[j];
                            if (node.nodeType !== 1) continue;
                            var id = (node.id || '').toLowerCase();
                            var cls = (node.className || '').toString().toLowerCase();
                            if (id.indexOf('cookie') !== -1 || id.indexOf('consent') !== -1 || 
                                id.indexOf('gdpr') !== -1 || id.indexOf('privacy') !== -1 ||
                                cls.indexOf('cookie') !== -1 || cls.indexOf('consent') !== -1 ||
                                cls.indexOf('gdpr') !== -1 || cls.indexOf('cmp') !== -1) {
                                // Found a cookie popup node — try dismiss
                                setTimeout(tryDismiss, 300);
                                break;
                            }
                        }
                    }
                });
                
                observer.observe(document.documentElement, {
                    childList: true,
                    subtree: true
                });
                
                // Stop observer after 30 seconds (performance)
                setTimeout(function() {
                    observer.disconnect();
                    if (dismissInterval) clearInterval(dismissInterval);
                }, 30000);
                
                console.log('[WebToApp Shields] Cookie consent blocker loaded');
            })();
        """.trimIndent()
    }
}