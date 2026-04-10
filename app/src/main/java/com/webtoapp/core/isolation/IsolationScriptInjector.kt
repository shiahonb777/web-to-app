package com.webtoapp.core.isolation

/**
 * 隔离脚本注入器 — 综合反指纹 JavaScript 引擎
 *
 * 覆盖所有主要指纹向量:
 * - Navigator 属性 + navigator.userAgentData (Client Hints)
 * - 确定性 Canvas 噪声（seed-based, 会话内一致）
 * - WebGL vendor/renderer 伪装
 * - 确定性 AudioContext 噪声
 * - WebRTC IP 泄漏防护（不破坏功能）
 * - 完整字体指纹防护（FontFace API + 度量归一化）
 * - 屏幕/窗口尺寸伪装
 * - 时区伪装（Intl + Date）
 * - ClientRects / DOMRect 噪声
 * - performance.now() 精度降低
 * - navigator.plugins / mimeTypes 伪装
 * - Battery API 防护
 * - navigator.connection 伪装
 * - MediaDevices 枚举防护
 * - Storage 估计 API 归一化
 * - history.length 固定
 * - Permissions API 归一化
 */
object IsolationScriptInjector {

    /**
     * 生成完整的隔离脚本
     */
    fun generateIsolationScript(
        config: IsolationConfig,
        fingerprint: GeneratedFingerprint
    ): String {
        if (!config.enabled) return ""

        val scripts = mutableListOf<String>()

        // Seeded PRNG (used by canvas/audio/rect noise for deterministic output)
        scripts.add(generateSeededPrng(fingerprint.canvasNoiseSeed))

        // Navigator properties + Client Hints + userAgentData
        if (config.fingerprintConfig.randomize) {
            scripts.add(generateNavigatorScript(config, fingerprint))
        }

        // Canvas fingerprint — deterministic noise
        if (config.protectCanvas) {
            scripts.add(generateCanvasProtectionScript(fingerprint))
        }

        // WebGL vendor/renderer
        if (config.protectWebGL) {
            scripts.add(generateWebGLProtectionScript(fingerprint))
        }

        // AudioContext — deterministic noise
        if (config.protectAudio) {
            scripts.add(generateAudioProtectionScript(fingerprint))
        }

        // WebRTC — mask local IPs without breaking functionality
        if (config.blockWebRTC) {
            scripts.add(generateWebRTCProtectionScript())
        }

        // Font fingerprint — comprehensive
        if (config.protectFonts) {
            scripts.add(generateFontProtectionScript())
        }

        // Screen/window dimensions
        if (config.spoofScreen) {
            scripts.add(generateScreenSpoofScript(
                config.customScreenWidth ?: fingerprint.screenWidth,
                config.customScreenHeight ?: fingerprint.screenHeight,
                fingerprint.colorDepth
            ))
        }

        // Timezone
        if (config.spoofTimezone) {
            scripts.add(generateTimezoneSpoofScript(config.customTimezone ?: fingerprint.timezone))
        }

        // ClientRects noise
        if (config.protectCanvas) {
            scripts.add(generateClientRectsNoiseScript())
        }

        // performance.now() precision reduction
        scripts.add(generatePerformanceTimingScript())

        // Plugins / mimeTypes
        if (config.fingerprintConfig.randomize) {
            scripts.add(generatePluginsSpoofScript(fingerprint))
        }

        // Battery API
        scripts.add(generateBatteryProtectionScript())

        // navigator.connection
        scripts.add(generateConnectionSpoofScript())

        // MediaDevices enumeration
        scripts.add(generateMediaDevicesProtectionScript())

        // Storage estimation
        scripts.add(generateStorageEstimateScript())

        // history.length
        scripts.add(generateHistoryProtectionScript())

        // Permissions API
        scripts.add(generatePermissionsProtectionScript())

        return """
            (function() {
                'use strict';
                if(window.__wta_isolation_v2__)return;
                window.__wta_isolation_v2__=true;
                try {
                    ${scripts.joinToString("\n\n")}
                } catch(e) {
                    console.error('[WebToApp] isolation script error:', e);
                }
            })();
        """.trimIndent()
    }

    // ==================== Seeded PRNG ====================
    private fun generateSeededPrng(seed: Long): String {
        return """
            // Seeded PRNG (mulberry32) — deterministic noise per session
            var __wta_seed__ = ${seed.toUInt()};
            function __wta_prng__() {
                __wta_seed__ |= 0; __wta_seed__ = __wta_seed__ + 0x6D2B79F5 | 0;
                var t = Math.imul(__wta_seed__ ^ __wta_seed__ >>> 15, 1 | __wta_seed__);
                t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
                return ((t ^ t >>> 14) >>> 0) / 4294967296;
            }
        """.trimIndent()
    }

    // ==================== Navigator + Client Hints ====================
    private fun generateNavigatorScript(config: IsolationConfig, fp: GeneratedFingerprint): String {
        val userAgent = config.fingerprintConfig.customUserAgent ?: fp.userAgent
        val platform = config.fingerprintConfig.platform ?: fp.platform
        val vendor = config.fingerprintConfig.vendor ?: fp.vendor
        val hardwareConcurrency = config.fingerprintConfig.hardwareConcurrency ?: fp.hardwareConcurrency
        val deviceMemory = config.fingerprintConfig.deviceMemory ?: fp.deviceMemory
        val primaryLang = fp.language.split(",").first().split(";").first().trim()
        val langArray = fp.language.split(",").map { it.split(";").first().trim() }

        val clientHintsJs = if (fp.chUa.isNotEmpty()) {
            """
            // navigator.userAgentData (Client Hints API)
            var uadBrands = [${fp.chUa.split(", ").joinToString(",") { brand ->
                val parts = brand.split(";v=")
                if (parts.size == 2) "{brand:${parts[0]},version:${parts[1]}}" else ""
            }}];
            var uadObj = {
                brands: uadBrands,
                mobile: false,
                platform: ${fp.chUaPlatform},
                getHighEntropyValues: function(hints) {
                    return Promise.resolve({
                        brands: uadBrands,
                        mobile: false,
                        platform: ${fp.chUaPlatform},
                        platformVersion: ${fp.chUaPlatformVersion},
                        architecture: ${fp.chUaArch},
                        bitness: ${fp.chUaBitness},
                        model: ${fp.chUaModel},
                        uaFullVersion: ${fp.chUaFullVersion},
                        fullVersionList: uadBrands
                    });
                },
                toJSON: function() {
                    return {brands:uadBrands,mobile:false,platform:${fp.chUaPlatform}};
                }
            };
            try{Object.defineProperty(navigator,'userAgentData',{get:function(){return uadObj;},configurable:true});}catch(e){/* expected */}
            """
        } else {
            """
            // Firefox/Safari: no userAgentData — delete it if present
            try{Object.defineProperty(navigator,'userAgentData',{get:function(){return undefined;},configurable:true});}catch(e){/* expected */}
            """
        }

        return """
            // Navigator property spoofing
            var navProps = {
                userAgent: '$userAgent',
                appVersion: '${fp.appVersion}',
                platform: '$platform',
                vendor: '$vendor',
                hardwareConcurrency: $hardwareConcurrency,
                deviceMemory: $deviceMemory,
                language: '$primaryLang',
                languages: Object.freeze(${langArray.joinToString(",", "[", "]") { "'$it'" }}),
                maxTouchPoints: ${fp.maxTouchPoints},
                webdriver: false,
                doNotTrack: '1',
                pdfViewerEnabled: true,
                cookieEnabled: true
            };
            Object.keys(navProps).forEach(function(p){
                try{Object.defineProperty(navigator,p,{get:function(){return navProps[p];},configurable:true});}catch(e){/* expected */}
            });

            // Remove automation markers
            try{
                delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;
                delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;
                delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;
                delete window.__webdriver_evaluate;
                delete window.__selenium_evaluate;
                delete window.__fxdriver_evaluate;
                delete window.__driver_evaluate;
                delete window.__webdriver_unwrap;
                delete window.__selenium_unwrap;
                delete window.__fxdriver_unwrap;
                delete document.__webdriver_evaluate;
                delete document.__selenium_evaluate;
                delete document.__fxdriver_evaluate;
            }catch(e){/* expected */}

            $clientHintsJs
        """.trimIndent()
    }

    // ==================== Canvas — deterministic noise ====================
    private fun generateCanvasProtectionScript(fp: GeneratedFingerprint): String {
        return """
            // Canvas fingerprint — deterministic seed-based noise
            var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
            var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
            var origToBlob = HTMLCanvasElement.prototype.toBlob;

            function __wta_canvas_noise__(data) {
                // Save/restore PRNG state for determinism
                var saved = __wta_seed__;
                __wta_seed__ = ${fp.canvasNoiseSeed.toUInt()};
                for (var i = 0; i < data.length; i += 4) {
                    var r = __wta_prng__();
                    if (r < 0.1) {
                        var ch = (i % 3);
                        data[i + ch] = data[i + ch] ^ 1;
                    }
                }
                __wta_seed__ = saved;
            }

            HTMLCanvasElement.prototype.toDataURL = function() {
                try {
                    var ctx = this.getContext('2d');
                    if (ctx && this.width > 0 && this.height > 0 && this.width < 2000 && this.height < 2000) {
                        var imgData = origGetImageData.call(ctx, 0, 0, this.width, this.height);
                        __wta_canvas_noise__(imgData.data);
                        ctx.putImageData(imgData, 0, 0);
                    }
                } catch(e){ /* canvas noise injection failed */ }
                return origToDataURL.apply(this, arguments);
            };

            HTMLCanvasElement.prototype.toBlob = function() {
                try {
                    var ctx = this.getContext('2d');
                    if (ctx && this.width > 0 && this.height > 0 && this.width < 2000 && this.height < 2000) {
                        var imgData = origGetImageData.call(ctx, 0, 0, this.width, this.height);
                        __wta_canvas_noise__(imgData.data);
                        ctx.putImageData(imgData, 0, 0);
                    }
                } catch(e){ /* canvas noise injection failed */ }
                return origToBlob.apply(this, arguments);
            };

            CanvasRenderingContext2D.prototype.getImageData = function() {
                var imgData = origGetImageData.apply(this, arguments);
                try { __wta_canvas_noise__(imgData.data); } catch(e){ /* canvas noise failed */ }
                return imgData;
            };
        """.trimIndent()
    }

    // ==================== WebGL ====================
    private fun generateWebGLProtectionScript(fp: GeneratedFingerprint): String {
        return """
            // WebGL fingerprint spoofing
            var glParamHandler = {
                apply: function(target, thisArg, args) {
                    var p = args[0];
                    if (p === 37445) return '${fp.webglVendor}';
                    if (p === 37446) return '${fp.webglRenderer}';
                    return target.apply(thisArg, args);
                }
            };
            var origGlGetParam = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = new Proxy(origGlGetParam, glParamHandler);
            if (typeof WebGL2RenderingContext !== 'undefined') {
                var origGl2GetParam = WebGL2RenderingContext.prototype.getParameter;
                WebGL2RenderingContext.prototype.getParameter = new Proxy(origGl2GetParam, glParamHandler);
            }

            // getExtension — ensure WEBGL_debug_renderer_info returns correct constants
            var origGetExt = WebGLRenderingContext.prototype.getExtension;
            WebGLRenderingContext.prototype.getExtension = function(name) {
                if (name === 'WEBGL_debug_renderer_info') {
                    return { UNMASKED_VENDOR_WEBGL: 37445, UNMASKED_RENDERER_WEBGL: 37446 };
                }
                return origGetExt.apply(this, arguments);
            };
            if (typeof WebGL2RenderingContext !== 'undefined') {
                var origGetExt2 = WebGL2RenderingContext.prototype.getExtension;
                WebGL2RenderingContext.prototype.getExtension = function(name) {
                    if (name === 'WEBGL_debug_renderer_info') {
                        return { UNMASKED_VENDOR_WEBGL: 37445, UNMASKED_RENDERER_WEBGL: 37446 };
                    }
                    return origGetExt2.apply(this, arguments);
                };
            }
        """.trimIndent()
    }

    // ==================== AudioContext — deterministic noise ====================
    private fun generateAudioProtectionScript(fp: GeneratedFingerprint): String {
        return """
            // AudioContext fingerprint — deterministic noise
            var origGetFloatFreq = AnalyserNode.prototype.getFloatFrequencyData;
            var origGetByteFreq = AnalyserNode.prototype.getByteFrequencyData;
            var origGetFloatTime = AnalyserNode.prototype.getFloatTimeDomainData;
            var origGetByteTime = AnalyserNode.prototype.getByteTimeDomainData;

            function __wta_audio_noise_float__(arr) {
                var saved = __wta_seed__;
                __wta_seed__ = ${fp.audioNoiseSeed.toUInt()};
                for (var i = 0; i < arr.length; i++) {
                    arr[i] += (__wta_prng__() - 0.5) * 0.0001;
                }
                __wta_seed__ = saved;
            }
            function __wta_audio_noise_byte__(arr) {
                var saved = __wta_seed__;
                __wta_seed__ = ${fp.audioNoiseSeed.toUInt()};
                for (var i = 0; i < arr.length; i++) {
                    if (__wta_prng__() < 0.05) arr[i] = (arr[i] + 1) & 0xFF;
                }
                __wta_seed__ = saved;
            }

            AnalyserNode.prototype.getFloatFrequencyData = function(a) {
                origGetFloatFreq.call(this, a); __wta_audio_noise_float__(a);
            };
            AnalyserNode.prototype.getByteFrequencyData = function(a) {
                origGetByteFreq.call(this, a); __wta_audio_noise_byte__(a);
            };
            AnalyserNode.prototype.getFloatTimeDomainData = function(a) {
                origGetFloatTime.call(this, a); __wta_audio_noise_float__(a);
            };
            AnalyserNode.prototype.getByteTimeDomainData = function(a) {
                origGetByteTime.call(this, a); __wta_audio_noise_byte__(a);
            };

            // Spoof AudioContext.destination.channelCount
            try {
                var origACtx = window.AudioContext || window.webkitAudioContext;
                if (origACtx) {
                    var origCreateOsc = origACtx.prototype.createOscillator;
                    var origCreateDynComp = origACtx.prototype.createDynamicsCompressor;
                    // Wrap createOscillator to add noise to oscillator output
                    origACtx.prototype.createOscillator = function() {
                        var osc = origCreateOsc.apply(this, arguments);
                        // Slightly vary frequency to alter audio fingerprint
                        var origFreq = osc.frequency.value;
                        osc.frequency.value = origFreq + (__wta_prng__() - 0.5) * 0.01;
                        return osc;
                    };
                }
            } catch(e){ /* AudioContext spoofing failed */ }
        """.trimIndent()
    }

    // ==================== WebRTC — mask local IPs without breaking ====================
    private fun generateWebRTCProtectionScript(): String {
        return """
            // WebRTC IP leak protection (keeps functionality, masks local IPs)
            if (typeof RTCPeerConnection !== 'undefined') {
                var OrigRTC = RTCPeerConnection;

                var WtaRTC = function(config, constraints) {
                    // Keep TURN servers (relay), remove STUN (IP discovery)
                    if (config && config.iceServers) {
                        config.iceServers = config.iceServers.filter(function(s) {
                            var urls = s.urls || s.url || '';
                            if (typeof urls === 'string') urls = [urls];
                            return urls.some(function(u) { return u.indexOf('turn:') === 0 || u.indexOf('turns:') === 0; });
                        });
                    }
                    var pc = new OrigRTC(config, constraints);

                    // Filter ICE candidates — remove host candidates (local IPs)
                    var origAddEvent = pc.addEventListener.bind(pc);
                    pc.addEventListener = function(type, fn, opts) {
                        if (type === 'icecandidate') {
                            var wrapped = function(e) {
                                if (e.candidate && e.candidate.candidate) {
                                    var c = e.candidate.candidate;
                                    // Block host candidates (contain local IP)
                                    if (c.indexOf('typ host') !== -1) {
                                        // Fire event with null candidate (empty)
                                        var fakeEvt = new Event('icecandidate');
                                        fakeEvt.candidate = null;
                                        fn(fakeEvt);
                                        return;
                                    }
                                }
                                fn(e);
                            };
                            return origAddEvent(type, wrapped, opts);
                        }
                        return origAddEvent(type, fn, opts);
                    };

                    // Also filter onicecandidate setter
                    var origOnIce = Object.getOwnPropertyDescriptor(OrigRTC.prototype, 'onicecandidate');
                    if (origOnIce && origOnIce.set) {
                        Object.defineProperty(pc, 'onicecandidate', {
                            set: function(fn) {
                                if (typeof fn !== 'function') { origOnIce.set.call(pc, fn); return; }
                                origOnIce.set.call(pc, function(e) {
                                    if (e.candidate && e.candidate.candidate && e.candidate.candidate.indexOf('typ host') !== -1) {
                                        var fakeEvt = new Event('icecandidate');
                                        fakeEvt.candidate = null;
                                        fn(fakeEvt);
                                        return;
                                    }
                                    fn(e);
                                });
                            },
                            get: function() { return origOnIce.get ? origOnIce.get.call(pc) : undefined; },
                            configurable: true
                        });
                    }

                    return pc;
                };
                WtaRTC.prototype = OrigRTC.prototype;
                WtaRTC.generateCertificate = OrigRTC.generateCertificate;
                window.RTCPeerConnection = WtaRTC;
                if (window.webkitRTCPeerConnection) window.webkitRTCPeerConnection = WtaRTC;
            }
        """.trimIndent()
    }

    // ==================== Font fingerprint — comprehensive ====================
    private fun generateFontProtectionScript(): String {
        return """
            // Font fingerprint protection — normalize metrics + FontFace API
            var commonFonts = new Set([
                'Arial','Arial Black','Comic Sans MS','Courier New','Georgia',
                'Impact','Times New Roman','Trebuchet MS','Verdana','Helvetica',
                'Lucida Console','Palatino Linotype','Tahoma','Segoe UI',
                'Microsoft YaHei','SimSun','SimHei','PingFang SC','Hiragino Sans GB'
            ]);

            // 1. Override document.fonts.check() — always return true for common, false for others
            if (document.fonts && document.fonts.check) {
                var origFontsCheck = document.fonts.check.bind(document.fonts);
                document.fonts.check = function(font, text) {
                    // Extract font family name
                    var match = font.match(/['"](.*?)['"]/);
                    if (!match) match = font.match(/\d+(?:px|pt|em|rem)\s+(.*)/);
                    var family = match ? match[1].trim() : font;
                    if (commonFonts.has(family)) return true;
                    // For non-common fonts, randomize to hide real set
                    return __wta_prng__() > 0.5;
                };
            }

            // 2. Normalize font measurement probes
            // Font detection works by creating an element with a specific font, measuring width/height,
            // and comparing to a fallback font. We add small deterministic noise to defeat exact comparison.
            var origGetBCR = Element.prototype.getBoundingClientRect;
            var origOffsetW = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'offsetWidth');
            var origOffsetH = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'offsetHeight');

            function isFontProbe(el) {
                if (!el || !el.style) return false;
                var s = el.style;
                return (s.position === 'absolute' && (s.left === '-9999px' || s.top === '-9999px' || s.visibility === 'hidden')) ||
                       (el.parentNode && el.parentNode.style && el.parentNode.style.position === 'absolute' && el.parentNode.style.left === '-9999px');
            }

            if (origOffsetW && origOffsetW.get) {
                Object.defineProperty(HTMLElement.prototype, 'offsetWidth', {
                    get: function() {
                        var w = origOffsetW.get.call(this);
                        if (isFontProbe(this)) return w + ((__wta_prng__() < 0.3) ? 1 : 0);
                        return w;
                    }, configurable: true
                });
            }
            if (origOffsetH && origOffsetH.get) {
                Object.defineProperty(HTMLElement.prototype, 'offsetHeight', {
                    get: function() {
                        var h = origOffsetH.get.call(this);
                        if (isFontProbe(this)) return h + ((__wta_prng__() < 0.3) ? 1 : 0);
                        return h;
                    }, configurable: true
                });
            }
        """.trimIndent()
    }

    // ==================== Screen/window dimensions ====================
    private fun generateScreenSpoofScript(width: Int, height: Int, colorDepth: Int): String {
        val availHeight = height - 40
        val innerHeight = height - 100
        return """
            // Screen/window dimension spoofing
            var screenProps = {
                width: $width, height: $height,
                availWidth: $width, availHeight: $availHeight,
                colorDepth: $colorDepth, pixelDepth: $colorDepth
            };
            Object.keys(screenProps).forEach(function(p){
                try{Object.defineProperty(screen,p,{get:function(){return screenProps[p];},configurable:true});}catch(e){/* expected */}
            });
            try{Object.defineProperty(window,'devicePixelRatio',{get:function(){return 1;},configurable:true});}catch(e){/* expected */}
            try{
                Object.defineProperty(window,'outerWidth',{get:function(){return $width;},configurable:true});
                Object.defineProperty(window,'outerHeight',{get:function(){return $height;},configurable:true});
            }catch(e){/* expected */}
        """.trimIndent()
    }

    // ==================== Timezone ====================
    private fun generateTimezoneSpoofScript(timezone: String): String {
        return """
            // Timezone spoofing
            var origDTF = Intl.DateTimeFormat;
            var WtaDTF = function(locales, options) {
                options = Object.assign({}, options || {});
                options.timeZone = '$timezone';
                return new origDTF(locales, options);
            };
            WtaDTF.prototype = origDTF.prototype;
            WtaDTF.supportedLocalesOf = origDTF.supportedLocalesOf;
            Intl.DateTimeFormat = WtaDTF;

            // Timezone offset map
            var tzOffsets = {
                'Asia/Shanghai':-480,'Asia/Tokyo':-540,'Asia/Seoul':-540,
                'Asia/Singapore':-480,'Asia/Hong_Kong':-480,'Asia/Taipei':-480,
                'Asia/Kolkata':-330,
                'America/New_York':300,'America/Chicago':360,'America/Denver':420,
                'America/Los_Angeles':480,'America/Toronto':300,'America/Sao_Paulo':180,
                'Europe/London':0,'Europe/Paris':-60,'Europe/Berlin':-60,'Europe/Moscow':-180,
                'Australia/Sydney':-660,'Pacific/Auckland':-780
            };
            var tzOff = tzOffsets['$timezone'];
            if (tzOff !== undefined) {
                Date.prototype.getTimezoneOffset = function() { return tzOff; };
            }
        """.trimIndent()
    }

    // ==================== ClientRects noise ====================
    private fun generateClientRectsNoiseScript(): String {
        return """
            // ClientRects / DOMRect noise — defeats rect-based fingerprinting
            var origGetBCR = Element.prototype.getBoundingClientRect;
            Element.prototype.getBoundingClientRect = function() {
                var r = origGetBCR.call(this);
                var n = (__wta_prng__() - 0.5) * 0.5;
                return new DOMRect(r.x + n, r.y + n, r.width + n, r.height + n);
            };
            var origGetCR = Element.prototype.getClientRects;
            Element.prototype.getClientRects = function() {
                var rects = origGetCR.call(this);
                var result = [];
                for (var i = 0; i < rects.length; i++) {
                    var r = rects[i];
                    var n = (__wta_prng__() - 0.5) * 0.5;
                    result.push(new DOMRect(r.x + n, r.y + n, r.width + n, r.height + n));
                }
                // Return array-like with item() method
                result.item = function(idx) { return result[idx] || null; };
                return result;
            };
        """.trimIndent()
    }

    // ==================== performance.now() precision reduction ====================
    private fun generatePerformanceTimingScript(): String {
        return """
            // performance.now() — reduce precision to 100μs to defeat timing attacks
            var origPerfNow = performance.now.bind(performance);
            performance.now = function() {
                return Math.round(origPerfNow() * 10) / 10;
            };
        """.trimIndent()
    }

    // ==================== Plugins / mimeTypes ====================
    private fun generatePluginsSpoofScript(fp: GeneratedFingerprint): String {
        val isChromium = fp.browserType == "CHROME" || fp.browserType == "EDGE"
        return if (isChromium) {
            """
            // Plugins — Chromium default (PDF Viewer + Chrome PDF Viewer)
            try {
                var fakePlugins = [
                    {name:'PDF Viewer',filename:'internal-pdf-viewer',description:'Portable Document Format',length:1,
                     0:{type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'}},
                    {name:'Chrome PDF Viewer',filename:'internal-pdf-viewer',description:'Portable Document Format',length:1,
                     0:{type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'}},
                    {name:'Chromium PDF Viewer',filename:'internal-pdf-viewer',description:'Portable Document Format',length:1,
                     0:{type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'}},
                    {name:'Microsoft Edge PDF Viewer',filename:'internal-pdf-viewer',description:'Portable Document Format',length:1,
                     0:{type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'}},
                    {name:'WebKit built-in PDF',filename:'internal-pdf-viewer',description:'Portable Document Format',length:1,
                     0:{type:'application/pdf',suffixes:'pdf',description:'Portable Document Format'}}
                ];
                fakePlugins.item = function(i){return fakePlugins[i]||null;};
                fakePlugins.namedItem = function(n){return fakePlugins.find(function(p){return p.name===n;})||null;};
                fakePlugins.refresh = function(){};
                Object.defineProperty(navigator,'plugins',{get:function(){return fakePlugins;},configurable:true});
                // mimeTypes
                var fakeMime = [{type:'application/pdf',suffixes:'pdf',description:'Portable Document Format',enabledPlugin:fakePlugins[0]}];
                fakeMime.item = function(i){return fakeMime[i]||null;};
                fakeMime.namedItem = function(n){return fakeMime.find(function(m){return m.type===n;})||null;};
                Object.defineProperty(navigator,'mimeTypes',{get:function(){return fakeMime;},configurable:true});
            }catch(e){/* expected */}
            """.trimIndent()
        } else {
            """
            // Firefox/Safari: empty plugins (real behavior)
            try {
                Object.defineProperty(navigator,'plugins',{get:function(){var a=[];a.item=function(){return null;};a.namedItem=function(){return null;};a.refresh=function(){};return a;},configurable:true});
                Object.defineProperty(navigator,'mimeTypes',{get:function(){var a=[];a.item=function(){return null;};a.namedItem=function(){return null;};return a;},configurable:true});
            }catch(e){/* expected */}
            """.trimIndent()
        }
    }

    // ==================== Battery API ====================
    private fun generateBatteryProtectionScript(): String {
        return """
            // Battery API — return consistent fake values (always "plugged in, full")
            if (navigator.getBattery) {
                var fakeBattery = {
                    charging: true, chargingTime: 0, dischargingTime: Infinity, level: 1,
                    addEventListener: function(){}, removeEventListener: function(){},
                    dispatchEvent: function(){return true;},
                    onchargingchange: null, onchargingtimechange: null,
                    ondischargingtimechange: null, onlevelchange: null
                };
                navigator.getBattery = function() { return Promise.resolve(fakeBattery); };
            }
        """.trimIndent()
    }

    // ==================== navigator.connection ====================
    private fun generateConnectionSpoofScript(): String {
        return """
            // navigator.connection — normalize to common broadband profile
            try {
                var fakeConn = {
                    downlink: 10, effectiveType: '4g', rtt: 50, saveData: false,
                    type: 'wifi', downlinkMax: Infinity,
                    addEventListener: function(){}, removeEventListener: function(){},
                    dispatchEvent: function(){return true;},
                    onchange: null, ontypechange: null
                };
                Object.defineProperty(navigator,'connection',{get:function(){return fakeConn;},configurable:true});
            }catch(e){/* expected */}
        """.trimIndent()
    }

    // ==================== MediaDevices enumeration ====================
    private fun generateMediaDevicesProtectionScript(): String {
        return """
            // MediaDevices.enumerateDevices — return generic device list
            if (navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {
                var origEnum = navigator.mediaDevices.enumerateDevices.bind(navigator.mediaDevices);
                navigator.mediaDevices.enumerateDevices = function() {
                    return origEnum().then(function(devices) {
                        // Strip labels and deviceIds to prevent fingerprinting
                        return devices.map(function(d, i) {
                            return {
                                deviceId: 'device' + i,
                                groupId: 'group' + Math.floor(i/2),
                                kind: d.kind,
                                label: '',
                                toJSON: function() { return {deviceId:this.deviceId,groupId:this.groupId,kind:this.kind,label:''}; }
                            };
                        });
                    });
                };
            }
        """.trimIndent()
    }

    // ==================== Storage estimation ====================
    private fun generateStorageEstimateScript(): String {
        return """
            // StorageManager.estimate — return normalized values
            if (navigator.storage && navigator.storage.estimate) {
                navigator.storage.estimate = function() {
                    return Promise.resolve({ quota: 2147483648, usage: 0 });
                };
            }
        """.trimIndent()
    }

    // ==================== history.length ====================
    private fun generateHistoryProtectionScript(): String {
        return """
            // history.length — fixed at 1 to prevent history-based tracking
            try{Object.defineProperty(history,'length',{get:function(){return 1;},configurable:true});}catch(e){/* expected */}
        """.trimIndent()
    }

    // ==================== Permissions API ====================
    private fun generatePermissionsProtectionScript(): String {
        return """
            // Permissions API — normalize responses
            if (navigator.permissions && navigator.permissions.query) {
                var origQuery = navigator.permissions.query.bind(navigator.permissions);
                navigator.permissions.query = function(desc) {
                    return origQuery(desc).catch(function() {
                        return { state: 'prompt', onchange: null, addEventListener: function(){}, removeEventListener: function(){} };
                    });
                };
            }
        """.trimIndent()
    }
}
