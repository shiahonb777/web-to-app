package com.webtoapp.core.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.webtoapp.core.network.NetworkModule
import org.json.JSONArray
import org.json.JSONObject

/**
 * Greasemonkey / Tampermonkey API 桥接层
 * 
 * 通过 @JavascriptInterface 暴露 Native 方法给 JS，
 * 配合 JS 端的 GM_* polyfill 脚本实现完整的油猴 API。
 * 
 * 注册名: __WTA_GM_BRIDGE__
 */
class GreasemonkeyBridge(
    private val context: Context,
    private val webViewProvider: () -> WebView?
) {
    companion object {
        const val JS_INTERFACE_NAME = "__WTA_GM_BRIDGE__"
        private const val TAG = "GreasemonkeyBridge"
        private const val PREFS_PREFIX = "gm_storage_"
    
    // ==================== JS Polyfill 生成 ====================
    
        /**
         * 生成 GM_* API polyfill JS 代码
         *
         * @param scriptId 脚本唯一 ID（用于存储隔离）
         * @param grants @grant 声明的 API 列表
         * @param scriptInfo 脚本元信息（用于 GM_info）
         * @param resources @resource 资源映射
         */
        fun generatePolyfillScript(
            scriptId: String,
            grants: List<String>,
            scriptInfo: Map<String, String> = emptyMap(),
            resources: Map<String, String> = emptyMap()
        ): String {
            val infoJson = JSONObject(scriptInfo).toString()
            val resourcesJson = JSONObject(resources).toString()
            
            return """
(function() {
    'use strict';
    
    // GM callback registry
    if (!window.__WTA_GM_CALLBACKS__) window.__WTA_GM_CALLBACKS__ = {};
    var _cbId = 0;
    function _nextCbId() { return 'gm_' + (++_cbId) + '_' + Date.now(); }
    
    var _bridge = window.$JS_INTERFACE_NAME;
    var _sid = '$scriptId';
    var _info = $infoJson;
    var _resources = $resourcesJson;
    
    // ===== GM_info =====
    var GM_info = {
        script: {
            name: _info.name || '',
            version: _info.version || '',
            description: _info.description || '',
            author: _info.author || '',
            namespace: _info.namespace || '',
            grants: ${JSONArray(grants)},
            resources: _resources
        },
        scriptHandler: 'WebToApp',
        version: '1.0'
    };
    
    // ===== Storage =====
    function GM_getValue(key, defaultValue) {
        var raw = _bridge.getValue(_sid, key, null);
        if (raw === null || raw === undefined) return defaultValue;
        try { return JSON.parse(raw); } catch(e) { return raw; }
    }
    function GM_setValue(key, value) {
        _bridge.setValue(_sid, key, typeof value === 'string' ? value : JSON.stringify(value));
    }
    function GM_deleteValue(key) { _bridge.deleteValue(_sid, key); }
    function GM_listValues() {
        try { return JSON.parse(_bridge.listValues(_sid)); } catch(e) { return []; }
    }
    
    // ===== Network =====
    function GM_xmlhttpRequest(details) {
        var cbId = _nextCbId();
        window.__WTA_GM_CALLBACKS__[cbId] = {
            onload: details.onload,
            onerror: details.onerror,
            ontimeout: details.ontimeout,
            onprogress: details.onprogress
        };
        _bridge.xmlHttpRequest(cbId, JSON.stringify({
            method: details.method || 'GET',
            url: details.url,
            headers: details.headers || {},
            data: details.data || '',
            responseType: details.responseType || ''
        }));
        return { abort: function() {} };
    }
    
    // ===== Style =====
    function GM_addStyle(css) {
        var s = document.createElement('style');
        s.textContent = css;
        (document.head || document.documentElement).appendChild(s);
        return s;
    }
    
    // ===== Clipboard =====
    function GM_setClipboard(text, type) {
        _bridge.setClipboard(text, type || 'text/plain');
    }
    
    // ===== Tabs =====
    function GM_openInTab(url, options) {
        var bg = (typeof options === 'object') ? !!options.active === false : !!options;
        _bridge.openInTab(url, bg);
    }
    
    // ===== Logging =====
    function GM_log(message) { _bridge.log(String(message)); }
    
    // ===== Notification (simplified) =====
    function GM_notification(details, ondone) {
        var text = typeof details === 'string' ? details : (details.text || details.title || '');
        _bridge.log('[Notification] ' + text);
        if (ondone) setTimeout(ondone, 100);
    }
    
    // ===== Resources =====
    function GM_getResourceText(name) { return _resources[name] || ''; }
    function GM_getResourceURL(name) { return _resources[name] || ''; }
    
    // ===== Menu Commands (integrated with floating window) =====
    var _menuCommands = {};
    function GM_registerMenuCommand(name, fn, accessKey) {
        _menuCommands[name] = fn;
        // Register to floating window if window manager available
        if (window.__WTA_SCRIPT_WINDOWS__) {
            window.__WTA_SCRIPT_WINDOWS__.addMenuButton(_sid, name, fn, {
                windowTitle: _info.name || _sid,
                windowIcon: '\uD83D\uDC35'
            });
        }
        return name;
    }
    function GM_unregisterMenuCommand(name) { delete _menuCommands[name]; }
    
    // ===== Script Window API (WebToApp extension) =====
    function GM_openScriptWindow(html, options) {
        if (!window.__WTA_SCRIPT_WINDOWS__) return null;
        options = options || {};
        var w = window.__WTA_SCRIPT_WINDOWS__.createWindow(_sid, {
            title: options.title || _info.name || _sid,
            icon: options.icon || '\uD83D\uDC35',
            width: options.width || 300,
            height: options.height || 360,
            content: html || '',
            resizable: options.resizable !== false
        });
        return w;
    }
    function GM_updateScriptWindow(html) {
        if (window.__WTA_SCRIPT_WINDOWS__) {
            window.__WTA_SCRIPT_WINDOWS__.updateContent(_sid, html);
        }
    }
    function GM_closeScriptWindow() {
        if (window.__WTA_SCRIPT_WINDOWS__) {
            window.__WTA_SCRIPT_WINDOWS__.closeWindow(_sid);
        }
    }
    
    // ===== GM.* Promise-based API (Tampermonkey 4.x compat) =====
    var GM = {
        info: GM_info,
        getValue: function(k, d) { return Promise.resolve(GM_getValue(k, d)); },
        setValue: function(k, v) { GM_setValue(k, v); return Promise.resolve(); },
        deleteValue: function(k) { GM_deleteValue(k); return Promise.resolve(); },
        listValues: function() { return Promise.resolve(GM_listValues()); },
        xmlHttpRequest: GM_xmlhttpRequest,
        addStyle: GM_addStyle,
        setClipboard: GM_setClipboard,
        openInTab: GM_openInTab,
        notification: GM_notification,
        getResourceText: function(n) { return Promise.resolve(GM_getResourceText(n)); },
        getResourceUrl: function(n) { return Promise.resolve(GM_getResourceURL(n)); },
        registerMenuCommand: GM_registerMenuCommand,
        unregisterMenuCommand: GM_unregisterMenuCommand,
        log: GM_log,
        openScriptWindow: GM_openScriptWindow,
        updateScriptWindow: GM_updateScriptWindow,
        closeScriptWindow: GM_closeScriptWindow
    };
    
    // ===== Expose to global scope =====
    window.GM_info = GM_info;
    window.GM_getValue = GM_getValue;
    window.GM_setValue = GM_setValue;
    window.GM_deleteValue = GM_deleteValue;
    window.GM_listValues = GM_listValues;
    window.GM_xmlhttpRequest = GM_xmlhttpRequest;
    window.GM_addStyle = GM_addStyle;
    window.GM_setClipboard = GM_setClipboard;
    window.GM_openInTab = GM_openInTab;
    window.GM_log = GM_log;
    window.GM_notification = GM_notification;
    window.GM_getResourceText = GM_getResourceText;
    window.GM_getResourceURL = GM_getResourceURL;
    window.GM_registerMenuCommand = GM_registerMenuCommand;
    window.GM_unregisterMenuCommand = GM_unregisterMenuCommand;
    window.GM_openScriptWindow = GM_openScriptWindow;
    window.GM_updateScriptWindow = GM_updateScriptWindow;
    window.GM_closeScriptWindow = GM_closeScriptWindow;
    window.GM = GM;
    
    // unsafeWindow — in WebView there's no sandbox, so it's just window
    window.unsafeWindow = window;
})();
""".trimIndent()
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val httpClient get() = NetworkModule.defaultClient
    
    // ==================== Storage API ====================
    
    private fun getPrefs(scriptId: String) =
        context.getSharedPreferences("$PREFS_PREFIX$scriptId", Context.MODE_PRIVATE)
    
    @JavascriptInterface
    fun getValue(scriptId: String, key: String, defaultValue: String?): String? {
        return getPrefs(scriptId).getString(key, defaultValue)
    }
    
    @JavascriptInterface
    fun setValue(scriptId: String, key: String, value: String) {
        getPrefs(scriptId).edit().putString(key, value).apply()
    }
    
    @JavascriptInterface
    fun deleteValue(scriptId: String, key: String) {
        getPrefs(scriptId).edit().remove(key).apply()
    }
    
    @JavascriptInterface
    fun listValues(scriptId: String): String {
        val keys = getPrefs(scriptId).all.keys
        return JSONArray(keys.toList()).toString()
    }
    
    // ==================== Network API ====================
    
    @JavascriptInterface
    fun xmlHttpRequest(callbackId: String, detailsJson: String) {
        scope.launch {
            try {
                val details = JSONObject(detailsJson)
                val method = details.optString("method", "GET").uppercase()
                val url = details.getString("url")
                val data = details.optString("data", "")
                val headersObj = details.optJSONObject("headers")
                val responseType = details.optString("responseType", "")
                
                val requestBuilder = Request.Builder().url(url)
                
                // Headers
                if (headersObj != null) {
                    val headerMap = mutableMapOf<String, String>()
                    headersObj.keys().forEach { key ->
                        headerMap[key] = headersObj.getString(key)
                    }
                    requestBuilder.headers(headerMap.toHeaders())
                }
                
                // Body
                when (method) {
                    "POST", "PUT", "PATCH" -> {
                        val contentType = headersObj?.optString("Content-Type", "application/x-www-form-urlencoded")
                            ?: "application/x-www-form-urlencoded"
                        requestBuilder.method(method, data.toRequestBody(contentType.toMediaTypeOrNull()))
                    }
                    "DELETE" -> requestBuilder.delete(
                        if (data.isNotEmpty()) data.toRequestBody(null) else null
                    )
                    else -> requestBuilder.get()
                }
                
                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseText = response.body?.string() ?: ""
                val status = response.code
                val statusText = response.message
                
                // 收集响应头
                val responseHeaders = JSONObject()
                response.headers.forEach { (name, value) ->
                    responseHeaders.put(name.lowercase(), value)
                }
                
                val result = JSONObject().apply {
                    put("status", status)
                    put("statusText", statusText)
                    put("responseText", responseText)
                    put("responseHeaders", responseHeaders.toString())
                    put("finalUrl", response.request.url.toString())
                }
                
                callbackToJs(callbackId, "onload", result.toString())
                
            } catch (e: Exception) {
                AppLogger.e(TAG, "GM_xmlhttpRequest failed", e)
                val error = JSONObject().apply {
                    put("error", e.message ?: "Unknown error")
                    put("status", 0)
                }
                callbackToJs(callbackId, "onerror", error.toString())
            }
        }
    }
    
    // ==================== Clipboard API ====================
    
    @JavascriptInterface
    fun setClipboard(text: String, type: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("GM_setClipboard", text)
        clipboard.setPrimaryClip(clip)
        AppLogger.d(TAG, "GM_setClipboard: ${text.take(50)}...")
    }
    
    // ==================== Tab API ====================
    
    @JavascriptInterface
    fun openInTab(url: String, openInBackground: Boolean): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "GM_openInTab failed: $url", e)
            false
        }
    }
    
    // ==================== Log API ====================
    
    @JavascriptInterface
    fun log(message: String) {
        AppLogger.d(TAG, "[GM_log] $message")
    }
    
    // ==================== Internal ====================
    
    private fun callbackToJs(callbackId: String, event: String, dataJson: String) {
        val escapedData = dataJson.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
        val js = """
            (function() {
                var cb = window.__WTA_GM_CALLBACKS__['$callbackId'];
                if (cb && cb['$event']) {
                    try {
                        cb['$event'](JSON.parse('$escapedData'));
                    } catch(e) {
                        console.error('[GM Bridge] Callback error:', e);
                    }
                }
                if ('$event' === 'onload' || '$event' === 'onerror' || '$event' === 'ontimeout') {
                    delete window.__WTA_GM_CALLBACKS__['$callbackId'];
                }
            })();
        """.trimIndent()
        
        MainScope().launch {
            webViewProvider()?.evaluateJavascript(js, null)
        }
    }
    
    fun destroy() {
        scope.cancel()
    }
}
