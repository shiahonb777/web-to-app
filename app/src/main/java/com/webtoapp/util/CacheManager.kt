package com.webtoapp.util

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.webtoapp.util.toFileSizeString
import java.io.File





object CacheManager {

    private const val TAG = "CacheManager"


    private const val CACHE_SIZE_THRESHOLD = 500L * 1024 * 1024





    suspend fun autoCleanIfNeeded(context: Context, threshold: Long = CACHE_SIZE_THRESHOLD): Boolean {
        val cacheInfo = getCacheInfo(context)
        if (cacheInfo.totalSize > threshold) {
            AppLogger.d(TAG, "缓存超过阈值 (${cacheInfo.formatTotalSize()})\uff0c开始自动清理")
            clearAllCache(context)
            return true
        }
        return false
    }




    data class CacheInfo(
        val webViewCacheSize: Long,
        val appCacheSize: Long,
        val databaseSize: Long,
        val totalSize: Long
    ) {
        fun formatTotalSize(): String = formatSize(totalSize)
        fun formatWebViewCacheSize(): String = formatSize(webViewCacheSize)
        fun formatAppCacheSize(): String = formatSize(appCacheSize)
        fun formatDatabaseSize(): String = formatSize(databaseSize)
    }




    suspend fun getCacheInfo(context: Context): CacheInfo = withContext(Dispatchers.IO) {
        val webViewCacheSize = getDirectorySize(File(context.cacheDir, "WebView"))
        val appCacheSize = getDirectorySize(context.cacheDir) - webViewCacheSize
        val databaseSize = getDirectorySize(context.getDatabasePath("webview.db").parentFile)

        CacheInfo(
            webViewCacheSize = webViewCacheSize,
            appCacheSize = appCacheSize,
            databaseSize = databaseSize,
            totalSize = webViewCacheSize + appCacheSize + databaseSize
        )
    }




    suspend fun clearAllCache(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {

            clearWebViewCache(context)


            clearDirectory(context.cacheDir)


            context.externalCacheDir?.let { clearDirectory(it) }

            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除缓存失败", e)
            false
        }
    }





    suspend fun clearWebViewCache(context: Context) {
        try {
            withContext(Dispatchers.IO) {
                val webViewCacheDir = File(context.cacheDir, "WebView")
                if (webViewCacheDir.exists()) {
                    clearDirectory(webViewCacheDir)
                }
            }
            AppLogger.d(TAG, "WebView 缓存已清除")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除 WebView 缓存失败", e)
        }
    }




    fun clearCookies() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            AppLogger.d(TAG, "Cookie 已清除")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除 Cookie 失败", e)
        }
    }




    fun clearWebStorage() {
        try {
            WebStorage.getInstance().deleteAllData()
            AppLogger.d(TAG, "WebStorage 已清除")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除 WebStorage 失败", e)
        }
    }




    fun clearCookiesForDomain(domain: String) {
        try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(domain)

            if (cookies != null) {
                val cookieArray = cookies.split(";")
                for (cookie in cookieArray) {
                    val cookieName = cookie.split("=")[0].trim()
                    cookieManager.setCookie(domain, "$cookieName=; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
                }
                cookieManager.flush()
            }

            AppLogger.d(TAG, "域名 $domain 的 Cookie 已清除")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除域名 Cookie 失败", e)
        }
    }




    fun clearHistory(webView: WebView) {
        try {
            webView.clearHistory()
            AppLogger.d(TAG, "历史记录已清除")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除历史记录失败", e)
        }
    }




    @Suppress("DEPRECATION")
    fun clearFormData(webView: WebView) {
        try {
            webView.clearFormData()
            AppLogger.d(TAG, "表单数据已清除")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除表单数据失败", e)
        }
    }




    private fun getDirectorySize(directory: File?): Long {
        if (directory == null || !directory.exists()) return 0

        var size = 0L
        try {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "计算目录大小失败", e)
        }
        return size
    }




    private fun clearDirectory(directory: File): Boolean {
        if (!directory.exists()) return true

        return try {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "清除目录失败: ${directory.path}", e)
            false
        }
    }





    private fun formatSize(bytes: Long): String = bytes.toFileSizeString()
}
