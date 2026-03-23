package com.webtoapp.core.isolation

/**
 * 隔离脚本注入器
 * 
 * 生成用于防检测的 JavaScript 代码，注入到 WebView 中
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
        
        // 基础指纹伪造
        if (config.fingerprintConfig.randomize) {
            scripts.add(generateNavigatorScript(config, fingerprint))
        }
        
        // Canvas 指纹防护
        if (config.protectCanvas) {
            scripts.add(generateCanvasProtectionScript(fingerprint.canvasNoise))
        }
        
        // WebGL 指纹防护
        if (config.protectWebGL) {
            scripts.add(generateWebGLProtectionScript(fingerprint))
        }
        
        // AudioContext 指纹防护
        if (config.protectAudio) {
            scripts.add(generateAudioProtectionScript(fingerprint.audioNoise))
        }
        
        // WebRTC 防泄漏
        if (config.blockWebRTC) {
            scripts.add(generateWebRTCBlockScript())
        }
        
        // 字体指纹防护
        if (config.protectFonts) {
            scripts.add(generateFontProtectionScript())
        }
        
        // 屏幕分辨率伪装
        if (config.spoofScreen) {
            scripts.add(generateScreenSpoofScript(
                config.customScreenWidth ?: fingerprint.screenWidth,
                config.customScreenHeight ?: fingerprint.screenHeight
            ))
        }
        
        // 时区伪装
        if (config.spoofTimezone) {
            scripts.add(generateTimezoneSpoofScript(config.customTimezone ?: fingerprint.timezone))
        }
        
        return """
            (function() {
                'use strict';
                
                // WebToApp 隔离环境
                const __isolation__ = {
                    enabled: true,
                    version: '1.0.0'
                };
                
                try {
                    ${scripts.joinToString("\n\n")}
                    
                    console.log('[WebToApp] 隔离环境已启用');
                } catch(e) {
                    console.error('[WebToApp] 隔离脚本错误:', e);
                }
            })();
        """.trimIndent()
    }
    
    /**
     * Navigator 对象伪造
     */
    private fun generateNavigatorScript(config: IsolationConfig, fp: GeneratedFingerprint): String {
        val userAgent = config.fingerprintConfig.customUserAgent ?: fp.userAgent
        val platform = config.fingerprintConfig.platform ?: fp.platform
        val vendor = config.fingerprintConfig.vendor ?: fp.vendor
        val hardwareConcurrency = config.fingerprintConfig.hardwareConcurrency ?: fp.hardwareConcurrency
        val deviceMemory = config.fingerprintConfig.deviceMemory ?: fp.deviceMemory
        
        return """
            // Navigator 伪造
            const navigatorProps = {
                userAgent: '$userAgent',
                platform: '$platform',
                vendor: '$vendor',
                hardwareConcurrency: $hardwareConcurrency,
                deviceMemory: $deviceMemory,
                language: '${fp.language.split(",").first()}',
                languages: ${fp.language.split(",").map { "'${it.split(";").first().trim()}'" }},
                maxTouchPoints: 0,
                webdriver: false
            };
            
            Object.keys(navigatorProps).forEach(function(prop) {
                try {
                    Object.defineProperty(navigator, prop, {
                        get: function() { return navigatorProps[prop]; },
                        configurable: true
                    });
                } catch(e) {}
            });
            
            // Hide webdriver 标志
            Object.defineProperty(navigator, 'webdriver', {
                get: function() { return false; },
                configurable: true
            });
            
            // Hide自动化标志
            delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;
            delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;
            delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;
        """.trimIndent()
    }
    
    /**
     * Canvas 指纹防护
     */
    private fun generateCanvasProtectionScript(noise: Float): String {
        return """
            // Canvas 指纹防护
            const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
            const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
            const originalToBlob = HTMLCanvasElement.prototype.toBlob;
            
            function addCanvasNoise(canvas) {
                const ctx = canvas.getContext('2d');
                if (!ctx) return;
                
                try {
                    const imageData = originalGetImageData.call(ctx, 0, 0, canvas.width, canvas.height);
                    const data = imageData.data;
                    const noise = $noise;
                    
                    for (let i = 0; i < data.length; i += 4) {
                        // 添加微小噪声
                        data[i] = Math.max(0, Math.min(255, data[i] + (Math.random() - 0.5) * noise * 255));
                        data[i + 1] = Math.max(0, Math.min(255, data[i + 1] + (Math.random() - 0.5) * noise * 255));
                        data[i + 2] = Math.max(0, Math.min(255, data[i + 2] + (Math.random() - 0.5) * noise * 255));
                    }
                    
                    ctx.putImageData(imageData, 0, 0);
                } catch(e) {}
            }
            
            HTMLCanvasElement.prototype.toDataURL = function() {
                addCanvasNoise(this);
                return originalToDataURL.apply(this, arguments);
            };
            
            HTMLCanvasElement.prototype.toBlob = function() {
                addCanvasNoise(this);
                return originalToBlob.apply(this, arguments);
            };
            
            CanvasRenderingContext2D.prototype.getImageData = function() {
                const imageData = originalGetImageData.apply(this, arguments);
                const data = imageData.data;
                const noise = $noise;
                
                for (let i = 0; i < data.length; i += 4) {
                    data[i] = Math.max(0, Math.min(255, data[i] + (Math.random() - 0.5) * noise * 255));
                    data[i + 1] = Math.max(0, Math.min(255, data[i + 1] + (Math.random() - 0.5) * noise * 255));
                    data[i + 2] = Math.max(0, Math.min(255, data[i + 2] + (Math.random() - 0.5) * noise * 255));
                }
                
                return imageData;
            };
        """.trimIndent()
    }
    
    /**
     * WebGL 指纹防护
     */
    private fun generateWebGLProtectionScript(fp: GeneratedFingerprint): String {
        return """
            // WebGL 指纹防护
            const getParameterProxyHandler = {
                apply: function(target, thisArg, args) {
                    const param = args[0];
                    const gl = thisArg;
                    
                    // UNMASKED_VENDOR_WEBGL
                    if (param === 37445) {
                        return '${fp.webglVendor}';
                    }
                    // UNMASKED_RENDERER_WEBGL
                    if (param === 37446) {
                        return '${fp.webglRenderer}';
                    }
                    
                    return target.apply(thisArg, args);
                }
            };
            
            // WebGL1
            const originalGetParameter = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = new Proxy(originalGetParameter, getParameterProxyHandler);
            
            // WebGL2
            if (typeof WebGL2RenderingContext !== 'undefined') {
                const originalGetParameter2 = WebGL2RenderingContext.prototype.getParameter;
                WebGL2RenderingContext.prototype.getParameter = new Proxy(originalGetParameter2, getParameterProxyHandler);
            }
            
            // 扩展信息
            const originalGetExtension = WebGLRenderingContext.prototype.getExtension;
            WebGLRenderingContext.prototype.getExtension = function(name) {
                if (name === 'WEBGL_debug_renderer_info') {
                    return {
                        UNMASKED_VENDOR_WEBGL: 37445,
                        UNMASKED_RENDERER_WEBGL: 37446
                    };
                }
                return originalGetExtension.apply(this, arguments);
            };
        """.trimIndent()
    }
    
    /**
     * AudioContext 指纹防护
     */
    private fun generateAudioProtectionScript(noise: Float): String {
        return """
            // AudioContext 指纹防护
            const originalCreateAnalyser = AudioContext.prototype.createAnalyser;
            const originalGetFloatFrequencyData = AnalyserNode.prototype.getFloatFrequencyData;
            const originalGetByteFrequencyData = AnalyserNode.prototype.getByteFrequencyData;
            
            AnalyserNode.prototype.getFloatFrequencyData = function(array) {
                originalGetFloatFrequencyData.call(this, array);
                const noise = $noise;
                for (let i = 0; i < array.length; i++) {
                    array[i] += (Math.random() - 0.5) * noise * 100;
                }
            };
            
            AnalyserNode.prototype.getByteFrequencyData = function(array) {
                originalGetByteFrequencyData.call(this, array);
                const noise = $noise;
                for (let i = 0; i < array.length; i++) {
                    array[i] = Math.max(0, Math.min(255, array[i] + (Math.random() - 0.5) * noise * 255));
                }
            };
            
            // 修改 AudioContext 采样率
            const originalAudioContext = window.AudioContext || window.webkitAudioContext;
            if (originalAudioContext) {
                window.AudioContext = window.webkitAudioContext = function() {
                    const ctx = new originalAudioContext();
                    return ctx;
                };
            }
        """.trimIndent()
    }
    
    /**
     * WebRTC 防泄漏
     */
    private fun generateWebRTCBlockScript(): String {
        return """
            // WebRTC 防泄漏
            if (typeof RTCPeerConnection !== 'undefined') {
                const originalRTCPeerConnection = RTCPeerConnection;
                
                RTCPeerConnection = function(config) {
                    // 移除 STUN/TURN 服务器配置
                    if (config && config.iceServers) {
                        config.iceServers = [];
                    }
                    return new originalRTCPeerConnection(config);
                };
                
                RTCPeerConnection.prototype = originalRTCPeerConnection.prototype;
                
                // 阻止本地 IP 泄漏
                const originalCreateOffer = RTCPeerConnection.prototype.createOffer;
                RTCPeerConnection.prototype.createOffer = function() {
                    return originalCreateOffer.apply(this, arguments).then(function(offer) {
                        // 移除候选者中的本地 IP
                        offer.sdp = offer.sdp.replace(/a=candidate:.*typ host.*/g, '');
                        return offer;
                    });
                };
            }
            
            // Disable getUserMedia（可选）
            // navigator.mediaDevices.getUserMedia = undefined;
        """.trimIndent()
    }
    
    /**
     * 字体指纹防护
     */
    private fun generateFontProtectionScript(): String {
        return """
            // 字体指纹防护
            const commonFonts = [
                'Arial', 'Arial Black', 'Comic Sans MS', 'Courier New', 'Georgia',
                'Impact', 'Times New Roman', 'Trebuchet MS', 'Verdana', 'Webdings'
            ];
            
            // 覆盖字体检测
            const originalOffsetWidth = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'offsetWidth');
            const originalOffsetHeight = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'offsetHeight');
            
            // 返回一致的字体度量
            Object.defineProperty(HTMLElement.prototype, 'offsetWidth', {
                get: function() {
                    const style = window.getComputedStyle(this);
                    const fontFamily = style.fontFamily;
                    
                    // If it is字体检测元素，返回固定值
                    if (this.style.position === 'absolute' && this.style.left === '-9999px') {
                        return 100;
                    }
                    
                    return originalOffsetWidth.get.call(this);
                }
            });
        """.trimIndent()
    }
    
    /**
     * 屏幕分辨率伪装
     */
    private fun generateScreenSpoofScript(width: Int, height: Int): String {
        return """
            // 屏幕分辨率伪装
            Object.defineProperty(screen, 'width', { get: function() { return $width; } });
            Object.defineProperty(screen, 'height', { get: function() { return $height; } });
            Object.defineProperty(screen, 'availWidth', { get: function() { return $width; } });
            Object.defineProperty(screen, 'availHeight', { get: function() { return ${height - 40}; } });
            Object.defineProperty(window, 'innerWidth', { get: function() { return $width; } });
            Object.defineProperty(window, 'innerHeight', { get: function() { return ${height - 100}; } });
            Object.defineProperty(window, 'outerWidth', { get: function() { return $width; } });
            Object.defineProperty(window, 'outerHeight', { get: function() { return $height; } });
        """.trimIndent()
    }
    
    /**
     * 时区伪装
     */
    private fun generateTimezoneSpoofScript(timezone: String): String {
        return """
            // 时区伪装
            const originalDateTimeFormat = Intl.DateTimeFormat;
            Intl.DateTimeFormat = function(locales, options) {
                options = options || {};
                options.timeZone = '$timezone';
                return new originalDateTimeFormat(locales, options);
            };
            Intl.DateTimeFormat.prototype = originalDateTimeFormat.prototype;
            
            // 修改 Date.prototype.getTimezoneOffset
            const targetOffset = new originalDateTimeFormat('en-US', { timeZone: '$timezone' })
                .resolvedOptions().timeZone;
            
            // 简化处理：返回常见时区偏移
            const timezoneOffsets = {
                'Asia/Shanghai': -480,
                'America/New_York': 300,
                'America/Los_Angeles': 480,
                'Europe/London': 0,
                'Europe/Paris': -60,
                'Asia/Tokyo': -540
            };
            
            const offset = timezoneOffsets['$timezone'] || 0;
            Date.prototype.getTimezoneOffset = function() { return offset; };
        """.trimIndent()
    }
}
