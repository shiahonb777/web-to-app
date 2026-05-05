package com.webtoapp.util

import com.webtoapp.core.logging.AppLogger
import android.util.LruCache
import com.webtoapp.core.i18n.Strings
import java.io.File
import java.nio.charset.Charset










object HtmlProjectProcessor {

    private const val TAG = "HtmlProjectProcessor"


    private const val MAX_ANALYZE_FILE_SIZE = 5L * 1024 * 1024


    private val encodingCache = LruCache<String, String>(50)


    private val cssLinkRegex = Regex("""<link[^>]*href=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val jsScriptRegex = Regex("""<script[^>]*src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val imgSrcRegex = Regex("""<img[^>]*src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val absolutePathRegex = Regex("""(href|src)=["'](/[^"']+)["']""")
    private val protocolRelativeRegex = Regex("""(href|src)=["'](//[^"']+)["']""")

    private val localCssRegex = Regex("""<link[^>]*href=["'](?!https?://)[^"']*\.css["'][^>]*>""", RegexOption.IGNORE_CASE)



    private val localJsRegex = Regex("""(?s)<script[^>]*\bsrc=["'](?!https?://)([^"']*\.js)["'][^>]*>.*?</script>""", RegexOption.IGNORE_CASE)
    private val charsetRegex = Regex("""charset=["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE)


    private val closeHeadRegex = Regex("</head>", RegexOption.IGNORE_CASE)
    private val openBodyRegex = Regex("<body", RegexOption.IGNORE_CASE)
    private val openHtmlTagRegex = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)


    private val closeBodyRegex = Regex("</body>", RegexOption.IGNORE_CASE)
    private val closeHtmlRegex = Regex("</html>", RegexOption.IGNORE_CASE)


    private val openHeadRegex = Regex("<head>", RegexOption.IGNORE_CASE)




    data class ProjectAnalysis(
        val htmlFiles: List<FileInfo>,
        val cssFiles: List<FileInfo>,
        val jsFiles: List<FileInfo>,
        val otherFiles: List<FileInfo>,
        val issues: List<ProjectIssue>,
        val suggestions: List<String>
    )




    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val encoding: String?,
        val references: List<ResourceReference> = emptyList()
    )




    data class ResourceReference(
        val type: ReferenceType,
        val originalPath: String,
        val resolvedPath: String?,
        val isValid: Boolean,
        val issue: String? = null
    )

    enum class ReferenceType {
        CSS_LINK,
        JS_SCRIPT,
        IMAGE,
        CSS_IMPORT,
        CSS_URL,
        OTHER
    }




    data class ProjectIssue(
        val severity: IssueSeverity,
        val type: IssueType,
        val message: String,
        val file: String? = null,
        val suggestion: String? = null
    )

    enum class IssueSeverity {
        ERROR,
        WARNING,
        INFO
    }

    enum class IssueType {
        MISSING_FILE,
        ABSOLUTE_PATH,
        ENCODING_ISSUE,
        STRUCTURE_ISSUE,
        SYNTAX_ERROR,
        EXTERNAL_RESOURCE
    }




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


        htmlFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val encoding = detectEncoding(file)

                val references = if (file.length() <= MAX_ANALYZE_FILE_SIZE) {
                    val content = readFileWithEncoding(file, encoding)
                    analyzeHtmlReferences(content, file.parentFile)
                } else {
                    issues.add(ProjectIssue(
                        severity = IssueSeverity.INFO,
                        type = IssueType.STRUCTURE_ISSUE,
                        message = Strings.htmlFileTooLarge.replace("%s", (file.length() / 1024 / 1024).toString()),
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


                references.forEach { ref ->
                    if (!ref.isValid) {
                        issues.add(ProjectIssue(
                            severity = IssueSeverity.WARNING,
                            type = if (ref.originalPath.startsWith("/")) IssueType.ABSOLUTE_PATH else IssueType.MISSING_FILE,
                            message = ref.issue ?: "${Strings.resourceReferenceIssue}: ${ref.originalPath}",
                            file = file.name,
                            suggestion = when {
                                ref.originalPath.startsWith("/") -> Strings.suggestUseRelativePath
                                else -> Strings.suggestEnsureFileImported.replace("%s", ref.originalPath)
                            }
                        ))
                    }
                }
            } else {
                issues.add(ProjectIssue(
                    severity = IssueSeverity.ERROR,
                    type = IssueType.MISSING_FILE,
                    message = "${Strings.htmlFileNotFound}: $path"
                ))
            }
        }


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


                if (encoding != "UTF-8" && encoding != null) {
                    issues.add(ProjectIssue(
                        severity = IssueSeverity.WARNING,
                        type = IssueType.ENCODING_ISSUE,
                        message = Strings.cssEncodingWarning.replace("%s", encoding ?: "unknown"),
                        file = file.name,
                        suggestion = Strings.suggestSaveAsUtf8
                    ))
                }
            }
        }


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


                if (file.length() <= MAX_ANALYZE_FILE_SIZE) {
                    val content = readFileWithEncoding(file, encoding)

                    checkJsIssues(content, file.name, issues)
                }
            }
        }


        if (htmlFiles.isNotEmpty() && cssFiles.isEmpty() && jsFiles.isEmpty()) {
            val htmlFileObj = htmlFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() <= MAX_ANALYZE_FILE_SIZE }
            val htmlContent = htmlFileObj?.let { readFileWithEncoding(it, null) } ?: ""
            if (htmlContent.contains("<link", ignoreCase = true) || htmlContent.contains("<script", ignoreCase = true)) {
                suggestions.add(Strings.suggestExternalFilesDetected)
            }
        }

        if (issues.any { it.type == IssueType.ABSOLUTE_PATH }) {
            suggestions.add(Strings.suggestUseRelativePathsForAll)
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










    fun processHtmlContent(
        htmlContent: String,
        cssContent: String?,
        jsContent: String?,
        fixPaths: Boolean = true,
        removeLocalRefs: Boolean = true
    ): String {
        var result = htmlContent


        if (fixPaths) {
            result = fixResourcePaths(result)
        }


        if (removeLocalRefs) {
            result = removeLocalResourceReferences(result,
                hasCssContent = !cssContent.isNullOrBlank(),
                hasJsContent = !jsContent.isNullOrBlank())
        }


        if (!cssContent.isNullOrBlank()) {
            result = inlineCss(result, cssContent)
        }


        if (!jsContent.isNullOrBlank()) {
            result = inlineJs(result, jsContent)
        }


        if (!result.contains("viewport", ignoreCase = true)) {
            result = addViewportMeta(result)
        }

        return result
    }







    private fun fixResourcePaths(html: String): String {
        var result = html



        result = absolutePathRegex.replace(result) { match ->
            val attr = match.groupValues[1]
            val path = match.groupValues[2]
            """$attr=".${path}""""
        }

        // 修复协议相对路径（//example.com/js/app.js -> https://example.com/js/app.js）
        result = protocolRelativeRegex.replace(result) { match ->
            val attr = match.groupValues[1]
            val path = match.groupValues[2]
            """$attr="https:$path""""
        }

        // 不再修改 ../ 开头的相对路径，因为它们已经是正确的相对引用
        // 强制改为 ./ 会破坏子目录 HTML 文件的正确引用（如 pages/about.html 引用 ../js/app.js）

        return result
    }

    /**
     * 移除本地资源引用
     * @param html HTML 内容
     * @param hasCssContent 是否有 CSS 内容可内联
     * @param hasJsContent 是否有 JS 内容可内联
     * @return 处理后的 HTML
     */
    private fun removeLocalResourceReferences(
        html: String,
        hasCssContent: Boolean = true,
        hasJsContent: Boolean = true
    ): String {
        var result = html

        // 只在有 CSS 内容时才移除 CSS link 标签，否则保留引用让用户自己处理
        if (hasCssContent) {
            result = localCssRegex.replace(result, "<!-- CSS inlined -->")
        }

        // 只在有 JS 内容时才移除 JS script 标签，否则保留引用让用户自己处理
        if (hasJsContent) {
            result = localJsRegex.replace(result, "<!-- JS inlined -->")
        }

        return result
    }

    /**
     * 内联 CSS
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
     * 内联 JS
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
     * 包装 JS 确保安全执行
     */
    private fun wrapJsForSafeExecution(js: String): String {
        val trimmed = js.trim()
        if (trimmed.isEmpty()) return ""

        // Check是否已有 DOM 加载包装
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
     * 添加 viewport meta
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
     * 分析 HTML 中的资源引用
     */
    private fun analyzeHtmlReferences(html: String, baseDir: File?): List<ResourceReference> {
        val references = mutableListOf<ResourceReference>()

        // CSS link 标签
        cssLinkRegex.findAll(html).forEach { match ->
            val path = match.groupValues[1]
            if (!path.startsWith("http://") && !path.startsWith("https://") && path.endsWith(".css", ignoreCase = true)) {
                references.add(analyzeReference(path, ReferenceType.CSS_LINK, baseDir))
            }
        }

        // JS script 标签
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
     * 分析单个资源引用
     */
    private fun analyzeReference(path: String, type: ReferenceType, baseDir: File?): ResourceReference {
        val isAbsolute = path.startsWith("/")
        val resolvedPath = if (baseDir != null && !isAbsolute) {
            File(baseDir, path.removePrefix("./")).absolutePath
        } else null

        val exists = resolvedPath?.let { File(it).exists() } ?: false

        val issue = when {
            isAbsolute -> Strings.absolutePathWarning
            !exists && resolvedPath != null -> Strings.referencedFileNotExist
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
     * 检测文件编码（带缓存）
     */
    private fun detectEncoding(file: File): String? {
        val cacheKey = file.absolutePath + "_" + file.lastModified()

        // Check缓存
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
                    // 尝试检测 charset 声明
                    val content = String(header, Charsets.ISO_8859_1)
                    charsetRegex.find(content)?.groupValues?.get(1)?.uppercase() ?: "UTF-8"
                }
            }

            // Cache结果
            encodingCache.put(cacheKey, encoding)
            encoding
        } catch (e: Exception) {
            AppLogger.e(TAG, "检测编码失败: ${file.path}", e)
            "UTF-8"
        }
    }

    /**
     * 清除编码缓存
     */
    fun clearEncodingCache() {
        encodingCache.evictAll()
    }

    /**
     * 使用正确编码读取文件
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
            // 降级尝试
            try {
                file.readText(Charsets.UTF_8)
            } catch (e2: Exception) {
                file.readText(Charsets.ISO_8859_1)
            }
        }
    }

    /**
     * 检查 JS 常见问题
     */
    private fun checkJsIssues(content: String, fileName: String, issues: MutableList<ProjectIssue>) {
        // Check是否使用了 document.write（在 WebView 中可能有问题）
        if (content.contains("document.write", ignoreCase = true)) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.WARNING,
                type = IssueType.SYNTAX_ERROR,
                message = Strings.documentWriteWarning,
                file = fileName,
                suggestion = Strings.suggestUseDomMethods
            ))
        }

        // Check是否有语法错误的常见模式
        val unclosedBraces = content.count { it == '{' } - content.count { it == '}' }
        if (unclosedBraces != 0) {
            issues.add(ProjectIssue(
                severity = IssueSeverity.WARNING,
                type = IssueType.SYNTAX_ERROR,
                message = Strings.possiblyUnclosedBraces,
                file = fileName,
                suggestion = Strings.suggestCheckBracesPaired
            ))
        }
    }
}
