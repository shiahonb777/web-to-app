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
        // Desktop Chrome User-Agent Chrome from WebView Get.
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

        // Well-known map tile server host suffixes these must NEVER be blocked by
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
         * Viewport Fit-Screen Script Unity WebGL / Canvas large issue.
         *
         * issue .
         * 1. Unity WebGL etc use <canvas> not viewport.
         * 2. Android WebView DPI (devicePixelRatio > 1) by .
         * large UI to.
         *
         * Solution.
         * 1. viewport meta: width=device-width, initial-scale=1.0.
         * 2. <canvas> viewport use CSS transform.
         * 3. when Unity #unity-container / #unity-canvas.
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
                        // Allow cleartext HTTP bypasses Android network security config for this client only
                        okhttp3.ConnectionSpec.CLEARTEXT
                    )
                )
                .retryOnConnectionFailure(true)
                .build()
        }

        /**
         * Viewport Custom Script .
         *
         * viewport meta .
         * use 320-3840 .
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
         * Android WebView goBack() after scroll position issue.
         * Cause: goBack(), onPageStarted in large JS.
         * DOM in restore.
         * before, scroll position Reset to .
         *
         * Approach: sessionStorage in before Savescroll position,.
         * after in onPageFinished restore.
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
         * image loadingfix (Image Repair)
         *
         * issue CDN use Referer .
         * Android WebView request when not Referer CDN 403 .
         * in in WebView in .
         *
         * fix .
         * 1. <img>.
         * 2. fetch + no-referrer request.
         * 3. as Object URL and src.
         * 4. use MutationObserver observe DOM fix.
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
                    
                    // 1: referrerPolicy and.
                    img.referrerPolicy = 'no-referrer';
                    img.crossOrigin = 'anonymous';
                    
                    // 2: fetch + no-referrer Get and as blob URL.
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
                            // : image loading after Release Object URL.
                            img.addEventListener('load', function() {
                                setTimeout(function() { URL.revokeObjectURL(blobUrl); }, 5000);
                            }, { once: true });
                        }
                    }).catch(function() {
                        // fetch : referrerPolicy after.
                        img.src = '';
                        img.referrerPolicy = 'no-referrer';
                        setTimeout(function() { img.src = originalSrc; }, 50);
                    });
                }
                
                function scanBrokenImages() {
                    var images = document.querySelectorAll('img');
                    for (var i = 0; i < images.length; i++) {
                        var img = images[i];
                        // is: src naturalWidth=0.
                        if (img.src && img.complete && img.naturalWidth === 0 && img.naturalHeight === 0) {
                            repairImage(img);
                        }
                    }
                }
                
                // as error observe.
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
                
                // observe DOM fix.
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
                
                // .
                attachErrorListeners();
                setTimeout(scanBrokenImages, 1500);
                setTimeout(scanBrokenImages, 5000);
                
                // observer issue.
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
         * Android WebView not navigator.clipboard APIreadText/writeText.
         * NotAllowedError .
         *
         * polyfill NativeBridge to Android ClipboardManager.
         * can use navigator.clipboard.readText() writeText().
         *
         * when navigator.permissions.query() clipboard-read/clipboard-write.
         * 'granted' and Chrome as .
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
    
    // Browser Shields privacy protection manager
    private lateinit var shields: BrowserShields
    
    // Error page manager custom error page generation
    private var errorPageManager: ErrorPageManager? = null
    private var lastFailedUrl: String?
        get() = sessionState.lastFailedUrl
        set(value) {
            sessionState.lastFailedUrl = value
        }
    
    // file:// retry counter auto-retry when file not yet extracted (race condition)
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
    
    // Cookie flush debounce when onPageFinished.
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
    private val compatibilityScriptCoordinator = WebViewCompatibilityScriptCoordinator(
        adBlocker = adBlocker,
        urlPolicy = urlPolicy,
        getShields = ::getShields
    )
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
    private val navigationHandler = WebViewNavigationHandler(
        urlPolicy = urlPolicy,
        strictHostRuntimePolicy = strictHostRuntimePolicy,
        specialUrlHandler = specialUrlHandler,
        getCurrentMainFrameUrl = { currentMainFrameUrl },
        getCurrentConfig = { currentConfig },
        getManagedWebViews = { managedWebViews.keys },
        getShields = ::getShields
    )
    private val pageLifecycleHandler by lazy {
        WebViewPageLifecycleHandler(
            state = sessionState,
            adBlocker = adBlocker,
            strictHostRuntimePolicy = strictHostRuntimePolicy,
            scriptInjectionCoordinator = scriptInjectionCoordinator,
            webViewLifecycleCleaner = webViewLifecycleCleaner,
            specialUrlHandler = specialUrlHandler,
            getErrorPageManager = { errorPageManager },
            getShields = ::getShields,
            getCachedBrowserDisguiseConfig = { cachedBrowserDisguiseConfig },
            getCachedBrowserDisguiseJs = { cachedBrowserDisguiseJs },
            currentDeviceDisguiseConfig = { currentDeviceDisguiseConfig },
            cookieFlushRunnable = cookieFlushRunnable,
            urlPolicy = urlPolicy,
            fileMaxRetries = FILE_MAX_RETRIES,
            fileRetryDelayMs = FILE_RETRY_DELAY_MS
        )
    }
    private val setupCoordinator by lazy {
        WebViewSetupCoordinator(
            context = context,
            state = sessionState,
            settingsConfigurator = settingsConfigurator,
            userAgentResolver = userAgentResolver,
            extensionRuntimeCoordinator = extensionRuntimeCoordinator,
            getActiveModulesForCurrentApp = ::getActiveModulesForCurrentApp,
            createWebViewClient = ::createWebViewClient,
            createWebChromeClient = ::createWebChromeClient,
            openInCustomTab = specialUrlHandler::openInCustomTab
        )
    }
    
    /**
     * from WebView UA in Extract Chrome UA and .
     * when Chrome/120 as .
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
        ensureDynamicUserAgents()
        shields = BrowserShields.getInstance(context)
        setupCoordinator.configureWebView(
            webView = webView,
            config = config,
            callbacks = callbacks,
            extensionModuleIds = extensionModuleIds,
            embeddedExtensionModules = embeddedExtensionModules,
            extensionFabIcon = extensionFabIcon,
            allowGlobalModuleFallback = allowGlobalModuleFallback,
            browserDisguiseConfig = browserDisguiseConfig,
            deviceDisguiseConfig = deviceDisguiseConfig,
            shields = shields,
            currentLanguage = com.webtoapp.core.i18n.AppStringsProvider.currentLanguage.name,
            resolveUserAgent = ::resolveUserAgent,
            onErrorPageManagerChange = { errorPageManager = it }
        )
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

        return ManagedWebViewClient(
            shouldInterceptRequestHandler = { view, request, delegate ->
                handleShouldInterceptRequest(request, delegate.shouldInterceptRequest(view, request), config, diag)
            },
            shouldOverrideUrlLoadingHandler = { view, request ->
                navigationHandler.handleShouldOverrideUrlLoading(view, request, config, callbacks)
            },
            onPageStartedHandler = { view, url, favicon ->
                pageLifecycleHandler.onPageStarted(view, url, config, callbacks, diag, favicon)
            },
            onPageCommitVisibleHandler = { _, url ->
                pageLifecycleHandler.onPageCommitVisible(url, callbacks, diag)
            },
            doUpdateVisitedHistoryHandler = { view, url, _ ->
                callbacks.onUrlChanged(view, url)
            },
            onPageFinishedHandler = { view, url ->
                pageLifecycleHandler.onPageFinished(view, url, config, callbacks, diag)
            },
            onReceivedErrorHandler = { view, request, error ->
                pageLifecycleHandler.onReceivedError(view, request, error, callbacks, diag)
            },
            onReceivedHttpErrorHandler = { view, request, errorResponse ->
                pageLifecycleHandler.onReceivedHttpError(view, request, errorResponse, callbacks)
            },
            onReceivedSslErrorHandler = { view, handler, error ->
                pageLifecycleHandler.onReceivedSslError(view, handler, error, config, callbacks)
            },
            onRenderProcessGoneHandler = { view, detail ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pageLifecycleHandler.onRenderProcessGone(view, detail, callbacks)
                } else {
                    false
                }
            }
        )
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

    private fun createWebChromeClient(config: WebViewConfig, callbacks: WebViewCallbacks): WebChromeClient {
        return ManagedWebChromeClient(
            config = config,
            callbacks = callbacks,
            specialUrlHandler = specialUrlHandler
        )
    }

    /**
     * Apply strict host policy before first load so initial request already uses strict settings.
     */
    fun applyPreloadPolicyForUrl(webView: WebView, pageUrl: String?) {
        navigationHandler.applyPreloadPolicyForUrl(
            webView = webView,
            pageUrl = pageUrl,
            currentConfig = currentConfig,
            currentDeviceDisguiseConfig = currentDeviceDisguiseConfig
        )
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
    
    // Browser Disguise pre-generated anti-fingerprint JS (cached per configureWebView call)
    private var cachedBrowserDisguiseJs: String?
        get() = sessionState.cachedBrowserDisguiseJs
        set(value) {
            sessionState.cachedBrowserDisguiseJs = value
        }
    // Browser Disguise config reference (for BrowserDisguiseEngine integration)
    private var cachedBrowserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig?
        get() = sessionState.cachedBrowserDisguiseConfig
        set(value) {
            sessionState.cachedBrowserDisguiseConfig = value
        }
    // Device Disguise config reference (for device type/brand/model UA spoofing)
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
        compatibilityScriptCoordinator.injectCompatibilityScripts(
            webView = webView,
            pageUrl = pageUrl,
            config = config,
            conservativeMode = conservativeMode
        )
    }

}
