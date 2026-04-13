package com.webtoapp.core.webview

import android.annotation.SuppressLint
import com.webtoapp.core.logging.AppLogger
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ExtensionPanelScript
import com.webtoapp.core.extension.ModuleRunTime
import com.webtoapp.data.model.NewWindowBehavior
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.engine.shields.ThirdPartyCookiePolicy
import com.webtoapp.core.errorpage.ErrorPageManager
import com.webtoapp.core.errorpage.ErrorPageMode
import com.webtoapp.core.webview.intercept.RequestInterceptionCoordinator
import com.webtoapp.core.webview.intercept.ResourceFallbackLoader
import java.net.URL
import okhttp3.OkHttpClient
import okhttp3.ConnectionSpec
import okhttp3.Request
import okhttp3.Response
import okhttp3.TlsVersion

/**
 * WebView Manager - Configure and manage WebView
 */
class WebViewManager(
    private val context: Context,
    private val adBlocker: AdBlocker
) {
    
    companion object {
        // Desktop Chrome User-Agent — Chrome 版本从系统 WebView 动态获取
        internal var DESKTOP_USER_AGENT: String? = null
        internal const val DESKTOP_USER_AGENT_FALLBACK = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        
        // MIME type lookup map (replaces when-expression for O(1) lookup)
        internal val MIME_TYPE_MAP = mapOf(
            "html" to "text/html", "htm" to "text/html",
            "css" to "text/css", "js" to "application/javascript",
            "json" to "application/json", "xml" to "application/xml",
            "txt" to "text/plain", "png" to "image/png",
            "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
            "gif" to "image/gif", "webp" to "image/webp",
            "svg" to "image/svg+xml", "ico" to "image/x-icon",
            "mp3" to "audio/mpeg", "wav" to "audio/wav",
            "ogg" to "audio/ogg", "mp4" to "video/mp4",
            "webm" to "video/webm", "woff" to "font/woff",
            "woff2" to "font/woff2", "ttf" to "font/ttf",
            "otf" to "font/otf", "eot" to "application/vnd.ms-fontobject"
        )
        
        // Text MIME types for encoding detection
        internal val TEXT_MIME_TYPES = setOf(
            "text/html", "text/css", "text/plain",
            "application/javascript", "application/json",
            "application/xml", "image/svg+xml"
        )
        
        // Desktop UA modes set (avoids listOf per configureWebView call)
        internal val DESKTOP_UA_MODES = setOf(
            UserAgentMode.CHROME_DESKTOP,
            UserAgentMode.SAFARI_DESKTOP,
            UserAgentMode.FIREFOX_DESKTOP,
            UserAgentMode.EDGE_DESKTOP
        )
        
        // Headers to skip when proxying requests
        internal val SKIP_HEADERS = setOf("host", "connection")

        // Local cleartext hosts allowed by network security config
        internal val LOCAL_CLEARTEXT_HOSTS = setOf("localhost", "127.0.0.1", "10.0.2.2")

        // Well-known map tile server host suffixes — these must NEVER be blocked by
        // ad/tracker filters, otherwise Leaflet / Mapbox / Google Maps tile layers break.
        internal val MAP_TILE_HOST_SUFFIXES = setOf(
            "tile.openstreetmap.org",
            "openstreetmap.org",
            "tile.osm.org",
            "tiles.mapbox.com",
            "api.mapbox.com",
            "maps.googleapis.com",
            "maps.gstatic.com",
            "khms.googleapis.com",
            "mt0.google.com", "mt1.google.com", "mt2.google.com", "mt3.google.com",
            "basemaps.cartocdn.com",
            "cartodb-basemaps-a.global.ssl.fastly.net",
            "cartodb-basemaps-b.global.ssl.fastly.net",
            "cartodb-basemaps-c.global.ssl.fastly.net",
            "stamen-tiles.a.ssl.fastly.net",
            "tile.thunderforest.com",
            "server.arcgisonline.com",
            "tiles.stadiamaps.com",
            "cdn.jsdelivr.net",         // Leaflet CDN
            "unpkg.com",                // Leaflet CDN
            "cdnjs.cloudflare.com",     // Leaflet CDN
            "leafletjs.com",
            "leaflet-extras.github.io",
            "nominatim.openstreetmap.org", // OSM geocoding
            "overpass-api.de",            // OSM Overpass API
            "router.project-osrm.org",    // OSRM routing
            "routing.openstreetmap.de",   // OSM routing
            "valhalla.openstreetmap.de"   // Valhalla routing
        )

        // Domains that are sensitive to JS monkey-patching / request interception.
        // Keep runtime modifications minimal for these hosts to avoid blank pages.
        internal val STRICT_COMPAT_HOST_SUFFIXES = setOf(
            "douyin.com",
            "iesdouyin.com",
            "tiktok.com",
            "tiktokv.com",
            "byteoversea.com",
            "byteimg.com"
        )

        // OAuth provider detection is now centralized in OAuthCompatEngine.
        // See OAuthCompatEngine.kt for the full list of 16+ supported providers.

        // Mobile Chrome UA without "; wv" marker for strict anti-WebView sites.
        internal var STRICT_COMPAT_MOBILE_USER_AGENT: String? = null
        internal const val STRICT_COMPAT_MOBILE_UA_FALLBACK =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        // Common multi-part TLD suffixes for basic registrable-domain matching
        internal val COMMON_SECOND_LEVEL_TLDS = setOf(
            "co.uk", "org.uk", "gov.uk", "ac.uk",
            "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn",
            "com.hk", "com.tw",
            "com.au", "net.au", "org.au",
            "co.jp", "co.kr", "co.in", "com.br", "com.mx"
        )

        // Schemes that should never be delegated to external intents
        internal val BLOCKED_SPECIAL_SCHEMES = setOf("javascript", "data", "file", "content", "about")

        /**
         * Viewport Fit-Screen Script — 解决 Unity WebGL / Canvas 游戏放大裁切问题
         *
         * 问题根因：
         * 1. Unity WebGL 等游戏使用固定尺寸 <canvas>（如 960x600），不会响应 viewport 变化
         * 2. Android WebView 默认 DPI 缩放(devicePixelRatio > 1) 导致画布按物理像素渲染，
         *    实际显示比屏幕大，UI 元素被裁切到屏幕外
         *
         * 解决方案：
         * 1. 强制注入 viewport meta: width=device-width, initial-scale=1.0
         * 2. 检测 <canvas> 元素，如果宽度超出 viewport 则用 CSS transform 缩放至适配
         * 3. 同时处理 Unity 的 #unity-container / #unity-canvas 典型容器
         */
        internal const val VIEWPORT_FIT_SCREEN_JS = """(function(){
            'use strict';
            if(window.__wtaViewportFitApplied)return;
            window.__wtaViewportFitApplied=true;
            
            // 1. Force viewport meta tag
            var meta=document.querySelector('meta[name="viewport"]');
            if(!meta){
                meta=document.createElement('meta');
                meta.name='viewport';
                document.head.appendChild(meta);
            }
            meta.content='width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no';
            
            // 2. Detect and scale oversized canvas / content
            function fitContent(){
                var vw=window.innerWidth;
                var vh=window.innerHeight;
                if(!vw||!vh)return;
                
                // Find the main container or canvas
                var targets=[
                    document.getElementById('unity-container'),
                    document.getElementById('unity-canvas'),
                    document.getElementById('gameContainer'),
                    document.getElementById('game-container'),
                    document.getElementById('canvas'),
                    document.querySelector('canvas')
                ];
                
                var body=document.body;
                if(body){
                    var bodyW=body.scrollWidth;
                    var bodyH=body.scrollHeight;
                    // If body is significantly wider than viewport, scale it
                    if(bodyW>vw*1.1){
                        var scale=Math.min(vw/bodyW,vh/bodyH);
                        body.style.transformOrigin='0 0';
                        body.style.transform='scale('+scale+')';
                        body.style.overflow='hidden';
                        body.style.width=(bodyW)+'px';
                        body.style.height=(bodyH)+'px';
                        document.documentElement.style.overflow='hidden';
                        return;
                    }
                }
                
                for(var i=0;i<targets.length;i++){
                    var el=targets[i];
                    if(!el)continue;
                    var w=el.offsetWidth||parseInt(el.style.width)||el.width||0;
                    var h=el.offsetHeight||parseInt(el.style.height)||el.height||0;
                    if(w>vw*1.1||h>vh*1.1){
                        var scaleX=vw/w;
                        var scaleY=vh/h;
                        var s=Math.min(scaleX,scaleY);
                        el.style.transformOrigin='0 0';
                        el.style.transform='scale('+s+')';
                        el.style.position='absolute';
                        el.style.left=((vw-w*s)/2)+'px';
                        el.style.top=((vh-h*s)/2)+'px';
                        // Prevent scroll on parent
                        document.documentElement.style.overflow='hidden';
                        document.body.style.overflow='hidden';
                        break;
                    }
                }
            }
            
            // Run after DOM ready and after a delay (Unity loads async)
            if(document.readyState==='loading'){
                document.addEventListener('DOMContentLoaded',function(){
                    setTimeout(fitContent,300);
                    setTimeout(fitContent,1000);
                    setTimeout(fitContent,3000);
                });
            }else{
                setTimeout(fitContent,300);
                setTimeout(fitContent,1000);
                setTimeout(fitContent,3000);
            }
            
            // Also run on window resize
            window.addEventListener('resize',function(){setTimeout(fitContent,200);});
        })();"""

        /**
         * OkHttp client that allows cleartext (HTTP) connections.
         *
         * Used by the cleartext proxy in shouldInterceptRequest to fetch HTTP resources
         * that are blocked by Android's network security config. This bypasses
         * ERR_CLEARTEXT_NOT_PERMITTED for HTTP sub-resources (e.g., m3u8 video streams).
         *
         * BUILT with MODERN_TLS which supports TLS 1.2+ but includes CLEARTEXT for HTTP.
         * This is intentionally scoped to HTTP-only to minimize security surface.
         */
        internal val cleartextProxyClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(okhttp3.ConnectionPool(4, 30, java.util.concurrent.TimeUnit.SECONDS))
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .connectionSpecs(
                    listOf(
                        okhttp3.ConnectionSpec.Builder(okhttp3.ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                            .build(),
                        // Allow cleartext HTTP — bypasses Android network security config for this client only
                        okhttp3.ConnectionSpec.CLEARTEXT
                    )
                )
                .retryOnConnectionFailure(true)
                .build()
        }

        /**
         * Viewport Custom Script — 自定义视口宽度
         *
         * 通过注入 viewport meta 标签来设置自定义的视口宽度，
         * 允许用户指定 320-3840 像素范围内的任意视口宽度。
         */
        internal const val VIEWPORT_CUSTOM_JS = """(function(){
            'use strict';
            if(window.__wtaViewportCustomApplied)return;
            window.__wtaViewportCustomApplied=true;

            var meta=document.querySelector('meta[name="viewport"]');
            if(!meta){
                meta=document.createElement('meta');
                meta.name='viewport';
                document.head.appendChild(meta);
            }
            meta.content='width=CUSTOM_WIDTH_PLACEHOLDER,initial-scale=1.0,maximum-scale=1.0,user-scalable=no';
        })();"""

        /**
         * Scroll Position Save/Restore Script
         * 
         * 解决 Android WebView goBack() 后滚动位置丢失的问题。
         * 原因: goBack() 会重新加载页面, 加上 onPageStarted 中大量 JS 注入
         * (内核伪装/剪切板/性能优化/用户脚本) 导致 DOM 在浏览器原生滚动恢复
         * 之前就被修改, 滚动位置被重置到顶部。
         * 
         * 方案: 通过 sessionStorage 在页面离开前保存滚动位置,
         * 返回后在 onPageFinished 延迟恢复。
         */
        internal const val SCROLL_SAVE_JS = """(function(){
            'use strict';
            if(window.__wtaScrollSaveInstalled)return;
            window.__wtaScrollSaveInstalled=true;
            var KEY='__wta_scroll_';
            function getKey(){return KEY+location.href;}
            function savePos(){
                try{
                    var y=window.scrollY||window.pageYOffset||document.documentElement.scrollTop||0;
                    if(y>0)sessionStorage.setItem(getKey(),String(y));
                }catch(e){}
            }
            window.addEventListener('pagehide',savePos);
            window.addEventListener('beforeunload',savePos);
            var _t=null;
            window.addEventListener('scroll',function(){
                clearTimeout(_t);
                _t=setTimeout(savePos,500);
            },{passive:true});
        })();"""

        internal const val SCROLL_RESTORE_JS = """(function(){
            'use strict';
            var KEY='__wta_scroll_'+location.href;
            try{
                var saved=sessionStorage.getItem(KEY);
                if(saved){
                    var y=parseInt(saved,10);
                    if(y>0){
                        sessionStorage.removeItem(KEY);
                        function tryRestore(attempts){
                            if(attempts<=0)return;
                            var docH=Math.max(
                                document.body?document.body.scrollHeight:0,
                                document.documentElement?document.documentElement.scrollHeight:0
                            );
                            if(docH>=y+window.innerHeight*0.5){
                                window.scrollTo(0,y);
                            }else{
                                setTimeout(function(){tryRestore(attempts-1);},100);
                            }
                        }
                        setTimeout(function(){tryRestore(10);},150);
                    }
                }
            }catch(e){}
        })();"""
        
        /**
         * 图片加载修复 (Image Repair)
         * 
         * 解决问题：某些网站（动漫、游戏等站点）的图片 CDN 使用 Referer 防盗链，
         * Android WebView 跨域图片请求有时不带 Referer 头，导致 CDN 返回 403，图片显示破碎。
         * 在浏览器中正常加载但 WebView 中失败。
         * 
         * 修复策略：
         * 1. 检测所有加载失败的 <img> 元素
         * 2. 通过 fetch + no-referrer 重新请求图片数据
         * 3. 将响应转为 Object URL 并替换 src
         * 4. 使用 MutationObserver 监听 DOM 变化，修复动态加载的图片
         */
        internal const val IMAGE_REPAIR_JS = """
            (function() {
                'use strict';
                if (window.__wtaImageRepairActive) return;
                window.__wtaImageRepairActive = true;
                
                var repaired = new WeakSet();
                var MAX_RETRIES = 1;
                
                function repairImage(img) {
                    if (!img || !img.src || repaired.has(img)) return;
                    if (img.src.startsWith('data:') || img.src.startsWith('blob:')) return;
                    if (img.naturalWidth > 0 && img.naturalHeight > 0) return;
                    
                    repaired.add(img);
                    var originalSrc = img.src;
                    
                    // 方法1: 设置 referrerPolicy 属性并重新加载
                    img.referrerPolicy = 'no-referrer';
                    img.crossOrigin = 'anonymous';
                    
                    // 方法2: 通过 fetch + no-referrer 获取图片并转为 blob URL
                    fetch(originalSrc, {
                        referrerPolicy: 'no-referrer',
                        mode: 'cors',
                        credentials: 'omit'
                    }).then(function(resp) {
                        if (!resp.ok) throw new Error(resp.status);
                        return resp.blob();
                    }).then(function(blob) {
                        if (blob.size > 0) {
                            var blobUrl = URL.createObjectURL(blob);
                            img.src = blobUrl;
                            // 清理: 图片加载后释放 Object URL
                            img.addEventListener('load', function() {
                                setTimeout(function() { URL.revokeObjectURL(blobUrl); }, 5000);
                            }, { once: true });
                        }
                    }).catch(function() {
                        // fetch 也失败了（CORS 限制），回退: 仅设置 referrerPolicy 后重触发
                        img.src = '';
                        img.referrerPolicy = 'no-referrer';
                        setTimeout(function() { img.src = originalSrc; }, 50);
                    });
                }
                
                function scanBrokenImages() {
                    var images = document.querySelectorAll('img');
                    for (var i = 0; i < images.length; i++) {
                        var img = images[i];
                        // 检测图片是否加载失败: 有 src 但 naturalWidth=0 且已完成加载
                        if (img.src && img.complete && img.naturalWidth === 0 && img.naturalHeight === 0) {
                            repairImage(img);
                        }
                    }
                }
                
                // 为所有现有图片添加 error 监听
                function attachErrorListeners() {
                    var images = document.querySelectorAll('img');
                    for (var i = 0; i < images.length; i++) {
                        (function(img) {
                            if (img.__wtaErrorListening) return;
                            img.__wtaErrorListening = true;
                            img.addEventListener('error', function() {
                                repairImage(img);
                            }, { once: true });
                        })(images[i]);
                    }
                }
                
                // 监听 DOM 变化，修复动态加载的图片（懒加载等）
                var observer = new MutationObserver(function(mutations) {
                    var needsScan = false;
                    for (var m = 0; m < mutations.length; m++) {
                        var nodes = mutations[m].addedNodes;
                        for (var n = 0; n < nodes.length; n++) {
                            var node = nodes[n];
                            if (node.nodeName === 'IMG') {
                                node.addEventListener('error', function() { repairImage(this); }, { once: true });
                                needsScan = true;
                            } else if (node.querySelectorAll) {
                                var imgs = node.querySelectorAll('img');
                                for (var j = 0; j < imgs.length; j++) {
                                    imgs[j].addEventListener('error', function() { repairImage(this); }, { once: true });
                                }
                                if (imgs.length > 0) needsScan = true;
                            }
                        }
                    }
                });
                observer.observe(document.documentElement, { childList: true, subtree: true });
                
                // 初始扫描：延迟检测已加载失败的图片
                attachErrorListeners();
                setTimeout(scanBrokenImages, 1500);
                setTimeout(scanBrokenImages, 5000);
                
                // 自动停止 observer 防止性能问题
                setTimeout(function() { observer.disconnect(); }, 30000);
            })();
        """
        
        // Payment/Social App URL Scheme list
        internal val PAYMENT_SCHEMES = setOf(
            "alipay", "alipays",           // Alipay
            "weixin", "wechat",             // WeChat
            "mqq", "mqqapi", "mqqwpa",      // QQ
            "taobao",                        // Taobao
            "tmall",                         // Tmall
            "jd", "openapp.jdmobile",       // JD.com
            "pinduoduo",                     // Pinduoduo
            "meituan", "imeituan",          // Meituan
            "eleme",                         // Ele.me
            "dianping",                      // Dianping
            "sinaweibo", "weibo",           // Weibo
            "bilibili",                      // Bilibili
            "douyin",                        // Douyin/TikTok
            "snssdk",                        // ByteDance
            "bytedance"                      // ByteDance
        )
        
        /**
         * Clipboard API Polyfill for Android WebView
         * 
         * Android WebView 不实现 navigator.clipboard API（readText/writeText 始终返回
         * NotAllowedError），导致网页的粘贴功能完全失效。
         * 
         * 这个 polyfill 通过 NativeBridge 桥接到 Android 原生 ClipboardManager，
         * 让网页可以正常使用 navigator.clipboard.readText() 和 writeText()。
         * 
         * 同时修补 navigator.permissions.query() 让 clipboard-read/clipboard-write
         * 始终返回 'granted'，与 Chrome 行为一致。
         */
        internal const val CLIPBOARD_POLYFILL_JS = """
            (function() {
                'use strict';
                if (typeof window.NativeBridge === 'undefined') return;
                
                // 1. Override navigator.clipboard
                var clipboardProxy = {
                    writeText: function(text) {
                        return new Promise(function(resolve, reject) {
                            try {
                                var ok = window.NativeBridge.copyToClipboard(text);
                                if (ok) resolve(); else reject(new DOMException('Failed to copy', 'NotAllowedError'));
                            } catch(e) { reject(e); }
                        });
                    },
                    readText: function() {
                        return new Promise(function(resolve, reject) {
                            try {
                                var text = window.NativeBridge.getClipboardText();
                                resolve(text || '');
                            } catch(e) { reject(e); }
                        });
                    },
                    write: function(data) {
                        return new Promise(function(resolve, reject) {
                            try {
                                if (data && data.length > 0) {
                                    data[0].getType('text/plain').then(function(blob) {
                                        var reader = new FileReader();
                                        reader.onload = function() {
                                            window.NativeBridge.copyToClipboard(reader.result);
                                            resolve();
                                        };
                                        reader.readAsText(blob);
                                    }).catch(function() { resolve(); });
                                } else { resolve(); }
                            } catch(e) { resolve(); }
                        });
                    },
                    read: function() {
                        return new Promise(function(resolve, reject) {
                            try {
                                var text = window.NativeBridge.getClipboardText() || '';
                                var blob = new Blob([text], {type: 'text/plain'});
                                resolve([new ClipboardItem({'text/plain': blob})]);
                            } catch(e) { reject(e); }
                        });
                    }
                };
                
                try {
                    Object.defineProperty(navigator, 'clipboard', {
                        value: clipboardProxy,
                        writable: false,
                        configurable: true
                    });
                } catch(e) {
                    navigator.clipboard = clipboardProxy;
                }
                
                // 2. Patch Permissions API for clipboard
                if (navigator.permissions && navigator.permissions.query) {
                    var origQuery = navigator.permissions.query.bind(navigator.permissions);
                    navigator.permissions.query = function(desc) {
                        if (desc && (desc.name === 'clipboard-read' || desc.name === 'clipboard-write')) {
                            return Promise.resolve({
                                state: 'granted',
                                status: 'granted',
                                onchange: null,
                                addEventListener: function(){},
                                removeEventListener: function(){}
                            });
                        }
                        return origQuery(desc);
                    };
                }
            })();
        """
    }
    
    private val sessionState = WebViewSessionState()

    private var appExtensionModuleIds: List<String>
        get() = sessionState.appExtensionModuleIds
        set(value) {
            sessionState.appExtensionModuleIds = value
        }

    private var embeddedModules: List<com.webtoapp.core.shell.EmbeddedShellModule>
        get() = sessionState.embeddedModules
        set(value) {
            sessionState.embeddedModules = value
        }

    private var allowGlobalModuleFallback: Boolean
        get() = sessionState.allowGlobalModuleFallback
        set(value) {
            sessionState.allowGlobalModuleFallback = value
        }

    private var extensionFabIcon: String
        get() = sessionState.extensionFabIcon
        set(value) {
            sessionState.extensionFabIcon = value
        }

    private var gmBridge: com.webtoapp.core.extension.GreasemonkeyBridge?
        get() = sessionState.gmBridge
        set(value) {
            sessionState.gmBridge = value
        }

    private val extensionRuntimes: MutableMap<String, com.webtoapp.core.extension.ChromeExtensionRuntime>
        get() = sessionState.extensionRuntimes
    
    // File manager for @require/@resource cache access
    private val extensionFileManager by lazy {
        com.webtoapp.core.extension.ExtensionFileManager(context)
    }
    
    // Track configured WebViews for resource cleanup
    private val managedWebViews
        get() = sessionState.managedWebViews
    
    // Browser Shields — privacy protection manager
    private lateinit var shields: BrowserShields
    
    // Error page manager — custom error page generation
    private var errorPageManager: ErrorPageManager? = null
    private var lastFailedUrl: String?
        get() = sessionState.lastFailedUrl
        set(value) {
            sessionState.lastFailedUrl = value
        }
    
    // file:// retry counter — auto-retry when file not yet extracted (race condition)
    private var fileRetryCount: Int
        get() = sessionState.fileRetryCount
        set(value) {
            sessionState.fileRetryCount = value
        }
    private var fileRetryUrl: String?
        get() = sessionState.fileRetryUrl
        set(value) {
            sessionState.fileRetryUrl = value
        }
    private val FILE_MAX_RETRIES = 3
    private val FILE_RETRY_DELAY_MS = 500L
    
    // Main-frame URL cache (must be thread-safe for shouldInterceptRequest background thread)
    private var currentMainFrameUrl: String?
        get() = sessionState.currentMainFrameUrl
        set(value) {
            sessionState.currentMainFrameUrl = value
        }
    
    // Cookie flush 防抖 — 避免快速导航时每次 onPageFinished 都同步写磁盘
    private val cookieFlushRunnable = Runnable {
        try { CookieManager.getInstance().flush() } catch (_: Exception) {}
    }

    private val urlPolicy = WebViewUrlPolicy()
    private val userAgentResolver = UserAgentResolver(context)
    private val settingsConfigurator = WebViewSettingsConfigurator()
    private val resourceFallbackLoader = ResourceFallbackLoader(context)
    private val requestInterceptionCoordinator = RequestInterceptionCoordinator(
        context = context,
        adBlocker = adBlocker,
        urlPolicy = urlPolicy,
        resourceFallbackLoader = resourceFallbackLoader
    )
    private val strictHostRuntimePolicy = StrictHostRuntimePolicy(context, urlPolicy)
    private val specialUrlHandler = SpecialUrlHandler(context, urlPolicy)
    private val webViewLifecycleCleaner = WebViewLifecycleCleaner(sessionState)
    private val extensionRuntimeCoordinator by lazy {
        ExtensionRuntimeCoordinator(
            context = context,
            extensionFileManager = extensionFileManager,
            state = sessionState
        )
    }
    private val scriptInjectionCoordinator by lazy {
        ScriptInjectionCoordinator(
            context = context,
            getCurrentConfig = { currentConfig },
            buildPanelInitScripts = extensionRuntimeCoordinator::buildPanelInitScripts,
            shouldUseConservativeScriptMode = strictHostRuntimePolicy::shouldUseConservativeScriptMode,
            shouldUseScriptlessMode = strictHostRuntimePolicy::shouldUseScriptlessMode,
            injectCompatibilityScripts = ::injectCompatibilityScripts,
            injectAllExtensionModules = extensionRuntimeCoordinator::injectAllExtensionModules
        )
    }
    
    /**
     * 从系统 WebView 默认 UA 中提取 Chrome 版本号，保持 UA 与设备一致。
     * 避免硬编码过时的 Chrome/120 被网站检测为旧浏览器。
     */
    private fun ensureDynamicUserAgents() {
        val resolved = userAgentResolver.ensureDynamicUserAgents(
            desktopUserAgent = DESKTOP_USER_AGENT,
            strictCompatMobileUserAgent = STRICT_COMPAT_MOBILE_USER_AGENT
        )
        DESKTOP_USER_AGENT = resolved.desktopUserAgent
        STRICT_COMPAT_MOBILE_USER_AGENT = resolved.strictCompatMobileUserAgent
    }
    
    /**
     * Resolve active modules for the current app context.
     *
     * All module types (including Chrome extensions) are controlled per-app:
     * - If per-app module IDs are configured, returns those modules.
     * - Otherwise, falls back to globally enabled modules (if allowed).
     *
     * Users select which modules (including browser extensions) to use
     * in the app editor's Extension Module feature.
     */
    private fun getActiveModulesForCurrentApp(): List<com.webtoapp.core.extension.ExtensionModule> {
        return extensionRuntimeCoordinator.getActiveModulesForCurrentApp()
    }

    /**
     * Configure WebView
     * @param webView WebView instance
     * @param config WebView configuration
     * @param callbacks Callback interface
     * @param extensionModuleIds App configured extension module ID list (optional)
     * @param embeddedExtensionModules Embedded extension module data (for Shell mode, optional)
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(
        webView: WebView,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        extensionModuleIds: List<String> = emptyList(),
        embeddedExtensionModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList(),
        extensionFabIcon: String = "",
        allowGlobalModuleFallback: Boolean = false,
        browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null,
        deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null
    ) {
        // Initialize dynamic User-Agent strings from system WebView
        ensureDynamicUserAgents()
        // Save config reference
        this.currentConfig = config
        // Pre-generate Browser Disguise JS (cached for all page loads)
        this.cachedBrowserDisguiseConfig = browserDisguiseConfig
        this.cachedBrowserDisguiseJs = if (browserDisguiseConfig?.enabled == true) {
            com.webtoapp.core.disguise.BrowserDisguiseJsGenerator.generate(browserDisguiseConfig).also { js ->
                val coverage = com.webtoapp.core.disguise.BrowserDisguiseConfig.calculateCoverage(browserDisguiseConfig)
                AppLogger.d("WebViewManager", "Browser Disguise JS cached: ${js.length} chars, coverage=${"%,.0f".format(coverage * 100)}%")
            }
        } else null
        // Save extension module ID list
        this.appExtensionModuleIds = extensionModuleIds
        // Save embedded module data
        this.embeddedModules = embeddedExtensionModules
        this.allowGlobalModuleFallback = allowGlobalModuleFallback
        // Save custom FAB icon
        this.extensionFabIcon = extensionFabIcon
        // Save device disguise config
        this.currentDeviceDisguiseConfig = deviceDisguiseConfig
        
        // Initialize Browser Shields
        shields = BrowserShields.getInstance(context)
        
        // Initialize Error Page Manager
        if (config.errorPageConfig.mode != ErrorPageMode.DEFAULT) {
            // 始终注入当前语言到错误页配置，确保跟随用户语言设置
            val errorConfig = config.errorPageConfig.copy(
                language = com.webtoapp.core.i18n.Strings.currentLanguage.value.name
            )
            errorPageManager = ErrorPageManager(errorConfig)
        }
        
        // Debug log：Confirm extension module config
        AppLogger.d("WebViewManager", "configureWebView: extensionModuleIds=${extensionModuleIds.size}, embeddedModules=${embeddedExtensionModules.size}")
        embeddedExtensionModules.forEach { module ->
            AppLogger.d("WebViewManager", "  Embedded module: id=${module.id}, name=${module.name}, enabled=${module.enabled}, runAt=${module.runAt}")
        }
        
        // Track this WebView
        managedWebViews[webView] = true
        
        // ============ Cookie 持久化配置 ============
        // Enable cookies and third-party cookies for login persistence
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        // Shields: Apply third-party cookie policy
        // When disableShields is true (per-app setting), allow all third-party cookies
        val shieldsActive = shields.isEnabled() && !config.disableShields
        val cookiePolicy = shields.getConfig().thirdPartyCookiePolicy
        cookieManager.setAcceptThirdPartyCookies(
            webView,
            !shieldsActive || cookiePolicy == ThirdPartyCookiePolicy.ALLOW_ALL
        )
        // Ensure cookies are persisted to disk
        cookieManager.flush()
        AppLogger.d("WebViewManager", "Cookie persistence enabled (disableShields=${config.disableShields})")

        val isDesktopModeRequested = userAgentResolver.isDesktopUaRequested(config, currentDeviceDisguiseConfig)
        // Landscape apps should keep a native-sized viewport instead of overview shrink-fit.
        // This avoids "zoomed-out letterbox" rendering in wide screens.
        val preferLandscapeEmbeddedViewport = config.landscapeMode && !isDesktopModeRequested
        val effectiveUserAgent = resolveUserAgent(config)
        val hasActiveChromeExt = getActiveModulesForCurrentApp().any { module ->
            module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.chromeExtId.isNotEmpty()
        }
        
        webView.apply {
            settingsConfigurator.apply(
                webView = this,
                config = config,
                effectiveUserAgent = effectiveUserAgent,
                isDesktopModeRequested = isDesktopModeRequested,
                preferLandscapeEmbeddedViewport = preferLandscapeEmbeddedViewport,
                hasActiveChromeExtension = hasActiveChromeExt,
                desktopUserAgent = DESKTOP_USER_AGENT ?: DESKTOP_USER_AGENT_FALLBACK
            )

            // WebViewClient
            webViewClient = createWebViewClient(config, callbacks)

            // WebChromeClient
            webChromeClient = createWebChromeClient(config, callbacks)
            
            // Download listener
            if (config.downloadEnabled) {
                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    callbacks.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
                }
            }
            
            // Inject JavaScript bridge (navigator.share, etc.)
            if (config.enableShareBridge) {
                addJavascriptInterface(ShareBridge(context), "NativeShareBridge")
            }
            
            // Register OAuth block detection bridge — lets the safety-net JS
            // notify Kotlin when Google renders the "not secure" error page client-side
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onOAuthBlocked(url: String) {
                    AppLogger.w("WebViewManager", "OAuth block detected via JS bridge — redirecting to CCT: $url")
                    webView.post {
                        webView.stopLoading()
                        if (webView.canGoBack()) webView.goBack()
                        openInCustomTab(url)
                    }
                }
            }, "NativeOAuthBridge")
            
            // Register Greasemonkey API bridge for userscript support
            gmBridge?.destroy()
            val bridge = com.webtoapp.core.extension.GreasemonkeyBridge(context) { webView }
            gmBridge = bridge
            addJavascriptInterface(bridge, com.webtoapp.core.extension.GreasemonkeyBridge.JS_INTERFACE_NAME)

            // Initialize Chrome Extension background script runtimes
            extensionRuntimeCoordinator.initChromeExtensionRuntimes(webView)

            // 浏览器内核伪装 — 在 UA 设置完成后清洗, 移除 wv/Version 标识
            com.webtoapp.core.kernel.BrowserKernel.configureWebView(webView)
        }
    }
    
    /**
     * Parse User-Agent config
     * Priority: userAgentMode > desktopMode (backward compatible) > userAgent (legacy field)
     * @return Effective User-Agent string, or null if using system default
     */
    private fun resolveUserAgent(config: WebViewConfig): String? {
        return userAgentResolver.resolveUserAgent(
            config = config,
            deviceDisguiseConfig = currentDeviceDisguiseConfig,
            desktopUserAgent = DESKTOP_USER_AGENT ?: DESKTOP_USER_AGENT_FALLBACK
        )
    }

    /**
     * Create WebViewClient
     */
    private fun createWebViewClient(
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ): WebViewClient {
        val diag = sessionState.diagnostics
        
        return object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return handleShouldInterceptRequest(request, super.shouldInterceptRequest(view, request), config, diag)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return handleShouldOverrideUrlLoading(view, request, config, callbacks)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                handlePageStarted(view, url, config, callbacks, diag)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                handlePageCommitVisible(url, callbacks, diag)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                // SPA navigation (pushState/replaceState) triggers this but NOT onPageFinished.
                // Notify callback so canGoBack/canGoForward state updates in real time.
                callbacks.onUrlChanged(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                handlePageFinished(view, url, config, callbacks, diag)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                handleReceivedError(view, request, error, callbacks, diag)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                handleReceivedHttpError(view, request, errorResponse, callbacks)
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                handleReceivedSslError(view, handler, error, config, callbacks)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                return handleRenderProcessGone(view, detail, callbacks)
            }
        }
    }

    private fun handleShouldInterceptRequest(
        request: WebResourceRequest?,
        fallbackResponse: WebResourceResponse?,
        config: WebViewConfig,
        diag: WebViewLoadDiagnostics
    ): WebResourceResponse? {
        val webRequest = request ?: return fallbackResponse
        diag.requestCount++
        val result = requestInterceptionCoordinator.intercept(
            request = webRequest,
            config = config,
            currentMainFrameUrl = currentMainFrameUrl,
            shields = if (::shields.isInitialized) shields else null,
            diag = RequestInterceptionCoordinator.DiagSnapshot(
                requestCount = diag.requestCount,
                blockedCount = diag.blockedCount,
                errorCount = diag.errorCount,
                pageStartTime = diag.pageStartTime
            ),
            shouldBypassAggressiveNetworkHooks = { candidateRequest, requestUrl ->
                strictHostRuntimePolicy.shouldBypassAggressiveNetworkHooks(
                    request = candidateRequest,
                    requestUrl = requestUrl,
                    currentMainFrameUrl = currentMainFrameUrl
                )
            }
        )
        if (result.blocked) {
            diag.blockedCount++
        }
        return result.response ?: fallbackResponse
    }

    private fun handleShouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        val isUserGesture = request.hasGesture()
        if (request.isForMainFrame) {
            AppLogger.d("WebViewManager", "Main-frame navigation request: $url")
        }

        if (request.isForMainFrame && OAuthCompatEngine.shouldRedirectToCustomTab(url)) {
            val provider = OAuthCompatEngine.getProviderType(url)
            AppLogger.i("WebViewManager", "Google OAuth detected [$provider] — redirecting to Chrome Custom Tab: $url")
            view?.stopLoading()
            specialUrlHandler.openInCustomTab(url)
            return true
        }

        if (request.isForMainFrame && OAuthCompatEngine.isOAuthUrl(url)) {
            val provider = OAuthCompatEngine.getProviderType(url)
            AppLogger.d("WebViewManager", "OAuth detected [$provider] — allowing in-WebView with kernel disguise: $url")
        }

        val isSameOriginHttp = run {
            val currentUrl = currentMainFrameUrl
            if (currentUrl != null && currentUrl.startsWith("http://", ignoreCase = true)) {
                val currentHost = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull()
                val targetHost = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull()
                currentHost != null && targetHost != null && currentHost == targetHost
            } else {
                false
            }
        }
        if (!isSameOriginHttp && !config.disableShields) {
            val secureUrl = upgradeInsecureHttpUrl(url)
            if (secureUrl != null) {
                view?.loadUrl(secureUrl)
                AppLogger.d("WebViewManager", "Auto-upgraded insecure HTTP navigation: $url -> $secureUrl")
                return true
            }
        }

        if (!config.disableShields && ::shields.isInitialized && shields.isEnabled() && shields.getConfig().httpsUpgrade) {
            val upgradedUrl = shields.httpsUpgrader.tryUpgrade(url)
            if (upgradedUrl != null) {
                shields.stats.recordHttpsUpgrade()
                view?.loadUrl(upgradedUrl)
                return true
            }
        }

        if (specialUrlHandler.handleSpecialUrl(
                url = url,
                isUserGesture = isUserGesture,
                currentMainFrameUrl = currentMainFrameUrl,
                currentConfig = currentConfig,
                managedWebViews = managedWebViews.keys,
                shouldUseScriptlessMode = strictHostRuntimePolicy::shouldUseScriptlessMode
            )) {
            return true
        }

        if (config.openExternalLinks && isExternalUrl(url, view?.url)) {
            callbacks.onExternalLink(url)
            return true
        }

        return false
    }

    private fun handlePageStarted(
        view: WebView?,
        url: String?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics
    ) {
        currentMainFrameUrl = url
        diag.pageStartTime = System.currentTimeMillis()
        diag.requestCount = 0
        diag.blockedCount = 0
        diag.errorCount = 0
        android.util.Log.w("DIAG", "═══ PAGE_STARTED ═══ url=$url")
        android.util.Log.w("DIAG", "  config: disableShields=${config.disableShields} adBlockEnabled=${adBlocker.isEnabled()} crossOriginIsolation=${config.enableCrossOriginIsolation}")
        android.util.Log.w("DIAG", "  shields: initialized=${::shields.isInitialized} enabled=${if (::shields.isInitialized) shields.isEnabled().toString() else "N/A"}")
        if (::shields.isInitialized && shields.isEnabled()) {
            android.util.Log.w("DIAG", "  shields config: trackerBlocking=${shields.getConfig().trackerBlocking} httpsUpgrade=${shields.getConfig().httpsUpgrade}")
        }

        if (url != null) {
            OAuthCompatEngine.getAntiDetectionJs(url)?.let { js ->
                val provider = OAuthCompatEngine.getProviderType(url)
                AppLogger.d("WebViewManager", "Injecting OAuth anti-detection JS [$provider] for: $url")
                view?.evaluateJavascript(js, null)
            }
        }

        if (view != null) {
            strictHostRuntimePolicy.applyStrictHostRuntimePolicy(
                webView = view,
                pageUrl = url,
                currentConfig = currentConfig,
                currentDeviceDisguiseConfig = currentDeviceDisguiseConfig
            )
        }
        callbacks.onPageStarted(url)
        lastFailedUrl = null
        if (url != null && url != fileRetryUrl) {
            fileRetryCount = 0
            fileRetryUrl = null
        }
        if (::shields.isInitialized) shields.onPageStarted(url)
        adBlocker.invalidateCache()
        view?.let { webView ->
            com.webtoapp.core.disguise.BrowserDisguiseEngine.injectOnPageStarted(
                webView = webView,
                url = url,
                disguiseConfig = cachedBrowserDisguiseConfig,
                cachedDisguiseJs = cachedBrowserDisguiseJs,
                enableDiagnostic = false
            )
            scriptInjectionCoordinator.handlePageStarted(webView, url, config)
        }
    }

    private fun handlePageCommitVisible(
        url: String?,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics
    ) {
        currentMainFrameUrl = url ?: currentMainFrameUrl
        val elapsed = System.currentTimeMillis() - diag.pageStartTime
        android.util.Log.w("DIAG", "═══ PAGE_COMMIT_VISIBLE ═══ +${elapsed}ms requests=${diag.requestCount} blocked=${diag.blockedCount} url=$url")
        callbacks.onPageCommitVisible(url)
    }

    private fun handlePageFinished(
        view: WebView?,
        url: String?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics
    ) {
        currentMainFrameUrl = url ?: currentMainFrameUrl
        val elapsed = System.currentTimeMillis() - diag.pageStartTime
        android.util.Log.w("DIAG", "═══ PAGE_FINISHED ═══ +${elapsed}ms requests=${diag.requestCount} blocked=${diag.blockedCount} errors=${diag.errorCount} url=$url")
        if (url != null && url.startsWith("file://")) {
            fileRetryCount = 0
            fileRetryUrl = null
        }
        view?.let { webView ->
            scriptInjectionCoordinator.handlePageFinished(
                webView = webView,
                url = url,
                config = config,
                cookieFlushRunnable = cookieFlushRunnable,
                extractHostFromUrl = urlPolicy::extractHostFromUrl,
                adBlockerCssProvider = adBlocker::getCosmeticFilterCss
            )
        }
        callbacks.onPageFinished(url)
        if (::shields.isInitialized) shields.onPageFinished(url)
    }

    private fun handleReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
        callbacks: WebViewCallbacks,
        diag: WebViewLoadDiagnostics
    ) {
        val errUrl = request?.url?.toString() ?: "unknown"
        val errCode = error?.errorCode ?: -1
        val errDesc = error?.description?.toString() ?: "unknown"
        val isMain = request?.isForMainFrame == true
        diag.errorCount++
        android.util.Log.w("DIAG", "RECV_ERROR [${if (isMain) "MAIN" else "sub"}] code=$errCode desc=$errDesc url=${errUrl.take(120)}")
        if (request?.isForMainFrame != true) return

        val errorCode = error?.errorCode ?: -1
        val rawDescription = error?.description?.toString() ?: "Unknown error"
        val description = normalizeNetworkErrorDescription(rawDescription)
        val failedUrl = request.url?.toString()
        if (failedUrl == null || failedUrl == "about:blank") return

        if (view != null) {
            val upgradedUrl = upgradeInsecureHttpUrl(failedUrl)
            if (upgradedUrl != null && isCleartextBlockedError(errorCode, rawDescription, description)) {
                AppLogger.d("WebViewManager", "Auto-recover from cleartext block: $failedUrl -> $upgradedUrl")
                view.loadUrl(upgradedUrl)
                return
            }
        }

        if (view != null && failedUrl.startsWith("file://")) {
            val isSameRetry = failedUrl == fileRetryUrl
            val currentRetry = if (isSameRetry) fileRetryCount else 0
            if (currentRetry < FILE_MAX_RETRIES) {
                fileRetryUrl = failedUrl
                fileRetryCount = currentRetry + 1
                AppLogger.d(
                    "WebViewManager",
                    "file:// load failed (code=$errorCode, desc=$rawDescription), auto-retry ${fileRetryCount}/$FILE_MAX_RETRIES after ${FILE_RETRY_DELAY_MS}ms: $failedUrl"
                )
                view.postDelayed({ view.loadUrl(failedUrl) }, FILE_RETRY_DELAY_MS)
                return
            } else {
                AppLogger.w("WebViewManager", "file:// load failed after $FILE_MAX_RETRIES retries: $failedUrl")
                fileRetryCount = 0
                fileRetryUrl = null
            }
        }

        val manager = errorPageManager
        if (manager != null && view != null) {
            val errorHtml = manager.generateErrorPage(errorCode, description, failedUrl)
            if (errorHtml != null) {
                lastFailedUrl = failedUrl
                view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", failedUrl)
                AppLogger.d("WebViewManager", "Custom error page loaded for: $failedUrl")
                callbacks.onError(errorCode, description)
                return
            }
        }

        callbacks.onError(errorCode, description)
    }

    private fun handleReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
        callbacks: WebViewCallbacks
    ) {
        if (request?.isForMainFrame != true) return

        val statusCode = errorResponse?.statusCode ?: -1
        val reason = errorResponse?.reasonPhrase?.takeIf { it.isNotBlank() } ?: "HTTP Error"
        val failedUrl = request.url?.toString()
        val description = if (statusCode > 0) "HTTP $statusCode $reason" else reason
        AppLogger.w("WebViewManager", "Main-frame HTTP error: url=$failedUrl code=$statusCode reason=$reason")

        if (failedUrl != null && OAuthCompatEngine.isOAuthBlockedError(statusCode, failedUrl)) {
            val provider = OAuthCompatEngine.getProviderType(failedUrl)
            AppLogger.w("WebViewManager", "OAuth [$provider] $statusCode detected — kernel disguise insufficient, falling back to system browser: $failedUrl")
            view?.stopLoading()
            if (view?.canGoBack() == true) {
                view.goBack()
            }
            specialUrlHandler.openInSystemBrowser(failedUrl)
            return
        }

        val manager = errorPageManager
        if (manager != null && view != null && failedUrl != null && failedUrl != "about:blank") {
            val errorHtml = manager.generateErrorPage(statusCode, description, failedUrl)
            if (errorHtml != null) {
                lastFailedUrl = failedUrl
                view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", failedUrl)
                AppLogger.d("WebViewManager", "Custom HTTP error page loaded for: $failedUrl, code=$statusCode")
                callbacks.onError(statusCode, description)
                return
            }
        }

        callbacks.onError(statusCode, description)
    }

    private fun handleReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: android.net.http.SslError?,
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ) {
        if (!config.disableShields && ::shields.isInitialized && shields.isEnabled()) {
            when (shields.getConfig().sslErrorPolicy) {
                com.webtoapp.core.engine.shields.SslErrorPolicy.AUTO_HTTP_FALLBACK -> {
                    var fallbackUrl = shields.httpsUpgrader.onSslError(error?.url)
                    if (fallbackUrl != null) {
                        handler?.cancel()
                        view?.loadUrl(fallbackUrl)
                        AppLogger.d("WebViewManager", "HTTPS upgrade fallback: $fallbackUrl")
                        return
                    }
                    fallbackUrl = shields.httpsUpgrader.tryHttpFallback(error?.url)
                    if (fallbackUrl != null) {
                        handler?.cancel()
                        view?.loadUrl(fallbackUrl)
                        AppLogger.d("WebViewManager", "SSL error fallback to HTTP: $fallbackUrl")
                        return
                    }
                    handler?.cancel()
                    callbacks.onSslError(error?.toString() ?: "SSL Error")
                    return
                }

                com.webtoapp.core.engine.shields.SslErrorPolicy.ASK_USER,
                com.webtoapp.core.engine.shields.SslErrorPolicy.BLOCK -> {
                    handler?.cancel()
                    callbacks.onSslError(error?.toString() ?: "SSL Error")
                    return
                }
            }
        }

        handler?.cancel()
        callbacks.onSslError(error?.toString() ?: "SSL Error")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?,
        callbacks: WebViewCallbacks
    ): Boolean {
        val didCrash = detail?.didCrash() == true
        val reason = if (didCrash) {
            "WebView render process crashed"
        } else {
            "WebView render process was killed"
        }
        AppLogger.e("WebViewManager", "$reason, rendererPriority=${detail?.rendererPriorityAtExit()}")
        view?.let(webViewLifecycleCleaner::destroyWebView)
        callbacks.onError(
            -1003,
            if (didCrash) {
                "WebView render process crashed. Please reopen the page."
            } else {
                "WebView render process was killed due to memory pressure. Please reopen the page."
            }
        )
        callbacks.onRenderProcessGone(didCrash)
        return true
    }

    private fun normalizeNetworkErrorDescription(rawDescription: String): String {
        val normalized = rawDescription.uppercase()
        if (normalized.contains("CLEARTEXT") || normalized.contains("ERR_CLEARTEXT_NOT_PERMITTED")) {
            return "Cleartext HTTP is blocked by security policy. Please use HTTPS."
        }
        return rawDescription
    }

    private fun isCleartextBlockedError(errorCode: Int, rawDescription: String, normalizedDescription: String): Boolean {
        if (errorCode == WebViewClient.ERROR_UNSAFE_RESOURCE) return true
        val merged = "$rawDescription $normalizedDescription".uppercase()
        return merged.contains("CLEARTEXT") ||
            merged.contains("ERR_CLEARTEXT_NOT_PERMITTED") ||
            merged.contains("SECURITY POLICY")
    }

    /**
     * For remote sites, prefer conservative compatibility/shields JS injection.
     * These pages are often sensitive to prototype monkey-patching.
     */
    private fun shouldUseConservativeScriptMode(pageUrl: String?): Boolean {
        return strictHostRuntimePolicy.shouldUseConservativeScriptMode(pageUrl)
    }

    /**
     * Strict mode for high-friction anti-automation sites.
     * In this mode we disable aggressive JS/runtime/network hooks.
     */
    private fun shouldUseScriptlessMode(pageUrl: String?): Boolean {
        return strictHostRuntimePolicy.shouldUseScriptlessMode(pageUrl)
    }

    /**
     * Open a URL in the system browser (Chrome, default browser, etc.).
     * Used for OAuth flows that block embedded WebViews.
     */
    private fun openInSystemBrowser(url: String) {
        specialUrlHandler.openInSystemBrowser(url)
    }

    /**
     * Open a URL in Chrome Custom Tab.
     *
     * Chrome Custom Tab (CCT) uses a real Chrome renderer process, so Google's
     * server-side WebView detection does NOT trigger. CCT and WebView share the
     * same cookie jar, which means:
     * 1. User logs in via CCT → session cookie is set
     * 2. CCT closes → WebView resumes the original page
     * 3. WebView reloads → session cookie is present → user is logged in
     *
     * Fallback: If Chrome is not installed, falls back to system browser.
     */
    private fun openInCustomTab(url: String) {
        specialUrlHandler.openInCustomTab(url)
    }

    /**
     * C 级 URL host 提取 (零分配)
     * shouldInterceptRequest 每次子资源请求调用 ~3 次
     * 替换 Uri.parse(url).host 避免创建 URI 对象 + GC 压力
     */
    private fun extractHostFromUrl(url: String?): String? {
        val target = url?.takeIf { it.isNotBlank() } ?: return null
        // C 级零分配提取 → 回退到 Uri.parse
        return com.webtoapp.core.perf.NativePerfEngine.extractHost(target)?.lowercase()
            ?: runCatching { Uri.parse(target).host?.lowercase() }.getOrNull()
    }

    /**
     * Create WebChromeClient
     */
    private fun createWebChromeClient(config: WebViewConfig, callbacks: WebViewCallbacks): WebChromeClient {
        return ManagedWebChromeClient(
            config = config,
            callbacks = callbacks,
            specialUrlHandler = specialUrlHandler
        )
    }
    
    private fun normalizeHttpUrlForSecurity(url: String): String {
        return upgradeInsecureHttpUrl(url) ?: url
    }
    
    private fun upgradeInsecureHttpUrl(url: String): String? {
        if (!url.startsWith("http://", ignoreCase = true)) return null
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return null
        if (host in LOCAL_CLEARTEXT_HOSTS) return null
        return url.replaceFirst(Regex("(?i)^http://"), "https://")
    }

    /**
     * Apply strict host policy before first load so initial request already uses strict settings.
     */
    fun applyPreloadPolicyForUrl(webView: WebView, pageUrl: String?) {
        strictHostRuntimePolicy.applyPreloadPolicyForUrl(
            webView = webView,
            pageUrl = pageUrl,
            currentConfig = currentConfig,
            currentDeviceDisguiseConfig = currentDeviceDisguiseConfig
        )
    }

    private fun resetStrictHostSessionState(webView: WebView, pageUrl: String?) {
        strictHostRuntimePolicy.resetStrictHostSessionState(webView, pageUrl)
    }

    private fun buildStrictHostOrigins(pageUrl: String?): Set<String> {
        return strictHostRuntimePolicy.buildStrictHostOrigins(pageUrl)
    }

    private fun applyStrictHostRuntimePolicy(webView: WebView, pageUrl: String?) {
        strictHostRuntimePolicy.applyStrictHostRuntimePolicy(
            webView = webView,
            pageUrl = pageUrl,
            currentConfig = currentConfig,
            currentDeviceDisguiseConfig = currentDeviceDisguiseConfig
        )
    }

    private fun isDesktopUaRequested(config: WebViewConfig?): Boolean {
        val cfg = config ?: return false
        return cfg.desktopMode || cfg.userAgentMode in DESKTOP_UA_MODES || (currentDeviceDisguiseConfig?.requiresDesktopViewport() == true)
    }

    private fun applyRequestedWithHeaderAllowListForStrictHost(settings: WebSettings) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) return
        runCatching {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
            AppLogger.d("WebViewManager", "Strict host policy: X-Requested-With header disabled")
        }.onFailure { error ->
            AppLogger.w("WebViewManager", "Failed to disable X-Requested-With header allow-list", error)
        }
    }

    private fun isBackgroundBridgeScheme(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()
        if (scheme !in setOf("bytedance", "snssdk", "douyin")) return false
        return host == "dispatch_message" || path.contains("dispatch_message")
    }

    /**
     * Handle special URLs (tel, mailto, sms, third-party apps, etc.)
     */
    private fun handleSpecialUrl(url: String, isUserGesture: Boolean): Boolean {
        return specialUrlHandler.handleSpecialUrl(
            url = url,
            isUserGesture = isUserGesture,
            currentMainFrameUrl = currentMainFrameUrl,
            currentConfig = currentConfig,
            managedWebViews = managedWebViews.keys,
            shouldUseScriptlessMode = strictHostRuntimePolicy::shouldUseScriptlessMode
        )
    }
    
    private fun sanitizeFallbackUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)) {
            AppLogger.w("WebViewManager", "Ignoring non-http(s) fallback URL in intent:// payload")
            return null
        }
        return normalizeHttpUrlForSecurity(trimmed)
    }

    /**
     * Check if URL is external link
     */
    private fun isExternalUrl(targetUrl: String, currentUrl: String?): Boolean {
        if (currentUrl == null) return false
        val targetHost = runCatching { Uri.parse(targetUrl).host?.lowercase() }.getOrNull() ?: return false
        val currentHost = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull() ?: return false
        return !targetHost.endsWith(currentHost) && !currentHost.endsWith(targetHost)
    }
    
    /**
     * Clean up WebView resources to prevent memory leak
     * Should be called when Activity/Fragment is destroyed
     */
    fun destroyWebView(webView: WebView) {
        webViewLifecycleCleaner.destroyWebView(webView)
    }
    
    /**
     * Clean up all managed WebViews
     */
    fun destroyAll() {
        webViewLifecycleCleaner.destroyAll()
    }
    
    /**
     * Get BrowserShields instance for external access (UI, settings, etc.)
     */
    fun getShields(): BrowserShields? = if (::shields.isInitialized) shields else null
    
    // Save config reference (for script injection)
    private var currentConfig: WebViewConfig?
        get() = sessionState.currentConfig
        set(value) {
            sessionState.currentConfig = value
        }
    
    // Browser Disguise — pre-generated anti-fingerprint JS (cached per configureWebView call)
    private var cachedBrowserDisguiseJs: String?
        get() = sessionState.cachedBrowserDisguiseJs
        set(value) {
            sessionState.cachedBrowserDisguiseJs = value
        }
    // Browser Disguise — config reference (for BrowserDisguiseEngine integration)
    private var cachedBrowserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig?
        get() = sessionState.cachedBrowserDisguiseConfig
        set(value) {
            sessionState.cachedBrowserDisguiseConfig = value
        }
    // Device Disguise — config reference (for device type/brand/model UA spoofing)
    private var currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig?
        get() = sessionState.currentDeviceDisguiseConfig
        set(value) {
            sessionState.currentDeviceDisguiseConfig = value
        }
    
    /**
     * Inject browser compatibility scripts
     * Fix differences between Android WebView and browsers
     */
    private fun injectCompatibilityScripts(
        webView: WebView,
        pageUrl: String? = null,
        conservativeMode: Boolean = shouldUseConservativeScriptMode(pageUrl)
    ) {
        val config = currentConfig ?: return
        
        try {
            val scripts = mutableListOf<String>()
            if (conservativeMode) {
                AppLogger.d("WebViewManager", "Compatibility safe mode enabled for remote page: $pageUrl")
            }
            
            // 1. CSS zoom polyfill - convert zoom to transform: scale()
            if (config.enableZoomPolyfill && !conservativeMode) {
                scripts.add("""
                    // CSS zoom polyfill for Android WebView
                    (function() {
                        'use strict';
                        
                        // 标记 polyfill 已加载
                        if (window.__webtoapp_zoom_polyfill__) return;
                        window.__webtoapp_zoom_polyfill__ = true;
                        
                        // 存储元素原始宽度
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
                                    // 保存原始宽度
                                    if (!originalWidths.has(el)) {
                                        originalWidths.set(el, el.style.width || '');
                                    }
                                    // 清除 zoom 并应用 transform
                                    el.style.zoom = '';
                                    el.style.transform = 'scale(' + scale + ')';
                                    el.style.transformOrigin = 'top left';
                                    // 缩小时需要扩展宽度以避免内容被裁切
                                    if (scale < 1) {
                                        el.style.width = (100 / scale) + '%';
                                    }
                                    console.log('[WebToApp] Converted zoom to transform:', scale, 'for element:', el.tagName);
                                }
                            }
                        }
                        
                        // MutationObserver 监听 style 属性变化
                        var observer = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                                    convertZoomToTransform(mutation.target);
                                }
                                if (mutation.addedNodes) {
                                    mutation.addedNodes.forEach(function(node) {
                                        if (node.nodeType === 1) {
                                            convertZoomToTransform(node);
                                            // 也检查子元素
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
                        
                        // 设置 observer 的函数
                        function setupObserver() {
                            if (document.documentElement) {
                                observer.observe(document.documentElement, {
                                    attributes: true,
                                    childList: true,
                                    subtree: true,
                                    attributeFilter: ['style']
                                });
                                // 初始扫描
                                if (document.body) {
                                    convertZoomToTransform(document.body);
                                    document.body.querySelectorAll('*').forEach(function(el) {
                                        convertZoomToTransform(el);
                                    });
                                }
                                console.log('[WebToApp] CSS zoom observer started');
                            }
                        }
                        
                        // DOM 就绪后设置 observer
                        if (document.readyState === 'loading') {
                            document.addEventListener('DOMContentLoaded', setupObserver);
                        } else {
                            setupObserver();
                        }
                        
                        // Override CSSStyleDeclaration.zoom setter（最关键的拦截）
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
                                    // 重置为默认
                                    if (value === '' || value === '1' || value === 'normal' || value === 'initial') {
                                        this.transform = '';
                                        this.transformOrigin = '';
                                    }
                                    if (zoomDescriptor && zoomDescriptor.set) {
                                        zoomDescriptor.set.call(this, value);
                                    }
                                },
                                get: function() {
                                    // 返回基于 transform 计算的 zoom 值
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
                """.trimIndent())
            }
            
            // 2. navigator.share polyfill
            if (config.enableShareBridge && !conservativeMode) {
                scripts.add("""
                    // navigator.share polyfill for Android WebView
                    (function() {
                        'use strict';
                        
                        if (typeof NativeShareBridge !== 'undefined') {
                            // Implement navigator.share
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
                            
                            // Implement navigator.canShare
                            navigator.canShare = function(data) {
                                // Basic support for text and url
                                if (!data) return false;
                                if (data.files) return false; // File sharing not yet supported
                                return true;
                            };
                            
                            console.log('[WebToApp] navigator.share polyfill loaded');
                        }
                    })();
                """.trimIndent())
            }
            
            // 3. Clipboard API polyfill for non-HTTPS sites (e.g. code-server on http://localhost)
            // navigator.clipboard requires Secure Context (HTTPS), so we bridge it to NativeBridge
            if (!conservativeMode) {
                scripts.add("""
                    // Clipboard API polyfill for Android WebView (HTTP compatibility)
                    (function() {
                        'use strict';
                        
                        if (window.__webtoapp_clipboard_polyfill__) return;
                        window.__webtoapp_clipboard_polyfill__ = true;
                        
                        // Check if NativeBridge is available (injected by WebView)
                        var hasBridge = typeof NativeBridge !== 'undefined';
                        if (!hasBridge) {
                            console.log('[WebToApp] NativeBridge not found, clipboard polyfill skipped');
                            return;
                        }
                        
                        // Determine if we're in a non-secure context where clipboard API won't work natively
                        var isSecureContext = window.isSecureContext;
                        var needsPolyfill = !isSecureContext || 
                            !navigator.clipboard || 
                            typeof navigator.clipboard.readText !== 'function';
                        
                        if (!needsPolyfill) {
                            // Even in secure contexts, wrap to provide fallback
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
                        
                        // Full polyfill for non-secure contexts
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
                                        // ClipboardItem API - extract text/plain
                                        if (data && data.length > 0) {
                                            var item = data[0];
                                            if (item.getType) {
                                                item.getType('text/plain').then(function(blob) {
                                                    return blob.text();
                                                }).then(function(text) {
                                                    NativeBridge.copyToClipboard(text);
                                                    resolve();
                                                }).catch(function() {
                                                    resolve(); // Silently succeed for non-text items
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
                        
                        // Override navigator.clipboard
                        try {
                            Object.defineProperty(navigator, 'clipboard', {
                                value: clipboardPolyfill,
                                writable: true,
                                configurable: true,
                                enumerable: true
                            });
                        } catch(e) {
                            // Fallback: direct assignment
                            try {
                                navigator.clipboard = clipboardPolyfill;
                            } catch(e2) {
                                console.warn('[WebToApp] Cannot override navigator.clipboard:', e2);
                            }
                        }
                        
                        // Also override Permissions API for clipboard to always return 'granted'
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
                        
                        // Polyfill document.execCommand for legacy clipboard access
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
                """.trimIndent())
            }
            
            // 4. Hide link URL preview (tooltip)
            // This removes the small URL preview popup when hovering/long-pressing links
            // On Android WebView the popup is native Chromium behavior;
            // CSS -webkit-touch-callout only works on iOS.
            // We suppress it by: blocking contextmenu on links, removing title attrs,
            // and intercepting selection start events on anchor elements.
            if (!conservativeMode) {
                scripts.add("""
                // Hide link URL preview for privacy
                (function() {
                    'use strict';
                    if (window.__wtaLinkPreviewHidden) return;
                    window.__wtaLinkPreviewHidden = true;
                    
                    // --- CSS ---
                    var style = document.createElement('style');
                    style.id = 'webtoapp-hide-url-preview';
                    style.textContent = '\n' +
                        'a, a * {\n' +
                        '  -webkit-touch-callout: none !important;\n' +
                        '  -webkit-user-select: none !important;\n' +
                        '  user-select: none !important;\n' +
                        '}\n';
                    (document.head || document.documentElement).appendChild(style);
                    
                    // --- Helper: check if an element is inside an anchor ---
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
                    
                    // --- Block contextmenu on links (suppresses Android preview popup) ---
                    document.addEventListener('contextmenu', function(e) {
                        if (findAnchorParent(e.target)) {
                            e.preventDefault();
                            e.stopImmediatePropagation();
                            return false;
                        }
                    }, true);
                    
                    // --- Block selectstart on links ---
                    document.addEventListener('selectstart', function(e) {
                        if (findAnchorParent(e.target)) {
                            e.preventDefault();
                        }
                    }, true);
                    
                    // --- Remove title attribute from all links ---
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
                    
                    // Watch for dynamically added links
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
                    
                    // Intercept setAttribute to prevent title from being set on links
                    var originalSetAttribute = Element.prototype.setAttribute;
                    Element.prototype.setAttribute = function(name, value) {
                        if (this.tagName === 'A' && name.toLowerCase() === 'title') {
                            return;
                        }
                        return originalSetAttribute.call(this, name, value);
                    };
                    
                    console.log('[WebToApp] Link URL preview hidden (enhanced)');
                })();
            """.trimIndent())
            }
            
            // 5. Popup Blocker
            if (config.popupBlockerEnabled && !conservativeMode) {
                scripts.add("""
                    // Popup Blocker - blocks unwanted popups and redirects
                    (function() {
                        'use strict';
                        
                        // Track if popup blocker is enabled (can be toggled at runtime)
                        window.__webtoapp_popup_blocker_enabled__ = true;
                        
                        var blockedCount = 0;
                        var allowedDomains = []; // Can be configured later
                        
                        // Store original functions
                        var originalOpen = window.open;
                        var originalAlert = window.alert;
                        var originalConfirm = window.confirm;
                        
                        // Helper to check if URL is suspicious
                        function isSuspiciousUrl(url) {
                            if (!url) return true;
                            var lowerUrl = url.toLowerCase();
                            // Common ad/popup patterns
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
                        
                        // Helper to check if domain is allowed
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
                        
                        // Override window.open
                        window.open = function(url, target, features) {
                            if (!window.__webtoapp_popup_blocker_enabled__) {
                                return originalOpen.apply(window, arguments);
                            }
                            
                            // Allow same-origin and allowed domains
                            var isSameOrigin = false;
                            try {
                                if (url) {
                                    var urlObj = new URL(url, window.location.href);
                                    isSameOrigin = urlObj.origin === window.location.origin;
                                }
                            } catch(e) { /* URL parse failed, treat as cross-origin */ }
                            
                            // Block conditions
                            var shouldBlock = false;
                            
                            // Block about:blank and javascript: URLs (common popup tricks)
                            if (!url || url === 'about:blank' || url.indexOf('javascript:') === 0) {
                                shouldBlock = true;
                            }
                            // Block suspicious URLs
                            else if (isSuspiciousUrl(url) && !isSameOrigin && !isDomainAllowed(url)) {
                                shouldBlock = true;
                            }
                            
                            if (shouldBlock) {
                                blockedCount++;
                                console.log('[WebToApp PopupBlocker] Blocked popup #' + blockedCount + ':', url || '(empty)');
                                // Return fake window object to prevent errors
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
                            
                            // Allow legitimate popups
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
                        
                        // Block popup triggers via setTimeout/setInterval with very short delays
                        var originalSetTimeout = window.setTimeout;
                        var originalSetInterval = window.setInterval;
                        
                        window.setTimeout = function(fn, delay) {
                            // Block immediate timeouts that might be popup triggers
                            if (delay === 0 && typeof fn === 'string' && fn.indexOf('open(') !== -1) {
                                console.log('[WebToApp PopupBlocker] Blocked setTimeout popup trigger');
                                return 0;
                            }
                            return originalSetTimeout.apply(window, arguments);
                        };
                        
                        // Expose toggle function
                        window.__webtoapp_toggle_popup_blocker__ = function(enabled) {
                            window.__webtoapp_popup_blocker_enabled__ = enabled;
                            console.log('[WebToApp PopupBlocker] ' + (enabled ? 'Enabled' : 'Disabled'));
                        };
                        
                        // Expose stats
                        window.__webtoapp_popup_blocker_stats__ = function() {
                            return { blocked: blockedCount, enabled: window.__webtoapp_popup_blocker_enabled__ };
                        };
                        
                        console.log('[WebToApp] Popup blocker loaded');
                    })();
                """.trimIndent())
            }
            
            // 6. Other compatibility fixes
            scripts.add("""
                // Compatibility fixes
                (function() {
                    'use strict';
                    
                    // Fix requestIdleCallback (some WebViews don't support)
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
                    
                    // Fix ResizeObserver (some old WebViews don't support)
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
            """.trimIndent())
            
            val canInjectShieldsJs = !conservativeMode
            // 7. Shields: GPC (Global Privacy Control) signal
            // Skip all Shields scripts when disableShields is true (per-app setting)
            if (canInjectShieldsJs && !config.disableShields && ::shields.isInitialized && shields.isEnabled() && shields.getConfig().gpcEnabled) {
                scripts.add(shields.gpcInjector.generateScript())
            }
            
            // 8. Shields: Cookie consent auto-dismiss
            if (canInjectShieldsJs && !config.disableShields && ::shields.isInitialized && shields.isEnabled() && shields.getConfig().cookieConsentBlock) {
                scripts.add(shields.cookieConsentBlocker.generateScript())
                shields.stats.recordCookieConsentBlocked()
            }
            
            // 9. Shields: Referrer policy
            if (canInjectShieldsJs && !config.disableShields && ::shields.isInitialized && shields.isEnabled()) {
                val referrerPolicy = shields.getConfig().referrerPolicy.value
                scripts.add("""
                    // Shields: Referrer Policy
                    (function() {
                        'use strict';
                        if (window.__webtoapp_referrer_policy__) return;
                        window.__webtoapp_referrer_policy__ = true;
                        var meta = document.createElement('meta');
                        meta.name = 'referrer';
                        meta.content = '$referrerPolicy';
                        (document.head || document.documentElement).appendChild(meta);
                        console.log('[WebToApp Shields] Referrer policy set:', '$referrerPolicy');
                    })();
                """.trimIndent())
            }

            // 10. AdBlocker: Cosmetic element hiding CSS
            // Inject CSS rules that hide ad elements matching ## filter selectors
            if (canInjectShieldsJs && adBlocker.isEnabled()) {
                val adPageHost = pageUrl?.let { extractHostFromUrl(it) } ?: ""
                if (adPageHost.isNotEmpty()) {
                    val cosmeticCss = adBlocker.getCosmeticFilterCss(adPageHost)
                    if (cosmeticCss.isNotEmpty()) {
                        val escapedCss = cosmeticCss
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "")
                        scripts.add("""
                            // AdBlocker: Cosmetic element hiding
                            (function() {
                                'use strict';
                                if (window.__wta_cosmetic_filters__) return;
                                window.__wta_cosmetic_filters__ = true;
                                try {
                                    var style = document.createElement('style');
                                    style.setAttribute('type', 'text/css');
                                    style.setAttribute('data-wta', 'cosmetic');
                                    style.textContent = '$escapedCss';
                                    (document.head || document.documentElement).appendChild(style);
                                } catch(e) { console.warn('[WTA] Cosmetic filter injection error:', e); }
                            })();
                        """.trimIndent())
                        AppLogger.d("WebViewManager", "Cosmetic filters injected for: $adPageHost")
                    }

                    // 11. AdBlocker: Anti-anti-adblock scriptlet injection
                    // Defuse common adblock detection scripts
                    val antiAdblockScript = adBlocker.getAntiAdblockScript(adPageHost)
                    if (antiAdblockScript.isNotEmpty()) {
                        scripts.add(antiAdblockScript)
                    }
                }
            }

            // Execute all compatibility scripts
            val combinedScript = scripts.joinToString("\n\n")
            webView.evaluateJavascript(combinedScript, null)
            AppLogger.d("WebViewManager", "Browser compatibility scripts injected")
            
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Compatibility script injection failed", e)
        }
    }
    
}
