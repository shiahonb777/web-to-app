package com.webtoapp.core.extension

/**
 * Chrome Extension API Polyfill
 *
 * JavaScript polyfill in WebView in Chrome Extension API.
 * chrome.* browser.* API extension .
 *
 * Supports API.
 * - chrome.runtime.id / getURL / sendMessage / onMessage / onInstalled / getManifest / lastError
 * - chrome.storage.local.get / set / remove / clear + onChanged
 * - chrome.storage.sync.
 * - chrome.tabs.create / query
 * - browser.*.
 */
object ChromeExtensionPolyfill {

    /**
     * Chrome Extension API polyfill.
     *
     * @param extensionId extension.
     * @param manifestJson extension manifest.json.
     * @param isBackground is as background script.
     * @return JS polyfill.
     */
    fun generatePolyfill(
        extensionId: String,
        manifestJson: String = "{}",
        isBackground: Boolean = false
    ): String {
        val safeExtId = extensionId.replace("\\", "\\\\").replace("'", "\\\'")
        val safeManifest = manifestJson.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
        
        return """
(function() {
    'use strict';
    
    var EXT_ID = '$safeExtId';
    
    // ===== Prevent double-init (per extension) =====
    var POLYFILL_KEY = '__WTA_CHROME_POLYFILL_' + EXT_ID + '__';
    if (window[POLYFILL_KEY]) return;
    window[POLYFILL_KEY] = true;
    var IS_BACKGROUND = ${isBackground};
    var STORAGE_PREFIX = '__wta_ext_' + EXT_ID + '_';
    var MANIFEST = {};
    try { MANIFEST = JSON.parse('$safeManifest'); } catch(e) { MANIFEST = {}; }
    
    // ===== Native Bridge Messaging =====
    var _msgCounter = 0;
    var _pendingResponses = {};
    
    // ===== Event System =====
    function ChromeEvent() {
        this._listeners = [];
    }
    ChromeEvent.prototype.addListener = function(fn) {
        if (typeof fn === 'function' && this._listeners.indexOf(fn) === -1) {
            this._listeners.push(fn);
        }
    };
    ChromeEvent.prototype.removeListener = function(fn) {
        var idx = this._listeners.indexOf(fn);
        if (idx !== -1) this._listeners.splice(idx, 1);
    };
    ChromeEvent.prototype.hasListener = function(fn) {
        return this._listeners.indexOf(fn) !== -1;
    };
    ChromeEvent.prototype.hasListeners = function() {
        return this._listeners.length > 0;
    };
    ChromeEvent.prototype._fire = function() {
        var args = Array.prototype.slice.call(arguments);
        var listeners = this._listeners.slice();
        for (var i = 0; i < listeners.length; i++) {
            try { listeners[i].apply(null, args); } catch(e) { console.error('[ChromePolyfill] Event listener error:', e); }
        }
    };
    
    // ===== Storage Implementation =====
    function StorageArea(areaName) {
        this._area = areaName;
        this._prefix = STORAGE_PREFIX + areaName + '_';
    }
    
    StorageArea.prototype.get = function(keys, callback) {
        var self = this;
        var result = {};
        try {
            if (keys === null || keys === undefined) {
                // Get all
                for (var i = 0; i < localStorage.length; i++) {
                    var k = localStorage.key(i);
                    if (k && k.indexOf(self._prefix) === 0) {
                        var realKey = k.substring(self._prefix.length);
                        try { result[realKey] = JSON.parse(localStorage.getItem(k)); } catch(e) { result[realKey] = localStorage.getItem(k); }
                    }
                }
            } else if (typeof keys === 'string') {
                var val = localStorage.getItem(self._prefix + keys);
                if (val !== null) { try { result[keys] = JSON.parse(val); } catch(e) { result[keys] = val; } }
            } else if (Array.isArray(keys)) {
                keys.forEach(function(key) {
                    var val = localStorage.getItem(self._prefix + key);
                    if (val !== null) { try { result[key] = JSON.parse(val); } catch(e) { result[key] = val; } }
                });
            } else if (typeof keys === 'object') {
                Object.keys(keys).forEach(function(key) {
                    var val = localStorage.getItem(self._prefix + key);
                    if (val !== null) { try { result[key] = JSON.parse(val); } catch(e) { result[key] = val; } }
                    else { result[key] = keys[key]; } // default value
                });
            }
        } catch(e) {
            console.error('[ChromePolyfill] storage.get error:', e);
        }
        
        chrome.runtime.lastError = null;
        if (typeof callback === 'function') { callback(result); }
        return Promise.resolve(result);
    };
    
    StorageArea.prototype.set = function(items, callback) {
        var self = this;
        var changes = {};
        try {
            Object.keys(items).forEach(function(key) {
                var oldVal = localStorage.getItem(self._prefix + key);
                var oldParsed = undefined;
                if (oldVal !== null) { try { oldParsed = JSON.parse(oldVal); } catch(e) { oldParsed = oldVal; } }
                
                localStorage.setItem(self._prefix + key, JSON.stringify(items[key]));
                changes[key] = { oldValue: oldParsed, newValue: items[key] };
            });
            
            // Fire onChanged event
            if (Object.keys(changes).length > 0 && chrome.storage.onChanged) {
                chrome.storage.onChanged._fire(changes, self._area);
            }
        } catch(e) {
            console.error('[ChromePolyfill] storage.set error:', e);
            chrome.runtime.lastError = { message: e.message };
        }
        
        if (typeof callback === 'function') { callback(); }
        return Promise.resolve();
    };
    
    StorageArea.prototype.remove = function(keys, callback) {
        var self = this;
        var changes = {};
        try {
            if (typeof keys === 'string') keys = [keys];
            keys.forEach(function(key) {
                var oldVal = localStorage.getItem(self._prefix + key);
                if (oldVal !== null) {
                    var oldParsed;
                    try { oldParsed = JSON.parse(oldVal); } catch(e) { oldParsed = oldVal; }
                    localStorage.removeItem(self._prefix + key);
                    changes[key] = { oldValue: oldParsed };
                }
            });
            
            if (Object.keys(changes).length > 0 && chrome.storage.onChanged) {
                chrome.storage.onChanged._fire(changes, self._area);
            }
        } catch(e) {
            console.error('[ChromePolyfill] storage.remove error:', e);
        }
        
        if (typeof callback === 'function') { callback(); }
        return Promise.resolve();
    };
    
    StorageArea.prototype.clear = function(callback) {
        var self = this;
        try {
            var keysToRemove = [];
            for (var i = 0; i < localStorage.length; i++) {
                var k = localStorage.key(i);
                if (k && k.indexOf(self._prefix) === 0) keysToRemove.push(k);
            }
            keysToRemove.forEach(function(k) { localStorage.removeItem(k); });
        } catch(e) {
            console.error('[ChromePolyfill] storage.clear error:', e);
        }
        
        if (typeof callback === 'function') { callback(); }
        return Promise.resolve();
    };
    
    StorageArea.prototype.getBytesInUse = function(keys, callback) {
        var result = 0;
        if (typeof callback === 'function') { callback(result); }
        return Promise.resolve(result);
    };
    
    // ===== Build chrome.* API =====
    var localArea = new StorageArea('local');
    var syncArea = new StorageArea('sync'); // sync maps to local in WebView
    var sessionArea = new StorageArea('session');
    
    var onMessageEvent = new ChromeEvent();
    var onInstalledEvent = new ChromeEvent();
    var onChangedEvent = new ChromeEvent();
    
    var _chrome = {
        runtime: {
            id: EXT_ID,
            lastError: null,
            
            getURL: function(path) {
                // Return an HTTPS localhost URL that WebView's shouldInterceptRequest
                // can reliably intercept for <link>, <img>, fetch(), etc.
                // chrome-extension:// is a non-standard scheme that may not reliably
                // trigger onload events on DOM elements in Android WebView.
                return 'https://localhost/__ext__/' + EXT_ID + '/' + (path || '');
            },
            
            getManifest: function() {
                return MANIFEST;
            },
            
            sendMessage: function(extensionId, message, options, responseCallback) {
                // Normalize arguments (extensionId is optional)
                if (typeof extensionId !== 'string' || extensionId === EXT_ID) {
                    responseCallback = options || message;
                    message = (typeof extensionId === 'string') ? message : extensionId;
                }
                if (typeof responseCallback !== 'function') responseCallback = undefined;
                
                // Handle known message types internally
                if (message && typeof message === 'object') {
                    var msgType = message.type || message.action;
                    if (msgType === 'openLinkInBackground' || msgType === 'OPEN_LINK_IN_BACKGROUND') {
                        var url = (message.data && message.data.url) || message.url;
                        if (url) {
                            try { window.open(url, '_blank'); } catch(e) { console.warn('[ChromePolyfill] window.open failed:', e); }
                        }
                        if (typeof responseCallback === 'function') responseCallback({ success: true });
                        return Promise.resolve({ success: true });
                    }
                }
                
                // Content mode: route to background WebView via native bridge
                if (!IS_BACKGROUND && typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.postMessageToBackground === 'function') {
                    var msgId = '__msg_' + (++_msgCounter);
                    var promise = new Promise(function(resolve) {
                        _pendingResponses[msgId] = function(response) {
                            if (typeof responseCallback === 'function') responseCallback(response);
                            resolve(response);
                        };
                    });
                    try {
                        WtaExtBridge.postMessageToBackground(EXT_ID, JSON.stringify({
                            msgId: msgId,
                            message: message
                        }));
                    } catch(e) {
                        console.error('[ChromePolyfill] postMessageToBackground error:', e);
                        delete _pendingResponses[msgId];
                        if (typeof responseCallback === 'function') responseCallback(undefined);
                        return Promise.resolve(undefined);
                    }
                    return promise;
                }
                
                // Fallback: fire locally (no background runtime available)
                var responded = false;
                var sendResponse = function(response) {
                    if (!responded) {
                        responded = true;
                        if (typeof responseCallback === 'function') responseCallback(response);
                    }
                };
                onMessageEvent._fire(message, { id: EXT_ID, url: location.href }, sendResponse);
                if (!responded) {
                    setTimeout(function() { sendResponse(undefined); }, 0);
                }
                return new Promise(function(resolve) {
                    var origCb = responseCallback;
                    responseCallback = function(response) {
                        if (typeof origCb === 'function') origCb(response);
                        resolve(response);
                    };
                });
            },
            
            onMessage: onMessageEvent,
            onInstalled: onInstalledEvent,
            
            connect: function() {
                return {
                    onMessage: new ChromeEvent(),
                    onDisconnect: new ChromeEvent(),
                    postMessage: function() {},
                    disconnect: function() {},
                    name: ''
                };
            },
            
            onConnect: new ChromeEvent(),
            
            getPlatformInfo: function(callback) {
                var info = { os: 'android', arch: 'arm' };
                if (typeof callback === 'function') callback(info);
                return Promise.resolve(info);
            },
            
            openOptionsPage: function(callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            
            setUninstallURL: function(url, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            
            getBackgroundPage: function(callback) {
                if (typeof callback === 'function') callback(null);
                return Promise.resolve(null);
            }
        },
        
        storage: {
            local: localArea,
            sync: syncArea,
            session: sessionArea,
            managed: new StorageArea('managed'),
            onChanged: onChangedEvent
        },
        
        tabs: {
            create: function(createProperties, callback) {
                var url = createProperties && createProperties.url;
                if (url) {
                    try { window.open(url, '_blank'); } catch(e) { console.warn('[ChromePolyfill] tabs.create window.open failed:', e); }
                }
                var tab = { id: 1, url: url || '', active: true };
                if (typeof callback === 'function') callback(tab);
                return Promise.resolve(tab);
            },
            
            query: function(queryInfo, callback) {
                var tabs = [{ id: 1, url: location.href, active: true, title: document.title }];
                if (typeof callback === 'function') callback(tabs);
                return Promise.resolve(tabs);
            },
            
            sendMessage: function(tabId, message, options, responseCallback) {
                if (typeof options === 'function') { responseCallback = options; options = undefined; }
                if (typeof responseCallback !== 'function') responseCallback = undefined;
                
                // Background mode: route to content script via native bridge
                if (IS_BACKGROUND && typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.sendMessageToTab === 'function') {
                    var msgId = '__tabmsg_' + (++_msgCounter);
                    var promise = new Promise(function(resolve) {
                        _pendingResponses[msgId] = function(response) {
                            if (typeof responseCallback === 'function') responseCallback(response);
                            resolve(response);
                        };
                    });
                    try {
                        WtaExtBridge.sendMessageToTab(EXT_ID, JSON.stringify({
                            msgId: msgId,
                            tabId: tabId,
                            message: message
                        }));
                    } catch(e) {
                        console.error('[ChromePolyfill] sendMessageToTab error:', e);
                        delete _pendingResponses[msgId];
                        if (typeof responseCallback === 'function') responseCallback(undefined);
                        return Promise.resolve(undefined);
                    }
                    return promise;
                }
                
                // Content mode fallback: fire onMessage locally
                var responded = false;
                var sendResponse = function(response) {
                    if (!responded) { responded = true; if (typeof responseCallback === 'function') responseCallback(response); }
                };
                onMessageEvent._fire(message, { id: EXT_ID, tab: { id: tabId } }, sendResponse);
                if (!responded && typeof responseCallback === 'function') responseCallback(undefined);
                return Promise.resolve();
            },
            
            get: function(tabId, callback) {
                var tab = { id: tabId, url: location.href, active: true, title: document.title };
                if (typeof callback === 'function') callback(tab);
                return Promise.resolve(tab);
            },
            
            getCurrent: function(callback) {
                var tab = { id: 1, url: location.href, active: true, title: document.title };
                if (typeof callback === 'function') callback(tab);
                return Promise.resolve(tab);
            },
            
            update: function(tabId, updateProperties, callback) {
                if (typeof tabId === 'object') { updateProperties = tabId; tabId = 1; }
                if (updateProperties && updateProperties.url) {
                    try { window.location.href = updateProperties.url; } catch(e) { console.warn('[ChromePolyfill] tabs.update navigation failed:', e); }
                }
                var tab = { id: tabId || 1, url: location.href, active: true };
                if (typeof callback === 'function') callback(tab);
                return Promise.resolve(tab);
            },
            
            onUpdated: new ChromeEvent(),
            onActivated: new ChromeEvent(),
            onRemoved: new ChromeEvent(),
            onCreated: new ChromeEvent()
        },
        
        i18n: {
            getMessage: function(messageName, substitutions) {
                return messageName || '';
            },
            getUILanguage: function() {
                return navigator.language || 'en';
            },
            detectLanguage: function(text, callback) {
                var result = { isReliable: false, languages: [{ language: 'und', percentage: 100 }] };
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            }
        },
        
        notifications: {
            create: function(id, options, callback) {
                if (typeof id === 'object') { options = id; id = 'notif_' + Date.now(); }
                // Best-effort: use Notification API if available
                try {
                    if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
                        new Notification(options.title || '', { body: options.message || '' });
                    }
                } catch(e) { console.warn('[ChromePolyfill] notifications.create failed:', e); }
                if (typeof callback === 'function') callback(id);
                return Promise.resolve(id);
            },
            clear: function(id, callback) {
                if (typeof callback === 'function') callback(true);
                return Promise.resolve(true);
            },
            onClicked: new ChromeEvent(),
            onClosed: new ChromeEvent()
        },
        
        permissions: {
            contains: function(perms, callback) {
                if (typeof callback === 'function') callback(true);
                return Promise.resolve(true);
            },
            request: function(perms, callback) {
                if (typeof callback === 'function') callback(true);
                return Promise.resolve(true);
            },
            getAll: function(callback) {
                var result = { permissions: [], origins: [] };
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            },
            onAdded: new ChromeEvent(),
            onRemoved: new ChromeEvent()
        },
        
        alarms: {
            _alarms: {},
            create: function(name, alarmInfo) {
                if (typeof name === 'object') { alarmInfo = name; name = ''; }
                var delay = (alarmInfo.delayInMinutes || alarmInfo.when ? 0 : 1) * 60000;
                if (alarmInfo.delayInMinutes) delay = alarmInfo.delayInMinutes * 60000;
                var timerId = setTimeout(function() {
                    chrome.alarms.onAlarm._fire({ name: name });
                }, delay);
                chrome.alarms._alarms[name] = timerId;
            },
            clear: function(name, callback) {
                if (chrome.alarms._alarms[name]) {
                    clearTimeout(chrome.alarms._alarms[name]);
                    delete chrome.alarms._alarms[name];
                }
                if (typeof callback === 'function') callback(true);
                return Promise.resolve(true);
            },
            clearAll: function(callback) {
                Object.keys(chrome.alarms._alarms).forEach(function(k) {
                    clearTimeout(chrome.alarms._alarms[k]);
                });
                chrome.alarms._alarms = {};
                if (typeof callback === 'function') callback(true);
                return Promise.resolve(true);
            },
            get: function(name, callback) {
                if (typeof callback === 'function') callback(null);
                return Promise.resolve(null);
            },
            getAll: function(callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            onAlarm: new ChromeEvent()
        },
        
        webRequest: (function() {
            // Enhanced webRequest with native bridge registration
            var _onBeforeRequest = new ChromeEvent();
            var _origAddListener = _onBeforeRequest.addListener.bind(_onBeforeRequest);
            _onBeforeRequest.addListener = function(callback, filter, extraInfoSpec) {
                _origAddListener(callback);
                // Register filter patterns with native bridge for real request blocking
                if (filter && filter.urls && typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.registerWebRequestFilter === 'function') {
                    var isBlocking = Array.isArray(extraInfoSpec) && extraInfoSpec.indexOf('blocking') !== -1;
                    var types = (filter.types && Array.isArray(filter.types)) ? JSON.stringify(filter.types) : '[]';
                    try {
                        WtaExtBridge.registerWebRequestFilter(
                            EXT_ID,
                            JSON.stringify(filter.urls),
                            types,
                            isBlocking
                        );
                        console.log('[ChromePolyfill] Registered webRequest filter:', filter.urls.length, 'patterns, blocking=' + isBlocking);
                    } catch(e) {
                        console.error('[ChromePolyfill] registerWebRequestFilter error:', e);
                    }
                }
            };
            return {
                onBeforeRequest: _onBeforeRequest,
                onBeforeSendHeaders: new ChromeEvent(),
                onHeadersReceived: new ChromeEvent(),
                onAuthRequired: new ChromeEvent(),
                onResponseStarted: new ChromeEvent(),
                onCompleted: new ChromeEvent(),
                onErrorOccurred: new ChromeEvent(),
                handlerBehaviorChanged: function(callback) {
                    if (typeof callback === 'function') callback();
                }
            };
        })(),
        
        webNavigation: {
            onCompleted: new ChromeEvent(),
            onCommitted: new ChromeEvent(),
            onBeforeNavigate: new ChromeEvent()
        },
        
        extension: {
            getURL: function(path) {
                return _chrome.runtime.getURL(path);
            },
            isAllowedIncognitoAccess: function(callback) {
                if (typeof callback === 'function') callback(false);
                return Promise.resolve(false);
            }
        },
        
        contextMenus: {
            create: function(createProperties, callback) {
                if (typeof callback === 'function') callback();
            },
            remove: function(menuItemId, callback) {
                if (typeof callback === 'function') callback();
            },
            removeAll: function(callback) {
                if (typeof callback === 'function') callback();
            },
            onClicked: new ChromeEvent()
        },
        
        commands: {
            getAll: function(callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            onCommand: new ChromeEvent()
        },
        
        declarativeNetRequest: {
            updateDynamicRules: function(options, callback) {
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.updateDnrDynamicRules === 'function') {
                        var addRules = (options && options.addRules) ? JSON.stringify(options.addRules) : '[]';
                        var removeIds = (options && options.removeRuleIds) ? JSON.stringify(options.removeRuleIds) : '[]';
                        WtaExtBridge.updateDnrDynamicRules(EXT_ID, addRules, removeIds);
                    }
                } catch(e) { console.error('[ChromePolyfill] updateDynamicRules error:', e); }
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            getDynamicRules: function(callback) {
                var rules = [];
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.getDnrDynamicRules === 'function') {
                        var json = WtaExtBridge.getDnrDynamicRules(EXT_ID);
                        rules = JSON.parse(json || '[]');
                    }
                } catch(e) { console.warn('[ChromePolyfill] getDynamicRules error:', e); }
                if (typeof callback === 'function') callback(rules);
                return Promise.resolve(rules);
            },
            updateSessionRules: function(options, callback) {
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.updateDnrSessionRules === 'function') {
                        var addRules = (options && options.addRules) ? JSON.stringify(options.addRules) : '[]';
                        var removeIds = (options && options.removeRuleIds) ? JSON.stringify(options.removeRuleIds) : '[]';
                        WtaExtBridge.updateDnrSessionRules(EXT_ID, addRules, removeIds);
                    }
                } catch(e) { console.error('[ChromePolyfill] updateSessionRules error:', e); }
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            getSessionRules: function(callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            getMatchedRules: function(filter, callback) {
                if (typeof filter === 'function') { callback = filter; }
                var result = { rulesMatchedInfo: [] };
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            },
            isRegexSupported: function(regexOptions, callback) {
                var result = { isSupported: true };
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            },
            getAvailableStaticRuleCount: function(callback) {
                if (typeof callback === 'function') callback(30000);
                return Promise.resolve(30000);
            },
            onRuleMatchedDebug: new ChromeEvent()
        },
        
        cookies: {
            getAll: function(details, callback) {
                var domain = (details && details.domain) || '';
                var url = (details && details.url) || '';
                if (!url && domain) {
                    url = 'https://' + domain.replace(/^\./, '') + '/';
                }
                
                var result = [];
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.getCookies === 'function') {
                        var cookieStr = WtaExtBridge.getCookies(url);
                        if (cookieStr) {
                            cookieStr.split(';').forEach(function(pair) {
                                pair = pair.trim();
                                var eqIdx = pair.indexOf('=');
                                if (eqIdx > 0) {
                                    var name = pair.substring(0, eqIdx).trim();
                                    var value = pair.substring(eqIdx + 1);
                                    if (details && details.name && details.name !== name) return;
                                    result.push({
                                        name: name,
                                        value: value,
                                        domain: domain || (function(){ try { return new URL(url).hostname; } catch(e) { return ''; } })(),
                                        path: '/',
                                        secure: url.indexOf('https') === 0,
                                        httpOnly: false,
                                        sameSite: 'unspecified',
                                        storeId: '0'
                                    });
                                }
                            });
                        }
                    }
                } catch(e) {
                    console.error('[ChromePolyfill] cookies.getAll error:', e);
                }
                
                chrome.runtime.lastError = null;
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            },
            
            get: function(details, callback) {
                return _chrome.cookies.getAll(details).then(function(arr) {
                    var cookie = arr.length > 0 ? arr[0] : null;
                    if (typeof callback === 'function') callback(cookie);
                    return cookie;
                });
            },
            
            set: function(details, callback) {
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.setCookieValue === 'function') {
                        var url = details.url || 'https://' + (details.domain || '').replace(/^\./, '') + '/';
                        var cookieStr = details.name + '=' + details.value;
                        if (details.path) cookieStr += '; path=' + details.path;
                        if (details.domain) cookieStr += '; domain=' + details.domain;
                        if (details.secure) cookieStr += '; secure';
                        if (details.httpOnly) cookieStr += '; httponly';
                        if (details.expirationDate) {
                            cookieStr += '; expires=' + new Date(details.expirationDate * 1000).toUTCString();
                        }
                        WtaExtBridge.setCookieValue(url, cookieStr);
                    }
                } catch(e) {
                    console.error('[ChromePolyfill] cookies.set error:', e);
                }
                var cookie = { name: details.name, value: details.value, domain: details.domain || '' };
                if (typeof callback === 'function') callback(cookie);
                return Promise.resolve(cookie);
            },
            
            remove: function(details, callback) {
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.setCookieValue === 'function') {
                        var url = details.url || 'https://' + (details.domain || '').replace(/^\./, '') + '/';
                        WtaExtBridge.setCookieValue(url, details.name + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT');
                    }
                } catch(e) { console.warn('[ChromePolyfill] cookies.remove error:', e); }
                if (typeof callback === 'function') callback(details);
                return Promise.resolve(details);
            },
            
            getAllCookieStores: function(callback) {
                var stores = [{ id: '0', tabIds: [1] }];
                if (typeof callback === 'function') callback(stores);
                return Promise.resolve(stores);
            },
            
            onChanged: new ChromeEvent()
        },
        
        scripting: {
            executeScript: function(injection, callback) {
                // Stub: executeScript is handled natively by WebViewManager
                var results = [{ result: undefined }];
                if (typeof callback === 'function') callback(results);
                return Promise.resolve(results);
            },
            insertCSS: function(injection, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            removeCSS: function(injection, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            registerContentScripts: function(scripts, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            unregisterContentScripts: function(filter, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            getRegisteredContentScripts: function(filter, callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            }
        },
        
        action: {
            setIcon: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            setBadgeText: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            setBadgeBackgroundColor: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            setTitle: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            setPopup: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            onClicked: new ChromeEvent()
        },
        
        sidePanel: {
            setOptions: function(options, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            setPanelBehavior: function(behavior, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            }
        },
        
        // ===== Phase J: downloads =====
        downloads: {
            download: function(options, callback) {
                var downloadId = -1;
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.startDownload === 'function') {
                        var url = options.url || '';
                        var filename = options.filename || '';
                        var headers = options.headers ? JSON.stringify(options.headers) : '{}';
                        downloadId = parseInt(WtaExtBridge.startDownload(url, filename, headers)) || -1;
                    }
                } catch(e) { console.error('[ChromePolyfill] downloads.download error:', e); }
                if (typeof callback === 'function') callback(downloadId);
                return Promise.resolve(downloadId);
            },
            search: function(query, callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            pause: function(downloadId, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            resume: function(downloadId, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            cancel: function(downloadId, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            open: function(downloadId) {},
            show: function(downloadId) {},
            showDefaultFolder: function() {},
            erase: function(query, callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            removeFile: function(downloadId, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            getFileIcon: function(downloadId, options, callback) {
                if (typeof options === 'function') { callback = options; }
                if (typeof callback === 'function') callback(undefined);
                return Promise.resolve(undefined);
            },
            setShelfEnabled: function(enabled) {},
            setUiOptions: function(options, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            onCreated: new ChromeEvent(),
            onErased: new ChromeEvent(),
            onChanged: new ChromeEvent(),
            onDeterminingFilename: new ChromeEvent()
        },
        
        // ===== Phase J: identity =====
        identity: {
            getAuthToken: function(details, callback) {
                if (typeof details === 'function') { callback = details; }
                chrome.runtime.lastError = { message: 'getAuthToken is not available in WebView context' };
                if (typeof callback === 'function') callback(undefined);
                return Promise.reject(new Error('getAuthToken not available'));
            },
            removeCachedAuthToken: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            launchWebAuthFlow: function(details, callback) {
                // Best-effort: open auth URL, extension should handle redirect
                var url = details && details.url;
                if (url) {
                    try { window.open(url, '_blank'); } catch(e) { console.warn('[ChromePolyfill] launchWebAuthFlow window.open failed:', e); }
                }
                if (typeof callback === 'function') callback(url);
                return Promise.resolve(url);
            },
            getProfileUserInfo: function(details, callback) {
                if (typeof details === 'function') { callback = details; }
                var info = { email: '', id: '' };
                if (typeof callback === 'function') callback(info);
                return Promise.resolve(info);
            },
            getRedirectURL: function(path) {
                return 'https://' + EXT_ID + '.chromiumapp.org/' + (path || '');
            },
            onSignInChanged: new ChromeEvent()
        },
        
        // ===== Phase J: history =====
        history: {
            search: function(query, callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            getVisits: function(details, callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            addUrl: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            deleteUrl: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            deleteRange: function(range, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            deleteAll: function(callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            onVisited: new ChromeEvent(),
            onVisitRemoved: new ChromeEvent()
        },
        
        // ===== Phase J: proxy =====
        proxy: {
            settings: {
                get: function(details, callback) {
                    var config = { value: { mode: 'direct' }, levelOfControl: 'controllable_by_this_extension' };
                    if (typeof callback === 'function') callback(config);
                    return Promise.resolve(config);
                },
                set: function(details, callback) {
                    console.log('[ChromePolyfill] proxy.settings.set (limited support):', JSON.stringify(details));
                    if (typeof callback === 'function') callback();
                    return Promise.resolve();
                },
                clear: function(details, callback) {
                    if (typeof callback === 'function') callback();
                    return Promise.resolve();
                },
                onChange: new ChromeEvent()
            },
            onProxyError: new ChromeEvent()
        },
        
        // ===== Phase J: privacy =====
        privacy: (function() {
            function ChromeSettingType(defaultValue) {
                var _value = defaultValue;
                return {
                    get: function(details, callback) {
                        var result = { value: _value, levelOfControl: 'controllable_by_this_extension' };
                        if (typeof callback === 'function') callback(result);
                        return Promise.resolve(result);
                    },
                    set: function(details, callback) {
                        if (details && details.value !== undefined) _value = details.value;
                        if (typeof callback === 'function') callback();
                        return Promise.resolve();
                    },
                    clear: function(details, callback) {
                        if (typeof callback === 'function') callback();
                        return Promise.resolve();
                    },
                    onChange: new ChromeEvent()
                };
            }
            return {
                network: {
                    networkPredictionEnabled: ChromeSettingType(true),
                    webRTCIPHandlingPolicy: ChromeSettingType('default')
                },
                services: {
                    alternateErrorPagesEnabled: ChromeSettingType(true),
                    autofillEnabled: ChromeSettingType(true),
                    autofillAddressEnabled: ChromeSettingType(true),
                    autofillCreditCardEnabled: ChromeSettingType(true),
                    passwordSavingEnabled: ChromeSettingType(true),
                    safeBrowsingEnabled: ChromeSettingType(true),
                    safeBrowsingExtendedReportingEnabled: ChromeSettingType(false),
                    searchSuggestEnabled: ChromeSettingType(true),
                    spellingServiceEnabled: ChromeSettingType(false),
                    translationServiceEnabled: ChromeSettingType(true)
                },
                websites: {
                    thirdPartyCookiesAllowed: ChromeSettingType(true),
                    hyperlinkAuditingEnabled: ChromeSettingType(true),
                    referrersEnabled: ChromeSettingType(true),
                    doNotTrackEnabled: ChromeSettingType(false),
                    protectedContentEnabled: ChromeSettingType(true),
                    topicsEnabled: ChromeSettingType(false)
                }
            };
        })(),
        
        // ===== Phase L: windows =====
        windows: {
            WINDOW_ID_NONE: -1,
            WINDOW_ID_CURRENT: -2,
            getAll: function(getInfo, callback) {
                if (typeof getInfo === 'function') { callback = getInfo; }
                var wins = [{ id: 1, focused: true, top: 0, left: 0, width: screen.width, height: screen.height, type: 'normal', state: 'normal', tabs: [{ id: 1, url: location.href, active: true, title: document.title }] }];
                if (typeof callback === 'function') callback(wins);
                return Promise.resolve(wins);
            },
            getCurrent: function(getInfo, callback) {
                if (typeof getInfo === 'function') { callback = getInfo; }
                var win = { id: 1, focused: true, top: 0, left: 0, width: screen.width, height: screen.height, type: 'normal', state: 'normal' };
                if (typeof callback === 'function') callback(win);
                return Promise.resolve(win);
            },
            get: function(windowId, getInfo, callback) {
                if (typeof getInfo === 'function') { callback = getInfo; }
                return _chrome.windows.getCurrent({}, callback);
            },
            getLastFocused: function(getInfo, callback) {
                if (typeof getInfo === 'function') { callback = getInfo; }
                return _chrome.windows.getCurrent({}, callback);
            },
            create: function(createData, callback) {
                if (createData && createData.url) { try { window.open(createData.url); } catch(e) { console.warn('[ChromePolyfill] windows.create window.open failed:', e); } }
                var win = { id: 2, focused: true, type: 'normal', state: 'normal' };
                if (typeof callback === 'function') callback(win);
                return Promise.resolve(win);
            },
            update: function(windowId, updateInfo, callback) {
                if (typeof callback === 'function') callback({ id: windowId });
                return Promise.resolve({ id: windowId });
            },
            remove: function(windowId, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            onCreated: new ChromeEvent(),
            onRemoved: new ChromeEvent(),
            onFocusChanged: new ChromeEvent(),
            onBoundsChanged: new ChromeEvent()
        },
        
        // ===== Phase L: management =====
        management: {
            getSelf: function(callback) {
                var info = { id: EXT_ID, name: (MANIFEST.name || EXT_ID), version: (MANIFEST.version || '0.0.0'), type: 'extension', enabled: true, installType: 'normal' };
                if (typeof callback === 'function') callback(info);
                return Promise.resolve(info);
            },
            getAll: function(callback) {
                var list = [{ id: EXT_ID, name: (MANIFEST.name || EXT_ID), version: (MANIFEST.version || '0.0.0'), type: 'extension', enabled: true }];
                if (typeof callback === 'function') callback(list);
                return Promise.resolve(list);
            },
            get: function(id, callback) {
                return _chrome.management.getSelf(callback);
            },
            setEnabled: function(id, enabled, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            uninstall: function(id, options, callback) {
                if (typeof options === 'function') { callback = options; }
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            uninstallSelf: function(options, callback) {
                if (typeof options === 'function') { callback = options; }
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            onEnabled: new ChromeEvent(),
            onDisabled: new ChromeEvent(),
            onInstalled: new ChromeEvent(),
            onUninstalled: new ChromeEvent()
        },
        
        // ===== Phase L: topSites =====
        topSites: {
            get: function(callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            }
        },
        
        // ===== Phase L: fontSettings =====
        fontSettings: {
            getFont: function(details, callback) {
                var result = { fontId: 'sans-serif', levelOfControl: 'not_controllable' };
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            },
            setFont: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            getFontList: function(callback) {
                var fonts = [{ fontId: 'sans-serif', displayName: 'Sans Serif' }, { fontId: 'serif', displayName: 'Serif' }, { fontId: 'monospace', displayName: 'Monospace' }];
                if (typeof callback === 'function') callback(fonts);
                return Promise.resolve(fonts);
            },
            getDefaultFontSize: function(details, callback) {
                if (typeof details === 'function') { callback = details; }
                var result = { pixelSize: 16, levelOfControl: 'not_controllable' };
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            },
            setDefaultFontSize: function(details, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            onFontChanged: new ChromeEvent(),
            onDefaultFontSizeChanged: new ChromeEvent()
        },
        
        // ===== Phase L: system =====
        system: {
            cpu: {
                getInfo: function(callback) {
                    var info = { numOfProcessors: navigator.hardwareConcurrency || 4, archName: 'arm', modelName: 'Android Device', features: [] };
                    if (typeof callback === 'function') callback(info);
                    return Promise.resolve(info);
                }
            },
            memory: {
                getInfo: function(callback) {
                    var info = { capacity: (navigator.deviceMemory || 4) * 1073741824, availableCapacity: (navigator.deviceMemory || 4) * 536870912 };
                    if (typeof callback === 'function') callback(info);
                    return Promise.resolve(info);
                }
            },
            display: {
                getInfo: function(flags, callback) {
                    if (typeof flags === 'function') { callback = flags; }
                    var info = [{ id: '0', name: 'Default', isPrimary: true, isInternal: true, isEnabled: true, bounds: { left: 0, top: 0, width: screen.width, height: screen.height }, workArea: { left: 0, top: 0, width: screen.availWidth, height: screen.availHeight } }];
                    if (typeof callback === 'function') callback(info);
                    return Promise.resolve(info);
                },
                onDisplayChanged: new ChromeEvent()
            },
            storage: {
                getInfo: function(callback) {
                    var info = [{ id: '0', name: 'Internal', type: 'fixed', capacity: 64 * 1073741824 }];
                    if (typeof callback === 'function') callback(info);
                    return Promise.resolve(info);
                },
                onAttached: new ChromeEvent(),
                onDetached: new ChromeEvent()
            }
        },
        
        // ===== Phase L: bookmarks (stub) =====
        bookmarks: {
            get: function(idOrList, callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            getTree: function(callback) {
                if (typeof callback === 'function') callback([{ id: '0', title: '', children: [] }]);
                return Promise.resolve([{ id: '0', title: '', children: [] }]);
            },
            create: function(bookmark, callback) {
                var result = { id: String(Date.now()), title: bookmark.title || '', url: bookmark.url || '' };
                if (typeof callback === 'function') callback(result);
                return Promise.resolve(result);
            },
            remove: function(id, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            search: function(query, callback) {
                if (typeof callback === 'function') callback([]);
                return Promise.resolve([]);
            },
            onCreated: new ChromeEvent(),
            onRemoved: new ChromeEvent(),
            onChanged: new ChromeEvent()
        },
        
        // ===== Phase L: offscreen (MV3) =====
        offscreen: {
            createDocument: function(parameters, callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            closeDocument: function(callback) {
                if (typeof callback === 'function') callback();
                return Promise.resolve();
            },
            hasDocument: function(callback) {
                if (typeof callback === 'function') callback(false);
                return Promise.resolve(false);
            },
            Reason: { TESTING: 'TESTING', AUDIO_PLAYBACK: 'AUDIO_PLAYBACK', DOM_SCRAPING: 'DOM_SCRAPING', BLOBS: 'BLOBS', DOM_PARSER: 'DOM_PARSER', USER_MEDIA: 'USER_MEDIA', DISPLAY_MEDIA: 'DISPLAY_MEDIA', WEB_RTC: 'WEB_RTC', CLIPBOARD: 'CLIPBOARD', LOCAL_STORAGE: 'LOCAL_STORAGE', WORKERS: 'WORKERS', BATTERY_STATUS: 'BATTERY_STATUS', MATCH_MEDIA: 'MATCH_MEDIA', GEOLOCATION: 'GEOLOCATION' }
        },
        
        // ===== Phase L: tts =====
        tts: {
            speak: function(utterance, options, callback) {
                try {
                    if (typeof speechSynthesis !== 'undefined') {
                        var u = new SpeechSynthesisUtterance(utterance);
                        if (options && options.lang) u.lang = options.lang;
                        if (options && options.rate) u.rate = options.rate;
                        speechSynthesis.speak(u);
                    }
                } catch(e) { console.warn('[ChromePolyfill] tts.speak error:', e); }
                if (typeof callback === 'function') callback();
            },
            stop: function() {
                try { if (typeof speechSynthesis !== 'undefined') speechSynthesis.cancel(); } catch(e) { console.warn('[ChromePolyfill] tts.stop error:', e); }
            },
            isSpeaking: function(callback) {
                var speaking = false;
                try { if (typeof speechSynthesis !== 'undefined') speaking = speechSynthesis.speaking; } catch(e) { console.warn('[ChromePolyfill] tts.isSpeaking error:', e); }
                if (typeof callback === 'function') callback(speaking);
                return Promise.resolve(speaking);
            },
            getVoices: function(callback) {
                var voices = [];
                try { if (typeof speechSynthesis !== 'undefined') voices = speechSynthesis.getVoices().map(function(v) { return { voiceName: v.name, lang: v.lang }; }); } catch(e) { console.warn('[ChromePolyfill] tts.getVoices error:', e); }
                if (typeof callback === 'function') callback(voices);
                return Promise.resolve(voices);
            },
            onEvent: new ChromeEvent()
        },
        
        // ===== Phase L: browserAction (MV2 compat) =====
        browserAction: {
            setIcon: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            setBadgeText: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            setBadgeBackgroundColor: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            setTitle: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            setPopup: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            getPopup: function(details, callback) { if (typeof callback === 'function') callback(''); return Promise.resolve(''); },
            getBadgeText: function(details, callback) { if (typeof callback === 'function') callback(''); return Promise.resolve(''); },
            onClicked: new ChromeEvent()
        },
        
        // ===== Phase L: pageAction (MV2 compat) =====
        pageAction: {
            show: function(tabId, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            hide: function(tabId, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            setIcon: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            setTitle: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            setPopup: function(details, callback) { if (typeof callback === 'function') callback(); return Promise.resolve(); },
            onClicked: new ChromeEvent()
        }
    };
    
    // ===== Assign to globalThis =====
    // Full overwrite: each extension gets its own chrome.* API with correct EXT_ID closures.
    // In a real browser, each extension has its own isolated world with its own chrome binding.
    // Since WebView shares a single JS context, we overwrite chrome.* for each extension.
    // Per-extension state (storage prefix, runtime.id, getURL) is captured via closures.
    if (!globalThis.chrome) globalThis.chrome = {};
    Object.keys(_chrome).forEach(function(key) {
        globalThis.chrome[key] = _chrome[key];
    });
    
    // ===== browser.* alias =====
    // Our chrome.* API implementations already return Promises for async methods
    // (storage.get, sendMessage, tabs.create, etc.) and direct values for sync methods
    // (getURL, getManifest, getMessage, etc.).
    //
    // IMPORTANT: Do NOT wrap chrome with a Proxy that Promise-wraps all functions.
    // A Proxy wrapper causes synchronous APIs like getURL() to return Promise objects
    // instead of strings. This breaks DOM attribute assignments like:
    // linkEl.setAttribute('href', browser.runtime.getURL('style.css'))
    // which would set href="[object Promise]" instead of the actual URL.
    //
    // Extensions that bundle webextension-polyfill will create their own proper
    // browser wrapper that correctly distinguishes sync vs async APIs.
    if (!globalThis.browser) {
        globalThis.browser = globalThis.chrome;
    }
    
    // Ensure browser.runtime.id is set (critical for webextension-polyfill guard check)
    if (globalThis.browser && (!globalThis.browser.runtime || !globalThis.browser.runtime.id)) {
        if (!globalThis.browser.runtime) globalThis.browser.runtime = {};
        globalThis.browser.runtime.id = EXT_ID;
    }
    
    // ===== Fire onInstalled event (once per extension per page) =====
    var installedKey = STORAGE_PREFIX + '__installed__';
    var isFirstInstall = !localStorage.getItem(installedKey);
    localStorage.setItem(installedKey, '1');
    
    setTimeout(function() {
        onInstalledEvent._fire({
            reason: isFirstInstall ? 'install' : 'update',
            previousVersion: MANIFEST.version || '0.0.0'
        });
    }, 0);
    
    // ===== Native Bridge: Message Delivery Functions =====
    if (IS_BACKGROUND) {
        // Background mode: receive messages from content script via native bridge
        window.__WTA_DELIVER_TO_BACKGROUND__ = function(jsonStr) {
            try {
                var data = JSON.parse(jsonStr);
                var msgId = data.msgId;
                var message = data.message;
                var responded = false;
                
                var sendResponse = function(response) {
                    if (responded) return;
                    responded = true;
                    try {
                        if (typeof WtaExtBridge !== 'undefined') {
                            WtaExtBridge.postMessageToContent(JSON.stringify({
                                msgId: msgId,
                                response: response
                            }));
                        }
                    } catch(e) {
                        console.error('[ChromePolyfill BG] sendResponse error:', e);
                    }
                };
                
                var listeners = chrome.runtime.onMessage._listeners.slice();
                for (var i = 0; i < listeners.length; i++) {
                    try {
                        var result = listeners[i](message, {id: EXT_ID, url: '', tab: {id: 1, url: ''}}, sendResponse);
                        if (result === true) {
                            // Async response - listener will call sendResponse later
                            return;
                        }
                        if (result && typeof result.then === 'function') {
                            // Promise-based response
                            result.then(function(resp) {
                                sendResponse(resp !== undefined ? resp : null);
                            }).catch(function(err) {
                                console.error('[ChromePolyfill BG] onMessage handler error:', err);
                                sendResponse(undefined);
                            });
                            return;
                        }
                    } catch(e) {
                        console.error('[ChromePolyfill BG] onMessage listener error:', e);
                    }
                }
                // No async handler responded
                if (!responded) sendResponse(undefined);
            } catch(e) {
                console.error('[ChromePolyfill BG] __WTA_DELIVER_TO_BACKGROUND__ error:', e);
            }
        };
        // Background mode: tabs.sendMessage response handler
        window.__WTA_DELIVER_RESPONSE__ = function(jsonStr) {
            try {
                var data = JSON.parse(jsonStr);
                var msgId = data.msgId;
                var response = data.response;
                if (_pendingResponses[msgId]) {
                    _pendingResponses[msgId](response);
                    delete _pendingResponses[msgId];
                }
            } catch(e) { console.warn('[ChromePolyfill BG] __WTA_DELIVER_RESPONSE__ error:', e); }
        };
    } else {
        // Content mode: receive responses from background script via native bridge
        window.__WTA_DELIVER_RESPONSE__ = function(jsonStr) {
            try {
                var data = JSON.parse(jsonStr);
                var msgId = data.msgId;
                var response = data.response;
                if (_pendingResponses[msgId]) {
                    _pendingResponses[msgId](response);
                    delete _pendingResponses[msgId];
                }
            } catch(e) {
                console.error('[ChromePolyfill] __WTA_DELIVER_RESPONSE__ error:', e);
            }
        };
        
        // Content mode: receive messages from background via tabs.sendMessage
        window.__WTA_DELIVER_TO_CONTENT__ = function(jsonStr) {
            try {
                var data = JSON.parse(jsonStr);
                var msgId = data.msgId;
                var message = data.message;
                var responded = false;
                
                var sendResponse = function(response) {
                    if (responded) return;
                    responded = true;
                    try {
                        if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.postMessageToBackground === 'function') {
                            WtaExtBridge.postMessageToBackground(EXT_ID, JSON.stringify({
                                msgId: msgId,
                                response: response
                            }));
                        }
                    } catch(e) { console.warn('[ChromePolyfill] sendResponse to background error:', e); }
                };
                
                var listeners = chrome.runtime.onMessage._listeners.slice();
                for (var i = 0; i < listeners.length; i++) {
                    try {
                        var result = listeners[i](message, {id: EXT_ID, url: location.href, tab: {id: 1}}, sendResponse);
                        if (result === true) return;
                        if (result && typeof result.then === 'function') {
                            result.then(function(resp) { if (resp !== undefined) sendResponse(resp); })
                                  .catch(function() { sendResponse(undefined); });
                            return;
                        }
                    } catch(e) { console.warn('[ChromePolyfill] onMessage listener error:', e); }
                }
                if (!responded) sendResponse(undefined);
            } catch(e) {
                console.error('[ChromePolyfill] __WTA_DELIVER_TO_CONTENT__ error:', e);
            }
        };
    }
    
    // ===== Phase H: Port Long-Lived Connections =====
    var _ports = {}; // portId -> Port object
    
    // Override chrome.runtime.connect to create real Port objects
    _chrome.runtime.connect = function(extensionId, connectInfo) {
        if (typeof extensionId === 'object') { connectInfo = extensionId; extensionId = EXT_ID; }
        if (!extensionId) extensionId = EXT_ID;
        connectInfo = connectInfo || {};
        var name = connectInfo.name || '';
        
        var portId = '';
        try {
            if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.openPort === 'function') {
                portId = WtaExtBridge.openPort(extensionId, name);
            }
        } catch(e) { console.warn('[ChromePolyfill] openPort error:', e); }
        if (!portId) portId = 'local_port_' + (++_msgCounter);
        
        var port = _createPort(portId, name, IS_BACKGROUND ? 'toContent' : 'toBackground');
        _ports[portId] = port;
        return port;
    };
    
    function _createPort(portId, name, direction) {
        var _onMessage = new ChromeEvent();
        var _onDisconnect = new ChromeEvent();
        var _disconnected = false;
        
        var port = {
            name: name,
            sender: { id: EXT_ID },
            onMessage: _onMessage,
            onDisconnect: _onDisconnect,
            postMessage: function(msg) {
                if (_disconnected) return;
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.portPostMessage === 'function') {
                        WtaExtBridge.portPostMessage(portId, direction, JSON.stringify({
                            portId: portId,
                            message: msg
                        }));
                    }
                } catch(e) {
                    console.error('[ChromePolyfill] port.postMessage error:', e);
                }
            },
            disconnect: function() {
                if (_disconnected) return;
                _disconnected = true;
                try {
                    if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.portDisconnect === 'function') {
                        WtaExtBridge.portDisconnect(portId, direction);
                    }
                } catch(e) { console.warn('[ChromePolyfill] portDisconnect error:', e); }
                _onDisconnect._fire(port);
                delete _ports[portId];
            },
            _portId: portId
        };
        return port;
    }
    
    // Receive port messages from native bridge
    window.__WTA_PORT_MESSAGE__ = function(jsonStr) {
        try {
            var data = JSON.parse(jsonStr);
            
            // New port connection (background receives)
            if (data.event === 'connect' && IS_BACKGROUND) {
                var newPort = _createPort(data.portId, data.name || '', 'toContent');
                _ports[data.portId] = newPort;
                chrome.runtime.onConnect._fire(newPort);
                return;
            }
            
            var portId = data.portId;
            var port = _ports[portId];
            if (port && data.message !== undefined) {
                port.onMessage._fire(data.message, port);
            }
        } catch(e) {
            console.error('[ChromePolyfill] __WTA_PORT_MESSAGE__ error:', e);
        }
    };
    
    window.__WTA_PORT_DISCONNECT__ = function(jsonStr) {
        try {
            var portId = JSON.parse(jsonStr);
            var port = _ports[portId];
            if (port) {
                port.onDisconnect._fire(port);
                delete _ports[portId];
            }
        } catch(e) { console.warn('[ChromePolyfill] __WTA_PORT_DISCONNECT__ error:', e); }
    };
    
    // ===== Phase I: Service Worker Environment Shim (Background mode) =====
    if (IS_BACKGROUND) {
        (function() {
            // Shim self to point to window
            if (typeof self === 'undefined' || self !== window) {
                try { Object.defineProperty(window, 'self', { value: window, writable: true, configurable: true }); } catch(e) { /* expected in some environments */ }
            }
            
            // ExtendableEvent
            function ExtendableEvent(type) {
                this.type = type;
                this._promises = [];
            }
            ExtendableEvent.prototype.waitUntil = function(promise) {
                if (promise && typeof promise.then === 'function') this._promises.push(promise);
            };
            window.ExtendableEvent = ExtendableEvent;
            window.InstallEvent = ExtendableEvent;
            window.ActivateEvent = ExtendableEvent;
            
            // FetchEvent stub
            function FetchEvent(type, init) {
                ExtendableEvent.call(this, type);
                this.request = (init && init.request) || {};
                this._responded = false;
            }
            FetchEvent.prototype = Object.create(ExtendableEvent.prototype);
            FetchEvent.prototype.respondWith = function(response) { this._responded = true; };
            window.FetchEvent = FetchEvent;
            
            // self.addEventListener shim for SW lifecycle events
            var _swListeners = { install: [], activate: [], fetch: [], message: [] };
            var _origAddEventListener = window.addEventListener;
            window.addEventListener = function(type, fn) {
                if (_swListeners[type]) {
                    _swListeners[type].push(fn);
                }
                return _origAddEventListener.apply(window, arguments);
            };
            
            // self.skipWaiting
            window.skipWaiting = function() { return Promise.resolve(); };
            
            // self.clients
            window.clients = {
                matchAll: function(options) {
                    return Promise.resolve([{
                        id: 'main-client',
                        type: 'window',
                        url: location.href,
                        focused: true,
                        postMessage: function(msg) {
                            // Route to content script
                            try {
                                if (typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.postMessageToContent === 'function') {
                                    WtaExtBridge.postMessageToContent(JSON.stringify({ type: 'sw-message', data: msg }));
                                }
                            } catch(e) { console.warn('[ChromePolyfill] clients.postMessage error:', e); }
                        }
                    }]);
                },
                get: function(id) {
                    return window.clients.matchAll().then(function(c) { return c[0] || null; });
                },
                claim: function() { return Promise.resolve(); },
                openWindow: function(url) {
                    try { window.open(url); } catch(e) { console.warn('[ChromePolyfill] clients.openWindow failed:', e); }
                    return Promise.resolve(null);
                }
            };
            
            // self.registration
            window.registration = {
                scope: location.origin + '/',
                active: { state: 'activated', scriptURL: location.href },
                installing: null,
                waiting: null,
                navigationPreload: {
                    enable: function() { return Promise.resolve(); },
                    disable: function() { return Promise.resolve(); },
                    setHeaderValue: function() { return Promise.resolve(); },
                    getState: function() { return Promise.resolve({ enabled: false, headerValue: '' }); }
                },
                showNotification: function(title, options) { return Promise.resolve(); },
                getNotifications: function() { return Promise.resolve([]); },
                update: function() { return Promise.resolve(); },
                unregister: function() { return Promise.resolve(true); },
                onupdatefound: null
            };
            
            // CacheStorage shim
            if (typeof caches === 'undefined') {
                var _cacheStore = {};
                window.caches = {
                    open: function(name) {
                        if (!_cacheStore[name]) _cacheStore[name] = {};
                        var store = _cacheStore[name];
                        return Promise.resolve({
                            match: function(req) { var k = typeof req === 'string' ? req : req.url; return Promise.resolve(store[k] || undefined); },
                            put: function(req, resp) { var k = typeof req === 'string' ? req : req.url; store[k] = resp; return Promise.resolve(); },
                            add: function(req) { return Promise.resolve(); },
                            addAll: function(reqs) { return Promise.resolve(); },
                            delete: function(req) { var k = typeof req === 'string' ? req : req.url; delete store[k]; return Promise.resolve(true); },
                            keys: function() { return Promise.resolve(Object.keys(store).map(function(k) { return new Request(k); })); }
                        });
                    },
                    has: function(name) { return Promise.resolve(!!_cacheStore[name]); },
                    delete: function(name) { delete _cacheStore[name]; return Promise.resolve(true); },
                    keys: function() { return Promise.resolve(Object.keys(_cacheStore)); },
                    match: function(req) { return Promise.resolve(undefined); }
                };
            }
            
            // Fire install + activate events shortly after script loads
            setTimeout(function() {
                var installEvent = new ExtendableEvent('install');
                _swListeners.install.forEach(function(fn) { try { fn(installEvent); } catch(e) { console.warn('[ChromePolyfill] SW install listener error:', e); } });
                
                setTimeout(function() {
                    var activateEvent = new ExtendableEvent('activate');
                    _swListeners.activate.forEach(function(fn) { try { fn(activateEvent); } catch(e) { console.warn('[ChromePolyfill] SW activate listener error:', e); } });
                }, 10);
            }, 0);
            
            console.log('[WebToApp ChromePolyfill] Service Worker shim initialized');
        })();
    }
    
    // ===== Native Fetch Override (Background mode) =====
    // Override fetch() to use native HttpURLConnection, bypassing CORS entirely.
    // @JavascriptInterface methods block the calling JS thread, which is fine for background scripts.
    if (IS_BACKGROUND && typeof WtaExtBridge !== 'undefined' && typeof WtaExtBridge.nativeFetch === 'function') {
        (function() {
            var _origFetch = window.fetch;
            window.fetch = function(input, init) {
                init = init || {};
                var url = typeof input === 'string' ? input : (input && input.url ? input.url : String(input));
                var method = (init.method || (input && input.method) || 'GET').toUpperCase();
                var headers = {};
                try {
                    if (init.headers) {
                        if (typeof init.headers.forEach === 'function') {
                            init.headers.forEach(function(v, k) { headers[k] = v; });
                        } else if (typeof init.headers === 'object') {
                            Object.keys(init.headers).forEach(function(k) { headers[k] = init.headers[k]; });
                        }
                    }
                } catch(e) { console.warn('[ChromePolyfill] fetch headers parse error:', e); }
                var body = init.body ? String(init.body) : '';
                
                try {
                    var resultJson = WtaExtBridge.nativeFetch(url, method, JSON.stringify(headers), body);
                    var result = JSON.parse(resultJson);
                    
                    var respHeaders = new Headers();
                    if (result.headers) {
                        Object.keys(result.headers).forEach(function(k) {
                            try { respHeaders.set(k, result.headers[k]); } catch(e) { /* ignore invalid header */ }
                        });
                    }
                    
                    var response = new Response(result.body || '', {
                        status: result.status || 0,
                        statusText: result.statusText || '',
                        headers: respHeaders
                    });
                    return Promise.resolve(response);
                } catch(e) {
                    console.error('[ChromePolyfill BG] nativeFetch error:', e);
                    // Fallback to original fetch
                    if (typeof _origFetch === 'function') {
                        return _origFetch.call(window, input, init);
                    }
                    return Promise.reject(e);
                }
            };
            console.log('[WebToApp ChromePolyfill] fetch() overridden with native bridge');
        })();
    }
    
    console.log('[WebToApp ChromePolyfill] Initialized for extension: ' + EXT_ID + (IS_BACKGROUND ? ' (BACKGROUND)' : ' (CONTENT)'));
})();
""".trimIndent()
    }
}
