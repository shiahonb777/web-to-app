package com.webtoapp.util

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 缓存管理工具类
 * 提供 WebView 缓存、Cookie、本地存储等的管理功能
 */
object CacheManager {
    
    // Cache大小阈值（默认 500MB）
    private const val CACHE_SIZE_THRESHOLD = 500L * 1024 * 1024
    
    /**
     * 检查并自动清理过大的缓存
     * @return 是否执行了清理
     */
    suspend fun autoCleanIfNeeded(context: Context, threshold: Long = CACHE_SIZE_THRESHOLD): Boolean {
        val cacheInfo = getCacheInfo(context)
        if (cacheInfo.totalSize > threshold) {
            android.util.Log.d("CacheManager", "缓存超过阈值 (${cacheInfo.formatTotalSize()})，开始自动清理")
            clearAllCache(context)
            return true
        }
        return false
    }
    
    /**
     * 缓存信息
     */
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
    
    /**
     * 获取缓存信息
     */
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
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // 清除 WebView 缓存（内部会切换到主线程）
            clearWebViewCache(context)
            
            // 清除应用缓存目录
            clearDirectory(context.cacheDir)
            
            // 清除外部缓存
            context.externalCacheDir?.let { clearDirectory(it) }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清除缓存失败", e)
            false
        }
    }
    
    /**
     * 清除 WebView 缓存
     * 注意：必须在主线程调用，因为 WebView 只能在主线程创建
     */
    suspend fun clearWebViewCache(context: Context) {
        try {
            // WebView 必须在主线程创建和操作
            withContext(Dispatchers.Main) {
                try {
                    WebView(context).apply {
                        clearCache(true)
                        destroy()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CacheManager", "WebView clearCache 失败", e)
                }
            }
            
            // File操作可以在 IO 线程
            withContext(Dispatchers.IO) {
                val webViewCacheDir = File(context.cacheDir, "WebView")
                if (webViewCacheDir.exists()) {
                    clearDirectory(webViewCacheDir)
                }
            }
            
            android.util.Log.d("CacheManager", "WebView 缓存已清除")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清除 WebView 缓存失败", e)
        }
    }
    
    /**
     * 清除 Cookie
     */
    fun clearCookies() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            android.util.Log.d("CacheManager", "Cookie 已清除")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清除 Cookie 失败", e)
        }
    }
    
    /**
     * 清除 WebStorage（localStorage、sessionStorage、IndexedDB）
     */
    fun clearWebStorage() {
        try {
            WebStorage.getInstance().deleteAllData()
            android.util.Log.d("CacheManager", "WebStorage 已清除")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清除 WebStorage 失败", e)
        }
    }
    
    /**
     * 清除指定域名的 Cookie
     */
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
            
            android.util.Log.d("CacheManager", "域名 $domain 的 Cookie 已清除")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清除域名 Cookie 失败", e)
        }
    }
    
    /**
     * 清除 WebView 历史记录
     */
    fun clearHistory(webView: WebView) {
        try {
            webView.clearHistory()
            android.util.Log.d("CacheManager", "历史记录已清除")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清除历史记录失败", e)
        }
    }
    
    /**
     * 清除 WebView 表单数据
     */
    @Suppress("DEPRECATION")
    fun clearFormData(webView: WebView) {
        try {
            webView.clearFormData()
            android.util.Log.d("CacheManager", "表单数据已清除")
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清除表单数据失败", e)
        }
    }
    
    /**
     * 获取目录大小
     */
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
            android.util.Log.e("CacheManager", "计算目录大小失败", e)
        }
        return size
    }
    
    /**
     * 清除目录内容（保留目录本身）
     */
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
            android.util.Log.e("CacheManager", "清除目录失败: ${directory.path}", e)
            false
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
