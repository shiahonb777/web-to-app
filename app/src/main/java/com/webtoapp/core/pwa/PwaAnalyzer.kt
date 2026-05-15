package com.webtoapp.core.pwa

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

object PwaAnalyzer {

    private const val TAG = "PwaAnalyzer"
    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val READ_TIMEOUT_MS = 15_000L
    private const val CALL_TIMEOUT_MS = 20_000L
    private const val MAX_HTML_SIZE = 512 * 1024
    private const val MAX_JSON_SIZE = 256 * 1024
    private const val ANALYZER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val ACCEPT_LANGUAGE = "en-US,en;q=0.9,zh-CN;q=0.8"

    private enum class RequestKind {
        DOCUMENT,
        MANIFEST
    }

    private sealed interface FetchTextResult {
        data class Success(
            val body: String,
            val finalUrl: String
        ) : FetchTextResult

        data class Failure(
            val userMessage: String,
            val statusCode: Int? = null
        ) : FetchTextResult
    }

    private val client by lazy {
        NetworkModule.defaultClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", ANALYZER_USER_AGENT)
                        .header("Accept-Language", ACCEPT_LANGUAGE)
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .build()
                )
            }
            .build()
    }

    suspend fun analyze(url: String): PwaAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)
            AppLogger.i(TAG, "Starting PWA analysis for: $normalizedUrl")

            val htmlResult = when (val response = downloadHtml(normalizedUrl)) {
                is FetchTextResult.Success -> response
                is FetchTextResult.Failure -> {
                    AppLogger.w(TAG, "HTML download failed for $normalizedUrl: ${response.userMessage}")
                    return@withContext PwaAnalysisResult(errorMessage = response.userMessage)
                }
            }

            val html = htmlResult.body
            if (html.isBlank()) {
                AppLogger.w(TAG, "HTML download returned an empty document for ${htmlResult.finalUrl}")
                return@withContext PwaAnalysisResult(
                    errorMessage = "站点返回了空页面，无法完成分析"
                )
            }

            val baseUrl = htmlResult.finalUrl
            val manifestUrl = extractManifestUrl(html, baseUrl)
            AppLogger.d(TAG, "Manifest URL: $manifestUrl")

            if (manifestUrl != null) {
                when (val manifestResponse = downloadJson(manifestUrl)) {
                    is FetchTextResult.Success -> {
                        val manifestJson = manifestResponse.body.trim()
                        if (manifestJson.startsWith("{")) {
                            val manifest = parseManifest(manifestJson, baseUrl)
                            AppLogger.i(
                                TAG,
                                "Manifest parsed: name=${manifest.name}, icons=${manifest.icons.size}"
                            )
                            return@withContext buildResultFromManifest(manifest, baseUrl)
                        }
                        AppLogger.w(TAG, "Manifest response is not JSON: $manifestUrl")
                    }

                    is FetchTextResult.Failure -> {
                        AppLogger.w(
                            TAG,
                            "Manifest download failed for $manifestUrl: ${manifestResponse.userMessage}"
                        )
                    }
                }
            }

            AppLogger.d(TAG, "No manifest found, falling back to meta tags")
            return@withContext extractFromMetaTags(html, baseUrl)
        } catch (e: Exception) {
            AppLogger.e(TAG, "PWA analysis failed", e)
            PwaAnalysisResult(
                errorMessage = "分析失败: ${e.message ?: "未知错误"}"
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadHtml(url: String): FetchTextResult {
        return downloadText(
            url = url,
            accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            maxBytes = MAX_HTML_SIZE,
            kind = RequestKind.DOCUMENT
        )
    }

    private fun downloadJson(url: String): FetchTextResult {
        return downloadText(
            url = url,
            accept = "application/manifest+json,application/json,text/plain,*/*",
            maxBytes = MAX_JSON_SIZE,
            kind = RequestKind.MANIFEST
        )
    }

    private fun downloadText(
        url: String,
        accept: String,
        maxBytes: Int,
        kind: RequestKind
    ): FetchTextResult {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", accept)
                .header("Sec-Fetch-Dest", if (kind == RequestKind.DOCUMENT) "document" else "empty")
                .header("Sec-Fetch-Mode", if (kind == RequestKind.DOCUMENT) "navigate" else "cors")
                .header("Sec-Fetch-Site", "none")
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (!response.isSuccessful) {
                    val userMessage = buildHttpErrorMessage(response.code, kind)
                    AppLogger.w(TAG, "${kind.name} request failed: HTTP ${response.code} from $finalUrl")
                    return FetchTextResult.Failure(userMessage = userMessage, statusCode = response.code)
                }

                val body = response.body
                if (body == null) {
                    AppLogger.w(TAG, "${kind.name} request returned no body: $finalUrl")
                    return FetchTextResult.Failure(userMessage = "站点响应为空，无法完成分析")
                }

                val content = readLimitedText(body, maxBytes)
                FetchTextResult.Success(body = content, finalUrl = finalUrl)
            }
        } catch (e: Exception) {
            val userMessage = buildExceptionMessage(e, kind)
            AppLogger.e(TAG, "${kind.name} request failed for $url: $userMessage", e)
            FetchTextResult.Failure(userMessage = userMessage)
        }
    }

    private fun readLimitedText(body: ResponseBody, maxBytes: Int): String {
        val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        val output = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
        val buffer = ByteArray(8 * 1024)
        var remaining = maxBytes

        body.byteStream().use { input ->
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }

        return output.toByteArray().toString(charset)
    }

    private fun buildHttpErrorMessage(code: Int, kind: RequestKind): String {
        return when (code) {
            401, 403 -> "站点拒绝了分析请求（HTTP $code）"
            404 -> if (kind == RequestKind.MANIFEST) "Manifest 地址不存在（HTTP 404）" else "站点返回 HTTP 404"
            429 -> "站点限制了分析请求频率（HTTP 429）"
            500, 502, 503, 504 -> "站点返回 HTTP $code，当前可能不可用或拦截了自动分析请求"
            else -> "站点返回 HTTP $code，无法完成分析"
        }
    }

    private fun buildExceptionMessage(error: Exception, kind: RequestKind): String {
        return when (error) {
            is UnknownHostException -> "无法解析站点地址"
            is SocketTimeoutException, is InterruptedIOException ->
                if (kind == RequestKind.MANIFEST) {
                    "Manifest 请求超时"
                } else {
                    "站点连接超时，目标站点可能拦截了自动分析请求"
                }
            is SSLException -> "站点 TLS/SSL 握手失败"
            is IOException ->
                if (kind == RequestKind.MANIFEST) {
                    "Manifest 下载失败"
                } else {
                    "无法访问网站，目标站点可能拒绝了自动分析请求"
                }
            else -> "分析请求失败: ${error.message ?: "未知错误"}"
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
                return resolveUrl(baseUrl, match.groupValues[1])
            }
        }

        val commonPaths = listOf(
            "/manifest.json",
            "/manifest.webmanifest",
            "/site.webmanifest"
        )
        for (path in commonPaths) {
            val candidateUrl = resolveUrl(baseUrl, path)
            val json = downloadJson(candidateUrl)
            if (json is FetchTextResult.Success && json.body.trim().startsWith("{")) {
                return candidateUrl
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
                    icons.add(
                        PwaIcon(
                            src = resolveUrl(baseUrl, src),
                            sizes = iconJson.optString("sizes", null),
                            type = iconJson.optString("type", null),
                            purpose = iconJson.optString("purpose", null)
                        )
                    )
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
            .sortedWith(
                compareByDescending<PwaIcon> { it.purpose?.contains("maskable") == true }
                    .thenByDescending { it.maxSizePixels }
            )
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

        val rgbMatch = """rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)"""
            .toRegex(RegexOption.IGNORE_CASE)
            .find(trimmed)
        if (rgbMatch != null) {
            val r = rgbMatch.groupValues[1].toInt().coerceIn(0, 255)
            val g = rgbMatch.groupValues[2].toInt().coerceIn(0, 255)
            val b = rgbMatch.groupValues[3].toInt().coerceIn(0, 255)
            return String.format("#%02X%02X%02X", r, g, b)
        }

        val namedColors = mapOf(
            "white" to "#FFFFFF",
            "black" to "#000000",
            "red" to "#FF0000",
            "green" to "#008000",
            "blue" to "#0000FF",
            "yellow" to "#FFFF00",
            "purple" to "#800080",
            "orange" to "#FFA500",
            "pink" to "#FFC0CB",
            "gray" to "#808080",
            "grey" to "#808080"
        )
        namedColors[trimmed.lowercase()]?.let { return it }

        AppLogger.d(TAG, "Cannot normalize color: $trimmed")
        return null
    }
}
