package com.webtoapp.core.disguise

import android.webkit.WebView
import com.webtoapp.core.kernel.BrowserKernel
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.webview.OAuthCompatEngine

/**
 * 浏览器伪装统一编排引擎 v2.0
 * 
 * ## 🎯 职责
 * 
 * 统一编排三大反检测子系统, 确保无冲突、可诊断、按优先级注入:
 * 
 * ```
 * ┌─────────────────────────────────────────────────────┐
 * │           BrowserDisguiseEngine (本模块)              │
 * │                 统一编排 & 冲突解决                     │
 * ├────────────┬────────────────┬───────────────────────┤
 * │ BrowserKernel │ BrowserDisguise │ OAuthCompatEngine │
 * │ (HTTP层+基础JS) │ (22向量指纹伪装) │ (16+提供商专用)   │
 * │ Level 1 基础   │ Level 2-5 高级  │ OAuth 专用        │
 * └────────────┴────────────────┴───────────────────────┘
 * ```
 * 
 * ## 🔒 注入顺序 (DOCUMENT_START)
 * 
 * 1. **BrowserKernel** — HTTP 层 (UA sanitize, X-Requested-With) + 基础 JS
 * 2. **BrowserDisguise** — 22 向量指纹伪装 (Canvas/WebGL/Audio/Screen...)
 * 3. **OAuthCompatEngine** — OAuth 提供商专用反检测 (Google/FB/MS...)
 * 4. **FingerprintDiagnostic** — 可选诊断探针 (仅 debug 模式)
 * 
 * ## 🛡️ 冲突解决策略
 * 
 * - guard flag 互斥: 每个子系统有独立的 `window.__wta_xxx__` 标志
 * - BrowserDisguise 不重复 Level 1 向量 (当 BrowserKernel 已注入时)
 * - OAuth 层叠: OAuthCompatEngine 可覆盖 BrowserDisguise 的通用设置
 */
object BrowserDisguiseEngine {
    
    private const val TAG = "BrowserDisguiseEngine"
    
    /**
     * 引擎注入结果
     */
    data class InjectionResult(
        val kernelInjected: Boolean,
        val disguiseInjected: Boolean,
        val disguiseCoverage: Float,
        val oauthInjected: Boolean,
        val oauthProvider: String?,
        val diagnosticInjected: Boolean,
        val totalJsSize: Int
    ) {
        fun toLogString(): String {
            val parts = mutableListOf<String>()
            if (kernelInjected) parts.add("Kernel")
            if (disguiseInjected) parts.add("Disguise(${"%.0f".format(disguiseCoverage * 100)}%)")
            if (oauthInjected) parts.add("OAuth($oauthProvider)")
            if (diagnosticInjected) parts.add("Diag")
            return "Injected: [${parts.joinToString(" → ")}] ${totalJsSize}chars"
        }
    }
    
    /**
     * 在 onPageStarted 中调用 — 统一注入所有反检测层
     * 
     * @param webView WebView 实例
     * @param url 当前页面 URL
     * @param disguiseConfig 浏览器伪装配置 (null = 不启用高级伪装)
     * @param cachedDisguiseJs 预生成的伪装 JS (来自 WebViewManager 缓存)
     * @param enableDiagnostic 是否启用指纹诊断探针
     * @return 注入结果
     */
    fun injectOnPageStarted(
        webView: WebView,
        url: String?,
        disguiseConfig: BrowserDisguiseConfig?,
        cachedDisguiseJs: String?,
        enableDiagnostic: Boolean = false
    ): InjectionResult {
        var kernelInjected = false
        var disguiseInjected = false
        var oauthInjected = false
        var diagnosticInjected = false
        var oauthProvider: String? = null
        var totalJsSize = 0
        var disguiseCoverage = 0f
        
        // === 1. BrowserKernel (OAuth 页面 only) ===
        if (url != null && OAuthCompatEngine.isOAuthUrl(url)) {
            try {
                BrowserKernel.injectKernelJs(webView)
                kernelInjected = true
                AppLogger.d(TAG, "Layer1: BrowserKernel injected for OAuth: $url")
            } catch (e: Exception) {
                AppLogger.w(TAG, "Layer1: BrowserKernel injection failed", e)
            }
        }
        
        // === 2. BrowserDisguise (所有页面, if enabled) ===
        if (disguiseConfig?.enabled == true && !cachedDisguiseJs.isNullOrEmpty()) {
            try {
                webView.evaluateJavascript(cachedDisguiseJs, null)
                disguiseInjected = true
                disguiseCoverage = BrowserDisguiseConfig.calculateCoverage(disguiseConfig)
                totalJsSize += cachedDisguiseJs.length
                AppLogger.d(TAG, "Layer2: BrowserDisguise injected (${cachedDisguiseJs.length} chars)")
            } catch (e: Exception) {
                AppLogger.w(TAG, "Layer2: BrowserDisguise injection failed", e)
            }
        }
        
        // === 3. OAuthCompatEngine (OAuth 页面 only) ===
        if (url != null && OAuthCompatEngine.isOAuthUrl(url)) {
            try {
                OAuthCompatEngine.getAntiDetectionJs(url)?.let { js ->
                    webView.evaluateJavascript(js, null)
                    oauthInjected = true
                    oauthProvider = OAuthCompatEngine.getProviderType(url)?.name
                    totalJsSize += js.length
                    AppLogger.d(TAG, "Layer3: OAuth anti-detection for $oauthProvider")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Layer3: OAuth injection failed", e)
            }
        }
        
        // === 4. Diagnostic probe (debug only) ===
        if (enableDiagnostic) {
            try {
                val diagJs = FingerprintDiagnostic.generateProbeJs()
                webView.evaluateJavascript(diagJs, null)
                diagnosticInjected = true
                totalJsSize += diagJs.length
                AppLogger.d(TAG, "Layer4: Diagnostic probe injected")
            } catch (e: Exception) {
                AppLogger.w(TAG, "Layer4: Diagnostic probe failed", e)
            }
        }
        
        val result = InjectionResult(
            kernelInjected = kernelInjected,
            disguiseInjected = disguiseInjected,
            disguiseCoverage = disguiseCoverage,
            oauthInjected = oauthInjected,
            oauthProvider = oauthProvider,
            diagnosticInjected = diagnosticInjected,
            totalJsSize = totalJsSize
        )
        
        AppLogger.d(TAG, result.toLogString())
        return result
    }
    
    /**
     * 获取引擎状态摘要 (用于 UI 展示)
     */
    fun getEngineStatus(config: BrowserDisguiseConfig?): EngineStatus {
        val kernelAvailable = BrowserKernel.isAvailable()
        val disguiseEnabled = config?.enabled == true
        val coverage = if (disguiseEnabled) BrowserDisguiseConfig.calculateCoverage(config!!) else 0f
        val level = BrowserDisguiseConfig.getDisguiseLevel(coverage)
        
        return EngineStatus(
            kernelAvailable = kernelAvailable,
            disguiseEnabled = disguiseEnabled,
            disguiseCoverage = coverage,
            disguiseLevel = level,
            disguisePreset = config?.preset ?: BrowserDisguisePreset.OFF,
            activeVectorsCount = if (disguiseEnabled) countActiveVectors(config!!) else 0,
            totalVectorsCount = 28 // Updated total with new vectors
        )
    }
    
    private fun countActiveVectors(config: BrowserDisguiseConfig): Int {
        var c = 0
        if (config.removeXRequestedWith) c++
        if (config.sanitizeUserAgent) c++
        if (config.hideWebdriver) c++
        if (config.emulateWindowChrome) c++
        if (config.fakePlugins) c++
        if (config.fakeVendor) c++
        if (config.canvasNoise) c++
        if (config.webglSpoof) c++
        if (config.audioNoise) c++
        if (config.screenSpoof) c++
        if (config.clientRectsNoise) c++
        if (config.timezoneSpoof) c++
        if (config.languageSpoof) c++
        if (config.platformSpoof) c++
        if (config.hardwareConcurrencySpoof) c++
        if (config.deviceMemorySpoof) c++
        if (config.mediaDevicesSpoof) c++
        if (config.webrtcIpShield) c++
        if (config.fontEnumerationBlock) c++
        if (config.batteryShield) c++
        if (config.nativeToStringProtection) c++
        if (config.iframeDisguisePropagation) c++
        return c
    }
    
    data class EngineStatus(
        val kernelAvailable: Boolean,
        val disguiseEnabled: Boolean,
        val disguiseCoverage: Float,
        val disguiseLevel: String,
        val disguisePreset: BrowserDisguisePreset,
        val activeVectorsCount: Int,
        val totalVectorsCount: Int
    )
}

/**
 * 指纹诊断工具
 * 
 * 在页面中注入一个不可见的诊断探针，收集当前的浏览器指纹信息
 * 并通过 console.log 输出详细报告。
 * 
 * ## 使用场景
 * - 开发调试：确认伪装 JS 是否生效
 * - 覆盖率验证：对比注入前后的指纹差异
 * - 兼容性测试：识别网站使用的检测手段
 */
object FingerprintDiagnostic {
    
    /**
     * 生成指纹诊断探针 JS
     * 
     * 探针会在页面加载后 500ms 执行(确保伪装 JS 已生效)，
     * 收集 30+ 个指纹维度并输出结构化报告。
     */
    fun generateProbeJs(): String = """
(function(){
'use strict';
if(window.__wta_diag__)return;
window.__wta_diag__=true;

setTimeout(function(){
    var report={};
    
    // === Navigator ===
    try{
        report.navigator={
            userAgent:navigator.userAgent,
            platform:navigator.platform,
            vendor:navigator.vendor,
            language:navigator.language,
            languages:navigator.languages?Array.from(navigator.languages):[],
            hardwareConcurrency:navigator.hardwareConcurrency,
            deviceMemory:navigator.deviceMemory,
            webdriver:navigator.webdriver,
            maxTouchPoints:navigator.maxTouchPoints,
            plugins:navigator.plugins?navigator.plugins.length:0,
            mimeTypes:navigator.mimeTypes?navigator.mimeTypes.length:0,
            cookieEnabled:navigator.cookieEnabled,
            doNotTrack:navigator.doNotTrack,
            connection:navigator.connection?{
                effectiveType:navigator.connection.effectiveType,
                downlink:navigator.connection.downlink,
                rtt:navigator.connection.rtt,
                saveData:navigator.connection.saveData
            }:null
        };
    }catch(e){report.navigator={error:e.message}}
    
    // === Screen ===
    try{
        report.screen={
            width:screen.width,
            height:screen.height,
            availWidth:screen.availWidth,
            availHeight:screen.availHeight,
            colorDepth:screen.colorDepth,
            pixelDepth:screen.pixelDepth,
            devicePixelRatio:window.devicePixelRatio,
            outerWidth:window.outerWidth,
            outerHeight:window.outerHeight,
            innerWidth:window.innerWidth,
            innerHeight:window.innerHeight
        };
    }catch(e){report.screen={error:e.message}}
    
    // === Chrome Object ===
    try{
        report.chrome={
            exists:!!window.chrome,
            runtime:!!window.chrome?.runtime,
            loadTimes:typeof window.chrome?.loadTimes==='function',
            csi:typeof window.chrome?.csi==='function',
            app:!!window.chrome?.app
        };
    }catch(e){report.chrome={error:e.message}}
    
    // === WebGL ===
    try{
        var c=document.createElement('canvas');
        var gl=c.getContext('webgl')||c.getContext('experimental-webgl');
        if(gl){
            var ext=gl.getExtension('WEBGL_debug_renderer_info');
            report.webgl={
                vendor:ext?gl.getParameter(ext.UNMASKED_VENDOR_WEBGL):'N/A',
                renderer:ext?gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):'N/A',
                version:gl.getParameter(gl.VERSION),
                shadingLanguageVersion:gl.getParameter(gl.SHADING_LANGUAGE_VERSION)
            };
        }else{report.webgl={available:false}}
    }catch(e){report.webgl={error:e.message}}
    
    // === Canvas Fingerprint ===
    try{
        var c2=document.createElement('canvas');
        c2.width=200;c2.height=50;
        var ctx=c2.getContext('2d');
        ctx.textBaseline='top';
        ctx.font='14px Arial';
        ctx.fillStyle='#f60';
        ctx.fillRect(125,1,62,20);
        ctx.fillStyle='#069';
        ctx.fillText('Fingerprint',2,15);
        report.canvas={
            hash:c2.toDataURL().substring(0,60)+'...',
            length:c2.toDataURL().length
        };
    }catch(e){report.canvas={error:e.message}}
    
    // === Timezone ===
    try{
        report.timezone={
            offset:new Date().getTimezoneOffset(),
            resolved:Intl.DateTimeFormat().resolvedOptions().timeZone,
            locale:Intl.DateTimeFormat().resolvedOptions().locale
        };
    }catch(e){report.timezone={error:e.message}}
    
    // === Battery ===
    try{
        if(navigator.getBattery){
            navigator.getBattery().then(function(b){
                report.battery={charging:b.charging,level:b.level,chargingTime:b.chargingTime,dischargingTime:b.dischargingTime};
                console.log('[WTA-DIAG] Battery:',JSON.stringify(report.battery));
            });
        }else{report.battery={available:false}}
    }catch(e){report.battery={error:e.message}}
    
    // === toString Protection Check ===
    try{
        report.toStringCheck={
            navigatorWebdriver:Object.getOwnPropertyDescriptor(navigator.__proto__,'webdriver')?.get?.toString()?.includes('[native code]'),
            functionToString:Function.prototype.toString.toString().includes('[native code]')
        };
    }catch(e){report.toStringCheck={error:e.message}}
    
    // === Automation Flags ===
    try{
        report.automationFlags={
            webdriver:navigator.webdriver,
            __selenium:!!window.__selenium_unwrapped,
            __webdriver_evaluate:!!window.__webdriver_evaluate,
            __webdriver_script_fn:!!window.__webdriver_script_function,
            __wta_browser_disguise:!!window.__wta_browser_disguise__,
            domAutomation:!!window.domAutomation,
            domAutomationController:!!window.domAutomationController
        };
    }catch(e){report.automationFlags={error:e.message}}
    
    // === ClientRects ===
    try{
        var div=document.createElement('div');
        div.innerHTML='<b>X</b>';
        div.style.position='absolute';div.style.left='-9999px';
        document.body.appendChild(div);
        var r1=div.getBoundingClientRect();
        var r2=div.getBoundingClientRect();
        report.clientRects={
            consistent:r1.x===r2.x&&r1.width===r2.width,
            x1:r1.x,x2:r2.x,
            w1:r1.width,w2:r2.width
        };
        document.body.removeChild(div);
    }catch(e){report.clientRects={error:e.message}}
    
    // === Performance Timing ===
    try{
        var t1=performance.now();
        var t2=performance.now();
        report.performance={
            nowPrecision:t2-t1,
            timing:!!performance.timing
        };
    }catch(e){report.performance={error:e.message}}
    
    // === Storage ===
    try{
        report.storage={
            localStorage:!!window.localStorage,
            sessionStorage:!!window.sessionStorage,
            indexedDB:!!window.indexedDB
        };
        if(navigator.storage&&navigator.storage.estimate){
            navigator.storage.estimate().then(function(e){
                report.storage.quota=e.quota;
                report.storage.usage=e.usage;
            });
        }
    }catch(e){report.storage={error:e.message}}
    
    // Output
    console.log('%c[WTA Fingerprint Diagnostic Report]','color:#e91e63;font-weight:bold;font-size:14px');
    console.log('%c─────────────────────────────────','color:#9c27b0');
    Object.keys(report).forEach(function(k){
        console.log('%c'+k+':','color:#2196f3;font-weight:bold',JSON.stringify(report[k],null,2));
    });
    console.log('%c─────────────────────────────────','color:#9c27b0');
    
    // Also store for programmatic access
    window.__wta_fingerprint_report__=report;
    
},500);
})();
"""

    /**
     * 生成完整的指纹测试 HTML 页面
     * 
     * 返回一个自包含的 HTML 页面，显示所有指纹向量的当前值。
     * 用于开发者调试和伪装效果验证。
     */
    fun generateTestPageHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>🕶️ Browser Disguise Diagnostic</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',system-ui,sans-serif;background:#0a0a1a;color:#e0e0e8;padding:16px}
h1{text-align:center;font-size:20px;padding:16px;background:linear-gradient(135deg,#1a1a2e,#16213e);border-radius:12px;margin-bottom:16px;border:1px solid #2a2a4a}
h1 span{font-size:28px}
.section{background:#12122a;border-radius:10px;padding:14px;margin-bottom:12px;border:1px solid #1e1e3e}
.section h2{color:#bb86fc;font-size:14px;margin-bottom:10px;display:flex;align-items:center;gap:8px}
.row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #1a1a30;font-size:13px}
.row:last-child{border-bottom:none}
.key{color:#888;flex-shrink:0}
.val{color:#4fd1c5;text-align:right;word-break:break-all;max-width:60%}
.val.warn{color:#ff6b6b}
.val.pass{color:#4caf50}
.badge{display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:bold}
.badge.active{background:#4caf5022;color:#4caf50;border:1px solid #4caf5044}
.badge.inactive{background:#ff6b6b22;color:#ff6b6b;border:1px solid #ff6b6b44}
#status{text-align:center;padding:12px;border-radius:8px;margin-bottom:12px;font-weight:bold}
#status.protected{background:linear-gradient(135deg,#4caf5015,#4caf5005);border:1px solid #4caf5044;color:#4caf50}
#status.exposed{background:linear-gradient(135deg,#ff6b6b15,#ff6b6b05);border:1px solid #ff6b6b44;color:#ff6b6b}
</style>
</head>
<body>
<h1><span>🕶️</span> Browser Disguise Diagnostic</h1>
<div id="status">Analyzing...</div>
<div id="report"></div>
<script>
(function(){
var r=document.getElementById('report');
var s=document.getElementById('status');
var checks={pass:0,fail:0};

function addSection(title,icon,rows){
    var sec=document.createElement('div');
    sec.className='section';
    sec.innerHTML='<h2>'+icon+' '+title+'</h2>';
    rows.forEach(function(row){
        var d=document.createElement('div');d.className='row';
        var valClass='val';
        if(row[2]==='pass')valClass+=' pass';
        if(row[2]==='warn')valClass+=' warn';
        if(row[2]==='pass')checks.pass++;
        if(row[2]==='warn')checks.fail++;
        d.innerHTML='<span class="key">'+row[0]+'</span><span class="'+valClass+'">'+row[1]+'</span>';
        sec.appendChild(d);
    });
    r.appendChild(sec);
}

// 1. WebView Detection
var wvRows=[];
wvRows.push(['navigator.webdriver',navigator.webdriver?'true ⚠️':'false ✓',navigator.webdriver?'warn':'pass']);
wvRows.push(['window.chrome',window.chrome?'Present ✓':'Missing ⚠️',window.chrome?'pass':'warn']);
wvRows.push(['chrome.runtime',window.chrome&&window.chrome.runtime?'Present ✓':'Missing ⚠️',window.chrome&&window.chrome.runtime?'pass':'warn']);
wvRows.push(['chrome.loadTimes',typeof window.chrome?.loadTimes==='function'?'Function ✓':'Missing ⚠️',typeof window.chrome?.loadTimes==='function'?'pass':'warn']);
wvRows.push(['navigator.plugins',navigator.plugins.length+' plugins',navigator.plugins.length>=3?'pass':'warn']);
wvRows.push(['navigator.vendor',navigator.vendor,navigator.vendor==='Google Inc.'?'pass':'warn']);
addSection('WebView Detection','🛡️',wvRows);

// 2. Navigator
addSection('Navigator Properties','🧭',[
    ['userAgent',navigator.userAgent.substring(0,60)+'...'],
    ['platform',navigator.platform],
    ['language',navigator.language],
    ['languages','['+navigator.languages?.join(', ')+']'],
    ['hardwareConcurrency',navigator.hardwareConcurrency],
    ['deviceMemory',navigator.deviceMemory||'N/A'],
    ['maxTouchPoints',navigator.maxTouchPoints],
    ['cookieEnabled',navigator.cookieEnabled]
]);

// 3. Screen
addSection('Screen','📱',[
    ['width × height',screen.width+' × '+screen.height],
    ['availWidth × availHeight',screen.availWidth+' × '+screen.availHeight],
    ['colorDepth',screen.colorDepth],
    ['devicePixelRatio',window.devicePixelRatio],
    ['outerWidth × outerHeight',window.outerWidth+' × '+window.outerHeight]
]);

// 4. WebGL
try{
    var c=document.createElement('canvas');
    var gl=c.getContext('webgl');
    var ext=gl?gl.getExtension('WEBGL_debug_renderer_info'):null;
    addSection('WebGL','🎮',[
        ['Vendor',ext?gl.getParameter(ext.UNMASKED_VENDOR_WEBGL):'N/A'],
        ['Renderer',ext?gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):'N/A'],
        ['Version',gl?gl.getParameter(gl.VERSION):'N/A']
    ]);
}catch(e){addSection('WebGL','🎮',[['Error',e.message,'warn']])}

// 5. Canvas
try{
    var c2=document.createElement('canvas');c2.width=200;c2.height=50;
    var ctx=c2.getContext('2d');
    ctx.font='14px Arial';ctx.fillStyle='#f60';ctx.fillRect(125,1,62,20);
    ctx.fillStyle='#069';ctx.fillText('FP Test',2,15);
    var d1=c2.toDataURL();
    ctx.fillText('FP Test',2,15);
    var d2=c2.toDataURL();
    var noiseActive=d1!==d2;
    addSection('Canvas Fingerprint','🎨',[
        ['Hash (first 50)',d1.substring(22,72)+'...'],
        ['Length',d1.length+' chars'],
        ['Noise Active',noiseActive?'Yes ✓ (different each read)':'No (identical)',noiseActive?'pass':'']
    ]);
}catch(e){}

// 6. Timezone
addSection('Timezone','🕐',[
    ['getTimezoneOffset()',new Date().getTimezoneOffset()+' min'],
    ['Intl timezone',Intl.DateTimeFormat().resolvedOptions().timeZone],
    ['Intl locale',Intl.DateTimeFormat().resolvedOptions().locale]
]);

// 7. Protection Status
try{
    var toStr=Function.prototype.toString.toString();
    addSection('Protection','🔐',[
        ['toString protection',toStr.includes('[native code]')?'Active ✓':'Exposed ⚠️',toStr.includes('[native code]')?'pass':'warn'],
        ['__wta_browser_disguise__',window.__wta_browser_disguise__?'Injected ✓':'Not active',window.__wta_browser_disguise__?'pass':''],
        ['selenium flags',window.__selenium_unwrapped?'Present ⚠️':'Clean ✓',window.__selenium_unwrapped?'warn':'pass']
    ]);
}catch(e){}

// Status summary
var total=checks.pass+checks.fail;
if(checks.fail===0){
    s.className='protected';
    s.textContent='🛡️ ALL CHECKS PASSED ('+checks.pass+'/'+total+') — WebView Identity Hidden';
}else{
    s.className='exposed';
    s.textContent='⚠️ '+checks.fail+' EXPOSED / '+total+' — Disguise incomplete';
}
})();
</script>
</body>
</html>
"""
}
