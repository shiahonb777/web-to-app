package com.webtoapp.core.extension

/**
 * Chrome Extension Mobile Compatibility Layer
 * 
 * 在移动设备上运行桌面 Chrome 扩展的全面兼容层。
 * 
 * 设计原则：
 * - 对扩展代码透明：扩展认为自己运行在桌面 Chrome 中
 * - 所有桌面交互在移动端有对应实现（hover→touch, right-click→long-press 等）
 * - 不修改扩展源码，仅通过环境适配实现兼容
 * - 通用逻辑，不针对任何特定扩展
 * 
 * 覆盖的桌面→移动适配：
 *  1. 设备检测（触摸能力检测，不依赖 UA）
 *  2. CSS 桌面模拟层（视口约束、触摸目标、滚动条、safe area）
 *  3. 完整指针事件桥（touch→mouse/pointer 全链路映射）
 *  4. 右键菜单桥（长按→contextmenu）
 *  5. Hover 状态管理（CSS :hover 粘滞修复 + JS hover 事件）
 *  6. Tooltip 桥（title 属性→触摸友好提示）
 *  7. 拖放桥（touch drag→HTML5 DnD 事件）
 *  8. 窗口/方向变化桥（orientation→resize 事件统一）
 *  9. SPA 导航（pushState/replaceState 拦截）
 * 10. Viewport 保护
 * 11. 桌面 API Shim（matchMedia 桌面模拟、requestIdleCallback、PointerEvent 等）
 * 12. 滚动/溢出修复（scroll lock、overscroll、-webkit-overflow-scrolling）
 * 13. 性能优化（passive listeners、双击缩放抑制）
 * 14. 虚拟键盘适配（视口高度变化检测）
 * 
 * 注入时机：DOCUMENT_START，先于所有扩展内容脚本
 */
object ChromeExtensionMobileCompat {

    /**
     * 生成移动端兼容性脚本（CSS + JS）
     * 在 DOCUMENT_START 时注入，先于扩展内容脚本
     */
    fun generateCompatScript(): String = """
(function() {
    'use strict';
    
    // ===== 防止重复注入 =====
    if (window.__WTA_MOBILE_COMPAT__) return;
    window.__WTA_MOBILE_COMPAT__ = true;
    
    // ==================== 0. 设备检测 ====================
    // 不依赖 UA（因为可能已设置桌面 UA），改用触摸能力检测
    var isTouchDevice = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);
    var isSmallScreen = (screen.width <= 1024) || (screen.height <= 1024);
    var isMobileDevice = isTouchDevice && isSmallScreen;
    
    // 如果不是移动设备，不需要兼容层
    if (!isTouchDevice) return;
    
    // 导出设备信息供其他脚本使用
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
    
    // ==================== 1. CSS 桌面模拟层 ====================
    (function injectCompatCSS() {
        var css = [
            // --- 视口约束 ---
            // 防止扩展注入的元素导致水平滚动
            'html, body { overflow-x: hidden !important; max-width: 100vw !important; }',
            
            // 扩展注入的元素：约束到视口
            '[data-wta-ext] { max-width: 100vw !important; box-sizing: border-box !important; }',
            
            // 扩展注入的 fixed 定位弹出层：移动端自适应
            '[data-wta-ext] [style*="position: fixed"], [data-wta-ext] [style*="position:fixed"] {',
            '  max-width: 100vw !important;',
            '  max-height: 100vh !important;',
            '  max-height: 100dvh !important;',
            '  overflow: auto !important;',
            '  -webkit-overflow-scrolling: touch !important;',
            '}',
            
            // --- 触摸目标 ---
            // 确保可交互元素的最小触摸目标（WCAG 2.5.5）
            '[data-wta-ext] button, [data-wta-ext] a, [data-wta-ext] [role="button"],',
            '[data-wta-ext] input[type="checkbox"], [data-wta-ext] input[type="radio"],',
            '[data-wta-ext] select, [data-wta-ext] [tabindex] {',
            '  min-width: 36px; min-height: 36px;',
            '}',
            
            // --- 滚动条 ---
            // 桌面扩展可能自定义滚动条样式，确保在移动端可见
            '[data-wta-ext] ::-webkit-scrollbar { width: 4px !important; height: 4px !important; }',
            '[data-wta-ext] ::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.3) !important; border-radius: 2px !important; }',
            '[data-wta-ext] ::-webkit-scrollbar-track { background: transparent !important; }',
            
            // --- 双击缩放抑制 ---
            // 防止扩展 UI 上的快速操作触发浏览器双击缩放
            '[data-wta-ext] { touch-action: manipulation; }',
            
            // --- Tooltip 容器 ---
            '#__wta_tooltip__ {',
            '  position: fixed; z-index: 2147483645; pointer-events: none;',
            '  background: rgba(30,30,30,0.92); color: #fff; border-radius: 6px;',
            '  padding: 6px 12px; font-size: 13px; line-height: 1.4;',
            '  max-width: 260px; word-wrap: break-word; opacity: 0;',
            '  transition: opacity 0.15s; box-shadow: 0 2px 8px rgba(0,0,0,0.25);',
            '}',
            '#__wta_tooltip__.visible { opacity: 1; }',
            
            // --- Safe Area ---
            // 支持刘海屏/圆角屏
            '[data-wta-ext] [style*="position: fixed"][style*="bottom: 0"],',
            '[data-wta-ext] [style*="position:fixed"][style*="bottom:0"] {',
            '  padding-bottom: env(safe-area-inset-bottom, 0px) !important;',
            '}',
            '[data-wta-ext] [style*="position: fixed"][style*="top: 0"],',
            '[data-wta-ext] [style*="position:fixed"][style*="top:0"] {',
            '  padding-top: env(safe-area-inset-top, 0px) !important;',
            '}',
            
            // --- 文本选择 ---
            // 防止扩展 UI 中误触发文本选择
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
    
    // ==================== 2. 完整指针事件桥 ====================
    // 桌面扩展依赖 mouse/pointer 事件，移动端只有 touch 事件。
    // 此桥完整映射 touch→mouse+pointer 事件链。
    (function patchPointerEvents() {
        var lastTouchTarget = null;
        var activeTouchId = null;
        
        // 创建合成鼠标事件，携带正确的坐标
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
        
        // 创建合成指针事件
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
            if (activeTouchId !== null) return; // 只跟踪第一个触点
            var touch = e.changedTouches[0];
            if (!touch) return;
            activeTouchId = touch.identifier;
            var target = e.target;
            
            // 离开上一个元素
            if (lastTouchTarget && lastTouchTarget !== target) {
                dispatch(lastTouchTarget, syntheticPointer('pointerout', touch, lastTouchTarget));
                dispatch(lastTouchTarget, syntheticPointer('pointerleave', touch, lastTouchTarget));
                dispatch(lastTouchTarget, syntheticMouse('mouseout', touch, lastTouchTarget, true));
                dispatch(lastTouchTarget, syntheticMouse('mouseleave', touch, lastTouchTarget, false));
            }
            
            lastTouchTarget = target;
            
            // 进入+按下
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
            
            // 如果移动到了新元素
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
            
            // 释放
            dispatch(target, syntheticPointer('pointerup', touch, target));
            dispatch(target, syntheticMouse('mouseup', touch, target));
            
            // 延迟清除 hover 状态
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
    
    // ==================== 3. 右键菜单桥 ====================
    // 桌面：右键触发 contextmenu → 移动端：长按 500ms 触发
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
    
    // ==================== 4. Tooltip 桥 ====================
    // 桌面：hover 显示 title 属性 → 移动端：长按显示 tooltip
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
            // 定位：显示在触摸点上方
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
            
            // 查找带 title 的最近祖先
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
    
    // ==================== 5. 拖放桥 ====================
    // 桌面拖放 → 移动端触摸拖动
    (function patchDragDrop() {
        var dragSource = null;
        var dragData = {};
        var isDragging = false;
        var DRAG_THRESHOLD = 10;
        var startX = 0, startY = 0;
        
        document.addEventListener('touchstart', function(e) {
            var target = e.target;
            // 只对 draggable 元素启用
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
                // 开始拖动
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
    
    // ==================== 6. 窗口/方向变化桥 ====================
    // 移动端方向变化 → 桌面的 window resize 事件
    (function patchResizeEvents() {
        var lastWidth = window.innerWidth;
        var lastHeight = window.innerHeight;
        
        // 监听 orientationchange 并派发 resize
        window.addEventListener('orientationchange', function() {
            setTimeout(function() {
                if (window.innerWidth !== lastWidth || window.innerHeight !== lastHeight) {
                    lastWidth = window.innerWidth;
                    lastHeight = window.innerHeight;
                    try {
                        window.dispatchEvent(new Event('resize'));
                    } catch(e) { /* resize dispatch failed */ }
                }
            }, 200); // 延迟等待方向变化完成
        });
        
        // 视觉视口 resize 也通知（处理虚拟键盘弹出）
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
    
    // ==================== 7. SPA 导航事件 ====================
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
    
    // ==================== 8. Viewport 保护 ====================
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
    
    // ==================== 9. 桌面 API Shim ====================
    (function shimDesktopAPIs() {
        // --- matchMedia: 假装为桌面设备 ---
        // 扩展用 hover/pointer 媒体查询检测桌面端，决定是否显示 hover UI。
        // 我们假装为桌面，配合触摸事件桥让 hover UI 实际可用。
        var _matchMedia = window.matchMedia;
        if (_matchMedia) {
            window.matchMedia = function(query) {
                var result = _matchMedia.call(window, query);
                
                // hover: hover → true（我们的触摸桥让 hover 可用）
                if (/\(hover:\s*hover\)/.test(query) && !result.matches) {
                    return Object.create(result, {
                        matches: { get: function() { return true; } }
                    });
                }
                // hover: none → false
                if (/\(hover:\s*none\)/.test(query) && result.matches) {
                    return Object.create(result, {
                        matches: { get: function() { return false; } }
                    });
                }
                // pointer: fine → true
                if (/\(pointer:\s*fine\)/.test(query) && !result.matches) {
                    return Object.create(result, {
                        matches: { get: function() { return true; } }
                    });
                }
                // pointer: coarse → false
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
        // 部分扩展使用 navigator.userAgentData 检测平台
        if (!navigator.userAgentData) {
            try {
                Object.defineProperty(navigator, 'userAgentData', {
                    value: {
                        brands: [
                            { brand: 'Chromium', version: '120' },
                            { brand: 'Google Chrome', version: '120' },
                            { brand: 'Not_A Brand', version: '8' }
                        ],
                        mobile: false, // 假装桌面
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
        
        // --- 设备信息辅助 ---
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
    
    // ==================== 10. 滚动/溢出修复 ====================
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
        
        // 防止扩展弹出层的 wheel 事件泄漏到 body
        document.addEventListener('wheel', function(e) {
            var el = e.target;
            while (el && el !== document.body) {
                if (el.getAttribute && el.getAttribute('data-wta-ext')) {
                    var s = window.getComputedStyle(el);
                    if (s.overflow === 'auto' || s.overflow === 'scroll' ||
                        s.overflowY === 'auto' || s.overflowY === 'scroll') {
                        // 在扩展弹出层内，检查是否到达滚动边界
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
    
    // ==================== 11. 虚拟键盘适配 ====================
    // 虚拟键盘弹出时 viewport 高度变化，fixed 定位元素可能偏移
    (function patchVirtualKeyboard() {
        if (!window.visualViewport) return;
        
        var initialHeight = window.visualViewport.height;
        var keyboardVisible = false;
        
        window.visualViewport.addEventListener('resize', function() {
            var currentHeight = window.visualViewport.height;
            var heightDiff = initialHeight - currentHeight;
            var wasVisible = keyboardVisible;
            keyboardVisible = heightDiff > 100; // 键盘通常 > 100px
            
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
                
                // 调整扩展的 fixed 底部元素，避免被键盘遮挡
                document.documentElement.style.setProperty(
                    '--wta-keyboard-height',
                    keyboardVisible ? heightDiff + 'px' : '0px'
                );
            }
        });
    })();
    
    // ==================== 12. 双击事件桥 ====================
    // 某些扩展使用 dblclick 事件，移动端需要模拟
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
    
    // ==================== 13. 滚轮事件模拟 ====================
    // 桌面扩展可能监听 wheel 事件（如自定义滚动），移动端没有滚轮
    // 将两指捏合缩放手势映射为 wheel 事件（ctrl+wheel = zoom 是桌面惯例）
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
     * 生成扩展 Popup 集成脚本
     * 
     * 当扩展有 popup.html 时，在面板注册中添加 onClick 回调，
     * 点击时通过 JS bridge 通知原生端打开 popup WebView。
     * 
     * @param extensionId 扩展 ID
     * @param popupPath popup.html 的相对路径
     * @return 面板注册中使用的 onClick 脚本片段
     */
    fun getPopupOnClickScript(extensionId: String, popupPath: String): String {
        val safeExtId = extensionId.replace("'", "\\'")
        val safePath = popupPath.replace("'", "\\'")
        return """
            function() {
                if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.openPopup === 'function') {
                    WtaExtBridge.openPopup('$safeExtId', '$safePath');
                } else {
                    // Fallback: 在面板 iframe 中加载 popup
                    var url = 'chrome-extension://$safeExtId/$safePath';
                    console.log('[WTA] Opening popup: ' + url);
                }
            }
        """.trimIndent()
    }
}
