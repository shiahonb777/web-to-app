package com.webtoapp.core.scraper

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * 网站离线打包引擎
 * 
 * 输入一个 URL，自动抓取整个网站的前端文件（HTML/CSS/JS/图片/字体等），
 * 保存到本地目录，重写资源引用为相对路径，
 * 输出 HtmlFile 列表供现有的 HTML 应用打包流程使用。
 * 
 * 特性：
 * - 并发下载（可控并发数）
 * - 自动发现并下载 HTML 中引用的 CSS/JS/图片/字体
 * - CSS 中 url() 引用的资源递归下载
 * - 路径重写：绝对 URL → 相对路径
 * - 同域限制：默认只抓取同域名资源（CDN 资源可选跨域）
 * - 深度限制：防止无限爬取
 * - 大小限制：单文件和总大小限制
 * - 进度回调
 */
class WebsiteScraper(private val context: Context) {

    companion object {
        private const val TAG = "WebsiteScraper"
        
        // Limits
        private const val DEFAULT_MAX_DEPTH = 3        // 默认最大链接深度
        private const val DEFAULT_MAX_FILES = 500      // 默认最大文件数
        private const val DEFAULT_MAX_FILE_SIZE = 20L * 1024 * 1024  // 单文件最大 20MB
        private const val DEFAULT_MAX_TOTAL_SIZE = 200L * 1024 * 1024 // 总大小最大 200MB
        private const val DEFAULT_CONCURRENCY = 6      // 并发下载数
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 30L
        
        // Regex patterns for resource discovery
        private val HTML_SRC_PATTERN = Pattern.compile(
            """(?:src|href|data-src|data-original|poster)\s*=\s*["']([^"'#]+?)["']""",
            Pattern.CASE_INSENSITIVE
        )
        private val CSS_URL_PATTERN = Pattern.compile(
            """url\(\s*["']?([^"')]+?)["']?\s*\)""",
            Pattern.CASE_INSENSITIVE
        )
        private val CSS_IMPORT_PATTERN = Pattern.compile(
            """@import\s+["']([^"']+?)["']""",
            Pattern.CASE_INSENSITIVE
        )
        private val SRCSET_PATTERN = Pattern.compile(
            """srcset\s*=\s*["']([^"']+?)["']""",
            Pattern.CASE_INSENSITIVE
        )
        
        // File extensions to download
        private val DOWNLOADABLE_EXTENSIONS = setOf(
            // Web
            "html", "htm", "css", "js", "mjs", "json", "xml", "svg", "webmanifest",
            // Images
            "png", "jpg", "jpeg", "gif", "webp", "ico", "bmp", "avif",
            // Fonts
            "woff", "woff2", "ttf", "otf", "eot",
            // Media
            "mp3", "mp4", "webm", "ogg", "wav",
            // Other
            "txt", "map", "wasm"
        )
        
        // Content types that indicate text/parseable content
        private val TEXT_CONTENT_TYPES = setOf(
            "text/html", "text/css", "application/javascript", "text/javascript",
            "application/json", "text/xml", "application/xml", "image/svg+xml",
            "application/manifest+json", "text/plain"
        )
    }
    
    /**
     * 抓取配置
     */
    data class ScrapeConfig(
        val url: String,                              // 目标 URL
        val maxDepth: Int = DEFAULT_MAX_DEPTH,        // 最大链接爬取深度
        val maxFiles: Int = DEFAULT_MAX_FILES,        // 最大文件数
        val maxFileSize: Long = DEFAULT_MAX_FILE_SIZE,    // 单文件大小限制
        val maxTotalSize: Long = DEFAULT_MAX_TOTAL_SIZE,  // 总大小限制
        val concurrency: Int = DEFAULT_CONCURRENCY,   // 并发数
        val followLinks: Boolean = true,              // 是否跟踪同域 <a> 链接
        val downloadCdnResources: Boolean = true,     // 是否下载 CDN（跨域）资源
        val userAgent: String = "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
        val skipPatterns: List<String> = emptyList(),  // 跳过的 URL 模式（正则）
        val timeoutSeconds: Int = 30               // 硬超时（秒）
    )
    
    /**
     * 抓取进度
     */
    data class ScrapeProgress(
        val phase: Phase,
        val downloadedFiles: Int,
        val totalDiscovered: Int,
        val downloadedBytes: Long,
        val currentFile: String,
        val message: String
    ) {
        enum class Phase {
            ANALYZING,    // 分析页面结构
            DOWNLOADING,  // 下载资源
            REWRITING,    // 重写路径
            COMPLETE,     // 完成
            ERROR         // 出错
        }
    }
    
    /**
     * 抓取结果
     */
    sealed class ScrapeResult {
        data class Success(
            val projectDir: File,          // 项目目录
            val files: List<HtmlFile>,     // 文件列表（供 APK 打包用）
            val entryFile: String,         // 入口 HTML 文件名
            val totalFiles: Int,
            val totalSize: Long,
            val elapsedMs: Long
        ) : ScrapeResult()
        
        data class Error(val message: String, val cause: Exception? = null) : ScrapeResult()
    }
    
    // OkHttp client
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectionPool(ConnectionPool(DEFAULT_CONCURRENCY, 30, TimeUnit.SECONDS))
        .build()
    
    // State
    private val downloadedUrls = ConcurrentHashMap<String, String>() // URL -> local relative path
    private val pendingUrls = ConcurrentHashMap<String, Int>()       // URL -> depth
    private val failedUrls = ConcurrentHashMap<String, String>()     // URL -> error reason
    private val fileCounter = AtomicInteger(0)
    private var totalDownloadedBytes = 0L
    private val downloadedBytesLock = Any()
    
    // Project directory
    private lateinit var projectDir: File
    private lateinit var baseUrl: URL
    private lateinit var baseHost: String
    
    /**
     * 开始抓取网站
     * 
     * @param config 抓取配置
     * @param onProgress 进度回调
     * @return 抓取结果
     */
    suspend fun scrape(
        config: ScrapeConfig,
        onProgress: (ScrapeProgress) -> Unit = {}
    ): ScrapeResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Parse and validate URL
            baseUrl = URL(config.url)
            baseHost = baseUrl.host
            
            // Create project directory
            val projectId = generateProjectId(config.url)
            projectDir = File(context.filesDir, "scraped_sites/$projectId")
            if (projectDir.exists()) projectDir.deleteRecursively()
            projectDir.mkdirs()
            
            AppLogger.d(TAG, "Starting scrape: url=${config.url}, projectDir=${projectDir.absolutePath}")
            
            // Reset state
            downloadedUrls.clear()
            pendingUrls.clear()
            failedUrls.clear()
            fileCounter.set(0)
            totalDownloadedBytes = 0L
            
            // Phase 1: Download entry page
            onProgress(ScrapeProgress(
                phase = ScrapeProgress.Phase.ANALYZING,
                downloadedFiles = 0,
                totalDiscovered = 1,
                downloadedBytes = 0,
                currentFile = config.url,
                message = "分析页面结构..."
            ))
            
            val entryResult = downloadFile(config.url, 0, config)
            if (entryResult == null) {
                return@withContext ScrapeResult.Error("无法下载入口页面: ${config.url}")
            }
            
            // Phase 2: BFS download all discovered resources
            val semaphore = kotlinx.coroutines.sync.Semaphore(config.concurrency)
            var iteration = 0
            val maxIterations = config.maxDepth * 20  // safety limit to prevent infinite loops
            val deadline = System.currentTimeMillis() + config.timeoutSeconds * 1000L
            
            while (pendingUrls.isNotEmpty() 
                && fileCounter.get() < config.maxFiles
                && iteration < maxIterations
                && System.currentTimeMillis() < deadline
            ) {
                iteration++
                
                // Trim batch to respect maxFiles limit
                val remainingSlots = config.maxFiles - fileCounter.get()
                val batch = pendingUrls.entries.toList()
                    .sortedBy { it.value }  // process shallower depths first
                    .take(remainingSlots)
                pendingUrls.clear()
                
                if (batch.isEmpty()) break
                
                AppLogger.d(TAG, "Download iteration $iteration: ${batch.size} URLs pending, ${fileCounter.get()} files done")
                
                val jobs = batch.map { (url, depth) ->
                    async {
                        semaphore.acquire()
                        try {
                            if (fileCounter.get() < config.maxFiles) {
                                val totalSize: Long
                                synchronized(downloadedBytesLock) {
                                    totalSize = totalDownloadedBytes
                                }
                                if (totalSize < config.maxTotalSize) {
                                    onProgress(ScrapeProgress(
                                        phase = ScrapeProgress.Phase.DOWNLOADING,
                                        downloadedFiles = fileCounter.get(),
                                        totalDiscovered = fileCounter.get() + pendingUrls.size + batch.size,
                                        downloadedBytes = totalSize,
                                        currentFile = url.substringAfterLast("/").take(40),
                                        message = "下载中: ${fileCounter.get()} 个文件..."
                                    ))
                                    downloadFile(url, depth, config)
                                }
                            }
                        } finally {
                            semaphore.release()
                        }
                    }
                }
                
                jobs.awaitAll()
            }
            
            if (System.currentTimeMillis() >= deadline) {
                AppLogger.w(TAG, "Scrape timed out after ${config.timeoutSeconds}s, ${fileCounter.get()} files downloaded")
            }
            
            // Phase 3: Rewrite URLs in HTML/CSS files
            onProgress(ScrapeProgress(
                phase = ScrapeProgress.Phase.REWRITING,
                downloadedFiles = fileCounter.get(),
                totalDiscovered = fileCounter.get(),
                downloadedBytes = totalDownloadedBytes,
                currentFile = "",
                message = "重写资源路径..."
            ))
            
            rewriteUrls()
            
            // Phase 4: Generate HtmlFile list
            val files = collectHtmlFiles()
            val entryFileName = downloadedUrls[normalizeUrl(config.url)]
                ?: "index.html"
            
            val elapsed = System.currentTimeMillis() - startTime
            
            AppLogger.d(TAG, "Scrape complete: ${files.size} files, ${totalDownloadedBytes / 1024} KB, ${elapsed}ms")
            AppLogger.d(TAG, "Failed URLs: ${failedUrls.size}")
            failedUrls.forEach { (url, reason) ->
                AppLogger.d(TAG, "  FAILED: $url -> $reason")
            }
            
            onProgress(ScrapeProgress(
                phase = ScrapeProgress.Phase.COMPLETE,
                downloadedFiles = files.size,
                totalDiscovered = files.size,
                downloadedBytes = totalDownloadedBytes,
                currentFile = "",
                message = "完成: ${files.size} 个文件, ${totalDownloadedBytes / 1024} KB"
            ))
            
            ScrapeResult.Success(
                projectDir = projectDir,
                files = files,
                entryFile = entryFileName,
                totalFiles = files.size,
                totalSize = totalDownloadedBytes,
                elapsedMs = elapsed
            )
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Scrape failed", e)
            onProgress(ScrapeProgress(
                phase = ScrapeProgress.Phase.ERROR,
                downloadedFiles = fileCounter.get(),
                totalDiscovered = fileCounter.get(),
                downloadedBytes = totalDownloadedBytes,
                currentFile = "",
                message = "抓取失败: ${e.message}"
            ))
            ScrapeResult.Error("抓取失败: ${e.message}", e)
        }
    }
    
    /**
     * 下载单个文件并发现其中的资源引用
     */
    private fun downloadFile(
        urlString: String, 
        depth: Int, 
        config: ScrapeConfig
    ): String? {
        val normalizedUrl = normalizeUrl(urlString)
        
        // Already downloaded?
        if (downloadedUrls.containsKey(normalizedUrl)) {
            return downloadedUrls[normalizedUrl]
        }
        
        // Skip patterns
        if (config.skipPatterns.any { normalizedUrl.matches(Regex(it)) }) {
            return null
        }
        
        // Mark as in-progress (prevent duplicate downloads)
        if (downloadedUrls.putIfAbsent(normalizedUrl, "__pending__") != null) {
            return downloadedUrls[normalizedUrl]?.takeIf { it != "__pending__" }
        }
        
        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", config.userAgent)
                .header("Accept", "*/*")
                .header("Referer", config.url)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                failedUrls[normalizedUrl] = "HTTP ${response.code}"
                downloadedUrls.remove(normalizedUrl)
                response.close()
                return null
            }
            
            val body = response.body ?: run {
                failedUrls[normalizedUrl] = "Empty response body"
                downloadedUrls.remove(normalizedUrl)
                response.close()
                return null
            }
            
            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val contentLength = body.contentLength()
            
            // Size check
            if (contentLength > config.maxFileSize) {
                failedUrls[normalizedUrl] = "Too large: ${contentLength / 1024} KB"
                downloadedUrls.remove(normalizedUrl)
                response.close()
                return null
            }
            
            // Read content
            val data = body.bytes()
            response.close()
            
            // Total size check
            synchronized(downloadedBytesLock) {
                if (totalDownloadedBytes + data.size > config.maxTotalSize) {
                    failedUrls[normalizedUrl] = "Total size limit exceeded"
                    downloadedUrls.remove(normalizedUrl)
                    return null
                }
                totalDownloadedBytes += data.size
            }
            
            // Determine local filename
            val localPath = urlToLocalPath(normalizedUrl)
            val localFile = File(projectDir, localPath)
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(data)
            
            fileCounter.incrementAndGet()
            downloadedUrls[normalizedUrl] = localPath
            
            AppLogger.d(TAG, "Downloaded: $localPath (${data.size / 1024} KB) depth=$depth")
            
            // Discover resources in this file
            if (depth < config.maxDepth) {
                val isTextContent = TEXT_CONTENT_TYPES.any { contentType.contains(it) } ||
                    localPath.endsWith(".html") || localPath.endsWith(".htm") ||
                    localPath.endsWith(".css") || localPath.endsWith(".js") ||
                    localPath.endsWith(".svg")
                
                if (isTextContent) {
                    val text = String(data, Charsets.UTF_8)
                    discoverResources(text, normalizedUrl, depth, config, contentType)
                }
            }
            
            return localPath
            
        } catch (e: Exception) {
            failedUrls[normalizedUrl] = e.message ?: "Unknown error"
            downloadedUrls.remove(normalizedUrl)
            AppLogger.d(TAG, "Download failed: $normalizedUrl -> ${e.message}")
            return null
        }
    }
    
    /**
     * 从文本内容中发现资源引用
     */
    private fun discoverResources(
        content: String, 
        sourceUrl: String, 
        depth: Int, 
        config: ScrapeConfig,
        contentType: String
    ) {
        val isHtml = contentType.contains("html") || 
            sourceUrl.endsWith(".html") || sourceUrl.endsWith(".htm") ||
            content.trimStart().startsWith("<!") || content.trimStart().startsWith("<html")
        val isCss = contentType.contains("css") || sourceUrl.endsWith(".css")
        
        val discoveredUrls = mutableSetOf<String>()
        
        if (isHtml) {
            // Find src, href, data-src attributes
            val matcher = HTML_SRC_PATTERN.matcher(content)
            while (matcher.find()) {
                matcher.group(1)?.let { discoveredUrls.add(it.trim()) }
            }
            
            // Find srcset
            val srcsetMatcher = SRCSET_PATTERN.matcher(content)
            while (srcsetMatcher.find()) {
                srcsetMatcher.group(1)?.let { srcset ->
                    srcset.split(",").forEach { entry ->
                        entry.trim().split("\\s+".toRegex()).firstOrNull()?.let { url ->
                            discoveredUrls.add(url.trim())
                        }
                    }
                }
            }
        }
        
        if (isCss || isHtml) {
            // Find url() in CSS
            val urlMatcher = CSS_URL_PATTERN.matcher(content)
            while (urlMatcher.find()) {
                urlMatcher.group(1)?.let { url ->
                    if (!url.startsWith("data:")) {
                        discoveredUrls.add(url.trim())
                    }
                }
            }
            
            // Find @import
            val importMatcher = CSS_IMPORT_PATTERN.matcher(content)
            while (importMatcher.find()) {
                importMatcher.group(1)?.let { discoveredUrls.add(it.trim()) }
            }
        }
        
        // Resolve and queue discovered URLs
        for (rawUrl in discoveredUrls) {
            if (rawUrl.isBlank() || rawUrl.startsWith("data:") || rawUrl.startsWith("javascript:") ||
                rawUrl.startsWith("mailto:") || rawUrl.startsWith("tel:") || rawUrl.startsWith("#")) {
                continue
            }
            
            val resolvedUrl = resolveUrl(rawUrl, sourceUrl) ?: continue
            val normalizedResolved = normalizeUrl(resolvedUrl)
            
            // Already downloaded or pending?
            if (downloadedUrls.containsKey(normalizedResolved) || pendingUrls.containsKey(normalizedResolved)) {
                continue
            }
            
            // Same domain check (allow CDN resources if config allows)
            val resolvedHost = try { URL(normalizedResolved).host } catch (e: Exception) { continue }
            val isSameDomain = resolvedHost == baseHost || resolvedHost.endsWith(".$baseHost")
            
            if (!isSameDomain && !config.downloadCdnResources) {
                continue
            }
            
            // Only follow <a href> links if it's HTML and same domain
            val isPageLink = isHtml && rawUrl !in extractResourceUrls(content)
            if (isPageLink && !config.followLinks) {
                continue
            }
            if (isPageLink && !isSameDomain) {
                continue
            }
            
            // Check file extension is downloadable
            val ext = normalizedResolved.substringAfterLast(".").substringBefore("?").lowercase()
            val isResource = ext in DOWNLOADABLE_EXTENSIONS || !isPageLink
            
            if (isResource || (isPageLink && depth + 1 <= config.maxDepth)) {
                pendingUrls[normalizedResolved] = depth + 1
            }
        }
    }
    
    /**
     * Extract URLs that are clearly resource references (src, not href to pages)
     */
    private fun extractResourceUrls(htmlContent: String): Set<String> {
        val resources = mutableSetOf<String>()
        val srcPattern = Pattern.compile(
            """(?:src|data-src|data-original|poster)\s*=\s*["']([^"']+?)["']""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = srcPattern.matcher(htmlContent)
        while (matcher.find()) {
            matcher.group(1)?.let { resources.add(it.trim()) }
        }
        // link[rel=stylesheet/icon/preload]
        val linkPattern = Pattern.compile(
            """<link[^>]+href\s*=\s*["']([^"']+?)["'][^>]*rel\s*=\s*["'](stylesheet|icon|shortcut icon|apple-touch-icon|preload|prefetch)["']""",
            Pattern.CASE_INSENSITIVE
        )
        val linkMatcher = linkPattern.matcher(htmlContent)
        while (linkMatcher.find()) {
            linkMatcher.group(1)?.let { resources.add(it.trim()) }
        }
        return resources
    }
    
    /**
     * 重写所有已下载文件中的 URL 引用为相对路径
     *
     * 策略：
     * - 成功下载的资源 → 替换为本地相对路径（离线可用）
     * - 下载失败的资源 → 保留原始在线 URL（在线时仍可加载，避免白屏）
     */
    private fun rewriteUrls() {
        projectDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val ext = file.extension.lowercase()
            if (ext in setOf("html", "htm", "css", "js", "svg")) {
                try {
                    var content = file.readText(Charsets.UTF_8)
                    val originalContent = content
                    var modified = false
                    
                    // Only rewrite URLs for resources that were successfully downloaded
                    // Sort by localPath length descending so deeper paths get replaced first
                    val sortedEntries = downloadedUrls.entries
                        .filter { it.value != "__pending__" }
                        .filter { entry ->
                            // Verify the local file actually exists on disk
                            File(projectDir, entry.value).isFile
                        }
                        .sortedByDescending { it.value.length }
                    
                    for ((url, localPath) in sortedEntries) {
                        // Calculate relative path from this file to the target
                        val fileRelDir = file.relativeTo(projectDir).parent ?: ""
                        val relativePath = calculateRelativePath(fileRelDir, localPath)
                        
                        // Replace various URL forms — only match in ORIGINAL content
                        // to prevent short variants from matching already-replaced text
                        val urlVariants = generateUrlVariants(url)
                            .sortedByDescending { it.length }
                        
                        for (variant in urlVariants) {
                            if (originalContent.contains(variant)) {
                                content = content.replace(variant, relativePath)
                                modified = true
                            }
                        }
                    }
                    
                    if (modified) {
                        file.writeText(content, Charsets.UTF_8)
                    }
                } catch (e: Exception) {
                    AppLogger.d(TAG, "Rewrite failed for ${file.name}: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Generate variants of a URL for replacement matching
     */
    private fun generateUrlVariants(url: String): List<String> {
        val variants = mutableListOf(url)
        
        // Protocol-relative variant: https://host/path → //host/path
        // This is the only valid short form that appears in HTML (src="//host/path")
        // Do NOT generate bare "host/path" — it's never a valid HTML URL reference
        // and will falsely match substrings in already-replaced content.
        if (url.startsWith("https://")) {
            variants.add(url.removePrefix("https:"))  // → //host/path
        } else if (url.startsWith("http://")) {
            variants.add(url.removePrefix("http:"))    // → //host/path
        }
        
        // Without query parameters
        if (url.contains("?")) {
            variants.add(url.substringBefore("?"))
        }
        
        return variants
    }
    
    /**
     * Calculate relative path from one file's directory to another file
     */
    private fun calculateRelativePath(fromDir: String, toPath: String): String {
        if (fromDir.isEmpty()) return toPath
        
        val fromParts = fromDir.split("/").filter { it.isNotEmpty() }
        val toParts = toPath.split("/").filter { it.isNotEmpty() }
        
        var commonPrefix = 0
        while (commonPrefix < fromParts.size && commonPrefix < toParts.size &&
               fromParts[commonPrefix] == toParts[commonPrefix]) {
            commonPrefix++
        }
        
        val upCount = fromParts.size - commonPrefix
        val relativeParts = List(upCount) { ".." } + toParts.subList(commonPrefix, toParts.size)
        
        return relativeParts.joinToString("/")
    }
    
    /**
     * Collect all files as HtmlFile list for APK builder
     */
    private fun collectHtmlFiles(): List<HtmlFile> {
        return projectDir.walkTopDown()
            .filter { it.isFile }
            .map { file ->
                val relativePath = file.relativeTo(projectDir).path
                HtmlFile(
                    name = relativePath,
                    path = file.absolutePath,
                    type = when (file.extension.lowercase()) {
                        "html", "htm" -> HtmlFileType.HTML
                        "css" -> HtmlFileType.CSS
                        "js", "mjs" -> HtmlFileType.JS
                        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "avif", "bmp" -> HtmlFileType.IMAGE
                        "woff", "woff2", "ttf", "otf", "eot" -> HtmlFileType.FONT
                        else -> HtmlFileType.OTHER
                    }
                )
            }
            .toList()
    }
    
    // ==================== URL Utilities ====================
    
    /**
     * Resolve a relative or absolute URL against a base URL
     */
    private fun resolveUrl(rawUrl: String, baseUrlString: String): String? {
        return try {
            val base = URI(baseUrlString)
            val resolved = base.resolve(rawUrl.replace(" ", "%20"))
            val result = resolved.toString()
            // Only http/https
            if (result.startsWith("http://") || result.startsWith("https://")) result else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Normalize URL: remove fragment, sort query params
     */
    private fun normalizeUrl(url: String): String {
        return try {
            val u = URL(url)
            val path = if (u.path.isNullOrEmpty()) "/" else u.path
            val query = u.query?.let { "?$it" } ?: ""
            "${u.protocol}://${u.host}${if (u.port != -1 && u.port != u.defaultPort) ":${u.port}" else ""}$path$query"
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Convert URL to local filesystem path, preserving directory structure
     */
    private fun urlToLocalPath(url: String): String {
        return try {
            val u = URL(url)
            val host = u.host.replace(":", "_")
            var path = u.path?.trimStart('/') ?: ""
            
            // Handle query parameters by hashing
            if (!u.query.isNullOrEmpty()) {
                val ext = path.substringAfterLast(".", "")
                val baseName = path.substringBeforeLast(".")
                val queryHash = md5(u.query).take(8)
                path = if (ext.isNotEmpty()) {
                    "${baseName}_${queryHash}.$ext"
                } else {
                    "${path}_${queryHash}"
                }
            }
            
            // If path is empty or ends with /, add index.html
            if (path.isEmpty() || path.endsWith("/")) {
                path += "index.html"
            }
            
            // If no extension, assume HTML
            if (!path.contains(".") || path.substringAfterLast(".").length > 10) {
                path += ".html"
            }
            
            // Sanitize path
            path = path.replace("[^a-zA-Z0-9/_.-]".toRegex(), "_")
            
            // For same-domain: use path directly
            // For cross-domain (CDN): prefix with host
            if (u.host == baseHost || u.host.endsWith(".$baseHost")) {
                path
            } else {
                "_cdn/$host/$path"
            }
        } catch (e: Exception) {
            "file_${fileCounter.get()}.dat"
        }
    }
    
    /**
     * Generate project ID from URL
     */
    private fun generateProjectId(url: String): String {
        val host = try { URL(url).host } catch (e: Exception) { "unknown" }
        val hash = md5(url).take(8)
        return "${host.replace(".", "_")}_$hash"
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 获取已抓取站点列表
     */
    fun getScrapedSites(): List<ScrapedSiteInfo> {
        val sitesDir = File(context.filesDir, "scraped_sites")
        if (!sitesDir.exists()) return emptyList()
        
        return sitesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val files = dir.walkTopDown().filter { it.isFile }.toList()
                val totalSize = files.sumOf { it.length() }
                ScrapedSiteInfo(
                    projectId = dir.name,
                    dirPath = dir.absolutePath,
                    fileCount = files.size,
                    totalSize = totalSize,
                    lastModified = dir.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }
    
    /**
     * 删除已抓取站点
     */
    fun deleteScrapedSite(projectId: String) {
        val siteDir = File(context.filesDir, "scraped_sites/$projectId")
        if (siteDir.exists()) {
            siteDir.deleteRecursively()
        }
    }
    
    /**
     * 清理所有已抓取站点
     */
    fun clearAllScrapedSites() {
        val sitesDir = File(context.filesDir, "scraped_sites")
        if (sitesDir.exists()) {
            sitesDir.deleteRecursively()
        }
    }
    
    /**
     * 已抓取站点信息
     */
    data class ScrapedSiteInfo(
        val projectId: String,
        val dirPath: String,
        val fileCount: Int,
        val totalSize: Long,
        val lastModified: Long
    )
}
