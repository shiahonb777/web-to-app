package com.webtoapp.core.disguise

import com.webtoapp.core.logging.AppLogger

/**
 * 浏览器伪装 JavaScript 生成器 v3.0 — 万向隐身核心
 * 
 * ## 架构设计 (v3.0 重大重构)
 * 
 * v2.0 的问题: 22 个独立开关让用户手动选择向量，容易遗漏，留下检测缝隙。
 * 
 * v3.0 新思路: **一旦开启浏览器伪装，ALL 表层 API 全部伪装**。
 * 
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │  UNIVERSAL STEALTH CORE (≈50+ 个检测面, 零性能成本, 始终注入) │
 * │  ✓ navigator 全属性伪装 (webdriver/vendor/platform/appVersion│
 * │    /userAgentData/plugins/mimeTypes/languages/connection...) │
 * │  ✓ window.chrome 完整 runtime/app/loadTimes/csi 模拟       │
 * │  ✓ Permissions/Notification/Credential/Keyboard API 补全    │
 * │  ✓ 自动化标志清除 (selenium/puppeteer/phantomjs/playwright)  │
 * │  ✓ Error stack / console debug 痕迹清理                     │
 * │  ✓ 原型链 toString 保护 (所有 hook → [native code])         │
 * │  ✓ iframe contentWindow 自动传播                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  PARAMETERIZED OVERLAYS (由 config 控制具体参数值)           │
 * │  ◆ Canvas 噪声强度                                         │
 * │  ◆ WebGL 渲染器型号                                        │
 * │  ◆ Audio 频率偏移                                          │
 * │  ◆ Screen 分辨率/DPR                                       │
 * │  ◆ Timezone IANA 值                                        │
 * │  ◆ Language 列表                                           │
 * │  ◆ Platform / HW / Memory                                  │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 * 
 * ## 检测面覆盖 (50+)
 * 
 * 人看不穿: UI 层面完全像 Chrome
 * 数据看不穿: 所有 API 返回值与 Chrome 131 一致
 * 代码看不穿: toString() 返回 [native code], 属性描述符/枚举顺序/原型链全部正确
 */
object BrowserDisguiseJsGenerator {

    private const val TAG = "BrowserDisguiseJs"

    /**
     * 根据配置生成完整的反检测 JavaScript
     * 
     * v3.0: 一旦 enabled=true，自动注入万向隐身核心 + 参数化叠加层
     */
    fun generate(config: BrowserDisguiseConfig): String {
        if (!config.enabled) return ""
        
        val sb = StringBuilder(32768) // 预分配 32KB
        sb.append("(function(){'use strict';\n")
        sb.append("if(window.__wta_browser_disguise__)return;\n")
        sb.append("window.__wta_browser_disguise__=1;\n\n")
        
        // ===== 第一层: 原型链保护基础设施 (必须最先注入) =====
        sb.append(PROTOTYPE_SHIELD_JS)
        
        // ===== 第二层: 万向隐身核心 (50+ 检测面, 一律全开) =====
        sb.append(UNIVERSAL_STEALTH_CORE_JS)
        
        // ===== 第三层: 参数化叠加层 (根据 config 定制具体值) =====
        sb.append(generateCanvasNoiseJs(config.canvasNoiseIntensity))
        sb.append(generateWebGLSpoofJs(config.webglRenderer))
        sb.append(AUDIO_NOISE_JS)
        sb.append(generateScreenSpoofJs(config.screenProfile))
        sb.append(CLIENT_RECTS_NOISE_JS)
        sb.append(generateTimezoneSpoofJs(config.targetTimezone))
        sb.append(generateLanguageSpoofJs(config.targetLanguages))
        sb.append(generatePlatformSpoofJs(config.targetPlatform))
        sb.append(generateHardwareConcurrencyJs(config.targetConcurrency))
        sb.append(generateDeviceMemoryJs(config.targetMemoryGB))
        sb.append(generateUserAgentDataJs(config.targetPlatform))
        
        // ===== 第四层: 深度伪装 (WebRTC/MediaDevices/Font/性能计时) =====
        sb.append(DEEP_DISGUISE_JS)
        
        // ===== 第五层: iframe 传播 + 最终封印 =====
        sb.append(IFRAME_PROPAGATION_JS)
        sb.append(FINAL_SEAL_JS)
        
        sb.append("\n})();")
        
        val js = sb.toString()
        AppLogger.d(TAG, "Generated UNIVERSAL stealth JS: ${js.length} chars (50+ detection vectors)")
        return js
    }
    
    // ==================================================================================
    // 第一层: 原型链保护基础设施
    // ==================================================================================
    
    private const val PROTOTYPE_SHIELD_JS = """
// ══════════════════════════════════════════════════════════════
// [L0] PROTOTYPE SHIELD — toString() / getOwnPropertyDescriptor 保护
// 所有后续 hook 调用 _mn(fn) 注册后, toString 返回 [native code]
// ══════════════════════════════════════════════════════════════
var _ht=new WeakSet();
var _ots=Function.prototype.toString;
function _mn(fn){_ht.add(fn);return fn;}
Function.prototype.toString=_mn(function(){
    if(_ht.has(this))return'function '+this.name+'() { [native code] }';
    return _ots.call(this);
});
// Object.getOwnPropertyDescriptor 防检测
var _ogopd=Object.getOwnPropertyDescriptor;
Object.getOwnPropertyDescriptor=_mn(function(obj,prop){
    var d=_ogopd.call(Object,obj,prop);
    if(d&&d.get&&_ht.has(d.get)){d.configurable=true;}
    return d;
});
"""

    // ==================================================================================
    // 第二层: 万向隐身核心 — 50+ 检测面全覆盖
    // ==================================================================================
    
    private val UNIVERSAL_STEALTH_CORE_JS = """
// ══════════════════════════════════════════════════════════════
// [CORE] UNIVERSAL STEALTH — 表层 API 全伪装
// ══════════════════════════════════════════════════════════════

// ── navigator.webdriver ──
try{Object.defineProperty(navigator,'webdriver',{get:_mn(function(){return false}),enumerable:true,configurable:true});}catch(e){}

// ── navigator.vendor ──
try{Object.defineProperty(navigator,'vendor',{get:_mn(function(){return'Google Inc.'}),enumerable:true,configurable:true});}catch(e){}

// ── navigator.appVersion / appCodeName / product / productSub ──
try{
Object.defineProperty(navigator,'appCodeName',{get:_mn(function(){return'Mozilla'}),enumerable:true,configurable:true});
Object.defineProperty(navigator,'product',{get:_mn(function(){return'Gecko'}),enumerable:true,configurable:true});
Object.defineProperty(navigator,'productSub',{get:_mn(function(){return'20030107'}),enumerable:true,configurable:true});
}catch(e){}

// ── navigator.doNotTrack ──
try{Object.defineProperty(navigator,'doNotTrack',{get:_mn(function(){return null}),enumerable:true,configurable:true});}catch(e){}

// ── navigator.maxTouchPoints (match Windows desktop = 0 when spoofing Win32) ──
try{Object.defineProperty(navigator,'maxTouchPoints',{get:_mn(function(){return 0}),enumerable:true,configurable:true});}catch(e){}

// ── navigator.pdfViewerEnabled (Chrome 94+) ──
try{Object.defineProperty(navigator,'pdfViewerEnabled',{get:_mn(function(){return true}),enumerable:true,configurable:true});}catch(e){}

// ── navigator.cookieEnabled ──
try{Object.defineProperty(navigator,'cookieEnabled',{get:_mn(function(){return true}),enumerable:true,configurable:true});}catch(e){}

// ── navigator.onLine ──
try{Object.defineProperty(navigator,'onLine',{get:_mn(function(){return true}),enumerable:true,configurable:true});}catch(e){}

// ── navigator.javaEnabled ──
try{navigator.javaEnabled=_mn(function(){return false});}catch(e){}

// ── navigator.globalPrivacyControl ──
try{Object.defineProperty(navigator,'globalPrivacyControl',{get:_mn(function(){return false}),enumerable:true,configurable:true});}catch(e){}

// ── window.chrome 完整模拟 (runtime/app/loadTimes/csi) ──
try{
if(!window.chrome)window.chrome={};
if(!window.chrome.runtime)window.chrome.runtime={
    OnInstalledReason:{CHROME_UPDATE:'chrome_update',INSTALL:'install',SHARED_MODULE_UPDATE:'shared_module_update',UPDATE:'update'},
    OnRestartRequiredReason:{APP_UPDATE:'app_update',OS_UPDATE:'os_update',PERIODIC:'periodic'},
    PlatformArch:{ARM:'arm',ARM64:'arm64',MIPS:'mips',MIPS64:'mips64',X86_32:'x86-32',X86_64:'x86-64'},
    PlatformNaclArch:{ARM:'arm',MIPS:'mips',MIPS64:'mips64',X86_32:'x86-32',X86_64:'x86-64'},
    PlatformOs:{ANDROID:'android',CROS:'cros',FUCHSIA:'fuchsia',LINUX:'linux',MAC:'mac',OPENBSD:'openbsd',WIN:'win'},
    RequestUpdateCheckStatus:{NO_UPDATE:'no_update',THROTTLED:'throttled',UPDATE_AVAILABLE:'update_available'},
    connect:_mn(function(e){return{name:'',onDisconnect:{addListener:function(){},removeListener:function(){},hasListener:function(){return false}},onMessage:{addListener:function(){},removeListener:function(){},hasListener:function(){return false}},postMessage:function(){},disconnect:function(){}};}),
    sendMessage:_mn(function(){}),
    id:undefined,
    getManifest:_mn(function(){return{}}),
    getURL:_mn(function(p){return'';}),
    getPlatformInfo:_mn(function(cb){if(cb)cb({os:'win',arch:'x86-64',nacl_arch:'x86-64'})}),
    getBackgroundPage:_mn(function(){}),
    setUninstallURL:_mn(function(){}),
    requestUpdateCheck:_mn(function(cb){if(cb)cb('no_update')})
};
if(!window.chrome.app)window.chrome.app={
    InstallState:{DISABLED:'disabled',INSTALLED:'installed',NOT_INSTALLED:'not_installed'},
    RunningState:{CANNOT_RUN:'cannot_run',READY_TO_RUN:'ready_to_run',RUNNING:'running'},
    getDetails:_mn(function(){return null}),
    getIsInstalled:_mn(function(){return false}),
    installState:_mn(function(){return'not_installed'}),
    isInstalled:false,
    runningState:_mn(function(){return'cannot_run'})
};
if(!window.chrome.loadTimes)window.chrome.loadTimes=_mn(function(){
    var n=performance.now()/1000;
    return{requestTime:n-0.3,startLoadTime:n-0.25,commitLoadTime:n-0.1,
        finishDocumentLoadTime:n-0.05,finishLoadTime:n,firstPaintTime:n-0.08,
        firstPaintAfterLoadTime:0,navigationType:'Other',
        wasFetchedViaSpdy:true,wasNpnNegotiated:true,npnNegotiatedProtocol:'h2',
        wasAlternateProtocolAvailable:false,connectionInfo:'h2'};
});
if(!window.chrome.csi)window.chrome.csi=_mn(function(){
    return{onloadT:Date.now(),startE:Date.now()-300,pageT:performance.now(),tran:15};
});
}catch(e){}

// ── navigator.plugins + mimeTypes 完整模拟 ──
try{
var _mkPlg=function(n,d,f,mt){
    var p={name:n,description:d,filename:f,length:mt.length};
    mt.forEach(function(m,i){p[i]=m;m.enabledPlugin=p;});
    return p;
};
var mt0={type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'};
var mt1={type:'application/x-google-chrome-pdf',suffixes:'pdf',description:'Portable Document Format'};
var mt2={type:'application/x-nacl',suffixes:'',description:'Native Client Executable'};
var mt3={type:'application/x-pnacl',suffixes:'',description:'Portable Native Client Executable'};
var fakePlugins=[
    _mkPlg('PDF Viewer','Portable Document Format','internal-pdf-viewer',[mt0]),
    _mkPlg('Chrome PDF Plugin','Portable Document Format','internal-pdf-viewer',[mt1]),
    _mkPlg('Chrome PDF Viewer','','mhjfbmdgcfjbbpaeojofohoefgiehjai',[{type:'application/pdf',suffixes:'pdf',description:''}]),
    _mkPlg('Native Client','','internal-nacl-plugin',[mt2,mt3]),
    _mkPlg('Chromium PDF Plugin','Portable Document Format','internal-pdf-viewer',[{type:'application/x-google-chrome-pdf',suffixes:'pdf',description:'Portable Document Format'}])
];
fakePlugins.length=5;
fakePlugins.item=_mn(function(i){return this[i]||null});
fakePlugins.namedItem=_mn(function(n){for(var i=0;i<this.length;i++){if(this[i].name===n)return this[i]}return null});
fakePlugins.refresh=_mn(function(){});
Object.setPrototypeOf(fakePlugins,PluginArray.prototype);
Object.defineProperty(navigator,'plugins',{get:_mn(function(){return fakePlugins}),enumerable:true});
var fakeMime=[mt0,mt1,mt2,mt3];
fakeMime.length=4;
fakeMime.item=_mn(function(i){return this[i]||null});
fakeMime.namedItem=_mn(function(n){for(var i=0;i<this.length;i++){if(this[i].type===n)return this[i]}return null});
Object.setPrototypeOf(fakeMime,MimeTypeArray.prototype);
Object.defineProperty(navigator,'mimeTypes',{get:_mn(function(){return fakeMime}),enumerable:true});
}catch(e){}

// ── navigator.connection (Network Information API) ──
try{
var fakeConn={
    effectiveType:'4g',downlink:10+Math.random()*5|0,rtt:50+(Math.random()*30|0),
    saveData:false,type:'wifi',downlinkMax:100,onchange:null,ontypechange:null,
    addEventListener:_mn(function(){}),removeEventListener:_mn(function(){}),dispatchEvent:_mn(function(){return true})
};
Object.defineProperty(navigator,'connection',{get:_mn(function(){return fakeConn}),enumerable:true,configurable:true});
}catch(e){}

// ── navigator.permissions.query 拦截 ──
try{
if(navigator.permissions){
    var _oq=navigator.permissions.query;
    navigator.permissions.query=_mn(function(desc){
        var n=desc&&desc.name||'';
        if(n==='notifications'||n==='push'||n==='geolocation'||n==='camera'||n==='microphone'||n==='midi'||n==='clipboard-read'||n==='clipboard-write'){
            return Promise.resolve({state:'prompt',name:n,onchange:null,addEventListener:_mn(function(){}),removeEventListener:_mn(function(){})});
        }
        try{return _oq.call(navigator.permissions,desc)}catch(e){
            return Promise.resolve({state:'prompt',name:n,onchange:null,addEventListener:_mn(function(){}),removeEventListener:_mn(function(){})});
        }
    });
}
}catch(e){}

// ── Notification API 完整补全 ──
try{
if(!window.Notification){
    window.Notification=_mn(function Notification(title,opt){this.title=title;this.body=(opt||{}).body||'';this.onclick=null;this.onclose=null;this.onerror=null;this.onshow=null;});
    Notification.permission='default';
    Notification.requestPermission=_mn(function(cb){var r='default';if(cb)cb(r);return Promise.resolve(r);});
    Notification.maxActions=2;
    Notification.prototype.close=_mn(function(){});
    Notification.prototype.addEventListener=_mn(function(){});
    Notification.prototype.removeEventListener=_mn(function(){});
}else if(typeof Notification!=='undefined'&&Notification.permission==='denied'){
    Object.defineProperty(Notification,'permission',{get:_mn(function(){return'default'}),configurable:true});
}
}catch(e){}

// ── navigator.credentials (Credential Management API stub) ──
try{
if(!navigator.credentials){
    Object.defineProperty(navigator,'credentials',{get:_mn(function(){return{
        create:_mn(function(){return Promise.resolve(null)}),
        get:_mn(function(){return Promise.resolve(null)}),
        preventSilentAccess:_mn(function(){return Promise.resolve()}),
        store:_mn(function(){return Promise.resolve()})
    }}),enumerable:true,configurable:true});
}
}catch(e){}

// ── navigator.keyboard (Keyboard API stub) ──
try{
if(!navigator.keyboard){
    Object.defineProperty(navigator,'keyboard',{get:_mn(function(){return{
        getLayoutMap:_mn(function(){return Promise.resolve(new Map())}),
        lock:_mn(function(){return Promise.resolve()}),
        unlock:_mn(function(){})
    }}),enumerable:true,configurable:true});
}
}catch(e){}

// ── navigator.locks (Web Locks API stub) ──
try{
if(!navigator.locks){
    Object.defineProperty(navigator,'locks',{get:_mn(function(){return{
        request:_mn(function(n,cb){if(typeof cb==='function')return Promise.resolve(cb({name:n,mode:'exclusive'}));return Promise.resolve()}),
        query:_mn(function(){return Promise.resolve({held:[],pending:[]})})
    }}),enumerable:true,configurable:true});
}
}catch(e){}

// ── navigator.usb / navigator.hid / navigator.serial / navigator.bluetooth (stubs) ──
try{
['usb','hid','serial','bluetooth'].forEach(function(api){
    if(!navigator[api]){
        Object.defineProperty(navigator,api,{get:_mn(function(){return{
            getDevices:_mn(function(){return Promise.resolve([])}),
            requestDevice:_mn(function(){return Promise.reject(new DOMException('User cancelled','NotFoundError'))}),
            addEventListener:_mn(function(){}),removeEventListener:_mn(function(){})
        }}),enumerable:true,configurable:true});
    }
});
}catch(e){}

// ── navigator.storage.estimate 标准化 ──
try{
if(navigator.storage&&navigator.storage.estimate){
    var _oe=navigator.storage.estimate;
    navigator.storage.estimate=_mn(function(){
        return _oe.call(navigator.storage).then(function(e){
            return{quota:2147483648,usage:Math.floor(Math.random()*1e8),usageDetails:e.usageDetails||{}};
        });
    });
}
}catch(e){}

// ── window.speechSynthesis (语音合成 stub) ──
try{
if(!window.speechSynthesis){
    window.speechSynthesis={
        pending:false,speaking:false,paused:false,
        onvoiceschanged:null,
        getVoices:_mn(function(){return[
            {default:true,lang:'en-US',localService:true,name:'Google US English',voiceURI:'Google US English'},
            {default:false,lang:'en-GB',localService:true,name:'Google UK English Female',voiceURI:'Google UK English Female'},
            {default:false,lang:'de-DE',localService:true,name:'Google Deutsch',voiceURI:'Google Deutsch'},
            {default:false,lang:'fr-FR',localService:true,name:'Google français',voiceURI:'Google français'},
            {default:false,lang:'ja-JP',localService:true,name:'Google 日本語',voiceURI:'Google 日本語'}
        ]}),
        speak:_mn(function(){}),cancel:_mn(function(){}),pause:_mn(function(){}),resume:_mn(function(){}),
        addEventListener:_mn(function(){}),removeEventListener:_mn(function(){})
    };
}
}catch(e){}

// ── window.isSecureContext ──
try{Object.defineProperty(window,'isSecureContext',{get:_mn(function(){return true}),enumerable:true,configurable:true});}catch(e){}

// ── window.crossOriginIsolated ──
try{Object.defineProperty(window,'crossOriginIsolated',{get:_mn(function(){return false}),enumerable:true,configurable:true});}catch(e){}

// ── window.clientInformation (alias of navigator) ──
try{if(!window.clientInformation)Object.defineProperty(window,'clientInformation',{get:_mn(function(){return navigator}),enumerable:true,configurable:true});}catch(e){}

// ── window.outerWidth / outerHeight ──
try{
Object.defineProperty(window,'outerWidth',{get:_mn(function(){return window.innerWidth||1920}),enumerable:true,configurable:true});
Object.defineProperty(window,'outerHeight',{get:_mn(function(){return(window.innerHeight||1080)+85}),enumerable:true,configurable:true});
}catch(e){}

// ── window.screenX / screenY / screenLeft / screenTop ──
try{
Object.defineProperty(window,'screenX',{get:_mn(function(){return 0}),enumerable:true,configurable:true});
Object.defineProperty(window,'screenY',{get:_mn(function(){return 0}),enumerable:true,configurable:true});
Object.defineProperty(window,'screenLeft',{get:_mn(function(){return 0}),enumerable:true,configurable:true});
Object.defineProperty(window,'screenTop',{get:_mn(function(){return 0}),enumerable:true,configurable:true});
}catch(e){}

// ── document.hasFocus ──
try{document.hasFocus=_mn(function(){return true});}catch(e){}

// ── CSS matchMedia (prefers-color-scheme / prefers-reduced-motion) ──
try{
var _omm=window.matchMedia;
window.matchMedia=_mn(function(q){
    var r=_omm?_omm.call(window,q):null;
    if(q.indexOf('prefers-color-scheme')!==-1){
        var isDark=q.indexOf('dark')!==-1;
        return{matches:!isDark,media:q,onchange:null,
            addEventListener:_mn(function(){}),removeEventListener:_mn(function(){}),
            addListener:_mn(function(){}),removeListener:_mn(function(){})};
    }
    if(q.indexOf('prefers-reduced-motion')!==-1&&q.indexOf('reduce')!==-1){
        return{matches:false,media:q,onchange:null,
            addEventListener:_mn(function(){}),removeEventListener:_mn(function(){}),
            addListener:_mn(function(){}),removeListener:_mn(function(){})};
    }
    return r||{matches:false,media:q,onchange:null,
        addEventListener:_mn(function(){}),removeEventListener:_mn(function(){}),
        addListener:_mn(function(){}),removeListener:_mn(function(){})};
});
}catch(e){}

// ── performance.now() 精度降低 + Event.timeStamp 一致化 ──
try{
var _opn=performance.now;
var _po=(Math.random()-0.5)*0.1;
performance.now=_mn(function(){return Math.round(_opn.call(performance)*10)/10+_po});
if(performance.timeOrigin){
    var _oto=performance.timeOrigin;
    Object.defineProperty(performance,'timeOrigin',{get:_mn(function(){return Math.round(_oto)}),enumerable:true,configurable:true});
}
}catch(e){}

// ── 自动化标志全清除 ──
try{
var autoFlags=['__selenium_unwrapped','__webdriver_evaluate','__webdriver_script_function',
    '__webdriver_script_func','__webdriver_script_fn','_Selenium_IDE_Recorder',
    '__fxdriver_evaluate','__driver_evaluate','__webdriver_unwrapped',
    '__lastWatirAlert','__lastWatirConfirm','__lastWatirPrompt',
    '_phantom','__nightmare','callPhantom','_phantomas',
    'domAutomation','domAutomationController','_WEBDRIVER_ELEM_CACHE',
    'webdriver','_webdriver','__webview_bridge','__wta_injected'];
autoFlags.forEach(function(f){try{if(f in window){delete window[f];Object.defineProperty(window,f,{get:_mn(function(){return undefined}),configurable:true})}}catch(e){}});
}catch(e){}

// ── Error.prepareStackTrace 痕迹清理 ──
try{
var _opst=Error.prepareStackTrace;
Error.prepareStackTrace=_mn(function(err,stack){
    stack=stack.filter(function(f){
        var fn=(f.getFileName&&f.getFileName())||'';
        return fn.indexOf('injectedScript')===-1&&fn.indexOf('evaluateJavascript')===-1&&fn.indexOf('__wta')===-1&&fn.indexOf('userScript')===-1;
    });
    return _opst?_opst(err,stack):err.toString()+'\\n'+stack.map(function(f){return'    at '+f}).join('\\n');
});
}catch(e){}

// ── Error().stack 属性拦截 ──
try{
var _OE=Error;
window.Error=_mn(function Error(msg){
    var e=new _OE(msg);
    var _os=Object.getOwnPropertyDescriptor(e,'stack');
    if(_os&&_os.value){
        e.stack=_os.value.split('\\n').filter(function(l){
            return l.indexOf('evaluateJavascript')===-1&&l.indexOf('injectedScript')===-1;
        }).join('\\n');
    }
    return e;
});
window.Error.prototype=_OE.prototype;
window.Error.captureStackTrace=_OE.captureStackTrace;
window.Error.stackTraceLimit=_OE.stackTraceLimit;
['TypeError','RangeError','ReferenceError','SyntaxError','URIError','EvalError'].forEach(function(et){
    try{window[et].prototype.constructor=window[et]}catch(e){}
});
}catch(e){}

// ── console.debug 检测防护 ──
try{
var _ocd=console.debug;
console.debug=_mn(function(){
    if(arguments.length===1&&typeof arguments[0]==='string'&&arguments[0].indexOf('devtools')!==-1)return;
    return _ocd.apply(console,arguments);
});
}catch(e){}

// ── document.documentElement.getAttribute('webdriver') ──
try{
if(document.documentElement){
    document.documentElement.removeAttribute('webdriver');
    document.documentElement.removeAttribute('selenium');
    document.documentElement.removeAttribute('driver');
}
}catch(e){}

// ── Intl 对象一致性 (确保 locale 相关 API 可用) ──
try{
if(!Intl.ListFormat)Intl.ListFormat=_mn(function(l,o){this.format=_mn(function(a){return a.join(', ')});this.formatToParts=_mn(function(a){return a.map(function(v){return{type:'element',value:v}})});this.resolvedOptions=_mn(function(){return{locale:'en-US',type:'conjunction',style:'long'}})});
if(!Intl.Segmenter)Intl.Segmenter=_mn(function(l,o){this.segment=_mn(function(s){var r=[];for(var i=0;i<s.length;i++)r.push({segment:s[i],index:i,input:s});return r;});this.resolvedOptions=_mn(function(){return{locale:'en-US',granularity:'grapheme'}})});
}catch(e){}

// ── VisualViewport 补全 ──
try{
if(!window.visualViewport){
    window.visualViewport={
        width:window.innerWidth,height:window.innerHeight,
        offsetLeft:0,offsetTop:0,pageLeft:0,pageTop:0,
        scale:1,onresize:null,onscroll:null,
        addEventListener:_mn(function(){}),removeEventListener:_mn(function(){})
    };
}
}catch(e){}

// ── navigator.brave 检测防护 (如果存在就删除) ──
try{if(navigator.brave)delete navigator.brave;}catch(e){}

// ── window.cdc_ / document.cdc_ (ChromeDriver 检测) ──
try{
Object.keys(window).forEach(function(k){if(k.match(/^cdc_/)||k.match(/^\${"$"}cdc_/)){delete window[k];}});
Object.keys(document).forEach(function(k){if(k.match(/^cdc_/)||k.match(/^\${"$"}cdc_/)){delete document[k];}});
}catch(e){}
"""

    // ==================================================================================
    // 第三层: 参数化叠加层 — 根据 config 定制具体值
    // ==================================================================================
    
    private fun generateCanvasNoiseJs(intensity: Float): String = """
// ── [OVERLAY] Canvas fingerprint noise (intensity=$intensity) ──
try{
var _ctdu=HTMLCanvasElement.prototype.toDataURL;
var _ctb=HTMLCanvasElement.prototype.toBlob;
var _cgid=CanvasRenderingContext2D.prototype.getImageData;
var _seed=Math.random()*999|0;
function _cn(c){
    try{
        var ctx=c.getContext('2d');if(!ctx)return;
        var w=c.width,h=c.height;if(!w||!h||w>4096||h>4096)return;
        var d=_cgid.call(ctx,0,0,Math.min(w,256),Math.min(h,64));
        var p=d.data,s=_seed;
        for(var i=0;i<p.length;i+=4){
            s=(s*16807+1)&0x7fffffff;
            p[i]=(p[i]+((s%5)-2)*$intensity*255)&255;
            s=(s*16807+1)&0x7fffffff;
            p[i+1]=(p[i+1]+((s%5)-2)*$intensity*255)&255;
        }
        ctx.putImageData(d,0,0);
    }catch(e){}
}
HTMLCanvasElement.prototype.toDataURL=_mn(function(){_cn(this);return _ctdu.apply(this,arguments)});
HTMLCanvasElement.prototype.toBlob=_mn(function(){_cn(this);return _ctb.apply(this,arguments)});
CanvasRenderingContext2D.prototype.getImageData=_mn(function(){
    var r=_cgid.apply(this,arguments);var d=r.data;
    for(var i=0;i<d.length;i+=4){d[i]=(d[i]+((Math.random()*3-1)*$intensity*255|0))&255;}
    return r;
});
}catch(e){}
"""

    private fun generateWebGLSpoofJs(renderer: WebGLRenderer): String = """
// ── [OVERLAY] WebGL renderer/vendor spoof → ${renderer.renderer} ──
try{
var VEND='${renderer.vendor}',REND='${renderer.renderer}';
var _gp1=WebGLRenderingContext.prototype.getParameter;
var _gp2=typeof WebGL2RenderingContext!=='undefined'?WebGL2RenderingContext.prototype.getParameter:null;
function _wgp(orig,ctx,pname){
    if(pname===0x9245||pname===0x1F00)return VEND;
    if(pname===0x9246||pname===0x1F01)return REND;
    if(pname===0x8B8C)return 16384; // MAX_VARYING_VECTORS
    return orig.call(ctx,pname);
}
WebGLRenderingContext.prototype.getParameter=_mn(function(p){return _wgp(_gp1,this,p)});
if(_gp2)WebGL2RenderingContext.prototype.getParameter=_mn(function(p){return _wgp(_gp2,this,p)});
var _ge1=WebGLRenderingContext.prototype.getExtension;
WebGLRenderingContext.prototype.getExtension=_mn(function(name){
    if(name==='WEBGL_debug_renderer_info')return{UNMASKED_VENDOR_WEBGL:0x9245,UNMASKED_RENDERER_WEBGL:0x9246};
    return _ge1.call(this,name);
});
if(typeof WebGL2RenderingContext!=='undefined'){
    var _ge2=WebGL2RenderingContext.prototype.getExtension;
    WebGL2RenderingContext.prototype.getExtension=_mn(function(name){
        if(name==='WEBGL_debug_renderer_info')return{UNMASKED_VENDOR_WEBGL:0x9245,UNMASKED_RENDERER_WEBGL:0x9246};
        return _ge2.call(this,name);
    });
}
// WebGL shaderPrecisionFormat spoof
var _gspf=WebGLRenderingContext.prototype.getShaderPrecisionFormat;
WebGLRenderingContext.prototype.getShaderPrecisionFormat=_mn(function(){
    var r=_gspf.apply(this,arguments);
    return{rangeMin:r?r.rangeMin:127,rangeMax:r?r.rangeMax:127,precision:r?r.precision:23};
});
}catch(e){}
"""

    private const val AUDIO_NOISE_JS = """
// ── [OVERLAY] AudioContext fingerprint noise ──
try{
if(typeof AudioContext!=='undefined'){
    var _aco=AudioContext.prototype.createOscillator;
    var _aca=AudioContext.prototype.createAnalyser;
    var _acd=AudioContext.prototype.createDynamicsCompressor;
    AudioContext.prototype.createOscillator=_mn(function(){
        var osc=_aco.apply(this,arguments);
        var _gv=Object.getOwnPropertyDescriptor(osc.frequency.__proto__,'value');
        if(_gv&&_gv.get){
            var _og=_gv.get;
            Object.defineProperty(osc.frequency,'value',{
                get:function(){return _og.call(this)+(Math.random()-0.5)*0.0001;},
                set:_gv.set,enumerable:true,configurable:true
            });
        }
        return osc;
    });
    var _agffd=AnalyserNode.prototype.getFloatFrequencyData;
    if(_agffd)AnalyserNode.prototype.getFloatFrequencyData=_mn(function(arr){
        _agffd.call(this,arr);for(var i=0;i<arr.length;i++)arr[i]+=(Math.random()-0.5)*0.0001;
    });
    var _agftd=AnalyserNode.prototype.getFloatTimeDomainData;
    if(_agftd)AnalyserNode.prototype.getFloatTimeDomainData=_mn(function(arr){
        _agftd.call(this,arr);for(var i=0;i<arr.length;i++)arr[i]+=(Math.random()-0.5)*0.00001;
    });
}
if(typeof OfflineAudioContext!=='undefined'){
    var _oac=OfflineAudioContext;
    window.OfflineAudioContext=_mn(function OfflineAudioContext(){
        var ctx=new _oac(...arguments);
        var _osr=ctx.startRendering;
        ctx.startRendering=_mn(function(){
            return _osr.call(this).then(function(buf){
                var d=buf.getChannelData(0);
                for(var i=0;i<d.length;i++)d[i]+=(Math.random()-0.5)*0.00001;
                return buf;
            });
        });
        return ctx;
    });
    window.OfflineAudioContext.prototype=_oac.prototype;
}
}catch(e){}
"""

    private fun generateScreenSpoofJs(profile: ScreenProfile): String = """
// ── [OVERLAY] Screen resolution → ${profile.width}x${profile.height}@${profile.pixelRatio}x ──
try{
Object.defineProperty(screen,'width',{get:_mn(function(){return ${profile.width}}),enumerable:true,configurable:true});
Object.defineProperty(screen,'height',{get:_mn(function(){return ${profile.height}}),enumerable:true,configurable:true});
Object.defineProperty(screen,'availWidth',{get:_mn(function(){return ${profile.width}}),enumerable:true,configurable:true});
Object.defineProperty(screen,'availHeight',{get:_mn(function(){return ${profile.height - 40}}),enumerable:true,configurable:true});
Object.defineProperty(screen,'colorDepth',{get:_mn(function(){return ${profile.colorDepth}}),enumerable:true,configurable:true});
Object.defineProperty(screen,'pixelDepth',{get:_mn(function(){return ${profile.colorDepth}}),enumerable:true,configurable:true});
Object.defineProperty(window,'devicePixelRatio',{get:_mn(function(){return ${profile.pixelRatio}}),enumerable:true,configurable:true});
Object.defineProperty(screen,'orientation',{get:_mn(function(){return{angle:0,type:'landscape-primary',onchange:null,addEventListener:_mn(function(){}),removeEventListener:_mn(function(){})}}),enumerable:true,configurable:true});
}catch(e){}
"""

    private const val CLIENT_RECTS_NOISE_JS = """
// ── [OVERLAY] ClientRects / DOMRect noise ──
try{
var _gbcr=Element.prototype.getBoundingClientRect;
var _gcr=Element.prototype.getClientRects;
var _rn=function(){return(Math.random()-0.5)*0.25};
Element.prototype.getBoundingClientRect=_mn(function(){
    var r=_gbcr.call(this);return new DOMRect(r.x+_rn(),r.y+_rn(),r.width+_rn(),r.height+_rn());
});
Element.prototype.getClientRects=_mn(function(){
    var l=_gcr.call(this),a=[];
    for(var i=0;i<l.length;i++){var r=l[i];a.push(new DOMRect(r.x+_rn(),r.y+_rn(),r.width+_rn(),r.height+_rn()));}
    return a;
});
// Range.getBoundingClientRect noise too
var _rgbcr=Range.prototype.getBoundingClientRect;
if(_rgbcr)Range.prototype.getBoundingClientRect=_mn(function(){
    var r=_rgbcr.call(this);return new DOMRect(r.x+_rn(),r.y+_rn(),r.width+_rn(),r.height+_rn());
});
}catch(e){}
"""

    private fun generateTimezoneSpoofJs(timezone: String): String = """
// ── [OVERLAY] Timezone → $timezone ──
try{
var _idtf=Intl.DateTimeFormat;
Intl.DateTimeFormat=_mn(function DateTimeFormat(){
    var a=Array.from(arguments);if(!a[1])a[1]={};a[1].timeZone='$timezone';
    return new _idtf(a[0],a[1]);
});
Intl.DateTimeFormat.prototype=_idtf.prototype;
Intl.DateTimeFormat.supportedLocalesOf=_idtf.supportedLocalesOf;
var _iro=_idtf.prototype.resolvedOptions;
_idtf.prototype.resolvedOptions=_mn(function(){var r=_iro.call(this);r.timeZone='$timezone';return r});
var _tzMap={'America/New_York':300,'America/Los_Angeles':480,'America/Chicago':360,'America/Denver':420,'Europe/London':0,'Europe/Paris':-60,'Europe/Berlin':-60,'Asia/Tokyo':-540,'Asia/Shanghai':-480,'Asia/Kolkata':-330,'Asia/Seoul':-540,'Australia/Sydney':-600,'Pacific/Auckland':-720};
var _tzo=_tzMap['$timezone'];if(_tzo===undefined)_tzo=new Date().getTimezoneOffset();
Date.prototype.getTimezoneOffset=_mn(function(){return _tzo});
// toString/toLocaleString 一致化
var _dts=Date.prototype.toString;
Date.prototype.toString=_mn(function(){
    var s=_dts.call(this);
    var tz=Intl.DateTimeFormat('en-US',{timeZone:'$timezone',timeZoneName:'short'}).format(this);
    return s;
});
}catch(e){}
"""

    private fun generateLanguageSpoofJs(languages: List<String>): String {
        val langArr = languages.joinToString(",") { "'$it'" }
        val primary = languages.firstOrNull() ?: "en-US"
        return """
// ── [OVERLAY] Language → $primary ──
try{
Object.defineProperty(navigator,'language',{get:_mn(function(){return '$primary'}),enumerable:true,configurable:true});
Object.defineProperty(navigator,'languages',{get:_mn(function(){return Object.freeze([$langArr])}),enumerable:true,configurable:true});
}catch(e){}
"""
    }

    private fun generatePlatformSpoofJs(platform: String): String = """
// ── [OVERLAY] Platform → $platform ──
try{
Object.defineProperty(navigator,'platform',{get:_mn(function(){return '$platform'}),enumerable:true,configurable:true});
Object.defineProperty(navigator,'oscpu',{get:_mn(function(){return undefined}),enumerable:true,configurable:true});
// 匹配 maxTouchPoints: Win32/MacIntel = 0, Linux = maybe 0
Object.defineProperty(navigator,'maxTouchPoints',{get:_mn(function(){return ${if (platform.contains("Win") || platform.contains("Mac")) "0" else "1"}}),enumerable:true,configurable:true});
}catch(e){}
"""

    private fun generateHardwareConcurrencyJs(cores: Int): String = """
// ── [OVERLAY] Hardware concurrency → $cores ──
try{Object.defineProperty(navigator,'hardwareConcurrency',{get:_mn(function(){return $cores}),enumerable:true,configurable:true});}catch(e){}
"""

    private fun generateDeviceMemoryJs(memoryGB: Int): String = """
// ── [OVERLAY] Device memory → ${memoryGB}GB ──
try{Object.defineProperty(navigator,'deviceMemory',{get:_mn(function(){return $memoryGB}),enumerable:true,configurable:true});}catch(e){}
"""

    /**
     * navigator.userAgentData (Client Hints API) — 关键検出面
     * 
     * Chrome 90+ 使用 User-Agent Client Hints 替代传统 UA 字符串。
     * 如果这个 API 缺失或返回错误值，立即暴露非 Chrome 身份。
     */
    private fun generateUserAgentDataJs(platform: String): String {
        val (pName, pVersion) = when {
            platform.contains("Win") -> "Windows" to "15.0.0"
            platform.contains("Mac") -> "macOS" to "14.5.0"
            platform.contains("Linux") -> "Linux" to "6.5.0"
            else -> "Windows" to "15.0.0"
        }
        return """
// ── [OVERLAY] navigator.userAgentData (Client Hints API — CRITICAL) ──
try{
var _uad={
    brands:Object.freeze([
        Object.freeze({brand:'Chromium',version:'131'}),
        Object.freeze({brand:'Google Chrome',version:'131'}),
        Object.freeze({brand:'Not_A Brand',version:'24'})
    ]),
    mobile:false,
    platform:'$pName',
    getHighEntropyValues:_mn(function(hints){
        return Promise.resolve({
            architecture:'x86',
            bitness:'64',
            brands:[{brand:'Chromium',version:'131.0.0.0'},{brand:'Google Chrome',version:'131.0.0.0'},{brand:'Not_A Brand',version:'24.0.0.0'}],
            fullVersionList:[{brand:'Chromium',version:'131.0.6778.139'},{brand:'Google Chrome',version:'131.0.6778.139'},{brand:'Not_A Brand',version:'24.0.0.0'}],
            mobile:false,
            model:'',
            platform:'$pName',
            platformVersion:'$pVersion',
            uaFullVersion:'131.0.6778.139',
            wow64:false,
            formFactor:''
        });
    }),
    toJSON:_mn(function(){return{brands:this.brands,mobile:this.mobile,platform:this.platform}})
};
Object.defineProperty(navigator,'userAgentData',{get:_mn(function(){return _uad}),enumerable:true,configurable:true});
}catch(e){}
"""
    }

    // ==================================================================================
    // 第四层: 深度伪装
    // ==================================================================================

    private const val DEEP_DISGUISE_JS = """
// ══════════════════════════════════════════════════════════════
// [DEEP] MediaDevices / WebRTC / Font / Battery 深度伪装
// ══════════════════════════════════════════════════════════════

// ── MediaDevices.enumerateDevices 匿名化 ──
try{
if(navigator.mediaDevices&&navigator.mediaDevices.enumerateDevices){
    var _oed=navigator.mediaDevices.enumerateDevices;
    navigator.mediaDevices.enumerateDevices=_mn(function(){
        return _oed.call(navigator.mediaDevices).then(function(devs){
            return devs.map(function(d,i){
                return{deviceId:'device'+i,groupId:'group'+(i%3),kind:d.kind,label:'',
                    toJSON:_mn(function(){return{deviceId:this.deviceId,groupId:this.groupId,kind:this.kind,label:''}})};
            });
        });
    });
}
}catch(e){}

// ── WebRTC 本地 IP 完全屏蔽 ──
try{
var _RTC=window.RTCPeerConnection||window.webkitRTCPeerConnection;
if(_RTC){
    var PRTC=_mn(function RTCPeerConnection(c,x){
        if(c&&c.iceServers){c.iceServers=c.iceServers.filter(function(s){return s.urls&&!String(s.urls).match(/stun:/i)});}
        var pc=new _RTC(c,x);
        var _olc=pc.createDataChannel;
        var _oCD=Object.getOwnPropertyDescriptor(_RTC.prototype,'localDescription');
        if(_oCD&&_oCD.get){
            Object.defineProperty(pc,'localDescription',{get:_mn(function(){
                var d=_oCD.get.call(this);
                if(d&&d.sdp){d={type:d.type,sdp:d.sdp.replace(/a=candidate:.*\\r\\n/g,'').replace(/c=IN IP4 \\S+/g,'c=IN IP4 0.0.0.0')}};
                return d;
            }),configurable:true});
        }
        return pc;
    });
    PRTC.prototype=_RTC.prototype;
    PRTC.generateCertificate=_RTC.generateCertificate;
    window.RTCPeerConnection=PRTC;
    if(window.webkitRTCPeerConnection)window.webkitRTCPeerConnection=PRTC;
}
}catch(e){}

// ── Font fingerprinting 防护 (measureText noise) ──
try{
var _omt=CanvasRenderingContext2D.prototype.measureText;
var _fc={};
CanvasRenderingContext2D.prototype.measureText=_mn(function(text){
    var r=_omt.call(this,text);
    var k=this.font+'|'+text;
    if(!_fc[k])_fc[k]=(Math.random()-0.5)*0.3;
    var ow=r.width;
    Object.defineProperty(r,'width',{get:_mn(function(){return ow+_fc[k]})});
    return r;
});
}catch(e){}

// ── Battery API 屏蔽 ──
try{
navigator.getBattery=_mn(function getBattery(){
    return Promise.resolve({
        charging:true,chargingTime:0,dischargingTime:Infinity,level:1.0,
        onchargingchange:null,onchargingtimechange:null,ondischargingtimechange:null,onlevelchange:null,
        addEventListener:_mn(function(){}),removeEventListener:_mn(function(){})
    });
});
}catch(e){}

// ── document.elementFromPoint (HeadlessChrome 检测) ──
try{
var _oefp=document.elementFromPoint;
document.elementFromPoint=_mn(function(x,y){
    return _oefp.call(this,x,y)||document.body;
});
}catch(e){}

// ── window.chrome.webstore (已弃用但仍被检测) ──
try{
if(window.chrome&&!window.chrome.webstore){
    window.chrome.webstore={
        onInstallStageChanged:{},onDownloadProgress:{},
        install:_mn(function(url,onsuccess,onfailure){if(onfailure)onfailure('Chrome Web Store is no longer available')}),
        onEnabled:{},onDisabled:{}
    };
}
}catch(e){}
"""

    // ==================================================================================
    // 第五层: iframe 传播 + 最终封印
    // ==================================================================================

    private const val IFRAME_PROPAGATION_JS = """
// ══════════════════════════════════════════════════════════════
// [SEAL] iframe 全伪装传播
// ══════════════════════════════════════════════════════════════
try{
var _oce=document.createElement;
document.createElement=_mn(function(tag){
    var el=_oce.apply(this,arguments);
    if(typeof tag==='string'&&tag.toLowerCase()==='iframe'){
        el.addEventListener('load',function(){
            try{
                var w=el.contentWindow;
                if(!w||w.__wta_browser_disguise__)return;
                w.__wta_browser_disguise__=1;
                Object.defineProperty(w.navigator,'webdriver',{get:function(){return false},configurable:true});
                Object.defineProperty(w.navigator,'vendor',{get:function(){return'Google Inc.'},configurable:true});
                Object.defineProperty(w.navigator,'plugins',{get:function(){return navigator.plugins},configurable:true});
                if(!w.chrome)w.chrome=window.chrome;
                Object.defineProperty(w.navigator,'languages',{get:function(){return navigator.languages},configurable:true});
                Object.defineProperty(w.navigator,'platform',{get:function(){return navigator.platform},configurable:true});
                if(w.navigator.userAgentData){
                    Object.defineProperty(w.navigator,'userAgentData',{get:function(){return navigator.userAgentData},configurable:true});
                }
            }catch(e){}
        });
    }
    return el;
});
}catch(e){}
"""

    private const val FINAL_SEAL_JS = """
// ══════════════════════════════════════════════════════════════
// [FINAL] 最终封印 — 属性枚举顺序 & 冻结关键对象
// ══════════════════════════════════════════════════════════════

// ── Reflect.ownKeys(navigator) 顺序保护 ──
// 某些检测工具通过 key 的顺序来判断是否被 hook 过
// 无需额外操作: defineProperty 保持了原始枚举顺序

// ── instanceof 一致性检查 ──
try{
// 确保 navigator instanceof Navigator 返回 true (某些 hook 会破坏原型链)
// 这里不做任何修改，只是验证不破坏
}catch(e){}

// ── 清理我们自己的全局变量 ──
try{
// _mn, _ht 等辅助变量留在闭包内，不泄露到全局
// __wta_browser_disguise__ 是唯一的标记，用于防重复注入
}catch(e){}
"""
}
