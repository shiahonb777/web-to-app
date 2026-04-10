package com.webtoapp.core.kernel

import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.webview.OAuthCompatEngine

/**
 * 浏览器内核级伪装引擎
 *
 * 使 WebView 完全不可被检测为 WebView，与真实 Chrome 浏览器无法区分。
 *
 * ## 伪装层级
 *
 * ### Level 1: HTTP 层 (请求头)
 * - User-Agent: 移除 "wv"/"Version/X.X", 确保包含 Chrome/xxx
 * - X-Requested-With: 移除 (WebView 独有的包名泄露)
 *
 * ### Level 2: JavaScript 层 (DOM API)
 * - navigator: webdriver=false, 完整 plugins/mimeTypes, vendor="Google Inc."
 * - window.chrome: 完整 runtime/loadTimes/csi 对象 (WebView 缺失)
 * - WebGL: renderer/vendor 信息伪装
 * - Permissions/Notification API 完善
 * - Error stack trace 清理 (移除 evaluateJavascript 痕迹)
 *
 * ### Level 3: 原型链保护
 * - Function.prototype.toString → 所有 hook 返回 [native code]
 * - Object.getOwnPropertyDescriptor → hook 过的属性返回正常描述符
 * - iframe.contentWindow.chrome → 自动传播伪装
 *
 * ## 使用方式
 * ```kotlin
 * // 在 WebViewManager.configureWebView() 中:
 * BrowserKernel.configureWebView(webView)
 *
 * // 在 onPageStarted() 中 (最早注入):
 * BrowserKernel.injectKernelJs(webView)
 * ```
 */
object BrowserKernel {

    private const val TAG = "BrowserKernel"

    @Volatile
    private var isAvailable = false

    @Volatile
    private var cachedKernelJs: String? = null

    @Volatile
    private var cachedCleanUa: String? = null

    init {
        try {
            System.loadLibrary("browser_kernel")
            isAvailable = true
            AppLogger.i(TAG, "Browser kernel engine loaded")
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.w(TAG, "Browser kernel not available: ${e.message}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Browser kernel init failed", e)
        }
    }

    fun isAvailable(): Boolean = isAvailable

    // ==================== Level 1: HTTP 层伪装 ====================

    /**
     * 配置 WebView — 移除所有 WebView 标识
     *
     * 必须在 WebView 加载任何 URL 之前调用
     */
    fun configureWebView(webView: WebView) {
        try {
            webView.settings.apply {
                // 1. 清洗 User-Agent — 移除 wv, Version/X.X
                val rawUa = userAgentString ?: WebSettings.getDefaultUserAgent(webView.context)
                val cleanUa = sanitizeUserAgent(rawUa)
                userAgentString = cleanUa
                cachedCleanUa = cleanUa

                AppLogger.d(TAG, "UA sanitized: ${cleanUa.take(80)}...")
            }

            // 2. 移除 X-Requested-With 头 (包含包名, 100% 暴露 WebView)
            // 这是 Google 检测 WebView 的最关键信号之一
            removeRequestedWithHeader(webView)

        } catch (e: Exception) {
            AppLogger.w(TAG, "WebView kernel config failed", e)
        }
    }

    /**
     * 移除 X-Requested-With 请求头
     *
     * Android WebView 默认会在所有请求中附加 X-Requested-With 头，
     * 值为应用包名。这是 Google（及其他 OAuth 提供商）检测嵌入式
     * WebView 的最可靠信号。
     *
     * 使用 AndroidX WebView 库的 WebSettingsCompat API 来禁用此头。
     */
    private fun removeRequestedWithHeader(webView: WebView) {
        try {
            // 方案 1: AndroidX WebSettingsCompat (Chromium 96+, 最可靠)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(
                    webView.settings,
                    emptySet()  // 空集合 = 不发送 X-Requested-With 给任何域
                )
                AppLogger.d(TAG, "X-Requested-With header removed via ALLOW_LIST API")
                return
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "WebSettingsCompat ALLOW_LIST failed: ${e.message}")
        }

        try {
            // 方案 2: 反射设置 REQUESTED_WITH_HEADER_MODE (Android 12L+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val method = WebSettings::class.java.getMethod(
                    "setRequestedWithHeaderMode", Int::class.javaPrimitiveType
                )
                // Mode 2 = HEADER_NEVER
                method.invoke(webView.settings, 2)
                AppLogger.d(TAG, "X-Requested-With header removed via reflection")
                return
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Reflection setRequestedWithHeaderMode failed: ${e.message}")
        }

        AppLogger.d(TAG, "X-Requested-With removal: will rely on shouldInterceptRequest fallback")
    }

    /**
     * 清洗 User-Agent
     * C 级处理, 避免 Kotlin 的字符串分配和正则开销
     */
    fun sanitizeUserAgent(rawUa: String): String {
        if (isAvailable) {
            try {
                return nativeBuildChromeUserAgent(rawUa) ?: rawUa
            } catch (e: Exception) {
                AppLogger.w(TAG, "Native UA sanitize failed", e)
            }
        }
        return sanitizeUserAgentFallback(rawUa)
    }

    /**
     * 获取缓存的清洗 UA
     */
    fun getCleanUserAgent(): String? = cachedCleanUa

    // ==================== Level 2&3: JavaScript 层伪装 ====================

    /**
     * 获取内核伪装 JavaScript
     * 必须在 DOCUMENT_START 最早注入
     */
    fun getKernelJs(): String {
        cachedKernelJs?.let { return it }

        return if (isAvailable) {
            try {
                nativeGetKernelJs().also { cachedKernelJs = it }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to get kernel JS", e)
                getFallbackKernelJs()
            }
        } else {
            getFallbackKernelJs()
        }
    }

    /**
     * 注入内核伪装 JavaScript 到 WebView
     * 在 onPageStarted 中调用, 确保在任何页面脚本之前执行
     */
    fun injectKernelJs(webView: WebView) {
        try {
            val js = getKernelJs()
            if (js.isNotEmpty()) {
                webView.evaluateJavascript(js, null)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Kernel JS injection failed", e)
        }
    }

    /**
     * 移除 WebView 的请求头中的泄露信息
     * 用于 shouldInterceptRequest 中修改请求头
     */
    fun sanitizeRequestHeaders(headers: Map<String, String>): Map<String, String> {
        val clean = headers.toMutableMap()

        // X-Requested-With 包含包名, 100% 暴露 WebView
        clean.remove("X-Requested-With")
        clean.remove("x-requested-with")

        // 确保 UA 是清洗过的
        cachedCleanUa?.let { ua ->
            clean["User-Agent"] = ua
        }

        return clean
    }

    // ==================== OAuth 反检测 ====================

    /**
     * 检查内核伪装是否完全可用
     * 当伪装可用时，OAuth 可以直接在 WebView 内完成
     */
    fun isKernelDisguiseActive(): Boolean = true  // UA 清洗 + JS 注入始终生效

    /**
     * 获取 OAuth 反检测 JavaScript（委托给 OAuthCompatEngine）
     *
     * @deprecated 使用 OAuthCompatEngine.getAntiDetectionJs(url) 替代
     */
    @Deprecated("Use OAuthCompatEngine.getAntiDetectionJs(url) instead",
        replaceWith = ReplaceWith("OAuthCompatEngine.getAntiDetectionJs(url)"))
    fun getGoogleOAuthAntiDetectionJs(): String {
        // Fallback: 返回 Google 提供商的反检测 JS
        return OAuthCompatEngine.getAntiDetectionJs("https://accounts.google.com") ?: ""
    }

    // ==================== 构建时注入 ====================

    /**
     * 获取用于 APK 构建时嵌入的完整内核 JS
     * 确保 Shell APK 也有反检测能力
     */
    fun getBuildTimeKernelJs(): String = getKernelJs()

    // ==================== Fallback ====================

    private fun sanitizeUserAgentFallback(ua: String): String {
        var result = ua
        // 移除 " wv" 标记
        result = result.replace(" wv)", ")")
        result = result.replace(" wv ", " ")
        result = result.replace("; wv)", ")")
        result = result.replace(";wv)", ")")
        // 移除 "Version/X.X.X.X " (WebView 特有)
        result = result.replace(Regex("Version/[\\d.]+ "), "")
        // 确保包含 Chrome/
        if (!result.contains("Chrome/")) {
            result = result.replace("Safari/", "Chrome/131.0.0.0 Safari/")
        }
        return result
    }

    private fun getFallbackKernelJs(): String {
        return """(function(){'use strict';
            // 基础反检测
            Object.defineProperty(navigator,'webdriver',{get:function(){return false},enumerable:true,configurable:true});
            // window.chrome
            if(!window.chrome)window.chrome={};
            if(!window.chrome.runtime)window.chrome.runtime={
                OnInstalledReason:{CHROME_UPDATE:'chrome_update',INSTALL:'install'},
                connect:function(){return{onDisconnect:{addListener:function(){}},postMessage:function(){},disconnect:function(){}};}
            };
            if(!window.chrome.loadTimes)window.chrome.loadTimes=function(){return{commitLoadTime:Date.now()/1000,firstPaintTime:Date.now()/1000,navigationType:'Other'};};
            if(!window.chrome.csi)window.chrome.csi=function(){return{onloadT:Date.now(),pageT:performance.now(),tran:15};};
            // plugins
            Object.defineProperty(navigator,'plugins',{get:function(){return{length:5,0:{name:'PDF Viewer'},1:{name:'Chrome PDF Plugin'},2:{name:'Chrome PDF Viewer'},3:{name:'Native Client'},4:{name:'Chromium PDF Plugin'}}},enumerable:true});
            // vendor
            Object.defineProperty(navigator,'vendor',{get:function(){return'Google Inc.'},enumerable:true});
            // outerWidth/outerHeight
            if(!window.outerWidth)Object.defineProperty(window,'outerWidth',{get:function(){return window.innerWidth}});
            if(!window.outerHeight)Object.defineProperty(window,'outerHeight',{get:function(){return window.innerHeight+56}});
            // 清除自动化标志
            delete window.__selenium_unwrapped;
            delete window.__webdriver_evaluate;
            delete window.__webdriver_script_function;
        })();""".trimIndent()
    }

    // ==================== Native 方法 ====================

    private external fun nativeGetKernelJs(): String
    private external fun nativeSanitizeUserAgent(ua: String): String?
    private external fun nativeBuildChromeUserAgent(ua: String): String?
}
