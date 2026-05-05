package com.webtoapp.core.pwa

import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL














object PwaAnalyzer {

    private const val TAG = "PwaAnalyzer"
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT = 15_000
    private const val MAX_HTML_SIZE = 512 * 1024







    suspend fun analyze(url: String): PwaAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            AppLogger.i(TAG, "Starting PWA analysis for: $normalizedUrl")


            val html = downloadHtml(normalizedUrl)
            if (html.isNullOrBlank()) {
                AppLogger.w(TAG, "Failed to download HTML from $normalizedUrl")
                return@withContext PwaAnalysisResult(
                    errorMessage = "无法访问网站"
                )
            }


            val manifestUrl = extractManifestUrl(html, normalizedUrl)
            AppLogger.d(TAG, "Manifest URL: $manifestUrl")


            if (manifestUrl != null) {
                val manifestJson = downloadJson(manifestUrl)
                if (manifestJson != null) {
                    val manifest = parseManifest(manifestJson, normalizedUrl)
                    AppLogger.i(TAG, "Manifest parsed: name=${manifest.name}, icons=${manifest.icons.size}")
                    return@withContext buildResultFromManifest(manifest, normalizedUrl)
                }
            }


            AppLogger.d(TAG, "No manifest found, falling back to meta tags")
            return@withContext extractFromMetaTags(html, normalizedUrl)

        } catch (e: Exception) {
            AppLogger.e(TAG, "PWA analysis failed", e)
            PwaAnalysisResult(
                errorMessage = "分析失败: ${e.message}"
            )
        }
    }





    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }




    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) {
            return relative
        }
        return try {
            URL(URL(base), relative).toString()
        } catch (e: Exception) {

            val baseUrl = URL(base)
            if (relative.startsWith("/")) {
                "${baseUrl.protocol}://${baseUrl.host}${if (baseUrl.port > 0 && baseUrl.port != baseUrl.defaultPort) ":${baseUrl.port}" else ""}$relative"
            } else {
                val basePath = base.substringBeforeLast("/")
                "$basePath/$relative"
            }
        }
    }




    fun extractHost(url: String): String? {
        return try {
            URL(normalizeUrl(url)).host
        } catch (e: Exception) {
            null
        }
    }





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









    private fun extractManifestUrl(html: String, baseUrl: String): String? {


        val patterns = listOf(

            """<link\s+[^>]*rel\s*=\s*["']manifest["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*/?>""".toRegex(RegexOption.IGNORE_CASE),

            """<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']manifest["'][^>]*/?>""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                val href = match.groupValues[1]
                return resolveUrl(baseUrl, href)
            }
        }


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




    private fun buildResultFromManifest(manifest: PwaManifest, baseUrl: String): PwaAnalysisResult {

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




    private fun extractTitle(html: String): String? {
        val match = """<title[^>]*>([^<]+)</title>""".toRegex(RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.get(1)?.trim()
    }





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

        return resolveUrl(baseUrl, "/favicon.ico")
    }









    fun suggestDeepLinkHosts(result: PwaAnalysisResult, originalUrl: String): List<String> {
        val hosts = mutableSetOf<String>()


        extractHost(originalUrl)?.let { hosts.add(it) }


        result.scope?.let { extractHost(it) }?.let { hosts.add(it) }


        result.startUrl?.let { extractHost(it) }?.let { hosts.add(it) }


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




    private fun JSONObject.optStringOrNull(key: String): String? {
        val value = optString(key, "")
        return value.ifBlank { null }
    }





    private fun String.normalizeColor(): String? {
        val trimmed = trim()


        if (trimmed.matches("""^#[0-9A-Fa-f]{6}$""".toRegex())) {
            return trimmed.uppercase()
        }


        if (trimmed.matches("""^#[0-9A-Fa-f]{3}$""".toRegex())) {
            val r = trimmed[1]
            val g = trimmed[2]
            val b = trimmed[3]
            return "#$r$r$g$g$b$b".uppercase()
        }


        if (trimmed.matches("""^#[0-9A-Fa-f]{8}$""".toRegex())) {
            return trimmed.substring(0, 7).uppercase()
        }


        val rgbMatch = """rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""".toRegex(RegexOption.IGNORE_CASE).find(trimmed)
        if (rgbMatch != null) {
            val r = rgbMatch.groupValues[1].toInt().coerceIn(0, 255)
            val g = rgbMatch.groupValues[2].toInt().coerceIn(0, 255)
            val b = rgbMatch.groupValues[3].toInt().coerceIn(0, 255)
            return String.format("#%02X%02X%02X", r, g, b)
        }


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
