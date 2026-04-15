package com.webtoapp.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger

/**
 * WebView prewarm pool.
 *
 * WebView in Android 200-500ms.
 * in Application when WebView can use use when etc when 0.
 *
 * use .
 * 1. Application.onCreate() in use WebViewPool.prewarm(context)
 * 2. Activity in use WebViewPool.acquire(context) WebView(context)
 * 3. Activity.onDestroy() in use WebViewPool.recycle(webView) recycle.
 */
object WebViewPool {
    
    private const val TAG = "WebViewPool"
    private const val MAX_POOL_SIZE = 2
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pool = ArrayDeque<WebView>(MAX_POOL_SIZE)
    
    @Volatile
    private var isPrewarmed = false
    
    /**
     * prewarm WebView.
     * in use or Handler post to.
     * in Application.onCreate() in use.
     */
    fun prewarm(context: Context) {
        if (isPrewarmed) return
        isPrewarmed = true
        
        val appContext = context.applicationContext
        
        // Post to main thread to avoid blocking Application.onCreate()
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
    
    /**
     * Get WebView.
     * from in Getprewarm as.
     *
     * @param context Activity Context.
     * @return WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun acquire(context: Context): WebView {
        val webView = synchronized(pool) {
            pool.removeLastOrNull()
        }
        
        return if (webView != null) {
            // Context as Activity Context.
            // prewarm when use MutableContextWrapper as Context.
            (webView.context as? MutableContextWrapper)?.baseContext = context
            AppLogger.d(TAG, "WebView acquired from pool (remaining: ${pool.size})")
            
            // Refill asynchronously.
            replenishPool(context.applicationContext)
            
            webView
        } else {
            AppLogger.d(TAG, "Pool empty, creating new WebView")
            WebView(context)
        }
    }
    
    /**
     * recycle WebView to in.
     * or WebView not .
     */
    fun recycle(webView: WebView) {
        try {
            // Reset state.
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
            
            // Destroy when pool is full.
            webView.destroy()
            AppLogger.d(TAG, "Pool full, WebView destroyed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "WebView recycle failed, destroying", e)
            try { webView.destroy() } catch (_: Exception) {}
        }
    }
    
    /**
     * Release in WebView.
     * in Application.onTerminate() in use.
     */
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
    
    /**
     * Get before large small.
     */
    fun poolSize(): Int = synchronized(pool) { pool.size }
    
    /**
     * Refill asynchronously in WebView.
     */
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
    
    /**
     * prewarm WebView.
     * use MutableContextWrapper after as Activity Context.
     * config multiple after configureWebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun createPrewarmedWebView(appContext: Context): WebView {
        val contextWrapper = MutableContextWrapper(appContext)
        val webView = WebView(contextWrapper)
        
        // config use after configureWebView.
        // WebViewManager.configureWebView config.
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            // use.
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
