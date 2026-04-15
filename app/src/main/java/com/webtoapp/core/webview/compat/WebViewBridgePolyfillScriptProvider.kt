package com.webtoapp.core.webview

import com.webtoapp.data.model.WebViewConfig

internal class WebViewBridgePolyfillScriptProvider {
    fun shareBridgeScript(config: WebViewConfig, conservativeMode: Boolean): String? {
        if (!config.enableShareBridge || conservativeMode) return null
        return """
            // navigator.share polyfill for Android WebView
            (function() {
                'use strict';
                if (typeof NativeShareBridge !== 'undefined') {
                    navigator.share = function(data) {
                        return new Promise(function(resolve, reject) {
                            try {
                                var title = data.title || '';
                                var text = data.text || '';
                                var url = data.url || '';
                                NativeShareBridge.shareText(title, text, url);
                                resolve();
                            } catch(e) {
                                reject(e);
                            }
                        });
                    };
                    navigator.canShare = function(data) {
                        if (!data) return false;
                        if (data.files) return false;
                        return true;
                    };
                    console.log('[WebToApp] navigator.share polyfill loaded');
                }
            })();
        """.trimIndent()
    }

    fun clipboardPolyfillScript(conservativeMode: Boolean): String? {
        if (conservativeMode) return null
        return """
            // Clipboard API polyfill for Android WebView (HTTP compatibility)
            (function() {
                'use strict';
                if (window.__webtoapp_clipboard_polyfill__) return;
                window.__webtoapp_clipboard_polyfill__ = true;
                var hasBridge = typeof NativeBridge !== 'undefined';
                if (!hasBridge) {
                    console.log('[WebToApp] NativeBridge not found, clipboard polyfill skipped');
                    return;
                }
                var isSecureContext = window.isSecureContext;
                var needsPolyfill = !isSecureContext ||
                    !navigator.clipboard ||
                    typeof navigator.clipboard.readText !== 'function';
                if (!needsPolyfill) {
                    var originalWriteText = navigator.clipboard.writeText.bind(navigator.clipboard);
                    var originalReadText = navigator.clipboard.readText.bind(navigator.clipboard);
                    navigator.clipboard.writeText = function(text) {
                        return originalWriteText(text).catch(function(err) {
                            console.log('[WebToApp] Native clipboard write failed, using bridge:', err.message);
                            try {
                                NativeBridge.copyToClipboard(String(text));
                                return Promise.resolve();
                            } catch(e) {
                                return Promise.reject(e);
                            }
                        });
                    };
                    navigator.clipboard.readText = function() {
                        return originalReadText().catch(function(err) {
                            console.log('[WebToApp] Native clipboard read failed, using bridge:', err.message);
                            try {
                                var text = NativeBridge.getClipboardText();
                                return Promise.resolve(text || '');
                            } catch(e) {
                                return Promise.reject(e);
                            }
                        });
                    };
                    console.log('[WebToApp] Clipboard API wrapped with NativeBridge fallback');
                    return;
                }
                var clipboardPolyfill = {
                    writeText: function(text) {
                        return new Promise(function(resolve, reject) {
                            try {
                                NativeBridge.copyToClipboard(String(text));
                                resolve();
                            } catch(e) {
                                console.error('[WebToApp] Clipboard writeText error:', e);
                                reject(e);
                            }
                        });
                    },
                    readText: function() {
                        return new Promise(function(resolve, reject) {
                            try {
                                var text = NativeBridge.getClipboardText();
                                resolve(text || '');
                            } catch(e) {
                                console.error('[WebToApp] Clipboard readText error:', e);
                                reject(e);
                            }
                        });
                    },
                    write: function(data) {
                        return new Promise(function(resolve, reject) {
                            try {
                                if (data && data.length > 0) {
                                    var item = data[0];
                                    if (item.getType) {
                                        item.getType('text/plain').then(function(blob) {
                                            return blob.text();
                                        }).then(function(text) {
                                            NativeBridge.copyToClipboard(text);
                                            resolve();
                                        }).catch(function() {
                                            resolve();
                                        });
                                    } else {
                                        resolve();
                                    }
                                } else {
                                    resolve();
                                }
                            } catch(e) {
                                reject(e);
                            }
                        });
                    },
                    read: function() {
                        return new Promise(function(resolve, reject) {
                            try {
                                var text = NativeBridge.getClipboardText();
                                var blob = new Blob([text || ''], { type: 'text/plain' });
                                var item = new ClipboardItem({ 'text/plain': blob });
                                resolve([item]);
                            } catch(e) {
                                reject(e);
                            }
                        });
                    },
                    addEventListener: function() {},
                    removeEventListener: function() {},
                    dispatchEvent: function() { return true; }
                };
                try {
                    Object.defineProperty(navigator, 'clipboard', {
                        value: clipboardPolyfill,
                        writable: true,
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) {
                    try {
                        navigator.clipboard = clipboardPolyfill;
                    } catch(e2) {
                        console.warn('[WebToApp] Cannot override navigator.clipboard:', e2);
                    }
                }
                if (navigator.permissions && navigator.permissions.query) {
                    var originalQuery = navigator.permissions.query.bind(navigator.permissions);
                    navigator.permissions.query = function(desc) {
                        if (desc && (desc.name === 'clipboard-read' || desc.name === 'clipboard-write')) {
                            return Promise.resolve({
                                state: 'granted',
                                status: 'granted',
                                onchange: null,
                                addEventListener: function() {},
                                removeEventListener: function() {}
                            });
                        }
                        return originalQuery(desc);
                    };
                }
                var originalExecCommand = document.execCommand.bind(document);
                document.execCommand = function(command) {
                    if (command === 'copy') {
                        try {
                            var selection = window.getSelection();
                            if (selection && selection.toString()) {
                                NativeBridge.copyToClipboard(selection.toString());
                                return true;
                            }
                        } catch(e) {}
                    }
                    return originalExecCommand.apply(document, arguments);
                };
                console.log('[WebToApp] Clipboard API polyfill loaded (non-secure context)');
            })();
        """.trimIndent()
    }
}
