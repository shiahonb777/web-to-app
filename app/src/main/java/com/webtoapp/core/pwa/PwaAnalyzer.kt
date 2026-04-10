package com.webtoapp.core.pwa

import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * PWA 网站分析器
 *
 * 分析目标网站的 PWA 配置（manifest.json、meta 标签），
 * 提取应用名称、图标、主题色、显示模式等信息，用于一键填充 App 配置。
 *
 * 工作流程:
 * 1. 下载目标网站 HTML
 * 2. 在 HTML 中查找 <link rel="manifest"> 标签
 * 3. 下载并解析 manifest.json
 * 4. 如果没有 manifest，从 HTML meta 标签提取信息（fallback）
 * 5. 返回 PwaAnalysisResult
 */
object PwaAnalyzer {

    private const val TAG = "PwaAnalyzer"
    private const val CONNECT_TIMEOUT = 10_000  // 10s
    private const val READ_TIMEOUT = 15_000     // 15s
    private const val MAX_HTML_SIZE = 512 * 1024 // 512 KB — 只需 <head> 部分

    /**
     * 分析指定 URL 的 PWA 配置
     * 
     * @param url 目标网站 URL
     * @return PwaAnalysisResult 分析结果
     */
    suspend fun analyze(url: String): PwaAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            AppLogger.i(TAG, "Starting PWA analysis for: $normalizedUrl")

            // Step 1: 下载 HTML
            val html = downloadHtml(normalizedUrl)
            if (html.isNullOrBlank()) {
                AppLogger.w(TAG, "Failed to download HTML from $normalizedUrl")
                return@withContext PwaAnalysisResult(
                    errorMessage = "无法访问网站"
                )
            }

            // Step 2: 查找 manifest.json URL
            val manifestUrl = extractManifestUrl(html, normalizedUrl)
            AppLogger.d(TAG, "Manifest URL: $manifestUrl")

            // Step 3: 尝试下载并解析 manifest.json
            if (manifestUrl != null) {
                val manifestJson = downloadJson(manifestUrl)
                if (manifestJson != null) {
                    val manifest = parseManifest(manifestJson, normalizedUrl)
                    AppLogger.i(TAG, "Manifest parsed: name=${manifest.name}, icons=${manifest.icons.size}")
                    return@withContext buildResultFromManifest(manifest, normalizedUrl)
                }
            }

            // Step 4: Fallback — 从 HTML meta 标签提取
            AppLogger.d(TAG, "No manifest found, falling back to meta tags")
            return@withContext extractFromMetaTags(html, normalizedUrl)

        } catch (e: Exception) {
            AppLogger.e(TAG, "PWA analysis failed", e)
            PwaAnalysisResult(
                errorMessage = "分析失败: ${e.message}"
            )
        }
    }

    // ═══════════════════════════════════════════
    //  URL 处理
    // ═══════════════════════════════════════════

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }

    /**
     * 解析相对 URL 为绝对 URL
     */
    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) {
            return relative
        }
        return try {
            URL(URL(base), relative).toString()
        } catch (e: Exception) {
            // 手动拼接
            val baseUrl = URL(base)
            if (relative.startsWith("/")) {
                "${baseUrl.protocol}://${baseUrl.host}${if (baseUrl.port > 0 && baseUrl.port != baseUrl.defaultPort) ":${baseUrl.port}" else ""}$relative"
            } else {
                val basePath = base.substringBeforeLast("/")
                "$basePath/$relative"
            }
        }
    }

    /**
     * 从 URL 中提取域名
     */
    fun extractHost(url: String): String? {
        return try {
            URL(normalizeUrl(url)).host
        } catch (e: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════
    //  网络下载
    // ═══════════════════════════════════════════

    private fun downloadHtml(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")

            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    AppLogger.w(TAG, "HTML download failed: HTTP $code")
                    return null
                }

                // 只读取前 MAX_HTML_SIZE 字节（我们只需要 <head> 部分）
                val stream = conn.inputStream
                val bytes = ByteArray(MAX_HTML_SIZE)
                var totalRead = 0
                var read: Int
                while (totalRead < MAX_HTML_SIZE) {
                    read = stream.read(bytes, totalRead, MAX_HTML_SIZE - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(bytes, 0, totalRead, Charsets.UTF_8)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to download HTML: ${e.message}")
            null
        }
    }

    private fun downloadJson(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "application/json,application/manifest+json,*/*")

            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    AppLogger.w(TAG, "JSON download failed: HTTP $code from $url")
                    return null
                }
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to download JSON from $url: ${e.message}")
            null
        }
    }

    // ═══════════════════════════════════════════
    //  HTML 解析
    // ═══════════════════════════════════════════

    /**
     * 从 HTML 中提取 manifest.json 的 URL
     * 查找: <link rel="manifest" href="/manifest.json">
     */
    private fun extractManifestUrl(html: String, baseUrl: String): String? {
        // 正则匹配 <link rel="manifest" href="...">
        // 支持属性顺序不同的情况
        val patterns = listOf(
            // rel 在 href 前
            """<link\s+[^>]*rel\s*=\s*["']manifest["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*/?>""".toRegex(RegexOption.IGNORE_CASE),
            // href 在 rel 前
            """<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']manifest["'][^>]*/?>""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                val href = match.groupValues[1]
                return resolveUrl(baseUrl, href)
            }
        }

        // 尝试常见路径
        val commonPaths = listOf(
            "/manifest.json",
            "/manifest.webmanifest",
            "/site.webmanifest"
        )
        for (path in commonPaths) {
            val url = resolveUrl(baseUrl, path)
            val json = downloadJson(url)
            if (json != null && json.trim().startsWith("{")) {
                return url
            }
        }

        return null
    }

    // ═══════════════════════════════════════════
    //  Manifest 解析
    // ═══════════════════════════════════════════

    /**
     * 解析 manifest.json
     */
    private fun parseManifest(jsonStr: String, baseUrl: String): PwaManifest {
        val json = JSONObject(jsonStr)

        val icons = mutableListOf<PwaIcon>()
        json.optJSONArray("icons")?.let { iconsArray ->
            for (i in 0 until iconsArray.length()) {
                val iconJson = iconsArray.getJSONObject(i)
                val src = iconJson.optString("src", "")
                if (src.isNotBlank()) {
                    icons.add(PwaIcon(
                        src = resolveUrl(baseUrl, src),
                        sizes = iconJson.optString("sizes", null),
                        type = iconJson.optString("type", null),
                        purpose = iconJson.optString("purpose", null)
                    ))
                }
            }
        }

        return PwaManifest(
            name = json.optStringOrNull("name"),
            shortName = json.optStringOrNull("short_name"),
            startUrl = json.optStringOrNull("start_url")?.let { resolveUrl(baseUrl, it) },
            scope = json.optStringOrNull("scope")?.let { resolveUrl(baseUrl, it) },
            display = json.optStringOrNull("display"),
            themeColor = json.optStringOrNull("theme_color")?.normalizeColor(),
            backgroundColor = json.optStringOrNull("background_color")?.normalizeColor(),
            icons = icons,
            orientation = json.optStringOrNull("orientation"),
            description = json.optStringOrNull("description"),
            lang = json.optStringOrNull("lang"),
            dir = json.optStringOrNull("dir")
        )
    }

    /**
     * 从 manifest 构建分析结果
     */
    private fun buildResultFromManifest(manifest: PwaManifest, baseUrl: String): PwaAnalysisResult {
        // 选择最佳图标：优先 maskable 的大图标，其次 any，最后最大的
        val bestIcon = manifest.icons
            .sortedWith(compareByDescending<PwaIcon> { it.purpose?.contains("maskable") == true }
                .thenByDescending { it.maxSizePixels })
            .firstOrNull()

        return PwaAnalysisResult(
            isPwa = true,
            suggestedName = manifest.shortName ?: manifest.name,
            suggestedIconUrl = bestIcon?.src,
            suggestedThemeColor = manifest.themeColor,
            suggestedBackgroundColor = manifest.backgroundColor,
            suggestedDisplay = manifest.display,
            suggestedOrientation = manifest.orientation,
            startUrl = manifest.startUrl,
            scope = manifest.scope,
            manifest = manifest,
            source = PwaDataSource.MANIFEST
        )
    }

    // ═══════════════════════════════════════════
    //  Meta 标签 Fallback
    // ═══════════════════════════════════════════

    /**
     * 从 HTML meta 标签提取 PWA 相关信息
     */
    private fun extractFromMetaTags(html: String, baseUrl: String): PwaAnalysisResult {
        val themeColor = extractMetaContent(html, "theme-color")
        val title = extractTitle(html)
        val ogTitle = extractMetaProperty(html, "og:title")
        val ogImage = extractMetaProperty(html, "og:image")
        val appleTouchIcon = extractAppleTouchIcon(html, baseUrl)
        val favicon = extractFavicon(html, baseUrl)
        val description = extractMetaContent(html, "description")
            ?: extractMetaProperty(html, "og:description")

        val hasAnyData = themeColor != null || ogTitle != null || appleTouchIcon != null

        return PwaAnalysisResult(
            isPwa = hasAnyData,
            suggestedName = ogTitle ?: title,
            suggestedIconUrl = appleTouchIcon ?: ogImage?.let { resolveUrl(baseUrl, it) } ?: favicon,
            suggestedThemeColor = themeColor?.normalizeColor(),
            suggestedDisplay = null,
            suggestedOrientation = null,
            source = if (hasAnyData) PwaDataSource.META_TAGS else PwaDataSource.NONE
        )
    }

    /**
     * 提取 <meta name="xxx" content="xxx">
     */
    private fun extractMetaContent(html: String, name: String): String? {
        val patterns = listOf(
            """<meta\s+[^>]*name\s*=\s*["']$name["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*/?>""",
            """<meta\s+[^>]*content\s*=\s*["']([^"']+)["'][^>]*name\s*=\s*["']$name["'][^>]*/?>"""
        )
        for (patternStr in patterns) {
            val match = patternStr.toRegex(RegexOption.IGNORE_CASE).find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    /**
     * 提取 <meta property="xxx" content="xxx">
     */
    private fun extractMetaProperty(html: String, property: String): String? {
        val patterns = listOf(
            """<meta\s+[^>]*property\s*=\s*["']$property["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*/?>""",
            """<meta\s+[^>]*content\s*=\s*["']([^"']+)["'][^>]*property\s*=\s*["']$property["'][^>]*/?>"""
        )
        for (patternStr in patterns) {
            val match = patternStr.toRegex(RegexOption.IGNORE_CASE).find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    /**
     * 提取 <title>xxx</title>
     */
    private fun extractTitle(html: String): String? {
        val match = """<title[^>]*>([^<]+)</title>""".toRegex(RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.get(1)?.trim()
    }

    /**
     * 提取 Apple Touch Icon
     * <link rel="apple-touch-icon" href="xxx">
     */
    private fun extractAppleTouchIcon(html: String, baseUrl: String): String? {
        val patterns = listOf(
            """<link\s+[^>]*rel\s*=\s*["']apple-touch-icon[^"']*["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*/?>""",
            """<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']apple-touch-icon[^"']*["'][^>]*/?>"""
        )
        for (patternStr in patterns) {
            val match = patternStr.toRegex(RegexOption.IGNORE_CASE).find(html)
            if (match != null) {
                return resolveUrl(baseUrl, match.groupValues[1])
            }
        }
        return null
    }

    /**
     * 提取 favicon
     * <link rel="icon" href="xxx">
     * <link rel="shortcut icon" href="xxx">
     */
    private fun extractFavicon(html: String, baseUrl: String): String? {
        val patterns = listOf(
            """<link\s+[^>]*rel\s*=\s*["'](?:shortcut\s+)?icon["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*/?>""",
            """<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["'](?:shortcut\s+)?icon["'][^>]*/?>"""
        )
        for (patternStr in patterns) {
            val match = patternStr.toRegex(RegexOption.IGNORE_CASE).find(html)
            if (match != null) {
                return resolveUrl(baseUrl, match.groupValues[1])
            }
        }
        // Default favicon
        return resolveUrl(baseUrl, "/favicon.ico")
    }

    // ═══════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════

    /**
     * 推断 Deep Link 域名
     * 从 manifest scope、start_url 和原始 URL 中推断应该绑定的域名
     */
    fun suggestDeepLinkHosts(result: PwaAnalysisResult, originalUrl: String): List<String> {
        val hosts = mutableSetOf<String>()
        
        // 从原始 URL 提取域名
        extractHost(originalUrl)?.let { hosts.add(it) }
        
        // 从 scope 提取域名
        result.scope?.let { extractHost(it) }?.let { hosts.add(it) }
        
        // 从 start_url 提取域名
        result.startUrl?.let { extractHost(it) }?.let { hosts.add(it) }

        // 移除 www. 前缀重复
        val normalized = hosts.toMutableSet()
        hosts.forEach { host ->
            if (host.startsWith("www.")) {
                normalized.add(host.removePrefix("www."))
            } else {
                normalized.add("www.$host")
            }
        }

        return normalized.toList().sorted()
    }

    /**
     * JSONObject 工具：返回非空非 blank 的字符串，否则 null
     */
    private fun JSONObject.optStringOrNull(key: String): String? {
        val value = optString(key, "")
        return value.ifBlank { null }
    }

    /**
     * 标准化颜色值
     * 支持: #RGB → #RRGGBB, rgb(r,g,b) → #RRGGBB
     */
    private fun String.normalizeColor(): String? {
        val trimmed = trim()

        // 已经是 #RRGGBB 格式
        if (trimmed.matches("""^#[0-9A-Fa-f]{6}$""".toRegex())) {
            return trimmed.uppercase()
        }

        // #RGB 格式
        if (trimmed.matches("""^#[0-9A-Fa-f]{3}$""".toRegex())) {
            val r = trimmed[1]
            val g = trimmed[2]
            val b = trimmed[3]
            return "#$r$r$g$g$b$b".uppercase()
        }

        // #RRGGBBAA 格式 — 截取前6位
        if (trimmed.matches("""^#[0-9A-Fa-f]{8}$""".toRegex())) {
            return trimmed.substring(0, 7).uppercase()
        }
        
        // rgb(r, g, b) 格式
        val rgbMatch = """rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
        if (rgbMatch != null) {
            val r = rgbMatch.groupValues[1].toInt().coerceIn(0, 255)
            val g = rgbMatch.groupValues[2].toInt().coerceIn(0, 255)
            val b = rgbMatch.groupValues[3].toInt().coerceIn(0, 255)
            return String.format("#%02X%02X%02X", r, g, b)
        }

        // CSS 命名颜色 — 常见的几种
        val namedColors = mapOf(
            "white" to "#FFFFFF", "black" to "#000000",
            "red" to "#FF0000", "green" to "#008000", "blue" to "#0000FF",
            "yellow" to "#FFFF00", "purple" to "#800080", "orange" to "#FFA500",
            "pink" to "#FFC0CB", "gray" to "#808080", "grey" to "#808080"
        )
        namedColors[trimmed.lowercase()]?.let { return it }

        AppLogger.d(TAG, "Cannot normalize color: $trimmed")
        return null
    }
}
