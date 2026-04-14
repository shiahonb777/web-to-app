package com.webtoapp.core.webview

internal class WebViewMiscCompatibilityScriptProvider {
    fun otherCompatibilityFixesScript(): String = """
        // Compatibility fixes
        (function() {
            'use strict';
            if (!window.requestIdleCallback) {
                window.requestIdleCallback = function(callback, options) {
                    var timeout = (options && options.timeout) || 1;
                    var start = Date.now();
                    return setTimeout(function() {
                        callback({
                            didTimeout: false,
                            timeRemaining: function() {
                                return Math.max(0, 50 - (Date.now() - start));
                            }
                        });
                    }, timeout);
                };
                window.cancelIdleCallback = function(id) {
                    clearTimeout(id);
                };
            }
            if (!window.ResizeObserver) {
                window.ResizeObserver = function(callback) {
                    this.callback = callback;
                    this.elements = [];
                };
                window.ResizeObserver.prototype.observe = function(el) {
                    this.elements.push(el);
                };
                window.ResizeObserver.prototype.unobserve = function(el) {
                    this.elements = this.elements.filter(function(e) { return e !== el; });
                };
                window.ResizeObserver.prototype.disconnect = function() {
                    this.elements = [];
                };
            }
            console.log('[WebToApp] Compatibility fixes loaded');
        })();
    """.trimIndent()
}
