package com.webtoapp.core.webview

import com.webtoapp.data.model.WebViewConfig

internal class WebViewInteractionScriptProvider {
    fun hideLinkPreviewScript(conservativeMode: Boolean): String? {
        if (conservativeMode) return null
        return """
            // Hide link URL preview for privacy
            (function() {
                'use strict';
                if (window.__wtaLinkPreviewHidden) return;
                window.__wtaLinkPreviewHidden = true;
                var style = document.createElement('style');
                style.id = 'webtoapp-hide-url-preview';
                style.textContent = '\n' +
                    'a, a * {\n' +
                    '  -webkit-touch-callout: none !important;\n' +
                    '  -webkit-user-select: none !important;\n' +
                    '  user-select: none !important;\n' +
                    '}\n';
                (document.head || document.documentElement).appendChild(style);
                function findAnchorParent(el) {
                    var current = el;
                    var depth = 0;
                    while (current && depth < 15) {
                        if (current.tagName && current.tagName.toUpperCase() === 'A') return current;
                        current = current.parentElement;
                        depth++;
                    }
                    return null;
                }
                document.addEventListener('contextmenu', function(e) {
                    if (findAnchorParent(e.target)) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                        return false;
                    }
                }, true);
                document.addEventListener('selectstart', function(e) {
                    if (findAnchorParent(e.target)) {
                        e.preventDefault();
                    }
                }, true);
                function removeAllTitles() {
                    document.querySelectorAll('a[title]').forEach(function(link) {
                        link.removeAttribute('title');
                    });
                }
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', removeAllTitles);
                } else {
                    removeAllTitles();
                }
                var titleObserver = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) {
                                if (node.tagName === 'A' && node.hasAttribute('title')) {
                                    node.removeAttribute('title');
                                }
                                node.querySelectorAll && node.querySelectorAll('a[title]').forEach(function(link) {
                                    link.removeAttribute('title');
                                });
                            }
                        });
                    });
                });
                if (document.body) {
                    titleObserver.observe(document.body, { childList: true, subtree: true });
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        titleObserver.observe(document.body, { childList: true, subtree: true });
                    });
                }
                var originalSetAttribute = Element.prototype.setAttribute;
                Element.prototype.setAttribute = function(name, value) {
                    if (this.tagName === 'A' && name.toLowerCase() === 'title') {
                        return;
                    }
                    return originalSetAttribute.call(this, name, value);
                };
                console.log('[WebToApp] Link URL preview hidden (enhanced)');
            })();
        """.trimIndent()
    }

    fun popupBlockerScript(config: WebViewConfig, conservativeMode: Boolean): String? {
        if (!config.popupBlockerEnabled || conservativeMode) return null
        return """
            // Popup Blocker - blocks unwanted popups and redirects
            (function() {
                'use strict';
                window.__webtoapp_popup_blocker_enabled__ = true;
                var blockedCount = 0;
                var allowedDomains = [];
                var originalOpen = window.open;
                function isSuspiciousUrl(url) {
                    if (!url) return true;
                    var lowerUrl = url.toLowerCase();
                    var suspiciousPatterns = [
                        'doubleclick', 'googlesyndication', 'googleadservices',
                        'facebook.com/tr', 'analytics', 'tracker',
                        'popup', 'popunder', 'clickunder',
                        'adserver', 'adservice', 'adsense',
                        'javascript:void', 'about:blank',
                        'data:text/html'
                    ];
                    return suspiciousPatterns.some(function(pattern) {
                        return lowerUrl.indexOf(pattern) !== -1;
                    });
                }
                function isDomainAllowed(url) {
                    if (!url || allowedDomains.length === 0) return false;
                    try {
                        var urlObj = new URL(url, window.location.href);
                        return allowedDomains.some(function(domain) {
                            return urlObj.hostname.indexOf(domain) !== -1;
                        });
                    } catch(e) {
                        return false;
                    }
                }
                window.open = function(url) {
                    if (!window.__webtoapp_popup_blocker_enabled__) {
                        return originalOpen.apply(window, arguments);
                    }
                    var isSameOrigin = false;
                    try {
                        if (url) {
                            var urlObj = new URL(url, window.location.href);
                            isSameOrigin = urlObj.origin === window.location.origin;
                        }
                    } catch(e) {}
                    var shouldBlock = false;
                    if (!url || url === 'about:blank' || url.indexOf('javascript:') === 0) {
                        shouldBlock = true;
                    } else if (isSuspiciousUrl(url) && !isSameOrigin && !isDomainAllowed(url)) {
                        shouldBlock = true;
                    }
                    if (shouldBlock) {
                        blockedCount++;
                        console.log('[WebToApp PopupBlocker] Blocked popup #' + blockedCount + ':', url || '(empty)');
                        return {
                            closed: true,
                            close: function() {},
                            focus: function() {},
                            blur: function() {},
                            postMessage: function() {},
                            location: { href: '' },
                            document: { write: function() {}, close: function() {} }
                        };
                    }
                    var result = originalOpen.apply(window, arguments);
                    if (!result) {
                        return {
                            closed: false,
                            close: function() {},
                            focus: function() {},
                            blur: function() {},
                            postMessage: function() {},
                            location: { href: url || '' }
                        };
                    }
                    return result;
                };
                var originalSetTimeout = window.setTimeout;
                window.setTimeout = function(fn, delay) {
                    if (delay === 0 && typeof fn === 'string' && fn.indexOf('open(') !== -1) {
                        console.log('[WebToApp PopupBlocker] Blocked setTimeout popup trigger');
                        return 0;
                    }
                    return originalSetTimeout.apply(window, arguments);
                };
                window.__webtoapp_toggle_popup_blocker__ = function(enabled) {
                    window.__webtoapp_popup_blocker_enabled__ = enabled;
                    console.log('[WebToApp PopupBlocker] ' + (enabled ? 'Enabled' : 'Disabled'));
                };
                window.__webtoapp_popup_blocker_stats__ = function() {
                    return { blocked: blockedCount, enabled: window.__webtoapp_popup_blocker_enabled__ };
                };
                console.log('[WebToApp] Popup blocker loaded');
            })();
        """.trimIndent()
    }
}
