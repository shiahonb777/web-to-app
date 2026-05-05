package com.webtoapp.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger












object WebViewPool {

    private const val TAG = "WebViewPool"
    private const val MAX_POOL_SIZE = 2

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pool = ArrayDeque<WebView>(MAX_POOL_SIZE)

    @Volatile
    private var isPrewarmed = false






    fun prewarm(context: Context) {
        if (isPrewarmed) return
        isPrewarmed = true

        val appContext = context.applicationContext


        mainHandler.post {
            try {
                val startTime = System.currentTimeMillis()
                val webView = createPrewarmedWebView(appContext)
                synchronized(pool) {
                    if (pool.size < MAX_POOL_SIZE) {
                        pool.addLast(webView)
                    } else {
                        webView.destroy()
                    }
                }
                val elapsed = System.currentTimeMillis() - startTime
                AppLogger.i(TAG, "WebView prewarmed in ${elapsed}ms (pool size: ${pool.size})")
            } catch (e: Exception) {
                AppLogger.e(TAG, "WebView prewarm failed", e)
            }
        }
    }








    @SuppressLint("SetJavaScriptEnabled")
    fun acquire(context: Context): WebView {
        val webView = synchronized(pool) {
            pool.removeLastOrNull()
        }

        return if (webView != null) {


            (webView.context as? MutableContextWrapper)?.baseContext = context
            AppLogger.d(TAG, "WebView acquired from pool (remaining: ${pool.size})")


            replenishPool(context.applicationContext)

            webView
        } else {
            AppLogger.d(TAG, "Pool empty, creating new WebView")
            WebView(context)
        }
    }





    fun recycle(webView: WebView) {
        try {

            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()

            synchronized(pool) {
                if (pool.size < MAX_POOL_SIZE) {
                    pool.addLast(webView)
                    AppLogger.d(TAG, "WebView recycled to pool (pool size: ${pool.size})")
                    return
                }
            }


            webView.destroy()
            AppLogger.d(TAG, "Pool full, WebView destroyed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "WebView recycle failed, destroying", e)
            try { webView.destroy() } catch (_: Exception) {}
        }
    }





    fun release() {
        synchronized(pool) {
            pool.forEach { webView ->
                try {
                    webView.destroy()
                } catch (_: Exception) {}
            }
            pool.clear()
        }
        isPrewarmed = false
        AppLogger.i(TAG, "WebView pool released")
    }




    fun poolSize(): Int = synchronized(pool) { pool.size }




    private fun replenishPool(appContext: Context) {
        mainHandler.post {
            synchronized(pool) {
                if (pool.size >= MAX_POOL_SIZE) return@post
            }
            try {
                val webView = createPrewarmedWebView(appContext)
                synchronized(pool) {
                    if (pool.size < MAX_POOL_SIZE) {
                        pool.addLast(webView)
                        AppLogger.d(TAG, "Pool replenished (pool size: ${pool.size})")
                    } else {
                        webView.destroy()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Pool replenish failed", e)
            }
        }
    }






    @SuppressLint("SetJavaScriptEnabled")
    private fun createPrewarmedWebView(appContext: Context): WebView {
        val contextWrapper = MutableContextWrapper(appContext)
        val webView = WebView(contextWrapper)



        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            @Suppress("DEPRECATION")
            setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                offscreenPreRaster = true
            }
        }


        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        webView.isScrollbarFadingEnabled = true

        return webView
    }
}
