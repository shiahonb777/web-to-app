package com.webtoapp.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger

/**
 * WebView 预热池
 * 
 * WebView 首次初始化在 Android 上需要 200-500ms（加载 libwebviewchromium.so、初始化 V8 等）。
 * 通过在 Application 启动时预创建 WebView 实例，可以将用户打开应用时的等待时间降至接近 0。
 * 
 * 使用方式：
 * 1. Application.onCreate() 中调用 WebViewPool.prewarm(context)
 * 2. Activity 中调用 WebViewPool.acquire(context) 代替 WebView(context)
 * 3. Activity.onDestroy() 中调用 WebViewPool.recycle(webView) 回收（可选）
 */
object WebViewPool {
    
    private const val TAG = "WebViewPool"
    private const val MAX_POOL_SIZE = 2
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pool = ArrayDeque<WebView>(MAX_POOL_SIZE)
    
    @Volatile
    private var isPrewarmed = false
    
    /**
     * 预热 WebView 引擎
     * 必须在主线程调用或通过 Handler post 到主线程
     * 建议在 Application.onCreate() 中调用
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
     * 获取一个 WebView 实例
     * 优先从池中获取预热的实例，如果池为空则新建
     * 
     * @param context 目标 Activity 的 Context（会替换预热时的 ApplicationContext）
     * @return WebView 实例
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun acquire(context: Context): WebView {
        val webView = synchronized(pool) {
            pool.removeLastOrNull()
        }
        
        return if (webView != null) {
            // 替换 Context 为目标 Activity 的 Context
            // 预热时使用 MutableContextWrapper，这里替换为实际 Context
            (webView.context as? MutableContextWrapper)?.baseContext = context
            AppLogger.d(TAG, "WebView acquired from pool (remaining: ${pool.size})")
            
            // 异步补充池
            replenishPool(context.applicationContext)
            
            webView
        } else {
            AppLogger.d(TAG, "Pool empty, creating new WebView")
            WebView(context)
        }
    }
    
    /**
     * 回收 WebView 到池中（可选）
     * 如果池已满或 WebView 状态不佳，则直接销毁
     */
    fun recycle(webView: WebView) {
        try {
            // 清理状态
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
            
            // 池已满，销毁
            webView.destroy()
            AppLogger.d(TAG, "Pool full, WebView destroyed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "WebView recycle failed, destroying", e)
            try { webView.destroy() } catch (_: Exception) {}
        }
    }
    
    /**
     * 释放池中所有 WebView
     * 在 Application.onTerminate() 中调用
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
     * 获取当前池大小（用于调试/监控）
     */
    fun poolSize(): Int = synchronized(pool) { pool.size }
    
    /**
     * 异步补充池中的 WebView
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
     * 创建预热的 WebView 实例
     * 使用 MutableContextWrapper 以便后续替换为 Activity Context
     * 预配置更多设置以减少后续 configureWebView 的开销
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun createPrewarmedWebView(appContext: Context): WebView {
        val contextWrapper = MutableContextWrapper(appContext)
        val webView = WebView(contextWrapper)
        
        // 预配置常用设置 — 减少后续 configureWebView 的设置项数量
        // WebViewManager.configureWebView 会覆盖详细配置
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            // 预设高频使用的设置 (减少后续主线程配置时间 ~20-50ms)
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            @Suppress("DEPRECATION")
            setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                offscreenPreRaster = true
            }
        }
        
        // 预设硬件加速层
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        webView.isScrollbarFadingEnabled = true
        
        return webView
    }
}
