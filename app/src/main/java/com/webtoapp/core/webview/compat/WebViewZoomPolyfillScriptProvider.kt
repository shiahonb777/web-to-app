package com.webtoapp.core.webview

import com.webtoapp.data.model.WebViewConfig

internal class WebViewZoomPolyfillScriptProvider {
    fun zoomPolyfillScript(config: WebViewConfig, conservativeMode: Boolean): String? {
        if (!config.enableZoomPolyfill || conservativeMode) return null
        return """
            // CSS zoom polyfill for Android WebView
            (function() {
                'use strict';
                if (window.__webtoapp_zoom_polyfill__) return;
                window.__webtoapp_zoom_polyfill__ = true;
                var originalWidths = new WeakMap();
                function convertZoomToTransform(el) {
                    if (!el || !el.style) return;
                    var zoom = el.style.zoom;
                    if (zoom && zoom !== '1' && zoom !== 'normal' && zoom !== 'initial' && zoom !== '') {
                        var scale = parseFloat(zoom);
                        if (zoom.indexOf('%') !== -1) {
                            scale = parseFloat(zoom) / 100;
                        }
                        if (!isNaN(scale) && scale > 0 && scale !== 1) {
                            if (!originalWidths.has(el)) {
                                originalWidths.set(el, el.style.width || '');
                            }
                            el.style.zoom = '';
                            el.style.transform = 'scale(' + scale + ')';
                            el.style.transformOrigin = 'top left';
                            if (scale < 1) {
                                el.style.width = (100 / scale) + '%';
                            }
                            console.log('[WebToApp] Converted zoom to transform:', scale, 'for element:', el.tagName);
                        }
                    }
                }
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                            convertZoomToTransform(mutation.target);
                        }
                        if (mutation.addedNodes) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeType === 1) {
                                    convertZoomToTransform(node);
                                    if (node.querySelectorAll) {
                                        node.querySelectorAll('*').forEach(function(child) {
                                            convertZoomToTransform(child);
                                        });
                                    }
                                }
                            });
                        }
                    });
                });
                function setupObserver() {
                    if (document.documentElement) {
                        observer.observe(document.documentElement, {
                            attributes: true,
                            childList: true,
                            subtree: true,
                            attributeFilter: ['style']
                        });
                        if (document.body) {
                            convertZoomToTransform(document.body);
                            document.body.querySelectorAll('*').forEach(function(el) {
                                convertZoomToTransform(el);
                            });
                        }
                        console.log('[WebToApp] CSS zoom observer started');
                    }
                }
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', setupObserver);
                } else {
                    setupObserver();
                }
                try {
                    var zoomDescriptor = Object.getOwnPropertyDescriptor(CSSStyleDeclaration.prototype, 'zoom');
                    Object.defineProperty(CSSStyleDeclaration.prototype, 'zoom', {
                        set: function(value) {
                            console.log('[WebToApp] zoom setter called with:', value);
                            if (value && value !== '1' && value !== 'normal' && value !== 'initial' && value !== '') {
                                var scale = parseFloat(value);
                                if (String(value).indexOf('%') !== -1) {
                                    scale = parseFloat(value) / 100;
                                }
                                if (!isNaN(scale) && scale > 0 && scale !== 1) {
                                    this.transform = 'scale(' + scale + ')';
                                    this.transformOrigin = 'top left';
                                    if (scale < 1) {
                                        this.width = (100 / scale) + '%';
                                    }
                                    console.log('[WebToApp] Intercepted zoom set, converted to transform:', scale);
                                    return;
                                }
                            }
                            if (value === '' || value === '1' || value === 'normal' || value === 'initial') {
                                this.transform = '';
                                this.transformOrigin = '';
                            }
                            if (zoomDescriptor && zoomDescriptor.set) {
                                zoomDescriptor.set.call(this, value);
                            }
                        },
                        get: function() {
                            var transform = this.transform;
                            if (transform && transform.indexOf('scale(') !== -1) {
                                var match = transform.match(/scale\(([\d.]+)\)/);
                                if (match) {
                                    return match[1];
                                }
                            }
                            if (zoomDescriptor && zoomDescriptor.get) {
                                return zoomDescriptor.get.call(this);
                            }
                            return '1';
                        },
                        configurable: true
                    });
                    console.log('[WebToApp] zoom setter override installed');
                } catch(e) {
                    console.warn('[WebToApp] Failed to override zoom setter:', e);
                }
                console.log('[WebToApp] CSS zoom polyfill loaded');
            })();
        """.trimIndent()
    }
}
