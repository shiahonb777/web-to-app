package com.webtoapp.core.perf

import android.content.Context
import android.os.Build
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger




























object NativePerfEngine {

    private const val TAG = "PerfEngine"

    @Volatile
    private var isLoaded = false

    @Volatile
    private var isAvailable = false


    @Volatile
    private var cachedPerfJsStart: String? = null

    @Volatile
    private var cachedPerfJsEnd: String? = null

    @Volatile
    private var cachedPerfCss: String? = null

    init {
        try {
            System.loadLibrary("perf_engine")
            nativeInit()
            isAvailable = true
            isLoaded = true
            AppLogger.i(TAG, "Native performance engine loaded")
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.w(TAG, "Performance engine not available: ${e.message}")
            isLoaded = true
            isAvailable = false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize perf engine", e)
            isLoaded = true
            isAvailable = false
        }
    }

    fun isAvailable(): Boolean = isAvailable







    fun getPerfJsStart(): String {
        cachedPerfJsStart?.let { return it }
        return if (isAvailable) {
            try {
                nativeGetPerfJsStart().also { cachedPerfJsStart = it }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to get perf JS start", e)
                getFallbackPerfJsStart()
            }
        } else {
            getFallbackPerfJsStart()
        }
    }





    fun getPerfJsEnd(): String {
        cachedPerfJsEnd?.let { return it }
        return if (isAvailable) {
            try {
                nativeGetPerfJsEnd().also { cachedPerfJsEnd = it }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to get perf JS end", e)
                getFallbackPerfJsEnd()
            }
        } else {
            getFallbackPerfJsEnd()
        }
    }




    fun getPerfCss(): String {
        cachedPerfCss?.let { return it }
        return if (isAvailable) {
            try {
                nativeGetPerfCss().also { cachedPerfCss = it }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to get perf CSS", e)
                ""
            }
        } else {
            ""
        }
    }





    fun injectPerfOptimizations(webView: WebView, phase: Phase) {
        try {
            when (phase) {
                Phase.DOCUMENT_START -> {
                    val js = getPerfJsStart()
                    if (js.isNotEmpty()) {
                        webView.evaluateJavascript(js, null)
                    }
                }
                Phase.DOCUMENT_END -> {
                    val js = getPerfJsEnd()
                    if (js.isNotEmpty()) {
                        webView.evaluateJavascript(js, null)
                    }

                    val css = getPerfCss()
                    if (css.isNotEmpty()) {
                        val cssInjection = "(function(){" +
                            "if(!document.getElementById('webtoapp-perf-css')){" +
                            "document.head.insertAdjacentHTML('beforeend',${css.toJsStringLiteral()});" +
                            "}})()"
                        webView.evaluateJavascript(cssInjection, null)
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Perf injection failed (phase=$phase)", e)
        }
    }







    fun extractHost(url: String): String? {
        if (!isAvailable) return extractHostFallback(url)
        return try {
            nativeExtractHost(url)
        } catch (e: Exception) {
            extractHostFallback(url)
        }
    }




    fun getMimeType(path: String): String {
        if (!isAvailable) return getMimeTypeFallback(path)
        return try {
            nativeGetMimeType(path) ?: "application/octet-stream"
        } catch (e: Exception) {
            getMimeTypeFallback(path)
        }
    }





    fun checkUrlScheme(url: String): Int {
        if (!isAvailable) return checkUrlSchemeFallback(url)
        return try {
            nativeCheckUrlScheme(url)
        } catch (e: Exception) {
            checkUrlSchemeFallback(url)
        }
    }





    fun matchHostSuffix(url: String, suffixes: Array<String>): Int {
        if (!isAvailable) return -1
        return try {
            nativeMatchHostSuffix(url, suffixes)
        } catch (e: Exception) {
            -1
        }
    }







    fun mmapRead(path: String): ByteArray? {
        if (!isAvailable) return null
        return try {
            nativeMmapRead(path)
        } catch (e: Exception) {
            AppLogger.w(TAG, "mmap read failed: $path", e)
            null
        }
    }







    fun getMemoryInfo(): LongArray? {
        if (!isAvailable) return null
        return try {
            nativeGetMemoryInfo()
        } catch (e: Exception) {
            null
        }
    }




    fun isLowMemory(): Boolean {
        val info = getMemoryInfo() ?: return false
        return info[1] < info[2]
    }







    fun optimizeWebViewSettings(webView: WebView) {
        try {
            webView.settings.apply {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = false
                }


                @Suppress("DEPRECATION")
                setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)


                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT


                if (isLowMemory()) {
                    blockNetworkImage = false
                    loadsImagesAutomatically = true
                    AppLogger.w(TAG, "Low memory mode: image optimization active")
                }


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    offscreenPreRaster = true
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "WebView optimization failed", e)
        }
    }







    fun generateBuildTimePerformanceScript(): String {
        val startJs = getPerfJsStart()
        val endJs = getPerfJsEnd()
        val css = getPerfCss()

        return buildString {
            appendLine("<!-- WebToApp Performance Optimization -->")
            appendLine("<script>")
            appendLine("// DOCUMENT_START optimizations (passive events, visibility)")
            appendLine(startJs)
            appendLine("</script>")
            appendLine(css)
            appendLine("<script>")
            appendLine("// DOCUMENT_END optimizations (lazy load, preconnect, jank detect)")
            appendLine("document.addEventListener('DOMContentLoaded',function(){")
            appendLine(endJs)
            appendLine("});")
            appendLine("</script>")
        }
    }



    private fun extractHostFallback(url: String): String? {
        return try {
            android.net.Uri.parse(url)?.host
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeTypeFallback(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js", "mjs" -> "application/javascript"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
    }

    private fun checkUrlSchemeFallback(url: String): Int {
        return when {
            url.startsWith("https://") -> 2
            url.startsWith("http://") -> 1
            url.startsWith("file://") -> 3
            url.startsWith("data:") -> 4
            url.startsWith("javascript:") -> 5
            url.startsWith("chrome-extension://") -> 6
            else -> 0
        }
    }

    private fun getFallbackPerfJsStart(): String {
        return """(function(){'use strict';
            var _o=EventTarget.prototype.addEventListener;
            var _p={touchstart:1,touchmove:1,touchend:1,wheel:1,scroll:1};
            EventTarget.prototype.addEventListener=function(t,fn,opts){
                if(_p[t]&&opts===undefined)opts={passive:true,capture:false};
                return _o.call(this,t,fn,opts);
            };
        })();""".trimIndent()
    }

    private fun getFallbackPerfJsEnd(): String {
        return """(function(){'use strict';
            if('IntersectionObserver' in window){
                var io=new IntersectionObserver(function(es){
                    for(var i=0;i<es.length;i++){
                        if(es[i].isIntersecting){
                            var img=es[i].target;
                            if(img.dataset.src){img.src=img.dataset.src;}
                            io.unobserve(img);
                        }
                    }
                },{rootMargin:'200px 0px'});
                document.querySelectorAll('img:not([loading])').forEach(function(img){img.loading='lazy';io.observe(img);});
            }
        })();""".trimIndent()
    }

    private fun String.toJsStringLiteral(): String {
        return "'" + this
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r") + "'"
    }

    enum class Phase {
        DOCUMENT_START,
        DOCUMENT_END
    }



    private external fun nativeInit(): Boolean
    private external fun nativeGetPerfJsStart(): String
    private external fun nativeGetPerfJsEnd(): String
    private external fun nativeGetPerfCss(): String
    private external fun nativeExtractHost(url: String): String?
    private external fun nativeGetMimeType(path: String): String?
    private external fun nativeCheckUrlScheme(url: String): Int
    private external fun nativeMatchHostSuffix(url: String, suffixes: Array<String>): Int
    private external fun nativeMmapRead(path: String): ByteArray?
    private external fun nativeGetMemoryInfo(): LongArray?
}
