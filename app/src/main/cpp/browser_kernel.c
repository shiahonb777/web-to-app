/**
 * browser_kernel.c — browser-level cloaking engine
 *
 * Injects layered JavaScript so WebView cannot be distinguished from Chrome.
 * Covers navigator spoofing, window.chrome reconstruction, WebGL/permissions,
 * iframe detection bypass, prototype protections, WebRTC, automation flags,
 * screen/window metrics, DevTools interference, connection APIs, and media enumeration.
 * Injects at DOCUMENT_START via evaluateJavascript() from the native layer.
 * Design principles:
 * - Hook via Object.defineProperty instead of replacing properties.
 * - Getters mirror real Chrome values.
 * - Function.prototype.toString proxies return "[native code]".
 * - All intercepted APIs report values that match a real browser.
 */

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <android/log.h>

#define TAG "BrowserKernel"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)

/* ====================================================================
 * Core anti-detection JavaScript — injected at DOCUMENT_START
 *
 * Must run before any page script executes.
 * Wrapped in an IIFE to avoid polluting the global namespace.
 * ==================================================================== */

static const char KERNEL_JS[] =
"(function(){'use strict';"

/* ================================================================
 * 0. Utility helpers — hide all hook traces
 * ================================================================ */

/* Save the original toString to prevent detection */
"var _origToString=Function.prototype.toString;"
"var _hookedFns=new WeakSet();"

/* Ensure hooked functions' toString() returns [native code] */
"var _nativeToString=function(){"
  "if(_hookedFns.has(this))return'function '+this.name+'() { [native code] }';"
  "return _origToString.call(this);"
"};"
"_hookedFns.add(_nativeToString);"
"Object.defineProperty(Function.prototype,'toString',{value:_nativeToString,writable:true,configurable:true});"

/* Helper to define properties safely */
"function _def(obj,prop,val){"
  "try{Object.defineProperty(obj,prop,{get:function(){return val},set:function(){},enumerable:true,configurable:true});}catch(e){}"
"}"
"function _defFn(obj,prop,fn){"
  "try{_hookedFns.add(fn);Object.defineProperty(obj,prop,{value:fn,writable:false,enumerable:true,configurable:true});}catch(e){}"
"}"

/* Note. */

/* Remove webdriver flag (Selenium/Puppeteer detection) */
"_def(navigator,'webdriver',false);"
"Object.defineProperty(navigator,'webdriver',{get:function(){return false},enumerable:true,configurable:true});"

/* Remove global markers left by automation frameworks */
"delete window.__selenium_unwrapped;"
"delete window.__webdriver_evaluate;"
"delete window.__webdriver_script_function;"
"delete window.__webdriver_script_func;"
"delete window.__webdriver_script_fn;"
"delete window.__fxdriver_evaluate;"
"delete window.__driver_evaluate;"
"delete window.__webdriver_unwrapped;"
"delete window.__nightmarejs;"
"delete window._phantom;"
"delete window.phantom;"
"delete window.callPhantom;"
"delete window.__phantomas;"
"delete window.Buffer;"
"delete window.emit;"
"delete window.spawn;"
"delete window.domAutomation;"
"delete window.domAutomationController;"
"delete window.external;"
"delete window._Selenium_IDE_Recorder;"

/* Note. */

/* plugins — Chrome ships with five default plugins */
"var _mkPlugin=function(name,desc,filename,mimes){"
  "var p={name:name,description:desc,filename:filename,length:mimes.length};"
  "for(var i=0;i<mimes.length;i++){p[i]=mimes[i];p[mimes[i].type]=mimes[i];}"
  "p[Symbol.iterator]=function(){return mimes[Symbol.iterator]();};"
  "return p;"
"};"

"var _pdfMime={type:'application/pdf',suffixes:'pdf',description:'Portable Document Format',enabledPlugin:null};"
"var _plugins=["
  "_mkPlugin('PDF Viewer','Portable Document Format','internal-pdf-viewer',[_pdfMime]),"
  "_mkPlugin('Chrome PDF Plugin','Portable Document Format','internal-pdf-viewer',[_pdfMime]),"
  "_mkPlugin('Chrome PDF Viewer','Portable Document Format','internal-pdf-viewer',[_pdfMime]),"
  "_mkPlugin('Native Client','','internal-nacl-plugin',[]),"
  "_mkPlugin('Chromium PDF Plugin','Portable Document Format','internal-pdf-viewer',[_pdfMime])"
"];"

/* PluginArray fully emulated */
"var _pluginArray={"
  "length:_plugins.length,"
  "item:function(i){return _plugins[i]||null;},"
  "namedItem:function(n){for(var i=0;i<_plugins.length;i++){if(_plugins[i].name===n)return _plugins[i];}return null;},"
  "refresh:function(){},"
  "[Symbol.iterator]:function(){return _plugins[Symbol.iterator]();}"
"};"
"for(var i=0;i<_plugins.length;i++){_pluginArray[i]=_plugins[i];}"
"_def(navigator,'plugins',_pluginArray);"

/* mimeTypes */
"var _mimeArray={"
  "length:1,"
  "item:function(i){return i===0?_pdfMime:null;},"
  "namedItem:function(n){return n==='application/pdf'?_pdfMime:null;},"
  "[Symbol.iterator]:function(){return[_pdfMime][Symbol.iterator]();}"
"};"
"_mimeArray[0]=_pdfMime;"
"_mimeArray['application/pdf']=_pdfMime;"
"_def(navigator,'mimeTypes',_mimeArray);"

/* languages — retain defaults or system locale */
"if(!navigator.languages||navigator.languages.length===0){"
  "_def(navigator,'languages',Object.freeze(['zh-CN','zh','en-US','en']));"
"}"

/* hardwareConcurrency — at least four cores */
"if(navigator.hardwareConcurrency<4)_def(navigator,'hardwareConcurrency',4);"

/* deviceMemory — at least 4GB */
"if(!navigator.deviceMemory||navigator.deviceMemory<4)_def(navigator,'deviceMemory',8);"

/* maxTouchPoints — must be >0 on mobile */
"if(!navigator.maxTouchPoints)_def(navigator,'maxTouchPoints',5);"

/* vendor — Chrome reports "Google Inc." */
"_def(navigator,'vendor','Google Inc.');"

/* platform — preserve original value and ensure it exists */
"if(!navigator.platform)_def(navigator,'platform','Linux armv81');"

/* Note. */

"if(!window.chrome)window.chrome={};"
"if(!window.chrome.app)window.chrome.app={"
  "isInstalled:false,"
  "InstallState:{'DISABLED':'disabled','INSTALLED':'installed','NOT_INSTALLED':'not_installed'},"
  "RunningState:{'CANNOT_RUN':'cannot_run','READY_TO_RUN':'ready_to_run','RUNNING':'running'},"
  "getDetails:function(){return null;},"
  "getIsInstalled:function(){return false;},"
  "installState:function(cb){if(cb)cb('not_installed');}"
"};"

"if(!window.chrome.runtime){"
  "window.chrome.runtime={"
    "OnInstalledReason:{CHROME_UPDATE:'chrome_update',INSTALL:'install',SHARED_MODULE_UPDATE:'shared_module_update',UPDATE:'update'},"
    "OnRestartRequiredReason:{APP_UPDATE:'app_update',OS_UPDATE:'os_update',PERIODIC:'periodic'},"
    "PlatformArch:{ARM:'arm',ARM64:'arm64',MIPS:'mips',MIPS64:'mips64',X86_32:'x86-32',X86_64:'x86-64'},"
    "PlatformNaclArch:{ARM:'arm',MIPS:'mips',MIPS64:'mips64',X86_32:'x86-32',X86_64:'x86-64'},"
    "PlatformOs:{ANDROID:'android',CROS:'cros',LINUX:'linux',MAC:'mac',OPENBSD:'openbsd',WIN:'win'},"
    "RequestUpdateCheckStatus:{NO_UPDATE:'no_update',THROTTLED:'throttled',UPDATE_AVAILABLE:'update_available'},"
    "connect:function(){return{onDisconnect:{addListener:function(){}},onMessage:{addListener:function(){}},postMessage:function(){},disconnect:function(){}};},"
    "sendMessage:function(){},"
    "id:undefined"
  "};"
"}"

/* chrome.loadTimes — Chrome-specific API */
"if(!window.chrome.loadTimes){"
  "window.chrome.loadTimes=function(){"
    "return{"
      "commitLoadTime:Date.now()/1000,"
      "connectionInfo:'http/1.1',"
      "finishDocumentLoadTime:Date.now()/1000,"
      "finishLoadTime:0,"
      "firstPaintAfterLoadTime:0,"
      "firstPaintTime:Date.now()/1000,"
      "navigationType:'Other',"
      "npnNegotiatedProtocol:'http/1.1',"
      "requestTime:Date.now()/1000-0.3,"
      "startLoadTime:Date.now()/1000-0.3,"
      "wasAlternateProtocolAvailable:false,"
      "wasFetchedViaSpdy:false,"
      "wasNpnNegotiated:true"
    "};"
  "};"
  "_hookedFns.add(window.chrome.loadTimes);"
"}"

/* chrome.csi — Chrome-specific API */
"if(!window.chrome.csi){"
  "window.chrome.csi=function(){"
    "return{"
      "onloadT:Date.now(),"
      "pageT:performance.now(),"
      "tran:15"   /* 15 = LINK */
    "};"
  "};"
  "_hookedFns.add(window.chrome.csi);"
"}"

/* Note. */

"(function(){"
  "var _origGetParam;"
  "try{"
    "var c=document.createElement('canvas');"
    "var gl=c.getContext('webgl')||c.getContext('experimental-webgl');"
    "if(gl){"
      "_origGetParam=gl.__proto__.getParameter;"
      "gl.__proto__.getParameter=function(p){"
        "var ext=this.getExtension('WEBGL_debug_renderer_info');"
        "if(ext){"
          /* UNMASKED_VENDOR_WEBGL = 0x9245 */
          "if(p===ext.UNMASKED_VENDOR_WEBGL||p===37445)return'Google Inc. (Qualcomm)';"
          /* UNMASKED_RENDERER_WEBGL = 0x9246 */
          "if(p===ext.UNMASKED_RENDERER_WEBGL||p===37446)return'ANGLE (Qualcomm, Adreno (TM) 640, OpenGL ES 3.2)';"
        "}"
        "return _origGetParam.call(this,p);"
      "};"
      "_hookedFns.add(gl.__proto__.getParameter);"
    "}"
  "}catch(e){}"
"})();"

/* Note. */

"(function(){"
  "if(!navigator.permissions)return;"
  "var _origQuery=navigator.permissions.query;"
  "navigator.permissions.query=function(desc){"
    "if(desc&&desc.name==='notifications'){"
      /* Return denied instead of prompt (WebView trait) */
      "return Promise.resolve({state:'denied',onchange:null});"
    "}"
    "return _origQuery.call(this,desc);"
  "};"
  "_hookedFns.add(navigator.permissions.query);"
"})();"

/* Note. */

"if(!window.outerWidth||window.outerWidth===0){"
  "_def(window,'outerWidth',window.innerWidth);"
"}"
"if(!window.outerHeight||window.outerHeight===0){"
  "_def(window,'outerHeight',window.innerHeight+56);"  /* 56px = browser address bar height */
"}"

/* screenX/screenY — real browsers never return 0 */
"if(!window.screenX&&!window.screenY){"
  "_def(window,'screenX',0);"
  "_def(window,'screenY',56);"
"}"

/* Note. */

"(function(){"
  "var _origCreate=document.createElement;"
  "var _iframeContentWindowGet;"
  "try{_iframeContentWindowGet=Object.getOwnPropertyDescriptor(HTMLIFrameElement.prototype,'contentWindow').get;}catch(e){}"
  "document.createElement=function(tag){"
    /* In strict mode this may be undefined; fallback to document */
    "var el=_origCreate.call(this&&this.createElement?this:document,tag);"
    "if(_iframeContentWindowGet&&tag.toLowerCase()==='iframe'){"
      "Object.defineProperty(el,'contentWindow',{"
        "get:function(){"
          "var w=_iframeContentWindowGet.call(this);"
          "if(w&&!w.chrome){"
            "try{w.chrome=window.chrome;}catch(e){}"
          "}"
          "return w;"
        "},"
        "configurable:true"
      "});"
    "}"
    "return el;"
  "};"
  "_hookedFns.add(document.createElement);"
"})();"


/* Note. */

"if(!window.Notification){"
  "window.Notification=function(title,opts){"
    "this.title=title;this.body=opts?opts.body:'';this.icon=opts?opts.icon:'';"
    "this.onclick=null;this.onclose=null;"
  "};"
  "window.Notification.permission='default';"
  "window.Notification.requestPermission=function(cb){"
    "var p=Promise.resolve('denied');"
    "if(cb)cb('denied');"
    "return p;"
  "};"
  "_hookedFns.add(window.Notification);"
  "_hookedFns.add(window.Notification.requestPermission);"
"}"

/* Note. */

"(function(){"
  "var _origDebug=console.debug;"
  "console.debug=function(){"
    "return _origDebug.apply(this,arguments);"
  "};"
  "_hookedFns.add(console.debug);"
"})();"

/* Note. */

"if(!navigator.connection){"
  "var _conn={"
    "effectiveType:'4g',"
    "rtt:50,"
    "downlink:10,"
    "saveData:false,"
    "onchange:null,"
    "addEventListener:function(){},"
    "removeEventListener:function(){}"
  "};"
  "_def(navigator,'connection',_conn);"
"}"

/* Note. */

"if(!navigator.getBattery){"
  "navigator.getBattery=function(){"
    "return Promise.resolve({"
      "charging:true,"
      "chargingTime:0,"
      "dischargingTime:Infinity,"
      "level:0.95,"
      "addEventListener:function(){},"
      "removeEventListener:function(){}"
    "});"
  "};"
  "_hookedFns.add(navigator.getBattery);"
"}"

/* Note. */

"(function(){"
  "var _origDesc=Object.getOwnPropertyDescriptor;"
  "Object.getOwnPropertyDescriptor=function(obj,prop){"
    /* Return native-like descriptors for our hooked properties */
    "if(obj===navigator&&(prop==='webdriver'||prop==='plugins'||prop==='mimeTypes'"
      "||prop==='languages'||prop==='hardwareConcurrency'||prop==='deviceMemory'"
      "||prop==='vendor'||prop==='maxTouchPoints'||prop==='connection')){"
      /* Native navigator properties are accessors; return accessor descriptors */
      "var v=navigator[prop];"
      "return{get:function(){return v},set:undefined,enumerable:true,configurable:true};"
    "}"
    /* In strict mode this may be undefined; fallback to Object */
    "return _origDesc.call(this||Object,obj,prop);"
  "};"
  "_hookedFns.add(Object.getOwnPropertyDescriptor);"
"})();"


/* Note. */

"(function(){"
  "var _suspects=['AndroidBridge','WebViewBridge','__WTA_BRIDGE__',"
    "'webkit','__WebViewJavascriptBridge','WKWebViewJavascriptBridge',"
    "'flutter_inappwebview','_cordovaNative','_AirshipInterface',"
    "'ReactNativeWebView','nativeApp','androidObj'];"
  "for(var i=0;i<_suspects.length;i++){"
    "try{if(window[_suspects[i]]){delete window[_suspects[i]];}}catch(e){}"
  "}"
"})();"

/* Note. */

"(function(){"
  "try{"
    /* V8's Error.prepareStackTrace allows custom stack trace formatting */
    /* This is the only safe way to tweak stack traces without breaking instanceof */
    "var _origPST=Error.prepareStackTrace;"
    "Error.prepareStackTrace=function(err,stack){"
      "var formatted=_origPST?_origPST(err,stack):"
        "err.toString()+'\\n'+stack.map(function(f){"
          "return'    at '+f;"
        "}).join('\\n');"
      /* Clean WebView traits out of stack traces */
      "if(typeof formatted==='string'){"
        "formatted=formatted.replace(/(?:webview|WebView|evaluateJavascript)/gi,'chrome');"
        /* Precisely remove "wv" markers (avoid touching words like "review"/"overview") */
        "formatted=formatted.replace(/\\bwv\\b/g,'chrome');"
      "}"
      "return formatted;"
    "};"
  "}catch(e){}"
"})();"

"})();";


/* Note. */

/* Note. */
static int sanitize_user_agent(const char *ua, char *out, int out_size) {
    if (!ua || !out || out_size <= 0) return -1;
    
    int src_len = (int)strlen(ua);
    int dst = 0;
    
    for (int i = 0; i < src_len && dst < out_size - 1; i++) {
        /* Detect " wv", ";wv", or "; wv" */
        if (i + 2 < src_len && (ua[i] == ' ' || ua[i] == ';')) {
            /* " wv" */
            if (ua[i] == ' ' && ua[i+1] == 'w' && ua[i+2] == 'v' &&
                (i+3 >= src_len || ua[i+3] == ')' || ua[i+3] == ' ' || ua[i+3] == ';')) {
                i += 2; /* Skip " wv" */
                continue;
            }
            /* "; wv" */
            if (ua[i] == ';' && i+3 < src_len &&
                ua[i+1] == ' ' && ua[i+2] == 'w' && ua[i+3] == 'v' &&
                (i+4 >= src_len || ua[i+4] == ')' || ua[i+4] == ' ')) {
                i += 3; /* Skip "; wv" */
                continue;
            }
            /* ";wv" */
            if (ua[i] == ';' && ua[i+1] == 'w' && ua[i+2] == 'v' &&
                (i+3 >= src_len || ua[i+3] == ')' || ua[i+3] == ' ')) {
                i += 2;
                continue;
            }
        }
        
        /* Detect "Version/X.X " (WebView-specific) */
        if (i + 8 < src_len && ua[i] == 'V' &&
            strncmp(ua + i, "Version/", 8) == 0) {
            /* Skip "Version/X.X.X.X " */
            int j = i + 8;
            while (j < src_len && (ua[j] == '.' || (ua[j] >= '0' && ua[j] <= '9'))) j++;
            if (j < src_len && ua[j] == ' ') j++; /* Skip trailing space */
            i = j - 1;
            continue;
        }
        
        out[dst++] = ua[i];
    }
    
    out[dst] = '\0';
    return dst;
}


/* Note. */

#define JNI_FUNC(name) Java_com_webtoapp_core_kernel_BrowserKernel_##name

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetKernelJs)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, KERNEL_JS);
}

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeSanitizeUserAgent)(JNIEnv *env, jobject thiz, jstring jUa) {
    (void)thiz;
    if (!jUa) return NULL;
    
    const char *ua = (*env)->GetStringUTFChars(env, jUa, NULL);
    if (!ua) return NULL;
    
    char buf[1024];
    int len = sanitize_user_agent(ua, buf, sizeof(buf));
    
    (*env)->ReleaseStringUTFChars(env, jUa, ua);
    
    if (len <= 0) return jUa;
    return (*env)->NewStringUTF(env, buf);
}

/* Note. */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeBuildChromeUserAgent)(JNIEnv *env, jobject thiz, jstring jUa) {
    (void)thiz;
    if (!jUa) return NULL;
    
    const char *ua = (*env)->GetStringUTFChars(env, jUa, NULL);
    if (!ua) return NULL;
    
    char sanitized[1024];
    sanitize_user_agent(ua, sanitized, sizeof(sanitized));
    
    (*env)->ReleaseStringUTFChars(env, jUa, ua);
    
    /* Check if Chrome/ is already present */
    if (strstr(sanitized, "Chrome/") != NULL) {
        return (*env)->NewStringUTF(env, sanitized);
    }
    
    /* Append Chrome/ if it's missing */
    char result[1200];
    int len = (int)strlen(sanitized);
    /* Insert Chrome/ before Safari/ if needed */
    char *safari_pos = strstr(sanitized, "Safari/");
    if (safari_pos) {
        int prefix_len = (int)(safari_pos - sanitized);
        snprintf(result, sizeof(result), "%.*sChrome/131.0.0.0 %s",
                prefix_len, sanitized, safari_pos);
    } else {
        snprintf(result, sizeof(result), "%s Chrome/131.0.0.0", sanitized);
    }
    
    return (*env)->NewStringUTF(env, result);
}

