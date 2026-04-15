package com.webtoapp.util

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URI

/**
 * URL
 *
 * URL ：
 * - （<title> og:title）
 * - Favicon URL
 * - （theme-color / og:color）
 */
object UrlMetadataFetcher {

    private const val TAG = "UrlMetadataFetcher"

    // HTML （）
    private const val MAX_HTML_SIZE = 2 * 1024 * 1024 // 2MB

    // User-Agent
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    // Note.
    private val META_OG_TITLE_REGEX = Regex(
        """<meta[^>]*property\s*=\s*["']og:title["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val META_OG_TITLE_REGEX_2 = Regex(
        """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]*property\s*=\s*["']og:title["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val TITLE_TAG_REGEX = Regex(
        """<title[^>]*>([^<]+)</title>""",
        RegexOption.IGNORE_CASE
    )
    private val META_THEME_COLOR_REGEX = Regex(
        """<meta[^>]*name\s*=\s*["']theme-color["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val META_THEME_COLOR_REGEX_2 = Regex(
        """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]*name\s*=\s*["']theme-color["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val META_OG_COLOR_REGEX = Regex(
        """<meta[^>]*property\s*=\s*["']og:color["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val META_OG_COLOR_REGEX_2 = Regex(
        """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]*property\s*=\s*["']og:color["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val LINK_ICON_REGEX = Regex(
        """<link[^>]*rel\s*=\s*["'](?:icon|shortcut icon|apple-touch-icon)["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val HREF_REGEX = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val SIZES_REGEX = Regex("""sizes\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val ICON_SIZE_REGEX = Regex("""(\d+)x(\d+)""")

    private val client get() = NetworkModule.defaultClient

    /**
     * Note.
     */
    data class Metadata(
        val title: String = "",
        val faviconUrl: String = "",
        val themeColor: String = ""
    )

    /**
     * URL
     *
     * @param url parameter
     * @return result
     */
    suspend fun fetch(url: String): Metadata = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url)
        val baseUrl = getBaseUrl(normalizedUrl)

        AppLogger.d(TAG, "Fetching metadata for: $baseUrl")

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLogger.w(TAG, "HTTP ${response.code} for $normalizedUrl")
                return@withContext Metadata()
            }

            val body = response.body ?: return@withContext Metadata()
            val contentLength = body.contentLength()
            val html = if (contentLength > 0 && contentLength <= MAX_HTML_SIZE) {
                body.string()
            } else {
                // ， 2MB
                val bytes = body.byteStream().use { stream ->
                    stream.readNBytes(MAX_HTML_SIZE)
                }
                String(bytes, Charsets.UTF_8)
            }

            if (html.isBlank()) {
                return@withContext Metadata()
            }

            val title = extractTitle(html)
            val faviconUrl = extractFaviconUrl(html, baseUrl)
            val themeColor = extractThemeColor(html)

            AppLogger.d(TAG, "Metadata extracted — title: $title, favicon: $faviconUrl, color: $themeColor")

            Metadata(title = title, faviconUrl = faviconUrl, themeColor = themeColor)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to fetch metadata for $normalizedUrl", e)
            Metadata()
        }
    }

    /**
     * （ og:title， <title>）
     */
    private fun extractTitle(html: String): String {
        META_OG_TITLE_REGEX.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return decodeHtmlEntities(it) }
        META_OG_TITLE_REGEX_2.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return decodeHtmlEntities(it) }
        TITLE_TAG_REGEX.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return decodeHtmlEntities(it.trim()) }
        return ""
    }

    /**
     * （ theme-color， og:color）
     */
    private fun extractThemeColor(html: String): String {
        META_THEME_COLOR_REGEX.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        META_THEME_COLOR_REGEX_2.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        META_OG_COLOR_REGEX.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        META_OG_COLOR_REGEX_2.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        return ""
    }

    /**
     * favicon URL（， favicon.ico）
     */
    private fun extractFaviconUrl(html: String, baseUrl: String): String {
        // icon link，
        val icons = mutableListOf<Pair<String, Int>>()

        LINK_ICON_REGEX.findAll(html).forEach { linkMatch ->
            val linkTag = linkMatch.value
            val hrefMatch = HREF_REGEX.find(linkTag) ?: return@forEach
            val href = hrefMatch.groupValues[1].takeIf { it.isNotBlank() } ?: return@forEach

            val resolved = resolveIconUrl(baseUrl, href)
            var size = 0

            SIZES_REGEX.find(linkTag)?.let { sizesMatch ->
                val sizesStr = sizesMatch.groupValues[1]
                if (sizesStr.equals("any", ignoreCase = true)) {
                    size = 512
                } else {
                    ICON_SIZE_REGEX.find(sizesStr)?.let { sizeMatch ->
                        size = sizeMatch.groupValues[1].toIntOrNull() ?: 0
                    }
                }
            }

            // sizes size ，
            if (size == 0) {
                size = guessSizeFromFilename(href)
            }

            icons.add(resolved to size)
        }

        // Note.
        icons.maxByOrNull { it.second }?.let { return it.first }

        // ：favicon.ico
        return "$baseUrl/favicon.ico"
    }

    /**
     * Note.
     */
    private fun guessSizeFromFilename(href: String): Int {
        val lower = href.lowercase()
        return when {
            lower.contains("192") -> 192
            lower.contains("180") -> 180
            lower.contains("152") -> 152
            lower.contains("144") -> 144
            lower.contains("128") -> 128
            lower.contains("96") -> 96
            lower.contains("64") -> 64
            lower.contains("48") -> 48
            lower.contains("32") -> 32
            lower.contains("16") -> 16
            lower.contains("apple-touch-icon") -> 180
            lower.contains("favicon") && lower.endsWith(".ico") -> 16
            else -> 0
        }
    }

    /**
     * URL（）
     */
    private fun resolveIconUrl(baseUrl: String, href: String): String {
        return when {
            href.startsWith("http://") || href.startsWith("https://") -> upgradeHttpToHttps(href)
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> "$baseUrl$href"
            else -> "$baseUrl/$href"
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
        return upgradeHttpToHttps(normalized)
    }

    /**
     * URL
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
     * HTTP URL HTTPS（ HTTP）
     */
    private fun upgradeHttpToHttps(url: String): String {
        if (!url.startsWith("http://")) return url
        return try {
            val uri = URI(url)
            val host = uri.host ?: return url
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

    /**
     * HTML
     */
    private fun decodeHtmlEntities(str: String): String {
        return str
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
    }
}
