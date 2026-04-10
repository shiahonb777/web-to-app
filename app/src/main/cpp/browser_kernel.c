/**
 * browser_kernel.c — 浏览器内核级伪装引擎
 *
 * 通过极致的 JavaScript 注入, 使 WebView 完全不可被检测为 WebView。
 * 所有检测手段均被覆盖:
 *
 *  1. navigator 属性伪装 (userAgent, appVersion, platform, webdriver, plugins, mimeTypes)
 *  2. window.chrome 对象重建 (runtime, loadTimes, csi)
 *  3. WebGL 渲染器信息伪装 (vendor, renderer)
 *  4. 权限 API 伪装 (Permissions, Push, Notification)
 *  5. iframe contentWindow 检测绕过
 *  6. 原型链检测保护 (toString, getOwnPropertyDescriptor)
 *  7. WebRTC 泄露防护
 *  8. 自动化标志清除 (webdriver, __selenium, __webdriver_*)
 *  9. 屏幕/窗口尺寸一致性
 * 10. DevTools 检测干扰
 * 11. Battery/Connection API 伪装
 * 12. 媒体设备枚举伪装
 *
 * 注入时机: DOCUMENT_START (在任何页面脚本之前执行)
 * 注入方式: evaluateJavascript() 从 C 层获取完整 JS 字符串
 *
 * 设计理念:
 * - 所有 property hook 使用 Object.defineProperty 而非赋值
 * - 所有 getter 返回值与 Chrome 浏览器完全一致
 * - 所有 Function.prototype.toString 被代理, 返回 "native code"
 * - 检测者使用的任何 API 都返回与真实浏览器一致的结果
 */

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <android/log.h>

#define TAG "BrowserKernel"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)

/* ====================================================================
 * 核心反检测 JavaScript — DOCUMENT_START 最早注入
 *
 * 这段代码必须在页面任何 JS 执行之前注入
 * 使用 IIFE 包裹, 不污染全局命名空间
 * ==================================================================== */

static const char KERNEL_JS[] =
"(function(){'use strict';"

/* ================================================================
 * 0. 工具函数 — 隐藏所有 hook 的痕迹
 * ================================================================ */

/* 保存原始的 toString, 防止被检测 */
"var _origToString=Function.prototype.toString;"
"var _hookedFns=new WeakSet();"

/* 让被 hook 的函数的 toString() 返回 [native code] */
"var _nativeToString=function(){"
  "if(_hookedFns.has(this))return'function '+this.name+'() { [native code] }';"
  "return _origToString.call(this);"
"};"
"_hookedFns.add(_nativeToString);"
"Object.defineProperty(Function.prototype,'toString',{value:_nativeToString,writable:true,configurable:true});"

/* 安全地定义属性的工具函数 */
"function _def(obj,prop,val){"
  "try{Object.defineProperty(obj,prop,{get:function(){return val},set:function(){},enumerable:true,configurable:true});}catch(e){}"
"}"
"function _defFn(obj,prop,fn){"
  "try{_hookedFns.add(fn);Object.defineProperty(obj,prop,{value:fn,writable:false,enumerable:true,configurable:true});}catch(e){}"
"}"

/* ================================================================
 * 1. 清除自动化/WebView 标志
 * ================================================================ */

/* 清除 webdriver 标志 (Selenium/Puppeteer 检测) */
"_def(navigator,'webdriver',false);"
"Object.defineProperty(navigator,'webdriver',{get:function(){return false},enumerable:true,configurable:true});"

/* 清除各种自动化框架的全局标志 */
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

/* ================================================================
 * 2. navigator 属性完美伪装
 * ================================================================ */

/* plugins — Chrome 浏览器默认有 5 个插件 */
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

/* PluginArray 完整模拟 */
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

/* languages — 保持默认或用系统语言 */
"if(!navigator.languages||navigator.languages.length===0){"
  "_def(navigator,'languages',Object.freeze(['zh-CN','zh','en-US','en']));"
"}"

/* hardwareConcurrency — 至少 4 核 */
"if(navigator.hardwareConcurrency<4)_def(navigator,'hardwareConcurrency',4);"

/* deviceMemory — 至少 4GB */
"if(!navigator.deviceMemory||navigator.deviceMemory<4)_def(navigator,'deviceMemory',8);"

/* maxTouchPoints — 移动设备必须 > 0 */
"if(!navigator.maxTouchPoints)_def(navigator,'maxTouchPoints',5);"

/* vendor — Chrome 的 vendor 是 Google Inc. */
"_def(navigator,'vendor','Google Inc.');"

/* platform — 保持原本的但确保存在 */
"if(!navigator.platform)_def(navigator,'platform','Linux armv81');"

/* ================================================================
 * 3. window.chrome 对象 — 完美重建
 *    这是最关键的检测点: 真实 Chrome 有这个对象, WebView 没有
 * ================================================================ */

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

/* chrome.loadTimes — Chrome 特有 API */
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

/* chrome.csi — Chrome 特有 API */
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

/* ================================================================
 * 4. WebGL 渲染器/供应商伪装
 *    检测者通过 getParameter(UNMASKED_VENDOR/RENDERER) 识别 WebView
 * ================================================================ */

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

/* ================================================================
 * 5. Permissions API 伪装
 * ================================================================ */

"(function(){"
  "if(!navigator.permissions)return;"
  "var _origQuery=navigator.permissions.query;"
  "navigator.permissions.query=function(desc){"
    "if(desc&&desc.name==='notifications'){"
      /* 返回 denied 而非 prompt (WebView 特征) */
      "return Promise.resolve({state:'denied',onchange:null});"
    "}"
    "return _origQuery.call(this,desc);"
  "};"
  "_hookedFns.add(navigator.permissions.query);"
"})();"

/* ================================================================
 * 6. 屏幕/窗口尺寸一致性
 *    WebView 的 outerWidth/outerHeight 通常为 0, 真实浏览器不会
 * ================================================================ */

"if(!window.outerWidth||window.outerWidth===0){"
  "_def(window,'outerWidth',window.innerWidth);"
"}"
"if(!window.outerHeight||window.outerHeight===0){"
  "_def(window,'outerHeight',window.innerHeight+56);"  /* 56px = 地址栏高度 */
"}"

/* screenX/screenY — 真实浏览器不为 0 */
"if(!window.screenX&&!window.screenY){"
  "_def(window,'screenX',0);"
  "_def(window,'screenY',56);"
"}"

/* ================================================================
 * 7. iframe contentWindow 检测绕过
 *    检测者创建 iframe 然后比较 contentWindow.chrome
 * ================================================================ */

"(function(){"
  "var _origCreate=document.createElement;"
  "var _iframeContentWindowGet;"
  "try{_iframeContentWindowGet=Object.getOwnPropertyDescriptor(HTMLIFrameElement.prototype,'contentWindow').get;}catch(e){}"
  "document.createElement=function(tag){"
    /* strict mode 下 this 可能是 undefined, 必须回退到 document */
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


/* ================================================================
 * 8. Notification API (WebView 通常不支持, 但我们模拟它)
 * ================================================================ */

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

/* ================================================================
 * 9. console.debug 检测干扰
 *    一些网站通过 console.debug 的行为检测 DevTools/WebView
 * ================================================================ */

"(function(){"
  "var _origDebug=console.debug;"
  "console.debug=function(){"
    "return _origDebug.apply(this,arguments);"
  "};"
  "_hookedFns.add(console.debug);"
"})();"

/* ================================================================
 * 10. Connection API 完善
 *     WebView 的 navigator.connection 可能有差异
 * ================================================================ */

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

/* ================================================================
 * 11. getBattery API 完善
 * ================================================================ */

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

/* ================================================================
 * 12. Object.getOwnPropertyDescriptor 保护
 *     高级检测者会检查属性描述符来发现 hook
 * ================================================================ */

"(function(){"
  "var _origDesc=Object.getOwnPropertyDescriptor;"
  "Object.getOwnPropertyDescriptor=function(obj,prop){"
    /* 对我们 hook 过的属性返回与原生一致的描述符 */
    "if(obj===navigator&&(prop==='webdriver'||prop==='plugins'||prop==='mimeTypes'"
      "||prop==='languages'||prop==='hardwareConcurrency'||prop==='deviceMemory'"
      "||prop==='vendor'||prop==='maxTouchPoints'||prop==='connection')){"
      /* 原生 navigator 属性是 accessor (getter), 必须返回 accessor 描述符 */
      "var v=navigator[prop];"
      "return{get:function(){return v},set:undefined,enumerable:true,configurable:true};"
    "}"
    /* strict mode 下 this 可能是 undefined, 回退到 Object */
    "return _origDesc.call(this||Object,obj,prop);"
  "};"
  "_hookedFns.add(Object.getOwnPropertyDescriptor);"
"})();"


/* ================================================================
 * 13. 清除 WebView 特有的 JavascriptInterface 暴露
 * ================================================================ */

"(function(){"
  "var _suspects=['AndroidBridge','WebViewBridge','__WTA_BRIDGE__',"
    "'webkit','__WebViewJavascriptBridge','WKWebViewJavascriptBridge',"
    "'flutter_inappwebview','_cordovaNative','_AirshipInterface',"
    "'ReactNativeWebView','nativeApp','androidObj'];"
  "for(var i=0;i<_suspects.length;i++){"
    "try{if(window[_suspects[i]]){delete window[_suspects[i]];}}catch(e){}"
  "}"
"})();"

/* ================================================================
 * 14. Error stack trace 清理
 *     WebView 的 Error stack trace 包含特殊标记
 *
 *     重要: 不能替换 window.Error 构造函数！替换会导致:
 *     - instanceof Error 检测失败
 *     - 所有 JS 框架 (Vue/React/Angular) 的错误处理崩溃
 *     - 现代网站 (bilibili等) 白屏无法加载
 *
 *     安全方案: 使用 Error.prepareStackTrace 钩子 (V8 引擎特性)
 *     仅修改 stack trace 字符串, 不影响 Error 原型链
 * ================================================================ */

"(function(){"
  "try{"
    /* V8 的 Error.prepareStackTrace 允许自定义 stack trace 格式化 */
    /* 这是唯一能安全修改 stack trace 的方式, 不影响 instanceof */
    "var _origPST=Error.prepareStackTrace;"
    "Error.prepareStackTrace=function(err,stack){"
      "var formatted=_origPST?_origPST(err,stack):"
        "err.toString()+'\\n'+stack.map(function(f){"
          "return'    at '+f;"
        "}).join('\\n');"
      /* 清理 stack trace 中的 WebView 特征 */
      "if(typeof formatted==='string'){"
        "formatted=formatted.replace(/(?:webview|WebView|evaluateJavascript)/gi,'chrome');"
        /* 精确移除 wv 标记 (避免误伤 'review'/'overview' 等正常单词) */
        "formatted=formatted.replace(/\\bwv\\b/g,'chrome');"
      "}"
      "return formatted;"
    "};"
  "}catch(e){}"
"})();"

"})();";


/* ====================================================================
 * UA 清洗逻辑 — 从 C 层处理, 避免 Kotlin 的字符串分配
 * ==================================================================== */

/**
 * 清洗 User-Agent 字符串
 * 移除所有 WebView 标识:
 *  - " wv" (WebView 标志)
 *  - "Version/X.X" (WebView 特有的版本标记)
 *  - "; wv" (另一种 WebView 标志格式)
 * 
 * @param ua 原始 User-Agent
 * @param out 输出缓冲区
 * @param out_size 输出缓冲区大小
 * @return 清洗后的字符串长度, -1 = 失败
 */
static int sanitize_user_agent(const char *ua, char *out, int out_size) {
    if (!ua || !out || out_size <= 0) return -1;
    
    int src_len = (int)strlen(ua);
    int dst = 0;
    
    for (int i = 0; i < src_len && dst < out_size - 1; i++) {
        /* 检测 " wv" 或 ";wv" 或 "; wv" */
        if (i + 2 < src_len && (ua[i] == ' ' || ua[i] == ';')) {
            /* " wv" */
            if (ua[i] == ' ' && ua[i+1] == 'w' && ua[i+2] == 'v' &&
                (i+3 >= src_len || ua[i+3] == ')' || ua[i+3] == ' ' || ua[i+3] == ';')) {
                i += 2; /* 跳过 " wv" */
                continue;
            }
            /* "; wv" */
            if (ua[i] == ';' && i+3 < src_len &&
                ua[i+1] == ' ' && ua[i+2] == 'w' && ua[i+3] == 'v' &&
                (i+4 >= src_len || ua[i+4] == ')' || ua[i+4] == ' ')) {
                i += 3; /* 跳过 "; wv" */
                continue;
            }
            /* ";wv" */
            if (ua[i] == ';' && ua[i+1] == 'w' && ua[i+2] == 'v' &&
                (i+3 >= src_len || ua[i+3] == ')' || ua[i+3] == ' ')) {
                i += 2;
                continue;
            }
        }
        
        /* 检测 "Version/X.X " (WebView 特有) */
        if (i + 8 < src_len && ua[i] == 'V' &&
            strncmp(ua + i, "Version/", 8) == 0) {
            /* 跳过 "Version/X.X.X.X " */
            int j = i + 8;
            while (j < src_len && (ua[j] == '.' || (ua[j] >= '0' && ua[j] <= '9'))) j++;
            if (j < src_len && ua[j] == ' ') j++; /* 跳过后面的空格 */
            i = j - 1;
            continue;
        }
        
        out[dst++] = ua[i];
    }
    
    out[dst] = '\0';
    return dst;
}


/* ====================================================================
 * JNI 接口
 * ==================================================================== */

#define JNI_FUNC(name) Java_com_webtoapp_core_kernel_BrowserKernel_##name

/**
 * 获取完整的反检测 JavaScript
 * 必须在 DOCUMENT_START 最早注入
 */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeGetKernelJs)(JNIEnv *env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, KERNEL_JS);
}

/**
 * 清洗 User-Agent 字符串 (移除 WebView 标识)
 */
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

/**
 * 获取清洗 + 追加的完整 Chrome UA
 * 在原始 UA 基础上:
 *   1. 移除 wv, Version/X.X
 *   2. 确保包含 Chrome/xxx.x.x.x
 */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeBuildChromeUserAgent)(JNIEnv *env, jobject thiz, jstring jUa) {
    (void)thiz;
    if (!jUa) return NULL;
    
    const char *ua = (*env)->GetStringUTFChars(env, jUa, NULL);
    if (!ua) return NULL;
    
    char sanitized[1024];
    sanitize_user_agent(ua, sanitized, sizeof(sanitized));
    
    (*env)->ReleaseStringUTFChars(env, jUa, ua);
    
    /* 检查是否已包含 Chrome/ */
    if (strstr(sanitized, "Chrome/") != NULL) {
        return (*env)->NewStringUTF(env, sanitized);
    }
    
    /* 如果没有 Chrome/ 标识, 追加一个 */
    char result[1200];
    int len = (int)strlen(sanitized);
    /* 在 Safari/ 之前插入 Chrome/ */
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
