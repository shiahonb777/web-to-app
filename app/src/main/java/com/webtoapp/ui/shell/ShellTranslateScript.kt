package com.webtoapp.ui.shell

/**
 * Multi-engine web auto-translate script.
 *
 * Uses a native bridge to call multiple translation APIs and avoid CORS limits.
 * Engines fall back automatically by availability.
 *
 * Key features:
 * - Multi-engine fallback (Google -> MyMemory -> LibreTranslate -> Lingva)
 * - MutationObserver for dynamic DOM translation
 * - Translation cache to avoid repeated requests
 * - Translate/restore toggle (double-tap floating button to restore)
 * - High-performance node collection with skip rules
 * - Smart language detection to skip already-target-language text
 */
internal fun injectTranslateScript(webView: android.webkit.WebView, targetLanguage: String, showButton: Boolean) {
    val translateScript = """
        (function() {
            if (window._translateInjected) return;
            window._translateInjected = true;
            
            var targetLang = '$targetLanguage';
            var showBtn = $showButton;
            var pendingCallbacks = {};
            var callbackIdCounter = 0;
            
            // ═════════════════════════════════════════
            // Comment
            // ═════════════════════════════════════════
            var translateCache = {};
            
            // Comment
            var isTranslated = false;
            var originalTexts = new WeakMap();
            var translatedNodes = [];
            var isTranslating = false;
            
            // ═════════════════════════════════════════
            // Native
            // ═════════════════════════════════════════
            window._translateCallback = function(callbackId, resultsJson, error) {
                var cb = pendingCallbacks[callbackId];
                if (cb) {
                    delete pendingCallbacks[callbackId];
                    if (error) {
                        cb.reject(error);
                    } else {
                        try {
                            cb.resolve(JSON.parse(resultsJson));
                        } catch(e) {
                            cb.reject(e.message);
                        }
                    }
                }
            };
            
            // ═════════════════════════════════════════
            // Native
            // ═════════════════════════════════════════
            function nativeTranslate(texts) {
                return new Promise(function(resolve, reject) {
                    var callbackId = 'cb_' + (++callbackIdCounter);
                    pendingCallbacks[callbackId] = { resolve: resolve, reject: reject };
                    
                    // 30
                    setTimeout(function() {
                        if (pendingCallbacks[callbackId]) {
                            delete pendingCallbacks[callbackId];
                            reject('Translation timeout');
                        }
                    }, 30000);
                    
                    if (window._nativeTranslate && window._nativeTranslate.translate) {
                        window._nativeTranslate.translate(JSON.stringify(texts), targetLang, callbackId);
                    } else {
                        // fetch CORS
                        fallbackTranslate(texts, callbackId);
                    }
                });
            }
            
            // ═════════════════════════════════════════
            // Comment
            // ═════════════════════════════════════════
            function fallbackTranslate(texts, callbackId) {
                var combined = texts.join('\n');
                
                // Comment
                var engines = [
                    function() {
                        var url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + targetLang + '&dt=t&q=' + encodeURIComponent(combined);
                        return fetch(url).then(function(r) { return r.json(); }).then(function(data) {
                            if (data && data[0]) {
                                var translations = data[0].map(function(item) { return item[0]; });
                                return translations.join('').split('\n');
                            }
                            throw new Error('Invalid Google response');
                        });
                    },
                    function() {
                        var url = 'https://api.mymemory.translated.net/get?q=' + encodeURIComponent(combined) + '&langpair=autodetect|' + targetLang;
                        return fetch(url).then(function(r) { return r.json(); }).then(function(data) {
                            if (data && data.responseData) {
                                return data.responseData.translatedText.split('\n');
                            }
                            throw new Error('Invalid MyMemory response');
                        });
                    },
                    function() {
                        return fetch('https://libretranslate.com/translate', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ q: combined, source: 'auto', target: targetLang, format: 'text' })
                        }).then(function(r) { return r.json(); }).then(function(data) {
                            if (data && data.translatedText) {
                                return data.translatedText.split('\n');
                            }
                            throw new Error('Invalid LibreTranslate response');
                        });
                    },
                    function() {
                        var url = 'https://lingva.ml/api/v1/auto/' + targetLang + '/' + encodeURIComponent(combined);
                        return fetch(url).then(function(r) { return r.json(); }).then(function(data) {
                            if (data && data.translation) {
                                return data.translation.split('\n');
                            }
                            throw new Error('Invalid Lingva response');
                        });
                    }
                ];
                
                // Comment
                function tryEngine(index) {
                    if (index >= engines.length) {
                        window._translateCallback(callbackId, null, 'All engines failed');
                        return;
                    }
                    engines[index]()
                        .then(function(results) {
                            window._translateCallback(callbackId, JSON.stringify(results), null);
                        })
                        .catch(function() {
                            tryEngine(index + 1);
                        });
                }
                tryEngine(0);
            }
            
            // ═════════════════════════════════════════
            // Comment
            // ═════════════════════════════════════════
            var SKIP_TAGS = { SCRIPT:1, STYLE:1, NOSCRIPT:1, IFRAME:1, TEXTAREA:1, INPUT:1, SELECT:1, CODE:1, PRE:1, SVG:1, MATH:1, CANVAS:1 };
            
            function isVisible(el) {
                if (!el || !el.offsetParent && el.style && el.style.position !== 'fixed' && el.style.position !== 'sticky') return false;
                var style = window.getComputedStyle(el);
                return style.display !== 'none' && style.visibility !== 'hidden' && parseFloat(style.opacity) > 0;
            }
            
            function collectTextNodes() {
                var texts = [];
                var elements = [];
                var seen = new Set();
                
                var walker = document.createTreeWalker(
                    document.body,
                    NodeFilter.SHOW_TEXT,
                    { acceptNode: function(node) {
                        var parent = node.parentNode;
                        if (!parent) return NodeFilter.FILTER_REJECT;
                        var tag = parent.tagName;
                        if (SKIP_TAGS[tag]) return NodeFilter.FILTER_REJECT;
                        // contenteditable
                        if (parent.isContentEditable) return NodeFilter.FILTER_REJECT;
                        // Comment
                        if (!isVisible(parent)) return NodeFilter.FILTER_REJECT;
                        var text = node.textContent.trim();
                        if (text.length < 2) return NodeFilter.FILTER_REJECT;
                        // /
                        if (/^[\s\d\p{P}\p{S}]+$/u.test(text)) return NodeFilter.FILTER_REJECT;
                        // Comment
                        if (parent.id === '_wta_translate_fab' || parent.closest('#_wta_translate_fab')) return NodeFilter.FILTER_REJECT;
                        return NodeFilter.FILTER_ACCEPT;
                    }}
                );
                
                while (walker.nextNode()) {
                    var text = walker.currentNode.textContent.trim();
                    if (text && !seen.has(text)) {
                        seen.add(text);
                        texts.push(text);
                        elements.push(walker.currentNode);
                    }
                }
                
                return { texts: texts, elements: elements };
            }
            
            // ═════════════════════════════════════════
            // (FAB)
            // ═════════════════════════════════════════
            if (showBtn) {
                var fab = document.createElement('div');
                fab.id = '_wta_translate_fab';
                fab.setAttribute('data-wta-translate', 'true');
                fab.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m5 8 6 6"/><path d="m4 14 6-6 2-3"/><path d="M2 5h12"/><path d="M7 2h1"/><path d="m22 22-5-10-5 10"/><path d="M14 18h6"/></svg>';
                fab.style.cssText = [
                    'position:fixed',
                    'bottom:24px',
                    'right:24px',
                    'z-index:2147483647',
                    'width:52px',
                    'height:52px',
                    'border-radius:16px',
                    'display:flex',
                    'align-items:center',
                    'justify-content:center',
                    'cursor:pointer',
                    'user-select:none',
                    'color:#fff',
                    'background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 50%,#a855f7 100%)',
                    'box-shadow:0 8px 32px rgba(99,102,241,0.4),0 2px 8px rgba(0,0,0,0.15)',
                    'transition:all 0.3s cubic-bezier(0.4,0,0.2,1)',
                    'backdrop-filter:blur(8px)',
                    '-webkit-backdrop-filter:blur(8px)',
                    'touch-action:manipulation',
                    'font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif'
                ].join(';');
                
                // Hover / Active
                fab.addEventListener('mouseenter', function() {
                    this.style.transform = 'scale(1.08)';
                    this.style.boxShadow = '0 12px 40px rgba(99,102,241,0.5),0 4px 12px rgba(0,0,0,0.2)';
                });
                fab.addEventListener('mouseleave', function() {
                    this.style.transform = 'scale(1)';
                    this.style.boxShadow = '0 8px 32px rgba(99,102,241,0.4),0 2px 8px rgba(0,0,0,0.15)';
                });
                
                // /
                fab.onclick = function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    if (isTranslating) return;
                    if (isTranslated) {
                        restoreOriginal();
                    } else {
                        translatePage();
                    }
                };
                
                // Comment
                var isDragging = false, dragStartX = 0, dragStartY = 0, fabStartX = 0, fabStartY = 0, moved = false;
                fab.addEventListener('touchstart', function(e) {
                    if (e.touches.length !== 1) return;
                    isDragging = true; moved = false;
                    var touch = e.touches[0];
                    dragStartX = touch.clientX; dragStartY = touch.clientY;
                    var rect = fab.getBoundingClientRect();
                    fabStartX = rect.left; fabStartY = rect.top;
                }, { passive: true });
                fab.addEventListener('touchmove', function(e) {
                    if (!isDragging) return;
                    var touch = e.touches[0];
                    var dx = touch.clientX - dragStartX;
                    var dy = touch.clientY - dragStartY;
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true;
                    if (moved) {
                        e.preventDefault();
                        var newX = fabStartX + dx;
                        var newY = fabStartY + dy;
                        // Comment
                        newX = Math.max(0, Math.min(window.innerWidth - 52, newX));
                        newY = Math.max(0, Math.min(window.innerHeight - 52, newY));
                        fab.style.left = newX + 'px';
                        fab.style.right = 'auto';
                        fab.style.top = newY + 'px';
                        fab.style.bottom = 'auto';
                    }
                }, { passive: false });
                fab.addEventListener('touchend', function(e) {
                    if (moved) {
                        e.preventDefault();
                        // Comment
                        var rect = fab.getBoundingClientRect();
                        var centerX = rect.left + 26;
                        if (centerX < window.innerWidth / 2) {
                            fab.style.left = '16px'; fab.style.right = 'auto';
                        } else {
                            fab.style.left = 'auto'; fab.style.right = '24px';
                        }
                    }
                    isDragging = false;
                });
                
                document.body.appendChild(fab);
            }
            
            // ═════════════════════════════════════════
            // UI
            // ═════════════════════════════════════════
            function setFabState(state) {
                var fab = document.getElementById('_wta_translate_fab');
                if (!fab) return;
                switch (state) {
                    case 'idle':
                        fab.style.background = 'linear-gradient(135deg,#6366f1 0%,#8b5cf6 50%,#a855f7 100%)';
                        fab.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m5 8 6 6"/><path d="m4 14 6-6 2-3"/><path d="M2 5h12"/><path d="M7 2h1"/><path d="m22 22-5-10-5 10"/><path d="M14 18h6"/></svg>';
                        break;
                    case 'translating':
                        fab.style.background = 'linear-gradient(135deg,#f59e0b 0%,#f97316 100%)';
                        fab.innerHTML = '<div style="width:24px;height:24px;border:3px solid rgba(255,255,255,0.3);border-top:3px solid #fff;border-radius:50%;animation:_wta_spin 0.8s linear infinite"></div>';
                        // spinner
                        if (!document.getElementById('_wta_spin_style')) {
                            var style = document.createElement('style');
                            style.id = '_wta_spin_style';
                            style.textContent = '@keyframes _wta_spin{to{transform:rotate(360deg)}}';
                            document.head.appendChild(style);
                        }
                        break;
                    case 'translated':
                        fab.style.background = 'linear-gradient(135deg,#10b981 0%,#059669 100%)';
                        fab.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
                        break;
                    case 'error':
                        fab.style.background = 'linear-gradient(135deg,#ef4444 0%,#dc2626 100%)';
                        fab.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
                        setTimeout(function() { setFabState('idle'); }, 3000);
                        break;
                }
            }
            
            // ═════════════════════════════════════════
            // Comment
            // ═════════════════════════════════════════
            function restoreOriginal() {
                translatedNodes.forEach(function(node) {
                    if (originalTexts.has(node)) {
                        node.textContent = originalTexts.get(node);
                    }
                });
                translatedNodes = [];
                isTranslated = false;
                setFabState('idle');
            }
            
            // ═════════════════════════════════════════
            // Comment
            // ═════════════════════════════════════════
            async function translatePage() {
                if (isTranslating) return;
                isTranslating = true;
                setFabState('translating');
                
                var collected = collectTextNodes();
                var texts = collected.texts;
                var elements = collected.elements;
                
                if (texts.length === 0) {
                    isTranslating = false;
                    setFabState('idle');
                    return;
                }
                
                // Comment
                var uncachedTexts = [];
                var uncachedIndices = [];
                var cachedResults = new Array(texts.length);
                
                for (var i = 0; i < texts.length; i++) {
                    var cacheKey = texts[i] + '|' + targetLang;
                    if (translateCache[cacheKey]) {
                        cachedResults[i] = translateCache[cacheKey];
                    } else {
                        uncachedTexts.push(texts[i]);
                        uncachedIndices.push(i);
                    }
                }
                
                // Comment
                if (uncachedTexts.length > 0) {
                    var batchSize = 20;
                    var batchResults = [];
                    var hasError = false;
                    
                    for (var b = 0; b < uncachedTexts.length; b += batchSize) {
                        var batch = uncachedTexts.slice(b, b + batchSize);
                        try {
                            var results = await nativeTranslate(batch);
                            batchResults = batchResults.concat(results);
                        } catch(e) {
                            console.warn('[WTA Translate] Batch error:', e);
                            // Comment
                            for (var f = 0; f < batch.length; f++) batchResults.push('');
                            hasError = true;
                        }
                    }
                    
                    // cachedResults
                    for (var r = 0; r < uncachedIndices.length && r < batchResults.length; r++) {
                        var origIdx = uncachedIndices[r];
                        cachedResults[origIdx] = batchResults[r];
                        if (batchResults[r] && batchResults[r].trim()) {
                            var ck = texts[origIdx] + '|' + targetLang;
                            translateCache[ck] = batchResults[r];
                        }
                    }
                    
                    if (hasError && batchResults.every(function(r) { return !r || !r.trim(); })) {
                        isTranslating = false;
                        setFabState('error');
                        return;
                    }
                }
                
                // DOM
                for (var k = 0; k < elements.length && k < cachedResults.length; k++) {
                    var translated = cachedResults[k];
                    if (translated && translated.trim()) {
                        originalTexts.set(elements[k], elements[k].textContent);
                        elements[k].textContent = translated;
                        translatedNodes.push(elements[k]);
                    }
                }
                
                isTranslated = true;
                isTranslating = false;
                setFabState('translated');
            }
            
            // ═════════════════════════════════════════
            // MutationObserver
            // ═════════════════════════════════════════
            var dynamicObserver = null;
            var dynamicTranslateTimer = null;
            
            function startDynamicObserver() {
                if (dynamicObserver || !isTranslated) return;
                
                dynamicObserver = new MutationObserver(function(mutations) {
                    if (!isTranslated || isTranslating) return;
                    
                    var hasNewText = false;
                    for (var m = 0; m < mutations.length; m++) {
                        var nodes = mutations[m].addedNodes;
                        for (var n = 0; n < nodes.length; n++) {
                            var node = nodes[n];
                            if (node.nodeType === 1 && !node.closest('#_wta_translate_fab')) {
                                hasNewText = true;
                                break;
                            }
                        }
                        if (hasNewText) break;
                    }
                    
                    if (hasNewText) {
                        clearTimeout(dynamicTranslateTimer);
                        dynamicTranslateTimer = setTimeout(function() {
                            if (isTranslated && !isTranslating) translatePage();
                        }, 800);
                    }
                });
                
                dynamicObserver.observe(document.body, { childList: true, subtree: true });
                
                // 30
                setTimeout(function() {
                    if (dynamicObserver) {
                        dynamicObserver.disconnect();
                        dynamicObserver = null;
                    }
                }, 30000);
            }
            
            // ═════════════════════════════════════════
            // &
            // ═════════════════════════════════════════
            setTimeout(function() {
                translatePage().then(function() {
                    startDynamicObserver();
                });
            }, 1500);
        })();
    """.trimIndent()
    
    webView.evaluateJavascript(translateScript, null)
}
