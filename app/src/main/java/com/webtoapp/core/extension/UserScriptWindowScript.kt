package com.webtoapp.core.extension

/**
 * manager JS.
 *
 * in WebView in window.__WTA_SCRIPT_WINDOWS__ .
 * multiple manage small restore .
 * use ExtensionPanelScript CSS .
 */
object UserScriptWindowScript {

    /**
     * Get manager JS.
     * in GM polyfill after userscript before.
     */
    fun getWindowManagerScript(): String = """
(function() {
    'use strict';
    if (window.__WTA_SCRIPT_WINDOWS__) return;

    // ==================== CSS ====================
    const SW_STYLES = `
        /* */
        #wta-sw-layer {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            z-index: 2147483600;
            pointer-events: none;
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;
            -webkit-font-smoothing: antialiased;
        }

        /* */
        .wta-sw-window {
            position: fixed;
            background: var(--wta-surface, rgba(255,255,255,0.95));
            border-radius: 14px;
            border: 1px solid var(--wta-outline, rgba(123,104,238,0.2));
            box-shadow: 0 8px 32px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.08);
            overflow: hidden;
            display: flex;
            flex-direction: column;
            pointer-events: auto;
            transition: box-shadow 0.2s ease, opacity 0.25s ease;
            opacity: 0;
            transform: scale(0.92);
            min-width: 200px;
            min-height: 150px;
        }
        .wta-sw-window.visible {
            opacity: 1;
            transform: scale(1);
            transition: box-shadow 0.2s ease, opacity 0.25s ease, transform 0.3s cubic-bezier(0.34,1.56,0.64,1);
        }
        .wta-sw-window.active {
            box-shadow: 0 12px 40px rgba(123,104,238,0.25), 0 4px 12px rgba(0,0,0,0.12);
        }
        .wta-sw-window.minimizing {
            opacity: 0;
            transform: scale(0.5) translateY(100px);
            transition: all 0.3s ease;
        }
        .wta-sw-window.maximized {
            border-radius: 0 !important;
            transition: all 0.3s cubic-bezier(0.34,1.56,0.64,1);
        }

        /* */
        .wta-sw-titlebar {
            display: flex;
            align-items: center;
            padding: 8px 10px;
            cursor: move;
            user-select: none;
            -webkit-user-select: none;
            touch-action: none;
            border-bottom: 1px solid var(--wta-outline, rgba(123,104,238,0.15));
            flex-shrink: 0;
            gap: 8px;
            background: var(--wta-surface-dim, rgba(255,255,255,0.85));
        }
        .wta-sw-window.active .wta-sw-titlebar {
            background: linear-gradient(135deg, rgba(123,104,238,0.12) 0%, rgba(157,141,241,0.08) 100%);
        }
        .wta-sw-icon {
            font-size: 16px;
            flex-shrink: 0;
            width: 22px;
            height: 22px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .wta-sw-title {
            flex: 1;
            font-size: 13px;
            font-weight: 600;
            color: var(--wta-on-surface, #1a1a2e);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .wta-sw-controls {
            display: flex;
            gap: 4px;
            flex-shrink: 0;
        }
        .wta-sw-ctrl-btn {
            width: 24px;
            height: 24px;
            border-radius: 6px;
            border: none;
            background: transparent;
            color: var(--wta-on-surface-variant, #6b7280);
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            font-size: 14px;
            transition: all 0.15s ease;
            padding: 0;
            -webkit-tap-highlight-color: transparent;
        }
        .wta-sw-ctrl-btn:hover {
            background: rgba(123,104,238,0.12);
            color: var(--wta-primary, #7B68EE);
        }
        .wta-sw-ctrl-btn.close:hover {
            background: rgba(239,68,68,0.12);
            color: #ef4444;
        }
        .wta-sw-ctrl-btn:active {
            transform: scale(0.88);
        }

        /* */
        .wta-sw-body {
            flex: 1;
            overflow-y: auto;
            overflow-x: hidden;
            padding: 10px;
            -webkit-overflow-scrolling: touch;
            color: var(--wta-on-surface, #1a1a2e);
            font-size: 14px;
        }

        /* */
        .wta-sw-resize {
            position: absolute;
            bottom: 0;
            right: 0;
            width: 18px;
            height: 18px;
            cursor: se-resize;
            touch-action: none;
            z-index: 1;
        }
        .wta-sw-resize::after {
            content: '';
            position: absolute;
            bottom: 3px;
            right: 3px;
            width: 8px;
            height: 8px;
            border-right: 2px solid var(--wta-primary, #7B68EE);
            border-bottom: 2px solid var(--wta-primary, #7B68EE);
            opacity: 0.4;
            border-radius: 0 0 2px 0;
        }

        /* */
        #wta-sw-taskbar {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            height: 44px;
            background: var(--wta-surface, rgba(255,255,255,0.95));
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border-top: 1px solid var(--wta-outline, rgba(123,104,238,0.2));
            z-index: 2147483601;
            pointer-events: auto;
            display: none;
            align-items: center;
            gap: 6px;
            padding: 4px 12px;
            padding-bottom: calc(4px + env(safe-area-inset-bottom, 0px));
            overflow-x: auto;
            overflow-y: hidden;
            -webkit-overflow-scrolling: touch;
        }
        #wta-sw-taskbar::-webkit-scrollbar { display: none; }
        #wta-sw-taskbar.visible { display: flex; }

        .wta-sw-taskbar-item {
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 4px 10px;
            border-radius: 16px;
            background: var(--wta-surface-dim, rgba(255,255,255,0.85));
            border: 1px solid var(--wta-outline, rgba(123,104,238,0.2));
            cursor: pointer;
            white-space: nowrap;
            flex-shrink: 0;
            transition: all 0.2s ease;
            -webkit-tap-highlight-color: transparent;
            font-size: 12px;
            color: var(--wta-on-surface, #1a1a2e);
        }
        .wta-sw-taskbar-item:hover {
            background: rgba(123,104,238,0.1);
            border-color: var(--wta-primary, #7B68EE);
        }
        .wta-sw-taskbar-item:active {
            transform: scale(0.95);
        }
        .wta-sw-taskbar-item .icon {
            font-size: 14px;
        }

        /* (GM_registerMenuCommand )*/
        .wta-sw-menu-btn {
            display: flex;
            align-items: center;
            gap: 8px;
            width: 100%;
            padding: 10px 14px;
            border: 1px solid var(--wta-outline, rgba(123,104,238,0.2));
            border-radius: 10px;
            background: var(--wta-surface-dim, rgba(255,255,255,0.85));
            color: var(--wta-on-surface, #1a1a2e);
            font-size: 14px;
            cursor: pointer;
            transition: all 0.2s ease;
            text-align: left;
            -webkit-tap-highlight-color: transparent;
        }
        .wta-sw-menu-btn:hover {
            background: rgba(123,104,238,0.08);
            border-color: var(--wta-primary, #7B68EE);
        }
        .wta-sw-menu-btn:active {
            transform: scale(0.98);
        }

        /* */
        @media (prefers-color-scheme: dark) {
            .wta-sw-window {
                box-shadow: 0 8px 32px rgba(0,0,0,0.4), 0 2px 8px rgba(0,0,0,0.3);
            }
            .wta-sw-window.active {
                box-shadow: 0 12px 40px rgba(123,104,238,0.3), 0 4px 12px rgba(0,0,0,0.4);
            }
        }
    `;

    var _LANG = (navigator.language || 'zh').toLowerCase().startsWith('ar') ? 'ar' :
                (navigator.language || 'zh').toLowerCase().startsWith('zh') ? 'zh' : 'en';
    var _I18N = {
        zh: { minimize: '最小化', close: '关闭' },
        en: { minimize: 'Minimize', close: 'Close' },
        ar: { minimize: 'تصغير', close: 'إغلاق' }
    };
    var _T = _I18N[_LANG] || _I18N.en;

    // ==================== State ====================
    var _windows = {};       // scriptId -> windowState
    var _minimized = {};     // scriptId -> true
    var _zBase = 2147483600;
    var _zCurrent = _zBase;
    var _activeId = null;
    var _cascadeOffset = 0;

    // ==================== Init ====================
    function _injectStyles() {
        if (document.getElementById('wta-sw-styles')) return;
        var s = document.createElement('style');
        s.id = 'wta-sw-styles';
        s.textContent = SW_STYLES;
        document.head.appendChild(s);
    }

    function _ensureLayer() {
        if (document.getElementById('wta-sw-layer')) return;
        var layer = document.createElement('div');
        layer.id = 'wta-sw-layer';
        document.body.appendChild(layer);
    }

    function _ensureTaskbar() {
        if (document.getElementById('wta-sw-taskbar')) return;
        var tb = document.createElement('div');
        tb.id = 'wta-sw-taskbar';
        document.body.appendChild(tb);
    }

    function _updateTaskbar() {
        var tb = document.getElementById('wta-sw-taskbar');
        if (!tb) return;
        tb.innerHTML = '';
        var hasItems = false;
        for (var id in _minimized) {
            if (!_minimized[id]) continue;
            hasItems = true;
            var w = _windows[id];
            if (!w) continue;
            var item = document.createElement('div');
            item.className = 'wta-sw-taskbar-item';
            item.innerHTML = '<span class="icon">' + (w.icon || '\uD83D\uDC35') + '</span><span>' + (w.title || '').substring(0, 12) + '</span>';
            item.onclick = (function(sid) { return function() { WM.restoreWindow(sid); }; })(id);
            tb.appendChild(item);
        }
        tb.classList.toggle('visible', hasItems);
    }

    // ==================== Drag ====================
    function _makeDraggable(win, handle) {
        var dragging = false, sx, sy, sl, st;
        function onStart(e) {
            if (e.target.closest('.wta-sw-ctrl-btn') || e.target.closest('.wta-sw-resize')) return;
            dragging = true;
            var t = e.touches ? e.touches[0] : e;
            sx = t.clientX; sy = t.clientY;
            var r = win.getBoundingClientRect();
            sl = r.left; st = r.top;
            WM.bringToFront(win.dataset.scriptId);
            e.preventDefault();
        }
        function onMove(e) {
            if (!dragging) return;
            var t = e.touches ? e.touches[0] : e;
            var nx = sl + (t.clientX - sx);
            var ny = st + (t.clientY - sy);
            nx = Math.max(0, Math.min(nx, window.innerWidth - 60));
            ny = Math.max(0, Math.min(ny, window.innerHeight - 40));
            win.style.left = nx + 'px';
            win.style.top = ny + 'px';
            win.style.right = 'auto';
            win.style.bottom = 'auto';
            e.preventDefault();
        }
        function onEnd() { dragging = false; }
        handle.addEventListener('mousedown', onStart);
        handle.addEventListener('touchstart', onStart, {passive: false});
        document.addEventListener('mousemove', onMove);
        document.addEventListener('touchmove', onMove, {passive: false});
        document.addEventListener('mouseup', onEnd);
        document.addEventListener('touchend', onEnd);
    }

    // ==================== Resize ====================
    function _makeResizable(win, handle) {
        var resizing = false, sx, sy, sw, sh;
        function onStart(e) {
            resizing = true;
            var t = e.touches ? e.touches[0] : e;
            sx = t.clientX; sy = t.clientY;
            sw = win.offsetWidth; sh = win.offsetHeight;
            WM.bringToFront(win.dataset.scriptId);
            e.preventDefault();
            e.stopPropagation();
        }
        function onMove(e) {
            if (!resizing) return;
            var t = e.touches ? e.touches[0] : e;
            var nw = Math.max(200, sw + (t.clientX - sx));
            var nh = Math.max(150, sh + (t.clientY - sy));
            nw = Math.min(nw, window.innerWidth - win.offsetLeft);
            nh = Math.min(nh, window.innerHeight - win.offsetTop);
            win.style.width = nw + 'px';
            win.style.height = nh + 'px';
            e.preventDefault();
        }
        function onEnd() { resizing = false; }
        handle.addEventListener('mousedown', onStart);
        handle.addEventListener('touchstart', onStart, {passive: false});
        document.addEventListener('mousemove', onMove);
        document.addEventListener('touchmove', onMove, {passive: false});
        document.addEventListener('mouseup', onEnd);
        document.addEventListener('touchend', onEnd);
    }

    // ==================== Window Manager ====================
    var WM = {
        createWindow: function(scriptId, options) {
            options = options || {};
            if (_windows[scriptId] && _windows[scriptId].el) {
                // Already exists - show and bring to front
                if (_minimized[scriptId]) {
                    this.restoreWindow(scriptId);
                } else {
                    this.bringToFront(scriptId);
                }
                return _windows[scriptId];
            }

            _injectStyles();
            _ensureLayer();
            _ensureTaskbar();

            var w = options.width || 300;
            var h = options.height || 360;
            var title = options.title || scriptId;
            var icon = options.icon || '\uD83D\uDC35';

            // Cascade positioning
            var left = 20 + (_cascadeOffset * 30) % (window.innerWidth - w - 40);
            var top = 60 + (_cascadeOffset * 30) % (window.innerHeight - h - 100);
            _cascadeOffset++;

            var el = document.createElement('div');
            el.className = 'wta-sw-window';
            el.dataset.scriptId = scriptId;
            el.style.left = left + 'px';
            el.style.top = top + 'px';
            el.style.width = w + 'px';
            el.style.height = h + 'px';
            el.style.zIndex = ++_zCurrent;

            el.innerHTML =
                '<div class="wta-sw-titlebar">' +
                    '<span class="wta-sw-icon">' + icon + '</span>' +
                    '<span class="wta-sw-title">' + title + '</span>' +
                    '<div class="wta-sw-controls">' +
                        '<button class="wta-sw-ctrl-btn minimize" title="' + _T.minimize + '">\u2212</button>' +
                        '<button class="wta-sw-ctrl-btn close" title="' + _T.close + '">\u00d7</button>' +
                    '</div>' +
                '</div>' +
                '<div class="wta-sw-body" id="wta-sw-body-' + scriptId + '">' +
                    (options.content || '') +
                '</div>' +
                (options.resizable !== false ? '<div class="wta-sw-resize"></div>' : '');

            document.body.appendChild(el);

            // Bind events
            var titlebar = el.querySelector('.wta-sw-titlebar');
            _makeDraggable(el, titlebar);

            var resizeHandle = el.querySelector('.wta-sw-resize');
            if (resizeHandle) _makeResizable(el, resizeHandle);

            el.querySelector('.wta-sw-ctrl-btn.minimize').onclick = function() { WM.minimizeWindow(scriptId); };
            el.querySelector('.wta-sw-ctrl-btn.close').onclick = function() { WM.closeWindow(scriptId); };

            // Click to focus
            el.addEventListener('mousedown', function() { WM.bringToFront(scriptId); });
            el.addEventListener('touchstart', function() { WM.bringToFront(scriptId); }, {passive: true});

            // Double-click titlebar to maximize/restore
            var dblClickTimer = null;
            titlebar.addEventListener('click', function(e) {
                if (e.target.closest('.wta-sw-ctrl-btn')) return;
                if (dblClickTimer) {
                    clearTimeout(dblClickTimer);
                    dblClickTimer = null;
                    WM.toggleMaximize(scriptId);
                } else {
                    dblClickTimer = setTimeout(function() { dblClickTimer = null; }, 300);
                }
            });

            var state = {
                el: el,
                title: title,
                icon: icon,
                menuCommands: {},
                maximized: false,
                prevRect: null
            };
            _windows[scriptId] = state;
            delete _minimized[scriptId];

            // Animate in
            requestAnimationFrame(function() {
                requestAnimationFrame(function() {
                    el.classList.add('visible');
                });
            });

            this.bringToFront(scriptId);
            return state;
        },

        closeWindow: function(scriptId) {
            var w = _windows[scriptId];
            if (!w || !w.el) return;
            w.el.classList.remove('visible');
            setTimeout(function() {
                if (w.el && w.el.parentNode) w.el.parentNode.removeChild(w.el);
            }, 300);
            delete _windows[scriptId];
            delete _minimized[scriptId];
            if (_activeId === scriptId) _activeId = null;
            _updateTaskbar();
        },

        minimizeWindow: function(scriptId) {
            var w = _windows[scriptId];
            if (!w || !w.el) return;
            w.el.classList.add('minimizing');
            setTimeout(function() {
                w.el.style.display = 'none';
                w.el.classList.remove('minimizing');
            }, 300);
            _minimized[scriptId] = true;
            if (_activeId === scriptId) _activeId = null;
            _updateTaskbar();
        },

        restoreWindow: function(scriptId) {
            var w = _windows[scriptId];
            if (!w || !w.el) return;
            w.el.style.display = 'flex';
            w.el.classList.add('visible');
            delete _minimized[scriptId];
            this.bringToFront(scriptId);
            _updateTaskbar();
        },

        bringToFront: function(scriptId) {
            var w = _windows[scriptId];
            if (!w || !w.el) return;
            // Remove active from all
            for (var id in _windows) {
                if (_windows[id].el) _windows[id].el.classList.remove('active');
            }
            w.el.style.zIndex = ++_zCurrent;
            w.el.classList.add('active');
            _activeId = scriptId;
        },

        toggleMaximize: function(scriptId) {
            var w = _windows[scriptId];
            if (!w || !w.el) return;
            if (w.maximized) {
                // Restore
                if (w.prevRect) {
                    w.el.style.left = w.prevRect.left + 'px';
                    w.el.style.top = w.prevRect.top + 'px';
                    w.el.style.width = w.prevRect.width + 'px';
                    w.el.style.height = w.prevRect.height + 'px';
                }
                w.el.classList.remove('maximized');
                w.maximized = false;
            } else {
                // Save current rect
                var r = w.el.getBoundingClientRect();
                w.prevRect = { left: r.left, top: r.top, width: r.width, height: r.height };
                w.el.style.left = '0';
                w.el.style.top = '0';
                w.el.style.width = '100vw';
                w.el.style.height = '100vh';
                w.el.classList.add('maximized');
                w.maximized = true;
            }
        },

        updateContent: function(scriptId, html) {
            var body = document.getElementById('wta-sw-body-' + scriptId);
            if (body) body.innerHTML = html;
        },

        appendContent: function(scriptId, html) {
            var body = document.getElementById('wta-sw-body-' + scriptId);
            if (body) body.insertAdjacentHTML('beforeend', html);
        },

        getBodyElement: function(scriptId) {
            return document.getElementById('wta-sw-body-' + scriptId);
        },

        addMenuButton: function(scriptId, name, callback, options) {
            options = options || {};
            var w = _windows[scriptId];
            if (!w) {
                // Auto-create window for this script
                w = this.createWindow(scriptId, {
                    title: options.windowTitle || scriptId,
                    icon: options.windowIcon || '\uD83D\uDC35',
                    width: 280,
                    height: 320
                });
            }
            var body = document.getElementById('wta-sw-body-' + scriptId);
            if (!body) return;
            // Check if already exists
            var existingId = 'wta-sw-menubtn-' + scriptId + '-' + name.replace(/[^a-zA-Z0-9]/g, '_');
            if (document.getElementById(existingId)) return;
            var btn = document.createElement('button');
            btn.id = existingId;
            btn.className = 'wta-sw-menu-btn';
            btn.innerHTML = (options.icon || '\u25b6\ufe0f') + ' ' + name;
            btn.onclick = function() {
                try { callback(); } catch(e) { console.error('[WTA SW] Menu command error:', e); }
            };
            body.appendChild(btn);
            // Store reference
            if (w.menuCommands) w.menuCommands[name] = { btn: btn, fn: callback };
        },

        cascadeAll: function() {
            var offset = 0;
            for (var id in _windows) {
                var w = _windows[id];
                if (!w.el || _minimized[id]) continue;
                w.el.style.left = (20 + offset * 30) + 'px';
                w.el.style.top = (60 + offset * 30) + 'px';
                w.el.classList.remove('maximized');
                w.maximized = false;
                offset++;
            }
        },

        tileAll: function() {
            var visible = [];
            for (var id in _windows) {
                if (_windows[id].el && !_minimized[id]) visible.push(id);
            }
            if (visible.length === 0) return;
            var cols = Math.ceil(Math.sqrt(visible.length));
            var rows = Math.ceil(visible.length / cols);
            var cw = Math.floor(window.innerWidth / cols);
            var ch = Math.floor((window.innerHeight - 50) / rows);
            visible.forEach(function(id, i) {
                var w = _windows[id];
                var col = i % cols;
                var row = Math.floor(i / cols);
                w.el.style.left = (col * cw) + 'px';
                w.el.style.top = (row * ch) + 'px';
                w.el.style.width = cw + 'px';
                w.el.style.height = ch + 'px';
                w.el.classList.remove('maximized');
                w.maximized = false;
            });
        },

        getWindow: function(scriptId) {
            return _windows[scriptId] || null;
        },

        hasWindow: function(scriptId) {
            return !!(_windows[scriptId] && _windows[scriptId].el);
        },

        getOpenWindowIds: function() {
            var ids = [];
            for (var id in _windows) {
                if (_windows[id].el && !_minimized[id]) ids.push(id);
            }
            return ids;
        },

        destroy: function() {
            for (var id in _windows) {
                var w = _windows[id];
                if (w.el && w.el.parentNode) w.el.parentNode.removeChild(w.el);
            }
            _windows = {};
            _minimized = {};
            _activeId = null;
            var tb = document.getElementById('wta-sw-taskbar');
            if (tb && tb.parentNode) tb.parentNode.removeChild(tb);
            var layer = document.getElementById('wta-sw-layer');
            if (layer && layer.parentNode) layer.parentNode.removeChild(layer);
        }
    };

    window.__WTA_SCRIPT_WINDOWS__ = WM;
    console.log('[WTA] Script window manager initialized');
})();
""".trimIndent()
}
