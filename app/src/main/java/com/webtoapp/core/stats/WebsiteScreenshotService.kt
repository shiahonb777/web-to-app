package com.webtoapp.core.stats

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

/**
 * 网站截图服务
 * 使用 enableSlowWholeDocumentDraw + 离屏 WebView 截图
 */
class WebsiteScreenshotService(private val context: Context) {
    
    companion object {
        private const val TAG = "WebsiteScreenshot"
        private const val SCREENSHOT_DIR = "screenshots"
        private const val SCREENSHOT_WIDTH = 540
        private const val SCREENSHOT_HEIGHT = 960
        private const val THUMBNAIL_QUALITY = 80
        private const val LOAD_TIMEOUT_MS = 20_000L
        private const val RENDER_DELAY_MS = 2000L
        
        private var slowDrawEnabled = false
        
        @Volatile
        private var INSTANCE: WebsiteScreenshotService? = null
        
        fun getInstance(context: Context): WebsiteScreenshotService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebsiteScreenshotService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val screenshotDir: File by lazy {
        File(context.filesDir, SCREENSHOT_DIR).also { it.mkdirs() }
    }
    
    fun getScreenshotPath(appId: Long): String {
        return File(screenshotDir, "app_${appId}.webp").absolutePath
    }
    
    fun hasScreenshot(appId: Long): Boolean {
        return File(getScreenshotPath(appId)).exists()
    }
    
    /**
     * 截取网站截图
     */
    suspend fun captureScreenshot(appId: Long, url: String): String? {
        val scheme = android.net.Uri.parse(url).scheme?.lowercase(java.util.Locale.ROOT)
        if (scheme != "http" && scheme != "https" && scheme != "file") {
            val invalidUrlMessage = "service skipped invalid url: appId=$appId, url=$url"
            AppLogger.i("ScreenshotFlow", invalidUrlMessage)
            Log.i("ScreenshotFlow", invalidUrlMessage)
            return null
        }
        
        return withContext(Dispatchers.Main) {
            try {
                val startMessage = "service start: appId=$appId, url=$url"
                AppLogger.i("ScreenshotFlow", startMessage)
                Log.i("ScreenshotFlow", startMessage)
                val isFileUrl = scheme == "file"
                // 启用完整文档渲染（离屏 WebView 必需）
                if (!slowDrawEnabled) {
                    try {
                        WebView.enableSlowWholeDocumentDraw()
                        slowDrawEnabled = true
                    } catch (_: Exception) {}
                }
                
                val pageLoaded = CompletableDeferred<Boolean>()
                
                val webView = WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.WHITE)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = isFileUrl
                        allowUniversalAccessFromFileURLs = isFileUrl
                        setSupportZoom(false)
                        cacheMode = WebSettings.LOAD_DEFAULT
                        blockNetworkImage = false
                        loadsImagesAutomatically = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                    }
                }
                
                // 先 measure + layout，让 WebView 有尺寸
                webView.measure(
                    View.MeasureSpec.makeMeasureSpec(SCREENSHOT_WIDTH, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(SCREENSHOT_HEIGHT, View.MeasureSpec.EXACTLY)
                )
                webView.layout(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT)
                
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, loadUrl: String?) {
                        super.onPageFinished(view, loadUrl)
                        val finishedMessage = "service page finished: appId=$appId, loadUrl=$loadUrl"
                        AppLogger.i("ScreenshotFlow", finishedMessage)
                        Log.i("ScreenshotFlow", finishedMessage)
                        if (pageLoaded.isActive) {
                            pageLoaded.complete(true)
                        }
                    }
                    
                    @Suppress("DEPRECATION")
                    override fun onReceivedError(
                        view: WebView?, errorCode: Int,
                        description: String?, failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        AppLogger.w(TAG, "页面加载出错: $failingUrl → $description")
                        val errorMessage = "service page error: appId=$appId, code=$errorCode, failingUrl=$failingUrl, description=$description"
                        AppLogger.i("ScreenshotFlow", errorMessage)
                        Log.i("ScreenshotFlow", errorMessage)
                        if (pageLoaded.isActive) {
                            pageLoaded.complete(false)
                        }
                    }
                }
                
                AppLogger.d(TAG, "开始加载: $url")
                val loadMessage = "service loadUrl: appId=$appId, url=$url"
                AppLogger.i("ScreenshotFlow", loadMessage)
                Log.i("ScreenshotFlow", loadMessage)
                webView.loadUrl(url)
                
                // 等待页面加载完成或超时
                val loadCompleted = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                    pageLoaded.await()
                }
                val waitMessage = "service wait finished: appId=$appId, pageLoaded=$loadCompleted"
                AppLogger.i("ScreenshotFlow", waitMessage)
                Log.i("ScreenshotFlow", waitMessage)
                
                // 无论成功与否，等 2 秒让内容渲染
                delay(RENDER_DELAY_MS)
                
                // 重新 layout 后截图
                val path = doCapture(webView, appId)
                destroyWebView(webView)
                val resultMessage = "service capture finished: appId=$appId, path=$path"
                AppLogger.i("ScreenshotFlow", resultMessage)
                Log.i("ScreenshotFlow", resultMessage)
                path
            } catch (e: Exception) {
                AppLogger.e(TAG, "截图失败: $url → ${e.message}")
                val exceptionMessage = "service exception: appId=$appId, url=$url, error=${e.message}"
                AppLogger.e("ScreenshotFlow", exceptionMessage, e)
                Log.e("ScreenshotFlow", exceptionMessage, e)
                null
            }
        }
    }
    
    /**
     * 执行截图：measure → layout → draw → save
     */
    private fun doCapture(webView: WebView, appId: Long): String? {
        return try {
            // 重新 measure + layout 确保最新内容
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(SCREENSHOT_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(SCREENSHOT_HEIGHT, View.MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT)
            
            val bitmap = Bitmap.createBitmap(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            webView.draw(canvas)
            
            // 检查是否全白（空白截图）
            val pixel = bitmap.getPixel(SCREENSHOT_WIDTH / 2, SCREENSHOT_HEIGHT / 2)
            if (pixel == android.graphics.Color.WHITE.toInt()) {
                val pixel2 = bitmap.getPixel(SCREENSHOT_WIDTH / 4, SCREENSHOT_HEIGHT / 4)
                if (pixel2 == android.graphics.Color.WHITE.toInt()) {
                    AppLogger.w(TAG, "截图疑似空白，仍然保存")
                }
            }
            
            saveBitmap(appId, bitmap)
        } catch (e: Exception) {
            AppLogger.e(TAG, "捕获 Bitmap 失败: ${e.message}")
            null
        }
    }
    
    private fun destroyWebView(webView: WebView) {
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (_: Exception) {}
    }
    
    private fun saveBitmap(appId: Long, bitmap: Bitmap): String? {
        return try {
            val file = File(getScreenshotPath(appId))
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, THUMBNAIL_QUALITY, fos)
                fos.flush()
            }
            bitmap.recycle()
            AppLogger.d(TAG, "截图已保存: ${file.absolutePath} (${file.length()} bytes)")
            file.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存截图失败: ${e.message}")
            bitmap.recycle()
            null
        }
    }
    
    fun deleteScreenshot(appId: Long) {
        try { File(getScreenshotPath(appId)).delete() } catch (_: Exception) {}
    }
    
    fun clearAllScreenshots() {
        screenshotDir.listFiles()?.forEach { it.delete() }
    }
}
