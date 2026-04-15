package com.webtoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.webtoapp.core.network.NetworkModule
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/**
 * Note.
 * 
 * ：
 * 1. /favicon.ico
 * 2. HTML link
 * 3. Google Favicon
 */
object FaviconFetcher {
    
    private const val TAG = "FaviconFetcher"
    
    // Min（）
    private const val MIN_ICON_SIZE = 32
    
    // Note.
    private const val PREFERRED_ICON_SIZE = 192
    
    // Pre-compiled regex for parsing icon sizes like "192x192"
    private val ICON_SIZE_REGEX = Regex("(\\d+)x(\\d+)")
    
    // Pre-compiled regex for HTML icon link parsing (avoid Pattern.compile per call)
    private val LINK_ICON_REGEX = Regex("""<link[^>]*rel\s*=\s*["'](?:icon|shortcut icon|apple-touch-icon)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val HREF_REGEX = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val SIZES_REGEX = Regex("""sizes\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val OG_IMAGE_REGEX = Regex("""<meta[^>]*property\s*=\s*["']og:image["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    
    private val client get() = NetworkModule.defaultClient
    
    /**
     * Note.
     * 
     * @param context parameter
     * @param url parameter
     * @return result
     */
    suspend fun fetchFavicon(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            val baseUrl = getBaseUrl(normalizedUrl)
            
            AppLogger.d(TAG, "开始获取网站图标: $baseUrl")
            
            // 1: HTML
            val htmlIcons = tryParseHtmlForIcons(normalizedUrl)
            if (htmlIcons.isNotEmpty()) {
                // ，
                val sortedIcons = htmlIcons.sortedByDescending { it.size }
                for (iconInfo in sortedIcons) {
                    val iconUrl = resolveIconUrl(baseUrl, iconInfo.href)
                    val savedPath = downloadAndSaveIcon(context, iconUrl)
                    if (savedPath != null) {
                        AppLogger.d(TAG, "从 HTML 获取图标成功: ${iconInfo.href}")
                        return@withContext savedPath
                    }
                }
            }
            
            // 2: favicon.ico
            val faviconUrl = "$baseUrl/favicon.ico"
            val faviconPath = downloadAndSaveIcon(context, faviconUrl)
            if (faviconPath != null) {
                AppLogger.d(TAG, "从 favicon.ico 获取图标成功")
                return@withContext faviconPath
            }
            
            // 3: Google Favicon （）
            val googleFaviconUrl = "https://www.google.com/s2/favicons?sz=128&domain_url=$baseUrl"
            val googlePath = downloadAndSaveIcon(context, googleFaviconUrl)
            if (googlePath != null) {
                AppLogger.d(TAG, "从 Google Favicon 服务获取图标成功")
                return@withContext googlePath
            }
            
            AppLogger.w(TAG, "无法获取网站图标: $url")
            null
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "获取网站图标失败: $url", e)
            null
        }
    }
    
    /**
     * Note.
     */
    private data class IconInfo(
        val href: String,
        val size: Int = 0,
        val type: String = ""
    )
    
    /**
     * HTML
     */
    private fun tryParseHtmlForIcons(url: String): List<IconInfo> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            
            val html = response.body?.string() ?: return emptyList()
            val icons = mutableListOf<IconInfo>()
            
            // link
            LINK_ICON_REGEX.findAll(html).forEach { linkMatch ->
                val linkTag = linkMatch.value
                
                val hrefMatch = HREF_REGEX.find(linkTag)
                if (hrefMatch != null) {
                    val href = hrefMatch.groupValues[1].takeIf { it.isNotBlank() } ?: return@forEach
                    
                    var size = 0
                    SIZES_REGEX.find(linkTag)?.let { sizesMatch ->
                        val sizesStr = sizesMatch.groupValues[1]
                        ICON_SIZE_REGEX.find(sizesStr)?.let { sizeMatch ->
                            size = sizeMatch.groupValues[1].toIntOrNull() ?: 0
                        }
                    }
                    
                    // Filter
                    if (size == 0 || size >= MIN_ICON_SIZE) {
                        icons.add(IconInfo(href, size))
                    }
                }
            }
            
            // meta og:image（）
            OG_IMAGE_REGEX.find(html)?.let { ogMatch ->
                val ogImage = ogMatch.groupValues[1]
                if (ogImage.isNotBlank()) {
                    icons.add(IconInfo(ogImage, PREFERRED_ICON_SIZE))
                }
            }
            
            icons
            
        } catch (e: Exception) {
            AppLogger.w(TAG, "解析 HTML 获取图标失败", e)
            emptyList()
        }
    }
    
    /**
     * Note.
     */
    private fun downloadAndSaveIcon(context: Context, iconUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(iconUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            
            val bytes = response.body?.bytes() ?: return null
            if (bytes.isEmpty()) return null
            
            // Verify
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                AppLogger.w(TAG, "无效的图片: $iconUrl")
                return null
            }
            
            // Check
            if (options.outWidth < MIN_ICON_SIZE && options.outHeight < MIN_ICON_SIZE) {
                AppLogger.w(TAG, "图标太小: ${options.outWidth}x${options.outHeight}")
                return null
            }
            
            // Save
            val iconsDir = File(context.filesDir, "website_icons")
            iconsDir.mkdirs()
            
            val fileName = "favicon_${System.currentTimeMillis()}.png"
            val outputFile = File(iconsDir, fileName)
            
            // PNG（）
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                AppLogger.w(TAG, "无法解码图片: $iconUrl")
                return null
            }
            
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            
            AppLogger.d(TAG, "图标已保存: ${outputFile.absolutePath} (${options.outWidth}x${options.outHeight})")
            outputFile.absolutePath
            
        } catch (e: Exception) {
            AppLogger.w(TAG, "下载图标失败: $iconUrl", e)
            null
        }
    }
    
    /**
     * URL
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        return upgradeRemoteHttpToHttps(normalized)
    }
    
    /**
     * URL（ + + ）
     */
    private fun getBaseUrl(url: String): String {
        return try {
            val uri = URI(url)
            val port = uri.port
            val host = uri.host ?: return url
            if (port != -1 && port != 80 && port != 443) {
                "${uri.scheme}://$host:$port"
            } else {
                "${uri.scheme}://$host"
            }
        } catch (e: Exception) {
            url.substringBefore("/", url).let {
                if (it.contains("://")) it.substringBefore("/", it)
                else url
            }
        }
    }
    
    /**
     * URL（）
     */
    private fun resolveIconUrl(baseUrl: String, href: String): String {
        val resolved = when {
            href.startsWith("http://") || href.startsWith("https://") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> "$baseUrl$href"
            else -> "$baseUrl/$href"
        }
        return upgradeRemoteHttpToHttps(resolved)
    }

    /**
     * HTTP URL HTTPS（ HTTP）
     */
    private fun upgradeRemoteHttpToHttps(url: String): String {
        if (!url.startsWith("http://")) return url
        return try {
            val uri = URI(url)
            val host = uri.host ?: return url
            // Note.
            if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0"
                || host.startsWith("192.168.") || host.startsWith("10.")
                || host.endsWith(".local")) {
                url
            } else {
                url.replaceFirst("http://", "https://")
            }
        } catch (e: Exception) {
            url
        }
    }
}
