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
import com.webtoapp.core.crypto.SecureAssetLoader
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
import java.io.ByteArrayInputStream
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
        private var DESKTOP_USER_AGENT: String? = null
        private const val DESKTOP_USER_AGENT_FALLBACK = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        
        // MIME type lookup map (replaces when-expression for O(1) lookup)
        private val MIME_TYPE_MAP = mapOf(
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
        private val TEXT_MIME_TYPES = setOf(
            "text/html", "text/css", "text/plain",
            "application/javascript", "application/json",
            "application/xml", "image/svg+xml"
        )
        
        // Desktop UA modes set (avoids listOf per configureWebView call)
        private val DESKTOP_UA_MODES = setOf(
            UserAgentMode.CHROME_DESKTOP,
            UserAgentMode.SAFARI_DESKTOP,
            UserAgentMode.FIREFOX_DESKTOP,
            UserAgentMode.EDGE_DESKTOP
        )
        
        // Headers to skip when proxying requests
        private val SKIP_HEADERS = setOf("host", "connection")

        // Local cleartext hosts allowed by network security config
        private val LOCAL_CLEARTEXT_HOSTS = setOf("localhost", "127.0.0.1", "10.0.2.2")

        // Well-known map tile server host suffixes — these must NEVER be blocked by
        // ad/tracker filters, otherwise Leaflet / Mapbox / Google Maps tile layers break.
        private val MAP_TILE_HOST_SUFFIXES = setOf(
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
        private val STRICT_COMPAT_HOST_SUFFIXES = setOf(
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
        private var STRICT_COMPAT_MOBILE_USER_AGENT: String? = null
        private const val STRICT_COMPAT_MOBILE_UA_FALLBACK =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        // Common multi-part TLD suffixes for basic registrable-domain matching
        private val COMMON_SECOND_LEVEL_TLDS = setOf(
            "co.uk", "org.uk", "gov.uk", "ac.uk",
            "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn",
            "com.hk", "com.tw",
            "com.au", "net.au", "org.au",
            "co.jp", "co.kr", "co.in", "com.br", "com.mx"
        )

        // Schemes that should never be delegated to external intents
        private val BLOCKED_SPECIAL_SCHEMES = setOf("javascript", "data", "file", "content", "about")

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
        private const val VIEWPORT_FIT_SCREEN_JS = """(function(){
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
        private val cleartextProxyClient: OkHttpClient by lazy {
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
        private const val VIEWPORT_CUSTOM_JS = """(function(){
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
        private const val SCROLL_SAVE_JS = """(function(){
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

        private const val SCROLL_RESTORE_JS = """(function(){
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
        private const val IMAGE_REPAIR_JS = """
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
        private val PAYMENT_SCHEMES = setOf(
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
        private const val CLIPBOARD_POLYFILL_JS = """
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
    
    // App configured extension module ID list
    private var appExtensionModuleIds: List<String> = emptyList()
    
    // Embedded extension module data (for Shell mode)
    private var embeddedModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList()

    // Whether to fallback to globally enabled modules when app module list is empty.
    private var allowGlobalModuleFallback: Boolean = false
    
    // Custom FAB icon for extension panel
    private var extensionFabIcon: String = ""
    
    // Greasemonkey API bridge for userscript support
    private var gmBridge: com.webtoapp.core.extension.GreasemonkeyBridge? = null
    
    // Chrome Extension background script runtimes (extId -> runtime)
    private var extensionRuntimes: MutableMap<String, com.webtoapp.core.extension.ChromeExtensionRuntime> = mutableMapOf()
    
    // File manager for @require/@resource cache access
    private val extensionFileManager by lazy {
        com.webtoapp.core.extension.ExtensionFileManager(context)
    }
    
    // Track configured WebViews for resource cleanup
    private val managedWebViews = java.util.WeakHashMap<WebView, Boolean>()
    
    // Browser Shields — privacy protection manager
    private lateinit var shields: BrowserShields
    
    // Error page manager — custom error page generation
    private var errorPageManager: ErrorPageManager? = null
    private var lastFailedUrl: String? = null
    
    // file:// retry counter — auto-retry when file not yet extracted (race condition)
    private var fileRetryCount = 0
    private var fileRetryUrl: String? = null
    private val FILE_MAX_RETRIES = 3
    private val FILE_RETRY_DELAY_MS = 500L
    
    // Main-frame URL cache (must be thread-safe for shouldInterceptRequest background thread)
    @Volatile
    private var currentMainFrameUrl: String? = null
    
    // Cookie flush 防抖 — 避免快速导航时每次 onPageFinished 都同步写磁盘
    private val cookieFlushRunnable = Runnable {
        try { CookieManager.getInstance().flush() } catch (_: Exception) {}
    }
    
    /**
     * 从系统 WebView 默认 UA 中提取 Chrome 版本号，保持 UA 与设备一致。
     * 避免硬编码过时的 Chrome/120 被网站检测为旧浏览器。
     */
    private fun ensureDynamicUserAgents() {
        if (DESKTOP_USER_AGENT != null) return
        try {
            val defaultUA = WebSettings.getDefaultUserAgent(context)
            val chromeVersion = Regex("""Chrome/(\d+\.\d+\.\d+\.\d+)""").find(defaultUA)
                ?.groupValues?.get(1) ?: "130.0.0.0"
            DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
            STRICT_COMPAT_MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
            AppLogger.d("WebViewManager", "Dynamic UA initialized: Chrome/$chromeVersion")
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "Failed to extract Chrome version, using fallback")
            DESKTOP_USER_AGENT = DESKTOP_USER_AGENT_FALLBACK
            STRICT_COMPAT_MOBILE_USER_AGENT = STRICT_COMPAT_MOBILE_UA_FALLBACK
        }
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
        val extensionManager = ExtensionManager.getInstance(context)
        
        return if (appExtensionModuleIds.isNotEmpty()) {
            extensionManager.getModulesByIds(appExtensionModuleIds)
        } else if (allowGlobalModuleFallback) {
            extensionManager.getEnabledModules()
        } else {
            emptyList()
        }
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

        val isDesktopModeRequested = config.userAgentMode in DESKTOP_UA_MODES || config.desktopMode || (currentDeviceDisguiseConfig?.requiresDesktopViewport() == true)
        // Landscape apps should keep a native-sized viewport instead of overview shrink-fit.
        // This avoids "zoomed-out letterbox" rendering in wide screens.
        val preferLandscapeEmbeddedViewport = config.landscapeMode && !isDesktopModeRequested
        
        webView.apply {
            settings.apply {
                // JavaScript
                javaScriptEnabled = config.javaScriptEnabled
                javaScriptCanOpenWindowsAutomatically = true

                // DOM storage
                domStorageEnabled = config.domStorageEnabled
                databaseEnabled = true

                // File access
                allowFileAccess = config.allowFileAccess
                allowContentAccess = config.allowContentAccess

                // Cache
                cacheMode = if (config.cacheEnabled) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_NO_CACHE
                }

                // Zoom
                setSupportZoom(config.zoomEnabled)
                builtInZoomControls = config.zoomEnabled
                displayZoomControls = false

                // Viewport
                useWideViewPort = true
                loadWithOverviewMode = !preferLandscapeEmbeddedViewport

                // ViewportMode: FIT_SCREEN — 针对 Unity/Canvas 游戏的特殊优化
                // 强制 initial-scale=1 防止 DPI 缩放放大内容
                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    AppLogger.d("WebViewManager", "ViewportMode.FIT_SCREEN applied: overview fit + JS adaptation")
                } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.DESKTOP) {
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    textZoom = 100
                    AppLogger.d("WebViewManager", "ViewportMode.DESKTOP applied")
                } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    AppLogger.d("WebViewManager", "ViewportMode.CUSTOM applied: width=${config.customViewportWidth}")
                }

                // User Agent config
                // Priority: userAgentMode > desktopMode (backward compatible) > userAgent (legacy field)
                val effectiveUserAgent = resolveUserAgent(config)
                if (effectiveUserAgent != null) {
                    userAgentString = effectiveUserAgent
                    AppLogger.d("WebViewManager", "User-Agent set: ${effectiveUserAgent.take(80)}...")
                }

                // Chrome extensions are designed for desktop browsers.
                // When active Chrome extension modules exist and the user hasn't explicitly
                // set a UA mode, automatically use desktop UA to prevent mobile redirects
                // (e.g. www.bilibili.com → m.bilibili.com) that break extension functionality.
                if (!isDesktopModeRequested && effectiveUserAgent == null) {
                    val hasActiveChromeExt = getActiveModulesForCurrentApp().any { module ->
                        module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                        module.chromeExtId.isNotEmpty()
                    }
                    if (hasActiveChromeExt) {
                        userAgentString = DESKTOP_USER_AGENT ?: DESKTOP_USER_AGENT_FALLBACK
                        AppLogger.d("WebViewManager", "Desktop UA auto-enabled for active Chrome extension(s)")
                    }
                }

                // Desktop mode viewport settings (independent of User-Agent)
                if (isDesktopModeRequested) {
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    // Set default zoom level to fit desktop pages
                    textZoom = 100
                } else if (preferLandscapeEmbeddedViewport) {
                    AppLogger.d(
                        "WebViewManager",
                        "Landscape viewport policy applied: disable overview shrink-fit (loadWithOverviewMode=false)"
                    )
                }

                // Mixed content — Allow mixed content to prevent WebView from downgrading
                // the page's Secure Context status when sub-resources load over HTTP.
                // This is critical for WebRTC/getUserMedia() (microphone, camera) which
                // requires a Secure Context. Without ALWAYS_ALLOW, COMPATIBILITY_MODE can
                // silently block getUserMedia() on some sites with mixed sub-resources.
                // Note: The app already enforces HTTPS via Shields auto-upgrade and
                // upgradeInsecureHttpUrl(), so double-blocking at WebSettings level is
                // unnecessary and can break media capture APIs.
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // Other settings
                mediaPlaybackRequiresUserGesture = false
                
                // Geolocation: explicitly enable for map-based apps (Leaflet, Google Maps, etc.)
                // While the default is true, setting explicitly ensures it's never accidentally disabled
                @Suppress("DEPRECATION")
                setGeolocationEnabled(true)
                // Set geolocation database path for older Android versions (deprecated but needed for API < 24)
                @Suppress("DEPRECATION")
                setGeolocationDatabasePath(context.filesDir.absolutePath)
                
                // SECURITY: Only enable file:// cross-origin access for local HTML/FRONTEND apps
                // that truly need it. For WEB apps loading remote URLs, this MUST be false
                // to prevent malicious web pages from reading local files.
                allowFileAccessFromFileURLs = config.allowFileAccessFromFileURLs
                allowUniversalAccessFromFileURLs = config.allowUniversalAccessFromFileURLs

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            // Scrollbar
            isScrollbarFadingEnabled = true
            scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

            // Hardware acceleration
            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            
            // C 级性能优化 — WebView 底层设置 (offscreenPreRaster, 渲染优先级)
            com.webtoapp.core.perf.NativePerfEngine.optimizeWebViewSettings(this)
            
            // ============ Compatibility Enhancements ============
            
            // Initial scale (fix CSS zoom not working in WebView)
            if (config.initialScale > 0) {
                setInitialScale(config.initialScale)
                AppLogger.d("WebViewManager", "Set initial scale: ${config.initialScale}%")
            } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
                // FIT_SCREEN: 强制 initial scale = 1，防止 DPI 缩放放大内容
                setInitialScale(1)
                AppLogger.d("WebViewManager", "FIT_SCREEN: forced initial scale to 1 (override DPI scaling)")
            } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
                // CUSTOM: 强制 initial scale = 1，配合自定义视口宽度
                setInitialScale(1)
                AppLogger.d("WebViewManager", "ViewportMode.CUSTOM: forced initial scale to 1 (custom width=${config.customViewportWidth})")
            }
            
            // Support window.open / target="_blank"
            settings.setSupportMultipleWindows(config.newWindowBehavior != NewWindowBehavior.SAME_WINDOW)

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
            initChromeExtensionRuntimes(webView)
            
            // 浏览器内核伪装 — 在 UA 设置完成后清洗, 移除 wv/Version 标识
            com.webtoapp.core.kernel.BrowserKernel.configureWebView(webView)
            
            // ============ 键盘输入支持 ============
            // 设置焦点属性，确保不需要触屏交互也能使用键盘
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
    }
    
    /**
     * Parse User-Agent config
     * Priority: userAgentMode > desktopMode (backward compatible) > userAgent (legacy field)
     * @return Effective User-Agent string, or null if using system default
     */
    private fun resolveUserAgent(config: WebViewConfig): String? {
        AppLogger.d("WebViewManager", "resolveUserAgent: userAgentMode=${config.userAgentMode}, customUserAgent=${config.customUserAgent?.take(30)}, desktopMode=${config.desktopMode}")
        
        // 0. Top priority: DeviceDisguiseConfig (new 设备伪装 system)
        val ddConfig = currentDeviceDisguiseConfig
        if (ddConfig != null && ddConfig.enabled) {
            val ua = ddConfig.generateUserAgent()
            if (ua.isNotBlank()) {
                AppLogger.d("WebViewManager", "resolveUserAgent: DeviceDisguise -> ${ua.take(80)}")
                return ua
            }
        }
        
        // 1. Priority: use userAgentMode
        when (config.userAgentMode) {
            UserAgentMode.DEFAULT -> {
                // Continue to check other config
            }
            UserAgentMode.CUSTOM -> {
                // Custom mode: use customUserAgent
                val ua = config.customUserAgent?.takeIf { it.isNotBlank() }
                AppLogger.d("WebViewManager", "resolveUserAgent: CUSTOM mode -> ${ua?.take(60) ?: "null"}")
                return ua
            }
            else -> {
                // Use preset User-Agent
                val ua = config.userAgentMode.userAgentString
                AppLogger.d("WebViewManager", "resolveUserAgent: ${config.userAgentMode.name} mode -> ${ua?.take(60) ?: "null"}")
                return ua
            }
        }
        
        // 2. Backward compatible: check desktopMode
        if (config.desktopMode) {
            AppLogger.d("WebViewManager", "resolveUserAgent: desktopMode fallback")
            return DESKTOP_USER_AGENT ?: DESKTOP_USER_AGENT_FALLBACK
        }
        
        // 3. Backward compatible: check legacy userAgent field
        val legacyUa = config.userAgent?.takeIf { it.isNotBlank() }
        AppLogger.d("WebViewManager", "resolveUserAgent: DEFAULT mode, legacyUA=${legacyUa?.take(60) ?: "null"}")
        return legacyUa
    }

    /**
     * Create WebViewClient
     */
    private fun createWebViewClient(
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ): WebViewClient {
        // ★ DIAGNOSTIC counters — 追踪页面加载问题
        var diagPageStartTime = 0L
        var diagRequestCount = 0
        var diagBlockedCount = 0
        var diagErrorCount = 0
        
        return object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.let {
                    val url = it.url?.toString() ?: ""
                    diagRequestCount++
                    // ★ DIAG: 每50个请求输出一次进度
                    if (diagRequestCount % 50 == 0) {
                        android.util.Log.w("DIAG", "Request progress: total=$diagRequestCount blocked=$diagBlockedCount errors=$diagErrorCount elapsed=${System.currentTimeMillis() - diagPageStartTime}ms")
                    }

                    // OAuth header sanitization is handled globally by
                    // BrowserKernel.configureWebView() which removes X-Requested-With for all requests.
                    // No per-request processing needed here.
                    
                    // Handle chrome-extension:// resource requests
                    // Extensions use chrome.runtime.getURL() which returns chrome-extension://{extId}/{path}
                    if (com.webtoapp.core.extension.ExtensionResourceInterceptor.isChromeExtensionUrl(url)) {
                        return com.webtoapp.core.extension.ExtensionResourceInterceptor.intercept(context, url)
                    }
                    
                    // Phase G: Check extension webRequest filters and declarativeNetRequest rules
                    // ★ Infer resource type once, reuse for both extension and ad blocker checks
                    val resType = if (url.startsWith("http://") || url.startsWith("https://")) inferResourceType(it) else null
                    if (resType != null) {
                        
                        // Check WebRequestBridge (chrome.webRequest registered filters)
                        if (com.webtoapp.core.extension.WebRequestBridge.shouldBlock(url, resType)) {
                            AppLogger.d("WebViewManager", "WebRequest extension blocked: $url")
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        
                        // Check DeclarativeNetRequestEngine (MV3 DNR rules)
                        val dnrResult = com.webtoapp.core.extension.DeclarativeNetRequestEngine.evaluate(
                            url = url,
                            resourceType = resType,
                            initiatorDomain = try { android.net.Uri.parse(currentMainFrameUrl ?: "").host ?: "" } catch (_: Exception) { "" },
                            method = it.method ?: "GET"
                        )
                        if (dnrResult != null) {
                            when (dnrResult.action) {
                                com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.BLOCK -> {
                                    AppLogger.d("WebViewManager", "DNR blocked: $url")
                                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                                }
                                com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.REDIRECT -> {
                                    // Redirect by loading empty response; WebView will follow redirect naturally
                                    AppLogger.d("WebViewManager", "DNR redirect: $url -> ${dnrResult.redirectUrl}")
                                }
                                com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.ALLOW,
                                com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.ALLOW_ALL_REQUESTS -> {
                                    // Explicitly allowed - skip further blocking
                                    return super.shouldInterceptRequest(view, request)
                                }
                                else -> { /* no action */ }
                            }
                        }
                    }
                    
                    // Handle extension resource requests via HTTPS localhost URL
                    // Extensions use chrome.runtime.getURL() which now returns
                    // https://localhost/__ext__/{extId}/{path} for reliable WebView loading
                    if (url.startsWith("https://localhost/__ext__/")) {
                        val extResourcePath = url.removePrefix("https://localhost/__ext__/")
                        // Reconstruct as chrome-extension:// URL for the interceptor
                        val chromeExtUrl = "chrome-extension://$extResourcePath"
                        return com.webtoapp.core.extension.ExtensionResourceInterceptor.intercept(context, chromeExtUrl)
                    }
                    
                    // Handle local resource requests (via virtual baseURL)
                    // This is for supporting CDN loading using loadDataWithBaseURL
                    if (url.startsWith("https://localhost/__local__/")) {
                        val localPath = url.removePrefix("https://localhost/__local__/")
                        AppLogger.d("WebViewManager", "Loading local resource: $localPath")
                        
                        return try {
                            val file = java.io.File(localPath)
                            if (file.exists() && file.isFile) {
                                val mimeType = getMimeType(localPath)
                                val inputStream = java.io.FileInputStream(file)
                                WebResourceResponse(mimeType, "UTF-8", inputStream)
                            } else {
                                AppLogger.w("WebViewManager", "Local file not found: $localPath")
                                null
                            }
                        } catch (e: Exception) {
                            AppLogger.e("WebViewManager", "Error loading local resource: $localPath", e)
                            null
                        }
                    }
                    
                    // Only handle local asset requests, let external network requests pass through
                    if (url.startsWith("file:///android_asset/")) {
                        val assetPath = url.removePrefix("file:///android_asset/")
                        return loadEncryptedAsset(assetPath)
                    }

                    val bypassAggressiveNetworkHooks = shouldBypassAggressiveNetworkHooks(it, url)
                    if (bypassAggressiveNetworkHooks && it.isForMainFrame) {
                        AppLogger.d("WebViewManager", "Strict compatibility mode: bypass request interception for $url")
                    }
                    
                    // C 级 URL scheme 检测 — 单次 JNI 调用替代多次 startsWith
                    // shouldInterceptRequest 每个子资源都调用, 每页面 50-200 次
                    val urlScheme = com.webtoapp.core.perf.NativePerfEngine.checkUrlScheme(url)
                    // scheme: 1=http, 2=https, 3=file, 4=data, 5=javascript, 6=chrome-ext
                    val isHttpOrHttps = urlScheme == 1 || urlScheme == 2
                    val isLocalhost = url.startsWith("https://localhost/__local__/")
                    val isThirdParty = if (!bypassAggressiveNetworkHooks && isHttpOrHttps && !isLocalhost)
                        isThirdPartySubResourceRequest(it) else false
                    val isMapTile = if (isThirdParty) isMapTileRequest(url) else false
                    
                    // Shields: Tracker blocking (before AdBlocker for dedicated tracking protection)
                    // Skip when disableShields is true (per-app setting for games etc.)
                    // Also skip for well-known map tile servers to prevent breaking Leaflet/Mapbox/Google Maps
                    if (!bypassAggressiveNetworkHooks &&
                        !config.disableShields &&
                        isThirdParty &&
                        !isMapTile &&
                        ::shields.isInitialized && shields.isEnabled() && shields.getConfig().trackerBlocking) {
                        val trackerCategory = shields.trackerBlocker.checkTracker(url)
                        if (trackerCategory != null) {
                            shields.stats.recordTrackerBlocked(trackerCategory)
                            diagBlockedCount++
                            android.util.Log.w("DIAG", "TRACKER_BLOCKED [$trackerCategory]: ${url.take(120)}")
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                    }
                    
                    // Ad blocking: check BOTH first-party and third-party requests
                    // ★ Reuse resType already inferred above; fallback to inferResourceType for non-HTTP
                    if (!bypassAggressiveNetworkHooks &&
                        !isMapTile &&
                        adBlocker.isEnabled()) {
                        val adResType = resType ?: inferResourceType(it)
                        val pageHost = extractHostFromUrl(currentMainFrameUrl)
                        if (adBlocker.shouldBlock(url, pageHost, adResType, isThirdParty)) {
                            if (::shields.isInitialized) shields.stats.recordAdBlocked()
                            diagBlockedCount++
                            android.util.Log.w("DIAG", "AD_BLOCKED [${if (isThirdParty) "3P" else "1P"}/$adResType]: ${url.take(120)}")
                            return adBlocker.createEmptyResponse(adResType)
                        }
                    }
                    
                    // Cross-Origin Isolation support (for SharedArrayBuffer / FFmpeg.wasm)
                    if (!bypassAggressiveNetworkHooks &&
                        config.enableCrossOriginIsolation &&
                        isHttpOrHttps) {
                        android.util.Log.w("DIAG", "CROSS_ORIGIN_PROXY: ${url.take(120)}")
                        return fetchWithCrossOriginHeaders(it)
                    }

                    // Cleartext proxy — bypass ERR_CLEARTEXT_NOT_PERMITTED for HTTP sub-resources
                    // (e.g., m3u8 video streams, HTTP images/scripts loaded from HTTPS pages).
                    // The WebView's network stack blocks all external HTTP due to
                    // android:usesCleartextTraffic="false" in the manifest.
                    // This proxy fetches HTTP content via OkHttp with explicit cleartext support
                    // and serves it back to the WebView, so the WebView never makes HTTP requests directly.
                    if (url.startsWith("http://")) {
                        val cleartextResponse = fetchCleartextResource(it)
                        if (cleartextResponse != null) {
                            return cleartextResponse
                        }
                    }
                }
                // Return null to let system handle (including external network requests)
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val isUserGesture = request.hasGesture()
                if (request.isForMainFrame) {
                    AppLogger.d("WebViewManager", "Main-frame navigation request: $url")
                }

                // ============ Google OAuth → Chrome Custom Tab (proactive redirect) ============
                // Google's server-side WebView detection is unfoolable — UA/JS tricks don't work.
                // The ONLY reliable solution is to open Google OAuth in Chrome Custom Tab (CCT),
                // which uses a real Chrome process that Google trusts.
                //
                // After login completes, the redirect_uri takes the user back to the website,
                // and the WebView picks up the authenticated session via shared cookies.
                if (request.isForMainFrame && OAuthCompatEngine.shouldRedirectToCustomTab(url)) {
                    val provider = OAuthCompatEngine.getProviderType(url)
                    AppLogger.i("WebViewManager", "Google OAuth detected [$provider] — redirecting to Chrome Custom Tab: $url")
                    view?.stopLoading()
                    openInCustomTab(url)
                    return true
                }
                
                // Other OAuth providers: Allow login in-WebView with full anti-detection
                // (X-Requested-With removed, UA cleaned, provider-specific JS injected in onPageStarted)
                if (request.isForMainFrame && OAuthCompatEngine.isOAuthUrl(url)) {
                    val provider = OAuthCompatEngine.getProviderType(url)
                    AppLogger.d("WebViewManager", "OAuth detected [$provider] — allowing in-WebView with kernel disguise: $url")
                    // Let it load normally — anti-detection JS will be injected in onPageStarted
                }
                
                // Security baseline: block/upgrade insecure remote HTTP regardless of Shields settings
                // Skip upgrade for same-origin navigation to avoid breaking HTTP-only sites
                // Also skip when disableShields is true (Shell mode default) - respect user's config choice
                // (initial page loads via webView.loadUrl() which bypasses shouldOverrideUrlLoading,
                //  so subsequent same-origin navigations should keep the same protocol)
                val isSameOriginHttp = run {
                    val currentUrl = currentMainFrameUrl
                    if (currentUrl != null && currentUrl.startsWith("http://", ignoreCase = true)) {
                        val currentHost = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull()
                        val targetHost = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull()
                        currentHost != null && targetHost != null && currentHost == targetHost
                    } else false
                }
                if (!isSameOriginHttp && !config.disableShields) {
                    val secureUrl = upgradeInsecureHttpUrl(url)
                    if (secureUrl != null) {
                        view?.loadUrl(secureUrl)
                        AppLogger.d("WebViewManager", "Auto-upgraded insecure HTTP navigation: $url -> $secureUrl")
                        return true
                    }
                }
                
                // Shields: HTTPS auto-upgrade (skip when disableShields is true)
                if (!config.disableShields && ::shields.isInitialized && shields.isEnabled() && shields.getConfig().httpsUpgrade) {
                    val upgradedUrl = shields.httpsUpgrader.tryUpgrade(url)
                    if (upgradedUrl != null) {
                        shields.stats.recordHttpsUpgrade()
                        view?.loadUrl(upgradedUrl)
                        return true
                    }
                }

                // Handle special protocols
                if (handleSpecialUrl(url, isUserGesture)) {
                    return true
                }

                // External link handling
                if (config.openExternalLinks && isExternalUrl(url, view?.url)) {
                    callbacks.onExternalLink(url)
                    return true
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                currentMainFrameUrl = url
                
                // ★ DIAG: 页面加载计时开始
                diagPageStartTime = System.currentTimeMillis()
                diagRequestCount = 0
                diagBlockedCount = 0
                diagErrorCount = 0
                android.util.Log.w("DIAG", "═══ PAGE_STARTED ═══ url=$url")
                android.util.Log.w("DIAG", "  config: disableShields=${config.disableShields} adBlockEnabled=${adBlocker.isEnabled()} crossOriginIsolation=${config.enableCrossOriginIsolation}")
                android.util.Log.w("DIAG", "  shields: initialized=${::shields.isInitialized} enabled=${if (::shields.isInitialized) shields.isEnabled().toString() else "N/A"}")
                if (::shields.isInitialized && shields.isEnabled()) {
                    android.util.Log.w("DIAG", "  shields config: trackerBlocking=${shields.getConfig().trackerBlocking} httpsUpgrade=${shields.getConfig().httpsUpgrade}")
                }
                
                // OAuth anti-detection injection:
                // When navigating to OAuth provider domains, inject provider-specific
                // anti-detection JS. This runs BEFORE any page scripts.
                if (url != null) {
                    OAuthCompatEngine.getAntiDetectionJs(url)?.let { js ->
                        val provider = OAuthCompatEngine.getProviderType(url)
                        AppLogger.d("WebViewManager", "Injecting OAuth anti-detection JS [$provider] for: $url")
                        view?.evaluateJavascript(js, null)
                    }
                }
                
                if (view != null) {
                    applyStrictHostRuntimePolicy(view, url)
                }
                callbacks.onPageStarted(url)
                // Clear error state on new navigation
                lastFailedUrl = null
                // Reset file retry state on successful navigation start
                if (url != null && url != fileRetryUrl) {
                    fileRetryCount = 0
                    fileRetryUrl = null
                }
                // Shields: page lifecycle — reset page stats
                if (::shields.isInitialized) shields.onPageStarted(url)
                // ★ Invalidate AdBlocker LRU cache on page navigation
                adBlocker.invalidateCache()
                // ★ Browser Disguise Engine v2.0 — 统一编排引擎
                // 按优先级注入 3 层反检测: BrowserKernel → BrowserDisguise → OAuthCompat
                // 冲突解决、诊断探针均由引擎内部处理
                view?.let { wv ->
                    com.webtoapp.core.disguise.BrowserDisguiseEngine.injectOnPageStarted(
                        webView = wv,
                        url = url,
                        disguiseConfig = cachedBrowserDisguiseConfig,
                        cachedDisguiseJs = cachedBrowserDisguiseJs,
                        enableDiagnostic = false // 设为 true 可启用指纹诊断 (debug 用)
                    )
                }
                // Clipboard API 桥接 — Android WebView 不支持 navigator.clipboard，
                // 通过 NativeBridge 桥接到原生 ClipboardManager
                view?.evaluateJavascript(CLIPBOARD_POLYFILL_JS, null)
                // 滚动位置保存 — 在页面离开前保存滚动位置到 sessionStorage
                view?.evaluateJavascript(SCROLL_SAVE_JS, null)
                // 视口适配 — Unity/Canvas 游戏放大裁切修复
                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
                    view?.evaluateJavascript(VIEWPORT_FIT_SCREEN_JS, null)
                }
                // 视口适配 — 自定义视口宽度
                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
                    val customWidth = config.customViewportWidth.coerceIn(320, 3840)
                    val customJs = VIEWPORT_CUSTOM_JS.replace("CUSTOM_WIDTH_PLACEHOLDER", customWidth.toString())
                    view?.evaluateJavascript(customJs, null)
                }
                // C 级性能优化 — DOCUMENT_START 注入 (被动事件监听, 页面可见性回收)
                view?.let { com.webtoapp.core.perf.NativePerfEngine.injectPerfOptimizations(it, com.webtoapp.core.perf.NativePerfEngine.Phase.DOCUMENT_START) }
                // Inject DOCUMENT_START scripts (use passed url parameter, as webView.url might still be old value)
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_START, url) }
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                currentMainFrameUrl = url ?: currentMainFrameUrl
                val elapsed = System.currentTimeMillis() - diagPageStartTime
                android.util.Log.w("DIAG", "═══ PAGE_COMMIT_VISIBLE ═══ +${elapsed}ms requests=$diagRequestCount blocked=$diagBlockedCount url=$url")
                callbacks.onPageCommitVisible(url)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                // SPA navigation (pushState/replaceState) triggers this but NOT onPageFinished.
                // Notify callback so canGoBack/canGoForward state updates in real time.
                callbacks.onUrlChanged(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentMainFrameUrl = url ?: currentMainFrameUrl
                val elapsed = System.currentTimeMillis() - diagPageStartTime
                android.util.Log.w("DIAG", "═══ PAGE_FINISHED ═══ +${elapsed}ms requests=$diagRequestCount blocked=$diagBlockedCount errors=$diagErrorCount url=$url")
                // Clear file retry state on successful page load
                if (url != null && url.startsWith("file://")) {
                    fileRetryCount = 0
                    fileRetryUrl = null
                }
                // Inject DOCUMENT_END scripts
                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_END, url) }
                // 视口适配 — 在 onPageFinished 重新触发 canvas 检测（Unity 异步加载 canvas）
                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
                    // 重置标志让 fitContent() 重新检测（DOM 已完全加载）
                    view?.evaluateJavascript("window.__wtaViewportFitApplied=false;", null)
                    view?.evaluateJavascript(VIEWPORT_FIT_SCREEN_JS, null)
                }
                // 视口适配 — 自定义视口宽度，在页面完全加载后重新注入
                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
                    val customWidth = config.customViewportWidth.coerceIn(320, 3840)
                    val customJs = VIEWPORT_CUSTOM_JS.replace("CUSTOM_WIDTH_PLACEHOLDER", customWidth.toString())
                    view?.evaluateJavascript("window.__wtaViewportCustomApplied=false;", null)
                    view?.evaluateJavascript(customJs, null)
                }
                // C 级性能优化 — DOCUMENT_END 注入 (懒加载, content-visibility, 预连接, 掉帧检测)
                view?.let { com.webtoapp.core.perf.NativePerfEngine.injectPerfOptimizations(it, com.webtoapp.core.perf.NativePerfEngine.Phase.DOCUMENT_END) }
                // 图片加载修复 — 检测并修复因 Referer 防盗链导致的破碎图片
                // 仅对 HTTP/HTTPS 页面生效（本地页面无此问题）
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    view?.evaluateJavascript(IMAGE_REPAIR_JS, null)
                }
                callbacks.onPageFinished(url)
                // 滚动位置恢复 — 从 sessionStorage 恢复之前保存的滚动位置
                view?.evaluateJavascript(SCROLL_RESTORE_JS, null)
                // OAuth 安全网检测 — 在 Google OAuth 页面加载完成后注入检测脚本
                // 检测 Google 客户端渲染的 "This browser or app may not be secure" 错误页
                // 如果检测到，通过 NativeOAuthBridge 通知 Kotlin 层重定向到 Chrome Custom Tab
                if (url != null && OAuthCompatEngine.isOAuthUrl(url)) {
                    view?.evaluateJavascript(OAuthCompatEngine.getOAuthBlockDetectionJs(), null)
                    AppLogger.d("WebViewManager", "OAuth block detection JS injected for: $url")
                }
                // Shields: page lifecycle
                if (::shields.isInitialized) shields.onPageFinished(url)
                
                // AdBlocker: DOCUMENT_END cosmetic re-injection with MutationObserver
                // Many ads are loaded dynamically after DOMContentLoaded, so we need to
                // continuously hide newly inserted ad elements using MutationObserver
                val canInjectAdBlockEnd = !config.disableShields && adBlocker.isEnabled()
                if (canInjectAdBlockEnd && view != null && url != null) {
                    view.postDelayed({
                        if (view.url == url) {
                            val pageHost = extractHostFromUrl(url) ?: ""
                            if (pageHost.isNotEmpty()) {
                                val cosmeticCss = adBlocker.getCosmeticFilterCss(pageHost)
                                if (cosmeticCss.isNotEmpty()) {
                                    val escapedCss = cosmeticCss
                                        .replace("\\", "\\\\")
                                        .replace("'", "\\'")
                                        .replace("\n", "\\n")
                                        .replace("\r", "")
                                    view.evaluateJavascript("""
                                        // AdBlocker: DOCUMENT_END dynamic cosmetic hiding
                                        (function() {
                                            'use strict';
                                            if (window.__wta_cosmetic_observer__) return;
                                            window.__wta_cosmetic_observer__ = true;
                                            
                                            // Re-inject cosmetic CSS if not present
                                            if (!document.querySelector('style[data-wta="cosmetic"]')) {
                                                var style = document.createElement('style');
                                                style.setAttribute('type', 'text/css');
                                                style.setAttribute('data-wta', 'cosmetic');
                                                style.textContent = '$escapedCss';
                                                (document.head || document.documentElement).appendChild(style);
                                            }
                                            
                                            // MutationObserver: hide dynamically inserted ad elements
                                            // Uses requestIdleCallback for non-blocking processing
                                            var selectors = '$escapedCss'.match(/([^{]+)\{/g);
                                            if (selectors && selectors.length > 0) {
                                                var selectorList = selectors.map(function(s) { 
                                                    return s.replace(/\s*\{${'$'}/, '').trim(); 
                                                }).join(',');
                                                
                                                var pending = false;
                                                var observer = new MutationObserver(function() {
                                                    if (pending) return;
                                                    pending = true;
                                                    (window.requestIdleCallback || setTimeout)(function() {
                                                        pending = false;
                                                        try {
                                                            var els = document.querySelectorAll(selectorList);
                                                            for (var i = 0; i < els.length; i++) {
                                                                if (els[i].style.display !== 'none') {
                                                                    els[i].style.setProperty('display', 'none', 'important');
                                                                    els[i].style.setProperty('visibility', 'hidden', 'important');
                                                                }
                                                            }
                                                        } catch(e) { /* selector parse error — skip */ }
                                                    }, { timeout: 100 });
                                                });
                                                
                                                observer.observe(document.documentElement, {
                                                    childList: true, subtree: true
                                                });
                                                
                                                // Auto-disconnect after 30s to avoid permanent performance cost
                                                setTimeout(function() { observer.disconnect(); }, 30000);
                                            }
                                        })();
                                    """.trimIndent(), null)
                                    AppLogger.d("WebViewManager", "DOCUMENT_END cosmetic observer injected for: $pageHost")
                                }
                            }
                        }
                    }, 200)
                }
                
                // Inject DOCUMENT_IDLE scripts (delayed execution)
                // URL 校验：防止快速导航时上一页的脚本在新页面执行
                val finishedUrl = url
                view?.postDelayed({
                    if (view.url == finishedUrl) {
                        injectScripts(view, config.injectScripts, ScriptRunTime.DOCUMENT_IDLE, view.url)
                    }
                }, 500)
                
                // Performance optimization: inject runtime performance script
                if (config.performanceOptimization) {
                    view?.postDelayed({
                        if (view.url == finishedUrl) {
                            val perfScript = com.webtoapp.core.linux.PerformanceOptimizer.generatePerformanceScript()
                            view.evaluateJavascript(perfScript, null)
                            AppLogger.d("WebViewManager", "Performance optimization script injected")
                        }
                    }, 300)
                }
                
                // PWA offline support: inject Service Worker for offline caching
                if (config.pwaOfflineEnabled) {
                    view?.postDelayed({
                        if (view.url == finishedUrl) {
                            val strategy = try {
                                PwaOfflineSupport.CacheStrategy.valueOf(config.pwaOfflineStrategy)
                            } catch (_: Exception) {
                                PwaOfflineSupport.CacheStrategy.NETWORK_FIRST
                            }
                            val offlineConfig = PwaOfflineSupport.OfflineConfig(
                                enabled = true,
                                strategy = strategy
                            )
                            PwaOfflineSupport.injectServiceWorker(view, offlineConfig)
                        }
                    }, 800)
                }
                
                // Cookie 持久化 — 防抖处理，快速导航时只 flush 一次
                view?.removeCallbacks(cookieFlushRunnable)
                view?.postDelayed(cookieFlushRunnable, 3000)
                
                // 确保页面加载后 WebView 仍有焦点，支持键盘输入
                view?.requestFocus()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // ★ DIAG: 记录所有错误（包括子资源）
                val errUrl = request?.url?.toString() ?: "unknown"
                val errCode = error?.errorCode ?: -1
                val errDesc = error?.description?.toString() ?: "unknown"
                val isMain = request?.isForMainFrame == true
                diagErrorCount++
                android.util.Log.w("DIAG", "RECV_ERROR [${if (isMain) "MAIN" else "sub"}] code=$errCode desc=$errDesc url=${errUrl.take(120)}")
                if (request?.isForMainFrame == true) {
                    val errorCode = error?.errorCode ?: -1
                    val rawDescription = error?.description?.toString() ?: "Unknown error"
                    val description = normalizeNetworkErrorDescription(rawDescription)
                    val failedUrl = request.url?.toString()

                    // Skip error handling for about:blank
                    if (failedUrl == null || failedUrl == "about:blank") return

                    if (view != null) {
                        val upgradedUrl = upgradeInsecureHttpUrl(failedUrl)
                        if (upgradedUrl != null && isCleartextBlockedError(errorCode, rawDescription, description)) {
                            AppLogger.d(
                                "WebViewManager",
                                "Auto-recover from cleartext block: $failedUrl -> $upgradedUrl"
                            )
                            view.loadUrl(upgradedUrl)
                            return
                        }
                    }
                    
                    // Auto-retry file:// URLs — files may not be fully extracted yet (race condition)
                    // ERR_ACCESS_DENIED (-1) or ERR_FILE_NOT_FOUND (-6) on file:// are transient
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
                            view.postDelayed({
                                view.loadUrl(failedUrl)
                            }, FILE_RETRY_DELAY_MS)
                            return
                        } else {
                            AppLogger.w(
                                "WebViewManager",
                                "file:// load failed after $FILE_MAX_RETRIES retries: $failedUrl"
                            )
                            // Reset retry state
                            fileRetryCount = 0
                            fileRetryUrl = null
                        }
                    }
                    
                    // Try to show custom error page
                    val manager = errorPageManager
                    if (manager != null && view != null) {
                        val errorHtml = manager.generateErrorPage(errorCode, description, failedUrl)
                        if (errorHtml != null) {
                            lastFailedUrl = failedUrl
                            view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", failedUrl)
                            AppLogger.d("WebViewManager", "Custom error page loaded for: $failedUrl")
                            // Still notify callback for status tracking
                            callbacks.onError(errorCode, description)
                            return
                        }
                    }
                    
                    callbacks.onError(errorCode, description)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val statusCode = errorResponse?.statusCode ?: -1
                    val reason = errorResponse?.reasonPhrase?.takeIf { it.isNotBlank() } ?: "HTTP Error"
                    val failedUrl = request.url?.toString()
                    val description = if (statusCode > 0) "HTTP $statusCode $reason" else reason
                    AppLogger.w("WebViewManager", "Main-frame HTTP error: url=$failedUrl code=$statusCode reason=$reason")

                    // OAuth blocked fallback: if provider still detects WebView despite
                    // our multi-layer disguise (403/400), fall back to system browser.
                    if (failedUrl != null && OAuthCompatEngine.isOAuthBlockedError(statusCode, failedUrl)) {
                        val provider = OAuthCompatEngine.getProviderType(failedUrl)
                        AppLogger.w("WebViewManager", "OAuth [$provider] $statusCode detected — kernel disguise insufficient, falling back to system browser: $failedUrl")
                        view?.stopLoading()
                        if (view?.canGoBack() == true) {
                            view.goBack()
                        }
                        openInSystemBrowser(failedUrl)
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
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                // 检查 Shields 配置中的 SSL 错误处理策略
                if (!config.disableShields && ::shields.isInitialized && shields.isEnabled()) {
                    val sslPolicy = shields.getConfig().sslErrorPolicy

                    when (sslPolicy) {
                        // 策略1: 自动回退到 HTTP — 尝试将 HTTPS 降级为 HTTP
                        com.webtoapp.core.engine.shields.SslErrorPolicy.AUTO_HTTP_FALLBACK -> {
                            // 首先检查是否是 HTTPS 升级导致的错误，回退到原始 HTTP
                            var fallbackUrl = shields.httpsUpgrader.onSslError(error?.url)
                            if (fallbackUrl != null) {
                                handler?.cancel()
                                view?.loadUrl(fallbackUrl)
                                AppLogger.d("WebViewManager", "HTTPS upgrade fallback: $fallbackUrl")
                                return
                            }
                            // 通用 SSL 错误，尝试 HTTP 回退
                            fallbackUrl = shields.httpsUpgrader.tryHttpFallback(error?.url)
                            if (fallbackUrl != null) {
                                handler?.cancel()
                                view?.loadUrl(fallbackUrl)
                                AppLogger.d("WebViewManager", "SSL error fallback to HTTP: $fallbackUrl")
                                return
                            }
                            // 如果无法回退，则拒绝连接
                            handler?.cancel()
                            callbacks.onSslError(error?.toString() ?: "SSL Error")
                            return
                        }

                        // 策略2: 询问用户 — 显示对话框让用户选择是否继续
                        com.webtoapp.core.engine.shields.SslErrorPolicy.ASK_USER -> {
                            // 让用户决定是否继续
                            handler?.cancel()
                            callbacks.onSslError(error?.toString() ?: "SSL Error")
                            return
                        }

                        // 策略3: 阻止连接 — 直接拒绝（最安全）
                        com.webtoapp.core.engine.shields.SslErrorPolicy.BLOCK -> {
                            handler?.cancel()
                            callbacks.onSslError(error?.toString() ?: "SSL Error")
                            return
                        }
                    }
                }

                // 默认行为（Shields 未启用时）
                handler?.cancel()
                callbacks.onSslError(error?.toString() ?: "SSL Error")
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                val didCrash = detail?.didCrash() == true
                val reason = if (didCrash) {
                    "WebView render process crashed"
                } else {
                    "WebView render process was killed"
                }
                AppLogger.e("WebViewManager", "$reason, rendererPriority=${detail?.rendererPriorityAtExit()}")

                view?.let { goneView ->
                    managedWebViews.remove(goneView)
                    goneView.stopLoading()
                    goneView.webChromeClient = null
                    (goneView.parent as? android.view.ViewGroup)?.removeView(goneView)
                    goneView.destroy()
                }

                callbacks.onError(
                    -1003,
                    if (didCrash) {
                        "WebView render process crashed. Please reopen the page."
                    } else {
                        "WebView render process was killed due to memory pressure. Please reopen the page."
                    }
                )
                // Notify UI to recreate WebView and reload
                callbacks.onRenderProcessGone(didCrash)
                return true
            }
        }
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
     * Only apply network-level blocking to third-party sub-resource requests.
     * This avoids breaking first-party scripts/styles for strict websites.
     */
    private fun isThirdPartySubResourceRequest(request: WebResourceRequest): Boolean {
        if (request.isForMainFrame) return false

        val requestHost = extractHostFromUrl(request.url?.toString()) ?: return false
        if (requestHost in LOCAL_CLEARTEXT_HOSTS) return false

        // IMPORTANT: shouldInterceptRequest may run on WebView background thread.
        // Never call WebView APIs (e.g. view.url) here; use cached main-frame URL + headers.
        val topLevelHost = extractHostFromUrl(currentMainFrameUrl)
            ?: extractHostFromUrl(request.requestHeaders["Referer"])
            ?: extractHostFromUrl(request.requestHeaders["referer"])
            ?: return false

        return !isSameSiteHost(requestHost, topLevelHost)
    }

    /**
     * Check if a URL is a request to a well-known map tile server.
     * These must never be blocked by ad/tracker filters as they are essential
     * for map libraries like Leaflet, Mapbox, Google Maps, etc.
     */
    private fun isMapTileRequest(url: String): Boolean {
        val host = extractHostFromUrl(url) ?: return false
        return MAP_TILE_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }

    /**
     * For remote sites, prefer conservative compatibility/shields JS injection.
     * These pages are often sensitive to prototype monkey-patching.
     */
    private fun shouldUseConservativeScriptMode(pageUrl: String?): Boolean {
        val url = pageUrl?.takeIf { it.isNotBlank() } ?: return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return host !in LOCAL_CLEARTEXT_HOSTS
    }

    /**
     * Strict mode for high-friction anti-automation sites.
     * In this mode we disable aggressive JS/runtime/network hooks.
     */
    private fun shouldUseScriptlessMode(pageUrl: String?): Boolean {
        val url = pageUrl?.takeIf { it.isNotBlank() } ?: return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return STRICT_COMPAT_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }

    /**
     * Check if a URL is an OAuth / sign-in page that may block embedded WebViews.
     * Delegates to OAuthCompatEngine which covers 16+ providers.
     *
     * @deprecated Use OAuthCompatEngine.isOAuthUrl() directly
     */
    private fun isGoogleOAuthUrl(url: String): Boolean = OAuthCompatEngine.isOAuthUrl(url)

    /**
     * Open a URL in the system browser (Chrome, default browser, etc.).
     * Used for OAuth flows that block embedded WebViews.
     */
    private fun openInSystemBrowser(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to open system browser for OAuth: $url", e)
        }
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
        try {
            val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_OFF)
                .build()
            customTabsIntent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, Uri.parse(url))
            AppLogger.i("WebViewManager", "Opened OAuth URL in Chrome Custom Tab: ${url.take(80)}")
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "Chrome Custom Tab failed, falling back to system browser", e)
            openInSystemBrowser(url)
        }
    }

    private fun shouldBypassAggressiveNetworkHooks(request: WebResourceRequest, requestUrl: String): Boolean {
        if (request.isForMainFrame) {
            return shouldUseScriptlessMode(requestUrl)
        }

        val topLevelUrl = currentMainFrameUrl
            ?: request.requestHeaders["Referer"]
            ?: request.requestHeaders["referer"]
            ?: return false

        return shouldUseScriptlessMode(topLevelUrl)
    }

    private fun isSameSiteHost(hostA: String, hostB: String): Boolean {
        if (hostA == hostB) return true
        if (hostA.endsWith(".$hostB") || hostB.endsWith(".$hostA")) return true

        val rootA = getRegistrableDomain(hostA) ?: return false
        val rootB = getRegistrableDomain(hostB) ?: return false
        return rootA == rootB
    }

    // 预编译的 IP 地址正则 — 避免每次调用 getRegistrableDomain 都重新编译
    private val IP_ADDRESS_REGEX = Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")

    private fun getRegistrableDomain(host: String): String? {
        val normalized = host.lowercase().trim('.')
        if (normalized.isBlank()) return null
        if (IP_ADDRESS_REGEX.matches(normalized)) return normalized

        val parts = normalized.split('.')
        if (parts.size <= 2) return normalized

        val suffix2 = parts.takeLast(2).joinToString(".")
        return if (suffix2 in COMMON_SECOND_LEVEL_TLDS && parts.size >= 3) {
            parts.takeLast(3).joinToString(".")
        } else {
            parts.takeLast(2).joinToString(".")
        }
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
     * Cleartext proxy — fetches HTTP resources that are blocked by Android's
     * network security config (ERR_CLEARTEXT_NOT_PERMITTED).
     *
     * Use case: m3u8 video streams, HTTP sub-resources (images, scripts) loaded
     * from HTTPS pages, where the app's cleartextTrafficPermitted="false" policy
     * would otherwise block them.
     *
     * @param request Original WebResourceRequest (must be HTTP)
     * @return WebResourceResponse with the fetched content, or null on error
     */
    private fun fetchCleartextResource(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()

            val method = request.method?.uppercase() ?: "GET"
            val body: okhttp3.RequestBody? = if (method == "POST" || method == "PUT" || method == "PATCH")
                okhttp3.RequestBody.create(null, ByteArray(0)) else null

            val okRequestBuilder = Request.Builder()
                .url(url)
                .method(method, body)

            request.requestHeaders?.forEach { (key, value) ->
                if (key.lowercase() !in SKIP_HEADERS) {
                    okRequestBuilder.addHeader(key, value)
                }
            }

            val okRequest = okRequestBuilder.build()

            val okResponse: Response = cleartextProxyClient.newCall(okRequest).execute()
            val responseCode = okResponse.code
            val responseMessage = okResponse.message.ifBlank { "OK" }

            val contentType = okResponse.header("Content-Type") ?: "application/octet-stream"
            val mimeType = contentType.split(";").firstOrNull() ?: "application/octet-stream"
            val charset = contentType.substringAfter("charset=", "").ifBlank { "UTF-8" }

            // Build response headers map
            val responseHeaders = mutableMapOf<String, String>()
            okResponse.headers.forEach { (name, value) ->
                responseHeaders[name] = value
            }

            // Get response body as InputStream
            // For streaming content (m3u8), use the raw source directly
            val inputStream = if (responseCode in 200..299) {
                okResponse.body?.byteStream() ?: ByteArrayInputStream(ByteArray(0))
            } else {
                okResponse.body?.byteStream() ?: ByteArrayInputStream(ByteArray(0))
            }

            AppLogger.d("WebViewManager", "CleartextProxy fetched: $url -> $responseCode")

            WebResourceResponse(
                mimeType,
                charset,
                responseCode,
                responseMessage,
                responseHeaders,
                inputStream
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "CleartextProxy failed: ${request.url}", e)
            null // Fall through to let WebView try its own handling
        }
    }

    /**
     * Fetch resource with Cross-Origin Isolation headers
     * Required for SharedArrayBuffer / FFmpeg.wasm support
     * 
     * @param request Original WebResourceRequest
     * @return WebResourceResponse with COOP/COEP headers, or null on error
     */
    private fun fetchWithCrossOriginHeaders(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            
            // Copy request headers
            request.requestHeaders?.forEach { (key, value) ->
                if (key.lowercase() !in SKIP_HEADERS) {
                    connection.setRequestProperty(key, value)
                }
            }
            
            connection.requestMethod = request.method ?: "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            val mimeType = connection.contentType?.split(";")?.firstOrNull() ?: "application/octet-stream"
            val encoding = connection.contentEncoding ?: "UTF-8"
            
            // Build response headers with Cross-Origin Isolation
            val responseHeaders = mutableMapOf<String, String>()
            connection.headerFields?.forEach { (key, values) ->
                if (key != null && values.isNotEmpty()) {
                    responseHeaders[key] = values.first()
                }
            }
            
            // Add Cross-Origin Isolation headers (required for SharedArrayBuffer)
            responseHeaders["Cross-Origin-Opener-Policy"] = "same-origin"
            responseHeaders["Cross-Origin-Embedder-Policy"] = "require-corp"
            // Also add CORS headers for sub-resources
            responseHeaders["Cross-Origin-Resource-Policy"] = "cross-origin"
            
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: ByteArrayInputStream(ByteArray(0))
            }
            
            AppLogger.d("WebViewManager", "CrossOriginIsolation fetch: $url -> $responseCode")
            
            WebResourceResponse(
                mimeType,
                encoding,
                responseCode,
                connection.responseMessage ?: "OK",
                responseHeaders,
                inputStream
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "CrossOriginIsolation fetch failed: ${request.url}", e)
            null // Let system handle on error
        }
    }
    
    /**
     * Load encrypted asset resource
     * If encrypted, decrypt and return; otherwise return original
     * 
     * @param assetPath asset path (without file:///android_asset/ prefix)
     * @return WebResourceResponse or null (let system handle)
     */
    private fun loadEncryptedAsset(assetPath: String): WebResourceResponse? {
        return try {
            val secureLoader = SecureAssetLoader.getInstance(context)
            
            // Check if resource exists (encrypted or unencrypted)
            if (!secureLoader.assetExists(assetPath)) {
                AppLogger.d("WebViewManager", "Resource not found: $assetPath")
                return null
            }
            
            // Load resource (auto-handle encrypted/unencrypted)
            val data = secureLoader.loadAsset(assetPath)
            val mimeType = getMimeType(assetPath)
            val encoding = if (isTextMimeType(mimeType)) "UTF-8" else null
            
            AppLogger.d("WebViewManager", "Load resource: $assetPath (${data.size} bytes, $mimeType)")
            
            WebResourceResponse(
                mimeType,
                encoding,
                ByteArrayInputStream(data)
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to load resource: $assetPath", e)
            null
        }
    }
    
    /**
     * Get MIME type by file extension
     */
    private fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return MIME_TYPE_MAP[extension] ?: "application/octet-stream"
    }
    
    /**
     * Check if MIME type is text
     */
    private fun isTextMimeType(mimeType: String): Boolean {
        return mimeType in TEXT_MIME_TYPES
    }

    /**
     * Infer resource type from WebResourceRequest for extension webRequest/DNR filtering.
     * Maps Accept header and URL extension to Chrome resource type strings.
     */
    private fun inferResourceType(request: WebResourceRequest): String {
        // Main frame
        if (request.isForMainFrame) return "main_frame"

        // Check Accept header
        val accept = request.requestHeaders?.entries?.firstOrNull {
            it.key.equals("Accept", ignoreCase = true)
        }?.value ?: ""

        if (accept.contains("text/html")) return "sub_frame"
        if (accept.contains("text/css")) return "stylesheet"
        if (accept.contains("image/")) return "image"
        if (accept.contains("font/") || accept.contains("application/font")) return "font"

        // Check URL extension
        val url = request.url?.toString() ?: ""
        val ext = url.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
        return when (ext) {
            "js", "mjs" -> "script"
            "css" -> "stylesheet"
            "png", "jpg", "jpeg", "gif", "webp", "svg", "ico" -> "image"
            "woff", "woff2", "ttf", "otf", "eot" -> "font"
            "html", "htm" -> "sub_frame"
            "json", "xml" -> "xmlhttprequest"
            "mp3", "wav", "ogg", "mp4", "webm" -> "media"
            else -> "other"
        }
    }

    /**
     * Create WebChromeClient
     */
    private fun createWebChromeClient(config: WebViewConfig, callbacks: WebViewCallbacks): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                callbacks.onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                callbacks.onTitleChanged(title)
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                callbacks.onIconReceived(icon)
            }

            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                callbacks.onShowCustomView(view, callback)
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                callbacks.onHideCustomView()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callbacks.onGeolocationPermission(origin, callback)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                AppLogger.d("WebViewManager", "onPermissionRequest called: ${request?.resources?.joinToString()}")
                if (request != null) {
                    callbacks.onPermissionRequest(request)
                } else {
                    AppLogger.w("WebViewManager", "onPermissionRequest: request is null!")
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val level = when (it.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> 4
                        ConsoleMessage.MessageLevel.WARNING -> 3
                        ConsoleMessage.MessageLevel.LOG -> 1
                        ConsoleMessage.MessageLevel.DEBUG -> 0
                        else -> 2
                    }
                    callbacks.onConsoleMessage(
                        level,
                        it.message() ?: "",
                        it.sourceId() ?: "unknown",
                        it.lineNumber()
                    )
                }
                return true
            }

            // File chooser
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return callbacks.onShowFileChooser(filePathCallback, fileChooserParams)
            }
            
            // Handle window.open / target="_blank"
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                if (view == null) return false
                
                // Try to get clicked link URL
                val href = view.hitTestResult.extra
                
                AppLogger.d("WebViewManager", "onCreateWindow: href=$href, behavior=${config.newWindowBehavior}")
                
                // Save reference to original WebView
                val originalWebView = view
                
                return when (config.newWindowBehavior) {
                    NewWindowBehavior.SAME_WINDOW -> {
                        // Open in current window - extract new window URL and load
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {
                            // Create temporary WebView to get URL
                            val tempWebView = WebView(context)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString()
                                    if (url != null) {
                                        val safeUrl = normalizeHttpUrlForSecurity(url)
                                        // Load in original WebView
                                        originalWebView.loadUrl(safeUrl)
                                        tempView?.destroy()
                                    }
                                    return true
                                }
                            }
                            transport.webView = tempWebView
                            resultMsg.sendToTarget()
                        }
                        true
                    }
                    NewWindowBehavior.EXTERNAL_BROWSER -> {
                        // Open in external browser
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {
                            val tempWebView = WebView(context)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString()
                                    if (url != null) {
                                        try {
                                            val safeUrl = normalizeHttpUrlForSecurity(url)
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(safeUrl))
                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            AppLogger.e("WebViewManager", "Cannot open external browser: $url", e)
                                        }
                                        tempView?.destroy()
                                    }
                                    return true
                                }
                            }
                            transport.webView = tempWebView
                            resultMsg.sendToTarget()
                        }
                        true
                    }
                    NewWindowBehavior.POPUP_WINDOW -> {
                        // Popup new window - call callback to let app handle
                        callbacks.onNewWindow(resultMsg)
                        true
                    }
                    NewWindowBehavior.BLOCK -> {
                        // Block opening
                        false
                    }
                }
            }
            
            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                AppLogger.d("WebViewManager", "onCloseWindow")
            }
        }
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
        resetStrictHostSessionState(webView, pageUrl)
        applyStrictHostRuntimePolicy(webView, pageUrl)
    }

    private fun resetStrictHostSessionState(webView: WebView, pageUrl: String?) {
        if (!shouldUseScriptlessMode(pageUrl)) return

        webView.clearCache(true)
        webView.clearHistory()

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()

        val origins = buildStrictHostOrigins(pageUrl)
        if (origins.isNotEmpty()) {
            val webStorage = WebStorage.getInstance()
            origins.forEach { origin ->
                webStorage.deleteOrigin(origin)
            }
        }

        AppLogger.d("WebViewManager", "Strict host session reset applied for $pageUrl")
    }

    private fun buildStrictHostOrigins(pageUrl: String?): Set<String> {
        val host = extractHostFromUrl(pageUrl) ?: return emptySet()
        val baseHost = host.removePrefix("www.")
        val hosts = linkedSetOf(host, baseHost, "www.$baseHost")
        return hosts
            .filter { it.isNotBlank() }
            .flatMap { targetHost -> listOf("https://$targetHost", "http://$targetHost") }
            .toSet()
    }

    private fun applyStrictHostRuntimePolicy(webView: WebView, pageUrl: String?) {
        if (!shouldUseScriptlessMode(pageUrl)) return

        val settings = webView.settings
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.removeJavascriptInterface("NativeShareBridge")
        webView.removeJavascriptInterface("AndroidDownload")
        webView.removeJavascriptInterface("NativeBridge")
        applyRequestedWithHeaderAllowListForStrictHost(settings)

        val desktopRequested = isDesktopUaRequested(currentConfig)
        val strictMobileUA = STRICT_COMPAT_MOBILE_USER_AGENT ?: STRICT_COMPAT_MOBILE_UA_FALLBACK
        if (!desktopRequested && settings.userAgentString != strictMobileUA) {
            settings.userAgentString = strictMobileUA
            AppLogger.d("WebViewManager", "Strict host policy: force strict mobile UA for $pageUrl")
        } else if (desktopRequested) {
            AppLogger.d("WebViewManager", "Strict host policy: keep desktop UA by user request for $pageUrl")
        }

        AppLogger.d(
            "WebViewManager",
            "Strict host runtime policy applied: url=$pageUrl, thirdPartyCookie=true, jsInterfacesRemoved=true"
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
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false
        
        // http/https handled by WebView
        if (scheme == "http" || scheme == "https") {
            return false
        }
        
        // Allow file:// navigation when current page is also file://
        // This is essential for HTML/FRONTEND apps that load via file:///android_asset/
        // and need to navigate between local pages (e.g. index.html -> login.html)
        if (scheme == "file") {
            val currentUrl = currentMainFrameUrl
            if (currentUrl != null && currentUrl.startsWith("file://")) {
                AppLogger.d("WebViewManager", "Allowing file:// same-origin navigation: $url")
                return false
            }
        }
        
        if (scheme in BLOCKED_SPECIAL_SCHEMES) {
            AppLogger.w("WebViewManager", "Blocked dangerous scheme navigation: $scheme")
            return true
        }

        if (!isUserGesture &&
            (shouldUseScriptlessMode(currentMainFrameUrl ?: url) || isBackgroundBridgeScheme(uri))) {
            AppLogger.d("WebViewManager", "Ignore non-user special scheme in strict mode: $url")
            return true
        }
        
        val paymentSchemesEnabled = currentConfig?.enablePaymentSchemes ?: true
        if (!paymentSchemesEnabled && scheme in PAYMENT_SCHEMES) {
            AppLogger.w("WebViewManager", "Payment scheme blocked by config: $scheme")
            return true
        }
        
        AppLogger.d("WebViewManager", "Handling special URL: $url (scheme=$scheme)")
        
        return try {
            val intent = when (scheme) {
                "intent" -> {
                    // intent:// URLs need Intent.parseUri to parse
                    // Common format used by download managers like 1DM, ADM
                    try {
                        val parsedIntent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                        val targetScheme = parsedIntent.data?.scheme?.lowercase()
                        if (targetScheme in BLOCKED_SPECIAL_SCHEMES) {
                            AppLogger.w("WebViewManager", "Blocked dangerous target scheme in intent:// URL: $targetScheme")
                            null
                        } else if (!paymentSchemesEnabled && targetScheme in PAYMENT_SCHEMES) {
                            AppLogger.w("WebViewManager", "Payment target scheme blocked by config in intent:// URL: $targetScheme")
                            null
                        } else {
                            parsedIntent.apply {
                                dataString?.let { original ->
                                    val safeUrl = normalizeHttpUrlForSecurity(original)
                                    if (safeUrl != original) {
                                        data = Uri.parse(safeUrl)
                                    }
                                }
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                // Add BROWSABLE category for security and compatibility
                                addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                                // Also add to selector if present
                                selector?.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                            }
                        }
                    } catch (e: java.net.URISyntaxException) {
                        AppLogger.e("WebViewManager", "Invalid intent URI: $url", e)
                        null
                    }
                }
                else -> {
                    // Other protocols (tel:, mailto:, sms:, etc.) use ACTION_VIEW
                    android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            
            if (intent != null) {
                // Get fallback URL first (for intent:// scheme)
                val fallbackUrl = if (scheme == "intent") {
                    sanitizeFallbackUrl(intent.getStringExtra("browser_fallback_url"))
                } else null
                
                // Try to launch the intent
                // On Android 11+, resolveActivity may return null due to package visibility
                // So we try to launch directly and catch ActivityNotFoundException
                try {
                    // First check if we can resolve it (works for declared packages in queries)
                    val resolveInfo = context.packageManager.resolveActivity(
                        intent, 
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )
                    
                    if (resolveInfo != null) {
                        AppLogger.d("WebViewManager", "Resolved activity: ${resolveInfo.activityInfo?.packageName}")
                        context.startActivity(intent)
                        return true
                    }
                    
                    // If resolveActivity returns null, still try to launch
                    // This handles cases where the target app isn't in queries but can still be launched
                    AppLogger.d("WebViewManager", "resolveActivity returned null, trying direct launch")
                    context.startActivity(intent)
                    return true
                    
                } catch (e: android.content.ActivityNotFoundException) {
                    AppLogger.w("WebViewManager", "No activity found for intent", e)
                    // Use fallback URL if available
                    if (!fallbackUrl.isNullOrEmpty()) {
                        AppLogger.d("WebViewManager", "Using fallback URL: $fallbackUrl")
                        // Load fallback URL in WebView
                        managedWebViews.keys.firstOrNull()?.loadUrl(fallbackUrl)
                        return true
                    }
                    // No fallback, return true to prevent ERR_UNKNOWN_URL_SCHEME
                    return true
                } catch (e: SecurityException) {
                    AppLogger.e("WebViewManager", "Security exception launching intent", e)
                    // Use fallback URL if available
                    if (!fallbackUrl.isNullOrEmpty()) {
                        AppLogger.d("WebViewManager", "Using fallback URL after security error: $fallbackUrl")
                        managedWebViews.keys.firstOrNull()?.loadUrl(fallbackUrl)
                        return true
                    }
                    return true
                }
            }
            true
        } catch (e: Exception) {
            // No app can handle this protocol, fail silently
            AppLogger.w("WebViewManager", "Error handling special URL: $scheme", e)
            true // Return true to prevent WebView loading, avoid ERR_UNKNOWN_URL_SCHEME
        }
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
        try {
            managedWebViews.remove(webView)
            
            webView.apply {
                // Stop loading
                stopLoading()
                
                // Clear history and cache
                clearHistory()
                
                // Remove all callbacks
                webChromeClient = null
                webViewClient = object : WebViewClient() {}
                
                // Clear JavaScript interfaces
                removeJavascriptInterface("NativeBridge")
                removeJavascriptInterface("DownloadBridge")
                removeJavascriptInterface("NativeShareBridge")
                removeJavascriptInterface(com.webtoapp.core.extension.GreasemonkeyBridge.JS_INTERFACE_NAME)
                removeJavascriptInterface(com.webtoapp.core.extension.ChromeExtensionRuntime.JS_BRIDGE_NAME)
                
                // Note: do NOT loadUrl("about:blank") here.
                // It causes a visible flash and on some Android versions
                // prevents localStorage from being flushed to disk.
                
                // Remove from parent view
                (parent as? android.view.ViewGroup)?.removeView(this)
                
                // Destroy WebView
                destroy()
            }
            
            AppLogger.d("WebViewManager", "WebView resources cleaned up")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to cleanup WebView", e)
        }
    }
    
    /**
     * Clean up all managed WebViews
     */
    fun destroyAll() {
        managedWebViews.keys.toList().forEach { webView ->
            destroyWebView(webView)
        }
        managedWebViews.clear()
        gmBridge?.destroy()
        gmBridge = null
        // Destroy Chrome Extension background runtimes
        extensionRuntimes.values.forEach { it.destroy() }
        extensionRuntimes.clear()
    }
    
    /**
     * Get BrowserShields instance for external access (UI, settings, etc.)
     */
    fun getShields(): BrowserShields? = if (::shields.isInitialized) shields else null
    
    // Save config reference (for script injection)
    private var currentConfig: WebViewConfig? = null
    
    // Browser Disguise — pre-generated anti-fingerprint JS (cached per configureWebView call)
    private var cachedBrowserDisguiseJs: String? = null
    // Browser Disguise — config reference (for BrowserDisguiseEngine integration)
    private var cachedBrowserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null
    // Device Disguise — config reference (for device type/brand/model UA spoofing)
    private var currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null
    
    /**
     * Initialize Chrome Extension background script runtimes.
     * For each enabled Chrome extension with a background script, creates a hidden WebView
     * that runs the background script with same-origin access and cookie sharing.
     */
    private fun initChromeExtensionRuntimes(webView: WebView) {
        // Destroy previous runtimes
        extensionRuntimes.values.forEach { it.destroy() }
        extensionRuntimes.clear()
        
        try {
            val chromeExtModules = getActiveModulesForCurrentApp().filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.chromeExtId.isNotEmpty() &&
                module.backgroundScript.isNotEmpty()
            }
            
            if (chromeExtModules.isEmpty()) return
            
            // Group by chromeExtId — one runtime per extension
            val extensionGroups = chromeExtModules.groupBy { it.chromeExtId }
            
            for ((extId, modules) in extensionGroups) {
                val primaryModule = modules.first()
                val originUrl = com.webtoapp.core.extension.deriveOriginUrl(primaryModule.urlMatches)
                
                val runtime = com.webtoapp.core.extension.ChromeExtensionRuntime(
                    context = context,
                    extensionId = extId,
                    backgroundScriptPath = primaryModule.backgroundScript,
                    originUrl = originUrl
                )
                runtime.initialize(webView)
                extensionRuntimes[extId] = runtime
                AppLogger.d("WebViewManager", "Created background runtime for extension: $extId")
            }
            
            // Register content-side bridge on main WebView for message routing
            if (extensionRuntimes.isNotEmpty()) {
                val contentBridge = com.webtoapp.core.extension.ContentExtensionBridge(extensionRuntimes)
                webView.addJavascriptInterface(contentBridge, com.webtoapp.core.extension.ChromeExtensionRuntime.JS_BRIDGE_NAME)
                AppLogger.d("WebViewManager", "Registered WtaExtBridge for ${extensionRuntimes.size} extension(s)")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to init Chrome Extension runtimes", e)
        }
    }
    
    /**
     * Inject user scripts
     * @param webView WebView instance
     * @param scripts User script list
     * @param runAt Run timing
     * @param pageUrl Current page URL (optional, gets from webView if not provided)
     */
    private fun injectScripts(webView: WebView, scripts: List<com.webtoapp.data.model.UserScript>, runAt: ScriptRunTime, pageUrl: String? = null) {
        // Prioritize passed pageUrl, because webView.url might still be old value at onPageStarted
        val url = pageUrl ?: webView.url ?: ""
        val conservativeMode = shouldUseConservativeScriptMode(url)
        val scriptlessMode = shouldUseScriptlessMode(url)

        // Inject download bridge script at DOCUMENT_START (ensure earliest injection)
        if (runAt == ScriptRunTime.DOCUMENT_START) {
            if (!conservativeMode && currentConfig?.downloadEnabled == true) {
                injectDownloadBridgeScript(webView)
            } else if (conservativeMode) {
                AppLogger.d("WebViewManager", "Skip download bridge for conservative page: $url")
            }

            // Inject unified extension panel script
            if (!scriptlessMode) {
                injectExtensionPanelScript(webView)
            }

            // Inject isolation environment script (earliest injection to ensure fingerprint spoofing works)
            if (!conservativeMode) {
                injectIsolationScript(webView)
            }

            // Inject browser compatibility scripts
            if (!scriptlessMode) {
                injectCompatibilityScripts(webView, url, conservativeMode)
            } else {
                AppLogger.d("WebViewManager", "Scriptless mode enabled for strict host: $url")
            }
        }

        if (scriptlessMode) {
            AppLogger.d("WebViewManager", "Scriptless mode: skip user/module injections (${runAt.name})")
            return
        }
        
        // Inject user custom scripts
        scripts.filter { it.enabled && it.runAt == runAt && it.code.isNotBlank() }
            .forEach { script ->
                try {
                    // Wrap script, add error handling
                    val wrappedCode = """
                        (function() {
                            try {
                                ${script.code}
                            } catch(e) {
                                console.error('[UserScript: ${script.name}] Error:', e);
                            }
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(wrappedCode, null)
                    AppLogger.d("WebViewManager", "Inject script: ${script.name} (${runAt.name})")
                } catch (e: Exception) {
                    AppLogger.e("WebViewManager", "Script injection failed: ${script.name}", e)
                }
            }
        
        // Unified extension module injection (Chrome extensions, userscripts, custom modules)
        injectAllExtensionModules(webView, url, runAt)
    }
    
    /**
     * Inject download bridge script
     * Intercept Blob/Data URL downloads and forward to native code
     */
    private fun injectDownloadBridgeScript(webView: WebView) {
        try {
            val script = DownloadBridge.getInjectionScript()
            webView.evaluateJavascript(script, null)
            AppLogger.d("WebViewManager", "Download bridge script injected")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Download bridge script injection failed", e)
        }
    }
    
    /**
     * Inject unified extension panel script
     * Provide unified UI panel, all extension module UI displayed in this panel
     * Only inject when extension modules are enabled
     */
    private fun injectExtensionPanelScript(webView: WebView) {
        // Check if any modules (including Chrome extensions) are active.
        // The management panel now shows ALL extension types with full info.
        val hasEmbeddedModules = embeddedModules.any { it.enabled }
        val hasAppModules = try {
            if (appExtensionModuleIds.isNotEmpty()) {
                val extensionManager = ExtensionManager.getInstance(context)
                extensionManager.getModulesByIds(appExtensionModuleIds).isNotEmpty()
            } else false
        } catch (e: Exception) { false }
        val hasGlobalModules = try {
            ExtensionManager.getInstance(context).getEnabledModules().isNotEmpty()
        } catch (e: Exception) { false }
        
        // Only inject panel if there are any enabled modules
        if (!hasEmbeddedModules && !hasAppModules && !hasGlobalModules) {
            AppLogger.d("WebViewManager", "No enabled modules, skip panel script injection")
            return
        }
        
        try {
            // Inject面板初始化脚本
            val panelScript = ExtensionPanelScript.getPanelInitScript(extensionFabIcon)
            webView.evaluateJavascript(panelScript, null)
            
            // Inject模块辅助脚本
            val helperScript = ExtensionPanelScript.getModuleHelperScript()
            webView.evaluateJavascript(helperScript, null)
            
            AppLogger.d("WebViewManager", "Extension panel script injected")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Extension panel script injection failed", e)
        }
    }
    
    /**
     * Inject isolation environment script
     * For anti-detection, fingerprint spoofing, etc.
     */
    private fun injectIsolationScript(webView: WebView) {
        try {
            val isolationManager = com.webtoapp.core.isolation.IsolationManager.getInstance(context)
            val script = isolationManager.generateIsolationScript()
            
            if (script.isNotEmpty()) {
                webView.evaluateJavascript(script, null)
                AppLogger.d("WebViewManager", "Isolation script injected")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Isolation script injection failed", e)
        }
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
    
    // ==================== Unified Extension Module Injection ====================
    
    /**
     * Convert ScriptRunTime to ModuleRunTime
     */
    private fun ScriptRunTime.toModuleRunTime(): ModuleRunTime = when (this) {
        ScriptRunTime.DOCUMENT_START -> ModuleRunTime.DOCUMENT_START
        ScriptRunTime.DOCUMENT_END -> ModuleRunTime.DOCUMENT_END
        ScriptRunTime.DOCUMENT_IDLE -> ModuleRunTime.DOCUMENT_IDLE
    }
    
    /**
     * Resolve active ExtensionModule instances for the current app.
     * Returns app-configured or globally-enabled modules (NOT embedded Shell modules).
     */
    private fun resolveActiveExtensionModules(): List<com.webtoapp.core.extension.ExtensionModule> {
        return when {
            appExtensionModuleIds.isNotEmpty() -> {
                ExtensionManager.getInstance(context).getModulesByIds(appExtensionModuleIds)
            }
            allowGlobalModuleFallback -> {
                ExtensionManager.getInstance(context).getEnabledModules()
            }
            else -> emptyList()
        }
    }
    
    /**
     * Unified extension module injection dispatcher.
     * Resolves modules once, filters once, and dispatches to type-specific injection helpers.
     * Panel registration happens once at DOCUMENT_END for all module types.
     *
     * Embedded modules (Shell mode) use a separate type (EmbeddedShellModule) and are
     * handled inline here, while ExtensionModule types go through the full dispatch.
     */
    private fun injectAllExtensionModules(webView: WebView, url: String, runAt: ScriptRunTime) {
        // Shell mode: embedded modules are a separate type, handle them directly
        if (embeddedModules.isNotEmpty()) {
            injectEmbeddedModules(webView, url, runAt)
            return
        }
        
        val moduleRunAt = runAt.toModuleRunTime()
        
        // 1. Resolve ALL active modules ONCE
        val allModules = resolveActiveExtensionModules()
        if (allModules.isEmpty()) {
            AppLogger.d("WebViewManager", "injectAllExtensionModules: No active modules (${runAt.name})")
            return
        }
        
        AppLogger.d("WebViewManager", "injectAllExtensionModules: runAt=${runAt.name}, url=$url, totalModules=${allModules.size}")
        
        // 2. DOCUMENT_START special: early CSS for Chrome extensions (FOUC prevention)
        //    Injects CSS from Chrome extension modules that run at LATER timings
        if (runAt == ScriptRunTime.DOCUMENT_START) {
            injectEarlyCss(webView, allModules, url, moduleRunAt)
        }
        
        // 3. Filter for current runAt + URL match
        val matching = allModules.filter { it.runAt == moduleRunAt && it.matchesUrl(url) }
        
        // 4. Group by sourceType and dispatch to type-specific injection helpers
        val chromeModules = matching.filter {
            it.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
            it.chromeExtId.isNotEmpty()
        }
        val userscriptModules = matching.filter {
            it.sourceType == com.webtoapp.core.extension.ModuleSourceType.USERSCRIPT
        }
        val customModules = matching.filter {
            it.sourceType != com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
            it.sourceType != com.webtoapp.core.extension.ModuleSourceType.USERSCRIPT
        }
        
        if (chromeModules.isNotEmpty()) {
            injectChromeExtModules(webView, chromeModules, runAt)
        }
        if (userscriptModules.isNotEmpty()) {
            injectUserscriptModules(webView, userscriptModules)
        }
        if (customModules.isNotEmpty()) {
            injectCustomModules(webView, customModules)
        }
        
        AppLogger.d("WebViewManager", "injectAllExtensionModules: Injected ${chromeModules.size} chrome + ${userscriptModules.size} userscript + ${customModules.size} custom modules (${runAt.name})")
        
        // 5. Panel registration at DOCUMENT_END (ALL types, not filtered by runAt)
        if (runAt == ScriptRunTime.DOCUMENT_END) {
            registerAllModulesInPanel(webView, allModules, url)
        }
    }
    
    /**
     * Inject embedded extension module code (Shell mode only).
     * Embedded modules use the EmbeddedShellModule type which has its own
     * matchesUrl() and generateExecutableCode() implementations.
     */
    private fun injectEmbeddedModules(webView: WebView, url: String, runAt: ScriptRunTime) {
        try {
            val targetRunAt = runAt.name
            
            AppLogger.d("WebViewManager", "injectEmbeddedModules: url=$url, runAt=$targetRunAt, totalModules=${embeddedModules.size}")
            
            val matchingModules = embeddedModules.filter { module ->
                module.enabled && module.runAt == targetRunAt && module.matchesUrl(url)
            }
            
            if (matchingModules.isEmpty()) {
                AppLogger.d("WebViewManager", "injectEmbeddedModules: No matching modules")
                return
            }
            
            // Each module wrapped independently, error isolation
            val injectionCode = matchingModules.joinToString("\n\n") { module ->
                """
                // ========== ${module.name} ==========
                (function() {
                    try {
                        ${module.generateExecutableCode()}
                    } catch(__moduleError__) {
                        console.error('[WebToApp Module Error] ${module.name}:', __moduleError__);
                    }
                })();
                """.trimIndent()
            }
            
            webView.evaluateJavascript(injectionCode, null)
            AppLogger.d("WebViewManager", "Injected ${matchingModules.size} embedded module(s) (${runAt.name})")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Embedded module injection failed", e)
        }
    }
    
    // ==================== Early CSS Injection ====================
    
    /**
     * Inject CSS early at DOCUMENT_START for Chrome extension modules that run at later timings.
     * Prevents Flash of Unstyled Content (FOUC).
     */
    private fun injectEarlyCss(
        webView: WebView,
        allModules: List<com.webtoapp.core.extension.ExtensionModule>,
        url: String,
        currentRunAt: ModuleRunTime
    ) {
        try {
            val earlyCssModules = allModules.filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.chromeExtId.isNotEmpty() &&
                module.cssCode.isNotBlank() &&
                module.runAt != currentRunAt && // Don't double-inject CSS for modules injected now
                module.matchesUrl(url)
            }
            if (earlyCssModules.isEmpty()) return
            
            val cssBuilder = StringBuilder()
            for (module in earlyCssModules) {
                val extId = module.chromeExtId
                val escapedCss = module.cssCode
                    .replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("\$", "\\\$")
                cssBuilder.appendLine("""
                    (function() {
                        try {
                            var style = document.createElement('style');
                            style.setAttribute('data-wta-ext', '$extId');
                            style.setAttribute('data-wta-early-css', 'true');
                            style.textContent = `$escapedCss`;
                            (document.head || document.documentElement).appendChild(style);
                        } catch(e) { console.warn('[WTA] Early CSS injection error:', e); }
                    })();
                """.trimIndent())
            }
            webView.evaluateJavascript(cssBuilder.toString(), null)
            AppLogger.d("WebViewManager", "Early CSS injected for ${earlyCssModules.size} Chrome extension module(s)")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Early CSS injection failed", e)
        }
    }
    
    // ==================== Chrome Extension Module Injection ====================
    
    /**
     * Inject Chrome extension modules with polyfill, CSS, and content scripts.
     * Handles single vs multi-extension code splitting for performance and isolation.
     * @param modules Pre-filtered Chrome extension modules matching current runAt and URL
     */
    private fun injectChromeExtModules(
        webView: WebView,
        modules: List<com.webtoapp.core.extension.ExtensionModule>,
        runAt: ScriptRunTime
    ) {
        try {
            val extensionGroups = modules.groupBy { it.chromeExtId }
            
            if (extensionGroups.size == 1) {
                // Single extension: inject as one block for performance
                val codeBuilder = StringBuilder()
                
                // Mobile compat layer at DOCUMENT_START (once, before any extension code)
                if (runAt == ScriptRunTime.DOCUMENT_START) {
                    codeBuilder.appendLine(com.webtoapp.core.extension.ChromeExtensionMobileCompat.generateCompatScript())
                    codeBuilder.appendLine()
                }
                
                for ((extId, extModules) in extensionGroups) {
                    // 1. Chrome API polyfill (once per extension)
                    codeBuilder.appendLine(com.webtoapp.core.extension.ChromeExtensionPolyfill.generatePolyfill(extensionId = extId))
                    codeBuilder.appendLine()
                    
                    // 2. CSS injection
                    appendChromeExtCss(codeBuilder, extId, extModules)
                    
                    // 3. Content scripts (MAIN vs ISOLATED world)
                    appendChromeExtScripts(codeBuilder, extModules)
                }
                
                val combinedCode = codeBuilder.toString()
                if (combinedCode.isNotBlank()) {
                    webView.evaluateJavascript(combinedCode, null)
                }
                AppLogger.d("WebViewManager", "Injected Chrome extension polyfills for ${modules.size} module(s) (${runAt.name})")
            } else {
                // Multiple extensions: inject each separately to:
                // 1. Avoid hitting evaluateJavascript size limits with combined 2MB+ code
                // 2. Ensure per-extension polyfill isolation
                // 3. Prevent one extension's parse error from breaking all subsequent extensions
                if (runAt == ScriptRunTime.DOCUMENT_START) {
                    webView.evaluateJavascript(
                        com.webtoapp.core.extension.ChromeExtensionMobileCompat.generateCompatScript(), null
                    )
                }
                for ((extId, extModules) in extensionGroups) {
                    val extBuilder = StringBuilder()
                    extBuilder.appendLine(com.webtoapp.core.extension.ChromeExtensionPolyfill.generatePolyfill(extensionId = extId))
                    appendChromeExtCss(extBuilder, extId, extModules)
                    appendChromeExtScripts(extBuilder, extModules)
                    webView.evaluateJavascript(extBuilder.toString(), null)
                }
                AppLogger.d("WebViewManager", "Injected Chrome extension polyfills for ${modules.size} module(s) across ${extensionGroups.size} extension(s) (${runAt.name})")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Chrome extension module injection failed", e)
        }
    }
    
    /**
     * Append Chrome extension CSS injection code to a StringBuilder.
     */
    private fun appendChromeExtCss(
        builder: StringBuilder,
        extId: String,
        modules: List<com.webtoapp.core.extension.ExtensionModule>
    ) {
        modules.filter { it.cssCode.isNotBlank() }.forEach { module ->
            val escapedCss = module.cssCode
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("\$", "\\\$")
            builder.appendLine("""
                // ===== CSS: ${module.name} =====
                (function() {
                    try {
                        var style = document.createElement('style');
                        style.setAttribute('data-wta-ext', '$extId');
                        style.textContent = `$escapedCss`;
                        (document.head || document.documentElement).appendChild(style);
                    } catch(e) {
                        console.error('[WebToApp Chrome Ext CSS] ${module.name}:', e);
                    }
                })();
            """.trimIndent())
            builder.appendLine()
        }
    }
    
    /**
     * Append Chrome extension content script injection code to a StringBuilder.
     * Handles MAIN vs ISOLATED world wrapping with Vue/Vite build-time global pre-definitions.
     */
    private fun appendChromeExtScripts(
        builder: StringBuilder,
        modules: List<com.webtoapp.core.extension.ExtensionModule>
    ) {
        for (module in modules) {
            if (module.code.isBlank()) continue
            
            if (module.world == "MAIN") {
                // MAIN world: inject directly without isolated wrapper
                builder.appendLine("""
                    // ===== MAIN world: ${module.name} =====
                    (function() {
                        if (typeof __INTLIFY_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__INTLIFY_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_OPTIONS_API__ === 'undefined') { try { Object.defineProperty(window, '__VUE_OPTIONS_API__', { value: true, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_HYDRATION_MISMATCH_DETAILS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                    })();
                    try {
                        ${module.code}
                    } catch(__extError__) {
                        console.error('[WebToApp Chrome Ext Error] ${module.name} (MAIN):', __extError__);
                    }
                """.trimIndent())
            } else {
                // ISOLATED world: wrap in IIFE for variable isolation
                // Chrome's ISOLATED world shares the DOM but has a separate JS namespace.
                builder.appendLine("""
                    // ===== ISOLATED world: ${module.name} =====
                    (function() {
                        if (typeof __INTLIFY_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__INTLIFY_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_DEVTOOLS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_DEVTOOLS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_OPTIONS_API__ === 'undefined') { try { Object.defineProperty(window, '__VUE_OPTIONS_API__', { value: true, writable: true, configurable: true }); } catch(e){/* expected */} }
                        if (typeof __VUE_PROD_HYDRATION_MISMATCH_DETAILS__ === 'undefined') { try { Object.defineProperty(window, '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__', { value: false, writable: true, configurable: true }); } catch(e){/* expected */} }
                        try {
                            ${module.code}
                        } catch(__extError__) {
                            console.error('[WebToApp Chrome Ext Error] ${module.name}:', __extError__);
                        }
                    })();
                """.trimIndent())
            }
            builder.appendLine()
        }
    }
    
    // ==================== Userscript Module Injection ====================
    
    /**
     * Inject userscript modules with GM_* API polyfill.
     * @param modules Pre-filtered userscript modules matching current runAt and URL
     */
    private fun injectUserscriptModules(
        webView: WebView,
        modules: List<com.webtoapp.core.extension.ExtensionModule>
    ) {
        try {
            // Window manager JS (once per page)
            val windowManagerJs = com.webtoapp.core.extension.UserScriptWindowScript.getWindowManagerScript()
            
            // Generate and inject polyfill + userscript code for each module
            val combinedCode = windowManagerJs + "\n\n" + modules.joinToString("\n\n") { module ->
                val scriptInfo = mapOf(
                    "name" to module.name,
                    "version" to module.version.name,
                    "description" to module.description,
                    "author" to (module.author?.name ?: ""),
                    "namespace" to module.id
                )
                
                // Resolve @resource content from cache (name -> actual content instead of URL)
                val resolvedResources = module.resources.mapValues { (name, url) ->
                    extensionFileManager.getCachedResource(name, url) ?: url
                }
                
                val polyfill = com.webtoapp.core.extension.GreasemonkeyBridge.generatePolyfillScript(
                    scriptId = module.id,
                    grants = module.gmGrants,
                    scriptInfo = scriptInfo,
                    resources = resolvedResources
                )
                
                // Collect cached @require JS to prepend before userscript code
                val requireJs = module.requireUrls.mapNotNull { url ->
                    extensionFileManager.getCachedRequire(url)
                }.joinToString("\n\n")
                
                // Wrap: polyfill first, then @require libs, then the userscript code
                """
                // ========== [Userscript] ${module.name} ==========
                (function() {
                    try {
                        $polyfill
                        $requireJs
                        ${module.code}
                    } catch(__usError__) {
                        console.error('[WebToApp Userscript Error] ${module.name}:', __usError__);
                    }
                })();
                """.trimIndent()
            }
            
            webView.evaluateJavascript(combinedCode, null)
            AppLogger.d("WebViewManager", "Injected ${modules.size} userscript module(s) with GM polyfills")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Userscript module injection failed", e)
        }
    }
    
    // ==================== Custom Module Injection ====================
    
    /**
     * Inject custom extension modules using generateExecutableCode().
     * Each module is independently wrapped with error isolation.
     * @param modules Pre-filtered custom modules matching current runAt and URL
     */
    private fun injectCustomModules(
        webView: WebView,
        modules: List<com.webtoapp.core.extension.ExtensionModule>
    ) {
        try {
            val injectionCode = modules.joinToString("\n\n") { module ->
                """
                // ========== ${module.name} (${module.version.name}) ==========
                (function() {
                    try {
                        ${module.generateExecutableCode()}
                    } catch(__moduleError__) {
                        console.error('[WebToApp Module Error] ${module.name}:', __moduleError__);
                    }
                })();
                """.trimIndent()
            }
            
            if (injectionCode.isNotBlank()) {
                webView.evaluateJavascript(injectionCode, null)
                AppLogger.d("WebViewManager", "Injected ${modules.size} custom module(s)")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Custom module injection failed", e)
        }
    }
    
    // ==================== Panel Registration ====================
    
    /**
     * Register ALL active extension modules in the management panel.
     * Called once at DOCUMENT_END, independently of module runAt timing.
     * This ensures DOCUMENT_START modules also appear in the panel.
     * Covers all module types: Chrome extensions, userscripts, and custom modules.
     */
    private fun registerAllModulesInPanel(
        webView: WebView,
        allModules: List<com.webtoapp.core.extension.ExtensionModule>,
        url: String
    ) {
        try {
            if (allModules.isEmpty()) return
            
            // Chrome extensions: register by chromeExtId (deduplicated)
            val chromeModules = allModules.filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.chromeExtId.isNotEmpty()
            }
            
            // Non-Chrome modules (standard + userscript)
            val nonChromeModules = allModules.filter { module ->
                module.sourceType != com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION
            }
            
            if (chromeModules.isEmpty() && nonChromeModules.isEmpty()) return
            
            val registeredExtIds = mutableSetOf<String>()
            val regBuilder = StringBuilder()
            
            // Register Chrome extensions
            for (module in chromeModules) {
                val extId = module.chromeExtId.ifBlank { module.id }
                if (extId in registeredExtIds) continue
                registeredExtIds.add(extId)
                
                // Collect all modules for this extension
                val extModules = chromeModules.filter { 
                    (it.chromeExtId.ifBlank { it.id }) == extId 
                }
                
                // Escape strings for JS single-quoted literals
                val jsName = module.name.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsDesc = (module.description).replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsVersion = module.version.name.replace("\\", "\\\\").replace("'", "\\'")
                val jsAuthor = (module.author?.name ?: "").replace("\\", "\\\\").replace("'", "\\'")
                
                val iconHtml = if (module.icon.isNotBlank()) {
                    module.icon.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "")
                } else ""
                
                val urlPatterns = extModules.flatMap { it.urlMatches }
                    .filter { !it.exclude }
                    .map { it.pattern.replace("\\", "\\\\").replace("'", "\\'") }
                    .distinct()
                val urlMatchesJs = urlPatterns.joinToString(",") { "'$it'" }
                
                val perms = extModules.flatMap { it.permissions }
                    .map { it.name }
                    .distinct()
                val permsJs = perms.joinToString(",") { "'$it'" }
                
                // Check if this extension matches the current page
                val matchesPage = extModules.any { it.matchesUrl(url) }
                
                regBuilder.appendLine("""
                    (function() {
                        function _reg() {
                            if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(_reg, 100); return; }
                            __WTA_MODULE_UI__.register({
                                id: '$extId',
                                name: '$jsName',
                                description: '$jsDesc',
                                version: '$jsVersion',
                                author: '$jsAuthor',
                                icon: '$iconHtml',
                                sourceType: 'CHROME_EXTENSION',
                                active: $matchesPage,
                                urlMatches: [${urlMatchesJs}],
                                permissions: [${permsJs}],
                                world: ""${module.world}"",
                                runAt: ""${module.runAt.name}"",
                                runMode: ""${module.runMode.name}""
                            });
                        }
                        setTimeout(_reg, 50);
                    })();
                """.trimIndent())
            }
            
            // Register all non-Chrome modules (standard + userscript) that are active
            for (module in nonChromeModules) {
                val jsName = module.name.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsDesc = (module.description).replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val jsVersion = module.version.name.replace("\\", "\\\\").replace("'", "\\'")
                val jsAuthor = (module.author?.name ?: "").replace("\\", "\\\\").replace("'", "\\'")
                val iconHtml = if (module.icon.isNotBlank()) {
                    module.icon.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "")
                } else ""
                val matchesPage = module.matchesUrl(url)
                
                regBuilder.appendLine("""
                    (function() {
                        function _reg() {
                            if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(_reg, 100); return; }
                            __WTA_MODULE_UI__.register({
                                id: ""${module.id.replace("'", "\\" + "'")}"",
                                name: '$jsName',
                                description: '$jsDesc',
                                version: '$jsVersion',
                                author: '$jsAuthor',
                                icon: '$iconHtml',
                                sourceType: ""${module.sourceType.name}"",
                                active: $matchesPage,
                                urlMatches: [],
                                permissions: [],
                                world: ""${module.world}"",
                                runAt: ""${module.runAt.name}"",
                                runMode: ""${module.runMode.name}""
                            });
                        }
                        setTimeout(_reg, 50);
                    })();
                """.trimIndent())
            }
            
            if (regBuilder.isNotBlank()) {
                webView.evaluateJavascript(regBuilder.toString(), null)
                AppLogger.d("WebViewManager", "Registered ${registeredExtIds.size} Chrome ext(s) + ${nonChromeModules.size} module(s) in panel")
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Panel registration failed", e)
        }
    }
}
