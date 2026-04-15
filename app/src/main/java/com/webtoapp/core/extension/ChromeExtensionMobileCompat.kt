package com.webtoapp.core.extension

/**
 * Chrome Extension Mobile Compatibility Layer
 *
 * in Chrome extension .
 *
 * .
 * - extension extension as in Chrome in.
 * - in.
 * - not extension .
 * - use not extension.
 *
 * .
 * 1.
 * 2. CSS.
 * 3.
 * 4. single.
 * 5. Hover manage.
 * 6. Tooltip.
 * 7.
 * 8. /.
 * 9. SPA pushState/replaceState intercept.
 * 10. Viewport.
 * 11. API Shim.
 * 12. / fixscroll lockoverscroll-webkit-overflow-scrolling.
 * 13.
 * 14.
 *
 * when DOCUMENT_START extension.
 */
object ChromeExtensionMobileCompat {

    /**
     * CSS + JS.
     * in DOCUMENT_START when extension.
     */
    fun generateCompatScript(): String = """
(function() {
    'use strict';
    
    // ===== =====.
    if (window.__WTA_MOBILE_COMPAT__) return;
    window.__WTA_MOBILE_COMPAT__ = true;
    
    // not UA use.
    var isTouchDevice = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);
    var isSmallScreen = (screen.width <= 1024) || (screen.height <= 1024);
    var isMobileDevice = isTouchDevice && isSmallScreen;
    
    // not is not.
    if (!isTouchDevice) return;
    
    // use.
    window.__WTA_DEVICE__ = {
        isTouchDevice: isTouchDevice,
        isSmallScreen: isSmallScreen,
        isMobileDevice: isMobileDevice,
        screenWidth: screen.width,
        screenHeight: screen.height,
        dpr: window.devicePixelRatio || 1,
        safeAreaTop: 0,
        safeAreaBottom: 0
    };
    
    (function injectCompatCSS() {
        var css = [
            // --- ---.
            // extension.
            'html, body { overflow-x: hidden !important; max-width: 100vw !important; }',
            
            // extension to.
            '[data-wta-ext] { max-width: 100vw !important; box-sizing: border-box !important; }',
            
            // extension fixed .
            '[data-wta-ext] [style*="position: fixed"], [data-wta-ext] [style*="position:fixed"] {',
            '  max-width: 100vw !important;',
            '  max-height: 100vh !important;',
            '  max-height: 100dvh !important;',
            '  overflow: auto !important;',
            '  -webkit-overflow-scrolling: touch !important;',
            '}',
            
            // --- ---.
            // can small WCAG 2.5.5.
            '[data-wta-ext] button, [data-wta-ext] a, [data-wta-ext] [role="button"],',
            '[data-wta-ext] input[type="checkbox"], [data-wta-ext] input[type="radio"],',
            '[data-wta-ext] select, [data-wta-ext] [tabindex] {',
            '  min-width: 36px; min-height: 36px;',
            '}',
            
            // --- ---.
            // extension can in can.
            '[data-wta-ext] ::-webkit-scrollbar { width: 4px !important; height: 4px !important; }',
            '[data-wta-ext] ::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.3) !important; border-radius: 2px !important; }',
            '[data-wta-ext] ::-webkit-scrollbar-track { background: transparent !important; }',
            
            // --- ---.
            // extension UI.
            '[data-wta-ext] { touch-action: manipulation; }',
            
            // --- Tooltip ---.
            '#__wta_tooltip__ {',
            '  position: fixed; z-index: 2147483645; pointer-events: none;',
            '  background: rgba(30,30,30,0.92); color: #fff; border-radius: 6px;',
            '  padding: 6px 12px; font-size: 13px; line-height: 1.4;',
            '  max-width: 260px; word-wrap: break-word; opacity: 0;',
            '  transition: opacity 0.15s; box-shadow: 0 2px 8px rgba(0,0,0,0.25);',
            '}',
            '#__wta_tooltip__.visible { opacity: 1; }',
            
            // --- Safe Area ---
            // Supports /.
            '[data-wta-ext] [style*="position: fixed"][style*="bottom: 0"],',
            '[data-wta-ext] [style*="position:fixed"][style*="bottom:0"] {',
            '  padding-bottom: env(safe-area-inset-bottom, 0px) !important;',
            '}',
            '[data-wta-ext] [style*="position: fixed"][style*="top: 0"],',
            '[data-wta-ext] [style*="position:fixed"][style*="top:0"] {',
            '  padding-top: env(safe-area-inset-top, 0px) !important;',
            '}',
            
            // --- ---.
            // extension UI in.
            '[data-wta-ext] button, [data-wta-ext] [role="button"] {',
            '  -webkit-user-select: none; user-select: none;',
            '}'
        ].join('\n');
        
        var style = document.createElement('style');
        style.id = '__wta_mobile_compat_css__';
        style.textContent = css;
        
        function ensureStyle() {
            if (!document.getElementById('__wta_mobile_compat_css__')) {
                (document.head || document.documentElement).appendChild(style);
            }
        }
        
        if (document.head) {
            ensureStyle();
        } else {
            var obs = new MutationObserver(function() {
                if (document.head) { ensureStyle(); obs.disconnect(); }
            });
            obs.observe(document.documentElement || document, { childList: true, subtree: true });
        }
    })();
    
    // extension mouse/pointer touch .
    // touchmouse+pointer .
    (function patchPointerEvents() {
        var lastTouchTarget = null;
        var activeTouchId = null;
        
        // .
        function syntheticMouse(type, touch, target, bubbles) {
            try {
                return new MouseEvent(type, {
                    bubbles: bubbles !== false,
                    cancelable: true,
                    view: window,
                    clientX: touch.clientX,
                    clientY: touch.clientY,
                    screenX: touch.screenX,
                    screenY: touch.screenY,
                    button: 0,
                    buttons: type === 'mouseup' ? 0 : 1
                });
            } catch(e) { return null; }
        }
        
        function syntheticPointer(type, touch, target, isPrimary) {
            try {
                return new PointerEvent(type, {
                    bubbles: true,
                    cancelable: true,
                    view: window,
                    clientX: touch.clientX,
                    clientY: touch.clientY,
                    screenX: touch.screenX,
                    screenY: touch.screenY,
                    pointerId: 1,
                    pointerType: 'touch',
                    isPrimary: isPrimary !== false,
                    width: touch.radiusX ? touch.radiusX * 2 : 24,
                    height: touch.radiusY ? touch.radiusY * 2 : 24,
                    pressure: touch.force || 0.5
                });
            } catch(e) { return null; }
        }
        
        function dispatch(target, event) {
            if (target && event) {
                try { target.dispatchEvent(event); } catch(e) { /* dispatch failed */ }
            }
        }
        
        document.addEventListener('touchstart', function(e) {
            if (activeTouchId !== null) return; // Note.
            var touch = e.changedTouches[0];
            if (!touch) return;
            activeTouchId = touch.identifier;
            var target = e.target;
            
            if (lastTouchTarget && lastTouchTarget !== target) {
                dispatch(lastTouchTarget, syntheticPointer('pointerout', touch, lastTouchTarget));
                dispatch(lastTouchTarget, syntheticPointer('pointerleave', touch, lastTouchTarget));
                dispatch(lastTouchTarget, syntheticMouse('mouseout', touch, lastTouchTarget, true));
                dispatch(lastTouchTarget, syntheticMouse('mouseleave', touch, lastTouchTarget, false));
            }
            
            lastTouchTarget = target;
            
            // + by.
            dispatch(target, syntheticPointer('pointerover', touch, target));
            dispatch(target, syntheticPointer('pointerenter', touch, target));
            dispatch(target, syntheticPointer('pointerdown', touch, target));
            dispatch(target, syntheticMouse('mouseover', touch, target, true));
            dispatch(target, syntheticMouse('mouseenter', touch, target, false));
            dispatch(target, syntheticMouse('mousedown', touch, target));
        }, { passive: true, capture: true });
        
        document.addEventListener('touchmove', function(e) {
            var touch = null;
            for (var i = 0; i < e.changedTouches.length; i++) {
                if (e.changedTouches[i].identifier === activeTouchId) {
                    touch = e.changedTouches[i]; break;
                }
            }
            if (!touch) return;
            var target = document.elementFromPoint(touch.clientX, touch.clientY) || lastTouchTarget;
            
            dispatch(target, syntheticPointer('pointermove', touch, target));
            dispatch(target, syntheticMouse('mousemove', touch, target));
            
            // to.
            if (target !== lastTouchTarget) {
                if (lastTouchTarget) {
                    dispatch(lastTouchTarget, syntheticMouse('mouseout', touch, lastTouchTarget, true));
                    dispatch(lastTouchTarget, syntheticMouse('mouseleave', touch, lastTouchTarget, false));
                }
                dispatch(target, syntheticMouse('mouseover', touch, target, true));
                dispatch(target, syntheticMouse('mouseenter', touch, target, false));
                lastTouchTarget = target;
            }
        }, { passive: true, capture: true });
        
        document.addEventListener('touchend', function(e) {
            var touch = null;
            for (var i = 0; i < e.changedTouches.length; i++) {
                if (e.changedTouches[i].identifier === activeTouchId) {
                    touch = e.changedTouches[i]; break;
                }
            }
            if (!touch) return;
            activeTouchId = null;
            var target = lastTouchTarget || e.target;
            
            // Release.
            dispatch(target, syntheticPointer('pointerup', touch, target));
            dispatch(target, syntheticMouse('mouseup', touch, target));
            
            // hover.
            var hoverTarget = target;
            setTimeout(function() {
                dispatch(hoverTarget, syntheticPointer('pointerout', touch, hoverTarget));
                dispatch(hoverTarget, syntheticPointer('pointerleave', touch, hoverTarget));
                dispatch(hoverTarget, syntheticMouse('mouseout', touch, hoverTarget, true));
                dispatch(hoverTarget, syntheticMouse('mouseleave', touch, hoverTarget, false));
                if (lastTouchTarget === hoverTarget) lastTouchTarget = null;
            }, 300);
        }, { passive: true, capture: true });
        
        document.addEventListener('touchcancel', function(e) {
            activeTouchId = null;
            if (lastTouchTarget) {
                try {
                    lastTouchTarget.dispatchEvent(new PointerEvent('pointercancel', { bubbles: true }));
                } catch(ex) { /* dispatch failed */ }
                lastTouchTarget = null;
            }
        }, { passive: true, capture: true });
    })();
    
    // contextmenu by 500ms.
    (function patchContextMenu() {
        var longPressTimer = null;
        var longPressTarget = null;
        var LONG_PRESS_MS = 500;
        var startX = 0, startY = 0;
        var MOVE_THRESHOLD = 10;
        
        document.addEventListener('touchstart', function(e) {
            var touch = e.touches[0];
            if (!touch) return;
            startX = touch.clientX;
            startY = touch.clientY;
            longPressTarget = e.target;
            
            longPressTimer = setTimeout(function() {
                if (longPressTarget) {
                    try {
                        var evt = new MouseEvent('contextmenu', {
                            bubbles: true, cancelable: true,
                            clientX: startX, clientY: startY,
                            screenX: touch.screenX, screenY: touch.screenY,
                            button: 2, buttons: 2
                        });
                        longPressTarget.dispatchEvent(evt);
                    } catch(ex) { /* contextmenu dispatch failed */ }
                }
                longPressTimer = null;
            }, LONG_PRESS_MS);
        }, { passive: true, capture: false });
        
        document.addEventListener('touchmove', function(e) {
            if (longPressTimer) {
                var touch = e.touches[0];
                if (touch && (Math.abs(touch.clientX - startX) > MOVE_THRESHOLD ||
                              Math.abs(touch.clientY - startY) > MOVE_THRESHOLD)) {
                    clearTimeout(longPressTimer);
                    longPressTimer = null;
                }
            }
        }, { passive: true, capture: false });
        
        document.addEventListener('touchend', function() {
            if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null; }
        }, { passive: true, capture: false });
        
        document.addEventListener('touchcancel', function() {
            if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null; }
        }, { passive: true, capture: false });
    })();
    
    // hover title by tooltip.
    (function patchTooltips() {
        var tooltipEl = null;
        var tooltipTimer = null;
        
        function getTooltip() {
            if (!tooltipEl) {
                tooltipEl = document.createElement('div');
                tooltipEl.id = '__wta_tooltip__';
            }
            return tooltipEl;
        }
        
        function showTooltip(text, x, y) {
            var tip = getTooltip();
            tip.textContent = text;
            if (!tip.parentNode) document.body.appendChild(tip);
            // in.
            var tipWidth = 260;
            var left = Math.max(8, Math.min(x - tipWidth / 2, window.innerWidth - tipWidth - 8));
            var top = Math.max(8, y - 50);
            tip.style.left = left + 'px';
            tip.style.top = top + 'px';
            tip.classList.add('visible');
        }
        
        function hideTooltip() {
            if (tooltipEl) {
                tooltipEl.classList.remove('visible');
            }
        }
        
        document.addEventListener('touchstart', function(e) {
            hideTooltip();
            var target = e.target;
            var touch = e.touches[0];
            if (!touch) return;
            
            // title.
            var el = target;
            var title = '';
            while (el && el !== document.body) {
                title = el.getAttribute('title') || el.getAttribute('data-tooltip') || '';
                if (title) break;
                el = el.parentElement;
            }
            if (!title) return;
            
            var tx = touch.clientX, ty = touch.clientY;
            tooltipTimer = setTimeout(function() {
                showTooltip(title, tx, ty);
            }, 600);
        }, { passive: true, capture: true });
        
        document.addEventListener('touchmove', function() {
            if (tooltipTimer) { clearTimeout(tooltipTimer); tooltipTimer = null; }
        }, { passive: true, capture: true });
        
        document.addEventListener('touchend', function() {
            if (tooltipTimer) { clearTimeout(tooltipTimer); tooltipTimer = null; }
            setTimeout(hideTooltip, 1500);
        }, { passive: true, capture: true });
    })();
    
    // .
    (function patchDragDrop() {
        var dragSource = null;
        var dragData = {};
        var isDragging = false;
        var DRAG_THRESHOLD = 10;
        var startX = 0, startY = 0;
        
        document.addEventListener('touchstart', function(e) {
            var target = e.target;
            // draggable use.
            var draggable = target.closest('[draggable="true"]');
            if (!draggable) return;
            var touch = e.touches[0];
            if (!touch) return;
            startX = touch.clientX;
            startY = touch.clientY;
            dragSource = draggable;
            isDragging = false;
            dragData = {};
        }, { passive: true, capture: false });
        
        document.addEventListener('touchmove', function(e) {
            if (!dragSource) return;
            var touch = e.touches[0];
            if (!touch) return;
            var dx = touch.clientX - startX, dy = touch.clientY - startY;
            
            if (!isDragging && (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD)) {
                isDragging = true;
                try {
                    var dtStub = {
                        setData: function(t,v) { dragData[t] = v; },
                        getData: function(t) { return dragData[t] || ''; },
                        types: [], effectAllowed: 'all', dropEffect: 'move',
                        files: [], items: []
                    };
                    var dragStartEvt = new DragEvent('dragstart', {
                        bubbles: true, cancelable: true, dataTransfer: dtStub
                    });
                    // DragEvent constructor may not accept dataTransfer in all browsers
                    // Fallback: use CustomEvent
                    dragSource.dispatchEvent(dragStartEvt);
                } catch(ex) {
                    var ce = new CustomEvent('dragstart', { bubbles: true, detail: { source: dragSource } });
                    dragSource.dispatchEvent(ce);
                }
            }
            
            if (isDragging) {
                var dropTarget = document.elementFromPoint(touch.clientX, touch.clientY);
                if (dropTarget) {
                    try {
                        dropTarget.dispatchEvent(new DragEvent('dragover', {
                            bubbles: true, cancelable: true,
                            clientX: touch.clientX, clientY: touch.clientY
                        }));
                    } catch(ex) { /* drag event dispatch failed */ }
                }
            }
        }, { passive: false, capture: false });
        
        document.addEventListener('touchend', function(e) {
            if (isDragging && dragSource) {
                var touch = e.changedTouches[0];
                var dropTarget = touch ? document.elementFromPoint(touch.clientX, touch.clientY) : null;
                if (dropTarget) {
                    try {
                        dropTarget.dispatchEvent(new DragEvent('drop', {
                            bubbles: true, cancelable: true,
                            clientX: touch.clientX, clientY: touch.clientY
                        }));
                    } catch(ex) { /* drop dispatch failed */ }
                }
                try {
                    dragSource.dispatchEvent(new DragEvent('dragend', { bubbles: true }));
                } catch(ex) { /* dragend dispatch failed */ }
            }
            dragSource = null;
            isDragging = false;
            dragData = {};
        }, { passive: true, capture: false });
    })();
    
    // window resize.
    (function patchResizeEvents() {
        var lastWidth = window.innerWidth;
        var lastHeight = window.innerHeight;
        
        // observe orientationchange and resize.
        window.addEventListener('orientationchange', function() {
            setTimeout(function() {
                if (window.innerWidth !== lastWidth || window.innerHeight !== lastHeight) {
                    lastWidth = window.innerWidth;
                    lastHeight = window.innerHeight;
                    try {
                        window.dispatchEvent(new Event('resize'));
                    } catch(e) { /* resize dispatch failed */ }
                }
            }, 200); // Note.
        });
        
        // resize.
        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', function() {
                try {
                    window.dispatchEvent(new CustomEvent('wta:viewportresize', {
                        detail: {
                            width: window.visualViewport.width,
                            height: window.visualViewport.height,
                            offsetTop: window.visualViewport.offsetTop,
                            offsetLeft: window.visualViewport.offsetLeft
                        }
                    }));
                } catch(e) { /* viewport resize dispatch failed */ }
            });
        }
    })();
    
    (function patchSPANavigation() {
        var _pushState = history.pushState;
        var _replaceState = history.replaceState;
        
        function dispatchUrlChange(method, url) {
            try {
                window.dispatchEvent(new CustomEvent('wta:urlchange', {
                    detail: { method: method, url: url || location.href, oldUrl: location.href, timestamp: Date.now() }
                }));
                setTimeout(function() {
                    window.dispatchEvent(new PopStateEvent('popstate', { state: history.state }));
                }, 0);
            } catch(e) { /* URL change dispatch failed */ }
        }
        
        history.pushState = function(state, title, url) {
            var result = _pushState.apply(this, arguments);
            dispatchUrlChange('pushState', url ? new URL(url, location.href).href : location.href);
            return result;
        };
        
        history.replaceState = function(state, title, url) {
            var result = _replaceState.apply(this, arguments);
            dispatchUrlChange('replaceState', url ? new URL(url, location.href).href : location.href);
            return result;
        };
    })();
    
    (function protectViewport() {
        var locked = false;
        var obs = new MutationObserver(function(mutations) {
            for (var i = 0; i < mutations.length; i++) {
                var added = mutations[i].addedNodes;
                for (var j = 0; j < added.length; j++) {
                    var node = added[j];
                    if (node.tagName === 'META' && node.name === 'viewport' && locked) {
                        if (node.getAttribute('data-wta-ext')) {
                            node.parentNode && node.parentNode.removeChild(node);
                        }
                    }
                }
            }
        });
        
        function lock() {
            if (document.querySelector('meta[name="viewport"]')) {
                locked = true;
                obs.observe(document.head || document.documentElement, { childList: true, subtree: true });
            }
        }
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', lock);
        } else { lock(); }
    })();
    
    (function shimDesktopAPIs() {
        // --- matchMedia: as ---.
        // extension use hover/pointer is hover UI.
        // as hover UI can use .
        var _matchMedia = window.matchMedia;
        if (_matchMedia) {
            window.matchMedia = function(query) {
                var result = _matchMedia.call(window, query);
                
                // hover: hover true.
                if (/\(hover:\s*hover\)/.test(query) && !result.matches) {
                    return Object.create(result, {
                        matches: { get: function() { return true; } }
                    });
                }
                // hover: none false
                if (/\(hover:\s*none\)/.test(query) && result.matches) {
                    return Object.create(result, {
                        matches: { get: function() { return false; } }
                    });
                }
                // pointer: fine true
                if (/\(pointer:\s*fine\)/.test(query) && !result.matches) {
                    return Object.create(result, {
                        matches: { get: function() { return true; } }
                    });
                }
                // pointer: coarse false
                if (/\(pointer:\s*coarse\)/.test(query) && result.matches) {
                    return Object.create(result, {
                        matches: { get: function() { return false; } }
                    });
                }
                return result;
            };
        }
        
        // --- requestIdleCallback polyfill ---
        if (typeof window.requestIdleCallback !== 'function') {
            window.requestIdleCallback = function(cb, opts) {
                var start = Date.now();
                return setTimeout(function() {
                    cb({
                        didTimeout: false,
                        timeRemaining: function() { return Math.max(0, 50 - (Date.now() - start)); }
                    });
                }, opts && opts.timeout ? Math.min(opts.timeout, 100) : 1);
            };
            window.cancelIdleCallback = function(id) { clearTimeout(id); };
        }
        
        // --- navigator.userAgentData shim ---
        // extension use navigator.userAgentData.
        if (!navigator.userAgentData) {
            try {
                Object.defineProperty(navigator, 'userAgentData', {
                    value: {
                        brands: [
                            { brand: 'Chromium', version: '120' },
                            { brand: 'Google Chrome', version: '120' },
                            { brand: 'Not_A Brand', version: '8' }
                        ],
                        mobile: false, // Note.
                        platform: 'Windows',
                        getHighEntropyValues: function(hints) {
                            return Promise.resolve({
                                brands: this.brands,
                                mobile: false,
                                platform: 'Windows',
                                platformVersion: '10.0.0',
                                architecture: 'x86',
                                model: '',
                                uaFullVersion: '120.0.0.0',
                                fullVersionList: this.brands
                            });
                        }
                    },
                    configurable: true
                });
            } catch(e) { /* userAgentData override failed */ }
        }
        
        // --- ---.
        if (!window.__WTA_VIEWPORT_INFO__) {
            Object.defineProperty(window, '__WTA_VIEWPORT_INFO__', {
                value: {
                    isMobile: true, isTouch: true,
                    screenWidth: screen.width, screenHeight: screen.height,
                    devicePixelRatio: window.devicePixelRatio || 1
                },
                writable: false, configurable: false
            });
        }
    })();
    
    (function fixScrolling() {
        var bodyObserver = null;
        
        function watchBody() {
            if (!document.body || bodyObserver) return;
            var lastOverflow = '';
            bodyObserver = new MutationObserver(function() {
                var cur = document.body.style.overflow || '';
                if (cur === 'hidden' && lastOverflow !== 'hidden') {
                    document.body.setAttribute('data-wta-original-overflow', lastOverflow);
                }
                lastOverflow = cur;
            });
            bodyObserver.observe(document.body, { attributes: true, attributeFilter: ['style'] });
        }
        
        if (document.body) { watchBody(); }
        else { document.addEventListener('DOMContentLoaded', watchBody); }
        
        window.__WTA_RESTORE_SCROLL__ = function() {
            if (document.body) {
                var orig = document.body.getAttribute('data-wta-original-overflow') || '';
                document.body.style.overflow = orig;
                document.body.removeAttribute('data-wta-original-overflow');
            }
        };
        
        // extension wheel to body.
        document.addEventListener('wheel', function(e) {
            var el = e.target;
            while (el && el !== document.body) {
                if (el.getAttribute && el.getAttribute('data-wta-ext')) {
                    var s = window.getComputedStyle(el);
                    if (s.overflow === 'auto' || s.overflow === 'scroll' ||
                        s.overflowY === 'auto' || s.overflowY === 'scroll') {
                        // in extension Check is to.
                        var atTop = el.scrollTop <= 0 && e.deltaY < 0;
                        var atBottom = (el.scrollTop + el.clientHeight >= el.scrollHeight - 1) && e.deltaY > 0;
                        if (atTop || atBottom) {
                            e.preventDefault();
                        }
                        return;
                    }
                }
                el = el.parentElement;
            }
        }, { passive: false });
    })();
    
    // when viewport fixed can.
    (function patchVirtualKeyboard() {
        if (!window.visualViewport) return;
        
        var initialHeight = window.visualViewport.height;
        var keyboardVisible = false;
        
        window.visualViewport.addEventListener('resize', function() {
            var currentHeight = window.visualViewport.height;
            var heightDiff = initialHeight - currentHeight;
            var wasVisible = keyboardVisible;
            keyboardVisible = heightDiff > 100; // px.
            
            if (keyboardVisible !== wasVisible) {
                try {
                    window.dispatchEvent(new CustomEvent('wta:keyboard', {
                        detail: {
                            visible: keyboardVisible,
                            height: keyboardVisible ? heightDiff : 0,
                            viewportHeight: currentHeight
                        }
                    }));
                } catch(e) { /* keyboard event dispatch failed */ }
                
                // extension fixed .
                document.documentElement.style.setProperty(
                    '--wta-keyboard-height',
                    keyboardVisible ? heightDiff + 'px' : '0px'
                );
            }
        });
    })();
    
    // extension use dblclick .
    (function patchDblClick() {
        var lastTapTime = 0;
        var lastTapTarget = null;
        var DOUBLE_TAP_MS = 300;
        
        document.addEventListener('touchend', function(e) {
            var now = Date.now();
            var target = e.target;
            if (now - lastTapTime < DOUBLE_TAP_MS && lastTapTarget === target) {
                var touch = e.changedTouches[0];
                if (touch) {
                    try {
                        target.dispatchEvent(new MouseEvent('dblclick', {
                            bubbles: true, cancelable: true,
                            clientX: touch.clientX, clientY: touch.clientY
                        }));
                    } catch(ex) { /* dblclick dispatch failed */ }
                }
                lastTapTime = 0;
                lastTapTarget = null;
            } else {
                lastTapTime = now;
                lastTapTarget = target;
            }
        }, { passive: true, capture: true });
    })();
    
    // extension can observe wheel .
    // as wheel.
    (function patchWheelEvents() {
        var lastScale = 1;
        
        document.addEventListener('touchstart', function(e) {
            if (e.touches.length === 2) {
                var dx = e.touches[0].clientX - e.touches[1].clientX;
                var dy = e.touches[0].clientY - e.touches[1].clientY;
                lastScale = Math.sqrt(dx * dx + dy * dy);
            }
        }, { passive: true });
        
        document.addEventListener('touchmove', function(e) {
            if (e.touches.length === 2) {
                var dx = e.touches[0].clientX - e.touches[1].clientX;
                var dy = e.touches[0].clientY - e.touches[1].clientY;
                var scale = Math.sqrt(dx * dx + dy * dy);
                var delta = lastScale - scale;
                lastScale = scale;
                
                var cx = (e.touches[0].clientX + e.touches[1].clientX) / 2;
                var cy = (e.touches[0].clientY + e.touches[1].clientY) / 2;
                var target = document.elementFromPoint(cx, cy) || document.body;
                
                try {
                    target.dispatchEvent(new WheelEvent('wheel', {
                        bubbles: true, cancelable: true,
                        clientX: cx, clientY: cy,
                        deltaY: delta, deltaMode: 0,
                        ctrlKey: true // ctrl+wheel = zoom on desktop
                    }));
                } catch(ex) { /* wheel dispatch failed */ }
            }
        }, { passive: true });
    })();
    
})();
""".trimIndent()

    /**
     * extension Popup.
     *
     * extension popup.html when in in onClick .
     * when JS bridge popup WebView.
     *
     * @param extensionId extension ID.
     * @param popupPath popup.html.
     * @return in use onClick.
     */
    fun getPopupOnClickScript(extensionId: String, popupPath: String): String {
        val safeExtId = extensionId.replace("'", "\\'")
        val safePath = popupPath.replace("'", "\\'")
        return """
            function() {
                if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.openPopup === 'function') {
                    WtaExtBridge.openPopup('$safeExtId', '$safePath');
                } else {
                    // Fallback: in iframe in popup.
                    var url = 'chrome-extension://$safeExtId/$safePath';
                    console.log('[WTA] Opening popup: ' + url);
                }
            }
        """.trimIndent()
    }
}