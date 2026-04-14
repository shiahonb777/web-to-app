package com.webtoapp.util

import com.webtoapp.core.logging.AppLogger
import android.util.LruCache
import com.webtoapp.core.i18n.AppStringsProvider
import java.io.File
import java.nio.charset.Charset

/**
 * HTML project processor.
 *
 * Features:
 * 1. Detect and fix resource path references inside HTML.
 * 2. Inline external CSS/JS into HTML.
 * 3. Detect file encoding and read files correctly.
 * 4. Analyze project structure and surface issues.
 */
object HtmlProjectProcessor {
    
    private const val TAG = "HtmlProjectProcessor"
    
    // Max text size (5MB) for analysis; skip larger files to avoid OOM
    private const val MAX_ANALYZE_FILE_SIZE = 5L * 1024 * 1024

    // Cache for encoding detection
    private val encodingCache = LruCache<String, String>(50)

    // Precompiled regex patterns (object handles lazy init)
    private val cssLinkRegex = Regex("""<link[^>]*href=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val jsScriptRegex = Regex("""<script[^>]*src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val imgSrcRegex = Regex("""<img[^>]*src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val absolutePathRegex = Regex("""(href|src)=["'](/[^"']+)["']""")
    private val protocolRelativeRegex = Regex("""(href|src)=["'](//[^"']+)["']""")
    // Match local CSS references (non-http(s) .css files)
    private val localCssRegex = Regex("""<link[^>]*href=["'](?!https?://)[^"']*\.css["'][^>]*>""", RegexOption.IGNORE_CASE)
    // Match local JS references (non-http(s) .js files, allowing inline content)
    // (?s) enables dot to match newlines for multi-line script tags
    // (?!https?://) excludes CDN references starting with http(s)
    private val localJsRegex = Regex("""(?s)<script[^>]*\bsrc=["'](?!https?://)([^"']*\.js)["'][^>]*>.*?</script>""", RegexOption.IGNORE_CASE)
    private val charsetRegex = Regex("""charset=["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE)

    // inlineCss
    private val closeHeadRegex = Regex("</head>", RegexOption.IGNORE_CASE)
    private val openBodyRegex = Regex("<body", RegexOption.IGNORE_CASE)
    private val openHtmlTagRegex = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)
    
    // inlineJs
    private val closeBodyRegex = Regex("</body>", RegexOption.IGNORE_CASE)
    private val closeHtmlRegex = Regex("</html>", RegexOption.IGNORE_CASE)
    
    // addViewportMeta
    private val openHeadRegex = Regex("<head>", RegexOption.IGNORE_CASE)
    
    /**
     * Project analysis results.
     */
    data class ProjectAnalysis(
        val htmlFiles: List<FileInfo>,
        val cssFiles: List<FileInfo>,
        val jsFiles: List<FileInfo>,
        val otherFiles: List<FileInfo>,
        val issues: List<ProjectIssue>,
        val suggestions: List<String>
    )
    
    /**
     * File information.
     */
    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val encoding: String?,
        val references: List<ResourceReference> = emptyList()
    )
    
    /**
     * Resource reference.
     */
    data class ResourceReference(
        val type: ReferenceType,
        val originalPath: String,
        val resolvedPath: String?,
        val isValid: Boolean,
        val issue: String? = null
    )
    
    enum class ReferenceType {
        CSS_LINK,       // <link href="...">
        JS_SCRIPT,      // <script src="...">
        IMAGE,          // <img src="...">
        CSS_IMPORT,     // @import url(...)
        CSS_URL,        // url(...) in CSS
        OTHER
    }
    
    /**
     * Project issue.
     */
    data class ProjectIssue(
        val severity: IssueSeverity,
        val type: IssueType,
        val message: String,
        val file: String? = null,
        val suggestion: String? = null
    )
    
    enum class IssueSeverity {
        ERROR,      // Causes functionality to break
        WARNING,    // Potential issue
        INFO        // Informational
    }

    enum class IssueType {
        MISSING_FILE,           // Referenced file missing
        ABSOLUTE_PATH,          // Uses absolute paths
        ENCODING_ISSUE,         // Encoding problem
        STRUCTURE_ISSUE,        // Directory layout issue
        SYNTAX_ERROR,           // Syntax error
        EXTERNAL_RESOURCE       // References external resource
    }
    
    /**
     * Analyze an HTML project.
     */
    fun analyzeProject(
        htmlFilePath: String?,
        cssFilePath: String?,
        jsFilePath: String?,
        additionalFiles: List<String> = emptyList()
    ): ProjectAnalysis {
        val issues = mutableListOf<ProjectIssue>()
        val suggestions = mutableListOf<String>()
        
        val htmlFiles = mutableListOf<FileInfo>()
        val cssFiles = mutableListOf<FileInfo>()
        val jsFiles = mutableListOf<FileInfo>()
        val otherFiles = mutableListOf<FileInfo>()
        
        // Analyze HTML files
        htmlFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val encoding = detectEncoding(file)
                // Skip content analysis for large files but record metadata
                val references = if (file.length() <= MAX_ANALYZE_FILE_SIZE) {
                    val content = readFileWithEncoding(file, encoding)
                    analyzeHtmlReferences(content, file.parentFile)
                } else {
                    issues.add(ProjectIssue(
                        severity = IssueSeverity.INFO,
                        type = IssueType.STRUCTURE_ISSUE,
                        message = AppStringsProvider.current().htmlFileTooLarge.replace("%s", (file.length() / 1024 / 1024).toString()),
                        file = file.name
                    ))
                    emptyList()
                }
                
                htmlFiles.add(FileInfo(
                    name = file.name,
                    path = path,
                    size = file.length(),
                    encoding = encoding,
                    references = references
                ))
                
        // Check reference issues
                references.forEach { ref ->
                    if (!ref.isValid) {
                        issues.add(ProjectIssue(
                            severity = IssueSeverity.WARNING,
                            type = if (ref.originalPath.startsWith("/")) IssueType.ABSOLUTE_PATH else IssueType.MISSING_FILE,
                            message = ref.issue ?: "${AppStringsProvider.current().resourceReferenceIssue}: ${ref.originalPath}",
                            file = file.name,
                            suggestion = when {
                                ref.originalPath.startsWith("/") -> AppStringsProvider.current().suggestUseRelativePath
                                else -> AppStringsProvider.current().suggestEnsureFileImported.replace("%s", ref.originalPath)
                            }
                        ))
                    }
                }
            } else {
                issues.add(ProjectIssue(
                    severity = IssueSeverity.ERROR,
                    type = IssueType.MISSING_FILE,
                    message = "${AppStringsProvider.current().htmlFileNotFound}: $path"
                ))
            }
        }
        
        // Analyze CSS files
        cssFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val encoding = detectEncoding(file)
                cssFiles.add(FileInfo(
                    name = file.name,
                    path = path,
                    size = file.length(),
                    encoding = encoding
                ))
                
        // Check encoding
                if (encoding != "UTF-8" && encoding != null) {
                    issues.add(ProjectIssue(
                        severity = IssueSeverity.WARNING,
                        type = IssueType.ENCODING_ISSUE,
                        message = AppStringsProvider.current().cssEncodingWarning.replace("%s", encoding ?: "unknown"),
                        file = file.name,
                        suggestion = AppStringsProvider.current().suggestSaveAsUtf8
                    ))
                }
            }
        }
        
        // Analyze JS files
        jsFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val encoding = detectEncoding(file)
                
                jsFiles.add(FileInfo(
                    name = file.name,
                    path = path,
                    size = file.length(),
                    encoding = encoding
                ))
                
        // Skip content analysis for large files
                if (file.length() <= MAX_ANALYZE_FILE_SIZE) {
                    val content = readFileWithEncoding(file, encoding)
        // Check common JS issues in JS files
                    checkJsIssues(content, file.name, issues)
                }
            }
        }
        
        // Generate suggestions
        if (htmlFiles.isNotEmpty() && cssFiles.isEmpty() && jsFiles.isEmpty()) {
            val htmlFileObj = htmlFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() <= MAX_ANALYZE_FILE_SIZE }
            val htmlContent = htmlFileObj?.let { readFileWithEncoding(it, null) } ?: ""
            if (htmlContent.contains("<link", ignoreCase = true) || htmlContent.contains("<script", ignoreCase = true)) {
                suggestions.add(AppStringsProvider.current().suggestExternalFilesDetected)
            }
        }
        
        if (issues.any { it.type == IssueType.ABSOLUTE_PATH }) {
            suggestions.add(AppStringsProvider.current().suggestUseRelativePathsForAll)
        }
        
        return ProjectAnalysis(
            htmlFiles = htmlFiles,
            cssFiles = cssFiles,
            jsFiles = jsFiles,
            otherFiles = otherFiles,
            issues = issues,
            suggestions = suggestions
        )
    }
    
    /**
     * Process HTML content by inlining CSS and JS.
     *
     * @param htmlContent HTML content
     * @param cssContent CSS content (nullable/empty)
     * @param jsContent JS content (nullable/empty)
     * @param fixPaths Whether to fix path references
     * @param removeLocalRefs Whether to remove local CSS/JS references (default true; skipped if cssContent/jsContent is empty)
     */
    fun processHtmlContent(
        htmlContent: String,
        cssContent: String?,
        jsContent: String?,
        fixPaths: Boolean = true,
        removeLocalRefs: Boolean = true
    ): String {
        var result = htmlContent

        // 1. Fix path references
        if (fixPaths) {
            result = fixResourcePaths(result)
        }

        // 2. Remove local CSS/JS references (only when inline content exists to avoid blank pages)
        if (removeLocalRefs) {
            result = removeLocalResourceReferences(result,
                hasCssContent = !cssContent.isNullOrBlank(),
                hasJsContent = !jsContent.isNullOrBlank())
        }

        // 3. Inline CSS
        if (!cssContent.isNullOrBlank()) {
            result = inlineCss(result, cssContent)
        }

        // 4. Inline JS
        if (!jsContent.isNullOrBlank()) {
            result = inlineJs(result, jsContent)
        }

        // 5. Add viewport meta if missing
        if (!result.contains("viewport", ignoreCase = true)) {
            result = addViewportMeta(result)
        }

        return result
    }
    
    /**
     * Fix resource path references.
     *
     * Note: only adjust absolute paths (starting with /) and protocol-relative paths (starting with //).
     * Relative paths (./ or ../) are already correct and should remain unchanged.
     */
    private fun fixResourcePaths(html: String): String {
        var result = html

        // Convert absolute paths to relative ones
        // /css/style.css -> ./css/style.css
        result = absolutePathRegex.replace(result) { match ->
            val attr = match.groupValues[1]
            val path = match.groupValues[2]
            """$attr=".${path}""""
        }

        // Fix protocol-relative paths (//example.com/js/app.js -> https://example.com/js/app.js)
        result = protocolRelativeRegex.replace(result) { match ->
            val attr = match.groupValues[1]
            val path = match.groupValues[2]
            """$attr="https:$path""""
        }

        // Do not modify ../ paths; they are already correct relative references
        // Forcing ./ would break subdirectory references (e.g., pages/about.html referencing ../js/app.js)

        return result
    }
    
    /**
     * Remove local resource references when inlining content.
     * @param html HTML content
     * @param hasCssContent Whether CSS content is available for inlining
     * @param hasJsContent Whether JS content is available for inlining
     * @return Processed HTML
     */
    private fun removeLocalResourceReferences(
        html: String,
        hasCssContent: Boolean = true,
        hasJsContent: Boolean = true
    ): String {
        var result = html

        // Remove CSS <link> tags only when CSS content exists; otherwise leave references for manual handling
        if (hasCssContent) {
            result = localCssRegex.replace(result, "<!-- CSS inlined -->")
        }

        // Remove JS <script> tags only when JS content exists; otherwise leave references for manual handling
        if (hasJsContent) {
            result = localJsRegex.replace(result, "<!-- JS inlined -->")
        }

        return result
    }
    
    /**
     * Inline CSS.
     */
    private fun inlineCss(html: String, css: String): String {
        val styleTag = "<style>\n/* Inlined CSS */\n$css\n</style>"
        val escapedStyleTag = Regex.escapeReplacement(styleTag)
        
        return when {
            html.contains("</head>", ignoreCase = true) -> {
                html.replaceFirst(closeHeadRegex, "$escapedStyleTag\n</head>")
            }
            html.contains("<body", ignoreCase = true) -> {
                html.replaceFirst(openBodyRegex, "$escapedStyleTag\n<body")
            }
            html.contains("<html", ignoreCase = true) -> {
                val match = openHtmlTagRegex.find(html)
                if (match != null) {
                    html.substring(0, match.range.last + 1) + 
                        "\n<head>\n$styleTag\n</head>" + 
                        html.substring(match.range.last + 1)
                } else {
                    "$styleTag\n$html"
                }
            }
            else -> "$styleTag\n$html"
        }
    }
    
    /**
     * Inline JS.
     */
    private fun inlineJs(html: String, js: String): String {
        val wrappedJs = wrapJsForSafeExecution(js)
        val scriptTag = "<script>\n/* Inlined JS */\n$wrappedJs\n</script>"
        val escapedScriptTag = Regex.escapeReplacement(scriptTag)
        
        return when {
            html.contains("</body>", ignoreCase = true) -> {
                html.replaceFirst(closeBodyRegex, "$escapedScriptTag\n</body>")
            }
            html.contains("</html>", ignoreCase = true) -> {
                html.replaceFirst(closeHtmlRegex, "$escapedScriptTag\n</html>")
            }
            else -> "$html\n$scriptTag"
        }
    }
    
    /**
     * Wrap JS to ensure safe execution.
     */
    private fun wrapJsForSafeExecution(js: String): String {
        val trimmed = js.trim()
        if (trimmed.isEmpty()) return ""
        
        // Check if a DOM-ready wrapper already exists
        val hasWrapper = trimmed.contains("DOMContentLoaded", ignoreCase = true) ||
                        trimmed.contains("window.onload", ignoreCase = true) ||
                        trimmed.contains("addEventListener('load'", ignoreCase = true) ||
                        trimmed.contains("addEventListener(\"load\"", ignoreCase = true) ||
                        trimmed.contains("\$(document).ready", ignoreCase = true) ||
                        trimmed.contains("\$(function()", ignoreCase = true)
        
        return if (hasWrapper) {
            trimmed
        } else {
            """
(function() {
    'use strict';
    
    function initApp() {
        try {
$trimmed
        } catch (e) {
            console.error('[WebToApp] JS execution error:', e);
        }
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initApp);
    } else {
        initApp();
    }
})();
            """.trimIndent()
        }
    }
    
    /**
     * Add viewport meta tag.
     */
    private fun addViewportMeta(html: String): String {
        val viewportMeta = """<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">"""
        
        return when {
            html.contains("<head>", ignoreCase = true) -> {
                html.replaceFirst(openHeadRegex, "<head>\n$viewportMeta")
            }
            html.contains("<html", ignoreCase = true) -> {
                val match = openHtmlTagRegex.find(html)
                if (match != null) {
                    html.substring(0, match.range.last + 1) + 
                        "\n<head>\n$viewportMeta\n</head>" + 
                        html.substring(match.range.last + 1)
                } else {
                    "$viewportMeta\n$html"
                }
            }
            else -> "$viewportMeta\n$html"
        }
    }
    
    /**
     * Analyze resource references inside HTML.
     */
    private fun analyzeHtmlReferences(html: String, baseDir: File?): List<ResourceReference> {
        val references = mutableListOf<ResourceReference>()
        
        // CSS <link> tags
        cssLinkRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            if (!path.startsWith("http://") && !path.startsWith("https://") && path.endsWith(".css", ignoreCase = true)) {
                references.add(analyzeReference(path, ReferenceType.CSS_LINK, baseDir))
            }
        }
        
        // JS <script> tags
        jsScriptRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            if (!path.startsWith("http://") && !path.startsWith("https://")) {
                references.add(analyzeReference(path, ReferenceType.JS_SCRIPT, baseDir))
            }
        }
        
        // Image
        imgSrcRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            if (!path.startsWith("http://") && !path.startsWith("https://") && !path.startsWith("data:")) {
                references.add(analyzeReference(path, ReferenceType.IMAGE, baseDir))
            }
        }
        
        return references
    }
    
    /**
     * Analyze a single resource reference.
     */
    private fun analyzeReference(path: String, type: ReferenceType, baseDir: File?): ResourceReference {
        val isAbsolute = path.startsWith("/")
        val resolvedPath = if (baseDir != null && !isAbsolute) {
            File(baseDir, path.removePrefix("./")).absolutePath
        } else null
        
        val exists = resolvedPath?.let { File(it).exists() } ?: false
        
        val issue = when {
            isAbsolute -> AppStringsProvider.current().absolutePathWarning
            !exists && resolvedPath != null -> AppStringsProvider.current().referencedFileNotExist
            else -> null
        }
        
        return ResourceReference(
            type = type,
            originalPath = path,
            resolvedPath = resolvedPath,
            isValid = !isAbsolute && (exists || resolvedPath == null),
            issue = issue
        )
    }
    
    /**
     * Detect file encoding (with cache).
     */
    private fun detectEncoding(file: File): String? {
        val cacheKey = file.absolutePath + "_" + file.lastModified()
        
        // Check cache
        encodingCache.get(cacheKey)?.let { return it }
        
        return try {
            // Only read the first 1000 bytes via stream to avoid loading the entire file into memory
            val bytes = ByteArray(minOf(1000, file.length().toInt().coerceAtLeast(0)))
            val bytesRead = file.inputStream().use { it.read(bytes) }
            val header = if (bytesRead < bytes.size) bytes.copyOf(bytesRead) else bytes
            
            val encoding = when {
                // Check BOM
                header.size >= 3 && header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte() -> "UTF-8"
                header.size >= 2 && header[0] == 0xFE.toByte() && header[1] == 0xFF.toByte() -> "UTF-16BE"
                header.size >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xFE.toByte() -> "UTF-16LE"
                else -> {
                    // Try to detect charset declarations
                    val content = String(header, Charsets.ISO_8859_1)
                    charsetRegex.find(content)?.groupValues?.get(1)?.uppercase() ?: "UTF-8"
                }
            }
            
            // Cache results
            encodingCache.put(cacheKey, encoding)
            encoding
        } catch (e: Exception) {
            AppLogger.e(TAG, "检测编码失败: ${file.path}", e)
            "UTF-8"
        }
    }
    
    /**
     * Clear the encoding cache.
     */
    fun clearEncodingCache() {
        encodingCache.evictAll()
    }
    
    /**
     * Read a file using the detected encoding.
     */
    fun readFileWithEncoding(file: File, encoding: String?): String {
        return try {
            val charset = when (encoding?.uppercase()) {
                "UTF-8", "UTF8" -> Charsets.UTF_8
                "GBK", "GB2312", "GB18030" -> Charset.forName("GBK")
                "UTF-16", "UTF-16BE" -> Charsets.UTF_16BE
                "UTF-16LE" -> Charsets.UTF_16LE
                "ISO-8859-1", "LATIN1" -> Charsets.ISO_8859_1
                else -> Charsets.UTF_8
            }
            file.readText(charset)
        } catch (e: Exception) {
            AppLogger.e(TAG, "读取文件失败: ${file.path}", e)
            // Fallback attempt
            try {
                file.readText(Charsets.UTF_8)
            } catch (e2: Exception) {
                file.readText(Charsets.ISO_8859_1)
            }
        }
    }
    
    /**
     * Check common JS issues.
     */
    private fun checkJsIssues(content: String, fileName: String, issues: MutableList<ProjectIssue>) {
        // Check if document.write is used (can cause issues in WebView)
        if (content.contains("document.write", ignoreCase = true)) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.WARNING,
                type = IssueType.SYNTAX_ERROR,
                message = AppStringsProvider.current().documentWriteWarning,
                file = fileName,
                suggestion = AppStringsProvider.current().suggestUseDomMethods
            ))
        }
        
        // Check for common syntax error patterns
        val unclosedBraces = content.count { it == '{' } - content.count { it == '}' }
        if (unclosedBraces != 0) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.WARNING,
                type = IssueType.SYNTAX_ERROR,
                message = AppStringsProvider.current().possiblyUnclosedBraces,
                file = fileName,
                suggestion = AppStringsProvider.current().suggestCheckBracesPaired
            ))
        }
    }
}
