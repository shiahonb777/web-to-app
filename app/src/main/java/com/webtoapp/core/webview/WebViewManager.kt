package com.webtoapp.core.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import com.webtoapp.core.logging.AppLogger
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.webkit.ScriptHandler
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
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
import com.webtoapp.core.engine.GeckoViewEngine
import com.webtoapp.core.engine.ProxyConfig
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.errorpage.ErrorPageManager
import com.webtoapp.core.errorpage.ErrorPageMode
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject




class WebViewManager(
    private val context: Context,
    private val adBlocker: AdBlocker
) {
    private val proxyScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var proxyApplyJob: Job? = null
    private var extensionPanelSyncJob: Job? = null
    private var extensionPanelDeferredInjectionJob: Job? = null
    private var extensionPanelInjected = false
    private val privateNetworkScriptHandlers = java.util.WeakHashMap<WebView, ScriptHandler>()
    private val downloadBridgeScriptHandlers = java.util.WeakHashMap<WebView, ScriptHandler>()

    companion object {

        private var DESKTOP_USER_AGENT: String? = null
        private const val DESKTOP_USER_AGENT_FALLBACK = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"


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


        private val TEXT_MIME_TYPES = setOf(
            "text/html", "text/css", "text/plain",
            "application/javascript", "application/json",
            "application/xml", "image/svg+xml"
        )


        private val DESKTOP_UA_MODES = setOf(
            UserAgentMode.CHROME_DESKTOP,
            UserAgentMode.SAFARI_DESKTOP,
            UserAgentMode.FIREFOX_DESKTOP,
            UserAgentMode.EDGE_DESKTOP
        )


        private val SKIP_HEADERS = setOf("host", "connection")




        private fun isLocalCleartextHost(host: String): Boolean {
            if (host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2") return true

            if (host.startsWith("10.") && host.count { it == '.' } == 3) return true

            if (host.startsWith("172.")) {
                val second = host.removePrefix("172.").substringBefore('.').toIntOrNull()
                if (second != null && second in 16..31) return true
            }

            if (host.startsWith("192.168.")) return true

            if (host == "::1" || host == "[::1]") return true

            if (host.endsWith(".local")) return true
            return false
        }




        private val CAPTCHA_HOST_SUFFIXES = setOf(


            "recaptcha.net",
            "www.recaptcha.net",

            "hcaptcha.com",
            "js.hcaptcha.com",
            "newassets.hcaptcha.com",
            "imgs.hcaptcha.com",

            "challenges.cloudflare.com",

            "arkoselabs.com",
            "client-api.arkoselabs.com",
            "cdn.arkoselabs.com",
            "funcaptcha.com"
        )






        private val OAUTH_HOST_SUFFIXES = setOf(

            "accounts.google.com",
            "accounts.youtube.com",
            "myaccount.google.com",

            "www.facebook.com",
            "m.facebook.com",

            "appleid.apple.com",

            "login.microsoftonline.com",
            "login.live.com",

            "github.com",

            "api.twitter.com",
            "api.x.com"
        )



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
            "cdn.jsdelivr.net",
            "unpkg.com",
            "cdnjs.cloudflare.com",
            "leafletjs.com",
            "leaflet-extras.github.io",
            "nominatim.openstreetmap.org",
            "overpass-api.de",
            "router.project-osrm.org",
            "routing.openstreetmap.de",
            "valhalla.openstreetmap.de"
        )



        private val STRICT_COMPAT_HOST_SUFFIXES = setOf(
            "douyin.com",
            "iesdouyin.com",
            "tiktok.com",
            "tiktokv.com",
            "byteoversea.com",
            "byteimg.com"
        )





        private var STRICT_COMPAT_MOBILE_USER_AGENT: String? = null
        private const val STRICT_COMPAT_MOBILE_UA_FALLBACK =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"


        private val WEBVIEW_WV_MARKER_REGEX = Regex(";\\s*wv\\s*\\)")


        @JvmStatic
        fun stripWebViewMarker(userAgent: String): String {
            if (userAgent.isEmpty()) return userAgent
            if (!userAgent.contains("wv")) return userAgent
            return WEBVIEW_WV_MARKER_REGEX.replace(userAgent, ")")
        }


        private val COMMON_SECOND_LEVEL_TLDS = setOf(
            "co.uk", "org.uk", "gov.uk", "ac.uk",
            "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn",
            "com.hk", "com.tw",
            "com.au", "net.au", "org.au",
            "co.jp", "co.kr", "co.in", "com.br", "com.mx"
        )


        private val BLOCKED_SPECIAL_SCHEMES = setOf("javascript", "data", "file", "content", "about")














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









        private const val SCROLL_SAVE_JS = """(function(){
            'use strict';
            if(window.__wtaScrollMemory&&window.__wtaScrollMemory.installed)return;

            var PREFIX='__wta_scroll_';
            var scrollTimer=null;
            var restoreTimers=[];
            var rawPushState=history.pushState?history.pushState.bind(history):null;
            var rawReplaceState=history.replaceState?history.replaceState.bind(history):null;

            // ★ 用户交互守卫：防止在用户主动滚动/触摸时执行恢复，避免"秒拉回"
            var userInteracting=false;
            var interactionTimer=null;
            var INTERACTION_COOLDOWN=1500; // 用户停止触摸后 1.5s 才允许恢复

            function markUserInteraction(){
                userInteracting=true;
                clearRestoreTimers(); // 立即取消所有待执行的恢复定时器
                clearTimeout(interactionTimer);
                interactionTimer=setTimeout(function(){
                    userInteracting=false;
                },INTERACTION_COOLDOWN);
            }

            function getKey(url){
                return PREFIX+(url||location.href);
            }

            function getScrollY(){
                return window.scrollY||window.pageYOffset||document.documentElement.scrollTop||document.body.scrollTop||0;
            }

            function getDocumentHeight(){
                return Math.max(
                    document.body?document.body.scrollHeight:0,
                    document.documentElement?document.documentElement.scrollHeight:0,
                    document.body?document.body.offsetHeight:0,
                    document.documentElement?document.documentElement.offsetHeight:0
                );
            }

            function savePos(urlOverride){
                try{
                    var targetUrl=urlOverride||location.href;
                    var y=getScrollY();
                    sessionStorage.setItem(getKey(targetUrl),String(y));
                    if(rawReplaceState){
                        try{
                            var st=(history.state&&typeof history.state==='object')?history.state:{};
                            st.__wtaScrollY=y;
                            st.__wtaScrollUrl=targetUrl;
                            rawReplaceState(st,document.title,location.href);
                        }catch(e2){}
                    }
                }catch(e){}
            }

            function readSavedY(){
                var saved=null;
                try{
                    saved=sessionStorage.getItem(getKey());
                }catch(e){}
                if((saved===null||saved==='')&&history.state&&history.state.__wtaScrollUrl===location.href){
                    try{
                        if(history.state.__wtaScrollY>=0)saved=String(history.state.__wtaScrollY);
                    }catch(e2){}
                }
                var y=parseInt(saved||'',10);
                return isNaN(y)?-1:y;
            }

            function clearRestoreTimers(){
                for(var i=0;i<restoreTimers.length;i++){
                    clearTimeout(restoreTimers[i]);
                }
                restoreTimers=[];
            }

            function restorePos(maxAttempts){
                // ★ 用户正在交互时，跳过恢复，避免"秒拉回"
                if(userInteracting)return false;
                var y=readSavedY();
                if(y<0)return false;
                // ★ 如果当前滚动位置已经 >= 保存的位置，说明用户已经滚到了更远处，不需要恢复
                var currentY=getScrollY();
                if(currentY>0&&currentY>=y-5)return false;
                if('scrollRestoration' in history){
                    try{history.scrollRestoration='manual';}catch(e3){}
                }
                var attempts=maxAttempts||60;
                function apply(remaining){
                    // ★ 每次递归前检查用户交互状态
                    if(userInteracting)return;
                    if(remaining<=0)return;
                    var docH=getDocumentHeight();
                    var maxY=Math.max(0,docH-window.innerHeight);
                    var targetY=Math.min(y,maxY);
                    window.scrollTo(0,targetY);
                    var nowY=getScrollY();
                    if(Math.abs(nowY-targetY)<=2&&(targetY===0||docH>=y+window.innerHeight*0.2)){
                        setTimeout(function(){if(!userInteracting)window.scrollTo(0,targetY);},60);
                        setTimeout(function(){if(!userInteracting)window.scrollTo(0,targetY);},180);
                        setTimeout(function(){if(!userInteracting)window.scrollTo(0,targetY);},360);
                        return;
                    }
                    setTimeout(function(){apply(remaining-1);},120);
                }
                apply(attempts);
                return true;
            }

            function scheduleRestore(){
                clearRestoreTimers();
                // ★ 用户正在交互时，延迟调度恢复而非立即执行
                if(userInteracting){
                    restoreTimers.push(setTimeout(function(){
                        if(!userInteracting)restorePos(60);
                    },INTERACTION_COOLDOWN+200));
                    return;
                }
                var delays=[0,60,180,420,900,1600,2600];
                for(var i=0;i<delays.length;i++){
                    (function(delayMs){
                        restoreTimers.push(setTimeout(function(){restorePos(60);},delayMs));
                    })(delays[i]);
                }
            }

            function saveSoon(){
                clearTimeout(scrollTimer);
                scrollTimer=setTimeout(function(){savePos();},180);
            }

            window.__wtaScrollMemory={
                installed:true,
                save:savePos,
                restore:restorePos,
                scheduleRestore:scheduleRestore
            };

            window.addEventListener('pagehide',function(){savePos();});
            window.addEventListener('beforeunload',function(){savePos();});
            window.addEventListener('pageshow',function(){scheduleRestore();});
            window.addEventListener('popstate',function(){scheduleRestore();});
            window.addEventListener('hashchange',function(){scheduleRestore();});
            window.addEventListener('load',function(){scheduleRestore();});
            document.addEventListener('visibilitychange',function(){
                if(document.visibilityState==='hidden'){
                    savePos();
                }else if(document.visibilityState==='visible'){
                    scheduleRestore();
                }
            });
            document.addEventListener('click',function(){savePos();},true);
            // ★ 用户滚动时标记交互 + 保存位置，取消恢复定时器
            window.addEventListener('scroll',function(){
                markUserInteraction();
                saveSoon();
            },{passive:true});
            // ★ 触摸开始时立即标记交互，防止恢复定时器抢占
            window.addEventListener('touchstart',function(){
                markUserInteraction();
            },{passive:true});
            // ★ 触摸结束时延长冷却期，防止恢复在手指刚抬起时抢跑
            window.addEventListener('touchend',function(){
                markUserInteraction();
            },{passive:true});

            if(rawPushState){
                history.pushState=function(){
                    savePos();
                    return rawPushState.apply(history,arguments);
                };
            }
            if(rawReplaceState){
                history.replaceState=function(){
                    savePos();
                    return rawReplaceState.apply(history,arguments);
                };
            }
        })();"""








        private const val SCROLL_RESTORE_JS = """(function(){
            'use strict';
            try{
                if(window.__wtaScrollMemory&&typeof window.__wtaScrollMemory.scheduleRestore==='function'){
                    window.__wtaScrollMemory.scheduleRestore();
                }else if(window.__wtaScrollMemory&&typeof window.__wtaScrollMemory.restore==='function'){
                    window.__wtaScrollMemory.restore(60);
                }
            }catch(e){}
        })();"""














        private const val IMAGE_REPAIR_JS = """
            (function() {
                'use strict';
                if (window.__wtaImageRepairActive) return;
                window.__wtaImageRepairActive = true;

                var repaired = new WeakSet();

                function repairImage(img) {
                    if (!img || !img.src || repaired.has(img)) return;
                    if (img.src.startsWith('data:') || img.src.startsWith('blob:')) return;
                    // 只修复真正加载失败的图片（error 事件触发后才进入此函数）
                    repaired.add(img);
                    var originalSrc = img.src;

                    // 尝试用 no-referrer 重新加载
                    img.referrerPolicy = 'no-referrer';
                    img.src = '';
                    setTimeout(function() { img.src = originalSrc; }, 50);
                }

                // 只监听 error 事件，不主动扫描 naturalWidth=0 的图片
                // （懒加载图片在加载前 naturalWidth=0 是正常的）
                function attachErrorListener(img) {
                    if (img.__wtaErrorListening) return;
                    img.__wtaErrorListening = true;
                    img.addEventListener('error', function() {
                        repairImage(img);
                    }, { once: true });
                }

                // 为现有图片添加 error 监听
                var images = document.querySelectorAll('img');
                for (var i = 0; i < images.length; i++) {
                    attachErrorListener(images[i]);
                }

                // 监听 DOM 变化，为动态加载的图片添加 error 监听
                var observer = new MutationObserver(function(mutations) {
                    for (var m = 0; m < mutations.length; m++) {
                        var nodes = mutations[m].addedNodes;
                        for (var n = 0; n < nodes.length; n++) {
                            var node = nodes[n];
                            if (node.nodeName === 'IMG') {
                                attachErrorListener(node);
                            } else if (node.querySelectorAll) {
                                var imgs = node.querySelectorAll('img');
                                for (var j = 0; j < imgs.length; j++) {
                                    attachErrorListener(imgs[j]);
                                }
                            }
                        }
                    }
                });
                var observerTarget = document.documentElement || document.body;
                if (observerTarget instanceof Node) {
                    observer.observe(observerTarget, { childList: true, subtree: true });
                }

                // 30 秒后停止 observer 防止性能问题
                setTimeout(function() { observer.disconnect(); }, 30000);
            })();
        """


        private val PAYMENT_SCHEMES = setOf(
            "alipay", "alipays",
            "weixin", "wechat",
            "mqq", "mqqapi", "mqqwpa",
            "taobao",
            "tmall",
            "jd", "openapp.jdmobile",
            "pinduoduo",
            "meituan", "imeituan",
            "eleme",
            "dianping",
            "sinaweibo", "weibo",
            "bilibili",
            "douyin",
            "snssdk",
            "bytedance"
        )













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

        private const val ORIENTATION_POLYFILL_JS = """
            (function() {
                'use strict';

                if (window.__webtoapp_orientation_polyfill__) return;
                window.__webtoapp_orientation_polyfill__ = true;

                var bridge = window.NativeBridge;
                var screenObj = window.screen;
                if (!bridge || !screenObj) return;

                var listeners = [];
                var onchangeHandler = null;
                var lastRequestedType = null;
                var baseOrientation = screenObj.orientation && typeof screenObj.orientation === 'object'
                    ? screenObj.orientation
                    : {};

                function normalizeType(value) {
                    var normalized = String(value || '').toLowerCase();
                    switch (normalized) {
                        case 'portrait':
                        case 'portrait-primary':
                            return 'portrait-primary';
                        case 'portrait-secondary':
                            return 'portrait-secondary';
                        case 'landscape':
                        case 'landscape-primary':
                            return 'landscape-primary';
                        case 'landscape-secondary':
                            return 'landscape-secondary';
                        default:
                            return '';
                    }
                }

                function readNativeType() {
                    try {
                        var orientation = String(bridge.getOrientation() || '').toLowerCase();
                        if (orientation === 'landscape') return 'landscape-primary';
                        if (orientation === 'portrait') return 'portrait-primary';
                    } catch (e) {}
                    return lastRequestedType || 'portrait-primary';
                }

                function currentType() {
                    return normalizeType(lastRequestedType) || readNativeType();
                }

                function currentAngle() {
                    switch (currentType()) {
                        case 'portrait-secondary':
                            return 180;
                        case 'landscape-primary':
                            return 90;
                        case 'landscape-secondary':
                            return 270;
                        default:
                            return 0;
                    }
                }

                function createChangeEvent() {
                    try {
                        return new Event('change');
                    } catch (e) {
                        var legacyEvent = document.createEvent('Event');
                        legacyEvent.initEvent('change', false, false);
                        return legacyEvent;
                    }
                }

                function emitChange() {
                    var event = createChangeEvent();
                    if (typeof baseOrientation.onchange === 'function') {
                        try {
                            baseOrientation.onchange.call(baseOrientation, event);
                        } catch (e) {}
                    }
                    listeners.slice().forEach(function(listener) {
                        try {
                            listener.call(baseOrientation, event);
                        } catch (e) {}
                    });
                }

                function scheduleChange() {
                    setTimeout(emitChange, 0);
                }

                function applyLock(lockType) {
                    var requestedType = String(lockType || '').toLowerCase();
                    switch (requestedType) {
                        case 'portrait':
                        case 'portrait-primary':
                            bridge.setOrientation('portrait');
                            lastRequestedType = 'portrait-primary';
                            break;
                        case 'portrait-secondary':
                            bridge.setOrientation('reverse_portrait');
                            lastRequestedType = 'portrait-secondary';
                            break;
                        case 'landscape':
                        case 'landscape-primary':
                            bridge.setOrientation('landscape');
                            lastRequestedType = 'landscape-primary';
                            break;
                        case 'landscape-secondary':
                            bridge.setOrientation('reverse_landscape');
                            lastRequestedType = 'landscape-secondary';
                            break;
                        case 'any':
                        case 'auto':
                        case 'default':
                            bridge.unlockOrientation();
                            lastRequestedType = null;
                            break;
                        case 'natural':
                            bridge.lockOrientation();
                            lastRequestedType = readNativeType();
                            break;
                        default:
                            bridge.lockOrientation();
                            lastRequestedType = readNativeType();
                            break;
                    }
                    scheduleChange();
                }

                function safeDefineProperty(target, key, descriptor) {
                    try {
                        Object.defineProperty(target, key, descriptor);
                        return true;
                    } catch (e) {
                        return false;
                    }
                }

                function assignProperty(target, key, value) {
                    if (safeDefineProperty(target, key, {
                        configurable: true,
                        writable: true,
                        value: value
                    })) {
                        return true;
                    }
                    try {
                        target[key] = value;
                        return target[key] === value;
                    } catch (e) {
                        return false;
                    }
                }

                var lockFn = function(lockType) {
                    try {
                        applyLock(lockType);
                        return Promise.resolve();
                    } catch (error) {
                        return Promise.reject(error);
                    }
                };

                var unlockFn = function() {
                    bridge.unlockOrientation();
                    lastRequestedType = null;
                    scheduleChange();
                };

                var addEventListenerFn = function(type, listener) {
                    if (type !== 'change' || typeof listener !== 'function') return;
                    if (listeners.indexOf(listener) === -1) {
                        listeners.push(listener);
                    }
                };

                var removeEventListenerFn = function(type, listener) {
                    if (type !== 'change' || typeof listener !== 'function') return;
                    listeners = listeners.filter(function(item) {
                        return item !== listener;
                    });
                };

                assignProperty(baseOrientation, 'lock', lockFn);
                assignProperty(baseOrientation, 'unlock', unlockFn);
                assignProperty(baseOrientation, 'addEventListener', addEventListenerFn);
                assignProperty(baseOrientation, 'removeEventListener', removeEventListenerFn);

                if (typeof baseOrientation.dispatchEvent !== 'function') {
                    assignProperty(baseOrientation, 'dispatchEvent', function() {
                        return true;
                    });
                }

                safeDefineProperty(baseOrientation, 'type', {
                    configurable: true,
                    get: currentType
                });

                safeDefineProperty(baseOrientation, 'angle', {
                    configurable: true,
                    get: currentAngle
                });

                safeDefineProperty(baseOrientation, 'onchange', {
                    configurable: true,
                    get: function() {
                        return onchangeHandler;
                    },
                    set: function(listener) {
                        onchangeHandler = typeof listener === 'function' ? listener : null;
                    }
                });

                if (!screenObj.orientation) {
                    if (!safeDefineProperty(screenObj, 'orientation', {
                        configurable: true,
                        value: baseOrientation
                    })) {
                        try {
                            screenObj.orientation = baseOrientation;
                        } catch (e) {}
                    }
                }

                if (typeof screenObj.lockOrientation !== 'function') {
                    screenObj.lockOrientation = function(lockType) {
                        try {
                            applyLock(lockType);
                            return true;
                        } catch (e) {
                            return false;
                        }
                    };
                }

                if (typeof screenObj.unlockOrientation !== 'function') {
                    screenObj.unlockOrientation = function() {
                        try {
                            bridge.unlockOrientation();
                            lastRequestedType = null;
                            scheduleChange();
                            return true;
                        } catch (e) {
                            return false;
                        }
                    };
                }

                window.addEventListener('orientationchange', function() {
                    lastRequestedType = null;
                    emitChange();
                }, { passive: true });

                console.log('[WebToApp] Screen orientation polyfill loaded');
            })();
        """
    }


    private var appExtensionModuleIds: List<String> = emptyList()


    private var embeddedModules: List<com.webtoapp.core.shell.EmbeddedShellModule> = emptyList()


    private var allowGlobalModuleFallback: Boolean = false


    private var extensionFabIcon: String = ""


    private var gmBridge: com.webtoapp.core.extension.GreasemonkeyBridge? = null


    private var extensionRuntimes: MutableMap<String, com.webtoapp.core.extension.ChromeExtensionRuntime> = mutableMapOf()
    private val extensionPopupDialogs = mutableMapOf<String, Dialog>()


    private val extensionFileManager by lazy {
        com.webtoapp.core.extension.ExtensionFileManager(context)
    }


    private val managedWebViews = java.util.WeakHashMap<WebView, Boolean>()

    private val userscriptInjectionState =
        java.util.WeakHashMap<WebView, MutableSet<String>>()

    private val pagePhaseExecutionState =
        java.util.WeakHashMap<WebView, MutableSet<String>>()



    @Volatile
    var isNavigatingBack: Boolean = false
        private set


    private var previousHistoryIndex: Int = -1








    fun markNavigatingBack() {
        isNavigatingBack = true
    }


    private lateinit var shields: BrowserShields


    private var errorPageManager: ErrorPageManager? = null
    private var lastFailedUrl: String? = null


    private var fileRetryCount = 0
    private var fileRetryUrl: String? = null
    private val FILE_MAX_RETRIES = 3
    private val FILE_RETRY_DELAY_MS = 500L
    private var loopbackMainFrameRetryCount = 0
    private var loopbackMainFrameRetryUrl: String? = null
    private var loopbackMainFrameRetryPending = false
    private var loopbackMainFrameRetryRunnable: Runnable? = null
    private var loopbackMainFrameRetryView: WebView? = null
    private val LOOPBACK_MAIN_FRAME_MAX_RETRIES = 3
    private val LOOPBACK_MAIN_FRAME_RETRY_DELAY_MS = 150L

    /**
     * 集中清理 loopback 主帧重试状态：
     * - 取消任何已用 [WebView.postDelayed] 排定但尚未执行的重试任务，避免在页面已恢复后还跑一次
     * - 重置计数器、url 标记和 pending 标志
     *
     * 调用场景：页面真正加载成功（onPageFinished 且无 onReceivedError）、
     *          重试达到上限放弃、WebView 切换到新 URL 等。
     */
    private fun cancelLoopbackMainFrameRetry() {
        val pending = loopbackMainFrameRetryRunnable
        val view = loopbackMainFrameRetryView
        if (pending != null && view != null) {
            view.removeCallbacks(pending)
        }
        loopbackMainFrameRetryRunnable = null
        loopbackMainFrameRetryView = null
        loopbackMainFrameRetryCount = 0
        loopbackMainFrameRetryUrl = null
        loopbackMainFrameRetryPending = false
    }


    @Volatile
    private var currentMainFrameUrl: String? = null

    private val externalOAuthLaunchTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()


    private val cookieFlushRunnable = Runnable {
        try { CookieManager.getInstance().flush() } catch (_: Exception) {}
    }





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

        this.currentConfig = config

        this.cachedBrowserDisguiseConfig = browserDisguiseConfig
        this.cachedBrowserDisguiseJs = if (browserDisguiseConfig?.enabled == true) {
            com.webtoapp.core.disguise.BrowserDisguiseJsGenerator.generate(browserDisguiseConfig).also { js ->
                val coverage = com.webtoapp.core.disguise.BrowserDisguiseConfig.calculateCoverage(browserDisguiseConfig)
                AppLogger.d("WebViewManager", "Browser Disguise JS cached: ${js.length} chars, coverage=${"%,.0f".format(coverage * 100)}%")
            }
        } else null

        this.appExtensionModuleIds = extensionModuleIds

        this.embeddedModules = embeddedExtensionModules
        this.allowGlobalModuleFallback = allowGlobalModuleFallback

        this.extensionFabIcon = extensionFabIcon

        this.currentDeviceDisguiseConfig = deviceDisguiseConfig


        shields = BrowserShields.getInstance(context)


        if (config.errorPageConfig.mode != ErrorPageMode.DEFAULT) {

            val errorConfig = config.errorPageConfig.copy(
                language = com.webtoapp.core.i18n.Strings.currentLanguage.value.name
            )
            errorPageManager = ErrorPageManager(errorConfig)
        } else {
            // Even in DEFAULT mode we keep an ErrorPageManager around so that high-value
            // socket-level errors (EADDRNOTAVAIL / ECONNREFUSED / ETIMEDOUT …) can be
            // translated into actionable guidance instead of raw errno strings. The manager
            // returns null for any error it can't explain, letting the system page render.
            val errorConfig = config.errorPageConfig.copy(
                language = com.webtoapp.core.i18n.Strings.currentLanguage.value.name
            )
            errorPageManager = ErrorPageManager(errorConfig)
        }


        AppLogger.d("WebViewManager", "configureWebView: extensionModuleIds=${extensionModuleIds.size}, embeddedModules=${embeddedExtensionModules.size}")
        embeddedExtensionModules.forEach { module ->
            AppLogger.d("WebViewManager", "  Embedded module: id=${module.id}, name=${module.name}, enabled=${module.enabled}, runAt=${module.runAt}")
        }

        val dnsManager = com.webtoapp.core.dns.DnsManager(context)
        val hostsMappingEnabled = config.hostsMappingEnabled && config.hostsMappings.isNotEmpty()
        val hostsMappingCanApply = hostsMappingEnabled && config.proxyMode == "NONE"

        if (config.dnsMode != "SYSTEM") {
            AppLogger.d("WebViewManager", "Applying DoH DNS: mode=${config.dnsMode}, provider=${config.dnsConfig.provider}")
            dnsManager.applyDnsConfig(config.dnsConfig)
            com.webtoapp.core.engine.GeckoViewEngine.applyDnsConfig(config.dnsConfig)
        } else {
            dnsManager.clearDnsConfig()
            com.webtoapp.core.engine.GeckoViewEngine.applyDnsConfig(config.dnsConfig)
        }

        if (config.proxyMode != "NONE") {
            LocalHttpHostMappingBridge.stop()
            AppLogger.d("WebViewManager", "Applying proxy: mode=${config.proxyMode}")

            val proxyManager = PacProxyManager(context)
            val proxyMode = config.proxyMode
            val proxyHost = config.proxyHost
            val proxyPort = config.proxyPort
            val proxyType = config.proxyType
            val pacUrl = config.pacUrl
            val bypassRules = config.proxyBypassRules
            proxyApplyJob?.cancel()
            proxyApplyJob = proxyScope.launch {
                try {
                    proxyManager.applyProxy(
                        proxyMode = proxyMode,
                        staticProxyHost = proxyHost,
                        staticProxyPort = proxyPort,
                        staticProxyType = proxyType,
                        pacUrl = pacUrl,
                        bypassRules = bypassRules,
                        username = config.proxyUsername,
                        password = config.proxyPassword
                    )
                } catch (e: Exception) {
                    AppLogger.e("WebViewManager", "Failed to apply proxy config", e)
                }
            }

            GeckoViewEngine.applyProxyConfig(
                ProxyConfig(
                    mode = proxyMode,
                    host = proxyHost,
                    port = proxyPort,
                    type = proxyType,
                    pacUrl = pacUrl,
                    username = config.proxyUsername,
                    password = config.proxyPassword
                )
            )
        } else {
            proxyApplyJob?.cancel()
            PacProxyManager(context).clearProxy()

            GeckoViewEngine.applyProxyConfig(
                ProxyConfig(mode = "NONE")
            )
            if (hostsMappingCanApply) {
                val bridgePort = LocalHttpHostMappingBridge.start(
                    config = LocalHttpHostMappingBridge.Config(
                        mappings = config.hostsMappings,
                        dnsMode = config.dnsMode,
                        dnsConfig = config.dnsConfig
                    ),
                    dnsManager = dnsManager
                )
                if (bridgePort > 0) {
                    AppLogger.d("WebViewManager", "Applying hosts mapping proxy on 127.0.0.1:$bridgePort")
                    proxyApplyJob = proxyScope.launch {
                        try {
                            PacProxyManager(context).applyProxy(
                                proxyMode = "STATIC",
                                staticProxyHost = "127.0.0.1",
                                staticProxyPort = bridgePort,
                                staticProxyType = "HTTP",
                                bypassRules = emptyList()
                            )
                        } catch (e: Exception) {
                            AppLogger.e("WebViewManager", "Failed to apply hosts mapping proxy", e)
                        }
                    }
                } else {
                    AppLogger.w("WebViewManager", "Hosts mapping bridge failed to start")
                }
            } else {
                LocalHttpHostMappingBridge.stop()
                if (hostsMappingEnabled) {
                    AppLogger.w("WebViewManager", "Hosts mapping skipped because proxyMode=${config.proxyMode}")
                }
            }
        }


        managedWebViews[webView] = true



        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)















        cookieManager.setAcceptThirdPartyCookies(webView, config.acceptThirdPartyCookies)

        cookieManager.flush()
        AppLogger.d("WebViewManager", "Cookie persistence enabled (thirdParty=${config.acceptThirdPartyCookies}, disableShields=${config.disableShields})")

        val isDesktopModeRequested = config.userAgentMode in DESKTOP_UA_MODES || config.desktopMode || (currentDeviceDisguiseConfig?.requiresDesktopViewport() == true)


        val preferLandscapeEmbeddedViewport = config.landscapeMode && !isDesktopModeRequested

        webView.apply {
            settings.apply {

                javaScriptEnabled = config.javaScriptEnabled
                javaScriptCanOpenWindowsAutomatically = config.javaScriptCanOpenWindows


                domStorageEnabled = config.domStorageEnabled
                databaseEnabled = config.databaseEnabled


                allowFileAccess = config.allowFileAccess
                allowContentAccess = config.allowContentAccess


                cacheMode = if (config.cacheEnabled) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_NO_CACHE
                }


                setSupportZoom(config.zoomEnabled)
                builtInZoomControls = config.zoomEnabled
                displayZoomControls = false


                useWideViewPort = true
                loadWithOverviewMode = !preferLandscapeEmbeddedViewport



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



                val effectiveUserAgent = resolveUserAgent(config)
                if (effectiveUserAgent != null) {
                    userAgentString = stripWebViewMarker(effectiveUserAgent)
                    AppLogger.d("WebViewManager", "User-Agent set: ${userAgentString.take(80)}...")
                } else {


                    val sanitized = stripWebViewMarker(userAgentString)
                    if (sanitized != userAgentString) {
                        userAgentString = sanitized
                        AppLogger.d("WebViewManager", "User-Agent sanitized (wv marker stripped): ${sanitized.take(80)}...")
                    }
                }





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


                if (isDesktopModeRequested) {
                    useWideViewPort = true
                    loadWithOverviewMode = true

                    textZoom = 100
                } else if (preferLandscapeEmbeddedViewport) {
                    AppLogger.d(
                        "WebViewManager",
                        "Landscape viewport policy applied: disable overview shrink-fit (loadWithOverviewMode=false)"
                    )
                }









                mixedContentMode = if (config.allowMixedContent) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW


                mediaPlaybackRequiresUserGesture = !config.mediaAutoplayEnabled



                @Suppress("DEPRECATION")
                setGeolocationEnabled(config.geolocationEnabled)

                @Suppress("DEPRECATION")
                setGeolocationDatabasePath(context.filesDir.absolutePath)




                allowFileAccessFromFileURLs = config.allowFileAccessFromFileURLs
                allowUniversalAccessFromFileURLs = config.allowUniversalAccessFromFileURLs

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = config.safeBrowsingEnabled
                }
            }


            isScrollbarFadingEnabled = true
            scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY


            com.webtoapp.core.perf.NativePerfEngine.optimizeWebViewSettings(this)




            if (config.initialScale > 0) {
                setInitialScale(config.initialScale)
                AppLogger.d("WebViewManager", "Set initial scale: ${config.initialScale}%")
            } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {

                setInitialScale(1)
                AppLogger.d("WebViewManager", "FIT_SCREEN: forced initial scale to 1 (override DPI scaling)")
            } else if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {

                setInitialScale(1)
                AppLogger.d("WebViewManager", "ViewportMode.CUSTOM: forced initial scale to 1 (custom width=${config.customViewportWidth})")
            }


            settings.setSupportMultipleWindows(config.newWindowBehavior != NewWindowBehavior.SAME_WINDOW)


            webViewClient = createWebViewClient(config, callbacks)


            webChromeClient = createWebChromeClient(config, callbacks)

            if (config.enablePrivateNetworkBridge) {
                installPrivateNetworkApiBridge(this, config)
            }


            if (config.downloadEnabled) {
                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    callbacks.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
                }
                // 在页面任何 JS 之前注入 DownloadBridge，确保 URL.createObjectURL 能被第一时间 hook
                // 这是唯一可靠的时机——页面自己的 inline script 可能在 onPageStarted 之前就已经执行
                if (config.enableBlobDownloadInterception) {
                    installDownloadBridgeDocumentStart(this)
                }
            }


            if (config.enableShareBridge) {
                addJavascriptInterface(ShareBridge(context), "NativeShareBridge")
            }




            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onOAuthBlocked(url: String) {
                    AppLogger.w("WebViewManager", "OAuth block detected via JS bridge — opening outside WebView: $url")
                    if (OAuthCompatEngine.shouldRedirectToCustomTab(url)) {
                        this@WebViewManager.openInCustomTab(url)
                    }
                }
            }, "NativeOAuthBridge")


            gmBridge?.destroy()
            val bridge = com.webtoapp.core.extension.GreasemonkeyBridge(context) { webView }
            gmBridge = bridge
            addJavascriptInterface(bridge, com.webtoapp.core.extension.GreasemonkeyBridge.JS_INTERFACE_NAME)


            initChromeExtensionRuntimes(webView)


            if (config.enableKernelDisguise) {
                com.webtoapp.core.kernel.BrowserKernel.configureWebView(webView)
            }



            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        extensionPanelInjected = false
        startExtensionPanelSync(webView)
    }






    private fun resolveUserAgent(config: WebViewConfig): String? {
        AppLogger.d("WebViewManager", "resolveUserAgent: userAgentMode=${config.userAgentMode}, customUserAgent=${config.customUserAgent?.take(30)}, desktopMode=${config.desktopMode}")


        val ddConfig = currentDeviceDisguiseConfig
        if (ddConfig != null && ddConfig.enabled) {
            val ua = ddConfig.generateUserAgent()
            if (ua.isNotBlank()) {
                AppLogger.d("WebViewManager", "resolveUserAgent: DeviceDisguise -> ${ua.take(80)}")
                return ua
            }
        }


        when (config.userAgentMode) {
            UserAgentMode.DEFAULT -> {

            }
            UserAgentMode.CUSTOM -> {

                val ua = config.customUserAgent?.takeIf { it.isNotBlank() }
                AppLogger.d("WebViewManager", "resolveUserAgent: CUSTOM mode -> ${ua?.take(60) ?: "null"}")
                return ua
            }
            else -> {

                val ua = config.userAgentMode.userAgentString
                AppLogger.d("WebViewManager", "resolveUserAgent: ${config.userAgentMode.name} mode -> ${ua?.take(60) ?: "null"}")
                return ua
            }
        }


        if (config.desktopMode) {
            AppLogger.d("WebViewManager", "resolveUserAgent: desktopMode fallback")
            return DESKTOP_USER_AGENT ?: DESKTOP_USER_AGENT_FALLBACK
        }


        val legacyUa = config.userAgent?.takeIf { it.isNotBlank() }
        AppLogger.d("WebViewManager", "resolveUserAgent: DEFAULT mode, legacyUA=${legacyUa?.take(60) ?: "null"}")
        return legacyUa
    }




    private fun createWebViewClient(
        config: WebViewConfig,
        callbacks: WebViewCallbacks
    ): WebViewClient {

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







                    if (com.webtoapp.core.extension.ExtensionResourceInterceptor.isChromeExtensionUrl(url)) {
                        return com.webtoapp.core.extension.ExtensionResourceInterceptor.intercept(context, url)
                    }



                    val resType = if (url.startsWith("http://") || url.startsWith("https://")) inferResourceType(it) else null
                    if (resType != null) {


                        if (com.webtoapp.core.extension.WebRequestBridge.shouldBlock(url, resType)) {
                            AppLogger.d("WebViewManager", "WebRequest extension blocked: $url")
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }


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

                                    AppLogger.d("WebViewManager", "DNR redirect: $url -> ${dnrResult.redirectUrl}")
                                }
                                com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.ALLOW,
                                com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.ALLOW_ALL_REQUESTS -> {

                                    return super.shouldInterceptRequest(view, request)
                                }
                                else -> {  }
                            }
                        }
                    }




                    if (url.startsWith("https://localhost/__ext__/")) {
                        val extResourcePath = url.removePrefix("https://localhost/__ext__/")

                        val chromeExtUrl = "chrome-extension://$extResourcePath"
                        return com.webtoapp.core.extension.ExtensionResourceInterceptor.intercept(context, chromeExtUrl)
                    }



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


                    if (url.startsWith("file:///android_asset/")) {
                        val assetPath = url.removePrefix("file:///android_asset/")
                        return loadEncryptedAsset(assetPath)
                    }

                    val bypassAggressiveNetworkHooks = shouldBypassAggressiveNetworkHooks(it, url)
                    if (bypassAggressiveNetworkHooks && it.isForMainFrame) {
                        AppLogger.d("WebViewManager", "Strict compatibility mode: bypass request interception for $url")
                    }



                    val urlScheme = com.webtoapp.core.perf.NativePerfEngine.checkUrlScheme(url)

                    val isHttpOrHttps = urlScheme == 1 || urlScheme == 2
                    val isLocalhost = url.startsWith("https://localhost/__local__/")
                    val isThirdParty = if (!bypassAggressiveNetworkHooks && isHttpOrHttps && !isLocalhost)
                        isThirdPartySubResourceRequest(it) else false
                    val isMapTile = if (isThirdParty) isMapTileRequest(url) else false

                    if (!it.isForMainFrame &&
                        isHttpOrHttps &&
                        OAuthCompatEngine.shouldRedirectToCustomTab(url) &&
                        shouldLaunchExternalOAuthFromSubframe(url, currentMainFrameUrl)) {
                        scheduleExternalOAuthLaunch(view, url)
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }




                    val isCaptchaRequest = if (isHttpOrHttps) isCaptchaServiceRequest(url) else false





                    if (!bypassAggressiveNetworkHooks &&
                        !config.disableShields &&
                        config.enableTrackerBlocking &&
                        isThirdParty &&
                        !isMapTile &&
                        !isCaptchaRequest &&
                        ::shields.isInitialized && shields.isEnabled() && shields.getConfig().trackerBlocking) {
                        val trackerCategory = shields.trackerBlocker.checkTracker(url)
                        if (trackerCategory != null) {
                            shields.stats.recordTrackerBlocked(trackerCategory)
                            diagBlockedCount++
                            AppLogger.d("WebViewManager", "Tracker blocked [$trackerCategory]: ${url.take(80)}")
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                    }




                    if (!bypassAggressiveNetworkHooks &&
                        !isMapTile &&
                        !isCaptchaRequest &&
                        adBlocker.isEnabled()) {
                        val adResType = resType ?: inferResourceType(it)
                        val pageHost = extractHostFromUrl(currentMainFrameUrl)
                        if (adBlocker.shouldBlock(url, pageHost, adResType, isThirdParty)) {
                            if (::shields.isInitialized) shields.stats.recordAdBlocked()
                            diagBlockedCount++
                            AppLogger.d("WebViewManager", "Ad blocked [${if (isThirdParty) "3P" else "1P"}/$adResType]: ${url.take(80)}")
                            return adBlocker.createEmptyResponse(adResType)
                        }
                    }




                    val isOAuthRequest = if (isHttpOrHttps) isOAuthServiceRequest(url) else false




                    val mainFrameUrl = currentMainFrameUrl
                    val isOAuthPageSubResource = !isOAuthRequest && isHttpOrHttps &&
                        mainFrameUrl != null && isOAuthServiceRequest(mainFrameUrl)







                    if (!bypassAggressiveNetworkHooks &&
                        config.enableCrossOriginIsolation &&
                        isHttpOrHttps &&
                        !it.isForMainFrame &&
                        !isCaptchaRequest &&
                        !isOAuthRequest &&
                        !isOAuthPageSubResource) {
                        AppLogger.d("WebViewManager", "Cross-origin proxy: ${url.take(80)}")
                        return fetchWithCrossOriginHeaders(it)
                    }












                    // NOTE: 这里曾经把所有非本地 http:// 请求转交给一个独立的 OkHttp
                    // 客户端再喂回 WebView。这会造成：
                    //   1. cookie 隔离（cleartextProxyClient 没有 CookieJar，与
                    //      android.webkit.CookieManager 完全不通），所以 SPA / SSO /
                    //      鉴权站点的二跳页会丢登录态（B 站视频页空白即此症状）；
                    //   2. main frame 被代理时 WebView 的真实 URL 会与渲染内容错位，
                    //      WebBackForwardList 的条目失效，导致 goBack() 时 URL 切换
                    //      但页面不刷新；
                    //   3. POST/PUT/PATCH 的 body 被 WebResourceRequest 接口吞掉，
                    //      代理端收到的全是空 body。
                    // AndroidManifest 里 cleartextTrafficPermitted 已经为 true，
                    // 留给 WebView 自己处理 http:// 即可。私网 host 的 http 资源也
                    // 会走系统正常路径。
                }

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








                if (request.isForMainFrame && config.enableOAuthExternalRedirect && OAuthCompatEngine.shouldRedirectToCustomTab(url)) {
                    val provider = OAuthCompatEngine.getProviderType(url)
                    AppLogger.i("WebViewManager", "Google OAuth detected [$provider] — opening outside WebView: $url")
                    view?.stopLoading()
                    openInCustomTab(url)
                    return true
                }



                if (request.isForMainFrame && OAuthCompatEngine.isOAuthUrl(url)) {
                    val provider = OAuthCompatEngine.getProviderType(url)
                    AppLogger.d("WebViewManager", "OAuth detected [$provider] — allowing in-WebView with kernel disguise: $url")

                }








                val targetHost = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull()
                val isPrivateNetworkHost = targetHost != null && isLocalCleartextHost(targetHost)
                if (config.enableHttpsUpgrade && !config.disableShields && !isPrivateNetworkHost && ::shields.isInitialized && shields.isEnabled() && shields.getConfig().httpsUpgrade) {
                    val upgradedUrl = shields.httpsUpgrader.tryUpgrade(url)
                    if (upgradedUrl != null) {
                        shields.stats.recordHttpsUpgrade()
                        view?.loadUrl(upgradedUrl)
                        return true
                    }
                }


                if (handleSpecialUrl(url, isUserGesture)) {
                    return true
                }

                // Base64 编码的 deep link 解码：某些 App（如 B 站）的网页版"下载 App"按钮
                // 会把 bilibili://video/xxx 这类 scheme URL 做 base64 编码后作为 location.href 跳转。
                // WebView 收到的 URL 是一串无 scheme 的 base64 文本，正常流程无法处理。
                // 开启此功能后，尝试 base64 解码，如果解码结果是合法的 deep link 就走 handleSpecialUrl。
                if (currentConfig?.decodeBase64DeepLinks == true && isUserGesture) {
                    val decoded = tryDecodeBase64DeepLink(url)
                    if (decoded != null && handleSpecialUrl(decoded, true)) {
                        return true
                    }
                }


                if (config.openExternalLinks && isExternalUrl(url, view?.url)) {
                    callbacks.onExternalLink(url)
                    return true
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                currentMainFrameUrl = url
                extensionPanelInjected = false
                view?.let {
                    userscriptInjectionState.remove(it)
                    pagePhaseExecutionState.remove(it)
                }




                view?.let { wv ->
                    try {
                        val list = wv.copyBackForwardList()
                        val currentIndex = list.currentIndex
                        if (previousHistoryIndex >= 0 && currentIndex < previousHistoryIndex) {
                            isNavigatingBack = true
                        }
                        previousHistoryIndex = currentIndex
                    } catch (e: Exception) {

                    }
                }


                diagPageStartTime = System.currentTimeMillis()
                diagRequestCount = 0
                diagBlockedCount = 0
                diagErrorCount = 0




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

                lastFailedUrl = null

                if (url != null && url != fileRetryUrl) {
                    fileRetryCount = 0
                    fileRetryUrl = null
                }
                if (url != null && url != loopbackMainFrameRetryUrl) {
                    cancelLoopbackMainFrameRetry()
                }

                if (::shields.isInitialized) shields.onPageStarted(url)

                adBlocker.invalidateCache()



                val isBack = isNavigatingBack




                view?.let { wv ->
                    com.webtoapp.core.disguise.BrowserDisguiseEngine.injectOnPageStarted(
                        webView = wv,
                        url = url,
                        disguiseConfig = cachedBrowserDisguiseConfig,
                        cachedDisguiseJs = cachedBrowserDisguiseJs,
                        enableDiagnostic = false
                    )
                }


                if (config.enableClipboardPolyfill) {
                    view?.evaluateJavascript(CLIPBOARD_POLYFILL_JS, null)
                }

                if (config.enableNotificationPolyfill) {
                    view?.let { injectNotificationPolyfill(it) }
                }

                view?.let { injectPrivateNetworkApiBridgeFallback(it, url) }

                if (config.enableScrollMemory) {
                    view?.evaluateJavascript(SCROLL_SAVE_JS, null)
                }

                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {
                    view?.evaluateJavascript(VIEWPORT_FIT_SCREEN_JS, null)
                }

                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
                    val customWidth = config.customViewportWidth.coerceIn(320, 3840)
                    val customJs = VIEWPORT_CUSTOM_JS.replace("CUSTOM_WIDTH_PLACEHOLDER", customWidth.toString())
                    view?.evaluateJavascript(customJs, null)
                }


                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_START, url) }
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                currentMainFrameUrl = url ?: currentMainFrameUrl
                // 注意：commit visible 对 WebView 的内置错误页（"Webpage not available"）也会触发，
                // 不能仅靠 url 匹配判断 loopback 主帧加载成功——会误清状态导致 retry 永久循环。
                // recovered 的清理改到 onPageFinished 中，按 diagErrorCount==0 才判成功。
                val elapsed = System.currentTimeMillis() - diagPageStartTime
                AppLogger.d("WebViewManager", "Page commit visible: +${elapsed}ms blocked=$diagBlockedCount")
                callbacks.onPageCommitVisible(url)


                if (isNavigatingBack) {
                    view?.evaluateJavascript(SCROLL_RESTORE_JS, null)
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)


                callbacks.onUrlChanged(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentMainFrameUrl = url ?: currentMainFrameUrl
                val elapsed = System.currentTimeMillis() - diagPageStartTime
                AppLogger.d("WebViewManager", "Page finished: +${elapsed}ms blocked=$diagBlockedCount errors=$diagErrorCount")

                if (url != null && url.startsWith("file://")) {
                    fileRetryCount = 0
                    fileRetryUrl = null
                }
                // 权威成功判定：Loopback URL 真正加载成功的唯一可靠信号是
                // onPageFinished 时主帧没收到任何 onReceivedError（diagErrorCount==0）。
                // 此时无论是否还在 pending retry，都要立即取消（包括撤掉 postDelayed 排好的任务），
                // 否则会出现"已经成功又被假重试覆盖"的循环。
                if (url != null && isLocalRuntimeUrl(url) && diagErrorCount == 0) {
                    cancelLoopbackMainFrameRetry()
                }

                val currentUrl = url ?: view?.url
                val endPhaseKey = buildPagePhaseExecutionKey(currentUrl, ScriptRunTime.DOCUMENT_END)
                val idlePhaseKey = buildPagePhaseExecutionKey(currentUrl, ScriptRunTime.DOCUMENT_IDLE)
                val shouldRunDocumentEnd = view != null && markPagePhaseExecuted(view, endPhaseKey)
                val shouldRunDocumentIdle = view != null && markPagePhaseExecuted(view, idlePhaseKey)

                if (!shouldRunDocumentEnd) {
                    AppLogger.d("WebViewManager", "Skip duplicate DOCUMENT_END processing for: $currentUrl")
                    return
                }

                view?.let { injectScripts(it, config.injectScripts, ScriptRunTime.DOCUMENT_END, url) }

                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.FIT_SCREEN) {

                    view?.evaluateJavascript("window.__wtaViewportFitApplied=false;", null)
                    view?.evaluateJavascript(VIEWPORT_FIT_SCREEN_JS, null)
                }

                if (config.viewportMode == com.webtoapp.data.model.ViewportMode.CUSTOM) {
                    val customWidth = config.customViewportWidth.coerceIn(320, 3840)
                    val customJs = VIEWPORT_CUSTOM_JS.replace("CUSTOM_WIDTH_PLACEHOLDER", customWidth.toString())
                    view?.evaluateJavascript("window.__wtaViewportCustomApplied=false;", null)
                    view?.evaluateJavascript(customJs, null)
                }

                if (url != null && (url.startsWith("http://") || url.startsWith("https://")) && config.enableImageRepair) {
                    view?.evaluateJavascript(IMAGE_REPAIR_JS, null)
                }
                callbacks.onPageFinished(url)



                if (config.enableScrollMemory) {
                    view?.evaluateJavascript(SCROLL_RESTORE_JS, null)
                }

                isNavigatingBack = false



                if (url != null && OAuthCompatEngine.isOAuthUrl(url)) {
                    view?.evaluateJavascript(OAuthCompatEngine.getOAuthBlockDetectionJs(), null)
                    AppLogger.d("WebViewManager", "OAuth block detection JS injected for: $url")
                }

                if (::shields.isInitialized) shields.onPageFinished(url)




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

                                                var observerTarget = document.documentElement || document.body;
                                                if (observerTarget instanceof Node) {
                                                    observer.observe(observerTarget, {
                                                        childList: true, subtree: true
                                                    });
                                                }

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



                val finishedUrl = url
                if (shouldRunDocumentIdle) {
                    view?.postDelayed({
                        if (view.url == finishedUrl) {
                            injectScripts(view, config.injectScripts, ScriptRunTime.DOCUMENT_IDLE, view.url)
                        }
                    }, 500)
                } else {
                    AppLogger.d("WebViewManager", "Skip duplicate DOCUMENT_IDLE processing for: $finishedUrl")
                }


                if (config.performanceOptimization) {
                    view?.postDelayed({
                        if (view.url == finishedUrl) {
                            val perfScript = com.webtoapp.core.linux.PerformanceOptimizer.generatePerformanceScript()
                            view.evaluateJavascript(perfScript, null)
                            AppLogger.d("WebViewManager", "Performance optimization script injected")
                        }
                    }, 300)
                }


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


                view?.removeCallbacks(cookieFlushRunnable)
                if (config.enableCookiePersistence) {
                    view?.postDelayed(cookieFlushRunnable, 3000)
                }


                view?.requestFocus()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                val errUrl = request?.url?.toString() ?: "unknown"
                val errCode = error?.errorCode ?: -1
                val errDesc = error?.description?.toString() ?: "unknown"
                val isMain = request?.isForMainFrame == true
                val normalizedErrDesc = normalizeNetworkErrorDescription(errDesc)
                val shouldSuppressDefaultHandling = isMain && view != null && errUrl != "unknown" && errUrl != "about:blank" && (
                    (upgradeInsecureHttpUrl(errUrl) != null && isCleartextBlockedError(errCode, errDesc, normalizedErrDesc)) ||
                        shouldRetryLoopbackMainFrameRequest(errUrl, errCode, errDesc, normalizedErrDesc)
                    )
                if (!shouldSuppressDefaultHandling) {
                    super.onReceivedError(view, request, error)
                }
                diagErrorCount++
                AppLogger.d(
                    "WebViewManager",
                    "Load error [${if (isMain) "main" else "sub"}] code=$errCode desc=$errDesc url=$errUrl"
                )
                if (request?.isForMainFrame == true) {
                    val errorCode = error?.errorCode ?: -1
                    val rawDescription = error?.description?.toString() ?: "Unknown error"
                    val description = normalizeNetworkErrorDescription(rawDescription)
                    val failedUrl = request.url?.toString()


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



                    if (view != null && shouldRetryLoopbackMainFrameRequest(failedUrl, errorCode, rawDescription, description)) {
                        val isSameRetry = failedUrl == loopbackMainFrameRetryUrl
                        val currentRetry = if (isSameRetry) loopbackMainFrameRetryCount else 0
                        if (currentRetry < LOOPBACK_MAIN_FRAME_MAX_RETRIES) {
                            // 排定新 retry 前，先取消同一个 view 上可能还挂着的旧 retry runnable
                            // （避免上次错误后排队的回调与本次错误叠加，导致同一个 url 被多次 loadUrl）
                            loopbackMainFrameRetryRunnable?.let { view.removeCallbacks(it) }

                            loopbackMainFrameRetryUrl = failedUrl
                            loopbackMainFrameRetryCount = currentRetry + 1
                            loopbackMainFrameRetryPending = true
                            loopbackMainFrameRetryView = view
                            AppLogger.i(
                                "WebViewManager",
                                "Loopback main-frame load failed (code=$errorCode, desc=$rawDescription), auto-retry ${loopbackMainFrameRetryCount}/$LOOPBACK_MAIN_FRAME_MAX_RETRIES after ${LOOPBACK_MAIN_FRAME_RETRY_DELAY_MS}ms: $failedUrl"
                            )
                            val retryRunnable = Runnable {
                                val currentUrl = view.url
                                if (currentUrl == null || currentUrl == "about:blank" || currentUrl == failedUrl || currentMainFrameUrl == failedUrl) {
                                    AppLogger.d("WebViewManager", "Retrying loopback main-frame request: $failedUrl")
                                    view.loadUrl(failedUrl)
                                } else {
                                    AppLogger.d(
                                        "WebViewManager",
                                        "Skip loopback auto-retry because WebView navigated away: current=$currentUrl, failed=$failedUrl"
                                    )
                                }
                                // retry 已发出，清掉本任务引用避免下一次 onReceivedError 试图取消已执行的回调
                                loopbackMainFrameRetryRunnable = null
                            }
                            loopbackMainFrameRetryRunnable = retryRunnable
                            view.postDelayed(retryRunnable, LOOPBACK_MAIN_FRAME_RETRY_DELAY_MS)
                            return
                        } else {
                            AppLogger.w(
                                "WebViewManager",
                                "Loopback main-frame load failed after $LOOPBACK_MAIN_FRAME_MAX_RETRIES retries: $failedUrl"
                            )
                            cancelLoopbackMainFrameRetry()
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
                            view.postDelayed({
                                view.loadUrl(failedUrl)
                            }, FILE_RETRY_DELAY_MS)
                            return
                        } else {
                            AppLogger.w(
                                "WebViewManager",
                                "file:// load failed after $FILE_MAX_RETRIES retries: $failedUrl"
                            )

                            fileRetryCount = 0
                            fileRetryUrl = null
                        }
                    }


                    val manager = errorPageManager
                    if (manager != null && view != null) {
                        val errorHtml = manager.generateErrorPage(errorCode, description, rawDescription, failedUrl)
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



                    if (failedUrl != null && OAuthCompatEngine.isOAuthBlockedError(statusCode, failedUrl)) {
                        val provider = OAuthCompatEngine.getProviderType(failedUrl)
                        AppLogger.w("WebViewManager", "OAuth [$provider] $statusCode detected — staying in WebView for diagnosis: $failedUrl")
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
                val errorUrl = error?.url






                val isMainFrameSslError = if (errorUrl != null && currentMainFrameUrl != null) {
                    val errorHost = extractHostFromUrl(errorUrl)
                    val mainHost = extractHostFromUrl(currentMainFrameUrl)


                    errorHost != null && (errorHost == mainHost || currentMainFrameUrl == "about:blank")
                } else {

                    true
                }

                if (!isMainFrameSslError) {

                    handler?.cancel()
                    AppLogger.d("WebViewManager", "Sub-resource SSL error silently cancelled: $errorUrl")
                    return
                }




                if (!config.disableShields && ::shields.isInitialized && shields.isEnabled()) {
                    val sslPolicy = shields.getConfig().sslErrorPolicy

                    when (sslPolicy) {

                        com.webtoapp.core.engine.shields.SslErrorPolicy.AUTO_HTTP_FALLBACK -> {

                            var fallbackUrl = shields.httpsUpgrader.onSslError(errorUrl)
                            if (fallbackUrl != null) {
                                handler?.cancel()
                                view?.loadUrl(fallbackUrl)
                                AppLogger.d("WebViewManager", "HTTPS upgrade fallback: $fallbackUrl")
                                return
                            }

                            fallbackUrl = shields.httpsUpgrader.tryHttpFallback(errorUrl)
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


                        com.webtoapp.core.engine.shields.SslErrorPolicy.ASK_USER -> {

                            handler?.cancel()
                            callbacks.onSslError(error?.toString() ?: "SSL Error")
                            return
                        }


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






            override fun onReceivedHttpAuthRequest(
                view: WebView?,
                handler: HttpAuthHandler?,
                host: String?,
                realm: String?
            ) {
                if (handler == null || view == null) {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm)
                    return
                }


                if (handler.useHttpAuthUsernamePassword()) {
                    val credentials = view.getHttpAuthUsernamePassword(host ?: "", realm ?: "")
                    if (credentials != null && credentials.size == 2) {
                        handler.proceed(credentials[0], credentials[1])
                        AppLogger.d("WebViewManager", "HTTP Auth: 使用缓存凭据 host=$host realm=$realm")
                        return
                    }
                }


                val activity = try {
                    var ctx = view.context
                    while (ctx is android.content.ContextWrapper) {
                        if (ctx is android.app.Activity) break
                        ctx = ctx.baseContext
                    }
                    ctx as? android.app.Activity
                } catch (_: Exception) { null }

                if (activity == null || activity.isFinishing || activity.isDestroyed) {
                    handler.cancel()
                    return
                }

                AppLogger.i("WebViewManager", "HTTP Auth 请求: host=$host realm=$realm")

                activity.runOnUiThread {
                    val dialogView = android.widget.LinearLayout(activity).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(64, 32, 64, 0)


                        if (!realm.isNullOrBlank()) {
                            addView(android.widget.TextView(activity).apply {
                                text = realm
                                setTextColor(android.graphics.Color.parseColor("#888888"))
                                textSize = 13f
                                setPadding(0, 0, 0, 24)
                            })
                        }


                        addView(com.google.android.material.textfield.TextInputLayout(activity).apply {
                            hint = com.webtoapp.core.i18n.Strings.httpAuthUsername
                            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                            setBoxCornerRadii(12f, 12f, 12f, 12f)
                            addView(com.google.android.material.textfield.TextInputEditText(activity).apply {
                                tag = "auth_username"
                                inputType = android.text.InputType.TYPE_CLASS_TEXT
                                isSingleLine = true
                            })
                        })

                        addView(android.view.View(activity).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 24
                            )
                        })


                        addView(com.google.android.material.textfield.TextInputLayout(activity).apply {
                            hint = com.webtoapp.core.i18n.Strings.httpAuthPassword
                            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                            setBoxCornerRadii(12f, 12f, 12f, 12f)
                            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
                            addView(com.google.android.material.textfield.TextInputEditText(activity).apply {
                                tag = "auth_password"
                                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                                isSingleLine = true
                            })
                        })
                    }

                    val hostDisplay = host ?: "server"

                    android.app.AlertDialog.Builder(activity)
                        .setTitle(com.webtoapp.core.i18n.Strings.httpAuthTitle)
                        .setMessage(com.webtoapp.core.i18n.Strings.httpAuthMessage.format(hostDisplay))
                        .setView(dialogView)
                        .setPositiveButton(com.webtoapp.core.i18n.Strings.httpAuthLogin) { dialog, _ ->
                            val username = dialogView.findViewWithTag<android.widget.EditText>("auth_username")?.text?.toString() ?: ""
                            val password = dialogView.findViewWithTag<android.widget.EditText>("auth_password")?.text?.toString() ?: ""

                            view.setHttpAuthUsernamePassword(host ?: "", realm ?: "", username, password)
                            handler.proceed(username, password)
                            AppLogger.i("WebViewManager", "HTTP Auth: 用户已登录 host=$host")
                            dialog.dismiss()
                        }
                        .setNegativeButton(com.webtoapp.core.i18n.Strings.btnCancel) { dialog, _ ->
                            handler.cancel()
                            dialog.dismiss()
                        }
                        .setOnCancelListener {
                            handler.cancel()
                        }
                        .create()
                        .show()
                }
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
                    userscriptInjectionState.remove(goneView)
                    pagePhaseExecutionState.remove(goneView)
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

    private fun isLocalRuntimeUrl(url: String?): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return isLocalCleartextHost(host)
    }

    private fun isCleartextBlockedError(errorCode: Int, rawDescription: String, normalizedDescription: String): Boolean {
        if (errorCode == WebViewClient.ERROR_UNSAFE_RESOURCE) return true
        val merged = "$rawDescription $normalizedDescription".uppercase()
        return merged.contains("CLEARTEXT") ||
            merged.contains("ERR_CLEARTEXT_NOT_PERMITTED") ||
            merged.contains("SECURITY POLICY")
    }

    private fun shouldRetryLoopbackMainFrameRequest(
        failedUrl: String,
        errorCode: Int,
        rawDescription: String,
        normalizedDescription: String
    ): Boolean {
        if (!isLocalRuntimeUrl(failedUrl)) return false
        if (isCleartextBlockedError(errorCode, rawDescription, normalizedDescription)) return false

        val merged = "$rawDescription $normalizedDescription".uppercase()
        return errorCode == WebViewClient.ERROR_UNKNOWN ||
            errorCode == WebViewClient.ERROR_CONNECT ||
            errorCode == WebViewClient.ERROR_TIMEOUT ||
            errorCode == WebViewClient.ERROR_IO ||
            errorCode == WebViewClient.ERROR_HOST_LOOKUP ||
            merged.contains("ERR_EMPTY_RESPONSE") ||
            merged.contains("ERR_CONNECTION") ||
            merged.contains("CONNECTION_REFUSED") ||
            merged.contains("CONNECTION RESET") ||
            merged.contains("ERR_ABORTED") ||
            merged.contains("TIMED_OUT")
    }





    private fun isThirdPartySubResourceRequest(request: WebResourceRequest): Boolean {
        if (request.isForMainFrame) return false

        val requestHost = extractHostFromUrl(request.url?.toString()) ?: return false
        if (isLocalCleartextHost(requestHost)) return false



        val topLevelHost = extractHostFromUrl(currentMainFrameUrl)
            ?: extractHostFromUrl(request.requestHeaders["Referer"])
            ?: extractHostFromUrl(request.requestHeaders["referer"])
            ?: return false

        return !isSameSiteHost(requestHost, topLevelHost)
    }






    private fun isMapTileRequest(url: String): Boolean {
        val host = extractHostFromUrl(url) ?: return false
        return MAP_TILE_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }











    private fun isCaptchaServiceRequest(url: String): Boolean {
        val host = extractHostFromUrl(url) ?: return false


        if (host == "www.google.com" || host == "www.gstatic.com" || host == "apis.google.com") {
            val pathStart = url.indexOf('/', url.indexOf(host) + host.length)
            if (pathStart >= 0) {
                return url.regionMatches(pathStart, "/recaptcha", 0, 10, ignoreCase = true)
            }
            return false
        }

        return CAPTCHA_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }









    private fun isOAuthServiceRequest(url: String): Boolean {
        val host = extractHostFromUrl(url) ?: return false
        return OAUTH_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }








    private fun isCustomTabAvailable(): Boolean {
        return try {
            val pm = context.packageManager

            val chromePackages = listOf(
                "com.android.chrome",
                "com.chrome.beta",
                "com.chrome.dev",
                "com.chrome.canary",
                "com.brave.browser",
                "com.microsoft.emmx",
                "org.mozilla.firefox",
                "com.opera.browser"
            )
            val hasKnownCctBrowser = chromePackages.any { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (hasKnownCctBrowser) return true


            val serviceIntent = android.content.Intent("android.support.customtabs.action.CustomTabsService")
            val resolvedServices = pm.queryIntentServices(serviceIntent, 0)
            resolvedServices.isNotEmpty()
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "Failed to check CCT availability", e)
            false
        }
    }





    private fun shouldUseConservativeScriptMode(pageUrl: String?): Boolean {
        val url = pageUrl?.takeIf { it.isNotBlank() } ?: return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return !isLocalCleartextHost(host)
    }





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

    private fun shouldMinimizeLocalRuntimeInjection(pageUrl: String?): Boolean {
        return isLocalRuntimeUrl(pageUrl)
    }







    private fun isGoogleOAuthUrl(url: String): Boolean = OAuthCompatEngine.isOAuthUrl(url)


    private fun shouldLaunchExternalOAuthFromSubframe(oauthUrl: String, topLevelUrl: String?): Boolean {
        if (topLevelUrl != null && OAuthCompatEngine.isOAuthUrl(topLevelUrl)) return false

        val uri = runCatching { Uri.parse(oauthUrl) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        val path = uri.path?.lowercase() ?: ""

        if (path.startsWith("/gsi/") || path.startsWith("/recaptcha")) return false

        return host == "accounts.google.com" ||
            host == "accounts.youtube.com" ||
            host == "myaccount.google.com" ||
            ((host.endsWith(".google.com") || host == "google.com") &&
                (path.startsWith("/o/oauth2") || path.startsWith("/signin/oauth")))
    }


    private fun scheduleExternalOAuthLaunch(view: WebView?, url: String) {
        val now = System.currentTimeMillis()
        val lastLaunch = externalOAuthLaunchTimes[url] ?: 0L
        if (now - lastLaunch < 30_000L) return
        externalOAuthLaunchTimes[url] = now

        if (externalOAuthLaunchTimes.size > 32) {
            val cutoff = now - 120_000L
            val iterator = externalOAuthLaunchTimes.entries.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().value < cutoff) iterator.remove()
            }
        }

        AppLogger.i("WebViewManager", "OAuth subframe detected — opening outside WebView: $url")
        view?.post {
            openInCustomTab(url)
        }
    }




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






    private fun extractHostFromUrl(url: String?): String? {
        val target = url?.takeIf { it.isNotBlank() } ?: return null

        return com.webtoapp.core.perf.NativePerfEngine.extractHost(target)?.lowercase()
            ?: runCatching { Uri.parse(target).host?.lowercase() }.getOrNull()
    }





















    private fun fetchWithCrossOriginHeaders(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val url = request.url.toString()
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection


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


            val responseHeaders = mutableMapOf<String, String>()
            connection.headerFields?.forEach { (key, values) ->
                if (key != null && values.isNotEmpty()) {
                    responseHeaders[key] = values.first()
                }
            }


            responseHeaders["Cross-Origin-Opener-Policy"] = "same-origin"
            responseHeaders["Cross-Origin-Embedder-Policy"] = "require-corp"

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
            null
        }
    }








    private fun loadEncryptedAsset(assetPath: String): WebResourceResponse? {
        return try {
            val secureLoader = SecureAssetLoader.getInstance(context)


            if (!secureLoader.assetExists(assetPath)) {
                AppLogger.d("WebViewManager", "Resource not found: $assetPath")
                return null
            }


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




    private fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return MIME_TYPE_MAP[extension] ?: "application/octet-stream"
    }




    private fun isTextMimeType(mimeType: String): Boolean {
        return mimeType in TEXT_MIME_TYPES
    }





    private fun inferResourceType(request: WebResourceRequest): String {

        if (request.isForMainFrame) return "main_frame"


        val accept = request.requestHeaders?.entries?.firstOrNull {
            it.key.equals("Accept", ignoreCase = true)
        }?.value ?: ""

        if (accept.contains("text/html")) return "sub_frame"
        if (accept.contains("text/css")) return "stylesheet"
        if (accept.contains("image/")) return "image"
        if (accept.contains("font/") || accept.contains("application/font")) return "font"


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


            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return callbacks.onShowFileChooser(filePathCallback, fileChooserParams)
            }


            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                if (view == null) return false


                val href = view.hitTestResult.extra

                AppLogger.d("WebViewManager", "onCreateWindow: href=$href, behavior=${config.newWindowBehavior}")

                if (!href.isNullOrBlank() && OAuthCompatEngine.shouldRedirectToCustomTab(href)) {
                    AppLogger.i("WebViewManager", "OAuth new-window URL detected — opening outside WebView: $href")
                    openInCustomTab(href)
                    return true
                }

                val originalWebView = view

                return when (config.newWindowBehavior) {
                    NewWindowBehavior.SAME_WINDOW -> {

                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {

                            val tempWebView = WebView(context)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString()
                                    if (url != null) {
                                        val safeUrl = normalizeHttpUrlForSecurity(url)

                                        if (OAuthCompatEngine.shouldRedirectToCustomTab(safeUrl)) {
                                            AppLogger.i("WebViewManager", "OAuth popup navigation detected — opening outside WebView: $safeUrl")
                                            openInCustomTab(safeUrl)
                                        } else {
                                            originalWebView.loadUrl(safeUrl)
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
                    NewWindowBehavior.EXTERNAL_BROWSER -> {

                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null) {
                            val tempWebView = WebView(context)
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString()
                                    if (url != null) {
                                        try {
                                            val safeUrl = normalizeHttpUrlForSecurity(url)
                                            if (OAuthCompatEngine.shouldRedirectToCustomTab(safeUrl)) {
                                                AppLogger.i("WebViewManager", "OAuth external popup navigation detected — opening Custom Tab: $safeUrl")
                                                openInCustomTab(safeUrl)
                                            } else {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(safeUrl))
                                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
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

                        callbacks.onNewWindow(resultMsg)
                        true
                    }
                    NewWindowBehavior.BLOCK -> {

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
        // 不做任何 HTTP→HTTPS 强制升级
        return null
    }




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

    @SuppressLint("RestrictedApi")
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
     * 尝试将 URL 作为 base64 解码，判断解码结果是否是合法的 deep link（含 :// 的非 http(s) scheme）。
     *
     * B 站等 App 的网页版会把 `bilibili://video/xxx` 做 base64 编码后赋给 location.href，
     * WebView 收到的 URL 形如 `2233DGYSNSYmlsaWJpbGk6Ly92aWRlby8xMTY1NjY5...`——
     * 没有 scheme，无法被 handleSpecialUrl 识别。
     *
     * 安全约束：
     * - 只在用户手势触发时尝试（防止恶意页面自动跳转）
     * - 解码结果必须包含 `://` 且 scheme 不在 BLOCKED_SPECIAL_SCHEMES 里
     * - 解码失败（非法 base64 / 解码后不含 scheme）直接返回 null，不影响正常流程
     */
    private fun tryDecodeBase64DeepLink(url: String): String? {
        // 快速排除：正常 URL 都有 scheme（含 ://），base64 编码的 deep link 不会有
        if (url.contains("://")) return null
        // base64 字符集：A-Z a-z 0-9 + / = 以及 URL-safe 变体 - _
        // 如果 URL 含有 base64 不可能出现的字符，直接跳过
        if (!url.matches(Regex("^[A-Za-z0-9+/=_-]+$"))) return null
        // 太短的不可能是有效 deep link
        if (url.length < 10) return null

        return try {
            // 尝试标准 base64 和 URL-safe base64 两种
            val decoded = try {
                String(android.util.Base64.decode(url, android.util.Base64.DEFAULT), Charsets.UTF_8)
            } catch (_: Exception) {
                String(android.util.Base64.decode(url, android.util.Base64.URL_SAFE), Charsets.UTF_8)
            }
            // 解码结果必须是 scheme://... 格式
            val schemeEnd = decoded.indexOf("://")
            if (schemeEnd <= 0) return null
            val scheme = decoded.substring(0, schemeEnd).lowercase()
            // 安全检查
            if (scheme in BLOCKED_SPECIAL_SCHEMES) return null
            if (scheme == "http" || scheme == "https") return null // 不处理普通 URL
            AppLogger.d("WebViewManager", "Decoded base64 deep link: $decoded (from: ${url.take(30)}...)")
            decoded
        } catch (_: Exception) {
            null
        }
    }




    private fun handleSpecialUrl(url: String, isUserGesture: Boolean): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false


        if (scheme == "http" || scheme == "https") {
            return false
        }




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

                                addCategory(android.content.Intent.CATEGORY_BROWSABLE)

                                selector?.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                            }
                        }
                    } catch (e: java.net.URISyntaxException) {
                        AppLogger.e("WebViewManager", "Invalid intent URI: $url", e)
                        null
                    }
                }
                else -> {

                    android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }

            if (intent != null) {

                val fallbackUrl = if (scheme == "intent") {
                    sanitizeFallbackUrl(intent.getStringExtra("browser_fallback_url"))
                } else null




                try {

                    val resolveInfo = context.packageManager.resolveActivity(
                        intent,
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )

                    if (resolveInfo != null) {
                        AppLogger.d("WebViewManager", "Resolved activity: ${resolveInfo.activityInfo?.packageName}")
                        context.startActivity(intent)
                        return true
                    }



                    AppLogger.d("WebViewManager", "resolveActivity returned null, trying direct launch")
                    context.startActivity(intent)
                    return true

                } catch (e: android.content.ActivityNotFoundException) {
                    AppLogger.w("WebViewManager", "No activity found for intent", e)

                    if (!fallbackUrl.isNullOrEmpty()) {
                        AppLogger.d("WebViewManager", "Using fallback URL: $fallbackUrl")

                        managedWebViews.keys.firstOrNull()?.loadUrl(fallbackUrl)
                        return true
                    }

                    return true
                } catch (e: SecurityException) {
                    AppLogger.e("WebViewManager", "Security exception launching intent", e)

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

            AppLogger.w("WebViewManager", "Error handling special URL: $scheme", e)
            true
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




    private fun isExternalUrl(targetUrl: String, currentUrl: String?): Boolean {
        if (currentUrl == null) return false
        val targetHost = runCatching { Uri.parse(targetUrl).host?.lowercase() }.getOrNull() ?: return false
        val currentHost = runCatching { Uri.parse(currentUrl).host?.lowercase() }.getOrNull() ?: return false
        return !targetHost.endsWith(currentHost) && !currentHost.endsWith(targetHost)
    }





    fun destroyWebView(webView: WebView) {
        try {
            managedWebViews.remove(webView)
            extensionPanelSyncJob?.cancel()
            extensionPanelSyncJob = null
            extensionPanelDeferredInjectionJob?.cancel()
            extensionPanelDeferredInjectionJob = null
            extensionPanelInjected = false

            webView.apply {

                stopLoading()


                clearHistory()


                webChromeClient = null
                webViewClient = object : WebViewClient() {}


                removeJavascriptInterface("NativeBridge")
                removeJavascriptInterface("DownloadBridge")
                removeJavascriptInterface("NativeShareBridge")
                removeJavascriptInterface(com.webtoapp.core.extension.GreasemonkeyBridge.JS_INTERFACE_NAME)
                removeJavascriptInterface(com.webtoapp.core.extension.ChromeExtensionRuntime.JS_BRIDGE_NAME)






                (parent as? android.view.ViewGroup)?.removeView(this)


                destroy()
            }

            AppLogger.d("WebViewManager", "WebView resources cleaned up")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to cleanup WebView", e)
        }
    }




    fun destroyAll() {
        proxyApplyJob?.cancel()
        proxyApplyJob = null
        privateNetworkScriptHandlers.values.toList().forEach { handler ->
            runCatching { handler.remove() }
        }
        privateNetworkScriptHandlers.clear()
        downloadBridgeScriptHandlers.values.toList().forEach { handler ->
            runCatching { handler.remove() }
        }
        downloadBridgeScriptHandlers.clear()
        managedWebViews.keys.toList().forEach { webView ->
            destroyWebView(webView)
        }
        managedWebViews.clear()
        gmBridge?.destroy()
        gmBridge = null

        extensionRuntimes.values.forEach { it.destroy() }
        extensionRuntimes.clear()

        PacProxyManager(context).clearProxy()

        GeckoViewEngine.applyProxyConfig(
            ProxyConfig(mode = "NONE")
        )
    }




    fun getShields(): BrowserShields? = if (::shields.isInitialized) shields else null

    private fun installPrivateNetworkApiBridge(webView: WebView, config: WebViewConfig) {
        if (!config.javaScriptEnabled) return
        if (!config.domStorageEnabled) return
        if (privateNetworkScriptHandlers.containsKey(webView)) return

        val rules = setOf("*")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                privateNetworkScriptHandlers[webView] = WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    PrivateNetworkApiBridgeScriptHolder.SCRIPT,
                    rules
                )
                AppLogger.d("WebViewManager", "Private network API bridge installed at document start")
            } catch (e: Exception) {
                AppLogger.w("WebViewManager", "Document-start private network bridge install failed", e)
            }
        } else {
            AppLogger.d("WebViewManager", "Document-start script unsupported; private network bridge will use runtime fallback")
        }
    }

    // 在页面任何 JS 之前注入 DownloadBridge
    // 真正的 document-start 时机，保证 URL.createObjectURL 和 <a>.click 的 hook
    // 在页面 inline script 执行前就已就位，避免 blob: URL 下载因 hook 晚到而失效
    private fun installDownloadBridgeDocumentStart(webView: WebView) {
        if (downloadBridgeScriptHandlers.containsKey(webView)) return
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            AppLogger.i("WebViewManager", "[DownloadBridge] Document-start script unsupported on this WebView version; falling back to onPageStarted/onPageFinished injection")
            return
        }
        try {
            downloadBridgeScriptHandlers[webView] = WebViewCompat.addDocumentStartJavaScript(
                webView,
                DownloadBridge.getInjectionScript(),
                setOf("*")
            )
            AppLogger.i("WebViewManager", "[DownloadBridge] Installed at document start (applies to all hosts)")
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "[DownloadBridge] Document-start install failed, will use onPageStarted fallback", e)
        }
    }

    private fun injectPrivateNetworkApiBridgeFallback(webView: WebView, pageUrl: String?) {
        if (!isLocalRuntimeUrl(pageUrl ?: webView.url)) return
        try {
            webView.evaluateJavascript(PrivateNetworkApiBridgeScriptHolder.SCRIPT, null)
        } catch (e: Exception) {
            AppLogger.w("WebViewManager", "Private network bridge fallback injection failed", e)
        }
    }

    internal fun getPrivateNetworkApiBridgeScript(): String = PrivateNetworkApiBridgeScriptHolder.SCRIPT


    private var currentConfig: WebViewConfig? = null


    private var cachedBrowserDisguiseJs: String? = null

    private var cachedBrowserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null

    private var currentDeviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null






    private fun initChromeExtensionRuntimes(webView: WebView) {

        extensionRuntimes.values.forEach { it.destroy() }
        extensionRuntimes.clear()

        try {
            val allChromeExtModules = getActiveModulesForCurrentApp().filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.chromeExtId.isNotEmpty()
            }

            if (allChromeExtModules.isEmpty()) return

            val chromeExtRuntimeModules = allChromeExtModules.filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                    module.chromeExtId.isNotEmpty() &&
                    (module.backgroundScript.isNotEmpty() || module.manifestJson.contains("declarative_net_request"))
            }


            val extensionGroups = chromeExtRuntimeModules.groupBy { it.chromeExtId }

            for ((extId, modules) in extensionGroups) {
                val primaryModule = modules.first()
                val originUrl = com.webtoapp.core.extension.deriveOriginUrl(primaryModule.urlMatches)

                val runtime = com.webtoapp.core.extension.ChromeExtensionRuntime(
                    context = context,
                    extensionId = extId,
                    backgroundScriptPath = primaryModule.backgroundScript,
                    originUrl = originUrl,
                    manifestJson = primaryModule.manifestJson.ifBlank { "{}" }
                )
                runtime.initialize(webView)
                extensionRuntimes[extId] = runtime
                loadDeclarativeNetRequestRules(extId, primaryModule.manifestJson)
                AppLogger.d("WebViewManager", "Created background runtime for extension: $extId")
            }


            if (allChromeExtModules.isNotEmpty()) {
                val contentBridge = com.webtoapp.core.extension.ContentExtensionBridge(
                    runtimes = extensionRuntimes,
                    currentWebViewProvider = { webView },
                    openPopupHandler = { extId, popupPath -> showChromeExtensionPage(extId, popupPath) },
                    openOptionsPageHandler = { extId, optionsPath -> showChromeExtensionPage(extId, optionsPath) }
                )
                webView.addJavascriptInterface(contentBridge, com.webtoapp.core.extension.ChromeExtensionRuntime.JS_BRIDGE_NAME)
                AppLogger.d(
                    "WebViewManager",
                    "Registered WtaExtBridge for ${allChromeExtModules.map { it.chromeExtId }.distinct().size} extension(s)"
                )
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to init Chrome Extension runtimes", e)
        }
    }








    private fun injectScripts(webView: WebView, scripts: List<com.webtoapp.data.model.UserScript>, runAt: ScriptRunTime, pageUrl: String? = null) {

        val url = pageUrl ?: webView.url ?: ""
        val conservativeMode = shouldUseConservativeScriptMode(url)
        val scriptlessMode = shouldUseScriptlessMode(url)
        val minimizeLocalRuntimeInjection = shouldMinimizeLocalRuntimeInjection(url)


        if (runAt == ScriptRunTime.DOCUMENT_END &&
            !minimizeLocalRuntimeInjection &&
            currentConfig?.downloadEnabled == true &&
            currentConfig?.enableBlobDownloadInterception == true) {
            // DOCUMENT_END 兜底注入一次（脚本自身幂等，_downloadBridgeInjected 会防止重复执行）
            // 防止 onPageStarted 时机因某些页面错过
            injectDownloadBridgeScript(webView)
        }

        if (runAt == ScriptRunTime.DOCUMENT_START) {
            // Download bridge 对 blob/data URL 下载是必需的（Android WebView 原生不支持 blob: 下载 scheme）
            // 它只 hook URL.createObjectURL/revokeObjectURL 和 <a>.click，无兼容性风险
            // 不受 conservativeMode/scriptlessMode 限制——对所有远程 https 站点都要装
            if (!minimizeLocalRuntimeInjection && currentConfig?.downloadEnabled == true && currentConfig?.enableBlobDownloadInterception == true) {
                injectDownloadBridgeScript(webView)
            } else if (minimizeLocalRuntimeInjection) {
                AppLogger.d("WebViewManager", "Skip download bridge for local runtime page: $url")
            }
            if (!scriptlessMode && !minimizeLocalRuntimeInjection) {
                injectExtensionPanelScript(webView)
            } else {
                AppLogger.d(
                    "WebViewManager",
                    if (minimizeLocalRuntimeInjection) {
                        "Skip extension panel injection for local runtime page: $url"
                    } else {
                        "Skip extension panel injection for scriptless page: $url"
                    }
                )
            }


            if (!conservativeMode && !minimizeLocalRuntimeInjection) {
                injectIsolationScript(webView)
            } else if (minimizeLocalRuntimeInjection) {
                AppLogger.d("WebViewManager", "Skip isolation script for local runtime page: $url")
            }

            if (minimizeLocalRuntimeInjection) {
                injectPrivateNetworkApiBridgeFallback(webView, url)
            }


            if (!scriptlessMode && !minimizeLocalRuntimeInjection) {
                injectCompatibilityScripts(webView, url, conservativeMode)
            } else {
                AppLogger.d(
                    "WebViewManager",
                    if (minimizeLocalRuntimeInjection) {
                        "Skip compatibility scripts for local runtime page: $url"
                    } else {
                        "Scriptless mode enabled for strict host: $url"
                    }
                )
            }
        }

        if (scriptlessMode) {
            AppLogger.d("WebViewManager", "Scriptless mode: skip user/module injections (${runAt.name})")
            return
        }


        scripts.filter { it.enabled && it.runAt == runAt }
            .forEach { script ->
                try {
                    if (minimizeLocalRuntimeInjection && script.name in setOf("__kernel__", "__perf_start__", "__perf_end__")) {
                        AppLogger.d("WebViewManager", "Skip shell script ${script.name} for local runtime page: $url")
                        return@forEach
                    }

                    val actualCode = if (com.webtoapp.core.script.UserScriptStorage.isFileReference(script.code)) {
                        com.webtoapp.core.script.UserScriptStorage.loadScriptCode(context, script.code)
                    } else {
                        script.code
                    }

                    if (actualCode.isBlank()) return@forEach


                    val wrappedCode = """
                        (function() {
                            try {
                                $actualCode
                            } catch(e) {
                                console.error('[UserScript: ${script.name}] Error:', e);
                            }
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(wrappedCode, null)
                    AppLogger.d("WebViewManager", "Inject script: ${script.name} (${runAt.name}, ${actualCode.length} chars)")
                } catch (e: Exception) {
                    AppLogger.e("WebViewManager", "Script injection failed: ${script.name}", e)
                }
            }

        if (minimizeLocalRuntimeInjection) {
            AppLogger.d("WebViewManager", "Skip extension/module injections for local runtime page (${runAt.name}): $url")
            return
        }


        injectAllExtensionModules(webView, url, runAt)
    }

    private fun buildPagePhaseExecutionKey(url: String?, runAt: ScriptRunTime): String {
        return "${runAt.name}|${url ?: "about:blank"}"
    }

    private fun markPagePhaseExecuted(webView: WebView, key: String): Boolean {
        val executed = pagePhaseExecutionState.getOrPut(webView) { mutableSetOf() }
        return executed.add(key)
    }





    private fun injectDownloadBridgeScript(webView: WebView) {
        try {
            val script = DownloadBridge.getInjectionScript()
            webView.evaluateJavascript(script, null)
            AppLogger.d("WebViewManager", "Download bridge script injected")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Download bridge script injection failed", e)
        }
    }






    private data class ExtensionPanelEligibility(
        val hasEmbeddedModules: Boolean,
        val hasAppModules: Boolean,
        val hasGlobalModules: Boolean,
        val isLoading: Boolean,
        val totalEnabledModules: Int,
    ) {
        val shouldInject: Boolean
            get() = hasEmbeddedModules || hasAppModules || hasGlobalModules
    }

    private fun getExtensionPanelEligibility(): ExtensionPanelEligibility {
        val extensionManager = ExtensionManager.getInstance(context)
        val hasEmbeddedModules = embeddedModules.any { it.enabled && it.shouldRegisterInPanel() }
        val appModules = if (appExtensionModuleIds.isNotEmpty()) {
            runCatching { extensionManager.getModulesByIds(appExtensionModuleIds) }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val hasAppModules = appModules.any { it.enabled && it.shouldRegisterInPanel() }
        val enabledModules = runCatching { extensionManager.getEnabledModules() }.getOrElse { emptyList() }
        val hasGlobalModules = allowGlobalModuleFallback && enabledModules.any { it.shouldRegisterInPanel() }
        return ExtensionPanelEligibility(
            hasEmbeddedModules = hasEmbeddedModules,
            hasAppModules = hasAppModules,
            hasGlobalModules = hasGlobalModules,
            isLoading = extensionManager.isLoading.value,
            totalEnabledModules = enabledModules.size,
        )
    }

    private fun logExtensionPanelEligibility(prefix: String, eligibility: ExtensionPanelEligibility) {
        AppLogger.d(
            "WebViewManager",
            "$prefix: shouldInject=${eligibility.shouldInject}, loading=${eligibility.isLoading}, " +
                "embedded=${eligibility.hasEmbeddedModules}, app=${eligibility.hasAppModules}, " +
                "global=${eligibility.hasGlobalModules}, enabled=${eligibility.totalEnabledModules}, " +
                "appIds=${appExtensionModuleIds.size}, embedded=${embeddedModules.size}, " +
                "allowGlobal=$allowGlobalModuleFallback, fabIcon=${extensionFabIcon.take(32)}"
        )
    }

    private fun startDeferredExtensionPanelInjection(webView: WebView) {
        extensionPanelDeferredInjectionJob?.cancel()
        extensionPanelDeferredInjectionJob = proxyScope.launch {
            val extensionManager = ExtensionManager.getInstance(context)
            extensionManager.isLoading.filter { !it }.first()
            val eligibility = getExtensionPanelEligibility()
            logExtensionPanelEligibility("Deferred extension panel eligibility", eligibility)
            val url = webView.url?.takeIf { it.isNotBlank() && it != "about:blank" }
            if (eligibility.shouldInject && url != null && !shouldUseScriptlessMode(url)) {
                injectExtensionPanelScript(webView)
            }
        }
    }

    private fun startExtensionPanelSync(webView: WebView) {
        extensionPanelSyncJob?.cancel()
        extensionPanelSyncJob = proxyScope.launch {
            val extensionManager = ExtensionManager.getInstance(context)
            combine(
                extensionManager.modules,
                extensionManager.builtInModules,
                extensionManager.isLoading
            ) { modules, builtIns, loading ->
                Triple(modules, builtIns, loading)
            }
                .distinctUntilChanged()
                .collect {
                    val eligibility = getExtensionPanelEligibility()
                    logExtensionPanelEligibility("Extension panel sync", eligibility)
                    val url = webView.url?.takeIf { it.isNotBlank() && it != "about:blank" }
                        ?: return@collect
                    if (shouldUseScriptlessMode(url)) {
                        hideExtensionPanel(webView)
                        extensionPanelInjected = false
                        return@collect
                    }
                    if (eligibility.shouldInject && !extensionPanelInjected) {
                        injectExtensionPanelScript(webView)
                    } else if (!eligibility.shouldInject && extensionPanelInjected) {
                        hideExtensionPanel(webView)
                        extensionPanelInjected = false
                    }
                }
        }
    }

    private fun hideExtensionPanel(webView: WebView) {
        try {
            webView.evaluateJavascript(
                """
                (function(){
                    try {
                        if (window.__WTA_PANEL__ && typeof window.__WTA_PANEL__.hideFab === 'function') {
                            window.__WTA_PANEL__.hideFab();
                        }
                    } catch(e) {}
                })();
                """.trimIndent(),
                null
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Extension panel hide failed", e)
        }
    }

    private fun injectExtensionPanelScript(webView: WebView) {


        val eligibility = getExtensionPanelEligibility()
        logExtensionPanelEligibility("Extension panel injection check", eligibility)
        if (!eligibility.shouldInject) {
            if (eligibility.isLoading) {
                startDeferredExtensionPanelInjection(webView)
            }
            return
        }

        try {

            val panelScript = ExtensionPanelScript.getPanelInitScript(extensionFabIcon)
            webView.evaluateJavascript(panelScript, null)


            val helperScript = ExtensionPanelScript.getModuleHelperScript()
            webView.evaluateJavascript(helperScript, null)

            extensionPanelInjected = true
            AppLogger.d("WebViewManager", "Extension panel script injected")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Extension panel script injection failed", e)
        }
    }





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
                            var observerTarget = document.documentElement || document.body;
                            if (observerTarget instanceof Node) {
                                observer.observe(observerTarget, {
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


            if (!conservativeMode) {
                scripts.add(getNotificationPolyfillScript())
            }

            if (config.enableOrientationPolyfill) {
                scripts.add(ORIENTATION_POLYFILL_JS)
            }



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









            if (config.hideUrlPreview && !conservativeMode) {
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

                    if (document.body instanceof Node) {
                        titleObserver.observe(document.body, { childList: true, subtree: true });
                    } else {
                        document.addEventListener('DOMContentLoaded', function() {
                            if (document.body instanceof Node) {
                                titleObserver.observe(document.body, { childList: true, subtree: true });
                            }
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

                            // ★ FIX: Same-origin window.open is always legitimate (menus, search, navigation)
                            // Never block it — many websites use window.open for UI interactions
                            if (isSameOrigin) {
                                shouldBlock = false;
                            }
                            // Block javascript: URLs (common popup tricks, but rarely legitimate)
                            else if (url && url.indexOf('javascript:') === 0) {
                                shouldBlock = true;
                            }
                            // Block about:blank only when clearly a popup trick (no target name, no features)
                            else if (!url || url === 'about:blank') {
                                // about:blank with target/features is often legitimate (iframe containers, etc.)
                                if (target || features) {
                                    shouldBlock = false;
                                } else {
                                    shouldBlock = true;
                                }
                            }
                            // Block suspicious cross-origin URLs
                            else if (isSuspiciousUrl(url) && !isDomainAllowed(url)) {
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
                            // Block immediate timeouts that are clearly popup triggers
                            // Only block string-based eval with window.open, not function callbacks
                            if (delay === 0 && typeof fn === 'string' && fn.indexOf('window.open(') !== -1) {
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


            if (config.enableCompatPolyfills) {
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
            }

            val canInjectShieldsJs = !conservativeMode


            if (canInjectShieldsJs && !config.disableShields && config.enableGpc && ::shields.isInitialized && shields.isEnabled() && shields.getConfig().gpcEnabled) {
                scripts.add(shields.gpcInjector.generateScript())
            }


            if (canInjectShieldsJs && !config.disableShields && config.enableCookieConsentBlock && ::shields.isInitialized && shields.isEnabled() && shields.getConfig().cookieConsentBlock) {
                scripts.add(shields.cookieConsentBlocker.generateScript())
                shields.stats.recordCookieConsentBlocked()
            }


            if (canInjectShieldsJs && !config.disableShields && config.enableReferrerPolicy && ::shields.isInitialized && shields.isEnabled()) {
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



                    val antiAdblockScript = adBlocker.getAntiAdblockScript(adPageHost)
                    if (antiAdblockScript.isNotEmpty()) {
                        scripts.add(antiAdblockScript)
                    }
                }
            }


            val combinedScript = scripts.joinToString("\n\n")
            webView.evaluateJavascript(combinedScript, null)
            AppLogger.d("WebViewManager", "Browser compatibility scripts injected")

        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Compatibility script injection failed", e)
        }
    }






    private fun ScriptRunTime.toModuleRunTime(): ModuleRunTime = when (this) {
        ScriptRunTime.DOCUMENT_START -> ModuleRunTime.DOCUMENT_START
        ScriptRunTime.DOCUMENT_END -> ModuleRunTime.DOCUMENT_END
        ScriptRunTime.DOCUMENT_IDLE -> ModuleRunTime.DOCUMENT_IDLE
    }





    private fun resolveActiveExtensionModules(): List<com.webtoapp.core.extension.ExtensionModule> {
        val baseModules = when {
            appExtensionModuleIds.isNotEmpty() -> {
                ExtensionManager.getInstance(context).getModulesByIds(appExtensionModuleIds)
            }
            allowGlobalModuleFallback -> {
                ExtensionManager.getInstance(context).getEnabledModules()
            }
            else -> emptyList()
        }
        if (baseModules.isEmpty()) return emptyList()

        val dynamicScripts = baseModules
            .asSequence()
            .filter {
                it.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                    it.chromeExtId.isNotEmpty()
            }
            .map { it.chromeExtId }
            .distinct()
            .flatMap { extId ->
                com.webtoapp.core.extension.ChromeExtensionContentScriptRegistry
                    .buildModules(context, extId)
                    .asSequence()
            }
            .toList()

        return if (dynamicScripts.isEmpty()) {
            baseModules
        } else {
            baseModules + dynamicScripts
        }
    }









    private fun injectAllExtensionModules(webView: WebView, url: String, runAt: ScriptRunTime) {

        if (embeddedModules.isNotEmpty()) {
            injectEmbeddedModules(webView, url, runAt)
            return
        }

        val moduleRunAt = runAt.toModuleRunTime()


        val allModules = resolveActiveExtensionModules()
        if (allModules.isEmpty()) {
            AppLogger.d("WebViewManager", "injectAllExtensionModules: No active modules (${runAt.name})")
            return
        }

        AppLogger.d("WebViewManager", "injectAllExtensionModules: runAt=${runAt.name}, url=$url, totalModules=${allModules.size}")



        if (runAt == ScriptRunTime.DOCUMENT_START) {
            injectEarlyCss(webView, allModules, url, moduleRunAt)
        }


        val matching = allModules.filter { it.runAt == moduleRunAt && it.matchesUrl(url) }


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


        if (runAt == ScriptRunTime.DOCUMENT_END) {
            registerAllModulesInPanel(webView, allModules, url)
        }
    }






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

            val userscriptModules = matchingModules.filter { it.isUserscript() }
            val standardModules = matchingModules.filterNot { it.isUserscript() }

            if (userscriptModules.isNotEmpty()) {
                injectEmbeddedUserscriptModules(webView, userscriptModules)
            }

            if (standardModules.isNotEmpty()) {
                val injectionCode = standardModules.joinToString("\n\n") { module ->
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
            }

            AppLogger.d(
                "WebViewManager",
                "Injected ${standardModules.size} embedded module(s) + ${userscriptModules.size} embedded userscript(s) (${runAt.name})"
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Embedded module injection failed", e)
        }
    }







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
                module.runAt != currentRunAt &&
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








    private fun injectChromeExtModules(
        webView: WebView,
        modules: List<com.webtoapp.core.extension.ExtensionModule>,
        runAt: ScriptRunTime
    ) {
        try {
            val extensionGroups = modules.groupBy { it.chromeExtId }

            if (extensionGroups.size == 1) {

                val codeBuilder = StringBuilder()


                if (runAt == ScriptRunTime.DOCUMENT_START) {
                    codeBuilder.appendLine(com.webtoapp.core.extension.ChromeExtensionMobileCompat.generateCompatScript())
                    codeBuilder.appendLine()
                }

                for ((extId, extModules) in extensionGroups) {
                    val manifestJson = extModules.firstOrNull()?.manifestJson ?: "{}"
                    codeBuilder.appendLine(
                        com.webtoapp.core.extension.ChromeExtensionPolyfill.generatePolyfill(
                            extensionId = extId,
                            manifestJson = manifestJson
                        )
                    )
                    codeBuilder.appendLine()


                    appendChromeExtCss(codeBuilder, extId, extModules)


                    appendChromeExtScripts(codeBuilder, extModules)
                }

                val combinedCode = codeBuilder.toString()
                if (combinedCode.isNotBlank()) {
                    webView.evaluateJavascript(combinedCode, null)
                }
                AppLogger.d("WebViewManager", "Injected Chrome extension polyfills for ${modules.size} module(s) (${runAt.name})")
            } else {




                if (runAt == ScriptRunTime.DOCUMENT_START) {
                    webView.evaluateJavascript(
                        com.webtoapp.core.extension.ChromeExtensionMobileCompat.generateCompatScript(), null
                    )
                }
                for ((extId, extModules) in extensionGroups) {
                    val extBuilder = StringBuilder()
                    val manifestJson = extModules.firstOrNull()?.manifestJson ?: "{}"
                    extBuilder.appendLine(
                        com.webtoapp.core.extension.ChromeExtensionPolyfill.generatePolyfill(
                            extensionId = extId,
                            manifestJson = manifestJson
                        )
                    )
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





    private fun appendChromeExtScripts(
        builder: StringBuilder,
        modules: List<com.webtoapp.core.extension.ExtensionModule>
    ) {
        for (module in modules) {
            if (module.code.isBlank()) continue

            if (module.world == "MAIN") {

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







    private fun injectUserscriptModules(
        webView: WebView,
        modules: List<com.webtoapp.core.extension.ExtensionModule>
    ) {
        try {
            ensureUserscriptBootstrap(webView)

            val injectedKeys = userscriptInjectionState.getOrPut(webView) { mutableSetOf() }
            var injectedCount = 0
            var skippedCount = 0

            for (module in modules) {
                val moduleKey = buildUserscriptInjectionKey(module)
                if (!injectedKeys.add(moduleKey)) {
                    skippedCount += 1
                    AppLogger.d(
                        "WebViewManager",
                        "Skip duplicate userscript injection: ${module.name} key=$moduleKey"
                    )
                    continue
                }

                val scriptInfo = mapOf(
                    "name" to module.name,
                    "version" to module.version.name,
                    "description" to module.description,
                    "author" to (module.author?.name ?: ""),
                    "namespace" to module.id
                )

                val resolvedResources = module.resources.mapValues { (name, url) ->
                    extensionFileManager.getCachedResource(name, url) ?: url
                }

                val polyfill = com.webtoapp.core.extension.GreasemonkeyBridge.generatePolyfillScript(
                    scriptId = module.id,
                    grants = module.gmGrants,
                    scriptInfo = scriptInfo,
                    resources = resolvedResources
                )

                val requireBlocks = module.requireUrls.mapNotNull { url ->
                    extensionFileManager.getCachedRequire(url)?.let { requireCode ->
                        url to requireCode
                    }
                }

                val totalRequireChars = requireBlocks.sumOf { it.second.length }
                AppLogger.d(
                    "WebViewManager",
                    "Userscript payload: name=${module.name}, body=${module.code.length}, requires=${requireBlocks.size}, requireChars=$totalRequireChars"
                )

                webView.evaluateJavascript(buildUserscriptPolyfillEval(module.id, polyfill), null)

                requireBlocks.forEachIndexed { index, (_, requireCode) ->
                    webView.evaluateJavascript(
                        buildUserscriptRequireEval(module.name, module.id, index, requireCode),
                        null
                    )
                }

                if (module.code.isNotBlank()) {
                    webView.evaluateJavascript(buildUserscriptModuleEval(module.name, module.id, module.code), null)
                }

                injectedCount += 1
            }

            AppLogger.d(
                "WebViewManager",
                "Injected $injectedCount userscript module(s); skipped $skippedCount duplicate(s)"
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Userscript module injection failed", e)
        }
    }

    private fun ensureUserscriptBootstrap(webView: WebView) {
        val bootstrap = """
            (function() {
                if (window.__WTA_USERSCRIPT_BOOTSTRAPPED__) return;
                window.__WTA_USERSCRIPT_BOOTSTRAPPED__ = true;
                window.__WTA_USERSCRIPT_POLYFILLS__ = window.__WTA_USERSCRIPT_POLYFILLS__ || {};
                window.__WTA_USERSCRIPT_REQUIRE_CACHE__ = window.__WTA_USERSCRIPT_REQUIRE_CACHE__ || {};
                ${com.webtoapp.core.extension.UserScriptWindowScript.getWindowManagerScript()}
            })();
        """.trimIndent()
        webView.evaluateJavascript(bootstrap, null)
    }

    private fun buildUserscriptInjectionKey(
        module: com.webtoapp.core.extension.ExtensionModule
    ): String {
        val version = module.version.name.ifBlank { "0" }
        return "${module.id}:$version:${module.runAt.name}"
    }

    private fun buildEmbeddedUserscriptInjectionKey(
        module: com.webtoapp.core.shell.EmbeddedShellModule
    ): String {
        val version = module.versionName.ifBlank { "0" }
        return "${module.id}:$version:${module.runAt}"
    }

    private fun buildUserscriptPolyfillEval(
        scriptId: String,
        polyfill: String
    ): String {
        val encoded = JSONObject.quote(polyfill)
        return """
            (function() {
                window.__WTA_USERSCRIPT_POLYFILLS__ = window.__WTA_USERSCRIPT_POLYFILLS__ || {};
                if (window.__WTA_USERSCRIPT_POLYFILLS__[${
                    JSONObject.quote(scriptId)
                }]) return;
                window.__WTA_USERSCRIPT_POLYFILLS__[${
                    JSONObject.quote(scriptId)
                }] = true;
                try {
                    eval($encoded);
                } catch(__wtaPolyfillError__) {
                    console.error('[WebToApp Userscript Polyfill Error] ${escapeForJsSingleQuoted(scriptId)}:', __wtaPolyfillError__);
                }
            })();
        """.trimIndent()
    }

    private fun buildUserscriptRequireEval(
        moduleName: String,
        scriptId: String,
        index: Int,
        requireCode: String
    ): String {
        val encodedScriptId = JSONObject.quote(scriptId)
        val requireKey = JSONObject.quote("$scriptId#$index")
        val encodedCode = JSONObject.quote(requireCode)
        return """
            (function() {
                window.__WTA_USERSCRIPT_REQUIRE_CACHE__ = window.__WTA_USERSCRIPT_REQUIRE_CACHE__ || {};
                if (window.__WTA_USERSCRIPT_REQUIRE_CACHE__[$requireKey]) return;
                window.__WTA_USERSCRIPT_REQUIRE_CACHE__[$requireKey] = true;
                try {
                    eval($encodedCode);
                } catch(__wtaRequireError__) {
                    console.error('[WebToApp Userscript Require Error] ${escapeForJsSingleQuoted(moduleName)} (${escapeForJsSingleQuoted(scriptId)}):', __wtaRequireError__);
                    delete window.__WTA_USERSCRIPT_REQUIRE_CACHE__[$requireKey];
                }
            })();
        """.trimIndent()
    }

    private fun buildUserscriptModuleEval(
        moduleName: String,
        scriptId: String,
        moduleCode: String
    ): String {
        val encodedCode = JSONObject.quote(moduleCode)
        return """
            (function() {
                try {
                    eval($encodedCode);
                } catch(__usError__) {
                    console.error('[WebToApp Userscript Error] ${escapeForJsSingleQuoted(moduleName)} (${escapeForJsSingleQuoted(scriptId)}):', __usError__);
                }
            })();
        """.trimIndent()
    }

    private fun escapeForJsSingleQuoted(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
    }

    private fun injectEmbeddedUserscriptModules(
        webView: WebView,
        modules: List<com.webtoapp.core.shell.EmbeddedShellModule>
    ) {
        try {
            ensureUserscriptBootstrap(webView)

            val injectedKeys = userscriptInjectionState.getOrPut(webView) { mutableSetOf() }
            var injectedCount = 0
            var skippedCount = 0

            for (module in modules) {
                val moduleKey = buildEmbeddedUserscriptInjectionKey(module)
                if (!injectedKeys.add(moduleKey)) {
                    skippedCount += 1
                    AppLogger.d(
                        "WebViewManager",
                        "Skip duplicate embedded userscript injection: ${module.name} key=$moduleKey"
                    )
                    continue
                }

                val scriptInfo = mapOf(
                    "name" to module.name,
                    "version" to module.versionName,
                    "description" to module.description,
                    "author" to module.authorName,
                    "namespace" to module.id
                )

                val polyfill = com.webtoapp.core.extension.GreasemonkeyBridge.generatePolyfillScript(
                    scriptId = module.id,
                    grants = module.gmGrants,
                    scriptInfo = scriptInfo,
                    resources = module.resources
                )

                val requireBlocks = module.requireUrls.mapNotNull { url ->
                    module.requireContents[url]?.let { requireCode ->
                        url to requireCode
                    }
                }

                AppLogger.d(
                    "WebViewManager",
                    "Embedded userscript payload: name=${module.name}, body=${module.code.length}, requires=${requireBlocks.size}, declaredRequires=${module.requireUrls.size}"
                )

                webView.evaluateJavascript(buildUserscriptPolyfillEval(module.id, polyfill), null)

                requireBlocks.forEachIndexed { index, (_, requireCode) ->
                    webView.evaluateJavascript(
                        buildUserscriptRequireEval(module.name, module.id, index, requireCode),
                        null
                    )
                }

                if (module.code.isNotBlank()) {
                    webView.evaluateJavascript(
                        buildUserscriptModuleEval(module.name, module.id, module.code),
                        null
                    )
                }

                injectedCount += 1
            }

            AppLogger.d(
                "WebViewManager",
                "Injected $injectedCount embedded userscript module(s); skipped $skippedCount duplicate(s)"
            )
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Embedded userscript module injection failed", e)
        }
    }








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









    private fun registerAllModulesInPanel(
        webView: WebView,
        allModules: List<com.webtoapp.core.extension.ExtensionModule>,
        url: String
    ) {
        try {
            if (allModules.isEmpty()) return


            val chromeModules = allModules.filter { module ->
                module.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.chromeExtId.isNotEmpty()
            }


            val nonChromeModules = allModules.filter { module ->
                module.sourceType != com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                module.shouldRegisterInPanel()
            }

            if (chromeModules.isEmpty() && nonChromeModules.isEmpty()) return

            val registeredExtIds = mutableSetOf<String>()
            val regBuilder = StringBuilder()


            for (module in chromeModules) {
                val extId = module.chromeExtId.ifBlank { module.id }
                if (extId in registeredExtIds) continue
                registeredExtIds.add(extId)


                val extModules = chromeModules.filter {
                    (it.chromeExtId.ifBlank { it.id }) == extId
                }


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
                val jsPopupPath = module.popupPath.replace("\\", "\\\\").replace("'", "\\'")
                val jsOptionsPagePath = module.optionsPagePath.replace("\\", "\\\\").replace("'", "\\'")


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
                                world: '${module.world}',
                                runAt: '${module.runAt.name}',
                                runMode: '${module.runMode.name}',
                                popupPath: '$jsPopupPath',
                                optionsPagePath: '$jsOptionsPagePath',
                                onAction: function(container) {
                                    var html = '';
                                    if ('$jsPopupPath') {
                                        html += '<button style="width:100%;padding:12px;border-radius:10px;border:1px solid rgba(0,0,0,0.08);background:rgba(59,130,246,0.08);color:#2563eb;font-weight:600;cursor:pointer;margin-bottom:10px" onclick="if(window.__WTA_EXT_ACTIONS__)window.__WTA_EXT_ACTIONS__.openPopup(\\'$extId\\', \\'$jsPopupPath\\')">Open popup</button>';
                                    }
                                    if ('$jsOptionsPagePath') {
                                        html += '<button style="width:100%;padding:12px;border-radius:10px;border:1px solid rgba(0,0,0,0.08);background:rgba(16,185,129,0.08);color:#047857;font-weight:600;cursor:pointer" onclick="if(window.__WTA_EXT_ACTIONS__)window.__WTA_EXT_ACTIONS__.openOptions(\\'$extId\\', \\'$jsOptionsPagePath\\')">Open options</button>';
                                    }
                                    container.innerHTML = html;
                                }
                            });
                        }
                        setTimeout(_reg, 50);
                    })();
                """.trimIndent())
            }


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
                                id: '${module.id.replace("\\", "\\\\").replace("'", "\\'")}',
                                name: '$jsName',
                                description: '$jsDesc',
                                version: '$jsVersion',
                                author: '$jsAuthor',
                                icon: '$iconHtml',
                                sourceType: '${module.sourceType.name}',
                                active: $matchesPage,
                                urlMatches: [],
                                permissions: [],
                                world: '${module.world}',
                                runAt: '${module.runAt.name}',
                                runMode: '${module.runMode.name}'
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

    internal fun getNotificationPolyfillScript(): String = NotificationPolyfillHolder.SCRIPT

    private fun loadDeclarativeNetRequestRules(extId: String, manifestJson: String) {
        try {
            com.webtoapp.core.extension.DeclarativeNetRequestEngine.clearExtension(extId)
            com.webtoapp.core.extension.WebRequestBridge.unregisterAll(extId)
            if (manifestJson.isBlank()) return
            val manifest = JSONObject(manifestJson)
            val ruleResources = manifest.optJSONObject("declarative_net_request")
                ?.optJSONArray("rule_resources")
                ?: return
            for (i in 0 until ruleResources.length()) {
                val resource = ruleResources.optJSONObject(i) ?: continue
                val enabled = resource.optBoolean("enabled", true)
                val rulesetId = resource.optString("id", "").trim()
                val path = resource.optString("path", "").trim().trimStart('/')
                if (path.isBlank()) continue
                val url = "chrome-extension://$extId/$path"
                val response = com.webtoapp.core.extension.ExtensionResourceInterceptor.intercept(context, url)
                    ?: continue
                val rulesJson = response.data?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: continue
                com.webtoapp.core.extension.DeclarativeNetRequestEngine.loadStaticRules(
                    extensionId = extId,
                    rulesetId = rulesetId,
                    path = path,
                    rulesJson = rulesJson,
                    enabled = enabled
                )
            }
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Failed to load static DNR rules for $extId", e)
        }
    }

    private fun showChromeExtensionPage(extId: String, pagePath: String) {
        if (pagePath.isBlank()) return
        val activity = context as? Activity ?: return
        val primaryModule = getActiveModulesForCurrentApp().firstOrNull {
            it.sourceType == com.webtoapp.core.extension.ModuleSourceType.CHROME_EXTENSION &&
                it.chromeExtId == extId
        } ?: return

        activity.runOnUiThread {
            extensionPopupDialogs.remove(extId)?.dismiss()

            val popupManager = com.webtoapp.core.extension.ExtensionPopupManager(
                context = activity,
                extensionId = extId,
                popupPath = pagePath,
                runtime = extensionRuntimes[extId],
                targetWebViewProvider = { managedWebViews.keys.firstOrNull() },
                manifestJson = primaryModule.manifestJson.ifBlank { "{}" },
                openPopupHandler = { nextExtId, nextPath -> showChromeExtensionPage(nextExtId, nextPath) },
                openOptionsPageHandler = { nextExtId, nextPath -> showChromeExtensionPage(nextExtId, nextPath) }
            )
            val popupWebView = popupManager.createPopupWebView()
            val dialog = Dialog(activity).apply {
                setContentView(
                    popupWebView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setOnDismissListener {
                    popupManager.destroy()
                    extensionPopupDialogs.remove(extId)
                }
            }
            extensionPopupDialogs[extId] = dialog
            dialog.show()
        }
    }

    internal object NotificationPolyfillHolder {
        val SCRIPT: String = """
            (function() {
                'use strict';

                if (window.__webtoapp_notification_polyfill__) return;
                window.__webtoapp_notification_polyfill__ = true;

                if (typeof NativeBridge === 'undefined') {
                    console.log('[WebToApp] NativeBridge not found, notification polyfill skipped');
                    return;
                }

                function getPermission() {
                    try {
                        return NativeBridge.getNotificationPermissionState() || 'default';
                    } catch (e) {
                        return 'default';
                    }
                }

                function requestPermission(callback) {
                    return new Promise(function(resolve) {
                        var result = 'default';
                        try {
                            result = NativeBridge.requestNotificationPermission() || getPermission();
                        } catch (e) {}
                        if (callback) callback(result);
                        resolve(result);
                    });
                }

                function showNativeNotification(title, options) {
                    options = options || {};
                    var ok = NativeBridge.showWebNotification(
                        String(title || ''),
                        String(options.body || ''),
                        String(options.tag || '')
                    );
                    if (!ok) {
                        throw new DOMException('Notification permission denied', 'NotAllowedError');
                    }
                }

                function WebToAppNotification(title, options) {
                    showNativeNotification(title, options);
                    this.title = String(title || '');
                    this.body = String((options && options.body) || '');
                    this.tag = String((options && options.tag) || '');
                    this.data = options && options.data;
                    this.close = function() {};
                }

                Object.defineProperty(WebToAppNotification, 'permission', {
                    get: getPermission,
                    configurable: true
                });
                WebToAppNotification.requestPermission = requestPermission;
                WebToAppNotification.maxActions = 0;
                window.Notification = WebToAppNotification;

                if (navigator.permissions && navigator.permissions.query) {
                    var originalQuery = navigator.permissions.query.bind(navigator.permissions);
                    navigator.permissions.query = function(desc) {
                        if (desc && desc.name === 'notifications') {
                            var state = getPermission();
                            return Promise.resolve({
                                state: state,
                                onchange: null,
                                addEventListener: function(){},
                                removeEventListener: function(){}
                            });
                        }
                        return originalQuery(desc);
                    };
                }

                if (typeof window.PushManager === 'undefined') {
                    function WebToAppPushManager() {}
                    WebToAppPushManager.supportedContentEncodings = ['aes128gcm'];
                    WebToAppPushManager.prototype.subscribe = function(opts) {
                        return Promise.reject(new DOMException(
                            'Push subscription is not available in this WebView; falling back to local notifications.',
                            'NotAllowedError'
                        ));
                    };
                    WebToAppPushManager.prototype.getSubscription = function() {
                        return Promise.resolve(null);
                    };
                    WebToAppPushManager.prototype.permissionState = function(opts) {
                        return Promise.resolve(getPermission());
                    };
                    window.PushManager = WebToAppPushManager;
                }

                if (typeof window.PushSubscription === 'undefined') {
                    function WebToAppPushSubscription() {}
                    WebToAppPushSubscription.prototype.unsubscribe = function() {
                        return Promise.resolve(true);
                    };
                    WebToAppPushSubscription.prototype.toJSON = function() {
                        return { endpoint: '', expirationTime: null, keys: {} };
                    };
                    window.PushSubscription = WebToAppPushSubscription;
                }

                if (navigator.serviceWorker && navigator.serviceWorker.ready) {
                    navigator.serviceWorker.ready.then(function(reg) {
                        if (!reg.showNotification) {
                            reg.showNotification = function(title, options) {
                                return new Promise(function(resolve, reject) {
                                    try {
                                        showNativeNotification(title, options);
                                        resolve();
                                    } catch (e) {
                                        reject(e);
                                    }
                                });
                            };
                        }
                        if (!reg.pushManager) {
                            try {
                                reg.pushManager = new window.PushManager();
                            } catch (e) {}
                        }
                        if (typeof reg.getNotifications !== 'function') {
                            reg.getNotifications = function() { return Promise.resolve([]); };
                        }
                    }).catch(function(){});
                }

                console.log('[WebToApp] Notification API polyfill loaded');
            })();
        """.trimIndent()
    }

    internal object PrivateNetworkApiBridgeScriptHolder {
        val SCRIPT: String = """
            (function() {
                'use strict';

                function isPrivateHost(host) {
                    if (!host) return false;
                    host = String(host).toLowerCase().replace(/^\[/, '').replace(/\]$/, '');
                    if (host === 'localhost' || host === '127.0.0.1' || host === '10.0.2.2' || host === '::1') return true;
                    if (host.endsWith('.local')) return true;
                    var parts = host.split('.');
                    if (parts.length !== 4) return false;
                    var nums = [];
                    for (var i = 0; i < parts.length; i++) {
                        if (!/^\d+$/.test(parts[i])) return false;
                        var n = Number(parts[i]);
                        if (n < 0 || n > 255) return false;
                        nums.push(n);
                    }
                    return nums[0] === 10 ||
                        (nums[0] === 172 && nums[1] >= 16 && nums[1] <= 31) ||
                        (nums[0] === 192 && nums[1] === 168) ||
                        nums[0] === 127;
                }

                function isLocalPackagedPage() {
                    return isPrivateHost(window.location.hostname);
                }

                if (!isLocalPackagedPage()) return;
                if (window.__wta_private_network_api_bridge__) return;
                window.__wta_private_network_api_bridge__ = true;

                var nativeFetch = window.fetch ? window.fetch.bind(window) : null;
                var NativeXHR = window.XMLHttpRequest;

                function shouldBridge(url) {
                    try {
                        var parsed = new URL(String(url), window.location.href);
                        if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return false;
                        if (!isPrivateHost(parsed.hostname)) return false;
                        if (parsed.hostname.toLowerCase() === window.location.hostname.toLowerCase() && parsed.port === window.location.port) {
                            return false;
                        }
                        return true;
                    } catch (e) {
                        return false;
                    }
                }

                function headersToObject(headers) {
                    var obj = {};
                    try {
                        if (!headers) return obj;
                        if (typeof Headers !== 'undefined' && headers instanceof Headers) {
                            headers.forEach(function(value, key) { obj[key] = String(value); });
                        } else if (Array.isArray(headers)) {
                            headers.forEach(function(pair) {
                                if (pair && pair.length >= 2) obj[String(pair[0])] = String(pair[1]);
                            });
                        } else if (typeof headers === 'object') {
                            Object.keys(headers).forEach(function(key) { obj[key] = String(headers[key]); });
                        }
                    } catch (e) {}
                    return obj;
                }

                function bytesToBase64(bytes) {
                    var binary = '';
                    var chunkSize = 0x8000;
                    for (var i = 0; i < bytes.length; i += chunkSize) {
                        var chunk = bytes.subarray(i, i + chunkSize);
                        binary += String.fromCharCode.apply(null, chunk);
                    }
                    return btoa(binary);
                }

                function textToBase64(text) {
                    return bytesToBase64(new TextEncoder().encode(String(text)));
                }

                function base64ToBytes(base64) {
                    var binary = atob(base64 || '');
                    var bytes = new Uint8Array(binary.length);
                    for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
                    return bytes;
                }

                function bodyToBase64(body) {
                    if (body == null) return Promise.resolve('');
                    if (typeof body === 'string') return Promise.resolve(textToBase64(body));
                    if (body instanceof URLSearchParams) return Promise.resolve(textToBase64(body.toString()));
                    if (body instanceof ArrayBuffer) return Promise.resolve(bytesToBase64(new Uint8Array(body)));
                    if (ArrayBuffer.isView(body)) return Promise.resolve(bytesToBase64(new Uint8Array(body.buffer, body.byteOffset, body.byteLength)));
                    if (typeof Blob !== 'undefined' && body instanceof Blob) {
                        return new Promise(function(resolve, reject) {
                            var reader = new FileReader();
                            reader.onload = function() {
                                var result = String(reader.result || '');
                                resolve(result.indexOf(',') >= 0 ? result.split(',').pop() : '');
                            };
                            reader.onerror = function() { reject(reader.error || new Error('Blob read failed')); };
                            reader.readAsDataURL(body);
                        });
                    }
                    try {
                        return Promise.resolve(textToBase64(JSON.stringify(body)));
                    } catch (e) {
                        return Promise.resolve(textToBase64(String(body)));
                    }
                }

                function normalizeFetchInput(input, init) {
                    var url = '';
                    var method = 'GET';
                    var headers = {};
                    var body = null;
                    if (typeof Request !== 'undefined' && input instanceof Request) {
                        url = input.url;
                        method = input.method || method;
                        headers = headersToObject(input.headers);
                        if (!init && method !== 'GET' && method !== 'HEAD') body = input.clone();
                    } else {
                        url = String(input);
                    }
                    if (init) {
                        if (init.method) method = String(init.method);
                        Object.assign(headers, headersToObject(init.headers));
                        if ('body' in init) body = init.body;
                    }
                    return { url: url, method: method.toUpperCase(), headers: headers, body: body };
                }

                function nativeHttpRequest(payload) {
                    if (!window.NativeBridge || typeof window.NativeBridge.httpRequest !== 'function') {
                        return Promise.reject(new TypeError('Native private network bridge is unavailable'));
                    }
                    return bodyToBase64(payload.body).then(function(bodyBase64) {
                        var raw = window.NativeBridge.httpRequest(JSON.stringify({
                            url: payload.url,
                            method: payload.method || 'GET',
                            headers: payload.headers || {},
                            bodyBase64: bodyBase64
                        }));
                        var result = JSON.parse(raw || '{}');
                        if (!result.ok) {
                            throw new TypeError(result.message || result.error || 'Private network request failed');
                        }
                        return result;
                    });
                }

                if (nativeFetch) {
                    window.fetch = function(input, init) {
                        var payload = normalizeFetchInput(input, init);
                        if (!shouldBridge(payload.url)) return nativeFetch(input, init);
                        return nativeHttpRequest(payload).then(function(result) {
                            var bytes = base64ToBytes(result.bodyBase64 || '');
                            return new Response(bytes, {
                                status: Number(result.status || 0),
                                statusText: String(result.statusText || ''),
                                headers: result.headers || {}
                            });
                        });
                    };
                }

                if (NativeXHR) {
                    function BridgedXHR() {
                        this._xhr = new NativeXHR();
                        this._listeners = {};
                        this._headers = {};
                        this._method = 'GET';
                        this._url = '';
                        this._async = true;
                        this.readyState = 0;
                        this.response = null;
                        this.responseText = '';
                        this.responseType = '';
                        this.responseURL = '';
                        this.status = 0;
                        this.statusText = '';
                        this.timeout = 0;
                        this.withCredentials = false;
                        this.onreadystatechange = null;
                        this.onload = null;
                        this.onerror = null;
                        this.onloadend = null;

                        var self = this;
                        ['readystatechange', 'load', 'error', 'loadend', 'timeout', 'abort', 'progress'].forEach(function(name) {
                            self._xhr.addEventListener(name, function(event) {
                                self._syncFromNative();
                                self._dispatch(name, event);
                            });
                        });
                    }

                    BridgedXHR.prototype._syncFromNative = function() {
                        this.readyState = this._xhr.readyState;
                        this.response = this._xhr.response;
                        this.responseText = this._xhr.responseText || '';
                        this.responseURL = this._xhr.responseURL || this._url;
                        this.status = this._xhr.status || 0;
                        this.statusText = this._xhr.statusText || '';
                    };

                    BridgedXHR.prototype._dispatch = function(type, sourceEvent) {
                        var event = sourceEvent || { type: type, target: this, currentTarget: this };
                        var list = this._listeners[type] || [];
                        for (var i = 0; i < list.length; i++) {
                            try { list[i].call(this, event); } catch (e) { setTimeout(function() { throw e; }); }
                        }
                        var handler = this['on' + type];
                        if (typeof handler === 'function') handler.call(this, event);
                    };

                    BridgedXHR.prototype.open = function(method, url, async, user, password) {
                        this._method = String(method || 'GET').toUpperCase();
                        this._url = new URL(String(url), window.location.href).href;
                        this._async = async !== false;
                        this.readyState = 1;
                        if (!shouldBridge(this._url)) {
                            this._native = true;
                            return this._xhr.open(method, url, async, user, password);
                        }
                        this._native = false;
                        this._dispatch('readystatechange');
                    };

                    BridgedXHR.prototype.setRequestHeader = function(name, value) {
                        if (this._native) return this._xhr.setRequestHeader(name, value);
                        this._headers[String(name)] = String(value);
                    };

                    BridgedXHR.prototype.getResponseHeader = function(name) {
                        if (this._native) return this._xhr.getResponseHeader(name);
                        if (!this._responseHeaders) return null;
                        return this._responseHeaders[String(name).toLowerCase()] || null;
                    };

                    BridgedXHR.prototype.getAllResponseHeaders = function() {
                        if (this._native) return this._xhr.getAllResponseHeaders();
                        var headers = this._responseHeaders || {};
                        return Object.keys(headers).map(function(name) { return name + ': ' + headers[name]; }).join('\r\n');
                    };

                    BridgedXHR.prototype.send = function(body) {
                        if (this._native) return this._xhr.send(body);
                        if (this._async === false) throw new Error('Synchronous private network XHR is not supported');
                        var self = this;
                        nativeHttpRequest({ url: this._url, method: this._method, headers: this._headers, body: body })
                            .then(function(result) {
                                var headers = {};
                                Object.keys(result.headers || {}).forEach(function(name) {
                                    headers[name.toLowerCase()] = String(result.headers[name]);
                                });
                                self._responseHeaders = headers;
                                self.status = Number(result.status || 0);
                                self.statusText = String(result.statusText || '');
                                self.responseURL = String(result.url || self._url);
                                var bytes = base64ToBytes(result.bodyBase64 || '');
                                if (self.responseType === 'arraybuffer') {
                                    self.response = bytes.buffer;
                                    self.responseText = '';
                                } else if (self.responseType === 'blob') {
                                    self.response = new Blob([bytes], { type: headers['content-type'] || '' });
                                    self.responseText = '';
                                } else if (self.responseType === 'json') {
                                    self.responseText = new TextDecoder().decode(bytes);
                                    try { self.response = JSON.parse(self.responseText); } catch (e) { self.response = null; }
                                } else {
                                    self.responseText = new TextDecoder().decode(bytes);
                                    self.response = self.responseText;
                                }
                                self.readyState = 4;
                                self._dispatch('readystatechange');
                                self._dispatch('load');
                                self._dispatch('loadend');
                            }).catch(function(error) {
                                self.status = 0;
                                self.statusText = '';
                                self.readyState = 4;
                                self._dispatch('readystatechange');
                                self._dispatch('error', { type: 'error', error: error, target: self, currentTarget: self });
                                self._dispatch('loadend');
                            });
                    };

                    BridgedXHR.prototype.abort = function() {
                        if (this._native) return this._xhr.abort();
                        this.readyState = 0;
                        this._dispatch('abort');
                        this._dispatch('loadend');
                    };

                    BridgedXHR.prototype.addEventListener = function(type, listener) {
                        if (!this._listeners[type]) this._listeners[type] = [];
                        this._listeners[type].push(listener);
                    };

                    BridgedXHR.prototype.removeEventListener = function(type, listener) {
                        var list = this._listeners[type] || [];
                        this._listeners[type] = list.filter(function(item) { return item !== listener; });
                    };

                    BridgedXHR.prototype.overrideMimeType = function(mimeType) {
                        if (this._native) return this._xhr.overrideMimeType(mimeType);
                    };

                    window.XMLHttpRequest = BridgedXHR;
                }
            })();
        """.trimIndent()
    }

    private fun injectNotificationPolyfill(webView: WebView) {
        try {
            val url = webView.url
            if (shouldMinimizeLocalRuntimeInjection(url)) {
                AppLogger.d("WebViewManager", "Skip notification polyfill for local runtime page: $url")
                return
            }
            val script = getNotificationPolyfillScript()
            webView.evaluateJavascript(script, null)
            AppLogger.d("WebViewManager", "Notification polyfill injected early")
        } catch (e: Exception) {
            AppLogger.e("WebViewManager", "Notification polyfill injection failed", e)
        }
    }
}
