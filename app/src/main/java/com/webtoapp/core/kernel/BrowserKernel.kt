package com.webtoapp.core.kernel

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.webview.OAuthCompatEngine

































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








    fun configureWebView(webView: WebView) {
        try {
            webView.settings.apply {

                val rawUa = userAgentString ?: WebSettings.getDefaultUserAgent(webView.context)
                val cleanUa = sanitizeUserAgent(rawUa)
                userAgentString = cleanUa
                cachedCleanUa = cleanUa

                AppLogger.d(TAG, "UA sanitized: ${cleanUa.take(80)}...")
            }



            removeRequestedWithHeader(webView)

        } catch (e: Exception) {
            AppLogger.w(TAG, "WebView kernel config failed", e)
        }
    }










    @SuppressLint("RestrictedApi")
    private fun removeRequestedWithHeader(webView: WebView) {
        try {

            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(
                    webView.settings,
                    emptySet()
                )
                AppLogger.d(TAG, "X-Requested-With header removed via ALLOW_LIST API")
                return
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "WebSettingsCompat ALLOW_LIST failed: ${e.message}")
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val method = WebSettings::class.java.getMethod(
                    "setRequestedWithHeaderMode", Int::class.javaPrimitiveType
                )

                method.invoke(webView.settings, 2)
                AppLogger.d(TAG, "X-Requested-With header removed via reflection")
                return
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Reflection setRequestedWithHeaderMode failed: ${e.message}")
        }

        AppLogger.d(TAG, "X-Requested-With removal: will rely on shouldInterceptRequest fallback")
    }





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




    fun getCleanUserAgent(): String? = cachedCleanUa







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





    fun sanitizeRequestHeaders(headers: Map<String, String>): Map<String, String> {
        val clean = headers.toMutableMap()


        clean.remove("X-Requested-With")
        clean.remove("x-requested-with")


        cachedCleanUa?.let { ua ->
            clean["User-Agent"] = ua
        }

        return clean
    }







    fun isKernelDisguiseActive(): Boolean = true






    @Deprecated("Use OAuthCompatEngine.getAntiDetectionJs(url) instead",
        replaceWith = ReplaceWith("OAuthCompatEngine.getAntiDetectionJs(url)"))
    fun getGoogleOAuthAntiDetectionJs(): String {

        return OAuthCompatEngine.getAntiDetectionJs("https://accounts.google.com") ?: ""
    }







    fun getBuildTimeKernelJs(): String = getKernelJs()



    private fun sanitizeUserAgentFallback(ua: String): String {
        var result = ua

        result = result.replace(" wv)", ")")
        result = result.replace(" wv ", " ")
        result = result.replace("; wv)", ")")
        result = result.replace(";wv)", ")")

        result = result.replace(Regex("Version/[\\d.]+ "), "")

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



    private external fun nativeGetKernelJs(): String
    private external fun nativeSanitizeUserAgent(ua: String): String?
    private external fun nativeBuildChromeUserAgent(ua: String): String?
}
